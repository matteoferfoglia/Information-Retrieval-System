package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.LineOfAFile;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.SynchronizedCounter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CorpusTest {

    private FakeCorpus corpus;

    @BeforeEach
    void setUp() {
        corpus = new FakeCorpus();
    }

    @AfterEach
    void tearDown() {
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, 100})
    void createCorpusFromDocumentCollectionAndGet(int expectedNumberOfDocuments) throws NoMoreDocIdsAvailable {
        Map<?, ?> createdCorpus = corpus.createCorpusFromDocumentCollectionAndGet(LineOfAFile.produceDocuments(expectedNumberOfDocuments));
        assertEquals(expectedNumberOfDocuments, createdCorpus.size());
    }

    @Test
    void throwExceptionWhenCreatingTheCorpusIfNoMoreDocIDasAreAvailable()
            throws NoSuchFieldException, IllegalAccessException {
        Field docIdGeneratorField = DocumentIdentifier.class.getDeclaredField("counter");
        docIdGeneratorField.setAccessible(true);
        ((SynchronizedCounter) docIdGeneratorField.get(null)).setValue(SynchronizedCounter.MAX_VALUE);
        try {
            createCorpusFromDocumentCollectionAndGet(1);    // at least one document to produce the overflow in the docId generator
            fail("An exception should have been thrown.");
        } catch (NoMoreDocIdsAvailable e) {
            assertTrue(true);// correct to be here
        }
    }

    private static class FakeCorpus extends Corpus {
    }
}