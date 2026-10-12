package nl.tivek.multiversepowers.creature.wizard;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.spell.WardCrystal;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The ward each phase brings round the Wizard: a bubble of his phase's light that takes every blow meant for him until
// it breaks. Three crystals circle him while it holds (spell/WardCrystal): while they are whole only a share of each blow
// reaches the ward, more for every crystal broken, and each crystal that breaks takes a share of the ward with it. When
// the ward breaks it bursts into shards and he reels, stunned (WizardMoves.reel); he can be harmed until his next phase
// brings a new one.
public final class WizardWard {
    public static final int CRYSTALS = 3;
    private static final float[] STRENGTH = { 0.0F, 25.5F, 31.9F, 38.3F, 44.7F, 51.0F };
    // An arrow strikes the ward for this share of its harm, crystals or not, and breaks on it.
    static final float ARROW = 0.6F;
    // The share of a blow that reaches the ward with every crystal whole (with none it takes the whole blow).
    private static final float SOFT = 0.3F;
    // The share of the whole ward a crystal takes with it when it breaks.
    private static final float CRYSTAL_SHARE = 0.15F;

    private WizardWard() {
    }

    static float strength(int phase) {
        return STRENGTH[Mth.clamp(phase, 1, WizardMove.PHASES)];
    }

    // A new ward and three new crystals round him.
    static void raise(ServerLevel level, Wizard wizard) {
        drop(level, wizard, false);
        wizard.setWard(strength(wizard.phase()), CRYSTALS);
        WardCrystal.ring(level, wizard, CRYSTALS);
        Vec3 at = middle(wizard);
        int light = Wizard.light(wizard.phase());
        ParticleFx.sphere(level, Wizard.dust(light, 1.6F), at, 1.8, 60, 0.0);
        ParticleFx.sphereOut(level, ParticleTypes.END_ROD, at, 24, 0.12);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.3F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F, 0.7F);
    }

    // Loaded with his ward up: the crystals that were still whole are set round him again.
    static void restore(ServerLevel level, Wizard wizard) {
        WardCrystal.ring(level, wizard, wizard.crystals());
    }

    // A blow meant for him reaches his ward. An arrow breaks on it instead of glancing off (a refused blow sends it
    // back) and counts however soon after another it comes; else, as his health does, the ward takes a blow at most
    // twice a second, or only what a harder one adds.
    static void struck(ServerLevel level, Wizard wizard, DamageSource source, float amount,
            @Nullable WizardMoves moves) {
        boolean arrow = source.getDirectEntity() instanceof AbstractArrow;
        if (arrow) {
            Vec3 at = source.getDirectEntity().position();
            ParticleFx.send(level, ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.1, 0.1, 0.1, 0.2);
            source.getDirectEntity().discard();
        }
        boolean fresh = arrow || wizard.tickCount - wizard.wardHitAt >= 10;
        float taken = fresh ? amount : amount - wizard.wardHit;
        if (taken <= 0.0F) {
            return;
        }
        if (fresh) {
            wizard.wardHitAt = wizard.tickCount;
        }
        wizard.wardHit = amount;
        if (source.getEntity() instanceof LivingEntity living) {
            wizard.setLastHurtByMob(living);
        }
        absorb(level, wizard, taken, source, moves);
    }

    // A blow meant for him strikes his ward instead.
    private static void absorb(ServerLevel level, Wizard wizard, float amount, DamageSource source,
            @Nullable WizardMoves moves) {
        boolean arrow = source.getDirectEntity() instanceof AbstractArrow;
        float share = arrow ? ARROW : SOFT + (1.0F - SOFT) * (CRYSTALS - wizard.crystals()) / CRYSTALS;
        float left = wizard.ward() - amount * share;
        Vec3 at = middle(wizard);
        Vec3 from = source.getSourcePosition();
        Vec3 hit = from == null ? at : at.add(from.subtract(at).normalize().scale(1.6));
        ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.4F), hit.x, hit.y, hit.z, 10, 0.2, 0.2,
                0.2, 0.0);
        ParticleFx.send(level, ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 6, 0.1, 0.1, 0.1, 0.15);
        level.playSound(null, hit.x, hit.y, hit.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.6F,
                0.6F + 0.8F * left / strength(wizard.phase()));
        if (left <= 0.0F) {
            shatter(level, wizard, moves);
        } else {
            wizard.setWard(left, wizard.crystals());
        }
    }

    // One of the crystals broke: the ward is weaker for it, and lets more of every blow through.
    static void crystalBroke(ServerLevel level, Wizard wizard, @Nullable WizardMoves moves) {
        if (wizard.ward() <= 0.0F) {
            return;
        }
        int crystals = Math.max(0, wizard.crystals() - 1);
        float left = wizard.ward() - strength(wizard.phase()) * CRYSTAL_SHARE;
        Vec3 at = middle(wizard);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WITHER_HURT, SoundSource.HOSTILE, 0.8F, 1.6F);
        if (left <= 0.0F) {
            shatter(level, wizard, moves);
        } else {
            wizard.setWard(left, crystals);
        }
    }

    // The ward bursts into shards, the crystals still whole with it, and he reels.
    private static void shatter(ServerLevel level, Wizard wizard, @Nullable WizardMoves moves) {
        wizard.setWard(0.0F, 0);
        drop(level, wizard, true);
        Vec3 at = middle(wizard);
        int light = Wizard.light(wizard.phase());
        ParticleFx.sphereOut(level, Wizard.dust(light, 2.0F), at, 90, 0.55);
        ParticleFx.sphereOut(level, ParticleTypes.END_ROD, at, 40, 0.4);
        ParticleFx.shockwave(level, Wizard.dust(Wizard.STARLIGHT, 1.8F), at, 48, 0.7);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 30, 0.5);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.5F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.5F, 0.4F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 1.0F, 1.4F);
        if (moves != null) {
            moves.reel();
        }
    }

    // His crystals are gone (bursting, or quietly as he dies).
    static void drop(ServerLevel level, Wizard wizard, boolean burst) {
        WardCrystal.dropAll(level, wizard, burst);
        if (!burst) {
            wizard.setWard(0.0F, 0);
        }
    }

    private static Vec3 middle(Wizard wizard) {
        return wizard.position().add(0.0, 1.0 + wizard.hover(), 0.0);
    }
}
