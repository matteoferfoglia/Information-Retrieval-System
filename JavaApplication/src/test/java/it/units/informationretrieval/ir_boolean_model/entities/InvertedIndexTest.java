package it.units.informationretrieval.ir_boolean_model.entities;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.fail;

class InvertedIndexTest {

    static {
        try {
            Properties.loadProperties();// TODO: is needed to use properties? Maybe better to have a class with public parameters
        } catch (IOException e) {
            fail(e);
        }
    }

    @Benchmark(warmUpIterations = 1, iterations = 3, tearDownIterations = 1)
    static void createInvertedIndexForMovieDataset() throws NoMoreDocIdsAvailable, URISyntaxException {
        new InvertedIndex(Movie.createCorpus());
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void indexCorpusAndGet() {
    }

    @Test
    void getEntrySetOfTokensAndCorrespondingTermsFromADocument() {
    }

    @Test
    void getDictionary() {
    }

    @Test
    void getCorpus() {
    }

    @Test
    void getPostingListForToken() {
    }
}