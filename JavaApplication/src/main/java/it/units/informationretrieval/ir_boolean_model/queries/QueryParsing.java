package it.units.informationretrieval.ir_boolean_model.queries;

import com.bpodgursky.jbool_expressions.Expression;
import com.bpodgursky.jbool_expressions.parsers.BooleanExprLexer;
import com.bpodgursky.jbool_expressions.parsers.ExprParser;
import com.bpodgursky.jbool_expressions.rules.RuleSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static it.units.informationretrieval.ir_boolean_model.queries.BINARY_OPERATOR.AND;
import static it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression.PHRASE_DELIMITER;
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
     * Parses the input textual query string.
     *
     * @param queryString The input query string to be parsed.
     *                    Only valid tokens must be present (see implementation of
     *                    {@link BooleanExprLexer#mTokens()}).
     * @return the expression for the input query string or null if the input is
     * blank or invalid.
     */
    @Nullable
    static Expression<String> parse(@NotNull String queryString) {

        queryString = removeInvalidCharacters(queryString);
        queryString = replaceSpacesWithAndOperator(queryString);
        queryString = removeDuplicatedAdjacentBinaryOperators(queryString);

        if (queryString.isBlank()) {
            System.err.println("The query string is blank.");
            return null;
        }

        // Parse expression
        try {
            var parsedExpression = ExprParser.parse(queryString);

            // Simplify and convert the expression to a "sum of products"
            //  i.e., AND operations will have the priority, then the OR of results will be evaluated
            // This can be very helpful for query optimization, because performing AND queries
            //  first reduces sizes of intermediate results
            return RuleSet.toDNF(parsedExpression);
        } catch (RuntimeException e) {
            // invalid input
            return null;
        }
    }

    /**
     * Removes duplicated adjacent operators, that might be wrongly inserted,
     * e.g. <code>a &&& c</code> becomes <code>a & c</code>
     *
     * @param queryString The input query string.
     * @return The query string without duplicated adjacent operators.
     */
    private static String removeDuplicatedAdjacentBinaryOperators(String queryString) {
        for (var bo : BINARY_OPERATOR.values()) {
            queryString = queryString
                    .replaceAll("\\" + bo.getSymbol() + "{2,}", "\\" + bo.getSymbol());
        }
        return queryString;
    }

    /**
     * Replaces all spaces between words with {@link BINARY_OPERATOR#AND} operator.
     *
     * @param queryString The query string on which to perform the replacement.
     * @return the resulting query string (after replacements).
     */
    @NotNull
    private static String replaceSpacesWithAndOperator(@NotNull String queryString) {

        queryString = queryString.strip();

        final String LOOK_AHEAD_REGEX =
                "(?=(" + "[^\\" + PHRASE_DELIMITER + "]*\\" + PHRASE_DELIMITER
                        + "[^\\" + PHRASE_DELIMITER + "]*\\" + PHRASE_DELIMITER + ")*"
                        + "[^\\" + PHRASE_DELIMITER + "]*$)";
        final Pattern REGEX_REPLACE_SPACES_WITH_AND_OPERATOR = Pattern.compile(
                "\\s*(\\" + NOT.getSymbol() + "*\\w+\\s+)+\\" + NOT.getSymbol() + "*\\w+\\s*" // match substring like {  a b  c }
                        + LOOK_AHEAD_REGEX); // matched string must not be between phrase delimiters (i.e., neither {"  d"} nor {"a b"} nor similar should match)

        Matcher m = REGEX_REPLACE_SPACES_WITH_AND_OPERATOR.matcher(queryString);
        while (m.find()) {
            queryString =
                    queryString.replaceAll(m.group(), m.group().replaceAll("\\s+", AND.getSymbol()));
        }

        // Latest refinements
        queryString = queryString
                .replaceAll("\\s+" + LOOK_AHEAD_REGEX, "")                // remove remaining spaces not inside phrase delimiters
                .replaceAll(NOT.getSymbol() + AND.getSymbol(), NOT.getSymbol());  // remove erroneously added AND symbols (not correctly matching in previous regex)

        return queryString;
    }

    /**
     * Searches for and eventually removes invalid characters from the query string.
     *
     * @param queryString The query string on which to perform the removal.
     * @return the resulting query string (after invalid character's removal).
     */
    @NotNull
    private static String removeInvalidCharacters(@NotNull String queryString) {
        // Check if present and eventually remove invalid characters

        final String REGEX_INVALID_INPUT_CHARACTERS = "[^\\w\\s\"'&|!()]";
        Matcher invalidCharacterMatcher = Pattern.compile(REGEX_INVALID_INPUT_CHARACTERS).matcher(queryString);
        if (invalidCharacterMatcher.find()) {
            invalidCharacterMatcher.reset();
            final List<String> FOUND_INVALID_CHARACTERS =
                    invalidCharacterMatcher.results().map(MatchResult::group).distinct().toList();
            final boolean MORE_THAN_1_INVALID_CHAR = FOUND_INVALID_CHARACTERS.size() > 1;
            final String CLEANED_QUERY_STRING = queryString.replaceAll(REGEX_INVALID_INPUT_CHARACTERS, "");
            System.err.println("Invalid character"
                    + (MORE_THAN_1_INVALID_CHAR ? "s" : "")
                    + " found {" + String.join(", ", FOUND_INVALID_CHARACTERS) + "}: "
                    + (MORE_THAN_1_INVALID_CHAR ? "they" : "it")
                    + " will be removed from the input query string " +
                    "({" + queryString + "} becomes {" + CLEANED_QUERY_STRING + "}).");
            queryString = CLEANED_QUERY_STRING;
        }

        return queryString;
    }

    /**
     * This method works like "toString" and can be used to print {@link Expression}s.
     *
     * @param expression The expression that is desired to print.
     * @return the {@link String} representation of the given {@link Expression}.
     */
    @NotNull
    public static String toString(@Nullable Expression<?> expression) {
        if (expression == null) {
            return "";
        } else {
            String toString = expression.toString();
            for (var bo : BINARY_OPERATOR.values()) {
                toString = toString.replaceAll("\\" + bo.getSymbol(), bo.toString());
            }
            return toString.replaceAll("\\" + NOT.getSymbol(), "  " + NOT + " ");
        }
    }

}
