package it.units.informationretrieval.ir_boolean_model.utils.stemmers;

import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.org.tartarus.snowball.SnowballProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * LICENSE: this class makes use of {@link SnowballProgram} and derived classes
 * under the <a href="https://www.apache.org/licenses/LICENSE-2.0.txt">Apache 2 license</a>,
 * as stated at the <a href="https://mvnrepository.com/artifact/org.apache.lucene/lucene-snowball/3.0.3">source</a>
 * where the repository was taken.
 */
class SnowballStemmer implements Stemmer {

    /**
     * The stemmer or null if not initialized.
     */
    @Nullable
    private SnowballProgram snowballStemmer = null;

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
        for (String word : input.split(" ")) {
            String stemmed = snowballStemmer.stem(word, Language.ENGLISH);
            System.out.print(stemmed + " ");
        }
    }

    private void setStemmer(@NotNull Language language)
            throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, InstantiationException, IllegalAccessException {
        String languageName = language.name().toLowerCase();
        languageName = languageName.substring(0, 1).toUpperCase() + languageName.substring(1);
        Class<?> stemClass = Class.forName("org.tartarus.snowball.ext." + languageName + "Stemmer");
        Constructor<?> ctor = stemClass.getConstructor();
        snowballStemmer = (SnowballProgram) ctor.newInstance();
    }

    @Override
    public String stem(@NotNull String input, @NotNull Language language) {
        try {
            setStemmer(language);
            assert snowballStemmer != null;
            snowballStemmer.setCurrent(input);
            snowballStemmer.stem();
            return snowballStemmer.getCurrent();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                InstantiationException | IllegalAccessException e) {
            System.err.println("Error during stemming. No stemming will be performed.");
            e.printStackTrace();
            return input;
        }
    }
}
