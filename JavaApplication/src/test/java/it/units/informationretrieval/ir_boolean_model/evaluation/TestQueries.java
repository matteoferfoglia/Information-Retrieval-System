package it.units.informationretrieval.ir_boolean_model.evaluation;

import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import org.junit.jupiter.api.Disabled;
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
@Disabled   // illustrative tests
class TestQueries {

    @Test
    void illustrativeExample() {
        // space AND jam
        List<Document> results = irs.createNewBooleanExpression()
                .setMatchingValue("space").and("jam").evaluate();
        int maxNumOfResultsToShow = 5;
        System.out.println(results.stream().limit(maxNumOfResultsToShow).toList());
    }

    @Test
    void illustrativeExample2() {
        // space AND (NOT jam)
        BooleanExpression be = irs.createNewBooleanExpression().setMatchingValue("space")
                .and(irs.createNewBooleanExpression().setMatchingValue("jam").not());
        List<Document> results = be.evaluate();
        int maxNumOfResultsToShow = 5;
        System.out.println("Results for query " + be.getQueryString() + ":"
                + System.lineSeparator()
                + results.stream().limit(maxNumOfResultsToShow)
                .map(aResult -> "\t - " + aResult).collect(Collectors.joining(System.lineSeparator())));
    }

    @Test
    void singleWordQuery() {
        assertTrue(irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .evaluate().contains(doc1));
    }

    @Test
    void AND_query_containedInBoth() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query2containedIn1stButNotIn2nd() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query_containedIn2ndButNotIn1st() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query_containedNeitherIn1stNorIn2nd() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query2_containedNeitherIn1stNorIn2nd() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .evaluate();
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void OR_query_containedInBoth() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2containedIn1stButNotIn2nd() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedIn2ndButNotIn1st() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedNeitherIn1stNorIn2nd() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .evaluate();
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2_containedNeitherIn1stNorIn2nd() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .evaluate();
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .not()
                .evaluate();
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query2() {
        var results = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .not()
                .evaluate();
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void phraseQuery() {
        var results = irs.createNewBooleanExpression()
                .setMatchingPhrase(new String[]{"Space", "jam"})
                .evaluate();
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery() {
        var results = irs.createNewBooleanExpression()
                .setMatchingPhrase("Space *am".split(" "))
                .evaluate();
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery2() {
        var results = irs.createNewBooleanExpression()
                .setMatchingPhrase("Sp*ce *am".split(" "))
                .evaluate();
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
        var results = be.evaluate();
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
        var results = be.evaluate();
        assertTrue(results.contains(doc1));
    }

}
