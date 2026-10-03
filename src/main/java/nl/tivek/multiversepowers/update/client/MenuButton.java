package nl.tivek.multiversepowers.update.client;

import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// A small icon button that opens the update manager, beside the Mods button as the language and accessibility buttons
// sit beside theirs: on the title screen right of the Realms and Mods row, in the pause menu right of Mods. A green dot
// on it while a newer version is out.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MenuButton extends Button {
    private static final int SIZE = 20;

    private final String anchor;

    private MenuButton(Screen screen, int x, int y, String anchor) {
        super(x, y, SIZE, SIZE, ManagerScreen.text("button"), button -> ManagerScreen.open(screen),
                DEFAULT_NARRATION);
        this.anchor = anchor;
        this.setTooltip(Tooltip.create(ManagerScreen.text("button")));
    }

    @SubscribeEvent
    static void onInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof TitleScreen) {
            AbstractWidget single = find(event, "menu.singleplayer");
            if (single != null) {
                event.addListener(new MenuButton(screen, screen.width / 2 + 104, single.getY() + 48, "title.updates"));
            }
        } else if (screen instanceof PauseScreen pause && pause.showsPauseMenu()) {
            AbstractWidget mods = find(event, "fml.menu.mods");
            AbstractWidget row = mods != null ? mods : find(event, "menu.returnToGame");
            if (row != null) {
                event.addListener(new MenuButton(screen, row.getX() + row.getWidth() + 4, row.getY(),
                        "pause.updates"));
            }
        }
    }

    @Nullable
    private static AbstractWidget find(ScreenEvent.Init.Post event, String key) {
        for (GuiEventListener child : event.getListenersList()) {
            if (child instanceof AbstractWidget widget && widget.getMessage().getContents()
                    instanceof TranslatableContents contents && contents.getKey().equals(key)) {
                return widget;
            }
        }
        return null;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        ScreenAnchors.report(this.anchor, this.getX(), this.getY(), this.width, this.height);
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
