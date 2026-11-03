package org.kiwiproject.jshell;

import org.apache.maven.plugin.logging.Log;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Removes classpath elements that JShell cannot use.
 */
final class ClasspathFilter {

    private static final String VERSION = Optional.ofNullable(
            ClasspathFilter.class.getPackage().getImplementationVersion()
    ).orElse("RELEASE");

    private ClasspathFilter() {
    }

    /**
     * Keep only elements that exist and are either directories or JAR files.
     */
    static List<String> filter(List<String> classpathElements, Log log) {
        return classpathElements.stream()
                .filter(element -> {
                    Path path = Path.of(element);
                    if (Files.notExists(path)) {
                        log.info(String.format(
                                "Removing: %s from the classpath.%n" +
                                        "If this is unexpected, please make sure you correctly build the project beforehand by invoking the correct Maven build phase (usually `install`, `test-compile` or `compile`). For example:%n" +
                                        "mvn test-compile org.kiwiproject:jshell-maven-plugin:%s:run%n" +
                                        "For more information visit https://github.com/kiwiproject/jshell-maven-plugin",
                                element,
                                VERSION
                        ));
                        return false;
                    }
                    if (Files.isDirectory(path) || element.endsWith(".jar")) {
                        return true;
                    }
                    if (log.isDebugEnabled()) {
                        log.debug("Removing: " + element + " from the classpath because it is unsupported in JShell.");
                    }
                    return false;
                }).toList();
    }
}
