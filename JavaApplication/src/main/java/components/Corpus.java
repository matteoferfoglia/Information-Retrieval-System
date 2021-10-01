package components;

import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Class representing the corpus for the Information Retrieval System.
 *
 * @author  Matteo Ferfoglia */
public class Corpus {

    /** The corpus, saved as relationships (with a {@link java.util.Map})
     * between a {@link Posting} and the corresponding {@link Document}. */
    @NotNull
    private final Map<Posting, Document> corpus;

    /** Default Constructor. Creates an empty-corpus. */
    public Corpus() {
        // TODO : not implemented yet
        this.corpus = new ConcurrentHashMap<>();  // TODO : is this the most suitable data structure?
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /** Getter for the {@link #corpus}.
     * @return the {@link #corpus}. */
    @NotNull
    public Map<Posting,Document> getCorpus() {
        return corpus;
    }


}