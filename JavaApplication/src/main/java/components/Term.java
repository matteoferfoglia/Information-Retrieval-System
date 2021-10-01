package components;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** This class represents a Term.
 *
 * @author Matteo Ferfoglia*/
public class Term implements Comparable<Term> {

    /** The {@link PostingList} associated with this {@link Term}. */
    @NotNull
    private final PostingList postingList;

    /** The term associated with this instance.*/
    @NotNull
    private final String term;  // TODO : needed? In InvertedIndex there is the term as key for the list of terms

    /** Constructor.
     * @param posting The {@link Posting} to be used to construct the
     *                new instance of {@link Term}.
     * @param term The actual term for this instance. */
    public Term(@NotNull Posting posting, @NotNull String term) {
        this.postingList = new PostingList(Objects.requireNonNull(posting, "The input posting cannot be null."));
        this.term = Objects.requireNonNull(term, "The input term cannot be null.");
    }

    /** Returns the Inverse-Document-Frequency for this {@link Term}.
     * @param numberOfDocsInCorpus The total number of {@link Document}s in the {@link Corpus}.
     * @return the Inverse-Document-Frequency for this {@link Term}.
     * @throws NoDocumentsAssociatedWithTerm If no documents associated with
     *          this {@link Term} are found.*/
    public double idf(int numberOfDocsInCorpus) throws Exception {
        int df = postingList.size();    // document frequency   // TODO : df ma be an attribute of the class (faster, but occupies memory spacer and must be eventually updated)
        if( df == 0 ) {
            throw new NoDocumentsAssociatedWithTerm("No documents associated with this term");
        }
        return Math.log( (double)numberOfDocsInCorpus / df );   // TODO : may be cached until any updated, as attribute
    }

    /** Merges a {@link Term} into this one (destructive merging).
     * @param other The other {@link Term} to be merged into this one.
     */
    public void merge(@NotNull Term other) {    // TODO : needed?
        if ( this.equals(other) ) {
            this.postingList.merge(other.postingList);
        } else {
            throw new ImpossibleMergeException("Terms " + this + " and " + other + " cannot be merged.");
        }
    }

    /** @return the {@link PostingList} associated with this instance. */
    @NotNull
    public PostingList getPostingList() {
        return postingList;
    }

    /** @return the term associated with this instance. */
    @NotNull
    public String getTerm() {
        return term;
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
        int result = postingList.hashCode();
        result = 31 * result + term.hashCode();
        return result;
    }

    @Override
    public int compareTo(Term term) {
        return this.term.compareTo(term.term);
    }

    @Override
    public String toString() {
        return term + ": " + postingList;
    }

    /** This exception is thrown when no documents associated with
     * the {@link Term} are found.*/
    public static class NoDocumentsAssociatedWithTerm extends Exception {
        public NoDocumentsAssociatedWithTerm(String msg) {
            super(msg);
        }
    }

    /** Exception thrown if merging terms is impossible. */
    public static class ImpossibleMergeException extends RuntimeException {
        public ImpossibleMergeException(String errorMessage) {
            super(errorMessage);
        }
    }
}