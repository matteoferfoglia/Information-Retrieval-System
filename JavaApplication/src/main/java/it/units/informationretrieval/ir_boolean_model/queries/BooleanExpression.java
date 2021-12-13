package it.units.informationretrieval.ir_boolean_model.queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.*;
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
 * have a unary operator (e.g, negation, i.e., 'NOT') and to "evaluateBothSimpleAndAggregatedExpressionRecursively
 * it" means to "apply it to sentence and evaluateBothSimpleAndAggregatedExpressionRecursively if the matching value
 * represented by the instance is contained in the sentence to which it
 * is applied". If the instance is not aggregated, it cannot have a
 * binary operator.
 *
 * @author Matteo Ferfoglia
 */
public class BooleanExpression {    // TODO: implement factory pattern which takes as input the IR System to use (only once) and allows to create boolean expression without asking for the inverted index. Each boolean expression must know its factory and two boolean expression of different factories cannot be aggregated

    /**
     * Flag which is true if this instance is an aggregated expression.
     * See the description of {@link BooleanExpression this class}.
     */
    private final boolean isAggregated;

    /**
     * The value to match.
     */
    @Nullable   // if there is matching phrase
    private String matchingValue;

    /**
     * The phrase to match (for a phrasal query), as a {@link List} of
     * {@link String}s.
     */
    @Nullable   // if there is matching value
    private List<String> matchingPhrase;

    /**
     * If this instance has to match a phrase, this attribute (which has the size
     * of {@link #matchingPhrase} -1) specifies at the k-th position the maximum
     * distance (number of terms) which can be present between the k-th and the
     * (k+1)-th term specified in {@link #matchingValue}.
     */
    @Nullable   // if the matchingPhrase is null
    private List<Integer> matchingPhraseMaxDistance;

    /**
     * The left-child (first) expression to evaluateBothSimpleAndAggregatedExpressionRecursively (for aggregated expressions).
     */
    @Nullable   // if this expression is not aggregated
    private final BooleanExpression leftChildOperand;

    /**
     * The right-child (second) expression to evaluateBothSimpleAndAggregatedExpressionRecursively (for aggregated expressions).
     */
    @Nullable   // if this expression is not aggregated
    private final BooleanExpression rightChildOperand;

    /**
     * The {@link UNARY_OPERATOR} to apply to this instance.
     */
    @Nullable   //  if this expression is aggregated
    private UNARY_OPERATOR unaryOperator;

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
     * Constructor. Creates a non-aggregated expression.
     *
     * @param informationRetrievalSystem The {@link InformationRetrievalSystem} on which the query must be performed.
     */
    protected BooleanExpression(@NotNull final InformationRetrievalSystem informationRetrievalSystem) {
        this.isAggregated = false;
        this.leftChildOperand = null;
        this.rightChildOperand = null;
        this.unaryOperator = UNARY_OPERATOR.IDENTITY;
        this.binaryOperator = null;
        this.informationRetrievalSystem = Objects.requireNonNull(informationRetrievalSystem);
    }

    /**
     * Copy Constructor.
     *
     * @param booleanExpression The instance to be copied.
     */
    private BooleanExpression(@NotNull BooleanExpression booleanExpression) throws IllegalArgumentException {
        this.isAggregated = booleanExpression.isAggregated;
        this.matchingValue = booleanExpression.matchingValue;
        this.matchingPhrase = booleanExpression.matchingPhrase;
        this.unaryOperator = booleanExpression.unaryOperator;
        this.binaryOperator = booleanExpression.binaryOperator;
        this.informationRetrievalSystem = booleanExpression.informationRetrievalSystem;
        this.matchingPhraseMaxDistance = booleanExpression.matchingPhraseMaxDistance;
        this.leftChildOperand = booleanExpression.leftChildOperand;
        this.rightChildOperand = booleanExpression.rightChildOperand;
    }

    /**
     * Constructor. Create an aggregated {@link BooleanExpression} starting from two {@link BooleanExpression}s.
     *
     * @param operator The {@link BINARY_OPERATOR} to use.
     * @param expr1    The first operand.
     * @param expr2    The second operand.
     */
    private BooleanExpression(@NotNull BINARY_OPERATOR operator,
                              @NotNull BooleanExpression expr1,
                              @NotNull BooleanExpression expr2) {
        if (Objects.requireNonNull(expr1.informationRetrievalSystem).equals(expr2.informationRetrievalSystem)) {
            this.informationRetrievalSystem = expr1.informationRetrievalSystem;
            this.isAggregated = true;
            this.leftChildOperand = Objects.requireNonNull(expr1);
            this.rightChildOperand = Objects.requireNonNull(expr2);
            this.binaryOperator = Objects.requireNonNull(operator);
        } else {
            throw new IllegalStateException("Impossible to create an aggregate expression with children" +
                    " expressions referring to different IR Systems.");
        }
    }

    /**
     * @return the result of {@link Utility#normalize(String)} or an empty
     * string if {@link Utility#normalize(String)} returns null.
     */
    @NotNull
    private static String normalizeToken(String inputWord) {
        String normalized = Utility.normalize(inputWord);
        return normalized == null ? "" : normalized;
    }

    /**
     * Sets the {@link #matchingValue}.
     *
     * @param matchingValue The value to match.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingValue(@NotNull String matchingValue) {    // todo: needed to separate matching value and matching phrase
        // TODO: test
        throwIfIsAggregated();
        if (isMatchingPhraseSet()) {
            throw new IllegalStateException("Matching phrase already set, cannot set matching value too.");
        }

        this.matchingValue = normalizeToken(Objects.requireNonNull(matchingValue));
        return this;
    }

    /**
     * @throws IllegalStateException if {@link #isAggregated} is true.
     */
    private void throwIfIsAggregated() {
        if (isAggregated) {
            throw new IllegalStateException("Impossible to set in aggregated expressions.");
        }
    }

    /**
     * Sets the {@link #matchingPhrase} and the {@link #matchingPhraseMaxDistance}.
     *
     * @param matchingPhrase            The phrase to match.
     * @param matchingPhraseMaxDistance The value for {@link #matchingPhraseMaxDistance}.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingPhrase(@NotNull List<String> matchingPhrase,
                                               @NotNull List<Integer> matchingPhraseMaxDistance) {    // todo: needed to separate matching value and matching phrase?
        // TODO: test
        // TODO: similar to previous method
        throwIfIsAggregated();
        if (isMatchingValueSet()) {
            throw new IllegalStateException("Matching value already set, cannot set matching value too.");
        }

        if (matchingPhraseMaxDistance.size() != matchingPhrase.size() - 1) {
            throw new IllegalArgumentException("The size for the max distance list must be equal to the size of the phrase minus one");
        } else if (matchingPhraseMaxDistance.stream().unordered().parallel().anyMatch(x -> x <= 0)) {
            throw new IllegalArgumentException("The distances must be positive.");
        }
        this.matchingPhraseMaxDistance = Objects.requireNonNull(matchingPhraseMaxDistance);

        this.matchingPhrase = Objects.requireNonNull(matchingPhrase).stream().map(BooleanExpression::normalizeToken).toList();
        return this;
    }

    /**
     * Sets the {@link #matchingPhrase} and the {@link #matchingPhraseMaxDistance} considering
     * that values in the phrase are adjacent.
     *
     * @param matchingPhrase The phrase to match.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingPhrase(@NotNull List<String> matchingPhrase) {    // todo: needed to separate matching value and matching phrase?
        // TODO: test
        return setMatchingPhrase(matchingPhrase, Collections.nCopies(Math.max(0, matchingPhrase.size() - 1), 1));
    }

    /**
     * Sets the {@link #unaryOperator}.
     *
     * @param unaryOperator The value for the {@link #unaryOperator}.
     * @return the instance after having set the value.
     */
    public BooleanExpression setUnaryOperator(@NotNull final UNARY_OPERATOR unaryOperator) {
        this.unaryOperator = Objects.requireNonNull(unaryOperator);
        return this;
    }

    /**
     * @return true if the {@link #matchingValue} is set, false otherwise.
     */
    private boolean isMatchingValueSet() {
        return matchingValue != null;
    }

    /**
     * @return true if the {@link #matchingPhrase} is set, false otherwise.
     */
    private boolean isMatchingPhraseSet() {
        return matchingPhrase != null;
    }

    /**
     * Updates this instance such that the AND boolean operator is computed
     * between this instance and the one passed as parameter.
     *
     * @param other The other instance (operand) for the AND operation.
     * @return This instance after setting the AND operand.
     */
    public BooleanExpression and(@NotNull BooleanExpression other) {
        return new BooleanExpression(BINARY_OPERATOR.AND, this, other);
    }

    /**
     * Updates this instance such that the OR boolean operator is computed
     * between this instance and the one passed as parameter.
     *
     * @param other The other instance (operand) for the OR operation.
     * @return This instance after setting the OR operand.
     */
    public BooleanExpression or(@NotNull BooleanExpression other) {
        return new BooleanExpression(BINARY_OPERATOR.OR, this, other);
    }

    /**
     * Negates the current instance.
     *
     * @return the instance corresponding to the negation.
     */
    public BooleanExpression not() {
        return setUnaryOperator(UNARY_OPERATOR.NOT);
    }

    /**
     * Evaluate this expression on the given {@link InvertedIndex}.
     *
     * @return the {@link PostingList} matching this {@link BooleanExpression}.
     * @throws UnsupportedOperationException If the operator for the expression is unknown.
     */
    @NotNull
    private List<Posting> evaluateBothSimpleAndAggregatedExpressionRecursively()
            throws UnsupportedOperationException {

        if (isAggregated) {

            List<BooleanExpression> booleanExpressions = new ArrayList<>(2);
            booleanExpressions.add(leftChildOperand);
            booleanExpressions.add(rightChildOperand);
            return booleanExpressions
                    .stream().unordered().parallel()
                    .map(BooleanExpression::evaluateBothSimpleAndAggregatedExpressionRecursively)
                    .reduce((listOfPostings1, listOfPostings2) ->
                            switch (Objects.requireNonNull(binaryOperator)) {
                                case AND -> Utility.intersectionOfSortedLists(listOfPostings1, listOfPostings2);
                                case OR -> Utility.unionOfSortedLists(listOfPostings1, listOfPostings2);
                                //noinspection UnnecessaryDefault
                                default -> throw new UnsupportedOperationException("Unknown operator");
                            })
                    .orElse(new ArrayList<>());

        } else {

            if (isMatchingValueSet()) {
                String normalizedToken = Utility.normalize(matchingValue);  // TODO : should be in the constructor?
                if (normalizedToken == null) {
                    // The normalization return null, then no matches
                    return new ArrayList<>();
                } else {
                    List<Posting> listOfPostingsForNormalizedInputToken =
                            informationRetrievalSystem.getListOfPostingForToken(normalizedToken);

                    assert unaryOperator != null;
                    return switch (unaryOperator) {
                        case NOT -> {   // TODO: try to improve query not
                            List<DocumentIdentifier> listOfDocIdToBeExcluded =
                                    listOfPostingsForNormalizedInputToken.stream().map(Posting::getDocId).toList();
                            yield informationRetrievalSystem.getAllDocIds()
                                    .stream().unordered().parallel()
                                    .filter(docId -> !listOfDocIdToBeExcluded.contains(docId))
                                    .map(Posting::new)
                                    .sorted()
                                    .toList();
                        }
                        case IDENTITY -> informationRetrievalSystem.getListOfPostingForToken(normalizedToken);
                    };
                }
            } else if (isMatchingPhraseSet()) {
                throw new UnsupportedOperationException("Not implemented yet");
                // TODO : implement for phrasal ir_system.queries (not implemented yet)
            } else {
                throw new NullPointerException("The matching value either the matching phrase were null but they sould not.");
            }

        }

    }

    /**
     * Evaluate this expression on the given {@link InvertedIndex}.
     *
     * @return the {@link Document}s matching this {@link BooleanExpression}.
     * @throws UnsupportedOperationException If the operator for the expression is unknown.
     */
    @NotNull
    public List<Document> evaluate()
            throws UnsupportedOperationException {
        return informationRetrievalSystem
                .getInvertedIndex()
                .getCorpus()
                .getDocuments(
                        evaluateBothSimpleAndAggregatedExpressionRecursively()
                                .stream()
                                .map(Posting::getDocId)
                                .distinct()
                                .toList());// TODO: message chain code smell
        // TODO : implement ranking and sort results accordingly and test the correct sorting of results
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
// TODO: query expansion and reformulation
}