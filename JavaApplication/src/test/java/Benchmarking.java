import benchmark.BenchmarkRunner;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Benchmarking {

    private static final String FOLDER_NAME_TO_SAVE_RESULTS = "benchmarks";

    public static void main(String[] args) throws IOException {    // TODO: create a Maven plugin to run this automatically
        // TODO: this method may be invoked from production code (e.g., a flag as command line arg to main)
        BenchmarkRunner benchmarkRunner = new BenchmarkRunner();
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
