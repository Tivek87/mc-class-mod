package nl.tivek.multiversepowers.update.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// A small icon button on the title screen, right of the Realms row as the language and accessibility buttons sit beside
// theirs, that opens the update manager; a green dot on it while a newer version is out.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class TitleButton extends Button {
    private static final int SIZE = 20;

    private TitleButton(Screen screen, int x, int y) {
        super(x, y, SIZE, SIZE, UpdateManagerScreen.text("button"),
                button -> Minecraft.getInstance().setScreen(new UpdateManagerScreen(screen)), DEFAULT_NARRATION);
        this.setTooltip(Tooltip.create(UpdateManagerScreen.text("button")));
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) {
            return;
        }
        for (GuiEventListener child : event.getListenersList()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getContents()
                    instanceof TranslatableContents key && key.getKey().equals("menu.singleplayer")) {
                event.addListener(new TitleButton(screen, screen.width / 2 + 104, widget.getY() + 48));
                return;
            }
        }
    }

    // In place of the message: a pixel arrow down into a tray, with the game's own text shadow under it.
    @Override
    public void renderString(GuiGraphics graphics, Font font, int color) {
        int alpha = FastColor.ARGB32.alpha(color) << 24;
        this.icon(graphics, 1, alpha | 0x3F3F3F);
        this.icon(graphics, 0, alpha | (this.active ? 0xFFFFFF : 0xA0A0A0));
        if (UpdateChecker.latest() != null) {
            int x = this.getX() + 14;
            int y = this.getY() + 2;
            graphics.fill(x - 1, y - 1, x + 4, y + 4, alpha | 0x000000);
            graphics.fill(x, y, x + 3, y + 3, alpha | UpdatePopup.ACCENT);
        }
    }

    private void icon(GuiGraphics graphics, int shadow, int color) {
        int x = this.getX() + shadow;
        int y = this.getY() + shadow;
        graphics.fill(x + 9, y + 4, x + 11, y + 9, color);
        graphics.fill(x + 6, y + 9, x + 14, y + 10, color);
        graphics.fill(x + 7, y + 10, x + 13, y + 11, color);
        graphics.fill(x + 8, y + 11, x + 12, y + 12, color);
        graphics.fill(x + 9, y + 12, x + 11, y + 13, color);
        graphics.fill(x + 4, y + 13, x + 5, y + 16, color);
        graphics.fill(x + 15, y + 13, x + 16, y + 16, color);
        graphics.fill(x + 4, y + 15, x + 16, y + 16, color);
    }
}
