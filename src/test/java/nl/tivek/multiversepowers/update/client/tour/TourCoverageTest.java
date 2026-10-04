package nl.tivek.multiversepowers.update.client.tour;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Kind;
import org.junit.jupiter.api.Test;

// Every release's tour shows every change of the CHANGELOG.md sections it covers (`TourSteps.FROM` up to its own); a
// Project line (the repository, nothing in the game) needs no step.
class TourCoverageTest {
    private static final Map<String, Kind> KINDS = Map.of("Added", Kind.NEW, "Changed", Kind.CHANGED, "Fixed",
            Kind.FIXED, "Removed", Kind.REMOVED);
    private static final String PROJECT = "Project:";

    private record Line(String version, Kind kind, String text) {
    }

    @Test
    void everyChangeOfThisVersionHasItsStep() throws IOException {
        Path root = Path.of(System.getProperty("project.root", "."));
        String version = version(root);
        List<String> problems = new ArrayList<>();
        List<Line> lines = sections(root, TourSteps.FROM, version, problems);
        if (!TourSteps.VERSION.equals(version)) {
            problems.add("TourSteps.VERSION is " + TourSteps.VERSION + " but mod_version is " + version
                    + ": give this version its own steps");
        }
        Set<Line> told = new HashSet<>();
        for (TourStep step : TourSteps.ALL) {
            check(step, lines, told, problems);
        }
        for (Line line : lines) {
            if (!line.text().startsWith(PROJECT) && !told.contains(line)) {
                problems.add("no step covers \"" + line.text() + "\"");
            }
        }
        assertTrue(problems.isEmpty(), "The tour of " + version + " misses changes (TourSteps, covers):\n  "
                + String.join("\n  ", problems));
    }

    private static void check(TourStep step, List<Line> lines, Set<Line> told, List<String> problems) {
        if (step.kind() == Kind.HOW) {
            if (!step.version().equals(TourSteps.VERSION)) {
                problems.add(step.id() + " is of " + step.version() + ", not " + TourSteps.VERSION);
            }
            if (!step.covers().isEmpty()) {
                problems.add(step.id() + " shows how something works, so it covers no change");
            }
            return;
        }
        if (step.covers().isEmpty()) {
            problems.add(step.id() + " is a " + step.kind() + " step but covers no changelog line");
        }
        for (int i = 0; i < step.covers().size(); i++) {
            String start = step.covers().get(i);
            List<Line> found = lines.stream().filter(line -> line.text().startsWith(start)).toList();
            if (found.size() != 1) {
                problems.add(step.id() + ": \"" + start + "\" starts " + found.size() + " changelog lines, not 1");
                continue;
            }
            Line line = found.get(0);
            told.add(line);
            if (i == 0 && (line.kind() != step.kind() || !line.version().equals(step.version()))) {
                problems.add(step.id() + " is " + step.kind() + " of " + step.version() + " but its first line is "
                        + line.kind() + " of " + line.version() + ": \"" + start + "\"");
            }
        }
    }

    private static String version(Path root) throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(root.resolve("gradle.properties"), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties.getProperty("mod_version", "").trim();
    }

    // The lines of the sections from `version` down to `from`, each joined into one, without its bold and code marks.
    private static List<Line> sections(Path root, String from, String version, List<String> problems)
            throws IOException {
        List<String> rows = Files.readAllLines(root.resolve("CHANGELOG.md"), StandardCharsets.UTF_8);
        List<Line> lines = new ArrayList<>();
        String section = null;
        boolean found = false;
        boolean last = false;
        String heading = null;
        StringBuilder text = null;
        for (String row : rows) {
            if (row.startsWith("## ")) {
                add(lines, section, heading, text, problems);
                text = null;
                heading = null;
                if (last) {
                    break;
                }
                int end = row.indexOf(']');
                String name = row.startsWith("## [") && end > 4 ? row.substring(4, end) : "";
                found |= name.equals(version);
                section = found ? name : null;
                last = found && name.equals(from);
                continue;
            }
            if (section == null) {
                continue;
            }
            if (text != null && row.startsWith("  ") && !row.isBlank()) {
                text.append(' ').append(row.trim());
                continue;
            }
            add(lines, section, heading, text, problems);
            text = null;
            if (row.startsWith("### ")) {
                heading = row.substring(4).trim();
                if (!KINDS.containsKey(heading)) {
                    problems.add("CHANGELOG.md: \"### " + heading + "\" is none of " + KINDS.keySet());
                }
            } else if (row.startsWith("- ")) {
                text = new StringBuilder(row.substring(2));
            }
        }
        add(lines, section, heading, text, problems);
        if (!found) {
            problems.add("CHANGELOG.md has no section ## [" + version + "]");
        } else if (!last) {
            problems.add("TourSteps.FROM " + from + " is neither " + version + " nor a section below it");
        }
        return lines;
    }

    private static void add(List<Line> lines, String section, String heading, StringBuilder text,
            List<String> problems) {
        if (text == null) {
            return;
        }
        String plain = text.toString().replace("**", "").replace("`", "").replaceAll("\\s+", " ").trim();
        if (heading == null) {
            problems.add("CHANGELOG.md: \"" + plain + "\" sits under no heading");
        } else if (KINDS.containsKey(heading)) {
            lines.add(new Line(section, KINDS.get(heading), plain));
        }
    }
}
