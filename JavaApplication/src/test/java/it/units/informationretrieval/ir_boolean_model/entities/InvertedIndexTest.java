package it.units.informationretrieval.ir_boolean_model.entities;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeCorpus;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocumentIdentifier;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import skiplist.SkipList;

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

public class InvertedIndexTest {

    private static final String PATH_TO_CORPUS = "/SampleCorpus.csv";
    private static final String PATH_TO_INVERTED_INDEX = "/InvertedIndexForSampleCorpus.csv";
    private static final String CSV_SEPARATOR = ",";
    private static final String POSTINGS_SEPARATOR_IN_CSV_FILE = "\\|";
    private static final String LIST_ELEMENTS_SEPARATOR_IN_CSV_FILE = "#";
    public static InvertedIndex invertedIndexForMovieCorpus;
    private static Corpus movieCorpus;
    private static Corpus sampleCorpus;
    private static InvertedIndex invertedIndexForTests;
    private static Map<String, SkipList<Posting>> expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfPostings;

    static {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));      // ignore std out for this block
        try {
            movieCorpus = Movie.createCorpus();                           // used for benchmarks
            invertedIndexForMovieCorpus = new InvertedIndex(movieCorpus); // used for benchmarks
        } catch (NoMoreDocIdsAvailable | URISyntaxException e) {
            fail(e);
        }
        System.setOut(realStdOut);
    }
    public static final Supplier<String> randomTokenFromDictionaryOfMovieInvertedIndex = new Supplier<>() {

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
        // parsing of file from resources for tests
        expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfPostings =
                readCsvAndGetStreamWithAnArrayForEachLine(PATH_TO_INVERTED_INDEX)
                        .map(invertedIndexEntry -> new AbstractMap.SimpleEntry<>(
                                (String) invertedIndexEntry[0],
                                new SkipList<>(
                                        Arrays.stream(((String) invertedIndexEntry[1])
                                                        .split(POSTINGS_SEPARATOR_IN_CSV_FILE))
                                                .map(postingAsString -> {
                                                    int docIdValue =
                                                            Integer.parseInt(postingAsString.substring(0, postingAsString.indexOf("[")));
                                                    int[] positionsOfTokenInThisDoc =
                                                            Arrays.stream(postingAsString
                                                                            .substring(postingAsString.indexOf("[") + 1, postingAsString.indexOf("]"))
                                                                            .split(LIST_ELEMENTS_SEPARATOR_IN_CSV_FILE))
                                                                    .mapToInt(Integer::parseInt)
                                                                    .toArray();
                                                    return new Posting(
                                                            new FakeDocumentIdentifier(docIdValue), positionsOfTokenInThisDoc);
                                                })
                                                .toList())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @BeforeAll
    static void loadSampleCorpusFromFile() throws URISyntaxException, IOException {
        sampleCorpus = getSampleCorpus();
    }

    @NotNull
    public static FakeCorpus getSampleCorpus() throws IOException, URISyntaxException {
        return new FakeCorpus(
                readCsvAndGetStreamWithAnArrayForEachLine(PATH_TO_CORPUS)
                        .map(invertedIndexEntry -> new AbstractMap.SimpleEntry<>(
                                Integer.parseInt((String) invertedIndexEntry[0]),
                                (String) invertedIndexEntry[1]))
                        .collect(Collectors.toMap(
                                oneLineAsEntry -> new FakeDocumentIdentifier(oneLineAsEntry.getKey()),
                                oneLineAsEntry -> new FakeDocument_LineOfAFile("", oneLineAsEntry.getValue()))));
    }

    @NotNull
    public static Corpus getLoadedSampleCorpus() throws URISyntaxException, IOException {
        if (sampleCorpus == null) {
            loadSampleCorpusFromFile();
        }
        return sampleCorpus;
    }

    @NotNull
    private static Stream<Object[]> readCsvAndGetStreamWithAnArrayForEachLine(String pathToCSVFile)
            throws IOException, URISyntaxException {
        final int NUMBER_OF_HEADING_LINES = 1;
        return Files
                .lines(Path.of(Objects.requireNonNull(FakeDocument_LineOfAFile.class.getResource(pathToCSVFile)).toURI()))
                .sequential()
                .skip(NUMBER_OF_HEADING_LINES)
                .map(aLine -> Arrays.stream(aLine.split(CSV_SEPARATOR))
                        .sequential()
                        .map(String::strip)
                        .toArray());
    }

    @BeforeEach
    void setUp() {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));      // ignore std out for this block
        invertedIndexForTests = new InvertedIndex(sampleCorpus);
        System.setOut(realStdOut);
    }

    @AfterEach
    void tearDown() {
    }

    private Map<String, SkipList<Posting>> getMapOfTermAndCorrespondingListOfPostingsFromInvertedIndex(
            InvertedIndex invertedIndex) {

        return invertedIndex
                .getDictionary()
                .stream()
                .map(tokenFromIndex -> new AbstractMap.SimpleEntry<>(
                        tokenFromIndex,
                        invertedIndex.getPostingListForToken(tokenFromIndex).toSkipList()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    void indexCorpusAndGet() {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));      // ignore std out for this block
        invertedIndexForTests = new InvertedIndex(sampleCorpus);
        System.setOut(realStdOut);
        assertEquals(
                expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfPostings,
                getMapOfTermAndCorrespondingListOfPostingsFromInvertedIndex(invertedIndexForTests));
    }

    @Test
    void testPositionalIndex() {
        indexCorpusAndGet();
        invertedIndexForTests.getDictionary()
                .forEach(tokenFromDictionaryOfCreatedInvertedIndex -> {
                    var createdListOfPostings = invertedIndexForTests
                            .getPostingListForToken(tokenFromDictionaryOfCreatedInvertedIndex)
                            .toSkipList();
                    var expectedListOfPostings =
                            expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfPostings
                                    .get(tokenFromDictionaryOfCreatedInvertedIndex);
                    assertEquals(expectedListOfPostings, createdListOfPostings);
                    createdListOfPostings.forEach(posting -> {
                        var actualPositionsOfTokenInDocument = posting.getTermPositionsInTheDocument();
                        var expectedPositionsOfTokenInDocument = expectedListOfPostings.stream()
                                .filter(posting1 -> posting1.equals(posting))
                                .findAny()
                                .map(Posting::getTermPositionsInTheDocument)
                                .orElseThrow();
                        assertEquals(
                                Arrays.toString(expectedPositionsOfTokenInDocument),    // use toString because test framework cannot compare arrays
                                Arrays.toString(actualPositionsOfTokenInDocument));
                    });
                });
    }

    @Test
    void getDictionary() {
        assertEquals(
                expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfPostings.keySet(),
                new HashSet<>(invertedIndexForTests.getDictionary()));
    }

    @Test
    void getCorpus() {
        assertEquals(sampleCorpus, invertedIndexForTests.getCorpus());
    }

    @Test
    void getPostingListForToken() {
        expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfPostings
                .forEach((token, listOfPostings) ->
                        assertEquals(
                                listOfPostings,
                                invertedIndexForTests.getPostingListForToken(token).toSkipList()));
    }
}