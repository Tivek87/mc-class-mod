package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.RevolverDuo;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

public final class RevolverAssembly implements Effect {
    private static final String KEY = "revolver";
    private static final double[] STAGES = { 1.0, 0.82, 0.64, 0.46 };
    private static final double VIEW_RANGE = 128.0;
    private static final double AIM_PULL = 0.4;
    private static final Vec3 RESTING = new Vec3(0.0, 4.0, -10.0);
    private static final Vec3[] ROOM = { new Vec3(7.5, 6.8, 4.7), new Vec3(-7.5, 6.8, 4.7), RevolverDuo.TOP,
            new Vec3(0.0, 13.3, 0.2), new Vec3(5.0, 7.4, 0.8), new Vec3(-5.0, 7.4, 0.8), new Vec3(0.0, 8.8, -2.0),
            new Vec3(0.0, 11.5, -5.0), RevolverDuo.SLAM_AT.add(0.0, 1.2, 0.0) };

    private static final Map<UUID, RevolverAssembly> ACTIVE = new HashMap<>();
    private static final Cooldowns<String> COOLDOWNS = new Cooldowns<>(1);

    final ServerPlayer owner;
    final CharacterAbility ability;
    final int id = PowerRing.newId();
    final Vec3 base;
    final int variant;
    final RevolverDuo.Stage stage;
    Vec3 aim;
    int t;
    private final LivingEntity[] pewAt = new LivingEntity[RevolverDuo.PEWS.length];
    @Nullable
    private LivingEntity after;
    private int afterFor = -1;

    private RevolverAssembly(ServerPlayer owner, CharacterAbility ability, Vec3 base, int variant) {
        this.owner = owner;
        this.ability = ability;
        this.base = base;
        this.variant = variant;
        this.stage = RevolverDuo.Stage.of(base, variant, GiantHands.SCALE);
        this.aim = this.stage.point(RESTING);
    }

    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        if (ACTIVE.containsKey(owner.getUUID())) {
            return false;
        }
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (GiantFist.holding(owner)) {
            PowerRing.tell(owner, "busy_fist");
            return false;
        }
        if (AirStrike.calling(owner)) {
            return false;
        }
        int left = COOLDOWNS.left(owner, KEY, 0);
        if (left > 0) {
            PowerRing.tell(owner, "revolver_wait", (left + 19) / 20);
            return false;
        }
        float cost = (float) ability.value("revolverPowerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        Vec3 look = owner.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? Vec3.directionFromRotation(0.0F, owner.getYRot()) : flat.normalize();
        int variant = RevolverDuo.variant(flat);
        Vec3 base = place(level, owner, RevolverDuo.way(variant), variant, ability.value("revolverStageBlocks"));
        if (base == null) {
            PowerRing.tell(owner, "revolver_no_room");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        int ticks = (int) Math.round(ability.value("revolverCooldown") * PowerRules.cooldowns());
        if (ticks > 0) {
            COOLDOWNS.start(owner, KEY, 0, ticks);
        }
        RevolverAssembly show = new RevolverAssembly(owner, ability, base, variant);
        ACTIVE.put(owner.getUUID(), show);
        Effects.start(level, show);
        PowerRing.tell(owner, "revolver");
        show.sound(level, owner.getEyePosition(), SoundEvents.BEACON_POWER_SELECT, 1.2F, 1.1F);
        show.sound(level, owner.getEyePosition(), SoundEvents.NOTE_BLOCK_BANJO.value(), 1.4F, 0.75F);
        return true;
    }

    public static void clear() {
        ACTIVE.clear();
        COOLDOWNS.clear();
    }

    @Nullable
    private static Vec3 place(ServerLevel level, ServerPlayer owner, Vec3 way, int variant, double far) {
        for (double share : STAGES) {
            Vec3 spot = owner.position().add(way.scale(far * share));
            Vec3 from = new Vec3(spot.x, owner.getY() + 6.0, spot.z);
            if (!level.isLoaded(BlockPos.containing(from))) {
                continue;
            }
            BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, from.subtract(0.0, 16.0, 0.0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            Vec3 base = hit.getLocation();
            RevolverDuo.Stage stage = RevolverDuo.Stage.of(base, variant, GiantHands.SCALE);
            boolean room = true;
            for (Vec3 point : ROOM) {
                room &= GiantHands.open(level, stage.point(point));
            }
            if (room) {
                return base;
            }
        }
        return null;
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            this.end(level);
            return false;
        }
        if (!PowerRing.fuels(this.owner, level)) {
            ACTIVE.remove(this.owner.getUUID(), this);
            this.end(level);
            return false;
        }
        this.t++;
        this.steer(level);
        RevolverBeats.play(this, level);
        if (this.t >= RevolverDuo.LIFE) {
            ACTIVE.remove(this.owner.getUUID(), this);
            this.end(level);
            return false;
        }
        PacketDistributor.sendToPlayersNear(level, null, this.base.x, this.base.y, this.base.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), this.base, this.aim.subtract(this.base),
                        (float) GiantHands.SCALE, 1.0F, 0.0F, false, ConstructPayload.REVOLVER, this.variant, this.t,
                        null));
        return true;
    }

    private void steer(ServerLevel level) {
        int turn = this.turn();
        Vec3 want;
        if (turn < 0) {
            this.after = null;
            this.afterFor = -1;
            want = this.stage.point(RESTING);
        } else {
            if (turn != this.afterFor || this.after != null && !this.alive(level, this.after)) {
                this.after = this.pick(level, turn);
                this.afterFor = turn;
            }
            want = this.after != null ? this.after.getBoundingBox().getCenter() : this.stage.point(idle(turn));
        }
        this.aim = this.aim.add(want.subtract(this.aim).scale(AIM_PULL));
    }

    // Which shot the hands are aiming for now: pews count 0 to 4, the revolver's six 10 to 15; -1 is none.
    private int turn() {
        if (this.t >= RevolverDuo.GUN_FORM - 6 && this.t < RevolverDuo.BLOW) {
            int landed = 0;
            for (int pew : RevolverDuo.PEWS) {
                if (this.t >= pew + RevolverDuo.BOLT_TICKS) {
                    landed++;
                }
            }
            return Math.min(landed, RevolverDuo.PEWS.length - 1);
        }
        if (this.t >= RevolverDuo.CYL_CLOSE && this.t < RevolverDuo.PRESENTS) {
            // A shot still aims at its own creature on the tick it fires; the next one is picked after it.
            return 10 + Math.min(RevolverDuo.shotsFired(this.t - 1), RevolverDuo.SHOTS.length - 1);
        }
        return -1;
    }

    private static Vec3 idle(int turn) {
        if (turn < 10) {
            double side = turn % 2 == 0 ? 1.0 : -1.0;
            return new Vec3(side * (4.0 + turn), 16.0 - turn, -6.0);
        }
        int shot = turn - 10;
        double side = shot % 2 == 0 ? 1.0 : -1.0;
        return new Vec3(side * (3.0 + shot * 0.8), 0.0, -8.0 - shot * 1.5);
    }

    @Nullable
    private LivingEntity pick(ServerLevel level, int turn) {
        List<LivingEntity> found = this.targets(level);
        if (found.isEmpty()) {
            return null;
        }
        return found.get((turn % 10) % found.size());
    }

    List<LivingEntity> targets(ServerLevel level) {
        double reach = this.ability.value("revolverRadius") * GiantHands.SCALE;
        List<LivingEntity> found = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,
                new AABB(this.base, this.base).inflate(reach), entity -> GiantHands.fair(this.owner, entity)));
        found.removeIf(living -> living.distanceToSqr(this.base) > reach * reach);
        found.sort(Comparator.comparingDouble(living -> living.distanceToSqr(this.base)));
        return found;
    }

    boolean alive(ServerLevel level, LivingEntity living) {
        return living.isAlive() && living.level() == level && GiantHands.fair(this.owner, living);
    }

    @Nullable
    LivingEntity pewTarget(int pew) {
        return this.pewAt[pew];
    }

    void firedPew(int pew) {
        this.pewAt[pew] = this.after;
    }

    RevolverDuo duo() {
        return RevolverDuo.at(this.base, this.variant, this.aim, this.t, GiantHands.SCALE);
    }

    void end(ServerLevel level) {
        ConstructPayload.sendRemove(level, this.id, this.base);
    }

    void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
