package it.units.informationretrieval.ir_boolean_model;

import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.factories.CorpusFactory;
import it.units.informationretrieval.ir_boolean_model.queries.BINARY_OPERATOR;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import it.units.informationretrieval.ir_boolean_model.queries.UNARY_OPERATOR;
import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;
import it.units.informationretrieval.ir_boolean_model.utils.ClassLoading;
import it.units.informationretrieval.ir_boolean_model.utils.ProgressInputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.PropertyChangeListener;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    /**
     * The flag to insert to terminate the execution.
     */
    public static final String EXIT_REQUEST = "-q";
    /**
     * The flag to insert if phonetic correction must be applied.
     */
    public static final String PHONETIC_CORRECTION_REQUEST_PREFIX = "-" + OptionsSpellingCorrection.USE_PHONETIC_CORRECTION;
    /**
     * The flag to insert if spelling correction must be applied.
     */
    public static final String SPELLING_CORRECTION_REQUEST_PREFIX = "-" + OptionsSpellingCorrection.USE_SPELLING_CORRECTION;
    /**
     * The name of the folder where pre-created IR Systems are saved.
     */
    @Nullable // null if problems when getting the folder name from app properties
    public static final File FOLDER_WITH_IRS;
    /**
     * The {@link Scanner} used to read from {@link System#in}.
     */
    private static final Scanner SCANNER_READER = new Scanner(System.in);

    static {
        File folderWithIrsTmp;        // defer the assignment of final field
        try {
            AppProperties appPropertiesTmp = AppProperties.getInstance();
            folderWithIrsTmp = new File(
                    Objects.requireNonNull(appPropertiesTmp).get("workingDirectory_name") + "/irs/");
            if (!folderWithIrsTmp.exists()) {
                if (!folderWithIrsTmp.mkdirs()) {
                    throw new IOException("Unable to create directory");
                }
            }
        } catch (IOException | NullPointerException e) {
            Logger.getLogger(Main.class.getCanonicalName())
                    .log(Level.SEVERE, "Error reading app properties", e);
            folderWithIrsTmp = null;
        }
        FOLDER_WITH_IRS = folderWithIrsTmp;
    }

    /**
     * Main method, used by the system to communicate with the user.
     */
    public static void main(String[] args) throws IOException {

        System.out.println("=========================================================================================");
        System.out.println("=====                  Information Retrieval System - Boolean Model                  ====");
        System.out.println("=========================================================================================");
        System.out.println(System.lineSeparator());

        InformationRetrievalSystem irs = loadOrCreateInformationRetrievalSystem_orNullIfErrors();
        if (irs == null) {
            return;
        }

        System.out.println();
        System.out.println("System ready");
        System.out.println();

        printInstructions();

        boolean anotherQuery;
        do {
            try {
                anotherQuery = answerOneQuery(irs);
            } catch (Exception e) {
                Logger.getLogger(Main.class.getCanonicalName())
                        .log(Level.SEVERE, "Error during main program execution.", e);
                anotherQuery = true;
                try {
                    Thread.sleep(500);  // just the time to show the log message
                } catch (InterruptedException ignored) {
                }
            }
        }
        while (anotherQuery);

    }

    /**
     * Asks the user if creating or loading from the file-system
     * an already-existent {@link InformationRetrievalSystem} and
     * do the required action, then returns the instance of
     * {@link InformationRetrievalSystem} or null if errors occurred.
     *
     * @return the instance of the required (by the user, via stdIn)
     * or null if errors occurred.
     * @throws IOException if errors occur while reading the required
     *                     instance of the {@link InformationRetrievalSystem}
     *                     from the file system.
     */
    @Nullable
    private static InformationRetrievalSystem loadOrCreateInformationRetrievalSystem_orNullIfErrors()
            throws IOException {
        OptionsIRS option = null;
        do {
            System.out.print("Insert '" + OptionsIRS.CREATE_NEW_IRS + "' to crate a new IR system or '"
                    + OptionsIRS.LOAD_IRS + "' to load an already existing one: ");
            try {
                char optionIRS_in = SCANNER_READER.nextLine().toUpperCase().charAt(0);
                option = Arrays.stream(OptionsIRS.values())
                        .filter(p -> p.charSelection == optionIRS_in)
                        .findAny().orElse(null);
            } catch (Exception ignored) {
            }
        } while (option == null);

        System.out.println();
        InformationRetrievalSystem irs;
        try {
            irs = switch (option) {
                case LOAD_IRS -> {
                    var irs_ = loadIRS();
                    yield irs_ == null ? createNewIRS() : irs_;
                }
                case CREATE_NEW_IRS -> createNewIRS();
            };
        } catch (NoAvailableCorpus e) {
            System.out.println("No available corpus for indexing. The program will terminate.");
            return null;
        } catch (NoMoreDocIdsAvailable e) {
            System.out.println("Corpus is too large and cannot be indexed. The program will terminate.");
            return null;
        }
        return irs;
    }

    /**
     * Prints on {@link System#out} the instructions to use the system.
     */
    private static void printInstructions() {
        System.out.println("INSTRUCTIONS" + System.lineSeparator() +
                "Insert:" + System.lineSeparator() +
                "\t'" + PHONETIC_CORRECTION_REQUEST_PREFIX + "' before the query " +
                "string to enable the phonetic correction, " + System.lineSeparator() +
                "\t'" + SPELLING_CORRECTION_REQUEST_PREFIX + "' before the query " +
                "string to enable the spelling correction, " + System.lineSeparator() +
                "\tthe query string only to retrieve exact matches." +
                System.lineSeparator() +
                "Spelling and phonetic correction can be used together. For each of them, " + System.lineSeparator() +
                "the number of corrections to attempt can be specified by " + System.lineSeparator() +
                "adding the number (the edit distance) after the flag (e.g., \"" +
                PHONETIC_CORRECTION_REQUEST_PREFIX + "2 foo&bar\" " + System.lineSeparator() +
                "means to try to correct (phonetic correction) two times; this does " + System.lineSeparator() +
                "not mean that the final query string (after correction) will have " + System.lineSeparator() +
                "edit distance of 2 from the initial query string, but it means that " + System.lineSeparator() +
                "it will have an edit distance of at least 2, in fact, suppose that " + System.lineSeparator() +
                "no corrections are available at distance 1, so the system automatically " + System.lineSeparator() +
                "increases the edit-distance to 2 and re-try: after one single attempt " + System.lineSeparator() +
                "of correction, in this example, the overall edit-distance will be 2, " + System.lineSeparator() +
                "but in the input string we specified to try to correct 2 times, hence " + System.lineSeparator() +
                "another correction attempt will be made and the resulting edit distance " + System.lineSeparator() +
                "will be >=2).");
        System.out.println("The query string can have AND, OR or NOT operations (use the symbols '&', '|', '!' " +
                System.lineSeparator() + "respectively, spaces are interpreted as AND queries).");
    }

    /**
     * Load one already-created IR system from the file system.
     * This method asks directly to the user what IR System to load.
     *
     * @return the {@link InformationRetrievalSystem} chosen by the user
     * or null if no IR systems are saved.
     */
    private static InformationRetrievalSystem loadIRS() {
        if (FOLDER_WITH_IRS != null) {
            File[] filesOfIRS = FOLDER_WITH_IRS.listFiles();
            if (Objects.requireNonNull(filesOfIRS).length == 0) {
                System.out.println("No IR Systems are available");
                return null;
            } else {
                System.out.println("Available IR Systems:");
                for (int i = 0; i < Objects.requireNonNull(filesOfIRS).length; i++) {
                    System.out.println("\t " + (i + 1) + ") \t" + filesOfIRS[i].getName());
                }
                File fileWithIrsChosen = null;
                do {
                    System.out.print("Insert the number of the IRS that you want to load: ");
                    try {
                        int insertedIrsIndex = Integer.parseInt(
                                SCANNER_READER.nextLine().replaceAll("\\s+", ""));
                        if (1 <= insertedIrsIndex && insertedIrsIndex <= filesOfIRS.length) {
                            fileWithIrsChosen = filesOfIRS[insertedIrsIndex - 1];
                        }
                    } catch (Exception ignored) {
                    }
                    if (fileWithIrsChosen == null) {
                        System.out.println("Please insert an integer between 1 and " + filesOfIRS.length + ".");
                    }
                } while (fileWithIrsChosen == null);

                System.out.println("Loading the IRSystem from the file system, please wait...");
                try (
                        ProgressInputStream pis = new ProgressInputStream(
                                new FileInputStream(fileWithIrsChosen), fileWithIrsChosen.length());
                        ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(pis))) {

                    PropertyChangeListener listener = evt -> {  // listener to print the progress of IRS loading
                        Supplier<Boolean> shouldPrintProgress = new Supplier<>() {
                            private final static long UPDATE_PERIOD_MILLIS = 1000;
                            private final static double MIN_DELTA_BETWEEN_PRINTED_PERCENTAGES = 0.001; // percentage in [0,1], e.g. 0.001 is 0.1%
                            private static long lastUpdate = 0;
                            private static double oldPrintedProgressPercentage = Double.MIN_VALUE;

                            @Override
                            public Boolean get() {
                                long now = System.currentTimeMillis();
                                if (now - lastUpdate > UPDATE_PERIOD_MILLIS) {
                                    double currentProgress = pis.getProgress();
                                    if (currentProgress - oldPrintedProgressPercentage > MIN_DELTA_BETWEEN_PRINTED_PERCENTAGES) {
                                        oldPrintedProgressPercentage = currentProgress;
                                        lastUpdate = now;
                                        return true;
                                    }
                                }
                                return false;
                            }
                        };
                        if (shouldPrintProgress.get()) {
                            System.out.print("\t" + ((int) (pis.getProgress() * 10000)) / 100.0 + "% ");
                        }
                    };
                    pis.addPropertyChangeListener(listener);
                    Object irSystem_object = ois.readObject();
                    pis.removePropertyChangeListener(listener);
                    System.out.println();   // add new line for output formatting

                    if (irSystem_object instanceof InformationRetrievalSystem irs) {
                        System.out.println("IRSystem loaded from the file system.");
                        return irs;
                    } else {
                        System.err.println("Invalid file.");
                        if (!fileWithIrsChosen.delete()) {
                            Logger.getLogger(Main.class.getCanonicalName())
                                    .log(Level.SEVERE, "Unable to delete file " + fileWithIrsChosen.getAbsolutePath());
                        }
                        throw new Exception("Deserialized object is not an instance of "
                                + InformationRetrievalSystem.class.getCanonicalName());
                    }
                } catch (Exception e) {
                    System.err.println("Errors occurred when reading the IR System from the file system. " +
                            "Please, create it");
                    Logger.getLogger(Main.class.getCanonicalName())
                            .log(Level.SEVERE, "Error reading IR system from file.", e);
                    System.out.println();
                    return null;
                }

            }
        } else {
            System.out.println("No IR System found in the file system.");
            return null;
        }
    }

    /**
     * Method which expects one input query string (requested by the method itself
     * to the user directly), retrieves the documents and shows them to the user.
     * Then the method requests to the user if they want to perform another query
     * and return the answer to the caller.
     *
     * @param irs The {@link InformationRetrievalSystem}.
     * @return true if the user wants to perform another query, false otherwise.
     */
    private static boolean answerOneQuery(InformationRetrievalSystem irs) {
        System.out.println();
        System.out.println("Insert a query or '" + EXIT_REQUEST + "' to exit: ");
        String queryString = SCANNER_READER.nextLine().strip();
        if (queryString.equalsIgnoreCase(EXIT_REQUEST)) {
            return false;   // user wants to stop querying the system
        }

        AtomicBoolean usePhoneticCorrection = new AtomicBoolean(false);
        AtomicInteger phoneticCorrectionMaxEditDistance = new AtomicInteger();
        AtomicBoolean useSpellingCorrection = new AtomicBoolean(false);
        AtomicInteger spellingCorrectionMaxEditDistance = new AtomicInteger();

        {
            // parse the query string to decide if applying spelling/phonetic correction
            //noinspection RegExpDuplicateAlternationBranch
            final Matcher USE_CORRECTIONS = Pattern.compile(
                            "\\s*((" +
                                    "((" + PHONETIC_CORRECTION_REQUEST_PREFIX + ")(\\d*))" +
                                    "|" +
                                    "((" + SPELLING_CORRECTION_REQUEST_PREFIX + ")(\\d*))" +
                                    ")\\s*){0,2}\\s*.*")
                    .matcher(queryString);
            final int USE_PHONETIC_CORRECTION_GROUP = 4;
            final int EDIT_DISTANCE_PHONETIC_CORRECTION_GROUP = 5;
            final int USE_SPELLING_CORRECTION_GROUP = 7;
            final int EDIT_DISTANCE_SPELLING_CORRECTION_GROUP = 8;

            Function<@Nullable String, @NotNull Integer> get1IfNullOrParseTheValue =
                    nullableStringRepresentingAnInteger -> Math.max(
                            0 /*must be >=1*/,
                            nullableStringRepresentingAnInteger == null || nullableStringRepresentingAnInteger.isBlank()
                                    ? 1
                                    : Integer.parseInt(nullableStringRepresentingAnInteger));
            if (USE_CORRECTIONS.find()) {
                // prepare the system to evaluate query with corrections
                Predicate<OptionsSpellingCorrection> shouldApplyCorrection = correctionType -> {
                    int groupNumberInRegex = switch (correctionType) {
                        case USE_PHONETIC_CORRECTION -> USE_PHONETIC_CORRECTION_GROUP;
                        case USE_SPELLING_CORRECTION -> USE_SPELLING_CORRECTION_GROUP;
                    };
                    String groupValue = USE_CORRECTIONS.group(groupNumberInRegex);
                    return groupValue != null && !groupValue.isBlank();
                };
                BiFunction<OptionsSpellingCorrection, String, String> setEditDistanceAndGetCleanedQueryString =
                        (correctionType, queryString_) -> {
                            AtomicBoolean useCorrection;
                            AtomicInteger maxEditDistance;
                            String queryStringPrefixSpecifyingTheCorrectionToApply;
                            int editDistanceGroupInRegex = switch (correctionType) {
                                case USE_PHONETIC_CORRECTION -> {
                                    useCorrection = usePhoneticCorrection;
                                    maxEditDistance = phoneticCorrectionMaxEditDistance;
                                    queryStringPrefixSpecifyingTheCorrectionToApply = PHONETIC_CORRECTION_REQUEST_PREFIX;
                                    yield EDIT_DISTANCE_PHONETIC_CORRECTION_GROUP;
                                }
                                case USE_SPELLING_CORRECTION -> {
                                    useCorrection = useSpellingCorrection;
                                    maxEditDistance = spellingCorrectionMaxEditDistance;
                                    queryStringPrefixSpecifyingTheCorrectionToApply = SPELLING_CORRECTION_REQUEST_PREFIX;
                                    yield EDIT_DISTANCE_SPELLING_CORRECTION_GROUP;
                                }
                                default -> throw new IllegalStateException("Unexpected value: " + correctionType);
                            };
                            useCorrection.set(true);
                            String editDistanceIfInserted = USE_CORRECTIONS.group(editDistanceGroupInRegex);
                            editDistanceIfInserted = editDistanceIfInserted == null ? "" : editDistanceIfInserted;
                            maxEditDistance.set(get1IfNullOrParseTheValue.apply(editDistanceIfInserted));
                            return queryString_.replace(
                                    queryStringPrefixSpecifyingTheCorrectionToApply
                                            + editDistanceIfInserted, "");
                        };
                BiFunction<OptionsSpellingCorrection, String, String> applyCorrectionIfRequiredAndGetCleanedQueryString =
                        (correctionType, queryString_) -> {
                            if (shouldApplyCorrection.test(correctionType)) {
                                return setEditDistanceAndGetCleanedQueryString.apply(correctionType, queryString_);
                            } else {
                                return queryString_;
                            }
                        };
                queryString = applyCorrectionIfRequiredAndGetCleanedQueryString.apply(
                        OptionsSpellingCorrection.USE_PHONETIC_CORRECTION, queryString);
                queryString = applyCorrectionIfRequiredAndGetCleanedQueryString.apply(
                        OptionsSpellingCorrection.USE_SPELLING_CORRECTION, queryString);
            }
        }

        var be = irs.createNewBooleanExpression().parseQuery(queryString);

        {   // Handle spelling correction
            var bePCorrection = new BooleanExpression(be);
            var beSCorrection = new BooleanExpression(be);

            if (usePhoneticCorrection.get()) {
                for (int i = 0; i < phoneticCorrectionMaxEditDistance.get(); i++) {
                    bePCorrection.spellingCorrection(true, true);
                }
            }
            if (useSpellingCorrection.get()) {
                for (int i = 0; i < spellingCorrectionMaxEditDistance.get(); i++) {
                    beSCorrection.spellingCorrection(false, true);
                }
            }

            if (usePhoneticCorrection.get() && useSpellingCorrection.get()) {
                Function<String, Set<String>> wordsExtractor = str ->
                        Arrays.stream(str.replaceAll("[^\\w]", " ")
                                        .replaceAll("\\s+", " ")
                                        .split(" "))
                                .collect(Collectors.toSet());
                Set<String> wordsInPCorrection = wordsExtractor.apply(bePCorrection.getQueryString());
                Set<String> wordsInSCorrection = wordsExtractor.apply(beSCorrection.getQueryString());
                if (wordsInPCorrection.equals(wordsInSCorrection)) {
                    be = bePCorrection;
                } else {    // no sense to compute OR if the resulting query is the same
                    String entireQueryString = bePCorrection.or(beSCorrection).getQueryString();
                    for (var op : BINARY_OPERATOR.values()) {
                        entireQueryString = entireQueryString.replaceAll(op.toString(), op.getSymbol());
                    }
                    for (var op : UNARY_OPERATOR.values()) {
                        entireQueryString = entireQueryString.replaceAll(op.toString(), op.getSymbol());
                    }
                    be = irs.createNewBooleanExpression().parseQuery(entireQueryString);  // simplify the resulting query string
                    // because both spelling and phonetic correction might lead to the same corrected words
                }
            } else if (usePhoneticCorrection.get()) {
                be = bePCorrection;
            } else if (useSpellingCorrection.get()) {
                be = beSCorrection;
            }
        }

        long start, end;
        start = System.nanoTime();
        var results = be.evaluate();
        end = System.nanoTime();
        var tmpResults = results;
        var numberOfAlreadyShowedResults = 0;
        System.out.print(results.size() + " results found in " + (end - start) / 1e6 + " ms");
        if (usePhoneticCorrection.get() || useSpellingCorrection.get()) {
            System.out.println(" for " + be.getQueryString());
        } else {
            System.out.println();
        }
        final int DEFAULT_NUM_OF_RESULTS_TO_PRINT = 10;
        boolean userWantsMoreResults;

        do { // results printing

            userWantsMoreResults = false;   // initialization at each iteration

            if (tmpResults.size() > DEFAULT_NUM_OF_RESULTS_TO_PRINT) {
                tmpResults = results.subList(
                        numberOfAlreadyShowedResults, numberOfAlreadyShowedResults + DEFAULT_NUM_OF_RESULTS_TO_PRINT);
                if (numberOfAlreadyShowedResults == 0) {
                    System.out.println("First " + DEFAULT_NUM_OF_RESULTS_TO_PRINT + " results:");
                }
            }
            tmpResults.stream().map(r -> "\t- " + r).forEachOrdered(System.out::println);
            numberOfAlreadyShowedResults += tmpResults.size();
            tmpResults = results.subList(numberOfAlreadyShowedResults, results.size()); // prepare for the next iteration

            int numOfUnseenResults = results.size() - numberOfAlreadyShowedResults;
            if (numOfUnseenResults > 0) {
                userWantsMoreResults = OptionsYesNo.getFromStdIn(
                        numOfUnseenResults + " result" + (numOfUnseenResults > 1 ? "s" : "") + " not showed yet. " +
                                "Do you want to see more results?").toBoolean();
            }
        } while (userWantsMoreResults);

        return true;       // user wants to continue querying the system
    }

    /**
     * Method executed when the user decides to create a new Information Retrieval System.
     *
     * @return the created {@link InformationRetrievalSystem}.
     * @throws IOException           If I/O errors occur while reading the corpus.
     * @throws NoAvailableCorpus     If no corpus are available for indexing.
     * @throws NoMoreDocIdsAvailable If during the corpus creation no more docIds were available.
     */
    private static InformationRetrievalSystem createNewIRS()
            throws IOException, NoAvailableCorpus, NoMoreDocIdsAvailable {
        var availableCorpusFactories = getCorpusFactories();
        switch (availableCorpusFactories.size()) {
            case 0 -> System.out.println("No available collections to index.");
            case 1 -> System.out.println("Available collection to index: ");
            default -> System.out.println("Available collections to index: ");
        }
        if (availableCorpusFactories.size() > 0) {
            for (int i = 0; i < availableCorpusFactories.size(); i++) {
                System.out.println("\t " + (i + 1) + ") \t" + availableCorpusFactories.get(i).getCorpusName());
            }
            CorpusFactory<?> corpusFactoryOfCollectionToIndex = null;
            do {
                System.out.print("Insert the number of the collection that you want to index: ");
                try {
                    int insertedCollectionIndex = Integer.parseInt(
                            SCANNER_READER.nextLine().replaceAll("\\s+", ""));
                    if (1 <= insertedCollectionIndex && insertedCollectionIndex <= availableCorpusFactories.size()) {
                        corpusFactoryOfCollectionToIndex = availableCorpusFactories.get(insertedCollectionIndex - 1);
                    }
                } catch (Exception ignored) {
                }
                if (corpusFactoryOfCollectionToIndex == null) {
                    System.out.println(
                            "Please insert an integer between 1 and " + availableCorpusFactories.size() + ".");
                }
            } while (corpusFactoryOfCollectionToIndex == null);

            var irs = new InformationRetrievalSystem(corpusFactoryOfCollectionToIndex.createCorpus());

            boolean saveToFile = OptionsYesNo.getFromStdIn("Do you want to save the IR system to file?").toBoolean();
            if (saveToFile) {

                String fileName_irSystem = FOLDER_WITH_IRS + File.separator
                        + corpusFactoryOfCollectionToIndex.getCorpusName();
                File file_irSystem = new File(fileName_irSystem);

                // Serialize and save the IR System to the file system
                if (file_irSystem.createNewFile()) {    // if file already exists will do nothing
                    System.out.println("File \"" + fileName_irSystem + "\" created.");
                } else {
                    System.out.println("File \"" + fileName_irSystem + "\" already exists. It will be replaced.");
                }
                System.out.println("Serializing and saving the system on file, please wait...");
                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new BufferedOutputStream(
                                new FileOutputStream(fileName_irSystem, false)))) {
                    oos.writeObject(irs);
                    oos.flush();
                    System.out.println("IR System saved to file " + fileName_irSystem);
                }

            }

            return irs;

        } else {
            throw new NoAvailableCorpus();
        }
    }

    /**
     * @return the {@link List} of available {@link CorpusFactory Corpus factories}.
     */
    @NotNull
    private static List<? extends CorpusFactory<?>> getCorpusFactories() throws IOException {
        //noinspection unchecked
        return ClassLoading.getAllClasses().stream()
                .filter(CorpusFactory.class::isAssignableFrom)
                .filter(aClass -> !Modifier.isAbstract(aClass.getModifiers())
                        && !aClass.isEnum() && !aClass.isInterface())
                .map(aClass -> (Class<CorpusFactory<?>>) aClass)
                .map(corpusFactoryClass -> {
                    CorpusFactory<?> corpusFactory = null;
                    try {
                        Constructor<?> ctor = corpusFactoryClass.getDeclaredConstructor();
                        ctor.setAccessible(true);
                        corpusFactory = (CorpusFactory<?>) ctor.newInstance();
                    } catch (NoSuchMethodException | InstantiationException |
                            IllegalAccessException | InvocationTargetException e) {
                        System.err.println("Error instantiating corpus factory " + corpusFactoryClass);
                        e.printStackTrace();
                    }
                    return corpusFactory;
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(CorpusFactory::getCorpusName))
                .toList();
    }

    /**
     * Enumeration of possible options for the user (creation of a new
     * Information Retrieval System vs loading one from the file system).
     */
    private enum OptionsIRS {
        /**
         * Option for which a new Information Retrieval System must be created.
         */
        CREATE_NEW_IRS('C'),
        /**
         * Option for which an already created Information Retrieval System must be loaded.
         */
        LOAD_IRS('L');

        /**
         * A character associated with the option.
         */
        private final char charSelection;

        /**
         * Constructor.
         *
         * @param charSelection The character associated with the option.
         */
        OptionsIRS(char charSelection) {
            this.charSelection = charSelection;
        }

        @Override
        public String toString() {
            return String.valueOf(charSelection);
        }
    }

    /**
     * Enumeration of possible options for the user (if they want to use
     * spelling correction).
     */
    private enum OptionsSpellingCorrection {
        /**
         * Option for which spelling correction must be used.
         */
        USE_SPELLING_CORRECTION('s'),
        /**
         * Option for which phonetic correction must be used.
         */
        USE_PHONETIC_CORRECTION('p');

        /**
         * A character associated with the option.
         */
        private final char charSelection;

        /**
         * Constructor.
         *
         * @param charSelection The character associated with the option.
         */
        OptionsSpellingCorrection(char charSelection) {
            this.charSelection = charSelection;
        }

        @Override
        public String toString() {
            return String.valueOf(charSelection);
        }
    }

    /**
     * Enumeration to describe a "yes/no" option.
     */
    private enum OptionsYesNo {
        /**
         * "Yes" option.
         */
        YES('y'),
        /**
         * "No" option.
         */
        NO('n');

        /**
         * A character associated with the option.
         */
        private final char charSelection;

        /**
         * Constructor.
         *
         * @param charSelection The character associated with the option.
         */
        OptionsYesNo(char charSelection) {
            this.charSelection = charSelection;
        }

        /**
         * Requests to the user (via stdIn) the {@link OptionsYesNo}.
         *
         * @param messageToDisplay The message to display.
         * @return the {@link OptionsYesNo} inserted on {@link System#in}.
         */
        @NotNull
        public static OptionsYesNo getFromStdIn(@NotNull String messageToDisplay) {
            OptionsYesNo input = null;
            do {
                System.out.print(messageToDisplay + " ["
                        + Arrays.stream(OptionsYesNo.values()).map(OptionsYesNo::toString).collect(Collectors.joining("/"))
                        + "]: ");
                try {
                    input = valueOf(SCANNER_READER.nextLine().toLowerCase().strip().toLowerCase().charAt(0));
                } catch (Exception ignored) {
                }
            } while (input == null);
            return input;
        }

        /**
         * @param charSelection The character for which the respective {@link OptionsYesNo}
         *                      enum constant is requested.
         * @return the enum constant of this type with the specified {@link #charSelection}
         * or null if no match is found.
         */
        @Nullable
        public static OptionsYesNo valueOf(char charSelection) {
            return Arrays.stream(OptionsYesNo.values())
                    .filter(enumVal -> enumVal.charSelection == charSelection)
                    .findAny()
                    .orElse(null);
        }

        /**
         * @return the natural boolean value associated with this instance.
         * (YES converted to true, NO to false).
         */
        public boolean toBoolean() {
            return switch (this) {
                case YES -> true;
                case NO -> false;
            };
        }

        @Override
        public String toString() {
            return String.valueOf(charSelection);
        }
    }

    /**
     * Class defining a {@link Throwable} object to be thrown if no
     * corpus is available.
     */
    private static class NoAvailableCorpus extends Throwable {
    }
}
