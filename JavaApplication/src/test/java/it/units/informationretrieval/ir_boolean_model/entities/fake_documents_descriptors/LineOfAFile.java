package it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors;

import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentContent;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentContentRank;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentRankedSubcontent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;

public class LineOfAFile extends Document {

    private static final String PATH_TO_CORPUS = "/SampleCorpus.txt";
    private static final String SAMPLE_DOCUMENT_CONTENT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    public LineOfAFile(@NotNull String title, @NotNull DocumentContent content) {
        super(title, content);
    }

    /**
     * Loads document from the file {@link #PATH_TO_CORPUS}
     */
    public static List<Document> loadDocumentsFromFile() throws IOException, URISyntaxException {
        List<String> linesFromFile = Files.readAllLines(
                Path.of(Objects.requireNonNull(LineOfAFile.class.getResource(PATH_TO_CORPUS)).toURI()));
        return linesFromFile
                .stream().sequential()
                .map(aLine -> {
                    String title = aLine.length() > 0 ? aLine.substring(0, aLine.indexOf(' ')) : "";
                    List<DocumentRankedSubcontent> content = new ArrayList<>();
                    content.add(new LineRankedSubcontent(new LineRank(), aLine));
                    return (Document) new LineOfAFile(title, new DocumentContent(content));
                })
                .toList();
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
                    List<DocumentRankedSubcontent> content = new ArrayList<>();
                    content.add(new LineRankedSubcontent(new LineRank(), SAMPLE_DOCUMENT_CONTENT));
                    return (Document) new LineOfAFile(title, new DocumentContent(content));
                })
                .toList();
    }

    @Override
    public @NotNull LinkedHashMap<String, ?> toSortedMapOfProperties() {
        return new LinkedHashMap<>() {{
            assert getContent() != null;
            put("Content", getContent().getEntireTextContent());
        }};
    }

    @Override
    public int compareTo(@NotNull Document o) {
        return toString().compareTo(o.toString());
    }

    private static class LineRank implements DocumentContentRank {
        @Override
        public int compareTo(@NotNull DocumentContentRank o) {
            return 0;
        }
    }

    private static class LineRankedSubcontent extends DocumentRankedSubcontent {

        public LineRankedSubcontent(@NotNull DocumentContentRank rank, @NotNull String subcontent) {
            super(rank, subcontent);
        }

        @Override
        public int sum(@NotNull DocumentRankedSubcontent documentRankedSubcontent) {
            return 0;
        }

        @Override
        public int sum(@NotNull Collection<@NotNull DocumentRankedSubcontent> documentRankedSubcontents) {
            return 0;
        }
    }
}
