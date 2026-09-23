package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * The Lantern Flare: Green Lantern throws his ring fist up high and the ring shapes his lantern over it; light gathers
 * in the lantern for most of a second, and it bursts like a small sun, breaking into pieces. Every creature within the
 * setting {@code radiusBlocks} that can see the ring is struck:
 * blinded for {@code blindSeconds}, and slowed down and weakened for {@code stunSeconds}; a creature that was after
 * someone loses them. The creatures of the dark (see {@link Fear}) cannot bear it: they burn, are hurt (the ability's
 * damage) and flee a moment. Players are only struck where players may fight each other.
 *
 * <p>The flash is light and nothing of it stays; the lantern is a construct, grown out of the ring's light and broken
 * into solid pieces. Everyone around sees it, and whoever looks at the burst is dazzled a moment (on their own screen,
 * see the client's FlareLight).
 */
public final class LightFlare implements SpellEffect {
    /** How long the light gathers in the lantern before it bursts, and how long the burst lasts, in ticks. */
    public static final int GATHER_TICKS = 14;
    public static final int BURST_TICKS = 12;
    // How long the creatures of the dark flee from the burst, in ticks, and how long they burn.
    private static final int FLEE_TICKS = 60;
    private static final int BURN_TICKS = 80;
    private static final double VIEW_RANGE = 96.0;

    private static final Map<UUID, LightFlare> ACTIVE = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private int age;

    private LightFlare(ServerPlayer owner, CharacterAbility ability) {
        this.owner = owner;
        this.ability = ability;
    }

    /**
     * The flare key: he throws his ring fist up and the light gathers, as long as the ring is free and can pay for it.
     *
     * @return true when it began
     */
    static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (ACTIVE.containsKey(owner.getUUID())) {
            return false;
        }
        if (Lantern.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (GiantFist.holding(owner)) {
            PowerRing.tell(owner, "busy_fist");
            return false;
        }
        float cost = (float) ability.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        // The ring hand goes up: the beam it pours out stops.
        LightBeam.stop(owner);
        LightFlare flare = new LightFlare(owner, ability);
        ACTIVE.put(owner.getUUID(), flare);
        SpellCasting.start(level, flare);
        flare.sound(level, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.2F);
        flare.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.7F);
        flare.send(level);
        return true;
    }

    /** True while this player's flare gathers or bursts: his ring fist is up. */
    static boolean up(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    /** The server stops: no flare is going any more. */
    static void clear() {
        ACTIVE.clear();
    }

    /**
     * Where the ring is while the flare goes: in his fist, thrown up high over his head, a little to his right. The
     * lantern and its burst are just over it.
     */
    private Vec3 ring() {
        Vec3 look = this.owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        return this.owner.getEyePosition().add(0.0, 0.75, 0.0).add(right.scale(0.3)).add(flat.scale(0.15));
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (!PowerRing.fuels(this.owner, level)) {
            this.end(level);
            return false;
        }
        this.age++;
        if (this.age < GATHER_TICKS && this.age % 3 == 0) {
            // The light gathers: specks of it run into the ring from all round.
            Vec3 at = this.ring();
            for (int i = 0; i < 6; i++) {
                Vec3 from = at.add(SpellFx.spread(2.0), SpellFx.spread(2.0), SpellFx.spread(2.0));
                SpellFx.fly(level, SpellFx.dust(PowerRing.PALE, 1.0F), from, at.subtract(from).normalize(), 0.25);
            }
        }
        if (this.age == GATHER_TICKS) {
            this.burst(level);
        }
        if (this.age >= GATHER_TICKS + BURST_TICKS) {
            this.end(level);
            return false;
        }
        this.send(level);
        return true;
    }

    /** The light bursts out: everything that can see the ring is struck by it. */
    private void burst(ServerLevel level) {
        Vec3 at = this.ring();
        double radius = this.ability.value("radiusBlocks");
        int blind = (int) Math.round(this.ability.value("blindSeconds") * 20.0);
        int stun = (int) Math.round(this.ability.value("stunSeconds") * 20.0);
        AABB area = new AABB(at, at).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                entity -> PowerRing.canHit(this.owner, entity))) {
            Vec3 eye = target.getEyePosition();
            if (eye.distanceToSqr(at) > radius * radius || !this.sees(level, at, eye)) {
                continue;
            }
            if (blind > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, blind, 0), this.owner);
            }
            if (stun > 0) {
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, stun, 3), this.owner);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stun, 1), this.owner);
            }
            if (target instanceof Mob mob) {
                // Dazzled, it loses whoever it was after and stands a moment.
                mob.setTarget(null);
                mob.getNavigation().stop();
                if (mob.getType().is(Fear.FEARS_THE_LIGHT)) {
                    target.invulnerableTime = 0;
                    target.hurt(level.damageSources().playerAttack(this.owner), this.ability.getDamage());
                    target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), BURN_TICKS));
                    Fear.frighten(mob, at, level.getGameTime() + FLEE_TICKS);
                }
            }
            SpellFx.cloud(level, SpellFx.dust(PowerRing.BRIGHT, 1.2F), target.getBoundingBox().getCenter(), 8, 0.3,
                    0.05);
        }
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 2.0F), at, 60, 0.6);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.GREEN, 2.5F), at, 40, 0.4);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        this.sound(level, SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F, 0.8F);
        this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.4F, 1.8F);
        this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 0.8F);
    }

    /** Whether nothing solid is in between: the flash only strikes what can see the ring. */
    private boolean sees(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, this.owner))
                .getType() == HitResult.Type.MISS;
    }

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
    }

    private void send(ServerLevel level) {
        Vec3 at = this.ring();
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.owner.getLookAngle(),
                        (float) this.ability.value("radiusBlocks"), 1.0F, 0.0F, true, ConstructPayload.FLARE, 0,
                        this.age, null));
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.owner.getX(), this.owner.getY() + 1.8, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }
}
