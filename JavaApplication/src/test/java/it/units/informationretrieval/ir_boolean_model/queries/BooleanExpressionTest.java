package it.units.informationretrieval.ir_boolean_model.queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BooleanExpressionTest {

    private static final String PATH_TO_FILE_WITH_QUERY_SAMPLES = "/singleWordQuery.csv";
    private static final String LIST_ELEMENTS_SEPARATOR_IN_CSV = "#";
    private static final int NUM_LINES_TO_SKIP_IN_CSV = 2;
    private static BooleanExpression booleanExpression;
    private static InformationRetrievalSystem irs;
    private static final Corpus corpus;

    static {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));    // ignore std out in tests
        try {
            irs = new InformationRetrievalSystem(InvertedIndexTest.getLoadedSampleCorpus());
        } catch (URISyntaxException | IOException e) {
            fail(e);
        }
        corpus = irs.getInvertedIndex().getCorpus();
        System.setOut(realStdOut);
    }

    private List<String> evaluateQueryAndGetResultingDocIdsAsStringList() {
        List<Document> queryResults = booleanExpression.evaluate();
        return corpus.getCorpus().entrySet()
                .stream()
                .filter(corpusEntry -> queryResults.contains(corpusEntry.getValue()))
                .map(Map.Entry::getKey)
                .map(String::valueOf)
                .sorted()
                .toList();
    }

    private List<String> splitDocIdIntoList(String postingListReferringTheTerm) {
        return postingListReferringTheTerm == null ?
                new ArrayList<>() :
                Arrays.stream(postingListReferringTheTerm.split(LIST_ELEMENTS_SEPARATOR_IN_CSV)).toList();
    }

    private void assertQueryResultsAreCorrect(String expectedResultingPostingList) {
        assertEquals(
                splitDocIdIntoList(expectedResultingPostingList),
                evaluateQueryAndGetResultingDocIdsAsStringList());
    }

    @BeforeEach
    void initializeBooleanExpression() {
        booleanExpression = irs.createNewBooleanExpression();
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateOneWordQuery(String word, String ignored, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryNot(String word, String ignored, String ignored2, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryAnd(String word, String word2, String ignored, String ignored2, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).and(word2);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryOr(String word, String word2, String ignored, String ignored2, String ignored3, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).or(word2);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryAndThenNot(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).and(word2).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryOrThenNot(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5,
                                String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).or(word2).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryNotThenNot(String word, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7,
                                 String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).not().not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryNotThenAndThenAndThenNot(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7,
                                               String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).not().and(word2).and(word).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryNotThenOr(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7, String ignored8,
                                String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).not().or(word);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryNotThenOrThenOr(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7, String ignored8, String ignored9,
                                      String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).not().or(word).or(word2);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryAndThenOr(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7, String ignored8, String ignored9, String ignored10,
                                String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word).and(word2).or(word);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @Test
    void throwIfNotAggregatedButNeitherValueNorPhraseToMatchIsSet() {
        try {
            booleanExpression.and("");
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingValueInAnAggregatedQuery() {
        try {
            irs.createNewBooleanExpression()
                    .and("")
                    .setMatchingValue("impossible to set this");
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingPhraseInAnAggregatedQuery() {
        try {
            irs.createNewBooleanExpression()
                    .and("")
                    .setMatchingPhrase(Arrays.asList("impossible to set this".split(" ")));
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingValueWhenMatchingValueIsAlreadySet() {
        try {
            irs.createNewBooleanExpression()
                    .setMatchingValue("foo")
                    .setMatchingValue("impossible to set this");
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingValueWhenMatchingPhraseIsAlreadySet() {
        try {
            irs.createNewBooleanExpression()
                    .setMatchingPhrase(Arrays.asList("foo bar".split(" ")))
                    .setMatchingValue("impossible to set this");
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingPhraseWhenMatchingValueIsAlreadySet() {
        try {
            irs.createNewBooleanExpression()
                    .setMatchingValue("foo")
                    .setMatchingPhrase(Arrays.asList("impossible to set this".split(" ")));
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingPhraseWhenMatchingPhraseIsAlreadySet() {
        try {
            irs.createNewBooleanExpression()
                    .setMatchingPhrase(Arrays.asList("foo bar".split(" ")))
                    .setMatchingPhrase(Arrays.asList("impossible to set this".split(" ")));
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }


//
//    @Test
//    void evaluatePhraseQuery() {// TODO: evaluate phrase queries
//    }

}