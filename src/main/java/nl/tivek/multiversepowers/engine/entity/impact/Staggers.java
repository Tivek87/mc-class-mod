package nl.tivek.multiversepowers.engine.entity.impact;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;

// How a creature that lives takes a blow (Impacts), by how hard it was pushed and hurt for its weight: a light one
// makes it flinch; a harder one staggers it, so it stops acting a moment, is carried back by the blow and steps to keep
// its feet; one to the legs sweeps them and it falls on its face; one harder still knocks it off its feet. Staggered
// over an edge, it loses its footing and falls. Every player near is told how it took the blow, and its body moves so.
// Staggered once, it braces itself a moment: blows then only make it flinch unless they would knock it down.
public final class Staggers {
    // A grown person's size (width squared times height): a heavier creature takes a blow less hard.
    private static final double PERSON = 0.7;
    // The game's own push for a blow (blocks a tick), and how much the push and the share of health taken count.
    private static final double KNOCKBACK = 0.4;
    private static final double SHOVE = 0.35;
    private static final double SHARE = 2.0;
    // How hard a blow must be to stagger, to sweep the legs (struck below LEGS of its height) and to knock it down.
    private static final double STAGGER_FROM = 0.6;
    private static final double TRIP_FROM = 0.9;
    private static final double FALL_FROM = 2.0;
    static final double LEGS = 0.33;
    // Ticks a flinch is shown, the shortest and longest stagger, and how long it braces itself after one.
    private static final int FLINCH_TICKS = 8;
    private static final int SHORTEST = 14;
    private static final int LONGEST = 30;
    private static final int BRACE = 20;
    // Swept off its legs it falls forward (blocks a tick), towards whoever struck it; knocked down it is shoved on the
    // way the blow went; staggered, the steps carry it back this much further than the blow alone.
    private static final double TRIP_PUSH = 0.32;
    private static final double TRIP_LIFT = 0.08;
    private static final double FALL_PUSH = 0.25;
    private static final double DRIFT = 0.08;
    // Falling this fast (blocks a tick) while staggering, it has gone over an edge.
    private static final double OVER_EDGE = -0.25;

    private static final Map<Mob, Stagger> STAGGERED = new IdentityHashMap<>();

    private static final class Stagger {
        final boolean noAi;
        int ticks;
        int age;

        Stagger(boolean noAi, int ticks) {
            this.noAi = noAi;
            this.ticks = ticks;
        }
    }

    private Staggers() {
    }

    static void struck(ServerLevel level, Mob mob, Impacts.Impact impact) {
        if (!PowerRules.staggers() || !Knockdowns.falls(mob) || mob.isPassenger() || mob.isVehicle()) {
            return;
        }
        if (HeldMobs.isHeld(mob) || Knockdowns.isDown(mob)) {
            // Limp: its body takes the blow where it struck.
            tell(mob, impact, 0.0, ImpactPayload.Reaction.LIMP, 0);
            return;
        }
        // A power throws it on top of the blow: it flies as the game would have thrown it, hop and all, Knockdowns
        // throws it as ever and its body takes the blow where it struck.
        Vec3 game = mob.getDeltaMovement().add(Impacts.lost(mob));
        if (Knockdowns.thrownBy(mob, game)) {
            mob.setDeltaMovement(game);
            mob.hurtMarked = true;
            tell(mob, impact, 0.0, ImpactPayload.Reaction.LIMP, 0);
            return;
        }
        AABB box = mob.getBoundingBox();
        double height = (impact.at().y - box.minY) / Math.max(0.1, box.getYsize());
        Vec3 push = mob.getDeltaMovement();
        double heavy = Math.sqrt(Math.max(0.3, Knockdowns.weight(mob) / PERSON));
        double shove = Math.max(push.horizontalDistance(), Impacts.shoved(mob));
        double strength = (SHOVE * shove / KNOCKBACK + SHARE * impact.share()) / heavy;
        Stagger had = STAGGERED.get(mob);
        boolean braced = had != null && had.age >= had.ticks;
        boolean falls = Knockdowns.mayFly(mob) && !mob.isInWater();
        ImpactPayload.Reaction reaction;
        if (falls && strength >= FALL_FROM) {
            reaction = ImpactPayload.Reaction.FALL;
        } else if (falls && !braced && height < LEGS && strength >= TRIP_FROM) {
            reaction = ImpactPayload.Reaction.TRIP;
        } else if (!braced && strength >= STAGGER_FROM && mob.onGround()) {
            reaction = ImpactPayload.Reaction.STAGGER;
        } else {
            reaction = ImpactPayload.Reaction.FLINCH;
        }
        Vec3 flat = new Vec3(impact.way().x, 0.0, impact.way().z);
        flat = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
        int ticks = FLINCH_TICKS;
        switch (reaction) {
            case FALL -> Knockdowns.trip(mob, push.add(flat.scale(FALL_PUSH / heavy)));
            case TRIP -> Knockdowns.trip(mob, new Vec3(-flat.x * TRIP_PUSH, TRIP_LIFT, -flat.z * TRIP_PUSH));
            case STAGGER -> {
                double share = Mth.clamp((strength - STAGGER_FROM) / (FALL_FROM - STAGGER_FROM), 0.0, 1.0);
                ticks = Mth.floor(Mth.lerp(share, SHORTEST, LONGEST));
                mob.setDeltaMovement(push.add(flat.scale(DRIFT * (0.5 + share) / heavy)));
                mob.hurtMarked = true;
                stagger(level, mob, ticks);
            }
            default -> {
            }
        }
        tell(mob, impact, strength, reaction, reaction == ImpactPayload.Reaction.STAGGER
                || reaction == ImpactPayload.Reaction.FLINCH ? ticks : 0);
    }

    private static void stagger(ServerLevel level, Mob mob, int ticks) {
        Stagger had = STAGGERED.get(mob);
        if (had != null) {
            had.ticks = Math.max(had.ticks, had.age + ticks);
            return;
        }
        Stagger mine = new Stagger(Knockdowns.ownNoAi(mob), ticks);
        STAGGERED.put(mob, mine);
        Knockdowns.pause(mob, mine.noAi);
        Effects.start(level, (lvl, age) -> {
            if (STAGGERED.get(mob) != mine) {
                return false;
            }
            if (!mob.isAlive() || mob.isRemoved()) {
                STAGGERED.remove(mob);
                Knockdowns.resume(mob, mine.noAi);
                return false;
            }
            mine.age = age;
            if (age < mine.ticks) {
                Knockdowns.fly(lvl, mob);
                if (!mob.onGround() && mob.getDeltaMovement().y < OVER_EDGE) {
                    // Over an edge: it loses its footing and falls.
                    STAGGERED.remove(mob);
                    Knockdowns.resume(mob, mine.noAi);
                    Knockdowns.topple(mob);
                    return false;
                }
                return true;
            }
            if (age == mine.ticks) {
                Knockdowns.resume(mob, mine.noAi);
            }
            if (age >= mine.ticks + BRACE) {
                STAGGERED.remove(mob);
                return false;
            }
            return true;
        });
    }

    // Whether the game had it without AI of its own, before a stagger stilled it.
    public static boolean ownNoAi(Mob mob) {
        Stagger stagger = STAGGERED.get(mob);
        return stagger != null && stagger.age < stagger.ticks ? stagger.noAi : mob.isNoAi();
    }

    // Something else takes the creature over (a throw, a hold): the stagger ends, its AI left as that has set it.
    public static void forget(Mob mob) {
        STAGGERED.remove(mob);
    }

    private static void tell(Mob mob, Impacts.Impact impact, double strength, ImpactPayload.Reaction reaction,
            int ticks) {
        PacketDistributor.sendToPlayersTrackingEntity(mob, new ImpactPayload(mob.getId(), impact.at(), impact.way(),
                (float) strength, reaction, ticks));
    }

    public static void clear() {
        for (Map.Entry<Mob, Stagger> stagger : new ArrayList<>(STAGGERED.entrySet())) {
            if (stagger.getValue().age < stagger.getValue().ticks) {
                Knockdowns.resume(stagger.getKey(), stagger.getValue().noAi);
            }
        }
        STAGGERED.clear();
    }
}
