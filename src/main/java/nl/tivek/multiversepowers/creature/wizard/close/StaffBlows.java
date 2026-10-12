package nl.tivek.multiversepowers.creature.wizard.close;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The blows of the Wizard's staff: the combo (an overhead smash, a backhand sweep, then a rising thrust whose crystal
// bursts in their face) and the whirl (the staff swung round him at arm's length, striking all round him again and
// again); his thrust and sweep (STRIKE) land here too.
public final class StaffBlows {
    private static final double SMASH_REACH = 3.2;
    private static final double SWEEP_REACH = 3.5;
    private static final double THRUST_REACH = 3.8;
    private static final double WHIRL_REACH = 3.5;
    // The thrust's crystal bursts this much harder than a blow.
    private static final float THRUST_HARM = 1.6F;

    private StaffBlows() {
    }

    static void release(ServerLevel level, Wizard wizard, int kind) {
        if (kind == WizardMove.COMBO) {
            smash(level, wizard);
        } else if (kind == WizardMove.WHIRL) {
            whirl(level, wizard, 0);
        }
    }

    static void during(ServerLevel level, Wizard wizard, int kind, int t) {
        if (kind == WizardMove.COMBO) {
            if (t == WizardMove.COMBO_SWEEP) {
                strike(level, wizard, SWEEP_REACH, 1.9, wizard.harm(WizardMove.COMBO), 1.0, 0.3);
            } else if (t == WizardMove.COMBO_THRUST) {
                thrust(level, wizard);
            } else if (t == WizardMove.COMBO_THRUST - 6 || t == WizardMove.COMBO_SMASH - 5) {
                Vec3 tip = wizard.staffTip();
                ParticleFx.implosion(level, Wizard.dust(Wizard.light(wizard.phase()), 1.2F), tip, 1.2, 8, 0.2);
            }
        } else if (kind == WizardMove.WHIRL) {
            int from = WizardMove.release(WizardMove.WHIRL);
            if (t > from && t <= WizardMove.WHIRL_STOP && (t - from) % WizardMove.WHIRL_EVERY == 0) {
                whirl(level, wizard, (t - from) / WizardMove.WHIRL_EVERY);
            }
        }
    }

    // A blow on whoever stands within `reach` before him, within `half` radians either side, thrown along `push` and
    // `up` as far as he throws (Wizard.knock). Whether it struck any.
    public static boolean strike(ServerLevel level, Wizard wizard, double reach, double half, float damage,
            double push, double up) {
        Vec3 ahead = MonsterBlows.ahead(wizard);
        Vec3 from = wizard.position();
        boolean struck = false;
        for (LivingEntity living : MonsterBlows.foes(wizard, wizard.getBoundingBox().inflate(reach, 1.0, reach))) {
            Vec3 off = new Vec3(living.getX() - from.x, 0.0, living.getZ() - from.z);
            double far = off.length() - living.getBbWidth() * 0.5;
            if (far > reach) {
                continue;
            }
            Vec3 way = off.lengthSqr() < 1.0E-4 ? ahead : off.normalize();
            if (Math.acos(Mth.clamp(way.dot(ahead), -1.0, 1.0)) > half) {
                continue;
            }
            if (MonsterBlows.hurt(living, wizard.damageSources().mobAttack(wizard), damage,
                    WizardKnock.knock(way.scale(push).add(0.0, up, 0.0)))) {
                struck = true;
                Vec3 hit = living.getBoundingBox().getCenter();
                ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.4F), hit.x, hit.y, hit.z, 12, 0.2,
                        0.3, 0.2, 0.0);
                ParticleFx.send(level, ParticleTypes.CRIT, hit.x, hit.y, hit.z, 8, 0.2, 0.2, 0.2, 0.3);
            }
        }
        Vec3 swing = from.add(ahead.scale(1.4)).add(0.0, 1.1 + wizard.hover(), 0.0);
        ParticleFx.at(level, ParticleTypes.SWEEP_ATTACK, swing);
        wizard.playSound(half > 1.0 ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.7F);
        return struck;
    }

    // The staff brought down overhead: a narrow blow that cracks the ground before him.
    private static void smash(ServerLevel level, Wizard wizard) {
        strike(level, wizard, SMASH_REACH, 0.8, wizard.harm(WizardMove.COMBO), 0.5, 0.1);
        Vec3 ground = wizard.position().add(MonsterBlows.ahead(wizard).scale(2.0)).add(0.0, 0.15 + wizard.hover(),
                0.0);
        ParticleFx.ring(level, Wizard.dust(Wizard.light(wizard.phase()), 1.6F), ground, 1.0, 16, 0.0);
        ParticleFx.send(level, ParticleTypes.CRIT, ground.x, ground.y, ground.z, 10, 0.4, 0.05, 0.4, 0.25);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6F, 1.5F);
    }

    // The rising thrust: his crystal driven at their chest bursts in light, throwing them up and back.
    private static void thrust(ServerLevel level, Wizard wizard) {
        strike(level, wizard, THRUST_REACH, 0.55, wizard.harm(WizardMove.COMBO) * THRUST_HARM, 1.2, 0.8);
        Vec3 burst = wizard.position().add(MonsterBlows.ahead(wizard).scale(2.2)).add(0.0, 1.5 + wizard.hover(),
                0.0);
        int light = Wizard.light(wizard.phase());
        ParticleFx.sphereOut(level, Wizard.dust(light, 1.8F), burst, 30, 0.35);
        ParticleFx.sphereOut(level, ParticleTypes.END_ROD, burst, 14, 0.25);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, burst, 16, 0.4);
        level.playSound(null, burst.x, burst.y, burst.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE,
                1.6F, 0.8F);
        level.playSound(null, burst.x, burst.y, burst.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2F,
                1.4F);
    }

    // One turn of the whirl (the `turn`th): the staff strikes all round him, a ring of light trailing it.
    private static void whirl(ServerLevel level, Wizard wizard, int turn) {
        strike(level, wizard, WHIRL_REACH, Math.PI, wizard.harm(WizardMove.WHIRL), 0.7, 0.25);
        Vec3 middle = wizard.position().add(0.0, 1.0 + wizard.hover(), 0.0);
        int light = Wizard.light(wizard.phase());
        ParticleFx.ring(level, Wizard.dust(light, 1.5F), middle, 2.6, 28, turn * 0.7);
        ParticleFx.ring(level, ParticleTypes.END_ROD, middle.add(0.0, 0.2, 0.0), 2.9, 10, turn * 0.7 + 0.3);
        for (int k = 0; k < 4; k++) {
            double angle = turn * 1.1 + k * Math.PI / 2.0;
            ParticleFx.at(level, ParticleTypes.SWEEP_ATTACK, middle.add(Math.cos(angle) * 1.9, 0.0,
                    Math.sin(angle) * 1.9));
        }
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE,
                1.0F, 0.8F + 0.12F * turn);
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.BREEZE_IDLE_GROUND, SoundSource.HOSTILE, 0.6F,
                1.4F);
    }
}
