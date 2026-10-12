package nl.tivek.multiversepowers.character.docock;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// Where Doctor Octopus's tentacles are, worked out the same by the server and every game: numbered 1 top left, 2 top
// right, 3 bottom left, 4 bottom right as they sit on his back (index 0 to 3). A held creature hangs before him, the
// upper tentacles' high over his shoulders, the lower ones' lower and wider; a move swings it from there.
public final class TentacleReach {
    public static final int COUNT = 4;
    // Every tentacle is this long (blocks), always: drawn so on his back, on the ground or striking, and no creature
    // further than this is reached.
    public static final double LENGTH = 12.0;
    // The order free tentacles take hold in (2, 1, 4, 3) and go down to the ground in (3, 4, 1, 2).
    public static final int[] GRAB_ORDER = { 1, 0, 3, 2 };
    public static final int[] GROUND_ORDER = { 2, 3, 0, 1 };
    // How many tentacles each stance puts on the ground: 4 free, 2 and 2, 1 and 3, all 4 down.
    public static final int[] GROUNDED = { 0, 2, 3, 4 };
    public static final int STANCES = GROUNDED.length;

    // A tentacle's move, as the server and every game show it, and how long it lasts in ticks.
    public static final int NONE = 0;
    public static final int SLAP = 1;
    public static final int SLAM = 2;
    public static final int WEAPON = 3;
    public static final int CLASH = 4;
    public static final int GRAB = 5;
    public static final int THROW = 6;
    public static final int MOVE_TICKS = 8;
    // The tick of a move its blow lands on.
    public static final int STRIKE_TICK = 4;

    private TentacleReach() {
    }

    public static boolean upper(int index) {
        return index < 2;
    }

    // +1 on his left (tentacles 1 and 3), -1 on his right.
    public static double side(int index) {
        return index % 2 == 0 ? 1.0 : -1.0;
    }

    public static Vec3 forward(float bodyYaw) {
        float r = bodyYaw * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(r), 0.0, Mth.cos(r));
    }

    public static Vec3 left(float bodyYaw) {
        float r = bodyYaw * Mth.DEG_TO_RAD;
        return new Vec3(Mth.cos(r), 0.0, Mth.sin(r));
    }

    // Where a tentacle holds a creature `width` wide, its middle, from where he stands (`feet`).
    public static Vec3 hold(Vec3 feet, float bodyYaw, int index, double width) {
        Vec3 forward = forward(bodyYaw);
        Vec3 left = left(bodyYaw);
        double out = 2.6 + width * 0.5;
        if (upper(index)) {
            return feet.add(left.scale(side(index) * (1.6 + width * 0.4))).add(0.0, 3.2, 0.0)
                    .add(forward.scale(out + 0.4));
        }
        return feet.add(left.scale(side(index) * (2.4 + width * 0.5))).add(0.0, 1.4, 0.0).add(forward.scale(out));
    }

    // How far a move carries the held creature from its hold, at `age` ticks into it and back: a slam 3 blocks out
    // along `way`, a blow with it or a clash all the way of `way` (from the hold to its target or the clash's middle).
    public static Vec3 swing(int move, float age, Vec3 way) {
        if (age < 0.0F || age > MOVE_TICKS) {
            return Vec3.ZERO;
        }
        double t = Mth.sin(Mth.PI * Mth.clamp(age / MOVE_TICKS, 0.0F, 1.0F));
        return switch (move) {
            case SLAM -> way.scale(3.0 * t);
            case WEAPON, CLASH -> way.scale(t);
            default -> Vec3.ZERO;
        };
    }
}
