### Workload 2 - Search (m = 1 000 contains)

| Structure | Operation | n | Avg time (ms) | Median (ms) | Std (ms) | ns / op (avg) | Metric | Metric total | Metric / op | Theory (per op) |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|---|
| DynamicArray | contains(key) | 100 | 0.287 | 0.262 | 0.054 | 287.5 | comparisons | 73,592 | 73.6 | Θ(1) best, Θ(n) avg/worst |
| LinkedList | contains(key) | 100 | 0.269 | 0.267 | 0.023 | 268.7 | comparisons | 73,592 | 73.6 | Θ(1) best, Θ(n) avg/worst |
| DynamicArray | contains(key) | 1,000 | 2.628 | 2.567 | 0.207 | 2627.5 | comparisons | 742,593 | 742.6 | Θ(1) best, Θ(n) avg/worst |
| LinkedList | contains(key) | 1,000 | 2.528 | 2.514 | 0.353 | 2527.7 | comparisons | 742,593 | 742.6 | Θ(1) best, Θ(n) avg/worst |
| DynamicArray | contains(key) | 10,000 | 32.802 | 35.155 | 5.470 | 32801.8 | comparisons | 7,599,563 | 7599.6 | Θ(1) best, Θ(n) avg/worst |
| LinkedList | contains(key) | 10,000 | 24.320 | 23.024 | 3.132 | 24319.9 | comparisons | 7,599,563 | 7599.6 | Θ(1) best, Θ(n) avg/worst |
| DynamicArray | contains(key) | 100,000 | 243.421 | 236.600 | 10.973 | 243420.6 | comparisons | 76,330,683 | 76330.7 | Θ(1) best, Θ(n) avg/worst |
| LinkedList | contains(key) | 100,000 | 230.850 | 232.072 | 10.385 | 230849.9 | comparisons | 76,330,683 | 76330.7 | Θ(1) best, Θ(n) avg/worst |
