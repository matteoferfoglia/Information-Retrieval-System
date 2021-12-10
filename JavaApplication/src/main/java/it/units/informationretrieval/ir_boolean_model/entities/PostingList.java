package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Class representing a {@link PostingList}.
 *
 * @author Matteo Ferfoglia
 */
public class PostingList implements Serializable/* TODO: implements Iterable with spliterator */ {

    /**
     * The actual posting list.
     */
    @NotNull
    private List<Posting> postings;   // TODO: a built-in array may be faster but must be kept ordered

    /**
     * Constructor. Creates an empty {@link PostingList}.
     */
    public PostingList() {
        this(new ArrayList<>(0));
    }

    /**
     * Constructor.
     * Creates a new instance of this class starting from a {@link Posting}.
     *
     * @param posting The {@link Posting} used to create the new {@link PostingList}.
     *                Immediately after the creation, this will be the only-one
     *                element in the list.
     */
    public PostingList(@NotNull final Posting posting) {
        this(new ArrayList<>() {{
            add(Objects.requireNonNull(posting));
        }});
    }

    /**
     * Constructor.
     * Creates a new instance of this class starting from a {@link List} of {@link Posting}s.
     *
     * @param postings The list of {@link Posting}s.
     */
    public PostingList(@NotNull final List<Posting> postings) {
        this.postings = Objects.requireNonNull(postings);
        sortAndRemoveDuplicates();
        setForwardPointers();
    }

    /**
     * Merges the {@link PostingList} given as parameter into this one (destructive merging).
     * Only distinct {@link Posting}s are kept (see {@link Posting#equals(Object)} and the
     * resulting {@link PostingList} (this one) is sorted.
     *
     * @param other The other {@link PostingList} to be merged into this one.
     */
    public void merge(@Nullable final PostingList other) {
        if (other == null) {
            return;
        }
        this.postings.addAll(other.postings);
        sortAndRemoveDuplicates();
    }

    /**
     * Sets the forward pointers for this instance of {@link PostingList}.
     * &radic;p evenly spaced forward pointers will be created, with p = the
     * number of elements in this {@link PostingList}.
     */
    private void setForwardPointers() {

        sortAndRemoveDuplicates();   // the postingList MUST be sorted

        final int numberOfPostings = postings.size();
        final int numberOfForwardPointers = (int) Math.ceil(Math.sqrt(numberOfPostings));

        if (numberOfForwardPointers > 0) {

            Posting previousForwardPointer = postings.get(postings.size() - 1/*last posting*/);

            // Set the forwardPointers
            for (int i = postings.size() - 2/*last posting is never a forward pointer*/; i >= 0; i--) {
                var posting = postings.get(i);
                if (i % numberOfForwardPointers == 0) {
                    previousForwardPointer = posting.setForwardPointer(previousForwardPointer);
                } else {
                    posting.setForwardPointer(null);
                }
            }
        }

    }

    /**
     * @return This instance as unmodifiable {@link List} of {@link Posting}s.
     */
    @NotNull
    public List<Posting> toListOfPostings() {
        return Collections.unmodifiableList(postings);
    }

    /**
     * Sort this instance.
     */
    private void sortAndRemoveDuplicates() {
        postings = Utility.sortAndRemoveDuplicates(postings);
    }

    /**
     * @return The size of this {@link PostingList}.
     */
    public int size() {
        return postings.size();
    }

    @Override
    public String toString() {
        return Arrays.toString(postings.toArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostingList that = (PostingList) o;

        return postings.equals(that.postings);
    }

    @Override
    public int hashCode() {
        return postings.hashCode();
    }
}
