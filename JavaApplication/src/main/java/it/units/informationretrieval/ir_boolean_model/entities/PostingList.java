package it.units.informationretrieval.ir_boolean_model.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skiplist.SkipList;

import java.io.Serializable;
import java.util.Arrays;

/**
 * Class representing a {@link PostingList}.
 *
 * @author Matteo Ferfoglia
 */
public class PostingList implements Serializable {

    /**
     * The actual posting list.
     */
    @NotNull
    private final SkipList<Posting> postings;

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
     * Sets to all {@link Posting}s belonging to this instance the
     * {@link Term} given as parameter.
     *
     * @param term The term to set.
     */
    public void setTermToPosting(@NotNull final Term term) {
        postings.forEach(posting -> posting.setTerm(term));
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
        this.postings.addAll(other.postings);
        this.postings.setMaxListLevel();
    }

    /**
     * @return This {@link SkipList} of {@link Posting}s contained in this instance.
     */
    @NotNull
    public SkipList<Posting> getSkipList() {
        return postings;
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
