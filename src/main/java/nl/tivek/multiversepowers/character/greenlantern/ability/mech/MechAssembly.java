package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.airstrike.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.ability.flight.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.hands.GiantHands;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightShield;
import nl.tivek.multiversepowers.character.greenlantern.ability.ring.Recharge;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBuild;
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
    // The waits of the mech's own moves, each on the key it sits on.
    private static final String MISSILES = "emerald_express";
    private static final String ROCKETS = "flight";
    private static final String SPIN = "shockwave";
    // A left click within this many ticks after a blow of the combo strikes its next one.
    private static final int COMBO_WINDOW = 16;
    private static final int COMBO = 3;
    // Its base this close above the ground it stands on.
    private static final double ON_GROUND = 0.4;
    private static final double VIEW_RANGE = 128.0;
    private static final double GROUND_BELOW = 12.0;
    private static final double GROUND_ABOVE = 3.0;
    private static final int VICTIM_AFTER = 10;
    private static final double MOST_STRIDE = 1.4;
    private static final double MOST_CLIMB = 3.0;
    private static final int MOST_CLIMB_BITS = 1 << 22;
    // How far (squared) the pilot may be from their seat as the server has it: close while the mech builds round them,
    // looser as they climb aboard or it breaks up, loosest once it walks, as their own game crouches it over its feet
    // and climbs it.
    private static final double HELD = 0.09;
    private static final double LOOSE = 1.0;
    private static final double WALKING = 9.0;
    // How far from where the mech stands a foot it says came down may be, and how many feet a tick.
    private static final double FOOT_REACH = 7.5;
    private static final double FOOT_RISE = 9.0;
    private static final int FEET = 2;

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
    private int climb;
    private int steppedAt = -1;
    private int steps;
    private int breaking = -1;
    @Nullable
    private MechAttack attack;
    private int combo;
    private int comboUntil = -1;

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
        if (PowerRing.armed(owner)) {
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

    // The pilot's own game walks the mech (see MechDrive) and says how far it has got climbing, which goes on to every
    // game; a step further than it could have walked since the last one is not taken.
    public static void drive(ServerPlayer player, Vec3 base, float yaw, int climb) {
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
                || !player.serverLevel().isLoaded(BlockPos.containing(base)) || climb < 0 || climb >= MOST_CLIMB_BITS) {
            return;
        }
        mech.stage = MechScript.Stage.facing(base, yaw);
        mech.drivenAt = mech.t;
        mech.climb = mech.attack == null ? climb : 0;
    }

    // The pilot's own game says where a foot of their mech came down (MechStepPayload): what is small and weak enough
    // under it is crushed.
    public static void stepped(ServerPlayer player, Vec3 sole) {
        MechAssembly mech = ACTIVE.get(player.getUUID());
        if (mech == null || mech.breaking >= 0 || mech.t < MechScript.SETTLED || !Double.isFinite(sole.x)
                || !Double.isFinite(sole.y) || !Double.isFinite(sole.z)) {
            return;
        }
        Vec3 base = mech.stage.base();
        double dx = sole.x - base.x;
        double dz = sole.z - base.z;
        if (dx * dx + dz * dz > FOOT_REACH * FOOT_REACH || Math.abs(sole.y - base.y) > FOOT_RISE
                || !player.serverLevel().isLoaded(BlockPos.containing(sole))) {
            return;
        }
        if (mech.steppedAt != mech.t) {
            mech.steppedAt = mech.t;
            mech.steps = 0;
        }
        if (++mech.steps > FEET) {
            return;
        }
        MechCrush.under(player.serverLevel(), player, mech.ability, sole, mech.stage.ahead(),
                mech.attack == null ? null : mech.attack.creature());
    }

    public static boolean piloting(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    // None while a mech stands: holding the scroll wheel then takes its pilot out.
    public static int waitLeft(ServerPlayer player) {
        return piloting(player) ? 0 : COOLDOWNS.left(player, KEY, 0);
    }

    // In the mech, the wait of the mech's own move on that ability's key (the missile arm, the rocket boots, the spin).
    public static int waitLeft(ServerPlayer player, String ability) {
        return piloting(player) ? COOLDOWNS.left(player, ability, 0) : 0;
    }

    // Every button and key while its pilot stands in a built mech: the left button strikes (or fires the missile arm),
    // the right one fires its eyes (held, a beam until let go), R raises the missile arm, a double space fires the
    // rocket boots, X spins on the ground and dives from the air; the held scroll wheel leaves it. Anything else is
    // shut. The mech's moves keep waits of their own, never the keys'.
    public static boolean control(ServerPlayer player, CharacterAbility key, boolean on, int data) {
        ServerLevel level = player.serverLevel();
        if (key.id().equals("mech")) {
            return on && use(player, level, key);
        }
        MechAssembly mech = ACTIVE.get(player.getUUID());
        if (mech == null) {
            return false;
        }
        if (key.id().equals("light_shield") && !on) {
            if (mech.attack != null && mech.attack.kind() == MechAttacks.GLARE) {
                mech.attack.letGo();
            }
            return false;
        }
        if (!on) {
            return false;
        }
        switch (key.id()) {
            case "light_bolt" -> {
                if ((data & Characters.HOLD) == 0) {
                    mech.strike(level);
                }
            }
            case "light_shield" -> {
                if ((data & (Characters.TAP | Characters.HOLD)) != 0) {
                    mech.eyes((data & Characters.HOLD) != 0);
                }
            }
            case "emerald_express" -> mech.missiles();
            case "flight" -> mech.rockets(level);
            case "shockwave" -> mech.spinOrDive();
            default -> PowerRing.tell(player, "mech_busy", Component.keybind("key." + MultiversePowers.MODID
                    + ".input.scroll_hold"));
        }
        return false;
    }

    private boolean ready() {
        return this.breaking < 0 && this.t >= MechScript.SETTLED && this.climb == 0;
    }

    // A left click: the next blow of the combo, or a click late in one chains it on; with the missile arm up, a salvo.
    private void strike(ServerLevel level) {
        if (!this.ready()) {
            return;
        }
        if (this.attack != null) {
            if (this.attack.kind() == MechAttacks.AIM) {
                this.attack.fire(level, this.owner, this.upright(), this.ability);
            } else if (this.attack.chains()) {
                this.attack.queue();
            }
            return;
        }
        this.blow(level);
    }

    private void blow(ServerLevel level) {
        if (!PowerRing.pay(this.owner, this.ability.value("mechBlowPowerCost"))) {
            return;
        }
        if (this.t > this.comboUntil) {
            this.combo = 0;
        }
        this.attack = MechAttack.strike(this.owner, level, this.upright(), this.combo);
        this.combo = (this.combo + 1) % COMBO;
    }

    // A right click: a ray from the eyes; held, a beam for as long as it is held, its first second paid at once.
    private void eyes(boolean held) {
        if (!this.ready() || this.attack != null || !PowerRing.pay(this.owner,
                this.ability.value(held ? "mechGlarePowerPerSecond" : "mechEyePowerCost"))) {
            return;
        }
        this.attack = MechAttack.move(held ? MechAttacks.GLARE : MechAttacks.EYE);
    }

    // R: the missile arm raised at the crosshair, or lowered again.
    private void missiles() {
        if (this.attack != null && this.attack.kind() == MechAttacks.AIM) {
            this.attack.close();
            return;
        }
        if (this.ready() && this.attack == null && this.waited(MISSILES)) {
            this.attack = MechAttack.move(MechAttacks.AIM);
        }
    }

    // A double space: up on the rocket boots; flying, their thrust cut.
    private void rockets(ServerLevel level) {
        if (this.attack != null && this.attack.kind() == MechAttacks.FLY) {
            this.attack.cut(level, this.stage.base());
            return;
        }
        if (this.ready() && this.attack == null && this.waited(ROCKETS)
                && PowerRing.pay(this.owner, this.ability.value("mechRocketPowerCost"))) {
            this.attack = MechAttack.move(MechAttacks.FLY);
        }
    }

    // X: in the air on the rocket boots, the dive; on the ground, the spin.
    private void spinOrDive() {
        if (this.attack != null && this.attack.flying()) {
            if (PowerRing.pay(this.owner, this.ability.value("mechDivePowerCost"))) {
                this.attack = MechAttack.move(MechAttacks.DIVE);
            }
            return;
        }
        if (this.ready() && this.attack == null && this.waited(SPIN)
                && PowerRing.pay(this.owner, this.ability.value("mechSpinPowerCost"))) {
            this.attack = MechAttack.move(MechAttacks.SPIN);
        }
    }

    // Whether the move on that key is past its wait; if not, its pilot is told how long it still is.
    private boolean waited(String key) {
        int left = COOLDOWNS.left(this.owner, key, 0);
        if (left > 0) {
            PowerRing.tell(this.owner, "mech_move_wait", (left + 19) / 20);
            return false;
        }
        return true;
    }

    // A move over: the missile arm, the rocket boots and the spin wait a while before the next.
    private void ended(MechAttack ended) {
        String key = switch (ended.kind()) {
            case MechAttacks.AIM -> MISSILES;
            case MechAttacks.FLY, MechAttacks.DIVE -> ROCKETS;
            case MechAttacks.SPIN -> SPIN;
            default -> null;
        };
        String setting = switch (ended.kind()) {
            case MechAttacks.AIM -> "mechMissileCooldown";
            case MechAttacks.FLY, MechAttacks.DIVE -> "mechRocketCooldown";
            case MechAttacks.SPIN -> "mechSpinCooldown";
            default -> null;
        };
        if (key != null) {
            int ticks = (int) Math.round(this.ability.value(setting) * PowerRules.cooldowns());
            if (ticks > 0) {
                COOLDOWNS.start(this.owner, key, 0, ticks);
            }
            Characters.sync(this.owner);
        }
        if (ended.combo()) {
            this.comboUntil = this.t + COMBO_WINDOW;
        }
    }

    // Whether its feet stand on the ground: something solid no further than ON_GROUND under its middle or either foot.
    private boolean grounded(ServerLevel level) {
        for (double x : new double[] { 0.0, MechScript.ANKLE.x, -MechScript.ANKLE.x }) {
            Vec3 at = this.stage.point(x, 0.0, 0.0);
            BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(at.add(0.0, 0.5, 0.0),
                    at.subtract(0.0, ON_GROUND, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY,
                    CollisionContext.empty()));
            if (hit.getType() != HitResult.Type.MISS) {
                return true;
            }
        }
        return false;
    }

    // Upright on its ground spot, the torso turned to where its pilot looks.
    private MechScript.Stage upright() {
        return MechScript.upper(this.stage, MechScript.turnTo(this.stage, this.owner.getYHeadRot()));
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
        if (this.breaking < 0 && (!PowerRing.fuels(this.owner, level)
                || !PowerRing.upkeep(this.owner, this.t, this.ability.value("mechPowerPerSecond")))) {
            this.dismantle(level);
        }
        if (this.breaking < 0 && this.t >= this.ability.intValue("mechTime")) {
            PowerRing.tell(this.owner, "mech_time");
            this.dismantle(level);
        }
        if (this.breaking >= 0) {
            this.breaking++;
            this.hold(this.stage.point(MechScript.lowered(this.breaking)), LOOSE);
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
        MechScript.Stage body = settled ? this.upright() : MechBuild.torso(this.stage, this.t);
        if (this.attack != null) {
            boolean grounded = this.attack.kind() != MechAttacks.FLY && this.attack.kind() != MechAttacks.DIVE
                    || this.grounded(level);
            if (this.attack.tick(level, this.owner, this.stage, body, this.ability, grounded)) {
                body = this.attack.torso(body);
            } else {
                MechAttack ended = this.attack;
                ended.release();
                this.attack = null;
                this.ended(ended);
                // A click late in a combo's blow strikes the next one straight on.
                if (ended.queued()) {
                    this.blow(level);
                }
            }
        }
        this.hold(body.point(MechScript.pilot(this.stage, this.t)),
                settled ? WALKING : this.t <= MechScript.ABOARD ? LOOSE : HELD);
        this.send(level);
        return true;
    }

    private void dismantle(ServerLevel level) {
        if (this.breaking >= 0) {
            return;
        }
        this.breaking = 0;
        this.target.release();
        this.stopAttack();
        Vec3 chest = this.stage.point(0.0, 9.5, 0.0);
        Sounds.play(level, chest, SoundEvents.AMETHYST_CLUSTER_BREAK, 3.0F, 0.6F);
        Sounds.play(level, chest, SoundEvents.GLASS_BREAK, 2.0F, 0.6F);
        Sounds.play(level, chest, SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.7F);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.GREEN, 2.0F), chest, 60, 3.0, 0.1);
        ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 1.4F), chest, 30, 2.5, 0.15);
    }

    // The pilot hangs where the mech needs them: their own game moves them there, the server only puts them back if they
    // are further off than `slack` (squared).
    private void hold(Vec3 at, double slack) {
        ServerPlayer player = this.owner;
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
        player.connection.aboveGroundTickCount = 0;
        if (player.position().distanceToSqr(at) > slack) {
            player.connection.teleport(at.x, at.y, at.z, player.getYRot(), player.getXRot(), RelativeMovement.ROTATION);
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private void stopAttack() {
        if (this.attack != null) {
            this.attack.release();
            this.attack = null;
        }
    }

    // What the clients read back with MechScript.Stage.of: the pilot's start in size and (while it builds) charge. The
    // target's id goes on a little after it is let go, so every client sees it spring back up. Once built, the blow
    // goes in the variant and the creature a throw takes in charge, held while it is in the fist.
    private void send(ServerLevel level) {
        Vec3 base = this.stage.base();
        boolean broken = this.breaking >= 0;
        int shown = broken || this.t > MechScript.DONE + VICTIM_AFTER ? -1 : this.victim;
        LivingEntity caught = this.attack == null ? null : this.attack.creature();
        float charge = broken ? (float) this.breaking : caught != null ? LightBubble.caught(caught.getId())
                : (float) this.stage.pilotY();
        PacketDistributor.sendToPlayersNear(level, null, base.x, base.y, base.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), base, this.stage.point(this.stage.target())
                        .subtract(base), (float) this.stage.pilotZ(), 1.0F, charge,
                        caught != null && this.attack.gripped(), ConstructPayload.MECH,
                        MechScript.variant(broken, shown, this.attack == null ? 0 : this.attack.packed(), this.climb),
                        this.t,
                        null));
    }

    // However it ends, the wait before the next one starts here.
    private void end(ServerLevel level) {
        ACTIVE.remove(this.owner.getUUID(), this);
        this.target.release();
        this.stopAttack();
        ConstructPayload.sendRemove(level, this.id, this.stage.base());
        this.owner.resetFallDistance();
        int ticks = (int) Math.round(this.ability.value("mechCooldown") * PowerRules.cooldowns());
        if (ticks > 0) {
            COOLDOWNS.start(this.owner, KEY, 0, ticks);
        }
        Characters.sync(this.owner);
    }
}
