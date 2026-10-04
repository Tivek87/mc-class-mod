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
    static final String VERSION = "0.7.7-alpha";
    // The oldest version whose changes this tour shows: normally its own.
    static final String FROM = VERSION;

    // Thor's new ways with his hammer and his leaping bolts, then the mech's beams, each by its character's rows of the
    // panel: as anyone else, the card stands in the middle.
    static final List<TourStep> ALL = List.of(
            step(VERSION, "thor_throw_follow", Kind.CHANGED, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: Throw and Follow"),
            step(VERSION, "thor_throw_stay", Kind.NEW, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: Throw to Stay"),
            step(VERSION, "thor_hammer_call", Kind.NEW, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake)
                    .covers("Thor: Call the Hammer", "Thor: with the hammer away, hold jump"),
            step(VERSION, "thor_hammer_follow", Kind.NEW, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: Follow the Hammer"),
            step(VERSION, "thor_storm_throw", Kind.NEW, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: Storm Throw"),
            step(VERSION, "thor_chain", Kind.CHANGED, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: a bolt called from the Thunderstorm"),
            step(VERSION, "mech_beams", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Eye Beam", "Green Lantern: Unibeam"));

    // Changes too small for a step of their own, each by how its changelog line starts, as in `covers`.
    static final List<String> SMALL = List.of(
            "Thor's settings: how far a throw flies",
            "Green Lantern: the mech has exhaust pipes",
            "Green Lantern's settings: the Eye Beam's",
            "Settings: Voice lines",
            "Thor: his thrown hammer hits each creature",
            "Thor: while a Storm Throw is out",
            "Thor: the thunder ring round the crosshair",
            "Players go limp: far less often",
            "Tour: asks once a version",
            "Thor: every bolt of his is the mod's own",
            "Thor: the Lightning Bomb charges faster",
            "Thor: the storm's clouds tower",
            "Thor: a creature he throws from his grab",
            "Green Lantern: the ring's voice",
            "Ability panel: in the mech it lists");

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
