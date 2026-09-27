### Workload 4 - Min-Heap priority processing (m = n)

| Structure | Operation | n | Avg time (ms) | Median (ms) | Std (ms) | ns / op (avg) | Metric | Metric total | Metric / op | Theory (per op) |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|---|
| MinHeap | insert x n | 100 | 0.007 | 0.007 | 0.001 | 71.8 | comparisons | 194 | 1.9 | O(log n) worst, Θ(1) avg (random) |
| MinHeap | extractMin x n | 100 | 0.009 | 0.010 | 0.002 | 89.2 | comparisons | 841 | 8.4 | Θ(log n) |
| MinHeap | insert x n | 1,000 | 0.039 | 0.037 | 0.006 | 39.2 | comparisons | 2,232 | 2.2 | O(log n) worst, Θ(1) avg (random) |
| MinHeap | extractMin x n | 1,000 | 0.074 | 0.063 | 0.014 | 74.5 | comparisons | 14,994 | 15.0 | Θ(log n) |
| MinHeap | insert x n | 10,000 | 0.228 | 0.242 | 0.032 | 22.8 | comparisons | 22,593 | 2.3 | O(log n) worst, Θ(1) avg (random) |
| MinHeap | extractMin x n | 10,000 | 1.340 | 1.541 | 0.305 | 134.0 | comparisons | 216,736 | 21.7 | Θ(log n) |
| MinHeap | insert x n | 100,000 | 1.635 | 1.647 | 0.249 | 16.4 | comparisons | 227,662 | 2.3 | O(log n) worst, Θ(1) avg (random) |
| MinHeap | extractMin x n | 100,000 | 17.809 | 17.670 | 0.284 | 178.1 | comparisons | 2,831,463 | 28.3 | Θ(log n) |
