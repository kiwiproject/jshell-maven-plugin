package org.kiwiproject.jshell;

import org.apache.maven.plugin.logging.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds the input for JShell's non-interactive mode, in which the scripts are fed to JShell as standard input.
 */
final class ScriptInput {

    private ScriptInput() {
    }

    /**
     * Concatenate the given script files, in order, into a single stream. Anything that is not a regular file
     * is skipped.
     *
     * @throws IOException if a script cannot be opened; any scripts already opened are closed
     */
    static InputStream sequence(List<String> scripts, Log log) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug("Non-interactive mode. inject script in stdin:" + String.join(", ", scripts));
        }

        List<InputStream> streams = new ArrayList<>();
        try {
            for (String script : scripts) {
                Path path = Path.of(script);
                if (Files.isRegularFile(path)) {
                    streams.add(new BufferedInputStream(Files.newInputStream(path)));
                } else if (log.isDebugEnabled()) {
                    log.debug("Not a regular file: " + script);
                }
            }
        } catch (IOException e) {
            closeQuietly(streams, e);
            throw e;
        }
        return new SequenceInputStream(Collections.enumeration(streams));
    }

    private static void closeQuietly(List<InputStream> streams, IOException primary) {
        for (InputStream stream : streams) {
            try {
                stream.close();
            } catch (IOException e) {
                primary.addSuppressed(e);
            }
        }
    }
}
