package nl.tivek.welcomescreen.character.lantern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.network.ConstructPayload;
import nl.tivek.welcomescreen.spell.HeldMobs;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * The Light Bubble, Green Lantern's prison: a press of its key and the ring throws a bubble of hard light round the
 * creature he looks at, a solid cage of glowing struts, and lifts it off the ground. In there it can do nothing: it
 * cannot move and it cannot hurt anyone, for {@code holdSeconds}, and then the bubble bursts and lets it drop.
 * <ul>
 * <li>A second press smashes the bubble down onto the ground with the creature in it: it takes the ability's damage,
 * a small shockwave throws what stands round it away (half the damage), and the bubble breaks into solid pieces.</li>
 * <li>Crouching and pressing the key lets it go without harm.</li>
 * </ul>
 * The cooldown only starts once the bubble is gone. Bosses and anything too big for it cannot be caught.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID)
public final class LightBubble implements SpellEffect {
    /** How long the bubble takes to grow round its creature, in ticks. */
    public static final int FORM_TICKS = 8;
    /** How long it takes to lift its creature off the ground, in ticks. */
    public static final int LIFT_TICKS = 20;
    /** How long the bubble takes to come down when it is smashed onto the ground, in ticks. */
    public static final int SLAM_TICKS = 6;
    /** How long it takes to break up, in ticks, once it burst or struck. */
    public static final int BREAK_TICKS = 10;
    /** What the bubble is doing, sent as its variant: holding its creature, being smashed down, breaking up. */
    public static final int HOLDING = 0;
    public static final int SMASHING = 1;
    public static final int BREAKING = 2;
    // How far off the line of his look a creature may be and still be caught, in blocks.
    private static final double AIM = 0.6;
    // The biggest creature a bubble holds (its width and height, in blocks), and how strong it may be.
    private static final double WIDEST = 3.0;
    private static final double TALLEST = 4.0;
    private static final double STRONGEST = 150.0;
    // How much room the bubble leaves round its creature, and how much it bobs in the air, in blocks.
    private static final double ROOM = 0.45;
    private static final double BOB = 0.12;
    private static final double VIEW_RANGE = 128.0;

    private static final Map<UUID, LightBubble> ACTIVE = new HashMap<>();
    // The creatures a bubble holds, by entity id: nothing they do can hurt anyone.
    private static final Map<Integer, LightBubble> TRAPPED = new HashMap<>();

    private final int id = PowerRing.newId();
    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private final LivingEntity target;
    private final Vec3 base;
    private final double radius;
    private final int holdTicks;
    private final Vec3 facing;
    private int age;
    private int phase = HOLDING;
    // When it began to be smashed down or to break up, and where it was then; where it strikes the ground.
    private int since;
    private Vec3 from;
    private Vec3 floor;
    private Vec3 center;

    private LightBubble(ServerPlayer owner, CharacterAbility ability, LivingEntity target) {
        this.owner = owner;
        this.ability = ability;
        this.target = target;
        this.base = target.position();
        this.radius = Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5 + ROOM;
        this.holdTicks = Math.max(20, (int) Math.round(ability.value("holdSeconds") * 20.0));
        this.facing = owner.getLookAngle();
        this.center = target.getBoundingBox().getCenter();
        this.from = this.center;
    }

    /**
     * The bubble's key: traps what he looks at, or smashes the bubble he already holds down onto the ground; crouching,
     * it lets the creature go. It never starts the cooldown itself: that starts once the bubble is gone.
     *
     * @return always false, so the key starts no cooldown of its own
     */
    static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability, int data) {
        LightBubble held = ACTIVE.get(owner.getUUID());
        boolean crouching = (data & Characters.SNEAKING) != 0;
        if (held != null) {
            if (crouching) {
                held.burst(level, false);
            } else if (held.phase == HOLDING && held.age >= FORM_TICKS) {
                held.smash(level);
            }
            return false;
        }
        if (crouching) {
            return false;
        }
        if (Lantern.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (GiantFist.holding(owner) || AirStrike.calling(owner) || LightFlare.up(owner)) {
            return false;
        }
        float cost = (float) ability.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        LivingEntity target = aim(owner, level, ability.value("rangeBlocks"));
        if (target == null) {
            PowerRing.tell(owner, "bubble_none");
            return false;
        }
        if (target.getBbWidth() > WIDEST || target.getBbHeight() > TALLEST || target.getMaxHealth() > STRONGEST
                || TRAPPED.containsKey(target.getId()) || HeldMobs.isHeld(target)) {
            PowerRing.tell(owner, "bubble_too_big");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        LightBeam.stop(owner);
        LightBubble bubble = new LightBubble(owner, ability, target);
        if (target instanceof Mob mob) {
            HeldMobs.hold(mob);
        }
        ACTIVE.put(owner.getUUID(), bubble);
        TRAPPED.put(target.getId(), bubble);
        SpellCasting.start(level, bubble);
        PowerRing.tell(owner, "bubble");
        bubble.sound(level, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.5F);
        bubble.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.8F);
        bubble.sound(level, SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.2F, 0.7F);
        bubble.send(level);
        return false;
    }

    /**
     * The creature he looks at: the first one along his line of sight within {@code range}, before any wall, that the
     * ring may take. Never his own pets.
     */
    @Nullable
    private static LivingEntity aim(ServerPlayer owner, ServerLevel level, double range) {
        Vec3 eye = owner.getEyePosition();
        Vec3 end = eye.add(owner.getLookAngle().scale(range));
        BlockHitResult wall = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                owner));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(owner, eye, end, new AABB(eye, end).inflate(1.0),
                entity -> entity instanceof LivingEntity living && PowerRing.canHit(owner, entity)
                        && entity.isPickable() && !(living instanceof OwnableEntity pet && pet.getOwner() == owner),
                eye.distanceToSqr(end));
        if (hit == null) {
            // Aimed a hair off: the nearest creature close to his line of sight.
            LivingEntity best = null;
            double nearest = AIM * AIM;
            Vec3 way = end.subtract(eye);
            double length = way.lengthSqr();
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(AIM),
                    entity -> PowerRing.canHit(owner, entity)
                            && !(entity instanceof OwnableEntity pet && pet.getOwner() == owner))) {
                Vec3 middle = living.getBoundingBox().getCenter();
                double t = length < 1.0E-9 ? 0.0 : Mth.clamp(middle.subtract(eye).dot(way) / length, 0.0, 1.0);
                double off = eye.add(way.scale(t)).distanceToSqr(middle) - Math.pow(living.getBbWidth() * 0.5, 2.0);
                if (off < nearest) {
                    nearest = off;
                    best = living;
                }
            }
            return best;
        }
        return (LivingEntity) hit.getEntity();
    }

    /** True while this creature sits in a bubble: it can do nothing. */
    public static boolean trapped(Entity entity) {
        return TRAPPED.containsKey(entity.getId());
    }

    /** True while this player holds a creature in a bubble. */
    static boolean holding(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    /** The server stops: every creature is let go (the mobs get their own will back). */
    static void clear() {
        for (LightBubble bubble : ACTIVE.values()) {
            bubble.free();
        }
        ACTIVE.clear();
        TRAPPED.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        this.age++;
        if (this.phase == BREAKING) {
            if (this.age - this.since >= BREAK_TICKS) {
                PacketDistributor.sendToPlayersInDimension(level, ConstructPayload.remove(this.id));
                return false;
            }
            this.send(level);
            return true;
        }
        if (!PowerRing.fuels(this.owner, level) || !this.target.isAlive() || this.target.level() != level
                || this.target.isRemoved()) {
            this.burst(level, false);
            this.send(level);
            return true;
        }
        if (this.phase == SMASHING) {
            double u = Math.min(1.0, (double) (this.age - this.since) / SLAM_TICKS);
            this.center = this.from.lerp(this.floor, u * u);
            this.hold();
            if (u >= 1.0) {
                this.strike(level);
            }
        } else {
            // Lifted off the ground as it forms round its creature, bobbing a little in the air.
            double lift = this.ability.value("liftBlocks") * smooth((double) this.age / LIFT_TICKS);
            double bob = BOB * Math.sin(this.age * 0.15) * smooth((this.age - LIFT_TICKS) / 10.0);
            this.center = this.base.add(0.0, this.target.getBbHeight() * 0.5 + lift + bob, 0.0);
            this.hold();
            if (this.age % 20 == 0) {
                SpellFx.sphere(level, SpellFx.dust(PowerRing.BRIGHT, 0.9F), this.center, this.radius, 6, 0.0);
            }
            if (this.age >= this.holdTicks) {
                this.burst(level, true);
            }
        }
        this.send(level);
        return true;
    }

    /** Keeps its creature right in the middle of it, still. */
    private void hold() {
        double x = this.center.x;
        double y = this.center.y - this.target.getBbHeight() * 0.5;
        double z = this.center.z;
        this.target.setDeltaMovement(Vec3.ZERO);
        this.target.resetFallDistance();
        if (this.target instanceof ServerPlayer player) {
            player.teleportTo(player.serverLevel(), x, y, z, player.getYRot(), player.getXRot());
        } else {
            this.target.setPos(x, y, z);
        }
    }

    /** A second press: he hurls the bubble down onto the ground below it. */
    private void smash(ServerLevel level) {
        this.phase = SMASHING;
        this.since = this.age;
        this.from = this.center;
        Vec3 below = this.center.subtract(0.0, this.radius + 24.0, 0.0);
        BlockHitResult ground = level.clip(new ClipContext(this.center, below, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, CollisionContext.empty()));
        double floorY = ground.getType() == HitResult.Type.MISS ? below.y : ground.getLocation().y;
        this.floor = new Vec3(this.center.x, Math.min(this.center.y, floorY + this.radius * 0.8), this.center.z);
        this.sound(level, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        this.sound(level, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.9F);
    }

    /**
     * The smashed bubble strikes the ground: its creature takes the full damage, a small shockwave throws whatever stands
     * round it away and hurts it half as much, and the bubble breaks into solid pieces.
     */
    private void strike(ServerLevel level) {
        float damage = this.ability.getDamage();
        this.target.invulnerableTime = 0;
        this.target.hurt(level.damageSources().playerAttack(this.owner), damage);
        Vec3 at = new Vec3(this.center.x, this.floor.y - this.radius * 0.8, this.center.z);
        double reach = this.ability.value("slamRadius");
        if (reach > 0.0) {
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(reach + 1.0),
                    entity -> entity != this.target && PowerRing.canHit(this.owner, entity)
                            && !(entity instanceof OwnableEntity pet && pet.getOwner() == this.owner))) {
                Vec3 middle = other.getBoundingBox().getCenter();
                double distance = middle.distanceTo(at);
                if (distance > reach + other.getBbWidth() * 0.5) {
                    continue;
                }
                other.invulnerableTime = 0;
                other.hurt(level.damageSources().playerAttack(this.owner), damage * 0.5F);
                Vec3 away = new Vec3(middle.x - at.x, 0.0, middle.z - at.z);
                away = away.lengthSqr() < 1.0E-4 ? Vec3.ZERO : away.normalize();
                double resist = Mth.clamp(other.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                other.setDeltaMovement(other.getDeltaMovement().add(away.scale(0.9 * (1.0 - resist)))
                        .add(0.0, 0.45 * (1.0 - resist), 0.0));
                other.hurtMarked = true;
            }
        }
        BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(0.0, 0.2, 0.0)));
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z, 40,
                    this.radius * 0.6, 0.1, this.radius * 0.6, 0.25);
        }
        SpellFx.shockwave(level, SpellFx.dust(PowerRing.GREEN, 1.8F), at.add(0.0, 0.2, 0.0), 48, 0.55);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.5F), this.center, 30, 0.3);
        level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, 1, 0.0, 0.0, 0.0, 0.0);
        this.sound(level, SoundEvents.ANVIL_LAND, 0.9F, 0.7F);
        this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.3F);
        this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.4F, 0.8F);
        this.end(level);
    }

    /** The bubble bursts by itself (its time is up), or he lets its creature go: it drops, unhurt. */
    private void burst(ServerLevel level, boolean timeUp) {
        if (this.phase == BREAKING) {
            return;
        }
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.3F), this.center, 22, 0.25);
        this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.1F, timeUp ? 1.0F : 1.3F);
        this.sound(level, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.4F, 0.8F);
        this.end(level);
    }

    /** It breaks up: its creature is let go and his cooldown starts. */
    private void end(ServerLevel level) {
        this.phase = BREAKING;
        this.since = this.age;
        this.free();
        ACTIVE.remove(this.owner.getUUID(), this);
        if (!this.owner.hasDisconnected() && this.owner.isAlive()) {
            Characters.startCooldown(this.owner, this.ability);
        }
    }

    /** Its creature gets its own will back. */
    private void free() {
        TRAPPED.remove(this.target.getId(), this);
        if (this.target instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        this.target.resetFallDistance();
    }

    private void send(ServerLevel level) {
        float grown = this.phase == BREAKING ? 1.0F : Math.min(1.0F, (float) this.age / FORM_TICKS);
        PacketDistributor.sendToPlayersNear(level, null, this.center.x, this.center.y, this.center.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.center, this.facing, (float) this.radius,
                        grown, this.phase == BREAKING ? this.age - this.since : this.target.getId(), this.phase != BREAKING,
                        ConstructPayload.BUBBLE, this.phase, this.age, null));
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.center.x, this.center.y, this.center.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** 0 below 0, 1 above 1, and a smooth S-curve in between. */
    private static double smooth(double t) {
        double c = Mth.clamp(t, 0.0, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /** Nothing a creature in a bubble does can hurt anyone: not its bite, not its arrows. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && TRAPPED.containsKey(attacker.getId())) {
            event.setCanceled(true);
        }
    }
}
