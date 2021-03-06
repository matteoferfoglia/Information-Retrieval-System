package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
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
     * The language of the corpus.
     */
    @NotNull
    private final Language language;

    /**
     * Constructor. Creates a corpus from a {@link Collection}.
     *
     * @param documents The {@link Collection} of {@link Document}s to use to create this instance.
     * @param language  The {@link Language} used in the corpus.
     * @throws NoMoreDocIdsAvailable If no more {@link DocumentIdentifier}s
     *                               are available, hence the {@link Corpus} cannot be created.
     */
    public Corpus(@NotNull Collection<Document> documents, @NotNull Language language) throws NoMoreDocIdsAvailable {
        this.language = Objects.requireNonNull(language);
        this.corpus = createCorpusFromDocumentCollectionAndGet(documents);
    }

    /**
     * Constructor. Creates a corpus from a {@link Collection}.
     *
     * @param documents The {@link Collection} of {@link Document}s to use to create this instance.
     * @throws NoMoreDocIdsAvailable If no more {@link DocumentIdentifier}s
     *                               are available, hence the {@link Corpus} cannot be created.
     */
    public Corpus(@NotNull Collection<Document> documents) throws NoMoreDocIdsAvailable {
        this.language = Objects.requireNonNull(Language.UNDEFINED);
        this.corpus = createCorpusFromDocumentCollectionAndGet(documents);
    }

    /**
     * Creates an empty corpus.
     *
     * @param language The {@link Language} used in the corpus.
     */
    protected Corpus(@NotNull Language language) {
        this.language = Objects.requireNonNull(language);
        this.corpus = new ConcurrentHashMap<>();
    }

    /**
     * Creates an empty corpus.
     */
    protected Corpus() {
        this.language = Language.UNDEFINED;
        this.corpus = new ConcurrentHashMap<>();
    }

    /**
     * Given a , creates and returns the
     * corresponding {@link Corpus} instance.
     *
     * @param documents The input {@link Collection} of {@link Document}s.
     * @return the {@link Corpus} instance.
     */
    @NotNull
    protected static ConcurrentMap<DocumentIdentifier, Document> createCorpusFromDocumentCollectionAndGet(
            @NotNull final Collection<? extends Document> documents) throws NoMoreDocIdsAvailable {

        AtomicReference<NoMoreDocIdsAvailable> eventuallyThrownException = new AtomicReference<>();

        ConcurrentMap<DocumentIdentifier, Document> corpus = documents
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
        } else {
            return corpus;
        }
    }

    /**
     * @param docIds The {@link List} of {@link DocumentIdentifier}s for which the
     *               corresponding {@link List} of {@link Document}s is desired.
     * @return the {@link List} of {@link Document}s in this {@link Corpus}
     * matching the input parameter.
     */
    @NotNull
    public List<Document> getDocumentsByDocIds(@NotNull List<DocumentIdentifier> docIds) {
        return docIds
                .stream()
                .distinct()
                .map(corpus::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * @param docId The {@link DocumentIdentifier} of the desired {@link Document}.
     * @return the {@link Document} in this {@link Corpus} corresponding to the
     * input {@link DocumentIdentifier} or null if it is not present.
     */
    @Nullable
    public Document getDocumentByDocId(@NotNull DocumentIdentifier docId) {
        return corpus.get(docId);
    }

    /**
     * @return the language used in this instance.
     */
    @NotNull
    public Language getLanguage() {
        return language;
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
    public String head(int howMany) {
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
     * Searches how many words (after normalization) are present in the title of
     * the given {@link Document} among the ones given as input parameter.
     * This method can be used for heuristics to give a higher rank
     * (when answering a query) to documents which have in the title terms in
     * common with a query (it would mean that some query terms are present in the
     * title, and so the document is probably more relevant).
     *
     * @param document   The {@link Document} for which to compute the number of normalized
     *                   tokens composing its title in common with the given input string list.
     * @param stringList The string list to be searched in the title for the matching.
     *                   <strong>No normalization</strong> is performed on the input
     *                   parameters.
     * @return the number of (normalized) words in common between
     * the title of this instance and the input parameters of the method.
     */
    public long howManyCommonNormalizedWordsWithDocTitle(
            @NotNull Document document, @NotNull List<String> stringList) {

        Function<Document, List<String>> getNormalizedTokensOfDocTitle = new Function<>() {
            /** For each {@link Document}, the {@link List} of normalized tokens
             * composing the {@link Document} title is cached, in the case it will
             * be asked again in the future. */
            @NotNull
            static final ConcurrentHashMap<Document, List<String>> listOfNormalizedTokensForDocument =
                    new ConcurrentHashMap<>();

            @Override
            public List<String> apply(Document document) {
                var normalizedTokens = listOfNormalizedTokensForDocument.get(document);
                if (normalizedTokens == null) {
                    normalizedTokens = document.getTitle() == null
                            ? new ArrayList<>(0)
                            : Arrays.stream(Utility.split(document.getTitle()))
                            .map(token -> Utility.preprocess(token, true, language))
                            .filter(Objects::nonNull)
                            .toList();
                    listOfNormalizedTokensForDocument.put(document, normalizedTokens);
                }
                return normalizedTokens;
            }
        };

        return getNormalizedTokensOfDocTitle.apply(document).stream().filter(stringList::contains).count();
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

    /**
     * @param document The document to check if present in this instance.
     * @return true if this instance contains the given input document.
     */
    public boolean contains(@NotNull Document document) {
        return corpus.containsValue(document);
    }

    /**
     * @return the documents contained in this instance.
     */
    public Collection<Document> getListOfDocuments() {
        return corpus.values();
    }
}