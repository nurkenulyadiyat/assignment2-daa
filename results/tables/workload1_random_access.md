### Workload 1 - Random access (m = 10 000 get)

| Structure | Operation | n | Avg time (ms) | Median (ms) | Std (ms) | ns / op (avg) | Metric | Metric total | Metric / op | Theory (per op) |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|---|
| DynamicArray | get(random i) | 100 | 0.399 | 0.449 | 0.202 | 39.9 | accesses | 10,000 | 1.0 | Θ(1) |
| LinkedList | get(random i) | 100 | 0.213 | 0.208 | 0.017 | 21.3 | accesses | 255,745 | 25.6 | Θ(min(i, n-i)) → Θ(n) avg |
| DynamicArray | get(random i) | 1,000 | 0.020 | 0.018 | 0.006 | 2.0 | accesses | 10,000 | 1.0 | Θ(1) |
| LinkedList | get(random i) | 1,000 | 2.347 | 2.344 | 0.023 | 234.7 | accesses | 2,497,236 | 249.7 | Θ(min(i, n-i)) → Θ(n) avg |
| DynamicArray | get(random i) | 10,000 | 0.034 | 0.034 | 0.001 | 3.4 | accesses | 10,000 | 1.0 | Θ(1) |
| LinkedList | get(random i) | 10,000 | 33.987 | 33.526 | 1.377 | 3398.7 | accesses | 25,125,062 | 2512.5 | Θ(min(i, n-i)) → Θ(n) avg |
| DynamicArray | get(random i) | 100,000 | 0.056 | 0.044 | 0.024 | 5.6 | accesses | 10,000 | 1.0 | Θ(1) |
| LinkedList | get(random i) | 100,000 | 410.491 | 406.177 | 22.644 | 41049.1 | accesses | 248,937,696 | 24893.8 | Θ(min(i, n-i)) → Θ(n) avg |
