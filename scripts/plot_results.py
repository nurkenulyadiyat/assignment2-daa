"""
Builds the plots from results/tables/all_results.csv.
Usage (from the repository root):  python3 scripts/plot_results.py
Requires: matplotlib (pip install matplotlib)
"""
import csv
import math
import os
from collections import defaultdict

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(ROOT, "results", "tables", "all_results.csv")
PLOT_DIR = os.path.join(ROOT, "results", "plots")

COLORS = {"DynamicArray": "#1f77b4", "LinkedList": "#d62728", "MinHeap": "#2ca02c"}
STYLES = {"index 0": "-", "index n/2": "--"}


def load():
    rows = []
    with open(CSV_PATH, newline="", encoding="utf-8") as f:
        for r in csv.DictReader(f):
            r["n"] = int(r["n"])
            r["m"] = int(r["m"])
            r["avg_ms"] = float(r["avg_ms"])
            r["median_ms"] = float(r["median_ms"])
            r["metric_total"] = int(r["metric_total"])
            r["metric_per_op"] = float(r["metric_per_op"])
            rows.append(r)
    return rows


def series(rows, workload):
    """Groups rows of one workload into {(structure, operation): [(n, row), ...]}."""
    groups = defaultdict(list)
    for r in rows:
        if r["workload"] == workload:
            groups[(r["structure"], r["operation"])].append((r["n"], r))
    for k in groups:
        groups[k].sort(key=lambda p: p[0])
    return groups


def style_for(structure, operation):
    color = COLORS.get(structure, "black")
    ls = "-"
    for key, s in STYLES.items():
        if operation.endswith(key):
            ls = s
    marker = "o" if ("insert" in operation or "get" in operation or "contains" in operation) else "s"
    return color, ls, marker


def setup_axes(ax, title, ylabel):
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_title(title, fontsize=11)
    ax.set_xlabel("n (initial number of elements)")
    ax.set_ylabel(ylabel)
    ax.grid(True, which="both", alpha=0.3)


def plot_time(rows):
    fig, axes = plt.subplots(2, 2, figsize=(13, 10))
    panels = [("W1", "Workload 1 - Random access (10 000 x get)"),
              ("W2", "Workload 2 - Search (1 000 x contains)"),
              ("W3", "Workload 3 - Insert / remove (1 000 ops)"),
              ("W4", "Workload 4 - Min-Heap (n inserts, n extracts)")]
    for ax, (wl, title) in zip(axes.flat, panels):
        for (structure, op), pts in series(rows, wl).items():
            color, ls, marker = style_for(structure, op)
            ax.plot([p[0] for p in pts], [p[1]["avg_ms"] for p in pts],
                    color=color, linestyle=ls, marker=marker, label=f"{structure}: {op}")
        setup_axes(ax, title, "average time (ms, log scale)")
        ax.legend(fontsize=8)
    fig.suptitle("Plot 1 - Execution time vs n (average of 5 runs, log-log)", fontsize=13)
    fig.tight_layout()
    fig.savefig(os.path.join(PLOT_DIR, "plot1_time_vs_n.png"), dpi=130)
    plt.close(fig)


def plot_ops(rows):
    fig, axes = plt.subplots(2, 2, figsize=(13, 10))
    ns = sorted({r["n"] for r in rows})
    panels = [("W1", "Workload 1 - element accesses", "accesses"),
              ("W2", "Workload 2 - element comparisons", "comparisons"),
              ("W3", "Workload 3 - movements (DA) / node accesses (LL)", "movements or accesses"),
              ("W4", "Workload 4 - heap comparisons", "comparisons")]
    for ax, (wl, title, ylabel) in zip(axes.flat, panels):
        for (structure, op), pts in series(rows, wl).items():
            color, ls, marker = style_for(structure, op)
            ax.plot([p[0] for p in pts], [p[1]["metric_total"] for p in pts],
                    color=color, linestyle=ls, marker=marker, label=f"{structure}: {op}")
        # theoretical reference curves
        if wl == "W1":
            ax.plot(ns, [10_000 * (n / 4 + 0.5) for n in ns], "k:", label="prediction LL: m(n/4 + 1/2)")
        if wl == "W2":
            ax.plot(ns, [1_000 * 0.75 * n for n in ns], "k:", label="prediction: m * 3n/4")
        if wl == "W4":
            ax.plot(ns, [2 * n * math.log2(n) for n in ns], "k:", label="reference: 2 n log2 n")
            ax.plot(ns, [2.6 * n for n in ns], "k-.", label="reference: 2.6 n")
        setup_axes(ax, title, ylabel + " (log scale)")
        ax.legend(fontsize=8)
    fig.suptitle("Plot 2 - Counted operations vs n (log-log)", fontsize=13)
    fig.tight_layout()
    fig.savefig(os.path.join(PLOT_DIR, "plot2_operations_vs_n.png"), dpi=130)
    plt.close(fig)


def plot_heap_per_op(rows):
    fig, ax = plt.subplots(figsize=(7.5, 5))
    for (structure, op), pts in series(rows, "W4").items():
        xs = [math.log2(p[0]) for p in pts]
        ax.plot(xs, [p[1]["metric_per_op"] for p in pts], marker="o",
                label=f"{op}: comparisons per operation")
    xs = [math.log2(n) for n in sorted({r["n"] for r in rows})]
    ax.plot(xs, [2 * x for x in xs], "k:", label="2 log2 n (upper bound for extractMin)")
    ax.set_xlabel("log2 n")
    ax.set_ylabel("comparisons per operation")
    ax.set_title("Plot 3 - Min-Heap: comparisons per operation vs log2 n")
    ax.grid(True, alpha=0.3)
    ax.legend(fontsize=9)
    fig.tight_layout()
    fig.savefig(os.path.join(PLOT_DIR, "plot3_heap_comparisons_per_op.png"), dpi=130)
    plt.close(fig)


def plot_w3_bars(rows):
    n_max = max(r["n"] for r in rows)
    data = [r for r in rows if r["workload"] == "W3" and r["n"] == n_max]
    labels = ["insert @ index 0", "remove @ index 0", "insert @ index n/2", "remove @ index n/2"]
    fig, ax = plt.subplots(figsize=(9, 5))
    width = 0.38
    for k, structure in enumerate(["DynamicArray", "LinkedList"]):
        vals = [next(r["avg_ms"] for r in data if r["structure"] == structure and r["operation"] == lab)
                for lab in labels]
        xs = [i + (k - 0.5) * width for i in range(len(labels))]
        bars = ax.bar(xs, vals, width, label=structure, color=COLORS[structure])
        for b, v in zip(bars, vals):
            ax.text(b.get_x() + b.get_width() / 2, v * 1.15, f"{v:.3g}", ha="center", fontsize=8)
    ax.set_yscale("log")
    ax.set_xticks(range(len(labels)))
    ax.set_xticklabels(labels)
    ax.set_ylabel("average time for 1 000 ops (ms, log scale)")
    ax.set_title(f"Plot 4 - Workload 3 at n = {n_max:,}: position matters")
    ax.grid(True, axis="y", alpha=0.3)
    ax.legend()
    fig.tight_layout()
    fig.savefig(os.path.join(PLOT_DIR, "plot4_workload3_positions.png"), dpi=130)
    plt.close(fig)


if __name__ == "__main__":
    os.makedirs(PLOT_DIR, exist_ok=True)
    data = load()
    plot_time(data)
    plot_ops(data)
    plot_heap_per_op(data)
    plot_w3_bars(data)
    print("Plots written to", PLOT_DIR)
