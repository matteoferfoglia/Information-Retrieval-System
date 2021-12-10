package it.units.informationretrieval.ir_boolean_model.entities;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors.FakeDocumentIdentifier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class TermTest {

    private static final double EPSILON_FOR_DOUBLE_COMPARISON = 1e-9;

    private static final int NUMBER_OF_GENERATED_POSTINGS_FOR_MERGING = 10000;

    private static final String[] SAMPLE_CORPUS = {
            "The cat is on the table",
            "The dog is eating",
            "The car is running",
    };
    private static final Map<Posting, TermStatistics> allTermStatistics;
    private static final Map<String, Term> allTerms;
    private static final Map<Term, PostingList> termsAndPostingList;
    private static final Map<List<Term>, Term> examplesOfTermsToBeMergedAndExpectedMerged;
    private static final Supplier<Term> newTermSuitableForMergingSupplier = new Supplier<>() {
        private static final AtomicInteger counter = new AtomicInteger(0);
        private static final Term[] termsWithSameToken;

        static {
            AtomicInteger currentDocIdValue = new AtomicInteger(0);
            AtomicReference<DocumentIdentifier> currentDocId =
                    new AtomicReference<>(new FakeDocumentIdentifier(currentDocIdValue.intValue()));
            termsWithSameToken =
                    IntStream.range(0, NUMBER_OF_GENERATED_POSTINGS_FOR_MERGING)
                            .mapToObj(i -> {
                                if (Math.random() > 0.5) { // randomly decide if changing docID
                                    currentDocId.set(new FakeDocumentIdentifier(currentDocIdValue.incrementAndGet()));
                                }
                                return currentDocId.get();
                            })
                            .map(Posting::new)
                            .map(PostingList::new)
                            .map(postingList -> new Term(postingList, "sampleTerm"))
                            .toArray(Term[]::new);
        }

        @Override
        public Term get() {
            return termsWithSameToken[counter.getAndIncrement() % termsWithSameToken.length];
        }
    };

    static {
        DocumentIdentifier docId1 = new FakeDocumentIdentifier(1);
        DocumentIdentifier docId2 = new FakeDocumentIdentifier(2);
        DocumentIdentifier docId3 = new FakeDocumentIdentifier(3);

        Map<String, List<Posting>> postingsOfSomeTerms = new HashMap<>() {{
            put("the", new ArrayList<>() {{
                addAll(List.of(new Posting(docId1), new Posting(docId2), new Posting(docId3)));
            }});
            put("is", new ArrayList<>() {{
                addAll(List.of(new Posting(docId1), new Posting(docId2), new Posting(docId3)));
            }});
        }};

        termsAndPostingList = new HashMap<>() {{
            put(new Term(new PostingList(postingsOfSomeTerms.get("the")), "the"), new PostingList(postingsOfSomeTerms.get("the")));
            put(new Term(new PostingList(postingsOfSomeTerms.get("is")), "is"), new PostingList(postingsOfSomeTerms.get("is")));
        }};

        examplesOfTermsToBeMergedAndExpectedMerged = new HashMap<>() {{
            put(new ArrayList<>() {{
                add(new Term(new PostingList(
                        List.of(postingsOfSomeTerms.get("the").get(0), postingsOfSomeTerms.get("the").get(2))),
                        "the"));
                add(new Term(new PostingList(postingsOfSomeTerms.get("the").get(1)), "the"));
            }}, new Term(new PostingList(postingsOfSomeTerms.get("the")), "the"));
            put(new ArrayList<>() {{
                add(new Term(new PostingList(
                        List.of(postingsOfSomeTerms.get("is").get(0), postingsOfSomeTerms.get("is").get(2))),
                        "is"));
                add(new Term(new PostingList(postingsOfSomeTerms.get("is").get(1)), "is"));
            }}, new Term(new PostingList(postingsOfSomeTerms.get("is")), "is"));
        }};

        allTerms = new HashMap<>() {{
            put("the", new Term(new PostingList(List.of(new Posting(docId1), new Posting(docId2), new Posting(docId3))), "the"));
            put("cat", new Term(new PostingList(new Posting(docId1)), "cat"));
            put("is", new Term(new PostingList(List.of(new Posting(docId1), new Posting(docId2), new Posting(docId3))), "is"));
            put("on", new Term(new PostingList(new Posting(docId1)), "on"));
            put("table", new Term(new PostingList(new Posting(docId1)), "table"));
            put("dog", new Term(new PostingList(new Posting(docId2)), "dog"));
            put("eating", new Term(new PostingList(new Posting(docId2)), "eating"));
            put("car", new Term(new PostingList(new Posting(docId3)), "car"));
            put("running", new Term(new PostingList(new Posting(docId3)), "running"));
        }};

        allTermStatistics = new HashMap<>() {{
            put(new Posting(docId1), new TermStatistics(allTerms.get("the"), 2, 3));
            put(new Posting(docId1), new TermStatistics(allTerms.get("cat"), 1, 1));
            put(new Posting(docId1), new TermStatistics(allTerms.get("is"), 1, 3));
            put(new Posting(docId1), new TermStatistics(allTerms.get("on"), 1, 1));
            put(new Posting(docId1), new TermStatistics(allTerms.get("table"), 1, 1));
            put(new Posting(docId2), new TermStatistics(allTerms.get("the"), 1, 3));
            put(new Posting(docId2), new TermStatistics(allTerms.get("dog"), 1, 1));
            put(new Posting(docId2), new TermStatistics(allTerms.get("is"), 1, 3));
            put(new Posting(docId2), new TermStatistics(allTerms.get("eating"), 1, 1));
            put(new Posting(docId3), new TermStatistics(allTerms.get("the"), 1, 3));
            put(new Posting(docId3), new TermStatistics(allTerms.get("car"), 1, 1));
            put(new Posting(docId3), new TermStatistics(allTerms.get("running"), 1, 1));
        }};
    }

    @Benchmark(warmUpIterations = 1, iterations = 3, tearDownIterations = 1,
            commentToReport = "Merging performed on " + NUMBER_OF_GENERATED_POSTINGS_FOR_MERGING + " terms")
    static void mergeTerms() {
        Term term = newTermSuitableForMergingSupplier.get();
        for (int i = 0; i < NUMBER_OF_GENERATED_POSTINGS_FOR_MERGING - 1; i++) {
            term.merge(newTermSuitableForMergingSupplier.get());
        }
    }

    @Test
    void idf() {
        allTermStatistics.forEach((posting, termStatistics) -> {
            try {
                assertTrue(
                        allTermStatistics.get(posting).inverseDocumentFrequency()
                                - termStatistics.term().idf(SAMPLE_CORPUS.length)
                                < EPSILON_FOR_DOUBLE_COMPARISON);
            } catch (Term.NoDocumentsAssociatedWithTermException e) {
                fail(e);
            }
        });
    }

    @Test
    void merge() {
        examplesOfTermsToBeMergedAndExpectedMerged
                .forEach((termsToBeMerged, expectedMergedTerm) ->
                        assertEquals(expectedMergedTerm, termsToBeMerged.stream().reduce(Term::merge).orElseThrow()));
    }

    @Test
    void getPostingList() {
        termsAndPostingList
                .forEach((term, postingList) -> assertEquals(postingList, term.getPostingList()));
    }

    @ParameterizedTest
    @CsvSource({"a,b,-1", "b,a,1", "a,a,0"})
    void compareTo(String term1, String term2, int expectedResultOfComparison) {
        assertEquals(
                expectedResultOfComparison,
                new Term(new PostingList(), term1).compareTo(new Term(new PostingList(), term2)));
    }

    private static record TermStatistics(@NotNull Term term, int termFrequency, int documentFrequency) {
        private final static int NUMBER_OF_DOCUMENTS_IN_THE_CORPUS = SAMPLE_CORPUS.length;

        public double inverseDocumentFrequency() {
            return Math.log(NUMBER_OF_DOCUMENTS_IN_THE_CORPUS / (double) documentFrequency);
        }
    }
}