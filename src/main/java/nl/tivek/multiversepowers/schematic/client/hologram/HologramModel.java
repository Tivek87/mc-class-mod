package nl.tivek.multiversepowers.schematic.client.hologram;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongLinkedOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.SchematicPlacement;
import org.joml.Matrix4f;

// One schematic drawn in the world: its 16-block cubes, each made into a mesh away from the game's thread (two at a
// time over every model) and handed to the graphics card a few a frame, so even a huge one never stalls the game; it
// simply fills in. A cube can be made again (a blueprint's block placed): the old mesh stays until the new is in.
// Each frame the cubes in sight are drawn twice, first only into the depth buffer, then see-through on top, so only the
// faces nearest the eye show, as a ghost of the real thing. Turning and mirroring only move the whole.
public final class HologramModel {
    private static final int BUILDING_AT_ONCE = 2;
    private static final AtomicInteger BUILDING = new AtomicInteger();

    private record Piece(VertexBuffer buffer, int x, int y, int z) {
    }

    private final Schematic schematic;
    private final SchematicView view;
    @Nullable
    private final HologramMesh.Filter filter;
    private final Long2ObjectMap<Piece> pieces = new Long2ObjectOpenHashMap<>();
    private final LongArrayList queue = new LongArrayList();
    private final LongLinkedOpenHashSet again = new LongLinkedOpenHashSet();
    private final LongOpenHashSet building = new LongOpenHashSet();
    private final ConcurrentLinkedQueue<HologramMesh.Built> done = new ConcurrentLinkedQueue<>();
    private int next;
    private int built;
    private volatile boolean released;

    // `filter` says how each cell is drawn (a blueprint's); none draws every block plainly.
    public HologramModel(Schematic schematic, ClientLevel level, @Nullable HologramMesh.Filter filter) {
        this.schematic = schematic;
        this.filter = filter;
        this.view = new SchematicView(schematic, biome(level), level.getLightEngine());
        for (int y = 0; y < schematic.sizeY; y += HologramMesh.SIZE) {
            for (int z = 0; z < schematic.sizeZ; z += HologramMesh.SIZE) {
                for (int x = 0; x < schematic.sizeX; x += HologramMesh.SIZE) {
                    if (HologramMesh.filled(schematic, x, y, z)) {
                        this.queue.add(HologramMesh.key(x, y, z));
                    }
                }
            }
        }
    }

    private static Biome biome(ClientLevel level) {
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(Biomes.PLAINS)
                .map(holder -> holder.value()).orElseGet(() -> level.getBiome(BlockPos.ZERO).value());
    }

    public Schematic schematic() {
        return this.schematic;
    }

    // The cube holding the schematic's own cell (x, y, z) is made again.
    public void refresh(int x, int y, int z) {
        this.again.add(HologramMesh.key(x - Math.floorMod(x, HologramMesh.SIZE), y - Math.floorMod(y,
                HologramMesh.SIZE), z - Math.floorMod(z, HologramMesh.SIZE)));
    }

    // Only the layers up to `y` are drawn (the filter hides the rest), so the faces under the top one show; the
    // layers between the old top and the new are made again. Below 0 every layer is drawn.
    public void top(int y) {
        int now = y < 0 ? Integer.MAX_VALUE : y;
        int was = this.view.top;
        if (now == was) {
            return;
        }
        this.view.top = now;
        int last = this.schematic.sizeY - 1;
        this.refreshLayers(Math.min(Math.min(now, was), last), Math.min(Math.max(now, was), last));
    }

    // Every cube holding a cell of the layers `from` to `to` (y, of the schematic's own) is made again.
    public void refreshLayers(int from, int to) {
        int top = Math.min(to, this.schematic.sizeY - 1);
        for (int y = Math.max(0, from) - Math.floorMod(Math.max(0, from), HologramMesh.SIZE); y <= top;
                y += HologramMesh.SIZE) {
            for (int z = 0; z < this.schematic.sizeZ; z += HologramMesh.SIZE) {
                for (int x = 0; x < this.schematic.sizeX; x += HologramMesh.SIZE) {
                    this.refresh(x, y, z);
                }
            }
        }
    }

    // How much of it is drawn yet, 0 to 1.
    public float progress() {
        return this.queue.isEmpty() ? 1.0F : this.built / (float) this.queue.size();
    }

    public void release() {
        this.released = true;
        for (Piece piece : this.pieces.values()) {
            piece.buffer().close();
        }
        this.pieces.clear();
        this.queue.clear();
        this.again.clear();
        HologramMesh.Built finished;
        while ((finished = this.done.poll()) != null) {
            finished.close();
        }
    }

    // Takes in the meshes made, at most `room` of them, and sets more to be made; how many it took.
    int work(int room) {
        int uploaded = 0;
        HologramMesh.Built finished;
        while (uploaded < room && (finished = this.done.poll()) != null) {
            long key = finished.key();
            this.building.remove(key);
            if (finished.fresh()) {
                this.built++;
            }
            Piece old = finished.mesh() == null ? this.pieces.remove(key) : this.pieces.get(key);
            if (finished.mesh() == null) {
                if (old != null) {
                    old.buffer().close();
                }
                finished.close();
                continue;
            }
            VertexBuffer buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(finished.mesh());
            VertexBuffer.unbind();
            if (finished.memory() != null) {
                finished.memory().close();
            }
            if (old != null) {
                old.buffer().close();
            }
            this.pieces.put(key, new Piece(buffer, BlockPos.getX(key), BlockPos.getY(key), BlockPos.getZ(key)));
            uploaded++;
        }
        while (BUILDING.get() < BUILDING_AT_ONCE) {
            long key;
            boolean fresh;
            if (this.next < this.queue.size()) {
                key = this.queue.getLong(this.next++);
                fresh = true;
            } else if (!this.again.isEmpty() && this.firstFree() != Long.MIN_VALUE) {
                key = this.firstFree();
                this.again.remove(key);
                fresh = false;
            } else {
                break;
            }
            this.build(key, fresh);
        }
        return uploaded;
    }

    // The first cube waiting to be made again that is not being made now.
    private long firstFree() {
        for (long key : this.again) {
            if (!this.building.contains(key)) {
                return key;
            }
        }
        return Long.MIN_VALUE;
    }

    private void build(long key, boolean fresh) {
        this.building.add(key);
        BUILDING.incrementAndGet();
        Schematic shown = this.schematic;
        SchematicView own = this.view;
        HologramMesh.Filter look = this.filter;
        Util.backgroundExecutor().execute(() -> {
            HologramMesh.Built made;
            try {
                made = this.released ? new HologramMesh.Built(key, fresh, null, null)
                        : HologramMesh.build(key, fresh, shown, own, look);
            } catch (RuntimeException e) {
                MultiversePowers.LOGGER.warn("Could not draw part of a schematic", e);
                made = new HologramMesh.Built(key, fresh, null, null);
            } finally {
                BUILDING.decrementAndGet();
            }
            if (this.released) {
                made.close();
                return;
            }
            this.done.add(made);
            // Released between the check and the add: nothing will take it in, so it is closed here.
            if (this.released) {
                HologramMesh.Built left;
                while ((left = this.done.poll()) != null) {
                    left.close();
                }
            }
        });
    }

    // Draws it where `placement` sets it, coloured by (r, g, b) and as solid as `alpha`.
    public void draw(RenderLevelStageEvent event, SchematicPlacement placement, float r, float g, float b,
            float alpha) {
        if (this.pieces.isEmpty()) {
            return;
        }
        ShaderInstance shader = GameRenderer.getRendertypeCutoutShader();
        if (shader == null) {
            return;
        }
        Vec3 camera = event.getCamera().getPosition();
        BlockPos origin = placement.origin();
        Matrix4f base = new Matrix4f(event.getModelViewMatrix()).translate((float) (origin.getX() - camera.x),
                (float) (origin.getY() - camera.y), (float) (origin.getZ() - camera.z));
        turn(base, placement, this.schematic);
        List<Matrix4f> seen = new ArrayList<>();
        List<Piece> drawn = new ArrayList<>();
        for (Piece piece : this.pieces.values()) {
            Vec3 a = placement.world(this.schematic, new Vec3(piece.x(), piece.y(), piece.z()));
            Vec3 c = placement.world(this.schematic, new Vec3(piece.x() + HologramMesh.SIZE, piece.y()
                    + HologramMesh.SIZE, piece.z() + HologramMesh.SIZE));
            if (!event.getFrustum().isVisible(new AABB(a, c))) {
                continue;
            }
            seen.add(new Matrix4f(base).translate(piece.x(), piece.y(), piece.z()));
            drawn.add(piece);
        }
        if (drawn.isEmpty()) {
            return;
        }
        HologramRenderer.light();
        // The world's own cubes leave their last place here.
        if (shader.CHUNK_OFFSET != null) {
            shader.CHUNK_OFFSET.set(0.0F, 0.0F, 0.0F);
        }
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderTexture(2, HologramRenderer.LIGHT);
        RenderSystem.enableDepthTest();
        boolean flipped = placement.mirror() != Mirror.NONE;
        if (flipped) {
            RenderSystem.disableCull();
        }
        Matrix4f projection = event.getProjectionMatrix();
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
        RenderSystem.setShaderColor(r, g, b, alpha);
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
}
