package it.units.informationretrieval.ir_boolean_model.document_descriptors;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentContent;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a document taken from the Cranfield collection.
 */
public class CranfieldDocument extends Document {

    /**
     * The folder name in the resources where files about this collection
     * are available.
     */
    private static final String RESOURCE_FOLDER_NAME_CRANFIELD = "cranfield_collection";

    /**
     * When using regexes to extract data from each document,
     * data can be extracted from the capturing group specified
     * by this field.
     */
    private static final int REGEX_CAPTURING_GROUP_WITH_DATA = 1;
    /**
     * The regex matching the document number (it
     * can be captured from group {@link #REGEX_CAPTURING_GROUP_WITH_DATA}).
     */
    @NotNull
    private static final Pattern REGEX_DOC_NUMBER = Pattern.compile("^\\.I (\\d+)\\n");
    /**
     * The number of the document in the collection.
     */
    private final int docNumber;
    /**
     * The authors for the document, represented in a single string.
     */
    @NotNull
    private final String authors;
    /**
     * The source of the document.
     */
    @NotNull
    private final String source;

    /**
     * Constructor.
     *
     * @param docNumber     The number of the document in the collection.
     * @param title         The authors for the document, represented in a single string.
     * @param authors       The source of the document.
     * @param source        The source of the document.
     * @param actualContent The actual content of the document (without title or other fields).
     */
    public CranfieldDocument(
            int docNumber, @NotNull String title, @NotNull String authors,
            @NotNull String source, @NotNull String actualContent) {
        super(title, new DocumentContent(Arrays.asList(title, authors, source, actualContent)), Language.ENGLISH);
        this.docNumber = docNumber;
        this.authors = authors;
        this.source = source;
    }

    public static Corpus createCorpus() throws URISyntaxException, IOException {
        var docs = getDocuments();
        docs.stream()   // extract and print doc numbers (just to show)
                .map(REGEX_DOC_NUMBER::matcher)
                .peek(Matcher::find)
                .map(matcher -> matcher.group(REGEX_CAPTURING_GROUP_WITH_DATA))
                .forEach(System.out::println);
        // TODO parse documents (for it is just a draft)

        throw new UnsupportedOperationException();
    }

    public static void main(String[] args) throws URISyntaxException, IOException {
        createCorpus();
    }

    /**
     * @return a {@link List} in which each element is a document from
     * the collection, represented as string.
     */
    @NotNull
    private static List<String> getDocuments() throws IOException, URISyntaxException {

        final String PATH_TO_FILE_WITH_DOCS = RESOURCE_FOLDER_NAME_CRANFIELD + "/cran.all.1400";
        String allLinesFromFile = Files.readString(Path.of(Objects.requireNonNull(
                CranfieldDocument.class.getResource(PATH_TO_FILE_WITH_DOCS)).toURI()));

        final String START_OF_DOC = ".I ";
        var corpusAsListOfDocs = Arrays.stream(allLinesFromFile.split(START_OF_DOC))
                .filter(s -> !s.isBlank())    //having used .split(), first element is empty
                .map(s -> START_OF_DOC + s)
                .toList();

        final int EXPECTED_CORPUS_SIZE = 1400;
        assert corpusAsListOfDocs.size() == EXPECTED_CORPUS_SIZE;
        assert corpusAsListOfDocs.stream()
                .filter(s -> Pattern.matches(REGEX_DOC_NUMBER.pattern() + ".*", s))  // each doc must start following this regex
                .count() == EXPECTED_CORPUS_SIZE;

        return corpusAsListOfDocs;
    }

    @Override
    public int compareTo(@NotNull Document o) {
        return o instanceof CranfieldDocument cranfieldDocument ? docNumber - cranfieldDocument.docNumber : -1;
    }
}
