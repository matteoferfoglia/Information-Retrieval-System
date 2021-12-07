package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.entities.document.Document;
import org.jetbrains.annotations.NotNull;

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
    private final String term;  // TODO : needed? In InvertedIndex there is the term as key for the list of terms

    /**
     * Constructor.
     *
     * @param posting The {@link Posting} to be used to construct the
     *                new instance of {@link Term}.
     * @param term    The actual term for this instance.
     */
    public Term(@NotNull final Posting posting, @NotNull final String term) {
        this.postingList = new PostingList(Objects.requireNonNull(posting));
        this.term = Objects.requireNonNull(term);
    }

    /**
     * Returns the Inverse-Document-Frequency for this {@link Term}.
     *
     * @param numberOfDocsInCorpus The total number of {@link Document}s in the {@link Corpus}.
     * @return the Inverse-Document-Frequency for this {@link Term}.
     * @throws NoDocumentsAssociatedWithTermException If no documents associated with
     *                                                this {@link Term} are found.
     */
    public double idf(int numberOfDocsInCorpus) throws NoDocumentsAssociatedWithTermException {// TODO: test and benchmark
        int df = postingList.size();    // document frequency   // TODO : df ma be an attribute of the class (faster, but occupies memory spacer and must be eventually updated)
        if (df == 0) {
            throw new NoDocumentsAssociatedWithTermException(this);
        }
        return Math.log((double) numberOfDocsInCorpus / df);   // TODO : may be cached until any updated, as attribute
    }

    /**
     * Merges a {@link Term} into this one (destructive merging).
     *
     * @param other The other {@link Term} to be merged into this one.
     */
    public void merge(@NotNull final Term other) {    // TODO : needed? // TODO: test and benchmark
        if (this.term.equals(other.term)) {
            this.postingList.merge(other.postingList);
        } else {
            throw new ImpossibleTermsMergingException(this, other);
        }
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

    /**
     * This exception is thrown when no documents associated with
     * the {@link Term} are found.
     */
    public static class NoDocumentsAssociatedWithTermException extends Exception {
        public NoDocumentsAssociatedWithTermException(@NotNull final Term term) {
            super("No documents associated with the term " + Objects.requireNonNull(term));
        }
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