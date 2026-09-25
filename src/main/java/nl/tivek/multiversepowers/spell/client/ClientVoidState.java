package nl.tivek.multiversepowers.spell.client;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientStandings;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientVoidState {
    private static final ResourceLocation SHADER =
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "shaders/post/void_world.json");
    // Same as the server's mark radius in VoidWalkSpell.
    private static final double MARK_RADIUS = 32.0;
    private static final int FADE_IN = 8;
    private static final int FADE_OUT = 15;

    private static int remaining;
    private static int age;
    @Nullable
    private static PostChain chain;
    private static boolean shaderFailed;
    private static final Set<Entity> MARKED = new HashSet<>();

    private ClientVoidState() {
    }

    public static void set(int ticks) {
        if (ticks <= 0) {
            end();
            return;
        }
        remaining = ticks;
        age = 0;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (remaining <= 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            end();
            return;
        }

        ensureShader(minecraft.gameRenderer);
        if (chain != null) {
            double in = Math.min(1.0, (age + 1.0) / FADE_IN);
            double out = Math.min(1.0, remaining / (double) FADE_OUT);
            chain.setUniform("Intensity", (float) Math.min(in, out));
        }
        markEnemies(player, level);
        if (age % 20 == 0) {
            level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_HEARTBEAT,
                    SoundSource.PLAYERS, 0.9F, 0.7F, false);
        }

        age++;
        remaining--;
        if (remaining <= 0) {
            end();
        }
    }

    private static void ensureShader(GameRenderer renderer) {
        if (shaderFailed || chain != null && renderer.currentEffect() == chain) {
            return;
        }
        renderer.loadEffect(SHADER);
        chain = renderer.currentEffect();
        // Once failed, stays failed: avoids retrying loadEffect every tick.
        shaderFailed = chain == null;
    }

    private static void markEnemies(LocalPlayer player, ClientLevel level) {
        Set<Entity> seen = new HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || !entity.isAlive() || entity.isSpectator()
                    || ClientStandings.of(entity) != Standing.HOSTILE || entity.distanceTo(player) > MARK_RADIUS) {
                continue;
            }
            if (!MARKED.contains(entity) && GlowFlag.isGlowing(entity)) {
                continue;
            }
            seen.add(entity);
            MARKED.add(entity);
            GlowFlag.setGlowing(entity, true);
        }
        Iterator<Entity> iterator = MARKED.iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (!seen.contains(entity)) {
                GlowFlag.setGlowing(entity, false);
                iterator.remove();
            }
        }
    }

    private static void end() {
        remaining = 0;
        age = 0;
        for (Entity entity : MARKED) {
            GlowFlag.setGlowing(entity, false);
        }
        MARKED.clear();
        GameRenderer renderer = Minecraft.getInstance().gameRenderer;
        if (chain != null && renderer.currentEffect() == chain) {
            renderer.shutdownEffect();
        }
        chain = null;
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        end();
    }
}
