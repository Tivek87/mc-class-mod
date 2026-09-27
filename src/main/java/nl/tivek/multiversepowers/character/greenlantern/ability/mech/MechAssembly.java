package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.flame.Flamethrower;
import nl.tivek.multiversepowers.character.greenlantern.ability.flight.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightShield;
import nl.tivek.multiversepowers.character.greenlantern.ability.ring.Recharge;
import nl.tivek.multiversepowers.character.greenlantern.ability.sword.SwordShield;
import nl.tivek.multiversepowers.character.greenlantern.ability.whip.EnergyWhip;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

public final class MechAssembly implements Effect {
    private static final String KEY = "mech";
    private static final double VIEW_RANGE = 128.0;
    private static final double GROUND_BELOW = 12.0;
    private static final double GROUND_ABOVE = 3.0;
    private static final int VICTIM_AFTER = 10;
    private static final double MOST_STRIDE = 1.0;
    private static final double MOST_CLIMB = 3.0;

    private static final Map<UUID, MechAssembly> ACTIVE = new HashMap<>();
    private static final Cooldowns<String> COOLDOWNS = new Cooldowns<>(1);

    private final ServerPlayer owner;
    private final CharacterAbility ability;
    private final int id = PowerRing.newId();
    private MechScript.Stage stage;
    private final MechTarget target;
    private final int victim;
    private int t;
    private int drivenAt = -1;
    private int breaking = -1;

    private MechAssembly(ServerPlayer owner, CharacterAbility ability, MechScript.Stage stage,
            @Nullable LivingEntity target) {
        this.owner = owner;
        this.ability = ability;
        this.stage = stage;
        this.target = new MechTarget(target);
        this.victim = target == null ? -1 : target.getId();
    }

    public static boolean use(ServerPlayer owner, ServerLevel level, CharacterAbility ability) {
        MechAssembly mech = ACTIVE.get(owner.getUUID());
        if (mech != null) {
            mech.dismantle(level);
            return true;
        }
        if (Recharge.busy(owner)) {
            PowerRing.tell(owner, "busy_lantern");
            return false;
        }
        if (AirStrike.calling(owner) || GiantHands.waving(owner)) {
            return false;
        }
        if (SwordShield.equipped(owner) || Flamethrower.equipped(owner) || EnergyWhip.equipped(owner)) {
            PowerRing.tell(owner, "mech_hands");
            return false;
        }
        int left = COOLDOWNS.left(owner, KEY, 0);
        if (left > 0) {
            PowerRing.tell(owner, "mech_wait", (left + 19) / 20);
            return false;
        }
        float cost = (float) ability.value("mechPowerCost");
        float power = PowerRing.power(owner);
        if (power + 1.0E-4F < cost) {
            PowerRing.tell(owner, "no_power");
            return false;
        }
        LivingEntity target = MechTarget.pick(owner, level, ability.value("mechReach"));
        MechScript.Stage stage = stage(owner, level, target);
        if (stage == null) {
            PowerRing.tell(owner, "mech_no_ground");
            return false;
        }
        PowerRing.setPower(owner, power - cost);
        int ticks = (int) Math.round(ability.value("mechCooldown") * PowerRules.cooldowns());
        if (ticks > 0) {
            COOLDOWNS.start(owner, KEY, 0, ticks);
        }
        Flight.stop(owner);
        LightShield.stop(owner);
        mech = new MechAssembly(owner, ability, stage, target);
        ACTIVE.put(owner.getUUID(), mech);
        Effects.start(level, mech);
        PowerRing.tell(owner, "mech");
        PowerRing.sync(owner);
        Vec3 eye = owner.getEyePosition();
        Sounds.play(level, eye, SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.6F);
        Sounds.play(level, eye, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.4F, 0.7F);
        Sounds.play(level, eye, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.5F);
        mech.send(level);
        return true;
    }

    // The mech stands with the target's spot TARGET_AHEAD in front of it, facing it from where its pilot is; with no
    // target that spot is on the ground ahead and the mech rises round the pilot.
    @Nullable
    private static MechScript.Stage stage(ServerPlayer owner, ServerLevel level, @Nullable LivingEntity target) {
        Vec3 feet = owner.position();
        Vec3 ahead = flat(owner.getLookAngle());
        Vec3 spot;
        if (target != null) {
            spot = target.position();
            Vec3 to = spot.subtract(feet);
            if (to.x * to.x + to.z * to.z > 0.25) {
                ahead = flat(to);
            }
        } else {
            Vec3 front = feet.add(ahead.scale(MechScript.TARGET_AHEAD));
            Double floor = ground(level, front, feet.y);
            spot = new Vec3(front.x, floor == null ? feet.y : floor, front.z);
        }
        Vec3 flatBase = spot.subtract(ahead.scale(MechScript.TARGET_AHEAD));
        Double floor = ground(level, flatBase, Math.max(feet.y, spot.y));
        if (floor == null) {
            return null;
        }
        Vec3 base = new Vec3(flatBase.x, floor, flatBase.z);
        Vec3 from = feet.subtract(base);
        return MechScript.Stage.of(base, ahead.scale(MechScript.TARGET_AHEAD).add(0.0, spot.y - floor, 0.0), from.y,
                from.x * ahead.x + from.z * ahead.z);
    }

    private static Vec3 flat(Vec3 way) {
        Vec3 flat = new Vec3(way.x, 0.0, way.z);
        return flat.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
    }

    @Nullable
    private static Double ground(ServerLevel level, Vec3 at, double from) {
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(new Vec3(at.x, from + GROUND_ABOVE, at.z),
                new Vec3(at.x, from - GROUND_BELOW, at.z), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation().y;
    }

    // The pilot's own game walks the mech (see MechDrive); a step further than it could have walked since the last one
    // is not taken.
    public static void drive(ServerPlayer player, Vec3 base, float yaw) {
        MechAssembly mech = ACTIVE.get(player.getUUID());
        if (mech == null || mech.breaking >= 0 || mech.t < MechScript.SETTLED || !Float.isFinite(yaw)
                || !Double.isFinite(base.x) || !Double.isFinite(base.y) || !Double.isFinite(base.z)) {
            return;
        }
        Vec3 was = mech.stage.base();
        double dx = base.x - was.x;
        double dz = base.z - was.z;
        double reach = MOST_STRIDE * Math.max(1, mech.drivenAt < 0 ? 1 : mech.t - mech.drivenAt);
        if (dx * dx + dz * dz > reach * reach || Math.abs(base.y - was.y) > MOST_CLIMB * reach
                || !player.serverLevel().isLoaded(BlockPos.containing(base))) {
            return;
        }
        mech.stage = MechScript.Stage.facing(base, yaw);
        mech.drivenAt = mech.t;
    }

    public static boolean piloting(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    public static void clear() {
        ACTIVE.clear();
        COOLDOWNS.clear();
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (this.owner.isRemoved() || !this.owner.isAlive() || this.owner.level() != level) {
            this.end(level);
            return false;
        }
        if (this.breaking < 0 && !PowerRing.fuels(this.owner, level)) {
            this.dismantle(level);
        }
        if (this.breaking >= 0) {
            this.breaking++;
            this.hold(this.stage.point(MechScript.lowered(this.breaking)), true);
            if (this.breaking >= MechScript.BREAK_TICKS) {
                this.end(level);
                return false;
            }
            this.send(level);
            return true;
        }
        this.t++;
        MechBlows.beats(level, this.owner, this.stage, this.target, this.ability, this.t);
        if (this.t <= MechScript.DONE) {
            this.target.place(this.stage.point(this.stage.target()));
        } else {
            this.target.release();
        }
        boolean settled = this.t >= MechScript.SETTLED;
        MechScript.Stage body = settled ? MechScript.upper(this.stage, MechScript.turnTo(this.stage,
                this.owner.getYHeadRot())) : this.stage;
        this.hold(body.point(MechScript.pilot(this.stage, this.t)), this.t <= MechScript.ABOARD || settled);
        this.send(level);
        return true;
    }

    private void dismantle(ServerLevel level) {
        if (this.breaking >= 0) {
            return;
        }
        this.breaking = 0;
        this.target.release();
        Vec3 chest = this.stage.point(0.0, 7.0, 0.0);
        Sounds.play(level, chest, SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
        Sounds.play(level, chest, SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
        Sounds.play(level, chest, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.7F);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 2.0F), chest, 60, 3.0, 0.1);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), chest, 30, 2.5, 0.15);
    }

    // The pilot hangs where the mech needs them: their own game moves them there, the server only puts them back if not.
    private void hold(Vec3 at, boolean loose) {
        ServerPlayer player = this.owner;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.connection.aboveGroundTickCount = 0;
        if (player.position().distanceToSqr(at) > (loose ? 1.0 : 0.09)) {
            player.connection.teleport(at.x, at.y, at.z, player.getYRot(), player.getXRot(), RelativeMovement.ROTATION);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    // What the clients read back with MechScript.Stage.of: the pilot's start in size and (while it builds) charge. The
    // target's id goes on a little after it is let go, so every client sees it spring back up.
    private void send(ServerLevel level) {
        Vec3 base = this.stage.base();
        boolean broken = this.breaking >= 0;
        int shown = broken || this.t > MechScript.DONE + VICTIM_AFTER ? -1 : this.victim;
        PacketDistributor.sendToPlayersNear(level, null, base.x, base.y, base.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), base, this.stage.point(this.stage.target())
                        .subtract(base), (float) this.stage.pilotZ(), 1.0F,
                        broken ? (float) this.breaking : (float) this.stage.pilotY(), false, ConstructPayload.MECH,
                        MechScript.variant(broken, shown), this.t, null));
    }

    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        this.target.release();
        ConstructPayload.sendRemove(level, this.id, this.stage.base());
        this.owner.resetFallDistance();
    }
}
