package it.units.informationretrieval.ir_boolean_model.evaluation;

import benchmark.BenchmarkRunner;
import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

class BenchmarkingTest {

    /**
     * The name of the {@link it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer} currently used.
     */
    final static String STEMMER_NAME;
    /**
     * The folder name where to save results.
     */
    private static final String FOLDER_NAME_TO_SAVE_RESULTS = "system_evaluation" + File.separator
            + "benchmarks" + File.separator + "stemmer=" + STEMMER_NAME;

    static {
        String stemmerNameTmp;
        try {
            stemmerNameTmp = AppProperties.getInstance().get("app.stemmer");
        } catch (IOException e) {
            Logger.getLogger(BenchmarkingTest.class.getCanonicalName())
                    .log(Level.SEVERE, "Property not found", e);
            stemmerNameTmp = "null";
        }
        STEMMER_NAME = stemmerNameTmp;
    }

    //    @Test
// this method will be executed with the test suite if annotated with @Test // TODO: better to create a specific configuration to run only benchmarking (it requires very long time)
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
