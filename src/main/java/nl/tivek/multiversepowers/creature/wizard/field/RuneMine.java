package nl.tivek.multiversepowers.creature.wizard.field;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The rune circle, his second phase: seven runes burn into the ground, one under his foe and six in a ring round
// them. Each glows brighter and spins faster until it goes off (`fuse`) in a pillar of violet light that throws
// whoever stands on it into the air; the one in the middle first, then the ring one after another round, so there is
// time to step off and through. Drawn by client/fx/RuneRenderer.
public final class RuneMine extends FieldSpell {
    public static final double RADIUS = 1.6;
    public static final int PILLAR = 12;
    private static final int RING = 6;
    private static final double RING_RADIUS = 3.4;
    private static final int FIRST = 26;
    private static final int NEXT = 3;
    private static final EntityDataAccessor<Integer> FUSE = SynchedEntityData.defineId(RuneMine.class,
            EntityDataSerializers.INT);

    public RuneMine(EntityType<? extends RuneMine> type, Level level) {
        super(type, level);
    }

    public static void ring(ServerLevel level, Wizard wizard, Vec3 middle, float damage) {
        lay(level, wizard, middle, damage, FIRST);
        double turn = wizard.getRandom().nextDouble() * Mth.TWO_PI;
        for (int i = 0; i < RING; i++) {
            double angle = turn + i * Mth.TWO_PI / RING;
            Vec3 at = middle.add(Math.cos(angle) * RING_RADIUS, 0.0, Math.sin(angle) * RING_RADIUS);
            lay(level, wizard, at, damage, FIRST + 8 + i * NEXT);
        }
        level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE,
                2.0F, 0.6F);
    }

    private static void lay(ServerLevel level, Wizard wizard, Vec3 at, float damage, int fuse) {
        RuneMine rune = new RuneMine(WizardEntities.RUNE.get(), level);
        rune.setPos(at.x, at.y, at.z);
        rune.castBy(wizard, damage);
        rune.entityData.set(FUSE, fuse);
        level.addFreshEntity(rune);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FUSE, FIRST);
    }

    // The tick it goes off.
    public int fuse() {
        return this.entityData.get(FUSE);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount == this.fuse()) {
            this.blow(level);
        }
        if (this.tickCount >= this.fuse() + PILLAR) {
            this.discard();
        }
    }

    private void blow(ServerLevel level) {
        Vec3 at = this.position();
        Wizard wizard = this.owner(level);
        if (wizard != null) {
            for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(RADIUS, 0.0, RADIUS)
                    .expandTowards(0.0, 3.0, 0.0))) {
                if (Math.hypot(living.getX() - at.x, living.getZ() - at.z) <= RADIUS + living.getBbWidth() * 0.5) {
                    MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.ARCANE), this.damage,
                            WizardKnock.knock(new Vec3(0.0, 0.95, 0.0)));
                }
            }
        }
        ParticleFx.helix(level, ParticleTypes.END_ROD, at, 0.6, 5.0, 2.0, 24, this.random.nextDouble() * 6.0);
        ParticleFx.send(level, Wizard.dust(Wizard.VIOLET, 2.0F), at.x, at.y + 1.0, at.z, 24, 0.4, 1.0, 0.4, 0.0);
        ParticleFx.send(level, ParticleTypes.WITCH, at.x, at.y + 0.3, at.z, 12, 0.6, 0.1, 0.6, 0.1);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_FANGS_ATTACK, SoundSource.HOSTILE, 1.4F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.2F, 0.5F);
    }
}
