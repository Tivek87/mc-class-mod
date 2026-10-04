package nl.tivek.multiversepowers.update.client.tour;

import static nl.tivek.multiversepowers.update.client.tour.TourStep.step;

import java.util.List;
import javax.annotation.Nullable;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Place;

// What the tour shows, in the order it shows it: where the player is first, then on to the place of the next step.
// Every release with something new that can be pointed at adds its steps here, under its own version, with their words
// in en_us.json; only the steps of the version installed are ever shown, so a release's steps replace the last one's.
final class TourSteps {
    static final List<TourStep> ALL = List.of(
            step("0.7.3-alpha", "updates_page", Place.MANAGER).at("manager"),
            step("0.7.3-alpha", "feedback_page", Place.MANAGER).at("nav.feedback").until("tab.bug"),
            step("0.7.3-alpha", "panel_corner", Place.GAME).at("game.panel").prepare(AbilityPanel::wake));

    // Shown first while a newer version is out: update to it, and its own tour shows what it brings.
    static final TourStep UPDATE_FIRST = step(UpdateChecker.installed(), "update_first", Place.MANAGER)
            .at("versions.buttons", "manager.status");

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
}
