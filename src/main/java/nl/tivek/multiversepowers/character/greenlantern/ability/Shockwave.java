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

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Shockwave implements Effect {
    private static final double DROP_SPEED = 1.5;
    private static final double DROP_KEEP = 0.3;
    private static final int DROP_MAX = 200;
    private static final double GROUND_GAP = 0.05;

    private static final Map<UUID, Shockwave> DROPPING = new HashMap<>();

    private final ServerPlayer owner;
    private final CharacterAbility ability;

    private Shockwave(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.ability = ability;
    }

    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (GiantFist.holding(owner)) {
            PowerRing.tell(owner, "busy_fist");
            return false;
        }
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
        if (owner.isInWater() || owner.isInLava()) {
            return false;
        }
        Shockwave drop = new Shockwave(owner, ability);
        DROPPING.put(owner.getUUID(), drop);
        Effects.start(level, drop);
        Vec3 motion = owner.getDeltaMovement();
        owner.setDeltaMovement(motion.x * DROP_KEEP, -DROP_SPEED, motion.z * DROP_KEEP);
        owner.hurtMarked = true;
        level.playSound(null, owner.getX(), owner.getY() + 1.0, owner.getZ(), SoundEvents.MACE_SMASH_AIR,
                SoundSource.PLAYERS, 1.0F, 0.8F);
        level.playSound(null, owner.getX(), owner.getY() + 1.0, owner.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.6F);
        PowerRing.sync(owner);
        return true;
    }

    public static boolean dropping(ServerPlayer player) {
        return DROPPING.containsKey(player.getUUID());
    }

    public static void clear() {
        DROPPING.clear();
    }

    private static boolean grounded(ServerPlayer player) {
        // The server's own onGround runs a tick ahead, so it can say true while he is still in the air.
        return !player.level().noCollision(player, player.getBoundingBox().move(0.0, -GROUND_GAP, 0.0));
    }

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

    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && dropping(player)) {
            event.setDamageMultiplier(0.0F);
        }
    }
}
