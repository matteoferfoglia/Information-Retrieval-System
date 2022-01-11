package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.units.informationretrieval.ir_boolean_model.queries.BINARY_OPERATOR.AND;
import static it.units.informationretrieval.ir_boolean_model.queries.BINARY_OPERATOR.OR;
import static it.units.informationretrieval.ir_boolean_model.queries.UNARY_OPERATOR.IDENTITY;
import static it.units.informationretrieval.ir_boolean_model.queries.UNARY_OPERATOR.NOT;

/**
 * This class provides the functionalities to parseBinaryExpression an input
 * query string and translate it to a {@link BooleanExpression}.
 * Expressions allowed are:
 * <ul>
 *     <li>AND queries, with the syntax, e.g.: <pre>a & b</pre> (spaces are not mandatory)</li>
 *     <li>OR queries, with the syntax, e.g.:: <pre>a | b</pre> (spaces are not mandatory)</li>
 *     <li>NOT queries, with the syntax, e.g.:: <pre>! a</pre> (spaces are not mandatory)</li>
 *     <li>parenthesis can be used to impose the priority, e.g.: <pre>(a|b)&c</pre> (spaces are not mandatory)</li>
 * </ul>
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
    private static final String REPLACED_EXPRESSION_PLACEHOLDER = "\0";
    /**
     * Compiled regex matching OR boolean expressions in text.
     */
    private static final Pattern REGEX_OR = Pattern.compile(
            getRegexMatchingBinaryExpressionIgnoringUnaryExpressions(OR));
    /**
     * Compiled regex matching AND boolean expressions in text.
     */
    private static final Pattern REGEX_AND = Pattern.compile(
            getRegexMatchingBinaryExpressionIgnoringUnaryExpressions(AND));
    /**
     * Compiled regex matching balanced brackets and text between them.
     */
    private static final Pattern REGEX_BRACKETS = Pattern.compile("[(][^()]*[)]");

    /**
     * @param binaryOperator The {@link BINARY_OPERATOR} of the expression
     *                       to match.
     * @return a {@link String} which is a regex that either matches a binary expression
     * in a text or matches a binary "OR" expressions, whose operands can contain one or
     * more "AND" expressions. This is done because AND operations have the priority over
     * the OR operations. The returned regex ignores unary expressions.
     */
    private static String getRegexMatchingBinaryExpressionIgnoringUnaryExpressions(
            @NotNull final BINARY_OPERATOR binaryOperator) {
        boolean andOperatorMustBeCaptured = binaryOperator.equals(OR);
        String otherThingsToCapture = andOperatorMustBeCaptured
                ? "([^\\|]+\\&[^\\|]+)+"    // OR has lower precedence than AND, so AND expressions can be present inside OR expressions (as inner expressions)
                : "";
        otherThingsToCapture += "\\" + BooleanExpression.PHRASE_DELIMITER // TODO: test
                + "\\" + BooleanExpression.NUM_OF_WORDS_FOLLOWS_CHARACTER;
        String escapedNotSymbol = "\\" + NOT.getSymbol();
        return "([" + REPLACED_EXPRESSION_PLACEHOLDER + escapedNotSymbol + otherThingsToCapture + "\\w]+\\s*"
                + "\\" + binaryOperator.getSymbol() + "+"  // "+" to handle the case if the use erroneously insert multiple time the operator in he query string
                + "\\s*[" + REPLACED_EXPRESSION_PLACEHOLDER + escapedNotSymbol + otherThingsToCapture + "\\w]+)";
    }

    /**
     * Parses the input textual query string.
     *
     * @param queryString The input query string to be parsed.
     * @return the expression for the input query string or null if
     * the input is invalid.
     */
    @Nullable
    static Expression parse(@NotNull String queryString) {    // TODO: benchmark

        // Create copy of query string abd remove the special character not allowed
        // in query strings because used in intermediate operations
        String queryStringWorkingCopy = queryString.replaceAll(REPLACED_EXPRESSION_PLACEHOLDER, "");

        {
            // Replace all spaces between words with AND operator
            final Pattern REGEX_REPLACE_QUOTES_WITH_AND_OPERATOR = Pattern.compile(
                    "(\\s*(\\" + NOT.getSymbol() + "*\\w+\\s+)+\\" + NOT.getSymbol() + "*\\w+\\s*(?=("
                            + "[^\\" + BooleanExpression.PHRASE_DELIMITER + "]*\\" + BooleanExpression.PHRASE_DELIMITER
                            + "[^\\" + BooleanExpression.PHRASE_DELIMITER + "]*\\" + BooleanExpression.PHRASE_DELIMITER + ")*"
                            + "[^\\" + BooleanExpression.PHRASE_DELIMITER + "]*$))"
                            + "|\\w+\\" + BooleanExpression.PHRASE_DELIMITER + "\\s+\\" + NOT.getSymbol() + "*\\w+"
                            + "|\\w+\\s+\\" + BooleanExpression.PHRASE_DELIMITER + "\\" + NOT.getSymbol() + "*\\w+");
            for (int i = 0; i < 2; i++) {   // need to pass 2 times over the string to do all replacements
                Matcher m = REGEX_REPLACE_QUOTES_WITH_AND_OPERATOR.matcher(queryStringWorkingCopy);
                while (m.find()) {
                    queryStringWorkingCopy = queryStringWorkingCopy
                            .replaceAll(m.group(), m.group().replaceAll("\\s+", AND.getSymbol()));
                }
            }
        }

        final Wrapper<String> wrappedQueryString = new Wrapper<>(queryStringWorkingCopy);

        try {
            return parseWithBracketsPriority(new Stack<>(), wrappedQueryString);
        } catch (Exception e) {
            // catch any exception due to parsing to avoid the program to crash
            Logger.getLogger(QueryParsing.class.getCanonicalName())
                    .log(
                            Level.WARNING,
                            "Exception thrown while parsing the query string (\"" + queryString + "\")",
                            e);
            return null;
        }
    }

    /**
     * The idea of this method is similar to {@link #parseBinaryExpression(Stack, Wrapper, BINARY_OPERATOR)}
     * and is used to handle the priority of the expressions thanks to brackets.
     *
     * @param alreadyFoundExpressions The {@link Stack} of already detected higher-priority expressions.
     *                                The higher priority is given by brackets.
     * @param remainingQueryString    The remaining query string, not yet parsed.
     * @return the {@link Expression} which considers both the remaining
     * query string terms and the already matched ones.
     */
    @NotNull
    private static Expression parseWithBracketsPriority(
            @NotNull Stack<Expression> alreadyFoundExpressions,
            @NotNull Wrapper<String> remainingQueryString) {

        // Handle brackets which determine the priority
        assert remainingQueryString.get() != null;
        Matcher bracketsMatcher = REGEX_BRACKETS.matcher(remainingQueryString.get());
        if (bracketsMatcher.find()) {
            bracketsMatcher.reset();
            while (bracketsMatcher.find()) {
                String bracketsPriorityQueryString = bracketsMatcher.group()
                        .replaceAll("[()]+", "");    // remove brackets rounding the higher priority query string

                Expression higherPriorityExpression = bracketsPriorityQueryString.equals(REPLACED_EXPRESSION_PLACEHOLDER)
                        ? alreadyFoundExpressions.pop() // expression previously matched
                        : parseWithBracketsPriority(alreadyFoundExpressions, new Wrapper<>(bracketsPriorityQueryString));
                alreadyFoundExpressions.add(higherPriorityExpression);
            }
            remainingQueryString.set(
                    remainingQueryString.get()
                            .replaceAll(REGEX_BRACKETS.pattern(), REPLACED_EXPRESSION_PLACEHOLDER)  // remove the already matched expression
                            .replaceAll("\\s*", ""));                               // remove extra spaces
            return parseWithBracketsPriority(alreadyFoundExpressions, remainingQueryString);
        } else {

            // Match low priority binary expressions (OR) first
            Expression topLevelORExpression = parseBinaryExpression(alreadyFoundExpressions, remainingQueryString, OR);

            // Match high priority binary expressions (AND)
            Optional<String> valueOfUnaryExpression;
            if (topLevelORExpression instanceof UnaryExpression unaryExpression &&
                    (valueOfUnaryExpression = unaryExpression.getValue()).isPresent()) {
                // here if no OR expressions were present and the expression was evaluated as a unary expression
                return new UnaryExpression(
                        parseBinaryExpression(new Stack<>(), new Wrapper<>(valueOfUnaryExpression.get()), AND),
                        unaryExpression.getOperator());
            } else if (remainingQueryString.get() != null) {
                return parseBinaryExpression(new Stack<>() {{
                    add(topLevelORExpression);
                }}, remainingQueryString, AND);
            } else {
                return topLevelORExpression;
            }
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
            @NotNull Stack<Expression> alreadyFoundExpressions,
            @NotNull Wrapper<String> remainingQueryString,
            @NotNull BINARY_OPERATOR binaryOperator) {

        assert remainingQueryString.get() != null;
        String remainingQueryStringTmp = remainingQueryString.get().strip();

        if (remainingQueryStringTmp.isBlank()) {
            // this check is just to avoid useless computations
            return finalizerWhenNoMoreMatch(alreadyFoundExpressions, remainingQueryString, binaryOperator);
        } else if (// Check if the remaining query string is boxed by the phrase delimiter
                remainingQueryStringTmp.length() >= 2 * BooleanExpression.PHRASE_DELIMITER.length()
                        && remainingQueryStringTmp.startsWith(BooleanExpression.PHRASE_DELIMITER)
                        && remainingQueryStringTmp.endsWith(BooleanExpression.PHRASE_DELIMITER)
                        && !remainingQueryStringTmp.substring(1, remainingQueryStringTmp.length() - 1)
                        .contains(BooleanExpression.PHRASE_DELIMITER)) {
            return finalizerWhenNoMoreMatch(alreadyFoundExpressions, remainingQueryString, binaryOperator);
        } else {

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
                                .replaceAll("\\s*", ""));                                 // remove extra spaces

                return parseBinaryExpression(alreadyFoundExpressions, remainingQueryString, binaryOperator);
            } else if (binaryOperator.equals(OR)) {
                // let the work to AND operations
                return parseBinaryExpression(alreadyFoundExpressions, remainingQueryString, AND);
            } else {
                return finalizerWhenNoMoreMatch(alreadyFoundExpressions, remainingQueryString, binaryOperator);
            }
        }
    }

    /**
     * This method is invoked by {@link #parseBinaryExpression(Stack, Wrapper, BINARY_OPERATOR)}
     * when no more match are found, hence an {@link Expression} must be returned,
     * i.e.: this is a terminal operation.
     *
     * @param foundExpression      The expressions found.
     * @param remainingQueryString The remaining unexamined query string.
     * @param binaryOperator       The binary operator for the binary expressions to match.
     * @return the (eventually aggregated) {@link Expression} obtained from parsing.
     */
    private static Expression finalizerWhenNoMoreMatch(
            @NotNull Stack<Expression> foundExpression,
            @NotNull Wrapper<String> remainingQueryString,
            @NotNull BINARY_OPERATOR binaryOperator) {

        // no more match, the remaining query string might be nothing (i.e., everything already parsed) or a unary expression

        UNARY_OPERATOR unaryOperator = IDENTITY;  // default unary operation

        assert remainingQueryString.get() != null;
        String remainingQueryStringVal = remainingQueryString.get().strip();
        int indexOfNotOperator = remainingQueryStringVal.indexOf(NOT.getSymbol());
        if (indexOfNotOperator > 0) {
            // NOT operator is present, but it is not at the beginning
            throw new IllegalArgumentException("Invalid expression (" + remainingQueryStringVal + ")");
        } else if (indexOfNotOperator == 0) {

            // remove all duplicated negations (which results in an identity if present in even numbers)
            remainingQueryStringVal = remainingQueryStringVal.replaceAll("\\" + NOT.getSymbol() + "{2}", "");

            if (remainingQueryStringVal.startsWith(NOT.getSymbol())) {
                unaryOperator = NOT;
                remainingQueryStringVal = remainingQueryStringVal.replace(NOT.getSymbol(), "");
                assert !remainingQueryStringVal.contains(NOT.getSymbol());
            }

            remainingQueryString.set(remainingQueryStringVal);
        }

        return switch (foundExpression.size()) {
            case 0 -> new UnaryExpression(new Expression.Value(remainingQueryString.getAndRemove()), unaryOperator);
            case 1 -> unaryOperator.equals(IDENTITY)
                    ? foundExpression.pop()
                    : new UnaryExpression(foundExpression.pop(), unaryOperator);
            default -> BinaryExpression.createFromList(foundExpression, binaryOperator);
        };
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
