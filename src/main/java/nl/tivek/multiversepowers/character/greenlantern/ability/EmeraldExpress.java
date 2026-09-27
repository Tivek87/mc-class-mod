package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.ExpressScript;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.BOOM;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.BRAKE;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.RAIL_LIFT;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.RUN;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.SLIDE;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.STEAM;
import static nl.tivek.multiversepowers.character.greenlantern.ExpressScript.TIP;

public final class EmeraldExpress extends ExpressRoute {
    private static final Map<UUID, EmeraldExpress> ACTIVE = new HashMap<>();
    private static final double VIEW_RANGE = 160.0;
    private static final double BESIDE = 3.4;
    private static final double BEHIND = 1.0;
    private static final double GROUND_NEAR = 5.0;
    private static final double BRAKING = 0.965;
    private static final double TIPPING = 0.975;
    private static final double SLIDE_FRICTION = 0.018;
    private static final SoundEvent CHUFF = Sounds.of("express.chuff");
    private static final SoundEvent HISS = Sounds.of("express.hiss");
    private static final SoundEvent BRAKES = Sounds.of("express.brakes");
    private static final SoundEvent SCRAPE = Sounds.of("express.scrape");
    private static final SoundEvent BLAST = Sounds.of("express.boom");

    private final int id = PowerRing.newId();
    private final int gateId = PowerRing.newId();
    private final Vec3 gate;
    private final Vec3 gateWay;
    private int age;
    private int phase = RUN;
    private int phaseAge;
    private int side = 1;
    private int gateShuts = -1;
    private boolean gateGone;
    private boolean quiet;
    private boolean funnelOut;
    private double chuffDue = 1.0;
    private int chuffs;

    private EmeraldExpress(ServerPlayer owner, CharacterAbility ability, Vec3 rail, Vec3 way) {
        super(owner, ability, rail.subtract(way.scale(0.5)), way);
        this.odometer = -0.5;
        this.gate = rail.add(0.0, ExpressScript.PORTAL_UP * SCALE, 0.0);
        this.gateWay = way;
    }

    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (ACTIVE.containsKey(owner.getUUID())) {
            PowerRing.tell(owner, "express_busy");
            return false;
        }
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        float cost = (float) ability.value("powerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        Vec3 look = owner.getLookAngle();
        Vec3 way = new Vec3(look.x, 0.0, look.z);
        way = way.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0.0F, owner.getYRot()) : way.normalize();
        Vec3 right = new Vec3(-way.z, 0.0, way.x);
        Vec3 spot = owner.position().add(right.scale(BESIDE)).subtract(way.scale(BEHIND));
        double ground = ground(level, spot.x, spot.z, owner.getY());
        double rail = ground >= owner.getY() - GROUND_NEAR ? ground + RAIL_LIFT : owner.getY() - 0.5;
        PowerRing.setPower(owner, power - cost);
        EmeraldExpress train = new EmeraldExpress(owner, ability, new Vec3(spot.x, rail, spot.z), way);
        ACTIVE.put(owner.getUUID(), train);
        Effects.start(level, train);
        PowerRing.tell(owner, "express");
        owner.swing(InteractionHand.MAIN_HAND, true);
        Vec3 eye = owner.getEyePosition();
        Sounds.play(level, eye, SoundEvents.BEACON_POWER_SELECT, 1.2F, 0.7F);
        Sounds.play(level, train.gate, SoundEvents.BEACON_ACTIVATE, 2.0F, 0.6F);
        Sounds.play(level, train.gate, SoundEvents.AMETHYST_BLOCK_RESONATE, 2.0F, 0.5F);
        Sounds.play(level, train.gate, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.6F, 0.6F);
        train.send(level);
        return true;
    }

    public static void clear() {
        ACTIVE.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            this.end(level);
            return false;
        }
        this.age++;
        this.phaseAge++;
        if (!this.quiet && this.phase != BOOM && !PowerRing.fuels(this.owner, level)) {
            this.quiet = true;
            this.phase(BOOM);
            Sounds.play(level, this.middle(), SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
        }
        switch (this.phase) {
            case RUN -> this.run(level);
            case BRAKE -> this.brake(level);
            case TIP -> this.tip(level);
            case SLIDE -> this.slide(level);
            case STEAM -> this.steam(level);
            default -> {
                if (this.phaseAge == ExpressScript.BOOM_TICKS - 5) {
                    this.dissolve(level);
                }
                if (this.phaseAge >= ExpressScript.BOOM_TICKS) {
                    this.end(level);
                    return false;
                }
            }
        }
        this.gate(level);
        this.send(level);
        return true;
    }

    private void phase(int phase) {
        this.phase = phase;
        this.phaseAge = 0;
    }

    private void run(ServerLevel level) {
        int since = this.age - ExpressScript.ROLLS;
        if (since < 0) {
            this.steamOut(level);
            return;
        }
        this.speed = ExpressScript.speedAt(since);
        boolean out = this.odometer > ExpressScript.LENGTH * SCALE + 0.5;
        if (!this.roll(level, out)) {
            this.brakes(level);
            return;
        }
        this.ram(level);
        this.chuff(level, 0.08 * (1.0 - Mth.clamp(since / 30.0, 0.0, 1.0)));
        if (since < 36) {
            this.cocks(level);
        }
        if (!this.funnelOut && this.odometer > ExpressScript.FUNNEL_BACK * SCALE) {
            this.funnelOut = true;
            this.blowOff(level);
        }
        if (out && this.runDone()) {
            this.brakes(level);
        }
    }

    private void brakes(ServerLevel level) {
        this.side = this.turning > 0.004 ? 1 : this.turning < -0.004 ? -1 : (this.id & 1) == 0 ? 1 : -1;
        this.phase(BRAKE);
        Vec3 at = this.middle();
        Sounds.play(level, at, BRAKES, 4.0F, 1.0F);
        Sounds.play(level, at, SoundEvents.GRINDSTONE_USE, 2.0F, 0.6F);
    }

    private void brake(ServerLevel level) {
        this.speed *= BRAKING;
        this.roll(level, false);
        this.ram(level);
        this.sparks(level, 3);
        if (this.phaseAge % 5 == 0) {
            Sounds.play(level, this.middle(), SoundEvents.GRINDSTONE_USE, 1.4F, 0.5F + 0.02F * this.phaseAge);
        }
        if (this.phaseAge >= ExpressScript.BRAKE_TICKS) {
            this.phase(TIP);
            Sounds.play(level, this.middle(), SoundEvents.IRON_GOLEM_DAMAGE, 2.5F, 0.45F);
            Sounds.play(level, this.middle(), SoundEvents.ANVIL_PLACE, 1.5F, 0.5F);
        }
    }

    private void tip(ServerLevel level) {
        this.speed *= TIPPING;
        this.roll(level, false);
        this.ram(level);
        this.sparks(level, 2);
        if (this.phaseAge >= ExpressScript.TIP_TICKS) {
            this.phase(SLIDE);
            this.slam(level);
        }
    }

    private void slam(ServerLevel level) {
        Vec3 at = this.middle();
        Sounds.play(level, at, SoundEvents.ANVIL_LAND, 3.0F, 0.5F);
        Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 3.0F, 0.6F);
        Sounds.play(level, at, SCRAPE, 4.0F, 1.0F);
        Vec3 down = this.lyingSide();
        BlockState ground = level.getBlockState(BlockPos.containing(this.head.x, this.head.y - RAIL_LIFT - 0.5,
                this.head.z));
        for (int i = 0; i <= 8; i++) {
            Vec3 point = this.along(i / 8.0 * ExpressScript.LENGTH * SCALE).add(down);
            if (!ground.isAir()) {
                ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), point, 12, 0.6, 0.2);
            }
            ParticleFx.cloud(level, ParticleTypes.CLOUD, point, 4, 0.5, 0.06);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), point, 6, 0.6, 0.1);
        }
    }

    private void slide(ServerLevel level) {
        this.speed = Math.max(0.0, this.speed - SLIDE_FRICTION);
        if (this.speed > 0.0) {
            this.roll(level, false);
            this.ram(level);
        }
        this.sparks(level, this.speed > 0.0 ? 8 : 0);
        if (this.phaseAge % 9 == 1 && this.speed > 0.05) {
            Sounds.play(level, this.middle(), SoundEvents.GRINDSTONE_USE, 2.0F, 0.4F);
        }
        if (this.speed <= 0.0) {
            this.phase(STEAM);
            Vec3 at = this.boiler();
            Sounds.play(level, at, HISS, 4.0F, 0.8F);
            Sounds.play(level, at, SoundEvents.LAVA_EXTINGUISH, 2.0F, 0.6F);
        }
    }

    private void steam(ServerLevel level) {
        double pressure = ExpressScript.pressure(this.phaseAge);
        Vec3 at = this.boiler();
        double reach = 0.7 + 1.3 * pressure;
        for (int k = -1; k <= 1; k++) {
            Vec3 part = at.add(this.way.scale(k * 2.2 * SCALE));
            ParticleFx.cloud(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, part, 1 + (int) (4.0 * pressure), reach, 0.015);
            ParticleFx.cloud(level, ParticleTypes.CLOUD, part, 3 + (int) (11.0 * pressure), reach,
                    0.05 + 0.12 * pressure);
            ParticleFx.cloud(level, ParticleTypes.WHITE_SMOKE, part, 2 + (int) (7.0 * pressure), reach, 0.05);
        }
        if (this.phaseAge % 4 == 0) {
            ParticleFx.cloud(level, ParticleTypes.POOF, at.add(this.way.scale(ParticleFx.spread(2.5) * SCALE)),
                    6 + (int) (10.0 * pressure), 0.4, 0.12);
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 1.3F), at, 3 + (int) (7.0 * pressure), reach * 1.5,
                0.03);
        int every = Math.max(2, 8 - (int) Math.round(6.0 * pressure));
        if (this.phaseAge % every == 0) {
            Sounds.play(level, at, SoundEvents.FIRE_EXTINGUISH, 1.5F + (float) pressure, 0.6F + 0.4F * (float) pressure);
        }
        if (this.phaseAge == 14 || this.phaseAge == 26) {
            Sounds.play(level, at, HISS, 4.0F, 0.9F + 0.15F * (float) pressure);
            Sounds.play(level, at, SoundEvents.IRON_GOLEM_DAMAGE, 2.0F, 0.4F);
        }
        if (this.phaseAge >= ExpressScript.STEAM_TICKS) {
            this.phase(BOOM);
            this.blast(level);
        }
    }

    private void blast(ServerLevel level) {
        Vec3 at = this.boiler();
        double radius = this.ability.value("blastRadius");
        double damage = this.ability.value("blastDamage");
        double push = this.ability.value("knockback");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                entity -> GiantHands.fair(this.owner, entity))) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(at);
            double far = to.length();
            if (far > radius) {
                continue;
            }
            double near = 1.0 - far / radius;
            living.invulnerableTime = 0;
            living.hurt(level.damageSources().playerAttack(this.owner), (float) (damage * (0.5 + 0.5 * near)));
            double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            Vec3 out = far < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / far, 0.0, to.z / far);
            double strength = push * (0.6 + 0.8 * near);
            living.setDeltaMovement(new Vec3(out.x * strength, 0.6 + 0.4 * near * push, out.z * strength)
                    .scale(1.0 - resist));
            living.hasImpulse = true;
            living.hurtMarked = true;
        }
        Sounds.play(level, at, BLAST, 8.0F, 1.0F);
        Sounds.play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 6.0F, 0.7F);
        Sounds.play(level, at, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 4.0F, 0.6F);
        Sounds.play(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 4.0F, 0.5F);
        ParticleFx.at(level, ParticleTypes.EXPLOSION_EMITTER, at);
        ParticleFx.at(level, ParticleTypes.FLASH, at);
        ParticleFx.cloud(level, ParticleTypes.EXPLOSION, at, 8, 2.5, 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 2.2F), at, 90, 0.7);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 2.6F), at, 60, 0.45);
        ParticleFx.cloud(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, at, 14, 1.8, 0.05);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, at, 50, 1.5, 0.45);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 2.0F), at.subtract(0.0, 1.2, 0.0), 48, 0.9);
    }

    private void dissolve(ServerLevel level) {
        for (int i = 0; i <= 6; i++) {
            Vec3 point = this.along(i / 6.0 * ExpressScript.LENGTH * SCALE).add(this.quiet ? Vec3.ZERO
                    : this.lyingSide().scale(0.5)).add(0.0, 1.8, 0.0);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 1.8F), point, 24, 2.2, 0.05);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), point, 14, 2.0, 0.08);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, point, 4, 1.6, 0.06);
        }
        Sounds.play(level, this.middle(), SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.9F);
        Sounds.play(level, this.middle(), SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.9F);
    }

    private void chuff(ServerLevel level, double atLeast) {
        this.chuffDue += this.speed / ExpressScript.CHUFF_BLOCKS + atLeast;
        if (this.chuffDue < 1.0) {
            return;
        }
        this.chuffDue -= 1.0;
        this.chuffs++;
        Vec3 funnel = this.funnel();
        double pace = Math.min(1.0, this.speed / ExpressScript.TOP_SPEED);
        float pitch = (float) (0.82 + 0.28 * pace + ParticleFx.spread(0.04));
        Sounds.play(level, funnel, CHUFF, this.chuffs % 2 == 1 ? 3.2F : 2.5F, pitch);
        if (this.odometer < ExpressScript.FUNNEL_BACK * SCALE) {
            return;
        }
        ParticleFx.cloud(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, funnel, 2, 0.25, 0.03);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, funnel, 5, 0.25, 0.1 + 0.1 * pace);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 1.4F), funnel, 4, 0.3, 0.05);
    }

    private void steamOut(ServerLevel level) {
        if (this.age == 2) {
            Sounds.play(level, this.gate, HISS, 4.0F, 1.0F);
            Sounds.play(level, this.gate, SoundEvents.FIRE_EXTINGUISH, 2.0F, 0.5F);
        }
        Vec3 mouth = this.gate.add(0.0, (ExpressScript.FUNNEL_UP - ExpressScript.PORTAL_UP) * SCALE * 0.8, 0.0)
                .add(this.way.scale(0.3));
        Vec3 blow = this.way.scale(0.35).add(0.0, 0.18, 0.0);
        for (int i = 0; i < 3; i++) {
            ParticleFx.send(level, ParticleTypes.CLOUD, mouth.x + ParticleFx.spread(0.6), mouth.y + ParticleFx.spread(0.4),
                    mouth.z + ParticleFx.spread(0.6), 0, blow.x, blow.y, blow.z, 1.0);
        }
        ParticleFx.cloud(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth, 1, 0.6, 0.02);
    }

    private void blowOff(ServerLevel level) {
        Vec3 funnel = this.funnel();
        Sounds.play(level, funnel, HISS, 4.0F, 0.9F);
        Sounds.play(level, funnel, SoundEvents.FIRE_EXTINGUISH, 2.5F, 0.6F);
        ParticleFx.cloud(level, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, funnel, 6, 0.3, 0.02);
        for (int i = 0; i < 24; i++) {
            ParticleFx.send(level, ParticleTypes.CLOUD, funnel.x + ParticleFx.spread(0.3), funnel.y,
                    funnel.z + ParticleFx.spread(0.3), 0, ParticleFx.spread(0.25), 1.0, ParticleFx.spread(0.25),
                    0.35 + 0.2 * ParticleFx.RANDOM.nextDouble());
        }
        ParticleFx.cloud(level, ParticleTypes.POOF, funnel, 10, 0.4, 0.08);
    }

    private void cocks(ServerLevel level) {
        Vec3 right = this.right();
        Vec3 at = this.along(ExpressScript.BOGIE * SCALE).add(0.0, 1.2 * SCALE, 0.0);
        if (this.odometer < ExpressScript.BOGIE * SCALE) {
            return;
        }
        for (int flank = -1; flank <= 1; flank += 2) {
            Vec3 out = right.scale(flank);
            Vec3 cock = at.add(out.scale(1.2 * SCALE));
            ParticleFx.send(level, ParticleTypes.CLOUD, cock.x, cock.y - 0.3, cock.z, 0, out.x + this.way.x * 0.4,
                    -0.05, out.z + this.way.z * 0.4, 0.3);
            ParticleFx.send(level, ParticleTypes.WHITE_SMOKE, cock.x, cock.y - 0.3, cock.z, 0, out.x, 0.0, out.z,
                    0.12);
        }
    }

    private void sparks(ServerLevel level, int count) {
        if (count <= 0) {
            return;
        }
        Vec3 right = this.right();
        boolean lying = this.phase == SLIDE;
        for (int i = 0; i < 4; i++) {
            double back = (lying ? 1.5 + i * 3.5 : 5.2 + i * 0.9) * SCALE;
            Vec3 point = this.along(back).subtract(0.0, RAIL_LIFT, 0.0);
            point = lying ? point.add(this.lyingSide()) : point.add(right.scale((i % 2 == 0 ? 0.72 : -0.72) * SCALE));
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, point, count, 0.25, 0.25);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.7F), point, count / 2 + 1, 0.3, 0.1);
            if (lying) {
                ParticleFx.cloud(level, ParticleTypes.FIREWORK, point, count / 2, 0.4, 0.15);
            }
        }
    }

    private Vec3 along(double back) {
        return this.head.subtract(this.way.scale(back));
    }

    private Vec3 lyingSide() {
        return this.right().scale(this.side * ExpressScript.PIVOT_OUT * SCALE);
    }

    private Vec3 middle() {
        return this.along(ExpressScript.BOILER_BACK * SCALE).add(0.0, 1.5 * SCALE, 0.0);
    }

    private Vec3 funnel() {
        return this.along(ExpressScript.FUNNEL_BACK * SCALE).add(0.0, ExpressScript.FUNNEL_UP * SCALE, 0.0);
    }

    private Vec3 boiler() {
        Vec3 rail = this.along(ExpressScript.BOILER_BACK * SCALE);
        if (this.phase < SLIDE || this.quiet) {
            return rail.add(0.0, ExpressScript.BOILER_UP * SCALE, 0.0);
        }
        return rail.add(this.right().scale(this.side * (ExpressScript.PIVOT_OUT + ExpressScript.BOILER_UP) * SCALE))
                .add(0.0, ExpressScript.PIVOT_OUT * SCALE - RAIL_LIFT, 0.0);
    }

    private void gate(ServerLevel level) {
        if (this.gateGone) {
            return;
        }
        if (this.gateShuts < 0 && (this.odometer > ExpressScript.LENGTH * SCALE + 1.0 || this.phase != RUN)) {
            this.gateShuts = this.age;
            Sounds.play(level, this.gate, SoundEvents.BEACON_DEACTIVATE, 1.6F, 0.8F);
        }
        if (this.gateShuts >= 0 && this.age >= this.gateShuts + ExpressScript.OPEN_TICKS) {
            this.gateGone = true;
            ConstructPayload.sendRemove(level, this.gateId, this.gate);
            return;
        }
        float open = (float) ExpressScript.portalOpen(this.age, this.gateShuts);
        PacketDistributor.sendToPlayersNear(level, null, this.gate.x, this.gate.y, this.gate.z, VIEW_RANGE,
                new ConstructPayload(this.gateId, this.owner.getId(), this.gate, this.gateWay,
                        (float) (ExpressScript.PORTAL_RADIUS * SCALE), open, (float) this.gateShuts, false,
                        ConstructPayload.EXPRESS_PORTAL, 0, this.age, null));
    }

    private void send(ServerLevel level) {
        int variant = ExpressScript.variant(this.phase, this.side, this.rams) | (this.quiet ? ExpressScript.QUIET : 0);
        PacketDistributor.sendToPlayersNear(level, null, this.head.x, this.head.y, this.head.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.head, this.way, (float) SCALE, 1.0F,
                        (float) this.odometer, false, ConstructPayload.EXPRESS, variant, this.age, null));
    }

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        ConstructPayload.sendRemove(level, this.id, this.head);
        if (!this.gateGone) {
            this.gateGone = true;
            ConstructPayload.sendRemove(level, this.gateId, this.gate);
        }
    }
}
