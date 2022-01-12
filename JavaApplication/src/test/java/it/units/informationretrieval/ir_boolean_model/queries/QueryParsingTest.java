package it.units.informationretrieval.ir_boolean_model.queries;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueryParsingTest {

    @ParameterizedTest
    @CsvSource({
            "a, a",
            "!!a, a",
            "!!! a, NOT a",
            "a b, (a AND b)",
            "a !b, (a AND NOTb)",
            "a | b, (a OR b)",
            "a    b, (a AND b)",
            "a     b|c, (c OR (a AND b))",
            "a&b|c||d&&&&f, ( c OR (a AND b) OR (d AND f))",
            "d&&&&e, (d AND e)",
            "!a, NOTa",
            "a&b, (a AND b)",
            "!a&b, (b AND NOT a)",
            "!(a&b), (NOT a OR NOT b)",
            "a&!b|c, (c OR (a AND NOT b))",
            "a&b&c, (a AND b AND c)",
            "(a&b)&c, (a AND b AND c)",
            "a&(b&c), (a AND b AND c)",
            "a|(b&c), (a OR (b AND c))",
            "a|(b|c), (a OR b OR c)",
            "(a|b)&c, ((a AND c)OR(b AND c))",
            "(a|b)&!c, ((a AND NOT c ) OR (b AND NOT c))",
            "(a|b)|!c, (a OR b OR NOT c)",
            "(a|b&d)|!c, (a OR NOT c OR (b AND d))",
            "(a|b&!d)|!c, (a OR NOT c OR (b AND NOT d ))",
            "((a|b)&!d)|!c, (NOT c OR (a AND NOT d) OR (b AND NOT d))",
            "((a|b)|!d)|!c, (a OR b OR NOT c OR NOT d)",
            "((a|b)|!d)&!c, ((a AND NOT c) OR (b AND NOT c) OR ( NOT c AND NOT d))"
    })
    void parse(String inputQueryString, String expectedParsedQueryString) {
        Function<String, String> whiteSpacesRemover = input -> input.replaceAll(" ", "");
        String actual = whiteSpacesRemover.apply(QueryParsing.toString(QueryParsing.parse(inputQueryString)));
        String expected = whiteSpacesRemover.apply(expectedParsedQueryString);
        assertEquals(expected, actual);
    }
}