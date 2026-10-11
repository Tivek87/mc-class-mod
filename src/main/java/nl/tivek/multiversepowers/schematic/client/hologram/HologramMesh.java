package nl.tivek.multiversepowers.schematic.client.hologram;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import nl.tivek.multiversepowers.schematic.Schematic;

// One 16-block cube of a schematic drawn as the game draws its blocks (models, fluids), fully lit, into a mesh, away
// from the game's thread. A block the game draws as a moving model (a chest, a bed, a sign) stands in as its shape
// in its own texture.
final class HologramMesh {
    static final int SIZE = 16;
    private static final int LIGHT = 0xF000F0;

    record Built(int generation, long key, @Nullable MeshData mesh, @Nullable ByteBufferBuilder memory) {
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

    static Built build(int generation, long key, Schematic schematic, SchematicView view) {
        int x0 = BlockPos.getX(key);
        int y0 = BlockPos.getY(key);
        int z0 = BlockPos.getZ(key);
        ByteBufferBuilder memory = new ByteBufferBuilder(1 << 16);
        BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        PoseStack pose = new PoseStack();
        RandomSource random = RandomSource.create();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int y = y0; y < Math.min(y0 + SIZE, schematic.sizeY); y++) {
            for (int z = z0; z < Math.min(z0 + SIZE, schematic.sizeZ); z++) {
                for (int x = x0; x < Math.min(x0 + SIZE, schematic.sizeX); x++) {
                    BlockState state = schematic.state(schematic.index(x, y, z));
                    if (state == null || state.isAir()) {
                        continue;
                    }
                    at.set(x, y, z);
                    FluidState fluid = state.getFluidState();
                    if (!fluid.isEmpty()) {
                        blocks.renderLiquid(at, view, buffer, state, fluid);
                    }
                    RenderShape shape = state.getRenderShape();
                    if (shape == RenderShape.MODEL) {
                        pose.pushPose();
                        pose.translate(x - x0, y - y0, z - z0);
                        blocks.renderBatched(state, at, view, pose, buffer, true, random,
                                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
                        pose.popPose();
                    } else if (shape == RenderShape.ENTITYBLOCK_ANIMATED) {
                        standIn(buffer, blocks, view, state, at, x - x0, y - y0, z - z0);
                    }
                }
            }
        }
        MeshData mesh = buffer.build();
        if (mesh == null) {
            memory.close();
            return new Built(generation, key, null, null);
        }
        return new Built(generation, key, mesh, memory);
    }

    // Its shape's boxes, each side in the texture of its breaking bits.
    private static void standIn(BufferBuilder buffer, BlockRenderDispatcher blocks, SchematicView view, BlockState state,
            BlockPos pos, int x, int y, int z) {
        TextureAtlasSprite sprite = blocks.getBlockModelShaper().getParticleIcon(state);
        for (AABB box : state.getShape(view, pos).toAabbs()) {
            for (Direction side : Direction.values()) {
                face(buffer, sprite, box.move(x, y, z), side, view.getShade(side, true));
            }
        }
    }

    private static void face(BufferBuilder buffer, TextureAtlasSprite sprite, AABB box, Direction side, float shade) {
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
        int grey = Math.round(shade * 255.0F);
        int color = 0xFF000000 | grey << 16 | grey << 8 | grey;
        for (int k = 0; k < 4; k++) {
            buffer.addVertex(corners[k][0], corners[k][1], corners[k][2]).setColor(color).setUv(uv[k][0], uv[k][1])
                    .setLight(LIGHT).setNormal(side.getStepX(), side.getStepY(), side.getStepZ());
        }
    }
}
