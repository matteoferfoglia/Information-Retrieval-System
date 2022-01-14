package it.units.informationretrieval.ir_boolean_model;

import it.units.informationretrieval.ir_boolean_model.entities.*;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import org.jetbrains.annotations.NotNull;
import skiplist.SkipList;

import java.io.Serializable;
import java.util.*;

/**
 * This class represents an Information Retrieval System.
 *
 * @author Matteo Ferfoglia
 */
public class InformationRetrievalSystem implements Serializable {

    /**
     * The {@link Corpus}.
     */
    @NotNull
    private final Corpus corpus;

    /**
     * The {@link InvertedIndex}.
     */
    @NotNull
    private final InvertedIndex invertedIndex;

    /**
     * Constructor.
     *
     * @param corpus The {@link Corpus} to use in this {@link InformationRetrievalSystem}.
     */
    public InformationRetrievalSystem(@NotNull Corpus corpus) {
        this.corpus = Objects.requireNonNull(corpus);
        this.invertedIndex = new InvertedIndex(corpus);
    }

    /**
     * Constructor.
     *
     * @param invertedIndex The {@link InvertedIndex} to use in this {@link InformationRetrievalSystem}.
     */
    public InformationRetrievalSystem(@NotNull InvertedIndex invertedIndex) {
        this.invertedIndex = invertedIndex;
        this.corpus = invertedIndex.getCorpus();
    }

    /**
     * @param normalizedToken The token to search in the {@link #invertedIndex}.
     * @return the {@link List} of {@link Posting} for the given token. Updates
     * on the object returned by this method reflects to the actual system data.
     */
    @NotNull
    public SkipList<Posting> getListOfPostingForToken(@NotNull final String normalizedToken) {
        return invertedIndex.getListOfPostingsForToken(normalizedToken);
    }

    /**
     * @param term The term to search.
     * @return the total number of occurrences of the given term in the entire {@link Corpus}
     * handled by this instance.
     */
    public int getTotalNumberOfOccurrencesOfTerm(@NotNull String term) {
        return invertedIndex.getTotalNumberOfOccurrencesOfTerm(term);
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
        return invertedIndex.getDictionaryTermsContainingSubstring(substring);
    }

    /**
     * @return a new instance of {@link BooleanExpression} for this instance.
     */
    @NotNull
    public BooleanExpression createNewBooleanExpression() {
        return new BooleanExpression(this);
    }

    /**
     * @param queryString The query string.
     * @return the sorted (according to the relevance) results for the given query string.
     */
    @NotNull
    public List<Document> retrieve(@NotNull String queryString) {
        return createNewBooleanExpression().parseQuery(queryString).evaluate();
    }

    /**
     * @return The {@link Set} of all (distinct) {@link DocumentIdentifier}s in the System.
     * <strong>Important</strong>: the returned collection is <em>not</em> suitable for concurrent actions.
     */
    @NotNull
    public Set<DocumentIdentifier> getAllDocIds() {
        return invertedIndex.getAllDocIds();
    }

    /**
     * @return the corpus associated with this instance.
     */
    @NotNull
    public Corpus getCorpus() {
        return corpus;
    }

    /**
     * @return the language used in this instance.
     */
    @NotNull
    public Language getLanguage() {
        return corpus.getLanguage();
    }

    /**
     * @param docId The {@link DocumentIdentifier} to find.
     * @return the {@link Set} (eventually empty) with all {@link Posting}s
     * having the given {@link DocumentIdentifier}.
     */
    @NotNull
    public Set<Posting> getPostingList(@NotNull DocumentIdentifier docId) {
        return invertedIndex.getPostingList(docId);
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
        return invertedIndex.getDictionaryTermsFromSoundexCorrectionOf(word);
    }

    /**
     * @return all terms (as strings) present in the dictionary.
     */
    @NotNull
    public Collection<String> getDictionary() {
        return invertedIndex.getDictionary();
    }

    /**
     * @param dfThreshold A threshold value for the document-frequency value.
     * @return all terms (as strings) present in the dictionary and
     * having a document-frequency value strictly higher than the
     * specified threshold.
     */
    @NotNull
    public Collection<String> getDictionary(double dfThreshold) {
        return invertedIndex.getDictionary(dfThreshold);
    }

    /**
     * @param tfThreshold A threshold value for the term-frequency value.
     * @return all terms (as strings) present in the dictionary and
     * having a term-frequency value strictly higher than the
     * specified threshold.
     */
    @NotNull
    public Collection<String> getDictionaryOverTf(int tfThreshold) {
        return invertedIndex.getDictionaryOverTf(tfThreshold);
    }

    /**
     * @return the average document-frequency value.
     */
    public double avgDf() {
        return invertedIndex.avgDf();
    }

    /**
     * @return the number of documents in the {@link Corpus}.
     */
    public int size() {
        return corpus.size();
    }

    /**
     * @return the average Wf-Idf value for the given string.
     * @throws NoSuchElementException if no value is present.
     */
    public double avgWfIdf(String str) throws NoSuchElementException {
        return getListOfPostingForToken(str)
                .stream().mapToDouble(posting -> posting.wfIdf(size())).average().orElseThrow();
    }

    /**
     * @param str The term for which the term-frequency is desired.
     * @return the term frequency (total number of occurrences) for the string given as parameter.
     */
    public int tf(String str) {
        return invertedIndex.getTotalNumberOfOccurrencesOfTerm(str);
    }

    /**
     * @return the average term frequency over the dictionary.
     * @throws NoSuchElementException if no value is present.
     */
    public double avgTf() throws NoSuchElementException {
        return getDictionary().stream()
                .mapToInt(invertedIndex::getTotalNumberOfOccurrencesOfTerm)
                .average()
                .orElseThrow();
    }

    /**
     * @return the average value of Wf-Idf over the dictionary.
     * @throws NoSuchElementException if no value is present.
     */
    public double avgWfIdf() throws NoSuchElementException {
        return getDictionary().stream()
                .mapToDouble(this::avgWfIdf)
                .average()
                .orElseThrow();
    }

}