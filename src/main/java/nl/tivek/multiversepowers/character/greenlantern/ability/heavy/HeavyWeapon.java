package nl.tivek.multiversepowers.character.greenlantern.ability.heavy;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightDome;
import nl.tivek.multiversepowers.character.greenlantern.construct.Construct;
import nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyPayload;
import nl.tivek.multiversepowers.engine.ability.Throttle;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The Battleaxe, the Heavy Chainsaw, the Rocket Launcher, the Sawed-off Shotgun, the Dual Revolvers, the Arm Cannon
// and the Minigun from the Construct Wheel: formed for formPowerCost, kept for heldPowerPerSecond, broken up when put
// away or the ring runs dry. Left click and its hold, right click and its hold are each weapon's four moves
// (HeavyBlows, HeavyGuns, HeavyRounds); every player near is told each move (HeavyPayload).
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class HeavyWeapon extends HeavyRounds implements Effect {
    private static final double VIEW_RANGE = 96.0;
    private static final int RESEND = 20;
    // The chops start over after this long without a click.
    private static final int COMBO_TICKS = 30;
    // A guard turns blows from this far round either side of his look (cosine).
    private static final double GUARD_ARC = 0.15;
    // What each weapon's click, hold, right click and right hold cost (a held move's a second).
    private static final String[][] COSTS = { { "axePowerCost", "leapPowerCost", "hookPowerCost",
            "axeWhirlPowerPerSecond" }, { "sawPowerCost", "rendPowerPerSecond", "impalePowerCost",
            "sawGuardPowerPerSecond" }, { "rpgPowerCost", "rpgClusterPowerCost", "rpgJumpPowerCost",
            "rpgGuidedPowerPerSecond" }, { "shotgunPowerCost", "shotgunDoublePowerCost", "shotgunBashPowerCost",
            "shotgunDeflectPowerPerSecond" }, { "revolverPowerCost", "revolverFanPowerCost", "revolverWhipPowerCost",
            "revolverDeadeyePowerPerSecond" }, { "cannonPowerCost", "cannonChargePowerPerSecond", "cannonBashPowerCost",
            "cannonShieldPowerPerSecond" }, { "minigunPowerCost", "minigunPowerPerSecond", "minigunVentPowerCost",
            "minigunSpinPowerPerSecond" } };

    private static final Map<UUID, HeavyWeapon> HELD = new HashMap<>();
    private static final Throttle PICK_SOUND = new Throttle(5);

    private int alive;
    private int idle;
    private int combo = -1;
    private boolean holding;
    private int breaking = -1;

    private HeavyWeapon(ServerPlayer owner, int weapon) {
        super(owner, weapon);
    }

    private static int weaponOf(Construct construct) {
        return switch (construct) {
            case BATTLEAXE -> AXE;
            case CHAINSAW -> SAW;
            case ROCKET_LAUNCHER -> RPG;
            case SHOTGUN -> SHOTGUN;
            case REVOLVERS -> REVOLVERS;
            case ARM_CANNON -> CANNON;
            case MINIGUN -> MINIGUN;
            default -> -1;
        };
    }

    public static void hold(ServerPlayer player, Construct construct) {
        HeavyWeapon now = HELD.get(player.getUUID());
        int kind = weaponOf(construct);
        boolean want = kind >= 0 && Characters.of(player) == GameCharacter.GREEN_LANTERN && player.isAlive()
                && !Arrival.busy(player);
        boolean same = now != null && now.breaking < 0 && now.weapon == kind;
        if (want && !same) {
            if (!PowerRing.pay(player, value("formPowerCost"))) {
                return;
            }
            if (now != null) {
                now.breakUp();
            }
            HeavyWeapon weapon = new HeavyWeapon(player, kind);
            HELD.put(player.getUUID(), weapon);
            Effects.start(player.serverLevel(), weapon);
            if (PICK_SOUND.allow(player)) {
                PowerRing.say(player, "equip_weapon");
            }
            weapon.sound(player.serverLevel(), player.position(), SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.2F);
            if (kind == AXE) {
                weapon.sound(player.serverLevel(), player.position(), SoundEvents.ANVIL_PLACE, 0.5F, 1.4F);
            } else if (gun(kind)) {
                weapon.sound(player.serverLevel(), player.position(), SoundEvents.CROSSBOW_LOADING_END.value(), 0.9F,
                        kind == RPG || kind == MINIGUN ? 0.6F : kind == REVOLVERS ? 1.5F : 1.1F);
            }
            weapon.send();
        } else if (!want && now != null) {
            now.breakUp();
        }
    }

    public static boolean equipped(ServerPlayer player) {
        HeavyWeapon weapon = HELD.get(player.getUUID());
        return weapon != null && weapon.breaking < 0;
    }

    public static boolean attack(ServerPlayer player, ServerLevel level, boolean on, int data) {
        HeavyWeapon weapon = HELD.get(player.getUUID());
        if (weapon == null || weapon.breaking >= 0) {
            return false;
        }
        int heldMove = weapon.weapon == AXE ? LEAP : weapon.weapon == SAW ? REND : AIM;
        if (!on) {
            return weapon.weapon != AXE && weapon.let(heldMove, ending(weapon.weapon, heldMove));
        }
        if ((data & Characters.HOLD) != 0) {
            // Out of the minigun's spun barrels the stream starts at once.
            if (weapon.weapon == MINIGUN && weapon.holding && weapon.move == BRACE && !weapon.overheated) {
                weapon.spun = true;
                weapon.go(AIM);
                return true;
            }
            // The rend, the cannon's charge and the minigun's stream pay a second as they go; the earthbreaker and
            // the other guns' aim pay once as they start.
            return weapon.start(heldMove, weapon.paysAsItGoes(heldMove) ? null : COSTS[weapon.weapon][1]);
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        return weapon.chop();
    }

    public static boolean defend(ServerPlayer player, ServerLevel level, boolean on, int data) {
        HeavyWeapon weapon = HELD.get(player.getUUID());
        if (weapon == null || weapon.breaking >= 0) {
            return false;
        }
        int heldMove = weapon.weapon == AXE ? WHIRL : weapon.weapon == SAW ? GUARD : BRACE;
        if (!on) {
            return weapon.let(heldMove, ending(weapon.weapon, heldMove));
        }
        if ((data & Characters.HOLD) != 0) {
            // The guided rocket pays for its rocket as it leaves, then a second while he steers it.
            return weapon.start(heldMove, weapon.weapon == RPG ? "rpgGuidedPowerCost" : null);
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        int move = weapon.weapon == AXE ? HOOK : weapon.weapon == SAW ? IMPALE : KICK;
        return weapon.start(move, COSTS[weapon.weapon][2]);
    }

    public static void clear() {
        HELD.clear();
        PICK_SOUND.clear();
        HeavyRocket.clear();
    }

    private boolean ready() {
        return !this.holding && (this.move == IDLE || this.age >= open(this.weapon, this.move));
    }

    // A click: the next chop or slash in turn, or a shot.
    private boolean chop() {
        if (gun(this.weapon)) {
            return this.start(SHOOT, COSTS[this.weapon][0]);
        }
        if (!this.ready() || !PowerRing.pay(this.owner, value(COSTS[this.weapon][0]))) {
            return false;
        }
        int count = this.weapon == AXE ? 3 : 2;
        this.combo = this.idle > COMBO_TICKS || this.combo < 0 ? 0 : (this.combo + 1) % count;
        this.go(this.weapon == AXE ? CHOP + this.combo : REV + this.combo);
        return true;
    }

    private boolean start(int next, @Nullable String cost) {
        if (!this.ready() || this.empty(next) || this.weapon == MINIGUN && this.overheated
                || cost != null && !PowerRing.pay(this.owner, value(cost))) {
            return false;
        }
        if (held(this.weapon, next) && !this.drains(next)) {
            return false;
        }
        this.holding = held(this.weapon, next);
        this.combo = -1;
        if (next == AIM) {
            this.spun = false;
        }
        if (next == BRACE) {
            this.marks.clear();
        }
        this.go(next);
        return true;
    }

    // A held move whose cost is taken a second at a time rather than once as it starts.
    private boolean paysAsItGoes(int heldMove) {
        return this.weapon == SAW || heldMove == AIM && (this.weapon == CANNON || this.weapon == MINIGUN);
    }

    // The button of a held move let go: its ending follows.
    private boolean let(int heldMove, int ending) {
        if (!this.holding || this.move != heldMove) {
            return false;
        }
        if (this.weapon == CANNON && heldMove == AIM) {
            this.charged = this.age;
        }
        this.holding = false;
        this.go(ending);
        return true;
    }

    @Override
    void stop() {
        this.let(this.move, ending(this.weapon, this.move));
    }

    private void go(int next) {
        this.begin(next);
        this.idle = 0;
        this.send();
    }

    // What a held move costs this tick; false with the ring too low. Most guns' aim was paid as it began.
    private boolean drains(int heldMove) {
        if (gun(this.weapon) && heldMove == AIM && !this.paysAsItGoes(heldMove)) {
            return true;
        }
        String key = heldMove == REND || heldMove == AIM ? COSTS[this.weapon][1] : COSTS[this.weapon][3];
        float price = (float) value(key) / 20.0F;
        float power = PowerRing.power(this.owner);
        if (power + 1.0E-4F < price) {
            PowerRing.tell(this.owner, "no_power");
            return false;
        }
        PowerRing.setPower(this.owner, Math.max(0.0F, power - price));
        return true;
    }

    private void breakUp() {
        if (this.breaking >= 0) {
            return;
        }
        this.breaking = 0;
        this.holding = false;
        this.caught = null;
        this.move = BREAK;
        this.age = 0;
        this.send();
        ServerLevel level = this.owner.serverLevel();
        this.sound(level, this.owner.position(), SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 1.1F);
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.breaking >= 0) {
            this.breaking++;
            if (this.breaking >= BREAK_TICKS) {
                HELD.remove(this.owner.getUUID(), this);
                return false;
            }
            return true;
        }
        if (HELD.get(this.owner.getUUID()) != this || !this.owner.isAlive()
                || Characters.of(this.owner) != GameCharacter.GREEN_LANTERN || !PowerRing.fuels(this.owner, level)
                || !PowerRing.upkeep(this.owner, this.alive, value("heldPowerPerSecond"))) {
            this.breakUp();
            return true;
        }
        this.alive++;
        this.landing();
        if (this.move == IDLE) {
            this.idle++;
        } else {
            this.age++;
            if (this.holding) {
                if (this.move == WHIRL && this.weapon == AXE && this.age >= WHIRL_MOST
                        || this.move == AIM && gun(this.weapon) && this.weapon != MINIGUN && this.weapon != REVOLVERS
                        && this.age >= AIM_MOST
                        || this.move == BRACE && this.weapon == RPG && this.age >= GUIDE_MOST) {
                    this.let(this.move, ending(this.weapon, this.move));
                } else if (!this.drains(this.move)) {
                    this.let(this.move, ending(this.weapon, this.move));
                }
            }
            this.blows(level);
            if (!this.holding && this.move != IDLE && this.age >= length(this.weapon, this.move)) {
                if (this.move != RELOAD && (ammo(this.weapon) > 0 && this.ammo < 1
                        || this.weapon == MINIGUN && this.overheated)) {
                    this.go(RELOAD);
                } else {
                    this.move = IDLE;
                    this.age = 0;
                    this.idle = 0;
                    this.send();
                }
            }
        }
        if (this.alive % RESEND == 0) {
            this.send();
        }
        return true;
    }

    @Override
    void send() {
        ServerLevel level = this.owner.serverLevel();
        PacketDistributor.sendToPlayersNear(level, null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                VIEW_RANGE, new HeavyPayload(this.owner.getId(), this.weapon, this.move,
                        this.move == IDLE ? this.idle : this.age, this.owner.getYRot(),
                        this.weapon == MINIGUN ? this.heatShown() : this.ammo));
    }

    private boolean whirling() {
        return this.breaking < 0 && this.weapon == AXE && (this.move == WHIRL || this.move == WHIRL_OUT);
    }

    // The chainsaw's guard, the shotgun's deflection and the cannon's shield turn what comes from the front.
    private boolean guarding() {
        return this.breaking < 0 && this.holding && this.age >= LOOP_FROM - 1
                && (this.weapon == SAW && this.move == GUARD
                || (this.weapon == SHOTGUN || this.weapon == CANNON) && this.move == BRACE);
    }

    // Whirling, nothing knocks him back.
    @SubscribeEvent
    public static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HeavyWeapon weapon = HELD.get(player.getUUID());
            if (weapon != null && weapon.whirling()) {
                event.setCanceled(true);
            }
        }
    }

    // The whirring guard grinds away blows from the front: a creature that struck it takes a bite back and is thrown
    // off, a shot is shredded.
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        HeavyWeapon weapon = HELD.get(player.getUUID());
        DamageSource source = event.getSource();
        if (weapon == null || !weapon.guarding() || event.getAmount() <= 0.0F || LightDome.goesThrough(source)) {
            return;
        }
        Vec3 from = source.getSourcePosition();
        if (from == null) {
            return;
        }
        Vec3 to = from.subtract(player.position());
        Vec3 flat = new Vec3(to.x, 0.0, to.z);
        Vec3 look = player.getLookAngle();
        Vec3 ahead = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-4 || ahead.lengthSqr() < 1.0E-4
                || flat.normalize().dot(ahead.normalize()) < GUARD_ARC) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 at = player.getEyePosition().add(flat.normalize().scale(0.8)).add(0.0, -0.4, 0.0);
        weapon.sparks(level, at, 12);
        weapon.sound(level, at, SoundEvents.GRINDSTONE_USE, 1.0F, 1.8F);
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile shot) {
            event.setCanceled(true);
            shot.discard();
            ParticleFx.cloud(level, ParticleFx.dust(PowerRing.BRIGHT, 0.9F), shot.position(), 10, 0.2, 0.2);
            return;
        }
        if (weapon.weapon == SHOTGUN || weapon.weapon == CANNON) {
            event.setAmount(event.getAmount() * (float) value(weapon.weapon == SHOTGUN ? "shotgunDeflectDamageKept"
                    : "cannonShieldDamageKept"));
            return;
        }
        event.setAmount(event.getAmount() * (float) value("sawGuardDamageKept"));
        if (direct instanceof LivingEntity attacker && attacker != player && attacker.isAlive()
                && PowerRing.canHit(player, attacker)) {
            weapon.strike(level, attacker, value("sawGuardRiposte"), flat.normalize().scale(0.9).add(0.0, 0.3, 0.0),
                    false);
        }
    }
}
