package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a logic expression.
 *
 * @author Matteo Ferfoglia
 */
interface Expression {

    /**
     * @return the {@link BOOLEAN_OPERATOR} of the instance.
     */
    @NotNull
    BOOLEAN_OPERATOR getOperator();

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
