package nl.tivek.multiversepowers.creature.wizard;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// What the world sees of the Wizard's death on the server's side, tick by tick (WizardMove.DEATH; the body's own pose is
// drawn by each game): light leaks from him as he sinks to his knees, cracks of it break out of his robe, a flash as it
// bursts free and lifts him, a stream of it rising off him, the light drawn into him once more, and he is gone in a
// last flash and a shower of stars.
final class WizardDeath {
    // How high the light lifts him before he is gone (blocks), as his pose shows it.
    private static final double RISE = 1.4;

    private WizardDeath() {
    }

    static void tick(ServerLevel level, Wizard wizard, int t) {
        WizardFlight.sink(wizard);
        int light = Wizard.light(wizard.phase());
        double lifted = t <= WizardMove.BREAK ? 0.0
                : RISE * Math.min(1.0, (t - WizardMove.BREAK) / (double) (WizardMove.GONE - 10 - WizardMove.BREAK));
        Vec3 body = wizard.position().add(0.0, 1.0 + lifted, 0.0);
        if (t == 1) {
            level.playSound(null, body.x, body.y, body.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F,
                    0.6F);
        }
        if (t < WizardMove.BREAK && t % 3 == 0) {
            double strength = t / (double) WizardMove.BREAK;
            ParticleFx.send(level, Wizard.dust(light, 1.0F + (float) strength), body.x, body.y, body.z,
                    2 + (int) (strength * 6), 0.3, 0.6, 0.3, 0.0);
        }
        if (t > 26 && t < WizardMove.BREAK && t % 2 == 0) {
            ParticleFx.send(level, ParticleTypes.END_ROD, body.x, body.y, body.z, 3, 0.25, 0.5, 0.25, 0.08);
        }
        if (t == 40) {
            level.playSound(null, body.x, body.y, body.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE,
                    2.0F, 0.5F);
        }
        if (t == WizardMove.BREAK) {
            ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, body, 30, 0.6);
            ParticleFx.shockwave(level, Wizard.dust(Wizard.STARLIGHT, 2.2F), wizard.position().add(0.0, 0.2, 0.0), 64,
                    0.8);
            ParticleFx.sphereOut(level, ParticleTypes.END_ROD, body, 40, 0.35);
            level.playSound(null, body.x, body.y, body.z, SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 1.6F, 0.5F);
            level.playSound(null, body.x, body.y, body.z, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 2.0F,
                    0.5F);
        }
        if (t > WizardMove.BREAK && t < WizardMove.GONE) {
            ParticleFx.send(level, Wizard.dust(light, 1.6F), body.x, body.y, body.z, 4, 0.35, 0.7, 0.35, 0.0);
            ParticleFx.fly(level, ParticleTypes.END_ROD, body.add((wizard.getRandom().nextDouble() - 0.5) * 0.8, 0.0,
                    (wizard.getRandom().nextDouble() - 0.5) * 0.8), new Vec3(0.0, 1.0, 0.0), 0.25);
        }
        if (t >= WizardMove.GONE - 14 && t < WizardMove.GONE && t % 2 == 0) {
            ParticleFx.implosion(level, Wizard.dust(Wizard.STARLIGHT, 1.5F), body, 3.5, 10, 0.4);
        }
        if (t == WizardMove.GONE - 12) {
            level.playSound(null, body.x, body.y, body.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2.0F,
                    1.4F);
        }
        if (t == WizardMove.GONE) {
            ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, body, 40, 0.7);
            ParticleFx.sphereOut(level, Wizard.dust(light, 2.4F), body, 90, 0.6);
            ParticleFx.sphereOut(level, ParticleTypes.END_ROD, body, 60, 0.5);
            ParticleFx.send(level, ParticleTypes.FIREWORK, body.x, body.y, body.z, 40, 0.6, 0.8, 0.6, 0.15);
            level.playSound(null, body.x, body.y, body.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE,
                    1.2F, 1.5F);
            level.playSound(null, body.x, body.y, body.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE,
                    2.5F, 0.5F);
        }
    }
}
