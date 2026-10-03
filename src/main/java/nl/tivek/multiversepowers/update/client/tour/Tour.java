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
import nl.tivek.multiversepowers.update.client.UpdateChecker;
import nl.tivek.multiversepowers.update.client.tour.TourStep.Place;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;

// After an update, a short tour points at what is new right where it is: in the menus, the update manager and the
// versions, in game at the panel and in the ability guide. It asks first on the title screen or in the pause menu; Next
// goes on (taking the player to the next place itself where it can), Back goes back, and a pill in the corner says what
// waits elsewhere. How far it got is kept in config/welcomescreen/tour.json; a first install gets no tour, as
// everything is new to it anyway. The update manager's Tour plays it again.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Tour {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE = "tour.json";
    // An install from before the tour existed gets the steps of this many newest versions.
    private static final int CATCH_UP = 2;
    // In game Enter held this long skips the tour; its key cap fills after the first moment.
    private static final long HOLD_MS = 1200L;
    private static final long HOLD_SHOWN_MS = 200L;
    // In game the pill fades after a while, so it never stays in the way.
    private static final long PILL_MS = 8000L;
    private static final long PILL_FADE_MS = 600L;

    private static boolean ranBefore;
    private static boolean loaded;
    // The newest version whose steps were all shown or skipped.
    private static String seen = "";
    private static boolean started;
    private static final Set<String> done = new LinkedHashSet<>();
    // This session only: the steps done in order (for Back), and whether the first ask was put off.
    private static final Deque<String> history = new ArrayDeque<>();
    private static boolean later;
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

    // The update manager's Tour: the tour waiting, or else every step again, from the manager's first page.
    public static void replay() {
        load();
        if (pending().isEmpty()) {
            seen = "";
            done.clear();
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || Place.now() != Place.GAME) {
            return;
        }
        load();
        if (!(started && !pending().isEmpty()) && !TourOverlay.busy()) {
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
                case LATER -> later();
                case SKIP -> skip();
                case NEXT -> next();
                case BACK -> back();
                case CONTINUE -> resume();
                default -> {
                }
            }
        }
        event.setCanceled(true);
    }

    // Enter goes on and Backspace back, while no text field has the keys.
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onKeyPressed(ScreenEvent.KeyPressed.Pre event) {
        if (typing(event.getScreen())) {
            return;
        }
        int key = event.getKeyCode();
        if (enter(key) && TourOverlay.introUp()) {
            start();
        } else if (enter(key) && TourOverlay.stepUp()) {
            next();
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
        if (enter(key) && event.getAction() == GLFW.GLFW_PRESS && (TourOverlay.stepUp() || TourOverlay.pillUp())) {
            enterAt = Util.getMillis();
        } else if (enter(key) && event.getAction() == GLFW.GLFW_RELEASE && enterAt >= 0L) {
            enterAt = -1L;
            if (TourOverlay.stepUp()) {
                next();
            } else if (TourOverlay.pillUp()) {
                resume();
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

    private static boolean typing(Screen screen) {
        return screen.getFocused() instanceof EditBox || screen.getFocused() instanceof MultiLineEditBox;
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
                TourOverlay.intro(graphics, TourStep.bare(newest()), pending.size(),
                        pending.stream().map(TourStep::title).toList(), mouseX, mouseY);
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
            pill(graphics, mouse, mouseX, mouseY);
            return;
        }
        pillSince = -1L;
        if (step.prepare() != null) {
            step.prepare().run();
        }
        ScreenAnchors.Rect target = step.target();
        if (step.points() && target == null) {
            return;
        }
        if (step != shown) {
            shown = step;
            untilGone = step.until() == null || !ScreenAnchors.shown(step.until());
        }
        pending = pending();
        TourOverlay.step(graphics, step, target, done.size() + 1, done.size() + pending.size(), mouse, canBack(),
                nextLabel(step), hold(), mouseX, mouseY);
    }

    // The first step waiting here; one with nothing to show is passed over for good.
    @Nullable
    private static TourStep current(Place place, List<TourStep> pending) {
        for (TourStep step : pending) {
            if (step.place() != place) {
                continue;
            }
            if (step.available() != null && !step.available().getAsBoolean()) {
                done.add(step.id());
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

    // Nothing waits here: a pill says how many wait elsewhere and takes the player there when it can. In game it
    // fades after a while, and shows only while it can take them.
    private static void pill(GuiGraphics graphics, boolean mouse, double mouseX, double mouseY) {
        List<TourStep> pending = pending();
        if (pending.isEmpty()) {
            return;
        }
        boolean reachable = pending.get(0).place().reachable();
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
        String why = reachable ? "pill" : Minecraft.getInstance().level == null ? "pill.world" : "pill.character";
        TourOverlay.pill(graphics, Component.translatable(TourCard.PREFIX + why, pending.size()), reachable, mouse,
                alpha, mouseX, mouseY);
    }

    // How far a held Enter is to skipping the tour.
    private static float hold() {
        if (enterAt < 0L) {
            return 0.0F;
        }
        return Mth.clamp((Util.getMillis() - enterAt - HOLD_SHOWN_MS) / (float) (HOLD_MS - HOLD_SHOWN_MS), 0.0F,
                1.0F);
    }

    private static List<TourStep> pending() {
        List<TourStep> pending = new ArrayList<>();
        for (TourStep step : TourSteps.ALL) {
            if (UpdateChecker.compare(step.version(), seen) > 0 && !done.contains(step.id())) {
                pending.add(step);
            }
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

    // Later: no asking again until the game starts anew.
    private static void later() {
        later = true;
        TourOverlay.click();
    }

    private static void skip() {
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

    // The pill's Continue: on to the place of the next step.
    private static void resume() {
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
    // the place of the next step when the tour can take them there; null to stay.
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
        if (after.stream().anyMatch(other -> other.place() == step.place())) {
            return null;
        }
        Place next = after.get(0).place();
        return next.reachable() && next != Place.now() ? next : null;
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
        seen = newest();
        done.clear();
        history.clear();
        started = false;
        shown = null;
        enterAt = -1L;
        save();
        if (cheer) {
            TourOverlay.cheer();
        }
    }

    // The newest version the tour knows: this one's, or a newer step's while testing a version not yet released.
    private static String newest() {
        String newest = UpdateChecker.installed();
        for (TourStep step : TourSteps.ALL) {
            if (UpdateChecker.compare(step.version(), newest) > 0) {
                newest = step.version();
            }
        }
        return newest;
    }

    // The version before the newest few that have steps, so only theirs are waiting.
    private static String catchUp() {
        List<String> versions = TourSteps.ALL.stream().map(TourStep::version).distinct()
                .sorted((a, b) -> UpdateChecker.compare(b, a)).toList();
        return versions.size() <= CATCH_UP ? "" : versions.get(CATCH_UP);
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
        if (!Files.exists(file)) {
            seen = ranBefore ? catchUp() : newest();
            save();
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            seen = root.has("seen") ? root.get("seen").getAsString() : newest();
            started = root.has("started") && root.get("started").getAsBoolean();
            if (root.get("done") instanceof JsonArray array) {
                for (JsonElement element : array) {
                    done.add(element.getAsString());
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Could not read {}: {}", file, e.toString());
            seen = newest();
        }
    }

    private static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("seen", seen);
        root.addProperty("started", started);
        JsonArray array = new JsonArray();
        done.forEach(array::add);
        root.add("done", array);
        Path file = file();
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not save {}: {}", file, e.toString());
        }
    }
}
