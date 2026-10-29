package org.kiwiproject.jshell;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The command line arguments passed to JShell.
 *
 * @param classpath      the class path, or null or empty to not pass {@code --class-path}
 * @param modulePath     the module path, or null
 * @param addModules     the modules to add, or null
 * @param addExports     the exports to add, or null
 * @param options        additional options
 * @param properties     Java properties, or null
 * @param interactive    whether JShell is interactive; if false, the scripts are read from standard input instead of
 *                       being passed as arguments
 * @param scripts        the startup scripts
 */
record JShellArguments(String classpath,
                       String modulePath,
                       String addModules,
                       String addExports,
                       List<String> options,
                       Map<String, String> properties,
                       boolean interactive,
                       List<String> scripts) {

    String[] toArray() {
        List<String> args = new ArrayList<>();
        if (classpath != null && !classpath.isEmpty()) {
            args.add("--class-path");
            args.add(classpath);
        }
        if (modulePath != null) {
            args.add("--module-path");
            args.add(modulePath);
        }
        if (addModules != null) {
            args.add("--add-modules");
            args.add(addModules);
        }
        if (addExports != null) {
            args.add("--add-exports");
            args.add(addExports);
        }

        args.addAll(options);

        if (properties != null) {
            properties.forEach((key, value) -> args.add("-R -D" + key + "=" + value));
        }

        if (interactive) {
            args.addAll(scripts);
        } else {
            args.add("-");
        }

        return args.toArray(new String[0]);
    }
}
