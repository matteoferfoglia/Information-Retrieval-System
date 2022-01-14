package it.units.informationretrieval.ir_boolean_model.entities;

import org.jetbrains.annotations.NotNull;
import skiplist.SkipList;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents a Term.
 *
 * @author Matteo Ferfoglia
 */
public class Term implements Comparable<Term>, Serializable {

    /**
     * The {@link PostingList} associated with this {@link Term}.
     */
    @NotNull
    private final PostingList postingList;

    /**
     * The term associated with this instance.
     */
    @NotNull
    private final String term;

    /**
     * Constructor.
     *
     * @param postingList The {@link PostingList} to be used to construct the
     *                    new instance of {@link Term}.
     * @param term        The actual term for this instance.
     */
    public Term(@NotNull final PostingList postingList, @NotNull final String term) {
        this.postingList = Objects.requireNonNull(postingList);
        this.term = Objects.requireNonNull(term);
        this.postingList.setTermToPosting(this);
    }

    /**
     * @return the Document-Frequency for this {@link Term}.
     */
    public double df() {
        return postingList.size();
    }

    /**
     * Returns the Inverse-Document-Frequency for this {@link Term}.
     *
     * @param numberOfDocsInCorpus The total number of documents in the Corpus.
     * @return the Inverse-Document-Frequency for this {@link Term}.
     */
    public double idf(int numberOfDocsInCorpus) {
        assert df() > 0;
        return Math.log((double) numberOfDocsInCorpus / df());
    }

    /**
     * @return the total number of occurrences of this {@link Term} in the entire {@link Corpus}.
     */
    public int totalNumberOfOccurrencesInCorpus() {
        return postingList.getSkipList()
                .stream().unordered().parallel()
                .mapToInt(Posting::tf)
                .sum();
    }

    /**
     * Merges a {@link Term} into this one (destructive merging).
     *
     * @param other The other {@link Term} to be merged into this one.
     * @return this instance after merging.
     */
    public Term merge(@NotNull final Term other) {
        if (this.term.equals(other.term)) {
            other.postingList.setTermToPosting(this);
            this.postingList.merge(other.postingList);
        } else {
            throw new ImpossibleTermsMergingException(this, other);
        }
        return this;
    }

    /**
     * @return the {@link PostingList} associated with this instance.
     */
    @NotNull
    public PostingList getPostingList() {
        return postingList;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Term term1 = (Term) o;

        if (!postingList.equals(term1.postingList)) return false;
        return term.equals(term1.term);
    }

    @Override
    public int hashCode() {
        return term.hashCode();
    }

    @Override
    public int compareTo(Term term) {
        return this.term.compareTo(term.term);
    }

    @Override
    public String toString() {
        return "{" + term + ": " + postingList + "}";
    }

    @NotNull
    public SkipList<Posting> getListOfPostings() {
        return postingList.getSkipList();
    }

    /**
     * @return the term (as {@link String}) of this instance.
     */
    @NotNull
    public String getTermString() {
        return term;
    }

    /**
     * Exception thrown if merging terms is impossible.
     */
    public static class ImpossibleTermsMergingException extends RuntimeException {
        public ImpossibleTermsMergingException(@NotNull final Term term1, @NotNull final Term term2) {
            super("Terms " + Objects.requireNonNull(term1) + " and " +
                    Objects.requireNonNull(term2) + " cannot be merged.");
        }
    }
}