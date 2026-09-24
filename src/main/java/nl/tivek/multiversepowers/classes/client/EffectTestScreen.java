package nl.tivek.multiversepowers.classes.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.classes.ClassGroup;
import nl.tivek.multiversepowers.classes.PlayerClass;
import nl.tivek.multiversepowers.classes.TestEffectPayload;
import nl.tivek.multiversepowers.engine.client.gui.DirtBackgroundScreen;

/**
 * Developer tool, opened with /classfx: plays the start ceremony of any class, the death animation
 * of any group, or the level-up animation on the spot, whatever class the player has.
 * Picking a button closes the screen so the effect is visible.
 */
public class EffectTestScreen extends DirtBackgroundScreen {
    private static final int COLUMNS = 5;
    private static final int MAX_BUTTON_WIDTH = 92;
    private static final int BUTTON_HEIGHT = 20;
    private static final int GAP = 4;
    private static final int MARGIN = 8;
    private static final int HEADER_HEIGHT = 30;
    private static final int TOGGLE_WIDTH = 220;
    private static final int EXTRA_COLUMNS = 4;
    private static final int MAX_EXTRA_WIDTH = 124;
    private static final int LABEL_HEIGHT = 12;
    private static final int LEVEL_UP_COLOR = 0x7FFF40;

    // Remembered while the game runs, so testing the same mode again is one click.
    private static boolean showTitle = true;

    private int headerTop;
    private int extraLabelTop;

    public EffectTestScreen() {
        super(Component.translatable("screen." + MultiversePowers.MODID + ".fx_test.title"));
    }

    @Override
    protected void init() {
        PlayerClass[] classes = PlayerClass.values();
        int rows = (classes.length + COLUMNS - 1) / COLUMNS;
        int buttonWidth = Math.min(MAX_BUTTON_WIDTH, (this.width - 2 * MARGIN - (COLUMNS - 1) * GAP) / COLUMNS);
        int gridWidth = COLUMNS * buttonWidth + (COLUMNS - 1) * GAP;
        int gridHeight = rows * (BUTTON_HEIGHT + GAP) - GAP;
        ClassGroup[] groups = ClassGroup.values();
        int extraCount = groups.length + 1;
        int extraRows = (extraCount + EXTRA_COLUMNS - 1) / EXTRA_COLUMNS;
        int extraButtonWidth = Math.min(MAX_EXTRA_WIDTH,
                (this.width - 2 * MARGIN - (EXTRA_COLUMNS - 1) * GAP) / EXTRA_COLUMNS);
        int extraWidth = EXTRA_COLUMNS * extraButtonWidth + (EXTRA_COLUMNS - 1) * GAP;
        int extraHeight = extraRows * (BUTTON_HEIGHT + GAP) - GAP;
        int contentHeight = HEADER_HEIGHT + BUTTON_HEIGHT + 2 * GAP + gridHeight + 2 * GAP + LABEL_HEIGHT
                + extraHeight + 2 * GAP + BUTTON_HEIGHT;

        this.headerTop = Math.max(MARGIN, (this.height - contentHeight) / 2);
        int toggleTop = this.headerTop + HEADER_HEIGHT;
        int gridLeft = (this.width - gridWidth) / 2;
        int gridTop = toggleTop + BUTTON_HEIGHT + 2 * GAP;

        this.addRenderableWidget(Button.builder(toggleLabel(), button -> {
                    showTitle = !showTitle;
                    button.setMessage(toggleLabel());
                })
                .bounds((this.width - TOGGLE_WIDTH) / 2, toggleTop, TOGGLE_WIDTH, BUTTON_HEIGHT)
                .build());

        for (int i = 0; i < classes.length; i++) {
            PlayerClass playerClass = classes[i];
            int x = gridLeft + (i % COLUMNS) * (buttonWidth + GAP);
            int y = gridTop + (i / COLUMNS) * (BUTTON_HEIGHT + GAP);
            this.addRenderableWidget(Button.builder(
                            playerClass.getDisplayName().copy().withColor(playerClass.getColor()),
                            button -> this.play(playerClass))
                    .bounds(x, y, buttonWidth, BUTTON_HEIGHT)
                    .build());
        }

        // Death animation per group, and the level-up animation that is the same for everyone.
        this.extraLabelTop = gridTop + gridHeight + 2 * GAP;
        int extraLeft = (this.width - extraWidth) / 2;
        int extraTop = this.extraLabelTop + LABEL_HEIGHT;
        for (int i = 0; i < extraCount; i++) {
            int x = extraLeft + (i % EXTRA_COLUMNS) * (extraButtonWidth + GAP);
            int y = extraTop + (i / EXTRA_COLUMNS) * (BUTTON_HEIGHT + GAP);
            Button.Builder builder;
            if (i < groups.length) {
                ClassGroup group = groups[i];
                builder = Button.builder(
                        Component.translatable("screen." + MultiversePowers.MODID + ".fx_test.death",
                                group.getDisplayName()).withColor(group.getColor()),
                        button -> this.send(new TestEffectPayload(TestEffectPayload.DEATH, group.getId(), false)));
            } else {
                builder = Button.builder(
                        Component.translatable("screen." + MultiversePowers.MODID + ".fx_test.level_up")
                                .withColor(LEVEL_UP_COLOR),
                        button -> this.send(new TestEffectPayload(TestEffectPayload.LEVEL_UP, "", false)));
            }
            this.addRenderableWidget(builder.bounds(x, y, extraButtonWidth, BUTTON_HEIGHT).build());
        }

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui." + MultiversePowers.MODID + ".close"),
                        button -> this.onClose())
                .bounds((this.width - TOGGLE_WIDTH) / 2, extraTop + extraHeight + 2 * GAP, TOGGLE_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private static Component toggleLabel() {
        return Component.translatable("screen." + MultiversePowers.MODID
                + (showTitle ? ".fx_test.with_title" : ".fx_test.without_title"));
    }

    private void play(PlayerClass playerClass) {
        this.send(new TestEffectPayload(TestEffectPayload.CEREMONY, playerClass.getId(), showTitle));
    }

    private void send(TestEffectPayload payload) {
        PacketDistributor.sendToServer(payload);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        this.drawBigCenteredString(guiGraphics, this.title, centerX, this.headerTop, 1.6F, 0xFFFFFFFF);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen." + MultiversePowers.MODID + ".fx_test.hint"),
                centerX, this.headerTop + 18, 0xFF000000 | MUTED_COLOR);
        guiGraphics.drawCenteredString(this.font,
                Component.translatable("screen." + MultiversePowers.MODID + ".fx_test.other"),
                centerX, this.extraLabelTop, 0xFF000000 | MUTED_COLOR);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    // The world keeps running behind the menu, it is only a tool.
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
