package nl.tivek.welcomescreen.client.character.lantern;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.SwordMove;

/**
 * How Green Lantern moves with the sword and shield of the construct wheel (see {@link SwordMove}): every move as a
 * handful of key poses on its own ticks, and smooth curves through them, so a cut winds up, whips through and follows
 * through. A new move starts from wherever the last one left the arms and runs on into its own first pose; once a move
 * is over the arms settle back into the guard.
 *
 * <p>A pose says where each arm points and where the sword and the shield point, seen from his upper body: x to his
 * right, y up, z ahead; a yaw turns to his right, a pitch up. The upper body itself can turn on the hips (twist),
 * crouch and step forward into a lunge. Both the body seen from outside and your own arms in first person are posed
 * from the very same numbers.
 */
final class SwordPoses {
    /**
     * One pose, in radians: where the sword arm points (yaw, pitch; pitch -90 degrees hangs straight down), where the
     * blade points and how far it is rolled about itself (roll 0: its edges up and down), the same for the shield arm and
     * the face of the shield, how far the upper body turns to his right, how far he crouches (0 to 1) and how far he has
     * stepped forward (0 to 1).
     */
    record Pose(float armYaw, float armPitch, float bladeYaw, float bladePitch, float bladeRoll, float shieldArmYaw,
            float shieldArmPitch, float shieldYaw, float shieldPitch, float shieldRoll, float twist, float crouch,
            float step) {
        Pose mix(Pose to, float t) {
            return new Pose(Mth.lerp(t, this.armYaw, to.armYaw), Mth.lerp(t, this.armPitch, to.armPitch),
                    Mth.lerp(t, this.bladeYaw, to.bladeYaw), Mth.lerp(t, this.bladePitch, to.bladePitch),
                    Mth.lerp(t, this.bladeRoll, to.bladeRoll), Mth.lerp(t, this.shieldArmYaw, to.shieldArmYaw),
                    Mth.lerp(t, this.shieldArmPitch, to.shieldArmPitch), Mth.lerp(t, this.shieldYaw, to.shieldYaw),
                    Mth.lerp(t, this.shieldPitch, to.shieldPitch), Mth.lerp(t, this.shieldRoll, to.shieldRoll),
                    Mth.lerp(t, this.twist, to.twist), Mth.lerp(t, this.crouch, to.crouch),
                    Mth.lerp(t, this.step, to.step));
        }

        /** The same pose with its twist brought back to within half a turn: after a spin, a whole turn is no turn. */
        Pose unwound() {
            float twist = Mth.wrapDegrees(this.twist * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            return new Pose(this.armYaw, this.armPitch, this.bladeYaw, this.bladePitch, this.bladeRoll,
                    this.shieldArmYaw, this.shieldArmPitch, this.shieldYaw, this.shieldPitch, this.shieldRoll, twist,
                    this.crouch, this.step);
        }

        float[] numbers() {
            return new float[] { this.armYaw, this.armPitch, this.bladeYaw, this.bladePitch, this.bladeRoll,
                    this.shieldArmYaw, this.shieldArmPitch, this.shieldYaw, this.shieldPitch, this.shieldRoll,
                    this.twist, this.crouch, this.step };
        }

        static Pose of(float[] n) {
            return new Pose(n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], n[8], n[9], n[10], n[11], n[12]);
        }
    }

    /** One key pose of a move, on its own tick. */
    private record Key(float tick, Pose pose) {
    }

    /** The guard he stands in: sword forward and a little up in front of his right hip, shield before his left side. */
    static final Pose GUARD = deg(12, -52, -6, 28, 0, 14, -36, -6, 2, 0, 0, 0, 0);
    // How long the arms take to settle back into the guard once a move is over, in ticks.
    private static final float SETTLE = 6.0F;
    // The shield held before the chest in the flurry, and the sword pulled back between two stabs.
    private static final Pose FLURRY_GUARD = deg(18, -34, 4, 0, 90, 36, -14, 6, 4, 0, 0, 0, 0);
    // How far a stab of the flurry reaches out, as a part of the way from pulled back to the full thrust.
    private static final float STAB_OUT = 1.5F;

    private static final Map<SwordMove, Key[]> MOVES = new EnumMap<>(SwordMove.class);

    static {
        // ---- The sword: twelve cuts and thrusts. Numbers: arm yaw, arm pitch, blade yaw, pitch, roll, and twist. ----
        sword(SwordMove.SLASH, 3, 68, 8, 104, 12, 90, 18, 5, 30, 6, 45, 6, 90, 8, 6, 0, 5, 0, 4, 90, 0, 7, -40, 3, -62,
                2, 90, -12, 9, -68, -12, -104, -8, 90, -22, 12, -22, -42, -34, 14, 45, -8);
        sword(SwordMove.BACKHAND, 3, -58, 10, -98, 14, -90, -22, 5, -24, 7, -40, 8, -90, -8, 6, 4, 5, 6, 5, -90, 2, 7,
                38, 3, 62, 2, -90, 12, 9, 66, -10, 100, -6, -90, 22, 12, 24, -40, 30, 14, -45, 6);
        sword(SwordMove.CLEAVE, 4, 40, 70, 55, 115, 20, 16, 6, 18, 30, 25, 45, 20, 6, 7, -6, -8, -10, -20, 20, -4, 8,
                -26, -34, -38, -52, 20, -12, 10, -40, -52, -55, -70, 20, -16, 13, -10, -48, -12, 5, 10, -5);
        sword(SwordMove.REVERSE_CLEAVE, 4, -34, 72, -50, 115, -20, -18, 6, -14, 30, -20, 45, -20, -6, 7, 6, -8, 10,
                -20, -20, 4, 8, 26, -34, 38, -52, -20, 12, 10, 40, -52, 55, -70, -20, 16, 13, 16, -48, 8, 8, -10, 4);
        sword(SwordMove.RISING, 3, -38, -62, -58, -42, -30, -18, 5, -12, -22, -18, -2, -30, -6, 6, 8, 8, 12, 24, -30,
                2, 7, 26, 30, 36, 50, -30, 10, 9, 40, 52, 52, 78, -30, 16, 12, 20, -20, 10, 30, -10, 5);
        sword(SwordMove.UPPERCUT, 4, 4, -78, 0, -62, 0, 0, 6, 2, -8, 0, 28, 0, 0, 7, 0, 38, 0, 72, 0, 0, 9, -2, 62, 0,
                100, 0, 0, 13, 8, -30, -4, 30, 0, 0);
        sword(SwordMove.OVERHEAD, 5, 4, 118, 0, 150, 0, 0, 7, 3, 92, 0, 110, 0, 0, 8, 2, 45, 0, 40, 0, 0, 9, 0, -2, 0,
                -14, 0, 0, 11, -2, -42, 0, -58, 0, 0, 15, 8, -50, -4, 20, 0, 0);
        sword(SwordMove.STAB, 3, 18, -34, 2, 2, 90, 10, 4, 8, -18, 0, 0, 90, 4, 5, 0, -3, 0, -2, 90, -10, 7, 0, -5, 0,
                -3, 90, -10, 10, 12, -46, -4, 22, 30, 0);
        stepping(SwordMove.STAB, 0.0F, 0.1F, 0.35F, 0.35F, 0.0F);
        sword(SwordMove.LUNGE, 5, 22, -44, 4, 0, 90, 14, 7, 8, -14, 0, -1, 90, -6, 8, 0, -2, 0, -3, 90, -14, 12, 0, -4,
                0, -4, 90, -14, 16, 12, -50, -4, 24, 30, 0);
        stepping(SwordMove.LUNGE, 0.0F, 0.8F, 1.0F, 1.0F, 0.0F);
        sword(SwordMove.LOW_SWEEP, 4, 66, -48, 100, -22, 90, 20, 6, 20, -54, 30, -26, 90, 6, 7, -12, -56, -18, -26,
                90, -6, 8, -40, -56, -60, -24, 90, -14, 10, -66, -52, -100, -20, 90, -22, 13, -18, -50, -24, 16, 40, -6);
        crouching(SwordMove.LOW_SWEEP, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F);
        sword(SwordMove.SPIN, 4, 62, 2, 92, 2, 90, 26, 6, 70, 2, 96, 2, 90, 0, 9, 70, 2, 96, 2, 90, -150, 12, 70, 2, 96,
                2, 90, -300, 14, 60, -6, 86, -2, 90, -360, 18, 14, -50, -4, 26, 0, -360);
        sword(SwordMove.CROSS, 2, 44, 62, 58, 96, 25, 14, 4, 8, 10, 10, 20, 25, 2, 5, -22, -26, -32, -40, 25, -8, 7,
                -40, 62, -56, 96, -25, -14, 9, -8, 10, -10, 20, -25, -2, 10, 22, -26, 32, -40, -25, 8, 12, 34, -44,
                46, -58, -25, 12, 16, 12, -50, -4, 24, 0, 0);
        // ---- The shield: six bashes. Numbers: shield arm yaw, pitch, shield yaw, pitch, twist and step. ----
        shield(SwordMove.BASH, 3, 28, -48, 12, -4, -16, 0, 5, 8, -4, 4, 2, 18, 0.4F, 7, 8, -6, 4, 2, 16, 0.4F, 11, 14,
                -36, -6, 2, 0, 0);
        shield(SwordMove.BASH_SWEEP, 3, -58, -12, -70, 0, -20, 0, 5, -12, -8, -18, 0, -4, 0, 6, 18, -8, 30, 0, 8, 0, 8,
                44, -12, 60, 0, 18, 0, 12, 14, -36, -6, 2, 0, 0);
        shield(SwordMove.BASH_BACKHAND, 3, 52, -22, 62, 0, 20, 0, 5, 18, -10, 20, 0, 6, 0, 6, -12, -8, -18, 0, -6, 0,
                8, -52, -12, -66, 0, -20, 0, 12, 14, -36, -6, 2, 0, 0);
        shield(SwordMove.BASH_UP, 3, 12, -74, 4, -42, 0, 0, 5, 10, -20, 4, 18, 0, 0, 6, 8, 30, 4, 52, 0, 0, 8, 8, 48, 4,
                66, 0, 0, 12, 14, -36, -6, 2, 0, 0);
        shield(SwordMove.BASH_DOWN, 4, 12, 76, 4, 62, 0, 0, 6, 10, 30, 4, 10, 0, 0, 7, 8, -30, 4, -62, 0, 0, 9, 8, -44,
                4, -74, 0, 0, 13, 14, -36, -6, 2, 0, 0);
        shield(SwordMove.BASH_SPIN, 3, -56, -10, -84, 0, 24, 0, 6, -62, -8, -90, 0, -60, 0, 8, -62, -8, -90, 0, -180, 0,
                11, -62, -8, -90, 0, -300, 0, 12, -50, -14, -76, 0, -336, 0, 15, 14, -36, -6, 2, -360, 0);
        // ---- The end of a charge: the shield raised and slammed into the ground before him. ----
        shield(SwordMove.SLAM, 4, 12, 74, 4, 58, -8, 0, 6, 10, 20, 4, -10, -4, 0, 7, 8, -58, 4, -84, 0, 0, 10, 8, -60, 4,
                -86, 0, 0, 14, 14, -36, -6, 2, 0, 0);
        crouching(SwordMove.SLAM, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F);
        // ---- Taking shape: the sword grows up out of the fist, is flicked up and caught, and knocks on the shield. ----
        MOVES.put(SwordMove.EQUIP, new Key[] { key(0, deg(10, -24, 0, 82, 0, 16, -30, -4, 2, 0, 0, 0, 0)),
                key(7, deg(12, -30, 0, 84, 0, 16, -32, -4, 2, 0, 0, 0, 0)),
                key(9, deg(12, -44, 0, 70, 0, 16, -32, -4, 2, 0, 0, 0, 0)),
                key(10, deg(10, 28, 0, 95, 0, 16, -30, -4, 2, 0, 0, 0, 0)),
                key(13, deg(8, 18, 0, 90, 0, 18, -28, -6, 2, 0, 0, 0, 0)),
                key(19, deg(8, 22, 0, 60, 0, 18, -28, -6, 2, 0, 0, 0, 0)),
                key(20, deg(8, 12, 0, 40, 0, 18, -28, -6, 2, 0, 0, 0, 0)),
                key(22, deg(10, -12, 0, 20, 0, 18, -24, -10, 0, 0, -4, 0, 0)),
                key(24, deg(-34, -18, -62, 4, 0, 18, -22, -14, 0, 0, -10, 0, 0)),
                key(26, deg(-12, -22, -30, 10, 0, 18, -22, -14, 0, 0, -4, 0, 0)),
                key(28, deg(-36, -18, -64, 4, 0, 18, -22, -14, 0, 0, -10, 0, 0)),
                key(32, GUARD) });
    }

    private SwordPoses() {
    }

    /** A pose from its thirteen numbers, the angles given in degrees. */
    private static Pose deg(float armYaw, float armPitch, float bladeYaw, float bladePitch, float bladeRoll,
            float shieldArmYaw, float shieldArmPitch, float shieldYaw, float shieldPitch, float shieldRoll, float twist,
            float crouch, float step) {
        float r = Mth.DEG_TO_RAD;
        return new Pose(armYaw * r, armPitch * r, bladeYaw * r, bladePitch * r, bladeRoll * r, shieldArmYaw * r,
                shieldArmPitch * r, shieldYaw * r, shieldPitch * r, shieldRoll * r, twist * r, crouch, step);
    }

    private static Key key(float tick, Pose pose) {
        return new Key(tick, pose);
    }

    /**
     * A move of the sword arm, as keys of seven numbers: the tick, where the arm points (yaw, pitch), where the blade
     * points and its roll, and how far the upper body turns. The shield stays in the guard.
     */
    private static void sword(SwordMove move, float... n) {
        Key[] keys = new Key[n.length / 7];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 7;
            keys[k] = key(n[i], deg(n[i + 1], n[i + 2], n[i + 3], n[i + 4], n[i + 5], 14, -36, -6, 2, 0, n[i + 6], 0, 0));
        }
        MOVES.put(move, keys);
    }

    /**
     * A move of the shield arm, as keys of seven numbers: the tick, where the shield arm points (yaw, pitch), where the
     * face of the shield points (yaw, pitch), how far the upper body turns and how far he steps forward. The sword is held
     * back low out of the way.
     */
    private static void shield(SwordMove move, float... n) {
        Key[] keys = new Key[n.length / 7];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 7;
            keys[k] = key(n[i], deg(22, -62, 8, 12, 0, n[i + 1], n[i + 2], n[i + 3], n[i + 4], 0, n[i + 5], 0,
                    n[i + 6]));
        }
        MOVES.put(move, keys);
    }

    /** Sets how far he has stepped forward on every key of a move, in order. */
    private static void stepping(SwordMove move, float... steps) {
        Key[] keys = MOVES.get(move);
        for (int k = 0; k < keys.length && k < steps.length; k++) {
            float[] n = keys[k].pose().numbers();
            n[12] = steps[k];
            keys[k] = key(keys[k].tick(), Pose.of(n));
        }
    }

    /** Sets how far he crouches on every key of a move, in order. */
    private static void crouching(SwordMove move, float... crouches) {
        Key[] keys = MOVES.get(move);
        for (int k = 0; k < keys.length && k < crouches.length; k++) {
            float[] n = keys[k].pose().numbers();
            n[11] = crouches[k];
            keys[k] = key(keys[k].tick(), Pose.of(n));
        }
    }

    /** How long a new move takes to get from wherever the arms were into its own first pose, in ticks. */
    static float blendIn(SwordMove move) {
        return switch (move) {
            case FLURRY -> 3.0F;
            case CHARGE -> 4.0F;
            default -> {
                Key[] keys = MOVES.get(move);
                yield keys == null ? 3.0F : Math.max(1.5F, keys[0].tick());
            }
        };
    }

    /**
     * The pose {@code t} ticks into a move: through its keys, settling back into the guard once it is over. The first
     * moment of a move is left to {@link #blendIn}: it comes in from wherever the arms were.
     *
     * @param time ticks of the client's own clock, for everything that sways
     */
    static Pose at(SwordMove move, float t, float time) {
        return switch (move) {
            case FLURRY -> flurry(t);
            case CHARGE -> charge(time);
            default -> keyed(MOVES.get(move), move.ticks(), t);
        };
    }

    private static Pose keyed(@Nullable Key[] keys, float ticks, float t) {
        if (keys == null || keys.length == 0) {
            return GUARD;
        }
        Key last = keys[keys.length - 1];
        if (t >= last.tick()) {
            return last.pose().unwound().mix(GUARD, smooth((t - Math.max(last.tick(), ticks)) / SETTLE));
        }
        if (t <= keys[0].tick()) {
            return keys[0].pose();
        }
        int i = 0;
        while (i + 1 < keys.length && keys[i + 1].tick() < t) {
            i++;
        }
        Key a = keys[i];
        Key b = keys[i + 1];
        Key before = i > 0 ? keys[i - 1] : a;
        Key after = i + 2 < keys.length ? keys[i + 2] : b;
        float span = b.tick() - a.tick();
        float u = (t - a.tick()) / span;
        float[] p0 = a.pose().numbers();
        float[] p1 = b.pose().numbers();
        float[] pb = before.pose().numbers();
        float[] pa = after.pose().numbers();
        float[] out = new float[p0.length];
        float h00 = 2 * u * u * u - 3 * u * u + 1;
        float h10 = u * u * u - 2 * u * u + u;
        float h01 = -2 * u * u * u + 3 * u * u;
        float h11 = u * u * u - u * u;
        // A curve through the keys: at every key it keeps going the way the keys on either side of it go.
        float spanBefore = Math.max(1.0E-3F, b.tick() - before.tick());
        float spanAfter = Math.max(1.0E-3F, after.tick() - a.tick());
        for (int k = 0; k < out.length; k++) {
            float m0 = i > 0 ? (p1[k] - pb[k]) / spanBefore * span : 0.0F;
            float m1 = i + 2 < keys.length ? (pa[k] - p0[k]) / spanAfter * span : 0.0F;
            out[k] = h00 * p0[k] + h10 * m0 + h01 * p1[k] + h11 * m1;
        }
        return Pose.of(out);
    }

    /**
     * The flurry: the shield comes up before his chest, and the sword stabs out twelve times all over the front, pulled
     * back between two stabs, each along its own way (see {@link SwordMove#stab}).
     */
    private static Pose flurry(float t) {
        float first = SwordMove.FIRST_STAB;
        float every = SwordMove.STAB_EVERY;
        int k = Mth.clamp(Math.round((t - first) / every), 0, SwordMove.STABS - 1);
        float u = (t - (first + k * every)) / STAB_OUT;
        float out = t < first - STAB_OUT || t > first + (SwordMove.STABS - 1) * every + STAB_OUT ? 0.0F
                : Math.max(0.0F, 1.0F - u * u);
        double[] way = SwordMove.stab(k);
        float side = (float) way[0];
        float up = (float) way[1];
        Pose stab = deg(side, up - 6, side, up, 90, 36, -14, 6, 4, 0, -10, 0, 0.15F);
        Pose pose = FLURRY_GUARD.mix(stab, out);
        return t > SwordMove.FLURRY.ticks() ? pose.mix(GUARD, smooth((t - SwordMove.FLURRY.ticks()) / SETTLE)) : pose;
    }

    /** Bent forward behind the shield locked before him, the sword held back low, running. */
    private static Pose charge(float time) {
        float bob = Mth.sin(time * 1.4F);
        return deg(24, -76, 14, -34, 90, 34, -6 + 2 * bob, 4, 8 + 3 * bob, 0, -16, 1, 0);
    }

    /**
     * Where the sword is while it is tossed up in the air while it takes shape, or null while it is in his hand: how far
     * it has come on its way up and back down (0 to 1) and how far it has spun.
     */
    @Nullable
    static float[] toss(SwordMove move, float t) {
        if (move != SwordMove.EQUIP || t <= 10.0F || t >= 20.0F) {
            return null;
        }
        float u = (t - 10.0F) / 10.0F;
        return new float[] { u, u * Mth.TWO_PI * 2.0F };
    }

    /** How far the sword has grown out of the ring's light while they take shape, 0 to 1 (1 for every other move). */
    static float swordGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? smooth(t / 7.0F) : 1.0F;
    }

    /** How far the shield has grown out of the ring's light while they take shape, 0 to 1. */
    static float shieldGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? smooth((t - 1.0F) / 7.0F) : 1.0F;
    }

    // ---- The pose as ways in space ----

    /** A way given as a yaw (to his right) and a pitch (up), one long: x to his right, y up, z ahead. */
    static Vec3 way(float yaw, float pitch) {
        return new Vec3(Mth.sin(yaw) * Mth.cos(pitch), Mth.sin(pitch), Mth.cos(yaw) * Mth.cos(pitch));
    }

    /**
     * The blade (or the shield) as two ways, seen from his upper body: where it points, and where its edge (the top of
     * the shield) faces, turned {@code roll} about it from facing up.
     */
    static Vec3[] frame(float yaw, float pitch, float roll) {
        Vec3 forward = way(yaw, pitch);
        Vec3 reference = Math.abs(forward.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : way(yaw, 0.0F).scale(-Math.signum(
                forward.y));
        Vec3 up = reference.subtract(forward.scale(forward.dot(reference))).normalize();
        Vec3 side = forward.cross(up);
        Vec3 edge = up.scale(Mth.cos(roll)).add(side.scale(Mth.sin(roll)));
        return new Vec3[] { forward, edge };
    }

    /** 0 below 0, 1 above 1, and a smooth S-curve in between. */
    static float smooth(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }
}
