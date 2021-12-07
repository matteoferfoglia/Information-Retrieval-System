package it.units.informationretrieval.ir_boolean_model.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
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
        return Arrays.stream(Objects.requireNonNull(Objects.requireNonNull(document).getContent())
                        .getEntireTextContent()
                        .replaceAll(REGEX__NOT__VALID_CHARACTERS, " ")
                        .replaceAll(REGEX_MULTIPLE_SPACES, " ").split(" "))
                .map(Utility::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // TODO : not implemented yet (only split documents into strings which are the token - DO NOT CUT)
        // TODO : test
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
    public static Map<String, ?> convertFromJsonToMap(@NotNull final String stringInJsonFormat) throws JsonProcessingException {    // TODO: test
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
        return new ObjectMapper().writeValueAsString(Objects.requireNonNull(object));   // TODO: test
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
