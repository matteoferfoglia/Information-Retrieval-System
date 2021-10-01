import components.Corpus;
import components.Posting;
import documentDescriptors.Movie;
import util.Properties;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * @author Matteo Ferfoglia
 */
public class Main {

    public static void main(String[] args) {
        Properties.loadProperties();
        Properties.appProperties.list(System.out);
        Properties.appProperties.setProperty("testProp", "1");
        Properties.appProperties.list(System.out);
        Properties.saveCurrentProperties("Changed testProp");

        try {
            Corpus corpus = Movie.createCorpus();
            System.out.println(corpus.head(3));
        } catch (Posting.DocumentIdentifier.NoMoreDocIdsAvailable | URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
}