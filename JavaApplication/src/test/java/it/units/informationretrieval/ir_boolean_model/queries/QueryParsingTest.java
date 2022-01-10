package it.units.informationretrieval.ir_boolean_model.queries;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryParsingTest {

    @ParameterizedTest
    @CsvSource({
            "a&b|c||d&&&&f|, (((a AND b)OR c) OR (d AND f))",
            "d&&&&e, (d AND e)"
    })
    void parse(String inputQueryString, String expectedParsedQueryString) {
        Function<String, String> whiteSpacesRemover = input -> input.replaceAll(" ", "");
        String actual = whiteSpacesRemover.apply(String.valueOf(QueryParsing.parse(inputQueryString)));
        String expected = whiteSpacesRemover.apply(expectedParsedQueryString);
        assertEquals(expected, actual);
    }
}