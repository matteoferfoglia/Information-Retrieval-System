package it.units.informationretrieval.ir_boolean_model.exceptions;

/**
 * This exception is thrown when no more
 * {@link it.units.informationretrieval.ir_boolean_model.entities.DocumentIdentifier} can be generated.
 */
public class NoMoreDocIdsAvailable extends Exception {
    public NoMoreDocIdsAvailable(String msg) {
        super(msg);
    }
}
