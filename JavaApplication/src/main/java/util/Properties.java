package util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Utility class for working with application properties.
 * The properties can be accessed from the static attribute
 * {@link #appProperties} of this class.
 *
 * @author Matteo Ferfoglia
 */
public class Properties {

    /** Name of the file with the properties for the application.
     * The file must be placed in the resource folder ('resources'). */
    @NotNull
    public static final String defaultPropertyFileName = "properties.default.env";

    /** Name of the file with the user-specific properties for the application.
     * The user may have changed default properties (if the application allows).
     * The file must be placed in the resource folder ('resources'). */
    @NotNull
    public static final String applicationPropertyFileName = "properties.userSpecific.env";

    /** The properties to use in the application.
     * This attribute consists in a new instance of {@link java.util.Properties},
     * until the method {@link #loadProperties()} is not invoked.*/
    @NotNull
    public volatile static java.util.Properties appProperties = new java.util.Properties();

    /** Saves a flag: it is true after the {@link #loadProperties()} method
     * was invoked for the first time and keeps the "true" value till the
     * end of the program.*/
    private volatile static boolean wasLoadPropertiesInvoked = false;

    /** The {@link ClassLoader} instance to use to load resources. */
    @NotNull
    private static final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();


    /** Load application properties.
     * First, default properties are loaded from the file named as specified in
     * {@link #defaultPropertyFileName}, then, user-specific properties will be
     * loaded from the file named as specified in {@link #applicationPropertyFileName},
     * if the file is found. The latest properties are used, but, if they are
     * not available, default properties are used.*/
    public static void loadProperties() {

        wasLoadPropertiesInvoked = true;    // saves that this method was invoked

        // create and load default prop
        java.util.Properties defaultProps = new java.util.Properties();
        try (InputStream in = getInputResource(defaultPropertyFileName) ) {
            defaultProps.load(in);
        } catch (IOException e) {
            System.err.println("Impossible to load default properties for the application.");
            e.printStackTrace();
            return;
        }

        // create and load user-specific application properties, if different from defaultProps
        appProperties = new java.util.Properties( defaultProps );
        try (InputStream in = getInputResource(applicationPropertyFileName) ) {
            if( in!=null ) {
                defaultProps.load(in);
            }
        } catch (IOException e) {
            System.err.println("Impossible to load user-specific properties for the application. " +
                               "Default properties will be used.");
            e.printStackTrace();
        }
    }

    /** Save current user-specific properties, assuming they have changed.
     * @param comment The comment associated with the save ("log"). */
    public static void saveCurrentProperties(@NotNull String comment) {

        if( !wasLoadPropertiesInvoked) {
            loadProperties();
        }

        //noinspection ConstantConditions // NullPointerException is possible when loading the resource, but it is catched
        try (FileOutputStream out = new FileOutputStream(classLoader.getResource(applicationPropertyFileName).getFile()) ) {
            appProperties.store(out, Objects.requireNonNull(comment));
        } catch (IOException | NullPointerException e) {
            System.err.println("Impossible to save.");
            e.printStackTrace();
        }

    }

    /** Load a resource.
     * @param resourceName The name of the resource to be loaded.
     * @return the desired resource or null if it is not found.*/
    @Nullable
    private static InputStream getInputResource(@NotNull String resourceName) {
        return classLoader.getResourceAsStream(resourceName);
    }

}