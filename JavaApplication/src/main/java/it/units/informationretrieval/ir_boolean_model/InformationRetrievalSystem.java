package it.units.informationretrieval.ir_boolean_model;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentIdentifier;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndex;
import it.units.informationretrieval.ir_boolean_model.entities.Posting;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
        this.invertedIndex = Objects.requireNonNull(invertedIndex);
        this.corpus = invertedIndex.getCorpus();
    }

    /**
     * @param normalizedToken The token to search in the {@link #invertedIndex}.
     * @return the {@link List} of {@link Posting} for the given token.
     */
    @NotNull
    public List<Posting> getListOfPostingForToken(@NotNull final String normalizedToken) {  // TODO: return SkipList?
        return invertedIndex.getPostingListForToken(normalizedToken).toUnmodifiableListOfPostings();
    }

    @NotNull
    public BooleanExpression createNewBooleanExpression() {
        return new BooleanExpression(this) {
        };
    }

    /**
     * @return The {@link Set} of all (distinct) {@link DocumentIdentifier}s in the System.
     * <strong>Important</strong>: the returned collection is <em>not</em> suitable for concurrent actions.
     */
    @NotNull
    public Set<DocumentIdentifier> getAllDocIds() {
        return new HashSet<>(corpus.getCorpus().keySet());
    }

    /**
     * @return the corpus associated with this instance.
     */
    @NotNull
    public Corpus getCorpus() {
        return corpus;
    }
}