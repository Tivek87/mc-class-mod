package nl.tivek.multiversepowers.faction.rules;

import nl.tivek.multiversepowers.faction.Standing;

// One faction as the world defines it: its words, how it starts toward players (`attitude`), and its reputation: a
// player's standing with it runs from -100 to 100, hostile at `hostileAt` and under, friendly at `friendlyAt` and
// over, and between them neutral. A player starts in the zone of its attitude; what they earn or lose is counted from
// there.
public record Faction(String id, String name, String description, String lore, int color, Standing attitude,
        boolean reputation, int hostileAt, int friendlyAt, boolean help, boolean open) {
    public static final int LOWEST = -100;
    public static final int HIGHEST = 100;
    public static final int HOSTILE_AT = -30;
    public static final int FRIENDLY_AT = 30;
    // How far inside its zone a player starts with a faction that is hostile or friendly from the start.
    private static final int INSIDE = 30;

    // Where every player starts with it.
    public int start() {
        if (this.attitude == Standing.HOSTILE) {
            return Math.max(LOWEST, this.hostileAt - INSIDE);
        }
        return this.attitude == Standing.FRIENDLY ? Math.min(HIGHEST, this.friendlyAt + INSIDE) : 0;
    }

    // What a member has earned at least: enough to stand half-way between its thresholds, clear of being cast out.
    public int member() {
        return this.clamp((this.hostileAt + this.friendlyAt) / 2 - this.start());
    }

    // A player's reputation with it, having earned (or lost) `earned` since the start.
    public int reputation(int earned) {
        return Math.max(LOWEST, Math.min(HIGHEST, this.start() + (this.reputation ? earned : 0)));
    }

    // How it stands to a player who earned `earned`.
    public Standing zone(int earned) {
        int now = this.reputation(earned);
        if (now <= this.hostileAt) {
            return Standing.HOSTILE;
        }
        if (now >= this.friendlyAt) {
            return Standing.FRIENDLY;
        }
        return Standing.NEUTRAL;
    }

    // What `earned` may be at most and at least, so the reputation stays on the scale.
    public int clamp(int earned) {
        return Math.max(LOWEST - this.start(), Math.min(HIGHEST - this.start(), earned));
    }
}
