import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * Array-based binary min-heap.
 * For a node at index i: parent = (i-1)/2, children = 2i+1 and 2i+2.
 * Heap property: heap[parent(i)] <= heap[i] for every i > 0, so the minimum is always heap[0].
 */
public class MinHeap<T extends Comparable<? super T>> {
    private static final int DEFAULT_CAPACITY = 16;

    private Object[] heap;
    private int size;

    private long comparisons;
    private long swaps;

    public MinHeap() {
        heap = new Object[DEFAULT_CAPACITY];
    }

    /** Adds x and restores the heap property by sifting it up. Θ(1) best, O(log n) worst. */
    public void insert(T x) {
        if (x == null) {
            throw new NullPointerException("MinHeap does not accept null");
        }
        if (size == heap.length) {
            heap = Arrays.copyOf(heap, heap.length * 2);
        }
        heap[size] = x;
        size++;
        siftUp(size - 1);
    }

    /** Returns the minimum without removing it. Θ(1). */
    public T peekMin() {
        if (size == 0) {
            throw new NoSuchElementException("Heap is empty");
        }
        return elementAt(0);
    }

    /** Removes and returns the minimum. Θ(log n) in the average and worst case. */
    public T extractMin() {
        if (size == 0) {
            throw new NoSuchElementException("Heap is empty");
        }
        T min = elementAt(0);
        size--;
        heap[0] = heap[size];   // move the last leaf to the root
        heap[size] = null;
        if (size > 0) {
            siftDown(0);
        }
        return min;
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    /** Checks the heap property on every parent-child pair. Used by tests only (not counted). */
    public boolean isValidHeap() {
        for (int i = 1; i < size; i++) {
            if (elementAt((i - 1) / 2).compareTo(elementAt(i)) > 0) {
                return false;
            }
        }
        return true;
    }

    private void siftUp(int i) {
        while (i > 0) {
            int parent = (i - 1) >>> 1;
            comparisons++;
            if (elementAt(i).compareTo(elementAt(parent)) >= 0) {
                break;          // parent <= child: heap property holds on the whole path
            }
            swap(i, parent);
            i = parent;
        }
    }

    /**
     * Loop invariant (proved in README, section 3.2): every parent-child pair (j, c) with j != i
     * satisfies heap[j] <= heap[c], and if i has a parent p, then heap[p] <= every child of i.
     */
    private void siftDown(int i) {
        while (true) {
            int left = 2 * i + 1;
            if (left >= size) {
                break;          // i is a leaf
            }
            int right = left + 1;
            int smallest = left;
            if (right < size) {
                comparisons++;
                if (elementAt(right).compareTo(elementAt(left)) < 0) {
                    smallest = right;
                }
            }
            comparisons++;
            if (elementAt(i).compareTo(elementAt(smallest)) <= 0) {
                break;          // i is not larger than its smaller child
            }
            swap(i, smallest);
            i = smallest;
        }
    }

    private void swap(int a, int b) {
        Object tmp = heap[a];
        heap[a] = heap[b];
        heap[b] = tmp;
        swaps++;
    }

    @SuppressWarnings("unchecked")
    private T elementAt(int i) {
        return (T) heap[i];
    }

    public long getComparisons() { return comparisons; }
    public long getSwaps()       { return swaps; }

    public void resetCounters() {
        comparisons = 0;
        swaps = 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(Arrays.copyOf(heap, size));
    }
}
