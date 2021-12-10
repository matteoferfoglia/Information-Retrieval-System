package it.units.informationretrieval.ir_boolean_model;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;

import static it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest.randomTokenFromDictionaryOfMovieInvertedIndex;
import static org.junit.jupiter.api.Assertions.fail;

class InformationRetrievalSystemTest {

    static InformationRetrievalSystem irs;

    static {
        PrintStream realStdOut = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));    // ignore stdout
        try {
            irs = new InformationRetrievalSystem(Movie.createCorpus());
        } catch (NoMoreDocIdsAvailable | URISyntaxException e) {
            fail(e);
        }
        System.setOut(realStdOut);
    }

    @Benchmark(warmUpIterations = 100, tearDownIterations = 100, commentToReport = "Token randomly taken from the dictionary associated to the movie corpus.")
    static void getListOfPostingForToken() {    // method already tested by the inverted index class test, which is the actual responsible to retrieve posting lists
        irs.getListOfPostingForToken(randomTokenFromDictionaryOfMovieInvertedIndex.get());
    }
}