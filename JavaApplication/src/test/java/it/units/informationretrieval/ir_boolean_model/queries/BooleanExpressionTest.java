package it.units.informationretrieval.ir_boolean_model.queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BooleanExpressionTest {   // TODO: add missing tests

    private static final String PATH_TO_FILE_WITH_QUERY_SAMPLES = "/queriesForSampleCorpus/singleWordQuery.csv";
    private static final String PATH_TO_FILE_WITH_QUERY_NOT_SAMPLES = "/queriesForSampleCorpus/singleWordQueryNot.csv";
    private static final String PATH_TO_FILE_WITH_QUERY_AND_SAMPLES = "/queriesForSampleCorpus/singleWordQueryAnd.csv";
    private static BooleanExpression booleanExpression;
    private static InformationRetrievalSystem irs;

    static {
        try {
            irs = new InformationRetrievalSystem(InvertedIndexTest.getLoadedSampleCorpus());
        } catch (URISyntaxException | IOException e) {
            fail(e);
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = 1)
    void evaluateOneWordQuery(String word, int expectedNumberOfDocsContainingTheWord) {
        booleanExpression = new BooleanExpression(word, irs);
        assertEquals(expectedNumberOfDocsContainingTheWord, booleanExpression.evaluate().size());
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_NOT_SAMPLES, numLinesToSkip = 1)
    void evaluateQueryNot(String word, int expectedNumberOfDocsNotContainingTheWord) {
        booleanExpression = new BooleanExpression(word, irs).not();
        assertEquals(expectedNumberOfDocsNotContainingTheWord, booleanExpression.evaluate().size());
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_AND_SAMPLES, numLinesToSkip = 1)
    void evaluateQueryAnd(String word, int expectedNumberOfDocsNotContainingTheWord) {
    }

    @Test
    void evaluateQueryOr() {
    }

    @Test
    void evaluateAggregateQueryNot() {
    }

//    @Test
//    void evaluatePhraseQuery() {// TODO: evaluate phrase queries
//    }

}