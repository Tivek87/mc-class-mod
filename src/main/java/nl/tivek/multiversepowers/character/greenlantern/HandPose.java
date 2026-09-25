package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class HandPose extends HandMoves {
    private static final double[] SPOT = { 5.8, 1.25, 2.2, 2.9, 5.0 };
    private static final int TAPS = 10;
    private static final double TAP = 0.35;
    private static final double[] WRIST_WEIGHTS = remembered(2.4, 0.45);
    private static final double[] FINGER_WEIGHTS = remembered(3.2, 0.4);
    private static final double WRIST_DRAG = 0.6;
    private static final double WRIST_DRAG_MOST = 0.35;
    private static final double FINGER_DRAG = 1.2;
    private static final double FINGER_DRAG_MOST = 0.5;
    private static final double[] TRAIL = { 1.0, 0.9, 1.0, 1.15 };
    private static final double ONTO = 1.6;

    HandPose() {
    }

    public static int move(int variant) {
        return Math.floorMod(variant, MOVES);
    }

    public static double side(int variant) {
        return variant >= MOVES ? -1.0 : 1.0;
    }

    public static int life(int variant) {
        int move = move(variant);
        return move == AXE ? HandDuo.LIFE : ticks(LIFE[move]);
    }

    public static int sinks(int variant) {
        int move = move(variant);
        return move == AXE ? HandDuo.AXE_BREAKS : ticks(SINK[move]);
    }

    public static int axeVariant(Vec3 way) {
        double theta = Math.atan2(way.x, way.z);
        int k = Math.floorMod((int) Math.round(theta / (Math.PI / 8.0)), 16);
        return AXE + MOVES * (1 + k);
    }

    public static Vec3 axeWay(int variant) {
        int k = Math.floorMod(variant / MOVES - 1, 16);
        double theta = k * Math.PI / 8.0;
        return new Vec3(Math.sin(theta), 0.0, Math.cos(theta));
    }

    public static boolean locked(int variant, double t) {
        double beat = t / SLOW;
        return switch (move(variant)) {
            case SMACK -> beat >= 17.0;
            case GRAB -> beat >= CATCH_BEAT - 2.0;
            case SLAM -> beat >= 20.0;
            case AXE -> t >= HandDuo.LOCKED_FROM;
            case POUND -> {
                for (double hit : POUND_BEATS) {
                    if (beat >= hit - 4.0 && beat <= hit + 3.0) {
                        yield true;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    public static double spot(int move) {
        return move >= 0 && move < SPOT.length ? SPOT[move] : 0.0;
    }

    public static HandPose at(int variant, double t, double reach) {
        int move = move(variant);
        if (move == AXE) {
            HandPose buried = new HandPose();
            buried.length = BURIED;
            return buried;
        }
        double side = side(variant);
        double beat = t / SLOW;
        HandPose pose = moving(move, side, beat, reach);
        for (double blow : blows(move)) {
            double near = Ease.bump((beat - blow) / ONTO);
            if (near > 0.0) {
                pose.onto(near, moving(move, side, blow, reach), struck(move, side, blow, reach));
            }
        }
        return pose;
    }

    private void onto(double near, HandPose there, HandPose blow) {
        this.lean += near * (blow.lean - there.lean);
        this.length += near * (blow.length - there.length);
        this.twist += near * (blow.twist - there.twist);
        this.flex += near * (blow.flex - there.flex);
        this.sweep += near * (blow.sweep - there.sweep);
    }

    private static HandPose moving(int move, double side, double t, double reach) {
        HandPose pose = shaped(move, side, t, reach);
        Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
        Vec3[] now = pose.axes(ahead);
        Vec3 arm = Vec3.ZERO;
        Vec3 hand = Vec3.ZERO;
        for (int k = 1; k <= TAPS; k++) {
            Vec3[] then = shaped(move, side, t - k * TAP, reach).axes(ahead);
            arm = arm.add(then[0].scale(WRIST_WEIGHTS[k]));
            hand = hand.add(then[3].scale(FINGER_WEIGHTS[k]));
        }
        double lag = Math.atan2(arm.dot(now[1]), arm.dot(now[0]));
        pose.flex += Ease.soft(WRIST_DRAG * lag, WRIST_DRAG_MOST);
        double flung = Ease.soft(FINGER_DRAG * Math.atan2(hand.dot(now[4]), hand.dot(now[3])), FINGER_DRAG_MOST);
        for (int k = 0; k < 5; k++) {
            if (move == FINGER && k == 1) {
                continue;
            }
            double trail = k < 4 ? TRAIL[k] : 0.5;
            double curl = pose.curl[k];
            pose.curl[k] = curl + flung * trail * (1.0 - curl) * (0.3 + curl);
            pose.hook[k] *= 1.0 + 0.8 * flung * trail;
        }
        return pose;
    }

    private static HandPose shaped(int move, double side, double t, double reach) {
        HandPose pose = new HandPose();
        pose.length = BURIED;
        switch (move) {
            case SMACK -> pose.smack(t, side, reach);
            case GRAB -> pose.grab(t);
            case FINGER -> pose.finger(t);
            case SLAM -> pose.slam(t);
            default -> pose.pound(t, reach);
        }
        return pose;
    }

    private static double[] remembered(double swing, double damping) {
        double[] weights = new double[TAPS + 1];
        double turn = swing * Math.sqrt(1.0 - damping * damping);
        double sum = 0.0;
        for (int k = 1; k <= TAPS; k++) {
            weights[k] = Math.exp(-damping * swing * k * TAP) * Math.sin(turn * k * TAP);
            sum += weights[k];
        }
        for (int k = 1; k <= TAPS; k++) {
            weights[k] /= sum;
        }
        return weights;
    }

    public record Place(Vec3 wrist, Vec3 arm, Vec3 armForward, Vec3 right, Vec3 up, Vec3 forward, double scale) {
        public Vec3 at(Vec3 local) {
            return this.wrist.add(this.right.scale(local.x * this.scale)).add(this.up.scale(local.y * this.scale))
                    .add(this.forward.scale(local.z * this.scale));
        }
    }

    public Place place(Vec3 base, Vec3 reach, double scale) {
        Vec3 flat = new Vec3(reach.x, 0.0, reach.z);
        flat = flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3[] axes = this.axes(flat);
        return new Place(base.add(axes[0].scale(this.length * scale)), axes[0], axes[1], axes[2], axes[3], axes[4],
                scale);
    }

    private Vec3[] axes(Vec3 flat) {
        Vec3 f = Vectors.spin(flat, Vectors.UP, -this.sweep);
        Vec3 s = f.cross(Vectors.UP).normalize();
        // Order fixed: lean, then twist about the forearm, then flex; swapping any bends the hand wrong.
        double cos = Math.cos(this.lean);
        double sin = Math.sin(this.lean);
        Vec3 arm = Vectors.UP.scale(cos).add(f.scale(sin));
        Vec3 face = f.scale(cos).subtract(Vectors.UP.scale(sin));
        double tc = Math.cos(this.twist);
        double ts = Math.sin(this.twist);
        Vec3 right = s.scale(tc).add(face.scale(ts));
        Vec3 palm = face.scale(tc).subtract(s.scale(ts));
        double fc = Math.cos(this.flex);
        double fs = Math.sin(this.flex);
        Vec3 up = arm.scale(fc).add(palm.scale(fs));
        Vec3 forward = palm.scale(fc).subtract(arm.scale(fs));
        return new Vec3[] { arm, palm, right, up, forward };
    }
}
