package nl.tivek.multiversepowers.character.docock;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.target.Targeting;

abstract class PortalPlacing extends PortalState {
    PortalPlacing(ServerPlayer caster, LivingEntity target) {
        super(caster, target);
    }

    boolean targetValid(ServerLevel level) {
        if (!OctoRig.mayHold(this.caster, this.target, level)) {
            return false;
        }
        double reach = MAX_FROM_CASTER + OctoRig.ability("portal").value("homingRangeBlocks");
        return this.held || this.target.distanceToSqr(this.caster) <= reach * reach;
    }

    void placeTarget(Vec3 center) {
        this.placeTarget(center, false);
    }

    void placeTarget(Vec3 center, boolean hard) {
        double y = center.y - this.target.getBbHeight() / 2;
        Vec3 was = this.target.position();
        if (this.target instanceof ServerPlayer player) {
            OctoRig.holdAt(player, center.x, y, center.z);
        } else if (hard) {
            this.target.teleportTo(center.x, y, center.z);
            this.target.setDeltaMovement(Vec3.ZERO);
        } else {
            this.target.moveTo(center.x, y, center.z);
            this.target.setDeltaMovement(center.x - was.x, y - was.y, center.z - was.z);
            this.target.hurtMarked = true;
            this.target.hasImpulse = true;
        }
        this.target.resetFallDistance();
    }

    void releaseTarget(boolean midAir) {
        this.clamped = false;
        if (this.held) {
            this.held = false;
            this.target.setDeltaMovement(Vec3.ZERO);
            this.target.hurtMarked = true;
            if (this.target instanceof Mob mob) {
                HeldMobs.release(mob);
            }
            if (midAir) {
                OctopusArms.setDown(this.target);
            }
        }
    }

    void impact(ServerLevel level) {
        Vec3 at = new Vec3(this.gateC.center.x, this.groundC, this.gateC.center.z);
        DamageSource source = level.damageSources().playerAttack(this.caster);
        this.releaseTarget(false);
        // Reset so recent-hit invulnerability does not block this damage.
        this.target.invulnerableTime = 0;
        this.target.hurt(source, OctoRig.damageOf("portal"));
        this.target.setDeltaMovement(0, 0.35, 0);

        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    90, 1.2, 0.3, 1.2, 0.45);
        }
        ParticleFx.cloud(level, ParticleTypes.EXPLOSION, at.add(0, 0.5, 0), 4, 0.8, 0.0);
        ParticleFx.shockwave(level, ParticleTypes.LARGE_SMOKE, at.add(0, 0.2, 0), 36, 0.4);
        ParticleFx.shockwave(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.3, 0), 44, 0.8);
        ParticleFx.cloud(level, ParticleTypes.CRIT, at.add(0, 0.5, 0), 24, 0.6, 0.5);
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8 + ParticleFx.spread(0.3);
            double length = 2.2 + ParticleFx.RANDOM.nextDouble() * 1.8;
            Vec3 end = at.add(Math.cos(angle) * length, 0.05, Math.sin(angle) * length);
            ParticleFx.zigzag(level, ParticleFx.dust(CRACK, 1.0F), at.add(0, 0.05, 0), end, 4, 0.35, 0.15);
        }
        ParticleFx.ring(level, ParticleFx.fade(HEAT, DARK, 1.8F), at.add(0, 0.15, 0), 1.6, 32, 0);
        sound(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1.5F, 0.7F);
        sound(level, at, SoundEvents.ANVIL_LAND, 0.9F, 0.5F);
        sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.7F);
    }

    void openEntry(ServerLevel level) {
        Vec3 goal = this.target.getBoundingBox().getCenter();
        Vec3 eye = this.caster.getEyePosition();
        Vec3 toCaster = new Vec3(eye.x - goal.x, 0, eye.z - goal.z);
        if (toCaster.lengthSqr() < 1.0E-4) {
            toCaster = new Vec3(-this.look.x, 0, -this.look.z);
        }
        toCaster = toCaster.lengthSqr() < 1.0E-4 ? new Vec3(1, 0, 0) : toCaster.normalize();
        double spread = Math.min(OctoRig.ability("portal").value("portalSpreadBlocks"),
                Math.max(2.0, goal.distanceTo(this.portalA) * 0.5));
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (double angle : new double[] { 0.9, -0.9, 1.4, -1.4, 0.45, -0.45, 2.0, -2.0, 0.0 }) {
            for (double distance : new double[] { 7.0, 5.5, 4.0 }) {
                for (double height : new double[] { 2.0, 3.5, 1.0 }) {
                    Vec3 center = goal.add(turn(toCaster, angle).scale(distance)).add(0, height, 0);
                    Vec3 normal = goal.subtract(center).normalize();
                    if (!fits(level, center, normal, RADIUS_B, this.caster)
                            || !Targeting.clearPath(level, center, goal, this.caster)) {
                        continue;
                    }
                    double fromA = center.distanceTo(this.portalA);
                    double score = (Targeting.clearPath(level, eye, center, this.caster) ? 2.0 : 0.0)
                            + (fromA >= spread ? 2.5 : fromA / spread * 2.5) + Math.min(fromA, 24.0) * 0.12
                            - Math.abs(Math.abs(angle) - 0.9) * 0.5 - Math.abs(distance - 5.5) * 0.2
                            - Math.abs(height - 2.0) * 0.2;
                    if (score > bestScore) {
                        bestScore = score;
                        best = center;
                    }
                }
            }
        }
        if (best == null) {
            best = goal.add(toCaster.scale(-2.5)).add(0, 1.5, 0);
        }
        best = this.pushApart(best, this.portalA, spread);
        best = this.nearCaster(best);
        Vec3 normal = goal.subtract(best);
        this.gateB.open(level, best, normal.lengthSqr() < 1.0E-4 ? toCaster.scale(-1) : normal);
    }

    private Vec3 pushApart(Vec3 point, Vec3 other, double apart) {
        Vec3 away = point.subtract(other);
        double distance = away.length();
        if (distance >= apart) {
            return point;
        }
        Vec3 direction = distance < 1.0E-4 ? new Vec3(0, 1, 0) : away.scale(1.0 / distance);
        return other.add(direction.scale(apart));
    }

    private Vec3 nearCaster(Vec3 point) {
        Vec3 eye = this.caster.getEyePosition();
        Vec3 away = point.subtract(eye);
        double distance = away.length();
        if (distance <= MAX_FROM_CASTER || distance < 1.0E-4) {
            return point;
        }
        return eye.add(away.scale(MAX_FROM_CASTER / distance));
    }

    void openSky(ServerLevel level) {
        Vec3 origin = this.target.position();
        Vec3 eye = this.caster.getEyePosition();
        Vec3 view = this.caster.getLookAngle();
        Vec3 best = null;
        double bestGround = origin.y;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = ParticleFx.RANDOM.nextDouble() * Math.PI * 2;
            double distance = attempt == 0 ? 0.0 : 1.0 + ParticleFx.RANDOM.nextDouble() * (SKY_RADIUS - 1.0);
            double x = origin.x + Math.cos(angle) * distance;
            double z = origin.z + Math.sin(angle) * distance;
            if (!loaded(level, x, z)) {
                continue;
            }
            double ground = Targeting.floorBelow(level, BlockPos.containing(x, origin.y + 3, z));
            Vec3 floor = new Vec3(x, ground, z);
            double room = this.room(level, floor);
            if (room < MIN_SKY) {
                continue;
            }
            Vec3 portal = floor.add(0, room, 0);
            if (!fits(level, portal, DOWN, RADIUS_C * 0.8, this.caster)) {
                continue;
            }
            double facing = view.dot(portal.subtract(eye).normalize());
            double fromB = Math.sqrt(portal.subtract(this.gateB.center).horizontalDistanceSqr());
            double score = room / SKY_HEIGHT + facing + Math.min(fromB, SKY_RADIUS) * 0.15
                    + (Targeting.clearPath(level, eye, portal, this.caster) ? 1.0 : 0.0)
                    + (Targeting.clearPath(level, eye, floor.add(0, 0.6, 0), this.caster) ? 0.5 : 0.0)
                    - distance * 0.03;
            if (score > bestScore) {
                bestScore = score;
                best = portal;
                bestGround = ground;
            }
        }
        if (best == null) {
            Vec3 floor = new Vec3(origin.x, origin.y, origin.z);
            best = floor.add(0, Math.max(3.0, this.room(level, floor)), 0);
            bestGround = origin.y;
        }
        this.groundC = bestGround;
        this.gateC.open(level, this.nearCaster(best), DOWN);
    }

    private double room(ServerLevel level, Vec3 floor) {
        BlockHitResult hit = level.clip(new ClipContext(floor.add(0, 0.5, 0), floor.add(0, SKY_HEIGHT, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.caster));
        return hit.getType() == HitResult.Type.MISS ? SKY_HEIGHT : hit.getLocation().y - floor.y - 1.0;
    }

    Vec3 farTip() {
        List<Vec3> shown = RobotArm.firstPart(this.trail, this.shown);
        return shown.get(shown.size() - 1);
    }

    void drawFar(ServerLevel level) {
        if (this.exit == null || this.shown <= PORTAL_DEPTH + 1.0E-3) {
            if (this.farShown) {
                this.farShown = false;
                RobotArm.remove(level, this.farArm);
            }
            return;
        }
        RobotArm.arm(this.farArm, RobotArm.firstPart(this.trail, this.shown)).claw(this.clawOpen)
                .holding(this.clamped ? this.target : null).cut(this.cut).tools(this.spike, this.thrust)
                .clip(this.exit.center, this.exit.normal.scale(-1)).send(level);
        this.farShown = true;
        if (this.thrust > 0.3) {
            Vec3 at = this.farTip();
            ParticleFx.cloud(level, ParticleTypes.FLAME, at, 2, 0.15, 0.02);
            if (ParticleFx.chance(0.5)) {
                ParticleFx.at(level, ParticleTypes.SMOKE, at);
            }
        }
    }

    private static Vec3 turn(Vec3 v, double angle) {
        double c = Math.cos(angle);
        double s = Math.sin(angle);
        return new Vec3(v.x * c - v.z * s, v.y, v.x * s + v.z * c);
    }

    private static boolean fits(ServerLevel level, Vec3 center, Vec3 normal, double radius, ServerPlayer caster) {
        if (!openSpace(level, center)) {
            return false;
        }
        Vec3[] b = ParticleFx.basis(normal.normalize());
        for (int k = 0; k < 6; k++) {
            double a = k * Math.PI / 3;
            Vec3 rim = center.add(b[0].scale(Math.cos(a) * radius * 0.85))
                    .add(b[1].scale(Math.sin(a) * radius * 0.85));
            if (!openSpace(level, rim)) {
                return false;
            }
        }
        return true;
    }

    private static boolean openSpace(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        return loaded(level, at.x, at.z) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    private static boolean loaded(ServerLevel level, double x, double z) {
        return level.isLoaded(BlockPos.containing(x, level.getMinBuildHeight(), z));
    }
}
