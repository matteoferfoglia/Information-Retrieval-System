package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.skiplist.SkipListElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Class representing a Posting.
 *
 * @author Matteo Ferfoglia.
 */
public class Posting implements SkipListElement<Posting> {

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
     * The forward pointer.
     */
    @Nullable
    private SkipListElement<Posting> forwardPointer;

    /**
     * The index in the {@link PostingList} of the element
     * forwarded by this instance.
     */
    private int forwardedElementIndex;


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

//    /**
//     * Returns the {@link List} of {@link Posting} which is the union of the
//     * two {@link PostingList}s given as parameters.
//     *
//     * @param a A {@link PostingList}.
//     * @param b The other {@link PostingList}.
//     * @return the {@link List} of {@link Posting} which is the union of the
//     * two given {@link PostingList}s as parameters.
//     */
//    @NotNull
//    public static List<Posting> union(@NotNull final List<Posting> a, @NotNull final List<Posting> b) {
//        // TODO : positional union not implemented yet
//    }

//    /**
//     * Returns Returns the {@link List} of {@link Posting} which is the intersection of the
//     * two {@link PostingList}s given as parameters.
//     *
//     * @param a A {@link PostingList}.
//     * @param b The other {@link PostingList}.
//     * @return the {@link List} of {@link Posting} which is the union of the
//     * two given {@link PostingList}s as parameters.
//     */
//    @NotNull
//    public static List<Posting> intersection(@NotNull final List<Posting> a, @NotNull final List<Posting> b) {
//        // TODO : positional intersect not implemented yet
//    }

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
     *
     * @param posting The posting to be compared with this one.
     * @return the result of the comparison of their {@link DocumentIdentifier}s.
     */
    @Override
    public int compareTo(@NotNull Posting posting) {
        return this.docId.compareTo(posting.docId);
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
     * @return true if the instances have the same {@link #docId}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Posting posting = (Posting) o;
        return docId.equals(posting.docId);
    }

    @Override
    public int hashCode() {
        return docId.hashCode();
    }

    @Override
    @NotNull
    public SkipListElement<Posting> setForwardPointer(
            int forwardedElementIndex, @Nullable final SkipListElement<Posting> e) {
        this.forwardedElementIndex = forwardedElementIndex;
        this.forwardPointer = e;
        return this;
    }

    @Override
    @Nullable
    public SkipListElement<Posting> getForwardedElement() {
        return this.forwardPointer;
    }

    @Override
    public boolean hasForwardPointer() {
        return this.forwardPointer != null;
    }

    @Override
    public Posting getElement() {
        return this;
    }

    @Override
    public int getForwardedIndex() {
        return hasForwardPointer() ? forwardedElementIndex : -1;
    }
}