package nl.tivek.welcomescreen.client.classes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.classes.ClassGroup;
import nl.tivek.welcomescreen.classes.PlayerClass;
import nl.tivek.welcomescreen.client.DirtBackgroundScreen;

/**
 * Step one of choosing: the six groups in a list, with The Forsaken set apart below them.
 * The panel on the right explains the group under the cursor (or the last one looked at),
 * so it is never empty; clicking a card opens that group's classes.
 */
public class GroupSelectionScreen extends DirtBackgroundScreen {
    private static final int MARGIN = 8;
    private static final int HEADER_HEIGHT = 30;
    private static final int CARD_HEIGHT = 18;
    private static final int CARD_GAP = 2;
    private static final int FORSAKEN_GAP = 6;
    private static final int NOTICE_GAP = 5;
    private static final int NOTICE_LINES = 2;
    private static final int MIN_LIST_WIDTH = 100;
    private static final int MAX_LIST_WIDTH = 130;
    private static final int COLUMN_GAP = 8;
    private static final int MAX_PANEL_WIDTH = 300;
    private static final int PANEL_PADDING = 6;
    private static final int SECTION_GAP = 3;
    private static final int BUTTON_WIDTH = 150;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen lastScreen;
    private final Map<ClassCardWidget, ClassGroup> cards = new LinkedHashMap<>();

    // The group the panel explains. Starts on the first group and follows the cursor.
    private ClassGroup shown = ClassGroup.WARRIORS;

    private int headerTop;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int contentLeft;
    private int contentWidth;
    private int noticeTop;
    private int noticeHeight;

    public GroupSelectionScreen(Screen lastScreen) {
        super(Component.translatable("screen." + WelcomeScreenMod.MODID + ".group_title"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        this.cards.clear();

        List<ClassGroup> groups = List.of(ClassGroup.WARRIORS, ClassGroup.RANGERS, ClassGroup.ROGUES,
                ClassGroup.MAGES, ClassGroup.FAITHFUL, ClassGroup.ALCHEMISTS);
        int listWidth = Math.max(MIN_LIST_WIDTH, Math.min(MAX_LIST_WIDTH, this.width / 3));
        this.panelWidth = Math.min(MAX_PANEL_WIDTH, this.width - 2 * MARGIN - listWidth - COLUMN_GAP);
        int listHeight = (groups.size() + 1) * CARD_HEIGHT + (groups.size() - 1) * CARD_GAP + FORSAKEN_GAP;
        this.panelHeight = listHeight;
        this.contentWidth = listWidth + COLUMN_GAP + this.panelWidth;
        this.noticeHeight = this.noticeHeight(null, notice("short"), this.contentWidth, NOTICE_LINES);
        int contentHeight = HEADER_HEIGHT + listHeight + this.noticeHeight + 2 * NOTICE_GAP + BUTTON_HEIGHT;

        this.headerTop = Math.max(MARGIN, (this.height - contentHeight) / 2);
        this.contentLeft = (this.width - this.contentWidth) / 2;
        int listLeft = this.contentLeft;
        this.panelLeft = listLeft + listWidth + COLUMN_GAP;
        this.panelTop = this.headerTop + HEADER_HEIGHT;
        this.noticeTop = this.panelTop + listHeight + NOTICE_GAP;

        int y = this.panelTop;
        for (ClassGroup group : groups) {
            this.addCard(group, listLeft, y, listWidth);
            y += CARD_HEIGHT + CARD_GAP;
        }
        // The Forsaken stands outside every group, so he is set apart from the list.
        this.addCard(ClassGroup.FORSAKEN, listLeft, y - CARD_GAP + FORSAKEN_GAP, listWidth);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui." + WelcomeScreenMod.MODID + ".back"),
                        button -> this.onClose())
                .bounds((this.width - BUTTON_WIDTH) / 2, this.noticeTop + this.noticeHeight + NOTICE_GAP,
                        BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void addCard(ClassGroup group, int x, int y, int width) {
        ClassCardWidget card = new ClassCardWidget(x, y, width, CARD_HEIGHT, group.getDisplayName(), null,
                group.getColor(), () -> this.openGroup(group));
        this.cards.put(card, group);
        this.addRenderableWidget(card);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        this.drawBigCenteredString(guiGraphics, this.title, centerX, this.headerTop, 1.6F, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen." + WelcomeScreenMod.MODID + ".group_hint"),
                centerX, this.headerTop + 18, 0xFF000000 | MUTED_COLOR);

        ClassGroup active = this.activeGroup();
        if (active != null) {
            this.shown = active;
        }
        this.renderPanel(guiGraphics, this.shown);
        this.drawNotice(guiGraphics, null, notice("short"), this.contentLeft, this.noticeTop,
                this.contentWidth, NOTICE_LINES);
    }

    /** Name, story, where the power comes from, why to pick it, and every class with its role. */
    private void renderPanel(GuiGraphics guiGraphics, ClassGroup group) {
        drawPanel(guiGraphics, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight,
                0xFF000000 | group.getColor());

        int x = this.panelLeft + PANEL_PADDING;
        int width = this.panelWidth - 2 * PANEL_PADDING;
        int bottom = this.panelTop + this.panelHeight - PANEL_PADDING;
        int labelColor = group.getColor();
        String prefix = "screen." + WelcomeScreenMod.MODID + ".label.";

        int y = this.panelTop + PANEL_PADDING;
        y = this.drawWrappedLeft(guiGraphics, group.getDisplayName(), x, y, width, 1, bottom,
                0xFF000000 | group.getColor());
        y = this.drawWrappedLeft(guiGraphics, group.getLore().copy().withStyle(ChatFormatting.ITALIC),
                x, y, width, 2, bottom, 0xFF000000 | MUTED_COLOR);
        drawDivider(guiGraphics, x, y + 1, width);
        y += 2 * SECTION_GAP;
        y = this.drawWrappedLeft(guiGraphics, labeled(Component.translatable(prefix + "pick"), group.getPick(),
                labelColor), x, y, width, 2, bottom, 0xFF000000 | TEXT_COLOR);
        y = this.drawWrappedLeft(guiGraphics, labeled(Component.translatable(prefix + "power"), group.getPower(),
                labelColor), x, y, width, 2, bottom, 0xFF000000 | TEXT_COLOR);
        drawDivider(guiGraphics, x, y + 1, width);
        y += 2 * SECTION_GAP;

        for (PlayerClass playerClass : group.getClasses()) {
            Component line = Component.empty()
                    .append(playerClass.getDisplayName().copy().withColor(labelColor))
                    .append(Component.literal(" - ").append(playerClass.getTagline())
                            .withColor(MUTED_COLOR));
            y = this.drawWrappedLeft(guiGraphics, line, x, y, width, 1, bottom, 0xFF000000 | TEXT_COLOR);
        }
    }

    /** The hovered card, or else the keyboard-focused one. */
    @Nullable
    private ClassGroup activeGroup() {
        for (Map.Entry<ClassCardWidget, ClassGroup> entry : this.cards.entrySet()) {
            if (entry.getKey().isHovered()) {
                return entry.getValue();
            }
        }
        for (Map.Entry<ClassCardWidget, ClassGroup> entry : this.cards.entrySet()) {
            if (entry.getKey().isFocused()) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void openGroup(ClassGroup group) {
        if (this.minecraft != null) {
            this.minecraft.setScreen(new ClassSelectionScreen(this, group));
        }
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
