package it.units.informationretrieval.ir_boolean_model.queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.*;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skiplist.SkipList;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
public class BooleanExpression {

    /**
     * Flag which is true if this instance is an aggregated expression.
     * See the description of {@link BooleanExpression this class}.
     */
    private final boolean isAggregated;
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
     * Flag: true if the {@link #maxNumberOfResults} is specified.
     */
    private boolean maxNumberOfResultsSpecified = false;
    /**
     * The maximum number of results that must be showed.
     */
    private int maxNumberOfResults = -1;
    /**
     * The value to match.
     */
    @Nullable   // not null if there is matching value, null otherwise
    private String matchingValue;
    /**
     * The phrase to match (for a phrasal query), as a {@link List} of
     * {@link String}s.
     */
    @Nullable   // not null if there is matching phrase, null otherwise
    private Phrase matchingPhrase;
    /**
     * The {@link UNARY_OPERATOR} to apply to this instance.
     */
    @NotNull
    private UNARY_OPERATOR unaryOperator = UNARY_OPERATOR.IDENTITY;// default is the unary operator

    /**
     * Constructor. Creates a non-aggregated expression.
     *
     * @param informationRetrievalSystem The {@link InformationRetrievalSystem} on which the query must be performed.
     */
    protected BooleanExpression(@NotNull final InformationRetrievalSystem informationRetrievalSystem) {
        this.isAggregated = false;
        this.leftChildOperand = null;
        this.rightChildOperand = null;
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
     * Sets the {@link #matchingValue}.
     *
     * @param matchingValue The value to match.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingValue(@NotNull String matchingValue) {    // todo: needed to separate matching value and matching phrase?
        throwIfIsAggregated();
        if (isMatchingPhraseSet()) {
            throw new IllegalStateException("Matching phrase already set, cannot set matching value too.");
        } else if (isMatchingValueSet()) {
            throw new IllegalStateException("Matching value already set, cannot re-set");
        }
        this.matchingValue = Utility.normalize(matchingValue);
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
     * Sets the {@link #matchingPhrase}.
     *
     * @param matchingPhrase            The phrase to match.
     * @param matchingPhraseMaxDistance Array with the exact distance allowed between words, saved as array:
     *                                  specifies at the k-th position the exact distance (number of terms)
     *                                  which must be present between the term at index k in the phrase and
     *                                  the term at position 0.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingPhrase(String[] matchingPhrase, int[] matchingPhraseMaxDistance) {    // todo: needed to separate matching value and matching phrase?
        throwIfIsAggregated();
        if (isMatchingValueSet()) {
            throw new IllegalStateException("Matching value already set, cannot set matching value too.");
        } else if (isMatchingPhraseSet()) {
            throw new IllegalStateException("Matching phrase already set, cannot re-set");
        }
        String[] phrase = Arrays.stream(matchingPhrase).map(Utility::normalize).filter(Objects::nonNull).toArray(String[]::new);
        if (phrase.length == 1) {
            // normalization lead to a phrase of a single word (hence, it is not a phrase anymore)
            return setMatchingValue(phrase[0]);
        } else {
            this.matchingPhrase = new Phrase(phrase, matchingPhraseMaxDistance);
            return this;
        }
    }

    /**
     * Sets the {@link #maxNumberOfResults}.
     *
     * @param maxNumberOfResults the maximum number of results that must be returned
     *                           by {@link #evaluate()}.
     * @return this instance after executing the method.
     * @throws IllegalArgumentException if a parameter &lt; 0 is provided.
     */
    public BooleanExpression limit(int maxNumberOfResults) throws IllegalArgumentException {
        final int MIN_ALLOWED_VALUE_INCLUDED = 0;
        if (maxNumberOfResults < MIN_ALLOWED_VALUE_INCLUDED) {
            throw new IllegalArgumentException(
                    "The minimum allowed value for the parameter is " + MIN_ALLOWED_VALUE_INCLUDED
                            + ", but " + maxNumberOfResults + " found.");
        }
        this.maxNumberOfResults = maxNumberOfResults;
        this.maxNumberOfResultsSpecified = true;
        return this;
    }

    /**
     * Sets the {@link #matchingPhrase} considering that values in the phrase are all adjacent.
     *
     * @param matchingPhrase The phrase to match.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingPhrase(String[] matchingPhrase) {
        return setMatchingPhrase(matchingPhrase, IntStream.range(1, matchingPhrase.length).toArray());
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
     * @throws IllegalStateException if neither the {@link #matchingValue} nor the {@link #matchingPhrase} is set.
     */
    private void throwIfNotAggregatedButNeitherValueNorPhraseToMatchIsSet() {
        if (!(isAggregated || isMatchingValueSet() || isMatchingPhraseSet())) {
            throw new IllegalStateException("Neither matching value or phrase is set.");
        }
    }

    //region query AND

    /**
     * Updates this instance such that the AND boolean operator is computed
     * between this instance and the one passed as parameter.
     *
     * @param other The other instance (operand) for the AND operation.
     * @return This instance after setting the AND operand.
     */
    public BooleanExpression and(@NotNull BooleanExpression other) {
        throwIfNotAggregatedButNeitherValueNorPhraseToMatchIsSet();
        return new BooleanExpression(BINARY_OPERATOR.AND, this, other);
    }

    /**
     * Like {@link #and(BooleanExpression)}, but accepts a word directly.
     */
    public BooleanExpression and(@NotNull String matchingValue) {
        return and(new BooleanExpression(informationRetrievalSystem).setMatchingValue(Objects.requireNonNull(matchingValue)));
    }

    /**
     * Like {@link #and(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression and(String[] matchingPhrase) {    // TODO: test and benchmark
        return and(new BooleanExpression(informationRetrievalSystem).setMatchingPhrase(Objects.requireNonNull(matchingPhrase)));
    }

    /**
     * Like {@link #and(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression and(String[] matchingPhrase, int[] matchingPhraseMaxDistance) {// TODO: test and benchmark
        return and(
                new BooleanExpression(informationRetrievalSystem)
                        .setMatchingPhrase(
                                Objects.requireNonNull(matchingPhrase), Objects.requireNonNull(matchingPhraseMaxDistance)));
    }
    //endregion

    //region query OR

    /**
     * Updates this instance such that the OR boolean operator is computed
     * between this instance and the one passed as parameter.
     *
     * @param other The other instance (operand) for the OR operation.
     * @return This instance after setting the OR operand.
     */
    public BooleanExpression or(@NotNull BooleanExpression other) {
        throwIfNotAggregatedButNeitherValueNorPhraseToMatchIsSet();
        return new BooleanExpression(BINARY_OPERATOR.OR, this, other);
    }

    /**
     * Like {@link #or(BooleanExpression)}, but accepts a word directly.
     */
    public BooleanExpression or(@NotNull String matchingValue) {
        return or(new BooleanExpression(informationRetrievalSystem).setMatchingValue(Objects.requireNonNull(matchingValue)));
    }

    /**
     * Like {@link #or(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression or(String[] matchingPhrase) {// TODO: test and benchmark
        return or(new BooleanExpression(informationRetrievalSystem).setMatchingPhrase(Objects.requireNonNull(matchingPhrase)));
    }

    /**
     * Like {@link #or(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression or(String[] matchingPhrase, int[] matchingPhraseMaxDistance) {// TODO: test and benchmark
        return or(
                new BooleanExpression(informationRetrievalSystem)
                        .setMatchingPhrase(Objects.requireNonNull(matchingPhrase), Objects.requireNonNull(matchingPhraseMaxDistance)));
    }
    //endregion

    /**
     * Negates the current instance.
     *
     * @return the instance corresponding to the negation.
     */
    public BooleanExpression not() {
        return isNotQuery() ?
                setUnaryOperator(UNARY_OPERATOR.IDENTITY/*negation of NOT is the identity*/) :
                setUnaryOperator(UNARY_OPERATOR.NOT);
    }

    /**
     * @return true if {@link #unaryOperator} is NOT, false otherwise.
     */
    private boolean isNotQuery() {
        return unaryOperator.equals(UNARY_OPERATOR.NOT);
    }

    /**
     * Evaluate this expression on the given {@link InvertedIndex}.
     *
     * @return the {@link PostingList} matching this {@link BooleanExpression}.
     * @throws UnsupportedOperationException If the operator for the expression is unknown.
     */
    @NotNull
    private synchronized SkipList<Posting> evaluateBothSimpleAndAggregatedExpressionRecursively()
            throws UnsupportedOperationException {

        return switch (unaryOperator) {
            case NOT -> {   // TODO: try to improve query not
                // First: solve the direct query (create a new query without the NOT operator),
                // then take the difference to get the results for the NOT query.
                List<DocumentIdentifier> listOfDocIdToBeExcluded =
                        new BooleanExpression(this)
                                .setUnaryOperator(UNARY_OPERATOR.IDENTITY)
                                .evaluateBothSimpleAndAggregatedExpressionRecursively()
                                .stream().map(Posting::getDocId).toList();
                yield new SkipList<>(
                        informationRetrievalSystem.getAllDocIds()
                                .stream().unordered()
                                .filter(docId -> !listOfDocIdToBeExcluded.contains(docId))
                                .map(docId -> new Posting(docId, new int[0]/*TODO: positions NOT handled!!!!!*/))
                                .toList());
            }
            case IDENTITY -> {
                if (isAggregated) {
                    yield Stream.of(leftChildOperand, rightChildOperand)
                            .unordered()
                            .map(BooleanExpression::evaluateBothSimpleAndAggregatedExpressionRecursively)
                            .reduce((listOfPostings1, listOfPostings2) -> {
                                SkipList<Posting> postings1 = new SkipList<>(listOfPostings1);
                                SkipList<Posting> postings2 = new SkipList<>(listOfPostings2);
                                return switch (Objects.requireNonNull(binaryOperator)) {
                                    case AND -> Utility.intersection(postings1, postings2);
                                    case OR -> Utility.union(postings1, postings2);
                                    //noinspection UnnecessaryDefault
                                    default -> throw new UnsupportedOperationException("Unknown operator");
                                };
                            })
                            .orElse(new SkipList<>());

                } else {

                    if (isMatchingPhraseSet()) {
                        //noinspection unchecked    // generic array creation
                        BiPredicate<Posting, Posting>[] biPredicatesForCheckingPositionsForPhrasalQueries =
                                IntStream.range(0, matchingPhrase.size() - 1)
                                        .mapToObj(i -> (BiPredicate<Posting, Posting>) (Posting posting1, Posting posting2) -> {
                                            // Note: order of input arg is important

                                            var positions1 = posting1.getTermPositionsInTheDocument();
                                            var positions2 = posting2.getTermPositionsInTheDocument();

                                            // assert positions were sorted
                                            assert Arrays.stream(positions1).sorted().boxed().toList().equals(Arrays.stream(positions1).boxed().toList());
                                            assert Arrays.stream(positions2).sorted().boxed().toList().equals(Arrays.stream(positions2).boxed().toList());

                                            int index1 = 0, index2 = 0;
                                            while (index1 < positions1.length && index2 < positions2.length) {
                                                int numberOfTermsBetweenWords =
                                                        positions2[index2] - positions1[index1]             // number of words between the *first* word of phrase and the word of posting2  (Note: when inserting a posting to the intersection list, only the posting referencing the first word of phrase is kept, hence distances must be computed respect the position of the first word)
                                                                - matchingPhrase.distanceFromFirstWord[i];   // exact distance allowed between *first* word and the word of posting 2
                                                if (numberOfTermsBetweenWords == 0) {
                                                    // words are adjacent
                                                    return true;
                                                } else if (numberOfTermsBetweenWords < 0) {
                                                    // word from posting2 is present in the document before the word of posting1
                                                    index2++;
                                                } else {
                                                    // word from posting1 is present in the document before the word of posting2
                                                    index1++;
                                                }
                                            }
                                            return false;   // no adjacency found
                                        })
                                        .toArray(BiPredicate[]::new);

                        assert matchingPhrase.words.length >= 2;

                        Map<String, SkipList<Posting>> cachedPostings =  // in phrases, common words (like articles) might be present more than once (if they were not excluded by normalization steps) and it would not be efficient to retrieve the corresponding posting list each time
                                new ConcurrentHashMap<>(matchingPhrase.size());

                        IntFunction<@NotNull SkipList<Posting>> getPostingListOfIthWordInPhrase = i -> {
                            assert i >= 0 && i < matchingPhrase.size();
                            String word = matchingPhrase.words[i];
                            var correspondingPostingList = cachedPostings.get(word);
                            if (correspondingPostingList == null) {
                                // posting list for the term was not cached
                                correspondingPostingList = new SkipList<>(informationRetrievalSystem
                                        .getListOfPostingForToken(word));// TODO: do not create a new SkipList instance if it is already returned by getListOfPostingForToken
                                cachedPostings.put(word, correspondingPostingList);
                            }
                            return correspondingPostingList;
                        };

                        SkipList<Posting> phraseQueryIntersection = getPostingListOfIthWordInPhrase.apply(0);
                        for (int i = 1; !phraseQueryIntersection.isEmpty() && i < matchingPhrase.size(); i++) {
                            SkipList<Posting> postings = getPostingListOfIthWordInPhrase.apply(i);
                            phraseQueryIntersection = SkipList.intersection(
                                    phraseQueryIntersection, postings, biPredicatesForCheckingPositionsForPhrasalQueries[i - 1]);
                        }

                        yield phraseQueryIntersection;
                    }

                    if (isMatchingValueSet()) {
                        yield new SkipList<>(informationRetrievalSystem.getListOfPostingForToken(matchingValue));
                    } else {
                        // normalization of input matching value leads to null, hence no results can be found
                        yield new SkipList<>();
                    }
                }
            }
        };

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
        var results = evaluateBothSimpleAndAggregatedExpressionRecursively();
        assert results.stream().sorted().distinct().toList().equals(results.stream().toList());
        if (!maxNumberOfResultsSpecified) {
            maxNumberOfResults = results.size();
        }
        return informationRetrievalSystem.getCorpus()
                .getDocuments(results.stream().map(Posting::getDocId).limit(maxNumberOfResults).toList());
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

    /**
     * Class representing a phrase, to be used for answering phrasal queries.
     */
    private static class Phrase {

        /**
         * The phrase represented as array of {@link String}.
         */
        final String[] words;

        /**
         * The exact distance allowed respect to the first word, saved
         * as array: specifies at the k-th position the exact distance
         * (number of words +1) which must be present between the first
         * and the (k+1)-th term specified in {@link #words};
         * e.g., the value at index 0 indicates the exact number of
         * words (+1) allowed between the word at position 0 and the word
         * at position 1, assuming that there are at least two words,
         * and, if that value is 1, then words at index 0 and word at
         * index 1 must be adjacent; assuming instead that three words are
         * present, the value at index 1 indicates the exact number of
         * words (+1) allowed between the word at position 0 and the word
         * at position 2: if that value is 3, then words at index 0 and word at
         * index 2 must be separated by exactly two words (in fact, number of
         * words between first and third word +1 = 2 +1 = 3 = the value at
         * index 2 of this attribute, i.e., the value referring to the
         * third word of the phrase).
         */
        final int[] distanceFromFirstWord;

        /**
         * Constructor.
         *
         * @param words                 See {@link #words}.
         * @param distanceFromFirstWord See {@link #distanceFromFirstWord}.
         * @throws IllegalArgumentException if any of the conditions
         *                                  <ul>
         *                                      <li><code>words.length>=2</code></li>
         *                                      <li><code>words.length==distanceFromFirstWord.length+1</code></li>
         *                                      <li>distanceFromFirstWord[j] &ge; 1 &forall; j &isin; 	&#123;0,1,..,distanceFromFirstWord-1&#125;</li>
         *                                      <li>distanceFromFirstWord[j] - distanceFromFirstWord[j-1] &ge; 1 &forall; j &isin; 	&#123;1,..,distanceFromFirstWord-1&#125;</li>
         *                                  </ul>
         *                                  do not hold. Phrase of a single word are not accepted.
         */
        Phrase(@NotNull String[] words, int[] distanceFromFirstWord) throws IllegalArgumentException {
            if (words.length > 1
                    && words.length == distanceFromFirstWord.length + 1
                    && distanceFromFirstWord[0] > 0
                    && IntStream.range(1, distanceFromFirstWord.length)
                    .allMatch(i -> distanceFromFirstWord[i] - distanceFromFirstWord[i - 1] > 0)) {
                this.words = words;
                this.distanceFromFirstWord = distanceFromFirstWord;
            } else {
                throw new IllegalArgumentException();
            }
        }

        /**
         * @return the number of words composing the phrase represented by this instance.
         */
        synchronized int size() {
            return words.length;
        }
    }
}   // TODO: remove Objects.requireNonNull