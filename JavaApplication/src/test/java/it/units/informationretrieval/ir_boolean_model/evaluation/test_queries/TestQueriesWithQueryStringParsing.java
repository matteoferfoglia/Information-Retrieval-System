package it.units.informationretrieval.ir_boolean_model.evaluation.test_queries;

import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies.Movie;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static it.units.informationretrieval.ir_boolean_model.evaluation.test_queries.TestQueries.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests the system on a set of test queries.
 * For these tests, the {@link Movie} corpus is used.
 * Queries are given as strings.
 * <p/>
 * <strong>Notice</strong>: the system was already opportunely tested
 * and this class is <strong>not</strong> intended for test purposes,
 * but most for demonstration use.
 * <p/>
 * The difference between this test class and {@link TestQueries} is
 * that in this class is showed how query can be constructed from
 * a string (query string parsing).
 */
class TestQueriesWithQueryStringParsing {

    /**
     * The {@link java.io.BufferedWriter} to print results to file.
     */
    private static Writer WRITER_TO_FILE;

    static void createOutputFileOfResults() {
        try {
            File directory = new File(FOLDER_NAME_TO_SAVE_RESULTS);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Output file not created.");
                }
            }
            var currentDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
            File f = new File(FOLDER_NAME_TO_SAVE_RESULTS + File.separator + currentDateTime + "_withQueryParsing" + ".txt");
            if (!f.createNewFile()) {
                throw new IOException("Output file not created.");
            }
            WRITER_TO_FILE = new BufferedWriter(new FileWriter(f));
        } catch (IOException e) {
            WRITER_TO_FILE = new BufferedWriter(new CharArrayWriter());
            e.printStackTrace();
        }
    }

    @BeforeAll
    static void noticeIfQueryResultsWillBePrinted() throws IOException {
        createOutputFileOfResults();
        TestQueries.noticeIfQueryResultsWillBePrinted(WRITER_TO_FILE);
    }

    @AfterAll
    static void printEndOfTests() throws IOException {
        TestQueries.printEndOfTests(WRITER_TO_FILE);
    }

    /**
     * Method to print and return query results.
     * See {@link TestQueries#evaluatePrintAndGetResultsOf(BooleanExpression, Writer)}.
     *
     * @param inputUnparsedQueryString The input (un-parsed) query string.
     * @return the {@link List} of results.
     */
    static List<Document> evaluatePrintAndGetResultsOf(String inputUnparsedQueryString) throws IOException {
        String out = "Input query string: " + inputUnparsedQueryString;
        WRITER_TO_FILE.write(out + "\n");
        WRITER_TO_FILE.flush();
        System.out.println(out);
        BooleanExpression be = irs.createNewBooleanExpression().parseQuery(inputUnparsedQueryString);
        return TestQueries.evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
    }

    @BeforeEach
    void printSpaceBeforeQuery() throws IOException {
        TestQueries.printNewLine(WRITER_TO_FILE);
    }

    @Test
    void illustrativeExample() throws IOException {
        String queryString = "space & jam";
//        List<Document> results = irs.retrieve(queryString);   // can be used to obtain results directly
        evaluatePrintAndGetResultsOf(queryString);
    }

    @Test
    void illustrativeExample2() throws IOException {
        String queryString = "space & !jam";
        evaluatePrintAndGetResultsOf(queryString);
    }

    @Test
    void singleWordQuery() throws IOException {
        assertTrue(evaluatePrintAndGetResultsOf(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .contains(doc1));
    }

    @Test
    void AND_query_containedInBoth() throws IOException {
        String queryString = wordsContainedBothInFirstAndSecondDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query2containedIn1stButNotIn2nd() throws IOException {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query_containedIn2ndButNotIn1st() throws IOException {
        String queryString = wordsContainedInSecondButNotInFirstDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query_containedNeitherIn1stNorIn2nd() throws IOException {
        String queryString = wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query2_containedNeitherIn1stNorIn2nd() throws IOException {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "&" + wordsContainedInSecondButNotInFirstDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void OR_query_containedInBoth() throws IOException {
        String queryString = wordsContainedBothInFirstAndSecondDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2containedIn1stButNotIn2nd() throws IOException {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedIn2ndButNotIn1st() throws IOException {
        String queryString = wordsContainedInSecondButNotInFirstDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedNeitherIn1stNorIn2nd() throws IOException {
        String queryString = wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2_containedNeitherIn1stNorIn2nd() throws IOException {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "|" + wordsContainedInSecondButNotInFirstDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query() throws IOException {
        String queryString = "!" + wordsContainedInFirstButNotInSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query2() throws IOException {
        String queryString = "!" + wordsContainedInSecondButNotInFirstDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void phraseQuery() throws IOException {
        String queryString = "\"Space jam\"";
        var results = evaluatePrintAndGetResultsOf(queryString);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery() throws IOException {
        String queryString = "Space *am";
        var results = evaluatePrintAndGetResultsOf(queryString);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery2() throws IOException {
        String queryString = "Sp*ce *am";
        var results = evaluatePrintAndGetResultsOf(queryString);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void spellingCorrection() throws IOException {
        String queryString = "Spade jam";
        var be = irs.createNewBooleanExpression()
                .parseQuery(queryString).spellingCorrection(false, true);
        assert 1 == be.getEditDistanceForSpellingCorrection();

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");

        String out = "Input query string: " + queryString;
        WRITER_TO_FILE.write(out + "\n");
        WRITER_TO_FILE.flush();
        System.out.println(out);
        var results = TestQueries.evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);

        assertTrue(results.contains(doc1));
    }

    @Test
    void phoneticCorrection() throws IOException {
        String queryString = "Space jem";
        var be = irs.createNewBooleanExpression()
                .parseQuery(queryString).spellingCorrection(true, true);

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");

        String out = "Input query string: " + queryString;
        WRITER_TO_FILE.write(out + "\n");
        WRITER_TO_FILE.flush();
        System.out.println(out);
        var results = TestQueries.evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);

        assertTrue(results.contains(doc1));
    }

}
