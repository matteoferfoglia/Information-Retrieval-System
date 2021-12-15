package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocumentIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PostingTest {

    private static final int[] positionsArray = new int[]{0, 1, 2, 5, 9};
    private static final DocumentIdentifier docId = new FakeDocumentIdentifier(0);
    private static Posting posting;

    @BeforeEach
    void setUp() {
        posting = new Posting(docId, positionsArray);
    }

    @Test
    void getDocId() {
        assertEquals(docId, posting.getDocId());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasForwardPointer(boolean hasForwardPointer) {
        if (hasForwardPointer) {
            posting.setForwardPointer(new Posting(docId, positionsArray));
        }
        assertEquals(hasForwardPointer, posting.hasForwardPointer());
    }

    @ParameterizedTest
    @CsvSource({
            "0, -1,  1",
            "0,  0,  0",
            "0,  1, -1"})
    void compareTo(int docIdThisPosting, int docIdOtherPosting, int expectedFromComparison) {
        Posting thisPosting = new Posting(new FakeDocumentIdentifier(docIdThisPosting), positionsArray);
        Posting otherPosting = new Posting(new FakeDocumentIdentifier(docIdOtherPosting), positionsArray);
        assertEquals(expectedFromComparison, thisPosting.compareTo(otherPosting));
    }

    @Test
    void compareCreationTimeTo() throws InterruptedException {
        Posting older = new Posting(docId, positionsArray);
        Thread.sleep(1);
        Posting newer = new Posting(docId, positionsArray);
        assertTrue(older.compareCreationTimeTo(newer) < 0);
        assertTrue(newer.compareCreationTimeTo(older) > 0);
    }

    @Test
    void tf() {
        assertEquals(positionsArray.length, posting.tf());
    }
}