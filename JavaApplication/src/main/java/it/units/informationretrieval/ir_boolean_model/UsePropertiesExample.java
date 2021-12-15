package it.units.informationretrieval.ir_boolean_model;

import it.units.informationretrieval.ir_boolean_model.utils.AppProperties;

/**
 * Class showing examples for using {@link AppProperties}.
 *
 * @author Matteo Ferfoglia.
 */
public class UsePropertiesExample { // TODO: delete this class
    public static void main(String[] args) {
        try {
            // Load application properties and create working directory
            AppProperties appProperties = AppProperties.getInstance();

            // Examples with AppProperties
            System.out.println(appProperties);
            appProperties.set("testProp", "0");
            System.out.println(appProperties);
            appProperties.set("testProp", "1");
            System.out.println(appProperties);
            appProperties.save("Changed testProp");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
