import components.Corpus;
import components.InvertedIndex;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * This class represents an Information Retrieval System.
 * @author Matteo Ferfoglia
 */
public class InformationRetrievalSystem implements Serializable {

    /** The {@link Corpus}. */
    @NotNull
    private final Corpus corpus;

    /** The {@link InvertedIndex}. */
    @NotNull
    private final InvertedIndex invertedIndex;

    /** Constructor.
     * @param corpus The {@link Corpus} to use in this {@link InformationRetrievalSystem}.*/
    public InformationRetrievalSystem(@NotNull Corpus corpus) {
        this.corpus = Objects.requireNonNull(corpus, "The corpus cannot be null");
        this.invertedIndex = new InvertedIndex(corpus);
    }

}
