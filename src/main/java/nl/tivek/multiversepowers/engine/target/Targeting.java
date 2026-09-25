package nl.tivek.multiversepowers.engine.target;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.entity.HeldMobs;

public final class Targeting {
    private static final double AIM_ASSIST = 0.5;

    private Targeting() {
    }

    public record Aim(Vec3 point, @Nullable LivingEntity entity) {
        public Vec3 current() {
            return this.entity != null && this.entity.isAlive() ? this.entity.position() : this.point;
        }
    }

    public static Aim aim(ServerPlayer player, ServerLevel level, double range) {
        Vec3 point = aimPoint(player, level, range);
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        AABB searchBox = player.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, end, searchBox,
                entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator(),
                range * range);
        if (entityHit != null && entityHit.getEntity().position().distanceToSqr(point) < 1.0E-4) {
            return new Aim(point, (LivingEntity) entityHit.getEntity());
        }
        return new Aim(point, null);
    }

    public static Vec3 aimPoint(ServerPlayer player, ServerLevel level, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 target = block.getType() == HitResult.Type.MISS ? end : block.getLocation();

        AABB searchBox = player.getBoundingBox().expandTowards(target.subtract(eye)).inflate(1.0);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eye, target, searchBox,
                entity -> entity instanceof LivingEntity && entity.isPickable() && !entity.isSpectator(),
                eye.distanceToSqr(target));
        if (entityHit != null) {
            return entityHit.getEntity().position();
        }

        BlockPos start = block.getType() == HitResult.Type.MISS
                ? BlockPos.containing(end)
                : block.getBlockPos().relative(block.getDirection());
        return new Vec3(target.x, floorBelow(level, start), target.z);
    }

    public static double floorBelow(ServerLevel level, BlockPos start) {
        BlockPos pos = start;
        for (int i = 0; i < 32 && pos.getY() > level.getMinBuildHeight(); i++) {
            BlockPos below = pos.below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return pos.getY();
            }
            pos = below;
        }
        return start.getY();
    }

    @Nullable
    public static LivingEntity aimLiving(ServerPlayer player, ServerLevel level, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult block = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        Vec3 limit = block.getType() == HitResult.Type.MISS ? end : block.getLocation();
        AABB searchBox = player.getBoundingBox().expandTowards(limit.subtract(eye)).inflate(1.5);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, searchBox,
                entity -> isTargetable(player, entity))) {
            Optional<Vec3> hit = living.getBoundingBox().inflate(AIM_ASSIST).clip(eye, limit);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < bestDistance) {
                bestDistance = eye.distanceToSqr(hit.get());
                best = living;
            }
        }
        return best;
    }

    public static boolean isTargetable(ServerPlayer player, Entity entity) {
        if (entity == player || !(entity instanceof LivingEntity living) || !living.isAlive() || entity.isSpectator()
                || entity instanceof ArmorStand || entity.getType().is(Tags.EntityTypes.BOSSES)
                || HeldMobs.isHeldByAnyone(entity)) {
            return false;
        }
        if (entity instanceof Player other) {
            return player.server.isPvpAllowed() && !other.isCreative() && player.canHarmPlayer(other);
        }
        return true;
    }

    public static boolean clearPath(ServerLevel level, Vec3 from, Vec3 to, Entity viewer) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, viewer))
                .getType() == HitResult.Type.MISS;
    }

    public static void noTarget(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("spell." + MultiversePowers.MODID + ".no_target"), true);
    }

    public static Vec3 handPoint(ServerPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 right = new Vec3(-look.z, 0, look.x);
        if (right.lengthSqr() < 1.0E-4) {
            right = new Vec3(1, 0, 0);
        }
        return player.getEyePosition().add(look.scale(0.8)).add(right.normalize().scale(0.35)).add(0, -0.35, 0);
    }
}
