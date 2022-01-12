package it.units.informationretrieval.ir_boolean_model.evaluation;

import benchmark.BenchmarkRunner;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

class Benchmarking {

    private static final String FOLDER_NAME_TO_SAVE_RESULTS = "benchmarks";

    @Test
        // this method will be executed with the test suite if annotated with @Test
    void benchmarkAll() throws IOException {
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

}
