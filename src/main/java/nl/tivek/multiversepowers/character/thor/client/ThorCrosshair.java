package nl.tivek.multiversepowers.character.thor.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.client.Crosshairs;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;
import nl.tivek.multiversepowers.engine.math.Noise;

// Thor's crosshair: a small rune diamond round a point, like the knot on Mjolnir's handle, with four short ticks
// outside it, in storm blue. The ticks spread as he strikes or runs and jolt out as a blow lands; on a creature the
// diamond turns into a square locked on it, in its colour. With the hammer charged it burns white and sparks crackle off
// its corners. It reacts as every crosshair does (Crosshairs.Feel); while lightning runs through him the ticks
// crackle, and with Mjolnir thrown a small arrow on its edge points the way to it. His charge ring (ThunderGauge)
// runs round it.
public final class ThorCrosshair {
    private static final int BLUE = 0x9FD8FF;
    private static final int WHITE = 0xF4FBFF;
    private static final float RUNE = 2.8F;
    private static final float GAP = 2.2F;
    private static final float TICK = 2.6F;
    private static final float WIDTH = 0.9F;

    private ThorCrosshair() {
    }

    public static float draw(GuiGraphics graphics, LocalPlayer player, float cx, float cy, Crosshairs.Feel feel) {
        boolean charged = ClientThor.has(player, ThorStatePayload.HAMMER_CHARGED);
        boolean lightning = ClientThor.has(player, ThorStatePayload.LIGHTNING)
                || ClientThor.has(player, ThorStatePayload.CHARGED);
        float time = feel.time();
        float aim = feel.aim();
        int own = charged ? WHITE : BLUE;
        int color = Crosshairs.tint(own, feel, 0.35F);
        float width = WIDTH + feel.thick();
        float rune = RUNE + 0.8F * feel.spread() - 0.5F * aim;
        float turn = Mth.HALF_PI * 0.5F * aim;
        for (int side = 0; side < 4; side++) {
            float a = side * Mth.HALF_PI + turn;
            float b = a + Mth.HALF_PI;
            Crosshairs.stroke(graphics, feel.x(cx, Mth.sin(a) * rune, -Mth.cos(a) * rune),
                    feel.y(cy, Mth.sin(a) * rune, -Mth.cos(a) * rune), feel.x(cx, Mth.sin(b) * rune,
                            -Mth.cos(b) * rune), feel.y(cy, Mth.sin(b) * rune, -Mth.cos(b) * rune), width, color,
                    0.9F);
        }
        float in = rune + GAP + 3.0F * feel.spread() - 0.8F * aim + 1.6F * feel.hit();
        float out = in + TICK;
        for (int side = 0; side < 4; side++) {
            float a = side * Mth.HALF_PI;
            float dx = Mth.sin(a);
            float dy = -Mth.cos(a);
            float jitter = lightning ? (float) (Noise.of((int) (time * 0.8F), side, 21) - 0.5) * 1.6F : 0.0F;
            Crosshairs.stroke(graphics, feel.x(cx, dx * in, dy * in), feel.y(cy, dx * in, dy * in),
                    feel.x(cx, dx * out - dy * jitter, dy * out + dx * jitter),
                    feel.y(cy, dx * out - dy * jitter, dy * out + dx * jitter), width, color, 0.9F);
        }
        float reach = out * feel.grow() * Math.max(feel.wide(), feel.tall());
        if (charged || lightning) {
            sparks(graphics, cx + feel.swayX(), cy + feel.swayY(), feel.radius(rune), time);
        }
        Crosshairs.dot(graphics, cx, cy, 0.6F + 0.25F * aim, Crosshairs.tint(own, feel, 1.0F), 0.95F);
        Crosshairs.health(graphics, cx + feel.swayX(), cy + feel.swayY(), feel.radius(rune) + 1.6F, feel);
        if (ClientThor.has(player, ThorStatePayload.THROWN)) {
            hammer(graphics, player, cx, cy, reach + 2.5F, color, feel.partialTick());
        }
        return reach;
    }

    // A small arrow on the crosshair's edge pointing the way to his thrown hammer; a ring round it when it lies
    // straight ahead.
    private static void hammer(GuiGraphics graphics, LocalPlayer player, float cx, float cy, float at, int color,
            float partialTick) {
        ThrownHammer hammer = null;
        for (Entity entity : player.clientLevel.entitiesForRendering()) {
            if (entity instanceof ThrownHammer thrown && thrown.owner() == player.getId()) {
                hammer = thrown;
                break;
            }
        }
        if (hammer == null) {
            return;
        }
        Vec3 to = hammer.getPosition(partialTick).subtract(player.getEyePosition(partialTick));
        float yaw = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z)));
        float sx = Mth.wrapDegrees(yaw - player.getViewYRot(partialTick));
        float sy = pitch - player.getViewXRot(partialTick);
        if (sx * sx + sy * sy < 16.0F) {
            Crosshairs.ring(graphics, cx, cy, at, 0.5F, color, 0.7F);
            return;
        }
        float a = (float) Math.atan2(sx, -sy);
        float dx = Mth.sin(a);
        float dy = -Mth.cos(a);
        float tipX = cx + dx * (at + 2.4F);
        float tipY = cy + dy * (at + 2.4F);
        for (int side = -1; side <= 1; side += 2) {
            float bx = cx + dx * at - dy * side * 1.8F;
            float by = cy + dy * at + dx * side * 1.8F;
            Crosshairs.stroke(graphics, bx, by, tipX, tipY, 0.8F, color, 0.9F);
        }
    }

    // Short forked sparks off the diamond's corners, each flaring for a moment now and then.
    private static void sparks(GuiGraphics graphics, float cx, float cy, float rune, float time) {
        int beat = (int) (time * 0.5F);
        for (int corner = 0; corner < 4; corner++) {
            if (Noise.of(beat, corner, 11) < 0.55) {
                continue;
            }
            float a = (corner + 0.5F) * Mth.HALF_PI + (float) (Noise.of(beat, corner, 12) - 0.5) * 0.5F;
            float dx = Mth.sin(a);
            float dy = -Mth.cos(a);
            float kink = (float) (Noise.of(beat, corner, 13) - 0.5) * 1.6F;
            float from = rune * 0.75F;
            float x1 = cx + dx * (from + 1.4F) - dy * kink;
            float y1 = cy + dy * (from + 1.4F) + dx * kink;
            Crosshairs.stroke(graphics, cx + dx * from, cy + dy * from, x1, y1, 0.6F, WHITE, 0.85F);
            Crosshairs.stroke(graphics, x1, y1, cx + dx * (from + 2.8F), cy + dy * (from + 2.8F), 0.6F, WHITE, 0.7F);
        }
    }
}
