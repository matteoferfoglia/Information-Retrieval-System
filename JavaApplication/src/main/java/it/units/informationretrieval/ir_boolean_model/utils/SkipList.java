package it.units.informationretrieval.ir_boolean_model.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of a skip list.
 *
 * @author Matteo Ferfoglia
 */
public class SkipList<T extends Comparable<T>> {

    /**
     * The list.
     */
    @NotNull
    private final List<T> list;

    /**
     * The forward pointers associated with the list.
     * Each element contains the position to (forward pointer)
     * the forwarded element in {@link #list}.
     */
    private final List<Integer> forwardPointers;

    /**
     * Creates an instance of this class starting from a {@link List}.
     *
     * @param list The {@link List} from which this instance is created.
     */
    public SkipList(@NotNull final List<T> list) {
        this.list = Objects.requireNonNull(list);
        this.forwardPointers = new ArrayList<>(Collections.nCopies(list.size(), null));    // list of forward pointers must have the same size as the list, but they are un-set as default
        setForwardPointers();
    }

    /**
     * Default constructor.
     */
    public SkipList() {
        this(new ArrayList<>());
    }

    /**
     * Sort and remove duplicates from this instance.
     */
    private void sortAndRemoveDuplicates() {
        Utility.sortAndRemoveDuplicates(list, forwardPointers);
    }

    /**
     * @return The size of {@link #list}.
     */
    public int size() {
        return list.size();
    }

    /**
     * @param index The index of the desired element in this instance.
     * @return true if the desired element has a forward pointer, false otherwise.
     * @throws IndexOutOfBoundsException – if the index is out of range (index < 0 || index >= size())
     */
    public boolean hasForwardPointer(int index) {
        return forwardPointers.get(index) != null;
    }

    /**
     * @param index The index of the desired element in this instance.
     * @return the index of the forwarded element.
     * @throws IndexOutOfBoundsException – if the index is out of range (index < 0 || index >= size())
     */
    public int getForwardPointer(int index) {   // TODO: test
        return forwardPointers.get(index);
    }

    /**
     * @param index The index of the desired element in this instance.
     * @return the element at the given index.
     * @throws IndexOutOfBoundsException – if the index is out of range (index < 0 || index >= size())
     */
    @NotNull
    public T get(int index) {
        return list.get(index);
    }

    /**
     * Sets the forward pointers.
     * If forward pointers were already set, the invocation of this method
     * will re-set them (e.g., if the list has changed).
     * &radic;p evenly spaced forward pointers will be created, with p = the
     * number of elements in this instance.
     * Rule: for a skipList of P elements, use F = floor(sqrt(P)) evenly spaced
     * (with space S = floor(P/F) ) forward pointers.
     */
    private void setForwardPointers() {

        sortAndRemoveDuplicates();   // the list MUST be sorted
        final int numberOfForwardPointers = (int) Math.ceil(Math.sqrt(size()));

        if (numberOfForwardPointers > 0) {

            int previousForwardPointerIndex = list.size() - 1/*last element*/;

            // Set the forwardPointers
            for (int i = list.size() - 2/*last element is never a forward pointer*/; i >= 0; i--) {
                if (i % numberOfForwardPointers == 0) {
                    forwardPointers.set(i, previousForwardPointerIndex);
                    previousForwardPointerIndex = i;
                } else {
                    forwardPointers.set(i, null);
                }
            }
        }

    }

    /**
     * Add all elements of the input instance to this one.
     *
     * @param list The input instance.
     */
    public void addAll(@NotNull final SkipList<T> list) {
        addAll(Objects.requireNonNull(list.list));
    }

    /**
     * Add all elements of the input instance to this one.
     *
     * @param list The input instance.
     */
    public void addAll(@NotNull final List<T> list) {
        if (list.size() > 0) {
            this.list.addAll(Objects.requireNonNull(list));
            this.forwardPointers.addAll(Collections.nCopies(list.size(), null));    // list of forward pointers must have the same size as the list, but they are un-set as default
            setForwardPointers();
        }
    }

    /**
     * Add the given element to this instance.
     * <strong>Recommended</strong> to use {@link #addAll(SkipList)}
     * or {@link #addAll(List)} to add a list of elements together
     * instead of only one for performance reasons (forward pointers
     * must be recomputed every time).
     *
     * @param element The element to add.
     */
    public void add(@NotNull final T element) {
        addAll(List.of(Objects.requireNonNull(element)));
    }

    /**
     * @return an unmodifiable {@link List} containing the element of this instance.
     */
    @Unmodifiable
    public List<T> toUnmodifiableList() {
        return Collections.unmodifiableList(list);
    }

    @Override
    public String toString() {
        return list.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SkipList<?> skipList = (SkipList<?>) o;
        return list.equals(skipList.list);
    }

    @Override
    public int hashCode() {
        return list.hashCode();
    }
}
