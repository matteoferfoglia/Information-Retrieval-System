package it.units.informationretrieval.ir_boolean_model.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skiplist.SkipList;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

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
    private final SkipList<Posting> postings;   // TODO: a built-in array may be faster but must be kept ordered


    /**
     * Constructor.
     * Creates a new instance of this class starting from the given {@link Posting}s.
     *
     * @param postings The list of {@link Posting}s.
     */
    public PostingList(@NotNull final Posting... postings) {
        this.postings = new SkipList<>();
        this.postings.addAll(Arrays.asList(postings));
    }

    /**
     * Merges the {@link PostingList} given as parameter into this one (destructive merging).
     * Only distinct {@link Posting}s are kept (see {@link Posting#equals(Object)}) and the
     * resulting {@link PostingList} (this one) is sorted.
     *
     * @param other The other {@link PostingList} to be merged into this one.
     */
    public void merge(@Nullable final PostingList other) {
        if (other == null) {
            return;
        }
        this.postings.merge(other.postings);
    }

    /**
     * @return This instance as unmodifiable {@link List} of {@link Posting}s.
     */
    @NotNull
    public List<Posting> toUnmodifiableListOfPostings() {
        return postings.stream().toList();
    }

    /**
     * @return The size of this {@link PostingList}.
     */
    public int size() {
        return postings.size();
    }

    @Override
    public String toString() {
        return postings.toString();
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
