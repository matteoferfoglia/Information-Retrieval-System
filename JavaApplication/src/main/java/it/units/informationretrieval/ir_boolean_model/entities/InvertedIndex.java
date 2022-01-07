package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Soundex;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;
import skiplist.SkipList;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An instance of this class is an Inverted Index for a {@link Corpus}
 * of documents.
 *
 * @author Matteo Ferfoglia
 */
public class InvertedIndex implements Serializable {

    /**
     * Wildcard to indicate 0, 1 or more characters,
     * used to answer wildcard queries.
     */
    @NotNull
    public static final String WILDCARD = "*";
    /**
     * Same as {@link #WILDCARD}, but escaped to be used in wildcard queries.
     */
    @NotNull
    public static final String ESCAPED_WILDCARD_FOR_REGEX = "\\" + WILDCARD;
    /**
     * The symbol which indicates the end of a word, used in {@link #permutermIndex}.
     */
    @NotNull
    private static final String END_OF_WORD = "\3";
    /**
     * The inverted index, i.e., a {@link Map} having tokens as keys and a {@link Term}
     * as corresponding values, where the {@link Term} in the entry, if tokenized,
     * returns the corresponding key.
     */
    @NotNull
    private final ConcurrentMap<String, Term> invertedIndex;   // each Term has its own posting list

    /**
     * The {@link Map} collecting as keys {@link DocumentIdentifier}s and ad
     * corresponding value the {@link Set} of {@link Posting} referring to
     * that {@link DocumentIdentifier}. This field can be used to answer
     * NOT queries when also positions of words in {@link Document} matters
     * (because positions are saved in {@link Posting}s).
     */
    @NotNull
    private final ConcurrentMap<DocumentIdentifier, Set<Posting>> postingsByDocId;

    /**
     * The (reference to the) {@link Corpus} on which indexing is done.
     */
    @NotNull
    private final Corpus corpus;

    /**
     * The phonetic hash.
     */
    @NotNull
    private final ConcurrentMap<String, List<Term>> phoneticHashes;

    /**
     * The permuterm index, having as keys all rotations of all tokens in the dictionary
     * followed by the EndOfWord symbol and as corresponding values the corresponding
     * (un-rotated) token from the dictionary.
     */
    @NotNull
    private final PatriciaTrie<String> permutermIndex;

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
                printIndexingProgressAndGetTheRunnableToInterruptPrinting(corpus.size(), numberOfAlreadyProcessedDocuments);

        try {
            invertedIndex = indexCorpusAndGet(corpus, numberOfAlreadyProcessedDocuments);
            phoneticHashes = getPhoneticHashesOfDictionary();
            permutermIndex = createPermutermIndexAndGet();
        } finally {
            // Join the thread used to print the indexing progress
            indexingProgressPrinterInterrupter.run();
            System.out.println("\nIndexing ended");
        }
    }

    /**
     * @return a copy of {@link #permutermIndex}.
     */
    public PatriciaTrie<String> getCopyOfPermutermIndex() {
        return permutermIndex.entrySet().stream().sequential()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new IllegalStateException("No duplicates should be present");
                        },
                        PatriciaTrie::new));
    }

    @NotNull
    private PatriciaTrie<String> createPermutermIndexAndGet() {
        return getDictionary()
                .stream().unordered().parallel()
                .flatMap(strFromDictionary -> {
                    String str = strFromDictionary + END_OF_WORD;
                    return Arrays.stream(Utility.getAllRotationsOf(str))
                            .map(aRotation -> new Pair<>(aRotation, strFromDictionary));
                })
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (a, b) -> {
                            throw new IllegalStateException("No duplicates should be present");
                        },
                        PatriciaTrie::new));
    }

    @NotNull
    private ConcurrentMap<String, List<Term>> getPhoneticHashesOfDictionary() {
        return invertedIndex.entrySet()
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
                            return a.stream().distinct().toList();
                        },
                        ConcurrentHashMap::new));
    }

    /**
     * @param term The term to search.
     * @return the total number of occurrences of the given term in the entire {@link Corpus}.
     */
    public int getTotalNumberOfOccurrencesOfTerm(@NotNull String term) {
        Term termFromIndex = invertedIndex.get(term);
        return termFromIndex == null ? 0 : termFromIndex.totalNumberOfOccurrencesInCorpus();
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
    protected ConcurrentMap<String, Term> indexCorpusAndGet(
            @NotNull Corpus corpus, @NotNull AtomicLong numberOfAlreadyProcessedDocuments) {
        Predicate<Map.Entry<DocumentIdentifier, Document>> documentContentNotNullPredicate =
                entry -> entry != null
                        && entry.getKey() != null
                        && entry.getValue() != null
                        && entry.getValue().getContent() != null;

        return corpus.getCorpus()
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
                                Term::merge /* Merge terms with the same token */,
                                ConcurrentHashMap::new));
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
                .stream().unordered()
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
            int corpusSize, final AtomicLong numberOfAlreadyProcessedDocuments) {

        AtomicReference<Double> progressValue = new AtomicReference<>(0d);   // used only to show the indexing progress
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Thread progressControllerThread = new Thread(() -> {
            final double EPSILON = 0.001;
            double oldProgressValue = progressValue.get();
            progressValue.set(Math.round((0.0 + numberOfAlreadyProcessedDocuments.get()) / corpusSize * 10000) / 100.0);
            String currentProgress = "";
            if (progressValue.get() - oldProgressValue > EPSILON) {
                currentProgress = progressValue.toString();
                System.out.print(currentProgress + " % \t ");
            }
            if (numberOfAlreadyProcessedDocuments.get() == corpusSize && !currentProgress.equals(progressValue.toString())) {
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
                short awaitTermination = 1;    // seconds
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
    @NotNull
    public Corpus getCorpus() {
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
    public final SkipList<Posting> getListOfPostingsForToken(String normalizedToken) {  // TODO: wildcard is not capturing spaces
        String tokenWithEndOfWord = normalizedToken + END_OF_WORD;
        int indexOfFirstWildcardIfPresent = tokenWithEndOfWord.indexOf(WILDCARD);
        if (indexOfFirstWildcardIfPresent > -1) {
            int indexOfLastWildcardIfPresent = tokenWithEndOfWord.lastIndexOf(WILDCARD);
            boolean moreThanOneWildcardIsPresent = indexOfLastWildcardIfPresent > indexOfFirstWildcardIfPresent;

            if (moreThanOneWildcardIsPresent) {
                // Consider the simplified wildcard input token where everything between the first and
                // the last wildcard is folded in a single wildcard and
                tokenWithEndOfWord = tokenWithEndOfWord.replaceAll(
                        ESCAPED_WILDCARD_FOR_REGEX + ".*" + ESCAPED_WILDCARD_FOR_REGEX,
                        ESCAPED_WILDCARD_FOR_REGEX);
            }

            // Rotate the token such that the wildcard appears at the end
            String rotatedToken = tokenWithEndOfWord.substring(indexOfFirstWildcardIfPresent + 1)
                    + tokenWithEndOfWord.substring(0, indexOfFirstWildcardIfPresent);
            // now the wildcard is removed (via substring) but the token has been rotated correctly
            // and, because we are here, we are sure that there is at least one wildcard

            // Prepare the regex used to match the initial input token (with wildcards)
            Pattern pattern = Pattern.compile(normalizedToken.replaceAll(ESCAPED_WILDCARD_FOR_REGEX, ".*"));

            return new SkipList<>(getDictionaryTermsContainingSubstring(rotatedToken)
                    .stream().unordered().parallel()
                    .distinct()
                    .filter(tokenFromDictionary -> pattern.matcher(tokenFromDictionary).matches())
                    .map(invertedIndex::get)
                    .filter(Objects::nonNull)
                    .map(Term::getListOfPostings)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList()));

        } else {
            // no wildcards present in input token
            Term t = invertedIndex.get(normalizedToken);
            return t == null ? new SkipList<>() : t.getListOfPostings();
        }
    }

    /**
     * Exploits the permuterm index to get all terms in the dictionary having
     * a substring which is equal to the given input.
     *
     * @param substring The substring which must match with some term in the dictionary.
     * @return The {@link Collection} (eventually with duplicates) of terms in the
     * dictionary having a substring which is equal to the given one.
     */
    public Collection<String> getDictionaryTermsContainingSubstring(@NotNull String substring) {
        return permutermIndex.prefixMap(substring).values();
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

    /**
     * Exploits the phonetic-hash index to get all terms in the dictionary
     * which are the same hash as computed by the Soundex algorithm.
     *
     * @param word The input word.
     * @return The {@link Collection} (without duplicates) having the same
     * phonetic hash as the input word.
     */
    public Collection<String> getDictionaryTermsFromSoundexCorrectionOf(@NotNull String word) {
        var phoneticHashes = this.phoneticHashes.get(Soundex.getPhoneticHash(word));
        return phoneticHashes == null
                ? new ArrayList<>(0)
                : phoneticHashes.stream().map(Term::getTermString).toList();
    }
}