package it.units.informationretrieval.ir_boolean_model.utils;

import benchmark.Benchmark;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.SynchronizedSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import skiplist.SkipList;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UtilityTest {

    private static final String LONG_DOCUMENT_PATH = "/LongDocument.txt";
    private static final String COMMENT_FOR_BENCHMARKS = "Text document of about 140 KB";
    private static final int DEFAULT_NUM_OF_ITERATIONS_BENCHMARK = 10;
    private static final Supplier<List<Integer>> LIST_OF_1000_RANDOM_INTS_SUPPLIER = new Supplier<>() {
        private static final int NUMBER_OF_ELEMENTS_OF_EACH_LISTS = 1000;
        private static final int NUMBER_OF_PRECOMPUTED_CACHED_LISTS = 1000;
        private static final Supplier<List<Integer>> listsSupplier = () ->
                IntStream.range(0, NUMBER_OF_PRECOMPUTED_CACHED_LISTS)
                        .mapToObj(i -> (int) ((1 - 2 * Math.random()) * NUMBER_OF_ELEMENTS_OF_EACH_LISTS))
                        .collect(Collectors.toList());
        private static final List<List<Integer>> CACHED_LISTS =
                IntStream.range(0, NUMBER_OF_PRECOMPUTED_CACHED_LISTS)
                        .mapToObj(i -> listsSupplier.get())
                        .collect(Collectors.toList());
        private static int counter = 0;

        @Override
        public List<Integer> get() {
            return CACHED_LISTS.get(counter++ % NUMBER_OF_PRECOMPUTED_CACHED_LISTS);
        }
    };
    private static final Supplier<List<Integer>> LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER = new Supplier<>() {
        private static final int NUMBER_OF_ELEMENTS_OF_EACH_LISTS = 1000;
        private static final int NUMBER_OF_PRECOMPUTED_CACHED_LISTS = 1000;
        private static final Supplier<List<Integer>> listsSupplier = () ->
                IntStream.iterate(0, i -> i + 1)
                        .filter(ignored -> Math.random() < 0.5)
                        .limit(NUMBER_OF_ELEMENTS_OF_EACH_LISTS)
                        .boxed()
                        .collect(Collectors.toList());
        private static final List<List<Integer>> CACHED_LISTS =
                IntStream.range(0, NUMBER_OF_PRECOMPUTED_CACHED_LISTS)
                        .mapToObj(i -> listsSupplier.get())
                        .collect(Collectors.toList());
        private static int counter = 0;

        @Override
        public List<Integer> get() {
            return CACHED_LISTS.get(counter++ % NUMBER_OF_PRECOMPUTED_CACHED_LISTS);
        }
    };
    private static final Supplier<SkipList<Integer>> LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER = new Supplier<>() {
        private static final int NUMBER_OF_ELEMENTS_OF_EACH_LISTS = 1000;
        private static final int NUMBER_OF_PRECOMPUTED_CACHED_LISTS = 1000;
        private static final Supplier<SkipList<Integer>> listsSupplier = () -> {
            SkipList<Integer> skipList = new SkipList<>();
            skipList.addAll(
                    IntStream.iterate(0, i -> i + 1)
                            .filter(ignored -> Math.random() < 0.5)
                            .limit(NUMBER_OF_ELEMENTS_OF_EACH_LISTS)
                            .boxed()
                            .collect(Collectors.toList()));
            return skipList;
        };
        private static final List<SkipList<Integer>> CACHED_LISTS =
                IntStream.range(0, NUMBER_OF_PRECOMPUTED_CACHED_LISTS)
                        .mapToObj(i -> listsSupplier.get())
                        .collect(Collectors.toList());
        private static int counter = 0;

        @Override
        public SkipList<Integer> get() {
            return CACHED_LISTS.get(counter++ % NUMBER_OF_PRECOMPUTED_CACHED_LISTS);
        }
    };
    private static Document LONG_DOCUMENT;
    private static String LONG_DOCUMENT_CONTENT;
    private static Supplier<String> stringFromLongDocumentSupplier;

    static {
        try {
            LONG_DOCUMENT_CONTENT = Files.readString(Path.of(
                    Objects.requireNonNull(FakeDocument_LineOfAFile.class.getResource(LONG_DOCUMENT_PATH)).toURI()));
            LONG_DOCUMENT = new FakeDocument_LineOfAFile("title", LONG_DOCUMENT_CONTENT);
            stringFromLongDocumentSupplier = new Supplier<>() {
                /**
                 * {@link List} of all non-normalized tokens.
                 */
                private final static String[] tokens = Utility.split(LONG_DOCUMENT_CONTENT);
                private final static int size = tokens.length;
                private static int counter = 0;

                @Override
                public String get() {
                    return tokens[counter++ % size];
                }
            };
        } catch (IOException | URISyntaxException e) {
            fail(e);
        }
    }

    @Benchmark(
            warmUpIterations = DEFAULT_NUM_OF_ITERATIONS_BENCHMARK,
            iterations = DEFAULT_NUM_OF_ITERATIONS_BENCHMARK,
            tearDownIterations = DEFAULT_NUM_OF_ITERATIONS_BENCHMARK,
            commentToReport = COMMENT_FOR_BENCHMARKS)
    static void tokenizeLongDocument() {
        Utility.tokenize(LONG_DOCUMENT, Language.UNDEFINED, new SynchronizedSet<>());
    }

    @Benchmark(
            warmUpIterations = DEFAULT_NUM_OF_ITERATIONS_BENCHMARK,
            iterations = DEFAULT_NUM_OF_ITERATIONS_BENCHMARK,
            tearDownIterations = DEFAULT_NUM_OF_ITERATIONS_BENCHMARK,
            commentToReport = COMMENT_FOR_BENCHMARKS)
    static void normalizeLongDocument() {
        Utility.normalize(stringFromLongDocumentSupplier.get(), false, Language.UNDEFINED);
    }

    @Benchmark
    static void convertObjectWith3AttributesFromJsonToMap() throws JsonProcessingException {
        final String JSON_SAMPLE = "{\"a\": 1, \"b\":\"5\", \"c\": \"foo  bar \"}";
        final Map<String, ?> EXPECTED_MAP = new HashMap<>() {{
            put("a", 1);
            put("b", "5");
            put("c", "foo  bar ");
        }};
        assertEquals(EXPECTED_MAP, Utility.convertFromJsonToMap(JSON_SAMPLE));
    }

    @Benchmark
    static void convertMapOf3EntriesToJson() throws JsonProcessingException {
        final Map<String, ?> SAMPLE_MAP = new HashMap<>() {{
            put("a", 1);
            put("b", "5");
            put("c", "foo  bar ");
        }};
        final String EXPECTED_JSON = "{\"a\":1,\"b\":\"5\",\"c\":\"foo  bar \"}";
        assertEquals(EXPECTED_JSON, Utility.convertToJson(SAMPLE_MAP));
    }

    @Benchmark
    static void sortAndRemoveDuplicatesOnListOf1000RandomInts() {
        Utility.sortAndRemoveDuplicates(LIST_OF_1000_RANDOM_INTS_SUPPLIER.get());
    }

    @Benchmark
    static void unionOfTwoSortedListsOf1000RandomIntsEachOne() {
        Utility.unionOfSortedLists(LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get());
    }

    @Benchmark
    static void intersectionOfTwoSortedListsOf1000RandomIntsEachOne() {
        Utility.intersectionOfSortedLists(LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get());
    }

    @Benchmark
    static void unionOfFourSortedListsOf1000RandomIntsEachOne() {
        Utility.unionOfSortedLists(
                Utility.unionOfSortedLists(LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get()),
                Utility.unionOfSortedLists(LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get()));
    }

    @Benchmark
    static void intersectionOfFourSortedListsOf1000RandomIntsEachOne() {
        Utility.intersectionOfSortedLists(
                Utility.intersectionOfSortedLists(LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get()),
                Utility.intersectionOfSortedLists(LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SORTED_INTS_SUPPLIER.get()));
    }

    @Benchmark
    static void unionOfTwoSkipListsOf1000RandomIntsEachOne() {
        Utility.union(LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get());
    }

    @Benchmark
    static void intersectionOfTwoSkipListsOf1000RandomIntsEachOne() {
        Utility.intersection(LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get());
    }

    @Benchmark
    static void unionOfFourSkipListsOf1000RandomIntsEachOne() {
        Utility.union(
                LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(),
                LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get());
    }

    @Benchmark
    static void intersectionOfFourSkipListsOf1000RandomIntsEachOne() {
        Utility.intersection(
                LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(),
                LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get(), LIST_OF_1000_RANDOM_SKIP_LISTS_SUPPLIER.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void createFileAndWriteOnItIfDoesNotExist(boolean appendIfFileAlreadyExists) throws IOException {
        testWriteToFile(false, appendIfFileAlreadyExists);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void appendContentOnExistingFile(boolean appendIfFileAlreadyExists) throws IOException {
        testWriteToFile(true, appendIfFileAlreadyExists);
    }

    private void testWriteToFile(boolean createFileBeforeTest, boolean appendIfFileAlreadyExists) throws IOException {

        // test setup
        final String WHAT_TO_WRITE = "foo bar";
        final String CONTENT_ALREADY_ON_FILE = createFileBeforeTest ? "Already\npresent\0Onfile   " : "";
        final String EXPECTED_CONTENT_ON_FILE_AFTER_WRITING =
                (createFileBeforeTest && appendIfFileAlreadyExists ? CONTENT_ALREADY_ON_FILE : "") + WHAT_TO_WRITE;
        final File file = new File("foo");
        if (createFileBeforeTest) {
            if (!file.createNewFile()) {
                fail("File not created");
            }
            try (FileWriter fw = new FileWriter(file)) {
                fw.write(CONTENT_ALREADY_ON_FILE);
                fw.flush();
            }
        }

        // assert preconditions
        assert createFileBeforeTest == file.exists();
        assert !createFileBeforeTest || Files.readString(file.toPath()).equals(CONTENT_ALREADY_ON_FILE);

        // test
        Utility.writeToFile(WHAT_TO_WRITE, file, appendIfFileAlreadyExists);
        assertEquals(EXPECTED_CONTENT_ON_FILE_AFTER_WRITING, Files.readString(file.toPath()));

        // tear down
        if (!file.delete()) {
            throw new IOException("Error when deleting the file");
        }
    }

    @Test
    void tokenize() {
        Document document = new FakeDocument_LineOfAFile("a line  ", "  Content of A  line");
        assertEquals(
                Arrays.asList("a", "line", "content", "of", "a", "line"),   // conversion to lists because the test framework cannot compare arrays
                Arrays.asList(Utility.tokenize(document, Language.UNDEFINED, new SynchronizedSet<>())));
    }

    @ParameterizedTest
    @CsvSource({
            "Foo  bar, foo bar",
            "a*, a"
    })
    void normalize(String input, String expectedOutput) {
        assertEquals(expectedOutput, Utility.normalize(input, false, Language.UNDEFINED));
    }

    @ParameterizedTest
    @CsvSource({"1#5#5#2#1#7#-9, -9#1#2#5#7"})
    void sortAndRemoveDuplicates(String inputListAsString, String expectedSortedDistinctListAsString) {
        assertEquals(
                getListFromString(expectedSortedDistinctListAsString),
                Utility.sortAndRemoveDuplicates(getListFromString(inputListAsString)));
    }

    @NotNull
    private List<String> getListFromString(String inputListAsString) {
        return Arrays.asList(inputListAsString.split("#"));
    }

    @ParameterizedTest
    @CsvSource({"a#f#g#h, c#d#e#f#h#j, a#c#d#e#f#g#h#j"})
    void unionOfSortedLists(String inputList1AsString, String inputList2AsString, String expectedUnionListAsString) {
        assertEquals(
                getListFromString(expectedUnionListAsString),
                Utility.unionOfSortedLists(getListFromString(inputList1AsString), getListFromString(inputList2AsString)));
    }

    @Test
    void convertFromJsonToMap() throws JsonProcessingException {
        convertObjectWith3AttributesFromJsonToMap();
    }

    @ParameterizedTest
    @CsvSource({"a#f#g#h, c#d#e#f#h#j, f#h"})
    void intersectionOfSortedLists(String inputList1AsString, String inputList2AsString, String expectedIntersectionListAsString) {
        assertEquals(
                getListFromString(expectedIntersectionListAsString),
                Utility.intersectionOfSortedLists(getListFromString(inputList1AsString), getListFromString(inputList2AsString)));
    }

    @Test
    void convertToJson() throws JsonProcessingException {
        convertMapOf3EntriesToJson();
    }

    @ParameterizedTest
    @CsvSource({
            "a,a",
            "b\3a,b\3a#\3ab#ab\3#",// # at the end because otherwise the special char '\3' is not detected by CsvSource
            "abc,abc#bca#cab"
    })
    void getAllRotationsOf(String inputString, String expectedArrayAsString) {
        Set<String> expectedSetOfRotations = Arrays.stream(expectedArrayAsString.split("#")).collect(Collectors.toSet());
        Set<String> actualSetOfRotations = Arrays.stream(Utility.getAllRotationsOf(inputString)).collect(Collectors.toSet());
        assertEquals(expectedSetOfRotations, actualSetOfRotations);
    }

    @ParameterizedTest
    @CsvSource({
            "The cat is on the table, the[0#4]|cat[1]|is[2]|on[3]|table[5]",
            "Foo bar foo foo bar bar bar, foo[0#2#3]|bar[1#4#5#6]"
    })
    void tokenizeAndGetMapWithPositionsInDocument(String document, String expectedMapTokenToPositionsAsString) {
        // Use List instead of array in  tests (otherwise test assertion may fail)
        Map<String, List<Integer>> actualMapTokenToPositions =
                Utility.tokenizeAndGetMapWithPositionsInDocument(
                                new FakeDocument_LineOfAFile("", document), Language.UNDEFINED, new SynchronizedSet<>())
                        .entrySet()
                        .stream()
                        .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey(), Arrays.stream(entry.getValue()).boxed().toList()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, List<Integer>> expectedMapTokenToPositions =
                Arrays.stream(expectedMapTokenToPositionsAsString.split("\\|"))
                        .map(entryAsString -> {
                            assert entryAsString.length() > 2;
                            assert entryAsString.contains("[");
                            assert entryAsString.contains("]");
                            int indexOf1stBracket = entryAsString.indexOf("[");
                            int indexOf2ndBracket = entryAsString.indexOf("]");
                            assert indexOf1stBracket < indexOf2ndBracket;
                            String token = entryAsString.substring(0, indexOf1stBracket);
                            List<Integer> positions =
                                    Arrays.stream(
                                                    entryAsString.substring(indexOf1stBracket + 1, indexOf2ndBracket)
                                                            .split("#"))
                                            .map(Integer::parseInt)
                                            .toList();
                            return new AbstractMap.SimpleEntry<>(token, positions);
                        })
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(expectedMapTokenToPositions, actualMapTokenToPositions);
    }

    @ParameterizedTest
    @CsvSource({
            "{\"input\": [[0#1]#[0#1]]}, {\"expected\": [[0#0]#[0#1]#[1#0]#[1#1]]}",
            "{\"input\": [[0#1]#[0#1]#[0#1]]}, {\"expected\": [[0#0#0]#[0#0#1]#[0#1#0]#[0#1#1]#[1#0#0]#[1#0#1]#[1#1#0]#[1#1#1]]}"
    })
    void getCartesianProduct(String inputList, String expectedCartesianProduct) {
        Function<String, List<List<Integer>>> convertFromJson = json -> {
            try {
                return Utility.convertFromJsonToMap(json.replaceAll("#", ","))
                        .values()
                        .stream()
                        .flatMap(value -> ((List<?>) value)
                                .stream()
                                .map(innerList -> ((List<?>) innerList).stream().map(i -> (int) i).toList()))
                        .toList();
            } catch (JsonProcessingException e) {
                fail(e);
                return new ArrayList<>();
            }
        };
        List<List<Integer>> input = convertFromJson.apply(inputList);
        List<List<Integer>> expected = convertFromJson.apply(expectedCartesianProduct);
        List<List<Integer>> actual = Utility.getCartesianProduct(input);
        assertEquals(expected, actual);
    }

    @ParameterizedTest
    @CsvSource({
            "The, ",
            "car , car",
            "car?, car"

    })
    void normalizeEnglishWordsAndRemoveStopWords(String input, String expected) throws IOException {
        final String STOP_WORDS_EXCLUSION_PROP_NAME = "app.exclude_stop_words";
        var oldPropertyValue = AppProperties.getInstance()
                .set(STOP_WORDS_EXCLUSION_PROP_NAME, String.valueOf(true));
        assertEquals(expected, Utility.normalize(input, false, Language.ENGLISH));
        assert oldPropertyValue != null;
        AppProperties.getInstance().set(STOP_WORDS_EXCLUSION_PROP_NAME, oldPropertyValue);
    }

    @Test
    void readAllLines() {
        String TEXT = "Foo bar\nHello\r\nWorld";
        List<String> EXPECTED = Arrays.asList("Foo bar", "Hello", "World");
        List<String> ACTUAL = Utility.readAllLines(new ByteArrayInputStream(TEXT.getBytes()));
        assertEquals(EXPECTED, ACTUAL);
    }
}