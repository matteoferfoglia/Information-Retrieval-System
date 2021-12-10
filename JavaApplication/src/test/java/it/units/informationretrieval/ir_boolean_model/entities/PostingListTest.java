package it.units.informationretrieval.ir_boolean_model.entities;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocumentIdentifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PostingListTest {

    private static final Map<Integer, Posting> mapOfSampleDocIdsAndCorrespondingPosting = new HashMap<>() {{
        put(1, new Posting(new FakeDocumentIdentifier(1)));
        put(2, new Posting(new FakeDocumentIdentifier(2)));
        put(3, new Posting(new FakeDocumentIdentifier(3)));
        put(4, new Posting(new FakeDocumentIdentifier(4)));
        put(5, new Posting(new FakeDocumentIdentifier(5)));
    }};

    private static final List<Posting> sampleListOfPosting = List.of(
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3));
    private static final List<Posting> sampleListOfPostingWithDuplicates = List.of(
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3));
    private static final List<Posting> sampleListOfPostingUnordered = List.of(
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3));
    private static final List<Posting> sampleListOfPostingWithDuplicatesUnordered = List.of(
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2));
    private static final List<Posting> anotherListOfPostingUnordered = List.of(
            mapOfSampleDocIdsAndCorrespondingPosting.get(5),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(4));

    private static final PostingList onePostingList = new PostingList(sampleListOfPostingWithDuplicatesUnordered);
    private static final PostingList anotherPostingList = new PostingList(anotherListOfPostingUnordered);

    @Test
    void createEmptyPostingList() {
        final int EXPECTED_NUMBER_OF_POSTINGS = 0;
        assertEquals(EXPECTED_NUMBER_OF_POSTINGS, new PostingList().size());
    }

    private static final Supplier<List<Posting>> listOf1000PostingsSupplier = new Supplier<>() {
        private static final int NUMBER_OF_POSTINGS = 1000;
        private static final AtomicInteger docIdCounter = new AtomicInteger(0);
        private static final List<Posting> cachedList =
                IntStream.range(0, NUMBER_OF_POSTINGS)
                        .mapToObj(i -> new Posting(new FakeDocumentIdentifier(
                                // duplicates are possible
                                Math.random() < 0.5 ? docIdCounter.getAndIncrement() : docIdCounter.get())))
                        .toList();

        @Override
        public List<Posting> get() {
            return cachedList;
        }
    };

    @Benchmark(warmUpIterations = 100, iterations = 100, tearDownIterations = 100)
    static void createPostingListFromListOf1000UnorderedAndDuplicatedPostings() {
        new PostingList(listOf1000PostingsSupplier.get());
    }

    private void testConstructorWith3DistinctPostings(List<Posting> sampleListOfPosting) {
        final int EXPECTED_NUMBER_OF_POSTINGS = 3;
        var actualPostingList = new PostingList(sampleListOfPosting);
        assertEquals(EXPECTED_NUMBER_OF_POSTINGS, actualPostingList.size());
        assertThatPostingsAreSortedAndDistinct(actualPostingList);
    }

    private void assertThatPostingsAreSortedAndDistinct(PostingList postingList) {
        int i = 0;
        for (var posting : postingList.toListOfPostings()) {
            assertEquals(mapOfSampleDocIdsAndCorrespondingPosting.get(++i/*docId in the map starts from 1*/), posting);
        }
        assertThatPostingsAreDistinct(postingList);
    }

    private void assertThatPostingsAreDistinct(PostingList postingList) {
        assertEquals(postingList.size(), postingList.toListOfPostings().stream().distinct().count());
    }

    @Test
    void createPostingListFromCollection() {
        testConstructorWith3DistinctPostings(sampleListOfPosting);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatPostingsAreSorted() {
        testConstructorWith3DistinctPostings(sampleListOfPostingUnordered);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatPostingsAreDistinct() {
        testConstructorWith3DistinctPostings(sampleListOfPostingWithDuplicates);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatPostingsAreSortedAndDistinct() {
        testConstructorWith3DistinctPostings(sampleListOfPostingWithDuplicatesUnordered);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatCorrectNumberOfForwardPointersAreSet() {
        // Rule: for a postingList of P postings, use F = floor(sqrt(P)) evenly spaced
        //  (with space S = floor(P/F) ) forward pointers

        var postingList = new PostingList(sampleListOfPostingWithDuplicatesUnordered);
        int P = postingList.size();
        assert P > 0;
        int expectedNumberOfForwardPointers = (int) Math.floor(Math.sqrt(P));
        int actualNumberOfForwardPointers = (int) postingList.toListOfPostings()
                .stream()
                .filter(Posting::hasForwardPointer)
                .count();
        assertEquals(expectedNumberOfForwardPointers, actualNumberOfForwardPointers);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatForwardPointersAreSetAtCorrectPosition() {
        // Rule: for a postingList of P postings, use F = ceil(sqrt(P)) evenly spaced
        //  (with space S = floor(P/F) ) forward pointers; last posting is never a forward pointer.

        var postingList = new PostingList(sampleListOfPostingWithDuplicatesUnordered);
        int P = postingList.size();
        assert P > 0;
        int expectedNumberOfForwardPointers = (int) Math.ceil(Math.sqrt(P));
        List<Integer> expectedPositionOfForwardPointers = IntStream.range(0, P)
                .filter(i -> i % expectedNumberOfForwardPointers == 0)
                .filter(i -> i < P - 1) // last posting is never a forward pointer
                .boxed()
                .toList();
        List<Integer> actualPositionOfForwardPointers = IntStream.range(0, P)
                .filter(i -> postingList.toListOfPostings().get(i).hasForwardPointer())
                .boxed()
                .toList();
        assertEquals(expectedPositionOfForwardPointers, actualPositionOfForwardPointers);
    }

    @Test
    void merge() {
        var postingList1 = new PostingList(List.of(
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5)));
        var postingList2 = new PostingList(List.of(
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(3),
                mapOfSampleDocIdsAndCorrespondingPosting.get(4),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5)));

        postingList1.merge(postingList2);

        final int EXPECTED_SIZE_OF_MERGED_POSTING_LIST = 5;

        assertEquals(EXPECTED_SIZE_OF_MERGED_POSTING_LIST, postingList1.size());
        assertThatPostingsAreSortedAndDistinct(postingList1);
    }

    @Benchmark(commentToReport = "Merges two unordered posting lists with one posting in common")
    static void mergePostingListsOfSize3() {
        onePostingList.merge(anotherPostingList);
    }

    @Benchmark(warmUpIterations = 100, iterations = 100, tearDownIterations = 100)
    static void mergeEqualPostingListsOfSize1000() {
        onePostingList.merge(anotherPostingList);
    }

    @Test
    void toListOfPostings() {
        var samplePostingList = new PostingList(List.of(
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5)));
        final List<Posting> EXPECTED_LIST_OF_POSTING = List.of(
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5));
        assertEquals(EXPECTED_LIST_OF_POSTING, samplePostingList.toListOfPostings());
    }
}