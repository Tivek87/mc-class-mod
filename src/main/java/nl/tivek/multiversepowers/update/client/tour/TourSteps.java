package nl.tivek.multiversepowers.update.client.tour;

import static nl.tivek.multiversepowers.update.client.tour.TourStep.step;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.docock.client.TentacleHud;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Kind;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Place;

// What the tour shows, in the order it shows it: where the player is first, then on to the place of the next step.
// Every release puts its steps here with their words in en_us.json: each line of the CHANGELOG.md sections from `FROM`
// up to its own (`VERSION`, its `mod_version`; a Project line, nothing in the game, needs none) named in a step's
// `covers`, whose kind and version are those of the first line it covers, and where it helps, steps on how things work.
// `TourCoverageTest` fails the build while a line has no step. Only steps from `FROM` up to the version installed are
// ever shown, so a release's steps replace the last one's. A player's first tour starts with `INTRO`.
final class TourSteps {
    static final String VERSION = "0.7.4-alpha";
    // The oldest version whose changes this tour shows too: before 0.7.4 not every change had its step.
    static final String FROM = "0.6.6-alpha";

    static final List<TourStep> ALL = List.of(
            step("0.7.1-alpha", "updates_button", Kind.NEW, Place.MENU).at("title.updates", "pause.updates")
                    .leads(Place.MANAGER)
                    .covers("Title screen: a small arrow button", "Pause menu: the small arrow button"),
            step("0.7.3-alpha", "manager_pages", Kind.NEW, Place.MANAGER).at("nav")
                    .covers("Update manager: one window with three pages", "Update manager: marks in the sidebar",
                            "Update manager: What's new & versions, Report a bug",
                            "Update manager: the Updates page is calmer",
                            "Update manager: the YOURS, LATEST and INSTALLED chips"),
            step(VERSION, "manager_status", Kind.CHANGED, Place.MANAGER).at("manager.status")
                    .covers("Update manager: Update and Update on quit only show",
                            "Update manager: cleaner: one card says", "Tour: on an older version it first asks",
                            "Tour: the step asking you to update first", "Update screens: the popup",
                            "Update screens: panels, tags and download bars"),
            step(VERSION, "manager_news", Kind.CHANGED, Place.MANAGER).at("manager.news")
                    .covers("Update manager: the Updates page is calmer", "Update manager: versions read like",
                            "What's new & versions: each version shows the time"),
            step(VERSION, "manager_versions", Kind.CHANGED, Place.MANAGER).at("tab.versions").leads(Place.VERSIONS)
                    .covers("Update manager: the Updates page is calmer",
                            "Update manager: What's new is now What's new & versions"),
            step("0.7.0-alpha", "versions_list", Kind.NEW, Place.VERSIONS).at("versions.list")
                    .covers("What's new & versions: the last 10 versions",
                            "Update manager: Updates now lists the last ten versions",
                            "Update manager: versions read like", "What's new & versions: a roomier list",
                            "Update manager: the YOURS, LATEST and INSTALLED chips"),
            step("0.7.0-alpha", "versions_switch", Kind.NEW, Place.VERSIONS).at("versions.buttons")
                    .covers("Joining: a server, LAN world or friend", "Test game: switching versions now works",
                            "What's new & versions: the Use button for an older version"),
            step("0.7.3-alpha", "version_tag", Kind.NEW, Place.VERSIONS).at("window.tag")
                    .covers("Update manager: click your version at the top"),
            step(VERSION, "feedback_tab", Kind.HOW, Place.VERSIONS).at("nav.feedback").leads(Place.FEEDBACK),
            step("0.7.3-alpha", "feedback_form", Kind.NEW, Place.FEEDBACK).at("report.form", "tab.bug")
                    .covers("Feedback: one page with tabs", "Feedback: Sent is now Submitted"),
            step(VERSION, "feedback_submitted", Kind.CHANGED, Place.FEEDBACK).at("tab.reports").until("reports")
                    .covers("Feedback: Sent is now Submitted", "Feedback → Sent: Refresh"),
            step("0.7.3-alpha", "manager_tour", Kind.NEW, Place.FEEDBACK).at("nav.tour")
                    .covers("Tour: after an update a short tour asks", "Tour: says \"Nothing to show"),
            step(VERSION, "tour_more", Kind.NEW, Place.FEEDBACK).at("nav.tour")
                    .covers("Tour: this version's tour walks you through", "Tour: this version's tour also shows",
                            "Tour: the first time you see a tour", "Tour: each card's top says what it shows",
                            "Tour: when the next steps are in a world"),
            step(VERSION, "tour_cards", Kind.CHANGED, Place.FEEDBACK).at("nav.tour")
                    .covers("Tour: the first card counts", "Tour: a step whose thing does not show up",
                            "Tour: clearer words", "Tour: pressing Next without doing anything"),
            step(VERSION, "panel", Kind.CHANGED, Place.GAME).at("game.panel").prepare(AbilityPanel::wake)
                    .covers("Ability panel: gone 3 seconds", "Ability panel: fades in when you use or try",
                            "Ability panel: always in the bottom right corner",
                            "Ability panel: drawn at your GUI scale", "Ability panel: smaller, and smaller still"),
            step("0.7.0-alpha", "panel_rows", Kind.CHANGED, Place.GAME).at("game.panel.rows")
                    .prepare(AbilityPanel::wake)
                    .covers("Ability panel: cleaner and roomier", "Ability panel: lists every mouse, space and shift",
                            "Thor: his panel shows his moves", "Ability panel: a rounded card with key caps",
                            "Ability panel: nothing flashes or jumps"),
            step("0.6.9-alpha", "panel_ready", Kind.CHANGED, Place.GAME).at("game.panel.rows")
                    .prepare(AbilityPanel::wake)
                    .covers("Ability panel and guide: say ready only", "Ability panel: a green dot shows a ready move",
                            "Ability panel: the ultimate's countdown", "Ability panel: the green flash on use",
                            "Ability panel: a thin bar under a line", "Ability panel: Thor's and Doctor Octopus's"),
            step("0.6.7-alpha", "panel_names", Kind.CHANGED, Place.GAME).at("game.panel.rows")
                    .prepare(AbilityPanel::wake)
                    .covers("Green Lantern: his flight line says Stop Flying", "Thor: his panel says Put Away",
                            "Doctor Octopus: his stance key's line"),
            step("0.7.0-alpha", "panel_power", Kind.CHANGED, Place.GAME).at("game.panel.power")
                    .prepare(AbilityPanel::wake)
                    .covers("Green Lantern: the ring bar's drain", "Green Lantern: the seconds of flight left",
                            "Green Lantern: a weapon breaks up"),
            step("0.6.9-alpha", "ring_costs", Kind.NEW, Place.GAME).at("game.panel.power")
                    .prepare(AbilityPanel::wake)
                    .covers("Green Lantern: taking out a weapon costs", "Green Lantern: each sword cut and whip lash",
                            "Green Lantern: holding a Light Cage costs", "Green Lantern: piloting the mech costs",
                            "Green Lantern: taking off now costs", "Settings: every new cost is a setting"),
            step("0.6.7-alpha", "guide_open", Kind.NEW, Place.GAME).at("game.panel.guide", "game.panel")
                    .leads(Place.GUIDE).prepare(AbilityPanel::wake).key(key(() -> AbilityGuide.KEY))
                    .covers("Ability guide: press P while you play",
                            "Ability panel: its title line shows the guide's key"),
            step("0.7.0-alpha", "guide_summary", Kind.NEW, Place.GUIDE).at("guide.summary")
                    .covers("Ability guide: one short line about the character",
                            "Ability guide: a square Minecraft-style window"),
            step("0.6.9-alpha", "guide_modes", Kind.NEW, Place.GUIDE).at("guide.modes")
                    .covers("Ability guide: a tab for every mode", "Ability guide: a Modes section explains",
                            "Ability guide: the separate Modes section"),
            step("0.6.8-alpha", "guide_bind", Kind.CHANGED, Place.GUIDE).at("guide.list").until("guide.control")
                    .covers("Ability guide: redesigned as a window", "Ability guide: every key shows its cost"),
            step("0.6.8-alpha", "guide_detail", Kind.NEW, Place.GUIDE).at("guide.control", "guide.list")
                    .covers("Ability guide: each ability shows where it works",
                            "Ability guide: each move has one short line", "Abilities: every description is shorter",
                            "Settings: resting the pointer on an ability's title"),
            step("0.7.3-alpha", "guide_pairs", Kind.NEW, Place.GUIDE).at("guide.pair")
                    .prepare(() -> AbilityGuide.reveal(AbilityGuide.PAIR))
                    .covers("Ability guide: a move that only works with another"),
            step("0.7.3-alpha", "guide_how", Kind.NEW, Place.GUIDE).at("guide.how").until("guide.overview")
                    .prepare(() -> AbilityGuide.reveal(AbilityGuide.HOW))
                    .covers("Ability guide: each mode says how you get in", "Ability guide: \"Off meanwhile\" is gone"),
            step("0.7.3-alpha", "tentacles", Kind.NEW, Place.GAME).at(TentacleHud.ANCHOR)
                    .covers("Doctor Octopus: four corner marks"),
            step("0.7.3-alpha", "panel_hud", Kind.CHANGED, Place.GAME)
                    .covers("HUD: nothing overlaps any more", "Stamina bar: hidden with F1",
                            "Kill confirm: the sound is louder"),
            step("0.7.3-alpha", "thor_grab", Kind.NEW, Place.GAME)
                    .covers("Thor: after a grab you pick how it ends", "Thor: a grabbed creature hangs firmly",
                            "Thor: Grab-Dash Dive catches players too"),
            step("0.7.3-alpha", "thor_hammer", Kind.CHANGED, Place.GAME)
                    .covers("Thor: Mjolnir is bigger", "Thor: Mjolnir flies head first", "Thor: Mjolnir on his belt",
                            "Thor: Mjolnir no longer has see-through pixels"),
            step("0.7.3-alpha", "thor_blows", Kind.CHANGED, Place.GAME)
                    .covers("Thor: his blows are a little slower", "Thor: his kicks are clearly kicks",
                            "Thunderclap: the body no longer rocks back", "Thor: Sky Shockwave knocks every creature"),
            step("0.7.3-alpha", "lantern_light", Kind.CHANGED, Place.GAME)
                    .covers("Green Lantern: the dome is built", "Green Lantern: the beam's LOCKED sign",
                            "Green Lantern: holding towards Inferno", "Green Lantern: the flamethrower's gun",
                            "Green Lantern: his legs no longer fold up"),
            step("0.7.3-alpha", "mech", Kind.CHANGED, Place.GAME)
                    .covers("Mech: striking a blow while walking", "Mech: its legs no longer trail behind",
                            "Mech: no longer gets stuck", "Filmed moments: your own first-person arms"),
            step("0.6.9-alpha", "knockdowns", Kind.NEW, Place.GAME)
                    .covers("Knockdowns: players can be knocked down too", "Players thrown limp: a blow or blast",
                            "Players thrown limp: still in the air"),
            step("0.7.3-alpha", "bodies", Kind.CHANGED, Place.GAME)
                    .covers("Hands and feet: in poses", "Held creatures: blows no longer wear them out"),
            step("0.7.3-alpha", "host_tools", Kind.NEW, Place.GAME)
                    .covers("Test fight: the host or an operator", "Developer test hand: its fingers curl"));

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
