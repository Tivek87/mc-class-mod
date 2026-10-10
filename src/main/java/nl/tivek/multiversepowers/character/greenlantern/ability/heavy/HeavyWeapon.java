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

// The Battleaxe and the Heavy Chainsaw from the Construct Wheel, held in both hands: formed for formPowerCost, kept
// for heldPowerPerSecond, broken up when put away or the ring runs dry. Left click and its hold, right click and its
// hold are each weapon's four moves (HeavyBlows); every player near is told each move (HeavyPayload).
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class HeavyWeapon extends HeavyBlows implements Effect {
    private static final double VIEW_RANGE = 96.0;
    private static final int RESEND = 20;
    // The chops start over after this long without a click.
    private static final int COMBO_TICKS = 30;
    // A guard turns blows from this far round either side of his look (cosine).
    private static final double GUARD_ARC = 0.15;

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
        return construct == Construct.BATTLEAXE ? AXE : construct == Construct.CHAINSAW ? SAW : -1;
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
            weapon.sound(player.serverLevel(), player.position(), kind == AXE ? SoundEvents.ANVIL_PLACE
                    : SoundEvents.GRINDSTONE_USE, 0.5F, kind == AXE ? 1.4F : 1.2F);
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
        int heldMove = weapon.weapon == AXE ? LEAP : REND;
        if (!on) {
            return weapon.weapon == SAW && weapon.let(REND, REND_OUT);
        }
        if ((data & Characters.HOLD) != 0) {
            return weapon.start(heldMove, weapon.weapon == AXE ? "leapPowerCost" : null);
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
        int heldMove = weapon.weapon == AXE ? WHIRL : GUARD;
        if (!on) {
            return weapon.let(heldMove, weapon.weapon == AXE ? WHIRL_OUT : GUARD_DOWN);
        }
        if ((data & Characters.HOLD) != 0) {
            return weapon.start(heldMove, null);
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        return weapon.weapon == AXE ? weapon.start(HOOK, "hookPowerCost") : weapon.start(IMPALE, "impalePowerCost");
    }

    public static void clear() {
        HELD.clear();
        PICK_SOUND.clear();
    }

    private boolean ready() {
        return !this.holding && (this.move == IDLE || this.age >= open(this.weapon, this.move));
    }

    // A click: the next chop or slash in turn.
    private boolean chop() {
        if (!this.ready() || !PowerRing.pay(this.owner, value(this.weapon == AXE ? "axePowerCost" : "sawPowerCost"))) {
            return false;
        }
        int count = this.weapon == AXE ? 3 : 2;
        this.combo = this.idle > COMBO_TICKS || this.combo < 0 ? 0 : (this.combo + 1) % count;
        this.go(this.weapon == AXE ? CHOP + this.combo : REV + this.combo);
        return true;
    }

    private boolean start(int next, @Nullable String cost) {
        if (!this.ready() || cost != null && !PowerRing.pay(this.owner, value(cost))) {
            return false;
        }
        if (held(this.weapon, next) && !this.drains(next)) {
            return false;
        }
        this.holding = held(this.weapon, next);
        this.combo = -1;
        this.go(next);
        return true;
    }

    // The button of a held move let go: its ending follows.
    private boolean let(int heldMove, int ending) {
        if (!this.holding || this.move != heldMove) {
            return false;
        }
        this.holding = false;
        this.go(ending);
        return true;
    }

    private void go(int next) {
        this.begin(next);
        this.idle = 0;
        this.send();
    }

    // What a held move costs this tick; false with the ring too low.
    private boolean drains(int heldMove) {
        String key = this.weapon == AXE ? "axeWhirlPowerPerSecond"
                : heldMove == GUARD ? "sawGuardPowerPerSecond" : "rendPowerPerSecond";
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
        if (this.move == IDLE) {
            this.idle++;
        } else {
            this.age++;
            if (this.holding) {
                if (!this.drains(this.move)) {
                    this.let(this.move, this.weapon == AXE ? WHIRL_OUT : this.move == GUARD ? GUARD_DOWN : REND_OUT);
                }
            }
            this.blows(level);
            if (!this.holding && this.move != IDLE && this.age >= length(this.weapon, this.move)) {
                this.move = IDLE;
                this.age = 0;
                this.idle = 0;
                this.send();
            }
        }
        if (this.alive % RESEND == 0) {
            this.send();
        }
        return true;
    }

    private void send() {
        ServerLevel level = this.owner.serverLevel();
        PacketDistributor.sendToPlayersNear(level, null, this.owner.getX(), this.owner.getY(), this.owner.getZ(),
                VIEW_RANGE, new HeavyPayload(this.owner.getId(), this.weapon, this.move, this.age,
                        this.owner.getYRot()));
    }

    private boolean whirling() {
        return this.breaking < 0 && this.weapon == AXE && (this.move == WHIRL || this.move == WHIRL_OUT);
    }

    private boolean guarding() {
        return this.breaking < 0 && this.weapon == SAW && this.move == GUARD && this.holding
                && this.age >= LOOP_FROM - 1;
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
        event.setAmount(event.getAmount() * (float) value("sawGuardDamageKept"));
        if (direct instanceof LivingEntity attacker && attacker != player && attacker.isAlive()
                && PowerRing.canHit(player, attacker)) {
            weapon.strike(level, attacker, value("sawGuardRiposte"), flat.normalize().scale(0.9).add(0.0, 0.3, 0.0),
                    false);
        }
    }
}
