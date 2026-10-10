package nl.tivek.multiversepowers.character.greenlantern.ability.heavy;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The guns' shots as they land: the Rocket Launcher's rocket, cluster rocket, blast jump and guided rocket; the
// Sawed-off Shotgun's buckshot, both barrels, stock strike and the blast as the deflection drops. Shots fly from his
// eyes where he aims, drawn from the muzzle. Each shot spends a round (the launcher holds 1, the shotgun 2), and an
// empty gun reloads by itself.
abstract class HeavyGuns extends HeavyBlows {
    // How far a pellet strays from the aim (radians, about one in three this far or more).
    private static final double SPREAD = 0.06;
    private static final double DOUBLE_SPREAD = 0.1;
    private static final double BURST_SPREAD = 0.3;
    private static final double ROCKET_RANGE = 96.0;
    // After the blast jump he takes no fall damage until he lands, or this long.
    private static final int SOFT_FALL = 100;
    private int soft;
    int ammo;

    HeavyGuns(ServerPlayer owner, int weapon) {
        super(owner, weapon);
        this.ammo = ammo(weapon);
    }

    abstract void send();

    // Whether `next` fires a round: a shot, the aim, the launcher's blast jump and guided rocket.
    final boolean fires(int next) {
        return gun(this.weapon) && (next == SHOOT || next == AIM
                || this.weapon == RPG && (next == KICK || next == BRACE));
    }

    // Out of rounds: only the dry click of the trigger.
    final boolean empty(int next) {
        if (!this.fires(next) || this.ammo > 0) {
            return false;
        }
        this.sound(this.owner.serverLevel(), this.owner.getEyePosition(), SoundEvents.DISPENSER_FAIL, 0.6F, 1.7F);
        return true;
    }

    // Takes up to `most` rounds; how many there were.
    private int spend(int most) {
        int took = Math.min(most, this.ammo);
        if (took > 0) {
            this.ammo -= took;
            this.send();
        }
        return took;
    }

    @Override
    final void shots(ServerLevel level, int hit) {
        this.reloads(level);
        if (this.age != hit) {
            return;
        }
        switch (this.move) {
            case SHOOT -> {
                if (this.spend(1) == 0) {
                    return;
                }
                if (this.weapon == RPG) {
                    this.rocket(level, false, false);
                } else {
                    this.buckshot(level, (int) value("shotgunPellets"), SPREAD, value("shotgunRange"));
                }
            }
            case LOOSE -> {
                int barrels = this.spend(this.weapon == RPG ? 1 : 2);
                if (barrels == 0) {
                    return;
                }
                if (this.weapon == RPG) {
                    this.rocket(level, true, false);
                } else {
                    this.buckshot(level, (int) value("shotgunPellets") * barrels, barrels == 2 ? DOUBLE_SPREAD
                            : SPREAD, value("shotgunRange"));
                    this.recoil(barrels == 2 ? 0.55 : 0.3);
                }
            }
            case KICK -> {
                if (this.weapon == SHOTGUN) {
                    this.bash(level);
                } else if (this.spend(1) > 0) {
                    this.blastJump(level);
                }
            }
            case BRACE -> {
                if (this.weapon == RPG && this.spend(1) > 0) {
                    this.rocket(level, false, true);
                }
            }
            case UNBRACE -> {
                if (this.weapon == RPG) {
                    HeavyRocket.letGo(this.owner);
                } else if (this.spend(1) > 0) {
                    this.buckshot(level, (int) value("shotgunPellets"), BURST_SPREAD, value("shotgunRange") * 0.4);
                }
            }
            default -> {
            }
        }
    }

    // The reload's clicks and clacks: the shotgun broken open, its spent shells out, two new in and snapped shut; the
    // launcher's new rocket slid in and locked. The rounds are in a little before it ends.
    private void reloads(ServerLevel level) {
        if (this.move != RELOAD) {
            return;
        }
        Vec3 at = this.owner.getEyePosition();
        int age = this.age;
        if (age == length(this.weapon, RELOAD) - RELOADED) {
            this.ammo = ammo(this.weapon);
            this.send();
        }
        if (this.weapon == RPG) {
            if (age == 4) {
                this.sound(level, at, SoundEvents.CROSSBOW_LOADING_START.value(), 0.9F, 0.6F);
            } else if (age == 12) {
                this.sound(level, at, SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), 0.9F, 0.6F);
            } else if (age == 19) {
                this.sound(level, at, SoundEvents.CROSSBOW_LOADING_END.value(), 0.9F, 0.7F);
                this.sound(level, at, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.5F, 1.5F);
            }
        } else if (age == 4) {
            this.sound(level, at, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7F, 1.6F);
        } else if (age == 8) {
            this.sound(level, at, SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.6F, 1.5F);
        } else if (age == 15 || age == 20) {
            this.sound(level, at, SoundEvents.CROSSBOW_QUICK_CHARGE_1.value(), 0.7F, 1.3F + (age - 15) * 0.04F);
        } else if (age == 25) {
            this.sound(level, at, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.8F, 1.7F);
        }
    }

    // Each tick the weapon is held: the blast jump's soft landing.
    final void landing() {
        if (this.soft <= 0) {
            return;
        }
        this.owner.fallDistance = 0.0F;
        this.soft--;
        if (this.soft < SOFT_FALL - 10 && this.owner.onGround()) {
            this.soft = 0;
        }
    }

    private Vec3 right() {
        Vec3 look = this.owner.getLookAngle();
        Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
        return side.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0.0F, this.owner.getYRot() + 90.0F)
                : side.normalize();
    }

    // Where the muzzle is: ahead of his eyes, a little right and down.
    private Vec3 muzzle(double ahead) {
        return this.owner.getEyePosition().add(this.owner.getLookAngle().scale(ahead)).add(this.right().scale(0.28))
                .add(0.0, -0.18, 0.0);
    }

    // What his crosshair rests on, as far as `range`.
    private Vec3 aimed(ServerLevel level, double range) {
        Vec3 eye = this.owner.getEyePosition();
        Vec3 end = eye.add(this.owner.getLookAngle().scale(range));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        return block.getType() == HitResult.Type.MISS ? end : block.getLocation();
    }

    // A rocket, a cluster rocket or one he guides with his crosshair while he holds on.
    private void rocket(ServerLevel level, boolean cluster, boolean guided) {
        Vec3 mouth = this.muzzle(1.2).add(0.0, 0.12, 0.0);
        Vec3 way = this.aimed(level, ROCKET_RANGE).subtract(mouth);
        way = way.lengthSqr() < 1.0E-4 ? this.owner.getLookAngle() : way.normalize();
        double damage = value(cluster ? "rpgClusterDamage" : "rpgDamage");
        if (guided) {
            HeavyRocket.guide(level, this.owner, mouth, way, damage, value("rpgBlastRadius"));
        } else {
            HeavyRocket.fire(level, this.owner, mouth, way, cluster, damage, value("rpgBlastRadius"));
        }
        Vec3 back = mouth.subtract(this.owner.getLookAngle().scale(2.0));
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), mouth, 6, 0.15, 0.08);
        ParticleFx.cloud(level, ParticleTypes.CLOUD, back, 6, 0.25, 0.06);
        this.sound(level, mouth, SoundEvents.FIREWORK_ROCKET_LAUNCH, 2.2F, 0.6F);
        this.sound(level, mouth, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 1.8F);
        this.recoil(cluster ? 0.25 : 0.15);
    }

    // Shot into the ground at his feet: the blast throws him up and on the way he looks, and whatever stands near.
    private void blastJump(ServerLevel level) {
        Vec3 feet = this.owner.position();
        HeavyRocket.blast(level, this.owner, feet, value("rpgBlastRadius") * 0.7, value("rpgDamage") * 0.35, 1.0,
                false);
        Vec3 look = this.owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize();
        this.owner.setDeltaMovement(flat.scale(0.85).add(0.0, 1.15, 0.0));
        this.owner.hurtMarked = true;
        this.owner.fallDistance = 0.0F;
        this.soft = SOFT_FALL;
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), feet.add(0.0, 0.15, 0.0), 20, 0.45);
        ParticleFx.send(level, ParticleTypes.EXPLOSION, feet.x, feet.y + 0.3, feet.z, 1, 0.2, 0.1, 0.2, 0.0);
        this.sound(level, feet, SoundEvents.GENERIC_EXPLODE.value(), 1.6F, 1.2F);
        this.sound(level, feet, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.6F, 0.5F);
    }

    // `pellets` shots straying `spread` round his aim as far as `range`; each creature takes what struck it, less
    // the further it stood, and one struck by most of them is knocked down.
    private void buckshot(ServerLevel level, int pellets, double spread, double range) {
        Vec3 eye = this.owner.getEyePosition();
        Vec3 look = this.owner.getLookAngle();
        Vec3[] across = Vectors.across(look);
        RandomSource random = this.owner.getRandom();
        Vec3 muzzle = this.muzzle(0.9);
        double per = value("shotgunPelletDamage");
        Map<LivingEntity, double[]> struck = new LinkedHashMap<>();
        for (int i = 0; i < pellets; i++) {
            Vec3 way = look.add(across[0].scale(random.nextGaussian() * spread))
                    .add(across[1].scale(random.nextGaussian() * spread)).normalize();
            Vec3 end = eye.add(way.scale(range));
            BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (block.getType() != HitResult.Type.MISS) {
                end = block.getLocation();
            }
            LivingEntity first = null;
            double firstFar = Double.MAX_VALUE;
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(0.5),
                    living -> living != this.owner && living.isAlive() && PowerRing.canHit(this.owner, living))) {
                Optional<Vec3> on = living.getBoundingBox().inflate(0.15).clip(eye, end);
                if (on.isPresent() && eye.distanceToSqr(on.get()) < firstFar) {
                    firstFar = eye.distanceToSqr(on.get());
                    first = living;
                    end = on.get();
                }
            }
            if (first != null) {
                double far = Math.sqrt(firstFar);
                double[] sum = struck.computeIfAbsent(first, living -> new double[2]);
                sum[0] += per * (1.0 - 0.6 * Math.min(1.0, far / range));
                sum[1] += 1.0;
            }
            ParticleFx.line(level, ParticleFx.dust(PowerRing.BRIGHT, 0.45F), muzzle, end, 1.6);
            ParticleFx.cloud(level, first != null ? ParticleTypes.CRIT : ParticleTypes.SMOKE, end, 2, 0.05, 0.02);
        }
        for (Map.Entry<LivingEntity, double[]> hit : struck.entrySet()) {
            double share = hit.getValue()[1] / pellets;
            Vec3 push = look.scale(0.2 + 1.1 * share).add(0.0, 0.12 + 0.25 * share, 0.0);
            this.strike(level, hit.getKey(), hit.getValue()[0], push, share >= 0.6);
        }
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F), muzzle, 8, 0.12, 0.12);
        ParticleFx.cloud(level, ParticleTypes.SMOKE, muzzle.add(look.scale(0.3)), 4, 0.1, 0.03);
        this.sound(level, muzzle, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.75F);
        this.sound(level, muzzle, SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F, 0.55F);
    }

    // The stock driven into the creature ahead, knocking it off its feet.
    private void bash(ServerLevel level) {
        LivingEntity target = this.ahead(level, 3.0);
        Vec3 front = this.owner.getEyePosition().add(this.owner.getLookAngle().scale(1.5));
        this.sound(level, front, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.4F);
        if (target == null || !this.strike(level, target, value("shotgunBashDamage"), this.ahead.scale(1.0)
                .add(0.0, 0.35, 0.0), true)) {
            return;
        }
        this.sparks(level, target.getBoundingBox().getCenter(), 8);
        this.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
        this.sound(level, target.position(), SoundEvents.ANVIL_LAND, 0.25F, 1.8F);
    }

    // The kick of a shot pushes him back.
    private void recoil(double strength) {
        Vec3 look = this.owner.getLookAngle();
        this.owner.setDeltaMovement(this.owner.getDeltaMovement().add(look.scale(-strength)).add(0.0, 0.05, 0.0));
        this.owner.hurtMarked = true;
    }
}
