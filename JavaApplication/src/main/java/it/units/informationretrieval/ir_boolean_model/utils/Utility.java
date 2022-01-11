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
import java.util.function.BiFunction;
import java.util.function.Function;
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
     * Regex used in {@link #normalize(String, boolean, Language)}.
     */
    private static final String REGEX__NOT__VALID_CHARACTERS_WHEN_INDEXING = "[^a-zA-Z0-9 ]";

    /**
     * Regex used in {@link #normalize(String, boolean, Language)}.
     */
    private static final String REGEX__NOT__VALID_CHARACTERS_WHEN_QUERYING =
            "[^a-zA-Z0-9 " + InvertedIndex.ESCAPED_WILDCARD_FOR_REGEX + "]";

    /**
     * Regex used in {@link #normalize(String, boolean, Language)}.
     */
    private static final String REGEX_MULTIPLE_SPACES = "\\s+";

    /**
     * Tokenize a {@link Document} and return the {@link java.util.List} of tokens as
     * {@link String} (eventually with duplicates) obtained from the {@link Document}.
     *
     * @param document The {@link Document} to tokenize.
     * @param language The {@link Language} of the document.
     */
    @NotNull
    public static String[] tokenize(@NotNull Document document, @NotNull Language language) {
        assert document.getContent() != null;
        return Arrays.stream(
                        split(document.getTitle() + " " + document.getContent().getEntireTextContent()))
                .filter(text -> !text.isBlank())
                .map(token -> Utility.normalize(token, false, language))
                .filter(Objects::nonNull)
                .toArray(String[]::new);
    }

    /**
     * Tokenize a {@link Document} and return the {@link java.util.Map} having as key
     * a token and as corresponding value the sorted array of positions in the
     * {@link Document} at which the token in the key appears.
     * {@link String} (eventually with duplicates) obtained from the {@link Document}.
     *
     * @param document The {@link Document} to tokenize.
     * @param language The {@link Language} of the document.
     */
    @NotNull
    public static Map<String, int[]> tokenizeAndGetMapWithPositionsInDocument(
            @NotNull final Document document, @NotNull Language language) {
        String[] tokensEventuallyDuplicatesSortedByPositionInDocument = tokenize(document, language);
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

        String toReturn = removeInvalidCharsAndToLowerCase(token, fromQuery);

        // Stop-words handling
        if (shouldExcludeStopWords()) {
            if (isStopWord(toReturn, language)) {
                return null;
            }
        }

        // Stemming
        Stemmer stemmer = getStemmer();
        if (stemmer != null) {
            toReturn = stemmer.stem(toReturn, language);
        }

        return toReturn.isBlank() ? null : toReturn;
    }

    /**
     * @return the {@link Stemmer} to use or null, if the IR System is configured
     * to not performing stemming or if no {@link Stemmer} is available for the
     * name set in the IR System config file.
     */
    @Nullable
    private static Stemmer getStemmer() {
        try {
            String stemmerName = AppProperties.getInstance().get("app.stemmer");
            if (stemmerName == null || stemmerName.equals("null")) {
                return null;
            }
            return Stemmer.getStemmer(Stemmer.AvailableStemmer.valueOf(stemmerName.toUpperCase()));
        } catch (IOException e) {
            System.err.println("Error while reading app properties. Stemming will not be performed.");
            e.printStackTrace();
            return null;
        } catch (IllegalArgumentException e) {
            System.err.println("Stemmer not available while reading app properties. Stemming will not be performed.");
            e.printStackTrace();
            return null;
        }
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
     * @return true if the given word is a stop word for the given language.
     */
    private static boolean isStopWord(String word, @NotNull Language language) {
        return Arrays.asList(language.getStopWords()).contains(word);
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
     * Sort and remove duplicates from the given {@link List} in place.
     * The sorting and the deletion of duplicates are made according to the
     * first {@link List} (e.g., if the first list is like [1,1,3,5,2] and the second
     * is ['a','b','c','d','e'], then, after the execution of this method they
     * will be [1,2,3,5] and ['a','e','c','d'] respectively).
     *
     * @param lists the input {@link List}s. They <strong>must</strong> have the same size.
     * @throws UnsupportedOperationException If one of the given input {@link List} is
     *                                       a fixed size {@link List} (e.g., the inner class Arrays$ArrayList returned by
     *                                       {@link Arrays#asList(Object[])}. To solve this issue, it is suggested to create
     *                                       a new {@link List}, e.g.: <code>new ArrayList<>(Array.asList(1,2,3))</code>).
     */
    public static void sortAndRemoveDuplicates(@NotNull List<?>... lists) throws UnsupportedOperationException {  // TODO: test (with example from the Javadoc of this method) and benchmark
        if (lists.length == 0 || lists[0].isEmpty()) {
            return;
        }

        List<List<?>> transposedLists = Utility.transpose(Arrays.asList(lists));

        // sort according to the first list (respect the input)
        transposedLists.sort(Comparator.comparing(transposedList -> transposedList.get(0).toString()));

        // remove duplicates according to the first list (respect the input)
        Object previousElement = transposedLists.get(0).get(0);
        for (int i = 1; i < transposedLists.size(); ) {
            if (transposedLists.get(i).get(0).equals(previousElement)) {
                transposedLists.remove(i);
            } else {
                previousElement = transposedLists.get(i++).get(0);
            }
        }

        // re-transpose (to be as in input)
        var sortedLists = Utility.transpose(transposedLists);

        // set resulting lists to the input
        assert lists.length == sortedLists.size();
        IntStream.range(0, lists.length)
                .unordered().parallel()
                .forEach(i -> {
                    lists[i].clear();
                    lists[i].addAll((List) sortedLists.get(i));
                });
    }

    /**
     * Transpose the input {@link List} of {@link List}s (thought as a matrix).
     * All input {@link List}s must have the same size, otherwise the result is not defined
     * (the method will probably throw an {@link IndexOutOfBoundsException} // TODO: test behaviour).
     *
     * @param inputMatrix The input matrix.
     * @return the transposed matrix.
     */
    @NotNull
    public static List<List<?>> transpose(@NotNull final List<List<?>> inputMatrix) { // TODO: test
        assert inputMatrix.get(0) != null;
        return IntStream.range(0, inputMatrix.get(0).size())
                .mapToObj(colIndexInInputMtx ->
                        inputMatrix.stream().map(rowInInputMtx -> rowInInputMtx.get(colIndexInInputMtx))
                                .collect(toList()))
                .collect(Collectors.toList());
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
        return SkipList.intersection(a, b, comparator); // TODO: add support for intersection with comparator for varargs
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
        return SkipList.union(a, b, comparator); // TODO: add support for intersection with comparator for varargs
    }

    /**
     * Like {@link #unionOfSortedLists(List, List)}, but this method is
     * specific for {@link SkipList}s.
     *
     * @param <T>          Type of each element of the {@link  SkipList}s.
     * @param listsToUnion The lists for which the union will be computed.
     * @return the {@link SkipList} corresponding to the union of the given input lists.
     */
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
                .replaceAll("\"", "\\\"")
                .replaceAll("'", "\\'");

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
        final int CARTESIAN_PRODUCT_SIZE = lists.stream().mapToInt(List::size).reduce((a, b) -> a * b).orElse(0);
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
     * Generalization of {@link java.util.function.BiFunction} to take
     * three input arguments and produce one output argument.
     * From <a href="https://stackoverflow.com/a/19649473">Stackoverflow</a>.
     *
     * @param <A> Input type for argument 1.
     * @param <B> Input type for argument 2.
     * @param <C> Input type for argument 3.
     * @param <R> Output type.
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R> {  // TODO: needed?

        R apply(A a, B b, C c);

        default <V> TriFunction<A, B, C, V> andThen(
                Function<? super R, ? extends V> after) {
            return (A a, B b, C c) -> after.apply(apply(a, b, c));
        }

        default <D, V> TriFunction<A, B, C, V> andThen(
                BiFunction<? super R, D, ? extends V> after, D parameter) {
            return (A a, B b, C c) -> after.apply(apply(a, b, c), parameter);
        }
    }

    /**
     * Generalization of {@link java.util.function.BiFunction}.
     *
     * @param <A> Input type for argument 1.
     * @param <B> Input type for argument 2.
     * @param <R> Output type.
     */
    @FunctionalInterface
    public interface MyBiFunction<A, B, R> extends BiFunction<A, B, R> {  // TODO: needed?

        default <D, V> BiFunction<A, B, V> andThen(
                BiFunction<? super R, D, V> after, D parameter) {
            return (A a, B b) -> after.apply(apply(a, b), parameter);
        }
    }

}
