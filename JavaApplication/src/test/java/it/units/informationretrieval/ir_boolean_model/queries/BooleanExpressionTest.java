package it.units.informationretrieval.ir_boolean_model.queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest;
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

class BooleanExpressionTest {   // TODO: add missing tests

    private static final String PATH_TO_FILE_WITH_QUERY_SAMPLES = "/queriesForSampleCorpus/singleWordQuery.csv";
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

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateOneWordQuery(String word, String ignored, String expectedResultingPostingList) {
        booleanExpression = irs.createNewBooleanExpression().setMatchingValue(word);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryNot(String word, String ignored, String ignored2, String expectedResultingPostingList) {
        booleanExpression = irs.createNewBooleanExpression().setMatchingValue(word).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryAnd(String word, String word2, String ignored, String ignored2, String expectedResultingPostingList) {
        booleanExpression = irs.createNewBooleanExpression().setMatchingValue(word).and(word2);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryOr(String word, String word2, String ignored, String ignored2, String ignored3, String expectedResultingPostingList) {
        booleanExpression = irs.createNewBooleanExpression().setMatchingValue(word).or(word2);
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryAndThenNot(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String expectedResultingPostingList) {
        booleanExpression = irs.createNewBooleanExpression().setMatchingValue(word).and(word2).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryOrThenNot(String word, String word2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String expectedResultingPostingList) {
        booleanExpression = irs.createNewBooleanExpression().setMatchingValue(word).or(word2).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    // TODO: add more complicated query (e.g., NOT(NOT(term1 AND term2)), (term1 OR NOT(term1 AND term2)), ...)
    // TODO: add benchmarks

//
//    @Test
//    void evaluatePhraseQuery() {// TODO: evaluate phrase queries
//    }

}