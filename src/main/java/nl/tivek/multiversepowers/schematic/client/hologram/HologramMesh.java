package nl.tivek.multiversepowers.schematic.client.hologram;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import nl.tivek.multiversepowers.schematic.Schematic;

// One 16-block cube of a schematic drawn as the game draws its blocks (models, fluids), fully lit, into a mesh, away
// from the game's thread. A block the game draws as a moving model (a chest, a bed, a sign) stands in as its shape
// in its own texture. A filter (a blueprint's) leaves out what is built already, tints a wrong block red and one
// turned the wrong way orange, each a little larger than the block in its place, and marks what is in the way.
public final class HologramMesh {
    static final int SIZE = 16;
    private static final int LIGHT = 0xF000F0;
    public static final int HIDDEN = 0;
    public static final int PLAIN = 1;
    public static final int WRONG = 2;
    public static final int TURNED = 3;
    public static final int IN_THE_WAY = 4;
    private static final int RED = 0xFFFF5A4E;
    private static final int ORANGE = 0xFFFFB347;
    private static final float GROW = 1.04F;

    // How the schematic's cell at `index` is drawn: one of HIDDEN to IN_THE_WAY.
    @FunctionalInterface
    public interface Filter {
        int look(int index);
    }

    record Built(long key, boolean fresh, @Nullable MeshData mesh, @Nullable ByteBufferBuilder memory) {
        void close() {
            if (this.mesh != null) {
                this.mesh.close();
            }
            if (this.memory != null) {
                this.memory.close();
            }
        }
    }

    private HologramMesh() {
    }

    static long key(int x, int y, int z) {
        return BlockPos.asLong(x, y, z);
    }

    // Whether the cube holds any block but air.
    static boolean filled(Schematic schematic, int x0, int y0, int z0) {
        for (int y = y0; y < Math.min(y0 + SIZE, schematic.sizeY); y++) {
            for (int z = z0; z < Math.min(z0 + SIZE, schematic.sizeZ); z++) {
                for (int x = x0; x < Math.min(x0 + SIZE, schematic.sizeX); x++) {
                    BlockState state = schematic.state(schematic.index(x, y, z));
                    if (state != null && !state.isAir()) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    static Built build(long key, boolean fresh, Schematic schematic, SchematicView view, @Nullable Filter filter) {
        int x0 = BlockPos.getX(key);
        int y0 = BlockPos.getY(key);
        int z0 = BlockPos.getZ(key);
        ByteBufferBuilder memory = new ByteBufferBuilder(1 << 16);
        BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        PoseStack pose = new PoseStack();
        RandomSource random = RandomSource.create();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        TintedConsumer red = new TintedConsumer(buffer, RED);
        TintedConsumer orange = new TintedConsumer(buffer, ORANGE);
        TextureAtlasSprite blank = blocks.getBlockModelShaper().getParticleIcon(Blocks.WHITE_CONCRETE
                .defaultBlockState());
        for (int y = y0; y < Math.min(y0 + SIZE, schematic.sizeY); y++) {
            for (int z = z0; z < Math.min(z0 + SIZE, schematic.sizeZ); z++) {
                for (int x = x0; x < Math.min(x0 + SIZE, schematic.sizeX); x++) {
                    int index = schematic.index(x, y, z);
                    int look = filter == null ? PLAIN : filter.look(index);
                    if (look == HIDDEN) {
                        continue;
                    }
                    if (look == IN_THE_WAY) {
                        AABB box = new AABB(x - x0, y - y0, z - z0, x - x0 + 1, y - y0 + 1, z - z0 + 1)
                                .inflate((GROW - 1.0F) * 0.5F);
                        for (Direction side : Direction.values()) {
                            face(buffer, blank, box, side, view.getShade(side, true), RED);
                        }
                        continue;
                    }
                    BlockState state = schematic.state(index);
                    if (state == null || state.isAir()) {
                        continue;
                    }
                    VertexConsumer out = look == WRONG ? red : look == TURNED ? orange : buffer;
                    at.set(x, y, z);
                    FluidState fluid = state.getFluidState();
                    if (!fluid.isEmpty()) {
                        blocks.renderLiquid(at, view, out, state, fluid);
                    }
                    RenderShape shape = state.getRenderShape();
                    if (shape == RenderShape.MODEL) {
                        pose.pushPose();
                        pose.translate(x - x0, y - y0, z - z0);
                        if (look != PLAIN) {
                            pose.translate(0.5F, 0.5F, 0.5F);
                            pose.scale(GROW, GROW, GROW);
                            pose.translate(-0.5F, -0.5F, -0.5F);
                        }
                        blocks.renderBatched(state, at, view, pose, out, look == PLAIN, random,
                                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
                        pose.popPose();
                    } else if (shape == RenderShape.ENTITYBLOCK_ANIMATED) {
                        standIn(buffer, blocks, view, state, at, x - x0, y - y0, z - z0, look == WRONG ? RED
                                : look == TURNED ? ORANGE : 0xFFFFFFFF);
                    }
                }
            }
        }
        MeshData mesh = buffer.build();
        if (mesh == null) {
            memory.close();
            return new Built(key, fresh, null, null);
        }
        return new Built(key, fresh, mesh, memory);
    }

    // Its shape's boxes, each side in the texture of its breaking bits.
    private static void standIn(BufferBuilder buffer, BlockRenderDispatcher blocks, SchematicView view, BlockState state,
            BlockPos pos, int x, int y, int z, int tint) {
        TextureAtlasSprite sprite = blocks.getBlockModelShaper().getParticleIcon(state);
        for (AABB box : state.getShape(view, pos).toAabbs()) {
            for (Direction side : Direction.values()) {
                face(buffer, sprite, box.move(x, y, z), side, view.getShade(side, true), tint);
            }
        }
    }

    private static void face(BufferBuilder buffer, TextureAtlasSprite sprite, AABB box, Direction side, float shade,
            int tint) {
        float x0 = (float) box.minX;
        float y0 = (float) box.minY;
        float z0 = (float) box.minZ;
        float x1 = (float) box.maxX;
        float y1 = (float) box.maxY;
        float z1 = (float) box.maxZ;
        float[][] corners = switch (side) {
            case DOWN -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
            case UP -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
            case NORTH -> new float[][] { { x1, y1, z0 }, { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 } };
            case SOUTH -> new float[][] { { x0, y1, z1 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 } };
            case WEST -> new float[][] { { x0, y1, z0 }, { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 } };
            case EAST -> new float[][] { { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 } };
        };
        float[][] uv = { { sprite.getU0(), sprite.getV0() }, { sprite.getU0(), sprite.getV1() },
                { sprite.getU1(), sprite.getV1() }, { sprite.getU1(), sprite.getV0() } };
        int color = 0xFF000000 | Math.round((tint >> 16 & 0xFF) * shade) << 16 | Math.round((tint >> 8 & 0xFF)
                * shade) << 8 | Math.round((tint & 0xFF) * shade);
        for (int k = 0; k < 4; k++) {
            buffer.addVertex(corners[k][0], corners[k][1], corners[k][2]).setColor(color).setUv(uv[k][0], uv[k][1])
                    .setLight(LIGHT).setNormal(side.getStepX(), side.getStepY(), side.getStepZ());
        }
    }
}
