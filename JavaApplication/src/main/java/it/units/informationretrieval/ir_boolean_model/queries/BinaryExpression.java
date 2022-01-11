package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.Stack;

/**
 * Class representing a binary expression.
 * Example: "a AND b" is a binary expression.
 *
 * @author Matteo Ferfoglia
 */
class BinaryExpression implements Expression {

    /**
     * The {@link BINARY_OPERATOR} for this instance.
     */
    @NotNull
    private final BINARY_OPERATOR operator;

    /**
     * The left-operand for this instance.
     */
    @NotNull
    private final Expression leftOperand;

    /**
     * The right-operand for this instance.
     */
    @NotNull
    private final Expression rightOperand;

    /**
     * Constructor.
     *
     * @param leftOperand    The left-operand of this binary expression.
     * @param binaryOperator The binary operator of this binary expression.
     * @param rightOperand   The right-operand of this binary expression.
     */
    public BinaryExpression(
            @NotNull final Expression leftOperand,
            @NotNull final BINARY_OPERATOR binaryOperator,
            @NotNull final Expression rightOperand) {
        this.leftOperand = Objects.requireNonNull(leftOperand);
        this.operator = Objects.requireNonNull(binaryOperator);
        this.rightOperand = Objects.requireNonNull(rightOperand);
    }

    /**
     * Creates an aggregated instance, having as children the elements from
     * the input list.
     *
     * @param expressions  The list of expressions.
     * @param concatenator The {@link BINARY_OPERATOR} to use
     *                     to concatenate the input list of expressions.
     */
    public static BinaryExpression createFromList(
            @NotNull Stack<Expression> expressions,
            @NotNull BINARY_OPERATOR concatenator) {

        return switch (expressions.size()) {
            case 0 -> throw new IllegalArgumentException(
                    "At least one expression must be present in the input list");
            case 1 -> {
                Expression e = expressions.pop();
                if (e instanceof BinaryExpression binaryExpression) {
                    yield binaryExpression;
                } else {
                    throw new IllegalArgumentException(BinaryExpression.class.getCanonicalName() + " expected, but "
                            + e.getClass().getCanonicalName() + " found");
                }
            }
            default -> {
                List<UnaryExpression> unaryExpressions = expressions.stream()
                        .filter(expression -> expression instanceof UnaryExpression)
                        .map(expression -> (UnaryExpression) expression)
                        .toList();
                expressions.removeAll(unaryExpressions);
                if (unaryExpressions.size() > 0) {
                    if (unaryExpressions.size() % 2 == 0) {
                        for (int i = 0; i < unaryExpressions.size(); ) {
                            expressions.push(new BinaryExpression(
                                    unaryExpressions.get(i++), concatenator, unaryExpressions.get(i++)));
                        }
                    } else {
                        BinaryExpression be = (BinaryExpression) expressions.stream()
                                .filter(expression -> expression instanceof BinaryExpression)
                                .findAny()
                                .orElseThrow(); // if here: expressions.size()>=2 && unaryExpressions.size() is odd, hence a Binary expression MUST be present
                        expressions.remove(be);
                        expressions.push(new BinaryExpression(be, concatenator, unaryExpressions.get(0)));
                        unaryExpressions.remove(0);
                        assert unaryExpressions.size() == 0 || unaryExpressions.size() % 2 == 1;
                        for (int i = 0; i < unaryExpressions.size(); ) {
                            expressions.push(new BinaryExpression(
                                    unaryExpressions.get(i++), concatenator, unaryExpressions.get(i++)));
                        }
                    }
                }

                yield new BinaryExpression( // more than one boolean expression are present
                        expressions.pop(),
                        concatenator,
                        createFromList(expressions, concatenator));
            }
        };
    }

    @Override
    public String toString() {
        return "( " + leftOperand + " " + operator + " " + rightOperand + " )";
    }

    @Override
    @NotNull
    public BOOLEAN_OPERATOR getOperator() {
        return operator;
    }

    /**
     * Getter for the left-hand operand of this instance.
     */
    @NotNull
    public Expression getLeftOperand() {
        return leftOperand;
    }

    /**
     * Getter for the left-hand operand of this instance.
     */
    @NotNull
    public Expression getRightOperand() {
        return rightOperand;
    }
}
