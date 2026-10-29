package org.kiwiproject.jshell;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Decides whether a project is selected by the Maven {@code -pl} (or {@code --projects}) option.
 * <p>
 * Each {@code -pl} entry can be {@code :artifactId}, {@code groupId:artifactId}, or (when it has no colon) a path relative
 * to the root of the multi-module project, either to the module directory or to its POM file. Entries can have a {@code ?} prefix (optional); entries
 * with a {@code !} prefix are exclusions, which Maven already applies to the reactor, so they are ignored.
 */
final class ProjectSelector {

    private ProjectSelector() {
    }

    /**
     * @param selectedProjects the raw {@code -pl} entries, empty if {@code -pl} was not used
     * @param groupId          the groupId of the current project
     * @param artifactId       the artifactId of the current project
     * @param projectFile     the POM file of the current project, may be null
     * @param rootDir          the base directory of the multi-module project, may be null
     * @return true if {@code -pl} was not used, or if it selects the current project
     */
    static boolean isSelected(List<String> selectedProjects,
                              String groupId,
                              String artifactId,
                              Path projectFile,
                              Path rootDir) {

        List<String> selectors = selectedProjects.stream()
                .map(String::trim)
                .map(selector -> selector.startsWith("?") ? selector.substring(1) : selector)
                .filter(selector -> !selector.isEmpty() && !selector.startsWith("!"))
                .collect(Collectors.toList());
        if (selectors.isEmpty()) {
            return true;
        }

        Path normalizedProjectFile = projectFile == null ? null : projectFile.toAbsolutePath().normalize();
        Path normalizedProjectDir = normalizedProjectFile == null ? null : normalizedProjectFile.getParent();

        return selectors.stream().anyMatch(selector -> {
            if (selector.equals(":" + artifactId) || selector.equals(groupId + ":" + artifactId)) {
                return true;
            }
            if (normalizedProjectFile == null || rootDir == null) {
                return false;
            }
            Path selectedPath = rootDir.resolve(selector).toAbsolutePath().normalize();
            return selectedPath.equals(normalizedProjectDir) || selectedPath.equals(normalizedProjectFile);
        });
    }
}
