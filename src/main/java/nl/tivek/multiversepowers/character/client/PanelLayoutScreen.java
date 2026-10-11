package nl.tivek.multiversepowers.character.client;

import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;

// The ability overlay's layout: drag it anywhere on the screen, pull its free corner (or scroll over it) to size it.
// It is kept by the corner nearest the screen's nearest corner, so it grows away from the edge it was put against.
// Reset puts it back in the corner right of the hotbar at its own size. The game shows through, the overlay drawn as
// it is (a stand-in frame while no character or tool has one).
public final class PanelLayoutScreen extends Screen {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".panel_layout.";
    private static final int HANDLE = 6;
    private static final int GOLD = 0xFFFFD54A;
    // The stand-in frame's size at size 1, in GUI pixels.
    private static final int STAND_IN_WIDTH = 120;
    private static final int STAND_IN_HEIGHT = 76;

    @Nullable
    private final Screen back;
    private boolean moving;
    @Nullable
    private Button reset;
    @Nullable
    private Button done;
    private boolean sizing;
    private double grabX;
    private double grabY;
    private float startSize;
    private double startReach;

    public PanelLayoutScreen(@Nullable Screen back) {
        super(Component.translatable(PREFIX + "title"));
        this.back = back;
    }

    @Override
    protected void init() {
        int y = this.height / 2 - 10;
        this.reset = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "reset"), button -> {
            ClientSettings.PANEL_PLACED.set(0);
            ClientSettings.PANEL_SIZE.set(1.0);
            ClientSettings.PANEL_X.set(1.0);
            ClientSettings.PANEL_Y.set(1.0);
        }).bounds(this.width / 2 - 82, y, 80, 20).build());
        this.done = this.addRenderableWidget(Button.builder(Component.translatable("gui.done"),
                button -> this.onClose()).bounds(this.width / 2 + 2, y, 80, 20).build());
    }

    // The top of the words and buttons: in the middle, or above or below the overlay where it would cover them.
    private int guideTop(float[] box, int tall, int wide) {
        int middle = (this.height - tall) / 2;
        boolean across = box[0] < (this.width + wide) / 2.0F && box[0] + box[2] > (this.width - wide) / 2.0F;
        if (!across || box[1] >= middle + tall + 4 || box[1] + box[3] <= middle - 4) {
            return middle;
        }
        int below = Math.round(box[1] + box[3]) + 10;
        int above = Math.round(box[1]) - 10 - tall;
        if (below + tall <= this.height - 4 && (above < 4 || this.height - below > above)) {
            return below;
        }
        return Math.max(4, above);
    }

    @Override
    public void tick() {
        AbilityPanel.wake();
    }

    // Where the overlay is now (left, top, width, height): as drawn, or the stand-in where it would stand.
    private float[] box() {
        float[] drawn = AbilityPanel.drawn();
        if (drawn != null) {
            return drawn;
        }
        float size = (float) ClientSettings.get(ClientSettings.PANEL_SIZE);
        float wide = STAND_IN_WIDTH * size;
        float tall = STAND_IN_HEIGHT * size;
        if (ClientSettings.get(ClientSettings.PANEL_PLACED) == 0) {
            return new float[] { this.width - 2 - wide, this.height - 2 - tall, wide, tall };
        }
        double across = ClientSettings.get(ClientSettings.PANEL_X);
        double down = ClientSettings.get(ClientSettings.PANEL_Y);
        float x = (float) (across * this.width) - (across >= 0.5 ? wide : 0.0F);
        float y = (float) (down * this.height) - (down >= 0.5 ? tall : 0.0F);
        return new float[] { Mth.clamp(x, 0.0F, Math.max(0.0F, this.width - wide)), Mth.clamp(y, 0.0F,
                Math.max(0.0F, this.height - tall)), wide, tall };
    }

    // The corner it is kept by, and the free one across from it that sizes it.
    private boolean keptRight(float[] box) {
        return box[0] + box[2] / 2.0F > this.width / 2.0F;
    }

    private boolean keptBottom(float[] box) {
        return box[1] + box[3] / 2.0F > this.height / 2.0F;
    }

    private float[] handle(float[] box) {
        float x = this.keptRight(box) ? box[0] : box[0] + box[2];
        float y = this.keptBottom(box) ? box[1] : box[1] + box[3];
        return new float[] { x, y };
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, 0x30000000);
        int wide = this.guideWidth();
        int tall = this.guideHeight(wide);
        int y = this.guideTop(this.box(), tall, wide);
        graphics.fill(this.width / 2 - wide / 2 - 6, y - 5, this.width / 2 + wide / 2 + 6, y + tall - 2, 0xB0000000);
        int buttons = y + tall - 34;
        if (this.reset != null && this.done != null && this.reset.getY() != buttons) {
            this.reset.setY(buttons);
            this.done.setY(buttons);
        }
    }

    private int guideWidth() {
        return Math.min(this.width - 20, 260);
    }

    private int guideHeight(int wide) {
        return 14 + this.font.split(Component.translatable(PREFIX + "help"), wide).size() * 10 + 6 + 20 + 14;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        float[] box = this.box();
        int left = Math.round(box[0]);
        int top = Math.round(box[1]);
        int right = Math.round(box[0] + box[2]);
        int bottom = Math.round(box[1] + box[3]);
        boolean over = mouseX >= left && mouseX < right && mouseY >= top && mouseY < bottom;
        if (AbilityPanel.drawn() == null) {
            graphics.fill(left, top, right, bottom, 0x60101010);
            graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "stand_in"), (left + right) / 2,
                    (top + bottom) / 2 - 4, 0xFFB0B0B0);
        }
        int edge = this.moving || over ? GOLD : 0xC0FFFFFF;
        graphics.renderOutline(left - 1, top - 1, right - left + 2, bottom - top + 2, edge);
        float[] handle = this.handle(box);
        int hx = Math.round(handle[0]);
        int hy = Math.round(handle[1]);
        graphics.fill(hx - HANDLE / 2, hy - HANDLE / 2, hx + HANDLE / 2, hy + HANDLE / 2, this.sizing ? GOLD
                : 0xFFFFFFFF);
        int wide = this.guideWidth();
        int y = this.guideTop(box, this.guideHeight(wide), wide);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, y, 0xFFFFFFFF);
        y += 14;
        for (FormattedCharSequence line : this.font.split(Component.translatable(PREFIX + "help"), wide)) {
            graphics.drawCenteredString(this.font, line, this.width / 2, y, 0xFFD2D2D2);
            y += 10;
        }
        y += 6;
        graphics.drawCenteredString(this.font, Component.translatable(PREFIX + "size", Math.round(ClientSettings.get(
                ClientSettings.PANEL_SIZE) * 100.0)), this.width / 2, y + 24, 0xFF8E8E8E);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        float[] box = this.box();
        float[] handle = this.handle(box);
        if (Math.abs(mouseX - handle[0]) <= HANDLE && Math.abs(mouseY - handle[1]) <= HANDLE) {
            this.sizing = true;
            this.startSize = (float) ClientSettings.get(ClientSettings.PANEL_SIZE);
            float[] kept = this.kept(box);
            this.startReach = Math.max(4.0, Math.hypot(mouseX - kept[0], mouseY - kept[1]));
            return true;
        }
        if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3]) {
            this.moving = true;
            this.grabX = mouseX - box[0];
            this.grabY = mouseY - box[1];
            return true;
        }
        return false;
    }

    // The corner it is kept by.
    private float[] kept(float[] box) {
        return new float[] { this.keptRight(box) ? box[0] + box[2] : box[0], this.keptBottom(box) ? box[1] + box[3]
                : box[1] };
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float[] box = this.box();
        if (this.sizing) {
            float[] kept = this.kept(box);
            double reach = Math.hypot(mouseX - kept[0], mouseY - kept[1]);
            this.size(this.startSize * (float) (reach / this.startReach));
            return true;
        }
        if (this.moving) {
            double left = Mth.clamp(mouseX - this.grabX, 0.0, Math.max(0.0, this.width - box[2]));
            double top = Mth.clamp(mouseY - this.grabY, 0.0, Math.max(0.0, this.height - box[3]));
            this.place(left, top, box[2], box[3]);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    // Keeps it by the corner nearest the screen's nearest corner, as fractions of the screen.
    private void place(double left, double top, float wide, float tall) {
        boolean right = left + wide / 2.0 > this.width / 2.0;
        boolean bottom = top + tall / 2.0 > this.height / 2.0;
        double across = (right ? left + wide : left) / this.width;
        double down = (bottom ? top + tall : top) / this.height;
        // A kept corner on the wrong side of the middle would turn it round: hold it on its own side.
        across = right ? Math.max(0.5, across) : Math.min(0.4999, across);
        down = bottom ? Math.max(0.5, down) : Math.min(0.4999, down);
        ClientSettings.PANEL_PLACED.set(1);
        ClientSettings.PANEL_X.set(Mth.clamp(across, 0.0, 1.0));
        ClientSettings.PANEL_Y.set(Mth.clamp(down, 0.0, 1.0));
    }

    private void size(float size) {
        ClientSettings.PANEL_SIZE.set((double) Mth.clamp(Math.round(size * 20.0F) / 20.0F, 0.5F, 2.0F));
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.moving = false;
        this.sizing = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float[] box = this.box();
        if (mouseX >= box[0] && mouseX < box[0] + box[2] && mouseY >= box[1] && mouseY < box[1] + box[3]) {
            this.size((float) ClientSettings.get(ClientSettings.PANEL_SIZE) + (scrollY > 0.0 ? 0.05F : -0.05F));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        ClientSettings.SPEC.save();
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.back);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
