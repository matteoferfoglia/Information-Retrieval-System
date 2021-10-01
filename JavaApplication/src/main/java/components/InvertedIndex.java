package components;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;
import util.Properties;
import util.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/** An instance of this class is an Inverted Index for a {@link Corpus}
 * of documents.
 * @author Matteo Ferfoglia*/
public class InvertedIndex {

    /** The inverted index, i.e., a {@link Map} having tokens as keys and a {@link Term}
     * as corresponding values, where the {@link Term} in the entry, if tokenized,
     * returns the corresponding key. */
    @NotNull
    private final Map<String,Term> invertedIndex;   // a Term has a posting list

    /*/** The phonetic hash. */   /*  // TODO : implement phoneticHash
    @NotNull
    private final ConcurrentHashMap<String, List<Term>> phoneticHash;
    */

    /** Constructor. */
    public InvertedIndex(@NotNull Corpus corpus) {

        AtomicReference<Double> progressValue = new AtomicReference<>((double) 0);   // used only to show the indexing progress
        AtomicLong numberOfAlreadyProcessedDocuments = new AtomicLong(0);

        short invertedIndexType = Short.parseShort( Properties.appProperties.getProperty("index.dataStructure.type") );
        switch( invertedIndexType ) {
            case 1 :
                invertedIndex = new ConcurrentHashMap<>();
                break;
            case 2 :
                invertedIndex = new PatriciaTrie<>();
                break;
            default:    // 0 or anything else
                invertedIndex = new Hashtable<>();
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Thread progressControllerThread = new Thread( () -> {
            final double epsilon = 0.001;
            double oldProgressValue = progressValue.get();
            progressValue.set(Math.round( (0.0+numberOfAlreadyProcessedDocuments.get()) / corpus.size() * 10000) / 100.0);
            if( progressValue.get() - oldProgressValue > epsilon ) {
                System.out.print(progressValue + " % \t ");
            }
        });
        System.out.println("Indexing started");
        final int DELAY_PROGRESS_CONTROLLER = 5;    // seconds
        scheduler.scheduleAtFixedRate(progressControllerThread, DELAY_PROGRESS_CONTROLLER, DELAY_PROGRESS_CONTROLLER, TimeUnit.SECONDS);

        // Indexing
        invertedIndex.putAll(
                corpus.getCorpus()
                      .entrySet()
                      .parallelStream()
                      .map( anEntry -> {
                          List<String> tokens = Utility.tokenize(anEntry.getValue());   // TODO : tokenization should return as a map also the position where the token is found in the document (for phrase query)
                          Posting.DocumentIdentifier docIdThisDocument = anEntry.getKey();

                          // Return a Map having tokens as keys and the corresponding List<Terms> as values, for the document in this entry
                          Set<Map.Entry<String,Term>> entrySet =
                                 tokens.parallelStream()
                                       .map( aToken -> new AbstractMap.SimpleEntry<>( aToken, new Term(new Posting(docIdThisDocument), aToken) ) )
                                       .collect(
                                           Collectors.toConcurrentMap(
                                               Map.Entry::getKey,
                                               Map.Entry::getValue,
                                               (term1, term2) -> {  // Merge terms with the same token
                                                   term1.merge(term2);
                                                   return term1;
                                               }
                                           )
                                       )
                                      .entrySet();
                          numberOfAlreadyProcessedDocuments.getAndIncrement();
                          return entrySet;
                      })
                      .flatMap(Collection::stream /*outputs all entries (token,term) from all the documents*/)
                      .collect(
                            Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (term1, term2) -> {  // Merge terms with the same token
                                    term1.merge(term2);
                                    return term1;
                                }
                            )
                      )
        );

        // Join the thread used to print the indexing progress
        scheduler.shutdown();
        try {
            short awaitTermination = 10;    // us
            if (!scheduler.awaitTermination(awaitTermination, TimeUnit.MICROSECONDS)) {
                System.out.println("Still waiting after " + awaitTermination + " us.");
            }
        } catch (InterruptedException e) {
            System.err.println("Indexing progress controller thread not joined.");
            e.printStackTrace();
        }
        System.out.println("\nIndexing ended");
    }

    /** @return the dictionary as sorted {@link java.util.List} of {@link String}s (the terms). */
    public List<String> getDictionary() {
        return invertedIndex.keySet()
                            .stream()
                            .sorted()
                            .collect(Collectors.toList());
    }
}