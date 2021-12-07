package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Properties;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An instance of this class is an Inverted Index for a {@link Corpus}
 * of documents.
 *
 * @author Matteo Ferfoglia
 */
public class InvertedIndex implements Serializable {

    /**
     * The inverted index, i.e., a {@link Map} having tokens as keys and a {@link Term}
     * as corresponding values, where the {@link Term} in the entry, if tokenized,
     * returns the corresponding key.
     */
    @NotNull
    private final Map<String, Term> invertedIndex;   // each Term has its own posting list

    /**
     * The (reference to the) {@link Corpus} on which indexing is done.
     */
    @NotNull
    private final Corpus corpus;

//    /** The phonetic hash. */  // TODO : implement phoneticHash
//    @NotNull
//    private final ConcurrentHashMap<String, List<Term>> phoneticHash;

    /**
     * Constructor. Creates the instance and indexes the given {@link Corpus}.
     *
     * @param corpus The {@link Corpus} to be indexed.
     */
    public InvertedIndex(@NotNull final Corpus corpus) {    // TODO: test and benchmark

        this.corpus = Objects.requireNonNull(corpus);

        AtomicLong numberOfAlreadyProcessedDocuments = new AtomicLong(0L);
        Runnable indexingProgressPrinterInterrupter =
                printIndexingProgressAndGetTheRunnableToInterruptPrinting(corpus, numberOfAlreadyProcessedDocuments);

        try {
            invertedIndex = indexCorpusAndGet(corpus, numberOfAlreadyProcessedDocuments);
        } finally {
            // Join the thread used to print the indexing progress
            indexingProgressPrinterInterrupter.run();
            System.out.println("\nIndexing ended");
        }
    }

    protected static Map<String, Term> indexCorpusAndGet(
            @NotNull Corpus corpus, @NotNull AtomicLong numberOfAlreadyProcessedDocuments) {    // TODO: test and benchmark

        Predicate<Map.Entry<DocumentIdentifier, Document>> documentContentNotNullPredicate =
                entry -> entry != null
                        && entry.getKey() != null
                        && entry.getValue() != null
                        && entry.getValue().getContent() != null;

        Map<String, Term> targetDataStructureForInvertedIndex = createEmptyInvertedIndex();
        targetDataStructureForInvertedIndex.putAll(
                corpus.getCorpus()
                        .entrySet()
                        .stream().unordered().parallel()
                        .filter(documentContentNotNullPredicate)
                        .map(InvertedIndex::getEntrySetOfTokensAndCorrespondingTermsFromADocument)
                        .peek(ignored -> numberOfAlreadyProcessedDocuments.getAndIncrement()/*TODO: threads must wait to increase this value: needed?*/)
                        .flatMap(Collection::stream /*outputs all entries from all the documents*/)
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        Term::merge /* Merge terms with the same token */)));

        return targetDataStructureForInvertedIndex;
    }

    @NotNull
    protected static Set<Map.Entry<String, Term>> getEntrySetOfTokensAndCorrespondingTermsFromADocument(
            @NotNull Map.Entry<@NotNull DocumentIdentifier, @NotNull Document> entryFromCorpusRepresentingOneDocument) {    // TODO: test and benchmark

        DocumentIdentifier docIdThisDocument = entryFromCorpusRepresentingOneDocument.getKey();
        Document document = entryFromCorpusRepresentingOneDocument.getValue();
        List<String> tokensFromCurrentDocument = Utility.tokenize(document);   // TODO : tokenization should return as a map also the position where the token is found in the document (for phrase query)

        return tokensFromCurrentDocument
                .stream().unordered().parallel()
                .map(aToken ->
                        new AbstractMap.SimpleEntry<>(
                                aToken,
                                new Term(new PostingList(new Posting(docIdThisDocument))/*TODO: resee this*/, aToken)))
                .collect(
                        Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (t1, t2) -> t1/*ignore duplicate tokens in the same document. TODO: resee this for positional indexing*/))
                .entrySet();
    }

    @NotNull
    private static Map<String, Term> createEmptyInvertedIndex() {
        final Map<String, Term> invertedIndex;
        short invertedIndexType = Short.parseShort(Properties.appProperties.getProperty("index.dataStructure.type"));
        switch (invertedIndexType) {
            case 1 -> invertedIndex = new ConcurrentHashMap<>();
            case 2 -> invertedIndex = new PatriciaTrie<>();
            default ->    // 0 or anything else
                    invertedIndex = new Hashtable<>();
        }
        return invertedIndex;
    }

    @NotNull
    private Runnable printIndexingProgressAndGetTheRunnableToInterruptPrinting(@NotNull Corpus corpus, AtomicLong numberOfAlreadyProcessedDocuments) {
        AtomicReference<Double> progressValue = new AtomicReference<>(0d);   // used only to show the indexing progress
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Thread progressControllerThread = new Thread(() -> {
            final double EPSILON = 0.001;
            double oldProgressValue = progressValue.get();
            progressValue.set(Math.round((0.0 + numberOfAlreadyProcessedDocuments.get()) / corpus.size() * 10000) / 100.0);
            String currentProgress = "";
            if (progressValue.get() - oldProgressValue > EPSILON) {
                currentProgress = progressValue.toString();
                System.out.print(currentProgress + " % \t ");
            }
            if (numberOfAlreadyProcessedDocuments.get() == corpus.size() && !currentProgress.equals(progressValue.toString())) {
                // Avoid duplicate prints
                System.out.print("100 % \t ");
            }
        });
        System.out.println("Indexing started");
        final int DELAY_PROGRESS_CONTROLLER = 5;    // seconds
        scheduler.scheduleWithFixedDelay(progressControllerThread, DELAY_PROGRESS_CONTROLLER, DELAY_PROGRESS_CONTROLLER, TimeUnit.SECONDS);

        return () -> {
            scheduler.shutdown();
            try {
                short awaitTermination = 1;    // s
                if (!scheduler.awaitTermination(awaitTermination, TimeUnit.SECONDS)) {
                    System.err.println("Still waiting for thread termination after " + awaitTermination + " s.");
                }
            } catch (InterruptedException e) {
                System.err.println("Indexing progress controller thread not joined.");
                e.printStackTrace();
            }
        };
    }

    /**
     * @return the dictionary as sorted {@link java.util.List} of {@link String}s (the terms).
     */
    public List<String> getDictionary() {   // TODO: test and benchmark
        return invertedIndex.keySet()
                .stream().sequential()
                .sorted()
                .toList();
    }

    /**
     * Getter for {@link #corpus}.
     *
     * @return The {@link Corpus} on which indexing was done for this instance.
     */
    public @NotNull Corpus getCorpus() {
        return corpus;
    }

    /**
     * Getter for a {@link PostingList} associated with a
     * {@link Term} in this instance.
     *
     * @param normalizedToken The normalized term.
     * @return The {@link PostingList} associated with the desired term or null
     * if it is not found in this {@link InvertedIndex}.
     */
    @NotNull
    public final PostingList getPostingListForToken(String normalizedToken) {   // TODO: test and benchmark
        Term t = invertedIndex.get(normalizedToken);
        return t == null ? new PostingList() : t.getPostingList();
    }
}