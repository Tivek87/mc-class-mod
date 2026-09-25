package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import org.lwjgl.glfw.GLFW;

public class ConstructWheelScreen extends Screen {
    private static final String KEY = "screen." + MultiversePowers.MODID + ".construct_wheel.";

    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0xCFFFDC;
    private static final int DEEP = 0x0B2E18;

    private static final int DIM = 0x73000000;
    private static final int TEXT = 0xFFE8FFEE;
    private static final int MUTED = 0xFF7E9B88;
    private static final int FAINT = 0xFF5A6E61;

    private static final float OUTER_MAX = 170.0F;
    private static final float OUTER_MIN = 28.0F;
    private static final float BAR_PAD = 4.0F;
    private static final int BAR_LINE = 11;
    private static final float EDGE = 5.0F;
    private static final float DISC = 10.0F;
    private static final float INNER_SHARE = 0.58F;
    private static final float HUB_SHARE = 0.86F;
    private static final float HUB_TEXT = 0.8F;
    private static final float GAP_DEGREES = 1.6F;
    private static final float HOVER_GROW = 7.0F;

    private static final float OPEN_TIME = 0.075F;
    private static final float HOVER_TIME = 0.05F;

    private final KeyMapping key;
    private final List<Construct> wheel = Construct.WHEEL;
    private final float[] lit = new float[Construct.WHEEL.size()];

    private int pointed = -1;
    private double lastMouseX = Double.NaN;
    private double lastMouseY = Double.NaN;
    private float hubLit;
    private float open;
    private long lastFrame = Util.getMillis();
    private boolean armed;
    private boolean picked;

    public ConstructWheelScreen(KeyMapping key) {
        super(Component.translatable(KEY + "title"));
        this.key = key;
    }

    @Override
    public void tick() {
        super.tick();
        this.closeIfLetGo();
    }

    private boolean closeIfLetGo() {
        if (this.picked) {
            return true;
        }
        if (AbilityKeys.isDown(this.key)) {
            this.armed = true;
            return false;
        }
        if (!this.armed) {
            return false;
        }
        this.pick();
        return true;
    }

    private void pick() {
        this.picked = true;
        ConstructChoice.take(this.pointed < 0 ? Construct.NONE : this.wheel.get(this.pointed));
        this.onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            this.pick();
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            this.onClose();
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.key.matchesMouse(button)) {
            this.pick();
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int slots = this.wheel.size();
        int step = scrollY > 0.0 ? 1 : -1;
        // Shift by one so the middle slot (-1) wraps into the cycle cleanly
        int was = this.pointed + 1;
        this.point(Math.floorMod(was + step, slots + 1) - 1);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static float bar() {
        return BAR_PAD * 2.0F + BAR_LINE * 2;
    }

    private float outer() {
        float high = (this.height - bar() - EDGE * 2.0F) * 0.5F - DISC;
        float wide = this.width * 0.5F - DISC - EDGE;
        return Mth.clamp(Math.min(high, wide), OUTER_MIN, OUTER_MAX);
    }

    private float middleX() {
        return this.width * 0.5F;
    }

    private float middleY() {
        return (this.height + bar()) * 0.5F;
    }

    private float slice(int index) {
        return 360.0F / this.wheel.size() * index;
    }

    private void followMouse(double mouseX, double mouseY) {
        // Skip when unmoved, so scroll-wheel picks aren't overridden every frame
        if (mouseX == this.lastMouseX && mouseY == this.lastMouseY) {
            return;
        }
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        float hub = this.outer() * INNER_SHARE * HUB_SHARE;
        double dx = mouseX - this.width * 0.5;
        double dy = mouseY - this.height * 0.5;
        if (dx * dx + dy * dy < hub * hub) {
            this.point(-1);
            return;
        }
        float step = 360.0F / this.wheel.size();
        double degrees = Math.toDegrees(Math.atan2(dx, -dy));
        this.point(Mth.floor(((degrees + 360.0 + step * 0.5) % 360.0) / step) % this.wheel.size());
    }

    private void point(int slot) {
        if (slot == this.pointed) {
            return;
        }
        this.pointed = slot;
        if (this.minecraft != null) {
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(),
                    slot < 0 ? 1.1F : 1.5F, 0.18F));
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, this.width, this.height, GuiShapes.fade(DIM & 0xFFFFFF,
                (DIM >>> 24) / 255.0F * this.open));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (this.closeIfLetGo()) {
            return;
        }
        this.followMouse(mouseX, mouseY);
        this.breathe();
        super.render(graphics, mouseX, mouseY, partialTick);

        float middleX = this.middleX();
        float middleY = this.middleY();
        float outer = this.outer() * (0.86F + 0.14F * this.open);
        float inner = outer * INNER_SHARE;
        float hub = inner * HUB_SHARE;

        GuiShapes.disc(graphics, middleX, middleY, outer + DISC, GuiShapes.fade(0x000000, 0.34F * this.open));
        this.renderSlices(graphics, middleX, middleY, inner, outer);
        this.renderHub(graphics, middleX, middleY, hub);
        this.renderPointer(graphics, middleX, middleY, hub, inner);
        GuiShapes.flush(graphics);

        this.renderName(graphics, middleX, middleY, hub);
        this.renderBar(graphics);
    }

    private void breathe() {
        long now = Util.getMillis();
        float seconds = Math.min(0.1F, (now - this.lastFrame) / 1000.0F);
        this.lastFrame = now;
        this.open = towards(this.open, 1.0F, seconds, OPEN_TIME);
        for (int i = 0; i < this.lit.length; i++) {
            this.lit[i] = towards(this.lit[i], this.pointed == i ? 1.0F : 0.0F, seconds, HOVER_TIME);
        }
        this.hubLit = towards(this.hubLit, this.pointed < 0 ? 1.0F : 0.0F, seconds, HOVER_TIME);
    }

    private static float towards(float now, float want, float seconds, float time) {
        return Mth.lerp(Math.min(1.0F, seconds / time), now, want);
    }

    private void renderSlices(GuiGraphics graphics, float middleX, float middleY, float inner, float outer) {
        Construct held = ConstructChoice.held();
        float step = 360.0F / this.wheel.size();
        for (int i = 0; i < this.wheel.size(); i++) {
            Construct construct = this.wheel.get(i);
            float glow = this.lit[i];
            float from = this.slice(i) - step * 0.5F + GAP_DEGREES;
            float to = this.slice(i) + step * 0.5F - GAP_DEGREES;
            float edge = outer + HOVER_GROW * glow;
            int fill = GuiShapes.mix(DEEP, GREEN, glow * 0.45F);
            GuiShapes.arc(graphics, middleX, middleY, inner, edge,
                    from, to, GuiShapes.fade(fill, (0.62F + 0.3F * glow) * this.open));
            GuiShapes.arc(graphics, middleX, middleY, edge - 1.6F, edge,
                    from, to, GuiShapes.fade(GuiShapes.mix(GREEN, BRIGHT, glow), (0.4F + 0.6F * glow) * this.open));

            float radius = (inner + edge) * 0.5F;
            float angle = this.slice(i) * Mth.DEG_TO_RAD;
            float iconX = middleX + Mth.sin(angle) * radius;
            float iconY = middleY - Mth.cos(angle) * radius;
            float iconSize = (edge - inner) * 0.74F * (0.86F + 0.14F * this.open);
            ConstructIcons.draw(graphics, construct, iconX, iconY, iconSize, glow);
            if (construct == held) {
                GuiShapes.disc(graphics, middleX + Mth.sin(angle) * (inner + 4.0F),
                        middleY - Mth.cos(angle) * (inner + 4.0F), 1.8F, GuiShapes.fade(BRIGHT, this.open));
            }
        }
    }

    private void renderHub(GuiGraphics graphics, float middleX, float middleY, float hub) {
        float glow = this.hubLit;
        GuiShapes.disc(graphics, middleX, middleY, hub,
                GuiShapes.fade(GuiShapes.mix(DEEP, GREEN, glow * 0.3F), (0.8F + 0.15F * glow) * this.open));
        GuiShapes.ring(graphics, middleX, middleY, hub - 1.0F, 1.6F,
                GuiShapes.fade(GuiShapes.mix(GREEN, BRIGHT, glow), (0.45F + 0.55F * glow) * this.open));
        GuiShapes.ring(graphics, middleX, middleY, hub * 0.72F, Math.max(1.0F, hub * 0.05F),
                GuiShapes.fade(GREEN, (0.14F + 0.2F * glow) * this.open));
    }

    private void renderPointer(GuiGraphics graphics, float middleX, float middleY, float hub, float inner) {
        if (this.pointed < 0) {
            return;
        }
        float angle = this.slice(this.pointed);
        GuiShapes.arc(graphics, middleX, middleY, hub + 2.0F, inner - 2.0F, angle - 3.5F, angle + 3.5F,
                GuiShapes.fade(BRIGHT, 0.8F * this.open));
    }

    private void renderName(GuiGraphics graphics, float middleX, float middleY, float hub) {
        Construct construct = this.pointed < 0 ? Construct.NONE : this.wheel.get(this.pointed);
        float room = hub * 2.0F * HUB_TEXT;
        Lines name = this.fitName(construct.getDisplayName(), room);
        Component description = construct.getDescription();
        Lines about = description == null ? new Lines(List.of(), 1.0F)
                : this.fit(description.copy().withStyle(ChatFormatting.ITALIC), room, 0.8F, 3);
        float total = name.height(this.font) + (about.lines().isEmpty() ? 0.0F : 3.0F + about.height(this.font));
        float y = middleY - total * 0.5F;
        int alpha = (int) (255 * Mth.clamp(this.open, 0.1F, 1.0F)) << 24;
        this.renderLines(graphics, name, middleX, y, TEXT & 0xFFFFFF | alpha);
        this.renderLines(graphics, about, middleX, y + name.height(this.font) + 3.0F, MUTED & 0xFFFFFF | alpha);
    }

    private record Lines(List<FormattedCharSequence> lines, float scale) {
        float step(Font font) {
            return (font.lineHeight + 1) * this.scale;
        }

        float height(Font font) {
            return this.step(font) * this.lines.size();
        }
    }

    private Lines fitName(Component name, float room) {
        String text = name.getString();
        int slash = text.indexOf(" / ");
        if (slash < 0 || this.font.width(name) <= room) {
            return this.fit(name, room, 1.0F, 2);
        }
        List<FormattedCharSequence> lines = List.of(
                Component.literal(text.substring(0, slash + 2)).getVisualOrderText(),
                Component.literal(text.substring(slash + 3)).getVisualOrderText());
        int widest = Math.max(this.font.width(lines.get(0)), this.font.width(lines.get(1)));
        return new Lines(lines, Math.min(1.0F, room / widest));
    }

    private Lines fit(Component text, float room, float scale, int lines) {
        List<FormattedCharSequence> split = this.font.split(text, (int) (room / scale));
        while (split.size() > lines && scale > 0.5F) {
            scale -= 0.1F;
            split = this.font.split(text, (int) (room / scale));
        }
        int widest = 1;
        for (FormattedCharSequence line : split) {
            widest = Math.max(widest, this.font.width(line));
        }
        return new Lines(split.subList(0, Math.min(lines, split.size())), Math.min(scale, room / widest));
    }

    private void renderLines(GuiGraphics graphics, Lines text, float middleX, float y, int colour) {
        for (FormattedCharSequence line : text.lines()) {
            graphics.pose().pushPose();
            graphics.pose().translate(middleX, y, 0.0F);
            graphics.pose().scale(text.scale(), text.scale(), 1.0F);
            graphics.drawString(this.font, line, -this.font.width(line) / 2, 0, colour);
            graphics.pose().popPose();
            y += text.step(this.font);
        }
    }

    private void renderBar(GuiGraphics graphics) {
        Construct construct = this.pointed < 0 ? Construct.NONE : this.wheel.get(this.pointed);
        int middle = this.width / 2;
        float widest = this.width - EDGE * 2.0F;
        Component controls = Component.empty();
        if (construct != Construct.NONE) {
            controls = Component.translatable(KEY + "controls");
            if (this.font.width(controls) + 16 > widest) {
                controls = Component.translatable(KEY + "controls_short");
            }
        }
        Component hint = Component.translatable(KEY + (this.pointed < 0 ? "hint_middle" : "hint"),
                this.key.getTranslatedKeyMessage());
        float plateWidth = Math.min(widest, Math.max(this.font.width(controls), this.font.width(hint)) + 16.0F);
        float top = EDGE;
        GuiShapes.roundRect(graphics, middle - plateWidth * 0.5F, top, plateWidth, bar(), 4.0F,
                GuiShapes.fade(0x04140A, 0.78F * this.open));
        GuiShapes.flush(graphics);
        graphics.drawCenteredString(this.font, controls, middle, (int) (top + BAR_PAD) + 1, MUTED);
        graphics.drawCenteredString(this.font, hint, middle, (int) (top + BAR_PAD) + 1 + BAR_LINE, FAINT);
    }
}
