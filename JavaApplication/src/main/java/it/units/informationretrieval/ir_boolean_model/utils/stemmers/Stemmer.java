package it.units.informationretrieval.ir_boolean_model.utils.stemmers;

import it.units.informationretrieval.ir_boolean_model.entities.Language;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Interface for stemmer.
 */
public interface Stemmer {

    /**
     * @param stemmer the desired stemmer.
     * @return the desired stemmer.
     */
    static Stemmer getStemmer(@NotNull AvailableStemmer stemmer) {
        return switch (stemmer) {
            case NO_STEMMING -> (input, language) -> input;
            case PORTER -> new PorterStemmer();
            case SNOWBALL -> new SnowballStemmer();
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
        PORTER,
        SNOWBALL;

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
}
