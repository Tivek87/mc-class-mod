package nl.tivek.multiversepowers.character.greenlantern.client;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.body.BackSpot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlightPose;
import nl.tivek.multiversepowers.character.greenlantern.client.render.JetpackPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;

// Every client works out the jetpack itself from how the flyer lies, so all see it come and go alike.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Jetpacks {
    // Lying this flat he has picked up speed, and the ring puts the jetpack on.
    private static final float LIES = 0.5F;
    private static final float THRUST_FOLLOWS = 0.3F;
    private static final Map<Integer, Pack> PACKS = new HashMap<>();
    private static int clientTicks;

    private static final class Pack {
        private final int since;
        private int broke = Integer.MIN_VALUE;
        private float thrust;
        private float thrustO;
        @Nullable
        private Roar roar;

        private Pack(int since) {
            this.since = since;
        }

        private float age(float partialTick) {
            return clientTicks - this.since + partialTick;
        }

        private float broken(float partialTick) {
            return this.broke == Integer.MIN_VALUE ? -1.0F : clientTicks - this.broke + partialTick;
        }
    }

    private Jetpacks() {
    }

    public static boolean any() {
        return !PACKS.isEmpty();
    }

    public static boolean has(Entity player) {
        return PACKS.containsKey(player.getId());
    }

    public static void draw(LanternPainter painter, AbstractClientPlayer player, @Nullable Vec3 ring, boolean own,
            float partialTick) {
        Pack pack = PACKS.get(player.getId());
        if (pack == null) {
            return;
        }
        float t = pack.age(partialTick);
        float broken = pack.broken(partialTick);
        ConstructPainter.Frame back = BackSpot.of(player);
        if (back == null) {
            // In first person your body is not drawn: the ball still flies from your ring round behind you.
            if (own && ring != null && broken < 0.0F && t < JetpackPainter.FLIES + 3.0F) {
                Vec3 look = player.getViewVector(partialTick);
                Vec3 behind = player.getEyePosition(partialTick).subtract(look.scale(0.3)).subtract(0.0, 0.35, 0.0);
                JetpackPainter.ball(painter, ring, behind, look.scale(-1.0), t);
            }
            return;
        }
        double apart = broken < 0.0F ? 0.0 : Math.max(1.0E-3, broken / JetpackPainter.BREAK_TICKS);
        if (apart >= 1.0) {
            return;
        }
        if (ring != null && broken < 0.0F) {
            JetpackPainter.ball(painter, ring, back.center(), back.forward().normalize(), t);
        }
        JetpackPainter.pack(painter, back, t, apart);
        if (apart <= 0.0) {
            JetpackPainter.flames(painter, back, t, Mth.lerp(partialTick, pack.thrustO, pack.thrust));
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        for (AbstractClientPlayer player : level.players()) {
            Pack pack = PACKS.get(player.getId());
            boolean flying = ClientRing.flight(player, 0.0F) >= 0.0F && !ClientRing.has(player, RingPayload.DESCENT);
            if (pack == null) {
                if (flying && FlightPose.lying(player, 0.0F) >= LIES) {
                    PACKS.put(player.getId(), new Pack(clientTicks));
                    sound(level, player, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.7F);
                    sound(level, player, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.8F);
                }
                continue;
            }
            if (pack.broke != Integer.MIN_VALUE) {
                continue;
            }
            if (!flying) {
                pack.broke = clientTicks;
                if (pack.roar != null) {
                    pack.roar.on = false;
                }
                if (pack.age(0.0F) >= JetpackPainter.FLIES) {
                    sound(level, player, SoundEvents.AMETHYST_CLUSTER_BREAK, 1.0F, 1.1F);
                    sound(level, player, SoundEvents.FIRE_EXTINGUISH, 0.5F, 1.6F);
                }
                continue;
            }
            tick(level, player, pack);
        }
        PACKS.entrySet().removeIf(entry -> {
            Pack pack = entry.getValue();
            boolean gone = level.getEntity(entry.getKey()) == null || pack.broke != Integer.MIN_VALUE
                    && clientTicks - pack.broke > JetpackPainter.BREAK_TICKS;
            if (gone && pack.roar != null) {
                pack.roar.on = false;
            }
            return gone;
        });
    }

    private static void tick(ClientLevel level, AbstractClientPlayer player, Pack pack) {
        int age = clientTicks - pack.since;
        if (age == (int) JetpackPainter.FLIES) {
            sound(level, player, SoundEvents.AMETHYST_BLOCK_PLACE, 1.0F, 0.9F);
            sound(level, player, SoundEvents.ARMOR_EQUIP_IRON.value(), 0.9F, 1.1F);
        } else if (age == (int) JetpackPainter.SPARK) {
            sound(level, player, SoundEvents.FLINTANDSTEEL_USE, 0.8F, 1.3F);
        } else if (age == (int) JetpackPainter.LIT) {
            sound(level, player, SoundEvents.FIRECHARGE_USE, 0.8F, 1.2F);
            sound(level, player, SoundEvents.BLAZE_SHOOT, 0.5F, 0.8F);
            pack.roar = new Roar(player, pack);
            Minecraft.getInstance().getSoundManager().play(pack.roar);
        }
        ClientFlight.Motion motion = ClientFlight.motion(player);
        float speed = motion == null ? 0.0F : (float) (motion.velocity.length() / ClientFlight.fullSpeed());
        pack.thrustO = pack.thrust;
        pack.thrust = Mth.lerp(THRUST_FOLLOWS, pack.thrust, Mth.clamp(speed, 0.0F, 1.0F));
    }

    private static void sound(ClientLevel level, Entity player, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(player.getX(), player.getY() + 1.0, player.getZ(), sound, SoundSource.PLAYERS, volume,
                pitch, false);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        PACKS.clear();
        BackSpot.clear();
    }

    private static final class Roar extends AbstractTickableSoundInstance {
        private final Entity player;
        private final Pack pack;
        private boolean on = true;

        private Roar(Entity player, Pack pack) {
            super(SoundEvents.BLAZE_BURN, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.pack = pack;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.05F;
            this.pitch = 0.7F;
            this.x = player.getX();
            this.y = player.getY() + 1.0;
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            if (this.player.isRemoved()) {
                this.stop();
                return;
            }
            this.x = this.player.getX();
            this.y = this.player.getY() + 1.0;
            this.z = this.player.getZ();
            float loud = this.on ? 0.12F + 0.38F * this.pack.thrust : 0.0F;
            this.volume = Mth.lerp(this.on ? 0.3F : 0.4F, this.volume, loud);
            this.pitch = Mth.lerp(0.2F, this.pitch, 0.7F + 0.3F * this.pack.thrust);
            if (!this.on && this.volume < 0.02F) {
                this.stop();
            }
        }
    }
}
