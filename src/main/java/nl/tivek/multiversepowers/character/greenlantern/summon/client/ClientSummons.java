package nl.tivek.multiversepowers.character.greenlantern.summon.client;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheel;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.summon.SummonMarkerPayload;
import nl.tivek.multiversepowers.character.greenlantern.summon.SummonPayload;
import nl.tivek.multiversepowers.character.greenlantern.summon.Summons;
import nl.tivek.multiversepowers.engine.math.Colors;
import org.lwjgl.opengl.GL11;

// Every summon a player sees: when it came (it flares white as it forms, `HardLightLayer`) and whose it is; and for the
// player's own told to stay, the marker where they stay, the square they keep to drawn faintly round it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientSummons {
    // Ticks it takes to form.
    private static final float FORM = 16.0F;
    // When each summon came, in its own ticks on this game: entity id to the tick count it would have had at 0.
    private static final Int2IntOpenHashMap BORN = new Int2IntOpenHashMap();
    // Each summon's owner, by entity id: -1 for one out of this game's world.
    private static final Int2IntOpenHashMap OWNER = new Int2IntOpenHashMap();
    @Nullable
    private static Vec3 marker;
    private static long markedAt;

    private ClientSummons() {
    }

    public static void told(SummonPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity entity = minecraft.level.getEntity(payload.entity());
        BORN.put(payload.entity(), entity == null ? -payload.age() : entity.tickCount - payload.age());
        OWNER.put(payload.entity(), payload.owner());
    }

    public static void marked(SummonMarkerPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        marker = payload.stay() ? new Vec3(payload.x(), payload.y(), payload.z()) : null;
        markedAt = minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    // 1 as a summon comes, down to 0 once it has formed; 0 for anything else.
    static float forming(Entity entity, float partialTick) {
        if (!BORN.containsKey(entity.getId())) {
            return 0.0F;
        }
        return 1.0F - Mth.clamp((entity.tickCount - BORN.get(entity.getId()) + partialTick) / FORM, 0.0F, 1.0F);
    }

    // Whether `entity` is a summon of this game's player.
    public static boolean mine(@Nullable Entity entity) {
        Minecraft minecraft = Minecraft.getInstance();
        return entity != null && minecraft.player != null && OWNER.containsKey(entity.getId())
                && OWNER.get(entity.getId()) == minecraft.player.getId();
    }

    // Whether this game's player has a summon out where they can see it.
    public static boolean anyMine() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return false;
        }
        for (var entry : OWNER.int2IntEntrySet()) {
            if (entry.getIntValue() == minecraft.player.getId() && minecraft.level.getEntity(entry.getIntKey()) != null) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || marker == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || !anyMine()) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (float) (level.getGameTime() % 24000L) + partialTick;
        double age = level.getGameTime() - markedAt + partialTick;
        double grown = Mth.clamp(age / 8.0, 0.0, 1.0);
        Camera camera = event.getCamera();
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(), time,
                event.getFrustum());
        Vec3 ground = marker.add(0.0, 0.05, 0.0);
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        double pulse = 0.75 + 0.25 * Math.sin(time * 0.2);
        painter.circle(ground, east, south, 0.55 * grown, 0.06, 0.4, Colors.alpha(0.9 * pulse),
                Colors.alpha(0.45 * pulse));
        Vec3 top = ground.add(0.0, 1.1 * grown, 0.0);
        painter.edge(ground, top, 0.05, pulse);
        painter.flare(top, 0.22, pulse);
        double side = Summons.STAY * grown;
        Vec3[] corners = { ground.add(-side, 0.0, -side), ground.add(side, 0.0, -side), ground.add(side, 0.0, side),
                ground.add(-side, 0.0, side) };
        for (int k = 0; k < 4; k++) {
            painter.edge(corners[k], corners[(k + 1) % 4], 0.03, 0.35);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    // While the key is held on a summon to send it away: a ring round the crosshair filling up.
    @SubscribeEvent
    public static void onGui(RenderGuiEvent.Post event) {
        float held = ConstructWheel.dismissing();
        if (held <= 0.0F) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        graphics.flush();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        int x = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() / 2;
        int dots = 24;
        for (int k = 0; k < dots; k++) {
            double turn = Math.PI * 2.0 * k / dots - Math.PI * 0.5;
            int dx = x + (int) Math.round(Math.cos(turn) * 11.0);
            int dy = y + (int) Math.round(Math.sin(turn) * 11.0);
            boolean lit = k < held * dots;
            graphics.fill(dx - 1, dy - 1, dx + 1, dy + 1, lit ? 0xFF8CFFA6 : 0x60FFFFFF);
        }
        graphics.flush();
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            BORN.remove(event.getEntity().getId());
            OWNER.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BORN.clear();
        OWNER.clear();
        marker = null;
    }
}
