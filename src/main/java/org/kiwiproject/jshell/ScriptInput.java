package org.kiwiproject.jshell;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds the input for JShell's non-interactive mode, in which the scripts are fed to JShell as standard input.
 */
final class ScriptInput {

    private ScriptInput() {
    }

    /**
     * Concatenate the given script files, in order, into a single stream. Anything that is not a regular file
     * is skipped.
     */
    static InputStream sequence(List<String> scripts, Log log) {
        if (log.isDebugEnabled()) {
            log.debug("Non-interactive mode. inject script in stdin:" + String.join(", ", scripts));
        }
        return new SequenceInputStream(Collections.enumeration(
                scripts.stream()
                        .map(script -> toInputStream(script, log))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        ));
    }

    private static InputStream toInputStream(String scriptPath, Log log) {
        final Path path = Path.of(scriptPath);
        if (!Files.isRegularFile(path)) {
            if (log.isDebugEnabled()) {
                log.debug("Not a regular file: " + scriptPath);
            }
            return null;
        }
        try {
            return new BufferedInputStream(Files.newInputStream(path));
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read input script file(s)", e);
        }
    }
}
