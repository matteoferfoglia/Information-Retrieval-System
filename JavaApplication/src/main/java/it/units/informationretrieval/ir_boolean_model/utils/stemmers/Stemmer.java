package it.units.informationretrieval.ir_boolean_model.utils.stemmers;

import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Interface for stemmer.
 */
public interface Stemmer {

    /**
     * {@link BiFunction} that takes a {@link Stemmer} and a
     * {@link Language} and returns the array of the stemmed stop-words
     * for the given language, using the given stemmer.
     */
    BiFunction<@NotNull AvailableStemmer, @NotNull Language, @NotNull String[]> getStemmedStopWords = new BiFunction<>() {

        /**
         * Caches the stemmed version of {@link Language#getStopWords() stop-words},
         * according to the {@link Stemmer}.
         */
        @NotNull
        static final ConcurrentMap<@NotNull AvailableStemmer, @NotNull ConcurrentMap<@NotNull Language, @NotNull String[]>>
                stemmedStopWords =
                // in-place initialization, but also lazy initialization could be done
                //  (to avoid computing and storing immediately all stemmed stop-words for each language
                //   and for each stemmer, in fact: stemmer is a system property and a corpus generally
                //   has one language), but this is easier to understand
                Arrays.stream(AvailableStemmer.values())
                        .map(aStemmer -> new Pair<>(aStemmer, Arrays.stream(Language.values())
                                .map(language -> new Pair<>(language, Arrays.stream(language.getStopWords())
                                        .map(stopWord -> getStemmer(aStemmer).stem(stopWord, language))
                                        .toArray(String[]::new)))
                                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue))))
                        .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue));

        @Override
        @NotNull
        public String[] apply(@NotNull AvailableStemmer stemmer, @NotNull Language language) {
            return stemmedStopWords.get(stemmer).get(language);
        }
    };

    /**
     * @param stemmer the desired stemmer.
     * @return the desired stemmer.
     */
    static Stemmer getStemmer(@NotNull AvailableStemmer stemmer) {
        return switch (stemmer) {
            case NO_STEMMING -> new NoStemming();
            case PORTER -> new PorterStemmer();
        };
    }

    /**
     * @param input The input <strong>word</strong> to be stemmed.
     * @return the stemmed word.
     */
    String stem(@NotNull String input, @NotNull Language language);

    /**
     * The available stemmers
     */
    enum AvailableStemmer {
        NO_STEMMING,
        PORTER;

        /**
         * @return the enum for a given {@link Stemmer} instance.
         * @throws IllegalArgumentException if the given instance is unknown.
         */
        public static AvailableStemmer fromStemmer(@Nullable Stemmer stemmer) {
            if (stemmer == null || stemmer instanceof NoStemming) {
                return NO_STEMMING;
            } else if (stemmer instanceof PorterStemmer) {
                return PORTER;
            } else {
                throw new IllegalArgumentException("Unknown stemmer " + stemmer);
            }
        }

        /**
         * "Override" {@link Enum#valueOf(Class, String)}.
         */
        public static AvailableStemmer valueOf_(String valueAsString) {
            if (valueAsString == null || valueAsString.equalsIgnoreCase("null")) {
                return NO_STEMMING;
            }
            var tmp = Arrays.stream(AvailableStemmer.values())
                    .filter(enumVal -> enumVal.name().equalsIgnoreCase(valueAsString))
                    .toList();
            if (tmp.size() == 1) {
                return tmp.get(0);
            } else {
                System.err.println("Invalid enum \"" + valueAsString + "\". Valid possibilities are: "
                        + Arrays.toString(AvailableStemmer.values()) + ". "
                        + NO_STEMMING + " returned.");
                return NO_STEMMING;
            }
        }
    }

    class NoStemming implements Stemmer {
        @Override
        public String stem(@NotNull String input, @NotNull Language ignored) {
            return input;
        }
    }
}
