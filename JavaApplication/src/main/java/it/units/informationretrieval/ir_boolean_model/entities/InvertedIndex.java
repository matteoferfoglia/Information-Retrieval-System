package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Soundex;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
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
     * The {@link Map} collecting as keys {@link DocumentIdentifier}s and ad
     * corresponding value the {@link Set} of {@link Posting} referring to
     * that {@link DocumentIdentifier}. This field can be used to answer
     * NOT queries when also positions of words in {@link Document} matters
     * (because positions are saved in {@link Posting}s).
     */
    @NotNull
    private final ConcurrentHashMap<DocumentIdentifier, Set<Posting>> postingsByDocId;

    /**
     * The (reference to the) {@link Corpus} on which indexing is done.
     */
    @NotNull
    private final Corpus corpus;

    /**
     * The phonetic hash.
     */
    @NotNull
    private final ConcurrentHashMap<String, List<Term>> phoneticHash;   // TODO: test and use phonetic hash for queries

    /**
     * Constructor. Creates the instance and indexes the given {@link Corpus}.
     *
     * @param corpus The {@link Corpus} to be indexed.
     */
    public InvertedIndex(@NotNull final Corpus corpus) {

        this.corpus = corpus;
        this.postingsByDocId = new ConcurrentHashMap<>(corpus.size());

        AtomicLong numberOfAlreadyProcessedDocuments = new AtomicLong(0L);
        Runnable indexingProgressPrinterInterrupter =
                printIndexingProgressAndGetTheRunnableToInterruptPrinting(corpus, numberOfAlreadyProcessedDocuments);

        try {
            invertedIndex = indexCorpusAndGet(corpus, numberOfAlreadyProcessedDocuments);
            phoneticHash = invertedIndex.entrySet()
                    .stream().unordered().parallel()
                    .map(stringTermEntry -> {
                        var termList = new ArrayList<Term>();
                        termList.add(stringTermEntry.getValue());
                        return new Pair<>(Soundex.getPhoneticHash(stringTermEntry.getKey()), termList);
                    })
                    .collect(Collectors.toConcurrentMap(
                            Map.Entry::getKey,
                            AbstractMap.SimpleEntry::getValue,
                            (a, b) -> {
                                a.addAll(b);
                                return a;
                            },
                            ConcurrentHashMap::new));
        } finally {
            // Join the thread used to print the indexing progress
            indexingProgressPrinterInterrupter.run();
            System.out.println("\nIndexing ended");
        }
    }

    /**
     * Creates the data structure hosting an empty inverted index.
     *
     * @return an empty data structure which can host an inverted index.
     */
    @NotNull
    private static Map<String, Term> createEmptyInvertedIndex() {
        final Map<String, Term> invertedIndex;
        short invertedIndexType = 0;
        try {
            invertedIndexType = Short.parseShort(Objects.requireNonNull(
                    AppProperties.getInstance().get("index.dataStructure.type")));
        } catch (IOException e) {
            Logger.getLogger(InvertedIndex.class.getCanonicalName())
                    .log(Level.SEVERE, "Error reading data-structure type.", e);
        }
        switch (invertedIndexType) {
            case 1 -> invertedIndex = new ConcurrentHashMap<>();
            case 2 -> invertedIndex = new PatriciaTrie<>();
            default ->    // 0 or anything else
                    invertedIndex = new Hashtable<>();
        }
        return invertedIndex;
    }

    /**
     * Indexes the given {@link Corpus} and return the index as {@link Map}.
     *
     * @param corpus                            The {@link Corpus} to be indexed.
     * @param numberOfAlreadyProcessedDocuments The reference to the number of already
     *                                          processed documents (can be used by another
     *                                          thread to print the indexing progress).
     * @return The result of indexing represented as {@link Map} having a {@link DocumentIdentifier}
     * as key and the {@link Term} as corresponding value.
     */
    protected Map<String, Term> indexCorpusAndGet(
            @NotNull Corpus corpus, @NotNull AtomicLong numberOfAlreadyProcessedDocuments) {
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
                        .map(this::getEntrySetOfTokensAndCorrespondingTermsFromADocument)
                        .peek(ignored -> numberOfAlreadyProcessedDocuments.getAndIncrement()/*TODO: threads must wait to increase this value: needed?*/)
                        .flatMap(Collection::stream /*outputs all entries from all the documents*/)
                        .collect(
                                Collectors.toConcurrentMap(
                                        Map.Entry::getKey,
                                        Map.Entry::getValue,
                                        Term::merge /* Merge terms with the same token */)));

        return targetDataStructureForInvertedIndex;
    }

    /**
     * Maps token (obtained from tokenization preprocessing) to correspondent {@link Term}s.
     *
     * @param entryFromCorpusRepresentingOneDocument A document from the {@link Corpus} represented as
     *                                               entry of {@link DocumentIdentifier} (as key) and
     *                                               the actual {@link Document} (as value).
     * @return the entry set having as key a token and as value its correspondent {@link Term}.
     */
    @NotNull
    private Set<Map.Entry<String, Term>> getEntrySetOfTokensAndCorrespondingTermsFromADocument(
            @NotNull Map.Entry<@NotNull DocumentIdentifier, @NotNull Document> entryFromCorpusRepresentingOneDocument) {
        DocumentIdentifier docIdThisDocument = entryFromCorpusRepresentingOneDocument.getKey();
        Document document = entryFromCorpusRepresentingOneDocument.getValue();
        Map<String, int[]> tokensFromCurrentDocument = Utility.tokenizeAndGetMapWithPositionsInDocument(document);

        return tokensFromCurrentDocument
                .entrySet()
                .stream().unordered().parallel()
                .map(tokenAndPositions -> {
                    Posting posting = new Posting(docIdThisDocument, tokenAndPositions.getValue());
                    var setOfPostingsWithSameDocId = postingsByDocId.get(docIdThisDocument);
                    final int INITIAL_SET_CAPACITY = 50;
                    if (setOfPostingsWithSameDocId == null) {
                        setOfPostingsWithSameDocId = ConcurrentHashMap.newKeySet(INITIAL_SET_CAPACITY);
                        postingsByDocId.put(docIdThisDocument, setOfPostingsWithSameDocId);
                    }
                    boolean postingWasNotAlreadyPresent = setOfPostingsWithSameDocId.add(posting);
                    assert postingWasNotAlreadyPresent;
                    return new AbstractMap.SimpleEntry<>(
                            tokenAndPositions.getKey(),
                            new Term(new PostingList(posting), tokenAndPositions.getKey()));
                })
                .collect(
                        Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (t1, t2) -> {
                                    throw new IllegalStateException("Duplicated keys should not be present, but were");
                                }))
                .entrySet();
    }

    /**
     * @return the {@link Runnable} handling the print of the progress bar for the indexing process.
     */
    @NotNull
    private Runnable printIndexingProgressAndGetTheRunnableToInterruptPrinting(
            @NotNull Corpus corpus, AtomicLong numberOfAlreadyProcessedDocuments) {

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
    public List<String> getDictionary() {
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
    public final PostingList getPostingListForToken(String normalizedToken) {
        Term t = invertedIndex.get(normalizedToken);
        return t == null ? new PostingList() : t.getPostingList();
    }

    /**
     * @return the {@link Set} of all {@link DocumentIdentifier}s currently present.
     */
    @NotNull
    public Set<DocumentIdentifier> getAllDocIds() {
        return postingsByDocId.keySet();
    }

    /**
     * @param docId The {@link DocumentIdentifier} to find.
     * @return the {@link Set} (eventually empty) with all {@link Posting}s
     * having the given {@link DocumentIdentifier}.
     */
    @NotNull
    public Set<Posting> getPostingList(@NotNull DocumentIdentifier docId) {
        var results = postingsByDocId.get(docId);
        return results == null ? ConcurrentHashMap.newKeySet(0) : results;
    }
}