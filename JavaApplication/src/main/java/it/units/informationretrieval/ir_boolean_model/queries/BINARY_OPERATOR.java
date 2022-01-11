package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Enumeration for possible binary operators to apply on two {@link BooleanExpression}s,
 * which are the operands.
 *
 * @author Matteo Ferfoglia
 */
public enum BINARY_OPERATOR implements BOOLEAN_OPERATOR {
    /**
     * AND operator. Both the {@link BooleanExpression}s (operands) must hold.
     */
    AND("&"),

    /**
     * OR operator. At least one of the {@link BooleanExpression}s (operands) must hold.
     */
    OR("|");

    /**
     * The symbol for the operator.
     */
    private final String SYMBOL;

    /**
     * @param symbol The symbol for the operator.
     */
    BINARY_OPERATOR(@NotNull String symbol) {
        this.SYMBOL = Objects.requireNonNull(symbol);
    }

    @Override
    public String getSymbol() {
        return SYMBOL;
    }
}
