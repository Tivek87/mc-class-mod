package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.ExplosionKnockbackEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.effect.Effects;

// A creature a blow or a blast throws, or a power lets go of or knocks down, goes limp in every player's game
// (Ragdolls), lies where it falls and gets up: all that while it must not walk or fight, so it has no AI from the
// throw until it stands again, and is moved here as the game moves any falling creature (a player knocked down:
// PlayerKnockdowns). The players near are told when it lands and how long it lies, so its body gets up in time. A
// throw counts a little sooner than in a player's game: a body the server holds down is thrown limp there too, while
// one it never held down gets up at once.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Knockdowns {
    private static final String SAVED_TAG = "welcomescreen_downed_noai";
    private static final TagKey<EntityType<?>> STAYS_UP = TagKey.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "knockdown_none"));
    // In blocks a tick: pushed along the ground, upward, or by a blast; and too heavy to throw.
    private static final double THROWN = 0.8;
    private static final double TOSSED = 0.55;
    private static final double BLOWN = 0.2;
    // A player is thrown limp only by a blast that really throws them, not the push of one further off.
    private static final double PLAYER_BLOWN = 0.5;
    private static final double HEAVY = 3.5;
    // Ticks a thrown creature stays down from landing: it lies 3 seconds, then gets up in every player's game (a little
    // later there, as its body comes down after it), and stands a moment before its AI comes back. The players' games
    // get it up within the last RISING of them: a blow then knocks it down again from however far up it is.
    static final int DOWN = 125;
    private static final int RISING = 60;
    static final int LONGEST_FLIGHT = 200;
    // One no player's game can show limp, knocked down by a power, only drops and stays still this long.
    private static final int STILL = 60;
    // As long as a player's game shows a creature hurt, the push that throws it may still come.
    private static final int WATCH = 10;
    static final int FLYING = -1;
    // A creature without AI is slowed by this every tick by the game itself; its flight here makes up for it.
    private static final double STILL_DRAG = 0.98;
    // Flying along the ground faster than BUMPS (blocks a tick), it crashes into the creatures in its way, this far
    // round it; one pushed on faster than BOWLED goes down too, tossed up a little.
    private static final double BUMPS = 0.4;
    private static final double BUMP_REACH = 0.15;
    private static final double BOWLED = 0.3;
    private static final double TOSS = 0.2;

    private static final Map<Mob, Down> DOWNED = new IdentityHashMap<>();

    private static final class Down {
        final boolean noAi;
        // Whether the players' games show it limp (else it only drops and stays still), and so are told of it.
        final boolean limp;
        // The creatures it has crashed into in this flight, each only once.
        final Set<Mob> bumped = Collections.newSetFromMap(new IdentityHashMap<>());
        // Knocked down by a power: it falls even if it flies by itself.
        boolean forced;
        // How fast it fell here last tick (blocks a tick, below 0 downward).
        double falling;
        int age;
        int thrown;
        int landed = -1;

        Down(boolean noAi, boolean limp, boolean forced) {
            this.noAi = noAi;
            this.limp = limp;
            this.forced = forced;
        }

        int lies() {
            return this.limp ? DOWN : STILL;
        }
    }

    private Knockdowns() {
    }

    // Whether the game had it without AI of its own, before this or a hold stilled it.
    static boolean ownNoAi(Mob mob) {
        Down down = DOWNED.get(mob);
        return down != null ? down.noAi : mob.isNoAi();
    }

    // Something takes the creature over (a hand picks it up): it keeps lying still only as long as that says.
    static void forget(Mob mob) {
        Down down = DOWNED.remove(mob);
        if (down != null) {
            mob.getPersistentData().remove(SAVED_TAG);
            tell(mob, down, 0);
        }
    }

    // A power knocks it down where it stands or flies, whatever it is: it drops, lies 3 seconds and gets up (one no
    // player's game can show limp only drops and stays still as long). Not a boss, nor one something holds.
    public static void knock(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            PlayerKnockdowns.knock(player);
        } else if (entity instanceof Mob mob && mob.level() instanceof ServerLevel level && mob.isAlive()
                && !mob.getType().is(Tags.EntityTypes.BOSSES) && !HeldMobs.isHeld(mob) && !mob.isInWater()
                && !mob.isInLava()) {
            mob.stopRiding();
            mob.ejectPassengers();
            down(level, mob, true);
        }
    }

    // The creature goes down where it is, as if thrown: a power let go of it (it hung limp in every player's game), or
    // blows wore it out (Fatigue).
    static void drop(Mob mob) {
        if (mob.level() instanceof ServerLevel level && mob.isAlive() && falls(mob) && !mob.isInWater()
                && !mob.isInLava()) {
            down(level, mob);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerHurt(player, event.getSource().getEntity());
            return;
        }
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !mayFly(mob)) {
            return;
        }
        Down down = DOWNED.get(mob);
        if (down != null && down.landed >= 0 && down.age - down.landed >= DOWN - RISING && !HeldMobs.isHeld(mob)
                && Fatigue.blow(event.getSource())) {
            // Hit as it gets up: down again, and it lies anew from where it falls.
            down(level, mob);
        }
        // The push that throws it is given right after the blow, or by a power a few ticks on: looked at meanwhile.
        Effects.start(level, (lvl, age) -> {
            if (age > WATCH || !mob.isAlive() || mob.isRemoved()) {
                return false;
            }
            Vec3 push = mob.getDeltaMovement();
            if (!HeldMobs.isHeld(mob) && (push.horizontalDistanceSqr() > THROWN * THROWN || push.y > TOSSED)) {
                down(lvl, mob);
                return false;
            }
            return true;
        });
    }

    // A player a blow throws as hard goes limp too (world setting playerKnockdown). Their game moves them, so the
    // server's motion of a player holds only what was given them: the push is what this blow added to it.
    private static void playerHurt(ServerPlayer player, @Nullable Entity by) {
        if (!PowerRules.playerKnockdown() || by == player || !mayThrow(player)) {
            return;
        }
        Vec3 before = player.getDeltaMovement();
        Effects.start(player.serverLevel(), (lvl, age) -> {
            if (age > WATCH || !player.isAlive() || player.isRemoved()) {
                return false;
            }
            Vec3 push = player.getDeltaMovement().subtract(before);
            if (push.horizontalDistanceSqr() > THROWN * THROWN || push.y > TOSSED) {
                if (mayThrow(player)) {
                    PlayerKnockdowns.knock(player);
                }
                return false;
            }
            return true;
        });
    }

    // Never where lying limp would only be a nuisance or a danger: in creative, riding, gliding or flying, asleep, or
    // in water or lava.
    private static boolean mayThrow(ServerPlayer player) {
        return !player.isCreative() && !player.isSpectator() && !player.isPassenger() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.isSleeping() && !player.isInWater() && !player.isInLava();
    }

    // A blast's push is known exactly before it is given, while the creature has not moved off with it yet.
    @SubscribeEvent
    public static void onBlast(ExplosionKnockbackEvent event) {
        if (event.getAffectedEntity() instanceof ServerPlayer player) {
            if (PowerRules.playerKnockdown() && mayThrow(player)
                    && event.getExplosion().getIndirectSourceEntity() != player
                    && event.getKnockbackVelocity().lengthSqr() > PLAYER_BLOWN * PLAYER_BLOWN) {
                PlayerKnockdowns.knock(player);
            }
            return;
        }
        if (event.getAffectedEntity() instanceof Mob mob && mob.level() instanceof ServerLevel level
                && mob.isAlive() && !HeldMobs.isHeld(mob) && mayFly(mob)
                && event.getKnockbackVelocity().lengthSqr() > BLOWN * BLOWN) {
            down(level, mob);
        }
    }

    // Not one that flies by itself, rides or is ridden, nor one no player's game can show limp (knockdown_none: it
    // would stand frozen while it lay).
    static boolean falls(Mob mob) {
        return !mob.isNoGravity() && !mob.isPassenger() && !mob.isVehicle() && !(mob instanceof FlyingMob)
                && !(mob instanceof FlyingAnimal) && !(mob instanceof Bat) && !mob.getType().is(STAYS_UP);
    }

    static boolean mayFly(Mob mob) {
        return mayTopple(mob) && weight(mob) <= HEAVY;
    }

    // However heavy: what crashes into it moves it by their weights.
    private static boolean mayTopple(Mob mob) {
        return falls(mob) && !mob.isInWater() && !mob.isInLava();
    }

    private static void down(ServerLevel level, Mob mob) {
        down(level, mob, false);
    }

    private static void down(ServerLevel level, Mob mob, boolean forced) {
        Down down = DOWNED.get(mob);
        if (down != null) {
            // Thrown again while down: it flies, and lies from where it lands this time.
            down.forced |= forced;
            down.falling = 0.0;
            down.thrown = down.age;
            down.landed = -1;
            tell(mob, down, FLYING);
            return;
        }
        Down mine = new Down(mob.isNoAi(), !mob.getType().is(STAYS_UP), forced);
        DOWNED.put(mob, mine);
        mob.getPersistentData().putBoolean(SAVED_TAG, mine.noAi);
        mob.setNoAi(true);
        mob.getNavigation().stop();
        tell(mob, mine, FLYING);
        Effects.start(level, (lvl, age) -> {
            if (DOWNED.get(mob) != mine) {
                return false;
            }
            if (!mob.isAlive() || mob.isRemoved() || HeldMobs.isHeld(mob)) {
                if (!HeldMobs.isHeld(mob)) {
                    up(mob, true);
                }
                return false;
            }
            mine.age = age;
            fly(lvl, mob, mine);
            if (PowerRules.domino()) {
                bump(lvl, mob, mine);
            }
            if (mine.landed < 0) {
                int flight = age - mine.thrown;
                if (mob.onGround() && flight > 1 || mob.isInWater() || mob.isInLava() || flight > LONGEST_FLIGHT) {
                    mine.landed = age;
                    tell(mob, mine, mine.lies());
                }
                return true;
            }
            if (age - mine.landed >= mine.lies()) {
                up(mob, true);
                return false;
            }
            return true;
        });
    }

    // A creature without AI does not move by itself: while down it flies, lands and slides to a stop here as the game
    // moves any falling creature (in water it just floats where it is).
    private static void fly(ServerLevel level, Mob mob, Down down) {
        if (mob.isInWater() || mob.isInLava()) {
            return;
        }
        Vec3 push = mob.getDeltaMovement().scale(1.0 / STILL_DRAG);
        if (down.forced && push.y < 0.0 && down.falling < push.y) {
            // A chicken, parrot or bat slows its own fall as it flaps, in its own tick: knocked down, it drops.
            push = new Vec3(push.x, down.falling, push.z);
        }
        mob.setDeltaMovement(push);
        mob.move(MoverType.SELF, push);
        BlockPos below = mob.getBlockPosBelowThatAffectsMyMovement();
        float slip = mob.onGround() ? level.getBlockState(below).getFriction(level, below, mob) * 0.91F : 0.91F;
        Vec3 after = mob.getDeltaMovement();
        // A flyer keeps itself up by having no gravity while it flies; knocked down it falls all the same.
        double gravity = down.forced ? mob.getAttributeValue(Attributes.GRAVITY) : mob.getGravity();
        down.falling = (after.y - gravity) * 0.98;
        mob.setDeltaMovement(after.x * slip, down.falling, after.z * slip);
    }

    // Crashing into the creatures in its way: it and each of them go on together, at the speed their weights share (a
    // heavy one bowls a light one over and flies on; a light one hardly moves a heavy one, such as an iron golem), and
    // each pushed on fast enough goes down with it, into a pile.
    private static void bump(ServerLevel level, Mob mob, Down mine) {
        Vec3 push = mob.getDeltaMovement();
        if (push.horizontalDistanceSqr() < BUMPS * BUMPS) {
            return;
        }
        double weight = weight(mob);
        for (Mob other : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(BUMP_REACH),
                other -> other != mob && other.isAlive() && !mine.bumped.contains(other) && !HeldMobs.isHeld(other)
                        && mayTopple(other))) {
            mine.bumped.add(other);
            Vec3 theirs = other.getDeltaMovement();
            double share = weight / (weight + weight(other));
            double x = theirs.x + (push.x - theirs.x) * share;
            double z = theirs.z + (push.z - theirs.z) * share;
            push = new Vec3(x, push.y, z);
            mob.setDeltaMovement(push);
            mob.hurtMarked = true;
            other.setDeltaMovement(x, Math.max(theirs.y, TOSS), z);
            other.hurtMarked = true;
            if (x * x + z * z > BOWLED * BOWLED) {
                down(level, other);
            }
        }
    }

    // Something a power swings at `speed` (a creature it holds) crashes into `other`, which goes on at the speed their
    // weights share and, pushed on fast enough, goes down; as a thrown one crashes, but never held back itself.
    public static void bowl(LivingEntity mover, LivingEntity other, Vec3 speed) {
        double share = weight(mover) / (weight(mover) + weight(other));
        double x = speed.x * share;
        double z = speed.z * share;
        Vec3 theirs = other.getDeltaMovement();
        other.setDeltaMovement(theirs.x + x, Math.max(theirs.y, TOSS), theirs.z + z);
        other.hasImpulse = true;
        other.hurtMarked = true;
        if (x * x + z * z > BOWLED * BOWLED) {
            knock(other);
        }
    }

    private static double weight(Entity entity) {
        return entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight();
    }

    // Whether the creature is down (flying or lying) after a throw.
    static boolean isDown(Mob mob) {
        return DOWNED.containsKey(mob);
    }

    private static void up(Mob mob, boolean tell) {
        Down down = DOWNED.remove(mob);
        if (down != null) {
            mob.setNoAi(down.noAi);
            mob.getPersistentData().remove(SAVED_TAG);
            if (tell) {
                tell(mob, down, 0);
            }
        }
    }

    private static void tell(Mob mob, Down down, int ticks) {
        if (down.limp) {
            PacketDistributor.sendToPlayersTrackingEntity(mob, new KnockdownPayload(mob.getId(), ticks));
        }
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Mob mob && event.getEntity() instanceof ServerPlayer player) {
            Down down = DOWNED.get(mob);
            if (down != null && down.limp) {
                int left = down.landed < 0 ? FLYING : Math.max(1, DOWN - (down.age - down.landed));
                PacketDistributor.sendToPlayer(player, new KnockdownPayload(mob.getId(), left));
            }
        }
    }

    // A creature saved while it lay still gets its own AI back when it is loaded again.
    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Mob mob && !DOWNED.containsKey(mob)
                && mob.getPersistentData().contains(SAVED_TAG)) {
            mob.setNoAi(mob.getPersistentData().getBoolean(SAVED_TAG));
            mob.getPersistentData().remove(SAVED_TAG);
        }
    }

    public static void clear() {
        for (Mob mob : new ArrayList<>(DOWNED.keySet())) {
            up(mob, false);
        }
    }
}
