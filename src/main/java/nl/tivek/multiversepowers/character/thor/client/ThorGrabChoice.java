package nl.tivek.multiversepowers.character.thor.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.KeyCap;
import nl.tivek.multiversepowers.character.client.PowerInputs;
import nl.tivek.multiversepowers.character.thor.ThorGrab;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.lwjgl.glfw.GLFW;

// Your own Thor has just grabbed a creature: for ThorGrab.CHOICE ticks the attack, use and scroll-wheel buttons pick
// how it ends (ThorGrab.PICKS), shown under the crosshair as three keys over a bar running out; then the ending taken,
// yours or left to chance. While he holds it those buttons do nothing else, so no dash or blow cuts the grab short.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThorGrabChoice {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "grab_choice");
    // In the order of ThorGrab.PICKS.
    private static final CharacterAbility.Input[] BUTTONS = { CharacterAbility.Input.LEFT,
            CharacterAbility.Input.RIGHT, CharacterAbility.Input.SCROLL };
    private static final String KEY = "thor." + MultiversePowers.MODID + ".grab.";
    // The prompt waits this long past the window for the server's word, and the ending taken stays up this long.
    private static final float GRACE = 6.0F;
    private static final float SHOWN = 26.0F;
    private static final int COLUMN = 54;
    private static final int DARK = 0x0B1A2E;
    private static final int STORM = 0x3F8CFF;
    private static final int BOLT = 0x9FE8FF;
    private static final int WHITE = 0xF4FBFF;
    private static final int DIM = 0x8A96A6;
    private static final int LATE = 0xFF6A4D;
    private static final int GOLD = 0xFFD86B;

    // The grab this prompt is about (its start, in ClientThor's ticks; -1 none), the button pressed for it (-1 none),
    // and the ending the server took (a ThorGrab.Act, -1 not yet) and when.
    private static int grab = -1;
    private static int picked = -1;
    private static int result = -1;
    private static int resultAt;

    private ThorGrabChoice() {
    }

    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ThorGrabChoice::render);
    }

    // Your Thor holding a creature by the throat (not diving or flying with it), or null.
    @Nullable
    private static ClientThor.View holding(LocalPlayer player) {
        if (ClientCharacter.active() != GameCharacter.THOR) {
            return null;
        }
        ClientThor.View view = ClientThor.view(player);
        return view != null && view.has(ThorStatePayload.CARRYING) && view.carried >= 0
                && view.move() != ThorStatePayload.DIVE && !view.has(ThorStatePayload.FLYING) ? view : null;
    }

    // Whether your Thor holds a creature now: the guide's Holding mode.
    static boolean holds(LocalPlayer player) {
        return holding(player) != null;
    }

    private static boolean choosing(ClientThor.View view) {
        return view.move() == ThorStatePayload.GRAB && view.start == grab && view.act < 0 && result < 0
                && ClientThor.ticks() - view.start < ThorGrab.CHOICE;
    }

    @SubscribeEvent
    public static void onMouse(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null || minecraft.player == null) {
            return;
        }
        ClientThor.View view = holding(minecraft.player);
        if (view == null) {
            return;
        }
        for (int pick = 0; pick < BUTTONS.length; pick++) {
            InputConstants.Key key = PowerInputs.clickKey(BUTTONS[pick]).getKey();
            if (key.getType() == InputConstants.Type.MOUSE && key.getValue() == event.getButton()) {
                event.setCanceled(true);
                choose(minecraft, view, pick);
                return;
            }
        }
    }

    // The same picks from keys, for whoever put attack or use on the keyboard.
    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != GLFW.GLFW_PRESS || minecraft.screen != null || minecraft.player == null) {
            return;
        }
        ClientThor.View view = holding(minecraft.player);
        if (view == null) {
            return;
        }
        for (int pick = 0; pick < BUTTONS.length; pick++) {
            KeyMapping key = PowerInputs.clickKey(BUTTONS[pick]);
            if (key.getKey().getType() != InputConstants.Type.MOUSE && key.matches(event.getKey(), event.getScanCode())) {
                choose(minecraft, view, pick);
                return;
            }
        }
    }

    private static void choose(Minecraft minecraft, ClientThor.View view, int pick) {
        if (!choosing(view) || picked >= 0) {
            return;
        }
        picked = pick;
        ClientCharacter.sendAction(GameCharacter.THOR.byName("grab"), true,
                Characters.SLAM | pick << Characters.MOVE_SHIFT);
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.5F, 0.35F));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        ClientThor.View view = holding(minecraft.player);
        if (view != null && view.move() == ThorStatePayload.GRAB && view.start != grab) {
            grab = view.start;
            picked = -1;
            result = -1;
        }
        if (view != null && view.act >= 0 && result < 0 && grab >= 0) {
            result = view.act;
            resultAt = ClientThor.ticks();
        }
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || grab < 0 || minecraft.options.hideGui || minecraft.screen != null
                || ClientCharacter.active() != GameCharacter.THOR) {
            return;
        }
        float now = ClientThor.ticks() + deltaTracker.getGameTimeDeltaPartialTick(false);
        float age = now - grab;
        ClientThor.View view = holding(player);
        float alpha;
        if (result < 0) {
            if (view == null || age > ThorGrab.CHOICE + GRACE) {
                return;
            }
            alpha = (float) Ease.smooth(age / 3.0F);
        } else {
            float shown = now - resultAt;
            if (shown > SHOWN) {
                return;
            }
            alpha = 1.0F - (float) Ease.smooth((shown - (SHOWN - 8.0F)) / 8.0F);
        }
        if (alpha < 0.05F) {
            return;
        }
        Font font = minecraft.font;
        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        int cx = graphics.guiWidth() / 2;
        int top = graphics.guiHeight() / 2 + 16;
        int left = cx - COLUMN - 32;
        int width = COLUMN * 2 + 64;
        boolean mine = result >= 0 && picked >= 0 && ThorGrab.PICKS[picked].ordinal() == result;
        KeyCap.pill(graphics, left - 1, top - 1, width + 2, 50, GuiShapes.fade(STORM, 0.55F * alpha));
        KeyCap.pill(graphics, left, top, width, 48, GuiShapes.fade(DARK, 0.78F * alpha));
        Component title = result < 0 ? Component.translatable(KEY + "title")
                : mine ? Component.translatable(KEY + "act." + act(result))
                : Component.translatable(KEY + "chance", Component.translatable(KEY + "act." + act(result)));
        layer.shadowed(title, cx - font.width(title) / 2, top + 4, color(result >= 0 && !mine ? GOLD : BOLT, alpha));
        for (int pick = 0; pick < BUTTONS.length; pick++) {
            int x = cx + (pick - 1) * COLUMN;
            boolean lit = picked == pick;
            boolean faded = picked >= 0 && !lit || result >= 0 && !mine;
            if (lit) {
                float flash = result >= 0 ? 1.0F : 0.6F + 0.4F * (float) Math.sin(age * 1.4F);
                GuiShapes.roundRect(graphics, x - 26, top + 14, 52, 26, 2.0F, GuiShapes.fade(STORM, 0.5F * flash * alpha));
            }
            Component name = PowerInputs.keyName(PowerInputs.clickKey(BUTTONS[pick]));
            cap(layer, font, name, x, top + 16, faded ? 0.45F * alpha : alpha, lit);
            Component label = Component.translatable(KEY + "act." + act(ThorGrab.PICKS[pick].ordinal()));
            layer.text(label, x - font.width(label) / 2, top + 31, color(lit ? WHITE : faded ? DIM : WHITE,
                    faded ? 0.6F * alpha : alpha));
        }
        if (result < 0) {
            float left01 = Mth.clamp(1.0F - age / ThorGrab.CHOICE, 0.0F, 1.0F);
            int bar = 150;
            int fill = Math.round(bar * left01);
            int y = top + 43;
            GuiShapes.roundRect(graphics, cx - bar / 2.0F, y, bar, 2, 0.0F, GuiShapes.fade(0x000000, 0.6F * alpha));
            int tint = left01 < 0.35F ? GuiShapes.mix(LATE, STORM, left01 / 0.35F) : STORM;
            GuiShapes.roundRect(graphics, cx - fill / 2.0F, y, fill, 2, 0.0F, GuiShapes.fade(tint, alpha));
        }
        layer.finish();
    }

    // A key cap that fades with the prompt; lit, its face takes the storm's blue.
    private static void cap(KeyCap.Layer layer, Font font, Component text, int cx, int y, float alpha, boolean lit) {
        int width = KeyCap.width(font, text);
        int x = cx - width / 2;
        GuiGraphics graphics = layer.graphics();
        KeyCap.pill(graphics, x, y + 1, width, 11, GuiShapes.fade(0x121212, alpha));
        KeyCap.pill(graphics, x, y, width, 11, GuiShapes.fade(lit ? BOLT : 0x6B6B6B, alpha));
        KeyCap.pill(graphics, x, y + 1, width, 10, GuiShapes.fade(lit ? STORM : 0x3A3A3A, alpha));
        layer.text(text, x + 3, y + 2, color(WHITE, alpha));
    }

    private static String act(int ordinal) {
        ThorGrab.Act[] acts = ThorGrab.Act.values();
        return acts[Mth.clamp(ordinal, 0, acts.length - 1)].name().toLowerCase(Locale.ROOT);
    }

    // Text in `rgb` at `alpha`, never so faint the font would take it as opaque.
    private static int color(int rgb, float alpha) {
        return Math.max(5, Math.round(alpha * 255.0F)) << 24 | rgb;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        grab = -1;
        picked = -1;
        result = -1;
    }
}
