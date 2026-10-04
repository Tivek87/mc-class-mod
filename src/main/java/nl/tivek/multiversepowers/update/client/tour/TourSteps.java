package nl.tivek.multiversepowers.update.client.tour;

import static nl.tivek.multiversepowers.update.client.tour.TourStep.step;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.docock.client.TentacleHud;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Kind;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Place;

// What the tour shows, in the order it shows it: where the player is first, then on to the place of the next step.
// Every release adds its steps here, under its own version, with their words in en_us.json: a step for every change
// its changelog names (everything since the last release), and where it helps, steps on how things work. Only the
// steps of the version installed are ever shown, so a release's steps replace the last one's.
final class TourSteps {
    private static final String VERSION = "0.7.4-alpha";

    static final List<TourStep> ALL = List.of(
            step(VERSION, "updates_button", Kind.HOW, Place.MENU).at("title.updates", "pause.updates")
                    .leads(Place.MANAGER),
            step(VERSION, "manager_pages", Kind.CHANGED, Place.MANAGER).at("nav"),
            step(VERSION, "manager_status", Kind.CHANGED, Place.MANAGER).at("manager.status"),
            step(VERSION, "manager_news", Kind.CHANGED, Place.MANAGER).at("manager.news"),
            step(VERSION, "manager_versions", Kind.CHANGED, Place.MANAGER).at("tab.versions").leads(Place.VERSIONS),
            step(VERSION, "versions_list", Kind.CHANGED, Place.VERSIONS).at("versions.list"),
            step(VERSION, "versions_switch", Kind.HOW, Place.VERSIONS).at("versions.buttons"),
            step(VERSION, "version_tag", Kind.HOW, Place.VERSIONS).at("window.tag"),
            step(VERSION, "feedback_tab", Kind.HOW, Place.VERSIONS).at("nav.feedback").leads(Place.FEEDBACK),
            step(VERSION, "feedback_form", Kind.CHANGED, Place.FEEDBACK).at("report.form", "tab.bug"),
            step(VERSION, "feedback_submitted", Kind.CHANGED, Place.FEEDBACK).at("tab.reports").until("reports"),
            step(VERSION, "manager_tour", Kind.NEW, Place.FEEDBACK).at("nav.tour"),
            step(VERSION, "panel", Kind.CHANGED, Place.GAME).at("game.panel").prepare(AbilityPanel::wake),
            step(VERSION, "panel_rows", Kind.HOW, Place.GAME).at("game.panel.rows").prepare(AbilityPanel::wake),
            step(VERSION, "panel_power", Kind.HOW, Place.GAME).at("game.panel.foot").prepare(AbilityPanel::wake)
                    .when(() -> ClientCharacter.active() == GameCharacter.GREEN_LANTERN),
            step(VERSION, "tentacles", Kind.HOW, Place.GAME).at(TentacleHud.ANCHOR).when(TentacleHud::shown),
            step(VERSION, "guide_open", Kind.HOW, Place.GAME).at("game.panel.guide", "game.panel")
                    .leads(Place.GUIDE).prepare(AbilityPanel::wake).key(key(AbilityGuide.KEY)),
            step(VERSION, "guide_summary", Kind.HOW, Place.GUIDE).at("guide.summary"),
            step(VERSION, "guide_modes", Kind.HOW, Place.GUIDE).at("guide.modes")
                    .when(() -> ScreenAnchors.shown("guide.modes")),
            step(VERSION, "guide_bind", Kind.HOW, Place.GUIDE).at("guide.list").until("guide.control"),
            step(VERSION, "guide_detail", Kind.HOW, Place.GUIDE).at("guide.control")
                    .when(() -> ScreenAnchors.shown("guide.control")),
            step(VERSION, "guide_pairs", Kind.HOW, Place.GUIDE).at("guide.pair")
                    .prepare(() -> AbilityGuide.reveal(AbilityGuide.PAIR)).when(AbilityGuide::grouped),
            step(VERSION, "guide_how", Kind.HOW, Place.GUIDE).at("guide.how").until("guide.overview")
                    .prepare(() -> AbilityGuide.reveal(AbilityGuide.HOW)));

    // Shown first while a newer version is out: update to it, and its own tour shows what it brings.
    static final TourStep UPDATE_FIRST = step(UpdateChecker.installed(), "update_first", Kind.HOW, Place.MANAGER)
            .at("versions.buttons", "manager.status");

    // No steps of their own: the card saying how to go on while the next steps wait in a world, or for a character.
    static final TourStep JOIN_WORLD = step(UpdateChecker.installed(), "join_world", Kind.HOW, Place.MENU);
    static final TourStep PICK_CHARACTER = step(UpdateChecker.installed(), "pick_character", Kind.HOW, Place.GAME)
            .key(key(AbilityKeys.SPELL_WHEEL));

    private TourSteps() {
    }

    @Nullable
    static TourStep find(String id) {
        if (id.equals(UPDATE_FIRST.id())) {
            return UPDATE_FIRST;
        }
        for (TourStep step : ALL) {
            if (step.id().equals(id)) {
                return step;
            }
        }
        return null;
    }

    // A key's name for the cap before a prompt, or null while it is not bound.
    private static Supplier<Component> key(KeyMapping key) {
        return () -> key.isUnbound() ? null : key.getTranslatedKeyMessage();
    }
}
