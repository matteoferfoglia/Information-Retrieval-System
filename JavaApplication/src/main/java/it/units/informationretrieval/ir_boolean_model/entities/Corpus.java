package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.entities.document.Document;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Class representing the corpus for the Information Retrieval System.
 *
 * @author Matteo Ferfoglia
 */
public class Corpus implements Serializable {

    /**
     * The corpus, saved as relationships (with a {@link java.util.Map})
     * between a {@link DocumentIdentifier} and the corresponding {@link Document}.
     */
    @NotNull
    private final Map<DocumentIdentifier, Document> corpus;

    /**
     * Constructor. Creates a corpus from a {@link Collection}.
     *
     * @param documents The {@link Collection} of {@link Document}s to use to create this instance.
     * @throws NoMoreDocIdsAvailable If no more {@link DocumentIdentifier}s
     *                               are available, hence the {@link Corpus} cannot be created.
     */
    public Corpus(@NotNull Collection<? extends Document> documents) throws NoMoreDocIdsAvailable {// TODO: test and benchmark

        AtomicReference<NoMoreDocIdsAvailable> eventuallyThrownException = new AtomicReference<>();

        this.corpus = Objects.requireNonNull(documents)
                .stream().unordered().parallel()
                .map(aDocument -> {
                    try {
                        return new Pair<>(new DocumentIdentifier(), aDocument);
                    } catch (NoMoreDocIdsAvailable noMoreDocIdsAvailable) {
                        eventuallyThrownException.set(noMoreDocIdsAvailable);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        if (eventuallyThrownException.get() != null) {
            throw eventuallyThrownException.get();
        }
    }

    /**
     * @param docIds The {@link List} of {@link DocumentIdentifier}s for which the
     *               corresponding {@link List} of {@link Document}s is desired.
     * @return the {@link List} of {@link Document}s in this {@link Corpus}
     * corresponding matching the parameter.
     */
    @NotNull
    public List<Document> getDocuments(@NotNull List<DocumentIdentifier> docIds) {  // TODO: test and benchmark
        return Objects.requireNonNull(docIds)
                .stream().unordered().parallel()
                .map(corpus::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Returns the head of this instance, i.e., the first documents,
     * ordered according to their {@link DocumentIdentifier}.
     * This method can be used for printing purposes (e.g., during testing).
     *
     * @param howMany The number of documents to return. If it exceeds the total
     *                number of available documents, the entire corpus is returned.
     * @return A string corresponding to the head of this corpus.
     */
    @NotNull
    public String head(int howMany) {   // TODO: test and benchmark
        int howManyDocsToReturn = Math.min(Math.max(howMany, 0), corpus.size());
        final Comparator<? super Map.Entry<DocumentIdentifier, Document>> keyComparator = Map.Entry.comparingByKey();
        return corpus.entrySet()
                .stream().sequential()
                .sorted(keyComparator)
                .limit(howManyDocsToReturn)
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining(System.lineSeparator()));
    }

    /**
     * Getter for the {@link #corpus}.
     *
     * @return the {@link #corpus}.
     */
    @NotNull
    public Map<DocumentIdentifier, Document> getCorpus() {
        return corpus;
    }

    @Override
    public String toString() {
        return corpus.toString();
    }

    /**
     * @return The size (number of documents) in this {@link Corpus}.
     */
    public int size() {
        return corpus.size();
    }
}