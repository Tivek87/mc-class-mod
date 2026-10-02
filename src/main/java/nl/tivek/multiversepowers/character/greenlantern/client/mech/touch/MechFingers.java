package nl.tivek.multiversepowers.character.greenlantern.client.mech.touch;

import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.world.phys.AABB;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.rig.RigFrames;
import nl.tivek.multiversepowers.engine.rig.Rig;
import nl.tivek.multiversepowers.engine.rig.RigPose;

// The fingers of a built mech's hands as they are drawn: exactly as its arm poses them (MechHandRig.settled), and on top
// of that what the blocks, a held creature or the other hand make of them (MechHandRig.posed), taken on and let go at
// no more than FOLLOW radians a tick at each joint. A hand taking hold of a ledge, letting go or brushing past a wall
// closes and opens smoothly instead of jumping, and a finger never goes into what it touches: one that would takes its
// settled angles at once.
public final class MechFingers {
    private static final double FOLLOW = 0.25;
    // A hand not drawn for this many ticks takes its fingers as they are.
    private static final double LOST = 4.0;
    // What touching makes of each hand's angles (mech id and side), as drawn, and at what time of its build.
    private static final Long2ObjectOpenHashMap<double[]> TOUCHED = new Long2ObjectOpenHashMap<>();
    private static final Long2DoubleOpenHashMap TOUCHED_AT = new Long2DoubleOpenHashMap();

    private MechFingers() {
    }

    // The bones of the hand of mech `id` on its `right` arm, `t` ticks into its build, drawn with the bones of `own`
    // (see MechPainter.arm).
    public static Frame[] frames(int id, boolean right, double t, Frame hand, MechMoves.Arm arm, boolean own,
            @Nullable double[] wall, @Nullable AABB held, @Nullable MechHandRig.Ground ground) {
        Rig rig = own ? MechHandRig.RIGHT : MechHandRig.LEFT;
        RigPose free = MechHandRig.settled(arm, own);
        boolean touches = wall != null || held != null || ground != null;
        RigPose posed = touches ? MechHandRig.posed(hand, arm, own, wall, held, ground, free) : free;
        long key = (long) id << 1 | (right ? 1L : 0L);
        double[] touched = TOUCHED.get(key);
        double since = t - TOUCHED_AT.getOrDefault(key, Double.NaN);
        boolean fresh = touched == null || touched.length != rig.values() || !(since >= 0.0 && since <= LOST);
        if (fresh) {
            touched = new double[rig.values()];
            TOUCHED.put(key, touched);
        }
        TOUCHED_AT.put(key, t);
        RigPose shown = new RigPose(rig);
        double most = FOLLOW * Math.max(0.0, since);
        for (int b = 0; b < rig.size(); b++) {
            for (int turn = 0; turn < rig.joint(b).size(); turn++) {
                int i = rig.value(b, turn);
                double want = posed.get(b, turn) - free.get(b, turn);
                double d = want - touched[i];
                double now = fresh || Math.abs(d) <= most ? want : touched[i] + Math.copySign(most, d);
                shown.set(b, turn, free.get(b, turn) + now);
            }
        }
        if (touches) {
            MechHandRig.keepOut(shown, posed, hand, own, wall, held, ground);
        }
        for (int b = 0; b < rig.size(); b++) {
            for (int turn = 0; turn < rig.joint(b).size(); turn++) {
                touched[rig.value(b, turn)] = shown.get(b, turn) - free.get(b, turn);
            }
        }
        Frame[] frames = new Frame[rig.size()];
        RigFrames.pose(rig, shown, hand, frames, true);
        return frames;
    }

    public static void forget(int id) {
        for (long side = 0; side < 2; side++) {
            TOUCHED.remove((long) id << 1 | side);
            TOUCHED_AT.remove((long) id << 1 | side);
        }
    }

    public static void clear() {
        TOUCHED.clear();
        TOUCHED_AT.clear();
    }
}
