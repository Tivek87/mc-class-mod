package nl.tivek.multiversepowers.engine.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;

// A box of rows drawn by hand that scrolls: `begin` clips to it and says where the rows start, `end` draws the bar.
// What is clicked in it is kept per frame (`hit`) and found again by `click`, only inside the box.
public final class ScrollArea {
    private static final int BAR = 3;

    private record Hit(int x, int y, int width, int height, Runnable action) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private int x;
    private int top;
    private int width;
    private int bottom;
    private int content;
    private double scroll;

    // Clips drawing to the box for rows `content` high in all; returns the y of the first row.
    public int begin(GuiGraphics graphics, int x, int y, int width, int height, int content) {
        this.hits.clear();
        this.x = x;
        this.top = y;
        this.width = width;
        this.bottom = y + height;
        this.content = content;
        this.scroll = Math.max(0.0, Math.min(this.most(), this.scroll));
        graphics.enableScissor(x, y, x + width, this.bottom);
        return y - (int) this.scroll;
    }

    public void end(GuiGraphics graphics) {
        graphics.disableScissor();
        double most = this.most();
        if (most <= 0) {
            return;
        }
        int track = this.bottom - this.top;
        int thumb = Math.max(12, (int) (track * (double) track / this.content));
        int at = this.top + (int) ((track - thumb) * (this.scroll / most));
        int left = this.x + this.width - BAR;
        graphics.fill(left, this.top, left + BAR, this.bottom, 0x30000000);
        graphics.fill(left, at, left + BAR, at + thumb, 0xFF6A6A6A);
    }

    // The width rows may use, leaving room for the bar while there is one.
    public int rowWidth() {
        return this.width - (this.most() > 0 ? BAR + 3 : 0);
    }

    public boolean shows(int y, int height) {
        return y + height > this.top && y < this.bottom;
    }

    public boolean over(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= Math.max(y, this.top)
                && mouseY < Math.min(y + height, this.bottom) && mouseY >= this.top && mouseY < this.bottom;
    }

    public void hit(int x, int y, int width, int height, Runnable action) {
        int from = Math.max(y, this.top);
        int to = Math.min(y + height, this.bottom);
        if (to > from) {
            this.hits.add(new Hit(x, from, width, to - from, action));
        }
    }

    // Runs what lies under the pointer, if anything; whether it did.
    public boolean click(double mouseX, double mouseY) {
        for (Hit hit : List.copyOf(this.hits)) {
            if (mouseX >= hit.x() && mouseX < hit.x() + hit.width() && mouseY >= hit.y()
                    && mouseY < hit.y() + hit.height()) {
                hit.action().run();
                return true;
            }
        }
        return false;
    }

    public void scroll(double rows, int rowHeight) {
        this.scroll -= rows * rowHeight * 2;
    }

    public void top() {
        this.scroll = 0.0;
    }

    private double most() {
        return Math.max(0, this.content - (this.bottom - this.top));
    }
}
