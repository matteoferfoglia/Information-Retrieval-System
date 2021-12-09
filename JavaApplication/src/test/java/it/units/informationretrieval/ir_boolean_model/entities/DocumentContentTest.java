package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentContentTest {

    @Test
    void getEntireTextContent() {
        final List<String> SAMPLE_CONTENTS = new ArrayList<>() {{
            addAll(Arrays.asList("a", "b", "c"));
        }};
        Document document = new FakeDocument_LineOfAFile("title", SAMPLE_CONTENTS);
        String expected = SAMPLE_CONTENTS.stream().sequential().collect(Collectors.joining(System.lineSeparator()));
        assert document.getContent() != null;
        assertEquals(expected, document.getContent().getEntireTextContent());
    }

}