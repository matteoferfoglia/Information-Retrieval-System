package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public enum Language {
    ARABIC("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/arabic.txt"),
    BULGARIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/bulgarian.txt"),
    CATALAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/catalan.txt"),
    CZECH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/czech.txt"),
    DANISH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/danish.txt"),
    DUTCH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/dutch.txt"),
    ENGLISH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/english.txt"),
    FINNISH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/finnish.txt"),
    FRENCH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/french.txt"),
    GERMAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/german.txt"),
    GUJARATI("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/gujarati.txt"),
    HEBREW("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/hebrew.txt"),
    HINDI("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/hindi.txt"),
    HUNGARIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/hungarian.txt"),
    INDONESIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/indonesian.txt"),
    ITALIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/italian.txt"),
    MALAYSIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/malaysian.txt"),
    NORWEGIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/norwegian.txt"),
    POLISH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/polish.txt"),
    PORTUGUESE("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/portuguese.txt"),
    ROMANIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/romanian.txt"),
    RUSSIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/russian.txt"),
    SLOVAK("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/slovak.txt"),
    SPANISH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/spanish.txt"),
    SWEDISH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/swedish.txt"),
    TURKISH("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/turkish.txt"),
    UKRAINIAN("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/ukrainian.txt"),
    VIETNAMESE("/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/vietnamese.txt"),
    UNDEFINED("");

    /**
     * The path to use if the {@link Language} is {@link #UNDEFINED}.
     */
    private static final String PATH_TO_FILE_FOR_UNDEFINED_LANGUAGE = "";
    /**
     * The stop words for the given language.
     */
    private final String[] stopWords;

    /**
     * @param pathToStopWordsDataset The path from the context root to the dataset of stop-words fot the language.
     */
    Language(@NotNull String pathToStopWordsDataset) {
        String[] tmpStopWords;  // tmp variable used to ensure the field will be initialized, also if any error occurs.
        tmpStopWords = pathToStopWordsDataset.equals(PATH_TO_FILE_FOR_UNDEFINED_LANGUAGE)
                ? new String[0]
                : Utility.readAllLines(Objects.requireNonNull(
                        getClass().getResourceAsStream(Objects.requireNonNull(pathToStopWordsDataset))))
                .stream()
                .map(stopWord -> Utility.removeInvalidCharsAndToLowerCase(stopWord, false))
                .toArray(String[]::new);
        this.stopWords = tmpStopWords;
    }

    /**
     * @return The stop words for the given language.
     */
    public String[] getStopWords() {
        return stopWords;
    }

    /**
     * Small utility class usable to get all available languages
     * and print them in a format which is directly usable for the
     * enum {@link Language}.
     * In practice, this inner class is made to create all enum items
     * of the enum class.
     */
    @SuppressWarnings("unused") // class used to create the code for this enum
    private static class AvailableLanguagesGetter {
        /**
         * Invoking this method causes the print of the enum
         * items for {@link Language}.
         */
        public static void main(String[] args) {
            final String PATH_TO_FILES_RESOURCE =
                    // first slash matters (from root)
                    "/it/units/informationretrieval/ir_boolean_model/stop_words/datasets";
            File[] stopWordsFiles = new File(
                    Objects.requireNonNull(Language.class.getResource(PATH_TO_FILES_RESOURCE)).getFile())
                    .listFiles();
            System.out.println(
                    Arrays.stream(Objects.requireNonNull(stopWordsFiles))
                            .map(file -> file.getName()
                                    .substring(0, file.getName().lastIndexOf(".")/*remove extension*/)
                                    .toUpperCase()
                                    + "(\"" + PATH_TO_FILES_RESOURCE + "/" + file.getName() + "\")")
                            .collect(Collectors.joining("," + System.lineSeparator()))
                            + "," + System.lineSeparator() + "UNDEFINED(\"" + PATH_TO_FILE_FOR_UNDEFINED_LANGUAGE + "\");");
        }
    }
}


