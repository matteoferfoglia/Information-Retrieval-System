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
    private final String term;

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
    public void merge(@NotNull Term other) {
        throw new UnsupportedOperationException("Not implemented yet.");    // TODO : not implemented yet
    }

    @Override
    public int compareTo(Term term) {
        return this.term.compareTo(term.term);
    }

    /** This exception is thrown when no documents associated with
     * the {@link Term} are found.*/
    public static class NoDocumentsAssociatedWithTerm extends Exception {
        public NoDocumentsAssociatedWithTerm(String msg) {
            super(msg);
        }
    }
}