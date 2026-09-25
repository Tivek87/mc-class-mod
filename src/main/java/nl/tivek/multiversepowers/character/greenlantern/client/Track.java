package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.ArrayDeque;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPath;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter;
import static nl.tivek.multiversepowers.character.greenlantern.client.TrackedConstructs.clientTicks;

/**
 * The server updates of one construct, played back one per client tick. A construct that moves by itself (a
 * fist or bolt on its way, a landing slam) also runs on a clock of its own: the server says how long ago it
 * set off, and from then on the client counts on by itself, so it moves at an even pace however unevenly the
 * updates come in.
 */
final class Track {
    // A construct the server stopped updating (out of range, or a lost packet) disappears.
    private static final int TIMEOUT = 10;
    // A plane flies a path both sides work out alike: it stays this long without a word (a hitch of the server, or out
    // of its reach for a moment), in ticks.
    static final int PLANE_KEEP = 100;
    // A giant hand stays this long without a word (a hitch of the server) and then simply goes: it only breaks up when
    // the server says it was let go.
    private static final int HAND_KEEP = 60;
    // How a clock keeps time with the server's (see Track#retime): how far ahead of the last word it may be before the
    // server seems to run behind and where it is drifts later (at most this much, and this much of how far past that
    // it is, per tick); how far ahead is a hitch it gives up on; how hard it catches up (its pace, next to how far off
    // it is); how slow and how fast it may run and how much its pace may change in a tick; and how far ahead of the
    // last word it may run at all (what flies a path of its own a little further) and how hard it slows down as it gets
    // there.
    private static final double LEAD_OK = 2.5;
    private static final double DRIFT = 0.3;
    private static final double DRIFT_PULL = 0.3;
    private static final double LEAD_LOST = 40.0;
    private static final double CATCH_UP = 0.2;
    private static final double SLOWEST = 0.25;
    private static final double FASTEST = 2.0;
    private static final double PACE_CHANGE = 0.2;
    private static final double AHEAD = 6.0;
    private static final double AHEAD_OWN = 12.0;
    // A fist or bolt on its way runs no further ahead than this, slowing down only over the last tick of it (its own
    // pull): past it, it could fly on through what it struck.
    private static final double AHEAD_PATH = 2.0;
    private static final double AHEAD_PATH_PULL = 1.0;
    private static final double AHEAD_PULL = 0.3;
    // Updates waiting beyond this many are skipped, so a network hiccup never leaves one lagging behind.
    private static final int MAX_WAITING = 2;

    private final ArrayDeque<ConstructPayload> waiting = new ArrayDeque<>();
    ConstructPayload previous;
    ConstructPayload current;
    // The newest update, the moment it arrives.
    ConstructPayload latest;
    int lastSeen;
    // The client time it set off, the most the server told it has aged, and the way it flies (null: none).
    double start = Double.NaN;
    int told = -1;
    // Its clock as it is drawn (see clock): what it read on client tick shownAt, and how fast it runs now (ticks
    // per tick). It glides after the server's (see retime): it never jumps, never runs in steps, never stops dead.
    private double shown;
    private int shownAt;
    private double pace = 1.0;
    // A missile of an air strike: where it was on each tick it was told of (see Flown); null for anything else.
    @Nullable
    Flown flown;
    @Nullable
    ConstructPath path;
    // Where a steered one was drawn last, and the way it pointed: once it stops it falls apart right there.
    @Nullable
    Vec3 lastCenter;
    @Nullable
    Vec3 lastWay;
    // How long it stays without a word from the server: a round from a minigun is told about only once, as it is
    // fired, and flies on by itself.
    private final int keep;
    // True for what the server tells about only once (see sentOnce): its clock runs on by itself from there.
    private final boolean once;
    // The age of the first update that told of the variant it has now: a Light Bubble's pound is timed from there.
    int variantSince;

    Track(ConstructPayload first) {
        this.previous = first;
        this.current = first;
        this.latest = first;
        this.lastSeen = clientTicks;
        this.keep = first.shape() == ConstructPayload.BULLET ? PlanePainter.bulletTicks(first)
                : first.shape() == ConstructPayload.BLAST ? PlanePainter.BLAST_TICKS + 8
                : first.shape() == ConstructPayload.POUND ? BubblePainter.POUND_TICKS + 2
                : first.shape() == ConstructPayload.PLANE ? PLANE_KEEP
                : first.shape() == ConstructPayload.HAND ? HAND_KEEP : TIMEOUT;
        this.once = sentOnce(first.shape());
        this.variantSince = first.age();
        this.time(first);
    }

    void add(ConstructPayload update) {
        this.waiting.add(update);
        if (update.variant() != this.latest.variant()) {
            this.variantSince = update.age();
        }
        this.latest = update;
        this.lastSeen = clientTicks;
        this.time(update);
    }

    private void time(ConstructPayload update) {
        if (update.path() == null && !timed(update.shape())) {
            return;
        }
        if (update.path() != null) {
            this.path = update.path();
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        double setOff = clientTicks + partialTick - update.age();
        // The update that came through quickest tells best when it really set off.
        if (Double.isNaN(this.start)) {
            this.start = setOff;
            this.shownAt = clientTicks;
            this.shown = clientTicks - setOff;
        } else {
            this.start = Math.min(this.start, setOff);
        }
        this.told = Math.max(this.told, update.age());
    }

    /**
     * Ticks since it set off by the client's own clock: smooth, keeping time with the server's (see retime). What
     * the server tells about only once plays on by itself, however long ago that was.
     */
    double clock(float partialTick) {
        if (Double.isNaN(this.start)) {
            return Math.max(0, this.told);
        }
        if (this.once) {
            return Math.max(0.0, clientTicks + partialTick - this.start);
        }
        return Math.max(0.0, this.shown + (clientTicks - this.shownAt + partialTick) * this.pace);
    }

    /**
     * Once every client tick: its clock glides after the server's. Where the server is, is where its quickest
     * update says (see time), drifting later a little at a time while the server runs behind. The clock speeds up
     * or slows down gently to get there, so a late update, a hitch or a slow server never makes it jump, step or
     * stop dead. It runs ahead of the server's last word by a few ticks at most (what flies a path of its own, see
     * ownPath, a little further), easing to a stop when the server hitches and on again when it goes on. A missile
     * that struck is heard of no more: its clock runs on as it was, while it breaks up.
     */
    void retime() {
        if (this.once || Double.isNaN(this.start)) {
            return;
        }
        this.shown += (clientTicks - this.shownAt) * this.pace;
        this.shownAt = clientTicks;
        if (this.flown != null && this.flown.ends < Double.POSITIVE_INFINITY) {
            // Heard of no more: it eases back to the server's own pace (never stuck where a hitch had slowed it).
            this.pace += Mth.clamp(1.0 - this.pace, -PACE_CHANGE, PACE_CHANGE);
            return;
        }
        double lead = clientTicks - this.start - this.told;
        if (lead > LEAD_LOST) {
            this.start = clientTicks - this.told - 1.0;
        } else if (lead > LEAD_OK) {
            this.start += Math.min(DRIFT, (lead - LEAD_OK) * DRIFT_PULL);
        }
        double off = clientTicks - this.start - this.shown;
        if (Math.abs(off) > LEAD_LOST) {
            this.shown = clientTicks - this.start;
            this.pace = 1.0;
            return;
        }
        double pace = Mth.clamp(1.0 + off * CATCH_UP, SLOWEST, FASTEST);
        pace = Mth.clamp(pace, this.pace - PACE_CHANGE, this.pace + PACE_CHANGE);
        boolean own = ownPath(this.latest.shape());
        boolean onItsWay = !own && this.path != null;
        double ahead = own ? AHEAD_OWN : onItsWay ? AHEAD_PATH : AHEAD;
        double pull = onItsWay ? AHEAD_PATH_PULL : AHEAD_PULL;
        this.pace = Math.min(pace, Math.max(0.0, (this.told + ahead - this.shown) * pull));
    }

    /** Keeps the time of the plane it left: its clock is the plane's, less the tick it was let go on. */
    void follow(Track plane, int fired) {
        this.start = Math.min(this.start, plane.start + fired);
        this.shown = plane.shown - fired;
        this.shownAt = plane.shownAt;
        this.pace = plane.pace;
    }

    void advance() {
        this.previous = this.current;
        while (this.waiting.size() > MAX_WAITING) {
            this.waiting.poll();
        }
        if (!this.waiting.isEmpty()) {
            this.current = this.waiting.poll();
        }
    }

    boolean timedOut() {
        return clientTicks - this.lastSeen > this.keep;
    }

    /**
     * True for the shapes the server tells about only once, as they set off: a round from a minigun, a missile's blast,
     * a slam of a pound. Their clocks run on by themselves.
     */
    private static boolean sentOnce(int shape) {
        return shape == ConstructPayload.BULLET || shape == ConstructPayload.BLAST || shape == ConstructPayload.POUND;
    }

    /**
     * True for the shapes that fly a path of their own, worked out alike on both sides: the air strike's plane and its
     * missiles. Their clocks run on a little longer while the server says nothing, as the plane flies on.
     */
    private static boolean ownPath(int shape) {
        return shape == ConstructPayload.PLANE || shape == ConstructPayload.MISSILE;
    }

    /** True for the shapes that play along a timeline of their own, from how long ago they set off. */
    private static boolean timed(int shape) {
        return switch (shape) {
            case ConstructPayload.SLAM, ConstructPayload.SCAN, ConstructPayload.HAND, ConstructPayload.BEAM,
                    ConstructPayload.PLANE, ConstructPayload.MISSILE, ConstructPayload.BULLET, ConstructPayload.BLAST,
                    ConstructPayload.BUBBLE, ConstructPayload.SWORD, ConstructPayload.POUND -> true;
            default -> false;
        };
    }
}
