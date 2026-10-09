package nl.tivek.multiversepowers.character.greenlantern.ability.mech;

import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.ability.Cooldowns;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// What a built mech's pilot sets off with their buttons and keys, and the waits its moves leave on those keys.
abstract class MechControls {
    // The waits of the mech's own moves, each on the key it sits on.
    private static final String MISSILES = "emerald_express";
    private static final String ROCKETS = "flight";
    private static final String SPIN = "shockwave";
    private static final String FLAME = "construct_wheel";
    private static final String HELPERS = "giant_hands";
    // A left click within this many ticks after a blow of the combo strikes its next one.
    private static final int COMBO_WINDOW = 16;
    private static final int COMBO = 3;
    // Its base this close above the ground it stands on.
    private static final double ON_GROUND = 0.4;
    // The flamethrower wants the mech to have stood still this many ticks.
    private static final int STILL = 4;

    static final Cooldowns<String> COOLDOWNS = new Cooldowns<>(1);

    final ServerPlayer owner;
    final CharacterAbility ability;
    MechScript.Stage stage;
    int t;
    int drivenAt = -1;
    int movedAt = -1;
    int climb;
    int breaking = -1;
    @Nullable
    MechAttack attack;
    private int combo;
    private int comboUntil = -1;

    MechControls(ServerPlayer owner, CharacterAbility ability, MechScript.Stage stage) {
        this.owner = owner;
        this.ability = ability;
        this.stage = stage;
    }

    boolean ready() {
        return this.breaking < 0 && this.t >= MechScript.SETTLED && this.climb == 0;
    }

    // A left click: the next blow of the combo, or a click late in one chains it on; with the missile arm up, a missile.
    void strike(ServerLevel level) {
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

    void blow(ServerLevel level) {
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
    void eyes(boolean held) {
        if (!this.ready() || this.attack != null || !PowerRing.pay(this.owner,
                this.ability.value(held ? "mechGlarePowerPerSecond" : "mechEyePowerCost"))) {
            return;
        }
        this.attack = MechAttack.move(held ? MechAttacks.GLARE : MechAttacks.EYE);
    }

    // R: the missile arm raised at the crosshair, or lowered again.
    void missiles() {
        if (this.attack != null && this.attack.kind() == MechAttacks.AIM) {
            this.attack.close();
            return;
        }
        if (this.ready() && this.attack == null && this.waited(MISSILES)) {
            this.attack = MechAttack.move(MechAttacks.AIM);
        }
    }

    // A double space: up on the rocket boots, in the middle of a jump at once; flying, their thrust cut.
    void rockets(ServerLevel level) {
        if (this.attack != null && this.attack.kind() == MechAttacks.FLY) {
            this.attack.cut(level, this.stage.base());
            return;
        }
        boolean jumping = this.attack != null && this.attack.kind() == MechAttacks.JUMP;
        if (this.ready() && (this.attack == null || jumping) && this.waited(ROCKETS)
                && PowerRing.pay(this.owner, this.ability.value("mechRocketPowerCost"))) {
            this.attack = jumping && this.attack.leaping() ? MechAttack.boosted(level, this.stage.base())
                    : MechAttack.move(MechAttacks.FLY);
        }
    }

    // Space on the ground: a jump.
    void jump(ServerLevel level) {
        if (this.ready() && this.attack == null && this.grounded(level)) {
            this.attack = MechAttack.move(MechAttacks.JUMP);
        }
    }

    // X: in the air on the rocket boots or in a jump, the dive; on the ground, the spin.
    void spinOrDive() {
        if (this.attack != null && (this.attack.flying() || this.attack.leaping())) {
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

    // V: the flamethrower, only while the mech stands still, its first second paid at once; V again shuts it.
    void flame() {
        if (this.attack != null && this.attack.kind() == MechAttacks.FLAME) {
            this.attack.close();
            return;
        }
        if (!this.ready() || this.attack != null) {
            return;
        }
        if (this.movedAt >= 0 && this.t - this.movedAt <= STILL) {
            PowerRing.tell(this.owner, "mech_flame_still");
            return;
        }
        if (this.waited(FLAME) && PowerRing.pay(this.owner, this.ability.value("mechFlamePowerPerSecond"))) {
            this.attack = MechAttack.move(MechAttacks.FLAME);
        }
    }

    // Left Alt: the hatch under the cockpit opens and the helpers its pilot is short of drop out.
    void hatch(ServerLevel level) {
        if (!this.ready() || this.attack != null || !this.waited(HELPERS)) {
            return;
        }
        if (MechHelpers.out(level, this.owner) >= MechHelpers.MOST) {
            PowerRing.tell(this.owner, "mech_helpers_out");
            return;
        }
        if (PowerRing.pay(this.owner, this.ability.value("mechHelperPowerCost"))) {
            this.attack = MechAttack.move(MechAttacks.HATCH);
        }
    }

    // Whether the move on that key is past its wait; if not, its pilot is told how long it still is.
    boolean waited(String key) {
        int left = COOLDOWNS.left(this.owner, key, 0);
        if (left > 0) {
            PowerRing.tell(this.owner, "mech_move_wait", (left + 19) / 20);
            return false;
        }
        return true;
    }

    // A move over: the missile arm, the rocket boots, the spin, the flamethrower and the hatch wait a while before the
    // next.
    void ended(MechAttack ended) {
        String key = switch (ended.kind()) {
            case MechAttacks.AIM -> MISSILES;
            case MechAttacks.FLY, MechAttacks.DIVE -> ROCKETS;
            case MechAttacks.SPIN -> SPIN;
            case MechAttacks.FLAME -> FLAME;
            case MechAttacks.HATCH -> HELPERS;
            default -> null;
        };
        String setting = switch (ended.kind()) {
            case MechAttacks.AIM -> "mechMissileCooldown";
            case MechAttacks.FLY, MechAttacks.DIVE -> "mechRocketCooldown";
            case MechAttacks.SPIN -> "mechSpinCooldown";
            case MechAttacks.FLAME -> "mechFlameCooldown";
            case MechAttacks.HATCH -> "mechHelperCooldown";
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
    boolean grounded(ServerLevel level) {
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
    MechScript.Stage upright() {
        return MechScript.upper(this.stage, MechScript.turnTo(this.stage, this.owner.getYHeadRot()));
    }
}
