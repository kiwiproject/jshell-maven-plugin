package org.kiwiproject.jshell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

class ClasspathFilterTest {

    @TempDir
    Path dir;

    Log log;

    @BeforeEach
    void setUp() {
        log = mock(Log.class);
    }

    @Test
    void shouldReturnEmptyList_WhenGivenNoElements() {
        assertThat(ClasspathFilter.filter(List.of(), log)).isEmpty();
    }

    @Test
    void shouldKeepDirectoriesAndJarFiles() throws Exception {
        var classes = Files.createDirectory(dir.resolve("classes")).toString();
        var jar = Files.createFile(dir.resolve("lib.jar")).toString();

        assertThat(ClasspathFilter.filter(List.of(classes, jar), log)).containsExactly(classes, jar);
    }

    @Test
    void shouldPreserveOrder() throws Exception {
        var first = Files.createFile(dir.resolve("b.jar")).toString();
        var second = Files.createDirectory(dir.resolve("a")).toString();
        var third = Files.createFile(dir.resolve("c.jar")).toString();

        assertThat(ClasspathFilter.filter(List.of(first, second, third), log))
                .containsExactly(first, second, third);
    }

    @Test
    void shouldRemoveAndLog_WhenElementDoesNotExist() {
        var missing = dir.resolve("missing.jar").toString();

        assertThat(ClasspathFilter.filter(List.of(missing), log)).isEmpty();

        var message = ArgumentCaptor.forClass(String.class);
        verify(log).info(message.capture());
        assertThat(message.getValue())
                .contains("Removing: " + missing + " from the classpath.")
                .contains("mvn test-compile org.kiwiproject:jshell-maven-plugin:")
                .contains("https://github.com/kiwiproject/jshell-maven-plugin");
    }

    @Test
    void shouldRemoveExistingFilesThatAreNotJars() throws Exception {
        var text = Files.createFile(dir.resolve("notes.txt")).toString();
        var war = Files.createFile(dir.resolve("app.war")).toString();

        assertThat(ClasspathFilter.filter(List.of(text, war), log)).isEmpty();
        verify(log, never()).info(anyString());
    }

    @Test
    void shouldLogUnsupportedElements_WhenDebugIsEnabled() throws Exception {
        when(log.isDebugEnabled()).thenReturn(true);
        var text = Files.createFile(dir.resolve("notes.txt")).toString();

        ClasspathFilter.filter(List.of(text), log);

        verify(log).debug("Removing: " + text + " from the classpath because it is unsupported in JShell.");
    }

    @Test
    void shouldNotLogUnsupportedElements_WhenDebugIsDisabled() throws Exception {
        var text = Files.createFile(dir.resolve("notes.txt")).toString();

        ClasspathFilter.filter(List.of(text), log);

        verify(log, never()).debug(anyString());
    }

    @Test
    void shouldKeepOnlyUsableElements_WhenGivenAMix() throws Exception {
        var classes = Files.createDirectory(dir.resolve("classes")).toString();
        var jar = Files.createFile(dir.resolve("lib.jar")).toString();
        var text = Files.createFile(dir.resolve("notes.txt")).toString();
        var missing = dir.resolve("missing").toString();

        assertThat(ClasspathFilter.filter(List.of(missing, classes, text, jar), log))
                .containsExactly(classes, jar);
    }
}
