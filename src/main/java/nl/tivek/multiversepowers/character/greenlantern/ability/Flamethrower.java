package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
public final class Flamethrower extends FlameHits {
    public static final int BREAK_TICKS = 8;
    // A move may start this many ticks before the last one is ready: the click came over the network a little later.
    private static final int SLACK = 2;
    private static final double VIEW_RANGE = 96.0;
    private static final int GUARD_FROM = 4;

    private static final Map<UUID, Flamethrower> HELD = new HashMap<>();
    private static final Throttle PICK_SOUND = new Throttle(5);

    private final int id = PowerRing.newId();
    private boolean pouring;
    private boolean swirling;
    private int breaking = -1;

    private Flamethrower(ServerPlayer owner) {
        super(owner);
    }

    public static void hold(ServerPlayer player, Construct construct) {
        Flamethrower now = HELD.get(player.getUUID());
        boolean want = construct == Construct.FLAMETHROWER && Characters.of(player) == GameCharacter.GREEN_LANTERN
                && player.isAlive() && !Arrival.busy(player);
        if (want && (now == null || now.breaking >= 0)) {
            Flamethrower gun = new Flamethrower(player);
            if (now != null) {
                now.breaking = Math.max(now.breaking, BREAK_TICKS - 1);
            }
            HELD.put(player.getUUID(), gun);
            Effects.start(player.serverLevel(), gun);
            if (PICK_SOUND.allow(player)) {
                gun.soundForOthers(SoundEvents.BEACON_POWER_SELECT, 0.8F, 1.4F);
                gun.soundForOthers(SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.8F);
            }
            gun.send(player.serverLevel());
        } else if (!want && now != null) {
            now.breakUp();
        }
    }

    public static boolean equipped(ServerPlayer player) {
        Flamethrower gun = HELD.get(player.getUUID());
        return gun != null && gun.breaking < 0;
    }

    public static boolean attack(ServerPlayer player, ServerLevel level, boolean on, int data) {
        Flamethrower gun = HELD.get(player.getUUID());
        if (gun == null || gun.breaking >= 0) {
            return false;
        }
        if (!on) {
            return gun.stopPouring();
        }
        if ((data & Characters.HOLD) != 0) {
            return gun.startPouring();
        }
        if ((data & Characters.TAP) == 0) {
            return false;
        }
        FlameMove asked = FlameMove.byIndex(data >> Characters.MOVE_SHIFT);
        return gun.begin(asked != null && asked.kind() == FlameMove.Kind.SWEEP ? asked
                : FlameMove.sweepAfter(gun.move));
    }

    public static boolean defend(ServerPlayer player, ServerLevel level, boolean on, int data) {
        Flamethrower gun = HELD.get(player.getUUID());
        if (gun == null || gun.breaking >= 0) {
            return false;
        }
        if (!on) {
            return gun.stopSwirling();
        }
        if ((data & Characters.HOLD) != 0) {
            return gun.startSwirling();
        }
        return (data & Characters.TAP) != 0 && gun.startWall();
    }

    public static void clear() {
        HELD.clear();
        PICK_SOUND.clear();
        FlameWall.clear();
        FlameBurn.clear();
    }

    private boolean free() {
        return !this.pouring && !this.swirling && this.age - this.moveStart >= this.move.ready() - SLACK;
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

    private void start(FlameMove next) {
        this.move = next;
        this.moveStart = this.age;
        this.swept.clear();
    }

    private boolean begin(FlameMove sweep) {
        if (!this.free() || !this.pay("sweepPowerCost")) {
            return false;
        }
        this.start(sweep);
        this.sound(SoundEvents.FIRECHARGE_USE, 0.8F, 1.1F + 0.2F * this.owner.getRandom().nextFloat());
        this.sound(SoundEvents.BLAZE_SHOOT, 0.45F, 1.5F);
        this.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 0.5F, 0.8F);
        return true;
    }

    private boolean startPouring() {
        if (!this.free() || !this.drain("infernoPowerPerSecond")) {
            return false;
        }
        this.start(FlameMove.INFERNO);
        this.pouring = true;
        this.sound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.7F);
        this.sound(SoundEvents.BLAZE_SHOOT, 0.8F, 0.6F);
        return true;
    }

    private boolean stopPouring() {
        if (!this.pouring) {
            return false;
        }
        this.pouring = false;
        this.start(FlameMove.VENT);
        this.sound(SoundEvents.FIRE_EXTINGUISH, 0.6F, 1.0F);
        return true;
    }

    private boolean startSwirling() {
        if (!this.free() || !this.drain("vortexPowerPerSecond")) {
            return false;
        }
        this.start(FlameMove.VORTEX);
        this.swirling = true;
        this.sound(SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
        this.sound(SoundEvents.BLAZE_SHOOT, 0.6F, 0.8F);
        return true;
    }

    private boolean stopSwirling() {
        if (!this.swirling) {
            return false;
        }
        this.swirling = false;
        this.start(FlameMove.BURST);
        return true;
    }

    private boolean startWall() {
        if (!this.free()) {
            return false;
        }
        if (FlameWall.base(this.owner.level(), this.owner, this.owner.getLookAngle()) == null) {
            PowerRing.tell(this.owner, "wall_no_ground");
            return false;
        }
        if (!this.pay("wallPowerCost")) {
            return false;
        }
        this.start(FlameMove.WALL);
        this.sound(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.6F, 1.3F);
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
            case SWEEP -> {
                if (t > FlameMove.SPRAY_FROM && t <= FlameMove.SPRAY_TO + 2) {
                    this.sweep(level, t);
                }
            }
            case INFERNO -> {
                if (!this.drain("infernoPowerPerSecond")) {
                    this.stopPouring();
                } else {
                    int every = Math.max(1, (int) wheel().value("infernoTicks"));
                    if (t >= FlameMove.BRACE && (t - FlameMove.BRACE) % every == 0) {
                        this.pour(level);
                    }
                    if (t % 10 == 0) {
                        this.sound(SoundEvents.FIRE_AMBIENT, 0.9F, 1.3F);
                    }
                }
            }
            case VORTEX -> {
                if (!this.drain("vortexPowerPerSecond")) {
                    this.stopSwirling();
                } else {
                    if (t >= FlameMove.SPIN_UP && (t - FlameMove.SPIN_UP) % 10 == 0) {
                        this.swirl(level, false);
                    }
                    if (t >= GUARD_FROM) {
                        this.scorch(level);
                    }
                    if (t % 12 == 0) {
                        this.sound(SoundEvents.FIRE_AMBIENT, 1.0F, 0.9F);
                    }
                }
            }
            case BURST -> {
                if (t == FlameMove.BLAST) {
                    this.swirl(level, true);
                    this.sound(SoundEvents.BLAZE_SHOOT, 1.0F, 0.6F);
                    this.sound(SoundEvents.GENERIC_EXPLODE.value(), 0.45F, 1.5F);
                    ParticleFx.shockwave(level, ParticleFx.dust(0xE4FFEA, 1.6F), this.owner.position().add(0.0, 0.6,
                            0.0), 48, 0.6);
                }
            }
            case WALL -> {
                if (t == FlameMove.LAY_FROM) {
                    Vec3 base = FlameWall.base(level, this.owner, this.owner.getLookAngle());
                    if (base != null) {
                        FlameWall.start(level, this.owner, base, flat(this.owner.getLookAngle()), wheel());
                    } else {
                        PowerRing.setPower(this.owner, PowerRing.power(this.owner)
                                + (float) wheel().value("wallPowerCost"));
                        PowerRing.tell(this.owner, "wall_no_ground");
                    }
                }
            }
            case VENT -> {
            }
        }
        this.send(level);
        return true;
    }

    @FunctionalInterface
    public interface Sounding {
        void play(SoundEvent sound, float volume, float pitch);
    }

    public static void equipSounds(float from, float to, Sounding sounding) {
        if (crossed(from, to, 3)) {
            sounding.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.6F, 1.3F);
        }
        for (int k = 0; k < 3; k++) {
            if (crossed(from, to, 9 + 2 * k)) {
                sounding.play(SoundEvents.AMETHYST_BLOCK_HIT, 0.45F, 1.6F + 0.15F * k);
            }
        }
        if (crossed(from, to, FlameMove.LEFT_GRAB)) {
            sounding.play(SoundEvents.ARMOR_EQUIP_IRON.value(), 0.8F, 1.25F);
        }
        if (crossed(from, to, FlameMove.VALVE)) {
            sounding.play(SoundEvents.LEVER_CLICK, 0.7F, 1.5F);
            sounding.play(SoundEvents.IRON_TRAPDOOR_CLOSE, 0.35F, 1.8F);
        }
        if (crossed(from, to, FlameMove.VALVE + 1)) {
            sounding.play(SoundEvents.FIRE_EXTINGUISH, 0.25F, 1.9F);
            sounding.play(SoundEvents.BEACON_ACTIVATE, 0.4F, 1.9F);
        }
        if (crossed(from, to, FlameMove.SPARK)) {
            sounding.play(SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.3F);
        }
        if (crossed(from, to, FlameMove.SPARK_AGAIN)) {
            sounding.play(SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.5F);
        }
        if (crossed(from, to, FlameMove.PILOT)) {
            sounding.play(SoundEvents.FIRECHARGE_USE, 0.45F, 1.7F);
            sounding.play(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.5F);
        }
        if (crossed(from, to, FlameMove.TEST)) {
            sounding.play(SoundEvents.BLAZE_SHOOT, 1.0F, 0.7F);
            sounding.play(SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
            sounding.play(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 0.9F);
        }
        if (crossed(from, to, FlameMove.HISS)) {
            sounding.play(SoundEvents.FIRE_EXTINGUISH, 0.35F, 1.3F);
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
        this.pouring = false;
        this.swirling = false;
        this.sound(SoundEvents.AMETHYST_CLUSTER_BREAK, 0.9F, 1.2F);
    }

    private void send(ServerLevel level) {
        Vec3 at = this.owner.position();
        int move = this.move.ordinal() | (this.pouring ? FlameMove.FIRING : 0)
                | (this.swirling ? FlameMove.SWIRLING : 0);
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, VIEW_RANGE,
                new ConstructPayload(this.id, this.owner.getId(), at, this.owner.getLookAngle(), this.breaking, 1.0F,
                        this.moveStart, true, ConstructPayload.FLAME, move, this.age, null));
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
        Flamethrower gun = HELD.get(player.getUUID());
        if (gun == null || gun.breaking >= 0 || event.getAmount() <= 0.0F
                || LightShield.goesThrough(event.getSource())) {
            return;
        }
        int t = gun.age - gun.moveStart;
        boolean guarding = gun.swirling && t >= GUARD_FROM || gun.move == FlameMove.BURST && t <= FlameMove.BLAST;
        if (!guarding) {
            return;
        }
        event.setAmount(event.getAmount() * (float) wheel().value("vortexDamageKept"));
        gun.sound(SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.5F);
        Vec3 from = event.getSource().getSourcePosition();
        Vec3 at = player.position().add(0.0, 1.0, 0.0);
        if (from != null) {
            Vec3 toward = from.subtract(at);
            if (toward.lengthSqr() > 1.0E-4) {
                at = at.add(toward.normalize().scale(wheel().value("vortexRadius") * 0.8));
            }
        }
        ParticleFx.cloud(player.serverLevel(), ParticleFx.fade(0xE4FFEA, PowerRing.GREEN, 1.2F), at, 8, 0.3, 0.06);
    }
}
