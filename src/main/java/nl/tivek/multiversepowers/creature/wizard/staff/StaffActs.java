package nl.tivek.multiversepowers.creature.wizard.staff;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.field.RuneMine;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcLightning;
import nl.tivek.multiversepowers.creature.wizard.spell.ArcaneBolt;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// What the Wizard's staff does when it fights on its own (WizardStaff.FREE): seven things from afar (a volley of
// sparks, a short ray, a seeking orb, a fan of frost shards, a lightning zap, a ring of rune mines, a pull that drags its
// foe in) and three up close (spinning through them like a blade, a thrust crystal first, a slam down from above). Each
// lasts its own ticks; the staff holds still or moves as the act wants and points its crystal where it casts.
public final class StaffActs {
    public static final int VOLLEY = 1;
    public static final int RAY = 2;
    public static final int SEEKER = 3;
    public static final int SHARDS = 4;
    public static final int ZAP = 5;
    public static final int MINES = 6;
    public static final int PULL = 7;
    public static final int SPIN = 8;
    public static final int THRUST = 9;
    public static final int SLAM = 10;
    private static final int[] LENGTH = { 0, 20, 32, 16, 14, 16, 18, 24, 24, 22, 28 };
    // Up close it strikes rather than casts (most of the time); its ray reaches this far.
    private static final double CLOSE = 5.5;
    private static final double RAY_REACH = 14.0;
    private static final double RAY_WIDTH = 0.7;
    private static final double SPIN_REACH = 1.9;
    private static final double SLAM_REACH = 3.4;

    private StaffActs() {
    }

    public static int length(int act) {
        return act > 0 && act < LENGTH.length ? LENGTH[act] : 0;
    }

    // Spinning like a blade, it lies flat and turns about its middle.
    public static boolean spins(int act) {
        return act == SPIN;
    }

    static int pick(RandomSource random, double far) {
        if (far < CLOSE && random.nextInt(3) != 0) {
            return SPIN + random.nextInt(3);
        }
        return VOLLEY + random.nextInt(7);
    }

    static void tick(ServerLevel level, WizardStaff staff, Wizard wizard, LivingEntity target, int act, int t) {
        Vec3 aimAt = target.getBoundingBox().getCenter();
        float harm = wizard.harm(WizardMove.STAFF_FREE);
        int light = Wizard.light(wizard.phase());
        if (act < SPIN) {
            // Casting it hangs back where it is, its crystal on its foe.
            staff.steer(staff.middle(), 0.1);
            staff.point(aimAt.subtract(staff.middle()));
            if (t == 1) {
                Vec3 tip = staff.tip();
                ParticleFx.send(level, Wizard.dust(light, 1.3F), tip.x, tip.y, tip.z, 10, 0.2, 0.2, 0.2, 0.0);
                level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE,
                        1.0F, 1.6F);
            }
        }
        Vec3 tip = staff.tip();
        switch (act) {
            case VOLLEY -> {
                if (t >= 4 && t <= 16 && (t - 4) % 3 == 0) {
                    ArcaneBolt.spark(level, wizard, tip, target, harm * 0.55F);
                }
            }
            case RAY -> ray(level, staff, wizard, target, t, harm, light);
            case SEEKER -> {
                if (t == 8) {
                    ArcaneBolt.castFrom(level, wizard, tip, target, harm * 1.1F);
                }
            }
            case SHARDS -> {
                if (t == 7) {
                    Vec3 way = aimAt.subtract(tip).normalize();
                    for (int k = -2; k <= 2; k++) {
                        double turn = k * 0.17;
                        Vec3 w = new Vec3(way.x * Math.cos(turn) - way.z * Math.sin(turn), way.y,
                                way.x * Math.sin(turn) + way.z * Math.cos(turn));
                        ArcaneBolt.shard(level, wizard, tip, w, harm * 0.7F);
                    }
                    ParticleFx.send(level, ParticleTypes.SNOWFLAKE, tip.x, tip.y, tip.z, 16, 0.2, 0.2, 0.2, 0.1);
                    level.playSound(null, tip.x, tip.y, tip.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.2F,
                            1.6F);
                }
            }
            case ZAP -> {
                if (t == 6) {
                    ArcLightning.strikeFrom(level, wizard, staff, target, harm, 2);
                }
            }
            case MINES -> {
                if (t == 8) {
                    RuneMine.ring(level, wizard, Wizard.groundUnder(level, target.position()), harm);
                }
            }
            case PULL -> pull(level, staff, target, t, light);
            case SPIN -> spin(level, staff, wizard, target, t, harm, light);
            case THRUST -> thrust(level, staff, wizard, target, t, harm, light);
            case SLAM -> slam(level, staff, wizard, target, t, harm, light);
            default -> {
            }
        }
    }

    // A short ray out of its crystal, following its foe slowly, burning whoever it crosses four times a second.
    private static void ray(ServerLevel level, WizardStaff staff, Wizard wizard, LivingEntity target, int t,
            float harm, int light) {
        if (t < 6 || t > 28) {
            return;
        }
        staff.aim = staff.aim.lerp(target.getBoundingBox().getCenter(), 0.12);
        Vec3 from = staff.tip();
        Vec3 way = staff.aim.subtract(from).normalize();
        staff.point(way);
        Vec3 to = from.add(way.scale(RAY_REACH));
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, staff));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        if (t == 6) {
            level.playSound(null, from.x, from.y, from.z, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 1.4F,
                    2.0F);
        }
        if (t % 2 == 0) {
            ParticleFx.line(level, Wizard.dust(light, 0.9F), from, end, 0.35);
            ParticleFx.send(level, ParticleTypes.END_ROD, end.x, end.y, end.z, 2, 0.1, 0.1, 0.1, 0.04);
        }
        if (t % 5 != 0) {
            return;
        }
        Vec3 line = end.subtract(from);
        double length = line.length();
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(from, end).inflate(RAY_WIDTH + 1.0))) {
            Vec3 middle = living.getBoundingBox().getCenter();
            double along = Mth.clamp(middle.subtract(from).dot(way), 0.0, length);
            if (middle.distanceTo(from.add(way.scale(along))) < RAY_WIDTH + living.getBbWidth() * 0.5) {
                MonsterBlows.hurt(living, Wizard.source(staff, wizard, WizardEntities.ARCANE), harm * 0.4F,
                        WizardKnock.knock(way.scale(0.12)));
            }
        }
    }

    // It drags its foe in towards itself on a stream of the void's motes, ready for a blow.
    private static void pull(ServerLevel level, WizardStaff staff, LivingEntity target, int t, int light) {
        if (t < 4 || t > 20) {
            return;
        }
        Vec3 at = staff.tip();
        Vec3 way = at.subtract(target.getBoundingBox().getCenter());
        if (t == 4) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.HOSTILE,
                    1.4F, 0.8F);
        }
        if (way.length() > 2.0) {
            Vec3 tug = way.normalize().scale(0.13);
            target.setDeltaMovement(target.getDeltaMovement().add(tug.x, tug.y * 0.5 + 0.02, tug.z));
            target.hurtMarked = true;
        }
        ParticleFx.line(level, ParticleTypes.REVERSE_PORTAL, target.getBoundingBox().getCenter(), at, 0.6);
        if (t % 3 == 0) {
            ParticleFx.line(level, Wizard.dust(light, 0.8F), target.getBoundingBox().getCenter(), at, 0.8);
        }
    }

    // Lying flat it spins like a blade through its foe, striking all round it.
    private static void spin(ServerLevel level, WizardStaff staff, Wizard wizard, LivingEntity target, int t,
            float harm, int light) {
        Vec3 middle = target.getBoundingBox().getCenter();
        staff.steer(middle, t < 6 ? 0.5 : 0.25);
        staff.point(new Vec3(1.0, 0.0, 0.0));
        if (t == 2) {
            Vec3 at = staff.middle();
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BREEZE_WHIRL, SoundSource.HOSTILE, 1.4F, 1.4F);
        }
        if (t < 4 || t > 20 || t % 4 != 0) {
            return;
        }
        Vec3 at = staff.middle();
        ParticleFx.ring(level, Wizard.dust(light, 1.1F), at, SPIN_REACH * 0.8, 14, t * 0.4);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 1.3F);
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(SPIN_REACH, 1.2,
                SPIN_REACH))) {
            Vec3 away = living.position().subtract(at).multiply(1.0, 0.0, 1.0);
            away = away.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
            MonsterBlows.hurt(living, Wizard.source(staff, wizard, WizardEntities.ARCANE), harm * 0.5F,
                    WizardKnock.knock(away.scale(0.45).add(0.0, 0.15, 0.0)));
        }
    }

    // It draws back, crystal on its foe, then darts through them.
    private static void thrust(ServerLevel level, WizardStaff staff, Wizard wizard, LivingEntity target, int t,
            float harm, int light) {
        Vec3 middle = target.getBoundingBox().getCenter();
        Vec3 from = staff.middle().subtract(middle).multiply(1.0, 0.0, 1.0);
        Vec3 back = from.lengthSqr() < 1.0E-4 ? new Vec3(0.0, 0.0, 1.0) : from.normalize();
        if (t < 9) {
            staff.steer(middle.add(back.scale(3.2)).add(0.0, 0.3, 0.0), 0.4);
            staff.point(middle.subtract(staff.middle()));
            return;
        }
        if (t == 9) {
            Vec3 at = staff.middle();
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.HOSTILE, 1.3F,
                    1.4F);
        }
        if (t <= 14) {
            Vec3 way = staff.way();
            Vec3 at = staff.middle();
            staff.setDeltaMovement(way.scale(1.1));
            staff.setMiddle(at.add(way.scale(1.1)));
            ParticleFx.send(level, Wizard.dust(light, 1.0F), at.x, at.y, at.z, 3, 0.1, 0.1, 0.1, 0.0);
            Vec3 tip = staff.tip();
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(tip, tip).inflate(0.8))) {
                MonsterBlows.hurt(living, Wizard.source(staff, wizard, WizardEntities.ARCANE), harm * 1.3F,
                        WizardKnock.knock(way.scale(1.0).add(0.0, 0.3, 0.0)));
                ParticleFx.send(level, ParticleTypes.CRIT, tip.x, tip.y, tip.z, 12, 0.2, 0.2, 0.2, 0.3);
            }
            return;
        }
        staff.steer(staff.middle(), 0.1);
    }

    // It rises high over its foe, then crashes down crystal first: a ring of force bursts out of the ground.
    private static void slam(ServerLevel level, WizardStaff staff, Wizard wizard, LivingEntity target, int t,
            float harm, int light) {
        Vec3 feet = target.position();
        if (t < 13) {
            staff.steer(feet.add(0.0, 5.0, 0.0), 0.5);
            staff.point(new Vec3(0.0, 1.0, 0.0));
            return;
        }
        Vec3 ground = Wizard.groundUnder(level, staff.middle());
        if (t < 17) {
            staff.point(new Vec3(0.0, -1.0, 0.0));
            Vec3 at = staff.middle();
            double drop = Math.max(0.0, at.y - (ground.y + WizardStaff.CRYSTAL));
            staff.setMiddle(at.add(0.0, -Math.min(1.8, drop), 0.0));
            if (t == 16) {
                boom(level, staff, wizard, ground, harm, light);
            }
        }
    }

    private static void boom(ServerLevel level, WizardStaff staff, Wizard wizard, Vec3 ground, float harm,
            int light) {
        Vec3 at = ground.add(0.0, 0.2, 0.0);
        ParticleFx.shockwave(level, Wizard.dust(light, 2.0F), at, 40, 0.6);
        ParticleFx.shockwave(level, ParticleTypes.CLOUD, at, 24, 0.35);
        MonsterBlows.dust(level, at, 30, 1.5);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.HOSTILE, 1.6F, 0.9F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.HOSTILE, 1.4F, 0.6F);
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(SLAM_REACH, 2.0,
                SLAM_REACH))) {
            Vec3 away = living.position().subtract(at).multiply(1.0, 0.0, 1.0);
            double far = away.length();
            if (far > SLAM_REACH) {
                continue;
            }
            away = far < 1.0E-3 ? new Vec3(1.0, 0.0, 0.0) : away.scale(1.0 / far);
            MonsterBlows.hurt(living, Wizard.source(staff, wizard, WizardEntities.ARCANE),
                    harm * 1.2F * (float) (1.0 - 0.5 * far / SLAM_REACH),
                    WizardKnock.knock(away.scale(0.7).add(0.0, 0.6, 0.0)));
        }
    }
}
