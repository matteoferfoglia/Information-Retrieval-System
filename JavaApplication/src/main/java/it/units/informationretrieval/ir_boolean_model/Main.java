package it.units.informationretrieval.ir_boolean_model;

import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;

import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws NoMoreDocIdsAvailable, URISyntaxException {
        var irs = new InformationRetrievalSystem(Movie.createCorpus());
        var be = irs.createNewBooleanExpression()
                .setMatchingValue("beware")
                .not();
        long start, end;
        start = System.nanoTime();
        var results = be.evaluate();
        end = System.nanoTime();
        System.out.println(
                "Query evaluated in " + (end - start) / 1e6 + " ms and produced " + results.size() + " results.");
    }
}
