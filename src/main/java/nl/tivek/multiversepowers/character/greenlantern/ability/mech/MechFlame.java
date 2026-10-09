package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.flame.FlameBurn;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The mech's flamethrower: fire out of the nozzle over its right fist to where its pilot aims, wider the further it
// reaches and stopped by a wall. Whatever is in it is struck every few ticks and set burning.
final class MechFlame {
    private static final double NEAR = 0.9;
    private static final double FAR = 3.0;
    private static final int EVERY = 4;
    private static final int GLOW = 0xE4FFEA;

    private MechFlame() {
    }

    static void pour(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 toward, CharacterAbility ability,
            int age) {
        if (toward.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 way = toward.normalize();
        double range = ability.value("mechFlameReach");
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(from, from.add(way.scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        boolean stopped = wall.getType() != HitResult.Type.MISS;
        double reach = stopped ? from.distanceTo(wall.getLocation()) : range;
        if (age % 5 == 0) {
            Sounds.play(level, from, SoundEvents.BLAZE_SHOOT, 2.5F, 0.45F + 0.1F * level.random.nextFloat());
        }
        if (stopped && age % 2 == 0) {
            ParticleFx.cloud(level, ParticleFx.fade(GLOW, PowerRing.GREEN, 1.6F), wall.getLocation(), 5, 0.7, 0.06);
        }
        if (age % EVERY != 0) {
            return;
        }
        Vec3 end = from.add(way.scale(reach));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(FAR + 1.0),
                living -> PowerRing.canHit(owner, living))) {
            Vec3 middle = target.getBoundingBox().getCenter();
            double along = middle.subtract(from).dot(way);
            if (along < -1.0 || along > reach + target.getBbWidth()) {
                continue;
            }
            double radius = Mth.lerp(Mth.clamp(along / range, 0.0, 1.0), NEAR, FAR);
            double off = middle.subtract(from.add(way.scale(Math.max(0.0, along)))).length()
                    - Math.max(target.getBbWidth(), target.getBbHeight()) * 0.5;
            if (off > radius) {
                continue;
            }
            MechBlows.hit(level, owner, target, ability.value("mechFlameDamage"));
            FlameBurn.ignite(level, target);
            ParticleFx.cloud(level, ParticleFx.fade(GLOW, PowerRing.GREEN, 1.2F), middle, 4, 0.4, 0.05);
        }
    }
}
