package it.units.informationretrieval.ir_boolean_model.queries;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Representation of a phrase.
 */
public class Phrase {
    /**
     * The phrase as list of words.
     */
    private final List<String> phrase;

    /**
     * Constructor.
     *
     * @param phrase the phrase as {@link List} of {@link String}.
     */
    public Phrase(@NotNull final List<String> phrase) {
        this.phrase = Objects.requireNonNull(phrase);
    }

    /**
     * Constructor.
     *
     * @param phrase the phrase as {@link List} of {@link String}.
     */
    public Phrase(@NotNull final String... phrase) {
        this.phrase = Arrays.asList(Objects.requireNonNull(phrase));
    }

    /**
     * @return the phrase as {@link List} of words.
     */
    @NotNull
    public List<String> getListOfWords() {
        return phrase;
    }

    /**
     * @param position The position of a word in this phrase.
     * @return the word at the specified position.
     * @throws IndexOutOfBoundsException if the position is out of range
     *                                   <code>(position < 0 || position >= size())</code>
     */
    public String getWordAt(int position) {
        return phrase.get(position);
    }

    /**
     * @return the number of words of the phrase.
     */
    public int size() {
        return phrase.size();
    }

    /**
     * @return the phrase as array of words.
     */
    @NotNull
    public String[] getArrayOfWords() {
        return phrase.toArray(String[]::new);
    }
}
