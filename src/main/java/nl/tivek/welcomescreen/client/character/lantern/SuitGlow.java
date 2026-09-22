package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.character.lantern.LandingSlam;
import nl.tivek.welcomescreen.client.character.ClientCharacter;
import nl.tivek.welcomescreen.client.character.MouseHold;
import nl.tivek.welcomescreen.network.RingPayload;
import org.joml.Matrix4f;

/**
 * The uniform lighting up while the ring works. Only then: a resting ring leaves the suit as it is. The more the
 * ring does (a shield, a fist, flying, the beam), the brighter it gets:
 * <ul>
 * <li>the whole uniform glows green from head to toe, with a soft haze of light around it;</li>
 * <li>the lantern on the chest, the core of the suit, burns bright;</li>
 * <li>the energy streams out of the core: first and most of all over the right shoulder and down the right arm
 * into the ring, the thickest and fastest stream of all, and from there on through the rest of the body, the
 * other arm, the back and the legs;</li>
 * <li>the ring itself flares (see {@link Ring}).</li>
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
    private static final float FLOW = 1.8F;
    private static final float GAP = 6.0F;
    // The light lying over the whole uniform, and the haze around it: how far out each lies, in pixels, and how
    // bright each is at full strength.
    private static final float SHELL = SUIT + 0.12F;
    private static final float HAZE = SUIT + 1.1F;
    private static final float SHELL_LIGHT = 0.3F;
    private static final float HAZE_LIGHT = 0.16F;
    // The main stream (core to ring) is this much wider than the others.
    private static final float MAIN = 1.6F;
    /**
     * The light over the uniform: added on top like the other glow, but only on the sides that face you, so a side
     * behind never adds to one in front.
     */
    private static final RenderType SHELL_TYPE = RenderType.create("welcomescreen_suit_shell",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));
    // The boxes of the body's parts, in each part's own pixels: {x0, y0, z0, x1, y1, z1}.
    private static final float[] HEAD = { -4, -8, -4, 4, 0, 4 };
    private static final float[] TORSO = { -4, 0, -2, 4, 12, 2 };
    private static final float[] LEG_BOX = { -2, 0, -2, 2, 12, 2 };

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
        // A landing slam: the ring throws everything it has into the construct.
        float slam = ClientFlight.slam(player, partialTick);
        if (slam >= 0.0F && slam < LandingSlam.BURST_TICK) {
            want = 1.0F;
        }
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
        // The light over the whole uniform and the haze around it, breathing slowly.
        float breath = glow * (0.88F + 0.12F * Mth.sin(time * 0.25F));
        VertexConsumer shell = buffers.getBuffer(SHELL_TYPE);
        shell(pose, shell, suit.head, HEAD, breath * 0.7F);
        shell(pose, shell, suit.body, TORSO, breath);
        shell(pose, shell, suit.rightArm, armBox(true, slim), breath * 1.15F);
        shell(pose, shell, suit.leftArm, armBox(false, slim), breath * 0.85F);
        shell(pose, shell, suit.rightLeg, LEG_BOX, breath * 0.8F);
        shell(pose, shell, suit.leftLeg, LEG_BOX, breath * 0.8F);
        VertexConsumer light = buffers.getBuffer(Ring.HALO);
        pose.pushPose();
        suit.body.translateAndRotate(pose);
        chest(light, pose.last().pose(), glow, time);
        pose.popPose();
        part(pose, light, suit.body, glow, time, MAIN_BODY, MAIN);
        part(pose, light, suit.body, glow * 0.8F, time, BODY, 1.0F);
        part(pose, light, suit.rightArm, glow, time, rightArm(slim), MAIN);
        part(pose, light, suit.leftArm, glow * 0.75F, time, leftArm(slim), 1.0F);
        part(pose, light, suit.rightLeg, glow * 0.7F, time, LEG, 1.0F);
        part(pose, light, suit.leftLeg, glow * 0.7F, time, LEG, 1.0F);
    }

    /** One arm of your own in first person: the light over it, the lines on it, and the flow into the ring. */
    static void arm(PoseStack pose, MultiBufferSource buffers, ModelPart sleeve, boolean right, boolean slim,
            float glow, float time) {
        if (glow <= 0.01F) {
            return;
        }
        // Right in front of your eyes the haze would cover half the screen, so it stays fainter here.
        shell(pose, buffers.getBuffer(SHELL_TYPE), sleeve, armBox(right, slim), glow * (right ? 0.8F : 0.6F));
        part(pose, buffers.getBuffer(Ring.HALO), sleeve, right ? glow : glow * 0.75F, time,
                right ? rightArm(slim) : leftArm(slim), right ? MAIN : 1.0F);
    }

    private static void part(PoseStack pose, VertexConsumer light, ModelPart part, float glow, float time,
            float[][] lines, float width) {
        if (!part.visible) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        Matrix4f matrix = pose.last().pose();
        for (float[] line : lines) {
            path(light, matrix, line, glow, time, width);
        }
        pose.popPose();
    }

    /** The box of an arm, in its own pixels: slim arms are a pixel narrower on the outside. */
    private static float[] armBox(boolean right, boolean slim) {
        float narrow = slim ? 1.0F : 0.0F;
        return right ? new float[] { -3 + narrow, -2, -2, 1, 10, 2 } : new float[] { -1, -2, -2, 3 - narrow, 10, 2 };
    }

    /**
     * The glow of one part of the uniform: a thin layer of green light right over it, so the whole uniform reads
     * as lit from within, and a haze further out that is brightest in the middle of each side and fades to nothing
     * at its edges, so it looks like light spilling off the body rather than a box around it.
     */
    private static void shell(PoseStack pose, VertexConsumer shell, ModelPart part, float[] box, float glow) {
        if (!part.visible || glow <= 0.01F) {
            return;
        }
        pose.pushPose();
        part.translateAndRotate(pose);
        Matrix4f matrix = pose.last().pose();
        for (int face = 0; face < 6; face++) {
            side(shell, matrix, box, SHELL, face, GREEN, glow * SHELL_LIGHT, glow * SHELL_LIGHT);
            side(shell, matrix, box, HAZE, face, BRIGHT, glow * HAZE_LIGHT, 0.0F);
        }
        pose.popPose();
    }

    /**
     * One side of a box grown by {@code out} pixels, as four pieces around its middle: {@code middle} is the light
     * in the middle of the side and {@code edge} at its edges. The corners run the right way round for each side, so
     * only the sides that face the camera are drawn.
     *
     * @param face 0 = -x, 1 = +x, 2 = -y, 3 = +y, 4 = -z, 5 = +z
     */
    private static void side(VertexConsumer shell, Matrix4f matrix, float[] box, float out, int face, int rgb,
            float middle, float edge) {
        float x0 = box[0] - out;
        float y0 = box[1] - out;
        float z0 = box[2] - out;
        float x1 = box[3] + out;
        float y1 = box[4] + out;
        float z1 = box[5] + out;
        float[][] c = switch (face) {
            case 0 -> new float[][] { { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 }, { x0, y1, z0 } };
            case 1 -> new float[][] { { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 }, { x1, y1, z1 } };
            case 2 -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
            case 3 -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
            case 4 -> new float[][] { { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 }, { x1, y1, z0 } };
            default -> new float[][] { { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 }, { x0, y1, z1 } };
        };
        float mx = (c[0][0] + c[2][0]) * 0.5F;
        float my = (c[0][1] + c[2][1]) * 0.5F;
        float mz = (c[0][2] + c[2][2]) * 0.5F;
        for (int i = 0; i < 4; i++) {
            float[] a = c[i];
            float[] b = c[(i + 1) % 4];
            vertex(shell, matrix, a[0], a[1], a[2], rgb, edge);
            vertex(shell, matrix, b[0], b[1], b[2], rgb, edge);
            vertex(shell, matrix, mx, my, mz, rgb, middle);
            vertex(shell, matrix, mx, my, mz, rgb, middle);
        }
    }

    // ---- The lines, in each part's own pixels ----
    // A line is a side and a run of points on that side of the part's box: {side, start, x0, y0, z0, x1, y1, z1,
    // ...}. The side is the way it looks (0 = -x, 1 = +x, 4 = -z the front, 5 = +z the back); start is how far from
    // the core the line begins, in pixels, so the pulses run on from one part into the next.

    /**
     * The main stream on the body: out of the lantern on the chest over the right shoulder (the right side of the
     * body is its -x), on the front and, straight through, on the back, so it shows from every side. It goes on down
     * the right arm into the ring.
     */
    private static final float[][] MAIN_BODY = {
            { 4, 0, -1.5F, 2.4F, -2, -3.1F, 0.9F, -2, -4.0F, 0.2F, -2 },
            { 5, 0, -0.5F, 3.0F, 2, -2.6F, 1.4F, 2, -4.0F, 0.2F, 2 } };

    /** The rest of the body: to the other shoulder, down the middle and the sides to the legs, and down the back. */
    private static final float[][] BODY = {
            { 4, 0, 1.5F, 2.4F, -2, 3.1F, 0.9F, -2, 4.0F, 0.2F, -2 },
            { 4, 0, -0.5F, 4.8F, -2, -0.5F, 8.6F, -2, -1.9F, 11.8F, -2 },
            { 4, 4, -0.5F, 8.6F, -2, 1.9F, 11.8F, -2 },
            { 4, 2, -1.5F, 3.6F, -2, -3.4F, 6.8F, -2, -3.6F, 11.6F, -2 },
            { 4, 2, 1.5F, 3.6F, -2, 3.4F, 6.8F, -2, 3.6F, 11.6F, -2 },
            { 5, 0, 0.5F, 3.0F, 2, 2.6F, 1.4F, 2, 4.0F, 0.2F, 2 },
            { 5, 0, 0.0F, 3.0F, 2, 0.0F, 7.5F, 2, -1.9F, 11.8F, 2 },
            { 5, 5, 0.0F, 7.5F, 2, 1.9F, 11.8F, 2 } };

    /**
     * The right arm, the end of the main stream: a thick line down its outer side (the back of the hand) straight
     * into the ring, one down its front and one down its back.
     */
    private static float[][] rightArm(boolean slim) {
        float out = slim ? -2.0F : -3.0F;
        return new float[][] { { 0, 6, out, -1.8F, -0.5F, out, 3.8F, -0.5F, out, 8.0F, -0.5F },
                { 4, 6, out + 1.2F, -1.8F, -2, out + 1.0F, 4.0F, -2, out + 1.2F, 9.6F, -2 },
                { 5, 6, out + 1.2F, -1.8F, 2, out + 1.0F, 4.0F, 2, out + 1.2F, 9.6F, 2 } };
    }

    /** The left arm: the same lines, weaker, running down to the hand. */
    private static float[][] leftArm(boolean slim) {
        float out = slim ? 2.0F : 3.0F;
        return new float[][] { { 1, 6, out, -1.8F, -0.5F, out, 4.0F, -0.5F, out, 9.6F, -0.5F },
                { 4, 6, out - 1.2F, -1.8F, -2, out - 1.0F, 4.0F, -2, out - 1.2F, 9.6F, -2 },
                { 5, 6, out - 1.2F, -1.8F, 2, out - 1.0F, 4.0F, 2, out - 1.2F, 9.6F, 2 } };
    }

    /** A leg: one line down its front and one down its back. */
    private static final float[][] LEG = { { 4, 12, 0, 0.3F, -2, 0.2F, 6.0F, -2, 0, 11.6F, -2 },
            { 5, 12, 0, 0.3F, 2, -0.2F, 6.0F, 2, 0, 11.6F, 2 } };

    /**
     * One line: a bright core and a soft glow either side, lying flat on its side of the part, with pulses of
     * light running along it away from the core.
     *
     * @param width how much wider than an ordinary line it is (the main stream is wider)
     */
    private static void path(VertexConsumer light, Matrix4f matrix, float[] line, float glow, float time,
            float width) {
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
                float bright = glow * (0.7F + 0.3F * pulse);
                segment(light, matrix, face, Mth.lerp(a, x0, x1), Mth.lerp(a, y0, y1), Mth.lerp(a, z0, z1),
                        Mth.lerp(b, x0, x1), Mth.lerp(b, y0, y1), Mth.lerp(b, z0, z1), bright, pulse, width);
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
            float x1, float y1, float z1, float bright, float pulse, float width) {
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
        float core = (0.42F + 0.3F * pulse) * width;
        float halo = (1.9F + 0.9F * pulse) * width;
        int coreRgb = Ring.mix(BRIGHT, 0xE6FFEC, pulse);
        quad(light, matrix, face, x0, y0, z0, x1, y1, z1, ny, na, core, coreRgb, bright);
        quad(light, matrix, face, x0, y0, z0, x1, y1, z1, ny, na, halo, GREEN, bright * 0.6F);
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
            // The soft light over the chest, and the hot middle of the core.
            fan(light, matrix, cx, cy, z, a0, a1, 0.0F, 5.5F * beat, GREEN, 0.85F * glow, 0.0F);
            fan(light, matrix, cx, cy, z, a0, a1, 0.0F, 1.1F, 0xE6FFEC, 0.8F * glow * beat, 0.3F * glow);
            // The lantern's ring, bright.
            ringBand(light, matrix, cx, cy, z, a0, a1, 1.0F, 1.6F, BRIGHT, glow * beat);
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
