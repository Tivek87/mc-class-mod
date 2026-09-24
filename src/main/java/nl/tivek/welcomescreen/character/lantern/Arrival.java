package nl.tivek.welcomescreen.character.lantern;

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
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.spell.SpellCasting;
import nl.tivek.welcomescreen.spell.SpellEffect;
import nl.tivek.welcomescreen.spell.SpellFx;

/**
 * Becoming Green Lantern: the ring comes for you, as it comes for everyone it chooses. It streaks down out of the sky
 * like a comet somewhere 26 to 42 blocks away, flares up there and pulses, then flies to you, circling you once on its
 * way down, to hang three blocks before your eyes. It scans you from head to toe and back, and speaks: you have the
 * ability to overcome great fear. It shapes your lantern out of its light, and the lantern flies into your left hand.
 * Then the ring flies onto the middle finger of your right hand, and the moment it is on, its light bursts out around
 * you in a shockwave that sends the creatures of the dark running (see {@link Fear}), and flares up around you. From
 * the ring the uniform spreads up your arm to the lantern on your chest, and from there over all of you; the mask over
 * your eyes comes last, your eyes light up, and the ring welcomes you to the Corps. Then you smack the ring into the
 * lantern, as you do to recharge, and it is done.
 *
 * <p>Every client plays it along the same timeline, from how long ago it began (see
 * {@link nl.tivek.welcomescreen.network.RingPayload}); the ticks
 * below are that timeline. Until it is over the ring does nothing else.
 */
public final class Arrival implements SpellEffect {
    /** The ring sets off from where it showed up. */
    public static final int SET_OFF = 16;
    /** The ring hangs three blocks before your eyes: its flight in from where it showed up is over. */
    public static final int APPROACH = 84;
    /** It scans you, from head to toe and back up, and has done so. */
    public static final int SCAN = 86;
    public static final int SCANNED = 120;
    /** It starts to shape the lantern out of its light, and has finished it. */
    public static final int LANTERN_FORM = 122;
    public static final int LANTERN_FORMED = 140;
    /** The lantern has flown into your left hand. */
    public static final int LANTERN_CAUGHT = 156;
    /** The ring sets off for your finger, and is on it: the shockwave, and the uniform starts to spread. */
    public static final int RING_FLY = 160;
    public static final int RING_ON = 174;
    /** How long the uniform takes to spread over you, the mask included. */
    public static final int SUIT_TICKS = 80;
    /** The uniform is complete, mask and all: your eyes light up. */
    public static final int DRESSED = RING_ON + SUIT_TICKS;
    /** You smack the ring into the lantern: the recharge that ends it all. */
    public static final int RECHARGE = DRESSED + 2;
    /** It is over. */
    public static final int TICKS = RECHARGE + PowerRing.RECHARGE_TICKS;
    /** How far before your eyes the ring hangs, in blocks. */
    public static final double HOVER = 3.0;
    /** How far the shockwave of the ring reaches, and how long the creatures of the dark run from it, in ticks. */
    public static final double FEAR_RADIUS = 16.0;
    private static final int FEAR_TICKS = 200;
    private static final double FEAR_PUSH = 0.9;
    // How far away the ring shows up, in blocks, and how high over your eyes.
    private static final double NEAR = 26.0;
    private static final double FAR = 42.0;
    private static final double LOW = 8.0;
    private static final double HIGH = 20.0;
    // The tick the ring, circling him, sweeps past behind him.
    private static final int PASS = 52;
    // How far to either side of where you look it may show up, in degrees, so you see it coming.
    private static final double SPREAD = 50.0;

    private static final Map<UUID, Arrival> ACTIVE = new HashMap<>();

    private final ServerPlayer owner;
    private final Vec3 from;
    private int ticks;

    private Arrival(ServerPlayer owner, Vec3 from) {
        this.owner = owner;
        this.from = from;
    }

    /** He has just become Green Lantern: the ring sets out for him. */
    public static void begin(ServerPlayer owner) {
        ServerLevel level = owner.serverLevel();
        Arrival arrival = new Arrival(owner, start(owner, level));
        ACTIVE.put(owner.getUUID(), arrival);
        SpellCasting.start(level, arrival);
        arrival.soundAt(level, arrival.from, SoundEvents.CONDUIT_ACTIVATE, 1.6F, 1.4F);
        arrival.soundAt(level, arrival.from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, 0.8F);
        PowerRing.sync(owner);
    }

    /** He is no longer Green Lantern (or gone): whatever of the arrival was still to come, does not. */
    public static void end(ServerPlayer owner) {
        if (ACTIVE.remove(owner.getUUID()) != null) {
            PowerRing.sync(owner);
        }
    }

    /** True while the ring is still on its way to him or dressing him: it makes nothing else yet. */
    public static boolean busy(ServerPlayer player) {
        return ACTIVE.containsKey(player.getUUID());
    }

    /** How many ticks ago this player's ring set out for him, or -1 when it is not on its way. */
    static int ticks(ServerPlayer player) {
        Arrival arrival = ACTIVE.get(player.getUUID());
        return arrival == null ? -1 : arrival.ticks;
    }

    /** Where this player's ring showed up, or null when it is not on its way. */
    @Nullable
    static Vec3 from(ServerPlayer player) {
        Arrival arrival = ACTIVE.get(player.getUUID());
        return arrival == null ? null : arrival.from;
    }

    /** The server stops: no ring is on its way any more. */
    static void clear() {
        ACTIVE.clear();
    }

    /**
     * Where the ring shows up: 26 to 42 blocks away, somewhat ahead of where he looks and high over his eyes, where
     * nothing is in between, so he sees it. Where every way is closed off (a cave) it shows up as far away as it
     * can.
     */
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
            case SCANNED - 8 -> {
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
            case RECHARGE -> Lantern.arrive(this.owner, level);
            default -> {
                // The uniform spreads: a chime every so often, higher every time; the mask comes on with its own.
                int into = this.ticks - RING_ON;
                if (into > 0 && into < SUIT_TICKS && into % 12 == 0) {
                    this.sound(level, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 0.7F + 0.1F * into / 12.0F);
                } else if (into == SUIT_TICKS - 6) {
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

    /**
     * The ring slides onto his finger: its light bursts out around him, a ring of it racing out over the ground, and
     * the creatures of the dark close by are thrown back and run.
     */
    private void ringOn(ServerLevel level) {
        Vec3 feet = this.owner.position();
        this.sound(level, SoundEvents.BEACON_ACTIVATE, 1.6F, 1.2F);
        this.sound(level, SoundEvents.WIND_CHARGE_BURST.value(), 1.4F, 0.6F);
        this.sound(level, SoundEvents.RESPAWN_ANCHOR_CHARGE, 1.2F, 1.4F);
        SpellFx.sphereOut(level, SpellFx.dust(PowerRing.BRIGHT, 1.6F), this.owner.getEyePosition().subtract(0.0, 0.4,
                0.0), 30, 0.35);
        SpellFx.shockwave(level, SpellFx.dust(PowerRing.GREEN, 2.0F), feet.add(0.0, 0.2, 0.0), 90, 1.4);
        SpellFx.shockwave(level, SpellFx.dust(PowerRing.PALE, 1.4F), feet.add(0.0, 0.4, 0.0), 60, 0.9);
        Fear.strike(this.owner, level, FEAR_RADIUS, FEAR_TICKS, FEAR_PUSH);
    }

    /** The ring speaks to him, in its own green, over his hotbar. */
    private void say(String key, Object... args) {
        this.owner.displayClientMessage(Component.translatable("ring." + WelcomeScreenMod.MODID + "." + key, args)
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
