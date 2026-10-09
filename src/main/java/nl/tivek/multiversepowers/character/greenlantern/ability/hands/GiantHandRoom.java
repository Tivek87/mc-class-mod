package nl.tivek.multiversepowers.character.greenlantern.ability.hands;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.duo.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import static nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands.SCALE;

final class GiantHandRoom {
    private static final int STEP = 2;
    private static final int PAIR_STEP = 4;
    private static final int SLACK = 3;
    private static final double DRIFT = 0.25;
    private static final double HAND = 1.8;
    private static final double HAFT = 0.8;
    private static final double ARM_STEP = 2.0;
    private static final double[] REACH = { 16.0, 11.0, 6.0, 10.0, 13.0, 14.0, 12.0, 9.0, 14.0, 10.0, 16.0, 34.0,
            12.0, 8.0, 10.0, 12.0, 16.0, 14.0, 8.0, 14.0, 12.0, 12.0, 14.0, 12.0, 17.0, 18.0, 9.0, 11.0, 12.0,
            22.0 };
    private static final double[] ALONG = { 0.0, 2.1, 4.2, 6.0 };
    private static final double[] PAIR_ALONG = { 0.0, 2.1, 4.2 };
    private static final double[] HAFT_AT = { 1.0, 4.5, 8.0 };

    private record Ball(Vec3 at, double radius) {
    }

    private record Frame(int t, List<Ball> balls, AABB box) {
    }

    private final GiantHandBase hand;
    private final Vec3 aim;
    private final List<Frame> frames = new ArrayList<>();

    private GiantHandRoom(GiantHandBase hand) {
        this.hand = hand;
        this.aim = hand.aim;
        if (hand.move == HandPose.AXE) {
            for (int t = hand.t; t <= HandDuo.LIFE; t += PAIR_STEP) {
                this.pair(t, HandDuo.at(hand.base, hand.variant, hand.aim, t, SCALE));
            }
        } else if (HandGroup.is(hand.variant)) {
            for (int t = hand.t; t <= HandPose.life(hand.variant); t += PAIR_STEP) {
                List<Ball> balls = new ArrayList<>();
                for (HandGroup.Sub sub : HandGroup.at(hand.variant, hand.base, hand.aim.subtract(hand.base), t)) {
                    balls.add(new Ball(sub.place().at(new Vec3(0.0, 2.0, 0.0)), HAND * sub.place().scale()));
                    if (sub.portal().open() > 0.0) {
                        balls.add(new Ball(sub.portal().center(), sub.portal().radius()));
                    }
                }
                this.keep(t, balls);
            }
        } else {
            Vec3 reach = hand.aim.subtract(hand.base);
            for (int t = hand.t; t <= HandPose.life(hand.variant); t += STEP) {
                HandPose pose = hand.pose(t);
                this.single(t, pose.place(hand.base, reach, SCALE), pose.length * SCALE);
            }
        }
    }

    static GiantHandRoom of(GiantHandBase hand) {
        GiantHandRoom room = hand.room;
        if (room == null || room.aim.distanceToSqr(hand.aim) > DRIFT * DRIFT) {
            room = new GiantHandRoom(hand);
            hand.room = room;
        }
        return room;
    }

    static boolean near(GiantHandBase a, GiantHandBase b) {
        double reach = (REACH[a.move] + REACH[b.move]) * SCALE;
        double dx = a.base.x - b.base.x;
        double dz = a.base.z - b.base.z;
        return dx * dx + dz * dz < reach * reach;
    }

    // Two hands clash only where both are at the same moment: a place one has left is free again.
    boolean clashes(GiantHandRoom other) {
        for (Frame a : this.frames) {
            int when = a.t() - this.hand.t;
            if (when < 0) {
                continue;
            }
            for (Frame b : other.frames) {
                int then = b.t() - other.hand.t;
                if (then < when - SLACK) {
                    continue;
                }
                if (then > when + SLACK) {
                    break;
                }
                if (a.box().intersects(b.box()) && touch(a, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean touch(Frame a, Frame b) {
        for (Ball x : a.balls()) {
            for (Ball y : b.balls()) {
                double apart = x.radius() + y.radius();
                if (x.at().distanceToSqr(y.at()) < apart * apart) {
                    return true;
                }
            }
        }
        return false;
    }

    private void single(int t, HandPose.Place place, double out) {
        List<Ball> balls = new ArrayList<>();
        for (double along = ARM_STEP * 0.5; along < out; along += ARM_STEP) {
            this.add(balls, this.hand.base.add(place.arm().scale(along)), HAND * SCALE);
        }
        for (double up : ALONG) {
            this.add(balls, place.at(new Vec3(0.0, up, 0.0)), HAND * SCALE);
        }
        this.keep(t, balls);
    }

    private void pair(int t, HandDuo duo) {
        List<Ball> balls = new ArrayList<>();
        if (duo.handsThere) {
            for (HandPose.Place place : new HandPose.Place[] { duo.leftPlace, duo.rightPlace }) {
                for (double up : PAIR_ALONG) {
                    this.add(balls, place.at(new Vec3(0.0, up, 0.0)), HAND * SCALE);
                }
            }
        }
        for (HandDuo.Portal portal : new HandDuo.Portal[] { duo.leftPortal, duo.rightPortal, duo.axePortal }) {
            if (portal.open() > 0.0) {
                this.add(balls, portal.center(), portal.radius());
            }
        }
        if (duo.axeThere) {
            for (double along : HAFT_AT) {
                this.add(balls, duo.axeEnd.add(duo.axeUp.scale(along * SCALE)), HAFT * SCALE);
            }
            this.add(balls, GiantHands.head(duo), HandDuo.BLADE_HALF * SCALE);
        }
        this.keep(t, balls);
    }

    private void add(List<Ball> balls, Vec3 at, double radius) {
        // Below the ground or behind its portal a hand is still hidden: only what has come out takes room.
        Vec3 out = HandPose.portal(this.hand.variant) ? HandPose.portalOf(this.hand.variant, this.hand.base,
                this.aim.subtract(this.hand.base), 0.0, SCALE).normal()
                : HandPose.rootNormal(this.hand.variant, this.aim.subtract(this.hand.base));
        if (at.subtract(this.hand.base).dot(out) >= -0.5) {
            balls.add(new Ball(at, radius));
        }
    }

    private void keep(int t, List<Ball> balls) {
        if (balls.isEmpty()) {
            return;
        }
        AABB box = null;
        for (Ball ball : balls) {
            Vec3 at = ball.at();
            double r = ball.radius();
            AABB around = new AABB(at.x - r, at.y - r, at.z - r, at.x + r, at.y + r, at.z + r);
            box = box == null ? around : box.minmax(around);
        }
        this.frames.add(new Frame(t, balls, box));
    }
}
