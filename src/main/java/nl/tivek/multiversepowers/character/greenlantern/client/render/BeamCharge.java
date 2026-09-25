package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
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
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class BeamCharge {
    private static final float SLACK = 10.0F;
    private static final double OWN_SCALE = 0.3;
    private static final float KICK = 3.0F;
    private static final float KICK_TICKS = 6.0F;

    private static final Map<Integer, ChargeSound> SOUNDS = new HashMap<>();
    private static int clientTicks;

    private BeamCharge() {
    }

    public static float charge(Entity player, float partialTick) {
        if (ClientRing.has(player, RingPayload.BEAM) || ClientRing.power(player) <= 0.0F) {
            return -1.0F;
        }
        CharacterAbility bolt = GameCharacter.GREEN_LANTERN.byName("light_bolt");
        if (bolt == null || bolt.holdTicks() <= 0) {
            return -1.0F;
        }
        if (player == Minecraft.getInstance().player) {
            // Sword and shield own the hold button then, not the beam charge
            if (ClientCharacter.active() != GameCharacter.GREEN_LANTERN || SwordArms.holding()
                    || FlameArms.holding()) {
                return -1.0F;
            }
            float progress = MouseHold.progress(bolt, partialTick);
            return progress < 0.0F || progress >= 1.0F ? -1.0F : progress;
        }
        float ticks = ClientRing.charging(player, partialTick);
        if (ticks < 0.0F || ticks > bolt.holdTicks() + SLACK) {
            return -1.0F;
        }
        return Math.min(1.0F, ticks / bolt.holdTicks());
    }

    public static boolean any(ClientLevel level) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        for (AbstractClientPlayer player : level.players()) {
            if (charge(player, partialTick) >= 0.0F) {
                return true;
            }
        }
        return false;
    }

    public static void draw(LanternPainter painter, Entity player, Vec3 ring, Camera camera, float partialTick) {
        float charge = charge(player, partialTick);
        if (charge < 0.0F) {
            return;
        }
        double c = charge;
        boolean own = player == Minecraft.getInstance().player && camera.getEntity() == player && !camera.isDetached();
        double scale = own ? OWN_SCALE : 1.0;
        double time = clientTicks + partialTick;
        Vec3 axis = player.getViewVector(partialTick);
        Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(Vectors.UP).normalize()
                : axis.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        Vec3 other = side.cross(axis).normalize();
        painter.flare(ring, (0.06 + 0.42 * c * c) * scale, 0.4 + 0.6 * c);
        double spin = time * (0.12 + 0.6 * c);
        double radius = (0.14 + 0.14 * c) * scale;
        for (int k = 0; k < 2; k++) {
            double tip = k == 0 ? spin : -1.3 * spin + 1.1;
            Vec3 a = (k == 0 ? side : other).scale(Math.cos(tip)).add(axis.scale(Math.sin(tip)));
            Vec3 b = k == 0 ? other : side;
            painter.circle(ring, a, b, radius, 0.012 * scale, 0.06 * scale, Colors.alpha(0.35 + 0.6 * c),
                    Colors.alpha(0.2 + 0.35 * c));
        }
        int specks = 6 + (int) (18 * c);
        double period = 12.0 - 7.0 * c;
        for (int k = 0; k < specks; k++) {
            double phase = time / period + Noise.of(k, 91, 0);
            int round = (int) Math.floor(phase);
            double cycle = phase - round;
            Vec3 way = Noise.direction(k, 91 + round);
            double far = 2.4 * (1.0 - cycle) * (1.0 - cycle) * scale;
            Vec3 head = ring.add(Vectors.spin(way, axis, 1.5 * cycle).scale(far));
            Vec3 tail = ring.add(Vectors.spin(way, axis, 1.5 * cycle - 0.3).scale(far + 0.25 * scale));
            painter.edge(tail, head, 0.025 * scale, (0.3 + 0.7 * cycle) * (0.4 + 0.6 * c));
        }
        if (c > 0.6) {
            int flick = (int) (time / 2.0);
            double crackle = (c - 0.6) / 0.4;
            for (int k = 0; k < 4; k++) {
                if (Noise.of(flick, k, 93) > crackle) {
                    continue;
                }
                Vec3 way = Noise.direction(flick, 93 + k);
                Vec3 bend = ring.add(way.scale(0.2 * scale)).add(Noise.direction(flick, 94 + k)
                        .scale(0.08 * scale));
                painter.edge(ring, bend, 0.02 * scale, 0.9);
                painter.edge(bend, ring.add(way.scale(0.4 * scale)), 0.015 * scale, 0.7);
            }
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
            if (charge(player, 0.0F) >= 0.0F && !SOUNDS.containsKey(player.getId())) {
                ChargeSound sound = new ChargeSound(player);
                SOUNDS.put(player.getId(), sound);
                minecraft.getSoundManager().play(sound);
            }
        }
        SOUNDS.values().removeIf(ChargeSound::isStopped);
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getEntity() != player) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float age = ClientConstructs.beamAge(player.getId(), partialTick);
        if (age < 0.0F) {
            return;
        }
        float time = player.tickCount + partialTick;
        float shake = ClientSettings.cameraShake();
        float kick = age < KICK_TICKS ? KICK * (1.0F - age / KICK_TICKS) * (1.0F - age / KICK_TICKS) : 0.0F;
        event.setPitch(event.getPitch() + shake * (-kick + 0.18F * Mth.sin(time * 3.1F)));
        event.setYaw(event.getYaw() + shake * 0.14F * Mth.sin(time * 2.3F + 1.0F));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SOUNDS.clear();
    }

    private static final class ChargeSound extends AbstractTickableSoundInstance {
        private final Entity player;

        ChargeSound(Entity player) {
            super(SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.25F;
            this.pitch = 0.5F;
            this.x = player.getX();
            this.y = player.getEyeY();
            this.z = player.getZ();
        }

        @Override
        public void tick() {
            float charge = charge(this.player, 0.0F);
            if (this.player.isRemoved() || charge < 0.0F) {
                this.stop();
                return;
            }
            this.x = this.player.getX();
            this.y = this.player.getEyeY();
            this.z = this.player.getZ();
            this.volume = 0.3F + 0.7F * charge;
            this.pitch = 0.5F + 1.5F * charge;
        }
    }
}
