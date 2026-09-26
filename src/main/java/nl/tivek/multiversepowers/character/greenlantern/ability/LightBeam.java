package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

public final class LightBeam implements Effect {
    private static final double VIEW_RANGE = 128.0;
    private static final double REACH = 0.35;
    private static final int FADE_TICKS = 3;

    private static final int CHARGE_FORGET = 100;
    // How much of the gathering the server must have seen: it falls behind and can count fewer ticks than his own
    // game did, and the network never brings two presses exactly as far apart as they were made.
    private static final double GATHER_SEEN = 0.5;

    private static final Map<UUID, LightBeam> FIRING = new HashMap<>();
    private static final Map<UUID, Integer> CHARGING = new HashMap<>();

    // The beam grows at these seconds of holding the button, counted from the press; its first stage comes with the
    // beam itself, once the button has been held for its hold time.
    private static final int[] STAGE_SECONDS = { 4, 6, 8, 10 };
    public static final int STAGES = STAGE_SECONDS.length + 1;
    public static final int LAST = STAGES - 1;
    public static final float[] STAGE_THICK = { 0.5F, 0.85F, 1.3F, 1.95F, 2.9F };
    // Heavy particles only this far out along the beam, never right before its owner's eyes.
    private static final double SHED_FROM = 4.0;
    private static final double FIRST_WALK = 0.85;
    private static final ResourceLocation SLOW = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "beam_slow");

    private final int id;
    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private final float baseDamage;
    private final int every;
    private final double basePerTick;
    private final double basePush;
    private final double topDamage;
    private final double topPerTick;
    private final double topWalk;
    private final int[] stageAges = new int[STAGE_SECONDS.length];
    private float damage;
    private float perTick;
    private double range;
    private double push;
    private int stage = -1;
    private int age;
    private int fade = -1;
    private Vec3 facing;
    private double length;
    private Vec3 end;

    private LightBeam(ServerPlayer owner, CharacterAbility ability) {
        this.id = PowerRing.newId();
        this.owner = owner;
        this.ability = ability;
        this.baseDamage = (float) ability.value("beamDamage");
        this.every = Math.max(1, ability.intValue("beamTicks"));
        this.basePerTick = ability.value("beamPowerPerSecond") / 20.0;
        this.basePush = ability.value("beamKnockback");
        this.topDamage = ability.value("beamTopDamage");
        this.topPerTick = ability.value("beamTopPowerPerSecond") / 20.0;
        this.topWalk = ability.value("beamTopWalk");
        for (int i = 0; i < STAGE_SECONDS.length; i++) {
            this.stageAges[i] = Math.max(1, stageFrom(i + 1, ability.holdTicks()) - ability.holdTicks());
        }
        this.facing = owner.getLookAngle();
        this.end = owner.getEyePosition();
        this.grow(0);
    }

    // Ticks from the press to where a stage begins.
    public static int stageFrom(int stage, int holdTicks) {
        return stage <= 0 ? holdTicks : STAGE_SECONDS[Math.min(stage, STAGE_SECONDS.length) - 1] * 20;
    }

    public static double range(CharacterAbility ability, int stage) {
        return Mth.lerp((double) Mth.clamp(stage, 0, LAST) / LAST, ability.value("beamRangeBlocks"),
                ability.value("beamTopRangeBlocks"));
    }

    private int stageAt(int age) {
        int stage = 0;
        while (stage < this.stageAges.length && age >= this.stageAges[stage]) {
            stage++;
        }
        return stage;
    }

    private void grow(int stage) {
        this.stage = stage;
        double climb = (double) stage / LAST;
        this.damage = (float) (this.baseDamage * Math.pow(this.topDamage, climb));
        // The last stage drains its own fixed amount a second; the stages before climb towards it.
        boolean scaled = this.basePerTick > 0.0 && this.topPerTick > 0.0 && stage < LAST;
        this.perTick = (float) (scaled ? this.basePerTick * Math.pow(this.topPerTick / this.basePerTick, climb)
                : Mth.lerp(climb, this.basePerTick, this.topPerTick));
        this.range = range(this.ability, stage);
        this.push = this.basePush * (1.0 + 0.5 * stage);
        double walk = Mth.lerp(climb, FIRST_WALK, Math.min(FIRST_WALK, this.topWalk));
        AttributeInstance speed = this.owner.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOW);
            speed.addTransientModifier(new AttributeModifier(SLOW, walk - 1.0,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private void unslow() {
        AttributeInstance speed = this.owner.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.removeModifier(SLOW);
        }
    }

    private void stageUp(ServerLevel level) {
        Vec3 eye = this.owner.getEyePosition();
        float rise = (float) this.stage / LAST;
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                0.9F + 0.4F * rise, 1.4F - 0.7F * rise);
        level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS,
                0.35F + 0.65F * rise, 1.8F - 1.0F * rise);
        if (this.stage >= LAST - 1) {
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS,
                    this.stage == LAST ? 1.2F : 0.6F, this.stage == LAST ? 0.9F : 1.6F);
        }
        if (this.stage == LAST) {
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9F,
                    0.55F);
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.0F,
                    0.8F);
        }
        // Out of the beam a little ahead of the ring and flying off sideways, so none of it flies into your own eyes.
        Vec3 ring = eye.add(this.facing.scale(2.5));
        Vec3[] across = Vectors.across(this.facing);
        int spokes = 16 + 8 * this.stage;
        for (int i = 0; i < spokes; i++) {
            double angle = Math.PI * 2.0 * i / spokes;
            Vec3 out = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            ParticleFx.fly(level, ParticleFx.dust(PowerRing.GREEN, 1.0F + 0.15F * this.stage), ring, out,
                    0.35 + 0.2 * this.stage);
        }
        if (this.stage >= 2) {
            // The surge of a new stage runs down the whole beam.
            for (double d = SHED_FROM; d < this.length; d += 2.5 - 0.3 * this.stage) {
                Vec3 at = eye.add(this.facing.scale(d));
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.8F + 0.1F * this.stage), at,
                        this.stage, 0.2 * STAGE_THICK[this.stage], 0.05);
            }
        }
        PowerRing.tell(this.owner, "beam_stage." + (this.stage + 1));
    }

    static boolean start(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        Integer since = CHARGING.remove(owner.getUUID());
        boolean charged = since != null;
        boolean ready = charged && owner.server.getTickCount() - since >= ability.holdTicks() * GATHER_SEEN;
        if (!ready || FIRING.containsKey(owner.getUUID()) || handsFull(owner) || Flight.descending(owner)) {
            if (charged) {
                PowerRing.sync(owner);
            }
            return false;
        }
        if (PowerRing.power(owner) <= 0.0F) {
            PowerRing.tell(owner, "no_power");
            if (charged) {
                PowerRing.sync(owner);
            }
            return false;
        }
        LightBeam beam = new LightBeam(owner, ability);
        FIRING.put(owner.getUUID(), beam);
        Effects.start(level, beam);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.GUARDIAN_ATTACK,
                SoundSource.PLAYERS, 0.8F, 1.9F);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 0.8F, 1.8F);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.WARDEN_SONIC_BOOM,
                SoundSource.PLAYERS, 0.45F, 1.7F);
        level.playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST,
                SoundSource.PLAYERS, 0.8F, 0.6F);
        Vec3 ring = owner.getEyePosition().add(owner.getLookAngle().scale(0.9));
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), ring, 24, 0.35);
        PowerRing.sync(owner);
        return true;
    }

    static void charge(ServerPlayer owner) {
        if (FIRING.containsKey(owner.getUUID()) || CHARGING.containsKey(owner.getUUID())) {
            return;
        }
        CHARGING.put(owner.getUUID(), owner.server.getTickCount());
        PowerRing.sync(owner);
    }

    public static int charging(ServerPlayer player) {
        Integer since = CHARGING.get(player.getUUID());
        if (since == null) {
            return -1;
        }
        int ticks = player.server.getTickCount() - since;
        if (ticks > CHARGE_FORGET) {
            CHARGING.remove(player.getUUID());
            return -1;
        }
        return Math.max(0, ticks);
    }

    static boolean stop(ServerPlayer owner) {
        LightBeam beam = FIRING.remove(owner.getUUID());
        if (beam == null) {
            if (CHARGING.remove(owner.getUUID()) != null) {
                PowerRing.sync(owner);
            }
            return false;
        }
        beam.fade = 0;
        PowerRing.sync(owner);
        return true;
    }

    public static boolean firing(ServerPlayer player) {
        return FIRING.containsKey(player.getUUID());
    }

    // Bolt and beam leave the ring whenever at least one hand is free.
    static boolean handsFull(ServerPlayer player) {
        if (Recharge.busy(player) || Flight.flying(player) && Flight.ticks(player) < Flight.ARISE_TICKS) {
            return true;
        }
        boolean ringHand = GiantFist.holding(player) || GiantHands.waving(player) || AirStrike.calling(player);
        return ringHand && (LightShield.up(player) || LightDome.up(player));
    }

    public static void clear() {
        FIRING.clear();
        CHARGING.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.fade >= 0) {
            this.unslow();
            this.fade++;
            if (this.fade >= FADE_TICKS) {
                ConstructPayload.sendRemove(level, this.id, this.owner.position());
                return false;
            }
            this.send(level);
            return true;
        }
        if (FIRING.get(this.owner.getUUID()) != this) {
            this.fade = 0;
            return true;
        }
        float power = PowerRing.power(this.owner);
        if (!PowerRing.fuels(this.owner, level) || handsFull(this.owner) || Flight.descending(this.owner)
                || power <= 0.0F) {
            if (power <= 0.0F) {
                PowerRing.tell(this.owner, "no_power");
            }
            stop(this.owner);
            this.send(level);
            return true;
        }
        PowerRing.setPower(this.owner, power - this.perTick);
        this.age++;
        int stage = this.stageAt(this.age);
        if (stage != this.stage) {
            this.grow(stage);
            this.stageUp(level);
        }
        this.shine(level);
        this.send(level);
        return true;
    }

    private void shine(ServerLevel level) {
        Vec3 eye = this.owner.getEyePosition();
        this.facing = this.owner.getLookAngle();
        Vec3 far = eye.add(this.facing.scale(this.range));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.owner));
        boolean wall = block.getType() != HitResult.Type.MISS;
        this.end = wall ? block.getLocation() : far;
        this.length = eye.distanceTo(this.end);
        float thick = STAGE_THICK[this.stage];
        double reach = REACH * Math.max(1.0F, thick);
        if (this.age % this.every == 1 || this.every == 1) {
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, this.end)
                    .inflate(reach + 1.0), entity -> PowerRing.canHit(this.owner, entity))) {
                if (target.getBoundingBox().inflate(reach).clip(eye, this.end).isEmpty()) {
                    continue;
                }
                // Vanilla invulnerability after a hit would otherwise swallow the beam's next, rapid tick.
                target.invulnerableTime = 0;
                target.hurt(level.damageSources().playerAttack(this.owner), this.damage);
                double resist = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
                Vec3 shove = new Vec3(this.facing.x, 0.0, this.facing.z).scale(this.push * (1.0 - resist));
                target.setDeltaMovement(target.getDeltaMovement().add(shove.x, 0.04 * this.push, shove.z));
                target.hasImpulse = true;
                // A player moves himself on his own client, so the push has to be told to him, unlike a mob's.
                target.hurtMarked = true;
                Vec3 at = target.getBoundingBox().getCenter();
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F + 0.1F * this.stage), at,
                        5 + 3 * this.stage, 0.25 + 0.1 * this.stage, 0.0);
                ParticleFx.cloud(level, ParticleTypes.CRIT, at, 4 + 2 * this.stage, 0.3, 0.2 + 0.1 * this.stage);
            }
        }
        if (wall && this.age % 2 == 0) {
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 1.6F), this.end, (int) (5 * thick), 0.25 * thick,
                    0.0);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), this.end, (int) (3 * thick),
                    0.15 * thick, 0.0);
            Vec3 back = this.facing.scale(-1.0);
            for (int i = 0; i < 2 + 3 * this.stage; i++) {
                Vec3 way = back.add(ParticleFx.spread(0.9), ParticleFx.spread(0.9) + 0.3,
                        ParticleFx.spread(0.9)).normalize();
                ParticleFx.fly(level, ParticleTypes.END_ROD, this.end.add(back.scale(0.1)), way,
                        0.25 + 0.06 * this.stage);
            }
            if (this.stage >= 2) {
                BlockState hit = level.getBlockState(block.getBlockPos());
                if (!hit.isAir()) {
                    ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, hit), this.end.x, this.end.y,
                            this.end.z, 3 * this.stage, 0.2 * thick, 0.2 * thick, 0.2 * thick, 0.25);
                }
            }
            if (this.stage == LAST && this.age % 8 == 0) {
                ParticleFx.send(level, ParticleTypes.EXPLOSION, this.end.x, this.end.y, this.end.z, 1, 0.4, 0.4, 0.4,
                        0.0);
            }
        }
        if (this.stage >= 2 && this.age % 2 == 1) {
            this.shed(level, eye, thick);
        }
        this.hum(level, eye);
    }

    // The stronger stages shed heavy sparks and flecks of light all along the beam.
    private void shed(ServerLevel level, Vec3 eye, float thick) {
        if (this.length <= SHED_FROM) {
            return;
        }
        Vec3[] across = Vectors.across(this.facing);
        int count = 3 * (this.stage - 1);
        for (int i = 0; i < count; i++) {
            double d = SHED_FROM + ParticleFx.RANDOM.nextDouble() * (this.length - SHED_FROM);
            double angle = ParticleFx.RANDOM.nextDouble() * Math.PI * 2.0;
            Vec3 out = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle)));
            Vec3 at = eye.add(this.facing.scale(d)).add(out.scale(0.25 * thick));
            ParticleFx.fly(level, this.stage == LAST && i % 2 == 0 ? ParticleTypes.ELECTRIC_SPARK
                    : ParticleFx.dust(PowerRing.BRIGHT, 0.6F + 0.15F * this.stage), at, out,
                    0.08 + 0.05 * this.stage);
        }
        if (this.stage >= LAST - 1) {
            for (int i = 0; i < this.stage; i++) {
                double d = SHED_FROM + ParticleFx.RANDOM.nextDouble() * (this.length - SHED_FROM);
                Vec3 at = eye.add(this.facing.scale(d));
                ParticleFx.fly(level, ParticleTypes.END_ROD, at, this.facing.add(ParticleFx.spread(0.6),
                        ParticleFx.spread(0.6), ParticleFx.spread(0.6)).normalize(), 0.15);
            }
        }
    }

    private void hum(ServerLevel level, Vec3 eye) {
        if (this.age % 12 == 1) {
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    0.9F + 0.25F * this.stage, 1.9F - 0.3F * this.stage);
        }
        if (this.stage >= 2 && this.age % 20 == 7) {
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS,
                    0.3F + 0.15F * this.stage, 0.5F + 0.1F * this.stage);
        }
        if (this.stage == LAST && this.age % 16 == 3) {
            // Unstable at full power: it crackles now and then.
            level.playSound(null, eye.x, eye.y, eye.z, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.45F,
                    1.4F + 0.4F * ParticleFx.RANDOM.nextFloat());
        }
    }

    private void send(ServerLevel level) {
        float solid = this.fade < 0 ? 1.0F : 1.0F - (float) this.fade / FADE_TICKS;
        PacketDistributor.sendToPlayersNear(level, null, this.owner.getX(), this.owner.getEyeY(), this.owner.getZ(),
                VIEW_RANGE, new ConstructPayload(this.id, this.owner.getId(), this.end, this.facing,
                        (float) this.length, solid, 0.0F, true, ConstructPayload.BEAM, Math.max(0, this.stage),
                        this.age, null));
    }
}
