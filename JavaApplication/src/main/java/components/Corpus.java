package components;

import org.jetbrains.annotations.NotNull;
import util.Utility;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/** Class representing the corpus for the Information Retrieval System.
 *
 * @author  Matteo Ferfoglia */
public class Corpus {

    /** The corpus, saved as relationships (with a {@link java.util.Map})
     * between a {@link Posting} and the corresponding {@link Document}. */
    @NotNull
    private final Map<Posting.DocumentIdentifier, Document> corpus;

    /** Default Constructor. Creates an empty-corpus.
     * @throws components.Posting.DocumentIdentifier.NoMoreDocIdsAvailable If no more {@link components.Posting.DocumentIdentifier}s
     *          are available, hence the {@link Corpus} cannot be created.*/
    public Corpus(Collection<? extends Document> documents) throws Posting.DocumentIdentifier.NoMoreDocIdsAvailable {

        AtomicBoolean exceptionFlag = new AtomicBoolean(false); // becomes true if an exception is thrown in lambda function

        this.corpus = documents
                .parallelStream()
                .map(aDocument -> {
                    try {
                        return new AbstractMap.SimpleEntry<>(new Posting.DocumentIdentifier(), aDocument);
                    } catch (Posting.DocumentIdentifier.NoMoreDocIdsAvailable noMoreDocIdsAvailable) {
                        System.err.println(Utility.stackTraceToString(noMoreDocIdsAvailable));
                        exceptionFlag.set(true);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        if(exceptionFlag.get()) {
            throw new Posting.DocumentIdentifier.NoMoreDocIdsAvailable("Exception thrown.");
        }
    }

    /** Returns the head of this instance, i.e., the first documents,
     * ordered according to their {@link components.Posting.DocumentIdentifier}.
     * This method can be used for printing purposes (e.g., during testing).
     * @param howMany The number of documents to return. If it exceeds the total
     *                number of available documents, the entire corpus is returned.
     * @return A string corresponding to the head of this corpus.
     * */
    @NotNull
    public String head(int howMany) {
        int howManyDocsToReturn = Math.min(Math.max(howMany, 0), corpus.size());
        final Comparator<? super Map.Entry<Posting.DocumentIdentifier, Document>> keyComparator = Map.Entry.comparingByKey();
        return corpus .entrySet()
                      .stream()
                      .sorted(keyComparator)
                      .limit(howManyDocsToReturn)
                      .map(entry -> entry.getKey() + " = " + entry.getValue())
                      .collect(Collectors.joining("\n"));
    }

    /** Getter for the {@link #corpus}.
     * @return the {@link #corpus}. */
    @NotNull
    public Map<Posting.DocumentIdentifier,Document> getCorpus() {
        return corpus;
    }

    @Override
    public String toString() {
        return corpus.toString();
    }
}