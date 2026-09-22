package nl.tivek.welcomescreen.client.config;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.client.DirtBackgroundScreen;

/**
 * One settings page in the game: the stamina bar, or everything of one character. On the left a button per
 * ability jumps straight to it; on the right every number of the page in one scrolling list (see
 * {@link SettingsList}). Nothing is written until you press save, and then it goes into the same settings file you
 * could open by hand.
 */
public class SettingsScreen extends DirtBackgroundScreen {
    private static final String PREFIX = "config." + WelcomeScreenMod.MODID + ".";
    private static final int MAX_WIDTH = 480;
    private static final int SIDE_WIDTH = 104;
    private static final int SIDE_STEP = 18;

    @Nullable
    private final Screen lastScreen;
    private final SettingsPages.Page page;
    private final List<Button> jumps = new ArrayList<>();
    private SettingsList list;
    private int panelLeft;
    private int panelWidth;

    public SettingsScreen(@Nullable Screen lastScreen, SettingsPages.Page page) {
        super(page.title());
        this.lastScreen = lastScreen;
        this.page = page;
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(this.width - 12, MAX_WIDTH);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        int top = this.listTop();
        int bottom = this.height - 34;
        int listX = this.panelLeft + 6;
        this.jumps.clear();
        if (this.page.sections().size() > 1) {
            // A button per ability; closer together when they would not all fit.
            int count = this.page.sections().size();
            int step = Math.max(14, Math.min(SIDE_STEP, (bottom - top) / count));
            for (int i = 0; i < count; i++) {
                int section = i;
                Button jump = Button.builder(this.page.sections().get(i).title(), button -> this.list.scrollTo(section))
                        .bounds(listX, top + i * step, SIDE_WIDTH, step - 2).build();
                this.jumps.add(this.addRenderableWidget(jump));
            }
            listX += SIDE_WIDTH + 6;
        }
        int listWidth = this.panelLeft + this.panelWidth - 6 - listX;
        this.list = this.addRenderableWidget(new SettingsList(this.minecraft, this, this.page, listX, top, listWidth,
                bottom - top));

        int buttonsY = this.height - 26;
        int center = this.width / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "defaults"), button -> this.list.defaults())
                .bounds(center - 154, buttonsY, 100, 20)
                .tooltip(Tooltip.create(Component.translatable(PREFIX + "defaults.desc"))).build());
        Button save = Button.builder(Component.translatable(PREFIX + "save"), button -> this.saveAndClose())
                .bounds(center - 50, buttonsY, 100, 20).build();
        save.active = this.page.editable();
        this.addRenderableWidget(save);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(center + 54, buttonsY, 100, 20).build());
    }

    private int listTop() {
        return 34;
    }

    private void saveAndClose() {
        if (this.page.editable()) {
            this.list.store();
            this.page.save().run();
        }
        this.onClose();
    }

    /**
     * The dirt, the panel and its titles. The game draws the background again at the start of every frame, before
     * the buttons and the list, so everything that has to lie under them goes here.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        drawPanel(graphics, this.panelLeft, 4, this.panelWidth, this.height - 8, PANEL_BORDER);
        int center = this.width / 2;
        this.drawBigCenteredString(graphics, this.page.title(), center, 9, 1.2F, 0xFF000000 | this.page.color());
        Component line;
        int color;
        if (!this.page.editable()) {
            line = Component.translatable(PREFIX + "locked");
            color = NOTICE_COLOR;
        } else if (this.minecraft != null && this.minecraft.level != null && !this.minecraft.hasSingleplayerServer()) {
            // On someone else's server their own file decides; this only changes your own copy.
            line = Component.translatable(PREFIX + "server_decides");
            color = NOTICE_COLOR;
        } else {
            line = Component.translatable(PREFIX + "help");
            color = MUTED_COLOR;
        }
        graphics.drawCenteredString(this.font, line, center, 22, color);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // The ability shown at the top of the list is the one whose button stays pressed in.
        int shown = this.list.shownSection();
        for (int i = 0; i < this.jumps.size(); i++) {
            this.jumps.get(i).active = i != shown;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
