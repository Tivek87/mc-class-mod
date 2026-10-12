package nl.tivek.multiversepowers.creature.wizard.field;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// A strike marked on the ground before it falls: a circle of light grows to its rim while its foe may still step out,
// then the strike comes down (or up) on it: a pillar of fire bursting out of the ground (PILLAR: his flame pillars), a
// shaft of light from the sky (SHAFT: the Ascended's rain of light), the void erupting out of a crack (RIFT: the
// Voidborn's eclipse) or a golden lance falling (LANCE: the Eternal's judgment). Whoever stands in it then is struck
// once. Drawn by client/field/StrikeRenderer.
public final class MarkedStrike extends FieldSpell {
    public static final int PILLAR = 0;
    public static final int SHAFT = 1;
    public static final int RIFT = 2;
    public static final int LANCE = 3;
    private static final double[] RADIUS = { 1.7, 1.4, 1.8, 1.3 };
    private static final int[] LINGER = { 16, 9, 18, 12 };
    private static final double[] TALL = { 6.0, 40.0, 5.0, 3.0 };
    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(MarkedStrike.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DELAY = SynchedEntityData.defineId(MarkedStrike.class,
            EntityDataSerializers.INT);
    private final Set<Integer> struck = new HashSet<>();

    public MarkedStrike(EntityType<? extends MarkedStrike> type, Level level) {
        super(type, level);
    }

    // One strike of `kind` on the ground at `ground`, falling `delay` ticks from now.
    public static MarkedStrike mark(ServerLevel level, Wizard wizard, Vec3 ground, int kind, int delay,
            float damage) {
        MarkedStrike strike = new MarkedStrike(WizardEntities.STRIKE.get(), level);
        strike.moveTo(ground.x, ground.y, ground.z, wizard.getRandom().nextFloat() * 360.0F, 0.0F);
        strike.castBy(wizard, damage);
        strike.entityData.set(KIND, kind);
        strike.entityData.set(DELAY, delay);
        level.addFreshEntity(strike);
        return strike;
    }

    // His flame pillars: one on his foe and `count - 1` round it, each a little later than the one before.
    public static void pillars(ServerLevel level, Wizard wizard, LivingEntity target, float damage, int count) {
        Vec3 at = target.position();
        mark(level, wizard, Wizard.groundUnder(level, at), PILLAR, 20, damage);
        double turn = wizard.getRandom().nextDouble() * Mth.TWO_PI;
        for (int k = 1; k < count; k++) {
            double angle = turn + k * Mth.TWO_PI / (count - 1);
            double far = 3.2 + wizard.getRandom().nextDouble() * 1.4;
            Vec3 spot = at.add(Math.cos(angle) * far, 0.0, Math.sin(angle) * far);
            mark(level, wizard, Wizard.groundUnder(level, spot.add(0.0, 2.0, 0.0)), PILLAR, 20 + 3 * k, damage);
        }
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.4F, 0.5F);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, PILLAR);
        builder.define(DELAY, 20);
    }

    public int kind() {
        return Mth.clamp(this.entityData.get(KIND), PILLAR, LANCE);
    }

    // Ticks until it falls.
    public int delay() {
        return this.entityData.get(DELAY);
    }

    public double radius() {
        return RADIUS[this.kind()];
    }

    public int linger() {
        return LINGER[this.kind()];
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        int delay = this.delay();
        if (wizard == null || this.tickCount > delay + this.linger()) {
            this.discard();
            return;
        }
        int kind = this.kind();
        Vec3 at = this.position();
        if (this.tickCount < delay) {
            if (this.tickCount % 4 == 0) {
                ParticleFx.ring(level, Wizard.dust(colour(kind), 1.1F), at.add(0.0, 0.15, 0.0), this.radius(), 14,
                        this.tickCount * 0.2);
            }
            if (delay - this.tickCount == 6) {
                level.playSound(null, at.x, at.y, at.z, warning(kind), SoundSource.HOSTILE, 1.2F, 1.4F);
            }
            return;
        }
        if (this.tickCount > delay + 2) {
            return;
        }
        double radius = this.radius();
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(radius + 1.0, TALL[kind],
                radius + 1.0))) {
            double far = Math.hypot(living.getX() - at.x, living.getZ() - at.z) - living.getBbWidth() * 0.5;
            double over = living.getY() - at.y;
            if (far > radius || over < -1.5 || over > TALL[kind] || !this.struck.add(living.getId())) {
                continue;
            }
            if (MonsterBlows.hurt(living, Wizard.source(this, wizard, type(kind)), this.damage,
                    WizardKnock.knock(new Vec3(0.0, kind == RIFT ? 1.0 : kind == PILLAR ? 0.8 : 0.4, 0.0)))) {
                this.after(living, wizard, kind);
            }
        }
        if (this.tickCount == delay) {
            this.burst(level, at, kind);
        }
    }

    private void after(LivingEntity living, Wizard wizard, int kind) {
        switch (kind) {
            case PILLAR -> living.igniteForSeconds(5.0F);
            case SHAFT -> living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0), wizard);
            case RIFT -> living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), wizard);
            default -> living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), wizard);
        }
    }

    private void burst(ServerLevel level, Vec3 at, int kind) {
        Vec3 up = at.add(0.0, 0.4, 0.0);
        int colour = colour(kind);
        ParticleFx.shockwave(level, Wizard.dust(colour, 1.8F), up, 36, 0.45);
        ParticleOptions rising = switch (kind) {
            case PILLAR -> ParticleTypes.FLAME;
            case SHAFT -> ParticleTypes.END_ROD;
            case RIFT -> ParticleTypes.REVERSE_PORTAL;
            default -> ParticleTypes.WAX_OFF;
        };
        ParticleFx.send(level, rising, at.x, at.y + 1.5, at.z, 30, 0.5, 1.5, 0.5, 0.06);
        if (kind == PILLAR) {
            ParticleFx.send(level, ParticleTypes.LAVA, at.x, at.y + 0.5, at.z, 6, 0.4, 0.2, 0.4, 0.0);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 1.8F, 0.5F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 1.4F, 0.7F);
        } else if (kind == SHAFT) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2F, 1.6F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0F, 0.6F);
        } else if (kind == RIFT) {
            ParticleFx.send(level, ParticleTypes.SQUID_INK, at.x, at.y + 1.0, at.z, 16, 0.4, 0.8, 0.4, 0.05);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 0.8F, 1.4F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 0.8F,
                    0.6F);
        } else {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.8F, 0.6F);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.BELL_RESONATE, SoundSource.HOSTILE, 1.0F, 1.6F);
        }
    }

    private static SoundEvent warning(int kind) {
        return switch (kind) {
            case PILLAR -> SoundEvents.FIRE_AMBIENT;
            case RIFT -> SoundEvents.SCULK_CLICKING;
            default -> SoundEvents.AMETHYST_BLOCK_RESONATE;
        };
    }

    public static int colour(int kind) {
        return switch (kind) {
            case PILLAR -> Wizard.FIRE;
            case SHAFT -> 0x9FE4FF;
            case RIFT -> Wizard.VOID;
            default -> Wizard.TIME;
        };
    }

    private static ResourceKey<DamageType> type(int kind) {
        return switch (kind) {
            case PILLAR -> WizardEntities.INFERNO;
            case SHAFT -> WizardEntities.STARFALL;
            case RIFT -> WizardEntities.VOID;
            default -> WizardEntities.ARCANE;
        };
    }
}
