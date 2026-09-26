package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.engine.ability.Throttle;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class EnergyWhip extends WhipHits {
    public static final int BREAK_TICKS = 8;
    // A move may start this many ticks before the last one is ready: the click came over the network a little later.
    private static final int SLACK = 2;
    private static final double VIEW_RANGE = 96.0;
    private static final double FRONT = 0.2;

    private static final Map<UUID, EnergyWhip> HELD = new HashMap<>();
    private static final Throttle PICK_SOUND = new Throttle(5);

    private final int id = PowerRing.newId();
    private boolean whirling;
    private boolean spinning;
    private int breaking = -1;
    @Nullable
    private WhipSnare snare;

    private EnergyWhip(ServerPlayer owner) {
        super(owner);
    }

    public static void hold(ServerPlayer player, Construct construct) {
        EnergyWhip now = HELD.get(player.getUUID());
        boolean want = construct == Construct.ENERGY_WHIP && Characters.of(player) == GameCharacter.GREEN_LANTERN
                && player.isAlive() && !Arrival.busy(player);
        if (want && (now == null || now.breaking >= 0)) {
            EnergyWhip whip = new EnergyWhip(player);
            if (now != null) {
                now.breaking = Math.max(now.breaking, BREAK_TICKS - 1);
            }
            HELD.put(player.getUUID(), whip);
            Effects.start(player.serverLevel(), whip);
            if (PICK_SOUND.allow(player)) {
                whip.soundForOthers(SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.5F);
                whip.soundForOthers(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.0F);
            }
            whip.send(player.serverLevel());
        } else if (!want && now != null) {
            now.breakUp();
        }
    }

    public static boolean equipped(ServerPlayer player) {
        EnergyWhip whip = HELD.get(player.getUUID());
        return whip != null && whip.breaking < 0;
    }

    public static boolean attack(ServerPlayer player, ServerLevel level, boolean on, int data) {
        EnergyWhip whip = HELD.get(player.getUUID());
        if (whip == null || whip.breaking >= 0) {
            return false;
        }
        if (!on) {
            return whip.stopWhirl();
        }
        if ((data & Characters.HOLD) != 0) {
            return whip.startWhirl();
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        WhipMove asked = WhipMove.byIndex(data >> Characters.MOVE_SHIFT);
        return whip.begin(asked != null && asked.kind() == WhipMove.Kind.ATTACK ? asked
                : WhipMove.randomAttack(player.getRandom(), whip.move));
    }

    public static boolean defend(ServerPlayer player, ServerLevel level, boolean on, int data) {
        EnergyWhip whip = HELD.get(player.getUUID());
        if (whip == null || whip.breaking >= 0) {
            return false;
        }
        if (!on) {
            return whip.stopSpin();
        }
        if ((data & Characters.HOLD) != 0) {
            return whip.startSpin();
        }
        return (data & Characters.TAP) != 0 && whip.startLasso(level);
    }

    public static void clear() {
        for (EnergyWhip whip : HELD.values()) {
            if (whip.snare != null) {
                whip.snare.release();
            }
        }
        HELD.clear();
        PICK_SOUND.clear();
    }

    private boolean free() {
        return !this.whirling && !this.spinning && this.age - this.moveStart >= this.move.ready() - SLACK;
    }

    private boolean pay(String cost) {
        float price = (float) wheel().value(cost);
        float power = PowerRing.power(this.owner);
        if (power + 1.0E-4F < price) {
            PowerRing.tell(this.owner, "no_power");
            return false;
        }
        PowerRing.setPower(this.owner, power - price);
        return true;
    }

    private boolean drain(String perSecond) {
        float price = (float) wheel().value(perSecond) / 20.0F;
        float power = PowerRing.power(this.owner);
        if (power + 1.0E-4F < price) {
            PowerRing.tell(this.owner, "no_power");
            return false;
        }
        PowerRing.setPower(this.owner, Math.max(0.0F, power - price));
        return true;
    }

    private void start(WhipMove next, double ran) {
        this.move = next;
        this.moveStart = this.age;
        this.before = ran;
        this.stroke = -1;
        this.struck.clear();
    }

    private boolean begin(WhipMove attack) {
        if (!this.free()) {
            return false;
        }
        this.start(attack, 0.0);
        return true;
    }

    private boolean startWhirl() {
        if (!this.free() || !this.drain("whirlPowerPerSecond")) {
            return false;
        }
        this.start(WhipMove.WHIRL, 0.0);
        this.whirling = true;
        this.sound(SoundEvents.BREEZE_WHIRL, 0.7F, 1.3F);
        this.sound(SoundEvents.BEACON_POWER_SELECT, 0.5F, 1.9F);
        return true;
    }

    private boolean stopWhirl() {
        if (!this.whirling) {
            return false;
        }
        this.whirling = false;
        this.start(WhipMove.WHIRL_CRACK, this.age - this.moveStart);
        return true;
    }

    private boolean startSpin() {
        if (!this.free() || !this.drain("spinPowerPerSecond")) {
            return false;
        }
        this.start(WhipMove.SPIN_SHIELD, 0.0);
        this.spinning = true;
        this.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.7F);
        this.sound(SoundEvents.BEACON_POWER_SELECT, 0.5F, 1.8F);
        return true;
    }

    private boolean stopSpin() {
        if (!this.spinning) {
            return false;
        }
        this.spinning = false;
        this.start(WhipMove.SPIN_END, this.age - this.moveStart);
        return true;
    }

    private boolean startLasso(ServerLevel level) {
        if (!this.free()) {
            return false;
        }
        WhipSnare caught = WhipSnare.aim(this.owner, level, wheel().value("lassoRange"));
        if (caught != null && !this.pay("lassoPowerCost")) {
            return false;
        }
        this.start(WhipMove.LASSO, 0.0);
        this.snare = caught;
        this.sound(SoundEvents.FISHING_BOBBER_THROW, 0.9F, 0.8F);
        if (caught != null) {
            caught.send(level, 0);
        }
        return true;
    }

    @Override
    public boolean tick(ServerLevel level, int tick) {
        if (this.breaking >= 0) {
            // Its clock runs on while it breaks up: everyone else's game keeps time by it (see send).
            this.age++;
            this.breaking++;
            if (this.breaking >= BREAK_TICKS) {
                ConstructPayload.sendRemove(level, this.id, this.owner.position());
                HELD.remove(this.owner.getUUID(), this);
                return false;
            }
            this.send(level);
            return true;
        }
        if (HELD.get(this.owner.getUUID()) != this || !PowerRing.fuels(this.owner, level)) {
            this.breakUp();
            this.send(level);
            return true;
        }
        this.age++;
        int t = this.age - this.moveStart;
        switch (this.move.kind()) {
            case EQUIP -> equipSounds(t - 1, t, this::soundForOthers);
            case ATTACK, WHIRL_CRACK -> this.lash(level, t);
            case WHIRL -> {
                if (!this.drain("whirlPowerPerSecond")) {
                    this.stopWhirl();
                } else {
                    int from = WhipMove.WHIRL_UP - 2;
                    if (t >= from && (t - from) % WhipMove.WHIRL_EVERY == 0) {
                        this.whirlHits(level);
                    }
                    if (t % 5 == 0) {
                        this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.4F, 1.15F + 0.15F
                                * this.owner.getRandom().nextFloat());
                    }
                }
            }
            case SPIN -> {
                if (!this.drain("spinPowerPerSecond")) {
                    this.stopSpin();
                } else {
                    if (t >= WhipMove.SPIN_GUARD) {
                        this.deflect(level);
                    }
                    if (t % 4 == 0) {
                        this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.22F, 1.9F);
                    }
                }
            }
            case LASSO -> this.lasso(level, t);
            case SPIN_END -> {
            }
        }
        this.send(level);
        return true;
    }

    private void lasso(ServerLevel level, int t) {
        WhipSnare held = this.snare;
        if (held == null) {
            if (t == WhipMove.LASSO_REACH + 1) {
                Vec3[] points = this.lashAt(this.look(), t);
                this.crack(level, points[points.length - 1], 0.8F);
            }
            return;
        }
        if (held.lost(level, wheel().value("lassoRange"))) {
            held.gone(level);
            this.snare = null;
            return;
        }
        if (t == WhipMove.LASSO_REACH) {
            this.crack(level, held.target.getBoundingBox().getCenter(), 0.7F);
        }
        if (held.tick(level, t, new WhipSnare.Hurt(wheel().value("lassoDamage"), wheel().value("lassoSlowSeconds")))) {
            held.send(level, t);
        } else {
            held.gone(level);
            this.snare = null;
        }
    }

    @FunctionalInterface
    public interface Sounding {
        void play(SoundEvent sound, float volume, float pitch);
    }

    public static void equipSounds(float from, float to, Sounding sounding) {
        if (crossed(from, to, 2)) {
            sounding.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.3F);
            sounding.play(SoundEvents.BEACON_POWER_SELECT, 0.5F, 1.7F);
        }
        if (crossed(from, to, WhipMove.FORMED)) {
            sounding.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9F, 1.4F);
            sounding.play(SoundEvents.ARMOR_EQUIP_LEATHER.value(), 0.7F, 1.2F);
        }
        for (int k = 0; k < 4; k++) {
            if (crossed(from, to, WhipMove.FORMED + 2 + 3 * k)) {
                sounding.play(SoundEvents.AMETHYST_BLOCK_HIT, 0.35F, 1.3F + 0.12F * k);
            }
        }
        if (crossed(from, to, WhipMove.POURED)) {
            sounding.play(SoundEvents.LEASH_KNOT_PLACE, 0.5F, 1.4F);
        }
        if (crossed(from, to, WhipMove.RISE)) {
            sounding.play(SoundEvents.FISHING_BOBBER_THROW, 0.7F, 1.2F);
        }
        for (int k = 0; k < 4; k++) {
            if (crossed(from, to, WhipMove.TWIRL + 1 + 3 * k)) {
                sounding.play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.45F, 1.3F + 0.08F * k);
            }
        }
        if (crossed(from, to, WhipMove.THROW)) {
            sounding.play(SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 0.9F);
        }
        WhipMove.Crack[] cracks = WhipMove.EQUIP.cracks();
        if (cracks.length > 0 && crossed(from, to, cracks[0].tick())) {
            sounding.play(SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, 1.85F);
            sounding.play(SoundEvents.AMETHYST_BLOCK_HIT, 0.7F, 1.9F);
            sounding.play(SoundEvents.BEACON_POWER_SELECT, 0.35F, 2.0F);
        }
        if (crossed(from, to, WhipMove.THROW + 10)) {
            sounding.play(SoundEvents.LEASH_KNOT_PLACE, 0.45F, 1.3F);
        }
    }

    private static boolean crossed(float from, float to, float moment) {
        return from < moment && moment <= to;
    }

    private void breakUp() {
        if (this.breaking >= 0) {
            return;
        }
        this.breaking = 0;
        this.whirling = false;
        this.spinning = false;
        if (this.snare != null) {
            this.snare.gone(this.owner.serverLevel());
            this.snare = null;
        }
        this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 1.3F);
    }

    private void send(ServerLevel level) {
        Vec3 at = this.owner.position();
        int move = this.move.ordinal() | (this.whirling ? WhipMove.WHIRLING : 0)
                | (this.spinning ? WhipMove.SPINNING : 0);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.owner.getLookAngle(), this.breaking, 1.0F,
                        this.moveStart, true, ConstructPayload.WHIP, move, this.age, null));
    }

    private void soundForOthers(SoundEvent sound, float volume, float pitch) {
        this.owner.level().playSound(this.owner, this.owner.getX(), this.owner.getY() + 1.0, this.owner.getZ(), sound,
                SoundSource.PLAYERS, volume, pitch);
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        EnergyWhip whip = HELD.get(player.getUUID());
        if (whip == null || whip.breaking >= 0 || !whip.spinning || whip.age - whip.moveStart < WhipMove.SPIN_GUARD
                || event.getAmount() <= 0.0F || LightShield.goesThrough(event.getSource())) {
            return;
        }
        Vec3 front = flat(player.getLookAngle());
        Vec3 from = event.getSource().getSourcePosition();
        if (from != null) {
            Vec3 toSource = new Vec3(from.x - player.getX(), 0.0, from.z - player.getZ());
            if (toSource.lengthSqr() > 1.0E-4 && front.dot(toSource.normalize()) < FRONT) {
                return;
            }
        }
        event.setAmount(event.getAmount() * (float) wheel().value("spinDamageKept"));
        whip.sound(SoundEvents.SHIELD_BLOCK, 0.9F, 1.3F + 0.2F * player.getRandom().nextFloat());
        whip.sound(SoundEvents.AMETHYST_BLOCK_HIT, 0.8F, 1.8F);
        Vec3 disc = player.getEyePosition().add(player.getLookAngle().scale(1.6)).subtract(0.0, 0.25, 0.0);
        ParticleFx.cloud(player.serverLevel(), ParticleFx.dust(PowerRing.BRIGHT, 0.8F), disc, 4, 0.4, 0.06);
    }
}
