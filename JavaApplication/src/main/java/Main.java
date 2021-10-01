import components.Corpus;
import components.Posting;
import documentDescriptors.Movie;
import util.Properties;

import java.net.URISyntaxException;

/**
 * @author Matteo Ferfoglia
 */
public class Main {

    public static void main(String[] args) {

        Properties.loadProperties();
        /* // Examples with Properties
        Properties.appProperties.list(System.out);
        Properties.appProperties.setProperty("testProp", "0");
        Properties.appProperties.list(System.out);
        Properties.appProperties.setProperty("testProp", "1");
        Properties.appProperties.list(System.out);
        Properties.saveCurrentProperties("Changed testProp");
        */

        try {
            Corpus corpus = Movie.createCorpus();
            System.out.println(corpus.head(3));

            InformationRetrievalSystem ir = new InformationRetrievalSystem(corpus);
            System.out.println();
        } catch (Posting.DocumentIdentifier.NoMoreDocIdsAvailable | URISyntaxException e) {
            e.printStackTrace();
        }

        System.exit(0); // terminate the JVM, normal termination
    }
}