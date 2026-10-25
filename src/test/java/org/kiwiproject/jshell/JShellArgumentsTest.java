package org.kiwiproject.jshell;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class JShellArgumentsTest {

    private static JShellArguments args(String classpath,
                                        String modulePath,
                                        String addModules,
                                        String addExports,
                                        List<String> options,
                                        Map<String, String> properties,
                                        boolean nonInteractive,
                                        List<String> scripts) {
        return new JShellArguments(classpath, modulePath, addModules, addExports, options, properties, nonInteractive, scripts);
    }

    @Test
    void shouldBeEmpty_WhenNothingIsConfigured() {
        assertThat(args(null, null, null, null, List.of(), null, false, List.of()).toArray()).isEmpty();
    }

    @Test
    void shouldNotAddClasspath_WhenItIsEmpty() {
        assertThat(args("", null, null, null, List.of(), null, false, List.of()).toArray()).isEmpty();
    }

    @Test
    void shouldAddClasspath() {
        assertThat(args("a.jar:b.jar", null, null, null, List.of(), null, false, List.of()).toArray())
                .containsExactly("--class-path", "a.jar:b.jar");
    }

    @Test
    void shouldAddModulePathAddModulesAndAddExportsInThatOrder() {
        assertThat(args(null, "mods", "m1,m2", "m1/p=m2", List.of(), null, false, List.of()).toArray())
                .containsExactly("--module-path", "mods", "--add-modules", "m1,m2", "--add-exports", "m1/p=m2");
    }

    @Test
    void shouldAddOptionsAsIs() {
        assertThat(args(null, null, null, null, List.of("--enable-preview", "-s"), null, false, List.of()).toArray())
                .containsExactly("--enable-preview", "-s");
    }

    @Test
    void shouldAddPropertiesAsRemoteVmSystemProperties() {
        var properties = new LinkedHashMap<String, String>();
        properties.put("user.language", "fr");
        properties.put("debug", "true");

        assertThat(args(null, null, null, null, List.of(), properties, false, List.of()).toArray())
                .containsExactly("-R -Duser.language=fr", "-R -Ddebug=true");
    }

    @Test
    void shouldAddScripts_WhenInteractive() {
        assertThat(args(null, null, null, null, List.of(), null, false, List.of("a.jsh", "b.jsh")).toArray())
                .containsExactly("a.jsh", "b.jsh");
    }

    @Test
    void shouldReadFromStandardInputInsteadOfAddingScripts_WhenNonInteractive() {
        assertThat(args(null, null, null, null, List.of(), null, true, List.of("a.jsh", "b.jsh")).toArray())
                .containsExactly("-");
    }

    @Test
    void shouldAddEverythingInOrder() {
        var args = args("cp", "mp", "am", "ae", List.of("--enable-preview"), Map.of("k", "v"), false, List.of("a.jsh"));

        assertThat(args.toArray()).containsExactly(
                "--class-path", "cp",
                "--module-path", "mp",
                "--add-modules", "am",
                "--add-exports", "ae",
                "--enable-preview",
                "-R -Dk=v",
                "a.jsh");
    }
}
