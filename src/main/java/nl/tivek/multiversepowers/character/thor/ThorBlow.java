package nl.tivek.multiversepowers.character.thor;

import javax.annotation.Nullable;

// Thor's blows: twenty with his hands and three kicks for his combo without the hammer, four swings of the hammer for
// his combo with it, and the hammer's uppercut, its two throws and a grab's two endings, which only their own moves
// throw. His game chains them into a combo that flows (client/ThorCombo) and poses them (client/ThorBlowKeys); the
// server lands each on its tick (ThorBlows).
// He never spins round in a blow. Columns: what throws it, how high it lands, its length, the tick it lands, the tick
// the next may start, its reach in blocks and its width in degrees either side of the look, whether it hits all it
// sweeps through, then its damage (times the combo's), how hard it throws what it hits away and up, whether it ends a
// combo, how far it carries him forward (blocks a tick), and which set it belongs to.
public enum ThorBlow {
    JAB(Limb.LEFT, Height.HIGH, 7, 2, 4, 3.0, 25, false, 0.7, 0.25, 0.05, false, 0.0),
    CROSS(Limb.RIGHT, Height.HIGH, 8, 3, 5, 3.2, 25, false, 1.0, 0.45, 0.08, false, 0.08),
    LEAD_HOOK(Limb.LEFT, Height.HIGH, 9, 4, 6, 2.8, 55, false, 1.1, 0.5, 0.1, false, 0.0),
    REAR_HOOK(Limb.RIGHT, Height.HIGH, 9, 4, 6, 2.8, 55, false, 1.2, 0.55, 0.1, false, 0.0),
    OVERHAND(Limb.RIGHT, Height.HIGH, 11, 5, 7, 3.1, 30, false, 1.4, 0.6, 0.0, false, 0.1),
    BACKFIST(Limb.LEFT, Height.HIGH, 8, 3, 5, 3.0, 35, false, 0.9, 0.35, 0.05, false, 0.0),
    HAMMER_FIST(Limb.RIGHT, Height.LOW, 11, 6, 8, 2.9, 30, false, 1.4, 0.2, -0.3, false, 0.0),
    ELBOW(Limb.RIGHT, Height.HIGH, 8, 3, 5, 2.4, 50, false, 1.2, 0.5, 0.05, false, 0.05),
    LEAD_ELBOW(Limb.LEFT, Height.HIGH, 8, 3, 5, 2.4, 50, false, 1.1, 0.45, 0.05, false, 0.05),
    RISING_ELBOW(Limb.RIGHT, Height.HIGH, 9, 4, 6, 2.4, 35, false, 1.2, 0.2, 0.55, false, 0.0),
    PALM_STRIKE(Limb.RIGHT, Height.MID, 9, 4, 6, 3.0, 30, false, 1.0, 0.9, 0.15, false, 0.08),
    DOUBLE_PALM(Limb.BOTH, Height.MID, 12, 6, 8, 3.1, 40, true, 1.3, 1.4, 0.25, true, 0.1),
    BODY_HOOK(Limb.LEFT, Height.LOW, 9, 4, 6, 2.6, 50, false, 1.1, 0.4, 0.05, false, 0.0),
    BODY_SHOT(Limb.RIGHT, Height.LOW, 9, 4, 6, 3.0, 30, false, 1.1, 0.45, 0.05, false, 0.05),
    SUPERMAN_PUNCH(Limb.RIGHT, Height.HIGH, 14, 8, 10, 3.6, 30, false, 1.7, 1.0, 0.25, true, 0.55),
    KNIFE_HAND(Limb.LEFT, Height.HIGH, 9, 4, 6, 2.8, 45, false, 1.0, 0.35, 0.0, false, 0.0),
    SHOVEL_HOOK(Limb.RIGHT, Height.MID, 10, 5, 7, 2.6, 45, false, 1.2, 0.35, 0.35, false, 0.0),
    LEAD_STRAIGHT(Limb.LEFT, Height.HIGH, 9, 4, 6, 3.4, 25, false, 1.1, 0.5, 0.08, false, 0.28),
    HAYMAKER(Limb.RIGHT, Height.HIGH, 13, 7, 9, 3.0, 75, true, 1.7, 1.15, 0.2, true, 0.05),
    THUNDER_PUNCH(Limb.RIGHT, Height.HIGH, 15, 8, 11, 3.4, 30, false, 2.2, 1.6, 0.4, true, 0.22),
    FRONT_KICK(Limb.RIGHT_LEG, Height.MID, 12, 5, 8, 3.4, 30, false, 1.3, 1.3, 0.15, false, 0.05),
    ROUNDHOUSE(Limb.RIGHT_LEG, Height.HIGH, 13, 6, 9, 3.2, 70, true, 1.5, 1.0, 0.25, false, 0.0),
    SIDE_KICK(Limb.LEFT_LEG, Height.MID, 12, 6, 9, 3.5, 30, false, 1.5, 1.5, 0.1, false, 0.05),
    HAMMER_SWING(Limb.RIGHT, Height.HIGH, 12, 6, 8, 3.6, 70, true, 1.8, 1.3, 0.2, false, 0.05, Kit.HAMMER),
    HAMMER_BACKHAND(Limb.RIGHT, Height.HIGH, 11, 5, 7, 3.4, 60, true, 1.6, 1.1, 0.15, false, 0.0, Kit.HAMMER),
    HAMMER_THRUST(Limb.RIGHT, Height.MID, 10, 4, 6, 3.8, 25, false, 1.5, 1.5, 0.1, false, 0.15, Kit.HAMMER),
    HAMMER_SMASH(Limb.RIGHT, Height.MID, 14, 7, 10, 3.4, 40, true, 2.3, 0.5, 0.35, true, 0.0, Kit.HAMMER),
    HAMMER_UPPERCUT(Limb.RIGHT, Height.HIGH, 12, 5, 9, 3.6, 40, false, 2.0, 0.3, 1.6, true, 0.1, Kit.MOVE),
    // A grab's endings, with the creature in his right fist: hurled away, and slammed down onto its back.
    GRAB_HURL(Limb.RIGHT, Height.MID, 14, 9, 11, 3.0, 30, false, 1.0, 0.9, 0.15, true, 0.0, Kit.MOVE),
    GRAB_SLAM(Limb.RIGHT, Height.LOW, 16, 10, 12, 2.9, 30, false, 1.4, 0.2, -0.3, true, 0.0, Kit.MOVE),
    // The hammer's throws, only shown: hurled from his right hand on the ground, from his left in flight (a Storm
    // Throw). Mjolnir lets go of it as the arm comes through (its landing tick) and flies it.
    HAMMER_THROW(Limb.RIGHT, Height.HIGH, 11, 2, 7, 0.0, 0, false, 0.0, 0.0, 0.0, true, 0.05, Kit.MOVE),
    STORM_THROW(Limb.LEFT, Height.HIGH, 12, 2, 8, 0.0, 0, false, 0.0, 0.0, 0.0, true, 0.0, Kit.MOVE);

    // Every blow is this much longer than its numbers (ticks, landing, next) and keys say.
    public static final float PACE = 1.3F;

    // Which combo a blow is thrown in: without the hammer, with it, or only by an ability of its own.
    public enum Kit {
        FISTS,
        HAMMER,
        MOVE
    }

    // Which hand or foot throws it; side is +1 for his right, -1 for his left, 0 for both hands.
    public enum Limb {
        RIGHT(1, false),
        LEFT(-1, false),
        BOTH(0, false),
        RIGHT_LEG(1, true),
        LEFT_LEG(-1, true);

        private final int side;
        private final boolean leg;

        Limb(int side, boolean leg) {
            this.side = side;
            this.leg = leg;
        }

        public int side() {
            return this.side;
        }

        public boolean leg() {
            return this.leg;
        }
    }

    // Where it lands, as a band over his feet in blocks: what it hits must reach into it.
    public enum Height {
        LOW(0.0, 1.4),
        MID(0.3, 2.0),
        HIGH(0.8, 2.8);

        private final double from;
        private final double to;

        Height(double from, double to) {
            this.from = from;
            this.to = to;
        }

        public double from() {
            return this.from;
        }

        public double to() {
            return this.to;
        }
    }

    private final Limb limb;
    private final Height height;
    private final int ticks;
    private final int hit;
    private final int ready;
    private final double reach;
    private final double arc;
    private final boolean sweep;
    private final double power;
    private final double push;
    private final double lift;
    private final boolean finisher;
    private final double lunge;
    private final Kit kit;

    ThorBlow(Limb limb, Height height, int ticks, int hit, int ready, double reach, double arc, boolean sweep,
            double power, double push, double lift, boolean finisher, double lunge) {
        this(limb, height, ticks, hit, ready, reach, arc, sweep, power, push, lift, finisher, lunge, Kit.FISTS);
    }

    ThorBlow(Limb limb, Height height, int ticks, int hit, int ready, double reach, double arc, boolean sweep,
            double power, double push, double lift, boolean finisher, double lunge, Kit kit) {
        this.kit = kit;
        this.limb = limb;
        this.height = height;
        this.ticks = ticks;
        this.hit = hit;
        this.ready = ready;
        this.reach = reach;
        this.arc = arc;
        this.sweep = sweep;
        this.power = power;
        this.push = push;
        this.lift = lift;
        this.finisher = finisher;
        this.lunge = lunge;
    }

    @Nullable
    public static ThorBlow byIndex(int index) {
        ThorBlow[] all = values();
        return index >= 0 && index < all.length ? all[index] : null;
    }

    public Limb limb() {
        return this.limb;
    }

    public Height height() {
        return this.height;
    }

    public int ticks() {
        return Math.round(this.ticks * PACE);
    }

    public int hit() {
        return Math.round(this.hit * PACE);
    }

    public int ready() {
        return Math.round(this.ready * PACE);
    }

    public double reach() {
        return this.reach;
    }

    public double arc() {
        return this.arc;
    }

    public boolean sweep() {
        return this.sweep;
    }

    public double power() {
        return this.power;
    }

    public double push() {
        return this.push;
    }

    public double lift() {
        return this.lift;
    }

    public boolean finisher() {
        return this.finisher;
    }

    public double lunge() {
        return this.lunge;
    }

    public boolean kick() {
        return this.limb.leg();
    }

    public Kit kit() {
        return this.kit;
    }

    // What his right hand alone may throw while his left holds the hammer in flight.
    public boolean oneHanded() {
        return this.kit == Kit.FISTS && this.limb == Limb.RIGHT;
    }

    // A grab's own ending: thrown with the hand that holds the creature by the throat.
    public boolean grabbing() {
        return this == GRAB_HURL || this == GRAB_SLAM;
    }

    // Whether a grab's ending is under way `age` ticks into this blow.
    public static boolean grabbing(int index, float age) {
        ThorBlow blow = byIndex(index);
        return blow != null && blow.grabbing() && age < blow.ticks();
    }
}
