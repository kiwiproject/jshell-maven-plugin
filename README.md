[![Build](https://github.com/kiwiproject/jshell-maven-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/kiwiproject/jshell-maven-plugin/actions/workflows/build.yml?query=branch%3Amain)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_jshell-maven-plugin&metric=alert_status)](https://sonarcloud.io/dashboard?id=kiwiproject_jshell-maven-plugin)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_jshell-maven-plugin&metric=coverage)](https://sonarcloud.io/dashboard?id=kiwiproject_jshell-maven-plugin)
[![CodeQL](https://github.com/kiwiproject/jshell-maven-plugin/actions/workflows/codeql.yml/badge.svg)](https://github.com/kiwiproject/jshell-maven-plugin/actions/workflows/codeql.yml)
[![javadoc](https://javadoc.io/badge2/org.kiwiproject/jshell-maven-plugin/javadoc.svg)](https://javadoc.io/doc/org.kiwiproject/jshell-maven-plugin)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.kiwiproject/jshell-maven-plugin)](https://central.sonatype.com/artifact/org.kiwiproject/jshell-maven-plugin/)

---

🥝 _This README will be updated as we transition from johnpoth/jshell-maven-plugin to this repository._ 🥝

---

## Introduction

A Maven plugin for the Java Shell tool ([JShell](https://docs.oracle.com/en/java/javase/17/jshell/introduction-jshell.html)).
It starts JShell with your project's classpath, so you can try out your own classes and dependencies interactively.

## Background

This plugin was imported from [johnpoth/jshell-maven-plugin](https://github.com/johnpoth/jshell-maven-plugin) because
it is no longer maintained. We use it in [kiwi-parent](https://github.com/kiwiproject/kiwi-parent), and wanted to be
able to fix problems and keep dependencies up to date.

For now, we intend to at least keep dependency versions up to date, and perhaps make some minimal changes and/or
improvements as necessary. We make no guarantees whatsoever about anything, however.

All other [kiwiproject](https://github.com/kiwiproject) projects are MIT-licensed. However, because the original
plugin uses the Apache 2.0 license, this also uses the Apache 2.0 license. Changing it would require the consent of all
the original contributors.

This is a disconnected fork. We imported the original repository, so that it keeps the full history, but there is no
link to the original repository since it is no longer maintained. Our changes will _never_ be pushed back to it. While
we maintain the history that this is a fork, it is completely disconnected and is now a standalone (normal) repository.

Thanks to John Poth for creating the plugin, and to jycr and Pablo Ilardi for contributions that were never merged
upstream and which we have included here.

## Migrating from johnpoth/jshell-maven-plugin

* The groupId changed from `com.github.johnpoth` to `org.kiwiproject`. The artifactId is still
  `jshell-maven-plugin` and the plugin prefix is still `jshell`, so `mvn jshell:run` works as before.
* The first release is `2.0.0`, since you must change your POM.
* Java 17 or later is now required (upstream supported Java 9 and later).
* The project test classpath is now used by default (`testClasspath` is `true`), so make sure test classes are
  compiled, for example by running `mvn test-compile jshell:run`.
* The Java package changed from `com.github.johnpoth.jshell` to `org.kiwiproject.jshell`, and `module-info.java` was
  removed in favor of an `Automatic-Module-Name` of `org.kiwiproject.jshell`.
* Multi-module builds respect `-pl`. Without it, JShell is started once for each module, as before.
* A new `non-interactive` option runs scripts and then exits.

## Usage

Declare the plugin in your POM so that you can use the short prefix `jshell`:

```xml
<pluginManagement>
  <plugins>
    <plugin>
      <groupId>org.kiwiproject</groupId>
      <artifactId>jshell-maven-plugin</artifactId>
      <version>2.0.0</version>
    </plugin>
  </plugins>
</pluginManagement>
```

Then compile your project and run it:

```shell
mvn test-compile jshell:run
```

Or without declaring it, use the full coordinates:

```shell
mvn test-compile org.kiwiproject:jshell-maven-plugin:2.0.0:run
```

If you notice some resources missing, you may have to invoke the `install` phase instead:

```shell
mvn install jshell:run -DskipTests
```

Once your project has been built, and you haven't changed any source files, you can skip the build and invoke the
plugin directly:

```shell
mvn jshell:run
```

## Options

| Name                | Description                                                         | Default value |
|---------------------|---------------------------------------------------------------------|---------------|
| class-path          | Explicitly set JShell's class path                                  |               |
| module-path         | Set JShell's module path                                            |               |
| add-modules         | Add modules from JShell's module path                               |               |
| add-exports         | Explicitly add exports                                              |               |
| scripts             | Add startup scripts to JShell                                       |               |
| useProjectClasspath | Use the project class path in JShell                                | `true`        |
| runtimeClasspath    | Add `./target/classes` to the class path                            | `false`       |
| testClasspath       | Add `./target/test-classes` to the class path                       | `true`        |
| properties          | Configure Java properties                                           |               |
| options             | Add other options to JShell, see the [JShell options][jshell-opts]  |               |
| non-interactive     | If `true`, execute scripts and exit                                 | `false`       |

[jshell-opts]: https://docs.oracle.com/javase/9/tools/jshell.htm#GUID-C337353B-074A-431C-993F-60C226163F00__OPTIONSFORJSHELL-AF4AC615

Most options are prefixed with `jshell.` when passed on the command line, for example:

```shell
mvn jshell:run -Djshell.scripts="script0,script1"
```

The exceptions are `runtimeClasspath` and `testClasspath`, which are passed as `-DruntimeClasspath` and
`-DtestClasspath`.

Requires Java 17 or later.
