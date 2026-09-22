package nl.tivek.welcomescreen.client.classes;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.classes.PlayerClass;

/**
 * Shows the player's class as a label above the inventory, so it can always be looked up.
 * Hovering the label shows what the class is about.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClassInventoryDisplay {
    private static final int SURVIVAL_OFFSET = 14;
    // The creative screen has its tabs above the panel, so the label goes above those.
    private static final int CREATIVE_OFFSET = 42;
    private static final int PADDING_X = 4;
    private static final int PADDING_Y = 2;
    private static final int BACKING = 0xA0000000;
    private static final int MUTED_COLOR = 0xA8A090;
    private static final int NOTICE_COLOR = 0xF2C84B;
    private static final int TOOLTIP_WIDTH = 220;

    private ClassInventoryDisplay() {
    }

    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        PlayerClass playerClass = ClientClassData.get();
        if (playerClass == null) {
            return;
        }

        int offset;
        if (event.getScreen() instanceof InventoryScreen) {
            offset = SURVIVAL_OFFSET;
        } else if (event.getScreen() instanceof CreativeModeInventoryScreen) {
            offset = CREATIVE_OFFSET;
        } else {
            return;
        }
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();

        Font font = Minecraft.getInstance().font;
        GuiGraphics guiGraphics = event.getGuiGraphics();
        Component label = Component.empty()
                .append(playerClass.getDisplayName().copy().withColor(playerClass.getColor()))
                .append(Component.literal(" · ").append(playerClass.getGroup().getDisplayName())
                        .withColor(MUTED_COLOR));

        int width = font.width(label);
        int centerX = screen.getGuiLeft() + screen.getXSize() / 2;
        int x = centerX - width / 2;
        int y = Math.max(PADDING_Y, screen.getGuiTop() - offset);

        int left = x - PADDING_X;
        int top = y - PADDING_Y;
        int right = x + width + PADDING_X;
        int bottom = y + font.lineHeight + PADDING_Y;
        guiGraphics.fill(left, top, right, bottom, BACKING);
        guiGraphics.renderOutline(left, top, right - left, bottom - top, 0xFF000000 | playerClass.getColor());
        guiGraphics.drawString(font, label, x, y, 0xFFFFFFFF);

        int mouseX = event.getMouseX();
        int mouseY = event.getMouseY();
        if (mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom) {
            guiGraphics.renderTooltip(font, tooltip(font, playerClass), mouseX, mouseY);
        }
    }

    /** Name, role, a line of story and the reminder that the class locks nothing. */
    private static List<FormattedCharSequence> tooltip(Font font, PlayerClass playerClass) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(playerClass.getDisplayName().copy().withColor(playerClass.getColor()).getVisualOrderText());
        lines.add(playerClass.getTagline().copy().withColor(MUTED_COLOR).getVisualOrderText());
        lines.addAll(font.split(playerClass.getLore().copy().withStyle(ChatFormatting.ITALIC), TOOLTIP_WIDTH));
        lines.addAll(font.split(Component.translatable("screen." + WelcomeScreenMod.MODID + ".inventory.hint")
                .withColor(NOTICE_COLOR), TOOLTIP_WIDTH));
        return lines;
    }
}
