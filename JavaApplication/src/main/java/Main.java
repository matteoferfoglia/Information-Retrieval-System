import components.Corpus;
import components.Posting;
import documentDescriptors.Movie;
import org.jetbrains.annotations.NotNull;
import query.BooleanExpression;
import util.Properties;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Matteo Ferfoglia
 */
public class Main {

    public static void main(String[] args) {

        try {

            // Load application properties and create working directory
            Properties.loadProperties();
            /* // Examples with Properties
            Properties.appProperties.list(System.out);
            Properties.appProperties.setProperty("testProp", "0");
            Properties.appProperties.list(System.out);
            Properties.appProperties.setProperty("testProp", "1");
            Properties.appProperties.list(System.out);
            Properties.saveCurrentProperties("Changed testProp");
            */

            // For saving/loading the IRSystem as file
            String fileName_irSystem = Properties.appProperties.getProperty("workingDirectory_name") + "/irSystem";
            File file_irSystem = new File(fileName_irSystem);

            // Load the IR System if already exists
            InformationRetrievalSystem ir;

            //noinspection SwitchStatementWithTooFewBranches
            switch (file_irSystem.isFile() ? 1 : 0) {
                case 1:    // file exists
                    System.out.println("Loading the IRSystem from file system");
                    ObjectInputStream ois = new ObjectInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(file_irSystem)
                            )
                    );
                    try {
                        Object irSystem_object = ois.readObject();
                        if (irSystem_object instanceof InformationRetrievalSystem) {
                            ir = (InformationRetrievalSystem) irSystem_object;
                            System.out.println("IRSystem loaded from the file system.");
                            break;
                        }   // else create the IRSystem
                    } catch (Exception ignore) {
                        System.err.println("Errors occurred when reading the IRSystem from the file system. " +
                                "It will be recreated.");
                        // go to the next case and re-create the IRSystem
                    }

                default:
                    System.out.println("Reading the corpus");

                    // Load the corpus
                    Corpus corpus = Movie.createCorpus();
                    System.out.println(corpus.head(3));

                    // Create the IR System
                    System.out.println("Creating the IR System");
                    ir = new InformationRetrievalSystem(corpus);
            }

            // Serialize and save the IR System to the file system
            if (file_irSystem.createNewFile()) {    // if file already exists will do nothing
                System.out.println("File \"" + fileName_irSystem + "\" created.");
            } else {
                System.out.println("File \"" + fileName_irSystem + "\" already exists. It will be replaced.");
            }
            ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(
                            new FileOutputStream(fileName_irSystem, false)
                    )
            );
            oos.writeObject(ir);
            System.out.println("IR System saved to file " + fileName_irSystem);
            oos.flush();
            oos.close();

            // Use the information retrieval system
            final int MAX_N_RESULTS = 5;
            System.out.println(andQueryAndReturnResultsAsString(ir, Arrays.asList("Vidya", "Bagchi", "Kolkata"), MAX_N_RESULTS));
            System.out.println(andQueryAndReturnResultsAsString(ir, Arrays.asList("Space", "jam"), MAX_N_RESULTS));
            System.out.println(andQueryAndReturnResultsAsString(ir, Collections.singletonList("hand"), MAX_N_RESULTS));

        } catch (Posting.DocumentIdentifier.NoMoreDocIdsAvailable | URISyntaxException | IOException e) {
            e.printStackTrace();
        }

    }

    static String andQueryAndReturnResultsAsString(@NotNull final InformationRetrievalSystem irs,
                                                   @NotNull final Collection<@NotNull String> stringsToBePresent,
                                                   int maxNumberOfResultsToReturn) {
        long startTime, endTime;
        StringBuilder sb = new StringBuilder();
        startTime = System.currentTimeMillis();

        BooleanExpression be = Objects.requireNonNull(stringsToBePresent)
                .stream()
                .map(aValueToBePresent -> new BooleanExpression(BooleanExpression.UNARY_OPERATOR.IDENTITY, aValueToBePresent))
                .reduce((b1, b2) -> new BooleanExpression(BooleanExpression.BINARY_OPERATOR.AND, b1, b2))
                .orElse(new BooleanExpression(BooleanExpression.UNARY_OPERATOR.IDENTITY, ""));

        List<?> results = be.evaluate(Objects.requireNonNull(irs).getInvertedIndex());
        endTime = System.currentTimeMillis();
        sb.append(results.size()).append(" result").append(results.size() > 1 ? "s" : "").append(" for (\"")
                .append(String.join("\" AND \"", stringsToBePresent))
                .append("\") found in ").append(endTime - startTime)
                .append(" ms.");
        if (results.size() > 1) {
            sb.append("First ")
                    .append(Math.min(maxNumberOfResultsToReturn, results.size()))
                    .append(" results");
        }
        if (results.size() > 0) {
            sb.append(":\n-\t")
                    .append(
                            results.stream()
                                    .limit(maxNumberOfResultsToReturn)
                                    .map(Object::toString)
                                    .collect(Collectors.joining("\n-\t"))
                    );
        }

        return sb.toString();
    }
}