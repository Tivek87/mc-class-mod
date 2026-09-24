package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPath;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightShield;
import nl.tivek.multiversepowers.character.greenlantern.ability.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.body.LanternArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RingSpot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.HandPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.RingSight;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

/**
 * Keeps the hard-light constructs the server sends and draws them (see ConstructPainter), blended between
 * ticks so they fly smoothly. Updates are played back one per client tick, so updates that arrive
 * unevenly over the network still move evenly.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientConstructs {
    // A construct the server stopped updating (out of range, or a lost packet) disappears.
    private static final int TIMEOUT = 10;
    // A plane flies a path both sides work out alike: it stays this long without a word (a hitch of the server, or out
    // of its reach for a moment), in ticks.
    private static final int PLANE_KEEP = 100;
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
    private static final double AHEAD_PULL = 0.3;
    // Updates waiting beyond this many are skipped, so a network hiccup never leaves one lagging behind.
    private static final int MAX_WAITING = 2;
    // How long the shockwave of a slam shakes the view, in ticks.
    private static final double SHAKE_TICKS = 8.0;
    // How long the crash of an air strike's plane shakes the view, in ticks, and up to how far away, in blocks.
    private static final double CRASH_SHAKE_TICKS = 30.0;
    private static final double CRASH_SHAKE_RANGE = 140.0;
    // The same for the small blast of one of its missiles.
    private static final double BLAST_SHAKE_TICKS = 10.0;
    private static final double BLAST_SHAKE_RANGE = 24.0;
    // The same for a slam of a Light Bubble pounded into the ground.
    private static final double POUND_SHAKE_TICKS = 9.0;
    private static final double POUND_SHAKE_RANGE = 26.0;
    // The same for the axe a pair of giant hands chops into the ground, and for a giant middle finger bursting out of
    // it, with how hard each shakes it right next to it.
    private static final double AXE_SHAKE_TICKS = 10.0;
    private static final double AXE_SHAKE_RANGE = 24.0;
    private static final double AXE_SHAKE = 0.8;
    private static final double FINGER_SHAKE_TICKS = 8.0;
    private static final double FINGER_SHAKE_RANGE = 18.0;
    private static final double FINGER_SHAKE = 0.5;

    // The beam starts on the line from your eye through your own hand, but this much nearer than the hand
    // itself: on screen that is the same spot, and it keeps the beam from starting inside a wall.
    private static final double RING_NEAR = 0.45;
    // The ram cone points where its owner looks below the first speed, and the way he flies above the second, in
    // blocks per tick; seen from his own eyes its middle hangs this far in front of them.
    private static final double RAM_LOOK = 0.08;
    private static final double RAM_ALONG = 0.3;
    private static final double RAM_OWN_AHEAD = 0.85;

    private static final Map<Integer, Track> CONSTRUCTS = new HashMap<>();
    // Planes whose maker let go of them in the air: they break up where they were. By id: the last word about it, the
    // clock it had then, and the client tick it was let go on.
    private static final Map<Integer, Broken> BROKEN = new HashMap<>();
    // Giant hands whose maker let go of them before they were done: they break up where they were. By id, as above.
    private static final Map<Integer, Broken> BROKEN_HANDS = new HashMap<>();
    // How long a plane that was let go takes to break up and be gone, in ticks.
    private static final int BROKEN_TICKS = 42;
    // The client time each player's newest bolt left the ring, by the id of its owner (see boltAge), and how long that
    // is kept, in ticks.
    private static final Map<Integer, Double> BOLTS = new HashMap<>();
    private static final int BOLT_MEMORY = 40;
    // The tick of its plane's clock each pylon of each player's jets last fired a missile on (see launched), by the id
    // of the player and the pylon.
    private static final Map<Long, Integer> LAUNCHES = new HashMap<>();
    private static int clientTicks;

    /** A plane or giant hand let go of before it was done, breaking up. */
    private record Broken(ConstructPayload construct, double clock, int since) {
    }

    private ClientConstructs() {
    }

    /**
     * The server updates of one construct, played back one per client tick. A construct that moves by itself (a
     * fist or bolt on its way, a landing slam) also runs on a clock of its own: the server says how long ago it
     * set off, and from then on the client counts on by itself, so it moves at an even pace however unevenly the
     * updates come in.
     */
    private static final class Track {
        private final ArrayDeque<ConstructPayload> waiting = new ArrayDeque<>();
        private ConstructPayload previous;
        private ConstructPayload current;
        // The newest update, the moment it arrives.
        private ConstructPayload latest;
        private int lastSeen;
        // The client time it set off, the most the server told it has aged, and the way it flies (null: none).
        private double start = Double.NaN;
        private int told = -1;
        // Its clock as it is drawn (see clock): what it read on client tick shownAt, and how fast it runs now (ticks
        // per tick). It glides after the server's (see retime): it never jumps, never runs in steps, never stops dead.
        private double shown;
        private int shownAt;
        private double pace = 1.0;
        // A missile of an air strike: where it was on each tick it was told of (see Flown); null for anything else.
        @Nullable
        private Flown flown;
        @Nullable
        private ConstructPath path;
        // Where a steered one was drawn last, and the way it pointed: once it stops it falls apart right there.
        @Nullable
        private Vec3 lastCenter;
        @Nullable
        private Vec3 lastWay;
        // How long it stays without a word from the server: a round from a minigun is told about only once, as it is
        // fired, and flies on by itself.
        private final int keep;
        // True for what the server tells about only once (see sentOnce): its clock runs on by itself from there.
        private final boolean once;
        // The age of the first update that told of the variant it has now: a Light Bubble's pound is timed from there.
        private int variantSince;
        // A giant hand that slapped its palm down flat: what lay under it was squashed (see slapped).
        private boolean struck;

        Track(ConstructPayload first) {
            this.previous = first;
            this.current = first;
            this.latest = first;
            this.lastSeen = clientTicks;
            this.keep = first.shape() == ConstructPayload.BULLET ? PlanePainter.bulletTicks(first)
                    : first.shape() == ConstructPayload.BLAST ? PlanePainter.BLAST_TICKS + 8
                    : first.shape() == ConstructPayload.POUND ? BubblePainter.POUND_TICKS + 2
                    : first.shape() == ConstructPayload.PLANE ? PLANE_KEEP : TIMEOUT;
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
            double ahead = ownPath(this.latest.shape()) ? AHEAD_OWN : AHEAD;
            this.pace = Math.min(pace, Math.max(0.0, (this.told + ahead - this.shown) * AHEAD_PULL));
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

    /**
     * A construct someone is holding beside them: where it hangs, how far it has come in, which shape it is (the hand
     * that holds it depends on that: the ring hand attacks, the other one defends), and whether it is being smashed
     * down (a bubble: the ring hand swings down with it).
     */
    public record Held(Vec3 center, float strength, int shape, boolean smashing) {
        /** True while this is something the hand that defends holds up, not something the ring hand shapes. */
        public boolean defends() {
            return this.shape == ConstructPayload.SHIELD;
        }
    }

    /**
     * The construct this player holds out with one hand, or null when they hold none: a fist they charge or a bubble
     * the ring holds up (the ring hand), or else the shield in front of them (the other hand). (The dome, the ram cone
     * and the beam are posed with the flight, see FlightPose.)
     */
    @Nullable
    public static Held heldBy(int owner) {
        Held attacks = heldBy(owner, false);
        return attacks != null ? attacks : heldBy(owner, true);
    }

    /** The construct this player holds out with the hand that defends ({@code defends}), or with the ring hand. */
    @Nullable
    public static Held heldBy(int owner, boolean defends) {
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(owner);
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.owner() != owner || !now.held()) {
                continue;
            }
            if (now.shape() == ConstructPayload.FIST && !defends) {
                Vec3 center = entity == null ? now.center() : hung(entity, now.center(), partialTick);
                return new Held(center, now.solid(), now.shape(), false);
            }
            if (now.shape() == ConstructPayload.BUBBLE && !defends) {
                return new Held(track.previous.center().lerp(now.center(), partialTick),
                        Math.min(1.0F, now.solid() * 1.5F), now.shape(), now.variant() == LightBubble.SMASHING);
            }
            if (now.shape() == ConstructPayload.SHIELD && defends) {
                Vec3 center = entity == null ? now.center() : pane(entity, partialTick);
                return new Held(center, now.solid(), now.shape(), false);
            }
        }
        return null;
    }

    /**
     * What the server says about the sword and shield of one player (see {@link SwordArms}): the move they do, on which
     * tick of their clock it began, their clock (by the client's own clock), how many ticks ago they began to break up
     * (-1 while whole), and the way a charge runs.
     */
    public record Sword(int move, double moveStart, double clock, float broken, Vec3 way) {
    }

    /** The sword and shield this player holds, as the server tells, or null when he holds none. */
    @Nullable
    public static Sword sword(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload sword = track.latest;
            if (sword.shape() == ConstructPayload.SWORD && sword.owner() == owner) {
                double clock = track.clock(partialTick);
                float broken = sword.size() < 0.0F ? -1.0F
                        : (float) (sword.size() + Math.max(0.0, clock - sword.age()));
                return new Sword(sword.variant(), sword.charge(), clock, broken, sword.facing());
            }
        }
        return null;
    }

    /**
     * How many ticks ago this player caught a creature in the bubble he holds (by the client's own clock), or -1 when
     * he holds none.
     */
    public static float bubbleAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload bubble = track.latest;
            if (bubble.shape() == ConstructPayload.BUBBLE && bubble.owner() == owner
                    && bubble.variant() != LightBubble.BREAKING) {
                return (float) track.clock(partialTick);
            }
        }
        return -1.0F;
    }

    /**
     * How hard the constructs of this player make the ring work right now, for the glow of the ring and the uniform:
     * a fist that charges (more as it grows), a fist or a bolt on its way, a scan rolling out, the hands being called,
     * and an air strike most of all, from the call until its plane has crashed.
     */
    public static float working(int owner, float partialTick) {
        float most = 0.0F;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.owner() != owner || now.solid() <= 0.0F) {
                continue;
            }
            float here = switch (now.shape()) {
                case ConstructPayload.FIST -> now.held() ? 0.6F + 0.4F * now.charge() : 0.5F;
                case ConstructPayload.BOLT -> 0.7F;
                case ConstructPayload.SCAN -> {
                    // The ring's own scan, while its wave rolls out; the plane's scans glow with the plane.
                    double rolled = track.clock(partialTick) * RingScan.SPEED / Math.max(1.0, now.size());
                    yield now.variant() == ConstructPayload.SCAN_HOSTILE ? 0.0F
                            : (float) (0.95 * (1.0 - Ease.smooth((rolled - 0.6) / 0.6)));
                }
                // Every giant hand as the ring's light shoots off to it and it bursts out of the ground.
                case ConstructPayload.HAND -> (float) (0.9
                        * (1.0 - Ease.smooth((track.clock(partialTick) - 8.0) / 8.0)));
                // Holding a creature up in a bubble, and most of all hurling it down.
                case ConstructPayload.BUBBLE -> now.variant() == LightBubble.SMASHING ? 1.0F
                        : now.variant() == LightBubble.HOLDING ? 0.8F : 0.0F;
                // The sword and shield: shaping them, every swing, holding the shield up, and hardest in a flurry, a
                // charge (every ram most of all) or a slam.
                case ConstructPayload.SWORD -> {
                    SwordMove move = SwordMove.sent(now.variant());
                    double t = track.clock(partialTick) - now.charge();
                    if (now.size() >= 0.0F || move == null) {
                        yield 0.0F;
                    }
                    if ((now.variant() & SwordMove.CHARGING) != 0) {
                        yield move.kind() == SwordMove.Kind.BASH && t < move.ticks() ? 1.0F : 0.85F;
                    }
                    float held = (now.variant() & SwordMove.BLOCKING) != 0 ? 0.5F : 0.3F;
                    yield switch (move.kind()) {
                        case EQUIP -> t < 12.0 ? 0.9F : held;
                        case FLURRY, SLAM -> t < move.ticks() ? 0.85F : held;
                        default -> t < move.ticks() ? Math.max(0.6F, held) : held;
                    };
                }
                case ConstructPayload.PLANE -> {
                    // The ring pours everything it has into the call, holds the plane up hard the whole time it flies,
                    // and flares as it plunges.
                    double clock = track.clock(partialTick);
                    PlanePath path = PlanePainter.path(track.latest);
                    if (clock >= path.crashTick()) {
                        yield (float) Math.max(0.0, 1.0 - (clock - path.crashTick()) / 20.0);
                    }
                    yield clock < AirStrike.CALL_TICKS + 8.0 || clock >= path.diveTick() - 8.0 ? 1.0F : 0.85F;
                }
                default -> 0.0F;
            };
            most = Math.max(most, here * Math.min(1.0F, now.solid()));
        }
        return most;
    }

    /**
     * How many ticks ago this player's landing slam began, by the client's own clock, or -1 when there is none.
     * Counted at the pace the constructs were made for (see {@link SlamPainter#pace}), like the timeline of
     * {@link LandingSlam}.
     */
    static float slamAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.SLAM && track.latest.owner() == owner) {
                return (float) (track.clock(partialTick) / SlamPainter.pace(track.latest));
            }
        }
        return -1.0F;
    }

    /**
     * One scan rolling out: whose it is, where it set out from, how far it reaches and has got by now (by the client's
     * own clock), how long what it passes stays marked, in seconds, and whether it only marks what is out to hurt its
     * maker (the scans of the air strike's plane).
     */
    public record Scan(int id, int owner, Vec3 center, double radius, double reached, double seconds,
            boolean hostileOnly) {
    }

    /** Every scan rolling out right now. */
    public static List<Scan> scans(float partialTick) {
        List<Scan> scans = new ArrayList<>();
        for (Map.Entry<Integer, Track> entry : CONSTRUCTS.entrySet()) {
            ConstructPayload scan = entry.getValue().latest;
            if (scan.shape() == ConstructPayload.SCAN) {
                double reached = Math.min(scan.size(), RingScan.SPEED * entry.getValue().clock(partialTick));
                scans.add(new Scan(entry.getKey(), scan.owner(), scan.center(), scan.size(), reached, scan.charge(),
                        scan.variant() == ConstructPayload.SCAN_HOSTILE));
            }
        }
        return scans;
    }

    /**
     * How many ticks ago this player's own Ring Scan set out (by the client's own clock), or -1 when none is rolling:
     * for his arm, which holds the ring out while it scans.
     */
    public static float scanAge(int owner, float partialTick) {
        float youngest = -1.0F;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload scan = track.latest;
            if (scan.shape() == ConstructPayload.SCAN && scan.owner() == owner
                    && scan.variant() != ConstructPayload.SCAN_HOSTILE) {
                float clock = (float) track.clock(partialTick);
                youngest = youngest < 0.0F ? clock : Math.min(youngest, clock);
            }
        }
        return youngest;
    }

    /** Where a missile was on one tick it was told of: its middle, its nose and its up (carried along as it swings). */
    private record Spot(double age, Vec3 at, Vec3 nose, Vec3 up) {
    }

    /**
     * What this client keeps of one missile of an air strike: where it was on the ticks the server told of, so it is
     * drawn smoothly by its own clock between them (see missile); how it was let go; and, once it has struck, when by
     * that clock, so it is seen to get there before it breaks up.
     */
    private static final class Flown {
        // How many of the ticks it was told of are kept, and how soon a correction dies away (in ticks, most of it).
        private static final int KEPT = 8;
        private static final double SETTLES = 1.5;
        private final ArrayDeque<Spot> spots = new ArrayDeque<>();
        private final boolean small;
        private final int variant;
        private final int fired;
        private final int ignites;
        // The way the plane flies it left, when this client draws that plane (null: it does not): the plane then draws
        // it itself while it falls with its motor dead (see PlanePainter).
        @Nullable
        private final PlanePath path;
        // Ticks after it was let go (by its clock) that it struck, or forever while it flies on; and whether the blast
        // it struck with is timed from it yet.
        private double ends = Double.POSITIVE_INFINITY;
        private boolean blasted;
        // What is left of a correction: where it was drawn less where it is now worked out to be, as a tick told of
        // came in later than it was drawn (see settle), and from when (ticks after the first tick it was told of, by its
        // clock). It dies away instead of making the missile jump.
        private Vec3 offset = Vec3.ZERO;
        private double offsetFrom;

        Flown(ConstructPayload first, @Nullable PlanePath path) {
            this.small = first.variant() >= AirStrike.JET_MISSILE;
            this.variant = first.variant();
            this.fired = Math.round(first.size());
            this.ignites = Math.round(first.charge());
            this.path = path;
        }

        /** Until how many ticks after it was let go the plane draws it: while it still falls with its motor dead. */
        double leaves() {
            return this.path == null ? Double.NEGATIVE_INFINITY : Math.min(this.ignites, this.ends);
        }

        /** Where it is on a tick the server tells of, heard of as it was drawn {@code drawn} ticks after the first. */
        void add(ConstructPayload update, double drawn) {
            Spot last = this.spots.peekLast();
            if (this.ends < Double.POSITIVE_INFINITY || last != null && update.age() <= last.age()) {
                return;
            }
            Vec3 was = this.drawnAt(drawn);
            Vec3 nose = update.facing().lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : update.facing().normalize();
            Vec3 up = last != null ? PlanePath.carried(last.up(), nose) : this.firstUp(update.age(), nose);
            this.spots.add(new Spot(update.age(), update.center(), nose, up));
            while (this.spots.size() > KEPT) {
                this.spots.poll();
            }
            this.settle(was, drawn);
        }

        /**
         * Where it is drawn {@code age} ticks after the first tick it was told of, with what is left of a correction; null
         * while nothing is drawn of it yet, or the plane draws it (see leaves).
         */
        @Nullable
        Vec3 drawnAt(double age) {
            return this.spots.isEmpty() || age + 1.0 <= this.leaves() ? null : this.at(age).at().add(this.off(age));
        }

        /** What is left of a correction {@code age} ticks after the first tick it was told of. */
        Vec3 off(double age) {
            return this.offset.scale(Math.exp(-Math.max(0.0, age - this.offsetFrom) / SETTLES));
        }

        /**
         * News came in that moves where it is worked out to be right now, where it was drawn at {@code was}: it goes on
         * from there and eases over to where it now is, instead of jumping.
         */
        private void settle(@Nullable Vec3 was, double age) {
            if (was != null && age + 1.0 > this.leaves()) {
                this.offset = was.subtract(this.at(age).at());
                this.offsetFrom = age;
            }
        }

        /** Its up on the first tick heard of: carried along from how it left the plane, just as the plane draws it. */
        private Vec3 firstUp(int age, Vec3 nose) {
            if (this.path == null) {
                return PlanePath.carried(new Vec3(0.0, 1.0, 0.0), nose);
            }
            int k = (this.variant - AirStrike.JET_MISSILE) / 2;
            Vec3[] state = this.small ? this.path.firedOff(k, (this.variant - AirStrike.JET_MISSILE) % 2, this.fired)
                    : this.path.dropsOut(this.fired);
            for (int step = 0; step <= age && step < this.ignites; step++) {
                state = PlanePath.fall(state, this.small);
            }
            return PlanePath.carried(state[3], nose);
        }

        /**
         * It struck, or was let go of: the last tick told of is where it ends. Struck partway through that tick, it
         * gets there as much sooner as it went less far, so it never slows down on its last stretch. Heard of as it was
         * drawn {@code drawn} ticks after the first tick told of.
         */
        void strikes(double drawn) {
            if (this.ends < Double.POSITIVE_INFINITY || this.spots.isEmpty()) {
                return;
            }
            Vec3 was = this.drawnAt(drawn);
            Spot last = this.spots.pollLast();
            Spot before = this.spots.peekLast();
            double part = 1.0;
            if (before != null) {
                Spot earlier = null;
                for (Spot spot : this.spots) {
                    if (spot != before) {
                        earlier = spot;
                    }
                }
                double step = earlier == null ? 0.0 : before.at().distanceTo(earlier.at());
                if (step > 1.0E-6) {
                    part = Mth.clamp(last.at().distanceTo(before.at()) / step, 0.05, 1.0);
                }
            }
            double age = before == null ? last.age() : before.age() + part;
            this.spots.add(new Spot(age, last.at(), last.nose(), last.up()));
            this.ends = age + 1.0;
            this.settle(was, drawn);
        }

        /**
         * Where it is {@code age} ticks after the first tick it was told of by its own clock: between the ticks told
         * of, smooth; past the last one, on the way it went, a little way, until it hears more.
         */
        Spot at(double age) {
            Spot before = null;
            Spot after = null;
            Spot earlier = null;
            for (Spot spot : this.spots) {
                if (spot.age() <= age) {
                    earlier = before;
                    before = spot;
                } else {
                    after = spot;
                    break;
                }
            }
            if (before == null) {
                return this.spots.peekFirst();
            }
            if (after == null) {
                if (earlier == null || this.ends < Double.POSITIVE_INFINITY) {
                    return before;
                }
                double on = Math.min(2.0, age - before.age()) / Math.max(1.0E-6, before.age() - earlier.age());
                return new Spot(age, before.at().add(before.at().subtract(earlier.at()).scale(on)), before.nose(),
                        before.up());
            }
            double u = (age - before.age()) / Math.max(1.0E-6, after.age() - before.age());
            Vec3 nose = before.nose().lerp(after.nose(), u).normalize();
            return new Spot(age, before.at().lerp(after.at(), u), nose,
                    PlanePath.carried(before.up().lerp(after.up(), u), nose));
        }
    }

    /**
     * A missile of an air strike as this client knows it, for the plane to draw it leaving its hatch or a jet's wing:
     * how many ticks ago it was let go (by its own clock, which keeps the plane's time), and until how many ticks after
     * that the plane draws it (while it falls with its motor dead, until its motor fires or it strikes).
     */
    public record Launch(double since, double leaves) {
    }

    /**
     * The missile this player's air strike let go of on tick {@code fired} of its plane's clock from where
     * {@code variant} says (see {@link AirStrike#BIG_MISSILE}), or null when this client has not heard of it (yet).
     */
    @Nullable
    public static Launch launch(int owner, int variant, int fired, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            Flown flown = track.flown;
            if (flown != null && flown.fired == fired && flown.variant == variant && track.latest.owner() == owner) {
                return new Launch(track.clock(partialTick), Math.min(flown.ignites, flown.ends));
            }
        }
        return null;
    }

    /**
     * The tick of its plane's clock this player's air strike last let go of a missile from where {@code variant} says
     * (a pylon of one of its jets, see {@link AirStrike#JET_MISSILE}), or -1 when it has not yet: remembered however
     * soon that missile struck, so the next one grows on the pylon in its own time.
     */
    public static int launched(int owner, int variant) {
        return LAUNCHES.getOrDefault(((long) owner << 8) | variant, -1);
    }

    /**
     * The giant hands this player called lately, for his ring arm: where the newest came up and how long ago he called
     * it (by the client's own clock), and where the one before it came up (null when there is none).
     */
    public record Wave(Vec3 newest, double clock, @Nullable Vec3 before) {
    }

    /** The giant hands this player is calling, or null when he calls none. */
    @Nullable
    public static Wave wave(int owner, float partialTick) {
        ConstructPayload newest = null;
        ConstructPayload before = null;
        double newestClock = 0.0;
        double beforeClock = 0.0;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload hand = track.latest;
            if (hand.shape() != ConstructPayload.HAND || hand.owner() != owner) {
                continue;
            }
            double clock = track.clock(partialTick);
            if (newest == null || clock < newestClock) {
                before = newest;
                beforeClock = newestClock;
                newest = hand;
                newestClock = clock;
            } else if (before == null || clock < beforeClock) {
                before = hand;
                beforeClock = clock;
            }
        }
        return newest == null ? null : new Wave(newest.center(), newestClock, before == null ? null : before.center());
    }

    /**
     * How many ticks ago this player called his air strike (by the client's own clock), or -1 when he has none going.
     */
    public static float planeAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.PLANE && track.latest.owner() == owner) {
                return (float) track.clock(partialTick);
            }
        }
        return -1.0F;
    }

    /** How many ticks ago this player's beam broke loose (by the client's own clock), or -1 when it is not pouring. */
    public static float beamAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.BEAM && track.latest.owner() == owner
                    && track.latest.solid() >= 1.0F) {
                return (float) track.clock(partialTick);
            }
        }
        return -1.0F;
    }

    /**
     * How many ticks ago this player's newest bolt left the ring (by the client's own clock), or -1 when none did
     * lately: his ring arm points while he shoots.
     */
    public static float boltAge(int owner, float partialTick) {
        Double shot = BOLTS.get(owner);
        return shot == null ? -1.0F : (float) Math.max(0.0, clientTicks + partialTick - shot);
    }

    /** Which construct this player's landing slam throws up, or -1 when he has none (yet). */
    static int slamVariant(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.SLAM && track.latest.owner() == owner) {
                return track.latest.variant();
            }
        }
        return -1;
    }

    /** The way this player faced when his landing slam began, or null when he has none (yet). */
    @Nullable
    public static Vec3 slamFacing(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.SLAM && track.latest.owner() == owner) {
                return track.latest.facing();
            }
        }
        return null;
    }

    /**
     * How hard the shockwave of a slam nearby, or the crash of an air strike's plane, shakes a view from {@code from}:
     * 1 right next to it as it strikes, fading with distance and over the next few ticks, 0 when there is none.
     */
    static float shake(Vec3 from, float partialTick) {
        float most = 0.0F;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload slam = track.latest;
            if (slam.shape() == ConstructPayload.PLANE) {
                PlanePath path = PlanePainter.path(slam);
                double since = track.clock(partialTick) - path.crashTick();
                double near = 1.0 - from.distanceTo(path.crash()) / CRASH_SHAKE_RANGE;
                if (since >= 0.0 && since < CRASH_SHAKE_TICKS && near > 0.0) {
                    double fade = 1.0 - since / CRASH_SHAKE_TICKS;
                    most = Math.max(most, (float) (fade * fade * Math.min(1.0, near * 1.2)));
                }
                continue;
            }
            if (slam.shape() == ConstructPayload.BLAST) {
                double since = sinceSent(track, partialTick);
                double near = 1.0 - from.distanceTo(slam.center()) / BLAST_SHAKE_RANGE;
                if (since >= 0.0 && since < BLAST_SHAKE_TICKS && near > 0.0) {
                    double fade = 1.0 - since / BLAST_SHAKE_TICKS;
                    double hard = slam.variant() == AirStrike.SMALL_BLAST ? 0.3 : 0.65;
                    most = Math.max(most, (float) (hard * fade * fade * near));
                }
                continue;
            }
            if (slam.shape() == ConstructPayload.POUND) {
                double since = track.clock(partialTick);
                double near = 1.0 - from.distanceTo(slam.center()) / POUND_SHAKE_RANGE;
                if (since < POUND_SHAKE_TICKS && near > 0.0) {
                    double fade = 1.0 - since / POUND_SHAKE_TICKS;
                    double hard = BubblePainter.last(slam) ? 0.95 : 0.55;
                    most = Math.max(most, (float) (hard * fade * fade * Math.min(1.0, near * 1.3)));
                }
                continue;
            }
            if (slam.shape() == ConstructPayload.HAND) {
                most = Math.max(most, handShake(track, from, partialTick));
                continue;
            }
            if (slam.shape() != ConstructPayload.SLAM) {
                continue;
            }
            double since = track.clock(partialTick) / SlamPainter.pace(slam) - LandingSlam.IMPACT_TICK;
            double near = 1.0 - from.distanceTo(slam.center()) / (slam.size() * 3.0 + 4.0);
            if (since < 0.0 || since >= SHAKE_TICKS || near <= 0.0) {
                continue;
            }
            double fade = 1.0 - since / SHAKE_TICKS;
            most = Math.max(most, (float) (fade * fade * Math.min(1.0, near * 1.5)));
        }
        return most;
    }

    /**
     * How hard a giant hand shakes a view from {@code from} (see shake): the axe of a pair as it bites into the ground,
     * and a middle finger as it bursts out of it; 0 for the other moves.
     */
    private static float handShake(Track track, Vec3 from, float partialTick) {
        ConstructPayload hand = track.latest;
        int move = HandPose.move(hand.variant());
        double clock = track.clock(partialTick);
        double since;
        double ticks;
        double hard;
        double near;
        if (move == HandPose.AXE) {
            since = clock - HandDuo.IMPACT;
            if (since < 0.0 || since >= AXE_SHAKE_TICKS) {
                return 0.0F;
            }
            Vec3 strike = HandDuo.strike(hand.center(), hand.variant(), hand.center().add(hand.facing()),
                    Math.max(0.1, hand.size()));
            ticks = AXE_SHAKE_TICKS;
            hard = AXE_SHAKE;
            near = 1.0 - from.distanceTo(strike) / AXE_SHAKE_RANGE;
        } else if (move == HandPose.FINGER) {
            since = clock - HandPose.FINGER_BURSTS;
            ticks = FINGER_SHAKE_TICKS;
            hard = FINGER_SHAKE;
            near = 1.0 - from.distanceTo(hand.center()) / FINGER_SHAKE_RANGE;
        } else {
            return 0.0F;
        }
        if (since < 0.0 || since >= ticks || near <= 0.0) {
            return 0.0F;
        }
        double fade = 1.0 - since / ticks;
        return (float) (hard * fade * fade * Math.min(1.0, near * 1.2));
    }

    /**
     * Where a held fist hangs: the spot around its owner's eyes the server gave (x to his right, y up, z ahead),
     * turned with where he faces right now, so it keeps up with him however fast he turns or flies.
     */
    private static Vec3 hung(Entity owner, Vec3 spot, float partialTick) {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, owner.getViewYRot(partialTick));
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        return owner.getEyePosition(partialTick).add(right.scale(spot.x)).add(0.0, spot.y, 0.0)
                .add(ahead.scale(spot.z));
    }

    /** Where the shield stands: in front of its owner's eyes, in the way he looks right now. */
    private static Vec3 pane(Entity owner, float partialTick) {
        return owner.getEyePosition(partialTick).add(owner.getViewVector(partialTick).scale(LightShield.AHEAD));
    }

    public static void update(ConstructPayload payload) {
        if (payload.solid() < 0.0F) {
            Track going = CONSTRUCTS.get(payload.id());
            if (going != null && going.flown != null) {
                // A missile that struck (or was let go of) is seen to get there first, then breaks up (see missile).
                going.flown.strikes(going.clock(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false))
                        - 1.0);
                return;
            }
            letGo(CONSTRUCTS.remove(payload.id()));
            return;
        }
        Track track = CONSTRUCTS.get(payload.id());
        if (track == null) {
            track = new Track(payload);
            CONSTRUCTS.put(payload.id(), track);
            fromAirStrike(track, payload);
            if (payload.shape() == ConstructPayload.BOLT) {
                // Seen first on its way (it came into range late), it still left the ring as long ago as it has flown.
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                BOLTS.merge(payload.owner(), clientTicks + partialTick - (double) payload.age(), Math::max);
            }
        } else {
            track.add(payload);
        }
        if (track.flown != null) {
            track.flown.add(payload,
                    track.clock(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)) - 1.0);
        }
    }

    /**
     * Something new of an air strike: it keeps the time of its plane. A missile runs on the plane's clock (see
     * Track#follow) and a round flies out as its gun fired it by that clock, so they leave the plane just where it is
     * drawn; the gun swings to where the round says it is to point next; a missile's blast bursts as the missile gets
     * there on your screen; and a new plane forgets when the last one's jets fired.
     */
    private static void fromAirStrike(Track track, ConstructPayload payload) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        int owner = payload.owner();
        switch (payload.shape()) {
            case ConstructPayload.PLANE -> LAUNCHES.keySet().removeIf(key -> (int) (key >> 8) == owner);
            case ConstructPayload.MISSILE -> {
                Track plane = planeOf(owner);
                track.flown = new Flown(payload, plane == null ? null : PlanePainter.path(plane.latest));
                if (plane != null) {
                    track.follow(plane, track.flown.fired);
                }
                if (payload.variant() >= AirStrike.JET_MISSILE) {
                    LAUNCHES.put(((long) owner << 8) | payload.variant(), track.flown.fired);
                }
            }
            case ConstructPayload.BULLET -> {
                Track plane = planeOf(owner);
                if (plane != null) {
                    track.start = clientTicks + partialTick - (plane.clock(partialTick) - payload.charge());
                    PlanePainter.fired(plane.latest, payload);
                }
            }
            case ConstructPayload.BLAST -> {
                Track missile = null;
                double nearest = 64.0;
                for (Track other : CONSTRUCTS.values()) {
                    Flown flown = other.flown;
                    if (flown != null && !flown.blasted && flown.ends < Double.POSITIVE_INFINITY
                            && other.latest.owner() == owner) {
                        double far = flown.spots.getLast().at().distanceToSqr(payload.center());
                        if (far < nearest) {
                            nearest = far;
                            missile = other;
                        }
                    }
                }
                if (missile != null) {
                    missile.flown.blasted = true;
                    track.start = clientTicks + partialTick - (missile.clock(partialTick) - missile.flown.ends);
                }
            }
            default -> {
                // Nothing of an air strike.
            }
        }
    }

    /** The track of this player's air strike's plane, or null when this client has none. */
    @Nullable
    private static Track planeOf(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.PLANE && track.latest.owner() == owner) {
                return track;
            }
        }
        return null;
    }

    /**
     * Ticks since what the server tells about only once set off by the client's own clock, below 0 while it still waits
     * (a round fired a moment ahead of its plane's clock, a blast whose missile is still on its way).
     */
    private static double sinceSent(Track track, float partialTick) {
        return Double.isNaN(track.start) ? 0.0 : clientTicks + partialTick - track.start;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        Iterator<Track> tracks = CONSTRUCTS.values().iterator();
        while (tracks.hasNext()) {
            Track track = tracks.next();
            track.retime();
            if (track.flown != null && track.flown.ends < Double.POSITIVE_INFINITY) {
                // A missile that struck is gone once it has got there and broken up.
                if (track.clock(0.0F) > track.flown.ends + PlanePainter.MISSILE_BREAKS + 1.0
                        || clientTicks - track.lastSeen > PLANE_KEEP) {
                    tracks.remove();
                }
                continue;
            }
            if (track.timedOut()) {
                tracks.remove();
                // A plane not heard of a while is out of reach or the server hitches: it did not break up.
                if (track.latest.shape() != ConstructPayload.PLANE) {
                    letGo(track);
                }
            } else {
                track.advance();
                slapped(minecraft.level, track);
            }
        }
        BROKEN.values().removeIf(broken -> clientTicks - broken.since() > BROKEN_TICKS);
        BROKEN_HANDS.values().removeIf(broken -> clientTicks - broken.since() > HandPainter.breakTicks());
        BOLTS.values().removeIf(shot -> clientTicks - shot > BOLT_MEMORY);
    }

    /**
     * A giant hand slaps its palm down flat: every creature under it that the server presses against the ground (see
     * pressed) is drawn squashed flat a moment (see {@link Flattened}).
     */
    private static void slapped(ClientLevel level, Track track) {
        ConstructPayload hand = track.latest;
        if (hand.shape() != ConstructPayload.HAND || HandPose.move(hand.variant()) != HandPose.SLAM || track.struck
                || track.clock(0.0F) < HandPose.SLAM_HITS) {
            return;
        }
        track.struck = true;
        double scale = Math.max(0.1, hand.size());
        double reach = Math.sqrt(hand.facing().x * hand.facing().x + hand.facing().z * hand.facing().z) / scale;
        HandPose.Place place = HandPose.at(hand.variant(), HandPose.SLAM_HITS, reach).place(hand.center(),
                hand.facing(), scale);
        Vec3 along = new Vec3(place.up().x, 0.0, place.up().z);
        along = along.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : along.normalize();
        Vec3 across = along.cross(new Vec3(0.0, 1.0, 0.0));
        Entity owner = level.getEntity(hand.owner());
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(hand.center(), hand.center()).inflate(10.0 * scale),
                living -> pressed(owner, hand.owner(), living))) {
            Vec3 to = living.position().subtract(place.wrist());
            double wide = 2.0 * scale + living.getBbWidth() * 0.5;
            if (to.dot(along) >= -0.4 && to.dot(along) <= 6.6 * scale && Math.abs(to.dot(across)) <= wide
                    && to.y <= 1.6 * scale && to.y >= -2.5) {
                Flattened.flatten(living.getId());
            }
        }
    }

    /**
     * Whether the server presses this creature flat under a giant hand (see GiantHands), as far as a client can tell:
     * never the hand's maker, his pets, a spectator, an armor stand, a player in creative mode or one of his own team
     * that it spares; a monster or another player always, and any other creature only while it is out to attack (the
     * server strikes it only while it attacks the maker).
     */
    private static boolean pressed(@Nullable Entity owner, int ownerId, LivingEntity living) {
        if (living.getId() == ownerId || !living.isAlive() || living.isSpectator() || living instanceof ArmorStand) {
            return false;
        }
        if (living instanceof OwnableEntity pet && owner != null && owner.getUUID().equals(pet.getOwnerUUID())) {
            return false;
        }
        if (living instanceof Player player) {
            return !player.isCreative() && !(owner instanceof Player maker && !maker.canHarmPlayer(player));
        }
        return living instanceof Enemy || living instanceof Mob mob && mob.isAggressive();
    }

    /**
     * A creature caught in a Light Bubble stays right in its middle on your screen too, and one a giant hand holds right
     * in its fist. The game itself only tells where it is every few ticks and glides it there, well behind a bubble that
     * is smashed down or a hand that throws.
     */
    @SubscribeEvent
    public static void onClientTickDone(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.shape() == ConstructPayload.HAND) {
                held(minecraft, track);
                continue;
            }
            if (now.shape() != ConstructPayload.BUBBLE || now.variant() == LightBubble.BREAKING) {
                continue;
            }
            Entity caught = minecraft.level.getEntity(LightBubble.caughtId(now.charge()));
            if (caught == null || caught == minecraft.player) {
                continue;
            }
            caught.setPos(now.center().x, now.center().y - caught.getBbHeight() * 0.5, now.center().z);
            caught.setDeltaMovement(Vec3.ZERO);
        }
    }

    /**
     * What a giant hand holds sits in its fist, where the hand is by the client's own clock. A pair of hands with an
     * axe never holds anything.
     */
    private static void held(Minecraft minecraft, Track track) {
        ConstructPayload hand = track.latest;
        if (hand.charge() < 0.0F || minecraft.level == null || HandPose.move(hand.variant()) == HandPose.AXE) {
            return;
        }
        Entity caught = minecraft.level.getEntity(Math.round(hand.charge()));
        if (caught == null || caught == minecraft.player) {
            return;
        }
        double scale = Math.max(0.1, hand.size());
        double reach = Math.sqrt(hand.facing().x * hand.facing().x + hand.facing().z * hand.facing().z) / scale;
        Vec3 grip = HandPose.at(hand.variant(), track.clock(1.0F), reach).place(hand.center(), hand.facing(), scale)
                .at(HandPose.GRIP);
        caught.setPos(grip.x, grip.y - caught.getBbHeight() * 0.5, grip.z);
        caught.setDeltaMovement(Vec3.ZERO);
    }

    /**
     * A construct is gone. A plane still in the air when it goes was let go of by its maker, and so was a giant hand
     * that had not yet sunk back into the ground, or a pair of them whose axe had not yet broken up: they break into
     * solid pieces where they were instead of simply vanishing, as every construct does.
     */
    private static void letGo(@Nullable Track track) {
        if (track == null) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        double clock = track.clock(partialTick);
        if (track.latest.shape() == ConstructPayload.HAND) {
            if (clock < HandPose.sinks(track.latest.variant()) && clock > HandPose.ARRIVES) {
                BROKEN_HANDS.put(track.latest.id(), new Broken(track.latest, clock, clientTicks));
            }
            return;
        }
        if (track.latest.shape() == ConstructPayload.PLANE && clock < PlanePainter.path(track.latest).crashTick()) {
            BROKEN.put(track.latest.id(), new Broken(track.latest, clock, clientTicks));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CONSTRUCTS.clear();
        BROKEN.clear();
        BROKEN_HANDS.clear();
        BOLTS.clear();
        LAUNCHES.clear();
        RingSpot.clear();
        PlanePainter.clear();
        Flattened.clear();
    }

    // After water and glass: light never hides what is behind it, so it has to come after them.
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || CONSTRUCTS.isEmpty() && BROKEN.isEmpty() && BROKEN_HANDS.isEmpty()
                && !BeamCharge.any(level)) {
            return;
        }
        // The same blend between ticks that entities are drawn with.
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (float) (level.getGameTime() % 24000L) + partialTick;
        Camera camera = event.getCamera();
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(), time,
                event.getFrustum());
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload was = track.previous;
            ConstructPayload now = track.current;
            Entity owner = level.getEntity(now.owner());
            // A fist held beside its owner is placed round him: with him not here (too far to be seen) there is
            // nowhere to put it.
            if (owner == null && now.held() && now.shape() == ConstructPayload.FIST) {
                continue;
            }
            Vec3 facing = was.facing().lerp(now.facing(), partialTick);
            if (facing.lengthSqr() < 1.0E-6) {
                facing = now.facing();
            }
            Vec3 way = facing.normalize();
            double size = Mth.lerp(partialTick, was.size(), now.size());
            double solid = Mth.lerp(partialTick, was.solid(), now.solid());
            double charge = Mth.lerp(partialTick, was.charge(), now.charge());
            Vec3 center = where(was, now, owner, partialTick);
            // A fist or bolt on its way glides along its path by the client's own clock; a fist stays on its owner's
            // line of sight as he looks right now, so your own is always right under your crosshair. Once it stops
            // (it hit something, or falls apart at the end of its way) a bolt stays where the server says it stopped,
            // and a fist where it was drawn last.
            boolean onItsWay = track.path != null && !track.latest.held();
            if (onItsWay) {
                on(track, owner, partialTick);
                center = track.lastCenter;
                way = track.lastWay;
            }
            Vec3 ring = owner == null ? null : ringHand(minecraft, camera, owner, partialTick, event);
            // Seen from your own eyes: not from behind, and not while the camera looks out of something else.
            boolean own = owner == minecraft.player && camera.getEntity() == owner && !camera.isDetached();
            // What hangs on its owner is worked out here from how he stands right now, so it moves with him
            // without dragging a tick behind, however fast he turns or flies.
            if (owner != null) {
                switch (now.shape()) {
                    case ConstructPayload.SHIELD -> {
                        way = owner.getViewVector(partialTick);
                        center = pane(owner, partialTick);
                    }
                    case ConstructPayload.RAM -> {
                        way = ramWay(owner, partialTick);
                        // Seen from your own eyes it hangs in front of them, its open end just before the camera, so
                        // none of its sides ever sweeps through your view; seen from outside it is round his body.
                        center = own ? owner.getEyePosition(partialTick).add(way.scale(RAM_OWN_AHEAD))
                                : owner.getPosition(partialTick).add(0.0, owner.getBbHeight() * 0.5, 0.0);
                    }
                    case ConstructPayload.DOME -> center = owner.getPosition(partialTick)
                            .add(0.0, owner.getBbHeight() * 0.5, 0.0);
                    case ConstructPayload.BEAM -> way = owner.getViewVector(partialTick);
                    case ConstructPayload.FIST -> {
                        if (now.held() && !onItsWay) {
                            way = heldFacing(owner, partialTick);
                        }
                    }
                    default -> {
                        // A bolt flies on by itself.
                    }
                }
            }
            switch (now.shape()) {
                case ConstructPayload.BOLT -> painter.bolt(center, way, size, solid, ring);
                case ConstructPayload.SHIELD -> painter.shield(center, way, size, solid, charge, ring, own);
                case ConstructPayload.DOME -> painter.dome(center, size, solid, charge, own);
                case ConstructPayload.RAM -> painter.ram(center, way, solid, charge, own);
                case ConstructPayload.SLAM -> SlamPainter.draw(painter, track.latest, track.clock(partialTick), ring,
                        owner == null ? null : owner.getPosition(partialTick));
                case ConstructPayload.SCAN -> {
                    RingSight.wave(painter, now.center(), now.size(), track.clock(partialTick));
                    // The ring he holds out shines while it reads (the plane's scans shine out of its own sensor).
                    if (ring != null && now.variant() != ConstructPayload.SCAN_HOSTILE) {
                        RingSight.ringLight(painter, ring, track.clock(partialTick), own);
                    }
                }
                case ConstructPayload.HAND -> HandPainter.draw(painter, track.latest, was.facing().lerp(now.facing(),
                        partialTick), track.clock(partialTick), ring);
                case ConstructPayload.BEAM -> {
                    if (ring != null && owner != null) {
                        painter.beamOfLight(ring, beamEnd(level, owner, way, now, partialTick), solid,
                                track.clock(partialTick), 1.0);
                    }
                }
                case ConstructPayload.PLANE -> PlanePainter.draw(painter, now.id(), track.latest,
                        track.clock(partialTick), ring, partialTick);
                case ConstructPayload.MISSILE -> missile(painter, track, partialTick);
                case ConstructPayload.BULLET -> PlanePainter.bullet(painter, track.latest,
                        sinceSent(track, partialTick));
                case ConstructPayload.BLAST -> PlanePainter.missileBlast(painter, track.latest,
                        sinceSent(track, partialTick));
                // Until it breaks up its charge is the creature inside, not a time. A pound is timed by the updates as
                // they are drawn, so every slam squashes it the moment it is drawn on the ground.
                case ConstructPayload.BUBBLE -> BubblePainter.draw(painter, now, center, solid,
                        was.variant() == LightBubble.BREAKING ? charge : 0.0, now.held(), track.clock(partialTick), ring,
                        now.age() - 1.0 + partialTick - track.variantSince, now.center().subtract(was.center()));
                case ConstructPayload.POUND -> BubblePainter.pound(painter, track.latest, track.clock(partialTick));
                case ConstructPayload.SWORD -> {
                    // Your own in first person are drawn with your hands (see SwordArms).
                    if (owner != null && !own) {
                        SwordArms.draw(painter, owner, ring, partialTick);
                    }
                }
                default -> painter.fist(center, way, size, solid, charge, now.held() && !onItsWay, ring);
            }
        }
        for (Broken broken : BROKEN.values()) {
            double since = clientTicks - broken.since() + partialTick;
            PlanePainter.broken(painter, broken.construct(), broken.clock() + since, since);
        }
        for (Broken broken : BROKEN_HANDS.values()) {
            HandPainter.broken(painter, broken.construct(), broken.clock(),
                    clientTicks - broken.since() + partialTick);
        }
        // The light every ring gathers for the beam.
        for (AbstractClientPlayer player : level.players()) {
            if (BeamCharge.charge(player, partialTick) >= 0.0F) {
                BeamCharge.draw(painter, player, ringHand(minecraft, camera, player, partialTick, event), camera,
                        partialTick);
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    /**
     * A missile of an air strike on its way, drawn by its own clock (which keeps its plane's time) where it was on the
     * ticks told of, smooth between them (see Flown), and breaking into solid pieces once it has got where it struck.
     * While it still falls off the plane or a jet's wing with its motor dead, the plane draws it (see
     * {@link PlanePainter}), from the very spot it hung.
     */
    private static void missile(LanternPainter painter, Track track, float partialTick) {
        Flown flown = track.flown;
        if (flown == null || flown.spots.isEmpty()) {
            return;
        }
        double since = track.clock(partialTick);
        if (since <= flown.leaves() && planeOf(track.latest.owner()) != null) {
            return;
        }
        if (since >= flown.ends) {
            Spot last = flown.spots.getLast();
            PlanePainter.missile(painter, flown.small, last.at().add(flown.off(flown.ends - 1.0)), last.nose(),
                    last.up(), -1.0, since - flown.ends);
            return;
        }
        // Sent on the tick it was let go of, it had already moved one tick on: drawn a tick later, so it leaves the
        // plane from where it hung.
        Spot spot = flown.at(since - 1.0);
        PlanePainter.missile(painter, flown.small, spot.at().add(flown.off(since - 1.0)), spot.nose(), spot.up(),
                since - flown.ignites, -1.0);
    }

    /**
     * Moves a fist or bolt on its way along its path (see {@link ConstructPath}) and keeps where it is and the way it
     * points on its track. A steered fist whose owner is out of sight goes where the server says.
     */
    private static void on(Track track, @Nullable Entity owner, float partialTick) {
        ConstructPath path = track.path;
        ConstructPayload latest = track.latest;
        boolean moving = latest.path() != null;
        if (path == null) {
            return;
        }
        if (!path.steered()) {
            double travelled = path.travelled(moving ? track.clock(partialTick) : track.told);
            track.lastCenter = moving ? path.along(travelled, null) : latest.center();
            track.lastWay = path.way(travelled, null);
            return;
        }
        if (moving && owner != null) {
            ConstructPath.Sight sight = ConstructPath.Sight.of(owner.getEyePosition(partialTick),
                    owner.getViewYRot(partialTick), owner.getViewXRot(partialTick));
            double travelled = path.travelled(track.clock(partialTick));
            track.lastCenter = path.along(travelled, sight);
            track.lastWay = path.way(travelled, sight);
        } else if (track.lastCenter == null || moving) {
            track.lastCenter = track.previous.center().lerp(track.current.center(), partialTick);
            track.lastWay = latest.facing();
        }
    }

    /**
     * Where a construct is between two updates: a held fist hangs on its owner (see {@link #hung}), anything else
     * is blended from one update to the next. (A fist or bolt on its way follows its path instead.)
     */
    private static Vec3 where(ConstructPayload was, ConstructPayload now, @Nullable Entity owner, float partialTick) {
        if (now.held() && now.shape() == ConstructPayload.FIST) {
            Vec3 spot = was.held() ? was.center().lerp(now.center(), partialTick) : now.center();
            return owner == null ? now.center() : hung(owner, spot, partialTick);
        }
        return was.center().lerp(now.center(), partialTick);
    }

    /** The way a held fist points: where its owner looks, but tipped up or down no further than the server lets it. */
    private static Vec3 heldFacing(Entity owner, float partialTick) {
        return Vec3.directionFromRotation(Mth.clamp(owner.getViewXRot(partialTick), -25.0F, 25.0F),
                owner.getViewYRot(partialTick));
    }

    /**
     * The way the ram cone points: the way its owner looks while he hovers, the way he flies once he is going, and
     * in between it swings over smoothly. Both are taken as they are this very frame (the speed glides from one tick
     * to the next), so the cone never jumps from one way to the other or steps along with the ticks.
     */
    private static Vec3 ramWay(Entity owner, float partialTick) {
        Vec3 look = owner.getViewVector(partialTick);
        Vec3 moving = ClientFlight.velocity(owner, partialTick);
        double speed = moving.length();
        double along = Ease.smooth((speed - RAM_LOOK) / (RAM_ALONG - RAM_LOOK));
        if (along <= 0.0) {
            return look;
        }
        Vec3 way = look.scale(1.0 - along).add(moving.scale(along / speed));
        return way.lengthSqr() < 1.0E-6 ? look : way.normalize();
    }

    /**
     * Where the beam stops: at the first wall along the crosshair. For your own beam that is worked out here, so
     * it sits exactly on what you aim at; anyone else's beam is as long as the server says.
     */
    private static Vec3 beamEnd(ClientLevel level, Entity owner, Vec3 way, ConstructPayload now, float partialTick) {
        Vec3 eye = owner.getEyePosition(partialTick);
        if (owner == Minecraft.getInstance().player) {
            CharacterAbility bolt = GameCharacter.GREEN_LANTERN.byName("light_bolt");
            double range = bolt == null ? 40.0 : bolt.value("beamRangeBlocks");
            Vec3 far = eye.add(way.scale(range));
            BlockHitResult hit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, owner));
            return hit.getType() == HitResult.Type.MISS ? far : hit.getLocation();
        }
        return eye.add(way.scale(Math.max(0.5, now.size())));
    }

    /**
     * Where the ring is: measured where it was really drawn (see {@link RingSpot}), so the light leaves the stone
     * itself; worked out from the body when it was not drawn lately.
     */
    private static Vec3 ringHand(Minecraft minecraft, Camera camera, Entity owner, float partialTick,
            RenderLevelStageEvent event) {
        Vec3 seen = RingSpot.of(owner, camera, event.getProjectionMatrix(), event.getModelViewMatrix());
        if (seen != null) {
            return seen;
        }
        if (owner == minecraft.player && camera.getEntity() == owner && !camera.isDetached()) {
            // Your own ring in first person: straight at the ring on the hand the game draws low on the right
            // of your screen, but close to the camera, so the beam leaves the stone itself.
            Vector3f hand = LanternArms.handPoint(minecraft.player, partialTick);
            Vec3 forward = new Vec3(camera.getLookVector());
            Vec3 up = new Vec3(camera.getUpVector());
            Vec3 left = new Vec3(camera.getLeftVector());
            return camera.getPosition().add(forward.scale(-hand.z() * RING_NEAR))
                    .subtract(left.scale(hand.x() * RING_NEAR)).add(up.scale(hand.y() * RING_NEAR));
        }
        // Seen from outside: the end of the right arm, exactly where the body draws it.
        if (owner instanceof LivingEntity living) {
            return LanternArms.ringPoint(living, partialTick);
        }
        double yaw = Math.toRadians(owner.getViewYRot(partialTick));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        return owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.72, 0).add(forward.scale(0.3));
    }
}
