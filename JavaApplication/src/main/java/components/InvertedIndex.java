package components;

import org.apache.commons.collections4.trie.PatriciaTrie;
import org.jetbrains.annotations.NotNull;
import util.Properties;
import util.Utility;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

        // TODO : a thread could periodically read the current size of the InvertedIndex and print the progress bar wrt. the size of the corpus being indexed

        // Indexing
        invertedIndex.putAll(
                corpus.getCorpus()
                      .entrySet()
                      .parallelStream()
                      .map( anEntry -> {    // an entry consists of a posting (key) and the corresponding document (value)
                          List<String> tokens = Utility.tokenize(anEntry.getValue());
                          Posting posting = anEntry.getKey();

                          // Return a Map having tokens as keys and the corresponding List<Terms> as values, for the document in this entry
                          return tokens.parallelStream()
                                       .map( aToken -> new AbstractMap.SimpleEntry<>( aToken, new Term(posting, aToken) ) )
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
    }

    /** @return the dictionary as sorted {@link java.util.List} of {@link String}s (the terms). */
    public List<String> getDictionary() {
        return invertedIndex.keySet()
                            .stream()
                            .sorted()
                            .collect(Collectors.toList());
    }
}