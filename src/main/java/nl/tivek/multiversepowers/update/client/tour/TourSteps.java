package nl.tivek.multiversepowers.update.client.tour;

import static nl.tivek.multiversepowers.update.client.tour.TourStep.step;

import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.docock.client.TentacleHud;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Place;

// What the tour shows, in the order it shows it: where the player is first, then on to the place of the next step.
// Every release with something new that can be pointed at adds its steps here, under its own version, with their words
// in en_us.json.
final class TourSteps {
    private static final Supplier<Component> GUIDE_KEY = () -> AbilityGuide.KEY.isUnbound() ? null
            : AbilityGuide.KEY.getTranslatedKeyMessage();

    static final List<TourStep> ALL = List.of(
            step("0.7.2-alpha", "updates_button", Place.MENU).at("title.updates", "pause.updates")
                    .leads(Place.MANAGER),
            step("0.7.2-alpha", "manager_pages", Place.MANAGER).at("nav"),
            step("0.7.2-alpha", "manager_news", Place.MANAGER).at("manager.news"),
            step("0.7.2-alpha", "manager_reports", Place.MANAGER).at("nav.reports"),
            step("0.7.2-alpha", "tour_replay", Place.MANAGER).at("nav.tour"),
            step("0.7.0-alpha", "versions", Place.MANAGER).at("nav.versions").leads(Place.VERSIONS),
            step("0.7.1-alpha", "release_time", Place.VERSIONS).at("versions.list"),
            step("0.7.0-alpha", "versions_switch", Place.VERSIONS).at("versions.buttons"),
            step("0.7.0-alpha", "panel", Place.GAME).at("game.panel").prepare(AbilityPanel::wake),
            step("0.7.2-alpha", "tentacles", Place.GAME).at(TentacleHud.ANCHOR).when(TentacleHud::shown),
            step("0.7.0-alpha", "guide_open", Place.GAME).at("game.panel.guide", "game.panel").leads(Place.GUIDE)
                    .prepare(AbilityPanel::wake).key(GUIDE_KEY),
            step("0.7.0-alpha", "guide_summary", Place.GUIDE).at("guide.summary"),
            step("0.7.0-alpha", "guide_bind", Place.GUIDE).at("guide.list").until("guide.control"),
            step("0.7.2-alpha", "guide_pairs", Place.GUIDE).at("guide.pair")
                    .prepare(() -> AbilityGuide.reveal(AbilityGuide.PAIR)).when(AbilityGuide::grouped),
            step("0.7.2-alpha", "guide_how", Place.GUIDE).at("guide.how").until("guide.overview")
                    .prepare(() -> AbilityGuide.reveal(AbilityGuide.HOW)));

    private TourSteps() {
    }

    @Nullable
    static TourStep find(String id) {
        for (TourStep step : ALL) {
            if (step.id().equals(id)) {
                return step;
            }
        }
        return null;
    }
}
