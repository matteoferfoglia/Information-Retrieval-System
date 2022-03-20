package it.units.informationretrieval.ir_boolean_model;

import it.units.informationretrieval.ir_boolean_model.entities.*;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import org.jetbrains.annotations.NotNull;
import skiplist.SkipList;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * This class represents an Information Retrieval System.
 *
 * @author Matteo Ferfoglia
 */
public class InformationRetrievalSystem implements Externalizable {

    /**
     * The {@link Corpus}.
     */
    @NotNull
    private Corpus corpus;

    /**
     * The {@link InvertedIndex}.
     */
    @NotNull
    private InvertedIndex invertedIndex;

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

    public InformationRetrievalSystem() {
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
        return invertedIndex.cf(term);
    }

    /**
     * Exploits the permuterm index to get all terms in the dictionary having
     * a prefix which is equal to the given input.
     *
     * @param prefix          The prefix which must match with some term in the dictionary.
     * @param ignoreEndOfWord Flag: if true, the end-of-word symbol (present in terms
     *                        of the permuterm index) is not consider for the execution
     *                        (this is because the end-of-word symbol may be needed when
     *                        handling wildcard query, but must be omitted when
     *                        performing spelling correction).
     * @return The {@link Collection} (eventually with duplicates) of terms in the
     * dictionary having a prefix which is equal to the given one.
     */
    public Collection<String> getDictionaryTermsContainingPrefix(@NotNull String prefix, boolean ignoreEndOfWord) {
        return invertedIndex.getDictionaryTermsContainingPrefix(prefix, ignoreEndOfWord);
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
     * @return The sorted {@link SkipList} of all (distinct) {@link Posting}s in the System.
     */
    @NotNull
    public SkipList<Posting> getAllPostings() {
        return invertedIndex.getAllPostings();
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
     * @return the number of documents in the {@link Corpus}.
     */
    public int size() {
        return corpus.size();
    }

    /**
     * @param str The term for which the term-frequency is desired.
     * @return the collection frequency, i.e., the total number of occurrences
     * for the string given as parameter in the entire corpus.
     */
    public int cf(String str) {
        return invertedIndex.cf(str);
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(invertedIndex);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        invertedIndex = (InvertedIndex) in.readObject();
        corpus = invertedIndex.getCorpus();
    }
}