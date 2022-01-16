package it.units.informationretrieval.ir_boolean_model.evaluation;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.CranfieldDocument;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.evaluation.cranfield_collection.CranfieldQuery;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class evaluates the system.
 *
 * @author Matteo Ferfoglia
 */
public class Evaluation {

    /**
     * The {@link InformationRetrievalSystem} for the Cranfield's collection.
     */
    @NotNull
    private static final InformationRetrievalSystem CRANFIELD_IRS;

    /**
     * Queries for the Cranfield's collection.
     */
    @NotNull
    private static final List<CranfieldQuery> CRANFIELD_QUERIES;

    /**
     * Saves the precision for each query in {@link #CRANFIELD_QUERIES}
     * and in the same order.
     */
    @NotNull
    private final static List<Double> precisions = new ArrayList<>();

    /**
     * Saves the recall for each query in {@link #CRANFIELD_QUERIES}
     * and in the same order.
     */
    @NotNull
    private final static List<Double> recalls = new ArrayList<>();

    static {
        InformationRetrievalSystem irsTmp = null;
        List<CranfieldQuery> queriesTmp = null;
        try {
            PrintStream realStdOut = System.out;
            System.setOut(new PrintStream(new ByteArrayOutputStream()));  // avoid to print useless output during tests
            irsTmp = new InformationRetrievalSystem(CranfieldDocument.createCorpus());
            System.setOut(realStdOut);
            queriesTmp = CranfieldQuery.readQueries();
        } catch (URISyntaxException | IOException | NoMoreDocIdsAvailable e) {
            fail(e);
        } finally {
            assert irsTmp != null; // test should fail if null (errors while creating it)
            assert queriesTmp != null;
            CRANFIELD_IRS = irsTmp;
            CRANFIELD_QUERIES = queriesTmp;
        }
    }

    @BeforeAll
    static void computePrecisionAndRecallForAllQueries() {
        CRANFIELD_QUERIES.stream()
                .sequential()   // order matters to associate queries to their precisions (if desired)
                .forEach(query -> {
                    Set<Document> relevantDocuments = query.getRelevantDocs().keySet()
                            .stream().map(doc -> (Document) doc).collect(Collectors.toSet());
                    Set<Document> retrievedDocuments = new HashSet<>(CRANFIELD_IRS.retrieve(query.getQueryText()));
                    Set<Document> relevantAndRetrieved = relevantDocuments.stream()
                            .filter(retrievedDocuments::contains).collect(Collectors.toSet());
                    precisions.add(retrievedDocuments.size() > 0
                            ? (double) relevantAndRetrieved.size() / retrievedDocuments.size()
                            : relevantDocuments.size() == 0 ? 1 : 0);
                    recalls.add(relevantDocuments.size() > 0
                            ? (double) relevantAndRetrieved.size() / relevantDocuments.size()
                            : retrievedDocuments.size() == 0 ? 1 : 0);
                });
    }

    /**
     * Prints the statistics about the given input collection,
     * referring the physical dimension whose name is passed as parameter.
     *
     * @param dimensionName                The name of the physical dimension for which
     *                                     statistics will be printed.
     * @param whatToComputeStatisticsAbout The collection containing the measures about the physical
     *                                     dimension for which statistics will be printed.
     */
    private static void printStatistics(
            @NotNull String dimensionName, @NotNull Collection<? extends Number> whatToComputeStatisticsAbout) {

        AtomicReference<Double> max = new AtomicReference<>();
        AtomicReference<Double> min = new AtomicReference<>();

        final double EPSILON = 1E-18;   // used in comparison between doubles

        Consumer<@NotNull Collection<? extends Number>> statisticsMaker = whatToComputeStatisticsAbout_ -> {
            double avg_ = whatToComputeStatisticsAbout_.stream().mapToDouble(number -> (double) number).average().orElseThrow();
            double max_ = whatToComputeStatisticsAbout_.stream().mapToDouble(number -> (double) number).max().orElseThrow();
            double min_ = whatToComputeStatisticsAbout_.stream().mapToDouble(number -> (double) number).min().orElseThrow();

            max.set(max_);
            min.set(min_);

            long occurrencesOfMax = whatToComputeStatisticsAbout_.stream().filter(d -> Math.abs((double) d - max_) < EPSILON).count();
            long occurrencesOfMin = whatToComputeStatisticsAbout_.stream().filter(d -> Math.abs((double) d - min_) < EPSILON).count();

            System.out.println("\tNumber of available measures:  " + whatToComputeStatisticsAbout_.size());
            System.out.println("\tAverage: " + avg_);
            System.out.println("\tMax:     " + max_ + "\t observed in " + occurrencesOfMax + " samples");
            System.out.println("\tMin:     " + min_ + "\t observed in " + occurrencesOfMin + " samples");
        };

        System.out.println(System.lineSeparator() + "Statistics about " + dimensionName);
        statisticsMaker.accept(whatToComputeStatisticsAbout);
        System.out.println("Excluding max and min:");
        statisticsMaker.accept(whatToComputeStatisticsAbout.stream()
                .filter(number -> (double) number < max.get() - EPSILON)
                .filter(number -> (double) number > min.get() + EPSILON)
                .toList());
        System.out.println();
    }

    @Test
    void precision() {
        printStatistics("precision", precisions);
    }

    @Test
    void recall() {
        printStatistics("recall", recalls);
    }

}

