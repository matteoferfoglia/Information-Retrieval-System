package it.units.informationretrieval.ir_boolean_model.evaluation.test_queries;

import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies.Movie;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import static it.units.informationretrieval.ir_boolean_model.evaluation.test_queries.TestQueriesWithQueryStringParsing.*;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests the system on a set of test queries.
 * For these tests, the {@link Movie} corpus is used.
 * <p/>
 * <strong>Notice</strong>: the system was already opportunely tested
 * and this class is <strong>not</strong> intended for test purposes,
 * but most for demonstration use.
 */
class TestQueries {
    // TODO : refactor this class according to the other class in this inner-most package

    /**
     * Flag: if set to true, results will be printed to {@link System#out}.
     */
    final static boolean PRINT_RESULTS = true;
    /**
     * Max number of results to print.
     */
    private final static int MAX_NUM_OF_RESULTS_TO_PRINT = 10;
    /**
     * Max number of characters to print for each result (to avoid printing too many things).
     */
    private final static int MAX_NUM_OF_CHARS_TO_PRINT = 84 - 3 - 7;   // nothing special, only for prettier output format
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
            File f = new File(FOLDER_NAME_TO_SAVE_RESULTS + File.separator + currentDateTime + ".txt");
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
        noticeIfQueryResultsWillBePrinted(WRITER_TO_FILE);
    }

    static void noticeIfQueryResultsWillBePrinted(@NotNull Writer outputWriter) throws IOException {
        String out = """
                                
                ====================================================================================
                ======                              TEST QUERIES                              ======
                ====================================================================================
                                
                """
                + "Notice: At most " + MAX_NUM_OF_RESULTS_TO_PRINT + " results will be printed for each query."
                + "Notice: if assertions are enabled, timing values are unreliable.\n";
        outputWriter.write(out);
        outputWriter.flush();
        if (PRINT_RESULTS) {
            System.out.println(out);
        }
    }

    @AfterAll
    static void printEndOfTests() throws IOException {
        printEndOfTests(WRITER_TO_FILE);
    }

    static void printEndOfTests(@NotNull Writer writer) throws IOException {
        String out = """

                ====================================================================================
                ======                           TEST QUERIES - END                           ======
                ====================================================================================
                                
                """;
        writer.write(out);
        writer.flush();
        if (PRINT_RESULTS) {
            System.out.println(out);
        }
    }

    /**
     * Method to print and return query results, according
     * to the flag {@link #PRINT_RESULTS}.
     *
     * @param be The {@link BooleanExpression} to print.
     * @return the {@link List} of results.
     */
    static List<Document> evaluatePrintAndGetResultsOf(BooleanExpression be, Writer whereToWrite) throws IOException {
        long start, end;
        start = System.nanoTime();
        var results = be.evaluate();
        end = System.nanoTime();
        String out = results.size() + " result" + (results.size() != 1 ? "s" : "")
                + " for query " + be.getQueryString()
                + " found in " + ((end - start) / 1e6) + " ms :"
                + System.lineSeparator()
                + results.stream().limit(MAX_NUM_OF_RESULTS_TO_PRINT)
                .map(aResult -> "     - " + aResult)
                .map(aResult -> // cut the string (avoid printing too much)
                        aResult.substring(0, Math.min(aResult.length(), MAX_NUM_OF_CHARS_TO_PRINT))
                                + (aResult.length() > MAX_NUM_OF_CHARS_TO_PRINT ? "..." : ""))
                .collect(Collectors.joining(System.lineSeparator()))
                + (results.size() > 0 ? "\n" : "");
        whereToWrite.write(out + "\n");
        whereToWrite.flush();
        if (PRINT_RESULTS) {
            System.out.println(out);
        }
        return results;
    }

    static void printSpaceBeforeQuery_(Writer writerToFile) throws IOException {
        writerToFile.write("\n");
        writerToFile.flush();
        if (PRINT_RESULTS) {
            System.out.println();
        }
    }

    @BeforeEach
    void printSpaceBeforeQuery() throws IOException {
        printSpaceBeforeQuery_(WRITER_TO_FILE);
    }

    @Test
    void illustrativeExample() throws IOException {
        var be = irs.createNewBooleanExpression().setMatchingValue("space").and("jam");
        evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
    }

    @Test
    void illustrativeExample2() throws IOException {
        // space AND (NOT jam)
        BooleanExpression be = irs.createNewBooleanExpression().setMatchingValue("space")
                .and(irs.createNewBooleanExpression().setMatchingValue("jam").not());
        evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
    }

    @Test
    void singleWordQuery() throws IOException {
        BooleanExpression be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get());
        assertTrue(evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE).contains(doc1));
    }

    @Test
    void AND_query_containedInBoth() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query2containedIn1stButNotIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query_containedIn2ndButNotIn1st() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query2_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedInSecondButNotInFirstDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void OR_query_containedInBoth() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2containedIn1stButNotIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedIn2ndButNotIn1st() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedInSecondButNotInFirstDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .not();
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query2() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .not();
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void phraseQuery() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase(new String[]{"Space", "jam"});
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Space *am".split(" "));
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery2() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Sp*ce *am".split(" "));
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void spellingCorrection() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Spade jam".split(" "))
                .spellingCorrection(false, true);
        assert 1 == be.getEditDistanceForSpellingCorrection();

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
    }

    @Test
    void phoneticCorrection() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Space jem".split(" "))
                .spellingCorrection(
                        true,
                        true/*dangerous to set to false: will search for ALL possible corrections, risk of StackOverflowError*/
                );

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
    }

}
