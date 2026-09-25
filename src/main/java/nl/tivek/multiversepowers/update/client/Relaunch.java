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

/**
 * Puts together the command that started this game, so the game can be started again after an update.
 *
 * <p>Java does not keep that command (on Windows not at all), so it is rebuilt from what Java still knows: its own
 * options, the class path, and the main class with the game's options. That works for every launcher that starts
 * Java itself (the Minecraft Launcher, Modrinth App, CurseForge, ATLauncher, GDLauncher). Prism Launcher and MultiMC
 * hand the game its options through a hidden pipe instead, so a copy of the command cannot start the game there.
 *
 * <p>The command holds the player's login token, so it is only ever written to the one file the new game reads at
 * its start, and never to a log.
 */
final class Relaunch {
    /** Main classes of launchers that pipe the game's options in, so a rebuilt command would hang. */
    private static final List<String> PIPED_LAUNCHERS = List.of("org.prismlauncher.", "org.multimc.");
    /** How Java hands on the module options it was started with, and the option each one came from. */
    private static final Pattern MODULE_PROPERTY = Pattern.compile(
            "-Djdk\\.module\\.(path|upgrade\\.path|limitmods|addmods|addopens|addexports|addreads|patch|enable\\.native\\.access)(\\.\\d+)?=(.*)",
            Pattern.DOTALL);

    private Relaunch() {
    }

    /** Whether the game can be started again from here. */
    static boolean possible() {
        return arguments() != null && encoder().canEncode(String.join(" ", arguments()));
    }

    /** The Java program that runs this game. */
    static String javaCommand() {
        return ProcessHandle.current().info().command().orElse(UpdateInstaller.javaProgram(false));
    }

    /** Everything after the Java program itself, or null when this game cannot be started again from here. */
    @Nullable
    static List<String> arguments() {
        String command = System.getProperty("sun.java.command");
        if (command == null || command.isBlank()) {
            return null;
        }
        List<String> program = splitProgram(command.strip());
        String mainClass = program.get(0);
        if (mainClass.endsWith(".jar") || PIPED_LAUNCHERS.stream().anyMatch(mainClass::startsWith)) {
            return null;
        }
        List<String> arguments = new ArrayList<>();
        for (String option : ManagementFactory.getRuntimeMXBean().getInputArguments()) {
            if (option.startsWith("-Djava.class.path=") || option.startsWith("-Dsun.java.command=")
                    || option.startsWith("-Dsun.java.launcher")) {
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

    /**
     * The main class and the game's options, which Java keeps joined by spaces. The game's options come in pairs
     * ({@code --gameDir <folder>}), so words after a value that do not start with {@code --} belong to that value:
     * a folder like {@code My Pack} stays one option.
     */
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

    /**
     * Writes the arguments as a Java argument file ({@code java @file}): one per line, each in quotes, with its
     * backslashes and quotes escaped. Java reads the file in the system's own encoding.
     */
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
