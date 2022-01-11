package it.units.informationretrieval.ir_boolean_model.utils.stemmers;

import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.org.tartarus.snowball.SnowballProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * LICENSE: this class makes use of {@link SnowballProgram} and derived classes.
 * See the license in corresponding package.
 */
class SnowballStemmer implements Stemmer {

    /**
     * Caches stemmers of different language to avoid to re-instantiate them each
     * time that they are required.
     */
    private static final ConcurrentMap<Language, SnowballStemmer> stemmersByLanguage =
            new ConcurrentHashMap<>(Language.values().length);

    /**
     * The actual stemmer or null if not initialized.
     */
    @Nullable
    private SnowballProgram snowballStemmer = null;

    /**
     * Flag set to true if no stemmer is available for the specified language
     * (see {@link #initStemmer(Language)}).
     */
    private boolean noStemmerForTheLanguage = false;

    /**
     * Test method.
     */
    public static void main(String[] args) {
        Stemmer snowballStemmer = new SnowballStemmer();
        String input = """
                I come no more to make you laugh: things now,
                That bear a weighty and a serious brow,
                Sad, high, and working, full of state and woe,
                Such noble scenes as draw the eye to flow,
                We now present. Those that can pity, here
                May, if they think it well, let fall a tear;
                The subject will deserve it. Such as give
                Their money out of hope they may believe,
                May here find truth too. Those that come to see
                Only a show or two, and so agree
                The play may pass, if they be still and willing,
                I'll undertake may see away their shilling
                Richly in two short hours. Only they
                That come to hear a merry bawdy play,
                A noise of targets, or to see a fellow
                In a long motley coat guarded with yellow,
                Will be deceived; for, gentle hearers, know,
                To rank our chosen truth with such a show
                As fool and fight is, beside forfeiting
                Our own brains, and the opinion that we bring,
                To make that only true we now intend,
                Will leave us never an understanding friend.""";
        for (String word : Utility.split(input)) {
            String stemmed = snowballStemmer.stem(word, Language.ENGLISH);
            System.out.print(stemmed + " ");
        }
    }

    /**
     * Sets the stemmer for the given {@link Language}, according to the desired language.
     *
     * @param language The language for the stemmer.
     */
    private void setStemmer(@NotNull Language language) {
        var stemmerIfPresentOrNull = stemmersByLanguage.get(language);
        if (stemmerIfPresentOrNull == null) {
            stemmerIfPresentOrNull = new SnowballStemmer();
            try {
                stemmerIfPresentOrNull.initStemmer(language);
            } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                System.err.println("No stemmer available for language " + language);
                stemmerIfPresentOrNull.noStemmerForTheLanguage = true;
            }
            stemmersByLanguage.put(language, stemmerIfPresentOrNull);
        }
        assert stemmerIfPresentOrNull.snowballStemmer != null;    // must have been initialized if it was initially null
        this.snowballStemmer = stemmerIfPresentOrNull.snowballStemmer;
        this.noStemmerForTheLanguage = stemmerIfPresentOrNull.noStemmerForTheLanguage;
    }

    /**
     * Initialize the stemmer.
     *
     * @param language The language for the stemmer.
     */
    private void initStemmer(@NotNull Language language)
            throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        String languageName = language.name().toLowerCase();
        languageName = languageName.substring(0, 1).toUpperCase() + languageName.substring(1);
        Class<?> stemClass = Class.forName(
                getClass().getPackageName() + ".org.tartarus.snowball.ext." + languageName + "Stemmer");
        Constructor<?> ctor = stemClass.getConstructor();
        snowballStemmer = (SnowballProgram) ctor.newInstance();
    }

    @Override
    public String stem(@NotNull String input, @NotNull Language language) {
        if (!language.equals(Language.UNDEFINED)) {
            setStemmer(language);
            if (noStemmerForTheLanguage) {
                return input;
            } else {
                assert snowballStemmer != null;
                String stemmed = input;
                PrintStream err = System.err;
                try {
                    System.setErr(new PrintStream(new ByteArrayOutputStream()));
                    snowballStemmer.setCurrent(input);
                    snowballStemmer.stem();
                    stemmed = snowballStemmer.getCurrent();
                } catch (Exception ignored) {
                } finally {
                    System.setErr(err);
                }
                return stemmed;
            }
        } else {
            return input;   // no stemming due to undefined language
        }
    }
}
