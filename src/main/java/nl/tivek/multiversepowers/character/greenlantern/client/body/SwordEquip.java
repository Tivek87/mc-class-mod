package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

abstract class SwordEquip extends SwordKeys {
    static final Vec3 TOSS_AXIS = new Vec3(0.70, 0.05, -0.71).normalize();
    static final double SPIN = SwordMove.TOSS_TURNS * Mth.TWO_PI / (SwordMove.CATCH - SwordMove.TOSS);
    static final double BALANCE = 0.2;
    static final double OWN_BALANCE = BALANCE * SwordPainter.TIP * OWN_SWORD;
    private static final Vec3 RELEASE = new Vec3(0.26, -0.40, -1.26);
    private static final Vec3 CAUGHT = new Vec3(0.24, -0.30, -1.26);
    private static final double TOSS_HIGH = 0.5;
    static final Vec3 UPRIGHT = new Vec3(0.04, 0.99, -0.12).normalize();
    static final Vec3 FLAT = square(new Vec3(-1.0, 0.0, 0.0), UPRIGHT);
    private static final double COCK = 0.8;
    private static final double MEET = 0.9;
    private static final double CARRY = 0.42;
    static final Vec3 BLADE_TURN = TOSS_AXIS.cross(UPRIGHT).scale(SPIN);
    private static final Vec3 EDGE_TURN = TOSS_AXIS.cross(FLAT).scale(SPIN);
    private static final double FALL;
    private static final Vec3 THROWN;
    private static final Vec3 LET_GO;
    private static final Vec3 CAUGHT_AT;

    private static final Vec3 LOOKED_AT = new Vec3(-0.55, 0.74, -0.40).normalize();
    static final float INSPECT_FROM = SwordMove.CATCH + 8.0F;
    static final float INSPECT_TO = SwordMove.KNOCK - 8.0F;
    private static final Vec3 BRACED = new Vec3(-0.44, -0.50, -0.90);
    private static final Vec3 BRACED_FACE = new Vec3(0.12, 0.06, -1.0).normalize();
    private static final Vec3 BRACED_TOP = square(new Vec3(0.02, 1.0, 0.08), BRACED_FACE);
    private static final float BRACE_FROM = INSPECT_TO - 3.0F;
    private static final float BRACE_TO = SwordMove.KNOCK_AGAIN + 9.5F;
    private static final double FACE_X = 0.0;
    private static final double FACE_Y = 0.36;
    private static final double FACE_OUT = SwordPainter.faceOut(FACE_X, FACE_Y);
    private static final double STRIKE_AT = 0.9;
    static final double BLADE_WIDE = 0.095;
    private static final Vec3 KNOCKING = BRACED_TOP.cross(BRACED_FACE).normalize();
    static final Vec3 KNOCK_EDGE = BRACED_FACE.scale(-1.0);
    private static final double SWING_LEAN = 0.35;
    private static final Vec3 CHOP = BRACED_TOP.scale(Math.cos(SWING_LEAN)).add(KNOCKING.scale(Math.sin(SWING_LEAN)))
            .normalize();
    private static final Vec3 SWING_OUT = KNOCKING.scale(-1.0).add(BRACED_TOP.scale(0.35)).add(BRACED_FACE.scale(0.25))
            .normalize();
    private static final double WIND_OUT = 0.12;
    private static final double WIND_TURN = 2.2;
    private static final double AGAIN_OUT = 0.05;
    private static final double AGAIN_TURN = 0.42;
    private static final float WIND_TICKS = 3.0F;
    private static final float AGAIN_TICKS = 2.0F;
    private static final double BOUNCE = 0.33;
    private static final float REST_TICKS = 1.8F;
    static final Vec3 STRUCK;

    static {
        double time = SwordMove.CATCH - SwordMove.TOSS;
        double half = time / 2.0;
        FALL = fall(TOSS_HIGH, (CAUGHT.y - RELEASE.y) / time, time);
        THROWN = CAUGHT.subtract(RELEASE).scale(1.0 / time).add(0.0, FALL * half, 0.0);
        Vec3 turning = BLADE_TURN.scale(OWN_BALANCE);
        LET_GO = THROWN.subtract(turning);
        CAUGHT_AT = THROWN.subtract(0.0, FALL * time, 0.0).subtract(turning);
        STRUCK = struck(SwordMove.KNOCK);
        equip();
    }

    private static void equip() {
        float toss = SwordMove.TOSS;
        float caught = SwordMove.CATCH;
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        Vec3 hit = contact();
        Vec3 swungOut = hit.add(SWING_OUT.scale(WIND_OUT));
        Vec3 swungAgain = hit.add(SWING_OUT.scale(AGAIN_OUT));
        Vec3 strike = hit.subtract(swungOut).scale(2.0 / WIND_TICKS);
        Vec3 strikeAgain = hit.subtract(swungAgain).scale(2.0 / AGAIN_TICKS);
        float rest = again + REST_TICKS;
        Vec3 rested = hit.subtract(KNOCK_EDGE.scale(0.03));
        Vec3 guarded = GUARD.hand().add(0.01, 0.03, 0.0);
        Vec3 home = guarded.subtract(rested).scale(1.5 / (BRACE_TO - rest));
        Key low = held(BRACE_FROM, -0.54, -0.61, -0.94, -0.50, 0.01, -1.0, 0.10, 1.0, 0.10);
        Track hand = new Track(0, Pose.BLADE, new Key[] {
                at(4, false, 0.40, -0.44, -0.92),
                at(8, false, 0.34, -0.47, -0.98),
                at(toss - 1.5F, true, 0.33, -0.50, -1.08),
                moving(at(toss, false, RELEASE), 0, LET_GO, LET_GO),
                at(toss + 2.5F, true, RELEASE.add(LET_GO.scale(1.3))),
                at(toss + 7.0F, false, 0.28, -0.44, -1.20),
                at(caught - 5.0F, false, 0.29, -0.46, -1.21),
                moving(at(caught, false, CAUGHT), 0, CAUGHT_AT, CAUGHT_AT),
                at(caught + 2.5F, true, CAUGHT.add(CAUGHT_AT.scale(1.25))),
                at(INSPECT_FROM, false, 0.21, -0.31, -0.84),
                at((INSPECT_FROM + INSPECT_TO) / 2.0F, false, 0.19, -0.29, -0.82),
                at(INSPECT_TO, false, 0.22, -0.28, -0.83),
                at(knock - WIND_TICKS, true, swungOut),
                moving(at(knock, false, hit), 0, strike, strike.scale(-BOUNCE)),
                at(again - AGAIN_TICKS, true, swungAgain),
                moving(at(again, false, hit), 0, strikeAgain, strikeAgain.scale(-0.8 * BOUNCE)),
                at(rest, true, rested),
                moving(at((rest + BRACE_TO) / 2.0F, false, rested.lerp(guarded, 0.5).add(SWING_OUT.scale(0.04))), 0,
                        home, home),
                at(BRACE_TO, false, guarded) });
        Track shield = new Track(Pose.SHIELD_FROM, Pose.SHIELD_TO + 1, new Key[] {
                held(3, -0.44, -0.56, -0.90, -0.40, 0.06, -1.0, 0.06, 1.0, 0.12),
                held(7, -0.46, -0.53, -0.91, -0.43, 0.06, -1.0, 0.07, 1.0, 0.12),
                held(9, -0.47, -0.52, -0.92, -0.44, 0.06, -1.0, 0.08, 1.0, 0.12),
                held(SwordMove.SHIELD_LOCK + 0.6F, -0.48, -0.57, -0.92, -0.45, -0.03, -1.0, 0.08, 1.0, 0.03),
                held(toss + 0.5F, -0.50, -0.52, -0.93, -0.47, 0.06, -1.0, 0.08, 1.0, 0.13),
                held(toss + 6.0F, -0.50, -0.51, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12),
                held(caught - 2.0F, -0.50, -0.52, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12),
                held(caught + 1.0F, -0.51, -0.545, -0.925, -0.46, 0.03, -1.0, 0.08, 1.0, 0.10),
                held(caught + 4.0F, -0.50, -0.53, -0.92, -0.45, 0.05, -1.0, 0.08, 1.0, 0.12),
                held(INSPECT_FROM + 2.0F, -0.54, -0.62, -0.94, -0.50, 0.0, -1.0, 0.10, 1.0, 0.10),
                low,
                braced(knock - 3.0F, 0.0),
                moving(braced(knock, 0.0), Pose.SHIELD_FROM, BRACED_FACE.scale(0.01), KNOCK_EDGE.scale(0.03)),
                braced(knock + 1.8F, 1.0),
                moving(braced(again, 0.0), Pose.SHIELD_FROM, BRACED_FACE.scale(0.008), KNOCK_EDGE.scale(0.022)),
                braced(again + 1.8F, 0.75),
                braced(again + 3.5F, 0.1),
                held(BRACE_TO, -0.49, -0.50, -0.92, -0.44, 0.06, -1.0, 0.08, 1.0, 0.12) });
        Track body = new Track(Pose.BODY_FROM, Pose.SIZE, new Key[] {
                arrived(leaning(4, 0, 0.04F)),
                leaning(toss - 3.0F, 5, 0.12F),
                leaning(toss - 1.0F, -3, -0.02F),
                leaning(toss + 4.0F, -2, -0.10F),
                leaning(caught - 3.0F, 0, -0.06F),
                leaning(caught + 1.0F, 3, 0.09F),
                leaning(caught + 5.0F, -2, 0.02F),
                leaning(INSPECT_FROM + 2.0F, -7, -0.05F),
                leaning(INSPECT_TO - 1.0F, -5, -0.04F),
                leaning(knock - WIND_TICKS - 0.5F, 30, -0.02F),
                leaning(knock - 0.5F, 20, 0.10F),
                leaning(again - AGAIN_TICKS - 0.3F, 24, 0.06F),
                leaning(again - 0.5F, 20, 0.09F),
                leaning(again + 3.0F, 12, 0.05F),
                leaning(BRACE_TO - 1.0F, -1, 0.02F) });
        MOVES.put(SwordMove.EQUIP, new Track[] { hand, new Track(Pose.BLADE, Pose.SHIELD_FROM, equipWrist()), shield,
                body });
    }

    private static Key[] equipWrist() {
        float toss = SwordMove.TOSS;
        float caught = SwordMove.CATCH;
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        Key[] keys = new Key[25];
        int k = 0;
        keys[k++] = wrist(4, false, new Vec3(0.06, 0.98, -0.18), new Vec3(-1.0, 0.0, 0.0));
        keys[k++] = wrist(8, false, UPRIGHT, FLAT);
        keys[k++] = wrist(toss - 1.5F, true, tossTurn(-COCK, UPRIGHT), tossTurn(-COCK, FLAT));
        keys[k++] = moving(wrist(toss, false, UPRIGHT, FLAT), BLADE_TURN, EDGE_TURN, BLADE_TURN, EDGE_TURN);
        keys[k++] = wrist(toss + 2.5F, true, tossTurn(0.5, UPRIGHT), tossTurn(0.5, FLAT));
        keys[k++] = wrist(caught - 2.5F, true, tossTurn(-MEET, UPRIGHT), tossTurn(-MEET, FLAT));
        keys[k++] = moving(wrist(caught, false, UPRIGHT, FLAT), BLADE_TURN, EDGE_TURN, BLADE_TURN, EDGE_TURN);
        keys[k++] = wrist(caught + 1.4F, true, tossTurn(CARRY, UPRIGHT), tossTurn(CARRY, FLAT));
        keys[k++] = wrist(caught + 3.6F, false, tossTurn(-0.07, UPRIGHT), tossTurn(-0.07, FLAT));
        Vec3 up = new Vec3(0.0, 1.0, 0.0);
        Vec3 facing = square(LOOKED_AT.cross(new Vec3(0.0, 0.0, 1.0)), LOOKED_AT);
        Vec3 blade = LOOKED_AT;
        Vec3 edge = facing;
        for (int i = 0; i <= 6; i++) {
            double u = Ease.smoother(i / 6.0);
            double swing = 0.16 - 0.32 * u;
            blade = Vectors.spin(LOOKED_AT, up, swing);
            edge = Vectors.spin(square(Vectors.spin(facing, up, swing), blade), blade, Math.PI * u);
            keys[k++] = wrist(Mth.lerp(i / 6.0F, INSPECT_FROM, INSPECT_TO), i == 6, blade, edge);
        }
        double fast = 2.0 * WIND_TURN / WIND_TICKS;
        double fastAgain = 2.0 * AGAIN_TURN / AGAIN_TICKS;
        float wound = knock - WIND_TICKS;
        float rest = again + REST_TICKS;
        Vec3[] windUp = { chopped(KNOCKING, WIND_TURN), chopped(KNOCK_EDGE, WIND_TURN) };
        Vec3[] rested = { chopped(KNOCKING, 0.08), chopped(KNOCK_EDGE, 0.08) };
        keys[k++] = sweeping((INSPECT_TO + wound) / 2.0F, new Vec3[] { blade, edge }, windUp, wound - INSPECT_TO);
        keys[k++] = wrist(wound, true, windUp[0], windUp[1]);
        keys[k++] = swinging(knock - WIND_TICKS / 2.0F, 0.75 * WIND_TURN, fast / 2.0, fast / 2.0);
        keys[k++] = swinging(knock, 0.0, fast, -BOUNCE * fast);
        keys[k++] = wrist(again - AGAIN_TICKS, true, chopped(KNOCKING, AGAIN_TURN), chopped(KNOCK_EDGE, AGAIN_TURN));
        keys[k++] = swinging(again, 0.0, fastAgain, -0.8 * BOUNCE * fastAgain);
        keys[k++] = wrist(rest, true, rested[0], rested[1]);
        keys[k++] = sweeping((rest + BRACE_TO) / 2.0F, rested, new Vec3[] { GUARD.blade(), GUARD.edge() },
                BRACE_TO - rest);
        keys[k] = wrist(BRACE_TO, false, GUARD.blade(), GUARD.edge());
        return keys;
    }

    private static Vec3 chopped(Vec3 way, double angle) {
        return Vectors.spin(way, CHOP, -angle);
    }

    private static Key swinging(float tick, double angle, double in, double out) {
        return moving(wrist(tick, false, chopped(KNOCKING, angle), chopped(KNOCK_EDGE, angle)),
                swing(KNOCKING, angle, in), swing(KNOCK_EDGE, angle, in), swing(KNOCKING, angle, out),
                swing(KNOCK_EDGE, angle, out));
    }

    private static Vec3 swing(Vec3 way, double angle, double speed) {
        return CHOP.cross(chopped(way, angle)).scale(speed);
    }

    private static Key sweeping(float tick, Vec3[] from, Vec3[] to, float ticks) {
        Vec3 edge = to[1].dot(from[1]) < 0.0 ? to[1].scale(-1.0) : to[1];
        Vec3[] a = Vectors.frame(from[0], from[1]);
        Vec3 turn = Vectors.turn(a, Vectors.frame(to[0], edge));
        Vec3 blade = Vectors.turned(a[0], turn.scale(0.5));
        Vec3 halfway = Vectors.turned(a[1], turn.scale(0.5));
        Vec3 spin = turn.scale(1.5 / ticks);
        return moving(wrist(tick, false, blade, halfway), spin.cross(blade), spin.cross(halfway), spin.cross(blade),
                spin.cross(halfway));
    }

    private static Key braced(float tick, double give) {
        Vec3 tipped = Vectors.spin(BRACED_FACE, BRACED_FACE.cross(BRACED_TOP).normalize(), 0.07 * give);
        Vec3 top = square(BRACED_TOP, tipped);
        Vec3 middle = BRACED.add(KNOCK_EDGE.scale(0.035 * give));
        return held(tick, middle.x, middle.y, middle.z, tipped.x, tipped.y, tipped.z, top.x, top.y, top.z);
    }

    private static final float SWORD_FROM = 1.0F;
    private static final float SWORD_GROWS = 7.0F;

    static float swordGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? (float) Ease.smooth((t - SWORD_FROM) / SWORD_GROWS) : 1.0F;
    }

    static float shieldGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? Mth.clamp((t - 2.0F) / (SwordMove.SHIELD_LOCK - 2.0F), 0.0F, 1.0F) : 1.0F;
    }

    record Flight(Vec3 grip, Vec3 blade, Vec3 edge) {
    }

    static float tossed(SwordMove move, float t) {
        if (move != SwordMove.EQUIP || t <= SwordMove.TOSS || t >= SwordMove.CATCH) {
            return -1.0F;
        }
        return (t - SwordMove.TOSS) / (SwordMove.CATCH - SwordMove.TOSS);
    }

    static Vec3 tossTurn(double angle, Vec3 way) {
        return Vectors.spin(way, TOSS_AXIS, angle);
    }

    static double fall(double high, double climb, double time) {
        double half = time / 2.0;
        double b = 2.0 * half * climb - 2.0 * high;
        return (-b + Math.sqrt(Math.max(0.0, b * b - 4.0 * half * half * climb * climb))) / (2.0 * half * half);
    }

    static Flight flight(float t) {
        double tau = t - SwordMove.TOSS;
        Vec3 balance = RELEASE.add(UPRIGHT.scale(OWN_BALANCE)).add(THROWN.scale(tau)).subtract(0.0,
                0.5 * FALL * tau * tau, 0.0);
        Vec3 blade = tossTurn(SPIN * tau, UPRIGHT);
        return new Flight(balance.subtract(blade.scale(OWN_BALANCE)), blade, tossTurn(SPIN * tau, FLAT));
    }

    private static Vec3 contact() {
        return STRUCK.subtract(KNOCK_EDGE.scale(BLADE_WIDE * OWN_SWORD)).subtract(KNOCKING.scale(STRIKE_AT * OWN_SWORD));
    }

    private static Vec3 struck(float t) {
        Pose shield = Pose.of(braced(t, 0.0).numbers());
        return onFace(shield.shield(), shield.shieldRight(), shield.top(), shield.face(), OWN_SHIELD);
    }

    static Vec3 onFace(Vec3 middle, Vec3 right, Vec3 top, Vec3 face, double scale) {
        return middle.add(right.scale(FACE_X * scale)).add(top.scale(FACE_Y * scale)).add(face.scale(FACE_OUT * scale));
    }

    static float gleam(SwordMove move, float t) {
        if (move != SwordMove.EQUIP) {
            return -1.0F;
        }
        float u = (t - SwordMove.GLEAM) / 7.0F;
        return u < 0.0F || u > 1.0F ? -1.0F : u;
    }
}
