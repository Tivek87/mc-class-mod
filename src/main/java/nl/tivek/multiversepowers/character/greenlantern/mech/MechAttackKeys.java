package nl.tivek.multiversepowers.character.greenlantern.mech;

import static nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks.*;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Keyframes;

// The keys of the mech's moves (MechAttacks): per move where its right hand goes (and its left, for some), and how its
// body crouches, stoops and twists, tick by tick.
final class MechAttackKeys {
    // The arms' rest as the walk holds them.
    static final Vec3 REST = new Vec3(4.2, 5.4, 1.7);
    static final Vec3 INWARD = new Vec3(-1.0, 0.0, 0.0);
    private static final Move[] MOVES = new Move[LENGTHS.length];

    // A move's keys for its right hand and its body; `both` mirrors the right hand's keys onto the left, `left` gives
    // the left hand keys of its own (placed as for the right hand, mirrored).
    record Move(Keyframes.Key[] hand, Keyframes.Key[] body, boolean both, Vec3 pole, int[][] pulls,
            @Nullable Keyframes.Key[] left) {
        Move(Keyframes.Key[] hand, Keyframes.Key[] body, boolean both, Vec3 pole, int[][] pulls) {
            this(hand, body, both, pole, pulls, null);
        }
    }

    static {
        Vec3 rest = REST;
        Vec3 inward = INWARD;
        // A straight right fist driven down at what stands before it, the body twisting and stooping behind it, the
        // left fist pulled back to the hip.
        Vec3 knuckles = new Vec3(0.0, -1.0, 0.15);
        Vec3 hip = new Vec3(-0.3, 0.9, 0.2);
        MOVES[CROSS] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(6, true, new Vec3(3.0, 8.0, 0.3), new Vec3(-0.8, -0.2, 0.5), 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(CROSS_HIT, false, new Vec3(0.9, 2.7, 6.3), knuckles, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(13, true, new Vec3(1.0, 2.9, 6.1), knuckles, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(19, true, new Vec3(3.4, 7.2, 2.4), new Vec3(-0.6, -0.4, 0.7), 1.0, 0.1, 1.0, 0.0, 0.0),
                hand(26, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(6, true, bent(0.7, 0.0, -0.45, 0.0)),
                        body(CROSS_HIT, false, bent(3.2, 0.74, 0.55, 0.0)),
                        body(13, true, bent(3.1, 0.7, 0.5, 0.0)),
                        body(19, true, bent(0.8, 0.18, 0.1, 0.0)), body(26, true, Body.STILL) },
                false, new Vec3(1.0, -0.5, -0.4), new int[][] { { 6, CROSS_HIT, 2 } }, new Keyframes.Key[] {
                        hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                        hand(6, true, new Vec3(2.0, 8.3, 3.2), new Vec3(-0.6, -0.3, 0.7), 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(CROSS_HIT, false, new Vec3(4.6, 5.0, -1.4), hip, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(13, true, new Vec3(4.6, 5.0, -1.4), hip, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(26, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) });
        MOVES[SWEEP] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(7, true, new Vec3(-1.2, 7.8, 2.4), new Vec3(0.43, 0.2, -0.88), 0.2, 0.1, 1.0, 0.0, 0.0),
                hand(SWEEP_FROM, false, new Vec3(-0.6, 2.7, 5.6), new Vec3(-1.0, 0.0, -0.1), 0.15, 0.05, 1.0, 0.0,
                        0.0),
                hand(12, false, new Vec3(2.2, 2.5, 6.0), new Vec3(-1.0, 0.0, 0.05), 0.15, 0.05, 1.0, 0.0, 0.0),
                hand(14, false, new Vec3(5.0, 2.6, 4.8), new Vec3(-0.85, 0.0, 0.5), 0.15, 0.05, 1.0, 0.0, 0.0),
                hand(SWEEP_TO, false, new Vec3(6.8, 3.0, 2.2), new Vec3(-0.3, 0.0, 0.95), 0.15, 0.05, 1.0, 0.0, 0.0),
                hand(21, true, new Vec3(6.4, 5.4, -0.4), new Vec3(0.0, 0.0, 1.0), 0.3, 0.3, 1.0, 0.0, 0.0),
                hand(30, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(7, true, bent(0.9, 0.2, 0.6, 0.0)),
                        body(SWEEP_FROM, false, bent(3.3, 0.78, 0.45, 0.0)),
                        body(12, false, bent(3.3, 0.78, 0.15, 0.0)),
                        body(14, false, bent(3.3, 0.78, -0.2, 0.0)),
                        body(SWEEP_TO, false, bent(3.2, 0.72, -0.55, 0.0)),
                        body(21, true, bent(1.4, 0.35, -0.6, 0.0)), body(30, true, Body.STILL) },
                false, new Vec3(1.0, -0.3, -0.6), new int[0][], new Keyframes.Key[] {
                        hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                        hand(7, true, new Vec3(4.0, 7.4, -1.6), hip, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(12, true, new Vec3(4.4, 4.2, 0.6), hip, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(SWEEP_TO, true, new Vec3(2.6, 4.4, 2.2), new Vec3(-0.6, -0.2, 0.75), 1.0, 0.0, 1.0, 0.0,
                                0.0),
                        hand(30, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) });
        Vec3 down = new Vec3(-0.4, -0.9, 0.0);
        MOVES[STOMP] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(9, true, new Vec3(5.6, 7.6, 1.2), new Vec3(0.0, -1.0, 0.2), 0.6, 0.5, 1.0, 0.0, 0.0),
                hand(STOMP_HIT, true, new Vec3(4.8, 5.0, 2.4), down, 0.9, 0.3, 1.0, 0.0, 0.0),
                hand(16, true, new Vec3(4.8, 5.1, 2.3), down, 0.9, 0.3, 1.0, 0.0, 0.0),
                hand(26, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(9, true, bent(-0.15, -0.12, 0.0, 3.2)),
                        body(STOMP_HIT, true, bent(1.0, 0.3, 0.0, 0.0)),
                        body(16, true, bent(0.8, 0.25, 0.0, 0.0)), body(26, true, Body.STILL) },
                true, new Vec3(1.0, -0.2, -0.5), new int[][] { { 9, STOMP_HIT, 3 } });
        Vec3 edge = new Vec3(-1.0, 0.0, 0.2);
        MOVES[SLAM] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(10, true, new Vec3(1.2, 15.2, 0.6), edge, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(SLAM_HIT, true, new Vec3(1.4, 1.7, 5.2), inward, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(20, true, new Vec3(1.4, 1.9, 5.1), inward, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(36, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(10, true, bent(-0.3, -0.25, 0.0, 0.0)),
                        body(SLAM_HIT, true, bent(3.2, 0.92, 0.0, 0.0)),
                        body(20, true, bent(3.1, 0.88, 0.0, 0.0)), body(36, true, Body.STILL) },
                true, new Vec3(1.0, 0.1, -0.4), new int[][] { { 10, SLAM_HIT, 3 } });
        Vec3 under = new Vec3(0.0, -1.0, 0.25);
        Vec3 high = new Vec3(2.0, 13.4, 1.7);
        Vec3 ground = new Vec3(1.3, 1.1, 4.4);
        Body low = bent(3.2, 0.9, 0.1, 0.0);
        Body up = bent(0.15, -0.1, 0.0, 0.0);
        MOVES[THROW] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(9, true, new Vec3(2.0, 1.8, 4.6), under, 0.0, 0.9, 1.0, 1.0, 0.0),
                hand(GRAB + 1, true, new Vec3(2.0, 1.8, 4.6), under, 1.0, 0.2, 1.0, 1.0, 0.0),
                hand(20, true, high, under, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(SMASH, true, ground, under, 1.0, 0.2, 1.0, 0.0, 1.0),
                hand(33, true, high, under, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(SMASH2, true, ground, under, 1.0, 0.2, 1.0, 0.0, 1.0),
                hand(43, false, new Vec3(5.0, 5.6, 3.6), new Vec3(0.0, -0.9, 0.45), 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(46, false, new Vec3(6.0, 10.8, 0.6), new Vec3(0.0, -0.7, 0.65), 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(48, true, new Vec3(3.4, 12.6, -1.2), new Vec3(0.0, -0.5, 0.8), 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(53, false, new Vec3(1.2, 10.4, 4.8), new Vec3(0.0, -0.2, 1.0), 0.1, 0.6, 1.0, 0.0, 0.0),
                hand(57, true, new Vec3(0.9, 8.0, 4.4), new Vec3(0.0, -0.3, 1.0), 0.05, 0.8, 1.0, 0.0, 0.0),
                hand(66, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(9, true, bent(3.0, 0.85, 0.1, 0.0)),
                        body(GRAB + 1, true, bent(3.0, 0.85, 0.1, 0.0)), body(20, true, up),
                        body(SMASH, true, low), body(33, true, up), body(SMASH2, true, low),
                        body(48, true, bent(0.3, -0.25, -0.4, 0.0)),
                        body(53, false, bent(0.6, 0.25, 0.3, 0.0)),
                        body(57, true, bent(0.7, 0.3, 0.35, 0.0)), body(66, true, Body.STILL) },
                false, new Vec3(1.0, -0.2, -0.5), new int[][] { { 20, SMASH, 2 }, { 33, SMASH2, 2 }, { 48, 53, 2 } });
        // The eye ray: the head snaps forward as it fires and the body rocks back from it; the arms swing on.
        MOVES[EYE] = new Move(still(LENGTHS[EYE]),
                new Keyframes.Key[] { body(0, true, Body.STILL), body(EYE_FIRE, true, bent(0.15, 0.08, 0.0, 0.0)),
                        body(EYE_FIRE + 2, false, bent(0.2, -0.08, 0.0, 0.0)),
                        body(LENGTHS[EYE], true, Body.STILL) },
                false, new Vec3(1.0, -0.3, -0.6), new int[0][]);
        // The held beam: it braces, knees bent, and leans into it, rocked back as it bursts out.
        MOVES[GLARE] = new Move(still(LENGTHS[GLARE]),
                new Keyframes.Key[] { body(0, true, Body.STILL), body(GLARE_FIRE, true, bent(0.45, 0.12, 0.0, 0.0)),
                        body(GLARE_FIRE + 3, false, bent(0.6, -0.05, 0.0, 0.0)),
                        body(GLARE_FIRE + 10, true, bent(0.55, 0.06, 0.0, 0.0)),
                        body(GLARE_MOST, true, bent(0.55, 0.06, 0.0, 0.0)),
                        body(LENGTHS[GLARE], true, Body.STILL) },
                false, new Vec3(1.0, -0.3, -0.6), new int[0][]);
        // The missile arm: the arm itself follows the crosshair (aimArm); the body squares up behind it.
        Body aimed = bent(0.35, 0.08, 0.25, 0.0);
        MOVES[AIM] = new Move(still(LENGTHS[AIM]),
                new Keyframes.Key[] { body(0, true, Body.STILL), body(AIM_OPEN, true, aimed),
                        body(AIM_MOST, true, aimed), body(LENGTHS[AIM], true, Body.STILL) },
                false, new Vec3(0.6, -1.0, -0.2), new int[0][]);
        // The rocket boots: a deep crouch, a leap as they ignite, then hovering with its arms held out and down,
        // palms to the ground; out of thrust it throws its arms out and up, and lands crouched, hands braced.
        Vec3 hover = new Vec3(4.6, 6.4, 0.9);
        Vec3 palmsDown = new Vec3(0.0, -1.0, -0.25);
        MOVES[FLY] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(3, true, new Vec3(4.4, 5.2, 1.4), palmsDown, 0.7, 0.2, 1.0, 0.0, 0.0),
                hand(FLY_LAUNCH + 2, false, new Vec3(4.8, 6.6, 0.4), palmsDown, 0.3, 0.6, 1.0, 0.0, 0.0),
                hand(FLY_LAUNCH + 6, true, hover, palmsDown, 0.35, 0.5, 1.0, 0.0, 0.0),
                hand(FLY_FALL - 6, true, hover, palmsDown, 0.35, 0.5, 1.0, 0.0, 0.0),
                hand(FLY_FALL, true, new Vec3(5.6, 8.6, 0.6), new Vec3(0.0, -0.6, 0.8), 0.4, 0.7, 1.0, 0.0, 0.0),
                hand(FLY_LAND + 3, false, new Vec3(4.0, 4.4, 2.6), palmsDown, 0.9, 0.2, 1.0, 0.0, 0.0),
                hand(LENGTHS[FLY], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(3, true, bent(1.4, 0.2, 0.0, 0.0)),
                        body(FLY_LAUNCH + 2, false, bent(-0.3, -0.05, 0.0, 0.0)),
                        body(FLY_LAUNCH + 6, true, bent(0.0, 0.08, 0.0, 0.0)),
                        body(FLY_FALL - 6, true, bent(0.0, 0.08, 0.0, 0.0)),
                        body(FLY_FALL, true, bent(0.0, -0.1, 0.0, 0.0)),
                        body(FLY_LAND + 3, false, bent(2.2, 0.45, 0.0, 0.0)),
                        body(LENGTHS[FLY], true, Body.STILL) },
                true, new Vec3(1.0, -0.4, -0.3), new int[][] { { 3, FLY_LAUNCH, 2 } });
        // The dive: both fists swung up over its head, then driven ahead and down as it plunges, and into the ground.
        MOVES[DIVE] = new Move(new Keyframes.Key[] {
                hand(0, true, hover, palmsDown, 0.35, 0.5, 1.0, 0.0, 0.0),
                hand(4, true, new Vec3(1.0, 14.6, 0.8), edge, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(DIVE_PLUNGE, true, new Vec3(1.0, 6.0, 6.2), inward, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(DIVE_LAND, false, new Vec3(1.3, 0.8, 5.0), inward, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(14, true, new Vec3(1.3, 1.0, 4.9), inward, 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(24, true, new Vec3(3.6, 6.0, 2.0), new Vec3(-0.6, -0.5, 0.6), 0.8, 0.2, 1.0, 0.0, 0.0),
                hand(LENGTHS[DIVE], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, bent(0.0, 0.08, 0.0, 0.0)),
                        body(4, true, bent(-0.2, -0.3, 0.0, 0.0)),
                        body(DIVE_PLUNGE, true, bent(0.0, 0.9, 0.0, 0.0)),
                        body(DIVE_LAND, false, bent(3.1, 1.15, 0.0, 0.0)),
                        body(14, true, bent(3.0, 1.1, 0.0, 0.0)), body(24, true, bent(0.8, 0.3, 0.0, 0.0)),
                        body(LENGTHS[DIVE], true, Body.STILL) },
                true, new Vec3(1.0, 0.1, -0.4), new int[0][]);
        // The jump: it sinks deep, swinging its arms back, springs up throwing them forward and up, hangs with them out
        // and down as on the rocket boots (which can take over in the air) and lands crouched, hands braced.
        MOVES[JUMP] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(JUMP_LAUNCH - 1, true, new Vec3(4.2, 4.6, -1.4), new Vec3(0.0, -0.8, -0.6), 0.8, 0.2, 1.0, 0.0,
                        0.0),
                hand(JUMP_LAUNCH + 3, false, new Vec3(4.4, 8.0, 2.2), new Vec3(0.0, -0.6, 0.8), 0.4, 0.5, 1.0, 0.0,
                        0.0),
                hand(JUMP_LAUNCH + 9, true, hover, palmsDown, 0.35, 0.5, 1.0, 0.0, 0.0),
                hand(JUMP_FALL, true, new Vec3(5.2, 7.4, 0.8), palmsDown, 0.4, 0.6, 1.0, 0.0, 0.0),
                hand(JUMP_LAND + 3, false, new Vec3(4.0, 4.4, 2.6), palmsDown, 0.9, 0.2, 1.0, 0.0, 0.0),
                hand(LENGTHS[JUMP], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(JUMP_LAUNCH - 1, true, bent(1.9, 0.3, 0.0, 0.0)),
                        body(JUMP_LAUNCH + 2, false, bent(-0.3, -0.08, 0.0, 0.0)),
                        body(JUMP_LAUNCH + 9, true, bent(0.0, 0.08, 0.0, 0.0)),
                        body(JUMP_FALL, true, bent(0.0, -0.05, 0.0, 0.0)),
                        body(JUMP_LAND + 3, false, bent(2.0, 0.4, 0.0, 0.0)),
                        body(LENGTHS[JUMP], true, Body.STILL) },
                true, new Vec3(1.0, -0.4, -0.3), new int[][] { { JUMP_LAUNCH - 1, JUMP_LAUNCH + 3, 2 } });
        // The flamethrower: the right arm follows the crosshair (aimArm); the body braces behind it, rocked back as the
        // fire bursts out.
        Body braced = bent(0.6, 0.12, 0.25, 0.0);
        MOVES[FLAME] = new Move(still(LENGTHS[FLAME]),
                new Keyframes.Key[] { body(0, true, Body.STILL), body(FLAME_FIRE - 2, true, braced),
                        body(FLAME_FIRE + 2, false, bent(0.75, 0.0, 0.25, 0.0)), body(FLAME_FIRE + 8, true, braced),
                        body(FLAME_MOST, true, braced), body(LENGTHS[FLAME], true, Body.STILL) },
                false, new Vec3(0.6, -1.0, -0.2), new int[0][]);
        // The hatch: it kneels a little and stoops, so its helpers drop out under the cockpit nearer the ground.
        Body kneel = bent(1.6, 0.18, 0.0, 0.0);
        MOVES[HATCH] = new Move(still(LENGTHS[HATCH]),
                new Keyframes.Key[] { body(0, true, Body.STILL), body(HATCH_OPEN, true, kneel),
                        body(HATCH_SHUT, true, kneel), body(LENGTHS[HATCH], true, Body.STILL) },
                false, new Vec3(1.0, -0.3, -0.6), new int[0][]);
        // The spin: it drops onto its knees, legs folded under it, its torso spinning on the waist with both arms out
        // (spinAim), and rises again.
        MOVES[SPIN] = new Move(still(LENGTHS[SPIN]),
                new Keyframes.Key[] { body(0, true, Body.STILL), body(5, false, bent(2.2, 0.1, 0.0, 0.0)),
                        body(SPIN_FROM, true, bent(SPIN_CROUCH, 0.18, 0.0, 0.0)),
                        body(SPIN_TO + 2, true, bent(SPIN_CROUCH, 0.18, 0.0, 0.0)),
                        body(SPIN_TO + 9, false, bent(2.0, 0.08, 0.0, 0.0)),
                        body(LENGTHS[SPIN], true, Body.STILL) },
                true, new Vec3(1.0, -0.2, -0.3), new int[0][]);
        Vec3 guard = new Vec3(2.0, 8.0, 2.8);
        Vec3 guardPalm = new Vec3(-0.6, -0.3, 0.7);
        // The jab: the left fist snapped out low and back, the right held up at the chest.
        MOVES[JAB] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(4, true, guard, guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(14, true, guard, guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(LENGTHS[JAB], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(3, true, bent(0.8, 0.1, 0.2, 0.0)),
                        body(JAB_HIT, false, bent(2.4, 0.55, -0.5, 0.0)),
                        body(10, true, bent(2.3, 0.5, -0.45, 0.0)), body(LENGTHS[JAB], true, Body.STILL) },
                false, new Vec3(1.0, -0.5, -0.4), new int[][] { { 3, JAB_HIT, 2 } }, new Keyframes.Key[] {
                        hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                        hand(3, true, new Vec3(3.0, 8.0, 0.8), new Vec3(-0.8, -0.2, 0.5), 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(JAB_HIT, false, new Vec3(1.2, 3.6, 6.0), knuckles, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(10, true, new Vec3(1.3, 3.8, 5.8), knuckles, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(LENGTHS[JAB], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) });
        // The hook: the left fist swung back, then round in a wide flat arc across the front, the body unwinding
        // behind it.
        MOVES[HOOK] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(6, true, new Vec3(3.0, 5.8, 4.0), guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(HOOK_FROM, false, new Vec3(3.1, 3.8, 4.6), guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(HOOK_TO, true, new Vec3(4.4, 3.8, 1.4), guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(20, true, new Vec3(3.8, 5.6, 0.9), guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(LENGTHS[HOOK], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(7, true, bent(1.4, 0.3, 0.55, 0.0)),
                        body(HOOK_FROM, false, bent(3.0, 0.7, 0.3, 0.0)),
                        body(13, false, bent(3.1, 0.72, -0.1, 0.0)),
                        body(HOOK_TO, false, bent(3.0, 0.68, -0.6, 0.0)),
                        body(20, true, bent(1.5, 0.35, -0.5, 0.0)), body(LENGTHS[HOOK], true, Body.STILL) },
                false, new Vec3(1.0, -0.3, -0.6), new int[][] { { 7, HOOK_FROM, 2 } }, new Keyframes.Key[] {
                        hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                        hand(7, true, new Vec3(5.4, 6.8, -0.8), new Vec3(-0.3, 0.0, 0.95), 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(HOOK_FROM, false, new Vec3(5.6, 3.0, 3.8), new Vec3(-0.8, 0.0, 0.6), 1.0, 0.0, 1.0, 0.0,
                                0.0),
                        hand(13, false, new Vec3(2.0, 2.7, 5.8), new Vec3(-1.0, 0.0, 0.0), 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(HOOK_TO, false, new Vec3(-1.4, 3.2, 4.6), new Vec3(-0.6, 0.0, -0.8), 1.0, 0.0, 1.0, 0.0,
                                0.0),
                        hand(20, true, new Vec3(0.8, 6.2, 3.0), guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(LENGTHS[HOOK], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) });
        // The uppercut: it sinks low, its right fist dropped by the knee, and rises with the fist driven straight up,
        // the left at the chest.
        Vec3 up2 = new Vec3(-0.2, 0.2, -1.0);
        MOVES[UPPERCUT] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(UPPERCUT_FROM - 1, true, new Vec3(2.6, 2.8, 4.2), up2, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(UPPERCUT_HIT, false, new Vec3(1.4, 9.6, 4.8), up2, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(18, true, new Vec3(1.4, 10.0, 4.4), up2, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(LENGTHS[UPPERCUT], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL),
                        body(UPPERCUT_FROM - 1, true, bent(3.2, 0.8, -0.3, 0.0)),
                        body(UPPERCUT_HIT, false, bent(0.2, -0.15, 0.45, 0.0)),
                        body(18, true, bent(0.1, -0.12, 0.4, 0.0)), body(LENGTHS[UPPERCUT], true, Body.STILL) },
                false, new Vec3(1.0, -0.6, -0.3), new int[][] { { UPPERCUT_FROM - 1, UPPERCUT_HIT, 2 } },
                new Keyframes.Key[] {
                        hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                        hand(UPPERCUT_FROM - 1, true, guard, guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(20, true, guard, guardPalm, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(LENGTHS[UPPERCUT], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) });
        // The knee: both hands reach out and pull down as the right knee is driven up into what stands before it.
        MOVES[KNEE] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(6, true, new Vec3(2.2, 9.0, 4.0), new Vec3(-0.2, -0.6, 0.8), 0.3, 0.5, 1.0, 0.0, 0.0),
                hand(KNEE_HIT, false, new Vec3(1.6, 5.6, 4.6), new Vec3(-0.3, -0.9, 0.2), 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(16, true, new Vec3(1.7, 5.8, 4.4), new Vec3(-0.3, -0.9, 0.2), 1.0, 0.2, 1.0, 0.0, 0.0),
                hand(LENGTHS[KNEE], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(6, true, bent(-0.2, -0.12, 0.0, 1.0)),
                        body(KNEE_HIT, false, bent(0.5, 0.5, 0.0, 4.2)),
                        body(16, true, bent(0.5, 0.45, 0.0, 3.6)), body(22, true, bent(0.6, 0.2, 0.0, 0.0)),
                        body(LENGTHS[KNEE], true, Body.STILL) },
                true, new Vec3(1.0, -0.4, -0.4), new int[][] { { 6, KNEE_HIT, 2 } });
        // The clap: both hands thrown wide open, then clapped together before the chest, the body bowing into it.
        MOVES[CLAP] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(7, true, new Vec3(5.6, 7.6, 3.0), inward, 0.0, 0.8, 1.0, 0.0, 0.0),
                hand(CLAP_HIT, false, new Vec3(0.4, 6.6, 4.8), inward, 0.05, 0.1, 1.0, 0.0, 0.0),
                hand(17, true, new Vec3(0.4, 6.8, 4.7), inward, 0.05, 0.1, 1.0, 0.0, 0.0),
                hand(LENGTHS[CLAP], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(7, true, bent(0.4, -0.12, 0.0, 0.0)),
                        body(CLAP_HIT, false, bent(1.4, 0.35, 0.0, 0.0)),
                        body(17, true, bent(1.3, 0.3, 0.0, 0.0)), body(LENGTHS[CLAP], true, Body.STILL) },
                true, new Vec3(1.0, -0.2, -0.5), new int[][] { { 7, CLAP_HIT, 3 } });
        // The pound: bent low over the ground, its fists hammer down in turn, right, left, right, left.
        Vec3 raised = new Vec3(2.4, 8.2, 3.4);
        Vec3 struck = new Vec3(1.8, 1.6, 5.0);
        Vec3 fistDown = new Vec3(-0.3, -0.9, 0.2);
        MOVES[POUND] = new Move(new Keyframes.Key[] {
                hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(5, true, raised, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(POUNDS[0], false, struck, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(12, true, struck, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(17, true, raised, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(POUNDS[2], false, struck, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(24, true, struck, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                hand(32, true, new Vec3(3.6, 6.0, 2.0), new Vec3(-0.6, -0.5, 0.6), 0.8, 0.2, 1.0, 0.0, 0.0),
                hand(LENGTHS[POUND], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) },
                new Keyframes.Key[] { body(0, true, Body.STILL), body(6, true, bent(3.0, 0.85, 0.1, 0.0)),
                        body(POUNDS[0], false, bent(3.2, 0.92, 0.12, 0.0)),
                        body(POUNDS[1], false, bent(3.2, 0.92, -0.12, 0.0)),
                        body(POUNDS[2], false, bent(3.2, 0.92, 0.12, 0.0)),
                        body(POUNDS[3], false, bent(3.2, 0.92, -0.12, 0.0)),
                        body(37, true, bent(1.0, 0.3, 0.0, 0.0)), body(LENGTHS[POUND], true, Body.STILL) },
                false, new Vec3(1.0, 0.1, -0.4), new int[][] { { 5, POUNDS[0], 2 }, { 17, POUNDS[2], 2 } },
                new Keyframes.Key[] {
                        hand(0, true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0),
                        hand(8, true, raised, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(11, true, raised, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(POUNDS[1], false, struck, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(18, true, struck, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(23, true, raised, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(POUNDS[3], false, struck, fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(30, true, new Vec3(2.2, 2.6, 4.6), fistDown, 1.0, 0.0, 1.0, 0.0, 0.0),
                        hand(36, true, new Vec3(3.6, 6.0, 2.0), new Vec3(-0.6, -0.5, 0.6), 0.8, 0.2, 1.0, 0.0, 0.0),
                        hand(LENGTHS[POUND], true, rest, inward, 0.5, 0.4, 0.0, 0.0, 0.0) });
    }

    private MechAttackKeys() {
    }

    private static Keyframes.Key hand(int t, boolean stop, Vec3 at, Vec3 palm, double curl, double spread,
            double weight, double grab, double below) {
        Vec3 p = palm.normalize();
        return new Keyframes.Key(t, stop, new float[] { (float) at.x, (float) at.y, (float) at.z, (float) p.x,
                (float) p.y, (float) p.z, (float) curl, (float) spread, (float) weight, (float) grab, (float) below });
    }

    private static Body bent(double crouch, double stoop, double twist, double foot) {
        return new Body(crouch, stoop, twist, foot, 0.0);
    }

    private static Keyframes.Key body(int t, boolean stop, Body body) {
        return new Keyframes.Key(t, stop, new float[] { (float) body.crouch(), (float) body.stoop(),
                (float) body.twist(), (float) body.foot() });
    }

    // A move that leaves the arms to the walk from its start to its end.
    private static Keyframes.Key[] still(int length) {
        return new Keyframes.Key[] { hand(0, true, REST, INWARD, 0.5, 0.4, 0.0, 0.0, 0.0),
                hand(length, true, REST, INWARD, 0.5, 0.4, 0.0, 0.0, 0.0) };
    }

    @Nullable
    static Move move(int kind) {
        return kind > NONE && kind < MOVES.length ? MOVES[kind] : null;
    }

    // The keys at t; within a pull (from, to, power) the values run straight from one key to the next, gathering
    // speed into the blow instead of easing off before it.
    static float[] values(Keyframes.Key[] keys, Move move, double t) {
        for (int[] pull : move.pulls()) {
            if (t > pull[0] && t < pull[1]) {
                float[] from = Keyframes.at(keys, pull[0]);
                float[] to = Keyframes.at(keys, pull[1]);
                float u = (float) Math.pow((t - pull[0]) / (pull[1] - pull[0]), pull[2]);
                for (int i = 0; i < from.length; i++) {
                    from[i] = Mth.lerp(u, from[i], to[i]);
                }
                return from;
            }
        }
        return Keyframes.at(keys, (float) t);
    }
}
