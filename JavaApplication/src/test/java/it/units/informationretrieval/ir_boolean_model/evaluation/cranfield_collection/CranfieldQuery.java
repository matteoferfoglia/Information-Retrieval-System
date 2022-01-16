package it.units.informationretrieval.ir_boolean_model.evaluation.cranfield_collection;

import it.units.informationretrieval.ir_boolean_model.document_descriptors.CranfieldDocument;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class representing a query for the Cranfield's collection.
 * Query are read from a file, so regexes are used to parse the
 * file content and extract the queries.
 *
 * @author Matteo Ferfoglia
 */
public class CranfieldQuery {

    /**
     * The resource folder where files about the Cranfield collection (used
     * to evaluate the system) are.
     */
    @NotNull
    private final static String PATH_TO_CRANFIELD_RESOURCE_FOLDER = "cranfield_collection/boolean_queries/";

    /**
     * The name of the file containing the queries.
     */
    @NotNull
    private final static String RELATIVE_PATH_TO_QUERIES = "cran.bool.qry";

    /**
     * The name of the file containing the association between a query (the
     * query number is provided) and the {@link CranfieldDocument} (the doc
     * number is provided) which is an answer (with a specified degree of
     * relevance) to the given query.
     */
    @NotNull
    private final static String RELATIVE_PATH_TO_DOCS_ANSWERING_QUERIES = "cranboolqrel";

    /**
     * The content saved as {@link String} of the file whose name is specified
     * in {@link #RELATIVE_PATH_TO_DOCS_ANSWERING_QUERIES}.
     */
    @NotNull
    private final static String DOCS_ANSWERING_QUERIES_ASSOCIATION;
    /**
     * The text with which each new query starts.
     */
    @NotNull
    private final static String TEXT_START_OF_QUERY = ".I ";
    /**
     * The regex to match an entire query.
     * The {@link #TEXT_START_OF_QUERY} is not part of this regex because
     * method {@link String#split(String)} is used on {@link #TEXT_START_OF_QUERY}
     * to split queries.
     * The {@link #queryNumber} can be captured from the first capturing group
     * and the query text from the second capturing group.
     */
    @NotNull
    private final static Pattern REGEX_ENTIRE_QUERY = Pattern.compile(
            "(?s)" +                      // multiline capturing (for ".*")
                    "(\\d+)\\s+" +        // matches the query number       in the 1st capturing group
                    "\\.W\\s+(.*)" +      // matches the text of a query    in the 2nd capturing group
                    "(\\s+|\\z)");        // goes on until either a space or the end of file
    //                                          (2nd capturing group should capture as many characters as possible)

    /**
     * The capturing group matching the query number according to {@link #REGEX_ENTIRE_QUERY}.
     */
    private final static int CAPTURING_GROUP_QUERY_NUMBER = 1;
    /**
     * The capturing group matching the query text according to {@link #REGEX_ENTIRE_QUERY}.
     */
    private final static int CAPTURING_GROUP_QUERY_TEXT = 2;

    /**
     * The Cranfield's collection saved as {@link Map} (the key is the document
     * number, the value is the entire {@link CranfieldDocument}.
     */
    @NotNull
    private static final Map<Integer, CranfieldDocument> CRANFIELD_DOCUMENTS_MAP;

    static {

        // Get documents
        Map<Integer, CranfieldDocument> cranfieldDocumentsMapTmp = new HashMap<>();
        try {
            cranfieldDocumentsMapTmp = CranfieldDocument.createCorpus().getCorpus().values()
                    .stream()
                    .map(doc -> (CranfieldDocument) doc)
                    .collect(Collectors.toMap(CranfieldDocument::getDocNumber, Function.identity()));
        } catch (URISyntaxException | IOException | NoMoreDocIdsAvailable e) {
            System.err.println("Error reading documents");
            e.printStackTrace();
        } finally {
            CRANFIELD_DOCUMENTS_MAP = cranfieldDocumentsMapTmp;
        }

        // Get answers to queries
        var docsAnsweringQueriesAssociationTmp = "";
        try {
            var pathToQueries = Path.of(Objects.requireNonNull(
                            CranfieldDocument.class.getResource(
                                    PATH_TO_CRANFIELD_RESOURCE_FOLDER + RELATIVE_PATH_TO_DOCS_ANSWERING_QUERIES))
                    .toURI());
            docsAnsweringQueriesAssociationTmp = Files.readString(pathToQueries); // not working with large files
        } catch (URISyntaxException | IOException | OutOfMemoryError e) {
            System.err.println("Error reading queries answers association.");
            e.printStackTrace();
        } finally {
            DOCS_ANSWERING_QUERIES_ASSOCIATION = docsAnsweringQueriesAssociationTmp;
        }
    }

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
     * Maps a {@link CranfieldDocument} (key), which must be present among the results
     * of this query, to its relevance (value)
     */
    @NotNull
    private final Map<CranfieldDocument, CranfieldRelevance> mapDocToRelevance = new HashMap<>();

    /**
     * Constructor.
     */
    public CranfieldQuery(int queryNumber, @NotNull String queryText) {
        this.queryNumber = queryNumber;
        this.queryText = Objects.requireNonNull(queryText);

        // associate the query with its answers
        final Pattern REGEX_ANSWERS_EXTRACTION = Pattern.compile(
                "(?m)"                // accept the anchors ^ and $ to match at the start and end of each line (like to use the flag Pattern.MULTILINE)
                        + "^"         // start of line
                        + queryNumber // extract data for this query (according to queryNumber)
                        + "\\s"       // exactly one space after the query number
                        + "(\\d+)\\s" // extract the docNumber matching the results                (1st capturing group)
                        + "(\\d+)");  // extract the degree of relevance for the doc to this query (2nd capturing group)
        final int CAPTURING_GROUP_FOR_DOC_NUMBER_ANSWERING_THE_QUERY = 1;
        final int CAPTURING_GROUP_FOR_RELEVANCE_DEGREE = 2;
        Matcher answersMatcher = REGEX_ANSWERS_EXTRACTION.matcher(DOCS_ANSWERING_QUERIES_ASSOCIATION);
        while (answersMatcher.find()) {
            int answerDocNumber = Integer.parseInt(
                    answersMatcher.group(CAPTURING_GROUP_FOR_DOC_NUMBER_ANSWERING_THE_QUERY));
            int relevanceOfAnswerDocToThisQuery = Integer.parseInt(
                    answersMatcher.group(CAPTURING_GROUP_FOR_RELEVANCE_DEGREE));
            var document = Objects.requireNonNull(
                    CRANFIELD_DOCUMENTS_MAP.get(answerDocNumber),
                    "No documents found for docNumber " + answerDocNumber);
            CranfieldRelevance relevance;
            try {
                relevance = CranfieldRelevance.getEnumValueFromNumericRelevance(relevanceOfAnswerDocToThisQuery);
            } catch (NoSuchElementException e) {
                System.err.println("No enum values for " + relevanceOfAnswerDocToThisQuery);
                relevance = null;
            }
            mapDocToRelevance.put(document, relevance);
        }
    }

    /**
     * Reads the file containing the queries.
     *
     * @return the {@link List} of query.
     */
    @NotNull
    public static List<CranfieldQuery> readQueries() throws URISyntaxException, IOException {

        var pathToQueries = Path.of(Objects.requireNonNull(
                        CranfieldDocument.class.getResource(PATH_TO_CRANFIELD_RESOURCE_FOLDER + RELATIVE_PATH_TO_QUERIES))
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
                        String exceptionErrorMessage = "Query not matching pattern." + System.lineSeparator()
                                + "\tPattern: " + REGEX_ENTIRE_QUERY.pattern() + System.lineSeparator()
                                + "\tText:    " + queryAsString
                                .replaceAll("(\\r)*\\n", System.lineSeparator() + "\t         ");   // replaceAll for prettier printing
                        System.err.println(exceptionErrorMessage);
                        throw new IllegalArgumentException(exceptionErrorMessage);
                    }
                })
                .toList();
        assert queries.size() > 0;
        return queries;
    }

    /**
     * @return a {@link Map} with the relevant documents for this query, having
     * documents as key and the relevance as corresponding value.
     */
    public Map<CranfieldDocument, CranfieldRelevance> getRelevantDocs() {
        return mapDocToRelevance;
    }

    /**
     * @return the text of this query.
     */
    @NotNull
    public String getQueryText() {
        return queryText;
    }

    @Override
    public String toString() {
        return "Query " + queryNumber + ": " + queryText;
    }
}
