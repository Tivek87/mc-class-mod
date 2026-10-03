package nl.tivek.multiversepowers.update.client;

import com.mojang.logging.LogUtils;
import java.lang.reflect.Field;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ModMismatchDisconnectedScreen;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.update.VersionProbe;
import org.slf4j.Logger;

// When a server refuses you over the mod's version (VersionProbe), NeoForge's own screen lists the channels that failed
// and shows the two versions the wrong way round; this puts VersionMismatchScreen in its place, with the version the
// server runs. NeoForge keeps the reasons in a private field of its screen, so they are read by reflection.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class VersionMismatch {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MISMATCH = "neoforge.network.negotiation.failure.version.mismatch";

    private VersionMismatch() {
    }

    @SubscribeEvent
    static void onScreenOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof ModMismatchDisconnectedScreen refused)) {
            return;
        }
        Component reason = reasons(refused).get(VersionProbe.TYPE.id());
        String server = reason == null ? null : serverVersion(reason);
        if (server != null) {
            event.setNewScreen(new VersionMismatchScreen(server));
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<ResourceLocation, Component> reasons(ModMismatchDisconnectedScreen screen) {
        try {
            Field field = ModMismatchDisconnectedScreen.class.getDeclaredField("mismatchedChannelData");
            field.setAccessible(true);
            return (Map<ResourceLocation, Component>) field.get(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            LOGGER.warn("Could not read why the server refused the connection", e);
            return Map.of();
        }
    }

    // The first argument of the version mismatch, wherever NeoForge wrapped it: the server's own version.
    @Nullable
    static String serverVersion(Component reason) {
        if (reason.getContents() instanceof TranslatableContents translatable) {
            Object[] args = translatable.getArgs();
            if (translatable.getKey().equals(MISMATCH)) {
                return args.length == 0 ? null : args[0] instanceof Component text ? text.getString()
                        : String.valueOf(args[0]);
            }
            for (Object arg : args) {
                String found = arg instanceof Component inner ? serverVersion(inner) : null;
                if (found != null) {
                    return found;
                }
            }
        }
        for (Component sibling : reason.getSiblings()) {
            String found = serverVersion(sibling);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
