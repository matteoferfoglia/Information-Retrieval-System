package it.units.informationretrieval.ir_boolean_model.evaluation;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.CranfieldDocument;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Enumeration of possible value of relevance assigned to documents
 * from the Cranfield's collection.
 * Each enum value has its own value (used for classification in
 * input documents) and the corresponding description for the value.
 */
enum CranfieldRelevance {

    // enum values sorted by relevance
    COMPLETE_ANSWER(1, "References which are a complete answer to the question."),
    HIGH_RELEVANCE(2, "References of a high degree of relevance, the lack of which"
            + " either would have made the research impracticable or would"
            + "  have resulted in a considerable amount of extra work."),
    USEFUL(3, "References which were useful, either as general background"
            + " to the work or as suggesting methods of tackling certain aspects"
            + " of the work."),
    LOW_INTEREST(4, "References of minimum interest, for example, those that have been"
            + " included from an historical viewpoint."),
    NO_INTEREST(5, "References of no interest.");


    /**
     * Minimum value for {@link #relevance}.
     */
    private final static int MIN_RELEVANCE_VALUE = 1;
    /**
     * Maximum value for {@link #relevance}.
     */
    private final static int MAX_RELEVANCE_VALUE = 5;
    /**
     * Relevance numeric value.
     */
    @Range(from = MIN_RELEVANCE_VALUE, to = MAX_RELEVANCE_VALUE)
    private final int relevance;
    /**
     * Description for the enum value.
     */
    @NotNull
    private final String description;

    /**
     * Constructor.
     */
    CranfieldRelevance(int relevance, @NotNull String description) {
        this.relevance = relevance;
        this.description = Objects.requireNonNull(description);
        assert MIN_RELEVANCE_VALUE <= relevance && relevance <= MAX_RELEVANCE_VALUE;
    }

    /**
     * @param relevance The relevance, provided as numeric value.
     * @return the enum instance for the provided relevance value.
     * @throws NoSuchElementException if the input parameter is not a valid
     *                                value for {@link CranfieldRelevance}.
     */
    @NotNull
    public static CranfieldRelevance getEnumValueFromNumericRelevance(int relevance)
            throws NoSuchElementException {
        return Arrays.stream(values())
                .filter(enumVal -> enumVal.relevance == relevance)
                .findAny()
                .orElseThrow();
    }
}

/**
 * This class evaluates the system.
 *
 * @author Matteo Ferfoglia
 */
public class Evaluation {

    /**
     * The {@link InformationRetrievalSystem} for the Cranfield's collection.
     */
    @NotNull
    private static final InformationRetrievalSystem CRANFIELD_IRS;

    /**
     * Queries for the Cranfield's collection.
     */
    @NotNull
    private static final List<CranfieldQuery> CRANFIELD_QUERIES;

    static {
        InformationRetrievalSystem irsTmp = null;
        List<CranfieldQuery> queriesTmp = null;
        try {
            PrintStream realStdOut = System.out;
            System.setOut(new PrintStream(new ByteArrayOutputStream()));  // avoid to print useless output during tests
            irsTmp = new InformationRetrievalSystem(CranfieldDocument.createCorpus());
            System.setOut(realStdOut);
            queriesTmp = CranfieldQuery.readQueries();
        } catch (URISyntaxException | IOException | NoMoreDocIdsAvailable e) {
            fail(e);
        } finally {
            assert irsTmp != null; // test should fail if null (errors while creating it)
            assert queriesTmp != null;
            CRANFIELD_IRS = irsTmp;
            CRANFIELD_QUERIES = queriesTmp;
        }
    }

    /**
     * @param query A {@link CranfieldQuery} instance.
     * @return the query string obtained from the input instance
     * but keeping only terms which are in the IR system dictionary
     * and whose wf-idf value is higher than a specified threshold
     * (that depends on the implementation).
     */
    private static String parseQueryToKeepOnlyTermsKnownByTheIRSystemOverWfIdfThreshold(@NotNull CranfieldQuery query) {
        final double DF_THRESHOLD = 0.3 * CRANFIELD_IRS.size();  // term must compare at least in 10% of docs   // TODO: better to use stemming and avoid stop words
        var termsToKeep = CRANFIELD_IRS.getDictionary(DF_THRESHOLD);    // TODO: use .getListOfPostings(..) instead
        return Arrays.stream(Utility.split(query.getQueryText()))
                .map(tokenFromQuery -> Utility.normalize(
                        tokenFromQuery,
                        false, // parse like if it was a term of the IR System dictionary
                        CranfieldDocument.LANGUAGE))
                .filter(Objects::nonNull)
                .filter(termsToKeep::contains)
                .collect(Collectors.joining(" "));
    }

    @Test
    void precision() {
        var precisions = CRANFIELD_QUERIES.stream()
                .map(query -> {
                    String queryString = parseQueryToKeepOnlyTermsKnownByTheIRSystemOverWfIdfThreshold(query);
                    Set<Document> retrievedDocuments = new HashSet<>(CRANFIELD_IRS.retrieve(queryString));
                    Set<Document> relevantDocuments = query.getRelevantDocs().keySet()
                            .stream().map(doc -> (Document) doc).collect(Collectors.toSet());
                    Set<Document> relevantAndRetrieved = relevantDocuments.stream()
                            .filter(retrievedDocuments::contains).collect(Collectors.toSet());
                    return (double) relevantAndRetrieved.size() / retrievedDocuments.size();

//                    // TODO: sorting for ranking retrieval
//                    List<Document> sortedRetrievedDocuments = CRANFIELD_IRS.retrieve(queryString.replaceAll(" ", "|") /*OR query*/);
//                    List<Document> sortedRelevantDocuments =
//                            query.getRelevantDocs().entrySet()
//                                    .stream()
//                                    .sorted(Map.Entry.comparingByValue())
//                                    .map(Map.Entry::getKey)
//                                    .map(doc -> (Document) doc)
//                                    .toList();
//                    List<Document> sortedRelevantAndRetrieved =
//                            Utility.intersectionOfSortedLists(sortedRelevantDocuments, sortedRetrievedDocuments);
//                    return (double) sortedRelevantAndRetrieved.size() / sortedRetrievedDocuments.size();
                })
                .toList();

        System.out.println("Average precision: " + precisions.stream().mapToDouble(d -> d).average().orElseThrow());
        System.out.println("Best precision:    " + precisions.stream().mapToDouble(d -> d).max().orElseThrow());
        System.out.println("Worst precision:   " + precisions.stream().mapToDouble(d -> d).min().orElseThrow());

    }

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
    private final static String PATH_TO_CRANFIELD_RESOURCE_FOLDER = "cranfield_collection/";

    /**
     * The name of the file containing the queries.
     */
    @NotNull
    private final static String RELATIVE_PATH_TO_QUERIES = "cran.qry";

    /**
     * The name of the file containing the association between a query (the
     * query number is provided) and the {@link CranfieldDocument} (the doc
     * number is provided) which is an answer (with a specified degree of
     * relevance) to the given query.
     */
    @NotNull
    private final static String RELATIVE_PATH_TO_DOCS_ANSWERING_QUERIES = "cranqrel";

    /**
     * The content saved as {@link String} of the file whose name is specified
     * in {@link #RELATIVE_PATH_TO_DOCS_ANSWERING_QUERIES}.
     */
    @NotNull
    private final static String DOCS_ANSWERING_QUERIES_ASSOCIATION;
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
            docsAnsweringQueriesAssociationTmp = Files.readString(pathToQueries);
        } catch (URISyntaxException | IOException e) {
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