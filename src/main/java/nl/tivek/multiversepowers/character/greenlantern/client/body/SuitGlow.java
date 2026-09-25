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

public final class SuitGlow {
    static final int GREEN = 0x3CE86A;
    static final int BRIGHT = 0x9CFFB4;
    private static final float RISE = 10.0F;
    private static final float FALL = 2.5F;
    private static final float SURGE_TICKS = 14.0F;
    static final float SUIT = 0.3F;
    static final float LIFT = 0.06F;
    private static final float SHELL = SUIT + 0.12F;
    private static final float HAZE = SUIT + 1.1F;
    private static final float SHELL_LIGHT = 0.3F;
    private static final float HAZE_LIGHT = 0.16F;
    private static final float MAIN = 1.4F;
    private static final float FAST = 1.7F;
    private static final float ARM_FULL = 16.0F;
    private static final float SUIT_FULL = 22.0F;
    private static final float ARM_AT = 0.55F;
    private static final float SUIT_FROM = 0.3F;
    static final float FRONT = 1.6F;
    private static final float RUSH = 1.5F;
    private static final float ARM_SHINE = 1.0F;
    static final float ALL = Float.MAX_VALUE;
    private static final RenderType SHELL_TYPE = RenderType.create("welcomescreen_suit_shell",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.CULL)
                    .createCompositeState(false));
    private static final float[] HEAD = { -4, -8, -4, 4, 0, 4 };
    private static final float[] TORSO = { -4, 0, -2, 4, 12, 2 };
    private static final float[] LEG_BOX = { -2, 0, -2, 2, 12, 2 };

    private static final Map<Integer, Level> LEVELS = new HashMap<>();

    private SuitGlow() {
    }

    private static final class Level {
        float value;
        long last = Util.getMillis();
    }

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
            float arise = flight < ClientFlight.ARISE ? 1.0F - flight / ClientFlight.ARISE * 0.25F : 0.75F;
            want = Math.max(want, arise);
        }
        if (ClientRing.has(player, RingPayload.SHIELD)) {
            want = Math.max(want, 0.55F);
        }
        if (ClientRing.has(player, RingPayload.DIVE)) {
            want = Math.max(want, 0.9F);
        }
        float dressed = ClientRing.arrival(player, partialTick) - Arrival.DRESSED;
        if (dressed >= 0.0F && dressed < SURGE_TICKS) {
            want = Math.max(want, 1.0F - dressed / SURGE_TICKS);
        }
        float recharge = ClientRing.recharge(player, partialTick);
        if (recharge >= 0.0F) {
            want = Math.max(want, Mth.clamp(RechargeAnimation.glow(recharge) * 0.6F, 0.0F, 1.0F));
        }
        want = Math.max(want, ClientConstructs.working(player.getId(), partialTick));
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

    public static void clear() {
        LEVELS.clear();
    }

    static void body(PoseStack pose, MultiBufferSource buffers, PlayerModel<AbstractClientPlayer> suit, boolean slim,
            float glow, float time, boolean kneeling, float charge) {
        if (glow <= 0.01F) {
            return;
        }
        float arm = armReach(charge);
        float rest = restReach(charge);
        float rush = charge < 0.0F ? 1.0F : 1.0F + RUSH * charge;
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

    private static float armReach(float charge) {
        return charge < 0.0F ? ALL : ARM_FULL * armFill(charge);
    }

    private static float restReach(float charge) {
        return charge < 0.0F ? ALL : SUIT_FULL * restFill(charge);
    }

    private static float restFill(float charge) {
        return charge < 0.0F ? 0.0F : Mth.clamp((charge - SUIT_FROM) / (1.0F - SUIT_FROM), 0.0F, 1.0F);
    }

    private static float armFill(float charge) {
        return charge < 0.0F ? 0.0F : Mth.clamp(charge / ARM_AT, 0.0F, 1.0F);
    }

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

    public static void arm(PoseStack pose, MultiBufferSource buffers, ModelPart sleeve, boolean right, boolean slim,
            float glow, float time, float charge) {
        if (glow <= 0.01F) {
            return;
        }
        float rush = charge < 0.0F ? 1.0F : 1.0F + RUSH * charge;
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

    private static float[] armBox(boolean right, boolean slim) {
        float narrow = slim ? 1.0F : 0.0F;
        return right ? new float[] { -3 + narrow, -2, -2, 1, 10, 2 } : new float[] { -1, -2, -2, 3 - narrow, 10, 2 };
    }

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

    private static void chest(VertexConsumer light, Matrix4f matrix, float glow, float time) {
        float z = -2.0F - SUIT - LIFT;
        float cx = -0.5F;
        float cy = 3.0F;
        float beat = 0.8F + 0.2F * Mth.sin(time * 0.35F);
        int sides = 18;
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides;
            float a1 = Mth.TWO_PI * (i + 1) / sides;
            fan(light, matrix, cx, cy, z, a0, a1, 0.0F, 5.5F * beat, GREEN, 0.85F * glow, 0.0F);
            fan(light, matrix, cx, cy, z, a0, a1, 0.0F, 1.1F, 0xE6FFEC, 0.8F * glow * beat, 0.3F * glow);
            ringBand(light, matrix, cx, cy, z, a0, a1, 1.0F, 1.6F, BRIGHT, glow * beat);
        }
        bar(light, matrix, cx, cy - 1.9F, z, 1.4F, 0.3F, glow * beat);
        bar(light, matrix, cx, cy + 1.9F, z, 1.4F, 0.3F, glow * beat);
    }

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

    private static void ringBand(VertexConsumer light, Matrix4f matrix, float cx, float cy, float z, float a0,
            float a1, float r0, float r1, int rgb, float alpha) {
        vertex(light, matrix, cx + Mth.cos(a0) * r0, cy + Mth.sin(a0) * r0, z, rgb, alpha);
        vertex(light, matrix, cx + Mth.cos(a0) * r1, cy + Mth.sin(a0) * r1, z, rgb, alpha);
        vertex(light, matrix, cx + Mth.cos(a1) * r1, cy + Mth.sin(a1) * r1, z, rgb, alpha);
        vertex(light, matrix, cx + Mth.cos(a1) * r0, cy + Mth.sin(a1) * r0, z, rgb, alpha);
    }

    private static void bar(VertexConsumer light, Matrix4f matrix, float cx, float cy, float z, float half,
            float thick, float alpha) {
        vertex(light, matrix, cx - half, cy - thick, z, BRIGHT, alpha);
        vertex(light, matrix, cx + half, cy - thick, z, BRIGHT, alpha);
        vertex(light, matrix, cx + half, cy + thick, z, BRIGHT, alpha);
        vertex(light, matrix, cx - half, cy + thick, z, BRIGHT, alpha);
    }
}
