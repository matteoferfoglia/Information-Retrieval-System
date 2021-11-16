package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Class representing the corpus for the Information Retrieval System.
 *
 * @author Matteo Ferfoglia
 */
public class Corpus implements Serializable {

    /**
     * The corpus, saved as relationships (with a {@link java.util.Map})
     * between a {@link Posting} and the corresponding {@link Document}.
     */
    @NotNull
    private final Map<Posting.DocumentIdentifier, Document> corpus;

    /**
     * Default Constructor. Creates an empty-corpus.
     *
     * @throws Posting.DocumentIdentifier.NoMoreDocIdsAvailable If no more {@link Posting.DocumentIdentifier}s
     *                               are available, hence the {@link Corpus} cannot be created.
     */
    public Corpus(Collection<? extends Document> documents) throws Posting.DocumentIdentifier.NoMoreDocIdsAvailable {

        AtomicBoolean exceptionFlag = new AtomicBoolean(false); // becomes true if an exception is thrown in lambda function

        this.corpus = documents
                .stream().unordered().parallel()
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

        if (exceptionFlag.get()) {
            throw new Posting.DocumentIdentifier.NoMoreDocIdsAvailable("Exception thrown.");
        }
    }


    /**
     * @param postings The {@link List} of {@link Posting}s for which the
     *                 corresponding {@link List} of {@link Document}s is
     *                 wanted.
     * @return the {@link List} of {@link Document}s in this {@link Corpus}
     * for which the {@link Posting.DocumentIdentifier} is present
     * in any of the {@link Posting} in the parameter.
     */
    @NotNull
    public List<Document> getDocuments(@NotNull List<Posting> postings) {
        return Objects.requireNonNull(postings, "List of postings cannot be null")
                .stream().unordered().parallel()
                .map(aPosting -> corpus.get(aPosting.getDocId()))
                .filter(Objects::nonNull)
                .distinct() // more postings may refer to the same document
                .collect(Collectors.toList());
    }

    /**
     * Returns the head of this instance, i.e., the first documents,
     * ordered according to their {@link Posting.DocumentIdentifier}.
     * This method can be used for printing purposes (e.g., during testing).
     *
     * @param howMany The number of documents to return. If it exceeds the total
     *                number of available documents, the entire corpus is returned.
     * @return A string corresponding to the head of this corpus.
     */
    @NotNull
    public String head(int howMany) {
        int howManyDocsToReturn = Math.min(Math.max(howMany, 0), corpus.size());
        final Comparator<? super Map.Entry<Posting.DocumentIdentifier, Document>> keyComparator = Map.Entry.comparingByKey();
        return corpus.entrySet()
                .stream().sequential()
                .sorted(keyComparator)
                .limit(howManyDocsToReturn)
                .map(entry -> entry.getKey() + " = " + entry.getValue())
                .collect(Collectors.joining("\n"));
    }

    /**
     * Getter for the {@link #corpus}.
     *
     * @return the {@link #corpus}.
     */
    @NotNull
    public Map<Posting.DocumentIdentifier, Document> getCorpus() {
        return corpus;
    }

    @Override
    public String toString() {
        return corpus.toString();
    }

    /**
     * @return The size (number of documents) in this {@link Corpus}.
     */
    public int size() {
        return corpus.size();
    }
}