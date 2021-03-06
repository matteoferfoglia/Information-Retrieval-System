package it.units.informationretrieval.ir_boolean_model.entities;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeCorpus;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocumentIdentifier;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocument_LineOfAFile;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.custom_types.SynchronizedCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CorpusTest {

    private static final int DEFAULT_CORPUS_SIZE_FOR_TESTS = 1000;
    private static final List<Document> documents = FakeDocument_LineOfAFile.produceDocuments(DEFAULT_CORPUS_SIZE_FOR_TESTS);
    private static final int HOW_MANY_DOCS_TO_RETRIEVE = DEFAULT_CORPUS_SIZE_FOR_TESTS / 10;
    private static final List<DocumentIdentifier> docIdsOfFirstDocumentsInCorpus =
            IntStream.range(SynchronizedCounter.MIN_VALUE, SynchronizedCounter.MIN_VALUE + HOW_MANY_DOCS_TO_RETRIEVE)
                    .mapToObj(docIdValue -> new DocumentIdentifier(new FakeDocumentIdentifier(docIdValue)))
                    .toList();
    private static FakeCorpus corpus;

    static {
        try {
            corpus = new FakeCorpus(documents); // initialized here for benchmark tests.
        } catch (NoMoreDocIdsAvailable e) {
            fail(e);
        }
    }

    @Benchmark
    static void createCorpusFromDocumentCollectionOfLength1000() throws NoMoreDocIdsAvailable {
        final int COLLECTION_LENGTH = 1000;
        Corpus.createCorpusFromDocumentCollectionAndGet(FakeDocument_LineOfAFile.produceDocuments(COLLECTION_LENGTH));
    }

    @Benchmark(commentToReport = "First " + HOW_MANY_DOCS_TO_RETRIEVE + " documents are retrieved")
    static void getFirstDocumentsFromDocId() {
        corpus.getDocumentsByDocIds(docIdsOfFirstDocumentsInCorpus);
    }

    @Benchmark(commentToReport = "First " + HOW_MANY_DOCS_TO_RETRIEVE + " documents are retrieved")
    static void getHeadOfCorpus() {
        corpus.head(HOW_MANY_DOCS_TO_RETRIEVE);
    }

    public static void main(String[] args) {
        System.out.println(corpus.getDocumentsByDocIds(docIdsOfFirstDocumentsInCorpus));
    }

    @BeforeEach
    void setUp() throws NoMoreDocIdsAvailable {
        corpus = new FakeCorpus(documents);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 5, 10, 100})
    void createCorpusFromDocumentCollectionAndGet(int expectedNumberOfDocuments) throws NoMoreDocIdsAvailable {
        Map<?, ?> createdCorpus = Corpus.createCorpusFromDocumentCollectionAndGet(FakeDocument_LineOfAFile.produceDocuments(expectedNumberOfDocuments));
        assertEquals(expectedNumberOfDocuments, createdCorpus.size());
    }

    @Test
    void throwExceptionWhenCreatingTheCorpusIfNoMoreDocIDasAreAvailable() {

        Function<Integer, Integer> setDocIdGeneratorValueAndGeOldValue = valueToSet -> {
            try {
                Field docIdGeneratorField = DocumentIdentifier.class.getDeclaredField("counter");
                docIdGeneratorField.setAccessible(true);
                SynchronizedCounter docIdGenerator = (SynchronizedCounter) docIdGeneratorField.get(null);
                int oldValue = docIdGenerator.getValue();
                docIdGenerator.setValue(valueToSet);
                return oldValue;
            } catch (IllegalAccessException | NoSuchFieldException e) {
                fail(e);
                return 0;
            }
        };
        int realDocIdGeneratorValue = setDocIdGeneratorValueAndGeOldValue.apply(SynchronizedCounter.MAX_VALUE);
        try {
            createCorpusFromDocumentCollectionAndGet(1);    // at least one document to produce the overflow in the docId generator
            setDocIdGeneratorValueAndGeOldValue.apply(realDocIdGeneratorValue);
            fail("An exception should have been thrown.");
        } catch (NoMoreDocIdsAvailable e) {
            assertTrue(true);// correct to be here
            setDocIdGeneratorValueAndGeOldValue.apply(realDocIdGeneratorValue);
        }
    }

    @Test
    void getDocuments() {
        var docIdsInCorpus = corpus.getCorpus().keySet().stream().toList();
        assertEquals(new HashSet<>(documents), new HashSet<>(corpus.getDocumentsByDocIds(docIdsInCorpus)));
    }

    @Test
    void head() {
        var head = corpus.head(HOW_MANY_DOCS_TO_RETRIEVE);
        assertEquals(HOW_MANY_DOCS_TO_RETRIEVE, head.split(System.lineSeparator()).length);
    }

}