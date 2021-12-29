package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DocumentTest {

    static final String SAMPLE_TITLE = "title";
    static final String SAMPLE_CONTENT = "content";
    static final Document SAMPLE_DOCUMENT = new FakeDocument_LineOfAFile(SAMPLE_TITLE, SAMPLE_CONTENT);

    @Test
    void testToString() {
        assertEquals("{\"" + SAMPLE_TITLE + "\": " + SAMPLE_DOCUMENT.toJson() + "}", SAMPLE_DOCUMENT.toString());
    }

    @Test
    void toJson() {
        assertEquals("{\"Content\":\"" + SAMPLE_CONTENT + "\"}", SAMPLE_DOCUMENT.toJson().replaceAll("\s*", ""));
    }
}