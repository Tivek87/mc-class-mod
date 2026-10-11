package nl.tivek.multiversepowers.character.greenlantern.minion;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A sentinel that kills something feeds on it: a small ball of light rises out of the body and flies into the
// sentinel's chest, faster and faster, and mends it by the mech's setting as it strikes home.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class SentinelFeed {
    // Blocks a tick the ball starts at and gains every tick, and the most ticks it flies before it gives up.
    private static final double START = 0.15;
    private static final double GAIN = 0.05;
    private static final int LONGEST = 80;

    private SentinelFeed() {
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(event.getSource().getEntity() instanceof MechMinion sentinel) || victim == sentinel
                || !(victim.level() instanceof ServerLevel level)) {
            return;
        }
        double heal = GameCharacter.GREEN_LANTERN.byName("mech").value("mechHelperKillHeal");
        if (heal > 0.0) {
            Effects.start(level, new Ball(sentinel, victim.getBoundingBox().getCenter(), (float) heal));
        }
    }

    private static final class Ball implements Effect {
        private final MechMinion sentinel;
        private final float heal;
        private Vec3 at;

        private Ball(MechMinion sentinel, Vec3 from, float heal) {
            this.sentinel = sentinel;
            this.at = from;
            this.heal = heal;
        }

        @Override
        public boolean tick(ServerLevel level, int age) {
            if (!this.sentinel.isAlive() || this.sentinel.level() != level || age > LONGEST) {
                return false;
            }
            Vec3 chest = this.sentinel.position().add(0.0, this.sentinel.getBbHeight() * 0.62, 0.0);
            Vec3 to = chest.subtract(this.at);
            // Up out of the body first, then straight at the chest.
            Vec3 rise = age < 6 ? new Vec3(0.0, 0.12 * (6 - age), 0.0) : Vec3.ZERO;
            double step = START + GAIN * age;
            Vec3 next = to.length() <= step ? chest : this.at.add(to.normalize().scale(step)).add(rise);
            ParticleFx.line(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), this.at, next, 0.2);
            ParticleFx.cloud(level, ParticleFx.dust(0xE8FFEE, 1.3F), next, 3, 0.06, 0.0);
            ParticleFx.at(level, ParticleTypes.END_ROD, next);
            this.at = next;
            if (next.distanceToSqr(chest) > 0.25) {
                return true;
            }
            this.sentinel.heal(this.heal);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), chest, 18, 0.5, 0.05);
            ParticleFx.cloud(level, ParticleTypes.HAPPY_VILLAGER, chest, 6, 0.6, 0.0);
            level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL,
                    1.2F, 1.4F);
            level.playSound(null, chest.x, chest.y, chest.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.NEUTRAL,
                    0.6F, 1.8F);
            return false;
        }
    }
}
