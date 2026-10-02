package nl.tivek.multiversepowers.character.greenlantern.mech;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The mech's arms through its build, in MechScript's places: two loose forearms that fly in, reach for the target, clap
// it and rise, then set on the shoulders by their upper arms. Keys are the right arm's, x outwards; the left mirrors.
public final class MechMoves {
    private static final double CLAP_HIGH = 1.25;
    private static final double CLAP_OUT = 0.5;
    private static final Vec3 CLAP_WAY = new Vec3(0.0, 0.12, 1.0).normalize();
    private static final double SQUEEZE_LIFT = 0.3;
    private static final double SWING_PULL = 2.2;
    private static final double SPIN = Math.PI * 3.0;
    private static final int SPUN = 72;
    private static final double WALK_SWING = 0.3;
    private static final double WALK_BEND = 0.22;
    private static final Vec3 WALK_HANG = new Vec3(0.06, -0.94, 0.34).normalize();
    private static final Keyframes.Key[] LOOSE;
    private static final Keyframes.Key[] SET;
    private static final Keyframes.Key[] SET_LEFT;

    // A forearm with its hand: its elbow, which way it runs to the wrist, which way the palm faces, how far the fingers
    // curl (0 open, 1 a fist) and spread; upper is how far the upper arm has grown out to its elbow (0: none yet). The
    // hand turns at the wrist: folded towards its palm (+) and then tilted across it, in radians.
    public record Arm(Vec3 elbow, Vec3 way, Vec3 palm, double curl, double spread, double upper, double fold,
            double tilt) {
        public Arm(Vec3 elbow, Vec3 way, Vec3 palm, double curl, double spread, double upper) {
            this(elbow, way, palm, curl, spread, upper, 0.0, 0.0);
        }

        // The middle of the palm.
        public Vec3 hand() {
            if (this.fold == 0.0 && this.tilt == 0.0) {
                return this.elbow.add(this.way.scale(MechScript.PALM_ALONG));
            }
            // A stage's own places turn the other way round from the world's, so across the hand is way x palm here.
            Vec3 across = this.way.cross(this.palm).normalize();
            Vec3 folded = this.way.scale(Math.cos(this.fold)).add(this.palm.scale(Math.sin(this.fold)));
            Vec3 along = folded.scale(Math.cos(this.tilt)).subtract(across.scale(Math.sin(this.tilt)));
            return this.elbow.add(this.way.scale(MechScript.FOREARM))
                    .add(along.scale(MechScript.PALM_ALONG - MechScript.FOREARM));
        }
    }

    static {
        Vec3 clapHand = new Vec3(CLAP_OUT, CLAP_HIGH, MechScript.TARGET_AHEAD);
        Vec3 clapElbow = clapHand.subtract(CLAP_WAY.scale(MechScript.PALM_ALONG));
        Vec3 set = MechScript.SHOULDER.add(new Vec3(0.95, -0.2, 0.2).normalize().scale(MechScript.UPPER_ARM));
        Vec3 setWay = new Vec3(0.6, 0.12, 0.8);
        Vec3 setPalm = new Vec3(0.0, -0.6, 0.8);
        Vec3 inward = new Vec3(-1.0, 0.0, 0.0);
        LOOSE = new Keyframes.Key[] {
                loose(58, true, new Vec3(6.2, 13.5, -1.2), new Vec3(0.15, 0.85, 0.5), new Vec3(-0.3, -0.4, 0.85), 0.2,
                        0.6, 0.0),
                loose(63, true, new Vec3(6.0, 12.8, -0.6), new Vec3(0.15, 0.85, 0.5), new Vec3(-0.3, -0.4, 0.85), 0.2,
                        0.6, 0.0),
                loose(67, true, new Vec3(3.4, 5.4, 1.3), new Vec3(0.3, 0.85, 0.45), new Vec3(-0.2, -0.4, 0.9), 0.1,
                        1.0, 0.0),
                loose(72, false, new Vec3(3.4, 3.0, 1.2), new Vec3(-0.1, -0.3, 0.95), new Vec3(-0.9, 0.0, -0.1),
                        0.25, 0.7, 1.0),
                loose(75, true, new Vec3(4.2, 1.5, 1.05), new Vec3(0.08, 0.12, 1.0), inward, 0.05, 1.0, 1.0),
                loose(81, true, new Vec3(4.7, 1.6, 0.95), new Vec3(0.12, 0.14, 1.0), inward, 0.0, 1.0, 1.0),
                loose(MechScript.CLAP, true, clapElbow, CLAP_WAY, inward, 0.0, 0.2, 1.0),
                loose(92, false, clapElbow.add(0.0, SQUEEZE_LIFT, 0.0), CLAP_WAY, inward, 0.3, 0.1, 1.0),
                loose(MechScript.RELEASE, true, clapElbow.add(0.0, SQUEEZE_LIFT, 0.0), CLAP_WAY, inward, 0.3, 0.1,
                        1.0),
                loose(101, false, new Vec3(4.3, 2.8, 0.9), new Vec3(0.25, 0.3, 0.92), new Vec3(0.0, 0.1, 1.0), 0.05,
                        1.0, 0.6),
                loose(106, false, new Vec3(4.9, 5.6, 0.6), new Vec3(0.5, 0.75, 0.43), new Vec3(0.0, -0.3, 0.95), 0.1,
                        1.0, 0.0),
                loose(112, true, set, setWay, setPalm, 0.15, 0.8, 0.0),
                loose(MechScript.ELBOWS, true, set, setWay, setPalm, 0.15, 0.8, 0.0) };
        SET = new Keyframes.Key[] {
                set(MechScript.ELBOWS, true, new Vec3(0.95, -0.2, 0.2), setWay, setPalm, 0.15, 0.8),
                set(130, false, new Vec3(0.95, -0.15, 0.22), new Vec3(0.64, 0.1, 0.76), setPalm, 0.2, 0.8),
                set(139, false, new Vec3(0.96, -0.12, 0.24), new Vec3(0.82, 0.12, 0.56), new Vec3(0.0, 0.3, 0.95),
                        0.1, 1.0),
                set(146, true, new Vec3(0.96, -0.1, 0.26), new Vec3(0.8, 0.14, 0.58), new Vec3(0.0, 0.3, 0.95), 0.1,
                        1.0),
                set(151, false, new Vec3(0.95, -0.3, 0.2), new Vec3(0.8, -0.1, 0.6), new Vec3(0.0, 0.2, 1.0), 0.35,
                        0.9),
                set(160, true, new Vec3(0.95, -0.1, 0.25), new Vec3(0.82, 0.22, 0.55), new Vec3(0.0, 0.1, 1.0), 0.1,
                        1.0),
                set(168, false, new Vec3(0.93, 0.02, 0.3), new Vec3(0.8, 0.3, 0.52), new Vec3(0.0, 0.0, 1.0), 0.1,
                        1.0),
                set(MechScript.LOCK, true, new Vec3(0.8, 0.4, 0.45), new Vec3(0.42, 0.62, 0.66),
                        new Vec3(-0.1, -0.35, 1.0), 0.15, 1.0),
                set(186, true, new Vec3(0.8, 0.42, 0.43), new Vec3(0.4, 0.64, 0.66), new Vec3(-0.1, -0.35, 1.0), 0.2,
                        1.0),
                set(MechScript.SETTLED - 2, true, new Vec3(0.34, -0.93, 0.1), new Vec3(0.06, -0.78, 0.62), inward, 0.5,
                        0.4) };
        SET_LEFT = SET.clone();
        SET_LEFT[7] = set(MechScript.LOCK, true, new Vec3(0.92, -0.05, 0.4), new Vec3(0.55, 0.14, 0.83),
                new Vec3(0.0, 0.2, 1.0), 0.15, 1.0);
        SET_LEFT[8] = set(186, true, new Vec3(0.92, -0.03, 0.4), new Vec3(0.55, 0.16, 0.82), new Vec3(0.0, 0.2, 1.0),
                0.2, 1.0);
    }

    private MechMoves() {
    }

    private static Keyframes.Key loose(int t, boolean stop, Vec3 elbow, Vec3 way, Vec3 palm, double curl,
            double spread, double onTarget) {
        Vec3 w = way.normalize();
        Vec3 p = square(palm, w);
        return new Keyframes.Key(t, stop, new float[] { (float) elbow.x, (float) elbow.y, (float) elbow.z, (float) w.x,
                (float) w.y, (float) w.z, (float) p.x, (float) p.y, (float) p.z, (float) curl, (float) spread,
                (float) onTarget });
    }

    private static Keyframes.Key set(int t, boolean stop, Vec3 upper, Vec3 way, Vec3 palm, double curl,
            double spread) {
        Vec3 u = upper.normalize();
        Vec3 w = way.normalize();
        Vec3 p = square(palm, w);
        return new Keyframes.Key(t, stop, new float[] { (float) u.x, (float) u.y, (float) u.z, (float) w.x,
                (float) w.y, (float) w.z, (float) p.x, (float) p.y, (float) p.z, (float) curl, (float) spread });
    }

    public static Arm arm(boolean right, MechScript.Stage stage, double t) {
        Arm arm = t < MechScript.ELBOWS ? loose(stage, t) : set(right, t);
        if (right) {
            return arm;
        }
        return new Arm(MechScript.mirror(arm.elbow()), MechScript.mirror(arm.way()), MechScript.mirror(arm.palm()),
                arm.curl(), arm.spread(), arm.upper());
    }

    private static Arm loose(MechScript.Stage stage, double t) {
        float[] v = Keyframes.at(LOOSE, (float) t);
        if (t > MechScript.SWING && t < MechScript.CLAP) {
            // The swing gathers speed right into the clap instead of easing off before it.
            float[] from = Keyframes.at(LOOSE, MechScript.SWING);
            float[] to = Keyframes.at(LOOSE, MechScript.CLAP);
            float u = (float) Math.pow((t - MechScript.SWING) / (MechScript.CLAP - MechScript.SWING), SWING_PULL);
            for (int i = 0; i < v.length; i++) {
                v[i] = Mth.lerp(u, from[i], to[i]);
            }
        }
        Vec3 elbow = new Vec3(v[0], v[1] + v[11] * stage.targetY(), v[2]);
        if (t > MechScript.CLAP && t < MechScript.RELEASE) {
            double shake = Math.sin((t - MechScript.CLAP) * 2.3) * (1.0 - (t - MechScript.CLAP) / 14.0);
            elbow = elbow.add(0.05 * shake, 0.0, 0.0);
        }
        Vec3 way = new Vec3(v[3], v[4], v[5]).normalize();
        Vec3 palm = square(new Vec3(v[6], v[7], v[8]), way);
        double left = 1.0 - Mth.clamp((t - MechScript.ARMS_FORM) / (SPUN - MechScript.ARMS_FORM), 0.0, 1.0);
        if (left > 0.0) {
            // It spins about its length as it flies in, slowing down until it hangs ready by the target.
            palm = Vectors.spin(palm, way, SPIN * left * left);
        }
        return new Arm(elbow, way, palm, v[9], v[10], upper(t));
    }

    private static Arm set(boolean right, double t) {
        float[] v = Keyframes.at(right ? SET : SET_LEFT, (float) t);
        Vec3 upper = new Vec3(v[0], v[1], v[2]);
        if (t > MechScript.SETTLED) {
            upper = upper.add(0.0, 0.015 * Math.sin(t * 0.09), 0.0);
        } else if (t > MechScript.LOCK + 2 && t < 190) {
            upper = upper.add(0.0, 0.02 * Math.sin(t * 1.9), 0.0);
        }
        upper = upper.normalize();
        Vec3 way = new Vec3(v[3], v[4], v[5]).normalize();
        Vec3 elbow = MechScript.SHOULDER.add(upper.scale(MechScript.UPPER_ARM));
        return new Arm(elbow, way, square(new Vec3(v[6], v[7], v[8]), way), v[9], v[10], 1.0);
    }

    // A walking mech's arm, swinging from the shoulder against its legs: swing 1 has the right arm forward and the
    // left one back. The further it walks (walking 0 to 1) the lower its forearm hangs; it bends up a little as it
    // swings forward.
    public static Arm walking(boolean right, double t, double swing, double walking) {
        Arm set = set(right, Math.max(t, MechScript.SETTLED));
        double angle = WALK_SWING * (right ? swing : -swing);
        Vec3 across = new Vec3(1.0, 0.0, 0.0);
        Vec3 elbow = MechScript.SHOULDER.add(Vectors.spin(set.elbow().subtract(MechScript.SHOULDER), across, -angle));
        double bend = angle + WALK_BEND * Math.max(0.0, angle) / WALK_SWING;
        Vec3 hang = set.way().lerp(WALK_HANG, Mth.clamp(walking, 0.0, 1.0)).normalize();
        Vec3 palm = square(set.palm(), hang);
        Arm arm = new Arm(elbow, Vectors.spin(hang, across, -bend), Vectors.spin(palm, across, -bend), set.curl(),
                set.spread(), 1.0);
        if (right) {
            return arm;
        }
        return new Arm(MechScript.mirror(arm.elbow()), MechScript.mirror(arm.way()), MechScript.mirror(arm.palm()),
                arm.curl(), arm.spread(), arm.upper());
    }

    public static double upper(double t) {
        return Mth.clamp((t - MechScript.UPPER_ARMS) / (MechScript.ELBOWS - MechScript.UPPER_ARMS), 0.0, 1.0);
    }

    static Vec3 square(Vec3 palm, Vec3 way) {
        Vec3 flat = palm.subtract(way.scale(palm.dot(way)));
        if (flat.lengthSqr() < 1.0E-8) {
            Vec3 side = Math.abs(way.y) < 0.9 ? way.cross(new Vec3(0.0, 1.0, 0.0)) : way.cross(new Vec3(1.0, 0.0, 0.0));
            return side.normalize();
        }
        return flat.normalize();
    }
}
