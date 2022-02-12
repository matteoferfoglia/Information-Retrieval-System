package it.units.informationretrieval.ir_boolean_model.evaluation.test_queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.evaluation.Benchmarking;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies.Movie;
import it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies.MovieCorpusFactory;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import skiplist.SkipList;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests the system on a set of test queries.
 * For these tests, the {@link Movie} corpus is used.
 * <p/>
 * <strong>Notice</strong>: the system was already opportunely tested
 * and this class is <strong>not</strong> intended for test purposes,
 * but most for demonstration use.
 */
class TestQueries {

    /**
     * The {@link Language} used for test queries.
     */
    final static Language USED_LANGUAGE = Language.ENGLISH;

    /**
     * The folder name where to save results.
     */
    static final String FOLDER_NAME_TO_SAVE_RESULTS = "system_evaluation" + File.separator
            + "test_queries" + Benchmarking.PROPERTY_CONCATENATION;
    /**
     * Flag: if set to true, results will be printed to {@link System#out}.
     */
    final static boolean PRINT_RESULTS = true;
    /**
     * Max number of results to print.
     */
    private final static int MAX_NUM_OF_RESULTS_TO_PRINT = 10;
    /**
     * Max number of characters to print for each result (to avoid printing too many things).
     */
    private final static int MAX_NUM_OF_CHARS_TO_PRINT = 84 - 3 - 7;   // nothing special, only for prettier output format
    /**
     * {@link Document}s randomly chosen for tests.
     */
    static Document doc1, doc2;
    /**
     * The {@link InformationRetrievalSystem} used for tests.
     */
    static InformationRetrievalSystem irs;
    /**
     * Supplies a word which is contained both in {@link #doc1} and in {@link #doc2}.
     */
    static Supplier<String> wordsContainedBothInFirstAndSecondDocumentSupplier;
    /**
     * Supplies a word which is contained in {@link #doc1} but not in {@link #doc2}.
     */
    static Supplier<String> wordsContainedInFirstButNotInSecondDocumentSupplier;
    /**
     * Supplies a word which is contained in {@link #doc2} but not in {@link #doc1}.
     */
    static Supplier<String> wordsContainedInSecondButNotInFirstDocumentSupplier;
    /**
     * Supplies a word which is contained neither in {@link #doc1} nor in {@link #doc2}.
     */
    static Supplier<String> wordsContainedNeitherInFirstNorInSecondDocumentSupplier;
    /**
     * The {@link java.io.BufferedWriter} to print results to file.
     */
    private static Writer WRITER_TO_FILE;

    static {    // static initializer block
        try {
            // init fields without printing anything to stdOut
            var stdOut = System.out;
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            Corpus movieCorpus = new MovieCorpusFactory().createCorpus();
            irs = new InformationRetrievalSystem(movieCorpus);
            System.setOut(stdOut);
            doc1 = new Movie(
                    // important: invoke this constructor after the creation of the corpus, otherwise languages, genres and countries will not be initialized
                    "Space Jam", LocalDate.of(1996, 11, 10), -1, 83 * 60,
                    List.of("/m/02h40lc"), List.of("/m/09c7w0"),
                    Arrays.asList("/m/06n90", "/m/03k9fj", "/m/0hj3myq", "/m/0hcr", "/m/01z02hx", "/m/0hj3mt0", "/m/01hmnh", "/m/01z4y", "/m/0hqxf"),
                    """
                            A group of criminal aliens called the The Nerdlucks, led by their boss, Mister Swackhammer , plot to capture the Looney Tunes characters and make them their newest attractions in order to save their failing amusement park called Moron Mountain from foreclosure and bring in more customers. Seeing how short the aliens are, the Looney Tunes bargain for the freedom by challenging the Nerdlucks to a basketball game. Preparing to cheat in the game, the Nerdlucks return to Earth and steal the basketball talents of Patrick Ewing, Larry Johnson, Charles Barkley, Muggsy Bogues and Shawn Bradley. The Nerdlucks use their stolen talent to become the "Monstars" , gigantic creatures that the Looney Tunes are unable to defeat. To help them win, the characters recruit Jordan, who reluctantly agrees after the Monstars squash him into the shape of a basketball and bounce him around like one. In the beginning of the game between the TuneSquad and the Monstars, the Looney Tunes are injured one by one until only Jordan, Bugs, Lola and Daffy are left in the game, leaving them short one player. Marvin The Martian, who is the referee tells them that if there is no fifth player, the team will forfeit the game. At the last second, Bill Murray appears in the stadium and joins the team, narrowly averting forfeiture. Meanwhile, Jordan reluctantly makes a deal with Mister Swackhammer to spare the Looney Tunes in exchange for his own freedom as his newest attraction if the TuneSquad loses. He readily accepts it and Bugs tries to talk him out of it, apparently aware what it means for Jordan being subjected to humiliation on Moron Mountain for all time. At the game's climax, the TuneSquad are down by one, and it is up to Jordan to score the winning point. Extending his arm with the power of toon physics, Jordan makes the basket and wins the game. He convinces the Monstars that they're bigger than Mister Swackhammer, who yells at them for losing. Fed up with their boss, the Monstars tie him up and send him to the moon. At Jordan's request, they give back the stolen basketball talents from the other players by transferring them to a basketball. This reverts the Monstars back to the tiny Nerdlucks. Refusing to return to Moron Mountain to endure humiliation from their former boss, the Nerdlucks decide to stay with the Looney Tunes who only agree to let them if they can prove to be "looney". Afterwards, Jordan is returned back to Earth in the Nerdlucks' spaceship, where he makes a dramatic and appearance at the baseball game to the cheers of the audience, despite being late. The next day, Michael gives the stolen talent back to the other NBA players. He is later prompted by his rivals to return to the NBA, mirroring his real-life comeback. In a post-credits scene, Bugs Bunny appears in the classic bullseye featured in the original Looney Tunes shorts saying "That's all folks", only to be interrupted by Porky Pig, who in turn is interrupted by Daffy, who in turn gets thrown out by the Nerdlucks, leaving them to complete the line. Michael Jordan then lifts the page to ask if he can go home now.""");
            doc2 = new Movie(
                    // important: invoke this constructor after the creation of the corpus, otherwise languages, genres and countries will not be initialized
                    "Treasure Planet", LocalDate.of(2002, 11, 5), 109578115, 95 * 60,
                    List.of("/m/02h40lc"), List.of("/m/09c7w0"),
                    Arrays.asList("/m/06n90", "/m/03k9fj", "/m/0hj3myq", "/m/0hcr", "/m/02l7c8", "/m/0hqxf", "/m/0hj3n26"),
                    """
                            The film's prologue depicts Jim Hawkins as a five-year-old  reading a storybook in bed. Jim is enchanted by stories of the legendary pirate Captain Flint and his ability to appear from nowhere, raid passing ships, and disappear in order to hide the loot on the mysterious "Treasure Planet". Twelve years later, Jim  has grown into an aloof and alienated teenager. He is shown begrudgingly helping his mother Sarah  run an inn and deriving amusement from "solar surfing" , a pastime that frequently gets him in trouble. One day, a spaceship crashes near the inn. The dying pilot, Billy Bones , gives Jim a sphere and tells him to "beware the cyborg". Shortly thereafter, a gang of pirates raid and burn the inn. Jim, his mother, and their dog-like friend Dr. Delbert Doppler  barely escape. The sphere turns out to be a holographic projector, showing a map that Jim realizes leads to Treasure Planet. Doppler commissions a ship called RLS Legacy, on a mission to find Treasure Planet. The ship is commanded by the cat-like, sharp-witted Captain Amelia  along with her stony-skinned and disciplined First Mate, Mr. Arrow . The crew is a motley bunch, secretly led by cook John Silver ([[Brian Murray , whom Jim suspects is the cyborg of whom he was warned. Jim is sent down to work in the galley; despite his mistrust of Silver, they soon form a tenuous father-son relationship (a montage featuring the song "[[I'm Still Here . During an encounter with a supernova, Silver falls overboard but is saved by Jim. The supernova then devolves into a black hole, where Arrow drifts overboard and is lost, for which Jim blames himself for failing to secure the lifelines, while in fact Arrow's line was cut by a ruthless insectoid crew member named Scroop . As the ship reaches Treasure Planet, mutiny erupts, led by Silver. Jim, Doppler, and Amelia abandon the ship, accidentally leaving the map behind. Silver, who believes that Jim has the map, has a chance to kill Jim, but refuses to do so because of his attachment to the boy. The fugitives are shot down by a mutineer during their escape, causing injury to Amelia. While exploring Treasure Planet's forests, the fugitives meet B.E.N. , an abandoned, whimsical robot who claims to have lost most of his memory and invites them to his house to care for the wounded Amelia. The pirates corner the group here; using a back-door, Jim and B.E.N. return to the ship in an attempt to recover the map. Scroop, aboard the ship as lookout, stalks and fights Jim. B.E.N., working to sabotage the ship's artillery, accidentally turns off the artificial gravity, whereupon Jim and Scroop threaten to float off into space. Jim grabs the mast while Scroop becomes entangled in the flag and cuts himself free while Scroop floats away, presumably to his death. Jim and B.E.N. obtain the map. Upon their return, they are captured by Silver, who has already captured Doppler and Amelia. When Jim is forced to use the map, the group finds their way to a portal that can be opened to any place in the universe; this being the means by which Flint conducted his raids. The treasure is at the center of the planet, accessible only via the portal. Treasure Planet is revealed to be a large space station built by unknown architects and commandeered by Flint. In the stash of treasure, Jim comes across the skeletal remains of Flint himself, holding a missing part of B.E.N's cognitive computer. Jim replaces this piece, causing B.E.N. to remember that the planet is set to explode upon the treasure's discovery. In the ensuing catastrophe, Silver finds himself torn between holding onto a literal boat-load of gold and saving Jim, who hangs from a precipice after a fall. Silver saves Jim, and the group escapes to the Legacy, which is damaged and lacks the motive power required to leave the planet in time to escape. Jim attaches a rocket to a narrow plate of metal and rides it toward the portal to open it to a new location while Doppler pilots the ship behind him. Jim manages to open the portal to his home world's spaceport, through which all escape the destruction of Treasure Planet. After the escape, Amelia has the surviving pirates imprisoned aboard the ship and offers to recommend Jim to the Interstellar Academy for his heroic actions. Silver sneaks below deck, where Jim finds him preparing his escape. Jim lets him go, inheriting Silver's shape-changing pet called Morph . Silver predicts that Jim will "rattle the stars", then tosses him a handful of jewels and gold he had taken from Treasure Planet to pay for rebuilding the inn. The film ends with a party at the rebuilt inn, showing Doppler and Amelia now married with children, and Jim a military cadet. He looks to the skies and sees an image of Silver in the clouds.""");

            assert doc1.getContent() != null;
            assert doc2.getContent() != null;

            List<String> fromFirstDoc = getWordsFromDoc(doc1);
            List<String> fromSecondDoc = getWordsFromDoc(doc2);

            // Handle stemming (if performed)
            Stemmer stemmer = Utility.getStemmer();

            wordsContainedNeitherInFirstNorInSecondDocumentSupplier =
                    new WordSupplier(Arrays.asList("foo", "bar", "pippo", "pluto", "paperino"));
            wordsContainedBothInFirstAndSecondDocumentSupplier =
                    new WordSupplier(Utility.intersectionOfSortedLists(fromFirstDoc, fromSecondDoc));
            var wordsContainedInFirstButNotInSecondDocumentList =
                    getWordsContainedInDifferenceOfTwoListsButExcludeWordsWhichStemmingIsPresentInStemmedWordsOfThirdList(
                            fromFirstDoc, fromSecondDoc, stemmer);
            var wordsContainedInSecondButNotInFirstDocumentList =
                    getWordsContainedInDifferenceOfTwoListsButExcludeWordsWhichStemmingIsPresentInStemmedWordsOfThirdList(
                            fromSecondDoc, fromFirstDoc, stemmer);
            wordsContainedInFirstButNotInSecondDocumentSupplier =
                    new WordSupplier(wordsContainedInFirstButNotInSecondDocumentList);
            wordsContainedInSecondButNotInFirstDocumentSupplier =
                    new WordSupplier(wordsContainedInSecondButNotInFirstDocumentList);
        } catch (NoMoreDocIdsAvailable | InvocationTargetException | NoSuchMethodException
                | IllegalAccessException | IOException e) {
            Logger.getLogger(TestQueriesWithQueryStringParsing.class.getCanonicalName())
                    .log(Level.SEVERE, "Error during class initialization", e);
        }
    }

    /**
     * Method to create the output file where results will be printed.
     */
    static void createOutputFileOfResults() {
        try {
            File directory = new File(FOLDER_NAME_TO_SAVE_RESULTS);
            if (!directory.exists()) {
                if (!directory.mkdirs()) {
                    throw new IOException("Output file not created.");
                }
            }
            var currentDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.now());
            File f = new File(FOLDER_NAME_TO_SAVE_RESULTS + File.separator + currentDateTime + ".txt");
            if (!f.createNewFile()) {
                throw new IOException("Output file not created.");
            }
            WRITER_TO_FILE = new BufferedWriter(new FileWriter(f));
        } catch (IOException e) {
            WRITER_TO_FILE = new BufferedWriter(new CharArrayWriter());
            e.printStackTrace();
        }
    }

    @BeforeAll
    static void noticeIfQueryResultsWillBePrinted() throws IOException {
        createOutputFileOfResults();
        noticeIfQueryResultsWillBePrinted(WRITER_TO_FILE);
    }

    /**
     * Write to the output file and eventually (according to {@link #PRINT_RESULTS})
     * to {@link System#out} a notice about these tests.
     */
    static void noticeIfQueryResultsWillBePrinted(@NotNull Writer outputWriter) throws IOException {
        String out = """
                                
                ====================================================================================
                ======                              TEST QUERIES                              ======
                ====================================================================================
                                
                """
                + "Notice: At most " + MAX_NUM_OF_RESULTS_TO_PRINT + " results will be printed for each query.\n"
                + "Notice: if assertions are enabled, timing values are unreliable.\n";
        outputWriter.write(out);
        outputWriter.flush();
        if (PRINT_RESULTS) {
            System.out.println(out);
        }
    }

    @AfterAll
    static void printEndOfTests() throws IOException {
        printEndOfTests(WRITER_TO_FILE);
    }

    /**
     * Write to the output file and eventually (according to {@link #PRINT_RESULTS})
     * to {@link System#out} a notice about the end of these tests.
     */
    static void printEndOfTests(@NotNull Writer writer) throws IOException {
        String out = """

                ====================================================================================
                ======                           TEST QUERIES - END                           ======
                ====================================================================================
                                
                """;
        writer.write(out);
        writer.flush();
        if (PRINT_RESULTS) {
            System.out.println(out);
        }
    }

    /**
     * Method to print and return query results, according
     * to the flag {@link #PRINT_RESULTS}.
     *
     * @param be The {@link BooleanExpression} to print.
     * @return the {@link List} of results.
     */
    static List<Document> evaluatePrintAndGetResultsOf(BooleanExpression be, Writer whereToWrite) throws IOException {
        long start, end;
        start = System.nanoTime();
        var results = be.evaluate();
        end = System.nanoTime();
        String out = results.size() + " result" + (results.size() != 1 ? "s" : "")
                + " for query " + be.getQueryString()
                + " found in " + ((end - start) / 1e6) + " ms :"
                + System.lineSeparator()
                + results.stream().limit(MAX_NUM_OF_RESULTS_TO_PRINT)
                .map(aResult -> "     - " + aResult)
                .map(aResult -> // cut the string (avoid printing too much)
                        aResult.substring(0, Math.min(aResult.length(), MAX_NUM_OF_CHARS_TO_PRINT))
                                + (aResult.length() > MAX_NUM_OF_CHARS_TO_PRINT ? "..." : ""))
                .collect(Collectors.joining(System.lineSeparator()))
                + (results.size() > 0 ? "\n" : "");
        whereToWrite.write(out + "\n");
        whereToWrite.flush();
        if (PRINT_RESULTS) {
            System.out.println(out);
        }
        return results;
    }

    /**
     * Write to the output file and eventually (according to {@link #PRINT_RESULTS})
     * to {@link System#out} a new line.
     */
    static void printNewLine(Writer writerToFile) throws IOException {
        writerToFile.write("\n");
        writerToFile.flush();
        if (PRINT_RESULTS) {
            System.out.println();
        }
    }

    /**
     * @param document A {@link Document}
     * @return the {@link List} of words (normalized but not stemmed) from
     * the given document. Stop-words are excluded.
     */
    private static List<String> getWordsFromDoc(Document document)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Method invalidCharsRemoverAndToLowerCase = Utility.class.getDeclaredMethod(
                "removeInvalidCharsAndToLowerCase", String.class, boolean.class);
        Method stopWordsIdentifier = Utility.class.getDeclaredMethod("isStopWord", String.class, Language.class, boolean.class);
        Stream.of(invalidCharsRemoverAndToLowerCase, stopWordsIdentifier).forEach(method -> method.setAccessible(true));
        return Arrays.stream(Utility.split(
                        (String) invalidCharsRemoverAndToLowerCase.invoke(
                                null, Objects.requireNonNull(document.getContent()).getEntireTextContent(), true)))
                .filter(word -> {
                    try {
                        return !(boolean) stopWordsIdentifier.invoke(null, word, USED_LANGUAGE, true /*true by default, but if the system is not using stemming, then no stemming is performed*/);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        return false;
                    }
                })
                .distinct()
                .sorted()
                .toList();
    }

    /**
     * Gets the difference between the first and the second input list (non-commutative),
     * then filters them to exclude words which stemming is present among the stemmed words
     * of the second list.
     *
     * @param firstList  The first input list.
     * @param secondList The second input list.
     * @param stemmer    The stemmer to use.
     */
    private static List<String>
    getWordsContainedInDifferenceOfTwoListsButExcludeWordsWhichStemmingIsPresentInStemmedWordsOfThirdList(
            List<String> firstList, List<String> secondList, Stemmer stemmer) {
        var stemmedWordsNotToBePresent =
                secondList.stream().map(word -> stemmer == null ? word : stemmer.stem(word, USED_LANGUAGE)).toList();
        return SkipList.difference(new SkipList<>(firstList), new SkipList<>(secondList), Comparator.naturalOrder())
                .stream()
                .sequential()
                .map(word -> new Pair<>(stemmer != null ? stemmer.stem(word, USED_LANGUAGE) : word, word))
                .filter(stemmedToUnstemmedPair -> stemmer == null || !stemmedWordsNotToBePresent.contains(stemmedToUnstemmedPair.getKey()))
                .map(Map.Entry::getValue)
                .toList();
    }

    @BeforeEach
    void printNewLineBeforeQuery() throws IOException {
        printNewLine(WRITER_TO_FILE);
    }

    @Test
    void illustrativeExample() throws IOException {
        var be = irs.createNewBooleanExpression().setMatchingValue("space").and("jam");
        evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
    }

    @Test
    void illustrativeExample2() throws IOException {
        // space AND (NOT jam)
        BooleanExpression be = irs.createNewBooleanExpression().setMatchingValue("space")
                .and(irs.createNewBooleanExpression().setMatchingValue("jam").not());
        evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
    }

    @Test
    void singleWordQuery() throws IOException {
        BooleanExpression be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
    }

    @Test
    void AND_query_containedInBoth() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query2containedIn1stButNotIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query_containedIn2ndButNotIn1st() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .and(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query2_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .and(wordsContainedInSecondButNotInFirstDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void OR_query_containedInBoth() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedBothInFirstAndSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2containedIn1stButNotIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedIn2ndButNotIn1st() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get())
                .or(wordsContainedBothInFirstAndSecondDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2_containedNeitherIn1stNorIn2nd() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .or(wordsContainedInSecondButNotInFirstDocumentSupplier.get());
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .not();
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query2() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingValue(wordsContainedInSecondButNotInFirstDocumentSupplier.get())
                .not();
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void phraseQuery() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase(new String[]{"Space", "jam"});
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Space *am".split(" "));
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery2() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Sp*ce *am".split(" "));
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void spellingCorrection() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Spade jam".split(" "))
                .spellingCorrection(false, true);
        assert 1 == be.getEditDistanceForSpellingCorrection();

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
    }

    @Test
    void phoneticCorrection() throws IOException {
        var be = irs.createNewBooleanExpression()
                .setMatchingPhrase("Space jem".split(" "))
                .spellingCorrection(
                        true,
                        true/*dangerous to set to false: will search for ALL possible corrections, risk of StackOverflowError*/
                );

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        var results = evaluatePrintAndGetResultsOf(be, WRITER_TO_FILE);
        assertTrue(results.contains(doc1));
    }

    /**
     * Implementation of a {@link Supplier} which produces words taken from
     * a list which must be provided at the moment of the object construction.
     */
    static class WordSupplier implements Supplier<String> {
        /**
         * The list of words that this supplier can produce.
         */
        private final List<String> words;

        /**
         * Counter used to supply different words each time the method
         * {@link #get()} is invoked.
         */
        private long counter = 0;

        /**
         * Constructor.
         *
         * @param words The {@link List} of words that will be supplied by this instance.
         */
        public WordSupplier(List<String> words) {
            this.words = Objects.requireNonNull(words)
                    .stream()
                    .filter(word -> Utility.normalize(word, true, USED_LANGUAGE) != null) // normalization might remove stop words
                    //  to solve normalization issues, the system might return *all* documents from the corpus, but it might be computational and time expensive
                    .toList();
        }

        /**
         * @return one word from {@link #words}.
         */
        @Override
        public String get() {
            assert words.size() > 1;
            return words.get((int) (counter++ % words.size()));
        }
    }

}