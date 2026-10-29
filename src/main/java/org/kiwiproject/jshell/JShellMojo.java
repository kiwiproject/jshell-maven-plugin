/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.kiwiproject.jshell;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import javax.tools.Tool;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

@Mojo(name = "run", defaultPhase = LifecyclePhase.INSTALL, requiresDependencyResolution = ResolutionScope.TEST, requiresDependencyCollection = ResolutionScope.TEST)
public class JShellMojo extends AbstractMojo {

    @Parameter(defaultValue = "${project.runtimeClasspathElements}", readonly = true, required = true)
    private List<String> runtimeClasspathElements;

    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true, required = true)
    private List<String> testClasspathElements;

    @Parameter(property = "plugin.artifacts", required = true, readonly = true)
    private List<Artifact> pluginArtifacts;

    @Parameter(defaultValue = "false", property = "runtimeClasspath")
    private boolean runtimeClasspath;

    @Parameter(defaultValue = "true", property = "testClasspath")
    private boolean testClasspath;

    @Parameter(defaultValue = "true", property = "jshell.useProjectClasspath")
    private boolean useProjectClasspath;

    @Parameter(property = "jshell.class-path")
    private String classpath;

    @Parameter(property = "jshell.module-path")
    private String modulepath;

    @Parameter(property = "jshell.add-modules")
    private String addModules;

    @Parameter(property = "jshell.add-exports")
    private String addExports;

    @Parameter(property = "jshell.scripts")
    private List<String> scripts = new ArrayList<>();

    // additional options that may be added in future Java releases.
    @Parameter(property = "jshell.options")
    private List<String> options = new ArrayList<>();

    /**
     * Execution property overrides.
     * <p>
     * Any property defined here will take precedence over any other definition.
     */
    @Parameter
    private Map<String, String> properties;

    @Parameter(property = "jshell.non-interactive")
    private boolean nonInteractive = false;

    @Parameter(
        defaultValue = "${session}",
        readonly = true,
        required = true
    )
    private MavenSession session;

    @Override
    public void execute() throws MojoExecutionException {
        if (!isSelectedByProjectList()) {
            getLog().debug("Skipping JShell for: " + session.getCurrentProject().getArtifactId());
            return;
        }
        Optional<Module> module = ModuleLayer.boot().findModule("jdk.jshell");
        Optional<ClassLoader> classLoader = module.map(Module::getClassLoader);
        // Until https://issues.apache.org/jira/browse/MNG-6371 is resolved
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            classLoader.ifPresent(cl -> Thread.currentThread().setContextClassLoader(cl));
            if (nonInteractive) {
                try (InputStream scriptInput = ScriptInput.sequence(scripts, getLog())) {
                    runJShell(scriptInput);
                }
            } else {
                // Standard input must stay open and be passed to JShell as-is. Closing it would leave every
                // later module in a multi-module build without any input.
                runJShell(System.in);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read input script file(s)", e);
        } finally {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
        }
    }

    private void runJShell(InputStream input) throws MojoExecutionException {
        ServiceLoader<Tool> sl = ServiceLoader.load(javax.tools.Tool.class);
        Tool jshell = sl.stream()
                .filter(a -> a.get().name().equals("jshell"))
                .findAny()
                .orElseThrow(() -> new RuntimeException("No JShell service providers found!"))
                .get();
        String[] args = addArguments();

        int exitCode = jshell.run(input, System.out, System.err, args);
        if (exitCode != 0) {
            throw new MojoExecutionException("An error was encountered while executing. Exit code: " + exitCode);
        }
    }

    private boolean isSelectedByProjectList() {
        MavenProject project = session.getCurrentProject();
        File pomFile = project.getFile();
        File rootDir = session.getRequest().getMultiModuleProjectDirectory();
        return ProjectSelector.isSelected(
                session.getRequest().getSelectedProjects(),
                project.getGroupId(),
                project.getArtifactId(),
                pomFile == null ? null : pomFile.toPath(),
                rootDir == null ? null : rootDir.toPath());
    }

    private String buildClasspath() {
        final List<String> classpathElements = new ArrayList<>();
        if (testClasspath) {
            classpathElements.addAll(testClasspathElements);
        }
        if (runtimeClasspath) {
            classpathElements.addAll(runtimeClasspathElements);
        }
        if (classpath != null) {
            classpathElements.addAll(Arrays.asList(classpath.split(File.pathSeparator)));
        }
        if (!pluginArtifacts.isEmpty()) {
            classpathElements.addAll(
                    pluginArtifacts.stream()
                            .map(artifact -> artifact.getFile().getAbsolutePath())
                            .collect(Collectors.toList())
            );
        }
        return String.join(File.pathSeparator, ClasspathFilter.filter(classpathElements, getLog()));
    }

    private String[] addArguments() {
        String cp = null;
        if (useProjectClasspath) {
            cp = buildClasspath();
            if (getLog().isDebugEnabled()) {
                getLog().debug("Using classpath: " + cp);
            }
        }
        return new JShellArguments(cp, modulepath, addModules, addExports, options, properties, nonInteractive, scripts)
                .toArray();
    }
}
