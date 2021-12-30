package it.units.informationretrieval.ir_boolean_model.entities;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

/**
 * Class representing a Posting.
 *
 * @author Matteo Ferfoglia.
 */
public class Posting implements Comparable<Posting>, Serializable {

    /**
     * Compare postings considering only their {@link DocumentIdentifier}s.
     */
    @NotNull
    public static final Comparator<Posting> DOC_ID_COMPARATOR = Comparator.comparing(o -> o.docId);

    /**
     * The {@link DocumentIdentifier} of the document associated with this posting.
     */
    @NotNull
    private final DocumentIdentifier docId;
    /**
     * The {@link java.time.Instant} at which this instance has been created.
     */
    @NotNull
    private final Instant creationInstant;
    /**
     * Array of positions in the document where the term appears.
     */
    private final int[] termPositionsInTheDocument;

    /**
     * Constructor. Given a {@link DocumentIdentifier}, creates a new instance of this class.
     *
     * @param docId     The {@link DocumentIdentifier}.
     * @param positions The positions where the {@link Term} to which this instance refers
     *                  in the document to which this instance refers.
     */
    //* @param positions The {@link TermPositionsInADocument} object (see the description of the class). */
    public Posting(@NotNull DocumentIdentifier docId, final int[] positions) {
        this.docId = Objects.requireNonNull(docId, "The docId cannot be null");
        creationInstant = Instant.now();
        termPositionsInTheDocument = Objects.requireNonNull(positions);
    }

    /**
     * @return the number of occurrences of the {@link Term} associated with this
     * {@link Posting} (i.e., the term-frequency value).
     */
    public int tf() {
        return termPositionsInTheDocument.length;
    }

    /**
     * Getter for {@link #docId}.
     *
     * @return the {@link #docId} associated with this instance.
     */
    public @NotNull DocumentIdentifier getDocId() {
        return docId;
    }

    /**
     * @return {@link #termPositionsInTheDocument}.
     */
    public int[] getTermPositionsInTheDocument() {
        return termPositionsInTheDocument;
    }

    /**
     * Compares {@link Posting}s according to their {@link DocumentIdentifier}
     * (see {@link DocumentIdentifier#compareTo(DocumentIdentifier)}).
     * If the instances have equal {@link DocumentIdentifier}s, then the
     * comparison is done according to {@link #termPositionsInTheDocument}.
     *
     * @param o The posting to be compared with this one.
     * @return the result of the comparison of their {@link DocumentIdentifier}s.
     */
    @Override
    public int compareTo(@NotNull Posting o) {
        var docIdComparison = this.docId.compareTo(o.docId);
        if (docIdComparison == 0) {  // same docId
            return Arrays.compare(this.termPositionsInTheDocument, o.termPositionsInTheDocument);
        }
        return docIdComparison;
    }

    /**
     * Compares the {@link #creationInstant} of this instance with the {@link #creationInstant}
     * of the one given as parameter.
     *
     * @param posting The other instance.
     * @return an integer number which is:
     * <ul>
     *     <li> &lt; 0 if this instance is older than the other one;</li>
     *     <li> = 0 if instances have the same age;</li>
     *     <li> &gt; 0 otherwise.</li>
     * </ul>
     */
    public int compareCreationTimeTo(Posting posting) {
        return this.creationInstant.compareTo(posting.creationInstant);
    }

    @Override
    public String toString() {
        return "Posting{" +
                "docId=" + docId +
                ", creationInstant=" + creationInstant +
                ", termPositionsInTheDocument=" + Arrays.toString(termPositionsInTheDocument) +
                '}';
    }

    /**
     * @return true if the instances have the same {@link #docId}
     * and the same {@link #termPositionsInTheDocument}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Posting posting = (Posting) o;
        if (!docId.equals(posting.docId)) return false;
        return Arrays.equals(termPositionsInTheDocument, posting.termPositionsInTheDocument);
    }

    @Override
    public int hashCode() {
        return Objects.hash(docId, Arrays.hashCode(termPositionsInTheDocument));
    }

}