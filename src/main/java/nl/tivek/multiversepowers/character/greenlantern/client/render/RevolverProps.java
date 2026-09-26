package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.RevolverDuo;
import nl.tivek.multiversepowers.character.greenlantern.RevolverGun;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.RevolverDuo.*;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.PORTAL_SEAM;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter.part;

final class RevolverProps {
    private static final int HAT_PIECES = 300;
    private static final int BULLET_PIECES = 400;
    private static final Vec3[] FLOATS = { new Vec3(-2.6, 10.2, -5.0), new Vec3(-4.4, 11.6, -4.8),
            new Vec3(3.4, 10.4, -5.0), new Vec3(-1.4, 12.6, -5.2), new Vec3(1.6, 12.4, -5.2), new Vec3(4.2, 12.8, -4.8) };
    private static final double POP = 6.0;
    private static final double POP_FORWARD = 4.0;
    private static final Vec3[] HELD_AT = { new Vec3(0.0, 5.9, 0.5), new Vec3(0.9, 6.6, 0.2),
            new Vec3(0.3, 3.3, 1.5), new Vec3(-0.9, 6.3, 0.5), new Vec3(-0.6, 3.7, 1.3), new Vec3(0.8, 3.0, 1.4) };
    private static final Vec3[] SPIN_AXES = { new Vec3(0.3, 1.0, 0.2), new Vec3(1.0, 0.4, 0.1),
            new Vec3(0.2, 0.3, 1.0), new Vec3(0.0, 0.2, 1.0), new Vec3(0.8, 1.0, 0.3), new Vec3(1.0, 0.1, 0.6) };
    private static final double[] SPIN_SPEED = { 0.07, 0.1, 0.06, 0.16, 0.2, 0.14 };
    private static final double[] TILT = { 0.0, 1.9, 1.2, 0.4, 2.6, 3.4 };
    private static final Vec3[] BULLET_AT = { new Vec3(-0.6, 1.2, 0.0), new Vec3(0.0, 1.1, 0.0),
            new Vec3(0.6, 1.2, 0.0), new Vec3(-0.35, 1.9, 0.0), new Vec3(0.35, 1.9, 0.0), new Vec3(0.0, 2.6, 0.0) };
    private static final double BULLET_BEHIND = 1.1;
    private static final double BULLET_FLIGHT = 5.0;
    private static final double HAT_ON_THUMB = 1.55;
    private static final double HAT_HOP = 1.5;
    private static final double HAT_TOSS = 3.2;

    private RevolverProps() {
    }

    static void draw(LanternPainter painter, RevolverDuo duo, Vec3 aim, double clock, double apart) {
        hat(painter, duo, clock, apart);
        if (clock < WHOLE) {
            for (int part = 0; part < PARTS; part++) {
                piece(painter, duo, part, clock, apart);
            }
        }
        bullets(painter, duo, aim, clock, apart);
    }

    // A place and a turn: where the model's middle goes and which way its own forward and up point.
    private record Held(Vec3 center, Vec3 forward, Vec3 up) {
        Held turned(Vec3 axis, double angle) {
            return new Held(this.center, Vectors.spin(this.forward, axis, angle), Vectors.spin(this.up, axis, angle));
        }

        Held toward(Held other, double u) {
            if (u <= 0.0) {
                return this;
            }
            if (u >= 1.0) {
                return other;
            }
            Vec3[] from = Vectors.frame(this.forward, this.up);
            Vec3 turn = Vectors.turn(from, Vectors.frame(other.forward, other.up)).scale(u);
            return new Held(this.center.lerp(other.center, u), Vectors.turned(from[0], turn),
                    Vectors.turned(from[1], turn));
        }

        ConstructPainter.Frame frame(Vec3 middle, double scale) {
            Vec3[] axes = Vectors.frame(this.up, this.forward);
            Vec3 up = axes[0];
            Vec3 forward = axes[1];
            Vec3 right = forward.cross(up);
            Vec3 origin = this.center.subtract(right.scale(middle.x * scale)).subtract(up.scale(middle.y * scale))
                    .subtract(forward.scale(middle.z * scale));
            return new ConstructPainter.Frame(origin, right, up, forward, scale);
        }
    }

    private static void hat(LanternPainter painter, RevolverDuo duo, double clock, double apart) {
        if (clock < HAT_PINCH - 0.5 || clock > HAT_IN + 2) {
            return;
        }
        Held hat = hatAt(duo, clock);
        boolean cut = clock < HAT_OUT + 1 || clock > HAT_THROW;
        if (cut) {
            painter.clip(duo.topPortal.center(), duo.topPortal.normal(), PORTAL_SEAM);
        }
        part(painter, RevolverShapes.HAT, hat.frame(Vec3.ZERO, duo.stage.scale() * HAT_SIZE), 1.0, apart, HAT_PIECES);
        if (cut) {
            painter.noClip();
        }
    }

    private static Held hatAt(RevolverDuo duo, double clock) {
        Vec3 toViewer = duo.stage.way().scale(-1.0);
        double scale = duo.stage.scale();
        if (clock < HAT_ON - 2) {
            return pinched(duo, toViewer, scale);
        }
        if (clock < HAT_ON + 1) {
            return pinched(duo, toViewer, scale).toward(onBack(duo, clock, toViewer), Ease.smoother(
                    (clock - HAT_ON + 2) / 3.0));
        }
        if (clock < GUN_FORM - 6) {
            return onBack(duo, clock, toViewer);
        }
        if (clock < GUN_FORM) {
            double u = (clock - GUN_FORM + 6) / 6.0;
            Held hop = onBack(duo, clock, toViewer).toward(onThumb(duo, toViewer, scale), Ease.smoother(u));
            return new Held(hop.center.add(0.0, HAT_HOP * scale * 4.0 * u * (1.0 - u), 0.0), hop.forward, hop.up)
                    .turned(Vectors.UP, Math.PI * 2.0 * Ease.smoother(u));
        }
        if (clock < HAT_FLICK) {
            return onThumb(duo, toViewer, scale);
        }
        if (clock < HAT_CATCH) {
            RevolverDuo then = at(duo, HAT_FLICK);
            Held from = onThumb(then, toViewer, scale);
            Held to = onPalm(duo, toViewer);
            double u = (clock - HAT_FLICK) / (HAT_CATCH - HAT_FLICK);
            Held flying = from.toward(to, Ease.smoother(u));
            Vec3 side = flying.forward.cross(flying.up).normalize();
            return new Held(flying.center.add(0.0, HAT_TOSS * scale * 4.0 * u * (1.0 - u), 0.0), flying.forward,
                    flying.up).turned(side, Math.PI * 4.0 * Ease.smoother(u));
        }
        if (clock < HAT_THROW) {
            return onPalm(duo, toViewer);
        }
        RevolverDuo then = at(duo, HAT_THROW);
        Held from = onPalm(then, toViewer);
        double u = Mth.clamp((clock - HAT_THROW) / (HAT_IN - HAT_THROW), 0.0, 1.0);
        Vec3 top = duo.topPortal.center();
        Vec3 into = duo.topPortal.normal();
        Vec3 mid = top.add(into.scale(4.0 * scale)).add(0.0, 2.5 * scale, 0.0);
        Vec3 end = top.subtract(into.scale(2.5 * scale));
        double v = 1.0 - u;
        Vec3 center = from.center.scale(v * v).add(mid.scale(2.0 * u * v)).add(end.scale(u * u));
        Held flat = new Held(center, from.forward, from.up).toward(new Held(center, toViewer, into),
                Ease.smoother(u));
        return flat.turned(flat.up, Math.PI * 6.0 * u);
    }

    private static Held pinched(RevolverDuo duo, Vec3 toViewer, double scale) {
        Vec3 pinch = HandPainter.indexTip(duo.aPose, duo.aPlace, false)
                .lerp(HandPainter.thumbTip(duo.aPose, duo.aPlace, false), 0.5);
        Vec3 up = duo.aPlace.up().scale(-1.0);
        return new Held(pinch.subtract(up.scale((HAT_TALL - 0.1) * scale)), across(toViewer, up), up);
    }

    private static Held onBack(RevolverDuo duo, double clock, Vec3 toViewer) {
        Vec3 up = duo.bPlace.forward().scale(-1.0);
        Vec3 side = across(toViewer, up).cross(up);
        double proud = RevolverDuo.proud(clock);
        double wobble = 0.22 * Math.sin(Math.PI * 2.0 * (clock - SHAKE_FROM - 1.5) / 11.0) * proud;
        up = Vectors.spin(up, side.normalize(), wobble);
        return new Held(duo.bPlace.at(BACK), across(toViewer, up), up);
    }

    private static Held onThumb(RevolverDuo duo, Vec3 toViewer, double scale) {
        Vec3 tip = HandPainter.thumbTip(duo.bPose, duo.bPlace, true);
        Vec3 way = HandPainter.thumbWay(duo.bPose, duo.bPlace, true);
        Vec3 up = Vectors.UP.scale(0.85).add(way.scale(0.15)).normalize();
        return new Held(tip.subtract(up.scale(HAT_ON_THUMB * scale)), across(toViewer, up), up);
    }

    private static Held onPalm(RevolverDuo duo, Vec3 toViewer) {
        Vec3 up = duo.bPlace.forward();
        return new Held(duo.bPlace.at(PALM_AT), across(toViewer, up), up);
    }

    private static Vec3 across(Vec3 way, Vec3 up) {
        Vec3 flat = way.subtract(up.scale(way.dot(up)));
        return flat.lengthSqr() < 1.0E-6 ? Vectors.across(up)[0] : flat.normalize();
    }

    private static RevolverDuo at(RevolverDuo duo, double clock) {
        return RevolverDuo.at(duo.stage.base(), variantOf(duo), duo.stage.base(), clock, duo.stage.scale());
    }

    private static int variantOf(RevolverDuo duo) {
        return RevolverDuo.variant(duo.stage.way());
    }

    private static void piece(LanternPainter painter, RevolverDuo duo, int part, double clock, double apart) {
        boolean fromA = part == BARREL || part == HAMMER || part == GUARD;
        double shows = fromA ? PARTS_GRAB - 0.5 : CLAW_CLAMP - 0.5;
        if (clock < shows) {
            return;
        }
        Held held = partAt(duo, part, clock, fromA);
        boolean cut = fromA ? clock < PARTS_OUT + 1 : clock < CLAW_OUT + 1;
        if (cut) {
            painter.clip(duo.topPortal.center(), duo.topPortal.normal(), PORTAL_SEAM);
        }
        part(painter, RevolverShapes.PARTS[part], held.frame(RevolverShapes.CENTERS[part], duo.stage.scale()), 1.0,
                apart, RevolverPainter.GUN_PIECES + part * 20);
        if (cut) {
            painter.noClip();
        }
    }

    private static Held partAt(RevolverDuo duo, int part, double clock, boolean fromA) {
        int built = BUILT[part];
        if (clock >= built) {
            return assembled(duo, part);
        }
        double freed = fromA ? PARTS_FLICK : CLAW_DROP;
        if (clock < freed) {
            return inHand(duo, part, fromA);
        }
        double flies = built - FLY_TICKS;
        if (clock < freed + POP) {
            // Out of the hand forward first, then round to its place: never back through the fingers.
            Held from = inHand(at(duo, freed), part, fromA);
            Held to = floating(duo, part, clock, from, freed);
            double u = Ease.smoother((clock - freed) / POP);
            double v = 1.0 - u;
            Vec3 bend = from.center.subtract(duo.stage.way().scale(POP_FORWARD * duo.stage.scale()));
            Held turned = from.toward(to, u);
            return new Held(from.center.scale(v * v).add(bend.scale(2.0 * u * v)).add(to.center.scale(u * u)),
                    turned.forward, turned.up);
        }
        Held start = inHand(at(duo, freed), part, fromA);
        if (clock < flies) {
            return floating(duo, part, clock, start, freed);
        }
        Held from = floating(duo, part, flies, start, freed);
        double u = (clock - flies) / FLY_TICKS;
        return from.toward(assembled(duo, part), u * u * (3.0 - 2.0 * u) * 0.6 + 0.4 * u * u);
    }

    private static Held inHand(RevolverDuo duo, int part, boolean fromA) {
        HandPose.Place place = fromA ? duo.aPlace : duo.bPlace;
        Vec3 at = place.at(fromA ? HELD_AT[part] : HELD_AT[part].multiply(-1.0, 1.0, 1.0));
        Held held = fromA ? new Held(at, place.right(), place.forward().scale(-1.0))
                : new Held(at, place.forward().scale(-1.0), Vectors.UP);
        return held.turned(place.up(), TILT[part]);
    }

    private static Held floating(RevolverDuo duo, int part, double clock, Held start, double freed) {
        double bob = 0.2 * Math.sin(0.23 * clock + part * 1.3);
        Vec3 center = duo.stage.point(FLOATS[part].add(0.0, bob, 0.0));
        Vec3 axis = part == BARREL ? start.forward : SPIN_AXES[part].normalize();
        return new Held(center, start.forward, start.up).turned(axis, SPIN_SPEED[part] * (clock - freed));
    }

    private static Held assembled(RevolverDuo duo, int part) {
        RevolverGun.Gun gun = duo.gun;
        return new Held(gun.at(RevolverShapes.CENTERS[part]), gun.forward(), gun.up());
    }

    private static void bullets(LanternPainter painter, RevolverDuo duo, Vec3 aim, double clock, double apart) {
        if (clock < BULLETS_IN_HAND[0] || clock > CYL_CLOSE + 1) {
            return;
        }
        double scale = duo.stage.scale();
        RevolverGun.Pose pose = RevolverGun.pose(clock, duo.stage.local(aim));
        double crane = RevolverGun.crane(clock);
        double turn = RevolverGun.cylinderTurn(clock);
        Vec3 into = duo.stage.dir(pose.forward()).normalize();
        for (int k = 0; k < BULLETS_IN_HAND.length; k++) {
            double born = clock - BULLETS_IN_HAND[k];
            if (born < 0.0) {
                continue;
            }
            double grow = Math.min(1.0, Ease.spring(born, 1.2, 0.5));
            Vec3 seated = duo.stage.point(RevolverGun.chamber(pose, crane, turn, k, -0.4));
            Vec3 behind = seated.subtract(into.scale(BULLET_BEHIND * scale));
            Held held;
            double loads = LOADS[k];
            if (clock < loads - BULLET_FLIGHT) {
                held = inPalm(duo, k);
            } else if (clock < loads) {
                // Tossed off the palm in an arc that comes up into its chamber from behind the cylinder.
                Held from = inPalm(at(duo, loads - BULLET_FLIGHT), k);
                double u = (clock - loads + BULLET_FLIGHT) / BULLET_FLIGHT;
                double v = 1.0 - u;
                Vec3 center = from.center.scale(v * v).add(behind.subtract(into.scale(BULLET_BEHIND * scale))
                        .scale(2.0 * u * v)).add(seated.scale(u * u));
                Held turning = from.toward(new Held(seated, duo.bPlace.up(), into), Ease.smoother(u * 1.4));
                held = new Held(center, turning.forward, turning.up);
            } else {
                held = new Held(seated, duo.bPlace.up(), into);
            }
            ConstructPainter.Frame frame = held.frame(Vec3.ZERO, scale * Math.max(0.01, grow));
            part(painter, RevolverShapes.BULLET, frame, 1.0, apart, BULLET_PIECES + k * 5);
            if (apart < 0.0 && clock < loads + 1.0) {
                boolean flying = clock >= loads - BULLET_FLIGHT;
                painter.flare(held.center.add(held.up.scale(0.5 * scale)), (flying ? 1.1 : 0.7) * scale * grow,
                        flying ? 0.85 : 0.35);
            }
        }
    }

    private static Held inPalm(RevolverDuo duo, int k) {
        HandPose.Place place = duo.bPlace;
        Vec3 at = place.at(new Vec3(-BULLET_AT[k].x, BULLET_AT[k].y, PALM_AT.z + 0.27));
        return new Held(at, place.up(), place.right());
    }
}
