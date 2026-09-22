package nl.tivek.welcomescreen.spell;

import java.util.UUID;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Poison Area: a glowing green vial arcs from your hand to the target and shatters, leaving a bubbling
 * poison cloud with a turning rune ring and low toxic fog. Everything inside is poisoned, except you.
 */
final class PoisonSpell {
    private static final double RANGE = 24.0;
    private static final double RADIUS = 3.5;
    private static final int DURATION = 160;
    private static final int SPREAD_TIME = 8;
    private static final int FADE_TIME = 25;
    private static final int PULSE = 10;

    private static final int BRIGHT = 0x9BF26B;
    private static final int GREEN = 0x5FB04A;
    private static final int DARK = 0x2E5A1E;
    private static final int FOG = 0x3F7A2A;
    private static final int VIAL_GLASS = 0xD8FFE0;

    private PoisonSpell() {
    }

    static boolean cast(ServerPlayer player, ServerLevel level) {
        Vec3 target = SpellTargeting.aimPoint(player, level, RANGE);
        Vec3 hand = SpellTargeting.handPoint(player);
        double distance = hand.distanceTo(target);
        int flight = Mth.clamp((int) Math.round(distance / 1.2), 6, 18);
        double peak = 1.2 + distance * 0.15;
        UUID owner = player.getUUID();

        SpellFx.cloud(level, effect(GREEN), hand, 8, 0.15, 0.0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SPLASH_POTION_THROW,
                SoundSource.PLAYERS, 1.0F, 0.7F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WITCH_THROW,
                SoundSource.PLAYERS, 0.6F, 1.2F);

        SpellCasting.start(level, (lvl, age) -> {
            double t = Math.min(1.0, (age + 1.0) / flight);
            Vec3 point = hand.lerp(target, t).add(0, Math.sin(t * Math.PI) * peak, 0);
            SpellFx.sphere(lvl, SpellFx.dust(VIAL_GLASS, 0.6F), point, 0.18, 6, age * 0.8);
            SpellFx.cloud(lvl, SpellFx.dust(BRIGHT, 1.2F), point, 3, 0.08, 0.0);
            SpellFx.at(lvl, effect(GREEN), point);
            if (age % 2 == 0) {
                SpellFx.fly(lvl, ParticleTypes.FALLING_SPORE_BLOSSOM, point, new Vec3(0, -1, 0), 0.0);
            }
            if (t >= 1.0) {
                shatter(lvl, target);
                SpellCasting.start(lvl, cloud(target, owner));
                return false;
            }
            return true;
        });
        return true;
    }

    private static ParticleOptions effect(int rgb) {
        return ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0xFF000000 | rgb);
    }

    private static void shatter(ServerLevel level, Vec3 at) {
        Vec3 splash = at.add(0, 0.3, 0);
        SpellFx.sphereOut(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SLIME_BALL)),
                splash, 24, 0.25);
        SpellFx.sphereOut(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GLASS_BOTTLE)),
                splash, 10, 0.2);
        SpellFx.shockwave(level, effect(BRIGHT), splash, 36, 0.6);
        SpellFx.cloud(level, SpellFx.dust(BRIGHT, 2.0F), splash, 20, 0.5, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.2F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /** The lingering cloud: spreads out, bubbles and fumes, poisons on a pulse, then thins away. */
    private static SpellEffect cloud(Vec3 center, UUID owner) {
        return (level, age) -> {
            double radius = RADIUS * Math.min(1.0, (age + 1.0) / SPREAD_TIME);
            // 1 while active, thinning to 0 over the last FADE_TIME ticks.
            double density = Math.min(1.0, (DURATION - age) / (double) FADE_TIME);
            Vec3 ground = center.add(0, 0.1, 0);

            if (age % 2 == 0) {
                SpellFx.ring(level, SpellFx.fade(BRIGHT, DARK, 1.2F), ground, radius, (int) (40 * density) + 4,
                        age * 0.03);
            }
            if (age % 4 == 0) {
                SpellFx.groundStar(level, SpellFx.dust(GREEN, 0.9F), ground, radius * 0.8, 5, 2, -age * 0.02, 0.35);
                SpellFx.ring(level, SpellFx.dust(GREEN, 0.8F), ground, radius * 0.8, 24, -age * 0.02);
            }
            int fog = (int) Math.round(6 * density);
            for (int i = 0; i < fog; i++) {
                Vec3 spot = randomIn(center, radius).add(0, 0.2 + SpellFx.RANDOM.nextDouble() * 0.5, 0);
                SpellFx.at(level, SpellFx.dust(FOG, 2.5F + (float) SpellFx.RANDOM.nextDouble()), spot);
            }
            if (SpellFx.chance(density)) {
                SpellFx.fly(level, effect(GREEN), randomIn(center, radius).add(0, 0.2, 0), new Vec3(0, 1, 0), 0.1);
                SpellFx.fly(level, effect(BRIGHT), randomIn(center, radius).add(0, 0.2, 0), new Vec3(0, 1, 0), 0.1);
            }
            if (age % 3 == 0 && SpellFx.chance(density)) {
                SpellFx.at(level, ParticleTypes.BUBBLE_POP,
                        randomIn(center, radius).add(0, 0.3 + SpellFx.RANDOM.nextDouble() * 0.8, 0));
            }
            if (age % 20 == 5) {
                level.playSound(null, center.x, center.y, center.z, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                        SoundSource.PLAYERS, 0.8F, 0.6F + (float) SpellFx.RANDOM.nextDouble() * 0.3F);
            }
            if (age % PULSE == 0) {
                poison(level, center, radius, owner);
            }
            return age < DURATION;
        };
    }

    private static void poison(ServerLevel level, Vec3 center, double radius, UUID owner) {
        ServerPlayer caster = level.getServer().getPlayerList().getPlayer(owner);
        AABB box = new AABB(center, center).inflate(radius, 0, radius).expandTowards(0, 2.5, 0)
                .expandTowards(0, -1, 0);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (target.getUUID().equals(owner)) {
                continue;
            }
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            if (dx * dx + dz * dz <= radius * radius) {
                target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1), caster);
                // A green haze around every creature that breathes it in.
                SpellFx.send(level, effect(BRIGHT), target.getX(), target.getY() + target.getBbHeight() * 0.6,
                        target.getZ(), 6, target.getBbWidth() * 0.5, target.getBbHeight() * 0.3,
                        target.getBbWidth() * 0.5, 0.0);
            }
        }
    }

    /** A random point on the ground inside the circle, evenly spread. */
    private static Vec3 randomIn(Vec3 center, double radius) {
        double angle = SpellFx.RANDOM.nextDouble() * Math.PI * 2;
        double distance = Math.sqrt(SpellFx.RANDOM.nextDouble()) * radius;
        return center.add(Math.cos(angle) * distance, 0, Math.sin(angle) * distance);
    }
}
