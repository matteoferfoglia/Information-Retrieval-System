import benchmark.BenchmarkRunner;

public class Benchmarking {
    public static void main(String[] args) {    // TODO: create a Maven plugin to run this automatically
        BenchmarkRunner benchmarkRunner = new BenchmarkRunner();
        benchmarkRunner.benchmarkAllAnnotatedMethodsAndGetListOfResults();
        System.out.println(benchmarkRunner);
    }
}
