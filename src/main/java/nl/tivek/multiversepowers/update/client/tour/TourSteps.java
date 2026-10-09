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
    static final String VERSION = "0.7.8-alpha";
    // The oldest version whose changes this tour shows: normally its own.
    static final String FROM = VERSION;

    // The rebuilt mech and its new moves by Green Lantern's rows of the panel, the new Storm Throw and the hammer's
    // hold on Thor's flight by his: as anyone else, the card stands in the middle.
    static final List<TourStep> ALL = List.of(
            step(VERSION, "mech_jaeger", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: the mech is built like a jaeger"),
            step(VERSION, "mech_combo", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: the mech's blows are a combo",
                            "Green Lantern: the mech's blows reach further"),
            step(VERSION, "mech_eyes", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Eye Ray", "Green Lantern: Eye Beam",
                            "Green Lantern: the mech's Unibeam"),
            step(VERSION, "mech_missiles", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Missile arm"),
            step(VERSION, "mech_rockets", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Rocket boots"),
            step(VERSION, "mech_spin", Kind.NEW, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: Spin"),
            step(VERSION, "mech_exhaust", Kind.CHANGED, Place.GAME).at("game.panel.rows.green_lantern")
                    .prepare(AbilityPanel::wake).covers("Green Lantern: the mech's exhaust pipes"),
            step(VERSION, "thor_storm_throw", Kind.CHANGED, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: Storm Throw", "Thor: the Storm Throw's ring"),
            step(VERSION, "thor_hammer_flight", Kind.CHANGED, Place.GAME).at("game.panel.rows.thor")
                    .prepare(AbilityPanel::wake).covers("Thor: he flies only with the hammer"));

    // Changes too small for a step of their own, each by how its changelog line starts, as in `covers`.
    static final List<String> SMALL = List.of(
            "Green Lantern's settings: the straight right's damage",
            "Green Lantern: the mech runs steadier",
            "Green Lantern: the mech's moves have waits",
            "Green Lantern's settings: the mech's blows hit harder",
            "Ability panel and guide: in the mech",
            "Thor's settings: the Storm Throw's spread",
            "Thor's settings: the Storm Throw hits harder");

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
