package util;

import java.io.PrintWriter;
import java.io.StringWriter;

/** Utility class.
 * @author Matteo Ferfoglia */
public class Utils {

    /**
     * @param e An exception.
     * @return The stacktrace obtained from {@link Exception#printStackTrace()} as a {@link String}. */
    public static String stackTraceToString(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

}
