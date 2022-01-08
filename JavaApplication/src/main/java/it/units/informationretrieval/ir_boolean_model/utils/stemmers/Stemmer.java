package it.units.informationretrieval.ir_boolean_model.utils.stemmers;

import it.units.informationretrieval.ir_boolean_model.entities.Language;
import org.jetbrains.annotations.NotNull;

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
        PORTER,
        SNOWBALL
    }
}
