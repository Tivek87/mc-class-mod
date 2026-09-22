package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.character.ClientCharacter;
import nl.tivek.welcomescreen.client.character.MouseHold;
import nl.tivek.welcomescreen.network.RingPayload;
import org.joml.Matrix4f;

/**
 * The uniform lighting up while the ring works. Only then: a resting ring leaves the suit as it is. The more the
 * ring does (a shield, a fist, flying, the beam), the brighter it gets:
 * <ul>
 * <li>the lantern on the chest, the core of the suit, glows;</li>
 * <li>lines of green energy light up all over the suit, and bright pulses run along them out of the core;</li>
 * <li>most of it runs down the right arm into the ring, the thickest line of all, and the ring itself flares
 * (see {@link Ring}).</li>
 * </ul>
 * Drawn on the body seen from outside, and on your own arms in first person.
 */
final class SuitGlow {
    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0x9CFFB4;
    // How quickly the glow comes up and dies down again, per second.
    private static final float RISE = 10.0F;
    private static final float FALL = 2.5F;
    // How far the uniform lies over the body's own boxes, and how far above the uniform the lines lie, in pixels:
    // just clear of it, so they are never swallowed by it.
    private static final float SUIT = 0.3F;
    private static final float LIFT = 0.06F;
    // How fast the pulses run out of the core, in pixels per tick, and how far apart they are.
    private static final float FLOW = 0.9F;
    private static final float GAP = 7.0F;

    private static final Map<Integer, Level> LEVELS = new HashMap<>();

    private SuitGlow() {
    }

    /** How brightly one player's suit glows, eased from frame to frame. */
    private static final class Level {
        float value;
        long last = Util.getMillis();
    }

    /**
     * How hard this player's ring works right now, 0 = resting to 1 = as hard as it goes: what it holds up or
     * pours out, whether he flies, charges a fist or shot a bolt just now, and for yourself how far you hold a
     * button on its way to the hold version.
     */
    static float level(AbstractClientPlayer player, float partialTick) {
        float want = 0.0F;
        if (ClientRing.has(player, RingPayload.BEAM)) {
            want = 1.0F;
        }
        if (ClientRing.has(player, RingPayload.DOME)) {
            want = Math.max(want, 0.85F);
        }
        float flight = ClientRing.flight(player, partialTick);
        if (flight >= 0.0F && !ClientRing.has(player, RingPayload.DESCENT)) {
            // The take-off is the ring's hardest moment of a flight.
            float arise = flight < ClientFlight.ARISE ? 1.0F - flight / ClientFlight.ARISE * 0.25F : 0.75F;
            want = Math.max(want, arise);
        }
        if (ClientRing.has(player, RingPayload.SHIELD)) {
            want = Math.max(want, 0.55F);
        }
        float recharge = ClientRing.recharge(player, partialTick);
        if (recharge >= 0.0F) {
            want = Math.max(want, Mth.clamp(RechargeAnimation.glow(recharge) * 0.6F, 0.0F, 1.0F));
        }
        want = Math.max(want, ClientConstructs.working(player.getId()));
        if (player == Minecraft.getInstance().player && ClientCharacter.active() == GameCharacter.GREEN_LANTERN) {
            for (CharacterAbility ability : GameCharacter.GREEN_LANTERN.abilities()) {
                float hold = MouseHold.progress(ability, partialTick);
                if (hold > 0.1F) {
                    want = Math.max(want, 0.25F + 0.65F * hold);
                }
            }
        }
        Level level = LEVELS.computeIfAbsent(player.getId(), id -> new Level());
        long now = Util.getMillis();
        float seconds = Math.min(0.25F, (now - level.last) / 1000.0F);
        level.last = now;
        float rate = want > level.value ? RISE : FALL;
        level.value = Mth.lerp(1.0F - (float) Math.exp(-rate * seconds), level.value, want);
        return level.value;
    }

    /** Forgets everyone (you left the world). */
    static void clear() {
        LEVELS.clear();
    }

    // ---- Seen from outside ----

    /** The whole suit lit up, on a body posed as {@code suit}; {@code glow} from {@link #level}. */
    static void body(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> suit, boolean slim,
            float glow, float time) {
        if (glow <= 0.01F) {
            return;
        }
        VertexConsumer light = buffers.getBuffer(Ring.HALO);
        pose.pushPose();
        suit.body.translateAndRotate(pose);
        chest(light, pose.last().pose(), glow, time);
        pose.popPose();
        part(pose, light, suit.body, glow, time, BODY);
        part(pose, light, suit.rightArm, glow, time, rightArm(slim));
        part(pose, light, suit.leftArm, glow * 0.6F, time, leftArm(slim));
        part(pose, light, suit.rightLeg, glow * 0.5F, time, LEG);
        part(pose, light, suit.leftLeg, glow * 0.5F, time, LEG);
    }

    /** One arm of your own in first person: the lines on it, and for the right arm the flow into the ring. */
    static void arm(PoseStack pose, MultiBufferSource buffers, ModelPart sleeve, boolean right, boolean slim,
            float glow, float time) {
        if (glow <= 0.01F) {
            return;
        }
        part(pose, buffers.getBuffer(Ring.HALO), sleeve, right ? glow : glow * 0.6F, time,
                right ? rightArm(slim) : leftArm(slim));
    }

    private static void part(PoseStack pose, VertexConsumer light, ModelPart part, float glow, float time,
            float[][] lines) {
        if (!part.visible) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        Matrix4f matrix = pose.last().pose();
        for (float[] line : lines) {
            path(light, matrix, line, glow, time);
        }
        pose.popPose();
    }

    // ---- The lines, in each part's own pixels ----
    // A line is a side and a run of points on that side of the part's box: {side, start, x0, y0, z0, x1, y1, z1,
    // ...}. The side is the way it looks (0 = -x, 1 = +x, 4 = -z the front, 5 = +z the back); start is how far from
    // the core the line begins, in pixels, so the pulses run on from one part into the next.

    /** The body: from the lantern on the chest up to both shoulders, down the sides and down to the legs. */
    private static final float[][] BODY = {
            { 4, 0, -1.5F, 2.4F, -2, -3.1F, 0.9F, -2, -4.0F, 0.2F, -2 },
            { 4, 0, 1.5F, 2.4F, -2, 3.1F, 0.9F, -2, 4.0F, 0.2F, -2 },
            { 4, 0, -0.5F, 4.8F, -2, -0.5F, 8.6F, -2, -1.9F, 11.8F, -2 },
            { 4, 4, -0.5F, 8.6F, -2, 1.9F, 11.8F, -2 },
            { 4, 2, -1.5F, 3.6F, -2, -3.4F, 6.8F, -2, -3.6F, 11.6F, -2 },
            { 4, 2, 1.5F, 3.6F, -2, 3.4F, 6.8F, -2, 3.6F, 11.6F, -2 } };

    /**
     * The right arm: one thick line down its outer side (the back of the hand) straight into the ring, and a
     * thinner one down its front.
     */
    private static float[][] rightArm(boolean slim) {
        float out = slim ? -2.0F : -3.0F;
        return new float[][] { { 0, 6, out, -1.8F, -0.5F, out, 3.8F, -0.5F, out, 8.0F, -0.5F },
                { 4, 6, out + 1.2F, -1.8F, -2, out + 1.0F, 4.0F, -2, out + 1.2F, 9.6F, -2 } };
    }

    /** The left arm: the same lines, weaker, running down to the hand. */
    private static float[][] leftArm(boolean slim) {
        float out = slim ? 2.0F : 3.0F;
        return new float[][] { { 1, 6, out, -1.8F, -0.5F, out, 4.0F, -0.5F, out, 9.6F, -0.5F },
                { 4, 6, out - 1.2F, -1.8F, -2, out - 1.0F, 4.0F, -2, out - 1.2F, 9.6F, -2 } };
    }

    /** A leg: one line down its front. */
    private static final float[][] LEG = { { 4, 12, 0, 0.3F, -2, 0.2F, 6.0F, -2, 0, 11.6F, -2 } };

    /**
     * One line: a bright core and a soft glow either side, lying flat on its side of the part, with pulses of
     * light running along it away from the core.
     */
    private static void path(VertexConsumer light, Matrix4f matrix, float[] line, float glow, float time) {
        int face = (int) line[0];
        float along = line[1];
        for (int i = 2; i + 5 < line.length; i += 3) {
            float x0 = line[i];
            float y0 = line[i + 1];
            float z0 = line[i + 2];
            float x1 = line[i + 3];
            float y1 = line[i + 4];
            float z1 = line[i + 5];
            float length = Mth.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
            int steps = Math.max(1, (int) Math.ceil(length / 0.8F));
            for (int s = 0; s < steps; s++) {
                float a = (float) s / steps;
                float b = (float) (s + 1) / steps;
                float d = along + length * (a + b) * 0.5F;
                float pulse = pulse(d, time);
                float bright = glow * (0.45F + 0.55F * pulse);
                segment(light, matrix, face, Mth.lerp(a, x0, x1), Mth.lerp(a, y0, y1), Mth.lerp(a, z0, z1),
                        Mth.lerp(b, x0, x1), Mth.lerp(b, y0, y1), Mth.lerp(b, z0, z1), bright, pulse);
            }
            along += length;
        }
    }

    /** 0 to 1: how much of a pulse is at this distance from the core right now. */
    private static float pulse(float distance, float time) {
        float phase = (distance - time * FLOW) / GAP;
        float wave = phase - Mth.floor(phase);
        // A short bright head and a trailing tail, running outwards.
        return wave > 0.8F ? (1.0F - wave) / 0.2F : (float) Math.pow(wave / 0.8F, 3.0);
    }

    /**
     * One piece of a line, flat on its side: the side is found from the face and the points' own numbers. The
     * arms keep their line on the given x (outer side) or z (front); the body always on its front.
     */
    private static void segment(VertexConsumer light, Matrix4f matrix, int face, float x0, float y0, float z0,
            float x1, float y1, float z1, float bright, float pulse) {
        boolean sideways = face == 0 || face == 1;
        // Across the line, in the plane of its side.
        float dy = y1 - y0;
        float da = sideways ? z1 - z0 : x1 - x0;
        float length = Mth.sqrt(dy * dy + da * da);
        if (length < 1.0E-4F) {
            return;
        }
        float ny = -da / length;
        float na = dy / length;
        float core = 0.22F + 0.18F * pulse;
        float halo = 0.9F + 0.5F * pulse;
        int coreRgb = Ring.mix(GREEN, BRIGHT, 0.5F + 0.5F * pulse);
        quad(light, matrix, face, x0, y0, z0, x1, y1, z1, ny, na, core, coreRgb, bright);
        quad(light, matrix, face, x0, y0, z0, x1, y1, z1, ny, na, halo, GREEN, bright * 0.45F);
    }

    /** A strip along a piece of line, strongest along its middle and fading out to both edges. */
    private static void quad(VertexConsumer light, Matrix4f matrix, int face, float x0, float y0, float z0, float x1,
            float y1, float z1, float ny, float na, float width, int rgb, float alpha) {
        float half = width * 0.5F;
        // The line itself, then each edge.
        float[] a = at(face, x0, y0, z0);
        float[] b = at(face, x1, y1, z1);
        float[] across = face == 0 || face == 1 ? new float[] { 0, ny * half, na * half }
                : new float[] { na * half, ny * half, 0 };
        for (int side = -1; side <= 1; side += 2) {
            vertex(light, matrix, a[0], a[1], a[2], rgb, alpha);
            vertex(light, matrix, b[0], b[1], b[2], rgb, alpha);
            vertex(light, matrix, b[0] + side * across[0], b[1] + side * across[1], b[2] + side * across[2], rgb, 0);
            vertex(light, matrix, a[0] + side * across[0], a[1] + side * across[1], a[2] + side * across[2], rgb, 0);
        }
    }

    /** A point of a line lifted off the part's box onto the uniform, and just clear of it. */
    private static float[] at(int face, float x, float y, float z) {
        float push = SUIT + LIFT;
        return switch (face) {
            case 0 -> new float[] { x - push, y, z };
            case 1 -> new float[] { x + push, y, z };
            case 5 -> new float[] { x, y, z + push };
            default -> new float[] { x, y, z - push };
        };
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int rgb,
            float alpha) {
        buffer.addVertex(matrix, x / 16.0F, y / 16.0F, z / 16.0F).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF,
                rgb & 0xFF, (int) (255 * Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    /**
     * The core: the lantern on the chest glowing from inside, a bright ring around it and a soft light over the
     * chest that swells with every pulse.
     */
    private static void chest(VertexConsumer light, Matrix4f matrix, float glow, float time) {
        float z = -2.0F - SUIT - LIFT;
        float cx = -0.5F;
        float cy = 3.0F;
        float beat = 0.8F + 0.2F * Mth.sin(time * 0.35F);
        int sides = 18;
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides;
            float a1 = Mth.TWO_PI * (i + 1) / sides;
            // The soft light over the chest.
            fan(light, matrix, cx, cy, z, a0, a1, 0.0F, 3.4F * beat, GREEN, 0.5F * glow, 0.0F);
            // The lantern's ring, bright.
            fan(light, matrix, cx, cy, z, a0, a1, 1.0F, 1.55F, BRIGHT, 0.0F, 0.0F);
            ringBand(light, matrix, cx, cy, z, a0, a1, 1.05F, 1.5F, BRIGHT, glow * beat);
        }
        // The two bars of the lantern symbol, above and below the ring.
        bar(light, matrix, cx, cy - 1.9F, z, 1.4F, 0.3F, glow * beat);
        bar(light, matrix, cx, cy + 1.9F, z, 1.4F, 0.3F, glow * beat);
    }

    /** A slice of a disc around (cx, cy) on the chest, from radius r0 to r1, fading from a0 to a1 in alpha. */
    private static void fan(VertexConsumer light, Matrix4f matrix, float cx, float cy, float z, float a0, float a1,
            float r0, float r1, int rgb, float alphaIn, float alphaOut) {
        if (alphaIn <= 0.0F && alphaOut <= 0.0F) {
            return;
        }
        vertex(light, matrix, cx + Mth.cos(a0) * r0, cy + Mth.sin(a0) * r0, z, rgb, alphaIn);
        vertex(light, matrix, cx + Mth.cos(a0) * r1, cy + Mth.sin(a0) * r1, z, rgb, alphaOut);
        vertex(light, matrix, cx + Mth.cos(a1) * r1, cy + Mth.sin(a1) * r1, z, rgb, alphaOut);
        vertex(light, matrix, cx + Mth.cos(a1) * r0, cy + Mth.sin(a1) * r0, z, rgb, alphaIn);
    }

    /** A bright band of a ring around (cx, cy), fully lit across its width. */
    private static void ringBand(VertexConsumer light, Matrix4f matrix, float cx, float cy, float z, float a0,
            float a1, float r0, float r1, int rgb, float alpha) {
        vertex(light, matrix, cx + Mth.cos(a0) * r0, cy + Mth.sin(a0) * r0, z, rgb, alpha);
        vertex(light, matrix, cx + Mth.cos(a0) * r1, cy + Mth.sin(a0) * r1, z, rgb, alpha);
        vertex(light, matrix, cx + Mth.cos(a1) * r1, cy + Mth.sin(a1) * r1, z, rgb, alpha);
        vertex(light, matrix, cx + Mth.cos(a1) * r0, cy + Mth.sin(a1) * r0, z, rgb, alpha);
    }

    /** One bar of the lantern symbol, centred on (cx, cy). */
    private static void bar(VertexConsumer light, Matrix4f matrix, float cx, float cy, float z, float half,
            float thick, float alpha) {
        vertex(light, matrix, cx - half, cy - thick, z, BRIGHT, alpha);
        vertex(light, matrix, cx + half, cy - thick, z, BRIGHT, alpha);
        vertex(light, matrix, cx + half, cy + thick, z, BRIGHT, alpha);
        vertex(light, matrix, cx - half, cy + thick, z, BRIGHT, alpha);
    }
}
