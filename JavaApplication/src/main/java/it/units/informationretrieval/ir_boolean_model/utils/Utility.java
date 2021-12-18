package it.units.informationretrieval.ir_boolean_model.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.utils.skiplist.SkipList;
import it.units.informationretrieval.ir_boolean_model.utils.skiplist.SkipListElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
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

    private static final String REGEX__NOT__VALID_CHARACTERS = "[^a-zA-Z0-9 ]";
    private static final String REGEX_MULTIPLE_SPACES = " +";

    /**
     * Tokenize a {@link Document} and return the {@link java.util.List} of tokens as
     * {@link String} (eventually with duplicates) obtained from the {@link Document}.
     */
    @NotNull
    public static String[] tokenize(@NotNull Document document) {
        return Arrays.stream(
                        (Objects.requireNonNull(document).getTitle() + " " + Objects.requireNonNull(document.getContent()).getEntireTextContent())
                                .split(" "))
                .filter(text -> !text.isBlank())
                .map(Utility::normalize)
                .filter(Objects::nonNull)
                .toArray(String[]::new);
        // TODO : not implemented yet, just a draft (only split documents into strings which are the token - DO NOT CUT)
    }

    /**
     * Tokenize a {@link Document} and return the {@link java.util.Map} having as key
     * a token and as corresponding value the sorted array of positions in the
     * {@link Document} at which the token in the key appears.
     * {@link String} (eventually with duplicates) obtained from the {@link Document}.
     */
    @NotNull
    public static Map<String, int[]> tokenizeAndGetMapWithPositionsInDocument(@NotNull final Document document) {   // TODO: test and benchmark
        String[] tokensEventuallyDuplicatesSortedByPositionInDocument = tokenize(document);
        return IntStream
                .range(0, tokensEventuallyDuplicatesSortedByPositionInDocument.length)
                .unordered().parallel()
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(
                        tokensEventuallyDuplicatesSortedByPositionInDocument[i], i))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, toSet())))
                .entrySet()
                .stream().unordered().parallel()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().sorted().mapToInt(i -> i).toArray()));
    }

    /**
     * Normalize a token ({@link String}).
     *
     * @param token The {@link String token} to be normalized.
     * @return the corresponding normalized token or null if the normalization
     * brings to an empty string.
     */
    @Nullable
    public static String normalize(@NotNull String token) {
        // TODO : not implemented yet, just a draft
        String toReturn = token
                .replaceAll(REGEX__NOT__VALID_CHARACTERS, " ")
                .replaceAll(REGEX_MULTIPLE_SPACES, " ")
                .toLowerCase(Locale.ROOT)
                .trim();
        return toReturn.isEmpty() ? null : toReturn;
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
        if (Objects.requireNonNull(lists).length == 0 || lists[0].isEmpty()) {
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
        return IntStream.range(0, Objects.requireNonNull(Objects.requireNonNull(inputMatrix).get(0)).size())
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
        ArrayList<T> union = new ArrayList<>(Objects.requireNonNull(a).size() + Objects.requireNonNull(b).size());
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
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

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
     * @param <T> Type of each element of the {@link  SkipList}s.
     * @param a   Sorted input list.
     * @param b   Sorted input list.
     * @return the {@link SkipList} corresponding to the intersection of the given input lists.
     */
    @NotNull
    public static <T extends Comparable<T>> List<SkipListElement<T>> intersectionOfSortedSkipLists(
            @NotNull SkipList<T> a, @NotNull SkipList<T> b) {
        Objects.requireNonNull(a);
        Objects.requireNonNull(b);

        ArrayList<SkipListElement<T>> intersection = new ArrayList<>(a.size());
        int i = 0, j = 0, comparison;
        while (i < a.size() && j < b.size()) {
            comparison = a.get(i).getElement().compareTo(b.get(j).getElement());
            if (comparison == 0) {
                intersection.add(a.get(i++));
                j++;
            } else if (comparison < 0) {
                SkipListElement<T> forwardedElement = a.get(i).getForwardedElement();
                if (forwardedElement != null && forwardedElement.getElement().compareTo(b.get(j).getElement()) < 0) {
                    i = a.get(i).getForwardedIndex();
                } else {
                    i++;
                }
            } else {
                SkipListElement<T> forwardedElement = b.get(j).getForwardedElement();
                if (forwardedElement != null && forwardedElement.getElement().compareTo(a.get(i).getElement()) < 0) {
                    j = b.get(j).getForwardedIndex();
                } else {
                    j++;
                }
            }
        }
        intersection.trimToSize();

        return intersection;
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
            Objects.requireNonNull(after);
            return (A a, B b, C c) -> after.apply(apply(a, b, c));
        }

        default <D, V> TriFunction<A, B, C, V> andThen(
                BiFunction<? super R, D, ? extends V> after, D parameter) {
            Objects.requireNonNull(after);
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
            Objects.requireNonNull(after);
            return (A a, B b) -> after.apply(apply(a, b), parameter);
        }
    }

}
