package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

/**
 * Taking the sword and shield out (see {@link SwordPoses}), before your own eyes: how they grow out of the ring's
 * light, the keys of the hand, the wrist, the shield and the body, the tossed sword flying freely, and the blade banged
 * on the rim of the shield.
 */
abstract class SwordEquip extends SwordKeys {
    // ---- The toss as they take shape ----

    /**
     * About which way before your eyes the tossed sword turns over: half across (the way a flick of the wrist turns it
     * over, end over end) and half along the way you look, so from your own eyes it turns over at a slant you can
     * follow all the way round, and from outside it cartwheels at a slant too.
     */
    static final Vec3 TOSS_AXIS = new Vec3(0.70, 0.05, -0.71).normalize();
    /** How fast it turns over in the air, in radians per tick: evenly, all through its flight. */
    static final double SPIN = SwordMove.TOSS_TURNS * Mth.TWO_PI / (SwordMove.CATCH - SwordMove.TOSS);
    // Where along its reach to the tip it balances (and turns about): just above the guard, the way a sword does; and
    // how far that is from the grip in your own hands, in blocks.
    static final double BALANCE = 0.2;
    static final double OWN_BALANCE = BALANCE * SwordPainter.TIP * OWN_SWORD;
    // Where your fist lets go of it and catches it again, before your eyes: well out in front, so the whole flight
    // stays in sight; and how high its balance point rises over where it left the hand.
    private static final Vec3 RELEASE = new Vec3(0.26, -0.40, -1.26);
    private static final Vec3 CAUGHT = new Vec3(0.24, -0.30, -1.26);
    private static final double TOSS_HIGH = 0.5;
    // How it stands in the hand as it is tossed and caught: upright, a little forward, the flat towards you.
    static final Vec3 UPRIGHT = new Vec3(0.04, 0.99, -0.12).normalize();
    static final Vec3 FLAT = square(new Vec3(-1.0, 0.0, 0.0), UPRIGHT);
    // How far back the wrist is cocked before the flick, how far it turns ahead to meet the spinning sword, and how far
    // the caught sword carries it on before it springs back (radians).
    private static final double COCK = 0.8;
    private static final double MEET = 0.9;
    private static final double CARRY = 0.42;
    // How fast the blade and its edge turn in the air, per tick.
    static final Vec3 BLADE_TURN = TOSS_AXIS.cross(UPRIGHT).scale(SPIN);
    private static final Vec3 EDGE_TURN = TOSS_AXIS.cross(FLAT).scale(SPIN);
    // How fast the balance point falls (blocks per tick per tick) and how fast it leaves the hand, worked out below so
    // that it rises TOSS_HIGH and comes down right into the hand; and how fast the fist moves as it lets go and as it
    // catches, which is how fast the grip moves then.
    private static final double FALL;
    private static final Vec3 THROWN;
    private static final Vec3 LET_GO;
    private static final Vec3 CAUGHT_AT;

    // ---- Looking the sword over, and banging it on the shield ----

    // The blade as he looks it over: across your view from low on the right to high on the left; and from when to when
    // the wrist turns it over: a while after the catch, until a while before the first bang.
    private static final Vec3 LOOKED_AT = new Vec3(-0.55, 0.74, -0.40).normalize();
    static final float INSPECT_FROM = SwordMove.CATCH + 8.0F;
    static final float INSPECT_TO = SwordMove.KNOCK - 6.0F;
    // Where on the rim of the shield the blade comes down (in the shield's own blocks at scale 1: on its arched top,
    // left of the middle), how thick the rim is, how far along the blade it strikes and how far the edge is from the
    // middle of the blade there (at scale 1).
    private static final double RIM_X = -0.30;
    private static final double RIM_Y = 0.52 + 0.04 * (1.0 - (RIM_X / 0.46) * (RIM_X / 0.46));
    static final double RIM_THICK = 0.034;
    private static final double STRIKE_AT = 1.05;
    static final double BLADE_WIDE = 0.08;
    // The blade as it comes down on the rim: lying across the top of the shield, its edge leading down onto it; and the
    // way the blade turns as it comes down.
    private static final Vec3 KNOCKING = new Vec3(-0.95, 0.05, -0.30).normalize();
    static final Vec3 KNOCK_EDGE = square(new Vec3(0.0, -1.0, 0.25), KNOCKING);
    private static final Vec3 CHOP = KNOCKING.cross(KNOCK_EDGE).normalize();
    // The shield raised to meet the bangs: its middle, the way its face points and where its top is.
    private static final Vec3 RAISED = new Vec3(-0.44, -0.44, -0.88);
    private static final Vec3 RAISED_FACE = new Vec3(-0.32, 0.16, -1.0).normalize();
    private static final Vec3 RAISED_TOP = square(new Vec3(0.05, 1.0, 0.18), RAISED_FACE);
    // How far the fist is raised and the blade turned back up before the first bang and between the two (blocks and
    // radians), how long each swing down takes (ticks), and how much of its speed the blade keeps as it bounces off.
    private static final double WIND_UP = 0.17;
    private static final double WIND_TURN = 0.6;
    private static final double AGAIN_UP = 0.1;
    private static final double AGAIN_TURN = 0.32;
    private static final float WIND_TICKS = 2.5F;
    private static final float AGAIN_TICKS = 2.0F;
    private static final double BOUNCE = 0.33;
    /** Where the blade strikes the rim of the shield before your eyes (the same at both bangs). */
    static final Vec3 STRUCK;

    static {
        // The toss: the balance point leaves the hand, rises TOSS_HIGH and comes down into the hand where it catches
        // it, in the time between; the grip moves as the balance point does, less the turn of the blade about it.
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

    /**
     * Taking them out, on four tracks of their own. The sword hand comes up out of the game's own resting pose while
     * the sword grows out of the fist, dips, and flicks the sword up into the air (see {@link #flight}); the fist
     * follows through, sinks to wait under it, reaches up to meet it and rides it down as it catches it, then brings it
     * up before your eyes, holds it there while the wrist turns it over, raises it and brings it down on the rim of the
     * shield twice, bouncing off it each time. The wrist cocks back before the flick and snaps round with it, turns
     * ahead to meet the spin of the sword as it catches it, is carried on by it and springs back. The shield comes up
     * while it grows out of the ring's light, dips as its strap closes, swings back with the toss, gives with the
     * catch, and rises to meet each bang and gives under it. The body leads all of it by a moment: it dips before the
     * hand does, rises into the flick, leans back to watch the sword go up, gives with the catch, and turns into the
     * bangs.
     */
    private static void equip() {
        float toss = SwordMove.TOSS;
        float caught = SwordMove.CATCH;
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        // Each swing down onto the rim speeds up evenly from where it hangs raised to the moment it strikes, the
        // fastest there, and bounces off at a part of that speed.
        Vec3 hit = contact();
        Vec3 raised = hit.subtract(KNOCK_EDGE.scale(WIND_UP));
        Vec3 lifted = hit.subtract(KNOCK_EDGE.scale(AGAIN_UP));
        Vec3 strike = hit.subtract(raised).scale(2.0 / WIND_TICKS);
        Vec3 strikeAgain = hit.subtract(lifted).scale(2.0 / AGAIN_TICKS);
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
                at(INSPECT_FROM - 2.5F, false, 0.24, -0.36, -0.92),
                at(INSPECT_FROM, false, 0.21, -0.31, -0.84),
                at((INSPECT_FROM + INSPECT_TO) / 2.0F, false, 0.19, -0.29, -0.82),
                at(INSPECT_TO, false, 0.21, -0.30, -0.83),
                at(knock - WIND_TICKS, true, raised),
                moving(at(knock, false, hit), 0, strike, strike.scale(-BOUNCE)),
                at(again - AGAIN_TICKS, true, lifted),
                moving(at(again, false, hit), 0, strikeAgain, strikeAgain.scale(-0.8 * BOUNCE)),
                at(again + 1.8F, true, hit.subtract(KNOCK_EDGE.scale(0.03))),
                at(again + 5.0F, false, 0.42, -0.43, -0.93) });
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
                held(INSPECT_TO - 1.0F, -0.54, -0.61, -0.94, -0.50, 0.01, -1.0, 0.10, 1.0, 0.10),
                raised(knock - 3.0F, 0.0),
                moving(raised(knock, 0.0), Pose.SHIELD_FROM, new Vec3(0.0, 0.012, 0.0), KNOCK_EDGE.scale(0.03)),
                raised(knock + 1.8F, 1.0),
                moving(raised(again, 0.0), Pose.SHIELD_FROM, new Vec3(0.0, 0.01, 0.0), KNOCK_EDGE.scale(0.022)),
                raised(again + 1.8F, 0.75),
                held(again + 5.0F, -0.47, -0.49, -0.92, -0.43, 0.07, -1.0, 0.07, 1.0, 0.13) });
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
                leaning(knock - WIND_TICKS - 0.5F, 10, -0.02F),
                leaning(knock - 0.5F, -13, 0.10F),
                leaning(again - AGAIN_TICKS - 0.3F, -7, 0.06F),
                leaning(again - 0.5F, -12, 0.09F),
                leaning(again + 3.0F, -4, 0.05F) });
        MOVES.put(SwordMove.EQUIP, new Track[] { hand, new Track(Pose.BLADE, Pose.SHIELD_FROM, equipWrist()), shield,
                body });
    }

    /**
     * The wrist as they take shape: upright as the sword grows, cocked back before the flick and snapping round with
     * it, turned ahead to meet the spin of the sword as it comes down and carried on by it; then turning the blade
     * before your eyes from the one flat over its edge to the other, slowly at first and last while the blade swings a
     * little towards you; raised, and brought down edge first on the rim twice.
     */
    private static Key[] equipWrist() {
        float toss = SwordMove.TOSS;
        float caught = SwordMove.CATCH;
        float knock = SwordMove.KNOCK;
        float again = SwordMove.KNOCK_AGAIN;
        Key[] keys = new Key[22];
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
        for (int i = 0; i <= 6; i++) {
            double u = Ease.smoother(i / 6.0);
            double swing = 0.16 - 0.32 * u;
            Vec3 blade = Vectors.spin(LOOKED_AT, up, swing);
            Vec3 edge = Vectors.spin(square(Vectors.spin(facing, up, swing), blade), blade, Math.PI * u);
            keys[k++] = wrist(Mth.lerp(i / 6.0F, INSPECT_FROM, INSPECT_TO), false, blade, edge);
        }
        // Raised, and swung down onto the rim: evenly faster all the way, bouncing off it; lifted a little way and
        // swung down again, a lighter bang.
        Vec3 bladeDown = KNOCKING.subtract(chopped(KNOCKING, WIND_TURN)).scale(2.0 / WIND_TICKS);
        Vec3 edgeDown = KNOCK_EDGE.subtract(chopped(KNOCK_EDGE, WIND_TURN)).scale(2.0 / WIND_TICKS);
        Vec3 bladeAgain = KNOCKING.subtract(chopped(KNOCKING, AGAIN_TURN)).scale(2.0 / AGAIN_TICKS);
        Vec3 edgeAgain = KNOCK_EDGE.subtract(chopped(KNOCK_EDGE, AGAIN_TURN)).scale(2.0 / AGAIN_TICKS);
        keys[k++] = wrist(knock - WIND_TICKS, true, chopped(KNOCKING, WIND_TURN), chopped(KNOCK_EDGE, WIND_TURN));
        keys[k++] = moving(wrist(knock, false, KNOCKING, KNOCK_EDGE), bladeDown, edgeDown, bladeDown.scale(-BOUNCE),
                edgeDown.scale(-BOUNCE));
        keys[k++] = wrist(again - AGAIN_TICKS, true, chopped(KNOCKING, AGAIN_TURN), chopped(KNOCK_EDGE, AGAIN_TURN));
        keys[k++] = moving(wrist(again, false, KNOCKING, KNOCK_EDGE), bladeAgain, edgeAgain,
                bladeAgain.scale(-0.8 * BOUNCE), edgeAgain.scale(-0.8 * BOUNCE));
        keys[k++] = wrist(again + 1.8F, true, chopped(KNOCKING, 0.08), chopped(KNOCK_EDGE, 0.08));
        keys[k] = wrist(again + 5.0F, false, GUARD.blade(), GUARD.edge());
        return keys;
    }

    /** The blade (or its edge) as it lies on the rim at a bang, turned {@code angle} (radians) back up off it. */
    private static Vec3 chopped(Vec3 way, double angle) {
        return Vectors.spin(way, CHOP, -angle);
    }

    /**
     * A key of the shield raised to meet the bangs, and how far ({@code give}, 0 to 1) it has given under the blade:
     * pushed down along the way the blade struck, and tipped away from it.
     */
    private static Key raised(float tick, double give) {
        Vec3 tipped = Vectors.spin(RAISED_FACE, RAISED_FACE.cross(RAISED_TOP).normalize(), -0.07 * give);
        Vec3 middle = RAISED.add(KNOCK_EDGE.scale(0.035 * give));
        return held(tick, middle.x, middle.y, middle.z, tipped.x, tipped.y, tipped.z, RAISED_TOP.x, RAISED_TOP.y,
                RAISED_TOP.z);
    }

    // When the sword starts to grow out of the fist as they take shape, and how long it takes to grow, in ticks.
    private static final float SWORD_FROM = 1.0F;
    private static final float SWORD_GROWS = 7.0F;

    /** How far the sword has grown out of the ring's light while they take shape, 0 to 1 (1 for every other move). */
    static float swordGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? (float) Ease.smooth((t - SWORD_FROM) / SWORD_GROWS) : 1.0F;
    }

    /**
     * How far the shield has come out of the ring's light while they take shape, 0 to 1 (1 for every other move): it
     * builds up evenly from the moment the ring's light reaches the forearm until its strap closes (see
     * {@link SwordPainter#shield}).
     */
    static float shieldGrown(SwordMove move, float t) {
        return move == SwordMove.EQUIP ? Mth.clamp((t - 2.0F) / (SwordMove.SHIELD_LOCK - 2.0F), 0.0F, 1.0F) : 1.0F;
    }

    // ---- The toss ----

    /** Where the tossed sword is in the air: its grip, and where its blade and edge point. */
    record Flight(Vec3 grip, Vec3 blade, Vec3 edge) {
    }

    /** How far through its flight the tossed sword is as they take shape, 0 to 1, or -1 while it is in the hand. */
    static float tossed(SwordMove move, float t) {
        if (move != SwordMove.EQUIP || t <= SwordMove.TOSS || t >= SwordMove.CATCH) {
            return -1.0F;
        }
        return (t - SwordMove.TOSS) / (SwordMove.CATCH - SwordMove.TOSS);
    }

    /** The blade (or its edge) as the wrist has turned it {@code angle} (radians) along with the toss. */
    static Vec3 tossTurn(double angle, Vec3 way) {
        return Vectors.spin(way, TOSS_AXIS, angle);
    }

    /**
     * How fast a thrown thing falls (blocks per tick per tick) that rises {@code high} over where it leaves the hand and
     * lands {@code time} ticks later, {@code climb} per tick higher than it left (on average).
     */
    static double fall(double high, double climb, double time) {
        double half = time / 2.0;
        double b = 2.0 * half * climb - 2.0 * high;
        return (-b + Math.sqrt(Math.max(0.0, b * b - 4.0 * half * half * climb * climb))) / (2.0 * half * half);
    }

    /**
     * The tossed sword {@code t} ticks into taking them out, before your own eyes: its balance point falls freely from
     * where the fist let go of it, and it turns over evenly about that, so it lands in the fist at the catch exactly as
     * the fist moves then. Past the catch it flies on the same way (only ever seen when it broke up in the air).
     */
    static Flight flight(float t) {
        double tau = t - SwordMove.TOSS;
        Vec3 balance = RELEASE.add(UPRIGHT.scale(OWN_BALANCE)).add(THROWN.scale(tau)).subtract(0.0,
                0.5 * FALL * tau * tau, 0.0);
        Vec3 blade = tossTurn(SPIN * tau, UPRIGHT);
        return new Flight(balance.subtract(blade.scale(OWN_BALANCE)), blade, tossTurn(SPIN * tau, FLAT));
    }

    // ---- Banging it on the shield ----

    /**
     * Where the fist is as the blade comes down on the rim of the shield, before your eyes: the edge of the blade just
     * touches the rim there, {@link #STRIKE_AT} along it.
     */
    private static Vec3 contact() {
        double clear = RIM_THICK * OWN_SHIELD + BLADE_WIDE * OWN_SWORD;
        return STRUCK.subtract(KNOCK_EDGE.scale(clear)).subtract(KNOCKING.scale(STRIKE_AT * OWN_SWORD));
    }

    /** Where the blade strikes the rim of the shield before your eyes at a bang {@code t} ticks in. */
    private static Vec3 struck(float t) {
        Pose shield = Pose.of(raised(t, 0.0).numbers());
        return rim(shield.shield(), shield.shieldRight(), shield.top(), OWN_SHIELD);
    }

    /**
     * Where the blade strikes the rim of a shield of scale {@code scale} at a bang: {@code middle} is its middle,
     * {@code right} and {@code top} its own right and top.
     */
    static Vec3 rim(Vec3 middle, Vec3 right, Vec3 top, double scale) {
        return middle.add(right.scale(RIM_X * scale)).add(top.scale(RIM_Y * scale));
    }

    /**
     * How far a gleam of light has run up the blade as he looks it over, 0 (at the guard) to 1 (at the tip), or -1 when
     * none runs.
     */
    static float gleam(SwordMove move, float t) {
        if (move != SwordMove.EQUIP) {
            return -1.0F;
        }
        float u = (t - SwordMove.GLEAM) / 7.0F;
        return u < 0.0F || u > 1.0F ? -1.0F : u;
    }
}
