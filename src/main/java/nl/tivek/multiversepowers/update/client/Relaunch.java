package nl.tivek.multiversepowers.update.client;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

final class Relaunch {
    // These pipe options in rather than a command line; a rebuilt one would hang.
    private static final List<String> PIPED_LAUNCHERS = List.of("org.prismlauncher.", "org.multimc.");
    // The Modrinth App's wrapper waits for a call from the app, which is gone by then: start its main class directly.
    private static final String MODRINTH_WRAPPER = "com.modrinth.theseus.MinecraftLaunch";
    private static final String MODRINTH_IPC = "-Dmodrinth.internal.";
    private static final Pattern MODULE_PROPERTY = Pattern.compile(
            "-Djdk\\.module\\.(path|upgrade\\.path|limitmods|addmods|addopens|addexports|addreads|patch|enable\\.native\\.access)(\\.\\d+)?=(.*)",
            Pattern.DOTALL);

    private Relaunch() {
    }

    static boolean possible() {
        return arguments() != null && encoder().canEncode(String.join(" ", arguments()));
    }

    static String javaCommand() {
        return ProcessHandle.current().info().command().orElse(UpdateInstaller.javaProgram(false));
    }

    @Nullable
    static List<String> arguments() {
        String command = System.getProperty("sun.java.command");
        if (command == null || command.isBlank()) {
            return null;
        }
        List<String> program = splitProgram(command.strip());
        if (program.get(0).equals(MODRINTH_WRAPPER)) {
            if (program.size() < 2) {
                return null;
            }
            program.remove(0);
        }
        String mainClass = program.get(0);
        if (mainClass.endsWith(".jar") || PIPED_LAUNCHERS.stream().anyMatch(mainClass::startsWith)) {
            return null;
        }
        List<String> arguments = new ArrayList<>();
        for (String option : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (option.startsWith("-Djava.class.path=") || option.startsWith("-Dsun.java.command=")
                    || option.startsWith("-Dsun.java.launcher") || option.startsWith(MODRINTH_IPC)) {
                continue;
            }
            if (option.startsWith("-Djdk.module.")) {
                Matcher module = MODULE_PROPERTY.matcher(option);
                if (!module.matches()) {
                    return null;
                }
                arguments.add(moduleOption(module.group(1)));
                arguments.add(module.group(3));
                continue;
            }
            arguments.add(option);
        }
        String classPath = System.getProperty("java.class.path");
        if (classPath != null && !classPath.isEmpty()) {
            arguments.add("-cp");
            arguments.add(classPath);
        }
        arguments.addAll(program);
        return arguments;
    }

    private static String moduleOption(String property) {
        return switch (property) {
            case "path" -> "--module-path";
            case "upgrade.path" -> "--upgrade-module-path";
            case "limitmods" -> "--limit-modules";
            case "addmods" -> "--add-modules";
            case "addopens" -> "--add-opens";
            case "addexports" -> "--add-exports";
            case "addreads" -> "--add-reads";
            case "patch" -> "--patch-module";
            default -> "--enable-native-access";
        };
    }

    static List<String> splitProgram(String command) {
        String[] words = command.split(" ", -1);
        List<String> program = new ArrayList<>();
        program.add(words[0]);
        boolean afterKey = false;
        for (int i = 1; i < words.length; i++) {
            String word = words[i];
            if (word.startsWith("--")) {
                program.add(word);
                afterKey = true;
            } else if (afterKey || program.size() == 1) {
                program.add(word);
                afterKey = false;
            } else {
                int last = program.size() - 1;
                program.set(last, program.get(last) + " " + word);
            }
        }
        return program;
    }

    // May hold the login token: write only here, never log it.
    static void writeArgumentFile(Path file, List<String> arguments) throws IOException {
        StringBuilder text = new StringBuilder();
        for (String argument : arguments) {
            text.append('"').append(argument.replace("\\", "\\\\").replace("\"", "\\\"")).append('"').append('\n');
        }
        Files.writeString(file, text, encoder().charset());
    }

    private static CharsetEncoder encoder() {
        String name = System.getProperty("native.encoding", System.getProperty("sun.jnu.encoding"));
        try {
            return (name == null ? Charset.defaultCharset() : Charset.forName(name)).newEncoder();
        } catch (IllegalArgumentException e) {
            return Charset.defaultCharset().newEncoder();
        }
    }
}
