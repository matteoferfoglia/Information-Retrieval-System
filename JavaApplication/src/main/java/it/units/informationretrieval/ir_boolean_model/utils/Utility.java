package it.units.informationretrieval.ir_boolean_model.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
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
    public static List<String> tokenize(@NotNull Document document) {
        return Arrays.stream(
                        (Objects.requireNonNull(document).getTitle() + " " + Objects.requireNonNull(document.getContent()).getEntireTextContent())
                                .replaceAll(REGEX__NOT__VALID_CHARACTERS, " ")
                                .replaceAll(REGEX_MULTIPLE_SPACES, " ")
                                .split(" "))
                .map(Utility::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // TODO : not implemented yet (only split documents into strings which are the token - DO NOT CUT)
        // TODO : benchmark
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
        // TODO: test
        String toReturn = token
                .replaceAll(REGEX__NOT__VALID_CHARACTERS, " ")
                .replaceAll(REGEX_MULTIPLE_SPACES, " ") // TODO : refactoring : same code in the previous method
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
    public static String stackTraceToString(Exception e) {  // TODO: test
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Convert a string in JSON format to a {@link Map}.
     */
    @NotNull
    public static Map<String, ?> convertFromJsonToMap(@NotNull final String stringInJsonFormat) throws JsonProcessingException {    // TODO: benchmark
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
        return new ObjectMapper().writeValueAsString(Objects.requireNonNull(object));   // TODO: benchmark
    }

    @NotNull
    public static <T> List<T> sortAndRemoveDuplicates(@NotNull final List<T> postings) {   // TODO: benchmark
        return postings.stream().sorted().distinct().collect(Collectors.toList());
    }

    /**
     * @param <T> Type of each element of the {@link  List}s.
     * @param a   Sorted input list.
     * @param b   Sorted input list.
     * @return the {@link List} corresponding to the union of the given input lists.
     */
    @NotNull
    public static <T extends Comparable<T>> List<T> unionOfSortedLists(@NotNull List<T> a, @NotNull List<T> b) {    // TODO: benchmark
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
            @NotNull List<T> a, @NotNull List<T> b) {   // TODO: benchmark
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
