package it.units.informationretrieval.ir_boolean_model.user_defined_contents.cranfield;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentContent;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Class representing a document taken from the Cranfield collection.
 */
public class CranfieldDocument extends Document {

    /**
     * The {@link Language}.
     */
    public static final Language LANGUAGE = Language.ENGLISH;

    //region regex for data extraction from text
    // -----------------------------------------------------------------------------------------------------------------

    // Regex are used to extract data from the text composing the collection.
    // Each region of a document (docNumber, title, ...) can be extracted from
    // the i-th capturing group of the regex given for the entire document.
    // Capturing group numbers are saved in fields in this region.

    /**
     * The number of the capturing group of the {@link #docNumber} of a Cranfield's
     * collection document, extracted with {@link #REGEX_ENTIRE_DOCUMENT}.
     */
    private static final int CAPTURING_GROUP_REGEX_DOC_NUMBER = 1;
    /**
     * The number of the capturing group of the title of a Cranfield's
     * collection document, extracted with {@link #REGEX_ENTIRE_DOCUMENT}.
     */
    private static final int CAPTURING_GROUP_REGEX_TITLE = 2;
    /**
     * The number of the capturing group of the {@link #authors} of a Cranfield's
     * collection document, extracted with {@link #REGEX_ENTIRE_DOCUMENT}.
     */
    private static final int CAPTURING_GROUP_REGEX_AUTHORS = 3;
    /**
     * The number of the capturing group of the {@link #source} of a Cranfield's
     * collection document, extracted with {@link #REGEX_ENTIRE_DOCUMENT}.
     */
    private static final int CAPTURING_GROUP_REGEX_SOURCE = 4;
    /**
     * The number of the capturing group of the actual content of a Cranfield's
     * collection document, extracted with {@link #REGEX_ENTIRE_DOCUMENT}.
     */
    private static final int CAPTURING_GROUP_REGEX_ACTUAL_CONTENT = 5;

    // -----------------------------------------------------------------------------------------------------------------
    // endregion regex
    /**
     * The regex for a Cranfield's collection document.
     */
    @NotNull
    private final static Pattern REGEX_ENTIRE_DOCUMENT = Pattern.compile(
            "(?s)"     // multiline matching, you can use Pattern.DOTALL flag instead
                    + "^\\.I (\\d+)\\s+.*?\\.T\\s*" // (\d+)  matches the doc number    (group 1 of the overall regex)
                    + "(.*?)\\s+\\.A\\s*"           // (.*?)  matches the doc title     (group 2 of the overall regex)
                    + "(.*?)\\s+\\.B\\s*"           // (.*?)  matches the doc authors   (group 3 of the overall regex)
                    + "(.*?)\\s+\\.W\\s*"           // (.*?)  matches the doc source    (group 4 of the overall regex)
                    + "(.*?)\\s*((\\.I\\s*)|\\z)"); // (.*?)  matches the doc actual content (group 5 of the overall regex), matching goes on until either the next doc beginning or the end of file
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
        super(title, new DocumentContent(Arrays.asList(title, authors, source, actualContent)), LANGUAGE);
        this.docNumber = docNumber;
        this.authors = authors;
        this.source = source;
    }

    static Corpus createCorpus() throws URISyntaxException, IOException, NoMoreDocIdsAvailable {
        return new Corpus(
                getDocuments()
                        .parallelStream()
                        .map(docAsStr -> {
                            var matcher = REGEX_ENTIRE_DOCUMENT.matcher(docAsStr);
                            if (matcher.find()) {
                                int docNumber = Integer.parseInt(matcher.group(CAPTURING_GROUP_REGEX_DOC_NUMBER));
                                String title = matcher.group(CAPTURING_GROUP_REGEX_TITLE);
                                String authors = matcher.group(CAPTURING_GROUP_REGEX_AUTHORS);
                                String source = matcher.group(CAPTURING_GROUP_REGEX_SOURCE);
                                String actualContent = matcher.group(CAPTURING_GROUP_REGEX_ACTUAL_CONTENT);
                                return (Document)
                                        new CranfieldDocument(docNumber, title, authors, source, actualContent);
                            } else {
                                throw new IllegalArgumentException("Not matching the pattern for document:"
                                        + System.lineSeparator() + "\tPattern: " + REGEX_ENTIRE_DOCUMENT
                                        + System.lineSeparator() + "\tDocument: "
                                        + System.lineSeparator() + docAsStr.replaceAll("\\n", "\t\n")/*only for printing*/);
                            }
                        })
                        .toList(), LANGUAGE);

    }

    /**
     * @return a {@link List} in which each element is a document from
     * the collection, represented as string.
     */
    @NotNull
    private static List<String> getDocuments() {

        final String PATH_TO_FILE_WITH_DOCS = "cran.all.1400";
        String allLinesFromFile = Utility.readAllLines(Objects.requireNonNull(
                        CranfieldDocument.class.getResourceAsStream(PATH_TO_FILE_WITH_DOCS)))
                .stream().collect(Collectors.joining(System.lineSeparator()));

        final String START_OF_DOC = ".I ";
        var corpusAsListOfDocs = Arrays.stream(allLinesFromFile.split(START_OF_DOC))
                .filter(s -> !s.isBlank())    //having used .split(), first element is empty
                .map(s -> START_OF_DOC + s)
                .toList();

        final int EXPECTED_CORPUS_SIZE = 1400;
        assert corpusAsListOfDocs.size() == EXPECTED_CORPUS_SIZE;
        assert corpusAsListOfDocs.stream()
                .map(REGEX_ENTIRE_DOCUMENT::matcher)
                .filter(Matcher::matches)
                .count() == EXPECTED_CORPUS_SIZE;   // assert that each document follows the correct pattern

        return corpusAsListOfDocs;
    }

    /**
     * @return the document number of this instance, as assigned by the input Cranfield's collection.
     */
    public int getDocNumber() {
        return docNumber;
    }

    @Override
    public int compareTo(@NotNull Document o) {
        return o instanceof CranfieldDocument cranfieldDocument ? docNumber - cranfieldDocument.docNumber : -1;
    }
}
