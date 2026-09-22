package nl.tivek.welcomescreen.client.character;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.DirtBackgroundScreen;
import nl.tivek.welcomescreen.config.CharacterConfig;

/**
 * Changing one character's numbers in the game itself: the cooldown, the damage and every setting of
 * every ability, one ability at a time. What you change here is written into that character's own
 * settings file, the same file you could open by hand.
 */
public class CharacterConfigScreen extends DirtBackgroundScreen {
    private static final int PANEL_WIDTH = 380;
    // The panel is at least this high, and grows with an ability that has many numbers.
    private static final int PANEL_HEIGHT = 216;
    // The rows start this far below the top of the panel; the buttons need this much room under them.
    private static final int ROWS_TOP = 52;
    private static final int BUTTON_ROOM = 32;
    // Rows are this far apart, or closer (never closer than MIN_ROW_STEP) when they would not all fit.
    private static final int ROW_STEP = 24;
    private static final int MIN_ROW_STEP = 16;
    private static final String PREFIX = "screen." + WelcomeScreenMod.MODID + ".config.";

    /** One line on the screen: a number you can change, with what it means. */
    private final class Row {
        private final String key;
        private final Component label;
        private final double min;
        private final double max;
        private final double step;
        private final boolean whole;
        private EditBox box;

        private Row(String key, Component label, double min, double max, double step, boolean whole) {
            this.key = key;
            this.label = label;
            this.min = min;
            this.max = max;
            this.step = step;
            this.whole = whole;
        }

        private double value() {
            try {
                return Mth.clamp(Double.parseDouble(this.box.getValue().trim()), this.min, this.max);
            } catch (NumberFormatException wrong) {
                return CharacterConfigScreen.this.edits.getOrDefault(this.path(), this.min);
            }
        }

        private String path() {
            return CharacterConfigScreen.this.ability().path() + "." + this.key;
        }

        private String text(double value) {
            return this.whole ? String.valueOf(Math.round(value)) : String.format(Locale.ROOT, "%.2f", value);
        }
    }

    private final Screen lastScreen;
    private final GameCharacter character;
    // Everything you changed, by "doc_ock.portal.damage", until you press save.
    private final Map<String, Double> edits = new HashMap<>();
    private final List<Row> rows = new ArrayList<>();
    private int index;

    public CharacterConfigScreen(Screen lastScreen, GameCharacter character) {
        super(character.getDisplayName());
        this.lastScreen = lastScreen;
        this.character = character;
    }

    private CharacterAbility ability() {
        return this.character.abilities().get(this.index);
    }

    /** The number in the file, or the one you already typed on this screen. */
    private double current(String key, double fromFile) {
        return this.edits.getOrDefault(this.ability().path() + "." + key, fromFile);
    }

    @Override
    protected void init() {
        super.init();
        this.rows.clear();
        if (this.character.abilities().isEmpty()) {
            this.addCloseButtons();
            return;
        }
        CharacterAbility ability = this.ability();
        this.rows.add(new Row("cooldownTicks", Component.translatable(PREFIX + "cooldown"), 0, 72000, 10, true));
        this.rows.add(new Row("damage", Component.translatable(PREFIX + "damage"), 0.0, 2000.0, 1.0, false));
        for (CharacterAbility.Setting setting : ability.settings()) {
            double step = setting.whole() ? 1.0 : (setting.max() <= 1.0 ? 0.05 : setting.max() <= 20.0 ? 0.25 : 1.0);
            this.rows.add(new Row(setting.key(), Component.literal(setting.key()), setting.min(), setting.max(),
                    step, setting.whole()));
        }

        int centerX = this.width / 2;
        int panelTop = (this.height - this.panelHeight()) / 2;
        int startY = panelTop + ROWS_TOP;
        int step = this.rowStep();
        int tall = this.rowHeight();

        // Flipping through the abilities.
        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> this.step(-1))
                .bounds(centerX - 175, panelTop + 26, 20, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> this.step(1))
                .bounds(centerX + 155, panelTop + 26, 20, 18).build());

        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            int y = startY + i * step;
            double value = row.key.equals("cooldownTicks")
                    ? this.current(row.key, ability.getCooldown())
                    : row.key.equals("damage") ? this.current(row.key, ability.getDamage())
                            : this.current(row.key, ability.value(row.key));
            row.box = new EditBox(this.font, centerX + 18, y, 52, tall, Component.empty());
            row.box.setValue(row.text(value));
            this.addRenderableWidget(row.box);
            this.addRenderableWidget(Button.builder(Component.literal("-"), button -> this.adjust(row, -row.step))
                    .bounds(centerX - 4, y, 18, tall).build());
            this.addRenderableWidget(Button.builder(Component.literal("+"), button -> this.adjust(row, row.step))
                    .bounds(centerX + 74, y, 18, tall).build());
            for (CharacterAbility.Setting setting : ability.settings()) {
                if (setting.key().equals(row.key)) {
                    row.box.setTooltip(Tooltip.create(Component.literal(setting.comment())));
                }
            }
        }
        this.addCloseButtons();
    }

    private void addCloseButtons() {
        int centerX = this.width / 2;
        int y = (this.height - this.panelHeight()) / 2 + this.panelHeight() - 26;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "defaults"), b -> this.defaults())
                .bounds(centerX - 175, y, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save"), b -> this.saveAndClose())
                .bounds(centerX - 60, y, 120, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, b -> this.onClose())
                .bounds(centerX + 75, y, 100, 20).build());
    }

    /** How high the panel is: room for every row of this ability, and the buttons under them. */
    private int panelHeight() {
        return Math.max(PANEL_HEIGHT, ROWS_TOP + this.rows.size() * this.rowStep() + BUTTON_ROOM);
    }

    /** The distance between two rows: roomy, or tighter when all of them would not fit on the screen. */
    private int rowStep() {
        int room = (this.height - 8 - ROWS_TOP - BUTTON_ROOM) / Math.max(1, this.rows.size());
        return Mth.clamp(room, MIN_ROW_STEP, ROW_STEP);
    }

    /** How high a row's box and buttons are: a little less than the distance between two rows. */
    private int rowHeight() {
        return Math.min(18, this.rowStep() - 2);
    }

    /** Another ability; what you typed is kept. */
    private void step(int by) {
        this.remember();
        int count = this.character.abilities().size();
        this.index = Math.floorMod(this.index + by, count);
        this.rebuildWidgets();
    }

    private void adjust(Row row, double by) {
        double next = Mth.clamp(row.value() + by, row.min, row.max);
        row.box.setValue(row.text(next));
    }

    /** Puts this ability's numbers back to what the mod itself says they are. */
    private void defaults() {
        if (this.rows.isEmpty()) {
            return;
        }
        CharacterAbility ability = this.ability();
        for (Row row : this.rows) {
            double value = switch (row.key) {
                case "cooldownTicks" -> ability.defaultCooldown();
                case "damage" -> ability.defaultDamage();
                default -> settingDefault(ability, row.key);
            };
            row.box.setValue(row.text(value));
        }
    }

    private static double settingDefault(CharacterAbility ability, String key) {
        for (CharacterAbility.Setting setting : ability.settings()) {
            if (setting.key().equals(key)) {
                return setting.value();
            }
        }
        return 0.0;
    }

    /** Keeps what is on the screen right now, so flipping to another ability loses nothing. */
    private void remember() {
        for (Row row : this.rows) {
            this.edits.put(row.path(), row.value());
        }
    }

    private void saveAndClose() {
        this.remember();
        for (CharacterAbility ability : this.character.abilities()) {
            for (Map.Entry<String, Double> entry : this.edits.entrySet()) {
                String start = ability.path() + ".";
                if (!entry.getKey().startsWith(start)) {
                    continue;
                }
                String key = entry.getKey().substring(start.length());
                switch (key) {
                    case "cooldownTicks" -> CharacterConfig.setCooldown(ability, (int) Math.round(entry.getValue()));
                    case "damage" -> CharacterConfig.setDamage(ability, entry.getValue());
                    default -> CharacterConfig.setValue(ability, key, entry.getValue());
                }
            }
        }
        CharacterConfig.save(this.character);
        this.onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int centerX = this.width / 2;
        int panelTop = (this.height - this.panelHeight()) / 2;
        drawPanel(graphics, centerX - PANEL_WIDTH / 2, panelTop, PANEL_WIDTH, this.panelHeight(), PANEL_BORDER);
        this.drawBigCenteredString(graphics, this.character.getDisplayName(), centerX, panelTop + 8, 1.2F,
                0xFF000000 | this.character.getColor());

        if (this.character.abilities().isEmpty()) {
            graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "no_abilities"), centerX,
                    panelTop + 60, MUTED_COLOR);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }
        CharacterAbility ability = this.ability();
        Component name = Component.translatable(PREFIX + "ability", ability.slot().number(),
                ability.getDisplayName());
        graphics.drawCenteredString(this.font, name, centerX, panelTop + 30, TEXT_COLOR);
        if (!CharacterConfig.canEdit(this.character)) {
            graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "locked"), centerX,
                    panelTop + 42, NOTICE_COLOR);
        } else if (this.minecraft != null && this.minecraft.level != null
                && !this.minecraft.hasSingleplayerServer()) {
            // On someone else's server their own file decides; this only changes your own copy.
            graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "server_decides"), centerX,
                    panelTop + 42, NOTICE_COLOR);
        }

        int startY = panelTop + ROWS_TOP;
        int step = this.rowStep();
        // The writing sits level with the text in the box.
        int textY = (this.rowHeight() - 8) / 2;
        for (int i = 0; i < this.rows.size(); i++) {
            Row row = this.rows.get(i);
            int y = startY + i * step;
            graphics.drawString(this.font, row.label, centerX - 175, y + textY, TEXT_COLOR);
            graphics.drawString(this.font, this.help(row), centerX + 98, y + textY, 0x55FF55);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** What the number means in plain words: seconds, hearts, or simply the number itself. */
    private Component help(Row row) {
        double value = row.value();
        return switch (row.key) {
            case "cooldownTicks" -> value <= 0 ? Component.translatable(PREFIX + "instant")
                    : Component.translatable(PREFIX + "seconds", String.format(Locale.ROOT, "%.1f", value / 20.0));
            case "damage" -> value <= 0 ? Component.translatable(PREFIX + "no_damage")
                    : Component.translatable(PREFIX + "hearts", String.format(Locale.ROOT, "%.1f", value / 2.0));
            default -> Component.empty();
        };
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.lastScreen);
        }
    }
}
