package nl.tivek.multiversepowers.character.docock;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;

abstract class RigGrab extends RigStrikes {
    RigGrab(ServerPlayer caster, ServerLevel home) {
        super(caster, home);
    }

    boolean grab(ServerLevel level) {
        if (this.searchedJustNow("grab")) {
            return false;
        }
        double range = ability("grab").value("rangeBlocks");
        Arm arm = this.freeArm();
        if (arm == null) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + MultiversePowers.MODID + ".busy"), true);
            return false;
        }
        LivingEntity wanted = Targeting.aimLiving(this.caster, level, range);
        if (wanted == null || this.targeted(wanted)) {
            wanted = null;
            for (LivingEntity other : this.threats(level, Math.min(range, 12.0))) {
                if (!this.targeted(other)) {
                    wanted = other;
                    break;
                }
            }
        }
        if (wanted == null) {
            Targeting.noTarget(this.caster);
            return this.foundNothing("grab");
        }
        this.reach(arm, wanted);
        this.caster.displayClientMessage(Component.translatable("octopus." + MultiversePowers.MODID + ".grabbing",
                this.busyArms()), true);
        this.sound(arm.tip, SoundEvents.PISTON_EXTEND, 1.0F, 0.8F);
        this.sound(arm.tip, SoundEvents.CHAIN_PLACE, 1.0F, 1.2F);
        return true;
    }

    private int busyArms() {
        int busy = 0;
        for (Arm arm : this.arms) {
            if (arm.held != null || (arm.job == Job.REACH && arm.target != null)) {
                busy++;
            }
        }
        return busy;
    }

    int freeArms() {
        int free = 0;
        for (Arm arm : this.arms) {
            if (arm.free()) {
                free++;
            }
        }
        return free + Math.max(0, this.legCount() - 2);
    }

    private void reach(Arm arm, LivingEntity target) {
        arm.job = Job.REACH;
        arm.age = 0;
        arm.target = target;
        arm.struckAt = -1;
    }

    private boolean targeted(LivingEntity entity) {
        for (Arm arm : this.arms) {
            if (arm.held == entity || (arm.job == Job.REACH && arm.target == entity)) {
                return true;
            }
        }
        return false;
    }

    boolean letGoAll() {
        if (!this.isHolding()) {
            return false;
        }
        this.letGo(false);
        this.sound(this.caster.position(), SoundEvents.PISTON_CONTRACT, 0.9F, 1.2F);
        this.caster.displayClientMessage(
                Component.translatable("octopus." + MultiversePowers.MODID + ".let_go"), true);
        return true;
    }

    @Nullable
    Arm freeArm() {
        for (int i : new int[] { 0, 1, 2, 3 }) {
            if (this.arms[i].free()) {
                return this.arms[i];
            }
        }
        if (this.legCount() > 2) {
            for (int i = this.arms.length - 1; i >= 0; i--) {
                Arm arm = this.arms[i];
                if (arm.leg && arm.job == Job.REST) {
                    arm.leg = false;
                    arm.foot = null;
                    arm.step = -1;
                    return arm;
                }
            }
        }
        return null;
    }

    void seize(ServerLevel level, Arm arm, LivingEntity target) {
        if (!mayHold(this.caster, target, level) || HeldMobs.isHeldByAnyone(target)
                || (target instanceof Mob mob && !HeldMobs.hold(mob))) {
            this.toRest(arm);
            return;
        }
        arm.held = target;
        arm.holdTicks = 0;
        arm.holdDistance = Mth.clamp(this.caster.getEyePosition().distanceTo(target.getBoundingBox().getCenter()),
                HOLD_DISTANCE, 10.0);
        arm.job = Job.HOLD;
        arm.age = 0;
        arm.target = null;
        PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(true, this.hasLoad()));
        this.caster.displayClientMessage(Component.translatable("octopus." + MultiversePowers.MODID + ".grab.hint"),
                true);
        Vec3 at = target.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.CRIT, at, 14, 0.3, 0.3);
        this.sound(at, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.8F);
        this.sound(at, SoundEvents.CHAIN_HIT, 1.0F, 0.8F);
    }

    void requestThrow() {
        this.throwRequested = true;
    }

    void tickHold(ServerLevel level) {
        boolean before = this.hasThrowable();
        if (this.throwRequested) {
            this.throwLoads(level);
        }
        for (Arm arm : this.arms) {
            LivingEntity target = arm.held;
            if (target == null) {
                continue;
            }
            arm.holdTicks++;
            if (!mayHold(this.caster, target, level)) {
                OctopusArms.setDown(target);
                this.letGo(arm);
                continue;
            }
            if (this.throwRequested) {
                this.fling(level, arm, target);
                continue;
            }
            if (this.heldSlam > 0) {
                this.slamHeld(level, arm, target);
                continue;
            }
            arm.holdDistance += (HOLD_DISTANCE - arm.holdDistance) * 0.08;
            Vec3 goal = this.caster.getEyePosition().add(this.caster.getLookAngle().scale(arm.holdDistance))
                    .add(this.right().scale(arm.side * 0.85)).add(0, arm.upper ? 0.35 : -0.35, 0);
            Vec3 center = target.getBoundingBox().getCenter();
            Vec3 wanted = goal.subtract(center).scale(FOLLOW);
            if (wanted.length() > MAX_SPEED) {
                wanted = wanted.normalize().scale(MAX_SPEED);
            }
            double smashSpeed = ability("grab").value("smashSpeed");
            if (arm.crashPause > 0) {
                arm.crashPause--;
            }
            Vec3 blocked = this.drag(target, wanted);
            if (arm.crashPause <= 0 && wanted.length() > smashSpeed && blocked.length() > smashSpeed * 0.6) {
                crash(level, this.caster, target, blocked.normalize().scale(wanted.length()));
                arm.crashPause = CRASH_PAUSE;
            }
            target.resetFallDistance();
            if (arm.holdTicks % 12 == 6) {
                this.sound(center, SoundEvents.CHAIN_STEP, 0.6F, 0.8F);
            }
        }
        this.throwRequested = false;
        if (before && !this.hasThrowable() && !this.caster.hasDisconnected()) {
            PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(false, this.hasLoad()));
        }
    }

    private void slamHeld(ServerLevel level, Arm arm, LivingEntity target) {
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 step = new Vec3(0, -1.4, 0);
        Vec3 blocked = this.drag(target, step);
        if (blocked.length() > step.length() * 0.35 && arm.crashPause <= 0) {
            this.hurt(target, (float) ability("ground_slam").value("heldSlamDamage"), 0.0);
            smashFx(level, target, step);
            arm.crashPause = CRASH_PAUSE;
            this.heldSlam = Math.min(this.heldSlam, 1);
        }
        arm.tip = center.add(0, target.getBbHeight() * 0.5 + 0.2, 0);
        arm.claw = target.getBbWidth() * 0.5 + 0.25;
    }

    void tickHeldSlam() {
        if (this.heldSlam > 0) {
            this.heldSlam--;
            if (this.heldSlam == 0) {
                this.sound(this.caster.position(), SoundEvents.PISTON_CONTRACT, 0.8F, 1.1F);
            }
        }
    }

    private Vec3 drag(LivingEntity target, Vec3 wanted) {
        Vec3 was = target.position();
        target.move(MoverType.SELF, wanted);
        target.setDeltaMovement(Vec3.ZERO);
        target.hurtMarked = true;
        if (target instanceof ServerPlayer player) {
            holdAt(player, target.getX(), target.getY(), target.getZ());
        }
        return wanted.subtract(target.position().subtract(was));
    }

    static void holdAt(ServerPlayer player, double x, double y, double z) {
        player.connection.teleport(x, y, z, player.getYRot(), player.getXRot(), RelativeMovement.ROTATION);
        player.connection.aboveGroundTickCount = 0;
    }

    static boolean mayHold(ServerPlayer caster, LivingEntity target, ServerLevel level) {
        if (!target.isAlive() || target.isRemoved() || target.level() != level || target.isSpectator()) {
            return false;
        }
        return !(target instanceof Player other)
                || (caster.server.isPvpAllowed() && !other.isCreative() && caster.canHarmPlayer(other));
    }

    void letGo(boolean setDown) {
        for (Arm arm : this.arms) {
            if (setDown && arm.held != null) {
                OctopusArms.setDown(arm.held);
            }
            this.letGo(arm);
        }
    }

    void letGo(Arm arm) {
        LivingEntity target = arm.held;
        if (target == null) {
            return;
        }
        arm.held = null;
        if (target instanceof Mob mob) {
            HeldMobs.release(mob);
        }
        this.toRest(arm);
        if (!this.caster.hasDisconnected() && !this.hasThrowable()) {
            PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(false, this.hasLoad()));
        }
    }

    private void fling(ServerLevel level, Arm arm, LivingEntity target) {
        this.letGo(arm);
        target.setDeltaMovement(this.caster.getLookAngle().scale(THROW_SPEED).add(0, 0.25, 0));
        target.hasImpulse = true;
        target.hurtMarked = true;
        Vec3 center = target.getBoundingBox().getCenter();
        ParticleFx.cloud(level, ParticleTypes.CLOUD, center, 12, 0.3, 0.15);
        this.sound(center, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
        this.sound(center, SoundEvents.PISTON_EXTEND, 1.0F, 1.4F);
        Effects.start(level, thrown(this.caster, target));
    }

    private static void crash(ServerLevel level, ServerPlayer caster, LivingEntity target, Vec3 blocked) {
        double speed = blocked.length();
        float damage = (float) Math.min(damageOf("grab"), 2.0 + speed * 4.0);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().playerAttack(caster), damage);
        smashFx(level, target, blocked);
    }

    private static void smashFx(ServerLevel level, LivingEntity target, Vec3 blocked) {
        Vec3 center = target.getBoundingBox().getCenter();
        Vec3 contact = center.add(blocked.normalize().scale(target.getBbWidth() / 2 + 0.3));
        BlockState hit = level.getBlockState(BlockPos.containing(contact));
        if (!hit.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, hit), contact.x, contact.y, contact.z,
                    30, 0.3, 0.3, 0.3, 0.3);
        }
        ParticleFx.cloud(level, ParticleTypes.CRIT, center, 12, 0.3, 0.4);
        ParticleFx.cloud(level, ParticleTypes.POOF, contact, 6, 0.2, 0.05);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, SoundSource.PLAYERS,
                0.8F, 0.8F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.3F);
    }

    private static Effect thrown(ServerPlayer caster, LivingEntity target) {
        double[] lastSpeed = { THROW_SPEED };
        return (level, age) -> {
            if (!target.isAlive() || target.isRemoved() || age >= THROWN_TRACK) {
                return false;
            }
            Vec3 velocity = target.getDeltaMovement();
            // Skip the first ticks: the collision flags still describe the moment it was held.
            if (age >= 2 && (target.horizontalCollision || target.verticalCollision)
                    && lastSpeed[0] > ability("grab").value("smashSpeed")) {
                Vec3 direction = velocity.lengthSqr() > 1.0E-4 ? velocity.normalize() : new Vec3(0, -1, 0);
                crash(level, caster, target, direction.scale(lastSpeed[0]));
                return false;
            }
            if (age % 2 == 0) {
                ParticleFx.at(level, ParticleTypes.CLOUD, target.getBoundingBox().getCenter());
            }
            lastSpeed[0] = velocity.length();
            return true;
        };
    }

    boolean blockAction(ServerLevel level, boolean cluster) {
        Arm arm = this.freeArm();
        if (arm == null) {
            return this.placeBlocks(level);
        }
        double range = BUILD_RANGE;
        TentacleBlocks.Load load = TentacleBlocks.pickUp(level, this.caster, cluster, BUILD_CLUSTER, range);
        if (load == null) {
            this.caster.displayClientMessage(
                    Component.translatable("octopus." + MultiversePowers.MODID + ".build.nothing"), true);
            return false;
        }
        arm.load = load;
        arm.job = Job.CARRY;
        arm.age = 0;
        this.caster.displayClientMessage(
                Component.translatable("octopus." + MultiversePowers.MODID + ".build.taken", load.size()), true);
        this.afterCarryChange();
        return true;
    }

    boolean placeBlocks(ServerLevel level) {
        double range = BUILD_RANGE;
        for (Arm arm : this.arms) {
            if (arm.load == null) {
                continue;
            }
            TentacleBlocks.Result result = TentacleBlocks.place(level, this.caster, arm.load, range);
            if (result.placed() <= 0) {
                this.caster.displayClientMessage(
                        Component.translatable("octopus." + MultiversePowers.MODID + ".build.no_room"), true);
                return false;
            }
            arm.load = result.left();
            if (arm.load == null) {
                this.toRest(arm);
            }
            this.caster.displayClientMessage(Component.translatable(
                    "octopus." + MultiversePowers.MODID + ".build.placed", result.placed()), true);
            this.afterCarryChange();
            return true;
        }
        this.caster.displayClientMessage(
                Component.translatable("octopus." + MultiversePowers.MODID + ".build.empty"), true);
        return false;
    }

    private boolean throwLoads(ServerLevel level) {
        boolean any = false;
        for (Arm arm : this.arms) {
            if (arm.load == null) {
                continue;
            }
            TentacleBlocks.hurl(level, this.caster, arm.load, arm.tip, this.caster.getLookAngle(),
                    BUILD_DAMAGE, TentacleBlocks.THROW_SPEED);
            arm.load = null;
            this.toRest(arm);
            any = true;
        }
        if (any) {
            this.afterCarryChange();
        }
        return any;
    }

    void dropLoads(ServerLevel level) {
        for (Arm arm : this.arms) {
            if (arm.load != null) {
                TentacleBlocks.drop(level, this.caster, arm.load, arm.tip);
                arm.load = null;
            }
        }
    }

    private void afterCarryChange() {
        if (!this.caster.hasDisconnected()) {
            PacketDistributor.sendToPlayer(this.caster, new GrabStatePayload(this.hasThrowable(), this.hasLoad()));
        }
    }
}
