import components.Corpus;
import components.Posting;
import documentDescriptors.Movie;
import util.Properties;
import util.Utility;

import java.io.*;
import java.net.URISyntaxException;

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
            File   file_irSystem     = new File(fileName_irSystem);

            // Load the IR System if already exists
            InformationRetrievalSystem ir;

            //noinspection SwitchStatementWithTooFewBranches
            switch( file_irSystem.isFile() ? 1 : 0 ) {
                case 1 :    // file exists
                    System.out.println("Loading the IRSystem from file system");
                    ObjectInputStream ois = new ObjectInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(file_irSystem)
                            )
                    );
                    Object irSystem_object = ois.readObject();
                    if( irSystem_object instanceof InformationRetrievalSystem ) {
                        ir = (InformationRetrievalSystem) irSystem_object;
                        System.out.println("IRSystem loaded from file system.");
                        break;
                    }   // else create the IRSystem

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
            if( file_irSystem.createNewFile() ) {    // if file already exists will do nothing
                System.out.println("File \"" + fileName_irSystem + "\" created.");
            } else {
                System.out.println("File \"" + fileName_irSystem + "\" already exists. Will be replaced.");
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

            System.out.println();
        } catch (Posting.DocumentIdentifier.NoMoreDocIdsAvailable | URISyntaxException | IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }
}