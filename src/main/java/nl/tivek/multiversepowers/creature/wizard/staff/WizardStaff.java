package nl.tivek.multiversepowers.creature.wizard.staff;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The Wizard's staff out of his hand. Hurled as a spear (THROWN) it flies crystal first, piercing whoever it meets,
// and sticks fast where it strikes (STUCK) until he calls it home (RETURNING), spinning back through anyone in its way
// into his hand. Set free (FREE) it fights on its own beside him with a boss bar of its own: it circles his foe and
// casts or strikes (StaffActs) until he calls it back, its time runs out, or it is broken (BROKEN: it shatters, and
// reforms in his hand a while later). At rest he plants it (PLANTED), leans it on a wall (LEANED) or sits on it in
// the air (SEAT), placed by WizardPastimes. While it is out he casts from his bare hands (Wizard.staffOut).
public final class WizardStaff extends Entity {
    public static final int THROWN = 0;
    public static final int STUCK = 1;
    public static final int RETURNING = 2;
    public static final int FREE = 3;
    public static final int BROKEN = 4;
    public static final int PLANTED = 5;
    public static final int LEANED = 6;
    public static final int SEAT = 7;
    // Its middle is this high over its feet (the box is upright whatever way it points); its crystal and its foot this
    // far from its middle along it.
    public static final double MIDDLE = 1.2;
    public static final double CRYSTAL = 1.16;
    public static final double FOOT = 1.31;
    private static final double THROW_SPEED = 1.6;
    private static final double FALL = 0.025;
    private static final double REACH = 0.6;
    private static final int FLIGHT = 80;
    private static final int FREE_LIFE = 900;
    private static final int REFORM = 100;
    private static final float HEALTH = 40.0F;
    private static final float HEALTH_PER_PHASE = 18.0F;
    private static final Map<Wizard, WizardStaff> OUT = new WeakHashMap<>();
    private static final EntityDataAccessor<Integer> OWNER = SynchedEntityData.defineId(WizardStaff.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(WizardStaff.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(WizardStaff.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACT = SynchedEntityData.defineId(WizardStaff.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ACT_AT = SynchedEntityData.defineId(WizardStaff.class,
            EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HURT_AT = SynchedEntityData.defineId(WizardStaff.class,
            EntityDataSerializers.INT);
    private final ServerBossEvent bar = new ServerBossEvent(Component.translatable("entity.welcomescreen.wizard_staff"),
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<Integer> struck = new HashSet<>();
    @Nullable
    private Wizard wizard;
    private float health;
    private float most;
    private int modeAt;
    private int nextAct = 30;
    private double circle;
    // Where its ray points now, following its foe slowly (StaffActs).
    Vec3 aim = Vec3.ZERO;

    public WizardStaff(EntityType<? extends WizardStaff> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.bar.setVisible(false);
    }

    // His staff while it is out of his hand, on the server.
    @Nullable
    public static WizardStaff of(Wizard wizard) {
        WizardStaff staff = OUT.get(wizard);
        return staff != null && !staff.isRemoved() ? staff : null;
    }

    public static void clear() {
        OUT.clear();
    }

    private static WizardStaff make(ServerLevel level, Wizard wizard, int mode, Vec3 middle) {
        WizardStaff old = of(wizard);
        if (old != null) {
            old.discard();
        }
        WizardStaff staff = new WizardStaff(WizardEntities.STAFF.get(), level);
        staff.wizard = wizard;
        staff.entityData.set(OWNER, wizard.getId());
        staff.entityData.set(PHASE, wizard.phase());
        staff.setMiddle(middle);
        staff.setMode(mode);
        staff.point(wizard.getViewVector(1.0F));
        level.addFreshEntity(staff);
        OUT.put(wizard, staff);
        wizard.setStaffOut(true);
        return staff;
    }

    // Hurled at `target` (or along his look) from his right hand.
    public static void hurl(ServerLevel level, Wizard wizard, @Nullable LivingEntity target) {
        Vec3 from = wizard.at(0.45, 1.6, 0.5);
        Vec3 way = wizard.getViewVector(1.0F);
        if (target != null) {
            Vec3 to = target.getBoundingBox().getCenter();
            double time = from.distanceTo(to) / THROW_SPEED;
            to = to.add(target.getDeltaMovement().multiply(time, 0.0, time)).add(0.0, FALL * time * time * 0.5,
                    0.0);
            way = to.subtract(from).normalize();
        }
        WizardStaff staff = make(level, wizard, THROWN, from);
        staff.point(way);
        staff.setDeltaMovement(way.scale(THROW_SPEED));
        level.playSound(null, from.x, from.y, from.z, SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.6F,
                0.7F);
        level.playSound(null, from.x, from.y, from.z, SoundEvents.ILLUSIONER_CAST_SPELL, SoundSource.HOSTILE, 1.2F,
                1.4F);
    }

    // Set free to fight on its own: it leaps out of his hand into the air over him.
    public static void free(ServerLevel level, Wizard wizard) {
        WizardStaff staff = make(level, wizard, FREE, wizard.at(0.45, 1.8, 0.6));
        staff.point(new Vec3(0.0, 1.0, 0.0));
        staff.setDeltaMovement(0.0, 0.5, 0.0);
        staff.arm(level, wizard);
    }

    // Stuck fast where he hurled it, it wrenches itself out to fight on its own.
    public void wake() {
        if (this.mode() != STUCK || this.wizard == null || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.setDeltaMovement(this.way().scale(-0.4).add(0.0, 0.3, 0.0));
        this.setMode(FREE);
        this.arm(level, this.wizard);
    }

    // Whole and awake as it starts to fight on its own.
    private void arm(ServerLevel level, Wizard wizard) {
        this.most = HEALTH + HEALTH_PER_PHASE * wizard.phase();
        this.health = this.most;
        this.nextAct = this.tickCount + 30;
        this.bar.setProgress(1.0F);
        this.bar.setVisible(true);
        Vec3 at = this.middle();
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.light(wizard.phase()), 1.6F), at, 40, 0.3);
        ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, 16, 0.2, 0.6, 0.2, 0.08);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1.8F, 1.3F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2.0F, 0.6F);
    }

    // Laid out of his hand to stand by itself at rest (PLANTED, LEANED, SEAT), its middle at `middle` pointing `way`.
    public static WizardStaff lay(ServerLevel level, Wizard wizard, int mode, Vec3 middle, Vec3 way) {
        WizardStaff staff = make(level, wizard, mode, middle);
        staff.point(way);
        return staff;
    }

    public int mode() {
        return this.entityData.get(MODE);
    }

    private void setMode(int mode) {
        this.entityData.set(MODE, mode);
        this.modeAt = this.tickCount;
        this.struck.clear();
        this.bar.setVisible(mode == FREE);
    }

    // Ticks in its mode now.
    public int inMode() {
        return this.tickCount - this.modeAt;
    }

    public int phase() {
        return this.entityData.get(PHASE);
    }

    public int act() {
        return this.entityData.get(ACT);
    }

    // Ticks into its act, on any side.
    public float actAge(float partialTick) {
        return this.tickCount + partialTick - this.entityData.get(ACT_AT);
    }

    public int hurtAt() {
        return this.entityData.get(HURT_AT);
    }

    @Nullable
    public Entity owner() {
        return this.level().getEntity(this.entityData.get(OWNER));
    }

    public Vec3 middle() {
        return this.position().add(0.0, MIDDLE, 0.0);
    }

    public void setMiddle(Vec3 middle) {
        this.setPos(middle.x, middle.y - MIDDLE, middle.z);
    }

    // The way its crystal points.
    public Vec3 way() {
        return Vec3.directionFromRotation(this.getXRot(), this.getYRot());
    }

    public Vec3 tip() {
        return this.middle().add(this.way().scale(CRYSTAL));
    }

    // Turns its crystal along `way`.
    public void point(Vec3 way) {
        if (way.lengthSqr() < 1.0E-8) {
            return;
        }
        Vec3 w = way.normalize();
        this.setYRot((float) (Mth.atan2(-w.x, w.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (-Math.asin(Mth.clamp(w.y, -1.0, 1.0)) * Mth.RAD_TO_DEG));
    }

    // Called home: it flies back into his hand.
    public void call() {
        int mode = this.mode();
        if (mode == RETURNING || mode == BROKEN) {
            return;
        }
        this.setAct(0);
        this.setMode(RETURNING);
        Vec3 at = this.middle();
        this.level().playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_RETURN, SoundSource.HOSTILE, 1.6F, 0.8F);
    }

    // Back in his hand at once (a pastime over, or he is gone).
    public void putAway() {
        if (this.wizard != null) {
            this.wizard.setStaffOut(false);
            OUT.remove(this.wizard, this);
        }
        this.discard();
    }

    void setAct(int act) {
        this.entityData.set(ACT, act);
        this.entityData.set(ACT_AT, this.tickCount);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER, -1);
        builder.define(MODE, THROWN);
        builder.define(PHASE, 1);
        builder.define(ACT, 0);
        builder.define(ACT_AT, 0);
        builder.define(HURT_AT, -100);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        Wizard wizard = this.wizard;
        if (wizard == null || !wizard.isAlive() || wizard.isRemoved() || wizard.level() != level) {
            this.crumble(level);
            return;
        }
        this.entityData.set(PHASE, wizard.phase());
        switch (this.mode()) {
            case THROWN -> this.fly(level, wizard);
            case RETURNING -> this.home(level, wizard);
            case FREE -> this.fight(level, wizard);
            case BROKEN -> {
                this.setMiddle(wizard.at(0.45, 1.4, 0.5));
                if (this.inMode() >= REFORM) {
                    this.reform(level, wizard);
                }
            }
            default -> {
            }
        }
    }

    // Flying as a spear: falling a little, piercing whoever it meets, sticking fast in the first block.
    private void fly(ServerLevel level, Wizard wizard) {
        Vec3 v = this.getDeltaMovement().add(0.0, -FALL, 0.0);
        this.point(v);
        Vec3 tip = this.tip();
        Vec3 next = tip.add(v);
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(tip, next, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? next : hit.getLocation();
        float harm = wizard.harm(WizardMove.STAFF_THROW);
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(tip, end).inflate(REACH))) {
            if (this.struck.add(living.getId())) {
                Vec3 push = WizardKnock.knock(v.normalize().scale(0.9).add(0.0, 0.3, 0.0));
                MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.ARCANE), harm, push);
                Vec3 at = living.getBoundingBox().getCenter();
                ParticleFx.send(level, ParticleTypes.CRIT, at.x, at.y, at.z, 14, 0.3, 0.4, 0.3, 0.3);
                ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.4F), at.x, at.y, at.z, 10, 0.3,
                        0.4, 0.3, 0.0);
                level.playSound(null, at.x, at.y, at.z, SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.4F, 0.8F);
                v = v.scale(0.8);
            }
        }
        if (hit.getType() != HitResult.Type.MISS) {
            this.setMiddle(end.subtract(v.normalize().scale(CRYSTAL - 0.25)));
            this.setDeltaMovement(Vec3.ZERO);
            this.setMode(STUCK);
            ParticleFx.send(level, ParticleTypes.CRIT, end.x, end.y, end.z, 10, 0.2, 0.2, 0.2, 0.2);
            ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.2F), end.x, end.y, end.z, 8, 0.2,
                    0.2, 0.2, 0.0);
            level.playSound(null, end.x, end.y, end.z, SoundEvents.TRIDENT_HIT_GROUND, SoundSource.HOSTILE, 1.4F,
                    0.7F);
            return;
        }
        this.setMiddle(this.middle().add(v));
        this.setDeltaMovement(v);
        if (this.tickCount % 2 == 0) {
            ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.0F), tip.x, tip.y, tip.z, 2, 0.05,
                    0.05, 0.05, 0.0);
        }
        if (this.inMode() > FLIGHT) {
            this.call();
        }
    }

    // Spinning home to his hand, faster as it comes, through anyone in its way.
    private void home(ServerLevel level, Wizard wizard) {
        Vec3 hand = wizard.at(0.45, 1.4, 0.5);
        Vec3 at = this.middle();
        Vec3 to = hand.subtract(at);
        double far = to.length();
        double speed = Math.min(2.2, 0.5 + this.inMode() * 0.12);
        if (far <= Math.max(1.0, speed)) {
            this.caught(level, wizard, hand);
            return;
        }
        Vec3 step = to.scale(speed / far);
        float harm = wizard.harm(WizardMove.STAFF_CALL);
        for (LivingEntity living : MonsterBlows.foes(wizard, new AABB(at, at.add(step)).inflate(REACH + 0.4))) {
            if (this.struck.add(living.getId())) {
                Vec3 push = WizardKnock.knock(step.normalize().scale(0.7).add(0.0, 0.35, 0.0));
                MonsterBlows.hurt(living, Wizard.source(this, wizard, WizardEntities.ARCANE), harm, push);
                Vec3 c = living.getBoundingBox().getCenter();
                ParticleFx.send(level, ParticleTypes.CRIT, c.x, c.y, c.z, 12, 0.3, 0.4, 0.3, 0.3);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_HIT, SoundSource.HOSTILE, 1.2F, 1.1F);
            }
        }
        this.setMiddle(at.add(step));
        this.point(step);
        if (this.tickCount % 2 == 0) {
            ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.1F), at.x, at.y, at.z, 3, 0.2, 0.2,
                    0.2, 0.0);
        }
    }

    private void caught(ServerLevel level, Wizard wizard, Vec3 hand) {
        ParticleFx.send(level, Wizard.dust(Wizard.light(wizard.phase()), 1.4F), hand.x, hand.y, hand.z, 12, 0.2,
                0.3, 0.2, 0.0);
        ParticleFx.send(level, ParticleTypes.END_ROD, hand.x, hand.y, hand.z, 6, 0.1, 0.2, 0.1, 0.05);
        level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), SoundSource.HOSTILE,
                1.4F, 0.8F);
        level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.4F,
                0.9F);
        this.putAway();
    }

    // Free: circling his foe and casting or striking, until its time runs out or he has no foe.
    private void fight(ServerLevel level, Wizard wizard) {
        LivingEntity target = wizard.getTarget();
        if (target == null || !target.isAlive() || this.inMode() > FREE_LIFE) {
            this.call();
            return;
        }
        int act = this.act();
        if (act != 0) {
            int t = (int) this.actAge(0.0F);
            if (t >= StaffActs.length(act)) {
                this.setAct(0);
                this.nextAct = this.tickCount + 14 + this.random.nextInt(18);
            } else {
                StaffActs.tick(level, this, wizard, target, act, t);
                return;
            }
        }
        this.circle += 0.045;
        Vec3 round = new Vec3(Math.cos(this.circle) * 4.5, 2.2 + 0.4 * Math.sin(this.circle * 2.3),
                Math.sin(this.circle) * 4.5);
        this.steer(target.position().add(round), 0.45);
        this.point(new Vec3(0.0, 1.0, 0.0).add(target.getBoundingBox().getCenter().subtract(this.middle())
                .normalize().scale(0.45)));
        if (this.tickCount >= this.nextAct) {
            double far = this.middle().distanceTo(target.getBoundingBox().getCenter());
            this.setAct(StaffActs.pick(this.random, far));
            this.aim = target.getBoundingBox().getCenter();
        }
    }

    // Glides its middle towards `to`, at most `speed` a tick, easing in.
    void steer(Vec3 to, double speed) {
        Vec3 at = this.middle();
        Vec3 way = to.subtract(at);
        double far = way.length();
        if (far < 1.0E-4) {
            return;
        }
        Vec3 want = way.scale(Math.min(speed, far * 0.3) / far);
        Vec3 v = this.getDeltaMovement().scale(0.6).add(want.scale(0.4));
        this.setDeltaMovement(v);
        this.setMiddle(at.add(v));
    }

    @Override
    public boolean isPickable() {
        return this.mode() == FREE;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel level) || this.mode() != FREE || this.isRemoved()) {
            return false;
        }
        Entity by = source.getEntity();
        if (by != null && Wizard.ally(by) || this.isInvulnerableTo(source)) {
            return false;
        }
        if (this.tickCount - this.hurtAt() < 8) {
            return false;
        }
        this.entityData.set(HURT_AT, this.tickCount);
        this.health -= amount;
        this.bar.setProgress(Math.max(0.0F, this.health / this.most));
        Vec3 at = this.middle();
        ParticleFx.send(level, ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.2, 0.6, 0.2, 0.2);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1.4F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.WOOD_HIT, SoundSource.HOSTILE, 1.2F, 0.7F);
        if (by != null) {
            Vec3 away = at.subtract(by.position()).multiply(1.0, 0.0, 1.0);
            if (away.lengthSqr() > 1.0E-4) {
                this.setDeltaMovement(this.getDeltaMovement().add(away.normalize().scale(0.6)).add(0.0, 0.2, 0.0));
            }
        }
        if (this.health <= 0.0F) {
            this.shatter(level);
        }
        return true;
    }

    // Broken: it bursts into splinters and shards, and reforms in his hand a while later.
    private void shatter(ServerLevel level) {
        Vec3 at = this.middle();
        ParticleFx.send(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.STICK)), at.x, at.y,
                at.z, 30, 0.2, 0.9, 0.2, 0.2);
        ParticleFx.send(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.AMETHYST_SHARD)), at.x,
                at.y, at.z, 30, 0.2, 0.6, 0.2, 0.25);
        ParticleFx.sphereOut(level, Wizard.dust(Wizard.light(this.phase()), 1.8F), at, 40, 0.4);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.ITEM_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2.0F, 0.7F);
        this.setAct(0);
        this.setDeltaMovement(Vec3.ZERO);
        this.setMode(BROKEN);
    }

    // Grown again in his hand out of motes of light.
    private void reform(ServerLevel level, Wizard wizard) {
        Vec3 hand = wizard.at(0.45, 1.4, 0.5);
        ParticleFx.implosion(level, Wizard.dust(Wizard.light(wizard.phase()), 1.4F), hand.add(0.0, 0.6, 0.0), 2.0,
                30, 0.3);
        level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.6F,
                1.2F);
        this.putAway();
    }

    // Its Wizard gone: it falls apart where it is.
    private void crumble(ServerLevel level) {
        Vec3 at = this.middle();
        if (this.mode() != BROKEN) {
            ParticleFx.send(level, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.STICK)), at.x,
                    at.y, at.z, 20, 0.2, 0.8, 0.2, 0.1);
            ParticleFx.send(level, ParticleTypes.END_ROD, at.x, at.y, at.z, 10, 0.2, 0.6, 0.2, 0.05);
        }
        this.discard();
    }

    @Override
    public void remove(RemovalReason reason) {
        super.remove(reason);
        this.bar.removeAllPlayers();
        if (this.wizard != null) {
            OUT.remove(this.wizard, this);
        }
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bar.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bar.removePlayer(player);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 96.0 * 96.0;
    }
}
