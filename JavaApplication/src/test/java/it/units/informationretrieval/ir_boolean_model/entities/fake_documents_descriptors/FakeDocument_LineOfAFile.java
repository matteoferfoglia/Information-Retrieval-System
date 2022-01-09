package it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors;

import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentContent;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class FakeDocument_LineOfAFile extends Document {

    private static final String SAMPLE_DOCUMENT_CONTENT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    public FakeDocument_LineOfAFile(@NotNull final String title, @NotNull final DocumentContent content) {
        super(title, content, Language.UNDEFINED);
    }

    /**
     * Simplified constructor.
     */
    public FakeDocument_LineOfAFile(@NotNull final String title, @NotNull final String content) {
        this(title, new ArrayList<>() {{
            add(content);
        }});
    }

    /**
     * Simplified constructor.
     *
     * @param title    The title of this instance.
     * @param contents The contents of this instance.
     */
    public FakeDocument_LineOfAFile(@NotNull final String title, @NotNull final List<String> contents) {
        this(title, new DocumentContent(contents));
    }

    /**
     * Produces an arbitrary number of documents.
     *
     * @param numberOfDocumentsToProduce The desired number of documents to be produced.
     * @return the {@link List} of {@link Document} produced.
     */
    public static List<Document> produceDocuments(int numberOfDocumentsToProduce) {
        if (numberOfDocumentsToProduce < 0) {
            throw new IllegalArgumentException("Non-negative value expected but " + numberOfDocumentsToProduce + " found.");
        }
        return IntStream.range(0, numberOfDocumentsToProduce)
                .sequential()
                .mapToObj(i -> {
                    String title = String.valueOf(i + 1);
                    List<String> content = new ArrayList<>();
                    content.add(SAMPLE_DOCUMENT_CONTENT);
                    return (Document) new FakeDocument_LineOfAFile(title, new DocumentContent(content));
                })
                .toList();
    }

    @Override
    public int compareTo(@NotNull Document o) {
        return toString().compareTo(o.toString());
    }

}
