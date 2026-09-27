import java.util.Arrays;
import java.util.Objects;

/**
 * Doubly linked list with head and tail references.
 * Positional operations walk from whichever end is closer to the index,
 * so reaching position i touches min(i, size-1-i) + 1 nodes.
 */
public class LinkedList<T> implements SimpleList<T> {

    private static final class Node<T> {
        T value;
        Node<T> prev;
        Node<T> next;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;

    private long accesses;
    private long comparisons;
    private long movements; // always 0: a linked list never moves elements, it relinks nodes

    /** Appends at the tail: Θ(1). */
    @Override
    public void add(T x) {
        linkLast(x);
    }

    /** Inserts at index (0 <= index <= size). Θ(1) at either end, Θ(min(i, n-i)) otherwise. */
    @Override
    public void add(int index, T x) {
        if (index < 0 || index > size) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        }
        if (index == size) {
            linkLast(x);
        } else if (index == 0) {
            linkFirst(x);
        } else {
            linkBefore(x, node(index));
        }
    }

    /** Removes the node at index: locate Θ(min(i, n-i)) + unlink Θ(1). */
    @Override
    public T remove(int index) {
        checkElementIndex(index);
        return unlink(node(index));
    }

    /** Θ(min(i, n-i)): must follow pointers node by node. */
    @Override
    public T get(int index) {
        checkElementIndex(index);
        return node(index).value;
    }

    /** Linear search from the head. Θ(1) best, Θ(n) worst. */
    @Override
    public boolean contains(T x) {
        long checked = 0;
        for (Node<T> cur = head; cur != null; cur = cur.next) {
            checked++;
            if (Objects.equals(cur.value, x)) {
                comparisons += checked;
                return true;
            }
        }
        comparisons += checked;
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Object[] toArray() {
        Object[] out = new Object[size];
        int i = 0;
        for (Node<T> cur = head; cur != null; cur = cur.next) {
            out[i++] = cur.value;
        }
        return out;
    }

    /** Returns the node at index, walking from the nearer end. Counts every node touched. */
    private Node<T> node(int index) {
        Node<T> cur;
        if (index < (size >> 1)) {
            cur = head;
            for (int i = 0; i < index; i++) {
                cur = cur.next;
            }
            accesses += index + 1;
        } else {
            cur = tail;
            for (int i = size - 1; i > index; i--) {
                cur = cur.prev;
            }
            accesses += size - index;
        }
        return cur;
    }

    private void linkFirst(T x) {
        Node<T> n = new Node<>(x);
        n.next = head;
        if (head == null) {
            tail = n;
        } else {
            head.prev = n;
        }
        head = n;
        size++;
        accesses++;
    }

    private void linkLast(T x) {
        Node<T> n = new Node<>(x);
        n.prev = tail;
        if (tail == null) {
            head = n;
        } else {
            tail.next = n;
        }
        tail = n;
        size++;
        accesses++;
    }

    private void linkBefore(T x, Node<T> succ) {
        Node<T> pred = succ.prev;
        Node<T> n = new Node<>(x);
        n.prev = pred;
        n.next = succ;
        succ.prev = n;
        if (pred == null) {
            head = n;
        } else {
            pred.next = n;
        }
        size++;
    }

    private T unlink(Node<T> n) {
        Node<T> pred = n.prev;
        Node<T> succ = n.next;
        if (pred == null) {
            head = succ;
        } else {
            pred.next = succ;
        }
        if (succ == null) {
            tail = pred;
        } else {
            succ.prev = pred;
        }
        T value = n.value;
        n.value = null;
        n.prev = null;
        n.next = null;
        size--;
        return value;
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
