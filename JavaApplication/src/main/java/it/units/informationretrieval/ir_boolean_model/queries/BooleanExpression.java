package it.units.informationretrieval.ir_boolean_model.queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndex;
import it.units.informationretrieval.ir_boolean_model.entities.Posting;
import it.units.informationretrieval.ir_boolean_model.entities.PostingList;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * An instance of this class is a boolean expression to be evaluated
 * for answering a query.
 * An instance of this class can be "aggregated": this means that it
 * is composed of left-child and right-child boolean expressions (it
 * is a binary expression), which can be again composed by left-child
 * and right-child and so on, to form a hierarchical structure to be
 * respected when the overall resulting boolean expression will be
 * evaluated. If an expression is not aggregated, it will optionally
 * have a unary operator (e.g, negation, i.e., 'NOT') and to "evaluate
 * it" means to "apply it to sentence and evaluate if the matching value
 * represented by the instance is contained in the sentence to which it
 * is applied". If the instance is not aggregated, it cannot have a
 * binary operator.
 *
 * @author Matteo Ferfoglia
 */
public class BooleanExpression {

    /**
     * Flag which is true if this instance is an aggregated expression.
     * See the description of {@link BooleanExpression this class}.
     */
    private final boolean isAggregated;

    /**
     * The value to match.
     */
    @Nullable   // if there is matching phrase
    private final String matchingValue;

    /**
     * The phrase to match (for a phrasal query), as a {@link List} of
     * {@link String}s.
     */
    @Nullable   // if there is matching value
    private final List<String> matchingPhrase;

    /**
     * If this instance has to match a phrase, this attribute (which has the size
     * of {@link #matchingPhrase} -1) specifies at the k-th position the maximum
     * distance (number of terms) which can be present between the k-th and the
     * (k+1)-th term specified in {@link #matchingValue}.
     */
    @Nullable   // if the matchingPhrase is null
    private final List<Integer> matchingPhraseMaxDistance;

    /**
     * The left-child (first) expression to evaluate (for aggregated expressions).
     */
    @Nullable   // if this expression is not aggregated
    private final BooleanExpression leftChildOperand;

    /**
     * The right-child (second) expression to evaluate (for aggregated expressions).
     */
    @Nullable   // if this expression is not aggregated
    private final BooleanExpression rightChildOperand;

    /**
     * The {@link UNARY_OPERATOR} to apply to this instance.
     */
    @Nullable   //  if this expression is aggregated
    private final UNARY_OPERATOR unaryOperator;

    /**
     * The {@link BINARY_OPERATOR} to apply to this instance.
     * It must be null if this is a non-aggregated instance.
     */
    @Nullable   // if this expression is not aggregated
    private final BINARY_OPERATOR binaryOperator;

    /**
     * The {@link InformationRetrievalSystem} to use for evaluating this instance.
     */
    @NotNull
    private final InformationRetrievalSystem informationRetrievalSystem;

    /**
     * Constructor. Creates a non-aggregated {@link BooleanExpression}.
     *
     * @param unaryOperator              The {@link UNARY_OPERATOR} to use.
     * @param matchingValue              The {@link #matchingValue}. Must be null if matchingPhrase is non-null and cannot be null if matchingPhrase is null.
     * @param matchingPhrase             The {@link #matchingPhrase}. Must be null if matchingValue is non-null and cannot be null if matchingValue is null.
     * @param matchingPhraseMaxDistance  The {@link #matchingPhraseMaxDistance}.
     *                                   If the matchingPhrase is specified but this parameter is null,
     *                                   then '1' will be considered as the maximum distance.
     * @param informationRetrievalSystem The {@link InformationRetrievalSystem} on which to perform the query.
     * @throws IllegalArgumentException If both the parameters are null or non-null, or if the size of
     *                                  matchingPhraseMaxDistance parameter list is not equal to the size
     *                                  of the matchingPhrase minus one (if specified), or if the
     *                                  matchingPhraseMaxDistance contains non-positive (0 included)
     *                                  values.
     */ // TODO : a lot of params: valuate if using builder pattern
    private BooleanExpression(@Nullable UNARY_OPERATOR unaryOperator,
                              @Nullable String matchingValue,
                              @Nullable List<String> matchingPhrase,
                              @Nullable List<Integer> matchingPhraseMaxDistance,
                              @NotNull final InformationRetrievalSystem informationRetrievalSystem)
            throws IllegalArgumentException {

        this.informationRetrievalSystem = informationRetrievalSystem;

        // Check parameters

        if (matchingValue == null && matchingPhrase == null) {
            throw new IllegalArgumentException("Either the matching value or the matching phrase must be non-null");
        }
        if (matchingValue != null && matchingPhrase != null) {
            throw new IllegalArgumentException("Either the matching value or the matching phrase must be null");
        }

        if (matchingPhrase == null) {
            this.matchingPhraseMaxDistance = null;
        } else {
            if (matchingPhraseMaxDistance == null) {
                this.matchingPhraseMaxDistance = Collections.nCopies(
                        Math.max(0, matchingPhrase.size() - 1),
                        1
                );
            } else {
                if (matchingPhraseMaxDistance.size() != matchingPhrase.size() - 1) {
                    throw new IllegalArgumentException("The size for the max distance list must be equal to the size of the phrase minus one");
                } else {
                    if (matchingPhraseMaxDistance.stream().unordered().parallel().anyMatch(x -> x <= 0)) {
                        throw new IllegalArgumentException("The distances must be positive.");
                    } else {    // Valida parameter
                        this.matchingPhraseMaxDistance = matchingPhraseMaxDistance;
                    }
                }
            }
        }


        // Initializations

        String tmp = matchingValue == null ? null : Utility.normalize(matchingValue);
        this.isAggregated = false;
        this.leftChildOperand = null;
        this.rightChildOperand = null;
        this.matchingValue = tmp == null ? "" : tmp;  // TODO : if empty string, the expression should match any document (TEST this behaviour!)
        this.matchingPhrase = matchingPhrase;
        this.unaryOperator = unaryOperator == null ? UNARY_OPERATOR.IDENTITY : unaryOperator;
        this.binaryOperator = null;

    }

    /**
     * Constructor for {@link BooleanExpression}s with a {@link #matchingValue}.
     *
     * @param unaryOperator The {@link UNARY_OPERATOR} to use. If it is null, {@link UNARY_OPERATOR#IDENTITY} will be use.
     * @param matchingValue The value to match.
     */
    public BooleanExpression(@Nullable UNARY_OPERATOR unaryOperator, @NotNull String matchingValue,
                             @NotNull final InformationRetrievalSystem informationRetrievalSystem) {
        this(unaryOperator, Objects.requireNonNull(matchingValue, "The given value cannot be null"), null, null, informationRetrievalSystem);
    }

    /**
     * Constructor for {@link BooleanExpression}s with a {@link #matchingPhrase},
     * supposing that each term in the given list can be at most one term spaced
     * each other.
     *
     * @param unaryOperator  The {@link UNARY_OPERATOR} to use. If it is null, {@link UNARY_OPERATOR#IDENTITY} will be use.
     * @param matchingPhrase The phrase to match.
     */
    public BooleanExpression(@Nullable UNARY_OPERATOR unaryOperator, @NotNull List<String> matchingPhrase,
                             @NotNull final InformationRetrievalSystem informationRetrievalSystem) {
        this(unaryOperator, null, Objects.requireNonNull(matchingPhrase, "The given value cannot be null"), null, informationRetrievalSystem);
    }

    /**
     * Constructor for {@link BooleanExpression}s with a {@link #matchingPhrase},
     * supposing that each term in the given list can be at most one term spaced
     * each other.
     *
     * @param unaryOperator             The {@link UNARY_OPERATOR} to use. If it is null, {@link UNARY_OPERATOR#IDENTITY} will be use.
     * @param matchingPhrase            The phrase to match.
     * @param matchingPhraseMaxDistance The maximum possible distance between terms of
     *                                  the given phrase. The list must have the size
     *                                  equals to the size of the phrase minus one and can
     *                                  contain only positive (0 excluded) values.
     */
    public BooleanExpression(@Nullable UNARY_OPERATOR unaryOperator,
                             @NotNull List<String> matchingPhrase,
                             @NotNull List<Integer> matchingPhraseMaxDistance,
                             @NotNull final InformationRetrievalSystem informationRetrievalSystem) {
        this(unaryOperator, null, Objects.requireNonNull(matchingPhrase, "The given value cannot be null"), Objects.requireNonNull(matchingPhraseMaxDistance), informationRetrievalSystem);
    }

    /**
     * Constructor. Create an aggregated {@link BooleanExpression} starting from two {@link BooleanExpression}s.
     *
     * @param operator The {@link BINARY_OPERATOR} to use.
     * @param expr1    The first operand.
     * @param expr2    The second operand.
     */
    public BooleanExpression(@NotNull BINARY_OPERATOR operator,
                             @NotNull BooleanExpression expr1,
                             @NotNull BooleanExpression expr2) {
        this.informationRetrievalSystem = Objects.requireNonNull(
                Objects.equals(expr1.informationRetrievalSystem, expr2.informationRetrievalSystem)
                        ? expr1.informationRetrievalSystem : null,
                "Given input parameters must refere to the same non-null IR System.");
        this.isAggregated = true;
        this.leftChildOperand = Objects.requireNonNull(expr1);
        this.rightChildOperand = Objects.requireNonNull(expr2);
        this.matchingValue = null;
        this.matchingPhrase = null;
        this.unaryOperator = null;
        this.binaryOperator = Objects.requireNonNull(operator);
        this.matchingPhraseMaxDistance = null;  // TODO: valuate if using builder pattern
    }

    /**
     * Evaluate this expression on the given {@link InvertedIndex}.
     *
     * @return the {@link PostingList} matching this {@link BooleanExpression}.
     * @throws UnsupportedOperationException If the operator for the expression is unknown.
     */
    @NotNull
    private List<Posting> evaluate()
            throws UnsupportedOperationException {

        if (isAggregated) {

            List<BooleanExpression> booleanExpressions = new ArrayList<>(2);
            booleanExpressions.add(leftChildOperand);
            booleanExpressions.add(rightChildOperand);
            return booleanExpressions
                    .stream().unordered().parallel()
                    .map(BooleanExpression::evaluate)
                    .reduce((listOfPostings1, listOfPostings2) ->
                            switch (Objects.requireNonNull(binaryOperator)) {
                                case AND -> Utility.intersectionOfSortedLists(listOfPostings1, listOfPostings2);
                                case OR -> Utility.unionOfSortedLists(listOfPostings1, listOfPostings2);
                                //noinspection UnnecessaryDefault
                                default -> throw new UnsupportedOperationException("Unknown operator");
                            })
                    .orElse(new ArrayList<>());

        } else {

            if (matchingValue != null) {
                String normalizedToken = Utility.normalize(matchingValue);  // TODO : should be in the constructor?
                if (normalizedToken == null) {
                    // The normalization return null, then no matches
                    return new ArrayList<>();
                } else {
                    return informationRetrievalSystem.getListOfPostingForToken(normalizedToken);
                }
            } else if (matchingPhrase != null) {
                throw new UnsupportedOperationException("Not implemented yet");
                // TODO : implement for phrasal ir_system.queries (not implemented yet)
            } else {
                throw new NullPointerException("The matching value either the matching phrase were null but they sould not.");
            }
            // TODO : implement query with NOT

        }

    }

    /**
     * Evaluate this expression on the given {@link InvertedIndex}.
     *
     * @param invertedIndex The {@link InvertedIndex}.
     * @return the {@link Document}s matching this {@link BooleanExpression}.
     * @throws UnsupportedOperationException If the operator for the expression is unknown.
     */
    @NotNull
    public List<Document> evaluate(@NotNull final InvertedIndex invertedIndex)
            throws UnsupportedOperationException {
        return invertedIndex.getCorpus()
                .getDocuments(
                        evaluate()
                                .stream()
                                .map(Posting::getDocId)
                                .distinct()
                                .toList());// TODO: message chain code smell
        // TODO : implement ranking and sort results accordingly
    }

    /**
     * Enumeration for possible unary operators to apply on a {@link BooleanExpression}.
     */
    public enum UNARY_OPERATOR {
        /**
         * IDENTITY. No operator is applied.
         */
        IDENTITY,

        /**
         * NOT operator. If it is applied on a non-aggregated {@link BooleanExpression},
         * it search for documents which do not have the matching value of that {@link
         * BooleanExpression}; if it is applied to an aggregated {@link BooleanExpression},
         * it search for documents which do <strong>not</strong> match the given expression.
         */
        NOT
    }

    /**
     * Enumeration for possible binary operators to apply on two {@link BooleanExpression}s,
     * which are the operands.
     */
    public enum BINARY_OPERATOR {
        /**
         * AND operator. Both the {@link BooleanExpression}s (operands) must hold.
         */
        AND,

        /**
         * OR operator. At least one of the {@link BooleanExpression}s (operands) must hold.
         */
        OR
    }

    // TODO: query optimization
}