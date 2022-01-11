package it.units.informationretrieval.ir_boolean_model.queries;

/**
 * Represents a logic expression.
 *
 * @author Matteo Ferfoglia
 */
interface Expression {
    /**
     * Class representing the value of an {@link Expression}.
     */
    record Value(String value) {
        @Override
        public String toString() {
            return value;
        }
    }
}
