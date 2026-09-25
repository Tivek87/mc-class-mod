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

final class Track {
    private static final int TIMEOUT = 10;
    static final int PLANE_KEEP = 100;
    private static final int HAND_KEEP = 60;
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
    private static final double AHEAD_PATH = 2.0;
    private static final double AHEAD_PATH_PULL = 1.0;
    private static final double AHEAD_PULL = 0.3;
    private static final int MAX_WAITING = 2;

    private final ArrayDeque<ConstructPayload> waiting = new ArrayDeque<>();
    ConstructPayload previous;
    ConstructPayload current;
    ConstructPayload latest;
    int lastSeen;
    double start = Double.NaN;
    int told = -1;
    private double shown;
    private int shownAt;
    private double pace = 1.0;
    @Nullable
    Flown flown;
    @Nullable
    ConstructPath path;
    @Nullable
    Vec3 lastCenter;
    @Nullable
    Vec3 lastWay;
    private final int keep;
    private final boolean once;
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
        if (Double.isNaN(this.start)) {
            this.start = setOff;
            this.shownAt = clientTicks;
            this.shown = clientTicks - setOff;
        } else {
            // The update that came through quickest tells best when it really set off.
            this.start = Math.min(this.start, setOff);
        }
        this.told = Math.max(this.told, update.age());
    }

    double clock(float partialTick) {
        if (Double.isNaN(this.start)) {
            return Math.max(0, this.told);
        }
        if (this.once) {
            return Math.max(0.0, clientTicks + partialTick - this.start);
        }
        return Math.max(0.0, this.shown + (clientTicks - this.shownAt + partialTick) * this.pace);
    }

    void retime() {
        if (this.once || Double.isNaN(this.start)) {
            return;
        }
        this.shown += (clientTicks - this.shownAt) * this.pace;
        this.shownAt = clientTicks;
        if (this.flown != null && this.flown.ends < Double.POSITIVE_INFINITY) {
            // Heard of no more: it eases back to the server's own pace, never stuck where a hitch had slowed it.
            this.pace += Mth.clamp(1.0 - this.pace, -PACE_CHANGE, PACE_CHANGE);
            return;
        }
        double lead = clientTicks - this.start - this.told;
        if (lead > LEAD_LOST) {
            // A long hitch: snap forward instead of slowly drifting to catch up.
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

    private static boolean sentOnce(int shape) {
        return shape == ConstructPayload.BULLET || shape == ConstructPayload.BLAST || shape == ConstructPayload.POUND;
    }

    private static boolean ownPath(int shape) {
        return shape == ConstructPayload.PLANE || shape == ConstructPayload.MISSILE;
    }

    private static boolean timed(int shape) {
        return switch (shape) {
            case ConstructPayload.SLAM, ConstructPayload.SCAN, ConstructPayload.HAND, ConstructPayload.BEAM,
                    ConstructPayload.PLANE, ConstructPayload.MISSILE, ConstructPayload.BULLET, ConstructPayload.BLAST,
                    ConstructPayload.BUBBLE, ConstructPayload.SWORD, ConstructPayload.POUND -> true;
            default -> false;
        };
    }
}
