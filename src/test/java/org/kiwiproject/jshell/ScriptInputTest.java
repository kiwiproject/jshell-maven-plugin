package org.kiwiproject.jshell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

class ScriptInputTest {

    @TempDir
    Path dir;

    Log log;

    @BeforeEach
    void setUp() {
        log = mock(Log.class);
    }

    private String script(String name, String content) throws Exception {
        return Files.writeString(dir.resolve(name), content).toString();
    }

    private String read(List<String> scripts) throws Exception {
        try (InputStream in = ScriptInput.sequence(scripts, log)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void shouldBeEmpty_WhenThereAreNoScripts() throws Exception {
        assertThat(read(List.of())).isEmpty();
    }

    @Test
    void shouldReadASingleScript() throws Exception {
        assertThat(read(List.of(script("a.jsh", "int x = 1;\n")))).isEqualTo("int x = 1;\n");
    }

    @Test
    void shouldConcatenateScriptsInOrder() throws Exception {
        var first = script("first.jsh", "int x = 1;\n");
        var second = script("second.jsh", "int y = 2;\n");
        var third = script("third.jsh", "/exit\n");

        assertThat(read(List.of(second, third, first))).isEqualTo("int y = 2;\n/exit\nint x = 1;\n");
    }

    @Test
    void shouldSkipScriptsThatDoNotExist() throws Exception {
        var present = script("present.jsh", "int x = 1;\n");

        assertThat(read(List.of(dir.resolve("missing.jsh").toString(), present))).isEqualTo("int x = 1;\n");
    }

    @Test
    void shouldSkipDirectories() throws Exception {
        var directory = Files.createDirectory(dir.resolve("a-directory")).toString();
        var present = script("present.jsh", "int x = 1;\n");

        assertThat(read(List.of(directory, present))).isEqualTo("int x = 1;\n");
    }

    @Test
    void shouldBeEmpty_WhenNoScriptCanBeRead() throws Exception {
        assertThat(read(List.of(dir.resolve("missing.jsh").toString()))).isEmpty();
    }

    @Test
    void shouldLogScriptsAndSkippedFiles_WhenDebugIsEnabled() throws Exception {
        when(log.isDebugEnabled()).thenReturn(true);
        var present = script("present.jsh", "int x = 1;\n");
        var missing = dir.resolve("missing.jsh").toString();

        read(List.of(present, missing));

        verify(log).debug("Non-interactive mode. inject script in stdin:" + present + ", " + missing);
        verify(log).debug("Not a regular file: " + missing);
    }

    @Test
    void shouldFail_WhenScriptCannotBeRead() throws Exception {
        assumeTrue(dir.getFileSystem().supportedFileAttributeViews().contains("posix"), "requires POSIX permissions");
        var unreadable = Files.createFile(dir.resolve("unreadable.jsh"));
        Files.setPosixFilePermissions(unreadable, PosixFilePermissions.fromString("---------"));
        assumeTrue(!Files.isReadable(unreadable), "permissions are not enforced, e.g. when running as root");

        var scripts = List.of(unreadable.toString());
        assertThatIOException().isThrownBy(() -> ScriptInput.sequence(scripts, log));
    }
}
