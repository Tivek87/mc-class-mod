package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// The guide's rows under `AbilityGuideScreen`: the open tab's binds in order, which one is chosen, how far the list and
// the detail are scrolled, and what a guided tour may point at or ask to see.
abstract class GuideRows extends Screen {
    static final String PREFIX = "screen." + MultiversePowers.MODID + ".guide.";
    static final int ROW = 15;
    static final int HEADING = 14;
    static final int TAB = 15;
    static final int INDENT = 9;
    static final Component AGAIN = Component.translatable(PREFIX + "again");

    final GameCharacter character;
    final List<GuideMode> modes;
    // The open tab's rows: null is its overview, then its controls and headings in order.
    final List<GuideMode.Control> rows = new ArrayList<>();
    int[] tabX = new int[0];
    int[] tabY = new int[0];
    int[] tabWidth = new int[0];
    final GuideAbout about;
    // Whether the chosen control's full explanation is unfolded, and where its fold line was drawn.
    boolean more;
    int fold = -1;
    int tab;
    int selected;
    double listScroll;
    double detailScroll;
    int detailHeight;
    int left;
    int top;
    int windowWidth;
    int windowHeight;
    int listX;
    int listY;
    int listWidth;
    int bodyHeight;
    int detailX;
    int detailWidth;

    GuideRows(GameCharacter character) {
        super(Component.translatable(PREFIX + "title", character.getDisplayName()));
        this.character = character;
        this.modes = AbilityGuide.modes(character);
        this.about = new GuideAbout(character);
        LocalPlayer player = Minecraft.getInstance().player;
        for (int i = 0; player != null && i < this.modes.size(); i++) {
            if (this.modes.get(i).active().test(player)) {
                this.tab = i;
                break;
            }
        }
        this.fill();
    }

    private void fill() {
        this.rows.clear();
        if (this.tab < this.modes.size()) {
            this.rows.add(null);
            this.rows.addAll(this.modes.get(this.tab).controls());
        }
    }

    void openAbout(boolean open) {
        if (this.about.open(open)) {
            this.click(open ? 1.2F : 1.0F);
        }
    }

    void click(float pitch) {
        if (this.minecraft != null) {
            this.minecraft.getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, 0.3F));
        }
    }

    boolean onFold(double mouseX, double mouseY) {
        if (this.about.open() || this.fold < 0 || mouseX < this.detailX || mouseX >= this.detailX + this.detailWidth) {
            return false;
        }
        double foldTop = this.listY - this.detailScroll + this.fold;
        return mouseY >= foldTop && mouseY < foldTop + GuideDetail.FOLD && mouseY >= this.listY
                && mouseY < this.listY + this.bodyHeight - GuideDetail.STATUS;
    }

    // Opens another tab on the same control when it has one, else on its overview.
    void open(int index) {
        if (index < 0 || index >= this.modes.size() || index == this.tab) {
            return;
        }
        GuideMode.Control was = this.rows.isEmpty() ? null : this.rows.get(this.selected);
        this.tab = index;
        this.fill();
        int found = 0;
        for (int i = 1; was != null && i < this.rows.size(); i++) {
            if (!this.heading(i) && this.rows.get(i).id().equals(was.id())) {
                found = i;
                break;
            }
        }
        this.listScroll = 0.0;
        this.selected = -1;
        this.select(found);
        this.click(1.4F);
    }

    boolean heading(int index) {
        GuideMode.Control row = this.rows.get(index);
        return row != null && row.heading();
    }

    int height(int index) {
        return this.heading(index) ? HEADING : ROW;
    }

    // The next row that is no heading from `from` going `step`, or `from` when there is none.
    int next(int from, int step) {
        for (int i = from + step; i >= 0 && i < this.rows.size(); i += step) {
            if (!this.heading(i)) {
                return i;
            }
        }
        return from;
    }

    void select(int index) {
        if (index < 0 || index >= this.rows.size() || this.heading(index)) {
            return;
        }
        if (index != this.selected) {
            this.detailScroll = 0.0;
        }
        this.selected = index;
        int start = this.offset(index);
        if (index > 0 && this.heading(index - 1)) {
            start -= HEADING;
        }
        int end = this.offset(index) + ROW + 4;
        if (start < this.listScroll) {
            this.listScroll = start;
        } else if (end > this.listScroll + this.bodyHeight) {
            this.listScroll = end - this.bodyHeight;
        }
        this.listScroll = Mth.clamp(this.listScroll, 0.0, this.maxListScroll());
    }

    // A row's top within the list before scrolling.
    int offset(int index) {
        int y = 4;
        for (int i = 0; i < index; i++) {
            y += this.height(i);
        }
        return y;
    }

    double maxListScroll() {
        return Math.max(0, this.offset(this.rows.size()) + 4 - this.bodyHeight);
    }

    double maxDetailScroll() {
        return Math.max(0, this.detailHeight - (this.bodyHeight - GuideDetail.STATUS));
    }

    int rowAt(double mouseX, double mouseY) {
        if (mouseX < this.listX || mouseX >= this.listX + this.listWidth || mouseY < this.listY
                || mouseY >= this.listY + this.bodyHeight) {
            return -1;
        }
        double y = this.listY - this.listScroll;
        for (int i = 0; i < this.rows.size(); i++) {
            double rowTop = y + this.offset(i);
            if (mouseY >= rowTop && mouseY < rowTop + this.height(i)) {
                return i;
            }
        }
        return -1;
    }

    int tabAt(double mouseX, double mouseY) {
        for (int i = 0; i < this.tabX.length; i++) {
            if (mouseX >= this.tabX[i] && mouseX < this.tabX[i] + this.tabWidth[i] && mouseY >= this.tabY[i]
                    && mouseY < this.tabY[i] + TAB) {
                return i;
            }
        }
        return -1;
    }

    static FormattedCharSequence fit(Font font, Component text, int width) {
        if (font.width(text) <= width) {
            return text.getVisualOrderText();
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width("...")));
        return Component.literal(cut + "...").withStyle(text.getStyle()).getVisualOrderText();
    }

    // The first bind of this tab with others hanging under it, or -1.
    int group() {
        for (int i = 1; i + 1 < this.rows.size(); i++) {
            GuideMode.Control row = this.rows.get(i);
            if (!row.heading() && row.id().equals(this.rows.get(i + 1).parent())) {
                return i;
            }
        }
        return -1;
    }

    int groupEnd(int start) {
        int end = start;
        while (end + 1 < this.rows.size() && this.rows.get(end + 1).child()) {
            end++;
        }
        return end;
    }

    // A guided tour asks to see the overview's row or the first binds that belong together: the list glides to it,
    // on the first tab that has such binds when this one has none.
    void reveal(float ease) {
        String what = AbilityGuide.revealing();
        if (what == null || this.about.open() || this.rows.isEmpty()) {
            return;
        }
        int from = 0;
        int to = 0;
        if (what.equals(AbilityGuide.PAIR)) {
            for (int i = 0; this.group() < 0 && i < this.modes.size(); i++) {
                if (this.modes.get(i).controls().stream().anyMatch(GuideMode.Control::child)) {
                    this.open(i);
                }
            }
            from = this.group();
            if (from < 0) {
                return;
            }
            to = this.groupEnd(from);
        }
        double wanted = Math.min(Math.max(this.listScroll, this.offset(to) + this.height(to) + 4 - this.bodyHeight),
                this.offset(from) - 4);
        wanted = Mth.clamp(wanted, 0.0, this.maxListScroll());
        this.listScroll += (wanted - this.listScroll) * ease;
    }

    // What a guided tour can point at: the window, its summary (from `GuideAbout`), the list, the detail as an overview
    // or a control, the overview's row and the first binds that belong together, each while wholly in view.
    void anchors(@Nullable GuideMode.Control chosen) {
        ScreenAnchors.report("guide.list", this.listX, this.listY, this.listWidth, this.bodyHeight);
        ScreenAnchors.report(chosen == null ? "guide.overview" : "guide.control", this.detailX - 3, this.listY - 3,
                this.detailWidth + 6, this.bodyHeight + 6);
        this.rowAnchor("guide.how", 0, 0);
        int group = this.group();
        if (group > 0) {
            this.rowAnchor("guide.pair", group, this.groupEnd(group));
        }
    }

    private void rowAnchor(String id, int from, int to) {
        float top = (float) (this.listY - this.listScroll + this.offset(from));
        float bottom = (float) (this.listY - this.listScroll + this.offset(to) + this.height(to));
        if (top >= this.listY - 0.5F && bottom <= this.listY + this.bodyHeight + 0.5F) {
            ScreenAnchors.report(id, this.listX + 1, top, this.listWidth - 2, bottom - top);
        }
    }
}
