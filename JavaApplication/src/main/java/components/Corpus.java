package components;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

/** Class representing the corpus for the Information Retrieval System.
 *
 * @author  Matteo Ferfoglia */
public class Corpus {

    /** The corpus, saved as relationships (with a {@link java.util.Map})
     * between a {@link Posting} and the corresponding {@link Document}. */
    @NotNull
    private final Map<Posting, Document> corpus;

    public Corpus() {
        // TODO : not implemented yet
        this.corpus = new HashMap<>();  // TODO : is HashMap the most suitable data structure?
        throw new UnsupportedOperationException("Not implemented yet");
    }


}
