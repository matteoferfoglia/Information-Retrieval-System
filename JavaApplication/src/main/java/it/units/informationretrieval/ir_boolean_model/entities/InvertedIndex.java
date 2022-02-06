package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Soundex;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer;
import it.units.informationretrieval.ir_boolean_model.utils.wildcards.MatcherForPermutermIndex;
import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;
import skiplist.SkipList;
import skiplist.SkipListHashMap;

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
     * The {@link Map} collecting as keys {@link DocumentIdentifier}s and ad
     * corresponding value the {@link Set} of {@link Posting} referring to
     * that {@link DocumentIdentifier}. This field can be used to answer
     * NOT queries when also positions of words in {@link Document} matters
     * (because positions are saved in {@link Posting}s).
     */
    @NotNull
    private final SkipListHashMap<DocumentIdentifier, Set<Posting>> postingsByDocId;

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
     * {@link List} of all <strong>un</strong>stemmed terms composing the dictionary.
     */
    @NotNull
    private final List<String> unstemmedTerms = new ArrayList<>();

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
        this.postingsByDocId = new SkipListHashMap<>();

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

    @NotNull
    private PatriciaTrie<String> createPermutermIndexAndGet() {
        Stemmer stemmer = Utility.getStemmer() == null
                ? Stemmer.getStemmer(Stemmer.AvailableStemmer.NO_STEMMING)
                : Utility.getStemmer();
        return Stream.of(getDictionary(), getUnstemmedDictionary())
                .flatMap(Collection::stream)
                .distinct().unordered().parallel()
                .filter(Objects::nonNull)
                .filter(str -> !str.isBlank())
                .flatMap(strFromDictionary -> {
                    String str = strFromDictionary + END_OF_WORD;
                    return Arrays.stream(Utility.getAllRotationsOf(str))
                            .map(aRotation -> new Pair<>(
                                    aRotation,                                           /* the rotation */
                                    stemmer.stem(strFromDictionary, corpus.getLanguage())/* the eventually stemmed correspondent term in the dictionary*/))
                            .filter(pair -> !pair.getValue().isBlank());
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
     * @param dfThreshold A threshold value for the document-frequency value.
     * @return all terms (as strings) present in the dictionary and
     * having a document-frequency value strictly higher than the
     * specified threshold.
     */
    @NotNull
    public Collection<String> getDictionary(double dfThreshold) {
        return invertedIndex.entrySet()
                .stream().sequential()
                .filter(entryTerm -> entryTerm.getValue().df() > dfThreshold)
                .map(Map.Entry::getKey)
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

            return new SkipList<>(getDictionaryTermsContainingSubstring(rotatedToken)
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
     * @return the sorted {@link Set} of all {@link DocumentIdentifier}s currently present.
     * <strong>Notice</strong>: the returned collection is the keySet of a {@link SkipListHashMap},
     * this means that it is sorted, because it is based on a skipList.
     * </ol>
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

    /**
     * @return the average document-frequency value.
     */
    public double avgDf() {
        return invertedIndex.values().stream()
                .mapToInt(Term::df)
                .average()
                .orElseThrow();
    }

    /**
     * @param tfThreshold A threshold value for the term-frequency value.
     * @return all terms (as strings) present in the dictionary and
     * having a term-frequency value strictly higher than the
     * specified threshold.
     */
    @NotNull
    public Collection<String> getDictionaryOverTf(int tfThreshold) {
        return invertedIndex.keySet()
                .stream().sequential()
                .filter(term -> cf(term) > tfThreshold)
                .sorted()
                .toList();
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