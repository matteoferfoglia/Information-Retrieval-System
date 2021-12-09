package it.units.informationretrieval.ir_boolean_model.entities;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeCorpus;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocumentIdentifier;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Properties;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class InvertedIndexTest {

    private static final String PATH_TO_CORPUS = "/SampleCorpus.csv";
    private static final String PATH_TO_INVERTED_INDEX = "/InvertedIndexForSampleCorpus.csv";
    private static final String CSV_SEPARATOR = ",";
    private static final String DOC_ID_SEPARATOR_IN_CSV_FILE = "#";

    private static Corpus movieCorpus;
    private static Corpus sampleCorpus;

    private static InvertedIndex invertedIndexForTests;
    private static InvertedIndex invertedIndexForMovieCorpus;
    private static final Supplier<String> randomTokenFromDictionaryOfMovieInvertedIndex = new Supplier<>() {

        private static final List<String> dictionary = new ArrayList<>(invertedIndexForMovieCorpus.getDictionary());
        private static final int dictionaryLength = dictionary.size();
        private static final String[] randomPermutationOfTokensFromDictionary = dictionary.toArray(String[]::new);
        private static int numberOfGeneratedToken = 0;

        static {
            Collections.shuffle(dictionary);
        }

        @Override
        public String get() {
            return randomPermutationOfTokensFromDictionary[numberOfGeneratedToken++ % dictionaryLength];
        }
    };
    private static Map<String, List<Integer>> expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfDocsContainingIt;

    static {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));      // ignore std out for this block
        try {
            Properties.loadProperties();// TODO: is needed to use properties? Maybe better to have a class with public parameters
            movieCorpus = Movie.createCorpus();                           // used for benchmarks
            invertedIndexForMovieCorpus = new InvertedIndex(movieCorpus); // used for benchmarks
        } catch (IOException | NoMoreDocIdsAvailable | URISyntaxException e) {
            fail(e);
        }
        System.setOut(realStdOut);
    }

    @Benchmark(warmUpIterations = 1, iterations = 3, tearDownIterations = 1, commentToReport = "Inverted index for the Movie corpus.")
    static void createInvertedIndexForMovieCorpus() {
        new InvertedIndex(movieCorpus);
    }

    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10)
    static void getDictionaryOfInvertedIndexForMovieCorpus() {
        invertedIndexForMovieCorpus.getDictionary();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")  // used for benchmark
    @Benchmark(warmUpIterations = 10, iterations = 10, tearDownIterations = 10)
    static void getCorpusOfInvertedIndexForMovieCorpus() {
        invertedIndexForMovieCorpus.getCorpus();
    }

    @Benchmark(warmUpIterations = 100, iterations = 10000, tearDownIterations = 100)
    static void getPostingListOfARandomTokenChosenFromDictionaryFromInvertedIndexForMovieCorpus() {
        invertedIndexForMovieCorpus.getPostingListForToken(randomTokenFromDictionaryOfMovieInvertedIndex.get());
    }

    @BeforeAll
    static void loadExpectedInvertedIndexFromFile() throws URISyntaxException, IOException {
        expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfDocsContainingIt =
                readCsvAndGetStreamWithAnArrayForEachLine(PATH_TO_INVERTED_INDEX)
                        .map(invertedIndexEntry -> new AbstractMap.SimpleEntry<>(
                                (String) invertedIndexEntry[0],
                                Arrays.stream(((String) invertedIndexEntry[1])
                                                .split(DOC_ID_SEPARATOR_IN_CSV_FILE))
                                        .map(Integer::parseInt)
                                        .toList()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @BeforeAll
    static void loadSampleCorpusFromFile() throws URISyntaxException, IOException {
        sampleCorpus =
                new FakeCorpus(
                        readCsvAndGetStreamWithAnArrayForEachLine(PATH_TO_CORPUS)
                                .map(invertedIndexEntry -> new AbstractMap.SimpleEntry<>(
                                        Integer.parseInt((String) invertedIndexEntry[0]),
                                        (String) invertedIndexEntry[1]))
                                .collect(Collectors.toMap(
                                        oneLineAsEntry -> new FakeDocumentIdentifier(oneLineAsEntry.getKey()),
                                        oneLineAsEntry -> new FakeDocument_LineOfAFile("", oneLineAsEntry.getValue()))));
    }

    @NotNull
    private static Stream<Object[]> readCsvAndGetStreamWithAnArrayForEachLine(String pathToCSVFile)
            throws IOException, URISyntaxException {
        final int NUMBER_OF_HEADING_LINES = 1;
        return Files
                .lines(
                        Path.of(Objects.requireNonNull(FakeDocument_LineOfAFile.class.getResource(pathToCSVFile)).toURI()))
                .sequential()
                .skip(NUMBER_OF_HEADING_LINES)
                .map(aLine -> Arrays.stream(aLine.split(CSV_SEPARATOR))
                        .sequential()
                        .map(String::strip)
                        .toArray());
    }

    @BeforeEach
    void setUp() {
        invertedIndexForTests = new InvertedIndex(sampleCorpus);
    }

    @AfterEach
    void tearDown() {
    }

    private Map<String, List<Integer>> getMapOfTermAndCorrespondingListOfDocIdsFromInvertedIndex(
            InvertedIndex invertedIndex) {

        return invertedIndex
                .getDictionary()
                .stream()
                .map(tokenFromIndex -> new AbstractMap.SimpleEntry<>(
                        tokenFromIndex,
                        getListOfDocIdsForToken(invertedIndex, tokenFromIndex)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<Integer> getListOfDocIdsForToken(InvertedIndex invertedIndex, String tokenFromIndex) {
        return invertedIndex.getPostingListForToken(tokenFromIndex)
                .toListOfPostings()
                .stream()
                .map(posting -> posting.getDocId().getDocId())
                .toList();
    }

    @Test
    void indexCorpusAndGet() {
        invertedIndexForTests = new InvertedIndex(sampleCorpus);
        assertEquals(
                expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfDocsContainingIt,
                getMapOfTermAndCorrespondingListOfDocIdsFromInvertedIndex(invertedIndexForTests));
    }

    @Test
    void getDictionary() {
        assertEquals(
                expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfDocsContainingIt.keySet(),
                new HashSet<>(invertedIndexForTests.getDictionary()));
    }

    @Test
    void getCorpus() {
        assertEquals(sampleCorpus, invertedIndexForTests.getCorpus());
    }

    @Test
    void getPostingListForToken() {
        expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfDocsContainingIt
                .forEach((token, listOfDocIds) ->
                        assertEquals(listOfDocIds, getListOfDocIdsForToken(invertedIndexForTests, token)));
    }
}