package it.units.informationretrieval.ir_boolean_model.evaluation;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.evaluation.cranfield_collection.CranfieldQuery;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.plots.Point;
import it.units.informationretrieval.ir_boolean_model.plots.XYLineChart;
import it.units.informationretrieval.ir_boolean_model.user_defined_contents.cranfield.CranfieldCorpusFactory;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.*;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * This class evaluates the system.
 * Results are also saved to file in directory
 * {@link #FOLDER_NAME_TO_SAVE_RESULTS}.
 *
 * @author Matteo Ferfoglia
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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

    private static String currentDateTime;
    /**
     * {@link List} of {@link Point.Series} of points (precision,recall)
     * for the precision-recall curve, computed on {@link #CRANFIELD_QUERIES}.
     */
    private static List<Point.Series> precisionRecallSeries;
    /**
     * {@link List} of {@link Point.Series} of points (precision,recall)
     * for the interpolation precisions curve, computed on {@link #precisionRecallSeries}.
     */
    private static List<Point.Series> interpolatedPrecisionSeries;

    static {
        BufferedWriter bwToFile1;   // tmp variable to defer the assignment to a final variable
        try {
            File directory = new File(FOLDER_NAME_TO_SAVE_RESULTS);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Output file not created.");
                }
            }
            currentDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
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
            irsTmp = new InformationRetrievalSystem(new CranfieldCorpusFactory().createCorpus());
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
                    var relevantDocuments = getRelevantDocsForQuery(query);
                    var retrievedDocuments = getRetrievedDocsForQuery(query);
                    Set<Document> relevantAndRetrieved = getRelevantAndRetrieved(relevantDocuments, retrievedDocuments);
                    // TODO : investigate on errors (retrievedDocuments.stream().filter(d -> ! relevantDocuments.contains(d)).toList()) to improve precision and recall
                    //        Stemming problems, e.g.: wildcard f*ow matches "following", because stemming of "following" is "follow" --> add to stop words?
                    //        Test queries construction problems, e.g.: wildcard "*ch" matches "match"
                    precisions.add(retrievedDocuments.size() > 0
                            ? (double) relevantAndRetrieved.size() / retrievedDocuments.size()
                            : relevantDocuments.size() == 0 ? 1 : 0);
                    recalls.add(relevantDocuments.size() > 0
                            ? (double) relevantAndRetrieved.size() / relevantDocuments.size()
                            : retrievedDocuments.size() == 0 ? 1 : 0);
                });
    }

    /**
     * @param relevantDocuments  {@link List} of relevant documents (referring to a query).
     * @param retrievedDocuments {@link List} of retrieved documents (referring ot the same query of the other param).
     * @return The intersection of relevant and retrieved documents as {@link Set}.
     */
    @NotNull
    private static Set<Document> getRelevantAndRetrieved(
            @NotNull List<Document> relevantDocuments,
            @NotNull List<Document> retrievedDocuments) {
        return relevantDocuments.stream()
                .filter(retrievedDocuments::contains).collect(Collectors.toSet());
    }

    /**
     * @param query A query.
     * @return The {@link List} of {@link Document}s retrieved for the input query,
     * sorted as described in {@link InformationRetrievalSystem#retrieve(String)}.
     */
    @NotNull
    private static List<Document> getRetrievedDocsForQuery(@NotNull CranfieldQuery query) {
        return CRANFIELD_IRS.retrieve(query.getQueryText());
    }

    /**
     * @param query A query.
     * @return The {@link List} of {@link Document}s which are relevant for the input query.
     */
    @NotNull
    private static List<Document> getRelevantDocsForQuery(@NotNull CranfieldQuery query) {
        return query.getRelevantDocs().keySet().stream().map(doc -> (Document) doc).toList();
    }

    @AfterAll
    static void closeJavaFX() {
        XYLineChart.close();
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

            Function<Double, String> formatDouble = doubleIn -> {
                final int NUM_OF_DECIMALS = 16;
                final String PRINT_FORMAT = "%." + NUM_OF_DECIMALS + "f";
                return String.format(PRINT_FORMAT, doubleIn);
            };
            sb.append("\tNumber of available measures:  ").append(whatToComputeStatisticsAbout_.size()).append(System.lineSeparator());
            sb.append("\tAverage: ").append(formatDouble.apply(avg_)).append(System.lineSeparator());
            sb.append("\tMax:     ").append(formatDouble.apply(max_)).append("\t observed in ").append(occurrencesOfMax).append(" samples").append(System.lineSeparator());
            sb.append("\tMin:     ").append(formatDouble.apply(min_)).append("\t observed in ").append(occurrencesOfMin).append(" samples").append(System.lineSeparator());
        };

        sb.append("Statistics about ").append(dimensionName).append(System.lineSeparator());
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
    @Order(1)
    void precision() {
        printStatistics("precision", precisions);
    }

    @Test
    @Order(2)
    void recall() {
        printStatistics("recall", recalls);
    }

    @Test
    @Order(3)
    void precisionRecallCurve() {   // TODO : look at worst queries and understand why

        precisionRecallSeries = CRANFIELD_QUERIES.parallelStream().unordered()
                .map(query -> {
                    var relevantDocuments = getRelevantDocsForQuery(query);
                    var retrievedDocuments = getRetrievedDocsForQuery(query);
                    Point.Series recall_precision_points = new Point.Series(String.valueOf(query.getQueryNumber()));

                    for (int j = 0; j < retrievedDocuments.size(); j++) {
                        var retrievedDocsTillJth = retrievedDocuments.subList(0, j + 1);
                        var relevantAndRetrievedTillJth = getRelevantAndRetrieved(relevantDocuments, retrievedDocsTillJth);
                        double precision = (double) relevantAndRetrievedTillJth.size() / retrievedDocsTillJth.size();
                        double recall = (double) relevantAndRetrievedTillJth.size() / relevantDocuments.size();
                        recall_precision_points.add(new Point<>(recall, precision));
                    }
                    if (retrievedDocuments.isEmpty()) {
                        double precision = 0D;
                        double recall = 0D;
                        recall_precision_points.add(new Point<>(recall, precision));
                    }

                    return recall_precision_points;
                })
                .collect(Collectors.toList());

        do {
            try {
                Point.plotAndSavePNG_ofMultipleSeries("Precision-Recall curve", precisionRecallSeries, "Recall", "Precision", false,
                        FOLDER_NAME_TO_SAVE_RESULTS + File.separator + currentDateTime + "_precisionRecallCurves.png", 10);
                break;
            } catch (OutOfMemoryError e) {
                Logger.getLogger(getClass().getCanonicalName()).log(Level.SEVERE, "Out of memory. Re-trying with less data", e);
                if (precisionRecallSeries.size() > 0) {
                    // remove one series randomly (to reduce the size) a re-try
                    Collections.shuffle(precisionRecallSeries);
                    precisionRecallSeries.remove(precisionRecallSeries.size() - 1);
                } else {
                    throw e;
                }
            }
        } while (true /*exit via break instruction*/);

        // Investigation on the worst query (according to the worst precision)
        // TODO: low precision results due to wildcard query that, if stemming is applied, do not work correctly (system should have a supplementary index containing all rotations of un-stemmed words!)
        final int MAX_NUM_OF_WORST_QUERIES = 20;    // how many to show
        StringBuilder worstQueriesSummary = new StringBuilder(
                "The " + MAX_NUM_OF_WORST_QUERIES + " queries with the worst precision:" + System.lineSeparator());
        worstQueriesSummary.append(
                        // sort list of series according to avg precision (the worst series at beginning)
                        precisionRecallSeries.stream()
                                .map(series -> new Pair<>(
                                        series.stream().mapToDouble(Point::getY).average().orElseThrow(),
                                        series.getName()))
                                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                                .limit(MAX_NUM_OF_WORST_QUERIES)
                                .map(aAvgPrecisionToSeries -> {
                                    var avgPrecision = aAvgPrecisionToSeries.getKey();
                                    var seriesName = aAvgPrecisionToSeries.getValue();
                                    return "\tQuery " + seriesName + ") \tAvg precision: " + avgPrecision;
                                })
                                .collect(Collectors.joining(System.lineSeparator())))
                .append(System.lineSeparator());
        System.out.println(worstQueriesSummary);
        try {
            WRITER_TO_FILE.write(worstQueriesSummary.toString());
            WRITER_TO_FILE.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    @Order(4)
    void interpolatedPrecisions() {    // TODO: re-check calculus
        if (precisionRecallSeries == null) {
            precisionRecallCurve();
        }
        interpolatedPrecisionSeries = precisionRecallSeries
                .stream().sequential() // order matters to guarantee to match series of precision-recall curve
                .map(aSeries -> {
                    var interpolatedPoints = IntStream.range(0, aSeries.size())
                            .mapToObj(i -> {
                                var recallLevel = aSeries.get(i).getX();
                                var maxPrecisionForRecallLevelsGreaterOrEqual =
                                        aSeries.getSortedListOfPointWithXGreaterOrEqual(recallLevel)
                                                .stream().mapToDouble(Point::getY).max().orElseThrow();
                                return new Point<>(recallLevel, maxPrecisionForRecallLevelsGreaterOrEqual);
                            })
                            .collect(Collectors.toList());

                    // add point at recall=0
                    var recall0 = 0D;
                    var precision0 = interpolatedPoints.isEmpty() ? 0.0 : interpolatedPoints.get(0).getY();
                    interpolatedPoints.add(0 /*add at beginning*/, new Point<>(recall0, precision0));

                    return new Point.Series(interpolatedPoints, aSeries.getName());
                })
                .toList();

        try {
            Point.plotAndSavePNG_ofMultipleSeries("Interpolated precisions curve", interpolatedPrecisionSeries, "Recall", "Precision", false,
                    FOLDER_NAME_TO_SAVE_RESULTS + File.separator + currentDateTime + "_interpolatedPrecisions.png", 10);
        } catch (OutOfMemoryError e) {
            Logger.getLogger(getClass().getCanonicalName())
                    .log(Level.SEVERE, "Error with plot.", e);
        }
    }

    @ParameterizedTest
    @Order(5)
    @ValueSource(ints = {11/*11-point interpolated average precision*/})
    void NPointInterpolatedAveragePrecision(int NUM_OF_POINTS) {    // TODO: re-check calculus

        if (interpolatedPrecisionSeries == null) {
            interpolatedPrecisions();
        }

        final double MIN_RECALL = 0D;
        final double MAX_RECALL = 1D;
        final double INCREMENT = MAX_RECALL / (NUM_OF_POINTS - 1); // -1 to consider the zero as first point,
        // i.e., we will have N points and N-1 intervals

        // Create interpolated series on points
        var interpolatedPrecisionSeriesOnTargetPoints = interpolatedPrecisionSeries.parallelStream()
                .map(aSeries -> {
                    var recallsThisSeries = aSeries.stream().map(Point::getX).toList();
                    var precisionsThisSeries = aSeries.stream().map(Point::getY).toList();

                    Point.Series interpolated = new Point.Series(aSeries.getName());

                    BiFunction<List<Double>, Double, Integer> findNearestDoubleInListAndGetItsIndex = (list, target) -> {
                        assert list.size() > 0;
                        int answerIndex = 0;
                        double distance = Double.MAX_VALUE;
                        for (int i = 0; i < list.size(); i++) {
                            double currentDistance = Math.abs(list.get(i) - target);
                            if (currentDistance < distance) {
                                answerIndex = i;
                                distance = currentDistance;
                            }
                        }
                        return answerIndex;
                    };

                    for (double recall = MIN_RECALL; recall <= MAX_RECALL; recall += INCREMENT) {
                        var indexOfPointWithRecallNearestToCurrent = findNearestDoubleInListAndGetItsIndex.apply(recallsThisSeries, recall);
                        interpolated.add(new Point<>(
                                Math.round(recall * 100D) / 100D, // round to 2 decimals
                                precisionsThisSeries.get(indexOfPointWithRecallNearestToCurrent)));
                    }

                    return interpolated;
                })
                .toList();

        final String SERIES_NAME = NUM_OF_POINTS + "-point interpolated average precision";
        var NPointsInterpolatedAveragePrecision =
                new Point.Series(IntStream.range(0, NUM_OF_POINTS)
                        .sequential()   // respect order of growing recall values
                        .mapToObj(i -> new Point<>(
                                interpolatedPrecisionSeriesOnTargetPoints.get(0).get(i).getX(),    // i-th recall value (taken from first series, but recall values are the same for all the series)
                                interpolatedPrecisionSeriesOnTargetPoints.stream().mapToDouble(aSeries -> aSeries.get(i).getY()).average().orElseThrow()))
                        .toList(), SERIES_NAME);

        try {
            Point.plotAndSavePNG_ofMultipleSeries(
                    SERIES_NAME, List.of(NPointsInterpolatedAveragePrecision), "Recall", "Precision", false,
                    FOLDER_NAME_TO_SAVE_RESULTS + File.separator + currentDateTime + "_NPointInterpolatedAveragePrecisions.png", 10);
        } catch (OutOfMemoryError e) {
            Logger.getLogger(getClass().getCanonicalName())
                    .log(Level.SEVERE, "Error with plot.", e);
        }


        // Write results
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator()).append(SERIES_NAME).append(System.lineSeparator());
        sb.append(
                IntStream.range(0, NPointsInterpolatedAveragePrecision.size())
                        .mapToObj(i -> {
                            var p = NPointsInterpolatedAveragePrecision.get(i);
                            return "\t" + i + ")\trecall: " + p.getX() + ", precision: " + p.getY();
                        })
                        .collect(Collectors.joining(System.lineSeparator())));
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

}
