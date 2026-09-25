package nl.tivek.multiversepowers.character.docock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.faction.Factions;

abstract class RigGround extends RigGrab {
    RigGround(ServerPlayer caster, ServerLevel home) {
        super(caster, home);
    }

    boolean groundStrike(ServerLevel level) {
        if (this.searchedJustNow("ground_strike")) {
            return false;
        }
        double range = ability("ground_strike").value("rangeBlocks");
        int room = this.freeArms();
        LivingEntity aimed = Targeting.aimLiving(this.caster, level, range);
        if (aimed != null && !this.marks.contains(aimed) && this.marks.size() < room) {
            this.marks.add(aimed);
            glow(aimed);
            Vec3 at = aimed.getBoundingBox().getCenter();
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 12, 0.35, 0.1);
            this.sound(at, SoundEvents.NOTE_BLOCK_BELL.value(), 0.6F, 1.9F);
            this.caster.displayClientMessage(Component.translatable(
                    "octopus." + MultiversePowers.MODID + ".strike.marked", this.marks.size(), room), true);
            return false;
        }
        if (this.marks.isEmpty()) {
            this.caster.displayClientMessage(Component.translatable("octopus." + MultiversePowers.MODID
                    + (room == 0 ? ".busy" : ".strike.none")), true);
            return this.foundNothing("ground_strike");
        }
        return this.launchStrike(level);
    }

    boolean clearMarks() {
        if (this.marks.isEmpty()) {
            return false;
        }
        this.dropMarks();
        this.sound(this.caster.position(), SoundEvents.NOTE_BLOCK_BASS.value(), 0.6F, 0.8F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + MultiversePowers.MODID + ".strike.cleared"), true);
        return true;
    }

    private boolean launchStrike(ServerLevel level) {
        int launched = 0;
        for (LivingEntity target : this.marks) {
            Arm arm = this.freeArm();
            if (arm == null) {
                break;
            }
            this.startDig(level, arm, target);
            launched++;
        }
        this.dropMarks();
        if (launched == 0) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + MultiversePowers.MODID + ".busy"), true);
            return false;
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 0.6F);
        this.sound(this.caster.position(), SoundEvents.IRON_GOLEM_ATTACK, 0.9F, 1.4F);
        this.caster.displayClientMessage(Component.translatable(
                "octopus." + MultiversePowers.MODID + ".strike.launched", launched), true);
        return true;
    }

    private void startDig(ServerLevel level, Arm arm, LivingEntity target) {
        arm.job = Job.BURROW;
        arm.age = 0;
        arm.dig = Dig.SHOW;
        arm.target = target;
        arm.risen = 0;
        arm.digHit = false;
        arm.tipOffset = 0;
        arm.spike = 0;
        arm.thrust = 0;
        Vec3 spot = this.caster.position().add(this.right().scale(arm.side * 0.9))
                .add(this.forward().scale(arm.upper ? 1.3 : 0.6));
        double ground = Targeting.floorBelow(level,
                BlockPos.containing(spot.x, this.caster.getY() + 1.0, spot.z));
        arm.spot = new Vec3(spot.x, ground + 0.05, spot.z);
        arm.digTravel = TRAVEL_MIN + (int) (this.caster.distanceTo(target) * TRAVEL_PER_BLOCK);
    }

    void tickDig(ServerLevel level, Arm arm) {
        LivingEntity target = arm.target;
        boolean gone = target == null || !target.isAlive() || target.isRemoved() || target.level() != level;
        if (gone && arm.dig == Dig.SHOW) {
            this.toRest(arm);
            return;
        }
        if (gone && arm.dig.ordinal() < Dig.DOWN.ordinal()) {
            arm.dig = arm.risen > 0 ? Dig.DOWN : Dig.BACK;
            arm.age = 0;
        }
        switch (arm.dig) {
            case SHOW -> {
                Vec3 show = this.caster.getEyePosition().add(this.forward().scale(2.0))
                        .add(this.right().scale(arm.side * (arm.upper ? 0.5 : 0.95)))
                        .add(0, arm.upper ? 0.3 : -0.35, 0);
                arm.claw = 0.6;
                arm.spike = Math.min(1.0, arm.spike + 1.0 / SHOW_TIME);
                arm.aim = this.caster.getEyePosition().subtract(arm.tip);
                this.glide(arm, show, 0.55, 0.9);
                if (arm.age == 1) {
                    this.sound(arm.tip, SoundEvents.NETHERITE_BLOCK_PLACE, 0.8F, 1.5F);
                }
                if (arm.age % 3 == 0) {
                    ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, arm.tip, 3, 0.15, 0.02);
                }
                if (arm.age >= SHOW_TIME) {
                    arm.dig = Dig.TO_HOLE;
                    arm.age = 0;
                }
            }
            case TO_HOLE -> {
                arm.claw = 0.05;
                arm.spike = Math.min(1.0, arm.spike + 1.0 / DIVE_TIME);
                this.glide(arm, arm.spot, 0.55, 1.0);
                if (arm.tip.distanceTo(arm.spot) < 0.5 || arm.age > DIVE_TIME * 4) {
                    arm.dig = Dig.SINK;
                    arm.age = 0;
                    arm.clipPoint = arm.spot;
                    arm.clipNormal = new Vec3(0, -1, 0);
                    this.groundBurst(level, arm.spot, 18);
                    this.sound(arm.spot, SoundEvents.NETHERITE_BLOCK_BREAK, 1.0F, 0.6F);
                }
            }
            case SINK -> {
                arm.tip = arm.tip.add(0, -SINK_SPEED, 0);
                arm.tipOffset += SINK_SPEED;
                if (arm.tipOffset >= SINK_DEPTH) {
                    arm.dig = Dig.UNDER;
                    arm.age = 0;
                }
            }
            case UNDER -> {
                arm.tip = arm.spot.add(0, -SINK_DEPTH, 0);
                arm.tipOffset = SINK_DEPTH;
                if (!gone && this.age % 2 == 0) {
                    this.rumble(level, arm, target);
                }
                if (arm.age >= arm.digTravel && !gone) {
                    Vec3 at = target.position();
                    arm.digAt = at;
                    arm.digGround = Targeting.floorBelow(level,
                            BlockPos.containing(at.x, at.y + 1.0, at.z));
                    arm.dig = Dig.UP;
                    arm.age = 0;
                    this.groundBurst(level, new Vec3(at.x, arm.digGround, at.z), 30);
                    this.sound(at, SoundEvents.NETHERITE_BLOCK_BREAK, 1.2F, 0.5F);
                }
            }
            case UP -> {
                double top = gone ? RISE_ABOVE
                        : target.getBoundingBox().getCenter().y - arm.digGround + RISE_ABOVE;
                top = Math.max(1.4, top);
                arm.risen += Math.min(RISE_SPEED, Math.max(0.35, (top - arm.risen) * 0.55));
                if (!arm.digHit && !gone && arm.risen >= top - RISE_ABOVE) {
                    arm.digHit = true;
                    this.spikeHit(level, arm, target);
                }
                if (arm.risen >= top) {
                    arm.risen = top;
                    arm.dig = Dig.DOWN;
                    arm.age = 0;
                }
            }
            case DOWN -> {
                if (arm.age >= SPIKE_HOLD) {
                    arm.risen -= Math.max(SPIKE_SINK * 0.3, arm.risen * 0.35);
                    if (arm.risen <= 0) {
                        arm.risen = 0;
                        arm.dig = Dig.BACK;
                        arm.age = 0;
                    }
                }
            }
            case BACK -> {
                arm.tipOffset -= SINK_SPEED;
                arm.spike = Math.max(0.0, arm.spike - 0.12);
                arm.tip = arm.spot.add(0, -Math.max(0.0, arm.tipOffset), 0);
                if (arm.tipOffset <= 0) {
                    this.toRest(arm);
                }
            }
        }
        this.drawSpike(level, arm);
    }

    private void rumble(ServerLevel level, Arm arm, LivingEntity target) {
        double t = Math.min(1.0, (arm.age + 1.0) / Math.max(1, arm.digTravel));
        Vec3 at = arm.spot.lerp(target.position(), t);
        BlockPos above = BlockPos.containing(at.x, at.y + 1.0, at.z);
        // Skip when the chunk isn't loaded: never force one to load just for dust.
        if (!level.isLoaded(above)) {
            return;
        }
        double ground = Targeting.floorBelow(level, above);
        this.groundBurst(level, new Vec3(at.x, ground, at.z), 4);
    }

    private void groundBurst(ServerLevel level, Vec3 at, int count) {
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.1, at.z,
                    count, 0.35, 0.15, 0.35, 0.25);
        }
        ParticleFx.cloud(level, ParticleTypes.POOF, at.add(0, 0.15, 0), Math.max(2, count / 4), 0.3, 0.05);
    }

    private void spikeHit(ServerLevel level, Arm arm, LivingEntity target) {
        if (this.hurt(target, damageOf("ground_strike"), 0.0)) {
            double up = ability("ground_strike").value("knockUp");
            target.setDeltaMovement(target.getDeltaMovement().x, up, target.getDeltaMovement().z);
            target.hurtMarked = true;
            target.resetFallDistance();
        }
        Vec3 at = target.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 18, 0.4, 0.4);
        ParticleFx.shockwave(level, ParticleTypes.LARGE_SMOKE, new Vec3(at.x, arm.digGround + 0.15, at.z), 24, 0.35);
        this.sound(at, SoundEvents.ANVIL_LAND, 0.9F, 1.3F);
        this.sound(at, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
    }

    private void drawSpike(ServerLevel level, Arm arm) {
        if (arm.risen <= 0.01) {
            if (arm.farId >= 0) {
                RobotArm.remove(level, arm.farId);
                arm.farId = -1;
            }
            return;
        }
        if (arm.farId < 0) {
            arm.farId = RobotArm.newId();
        }
        Vec3 bottom = new Vec3(arm.digAt.x, arm.digGround - SPIKE_BURIED, arm.digAt.z);
        Vec3 top = bottom.add(0, SPIKE_BURIED + arm.risen, 0);
        RobotArm.arm(arm.farId, List.of(bottom, top)).claw(0.3).tools(1.0, 0.0)
                .clip(new Vec3(arm.digAt.x, arm.digGround, arm.digAt.z), new Vec3(0, -1, 0)).send(level);
    }

    void dropMarks() {
        this.marks.clear();
    }

    void tickMarks(ServerLevel level) {
        if (this.marks.isEmpty()) {
            return;
        }
        double range = ability("ground_strike").value("rangeBlocks") + 4.0;
        this.marks.removeIf(target -> !target.isAlive() || target.isRemoved() || target.level() != level
                || target.distanceTo(this.caster) > range);
        for (LivingEntity target : this.marks) {
            glow(target);
        }
        if (this.age % 4 != 0) {
            return;
        }
        for (LivingEntity target : this.marks) {
            Vec3 at = target.getBoundingBox().getCenter();
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 2, target.getBbWidth() * 0.5 + 0.2, 0.01);
        }
    }

    private static void glow(LivingEntity target) {
        MobEffectInstance glow = target.getEffect(MobEffects.GLOWING);
        if (glow == null || glow.endsWithin(MARK_GLOW / 2)) {
            target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MARK_GLOW, 0, true, false, false));
        }
    }

    boolean heavy(ServerLevel level, boolean areaOnly) {
        if (this.isHolding() && !areaOnly) {
            this.heldSlam = HELD_SLAM_TIME;
            for (Arm arm : this.arms) {
                if (arm.held != null) {
                    arm.crashPause = 0;
                }
            }
            ParticleFx.cloud(level, ParticleTypes.CRIT, this.caster.position().add(0, 1.0, 0), 14, 0.5, 0.3);
            this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.2F, 0.5F);
            this.sound(this.caster.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
            return true;
        }
        return this.groundSlam(level);
    }

    private boolean groundSlam(ServerLevel level) {
        if (this.slam != Slam.NONE) {
            return false;
        }
        this.airSlam = !this.caster.onGround();
        this.slam = this.airSlam ? Slam.DROP : Slam.RISE;
        this.slamAge = 0;
        if (this.airSlam) {
            Vec3 motion = this.caster.getDeltaMovement();
            this.caster.setDeltaMovement(motion.x * 0.3, -AIR_DROP, motion.z * 0.3);
            this.caster.hurtMarked = true;
            OctopusArms.safeFall(this.caster, MAX_DROP + 20);
        }
        for (Arm arm : this.arms) {
            if (arm.free()) {
                arm.job = Job.RISE;
                arm.age = 0;
            }
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.0F, 0.5F);
        this.sound(this.caster.position(), SoundEvents.WARDEN_ATTACK_IMPACT, 0.6F, 1.4F);
        return true;
    }

    void tickSlam(ServerLevel level) {
        if (this.slam == Slam.NONE) {
            return;
        }
        this.slamAge++;
        switch (this.slam) {
            case RISE -> {
                if (this.slamAge >= RISE_TIME) {
                    this.smashDown(level);
                }
            }
            case DROP -> {
                if (this.caster.onGround() || this.slamAge > MAX_DROP) {
                    this.smashDown(level);
                }
            }
            case SMASH -> {
                if (this.slamAge >= 3) {
                    this.impact(level);
                    this.slam = Slam.NONE;
                }
            }
            default -> {
            }
        }
    }

    private void smashDown(ServerLevel level) {
        this.slam = Slam.SMASH;
        this.slamAge = 0;
        double radius = this.airSlam ? 2.8 : 2.3;
        for (Arm arm : this.arms) {
            if (arm.job != Job.RISE && !arm.free()) {
                continue;
            }
            double angle = Math.toRadians(this.caster.getYRot()) + Math.PI / 4 + arm.index * Math.PI / 2;
            Vec3 spot = this.caster.position().add(-Math.sin(angle) * radius, 0, Math.cos(angle) * radius);
            double ground = Targeting.floorBelow(level,
                    BlockPos.containing(spot.x, this.caster.getY() + 1.0, spot.z));
            arm.job = Job.SMASH;
            arm.age = 0;
            arm.spot = new Vec3(spot.x, Math.max(ground, this.caster.getY() - 3.0), spot.z);
        }
        this.sound(this.caster.position(), SoundEvents.PISTON_EXTEND, 1.2F, 0.4F);
    }

    private void impact(ServerLevel level) {
        double radius = this.airSlam ? AIR_SLAM_RADIUS : SLAM_RADIUS;
        float damage = this.airSlam ? (float) ability("ground_slam").value("airDamage")
                : damageOf("ground_slam");
        Vec3 center = this.caster.position();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                this.caster.getBoundingBox().inflate(radius, 3.0, radius),
                entity -> !this.holds(entity) && Targeting.isTargetable(this.caster, entity)
                        && Factions.hostile(this.caster, entity))) {
            Vec3 away = living.position().subtract(center);
            double distance = Math.sqrt(away.horizontalDistanceSqr());
            double strength = 1.0 - distance / radius;
            if (strength <= 0.0 || !this.hurt(living, (float) (damage * (0.5 + 0.5 * strength)), 0.0)) {
                continue;
            }
            Vec3 push = distance < 1.0E-3 ? Vec3.ZERO : new Vec3(away.x / distance, 0, away.z / distance);
            living.setDeltaMovement(living.getDeltaMovement().add(push.scale(1.3 * strength))
                    .add(0, 0.45 + 0.35 * strength, 0));
            living.hasImpulse = true;
            living.hurtMarked = true;
        }
        double groundY = Targeting.floorBelow(level, BlockPos.containing(center.x, center.y + 0.5, center.z));
        BlockState ground = level.getBlockState(BlockPos.containing(center.x, groundY - 0.5, center.z));
        if (!ground.isAir()) {
            for (double ring = 1.0; ring <= radius; ring += 1.5) {
                for (int i = 0; i < 16; i++) {
                    double angle = Math.PI * 2 * i / 16;
                    ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground),
                            center.x + Math.cos(angle) * ring, groundY + 0.1, center.z + Math.sin(angle) * ring,
                            4, 0.2, 0.1, 0.2, 0.25);
                }
            }
        }
        Vec3 at = new Vec3(center.x, groundY, center.z);
        ParticleFx.cloud(level, ParticleTypes.EXPLOSION, at.add(0, 0.4, 0), this.airSlam ? 5 : 3, 1.0, 0.0);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, at.add(0, 0.2, 0), 40, 0.6);
        ParticleFx.shockwave(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.3, 0), 30, 0.8);
        this.sound(at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.4F, 0.8F);
        this.sound(at, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.9F);
        this.sound(at, SoundEvents.ANVIL_LAND, 0.7F, 0.6F);
    }
}
