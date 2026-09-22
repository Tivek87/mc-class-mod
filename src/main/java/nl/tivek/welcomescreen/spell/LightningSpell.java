package nl.tivek.welcomescreen.spell;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Lightning Strike: sparks jump from your hand, a storm cloud gathers over the
 * target while a glowing
 * rune circle charges on the ground, then a branching bolt tears down with a
 * flash, a shockwave of
 * sparks and a crackling scorch mark. A creature you aimed at is followed while
 * the spell charges.
 */
final class LightningSpell {
    private static final double RANGE = 40.0;
    private static final int CHARGE = 14;
    private static final int DURATION = CHARGE + 30;
    private static final double RUNE_RADIUS = 2.2;
    private static final double CLOUD_HEIGHT = 18.0;

    private static final int GLOW = 0x00D2FF;
    private static final int CYAN = 0x48DBFB;
    private static final int WHITE = 0xF4FBFF;
    private static final int DEEP = 0x0984E3;
    private static final int STORM = 0x1E272E;

    private LightningSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        SpellTargeting.Aim aim = SpellTargeting.aim(player, level, RANGE);
        Vec3 hand = SpellTargeting.handPoint(player);
        SpellFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, hand, 16, 0.2, 0.25);
        SpellFx.zigzag(level, SpellFx.dust(GLOW, 0.6F), hand, hand.add(0, 1.6, 0), 5, 0.25, 0.1);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BEACON_POWER_SELECT,
                SoundSource.PLAYERS, 1.0F, 1.8F);
        SpellCasting.start(level, strike(player, aim));
        return true;
    }

    private static SpellEffect strike(ServerPlayer caster, SpellTargeting.Aim aim) {
        // Where the bolt lands; follows the target during the charge, then stays put.
        Vec3[] target = { aim.current() };
        return (level, age) -> {
            if (age <= CHARGE) {
                target[0] = aim.current();
            }
            Vec3 at = target[0];
            if (age < CHARGE) {
                charge(level, at, age);
            } else if (age == CHARGE) {
                hit(level, caster, at);
            } else {
                afterglow(level, at, age - CHARGE);
            }
            return age < DURATION;
        };
    }

    /**
     * The rune circle drawing itself, sparks rising, and the storm cloud gathering
     * overhead.
     */
    private static void charge(ServerLevel level, Vec3 at, int age) {
        double progress = (double) age / CHARGE;
        Vec3 ground = at.add(0, 0.1, 0);
        if (age % 2 == 0) {
            SpellFx.ring(level, SpellFx.dust(GLOW, 1.2F), ground, RUNE_RADIUS, 48, age * 0.08);
            SpellFx.ring(level, SpellFx.dust(CYAN, 1.0F), ground, RUNE_RADIUS * 0.65, 32, -age * 0.1);
            SpellFx.ring(level, SpellFx.dust(WHITE, 0.8F), ground, RUNE_RADIUS * 0.35, 20, age * 0.12);

            // Rotating electric rays
            for (int r = 0; r < 4; r++) {
                double a = age * 0.05 + r * (Math.PI / 2.0);
                Vec3 p = ground.add(Math.cos(a) * RUNE_RADIUS, 0, Math.sin(a) * RUNE_RADIUS);
                SpellFx.line(level, SpellFx.dust(CYAN, 0.7F), ground, p, 0.5);
            }
        }
        int sparks = 3 + (int) (progress * 8);
        for (int i = 0; i < sparks; i++) {
            double angle = SpellFx.RANDOM.nextDouble() * Math.PI * 2;
            double distance = Math.sqrt(SpellFx.RANDOM.nextDouble()) * RUNE_RADIUS;
            SpellFx.fly(level, ParticleTypes.ELECTRIC_SPARK,
                    ground.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance), new Vec3(0, 1, 0), 0.35);
        }
        Vec3 sky = at.add(0, CLOUD_HEIGHT, 0);
        double cloudSize = 1.2 + progress * 2.8;
        SpellFx.send(level, SpellFx.dust(STORM, 4.0F), sky.x, sky.y, sky.z, 10, cloudSize, 0.4, cloudSize, 0);
        SpellFx.send(level, ParticleTypes.LARGE_SMOKE, sky.x, sky.y, sky.z, 4, cloudSize, 0.3, cloudSize, 0.01);
        if (age % 3 == 1) {
            // Little electric flickers inside the cloud
            Vec3 a = sky.add(SpellFx.spread(cloudSize), 0, SpellFx.spread(cloudSize));
            Vec3 b = a.add(SpellFx.spread(1.5), SpellFx.spread(0.5), SpellFx.spread(1.5));
            SpellFx.zigzag(level, SpellFx.dust(WHITE, 0.8F), a, b, 3, 0.3, 0.15);
            SpellFx.zigzag(level, SpellFx.dust(CYAN, 1.2F), a, b, 3, 0.2, 0.2);
        }
        if (age == CHARGE / 2) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.9F, 2.0F);
        }
    }

    /**
     * The real bolt (for damage and fire) plus visual side bolts, flash and
     * shockwave.
     */
    private static void hit(ServerLevel level, ServerPlayer caster, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at);
            bolt.setCause(caster);
            level.addFreshEntity(bolt);
        }

        drawBolt(level, at);
        SpellFx.at(level, ParticleTypes.FLASH, at.add(0, 0.5, 0));
        SpellFx.shockwave(level, ParticleTypes.ELECTRIC_SPARK, at.add(0, 0.2, 0), 56, 0.85);
        SpellFx.shockwave(level, SpellFx.dust(GLOW, 1.8F), at.add(0, 0.2, 0), 36, 0.65);
        SpellFx.sphereOut(level, SpellFx.dust(CYAN, 1.5F), at.add(0, 0.5, 0), 32, 0.5);
        SpellFx.cloud(level, ParticleTypes.LARGE_SMOKE, at, 12, 0.5, 0.05);
        BlockState ground = level.getBlockState(BlockPos.containing(at.x, at.y - 0.5, at.z));
        if (!ground.isAir()) {
            SpellFx.send(level, new BlockParticleOption(ParticleTypes.BLOCK, ground), at.x, at.y + 0.2, at.z,
                    40, 0.6, 0.2, 0.6, 0.3);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 2.0F, 1.0F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 2.0F, 1.0F);
    }

    /**
     * Flickering bolt for two more ticks, a spreading ring, then sparks and smoke
     * from the scorch.
     */
    private static void afterglow(ServerLevel level, Vec3 at, int t) {
        if (t <= 2) {
            drawBolt(level, at);
        }
        if (t <= 8) {
            ParticleOptions ring = SpellFx.fade(WHITE, DEEP, 1.3F - t * 0.1F);
            SpellFx.ring(level, ring, at.add(0, 0.15, 0), 0.6 + t * 0.45, 20 + t * 4, t * 0.3);
        }
        double fade = 1.0 - t / 30.0;
        if (SpellFx.chance(fade)) {
            SpellFx.fly(level, ParticleTypes.ELECTRIC_SPARK, at.add(SpellFx.spread(1.2), 0.1, SpellFx.spread(1.2)),
                    new Vec3(0, 1, 0), 0.15);
        }
        if (t % 4 == 0 && SpellFx.chance(fade)) {
            Vec3 a = at.add(SpellFx.spread(1.0), 0.1, SpellFx.spread(1.0));
            Vec3 b = at.add(SpellFx.spread(1.0), 0.3, SpellFx.spread(1.0));
            SpellFx.zigzag(level, SpellFx.dust(GLOW, 0.5F), a, b, 3, 0.2, 0.08);
            SpellFx.fly(level, ParticleTypes.SMOKE, at.add(SpellFx.spread(0.6), 0.2, SpellFx.spread(0.6)),
                    new Vec3(0, 1, 0), 0.04);
        }
        if (t <= 16) {
            // The storm cloud breaks up.
            Vec3 sky = at.add(0, CLOUD_HEIGHT, 0);
            double size = 3.5 + t * 0.2;
            SpellFx.send(level, SpellFx.dust(STORM, 3.0F), sky.x, sky.y, sky.z, 4, size, 0.5, size, 0);
        }
    }

    /**
     * A jagged main bolt from the cloud to the ground, with a few side branches.
     */
    private static void drawBolt(ServerLevel level, Vec3 at) {
        Vec3 top = at.add(SpellFx.spread(1.0), CLOUD_HEIGHT, SpellFx.spread(1.0));
        List<Vec3> path = jaggedPath(top, at, 18, 0.9);
        ParticleOptions core = SpellFx.dust(WHITE, 0.9F);
        ParticleOptions glow = SpellFx.dust(GLOW, 1.8F);
        for (int i = 1; i < path.size(); i++) {
            SpellFx.line(level, core, path.get(i - 1), path.get(i), 0.2);
            SpellFx.line(level, glow, path.get(i - 1), path.get(i), 0.35);
        }
        for (int branch = 0; branch < 3; branch++) {
            Vec3 start = path.get(2 + SpellFx.RANDOM.nextInt(path.size() - 5));
            Vec3 end = start.add(SpellFx.spread(3.5), -2.0 - SpellFx.RANDOM.nextDouble() * 3.0, SpellFx.spread(3.5));
            List<Vec3> side = jaggedPath(start, end, 5, 0.5);
            for (int i = 1; i < side.size(); i++) {
                SpellFx.line(level, SpellFx.dust(GLOW, 0.9F), side.get(i - 1), side.get(i), 0.25);
            }
        }
    }

    private static List<Vec3> jaggedPath(Vec3 from, Vec3 to, int segments, double jitter) {
        List<Vec3> points = new ArrayList<>();
        points.add(from);
        for (int i = 1; i < segments; i++) {
            Vec3 p = from.lerp(to, (double) i / segments);
            points.add(p.add(SpellFx.spread(jitter), SpellFx.spread(jitter * 0.3), SpellFx.spread(jitter)));
        }
        points.add(to);
        return points;
    }
}
