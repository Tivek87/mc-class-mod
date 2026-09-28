package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;

// What the mech's build sounds like and does to the world, beat by beat.
final class MechBlows {
    private static final SoundEvent STOMP = Sounds.of("mech.stomp");
    private static final SoundEvent CLAP = Sounds.of("mech.clap");
    private static final SoundEvent CLACK = Sounds.of("mech.clack");
    private static final SoundEvent BUILD = Sounds.of("mech.build");
    private static final SoundEvent WHOOSH = Sounds.of("mech.whoosh");
    private static final SoundEvent CRASH = Sounds.of("mech.crash");
    private static final SoundEvent SURGE = Sounds.of("mech.surge");
    private static final double STOMP_RADIUS = 3.0;
    private static final double CLAP_RADIUS = 3.0;
    private static final double CRASH_RADIUS = 4.5;
    private static final double STOMP_PUSH = 1.0;
    private static final double CLAP_PUSH = 1.1;
    private static final double CRASH_PUSH = 1.5;

    private MechBlows() {
    }

    static void beats(ServerLevel level, ServerPlayer owner, MechScript.Stage stage, MechTarget target,
            CharacterAbility ability, int t) {
        Vec3 foot = stage.point(MechScript.ankle(true, stage, t));
        Vec3 footLeft = stage.point(MechScript.ankle(false, stage, t));
        switch (t) {
            case MechScript.FOOT_FORM -> build(level, foot, 1.3F);
            case MechScript.FOOT_DROP, MechScript.FOOT2_DROP -> Sounds.play(level, t == MechScript.FOOT_DROP ? foot
                    : footLeft, WHOOSH, 3.0F, 0.75F);
            case MechScript.STOMP -> {
                stomp(level, foot.subtract(0.0, MechScript.ANKLE.y, 0.0), true);
                target.hit(level, owner, ability.value("mechStompDamage"));
                blast(level, owner, target.creature(), foot, STOMP_RADIUS, ability.value("mechStompDamage") * 0.5, STOMP_PUSH);
            }
            case MechScript.FOOT2_FORM -> build(level, footLeft, 1.4F);
            case MechScript.STOMP2 -> {
                stomp(level, footLeft.subtract(0.0, MechScript.ANKLE.y, 0.0), false);
                blast(level, owner, target.creature(), footLeft, STOMP_RADIUS * 0.8, 0.0, STOMP_PUSH * 0.7);
            }
            case MechScript.LEAP -> Sounds.play(level, stage.point(MechScript.pilot(stage, t)), WHOOSH, 1.6F, 1.35F);
            case MechScript.THIGHS -> {
                build(level, stage.point(MechScript.KNEE), 1.1F);
                build(level, stage.point(MechScript.mirror(MechScript.KNEE)), 1.1F);
            }
            case MechScript.THIGHS + MechScript.FORM_TICKS -> {
                click(level, stage.point(MechScript.HIP), 1.4F);
                click(level, stage.point(MechScript.mirror(MechScript.HIP)), 1.4F);
            }
            case MechScript.CORE -> {
                Vec3 core = stage.point(MechScript.COCKPIT.add(0.0, 1.1, 0.0));
                Sounds.play(level, core, SoundEvents.BEACON_POWER_SELECT, 2.0F, 0.6F);
                Sounds.play(level, core, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.6F, 0.8F);
            }
            case MechScript.ARMS_FORM -> {
                build(level, stage.point(MechMoves.arm(true, stage, t).hand()), 0.9F);
                build(level, stage.point(MechMoves.arm(false, stage, t).hand()), 0.9F);
            }
            case MechScript.ARMS_IN, MechScript.SWING, MechScript.RELEASE -> Sounds.play(level,
                    stage.point(stage.target().add(0.0, 2.0, 0.0)), WHOOSH, 2.4F, t == MechScript.SWING ? 1.1F : 0.9F);
            case MechScript.CLAP -> {
                clap(level, stage.point(stage.target().add(0.0, 1.25, 0.0)));
                target.hit(level, owner, ability.value("mechClapDamage"));
                blast(level, owner, target.creature(), stage.point(stage.target().add(0.0, 1.25, 0.0)), CLAP_RADIUS,
                        ability.value("mechClapDamage") * 0.5, CLAP_PUSH);
            }
            case MechScript.ARMOR -> build(level, stage.point(0.0, 7.0, 0.0), 0.7F);
            case MechScript.SHOULDERS + 12 -> {
                click(level, stage.point(MechScript.SHOULDER), 2.0F);
                click(level, stage.point(MechScript.mirror(MechScript.SHOULDER)), 2.0F);
            }
            case MechScript.ELBOWS -> {
                click(level, stage.point(MechMoves.arm(true, stage, t).elbow()), 2.6F);
                click(level, stage.point(MechMoves.arm(false, stage, t).elbow()), 2.6F);
                Sounds.play(level, stage.point(0.0, 7.0, 0.0), SoundEvents.ANVIL_LAND, 0.5F, 1.4F);
            }
            case MechScript.HEAD_FORM -> {
                Vec3 head = stage.point(MechScript.head(stage, t));
                build(level, head, 0.8F);
                Sounds.play(level, head, SoundEvents.BEACON_ACTIVATE, 3.0F, 0.9F);
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 2.0F), head, 60, 0.5);
            }
            case MechScript.HEAD_DROP -> Sounds.play(level, stage.point(MechScript.head(stage, t)), WHOOSH, 4.0F,
                    0.6F);
            case MechScript.CRASH -> {
                Vec3 at = stage.point(stage.target());
                crash(level, at);
                target.hit(level, owner, ability.value("mechHeadDamage"));
                blast(level, owner, target.creature(), at, CRASH_RADIUS, ability.value("mechHeadDamage") * 0.5, CRASH_PUSH);
            }
            case MechScript.HEAD_RISE -> Sounds.play(level, stage.point(stage.target()), WHOOSH, 3.0F, 0.85F);
            case MechScript.LOCK -> {
                Vec3 neck = stage.point(MechScript.NECK);
                Sounds.play(level, neck, CLACK, 5.0F, 0.75F);
                Sounds.play(level, neck, SoundEvents.ANVIL_LAND, 1.2F, 0.7F);
                Sounds.play(level, neck, SoundEvents.IRON_DOOR_CLOSE, 2.0F, 0.5F);
                ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), neck.add(0.0, 0.5, 0.0), 40, 0.35);
            }
            case MechScript.LOCK + 2 -> Sounds.play(level, stage.point(0.0, 7.0, 0.0), SURGE, 4.0F, 1.0F);
            case MechScript.DONE -> Sounds.play(level, stage.point(0.0, 7.0, 0.0), SoundEvents.BEACON_ACTIVATE, 2.5F,
                    0.7F);
            default -> {
            }
        }
        for (int side = 0; side < 2; side++) {
            if (t == MechScript.STEPS[side] + MechScript.STEP_TICKS) {
                Vec3 at = side == 0 ? foot : footLeft;
                Sounds.play(level, at, SoundEvents.IRON_GOLEM_STEP, 2.4F, 0.45F);
                Sounds.play(level, at, STOMP, 1.6F, 1.25F);
                dust(level, at.subtract(0.0, MechScript.ANKLE.y, 0.0), 10, 0.6);
            }
        }
        if (t > MechScript.CLAP && t < MechScript.RELEASE && (t - MechScript.CLAP) % 4 == 2) {
            Vec3 at = stage.point(stage.target().add(0.0, 1.4, 0.0));
            Sounds.play(level, at, SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 0.55F);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), at, 6, 0.4, 0.05);
        }
    }

    private static void build(ServerLevel level, Vec3 at, float pitch) {
        Sounds.play(level, at, BUILD, 2.4F, pitch);
        Sounds.play(level, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, pitch * 0.6F);
    }

    private static void click(ServerLevel level, Vec3 at, float volume) {
        Sounds.play(level, at, CLACK, volume, 1.25F);
        Sounds.play(level, at, SoundEvents.PISTON_EXTEND, volume * 0.5F, 0.7F);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.1F), at, 12, 0.2);
    }

    static void stomp(ServerLevel level, Vec3 ground, boolean hard) {
        Sounds.play(level, ground, STOMP, hard ? 5.0F : 3.2F, hard ? 0.85F : 1.0F);
        Sounds.play(level, ground, SoundEvents.MACE_SMASH_GROUND_HEAVY, hard ? 2.0F : 1.2F, 0.7F);
        dust(level, ground, hard ? 24 : 12, hard ? 1.1 : 0.8);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, ground.add(0.0, 0.1, 0.0), hard ? 12 : 8, hard ? 0.45 : 0.3);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), ground.add(0.0, 0.4, 0.0), hard ? 40 : 18,
                1.0, 0.25);
        ParticleFx.cloud(level, ParticleTypes.CRIT, ground.add(0.0, 0.5, 0.0), hard ? 20 : 8, 0.8, 0.4);
    }

    private static void clap(ServerLevel level, Vec3 at) {
        Sounds.play(level, at, CLAP, 5.0F, 1.0F);
        Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.0F, 0.9F);
        Sounds.play(level, at, SoundEvents.FIREWORK_ROCKET_BLAST, 2.0F, 0.6F);
        ParticleFx.at(level, ParticleTypes.FLASH, at);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.8F), at, 50, 0.45);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, at, 6, 0.5, 0.2);
    }

    static void crash(ServerLevel level, Vec3 at) {
        Sounds.play(level, at, CRASH, 6.0F, 1.0F);
        Sounds.play(level, at, SoundEvents.GENERIC_EXPLODE.value(), 2.5F, 0.8F);
        Sounds.play(level, at, SoundEvents.MACE_SMASH_GROUND_HEAVY, 2.5F, 0.6F);
        BlockState under = under(level, at);
        if (!under.isAir()) {
            Sounds.play(level, at, under.getSoundType(level, BlockPos.containing(at.x, at.y - 0.5, at.z), null)
                    .getBreakSound(), 2.5F, 0.6F);
        }
        dust(level, at, 50, 1.6);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, at.add(0.0, 0.15, 0.0), 16, 0.6);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 2.0F), at.add(0.0, 0.25, 0.0), 40, 0.8);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.GREEN, 2.2F), at.add(0.0, 0.8, 0.0), 50, 0.6);
    }

    private static void dust(ServerLevel level, Vec3 ground, int count, double spread) {
        BlockState under = under(level, ground);
        if (!under.isAir()) {
            ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, under), ground.add(0.0, 0.3, 0.0),
                    count, spread, 0.35);
        }
    }

    private static BlockState under(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at.x, at.y - 0.5, at.z);
        return level.isLoaded(pos) ? level.getBlockState(pos) : Blocks.AIR.defaultBlockState();
    }

    // Everything the owner may hit, but never the creature the mech holds: that one takes each blow whole.
    static void blast(ServerLevel level, ServerPlayer owner, @Nullable Entity held, Vec3 at, double radius,
            double damage, double push) {
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                entity -> entity != held && PowerRing.canHit(owner, entity))) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(at);
            double far = to.length();
            if (far > radius) {
                continue;
            }
            double near = 1.0 - far / radius;
            if (damage > 0.0) {
                living.invulnerableTime = 0;
                living.hurt(level.damageSources().playerAttack(owner), (float) (damage * (0.5 + 0.5 * near)));
            }
            double resist = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0, 1.0);
            double flat = Math.sqrt(to.x * to.x + to.z * to.z);
            Vec3 out = flat < 1.0E-3 ? Vec3.ZERO : new Vec3(to.x / flat, 0.0, to.z / flat);
            double strength = push * (0.5 + 0.5 * near);
            living.setDeltaMovement(new Vec3(out.x * strength, 0.35 + 0.3 * near, out.z * strength)
                    .scale(1.0 - resist));
            living.hasImpulse = true;
            living.hurtMarked = true;
        }
    }
}
