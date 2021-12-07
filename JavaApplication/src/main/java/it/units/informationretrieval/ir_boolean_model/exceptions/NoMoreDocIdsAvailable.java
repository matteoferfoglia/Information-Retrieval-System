package it.units.informationretrieval.ir_boolean_model.exceptions;

import it.units.informationretrieval.ir_boolean_model.entities.DocumentIdentifier;

/**
 * This exception is thrown when no more {@link DocumentIdentifier} can be generated.
 */
public class NoMoreDocIdsAvailable extends Exception {
    public NoMoreDocIdsAvailable(String msg) {
        super(msg);
    }
}
