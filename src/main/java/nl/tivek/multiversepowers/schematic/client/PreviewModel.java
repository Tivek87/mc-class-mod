package nl.tivek.multiversepowers.schematic.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import nl.tivek.multiversepowers.schematic.Schematic;
import org.joml.Matrix4f;

// The schematic small in the menu, slowly turning: its blocks gathered in cells (at most 48 a side), each cell coloured
// as a map shows its highest block, only the sides that face out, made away from the game's thread.
final class PreviewModel {
    private static final int MOST = 48;
    // A block with no map colour of its own (glass, a torch).
    private static final int CLEAR = 0xB8D4E0;

    private record Mesh(@Nullable MeshData data, @Nullable ByteBufferBuilder memory, int cx, int cy, int cz) {
        void close() {
            if (this.data != null) {
                this.data.close();
            }
            if (this.memory != null) {
                this.memory.close();
            }
        }
    }

    @Nullable
    private static VertexBuffer buffer;
    private static int cx;
    private static int cy;
    private static int cz;
    private static int generation;
    private static boolean building;

    private PreviewModel() {
    }

    static void build(Schematic schematic) {
        release();
        int ours = ++generation;
        building = true;
        CompletableFuture.supplyAsync(() -> mesh(schematic), Util.backgroundExecutor()).whenComplete(
                (mesh, error) -> Minecraft.getInstance().execute(() -> {
                    if (mesh == null) {
                        if (ours == generation) {
                            building = false;
                        }
                        return;
                    }
                    if (ours != generation) {
                        mesh.close();
                        return;
                    }
                    building = false;
                    cx = mesh.cx();
                    cy = mesh.cy();
                    cz = mesh.cz();
                    if (mesh.data() == null) {
                        mesh.close();
                        return;
                    }
                    VertexBuffer made = new VertexBuffer(VertexBuffer.Usage.STATIC);
                    made.bind();
                    made.upload(mesh.data());
                    VertexBuffer.unbind();
                    if (mesh.memory() != null) {
                        mesh.memory().close();
                    }
                    buffer = made;
                }));
    }

    static boolean building() {
        return building;
    }

    static void release() {
        generation++;
        building = false;
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    private static Mesh mesh(Schematic schematic) {
        int step = Math.max(1, (Math.max(schematic.sizeX, Math.max(schematic.sizeY, schematic.sizeZ)) + MOST - 1)
                / MOST);
        int sx = (schematic.sizeX + step - 1) / step;
        int sy = (schematic.sizeY + step - 1) / step;
        int sz = (schematic.sizeZ + step - 1) / step;
        int[] colors = new int[sx * sy * sz];
        int[] tops = new int[colors.length];
        int[] paletteColors = new int[schematic.palette.size()];
        for (int k = 0; k < paletteColors.length; k++) {
            BlockState state = schematic.palette.get(k);
            if (state.isAir()) {
                paletteColors[k] = -1;
                continue;
            }
            MapColor map = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
            paletteColors[k] = map == MapColor.NONE ? CLEAR : map.col;
        }
        for (int y = 0; y < schematic.sizeY; y++) {
            for (int z = 0; z < schematic.sizeZ; z++) {
                for (int x = 0; x < schematic.sizeX; x++) {
                    short cell = schematic.cells[schematic.index(x, y, z)];
                    if (cell == Schematic.VOID || paletteColors[cell] < 0) {
                        continue;
                    }
                    int at = (y / step * sz + z / step) * sx + x / step;
                    if (colors[at] == 0 || y >= tops[at]) {
                        colors[at] = 0xFF000000 | paletteColors[cell];
                        tops[at] = y;
                    }
                }
            }
        }
        ByteBufferBuilder memory = new ByteBufferBuilder(1 << 16);
        BufferBuilder out = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int color = colors[(y * sz + z) * sx + x];
                    if (color == 0) {
                        continue;
                    }
                    for (Direction side : Direction.values()) {
                        int nx = x + side.getStepX();
                        int ny = y + side.getStepY();
                        int nz = z + side.getStepZ();
                        boolean open = nx < 0 || ny < 0 || nz < 0 || nx >= sx || ny >= sy || nz >= sz
                                || colors[(ny * sz + nz) * sx + nx] == 0;
                        if (open) {
                            face(out, x, y, z, side, color);
                        }
                    }
                }
            }
        }
        MeshData data = out.build();
        if (data == null) {
            memory.close();
            return new Mesh(null, null, sx, sy, sz);
        }
        return new Mesh(data, memory, sx, sy, sz);
    }

    private static void face(BufferBuilder out, int x, int y, int z, Direction side, int color) {
        float shade = switch (side) {
            case UP -> 1.0F;
            case DOWN -> 0.45F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.62F;
        };
        int r = Math.round((color >> 16 & 0xFF) * shade);
        int g = Math.round((color >> 8 & 0xFF) * shade);
        int b = Math.round((color & 0xFF) * shade);
        int shaded = 0xFF000000 | r << 16 | g << 8 | b;
        float x0 = x;
        float y0 = y;
        float z0 = z;
        float x1 = x + 1;
        float y1 = y + 1;
        float z1 = z + 1;
        float[][] corners = switch (side) {
            case DOWN -> new float[][] { { x0, y0, z1 }, { x0, y0, z0 }, { x1, y0, z0 }, { x1, y0, z1 } };
            case UP -> new float[][] { { x0, y1, z0 }, { x0, y1, z1 }, { x1, y1, z1 }, { x1, y1, z0 } };
            case NORTH -> new float[][] { { x1, y1, z0 }, { x1, y0, z0 }, { x0, y0, z0 }, { x0, y1, z0 } };
            case SOUTH -> new float[][] { { x0, y1, z1 }, { x0, y0, z1 }, { x1, y0, z1 }, { x1, y1, z1 } };
            case WEST -> new float[][] { { x0, y1, z0 }, { x0, y0, z0 }, { x0, y0, z1 }, { x0, y1, z1 } };
            case EAST -> new float[][] { { x1, y1, z1 }, { x1, y0, z1 }, { x1, y0, z0 }, { x1, y1, z0 } };
        };
        for (float[] corner : corners) {
            out.addVertex(corner[0], corner[1], corner[2]).setColor(shaded);
        }
    }

    // Draws it in a box of the screen, turned by `turn` (radians) and looked at a little from above.
    static void draw(GuiGraphics graphics, int x, int y, int width, int height, float turn) {
        VertexBuffer model = buffer;
        ShaderInstance shader = GameRenderer.getPositionColorShader();
        if (model == null || shader == null) {
            return;
        }
        graphics.flush();
        float span = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float scale = Math.min(width, height) * 0.9F / Math.max(1.0F, span);
        Matrix4f matrix = new Matrix4f(graphics.pose().last().pose()).translate(x + width / 2.0F, y + height / 2.0F,
                150.0F).scale(scale, -scale, scale).rotateX((float) Math.toRadians(24.0)).rotateY(turn)
                .translate(-cx / 2.0F, -cy / 2.0F, -cz / 2.0F);
        Matrix4f view = new Matrix4f(RenderSystem.getModelViewMatrix()).mul(matrix);
        graphics.enableScissor(x, y, x + width, y + height);
        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        model.bind();
        model.drawWithShader(view, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();
        RenderSystem.enableCull();
        graphics.disableScissor();
    }
}
