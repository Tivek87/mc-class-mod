package nl.tivek.multiversepowers.creature.wizard.field;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.MonsterBlows;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardEntities;
import nl.tivek.multiversepowers.creature.wizard.WizardKnock;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The arcane tornado, his second phase: a funnel of violet wind roams the ground after his foe, wandering as it goes,
// dragging in whoever stands near. Whoever it catches is swept up round and round inside it, higher and higher, and
// flung out of its top, far, to land hard. It blows itself out after a while. Drawn by client/field/TornadoRenderer.
public final class ArcaneTornado extends FieldSpell {
    public static final int LIFE = 170;
    public static final double TALL = 7.0;
    private static final double SPEED = 0.17;
    private static final double PULL = 7.0;
    private static final double CORE = 1.7;
    // Ticks a foe is swept round before it is flung out, and before it can be caught again.
    private static final int SWEPT = 28;
    private static final int AGAIN = 50;
    private final Map<Integer, Integer> caught = new HashMap<>();
    @Nullable
    private LivingEntity chases;

    public ArcaneTornado(EntityType<? extends ArcaneTornado> type, Level level) {
        super(type, level);
    }

    public static void raise(ServerLevel level, Wizard wizard, Vec3 ground, @Nullable LivingEntity target,
            float damage) {
        ArcaneTornado tornado = new ArcaneTornado(WizardEntities.TORNADO.get(), level);
        tornado.setPos(ground.x, ground.y, ground.z);
        tornado.castBy(wizard, damage);
        tornado.chases = target;
        level.addFreshEntity(tornado);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 2.0F, 0.5F);
        level.playSound(null, ground.x, ground.y, ground.z, SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 2.0F,
                0.6F);
    }

    // How strong it blows `age` ticks in: it gathers, blows and dies down (0 to 1).
    public static float strength(float age) {
        return Mth.clamp(age / 16.0F, 0.0F, 1.0F) * Mth.clamp((LIFE - age) / 20.0F, 0.0F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.owner(level);
        if (wizard == null || this.tickCount > LIFE) {
            this.discard();
            return;
        }
        this.roam(level, wizard);
        float strength = strength(this.tickCount);
        Vec3 at = this.position();
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at).inflate(PULL, TALL, PULL))) {
            if (!Wizard.ally(living)) {
                this.blow(level, wizard, living, at, strength);
            }
        }
        if (this.tickCount % 3 == 0) {
            ParticleFx.helix(level, Wizard.dust(Wizard.VIOLET, 1.6F), at, 1.2 + 0.5 * strength, TALL * strength, 2.0,
                    12, this.tickCount * 0.5);
            ParticleFx.send(level, ParticleTypes.CLOUD, at.x, at.y + 0.3, at.z, 3, 1.0, 0.1, 1.0, 0.04);
        }
        if (this.tickCount % 20 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 1.2F * strength,
                    0.6F);
        }
    }

    // After its foe, wandering this way and that, along the ground.
    private void roam(ServerLevel level, Wizard wizard) {
        LivingEntity target = this.chases != null && this.chases.isAlive() ? this.chases : wizard.getTarget();
        Vec3 at = this.position();
        Vec3 way = target == null ? Vec3.ZERO : target.position().subtract(at).multiply(1.0, 0.0, 1.0);
        double wander = Math.sin(this.tickCount * 0.11 + this.getId()) * 0.9;
        way = way.lengthSqr() < 1.0E-3 ? Vec3.ZERO : way.normalize();
        Vec3 side = new Vec3(-way.z, 0.0, way.x).scale(wander);
        Vec3 next = at.add(way.add(side).scale(SPEED));
        BlockHitResult down = LoadedWorld.clip(level, new ClipContext(next.add(0.0, 2.0, 0.0), next.add(0.0, -6.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, this));
        double y = down.getType() == HitResult.Type.MISS ? at.y : down.getLocation().y;
        this.setPos(next.x, Mth.lerp(0.3, at.y, y), next.z);
    }

    // Dragged in from near it; swept round and up inside it; flung out of its top.
    private void blow(ServerLevel level, Wizard wizard, LivingEntity living, Vec3 at, float strength) {
        int id = living.getId();
        Integer since = this.caught.get(id);
        Vec3 off = new Vec3(living.getX() - at.x, 0.0, living.getZ() - at.z);
        double far = off.length();
        double over = living.getY() - at.y;
        Vec3 out = far < 1.0E-3 ? new Vec3(1.0, 0.0, 0.0) : off.scale(1.0 / far);
        Vec3 round = new Vec3(-out.z, 0.0, out.x);
        if (since != null && since < 0) {
            // Flung out a moment ago: it cannot catch them again just yet.
            this.caught.put(id, since + 1);
            return;
        }
        if (since == null) {
            if (far < CORE && over > -1.0 && over < TALL && strength > 0.5F) {
                this.caught.put(id, 0);
                living.hurt(Wizard.source(this, wizard, WizardEntities.ARCANE), this.damage * 0.3F);
                level.playSound(null, living.getX(), living.getY(), living.getZ(), SoundEvents.BREEZE_JUMP,
                        SoundSource.HOSTILE, 1.4F, 0.6F);
            } else if (far < PULL) {
                double pull = 0.07 * strength * (1.0 - far / PULL) + 0.01;
                living.setDeltaMovement(living.getDeltaMovement().add(out.scale(-pull)).add(round.scale(pull * 0.6)));
                living.hurtMarked = true;
            }
            return;
        }
        int swept = since + 1;
        if (swept < SWEPT && over < TALL - 0.5) {
            this.caught.put(id, swept);
            double want = 1.1 + 0.03 * swept;
            Vec3 in = out.scale((want - far) * 0.4);
            living.setDeltaMovement(round.scale(0.55).add(in).add(0.0, 0.32, 0.0));
            living.fallDistance = 0.0F;
            living.hurtMarked = true;
            return;
        }
        this.caught.put(id, -AGAIN);
        MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.ARCANE), this.damage,
                WizardKnock.knock(out.scale(1.9).add(round.scale(0.6)).add(0.0, 0.9, 0.0)));
        Vec3 top = living.position().add(0.0, 0.5, 0.0);
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.VIOLET, 1.5F), top, 20, 0.3);
        level.playSound(null, top.x, top.y, top.z, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE,
                1.6F, 0.7F);
    }
}
