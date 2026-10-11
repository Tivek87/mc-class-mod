package nl.tivek.multiversepowers.creature.wizard;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.abyss.SoulDrain;
import nl.tivek.multiversepowers.creature.wizard.abyss.Supernova;
import nl.tivek.multiversepowers.creature.wizard.abyss.VoidOrb;
import nl.tivek.multiversepowers.creature.wizard.abyss.VoidTendrils;
import nl.tivek.multiversepowers.creature.wizard.field.ArcaneChains;
import nl.tivek.multiversepowers.creature.wizard.field.ArcaneTornado;
import nl.tivek.multiversepowers.creature.wizard.field.Blizzard;
import nl.tivek.multiversepowers.creature.wizard.field.FrostNova;
import nl.tivek.multiversepowers.creature.wizard.field.GravityWell;
import nl.tivek.multiversepowers.creature.wizard.field.InfernoRing;
import nl.tivek.multiversepowers.creature.wizard.field.RuneMine;
import nl.tivek.multiversepowers.creature.wizard.field.StarRift;
import nl.tivek.multiversepowers.creature.wizard.field.TimeWarp;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcLightning;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBeam;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBolt;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneFamiliar;
import nl.tivek.multiversepowers.creature.wizard.spell.GreatMeteor;
import nl.tivek.multiversepowers.creature.wizard.spell.PrismLance;
import nl.tivek.multiversepowers.creature.wizard.spell.WizardFireball;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// What each of the Wizard's moves does on the server at its moments (WizardMove): the sound as he begins, the magic he
// lets go at its release, and what follows during it (the sweep after the thrust, his second and third bolt, the slow
// turn of his beam, the trail of a skip, the star spiral's volleys, the supernova kindled once he has risen, his slow
// descent out of his rift and the blow of his landing). How he moves in the air is WizardFlight's.
final class WizardCasts {
    private static final double JAB_REACH = 2.9;
    private static final double SWEEP_REACH = 3.3;
    // How far a phase bursting out of him throws whoever stands near.
    private static final double BURST_REACH = 9.0;
    // How high over the ground he comes out of his rift, and how far his landing throws.
    static final double ARRIVE_HEIGHT = 8.0;
    private static final double LAND_REACH = 6.0;
    // The star spiral lets go a volley every this many ticks.
    private static final int SPIRAL_EVERY = 2;

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
            case WizardMove.ASCEND, WizardMove.UNBIND, WizardMove.AWAKEN, WizardMove.APOTHEOSIS ->
                    SoundEvents.WITHER_AMBIENT;
            case WizardMove.STUNNED -> SoundEvents.ZOMBIE_VILLAGER_CURE;
            case WizardMove.SKIP, WizardMove.AIRSTEP -> SoundEvents.ILLUSIONER_MIRROR_MOVE;
            case WizardMove.ARRIVE -> SoundEvents.END_PORTAL_SPAWN;
            case WizardMove.ORBIT, WizardMove.LANCES -> SoundEvents.AMETHYST_BLOCK_RESONATE;
            case WizardMove.NOVA -> SoundEvents.POWDER_SNOW_STEP;
            case WizardMove.TORNADO -> SoundEvents.BREEZE_CHARGE;
            case WizardMove.COMET, WizardMove.INFERNO -> SoundEvents.BLAZE_AMBIENT;
            case WizardMove.PHANTOM -> SoundEvents.PHANTOM_AMBIENT;
            case WizardMove.TENDRILS, WizardMove.VOID_ORB, WizardMove.DRAIN -> SoundEvents.WARDEN_NEARBY_CLOSER;
            case WizardMove.SPIRAL, WizardMove.SUPERNOVA -> SoundEvents.EVOKER_PREPARE_SUMMON;
            default -> SoundEvents.EVOKER_PREPARE_ATTACK;
        };
        float pitch = kind == WizardMove.STRIKE ? 0.6F : kind == WizardMove.SPARK ? 1.6F
                : kind == WizardMove.SKIP || kind == WizardMove.AIRSTEP ? 1.5F : 0.7F;
        boolean changes = WizardMove.carries(kind) != 0;
        boolean loud = changes || kind == WizardMove.ARRIVE || kind == WizardMove.SUPERNOVA;
        wizard.playSound(sound, loud ? 2.5F : kind == WizardMove.SKIP || kind == WizardMove.AIRSTEP ? 0.8F : 1.3F,
                pitch);
        if (changes) {
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
            case WizardMove.ASCEND, WizardMove.UNBIND, WizardMove.AWAKEN, WizardMove.APOTHEOSIS -> burst(level,
                    wizard, WizardMove.carries(kind));
            case WizardMove.ARRIVE -> land(level, wizard);
            case WizardMove.ORBIT -> {
                if (target != null) {
                    ArcaneBolt.orbit(level, wizard, target, wizard.harm(WizardMove.ORBIT), 4 + wizard.phase() / 2,
                            14, 5);
                }
            }
            case WizardMove.LANCES -> {
                if (target != null) {
                    PrismLance.fan(level, wizard, target, wizard.harm(WizardMove.LANCES),
                            wizard.phase() > 3 ? 5 : 3, 16, 4);
                }
            }
            case WizardMove.NOVA -> FrostNova.burst(level, wizard, ground(level, wizard.position()),
                    wizard.harm(WizardMove.NOVA));
            case WizardMove.TORNADO -> {
                Vec3 toward = wizard.position().subtract(aim).multiply(1.0, 0.0, 1.0);
                Vec3 at = toward.lengthSqr() < 1.0 ? aim : aim.add(toward.normalize().scale(4.0));
                ArcaneTornado.raise(level, wizard, ground(level, at), target, wizard.harm(WizardMove.TORNADO));
            }
            case WizardMove.INFERNO -> InfernoRing.kindle(level, wizard, ground(level, aim),
                    wizard.harm(WizardMove.INFERNO));
            case WizardMove.TENDRILS -> tendrils(level, wizard, target);
            case WizardMove.VOID_ORB -> VoidOrb.loose(level, wizard, wizard.staffTip(), target,
                    wizard.harm(WizardMove.VOID_ORB));
            case WizardMove.DRAIN -> SoulDrain.reach(level, wizard, target, wizard.harm(WizardMove.DRAIN),
                    WizardMove.DRAIN_STOP - WizardMove.release(WizardMove.DRAIN));
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
                        && extra / WizardMove.BOLT_AGAIN < bolts(wizard)) {
                    ArcaneBolt.cast(level, wizard, target, wizard.harm(WizardMove.BOLT));
                }
            }
            case WizardMove.BEAM -> {
                // The beam follows where he looks, and he turns after them only slowly.
                if (target != null) {
                    wizard.getLookControl().setLookAt(target, 3.0F, 3.0F);
                }
            }
            case WizardMove.SKIP -> {
                Vec3 at = wizard.position().add(0.0, 1.0 + wizard.hover(), 0.0);
                ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.3F), at.x, at.y, at.z, 6, 0.25,
                        0.6, 0.25, 0.0);
                ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y - 0.4, at.z, 2, 0.2, 0.3, 0.2, 0.01);
            }
            case WizardMove.ARRIVE -> descend(level, wizard, t);
            case WizardMove.SPIRAL -> spiral(level, wizard, t);
            case WizardMove.SUPERNOVA -> {
                if (t == WizardMove.NOVA_RISE) {
                    Supernova.kindle(level, wizard, wizard.harm(WizardMove.SUPERNOVA));
                }
            }
            case WizardMove.ASCEND, WizardMove.UNBIND, WizardMove.AWAKEN, WizardMove.APOTHEOSIS -> {
                Vec3 at = wizard.position().add(0.0, 1.2 + wizard.hover(), 0.0);
                if (t < WizardMove.BURST && t % 3 == 0) {
                    ParticleFx.implosion(level, Wizard.dust(Wizard.light(WizardMove.carries(kind)), 1.6F), at,
                            5.0 + t * 0.06, 10 + t / 5, 0.5);
                }
                if (t == WizardMove.BURST - 20) {
                    wizard.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.5F);
                }
            }
            default -> {
            }
        }
    }

    // How many bolts he lets go at once: one more each phase, three at most.
    static int bolts(Wizard wizard) {
        return Math.min(3, wizard.phase());
    }

    // Tendrils under his target and one more foe he can see, from his fifth phase two more.
    private static void tendrils(ServerLevel level, Wizard wizard, @Nullable LivingEntity target) {
        List<LivingEntity> foes = MonsterBlows.foes(wizard, wizard.getBoundingBox().inflate(16.0));
        foes.removeIf(living -> living == target || !Wizard.prey(living) || !wizard.hasLineOfSight(living));
        if (target != null && target.isAlive()) {
            foes.add(0, target);
        }
        int most = wizard.phase() > 4 ? 3 : 2;
        for (int k = 0; k < Math.min(most, foes.size()); k++) {
            LivingEntity living = foes.get(k);
            VoidTendrils.rise(level, wizard, living, ground(level, living.position()),
                    wizard.harm(WizardMove.TENDRILS));
        }
    }

    // The star spiral: shards of starlight flung out of him on turning arms, every volley a little further round.
    private static void spiral(ServerLevel level, Wizard wizard, int t) {
        int from = WizardMove.release(WizardMove.SPIRAL);
        if (t < from || t > WizardMove.SPIRAL_STOP || (t - from) % SPIRAL_EVERY != 0) {
            return;
        }
        int arms = 5;
        Vec3 middle = wizard.position().add(0.0, 1.3 + wizard.hover(), 0.0);
        double turn = (t - from) * 0.21;
        for (int arm = 0; arm < arms; arm++) {
            double angle = turn + arm * Mth.TWO_PI / arms;
            Vec3 way = new Vec3(Math.cos(angle), -0.08 + 0.06 * Math.sin(t * 0.4 + arm), Math.sin(angle));
            ArcaneBolt.shard(level, wizard, middle.add(way.scale(0.8)), way, wizard.harm(WizardMove.SPIRAL));
        }
        if ((t - from) % 6 == 0) {
            wizard.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.6F, 1.2F + 0.4F * wizard.getRandom().nextFloat());
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

    // The ground under (or a little below) `near`: the first solid block down from it, as far as 24 blocks.
    static Vec3 ground(ServerLevel level, Vec3 near) {
        BlockHitResult down = LoadedWorld.clip(level, new ClipContext(near.add(0.0, 0.5, 0.0), near.add(0.0, -24.0,
                0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (down.getType() != HitResult.Type.MISS) {
            return new Vec3(near.x, down.getLocation().y, near.z);
        }
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
        if (wizard.moves() == null || !wizard.moves().blinkNear(level, middle, target)) {
            teleportNear(wizard, middle, target, from);
        }
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

    // Out of his rift he sinks slowly to the ground, a spiral of light round him, and stops just over it before his
    // landing.
    private static void descend(ServerLevel level, Wizard wizard, int t) {
        Vec3 ground = wizard.arrivesAt();
        if (ground == null || t > WizardMove.LAND) {
            return;
        }
        double f = Math.min(1.0, t / (double) WizardMove.LAND);
        double eased = 1.0 - (1.0 - f) * (1.0 - f) * (1.0 - f);
        double y = ground.y + ARRIVE_HEIGHT * (1.0 - eased) + 0.15 * (1.0 - f);
        wizard.setNoGravity(true);
        wizard.setDeltaMovement(Vec3.ZERO);
        wizard.moveTo(ground.x, y, ground.z, wizard.getYRot(), wizard.getXRot());
        Vec3 at = wizard.position().add(0.0, 1.0, 0.0);
        if (t % 2 == 0) {
            ParticleFx.helix(level, Wizard.dust(Wizard.VIOLET, 1.4F), at.add(0.0, -1.0, 0.0), 1.2, 2.4, 1.5, 10,
                    t * 0.25);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y + 1.0, at.z, 2, 0.3, 0.4, 0.3, 0.02);
        }
        if (t % 8 == 0) {
            ParticleFx.ring(level, Wizard.dust(Wizard.STARLIGHT, 1.8F), ground.add(0.0, 0.1, 0.0), 2.6, 28, t * 0.1);
        }
        if (t == WizardMove.LAND - 10) {
            wizard.playSound(SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.5F);
        }
    }

    // He lands: the ground cracks in a ring of light, whoever stands near is thrown back, and his bar appears.
    private static void land(ServerLevel level, Wizard wizard) {
        landed(wizard);
        WizardWard.raise(level, wizard);
        Vec3 at = wizard.position().add(0.0, 0.2, 0.0);
        for (LivingEntity living : MonsterBlows.foes(wizard, wizard.getBoundingBox().inflate(LAND_REACH))) {
            Vec3 away = living.position().subtract(wizard.position()).multiply(1.0, 0.0, 1.0);
            double far = away.length();
            if (far > LAND_REACH) {
                continue;
            }
            away = far < 1.0E-3 ? MonsterBlows.ahead(wizard) : away.scale(1.0 / far);
            MonsterBlows.hurt(living, Wizard.source(wizard, wizard, WizardEntities.ARCANE),
                    wizard.harm(WizardMove.ARRIVE) * (float) (1.0 - 0.6 * far / LAND_REACH),
                    away.scale(1.3).add(0.0, 0.55, 0.0));
        }
        ParticleFx.shockwave(level, Wizard.dust(Wizard.VIOLET, 2.4F), at, 72, 0.9);
        ParticleFx.shockwave(level, ParticleTypes.END_ROD, at, 40, 0.6);
        ParticleFx.groundStar(level, Wizard.dust(Wizard.STARLIGHT, 1.8F), at, 4.5, 6, 2, 0.0, 0.3);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at.add(0.0, 1.0, 0.0), 30, 0.6);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5F, 0.7F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.4F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    // The arrival is over (or cut short): he falls as anything does again and his bar shows.
    static void landed(Wizard wizard) {
        wizard.setNoGravity(false);
        wizard.arrived();
    }

    // A phase bursting out of him: whoever stands near is thrown back, and he is the next phase.
    private static void burst(ServerLevel level, Wizard wizard, int phase) {
        wizard.setPhase(phase);
        WizardWard.raise(level, wizard);
        Vec3 at = wizard.position().add(0.0, 1.0 + wizard.hover(), 0.0);
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
        ParticleFx.sphereOut(level, switch (phase) {
            case 3 -> ParticleTypes.FLAME;
            case 4 -> ParticleTypes.SQUID_INK;
            case 5 -> ParticleTypes.END_ROD;
            default -> ParticleTypes.REVERSE_PORTAL;
        }, at, 80 + 20 * phase, 0.6);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 40, 0.7);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.TOTEM_USE, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.5F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.4F, 1.4F);
    }
}
