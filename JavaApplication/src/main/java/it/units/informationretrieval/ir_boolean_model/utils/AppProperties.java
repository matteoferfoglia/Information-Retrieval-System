package it.units.informationretrieval.ir_boolean_model.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Utility class for working with application properties.
 * The properties can be accessed from the static attribute
 * {@link #appProperties} of this class.
 *
 * @author Matteo Ferfoglia
 */
public class AppProperties {

    //region final const parameters

    /**
     * Name of the file with the properties for the application.
     * The file must be placed in the resource folder ('resources').
     */
    @NotNull
    private static final String DEFAULT_PROPERTIES_FILE_NAME = "properties.default.env";

    /**
     * Name of the file in the working directory containing the user-specific properties.
     */
    @NotNull
    private static final String USER_SPECIFIC_PROPERTIES_FILE_NAME = "properties.userSpecific.env";

    /**
     * The {@link ClassLoader} instance to use to load resources.
     */
    @NotNull
    private static final ClassLoader CLASS_LOADER = AppProperties.class.getClassLoader();

    /**
     * Name of the property (as written in {@link #DEFAULT_PROPERTIES_FILE_NAME})
     * containing the working directory name.
     */
    @NotNull
    private static final String WORKING_DIRECTORY_PROPERTY_NAME = "workingDirectory_name";

    /**
     * {@link Logger} of this class.
     */
    @NotNull
    private static final Logger LOGGER_THIS_CLASS = Logger.getLogger(AppProperties.class.getCanonicalName());

    //endregion

    /**
     * {@link SupplierThrowingException} which provide the single instance of this class.
     * or throws an {@link IOException} if I/O errors occurs while loading properties.
     */
    @NotNull
    private static final SupplierThrowingException<AppProperties, IOException> singleInstanceSupplier =
            new SupplierThrowingException<>() {

                /** The cached instance. */
                private static AppProperties appProperties;   // "closure" to cache the instance instead of reloading

                /** The eventually thrown exception if I/O errors occur when loading properties. */
                private static IOException eventuallyThrownException;

                static {
                    try {
                        appProperties = new AppProperties();
                        eventuallyThrownException = null;
                    } catch (IOException e) {
                        appProperties = null;
                        eventuallyThrownException = e;
                    }
                }

                @Override
                public AppProperties get() throws IOException {
                    if (appProperties == null) {
                        throw eventuallyThrownException;
                    } else {
                        return appProperties;
                    }
                }
            };

    /**
     * The properties to use in the application.
     * This attribute consists in a new instance of {@link java.util.Properties},
     * until the method {@link #loadProperties()} is not invoked.
     */
    @NotNull
    private java.util.Properties appProperties = new java.util.Properties();

    /**
     * File containing the user-specific properties.
     */
    @Nullable
    private File userSpecificPropertyFile;

    /**
     * Constructor.
     *
     * @throws IOException If I/O errors occur while loading properties.
     */
    private AppProperties() throws IOException {
        loadProperties();
    }

    /**
     * @return the singleton instance of this class.
     * @throws IOException if I/O errors occur when loading properties.
     */
    public static AppProperties getInstance() throws IOException {
        return singleInstanceSupplier.get();
    }

    /**
     * Creates (if it does not already exist) the working directory.
     *
     * @param workingDirectory The {@link File} instance representing
     *                         the working directory to be created.
     * @throws IOException If I/O errors occur.
     */
    private static void createWorkingDirectoryOnFileSystemIfNotExist(@NotNull final File workingDirectory)
            throws IOException {
        String workingDirectoryAbsolutePath = workingDirectory.getAbsolutePath();
        if (!workingDirectory.exists()) {
            if (!workingDirectory.mkdir()) {
                throw new IOException("Directory " + workingDirectoryAbsolutePath + " cannot be created.");
            }
        } else if (!workingDirectory.isDirectory()) {
            throw new IOException("The file " + workingDirectoryAbsolutePath + " already exists but it is not a directory. " +
                    "Please, remove it to continue.");
        } // otherwise, the directory already exists

    }

    /**
     * @return the default {@link java.util.Properties}.
     * @throws IOException If I/O errors occur
     */
    @NotNull
    private static java.util.Properties getDefaultProperties() throws IOException {
        java.util.Properties defaultProps = new java.util.Properties();
        try (InputStream in = getInputResource(DEFAULT_PROPERTIES_FILE_NAME)) {
            defaultProps.load(in);
        }
        return defaultProps;
    }

    /**
     * @param userSpecificPropertyFile The {@link File} instance containing the user-specific properties.
     * @return the user-specific {@link java.util.Properties}.
     * @throws IOException If the file containing the user-specific properties cannot be created on the file system.
     */
    @NotNull
    private static java.util.Properties getUserSpecificProperties(@NotNull final File userSpecificPropertyFile)
            throws IOException {
        if (!userSpecificPropertyFile.exists() && !userSpecificPropertyFile.createNewFile()) {
            // Fail during file creation
            throw new IOException(
                    "Unable to create the file " + userSpecificPropertyFile.getAbsolutePath() + ".");
        }
        java.util.Properties userSpecificProperties = new java.util.Properties();
        userSpecificProperties.load(new FileInputStream(userSpecificPropertyFile));
        return userSpecificProperties;
    }

    /**
     * Load a resource.
     *
     * @param resourceName The name of the resource to be loaded.
     * @return the desired resource or null if it is not found.
     */
    @Nullable
    private static InputStream getInputResource(@NotNull String resourceName) {
        return CLASS_LOADER.getResourceAsStream(resourceName);
    }

    /**
     * Load application properties and create the working directory for the application.
     * First, default properties are loaded from the file named as specified in
     * {@link #DEFAULT_PROPERTIES_FILE_NAME}, then, user-specific properties will be
     * loaded from the file named as specified in {@link #USER_SPECIFIC_PROPERTIES_FILE_NAME},
     * if the file is found. The latest properties are used, but, if they are
     * not available, default properties are used.
     *
     * @throws IOException If I/O Exceptions are thrown when loading properties or
     *                     creating the working directory.
     */
    private synchronized void loadProperties() throws IOException {

        java.util.Properties defaultProps = getDefaultProperties();
        appProperties = new java.util.Properties();
        appProperties.putAll(defaultProps);

        File workingDirectory = new File(Objects.requireNonNull(
                defaultProps.getProperty(WORKING_DIRECTORY_PROPERTY_NAME)));
        createWorkingDirectoryOnFileSystemIfNotExist(workingDirectory);

        userSpecificPropertyFile = new File(
                workingDirectory.getAbsolutePath() + File.separator + USER_SPECIFIC_PROPERTIES_FILE_NAME);
        try {
            appProperties.putAll(getUserSpecificProperties(userSpecificPropertyFile));
        } catch (IOException e) {
            LOGGER_THIS_CLASS.log(
                    Level.SEVERE, "User-specific properties will not be saved neither used due to an exception. ", e);
        }

    }

    /**
     * Save current user-specific properties, assuming they have changed.
     * AppProperties are saved to file {@link #userSpecificPropertyFile},
     * assuming it exists.
     *
     * @param comment The comment associated with the save ("log").
     * @throws IOException If I/O Exceptions are thrown when saving properties.
     */
    public synchronized void save(@NotNull String comment) throws IOException {
        try (FileOutputStream out = new FileOutputStream(Objects.requireNonNull(userSpecificPropertyFile))) {
            appProperties.store(out, Objects.requireNonNull(comment));
        }
    }

    /**
     * Gets a property by its name.
     *
     * @param propertyName the name of the property to get.
     * @return the property value or null if the property is not found.
     */
    @Nullable
    public synchronized String get(@NotNull final String propertyName) {
        return appProperties.getProperty(Objects.requireNonNull(propertyName));
    }

    /**
     * Sets a property by its name.
     * If the property already existed, it is overwritten, otherwise it is created.
     * Default properties are never overwritten: this setting will influence
     * user-specific properties only.
     *
     * @param propertyName the name of the property to get.
     * @return the <strong>old</strong> value for the property or null if it was not set.
     */
    @Nullable
    public synchronized String set(@NotNull final String propertyName, @NotNull final String newPropertyValue) {
        return (String) appProperties.setProperty(
                Objects.requireNonNull(propertyName), Objects.requireNonNull(newPropertyValue));
    }

    /**
     * @return the number of properties.
     */
    public int size() {
        return appProperties.size();
    }

    /**
     * @param propertyName The property name for which the user wants to know if is present.
     * @return true if the given property (by name) is present, false otherwise.
     */
    public boolean contains(@NotNull final String propertyName) {
        return appProperties.containsKey(propertyName);
    }

    /**
     * @return the entry set with all the properties.
     */
    public Set<Map.Entry<Object, Object>> entrySet() {
        return appProperties.entrySet();
    }

    @Override
    public synchronized String toString() {
        return "App Properties (" + appProperties.size() + " props):" + System.lineSeparator()
                + appProperties.entrySet().stream()
                .map(propertyNameAndValue ->
                        "\t" + propertyNameAndValue.getKey() + ": " + propertyNameAndValue.getValue())
                .sorted()
                .collect(Collectors.joining(System.lineSeparator()))
                + System.lineSeparator();
    }
}