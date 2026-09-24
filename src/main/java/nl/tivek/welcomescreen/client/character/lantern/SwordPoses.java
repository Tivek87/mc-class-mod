package nl.tivek.welcomescreen.client.character.lantern;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.SwordMove;

/**
 * How Green Lantern moves with the sword and shield of the construct wheel (see {@link SwordMove}).
 *
 * <p>Every move is made as it looks through his own eyes, the way a game with a sword in first person makes it: a
 * handful of key poses, each saying where the sword hand grips, where the blade points and which way its edge faces,
 * and where the shield hangs and faces. Between the keys everything runs along smooth curves through all of them
 * (never from key to key and stopping), so a cut winds up, whips through the moment it strikes and brakes after, and
 * the tip of the blade draws one clean arc; a key can also be a stop, where the move hangs a moment before it strikes. A
 * new move starts from wherever the last one left the arms, and once a move is over they settle back into the guard.
 *
 * <p>In the guard the sword stands upright in the right fist, the blade a little forward, and the shield hangs on the left
 * forearm, its back to you and its face turned out and forward. The body seen from outside is posed from the very same
 * poses (see {@link SwordArms}); for that each key also says how far the upper body turns into the move (twist), bends
 * forward (lean) and steps into it (step), and for the spinning cut how far the whole body has spun round (orbit).
 */
final class SwordPoses {
    /**
     * One pose, as your own eyes see it (x to the right, y up, -z ahead, in blocks): where the sword hand grips, where
     * the blade points and where its edge faces (one long, square to each other), where the middle of the shield is, where
     * its face points and where its top is; how far the upper body turns to the right (twist, radians), bends forward
     * (lean, 0 to 1, a little back below 0) and has stepped forward (step, 0 to 1); and how far the whole body has spun
     * round to the left (orbit, radians), which in first person takes everything round with it.
     */
    record Pose(Vec3 hand, Vec3 blade, Vec3 edge, Vec3 shield, Vec3 face, Vec3 top, float twist, float lean,
            float step, float orbit) {
        static final int SIZE = 22;
        // Where the shield's numbers are in numbers(): its middle, face and top.
        private static final int SHIELD_FROM = 9;
        private static final int SHIELD_TO = 17;

        float[] numbers() {
            return new float[] { (float) this.hand.x, (float) this.hand.y, (float) this.hand.z, (float) this.blade.x,
                    (float) this.blade.y, (float) this.blade.z, (float) this.edge.x, (float) this.edge.y,
                    (float) this.edge.z, (float) this.shield.x, (float) this.shield.y, (float) this.shield.z,
                    (float) this.face.x, (float) this.face.y, (float) this.face.z, (float) this.top.x, (float) this.top.y,
                    (float) this.top.z, this.twist, this.lean, this.step, this.orbit };
        }

        /** A pose from its numbers; the ways are made one long and square to each other again. */
        static Pose of(float[] n) {
            Vec3 blade = unit(new Vec3(n[3], n[4], n[5]), new Vec3(0.0, 1.0, 0.0));
            Vec3 edge = square(new Vec3(n[6], n[7], n[8]), blade);
            Vec3 face = unit(new Vec3(n[12], n[13], n[14]), new Vec3(0.0, 0.0, -1.0));
            Vec3 top = square(new Vec3(n[15], n[16], n[17]), face);
            return new Pose(new Vec3(n[0], n[1], n[2]), blade, edge, new Vec3(n[9], n[10], n[11]), face, top, n[18],
                    n[19], n[20], n[21]);
        }

        Pose mix(Pose to, float t) {
            float[] a = this.numbers();
            float[] b = to.numbers();
            for (int k = 0; k < a.length; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        /** The same pose with the shield of {@code other}, {@code t} of the way. */
        Pose shieldOf(Pose other, float t) {
            if (t <= 0.0F) {
                return this;
            }
            float[] a = this.numbers();
            float[] b = other.numbers();
            for (int k = SHIELD_FROM; k <= SHIELD_TO; k++) {
                a[k] = Mth.lerp(t, a[k], b[k]);
            }
            return of(a);
        }

        /** The same pose with its twist and its spin brought back to within half a turn: a whole turn is no turn. */
        Pose unwound() {
            return new Pose(this.hand, this.blade, this.edge, this.shield, this.face, this.top, wrap(this.twist),
                    this.lean, this.step, wrap(this.orbit));
        }

        /** The same pose turned {@code angle} (radians) to the left about the upright line through your eyes. */
        Pose turned(double angle) {
            if (angle == 0.0) {
                return this;
            }
            return new Pose(spin(this.hand, angle), spin(this.blade, angle), spin(this.edge, angle),
                    spin(this.shield, angle), spin(this.face, angle), spin(this.top, angle), this.twist, this.lean,
                    this.step, this.orbit);
        }

        /** The shield's own right: along it the forearm lies on its back, from the elbow to the fist. */
        Vec3 shieldRight() {
            return this.face.cross(this.top).normalize();
        }

        /** Where the fist grips the shield: the grip on its back, at a shield of scale {@code scale}. */
        Vec3 shieldGrip(double scale) {
            return this.shield.add(this.shieldRight().scale(SwordPainter.GRIP_X * scale))
                    .add(this.face.scale(SwordPainter.GRIP_Z * scale));
        }
    }

    /** One key pose of a move: on its own tick, and whether the move stops there a moment (a windup before a strike). */
    private record Key(float tick, boolean stop, float[] numbers) {
    }

    // How long the arms take to settle back into the guard once the last key of a move is past, and into the run once a
    // ram of the shield is past, in ticks.
    private static final float SETTLE = 7.0F;
    private static final float RAM_SETTLE = 4.0F;
    // How long a flurry or a charge takes to come in from wherever the arms were, in ticks.
    private static final float BLEND_IN = 3.0F;

    /**
     * The guard: the sword upright in the right fist before the right hip, its blade a little forward and its edge
     * turned so you see its flat, and the shield on the left forearm low before the left hip, its back to you.
     */
    static final Pose GUARD = pose(0.46, -0.50, -0.92, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0,
            -0.50, -0.52, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0);
    /**
     * The shield held up to block: the forearm across before the chest, the shield square to the front just below your
     * line of sight, so you look over it. Laid over the shield only, so the sword can still cut behind it.
     */
    private static final Pose BLOCK = pose(0.46, -0.50, -0.92, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0,
            -0.28, -0.44, -0.76, 0.14, 0.03, -1.0, 0.0, 1.0, 0.08, 0, 0.12F, 0, 0);
    // The flurry: the shield up before the chest, the sword pulled back beside it between two stabs, and how far a stab
    // reaches out, as a part of the way from pulled back to the full thrust.
    private static final Pose FLURRY_GUARD = pose(0.46, -0.36, -0.72, -0.12, 0.08, -0.99, 1.0, 0.0, 0.0,
            -0.30, -0.38, -0.80, 0.06, 0.05, -1.0, 0.0, 1.0, 0.05, -6, 0.2F, 0.15F, 0);
    private static final float STAB_OUT = 1.5F;

    private static final Map<SwordMove, Key[]> MOVES = new EnumMap<>(SwordMove.class);

    static {
        // ---- The sword: twelve cuts and thrusts, the shield kept in its guard. Numbers per key: the tick, whether it
        // stops there (1), where the fist is (x, y, z), where the blade points, where its edge faces, the twist of the
        // upper body (degrees), how far it bends forward and how far it steps. ----
        sword(SwordMove.SLASH,
                4, 1, 0.70, 0.02, -0.68, 0.55, 0.62, 0.55, -0.50, 0.40, -0.75, 34, 0.10F, 0,
                5, 0, 0.48, -0.06, -0.94, 0.70, 0.30, -0.65, -0.70, 0.20, -0.70, 22, 0.16F, 0.12F,
                6, 0, 0.12, -0.14, -1.05, -0.40, 0.10, -0.91, -0.90, 0.05, 0.40, 6, 0.24F, 0.25F,
                8, 0, -0.48, -0.38, -0.82, -0.88, -0.30, -0.08, -0.25, -0.90, 0.30, -32, 0.28F, 0.30F,
                11, 0, 0.05, -0.50, -0.90, -0.35, 0.65, -0.55, -0.30, 0.0, -1.0, -14, 0.12F, 0.12F);
        sword(SwordMove.BACKHAND,
                4, 1, -0.30, -0.02, -0.66, -0.55, 0.62, 0.56, 0.60, 0.40, -0.70, -34, 0.10F, 0,
                5, 0, -0.14, -0.08, -0.92, -0.60, 0.32, -0.72, 0.70, 0.20, -0.70, -22, 0.16F, 0.12F,
                6, 0, 0.20, -0.14, -1.05, 0.38, 0.10, -0.92, 0.92, 0.05, 0.38, -6, 0.24F, 0.25F,
                8, 0, 0.76, -0.34, -0.80, 0.90, -0.28, -0.05, 0.20, -0.90, 0.35, 32, 0.26F, 0.28F,
                11, 0, 0.55, -0.46, -0.90, 0.05, 0.80, -0.55, -0.30, 0.0, -1.0, 14, 0.12F, 0.10F);
        sword(SwordMove.CLEAVE,
                5, 1, 0.60, 0.22, -0.64, 0.30, 0.82, 0.48, -0.40, 0.30, -0.85, 28, -0.10F, 0,
                7, 0, 0.12, -0.10, -1.02, -0.42, -0.22, -0.88, -0.70, -0.60, 0.30, 2, 0.32F, 0.32F,
                9, 0, -0.42, -0.56, -0.82, -0.58, -0.78, -0.22, -0.30, -0.40, 0.85, -28, 0.46F, 0.36F,
                12, 0, 0.08, -0.52, -0.92, -0.28, 0.70, -0.62, -0.30, 0.0, -1.0, -12, 0.20F, 0.15F);
        sword(SwordMove.REVERSE_CLEAVE,
                5, 1, -0.24, 0.20, -0.66, -0.32, 0.82, 0.46, 0.40, 0.30, -0.85, -28, -0.10F, 0,
                7, 0, 0.26, -0.10, -1.02, 0.44, -0.22, -0.87, 0.70, -0.60, 0.30, -2, 0.32F, 0.32F,
                9, 0, 0.74, -0.52, -0.80, 0.62, -0.74, -0.20, 0.30, -0.40, 0.85, 28, 0.46F, 0.36F,
                12, 0, 0.55, -0.48, -0.92, 0.02, 0.82, -0.55, -0.30, 0.0, -1.0, 12, 0.20F, 0.15F);
        sword(SwordMove.RISING,
                4, 1, -0.30, -0.62, -0.78, -0.62, -0.62, -0.30, 0.55, 0.60, -0.55, -30, 0.36F, 0.10F,
                6, 0, 0.18, -0.24, -1.05, 0.44, 0.34, -0.82, 0.70, 0.60, 0.35, 0, 0.20F, 0.22F,
                8, 0, 0.66, 0.12, -0.78, 0.50, 0.82, 0.18, 0.60, 0.0, 0.80, 30, 0.0F, 0.25F,
                11, 0, 0.52, -0.38, -0.90, 0.05, 0.90, -0.40, -0.30, 0.0, -1.0, 10, 0.04F, 0.10F);
        sword(SwordMove.UPPERCUT,
                5, 1, 0.26, -0.72, -0.74, 0.04, -0.55, -0.83, 0.0, -0.83, 0.55, 4, 0.60F, 0.25F,
                7, 0, 0.18, -0.24, -1.12, 0.0, 0.58, -0.81, 0.0, 0.81, 0.58, 0, 0.28F, 0.30F,
                9, 0, 0.16, 0.20, -0.85, 0.0, 0.96, 0.28, 0.0, -0.28, 0.96, 0, -0.18F, 0.26F,
                12, 0, 0.42, -0.38, -0.92, -0.05, 0.92, -0.38, -0.30, 0.0, -1.0, 2, 0.0F, 0.10F);
        sword(SwordMove.OVERHEAD,
                6, 1, 0.24, 0.28, -0.56, 0.05, 0.62, 0.78, 0.0, 0.78, -0.62, 0, -0.25F, 0,
                7, 1, 0.24, 0.31, -0.55, 0.05, 0.56, 0.83, 0.0, 0.83, -0.56, 0, -0.30F, 0.10F,
                8, 0, 0.20, 0.15, -0.80, 0.02, 0.85, -0.52, 0.0, 0.52, 0.85, 0, 0.10F, 0.35F,
                9, 0, 0.14, -0.12, -1.06, 0.0, -0.32, -0.95, 0.0, -0.95, 0.32, 0, 0.55F, 0.60F,
                11, 0, 0.12, -0.60, -0.86, 0.0, -0.90, -0.40, 0.0, -0.40, 0.90, 0, 0.72F, 0.62F,
                13, 0, 0.30, -0.55, -0.95, 0.05, 0.10, -1.0, -0.30, 0.0, -1.0, 0, 0.50F, 0.45F,
                15, 0, 0.42, -0.46, -0.92, -0.05, 0.85, -0.50, -0.30, 0.0, -1.0, 0, 0.30F, 0.30F);
        sword(SwordMove.STAB,
                3, 1, 0.54, -0.44, -0.62, -0.18, 0.06, -0.98, 1.0, 0.0, 0.0, 14, 0.10F, 0,
                5, 0, 0.14, -0.20, -1.50, -0.06, 0.03, -1.0, 1.0, 0.0, 0.0, -16, 0.32F, 0.55F,
                7, 1, 0.14, -0.21, -1.46, -0.06, 0.03, -1.0, 1.0, 0.0, 0.0, -16, 0.34F, 0.55F,
                9, 0, 0.45, -0.48, -0.96, -0.10, 0.82, -0.56, -0.30, 0.0, -1.0, -4, 0.12F, 0.18F);
        sword(SwordMove.LUNGE,
                4, 1, 0.60, -0.40, -0.56, -0.22, 0.10, -0.97, 1.0, 0.0, 0.0, 22, 0.20F, 0,
                6, 0, 0.40, -0.30, -0.95, -0.12, 0.06, -0.99, 1.0, 0.0, 0.0, 10, 0.40F, 0.50F,
                8, 0, 0.10, -0.18, -1.72, -0.04, 0.02, -1.0, 1.0, 0.0, 0.0, -24, 0.62F, 1.0F,
                11, 1, 0.10, -0.20, -1.66, -0.04, 0.02, -1.0, 1.0, 0.0, 0.0, -24, 0.62F, 1.0F,
                14, 0, 0.45, -0.48, -0.96, -0.10, 0.82, -0.56, -0.30, 0.0, -1.0, -6, 0.20F, 0.30F);
        sword(SwordMove.LOW_SWEEP,
                5, 1, 0.74, -0.56, -0.72, 0.80, -0.10, 0.55, -0.60, -0.20, -0.75, 32, 0.80F, 0.50F,
                7, 0, 0.50, -0.60, -0.98, 0.55, -0.30, -0.78, -0.80, -0.10, 0.55, 16, 0.88F, 0.50F,
                8, 0, 0.12, -0.62, -1.05, -0.30, -0.30, -0.90, -0.95, -0.10, 0.30, 0, 0.90F, 0.50F,
                10, 0, -0.52, -0.60, -0.80, -0.90, -0.32, -0.02, -0.10, -0.10, 1.0, -34, 0.86F, 0.50F,
                13, 0, 0.08, -0.55, -0.92, -0.22, 0.66, -0.72, -0.30, 0.0, -1.0, -14, 0.40F, 0.20F);
        // The spinning cut: the blade held out to his right while the whole body goes round once to the left.
        spin(SwordMove.SPIN,
                4, 1, 0.62, -0.28, -0.74, 0.80, 0.06, 0.60, -0.60, 0.0, 0.80, 20, 0.20F, -20,
                7, 0, 0.58, -0.25, -0.82, 0.70, 0.04, -0.71, 0.70, 0.0, 0.70, 0, 0.26F, 40,
                10, 0, 0.58, -0.25, -0.82, 0.70, 0.04, -0.71, 0.70, 0.0, 0.70, 0, 0.28F, 190,
                13, 0, 0.58, -0.25, -0.82, 0.70, 0.04, -0.71, 0.70, 0.0, 0.70, 0, 0.26F, 310,
                15, 0, 0.50, -0.32, -0.86, 0.25, 0.40, -0.88, 0.60, 0.0, 0.80, 0, 0.18F, 360,
                18, 0, 0.46, -0.48, -0.92, -0.08, 0.90, -0.42, -0.30, 0.0, -1.0, 0, 0.08F, 360);
        sword(SwordMove.CROSS,
                3, 1, 0.60, 0.16, -0.66, 0.36, 0.78, 0.50, -0.40, 0.30, -0.85, 24, 0.0F, 0,
                4, 0, 0.42, 0.02, -0.90, 0.25, 0.70, -0.66, -0.60, 0.30, -0.70, 14, 0.08F, 0.10F,
                5, 0, 0.10, -0.12, -1.04, -0.44, -0.24, -0.86, -0.70, -0.60, 0.30, 0, 0.24F, 0.20F,
                7, 0, -0.44, -0.46, -0.82, -0.60, -0.72, -0.25, -0.30, -0.40, 0.85, -20, 0.20F, 0.20F,
                8, 0, -0.50, -0.20, -0.72, -0.80, -0.10, 0.55, 0.40, 0.0, 0.90, -24, 0.14F, 0.20F,
                9, 1, -0.28, 0.14, -0.70, -0.36, 0.80, 0.46, 0.40, 0.30, -0.85, -24, 0.10F, 0.20F,
                10, 0, -0.05, 0.02, -0.92, -0.22, 0.72, -0.66, 0.60, 0.30, -0.70, -12, 0.16F, 0.25F,
                11, 0, 0.24, -0.12, -1.04, 0.44, -0.24, -0.86, 0.70, -0.60, 0.30, 0, 0.32F, 0.35F,
                13, 0, 0.72, -0.48, -0.80, 0.62, -0.72, -0.22, 0.30, -0.40, 0.85, 22, 0.36F, 0.35F,
                14, 0, 0.70, -0.35, -0.85, 0.75, 0.20, -0.60, -0.40, 0.0, -0.90, 16, 0.24F, 0.25F,
                16, 0, 0.50, -0.46, -0.92, 0.0, 0.85, -0.52, -0.30, 0.0, -1.0, 8, 0.10F, 0.10F);
        // ---- The shield's six rams, thrown at whatever stands in the way of a charge while the sword is held back
        // along his side. Numbers per key: the tick, whether it stops there, the middle of the shield, the way its face
        // points and where its top is, the twist of the upper body (degrees) and how far it bends forward. ----
        ram(SwordMove.BASH,
                1, 1, -0.12, -0.40, -0.60, 0.05, 0.02, -1.0, 0.0, 1.0, 0.05, 12, 0.70F,
                2, 0, -0.08, -0.30, -1.08, 0.06, 0.04, -1.0, 0.0, 1.0, 0.05, -14, 0.82F,
                5, 0, -0.10, -0.34, -0.90, 0.07, 0.03, -1.0, 0.0, 1.0, 0.05, -10, 0.80F);
        ram(SwordMove.BASH_SWEEP,
                1, 1, -0.42, -0.36, -0.70, -0.55, 0.02, -0.84, 0.0, 1.0, 0.0, -22, 0.70F,
                2, 0, 0.0, -0.33, -1.02, 0.25, 0.02, -0.97, 0.0, 1.0, 0.0, 4, 0.76F,
                5, 0, 0.46, -0.36, -0.84, 0.85, 0.02, -0.52, 0.0, 1.0, 0.0, 30, 0.74F);
        ram(SwordMove.BASH_BACKHAND,
                1, 1, 0.14, -0.36, -0.72, 0.45, 0.02, -0.90, 0.0, 1.0, 0.0, 24, 0.70F,
                2, 0, -0.18, -0.33, -1.02, -0.25, 0.02, -0.97, 0.0, 1.0, 0.0, -4, 0.76F,
                5, 0, -0.58, -0.36, -0.80, -0.85, 0.02, -0.50, 0.0, 1.0, 0.0, -30, 0.74F);
        ram(SwordMove.BASH_UP,
                1, 1, -0.10, -0.62, -0.72, 0.05, -0.50, -0.86, 0.0, 0.86, -0.50, 0, 0.90F,
                2, 0, -0.08, -0.22, -1.02, 0.05, 0.38, -0.92, 0.0, 0.92, 0.38, 0, 0.60F,
                5, 0, -0.08, 0.05, -0.90, 0.05, 0.75, -0.66, 0.0, 0.66, 0.75, 0, 0.42F);
        ram(SwordMove.BASH_DOWN,
                2, 1, -0.10, 0.05, -0.70, 0.05, 0.62, -0.78, 0.0, 0.78, 0.62, 6, 0.50F,
                3, 0, -0.08, -0.36, -1.02, 0.05, -0.42, -0.90, 0.0, 0.90, -0.42, 0, 0.80F,
                6, 0, -0.10, -0.56, -0.90, 0.05, -0.75, -0.66, 0.0, 0.66, -0.75, -4, 0.96F);
        ram(SwordMove.BASH_SPIN,
                2, 1, -0.46, -0.32, -0.64, -0.62, 0.02, -0.78, 0.0, 1.0, 0.0, 46, 0.78F,
                3, 0, -0.04, -0.28, -1.06, 0.10, 0.02, -1.0, 0.0, 1.0, 0.0, 0, 0.90F,
                6, 0, 0.12, -0.30, -1.0, 0.30, 0.02, -0.95, 0.0, 1.0, 0.0, -34, 0.94F);
        // ---- The end of a charge: the shield raised high and slammed down into the ground before him, face down. ----
        MOVES.put(SwordMove.SLAM, new Key[] {
                key(4, true, pose(0.55, -0.52, -0.78, -0.08, 0.94, -0.34, -0.30, 0.0, -1.0, -0.10, 0.22, -0.70, 0.05,
                        0.35, -0.94, 0.0, 0.94, -0.35, 0, -0.10F, 0, 0)),
                key(6, false, pose(0.55, -0.54, -0.80, -0.08, 0.94, -0.34, -0.30, 0.0, -1.0, -0.08, -0.18, -0.95,
                        0.05, -0.55, -0.83, 0.0, 0.83, -0.55, 0, 0.40F, 0.20F, 0)),
                key(7, false, pose(0.56, -0.58, -0.82, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0, -0.06, -0.64, -1.06,
                        0.03, -0.97, -0.25, 0.0, 0.25, -0.97, 0, 0.90F, 0.40F, 0)),
                key(10, true, pose(0.56, -0.58, -0.82, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0, -0.06, -0.62, -1.05,
                        0.03, -0.97, -0.25, 0.0, 0.25, -0.97, 0, 0.96F, 0.40F, 0)) });
        // ---- Taking them out: both hands come up out of sight while the sword grows out of the fist and the shield on
        // the forearm; the sword is held up before the eyes and turned to show both flats, twirled once round like a
        // wheel, and knocked twice on the rim of the shield. ----
        MOVES.put(SwordMove.EQUIP, new Key[] {
                key(0, true, pose(0.40, -1.05, -0.80, -0.05, 0.95, -0.30, -0.30, 0.0, -1.0, -0.46, -1.10, -0.85, -0.30,
                        0.10, -1.0, 0.0, 1.0, 0.10, 0, 0, 0, 0)),
                key(7, true, pose(0.20, -0.30, -0.78, -0.04, 0.99, -0.12, -1.0, 0.0, 0.0, -0.52, -0.55, -0.92, -0.45,
                        0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0)),
                key(10, false, pose(0.22, -0.28, -0.78, -0.04, 0.99, -0.12, 0.0, 0.0, -1.0, -0.52, -0.55, -0.92, -0.45,
                        0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0)),
                key(SwordMove.TWIRL, true, pose(0.30, -0.36, -0.86, 0.0, 1.0, -0.05, 1.0, 0.0, 0.0, -0.52, -0.55,
                        -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0)),
                key(14, false, pose(0.27, -0.36, -0.86, 1.0, 0.0, -0.10, 0.0, -1.0, 0.0, -0.52, -0.55, -0.92, -0.45,
                        0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0)),
                key(16, false, pose(0.30, -0.33, -0.86, 0.0, -1.0, -0.10, -1.0, 0.0, 0.0, -0.52, -0.55, -0.92, -0.45,
                        0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0)),
                key(18, false, pose(0.33, -0.36, -0.86, -1.0, 0.0, -0.10, 0.0, 1.0, 0.0, -0.52, -0.55, -0.92, -0.45,
                        0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0)),
                key(SwordMove.TWIRLED, true, pose(0.30, -0.39, -0.86, 0.0, 1.0, -0.10, 1.0, 0.0, 0.0, -0.52, -0.55,
                        -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0)),
                key(24, true, pose(0.12, -0.18, -0.80, -0.35, 0.80, -0.48, -0.60, -0.40, -0.70, -0.46, -0.46, -0.90,
                        -0.30, 0.10, -1.0, 0.05, 1.0, 0.12, -8, 0.05F, 0, 0)),
                key(SwordMove.KNOCK, false, pose(-0.02, -0.34, -0.86, -0.75, 0.25, -0.60, -0.40, -0.90, 0.10, -0.46,
                        -0.44, -0.90, -0.30, 0.10, -1.0, 0.05, 1.0, 0.12, -14, 0.08F, 0, 0)),
                key(SwordMove.KNOCK + 1.5F, false, pose(0.06, -0.24, -0.84, -0.50, 0.62, -0.60, -0.55, -0.60, -0.60,
                        -0.46, -0.45, -0.90, -0.30, 0.10, -1.0, 0.05, 1.0, 0.12, -10, 0.06F, 0, 0)),
                key(SwordMove.KNOCK + 3, false, pose(-0.02, -0.34, -0.86, -0.75, 0.25, -0.60, -0.40, -0.90, 0.10,
                        -0.46, -0.44, -0.90, -0.30, 0.10, -1.0, 0.05, 1.0, 0.12, -14, 0.08F, 0, 0)),
                key(SwordMove.KNOCK + 5, false, pose(0.20, -0.36, -0.88, -0.30, 0.80, -0.52, -0.40, 0.0, -0.90, -0.48,
                        -0.50, -0.91, -0.40, 0.07, -1.0, 0.07, 1.0, 0.12, -6, 0.06F, 0, 0)) });
    }

    private SwordPoses() {
    }

    /** A pose from its numbers: the fist, blade and edge, the shield, its face and top, twist (degrees), lean, step, orbit (degrees). */
    private static Pose pose(double hx, double hy, double hz, double bx, double by, double bz, double ex, double ey,
            double ez, double sx, double sy, double sz, double fx, double fy, double fz, double tx, double ty, double tz,
            float twist, float lean, float step, float orbit) {
        return Pose.of(new float[] { (float) hx, (float) hy, (float) hz, (float) bx, (float) by, (float) bz, (float) ex,
                (float) ey, (float) ez, (float) sx, (float) sy, (float) sz, (float) fx, (float) fy, (float) fz,
                (float) tx, (float) ty, (float) tz, twist * Mth.DEG_TO_RAD, lean, step, orbit * Mth.DEG_TO_RAD });
    }

    private static Key key(float tick, boolean stop, Pose pose) {
        return new Key(tick, stop, pose.numbers());
    }

    /**
     * A move of the sword, as keys of fourteen numbers: the tick, whether it stops there, the fist, the blade, its edge,
     * the twist of the upper body (degrees), how far it bends forward and how far it steps. The shield stays in its guard.
     */
    private static void sword(SwordMove move, double... n) {
        Key[] keys = new Key[n.length / 14];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 14;
            keys[k] = key((float) n[i], n[i + 1] > 0.5, pose(n[i + 2], n[i + 3], n[i + 4], n[i + 5], n[i + 6], n[i + 7],
                    n[i + 8], n[i + 9], n[i + 10], GUARD.shield().x, GUARD.shield().y, GUARD.shield().z,
                    GUARD.face().x, GUARD.face().y, GUARD.face().z, GUARD.top().x, GUARD.top().y, GUARD.top().z,
                    (float) n[i + 11], (float) n[i + 12], (float) n[i + 13], 0));
        }
        MOVES.put(move, keys);
    }

    /**
     * The spinning cut, as keys of fourteen numbers: the tick, whether it stops there, the fist, the blade, its edge, the
     * twist of the upper body (degrees), how far it bends forward, and how far the whole body has spun round (degrees).
     */
    private static void spin(SwordMove move, double... n) {
        Key[] keys = new Key[n.length / 14];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 14;
            keys[k] = key((float) n[i], n[i + 1] > 0.5, pose(n[i + 2], n[i + 3], n[i + 4], n[i + 5], n[i + 6], n[i + 7],
                    n[i + 8], n[i + 9], n[i + 10], GUARD.shield().x, GUARD.shield().y, GUARD.shield().z,
                    GUARD.face().x, GUARD.face().y, GUARD.face().z, GUARD.top().x, GUARD.top().y, GUARD.top().z,
                    (float) n[i + 11], (float) n[i + 12], 0, (float) n[i + 13]));
        }
        MOVES.put(move, keys);
    }

    /**
     * A ram of the shield in a charge, as keys of thirteen numbers: the tick, whether it stops there, the middle of the
     * shield, its face, its top, the twist of the upper body (degrees) and how far it bends forward. The sword stays held
     * back as it runs (see {@link #charge}).
     */
    private static void ram(SwordMove move, double... n) {
        Pose run = charge(0.0F);
        Key[] keys = new Key[n.length / 13];
        for (int k = 0; k < keys.length; k++) {
            int i = k * 13;
            keys[k] = key((float) n[i], n[i + 1] > 0.5, pose(run.hand().x, run.hand().y, run.hand().z, run.blade().x,
                    run.blade().y, run.blade().z, run.edge().x, run.edge().y, run.edge().z, n[i + 2], n[i + 3],
                    n[i + 4], n[i + 5], n[i + 6], n[i + 7], n[i + 8], n[i + 9], n[i + 10], (float) n[i + 11],
                    (float) n[i + 12], 0, 0));
        }
        MOVES.put(move, keys);
    }

    /**
     * The pose {@code t} ticks into a move, coming in from {@code from} (where the arms were as it began) and settling
     * back into the guard (or, after a ram, into the run) once it is over.
     *
     * @param time ticks of the client's own clock, for everything that sways
     */
    static Pose at(SwordMove move, float t, float time, Pose from) {
        return switch (move) {
            case FLURRY -> from.mix(flurry(t), smooth(t / BLEND_IN));
            case CHARGE -> from.mix(charge(time), smooth(t / BLEND_IN));
            default -> keyed(MOVES.get(move), t, from, move.kind() == SwordMove.Kind.BASH ? charge(time) : GUARD,
                    move.kind() == SwordMove.Kind.BASH ? RAM_SETTLE : SETTLE);
        };
    }

    /**
     * The pose {@code t} ticks into a move of keys: along a smooth curve through {@code from} (at tick 0), every key, and
     * {@code rest} once the last key is {@code settle} ticks past. Each key is passed at the speed the keys round it
     * give (the way from the one before to the one after), so nothing stops on a key but the stops, the start and the
     * end.
     */
    private static Pose keyed(Key[] keys, float t, Pose from, Pose rest, float settle) {
        if (keys == null || keys.length == 0) {
            return rest;
        }
        Key last = keys[keys.length - 1];
        // A move that spun the body round ends a whole number of turns further: that is where it rests.
        float turns = Math.round(last.numbers()[Pose.SIZE - 1] / Mth.TWO_PI) * Mth.TWO_PI;
        float[] restNumbers = rest.numbers();
        restNumbers[Pose.SIZE - 1] += turns;
        float end = last.tick() + settle;
        if (t >= end) {
            return Pose.of(restNumbers);
        }
        boolean fromStart = keys[0].tick() > 0.0F;
        int count = keys.length + (fromStart ? 2 : 1);
        float[] ticks = new float[count];
        float[][] values = new float[count][];
        boolean[] stops = new boolean[count];
        int i = 0;
        if (fromStart) {
            ticks[i] = 0.0F;
            values[i] = from.numbers();
            stops[i] = true;
            i++;
        }
        for (Key key : keys) {
            ticks[i] = key.tick();
            values[i] = key.numbers();
            stops[i] = key.stop();
            i++;
        }
        ticks[i] = end;
        values[i] = restNumbers;
        stops[i] = true;
        return Pose.of(curve(ticks, values, stops, Math.max(0.0F, t)));
    }

    /**
     * A smooth curve through numbers given at ticks: between two of them a cubic that leaves the first and reaches the
     * second at the speed the neighbours round each give (zero at a stop and at both ends).
     */
    private static float[] curve(float[] ticks, float[][] values, boolean[] stops, float t) {
        int n = ticks.length;
        int i = 0;
        while (i + 2 < n && ticks[i + 1] <= t) {
            i++;
        }
        float t0 = ticks[i];
        float t1 = ticks[i + 1];
        float h = Math.max(1.0E-3F, t1 - t0);
        float s = Mth.clamp((t - t0) / h, 0.0F, 1.0F);
        float s2 = s * s;
        float s3 = s2 * s;
        float h00 = 2.0F * s3 - 3.0F * s2 + 1.0F;
        float h10 = s3 - 2.0F * s2 + s;
        float h01 = -2.0F * s3 + 3.0F * s2;
        float h11 = s3 - s2;
        float[] a = values[i];
        float[] b = values[i + 1];
        float[] out = new float[a.length];
        for (int c = 0; c < a.length; c++) {
            float m0 = slope(ticks, values, stops, i, c);
            float m1 = slope(ticks, values, stops, i + 1, c);
            out[c] = h00 * a[c] + h10 * h * m0 + h01 * b[c] + h11 * h * m1;
        }
        return out;
    }

    /** How fast number {@code c} runs through key {@code i}: from the key before to the one after, or zero at a stop. */
    private static float slope(float[] ticks, float[][] values, boolean[] stops, int i, int c) {
        if (stops[i] || i == 0 || i == ticks.length - 1) {
            return 0.0F;
        }
        return (values[i + 1][c] - values[i - 1][c]) / Math.max(1.0E-3F, ticks[i + 1] - ticks[i - 1]);
    }

    /**
     * The flurry: the shield comes up before the chest, and the sword stabs out twelve times all over the front, pulled
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
        double side = way[0] * Mth.DEG_TO_RAD;
        double up = way[1] * Mth.DEG_TO_RAD;
        Vec3 aim = new Vec3(Math.sin(side) * Math.cos(up), Math.sin(up), -Math.cos(side) * Math.cos(up));
        // Every stab thrown with the shoulder behind it: the body twists into it and leans a little further.
        Pose stab = pose(0.16 + aim.x * 0.9, -0.22 + aim.y * 0.9, -1.48, aim.x, aim.y, aim.z, 1.0, 0.0, 0.0,
                FLURRY_GUARD.shield().x, FLURRY_GUARD.shield().y, FLURRY_GUARD.shield().z, FLURRY_GUARD.face().x,
                FLURRY_GUARD.face().y, FLURRY_GUARD.face().z, 0.0, 1.0, 0.05, (float) (-16.0 + way[0] * 0.25), 0.3F,
                0.3F, 0);
        Pose pose = FLURRY_GUARD.mix(stab, out);
        return t > SwordMove.FLURRY.ticks() ? pose.mix(GUARD, smooth((t - SwordMove.FLURRY.ticks()) / SETTLE)) : pose;
    }

    /**
     * Running behind the shield in a charge: the shield locked before the body, the sword held back low along the right
     * side, and everything bobbing with the steps; seen from outside he is bent far forward.
     */
    static Pose charge(float time) {
        float bob = Mth.sin(time * 1.4F);
        return pose(0.62, -0.62 + 0.015 * bob, -0.66, 0.30, 0.62, 0.72, 0.0, 0.76, -0.64, -0.14, -0.40 + 0.02 * bob,
                -0.74, 0.08, 0.02, -1.0, 0.0, 1.0, 0.08, -8.0F * bob * 0.3F, 0.72F, 0, 0);
    }

    /** The pose with the shield held up to block laid over it, {@code amount} (0 to 1) of the way. */
    static Pose block(Pose pose, float amount) {
        return pose.shieldOf(BLOCK, amount);
    }

    /** How far the sword has grown out of the ring's light while they take shape, 0 to 1 (1 for every other move). */
    static float swordGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? smooth((t - 1.0F) / 7.0F) : 1.0F;
    }

    /** How far the shield has grown out of the ring's light while they take shape, 0 to 1. */
    static float shieldGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? smooth((t - 2.0F) / 7.0F) : 1.0F;
    }

    /**
     * The pose with the edge of the blade turned into the way the blade sweeps, the faster the more: a cut always leads
     * with its edge, whichever way it goes. Of the two edges the one nearer to where the keys put it leads, so the blade
     * never flips over. {@code before} is the pose a moment earlier.
     */
    static Pose led(Pose now, Pose before) {
        Vec3 tip = now.hand().add(now.blade());
        Vec3 was = before.hand().add(before.blade());
        Vec3 sweep = tip.subtract(was);
        Vec3 across = sweep.subtract(now.blade().scale(sweep.dot(now.blade())));
        double speed = across.length();
        if (speed < 1.0E-3) {
            return now;
        }
        Vec3 lead = across.scale(1.0 / speed);
        if (lead.dot(now.edge()) < 0.0) {
            lead = lead.scale(-1.0);
        }
        double w = smooth((float) (speed / 0.12));
        Vec3 edge = square(now.edge().lerp(lead, w), now.blade());
        return new Pose(now.hand(), now.blade(), edge, now.shield(), now.face(), now.top(), now.twist(), now.lean(),
                now.step(), now.orbit());
    }

    // ---- Ways ----

    /** {@code way} made one long, or {@code otherwise} when it has hardly any length. */
    private static Vec3 unit(Vec3 way, Vec3 otherwise) {
        double length = way.length();
        return length < 1.0E-4 ? otherwise : way.scale(1.0 / length);
    }

    /** {@code way} with the part along {@code axis} taken out, made one long: square to that axis. */
    private static Vec3 square(Vec3 way, Vec3 axis) {
        Vec3 flat = way.subtract(axis.scale(way.dot(axis)));
        if (flat.lengthSqr() < 1.0E-6) {
            Vec3 other = Math.abs(axis.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
            flat = other.subtract(axis.scale(other.dot(axis)));
        }
        return flat.normalize();
    }

    /** {@code way} turned {@code angle} (radians) to the left about the upright line: +x goes towards -z. */
    static Vec3 spin(Vec3 way, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(way.x * cos + way.z * sin, way.y, -way.x * sin + way.z * cos);
    }

    private static float wrap(float radians) {
        return Mth.wrapDegrees(radians * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
    }

    /** 0 below 0, 1 above 1, and a smooth S-curve in between. */
    static float smooth(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }
}
