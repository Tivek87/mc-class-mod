package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;

/**
 * The shockwave key: the slam of a landing at full speed (see {@link LandingSlam}) whenever Green Lantern wants it.
 * He smashes his ring fist into the ground and the ring throws up a construct in front of him that strikes and sends
 * a shockwave over the ground. On the ground that happens at once. In the air he goes down to the ground first, fist
 * cocked, and slams where he hits it: flying, his own game dives him straight down and lands it like any slam (see
 * {@link Flight#dive}); jumping or falling, he drops straight down, and the ring breaks his fall.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Shockwave implements Effect {
    // How fast he drops when he uses it jumping or falling, in blocks per tick; the game's gravity comes on top.
    private static final double DROP_SPEED = 1.5;
    // How much of his sideways speed he keeps as he drops.
    private static final double DROP_KEEP = 0.3;
    // A drop that somehow never reaches the ground ends by itself after this many ticks.
    private static final int DROP_MAX = 200;
    // How far below his feet the ground may be for him to stand on it, in blocks.
    private static final double GROUND_GAP = 0.05;

    // Everyone who drops down to a slam right now.
    private static final Map<UUID, Shockwave> DROPPING = new HashMap<>();

    private final ServerPlayer owner;
    private final CharacterAbility ability;

    private Shockwave(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.ability = ability;
    }

    /**
     * The key: a slam right here on the ground, or on the way down to one from the air.
     *
     * @return true when it went off or he is on his way down, so the cooldown starts
     */
    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (GiantFist.holding(owner)) {
            PowerRing.tell(owner, "busy_fist");
            return false;
        }
        // His last slam is still going, or he is on his way down to one already.
        if (LandingSlam.running(owner) || dropping(owner) || Flight.diving(owner) || owner.isPassenger()
                || owner.isSleeping() || owner.isFallFlying()) {
            return false;
        }
        if (PowerRing.power(owner) + 1.0E-4F < (float) ability.value("powerCost")) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        int flying = Flight.ticks(owner);
        if (flying >= 0) {
            // Not during the take-off: both fists are lifting him then.
            if (flying < Flight.ARISE_TICKS) {
                PowerRing.tell(owner, "busy_flying");
                return false;
            }
            return Flight.dive(owner, level);
        }
        if (grounded(owner)) {
            return slam(owner, level, ability);
        }
        // Swimming, there is no ground to drop onto.
        if (owner.isInWater() || owner.isInLava()) {
            return false;
        }
        Shockwave drop = new Shockwave(owner, ability);
        DROPPING.put(owner.getUUID(), drop);
        Effects.start(level, drop);
        Vec3 motion = owner.getDeltaMovement();
        owner.setDeltaMovement(motion.x * DROP_KEEP, -DROP_SPEED, motion.z * DROP_KEEP);
        // Players move themselves on their own game, so they have to be told about the push.
        owner.hurtMarked = true;
        level.playSound(null, owner.getX(), owner.getY() + 1.0, owner.getZ(), SoundEvents.MACE_SMASH_AIR,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, owner.getX(), owner.getY() + 1.0, owner.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.6F);
        PowerRing.sync(owner);
        return true;
    }

    /** True while this player drops straight down to a slam. */
    public static boolean dropping(ServerPlayer player) {
        return DROPPING.containsKey(player.getUUID());
    }

    /** The server stops: nobody drops any more. */
    public static void clear() {
        DROPPING.clear();
    }

    /**
     * True when he really stands on something. The server's own {@code onGround} cannot say so: every tick it moves
     * him one step by itself and puts him back where his game says he is, but keeps whether that step hit the ground,
     * so near the ground (or right after a push down) it is true while he is still in the air.
     */
    private static boolean grounded(ServerPlayer player) {
        return !player.level().noCollision(player, player.getBoundingBox().move(0.0, -GROUND_GAP, 0.0));
    }

    /**
     * He is down on the ground: he stops dead on his fist and the ring throws up its construct, or, when it cannot,
     * he just lands hard.
     *
     * @return true when the construct came
     */
    private static boolean slam(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        owner.setDeltaMovement(Vec3.ZERO);
        owner.hurtMarked = true;
        if (LandingSlam.start(owner, level, ability)) {
            return true;
        }
        level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.MACE_SMASH_GROUND,
                SoundSource.PLAYERS, 1.0F, 1.1F);
        return false;
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (DROPPING.get(this.owner.getUUID()) != this) {
            return false;
        }
        // No longer Green Lantern or gone, up on his ring again, or caught by water: no slam.
        if (!PowerRing.fuels(this.owner, level) || Flight.ticks(this.owner) >= 0 || this.owner.isPassenger()
                || this.owner.isSpectator() || this.owner.isFallFlying() || this.owner.isInWater()
                || this.owner.isInLava() || age > DROP_MAX) {
            this.end();
            return false;
        }
        if (!grounded(this.owner)) {
            return true;
        }
        this.end();
        slam(this.owner, level, this.ability);
        return false;
    }

    private void end() {
        DROPPING.remove(this.owner.getUUID(), this);
        this.owner.resetFallDistance();
        PowerRing.sync(this.owner);
    }

    /** Dropping down to a slam, the ring breaks his fall: however far he came down, landing never hurts him. */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && dropping(player)) {
            event.setDamageMultiplier(0.0F);
        }
    }
}
