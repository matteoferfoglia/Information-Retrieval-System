package it.units.informationretrieval.ir_boolean_model.evaluation;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.Point;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.CranfieldDocument;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.evaluation.cranfield_collection.CranfieldQuery;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import skiplist.SkipList;

import java.io.*;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class evaluates the system.
 * Results are also saved to file in directory
 * {@link #FOLDER_NAME_TO_SAVE_RESULTS}.
 *
 * @author Matteo Ferfoglia
 */
public class EvaluationTest {

    /**
     * The folder name where to save results.
     */
    private static final String FOLDER_NAME_TO_SAVE_RESULTS = "system_evaluation" + File.separator + "statistics";

    /**
     * The {@link java.io.BufferedWriter} to print results to file.
     */
    private static final Writer WRITER_TO_FILE;
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
        BufferedWriter bwToFile1;   // tmp variable to defer the assignment to a final variable
        try {
            File directory = new File(FOLDER_NAME_TO_SAVE_RESULTS);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Output file not created.");
                }
            }
            var currentDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
            File f = new File(FOLDER_NAME_TO_SAVE_RESULTS + File.separator + currentDateTime + ".txt");
            if (!f.createNewFile()) {
                throw new IOException("Output file not created.");
            }
            bwToFile1 = new BufferedWriter(new FileWriter(f));
            bwToFile1.write("EVALUATION OF THE INFORMATION RETRIEVAL SYSTEM" + System.lineSeparator() + System.lineSeparator());
            bwToFile1.write("             " + currentDateTime.replace("_", " ") + System.lineSeparator());
            bwToFile1.write("===============================================" + System.lineSeparator() + System.lineSeparator());
        } catch (IOException e) {
            bwToFile1 = new BufferedWriter(new CharArrayWriter());
            e.printStackTrace();
        }
        WRITER_TO_FILE = bwToFile1;
    }

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
                    // TODO : investigate on errors (retrievedDocuments.stream().filter(d -> ! relevantDocuments.contains(d)).toList()) to improve precision and recall
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

        StringBuilder sb = new StringBuilder();

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

            sb.append("\tNumber of available measures:  ").append(whatToComputeStatisticsAbout_.size()).append(System.lineSeparator());
            sb.append("\tAverage: ").append(avg_).append(System.lineSeparator());
            sb.append("\tMax:     ").append(max_).append("\t observed in ").append(occurrencesOfMax).append(" samples").append(System.lineSeparator());
            sb.append("\tMin:     ").append(min_).append("\t observed in ").append(occurrencesOfMin).append(" samples").append(System.lineSeparator());
        };

        sb.append(System.lineSeparator()).append("Statistics about ").append(dimensionName).append(System.lineSeparator());
        statisticsMaker.accept(whatToComputeStatisticsAbout);
        sb.append("Excluding max and min:").append(System.lineSeparator());
        statisticsMaker.accept(whatToComputeStatisticsAbout.stream()
                .filter(number -> (double) number < max.get() - EPSILON)
                .filter(number -> (double) number > min.get() + EPSILON)
                .toList());
        sb.append(System.lineSeparator());

        String toString = sb.toString();
        System.out.println(toString);
        try {
            WRITER_TO_FILE.write(toString);
            WRITER_TO_FILE.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    void precision() {
        printStatistics("precision", precisions);
    }

    @Test
    void recall() {
        printStatistics("recall", recalls);
    }

    @Test
    void precisionRecallCurve() {
        Map<CranfieldQuery, Point.Series> mapQueryToRecallPrecisionPoints =
                new HashMap<>(CRANFIELD_QUERIES.size());

        List<Point.Series> seriesList = new ArrayList<>();
        for (var query : CRANFIELD_QUERIES) {   // TODO: parallelstream()
            SkipList<Document> relevantDocuments = new SkipList<>(
                    query.getRelevantDocs().keySet().stream().map(doc -> (Document) doc).toList());
            List<Document> retrievedDocuments = CRANFIELD_IRS.retrieve(query.getQueryText());
            Point.Series recall_precision_points = new Point.Series(String.valueOf(query.getQueryNumber()));
            for (int j = 1; j < retrievedDocuments.size(); j++) {
                var retrievedDocsTillJth = new SkipList<>(retrievedDocuments.subList(0, j));
                var relevantAndRetrievedTillJth = Utility.intersection(relevantDocuments, retrievedDocsTillJth);
                double precision = (double) relevantAndRetrievedTillJth.size() / retrievedDocsTillJth.size();
                double recall = (double) relevantAndRetrievedTillJth.size() / relevantDocuments.size();
                recall_precision_points.add(new Point<>(recall, precision));
            }
            seriesList.add(recall_precision_points);
        }

        Point.plotAndSavePNG_ofMultipleSeries("Precision-Recall curve", seriesList, "Recall", "Precision", true,
                FOLDER_NAME_TO_SAVE_RESULTS + File.separator + "precisionRecallCurves.png", 10);

    }

}
