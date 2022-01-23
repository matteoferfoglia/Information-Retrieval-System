package it.units.informationretrieval.ir_boolean_model.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility class about class loading.
 *
 * @author Matteo Ferfoglia
 */
public class ClassLoading {
    /**
     * @return the {@link List} of all classes found in this project.
     */
    public static List<Class<?>> getAllClasses() throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        for (File directory : Objects.requireNonNull(getRootDirectoryOfProject().listFiles())) {
            classes.addAll(findClasses(directory));
        }
        return classes;
    }

    /**
     * @return the root directory of the current project.
     */
    private static File getRootDirectoryOfProject() {
        return new File(System.getProperty("user.dir"));
    }

    /**
     * @return the {@link List} of classes found at the given directory
     * and in its subdirectories.
     */
    private static List<Class<?>> findClasses(File directory) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        final String rootOfProjectDirectoryOfClasses = getRootDirectoryOfProject().getCanonicalPath();
        String classExtension = ".class";   // compiled classes
        if (directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                classes.addAll(findClasses(file));
            }
        } else {
            @SuppressWarnings("UnnecessaryLocalVariable") File file = directory;  // it is a file and not a directory
            if (file.getName().endsWith(classExtension) && !file.getName().startsWith("module-info")) {
                try {
                    final String ESCAPED_FILE_SEPARATOR = "\\" + File.separator;    // correctly escaped
                    classes.add(
                            Class.forName(
                                    file.getCanonicalPath()
                                            .substring(rootOfProjectDirectoryOfClasses.length() + 1,
                                                    file.getCanonicalPath().length() - classExtension.length())
                                            .replace("target" + File.separator + "classes" + File.separator, "")        // remove folder names till the project classes
                                            .replaceAll(ESCAPED_FILE_SEPARATOR, ".")
                            ));
                } catch (ClassNotFoundException ignored) {
                    // test-class are found too as files, but they are not accessible from production code
                    // and this results in ClassNotFoundException; furthermore, all non-class files are found too
                }
            }
        }
        return classes;
    }
}
