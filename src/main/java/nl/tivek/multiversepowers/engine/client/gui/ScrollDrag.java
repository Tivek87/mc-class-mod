package nl.tivek.multiversepowers.engine.client.gui;

import net.minecraft.util.Mth;

// A scroll bar the mouse takes hold of: told each frame where it was drawn (`bar`, or `hide` when it was not), a press
// on its thumb takes hold where it was pressed, one on its track beside the thumb takes the thumb's middle there, and a
// drag then moves the scroll until the button is let go.
public final class ScrollDrag {
    // How far either side of a thin bar a press still takes it.
    private static final int REACH = 3;

    private boolean shown;
    private int x;
    private int width;
    private double top;
    private double track;
    private double thumb;
    private double thumbTop;
    private double most;
    private boolean held;
    private double grab;

    public void bar(int x, int width, double top, double track, double thumb, double thumbTop, double most) {
        this.shown = most > 0.0 && track > thumb;
        this.x = x;
        this.width = width;
        this.top = top;
        this.track = track;
        this.thumb = thumb;
        this.thumbTop = thumbTop;
        this.most = most;
    }

    public void hide() {
        this.shown = false;
        this.held = false;
    }

    public boolean press(double mouseX, double mouseY) {
        if (!this.shown || mouseX < this.x - REACH || mouseX > this.x + this.width + REACH || mouseY < this.top
                || mouseY > this.top + this.track) {
            return false;
        }
        boolean onThumb = mouseY >= this.thumbTop && mouseY <= this.thumbTop + this.thumb;
        this.grab = onThumb ? mouseY - this.thumbTop : this.thumb * 0.5;
        this.held = true;
        return true;
    }

    public boolean held() {
        return this.held;
    }

    // The scroll that puts the thumb under the pointer, where it was taken hold of.
    public double to(double mouseY) {
        return Mth.clamp((mouseY - this.grab - this.top) / (this.track - this.thumb) * this.most, 0.0, this.most);
    }

    public void release() {
        this.held = false;
    }
}
