package nl.tivek.multiversepowers.engine.client.gui;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import nl.tivek.multiversepowers.MultiversePowers;

// Room on the HUD this frame, in GUI units: every layer claims what it draws, and one that could land on another's
// spot asks for the free place nearest where it wants to be, so nothing is drawn over something else. Round the
// crosshair rings and arcs claim a band of radii and angles; one that would cross another moves out past it, and words
// keep off them too.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HudSpace {
    // Kept free between two neighbours, and between two bands.
    public static final float GAP = 2.0F;
    private static final float BAND_GAP = 1.0F;
    private static final float STEP = 1.0F;
    private static final int FARTHEST = 600;
    private static final float SAMPLE_DEGREES = 4.0F;
    private static final List<Box> BOXES = new ArrayList<>();
    private static final List<Band> BANDS = new ArrayList<>();

    public enum Way {
        UP(0.0F, -1.0F), DOWN(0.0F, 1.0F), LEFT(-1.0F, 0.0F), RIGHT(1.0F, 0.0F);

        final float dx;
        final float dy;

        Way(float dx, float dy) {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public record Box(float x, float y, float width, float height) {
        public float right() {
            return this.x + this.width;
        }

        public float bottom() {
            return this.y + this.height;
        }

        Box moved(float dx, float dy) {
            return new Box(this.x + dx, this.y + dy, this.width, this.height);
        }

        boolean overlaps(Box other) {
            return this.x < other.right() + GAP && other.x < this.right() + GAP && this.y < other.bottom() + GAP
                    && other.y < this.bottom() + GAP;
        }

        boolean holds(float px, float py) {
            return px > this.x - GAP && px < this.right() + GAP && py > this.y - GAP && py < this.bottom() + GAP;
        }
    }

    // From `inner` to `outer` round (cx, cy), over the degrees `from` to `to` clockwise from straight up.
    private record Band(float cx, float cy, float inner, float outer, float from, float to) {
        boolean crosses(Band other) {
            if (Math.abs(this.cx - other.cx) > 0.5F || Math.abs(this.cy - other.cy) > 0.5F) {
                // Round two different middles: their squares are near enough.
                return Math.abs(this.cx - other.cx) < this.outer + other.outer + BAND_GAP
                        && Math.abs(this.cy - other.cy) < this.outer + other.outer + BAND_GAP;
            }
            return this.inner < other.outer + BAND_GAP && other.inner < this.outer + BAND_GAP && this.turnsWith(other);
        }

        private boolean turnsWith(Band other) {
            if (this.to - this.from >= 360.0F || other.to - other.from >= 360.0F) {
                return true;
            }
            float a = Mth.positiveModulo(this.from, 360.0F);
            float b = Mth.positiveModulo(other.from, 360.0F);
            for (int turn = -1; turn <= 1; turn++) {
                float start = b + turn * 360.0F;
                if (a < start + (other.to - other.from) && start < a + (this.to - this.from)) {
                    return true;
                }
            }
            return false;
        }

        boolean has(float px, float py) {
            float dx = px - this.cx;
            float dy = py - this.cy;
            float far = Mth.sqrt(dx * dx + dy * dy);
            if (far < this.inner - GAP || far > this.outer + GAP) {
                return false;
            }
            if (this.to - this.from >= 360.0F) {
                return true;
            }
            float angle = Mth.positiveModulo((float) Math.toDegrees(Math.atan2(dx, -dy)) - this.from, 360.0F);
            return angle <= this.to - this.from;
        }

        // Whether any of it lies in the box, or the box in it.
        boolean hits(Box box) {
            float span = this.to - this.from;
            int steps = Math.max(1, Mth.ceil(span / SAMPLE_DEGREES));
            for (int i = 0; i <= steps; i++) {
                float rad = (this.from + span * i / steps) * Mth.DEG_TO_RAD;
                float sin = Mth.sin(rad);
                float cos = Mth.cos(rad);
                for (float r : new float[] { this.inner, (this.inner + this.outer) * 0.5F, this.outer }) {
                    if (box.holds(this.cx + sin * r, this.cy - cos * r)) {
                        return true;
                    }
                }
            }
            float midX = (box.x() + box.right()) * 0.5F;
            float midY = (box.y() + box.bottom()) * 0.5F;
            return this.has(box.x(), box.y()) || this.has(box.right(), box.y()) || this.has(box.x(), box.bottom())
                    || this.has(box.right(), box.bottom()) || this.has(midX, midY);
        }
    }

    private HudSpace() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    static void onRenderGui(RenderGuiEvent.Pre event) {
        BOXES.clear();
        BANDS.clear();
        effects(event.getGuiGraphics().guiWidth());
    }

    // The game's own effect icons in the top right corner, laid out as it does: the good ones in a row over the bad.
    private static void effects(int screenWidth) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }
        int good = 0;
        int bad = 0;
        for (MobEffectInstance effect : minecraft.player.getActiveEffects()) {
            if (effect.showIcon() && IClientMobEffectExtensions.of(effect).isVisibleInGui(effect)) {
                if (effect.getEffect().value().isBeneficial()) {
                    good++;
                } else {
                    bad++;
                }
            }
        }
        int top = minecraft.isDemo() ? 16 : 1;
        if (good > 0) {
            claim(screenWidth - 25 * good, top, 25 * good, 24);
        }
        if (bad > 0) {
            claim(screenWidth - 25 * bad, top + 26, 25 * bad, 24);
        }
    }

    // Each boss bar with its name over it.
    @SubscribeEvent
    static void onBossBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        claim(event.getX(), event.getY() - 9, 182, 14);
    }

    // What is drawn here stays here: whatever comes later keeps off it.
    public static void claim(float x, float y, float width, float height) {
        BOXES.add(new Box(x, y, width, height));
    }

    public static boolean free(float x, float y, float width, float height) {
        return free(new Box(x, y, width, height));
    }

    private static boolean free(Box box) {
        for (Box taken : BOXES) {
            if (taken.overlaps(box)) {
                return false;
            }
        }
        for (Band band : BANDS) {
            if (band.hits(box)) {
                return false;
            }
        }
        return true;
    }

    // The free place nearest the one wanted, moving only along `ways` (the first given wins a tie), claimed; where none
    // is free within reach, the one wanted.
    public static Box place(float x, float y, float width, float height, Way... ways) {
        Box wanted = new Box(x, y, width, height);
        if (free(wanted)) {
            BOXES.add(wanted);
            return wanted;
        }
        for (int k = 1; k <= FARTHEST; k++) {
            for (Way way : ways) {
                Box tried = wanted.moved(way.dx * k * STEP, way.dy * k * STEP);
                if (free(tried)) {
                    BOXES.add(tried);
                    return tried;
                }
            }
        }
        BOXES.add(wanted);
        return wanted;
    }

    // The inner radius, from `inner` out, at which a band `thick` wide over the degrees `from` to `to` (clockwise from
    // straight up) round (cx, cy) crosses no other band, claimed.
    public static float ring(float cx, float cy, float inner, float thick, float from, float to) {
        float at = inner;
        for (int tries = 0; tries < BANDS.size() + 1; tries++) {
            Band band = new Band(cx, cy, at, at + thick, from, to);
            Band crossed = null;
            for (Band taken : BANDS) {
                if (taken.crosses(band)) {
                    crossed = taken;
                    break;
                }
            }
            if (crossed == null) {
                BANDS.add(band);
                return at;
            }
            at = crossed.outer + BAND_GAP;
        }
        BANDS.add(new Band(cx, cy, at, at + thick, from, to));
        return at;
    }
}
