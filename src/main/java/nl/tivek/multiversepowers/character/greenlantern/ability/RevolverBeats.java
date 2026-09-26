package nl.tivek.multiversepowers.character.greenlantern.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.RevolverDuo;
import nl.tivek.multiversepowers.character.greenlantern.RevolverGun;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.RevolverDuo.*;

final class RevolverBeats {
    private static final double SHOT_RANGE = 64.0;
    private static final double SHOT_WIDE = 0.6;
    private static final double SLAM_REACH = 5.5;
    private static final float[] BANJO = { 0.94F, 1.19F, 1.41F, 1.19F, 1.59F, 1.41F, 1.19F, 0.94F };
    private static final int[] BANJO_AT = { 0, 3, 6, 9, 13, 16, 20, 26 };
    private static final int[] CLAW_BLIPS = { CLAW_UP, CLAW_UP + 6, CLAW_UP + 12, CLAW_UP + 16, CLAW_UP + 22 };

    private RevolverBeats() {
    }

    static void play(RevolverAssembly show, ServerLevel level) {
        int t = show.t;
        if (t == ARRIVES) {
            RevolverDuo duo = show.duo();
            opens(show, level, duo.aPortal, 1.5F);
            opens(show, level, duo.bPortal, 1.7F);
        }
        if (t == OUT) {
            RevolverDuo duo = show.duo();
            for (Vec3 wrist : new Vec3[] { duo.aPlace.wrist(), duo.bPlace.wrist() }) {
                ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), wrist, 8, 0.6, 0.03);
                show.sound(level, wrist, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.3F);
            }
        }
        if (t == TOP_OPENS || t == HAT_IN || t == GUN_IN) {
            HandDuo.Portal top = show.duo().topPortal;
            if (t == TOP_OPENS) {
                opens(show, level, top, 0.8F);
            } else {
                show.sound(level, top.center(), SoundEvents.ITEM_PICKUP, 1.8F, 0.4F);
                show.sound(level, top.center(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 1.6F, 0.6F);
            }
        }
        hat(show, level, t);
        pews(show, level, t);
        build(show, level, t);
        load(show, level, t);
        shots(show, level, t);
        slams(show, level, t);
        bows(show, level, t);
    }

    private static void hat(RevolverAssembly show, ServerLevel level, int t) {
        if (t == HAT_PINCH || t == PARTS_GRAB) {
            Vec3 hand = show.duo().aPlace.wrist();
            show.sound(level, hand, SoundEvents.ITEM_PICKUP, 1.4F, 0.7F);
            if (t == PARTS_GRAB) {
                show.sound(level, hand, SoundEvents.CHAIN_PLACE, 1.6F, 1.2F);
            }
        }
        if (t == HAT_OUT) {
            Vec3 hand = show.duo().aPlace.wrist();
            ParticleFx.cloud(level, ParticleTypes.END_ROD, hand, 10, 1.2, 0.03);
            show.sound(level, hand, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.8F, 1.6F);
            show.sound(level, hand, SoundEvents.PLAYER_LEVELUP, 0.6F, 1.8F);
        }
        if (t == HAT_ON) {
            Vec3 hand = show.duo().bPlace.wrist();
            show.sound(level, hand, SoundEvents.ARMOR_EQUIP_LEATHER.value(), 2.0F, 0.8F);
            show.sound(level, hand, SoundEvents.WOOL_PLACE, 1.6F, 0.7F);
        }
        if (t == HAT_TAP) {
            show.sound(level, show.duo().bPlace.wrist(), SoundEvents.NOTE_BLOCK_HAT.value(), 2.0F, 1.6F);
        }
        for (int i = 0; i < BANJO_AT.length; i++) {
            if (t == SHAKE_FROM + BANJO_AT[i]) {
                show.sound(level, show.duo().bPlace.wrist(), SoundEvents.NOTE_BLOCK_BANJO.value(), 2.0F, BANJO[i]);
            }
        }
        if (t == HAT_FLICK) {
            show.sound(level, show.duo().bPlace.wrist(), SoundEvents.NOTE_BLOCK_PLING.value(), 1.6F, 1.8F);
        }
        if (t == HAT_CATCH) {
            show.sound(level, show.duo().bPlace.wrist(), SoundEvents.WOOL_PLACE, 1.6F, 1.0F);
        }
        if (t == HAT_THROW) {
            show.sound(level, show.duo().bPlace.wrist(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.8F, 1.4F);
        }
    }

    private static void pews(RevolverAssembly show, ServerLevel level, int t) {
        if (t == GUN_FORM) {
            show.sound(level, show.duo().bPlace.wrist(), SoundEvents.LEVER_CLICK, 1.6F, 1.6F);
        }
        for (int i = 0; i < PEWS.length; i++) {
            if (t == PEWS[i]) {
                show.firedPew(i);
                Vec3 tip = RevolverDuo.indexTip(show.duo().bPlace, true);
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), tip, 8, 0.2);
                show.sound(level, tip, SoundEvents.NOTE_BLOCK_BIT.value(), 2.0F, 2.0F - 0.08F * i);
                show.sound(level, tip, SoundEvents.AMETHYST_BLOCK_HIT, 1.6F, 1.8F);
            }
            if (t == PEWS[i] + BOLT_TICKS) {
                LivingEntity target = show.pewTarget(i);
                Vec3 at = show.aim;
                if (target != null && show.alive(level, target)) {
                    at = target.getBoundingBox().getCenter();
                    Vec3 away = at.subtract(show.base).multiply(1.0, 0.0, 1.0);
                    hit(show, level, target, show.ability.value("revolverPewDamage"), away, 0.15, 0.1);
                }
                ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 14, 0.35);
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 0.8F), at, 10, 0.25);
                show.sound(level, at, SoundEvents.AMETHYST_CLUSTER_HIT, 1.4F, 1.7F);
            }
        }
        if (t == BLOW) {
            Vec3 tip = RevolverDuo.indexTip(show.duo().bPlace, true);
            ParticleFx.send(level, ParticleTypes.SMOKE, tip.x, tip.y + 0.3, tip.z, 8, 0.1, 0.3, 0.1, 0.01);
            show.sound(level, tip, SoundEvents.FIRE_EXTINGUISH, 0.8F, 1.8F);
        }
    }

    private static void build(RevolverAssembly show, ServerLevel level, int t) {
        if (t == PARTS_FLICK) {
            Vec3 hand = show.duo().aPlace.wrist();
            show.sound(level, hand, SoundEvents.NOTE_BLOCK_CHIME.value(), 1.6F, 1.2F);
            show.sound(level, hand, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 1.5F);
        }
        for (int i = 0; i < CLAW_BLIPS.length; i++) {
            if (t == CLAW_BLIPS[i]) {
                show.sound(level, show.duo().bPlace.wrist(), SoundEvents.NOTE_BLOCK_BIT.value(), 1.6F,
                        0.8F + 0.15F * i);
            }
        }
        if (t == CLAW_CLAMP) {
            Vec3 hand = show.duo().bPlace.wrist();
            show.sound(level, hand, SoundEvents.IRON_TRAPDOOR_CLOSE, 1.6F, 1.2F);
            show.sound(level, hand, SoundEvents.NOTE_BLOCK_BIT.value(), 1.6F, 0.6F);
        }
        if (t == CLAW_DROP) {
            Vec3 hand = show.duo().bPlace.wrist();
            for (int i = 0; i < 3; i++) {
                show.sound(level, hand, SoundEvents.NOTE_BLOCK_BIT.value(), 1.4F, 1.2F + 0.3F * i);
            }
        }
        for (int part = 0; part < PARTS; part++) {
            if (t == BUILT[part] - FLY_TICKS && (part == BARREL || part == HAMMER || part == GUARD)) {
                show.sound(level, show.duo().aPlace.wrist(), SoundEvents.WOODEN_BUTTON_CLICK_ON, 2.0F, 1.9F);
            }
            if (t == BUILT[part]) {
                Vec3 gun = show.duo().gun.at(RevolverGun.CENTER);
                ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, gun, 10, 0.3);
                SoundEvent[] clack = { SoundEvents.ANVIL_PLACE, SoundEvents.ARMOR_EQUIP_IRON.value(),
                        SoundEvents.IRON_DOOR_CLOSE, SoundEvents.CROSSBOW_LOADING_END.value(), SoundEvents.LEVER_CLICK,
                        SoundEvents.IRON_TRAPDOOR_CLOSE };
                show.sound(level, gun, clack[part], part == FRAME ? 0.8F : 2.0F, part == FRAME ? 1.8F : 1.5F);
                show.sound(level, gun, SoundEvents.CHAIN_PLACE, 1.2F, 1.6F);
            }
        }
        if (t == WHOLE + 2) {
            Vec3 gun = show.duo().gun.at(RevolverGun.CENTER);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, gun, 16, 2.0, 0.05);
            show.sound(level, gun, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.2F);
            show.sound(level, gun, SoundEvents.PLAYER_LEVELUP, 0.8F, 1.5F);
        }
    }

    private static void load(RevolverAssembly show, ServerLevel level, int t) {
        if (t == GRABS) {
            show.sound(level, show.duo().gun.at(RevolverGun.GRIP_HOLE), SoundEvents.ARMOR_EQUIP_NETHERITE.value(),
                    1.8F, 0.8F);
        }
        for (int i = 0; i < BULLETS_IN_HAND.length; i++) {
            if (t == BULLETS_IN_HAND[i]) {
                show.sound(level, show.duo().bPlace.wrist(), SoundEvents.AMETHYST_CLUSTER_PLACE, 1.4F, 1.3F + 0.1F * i);
            }
            if (t == LOADS[i]) {
                show.sound(level, show.duo().gun.at(RevolverGun.CYLINDER_AT), SoundEvents.ARMOR_EQUIP_CHAIN.value(),
                        1.6F, 1.5F + 0.05F * i);
            }
        }
        if (t == CYL_OPEN) {
            Vec3 at = show.duo().gun.at(RevolverGun.CYLINDER_AT);
            show.sound(level, at, SoundEvents.IRON_TRAPDOOR_OPEN, 1.6F, 1.5F);
            show.sound(level, at, SoundEvents.CROSSBOW_QUICK_CHARGE_1.value(), 1.4F, 1.4F);
        }
        if (t == CYL_CLOSE) {
            Vec3 at = show.duo().gun.at(RevolverGun.CYLINDER_AT);
            show.sound(level, at, SoundEvents.IRON_TRAPDOOR_CLOSE, 2.0F, 1.3F);
            show.sound(level, at, SoundEvents.LEVER_CLICK, 2.4F, 0.7F);
            show.sound(level, at, SoundEvents.CROSSBOW_LOADING_END.value(), 1.6F, 1.2F);
        }
    }

    private static void shots(RevolverAssembly show, ServerLevel level, int t) {
        for (int i = 0; i < SHOTS.length; i++) {
            if (t == SHOTS[i] - 8) {
                show.sound(level, show.duo().gun.at(RevolverGun.CYLINDER_AT), SoundEvents.LEVER_CLICK, 1.6F, 1.5F);
            }
            if (t == SHOTS[i]) {
                shoot(show, level);
            }
        }
        for (int dry : DRY) {
            if (t == dry - 8) {
                show.sound(level, show.duo().gun.at(RevolverGun.CYLINDER_AT), SoundEvents.LEVER_CLICK, 1.6F, 1.5F);
            }
            if (t == dry) {
                show.sound(level, show.duo().gun.at(RevolverGun.CYLINDER_AT), SoundEvents.DISPENSER_FAIL, 2.0F, 1.8F);
            }
        }
        if (t == SPIN_FROM + 2) {
            show.sound(level, show.duo().gun.at(RevolverGun.CENTER), SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 1.7F);
        }
    }

    private static void shoot(RevolverAssembly show, ServerLevel level) {
        RevolverDuo duo = show.duo();
        Vec3 muzzle = duo.gun.at(RevolverGun.MUZZLE);
        Vec3 way = show.aim.subtract(muzzle);
        way = way.lengthSqr() < 1.0E-4 ? duo.gun.forward() : way.normalize();
        Vec3 end = muzzle.add(way.scale(SHOT_RANGE));
        BlockHitResult wall = LoadedWorld.clip(level, new ClipContext(muzzle, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        double damage = show.ability.value("revolverShotDamage");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(muzzle, end).inflate(SHOT_WIDE + 1.0), entity -> GiantHands.fair(show.owner, entity))) {
            AABB box = living.getBoundingBox().inflate(SHOT_WIDE);
            if (box.contains(muzzle) || box.clip(muzzle, end).isPresent()) {
                hit(show, level, living, damage, way, 0.9, 0.25);
                ParticleFx.send(level, ParticleTypes.CRIT, living.getX(), living.getY() + living.getBbHeight() * 0.5,
                        living.getZ(), 12, 0.3, 0.3, 0.3, 0.3);
            }
        }
        ParticleFx.line(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), muzzle, end, 0.8);
        ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, muzzle.x, muzzle.y, muzzle.z, 4, 0.3, 0.3, 0.3,
                0.02);
        ParticleFx.send(level, ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0, 0.0, 0.0, 0.0);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), end, 16, 0.35);
        show.sound(level, muzzle, SoundEvents.GENERIC_EXPLODE.value(), 2.6F, 1.25F);
        show.sound(level, muzzle, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 2.4F, 0.6F);
        show.sound(level, muzzle, SoundEvents.CROSSBOW_SHOOT, 2.0F, 0.5F);
        show.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.6F);
    }

    private static void slams(RevolverAssembly show, ServerLevel level, int t) {
        for (int i = 0; i < SLAMS.length; i++) {
            if (t == SLAMS[i] - 5) {
                show.sound(level, show.duo().gun.at(RevolverGun.BUTT), SoundEvents.PLAYER_ATTACK_SWEEP, 1.8F,
                        0.6F + 0.1F * i);
            }
            if (t == SLAMS[i]) {
                slam(show, level, i);
            }
        }
        if (t == TOSS) {
            show.sound(level, show.duo().gun.at(RevolverGun.CENTER), SoundEvents.PLAYER_ATTACK_SWEEP, 1.6F, 1.2F);
        }
        if (t == TOP_SHUT) {
            HandDuo.Portal top = show.duo().topPortal;
            rim(level, top, true);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, top.center(), 20, top.radius() * 0.5, 0.1);
            show.sound(level, top.center(), SoundEvents.BEACON_DEACTIVATE, 2.0F, 1.4F);
            show.sound(level, top.center(), SoundEvents.IRON_DOOR_CLOSE, 1.8F, 0.6F);
        }
    }

    private static void slam(RevolverAssembly show, ServerLevel level, int i) {
        Vec3 at = show.stage.point(SLAM_AT);
        Vec3 ground = ground(level, at);
        double reach = SLAM_REACH * show.stage.scale() * (1.0 + 0.15 * i);
        double damage = show.ability.value("revolverSlamDamage");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(ground, ground).inflate(reach + 2.0), entity -> GiantHands.fair(show.owner, entity))) {
            Vec3 to = living.position().subtract(ground);
            Vec3 flat = new Vec3(to.x, 0.0, to.z);
            double out = flat.length();
            if (out > reach + living.getBbWidth() * 0.5 || to.y < -2.5 || to.y > 3.5) {
                continue;
            }
            Vec3 away = out < 1.0E-2 ? show.stage.way() : flat.scale(1.0 / out);
            hit(show, level, living, damage * Mth.lerp(Math.min(1.0, out / reach), 1.0, 0.5), away, 1.6, 0.9);
        }
        BlockPos under = BlockPos.containing(ground.x, ground.y - 0.5, ground.z);
        BlockState block = level.isLoaded(under) ? level.getBlockState(under) : null;
        if (block != null && !block.isAir()) {
            ParticleFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, block), ground.x, ground.y + 0.2,
                    ground.z, 50, 1.6, 0.2, 1.6, 0.25);
        }
        Vec3 low = ground.add(0.0, 0.2, 0.0);
        ParticleFx.send(level, ParticleTypes.EXPLOSION_EMITTER, ground.x, ground.y + 0.4, ground.z, 1, 0.0, 0.0, 0.0,
                0.0);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 2.0F), low, 56, 0.9 + 0.15 * i);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.5F), low, 36, 0.5 + 0.1 * i);
        ParticleFx.send(level, ParticleTypes.CAMPFIRE_COSY_SMOKE, ground.x, ground.y + 0.4, ground.z, 10, 1.8, 0.4,
                1.8, 0.03);
        show.sound(level, ground, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.4F, 0.7F - 0.05F * i);
        show.sound(level, ground, SoundEvents.ANVIL_LAND, 1.6F, 0.5F);
        show.sound(level, ground, SoundEvents.GENERIC_EXPLODE.value(), 2.0F, 0.8F - 0.08F * i);
    }

    private static void bows(RevolverAssembly show, ServerLevel level, int t) {
        if (t == WIPED) {
            RevolverDuo duo = show.duo();
            ParticleOptions drop = ParticleFx.dust(PowerRing.PALE, 0.7F);
            for (Vec3 wrist : new Vec3[] { duo.aPlace.at(new Vec3(0.0, 3.0, -0.4)),
                    duo.bPlace.at(new Vec3(0.0, 3.0, -0.4)) }) {
                ParticleFx.send(level, drop, wrist.x, wrist.y, wrist.z, 14, 0.6, 0.3, 0.6, 0.15);
                show.sound(level, wrist, SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 2.0F, 1.2F);
            }
        }
        for (int i = 0; i < 3; i++) {
            if (t == TIRED + 2 + 7 * i) {
                show.sound(level, show.duo().aPlace.wrist(), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), 1.6F,
                        0.9F - 0.12F * i);
            }
        }
        if (t == HIGH_FIVE) {
            RevolverDuo duo = show.duo();
            Vec3 clap = duo.aPlace.wrist().lerp(duo.bPlace.wrist(), 0.5).add(0.0, 2.0, 0.0);
            ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.3F), clap, 24, 0.4);
            ParticleFx.cloud(level, ParticleTypes.END_ROD, clap, 10, 0.4, 0.08);
            show.sound(level, clap, SoundEvents.PLAYER_ATTACK_CRIT, 2.4F, 1.2F);
            show.sound(level, clap, SoundEvents.NOTE_BLOCK_SNARE.value(), 2.4F, 1.4F);
        }
        if (t == SALUTE) {
            Vec3 hand = show.duo().aPlace.wrist();
            for (int i = 0; i < 3; i++) {
                show.sound(level, hand, SoundEvents.NOTE_BLOCK_BANJO.value(), 1.8F, BANJO[i * 3]);
            }
            show.sound(level, hand, SoundEvents.PLAYER_LEVELUP, 0.5F, 1.9F);
        }
        if (t == RETRACT) {
            RevolverDuo duo = show.duo();
            show.sound(level, duo.aPlace.wrist(), SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.6F);
            show.sound(level, duo.bPlace.wrist(), SoundEvents.ENDER_DRAGON_FLAP, 1.2F, 1.7F);
        }
        if (t == HANDS_GONE) {
            RevolverDuo duo = show.duo();
            shuts(show, level, duo.aPortal, 1.6F);
            shuts(show, level, duo.bPortal, 1.8F);
        }
    }

    private static Vec3 ground(ServerLevel level, Vec3 at) {
        Vec3 from = at.add(0.0, 3.0, 0.0);
        if (!level.isLoaded(BlockPos.containing(from))) {
            return at;
        }
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, at.subtract(0.0, 6.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
    }

    private static void hit(RevolverAssembly show, ServerLevel level, LivingEntity living, double damage, Vec3 away,
            double out, double up) {
        // Hits in quick succession all land.
        living.invulnerableTime = 0;
        living.hurt(level.damageSources().playerAttack(show.owner), (float) damage);
        double knockback = show.ability.value("revolverKnockback");
        double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        flat = flat.lengthSqr() < 1.0E-6 ? Vec3.ZERO : flat.normalize();
        Vec3 push = flat.scale(out * knockback).add(0.0, up * Math.min(1.0, knockback), 0.0).scale(1.0 - resist);
        if (push.lengthSqr() > 1.0E-6) {
            living.setDeltaMovement(living.getDeltaMovement().add(push));
            living.hasImpulse = true;
            // Players move themselves on their own client, so they have to be told about the push.
            living.hurtMarked = true;
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), living.getBoundingBox().getCenter(), 8,
                0.3, 0.05);
    }

    private static void opens(RevolverAssembly show, ServerLevel level, HandDuo.Portal portal, float pitch) {
        Vec3 middle = portal.center();
        rim(level, portal, false);
        ParticleFx.cloud(level, ParticleTypes.END_ROD, middle, 8, portal.radius() * 0.3, 0.06);
        show.sound(level, middle, SoundEvents.BEACON_ACTIVATE, 1.6F, pitch);
        show.sound(level, middle, SoundEvents.ENDER_DRAGON_FLAP, 1.4F, pitch * 0.9F);
        show.sound(level, middle, SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, pitch * 0.7F);
    }

    private static void shuts(RevolverAssembly show, ServerLevel level, HandDuo.Portal portal, float pitch) {
        Vec3 middle = portal.center();
        rim(level, portal, true);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), middle, 8, 0.3, 0.05);
        show.sound(level, middle, SoundEvents.ITEM_PICKUP, 1.6F, pitch * 0.35F);
        show.sound(level, middle, SoundEvents.BEACON_DEACTIVATE, 1.2F, pitch);
    }

    private static void rim(ServerLevel level, HandDuo.Portal portal, boolean in) {
        ParticleOptions light = ParticleFx.dust(PowerRing.BRIGHT, 1.3F);
        int points = Math.max(12, (int) Math.round(portal.radius() * 10.0));
        for (int i = 0; i < points; i++) {
            double angle = Math.PI * 2.0 * i / points;
            Vec3 out = portal.a().scale(Math.cos(angle)).add(portal.b().scale(Math.sin(angle)));
            Vec3 at = portal.center().add(out.scale(portal.radius()));
            ParticleFx.fly(level, light, at, in ? out.scale(-1.0) : out, in ? 0.3 : 0.4);
        }
    }
}
