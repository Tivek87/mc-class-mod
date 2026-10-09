package nl.tivek.multiversepowers.character.thor.hammer;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.thor.ThorCharge;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.storm.StormFxPayload;
import nl.tivek.multiversepowers.engine.entity.DeathStyles;
import nl.tivek.multiversepowers.engine.entity.Knockdowns;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.target.Targeting;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import nl.tivek.multiversepowers.spell.SpellTargets;

// A Storm Throw's hammer as everyone sees it hurled (a bolt growing behind it as it flies), and where it strikes: its
// lightning bursts up out of the ground and runs out over it in a wide V that opens away from him, past the foe,
// striking everything in the V (burnt to ash if it dies of it, less towards the ends of its arms) and flinging it away
// from where it struck.
final class StormStrike {
    // Half the V's opening, in radians: wide, its arms running out more to the sides than ahead. What stands this
    // close round where it strikes is struck too, and anything this far over or under the ground there.
    private static final double HALF = 1.05;
    private static final double BURST = 2.0;
    private static final double HEIGHT = 3.5;
    // Struck closer than this to right under him, its V opens the way he faces.
    private static final double NEAR = 1.0;
    private static final double GROUND_LOOK = 6.0;
    // At the V's far end its lightning does this share of the harm it does where it strikes.
    private static final double FAR_SHARE = 0.6;
    private static final double KNOCK = 1.4;
    private static final double KNOCK_UP = 0.7;
    private static final int SHOCKED = 30;

    private StormStrike() {
    }

    // Let go: a bolt runs from his hand to where it will strike, as fast as the hammer flies.
    static void hurled(ServerLevel level, ServerPlayer owner, Vec3 from, Vec3 way, double far) {
        Vec3 to = from.add(way.scale(far));
        BlockHitResult block = LoadedWorld.clip(level, new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, owner));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        StormFxPayload.send(level, StormFxPayload.HURL, from, end, (float) HammerRules.STORM_SPEED);
    }

    // It struck at `at`; `struck` (what it hit on the way, if anything) was hurt by it already. Its V opens the way
    // from under him to where it struck, which is on to the foe it was hurled at.
    static void strike(ServerLevel level, ServerPlayer owner, Vec3 at, @Nullable LivingEntity struck, float damage) {
        CharacterAbility ability = GameCharacter.THOR.byName("storm_throw");
        double spread = ability == null ? 6.0 : ability.value("spreadBlocks");
        Vec3 ground = ground(level, at);
        Vec3 flat = new Vec3(ground.x - owner.getX(), 0.0, ground.z - owner.getZ());
        if (flat.lengthSqr() < NEAR * NEAR) {
            flat = Vec3.directionFromRotation(0.0F, owner.getYRot());
        }
        Vec3 ahead = flat.normalize();
        StormFxPayload.send(level, StormFxPayload.STRIKE, ground, ground.add(ahead.scale(spread)), (float) HALF);
        Vec3 low = ground.add(0.0, 0.2, 0.0);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, low.add(0.0, 0.3, 0.0), 16, 0.7, 0.3);
        Vec3 side = ahead.cross(Vectors.UP);
        for (int arm = -1; arm <= 1; arm += 2) {
            Vec3 along = ahead.scale(Math.cos(HALF)).add(side.scale(arm * Math.sin(HALF)));
            ParticleFx.zigzag(level, ParticleFx.dust(ThorMoves.GLOW, 1.3F), low, low.add(along.scale(spread)), 6, 0.4,
                    0.2);
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(ground, ground).inflate(spread + 2.0, HEIGHT + 2.0, spread + 2.0),
                entity -> entity != struck && Targeting.mayStrike(owner, entity))) {
            double share = share(target, ground, ahead, spread);
            if (share > 0.0) {
                hit(level, owner, target, ground, ahead, (float) (damage * share));
            }
        }
    }

    // How much of its harm the V does to a creature: all of it round where it struck, less towards the ends of its
    // arms, none outside it.
    private static double share(LivingEntity target, Vec3 ground, Vec3 ahead, double spread) {
        AABB box = target.getBoundingBox();
        if (box.minY > ground.y + HEIGHT || box.maxY < ground.y - HEIGHT) {
            return 0.0;
        }
        Vec3 off = new Vec3(target.getX() - ground.x, 0.0, target.getZ() - ground.z);
        double far = off.length();
        double body = target.getBbWidth() * 0.5;
        if (far <= BURST + body) {
            return 1.0;
        }
        double along = off.dot(ahead);
        double aside = Math.sqrt(Math.max(0.0, far * far - along * along));
        if (far > spread + body || along <= 0.0 || aside > along * Math.tan(HALF) + body) {
            return 0.0;
        }
        return 1.0 - (1.0 - FAR_SHARE) * Math.min(1.0, far / spread);
    }

    private static void hit(ServerLevel level, ServerPlayer owner, LivingEntity target, Vec3 ground, Vec3 ahead,
            float damage) {
        Vec3 at = target.getBoundingBox().getCenter();
        DeathStyles.mark(target, DeathStyles.Style.ASH);
        Knockdowns.brief(target);
        target.invulnerableTime = 0;
        target.hurt(level.damageSources().source(DamageTypes.LIGHTNING_BOLT, owner), damage);
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, SHOCKED, 2), owner);
        Vec3 away = new Vec3(at.x - ground.x, 0.0, at.z - ground.z);
        float more = ThorCharge.hammer(owner);
        SpellTargets.push(target, away.lengthSqr() < 0.04 ? ahead : away.normalize(), KNOCK * more, KNOCK_UP * more);
        ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, at, 14, 0.3, 0.25);
    }

    // Where it strikes: the ground under where it stopped, else there, up in the air.
    private static Vec3 ground(ServerLevel level, Vec3 at) {
        BlockHitResult floor = LoadedWorld.clip(level, new ClipContext(at.add(0.0, 0.3, 0.0),
                at.add(0.0, -GROUND_LOOK, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        return floor.getType() == HitResult.Type.MISS ? at : floor.getLocation();
    }
}
