package nl.tivek.multiversepowers.character.greenlantern;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.Fear;
import nl.tivek.multiversepowers.character.greenlantern.ability.Recharge;
import nl.tivek.multiversepowers.engine.effect.Effect;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

public final class Arrival implements Effect {
    public static final int SET_OFF = 10;
    public static final int APPROACH = 40;
    public static final int SCAN = 41;
    public static final int SCANNED = 57;
    public static final int LANTERN_FORM = 58;
    public static final int LANTERN_FORMED = 68;
    public static final int LANTERN_CAUGHT = 76;
    public static final int RING_FLY = 78;
    public static final int RING_ON = 85;
    public static final int SUIT_TICKS = 36;
    public static final int DRESSED = RING_ON + SUIT_TICKS;
    public static final int RECHARGE = DRESSED + 2;
    public static final int TICKS = RECHARGE + PowerRing.RECHARGE_TICKS;
    public static final double HOVER = 3.0;
    public static final double PILLAR_HIGH = 24.0;
    public static final double FEAR_RADIUS = 16.0;
    private static final int FEAR_TICKS = 200;
    private static final double FEAR_PUSH = 0.9;
    private static final double NEAR = 26.0;
    private static final double FAR = 42.0;
    private static final double LOW = 8.0;
    private static final double HIGH = 20.0;
    private static final int PASS = 25;
    private static final double SPREAD = 50.0;

    private static final Map<UUID, Arrival> ACTIVE = new HashMap<>();

    private final ServerPlayer owner;
    private final Vec3 from;
    private int ticks;

    private Arrival(ServerPlayer owner, Vec3 from) {
        this.owner = owner;
        this.from = from;
    }

    public static void begin(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        Arrival arrival = new Arrival(owner, start(owner, level));
        ACTIVE.put(owner.getUUID(), arrival);
        Effects.start(level, arrival);
        arrival.soundAt(level, arrival.from, SoundEvents.CONDUIT_ACTIVATE, 1.6F, 1.4F);
        arrival.soundAt(level, arrival.from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.8F);
        PowerRing.sync(owner);
    }

    public static void end(ServerPlayer owner) {
        if (ACTIVE.remove(owner.getUUID()) != null) {
            PowerRing.sync(owner);
        }
    }

    public static boolean busy(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    static int ticks(ServerPlayer player) {
        Arrival arrival = ACTIVE.get(player.getUUID());
        return arrival == null ? -1 : arrival.ticks;
    }

    @Nullable
    static Vec3 from(ServerPlayer player) {
        Arrival arrival = ACTIVE.get(player.getUUID());
        return arrival == null ? null : arrival.from;
    }

    static void clear() {
        ACTIVE.clear();
    }

    private static Vec3 start(ServerPlayer owner, ServerLevel level) {
        RandomSource random = owner.getRandom();
        Vec3 eye = owner.getEyePosition();
        Vec3 best = eye.add(owner.getLookAngle().scale(2.0));
        double bestReach = -1.0;
        for (int attempt = 0; attempt < 16; attempt++) {
            double yaw = Math.toRadians(owner.getYRot() + (random.nextDouble() * 2.0 - 1.0) * SPREAD);
            double distance = NEAR + random.nextDouble() * (FAR - NEAR);
            double rise = LOW + random.nextDouble() * (HIGH - LOW);
            Vec3 at = eye.add(-Math.sin(yaw) * distance, rise, Math.cos(yaw) * distance);
            BlockHitResult hit = level.clip(new ClipContext(eye, at, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE,
                    owner));
            if (hit.getType() == HitResult.Type.MISS) {
                return at;
            }
            double reach = hit.getLocation().distanceTo(eye) - 1.0;
            if (reach > bestReach) {
                bestReach = reach;
                best = eye.add(at.subtract(eye).normalize().scale(Math.max(1.5, reach)));
            }
        }
        return best;
    }

    @Override
    public boolean tick(ServerLevel level, int age) {
        if (ACTIVE.get(this.owner.getUUID()) != this) {
            return false;
        }
        if (!PowerRing.fuels(this.owner, level)) {
            end(this.owner);
            return false;
        }
        this.ticks++;
        switch (this.ticks) {
            case SET_OFF -> this.sound(level, SoundEvents.TRIDENT_RIPTIDE_2, 0.8F, 1.3F);
            case PASS -> this.sound(level, SoundEvents.TRIDENT_RIPTIDE_1, 0.7F, 1.7F);
            case APPROACH -> {
                this.sound(level, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.5F);
                this.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, 1.1F);
            }
            case SCAN -> {
                this.sound(level, SoundEvents.BEACON_AMBIENT, 1.4F, 2.0F);
                this.sound(level, SoundEvents.CONDUIT_ATTACK_TARGET, 0.6F, 1.8F);
            }
            case SCANNED - 4 -> {
                this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.0F);
                this.say("arrival_chosen", this.owner.getName());
            }
            case LANTERN_FORM -> this.sound(level, SoundEvents.ENCHANTMENT_TABLE_USE, 1.2F, 0.8F);
            case LANTERN_FORMED -> {
                this.sound(level, SoundEvents.BEACON_AMBIENT, 1.2F, 1.4F);
                this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.3F);
            }
            case LANTERN_CAUGHT -> {
                this.sound(level, SoundEvents.LANTERN_PLACE, 1.2F, 0.8F);
                this.sound(level, SoundEvents.AMETHYST_BLOCK_HIT, 1.0F, 0.9F);
            }
            case RING_FLY -> this.sound(level, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.6F);
            case RING_ON -> this.ringOn(level);
            case DRESSED -> {
                this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
                this.say("arrival_welcome");
            }
            case RECHARGE -> Recharge.arrive(this.owner, level);
            default -> {
                int into = this.ticks - RING_ON;
                if (into > 0 && into < SUIT_TICKS - 4 && into % 6 == 0) {
                    this.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.7F + 0.1F * into / 6.0F);
                } else if (into == SUIT_TICKS - 4) {
                    this.sound(level, SoundEvents.BEACON_POWER_SELECT, 0.9F, 1.8F);
                }
            }
        }
        if (this.ticks % 20 == 0) {
            PowerRing.sync(this.owner);
        }
        if (this.ticks >= TICKS) {
            end(this.owner);
            return false;
        }
        return true;
    }

    private void ringOn(ServerLevel level) {
        Vec3 feet = this.owner.position();
        this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.6F, 1.2F);
        this.sound(level, SoundEvents.WIND_CHARGE_BURST.value(), 1.4F, 0.6F);
        this.sound(level, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.2F, 1.4F);
        this.sound(level, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9F, 1.5F);
        ParticleFx.sphereOut(level, ParticleFx.dust(PowerRing.BRIGHT, 1.6F),
                this.owner.getEyePosition().subtract(0.0, 0.4, 0.0), 30, 0.35);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.GREEN, 2.0F), feet.add(0.0, 0.2, 0.0), 90, 1.4);
        ParticleFx.shockwave(level, ParticleFx.dust(PowerRing.PALE, 1.4F), feet.add(0.0, 0.4, 0.0), 60, 0.9);
        for (int k = 0; k < 2; k++) {
            ParticleFx.helix(level, ParticleFx.dust(k == 0 ? PowerRing.BRIGHT : PowerRing.GREEN, 1.3F), feet, 0.7,
                    PILLAR_HIGH * 0.5, 3.0, 48, Math.PI * k);
        }
        Fear.strike(this.owner, level, FEAR_RADIUS, FEAR_TICKS, FEAR_PUSH);
    }

    private void say(String key, Object... args) {
        this.owner.displayClientMessage(Component.translatable("ring." + MultiversePowers.MODID + "." + key, args)
                .withColor(PowerRing.GREEN), true);
    }

    private void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        this.soundAt(level, this.owner.position().add(0.0, 1.2, 0.0), sound, volume, pitch);
    }

    private void sound(ServerLevel level, Holder<SoundEvent> sound, float volume, float pitch) {
        this.sound(level, sound.value(), volume, pitch);
    }

    private void soundAt(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
