import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Runs the four workloads of the assignment and writes the results to results/tables/.
 *
 * Rules followed for every experiment:
 *  - n in {100, 1 000, 10 000, 100 000}; the workload definition does not change with n;
 *  - input data (values, indices, search keys) is generated with new Random(42) BEFORE timing;
 *  - the structure is (re)built outside the timed section before every run;
 *  - a global warm-up pass (every experiment with n <= 10 000 run 3 times, discarded), then for
 *    every experiment 2 more warm-up runs (discarded) + 5 measured runs; the average is reported;
 *  - the timed loops live in small static "kernel" methods so the JIT compiles them as ordinary
 *    hot methods, which makes short measurements more stable;
 *  - System.nanoTime() around the operations only; no printing inside the timed section;
 *  - instrumentation counters are reset right before the timed section.
 *
 * Run: java -XX:+UseSerialGC -Xms1g -Xmx1g -cp out Benchmark [outputDir]
 * (Serial GC + fixed heap size = fewer background threads and more stable timings.)
 */
public class Benchmark {
    static final int[] NS = {100, 1_000, 10_000, 100_000};
    /** Sizes used in the global JIT warm-up pass (results discarded). */
    static final int[] WARMUP_NS = {100, 1_000, 10_000};
    static final int GLOBAL_WARMUP_REPEATS = 3;
    static boolean recording = true;
    static final int RUNS = 5;
    static final int WARMUP_RUNS = 2;
    static final long SEED = 42L;
    static final int VALUE_BOUND = 1_000_000;   // random values are in [0, 1 000 000)

    static final int M_GET = 10_000;             // Workload 1
    static final int M_SEARCH = 1_000;           // Workload 2
    static final int M_UPDATE = 1_000;           // Workload 3

    /** Results of computations are accumulated here so the JIT cannot remove the timed code. */
    static volatile long sink;

    static final Map<String, Supplier<SimpleList<Integer>>> LISTS = new LinkedHashMap<>();
    static {
        LISTS.put("DynamicArray", DynamicArray::new);
        LISTS.put("LinkedList", LinkedList::new);
    }

    // ------------------------------------------------------------------ measurement framework

    enum Metric { ACCESSES, COMPARISONS, MOVEMENTS }

    /** Filled in by one run of an experiment. */
    static final class Counters {
        long accesses;
        long comparisons;
        long movements;
        String note = "";

        long get(Metric m) {
            return switch (m) {
                case ACCESSES -> accesses;
                case COMPARISONS -> comparisons;
                case MOVEMENTS -> movements;
            };
        }
    }

    /** One run: build the input state (untimed), time the operations, return elapsed nanoseconds. */
    @FunctionalInterface
    interface Experiment {
        long runOnce(Counters c);
    }

    record Result(String workload, String structure, String operation, int n, int m,
                  long[] runNs, Metric metric, long metricValue, String theoryPerOp, String note) {

        double avgNs() {
            return Arrays.stream(runNs).average().orElse(0);
        }

        double stdNs() {
            double mean = avgNs();
            double var = Arrays.stream(runNs).mapToDouble(t -> (t - mean) * (t - mean)).sum() / runNs.length;
            return Math.sqrt(var);
        }

        double medianNs() {
            long[] sorted = runNs.clone();
            Arrays.sort(sorted);
            return sorted[sorted.length / 2];
        }

        double avgMs()    { return avgNs() / 1e6; }
        double nsPerOp()  { return avgNs() / m; }
        double metricPerOp() { return (double) metricValue / m; }
    }

    static final List<Result> RESULTS = new ArrayList<>();

    static Result measure(String workload, String structure, String operation, int n, int m,
                          Metric metric, String theory, Experiment e) {
        Counters c = new Counters();
        if (!recording) {           // global warm-up pass: execute, but record nothing
            for (int w = 0; w < GLOBAL_WARMUP_REPEATS; w++) {
                e.runOnce(c);
            }
            return null;
        }
        for (int w = 0; w < WARMUP_RUNS; w++) {
            e.runOnce(c);
        }
        long[] times = new long[RUNS];
        long metricValue = -1;
        String note = "";
        for (int r = 0; r < RUNS; r++) {
            c = new Counters();
            System.gc();                     // reduce the chance of a GC pause inside the timed section
            times[r] = e.runOnce(c);
            long value = c.get(metric);
            if (metricValue != -1 && value != metricValue) {
                throw new IllegalStateException("Metric changed between runs: " + workload + " " + structure);
            }
            metricValue = value;
            note = c.note;
        }
        Result res = new Result(workload, structure, operation, n, m, times, metric, metricValue, theory, note);
        RESULTS.add(res);
        log(String.format(Locale.US, "  %-13s %-22s n=%-7d avg=%10.3f ms  %-11s=%,d %s%n",
                structure, operation, n, res.avgMs(), metric.name().toLowerCase(), metricValue, note));
        return res;
    }

    // ------------------------------------------------------------------ helpers

    static void log(String line) {
        if (recording) {
            System.out.println(line);
        }
    }

    static Integer[] randomValues(Random rnd, int count) {
        Integer[] a = new Integer[count];      // boxed in advance so boxing is not timed
        for (int i = 0; i < count; i++) {
            a[i] = rnd.nextInt(VALUE_BOUND);
        }
        return a;
    }

    // Timed kernels are separate small methods: the JIT compiles each of them once as a
    // normal hot method (instead of on-stack-replacing a loop inside a lambda), which makes
    // the timings of short experiments more stable.

    static long getAll(SimpleList<Integer> s, int[] indices) {
        long acc = 0;
        for (int index : indices) {
            acc += s.get(index);
        }
        return acc;
    }

    static int searchAll(SimpleList<Integer> s, Integer[] keys) {
        int hits = 0;
        for (Integer key : keys) {
            if (s.contains(key)) {
                hits++;
            }
        }
        return hits;
    }

    static void insertAll(SimpleList<Integer> s, int pos, Integer[] values) {
        for (Integer v : values) {
            s.add(pos, v);
        }
    }

    static long removeAll(SimpleList<Integer> s, int pos, int count) {
        long acc = 0;
        for (int k = 0; k < count; k++) {
            acc += s.remove(pos);
        }
        return acc;
    }

    static void heapInsertAll(MinHeap<Integer> h, Integer[] values) {
        for (Integer v : values) {
            h.insert(v);
        }
    }

    static void heapExtractAll(MinHeap<Integer> h, Integer[] out) {
        for (int k = 0; k < out.length; k++) {
            out[k] = h.extractMin();
        }
    }

    static SimpleList<Integer> build(Supplier<SimpleList<Integer>> factory, Integer[] values) {
        SimpleList<Integer> s = factory.get();
        for (Integer v : values) {
            s.add(v);
        }
        return s;
    }

    // ------------------------------------------------------------------ Workload 1: random access

    static void workload1(int[] sizes) {
        log("\nWorkload 1 - Random access (m = " + M_GET + " get operations)");
        for (int n : sizes) {
            Random rnd = new Random(SEED);
            Integer[] base = randomValues(rnd, n);
            int[] indices = new int[M_GET];
            for (int k = 0; k < M_GET; k++) {
                indices[k] = rnd.nextInt(n);
            }
            for (var entry : LISTS.entrySet()) {
                String name = entry.getKey();
                String theory = name.equals("DynamicArray") ? "Θ(1)" : "Θ(min(i, n-i)) → Θ(n) avg";
                measure("W1", name, "get(random i)", n, M_GET, Metric.ACCESSES, theory, c -> {
                    SimpleList<Integer> s = build(entry.getValue(), base);
                    s.resetCounters();
                    long t0 = System.nanoTime();
                    long acc = getAll(s, indices);
                    long t1 = System.nanoTime();
                    sink += acc;
                    c.accesses = s.getAccesses();
                    return t1 - t0;
                });
            }
        }
    }

    // ------------------------------------------------------------------ Workload 2: search

    static void workload2(int[] sizes) {
        log("\nWorkload 2 - Search (m = " + M_SEARCH + " contains operations, ~50% hits)");
        for (int n : sizes) {
            Random rnd = new Random(SEED);
            Integer[] base = randomValues(rnd, n);
            // Half of the keys are taken from the structure (successful search),
            // half are negative numbers, which can never be present (unsuccessful search).
            Integer[] keys = new Integer[M_SEARCH];
            for (int k = 0; k < M_SEARCH; k++) {
                keys[k] = rnd.nextBoolean()
                        ? Integer.valueOf(base[rnd.nextInt(n)])
                        : Integer.valueOf(-1 - rnd.nextInt(VALUE_BOUND));
            }
            for (var entry : LISTS.entrySet()) {
                measure("W2", entry.getKey(), "contains(key)", n, M_SEARCH, Metric.COMPARISONS,
                        "Θ(1) best, Θ(n) avg/worst", c -> {
                    SimpleList<Integer> s = build(entry.getValue(), base);
                    s.resetCounters();
                    long t0 = System.nanoTime();
                    int hits = searchAll(s, keys);
                    long t1 = System.nanoTime();
                    sink += hits;
                    c.comparisons = s.getComparisons();
                    c.note = "hits=" + hits;
                    return t1 - t0;
                });
            }
        }
    }

    // ------------------------------------------------------------------ Workload 3: insertion / removal

    static void workload3(int[] sizes) {
        log("\nWorkload 3 - Insertion and removal (m = " + M_UPDATE + " operations)");
        for (int n : sizes) {
            Random rnd = new Random(SEED);
            Integer[] base = randomValues(rnd, n);
            Integer[] extra = randomValues(rnd, M_UPDATE);
            int[][] positions = {{0}, {n / 2}};
            String[] posNames = {"index 0", "index n/2"};

            for (int p = 0; p < positions.length; p++) {
                final int pos = positions[p][0];
                final String posName = posNames[p];
                for (var entry : LISTS.entrySet()) {
                    String name = entry.getKey();
                    boolean isArray = name.equals("DynamicArray");
                    Metric metric = isArray ? Metric.MOVEMENTS : Metric.ACCESSES;
                    String theory;
                    if (isArray) {
                        theory = pos == 0 ? "Θ(n) (shift all)" : "Θ(n) (shift n/2)";
                    } else {
                        theory = pos == 0 ? "Θ(1)" : "Θ(n) (walk to n/2)";
                    }

                    // A. / C. insertion: structure of n elements, 1 000 insertions at pos
                    measure("W3", name, "insert @ " + posName, n, M_UPDATE, metric, theory, c -> {
                        SimpleList<Integer> s = build(entry.getValue(), base);
                        s.resetCounters();
                        long t0 = System.nanoTime();
                        insertAll(s, pos, extra);
                        long t1 = System.nanoTime();
                        c.accesses = s.getAccesses();
                        c.movements = s.getMovements();
                        return t1 - t0;
                    });

                    // B. / C. removal: restore the state reached after the insertions (untimed),
                    // then 1 000 removals at pos. They remove exactly the inserted elements,
                    // so the structure returns to the original n elements (checked below).
                    measure("W3", name, "remove @ " + posName, n, M_UPDATE, metric, theory, c -> {
                        SimpleList<Integer> s = build(entry.getValue(), base);
                        for (int k = 0; k < M_UPDATE; k++) {
                            s.add(pos, extra[k]);
                        }
                        s.resetCounters();
                        long t0 = System.nanoTime();
                        long acc = removeAll(s, pos, M_UPDATE);
                        long t1 = System.nanoTime();
                        sink += acc;
                        c.accesses = s.getAccesses();
                        c.movements = s.getMovements();
                        boolean restored = Arrays.equals(s.toArray(), base);
                        if (!restored) {
                            throw new IllegalStateException("Removal did not restore the original structure");
                        }
                        c.note = "restored=OK";
                        return t1 - t0;
                    });
                }
            }
        }
    }

    // ------------------------------------------------------------------ Workload 4: priority processing

    static void workload4(int[] sizes) {
        log("\nWorkload 4 - Priority processing with MinHeap (m = n)");
        for (int n : sizes) {
            Random rnd = new Random(SEED);
            Integer[] values = randomValues(rnd, n);
            Integer[] expectedOrder = values.clone();
            Arrays.sort(expectedOrder);

            // Steps 1-3, 6: empty heap, n insertions (timed), comparisons counted
            measure("W4", "MinHeap", "insert x n", n, n, Metric.COMPARISONS,
                    "O(log n) worst, Θ(1) avg (random)", c -> {
                MinHeap<Integer> h = new MinHeap<>();
                long t0 = System.nanoTime();
                heapInsertAll(h, values);
                long t1 = System.nanoTime();
                c.comparisons = h.getComparisons();
                if (!h.isValidHeap()) {
                    throw new IllegalStateException("Heap property violated after insertions");
                }
                c.note = "heap-valid=OK";
                return t1 - t0;
            });

            // Steps 4-7: n extractions (timed), comparisons counted, order verified afterwards
            measure("W4", "MinHeap", "extractMin x n", n, n, Metric.COMPARISONS,
                    "Θ(log n)", c -> {
                MinHeap<Integer> h = new MinHeap<>();
                for (int k = 0; k < n; k++) {
                    h.insert(values[k]);
                }
                h.resetCounters();
                Integer[] out = new Integer[n];
                long t0 = System.nanoTime();
                heapExtractAll(h, out);
                long t1 = System.nanoTime();
                c.comparisons = h.getComparisons();
                for (int k = 1; k < n; k++) {
                    if (out[k - 1] > out[k]) {
                        throw new IllegalStateException("Extracted sequence is not non-decreasing");
                    }
                }
                if (!Arrays.equals(out, expectedOrder) || !h.isEmpty()) {
                    throw new IllegalStateException("Extracted sequence differs from sorted input");
                }
                c.note = "sorted=OK";
                return t1 - t0;
            });
        }
    }

    // ------------------------------------------------------------------ output

    static void writeOutputs(Path dir) throws IOException {
        Files.createDirectories(dir);

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("all_results.csv")))) {
            w.println("workload,structure,operation,n,m,avg_ms,median_ms,std_ms,ns_per_op,metric,metric_total,metric_per_op,theory_per_op,check");
            for (Result r : RESULTS) {
                w.printf(Locale.US, "%s,%s,%s,%d,%d,%.4f,%.4f,%.4f,%.2f,%s,%d,%.2f,\"%s\",%s%n",
                        r.workload(), r.structure(), r.operation(), r.n(), r.m(), r.avgMs(),
                        r.medianNs() / 1e6, r.stdNs() / 1e6, r.nsPerOp(), r.metric().name().toLowerCase(),
                        r.metricValue(), r.metricPerOp(), r.theoryPerOp(), r.note());
            }
        }

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("raw_runs.csv")))) {
            w.println("workload,structure,operation,n,run,ns");
            for (Result r : RESULTS) {
                for (int i = 0; i < r.runNs().length; i++) {
                    w.printf("%s,%s,%s,%d,%d,%d%n", r.workload(), r.structure(), r.operation(), r.n(), i + 1, r.runNs()[i]);
                }
            }
        }

        String[][] workloads = {
                {"W1", "workload1_random_access.md", "Workload 1 - Random access (m = 10 000 get)"},
                {"W2", "workload2_search.md", "Workload 2 - Search (m = 1 000 contains)"},
                {"W3", "workload3_insert_remove.md", "Workload 3 - Insertion and removal (m = 1 000)"},
                {"W4", "workload4_priority.md", "Workload 4 - Min-Heap priority processing (m = n)"},
        };
        for (String[] wl : workloads) {
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve(wl[1])))) {
                w.println("### " + wl[2]);
                w.println();
                w.println("| Structure | Operation | n | Avg time (ms) | Median (ms) | Std (ms) | ns / op (avg) | Metric | Metric total | Metric / op | Theory (per op) |");
                w.println("|---|---|---:|---:|---:|---:|---:|---|---:|---:|---|");
                for (Result r : RESULTS) {
                    if (!r.workload().equals(wl[0])) continue;
                    w.printf(Locale.US, "| %s | %s | %,d | %.3f | %.3f | %.3f | %.1f | %s | %,d | %.1f | %s |%n",
                            r.structure(), r.operation(), r.n(), r.avgMs(), r.medianNs() / 1e6, r.stdNs() / 1e6, r.nsPerOp(),
                            r.metric().name().toLowerCase(), r.metricValue(), r.metricPerOp(), r.theoryPerOp());
                }
            }
        }

        try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(dir.resolve("environment.txt")))) {
            w.println("Date:        " + LocalDateTime.now());
            w.println("Java:        " + System.getProperty("java.vm.name") + " " + System.getProperty("java.version"));
            w.println("OS:          " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
            w.println("CPU cores:   " + Runtime.getRuntime().availableProcessors());
            w.println("JVM args:    " + java.lang.management.ManagementFactory.getRuntimeMXBean().getInputArguments());
            w.println("Max heap:    " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
            w.println("Runs:        " + RUNS + " measured + " + WARMUP_RUNS + " warm-up");
            w.println("Seed:        " + SEED);
        }
    }

    public static void main(String[] args) throws IOException {
        Path outDir = Paths.get(args.length > 0 ? args[0] : "results/tables");
        long start = System.nanoTime();

        // Global warm-up: run every workload once on smaller sizes so the JIT compiler has
        // already optimised all code paths before the first measured run (important for n = 100).
        System.out.println("JIT warm-up pass (not recorded)...");
        recording = false;
        workload1(WARMUP_NS);
        workload2(WARMUP_NS);
        workload3(WARMUP_NS);
        workload4(WARMUP_NS);
        recording = true;

        workload1(NS);
        workload2(NS);
        workload3(NS);
        workload4(NS);

        writeOutputs(outDir);
        System.out.printf(Locale.US, "%nDone in %.1f s. Results written to %s (sink=%d)%n",
                (System.nanoTime() - start) / 1e9, outDir.toAbsolutePath(), sink);
    }
}
