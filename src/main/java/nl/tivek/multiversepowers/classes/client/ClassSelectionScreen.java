package nl.tivek.multiversepowers.classes.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.classes.ClassGroup;
import nl.tivek.multiversepowers.classes.ClassLoadouts;
import nl.tivek.multiversepowers.classes.PlayerClass;
import nl.tivek.multiversepowers.classes.SelectClassPayload;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

/**
 * Step two of choosing: the classes of one group in a list, with a panel on the right that
 * explains the class under the cursor (or the picked one): its role, its power, its strong and
 * weak sides and why you would pick it. Clicking a card picks it; starting needs a pick.
 */
public class ClassSelectionScreen extends DirtBackgroundScreen {
    private static final int MARGIN = 8;
    private static final int HEADER_HEIGHT = 32;
    private static final int CARD_HEIGHT = 30;
    private static final int CARD_GAP = 4;
    private static final int MIN_LIST_WIDTH = 100;
    private static final int MAX_LIST_WIDTH = 130;
    private static final int COLUMN_GAP = 8;
    private static final int MAX_PANEL_WIDTH = 300;
    private static final int MIN_PANEL_HEIGHT = 136;
    private static final int MAX_PANEL_HEIGHT = 176;
    private static final int PANEL_PADDING = 6;
    private static final int SECTION_GAP = 3;
    private static final int ICON_SIZE = 16;
    private static final int ICON_GAP = 2;
    private static final int LABEL_GAP = 4;
    private static final int BUTTON_WIDTH = 120;
    private static final int BUTTON_GAP = 8;
    private static final int BUTTON_HEIGHT = 20;
    private static final int NOTICE_GAP = 5;
    private static final int NOTICE_LINES = 2;
    private static final int STRONG_COLOR = 0x7FD46B;
    private static final int WEAK_COLOR = 0xE07060;

    private final Screen lastScreen;
    private final ClassGroup group;
    private final Map<ClassCardWidget, PlayerClass> cards = new LinkedHashMap<>();

    @Nullable
    private PlayerClass selected;
    @Nullable
    private Button confirmButton;
    // The class the panel explains when nothing is hovered or picked: the last one looked at.
    private PlayerClass lastShown;

    private int headerTop;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int contentLeft;
    private int contentWidth;
    private int noticeTop;
    private int noticeHeight;

    public ClassSelectionScreen(Screen lastScreen, ClassGroup group) {
        super(group.getDisplayName());
        this.lastScreen = lastScreen;
        this.group = group;
        this.lastShown = group.getClasses().get(0);
    }

    @Override
    protected void init() {
        this.cards.clear();

        List<PlayerClass> classes = this.group.getClasses();
        // A group with a single class (The Forsaken) has nothing to choose between.
        if (classes.size() == 1) {
            this.selected = classes.get(0);
        }

        int listWidth = Math.max(MIN_LIST_WIDTH, Math.min(MAX_LIST_WIDTH, this.width / 3));
        this.panelWidth = Math.min(MAX_PANEL_WIDTH, this.width - 2 * MARGIN - listWidth - COLUMN_GAP);
        this.contentWidth = listWidth + COLUMN_GAP + this.panelWidth;
        this.noticeHeight = this.noticeHeight(null, notice("short"), this.contentWidth, NOTICE_LINES);
        int listHeight = classes.size() * CARD_HEIGHT + (classes.size() - 1) * CARD_GAP;
        int room = this.height - 2 * MARGIN - HEADER_HEIGHT - this.noticeHeight - 2 * NOTICE_GAP - BUTTON_HEIGHT;
        this.panelHeight = Math.max(listHeight, Math.max(MIN_PANEL_HEIGHT, Math.min(MAX_PANEL_HEIGHT, room)));
        int contentHeight = HEADER_HEIGHT + this.panelHeight + this.noticeHeight + 2 * NOTICE_GAP + BUTTON_HEIGHT;

        this.headerTop = Math.max(MARGIN, (this.height - contentHeight) / 2);
        this.contentLeft = (this.width - this.contentWidth) / 2;
        int listLeft = this.contentLeft;
        this.panelLeft = listLeft + listWidth + COLUMN_GAP;
        this.panelTop = this.headerTop + HEADER_HEIGHT;
        this.noticeTop = this.panelTop + this.panelHeight + NOTICE_GAP;

        for (int i = 0; i < classes.size(); i++) {
            PlayerClass playerClass = classes.get(i);
            int y = this.panelTop + i * (CARD_HEIGHT + CARD_GAP);
            ClassCardWidget card = new ClassCardWidget(listLeft, y, listWidth, CARD_HEIGHT,
                    playerClass.getDisplayName(), playerClass.getTagline(), playerClass.getColor(),
                    () -> this.select(playerClass));
            this.cards.put(card, playerClass);
            this.addRenderableWidget(card);
        }

        int buttonTop = this.noticeTop + this.noticeHeight + NOTICE_GAP;
        int buttonsLeft = (this.width - 2 * BUTTON_WIDTH - BUTTON_GAP) / 2;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui." + MultiversePowers.MODID + ".back"),
                        button -> this.onClose())
                .bounds(buttonsLeft, buttonTop, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        this.confirmButton = this.addRenderableWidget(Button.builder(
                        Component.translatable("gui." + MultiversePowers.MODID + ".confirm"),
                        button -> this.confirm())
                .bounds(buttonsLeft + BUTTON_WIDTH + BUTTON_GAP, buttonTop, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        this.updateSelection();
    }

    private void select(PlayerClass playerClass) {
        this.selected = playerClass;
        this.updateSelection();
    }

    private void updateSelection() {
        for (Map.Entry<ClassCardWidget, PlayerClass> entry : this.cards.entrySet()) {
            entry.getKey().setSelected(entry.getValue() == this.selected);
        }
        if (this.confirmButton != null) {
            this.confirmButton.active = this.selected != null;
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        this.drawBigCenteredString(guiGraphics, this.title, centerX, this.headerTop, 1.6F,
                0xFF000000 | this.group.getColor());
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen." + MultiversePowers.MODID + ".class_hint"),
                centerX, this.headerTop + 18, 0xFF000000 | MUTED_COLOR);

        this.drawNotice(guiGraphics, null, notice("short"), this.contentLeft, this.noticeTop,
                this.contentWidth, NOTICE_LINES);
        this.renderPanel(guiGraphics, mouseX, mouseY);
    }

    /**
     * The class under the cursor, or else the picked one, or else the last one looked at.
     * Top: name, role and a line of story. Middle: why to pick it, then what it does, what it is
     * good at and bad at. Bottom: what it starts with.
     */
    private void renderPanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PlayerClass shown = this.hoveredClass();
        if (shown == null) {
            shown = this.selected != null ? this.selected : this.lastShown;
        }
        this.lastShown = shown;

        int color = shown.getColor();
        drawPanel(guiGraphics, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight, 0xFF000000 | color);

        int x = this.panelLeft + PANEL_PADDING;
        int width = this.panelWidth - 2 * PANEL_PADDING;
        int itemsTop = this.panelTop + this.panelHeight - PANEL_PADDING - ICON_SIZE;
        int bottom = itemsTop - SECTION_GAP;
        String prefix = "screen." + MultiversePowers.MODID + ".label.";

        Component heading = Component.empty()
                .append(shown.getDisplayName().copy().withColor(color))
                .append(Component.literal("  ").append(shown.getTagline()).withColor(MUTED_COLOR));
        int y = this.panelTop + PANEL_PADDING;
        y = this.drawWrappedLeft(guiGraphics, heading, x, y, width, 1, bottom, 0xFF000000 | TEXT_COLOR);
        y = this.drawWrappedLeft(guiGraphics, shown.getLore().copy().withStyle(ChatFormatting.ITALIC),
                x, y, width, 2, bottom, 0xFF000000 | MUTED_COLOR);
        drawDivider(guiGraphics, x, y + 1, width);
        y += 2 * SECTION_GAP;

        y = this.drawWrappedLeft(guiGraphics, labeled(Component.translatable(prefix + "pick"),
                shown.getPick(), color), x, y, width, 2, bottom, 0xFF000000 | TEXT_COLOR) + SECTION_GAP;
        y = this.drawWrappedLeft(guiGraphics, labeled(Component.translatable(prefix + "special"),
                shown.getSpecial(), color), x, y, width, 3, bottom, 0xFF000000 | TEXT_COLOR);
        y = this.drawWrappedLeft(guiGraphics, labeled(Component.translatable(prefix + "strong"),
                shown.getStrong(), STRONG_COLOR), x, y, width, 2, bottom, 0xFF000000 | TEXT_COLOR);
        this.drawWrappedLeft(guiGraphics, labeled(Component.translatable(prefix + "weak"),
                shown.getWeak(), WEAK_COLOR), x, y, width, 2, bottom, 0xFF000000 | TEXT_COLOR);

        drawDivider(guiGraphics, x, itemsTop - SECTION_GAP - 1, width);
        this.renderStartingItems(guiGraphics, shown, x, itemsTop, mouseX, mouseY);
    }

    /** One row: "You start with:" followed by the item icons, with a tooltip on hover. */
    private void renderStartingItems(GuiGraphics guiGraphics, PlayerClass playerClass, int left, int top,
                                     int mouseX, int mouseY) {
        List<ItemStack> items = ClassLoadouts.build(playerClass).allItems();
        Component label = Component.translatable("screen." + MultiversePowers.MODID + ".start_with");
        int labelWidth = this.font.width(label);

        guiGraphics.drawString(this.font, label, left, top + (ICON_SIZE - this.font.lineHeight) / 2 + 1,
                0xFF000000 | MUTED_COLOR);

        ItemStack hovered = null;
        int iconX = left + labelWidth + LABEL_GAP;
        for (ItemStack stack : items) {
            guiGraphics.renderItem(stack, iconX, top);
            guiGraphics.renderItemDecorations(this.font, stack, iconX, top);
            if (mouseX >= iconX && mouseX < iconX + ICON_SIZE
                    && mouseY >= top && mouseY < top + ICON_SIZE) {
                hovered = stack;
            }
            iconX += ICON_SIZE + ICON_GAP;
        }

        if (hovered != null) {
            guiGraphics.renderTooltip(this.font, hovered, mouseX, mouseY);
        }
    }

    @Nullable
    private PlayerClass hoveredClass() {
        for (Map.Entry<ClassCardWidget, PlayerClass> entry : this.cards.entrySet()) {
            if (entry.getKey().isHovered()) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }

    private void confirm() {
        PlayerClass current = this.selected;
        if (current == null) {
            return;
        }

        PacketDistributor.sendToServer(new SelectClassPayload(current.getId()));

        Minecraft minecraft = this.minecraft;
        if (minecraft != null) {
            minecraft.setScreen(null);
        }
    }
}
