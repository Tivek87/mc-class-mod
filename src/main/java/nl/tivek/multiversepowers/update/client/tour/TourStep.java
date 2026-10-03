package nl.tivek.multiversepowers.update.client.tour;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.update.client.ManagerScreen;

// One thing the tour shows: the version it came in, where it is (`place`), what it points at (`targets`, anchors of
// `ScreenAnchors`, the first one drawn; none for a card in the middle), and what the player may do to go on (`until`, an
// anchor that shows once they did it). A step that `leads` to a place is done by going there, which Next does for them.
// `prepare` runs every frame it is up (to wake what it points at), `available` leaves it out when it has nothing to
// show. Its words are `tour.<mod>.<id>.title`, `.text` and `.prompt`, with `key` drawn as a key cap before the prompt.
record TourStep(String version, String id, Place place, List<String> targets, @Nullable String until,
        @Nullable Place leads, @Nullable Runnable prepare, @Nullable BooleanSupplier available,
        @Nullable Supplier<Component> key) {
    private static final String PREFIX = "tour." + MultiversePowers.MODID + ".";

    enum Place {
        // The title screen, or the pause menu in a world.
        MENU(null), MANAGER("manager"), VERSIONS("versions"), GAME(null), GUIDE("guide");

        // What the place's screen reports while it is drawn.
        @Nullable
        final String anchor;

        Place(@Nullable String anchor) {
            this.anchor = anchor;
        }

        @Nullable
        static Place now() {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.screen instanceof TitleScreen
                    || minecraft.screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
                return MENU;
            }
            for (Place place : values()) {
                if (place.anchor != null && ScreenAnchors.shown(place.anchor)) {
                    return place;
                }
            }
            return minecraft.screen == null && minecraft.level != null && minecraft.player != null ? GAME : null;
        }

        // Whether the tour can take the player here: what is in game needs a world and a character to show.
        boolean reachable() {
            Minecraft minecraft = Minecraft.getInstance();
            return switch (this) {
                case MENU, MANAGER, VERSIONS -> true;
                case GAME, GUIDE -> minecraft.level != null && minecraft.player != null
                        && ClientCharacter.active() != null;
            };
        }

        void go() {
            Minecraft minecraft = Minecraft.getInstance();
            Screen home = ManagerScreen.rootOf(minecraft.screen);
            switch (this) {
                case MENU -> {
                    if (minecraft.level == null) {
                        minecraft.setScreen(home instanceof TitleScreen title ? title : new TitleScreen());
                    } else {
                        minecraft.setScreen(null);
                        minecraft.pauseGame(false);
                    }
                }
                case MANAGER -> ManagerScreen.open(home, ManagerScreen.UPDATES);
                case VERSIONS -> ManagerScreen.open(home, ManagerScreen.VERSIONS);
                case GAME -> minecraft.setScreen(null);
                case GUIDE -> AbilityGuide.open();
            }
        }
    }

    static TourStep step(String version, String id, Place place) {
        return new TourStep(version, id, place, List.of(), null, null, null, null, null);
    }

    TourStep at(String... targets) {
        return new TourStep(this.version, this.id, this.place, List.of(targets), this.until, this.leads,
                this.prepare, this.available, this.key);
    }

    TourStep until(String anchor) {
        return new TourStep(this.version, this.id, this.place, this.targets, anchor, this.leads, this.prepare,
                this.available, this.key);
    }

    TourStep leads(Place to) {
        return new TourStep(this.version, this.id, this.place, this.targets, to.anchor, to, this.prepare,
                this.available, this.key);
    }

    TourStep prepare(Runnable prepare) {
        return new TourStep(this.version, this.id, this.place, this.targets, this.until, this.leads, prepare,
                this.available, this.key);
    }

    TourStep when(BooleanSupplier available) {
        return new TourStep(this.version, this.id, this.place, this.targets, this.until, this.leads, this.prepare,
                available, this.key);
    }

    TourStep key(Supplier<Component> key) {
        return new TourStep(this.version, this.id, this.place, this.targets, this.until, this.leads, this.prepare,
                this.available, key);
    }

    boolean points() {
        return !this.targets.isEmpty();
    }

    @Nullable
    ScreenAnchors.Rect target() {
        for (String target : this.targets) {
            ScreenAnchors.Rect rect = ScreenAnchors.get(target);
            if (rect != null) {
                return rect;
            }
        }
        return null;
    }

    Component title() {
        return Component.translatable(PREFIX + this.id + ".title");
    }

    Component text() {
        return Component.translatable(PREFIX + this.id + ".text");
    }

    @Nullable
    Component prompt() {
        return this.until == null ? null : Component.translatable(PREFIX + this.id + ".prompt");
    }

    // The key the prompt asks for, or null while it has none or it is not bound.
    @Nullable
    Component keyName() {
        return this.key == null ? null : this.key.get();
    }

    String shortVersion() {
        return bare(this.version);
    }

    // A version without its suffix, as the tour shows it.
    static String bare(String version) {
        int dash = version.indexOf('-');
        return dash < 0 ? version : version.substring(0, dash);
    }
}
