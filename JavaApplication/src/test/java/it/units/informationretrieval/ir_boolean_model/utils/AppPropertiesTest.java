package it.units.informationretrieval.ir_boolean_model.utils;

import it.units.informationretrieval.ir_boolean_model.utils.functional.SupplierThrowingException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AppPropertiesTest {

    private static final String SAMPLE_PROP_VALUE = "Bar";
    private static final String SAMPLE_PROP_NAME = "Foo";
    private static final File fakeFile = new File("fakeFile");
    private static AppProperties appProperties;

    @BeforeEach
    void setInstance() throws IOException, NoSuchFieldException, IllegalAccessException {
        try {
            Constructor<?> ctor = AppProperties.class.getDeclaredConstructor();
            ctor.setAccessible(true);
            appProperties = (AppProperties) ctor.newInstance(); // reflection needed to force the singleton to create a new instance
        } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            fail(e);
        }
        setFakeUserSpecificPropertiesFile();
    }

    private void setFakeUserSpecificPropertiesFile()
            throws IllegalAccessException, NoSuchFieldException, IOException {
        if (!fakeFile.createNewFile()) {
            throw new IOException("Error creating fake file.");
        }
        Field userSpecificPropertiesFileField = appProperties.getClass()
                .getDeclaredField("userSpecificPropertyFile");
        userSpecificPropertiesFileField.setAccessible(true);
        userSpecificPropertiesFileField.set(appProperties, fakeFile);
    }

    @AfterEach
    void removeFakeUserSpecificPropertiesFile() throws IOException {
        if (fakeFile.exists() && !fakeFile.delete()) {
            throw new IOException("Error when deleting the fake file.");
        }
    }

    @Test
    void getInstance() {
        try {
            appProperties = AppProperties.getInstance();
            assertNotNull(appProperties);
        } catch (IOException e) {
            fail(e);
        }
    }

    @Test
    void assertThatSavedPropertiesAreAvailableNextTimePropertiesAreLoaded()
            throws IOException, NoSuchFieldException, IllegalAccessException {
        appProperties = AppProperties.getInstance();
        removeFakeUserSpecificPropertiesFile();
        setFakeUserSpecificPropertiesFile();
        assert !appProperties.contains(SAMPLE_PROP_NAME);
        int sizeBeforeUpdating = appProperties.size();
        appProperties.set(SAMPLE_PROP_NAME, SAMPLE_PROP_VALUE);
        appProperties.save("Added property");
        appProperties = AppProperties.getInstance();
        assertEquals(sizeBeforeUpdating + 1, appProperties.size());
        assertTrue(appProperties.contains(SAMPLE_PROP_NAME));
        assertEquals(SAMPLE_PROP_VALUE, appProperties.get(SAMPLE_PROP_NAME));
    }

    @Test
    void testContainMethodWithAPropertyThatDoesNotExist() {
        assertNull(appProperties.get("prop name that does not exist for sure"));
    }

    @Test
    void testGetterAndSetter() {
        assert !appProperties.contains(SAMPLE_PROP_NAME);
        appProperties.set(SAMPLE_PROP_NAME, SAMPLE_PROP_VALUE);
        assertEquals(SAMPLE_PROP_VALUE, appProperties.get(SAMPLE_PROP_NAME));
    }

    @Test
    void assertThatDefaultPropertiesAreNeverOverwritten() throws IOException {

        Function<AppProperties, Map<?, ?>> getMapFromProperties = appProperties -> appProperties.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        SupplierThrowingException<Map<?, ?>, IOException> removeUserSpecificAndGetDefaultPropertiesAsMapSupplier = () -> {
            removeFakeUserSpecificPropertiesFile();
            try {
                setInstance();
                return getMapFromProperties.apply(appProperties); // reflection needed to force the singleton to create a new instance
            } catch (IllegalAccessException | NoSuchFieldException e) {
                fail(e);
                return null;
            }
        };

        Map<?, ?> defaultProperties = removeUserSpecificAndGetDefaultPropertiesAsMapSupplier.get();

        appProperties.set(SAMPLE_PROP_NAME, SAMPLE_PROP_VALUE);
        assert !defaultProperties.containsKey(SAMPLE_PROP_NAME);

        appProperties.save("Update default properties");
        Map<?, ?> savedAppProperties = getMapFromProperties.apply(appProperties);

        assertEquals(SAMPLE_PROP_VALUE, appProperties.get(SAMPLE_PROP_NAME));
        assertEquals(defaultProperties, removeUserSpecificAndGetDefaultPropertiesAsMapSupplier.get());
        assertNotEquals(savedAppProperties, removeUserSpecificAndGetDefaultPropertiesAsMapSupplier.get());

    }

    @Test
    void testToStringToShowCorrectNumberOfProperties() {
        final int NUMBER_OF_HEADING_LINES = 1;
        assertEquals(
                NUMBER_OF_HEADING_LINES + appProperties.size(),
                appProperties.toString().split(System.lineSeparator()).length);
    }
}