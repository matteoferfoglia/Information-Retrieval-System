package util;

import components.Document;
import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/** Utility class.
 * @author Matteo Ferfoglia */
public class Utility {

    /** Tokenize a {@link components.Document} and return the {@link java.util.List} of tokens as
     * {@link String} (eventually with duplicates) obtained from the {@link components.Document}.*/
    @NotNull
    public static List<String> tokenize(@NotNull Document document) {
        // TODO : not implemented yet
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Print an exception stacktrace to a string and return the string.
     * @param e An exception.
     * @return The stacktrace obtained from {@link Exception#printStackTrace()} as a {@link String}. */
    @NotNull
    public static String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}