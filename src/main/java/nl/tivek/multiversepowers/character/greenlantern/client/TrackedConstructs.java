package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.hung;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.pane;

/**
 * The constructs this client keeps (see {@link ClientConstructs}), and what the rest of the client asks about them:
 * what a player holds out, how long ago his slam, scan, beam, bolt or air strike began, which missile his air strike
 * let go of, and how hard a slam, a crash or a blast nearby shakes your view. {@link ClientConstructs} builds on this,
 * so everything here is asked of it.
 */
abstract class TrackedConstructs {
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

    static final Map<Integer, Track> CONSTRUCTS = new HashMap<>();
    // The client time each player's newest bolt left the ring, by the id of its owner (see boltAge), and how long that
    // is kept, in ticks.
    static final Map<Integer, Double> BOLTS = new HashMap<>();
    static final int BOLT_MEMORY = 40;
    // The tick of its plane's clock each pylon of each player's jets last fired a missile on (see launched), by the id
    // of the player and the pylon.
    static final Map<Long, Integer> LAUNCHES = new HashMap<>();
    static int clientTicks;

    TrackedConstructs() {
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
                        case EQUIP -> t < SwordMove.TOSS ? 0.9F : held;
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
     * The giant hand this player called last, for his ring arm: where it came up and how long ago he called it (by the
     * client's own clock).
     */
    public record Wave(Vec3 newest, double clock) {
    }

    /** The giant hands this player is calling, or null when he calls none. */
    @Nullable
    public static Wave wave(int owner, float partialTick) {
        ConstructPayload newest = null;
        double newestClock = 0.0;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload hand = track.latest;
            if (hand.shape() != ConstructPayload.HAND || hand.owner() != owner) {
                continue;
            }
            double clock = track.clock(partialTick);
            if (newest == null || clock < newestClock) {
                newest = hand;
                newestClock = clock;
            }
        }
        return newest == null ? null : new Wave(newest.center(), newestClock);
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
     * Ticks since what the server tells about only once set off by the client's own clock, below 0 while it still waits
     * (a round fired a moment ahead of its plane's clock, a blast whose missile is still on its way).
     */
    static double sinceSent(Track track, float partialTick) {
        return Double.isNaN(track.start) ? 0.0 : clientTicks + partialTick - track.start;
    }
}
