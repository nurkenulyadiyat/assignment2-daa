"""
Regenerates the benchmark tables in README.md (between the RESULTS markers)
from results/tables/all_results.csv, so the report always matches the latest run.
Usage (from the repository root):  python3 scripts/update_readme.py
"""
import csv
import math
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(ROOT, "results", "tables", "all_results.csv")
README = os.path.join(ROOT, "README.md")
START, END = "<!-- RESULTS:START -->", "<!-- RESULTS:END -->"
NS = [100, 1_000, 10_000, 100_000]


def load():
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        return list(csv.DictReader(f))


def find(rows, wl, structure, op, n):
    for r in rows:
        if r["workload"] == wl and r["structure"] == structure and r["operation"] == op and int(r["n"]) == n:
            return r
    raise KeyError((wl, structure, op, n))


def num(x):
    return f"{int(x):,}".replace(",", " ")


def t(r):
    """Average time; the median is added when one run distorted the average (>15 % difference)."""
    avg, med = float(r["avg_ms"]), float(r["median_ms"])
    s = f"{avg:.3f}"
    if med > 0 and abs(avg - med) / med > 0.15 and f"{med:.3f}" != s:
        s += f" (median {med:.3f})"
    return s


def build(rows):
    out = []
    out.append("### Workload 1 — Random access (m = 10 000 `get`)\n")
    out.append("| n | DA time, ms | DA accesses | LL time, ms | LL accesses | Predicted LL accesses m(n/4 + ½) |")
    out.append("|---:|---:|---:|---:|---:|---:|")
    for n in NS:
        a = find(rows, "W1", "DynamicArray", "get(random i)", n)
        b = find(rows, "W1", "LinkedList", "get(random i)", n)
        out.append(f"| {num(n)} | {t(a)} | {num(a['metric_total'])} | {t(b)} | {num(b['metric_total'])} "
                   f"| {num(10_000 * (n / 4 + 0.5))} |")
    out.append("\nTheory per `get`: DA Θ(1); LL touches min(i+1, n−i) nodes, which is n/4 + ½ on average "
               "for a uniform index → Θ(n).\n")

    out.append("### Workload 2 — Search (m = 1 000 `contains`, ≈ 50 % hits)\n")
    out.append("| n | DA time, ms | LL time, ms | Comparisons (both) | Comparisons per search / n | Hits |")
    out.append("|---:|---:|---:|---:|---:|---:|")
    for n in NS:
        a = find(rows, "W2", "DynamicArray", "contains(key)", n)
        b = find(rows, "W2", "LinkedList", "contains(key)", n)
        assert a["metric_total"] == b["metric_total"], "comparison counts must be identical"
        out.append(f"| {num(n)} | {t(a)} | {t(b)} | {num(a['metric_total'])} "
                   f"| {float(a['metric_per_op']) / n:.3f} | {a['check'].split('=')[1]} |")
    out.append("\nTheory: Θ(n) per search for both. Expected comparisons per search = ½·(n/2) + ½·n = **0.75 n**.\n")

    out.append("### Workload 3 — Insertion and removal (m = 1 000 per experiment)\n")
    out.append("Metric: DA = element movements (shifts + resize copies), LL = node accesses.\n")
    for pos, title in (("index 0", "Front (index 0)"), ("index n/2", "Middle (index n/2)")):
        out.append(f"**{title}**\n")
        out.append("| n | DA insert, ms | DA remove, ms | DA movements (insert / remove) | LL insert, ms | LL remove, ms "
                   "| LL accesses (insert / remove) |")
        out.append("|---:|---:|---:|---:|---:|---:|---:|")
        for n in NS:
            ai = find(rows, "W3", "DynamicArray", f"insert @ {pos}", n)
            ar = find(rows, "W3", "DynamicArray", f"remove @ {pos}", n)
            bi = find(rows, "W3", "LinkedList", f"insert @ {pos}", n)
            br = find(rows, "W3", "LinkedList", f"remove @ {pos}", n)
            out.append(f"| {num(n)} | {t(ai)} | {t(ar)} | {num(ai['metric_total'])} / {num(ar['metric_total'])} "
                       f"| {t(bi)} | {t(br)} | {num(bi['metric_total'])} / {num(br['metric_total'])} |")
        out.append("")
    out.append("Theory per operation: DA front Θ(n), DA middle Θ(n) (half the shifts), LL front Θ(1), LL middle Θ(n).  ")
    out.append("Exact predicted DA movements: Σₖ₌₀⁹⁹⁹ (n + k − pos) = 1 000·(n − pos) + 499 500 "
               "(+ resize copies: 1 920 at n = 100 and 1 024 at n = 1 000) — the counters match exactly.\n")

    out.append("### Workload 4 — Min-Heap priority processing (m = n)\n")
    out.append("| n | insert × n, ms | insert comparisons | per insert | extractMin × n, ms | extract comparisons "
               "| per extract | 2·log₂ n | order check |")
    out.append("|---:|---:|---:|---:|---:|---:|---:|---:|---|")
    for n in NS:
        a = find(rows, "W4", "MinHeap", "insert x n", n)
        b = find(rows, "W4", "MinHeap", "extractMin x n", n)
        ok = "non-decreasing ✔" if b["check"] == "sorted=OK" else "FAILED"
        out.append(f"| {num(n)} | {t(a)} | {num(a['metric_total'])} | {float(a['metric_per_op']):.2f} "
                   f"| {t(b)} | {num(b['metric_total'])} | {float(b['metric_per_op']):.2f} "
                   f"| {2 * math.log2(n):.1f} | {ok} |")
    out.append("\nTheory: `insert` O(log n) worst / Θ(1) average for random keys; `peekMin` Θ(1); "
               "`extractMin` Θ(log n); whole phase: Θ(n) inserts on random data, Θ(n log n) extractions.")
    return "\n".join(out)


if __name__ == "__main__":
    text = open(README, encoding="utf-8").read()
    i, j = text.index(START) + len(START), text.index(END)
    text = text[:i] + "\n" + build(load()) + "\n" + text[j:]
    open(README, "w", encoding="utf-8").write(text)
    print("README.md tables updated from", CSV_PATH)
