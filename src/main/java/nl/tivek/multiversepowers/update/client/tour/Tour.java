package nl.tivek.multiversepowers.update.client.tour;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.update.client.ManagerScreen;
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Place;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

// After an update, a tour shows every big change of the versions it covers (`TourSteps.FROM` up to the one installed)
// right where it is (in the menus, the update manager, the settings, in game at the panel and in the ability guide),
// with steps on how things work where they help; a player's first tour starts by saying what the tour is, and never
// again. It asks once a version, on the title screen (from its first frame, while the loading screen still fades) or
// in the pause menu, in game with a pill when the player got there first, after that only the update manager plays
// it; while a newer version is out it asks to update to that one first;
// Next goes on (taking the player to the next place itself where it can), Back goes back.
// While the next steps wait in a world or for a character, a card says how to get there (from the title screen or the
// pause menu with a button that takes the player on, in game with the key to press); elsewhere a pill says what waits.
// How far it got is kept in config/welcomescreen/tour.json; a first install gets no tour, as everything is new to it
// anyway. The update manager's Tour plays it again.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Tour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "tour.json";
    // In game Enter held this long skips the tour; its key cap fills after the first moment.
    private static final long HOLD_MS = 1200L;
    private static final long HOLD_SHOWN_MS = 200L;
    // In game the pill fades after a while, so it never stays in the way.
    private static final long PILL_MS = 8000L;
    private static final long PILL_FADE_MS = 600L;
    // What a step points at may show a moment after the step comes up; past this its card stands on its own.
    private static final long MISSING_MS = 600L;

    private static boolean ranBefore;
    private static boolean loaded;
    // The newest version whose steps were all shown or skipped.
    private static String seen = "";
    // The newest version whose tour was offered: it is offered once, after that only the update manager plays it.
    private static String asked = "";
    // Whether a tour this player went through began with the card on what the tour is.
    private static boolean introduced;
    private static boolean started;
    private static final Set<String> done = new LinkedHashSet<>();
    // Steps passed over for good, having nothing to show: counted neither as done nor as waiting.
    private static final Set<String> passed = new LinkedHashSet<>();
    // This session only: the steps done in order (for Back), whether the first ask was put off, and which card saying
    // how to go on was.
    private static final Deque<String> history = new ArrayDeque<>();
    private static boolean later;
    @Nullable
    private static TourStep putOff;
    private static long missingSince = -1L;
    @Nullable
    private static TourStep shown;
    // Whether the shown step's `until` was away while it was up: only its coming after that counts.
    private static boolean untilGone;
    private static long enterAt = -1L;
    private static long pillSince = -1L;

    private Tour() {
    }

    // Called before the client settings file is made: whether this game has run the mod before.
    public static void ranBefore(boolean ran) {
        ranBefore = ran;
    }

    // The update manager's Tour: the tour waiting, or else every step of this version again, from the manager's first
    // page; a version with nothing to show says so.
    public static void replay() {
        load();
        if (pending().isEmpty()) {
            seen = "";
            done.clear();
            passed.clear();
        }
        if (pending().isEmpty()) {
            seen = UpdateChecker.installed();
            TourOverlay.nothing();
            return;
        }
        history.clear();
        started = true;
        later = false;
        shown = null;
        save();
        if (Place.now() != Place.MANAGER) {
            Place.MANAGER.go();
        }
    }

    // Whether a tour of what is new waits to be shown or finished.
    public static boolean waiting() {
        load();
        return !pending().isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRenderScreen(ScreenEvent.Render.Post event) {
        Place place = Place.now();
        // The update manager's other pages show only the pill.
        if (place == null && !ScreenAnchors.shown("window")) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 500.0F);
        frame(graphics, place, true, event.getMouseX(), event.getMouseY());
        graphics.pose().popPose();
    }

    // The title screen as the loading screen fades off it, drawn without screen events: the first ask comes with it.
    public static void underOverlay(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 500.0F);
        frame(graphics, Place.MENU, true, -1.0, -1.0);
        graphics.pose().popPose();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || Place.now() != Place.GAME) {
            return;
        }
        load();
        if ((pending().isEmpty() || !started && later) && !TourOverlay.busy()) {
            return;
        }
        // Every HUD layer stands further forward than the last: start a clean depth so the tour lies over all of them.
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        frame(graphics, Place.GAME, false, -1.0, -1.0);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        TourOverlay.Hit hit = TourOverlay.hit(event.getMouseX(), event.getMouseY());
        if (hit == TourOverlay.Hit.NONE) {
            return;
        }
        if (event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            switch (hit) {
                case SHOW -> start();
                case UPDATE -> update();
                case LATER -> later();
                case SKIP -> skip();
                case NEXT -> next();
                case BACK -> back();
                case CONTINUE -> resume();
                case GO -> go();
                case DISMISS -> wayLater();
                default -> {
                }
            }
        }
        event.setCanceled(true);
    }

    // Enter goes on and Backspace back, while no text field needs the key.
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        int key = event.getKeyCode();
        if (typing(event.getScreen(), key)) {
            return;
        }
        if (enter(key) && TourOverlay.introUp()) {
            start();
        } else if (enter(key) && TourOverlay.stepUp()) {
            next();
        } else if (enter(key) && TourOverlay.wayUp()) {
            go();
        } else if (key == GLFW.GLFW_KEY_BACKSPACE && TourOverlay.stepUp() && canBack()) {
            back();
        } else {
            return;
        }
        event.setCanceled(true);
    }

    // In game the mouse steers the camera: Enter goes on (held, it skips the tour) and Backspace goes back.
    @SubscribeEvent
    static void onKey(InputEvent.Key event) {
        if (Minecraft.getInstance().screen != null) {
            enterAt = -1L;
            return;
        }
        int key = event.getKey();
        if (enter(key) && event.getAction() == GLFW.GLFW_PRESS
                && (TourOverlay.stepUp() || TourOverlay.pillUp() || TourOverlay.wayUp())) {
            enterAt = Util.getMillis();
        } else if (enter(key) && event.getAction() == GLFW.GLFW_RELEASE && enterAt >= 0L) {
            enterAt = -1L;
            if (TourOverlay.stepUp()) {
                next();
            } else if (TourOverlay.pillUp()) {
                resume();
            } else if (TourOverlay.wayUp()) {
                wayLater();
            }
        } else if (key == GLFW.GLFW_KEY_BACKSPACE && event.getAction() == GLFW.GLFW_PRESS && TourOverlay.stepUp()
                && canBack()) {
            back();
        }
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        TourOverlay.tick();
        if (enterAt >= 0L && Util.getMillis() - enterAt >= HOLD_MS) {
            enterAt = -1L;
            skip();
        }
    }

    private static boolean enter(int key) {
        return key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER;
    }

    // A one-line field on the update manager's pages has no use for Enter (the bug form focuses its name by itself).
    private static boolean typing(Screen screen, int key) {
        if (screen.getFocused() instanceof MultiLineEditBox) {
            return true;
        }
        return screen.getFocused() instanceof EditBox && !(enter(key) && screen instanceof ManagerScreen);
    }

    // `mouse`: a screen is open, so the card has buttons rather than keys.
    private static void frame(GuiGraphics graphics, @Nullable Place place, boolean mouse, double mouseX,
            double mouseY) {
        load();
        show(graphics, place, mouse, mouseX, mouseY);
        TourOverlay.extras(graphics);
    }

    private static void show(GuiGraphics graphics, @Nullable Place place, boolean mouse, double mouseX,
            double mouseY) {
        List<TourStep> pending = pending();
        if (pending.isEmpty()) {
            return;
        }
        if (!started) {
            if (place == Place.MENU && !later) {
                List<TourStep> news = pending.stream().filter(step -> step != TourSteps.UPDATE_FIRST).toList();
                List<TourStep> changes = news.stream().filter(TourStep::change).toList();
                String newer = UpdateChecker.newerVersion();
                String installed = UpdateChecker.installed();
                TourOverlay.intro(graphics, TourStep.bare(installed),
                        UpdateChecker.compare(TourSteps.FROM, installed) < 0 ? TourStep.bare(TourSteps.FROM) : null,
                        changes.size(), news.size() - changes.size(),
                        (changes.isEmpty() ? news : changes).stream().map(TourStep::title).toList(),
                        newer == null ? null : TourStep.bare(newer), mouseX, mouseY);
                offered();
            } else if (place == Place.GAME && !later) {
                // In game before any menu asked (a world joined from the launcher): a pill, Enter starts the tour.
                pill(graphics, mouse, mouseX, mouseY);
                offered();
            }
            return;
        }
        if (shown != null && shown.until() != null) {
            if (!ScreenAnchors.shown(shown.until())) {
                untilGone = true;
            } else if (untilGone) {
                complete(shown, true);
                pending = pending();
                if (pending.isEmpty()) {
                    return;
                }
            }
        }
        TourStep step = place == null ? null : current(place, pending);
        if (step == null) {
            TourStep way = waypoint(place);
            if (way != null) {
                TourOverlay.waypoint(graphics, way, mouse, hold(), mouseX, mouseY);
            } else {
                pill(graphics, mouse, mouseX, mouseY);
            }
            return;
        }
        pillSince = -1L;
        if (step.prepare() != null) {
            step.prepare().run();
        }
        if (step != shown) {
            shown = step;
            untilGone = step.until() == null || !ScreenAnchors.shown(step.until());
            missingSince = -1L;
        }
        ScreenAnchors.Rect target = step.target();
        if (step.points() && target == null) {
            long now = Util.getMillis();
            missingSince = missingSince < 0L ? now : missingSince;
            if (now - missingSince < MISSING_MS) {
                return;
            }
        } else {
            missingSince = -1L;
        }
        pending = pending();
        TourOverlay.step(graphics, step, target, done.size() + 1, done.size() + pending.size(), mouse, canBack(),
                nextLabel(step), hold(), mouseX, mouseY);
    }

    // The first step waiting here; one with nothing to show is passed over for good. In game they wait for a character.
    @Nullable
    private static TourStep current(Place place, List<TourStep> pending) {
        if (!place.reachable()) {
            return null;
        }
        for (TourStep step : pending) {
            if (step.place() != place && step.place() != Place.ANY) {
                continue;
            }
            if (step.available() != null && !step.available().getAsBoolean()) {
                passed.add(step.id());
                save();
                continue;
            }
            return step;
        }
        if (pending().isEmpty()) {
            finish(true);
        }
        return null;
    }

    // While steps wait in a world or for a character, the card saying how to get there: on the title screen (join a
    // world) and in the pause menu (pick a character) once they come next, in game (pick a character) whenever some
    // wait there; null where the pill says it instead, or once the player put that card off.
    @Nullable
    private static TourStep waypoint(@Nullable Place place) {
        List<TourStep> pending = pending();
        if (pending.stream().allMatch(step -> step.place().reachable())) {
            return null;
        }
        boolean next = !pending.get(0).place().reachable();
        TourStep way;
        if (Minecraft.getInstance().level == null) {
            way = place == Place.MENU && next ? TourSteps.JOIN_WORLD : null;
        } else {
            way = place == Place.GAME || place == Place.MENU && next ? TourSteps.PICK_CHARACTER : null;
        }
        return way == putOff ? null : way;
    }

    // Nothing waits here: a pill says how many wait elsewhere and takes the player there when it can. In game it
    // fades after a while, and shows only while it can take them.
    private static void pill(GuiGraphics graphics, boolean mouse, double mouseX, double mouseY) {
        List<TourStep> pending = pending();
        if (pending.isEmpty()) {
            return;
        }
        boolean reachable = !started || pending.get(0).place().reachable();
        float alpha = 1.0F;
        if (!mouse) {
            long now = Util.getMillis();
            if (pillSince < 0L) {
                pillSince = now;
            }
            alpha = Mth.clamp((PILL_MS - (now - pillSince)) / (float) PILL_FADE_MS, 0.0F, 1.0F);
            if (!reachable || alpha <= 0.0F) {
                return;
            }
        }
        if (!started) {
            TourOverlay.pill(graphics, Component.translatable(TourCard.PREFIX + "pill.ask",
                    TourStep.bare(UpdateChecker.installed())),
                    Component.translatable(TourCard.PREFIX + "pill.ask.short"), true, mouse, alpha, mouseX, mouseY);
            return;
        }
        String why = reachable ? "pill" : Minecraft.getInstance().level == null ? "pill.world" : "pill.character";
        TourOverlay.pill(graphics, Component.translatable(TourCard.PREFIX + why, pending.size()),
                Component.translatable(TourCard.PREFIX + "pill.short", pending.size()), reachable, mouse, alpha,
                mouseX, mouseY);
    }

    // How far a held Enter is to skipping the tour.
    private static float hold() {
        if (enterAt < 0L) {
            return 0.0F;
        }
        return Mth.clamp((Util.getMillis() - enterAt - HOLD_SHOWN_MS) / (float) (HOLD_MS - HOLD_SHOWN_MS), 0.0F,
                1.0F);
    }

    // The steps of the versions the tour covers not yet done, while its tour is not over; first, while a newer version
    // is out, the one asking to update to it, and before that on a player's first tour the card on what the tour is.
    private static List<TourStep> pending() {
        List<TourStep> pending = new ArrayList<>();
        String installed = UpdateChecker.installed();
        if (UpdateChecker.compare(installed, seen) <= 0) {
            return pending;
        }
        for (TourStep step : TourSteps.ALL) {
            if (UpdateChecker.compare(step.version(), TourSteps.FROM) >= 0
                    && UpdateChecker.compare(step.version(), installed) <= 0 && !done.contains(step.id())
                    && !passed.contains(step.id())) {
                pending.add(step);
            }
        }
        if (!pending.isEmpty() && UpdateChecker.newerVersion() != null
                && !done.contains(TourSteps.UPDATE_FIRST.id())) {
            pending.add(0, TourSteps.UPDATE_FIRST);
        }
        if (!pending.isEmpty() && !introduced && !done.contains(TourSteps.INTRO.id())) {
            pending.add(0, TourSteps.INTRO);
        }
        return pending;
    }

    // Show me: the steps here, else on to the place of the first one.
    private static void start() {
        started = true;
        save();
        TourOverlay.click();
        List<TourStep> pending = pending();
        Place here = Place.now();
        if (!pending.isEmpty() && pending.stream().noneMatch(step -> step.place() == here)
                && pending.get(0).place().reachable()) {
            pending.get(0).place().go();
        }
    }

    // Update first: on to the update manager, where the newer version is picked; the ask comes back until it is in.
    private static void update() {
        TourOverlay.click();
        Minecraft minecraft = Minecraft.getInstance();
        ManagerScreen.open(ManagerScreen.rootOf(minecraft.screen), ManagerScreen.UPDATES);
    }

    // Later: no asking again; the update manager's Tour still plays it.
    private static void later() {
        later = true;
        TourOverlay.click();
    }

    // The ask is shown once a version: from the next start on the tour waits in the update manager.
    private static void offered() {
        String installed = UpdateChecker.installed();
        if (!installed.equals(asked)) {
            asked = installed;
            save();
        }
    }

    // The button on the card saying how to go on: to the worlds, or out of the pause menu into the game.
    private static void go() {
        TourOverlay.click();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.setScreen(minecraft.level == null ? new SelectWorldScreen(minecraft.screen) : null);
    }

    // Later on that card: only the pill says what waits, until the game starts anew or another such card is due.
    private static void wayLater() {
        putOff = waypoint(Place.now());
        TourOverlay.click();
    }

    private static void skip() {
        introduced |= shown == TourSteps.INTRO;
        TourOverlay.skipped();
        finish(false);
    }

    private static void next() {
        TourStep step = shown;
        if (step == null) {
            return;
        }
        Place to = destination(step);
        complete(step, false);
        if (to != null) {
            to.go();
        }
    }

    // The pill's Continue: on to the place of the next step; on a tour not begun yet, Show me.
    private static void resume() {
        if (!started) {
            start();
            return;
        }
        List<TourStep> pending = pending();
        if (!pending.isEmpty() && pending.get(0).place().reachable()) {
            TourOverlay.click();
            pillSince = -1L;
            pending.get(0).place().go();
        }
    }

    private static boolean canBack() {
        String last = history.peekLast();
        TourStep step = last == null ? null : TourSteps.find(last);
        return step != null && (step.place() == Place.now() || step.place().reachable());
    }

    // Back to the step done last, wherever it was.
    private static void back() {
        if (!canBack()) {
            return;
        }
        TourStep step = TourSteps.find(history.pollLast());
        done.remove(step.id());
        shown = null;
        save();
        TourOverlay.back();
        if (step.place() != Place.now()) {
            step.place().go();
        }
    }

    // Where Next on `step` takes the player: where it leads, while steps wait there; else, once nothing waits here,
    // the place of the next step when the tour can take them there, or the menu, where a card says how to get there
    // when the next steps need a world or a character; null to stay.
    @Nullable
    private static Place destination(TourStep step) {
        List<TourStep> after = pending().stream().filter(other -> other != step).toList();
        if (after.isEmpty()) {
            return null;
        }
        Place leads = step.leads();
        if (leads != null && after.stream().anyMatch(other -> other.place() == leads)) {
            return leads.reachable() ? leads : null;
        }
        Place here = step.place() == Place.ANY ? Place.now() : step.place();
        if (after.stream().anyMatch(other -> other.place() == here)) {
            return null;
        }
        Place next = after.get(0).place();
        if (!next.reachable()) {
            return Place.now() == Place.MENU ? null : Place.MENU;
        }
        return next != Place.now() ? next : null;
    }

    // What Next says: the place it takes the player to, Done on the last step, else Next.
    private static Component nextLabel(TourStep step) {
        Place to = destination(step);
        if (to == null) {
            return Component.translatable(TourCard.PREFIX + (pending().size() <= 1 ? "done" : "next"));
        }
        String place = to != Place.MENU ? to.name().toLowerCase(Locale.ROOT)
                : Minecraft.getInstance().level == null ? "title" : "pause";
        return Component.translatable(TourCard.PREFIX + "go." + place);
    }

    // `acted`: the player did what the step asked rather than pressing Next.
    private static void complete(TourStep step, boolean acted) {
        shown = null;
        // The card on what the tour is comes once: never again, also when this tour is left unfinished.
        introduced |= step == TourSteps.INTRO;
        done.add(step.id());
        history.addLast(step.id());
        TourOverlay.done(acted, done.size());
        if (pending().isEmpty()) {
            finish(true);
            return;
        }
        save();
    }

    private static void finish(boolean cheer) {
        seen = UpdateChecker.installed();
        introduced |= done.contains(TourSteps.INTRO.id());
        done.clear();
        passed.clear();
        history.clear();
        started = false;
        shown = null;
        enterAt = -1L;
        save();
        if (cheer) {
            TourOverlay.cheer();
        }
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(ModConfigs.FOLDER).resolve(FILE);
    }

    private static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path file = file();
        String installed = UpdateChecker.installed();
        if (!Files.exists(file)) {
            // An install from before the tour existed sees this version's tour; a first one none.
            seen = ranBefore ? "" : installed;
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            seen = root.has("seen") ? root.get("seen").getAsString() : installed;
            asked = root.has("asked") ? root.get("asked").getAsString() : "";
            introduced = root.has("introduced") && root.get("introduced").getAsBoolean();
            // How far a tour got counts only for the version it was of.
            if (root.has("version") && root.get("version").getAsString().equals(installed)) {
                started = root.has("started") && root.get("started").getAsBoolean();
                read(root, "done", done);
                read(root, "passed", passed);
            }
            // Offered before and not taken: it no longer asks at every start.
            later |= !started && asked.equals(installed);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
            seen = installed;
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("version", UpdateChecker.installed());
        root.addProperty("seen", seen);
        root.addProperty("asked", asked);
        root.addProperty("introduced", introduced);
        root.addProperty("started", started);
        root.add("done", array(done));
        root.add("passed", array(passed));
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }

    private static void read(JsonObject root, String name, Set<String> into) {
        if (root.get(name) instanceof JsonArray array) {
            for (JsonElement element : array) {
                into.add(element.getAsString());
            }
        }
    }

    private static JsonArray array(Set<String> ids) {
        JsonArray array = new JsonArray();
        ids.forEach(array::add);
        return array;
    }
}
