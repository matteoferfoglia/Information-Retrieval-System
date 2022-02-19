package it.units.informationretrieval.ir_boolean_model.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

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

        // Search classes from the root directory
        for (File directory : Objects.requireNonNull(getRootDirectoryOfProject().listFiles())) {
            classes.addAll(findClasses(directory));
        }

        // Search classes from the JAR file (if the program is executed from JAR)
        classes.addAll(findClassesFromCurrentJar());

        return classes;
    }

    /**
     * Searches and return all classes present in the "current" JAR file, where
     * "current" means that a JAR file for this application was created and the
     * program has been launched from the jar (i.e., with
     * <code>java -jar programName.jar argsToMain</code>
     * ).
     *
     * @return the {@link Collection} of classes in this JAR file
     */
    private static Collection<Class<?>> findClassesFromCurrentJar() {
        try {
            String folderNameWhereToSearchForJarFiles = ".";    // current directory
            return Stream.of(Objects.requireNonNull(new File(folderNameWhereToSearchForJarFiles).listFiles()))
                    .map(File::getAbsolutePath)
                    .filter(fileName -> fileName.endsWith(".jar"))
                    .flatMap(jarFileName -> {
                        try {
                            return new JarFile(new File(jarFileName)).stream()
                                    .map(JarEntry::getRealName)
                                    .filter(s -> s.endsWith(".class"))
                                    .map(className -> className.substring(0, className.length() - ".class".length())) // remove .class extension
                                    .map(className -> className.replaceAll("/", "."))                   // replace path separator "/" with "." (class name)
                                    .map(className -> {
                                        try {
                                            return Class.forName(className, false, ClassLoading.class.getClassLoader());
                                        } catch (Throwable ignored) {
                                            return (Class<?>) null;
                                        }
                                    })
                                    .filter(Objects::nonNull);
                        } catch (Throwable ignored) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Throwable ignored) {
            return new ArrayList<>(0);
        }
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
