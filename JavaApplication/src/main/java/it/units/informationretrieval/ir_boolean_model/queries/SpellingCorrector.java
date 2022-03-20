package it.units.informationretrieval.ir_boolean_model.queries;

import edit_distance.entities.EditDistanceCalculator;
import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.Phrase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class ables to perform the spelling correction.
 * This class makes use of {@link edit_distance.entities.EditDistanceCalculator}
 * and, for a phrase, the edit-distance is considered as the
 * sum of the edit-distances of the words composing the phrase
 * wrt. the initial phrase.
 * <p>
 * Applying spelling correction on phrase is quit difficult, because
 * the algorithm must care about considering all the combinations (correcting
 * first words once per time, then keeping one word corrected and
 * correcting the other once per time and then correcting the one word
 * twice and keep the other un-corrected and so on).
 * The following table summarizes the correction schema.
 * All entries between two horizontal lines correspond to the schema of
 * phrase to be returned at the same invocation of
 * {@link SpellingCorrector#getNewCorrections()} applied to a phrase of tree
 * words.
 * <table>
 *     <tr style="border-bottom: 1px solid">
 *         <th>Invocation of {@link SpellingCorrector#getNewCorrections()}</th>|
 *         <th>One-word edit distance</th>|
 *         <th>Overall edit-distance</th>
 *     </tr>
 *
 *     <tr style="border-bottom: 1px solid;">
 *         <td>0</td>|<td>0 0 0</td>|<td>0</td>
 *     </tr>
 *
 *     <tr>
 *         <td></td>|<td>0 0 1</td>|<td>1</td>
 *     </tr>
 *     <tr>
 *         <td>1</td>|<td>0 1 0</td>|<td>1</td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td></td>|<td>1 0 0</td>|<td>1</td>
 *     </tr>
 *
 *     <tr>
 *         <td></td>|<td>0 1 1</td>|<td>2</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>1 1 0</td>|<td>2</td>
 *     </tr>
 *     <tr>
 *         <td>2</td>|<td>1 0 1</td>|<td>2</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>0 0 2</td>|<td>2</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>0 2 0</td>|<td>2</td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td></td>|<td>2 0 0</td>|<td>2</td>
 *     </tr>
 *
 *     <tr>
 *         <td></td>|<td>1 1 1</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>0 1 2</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>1 2 0</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>1 0 2</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td>3</td>|<td>0 2 1</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>2 1 0</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>2 0 1</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>0 0 3</td>|<td>3</td>
 *     </tr>
 *     <tr>
 *         <td></td>|<td>0 3 0</td>|<td>3</td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;">
 *         <td></td>|<td>3 0 0</td>|<td>3</td>
 *     </tr>
 *     <tr style="border-bottom: 1px solid;"><td>...</td>|<td>...</td>|<td>...</td></tr>
 *
 * </table>
 *
 * @author Matteo Ferfoglia
 */
class SpellingCorrector {

    /**
     * The length of the suffix to remove from the rotations
     * of words to be corrected (see {@link #getNewCorrections()}).
     * If the string to be corrected is shorter than the value
     * specified by this field, no corrections will be applied.
     */
    private static final int SUFFIX_LENGTH = 2;

    /**
     * The {@link SpellingCorrector} must be able to correct also phrases,
     * hence the words to correct are saved into the list (this field).
     * If a single word must be corrected, you simply will have a {@link List}
     * containing only one element.
     */
    @NotNull
    private final Phrase PHRASE_TO_CORRECT;

    /**
     * The {@link InformationRetrievalSystem} to use for corrections.
     */
    @NotNull
    private final InformationRetrievalSystem informationRetrievalSystem;

    /**
     * The comparator to use to sort words when they have the same edit-distance.
     */
    @NotNull
    private final Comparator<String> comparator;

    /**
     * Caches a {@link Map} having as key a word (let id be w) and as value another
     * {@link Map} which saves as value a {@link List} of words ({@link String}s)
     * and as corresponding key the edit-distance between each of the word in the
     * inner {@link List} and the word w.
     */
    @NotNull
    private final ConcurrentMap<String, ConcurrentMap<Integer, List<String>>>
            correctionsCache = new ConcurrentHashMap<>();

    /**
     * True if this instance will handle phone corrections or false for spelling corrections.
     */
    private final boolean PHONETIC_CORRECTION;

    /**
     * False if this edit distance must be ignored.
     * This flag is ignored if {@link #PHONETIC_CORRECTION} is false.
     */
    private final boolean USE_EDIT_DISTANCE;

    /**
     * Similar to {@link #correctionsCache} but used for phonetic correction.
     * This field caches a {@link Map} having as key a word (let id be w) and as value
     * another {@link Map} which saves as value a {@link List} of words ({@link String}s)
     * and as corresponding key the edit-distance between each of the word in the
     * inner {@link List} and the word w.
     * The inner-most list of words is computed according to the Soundex algorithm.
     */
    @NotNull
    private final ConcurrentMap<String, ConcurrentMap<Integer, List<String>>>
            phoneticCorrectionsCache = new ConcurrentHashMap<>();

    /**
     * Saves the overall edit-distance between the last returned instance
     * from {@link #getNewCorrections()} and the initial {@link #PHRASE_TO_CORRECT}.
     */
    private int overallEditDistance = 0;

    /**
     * Saves the overall edit-distance that was before {@link #getNewCorrections()} ()} invocation.
     * This field is used to determine if further corrections are still possible.
     */
    private int oldOverallEditDistance = overallEditDistance - 1; // initial value

    /**
     * Changes status according to {@link #stop()}.
     */
    private boolean stopped = false;

    /**
     * Constructor.
     *
     * @param phraseToCorrect            The phrase to be corrected.
     * @param phoneticCorrection         True if phonetic correction must be performed,
     *                                   false if "classic" spelling correction is desired.
     * @param useEditDistance            is a flag which is <strong>ignored if</strong> a
     *                                   <strong>non</strong> phonetic correction is being
     *                                   performed, and, if it is set to true, the edit distance
     *                                   is used (like in normal spelling correction), while if
     *                                   it is set to false, all possible phonetic corrections
     *                                   will be considered, independently of how far the resulting
     *                                   correction is from the initial query.
     * @param informationRetrievalSystem The {@link InformationRetrievalSystem} to use for corrections.
     * @param comparator                 The comparator to use to sort words when they have the same edit-distance.
     */
    public SpellingCorrector(@NotNull final Phrase phraseToCorrect,
                             boolean phoneticCorrection,
                             boolean useEditDistance,
                             @NotNull final InformationRetrievalSystem informationRetrievalSystem,
                             @NotNull final Comparator<String> comparator) {
        this.PHRASE_TO_CORRECT = Objects.requireNonNull(phraseToCorrect);
        this.informationRetrievalSystem = Objects.requireNonNull(informationRetrievalSystem);
        this.comparator = Objects.requireNonNull(comparator);
        this.PHONETIC_CORRECTION = phoneticCorrection;
        if (phoneticCorrection) {
            this.USE_EDIT_DISTANCE = useEditDistance;
            if (!useEditDistance) {
                this.overallEditDistance = // fictitious value to ignore edit distance to be sure to ignore the edit distance
                        2 * PHRASE_TO_CORRECT.getListOfWords().stream().mapToInt(String::length).sum();
            }
        } else {
            this.USE_EDIT_DISTANCE = true;
        }
    }

    /**
     * @param irs The {@link InformationRetrievalSystem}.
     * @return The comparator to sort corrected query words after applying spelling
     * correction if they have the same edit-distance wrt. the initial query string.
     */
    @NotNull
    public static Comparator<String> spellingCorrectedQueryWordsComparatorFactory(
            @NotNull InformationRetrievalSystem irs) {
        return (s1, s2) -> {
            int comparison = s1.compareTo(s2);
            return comparison == 0  // if same edit-distance, then give precedence to the most frequent term
                    ? irs.getTotalNumberOfOccurrencesOfTerm(s2)
                    - irs.getTotalNumberOfOccurrencesOfTerm(s1)
                    : comparison;
        };
    }

    /**
     * This method returns a {@link List} in which each element is a {@link List}
     * of {@link Integer}s, where (in the inner-most list) the i-th element is
     * the edit-distance that the i-th spelling corrected word in the phrase
     * should have in order to reach an overall (for the entire phrase)
     * edit-distance equals to the one given as parameter.
     * Because of there may exist more combinations of different edit-distances
     * which lead to the same overall edit-distance, this method returns a
     * {@link List} containing all possible distinct combinations.
     * <p/>
     * <strong>Important</strong>: this method involves a lot of combinations
     * hence it is computationally very expensive.
     *
     * @param targetOverallDistance The target overall distance.
     * @return the {@link List} of all possible combinations of single-word
     * edit-distances in the phrase such that the overall distance is equals
     * to the one given as parameter.
     */
    private List<List<Integer>> getEditDistances(int targetOverallDistance) {
        return IntStream.rangeClosed(0, targetOverallDistance)
                .mapToObj(overallDistance -> IntStream.range(0, PHRASE_TO_CORRECT.size())
                        .mapToObj(wordIndex -> IntStream.rangeClosed(0, overallDistance).boxed().toList())
                        .toList())
                .map(Utility::getCartesianProduct)
                .flatMap(Collection::stream)
                .distinct()
                .filter(intList -> intList.stream().mapToInt(i -> i).sum() == targetOverallDistance)
                .toList();
    }

    /**
     * Returns the {@link List} of {@link Phrase}s with a correction, hence
     * the edit distance between each element of the instance returned by this
     * method and the initial phrase is (if any correction is possible) strictly
     * greater than 1 (&gt;1).
     * <p/>
     * <strong>Important</strong>: if this method is invoked multiple times,
     * each invocation will return a different instance (until any correction
     * will be possible) and each element of each returned instance will have an
     * edit-distance which is greater than equal to (&ge;) the instance returned
     * from the previous invocation.
     *
     * <strong>Important</strong>: this method involves a lot of combinations
     * hence it is computationally very expensive.
     *
     * @return the phrase with a correction (if any correction is possible).
     */
    @NotNull
    public List<Phrase> getNewCorrections() {
        oldOverallEditDistance = overallEditDistance++;
        var results = getCorrections(overallEditDistance);
        if (results.isEmpty() && USE_EDIT_DISTANCE) {
            // no corrections were made
            overallEditDistance--;
        }
        if (!USE_EDIT_DISTANCE) {
            // during this invocation all results were retrieved ignoring how far they are from the input query
            // so there is no sense to repeat the correction in the future because there will not be more results.
            stop();
            overallEditDistance = Integer.MAX_VALUE;    // fictitious value
        }
        return results;
    }

    /**
     * This method actually does what explained by {@link #getNewCorrections()},
     * but this method does not change the value of {@link #overallEditDistance},
     * which is taken as input parameter.
     *
     * @param overallEditDistance The desired overall edit-distance.
     */
    private List<Phrase> getCorrections(int overallEditDistance) {

        if (isPossibleToCorrect()) {
            var editDistancesAfterCorrection = getEditDistances(overallEditDistance);
            var corrections = editDistancesAfterCorrection
                    .stream()
                    .map(oneTupleOfEditDistancesAfterCorrection ->
                            IntStream.range(0, oneTupleOfEditDistancesAfterCorrection.size())
                                    .mapToObj(wordIndex -> correct(
                                            PHRASE_TO_CORRECT.getWordAt(wordIndex),
                                            oneTupleOfEditDistancesAfterCorrection.get(wordIndex)))
                                    .toList())
                    .map(Utility::getCartesianProduct)
                    .flatMap(Collection::stream)
                    .map(Phrase::new)
                    .toList();
            if (corrections.isEmpty()) {
                boolean furtherCorrectionsPossible = PHRASE_TO_CORRECT.getListOfWords()
                        .stream()
                        .map(correctionsCache::get)
                        .filter(Objects::nonNull)
                        .map(Map::keySet)
                        .flatMap(Collection::stream)
                        .mapToInt(i -> i)
                        .filter(editDistanceComputed -> editDistanceComputed >= overallEditDistance)
                        .findAny()
                        .isPresent();
                return furtherCorrectionsPossible ? getNewCorrections() : corrections;
            }
            return corrections;
        } else {
            return new ArrayList<>();
        }

    }

    /**
     * @return the currently considered edit-distance wrt. the initial query.
     * A returned value of {@link Integer#MAX_VALUE} means that the edit-distance
     * is not used by this instance.
     */
    public int getOverallEditDistance() {
        return overallEditDistance;
    }

    /**
     * @return true if further corrections are possible, false otherwise.
     */
    public boolean isPossibleToCorrect() {
        // if counter was not updated, means that no further corrections are possible
        return !stopped && oldOverallEditDistance < overallEditDistance;
    }

    /**
     * Corrects the given query word (single word) and returns a {@link List}
     * of words which have an edit-distance from the given word of exactly
     * the given value.
     * This method performs either the classic spelling correction or the
     * phonetic correction, according to {@link #PHONETIC_CORRECTION}.
     *
     * @param queryWord          The word to correct.
     * @param targetEditDistance The desired edit-distance which must be present
     *                           between returned corrected words and the input
     *                           word.
     * @return The {@link List} (eventually empty) of corrections, sorted according
     * the {@link #comparator} given to this instance.
     */
    @NotNull
    private List<String> correct(String queryWord, int targetEditDistance) {

        List<String> correctionForTargetDistance;
        var cache = PHONETIC_CORRECTION ? phoneticCorrectionsCache : correctionsCache;

        // First: check if present in cache
        @Nullable // null if not present
        var correctionsMapIfPresent = cache.get(queryWord);

        if (correctionsMapIfPresent != null
                && (correctionForTargetDistance = correctionsMapIfPresent.get(targetEditDistance)) != null) {

            return correctionForTargetDistance;

        } else {

            final String normalizedQueryWord = Utility.preprocess(
                    queryWord, !PHONETIC_CORRECTION, informationRetrievalSystem.getLanguage());
            ConcurrentMap<Integer, List<String>> mapOfCorrectionsHavingDistanceAsKey = null;

            if (normalizedQueryWord != null) {

                String[] rotations;
                if (!PHONETIC_CORRECTION) {
                    rotations = Utility.getAllRotationsOf(normalizedQueryWord);
                } else {
                    // no rotations for phonetic correction
                    rotations = new String[]{normalizedQueryWord};
                }
                mapOfCorrectionsHavingDistanceAsKey =
                        Arrays.stream(rotations)
                                .parallel()
                                .filter(s -> PHONETIC_CORRECTION || s.length() > SUFFIX_LENGTH)
                                .map(s -> PHONETIC_CORRECTION ? s : s.substring(0, s.length() - SUFFIX_LENGTH))
                                .map(s -> PHONETIC_CORRECTION
                                        ? informationRetrievalSystem.getDictionaryTermsFromSoundexCorrectionOf(s)
                                        : informationRetrievalSystem.getDictionaryTermsContainingPrefix(s, true))
                                .flatMap(Collection::stream)
                                .distinct()
                                .map(termFromDictionary -> new Pair<>(
                                        termFromDictionary,
                                        new EditDistanceCalculator(
                                                normalizedQueryWord, termFromDictionary).getEditDistance()))
                                .collect(Collectors.groupingByConcurrent(
                                        Map.Entry::getValue/*group by edit-distance*/,
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                list -> list.stream()
                                                        .map(Map.Entry::getKey)
                                                        .sorted(comparator)
                                                        .toList())));
                cache.put(queryWord, mapOfCorrectionsHavingDistanceAsKey);
            } else {
                cache.put(queryWord, new ConcurrentHashMap<>(0));
            }

            assert mapOfCorrectionsHavingDistanceAsKey != null; // map must be initialized in one of IF branches
            return USE_EDIT_DISTANCE
                    ? mapOfCorrectionsHavingDistanceAsKey.getOrDefault(targetEditDistance, new ArrayList<>())
                    : mapOfCorrectionsHavingDistanceAsKey.values().stream().flatMap(Collection::stream).toList();

        }
    }

    /**
     * If invoked, this method will stop any further spelling-correction
     * procedure, like if no more results are available.
     */
    public void stop() {
        stopped = true;
        oldOverallEditDistance = overallEditDistance;
    }
}
