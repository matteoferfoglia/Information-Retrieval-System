package it.units.informationretrieval.ir_boolean_model.utils;

import org.jetbrains.annotations.NotNull;

import java.util.stream.IntStream;

/**
 * Class implementing the Soundex algorithm for spelling correction.
 */
public abstract class Soundex {

    /**
     * @param inputWord A word.
     * @return the phonetic hash of the input word (lowercase).
     */
    public static String getPhoneticHash(@NotNull final String inputWord) {
        var inputWord_ = inputWord.strip().toLowerCase();
        if (!inputWord_.isBlank()) {
            if (inputWord_.length() == 1) {
                return inputWord_;
            } else {
                return padWith0IfNeededAndGetFirst4LettersOf(
                        inputWord_.charAt(0) +
                                removeAllOccurrencesOf0ButFirstLetter(
                                        replaceLettersWithSoundexDigits(
                                                keepOnlyTheFirstLetterIfTwoOrMoreLettersWithTheSameSoundexDigitAreAdjacentOrSeparatedByHorWorY(inputWord_)))
                                        .substring(1));
            }
        } else {
            return "";
        }
    }

    private static String keepOnlyTheFirstLetterIfTwoOrMoreLettersWithTheSameSoundexDigitAreAdjacentOrSeparatedByHorWorY(String inputString) {
        char[] letters = inputString.toCharArray();
        char[] digits = replaceLettersWithSoundexDigits(inputString).toCharArray();
        StringBuilder output = new StringBuilder().append(letters[0]);
        int[] magicSeparationLetters = {'h', 'w', 'y'};
        for (int i = 1; i < digits.length; i++) {
            if (digits[i] != digits[i - 1]) {
                final int finalI = i;
                if (i < 2 /* impossible to have a magic letter in the middle if we are currently examining the 1st or 2nd letter */
                        /* if i>2 check not to be present magic letters in the middle of two letters with the same soundex digit */
                        || IntStream.of(magicSeparationLetters).noneMatch(x -> x == (int) letters[finalI - 1])
                        || digits[i] != digits[i - 2]) {
                    output.append(letters[i]);
                }
            }
        }
        return output.toString();
    }

    private static String replaceLettersWithSoundexDigits(String inputString) {
        return inputString
                .replaceAll("[aeiouhwy]", "0")
                .replaceAll("[bfpv]", "1")
                .replaceAll("[cgjkqsxz]", "2")
                .replaceAll("[dt]", "3")
                .replaceAll("[l]", "4")
                .replaceAll("[mn]", "5")
                .replaceAll("[r]", "6");
    }

    private static String removeAllOccurrencesOf0ButFirstLetter(String inputString) {
        return inputString.charAt(0) + inputString.substring(1).replaceAll("0", "");
    }

    private static String padWith0IfNeededAndGetFirst4LettersOf(String inputString) {
        final int TARGET_WORD_LENGTH = 4;
        inputString = inputString.length() > TARGET_WORD_LENGTH ? inputString.substring(0, TARGET_WORD_LENGTH) : inputString;
        return String.format("%-" + TARGET_WORD_LENGTH + "s", inputString).replaceAll(" ", "0");
    }

}