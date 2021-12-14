package it.units.informationretrieval.ir_boolean_model.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

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
}