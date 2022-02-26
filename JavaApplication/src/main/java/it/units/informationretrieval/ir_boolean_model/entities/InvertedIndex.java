package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import it.units.informationretrieval.ir_boolean_model.utils.Soundex;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.SynchronizedSet;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer;
import it.units.informationretrieval.ir_boolean_model.utils.wildcards.MatcherForPermutermIndex;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;
import skiplist.SkipList;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * The (reference to the) {@link Corpus} on which indexing is done.
     */
    @NotNull
    private final Corpus corpus;

    /**
     * The {@link SkipList} of all {@link Posting}s in this system,
     * needed to answer efficiently to Boolean NOT queries.
     */
    @NotNull
    private final SkipList<Posting> postings;

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
     * Like the {@link #permutermIndex}, but keys omit the {@link #END_OF_WORD} symbol.
     * This is done because the {@link #END_OF_WORD} symbol may be needed when handling
     * wildcard queries, but must be omitted when performing spelling correction, and
     * the only way to exploit the fast performances of {@link PatriciaTrie} data-structure
     * in searching by prefix is to create this data-structure, even if it is very
     * similar to the {@link #permutermIndex}: pros: fast, cons: memory waste and
     * updating problems.
     */
    @NotNull
    private final PatriciaTrie<String> permutermIndexWithoutEndOfWord;

    /**
     * {@link List} of all <strong>un</strong>stemmed terms composing the dictionary.
     */
    @NotNull
    private final SynchronizedSet<String> unstemmedTerms = new SynchronizedSet<>();

    /**
     * The {@link Stemmer.AvailableStemmer} used by the system when this instance was created.
     */
    @NotNull
    private final Stemmer.AvailableStemmer stemmer;

    /**
     * Constructor. Creates the instance and indexes the given {@link Corpus}.
     *
     * @param corpus The {@link Corpus} to be indexed.
     */
    public InvertedIndex(@NotNull final Corpus corpus) {

        this.corpus = corpus;

        AtomicLong numberOfAlreadyProcessedDocuments = new AtomicLong(0L);
        Runnable indexingProgressPrinterInterrupter =
                printIndexingProgressAndGetTheRunnableToInterruptPrinting(corpus.size(), numberOfAlreadyProcessedDocuments);

        try {
            invertedIndex = indexCorpusAndGet(corpus, numberOfAlreadyProcessedDocuments);
            postings = SkipList.createNewInstanceFromSortedCollection(
                    invertedIndex.entrySet()
                            .stream().unordered().parallel()
                            .map(Map.Entry::getValue)
                            .map(Term::getListOfPostings)
                            .flatMap(Collection::stream)
                            .sorted()
                            .toList()/*unmodifiable list*/,
                    Posting.DOC_ID_COMPARATOR);
            phoneticHashes = getPhoneticHashesOfDictionary();
            permutermIndex = createPermutermIndexAndGet(END_OF_WORD);
            permutermIndexWithoutEndOfWord = createPermutermIndexAndGet("");// NO end-of-word symbol
        } finally {
            // Join the thread used to print the indexing progress
            indexingProgressPrinterInterrupter.run();
            System.out.println("\nIndexing ended");
        }

        Stemmer.AvailableStemmer stemmerTmp;    // defer assignment to final field
        try {
            stemmerTmp = Stemmer.AvailableStemmer.valueOf_(AppProperties.getInstance().get("app.stemmer"));
        } catch (IOException e) {
            stemmerTmp = Stemmer.AvailableStemmer.NO_STEMMING;
            Logger.getLogger(getClass().getCanonicalName())
                    .log(Level.SEVERE, "Errors while reading app properties", e);
        }
        this.stemmer = stemmerTmp;
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

    /**
     * Creates the permuterm index.
     *
     * @param endOfWordSymbol The (eventually empty, but not-null) end-of-word symbol
     *                        to use in keys of the permuterm-index created by this method.
     * @return the permuterm index.
     */
    @NotNull
    private PatriciaTrie<String> createPermutermIndexAndGet(@NotNull String endOfWordSymbol) {
        Objects.requireNonNull(endOfWordSymbol);
        Stemmer stemmer = Utility.getStemmer() == null
                ? Stemmer.getStemmer(Stemmer.AvailableStemmer.NO_STEMMING)
                : Utility.getStemmer();
        PatriciaTrie<String> permutermIndex = new PatriciaTrie<>();
        Stream.of(getUnstemmedDictionary())
                        // always use un-stemmed words, which are eventually mapped to stemmed words (if stemming must be performed)
                .flatMap(Collection::stream)
                .distinct().unordered().parallel()
                .filter(Objects::nonNull)
                .filter(str -> !str.isBlank())
                .flatMap(strFromDictionary -> {
                    String str = strFromDictionary + endOfWordSymbol;
                    return Arrays.stream(Utility.getAllRotationsOf(str))
                            .map(aRotation -> new Pair<>(
                                    aRotation,                                           /* the rotation */
                                    stemmer.stem(strFromDictionary, corpus.getLanguage())/* the eventually stemmed correspondent term in the dictionary*/))
                            .filter(pair -> !pair.getValue().isBlank());
                })
                .forEachOrdered(pair -> {   // cannot use "collect" because PatriciaTrie is not thread-safe, so error can occours if PatriciaTrie is used as data-structure and parallel streams are used
                    permutermIndex.put(pair.getKey(), pair.getValue());
                });
        return permutermIndex;
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
                            return a.stream().distinct().collect(Collectors.toList());
                        },
                        ConcurrentHashMap::new));
    }

    /**
     * @param term The term to search.
     * @return the Collection Frequency, i.e., the total number of
     * occurrences of the given term in the entire {@link Corpus}.
     */
    public int cf(@NotNull String term) {
        Term termFromIndex = invertedIndex.get(term);
        return termFromIndex == null ? 0 : termFromIndex.cf();
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

        // Avoid documents with null content
        Predicate<Map.Entry<DocumentIdentifier, Document>> documentContentNotNullPredicate =
                entry -> entry != null
                        && entry.getKey() != null
                        && entry.getValue() != null
                        && entry.getValue().getContent() != null;

        return corpus.getCorpus()
                .entrySet()
                .stream().unordered().parallel()
                .peek(ignored -> numberOfAlreadyProcessedDocuments.getAndIncrement()/*document is going to be processed*/)
                .filter(documentContentNotNullPredicate)
                .map(this::getEntrySetOfTokensAndCorrespondingTermsFromADocument)
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
        Map<String, int[]> tokensFromCurrentDocument =
                Utility.tokenizeAndGetMapWithPositionsInDocument(document, corpus.getLanguage(), unstemmedTerms);

        return tokensFromCurrentDocument
                .entrySet()
                .stream().unordered()
                .map(tokenAndPositions -> new AbstractMap.SimpleEntry<>(
                        tokenAndPositions.getKey(),
                        new Term(
                                new PostingList(new Posting(docIdThisDocument, tokenAndPositions.getValue())),
                                tokenAndPositions.getKey())))
                .filter(e -> !e.getKey()/*the token*/.isBlank())
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
            String currentProgress;
            if (progressValue.get() - oldProgressValue > EPSILON) {
                currentProgress = progressValue.toString();
                System.out.print(currentProgress + " % \t ");
            }
        });
        System.out.println("Indexing started");
        final int DELAY_PROGRESS_CONTROLLER = 1;    // seconds
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
     * <p/>
     * <strong>NOTICE</strong>: terms returned by this method are stemmed, if the system is
     * configured to perform stemming.
     */
    @NotNull
    public List<String> getDictionary() {
        return invertedIndex.keySet()
                .stream().sequential()
                .sorted()
                .toList();
    }

    /**
     * @return the dictionary as sorted {@link java.util.List} of {@link String}s (the terms).
     * <p/>
     * <strong>NOTICE</strong>: terms returned by this method are <strong>NOT</strong> stemmed.
     */
    @NotNull
    public List<String> getUnstemmedDictionary() {
        return unstemmedTerms.stream().toList();
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
     * This method does work also with {@link #WILDCARD} symbol
     * in the term, but the wildcard does <sttong>not</sttong>
     * capture spaces (in such case, it would refer to more than
     * one token, and this function is not currently supported).
     *
     * @param normalizedToken The normalized term.
     * @return The {@link PostingList} associated with the desired term or null
     * if it is not found in this {@link InvertedIndex}.
     */
    @NotNull
    public final SkipList<Posting> getListOfPostingsForToken(String normalizedToken) {
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

            return new SkipList<>(getDictionaryTermsContainingPrefix(rotatedToken, false)
                    .stream().unordered().parallel()
                    .distinct()
                    .peek(tokenFromDictionary -> {
                        assert tokenFromDictionary != null && !tokenFromDictionary.isBlank();
                    })
                    .filter(tokenFromDictionary -> MatcherForPermutermIndex
                            .isWildcardQueryCompatibleWithStemmedTokenFromIndex(
                                    normalizedToken.replaceAll(ESCAPED_WILDCARD_FOR_REGEX, WILDCARD),
                                    tokenFromDictionary, corpus.getLanguage()))
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
     * a prefix which is equal to the given input.
     *
     * @param prefix          The prefix which must match with some term in the dictionary.
     * @param ignoreEndOfWord Flag: if true, the {@link #END_OF_WORD} (present in terms
     *                        of the {@link #permutermIndex}) is not consider for the
     *                        execution (this is because the {@link #END_OF_WORD} may
     *                        be needed when handling wildcard query, but must be omitted
     *                        when performing spelling correction).
     * @return The {@link Collection} (eventually with duplicates) of terms in the
     * dictionary having a prefix which is equal to the given one.
     */
    public Collection<String> getDictionaryTermsContainingPrefix(@NotNull String prefix, boolean ignoreEndOfWord) {
        var permutermIndexToUse = ignoreEndOfWord ? permutermIndexWithoutEndOfWord : permutermIndex;
        return permutermIndexToUse.prefixMap(prefix).values();
    }

    /**
     * @return the sorted {@link SkipList} of all {@link Posting}s currently present.
     * <strong>Notice</strong>: the returned collection is sorted.
     * </ol>
     */
    @NotNull
    public SkipList<Posting> getAllPostings() {
        return postings;
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

    @Serial
    private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
        in.defaultReadObject();
        try {
            Stemmer.AvailableStemmer currentAvailableStemmer =
                    Stemmer.AvailableStemmer.valueOf_(AppProperties.getInstance().get("app.stemmer"));
            if (currentAvailableStemmer != stemmer/*obtained from deserialization*/) {
                System.err.println(
                        "The inverted index was created with different app settings:" + System.lineSeparator() +
                                "\t Stemmer was " + stemmer + " but now it is " + currentAvailableStemmer + "." + System.lineSeparator() +
                                "\t Please, re-create the instance for coherent results.");
            }
        } catch (IOException e) {
            Logger.getLogger(getClass().getCanonicalName())
                    .log(Level.SEVERE, "Errors while reading app properties", e);
        }
    }
}