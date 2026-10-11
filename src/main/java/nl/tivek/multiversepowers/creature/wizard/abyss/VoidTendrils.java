package nl.tivek.multiversepowers.creature.wizard.abyss;

import javax.annotation.Nullable;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardDecoy;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.field.FieldSpell;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The void tendrils, his fourth phase: a pool of the void opens under his foe's feet, and a moment later black
// tentacles burst out of it and wind round their legs, holding them where they stand and squeezing. A foe who steps out
// of the pool in time is missed. Held, a player breaks free by struggling (trying to walk or jump away, again and
// again) or by striking the tendrils until they tear; anyone else is let go after a while. Drawn by
// client/abyss/TendrilsRenderer.
public final class VoidTendrils extends FieldSpell {
    private static final EntityDataAccessor<Integer> HELD = SynchedEntityData.defineId(VoidTendrils.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LET_GO = SynchedEntityData.defineId(VoidTendrils.class,
            EntityDataSerializers.INT);
    // The pool opens; the tendrils grab at GRAB; they hold at most HOLD ticks; they sink away in SINK.
    public static final int GRAB = 16;
    public static final int HOLD = 110;
    public static final int SINK = 14;
    private static final double POOL = 1.5;
    private static final float TOUGH = 14.0F;
    private static final int STRUGGLES = 5;
    private static final int SQUEEZE_EVERY = 20;
    @Nullable
    private LivingEntity mark;
    private Vec3 anchor = Vec3.ZERO;
    private float health = TOUGH;
    private int struggles;
    private int lastStruggle;

    public VoidTendrils(EntityType<? extends VoidTendrils> type, Level level) {
        super(type, level);
    }

    public static void rise(ServerLevel level, Wizard wizard, LivingEntity target, Vec3 ground, float damage) {
        VoidTendrils tendrils = new VoidTendrils(WizardEntities.TENDRILS.get(), level);
        tendrils.setPos(ground.x, ground.y, ground.z);
        tendrils.castBy(wizard, damage);
        tendrils.mark = target;
        level.addFreshEntity(tendrils);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 1.0F,
                0.6F);
    }

    // Who it holds (null while it opens, misses or sinks), and the tick it let go (-1 while it has not).
    @Nullable
    public Entity held() {
        int id = this.entityData.get(HELD);
        return id < 0 ? null : this.level().getEntity(id);
    }

    public int letGo() {
        return this.entityData.get(LET_GO);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(HELD, -1);
        builder.define(LET_GO, -1);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        int letGo = this.letGo();
        if (letGo >= 0) {
            if (this.tickCount > letGo + SINK) {
                this.discard();
            }
            return;
        }
        if (wizard == null || this.tickCount > GRAB + HOLD) {
            this.release(level);
            return;
        }
        Vec3 at = this.position();
        if (this.tickCount < GRAB) {
            if (this.tickCount % 2 == 0) {
                ParticleFx.ring(level, Wizard.dust(Wizard.VOID, 1.2F), at.add(0.0, 0.1, 0.0),
                        POOL * this.tickCount / GRAB, 16, this.tickCount * 0.4);
                ParticleFx.send(level, Wizard.dust(0x1A0626, 1.2F), at.x, at.y + 0.1, at.z, 2, 0.5, 0.0, 0.5, 0.0);
            }
            return;
        }
        if (this.tickCount == GRAB) {
            this.grab(level, wizard, at);
            return;
        }
        if (!(this.held() instanceof LivingEntity living) || !living.isAlive()) {
            this.release(level);
            return;
        }
        this.hold(level, wizard, living);
    }

    private void grab(ServerLevel level, Wizard wizard, Vec3 at) {
        LivingEntity target = this.mark;
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_TENDRIL_CLICKS, SoundSource.HOSTILE, 2.0F, 0.5F);
        // Dust rather than ink round whoever stands in it: ink before their eyes fills the view with black blocks.
        ParticleFx.send(level, Wizard.dust(0x1A0626, 1.4F), at.x, at.y + 0.3, at.z, 24, 0.6, 0.2, 0.6, 0.0);
        if (target == null || !target.isAlive() || target.distanceToSqr(at.x, target.getY(), at.z) > POOL * POOL
                || Math.abs(target.getY() - at.y) > 1.6) {
            this.release(level);
            return;
        }
        this.anchor = target.position();
        this.lastStruggle = this.tickCount;
        this.entityData.set(HELD, target.getId());
        this.setPos(this.anchor.x, this.anchor.y, this.anchor.z);
        target.hurt(Wizard.source(this, wizard, WizardEntities.VOID), this.damage);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_ATTACK, SoundSource.HOSTILE, 2.0F, 0.4F);
    }

    // Holding them where they stand: a player trying to get away is pulled back, and every try is a struggle.
    private void hold(ServerLevel level, Wizard wizard, LivingEntity living) {
        Vec3 now = living.position();
        double away = Math.sqrt(now.distanceToSqr(this.anchor.x, now.y, this.anchor.z));
        boolean tried = away > 0.3 || now.y > this.anchor.y + 0.3;
        if (tried && this.tickCount - this.lastStruggle >= 4) {
            this.lastStruggle = this.tickCount;
            if (living instanceof ServerPlayer && ++this.struggles >= STRUGGLES) {
                this.release(level);
                return;
            }
            level.playSound(null, now.x, now.y, now.z, SoundEvents.SLIME_SQUISH, SoundSource.HOSTILE, 1.2F, 0.6F);
        }
        if (tried) {
            if (living instanceof ServerPlayer player) {
                player.connection.teleport(this.anchor.x, this.anchor.y, this.anchor.z, player.getYRot(),
                        player.getXRot());
            } else {
                living.setPos(this.anchor.x, this.anchor.y, this.anchor.z);
            }
        }
        living.setDeltaMovement(0.0, Math.min(0.0, living.getDeltaMovement().y), 0.0);
        living.hurtMarked = true;
        if ((this.tickCount - GRAB) % SQUEEZE_EVERY == 0) {
            living.hurt(Wizard.source(this, wizard, WizardEntities.VOID), this.damage);
            if (living instanceof ServerPlayer) {
                living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 40, 0, false, false), wizard);
            }
            ParticleFx.send(level, Wizard.dust(0x1A0626, 1.0F), now.x, now.y + 0.3, now.z, 8, 0.35, 0.2, 0.35, 0.0);
        }
    }

    private void release(ServerLevel level) {
        if (this.letGo() >= 0) {
            return;
        }
        this.entityData.set(HELD, -1);
        this.entityData.set(LET_GO, this.tickCount);
        Vec3 at = this.position();
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SCULK_BLOCK_BREAK, SoundSource.HOSTILE, 1.5F, 0.6F);
    }

    // Struck, the tendrils tear (none but his foes may strike them).
    @Override
    public boolean hurt(DamageSource source, float amount) {
        Entity by = source.getEntity();
        if (by instanceof Wizard || by instanceof WizardDecoy || this.letGo() >= 0 || this.held() == null
                || source.is(DamageTypeTags.IS_FALL) || !(this.level() instanceof ServerLevel level)) {
            return false;
        }
        this.health -= amount;
        Vec3 at = this.position().add(0.0, 0.6, 0.0);
        ParticleFx.send(level, Wizard.dust(0x1A0626, 1.4F), at.x, at.y, at.z, 10, 0.25, 0.3, 0.25, 0.0);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.SLIME_HURT, SoundSource.HOSTILE, 1.5F, 0.5F);
        if (this.health <= 0.0F) {
            this.release(level);
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return this.held() != null && this.letGo() < 0;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }
}
