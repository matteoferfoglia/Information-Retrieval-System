package it.units.informationretrieval.ir_boolean_model.factories;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

/**
 * Factory class to create a {@link Corpus}.
 * <strong>Notice:</strong> it is mandatory for classes that extend this
 * class to exhibit a (eventually private) no-args constructor.
 *
 * @param <T> The type of the actual document, i.e. the class extending {@link Document}
 *            of the documents composing the {@link Corpus} returned by {@link #createCorpus()}.
 * @author Matteo Ferfoglia.
 */
public abstract class CorpusFactory<T extends Document> {

    /**
     * The class representing the document.
     */
    @NotNull
    private final Class<T> documentClass;

    /**
     * The name for the collection.
     */
    @NotNull
    private final String corpusName;

    /**
     * Constructor.
     *
     * @param documentClass The class representing the document.
     * @param corpusName    The name for the collection.
     */
    protected CorpusFactory(@NotNull final Class<T> documentClass, @NotNull String corpusName) {
        this.documentClass = Objects.requireNonNull(documentClass);
        this.corpusName = Objects.requireNonNull(corpusName);
    }

    /**
     * @return the {@link Corpus} of documents.
     * @throws NoMoreDocIdsAvailable If, during the corpus creation, there are
     *                               no more available docIds. This might happen if
     *                               the corpus has too many documents.
     * @throws IOException           If errors occur while reading the corpus.
     */
    public abstract Corpus createCorpus() throws NoMoreDocIdsAvailable, IOException;

    /**
     * @return the actual class of the documents composing the corpus created by {@link #createCorpus()}.
     */
    public Class<T> getDocumentClass() {
        return documentClass;
    }

    /**
     * @return the name for the corpus created by this factory.
     */
    public String getCorpusName() {
        return corpusName;
    }
}
