package it.units.informationretrieval.ir_boolean_model.document_descriptors;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentContent;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
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

    //region regex for data extraction from text
    // -----------------------------------------------------------------------------------------------------------------

    // Regex are used to extract data from the text composing the collection.
    // For each region of the text to be extracted, a static Pair instance is
    // provided: the key is the regex pattern for the region and the value is
    // the number of the capturing group in which the text to be extracted  is
    // present.

    /**
     * The regex and the corresponding capturing group to extract the {@link #docNumber}
     * from a Cranfield's collection document given as string.
     */
    @NotNull
    private static final Pair<Pattern, Integer> REGEX_DOC_NUMBER =
            new Pair<>(Pattern.compile("(?s)^\\.I (\\d+)\\s+.*?\\.T\\s+"), 1);
    /**
     * The regex and the corresponding capturing group to extract the title from a
     * Cranfield's collection document given as string.
     */
    @NotNull
    private static final Pair<Pattern, Integer> REGEX_TITLE =
            new Pair<>(Pattern.compile(REGEX_DOC_NUMBER.getKey().pattern() + "(.*?)\\s+\\.A\\s+"), 2);
    /**
     * The regex and the corresponding capturing group to extract the {@link #authors} from a
     * Cranfield's collection document given as string.
     */
    @NotNull
    private static final Pair<Pattern, Integer> REGEX_AUTHORS =
            new Pair<>(Pattern.compile(REGEX_TITLE.getKey().pattern() + "(.*?)\\s+\\.B\\s+"), 3);
    /**
     * The regex and the corresponding capturing group to extract the {@link #source} from a
     * Cranfield's collection document given as string.
     */
    @NotNull
    private static final Pair<Pattern, Integer> REGEX_SOURCE =
            new Pair<>(Pattern.compile(REGEX_AUTHORS.getKey().pattern() + "(.*?)\\s+\\.W\\s+"), 4);
    /**
     * The regex and the corresponding capturing group to extract the actual content from a
     * Cranfield's collection document given as string.
     */
    @NotNull
    private static final Pair<Pattern, Integer> REGEX_ACTUAL_CONTENT =
            new Pair<>(Pattern.compile(REGEX_SOURCE.getKey().pattern() + "(.*?)\\s+((\\.I\\s+)|\\z)"), 5);

    // -----------------------------------------------------------------------------------------------------------------
    // endregion regex

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

    public static Corpus createCorpus() throws URISyntaxException, IOException, NoMoreDocIdsAvailable {
        var docs = getDocuments();
        BiFunction<Pair<Pattern, Integer>, String, String> dataExtractor =
                (patternToExtractAndCapturingGroup, inputText) -> {
                    final var pattern = patternToExtractAndCapturingGroup.getKey();
                    final var capturingGroup = patternToExtractAndCapturingGroup.getValue();
                    var matcher = pattern.matcher(inputText);
                    if (matcher.find()) {
                        return matcher.group(capturingGroup);
                    } else {
                        return "";  // pattern not present
                    }
                };
        return new Corpus(
                docs.stream()   // extract and print doc numbers (just to show)
                        .map(docAsStr -> {
                            int docNumber = Integer.parseInt(dataExtractor.apply(REGEX_DOC_NUMBER, docAsStr));
                            String title = dataExtractor.apply(REGEX_TITLE, docAsStr);
                            String authors = dataExtractor.apply(REGEX_AUTHORS, docAsStr);
                            String source = dataExtractor.apply(REGEX_SOURCE, docAsStr);
                            String actualContent = dataExtractor.apply(REGEX_ACTUAL_CONTENT, docAsStr);
                            return (Document)
                                    new CranfieldDocument(docNumber, title, authors, source, actualContent);
                        })
                        .toList());

    }

    public static void main(String[] args) throws URISyntaxException, IOException, NoMoreDocIdsAvailable {
        var corpus = createCorpus();
        var m = corpus.getCorpus();
        m.values().stream().map(Document::getContentAsString).forEach(System.out::println);
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
                .filter(s -> Pattern.matches(REGEX_DOC_NUMBER.getKey().pattern() + ".*", s))  // each doc must start following this regex
                .count() == EXPECTED_CORPUS_SIZE;

        return corpusAsListOfDocs;
    }

    @Override
    public int compareTo(@NotNull Document o) {
        return o instanceof CranfieldDocument cranfieldDocument ? docNumber - cranfieldDocument.docNumber : -1;
    }
}
