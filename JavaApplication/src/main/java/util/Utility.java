package util;

import components.Document;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/** Utility class.
 * @author Matteo Ferfoglia */
public class Utility {

    /** Tokenize a {@link components.Document} and return the {@link java.util.List} of tokens as
     * {@link String} (eventually with duplicates) obtained from the {@link components.Document}.*/
    @NotNull
    public static List<String> tokenize(@NotNull Document document) {
        return Arrays.stream(document.getContent().split(" ")).collect(Collectors.toList());    // TODO : not implemented yet
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

    /** Create the working directory for the application.
     * @throws IOException If I/O exceptions are thrown. */
    public static void createWorkingDirectory(@NotNull String workingDirectoryName) throws IOException {
        File file_workingDirectory = new File(
                Objects.requireNonNull(
                    workingDirectoryName,
                    "The name of the working directory to create cannot be null"
                )
        );
        if( !file_workingDirectory.isDirectory() ) {
            if( ( file_workingDirectory.exists() && !file_workingDirectory.delete() ) || !file_workingDirectory.mkdir() ) {
                // If the folder exists but cannot be deleted or if it cannot be created
                throw new IOException("Unable to create the directory " + file_workingDirectory.getAbsolutePath());
            } else {
                System.out.println("Working directory \"" + file_workingDirectory.getAbsolutePath() + "\" created.");
            }
        }
    }

}
