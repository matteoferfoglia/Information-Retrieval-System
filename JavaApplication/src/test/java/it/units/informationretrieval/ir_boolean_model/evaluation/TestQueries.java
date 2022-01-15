package it.units.informationretrieval.ir_boolean_model.evaluation;

import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static it.units.informationretrieval.ir_boolean_model.evaluation.TestQueriesWithQueryStringParsing.*;
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

    @BeforeAll
    static void noticeIfQueryResultsWillBePrinted() {
        if (PRINT_RESULTS) {
            System.out.println();
            System.out.println("====================================================================================");
            System.out.println("======                              TEST QUERIES                              ======");
            System.out.println("====================================================================================");
            System.out.println("Notice: At most " + MAX_NUM_OF_RESULTS_TO_PRINT
                    + " results will be printed for each query.");
        }
    }

    @AfterAll
    static void printEndOfTests() {
        if (PRINT_RESULTS) {
            System.out.println();
            System.out.println("====================================================================================");
            System.out.println("======                           TEST QUERIES - END                           ======");
            System.out.println("====================================================================================");
            System.out.println(System.lineSeparator());
        }
    }

    /**
     * Method to print and return query results, according
     * to the flag {@link #PRINT_RESULTS}.
     *
     * @param be The {@link BooleanExpression} to print.
     * @return the {@link List} of results.
     */
    static List<Document> evaluatePrintAndGetResultsOf(BooleanExpression be) {
        long start, end;
        start = System.nanoTime();
        var results = be.evaluate();
        end = System.nanoTime();
        if (PRINT_RESULTS) {
            System.out.println(results.size() + " result" + (results.size() != 1 ? "s" : "")
                    + " for query " + be.getQueryString()
                    + " found in " + ((end - start) / 1e6) + " ms :"
                    + System.lineSeparator()
                    + results.stream().limit(MAX_NUM_OF_RESULTS_TO_PRINT)
                    .map(aResult -> "     - " + aResult)
                    .map(aResult -> // cut the string (avoid printing too much)
                            aResult.substring(0, Math.min(aResult.length(), MAX_NUM_OF_CHARS_TO_PRINT))
                                    + (aResult.length() > MAX_NUM_OF_CHARS_TO_PRINT ? "..." : ""))
                    .collect(Collectors.joining(System.lineSeparator()))
                    + System.lineSeparator());
        }
        return results;
    }

    static void printSpaceBeforeQuery_() {
        if (PRINT_RESULTS) {
            System.out.println();
        }
    }

    @BeforeEach
    void printSpaceBeforeQuery() {
        printSpaceBeforeQuery_();
    }

    @Test
    void illustrativeExample() {
        var be = irs.createNewBooleanExpression().setMatchingValue("space").and("jam");
        evaluatePrintAndGetResultsOf(be);
    }

    @Test
    void illustrativeExample2() {
        // space AND (NOT jam)
        BooleanExpression be = irs.createNewBooleanExpression().setMatchingValue("space")
                .and(irs.createNewBooleanExpression().setMatchingValue("jam").not());
        evaluatePrintAndGetResultsOf(be);
    }

    @Test
    void singleWordQuery() {
        BooleanExpression be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get());
        assertTrue(evaluatePrintAndGetResultsOf(be).contains(doc1));
    }

    @Test
    void AND_query_containedInBoth() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query2containedIn1stButNotIn2nd() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query_containedIn2ndButNotIn1st() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query_containedNeitherIn1stNorIn2nd() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query2_containedNeitherIn1stNorIn2nd() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedInSecondButNotInFirstDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void OR_query_containedInBoth() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2containedIn1stButNotIn2nd() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedIn2ndButNotIn1st() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedNeitherIn1stNorIn2nd() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2_containedNeitherIn1stNorIn2nd() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedInSecondButNotInFirstDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .not();
        var results = evaluatePrintAndGetResultsOf(be);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query2() {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .not();
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void phraseQuery() {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase(new String[]{"Space", "jam"});
        var results = evaluatePrintAndGetResultsOf(be);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery() {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Space *am".split(" "));
        var results = evaluatePrintAndGetResultsOf(be);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery2() {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Sp*ce *am".split(" "));
        var results = evaluatePrintAndGetResultsOf(be);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void spellingCorrection() {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Spade jam".split(" "))
                .spellingCorrection(false, true);
        assert 1 == be.getEditDistanceForSpellingCorrection();

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
    }

    @Test
    void phoneticCorrection() {
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
        var results = evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
    }

}
