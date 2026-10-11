package nl.tivek.multiversepowers.schematic.client.hologram;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Mirror;
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
import org.joml.Matrix4f;

// Draws the hologram: the schematic in 16-block cubes, each made into a mesh away from the game's thread (two at a
// time) and handed to the graphics card a few a frame, so even a huge one never stalls the game; it simply fills in.
// Each frame the cubes in sight are drawn twice, first only into the depth buffer, then see-through on top, so only
// the faces nearest the eye show, as a ghost of the real thing. Turning and mirroring only move the whole, never
// remake it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HologramRenderer {
    private static final int BUILDING_AT_ONCE = 2;
    private static final int UPLOADS_A_FRAME = 4;
    private static final ResourceLocation LIGHT = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "schematic_light");

    private record Piece(VertexBuffer buffer, int x, int y, int z) {
    }

    @Nullable
    private static Schematic schematic;
    @Nullable
    private static SchematicView view;
    private static int generation;
    private static final Long2ObjectMap<Piece> PIECES = new Long2ObjectOpenHashMap<>();
    private static final LongArrayList QUEUE = new LongArrayList();
    private static int next;
    private static int building;
    private static int built;
    private static final ConcurrentLinkedQueue<HologramMesh.Built> DONE = new ConcurrentLinkedQueue<>();
    private static boolean lightMade;

    private HologramRenderer() {
    }

    static void load(Schematic shown) {
        release();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        schematic = shown;
        view = new SchematicView(shown, biome(level), level.getLightEngine());
        for (int y = 0; y < shown.sizeY; y += HologramMesh.SIZE) {
            for (int z = 0; z < shown.sizeZ; z += HologramMesh.SIZE) {
                for (int x = 0; x < shown.sizeX; x += HologramMesh.SIZE) {
                    if (HologramMesh.filled(shown, x, y, z)) {
                        QUEUE.add(HologramMesh.key(x, y, z));
                    }
                }
            }
        }
    }

    private static Biome biome(ClientLevel level) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(Biomes.PLAINS)
                .map(holder -> holder.value()).orElseGet(() -> level.getBiome(BlockPos.ZERO).value());
    }

    static void release() {
        generation++;
        for (Piece piece : PIECES.values()) {
            piece.buffer().close();
        }
        PIECES.clear();
        QUEUE.clear();
        next = 0;
        building = 0;
        built = 0;
        schematic = null;
        view = null;
        HologramMesh.Built done;
        while ((done = DONE.poll()) != null) {
            done.close();
        }
    }

    // How much of it is drawn yet, 0 to 1.
    public static float progress() {
        return QUEUE.isEmpty() ? 1.0F : built / (float) QUEUE.size();
    }

    @SubscribeEvent
    static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Schematic shown = schematic;
        if (shown == null || !Hologram.shown()) {
            return;
        }
        upload();
        start(shown);
        SchematicPlacement placement = Hologram.placement();
        Vec3 camera = event.getCamera().getPosition();
        BlockPos origin = placement.origin();
        Matrix4f base = new Matrix4f(event.getModelViewMatrix()).translate((float) (origin.getX() - camera.x),
                (float) (origin.getY() - camera.y), (float) (origin.getZ() - camera.z));
        turn(base, placement, shown);
        draw(event, shown, placement, base);
        if (ClientSettings.get(ClientSettings.SCHEMATIC_OUTLINE) != 0) {
            outline(event, placement, shown, camera);
        }
    }

    private static void upload() {
        int uploaded = 0;
        HologramMesh.Built done;
        while (uploaded < UPLOADS_A_FRAME && (done = DONE.poll()) != null) {
            if (done.generation() != generation) {
                done.close();
                continue;
            }
            building--;
            built++;
            if (done.mesh() == null) {
                done.close();
                continue;
            }
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(done.mesh());
            VertexBuffer.unbind();
            if (done.memory() != null) {
                done.memory().close();
            }
            long key = done.key();
            PIECES.put(key, new Piece(buffer, BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key)));
            uploaded++;
        }
    }

    private static void start(Schematic shown) {
        SchematicView own = view;
        while (building < BUILDING_AT_ONCE && next < QUEUE.size() && own != null) {
            long key = QUEUE.getLong(next++);
            int ours = generation;
            building++;
            Util.backgroundExecutor().execute(() -> {
                HologramMesh.Built done;
                try {
                    done = HologramMesh.build(ours, key, shown, own);
                } catch (RuntimeException e) {
                    MultiversePowers.LOGGER.warn("Could not draw part of a schematic", e);
                    done = new HologramMesh.Built(ours, key, null, null);
                }
                DONE.add(done);
            });
        }
    }

    // From the schematic's own blocks to its turned, mirrored box: the mirror first, then the turn, as it is placed.
    private static void turn(Matrix4f matrix, SchematicPlacement placement, Schematic shown) {
        float sx = shown.sizeX;
        float sz = shown.sizeZ;
        switch (placement.rotation()) {
            case CLOCKWISE_90 -> matrix.translate(sz, 0.0F, 0.0F).rotateY((float) (-Math.PI / 2.0));
            case CLOCKWISE_180 -> matrix.translate(sx, 0.0F, sz).rotateY((float) Math.PI);
            case COUNTERCLOCKWISE_90 -> matrix.translate(0.0F, 0.0F, sx).rotateY((float) (Math.PI / 2.0));
            default -> {
            }
        }
        if (placement.mirror() == Mirror.LEFT_RIGHT) {
            matrix.translate(0.0F, 0.0F, sz).scale(1.0F, 1.0F, -1.0F);
        } else if (placement.mirror() == Mirror.FRONT_BACK) {
            matrix.translate(sx, 0.0F, 0.0F).scale(-1.0F, 1.0F, 1.0F);
        }
    }

    private static void draw(RenderLevelStageEvent event, Schematic shown, SchematicPlacement placement,
            Matrix4f base) {
        if (PIECES.isEmpty()) {
            return;
        }
        ShaderInstance shader = GameRenderer.getRendertypeCutoutShader();
        if (shader == null) {
            return;
        }
        light();
        // The world's own cubes leave their last place here.
        if (shader.CHUNK_OFFSET != null) {
            shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
        }
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderTexture(2, LIGHT);
        RenderSystem.enableDepthTest();
        boolean flipped = placement.mirror() != Mirror.NONE;
        if (flipped) {
            RenderSystem.disableCull();
        }
        Matrix4f projection = event.getProjectionMatrix();
        List<Matrix4f> seen = new ArrayList<>();
        List<Piece> drawn = new ArrayList<>();
        for (Piece piece : PIECES.values()) {
            Vec3 a = placement.world(shown, new Vec3(piece.x(), piece.y(), piece.z()));
            Vec3 b = placement.world(shown, new Vec3(piece.x() + HologramMesh.SIZE, piece.y() + HologramMesh.SIZE,
                    piece.z() + HologramMesh.SIZE));
            if (!event.getFrustum().isVisible(new AABB(a, b))) {
                continue;
            }
            seen.add(new Matrix4f(base).translate(piece.x(), piece.y(), piece.z()));
            drawn.add(piece);
        }
        RenderSystem.colorMask(false, false, false, false);
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        for (int k = 0; k < drawn.size(); k++) {
            drawn.get(k).buffer().bind();
            drawn.get(k).buffer().drawWithShader(seen.get(k), projection, shader);
        }
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float) ClientSettings.get(ClientSettings.SCHEMATIC_OPACITY));
        for (int k = 0; k < drawn.size(); k++) {
            drawn.get(k).buffer().bind();
            drawn.get(k).buffer().drawWithShader(seen.get(k), projection, shader);
        }
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        if (flipped) {
            RenderSystem.enableCull();
        }
    }

    // A white light map: the hologram is as bright by night as by day.
    private static void light() {
        if (lightMade) {
            return;
        }
        NativeImage image = new NativeImage(1, 1, false);
        image.setPixelRGBA(0, 0, 0xFFFFFFFF);
        Minecraft.getInstance().getTextureManager().register(LIGHT, new DynamicTexture(image));
        lightMade = true;
    }

    private static void outline(RenderLevelStageEvent event, SchematicPlacement placement, Schematic shown,
            Vec3 camera) {
        BlockPos origin = placement.origin();
        AABB box = new AABB(origin.getX(), origin.getY(), origin.getZ(), origin.getX() + placement.sizeX(shown),
                origin.getY() + shown.sizeY, origin.getZ() + placement.sizeZ(shown)).move(-camera.x, -camera.y,
                        -camera.z);
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        PoseStack pose = event.getPoseStack();
        boolean pinned = Hologram.pinned;
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), box, pinned ? 1.0F : 0.45F,
                pinned ? 0.8F : 0.9F, pinned ? 0.3F : 1.0F, 0.9F);
        buffers.endBatch(RenderType.lines());
    }
}
