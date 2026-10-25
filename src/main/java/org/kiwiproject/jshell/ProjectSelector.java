package org.kiwiproject.jshell;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Decides whether a project is selected by the Maven {@code -pl} (or {@code --projects}) option.
 * <p>
 * Each {@code -pl} entry can be {@code :artifactId}, {@code groupId:artifactId}, or a path relative
 * to the root of the multi-module project. Entries can have a {@code ?} prefix (optional); entries
 * with a {@code !} prefix are exclusions, which Maven already applies to the reactor, so they are ignored.
 */
final class ProjectSelector {

    private ProjectSelector() {
    }

    /**
     * @param selectedProjects the raw {@code -pl} entries, empty if {@code -pl} was not used
     * @param groupId          the groupId of the current project
     * @param artifactId       the artifactId of the current project
     * @param projectDir       the base directory of the current project, may be null
     * @param rootDir          the base directory of the multi-module project, may be null
     * @return true if {@code -pl} was not used, or if it selects the current project
     */
    static boolean isSelected(List<String> selectedProjects,
                              String groupId,
                              String artifactId,
                              Path projectDir,
                              Path rootDir) {

        List<String> selectors = selectedProjects.stream()
                .map(String::trim)
                .map(selector -> selector.startsWith("?") ? selector.substring(1) : selector)
                .filter(selector -> !selector.isEmpty() && !selector.startsWith("!"))
                .collect(Collectors.toList());
        if (selectors.isEmpty()) {
            return true;
        }

        Path normalizedProjectDir = projectDir == null ? null : projectDir.toAbsolutePath().normalize();

        return selectors.stream().anyMatch(selector -> {
            if (selector.equals(artifactId) || selector.equals(":" + artifactId) || selector.equals(groupId + ":" + artifactId)) {
                return true;
            }
            if (normalizedProjectDir == null || rootDir == null) {
                return false;
            }
            Path selectedDir = rootDir.resolve(selector).toAbsolutePath().normalize();
            return selectedDir.equals(normalizedProjectDir)
                    || selectedDir.resolve("pom.xml").equals(normalizedProjectDir.resolve("pom.xml"));
        });
    }
}
