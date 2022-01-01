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

    private static final int[] positionsArray = new int[]{0};
    private static final Map<Integer, Posting> mapOfSampleDocIdsAndCorrespondingPosting = new HashMap<>() {{
        put(1, new Posting(new FakeDocumentIdentifier(1), positionsArray));
        put(2, new Posting(new FakeDocumentIdentifier(2), positionsArray));
        put(3, new Posting(new FakeDocumentIdentifier(3), positionsArray));
        put(4, new Posting(new FakeDocumentIdentifier(4), positionsArray));
        put(5, new Posting(new FakeDocumentIdentifier(5), positionsArray));
    }};

    private static final Posting[] sampleArrayOfPosting = new Posting[]{
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3)};
    private static final Posting[] sampleArrayOfPostingWithDuplicates = new Posting[]{
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3)};
    private static final Posting[] sampleListOfPostingUnordered = new Posting[]{
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3)};
    private static final Posting[] sampleListOfPostingWithDuplicatesUnordered = new Posting[]{
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3),
            mapOfSampleDocIdsAndCorrespondingPosting.get(3),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(2)};
    private static final Posting[] anotherListOfPostingUnordered = new Posting[]{
            mapOfSampleDocIdsAndCorrespondingPosting.get(5),
            mapOfSampleDocIdsAndCorrespondingPosting.get(1),
            mapOfSampleDocIdsAndCorrespondingPosting.get(4)};

    private static final PostingList onePostingList = new PostingList(sampleListOfPostingWithDuplicatesUnordered);
    private static final PostingList anotherPostingList = new PostingList(anotherListOfPostingUnordered);

    @Test
    void createEmptyPostingList() {
        final int EXPECTED_NUMBER_OF_POSTINGS = 0;
        assertEquals(EXPECTED_NUMBER_OF_POSTINGS, new PostingList().size());
    }

    private static final Supplier<Posting[]> listOf1000PostingsSupplier = new Supplier<>() {
        private static final int NUMBER_OF_POSTINGS = 1000;
        private static final AtomicInteger docIdCounter = new AtomicInteger(0);
        private static final Posting[] cachedArray =
                IntStream.range(0, NUMBER_OF_POSTINGS)
                        .mapToObj(i -> new Posting(new FakeDocumentIdentifier(
                                // duplicates are possible
                                Math.random() < 0.5 ? docIdCounter.getAndIncrement() : docIdCounter.get()),
                                positionsArray))
                        .toArray(Posting[]::new);

        @Override
        public Posting[] get() {
            return cachedArray;
        }
    };

    @Benchmark(warmUpIterations = 100, iterations = 100, tearDownIterations = 100)
    static void createPostingListFromListOf1000UnorderedAndDuplicatedPostings() {
        new PostingList(listOf1000PostingsSupplier.get());
    }

    private void testConstructorWith3DistinctPostings(Posting[] sampleArrayOfPosting) {
        final int EXPECTED_NUMBER_OF_POSTINGS = 3;
        var actualPostingList = new PostingList(sampleArrayOfPosting);
        assertEquals(EXPECTED_NUMBER_OF_POSTINGS, actualPostingList.size());
        assertThatPostingsAreSortedAndDistinct(actualPostingList);
    }

    private void assertThatPostingsAreSortedAndDistinct(PostingList postingList) {
        int i = 0;
        for (var posting : postingList.toSkipList()) {
            assertEquals(mapOfSampleDocIdsAndCorrespondingPosting.get(++i/*docId in the map starts from 1*/), posting);
        }
        assertThatPostingsAreDistinct(postingList);
    }

    private void assertThatPostingsAreDistinct(PostingList postingList) {
        assertEquals(postingList.size(), postingList.toSkipList().stream().distinct().count());
    }

    @Test
    void createPostingListFromCollection() {
        testConstructorWith3DistinctPostings(sampleArrayOfPosting);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatPostingsAreSorted() {
        testConstructorWith3DistinctPostings(sampleListOfPostingUnordered);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatPostingsAreDistinct() {
        testConstructorWith3DistinctPostings(sampleArrayOfPostingWithDuplicates);
    }

    @Test
    void createPostingListFromCollectionAndAssertThatPostingsAreSortedAndDistinct() {
        testConstructorWith3DistinctPostings(sampleListOfPostingWithDuplicatesUnordered);
    }

    @Test
    void merge() {
        var postingList1 = new PostingList(
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5));
        var postingList2 = new PostingList(
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(3),
                mapOfSampleDocIdsAndCorrespondingPosting.get(4),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5));

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
        var samplePostingList = new PostingList(
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5));
        final List<Posting> EXPECTED_LIST_OF_POSTING = List.of(
                mapOfSampleDocIdsAndCorrespondingPosting.get(1),
                mapOfSampleDocIdsAndCorrespondingPosting.get(2),
                mapOfSampleDocIdsAndCorrespondingPosting.get(5));
        assertEquals(EXPECTED_LIST_OF_POSTING, samplePostingList.toSkipList().stream().toList());
    }
}