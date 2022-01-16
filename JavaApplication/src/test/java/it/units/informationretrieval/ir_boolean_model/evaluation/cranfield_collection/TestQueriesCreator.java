package it.units.informationretrieval.ir_boolean_model.evaluation.cranfield_collection;

import it.units.informationretrieval.ir_boolean_model.document_descriptors.CranfieldDocument;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.queries.BINARY_OPERATOR;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import it.units.informationretrieval.ir_boolean_model.queries.UNARY_OPERATOR;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Due to the lack of test boolean queries, this class aims to create them in order
 * to evaluate the Information Retrieval System.
 * The README in the package relative to the boolean queries from the Cranfield
 * collections (see resource folders) explains the creation process.
 * Execute the {@link #main(String[]) main} method of this class to produce the files
 * named {@link #BOOLEAN_QUERIES_FILE_NAME} and {@link #BOOLEAN_QUERIES_ASSOCIATION_TO_DOCS_FILE_NAME}
 * respectively.
 *
 * @author Matteo Ferfoglia
 */
class TestQueriesCreator {

    /**
     * The {@link Language} of the input corpus.
     */
    private static final Language CORPUS_LANGUAGE = CranfieldDocument.LANGUAGE;
    /**
     * The number of AND, OR, NOT and PHRASE distinct queries to create.
     */
    private static final int T = 10;
    /**
     * The number of words in AND, OR and phrase queries.
     */
    private final static int N = 2;
    /**
     * The name of the file where to save the boolean queries.
     */
    private static final String BOOLEAN_QUERIES_FILE_NAME = "cran.bool.qry";
    /**
     * The name of the file where to save the association between boolean queries and answering documents.
     */
    private static final String BOOLEAN_QUERIES_ASSOCIATION_TO_DOCS_FILE_NAME = "cranboolqrel";
    /**
     * Maps query strings to the list of doc ids referring to {@link StemmedDocument}
     * that answer the given query string saved as corresponding key in this map.
     */
    private static Map<String, List<Integer>> mapQueryStringsToDocIdsOfAnswers;

    public static void main(String[] args) throws NoMoreDocIdsAvailable, URISyntaxException, IOException {

        List<Document> corpusAsSortedListOfDocs = CranfieldDocument.createCorpus()
                .getListOfDocuments().stream().sorted().toList();    // sort according doc numbers;
        List<String> allWordsFromAllDocsWithoutStopWords = getAllWordsFromAllDocs(corpusAsSortedListOfDocs, true);
        List<String> allWordsFromAllDocsKeepingStopWords = getAllWordsFromAllDocs(corpusAsSortedListOfDocs, false);
        List<StemmedDocument> stemmedDocumentsWithoutStopWords = getStemmedDocuments(corpusAsSortedListOfDocs, true);
        List<StemmedDocument> stemmedDocumentsKeepingStopWords = getStemmedDocuments(corpusAsSortedListOfDocs, false);
        mapQueryStringsToDocIdsOfAnswers = new HashMap<>();

        for (int i = 0; i < T; i++) {
            createAndAddToMap_AND_OR_NOT_queries(allWordsFromAllDocsWithoutStopWords, stemmedDocumentsWithoutStopWords);
            createAndAddToMap_PHRASE_queries(allWordsFromAllDocsKeepingStopWords, stemmedDocumentsKeepingStopWords);
        }

        createAndAddToMapComplexQueries();

        // keep only entries that refer to queries with at least one result
        mapQueryStringsToDocIdsOfAnswers = mapQueryStringsToDocIdsOfAnswers.entrySet()
                .stream().filter(e -> e.getValue().size() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        writeQueriesToFile();
    }

    /**
     * Write files of query (to {@link #BOOLEAN_QUERIES_FILE_NAME}) and Of association
     * of queries with documents (to {@link #BOOLEAN_QUERIES_ASSOCIATION_TO_DOCS_FILE_NAME}).
     */
    private static void writeQueriesToFile() {

        try (
                Writer queryWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(BOOLEAN_QUERIES_FILE_NAME), StandardCharsets.UTF_8));
                Writer queryAssociationWriter = new BufferedWriter(new OutputStreamWriter(
                        new FileOutputStream(BOOLEAN_QUERIES_ASSOCIATION_TO_DOCS_FILE_NAME), StandardCharsets.UTF_8))
        ) {
            int i = 1;
            for (var entry : mapQueryStringsToDocIdsOfAnswers.entrySet()) {
                int I = i;
                boolean addNewLineAtBeginning = i++ > 1;
                StringBuilder queryToDocAssociationFileContent = new StringBuilder();
                String queryFileContent = (addNewLineAtBeginning ? "\n" : "") +
                        ".I " + I + " \n.W\n" + entry.getKey();
                queryToDocAssociationFileContent.append(addNewLineAtBeginning ? "\n" : "");
                entry.getValue().forEach(docNumber ->
                        queryToDocAssociationFileContent.append(I).append(" ").append(docNumber)
                                .append(" 1").append("\n"));
                queryWriter.write(queryFileContent);
                queryAssociationWriter.write(queryToDocAssociationFileContent
                        .substring(0, Math.max(0, queryToDocAssociationFileContent.lastIndexOf("\n"))));    // remove last new line
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Creates and adds to {@link #mapQueryStringsToDocIdsOfAnswers} complex queries,
     * realized by compositions of queries already in the map.
     */
    private static void createAndAddToMapComplexQueries() {

        final int MAX_NUMBER_OF_QUERY_PER_TYPE = 5;    // with cartesian product, complexity increases fast

        Pattern andQueriesPattern = Pattern.compile("^\\w+ \\" + BINARY_OPERATOR.AND.getSymbol() + " \\w+$");
        Pattern orQueriesPattern = Pattern.compile("^\\w+ \\" + BINARY_OPERATOR.OR.getSymbol() + " \\w+$");
        Pattern notQueriesPattern = Pattern.compile("^\\" + UNARY_OPERATOR.NOT.getSymbol() + " \\w+$");
        Pattern phraseQueriesPattern = Pattern.compile("^[" + BooleanExpression.PHRASE_DELIMITER
                + "][\\w ]+[" + BooleanExpression.PHRASE_DELIMITER + "]$");

        final BiFunction<Map<String, List<Integer>>, Pattern, List<Map.Entry<String, List<Integer>>>>
                getListOfEntriesWithQueriesMatchingThePattern = (mapQueryStringsToDocIds, pattern) ->
                mapQueryStringsToDocIds.entrySet().stream()
                        .filter(e -> pattern.matcher(e.getKey()).matches())
                        .limit(MAX_NUMBER_OF_QUERY_PER_TYPE)
                        .toList();

        List<Map.Entry<String, List<Integer>>> andQueriesEntries =
                getListOfEntriesWithQueriesMatchingThePattern.apply(mapQueryStringsToDocIdsOfAnswers, andQueriesPattern);
        List<Map.Entry<String, List<Integer>>> orQueriesEntries =
                getListOfEntriesWithQueriesMatchingThePattern.apply(mapQueryStringsToDocIdsOfAnswers, orQueriesPattern);
        List<Map.Entry<String, List<Integer>>> notQueriesEntries =
                getListOfEntriesWithQueriesMatchingThePattern.apply(mapQueryStringsToDocIdsOfAnswers, notQueriesPattern);
        List<Map.Entry<String, List<Integer>>> phraseQueriesEntries =
                getListOfEntriesWithQueriesMatchingThePattern.apply(mapQueryStringsToDocIdsOfAnswers, phraseQueriesPattern);

        var cartesianProduct =
                Utility.getCartesianProduct(Arrays.asList(andQueriesEntries, orQueriesEntries, notQueriesEntries, phraseQueriesEntries));

        BiFunction<List<Map.Entry<String, List<Integer>>>, BINARY_OPERATOR, Map.Entry<String, List<Integer>>>
                complexQueryStringCreator = (queryEntryList, binaryOperator) -> {
            String complexQueryString = queryEntryList.stream()
                    .map(Map.Entry::getKey).map(queryString -> "(" + queryString + ")")
                    .collect(Collectors.joining(" " + binaryOperator.getSymbol() + " "));

            BinaryOperator<List<Integer>> accumulator = switch (binaryOperator) {
                case AND -> Utility::intersectionOfSortedLists;
                case OR -> Utility::unionOfSortedLists;
                //noinspection UnnecessaryDefault
                default -> throw new UnsupportedOperationException("Unknown binary operator");
            };
            List<Integer> docIdsOfAnswers = queryEntryList.stream()
                    .map(Map.Entry::getValue)
                    .map(l -> l.stream().sorted().toList())
                    .reduce(accumulator)
                    .orElse(new ArrayList<>());

            return new Pair<>(complexQueryString, docIdsOfAnswers);
        };
        cartesianProduct.forEach(queryEntryList -> {
            var andComplexQuery = complexQueryStringCreator.apply(queryEntryList, BINARY_OPERATOR.AND);
            mapQueryStringsToDocIdsOfAnswers.put(andComplexQuery.getKey(), andComplexQuery.getValue());
            var orComplexQuery = complexQueryStringCreator.apply(queryEntryList, BINARY_OPERATOR.OR);
            mapQueryStringsToDocIdsOfAnswers.put(orComplexQuery.getKey(), orComplexQuery.getValue());
        });

    }

    /**
     * Creates and adds to {@link #mapQueryStringsToDocIdsOfAnswers} phrase queries.
     *
     * @param allWordsFromAllDocsKeepingStopWords The list of words to use to create phrase queries.
     * @param stemmedDocumentsKeepingStopWords    The list of {@link StemmedDocument} obtained by keeping stop-words.
     */
    private static void createAndAddToMap_PHRASE_queries(
            List<String> allWordsFromAllDocsKeepingStopWords, List<StemmedDocument> stemmedDocumentsKeepingStopWords) {

        if (N > allWordsFromAllDocsKeepingStopWords.size()) {
            throw new IllegalArgumentException(
                    "Value N=" + N + " is too big: max allowed for this corpus is N=" + allWordsFromAllDocsKeepingStopWords.size());
        } else {

            List<String> queryWordsForPhrase;
            List<String> stemmedQueryWordsForPhrase;
            String phraseQueryString = null;

            int iterationsCounter = 0;
            do {
                int indexOfFirstWordOfPhraseInListOfAllWords = (int) (Math.random() *
                        (allWordsFromAllDocsKeepingStopWords.size() - 1/*index goes from 0 to .size()-1*/ - N));
                queryWordsForPhrase = allWordsFromAllDocsKeepingStopWords.subList(
                        indexOfFirstWordOfPhraseInListOfAllWords, indexOfFirstWordOfPhraseInListOfAllWords + N);
                stemmedQueryWordsForPhrase = getStemmed(queryWordsForPhrase);
            } while ((stemmedQueryWordsForPhrase.size() != N // stemming may remove entire words
                    // following conditions enforce to have distinct queries:
                    || mapQueryStringsToDocIdsOfAnswers.containsKey(phraseQueryString =
                    BooleanExpression.PHRASE_DELIMITER + String.join(" ", queryWordsForPhrase) + BooleanExpression.PHRASE_DELIMITER))
                    && iterationsCounter++ < allWordsFromAllDocsKeepingStopWords.size()/*avoid infinity loop*/);

            final List<String> finalStemmedQueryWordsForPhrase = stemmedQueryWordsForPhrase;
            String finalPhraseQueryString = Objects.requireNonNull(phraseQueryString);

            // add phrase query strings and results to the map
            stemmedDocumentsKeepingStopWords.forEach(stemmedDocument ->
                    addQueryToMapIf(
                            finalPhraseQueryString, stemmedDocument.getDocId(),
                            stemmedDocument.containsConsecutive(finalStemmedQueryWordsForPhrase)));
        }
    }

    /**
     * Creates and adds to {@link #mapQueryStringsToDocIdsOfAnswers} AND / OR / NOT queries.
     *
     * @param allWordsFromAllDocsWithoutStopWords The list of words to use to create the queries.
     * @param stemmedDocumentsWithoutStopWords    The list of {@link StemmedDocument} obtained by removing stop-words.
     */
    private static void createAndAddToMap_AND_OR_NOT_queries(
            List<String> allWordsFromAllDocsWithoutStopWords, List<StemmedDocument> stemmedDocumentsWithoutStopWords) {

        // AND and OR boolean query strings creation
        List<String> queryWords, stemmedQueryWords;
        String andQueryString = null, orQueryString = null;

        int iterationsCounter = 0;
        do {
            queryWords = getNDistinctWordsRandomly(N, allWordsFromAllDocsWithoutStopWords);
            stemmedQueryWords = getStemmed(queryWords);
        } while ((queryWords.size() != stemmedQueryWords.size()   // stemming may remove entire words
                // following conditions enforce to have distinct queries:
                || mapQueryStringsToDocIdsOfAnswers.containsKey(andQueryString = String.join(" " + BINARY_OPERATOR.AND.getSymbol() + " ", queryWords))
                || mapQueryStringsToDocIdsOfAnswers.containsKey(orQueryString = String.join(" " + BINARY_OPERATOR.OR.getSymbol() + " ", queryWords)))
                && iterationsCounter++ < allWordsFromAllDocsWithoutStopWords.size()/*avoid infinity loop*/);
        final List<String> finalStemmedQueryWords = stemmedQueryWords;

        // NOT boolean query string creation (negation of single word)
        List<String> stemmedNotQueryWordListOf1Element;
        String notQueryWord, notQueryString = null;
        iterationsCounter = 0;
        do {
            notQueryWord = getNDistinctWordsRandomly(1, allWordsFromAllDocsWithoutStopWords).get(0);
            stemmedNotQueryWordListOf1Element = getStemmed(List.of(notQueryWord));
        } while ((stemmedNotQueryWordListOf1Element.size() != 1   // stemming may remove entire words
                // following condition enforces to have distinct queries:
                || mapQueryStringsToDocIdsOfAnswers.containsKey(notQueryString = UNARY_OPERATOR.NOT.getSymbol() + " " + notQueryWord))
                && iterationsCounter++ < allWordsFromAllDocsWithoutStopWords.size()/*avoid infinity loop*/);
        final String finalStemmedQueryWordNot = stemmedNotQueryWordListOf1Element.get(0);

        // add query strings and results to the map
        String finalAndQueryString = Objects.requireNonNull(andQueryString);
        String finalOrQueryString = Objects.requireNonNull(orQueryString);
        String finalNotQueryString = Objects.requireNonNull(notQueryString);
        stemmedDocumentsWithoutStopWords.forEach(stemmedDocument -> {
            addQueryToMapIf(finalAndQueryString, stemmedDocument.getDocId(), stemmedDocument.containsAllWords(finalStemmedQueryWords));
            addQueryToMapIf(finalOrQueryString, stemmedDocument.getDocId(), stemmedDocument.containsAtLeastOneWord(finalStemmedQueryWords));
            addQueryToMapIf(finalNotQueryString, stemmedDocument.getDocId(), stemmedDocument.notContain(finalStemmedQueryWordNot));
        });
    }

    /**
     * Add a pair(queryString, docId) to {@link #mapQueryStringsToDocIdsOfAnswers} if the condition is true.
     *
     * @param queryString The query string.
     * @param docId       The docId of the document answering the query.
     * @param condition   The condition: if true, the pair is added, otherwise the query string
     *                    is added, but not the corresponding answer.
     */
    private static void addQueryToMapIf(String queryString, int docId, boolean condition) {
        var docIdsForQuery = mapQueryStringsToDocIdsOfAnswers.getOrDefault(queryString, new ArrayList<>());
        if (condition) {
            docIdsForQuery.add(docId);
        }
        mapQueryStringsToDocIdsOfAnswers.put(queryString, docIdsForQuery);
    }

    /**
     * @param docsFromCorpus  The input corpus.
     * @param removeStopWords Flag: true if stop words must be removed, false to keep them.
     * @return The {@link List} of {@link StemmedDocument}s from the input {@link Corpus}.
     */
    private static List<StemmedDocument> getStemmedDocuments(List<Document> docsFromCorpus, boolean removeStopWords) {
        return docsFromCorpus.stream().sequential()
                .map(doc -> new StemmedDocument(doc, removeStopWords))
                .toList();
    }

    /**
     * @param words The input {@link List} of words.
     * @return the corresponding list of stemmed words.
     */
    private static List<String> getStemmed(List<String> words) {
        return words.stream().sequential()
                .map(s -> Stemmer.getStemmer(Stemmer.AvailableStemmer.PORTER).stem(s, CORPUS_LANGUAGE))
                .filter(s -> !s.isBlank())
                .toList();
    }

    /**
     * @param stringList Input list of words.
     * @return a {@link List} of N distinct words randomly taken from the input list.
     */
    private static List<String> getNDistinctWordsRandomly(int N, List<String> stringList) {
        var toReturn = IntStream.iterate(0, j -> j + 1)
                .mapToObj(j -> IntStream.of(
                                (int) (Math.random() * (stringList.size() - 1)),
                                (int) (Math.random() * (stringList.size() - 1)))
                        .mapToObj(stringList::get)
                        .distinct()
                        .toList())
                .filter(s -> s.size() == N)
                .findAny()
                .orElseThrow();
        assert toReturn.size() == N;
        return toReturn;
    }

    /**
     * @param docsFromCorpus  the input corpus.
     * @param removeStopWords Flag: true if stop words must be removed, false to keep them.
     * @return the {@link List} of all words from all documents in the same order as they
     * appear in documents (duplicates can be present if the same word appear multiple
     * times either in the same or in different documents).
     */
    private static List<String> getAllWordsFromAllDocs(List<Document> docsFromCorpus, boolean removeStopWords) {
        return docsFromCorpus.stream().sequential()
                .flatMap(doc -> TestQueriesCreator.normalizeTestDocument(doc, removeStopWords))
                .toList();
    }

    /**
     * @param document        A {@link Document}.
     * @param removeStopWords Flag: true if stop words must be removed, false to keep them.
     * @return the sequential {@link Stream} of normalized {@link String}s present in the given document.
     */
    private static Stream<String> normalizeTestDocument(Document document, boolean removeStopWords) {
        return Arrays.stream(Utility.split(document.getContentAsString().toLowerCase()
                        .replaceAll("[^a-z]", " ")))// remove everything but lowercase characters
                .sequential()
                .filter(s -> !s.isBlank())
                .filter(s -> !removeStopWords || !Utility.isStopWord(s, CORPUS_LANGUAGE));
    }

    /**
     * Utility class to save the stemmed content of a {@link Document}.
     */
    private static class StemmedDocument {

        /**
         * The document identifier.
         */
        final int docId;

        /**
         * The entire stemmed content of the document.
         */
        @NotNull
        private final List<String> entireContent;

        /**
         * @param document        A {@link Document}.
         * @param removeStopWords Flag: true if stop words must be removed, false to keep them.
         */
        public StemmedDocument(@NotNull final Document document, boolean removeStopWords) {
            this.entireContent = getStemmed(normalizeTestDocument(document, removeStopWords).toList());
            if (document instanceof CranfieldDocument cd) {
                this.docId = cd.getDocNumber();
            } else {
                throw new UnsupportedOperationException(
                        "Only " + CranfieldDocument.class.getCanonicalName() + " instances handled.");
            }
        }

        /**
         * @param words A {@link List} of words.
         * @return true if this instance contains all the given words, false otherwise.
         */
        public boolean containsAllWords(@NotNull List<String> words) {
            return entireContent.containsAll(Objects.requireNonNull(words));
        }

        /**
         * @return the document identifier.
         */
        public int getDocId() {
            return docId;
        }

        /**
         * @param words A {@link List} of words.
         * @return true if this instance contains at least one of the given words, false otherwise.
         */
        public boolean containsAtLeastOneWord(@NotNull List<String> words) {
            return words.stream().anyMatch(entireContent::contains);
        }

        /**
         * @param words A {@link List} of words.
         * @return true if this instance contains all the given words, and they are consecutive, false otherwise.
         */
        public boolean containsConsecutive(@NotNull List<String> words) {
            if (words.size() > 0) {
                int indexOfFirstWord = 0, prevIndexValue = indexOfFirstWord;
                while (indexOfFirstWord < entireContent.size()
                        && (indexOfFirstWord += entireContent.subList(indexOfFirstWord, entireContent.size())    // sublist to examine the remaining part only
                        .indexOf(words.get(0))) > prevIndexValue) {
                    prevIndexValue = indexOfFirstWord;
                    int i, j;
                    for (i = 0; i < words.size() && (j = i + indexOfFirstWord) < entireContent.size(); i++) {
                        if (!entireContent.get(j).equals(words.get(i))) {
                            break;  // exit from inner for-loop (if here, i<words.size() for sure, because the loop is interrupted for sure before the last incrementation of i
                        }
                    }
                    if (i == words.size()) {
                        return true;
                    }
                    indexOfFirstWord++;
                }
                return false;
            } else {
                throw new IllegalArgumentException("At least one word expected");
            }
        }

        /**
         * @param word A word.
         * @return true if this instance does <strong>NOT</strong> contain the given word, false otherwise.
         */
        public boolean notContain(@NotNull String word) {
            return !entireContent.contains(Objects.requireNonNull(word));
        }
    }
}