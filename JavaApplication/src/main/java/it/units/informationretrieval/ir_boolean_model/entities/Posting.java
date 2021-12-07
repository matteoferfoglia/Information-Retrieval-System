package it.units.informationretrieval.ir_boolean_model.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class representing a Posting.
 *
 * @author Matteo Ferfoglia.
 */
public class Posting implements Comparable<Posting>, Serializable {

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

//    /**
//     * Array of positions in the document where the term appears.
//     */
//    private final int[] termPositionsInTheDocument;// TODO : positions not handled yet

//    /** The object that saves the position at which the term (referred to this
//     * {@link Posting}) compares in the document to which this {@link Posting}
//     * refers.*/
//    @NotNull
//    private final TermPositionsInADocument positions;  // TODO : positions not handled yet
//

    /**
     * The forward pointer needed to implement a skip list.
     */
    @Nullable
    private Posting forwardPointer = null;

    /**
     * Constructor. Given a {@link DocumentIdentifier}, creates a new instance of this class.
     *
     * @param docId The {@link DocumentIdentifier}.
     */
    //* @param positions The {@link TermPositionsInADocument} object (see the description of the class). */
    public Posting(@NotNull DocumentIdentifier docId/*, @NotNull final int[] positions*/) {
        this.docId = Objects.requireNonNull(docId, "The docId cannot be null");
        creationInstant = Instant.now();
//        this. positions = Objects.requireNonNull(positions);  // TODO : positions not handled yet
        // TODO : skipPointer not handled yet.
    }

    /**
     * Returns the {@link List} of {@link Posting} which is the union of the
     * two {@link PostingList}s given as parameters.
     *
     * @param a A {@link PostingList}.
     * @param b The other {@link PostingList}.
     * @return the {@link List} of {@link Posting} which is the union of the
     * two given {@link PostingList}s as parameters.
     */
    @NotNull
    public static List<Posting> union(@NotNull final List<Posting> a, @NotNull final List<Posting> b) {   // TODO: test and benchmark
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        ArrayList<Posting> union = new ArrayList<>(a.size() + b.size());
        int i = 0, j = 0, comparison;
        while (i < a.size() && j < b.size()) {
            comparison = a.get(i).compareTo(b.get(j));
            if (comparison == 0) {
                union.add(a.get(i++));
                j++;
            } else if (comparison < 0) {
                union.add(a.get(i++));
            } else {
                union.add(b.get(j++));
            }
        }
        union.addAll(a.subList(i, a.size()));
        union.addAll(b.subList(j, b.size()));
        union.trimToSize();

        return union;

        // TODO : positional union not implemented yet
    }

    /**
     * Returns Returns the {@link List} of {@link Posting} which is the intersection of the
     * two {@link PostingList}s given as parameters.
     *
     * @param a A {@link PostingList}.
     * @param b The other {@link PostingList}.
     * @return the {@link List} of {@link Posting} which is the union of the
     * two given {@link PostingList}s as parameters.
     */
    @NotNull
    public static List<Posting> intersection(@NotNull final List<Posting> a, @NotNull final List<Posting> b) {    //  TODO: test and benchmark
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        ArrayList<Posting> intersection = new ArrayList<>(a.size());
        int i = 0, j = 0, comparison;
        while (i < a.size() && j < b.size()) {
            comparison = a.get(i).compareTo(b.get(j));
            if (comparison == 0) {
                intersection.add(a.get(i++));
                j++;
            } else if (comparison < 0) {
                i++;
            } else {
                j++;
            }
        }
        intersection.trimToSize();

        return intersection;

        // TODO : positional intersect not implemented yet
    }

//    /** @return the number of occurrences of the {@link Term} associated with this
//     * {@link Posting} (i.e., the term-frequency value). */
//    public int tf() {
//        return positions.length;    // TODO : positions not handled yet
//    }

    /**
     * Getter for {@link #docId}.
     *
     * @return the {@link #docId} associated with this instance.
     */
    public @NotNull DocumentIdentifier getDocId() {
        return docId;
    }

    /**
     * @return true if this instance has a skip pointer, false otherwise.
     */
    public boolean hasSkipPointer() {
        return forwardPointer == null;
    }

    /**
     * Sets the {@link #forwardPointer} of this instance to the given one.
     *
     * @param forwardPointer The skipPointer (i.e., the next {@link Posting} where
     *                       to point).
     */
    public void setForwardPointer(@Nullable Posting forwardPointer) {
        this.forwardPointer = forwardPointer;
    }

    @Override
    public int compareTo(Posting posting) { // TODO: test
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
    public int compareCreationTimeTo(Posting posting) { // TODO: test
        return this.creationInstant.compareTo(posting.creationInstant);
    }

}