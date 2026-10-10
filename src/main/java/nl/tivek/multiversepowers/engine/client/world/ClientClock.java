package nl.tivek.multiversepowers.engine.client.world;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;

// Ticks this game has run in a world. Unlike the level's game time, which the server sets back whenever it runs
// behind, it never steps back, so what is timed from it never jumps or ends early.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientClock {
    private static long ticks;

    private ClientClock() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && !minecraft.isPaused()) {
            ticks++;
        }
    }

    public static double now(float partialTick) {
        return ticks + partialTick;
    }
}
