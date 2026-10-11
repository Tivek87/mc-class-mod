package nl.tivek.multiversepowers.character.greenlantern.ability.heavy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyShots;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The Dual Revolvers', the Arm Cannon's and the Minigun's moves as they land (when each shot leaves is HeavyShots, the
// same in every game, which draws and sounds them): the revolvers fire in turn, fan their hammers, whip with a butt
// and dead-eye up to six marks; the cannon fires plasma, charges a big blast, bashes and pours out a rapid stream of
// small bolts; the minigun bursts and streams rounds until it overheats, vents its heat and spins its barrels ready.
abstract class HeavyRounds extends HeavyGuns {
    private static final double FAN_SPREAD = 0.035;
    private static final double MINIGUN_SPREAD = 0.045;
    private static final double RAPID_SPREAD = 0.02;
    // Dead-eye marks what lies within this of his crosshair (cosine), as far as the revolvers reach.
    private static final double MARK_CONE = 0.985;
    // A hit this far below the top of a creature's box is to the head.
    private static final double HEAD = 0.25;
    final List<LivingEntity> marks = new ArrayList<>();
    // The minigun's heat, in rounds fired (HeavyShots.HEAT_ROUNDS overheats it), and whether it overheated.
    double heat;
    boolean overheated;
    // The minigun's barrels were spinning as its stream began.
    boolean spun;
    // How long the cannon charged before he let go.
    int charged;

    HeavyRounds(ServerPlayer owner, int weapon) {
        super(owner, weapon);
    }

    // A held move to let go of before its time: the minigun's stream as it overheats, the fan as the guns run dry.
    abstract void stop();

    @Override
    final void rounds(ServerLevel level, int hit) {
        boolean shot = HeavyShots.fires(this.weapon, this.move, this.age, this.spun, this.marks.size());
        if (this.weapon == MINIGUN && !shot) {
            this.heat = Math.max(0.0, this.heat - (this.move == RELOAD ? 0.0 : HeavyShots.COOL));
        }
        if (this.move == RELOAD) {
            this.reloading(level);
        }
        if (this.weapon == REVOLVERS && this.move == BRACE && this.age >= LOOP_FROM - 1) {
            this.mark(level);
        }
        if (shot) {
            switch (this.weapon) {
                case REVOLVERS -> this.revolver(level);
                case CANNON -> this.plasma(level);
                default -> this.minigun(level);
            }
        }
        if (this.move == KICK && this.age == hit) {
            switch (this.weapon) {
                case REVOLVERS -> this.whip(level);
                case CANNON -> this.bash(level);
                default -> this.vent(level);
            }
        }
        if (this.weapon == MINIGUN && (this.move == AIM || this.move == BRACE) && this.age % 4 == 0) {
            float rise = this.spun || this.move == BRACE && this.age >= HeavyShots.SPIN_UP ? 1.0F
                    : Math.min(1.0F, this.age / (float) HeavyShots.SPIN_UP);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.BEACON_AMBIENT, 0.5F, 0.9F + 1.1F * rise);
        }
        if (this.weapon == CANNON && this.move == AIM && this.age % 8 == 0) {
            float rise = Math.min(1.0F, this.age / (float) HeavyShots.CHARGE);
            this.sound(level, this.owner.getEyePosition(), SoundEvents.BEACON_AMBIENT, 0.6F + 0.6F * rise,
                    0.8F + 1.0F * rise);
        }
    }

    // The heat the minigun shows, out of 100.
    final int heatShown() {
        return (int) Math.round(Math.min(1.0, this.heat / HeavyShots.HEAT_ROUNDS) * 100.0);
    }

    // The reloads' sounds: the cylinders swung out, shaken empty, filled with light and snapped shut with a spin; the
    // minigun's barrels hissing as they cool. The rounds are in, and the heat gone, a little before it ends.
    private void reloading(ServerLevel level) {
        Vec3 at = this.owner.getEyePosition();
        int done = length(this.weapon, RELOAD) - RELOADED;
        if (this.age == done) {
            if (this.weapon == MINIGUN) {
                this.heat = 0.0;
                this.overheated = false;
            } else {
                this.ammo = ammo(this.weapon);
            }
            this.send();
        }
        if (this.weapon == REVOLVERS) {
            switch (this.age) {
                case 5 -> this.sound(level, at, SoundEvents.IRON_TRAPDOOR_OPEN, 0.7F, 1.9F);
                case 10 -> this.sound(level, at, SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.8F, 1.8F);
                case 14, 17, 20 -> this.sound(level, at, SoundEvents.AMETHYST_BLOCK_CHIME, 0.6F, 1.4F + this.age * 0.02F);
                case 25 -> this.sound(level, at, SoundEvents.IRON_TRAPDOOR_CLOSE, 0.8F, 1.9F);
                case 27 -> this.sound(level, at, SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), 0.6F, 1.6F);
                default -> {
                }
            }
        } else if (this.weapon == MINIGUN && this.age % 6 == 0) {
            this.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.4F - this.age * 0.01F);
            ParticleFx.cloud(level, ParticleTypes.CLOUD, this.muzzle(1.4), 3, 0.15, 0.03);
        }
    }

    // A revolver round: from the gun whose turn it is, a straight shot at the crosshair (fanned, a little off it;
    // dead-eye, at the next mark).
    private void revolver(ServerLevel level) {
        if (this.spend(1) == 0) {
            this.sound(level, this.owner.getEyePosition(), SoundEvents.DISPENSER_FAIL, 0.6F, 1.9F);
            if (this.move == AIM) {
                this.stop();
            }
            return;
        }
        Vec3 eye = this.owner.getEyePosition();
        Vec3 way = this.owner.getLookAngle();
        double spread = this.move == AIM ? FAN_SPREAD : 0.006;
        if (this.move == UNBRACE) {
            int k = HeavyShots.fired(this.weapon, this.move, this.age, false, this.marks.size()) - 1;
            LivingEntity mark = k >= 0 && k < this.marks.size() ? this.marks.get(k) : null;
            if (mark != null && mark.isAlive() && this.owner.hasLineOfSight(mark)) {
                Vec3 to = mark.getBoundingBox().getCenter().add(0.0, mark.getBbHeight() * 0.25, 0.0).subtract(eye);
                way = to.normalize();
                spread = 0.0;
            }
        }
        this.bullet(level, eye, way, spread, value("revolverRange"), value("revolverDamage"),
                value("revolverHeadshot"), 0.35);
        float pitch = this.move == AIM ? 1.9F : 1.7F;
        this.sound(level, eye, SoundEvents.GENERIC_EXPLODE.value(), 0.45F, pitch + 0.1F);
        this.sound(level, eye, SoundEvents.CROSSBOW_SHOOT, 0.9F, pitch);
        if (this.move == SHOOT) {
            this.recoil(0.04);
        }
    }

    // The cannon's plasma: a quick ball, or let go after a charge one that grows with it, kicking him back; held on
    // the right button, a stream of small bolts straying a little round the aim.
    private void plasma(ServerLevel level) {
        Vec3 mouth = this.muzzle(1.3).add(0.0, 0.05, 0.0);
        Vec3 way = this.aimed(level, 96.0).subtract(mouth);
        way = way.lengthSqr() < 1.0E-4 ? this.owner.getLookAngle() : way.normalize();
        if (this.move == BRACE) {
            RandomSource random = this.owner.getRandom();
            Vec3[] across = Vectors.across(way);
            way = way.add(across[0].scale(random.nextGaussian() * RAPID_SPREAD))
                    .add(across[1].scale(random.nextGaussian() * RAPID_SPREAD)).normalize();
            HeavyPlasma.fire(level, this.owner, mouth, way, value("cannonRapidDamage"), value("cannonRapidRadius"));
            this.sound(level, mouth, SoundEvents.BLAZE_SHOOT, 0.45F, 1.8F + 0.2F * random.nextFloat());
            this.recoil(0.015);
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.8F), mouth, 2, 0.06, 0.05);
            return;
        }
        double charge = this.move == LOOSE ? Mth.clamp(this.charged / (double) HeavyShots.CHARGE, 0.0, 1.0) : 0.0;
        double damage = Mth.lerp(charge, value("cannonDamage"), value("cannonChargedDamage"));
        double radius = Mth.lerp(charge, value("cannonRadius"), value("cannonChargedRadius"));
        HeavyPlasma.fire(level, this.owner, mouth, way, damage, radius);
        this.sound(level, mouth, SoundEvents.BLAZE_SHOOT, 0.8F + 0.6F * (float) charge, 1.5F - 0.7F * (float) charge);
        this.sound(level, mouth, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.7F + 0.8F * (float) charge,
                1.4F - 0.6F * (float) charge);
        this.recoil(0.08 + 0.55 * charge);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F + (float) charge), mouth, 6 + (int) (10 * charge),
                0.12 + 0.2 * charge, 0.08);
    }

    // A minigun round, straying round the aim; the barrels heat as it pours.
    private void minigun(ServerLevel level) {
        this.heat += 1.0;
        Vec3 eye = this.owner.getEyePosition();
        this.bullet(level, eye, this.owner.getLookAngle(), MINIGUN_SPREAD, value("minigunRange"),
                value("minigunDamage"), 1.0, 0.12);
        this.sound(level, eye, SoundEvents.CROSSBOW_SHOOT, 0.45F, 1.9F + 0.2F * this.owner.getRandom().nextFloat());
        if (this.age % 2 == 0) {
            this.sound(level, eye, SoundEvents.GENERIC_EXPLODE.value(), 0.18F, 2.0F);
        }
        if (this.heat >= HeavyShots.HEAT_ROUNDS && this.move == AIM) {
            this.overheated = true;
            this.sound(level, eye, SoundEvents.FIRE_EXTINGUISH, 1.2F, 0.7F);
            this.stop();
        }
    }

    // One round from `eye` along `way`, straying `spread`: what it strikes first within `range` takes `damage` (times
    // `head` to the head), pushed back by `push`; where it ends, sparks.
    private void bullet(ServerLevel level, Vec3 eye, Vec3 way, double spread, double range, double damage,
            double head, double push) {
        RandomSource random = this.owner.getRandom();
        Vec3[] across = Vectors.across(way);
        Vec3 aim = way.add(across[0].scale(random.nextGaussian() * spread))
                .add(across[1].scale(random.nextGaussian() * spread)).normalize();
        Vec3 end = eye.add(aim.scale(range));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        boolean wall = block.getType() != HitResult.Type.MISS;
        if (wall) {
            end = block.getLocation();
        }
        LivingEntity first = null;
        double firstFar = Double.MAX_VALUE;
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(0.5),
                living -> living != this.owner && living.isAlive() && PowerRing.canHit(this.owner, living))) {
            Optional<Vec3> on = living.getBoundingBox().inflate(0.1).clip(eye, end);
            if (on.isPresent() && eye.distanceToSqr(on.get()) < firstFar) {
                firstFar = eye.distanceToSqr(on.get());
                first = living;
                end = on.get();
            }
        }
        if (first != null) {
            boolean headshot = end.y > first.getBoundingBox().maxY - HEAD * first.getBbHeight();
            this.strike(level, first, damage * (headshot ? head : 1.0), aim.scale(push).add(0.0, 0.08, 0.0), false);
            ParticleFx.cloud(level, headshot ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT, end, 3, 0.05, 0.1);
        } else if (wall) {
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.6F), end, 2, 0.03, 0.02);
        }
    }

    // Holding dead-eye: every creature that crosses his crosshair is marked, as many as the revolvers have rounds.
    private void mark(ServerLevel level) {
        this.marks.removeIf(living -> !living.isAlive());
        int most = Math.min(HeavyShots.MARKS, this.ammo);
        if (this.marks.size() >= most) {
            return;
        }
        Vec3 eye = this.owner.getEyePosition();
        Vec3 look = this.owner.getLookAngle();
        double range = value("revolverRange");
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(eye, eye.add(look.scale(range))).inflate(4.0), living -> living != this.owner
                        && living.isAlive() && PowerRing.canHit(this.owner, living) && !this.marks.contains(living))) {
            Vec3 to = living.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > range || to.normalize().dot(look) < MARK_CONE || !this.owner.hasLineOfSight(living)) {
                continue;
            }
            this.marks.add(living);
            this.sound(level, eye, SoundEvents.NOTE_BLOCK_HAT.value(), 0.8F, 1.6F + 0.1F * this.marks.size());
            if (this.marks.size() >= most) {
                return;
            }
        }
    }

    // The right revolver's butt driven into the creature ahead.
    private void whip(ServerLevel level) {
        LivingEntity target = this.ahead(level, 3.0);
        this.sound(level, this.owner.getEyePosition(), SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F, 1.6F);
        if (target == null || !this.strike(level, target, value("revolverWhipDamage"), this.ahead.scale(0.8)
                .add(0.0, 0.3, 0.0), true)) {
            return;
        }
        this.sparks(level, target.getBoundingBox().getCenter(), 6);
        this.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 1.1F);
    }

    // The cannon rammed forward: the creature ahead takes it and a ring of force throws everything near it back.
    private void bash(ServerLevel level) {
        Vec3 front = this.owner.getEyePosition().add(this.owner.getLookAngle().scale(1.6));
        this.sound(level, front, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.8F);
        boolean any = false;
        for (LivingEntity living : this.arc(level, 3.2, 0.45)) {
            any |= this.strike(level, living, value("cannonBashDamage"), this.ahead.scale(1.2).add(0.0, 0.35, 0.0),
                    true);
        }
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.BRIGHT, 1.2F), front, 14, 0.3);
        if (any) {
            this.sound(level, front, SoundEvents.ANVIL_LAND, 0.4F, 1.6F);
            this.sound(level, front, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.7F);
        }
    }

    // The minigun vents all its heat at once: a blast of steam out of the barrels throws back what stands before it.
    private void vent(ServerLevel level) {
        this.heat = 0.0;
        this.overheated = false;
        this.send();
        Vec3 mouth = this.muzzle(1.5);
        for (LivingEntity living : this.arc(level, value("minigunVentReach"), 0.5)) {
            this.strike(level, living, value("minigunVentDamage"), this.ahead.scale(1.1).add(0.0, 0.3, 0.0), false);
        }
        ParticleFx.cloud(level, ParticleTypes.CLOUD, mouth.add(this.owner.getLookAngle().scale(1.0)), 18, 0.6, 0.12);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.0F), mouth, 10, 0.3, 0.1);
        this.sound(level, mouth, SoundEvents.FIRE_EXTINGUISH, 1.4F, 0.6F);
        this.sound(level, mouth, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 0.5F);
    }
}
