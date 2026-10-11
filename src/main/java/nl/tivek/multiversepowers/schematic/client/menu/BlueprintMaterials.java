package nl.tivek.multiversepowers.schematic.client.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprint;
import nl.tivek.multiversepowers.schematic.client.SchematicClient;
import nl.tivek.multiversepowers.schematic.client.blueprint.ClientBlueprints;

// The blueprints page's materials: per kind of block, how many are still to place, how many the plan holds in all and
// how many you carry, with a plain word on each (done, enough, or how many more you need); shown all, still to place
// or short only, in three orders, and copied as text.
abstract class BlueprintMaterials extends SchematicScreen {
    static final int LINE = 20;
    static final int GOOD = 0xFF8CE08C;
    static final int BAD = 0xFFEF7B6E;
    static final int SOFT = 0xFFE9C46A;
    private static final int LEFT_COLUMN = 58;
    private static final int TOTAL_COLUMN = 50;
    private static final int HAVE_COLUMN = 56;
    private static final int STATUS_COLUMN = 76;
    private static final int HEAD = 44;
    private static final int NAME_ROOM = 90;

    // All, only what is still to place, or only what you are short of.
    static int filter;
    // Most still to place, by name, or most short first.
    static int order;
    private double linesScroll;
    private int linesY;
    private int linesHeight;
    // The number columns' widths, narrowed together where the page is too narrow to leave the names room.
    private int leftColumn = LEFT_COLUMN;
    private int totalColumn = TOTAL_COLUMN;
    private int haveColumn = HAVE_COLUMN;
    private int statusColumn = STATUS_COLUMN;

    protected BlueprintMaterials(@Nullable Screen root, String page) {
        super(root, page);
    }

    // One kind of block as shown: what you carry of it, and how many more you need.
    record Row(ClientBlueprints.Line line, int carried, int short_) {
    }

    List<Row> rows(ClientBlueprints.Progress progress) {
        List<Row> rows = new ArrayList<>();
        for (ClientBlueprints.Line line : progress.lines()) {
            int carried = this.carried(line.item());
            Row row = new Row(line, carried, Math.max(0, line.left() - carried));
            if (filter == 1 && line.left() == 0 || filter == 2 && row.short_() == 0) {
                continue;
            }
            rows.add(row);
        }
        Comparator<Row> by = switch (order) {
            case 1 -> Comparator.comparing(row -> row.line().item().getDescription().getString());
            case 2 -> Comparator.comparingInt((Row row) -> -row.short_()).thenComparingInt(row -> -row.line().left());
            default -> Comparator.comparingInt((Row row) -> row.line().left() == 0 ? 1 : 0).thenComparingInt(row ->
                    -row.line().left());
        };
        rows.sort(by);
        return rows;
    }

    // NavScreen has an Item of its own.
    private int carried(net.minecraft.world.item.Item item) {
        return this.minecraft == null || this.minecraft.player == null ? 0 : this.minecraft.player.getInventory()
                .countItem(item);
    }

    // The whole materials part: the sum at the top, the filter, order and copy chips, the column names and the rows.
    void materials(GuiGraphics graphics, Blueprint blueprint, ClientBlueprints.Progress progress, int x, int y,
            int width, int height, int mouseX, int mouseY) {
        ScreenAnchors.report("schematic.materials", x, y, width, height);
        List<Row> rows = this.rows(progress);
        int left = 0;
        int kinds = 0;
        int enough = 0;
        for (ClientBlueprints.Line line : progress.lines()) {
            left += line.left();
            if (line.left() > 0) {
                kinds++;
                if (this.carried(line.item()) >= line.left()) {
                    enough++;
                }
            }
        }
        if (progress.schematic() == null) {
            graphics.drawString(this.font, this.fit(Component.literal(blueprint.name()), width), x, y, TEXT, false);
            this.wrap(graphics, Component.translatable(PREFIX + (progress.failed() ? "blueprint.failed"
                    : "blueprint.loading")), x, y + 14, width, progress.failed() ? BAD : MUTED, 2);
            return;
        }
        Component sum = left == 0 ? Component.translatable(PREFIX + "materials.all_done")
                : Component.translatable(PREFIX + "materials.left", String.format("%,d", left), String.format("%,d",
                        progress.needed()));
        graphics.drawString(this.font, this.fit(sum, width), x, y + 11, left == 0 ? GOOD : BODY, false);
        if (kinds > 0) {
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "materials.enough", enough, kinds),
                    width), x, y + 21, enough == kinds ? GOOD : SOFT, false);
        }
        int chipX = x + width;
        chipX -= this.font.width(Component.translatable(PREFIX + "materials.copy")) + 6;
        this.smallButton(graphics, Component.translatable(PREFIX + "materials.copy"), chipX, y, mouseX, mouseY,
                !progress.lines().isEmpty(), () -> this.copy(blueprint, progress));
        Component sortChip = Component.translatable(PREFIX + "materials.order." + order);
        chipX -= this.font.width(sortChip) + 9;
        this.smallButton(graphics, sortChip, chipX, y, mouseX, mouseY, true, () -> {
            order = (order + 1) % 3;
            this.click(1.0F);
        });
        Component filterChip = Component.translatable(PREFIX + "materials.filter." + filter);
        chipX -= this.font.width(filterChip) + 9;
        this.smallButton(graphics, filterChip, chipX, y, mouseX, mouseY, true, () -> {
            filter = (filter + 1) % 3;
            this.linesScroll = 0.0;
            this.click(1.0F);
        });
        graphics.drawString(this.font, this.fit(Component.literal(blueprint.name()), chipX - x - 6), x, y, TEXT,
                false);
        this.columns(width);
        int headY = y + HEAD - 12;
        graphics.fill(x, headY - 2, x + width, headY + 10, 0x50000000);
        int right = x + width - 4;
        this.head(graphics, "materials.status", right - this.statusColumn + 4, headY, false);
        right -= this.statusColumn;
        this.head(graphics, "materials.have", right, headY, true);
        right -= this.haveColumn;
        this.head(graphics, "materials.total", right, headY, true);
        right -= this.totalColumn;
        this.head(graphics, "materials.to_place", right, headY, true);
        this.head(graphics, "materials.block", x + 4, headY, false);
        this.linesY = y + HEAD;
        this.linesHeight = y + height - this.linesY;
        graphics.fill(x, this.linesY, x + width, y + height, 0x40000000);
        if (rows.isEmpty()) {
            this.wrap(graphics, Component.translatable(PREFIX + (progress.lines().isEmpty() ? "materials.none"
                    : "materials.filtered")), x + 6, this.linesY + 6, width - 12, MUTED, 3);
            return;
        }
        graphics.enableScissor(x, this.linesY, x + width, y + height);
        int top = this.linesY + 1 - (int) Math.round(this.linesScroll);
        for (int k = 0; k < rows.size(); k++) {
            int rowY = top + k * LINE;
            if (rowY + LINE < this.linesY || rowY > y + height) {
                continue;
            }
            this.row(graphics, rows.get(k), x, rowY, width, mouseX, mouseY);
        }
        graphics.disableScissor();
    }

    private void columns(int width) {
        int all = LEFT_COLUMN + TOTAL_COLUMN + HAVE_COLUMN + STATUS_COLUMN;
        float scale = Math.min(1.0F, Math.max(0.6F, (width - NAME_ROOM - 28) / (float) all));
        this.leftColumn = this.column(LEFT_COLUMN, scale, "materials.to_place");
        this.totalColumn = this.column(TOTAL_COLUMN, scale, "materials.total");
        this.haveColumn = this.column(HAVE_COLUMN, scale, "materials.have");
        this.statusColumn = this.column(STATUS_COLUMN, scale, "materials.status");
    }

    // Never narrower than its name at the top.
    private int column(int full, float scale, String head) {
        return Math.max(Math.round(full * scale), this.font.width(Component.translatable(PREFIX + head)) + 8);
    }

    private void head(GuiGraphics graphics, String key, int at, int y, boolean right) {
        Component text = Component.translatable(PREFIX + key);
        graphics.drawString(this.font, text, right ? at - this.font.width(text) : at, y, MUTED, false);
    }

    private void row(GuiGraphics graphics, Row row, int x, int y, int width, int mouseX, int mouseY) {
        ClientBlueprints.Line line = row.line();
        boolean done = line.left() == 0;
        boolean enough = !done && row.short_() == 0;
        graphics.fill(x + 1, y, x + width - 1, y + LINE - 1, done ? 0x10FFFFFF : enough ? 0x1E4CC864 : 0x22E0503C);
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + LINE && mouseY >= this.linesY) {
            graphics.fill(x + 1, y, x + width - 1, y + LINE - 1, 0x18FFFFFF);
        }
        graphics.renderItem(new ItemStack(line.item()), x + 3, y + 1);
        int right = x + width - 4;
        int statusX = right - this.statusColumn + 4;
        Component status = done ? Component.translatable(PREFIX + "materials.done") : enough
                ? Component.translatable(PREFIX + "materials.ready") : Component.translatable(PREFIX
                        + "materials.need", String.format("%,d", row.short_()));
        if (!done && !enough && this.font.width(status) > this.statusColumn - 6) {
            status = Component.translatable(PREFIX + "materials.need.short", String.format("%,d", row.short_()));
        }
        graphics.drawString(this.font, this.fit(status, this.statusColumn - 6), statusX, y + 6, done ? MUTED : enough
                ? GOOD : BAD, false);
        right -= this.statusColumn;
        this.number(graphics, row.carried(), right, y + 6, done ? DIM : enough ? GOOD : SOFT);
        right -= this.haveColumn;
        this.number(graphics, line.needed(), right, y + 6, MUTED);
        right -= this.totalColumn;
        this.number(graphics, line.left(), right, y + 6, done ? DIM : TEXT);
        right -= this.leftColumn;
        int nameWidth = right - x - 24;
        graphics.drawString(this.font, this.fit(line.item().getDescription(), nameWidth), x + 23, y + 3, done ? MUTED
                : BODY, false);
        float placed = line.needed() == 0 ? 1.0F : 1.0F - line.left() / (float) line.needed();
        int barWidth = Math.max(10, Math.min(nameWidth, 80));
        graphics.fill(x + 23, y + 13, x + 23 + barWidth, y + 15, 0xFF2A2A2A);
        graphics.fill(x + 23, y + 13, x + 23 + Math.round(barWidth * placed), y + 15, done ? 0xFF5E9E5E
                : 0xFF000000 | COLOR);
    }

    private void number(GuiGraphics graphics, int value, int right, int y, int color) {
        String text = String.format("%,d", value);
        graphics.drawString(this.font, text, right - this.font.width(text), y, color, false);
    }

    // The list as text on the clipboard, one kind a line, what is still to place first.
    private void copy(Blueprint blueprint, ClientBlueprints.Progress progress) {
        StringBuilder text = new StringBuilder(blueprint.name()).append('\n');
        int saved = filter;
        filter = 0;
        for (Row row : this.rows(progress)) {
            ClientBlueprints.Line line = row.line();
            text.append(line.item().getDescription().getString()).append(": ").append(line.left()).append(" / ")
                    .append(line.needed()).append(" (").append(Component.translatable(PREFIX + "materials.have")
                            .getString()).append(' ').append(row.carried()).append(")\n");
        }
        filter = saved;
        this.minecraft.keyboardHandler.setClipboard(text.toString());
        SchematicClient.say(Component.translatable(PREFIX + "materials.copied"), false);
        this.click(1.2F);
    }

    // The rows scroll under the pointer; true when it was over them.
    boolean scrollMaterials(ClientBlueprints.Progress progress, double mouseY, double scrollY) {
        if (mouseY < this.linesY || mouseY >= this.linesY + this.linesHeight) {
            return false;
        }
        int most = Math.max(0, this.rows(progress).size() * LINE + 2 - this.linesHeight);
        this.linesScroll = Mth.clamp(this.linesScroll - scrollY * LINE * 2, 0.0, most);
        return true;
    }

    void resetMaterials() {
        this.linesScroll = 0.0;
    }
}
