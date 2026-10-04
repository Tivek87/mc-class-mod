package nl.tivek.multiversepowers.engine.client.world;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// Weather only where something makes it, such as a power's storm: over the world's own, in this game only. Once a tick
// every source says how hard it rains and thunders where the camera is (0 to 1); the game then darkens its sky, draws
// its rain and plays its sound as for real weather (`mixin/client/LevelWeatherMixin`), while the server's stays as it is.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class LocalWeather {
    public interface Source {
        float rain(ClientLevel level, Vec3 at);

        default float thunder(ClientLevel level, Vec3 at) {
            return this.rain(level, at);
        }
    }

    // As fast as it may change a tick: a second from clear to the full storm.
    private static final float STEP = 0.05F;
    private static final List<Source> SOURCES = new ArrayList<>();
    private static float rain;
    private static float rainBefore;
    private static float thunder;
    private static float thunderBefore;

    private LocalWeather() {
    }

    public static void add(Source source) {
        SOURCES.add(source);
    }

    // The world's own rain (`own`), raised to the local weather where there is any.
    public static float rain(Level level, float partialTick, float own) {
        return level == Minecraft.getInstance().level ? Math.max(own, Mth.lerp(partialTick, rainBefore, rain)) : own;
    }

    public static float thunder(Level level, float partialTick, float own) {
        return level == Minecraft.getInstance().level ? Math.max(own, Mth.lerp(partialTick, thunderBefore, thunder))
                : own;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (minecraft.isPaused()) {
            return;
        }
        rainBefore = rain;
        thunderBefore = thunder;
        float wantRain = 0.0F;
        float wantThunder = 0.0F;
        if (level != null && minecraft.player != null) {
            Vec3 at = minecraft.gameRenderer.getMainCamera().isInitialized()
                    ? minecraft.gameRenderer.getMainCamera().getPosition() : minecraft.player.getEyePosition();
            for (Source source : SOURCES) {
                wantRain = Math.max(wantRain, Mth.clamp(source.rain(level, at), 0.0F, 1.0F));
                wantThunder = Math.max(wantThunder, Mth.clamp(source.thunder(level, at), 0.0F, 1.0F));
            }
        }
        rain = Mth.approach(rain, wantRain, STEP);
        thunder = Mth.approach(thunder, wantThunder, STEP);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        rain = rainBefore = thunder = thunderBefore = 0.0F;
    }
}
