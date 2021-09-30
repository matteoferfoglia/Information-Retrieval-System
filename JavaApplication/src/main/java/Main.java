import util.Properties;

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
    }
}