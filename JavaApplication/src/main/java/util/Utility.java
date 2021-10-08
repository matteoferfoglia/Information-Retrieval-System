package util;

import components.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class.
 *
 * @author Matteo Ferfoglia
 */
public class Utility {

    /**
     * Tokenize a {@link components.Document} and return the {@link java.util.List} of tokens as
     * {@link String} (eventually with duplicates) obtained from the {@link components.Document}.
     */
    @NotNull
    public static List<String> tokenize(@NotNull Document document) {
        return Arrays.stream(document.getContent().split(" "))
                .map(Utility::normalize)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // TODO : not implemented yet (only split documents into strings which are the token - DO NOT CUT)
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
        String toReturn = token.replaceAll("[^a-zA-Z0-9]", "").trim();
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

}
