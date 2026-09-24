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
import net.minecraft.world.level.ClipContext;
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
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightFlare;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightShield;
import nl.tivek.multiversepowers.character.greenlantern.ability.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.body.LanternArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RingSpot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FlareLight;
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
    // How long a plane that was let go takes to break up and be gone, in ticks.
    private static final int BROKEN_TICKS = 42;
    // The client time each player's newest bolt left the ring, by the id of its owner (see boltAge), and how long that
    // is kept, in ticks.
    private static final Map<Integer, Double> BOLTS = new HashMap<>();
    private static final int BOLT_MEMORY = 40;
    private static int clientTicks;

    /** A plane let go of in the air, breaking up. */
    private record Broken(ConstructPayload plane, double clock, int since) {
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
        // The age of the first update that told of the variant it has now: a Light Bubble's pound is timed from there.
        private int variantSince;

        Track(ConstructPayload first) {
            this.previous = first;
            this.current = first;
            this.latest = first;
            this.lastSeen = clientTicks;
            this.keep = first.shape() == ConstructPayload.BULLET ? PlanePainter.bulletTicks(first)
                    : first.shape() == ConstructPayload.BLAST ? PlanePainter.BLAST_TICKS + 2
                    : first.shape() == ConstructPayload.POUND ? BubblePainter.POUND_TICKS + 2 : TIMEOUT;
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
            this.start = Double.isNaN(this.start) ? setOff : Math.min(this.start, setOff);
            this.told = Math.max(this.told, update.age());
        }

        /** Ticks since it set off by the client's own clock: smooth, and never far ahead of what the server told. */
        double clock(float partialTick) {
            if (Double.isNaN(this.start)) {
                return Math.max(0, this.told);
            }
            return Mth.clamp(clientTicks + partialTick - this.start, 0.0, this.told + 1.5);
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

    /** True for the shapes that play along a timeline of their own, from how long ago they set off. */
    private static boolean timed(int shape) {
        return switch (shape) {
            case ConstructPayload.SLAM, ConstructPayload.SCAN, ConstructPayload.FLARE, ConstructPayload.BEAM,
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
     * a fist that charges (more as it grows), a fist or a bolt on its way, a scan rolling out, a flare gathering its
     * light, and an air strike most of all, from the call until its plane has crashed.
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
                case ConstructPayload.FLARE -> track.clock(partialTick) < LightFlare.GATHER_TICKS + 4.0 ? 1.0F : 0.0F;
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

    /** One round a minigun fired: from its muzzle to where it strikes, and how long ago, by the client's own clock. */
    public record Shot(Vec3 muzzle, Vec3 to, double clock) {
    }

    /** The last two rounds one gun of this player's plane fired (0 the left one, 1 the right one), newest first. */
    public static List<Shot> shots(int owner, int gun, float partialTick) {
        Shot newest = null;
        Shot before = null;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload round = track.latest;
            if (round.shape() != ConstructPayload.BULLET || round.owner() != owner || round.variant() != gun) {
                continue;
            }
            Shot shot = new Shot(round.center().add(round.facing().scale(round.charge())), round.center(),
                    track.clock(partialTick));
            if (newest == null || shot.clock() < newest.clock()) {
                before = newest;
                newest = shot;
            } else if (before == null || shot.clock() < before.clock()) {
                before = shot;
            }
        }
        List<Shot> shots = new ArrayList<>(2);
        if (newest != null) {
            shots.add(newest);
            if (before != null) {
                shots.add(before);
            }
        }
        return shots;
    }

    /**
     * How many ticks ago one launcher of this player's plane fired its newest missile that is still on its way (0 the
     * left one, 1 the right one), or -1 when none is.
     */
    public static double missileSince(int owner, int side, float partialTick) {
        double youngest = -1.0;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload missile = track.latest;
            if (missile.shape() == ConstructPayload.MISSILE && missile.owner() == owner && missile.variant() == side) {
                double clock = track.clock(partialTick);
                youngest = youngest < 0.0 ? clock : Math.min(youngest, clock);
            }
        }
        return youngest;
    }

    /**
     * How long ago this player's flare began to gather its light, by the client's own clock, and where it is; null when
     * he has none going.
     */
    @Nullable
    public static FlareLight.Going flare(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload flare = track.latest;
            if (flare.shape() == ConstructPayload.FLARE && flare.owner() == owner) {
                return new FlareLight.Going(owner, track.clock(partialTick), flare.center(), flare.size());
            }
        }
        return null;
    }

    /** Every flare going right now, whoever's it is. */
    public static List<FlareLight.Going> flares(float partialTick) {
        List<FlareLight.Going> flares = new ArrayList<>();
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload flare = track.latest;
            if (flare.shape() == ConstructPayload.FLARE) {
                flares.add(new FlareLight.Going(flare.owner(), track.clock(partialTick), flare.center(), flare.size()));
            }
        }
        return flares;
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
                double since = track.clock(partialTick);
                double near = 1.0 - from.distanceTo(slam.center()) / BLAST_SHAKE_RANGE;
                if (since < BLAST_SHAKE_TICKS && near > 0.0) {
                    double fade = 1.0 - since / BLAST_SHAKE_TICKS;
                    most = Math.max(most, (float) (0.55 * fade * fade * near));
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
            letGo(CONSTRUCTS.remove(payload.id()));
            return;
        }
        Track track = CONSTRUCTS.get(payload.id());
        if (track == null) {
            CONSTRUCTS.put(payload.id(), new Track(payload));
            if (payload.shape() == ConstructPayload.BOLT) {
                // Seen first on its way (it came into range late), it still left the ring as long ago as it has flown.
                float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
                BOLTS.merge(payload.owner(), clientTicks + partialTick - (double) payload.age(), Math::max);
            }
        } else {
            track.add(payload);
        }
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
            if (track.timedOut()) {
                tracks.remove();
                letGo(track);
            } else {
                track.advance();
            }
        }
        BROKEN.values().removeIf(broken -> clientTicks - broken.since() > BROKEN_TICKS);
        BOLTS.values().removeIf(shot -> clientTicks - shot > BOLT_MEMORY);
    }

    /**
     * A creature caught in a Light Bubble stays right in its middle on your screen too. The game itself only tells where
     * it is every few ticks and glides it there, well behind a bubble that is smashed down.
     */
    @SubscribeEvent
    public static void onClientTickDone(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
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
     * A construct is gone. A plane still in the air when it goes was let go of by its maker: it breaks into solid pieces
     * where it was instead of simply vanishing, as every construct does.
     */
    private static void letGo(@Nullable Track track) {
        if (track == null || track.latest.shape() != ConstructPayload.PLANE) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        double clock = track.clock(partialTick);
        if (clock < PlanePainter.path(track.latest).crashTick()) {
            BROKEN.put(track.latest.id(), new Broken(track.latest, clock, clientTicks));
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        CONSTRUCTS.clear();
        BROKEN.clear();
        BOLTS.clear();
        RingSpot.clear();
        PlanePainter.clear();
    }

    // After water and glass: light never hides what is behind it, so it has to come after them.
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || CONSTRUCTS.isEmpty() && BROKEN.isEmpty() && !BeamCharge.any(level)) {
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
                case ConstructPayload.FLARE -> FlareLight.draw(painter, owner == null ? now.center()
                        : own ? FlareLight.ownRing(camera) : ring != null ? ring : FlareLight.ring(owner, partialTick),
                        owner == null ? way : owner.getViewVector(partialTick), track.clock(partialTick), own);
                case ConstructPayload.BEAM -> {
                    if (ring != null && owner != null) {
                        painter.beamOfLight(ring, beamEnd(level, owner, way, now, partialTick), solid,
                                track.clock(partialTick), 1.0);
                    }
                }
                case ConstructPayload.PLANE -> PlanePainter.draw(painter, now.id(), track.latest,
                        track.clock(partialTick), ring, partialTick);
                case ConstructPayload.MISSILE -> PlanePainter.missile(painter, center, way, track.clock(partialTick));
                case ConstructPayload.BULLET -> PlanePainter.bullet(painter, track.latest, track.clock(partialTick));
                case ConstructPayload.BLAST -> PlanePainter.missileBlast(painter, track.latest,
                        track.clock(partialTick));
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
            PlanePainter.broken(painter, broken.plane(), broken.clock() + since, since);
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
