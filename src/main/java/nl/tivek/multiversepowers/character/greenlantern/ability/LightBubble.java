package nl.tivek.multiversepowers.character.greenlantern.ability;

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
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * The Light Bubble, Green Lantern's prison: a press of its key and the ring throws a bubble of hard light round the
 * creature he looks at, a solid cage of glowing struts, and lifts it off the ground. In there it can do nothing: it
 * cannot move and it cannot hurt anyone, for {@code holdSeconds}, and then the bubble bursts and lets it drop.
 * <ul>
 * <li>A second press pounds the creature into the ground in its bubble: the ring yanks the bubble up and slams it down,
 * then swings it up over its own height onto his left and slams it down there, and swings it over once more onto his
 * right for the hardest slam of all. Every slam hurts the creature (the last one the ability's full damage), throws up
 * the ground and sends out a shockwave that knocks away what stands round it; the last one breaks the bubble into solid
 * pieces.</li>
 * <li>Crouching and pressing the key lets it go without harm.</li>
 * </ul>
 * The cooldown only starts once the bubble is gone. Bosses and anything too big for it cannot be caught.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class LightBubble implements Effect {
    /** How long the bubble takes to grow round its creature, in ticks. */
    public static final int FORM_TICKS = 8;
    /** How long it takes to lift its creature off the ground, in ticks. */
    public static final int LIFT_TICKS = 20;
    /** How long it takes to break up, in ticks, once it burst or struck. */
    public static final int BREAK_TICKS = 10;
    /**
     * What the bubble is doing, sent as its variant: holding its creature, being pounded into the ground, breaking up.
     * The first update of a pound is sent on its first tick, so clients time the pound from that update.
     */
    public static final int HOLDING = 0;
    public static final int SMASHING = 1;
    public static final int BREAKING = 2;
    // The pound, slam by slam: how many ticks the bubble is swung up (quickly, hanging a moment at the top) and how many
    // it takes to come down (faster and faster); between two slams it lies on the ground for a tick.
    private static final int[] RISE = { 5, 5, 6 };
    private static final int STRIKE = 3;
    /** The ticks of the pound its slams strike the ground on; the last one breaks the bubble. */
    public static final int[] POUND = pound();
    // How far over where it comes from each slam swings the bubble up, and how far the second and third throw it off to
    // his left and to his right, in blocks.
    private static final double[] SWING = { 1.8, 2.6, 3.2 };
    private static final double SIDE = 1.8;
    // What every slam before the last deals, of the ability's damage (the last deals all of it), and how far its
    // shockwave reaches and throws, next to the last one's.
    private static final float EARLY_DAMAGE = 0.35F;
    private static final double EARLY_WAVE = 0.7;
    private static final double EARLY_THROW = 0.55;
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
    // When the pound or the breaking up began, and where the bubble was then; for the pound, where each slam strikes
    // the ground (the middle of the bubble as it lies there) and how high each swings it up.
    private int since;
    private Vec3 from;
    private Vec3[] slams = new Vec3[0];
    private double[] tops = new double[0];
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
    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability, int data) {
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
        if (Recharge.busy(owner)) {
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
        Effects.start(level, bubble);
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
    public static void clear() {
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
            int p = this.age - this.since;
            this.center = this.pounded(p);
            this.hold();
            for (int k = 0; k < POUND.length; k++) {
                if (p == POUND[k]) {
                    this.strike(level, k);
                }
            }
        } else {
            // Lifted off the ground as it forms round its creature, bobbing a little in the air.
            double lift = this.ability.value("liftBlocks") * Ease.smooth((double) this.age / LIFT_TICKS);
            double bob = BOB * Math.sin(this.age * 0.15) * Ease.smooth((this.age - LIFT_TICKS) / 10.0);
            this.center = this.base.add(0.0, this.target.getBbHeight() * 0.5 + lift + bob, 0.0);
            this.hold();
            if (this.age % 20 == 0) {
                ParticleFx.sphere(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), this.center, this.radius, 6, 0.0);
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

    /**
     * A second press: the pound. Where each slam will strike is worked out now: straight down below the bubble, then off
     * to his left and to his right of that, never through a wall, and never swung up into a ceiling.
     */
    private void smash(ServerLevel level) {
        this.phase = SMASHING;
        // It is sent as pounded from the next tick on, and that tick is the first of the pound (see POUND).
        this.since = this.age + 1;
        this.from = this.center;
        Vec3 flat = new Vec3(this.facing.x, 0.0, this.facing.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        this.slams = new Vec3[POUND.length];
        this.tops = new double[POUND.length];
        this.slams[0] = this.floorBelow(level, this.center);
        for (int k = 1; k < POUND.length; k++) {
            this.slams[k] = this.beside(level, this.slams[0], right.scale(k % 2 == 1 ? -SIDE : SIDE));
        }
        for (int k = 0; k < POUND.length; k++) {
            Vec3 a = k == 0 ? this.from : this.slams[k - 1];
            Vec3 b = this.slams[k];
            double high = Math.max(a.y, b.y);
            Vec3 middle = new Vec3((a.x + b.x) * 0.5, high, (a.z + b.z) * 0.5);
            BlockHitResult ceiling = level.clip(new ClipContext(middle, middle.add(0.0, SWING[k] + this.radius, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            this.tops[k] = ceiling.getType() == HitResult.Type.MISS ? high + SWING[k]
                    : Math.max(high, ceiling.getLocation().y - this.radius);
        }
        this.sound(level, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.6F);
        this.sound(level, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.9F);
    }

    /** Where the bubble lies on the ground below {@code at}: sunk a little into it, as it strikes. */
    private Vec3 floorBelow(ServerLevel level, Vec3 at) {
        Vec3 below = at.subtract(0.0, this.radius + 24.0, 0.0);
        BlockHitResult ground = level.clip(new ClipContext(at, below, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, CollisionContext.empty()));
        double floorY = ground.getType() == HitResult.Type.MISS ? below.y : ground.getLocation().y;
        return new Vec3(at.x, Math.min(at.y, floorY + this.radius * 0.8), at.z);
    }

    /**
     * Where the bubble lies on the ground {@code aside} of {@code base}: only as far as it gets before a wall, and on
     * whatever ground is there, a step higher or lower.
     */
    private Vec3 beside(ServerLevel level, Vec3 base, Vec3 aside) {
        Vec3 to = base.add(aside);
        BlockHitResult wall = level.clip(new ClipContext(base, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                CollisionContext.empty()));
        if (wall.getType() != HitResult.Type.MISS) {
            double room = Math.max(0.0, wall.getLocation().distanceTo(base) - this.radius);
            to = base.add(aside.normalize().scale(Math.min(room, aside.length())));
        }
        return this.floorBelow(level, to.add(0.0, 0.5, 0.0));
    }

    /**
     * Where the bubble is {@code p} ticks into the pound: every slam swings it up out of where it lay, quickly and
     * hanging a moment at the top, then drives it down faster and faster onto where it strikes, gliding sideways all
     * the way; after a slam it lies on the ground for a tick.
     */
    private Vec3 pounded(int p) {
        for (int k = 0; k < POUND.length; k++) {
            if (p > POUND[k]) {
                continue;
            }
            Vec3 a = k == 0 ? this.from : this.slams[k - 1];
            Vec3 b = this.slams[k];
            int start = k == 0 ? 0 : POUND[k - 1] + 1;
            if (p <= start) {
                return a;
            }
            double s = p - start;
            double across = Ease.smooth(s / (POUND[k] - start));
            double y;
            if (s <= RISE[k]) {
                double u = s / RISE[k];
                y = Mth.lerp(1.0 - (1.0 - u) * (1.0 - u), a.y, this.tops[k]);
            } else {
                double u = (s - RISE[k]) / STRIKE;
                y = Mth.lerp(u * u, this.tops[k], b.y);
            }
            return new Vec3(Mth.lerp(across, a.x, b.x), y, Mth.lerp(across, a.z, b.z));
        }
        return this.slams.length == 0 ? this.center : this.slams[this.slams.length - 1];
    }

    /** The ticks of the pound its slams strike on, from how long each is swung up and driven down. */
    private static int[] pound() {
        int[] ticks = new int[RISE.length];
        int t = 0;
        for (int k = 0; k < ticks.length; k++) {
            t += (k == 0 ? 0 : 1) + RISE[k] + STRIKE;
            ticks[k] = t;
        }
        return ticks;
    }

    /**
     * Slam {@code k} of the pound strikes the ground: its creature is hurt (by the last one the full damage), the ground
     * bursts up round it and a shockwave throws whatever stands round it away, hurting it half as much. The last slam is
     * the hardest, and breaks the bubble into solid pieces.
     */
    private void strike(ServerLevel level, int k) {
        boolean last = k == POUND.length - 1;
        float damage = this.ability.getDamage() * (last ? 1.0F : EARLY_DAMAGE);
        this.target.invulnerableTime = 0;
        this.target.hurt(level.damageSources().playerAttack(this.owner), damage);
        Vec3 at = new Vec3(this.center.x, this.center.y - this.radius * 0.8, this.center.z);
        double reach = this.ability.value("slamRadius") * (last ? 1.0 : EARLY_WAVE);
        double thrown = last ? 1.0 : EARLY_THROW;
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
                double near = 1.0 - 0.5 * Math.min(1.0, distance / Math.max(reach, 1.0E-3));
                other.setDeltaMovement(other.getDeltaMovement().add(away.scale(1.3 * thrown * near * (1.0 - resist)))
                        .add(0.0, 0.6 * thrown * near * (1.0 - resist), 0.0));
                other.hurtMarked = true;
            }
        }
        this.burstUp(level, at, reach, last);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(PowerRing.newId(), this.owner.getId(), at, this.facing, (float) Math.max(reach, 1.0),
                        1.0F, 0.0F, false, ConstructPayload.POUND, k, 0, null));
        if (last) {
            this.end(level);
        }
    }

    /**
     * The ground bursts up where the bubble strikes: pillars of its own dust and chunks of it thrown up all round, the
     * green of the shockwave running out over it, and the boom; the last slam louder and bigger than the others.
     */
    private void burstUp(ServerLevel level, Vec3 at, double reach, boolean last) {
        BlockState ground = level.getBlockState(BlockPos.containing(at.subtract(0.0, 0.2, 0.0)));
        if (!ground.isAir()) {
            int pillars = last ? 34 : 20;
            for (int i = 0; i < pillars; i++) {
                double angle = Math.PI * 2.0 * i / pillars + ParticleFx.spread(0.15);
                double out = this.radius * 0.7 + ParticleFx.RANDOM.nextDouble() * reach * 0.6;
                ParticleFx.send(level, new BlockParticleOption(ParticleTypes.DUST_PILLAR, ground),
                        at.x + Math.cos(angle) * out, at.y + 0.1, at.z + Math.sin(angle) * out, 1, 0.0, 0.0, 0.0, 0.0);
            }
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    last ? 90 : 45, this.radius * 0.7, 0.2, this.radius * 0.7, last ? 0.45 : 0.3);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.GREEN, 2.0F), at.add(0.0, 0.2, 0.0), last ? 64 : 40,
                last ? 0.75 : 0.5);
        ParticleFx.send(level, ParticleTypes.EXPLOSION, at.x, at.y + 0.5, at.z, last ? 3 : 1, this.radius * 0.4, 0.1,
                this.radius * 0.4, 0.0);
        ParticleFx.send(level, ParticleTypes.POOF, at.x, at.y + 0.3, at.z, last ? 30 : 16, this.radius * 0.6, 0.1,
                this.radius * 0.6, 0.12);
        this.sound(level, SoundEvents.MACE_SMASH_GROUND_HEAVY, last ? 1.6F : 1.2F, last ? 0.7F : 0.85F);
        this.sound(level, SoundEvents.ANVIL_LAND, last ? 1.0F : 0.7F, last ? 0.5F : 0.65F);
        this.sound(level, SoundEvents.AMETHYST_BLOCK_HIT, 1.2F, last ? 0.6F : 0.8F);
        if (last) {
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), this.center, 40, 0.4);
            this.sound(level, SoundEvents.GENERIC_EXPLODE.value(), 1.1F, 0.9F);
            this.sound(level, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.6F, 0.7F);
        }
    }

    /** The bubble bursts by itself (its time is up), or he lets its creature go: it drops, unhurt. */
    private void burst(ServerLevel level, boolean timeUp) {
        if (this.phase == BREAKING) {
            return;
        }
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), this.center, 22, 0.25);
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

    /** Nothing a creature in a bubble does can hurt anyone: not its bite, not its arrows. */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker != null && TRAPPED.containsKey(attacker.getId())) {
            event.setCanceled(true);
        }
    }
}
