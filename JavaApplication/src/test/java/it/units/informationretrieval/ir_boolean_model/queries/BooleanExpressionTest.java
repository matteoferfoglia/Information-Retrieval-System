package it.units.informationretrieval.ir_boolean_model.queries;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest.randomPhraseFromDictionaryOfMovieInvertedIndex;
import static it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest.randomTokenFromDictionaryOfMovieInvertedIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class BooleanExpressionTest {

    private static final String PATH_TO_FILE_WITH_QUERY_SAMPLES = "/singleWordQuery.csv";
    private static final String PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES = "/phraseQuery.csv";
    private static final String LIST_ELEMENTS_SEPARATOR_IN_CSV = "#";
    private static final int NUM_LINES_TO_SKIP_IN_CSV = 1;
    private static final String COMMENT_FOR_BENCHMARK = "Terms in queries randomly taken from the dictionary.";
    private static final InformationRetrievalSystem irsForBenchmark =
            new InformationRetrievalSystem(InvertedIndexTest.invertedIndexForMovieCorpus);
    private static final Corpus corpus;
    private static BooleanExpression booleanExpression;
    private static InformationRetrievalSystem irsForTests;

    static {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));    // ignore std out in tests
        try {
            irsForTests = new InformationRetrievalSystem(InvertedIndexTest.getLoadedSampleCorpus());
        } catch (URISyntaxException | IOException e) {
            fail(e);
        }
        corpus = irsForTests.getCorpus();
        System.setOut(realStdOut);
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateOneWordQuery() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryAnd() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .and(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .or(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryAndThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .and(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryOrThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .or(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryNotThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryNotThenAndThenAndThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .and(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .and(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryNotThenOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .or(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryNotThenOrThenOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .or(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .or(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluateQueryAndThenOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingValue(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .and(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .or(randomTokenFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryAnd() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .and(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .or(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 2, iterations = 3, tearDownIterations = 1, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryAndThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .and(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryOrThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .or(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryNotThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryNotThenAndThenAndThenNot() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .and(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .and(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryNotThenOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .or(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryNotThenOrThenOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .not()
                .or(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .or(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQueryAndThenOr() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .and(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .or(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10, commentToReport = COMMENT_FOR_BENCHMARK)
    static void createAndEvaluatePhraseQuery() {
        irsForBenchmark.createNewBooleanExpression()
                .setMatchingPhrase(randomPhraseFromDictionaryOfMovieInvertedIndex.get())
                .evaluate();
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
    void evaluateQueryNotThenOr(String word, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7, String ignored8, String ignored9,
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

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQuery(String phrase, String ignored, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase.split(" "));
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryNot(String phrase, String ignored, String ignored2, String ignored3, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase.split(" ")).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateQueryAnd(String phrase1, String phrase2, String ignored, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" ")).and(phrase2.split(" "));
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryOr(String phrase1, String phrase2, String ignored, String ignored2, String ignored3,
                               String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" ")).or(phrase2.split(" "));
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryAndThenNot(String phrase1, String phrase2, String ignored, String ignored2, String ignored3, String ignored4,
                                       String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" ")).and(phrase2.split(" ")).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryOrThenNot(String phrase1, String phrase2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5,
                                      String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" ")).or(phrase2.split(" ")).not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryNotThenNot(String phrase1, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7,
                                       String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" ")).not().not();
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryNotThenAndThenAnd(String phrase1, String phrase2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7,
                                              String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" "))
                .not().and(phrase2.split(" ")).and(phrase1.split(" "));
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryNotThenOr(String phrase1, String phrase2, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7, String ignored8,
                                      String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" ")).not().or(phrase2.split(" "));
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_PHRASE_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluatePhraseQueryNotThenOr2(String phrase1, String ignored0, String ignored, String ignored2, String ignored3, String ignored4, String ignored5, String ignored6, String ignored7, String ignored8, String ignored9,
                                       String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingPhrase(phrase1.split(" ")).not().or(phrase1.split(" "));
        assertQueryResultsAreCorrect(expectedResultingPostingList);
    }

    @BeforeEach
    void initializeBooleanExpression() {
        booleanExpression = irsForTests.createNewBooleanExpression();
    }

    @Test
    void throwIfTryingToSetMatchingValueInAnAlreadySetQuery() {
        try {
            irsForTests.createNewBooleanExpression()                            // query created
                    .and("foo")                                                 // condition set
                    .setMatchingValue("impossible to set this");                // should aggregate with a binary operator to another query, not to set directly
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingPhraseInAnAlreadySetQuery() {
        try {
            irsForTests.createNewBooleanExpression()                                // query created
                    .and("foo")                                                     // condition set
                    .setMatchingPhrase("impossible to set this".split(" "));  // should aggregate with a binary operator to another query, not to set directly
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingValueWhenMatchingValueIsAlreadySet() {
        try {
            irsForTests.createNewBooleanExpression()
                    .setMatchingValue("foo")
                    .setMatchingValue("impossible to set this");
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingValueWhenMatchingPhraseIsAlreadySet() {
        try {
            irsForTests.createNewBooleanExpression()
                    .setMatchingPhrase("foo bar".split(" "))
                    .setMatchingValue("impossible to set this");
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingPhraseWhenMatchingValueIsAlreadySet() {
        try {
            irsForTests.createNewBooleanExpression()
                    .setMatchingValue("foo")
                    .setMatchingPhrase("impossible to set this".split(" "));
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToSetMatchingPhraseWhenMatchingPhraseIsAlreadySet() {
        try {
            irsForTests.createNewBooleanExpression()
                    .setMatchingPhrase("foo bar".split(" "))
                    .setMatchingPhrase("impossible to set this".split(" "));
            fail("Should have thrown but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void throwIfTryingToLimitResultsWithNegativeNumber() {
        try {
            irsForTests.createNewBooleanExpression()
                    .limit(-1);
            fail("Should have thrown but did not.");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = PATH_TO_FILE_WITH_QUERY_SAMPLES, numLinesToSkip = NUM_LINES_TO_SKIP_IN_CSV)
    void evaluateOneWordQueryAndLimitNumberOfResults(String word, String ignored, String expectedResultingPostingList) {
        booleanExpression = booleanExpression.setMatchingValue(word);
        final int NUM_OF_RESULTS = splitDocIdIntoList(expectedResultingPostingList).size();
        final int LIMITED_NUM_OF_RESULTS = NUM_OF_RESULTS > 0 ? NUM_OF_RESULTS - 1 : NUM_OF_RESULTS;
        assertEquals(LIMITED_NUM_OF_RESULTS, booleanExpression.limit(LIMITED_NUM_OF_RESULTS).evaluate().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"The cat", "a  ", "The   cate"})
    void parseQuery(String inputQueryString) {
        String[] words = Utility.split(inputQueryString);
        assert words.length > 0;
        BooleanExpression expected = irsForTests.createNewBooleanExpression().setMatchingValue(words[0]);
        for (int i = 1; i < words.length; i++) {
            expected.and(words[i]);
        }

        booleanExpression.parseQuery(inputQueryString);
        assertEquals(expected.getQueryString(), booleanExpression.getQueryString());
    }

    @Test
    void parseQueryWithNot() {
        String inputQueryString = "Foo !bar";
        BooleanExpression expected = irsForTests.createNewBooleanExpression()
                .setMatchingValue("Foo")
                .and(irsForTests.createNewBooleanExpression().setMatchingValue("bar").not());
        booleanExpression.parseQuery(inputQueryString);
        assertEquals(expected.getQueryString(), booleanExpression.getQueryString());
    }

    @Test
    void parseQueryWithPrecedence() {
        String inputQueryString = "!(Foo & (!bar| foo))";
        BooleanExpression be1 = irsForTests.createNewBooleanExpression().setMatchingValue("bar").not();
        BooleanExpression expected = irsForTests.createNewBooleanExpression()
                .setMatchingValue("Foo")
                .and(be1.or("foo"))
                .not();
        booleanExpression.parseQuery(inputQueryString);

        Function<BooleanExpression, String> simplifyQueryString = be ->
                QueryParsing.toString(
                        QueryParsing.parse(be.getQueryString()
                                .replaceAll(BINARY_OPERATOR.AND.toString(), BINARY_OPERATOR.AND.getSymbol())
                                .replaceAll(BINARY_OPERATOR.OR.toString(), BINARY_OPERATOR.OR.getSymbol())
                                .replaceAll(UNARY_OPERATOR.NOT.toString(), UNARY_OPERATOR.NOT.getSymbol())));

        assertEquals(simplifyQueryString.apply(expected), simplifyQueryString.apply(booleanExpression));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"The cat\"", "\"The cat is on  the table\""})
    void parsePhraseQuery(String inputPhraseQueryString) {
        BooleanExpression expected = irsForTests.createNewBooleanExpression()
                .setMatchingPhrase(
                        Arrays.stream(Utility.split(inputPhraseQueryString.replaceAll("\"", "")))
                                .filter(s -> !s.isBlank()).toArray(String[]::new));
        booleanExpression.parseQuery(inputPhraseQueryString);
        assertEquals(expected.getQueryString(), booleanExpression.getQueryString());
    }

    @Test
    void parsePhraseQueryWithIntermediateWords() {
        String inputPhraseQueryString = BooleanExpression.PHRASE_DELIMITER
                + "The" + BooleanExpression.NUM_OF_WORDS_FOLLOWS_CHARACTER + "0cat"
                + BooleanExpression.PHRASE_DELIMITER;
        BooleanExpression expected = irsForTests.createNewBooleanExpression()
                .setMatchingPhrase(new String[]{"The", "cat"}, new int[]{1});
        booleanExpression.parseQuery(inputPhraseQueryString);
        assertEquals(expected.getQueryString(), booleanExpression.getQueryString());
    }

    @Test
    void parsePhraseQueryWithIntermediateWords2() {
        String inputPhraseQueryString = BooleanExpression.PHRASE_DELIMITER
                + "The" + BooleanExpression.NUM_OF_WORDS_FOLLOWS_CHARACTER + "1is"
                + BooleanExpression.PHRASE_DELIMITER;
        BooleanExpression expected = irsForTests.createNewBooleanExpression()
                .setMatchingPhrase(new String[]{"The", "is"}, new int[]{2});
        booleanExpression.parseQuery(inputPhraseQueryString);
        assertEquals(expected.getQueryString(), booleanExpression.getQueryString());
    }

    @Test
    void parsePhraseQueryWithIntermediateWords3() {
        String inputPhraseQueryString = BooleanExpression.PHRASE_DELIMITER
                + "The" + BooleanExpression.NUM_OF_WORDS_FOLLOWS_CHARACTER + "1is"
                + BooleanExpression.NUM_OF_WORDS_FOLLOWS_CHARACTER + "2table"
                + BooleanExpression.PHRASE_DELIMITER;
        BooleanExpression expected = irsForTests.createNewBooleanExpression()
                .setMatchingPhrase(new String[]{"The", "is", "table"}, new int[]{2, 5});
        booleanExpression.parseQuery(inputPhraseQueryString);
        assertEquals(expected.getQueryString(), booleanExpression.getQueryString());
    }

    @Test
    void throwIfTryToParseQueryOnAnAlreadySetExpression() {
        String whatever = "whatever";
        booleanExpression.setMatchingValue(whatever);
        try {
            booleanExpression.parseQuery(whatever);
            fail("Should have thrown exception but did not.");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void numOfRepetitionsForHandlingSpecialCharactersAreAlwaysDifferent() {
        List<Integer> usedValues = Arrays.stream(BooleanExpression.class.getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .filter(f -> f.getName().startsWith("VALID_PARSING_CHAR_REPETITIONS_FOR_"))
                .map(f -> {
                    try {
                        return (int) f.get(null/*static fields*/);
                    } catch (IllegalAccessException e) {
                        fail(e);
                        return 0;
                    }
                })
                .toList();
        assertEquals(usedValues.size(), usedValues.stream().distinct().toList().size());
        // If any of that values is duplicated, it would not be possible to distinguish
        //  two special characters that at beginning were different
        // This is done to handle the parsing of special characters
    }
}