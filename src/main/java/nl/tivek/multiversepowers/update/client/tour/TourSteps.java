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
    static final String VERSION = "0.8.0-alpha";
    // The oldest version whose changes this tour shows: normally its own.
    static final String FROM = VERSION;

    // Green Lantern's new fists, clicks and weapons by his rows of the panel, then the hands and the mech; the escape game
    // and wild helpers in the middle; Thor's flight by his rows. As anyone else, the card stands in the middle.
    static final List<TourStep> ALL = List.of(
            step(VERSION, "lantern_fists", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Construct Fists"),
            step(VERSION, "lantern_clicks", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: right click is the bolt",
                            "Green Lantern: the Light Dome is held", "Green Lantern: the Light Shield"),
            step(VERSION, "lantern_axe", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Battleaxe in the Construct Wheel"),
            step(VERSION, "lantern_saw", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Heavy Chainsaw in the Construct Wheel"),
            step(VERSION, "hands_random", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: the Giant Hands call up 1 to 5"),
            step(VERSION, "hand_gauntlet", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: the snapping Giant Hand"),
            step(VERSION, "lantern_recharge", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: to recharge you press",
                            "Green Lantern: the Power Battery looks like"),
            step(VERSION, "mech_combo", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: the mech's left-click combo"),
            step(VERSION, "mech_armor", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: in the mech the pilot is guarded"),
            step(VERSION, "escape_game", Kind.NEW, Place.GAME).covers("Escape game: caught in"),
            step(VERSION, "wild_helpers", Kind.NEW, Place.GAME).covers("Hard-Light Helpers also come wild"),
            step(VERSION, "thor_descend", Kind.NEW, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: hold C in flight"));

    // Changes too small for a step of their own, each by how its changelog line starts, as in `covers`.
    static final List<String> SMALL = List.of(
            "Crosshairs: Green Lantern, Thor",
            "Green Lantern's settings: the fists'",
            "Green Lantern: the ram cone does",
            "Green Lantern: in flight the Construct Wheel",
            "Green Lantern: the mech's head is bigger",
            "Green Lantern: the mech's missiles have",
            "Green Lantern: for the mech's flamethrower",
            "Green Lantern: a creature the Beam kills",
            "Thor: the Lightning Bomb is about",
            "Thor: the Storm Bolt no longer",
            "Green Lantern: the roar of the mech's");

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
