package it.units.informationretrieval.ir_boolean_model.entities.document;

import it.units.informationretrieval.ir_boolean_model.entities.Posting;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.SynchronizedCounter;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;

import java.io.Serializable;

/**
 * Class representing a Document Identifier.
 * Implementing a separate class for the document identifier
 * allows separating components and keep an high level of
 * abstraction (the actual type of document identifier is
 * encapsulated in this class).
 *
 * @author Matteo Ferfoglia
 */
public class DocumentIdentifier implements Comparable<DocumentIdentifier>, Serializable {

    /**
     * Static counter to generate new docIDs without duplicates.
     */
    private final static SynchronizedCounter counter = new SynchronizedCounter();

    /**
     * The Document identifier of the document associated with this posting.
     */
    private final int docId;

    /**
     * Constructor.
     * Creates a new instance of this class, without any duplicates.
     *
     * @throws NoMoreDocIdsAvailable If no more docIDs are available
     *                               (overflow of the counter).
     */
    public DocumentIdentifier() throws NoMoreDocIdsAvailable {
        try {
            this.docId = counter.postIncrement();   // saves the counter value before incrementing
        } catch (SynchronizedCounter.CounterOverflowException e) {
            throw new NoMoreDocIdsAvailable(Utility.stackTraceToString(e));
        }
    }

    @Override
    public int compareTo(DocumentIdentifier documentIdentifier) {
        return this.docId - documentIdentifier.docId;
    }

    /**
     * Two {@link Posting}s are equals if they have the same docID.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentIdentifier that = (DocumentIdentifier) o;

        return docId == that.docId;
    }

    @Override
    public int hashCode() {
        return docId;
    }

    @Override
    public String toString() {
        return String.valueOf(docId);
    }

}
