package it.units.informationretrieval.ir_boolean_model.utils;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SoundexTest {
    @ParameterizedTest
    @CsvSource({
            "Robert, r163",
            "Rupert, r163",
            "Rubin, r150",
            "Ashcraft, a261",
            "Ashcroft, a261",
            "Tymczak, t522",
            "Pfister, p236",
            "Honeyman, h555"
    })
    void testPhoneticHash(String inputWord, String expectedHash) {
        assertEquals(expectedHash, Soundex.getPhoneticHash(inputWord));
    }
}