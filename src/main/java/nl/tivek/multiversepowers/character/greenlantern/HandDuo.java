package nl.tivek.multiversepowers.character.greenlantern;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * The pair of giant hands with the axe (the move {@link HandPose#AXE} of the Giant Hands), worked out alike by the
 * server (when and where it strikes, where its sounds and sparks go) and by every client (how it looks), from nothing
 * but where it was called, the way it is laid out, the spot its axe strikes and how long ago it was called.
 *
 * <p>Two portals of the ring's light burst open beyond the creature, one on either side of it, and a giant right hand
 * (with the ring) and left hand push out of them fingertips first, their fingers wriggling, a little corkscrewed, and
 * come to rest in front of them: the hands of an invisible giant standing behind the creature, facing the one who
 * called them. Their fingers ripple up and down like a pianist's warming up. The right hand rises, turns its palm to
 * him, presses its middle finger to its thumb and snaps its fingers, jolting with it; the left answers with the OK
 * sign and a proud little wobble. Then they glide together, palms to each other, and roll round each other like
 * winding wool, their fingers crawling, and fling apart palms up as if conjuring: a third portal bursts open beyond
 * them. A giant axe slides out of it pommel first; both hands reach for it, close on its haft one finger after another,
 * and draw it out like a sword from its sheath. They dip, heave it up and back, hang there a beat trembling with its
 * weight, and chop it down over the top onto the creature, the middle of its edge biting into the ground where the
 * creature stands. They leave it stuck there quivering, let go, rise and give him a thumbs up, then pull back into
 * their portals, which pop shut, and the axe left behind breaks into solid pieces.
 *
 * <p>Everything is laid out along the flat way from the caster to the creature ({@link HandPose#axeWay}): the portals
 * and all the hands do before the raise stay where they are while the spot the axe strikes follows the creature; from
 * {@link #LOCKED_FROM} on that spot stays put. Every value moves smoothly: nothing jumps, and nothing starts or stops
 * moving with a jolt.
 */
public final class HandDuo extends HandDuoScript {
    /** A portal of the ring's light: a round opening facing {@code normal} (what comes out comes out that way). */
    public record Portal(Vec3 center, Vec3 normal, Vec3 a, Vec3 b, double radius, double open) {
    }

    /** Where the LEFT hand comes out. */
    public final Portal leftPortal;
    /** Where the RIGHT hand comes out. */
    public final Portal rightPortal;
    /** Where the axe comes out. */
    public final Portal axePortal;
    /** The left hand's fingers: curl, hook and spread (the rest unused). */
    public final HandPose leftPose;
    /** The right hand's fingers: curl, hook and spread (the rest unused). */
    public final HandPose rightPose;
    /**
     * Where the left hand is. Its right points from its little finger's side to its THUMB (it is drawn as the right
     * hand's shapes mirrored), and its forearm runs out of its portal: arm is the way from the portal's middle to the
     * wrist.
     */
    public final HandPose.Place leftPlace;
    /** Where the right hand is (the ring on its middle finger); its forearm runs out of its portal as the left's. */
    public final HandPose.Place rightPlace;
    /** True from ARRIVES until HANDS_GONE: the hands are drawn. */
    public final boolean handsThere;
    /** The end of the axe's pommel, in the world. */
    public final Vec3 axeEnd;
    /** One long: along the haft, pommel to head. */
    public final Vec3 axeUp;
    /** One long, square to axeUp: the way the blade's edge faces. */
    public final Vec3 axeFace;
    /** True from the moment its pommel first shows until it has broken away. */
    public final boolean axeThere;
    /** True while it still comes out of its portal: draw it cut at the axe portal's plane. */
    public final boolean axeCut;
    /** 0 whole .. 1 broken away (from AXE_BREAKS). */
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

    /**
     * The pair and its axe {@code t} ticks after it was called.
     *
     * @param base    the ground where it was called (the creature's feet)
     * @param variant its variant: the way it is laid out (see {@link HandPose#axeVariant})
     * @param aim     the spot the axe strikes (only its x and z count; kept within REACH of base)
     * @param scale   its size (1: the size it is made at)
     */
    public static HandDuo at(Vec3 base, int variant, Vec3 aim, double t, double scale) {
        Layout layout = Layout.of(base, variant, aim, scale);
        Axe axe = axe(layout, t);
        Hand left = hand(layout, false, t, axe);
        Hand right = hand(layout, true, t, axe);
        return new HandDuo(sidePortal(layout, false, t, left.arm), sidePortal(layout, true, t, right.arm),
                axePortal(layout, t), LEFT_FINGERS.pose(t, left.drag, left.held),
                RIGHT_FINGERS.pose(t, right.drag, right.held), left.place, right.place, t, axe);
    }

    /** The middle of the blade's edge as it strikes (at IMPACT), in the ground at the (clamped) aim. */
    public static Vec3 strike(Vec3 base, int variant, Vec3 aim, double scale) {
        Layout layout = Layout.of(base, variant, aim, scale);
        return layout.point(layout.strike());
    }

    /**
     * Only the axe of the pair {@code t} ticks after it was called, just where {@link #at} puts it (axeEnd, axeUp and
     * axeFace), without working out the hands: for what looks back at where the axe was a moment ago.
     */
    public static Axe axe(Vec3 base, int variant, Vec3 aim, double t, double scale) {
        return axe(Layout.of(base, variant, aim, scale), t);
    }

    // ---- The axe ----

    /**
     * Where the axe is: the end of its pommel, the way along its haft (pommel to head), the way its edge faces, and how
     * far its haft is tipped up from level with its head away from the caster (radians, before it quivers).
     */
    public record Axe(Vec3 end, Vec3 up, Vec3 face, double tilt) {
    }

    /**
     * How far the axe is out of its portal (from the end of its pommel): it slides out slowly, pommel first, until the
     * hands close on it, is drawn out by them until its head comes free and comes to rest in their hands. Below 0 it is
     * still inside, sliding towards the opening.
     */
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

    /**
     * The axe at {@code t}: out of its portal level, head last and edge up; carried by the hands (the middle of its
     * grips, how far it leans up from level with its head away from the caster, and how far it is rolled about its
     * haft so its edge reaches a strike off to the side) through the dip, the heave, the hang and the chop; and left
     * where it struck, quivering a moment. Its haft always stays square to the caster's right, as the forearms come
     * from there.
     */
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
            // Trembling with its weight at the top.
            double hang = window(t, RAISED - 1.5, RAISED + 0.5, CHOP_FROM - 0.5, CHOP_FROM + 1.0);
            tilt += 0.012 * Math.sin(2.4 * t) * hang;
            grips = grips.add(0.0, 0.05 * Math.sin(3.1 * t + 1.0) * hang, 0.0);
        }
        Vec3 away = layout.way();
        Vec3 up = away.scale(Math.cos(tilt)).add(Vectors.UP.scale(Math.sin(tilt)));
        // Its edge leads the way it swings up and over, rolled towards the caster's right by roll.
        Vec3 face = away.scale(-Math.sin(tilt)).add(Vectors.UP.scale(Math.cos(tilt))).scale(Math.cos(roll))
                .add(layout.side().scale(Math.sin(roll)));
        Vec3 end = layout.point(grips).subtract(up.scale(GRIPS * layout.scale()));
        if (t > IMPACT) {
            // Stuck in the ground it quivers a moment, turning about where it bites, and is still again before the
            // hands let go.
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

    // ---- The hands ----

    /** A hand worked out: where it is, the way its forearm runs, how its fingers are dragged and how hard it holds. */
    private record Hand(HandPose.Place place, Vec3 arm, double drag, double held) {
    }

    /**
     * One hand at {@code t}: pushing out of its portal, free in the air (its keys, bobbing, snapping, rolling...),
     * closing on and carrying the haft, and pulling back into its portal.
     */
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
            // Turned the shortest way from how it is in the air to how it holds on, never flipping through.
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

    /**
     * How far a hand's wrist is twisted: how far the hand is turned round its forearm past the forearm itself (radians,
     * by the right-hand rule about the way the forearm runs out of its portal). Holding the axe, a fist turns with its
     * haft, well over half a turn from grabbing it to chopping, and its forearm comes in from the side, nearly along the
     * line the haft swings round: a forearm that took all of that turn would corkscrew in its portal. So the wrist takes
     * {@link #WRIST_SHARE} of it: as the hand turns over to reach for the haft its forearm already turns on ahead of
     * it (the wrist twisted as far one way at the grab as the other way at the blow), as the axe swings the forearm
     * takes only the rest of the turn, and once the hand lets go the wrist straightens out again.
     */
    private static double twist(Layout layout, double t, Axe axe, Vec3 arm) {
        double wound = Ease.smoother((t - AXE_OPENS - 2.0) / (GRAB - 6.0 - AXE_OPENS - 2.0))
                * (1.0 - Ease.smoother((t - RELEASE) / (THUMBS - RELEASE)));
        if (wound <= 0.0) {
            return 0.0;
        }
        double middle = (Math.PI + layout.lean()) * 0.5;
        return wound * WRIST_SHARE * (axe.tilt() - middle) * arm.dot(layout.side());
    }

    /** The hand in the air (not in its portal, not holding on): its wrist, the way its fingers point, its palm. */
    private static Vec3[] free(Layout layout, boolean right, double t) {
        Vec3[] key = (right ? RIGHT : LEFT).at(t);
        Vec3 wrist = layout.point(key[0].add(lift(right, t)));
        Vec3 up = layout.dir(key[1]);
        Vec3 palm = layout.dir(key[2]);
        Vec3 side = layout.side();
        // Rolling round the other hand, it tips with its swing.
        double swing = roll(t);
        if (swing != 0.0) {
            double tip = (right ? 0.2 : -0.2) * swing * Math.sin(rollTurn(t) - Math.PI * 0.5);
            up = Vectors.spin(up, side, tip);
            palm = Vectors.spin(palm, side, tip);
        }
        Vec3 face = palm.normalize();
        if (right) {
            // The snap flicks its fingers down towards its palm, turning about the way across the hand.
            double jolt = -0.12 * kick(t - SNAP, 0.9);
            Vec3 across = face.cross(up).normalize();
            up = Vectors.spin(up, across, jolt);
            palm = Vectors.spin(palm, across, jolt);
        } else {
            // A proud little wobble with its OK sign, turning about the way its palm faces.
            double proud = 0.12 * wobble(t - OK, 0.9, 0.18, 1.5) * (1.0 - Ease.smoother((t - OK - 11.0) / 5.0));
            up = Vectors.spin(up, face, proud);
        }
        // A nod with the thumbs up.
        double nod = 0.16 * wobble(t - THUMBS - 0.5 - (right ? 0.0 : 0.4), 0.8, 0.22, 1.5)
                * (1.0 - Ease.smoother((t - THUMBS - 9.0) / 5.0));
        up = Vectors.spin(up, side, nod);
        palm = Vectors.spin(palm, side, nod);
        return new Vec3[] { wrist, up, palm };
    }

    /**
     * How far the wrist of a hand in the air is moved off its keys, at scale 1 in the pair's terms: breathing while it
     * hovers, the jolt of the snap, the pop of the OK sign, rolling round the other hand and the pop of the thumbs up.
     */
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

    /** How far out the hands swing as they roll round each other: 0 before and after, 1 in full swing. */
    private static double roll(double t) {
        return window(t, ROLL_FROM, ROLL_FROM + 3.5, ROLL_TO - 3.5, ROLL_TO);
    }

    /** How far round the hands have rolled (radians). */
    private static double rollTurn(double t) {
        return Math.PI * 0.5 + ROLL_TURNS * Ease.smoother((t - ROLL_FROM) / (ROLL_TO - ROLL_FROM));
    }

    /**
     * How much a hand's fingers are dragged by how it moves: moving towards its palm pushes them open, moving away
     * curls them (from how far it went the last one and a half ticks, so it follows smoothly).
     */
    private static double drag(boolean right, double t) {
        Track track = right ? RIGHT : LEFT;
        Vec3[] now = track.at(t);
        Vec3[] then = track.at(t - 1.5);
        Vec3 moved = now[0].add(lift(right, t)).subtract(then[0].add(lift(right, t - 1.5))).scale(1.0 / 1.5);
        double along = moved.dot(now[2].normalize()) / 0.35;
        return -0.1 * along / Math.sqrt(1.0 + along * along) * window(t, OUT, OUT + 3.0, RETRACT - 3.0, RETRACT);
    }

    /**
     * Pushing out of its portal (before OUT): fingertips first, straight along the way it comes out, a little
     * corkscrewed, coasting to rest in front of it with a soft overshoot and bending into its hovering pose.
     */
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

    /**
     * Pulling back into its portal (from RETRACT): it lifts a hair, then slides back along its forearm into the
     * portal, straightening out, its fingers trailing last.
     */
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

    /**
     * A hand's fist exactly round the haft: the haft runs through FIST_HOLE along the hand's x, its thumb's side to the
     * head (the right hand's right is down the haft, the left hand's up it), its fingers pointing the way its forearm
     * comes from its portal, turned square to the haft, so the wrist bends least. Trembling after the blow, it turns a
     * hair about the haft. Coming down on the haft it stays a little off it until its fingers start to close (see
     * HOLD_CLEAR); letting go, it drops off the haft and slides back off it along its fingers, so nothing of it passes
     * through the haft on its way up (see DROP_OFF). Gives its wrist, the way its fingers point and its palm.
     */
    private static Vec3[] grip(Layout layout, boolean right, double t, Axe axe, Vec3 portal) {
        double scale = layout.scale();
        Vec3 hole = axe.end().add(axe.up().scale((right ? GRIP_HIGH : GRIP_LOW) * scale));
        Vec3 thumbSide = right ? axe.up().scale(-1.0) : axe.up();
        double shake = 0.07 * wobble(t - IMPACT - (right ? 0.0 : 0.3), 2.2, 0.35, 0.8)
                * (1.0 - Ease.smoother((t - IMPACT - 8.0) / 4.0));
        double off = (HOLD_CLEAR * (1.0 - Ease.smoother((t - GRAB + 6.0) / 4.0))
                + DROP_OFF * Ease.smoother((t - RELEASE + 0.5) / 2.5)) * scale;
        double back = SLIDE_OFF * Ease.smoother((t - RELEASE - 0.5) / 4.0) * scale;
        // The fingers point the way from the portal to the wrist, and the wrist hangs off the fist the way the fingers
        // point: going round this a few times settles them.
        Vec3 toward = hole.subtract(portal);
        Vec3[] fist = new Vec3[0];
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

    /**
     * The place of a hand from its wrist, the way its forearm runs, the way its fingers point and its palm (these two
     * need not be one long nor square): straightened out, its right worked out so the frame is left-handed like every
     * construct's, and its forearm's palm side the palm turned as little as can be to lie square to the forearm, then
     * turned back round the forearm by how far the wrist is twisted ({@code twist}, radians; see twist).
     */
    private static HandPose.Place place(Vec3 wrist, Vec3 arm, Vec3 up, Vec3 palm, double scale, double twist) {
        Vec3 u = up.normalize();
        Vec3 f = palm.subtract(u.scale(palm.dot(u))).normalize();
        Vec3 r = f.cross(u);
        // The palm carried round by the turn that takes the fingers' way onto the forearm's way.
        Vec3 axis = u.cross(arm);
        double cos = u.dot(arm);
        Vec3 turned = f.scale(cos).add(axis.cross(f)).add(axis.scale(axis.dot(f) / (1.0 + cos)));
        Vec3 armForward = turned.subtract(arm.scale(turned.dot(arm))).normalize();
        if (twist != 0.0) {
            armForward = Vectors.spin(armForward, arm, -twist);
        }
        return new HandPose.Place(wrist, arm, armForward, r, u, f, scale);
    }

    // ---- The portals ----

    /** The middle of a hand's portal, at scale 1 in the pair's terms. */
    private static Vec3 portal(boolean right) {
        return right ? RIGHT_PORTAL : RIGHT_PORTAL.multiply(-1.0, 1.0, 1.0);
    }

    /** Where a hand hovers once it is out (its wrist at OUT), at scale 1 in the pair's terms. */
    private static Vec3 hover(boolean right) {
        return right ? HOVER : HOVER.multiply(-1.0, 1.0, 1.0);
    }

    /**
     * A hand's portal: facing along the hand's forearm (it swivels with it, so the forearm always runs square through
     * it); bursting open as the ring's light reaches the spot, shrinking and popping shut as the hand is gone.
     */
    private static Portal sidePortal(Layout layout, boolean right, double t, Vec3 arm) {
        Vec3 center = layout.point(portal(right));
        Vec3 rest = layout.dir(hover(right).subtract(portal(right))).normalize();
        Vec3 a0 = Vectors.UP.cross(rest).normalize();
        // Its own ways carried round by the turn that takes its first facing onto the forearm's.
        Vec3 axis = rest.cross(arm);
        double cos = rest.dot(arm);
        Vec3 a = a0.scale(cos).add(axis.cross(a0)).add(axis.scale(axis.dot(a0) / (1.0 + cos))).normalize();
        Vec3 b = arm.cross(a).normalize();
        double open = Ease.spring(t - ARRIVES, 1.05, 0.62) * shut((t - HANDS_GONE + 5.0) / 5.0);
        return new Portal(center, arm, a, b, SIDE_RADIUS * layout.scale(), open);
    }

    /** The axe's portal: facing the caster, bursting open as the hands fling apart, shut once the head is out. */
    private static Portal axePortal(Layout layout, double t) {
        Vec3 normal = layout.dir(new Vec3(0.0, 0.0, -1.0));
        Vec3 a = layout.side();
        double open = Ease.spring(t - AXE_OPENS, 0.9, 0.62) * shut((t - AXE_FREE) / 3.5);
        return new Portal(layout.point(AXE_PORTAL), normal, a, normal.cross(a), AXE_RADIUS * layout.scale(), open);
    }

    /** A portal shrinking shut over u from 0 to 1: it swells a touch, then snaps shut (1 before, 0 after). */
    private static double shut(double u) {
        if (u <= 0.0) {
            return 1.0;
        }
        if (u < 0.35) {
            return 1.0 + 0.06 * Ease.smoother(u / 0.35);
        }
        return 1.06 * (1.0 - Ease.smoother((u - 0.35) / 0.65));
    }
}
