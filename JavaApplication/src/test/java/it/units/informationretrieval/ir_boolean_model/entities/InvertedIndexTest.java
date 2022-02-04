package it.units.informationretrieval.ir_boolean_model.entities;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeCorpus;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocumentIdentifier;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies.MovieCorpusFactory;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import skiplist.SkipList;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
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
    private static final String PATH_TO_PERMUTERM_INDEX = "/PermutermIndexForSampleCorpus.csv";
    private static final String PATH_TO_TOKEN_WITH_WILDCARDS_AND_CORRESPONDING_LIST_OF_POSTING = "/WildcardTokenForSampleCorpus.csv";
    private static final String CSV_SEPARATOR = ",";
    private static final String POSTINGS_SEPARATOR_IN_CSV_FILE = "\\|";
    private static final String LIST_ELEMENTS_SEPARATOR_IN_CSV_FILE = "#";
    private static final String END_OF_WORD_PERMUTERM_USED_IN_TESTS = "\\$";
    public static InvertedIndex invertedIndexForMovieCorpus;
    public static Supplier<String> randomTokenFromDictionaryOfMovieInvertedIndex;
    public static Supplier<String[]> randomPhraseFromDictionaryOfMovieInvertedIndex;
    private static String END_OF_WORD_PERMUTERM_USED_IN_INVERTED_INDEX;
    private static Corpus sampleCorpus;
    private static Corpus movieCorpus;
    private static InvertedIndex invertedIndexForTests;
    private static Map<String, SkipList<Posting>> expectedInvertedIndexFromFileAsMapOfStringAndCorrespondingListOfPostings;
    private static Map<String, String> expectedPermutermIndexFromFileAsMapOfStringAndCorrespondingStringFromDictionary;
    private static Map<String, SkipList<Posting>> expectedMapOfStringWithWildcardAndCorrespondingListOfPostingsFromFile;

    static {

        try {
            movieCorpus = new MovieCorpusFactory().createCorpus();
        } catch (NoMoreDocIdsAvailable | IOException e) {
            fail(e);
        }

        try {
            Field endOfWordPermutermField = InvertedIndex.class.getDeclaredField("END_OF_WORD");
            endOfWordPermutermField.setAccessible(true);
            END_OF_WORD_PERMUTERM_USED_IN_INVERTED_INDEX = (String) endOfWordPermutermField.get(null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail(e);
        }

        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));      // ignore std out for this block
        try {
            Corpus movieCorpus = new MovieCorpusFactory().createCorpus(); // used for benchmarks
            invertedIndexForMovieCorpus = new InvertedIndex(movieCorpus); // used for benchmarks
            randomTokenFromDictionaryOfMovieInvertedIndex = new Supplier<>() {

                private static final List<String> dictionary = new ArrayList<>(invertedIndexForMovieCorpus.getDictionary());
                private static final int dictionaryLength = dictionary.size();
                private static final String[] randomPermutationOfTokensFromDictionary;
                private static int numberOfGeneratedToken = 0;

                static {
                    Collections.shuffle(dictionary);
                    randomPermutationOfTokensFromDictionary = dictionary.toArray(String[]::new);
                }

                @Override
                public String get() {
                    return randomPermutationOfTokensFromDictionary[numberOfGeneratedToken++ % dictionaryLength];
                }
            };
            randomPhraseFromDictionaryOfMovieInvertedIndex = new Supplier<>() {

                private static final int PHRASE_LENGTH = 5;
                private static final int NUM_OF_DOCS_TO_USE = 10000;

                private static final List<Document> movies = new ArrayList<>(invertedIndexForMovieCorpus.getCorpus().getCorpus().values());
                private static final List<String[]> randomPermutationOfPhrasesFromDocuments =
                        movies.stream().unordered().sequential()
                                .map(Document::getContent)
                                .filter(Objects::nonNull)
                                .limit(NUM_OF_DOCS_TO_USE)
                                .map(DocumentContent::getEntireTextContent)
                                .map(docContent -> {
                                    var randomStartingPosition = (int) (Math.random() * Math.max(0, docContent.length() - PHRASE_LENGTH));
                                    return docContent.substring(randomStartingPosition, randomStartingPosition + PHRASE_LENGTH);
                                })
                                .map(phrase -> Arrays.stream(phrase.split(" "))
                                        .filter(Objects::nonNull).toArray(String[]::new))
                                .collect(Collectors.toList());
                private static int numberOfGeneratedPhrase = 0;

                static {
                    assert randomPermutationOfPhrasesFromDocuments.size() > 0;
                    Collections.shuffle(randomPermutationOfPhrasesFromDocuments);
                }

                @Override
                public String[] get() {
                    var phraseToReturn = randomPermutationOfPhrasesFromDocuments
                            .get(numberOfGeneratedPhrase++ % randomPermutationOfPhrasesFromDocuments.size());
                    assert Arrays.stream(phraseToReturn).noneMatch(Objects::isNull);
                    return phraseToReturn;
                }
            };
        } catch (NoMoreDocIdsAvailable | IOException e) {
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

    @Benchmark(warmUpIterations = 100, iterations = 100, tearDownIterations = 100)
    static void getPostingListOfARandomTokenChosenFromDictionaryFromInvertedIndexForMovieCorpus() {
        invertedIndexForMovieCorpus.getListOfPostingsForToken(randomTokenFromDictionaryOfMovieInvertedIndex.get());
    }

    @BeforeAll
    static void loadExpectedInvertedAndPermutermIndexFromFile() throws URISyntaxException, IOException {
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

        expectedMapOfStringWithWildcardAndCorrespondingListOfPostingsFromFile =
                readCsvAndGetStreamWithAnArrayForEachLine(PATH_TO_TOKEN_WITH_WILDCARDS_AND_CORRESPONDING_LIST_OF_POSTING)
                        .map(invertedIndexEntry -> new AbstractMap.SimpleEntry<>(
                                (String) invertedIndexEntry[0],
                                new SkipList<>(
                                        Arrays.stream(((String) invertedIndexEntry[1])
                                                        .split(POSTINGS_SEPARATOR_IN_CSV_FILE))
                                                .map(Integer::parseInt)
                                                .map(FakeDocumentIdentifier::new)
                                                .map(docId -> new Posting(docId, new int[0]))
                                                .toList())))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        expectedPermutermIndexFromFileAsMapOfStringAndCorrespondingStringFromDictionary =
                readCsvAndGetStreamWithAnArrayForEachLine(PATH_TO_PERMUTERM_INDEX)
                        .map(aRow -> Arrays.stream(aRow)
                                .map(val -> (String) val)
                                .map(val -> val.replaceAll(END_OF_WORD_PERMUTERM_USED_IN_TESTS, END_OF_WORD_PERMUTERM_USED_IN_INVERTED_INDEX))
                                .toArray(String[]::new))
                        .map(aRow -> new Pair<>(aRow[0], aRow[1]))
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
        final int NUMBER_OF_HEADING_LINES = 1; // commented lines excluded
        return Files
                .lines(Path.of(Objects.requireNonNull(FakeDocument_LineOfAFile.class.getResource(pathToCSVFile)).toURI()))
                .sequential()
                .filter(line -> !line.strip().startsWith("#")/*commented lines start with #*/)
                .skip(NUMBER_OF_HEADING_LINES)
                .map(aLine -> Arrays.stream(aLine.split(CSV_SEPARATOR))
                        .sequential()
                        .map(String::strip)
                        .toArray());
    }

    public static Stream<Arguments> getTokenAndCorrespondingListOfPostings() {
        return expectedMapOfStringWithWildcardAndCorrespondingListOfPostingsFromFile
                .entrySet().stream()
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
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
                        invertedIndex.getListOfPostingsForToken(tokenFromIndex)))
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
    void testPermutermIndexCreation() {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));      // ignore std out for this block
        invertedIndexForTests = new InvertedIndex(sampleCorpus);
        System.setOut(realStdOut);
        assertEquals(
                expectedPermutermIndexFromFileAsMapOfStringAndCorrespondingStringFromDictionary,
                invertedIndexForTests.getCopyOfPermutermIndex());
    }

    @Test
    void testPositionalIndex() {
        indexCorpusAndGet();
        invertedIndexForTests.getDictionary()
                .forEach(tokenFromDictionaryOfCreatedInvertedIndex -> {
                    var createdListOfPostings = invertedIndexForTests
                            .getListOfPostingsForToken(tokenFromDictionaryOfCreatedInvertedIndex);
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

    @ParameterizedTest
    @MethodSource("getTokenAndCorrespondingListOfPostings")
    void getListOfPostingForTokenWithWildcard(String token, SkipList<Posting> listOfPostings) {
        assertEquals(
                // NOTE: a number of postings can have the same docId (one posting for each distinct term),
                //       but using the comparator Posting.DOC_ID_COMPARATOR, all postings having the same
                //       docId are considered equals
                new SkipList<>(listOfPostings, Posting.DOC_ID_COMPARATOR),
                new SkipList<>(invertedIndexForTests.getListOfPostingsForToken(token), Posting.DOC_ID_COMPARATOR));
    }
}