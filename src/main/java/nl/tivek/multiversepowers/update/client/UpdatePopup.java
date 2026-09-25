package nl.tivek.multiversepowers.update.client;

import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class UpdatePopup {
    public static final KeyMapping OPEN_KEY = new KeyMapping("key." + MultiversePowers.MODID + ".open_update",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, AbilityKeys.CATEGORY);

    static final int ACCENT = 0x6EE7A0;
    private static final int HEIGHT = 27;
    private static final int MARGIN = 6;
    private static final int PADDING = 8;
    private static final int FILL = 0xEE12161C;
    private static final int FILL_HOVER = 0xEE1C2430;
    private static final int MUTED = 0xA8B0BC;
    private static final long SLIDE_MS = 350L;
    private static final long IN_GAME_MS = 15_000L;
    private static final long FADE_MS = 600L;
    private static final int SECOND_NOTE_TICKS = 3;

    @Nullable
    private static Release shown;
    private static long shownAt;
    private static boolean hidden;
    private static int secondNote = -1;
    // Set by draw() and read by over() for hit-testing; draw must run first.
    private static int width = 160;

    private UpdatePopup() {
    }

    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_KEY);
    }

    static void announce(Release release) {
        shown = release;
        shownAt = System.currentTimeMillis();
        hidden = false;
        pling(1.0F);
        secondNote = SECOND_NOTE_TICKS;
    }

    static void hide() {
        hidden = true;
    }

    private static void pling(float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), pitch, 0.7F));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (secondNote >= 0 && secondNote-- == 0) {
            pling(1.5F);
        }
        Minecraft minecraft = Minecraft.getInstance();
        while (OPEN_KEY.consumeClick()) {
            if (minecraft.screen == null) {
                minecraft.setScreen(new UpdateManagerScreen(null));
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        long age = System.currentTimeMillis() - shownAt;
        if (!active() || minecraft.screen != null || minecraft.options.hideGui || age > IN_GAME_MS) {
            return;
        }
        float fade = Mth.clamp((IN_GAME_MS - age) / (float) FADE_MS, 0.0F, 1.0F);
        Component hint = Component.translatable("screen." + MultiversePowers.MODID + ".update.popup.key",
                OPEN_KEY.getTranslatedKeyMessage().copy().withStyle(ChatFormatting.WHITE));
        draw(event.getGuiGraphics(), minecraft.font, event.getGuiGraphics().guiWidth(), age, fade, false, hint);
    }

    @SubscribeEvent
    public static void onRenderScreen(ScreenEvent.Render.Post event) {
        if (!active() || !showsOn(event.getScreen())) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getScreen().width;
        boolean hover = over(width, event.getMouseX(), event.getMouseY());
        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 400.0F);
        draw(graphics, Minecraft.getInstance().font, width, System.currentTimeMillis() - shownAt, 1.0F, hover,
                Component.translatable("screen." + MultiversePowers.MODID + ".update.popup.click"));
        graphics.pose().popPose();
    }

    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen screen = event.getScreen();
        if (active() && showsOn(screen) && event.getButton() == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && over(screen.width, event.getMouseX(), event.getMouseY())) {
            Minecraft.getInstance().setScreen(new UpdateManagerScreen(screen));
            event.setCanceled(true);
        }
    }

    private static boolean active() {
        return shown != null && !hidden && UpdateChecker.latest() != null;
    }

    private static boolean showsOn(Screen screen) {
        return screen instanceof TitleScreen || screen instanceof PauseScreen;
    }

    private static boolean over(int screenWidth, double mouseX, double mouseY) {
        int x = screenWidth - width - MARGIN;
        return mouseX >= x && mouseX < x + width && mouseY >= MARGIN && mouseY < MARGIN + HEIGHT;
    }

    private static void draw(GuiGraphics graphics, Font font, int screenWidth, long age, float alpha, boolean hover,
            Component hint) {
        Component title = UpdateManagerScreen.text("popup.title");
        Component line = Component.empty()
                .append(Component.literal("v" + shown.version()).withColor(ACCENT))
                .append(Component.literal("  ·  ").withColor(MUTED))
                .append(hint.copy().withColor(MUTED));
        width = Math.max(font.width(title), font.width(line)) + 2 * PADDING;
        float slide = 1.0F - Mth.clamp(age / (float) SLIDE_MS, 0.0F, 1.0F);
        float x = screenWidth - width - MARGIN + slide * slide * (width + MARGIN);
        int fill = hover ? FILL_HOVER : FILL;
        GuiShapes.roundRect(graphics, x, MARGIN, width, HEIGHT, 4.0F, GuiShapes.fade(fill, alpha * (fill >>> 24) / 255.0F));
        GuiShapes.flush(graphics);

        int textAlpha = Mth.clamp((int) (alpha * 255.0F), 4, 255) << 24;
        graphics.drawString(font, title, (int) x + PADDING, MARGIN + 5, textAlpha | 0xFFFFFF, false);
        graphics.drawString(font, line, (int) x + PADDING, MARGIN + 15, textAlpha | MUTED, false);
    }
}
