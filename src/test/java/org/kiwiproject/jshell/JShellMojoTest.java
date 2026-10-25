package org.kiwiproject.jshell;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Runs the Mojo against the real JShell tool from the JDK running the tests, in non-interactive mode.
 */
class JShellMojoTest {

    @TempDir
    Path dir;

    JShellMojo mojo;
    MavenExecutionRequest request;
    MavenProject project;

    PrintStream originalOut;
    ByteArrayOutputStream capturedOut;

    @BeforeEach
    void setUp() throws Exception {
        originalOut = System.out;
        capturedOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(capturedOut, true, StandardCharsets.UTF_8));

        request = mock(MavenExecutionRequest.class);
        when(request.getSelectedProjects()).thenReturn(List.of());

        project = mock(MavenProject.class);
        when(project.getGroupId()).thenReturn("com.acme");
        when(project.getArtifactId()).thenReturn("foo");
        when(project.getFile()).thenReturn(dir.resolve("pom.xml").toFile());

        var session = mock(MavenSession.class);
        when(session.getRequest()).thenReturn(request);
        when(session.getCurrentProject()).thenReturn(project);

        mojo = new JShellMojo();
        mojo.setLog(new SystemStreamLog());
        set("session", session);
        set("runtimeClasspathElements", List.of());
        set("testClasspathElements", List.of());
        set("pluginArtifacts", List.of());
        set("useProjectClasspath", true);
        set("testClasspath", true);
        set("nonInteractive", true);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
    }

    private void set(String fieldName, Object value) throws Exception {
        Field field = JShellMojo.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(mojo, value);
    }

    private String script(String content) throws Exception {
        return Files.writeString(dir.resolve("script.jsh"), content).toString();
    }

    private String output() {
        return capturedOut.toString(StandardCharsets.UTF_8);
    }

    @Test
    void shouldRunScriptsInJShell() throws Exception {
        set("scripts", List.of(script("System.out.println(\"hello from jshell\")\n/exit\n")));

        mojo.execute();

        assertThat(output()).contains("hello from jshell");
    }

    @Test
    void shouldRunMultipleScriptsInOrder() throws Exception {
        var first = Files.writeString(dir.resolve("first.jsh"), "int answer = 40 + 2;\n").toString();
        var second = Files.writeString(dir.resolve("second.jsh"), "System.out.println(\"answer=\" + answer)\n/exit\n").toString();
        set("scripts", List.of(first, second));

        mojo.execute();

        assertThat(output()).contains("answer=42");
    }

    @Test
    void shouldMakeProjectClasspathAvailable() throws Exception {
        var classes = Files.createDirectory(dir.resolve("classes"));
        set("testClasspathElements", List.of(classes.toString()));
        set("scripts", List.of(script("System.out.println(System.getProperty(\"java.class.path\"))\n/exit\n")));

        mojo.execute();

        assertThat(output()).contains(classes.toString());
    }

    @Test
    void shouldPassPropertiesToTheRemoteVm() throws Exception {
        set("properties", Map.of("greeting", "howdy"));
        set("scripts", List.of(script("System.out.println(\"greeting=\" + System.getProperty(\"greeting\"))\n/exit\n")));

        mojo.execute();

        assertThat(output()).contains("greeting=howdy");
    }

    @Test
    void shouldNotStartTheClasspathWithASeparator() throws Exception {
        var classes = Files.createDirectory(dir.resolve("classes"));
        set("testClasspathElements", List.of(classes.toString()));
        set("scripts", List.of(script("System.out.println(\"cp=[\" + System.getProperty(\"java.class.path\") + \"]\")\n/exit\n")));

        mojo.execute();

        assertThat(output())
                .contains("cp=[" + classes)
                .doesNotContain("cp=[" + File.pathSeparator);
    }

    @Test
    void shouldSkipProject_WhenNotSelectedByProjectList() throws Exception {
        when(request.getSelectedProjects()).thenReturn(List.of(":bar"));
        set("scripts", List.of(script("System.out.println(\"should not run\")\n/exit\n")));

        mojo.execute();

        assertThat(output()).doesNotContain("should not run");
    }

    @Test
    void shouldRunProject_WhenSelectedByProjectList() throws Exception {
        when(request.getSelectedProjects()).thenReturn(List.of(":foo"));
        set("scripts", List.of(script("System.out.println(\"selected\")\n/exit\n")));

        mojo.execute();

        assertThat(output()).contains("selected");
    }

    @Test
    void shouldNotFail_WhenThereAreNoScripts() throws Exception {
        set("scripts", List.of());

        assertThatCode(() -> mojo.execute()).doesNotThrowAnyException();
    }
}
