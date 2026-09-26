package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

public final class HandDuo extends HandDuoScript {
    public record Portal(Vec3 center, Vec3 normal, Vec3 a, Vec3 b, double radius, double open) {
    }

    public final Portal leftPortal;
    public final Portal rightPortal;
    public final Portal axePortal;
    public final HandPose leftPose;
    public final HandPose rightPose;
    public final HandPose.Place leftPlace;
    public final HandPose.Place rightPlace;
    public final boolean handsThere;
    public final Vec3 axeEnd;
    public final Vec3 axeUp;
    public final Vec3 axeFace;
    public final boolean axeThere;
    public final boolean axeCut;
    public final double axeBreak;

    private HandDuo(Portal leftPortal, Portal rightPortal, Portal axePortal, HandPose leftPose, HandPose rightPose,
            HandPose.Place leftPlace, HandPose.Place rightPlace, double t, Axe axe) {
        this.leftPortal = leftPortal;
        this.rightPortal = rightPortal;
        this.axePortal = axePortal;
        this.leftPose = leftPose;
        this.rightPose = rightPose;
        this.leftPlace = leftPlace;
        this.rightPlace = rightPlace;
        this.handsThere = t >= ARRIVES && t < HANDS_GONE;
        this.axeEnd = axe.end();
        this.axeUp = axe.up();
        this.axeFace = axe.face();
        this.axeBreak = Mth.clamp((t - AXE_BREAKS) / BREAK_TICKS, 0.0, 1.0);
        this.axeThere = drawnOut(t) > 0.0 && this.axeBreak < 1.0;
        this.axeCut = this.axeThere && drawnOut(t) < DRAWN;
    }

    public static HandDuo at(Vec3 base, int variant, Vec3 aim, double t, double scale) {
        Layout layout = Layout.of(base, variant, aim, scale);
        Axe axe = axe(layout, t);
        Hand left = hand(layout, false, t, axe);
        Hand right = hand(layout, true, t, axe);
        return new HandDuo(sidePortal(layout, false, t, left.arm), sidePortal(layout, true, t, right.arm),
                axePortal(layout, t), LEFT_FINGERS.pose(t, left.drag, left.held),
                RIGHT_FINGERS.pose(t, right.drag, right.held), left.place, right.place, t, axe);
    }

    public static Vec3 strike(Vec3 base, int variant, Vec3 aim, double scale) {
        Layout layout = Layout.of(base, variant, aim, scale);
        return layout.point(layout.strike());
    }

    public static Axe axe(Vec3 base, int variant, Vec3 aim, double t, double scale) {
        return axe(Layout.of(base, variant, aim, scale), t);
    }

    public record Axe(Vec3 end, Vec3 up, Vec3 face, double tilt) {
    }

    private static double drawnOut(double t) {
        double from = -1.5;
        if (t < AXE_OPENS) {
            return from + 0.3 * (t - AXE_OPENS);
        }
        if (t < GRAB) {
            return hermite(t, AXE_OPENS, from, 0.3, GRAB, SLID, 0.1);
        }
        if (t < AXE_FREE) {
            return hermite(t, GRAB, SLID, 0.1, AXE_FREE, DRAWN, 0.55);
        }
        if (t < LOCKED_FROM) {
            return hermite(t, AXE_FREE, DRAWN, 0.55, LOCKED_FROM, DRAWN_REST, 0.0);
        }
        return DRAWN_REST;
    }

    private static Axe axe(Layout layout, double t) {
        Vec3 grips;
        double tilt;
        double roll = 0.0;
        Vec3 drawn = AXE_PORTAL.add(0.0, 0.0, GRIPS - drawnOut(Math.min(t, LOCKED_FROM)));
        if (t < LOCKED_FROM) {
            grips = drawn;
            tilt = t < AXE_FREE - 1 ? 0.0 : hermite(t, AXE_FREE - 1, 0.0, 0.0, LOCKED_FROM + 3, DIP_TILT, 0.0);
        } else {
            Vec3 raised = layout.raised();
            roll = layout.roll() * Ease.smoother((t - LOCKED_FROM - 2.0) / 9.0);
            double dipAt = LOCKED_FROM + 3.5;
            Vec3 dipped = drawn.add(raised.subtract(drawn).multiply(0.12, 0.0, 0.12)).add(0.0, -DIP, 0.0);
            if (t < dipAt) {
                grips = hermite(t, LOCKED_FROM, drawn, Vec3.ZERO, dipAt, dipped,
                        raised.subtract(drawn).multiply(0.1, 0.0, 0.1));
            } else if (t < RAISED) {
                double u = (t - dipAt) / (RAISED - dipAt);
                Vec3 flat = hermite(t, dipAt, dipped, raised.subtract(drawn).multiply(0.1, 0.0, 0.1), RAISED, raised,
                        Vec3.ZERO);
                grips = new Vec3(flat.x, Mth.lerp(early(u), dipped.y, raised.y), flat.z);
            } else if (t < CHOP_FROM) {
                grips = raised;
            } else {
                double u = Math.min(1.0, (t - CHOP_FROM) / (IMPACT - CHOP_FROM));
                grips = raised.lerp(layout.struck(), Ease.smoother(u / HANDS_DOWN));
            }
            if (t < LOCKED_FROM + 3) {
                tilt = hermite(t, AXE_FREE - 1, 0.0, 0.0, LOCKED_FROM + 3, DIP_TILT, 0.0);
            } else if (t < RAISED) {
                tilt = Mth.lerp(early((t - LOCKED_FROM - 3) / (RAISED - LOCKED_FROM - 3)), DIP_TILT, RAISE_TILT);
            } else if (t < CHOP_FROM) {
                tilt = RAISE_TILT;
            } else {
                double u = Math.min(1.0, (t - CHOP_FROM) / (IMPACT - CHOP_FROM));
                tilt = Mth.lerp(chop(u), RAISE_TILT, Math.PI + layout.lean());
            }
            double hang = window(t, RAISED - 1.5, RAISED + 0.5, CHOP_FROM - 0.5, CHOP_FROM + 1.0);
            tilt += 0.012 * Math.sin(2.4 * t) * hang;
            grips = grips.add(0.0, 0.05 * Math.sin(3.1 * t + 1.0) * hang, 0.0);
        }
        Vec3 away = layout.way();
        Vec3 up = away.scale(Math.cos(tilt)).add(Vectors.UP.scale(Math.sin(tilt)));
        Vec3 face = away.scale(-Math.sin(tilt)).add(Vectors.UP.scale(Math.cos(tilt))).scale(Math.cos(roll))
                .add(layout.side().scale(Math.sin(roll)));
        Vec3 end = layout.point(grips).subtract(up.scale(GRIPS * layout.scale()));
        if (t > IMPACT) {
            double u = t - IMPACT;
            double quiver = 0.05 * wobble(u, 1.9, 0.3, 0.7) * (1.0 - Ease.smoother((u - 7.0) / 5.0));
            if (quiver != 0.0) {
                Vec3 axis = layout.side();
                Vec3 bite = layout.point(layout.strike());
                end = bite.add(Vectors.spin(end.subtract(bite), axis, quiver));
                up = Vectors.spin(up, axis, quiver);
                face = Vectors.spin(face, axis, quiver);
            }
        }
        return new Axe(end, up, face, tilt);
    }

    private record Hand(HandPose.Place place, Vec3 arm, double drag, double held) {
    }

    private static Hand hand(Layout layout, boolean right, double t, Axe axe) {
        Vec3 portal = layout.point(portal(right));
        double scale = layout.scale();
        if (t < OUT) {
            return coming(layout, right, t, portal);
        }
        if (t >= RETRACT) {
            return going(layout, right, t, portal);
        }
        Vec3[] free = free(layout, right, t);
        Vec3 wrist = free[0];
        Vec3 up = free[1];
        Vec3 palm = free[2];
        double held = Ease.smoother((t - GRAB + 8.0) / 8.0) * (1.0 - Ease.smoother((t - RELEASE - LET_GO) / 5.0));
        if (held > 0.0) {
            Vec3[] grip = grip(layout, right, t, axe, portal);
            wrist = wrist.lerp(grip[0], held);
            Vec3[] from = Vectors.frame(up, palm);
            Vec3 turn = Vectors.turn(from, Vectors.frame(grip[1], grip[2])).scale(held);
            up = Vectors.turned(from[0], turn);
            palm = Vectors.turned(from[1], turn);
        }
        Vec3 arm = wrist.subtract(portal).normalize();
        return new Hand(place(wrist, arm, up, palm, scale, twist(layout, t, axe, arm)), arm, drag(right, t), held);
    }

    private static double twist(Layout layout, double t, Axe axe, Vec3 arm) {
        double wound = Ease.smoother((t - AXE_OPENS - 2.0) / (GRAB - 6.0 - AXE_OPENS - 2.0))
                * (1.0 - Ease.smoother((t - RELEASE) / (THUMBS - RELEASE)));
        if (wound <= 0.0) {
            return 0.0;
        }
        double middle = (Math.PI + layout.lean()) * 0.5;
        // Only a share of the turn: a forearm that took all of it would corkscrew in its portal.
        return wound * WRIST_SHARE * (axe.tilt() - middle) * arm.dot(layout.side());
    }

    private static Vec3[] free(Layout layout, boolean right, double t) {
        Vec3[] key = (right ? RIGHT : LEFT).at(t);
        Vec3 wrist = layout.point(key[0].add(lift(right, t)));
        Vec3 up = layout.dir(key[1]);
        Vec3 palm = layout.dir(key[2]);
        Vec3 side = layout.side();
        double swing = roll(t);
        if (swing != 0.0) {
            double tip = (right ? 0.2 : -0.2) * swing * Math.sin(rollTurn(t) - Math.PI * 0.5);
            up = Vectors.spin(up, side, tip);
            palm = Vectors.spin(palm, side, tip);
        }
        Vec3 face = palm.normalize();
        if (right) {
            double jolt = -0.12 * kick(t - SNAP, 0.9);
            Vec3 across = face.cross(up).normalize();
            up = Vectors.spin(up, across, jolt);
            palm = Vectors.spin(palm, across, jolt);
        } else {
            double proud = 0.12 * wobble(t - OK, 0.9, 0.18, 1.5) * (1.0 - Ease.smoother((t - OK - 11.0) / 5.0));
            up = Vectors.spin(up, face, proud);
        }
        double nod = 0.16 * wobble(t - THUMBS - 0.5 - (right ? 0.0 : 0.4), 0.8, 0.22, 1.5)
                * (1.0 - Ease.smoother((t - THUMBS - 9.0) / 5.0));
        up = Vectors.spin(up, side, nod);
        palm = Vectors.spin(palm, side, nod);
        return new Vec3[] { wrist, up, palm };
    }

    private static Vec3 lift(boolean right, double t) {
        double p = right ? 0.0 : 2.1;
        double breathe = window(t, OUT, OUT + 6, GRAB - 10, GRAB - 4) + window(t, RELEASE + 4, RELEASE + 10,
                RETRACT - 4, RETRACT);
        Vec3 lift = new Vec3(0.07 * Math.sin(0.31 * t + p), 0.14 * Math.sin(0.19 * t + 0.4 + p)
                + 0.05 * Math.sin(0.47 * t + 2.0 + p), 0.06 * Math.sin(0.26 * t + 2.6 + p)).scale(breathe);
        if (right) {
            lift = lift.add(new Vec3(0.05, -0.22, -0.15).scale(kick(t - SNAP, 0.9)));
        } else {
            lift = lift.add(new Vec3(0.0, 0.22, -0.1).scale(kick(t - OK, 1.4)));
        }
        double swing = roll(t);
        if (swing != 0.0) {
            double turn = rollTurn(t);
            lift = lift.add(new Vec3(0.0, Math.sin(turn), Math.cos(turn)).scale((right ? 1.0 : -1.0) * swing
                    * ROLL_RADIUS));
        }
        return lift.add(new Vec3(0.0, 0.4, -0.12).scale(kick(t - THUMBS - (right ? 0.0 : 0.4), 1.2)));
    }

    private static double roll(double t) {
        return window(t, ROLL_FROM, ROLL_FROM + 3.5, ROLL_TO - 3.5, ROLL_TO);
    }

    private static double rollTurn(double t) {
        return Math.PI * 0.5 + ROLL_TURNS * Ease.smoother((t - ROLL_FROM) / (ROLL_TO - ROLL_FROM));
    }

    private static double drag(boolean right, double t) {
        Track track = right ? RIGHT : LEFT;
        Vec3[] now = track.at(t);
        Vec3[] then = track.at(t - 1.5);
        Vec3 moved = now[0].add(lift(right, t)).subtract(then[0].add(lift(right, t - 1.5))).scale(1.0 / 1.5);
        double along = moved.dot(now[2].normalize()) / 0.35;
        return -0.1 * along / Math.sqrt(1.0 + along * along) * window(t, OUT, OUT + 3.0, RETRACT - 3.0, RETRACT);
    }

    private static Hand coming(Layout layout, boolean right, double t, Vec3 portal) {
        Vec3[] rest = free(layout, right, OUT);
        Vec3 arm = rest[0].subtract(portal).normalize();
        double reach = rest[0].distanceTo(portal) / layout.scale();
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
        Vec3 wrist = portal.add(arm.scale(out * layout.scale()));
        Vec3 straight = rest[2].subtract(arm.scale(rest[2].dot(arm)));
        double bend = Ease.smoother((t - ARRIVES - 5.0) / (OUT - ARRIVES - 5.0));
        Vec3 up = arm.lerp(rest[1], bend);
        Vec3 palm = straight.lerp(rest[2], bend);
        double screw = (right ? 0.9 : -0.9) * (1.0 - Ease.smoother((t - ARRIVES) / (OUT - ARRIVES - 2.0)));
        up = Vectors.spin(up, arm, screw);
        palm = Vectors.spin(palm, arm, screw);
        return new Hand(place(wrist, arm, up, palm, layout.scale(), 0.0), arm, 0.0, 0.0);
    }

    private static Hand going(Layout layout, boolean right, double t, Vec3 portal) {
        Vec3[] last = free(layout, right, RETRACT);
        Vec3 arm = last[0].subtract(portal).normalize();
        double reach = last[0].distanceTo(portal) / layout.scale();
        double in;
        if (t < RETRACT + 2.5) {
            in = hermite(t, RETRACT, reach, 0.0, RETRACT + 2.5, reach + 0.35, 0.0);
        } else {
            in = Mth.lerp(late((t - RETRACT - 2.5) / (HANDS_GONE - 2.0 - RETRACT - 2.5)), reach + 0.35, -7.6);
        }
        Vec3 wrist = portal.add(arm.scale(in * layout.scale()));
        double bend = Ease.smoother((t - RETRACT - 1.0) / 6.0);
        Vec3 straight = last[2].subtract(arm.scale(last[2].dot(arm)));
        Vec3 up = last[1].lerp(arm, bend);
        Vec3 palm = last[2].lerp(straight, bend);
        return new Hand(place(wrist, arm, up, palm, layout.scale(), 0.0), arm, 0.0, 0.0);
    }

    private static Vec3[] grip(Layout layout, boolean right, double t, Axe axe, Vec3 portal) {
        double scale = layout.scale();
        Vec3 hole = axe.end().add(axe.up().scale((right ? GRIP_HIGH : GRIP_LOW) * scale));
        Vec3 thumbSide = right ? axe.up().scale(-1.0) : axe.up();
        double shake = 0.07 * wobble(t - IMPACT - (right ? 0.0 : 0.3), 2.2, 0.35, 0.8)
                * (1.0 - Ease.smoother((t - IMPACT - 8.0) / 4.0));
        double off = (HOLD_CLEAR * (1.0 - Ease.smoother((t - GRAB + 6.0) / 4.0))
                + DROP_OFF * Ease.smoother((t - RELEASE + 0.5) / 2.5)) * scale;
        double back = SLIDE_OFF * Ease.smoother((t - RELEASE - 0.5) / 4.0) * scale;
        Vec3 toward = hole.subtract(portal);
        Vec3[] fist = new Vec3[0];
        // Each pass feeds the wrist back into the next: a few passes settle it onto a stable fit.
        for (int i = 0; i < 4; i++) {
            Vec3 up = Vectors.spin(toward.subtract(axe.up().scale(toward.dot(axe.up()))).normalize(), axe.up(), shake);
            Vec3 palm = up.cross(thumbSide);
            Vec3 wrist = hole.subtract(up.scale(FIST_HOLE.y * scale + back))
                    .subtract(palm.scale(FIST_HOLE.z * scale + off));
            fist = new Vec3[] { wrist, up, palm };
            toward = wrist.subtract(portal);
        }
        return fist;
    }

    private static Vec3 portal(boolean right) {
        return right ? RIGHT_PORTAL : RIGHT_PORTAL.multiply(-1.0, 1.0, 1.0);
    }

    private static Vec3 hover(boolean right) {
        return right ? HOVER : HOVER.multiply(-1.0, 1.0, 1.0);
    }

    private static Portal sidePortal(Layout layout, boolean right, double t, Vec3 arm) {
        Vec3 center = layout.point(portal(right));
        Vec3 rest = layout.dir(hover(right).subtract(portal(right))).normalize();
        Vec3 a0 = Vectors.UP.cross(rest).normalize();
        Vec3 axis = rest.cross(arm);
        double cos = rest.dot(arm);
        Vec3 a = a0.scale(cos).add(axis.cross(a0)).add(axis.scale(axis.dot(a0) / (1.0 + cos))).normalize();
        Vec3 b = arm.cross(a).normalize();
        double open = Ease.spring(t - ARRIVES, 1.05, 0.62) * shut((t - HANDS_GONE + 5.0) / 5.0);
        return new Portal(center, arm, a, b, SIDE_RADIUS * layout.scale(), open);
    }

    private static Portal axePortal(Layout layout, double t) {
        Vec3 normal = layout.dir(new Vec3(0.0, 0.0, -1.0));
        Vec3 a = layout.side();
        double open = Ease.spring(t - AXE_OPENS, 0.9, 0.62) * shut((t - AXE_FREE) / 3.5);
        return new Portal(layout.point(AXE_PORTAL), normal, a, normal.cross(a), AXE_RADIUS * layout.scale(), open);
    }
}
