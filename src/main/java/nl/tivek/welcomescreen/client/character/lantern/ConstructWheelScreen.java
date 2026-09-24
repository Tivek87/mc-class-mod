package nl.tivek.welcomescreen.client.character.lantern;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.lantern.Construct;
import nl.tivek.welcomescreen.client.GuiShapes;
import nl.tivek.welcomescreen.client.character.AbilityKeys;
import org.lwjgl.glfw.GLFW;

/**
 * The wheel of hard-light constructs you get while you hold the construct key.
 *
 * <p>How it is used: hold the key, flick the mouse towards the slot you want, let the key go. It is the
 * direction that picks, not the distance, so a short flick is enough and the mouse may run off the edge
 * of the screen. Letting go in the middle puts whatever you hold away again. The scroll wheel steps from
 * slot to slot for whoever prefers that, a left-click picks straight away, and Escape or a right-click
 * closes without changing anything. Only tapping the key never gets this far: that swaps on the spot
 * (see {@link ConstructWheel}).
 *
 * <p>The wheel fills almost the whole height of the screen. What the mouse points at is written in its middle:
 * its name, and what it is. A thin plate along the top of the screen (never over the wheel) says how a
 * construct's mouse buttons work and what to do. Every construct will use the mouse the same way: left click
 * attacks, from the right hand (where the ring is); right click defends, from the left hand. None of them do
 * anything yet: picking one only writes it down (see {@link ConstructChoice}) and shows it above your hotbar.
 */
public class ConstructWheelScreen extends Screen {
    private static final String KEY = "screen." + WelcomeScreenMod.MODID + ".construct_wheel.";

    /** The green of the ring's light, its bright middle, and the dark green the wheel is filled with. */
    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0xCFFFDC;
    private static final int DEEP = 0x0B2E18;

    private static final int DIM = 0x73000000;
    private static final int TEXT = 0xFFE8FFEE;
    private static final int MUTED = 0xFF7E9B88;
    private static final int FAINT = 0xFF5A6E61;

    /** How big the wheel gets at most, and the least it shrinks to on the smallest screens. */
    private static final float OUTER_MAX = 170.0F;
    private static final float OUTER_MIN = 28.0F;
    /** The plate along the top: its padding, the height of a line, and how far it keeps from the edge. */
    private static final float BAR_PAD = 4.0F;
    private static final int BAR_LINE = 11;
    private static final float EDGE = 5.0F;
    /** How far the dark disc behind the wheel reaches past it. */
    private static final float DISC = 10.0F;
    /** The hole in the middle, and the button inside that hole, both as a part of the ring outside them. */
    private static final float INNER_SHARE = 0.58F;
    private static final float HUB_SHARE = 0.86F;
    /** How much of the button's width the writing in it may use. */
    private static final float HUB_TEXT = 0.8F;
    /** The gap between two slices, in degrees. */
    private static final float GAP_DEGREES = 1.6F;
    /** How much further out a slice reaches while the mouse points at it. */
    private static final float HOVER_GROW = 7.0F;

    /**
     * How quickly the wheel swings open and a slice lights up, in seconds: this is how long each takes to
     * get most of the way there, so it never snaps and never drags.
     */
    private static final float OPEN_TIME = 0.075F;
    private static final float HOVER_TIME = 0.05F;

    private final KeyMapping key;
    private final List<Construct> wheel = Construct.WHEEL;
    private final float[] lit = new float[Construct.WHEEL.size()];

    /** Which slice the mouse points at, or -1 for the middle (no construct). */
    private int pointed = -1;
    /** Where the mouse was last frame, so a mouse standing still never overrules the scroll wheel. */
    private double lastMouseX = Double.NaN;
    private double lastMouseY = Double.NaN;
    private float hubLit;
    private float open;
    private long lastFrame = Util.getMillis();
    /** True once the key was really seen held down: only then does letting it go pick something. */
    private boolean armed;
    private boolean picked;

    public ConstructWheelScreen(KeyMapping key) {
        super(Component.translatable(KEY + "title"));
        this.key = key;
    }

    // ---- What the key does ----

    @Override
    public void tick() {
        super.tick();
        this.closeIfLetGo();
    }

    /**
     * Holding the key and letting it go picks; a tap too short to be seen leaves the wheel standing.
     *
     * @return true when this closed the wheel
     */
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

    /** Takes out whatever the mouse points at; the middle puts your construct away. */
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

    /**
     * The scroll wheel steps from slot to slot, for when flicking the mouse is awkward. Scrolling past
     * the last slot comes back round to the middle, so you can reach "nothing" this way too.
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0.0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        int slots = this.wheel.size();
        int step = scrollY > 0.0 ? 1 : -1;
        // The middle counts as one more place in the row, just before the first slot.
        int was = this.pointed + 1;
        this.point(Math.floorMod(was + step, slots + 1) - 1);
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ---- Where everything sits ----

    /** How tall the plate along the top is: two lines. */
    private static float bar() {
        return BAR_PAD * 2.0F + BAR_LINE * 2;
    }

    /**
     * Half the height of the wheel: as big as fits below the plate at the top, with the dark disc behind it clear
     * of the edges too, under a limit.
     */
    private float outer() {
        float high = (this.height - bar() - EDGE * 2.0F) * 0.5F - DISC;
        float wide = this.width * 0.5F - DISC - EDGE;
        return Mth.clamp(Math.min(high, wide), OUTER_MIN, OUTER_MAX);
    }

    private float middleX() {
        return this.width * 0.5F;
    }

    /** The middle of the wheel: halfway down the room below the plate, so a little below the middle of the screen. */
    private float middleY() {
        return (this.height + bar()) * 0.5F;
    }

    /** The middle of slice number {@code index}, in degrees clockwise from straight up. */
    private float slice(int index) {
        return 360.0F / this.wheel.size() * index;
    }

    /**
     * Which slice the mouse points at. Only the direction counts, so a flick of the mouse is enough and
     * it does not matter how far it went; close to the middle nothing is pointed at. The direction is taken
     * from the middle of the screen, where the mouse stands when the wheel opens, so a flick straight to the
     * right picks the slice straight to the right even though the wheel sits a little lower.
     */
    private void followMouse(double mouseX, double mouseY) {
        // A mouse that has not moved says nothing: that leaves a slot picked with the scroll wheel alone
        // until you really move the mouse again.
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

    /** Points at a slot ({@code -1} is the middle), with a tick you can hear when it changes. */
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

    // ---- Drawing ----

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

        // A soft dark disc so the wheel reads over any world behind it.
        GuiShapes.disc(graphics, middleX, middleY, outer + DISC, GuiShapes.fade(0x000000, 0.34F * this.open));
        this.renderSlices(graphics, middleX, middleY, inner, outer);
        this.renderHub(graphics, middleX, middleY, hub);
        this.renderPointer(graphics, middleX, middleY, hub, inner);
        GuiShapes.flush(graphics);

        this.renderName(graphics, middleX, middleY, hub);
        this.renderBar(graphics);
    }

    /** Moves every glow one frame further towards where it should be, so nothing jumps. */
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
            // A bright rim on the outside, strongest on the slice the mouse points at.
            GuiShapes.arc(graphics, middleX, middleY, edge - 1.6F, edge,
                    from, to, GuiShapes.fade(GuiShapes.mix(GREEN, BRIGHT, glow), (0.4F + 0.6F * glow) * this.open));

            float radius = (inner + edge) * 0.5F;
            float angle = this.slice(i) * Mth.DEG_TO_RAD;
            float iconX = middleX + Mth.sin(angle) * radius;
            float iconY = middleY - Mth.cos(angle) * radius;
            float iconSize = (edge - inner) * 0.34F * (1.0F + 0.08F * glow);
            int iconColour = GuiShapes.mix(GREEN, BRIGHT, 0.3F + 0.7F * glow);
            float iconAlpha = (0.55F + 0.45F * glow) * this.open;
            if (construct == Construct.SWORD_SHIELD) {
                swordShieldIcon(graphics, iconX, iconY, iconSize, iconColour, iconAlpha);
            } else {
                emptyIcon(graphics, iconX, iconY, iconSize, iconColour, iconAlpha);
            }
            // A dot marks the one you already have out.
            if (construct == held) {
                GuiShapes.disc(graphics, middleX + Mth.sin(angle) * (inner + 4.0F),
                        middleY - Mth.cos(angle) * (inner + 4.0F), 1.8F, GuiShapes.fade(BRIGHT, this.open));
            }
        }
    }

    /**
     * The button in the middle: empty hands, only the ring. It is dark, with a faint ring of the ring's light
     * behind the writing, so the name of what the mouse points at reads clearly on it.
     */
    private void renderHub(GuiGraphics graphics, float middleX, float middleY, float hub) {
        float glow = this.hubLit;
        GuiShapes.disc(graphics, middleX, middleY, hub,
                GuiShapes.fade(GuiShapes.mix(DEEP, GREEN, glow * 0.3F), (0.8F + 0.15F * glow) * this.open));
        GuiShapes.ring(graphics, middleX, middleY, hub - 1.0F, 1.6F,
                GuiShapes.fade(GuiShapes.mix(GREEN, BRIGHT, glow), (0.45F + 0.55F * glow) * this.open));
        GuiShapes.ring(graphics, middleX, middleY, hub * 0.72F, Math.max(1.0F, hub * 0.05F),
                GuiShapes.fade(GREEN, (0.14F + 0.2F * glow) * this.open));
    }

    /**
     * The empty square where a construct's picture will go. The drawings are not made yet, so every slot
     * shows the same frame; swap this for the 16x16 icon once it exists.
     */
    private static void emptyIcon(GuiGraphics graphics, float x, float y, float size, int rgb, float alpha) {
        float half = size * 0.5F;
        float thick = Math.max(1.0F, size * 0.09F);
        int line = GuiShapes.fade(rgb, alpha);
        // Four short corner pieces rather than a full box, so it reads as an empty slot, not a button.
        float leg = half * 0.62F;
        for (int sx = -1; sx <= 1; sx += 2) {
            for (int sy = -1; sy <= 1; sy += 2) {
                GuiShapes.stroke(graphics, x + sx * half, y + sy * half, x + sx * (half - leg), y + sy * half,
                        thick, line);
                GuiShapes.stroke(graphics, x + sx * half, y + sy * half, x + sx * half, y + sy * (half - leg),
                        thick, line);
            }
        }
        GuiShapes.disc(graphics, x, y, thick * 0.9F, GuiShapes.fade(rgb, alpha * 0.8F));
    }

    /**
     * The picture of the sword and shield: a heater shield with a ring on its face, and a sword crossing behind it from
     * its bottom left to its top right, with its crossguard and pommel.
     */
    private static void swordShieldIcon(GuiGraphics graphics, float x, float y, float size, int rgb, float alpha) {
        float half = size * 0.5F;
        float thick = Math.max(1.0F, size * 0.09F);
        int line = GuiShapes.fade(rgb, alpha);
        // The sword, corner to corner behind the shield.
        float fromX = x - half * 0.95F;
        float fromY = y + half * 0.95F;
        float toX = x + half * 1.05F;
        float toY = y - half * 1.05F;
        GuiShapes.stroke(graphics, fromX, fromY, toX, toY, thick * 1.1F, line);
        float guardX = x - half * 0.55F;
        float guardY = y + half * 0.55F;
        float cross = half * 0.32F;
        GuiShapes.stroke(graphics, guardX - cross, guardY - cross, guardX + cross, guardY + cross, thick, line);
        GuiShapes.disc(graphics, fromX, fromY, thick * 1.1F, line);
        // The shield: flat on top, sides coming in to a point below.
        float w = half * 0.62F;
        float top = y - half * 0.62F;
        float waist = y + half * 0.1F;
        float point = y + half * 0.78F;
        int shield = GuiShapes.fade(GuiShapes.mix(0x0B2E18, rgb, 0.35F), Math.min(1.0F, alpha * 1.1F));
        for (float row = top; row <= point; row += 0.75F) {
            float across = row <= waist ? w : w * (1.0F - (row - waist) / (point - waist));
            GuiShapes.stroke(graphics, x - across, row, x + across, row, 1.0F, shield);
        }
        GuiShapes.stroke(graphics, x - w, top, x + w, top, thick, line);
        GuiShapes.stroke(graphics, x - w, top, x - w, waist, thick, line);
        GuiShapes.stroke(graphics, x + w, top, x + w, waist, thick, line);
        GuiShapes.stroke(graphics, x - w, waist, x, point, thick, line);
        GuiShapes.stroke(graphics, x + w, waist, x, point, thick, line);
        GuiShapes.ring(graphics, x, y - half * 0.1F, half * 0.26F, Math.max(1.0F, thick * 0.8F), line);
    }

    /** A little wedge that shows which way the mouse is pointing. */
    private void renderPointer(GuiGraphics graphics, float middleX, float middleY, float hub, float inner) {
        if (this.pointed < 0) {
            return;
        }
        float angle = this.slice(this.pointed);
        GuiShapes.arc(graphics, middleX, middleY, hub + 2.0F, inner - 2.0F, angle - 3.5F, angle + 3.5F,
                GuiShapes.fade(BRIGHT, 0.8F * this.open));
    }

    /**
     * What the mouse points at, in the middle of the wheel: its name, and under it what it is, both made smaller
     * when they would not fit in the button.
     */
    private void renderName(GuiGraphics graphics, float middleX, float middleY, float hub) {
        Construct construct = this.pointed < 0 ? Construct.NONE : this.wheel.get(this.pointed);
        float room = hub * 2.0F * HUB_TEXT;
        Component name = construct.getDisplayName();
        float nameScale = Math.min(1.0F, room / Math.max(1, this.font.width(name)));
        // What it is, broken over as many lines as it needs at a size that fits, never more than three.
        float aboutScale = 0.8F;
        List<FormattedCharSequence> about = this.font.split(
                construct.getDescription().copy().withStyle(ChatFormatting.ITALIC), (int) (room / aboutScale));
        while (about.size() > 3 && aboutScale > 0.5F) {
            aboutScale -= 0.1F;
            about = this.font.split(construct.getDescription().copy().withStyle(ChatFormatting.ITALIC),
                    (int) (room / aboutScale));
        }
        float nameHeight = this.font.lineHeight * nameScale;
        float aboutLine = (this.font.lineHeight + 1) * aboutScale;
        float total = nameHeight + 3.0F + aboutLine * Math.min(3, about.size());
        float y = middleY - total * 0.5F;
        int alpha = (int) (255 * Mth.clamp(this.open, 0.1F, 1.0F)) << 24;
        graphics.pose().pushPose();
        graphics.pose().translate(middleX, y, 0.0F);
        graphics.pose().scale(nameScale, nameScale, 1.0F);
        graphics.drawString(this.font, name, -this.font.width(name) / 2, 0, TEXT & 0xFFFFFF | alpha);
        graphics.pose().popPose();
        y += nameHeight + 3.0F;
        for (int i = 0; i < Math.min(3, about.size()); i++) {
            FormattedCharSequence line = about.get(i);
            graphics.pose().pushPose();
            graphics.pose().translate(middleX, y, 0.0F);
            graphics.pose().scale(aboutScale, aboutScale, 1.0F);
            graphics.drawString(this.font, line, -this.font.width(line) / 2, 0, MUTED & 0xFFFFFF | alpha);
            graphics.pose().popPose();
            y += aboutLine;
        }
    }

    /**
     * A thin plate along the top of the screen: how a construct's mouse buttons work (left click attacks from the
     * right hand, right click defends from the left hand) and what to do. It never reaches the wheel.
     */
    private void renderBar(GuiGraphics graphics) {
        Construct construct = this.pointed < 0 ? Construct.NONE : this.wheel.get(this.pointed);
        int middle = this.width / 2;
        float widest = this.width - EDGE * 2.0F;
        // Empty hands work as they always do; the line stays, empty, so the plate does not jump.
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
