package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class RevolverDuo extends RevolverKeys {
    // Each hand reaches out of a portal a forearm's length behind its wrist, on the line from an unseen shoulder
    // behind the show: the portal glides along after the hand, so no arm ever sweeps across the scene.
    private static final Vec3 SHOULDER = new Vec3(11.5, 7.0, 9.5);
    private static final double SLEEVE = 4.6;
    private static final double[] TRAIL_AT = { 0.0, 3.0, 6.0 };
    private static final double[] TRAIL_SHARE = { 0.45, 0.3, 0.25 };

    public final Stage stage;
    public final HandDuo.Portal aPortal;
    public final HandDuo.Portal bPortal;
    public final HandDuo.Portal topPortal;
    public final HandPose aPose;
    public final HandPose bPose;
    public final HandPose.Place aPlace;
    public final HandPose.Place bPlace;
    public final boolean handsThere;
    public final RevolverGun.Gun gun;

    private RevolverDuo(Stage stage, Hand a, Hand b, double t, RevolverGun.Gun gun) {
        this.stage = stage;
        this.aPortal = sidePortal(stage, t, a);
        this.bPortal = sidePortal(stage, t, b);
        this.topPortal = topPortal(stage, t);
        this.aPose = A_FINGERS.pose(t, a.drag, a.held);
        this.bPose = B_FINGERS.pose(t, b.drag, 0.0);
        this.aPlace = a.place;
        this.bPlace = b.place;
        this.handsThere = t >= ARRIVES && t < HANDS_GONE;
        this.gun = gun;
    }

    public static RevolverDuo at(Vec3 base, int variant, Vec3 aim, double t, double scale) {
        Stage stage = Stage.of(base, variant, scale);
        Vec3 target = stage.local(aim);
        return new RevolverDuo(stage, hand(stage, true, t, target), hand(stage, false, t, target), t,
                RevolverGun.at(stage, aim, t));
    }

    public static Vec3 indexTip(HandPose.Place place, boolean left) {
        return place.at(left ? INDEX_TIP.multiply(-1.0, 1.0, 1.0) : INDEX_TIP);
    }

    public static double proud(double t) {
        return window(t, SHAKE_FROM, SHAKE_FROM + 3, SHAKE_TO - 3, SHAKE_TO);
    }

    public static boolean dips(boolean right, double t) {
        if (right) {
            return t > HAT_REACH + 2 && t < HAT_OUT - 1 || t > PARTS_REACH + 2 && t < PARTS_OUT - 1;
        }
        return t > CLAW_UP + 8 && t < CLAW_OUT - 1;
    }

    private record Hand(HandPose.Place place, Vec3 arm, Vec3 portal, double drag, double held) {
    }

    private static Hand hand(Stage stage, boolean right, double t, Vec3 aim) {
        Vec3 portal = portalAt(right, t, aim);
        if (t < OUT) {
            return coming(stage, right, t, portal, aim);
        }
        if (t >= RETRACT) {
            return going(stage, right, t, portal, aim);
        }
        Vec3[] now = state(right, t, aim);
        double held = now[3].x;
        return world(stage, now[0], now[1], now[2], portal, drag(right, t) * (1.0 - held), held);
    }

    private static Hand world(Stage stage, Vec3 wrist, Vec3 up, Vec3 palm, Vec3 portal, double drag, double held) {
        Vec3 at = stage.point(wrist);
        Vec3 from = stage.point(portal);
        Vec3 arm = at.subtract(from).normalize();
        return new Hand(place(at, arm, stage.dir(up), stage.dir(palm), stage.scale(), 0.0), arm, from, drag, held);
    }

    // Where the hand is and how it is turned, free or on the gun, with how far it holds the gun in the last x.
    private static Vec3[] state(boolean right, double t, Vec3 aim) {
        Vec3[] free = free(right, t, aim);
        Vec3 wrist = free[0];
        Vec3 up = free[1];
        Vec3 palm = free[2];
        double held = 0.0;
        if (right) {
            Vec3[] grip = held(t, aim);
            held = grip == null ? 0.0 : grip[3].x;
            if (held > 0.0) {
                wrist = wrist.lerp(grip[0], held);
                Vec3[] from = Vectors.frame(up, palm);
                Vec3 turn = Vectors.turn(from, Vectors.frame(grip[1], grip[2])).scale(held);
                up = Vectors.turned(from[0], turn);
                palm = Vectors.turned(from[1], turn);
            }
        }
        return new Vec3[] { wrist, up, palm, new Vec3(held, 0.0, 0.0) };
    }

    private static Vec3 portalAt(boolean right, double t, Vec3 aim) {
        double at = Mth.clamp(t, OUT, RETRACT);
        Vec3 anchor = Vec3.ZERO;
        for (int i = 0; i < TRAIL_AT.length; i++) {
            anchor = anchor.add(state(right, Math.max(OUT, at - TRAIL_AT[i]), aim)[0].scale(TRAIL_SHARE[i]));
        }
        Vec3 shoulder = right ? SHOULDER : SHOULDER.multiply(-1.0, 1.0, 1.0);
        return anchor.subtract(anchor.subtract(shoulder).normalize().scale(SLEEVE));
    }

    private static Vec3[] held(double t, Vec3 aim) {
        if (t < GRABS - 6 || t >= TOSS + 6) {
            return null;
        }
        Vec3[] grip;
        Vec3 portal = SHOULDER;
        double held = 1.0;
        if (t < SPIN_FROM) {
            grip = RevolverGun.pistolGrip(RevolverGun.pose(t, aim));
            held = Ease.smoother((t - GRABS + 6.0) / 6.0);
        } else if (t < SPIN_TO) {
            grip = RevolverGun.pistolGrip(RevolverGun.pose(SPIN_FROM, aim));
        } else if (t < SPIN_TO + 5) {
            Vec3[] from = RevolverGun.pistolGrip(RevolverGun.pose(SPIN_FROM, aim));
            Vec3[] to = RevolverGun.barrelGrip(RevolverGun.pose(t, aim), portal);
            double u = Ease.smoother((t - SPIN_TO) / 5.0);
            Vec3[] start = Vectors.frame(from[1], from[2]);
            Vec3 turn = Vectors.turn(start, Vectors.frame(to[1], to[2])).scale(u);
            grip = new Vec3[] { from[0].lerp(to[0], u), Vectors.turned(start[0], turn),
                    Vectors.turned(start[1], turn) };
        } else if (t < TOSS) {
            grip = RevolverGun.barrelGrip(RevolverGun.pose(t, aim), portal);
        } else {
            grip = RevolverGun.barrelGrip(RevolverGun.pose(TOSS, aim), portal);
            held = 1.0 - Ease.smoother((t - TOSS) / 6.0);
        }
        return new Vec3[] { grip[0], grip[1], grip[2], new Vec3(held, 0.0, 0.0) };
    }

    private static Vec3[] free(boolean right, double t, Vec3 aim) {
        Vec3[] key = (right ? A : B).at(t);
        Vec3 wrist = key[0].add(lift(right, t));
        Vec3 up = key[1];
        Vec3 palm = key[2];
        if (!right) {
            double proud = proud(t);
            if (proud > 0.0) {
                double yaw = 0.34 * Math.sin(Math.PI * 2.0 * (t - SHAKE_FROM) / 11.0) * proud;
                double roll = 0.14 * Math.sin(Math.PI * 2.0 * (t - SHAKE_FROM) / 5.5) * proud;
                up = Vectors.spin(up, Vectors.UP, yaw);
                palm = Vectors.spin(palm, Vectors.UP, yaw);
                palm = Vectors.spin(palm, up.normalize(), roll);
            }
            double aiming = window(t, GUN_FORM - 2, GUN_FORM + 5, BLOW - 7, BLOW);
            Vec3 way = aim.subtract(wrist);
            if (aiming > 0.0 && way.lengthSqr() > 1.0E-4) {
                Vec3 aimUp = steady(way);
                Vec3 aimPalm = palmFor(false, aimUp, Vectors.UP);
                Vec3 lift = aimUp.cross(Vectors.UP).normalize();
                double kick = 0.0;
                for (int pew : PEWS) {
                    kick += 0.3 * kick(t - pew, 0.9);
                }
                aimUp = Vectors.spin(aimUp, lift, kick);
                aimPalm = Vectors.spin(aimPalm, lift, kick);
                Vec3[] from = Vectors.frame(up, palm);
                Vec3 turn = Vectors.turn(from, Vectors.frame(aimUp, aimPalm)).scale(aiming);
                up = Vectors.turned(from[0], turn);
                palm = Vectors.turned(from[1], turn);
            }
        }
        return new Vec3[] { wrist, up, palm };
    }

    private static Vec3 lift(boolean right, double t) {
        double p = right ? 0.0 : 2.1;
        double side = right ? 1.0 : -1.0;
        double breathe = window(t, OUT, OUT + 6, HAT_REACH - 6, HAT_REACH) + window(t, HAT_TAP + 8,
                HAT_TAP + 14, PARTS_REACH - 8, PARTS_REACH - 2) + window(t, WHOLE + 4, WHOLE + 10, GRABS - 10,
                        GRABS - 6) + window(t, WIPED + 2, WIPED + 6, RETRACT - 4, RETRACT);
        if (!right) {
            breathe = window(t, OUT, OUT + 6, HAT_OVER - 12, HAT_OVER - 6) + window(t, HAT_THROW + 8,
                    HAT_THROW + 12, CLAW_UP - 6, CLAW_UP) + window(t, AIMS + 4, AIMS + 10, WIPE - 6, WIPE - 2)
                    + window(t, WIPED + 2, WIPED + 6, RETRACT - 4, RETRACT);
        }
        Vec3 lift = new Vec3(0.07 * Math.sin(0.21 * t + p), 0.12 * Math.sin(0.15 * t + 0.4 + p)
                + 0.04 * Math.sin(0.33 * t + 2.0 + p), 0.06 * Math.sin(0.18 * t + 2.6 + p)).scale(breathe);
        double warm = window(t, OUT + 2, OUT + 6, WIGGLE_TO - 8, WIGGLE_TO - 2);
        if (warm > 0.0) {
            double round = 0.32 * (t - OUT) + p;
            lift = lift.add(new Vec3(0.25 * Math.cos(round), 0.25 * Math.sin(round), 0.0).scale(warm));
        }
        double conduct = window(t, CONDUCT, CONDUCT + 4, WHOLE, WHOLE + 4);
        if (conduct > 0.0) {
            double beat = Math.PI * 2.0 * (t - CONDUCT) / 14.0 + p;
            lift = lift.add(new Vec3(side * 0.3 * Math.sin(beat), 0.25 * Math.abs(Math.sin(beat)), 0.0)
                    .scale(conduct));
        }
        if (right) {
            for (int pew : PEWS) {
                lift = lift.add(new Vec3(0.1, 0.15, 0.08).scale(kick(t - pew - 0.5, 0.9)));
            }
        } else {
            lift = lift.add(0.0, 0.16 * Math.abs(Math.sin(Math.PI * 2.0 * (t - SHAKE_FROM) / 11.0)) * proud(t), 0.0);
            double claw = window(t, CLAW_UP + 2, CLAW_UP + 4, CLAW_CLAMP + 4, CLAW_CLAMP + 8);
            lift = lift.add(new Vec3(0.04 * Math.sin(1.7 * t), 0.03 * Math.sin(1.3 * t + 1.0), 0.0).scale(claw));
        }
        double limp = window(t, TIRED + 4, TIRED + 8, HIGH_FIVE - 12, HIGH_FIVE - 8);
        if (limp > 0.0) {
            lift = lift.add(new Vec3(side * 0.28 * Math.sin(0.7 * t + p), 0.1 * Math.sin(1.4 * t + p), 0.0)
                    .scale(limp));
        }
        return lift;
    }

    private static double drag(boolean right, double t) {
        Track track = right ? A : B;
        Vec3[] now = track.at(t);
        Vec3[] then = track.at(t - 1.5);
        Vec3 moved = now[0].add(lift(right, t)).subtract(then[0].add(lift(right, t - 1.5))).scale(1.0 / 1.5);
        double along = moved.dot(now[2].normalize()) / 0.35;
        return -0.1 * along / Math.sqrt(1.0 + along * along) * window(t, OUT, OUT + 3.0, RETRACT - 3.0, RETRACT);
    }

    private static Hand coming(Stage stage, boolean right, double t, Vec3 portal, Vec3 aim) {
        Vec3[] rest = state(right, OUT, aim);
        Vec3 arm = rest[0].subtract(portal).normalize();
        double reach = rest[0].distanceTo(portal);
        double out;
        if (t < ARRIVES + 1) {
            out = -7.6;
        } else if (t < ARRIVES + 6.5) {
            out = hermite(t, ARRIVES + 1, -7.6, 0.0, ARRIVES + 6.5, -0.4, 1.6);
        } else if (t < ARRIVES + 10.5) {
            out = hermite(t, ARRIVES + 6.5, -0.4, 1.6, ARRIVES + 10.5, reach + 0.55, 0.0);
        } else {
            out = hermite(t, ARRIVES + 10.5, reach + 0.55, 0.0, OUT, reach, 0.0);
        }
        Vec3 wrist = portal.add(arm.scale(out));
        Vec3 straight = rest[2].subtract(arm.scale(rest[2].dot(arm)));
        double bend = Ease.smoother((t - ARRIVES - 5.0) / (OUT - ARRIVES - 5.0));
        Vec3 up = arm.lerp(rest[1], bend);
        Vec3 palm = straight.lerp(rest[2], bend);
        double screw = (right ? 0.9 : -0.9) * (1.0 - Ease.smoother((t - ARRIVES) / (OUT - ARRIVES - 2.0)));
        up = Vectors.spin(up, arm, screw);
        palm = Vectors.spin(palm, arm, screw);
        return world(stage, wrist, up, palm, portal, 0.0, 0.0);
    }

    private static Hand going(Stage stage, boolean right, double t, Vec3 portal, Vec3 aim) {
        Vec3[] last = state(right, RETRACT, aim);
        Vec3 arm = last[0].subtract(portal).normalize();
        double reach = last[0].distanceTo(portal);
        double in;
        if (t < RETRACT + 2.5) {
            in = hermite(t, RETRACT, reach, 0.0, RETRACT + 2.5, reach + 0.35, 0.0);
        } else {
            in = Mth.lerp(late((t - RETRACT - 2.5) / (HANDS_GONE - 2.0 - RETRACT - 2.5)), reach + 0.35, -7.6);
        }
        Vec3 wrist = portal.add(arm.scale(in));
        double bend = Ease.smoother((t - RETRACT - 1.0) / 6.0);
        Vec3 straight = last[2].subtract(arm.scale(last[2].dot(arm)));
        Vec3 up = last[1].lerp(arm, bend);
        Vec3 palm = last[2].lerp(straight, bend);
        return world(stage, wrist, up, palm, portal, 0.0, 0.0);
    }

    private static HandDuo.Portal sidePortal(Stage stage, double t, Hand hand) {
        Vec3 arm = hand.arm;
        Vec3 a = Vectors.UP.cross(arm);
        a = a.lengthSqr() < 1.0E-6 ? stage.side() : a.normalize();
        Vec3 b = arm.cross(a).normalize();
        double open = Ease.spring(t - ARRIVES, 1.05, 0.62) * shut((t - HANDS_GONE + 5.0) / 5.0);
        return new HandDuo.Portal(hand.portal, arm, a, b, SIDE_RADIUS * stage.scale(), open);
    }

    private static HandDuo.Portal topPortal(Stage stage, double t) {
        Vec3 normal = stage.dir(TOP_NORMAL).normalize();
        Vec3 a = stage.side();
        double open = Ease.spring(t - TOP_OPENS, 0.9, 0.62) * shut((t - TOP_SHUT) / 2.5);
        return new HandDuo.Portal(stage.point(TOP), normal, a, normal.cross(a), TOP_RADIUS * stage.scale(), open);
    }
}
