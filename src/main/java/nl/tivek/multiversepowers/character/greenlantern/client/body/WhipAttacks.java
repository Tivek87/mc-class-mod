package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipLash;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.engine.math.Keyframes;

// The body through each of the twelve lashes: every beat puts the fist somewhere and the body in a stance, and the
// handle points where the lash is flung a tick later, so the wrist always leads the lash.
final class WhipAttacks {
    static final float LEAD = 1.0F;

    private WhipAttacks() {
    }

    static final class Beat {
        final float tick;
        boolean stop;
        Vec3 grip = WhipKeys.GUARD.grip();
        @Nullable
        Vec3 handle;
        Vec3 left = WhipKeys.GUARD.left();
        float leftOn;
        double twist = 12.0;
        float lean = 0.04F;
        float step = 0.12F;
        float roll;
        float squat = 0.08F;
        float wide = 0.3F;
        float kneel;
        double orbit;

        Beat(float tick) {
            this.tick = tick;
        }

        Beat stop() {
            this.stop = true;
            return this;
        }

        Beat grip(double x, double y, double z) {
            this.grip = new Vec3(x, y, z);
            return this;
        }

        Beat handle(double x, double y, double z) {
            this.handle = new Vec3(x, y, z);
            return this;
        }

        Beat left(double x, double y, double z, double on) {
            this.left = new Vec3(x, y, z);
            this.leftOn = (float) on;
            return this;
        }

        Beat twist(double degrees) {
            this.twist = degrees;
            return this;
        }

        Beat lean(double lean) {
            this.lean = (float) lean;
            return this;
        }

        Beat step(double step) {
            this.step = (float) step;
            return this;
        }

        Beat roll(double roll) {
            this.roll = (float) roll;
            return this;
        }

        Beat squat(double squat) {
            this.squat = (float) squat;
            return this;
        }

        Beat wide(double wide) {
            this.wide = (float) wide;
            return this;
        }

        Beat kneel(double kneel) {
            this.kneel = (float) kneel;
            return this;
        }

        Beat orbit(double degrees) {
            this.orbit = degrees;
            return this;
        }

        Keyframes.Key key(@Nullable Keyframes.Key[] lash) {
            Vec3 way = this.handle;
            if (way == null) {
                float[] aim = lash == null ? WhipMove.REST : Keyframes.at(lash, this.tick + LEAD);
                way = WhipCurves.aimed(aim[WhipLash.YAW] + Math.toRadians(this.orbit), aim[WhipLash.PITCH]);
            }
            WhipCurves.Pose pose = WhipCurves.pose(this.grip.x, this.grip.y, this.grip.z, way.x, way.y, way.z,
                    this.left.x, this.left.y, this.left.z, this.leftOn, (float) this.twist, this.lean, this.step);
            float[] n = pose.numbers();
            n[WhipCurves.Pose.ROLL] = this.roll;
            n[WhipCurves.Pose.SQUAT] = this.squat;
            n[WhipCurves.Pose.WIDE] = this.wide;
            n[WhipCurves.Pose.KNEEL] = this.kneel;
            n[WhipCurves.Pose.ORBIT] = (float) Math.toRadians(this.orbit);
            return WhipCurves.key(this.tick, this.stop, WhipCurves.Pose.of(n));
        }
    }

    static Beat beat(float tick) {
        return new Beat(tick);
    }

    static Keyframes.Key[] keys(WhipMove move, Beat... beats) {
        List<Keyframes.Key> keys = new ArrayList<>();
        for (Beat beat : beats) {
            keys.add(beat.key(move.lash()));
        }
        return keys.toArray(Keyframes.Key[]::new);
    }

    static void fill(Map<WhipMove, Keyframes.Key[]> moves) {
        across(moves);
        overhead(moves);
        diagonals(moves);
        low(moves);
        round(moves);
    }

    // Forehand from the right, backhand from the left: the lash sweeps flat across, the body turns and steps with it.
    private static void across(Map<WhipMove, Keyframes.Key[]> moves) {
        moves.put(WhipMove.FOREHAND, keys(WhipMove.FOREHAND,
                beat(3).grip(0.62, -0.38, -0.5).twist(32).step(-0.2).squat(0.15).wide(0.45).roll(0.05),
                beat(4.5F).stop().grip(0.7, -0.36, -0.42).twist(40).step(-0.3).squat(0.18).wide(0.5).roll(0.08),
                beat(7).grip(0.3, -0.44, -0.92).twist(10).step(0.1).squat(0.2).wide(0.5),
                beat(8.5F).grip(-0.1, -0.46, -0.9).twist(-22).step(0.3).squat(0.2).wide(0.5).roll(-0.06),
                beat(10.5F).stop().grip(-0.3, -0.5, -0.72).twist(-34).lean(0.1).step(0.35).squat(0.18).wide(0.5)
                        .roll(-0.08),
                beat(14).grip(0.05, -0.55, -0.8).twist(-8).step(0.15).squat(0.1)));
        moves.put(WhipMove.BACKHAND, keys(WhipMove.BACKHAND,
                beat(3).grip(-0.15, -0.36, -0.55).twist(-26).step(0.2).squat(0.15).wide(0.45).roll(-0.05),
                beat(4.5F).stop().grip(-0.25, -0.32, -0.48).twist(-34).step(0.3).squat(0.18).wide(0.5).roll(-0.08),
                beat(7).grip(0.1, -0.42, -0.92).twist(-6).step(0.1).squat(0.2).wide(0.5),
                beat(8.5F).grip(0.5, -0.44, -0.82).twist(24).step(-0.2).squat(0.2).wide(0.5).roll(0.06),
                beat(10.5F).stop().grip(0.72, -0.46, -0.6).twist(38).lean(0.08).step(-0.3).squat(0.18).wide(0.5)
                        .roll(0.08),
                beat(14).grip(0.5, -0.55, -0.75).twist(20).step(-0.05).squat(0.1)));
        moves.put(WhipMove.SIDEARM, keys(WhipMove.SIDEARM,
                beat(3).grip(0.65, -0.52, -0.5).twist(36).step(-0.2).squat(0.25).wide(0.5).roll(0.1),
                beat(5.5F).stop().grip(0.78, -0.5, -0.3).twist(48).step(-0.3).squat(0.3).wide(0.55).roll(0.14),
                beat(8).grip(0.55, -0.5, -0.8).twist(22).step(0.1).squat(0.3).wide(0.55),
                beat(9.5F).stop().grip(0.2, -0.48, -1.05).twist(-6).lean(0.15).step(0.45).squat(0.28).wide(0.55)
                        .roll(-0.05),
                beat(12).stop().grip(0.12, -0.5, -0.98).twist(-10).lean(0.15).step(0.45).squat(0.25).wide(0.5),
                beat(15).grip(0.3, -0.55, -0.85).twist(4).step(0.2).squat(0.12)));
    }

    // Up over the shoulder and down in front, a quick pop of the wrist, and a twirl overhead before the crack.
    private static void overhead(Map<WhipMove, Keyframes.Key[]> moves) {
        moves.put(WhipMove.OVERHEAD, keys(WhipMove.OVERHEAD,
                beat(3).grip(0.4, -0.1, -0.55).twist(14).lean(-0.1).squat(0.1),
                beat(6).stop().grip(0.32, 0.12, -0.38).twist(18).lean(-0.2).step(-0.2).squat(0.12),
                beat(7.5F).grip(0.32, 0.14, -0.36).twist(18).lean(-0.22).step(-0.2).squat(0.12),
                beat(10).grip(0.28, 0.02, -0.7).twist(8).lean(0.05).step(0.3).squat(0.15),
                beat(11.5F).stop().grip(0.22, -0.38, -1.02).twist(-4).lean(0.3).step(0.55).squat(0.2),
                beat(14).stop().grip(0.24, -0.5, -0.95).twist(-4).lean(0.35).step(0.55).squat(0.25),
                beat(18).grip(0.32, -0.55, -0.85).twist(6).lean(0.1).step(0.2)));
        moves.put(WhipMove.SNAP, keys(WhipMove.SNAP,
                beat(2).grip(0.4, -0.3, -0.7).twist(14),
                beat(3.5F).stop().grip(0.38, -0.18, -0.62).twist(16).lean(-0.05),
                beat(6).stop().grip(0.3, -0.4, -0.98).twist(4).lean(0.12).step(0.25),
                beat(9).grip(0.34, -0.5, -0.88).twist(8).lean(0.06).step(0.2),
                beat(11).grip(0.37, -0.55, -0.82).twist(10)));
        moves.put(WhipMove.COWBOY, keys(WhipMove.COWBOY,
                beat(3).grip(0.34, 0.02, -0.5).twist(10).lean(-0.05).squat(0.1),
                beat(6).grip(0.26, 0.12, -0.46).twist(4).lean(-0.08).squat(0.1),
                beat(9).grip(0.34, 0.12, -0.38).twist(-6).lean(-0.1).squat(0.12),
                beat(11.5F).grip(0.36, 0.14, -0.36).twist(14).lean(-0.2).step(-0.2).squat(0.12),
                beat(13).grip(0.3, 0.05, -0.62).twist(8).step(0.2).squat(0.15),
                beat(14.5F).stop().grip(0.22, -0.38, -1.02).twist(-4).lean(0.3).step(0.55).squat(0.2),
                beat(17).stop().grip(0.24, -0.48, -0.96).twist(-4).lean(0.32).step(0.55).squat(0.22),
                beat(21).grip(0.32, -0.55, -0.85).twist(6).lean(0.1).step(0.2)));
    }

    // High to low from either side, low to high from the right, and two crossing lashes in a figure of eight.
    private static void diagonals(Map<WhipMove, Keyframes.Key[]> moves) {
        moves.put(WhipMove.CLEAVE, keys(WhipMove.CLEAVE,
                beat(3).grip(0.55, -0.05, -0.55).twist(26).lean(-0.1).step(-0.15),
                beat(5).stop().grip(0.58, 0.08, -0.42).twist(32).lean(-0.15).step(-0.25).roll(0.06),
                beat(9).grip(0.05, -0.45, -0.95).twist(-10).lean(0.25).step(0.3).squat(0.15),
                beat(10.5F).stop().grip(-0.22, -0.62, -0.78).twist(-26).lean(0.35).step(0.35).squat(0.25).roll(-0.08),
                beat(14).grip(0.1, -0.58, -0.82).twist(-4).lean(0.12).step(0.15).squat(0.1)));
        moves.put(WhipMove.REVERSE_CLEAVE, keys(WhipMove.REVERSE_CLEAVE,
                beat(3).grip(-0.05, -0.05, -0.58).twist(-18).lean(-0.1).step(0.15),
                beat(5).stop().grip(-0.15, 0.06, -0.48).twist(-26).lean(-0.15).step(0.25).roll(-0.06),
                beat(9).grip(0.4, -0.45, -0.95).twist(14).lean(0.25).step(-0.2).squat(0.15),
                beat(10.5F).stop().grip(0.66, -0.6, -0.72).twist(34).lean(0.32).step(-0.3).squat(0.25).roll(0.08),
                beat(14).grip(0.45, -0.58, -0.8).twist(16).lean(0.1).step(-0.05).squat(0.1)));
        moves.put(WhipMove.RISING, keys(WhipMove.RISING,
                beat(3).grip(0.55, -0.7, -0.55).twist(28).lean(0.2).step(-0.1).squat(0.35).wide(0.45),
                beat(5).stop().grip(0.62, -0.78, -0.45).twist(34).lean(0.28).step(-0.15).squat(0.42).wide(0.5)
                        .roll(0.1),
                beat(9).grip(0.05, -0.2, -0.85).twist(-8).lean(-0.1).step(0.2).squat(0.1),
                beat(10.5F).stop().grip(-0.18, 0.02, -0.68).twist(-22).lean(-0.2).step(0.25).roll(-0.08),
                beat(14).grip(0.2, -0.45, -0.8).twist(4).step(0.15)));
        moves.put(WhipMove.FIGURE_EIGHT, keys(WhipMove.FIGURE_EIGHT,
                beat(2.5F).grip(0.5, -0.1, -0.6).twist(22),
                beat(4).stop().grip(0.52, 0.0, -0.52).twist(26).lean(-0.1).step(-0.1),
                beat(7).grip(-0.05, -0.48, -0.92).twist(-14).lean(0.2).step(0.2).squat(0.15),
                beat(8.5F).grip(-0.2, -0.25, -0.75).twist(-20).lean(0.05).step(0.15).squat(0.12),
                beat(10.5F).stop().grip(-0.12, 0.02, -0.55).twist(-18).lean(-0.1).step(0.1),
                beat(13.5F).grip(0.4, -0.48, -0.95).twist(14).lean(0.22).step(0.3).squat(0.15),
                beat(15).stop().grip(0.62, -0.58, -0.75).twist(28).lean(0.28).step(0.3).squat(0.2).roll(0.06),
                beat(19).grip(0.42, -0.56, -0.82).twist(14).lean(0.08).step(0.15)));
    }

    // Down on bent knees, the lash skims the ground at the legs.
    private static void low(Map<WhipMove, Keyframes.Key[]> moves) {
        moves.put(WhipMove.LEG_SWEEP, keys(WhipMove.LEG_SWEEP,
                beat(3).grip(0.6, -0.72, -0.5).twist(30).lean(0.35).step(-0.2).squat(0.55).wide(0.6),
                beat(5).stop().grip(0.72, -0.78, -0.35).twist(40).lean(0.4).step(-0.25).squat(0.65).wide(0.65)
                        .roll(0.08),
                beat(10).grip(-0.1, -0.8, -0.85).twist(-18).lean(0.4).step(0.2).squat(0.65).wide(0.65),
                beat(12).stop().grip(-0.35, -0.78, -0.6).twist(-32).lean(0.38).step(0.3).squat(0.6).wide(0.6)
                        .roll(-0.08),
                beat(15).grip(0.2, -0.6, -0.78).twist(0).lean(0.15).step(0.15).squat(0.3).wide(0.45)));
    }

    // A whole turn of the body with the lash straight out at the waist: it passes everything round.
    private static void round(Map<WhipMove, Keyframes.Key[]> moves) {
        moves.put(WhipMove.SPIN, keys(WhipMove.SPIN,
                beat(3).grip(0.62, -0.42, -0.5).twist(10).squat(0.2).wide(0.5).orbit(-10),
                beat(5).grip(0.68, -0.4, -0.35).twist(16).squat(0.25).wide(0.55).orbit(-45),
                beat(8).grip(0.66, -0.4, -0.35).twist(8).squat(0.25).wide(0.55).orbit(70).lean(0.1),
                beat(11).grip(0.66, -0.4, -0.35).twist(8).squat(0.25).wide(0.55).orbit(230).lean(0.1),
                beat(14).grip(0.6, -0.42, -0.45).twist(4).squat(0.22).wide(0.5).orbit(340).lean(0.08),
                beat(15.5F).stop().grip(0.1, -0.48, -0.85).twist(-18).squat(0.2).wide(0.5).orbit(360).lean(0.1),
                beat(19).grip(0.3, -0.55, -0.82).twist(4).squat(0.1).orbit(360)));
    }
}
