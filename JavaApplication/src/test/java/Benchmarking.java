import benchmark.BenchmarkRunner;

public class Benchmarking {
    public static void main(String[] args) {    // TODO: create a Maven plugin to run this automatically
        // TODO: this method may be invoked from production code (e.g., a flag as command line arg to main)
        BenchmarkRunner benchmarkRunner = new BenchmarkRunner();
        benchmarkRunner.benchmarkAllAnnotatedMethodsAndGetListOfResults();
        System.out.println(benchmarkRunner);
    }
}
