package it.units.informationretrieval.ir_boolean_model.evaluation.test_queries;

import it.units.informationretrieval.ir_boolean_model.InformationRetrievalSystem;
import it.units.informationretrieval.ir_boolean_model.document_descriptors.Movie;
import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.queries.BooleanExpression;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class tests the system on a set of test queries.
 * For these tests, the {@link Movie} corpus is used.
 * Queries are given as strings.
 * <p/>
 * <strong>Notice</strong>: the system was already opportunely tested
 * and this class is <strong>not</strong> intended for test purposes,
 * but most for demonstration use.
 * <p/>
 * The difference between this test class and {@link TestQueries} is
 * that in this class is showed how query can be constructed from
 * a string (query string parsing).
 */
class TestQueriesWithQueryStringParsing {

    // documents randomly chosen for tests
    static Document doc1, doc2;
    static InformationRetrievalSystem irs;

    static Supplier<String> wordsContainedBothInFirstAndSecondDocumentSupplier;
    static Supplier<String> wordsContainedInFirstButNotInSecondDocumentSupplier;
    static Supplier<String> wordsContainedInSecondButNotInFirstDocumentSupplier;
    static Supplier<String> wordsContainedNeitherInFirstNorInSecondDocumentSupplier;

    static {
        try {
            // init fields without printing anything to stdOut
            var stdOut = System.out;
            System.setOut(new PrintStream(new ByteArrayOutputStream()));
            Corpus movieCorpus = Movie.createCorpus();
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
            List<String> fromFirstDoc = Arrays.stream(Utility.split(doc1.getContent().getEntireTextContent())).sorted().toList();
            List<String> fromSecondDoc = Arrays.stream(Utility.split(doc2.getContent().getEntireTextContent())).sorted().toList();

            wordsContainedNeitherInFirstNorInSecondDocumentSupplier =
                    new WordSupplier(Arrays.asList("foo", "bar", "pippo", "pluto", "paperino"));
            wordsContainedBothInFirstAndSecondDocumentSupplier =
                    new WordSupplier(Utility.intersectionOfSortedLists(fromFirstDoc, fromSecondDoc));
            wordsContainedInFirstButNotInSecondDocumentSupplier =
                    new WordSupplier(new ArrayList<>(fromFirstDoc) {{
                        removeAll(((WordSupplier) wordsContainedBothInFirstAndSecondDocumentSupplier).getAllWords());
                    }});
            wordsContainedInSecondButNotInFirstDocumentSupplier =
                    new WordSupplier(new ArrayList<>(fromSecondDoc) {{
                        removeAll(((WordSupplier) wordsContainedBothInFirstAndSecondDocumentSupplier).getAllWords());
                    }});
        } catch (NoMoreDocIdsAvailable | URISyntaxException e) {
            Logger.getLogger(TestQueriesWithQueryStringParsing.class.getCanonicalName())
                    .log(Level.SEVERE, "Error during class initialization", e);
        }
    }

    @BeforeAll
    static void noticeIfQueryResultsWillBePrinted() {
        TestQueries.noticeIfQueryResultsWillBePrinted();
    }

    @AfterAll
    static void printEndOfTests() {
        TestQueries.printEndOfTests();
    }

    /**
     * Method to print and return query results.
     * See {@link TestQueries#evaluatePrintAndGetResultsOf(BooleanExpression)}.
     *
     * @param inputUnparsedQueryString The input (un-parsed) query string.
     * @return the {@link List} of results.
     */
    static List<Document> evaluatePrintAndGetResultsOf(String inputUnparsedQueryString) {
        System.out.println("Input query string: " + inputUnparsedQueryString);
        BooleanExpression be = irs.createNewBooleanExpression().parseQuery(inputUnparsedQueryString);
        return TestQueries.evaluatePrintAndGetResultsOf(be);
    }

    @BeforeEach
    void printSpaceBeforeQuery() {
        TestQueries.printSpaceBeforeQuery_();
    }

    @Test
    void illustrativeExample() {
        String queryString = "space & jam";
//        List<Document> results = irs.retrieve(queryString);   // can be used to obtain results directly
        evaluatePrintAndGetResultsOf(queryString);
    }

    @Test
    void illustrativeExample2() {
        String queryString = "space & !jam";
        evaluatePrintAndGetResultsOf(queryString);
    }

    @Test
    void singleWordQuery() {
        assertTrue(evaluatePrintAndGetResultsOf(wordsContainedInFirstButNotInSecondDocumentSupplier.get())
                .contains(doc1));
    }

    @Test
    void AND_query_containedInBoth() {
        String queryString = wordsContainedBothInFirstAndSecondDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query2containedIn1stButNotIn2nd() {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query_containedIn2ndButNotIn1st() {
        String queryString = wordsContainedInSecondButNotInFirstDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void AND_query_containedNeitherIn1stNorIn2nd() {
        String queryString = wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get()
                + "&" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void AND_query2_containedNeitherIn1stNorIn2nd() {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "&" + wordsContainedInSecondButNotInFirstDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void OR_query_containedInBoth() {
        String queryString = wordsContainedBothInFirstAndSecondDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2containedIn1stButNotIn2nd() {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedIn2ndButNotIn1st() {
        String queryString = wordsContainedInSecondButNotInFirstDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query_containedNeitherIn1stNorIn2nd() {
        String queryString = wordsContainedNeitherInFirstNorInSecondDocumentSupplier.get()
                + "|" + wordsContainedBothInFirstAndSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void OR_query2_containedNeitherIn1stNorIn2nd() {
        String queryString = wordsContainedInFirstButNotInSecondDocumentSupplier.get()
                + "|" + wordsContainedInSecondButNotInFirstDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query() {
        String queryString = "!" + wordsContainedInFirstButNotInSecondDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertFalse(results.contains(doc1));
        assertTrue(results.contains(doc2));
    }

    @Test
    void NOT_query2() {
        String queryString = "!" + wordsContainedInSecondButNotInFirstDocumentSupplier.get();
        var results = evaluatePrintAndGetResultsOf(queryString);
        assertTrue(results.contains(doc1));
        assertFalse(results.contains(doc2));
    }

    @Test
    void phraseQuery() {
        String queryString = "\"Space jam\"";
        var results = evaluatePrintAndGetResultsOf(queryString);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery() {
        String queryString = "Space *am";
        var results = evaluatePrintAndGetResultsOf(queryString);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void wildcardQuery2() {
        String queryString = "Sp*ce *am";
        var results = evaluatePrintAndGetResultsOf(queryString);
        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        assertTrue(results.contains(doc1));
    }

    @Test
    void spellingCorrection() {
        String queryString = "Spade jam";
        var be = irs.createNewBooleanExpression()
                .parseQuery(queryString).spellingCorrection(false, true);
        assert 1 == be.getEditDistanceForSpellingCorrection();

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        var results = TestQueries.evaluatePrintAndGetResultsOf(be);
        assertTrue(results.contains(doc1));
    }

    @Test
    void phoneticCorrection() {
        String queryString = "Space jem";
        var be = irs.createNewBooleanExpression()
                .parseQuery(queryString).spellingCorrection(true, true);

        // assert that the correction is present
        assert be.getQueryString().toLowerCase().contains("space");
        assert be.getQueryString().toLowerCase().contains("jam");

        assert doc1.getTitle() != null && doc1.getTitle().equals("Space Jam");
        var results = TestQueries.evaluatePrintAndGetResultsOf(be);
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

        private long counter = 0;

        public WordSupplier(List<String> words) {
            this.words = Objects.requireNonNull(words).stream()
                    .map(word -> Utility.normalize(word, false, Language.UNDEFINED))
                    .filter(Objects::nonNull).toList();
        }

        public List<String> getAllWords() {
            return words;
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
