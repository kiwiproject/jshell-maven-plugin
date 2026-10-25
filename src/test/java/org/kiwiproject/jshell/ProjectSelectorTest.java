package org.kiwiproject.jshell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ProjectSelectorTest {

    private static final String GROUP_ID = "com.acme";
    private static final String ARTIFACT_ID = "foo";

    @TempDir
    Path rootDir;

    Path fooDir;

    Path fooPom;

    @BeforeEach
    void setUp() throws Exception {
        fooDir = Files.createDirectories(rootDir.resolve("modules").resolve("foo"));
        fooPom = fooDir.resolve("pom.xml");
    }

    private boolean isSelected(String... selectors) {
        return ProjectSelector.isSelected(List.of(selectors), GROUP_ID, ARTIFACT_ID, fooPom, rootDir);
    }

    @Test
    void shouldBeSelected_WhenNoProjectsAreSelected() {
        assertThat(isSelected()).isTrue();
    }

    @Nested
    class MatchByCoordinates {

        @ParameterizedTest
        @ValueSource(strings = {":foo", "com.acme:foo"})
        void shouldBeSelected_WhenSelectorMatchesArtifactId(String selector) {
            assertThat(isSelected(selector)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {":bar", "bar", "com.acme:bar", "org.other:foo", "fo", ":fo"})
        void shouldNotBeSelected_WhenSelectorDoesNotMatch(String selector) {
            assertThat(isSelected(selector)).isFalse();
        }

        @Test
        void shouldTreatSelectorWithoutColonAsPath_NotAsArtifactId() {
            // Maven only treats a selector with a colon as coordinates, so "foo" means the path ./foo
            assertThat(isSelected("foo")).isFalse();
        }

        @Test
        void shouldBeSelected_WhenAnySelectorMatches() {
            assertThat(isSelected(":bar", ":foo", ":baz")).isTrue();
        }

        @Test
        void shouldNotBeSelected_WhenNoSelectorMatches() {
            assertThat(isSelected(":bar", ":baz")).isFalse();
        }
    }

    @Nested
    class MatchByPath {

        @ParameterizedTest
        @ValueSource(strings = {"modules/foo", "./modules/foo", "modules/foo/", "modules/../modules/foo", "modules/foo/pom.xml"})
        void shouldBeSelected_WhenSelectorIsPathToProject(String selector) {
            assertThat(isSelected(selector)).isTrue();
        }

        @Test
        void shouldNotBeSelected_WhenSelectorIsPathToAnotherModule() throws Exception {
            Files.createDirectories(rootDir.resolve("bar"));

            assertThat(isSelected("bar")).isFalse();
            assertThat(isSelected("modules/bar")).isFalse();
        }

        @Test
        void shouldNotBeSelected_WhenSelectorIsTheRootDirectory() {
            assertThat(isSelected(".")).isFalse();
        }

        @Test
        void shouldBeSelected_WhenCurrentProjectIsTheRoot() {
            assertThat(ProjectSelector.isSelected(List.of("."), "com.acme", "parent", rootDir.resolve("pom.xml"), rootDir)).isTrue();
        }

        @Test
        void shouldBeSelected_WhenSelectorIsPathToPomWithNonStandardName() {
            var customPom = fooDir.resolve("pom-foo.xml");

            assertThat(ProjectSelector.isSelected(List.of("modules/foo/pom-foo.xml"), GROUP_ID, ARTIFACT_ID, customPom, rootDir)).isTrue();
        }

        @Test
        void shouldNotBeSelected_WhenSelectorIsPathToADifferentPomInTheSameDirectory() {
            var customPom = fooDir.resolve("pom-foo.xml");

            assertThat(ProjectSelector.isSelected(List.of("modules/foo/pom.xml"), GROUP_ID, ARTIFACT_ID, customPom, rootDir)).isFalse();
        }

        @Test
        void shouldNotMatchPaths_WhenProjectFileIsUnknown() {
            assertThat(ProjectSelector.isSelected(List.of("modules/foo"), GROUP_ID, ARTIFACT_ID, null, rootDir)).isFalse();
        }

        @Test
        void shouldNotMatchPaths_WhenRootDirectoryIsUnknown() {
            assertThat(ProjectSelector.isSelected(List.of("modules/foo"), GROUP_ID, ARTIFACT_ID, fooPom, null)).isFalse();
        }

        @Test
        void shouldStillMatchCoordinates_WhenDirectoriesAreUnknown() {
            assertThat(ProjectSelector.isSelected(List.of(":foo"), GROUP_ID, ARTIFACT_ID, null, null)).isTrue();
        }
    }

    @Nested
    class OptionalAndExcludedSelectors {

        @Test
        void shouldIgnoreOptionalPrefix() {
            assertThat(isSelected("?:foo")).isTrue();
            assertThat(isSelected("?modules/foo")).isTrue();
            assertThat(isSelected("?:bar")).isFalse();
        }

        @Test
        @DisplayName("exclusions are applied by Maven, so a list of only exclusions selects everything")
        void shouldBeSelected_WhenOnlyExclusionsAreGiven() {
            assertThat(isSelected("!:bar")).isTrue();
            assertThat(isSelected("!:foo")).isTrue();
        }

        @Test
        void shouldIgnoreExclusions_WhenMixedWithInclusions() {
            assertThat(isSelected("!:bar", ":foo")).isTrue();
            assertThat(isSelected("!:bar", ":baz")).isFalse();
        }

        @Test
        void shouldIgnoreBlankSelectors() {
            assertThat(isSelected("", "  ")).isTrue();
            assertThat(isSelected("", ":bar")).isFalse();
        }

        @Test
        void shouldTrimSelectors() {
            assertThat(isSelected("  :foo  ")).isTrue();
        }
    }
}
