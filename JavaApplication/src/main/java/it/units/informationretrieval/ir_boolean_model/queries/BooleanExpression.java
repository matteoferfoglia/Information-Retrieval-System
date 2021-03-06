package it.units.informationretrieval.ir_boolean_model.queries;

import com.bpodgursky.jbool_expressions.*;
import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.*;
import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skiplist.SkipList;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.units.informationretrieval.ir_boolean_model.entities.InvertedIndex.ESCAPED_WILDCARD_FOR_REGEX;
import static it.units.informationretrieval.ir_boolean_model.entities.InvertedIndex.WILDCARD;

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
 * <p/>
 * When a query must be constructed, I (the author of this class) suggest
 * solving AND queries first (intersection of posting lists might reduce the
 * size of operands and allows faster operations) or to use the method
 * {@link #parseExpression(Expression)} which create an instance of this
 * class by parsing an input query string and try to simplify the query.
 * Furthermore, for performance reasons, I suggest avoiding general NOT
 * queries as much as possible; if the user needs to perform a NOT query,
 * I suggest doing it inside an AND query (e.g., avoid "NOT foo", and
 * prefer "bar AND NOT foo": the AND query is exploited by the system
 * for optimization reasons).
 *
 * @author Matteo Ferfoglia
 */
public class BooleanExpression {

    /**
     * The delimiter for a phrase (if matched from an input textual query string.)
     */
    public static final String PHRASE_DELIMITER = "\"";
    /**
     * If setting a phrase query, this character to specify that the following
     * <strong>number</strong> is the number of words that must be present
     * between the two words, e.g. <pre>"The/0cat/1on"</pre> means that between
     * <pre>"The"</pre> and <pre>"cat"</pre> exactly 0 words are present, while
     * between <pre>"cat"</pre> and <pre>"on"</pre> exactly 1 word is present
     * (The entire phrase might be <pre>"The cat is on"</pre>, but we do not
     * know it).
     */
    public static final String NUM_OF_WORDS_FOLLOWS_CHARACTER = "/";

    /**
     * Valid character for the {@link QueryParsing}, which is used
     * for escaping reasons.
     */
    private static final String VALID_CHAR_FOR_PARSER = "_";
    /**
     * {@link QueryParsing} might not accept the wildcard symbol,
     * so all occurrences of the wildcard symbol in any query strings should
     * be replaced with {@link #VALID_CHAR_FOR_PARSER}, repeated for
     * the number of times specified by this field.
     */
    private final static int VALID_PARSING_CHAR_REPETITIONS_FOR_WILDCARD = 3;

    /**
     * {@link QueryParsing} might not accept the {@link #NUM_OF_WORDS_FOLLOWS_CHARACTER},
     * so all of its occurrences in any query strings should
     * be replaced with {@link #VALID_CHAR_FOR_PARSER}, repeated for the number
     * of times specified by this field.
     */
    private final static int VALID_PARSING_CHAR_REPETITIONS_FOR_NUM_OF_WORDS_FOLLOW = 5;

    /**
     * {@link QueryParsing} might not accept the {@link #PHRASE_DELIMITER},
     * so all of its occurrences in any query strings should
     * be replaced with {@link #VALID_CHAR_FOR_PARSER}, repeated for the number
     * of times specified by this field.
     */
    private final static int VALID_PARSING_CHAR_REPETITIONS_FOR_PHRASE_DELIMITER = 7;

    /**
     * {@link QueryParsing} might not accept the literal "true",
     * so all of its occurrences in any query strings should
     * be replaced with {@link #VALID_CHAR_FOR_PARSER}, repeated for the number
     * of times specified by this field.
     */
    private final static int VALID_PARSING_CHAR_REPETITIONS_FOR_TRUE_LITERAL = 9;

    /**
     * {@link QueryParsing} might not accept the literal "false",
     * so all of its occurrences in any query strings should
     * be replaced with {@link #VALID_CHAR_FOR_PARSER}, repeated for the number
     * of times specified by this field.
     */
    private final static int VALID_PARSING_CHAR_REPETITIONS_FOR_FALSE_LITERAL = 11;
    /**
     * Flag that enables some more output, useful for debugging, for method
     * {@link #evaluateNotQuery()}, in particular to try to improve performances.
     */
    private final static boolean DEBUG_NOT_QUERY;
    /**
     * Flag which is set to true if results of queries must be ranked.
     */
    private static boolean RANK_RESULTS;
    /**
     * Flag which is set to true if, for ranking query results, the
     * wf-idf value must be used, otherwise (if it is false) the tf-idf
     * value will be used.
     */
    private static boolean USE_WF_IDF;

    static {
        try {
            RANK_RESULTS = Boolean.parseBoolean(AppProperties.getInstance().get("app.rank_query_results"));
            USE_WF_IDF = Boolean.parseBoolean(AppProperties.getInstance().get("app.rank_query_results"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static {
        boolean DEBUG_NOT_QUERYdebugNotQueryTmp = false;    // tmp variable to defer the assignment of final constant
        try {
            DEBUG_NOT_QUERYdebugNotQueryTmp = Boolean.parseBoolean(
                    AppProperties.getInstance().get("app.debug.NOTQueries"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            DEBUG_NOT_QUERY = DEBUG_NOT_QUERYdebugNotQueryTmp;
        }
    }

    /**
     * The {@link InformationRetrievalSystem} to use for evaluating this instance.
     */
    @NotNull
    private final InformationRetrievalSystem informationRetrievalSystem;
    /**
     * Flag: the value is true if this instance was created as consequence of
     * a spelling correction.
     */
    private final boolean createdForSpellingCorrection;   // default value is false
    /**
     * The comparator to sort corrected query words when applying spelling
     * correction if they have the same edit-distance wrt. the initial query
     * string.
     */
    @NotNull
    private final Comparator<String> spellingCorrectedQueryWordsComparator;
    /**
     * When parsing an input query string, this field is used to handle
     * the content between quotes (it must not be modified).
     * For this map:
     * <ol>
     *     <li>key: the replacement</li>
     *     <li>value: the initial value before replacing</li>
     * </ol>
     */
    @NotNull
    private final Map<String, String> mapReplacedContentInQueryParsing = new HashMap<>(10);
    /**
     * {@link Map} that saves the applied automatic correction (when
     * {@link #automaticCorrectionEnabled} is enabled, see its description).
     * For each entry of the map, the key is the token as inserted by
     * the user and the corresponding value is the {@link List} of
     * corrections proposed by the system and searched (note: the same
     * wrongly token inserted by the user can lead to a number of "nearest"
     * corrections, this the reason because a {@link List} is used).
     */
    @NotNull
    private final ConcurrentHashMap<String, List<String>> automaticCorrections = new ConcurrentHashMap<>();
    /**
     * This field save the results retrieved at the previous invocation of
     * {@link #evaluate()} (if it was invoked), for caching reasons.
     */
    @NotNull
    private SkipList<Posting> results = new SkipList<>();
    /**
     * The {@link SpellingCorrector} used for this instance. It is important
     * to save this field to handle multiple invocation of {@link #spellingCorrection(boolean, boolean)}.
     */
    @Nullable
    private SpellingCorrector spellingCorrector = null;
    /**
     * The {@link BINARY_OPERATOR} to apply to this instance.
     * It must be null if this is a non-aggregated instance.
     */
    @Nullable   // if this expression is not aggregated
    private BINARY_OPERATOR binaryOperator;
    /**
     * Flag which is true if this instance is an aggregated expression.
     * See the description of {@link BooleanExpression this class}.
     */
    private boolean isAggregated;
    /**
     * The left-child (first) expression to evaluate (for aggregated expressions).
     */
    @Nullable   // if this expression is not aggregated
    private BooleanExpression leftChildOperand;
    /**
     * The right-child (second) expression to evaluate (for aggregated expressions).
     */
    @Nullable   // if this expression is not aggregated
    private BooleanExpression rightChildOperand;
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
    private UNARY_OPERATOR unaryOperator = UNARY_OPERATOR.IDENTITY; // default is the identity operator
    /**
     * The query represented as {@link String} for this instance.
     */
    @NotNull
    private String queryString = "";
    /**
     * Flag: true if this instance has already been evaluated
     * (i.e., method {@link #evaluateBothSimpleAndAggregatedExpressionRecursively()}
     * already invoked).
     */
    private boolean alreadyEvaluated = false;
    /**
     * Flag: if true, in the case that query evaluation does not produce
     * any results, try to correct the input query word-by-word,
     * automatically.
     */
    private boolean automaticCorrectionEnabled = false;

    /**
     * Constructor. Creates a non-aggregated expression.
     *
     * @param informationRetrievalSystem The {@link InformationRetrievalSystem} on which the query must be performed.
     */
    public BooleanExpression(@NotNull final InformationRetrievalSystem informationRetrievalSystem) {
        this.isAggregated = false;
        this.createdForSpellingCorrection = false;
        this.leftChildOperand = null;
        this.rightChildOperand = null;
        this.binaryOperator = null;
        this.informationRetrievalSystem = Objects.requireNonNull(informationRetrievalSystem);
        this.spellingCorrectedQueryWordsComparator =
                SpellingCorrector.spellingCorrectedQueryWordsComparatorFactory(informationRetrievalSystem);
    }

    /**
     * Copy Constructor.
     *
     * @param booleanExpression The instance to be copied.
     */
    public BooleanExpression(@NotNull BooleanExpression booleanExpression) throws IllegalArgumentException {
        this(booleanExpression, false);
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
            this.createdForSpellingCorrection = false;
            this.leftChildOperand = Objects.requireNonNull(expr1);
            this.rightChildOperand = Objects.requireNonNull(expr2);
            this.binaryOperator = Objects.requireNonNull(operator);
            this.spellingCorrectedQueryWordsComparator =
                    SpellingCorrector.spellingCorrectedQueryWordsComparatorFactory(informationRetrievalSystem);
        } else {
            throw new IllegalStateException(
                    "Impossible to create an aggregate expression with children" +
                            " expressions referring to different IR Systems.");
        }
    }

    /**
     * Copy constructor.
     */
    private BooleanExpression(BooleanExpression booleanExpression, boolean createdForSpellingCorrection) {
        this.createdForSpellingCorrection = createdForSpellingCorrection;
        this.isAggregated = booleanExpression.isAggregated;
        this.matchingValue = booleanExpression.matchingValue;
        this.matchingPhrase = booleanExpression.matchingPhrase;
        this.unaryOperator = booleanExpression.unaryOperator;
        this.binaryOperator = booleanExpression.binaryOperator;
        this.informationRetrievalSystem = booleanExpression.informationRetrievalSystem;
        this.leftChildOperand = booleanExpression.leftChildOperand;
        this.rightChildOperand = booleanExpression.rightChildOperand;
        this.automaticCorrectionEnabled = booleanExpression.automaticCorrectionEnabled;
        this.queryString = booleanExpression.queryString;
        this.spellingCorrector = booleanExpression.spellingCorrector;
        this.spellingCorrectedQueryWordsComparator =
                SpellingCorrector.spellingCorrectedQueryWordsComparatorFactory(informationRetrievalSystem);
    }

    /**
     * Parser might not accept wildcards, so this method escapes the wildcard
     * symbol in an invertible way (by invoking {@link #escapeSpecialCharactersReverse(String)}).
     * <strong>Notice</strong>: duplicates of {@link #VALID_CHAR_FOR_PARSER} (if present in the
     * initial query string) will be lost, also after invoking {@link #escapeSpecialCharactersReverse(String)}.
     *
     * @param queryString The query string to be escaped.
     * @return the escaped query string.
     */
    @NotNull
    private static String escapeSpecialCharacters(@NotNull String queryString) {
        // escaping is done by replicating a valid char for the parser

        return queryString
                .replaceAll(VALID_CHAR_FOR_PARSER + "+", VALID_CHAR_FOR_PARSER) // remove duplicates
                .replaceAll(    // escape wildcard
                        ESCAPED_WILDCARD_FOR_REGEX,
                        Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_WILDCARD, VALID_CHAR_FOR_PARSER))

                // escape other used special characters
                .replaceAll(
                        NUM_OF_WORDS_FOLLOWS_CHARACTER,
                        Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_NUM_OF_WORDS_FOLLOW, VALID_CHAR_FOR_PARSER))

                // words with logical meaning are evaluated like literal expressions by the parser, hence they need kind of escaping
                .replaceAll(
                        "true",
                        Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_TRUE_LITERAL, VALID_CHAR_FOR_PARSER))
                .replaceAll(
                        "false",
                        Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_FALSE_LITERAL, VALID_CHAR_FOR_PARSER));

    }

    /**
     * This method is the inverse function of {@link #escapeSpecialCharacters(String)}.
     * No duplicates for the symbol {@link #VALID_CHAR_FOR_PARSER} will be present.
     *
     * @param queryString The escaped query string.
     * @return the unescaped query string.
     */
    @NotNull
    private static String escapeSpecialCharactersReverse(@NotNull String queryString) {

        //noinspection ConstantConditions // order in which replacement operations are made matters and this assertion checks if the value is changed for some reasons
        assert VALID_PARSING_CHAR_REPETITIONS_FOR_NUM_OF_WORDS_FOLLOW > VALID_PARSING_CHAR_REPETITIONS_FOR_WILDCARD;
        return queryString
                // replacements must be done in order: longest escaping sequence first
                .replaceAll(Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_FALSE_LITERAL, VALID_CHAR_FOR_PARSER), "false")
                .replaceAll(Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_TRUE_LITERAL, VALID_CHAR_FOR_PARSER), "true")
                .replaceAll(Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_NUM_OF_WORDS_FOLLOW, VALID_CHAR_FOR_PARSER), NUM_OF_WORDS_FOLLOWS_CHARACTER)
                .replaceAll(Utility.getNReplicationsOfString(VALID_PARSING_CHAR_REPETITIONS_FOR_WILDCARD, VALID_CHAR_FOR_PARSER), WILDCARD);
    }

    /**
     * @return true if this instance is set to automatically try to
     * correct the input query inserted by the user, false otherwise.
     */
    public boolean isAutomaticCorrectionEnabled() {
        return automaticCorrectionEnabled;
    }

    /**
     * Set this instance to automatically try to correct the input query
     * inserted by the user, according to the input parameter.
     *
     * @param automaticCorrectionEnabled true if this instance must be set to automatically try to
     *                                   correct the input query inserted by the user, false otherwise.
     * @return this instance, after setting the new value for the property.
     */
    public BooleanExpression setAutomaticCorrectionEnabled(boolean automaticCorrectionEnabled) {
        this.automaticCorrectionEnabled = automaticCorrectionEnabled;
        if (this.isAggregated) {
            // propagate the decision to children
            assert this.leftChildOperand != null;
            assert this.rightChildOperand != null;
            this.leftChildOperand.setAutomaticCorrectionEnabled(automaticCorrectionEnabled);
            this.rightChildOperand.setAutomaticCorrectionEnabled(automaticCorrectionEnabled);
        }
        return this;
    }

    /**
     * Parses the input {@link String} representing a query and
     * returns the corresponding {@link BooleanExpression}.
     * The input query must respect the following rules:
     * <ul>
     *     <li>the character &amp; is the AND operator</li>
     *     <li>the character | is the OR operator</li>
     *     <li>parenthesis ( ) can be used for prioritization</li>
     * </ul>
     * <p/>
     * Examples:
     * <ul>
     *     <li>america | amusement</li>
     *     <li>foo</li>
     *     <li>!bar</li>
     *     <li>foo & (!bar | car)</li>
     *     <li>foo & !bar</li>
     * </ul>
     *
     * @param queryString The query string.
     * @return the {@link BooleanExpression} for the input query string.
     * @throws IllegalArgumentException if the input does not represent a valid query
     * @throws IllegalStateException    if matching value or phrase were already set for this instance.
     */
    public BooleanExpression parseQuery(@NotNull String queryString)
            throws IllegalArgumentException, IllegalStateException {

        Objects.requireNonNull(queryString);
        throwIfAggregatedOrMatchingValueAlreadySetOrMatchingPhraseAlreadySet();

        queryString = escapeSpecialCharacters(queryString);

        // Handle content between quotes in query string (must not be modified)
        int counter = 0;
        final Pattern BETWEEN_QUOTES_REGEX = Pattern.compile("[\"][^\"]*[\"]");
        Matcher betweenQuotesMatcher = BETWEEN_QUOTES_REGEX.matcher(queryString);
        while (betweenQuotesMatcher.find()) {
            String match = betweenQuotesMatcher.group();
            final String replacementDelimiter = Utility.getNReplicationsOfString(
                    VALID_PARSING_CHAR_REPETITIONS_FOR_PHRASE_DELIMITER, VALID_CHAR_FOR_PARSER);
            String replacement = replacementDelimiter + (counter++) + replacementDelimiter;
            mapReplacedContentInQueryParsing.put(replacement, match);
            queryString = queryString.replaceAll(match, replacement);
        }

        return set(parseExpression(QueryParsing.parse(queryString)));
    }

    /**
     * Sets all non-static and non-final fields of the given instance to this one.
     * This method makes use of reflections.
     *
     * @param parsedBooleanExpression The input instance from which fields must be copied.
     * @return this instance after the method execution.
     */
    private BooleanExpression set(BooleanExpression parsedBooleanExpression) {
        Arrays.stream(parsedBooleanExpression.getClass().getDeclaredFields())
                .peek(field -> field.setAccessible(true))
                .filter(field -> !Modifier.isStatic(field.getModifiers())
                        && !Modifier.isFinal(field.getModifiers()))
                .forEach(field -> {
                    try {
                        field.set(this, field.get(parsedBooleanExpression));
                    } catch (IllegalAccessException e) {
                        Logger.getLogger(getClass().getCanonicalName())
                                .log(Level.SEVERE, "Impossible to copy field value of field " + field, e);
                    }
                });
        return this;
    }

    /**
     * This method translates an instance of {@link Expression} to an instance of
     * this class.
     *
     * @param expression The {@link Expression} to be translated.
     * @return The resulting instance of this class obtained from the input parameter.
     */
    @NotNull
    private BooleanExpression parseExpression(@Nullable final Expression<String> expression) {
        if (expression == null) {   // invalid query string
            return this;
        } else {

            if (expression instanceof Variable variable) {

                String queryString = (String) variable.getValue();
                {
                    // Handle the inverse operations for the escaping done for the parser
                    for (Map.Entry<String, String> entry : mapReplacedContentInQueryParsing.entrySet()) {
                        String replacement = entry.getKey();
                        String initialValue = entry.getValue();
                        queryString = queryString.replaceAll(replacement, initialValue);
                    }
                    queryString = escapeSpecialCharactersReverse(queryString);
                }

                final String REGEX_MATCHING_DELIMITED_PHRASE =
                        "[" + PHRASE_DELIMITER + "][^" + PHRASE_DELIMITER + "]*[" + PHRASE_DELIMITER + "]";
                Matcher phraseMatcher = Pattern.compile(REGEX_MATCHING_DELIMITED_PHRASE).matcher(queryString);

                // new instance needed
                BooleanExpression be = new BooleanExpression(informationRetrievalSystem);

                if (phraseMatcher.matches()) {
                    // phrase is present
                    String phrase = phraseMatcher.group()
                            .replaceAll(PHRASE_DELIMITER, "").strip();
                    String[] words;
                    int[] distancesOfIthWordFromTheFirstWord = null;
                    final String REGEX_SEARCHING_INTERMEDIATE_NUM_OF_WORDS =
                            "\\" + NUM_OF_WORDS_FOLLOWS_CHARACTER + "\\d+";
                    if (Pattern.compile(REGEX_SEARCHING_INTERMEDIATE_NUM_OF_WORDS).matcher(phrase).find()) {
                        phrase = phrase.replaceAll("\\s+(?!\\d+)", NUM_OF_WORDS_FOLLOWS_CHARACTER + "0");  // replace spaces with number of (0) intermediate words if not specified
                        Matcher intermediateNumOfWordsMatcher =
                                Pattern.compile(REGEX_SEARCHING_INTERMEDIATE_NUM_OF_WORDS).matcher(phrase);
                        words = phrase.split(REGEX_SEARCHING_INTERMEDIATE_NUM_OF_WORDS);
                        int[] intermediateNumOfWords = new int[words.length - 1];
                        for (int i = 0; i < intermediateNumOfWords.length && intermediateNumOfWordsMatcher.find(); i++) {
                            intermediateNumOfWords[i] = Integer.parseInt(
                                    intermediateNumOfWordsMatcher.group()
                                            .replaceAll("\\" + NUM_OF_WORDS_FOLLOWS_CHARACTER, ""));
                        }
                        distancesOfIthWordFromTheFirstWord = new int[intermediateNumOfWords.length];
                        int cumulativeDistance = 0;
                        for (int i = 0; i < distancesOfIthWordFromTheFirstWord.length; i++) {
                            cumulativeDistance += intermediateNumOfWords[i] + 1; // each word is at least one (+1) word ahead than the previous one
                            distancesOfIthWordFromTheFirstWord[i] = cumulativeDistance;
                        }
                    } else {
                        // phrase does not specify the number of words which must be present between words composing the phrase itself
                        words = Utility.split(phrase);
                    }
                    if (distancesOfIthWordFromTheFirstWord == null) {
                        be.setMatchingPhrase(words);
                    } else {
                        be.setMatchingPhrase(words, distancesOfIthWordFromTheFirstWord);
                    }
                } else {
                    be.setMatchingValue(queryString);
                }

                return be;
            } else if (expression instanceof Not negation) {

                //noinspection unchecked
                return parseExpression(negation.getE()).setUnaryOperator(UNARY_OPERATOR.NOT);
            } else if (expression instanceof Literal literal) {
                if (literal.getValue()) {
                    System.err.println("Input query evaluated to literal true, this means that" +
                            " all indexed documents should be retrieved during the evaluation of" +
                            " this instance, but no results will be returned. Please, reformulate" +
                            " your query.");
                }

                // evaluation will return nothing (either if query expression evaluated true or false)
                return informationRetrievalSystem.createNewBooleanExpression();
            } else {
                // binary expression

                BinaryOperator<BooleanExpression> accumulator;
                if (expression instanceof And) {
                    accumulator = BooleanExpression::and;
                } else if (expression instanceof Or) {
                    accumulator = BooleanExpression::or;
                } else {
                    throw new IllegalStateException("Unknown binary operation in expression " + expression);
                }
                return expression.getChildren()
                        .stream()
                        .map(this::parseExpression)
                        .reduce(accumulator)
                        .orElseThrow();
            }
        }
    }

    /**
     * This method tries to perform a spelling correction on the query inserted
     * by the user and returns the instance, ready for a new evaluation.
     * <strong>NOTICE:</strong> the same instance of this class does
     * <strong>NOT</strong> support different corrector together. This
     * method <strong>can</strong> be invoked multiple time to have a
     * correction which is further (wrt. the edit-distance from the initial
     * query string) at each invocation, but you cannot invoke this method
     * once for spelling correction and then for phonetic correction,
     * because it will simply <strong>not</strong> working as expected
     * (only the first corrector, i.e., the spelling one for this example,
     * will be invoked). <strong>If you need to use different type of
     * spelling correction</strong>, you can create different instances
     * of this class (same matching value/phrase) and apply the different
     * correction on them and then use the OR operator among all the created
     * instance (<strong>NOTICE:</strong> copies of the instance must be
     * done before invoking this method the first time, otherwise it simply
     * will not work, because the corrector would have already set, as
     * just explained above).
     *
     * @param phoneticCorrection true if phonetic correction is desired, false if
     *                           "classic" spelling correction is preferred.
     * @param useEditDistance    is a flag which is <strong>ignored if</strong> a
     *                           <strong>non</strong> phonetic correction is being
     *                           performed, and, if it is set to true, the edit distance
     *                           is used (like in normal spelling correction), while if
     *                           it is set to false, all possible phonetic corrections
     *                           will be considered, independently of how far the resulting
     *                           correction is from the initial query.
     *                           <strong>NOTICE</strong>: it can be <strong>dangerous</strong>
     *                           to set this parameter to <code>false</code>, due to the high
     *                           number of combinations in which the correction can take place
     *                           (very high computational cost) and it might lead to
     *                           {@link StackOverflowError}.
     * @return a new instance of this class with the spelling correction, ready
     * to invoke {@link #evaluate()} for a new evaluation of the instance on the
     * spelling-corrected query.
     */
    @NotNull
    public BooleanExpression spellingCorrection(boolean phoneticCorrection, boolean useEditDistance) {

        if (spellingCorrector != null && !spellingCorrector.isPossibleToCorrect()) {
            return this;
        } else {

            try {

                // If this is an aggregated query, then spelling correction must be performed on its children too
                if (isAggregated) {
                    assert leftChildOperand != null;
                    assert binaryOperator != null;
                    assert rightChildOperand != null;

                    // don't apply spelling correction on an instance that was created by this method
                    leftChildOperand = leftChildOperand.createdForSpellingCorrection
                            ? leftChildOperand : leftChildOperand.spellingCorrection(phoneticCorrection, useEditDistance);
                    rightChildOperand = rightChildOperand.createdForSpellingCorrection
                            ? rightChildOperand : rightChildOperand.spellingCorrection(phoneticCorrection, useEditDistance);
                } else {

                    if (!isSpellingCorrectionApplied()) {   // first initialization for this instance

                        if (isMatchingValueSet()) {
                            spellingCorrector = new SpellingCorrector(
                                    new it.units.informationretrieval.ir_boolean_model.utils.custom_types.Phrase(matchingValue),
                                    phoneticCorrection, useEditDistance,
                                    informationRetrievalSystem, spellingCorrectedQueryWordsComparator);
                        } else if (isMatchingPhraseSet()) {
                            spellingCorrector = new SpellingCorrector(
                                    new it.units.informationretrieval.ir_boolean_model.utils.custom_types.Phrase(matchingPhrase.words),
                                    phoneticCorrection, useEditDistance,
                                    informationRetrievalSystem, spellingCorrectedQueryWordsComparator);
                        } else {
                            throw new IllegalStateException("Unexpected that neither the value nor the phrase were set.");
                        }
                    }

                    assert spellingCorrector != null;
                    var corrections = spellingCorrector.getNewCorrections();
                    if (corrections.size() > 0) {
                        BooleanExpression booleanExpressionWithCorrection;
                        if (isMatchingValueSet()) {
                            booleanExpressionWithCorrection =
                                    corrections.stream()
                                            .filter(phrase -> phrase.size() > 0)
                                            .map(phrase -> phrase.getWordAt(0)/*single word query, so take the first word in the phrase*/)
                                            .map(correctedWord -> new BooleanExpression(this, true)
                                                    .setMatchingValueWithoutCheckingIfAggregatedQueryNeitherIfAlreadySet(correctedWord))
                                            .reduce(BooleanExpression::or)
                                            .orElse(this);
                        } else if (isMatchingPhraseSet()) {
                            booleanExpressionWithCorrection =
                                    corrections.stream()
                                            .map(it.units.informationretrieval.ir_boolean_model.utils.custom_types.Phrase::getArrayOfWords)
                                            .map(correctedPhrase -> new BooleanExpression(this, true)
                                                    .setMatchingPhraseWithoutCheckingIfAggregatedQueryNeitherIfAlreadySet(
                                                            correctedPhrase, matchingPhrase.distanceFromFirstWord))
                                            .reduce(BooleanExpression::or)
                                            .orElse(this);
                        } else {
                            throw new IllegalStateException("Unexpected that neither the value nor the phrase were not set.");
                        }

                        if (booleanExpressionWithCorrection == this/*same reference if no corrections were made*/) {
                            return this;
                        } else {
                            // this becomes an aggregated expression
                            leftChildOperand = new BooleanExpression(this, false); // this is the actual expression inserted by the user to be spelling-corrected
                            isAggregated = true;
                            matchingValue = null;
                            matchingPhrase = null;
                            rightChildOperand = booleanExpressionWithCorrection;
                            binaryOperator = BINARY_OPERATOR.OR;
                        }
                    }
                }
                return this;    // nothing else to do
            } catch (StackOverflowError e) {
                if (spellingCorrector != null) {
                    spellingCorrector.stop();
                }
                return this;
            }

        }
    }

    /**
     * Sets the {@link #matchingValue}.
     *
     * @param matchingValue The value to match.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingValue(@NotNull String matchingValue) {
        throwIfAggregatedOrMatchingValueAlreadySetOrMatchingPhraseAlreadySet();
        return setMatchingValueWithoutCheckingIfAggregatedQueryNeitherIfAlreadySet(matchingValue);
    }

    /**
     * Like {@link #setMatchingValue(String)}, but this method does not check
     * either if the query is aggregated nor if matching value/phrase is already set.
     */
    @NotNull
    private BooleanExpression setMatchingValueWithoutCheckingIfAggregatedQueryNeitherIfAlreadySet(
            @NotNull String matchingValue) {
        // Save the query string inserted by the user, before any normalization is applied
        // This query string will NOT be used for the evaluation but only for toString methods
        this.queryString = matchingValue;

        this.matchingValue = Utility.preprocess(matchingValue, true, informationRetrievalSystem.getLanguage());
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
     * @param matchingPhrase          The phrase to match.
     * @param matchingPhraseDistances Array with the exact distance allowed between words, saved as array:
     *                                specifies at the k-th position the exact distance (number of terms)
     *                                which must be present between the term at index k in the phrase and
     *                                the term at position 0.
     * @return This instance after the execution of this method.
     */
    public BooleanExpression setMatchingPhrase(@NotNull String[] matchingPhrase, int[] matchingPhraseDistances) {
        throwIfAggregatedOrMatchingValueAlreadySetOrMatchingPhraseAlreadySet();
        return setMatchingPhraseWithoutCheckingIfAggregatedQueryNeitherIfAlreadySet(
                matchingPhrase, matchingPhraseDistances);

    }

    /**
     * @throws IllegalStateException if at least one of the following conditions holds:
     *                               <ul>
     *                                   <li>this instance is an aggregated expression</li>
     *                                   <li>the {@link #matchingValue} for this instance was already set</li>
     *                                   <li>the {@link #matchingPhrase} for this instance was already set</li>
     *                               </ul>
     */
    private void throwIfAggregatedOrMatchingValueAlreadySetOrMatchingPhraseAlreadySet() throws IllegalStateException {
        throwIfIsAggregated();
        StringBuilder errorMsg = new StringBuilder();
        if (isMatchingValueSet()) {
            errorMsg.append("Matching value");
        } else if (isMatchingPhraseSet()) {
            errorMsg.append("Matching phrase");
        }
        if (!errorMsg.isEmpty()) {
            throw new IllegalStateException(errorMsg.append(" already set, cannot re-set.").toString());
        }
    }

    /**
     * Like {@link #setMatchingPhrase(String[], int[])}, but this method does not check
     * either if the query is aggregated nor if matching value/phrase is already set.
     *
     * @throws IllegalArgumentException if the length of the array of distances is not equal to
     *                                  the length of the array composing the phrase minus 1.
     */
    @NotNull
    private BooleanExpression setMatchingPhraseWithoutCheckingIfAggregatedQueryNeitherIfAlreadySet(
            @NotNull String[] matchingPhrase, int[] matchingPhraseDistances)
            throws IllegalArgumentException {

        if (matchingPhrase.length < 1 || matchingPhraseDistances.length != matchingPhrase.length - 1) {
            throw new IllegalArgumentException(
                    "The size of the array of distances must be equal to the phrase length minus 1, but it is not:"
                            + System.lineSeparator()
                            + "\t- phrase length: " + matchingPhrase.length
                            + System.lineSeparator()
                            + "\t- distances length: " + matchingPhraseDistances.length);
        }

        // Save the query string inserted by the user, before any normalization is applied
        // This query string will NOT be used for the evaluation but only for toString methods
        try {
            queryString = new Phrase(matchingPhrase, matchingPhraseDistances).toString();
        } catch (IllegalArgumentException e) {
            queryString = matchingPhrase[0];
        }

        String[] tmpPhrase = Arrays.stream(matchingPhrase)
                .map(word -> Utility.preprocess(word, true, informationRetrievalSystem.getLanguage()))
                .toArray(String[]::new);    // null elements might be present

        String[] phrase = new String[tmpPhrase.length];
        int[] distances = new int[tmpPhrase.length - 1];

        int wordCounter = 0;
        int nonNullWordsCounter = 0;

        //noinspection StatementWithEmptyBody
        for (; wordCounter < tmpPhrase.length && tmpPhrase[wordCounter] == null; wordCounter++) {
            // find the first non-null word and increment the counter
        }
        if (wordCounter == tmpPhrase.length) { // true if all words are null
            return this;    // do not set anything
        } else {
            if (wordCounter > 0) {
                // Update distances due to removed words
                for (int i = 0; i < wordCounter; i++) {
                    matchingPhraseDistances[i]--;
                }
            }
            for (; wordCounter < matchingPhraseDistances.length; wordCounter++) {
                if (tmpPhrase[wordCounter] != null) {
                    phrase[nonNullWordsCounter] = tmpPhrase[wordCounter];
                    distances[nonNullWordsCounter++] = matchingPhraseDistances[wordCounter];
                } else {
                    // Update distances due to removed words
                    for (int i = wordCounter + 1; i < matchingPhraseDistances.length; i++) {
                        matchingPhraseDistances[i]--;  // remove the space occupied by the (one) just removed word
                    }
                }
            }
            if (tmpPhrase[wordCounter] != null) {    // copy the last word if non-null
                phrase[nonNullWordsCounter++] = tmpPhrase[wordCounter];
            }
            assert Arrays.stream(distances).filter(val -> val >= 0).count() == distances.length;
            assert distances.length < 2 || Arrays.stream(distances).sequential().reduce((a, b) -> a < b ? 1 : 0).orElse(1/*if here, array is empty*/) == 1;// check distances are monotonically growing
            assert wordCounter + 1 == tmpPhrase.length;
            assert nonNullWordsCounter >= 1;

            String[] finalPhrase = new String[nonNullWordsCounter];
            int[] finalDistances = new int[nonNullWordsCounter - 1];
            assert finalPhrase.length == Arrays.stream(tmpPhrase).filter(Objects::nonNull).count();
            assert finalDistances.length == finalPhrase.length - 1;

            System.arraycopy(phrase, 0, finalPhrase, 0, nonNullWordsCounter);
            System.arraycopy(distances, 0, finalDistances, 0, nonNullWordsCounter - 1);
            assert finalDistances.length == finalPhrase.length - 1;
            assert Arrays.stream(finalPhrase).filter(Objects::nonNull).count() == finalPhrase.length;
            assert Arrays.stream(finalDistances).filter(val -> val >= 0).count() == finalDistances.length;
            assert finalDistances.length < 2 || Arrays.stream(finalDistances).sequential().reduce((a, b) -> a < b ? 1 : 0).orElse(1/*if here, array is empty*/) == 1;// check distances are monotonically growing

            if (finalPhrase.length == 1) {
                // normalization may lead to a single word (hence, it is not a phrase anymore)
                return setMatchingValueWithoutCheckingIfAggregatedQueryNeitherIfAlreadySet(finalPhrase[0]);
            } else {
                assert Arrays.stream(finalPhrase).noneMatch(Objects::isNull);
                this.matchingPhrase = new Phrase(finalPhrase, finalDistances);
                return this;
            }
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
    public BooleanExpression setMatchingPhrase(@NotNull String[] matchingPhrase) {
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
     * @return true if either the {@link #matchingValue} or the {@link #matchingPhrase} is set, or
     * if it is aggregated, false otherwise.
     */
    private boolean isSet() {
        return isAggregated || isMatchingValueSet() || isMatchingPhraseSet();
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
        return set(isSet()
                ? new BooleanExpression(BINARY_OPERATOR.AND, new BooleanExpression(this), other)
                : other);
    }

    /**
     * Like {@link #and(BooleanExpression)}, but accepts a word directly.
     */
    public BooleanExpression and(@NotNull String matchingValue) {
        return and(new BooleanExpression(informationRetrievalSystem).setMatchingValue(matchingValue));
    }

    /**
     * Like {@link #and(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression and(@NotNull String[] matchingPhrase) {
        return and(new BooleanExpression(informationRetrievalSystem).setMatchingPhrase(matchingPhrase));
    }

    /**
     * Like {@link #and(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression and(@NotNull String[] matchingPhrase, int[] matchingPhraseMaxDistance) {
        return and(new BooleanExpression(informationRetrievalSystem)
                .setMatchingPhrase(matchingPhrase, matchingPhraseMaxDistance));
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
        return set(isSet()
                ? new BooleanExpression(BINARY_OPERATOR.OR, new BooleanExpression(this), other)
                : other);
    }

    /**
     * Like {@link #or(BooleanExpression)}, but accepts a word directly.
     */
    public BooleanExpression or(@NotNull String matchingValue) {
        return or(new BooleanExpression(informationRetrievalSystem).setMatchingValue(matchingValue));
    }

    /**
     * Like {@link #or(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression or(String[] matchingPhrase) {
        return or(new BooleanExpression(informationRetrievalSystem).setMatchingPhrase(matchingPhrase));
    }

    /**
     * Like {@link #or(BooleanExpression)}, but accepts a phrase directly.
     */
    public BooleanExpression or(String[] matchingPhrase, int[] matchingPhraseMaxDistance) {
        return or(new BooleanExpression(informationRetrievalSystem)
                .setMatchingPhrase(matchingPhrase, matchingPhraseMaxDistance));
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
    private SkipList<Posting> evaluateBothSimpleAndAggregatedExpressionRecursively()
            throws UnsupportedOperationException {

        alreadyEvaluated = true;
        results = switch (unaryOperator) {
            case NOT -> evaluateNotQuery();
            case IDENTITY -> {
                if (isAggregated) {

                    assert leftChildOperand != null;
                    assert rightChildOperand != null;
                    assert binaryOperator != null;
                    yield switch (binaryOperator) {
                        case AND -> {

                            // Optimization for NOT queries:
                            //     NOT(A) AND B = B \ A         (more efficient to compute the difference set, instead of the NOT(A) operation, which may involve all items from the "universe" set
                            //     A AND NOT(B) = A \ B

                            if (leftChildOperand.unaryOperator.equals(UNARY_OPERATOR.NOT)) {         // NOT(A) AND B = B \ A
                                SkipList<Posting> fromRightChild =
                                        rightChildOperand.evaluateBothSimpleAndAggregatedExpressionRecursively();
                                SkipList<Posting> fromLeftChildNotNegated =
                                        new BooleanExpression(leftChildOperand)
                                                .setUnaryOperator(UNARY_OPERATOR.IDENTITY)
                                                .evaluateBothSimpleAndAggregatedExpressionRecursively();
                                yield SkipList.difference(fromRightChild, fromLeftChildNotNegated, Comparator.naturalOrder());
                            } else if (rightChildOperand.unaryOperator.equals(UNARY_OPERATOR.NOT)) { // A AND NOT(B) = A \ B
                                SkipList<Posting> fromLeftChild =
                                        leftChildOperand.evaluateBothSimpleAndAggregatedExpressionRecursively();
                                SkipList<Posting> fromRightChildNotNegated =
                                        new BooleanExpression(rightChildOperand)
                                                .setUnaryOperator(UNARY_OPERATOR.IDENTITY)
                                                .evaluateBothSimpleAndAggregatedExpressionRecursively();
                                yield SkipList.difference(fromLeftChild, fromRightChildNotNegated, Comparator.naturalOrder());
                            } else {
                                SkipList<Posting> fromLeftChild = leftChildOperand.evaluateBothSimpleAndAggregatedExpressionRecursively();
                                SkipList<Posting> fromRightChild = rightChildOperand.evaluateBothSimpleAndAggregatedExpressionRecursively();
                                yield Utility.intersection(fromLeftChild, fromRightChild);
                            }
                        }
                        case OR -> {
                            SkipList<Posting> fromLeftChild = leftChildOperand.evaluateBothSimpleAndAggregatedExpressionRecursively();
                            SkipList<Posting> fromRightChild = rightChildOperand.evaluateBothSimpleAndAggregatedExpressionRecursively();
                            yield Utility.union(fromLeftChild, fromRightChild);
                        }
                        //noinspection UnnecessaryDefault
                        default -> throw new UnsupportedOperationException("Unknown operator");
                    };

                } else {

                    SkipList<Posting> tmpResults;

                    if (isMatchingPhraseSet()) {
                        //noinspection unchecked    // generic array creation
                        BiPredicate<Posting, Posting>[] biPredicatesForCheckingPositionsForPhrasalQueries =
                                IntStream.range(0, matchingPhrase.size() - 1)
                                        .mapToObj(i -> (BiPredicate<Posting, Posting>)
                                                (Posting posting1, Posting posting2) -> {
                                                    // Note: order of input arg is important

                                                    var positions1 = posting1.getTermPositionsInTheDocument();
                                                    var positions2 = posting2.getTermPositionsInTheDocument();

                                                    // assert positions were sorted
                                                    assert Arrays.stream(positions1).sorted().boxed().toList().equals(Arrays.stream(positions1).boxed().toList());
                                                    assert Arrays.stream(positions2).sorted().boxed().toList().equals(Arrays.stream(positions2).boxed().toList());

                                                    int index1 = 0, index2 = 0;
                                                    while (index1 < positions1.length && index2 < positions2.length) {
                                                        int numberOfTermsBetweenWords =
                                                                positions2[index2] - positions1[index1]              // number of words between the *first* word of phrase and the word of posting2  (Note: when inserting a posting to the intersection list, only the posting referencing the first word of phrase is kept, hence distances must be computed respect the position of the first word)
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
                            assert word != null;
                            var correspondingPostingList = cachedPostings.get(word);
                            if (correspondingPostingList == null) {
                                // posting list for the term was not cached
                                correspondingPostingList = new SkipList<>(
                                        informationRetrievalSystem.getListOfPostingForToken(
                                                word.replaceAll(VALID_CHAR_FOR_PARSER, WILDCARD)));
                                cachedPostings.put(word, correspondingPostingList);
                            }
                            return correspondingPostingList;
                        };

                        SkipList<Posting> phraseQueryIntersection = getPostingListOfIthWordInPhrase.apply(0);
                        for (int i = 1; !phraseQueryIntersection.isEmpty() && i < matchingPhrase.size(); i++) {
                            SkipList<Posting> postings = getPostingListOfIthWordInPhrase.apply(i);
                            phraseQueryIntersection = SkipList.intersection(
                                    phraseQueryIntersection, postings,
                                    biPredicatesForCheckingPositionsForPhrasalQueries[i - 1]);
                        }

                        tmpResults = phraseQueryIntersection;
                    } else if (isMatchingValueSet()) {
                        tmpResults = new SkipList<>(
                                informationRetrievalSystem.getListOfPostingForToken(matchingValue));
                    } else {
                        // normalization of input matching value leads to null, hence no results can be found
                        tmpResults = new SkipList<>();
                    }

                    if (tmpResults.isEmpty() && automaticCorrectionEnabled) {
                        if (isMatchingValueSet()) {
                            var old = matchingValue;
                            var nearestWords = findNearest(matchingValue);
                            if (!nearestWords.isEmpty()) {
                                matchingValue = nearestWords.get(0);
                                for (int i = 1; i < nearestWords.size(); i++) {
                                    set(or(nearestWords.get(i)));
                                }
                                automaticCorrections.put(old, nearestWords);
                                tmpResults = evaluateBothSimpleAndAggregatedExpressionRecursively();
                            }
                        }
                    }

                    yield tmpResults;
                }
            }
        };
        return results;

    }

    /**
     * @return all the automatic corrections (see {@link #automaticCorrections}), summarized in
     * a string, or an empty string if no automatic corrections were made.
     */
    @NotNull
    public String getAutomaticCorrections() {
        String toReturn = "";
        if (automaticCorrections.size() > 0) {
            toReturn = automaticCorrections.entrySet().stream()
                    .map(wrongWord_correctionList_entry ->
                            "\"" + wrongWord_correctionList_entry.getKey() + "\""
                                    + " not found, corrected with "
                                    + (wrongWord_correctionList_entry.getValue().size() == 1
                                    ? "" : "one of the following: ")
                                    + wrongWord_correctionList_entry.getValue()
                                    .stream().map(correction -> "\"" + correction + "\"")
                                    .collect(Collectors.joining(", ")))
                    .collect(Collectors.joining(System.lineSeparator()))
                    + System.lineSeparator();
        }
        if (isAggregated) {
            assert leftChildOperand != null;
            assert rightChildOperand != null;
            toReturn += leftChildOperand.getAutomaticCorrections()
                    + rightChildOperand.getAutomaticCorrections();
        }

        return toReturn;
    }

    /**
     * Searches for the nearest tokens in the dictionary of the {@link InvertedIndex}
     * of the {@link #informationRetrievalSystem} wrt. the input word and return them.
     * No normalization actions are taken on the input word.
     * This method makes use of the edit-distance concept.
     *
     * @param word A word (e.g., from an input query).
     * @return the {@link List} of the nearest word in the dictionary of the IR System
     * wrt. the input parameter, or null if not corrections are found. A {@link List}
     * is needed because there may exist more than one token with the same edit-distance
     * from the input word and that distance is the nearest.
     */
    @NotNull
    private List<String> findNearest(@NotNull String word) {
        //noinspection ConstantConditions
        assert word != null;

        return new SpellingCorrector(
                new it.units.informationretrieval.ir_boolean_model.utils.custom_types.Phrase(List.of(word)), false, true, informationRetrievalSystem, String::compareTo)
                .getNewCorrections()
                .stream()
                .map(it.units.informationretrieval.ir_boolean_model.utils.custom_types.Phrase::getListOfWords)
                .flatMap(Collection::stream)
                .toList();
    }

    /**
     * This method was created from a refactoring of method
     * {@link #evaluateBothSimpleAndAggregatedExpressionRecursively()}
     * and has the only responsibility to solve a {@link UNARY_OPERATOR#NOT}
     * query, i.e., it does <strong>NOT</strong> check if this instance
     * is aggregated nor anything like that: it just solves the
     * {@link UNARY_OPERATOR#NOT} query!
     *
     * @return The postings resulting from the evaluation of the
     * {@link UNARY_OPERATOR#NOT} query (for this instance of {@link BooleanExpression}).
     */
    @NotNull
    private SkipList<Posting> evaluateNotQuery() {
        // First: solve the direct query (create a new query without the NOT operator),
        // then take the difference to get the results for the NOT query.

        assert unaryOperator.equals(UNARY_OPERATOR.NOT);

        if (DEBUG_NOT_QUERY) {
            System.out.println("Evaluation of NOT query. DEBUG_NOT_QUERY flag is enabled, statistics are shown." +
                    " It can be disabled by app properties.");
        }

        long start = 0, stop = 0;
        if (DEBUG_NOT_QUERY) {
            start = System.nanoTime();
        }
        SkipList<Posting> listOfPostingsToBeExcluded =
                new BooleanExpression(this)
                        .setUnaryOperator(UNARY_OPERATOR.IDENTITY)
                        .evaluateBothSimpleAndAggregatedExpressionRecursively();
        if (DEBUG_NOT_QUERY) {
            stop = System.nanoTime();
            System.out.println("SkipList of postings to exclude computed in          " + (stop - start) / 1e6 + " ms.");
        }

        if (DEBUG_NOT_QUERY) {
            start = System.nanoTime();
        }
        var allPostings = informationRetrievalSystem.getAllPostings();
        if (DEBUG_NOT_QUERY) {
            stop = System.nanoTime();
            System.out.println("SkipList of all postings in the index obtained in    " + (stop - start) / 1e6 + " ms.");
        }

        if (DEBUG_NOT_QUERY) {
            start = System.nanoTime();
        }
        var difference = SkipList.difference(
                allPostings, listOfPostingsToBeExcluded, Comparator.naturalOrder()); // "true predicate" means "classical" difference between sets
        if (DEBUG_NOT_QUERY) {
            stop = System.nanoTime();
            System.out.println("SkipList of difference computed in                   " + (stop - start) / 1e6 + " ms.");
        }

        return difference;
    }

    /**
     * Evaluate this expression on the given {@link InvertedIndex}.
     *
     * @return the {@link Document}s matching this {@link BooleanExpression},
     * opportunely sorted according to their relevance to the query.
     * @throws UnsupportedOperationException If the operator for the expression is unknown.
     */
    @NotNull
    public List<Document> evaluate()
            throws UnsupportedOperationException {
        try {
            results = evaluateBothSimpleAndAggregatedExpressionRecursively();
        } catch (StackOverflowError e) {
            System.err.println("No more results will be shown due to low stack memory.");
            if (spellingCorrector != null) {    // Often spelling correction causes errors due to the high computational cost
                spellingCorrector.stop();
            }
        }
        assert results.stream().sorted().distinct().toList().equals(results.stream().toList());
        if (!maxNumberOfResultsSpecified) {
            maxNumberOfResults = results.size();
        }

        final var corpus = informationRetrievalSystem.getCorpus();
        if (RANK_RESULTS) {
            return new QueryResultsRanking().getRankedDocuments();
        } else {
            return results.stream()
                    .map(Posting::getDocId)
                    .map(corpus::getDocumentByDocId)
                    .limit(maxNumberOfResults)
                    .toList();
        }
    }

    /**
     * @return the input query string.
     */
    @NotNull
    public String getQueryString() throws UnsupportedOperationException {
        return getQueryWords(true, false);
    }

    /**
     * @param withOperator true if operators (AND/OR/NOT) must be included in the returned string.
     * @param normalize    true if operands ({@link #matchingValue} / {@link #matchingPhrase}) must
     *                     be normalized in the returned string.
     * @return the entire query string.
     */
    @NotNull
    private String getQueryWords(boolean withOperator, boolean normalize) {
        return switch (unaryOperator) {
            case NOT -> (withOperator ? "NOT " : "") +
                    new BooleanExpression(this).setUnaryOperator(UNARY_OPERATOR.IDENTITY).getQueryWords(withOperator, normalize);
            case IDENTITY -> {
                if (isAggregated) {
                    assert leftChildOperand != null;
                    assert binaryOperator != null;
                    assert rightChildOperand != null;
                    try {
                        yield (withOperator ? "( " : "")
                                + leftChildOperand.getQueryWords(withOperator, normalize)
                                + (withOperator ? " " + binaryOperator : "") + " "
                                + rightChildOperand.getQueryWords(withOperator, normalize)
                                + (withOperator ? " )" : "");
                    } catch (StackOverflowError e) {
                        yield withOperator ? "..." : "";
                    }
                } else {
                    if (normalize) {
                        var normalized = Utility.preprocess(queryString, true, informationRetrievalSystem.getLanguage());
                        yield normalized == null ? "" : normalized;
                    } else {
                        yield queryString;
                    }
                }
            }
        };
    }

    /**
     * @return true if a spelling correction was applied on this instance.
     */
    public boolean isSpellingCorrectionApplied() {
        if (spellingCorrector != null) {
            return true;
        } else if (isAggregated) {
            assert leftChildOperand != null;
            assert rightChildOperand != null;
            return leftChildOperand.isSpellingCorrectionApplied()
                    || rightChildOperand.isSpellingCorrectionApplied();
        } else {
            return false;
        }
    }

    /**
     * Edit-distance: if a spelling correction was applied, this method
     * returns the max edit-distance currently used between the user query
     * and the actual query to the IR system.
     *
     * @return the distance between words in this query (if it was spelling-corrected)
     * respect to the query inserted by the user or {@link Integer#MAX_VALUE} if
     * the user specified to use the maximum possible edit distance.
     */
    public int getEditDistanceForSpellingCorrection() {
        if (spellingCorrector != null) {
            return spellingCorrector.getOverallEditDistance();
        } else if (isAggregated) {
            assert leftChildOperand != null;
            assert rightChildOperand != null;
            assert leftChildOperand.getEditDistanceForSpellingCorrection()
                    == rightChildOperand.getEditDistanceForSpellingCorrection();
            return leftChildOperand.getEditDistanceForSpellingCorrection();
        } else {
            return 0;
        }
    }

    /**
     * @return the initial queryString without any spelling-correction.
     */
    public String getInitialQueryString() {
        return queryString;
    }

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
                    && words.length == Objects.requireNonNull(distanceFromFirstWord).length + 1
                    && distanceFromFirstWord[0] > 0
                    && IntStream.range(1, distanceFromFirstWord.length)
                    .allMatch(i -> distanceFromFirstWord[i] - distanceFromFirstWord[i - 1] > 0)) {
                this.words = words;
                this.distanceFromFirstWord = distanceFromFirstWord;
            } else {
                throw new IllegalArgumentException(
                        "\twords: " + Arrays.toString(words) + System.lineSeparator()
                                + "\tdistanceFromFirstWord: " + Arrays.toString(distanceFromFirstWord) + System.lineSeparator()
                                + "\twords.length: " + words.length
                                + ", distanceFromFirstWord.length: " + distanceFromFirstWord.length
                                + ", distances are monotonically growing: "
                                + (IntStream.range(1, distanceFromFirstWord.length)
                                .allMatch(i -> distanceFromFirstWord[i] - distanceFromFirstWord[i - 1] > 0)));
            }
        }

        /**
         * @return the number of words composing the phrase represented by this instance.
         */
        int size() {
            return words.length;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("\"");
            if (Arrays.equals(distanceFromFirstWord, IntStream.rangeClosed(1, distanceFromFirstWord.length).toArray())) {
                // all words in phrase are adjacent
                sb.append(String.join(" ", words));
            } else {
                int i = 0;
                for (; i < distanceFromFirstWord.length; i++) {
                    sb.append(words[i]).append("\\").append(distanceFromFirstWord[i]);
                }
                sb.append(words[i]);
            }
            return sb.append("\"").toString();
        }
    }

    /**
     * Class with capability to rank {@link Document}s.
     */
    public class QueryResultsRanking {

        /**
         * @return the {@link List} of results opportunely ranked.
         */
        @NotNull
        public List<Document> getRankedDocuments() {

            if (!alreadyEvaluated) {
                System.err.println("Trying to rank results of a NON-evaluated query");
                return new ArrayList<>();
            } else {

                Corpus corpus = informationRetrievalSystem.getCorpus();
                final int entireCorpusSize = corpus.size();

                return results.stream()
                        .collect(Collectors.toMap(
                                posting -> corpus.getDocumentByDocId(posting.getDocId()),                                   // docId as key
                                posting -> USE_WF_IDF ? posting.wfIdf(entireCorpusSize) : posting.tfIdf(entireCorpusSize),  // score as value
                                Double::sum,    // sum scores if more query terms are present in the same document
                                LinkedHashMap::new))
                        .entrySet().stream().sequential()
                        .map(entry_docToRank -> {
                            // Assign extra rank if any query term is present in the title of the document
                            double score = entry_docToRank.getValue();
                            List<String> queryTerms = Arrays.asList(
                                    Utility.split(getQueryWords(false, true)));
                            long extraScore = corpus.howManyCommonNormalizedWordsWithDocTitle(
                                    entry_docToRank.getKey(), queryTerms);
                            BiFunction<Double, Long, Double> assignExtraScore = (initialScore, extraScore_) ->
                                    extraScore_ > 1 ? initialScore * extraScore_ : extraScore_ + initialScore;
                            score = assignExtraScore.apply(score, extraScore);
                            return new Pair<>(entry_docToRank.getKey(), score);
                        })
                        .sorted(Map.Entry.<Document, Double>comparingByValue().reversed()) // highest rank first
                        .map(Map.Entry::getKey)
                        .limit(maxNumberOfResults)
                        .toList();
            }
        }
    }
}