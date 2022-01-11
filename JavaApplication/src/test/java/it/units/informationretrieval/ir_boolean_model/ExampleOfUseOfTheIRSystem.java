package it.units.informationretrieval.ir_boolean_model;

import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class showing an example of use of the IR system.
 *
 * @author Matteo Ferfoglia
 */
class ExampleOfUseOfTheIRSystem {

    public static void main(String[] args) {    // TODO: refactor

        try {

            // Load application properties and create working directory
            AppProperties appProperties = AppProperties.getInstance();

            // For saving/loading the IRSystem as file
            String fileName_irSystem = appProperties.get("workingDirectory_name") + "/irSystem";
            File file_irSystem = new File(fileName_irSystem);

            // Load the IR System if already exists
            InformationRetrievalSystem irs;

            switch (file_irSystem.isFile() ? 1 : 0) {
                case 1:    // file exists
                    System.out.println("Loading the IRSystem from the file system");
                    ObjectInputStream ois = new ObjectInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(file_irSystem)));
                    try {
                        Object irSystem_object = ois.readObject();
                        if (irSystem_object instanceof InformationRetrievalSystem) {
                            irs = (InformationRetrievalSystem) irSystem_object;
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
                    irs = new InformationRetrievalSystem(corpus);

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
                    oos.writeObject(irs);
                    System.out.println("IR System saved to file " + fileName_irSystem);
                    oos.flush();
                    oos.close();
            }

            // Use the information retrieval system
            final int MAX_N_RESULTS = 10;
            System.out.println(andQueryAndReturnResultsAsString(irs, Arrays.asList("Vidya", "Bagchi", "Kolkata"), MAX_N_RESULTS));
            System.out.println(andQueryAndReturnResultsAsString(irs, Arrays.asList("Space", "jam"), MAX_N_RESULTS));
            System.out.println(andQueryAndReturnResultsAsString(irs, Collections.singletonList("hand"), MAX_N_RESULTS));

            // Using Boolean expressions
            System.out.println(queryAndReturnResultsAsString(
                    irs.createNewBooleanExpression().setMatchingPhrase("Space jam".split(" ")), MAX_N_RESULTS));
            System.out.println(queryAndReturnResultsAsString(
                    irs.createNewBooleanExpression()
                            .setMatchingPhrase("Space jam".split(" "))
                            .or(irs.createNewBooleanExpression().setMatchingValue("Vidya").or("Bagchi")), MAX_N_RESULTS));

            // Wildcards queries
            System.out.println(queryAndReturnResultsAsString(
                    irs.createNewBooleanExpression()
                            .setMatchingPhrase("Space *am".split(" ")), MAX_N_RESULTS));
            System.out.println(queryAndReturnResultsAsString(
                    irs.createNewBooleanExpression()
                            .setMatchingPhrase("Space *am".split(" "))
                            .or(irs.createNewBooleanExpression().setMatchingValue("Vidya").or("Bag*")), MAX_N_RESULTS));

            // QueryString
            String[] queryStrings = new String[]{
                    "\"monster\"|\"space\"", "moon", "asterix & obelix", "love !story", "love&!story", "\"Space jam\""};
            System.out.println(System.lineSeparator() + "Query strings");
            for (String queryString : queryStrings) {
                System.out.println("Query string: " + queryString);
                int maxNumOfResultsToShow = 5;
                System.out.println(irs.retrieve(queryString).stream().limit(maxNumOfResultsToShow).toList());
            }
            System.out.println(System.lineSeparator() + "Query strings (with more output)");
            for (String queryString : queryStrings) {
                System.out.println("Query string: " + queryString);
                int maxNumOfResultsToShow = 5;
                long start, stop;
                start = System.nanoTime();
                BooleanExpression be = irs.createNewBooleanExpression().parseQuery(queryString);
                List<Document> results = be.evaluate();
                stop = System.nanoTime();
                System.out.println("Results for query \"" + queryString + "\"  {" + be.getQueryString() + "} "
                        + "found in " + (stop - start) / 1e6 + " ms:"
                        + System.lineSeparator()
                        + results.stream().limit(maxNumOfResultsToShow)
                        .map(aResult -> "\t - " + aResult).collect(Collectors.joining(System.lineSeparator())));
            }

            // Spelling correction
            for (var phoneticCorrection : new boolean[]{true, false}) {

                for (var useEditDistance : new boolean[]{true, false}) {

                    System.out.println(System.lineSeparator() + System.lineSeparator()
                            + "## " + (phoneticCorrection ? "Phonetic" : "Spelling") + " correction ##");
                    System.out.println("### " + (useEditDistance ? "with" : "without") + " edit distance ###");
                    System.out.println("### Note: edit-distance can be ignored only with phonetic correction");

                    BooleanExpression wrongQueryBE = irs.createNewBooleanExpression().setMatchingValue("Spack");
                    System.out.println(queryAndReturnResultsAsString(wrongQueryBE, MAX_N_RESULTS));
                    System.out.println(queryAndReturnResultsAsString(wrongQueryBE.spellingCorrection(phoneticCorrection, useEditDistance), MAX_N_RESULTS));
                    System.out.println(queryAndReturnResultsAsString(wrongQueryBE.spellingCorrection(phoneticCorrection, useEditDistance), MAX_N_RESULTS));// edit distance increases each time

                    // Spelling correction with phrase
                    BooleanExpression wrongPhraseQueryBE = irs.createNewBooleanExpression().setMatchingPhrase("Space jam".split(" "));
                    System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE, MAX_N_RESULTS));
                    System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection, useEditDistance), MAX_N_RESULTS));
                    System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection, useEditDistance), MAX_N_RESULTS));// edit distance increases each time
//                System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection), MAX_N_RESULTS));
//                System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection), MAX_N_RESULTS));
//                System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection), MAX_N_RESULTS));
//                System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection), MAX_N_RESULTS));
//                System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection), MAX_N_RESULTS));
//                System.out.println(queryAndReturnResultsAsString(wrongPhraseQueryBE.spellingCorrection(phoneticCorrection), MAX_N_RESULTS));
                }
            }


        } catch (URISyntaxException | IOException | NoMoreDocIdsAvailable e) {
            e.printStackTrace();
        }

    }

    static String andQueryAndReturnResultsAsString(@NotNull final InformationRetrievalSystem irs,
                                                   @NotNull final Collection<@NotNull String> stringsToBePresent,
                                                   int maxNumberOfResultsToReturn) {
        BooleanExpression be = Objects.requireNonNull(stringsToBePresent)
                .stream()
                .map(aValueToBePresent -> irs.createNewBooleanExpression().setMatchingValue(aValueToBePresent))
                .reduce(BooleanExpression::and)
                .orElse(irs.createNewBooleanExpression());
        return queryAndReturnResultsAsString(be, maxNumberOfResultsToReturn);
    }

    static String queryAndReturnResultsAsString(@NotNull final BooleanExpression be,
                                                int maxNumberOfResultsToReturn) {
        long startTime, endTime;
        StringBuilder sb = new StringBuilder();
        startTime = System.currentTimeMillis();

        List<?> results = be.evaluate();
        endTime = System.currentTimeMillis();
        sb.append(System.lineSeparator())
                .append(results.size()).append(" result").append(results.size() > 1 ? "s" : "")
                .append(" for ")
                .append(be.getQueryString())
                .append(System.lineSeparator())
                .append("  found in ").append(endTime - startTime)
                .append(" ms. ");
        if (be.isSpellingCorrectionApplied()) {
            sb.append(System.lineSeparator())
                    .append(" Spelling correction applied and terms are at most ")
                    .append(be.getEditDistanceForSpellingCorrection())
                    .append(" (edit-distance) far from the inserted query {")
                    .append(be.getInitialQueryString())
                    .append("}");
        }
        if (results.size() > 1) {
            sb.append(System.lineSeparator());
            if (results.size() > maxNumberOfResultsToReturn) {
                sb.append("First ")
                        .append(maxNumberOfResultsToReturn)
                        .append(" results");
            } else {
                sb.append("Results");
            }
        }
        if (results.size() > 0) {
            sb.append(":")
                    .append(System.lineSeparator())
                    .append("-\t")
                    .append(
                            results.stream()
                                    .limit(maxNumberOfResultsToReturn)
                                    .map(Object::toString)
                                    .collect(Collectors.joining("\n-\t")));
        }

        return sb.toString();
    }
}