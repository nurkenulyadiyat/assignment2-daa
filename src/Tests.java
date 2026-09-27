import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Self-contained test suite (no external libraries).
 * Run: java -cp out Tests      -> exits with code 1 if any check fails.
 *
 * Both list implementations are tested by the same code through SimpleList and are
 * compared against java.util.ArrayList. The heap is compared against java.util.PriorityQueue.
 */
public class Tests {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        runListSuite("DynamicArray", DynamicArray::new);
        runListSuite("LinkedList", LinkedList::new);
        runHeapSuite();

        System.out.printf("%nTests passed: %d, failed: %d%n", passed, failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ------------------------------------------------------------------ lists

    private static void runListSuite(String name, Supplier<SimpleList<Integer>> factory) {
        System.out.println("== " + name + " ==");

        // Empty structure
        SimpleList<Integer> s = factory.get();
        check(name + " empty: size 0", s.size() == 0 && s.isEmpty());
        check(name + " empty: contains false", !s.contains(1));
        expectIndexError(name + " empty: get(0)", () -> s.get(0));
        expectIndexError(name + " empty: remove(0)", () -> s.remove(0));
        s.add(0, 5); // add at index 0 == size is valid on an empty list
        check(name + " empty: add(0, x) works", s.size() == 1 && s.get(0) == 5);

        // One element
        SimpleList<Integer> one = factory.get();
        one.add(42);
        check(name + " one: get(0)", one.get(0) == 42);
        check(name + " one: contains", one.contains(42) && !one.contains(7));
        check(name + " one: remove returns value", one.remove(0) == 42);
        check(name + " one: empty after remove", one.isEmpty());

        // Multiple elements, add(index, x), remove(index)
        SimpleList<Integer> m = factory.get();
        for (int i = 0; i < 10; i++) {
            m.add(i);                                    // 0..9
        }
        check(name + " multi: order", Arrays.equals(m.toArray(), new Object[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));
        m.add(5, 100);                                   // middle insert
        m.add(0, -1);                                    // front insert
        m.add(m.size(), 999);                            // back insert
        check(name + " multi: inserts", Arrays.equals(m.toArray(),
                new Object[]{-1, 0, 1, 2, 3, 4, 100, 5, 6, 7, 8, 9, 999}));
        check(name + " multi: remove middle", m.remove(6) == 100);
        check(name + " multi: remove front", m.remove(0) == -1);
        check(name + " multi: remove back", m.remove(m.size() - 1) == 999);
        check(name + " multi: back to 0..9", Arrays.equals(m.toArray(), new Object[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9}));

        // Duplicates
        SimpleList<Integer> d = factory.get();
        for (int v : new int[]{3, 7, 3, 3, 9}) {
            d.add(v);
        }
        check(name + " dup: contains", d.contains(3) && d.contains(9) && !d.contains(4));
        d.remove(0);
        check(name + " dup: still contains 3 after removing one copy", d.contains(3));
        d.remove(1);
        d.remove(1);
        check(name + " dup: no 3 left", !d.contains(3) && d.size() == 2);

        // Boundary indices
        SimpleList<Integer> b = factory.get();
        for (int i = 0; i < 100; i++) {
            b.add(i);
        }
        check(name + " boundary: get(0)", b.get(0) == 0);
        check(name + " boundary: get(size-1)", b.get(99) == 99);
        check(name + " boundary: get(size/2)", b.get(50) == 50);
        check(name + " boundary: remove(size-1)", b.remove(99) == 99 && b.size() == 99);

        // Invalid indices
        expectIndexError(name + " invalid: get(-1)", () -> b.get(-1));
        expectIndexError(name + " invalid: get(size)", () -> b.get(b.size()));
        expectIndexError(name + " invalid: add(-1, x)", () -> b.add(-1, 0));
        expectIndexError(name + " invalid: add(size+1, x)", () -> b.add(b.size() + 1, 0));
        expectIndexError(name + " invalid: remove(size)", () -> b.remove(b.size()));
        check(name + " invalid: structure unchanged after errors", b.size() == 99);

        // Null values are allowed in lists
        SimpleList<Integer> nul = factory.get();
        nul.add(null);
        check(name + " null: contains(null)", nul.contains(null) && !nul.contains(0));

        // Large input + randomized differential test against java.util.ArrayList
        differentialTest(name, factory, 100_000, 20_000, 1);
        differentialTest(name, factory, 10, 5_000, 2);   // small list, many boundary situations

        // Counters sanity
        SimpleList<Integer> c = factory.get();
        for (int i = 0; i < 1000; i++) {
            c.add(i);
        }
        c.resetCounters();
        c.contains(-5);
        check(name + " counters: failed search compares all n", c.getComparisons() == 1000);
        c.resetCounters();
        c.contains(0);
        check(name + " counters: first-element search = 1 comparison", c.getComparisons() == 1);
    }

    private static void differentialTest(String name, Supplier<SimpleList<Integer>> factory,
                                         int n, int ops, long seed) {
        Random rnd = new Random(seed);
        SimpleList<Integer> mine = factory.get();
        List<Integer> ref = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int v = rnd.nextInt(1000);
            mine.add(v);
            ref.add(v);
        }
        boolean ok = true;
        for (int k = 0; k < ops && ok; k++) {
            int op = rnd.nextInt(5);
            switch (op) {
                case 0 -> { int v = rnd.nextInt(1000); mine.add(v); ref.add(v); }
                case 1 -> {
                    int idx = rnd.nextInt(ref.size() + 1);
                    int v = rnd.nextInt(1000);
                    mine.add(idx, v);
                    ref.add(idx, v);
                }
                case 2 -> {
                    if (!ref.isEmpty()) {
                        int idx = rnd.nextInt(ref.size());
                        ok = mine.remove(idx).equals(ref.remove(idx));
                    }
                }
                case 3 -> {
                    if (!ref.isEmpty()) {
                        int idx = rnd.nextInt(ref.size());
                        ok = mine.get(idx).equals(ref.get(idx));
                    }
                }
                default -> {
                    int v = rnd.nextInt(1200);
                    ok = mine.contains(v) == ref.contains(v);
                }
            }
            ok = ok && mine.size() == ref.size();
        }
        ok = ok && Arrays.equals(mine.toArray(), ref.toArray());
        check(name + " differential vs ArrayList (n=" + n + ", ops=" + ops + ")", ok);
    }

    // ------------------------------------------------------------------ heap

    private static void runHeapSuite() {
        System.out.println("== MinHeap ==");

        // Empty
        MinHeap<Integer> e = new MinHeap<>();
        check("heap empty: isEmpty", e.isEmpty() && e.size() == 0);
        expectNoSuchElement("heap empty: peekMin", e::peekMin);
        expectNoSuchElement("heap empty: extractMin", e::extractMin);

        // One element
        MinHeap<Integer> one = new MinHeap<>();
        one.insert(7);
        check("heap one: peekMin", one.peekMin() == 7 && one.size() == 1);
        check("heap one: extractMin", one.extractMin() == 7 && one.isEmpty());

        // Multiple elements with duplicates; heap property after every operation
        MinHeap<Integer> h = new MinHeap<>();
        int[] vals = {5, 3, 8, 3, 1, 9, 1, 7, 2, 2, 6, 0, 4};
        boolean validAfterInsert = true;
        for (int v : vals) {
            h.insert(v);
            validAfterInsert &= h.isValidHeap();
        }
        check("heap: property holds after each insert", validAfterInsert);
        check("heap: peekMin is 0", h.peekMin() == 0);
        check("heap: peekMin does not remove", h.size() == vals.length);
        int[] sorted = vals.clone();
        Arrays.sort(sorted);
        boolean validAfterExtract = true;
        boolean orderOk = true;
        for (int expected : sorted) {
            orderOk &= h.extractMin() == expected;
            validAfterExtract &= h.isValidHeap();
        }
        check("heap: property holds after each extract", validAfterExtract);
        check("heap: duplicates extracted in sorted order", orderOk && h.isEmpty());

        expectNullPointer("heap: insert(null) rejected", () -> new MinHeap<Integer>().insert(null));

        // Already sorted and reverse-sorted inputs (best and worst cases for insert)
        check("heap: ascending input", extractsSorted(ascending(1000)));
        check("heap: descending input", extractsSorted(descending(1000)));

        // Large random input compared against PriorityQueue
        Random rnd = new Random(42);
        MinHeap<Integer> big = new MinHeap<>();
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        for (int i = 0; i < 100_000; i++) {
            int v = rnd.nextInt(1_000_000);
            big.insert(v);
            pq.add(v);
        }
        check("heap large: valid after 100k inserts", big.isValidHeap());
        boolean same = true;
        boolean nonDecreasing = true;
        int prev = Integer.MIN_VALUE;
        while (!pq.isEmpty()) {
            int a = big.extractMin();
            same &= a == pq.poll();
            nonDecreasing &= a >= prev;
            prev = a;
        }
        check("heap large: same sequence as PriorityQueue", same && big.isEmpty());
        check("heap large: non-decreasing extraction", nonDecreasing);

        // Interleaved random operations vs PriorityQueue
        MinHeap<Integer> mix = new MinHeap<>();
        PriorityQueue<Integer> ref = new PriorityQueue<>();
        boolean ok = true;
        for (int k = 0; k < 50_000 && ok; k++) {
            if (ref.isEmpty() || rnd.nextInt(3) > 0) {
                int v = rnd.nextInt(500);
                mix.insert(v);
                ref.add(v);
            } else if (rnd.nextBoolean()) {
                ok = mix.extractMin().equals(ref.poll());
            } else {
                ok = mix.peekMin().equals(ref.peek());
            }
            ok = ok && mix.size() == ref.size();
        }
        check("heap: interleaved ops match PriorityQueue", ok && mix.isValidHeap());
    }

    private static boolean extractsSorted(int[] input) {
        MinHeap<Integer> h = new MinHeap<>();
        for (int v : input) {
            h.insert(v);
        }
        int[] sorted = input.clone();
        Arrays.sort(sorted);
        for (int v : sorted) {
            if (h.extractMin() != v) {
                return false;
            }
        }
        return h.isEmpty();
    }

    private static int[] ascending(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i;
        return a;
    }

    private static int[] descending(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = n - i;
        return a;
    }

    // ------------------------------------------------------------------ helpers

    private static void check(String label, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  PASS  " + label);
        } else {
            failed++;
            System.out.println("  FAIL  " + label);
        }
    }

    private static void expectIndexError(String label, Runnable action) {
        expectException(label, action, IndexOutOfBoundsException.class);
    }

    private static void expectNoSuchElement(String label, Runnable action) {
        expectException(label, action, NoSuchElementException.class);
    }

    private static void expectNullPointer(String label, Runnable action) {
        expectException(label, action, NullPointerException.class);
    }

    private static void expectException(String label, Runnable action, Class<? extends Throwable> type) {
        try {
            action.run();
            check(label + " throws " + type.getSimpleName(), false);
        } catch (Throwable t) {
            check(label + " throws " + type.getSimpleName(), type.isInstance(t));
        }
    }
}
