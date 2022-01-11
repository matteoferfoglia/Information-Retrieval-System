package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Enumeration for possible unary operators to apply on a {@link BooleanExpression}.
 *
 * @author Matteo Ferfoglia
 */
public enum UNARY_OPERATOR implements BOOLEAN_OPERATOR {
    /**
     * IDENTITY. No operator is applied.
     */
    IDENTITY(""),

    /**
     * NOT operator. If it is applied on a non-aggregated {@link BooleanExpression},
     * it search for documents which do not have the matching value of that {@link
     * BooleanExpression}; if it is applied to an aggregated {@link BooleanExpression},
     * it search for documents which do <strong>not</strong> match the given expression.
     */
    NOT("!");

    /**
     * The symbol for the operator.
     */
    private final String SYMBOL;

    /**
     * @param symbol The symbol for the operator.
     */
    UNARY_OPERATOR(@NotNull String symbol) {
        this.SYMBOL = Objects.requireNonNull(symbol);
    }

    @Override
    public String getSymbol() {
        return SYMBOL;
    }
}
