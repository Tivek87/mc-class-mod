package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * The key poses of the sword and shield's moves (see {@link SwordPoses}): the guard, the cuts and thrusts, the rams of
 * the shield and the slam as keys along one track, the flurry and the run of a charge worked out as they go, and what
 * builds the keys of every move.
 */
abstract class SwordKeys extends SwordCurves {
    // ---- How big the sword and shield are ----

    /** How big the sword and shield are in your own hands in first person, and on a body seen from outside. */
    static final double OWN_SWORD = 0.74;
    static final double OWN_SHIELD = 0.62;
    static final double SWORD_SCALE = 0.9;
    static final double SHIELD_SCALE = 0.76;
    /** How far the shield sits out in front of the forearm seen from outside. */
    static final double SHIELD_OUT = 0.07;

    // ---- The moves ----

    // How long the arms take to settle back into the guard once the last key of a move is past, and into the run once a
    // ram of the shield is past, in ticks.
    static final float SETTLE = 7.0F;
    static final float RAM_SETTLE = 4.0F;
    // How long a flurry or a charge takes to come in from wherever the arms were, in ticks.
    static final float BLEND_IN = 3.0F;
    // How long the guard takes to start breathing once a move has settled into it, in ticks.
    static final float IDLE_IN = 10.0F;

    /**
     * The guard: the sword upright in the right fist before the right hip, its blade a little forward and its edge
     * turned so you see its flat, and the shield on the left forearm low before the left hip, its back to you.
     */
    static final Pose GUARD = pose(0.46, -0.50, -0.92, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0,
            -0.50, -0.52, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12, 0, 0.05F, 0, 0);
    /**
     * The arms as the game holds your empty hands: the right one resting low on the right of your screen (the sword
     * about to grow out of its fist), the left one out of sight below. Seen from outside the arms are the game's own.
     */
    static final Pose REST = rest(pose(RechargeAnimation.HAND_RIGHT.x(), RechargeAnimation.HAND_RIGHT.y(),
            RechargeAnimation.HAND_RIGHT.z(), 0.04, 0.99, -0.12, -1.0, 0.0, 0.0, -0.46, -1.3, -0.84, -0.45, 0.05,
            -1.0, 0.08, 1.0, 0.12, 0, 0, 0, 0));
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

    static final Map<SwordMove, Track[]> MOVES = new EnumMap<>(SwordMove.class);

    static {
        swords();
    }

    /** The cuts, thrusts, rams and slam: every number of a pose along the one track. */
    private static void swords() {
        // ---- The sword: twelve cuts and thrusts, the shield kept in its guard. Numbers per key: the tick, whether it
        // stops there (1), where the fist is (x, y, z), where the blade points, where its edge faces, the twist of the
        // upper body (degrees), how far it bends forward and how far it steps. ----
        // Seen through your own eyes every cut stays in sight: the fist well out before you, and where a blade strikes it
        // lies across the screen (never pointing straight away from you, where it would shrink to a stub).
        sword(SwordMove.SLASH,
                4, 1, 0.66, 0.06, -0.86, 0.80, 0.50, 0.33, -0.35, 0.10, -0.93, 34, 0.10F, 0,
                5, 0, 0.50, -0.02, -0.98, 0.74, 0.26, -0.62, -0.64, 0.05, -0.76, 22, 0.16F, 0.12F,
                6, 0, 0.16, -0.14, -1.06, -0.80, -0.12, -0.58, -0.58, -0.12, 0.80, 6, 0.24F, 0.25F,
                8, 0, -0.20, -0.30, -0.94, -0.90, -0.30, 0.30, 0.30, -0.40, 0.86, -30, 0.28F, 0.30F,
                11, 0, 0.10, -0.46, -0.92, -0.35, 0.65, -0.55, -0.30, 0.0, -1.0, -14, 0.12F, 0.12F);
        sword(SwordMove.BACKHAND,
                4, 1, -0.18, 0.06, -0.84, -0.78, 0.52, 0.35, 0.35, 0.10, -0.93, -34, 0.10F, 0,
                5, 0, -0.06, -0.02, -0.96, -0.72, 0.26, -0.64, 0.64, 0.05, -0.76, -22, 0.16F, 0.12F,
                6, 0, 0.24, -0.14, -1.06, 0.80, -0.12, -0.58, 0.58, -0.12, 0.80, -6, 0.24F, 0.25F,
                8, 0, 0.62, -0.30, -0.90, 0.90, -0.30, 0.30, -0.30, -0.40, 0.86, 30, 0.26F, 0.28F,
                11, 0, 0.52, -0.44, -0.92, 0.08, 0.85, -0.52, -0.30, 0.0, -1.0, 14, 0.12F, 0.10F);
        sword(SwordMove.CLEAVE,
                5, 1, 0.56, 0.12, -0.86, 0.40, 0.80, 0.45, -0.40, 0.25, -0.88, 28, -0.10F, 0,
                6, 0, 0.36, -0.02, -0.98, -0.10, 0.75, -0.65, -0.61, -0.56, -0.55, 16, 0.10F, 0.15F,
                7, 0, 0.14, -0.14, -1.06, -0.62, -0.42, -0.66, -0.21, -0.72, 0.66, 2, 0.32F, 0.32F,
                9, 0, -0.16, -0.42, -0.90, -0.62, -0.70, 0.30, 0.44, 0.0, 0.90, -28, 0.46F, 0.36F,
                12, 0, 0.10, -0.48, -0.92, -0.28, 0.70, -0.62, -0.30, 0.0, -1.0, -12, 0.20F, 0.15F);
        sword(SwordMove.REVERSE_CLEAVE,
                5, 1, -0.14, 0.10, -0.86, -0.42, 0.80, 0.42, 0.40, 0.25, -0.88, -28, -0.10F, 0,
                6, 0, 0.02, -0.02, -0.98, 0.12, 0.75, -0.65, 0.61, -0.56, -0.55, -16, 0.10F, 0.15F,
                7, 0, 0.26, -0.14, -1.06, 0.64, -0.42, -0.64, 0.21, -0.72, 0.66, -2, 0.32F, 0.32F,
                9, 0, 0.60, -0.44, -0.88, 0.64, -0.70, 0.30, -0.44, 0.0, 0.90, 28, 0.46F, 0.36F,
                12, 0, 0.52, -0.46, -0.92, 0.02, 0.84, -0.54, -0.30, 0.0, -1.0, 12, 0.20F, 0.15F);
        sword(SwordMove.RISING,
                4, 1, -0.04, -0.50, -0.86, -0.62, -0.62, 0.40, -0.30, -0.20, -0.93, -30, 0.36F, 0.10F,
                5, 0, 0.04, -0.36, -0.98, -0.30, -0.35, -0.89, 0.69, 0.55, -0.45, -14, 0.30F, 0.16F,
                6, 0, 0.20, -0.20, -1.06, 0.70, 0.35, -0.62, 0.40, 0.53, 0.74, 0, 0.20F, 0.22F,
                8, 0, 0.56, 0.02, -0.88, 0.55, 0.78, 0.30, -0.69, 0.21, 0.69, 30, 0.0F, 0.25F,
                11, 0, 0.50, -0.40, -0.92, 0.05, 0.90, -0.42, -0.30, 0.0, -1.0, 10, 0.04F, 0.10F);
        sword(SwordMove.UPPERCUT,
                5, 1, 0.24, -0.52, -0.86, 0.04, -0.55, -0.83, 0.0, 0.83, -0.55, 4, 0.60F, 0.25F,
                7, 0, 0.18, -0.22, -1.10, 0.0, 0.60, -0.80, 0.0, 0.80, 0.60, 0, 0.28F, 0.30F,
                9, 0, 0.16, 0.08, -0.88, 0.0, 0.94, 0.34, 0.0, -0.34, 0.94, 0, -0.18F, 0.26F,
                12, 0, 0.40, -0.40, -0.92, -0.05, 0.92, -0.38, -0.30, 0.0, -1.0, 2, 0.0F, 0.10F);
        sword(SwordMove.OVERHEAD,
                6, 1, 0.22, 0.12, -0.83, 0.05, 0.62, 0.78, 0.0, 0.78, -0.62, 0, -0.25F, 0,
                7, 1, 0.22, 0.15, -0.82, 0.05, 0.55, 0.83, 0.0, 0.83, -0.55, 0, -0.30F, 0.10F,
                8, 0, 0.19, 0.06, -0.94, 0.02, 0.85, -0.52, 0.0, -0.52, -0.85, 0, 0.10F, 0.35F,
                9, 0, 0.14, -0.22, -1.06, 0.0, -0.60, -0.80, 0.0, -0.80, 0.60, 0, 0.55F, 0.60F,
                11, 0, 0.12, -0.50, -0.90, 0.0, -0.92, -0.40, 0.0, -0.40, 0.92, 0, 0.72F, 0.62F,
                13, 0, 0.28, -0.52, -0.94, 0.05, 0.10, -1.0, 0.0, 1.0, 0.10, 0, 0.50F, 0.45F,
                15, 0, 0.42, -0.46, -0.92, -0.05, 0.85, -0.50, -0.30, 0.0, -1.0, 0, 0.30F, 0.30F);
        // The thrusts go in from low at the right to where you aim, the flat of the blade level.
        sword(SwordMove.STAB,
                3, 1, 0.56, -0.44, -0.72, -0.25, 0.10, -0.96, 1.0, 0.0, -0.26, 14, 0.10F, 0,
                5, 0, 0.30, -0.30, -1.20, -0.30, 0.20, -0.93, 1.0, 0.0, -0.32, -16, 0.32F, 0.55F,
                7, 1, 0.30, -0.31, -1.16, -0.30, 0.20, -0.93, 1.0, 0.0, -0.32, -16, 0.34F, 0.55F,
                9, 0, 0.45, -0.48, -0.96, -0.10, 0.82, -0.56, -0.30, 0.0, -1.0, -4, 0.12F, 0.18F);
        sword(SwordMove.LUNGE,
                4, 1, 0.60, -0.42, -0.74, -0.28, 0.12, -0.95, 1.0, 0.0, -0.30, 22, 0.20F, 0,
                6, 0, 0.44, -0.34, -0.98, -0.28, 0.16, -0.95, 1.0, 0.0, -0.30, 10, 0.40F, 0.50F,
                8, 0, 0.26, -0.26, -1.30, -0.26, 0.19, -0.95, 1.0, 0.0, -0.27, -24, 0.62F, 1.0F,
                11, 1, 0.26, -0.27, -1.26, -0.26, 0.19, -0.95, 1.0, 0.0, -0.27, -24, 0.62F, 1.0F,
                14, 0, 0.45, -0.48, -0.96, -0.10, 0.82, -0.56, -0.30, 0.0, -1.0, -6, 0.20F, 0.30F);
        sword(SwordMove.LOW_SWEEP,
                5, 1, 0.56, -0.36, -0.84, 0.80, -0.10, 0.58, 0.58, -0.08, -0.80, 32, 0.80F, 0.50F,
                7, 0, 0.42, -0.40, -0.98, 0.62, -0.20, -0.76, -0.77, -0.06, -0.62, 16, 0.88F, 0.50F,
                8, 0, 0.10, -0.40, -1.04, -0.80, -0.20, -0.56, -0.56, -0.06, 0.82, 0, 0.90F, 0.50F,
                10, 0, -0.18, -0.40, -0.90, -0.95, -0.20, 0.22, 0.22, -0.06, 0.97, -34, 0.86F, 0.50F,
                13, 0, 0.08, -0.48, -0.92, -0.25, 0.68, -0.68, -0.30, 0.0, -1.0, -14, 0.40F, 0.20F);
        // The spinning cut: the blade held out to his right while the whole body goes round once to the left.
        spin(SwordMove.SPIN,
                4, 1, 0.62, -0.28, -0.80, 0.80, 0.06, 0.60, -0.60, 0.0, 0.80, 20, 0.20F, -20,
                7, 0, 0.58, -0.26, -0.90, 0.82, 0.04, -0.57, 0.57, 0.0, 0.82, 0, 0.26F, 40,
                10, 0, 0.58, -0.26, -0.90, 0.82, 0.04, -0.57, 0.57, 0.0, 0.82, 0, 0.28F, 190,
                13, 0, 0.58, -0.26, -0.90, 0.82, 0.04, -0.57, 0.57, 0.0, 0.82, 0, 0.26F, 310,
                15, 0, 0.50, -0.34, -0.90, 0.30, 0.45, -0.84, 0.60, 0.0, 0.80, 0, 0.18F, 360,
                18, 0, 0.46, -0.48, -0.92, -0.08, 0.90, -0.42, -0.30, 0.0, -1.0, 0, 0.08F, 360);
        sword(SwordMove.CROSS,
                3, 1, 0.56, 0.10, -0.86, 0.36, 0.80, 0.48, -0.40, 0.30, -0.85, 24, 0.0F, 0,
                4, 0, 0.40, -0.02, -0.98, 0.20, 0.75, -0.63, -0.61, -0.56, -0.55, 14, 0.08F, 0.10F,
                5, 0, 0.14, -0.16, -1.06, -0.62, -0.42, -0.66, -0.21, -0.72, 0.66, 0, 0.24F, 0.20F,
                7, 0, -0.14, -0.40, -0.90, -0.62, -0.70, 0.30, 0.44, 0.0, 0.90, -20, 0.20F, 0.20F,
                8, 0, -0.22, -0.18, -0.86, -0.85, 0.10, 0.50, 0.10, 1.0, 0.0, -24, 0.14F, 0.20F,
                9, 1, -0.14, 0.08, -0.86, -0.42, 0.80, 0.42, 0.40, 0.25, -0.88, -24, 0.10F, 0.20F,
                10, 0, 0.02, -0.02, -0.98, 0.12, 0.75, -0.65, 0.61, -0.56, -0.55, -12, 0.16F, 0.25F,
                11, 0, 0.26, -0.16, -1.06, 0.64, -0.42, -0.64, 0.21, -0.72, 0.66, 0, 0.32F, 0.35F,
                13, 0, 0.60, -0.44, -0.88, 0.64, -0.70, 0.30, -0.44, 0.0, 0.90, 22, 0.36F, 0.35F,
                14, 0, 0.62, -0.40, -0.90, 0.85, 0.30, -0.40, -0.40, 0.0, -0.85, 16, 0.24F, 0.25F,
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
        whole(SwordMove.SLAM,
                key(4, true, pose(0.55, -0.52, -0.78, -0.08, 0.94, -0.34, -0.30, 0.0, -1.0, -0.10, 0.22, -0.70, 0.05,
                        0.35, -0.94, 0.0, 0.94, -0.35, 0, -0.10F, 0, 0)),
                key(6, false, pose(0.55, -0.54, -0.80, -0.08, 0.94, -0.34, -0.30, 0.0, -1.0, -0.08, -0.18, -0.95,
                        0.05, -0.55, -0.83, 0.0, 0.83, -0.55, 0, 0.40F, 0.20F, 0)),
                key(7, false, pose(0.56, -0.58, -0.82, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0, -0.06, -0.64, -1.06,
                        0.03, -0.97, -0.25, 0.0, 0.25, -0.97, 0, 0.90F, 0.40F, 0)),
                key(10, true, pose(0.56, -0.58, -0.82, -0.10, 0.93, -0.36, -0.30, 0.0, -1.0, -0.06, -0.62, -1.05,
                        0.03, -0.97, -0.25, 0.0, 0.25, -0.97, 0, 0.96F, 0.40F, 0)));
    }

    /** A pose from its numbers: the fist, blade and edge, the shield, its face and top, twist (degrees), lean, step, orbit (degrees). */
    static Pose pose(double hx, double hy, double hz, double bx, double by, double bz, double ex, double ey,
            double ez, double sx, double sy, double sz, double fx, double fy, double fz, double tx, double ty, double tz,
            float twist, float lean, float step, float orbit) {
        return Pose.of(new float[] { (float) hx, (float) hy, (float) hz, (float) bx, (float) by, (float) bz, (float) ex,
                (float) ey, (float) ez, (float) sx, (float) sy, (float) sz, (float) fx, (float) fy, (float) fz,
                (float) tx, (float) ty, (float) tz, twist * Mth.DEG_TO_RAD, lean, step, orbit * Mth.DEG_TO_RAD, 0.0F });
    }

    /** The same pose with the arms fully the game's own. */
    private static Pose rest(Pose pose) {
        float[] n = pose.numbers();
        n[Pose.REST] = 1.0F;
        return Pose.of(n);
    }

    private static Key key(float tick, boolean stop, Pose pose) {
        return new Key(tick, stop, pose.numbers());
    }

    /** A move whose every number runs along the one track. */
    private static void whole(SwordMove move, Key... keys) {
        MOVES.put(move, new Track[] { new Track(0, Pose.SIZE, keys) });
    }

    /** A key of the sword hand: where the fist is. */
    static Key at(float tick, boolean stop, double x, double y, double z) {
        return at(tick, stop, new Vec3(x, y, z));
    }

    static Key at(float tick, boolean stop, Vec3 fist) {
        return key(tick, stop, GUARD.gripping(fist));
    }

    /** A key of the wrist: where the blade points and where its edge faces. */
    static Key wrist(float tick, boolean stop, Vec3 blade, Vec3 edge) {
        Vec3 way = blade.normalize();
        return key(tick, stop, GUARD.holding(way, square(edge, way)));
    }

    /** A key of the shield: its middle, the way its face points and where its top is. */
    static Key held(float tick, double sx, double sy, double sz, double fx, double fy, double fz, double tx,
            double ty, double tz) {
        return key(tick, false, pose(GUARD.hand().x, GUARD.hand().y, GUARD.hand().z, GUARD.blade().x, GUARD.blade().y,
                GUARD.blade().z, GUARD.edge().x, GUARD.edge().y, GUARD.edge().z, sx, sy, sz, fx, fy, fz, tx, ty, tz, 0,
                0, 0, 0));
    }

    /** A key of the body: how far it turns to the right (degrees) and bends forward, the arms fully its own. */
    static Key leaning(float tick, float twist, float lean) {
        return key(tick, false, pose(GUARD.hand().x, GUARD.hand().y, GUARD.hand().z, GUARD.blade().x, GUARD.blade().y,
                GUARD.blade().z, GUARD.edge().x, GUARD.edge().y, GUARD.edge().z, GUARD.shield().x, GUARD.shield().y,
                GUARD.shield().z, GUARD.face().x, GUARD.face().y, GUARD.face().z, GUARD.top().x, GUARD.top().y,
                GUARD.top().z, twist, lean, 0, 0));
    }

    /** The same key of the body, where the arms have come all the way out of the game's own: they stop coming there. */
    static Key arrived(Key key) {
        float[] still = unknown();
        still[Pose.REST] = 0.0F;
        return new Key(key.tick(), key.stop(), key.numbers(), still, still);
    }

    /** The same key of the wrist, its blade and its edge reached and left at these speeds (per tick). */
    static Key moving(Key key, Vec3 bladeIn, Vec3 edgeIn, Vec3 bladeOut, Vec3 edgeOut) {
        return moving(moving(key, Pose.BLADE, bladeIn, bladeOut), Pose.EDGE, edgeIn, edgeOut);
    }

    /** The same key with the three numbers from {@code at} on reached and left at these speeds (per tick). */
    static Key moving(Key key, int at, Vec3 in, Vec3 out) {
        float[] ins = key.in() == null ? unknown() : key.in().clone();
        float[] outs = key.out() == null ? unknown() : key.out().clone();
        ins[at] = (float) in.x;
        ins[at + 1] = (float) in.y;
        ins[at + 2] = (float) in.z;
        outs[at] = (float) out.x;
        outs[at + 1] = (float) out.y;
        outs[at + 2] = (float) out.z;
        return new Key(key.tick(), key.stop(), key.numbers(), ins, outs);
    }

    /** Speeds for every number of a pose, none of them said yet. */
    private static float[] unknown() {
        float[] speeds = new float[Pose.SIZE];
        Arrays.fill(speeds, Float.NaN);
        return speeds;
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
        whole(move, keys);
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
        whole(move, keys);
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
        whole(move, keys);
    }

    /**
     * The guard, breathing: the hands and the shield rise and sink a little with every breath, the blade sways and the
     * body turns a hair; {@code amount} of it (0 to 1).
     */
    static Pose idle(float time, float amount) {
        if (amount <= 0.0F) {
            return GUARD;
        }
        float[] n = GUARD.numbers();
        float breath = Mth.sin(time * 0.13F);
        float sway = Mth.sin(time * 0.061F + 1.3F);
        float drift = Mth.sin(time * 0.037F + 0.4F);
        n[0] += amount * 0.005F * sway;
        n[1] += amount * 0.008F * breath;
        n[2] += amount * 0.004F * drift;
        n[Pose.BLADE] += amount * 0.014F * sway;
        n[Pose.BLADE + 2] += amount * 0.012F * drift;
        n[Pose.SHIELD_FROM] += amount * 0.004F * drift;
        n[Pose.SHIELD_FROM + 1] += amount * 0.007F * Mth.sin(time * 0.13F - 0.5F);
        n[Pose.BODY_FROM] += amount * 0.012F * sway;
        n[Pose.BODY_FROM + 1] += amount * 0.012F * breath;
        return Pose.of(n);
    }

    /**
     * The flurry: the shield comes up before the chest, and the sword stabs out twelve times all over the front, pulled
     * back between two stabs, each along its own way (see {@link SwordMove#stab}).
     */
    static Pose flurry(float t) {
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
        return t > SwordMove.FLURRY.ticks()
                ? pose.mix(GUARD, (float) Ease.smooth((t - SwordMove.FLURRY.ticks()) / SETTLE)) : pose;
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

    /**
     * The pose with the edge of the blade turned into the way the blade sweeps, the faster the more: a cut always leads
     * with its edge, whichever way it goes. Of the two edges the one nearer to where the keys put it leads, so the blade
     * never flips over. {@code before} is the pose a moment earlier; {@code amount} (0 to 1) is how much of that turn it
     * takes.
     */
    static Pose led(Pose now, Pose before, float amount) {
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
        double w = amount * (float) Ease.smooth((float) (speed / 0.12));
        return now.holding(now.blade(), square(now.edge().lerp(lead, w), now.blade()));
    }
}
