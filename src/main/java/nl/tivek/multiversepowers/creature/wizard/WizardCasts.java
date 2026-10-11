package nl.tivek.multiversepowers.creature.wizard;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.field.ArcaneChains;
import nl.tivek.multiversepowers.creature.wizard.field.Blizzard;
import nl.tivek.multiversepowers.creature.wizard.field.GravityWell;
import nl.tivek.multiversepowers.creature.wizard.field.RuneMine;
import nl.tivek.multiversepowers.creature.wizard.field.StarRift;
import nl.tivek.multiversepowers.creature.wizard.field.TimeWarp;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcLightning;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBeam;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBolt;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;
import nl.tivek.multiversepowers.creature.wizard.spell.GreatMeteor;
import nl.tivek.multiversepowers.creature.wizard.spell.WizardFireball;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// What each of the Wizard's moves does on the server at its moments (WizardMove): the sound as he begins, the magic he
// lets go at its release, and what follows during it (the sweep after the thrust, his second and third bolt, the slow
// turn of his beam).
final class WizardCasts {
    private static final double JAB_REACH = 2.9;
    private static final double SWEEP_REACH = 3.3;
    // How far a phase bursting out of him throws whoever stands near.
    private static final double BURST_REACH = 9.0;

    private WizardCasts() {
    }

    static void begin(ServerLevel level, Wizard wizard, int kind) {
        SoundEvent sound = switch (kind) {
            case WizardMove.STRIKE -> SoundEvents.PLAYER_ATTACK_NODAMAGE;
            case WizardMove.SPARK -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case WizardMove.FIREBALL, WizardMove.METEOR -> SoundEvents.BLAZE_AMBIENT;
            case WizardMove.BLIZZARD -> SoundEvents.EVOKER_PREPARE_WOLOLO;
            case WizardMove.BLINK -> SoundEvents.ILLUSIONER_PREPARE_MIRROR;
            case WizardMove.STARFALL, WizardMove.FAMILIARS, WizardMove.MIRRORS -> SoundEvents.EVOKER_PREPARE_SUMMON;
            case WizardMove.LIGHTNING -> SoundEvents.BEACON_POWER_SELECT;
            case WizardMove.BEAM, WizardMove.WARP -> SoundEvents.BEACON_ACTIVATE;
            case WizardMove.ASCEND, WizardMove.UNBIND -> SoundEvents.WITHER_AMBIENT;
            case WizardMove.STUNNED -> SoundEvents.ZOMBIE_VILLAGER_CURE;
            default -> SoundEvents.EVOKER_PREPARE_ATTACK;
        };
        float pitch = kind == WizardMove.STRIKE ? 0.6F : kind == WizardMove.SPARK ? 1.6F : 0.7F;
        wizard.playSound(sound, kind == WizardMove.ASCEND || kind == WizardMove.UNBIND ? 2.5F : 1.3F, pitch);
        if (kind == WizardMove.ASCEND || kind == WizardMove.UNBIND) {
            wizard.removeAllEffects();
            wizard.clearFire();
        }
    }

    static void release(ServerLevel level, Wizard wizard, int kind, @Nullable LivingEntity target, Vec3 aim) {
        switch (kind) {
            case WizardMove.STRIKE -> strike(level, wizard, JAB_REACH, 0.6, wizard.harm(WizardMove.STRIKE), 0.5,
                    0.15);
            case WizardMove.SPARK -> {
                if (target != null) {
                    ArcaneBolt.spark(level, wizard, wizard.staffTip(), target, wizard.harm(WizardMove.SPARK));
                }
            }
            case WizardMove.BOLT -> {
                if (target != null) {
                    ArcaneBolt.cast(level, wizard, target, wizard.harm(WizardMove.BOLT));
                }
            }
            case WizardMove.FIREBALL -> {
                if (target != null) {
                    WizardFireball.cast(level, wizard, target, wizard.harm(WizardMove.FIREBALL));
                }
            }
            case WizardMove.BLIZZARD -> Blizzard.call(level, wizard, ground(level, aim),
                    wizard.harm(WizardMove.BLIZZARD));
            case WizardMove.BLINK -> vanish(level, wizard, target);
            case WizardMove.CHAINS -> ArcaneChains.cast(level, wizard, ground(level, aim),
                    wizard.harm(WizardMove.CHAINS), 40 + 10 * wizard.phase());
            case WizardMove.LIGHTNING -> {
                if (target != null) {
                    ArcLightning.strike(level, wizard, target, wizard.harm(WizardMove.LIGHTNING),
                            2 + wizard.phase());
                }
            }
            case WizardMove.STARFALL -> StarRift.open(level, wizard, ground(level, aim),
                    wizard.harm(WizardMove.STARFALL), wizard.phase() > 2 ? 4 : 5);
            case WizardMove.RUNES -> {
                if (target != null) {
                    RuneMine.ring(level, wizard, ground(level, target.position()), wizard.harm(WizardMove.RUNES));
                }
            }
            case WizardMove.LIFT -> {
                if (target != null) {
                    GravityWell.lift(level, wizard, target, wizard.harm(WizardMove.LIFT));
                }
            }
            case WizardMove.FAMILIARS -> ArcaneFamiliar.summon(level, wizard, 3);
            case WizardMove.BEAM -> ArcaneBeam.fire(level, wizard, wizard.harm(WizardMove.BEAM),
                    WizardMove.BEAM_STOP - WizardMove.release(WizardMove.BEAM));
            case WizardMove.METEOR -> GreatMeteor.call(level, wizard, ground(level, aim),
                    wizard.harm(WizardMove.METEOR));
            case WizardMove.WARP -> TimeWarp.cast(level, wizard);
            case WizardMove.ASCEND -> burst(level, wizard, 2);
            case WizardMove.UNBIND -> burst(level, wizard, 3);
            default -> {
            }
        }
    }

    static void during(ServerLevel level, Wizard wizard, int kind, int t, @Nullable LivingEntity target) {
        switch (kind) {
            case WizardMove.STRIKE -> {
                if (t == WizardMove.SWEEP) {
                    strike(level, wizard, SWEEP_REACH, 1.9, (float) (wizard.harm(WizardMove.STRIKE)
                            * WizardMove.SWEEP_HARM), 1.1, 0.4);
                }
            }
            case WizardMove.BOLT -> {
                int extra = t - WizardMove.release(WizardMove.BOLT);
                if (target != null && extra > 0 && extra % WizardMove.BOLT_AGAIN == 0
                        && extra / WizardMove.BOLT_AGAIN < wizard.phase()) {
                    ArcaneBolt.cast(level, wizard, target, wizard.harm(WizardMove.BOLT));
                }
            }
            case WizardMove.BEAM -> {
                // The beam follows where he looks, and he turns after them only slowly.
                if (target != null) {
                    wizard.getLookControl().setLookAt(target, 3.0F, 3.0F);
                }
            }
            case WizardMove.ASCEND, WizardMove.UNBIND -> {
                Vec3 at = wizard.position().add(0.0, 1.2, 0.0);
                if (t < WizardMove.BURST && t % 3 == 0) {
                    ParticleFx.implosion(level, Wizard.dust(Wizard.light(kind == WizardMove.ASCEND ? 2 : 3), 1.6F),
                            at, 5.0, 10, 0.5);
                }
                if (t == WizardMove.BURST - 20) {
                    wizard.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.5F);
                }
            }
            default -> {
            }
        }
    }

    // A blow of the staff on whoever stands within `reach` before him, within `half` radians either side.
    private static void strike(ServerLevel level, Wizard wizard, double reach, double half, float damage, double push,
            double up) {
        Vec3 ahead = MonsterBlows.ahead(wizard);
        Vec3 from = wizard.position();
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
                    way.scale(push).add(0.0, up, 0.0))) {
                Vec3 hit = living.getBoundingBox().getCenter();
                ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.4F), hit.x, hit.y, hit.z, 12, 0.2,
                        0.3, 0.2, 0.0);
                ParticleFx.send(level, ParticleTypes.CRIT, hit.x, hit.y, hit.z, 8, 0.2, 0.2, 0.2, 0.3);
            }
        }
        Vec3 swing = from.add(ahead.scale(1.4)).add(0.0, 1.1 + wizard.hover(), 0.0);
        ParticleFx.at(level, ParticleTypes.SWEEP_ATTACK, swing);
        wizard.playSound(half > 1.0 ? SoundEvents.PLAYER_ATTACK_SWEEP : SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.7F);
    }

    // The ground under (or a little below) `near`.
    static Vec3 ground(ServerLevel level, Vec3 near) {
        BlockPos column = BlockPos.containing(near);
        int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX(), column.getZ());
        return new Vec3(near.x, Math.min(near.y, top), near.z);
    }

    // A decoy of himself left standing where he was (two from his second phase), and he is somewhere else, still in
    // sight of them.
    private static void vanish(ServerLevel level, Wizard wizard, @Nullable LivingEntity target) {
        Vec3 from = wizard.position();
        WizardDecoy.leave(level, wizard, from);
        Vec3 middle = target == null ? from : target.position();
        if (wizard.phase() > 1 && target != null) {
            Vec3 across = middle.add(middle.subtract(from).multiply(1.0, 0.0, 1.0));
            WizardDecoy.leave(level, wizard, ground(level, across));
        }
        teleportNear(wizard, middle, target, from);
        WizardSparks.blink(level, from, wizard.position());
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.HOSTILE, 1.2F,
                0.9F);
    }

    // Somewhere 9 to 14 blocks from `middle`, in sight of `target` if he can.
    static void teleportNear(Wizard wizard, Vec3 middle, @Nullable LivingEntity target, Vec3 from) {
        boolean moved = false;
        for (int tries = 0; tries < 16 && !moved; tries++) {
            double angle = wizard.getRandom().nextDouble() * Mth.TWO_PI;
            double far = 9.0 + wizard.getRandom().nextDouble() * 5.0;
            Vec3 to = middle.add(Math.cos(angle) * far, 4.0, Math.sin(angle) * far);
            if (to.distanceTo(from) < 7.0) {
                continue;
            }
            moved = wizard.randomTeleport(to.x, to.y, to.z, false);
            if (moved && target != null && !wizard.hasLineOfSight(target) && tries < 12) {
                wizard.teleportTo(from.x, from.y, from.z);
                moved = false;
            }
        }
    }

    // A phase bursting out of him: whoever stands near is thrown back, and he is the next phase.
    private static void burst(ServerLevel level, Wizard wizard, int phase) {
        wizard.setPhase(phase);
        Vec3 at = wizard.position().add(0.0, 1.0, 0.0);
        for (LivingEntity living : MonsterBlows.foes(wizard, wizard.getBoundingBox().inflate(BURST_REACH))) {
            Vec3 away = living.position().subtract(wizard.position()).multiply(1.0, 0.0, 1.0);
            double far = away.length();
            if (far > BURST_REACH) {
                continue;
            }
            away = far < 1.0E-3 ? MonsterBlows.ahead(wizard) : away.scale(1.0 / far);
            double strength = 1.6 * (1.0 - far / BURST_REACH) + 0.4;
            living.setDeltaMovement(away.scale(strength).add(0.0, 0.5, 0.0));
            living.hurtMarked = true;
        }
        int light = Wizard.light(phase);
        ParticleFx.shockwave(level, Wizard.dust(light, 2.5F), at, 64, 0.9);
        ParticleFx.shockwave(level, ParticleTypes.END_ROD, at, 40, 0.7);
        ParticleFx.sphereOut(level, phase == 3 ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.REVERSE_PORTAL, at, 80,
                0.6);
        ParticleFx.at(level, ParticleTypes.FLASH, at);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.4F, 1.4F);
    }
}
