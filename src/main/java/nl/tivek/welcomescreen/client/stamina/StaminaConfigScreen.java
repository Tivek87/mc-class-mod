package nl.tivek.welcomescreen.client.stamina;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.welcomescreen.client.DirtBackgroundScreen;

import java.util.Locale;
import nl.tivek.welcomescreen.config.StaminaConfig;

/**
 * Gebruiksvriendelijk in-game configuratiescherm voor stamina.
 * Toegankelijk via het Mods-menu ("Config") of via de chat met /stamina config.
 */
public class StaminaConfigScreen extends DirtBackgroundScreen {
    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 210;

    private final Screen lastScreen;

    private EditBox maxStaminaBox;
    private EditBox sprintDrainBox;
    private EditBox jumpCostBox;
    private EditBox regenRateBox;
    private EditBox regenDelayBox;
    private EditBox exhaustionBox;

    public StaminaConfigScreen(Screen lastScreen) {
        super(Component.literal("Stamina Instellingen"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int panelTop = (this.height - PANEL_HEIGHT) / 2;
        int startY = panelTop + 38;
        int rowHeight = 24;

        // 1. Max Stamina
        maxStaminaBox = createNumberBox(centerX - 10, startY, String.format(Locale.ROOT, "%.0f", StaminaConfig.getMaxStamina()));
        addMinusPlus(centerX - 10, startY, maxStaminaBox, -10.0, 10.0, 10.0, 1000.0, true);
        maxStaminaBox.setTooltip(Tooltip.create(Component.literal("Totale stamina-voorraad van de speler. Standaard is 100.")));

        // 2. Sprintverbruik
        sprintDrainBox = createNumberBox(centerX - 10, startY + rowHeight, String.format(Locale.ROOT, "%.2f", StaminaConfig.getSprintDrain()));
        addMinusPlus(centerX - 10, startY + rowHeight, sprintDrainBox, -0.05, 0.05, 0.01, 10.0, false);
        sprintDrainBox.setTooltip(Tooltip.create(Component.literal("Hoeveel stamina sprinten kost per tick (20 ticks = 1 sec). Standaard: 0.20.")));

        // 3. Springkosten
        jumpCostBox = createNumberBox(centerX - 10, startY + rowHeight * 2, String.format(Locale.ROOT, "%.1f", StaminaConfig.getJumpCost()));
        addMinusPlus(centerX - 10, startY + rowHeight * 2, jumpCostBox, -0.5, 0.5, 0.0, 100.0, false);
        jumpCostBox.setTooltip(Tooltip.create(Component.literal("Stamina die per sprong wordt verbruikt. Standaard: 3.2.")));

        // 4. Herstelsnelheid
        regenRateBox = createNumberBox(centerX - 10, startY + rowHeight * 3, String.format(Locale.ROOT, "%.2f", StaminaConfig.getRegenRate()));
        addMinusPlus(centerX - 10, startY + rowHeight * 3, regenRateBox, -0.1, 0.1, 0.05, 10.0, false);
        regenRateBox.setTooltip(Tooltip.create(Component.literal("Hoe snel stamina herstelt bij stilstaan of wandelen (per tick). Standaard: 0.60.")));

        // 5. Herstelvertraging (rust)
        regenDelayBox = createNumberBox(centerX - 10, startY + rowHeight * 4, String.valueOf(StaminaConfig.getRegenDelay()));
        addMinusPlus(centerX - 10, startY + rowHeight * 4, regenDelayBox, -5, 5, 0, 200, true);
        regenDelayBox.setTooltip(Tooltip.create(Component.literal("Seconden rust na sprinten/springen voordat herstel begint (20 ticks = 1 sec).")));

        // 6. Uitputtingsdrempel
        exhaustionBox = createNumberBox(centerX - 10, startY + rowHeight * 5, String.format(Locale.ROOT, "%.0f", StaminaConfig.getExhaustionThreshold()));
        addMinusPlus(centerX - 10, startY + rowHeight * 5, exhaustionBox, -5.0, 5.0, 1.0, 500.0, true);
        exhaustionBox.setTooltip(Tooltip.create(Component.literal("Hoeveel stamina je moet bijkomen na uitputting voordat je weer mag sprinten en springen. Standaard: 10 (1 icoon).")));

        // Knoppen onderaan
        int btnY = panelTop + PANEL_HEIGHT - 28;

        Button resetBtn = Button.builder(Component.literal("Standaard"), btn -> resetDefaults())
                .bounds(centerX - 165, btnY, 100, 20)
                .tooltip(Tooltip.create(Component.literal("Herstel alle waarden naar de standaard fabrieksinstellingen.")))
                .build();
        this.addRenderableWidget(resetBtn);

        Button saveBtn = Button.builder(Component.literal("Opslaan & Sluiten"), btn -> saveAndClose())
                .bounds(centerX - 55, btnY, 115, 20)
                .tooltip(Tooltip.create(Component.literal("Sla de instellingen direct op in het configuratiebestand en pas toe.")))
                .build();
        this.addRenderableWidget(saveBtn);

        Button cancelBtn = Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
                .bounds(centerX + 68, btnY, 97, 20)
                .tooltip(Tooltip.create(Component.literal("Sluit dit venster zonder wijzigingen op te slaan.")))
                .build();
        this.addRenderableWidget(cancelBtn);
    }

    private EditBox createNumberBox(int x, int y, String initialValue) {
        EditBox box = new EditBox(this.font, x, y, 52, 18, Component.literal(""));
        box.setValue(initialValue);
        this.addRenderableWidget(box);
        return box;
    }

    private void addMinusPlus(int boxX, int y, EditBox box, double minusStep, double plusStep, double min, double max, boolean isInt) {
        Button minus = Button.builder(Component.literal("-"), btn -> adjust(box, minusStep, min, max, isInt))
                .bounds(boxX - 22, y, 18, 18)
                .tooltip(Tooltip.create(Component.literal("Verlaag met " + (isInt ? (int) Math.abs(minusStep) : Math.abs(minusStep)))))
                .build();
        this.addRenderableWidget(minus);

        Button plus = Button.builder(Component.literal("+"), btn -> adjust(box, plusStep, min, max, isInt))
                .bounds(boxX + 56, y, 18, 18)
                .tooltip(Tooltip.create(Component.literal("Verhoog met " + (isInt ? (int) plusStep : plusStep))))
                .build();
        this.addRenderableWidget(plus);
    }

    private void adjust(EditBox box, double delta, double min, double max, boolean isInt) {
        try {
            double current = Double.parseDouble(box.getValue().trim());
            double next = Mth.clamp(current + delta, min, max);
            if (isInt) {
                box.setValue(String.valueOf(Math.round(next)));
            } else {
                box.setValue(String.format(Locale.ROOT, "%.2f", next));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void resetDefaults() {
        maxStaminaBox.setValue("100");
        sprintDrainBox.setValue("0.20");
        jumpCostBox.setValue("3.2");
        regenRateBox.setValue("0.60");
        regenDelayBox.setValue("20");
        exhaustionBox.setValue("10");
    }

    private void saveAndClose() {
        try {
            double maxVal = Double.parseDouble(maxStaminaBox.getValue().trim());
            StaminaConfig.MAX_STAMINA.set(Mth.clamp(maxVal, 10.0, 1000.0));
        } catch (NumberFormatException ignored) {}

        try {
            double sprintVal = Double.parseDouble(sprintDrainBox.getValue().trim());
            StaminaConfig.SPRINT_DRAIN.set(Mth.clamp(sprintVal, 0.01, 10.0));
        } catch (NumberFormatException ignored) {}

        try {
            double jumpVal = Double.parseDouble(jumpCostBox.getValue().trim());
            StaminaConfig.JUMP_COST.set(Mth.clamp(jumpVal, 0.0, 100.0));
        } catch (NumberFormatException ignored) {}

        try {
            double regenVal = Double.parseDouble(regenRateBox.getValue().trim());
            StaminaConfig.REGEN_RATE.set(Mth.clamp(regenVal, 0.05, 10.0));
        } catch (NumberFormatException ignored) {}

        try {
            int delayVal = Integer.parseInt(regenDelayBox.getValue().trim());
            StaminaConfig.REGEN_DELAY.set(Mth.clamp(delayVal, 0, 200));
        } catch (NumberFormatException ignored) {}

        try {
            double exhaustVal = Double.parseDouble(exhaustionBox.getValue().trim());
            StaminaConfig.EXHAUSTION_THRESHOLD.set(Mth.clamp(exhaustVal, 1.0, 500.0));
        } catch (NumberFormatException ignored) {}

        StaminaConfig.SPEC.save();
        StaminaClient.onConfigUpdated();
        onClose();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int panelWidth = PANEL_WIDTH;
        int panelHeight = PANEL_HEIGHT;
        int panelTop = (this.height - panelHeight) / 2;
        int panelLeft = centerX - panelWidth / 2;

        // Teken een strak donker Minecraft-paneel met rand
        drawPanel(guiGraphics, panelLeft, panelTop, panelWidth, panelHeight, PANEL_BORDER);

        // Titel
        drawBigCenteredString(guiGraphics, Component.literal("Stamina Instellingen"), centerX, panelTop + 10, 1.2F, 0xFFFF55);

        int startY = panelTop + 38;
        int rowHeight = 24;

        // Bereken realtime hulpwaarden zodat de speler direct ziet wat het effect is
        double currentMax = parseDoubleSafe(maxStaminaBox.getValue(), 100.0);
        double currentSprint = parseDoubleSafe(sprintDrainBox.getValue(), 0.20);
        double currentJump = parseDoubleSafe(jumpCostBox.getValue(), 3.2);
        double currentRegen = parseDoubleSafe(regenRateBox.getValue(), 0.60);
        int currentDelay = (int) Math.round(parseDoubleSafe(regenDelayBox.getValue(), 20.0));
        double currentExhaust = parseDoubleSafe(exhaustionBox.getValue(), 10.0);

        String maxHelp = String.format(Locale.ROOT, "%.0f pnt", currentMax);
        String sprintHelp = currentSprint > 0 ? String.format(Locale.ROOT, "~%.0fs sprint", currentMax / (currentSprint * 20.0)) : "oneindig";
        String jumpHelp = currentJump > 0 ? (int)(currentMax / currentJump) + " sprongen" : "gratis";
        String regenHelp = currentRegen > 0 ? String.format(Locale.ROOT, "~%.0fs herstel", currentMax / (currentRegen * 20.0)) : "geen";
        String delayHelp = String.format(Locale.ROOT, "%.1fs rust", currentDelay / 20.0);
        String exhaustHelp = String.format(Locale.ROOT, "%d%% nodig", (int) Math.round((currentExhaust / currentMax) * 100.0));

        // Rij 1: Max Stamina
        guiGraphics.drawString(this.font, "Maximale Stamina:", centerX - 165, startY + 5, TEXT_COLOR);
        guiGraphics.drawString(this.font, maxHelp, centerX + 80, startY + 5, 0x55FF55);

        // Rij 2: Sprintverbruik
        guiGraphics.drawString(this.font, "Sprintverbruik / tick:", centerX - 165, startY + rowHeight + 5, TEXT_COLOR);
        guiGraphics.drawString(this.font, sprintHelp, centerX + 80, startY + rowHeight + 5, 0x55FF55);

        // Rij 3: Springkosten
        guiGraphics.drawString(this.font, "Kosten per sprong:", centerX - 165, startY + rowHeight * 2 + 5, TEXT_COLOR);
        guiGraphics.drawString(this.font, jumpHelp, centerX + 80, startY + rowHeight * 2 + 5, 0x55FF55);

        // Rij 4: Herstelsnelheid
        guiGraphics.drawString(this.font, "Herstel / tick:", centerX - 165, startY + rowHeight * 3 + 5, TEXT_COLOR);
        guiGraphics.drawString(this.font, regenHelp, centerX + 80, startY + rowHeight * 3 + 5, 0x55FF55);

        // Rij 5: Herstelvertraging
        guiGraphics.drawString(this.font, "Herstelvertraging:", centerX - 165, startY + rowHeight * 4 + 5, TEXT_COLOR);
        guiGraphics.drawString(this.font, delayHelp, centerX + 80, startY + rowHeight * 4 + 5, 0x55FF55);

        // Rij 6: Uitputting
        guiGraphics.drawString(this.font, "Uitputting hersteldrempel:", centerX - 165, startY + rowHeight * 5 + 5, TEXT_COLOR);
        guiGraphics.drawString(this.font, exhaustHelp, centerX + 80, startY + rowHeight * 5 + 5, 0x55FF55);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private double parseDoubleSafe(String text, double fallback) {
        if (text == null) return fallback;
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
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
