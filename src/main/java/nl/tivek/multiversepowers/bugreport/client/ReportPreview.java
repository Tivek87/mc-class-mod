package nl.tivek.multiversepowers.bugreport.client;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import nl.tivek.multiversepowers.bugreport.client.form.Block;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;

// The report as it will be sent, to read once more before Submit: its title, category and priority, then every block,
// a table as "question: answer" lines, in a box that scrolls.
final class ReportPreview {
    private static final int SCROLL_STEP = 18;

    private record Line(FormattedCharSequence text, int color, int x, int gap) {
    }

    private final Font font;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private final List<Line> lines = new ArrayList<>();
    private final int total;
    private double scroll;

    ReportPreview(Font font, BugReporter.Submission report, int x, int y, int width, int height) {
        this.font = font;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        int inner = width - 18;
        this.add(BugReportScreen.words("preview.note").withColor(0x8E8E8E), inner, 0xFF8E8E8E, 0, 0);
        this.add(Component.literal(report.title()).withStyle(ChatFormatting.BOLD), inner, 0xFFFFFFFF, 0, 6);
        Component facts = BugReportScreen.words("preview.facts",
                BugReportScreen.words("priority." + report.priority().id()).withColor(report.priority().color),
                BugReporter.username());
        if (!report.categoryName().isEmpty()) {
            facts = Component.literal(report.categoryName() + "  ·  ").withColor(0x6EE7A0).append(facts);
        }
        this.add(facts, inner, 0xFFB8B8B8, 0, 2);
        for (Block block : report.blocks()) {
            this.add(Component.literal(block.title()).withColor(NavScreen.ACCENT), inner, 0xFFFFFFFF, 0, 8);
            if (block.isTable()) {
                for (List<String> row : block.rows()) {
                    Component line = row.size() > 2
                            ? Component.literal(row.get(0) + ". ").withColor(0x8E8E8E)
                                    .append(Component.literal(row.get(1)).withColor(0xFFFFFF))
                                    .append(Component.literal(row.get(2).isEmpty() ? "" : ": " + row.get(2))
                                            .withColor(0xC8C8C8))
                            : Component.literal(row.get(0) + ": ").withColor(0x8E8E8E)
                                    .append(Component.literal(row.get(1)).withColor(0xFFFFFF));
                    this.add(line, inner - 6, 0xFFFFFFFF, 6, 1);
                }
            } else {
                for (String paragraph : block.text().split("\n")) {
                    this.add(Component.literal(paragraph.isBlank() ? " " : paragraph), inner - 6, 0xFFD2D2D2, 6, 1);
                }
            }
        }
        int sum = 6;
        for (Line line : this.lines) {
            sum += line.gap() + 10;
        }
        this.total = sum;
    }

    private void add(Component text, int width, int color, int indent, int gap) {
        List<FormattedCharSequence> split = this.font.split(text, width);
        for (int i = 0; i < split.size(); i++) {
            this.lines.add(new Line(split.get(i), color, indent, i == 0 ? gap : 0));
        }
    }

    void render(GuiGraphics graphics) {
        NavScreen.card(graphics, this.x, this.y, this.width, this.height, NavScreen.ACCENT);
        this.scroll = Math.max(0, Math.min(Math.max(0, this.total - this.height), this.scroll));
        graphics.enableScissor(this.x, this.y + 1, this.x + this.width, this.y + this.height - 1);
        int at = this.y + 5 - (int) this.scroll;
        for (Line line : this.lines) {
            at += line.gap();
            if (at + 10 > this.y && at < this.y + this.height) {
                graphics.drawString(this.font, line.text(), this.x + 8 + line.x(), at, line.color(), false);
            }
            at += 10;
        }
        graphics.disableScissor();
        if (this.total > this.height) {
            int thumb = Math.max(14, this.height * this.height / this.total);
            int top = this.y + (int) Math.round((this.height - thumb) * this.scroll / (this.total - this.height));
            graphics.fill(this.x + this.width - 3, top, this.x + this.width, top + thumb, 0xFF8A8A8A);
        }
    }

    boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        if (mouseX < this.x || mouseX >= this.x + this.width || mouseY < this.y || mouseY >= this.y + this.height) {
            return false;
        }
        this.scroll -= scrollY * SCROLL_STEP;
        return true;
    }
}
