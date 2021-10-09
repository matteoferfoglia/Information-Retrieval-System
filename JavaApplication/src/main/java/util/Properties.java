package util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Objects;

/**
 * Utility class for working with application properties.
 * The properties can be accessed from the static attribute
 * {@link #appProperties} of this class.
 *
 * @author Matteo Ferfoglia
 */
public class Properties {

    /**
     * Name of the file with the properties for the application.
     * The file must be placed in the resource folder ('resources').
     */
    @NotNull
    private static final String defaultPropertyFileName = "properties.default.env";


    /**
     * Name of the file with the user-specific properties for the application,
     * to be accessed with static getter/setter.
     * The user may have changed default properties (if the application allows).
     * The file must be placed in the resource folder ('resources').
     */
    private static class ApplicationPropertyFilePath {
        /**
         * The path.
         */
        @NotNull
        private volatile static String applicationPropertyFilePath = "properties.userSpecific.env";

        /**
         * Get the {@link ApplicationPropertyFilePath}.
         */
        @NotNull
        public static String get() {
            return applicationPropertyFilePath;
        }

        /**
         * Set the {@link ApplicationPropertyFilePath}.
         */
        public static void set(@NotNull String applicationPropertyFilePath) {
            ApplicationPropertyFilePath.applicationPropertyFilePath = Objects.requireNonNull(applicationPropertyFilePath);
        }
    }

    /**
     * The {@link ClassLoader} instance to use to load resources.
     */
    @NotNull
    private static final ClassLoader classLoader = Properties.class.getClassLoader();
    /**
     * The properties to use in the application.
     * This attribute consists in a new instance of {@link java.util.Properties},
     * until the method {@link #loadProperties()} is not invoked.
     */
    @NotNull
    public volatile static java.util.Properties appProperties = new java.util.Properties();
    /**
     * Saves a flag: it is true after the {@link #loadProperties()} method
     * was invoked for the first time and keeps the "true" value till the
     * end of the program.
     */
    private volatile static boolean wasLoadPropertiesInvoked = false;

    /**
     * Path of the working directory, saved as class (to allow updates via setter).
     */
    private static class WorkingDirectoryPath {
        /**
         * The path.
         */
        @NotNull
        private volatile static String workingDirectoryPath = "";

        /**
         * The path of {@link ApplicationPropertyFilePath} before {@link #set(String)} was invoked.
         */
        @NotNull
        private volatile static String oldApplicationPropertyFilePath = "";

        /**
         * Get the {@link WorkingDirectoryPath}.
         */
        @NotNull
        public static String get() {
            return workingDirectoryPath;
        }

        /**
         * Set the {@link WorkingDirectoryPath}.
         */
        public static void set(@NotNull String workingDirectoryPath) {
            oldApplicationPropertyFilePath = ApplicationPropertyFilePath.get();
            workingDirectoryPath = Objects.requireNonNull(workingDirectoryPath);
            ApplicationPropertyFilePath.set(workingDirectoryPath + '/' + oldApplicationPropertyFilePath);
        }
    }

    /**
     * Load application properties and create the working directory for the application.
     * First, default properties are loaded from the file named as specified in
     * {@link #defaultPropertyFileName}, then, user-specific properties will be
     * loaded from the file named as specified in {@link ApplicationPropertyFilePath},
     * if the file is found. The latest properties are used, but, if they are
     * not available, default properties are used.
     *
     * @throws IOException If I/O Exceptions are thrown when creating the working directory.
     */
    public static void loadProperties() throws IOException {

        wasLoadPropertiesInvoked = true;    // saves that this method was invoked

        // create and load default prop
        java.util.Properties defaultProps = new java.util.Properties();
        try (InputStream in = getInputResource(defaultPropertyFileName)) {
            defaultProps.load(in);
        } catch (IOException e) {
            System.err.println("Impossible to load default properties for the application.");
            e.printStackTrace();
            return;
        }

        // create and load user-specific application properties, if different from defaultProps
        File file_workingDirectory = new File(
                Objects.requireNonNull(
                        defaultProps.getProperty("workingDirectory_name"),
                        "The name of the working directory to create cannot be null"
                )
        );
        if (!file_workingDirectory.isDirectory()) {
            if ((file_workingDirectory.exists() && !file_workingDirectory.delete()) || !file_workingDirectory.mkdir()) {
                // If the folder exists but cannot be deleted or if it cannot be created
                throw new IOException("Unable to create the directory " + file_workingDirectory.getAbsolutePath());
            } else {
                System.out.println("Working directory \"" + file_workingDirectory.getAbsolutePath() + "\" created.");
            }
        }
        WorkingDirectoryPath.set(file_workingDirectory.getAbsolutePath());

        appProperties = new java.util.Properties(defaultProps);
        File userSpecificPropertyFile = new File(ApplicationPropertyFilePath.get());
        try {
            if (!userSpecificPropertyFile.exists()) {
                if (!userSpecificPropertyFile.createNewFile()) {
                    // Fail during file creation
                    throw new IOException("Unable to create the file.");
                }
            }
        } catch (IOException e) {
            System.err.println("User-specific properties will not be saved neither used.");
        }
        try (FileInputStream in = new FileInputStream(userSpecificPropertyFile)) {
            defaultProps.load(in);
        } catch (IOException e) {
            System.err.println("Impossible to load user-specific properties for the application. " +
                    "Default properties will be used.");
            e.printStackTrace();
        }
    }

    /**
     * Save current user-specific properties, assuming they have changed.
     *
     * @param comment The comment associated with the save ("log").
     * @throws IOException If I/O Exceptions are thrown when creating the working directory.
     */
    public static void saveCurrentProperties(@NotNull String comment) throws IOException {

        if (!wasLoadPropertiesInvoked) {
            loadProperties();
        }

        try (FileOutputStream out = new FileOutputStream(ApplicationPropertyFilePath.get())) {
            // TODO : to save in working directory
            appProperties.store(out, Objects.requireNonNull(comment));
        } catch (IOException | NullPointerException e) {
            System.err.println("Impossible to save.");
            e.printStackTrace();
        }

    }

    /**
     * Load a resource.
     *
     * @param resourceName The name of the resource to be loaded.
     * @return the desired resource or null if it is not found.
     */
    @Nullable
    private static InputStream getInputResource(@NotNull String resourceName) {
        return classLoader.getResourceAsStream(resourceName);
    }

}