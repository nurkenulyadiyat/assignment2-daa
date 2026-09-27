import java.util.Arrays;
import java.util.Objects;

/**
 * Resizable array list. Elements are stored contiguously in data[0..size-1].
 * Capacity doubles when full, which makes add(x) amortized Θ(1).
 */
public class DynamicArray<T> implements SimpleList<T> {
    private static final int DEFAULT_CAPACITY = 8;

    private Object[] data;
    private int size;

    private long accesses;
    private long comparisons;
    private long movements;

    public DynamicArray() {
        this(DEFAULT_CAPACITY);
    }

    public DynamicArray(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("Capacity must be positive: " + initialCapacity);
        }
        data = new Object[initialCapacity];
    }

    /** Appends x at the end. Amortized Θ(1), worst case Θ(n) when a resize happens. */
    @Override
    public void add(T x) {
        ensureCapacity(size + 1);
        data[size++] = x;
        accesses++;
    }

    /** Inserts x at position index (0 <= index <= size), shifting later elements right. Θ(n - index). */
    @Override
    public void add(int index, T x) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        ensureCapacity(size + 1);
        // Loop invariant (proved in README, section 3.1):
        // at the start of each iteration, data[i+1..size] holds the original A[i..size-1]
        // and data[0..i-1] still holds the original A[0..i-1].
        for (int i = size; i > index; i--) {
            data[i] = data[i - 1];
        }
        movements += size - index;
        data[index] = x;
        accesses++;
        size++;
    }

    /** Removes and returns the element at index, shifting later elements left. Θ(n - index). */
    @Override
    @SuppressWarnings("unchecked")
    public T remove(int index) {
        checkElementIndex(index);
        T removed = (T) data[index];
        for (int i = index; i < size - 1; i++) {
            data[i] = data[i + 1];
        }
        movements += size - 1 - index;
        data[--size] = null; // clear the stale reference so it can be garbage-collected
        accesses++;
        return removed;
    }

    /** Direct address computation: Θ(1). */
    @Override
    @SuppressWarnings("unchecked")
    public T get(int index) {
        checkElementIndex(index);
        accesses++;
        return (T) data[index];
    }

    /** Linear search from the front. Θ(1) best, Θ(n) worst. */
    @Override
    public boolean contains(T x) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(data[i], x)) {
                comparisons += i + 1;
                return true;
            }
        }
        comparisons += size;
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    public int capacity() {
        return data.length;
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(data, size);
    }

    private void ensureCapacity(int minCapacity) {
        if (minCapacity > data.length) {
            int newCapacity = Math.max(data.length * 2, minCapacity);
            data = Arrays.copyOf(data, newCapacity);
            movements += size; // every existing element is copied once
        }
    }

    private void checkElementIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
    }

    @Override public long getAccesses()    { return accesses; }
    @Override public long getComparisons() { return comparisons; }
    @Override public long getMovements()   { return movements; }

    @Override
    public void resetCounters() {
        accesses = comparisons = movements = 0;
    }

    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
}
