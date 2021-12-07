package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.entities.document.Document;
import it.units.informationretrieval.ir_boolean_model.entities.document.DocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.Instant;
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

//    /** The object that saves the position at which the term (referred to this
//     * {@link Posting}) compares in the document to which this {@link Posting}
//     * refers.*/
//    @NotNull
//    private final TermPositionsInADocument positions;  // TODO : positions not handled yet
//

    /**
     * The skip pointer needed to implement a skip list
     */
    @Nullable
    private Posting skipPointer = null;

    /**
     * Constructor. Given a {@link DocumentIdentifier} and a {@link TermPositionsInADocument}
     * object, creates a new instance of this class.
     *
     * @param docId The {@link DocumentIdentifier}.
     */
    //* @param positions The {@link TermPositionsInADocument} object (see the description of the class). */
    public Posting(@NotNull DocumentIdentifier docId/*, @NotNull TermPositionsInADocument positions*/) {
        this.docId = Objects.requireNonNull(docId, "The docId cannot be null");
        creationInstant = Instant.now();
//        this. positions = Objects.requireNonNull(positions, "The positions object cannot be null.");  // TODO : positions not handled yet
        // TODO : skipPointer not handled yet.
    }

//    /** @return the number of occurrences of the {@link Term} associated with this
//     * {@link Posting} (i.e., the term-frequency value). */
//    public int tf() {
//        return positions.size();    // TODO : positions not handled yet
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
        return skipPointer == null;
    }

    /**
     * Sets the {@link #skipPointer} of this instance to the given one.
     *
     * @param skipPointer The skipPointer (i.e., the next {@link Posting} where
     *                    to point).
     */
    public void setSkipPointer(@Nullable Posting skipPointer) {
        this.skipPointer = skipPointer;
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

    /**
     * An instance of this class stores <strong>without</strong> duplicates
     * the positions in a {@link Document} in which the same {@link Term}
     * compares.
     *
     * @author Matteo Ferfoglia
     */
    public static class TermPositionsInADocument {  // TODO: delete this inner class and make positions an attribute of the upper level class

        /**
         * Array of positions.
         */
        private final int[] positions;

        // NOTE: this instance could save the Document and the Term too,
        //       but they are not required and this would waste memory space.

        /**
         * Given a term and a document, creates a new instance of this class
         * and saves the positions in which the term compares in the document.
         *
         * @param document The document.
         * @param term     The term.
         */
        public TermPositionsInADocument(@NotNull Term term, @NotNull Document document) {
            Objects.requireNonNull(term, "The term cannot be null.");
            Objects.requireNonNull(document, "The document cannot be null.");
            positions = new int[0];   // TODO: NOT implemented yet
            throw new UnsupportedOperationException("Not implemented yet");
        }

        /**
         * @return the number of positions in which the term appears in the document.
         */
        public int size() {
            return positions.length;
        }
    }

}