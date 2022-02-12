package it.units.informationretrieval.ir_boolean_model.evaluation;

import benchmark.BenchmarkRunner;
import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Benchmarking {

    /**
     * Concatenation of all properties from {@link AppProperties}.
     */
    public final static String PROPERTY_CONCATENATION;
    /**
     * The folder name where to save results.
     */
    private static final String FOLDER_NAME_TO_SAVE_RESULTS;

    static {
        String propertyConcatenationTmp;
        try {
            propertyConcatenationTmp = AppProperties.getInstance().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> File.separator + e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining())
                    .replaceAll("[^\\w.=\\" + File.separator + "]", "");
        } catch (IOException e) {
            propertyConcatenationTmp = "";
            Logger.getLogger(Benchmarking.class.getCanonicalName())
                    .log(Level.SEVERE, "Property not found", e);
        }
        PROPERTY_CONCATENATION = propertyConcatenationTmp;
        FOLDER_NAME_TO_SAVE_RESULTS = "system_evaluation" + File.separator + "benchmarks" + PROPERTY_CONCATENATION;
    }

    static void benchmarkAll() throws IOException {
        final boolean printBenchmarkProgress = true;
        BenchmarkRunner benchmarkRunner = new BenchmarkRunner(printBenchmarkProgress);
        benchmarkRunner.benchmarkAllAnnotatedMethodsAndGetListOfResults();
        System.out.println(benchmarkRunner);
        Utility.writeToFile(
                benchmarkRunner.toString(),
                new File(FOLDER_NAME_TO_SAVE_RESULTS + File.separator +
                        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                                .withZone(ZoneId.systemDefault())
                                .format(Instant.now()) + ".txt"),
                false);
    }

    public static void main(String[] args) {
        System.out.println("Benchmarking started" + System.lineSeparator());
        try {
            benchmarkAll();
        } catch (Exception e) {
            Logger.getLogger(Benchmarking.class.getCanonicalName())
                    .log(Level.SEVERE, "Exception thrown", e);
        }
    }

}
