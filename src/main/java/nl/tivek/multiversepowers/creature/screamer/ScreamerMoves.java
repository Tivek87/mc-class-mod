package nl.tivek.multiversepowers.creature.screamer;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.ItemAbilities;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The Screamer's moves. Close up it BITEs. From 3 to 12 blocks, every 10 seconds or so, it SCREECHes: head thrown back
// as its jaw unhinges, then a scream thrust out in a cone in front of it that stops shields working for 4 seconds and
// leaves whatever it reaches dizzy (nausea) and slow, while three ferals (Feral) climb out of the ground round it to
// answer, never more than six near. Whatever comes within 2 blocks of it it BURSTs away, 5 blocks back.
public final class ScreamerMoves extends Goal {
    public static final int BITE = 1;
    public static final int SCREECH = 2;
    public static final int BURST = 3;
    public static final int KINDS = 3;
    public static final int BITE_HIT = 6;
    public static final int BITE_END = 12;
    public static final int SCREAM = 14;
    public static final int SCREAM_END = 40;
    public static final int SCREECH_END = 48;
    public static final int BLAST = 7;
    public static final int BURST_END = 18;
    private static final double REACH = 1.3;
    private static final double CLOSE = 2.0;
    private static final double CONE = 0.55;
    private static final double SCREECH_RANGE = 12.0;
    private static final double SCREECH_LEAST = 3.0;
    private static final int SHIELDS_OFF = 80;
    private static final int CALLED = 3;
    private static final int CALLED_MOST = 6;
    private static final int HANG_BACK = 40;

    private final Screamer screamer;
    private final Set<Integer> heard = new HashSet<>();
    private int kind;
    private int t;
    private int biteReady;
    private int screechReady = 30;
    private int burstReady;
    private int repath;
    private Vec3 aim = Vec3.ZERO;

    ScreamerMoves(Screamer screamer) {
        this.screamer = screamer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = this.screamer.getTarget();
        return target != null && target.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return this.kind != 0 || this.canUse();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.screamer.setAggressive(true);
    }

    @Override
    public void stop() {
        if (this.kind != 0) {
            this.screamer.start(0);
        }
        this.kind = 0;
        this.screamer.setAggressive(false);
        this.screamer.getNavigation().stop();
    }

    @Override
    public void tick() {
        LivingEntity target = this.screamer.getTarget();
        boolean screaming = this.kind == SCREECH && this.t >= SCREAM;
        if (target != null && !screaming) {
            this.screamer.getLookControl().setLookAt(target, 30.0F, 30.0F);
        }
        if (this.kind != 0) {
            this.t++;
            this.go(target);
            return;
        }
        if (target == null) {
            return;
        }
        int now = this.screamer.tickCount;
        double far = MonsterBlows.gap(this.screamer, target);
        if (now >= this.burstReady && this.close()) {
            this.begin(BURST);
        } else if (far >= SCREECH_LEAST && far <= SCREECH_RANGE && now >= this.screechReady
                && this.screamer.getSensing().hasLineOfSight(target)) {
            this.begin(SCREECH);
        } else if (far <= REACH && now >= this.biteReady) {
            this.begin(BITE);
        } else if (--this.repath <= 0) {
            this.repath = 5;
            // With its screech nearly back it hangs back at screeching range rather than walking into bite range.
            if (now >= this.screechReady - HANG_BACK && far < SCREECH_LEAST + 2.0 && far >= SCREECH_LEAST
                    && this.screamer.getSensing().hasLineOfSight(target)) {
                this.screamer.getNavigation().stop();
            } else {
                this.screamer.getNavigation().moveTo(target, 1.1);
            }
        }
    }

    // Whether anything it would hit stands within 2 blocks of it.
    private boolean close() {
        return !MonsterBlows.foes(this.screamer, this.screamer.getBoundingBox().inflate(CLOSE, 0.5, CLOSE)).isEmpty();
    }

    private void begin(int kind) {
        this.kind = kind;
        this.t = 0;
        this.screamer.getNavigation().stop();
        this.screamer.start(kind);
        switch (kind) {
            case SCREECH -> {
                this.heard.clear();
                this.screamer.playSound(SoundEvents.ZOMBIE_VILLAGER_CONVERTED, 0.9F, 0.6F);
            }
            case BURST -> this.screamer.playSound(SoundEvents.ZOMBIE_VILLAGER_AMBIENT, 1.2F, 0.7F);
            default -> {
            }
        }
    }

    private void go(@Nullable LivingEntity target) {
        int now = this.screamer.tickCount;
        switch (this.kind) {
            case BITE -> {
                if (this.t == BITE_HIT && target != null && MonsterBlows.gap(this.screamer, target) <= REACH + 0.5) {
                    MonsterBlows.hurt(target, this.screamer.damageSources().mobAttack(this.screamer),
                            MonsterBlows.damage(this.screamer), MonsterBlows.ahead(this.screamer).scale(0.3).add(0.0,
                                    0.2, 0.0));
                    this.screamer.playSound(SoundEvents.ZOMBIE_ATTACK_IRON_DOOR, 0.4F, 1.8F);
                }
                if (this.t >= BITE_END) {
                    this.biteReady = now + 8;
                    this.end();
                }
            }
            case SCREECH -> {
                if (this.t == SCREAM) {
                    Vec3 to = target == null ? MonsterBlows.ahead(this.screamer) : target.getEyePosition().subtract(
                            this.screamer.getEyePosition());
                    this.aim = to.lengthSqr() < 1.0E-4 ? MonsterBlows.ahead(this.screamer) : to.normalize();
                    MonsterBlows.face(this.screamer, this.aim);
                    this.screamer.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 2.0F, 1.5F);
                    this.screamer.playSound(SoundEvents.GHAST_SCREAM, 1.6F, 0.6F);
                }
                if (this.t >= SCREAM && this.t < SCREAM_END) {
                    this.scream();
                }
                if (this.t == SCREAM + 6 && target != null) {
                    this.call(target);
                }
                if (this.t >= SCREECH_END) {
                    this.screechReady = now + 180 + this.screamer.getRandom().nextInt(80);
                    this.end();
                }
            }
            case BURST -> {
                if (this.t == BLAST) {
                    this.burst();
                }
                if (this.t >= BURST_END) {
                    this.burstReady = now + 100;
                    this.end();
                }
            }
            default -> this.end();
        }
    }

    private void end() {
        this.kind = 0;
        this.screamer.start(0);
    }

    // The scream rolls out in rings from its mouth; whatever stands in the cone hears it once.
    private void scream() {
        if (!(this.screamer.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 mouth = this.screamer.getEyePosition().add(0.0, -0.25, 0.0).add(this.aim.scale(0.4));
        int into = this.t - SCREAM;
        double wide = Math.tan(Math.acos(CONE)) * 0.7;
        for (int k = 0; k < 3; k++) {
            double d = 1.6 + (into * 0.9 + k * 4.0) % (SCREECH_RANGE - 1.6);
            int points = 8 + (int) (d * 1.6);
            ParticleFx.disc(level, ParticleFx.dust(0xE6DFCF, (float) (0.8 + d * 0.06)), mouth.add(this.aim.scale(d)),
                    this.aim, d * wide, points, into * 0.3 + k);
        }
        if (into % 3 == 0) {
            ParticleFx.send(level, ParticleTypes.POOF, mouth.x, mouth.y, mouth.z, 3, 0.1, 0.1, 0.1, 0.05);
        }
        if (into % 4 != 0) {
            return;
        }
        AABB box = new AABB(mouth, mouth).inflate(SCREECH_RANGE);
        for (LivingEntity heard : MonsterBlows.foes(this.screamer, box)) {
            Vec3 to = heard.getBoundingBox().getCenter().subtract(mouth);
            double far = to.length();
            if (far > SCREECH_RANGE || far > 0.5 && to.scale(1.0 / far).dot(this.aim) < CONE
                    || !this.heard.add(heard.getId())) {
                continue;
            }
            stun(heard);
            MonsterBlows.hurt(heard, MonsterBlows.source(this.screamer, MonsterBlows.SCREAM), 2.0, heard
                    .getDeltaMovement());
        }
    }

    // A shield it hears stops working for a while, dropped if it was raised; the head spins and the legs go slow.
    private static void stun(LivingEntity heard) {
        heard.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 140, 0));
        heard.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
        if (!(heard instanceof Player player)) {
            return;
        }
        boolean dropped = false;
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack held = player.getItemInHand(hand);
            if (held.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
                player.getCooldowns().addCooldown(held.getItem(), SHIELDS_OFF);
                dropped |= player.isUsingItem() && player.getUsedItemHand() == hand;
            }
        }
        if (dropped) {
            player.stopUsingItem();
            player.level().broadcastEntityEvent(player, (byte) 30);
        }
    }

    // Ferals called up out of the ground round it, on open ground within 2 to 5 blocks.
    private void call(LivingEntity target) {
        if (!(this.screamer.level() instanceof ServerLevel level)) {
            return;
        }
        int near = level.getEntitiesOfClass(Feral.class, this.screamer.getBoundingBox().inflate(24.0)).size();
        int calls = Math.min(CALLED, CALLED_MOST - near);
        float start = this.screamer.getRandom().nextFloat() * Mth.TWO_PI;
        for (int i = 0; i < calls; i++) {
            for (int tries = 0; tries < 6; tries++) {
                float angle = start + (i + tries * 0.37F) * Mth.TWO_PI / CALLED;
                double far = 2.0 + this.screamer.getRandom().nextDouble() * 3.0;
                Vec3 at = ground(level, this.screamer.getX() + Mth.cos(angle) * far, this.screamer.getY(),
                        this.screamer.getZ() + Mth.sin(angle) * far);
                if (at != null) {
                    Vec3 look = MonsterBlows.toward(this.screamer, at, target.position());
                    Feral.call(level, at, (float) Math.toDegrees(Math.atan2(-look.x, look.z)), target);
                    break;
                }
            }
        }
    }

    // A standing place on solid ground within 3 blocks up or down of `y`, null when none.
    @Nullable
    private Vec3 ground(Level level, double x, double y, double z) {
        BlockPos start = BlockPos.containing(x, y, z);
        for (int dy = 2; dy >= -3; dy--) {
            BlockPos feet = start.above(dy);
            if (level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)
                    && level.noCollision(Feral.TYPE.get().getSpawnAABB(x, feet.getY(), z))
                    && level.getFluidState(feet).isEmpty()) {
                return new Vec3(x, feet.getY(), z);
            }
        }
        return null;
    }

    // A blast of its voice throws whatever is within 2 blocks back 5, and the dust up off the ground.
    private void burst() {
        if (!(this.screamer.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 at = this.screamer.position();
        level.explode(this.screamer, null, Screamer.HARMLESS, at.x, at.y + 1.0, at.z, 1.5F, false,
                Level.ExplosionInteraction.NONE, ParticleTypes.GUST, ParticleTypes.GUST_EMITTER_SMALL,
                SoundEvents.WIND_CHARGE_BURST);
        List<LivingEntity> near = MonsterBlows.foes(this.screamer, this.screamer.getBoundingBox().inflate(CLOSE + 0.5,
                1.0, CLOSE + 0.5));
        for (LivingEntity thrown : near) {
            Vec3 way = MonsterBlows.toward(this.screamer, at, thrown.position());
            MonsterBlows.hurt(thrown, MonsterBlows.source(this.screamer, MonsterBlows.SCREAM), 3.0, new Vec3(way.x
                    * 1.05, 0.5, way.z * 1.05));
            stun(thrown);
        }
        Vec3 chest = at.add(0.0, 1.2, 0.0);
        ParticleFx.shockwave(level, ParticleTypes.POOF, at.add(0.0, 0.2, 0.0), 28, 0.45);
        ParticleFx.shockwave(level, ParticleFx.dust(0xE6DFCF, 1.6F), chest, 22, 0.6);
        ParticleFx.ring(level, ParticleFx.dust(0xE6DFCF, 1.2F), at.add(0.0, 0.15, 0.0), CLOSE, 24, 0.0);
        MonsterBlows.dust(level, at, 30, 1.5);
        this.screamer.playSound(SoundEvents.SCULK_SHRIEKER_SHRIEK, 1.6F, 2.0F);
        this.screamer.playSound(SoundEvents.RAVAGER_ROAR, 1.0F, 1.7F);
    }
}
