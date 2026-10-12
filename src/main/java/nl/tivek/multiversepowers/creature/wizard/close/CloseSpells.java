package nl.tivek.multiversepowers.creature.wizard.close;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Wizard's short spells, for a foe a few steps off: a pulse of force out of his palm that throws all round him
// back (REPULSE), a cone of frost breathed out of his left hand that freezes and slows (FROST_CONE), a lash of fire
// whipped from his crystal across the ground before him (FLAME_LASH); and the tether, a chain of light from his crystal
// that drags a foe from afar in to his feet (TETHER), where his staff (StaffBlows) and these wait.
public final class CloseSpells {
    private static final double REPULSE_REACH = 4.8;
    private static final double CONE_REACH = 6.5;
    private static final double CONE_HALF = 0.55;
    private static final double LASH_REACH = 5.8;
    private static final double LASH_HALF = 1.25;
    private static final double TETHER_REACH = 20.0;
    // Where a tether drags its foe: this far before him.
    private static final double TETHER_TO = 2.2;

    private CloseSpells() {
    }

    public static void release(ServerLevel level, Wizard wizard, int kind, @Nullable LivingEntity target) {
        switch (kind) {
            case WizardMove.REPULSE -> repulse(level, wizard);
            case WizardMove.FROST_CONE -> breathe(level, wizard, target);
            case WizardMove.FLAME_LASH -> lash(level, wizard, 0);
            case WizardMove.TETHER -> tether(level, wizard, target);
            default -> StaffBlows.release(level, wizard, kind);
        }
    }

    public static void during(ServerLevel level, Wizard wizard, int kind, int t, @Nullable LivingEntity target) {
        int from = WizardMove.release(kind);
        switch (kind) {
            case WizardMove.REPULSE -> {
                if (t < from && t % 2 == 0) {
                    Vec3 at = wizard.hand();
                    ParticleFx.implosion(level, Wizard.dust(Wizard.VIOLET, 1.2F), at, 1.6, 6, 0.25);
                }
            }
            case WizardMove.FROST_CONE -> {
                if (t > from && t <= WizardMove.CONE_STOP && (t - from) % WizardMove.CONE_EVERY == 0) {
                    breathe(level, wizard, target);
                }
                if (t < from) {
                    Vec3 at = wizard.hand();
                    ParticleFx.send(level, ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 3, 0.15, 0.15, 0.15, 0.01);
                }
            }
            case WizardMove.FLAME_LASH -> {
                if (t > from && t <= from + 2) {
                    lash(level, wizard, t - from);
                }
                if (t < from && t % 2 == 0) {
                    Vec3 at = wizard.staffTip();
                    ParticleFx.send(level, ParticleTypes.FLAME, at.x, at.y, at.z, 4, 0.12, 0.12, 0.12, 0.02);
                }
            }
            case WizardMove.TETHER -> {
                if (t > from && t <= from + 6 && target != null && target.isAlive()) {
                    chain(level, wizard.staffTip(), target.getBoundingBox().getCenter());
                }
            }
            default -> StaffBlows.during(level, wizard, kind, t);
        }
    }

    // Force bursts out of him: each foe near is struck, harder the nearer, and thrown back.
    private static void repulse(ServerLevel level, Wizard wizard) {
        Vec3 middle = wizard.position().add(0.0, 1.0 + wizard.hover(), 0.0);
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(middle, middle).inflate(REPULSE_REACH))) {
            Vec3 off = living.getBoundingBox().getCenter().subtract(middle);
            double far = off.length();
            if (far > REPULSE_REACH + living.getBbWidth() * 0.5) {
                continue;
            }
            Vec3 away = new Vec3(off.x, 0.0, off.z);
            away = away.lengthSqr() < 1.0E-4 ? MonsterBlows.ahead(wizard) : away.normalize();
            float harm = wizard.harm(WizardMove.REPULSE) * (float) (0.6 + 0.4 * (1.0 - far / REPULSE_REACH));
            if (MonsterBlows.hurt(living, Wizard.source(wizard, wizard, WizardEntities.ARCANE), harm,
                    WizardKnock.knock(away.scale(1.8).add(0.0, 0.6, 0.0)))) {
                Vec3 hit = living.getBoundingBox().getCenter();
                ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.4F), hit.x, hit.y, hit.z, 10, 0.2,
                        0.3, 0.2, 0.0);
            }
        }
        int light = Wizard.light(wizard.phase());
        ParticleFx.shockwave(level, Wizard.dust(light, 2.0F), middle, 56, 0.75);
        ParticleFx.shockwave(level, Wizard.dust(Wizard.STARLIGHT, 1.4F), middle.add(0.0, -0.8, 0.0), 40, 0.6);
        ParticleFx.shockwave(level, ParticleTypes.END_ROD, middle, 28, 0.5);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, middle, 24, 0.5);
        ParticleFx.groundStar(level, Wizard.dust(light, 1.6F), wizard.position().add(0.0, 0.1, 0.0), 2.6, 5, 2, 0.0,
                0.3);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.6F,
                1.3F);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(),
                SoundSource.HOSTILE, 1.4F, 0.7F);
    }

    // One breath of the frost cone, from his left hand at his foe: each foe inside it is struck, frozen and slowed.
    private static void breathe(ServerLevel level, Wizard wizard, @Nullable LivingEntity target) {
        Vec3 from = wizard.hand();
        Vec3 way = target != null && target.isAlive() ? target.getBoundingBox().getCenter().subtract(from)
                : MonsterBlows.ahead(wizard);
        way = way.lengthSqr() < 1.0E-4 ? MonsterBlows.ahead(wizard) : way.normalize();
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(from, from).inflate(CONE_REACH))) {
            Vec3 off = living.getBoundingBox().getCenter().subtract(from);
            double far = off.length();
            if (far > CONE_REACH + living.getBbWidth() * 0.5 || far < 1.0E-3
                    || Math.acos(Mth.clamp(off.scale(1.0 / far).dot(way), -1.0, 1.0)) > CONE_HALF + 0.3 / far) {
                continue;
            }
            if (living.hurt(Wizard.source(wizard, wizard, WizardEntities.FROST), wizard.harm(WizardMove.FROST_CONE))) {
                living.setTicksFrozen(Math.min(living.getTicksRequiredToFreeze() + 120, living.getTicksFrozen() + 50));
                living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), wizard);
            }
        }
        Vec3[] side = ParticleFx.basis(way);
        for (int i = 0; i < 26; i++) {
            double angle = ParticleFx.RANDOM.nextDouble() * Mth.TWO_PI;
            double spread = Math.sqrt(ParticleFx.RANDOM.nextDouble()) * Math.tan(CONE_HALF);
            Vec3 dir = way.add(side[0].scale(Math.cos(angle) * spread)).add(side[1].scale(Math.sin(angle) * spread))
                    .normalize();
            ParticleFx.fly(level, i % 3 == 0 ? Wizard.dust(Wizard.ICE, 1.3F) : ParticleTypes.SNOWFLAKE,
                    from.add(dir.scale(0.3)), dir, 0.35 + 0.25 * ParticleFx.RANDOM.nextDouble());
        }
        for (int i = 0; i < 6; i++) {
            Vec3 dir = way.add(ParticleFx.spread(0.35), ParticleFx.spread(0.2), ParticleFx.spread(0.35)).normalize();
            ParticleFx.fly(level, ParticleTypes.CLOUD, from.add(dir.scale(0.5)), dir, 0.18);
        }
        level.playSound(null, from.x, from.y, from.z, SoundEvents.POWDER_SNOW_BREAK, SoundSource.HOSTILE, 1.2F,
                0.6F + 0.2F * ParticleFx.RANDOM.nextFloat());
        level.playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_HURT_FREEZE, SoundSource.HOSTILE, 0.5F, 1.6F);
    }

    // The lash of fire sweeps from his right across before him to his left over three ticks (`part` 0 to 2), drawn as
    // it goes; at its middle it strikes every foe in its arc and sets them burning.
    private static void lash(ServerLevel level, Wizard wizard, int part) {
        Vec3 ahead = MonsterBlows.ahead(wizard);
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        Vec3 feet = wizard.position().add(0.0, 0.25 + wizard.hover() * 0.5, 0.0);
        double a0 = LASH_HALF - part * 2.0 * LASH_HALF / 3.0;
        double a1 = a0 - 2.0 * LASH_HALF / 3.0;
        for (int i = 0; i <= 10; i++) {
            double a = Mth.lerp(i / 10.0, a0, a1);
            Vec3 way = ahead.scale(Math.cos(a)).add(right.scale(Math.sin(a)));
            for (double r = 1.2; r <= LASH_REACH; r += 0.9) {
                Vec3 at = feet.add(way.scale(r)).add(0.0, 0.35 * Math.sin(r / LASH_REACH * Math.PI), 0.0);
                ParticleFx.send(level, ParticleTypes.FLAME, at.x, at.y, at.z, 1, 0.08, 0.1, 0.08, 0.02);
                if (i % 2 == 0) {
                    ParticleFx.send(level, Wizard.dust(Wizard.FIRE, 1.6F), at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0.0);
                }
            }
        }
        if (part == 0) {
            Vec3 tip = wizard.staffTip();
            level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.4F, 0.6F);
            level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.2F, 0.6F);
            return;
        }
        if (part != 1) {
            return;
        }
        for (LivingEntity living : MonsterBlows.foes(wizard, wizard.getBoundingBox().inflate(LASH_REACH, 2.0,
                LASH_REACH))) {
            Vec3 off = new Vec3(living.getX() - feet.x, 0.0, living.getZ() - feet.z);
            double far = off.length() - living.getBbWidth() * 0.5;
            if (far > LASH_REACH || living.getY() > feet.y + 2.4) {
                continue;
            }
            Vec3 way = off.lengthSqr() < 1.0E-4 ? ahead : off.normalize();
            if (Math.acos(Mth.clamp(way.dot(ahead), -1.0, 1.0)) > LASH_HALF + 0.15) {
                continue;
            }
            if (MonsterBlows.hurt(living, Wizard.source(wizard, wizard, WizardEntities.INFERNO),
                    wizard.harm(WizardMove.FLAME_LASH), WizardKnock.knock(way.scale(0.9).add(0.0, 0.35, 0.0)))) {
                living.igniteForSeconds(4.0F);
                Vec3 hit = living.getBoundingBox().getCenter();
                ParticleFx.send(level, ParticleTypes.LAVA, hit.x, hit.y, hit.z, 4, 0.2, 0.3, 0.2, 0.0);
            }
        }
    }

    // A chain of light from his crystal to his foe drags it in to his feet.
    private static void tether(ServerLevel level, Wizard wizard, @Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || wizard.distanceTo(target) > TETHER_REACH
                || !wizard.hasLineOfSight(target)) {
            return;
        }
        Vec3 tip = wizard.staffTip();
        chain(level, tip, target.getBoundingBox().getCenter());
        target.hurt(Wizard.source(wizard, wizard, WizardEntities.ARCANE), wizard.harm(WizardMove.TETHER));
        Vec3 to = wizard.position().add(MonsterBlows.ahead(wizard).scale(TETHER_TO));
        Vec3 pull = to.subtract(target.position());
        double far = pull.length();
        if (far > 0.5) {
            Vec3 flat = new Vec3(pull.x, 0.0, pull.z);
            double speed = Math.min(2.4, 0.42 + far * 0.19);
            target.setDeltaMovement(flat.normalize().scale(speed).add(0.0, Math.min(0.9, 0.32 + far * 0.02)
                    + Math.max(0.0, pull.y * 0.12), 0.0));
            target.hasImpulse = true;
            target.hurtMarked = true;
        }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1), wizard);
        Vec3 at = target.getBoundingBox().getCenter();
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.VIOLET, 1.4F), at, 16, 0.25);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.CHAIN_BREAK, SoundSource.HOSTILE, 1.6F, 0.7F);
        level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.HOSTILE, 1.2F,
                1.3F);
    }

    // The chain: links of violet light along a line that sags a little, sparks along it.
    private static void chain(ServerLevel level, Vec3 from, Vec3 to) {
        Vec3 off = to.subtract(from);
        double far = off.length();
        int links = Math.max(4, (int) (far * 2.5));
        for (int i = 0; i <= links; i++) {
            double f = i / (double) links;
            Vec3 at = from.add(off.scale(f)).add(0.0, -0.6 * Math.sin(f * Math.PI) * Math.min(1.0, far / 10.0), 0.0);
            ParticleFx.send(level, Wizard.dust(i % 3 == 0 ? Wizard.STARLIGHT : Wizard.VIOLET, 1.1F), at.x, at.y, at.z,
                    1, 0.02, 0.02, 0.02, 0.0);
        }
        ParticleFx.zigzag(level, ParticleTypes.ELECTRIC_SPARK, from, to, Math.max(3, (int) far), 0.25, 0.6);
    }
}
