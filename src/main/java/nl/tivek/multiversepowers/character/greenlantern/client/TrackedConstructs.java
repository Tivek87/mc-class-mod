package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.flame.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.ring.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.ability.slam.LandingSlam;
import nl.tivek.multiversepowers.character.greenlantern.ability.sword.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.whip.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.flight.Flown;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BubblePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.express.ExpressPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.plane.PlanePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.revolver.RevolverPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.slam.SlamPainter;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.duo.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.express.ExpressScript;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.character.greenlantern.plane.PlanePath;
import nl.tivek.multiversepowers.engine.math.Ease;
import static nl.tivek.multiversepowers.character.greenlantern.client.ConstructPlaces.pane;

abstract class TrackedConstructs {
    private static final double SHAKE_TICKS = 8.0;
    private static final double CRASH_SHAKE_TICKS = 30.0;
    private static final double CRASH_SHAKE_RANGE = 140.0 * AirStrike.CRASH_SIZE;
    private static final double BLAST_SHAKE_TICKS = 10.0;
    private static final double BLAST_SHAKE_RANGE = 24.0;
    private static final double POUND_SHAKE_TICKS = 9.0;
    private static final double POUND_SHAKE_RANGE = 26.0;
    private static final double AXE_SHAKE_TICKS = 10.0;
    private static final double AXE_SHAKE_RANGE = 24.0;
    private static final double AXE_SHAKE = 0.8;
    private static final double FINGER_SHAKE_TICKS = 8.0;
    private static final double FINGER_SHAKE_RANGE = 18.0;
    private static final double FINGER_SHAKE = 0.5;

    static final Map<Integer, Track> CONSTRUCTS = new HashMap<>();
    static final Map<Integer, Double> BOLTS = new HashMap<>();
    static final int BOLT_MEMORY = 40;
    static final Map<Long, Integer> LAUNCHES = new HashMap<>();
    static int clientTicks;

    TrackedConstructs() {
    }

    public record Held(Vec3 center, float strength, int shape, boolean smashing) {
        public boolean defends() {
            return this.shape == ConstructPayload.SHIELD;
        }
    }

    @Nullable
    public static Held heldBy(int owner) {
        Held attacks = heldBy(owner, false);
        return attacks != null ? attacks : heldBy(owner, true);
    }

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

    public record Sword(int move, double moveStart, double clock, float broken, Vec3 way) {
    }

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

    public record Flame(int id, int move, double moveStart, double clock, float broken) {
    }

    // Taken out again while the last one still breaks up: the whole one counts.
    @Nullable
    public static Flame flame(int owner, float partialTick) {
        Track found = null;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload flame = track.latest;
            if (flame.shape() == ConstructPayload.FLAME && flame.owner() == owner
                    && (found == null || flame.size() < 0.0F)) {
                found = track;
            }
        }
        if (found == null) {
            return null;
        }
        ConstructPayload flame = found.latest;
        double clock = found.clock(partialTick);
        float broken = flame.size() < 0.0F ? -1.0F : (float) (flame.size() + Math.max(0.0, clock - flame.age()));
        return new Flame(flame.id(), flame.variant(), flame.charge(), clock, broken);
    }

    public record Whip(int id, int move, double moveStart, double clock, float broken) {
    }

    // Taken out again while the last one still breaks up: the whole one counts.
    @Nullable
    public static Whip whip(int owner, float partialTick) {
        Track found = null;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload whip = track.latest;
            if (whip.shape() == ConstructPayload.WHIP && whip.owner() == owner
                    && (found == null || whip.size() < 0.0F)) {
                found = track;
            }
        }
        if (found == null) {
            return null;
        }
        ConstructPayload whip = found.latest;
        double clock = found.clock(partialTick);
        float broken = whip.size() < 0.0F ? -1.0F : (float) (whip.size() + Math.max(0.0, clock - whip.age()));
        return new Whip(whip.id(), whip.variant(), whip.charge(), clock, broken);
    }

    // The creature a lasso holds, or -1.
    public static int snared(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload snare = track.latest;
            if (snare.shape() == ConstructPayload.WHIP_SNARE && snare.owner() == owner) {
                return LightBubble.caughtId(snare.charge());
            }
        }
        return -1;
    }

    public record Wall(Vec3 center, Vec3 normal, double width) {
    }

    @Nullable
    public static Wall wall(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload wall = track.latest;
            if (wall.shape() == ConstructPayload.FLAME_WALL && wall.owner() == owner && wall.solid() >= 1.0F) {
                return new Wall(wall.center(), wall.facing(), wall.size());
            }
        }
        return null;
    }

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

    public static float working(int owner, float partialTick) {
        float most = 0.0F;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.owner() != owner || now.solid() <= 0.0F) {
                continue;
            }
            float here = switch (now.shape()) {
                case ConstructPayload.EXPRESS_PORTAL -> now.age() < ExpressScript.OPEN_TICKS + 6 ? 0.9F : 0.0F;
                case ConstructPayload.MECH -> MechScript.working(track.clock(partialTick));
                case ConstructPayload.BOLT -> 0.7F;
                case ConstructPayload.SCAN -> {
                    double rolled = track.clock(partialTick) * RingScan.SPEED / Math.max(1.0, now.size());
                    yield now.variant() == ConstructPayload.SCAN_HOSTILE ? 0.0F
                            : (float) (0.95 * (1.0 - Ease.smooth((rolled - 0.6) / 0.6)));
                }
                case ConstructPayload.HAND, ConstructPayload.REVOLVER -> (float) (0.9
                        * (1.0 - Ease.smooth((track.clock(partialTick) - 8.0) / 8.0)));
                case ConstructPayload.BUBBLE -> now.variant() == LightBubble.SMASHING ? 1.0F
                        : now.variant() == LightBubble.HOLDING ? 0.8F : 0.0F;
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
                case ConstructPayload.FLAME -> {
                    FlameMove move = FlameMove.sent(now.variant());
                    double t = track.clock(partialTick) - now.charge();
                    if (now.size() >= 0.0F || move == null) {
                        yield 0.0F;
                    }
                    if ((now.variant() & (FlameMove.FIRING | FlameMove.SWIRLING)) != 0) {
                        yield 0.95F;
                    }
                    yield move == FlameMove.EQUIP ? (t < FlameMove.LEFT_GRAB ? 0.9F : 0.4F)
                            : t < move.ticks() ? 0.7F : 0.35F;
                }
                case ConstructPayload.WHIP -> {
                    WhipMove move = WhipMove.sent(now.variant());
                    double t = track.clock(partialTick) - now.charge();
                    if (now.size() >= 0.0F || move == null) {
                        yield 0.0F;
                    }
                    if ((now.variant() & (WhipMove.WHIRLING | WhipMove.SPINNING)) != 0) {
                        yield 0.9F;
                    }
                    yield switch (move.kind()) {
                        case EQUIP -> t < WhipMove.POURED ? 0.9F : t < WhipMove.THROW + 8 ? 0.6F : 0.3F;
                        case LASSO -> t < WhipMove.LASSO_LAND + 4 ? 0.85F : 0.3F;
                        default -> t < move.ticks() ? 0.65F : 0.3F;
                    };
                }
                case ConstructPayload.PLANE -> {
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

    public static float slamAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.SLAM && track.latest.owner() == owner) {
                return (float) (track.clock(partialTick) / SlamPainter.pace(track.latest));
            }
        }
        return -1.0F;
    }

    public record Scan(int id, int owner, Vec3 center, double radius, double reached, double seconds,
            boolean hostileOnly) {
    }

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

    public record Launch(double since, double leaves) {
    }

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

    public static int launched(int owner, int variant) {
        return LAUNCHES.getOrDefault(((long) owner << 8) | variant, -1);
    }

    public record Wave(Vec3 newest, double clock, @Nullable Vec3 before, double beforeClock) {
    }

    @Nullable
    public static Wave wave(int owner, float partialTick) {
        ConstructPayload newest = null;
        ConstructPayload before = null;
        double newestClock = 0.0;
        double beforeClock = 0.0;
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload hand = track.latest;
            if (hand.shape() != ConstructPayload.HAND && hand.shape() != ConstructPayload.REVOLVER
                    || hand.owner() != owner) {
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
        return newest == null ? null
                : new Wave(newest.center(), newestClock, before == null ? null : before.center(), beforeClock);
    }

    public static float planeAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.PLANE && track.latest.owner() == owner) {
                return (float) track.clock(partialTick);
            }
        }
        return -1.0F;
    }

    public static float beamAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.BEAM && track.latest.owner() == owner
                    && track.latest.solid() >= 1.0F) {
                return (float) track.clock(partialTick);
            }
        }
        return -1.0F;
    }

    public static int beamStage(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.BEAM && track.latest.owner() == owner) {
                return Math.max(0, track.latest.variant());
            }
        }
        return 0;
    }

    // The beam's stage clock as the server sent it (LightBeam.clockSent), or NaN without a beam.
    public static float beamClock(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.BEAM && track.latest.owner() == owner
                    && track.latest.solid() >= 1.0F) {
                return track.latest.charge();
            }
        }
        return Float.NaN;
    }

    // Ticks since the beam reached the stage it is at, or -1 without a beam.
    public static float beamStageAge(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.BEAM && track.latest.owner() == owner
                    && track.latest.solid() >= 1.0F) {
                return (float) Math.max(0.0, track.clock(partialTick) - track.variantSince);
            }
        }
        return -1.0F;
    }

    public static float boltAge(int owner, float partialTick) {
        Double shot = BOLTS.get(owner);
        return shot == null ? -1.0F : (float) Math.max(0.0, clientTicks + partialTick - shot);
    }

    public static int slamVariant(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.SLAM && track.latest.owner() == owner) {
                return track.latest.variant();
            }
        }
        return -1;
    }

    @Nullable
    public static Vec3 slamFacing(int owner) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.latest.shape() == ConstructPayload.SLAM && track.latest.owner() == owner) {
                return track.latest.facing();
            }
        }
        return null;
    }

    public static float shake(Vec3 from, float partialTick) {
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
            if (slam.shape() == ConstructPayload.REVOLVER) {
                most = Math.max(most, RevolverPainter.shake(slam, track.clock(partialTick), from));
                continue;
            }
            if (slam.shape() == ConstructPayload.EXPRESS) {
                most = Math.max(most, ExpressPainter.shake(slam, track.clock(partialTick), from));
                continue;
            }
            if (slam.shape() == ConstructPayload.MECH) {
                most = Math.max(most, MechPainter.shake(slam, track.clock(partialTick), from));
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
        } else if (move == HandPose.FINGER || move == HandPose.SNAP || move == HandPose.HAMMER
                || move == HandPose.RINGHOLD || move == HandPose.CLAP || move == HandPose.TEAR) {
            since = clock - switch (move) {
                case HandPose.FINGER -> HandPose.FINGER_BURSTS;
                case HandPose.SNAP -> HandPose.SNAP_HITS;
                case HandPose.RINGHOLD -> HandGroup.RING_BLASTS;
                case HandPose.CLAP -> HandGroup.CLAP_HITS;
                case HandPose.TEAR -> HandGroup.TEARS;
                default -> HandPose.HAMMER_HITS;
            };
            ticks = FINGER_SHAKE_TICKS;
            hard = FINGER_SHAKE * (move == HandPose.SNAP || move == HandPose.CLAP ? 0.6 : 1.0);
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

    // A mech its owner stands in: where it stands, how far it has come, and how far it has broken up (below 0: not).
    // A creature between clapping palms: the level line the palms close along, and how far into the clap it is.
    public record Clap(Vec3 across, double t) {
    }

    @Nullable
    public static Clap clapped(int entity, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload hand = track.latest;
            if (hand.shape() == ConstructPayload.HAND && HandPose.move(hand.variant()) == HandPose.CLAP && hand.held()
                    && LightBubble.caughtId(hand.charge()) == entity) {
                Vec3 ahead = new Vec3(hand.facing().x, 0.0, hand.facing().z);
                ahead = ahead.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : ahead.normalize();
                return new Clap(new Vec3(-ahead.z, 0.0, ahead.x), track.clock(partialTick));
            }
        }
        return null;
    }

    public record Piloted(int id, MechScript.Stage stage, double t, double broke) {
        public Vec3 feet() {
            return this.stage.point(this.broke >= 0.0 ? MechScript.lowered(this.broke)
                    : MechScript.pilot(this.stage, this.t));
        }
    }

    @Nullable
    public static Piloted piloted(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.shape() != ConstructPayload.MECH || now.owner() != owner) {
                continue;
            }
            boolean breaking = MechScript.breaking(now.variant());
            double broke = breaking ? Mth.lerp(partialTick, Math.max(0.0F, track.previous.charge()), now.charge())
                    : -1.0;
            return new Piloted(now.id(), MechScript.Stage.of(now), breaking ? now.age() : track.clock(partialTick),
                    broke);
        }
        return null;
    }

    // The creature a building mech holds under its blows, and how far that build has come.
    @Nullable
    public static Piloted mechVictim(int entity, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            ConstructPayload now = track.current;
            if (now.shape() == ConstructPayload.MECH && !MechScript.breaking(now.variant())
                    && MechScript.target(now.variant()) == entity) {
                return new Piloted(now.id(), MechScript.Stage.of(now), track.clock(partialTick), -1.0);
            }
        }
        return null;
    }

    @Nullable
    static ExpressPainter.Gate expressGate(int owner, float partialTick) {
        for (Track track : CONSTRUCTS.values()) {
            if (track.current.shape() == ConstructPayload.EXPRESS_PORTAL && track.current.owner() == owner) {
                return ExpressPainter.gateOf(track.previous, track.current, partialTick);
            }
        }
        return null;
    }

    static double sinceSent(Track track, float partialTick) {
        return Double.isNaN(track.start) ? 0.0 : clientTicks + partialTick - track.start;
    }
}
