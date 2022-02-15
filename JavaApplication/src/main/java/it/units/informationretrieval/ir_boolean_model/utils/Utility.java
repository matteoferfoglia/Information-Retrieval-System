package it.units.informationretrieval.ir_boolean_model.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndex;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import skiplist.SkipList;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Utility class.
 *
 * @author Matteo Ferfoglia
 */
public class Utility {

    /**
     * Regex of valid chars (for string normalization purposes).
     */
    private static final String REGEX_VALID_CHARACTERS = "a-zA-Z0-9\\s";

    /**
     * Regex of punctuation chars (for string normalization purposes).
     */
    private static final String REGEX_PUNCTUATION = "[|!\"£$%&\\/=?^_*\\-+,.:;#]";

    /**
     * Regex used in {@link #normalize(String, boolean, Language)}.
     */
    private static final String REGEX__NOT__VALID_CHARACTERS_WHEN_INDEXING = "[^" + REGEX_VALID_CHARACTERS + "]";

    /**
     * Regex used in {@link #normalize(String, boolean, Language)}.
     */
    private static final String REGEX__NOT__VALID_CHARACTERS_WHEN_QUERYING =
            "[^" + REGEX_VALID_CHARACTERS + InvertedIndex.ESCAPED_WILDCARD_FOR_REGEX + "]";

    /**
     * Regex used in {@link #normalize(String, boolean, Language)}.
     */
    private static final String REGEX_MULTIPLE_SPACES = "\\s+";

    /**
     * Caches the (eventually null) {@link Stemmer} to use.
     */
    @Nullable
    private static Stemmer cachedStemmed = null;
    /**
     * Flag: true if {@link #cachedStemmed} has been initialized.
     */
    private static boolean cachedStemmedInitialized;

    /**
     * Tokenize a {@link Document} and return the {@link java.util.List} of tokens as
     * {@link String} (eventually with duplicates) obtained from the {@link Document}.
     *
     * @param document       The {@link Document} to tokenize.
     * @param language       The {@link Language} of the document.
     * @param unstemmedWords A collection where this method will add terms before stemming
     *                       (it is used as output parameter if the caller needs to know the
     *                       un-stemmed version of the input token). Note: token inserted in the
     *                       collection might be equal to the returned one (e.g., if the system is
     *                       set to avoid stemming or if no stemming step are required on the
     *                       input token).
     */
    @NotNull
    public static String[] tokenize(
            @NotNull Document document, @NotNull Language language, @NotNull SynchronizedSet<String> unstemmedWords) {
        assert document.getContent() != null;
        return Arrays.stream(
                        split(/*title is included in the content*/document.getContent().getEntireTextContent()
                                .replaceAll(REGEX_PUNCTUATION, " ")))
                .filter(text -> !text.isBlank())
                .map(token -> Utility.normalize(token, false, language, unstemmedWords))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    /**
     * Tokenize a {@link Document} and return the {@link java.util.Map} having as key
     * a token and as corresponding value the sorted array of positions in the
     * {@link Document} at which the token in the key appears.
     * {@link String} (eventually with duplicates) obtained from the {@link Document}.
     *
     * @param document       The {@link Document} to tokenize.
     * @param language       The {@link Language} of the document.
     * @param unstemmedWords A collection where this method will add terms before stemming
     *                       (it is used as output parameter if the caller needs to know the
     *                       un-stemmed version of the input token). Note: token inserted in the
     *                       collection might be equal to the returned one (e.g., if the system is
     *                       set to avoid stemming or if no stemming step are required on the
     *                       input token).
     */
    @NotNull
    public static Map<String, int[]> tokenizeAndGetMapWithPositionsInDocument(
            @NotNull final Document document, @NotNull Language language, @NotNull SynchronizedSet<String> unstemmedWords) {
        String[] tokensEventuallyDuplicatesSortedByPositionInDocument = tokenize(document, language, unstemmedWords);
        Map<String, Set<Integer>> mapTokenToPositionsInDoc = IntStream
                .range(0, tokensEventuallyDuplicatesSortedByPositionInDocument.length)
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(
                        tokensEventuallyDuplicatesSortedByPositionInDocument[i], i))
                .collect(Collectors.groupingByConcurrent(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, toSet())));
        return mapTokenToPositionsInDoc.entrySet()
                .stream()
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().sorted().mapToInt(i -> i).toArray(),
                        (a, b) -> {
                            throw new IllegalStateException("No duplicates should be present, but were.");
                            // entries already grouped by token, hence no duplicates should be present
                        },
                        ConcurrentHashMap::new));
    }

    /**
     * Normalize a token ({@link String}) and add to the input collection the normalized
     * but non-stemmed token.
     *
     * @param token          The {@link String token} to be normalized.
     * @param fromQuery      True if this method was invoked to removeInvalidCharsAndToLowerCase a query,
     *                       false if it was invoked to removeInvalidCharsAndToLowerCase a word while
     *                       indexing process. This differentiation is made because
     *                       the user should be able to insert special characters
     *                       (like wildcards) in queries, but special characters
     *                       should not be present in the dictionary of the index.
     * @param language       The language of the input token.
     * @param unstemmedWords A {@link SynchronizedSet} where this method will add terms before
     *                       stemming (it is used as output parameter if the caller needs to know
     *                       the un-stemmed version of the input token). Note: token inserted in
     *                       the collection might be equal to the returned one (e.g., if the system
     *                       is set to avoid stemming or if no stemming step are required on the
     *                       input token).
     * @return the corresponding normalized token or null either if the normalization
     * leads to an empty string or (in the case that stop-words must be excluded) if
     * the input string is a stop word.
     */
    @Nullable
    public static String normalize(
            @NotNull String token, boolean fromQuery,
            @NotNull Language language, @NotNull SynchronizedSet<String> unstemmedWords) {

        String toReturn = removeInvalidCharsAndToLowerCase(token, fromQuery);

        Stemmer stemmer = getStemmer();

        // Stop-words handling (when stemming is not performed)
        if (stemmer == null && shouldExcludeStopWords()) {
            if (isStopWord(toReturn, language, false)) {
                return null;
            }
        }

        // Stemming
        unstemmedWords.add(toReturn);   // before stemming, save the un-stemmed word
        if (stemmer != null && !toReturn.contains(InvertedIndex.WILDCARD)) {
            toReturn = stemmer.stem(toReturn, language);
        }

        // Stop-words handling (when stemming is performed)
        if (shouldExcludeStopWords()) {
            if (isStopWord(toReturn, language, true)) {
                return null;
            }
        }

        return toReturn.isBlank() ? null : toReturn;
    }

    /**
     * Normalize a token ({@link String}).
     *
     * @param token     The {@link String token} to be normalized.
     * @param fromQuery True if this method was invoked to removeInvalidCharsAndToLowerCase a query,
     *                  false if it was invoked to removeInvalidCharsAndToLowerCase a word while
     *                  indexing process. This differentiation is made because
     *                  the user should be able to insert special characters
     *                  (like wildcards) in queries, but special characters
     *                  should not be present in the dictionary of the index.
     * @param language  The language of the input token.
     * @return the corresponding normalized token or null either if the normalization
     * leads to an empty string or (in the case that stop-words must be excluded) if
     * the input string is a stop word.
     */
    @Nullable
    public static String normalize(@NotNull String token, boolean fromQuery, @NotNull Language language) {
        return normalize(token, fromQuery, language, new SynchronizedSet<>());
    }

    /**
     * @return the {@link Stemmer} to use or null, if the IR System is configured
     * to not performing stemming or if no {@link Stemmer} is available for the
     * name set in the IR System config file.
     */
    @Nullable
    public static Stemmer getStemmer() {
        if (!cachedStemmedInitialized) {
            cachedStemmed = null;
            try {
                String stemmerName = AppProperties.getInstance().get("app.stemmer");
                cachedStemmed = Stemmer.getStemmer(Stemmer.AvailableStemmer.valueOf_(stemmerName));
            } catch (IOException e) {
                System.err.println("Error while reading app properties. Stemming will not be performed.");
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                System.err.println("Stemmer not available while reading app properties. Stemming will not be performed.");
                e.printStackTrace();
            }
            cachedStemmedInitialized = true;
        }
        return cachedStemmed;
    }

    /**
     * @return true if the IR system is configured to exclude stop words.
     */
    private static boolean shouldExcludeStopWords() {
        try {
            return Boolean.parseBoolean(AppProperties.getInstance().get("app.exclude_stop_words"));
        } catch (IOException e) {
            System.err.println("Error while reading app properties. Stop words not excluded.");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * @param word     The word.
     * @param language The language of the word.
     * @param stemming True if stemming must be applied to stop-words, false otherwise.
     * @return true if the given word is a stop word for the given language.
     */
    public static boolean isStopWord(String word, @NotNull Language language, boolean stemming) {
        return Arrays.asList(stemming
                ? Stemmer.getStemmedStopWords.apply(
                getStemmer() == null
                        ? Stemmer.AvailableStemmer.NO_STEMMING
                        : Stemmer.AvailableStemmer.fromStemmer(getStemmer()), language)
                : language.getStopWords()).contains(word);
    }

    /**
     * Like {@link #normalize(String, boolean, Language)}, but without stop-words removal.
     */
    @NotNull
    public static String removeInvalidCharsAndToLowerCase(@NotNull String token, boolean fromQuery) {
        return token
                .replaceAll(
                        fromQuery
                                ? REGEX__NOT__VALID_CHARACTERS_WHEN_QUERYING
                                : REGEX__NOT__VALID_CHARACTERS_WHEN_INDEXING,
                        "")
                .replaceAll(REGEX_MULTIPLE_SPACES, " ")
                .toLowerCase(Locale.ROOT)
                .strip();
    }

    /**
     * Print an exception stacktrace to a string and return the string.
     *
     * @param e An exception.
     * @return The stacktrace obtained from {@link Exception#printStackTrace()} as a {@link String}.
     */
    @NotNull
    public static String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Convert a string in JSON format to a {@link Map}.
     */
    @NotNull
    public static Map<String, ?> convertFromJsonToMap(@NotNull final String stringInJsonFormat) throws JsonProcessingException {
        return ((HashMap<?, ?>) new ObjectMapper().readValue(Objects.requireNonNull(stringInJsonFormat), HashMap.class))
                .entrySet().stream().unordered().parallel() // order does not matter in JSON entries
                .map(entry -> new AbstractMap.SimpleEntry<String, Object>((String) entry.getKey(), entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Convert a given object to JSON format.
     * <strong>Notice:</strong> special characters eventually present
     * (like "\n", "\r", ...) are escaped.
     *
     * @param object The object to convert to JSON.
     * @return The string representing the given object in JSON format.
     * @throws JsonProcessingException If an error occurs during the conversion to JSON.
     */
    @NotNull
    public static String convertToJson(@NotNull Object object)
            throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(Objects.requireNonNull(object));
    }

    /**
     * Sort and remove duplicates from the given {@link List} (a new instance
     * is created and returned).
     * <strong>Important: </strong> in order to correctly remove duplicates,
     * the equals() method must be defined opportunely.
     *
     * @param <T>  the type of each element of the {@link List}.
     * @param list the input {@link List}
     * @return the {@link List} resulting from the execution of this method.
     */
    @NotNull
    public static <T> List<T> sortAndRemoveDuplicates(@NotNull final List<T> list) {
        return list.stream().sorted().distinct().collect(toList());
    }

    /**
     * @param <T> Type of each element of the {@link  List}s.
     * @param a   Sorted input list.
     * @param b   Sorted input list.
     * @return the {@link List} corresponding to the union of the given input lists.
     */
    @NotNull
    public static <T extends Comparable<T>> List<T> unionOfSortedLists(@NotNull List<T> a, @NotNull List<T> b) {
        ArrayList<T> union = new ArrayList<>(a.size() + b.size());
        int i = 0, j = 0, comparison;
        while (i < a.size() && j < b.size()) {
            comparison = a.get(i).compareTo(b.get(j));
            if (comparison == 0) {
                union.add(a.get(i++));
                j++;
            } else if (comparison < 0) {
                union.add(a.get(i++));
            } else {
                union.add(b.get(j++));
            }
        }
        union.addAll(a.subList(i, a.size()));
        union.addAll(b.subList(j, b.size()));
        union.trimToSize();
        return union;
    }

    /**
     * @param <T> Type of each element of the {@link  List}s.
     * @param a   Sorted input list.
     * @param b   Sorted input list.
     * @return the {@link List} corresponding to the intersection of the given input lists.
     */
    @NotNull
    public static <T extends Comparable<T>> List<T> intersectionOfSortedLists(
            @NotNull List<T> a, @NotNull List<T> b) {

        ArrayList<T> intersection = new ArrayList<>(a.size());
        int i = 0, j = 0, comparison;
        while (i < a.size() && j < b.size()) {
            comparison = a.get(i).compareTo(b.get(j));
            if (comparison == 0) {
                intersection.add(a.get(i++));
                j++;
            } else if (comparison < 0) {
                i++;
            } else {
                j++;
            }
        }
        intersection.trimToSize();

        return intersection;
    }

    /**
     * Like {@link #intersectionOfSortedLists(List, List)}, but this method is
     * specific for {@link SkipList}s.
     *
     * @param <T>              Type of each element of the {@link  SkipList}s.
     * @param listsToIntersect The lists to intersect.
     * @return the {@link SkipList} corresponding to the intersection of the given input lists.
     */
    @SuppressWarnings("UnusedReturnValue") // utility method
    @SafeVarargs
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection(
            @NotNull SkipList<T>... listsToIntersect) {
        return SkipList.intersection(listsToIntersect);
    }

    /**
     * Like {@link #intersectionOfSortedLists(List, List)}, but this method is
     * specific for {@link SkipList}s.
     *
     * @param <T>        Type of each element of the {@link  SkipList}s.
     * @param a          The first list.
     * @param b          The second list.
     * @param comparator The comparator to use to compare instances.
     * @return the {@link SkipList} corresponding to the intersection of the given input lists.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> intersection(
            @NotNull SkipList<T> a, @NotNull SkipList<T> b, @NotNull Comparator<T> comparator) {
        return SkipList.intersection(a, b, comparator);
    }

    /**
     * Like {@link #unionOfSortedLists(List, List)}, but this method is
     * specific for {@link SkipList}s.
     *
     * @param <T>        Type of each element of the {@link  SkipList}s.
     * @param a          The first list.
     * @param b          The second list.
     * @param comparator The comparator to use to compare instances.
     * @return the {@link SkipList} corresponding to the intersection of the given input lists.
     */
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> union(
            @NotNull SkipList<T> a, @NotNull SkipList<T> b, @NotNull Comparator<T> comparator) {
        return SkipList.union(a, b, comparator);
    }

    /**
     * Like {@link #unionOfSortedLists(List, List)}, but this method is
     * specific for {@link SkipList}s.
     *
     * @param <T>          Type of each element of the {@link  SkipList}s.
     * @param listsToUnion The lists for which the union will be computed.
     * @return the {@link SkipList} corresponding to the union of the given input lists.
     */
    @SuppressWarnings("UnusedReturnValue") // utility method
    @SafeVarargs
    @NotNull
    public static <T extends Comparable<T>> SkipList<T> union(
            @NotNull SkipList<T>... listsToUnion) {
        return SkipList.union(listsToUnion);
    }

    /**
     * Writes given content to the given file.
     * A new file is created if it does not exist.
     *
     * @param whatToWrite               The content to write on file.
     * @param outputFile                Output file where to write the content.
     * @param appendIfFileAlreadyExists if the given file already exists, this flag
     *                                  determines if overwriting its content (if the flag
     *                                  is true) or appending the new content to the
     *                                  already present one (if the flag is false).
     * @throws IOException See {@link Files#write(Path, byte[], OpenOption...)} for exceptions thrown.
     */
    public static void writeToFile(
            @NotNull final String whatToWrite, @NotNull final File outputFile, boolean appendIfFileAlreadyExists)
            throws IOException {
        if (!outputFile.exists()
                && (outputFile.getParentFile() == null || Files.createDirectories(outputFile.getParentFile().toPath()) != null) // create parent directories if not existing
                && !outputFile.createNewFile()) {
            throw new IOException("Error when creating new file");
        }
        Files.write(
                outputFile.toPath(),
                whatToWrite.getBytes(),
                StandardOpenOption.WRITE,
                appendIfFileAlreadyExists ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * Replaces some characters (like quotes) such that the resulting string
     * can be used for JSON attributes (names or values)
     *
     * @param str The input string to be encoded.
     */
    public static String encodeForJson(@NotNull String str) {
        return str
                .replaceAll("\\r\\n", "\\\\r\\\\n")
                .replaceAll("\\n", "\\\\n")
                .replaceAll("\"", "\\\\\"")
                .replaceAll("'", "\\\\'");

    }

    /**
     * @param input Input string.
     * @return The array with all the rotations of the given input string.
     */
    @NotNull
    public static String[] getAllRotationsOf(@NotNull String input) {
        final char[] charArray = input.toCharArray();
        final int stringLength = charArray.length;
        return IntStream.range(0, stringLength)
                .mapToObj(i -> IntStream.range(i, i + stringLength)
                        .sequential()
                        .mapToObj(j -> String.valueOf(charArray[j % stringLength]))
                        .collect(Collectors.joining()))
                .toArray(String[]::new);
    }

    /**
     * Computes and returns the cartesian product of the input lists
     * (all combinations).
     *
     * @param lists Input lists.
     * @return the list of lists which is the cartesian product of the input list.
     */
    public static <T> List<List<T>> getCartesianProduct(List<List<T>> lists) {
        lists = lists.stream().filter(Objects::nonNull).collect(Collectors.toList());
        final int CARTESIAN_PRODUCT_SIZE = lists
                .stream().mapToInt(List::size).reduce((a, b) -> a * b).orElse(0);
        List<List<T>> resultLists = new ArrayList<>(CARTESIAN_PRODUCT_SIZE);
        if (lists.size() == 0) {
            resultLists.add(new ArrayList<>());
            return resultLists;
        } else {
            List<T> firstList = lists.get(0);
            List<List<T>> remainingLists = getCartesianProduct(lists.subList(1, lists.size()));
            for (T el : firstList) {
                for (List<T> remainingList : remainingLists) {
                    List<T> resultList = new ArrayList<>(1 + remainingList.size());
                    resultList.add(el);
                    resultList.addAll(remainingList);
                    resultLists.add(resultList);
                }
            }
        }
        return resultLists;
    }

    /**
     * Splits an input {@link String} to an array.
     *
     * @param toSplit The input {@link String} to be split.
     * @return the array obtained by splitting the input {@link String}.
     */
    public static String[] split(@NotNull String toSplit) {
        return toSplit.split(REGEX_MULTIPLE_SPACES);
    }

    /**
     * Replicates the given {@link String} for the specified number of times.
     *
     * @param N                 The desired number of replications
     * @param stringToReplicate the {@link String} to replicate.
     * @return the input string replicated for the desired number of times.
     */
    @NotNull
    public static String getNReplicationsOfString(int N, String stringToReplicate) {
        return new String(new char[N]).replace("\0", stringToReplicate);
    }

}
