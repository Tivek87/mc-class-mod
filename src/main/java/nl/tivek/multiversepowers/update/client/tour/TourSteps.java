package nl.tivek.multiversepowers.update.client.tour;

import static nl.tivek.multiversepowers.update.client.tour.TourStep.step;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Kind;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Place;

// What the tour shows, in the order it shows it: where the player is first, then on to the place of the next step.
// Every release puts its steps here with their words in en_us.json, short: each big change of the CHANGELOG.md
// sections from `FROM` up to its own (`VERSION`, its `mod_version`) named in a step's `covers`, whose kind and version
// are those of the first line it covers, and where it helps, steps on how things work. A small change is named in
// `SMALL` instead; a fix or a Project line (nothing in the game) needs neither. `TourCoverageTest` fails the build
// while a line is in neither, or a tour text is too long. Only steps from `FROM` up to the version installed are ever
// shown, so a release's steps replace the last one's. A player's first tour starts with `INTRO`.
final class TourSteps {
    static final String VERSION = "0.8.1-alpha";
    // The oldest version whose changes this tour shows: normally its own.
    static final String FROM = VERSION;

    // Green Lantern's new guns, the Cosmos Test, the hands' chances, the gloves and the weapons' feel by his rows of
    // the panel; the fixes in the middle; the new settings in the settings window. As anyone else, the card stands in the middle.
    static final List<TourStep> ALL = List.of(
            step(VERSION, "lantern_revolvers", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Dual Revolvers in the Construct Wheel"),
            step(VERSION, "lantern_cannon", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Arm Cannon in the Construct Wheel"),
            step(VERSION, "lantern_minigun", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Minigun in the Construct Wheel"),
            step(VERSION, "hand_cosmos", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Cosmos Test, a new Giant Hands move"),
            step(VERSION, "hands_even", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: every Giant Hands move has"),
            step(VERSION, "lantern_gloves", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: the Construct Fists' gloves"),
            step(VERSION, "gun_feel", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers(
                            "Green Lantern: the new guns' held moves",
                            "Green Lantern: the weapons breathe at rest"),
            step(VERSION, "fixes", Kind.FIXED, Place.GAME).covers(
                            "A player made a statue by the Giant Hands",
                            "Green Lantern: the Giant Hands move with the highest chance"),
            step(VERSION, "lantern_settings", Kind.NEW, Place.SETTINGS).at("nav.green_lantern").covers(
                            "Green Lantern's settings: the revolvers'",
                            "Settings: the Giant Hands' chances"));

    // Changes too small for a step of their own, each by how its changelog line starts, as in `covers`.
    static final List<String> SMALL = List.of();

    // Shown first on a player's first tour, wherever it starts: what the tour is and how to use it.
    static final TourStep INTRO = step(VERSION, "tour_intro", Kind.HOW, Place.ANY);

    // Shown first while a newer version is out: update to it, and its own tour shows what it brings.
    static final TourStep UPDATE_FIRST = step(VERSION, "update_first", Kind.HOW, Place.MANAGER)
            .at("versions.buttons", "manager.status");

    // No steps of their own: the card saying how to go on while the next steps wait in a world, or for a character.
    static final TourStep JOIN_WORLD = step(VERSION, "join_world", Kind.HOW, Place.MENU);
    static final TourStep PICK_CHARACTER = step(VERSION, "pick_character", Kind.HOW, Place.GAME)
            .key(key(() -> AbilityKeys.SPELL_WHEEL));

    private TourSteps() {
    }

    @Nullable
    static TourStep find(String id) {
        if (id.equals(UPDATE_FIRST.id())) {
            return UPDATE_FIRST;
        }
        if (id.equals(INTRO.id())) {
            return INTRO;
        }
        for (TourStep step : ALL) {
            if (step.id().equals(id)) {
                return step;
            }
        }
        return null;
    }

    // A key's name for the cap before a prompt, or null while it is not bound; looked up only when drawn, so the steps
    // load without the game (`TourCoverageTest`).
    private static Supplier<Component> key(Supplier<KeyMapping> key) {
        return () -> key.get().isUnbound() ? null : key.get().getTranslatedKeyMessage();
    }
}
