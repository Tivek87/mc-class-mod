package nl.tivek.multiversepowers.character.greenlantern.client.body;

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
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientFlight;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientLooks;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import org.joml.Matrix4f;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.BACK;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.BACK_MAIN;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.CHEST;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.CHEST_MAIN;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.FLANKS;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.HEAD_LINES;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.LEG;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.leftArm;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.lines;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.part;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.rightArm;
import static nl.tivek.multiversepowers.character.greenlantern.client.body.SuitLines.rightArmMain;

/**
 * The uniform lighting up while the ring works. Only then: a resting ring leaves the suit as it is. The more the
 * ring does (a shield, a fist, flying, the beam), the brighter it gets:
 * <ul>
 * <li>the whole uniform glows green from head to toe, with a soft haze of light around it;</li>
 * <li>the lantern on the chest, the core of the suit, burns bright;</li>
 * <li>the energy streams out of the core along lines of light all over the suit: most of it, the thickest and
 * fastest, over the right shoulder and down the right arm into the ring, and from there on over the chest and the
 * back, down the flanks, the other arm and the legs, and up the back of the head;</li>
 * <li>the ring itself flares (see {@link Ring}).</li>
 * </ul>
 * While the ring gathers its light for the beam (see {@link BeamCharge}) the energy fills the suit bit by bit: it runs
 * out of the core down the ring arm first, the arm glowing brighter and brighter, and then over the rest of the suit,
 * its front a bright band of light, until everything is lit as the beam breaks loose.
 * Drawn on the body seen from outside, and on your own arms in first person.
 */
public final class SuitGlow {
    static final int GREEN = 0x3CE86A;
    static final int BRIGHT = 0x9CFFB4;
    // How quickly the glow comes up and dies down again, per second.
    private static final float RISE = 10.0F;
    private static final float FALL = 2.5F;
    // How long the surge of light over a uniform just made complete by the ring's arrival lasts, in ticks.
    private static final float SURGE_TICKS = 14.0F;
    // How far the uniform lies over the body's own boxes, and how far above the uniform the lines lie, in pixels:
    // just clear of it, so they are never swallowed by it.
    static final float SUIT = 0.3F;
    static final float LIFT = 0.06F;
    // The light lying over the whole uniform, and the haze around it: how far out each lies, in pixels, and how
    // bright each is at full strength.
    private static final float SHELL = SUIT + 0.12F;
    private static final float HAZE = SUIT + 1.1F;
    private static final float SHELL_LIGHT = 0.3F;
    private static final float HAZE_LIGHT = 0.16F;
    // The main stream (core to ring) is this much wider than the others, and its pulses run this much faster.
    private static final float MAIN = 1.4F;
    private static final float FAST = 1.7F;
    // While the ring gathers its light for the beam the energy fills the suit out of the core: how far along its lines
    // it has got (in pixels from the core) once the ring arm is full and once the whole suit is. The ring arm is full
    // at this much of the charge; the rest of the suit starts to fill at this much. Its front is a band of light this
    // many pixels long, and the pulses run this much faster at full charge; the ring arm glows this much brighter.
    private static final float ARM_FULL = 16.0F;
    private static final float SUIT_FULL = 22.0F;
    private static final float ARM_AT = 0.55F;
    private static final float SUIT_FROM = 0.3F;
    static final float FRONT = 1.6F;
    private static final float RUSH = 1.5F;
    private static final float ARM_SHINE = 1.0F;
    // No charge: every line is lit all the way.
    static final float ALL = Float.MAX_VALUE;
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
     * pours out, whether he flies, goes down to a slam, charges a fist or shot a bolt just now, and for yourself how
     * far you hold a button on its way to the hold version.
     */
    public static float level(AbstractClientPlayer player, float partialTick) {
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
        // On the way down to a slam: the ring drives him down.
        if (ClientRing.has(player, RingPayload.DIVE)) {
            want = Math.max(want, 0.9F);
        }
        // The uniform just complete at the end of the ring's arrival: a surge of its light runs over all of it.
        float dressed = ClientRing.arrival(player, partialTick) - Arrival.DRESSED;
        if (dressed >= 0.0F && dressed < SURGE_TICKS) {
            want = Math.max(want, 1.0F - dressed / SURGE_TICKS);
        }
        float recharge = ClientRing.recharge(player, partialTick);
        if (recharge >= 0.0F) {
            want = Math.max(want, Mth.clamp(RechargeAnimation.glow(recharge) * 0.6F, 0.0F, 1.0F));
        }
        want = Math.max(want, ClientConstructs.working(player.getId(), partialTick));
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
        // Gathering light for the beam: it rises with the charge, for everyone who sees it.
        float charge = BeamCharge.charge(player, partialTick);
        if (charge >= 0.0F) {
            want = Math.max(want, 0.3F + 0.7F * charge);
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
    public static void clear() {
        LEVELS.clear();
    }

    // ---- Seen from outside ----

    /**
     * The whole suit lit up, on a body posed as {@code suit}; {@code glow} from {@link #level}. While he kneels his
     * legs bend (see {@link KneelLegs}) and the light on them, made for straight legs, is left out.
     *
     * @param charge how far his ring has gathered its light for the beam, 0 to 1, or -1 when it gathers none (see
     *               {@link BeamCharge#charge})
     */
    static void body(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> suit, boolean slim,
            float glow, float time, boolean kneeling, float charge) {
        if (glow <= 0.01F) {
            return;
        }
        float arm = armReach(charge);
        float rest = restReach(charge);
        float rush = charge < 0.0F ? 1.0F : 1.0F + RUSH * charge;
        // The light over the whole uniform and the haze around it, breathing slowly. While the ring gathers its light
        // for the beam, each part only lights up as the energy reaches it: the chest first, then the ring arm, then
        // the rest.
        float breath = glow * (0.88F + 0.12F * Mth.sin(time * 0.25F));
        float chest = charge < 0.0F ? 1.0F : 0.45F + 0.55F * Mth.clamp(charge / 0.15F, 0.0F, 1.0F);
        float others = charge < 0.0F ? 1.0F : restFill(charge);
        float ring = charge < 0.0F ? 1.15F : (1.15F + ARM_SHINE) * armFill(charge);
        VertexConsumer shell = buffers.getBuffer(SHELL_TYPE);
        shell(pose, shell, suit.head, HEAD, breath * 0.7F * others);
        shell(pose, shell, suit.body, TORSO, breath * chest);
        shell(pose, shell, suit.rightArm, armBox(true, slim), breath * ring);
        shell(pose, shell, suit.leftArm, armBox(false, slim), breath * 0.85F * others);
        if (!kneeling) {
            shell(pose, shell, suit.rightLeg, LEG_BOX, breath * 0.8F * others);
            shell(pose, shell, suit.leftLeg, LEG_BOX, breath * 0.8F * others);
        }
        VertexConsumer light = buffers.getBuffer(Ring.HALO);
        pose.pushPose();
        suit.body.translateAndRotate(pose);
        Matrix4f torso = pose.last().pose();
        chest(light, torso, glow, time);
        node(light, torso, glow * 0.6F, time);
        lines(light, torso, CHEST_MAIN, glow, time, MAIN, FAST * rush, arm);
        lines(light, torso, CHEST, glow * 0.85F, time, 1.0F, 1.0F, rest);
        lines(light, torso, BACK_MAIN, glow * 0.9F, time, MAIN, FAST * rush, arm);
        lines(light, torso, BACK, glow * 0.75F, time, 1.0F, 1.0F, rest);
        lines(light, torso, FLANKS, glow * 0.7F, time, 1.0F, 1.0F, rest);
        pose.popPose();
        part(pose, light, suit.rightArm, glow, time, rightArmMain(slim), MAIN, FAST * rush, arm);
        part(pose, light, suit.rightArm, glow * 0.9F, time, rightArm(slim), 1.0F, FAST * rush, arm);
        part(pose, light, suit.leftArm, glow * 0.75F, time, leftArm(slim), 1.0F, 1.0F, rest);
        if (!kneeling) {
            part(pose, light, suit.rightLeg, glow * 0.7F, time, LEG, 1.0F, 1.0F, rest);
            part(pose, light, suit.leftLeg, glow * 0.7F, time, LEG, 1.0F, 1.0F, rest);
        }
        part(pose, light, suit.head, glow * 0.45F, time, HEAD_LINES, 0.8F, 1.0F, rest);
    }

    /** How far out of the core the energy has got along the ring arm's lines, in pixels, for this charge. */
    private static float armReach(float charge) {
        return charge < 0.0F ? ALL : ARM_FULL * armFill(charge);
    }

    /** How far out of the core the energy has got along the lines of the rest of the suit, in pixels. */
    private static float restReach(float charge) {
        return charge < 0.0F ? ALL : SUIT_FULL * restFill(charge);
    }

    /** How full the rest of the suit is of the energy, 0 to 1: 0 with no charge going. */
    private static float restFill(float charge) {
        return charge < 0.0F ? 0.0F : Mth.clamp((charge - SUIT_FROM) / (1.0F - SUIT_FROM), 0.0F, 1.0F);
    }

    /** How full the ring arm is of the energy, 0 to 1: 0 with no charge going. */
    private static float armFill(float charge) {
        return charge < 0.0F ? 0.0F : Mth.clamp(charge / ARM_AT, 0.0F, 1.0F);
    }

    /**
     * Only the lantern on the chest, flaring: the uniform bursts out of it while the ring dresses him.
     *
     * @param strength 0 to 1, from {@link ClientLooks.Uniform#core}
     */
    static void core(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> suit, float strength,
            float time) {
        if (strength <= 0.01F) {
            return;
        }
        VertexConsumer light = buffers.getBuffer(Ring.HALO);
        pose.pushPose();
        suit.body.translateAndRotate(pose);
        Matrix4f torso = pose.last().pose();
        chest(light, torso, 1.4F * strength, time);
        node(light, torso, strength, time);
        pose.popPose();
    }

    /**
     * One arm of your own in first person: the light over it, and its lines into the ring.
     *
     * @param charge how far your ring has gathered its light for the beam, or -1 (see {@link BeamCharge#charge})
     */
    public static void arm(PoseStack pose, MultiBufferSource buffers, ModelPart sleeve, boolean right, boolean slim,
            float glow, float time, float charge) {
        if (glow <= 0.01F) {
            return;
        }
        float rush = charge < 0.0F ? 1.0F : 1.0F + RUSH * charge;
        // Right in front of your eyes the haze would cover half the screen, so it stays fainter here. Gathering light
        // for the beam, each arm only lights up as the energy reaches it.
        float lit = charge < 0.0F ? (right ? 0.8F : 0.6F)
                : right ? (0.8F + 0.7F * ARM_SHINE) * armFill(charge) : 0.6F * restFill(charge);
        shell(pose, buffers.getBuffer(SHELL_TYPE), sleeve, armBox(right, slim), glow * lit);
        VertexConsumer light = buffers.getBuffer(Ring.HALO);
        if (right) {
            part(pose, light, sleeve, glow * 0.85F, time, rightArmMain(slim), MAIN, FAST * rush, armReach(charge));
            part(pose, light, sleeve, glow * 0.75F, time, rightArm(slim), 1.0F, FAST * rush, armReach(charge));
        } else {
            part(pose, light, sleeve, glow * 0.65F, time, leftArm(slim), 1.0F, 1.0F, restReach(charge));
        }
    }

    /** Where the core's light comes through on the back: a soft glow the back's lines run out of. */
    private static void node(VertexConsumer light, Matrix4f matrix, float glow, float time) {
        float z = 2.0F + SUIT + LIFT;
        float beat = 0.8F + 0.2F * Mth.sin(time * 0.35F);
        int sides = 14;
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides;
            float a1 = Mth.TWO_PI * (i + 1) / sides;
            fan(light, matrix, 0.0F, 3.4F, z, a0, a1, 0.0F, 2.4F * beat, GREEN, 0.7F * glow, 0.0F);
            fan(light, matrix, 0.0F, 3.4F, z, a0, a1, 0.0F, 0.7F, 0xE6FFEC, 0.7F * glow * beat, 0.2F * glow);
        }
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

    static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int rgb,
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
