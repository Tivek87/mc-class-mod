package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.FlyingMob;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.FlyingAnimal;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effects;

// A creature a blow throws goes limp in every player's game (Ragdolls), lies where it falls and gets up: meanwhile
// it must not walk or fight on, so from the moment it lands it is stilled (no AI) for as long as that takes. The
// same throw a player's game counts: hurt, then sent off hard enough.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Knockdowns {
    private static final String SAVED_TAG = "welcomescreen_downed_noai";
    // As Ragdolls counts a throw, in blocks a tick: along the ground, and upward; and too heavy to throw.
    private static final double THROWN = 0.9;
    private static final double TOSSED = 0.6;
    private static final double HEAVY = 3.5;
    // Ticks a thrown creature lies still from landing: as long as it lies and gets up in a player's game.
    private static final int DOWN = 75;
    private static final int LONGEST_FLIGHT = 200;
    private static final int WATCH = 3;

    private static final Map<Mob, Down> DOWNED = new IdentityHashMap<>();

    private static final class Down {
        final boolean noAi;
        int landed = -1;

        Down(boolean noAi) {
            this.noAi = noAi;
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
        if (DOWNED.remove(mob) != null) {
            mob.getPersistentData().remove(SAVED_TAG);
        }
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level)
                || !mayFly(mob)) {
            return;
        }
        // The push that throws it is given right after the blow: looked at over the next ticks.
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

    private static boolean mayFly(Mob mob) {
        return !mob.isNoGravity() && !mob.isPassenger() && !mob.isVehicle() && !mob.isInWater() && !mob.isInLava()
                && !(mob instanceof FlyingMob) && !(mob instanceof FlyingAnimal) && !(mob instanceof Bat)
                && mob.getBbWidth() * mob.getBbWidth() * mob.getBbHeight() <= HEAVY;
    }

    private static void down(ServerLevel level, Mob mob) {
        Down down = DOWNED.get(mob);
        if (down != null) {
            // Thrown again while down: it lies from where it lands this time.
            down.landed = -1;
            return;
        }
        down = new Down(mob.isNoAi());
        DOWNED.put(mob, down);
        mob.getPersistentData().putBoolean(SAVED_TAG, down.noAi);
        Down mine = down;
        Effects.start(level, (lvl, age) -> {
            if (DOWNED.get(mob) != mine) {
                return false;
            }
            if (!mob.isAlive() || mob.isRemoved() || HeldMobs.isHeld(mob)) {
                if (!HeldMobs.isHeld(mob)) {
                    up(mob);
                }
                return false;
            }
            if (mine.landed < 0) {
                if (mob.onGround() && age > 1 || mob.isInWater() || age > LONGEST_FLIGHT) {
                    mine.landed = age;
                    mob.setNoAi(true);
                    mob.getNavigation().stop();
                }
                return true;
            }
            Vec3 push = mob.getDeltaMovement();
            mob.setDeltaMovement(push.x * 0.5, push.y, push.z * 0.5);
            if (age - mine.landed >= DOWN) {
                up(mob);
                return false;
            }
            return true;
        });
    }

    private static void up(Mob mob) {
        Down down = DOWNED.remove(mob);
        if (down != null) {
            mob.setNoAi(down.noAi);
            mob.getPersistentData().remove(SAVED_TAG);
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
            up(mob);
        }
    }
}
