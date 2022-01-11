package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static it.units.informationretrieval.ir_boolean_model.queries.UNARY_OPERATOR.IDENTITY;
import static it.units.informationretrieval.ir_boolean_model.queries.UNARY_OPERATOR.NOT;

/**
 * Class representing a unary expression.
 * Example: if "e" is an expression, then "NOT e" is
 * the unary expression which is the negation of "e".
 *
 * @author Matteo Ferfoglia
 */
class UnaryExpression implements Expression {
    /**
     * The {@link UNARY_OPERATOR} for this instance.
     */
    @NotNull
    private final UNARY_OPERATOR operator;

    /**
     * The {@link Value} for this instance, if this instance
     * is NOT an aggregated expression.
     */
    @Nullable
    private final Expression.Value value;

    /**
     * The {@link Expression} contained inside this instance.
     */
    @Nullable
    private final Expression innerExpression;

    /**
     * Constructor.
     *
     * @param expressionValue The value contained in this instance.
     * @param operator        The {@link UNARY_OPERATOR} for this instance.
     */
    public UnaryExpression(
            @NotNull final Value expressionValue,
            @NotNull final UNARY_OPERATOR operator) {
        this(operator, Objects.requireNonNull(expressionValue), null);
    }

    /**
     * Constructor.
     */
    private UnaryExpression(
            @NotNull final UNARY_OPERATOR operator,
            @Nullable final Value expressionValue,
            @Nullable final Expression innerExpression) {

        if (innerExpression instanceof UnaryExpression unaryExpression) {
            this.innerExpression = unaryExpression.innerExpression;
            this.value = unaryExpression.value;
            this.operator = operator.equals(IDENTITY)
                    ? unaryExpression.operator
                    : /*negation*/ unaryExpression.operator.equals(NOT) ? /*NOT * NOT = IDENTITY*/ IDENTITY : NOT;
        } else {
            this.innerExpression = innerExpression;
            this.operator = Objects.requireNonNull(operator);
            this.value = expressionValue;
        }

        assert isComplementaryConditionHolding();
    }

    /**
     * Constructor.
     *
     * @param innerExpression The {@link Expression} contained inside this instance.
     * @param operator        The {@link UNARY_OPERATOR} for this instance.
     */
    public UnaryExpression(
            @NotNull final Expression innerExpression,
            @NotNull final UNARY_OPERATOR operator) {
        this(operator, null, Objects.requireNonNull(innerExpression));
    }

    /**
     * @return true if the complementary conditions (which states that either
     * the {@link #value} or the {@link #innerExpression} must be not null
     * but not both of them) holds, false otherwise.
     */
    private boolean isComplementaryConditionHolding() {
        return value == null && innerExpression != null
                || value != null && innerExpression == null;
    }

    /**
     * @return true if this instance contains an inner {@link Expression}.
     */
    public boolean isAggregated() {
        assert isComplementaryConditionHolding();
        return innerExpression != null;
    }

    @Override
    public String toString() {
        String innerToString;
        if (isAggregated()) {
            assert innerExpression != null;
            innerToString = innerExpression.toString();
        } else {
            assert value != null;
            innerToString = value.value();
        }
        return operator.equals(NOT)
                ? "NOT " + (innerExpression == null ? "(" : "") + innerToString + " " + (innerExpression == null ? ")" : "")
                : innerToString;
    }

    /**
     * @return the value of this instance or null if it is an aggregated expression.
     */
    @Nullable
    public String getValue() {
        assert isComplementaryConditionHolding();
        return value != null ? value.value() : null;
    }

    /**
     * Getter for the operator of this expression.
     */
    @NotNull
    public UNARY_OPERATOR getOperator() {
        return operator;
    }
}
