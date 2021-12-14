package it.units.informationretrieval.ir_boolean_model.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class UtilityTest {

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
                new ArrayList<>() {{
                    addAll(Arrays.asList("a", "line", "content", "of", "a", "line"));
                }},
                Utility.tokenize(document));
    }

    @ParameterizedTest
    @CsvSource({"Foo  bar, foo bar"})
    void normalize(String input, String expectedOutput) {
        assertEquals(expectedOutput, Utility.normalize(input));
    }

    @Test
    void convertFromJsonToMap() throws JsonProcessingException {
        final String JSON_SAMPLE = "{\"a\": 1, \"b\":\"5\", \"c\": \"foo  bar \"}";
        final Map<String, ?> EXPECTED_MAP = new HashMap<>() {{
            put("a", 1);
            put("b", "5");
            put("c", "foo  bar ");
        }};
        assertEquals(EXPECTED_MAP, Utility.convertFromJsonToMap(JSON_SAMPLE));
    }

    @Test
    void convertToJson() throws JsonProcessingException {
        final Map<String, ?> SAMPLE_MAP = new HashMap<>() {{
            put("a", 1);
            put("b", "5");
            put("c", "foo  bar ");
        }};
        final String EXPECTED_JSON = "{\"a\":1,\"b\":\"5\",\"c\":\"foo  bar \"}";
        assertEquals(EXPECTED_JSON, Utility.convertToJson(SAMPLE_MAP));
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

    @ParameterizedTest
    @CsvSource({"a#f#g#h, c#d#e#f#h#j, f#h"})
    void intersectionOfSortedLists(String inputList1AsString, String inputList2AsString, String expectedIntersectionListAsString) {
        assertEquals(
                getListFromString(expectedIntersectionListAsString),
                Utility.intersectionOfSortedLists(getListFromString(inputList1AsString), getListFromString(inputList2AsString)));
    }
}