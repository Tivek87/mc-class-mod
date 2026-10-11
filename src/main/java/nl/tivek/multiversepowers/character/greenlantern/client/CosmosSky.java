package nl.tivek.multiversepowers.character.greenlantern.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandCosmos;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.world.LocalSky;

// A Cosmos Test near turns this game's sky to night with the moon standing on the side the caster looks to, takes the
// moon out of it once the hand has it, drains it black after the burst and gives it all back at the end; the dive and
// the burst shake the view and the burst flashes it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class CosmosSky {
    // How near a Cosmos Test must be to darken this sky.
    private static final double RANGE = 192.0;
    // The time of night with the moon about 35 degrees up in the east, and in the west.
    private static final long MOON_EAST = 14880L;
    private static final long MOON_WEST = 21120L;
    private static double lastClock = -1.0;

    private CosmosSky() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.isPaused()) {
            return;
        }
        Vec3 eye = minecraft.player.getEyePosition();
        TrackedConstructs.Cosmos cosmos = TrackedConstructs.cosmos(eye, RANGE, 0.0F);
        if (cosmos == null) {
            lastClock = -1.0;
            return;
        }
        double clock = cosmos.clock();
        // The caster stands on the far side of the facing: the moon goes where he looks.
        long time = -cosmos.facing().x < 0.0 ? MOON_WEST : MOON_EAST;
        LocalSky.wish(HandCosmos.night(clock), time, HandCosmos.black(clock), HandCosmos.moonless(clock));
        double near = 1.0 - Math.min(1.0, eye.distanceTo(cosmos.center()) / RANGE);
        if (crossed(clock, HandCosmos.PLUNGES)) {
            CameraShake.add((float) (2.5 * near), 10);
        }
        if (crossed(clock, HandCosmos.BURSTS)) {
            ScreenFlash.add(0xEEFFF2, 0.75F, 14);
            CameraShake.add(4.0F, 24);
        }
        lastClock = clock;
    }

    private static boolean crossed(double clock, int at) {
        return lastClock >= 0.0 && lastClock < at && clock >= at;
    }
}
