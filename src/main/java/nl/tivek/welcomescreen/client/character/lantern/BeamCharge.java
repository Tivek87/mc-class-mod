package nl.tivek.welcomescreen.client.character.lantern;

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
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.character.ClientCharacter;
import nl.tivek.welcomescreen.client.character.MouseHold;
import nl.tivek.welcomescreen.network.RingPayload;

/**
 * The ring gathering its light for the beam, while he holds the attack button on its way there (see
 * {@link MouseHold}). The energy runs out of the core on his chest, down the ring arm into the ring and then over the
 * whole suit, a little further the longer he holds (drawn by {@link SuitGlow}); specks of light stream into the ring
 * from all round, faster and faster; a ball of light swells in his fist with two rings of light turning round it, and
 * at the end sparks crackle off it. A whine rises with it. When the beam breaks loose your own view kicks back, and
 * trembles a little for as long as it pours. Light, not a construct.
 *
 * <p>Your own charge follows your own button; anyone else's the server's word (see {@link ClientRing#charging}).
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class BeamCharge {
    // How long after the button should have brought the beam anyone else's charge is still shown, in ticks.
    private static final float SLACK = 10.0F;
    // How much smaller all of it is drawn around your own ring in first person, where it hangs right before your eyes.
    private static final double OWN_SCALE = 0.3;
    // How hard your view kicks back as the beam breaks loose, in degrees, and for how many ticks.
    private static final float KICK = 3.0F;
    private static final float KICK_TICKS = 6.0F;

    private static final Map<Integer, ChargeSound> SOUNDS = new HashMap<>();
    private static int clientTicks;

    private BeamCharge() {
    }

    /** How far this player's ring has gathered its light for the beam, 0 to 1, or -1 while it gathers none. */
    static float charge(Entity player, float partialTick) {
        if (ClientRing.has(player, RingPayload.BEAM) || ClientRing.power(player) <= 0.0F) {
            return -1.0F;
        }
        CharacterAbility bolt = GameCharacter.GREEN_LANTERN.byName("light_bolt");
        if (bolt == null || bolt.holdTicks() <= 0) {
            return -1.0F;
        }
        if (player == Minecraft.getInstance().player) {
            if (ClientCharacter.active() != GameCharacter.GREEN_LANTERN) {
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

    /** True while anyone's ring in this level gathers its light for the beam. */
    static boolean any(ClientLevel level) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        for (AbstractClientPlayer player : level.players()) {
            if (charge(player, partialTick) >= 0.0F) {
                return true;
            }
        }
        return false;
    }

    /** The light gathering in one player's ring, at {@code ring}. */
    static void draw(ConstructPainter painter, Entity player, Vec3 ring, Camera camera, float partialTick) {
        float charge = charge(player, partialTick);
        if (charge < 0.0F) {
            return;
        }
        double c = charge;
        boolean own = player == Minecraft.getInstance().player && camera.getEntity() == player && !camera.isDetached();
        double scale = own ? OWN_SCALE : 1.0;
        double time = clientTicks + partialTick;
        Vec3 axis = player.getViewVector(partialTick);
        Vec3 side = Math.abs(axis.y) < 0.95 ? axis.cross(ConstructPainter.UP).normalize()
                : axis.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        Vec3 other = side.cross(axis).normalize();
        // The ball of light swelling in his fist.
        painter.flare(ring, (0.06 + 0.42 * c * c) * scale, 0.4 + 0.6 * c);
        // Two rings of light turning round it, faster and faster, tipped like the rings of a gyroscope.
        double spin = time * (0.12 + 0.6 * c);
        double radius = (0.14 + 0.14 * c) * scale;
        for (int k = 0; k < 2; k++) {
            double tip = k == 0 ? spin : -1.3 * spin + 1.1;
            Vec3 a = (k == 0 ? side : other).scale(Math.cos(tip)).add(axis.scale(Math.sin(tip)));
            Vec3 b = k == 0 ? other : side;
            painter.circle(ring, a, b, radius, 0.012 * scale, 0.06 * scale, ConstructPainter.alpha(0.35 + 0.6 * c),
                    ConstructPainter.alpha(0.2 + 0.35 * c));
        }
        // Specks of light streaming into the ring from all round, more and faster the fuller it gets, winding in.
        int specks = 6 + (int) (18 * c);
        double period = 12.0 - 7.0 * c;
        for (int k = 0; k < specks; k++) {
            double phase = time / period + ConstructPainter.noise(k, 91, 0);
            int round = (int) Math.floor(phase);
            double cycle = phase - round;
            Vec3 way = ConstructPainter.direction(k, 91 + round);
            double far = 2.4 * (1.0 - cycle) * (1.0 - cycle) * scale;
            Vec3 head = ring.add(ConstructPainter.spin(way, axis, 1.5 * cycle).scale(far));
            Vec3 tail = ring.add(ConstructPainter.spin(way, axis, 1.5 * cycle - 0.3).scale(far + 0.25 * scale));
            painter.edge(tail, head, 0.025 * scale, (0.3 + 0.7 * cycle) * (0.4 + 0.6 * c));
        }
        // Nearly full: sparks crackle off it, new ones every other tick.
        if (c > 0.6) {
            int flick = (int) (time / 2.0);
            double crackle = (c - 0.6) / 0.4;
            for (int k = 0; k < 4; k++) {
                if (ConstructPainter.noise(flick, k, 93) > crackle) {
                    continue;
                }
                Vec3 way = ConstructPainter.direction(flick, 93 + k);
                Vec3 bend = ring.add(way.scale(0.2 * scale)).add(ConstructPainter.direction(flick, 94 + k)
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
        // A whine rises with every charge.
        for (AbstractClientPlayer player : level.players()) {
            if (charge(player, 0.0F) >= 0.0F && !SOUNDS.containsKey(player.getId())) {
                ChargeSound sound = new ChargeSound(player);
                SOUNDS.put(player.getId(), sound);
                minecraft.getSoundManager().play(sound);
            }
        }
        SOUNDS.values().removeIf(ChargeSound::isStopped);
    }

    /** Your own beam breaking loose kicks your view back, and it trembles a little for as long as the beam pours. */
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
        float kick = age < KICK_TICKS ? KICK * (1.0F - age / KICK_TICKS) * (1.0F - age / KICK_TICKS) : 0.0F;
        event.setPitch(event.getPitch() - kick + 0.18F * Mth.sin(time * 3.1F));
        event.setYaw(event.getYaw() + 0.14F * Mth.sin(time * 2.3F + 1.0F));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SOUNDS.clear();
    }

    /** The whine of one charge: higher and louder the fuller the ring gets, gone the moment the charge ends. */
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
