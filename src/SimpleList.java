/**
 * Common interface of the two list implementations (DynamicArray and LinkedList).
 * Tests and benchmarks use it so both structures are exercised by exactly the same code.
 *
 * Instrumentation counters (reset with {@link #resetCounters()}):
 *   accesses    - number of elements / nodes touched to reach a position
 *   comparisons - number of element equality checks (contains)
 *   movements   - number of element copies (shifts and resize copies)
 */
public interface SimpleList<T> {
    void add(T x);
    void add(int index, T x);
    T remove(int index);
    T get(int index);
    boolean contains(T x);
    int size();

    default boolean isEmpty() {
        return size() == 0;
    }

    /** Returns the elements in order (used for validation, never inside a timed section). */
    Object[] toArray();

    long getAccesses();
    long getComparisons();
    long getMovements();
    void resetCounters();
}
