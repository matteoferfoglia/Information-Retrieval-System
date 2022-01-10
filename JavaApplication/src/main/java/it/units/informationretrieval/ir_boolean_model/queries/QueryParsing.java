package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression.BINARY_OPERATOR.AND;

/**
 * This class provides the functionalities to parseBinaryExpression an input
 * query string and translate it to a {@link BooleanExpression}.
 *
 * @author Matteo Ferfoglia
 */
class QueryParsing {

    /**
     * Special character that cannot be present in input query strings
     * and that is used during parsing: when an inner expression of the
     * input query string is already parsed, it is replaced with this
     * special character, i.e.: this special character is used in
     * intermediate operations.
     */
    private static final String REPLACED_EXPRESSION_PLACEHOLDER = "§";
    /**
     * Compiled regex matching OR boolean expressions in text.
     */
    private static final Pattern REGEX_OR = Pattern.compile(
            getRegexMatchingBinaryExpressionIgnoringUnaryExpressions(BooleanExpression.BINARY_OPERATOR.OR));
    /**
     * Compiled regex matching AND boolean expressions in text.
     */
    private static final Pattern REGEX_AND = Pattern.compile(
            getRegexMatchingBinaryExpressionIgnoringUnaryExpressions(AND));

    /**
     * @param binaryOperator The {@link BooleanExpression.BINARY_OPERATOR} of the expression
     *                       to match.
     * @return a {@link String} which is a regex that either matches a binary expression
     * in a text or matches a binary "OR" expressions, whose operands can contain one or
     * more "AND" expressions. This is done because AND operations have the priority over
     * the OR operations. The returned regex ignores unary expressions.
     */
    private static String getRegexMatchingBinaryExpressionIgnoringUnaryExpressions(
            @NotNull final BooleanExpression.BINARY_OPERATOR binaryOperator) {
        boolean andOperatorMustBeCaptured = binaryOperator.equals(BooleanExpression.BINARY_OPERATOR.OR);
        String otherThingsToCapture = andOperatorMustBeCaptured
                ? "|([^\\|]+\\&[^\\|]+)+"    // OR has lower precedence than AND, so AND expressions can be present inside OR expressions (as inner expressions)
                : "";
        return "((" + REPLACED_EXPRESSION_PLACEHOLDER + otherThingsToCapture + "|\\w+)\\s*\\"
                + binaryOperator.getSymbol() + "+"  // "+" to handle the case if the use erroneously insert multiple time the operator in he query string
                + "\\s*(" + REPLACED_EXPRESSION_PLACEHOLDER + otherThingsToCapture + "|\\w+))";
    }

    /**
     * Parses the input textual query string.
     * // TODO: write here and in the class description the rule for the input.
     *
     * @param queryString The input query string to be parsed.
     * @return the expression for the input query string.
     */
    static Expression parse(@NotNull String queryString) {

        // remove the special character not allowed in query strings because used in intermediate operations
        Wrapper<String> wrappedQueryString = new Wrapper<>(
                queryString.replaceAll(REPLACED_EXPRESSION_PLACEHOLDER, ""));

        // Match low priority binary expressions (OR) first
        Expression topLevelOrExpression = parseBinaryExpression(
                new Stack<>(), wrappedQueryString, BooleanExpression.BINARY_OPERATOR.OR);

        // Match high priority binary expressions (AND), if no OR expressions were present
        if (topLevelOrExpression instanceof UnaryExpression unaryExpression
                && (queryString = unaryExpression.getValue()) != null) {
            // here if no OR expressions were present and the expression was evaluated as a unary expression
            return parseBinaryExpression(new Stack<>(), new Wrapper<>(queryString), AND);
        } else {
            return topLevelOrExpression;
        }
    }


    /**
     * This method is the actual responsible for the parsing.
     * The input query string is recursively examined and,
     * while inner expressions are detected from the text and
     * extracted, the text is replaced with {@link #REPLACED_EXPRESSION_PLACEHOLDER},
     * then this method is recursively invoked on the "remaining"
     * query string which might contain some other expressions.
     * <p/>
     * <strong>Notice</strong>: this method is specific for
     * binary operation detection and does <strong>not</strong>
     * detect unary expressions.
     * <p/>
     * <strong>This method must be invoked first to find all
     * OR binary expressions, then for AND binary expressions,
     * otherwise the correct results is not guaranteed to be
     * returned in the overall query string (assuming that
     * this method will be invoked multiple times, without
     * considering the number of recursive "auto" invocations).</strong>
     *
     * @param alreadyFoundExpressions The expressions already found (this method is
     *                                recursively invoked) or an empty list at the
     *                                first invocation.
     * @param remainingQueryString    The query string to examine.
     * @param binaryOperator          The binary operator for the binary expressions to match.
     * @return the (eventually aggregated) {@link Expression} obtained from parsing.
     */
    @NotNull
    private static Expression parseBinaryExpression(
            @NotNull Stack<BinaryExpression> alreadyFoundExpressions,
            @NotNull Wrapper<String> remainingQueryString,
            @NotNull BooleanExpression.BINARY_OPERATOR binaryOperator) {

        Pattern compiledRegex = binaryOperator.equals(AND) ? REGEX_AND : REGEX_OR;
        assert remainingQueryString.get() != null;
        Matcher matcher = compiledRegex.matcher(remainingQueryString.get());
        if (matcher.find()) {    // .matches() gives wrong result, hence .find() + .reset() is used
            matcher.reset();
            while (matcher.find()) {
                // binary expression found in text (match found)

                String[] split = matcher.group()
                        .replaceAll("\\" + binaryOperator.getSymbol() + "+", binaryOperator.getSymbol()) // remove duplicates of the operator (if the user wrongly inserted it multiple times)
                        .split("\\" + binaryOperator.getSymbol());
                assert split.length == 2;   // two operands in binary expressions

                // When we discover a binary expression in the text, each of its two children can be:
                //   1) a unary expression
                //   2) an AND binary expression only if we are now looking for OR binary expressions (OR expressions have low priority)

                Expression leftChild = split[0].equals(REPLACED_EXPRESSION_PLACEHOLDER)
                        ? alreadyFoundExpressions.pop() // expression previously matched
                        : parseBinaryExpression(new Stack<>(), new Wrapper<>(split[0]), AND);
                Expression rightChild = split[1].equals(REPLACED_EXPRESSION_PLACEHOLDER)
                        ? alreadyFoundExpressions.pop() // expression previously matched
                        : parseBinaryExpression(new Stack<>(), new Wrapper<>(split[1]), AND);

                BinaryExpression be = new BinaryExpression(leftChild, binaryOperator, rightChild);
                alreadyFoundExpressions.add(be);
            }

            remainingQueryString.set(
                    remainingQueryString.get()
                            .replaceAll(compiledRegex.pattern(), REPLACED_EXPRESSION_PLACEHOLDER)     // remove the already matched expression
                            .replaceAll("\\s*", ""));    // remove parenthesis if used to box the just matched expression

            return parseBinaryExpression(alreadyFoundExpressions, remainingQueryString, binaryOperator);
        } else {
            // no more match, the remaining query string might be nothing (i.e., everything already parsed) or an unary expression
            // TODO : regex for unary operations and negations not handled

            return alreadyFoundExpressions.size() == 0
                    ? new UnaryExpression(
                    new Expression.Value(remainingQueryString.getAndRemove()),
                    BooleanExpression.UNARY_OPERATOR.IDENTITY)
                    : BinaryExpression.createFromList(alreadyFoundExpressions, binaryOperator);
        }
    }

    /**
     * Represents a logic expression.
     */
    private interface Expression {
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

    /**
     * Class representing a unary expression.
     * Example: if "e" is an expression, then "NOT e" is
     * the unary expression which is the negation of "e".
     */
    private static class UnaryExpression implements Expression {
        /**
         * The {@link BooleanExpression.UNARY_OPERATOR} for this instance.
         */
        @NotNull
        private final BooleanExpression.UNARY_OPERATOR operator;

        /**
         * The {@link Expression.Value} for this instance, if this instance
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
         * @param operator        The {@link BooleanExpression.UNARY_OPERATOR} for this instance.
         */
        public UnaryExpression(
                @NotNull final Value expressionValue,
                @NotNull final BooleanExpression.UNARY_OPERATOR operator) {
            this(operator, Objects.requireNonNull(expressionValue), null);
        }

        /**
         * Constructor.
         */
        private UnaryExpression(
                @NotNull final BooleanExpression.UNARY_OPERATOR operator,
                @Nullable final Value expressionValue,
                @Nullable final Expression innerExpression) {
            this.operator = Objects.requireNonNull(operator);
            this.value = expressionValue;
            this.innerExpression = innerExpression;
            assert isComplementaryConditionHolding();
        }

        /**
         * Constructor.
         *
         * @param innerExpression The {@link Expression} contained inside this instance.
         * @param operator        The {@link BooleanExpression.UNARY_OPERATOR} for this instance.
         */
        public UnaryExpression(
                @NotNull final Expression innerExpression,
                @NotNull final BooleanExpression.UNARY_OPERATOR operator) {
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
            return operator.equals(BooleanExpression.UNARY_OPERATOR.NOT)
                    ? "NOT( " + innerToString + " )"
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
    }

    /**
     * Class representing a binary expression.
     * Example: "a AND b" is a binary expression.
     */
    private static class BinaryExpression implements Expression {

        /**
         * The {@link BooleanExpression.BINARY_OPERATOR} for this instance.
         */
        @NotNull
        private final BooleanExpression.BINARY_OPERATOR operator;

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
                @NotNull final BooleanExpression.BINARY_OPERATOR binaryOperator,
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
         * @param concatenator The {@link BooleanExpression.BINARY_OPERATOR} to use
         *                     to concatenate the input list of expressions.
         */
        public static BinaryExpression createFromList(
                @NotNull Stack<BinaryExpression> expressions,
                @NotNull BooleanExpression.BINARY_OPERATOR concatenator) {
            return switch (expressions.size()) {
                case 0 -> throw new IllegalArgumentException(
                        "At least one expression must be present in the input list");
                case 1 -> expressions.pop();
                default -> new BinaryExpression( // more than one boolean expression are present
                        expressions.pop(),
                        concatenator,
                        createFromList(expressions, concatenator));
            };
        }

        @Override
        public String toString() {
            return "( " + leftOperand + " " + operator + " " + rightOperand + " )";
        }
    }

    /**
     * Wrapper class to keep the reference to an object.
     *
     * @param <T> The type of the wrapped value.
     */
    private static class Wrapper<T> {

        /**
         * The value tobe wrapped.
         */
        @Nullable
        private T value;

        /**
         * Constructor.
         *
         * @param value The value tobe wrapped.
         */
        public Wrapper(@Nullable T value) {
            this.value = value;
        }

        /**
         * Getter for the value.
         *
         * @return the value wrapped by this instance.
         */
        @Nullable
        public T get() {
            return value;
        }

        /**
         * Getter for the value.
         *
         * @param value The new value to set.
         * @return this instance after having set the new value.
         */
        @NotNull
        public Wrapper<T> set(T value) {
            this.value = value;
            return this;
        }

        /**
         * Gets the wrapped element and removes it from this instance,
         * setting the wrapped value to null.
         *
         * @return the wrapped value before removing it.
         */
        public T getAndRemove() {
            var value = this.value;
            this.value = null;
            return value;
        }
    }

}
