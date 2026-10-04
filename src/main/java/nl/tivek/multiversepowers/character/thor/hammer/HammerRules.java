package nl.tivek.multiversepowers.character.thor.hammer;

import javax.annotation.Nullable;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;

// The numbers both sides of the hammer go by: how far a drawn throw goes, how the dash to the hammer gets going and
// brakes into the catch, and how a throw that stopped comes to rest.
public final class HammerRules {
    // Throw and Follow: held this long the draw starts, at its shortest; full this long after the press; let go by
    // itself this long after.
    public static final int DRAW_FROM = 6;
    public static final int DRAW_FULL = 20;
    public static final int DRAW_LONGEST = 60;
    public static final double SHORTEST = 4.0;
    // The wait before the dash, while the lightning line grows between his hand and the hammer.
    public static final int WAIT = 10;
    // The dash gets going over this many ticks and brakes this hard (blocks a tick, each tick) into the catch.
    public static final int RAMP = 3;
    public static final double BRAKE = 0.35;
    // Ground this close under the hammer: he lands by it with it in his right hand, else he flies on with it.
    public static final double GROUND_BELOW = 2.5;
    // A wall in the dash's way lifts him this much a tick; stuck this long, he gives up.
    public static final double LIFT = 0.6;
    public static final int STUCK = 8;
    // He arrives this close to it (times his size); the server takes his word up to this much further off.
    public static final double THERE = 1.4;
    public static final double SLACK = 1.5;

    private HammerRules() {
    }

    // How far a throw drawn back for `held` ticks goes: from SHORTEST once the draw starts up to `longest` once full.
    public static double drawn(int held, double longest) {
        double full = Math.max(SHORTEST, longest);
        double u = Mth.clamp((held - DRAW_FROM) / (double) (DRAW_FULL - DRAW_FROM), 0.0, 1.0);
        return SHORTEST + (full - SHORTEST) * u;
    }

    // How far the dash goes in its tick `age` with `gap` blocks left, at `pace` blocks a tick once going: up to speed
    // over its first ticks, slowing over its last so it brakes into the catch and never goes past.
    public static double dashStep(int age, double gap, double pace) {
        double ramp = Math.min(1.0, (age + 1.0) / RAMP);
        double brake = Math.max(0.05, Math.sqrt(2.0 * BRAKE * Math.max(0.0, gap)));
        return Math.min(Math.max(0.0, gap), Math.min(pace * ramp, brake));
    }

    // How a throw that stopped rests: on top of a block it lies, in the side or underside of one it sticks, stopped by
    // a creature or the end of its reach (no face) it hangs in the air.
    public static byte restOn(@Nullable Direction face) {
        if (face == null) {
            return ThrownHammer.HANGING;
        }
        return face == Direction.UP ? ThrownHammer.LYING : ThrownHammer.STUCK;
    }
}
