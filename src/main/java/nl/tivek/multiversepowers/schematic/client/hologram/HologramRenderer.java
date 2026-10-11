package nl.tivek.multiversepowers.schematic.client.hologram;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;

// Draws the holograms: the one in hand (Hologram), and every blueprint set down in this world (Blueprint models,
// registered by their owner each frame). Each is a HologramModel; this takes in their meshes a few a frame in all.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HologramRenderer {
    private static final int UPLOADS_A_FRAME = 4;
    static final ResourceLocation LIGHT = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "schematic_light");
    // Placing as a blueprint, the hologram in hand is tinted as a plan.
    private static final float[] PLAN = { 0.55F, 0.85F, 1.0F };

    // What else is drawn each frame after the hologram in hand: the blueprints.
    public interface Others {
        void draw(RenderLevelStageEvent event, int room);
    }

    @Nullable
    private static HologramModel model;
    @Nullable
    private static Others others;
    private static boolean lightMade;

    private HologramRenderer() {
    }

    public static void others(Others drawn) {
        others = drawn;
    }

    static void load(Schematic shown) {
        release();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        int area = shown.sizeX * shown.sizeZ;
        model = new HologramModel(shown, level, index -> Hologram.layer >= 0 && index / area > Hologram.layer
                ? HologramMesh.HIDDEN : HologramMesh.PLAIN);
    }

    // Only the layers up to `layer` are drawn (-1: all).
    static void top(int layer) {
        if (model != null) {
            model.top(layer);
        }
    }

    static void release() {
        if (model != null) {
            model.release();
            model = null;
        }
    }

    // How much of it is drawn yet, 0 to 1.
    public static float progress() {
        HologramModel now = model;
        return now == null ? 1.0F : now.progress();
    }

    // Takes in at most `room` meshes the model has made; how many it took.
    public static int work(HologramModel drawn, int room) {
        return drawn.work(room);
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        int room = UPLOADS_A_FRAME;
        HologramModel now = model;
        if (now != null && Hologram.shown()) {
            room -= now.work(room);
            SchematicPlacement placement = Hologram.placement();
            float alpha = (float) ClientSettings.get(ClientSettings.SCHEMATIC_OPACITY);
            HologramReplaced.draw(event);
            if (Hologram.blueprint) {
                now.draw(event, placement, PLAN[0], PLAN[1], PLAN[2], alpha);
            } else {
                now.draw(event, placement, 1.0F, 1.0F, 1.0F, alpha);
            }
            if (ClientSettings.get(ClientSettings.SCHEMATIC_OUTLINE) != 0) {
                boolean pinned = Hologram.pinned;
                outline(event, placement, now.schematic(), pinned ? 1.0F : 0.45F, pinned ? 0.8F : 0.9F, pinned ? 0.3F
                        : 1.0F);
            }
        }
        Others drawn = others;
        if (drawn != null) {
            drawn.draw(event, Math.max(1, room));
        }
    }

    // A white light map: the hologram is as bright by night as by day.
    static void light() {
        if (lightMade) {
            return;
        }
        NativeImage image = new NativeImage(1, 1, false);
        image.setPixelRGBA(0, 0, 0xFFFFFFFF);
        Minecraft.getInstance().getTextureManager().register(LIGHT, new DynamicTexture(image));
        lightMade = true;
    }

    // The edges of a placed schematic's box, in (r, g, b).
    public static void outline(RenderLevelStageEvent event, SchematicPlacement placement, Schematic shown, float r,
            float g, float b) {
        BlockPos origin = placement.origin();
        box(event, new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + placement.sizeX(shown),
                origin.getY() + shown.sizeY, origin.getZ() + placement.sizeZ(shown)), r, g, b);
    }

    // The edges of a box in the world, in (r, g, b).
    public static void box(RenderLevelStageEvent event, AABB box, float r, float g, float b) {
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box.move(-camera.x, -camera.y,
                -camera.z), r, g, b, 0.9F);
        buffers.endBatch(RenderType.lines());
    }
}
