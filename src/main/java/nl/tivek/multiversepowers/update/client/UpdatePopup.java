package nl.tivek.multiversepowers.update.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
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
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class UpdatePopup {
    public static final KeyMapping OPEN_KEY = new KeyMapping("key." + MultiversePowers.MODID + ".open_update",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, AbilityKeys.CATEGORY);

    public static final int ACCENT = 0x6EE7A0;
    private static final int HEIGHT = 46;
    private static final int MARGIN = 8;
    private static final int PADDING = 10;
    private static final int TEXT_X = 44;
    private static final int MIN_WIDTH = 200;
    private static final int FILL = 0xF2121212;
    private static final int FILL_HOVER = 0xF21E1E1E;
    private static final int MUTED = 0x9A9A9A;
    private static final long SLIDE_MS = 350L;
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
                ManagerScreen.open(null);
            }
        }
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        long age = System.currentTimeMillis() - shownAt;
        long shownFor = ClientSettings.updatePopupMs();
        if (!active() || minecraft.screen != null || minecraft.options.hideGui || age > shownFor) {
            return;
        }
        float fade = Mth.clamp((shownFor - age) / (float) FADE_MS, 0.0F, 1.0F);
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
            ManagerScreen.open(screen);
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

    // The update manager's look in small: a black edge, a dark fill, the accent down its left side and the download
    // icon, sliding in from the right.
    private static void draw(GuiGraphics graphics, Font font, int screenWidth, long age, float alpha, boolean hover,
            Component hint) {
        Component title = ManagerScreen.text("popup.title").copy().withStyle(ChatFormatting.BOLD);
        Component versions = Component.empty()
                .append(Component.literal("v" + UpdateChecker.installed()).withColor(MUTED))
                .append(Component.literal("  →  ").withColor(MUTED))
                .append(Component.literal("v" + shown.version()).withColor(ACCENT));
        Component line = hint.copy().withColor(MUTED);
        width = Math.max(MIN_WIDTH, Math.max(font.width(title), Math.max(font.width(versions), font.width(line)))
                + TEXT_X + PADDING);
        float slide = 1.0F - Mth.clamp(age / (float) SLIDE_MS, 0.0F, 1.0F);
        int x = Math.round(screenWidth - width - MARGIN + slide * slide * (width + MARGIN));
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, Math.max(alpha, 0.02F));
        graphics.fill(x - 1, MARGIN - 1, x + width + 1, MARGIN + HEIGHT + 1, 0xFF000000);
        graphics.fill(x, MARGIN, x + width, MARGIN + HEIGHT, hover ? FILL_HOVER : FILL);
        graphics.renderOutline(x, MARGIN, width, HEIGHT, 0x1CFFFFFF);
        graphics.fill(x, MARGIN, x + 2, MARGIN + HEIGHT, 0xFF000000 | ACCENT);
        PixelIcons.draw(graphics, PixelIcons.Icon.DOWNLOAD, x + 12, MARGIN + 11, 2, 0xFF000000 | ACCENT, true);
        graphics.drawString(font, title, x + TEXT_X, MARGIN + 7, 0xFFFFFFFF, false);
        graphics.drawString(font, versions, x + TEXT_X, MARGIN + 19, 0xFF000000 | MUTED, false);
        graphics.drawString(font, line, x + TEXT_X, MARGIN + 31, 0xFF000000 | MUTED, false);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
