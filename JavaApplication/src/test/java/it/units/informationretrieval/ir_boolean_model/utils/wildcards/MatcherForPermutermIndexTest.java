package it.units.informationretrieval.ir_boolean_model.utils.wildcards;

import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MatcherForPermutermIndexTest {

    private final Language languageForTests = Language.ENGLISH;
    private final Stemmer stemmer = Utility.getStemmer() == null
            ? Stemmer.getStemmer(Stemmer.AvailableStemmer.NO_STEMMING)
            : Utility.getStemmer();

    @ParameterizedTest
    @CsvSource({
            "in*tigation, investigation, true",
            "in*tigation, instigation, true",
            "in*i*ation, instigation, true",
            "in*i*ation, invitation, true",
            "ma*h, math, true",
            "ma*h, mach, true",
            "b*ry, boundary, true",
            "*ry, theory, true",
            "*the, the, true",
            "*t*, the, true",
            "*t*, cat, true",
            "*t*, table, true",
            "*t*, t, true",
            "*t*, at, true",
            "*t*, pattern, true",
            "*, foo, true",
            "**, foo, true",
            "***, foo, true",
            "ca*, car, true",
            "ca*, cat, true",
            "ca*, ca, true",
            "tabl*e, table, true",
            "*bl*e, table, true",
            "*bl*e, blue, true",
            "*am, jam, true",
            "*am, yhisam, true"
    })
    void isWildcardQueryCompatibleWithStemmedTokenFromIndex(
            @NotNull String unstemmedInputWildcardQuery, @NotNull String unstemmedTokenFromIndex, boolean compatible) {
        String stemmedTokenFromIndex = stemmer.stem(unstemmedTokenFromIndex, languageForTests);
        boolean compatible_actualValue = MatcherForPermutermIndex.isWildcardQueryCompatibleWithStemmedTokenFromIndex(
                unstemmedInputWildcardQuery, stemmedTokenFromIndex, languageForTests);
        assertEquals(compatible, compatible_actualValue);
    }
}