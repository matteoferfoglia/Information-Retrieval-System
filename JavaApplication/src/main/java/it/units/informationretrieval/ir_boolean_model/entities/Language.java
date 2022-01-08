package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public enum Language {
    ARABIC("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/arabic.txt"),
    BULGARIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/bulgarian.txt"),
    CATALAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/catalan.txt"),
    CZECH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/czech.txt"),
    DANISH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/danish.txt"),
    DUTCH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/dutch.txt"),
    ENGLISH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/english.txt"),
    FINNISH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/finnish.txt"),
    FRENCH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/french.txt"),
    GERMAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/german.txt"),
    GUJARATI("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/gujarati.txt"),
    HEBREW("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/hebrew.txt"),
    HINDI("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/hindi.txt"),
    HUNGARIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/hungarian.txt"),
    INDONESIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/indonesian.txt"),
    ITALIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/italian.txt"),
    MALAYSIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/malaysian.txt"),
    NORWEGIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/norwegian.txt"),
    POLISH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/polish.txt"),
    PORTUGUESE("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/portuguese.txt"),
    ROMANIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/romanian.txt"),
    RUSSIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/russian.txt"),
    SLOVAK("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/slovak.txt"),
    SPANISH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/spanish.txt"),
    SWEDISH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/swedish.txt"),
    TURKISH("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/turkish.txt"),
    UKRAINIAN("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/ukrainian.txt"),
    VIETNAMESE("src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets/vietnamese.txt"),
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
        try {
            tmpStopWords = pathToStopWordsDataset.equals(PATH_TO_FILE_FOR_UNDEFINED_LANGUAGE)
                    ? new String[0]
                    : Files.readAllLines(Path.of(Objects.requireNonNull(pathToStopWordsDataset)))
                    .stream()
                    .map(stopWord -> Utility.normalize(stopWord, false))
                    .toArray(String[]::new);
        } catch (IOException e) {
            tmpStopWords = new String[0];
            System.err.println("In class " + getClass().getCanonicalName() + System.lineSeparator()
                    + "\t I/O errors occurred when trying to read stop-words from file. No stop-words available.");
            e.printStackTrace();
        }
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
    private static class AvailableLanguagesGetter {
        /**
         * Invoking this method causes the print of the enum
         * items for {@link Language}.
         */
        public static void main(String[] args) {
            final String PATH_TO_FILES =
                    "src/main/resources/it/units/informationretrieval/ir_boolean_model/stop_words/datasets";
            File[] stopWordsFiles = new File(PATH_TO_FILES).listFiles();
            System.out.println(
                    Arrays.stream(Objects.requireNonNull(stopWordsFiles))
                            .map(file -> file.getName()
                                    .substring(0, file.getName().lastIndexOf(".")/*remove extension*/)
                                    .toUpperCase()
                                    + "(\"" + PATH_TO_FILES + "/" + file.getName() + "\")")
                            .collect(Collectors.joining("," + System.lineSeparator()))
                            + "," + System.lineSeparator() + "UNDEFINED(\"" + PATH_TO_FILE_FOR_UNDEFINED_LANGUAGE + "\");");
        }
    }
}


