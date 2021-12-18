package it.units.informationretrieval.ir_boolean_model.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * Implementation of a skip list.
 * Note: from the point of view of the design of the classes,
 * it would be better for this class to control the position
 * of forward pointers and make this class to implement
 * List&lt;T&gt; (making the elements of the list completing
 * unaware about the existence of forward pointers), but this
 * had a very bad impact on the performance of the system,
 * hence the final choice was to use the interface {@link SkipListElement}
 * to describe each element of this {@link SkipList} and the
 * interface {@link SkipListElement} defines how forward
 * pointers must be handled.
 *
 * @author Matteo Ferfoglia
 */
public class SkipList<T extends Comparable<T>> implements Serializable {    // TODO: benchmark  // TODO: inverted index creation is very very slow with this instance // TODO: some query do not work

    /**
     * The list.
     */
    @NotNull
    private List<SkipListElement<T>> list;

    /**
     * Creates an instance of this class starting from a {@link List}.
     *
     * @param elements The elements from which this instance is created.
     */
    public SkipList(@NotNull List<SkipListElement<T>> elements) {
        this.list = elements;
        setForwardPointers();
    }

    /**
     * Sort and remove duplicates from this instance.
     */
    private void sortAndRemoveDuplicates() {
        list = Utility.sortAndRemoveDuplicates(list);
    }

    /**
     * @return The size of {@link #list}.
     */
    public int size() {
        return list.size();
    }

    /**
     * @param index The index of the desired element in this instance.
     * @return the element at the given index.
     * @throws IndexOutOfBoundsException – if the index is out of range (index < 0 || index >= size())
     */
    @NotNull
    public SkipListElement<T> get(int index) {
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
            SkipListElement<T> previousForwardPointer = list.get(previousForwardPointerIndex);

            // Set the forwardPointers
            for (int i = list.size() - 2/*last element is never a forward pointer*/; i >= 0; i--) {
                if (i % numberOfForwardPointers == 0) {
                    previousForwardPointer = list.get(i).setForwardPointer(previousForwardPointerIndex, previousForwardPointer);
                    previousForwardPointerIndex = i;
                } else {
                    list.get(i).setForwardPointer(-1, null);
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
    public void addAll(@NotNull final List<SkipListElement<T>> list) {
        if (list.size() > 0) {
            this.list.addAll(Objects.requireNonNull(list));
            setForwardPointers();
        }
    }

    /**
     * Add the given element to this instance.
     * <strong>Recommended</strong> to use {@link #addAll(List)} o add
     * a list of elements together instead of only one for performance
     * reasons (forward pointers must be recomputed every time).
     *
     * @param element The element to add.
     */
    public void add(@NotNull final SkipListElement<T> element) {
        addAll(List.of(Objects.requireNonNull(element)));
    }

    /**
     * @return an unmodifiable {@link List} containing the element of this instance.
     */
    @Unmodifiable
    public List<T> toUnmodifiableList() {
        return list.stream().map(SkipListElement::getElement).toList();
    }

    /**
     * @return The {@link List} of elements present in this instance.
     */
    @NotNull
    public List<SkipListElement<T>> getList() {
        return list;
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
