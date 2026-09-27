### Workload 3 - Insertion and removal (m = 1 000)

| Structure | Operation | n | Avg time (ms) | Median (ms) | Std (ms) | ns / op (avg) | Metric | Metric total | Metric / op | Theory (per op) |
|---|---|---:|---:|---:|---:|---:|---|---:|---:|---|
| DynamicArray | insert @ index 0 | 100 | 0.310 | 0.308 | 0.004 | 309.9 | movements | 601,420 | 601.4 | Θ(n) (shift all) |
| DynamicArray | remove @ index 0 | 100 | 0.325 | 0.325 | 0.000 | 325.2 | movements | 599,500 | 599.5 | Θ(n) (shift all) |
| LinkedList | insert @ index 0 | 100 | 0.027 | 0.027 | 0.000 | 27.0 | accesses | 1,000 | 1.0 | Θ(1) |
| LinkedList | remove @ index 0 | 100 | 0.050 | 0.044 | 0.010 | 49.8 | accesses | 1,000 | 1.0 | Θ(1) |
| DynamicArray | insert @ index n/2 | 100 | 0.287 | 0.286 | 0.002 | 286.7 | movements | 551,420 | 551.4 | Θ(n) (shift n/2) |
| DynamicArray | remove @ index n/2 | 100 | 0.304 | 0.303 | 0.002 | 303.9 | movements | 549,500 | 549.5 | Θ(n) (shift n/2) |
| LinkedList | insert @ index n/2 | 100 | 0.079 | 0.085 | 0.011 | 79.4 | accesses | 50,999 | 51.0 | Θ(n) (walk to n/2) |
| LinkedList | remove @ index n/2 | 100 | 0.104 | 0.110 | 0.015 | 103.7 | accesses | 51,000 | 51.0 | Θ(n) (walk to n/2) |
| DynamicArray | insert @ index 0 | 1,000 | 0.793 | 0.712 | 0.165 | 792.6 | movements | 1,500,524 | 1500.5 | Θ(n) (shift all) |
| DynamicArray | remove @ index 0 | 1,000 | 0.712 | 0.713 | 0.009 | 711.5 | movements | 1,499,500 | 1499.5 | Θ(n) (shift all) |
| LinkedList | insert @ index 0 | 1,000 | 0.010 | 0.009 | 0.002 | 9.8 | accesses | 1,000 | 1.0 | Θ(1) |
| LinkedList | remove @ index 0 | 1,000 | 0.010 | 0.010 | 0.001 | 10.0 | accesses | 1,000 | 1.0 | Θ(1) |
| DynamicArray | insert @ index n/2 | 1,000 | 0.555 | 0.500 | 0.114 | 555.4 | movements | 1,000,524 | 1000.5 | Θ(n) (shift n/2) |
| DynamicArray | remove @ index n/2 | 1,000 | 0.550 | 0.553 | 0.058 | 550.3 | movements | 999,500 | 999.5 | Θ(n) (shift n/2) |
| LinkedList | insert @ index n/2 | 1,000 | 0.540 | 0.484 | 0.091 | 540.3 | accesses | 500,999 | 501.0 | Θ(n) (walk to n/2) |
| LinkedList | remove @ index n/2 | 1,000 | 0.485 | 0.476 | 0.034 | 485.3 | accesses | 501,000 | 501.0 | Θ(n) (walk to n/2) |
| DynamicArray | insert @ index 0 | 10,000 | 5.876 | 5.091 | 1.105 | 5875.7 | movements | 10,499,500 | 10499.5 | Θ(n) (shift all) |
| DynamicArray | remove @ index 0 | 10,000 | 4.901 | 4.871 | 0.080 | 4900.7 | movements | 10,499,500 | 10499.5 | Θ(n) (shift all) |
| LinkedList | insert @ index 0 | 10,000 | 0.017 | 0.014 | 0.008 | 17.0 | accesses | 1,000 | 1.0 | Θ(1) |
| LinkedList | remove @ index 0 | 10,000 | 0.016 | 0.017 | 0.003 | 15.8 | accesses | 1,000 | 1.0 | Θ(1) |
| DynamicArray | insert @ index n/2 | 10,000 | 3.992 | 4.333 | 0.643 | 3991.7 | movements | 5,499,500 | 5499.5 | Θ(n) (shift n/2) |
| DynamicArray | remove @ index n/2 | 10,000 | 3.967 | 4.047 | 0.751 | 3967.1 | movements | 5,499,500 | 5499.5 | Θ(n) (shift n/2) |
| LinkedList | insert @ index n/2 | 10,000 | 7.020 | 7.070 | 0.179 | 7020.3 | accesses | 5,000,999 | 5001.0 | Θ(n) (walk to n/2) |
| LinkedList | remove @ index n/2 | 10,000 | 6.991 | 6.956 | 0.761 | 6991.4 | accesses | 5,001,000 | 5001.0 | Θ(n) (walk to n/2) |
| DynamicArray | insert @ index 0 | 100,000 | 69.074 | 75.014 | 9.555 | 69074.1 | movements | 100,499,500 | 100499.5 | Θ(n) (shift all) |
| DynamicArray | remove @ index 0 | 100,000 | 51.680 | 51.353 | 2.979 | 51679.6 | movements | 100,499,500 | 100499.5 | Θ(n) (shift all) |
| LinkedList | insert @ index 0 | 100,000 | 0.007 | 0.007 | 0.000 | 7.4 | accesses | 1,000 | 1.0 | Θ(1) |
| LinkedList | remove @ index 0 | 100,000 | 0.004 | 0.003 | 0.002 | 3.9 | accesses | 1,000 | 1.0 | Θ(1) |
| DynamicArray | insert @ index n/2 | 100,000 | 27.964 | 25.743 | 3.884 | 27964.3 | movements | 50,499,500 | 50499.5 | Θ(n) (shift n/2) |
| DynamicArray | remove @ index n/2 | 100,000 | 26.638 | 27.116 | 0.995 | 26637.8 | movements | 50,499,500 | 50499.5 | Θ(n) (shift n/2) |
| LinkedList | insert @ index n/2 | 100,000 | 67.827 | 67.506 | 1.618 | 67827.5 | accesses | 50,000,999 | 50001.0 | Θ(n) (walk to n/2) |
| LinkedList | remove @ index n/2 | 100,000 | 68.295 | 66.475 | 2.882 | 68294.9 | accesses | 50,001,000 | 50001.0 | Θ(n) (walk to n/2) |
