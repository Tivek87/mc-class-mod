package nl.tivek.multiversepowers.creature.wizard.abyss;

import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.field.FieldSpell;
import nl.tivek.multiversepowers.creature.wizard.spell.SpellBlast;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The void orb, his fourth phase: a small black hole let go from his staff that drifts slowly after his foe, through
// walls and all, growing as it goes, dragging everyone near it in and gnawing at whoever it touches. At the end it falls
// in on itself and bursts. The way to live is to keep running from it. Drawn by client/abyss/VoidOrbRenderer.
public final class VoidOrb extends FieldSpell {
    public static final int LIFE = 120;
    // It falls in on itself over the last COLLAPSE ticks, and bursts at LIFE.
    public static final int COLLAPSE = 14;
    private static final double SPEED = 0.13;
    private static final double PULL = 11.0;
    private static final double BITE = 1.6;
    private static final double BURST = 6.0;
    @Nullable
    private LivingEntity chases;

    public VoidOrb(EntityType<? extends VoidOrb> type, Level level) {
        super(type, level);
    }

    public static void loose(ServerLevel level, Wizard wizard, Vec3 from, @Nullable LivingEntity target,
            float damage) {
        VoidOrb orb = new VoidOrb(WizardEntities.VOID_ORB.get(), level);
        orb.setPos(from.x, from.y - 0.5, from.z);
        orb.castBy(wizard, damage);
        orb.chases = target;
        level.addFreshEntity(orb);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.WITHER_SHOOT, SoundSource.HOSTILE, 1.6F, 0.4F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, 1.0F, 1.6F);
    }

    // How big it has grown (0 to 1) `age` ticks in, and how far it has fallen in on itself (0 to 1).
    public static float size(float age) {
        float grow = Mth.clamp(age / 30.0F, 0.0F, 1.0F);
        return (0.35F + 0.65F * grow * grow) * (1.0F - 0.85F * collapse(age));
    }

    public static float collapse(float age) {
        return Mth.clamp((age - (LIFE - COLLAPSE)) / COLLAPSE, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null) {
            this.discard();
            return;
        }
        Vec3 at = this.position().add(0.0, 0.5, 0.0);
        if (this.tickCount >= LIFE) {
            this.burst(level, wizard, at);
            return;
        }
        LivingEntity target = this.chases != null && this.chases.isAlive() ? this.chases : wizard.getTarget();
        if (target != null && this.tickCount < LIFE - COLLAPSE) {
            Vec3 way = target.getBoundingBox().getCenter().subtract(at);
            if (way.lengthSqr() > 0.25) {
                Vec3 step = way.normalize().scale(SPEED * (1.0 + this.tickCount / (double) LIFE));
                this.setPos(this.getX() + step.x, this.getY() + step.y * 0.5, this.getZ() + step.z);
            }
        }
        float size = size(this.tickCount);
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(PULL))) {
            Vec3 in = at.subtract(living.getBoundingBox().getCenter());
            double far = in.length();
            if (far > PULL || far < 1.0E-3 || Wizard.ally(living)) {
                continue;
            }
            double pull = (0.035 + 0.05 * size) * (1.0 - far / PULL) * (this.tickCount < LIFE - COLLAPSE ? 1.0 : 2.0);
            living.setDeltaMovement(living.getDeltaMovement().add(in.scale(pull / far)));
            living.hurtMarked = true;
            if (far < BITE * (0.6 + size) && this.tickCount % 10 == 0) {
                living.hurt(Wizard.source(this, wizard, WizardEntities.VOID), this.damage * 0.2F);
            }
        }
        if (this.tickCount % 2 == 0) {
            ParticleFx.implosion(level, Wizard.dust(Wizard.VOID, 1.3F), at, 3.5 + 2.0 * size, 6, 0.3);
            ParticleFx.send(level, ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 4, 1.2, 1.2, 1.2, 0.05);
        }
        if (this.tickCount % 24 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
        if (this.tickCount == LIFE - COLLAPSE) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2.0F, 0.5F);
        }
    }

    private void burst(ServerLevel level, Wizard wizard, Vec3 at) {
        SpellBlast.burst(level, this, wizard, at, 3.0F, false, BURST, this.damage, WizardEntities.VOID);
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.VOID, 2.2F), at, 90, 0.7);
        ParticleFx.sphereOut(level, ParticleTypes.REVERSE_PORTAL, at, 60, 0.9);
        ParticleFx.shockwave(level, Wizard.dust(0x2A0838, 2.4F), at, 64, 0.8);
        ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, at, 30, 0.7);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.5F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.0F, 0.6F);
        this.discard();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 160.0 * 160.0;
    }
}
