package nl.tivek.multiversepowers.engine.client.fx;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class CameraShake {
    private static float strength;
    private static float fade;
    private static int clock;

    private CameraShake() {
    }

    // A jolt of the view that dies away over the given ticks; a stronger jolt replaces a weaker one.
    public static void add(float degrees, int ticks) {
        float scaled = degrees * ClientSettings.cameraShake();
        if (scaled > strength && ticks > 0) {
            strength = scaled;
            fade = scaled / ticks;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!Minecraft.getInstance().isPaused()) {
            clock++;
            strength = Math.max(0.0F, strength - fade);
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (strength <= 0.0F) {
            return;
        }
        float[] jolt = jolt((float) event.getPartialTick());
        event.setYaw(event.getYaw() + jolt[0]);
        event.setPitch(event.getPitch() + jolt[1]);
        event.setRoll(event.getRoll() + jolt[2]);
    }

    // The shake's turn right now, in degrees: yaw, pitch and roll.
    public static float[] jolt(float partialTick) {
        if (strength <= 0.0F) {
            return new float[3];
        }
        float time = clock + partialTick;
        float now = Math.max(0.0F, strength - fade * partialTick);
        return new float[] { 0.8F * now * Mth.sin(time * 4.3F + 1.3F), now * Mth.sin(time * 3.7F),
                0.6F * now * Mth.sin(time * 2.9F + 2.1F) };
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        strength = 0.0F;
    }
}
