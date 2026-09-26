package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipLash;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Keyframes;

abstract class WhipKeys extends WhipCurves {
    static final float SETTLE = 7.0F;
    static final float BLEND_IN = 4.0F;
    static final float IDLE_IN = 10.0F;
    private static final float FLICK_EVERY = 110.0F;
    private static final float FLICK_TICKS = 14.0F;

    // The handle held up and forward across the body, the lash drooping from its end.
    static final Pose GUARD = pose(0.38, -0.56, -0.8, -0.24, 0.42, -0.88, -0.5, -1.25, -0.55, 0.0F, 12.0F, 0.04F,
            0.12F);
    static final Pose REST = rest(pose(RechargeAnimation.HAND_RIGHT.x(), RechargeAnimation.HAND_RIGHT.y(),
            RechargeAnimation.HAND_RIGHT.z(), 0.05, -1.0, 0.1, -0.46, -1.3, -0.84, 0.0F, 0.0F, 0.0F, 0.0F));
    // The guard in a loose stance: feet apart, knees soft, the lash on the ground beside the right foot.
    static final Pose READY = stance(GUARD, 0.06F, 0.28F);
    private static final Pose WHIRLING = stance(pose(0.28, 0.08, -0.4, 0.2, 0.9, -0.4, -0.72, -0.5, -0.3, 0.85F, 5.0F,
            -0.05F, 0.1F), 0.15F, 0.5F);
    private static final Pose SPINNING = stance(pose(0.22, -0.3, -0.8, 0.0, 0.0, -1.0, -0.5, -1.25, -0.55, 0.0F, 4.0F,
            0.1F, 0.3F), 0.2F, 0.45F);

    static final Map<WhipMove, Keyframes.Key[]> MOVES = new EnumMap<>(WhipMove.class);

    static {
        equip();
        WhipAttacks.fill(MOVES);
        lasso();
        releases();
    }

    private static WhipAttacks.Beat beat(float tick) {
        return WhipAttacks.beat(tick);
    }

    // The handle grows in the fist and tips down while the lash pours out and coils on the ground; a flick lifts it,
    // it twirls overhead, and is thrown forward over the top for the crack.
    private static void equip() {
        MOVES.put(WhipMove.EQUIP, WhipAttacks.keys(WhipMove.EQUIP,
                beat(3).grip(0.3, -0.45, -0.7).handle(0.2, 0.05, -0.98).twist(5).lean(0.0).step(0.0).squat(0.02),
                beat(WhipMove.FORMED).stop().grip(0.28, -0.42, -0.7).handle(0.2, 0.0, -0.98).twist(6).lean(0.0)
                        .step(0.0).squat(0.02),
                beat(11).grip(0.34, -0.4, -0.72).handle(0.15, -0.7, -0.7).twist(8).lean(0.04).step(0.05),
                beat(WhipMove.POURED).stop().grip(0.36, -0.42, -0.74).handle(0.12, -0.78, -0.62).twist(10).lean(0.08)
                        .step(0.08),
                beat(23.5F).grip(0.37, -0.5, -0.72).handle(0.1, -0.85, -0.52).twist(10).lean(0.1).step(0.08)
                        .squat(0.1),
                beat(26).grip(0.4, -0.28, -0.66).twist(14).lean(0.0).step(0.1).squat(0.06),
                beat(29).grip(0.36, -0.05, -0.52).twist(12).lean(-0.05).step(0.1).squat(0.08),
                beat(WhipMove.TWIRL).grip(0.3, 0.08, -0.42).twist(8).lean(-0.1).step(0.1).squat(0.1).wide(0.4),
                beat(34).grip(0.26, 0.1, -0.46).twist(2).lean(-0.1).squat(0.1).wide(0.4),
                beat(37).grip(0.32, 0.12, -0.38).twist(-4).lean(-0.12).squat(0.12).wide(0.4),
                beat(40).grip(0.36, 0.14, -0.36).twist(14).lean(-0.2).step(-0.2).squat(0.12).wide(0.4),
                beat(42.5F).grip(0.3, 0.04, -0.62).twist(8).lean(0.0).step(0.2).squat(0.15).wide(0.45),
                beat(44).stop().grip(0.22, -0.38, -1.02).twist(-4).lean(0.3).step(0.55).squat(0.2).wide(0.45),
                beat(47).stop().grip(0.24, -0.46, -0.96).twist(-4).lean(0.3).step(0.5).squat(0.2).wide(0.4),
                beat(52).grip(0.34, -0.52, -0.84).twist(6).lean(0.1).step(0.2).squat(0.1),
                beat(58).stop().grip(0.38, -0.56, -0.8).twist(12).lean(0.04).step(0.12).squat(0.06).wide(0.28)));
    }

    // Thrown up and over the shoulder at the creature; the left hand takes hold of the lash and both haul it in.
    private static void lasso() {
        MOVES.put(WhipMove.LASSO, WhipAttacks.keys(WhipMove.LASSO,
                beat(2).grip(0.42, -0.15, -0.55).twist(10).lean(-0.05),
                beat(4).stop().grip(0.36, 0.08, -0.4).twist(15).lean(-0.15).step(-0.2),
                beat(6.5F).grip(0.3, -0.1, -0.75).twist(8).lean(0.05).step(0.25),
                beat(8).stop().grip(0.24, -0.36, -1.02).twist(0).lean(0.25).step(0.5).squat(0.12),
                beat(11).stop().grip(0.26, -0.38, -0.98).twist(2).lean(0.22).step(0.5).squat(0.12),
                beat(14).grip(0.28, -0.4, -0.92).left(0.05, -0.52, -0.8, 0.8).twist(4).lean(0.18).step(0.45)
                        .squat(0.14),
                beat(WhipMove.LASSO_GRAB).stop().grip(0.28, -0.4, -0.9).left(0.1, -0.42, -0.98, 1.0).twist(4)
                        .lean(0.15).step(0.4).squat(0.15),
                beat(17).grip(0.48, -0.42, -0.55).left(0.2, -0.42, -0.72, 1.0).twist(25).lean(-0.25).step(-0.35)
                        .squat(0.25).wide(0.45),
                beat(21).grip(0.52, -0.45, -0.45).left(0.22, -0.45, -0.62, 1.0).twist(30).lean(-0.32).step(-0.45)
                        .squat(0.3).wide(0.5),
                beat(WhipMove.LASSO_LAND).stop().grip(0.46, -0.5, -0.55).left(0.15, -0.5, -0.7, 1.0).twist(24)
                        .lean(-0.2).step(-0.35).squat(0.35).wide(0.5),
                beat(26).grip(0.4, -0.55, -0.7).left(-0.3, -0.9, -0.6, 0.4).twist(16).lean(0.0).step(-0.1)
                        .squat(0.15).wide(0.4),
                beat(30).grip(0.38, -0.56, -0.8).twist(12).step(0.1).squat(0.08)));
    }

    // Letting go of a whirl throws the spinning lash forward over the top; a spin just runs down.
    private static void releases() {
        MOVES.put(WhipMove.WHIRL_CRACK, WhipAttacks.keys(WhipMove.WHIRL_CRACK,
                beat(4).grip(0.32, 0.12, -0.42).left(-0.72, -0.5, -0.3, 0.85).twist(6).lean(-0.08).squat(0.15)
                        .wide(0.5),
                beat(8).grip(0.36, 0.14, -0.36).left(-0.6, -0.7, -0.4, 0.5).twist(14).lean(-0.2).step(-0.2)
                        .squat(0.15).wide(0.5),
                beat(10).grip(0.3, 0.04, -0.62).twist(8).lean(0.0).step(0.2).squat(0.18).wide(0.5),
                beat(12).stop().grip(0.22, -0.38, -1.02).twist(-4).lean(0.3).step(0.55).squat(0.22).wide(0.5),
                beat(15).stop().grip(0.24, -0.46, -0.96).twist(-4).lean(0.3).step(0.5).squat(0.2).wide(0.45),
                beat(19).grip(0.34, -0.52, -0.84).twist(6).lean(0.1).step(0.2).squat(0.1)));
        MOVES.put(WhipMove.SPIN_END, WhipAttacks.keys(WhipMove.SPIN_END,
                beat(4).grip(0.25, -0.36, -0.8).twist(6).lean(0.08).step(0.25).squat(0.15).wide(0.4),
                beat(8).grip(0.34, -0.48, -0.82).twist(10).lean(0.05).step(0.15).squat(0.1).wide(0.32),
                beat(12).stop().grip(0.38, -0.56, -0.8).twist(12).lean(0.04).step(0.12).squat(0.06).wide(0.28)));
    }

    private static Pose rest(Pose pose) {
        float[] n = pose.numbers();
        n[Pose.REST] = 1.0F;
        n[Pose.BODY + WhipLash.REACH] = 0.0F;
        return Pose.of(n);
    }

    private static Pose stance(Pose pose, float squat, float wide) {
        float[] n = pose.numbers();
        n[Pose.SQUAT] = squat;
        n[Pose.WIDE] = wide;
        return Pose.of(n);
    }

    // Never quite still: it breathes, the weight drifts, and now and then the wrist gives the lash a lazy flick that
    // runs down it to the tip.
    static Pose idle(float time, float amount) {
        if (amount <= 0.0F) {
            return READY;
        }
        float[] n = READY.numbers();
        float breath = Mth.sin(time * 0.13F);
        float sway = Mth.sin(time * 0.061F + 1.3F);
        float drift = Mth.sin(time * 0.037F + 0.4F);
        float weigh = Mth.sin(time * 0.029F + 2.1F);
        float flick = flick(time);
        n[0] += amount * 0.008F * sway;
        n[1] += amount * (0.012F * breath + 0.03F * flick);
        n[2] += amount * 0.006F * drift;
        n[3] += amount * 0.03F * sway;
        n[4] += amount * (0.02F * breath + 0.22F * flick);
        n[Pose.TWIST] += amount * 0.05F * sway;
        n[Pose.LEAN] += amount * 0.02F * breath;
        n[Pose.ROLL] += amount * 0.015F * weigh;
        n[Pose.SQUAT] += amount * 0.03F * (0.5F + 0.5F * breath);
        n[Pose.STEP] += amount * 0.08F * weigh;
        return Pose.of(n);
    }

    // The resting lash hangs to the ground and sways a little; the flick runs down it to the tip.
    static float[] idleLash(float time, float amount) {
        float[] lash = WhipMove.REST.clone();
        if (amount <= 0.0F) {
            return lash;
        }
        lash[WhipLash.YAW] += amount * 0.25F * Mth.sin(time * 0.061F + 1.3F);
        lash[WhipLash.WAVE] = amount * 0.5F * flick(time);
        return lash;
    }

    private static float flick(float time) {
        return (float) Ease.bump((time % FLICK_EVERY) / FLICK_TICKS * 2.0F - 1.0F);
    }

    // Arm up, the wrist turning small circles overhead in time with the lash going round.
    static Pose whirl(float t, float[] lash) {
        float[] n = WHIRLING.numbers();
        float yaw = lash[WhipLash.YAW];
        float up = (float) Ease.smooth(t / WhipMove.WHIRL_UP);
        n[0] += 0.05F * up * Mth.sin(yaw);
        n[2] -= 0.05F * up * Mth.cos(yaw);
        Vec3 way = aimed(yaw, Math.toRadians(25.0));
        n[3] = (float) way.x;
        n[4] = (float) way.y;
        n[5] = (float) way.z;
        n[Pose.TWIST] += 0.18F * up * Mth.sin(yaw);
        return Pose.of(n).lashed(lash);
    }

    // Arm out before the chest, the wrist circling with the lash like the hub of a propeller.
    static Pose spin(float[] lash) {
        float[] n = SPINNING.numbers();
        Vec3 way = aimed(lash[WhipLash.YAW], lash[WhipLash.PITCH]);
        n[0] += (float) (0.035 * way.x);
        n[1] += (float) (0.035 * way.y);
        Vec3 handle = new Vec3(0.0, 0.0, -1.0).lerp(way, 0.55).normalize();
        n[3] = (float) handle.x;
        n[4] = (float) handle.y;
        n[5] = (float) handle.z;
        return Pose.of(n).lashed(lash);
    }
}
