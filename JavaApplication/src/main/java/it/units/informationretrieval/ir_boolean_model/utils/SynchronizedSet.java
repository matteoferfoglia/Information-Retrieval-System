package it.units.informationretrieval.ir_boolean_model.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the interface {@link Set} and provides
 * a thread-safe Set class.
 *
 * @author Matteo Ferfoglia
 */
public class SynchronizedSet<T> implements Set<T> {

    /**
     * The synchronized set.
     */
    @NotNull
    private final Set<T> set;

    /**
     * Constructor.
     *
     * @param initialSize The initial size.
     * @throws IllegalArgumentException If invalid initial size.
     */
    public SynchronizedSet(int initialSize) {
        if (initialSize < 0) {
            throw new IllegalArgumentException("Initial size must be >0 but found " + initialSize);
        }
        set = ConcurrentHashMap.newKeySet();
    }

    /**
     * Constructor.
     */
    public SynchronizedSet() {
        set = ConcurrentHashMap.newKeySet();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.contains(o);
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return set.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return set.toArray();
    }

    @NotNull
    @Override
    public <T1> T1[] toArray(@NotNull T1[] a) {
        return set.toArray(a);
    }

    @Override
    public boolean add(T t) {
        return set.add(t);
    }

    @Override
    public boolean remove(Object o) {
        return set.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return set.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return set.addAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return set.retainAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return set.removeAll(c);
    }

    @Override
    public void clear() {
        set.clear();
    }
}
