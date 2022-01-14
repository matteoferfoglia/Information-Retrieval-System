package it.units.informationretrieval.ir_boolean_model.evaluation;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class evaluates the system.
 *
 * @author Matteo Ferfoglia
 */
public class Evaluation {


}

/**
 * Class representing a query for the Cranfield's collection.
 * Query are read from a file, so regexes are used to parse the
 * file content and extract the queries.
 */
class CranfieldQuery {

    /**
     * The resource folder where files about the Cranfield collection (used
     * to evaluate the system) are.
     */
    @NotNull
    private final static String PATH_TO_CRANFIELD_RESOURCE_FOLDER = "./../document_descriptors/cranfield_collection/";

    /**
     * The file containing queries.
     */
    @NotNull
    private final static String RELATIVE_PATH_TO_QUERIES = "cran.qry";

    /**
     * Expected total number of queries.
     */
    private final static int EXPECTED_TOTAL_NUM_OF_QUERIES = 225;

    /**
     * The text with which each new query starts.
     */
    @NotNull
    private final static String TEXT_START_OF_QUERY = ".I ";

    /**
     * The regex to match the start of a new query.
     */
    @NotNull
    private final static Pattern REGEX_START_OF_QUERY = Pattern.compile("^\\" + TEXT_START_OF_QUERY);

    /**
     * The regex to match an entire query.
     * The {@link #queryNumber} can be captured from the first capturing group
     * and the query text from the second capturing group.
     */
    @NotNull
    private final static Pattern REGEX_ENTIRE_QUERY = Pattern.compile(
            "(?s)" +                      // multiline capturing (for ".*")
                    "(\\d+)\\s+" +        // matches the query number       in the 1st capturing group
                    "\\.W\\s+(.*?)\\s+" + // matches the text of a query    in the 2nd capturing group
                    "((" + REGEX_START_OF_QUERY + ")|\\z)");// goes on until the start of a new query or the end of file

    /**
     * The capturing group matching the query number according to {@link #REGEX_ENTIRE_QUERY}.
     */
    @NotNull
    private final static int CAPTURING_GROUP_QUERY_NUMBER = 1;
    /**
     * The capturing group matching the query text according to {@link #REGEX_ENTIRE_QUERY}.
     */
    @NotNull
    private final static int CAPTURING_GROUP_QUERY_TEXT = 2;

    /**
     * The query number. It identifies the query.
     */
    private final int queryNumber;
    /**
     * The text of the query.
     */
    @NotNull
    private final String queryText;

    /**
     * Constructor.
     */
    public CranfieldQuery(int queryNumber, @NotNull String queryText) {
        this.queryNumber = queryNumber;
        this.queryText = Objects.requireNonNull(queryText);
    }

    /**
     * Reads the file containing the queries.
     *
     * @return the {@link List} of query.
     */
    @NotNull
    public static List<CranfieldQuery> readQueries() throws URISyntaxException, IOException {
        var pathToQueries = Path.of(Objects.requireNonNull(
                        Evaluation.class.getResource(PATH_TO_CRANFIELD_RESOURCE_FOLDER + RELATIVE_PATH_TO_QUERIES))
                .toURI());
        String allQueriesInAString = Files.readString(pathToQueries);
        List<CranfieldQuery> queries = Arrays.stream(allQueriesInAString.split(TEXT_START_OF_QUERY))
                .filter(s -> !s.isBlank())
                .map(queryAsString -> {
                    Matcher queryMatcher = REGEX_ENTIRE_QUERY.matcher(queryAsString);
                    if (queryMatcher.find()) {
                        return new CranfieldQuery(
                                Integer.parseInt(queryMatcher.group(CAPTURING_GROUP_QUERY_NUMBER)),
                                queryMatcher.group(CAPTURING_GROUP_QUERY_TEXT));
                    } else {
                        throw new IllegalArgumentException("Query not matching pattern." + System.lineSeparator()
                                + "\tPattern: " + REGEX_ENTIRE_QUERY.pattern() + System.lineSeparator()
                                + "\tText:    " + queryAsString
                                .replaceAll("(\\r)*\\n", System.lineSeparator() + "\t         "));    // replaceAll for prettier printing
                    }
                })
                .toList();
        assert queries.size() == EXPECTED_TOTAL_NUM_OF_QUERIES;
        return queries;
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        var queries = readQueries();
        System.out.println(queries.size() + " queries read:");
        System.out.println(readQueries());
    }

    @Override
    public String toString() {
        return "Query " + queryNumber + ": " + queryText;
    }
}