package it.units.informationretrieval.ir_boolean_model;

import benchmark.Benchmark;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies.MovieCorpusFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static it.units.informationretrieval.ir_boolean_model.entities.InvertedIndexTest.randomTokenFromDictionaryOfMovieInvertedIndex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class InformationRetrievalSystemTest {

    private static final PrintStream realStdOut = System.out;
    private static InformationRetrievalSystem irs;

    static {
        ignoreStdOut();
        try {
            irs = new InformationRetrievalSystem(new MovieCorpusFactory().createCorpus());
        } catch (NoMoreDocIdsAvailable | IOException e) {
            fail(e);
            Logger.getLogger(
                    InformationRetrievalSystemTest.class.getCanonicalName()).log(Level.SEVERE, e.getMessage(), e);
        }
        reSetRealStdOut();
    }

    private static void ignoreStdOut() {
        System.setOut(new PrintStream(new ByteArrayOutputStream()));    // ignore stdout
    }

    private static void reSetRealStdOut() {
        System.setOut(realStdOut);
    }

    @Benchmark(warmUpIterations = 100, tearDownIterations = 100, commentToReport = "Token randomly taken from the dictionary associated to the movie corpus.")
    static void getListOfPostingForToken() {    // method already tested by the inverted index class test, which is the actual responsible to retrieve posting lists
        irs.getListOfPostingForToken(randomTokenFromDictionaryOfMovieInvertedIndex.get());
    }

    @BeforeEach
    void setup() {
        ignoreStdOut();
    }

    @AfterEach()
    void tearDown() {
        reSetRealStdOut();
    }

    @Test
    void getAllDocIds() throws IOException, URISyntaxException {
        Corpus sampleCorpus = InvertedIndexTest.getSampleCorpus();
        assert sampleCorpus.size() > 0;
        irs = new InformationRetrievalSystem(sampleCorpus);
        assertEquals(sampleCorpus.getCorpus().keySet(), irs.getAllDocIds());
    }
}