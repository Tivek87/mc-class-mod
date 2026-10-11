package nl.tivek.multiversepowers.schematic.client.menu;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.MapColor;
import nl.tivek.multiversepowers.schematic.Schematic;
import org.joml.Matrix4f;

// A schematic small in a menu, slowly turning: its blocks gathered in cells (at most 128 a side), each side of a cell
// coloured as its block's own texture looks from that way (grass green on top, earth on its sides; tinted as in a
// plains biome), darker in the corners where cells meet, only the sides that face out, made away from the game's
// thread. One for the library, one for the blueprints.
public final class PreviewModel {
    private static final int MOST = 128;
    // A block with no colour of its own to be found (an invisible one).
    private static final int CLEAR = 0xB8D4E0;
    private static final int WATER = 0x3F76E4;
    private static final int LAVA = 0xD96415;
    // The average colour of each texture, worked out once.
    // By the sprite's own contents, which a resource reload replaces, so colours never outlive their textures.
    private static final Map<SpriteContents, Integer> SPRITES = Collections.synchronizedMap(new WeakHashMap<>());
    public static final PreviewModel LIBRARY = new PreviewModel();
    public static final PreviewModel BLUEPRINT = new PreviewModel();

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
    private VertexBuffer buffer;
    @Nullable
    private Schematic shown;
    private int cx;
    private int cy;
    private int cz;
    private int generation;
    private boolean building;

    private PreviewModel() {
    }

    public void build(Schematic schematic) {
        if (schematic == this.shown) {
            return;
        }
        this.release();
        this.shown = schematic;
        int ours = ++this.generation;
        this.building = true;
        CompletableFuture.supplyAsync(() -> mesh(schematic), Util.backgroundExecutor()).whenComplete(
                (mesh, error) -> Minecraft.getInstance().execute(() -> {
                    if (mesh == null) {
                        if (ours == this.generation) {
                            this.building = false;
                        }
                        return;
                    }
                    if (ours != this.generation) {
                        mesh.close();
                        return;
                    }
                    this.building = false;
                    this.cx = mesh.cx();
                    this.cy = mesh.cy();
                    this.cz = mesh.cz();
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
                    this.buffer = made;
                }));
    }

    boolean building() {
        return this.building;
    }

    @Nullable
    Schematic shown() {
        return this.shown;
    }

    public void release() {
        this.generation++;
        this.building = false;
        this.shown = null;
        if (this.buffer != null) {
            this.buffer.close();
            this.buffer = null;
        }
    }

    private static Mesh mesh(Schematic schematic) {
        int step = Math.max(1, (Math.max(schematic.sizeX, Math.max(schematic.sizeY, schematic.sizeZ)) + MOST - 1)
                / MOST);
        int sx = (schematic.sizeX + step - 1) / step;
        int sy = (schematic.sizeY + step - 1) / step;
        int sz = (schematic.sizeZ + step - 1) / step;
        // Per cell its highest block's palette entry, plus one (0: empty).
        short[] kinds = new short[sx * sy * sz];
        int[] top = new int[schematic.palette.size()];
        int[] side = new int[top.length];
        for (int k = 0; k < top.length; k++) {
            BlockState state = schematic.palette.get(k);
            if (state.isAir()) {
                top[k] = -1;
                continue;
            }
            int[] colours = colours(state);
            top[k] = colours[0];
            side[k] = colours[1];
        }
        for (int y = 0; y < schematic.sizeY; y++) {
            for (int z = 0; z < schematic.sizeZ; z++) {
                for (int x = 0; x < schematic.sizeX; x++) {
                    short cell = schematic.cells[schematic.index(x, y, z)];
                    if (cell == Schematic.VOID || top[cell] < 0) {
                        continue;
                    }
                    kinds[(y / step * sz + z / step) * sx + x / step] = (short) (cell + 1);
                }
            }
        }
        ByteBufferBuilder memory = new ByteBufferBuilder(1 << 16);
        BufferBuilder out = new BufferBuilder(memory, VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int[] cell = new int[3];
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    int kind = kinds[(y * sz + z) * sx + x] - 1;
                    if (kind < 0) {
                        continue;
                    }
                    cell[0] = x;
                    cell[1] = y;
                    cell[2] = z;
                    for (Direction face : Direction.values()) {
                        if (!filled(kinds, sx, sy, sz, x + face.getStepX(), y + face.getStepY(), z
                                + face.getStepZ())) {
                            face(out, kinds, sx, sy, sz, cell, face, face == Direction.UP ? top[kind] : side[kind]);
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

    private static boolean filled(short[] kinds, int sx, int sy, int sz, int x, int y, int z) {
        return x >= 0 && y >= 0 && z >= 0 && x < sx && y < sy && z < sz && kinds[(y * sz + z) * sx + x] != 0;
    }

    // The colour a block shows on top and on its sides: its model's faces' textures, averaged and tinted.
    private static int[] colours(BlockState state) {
        FluidState fluid = state.getFluidState();
        if (state.getBlock() instanceof LiquidBlock) {
            int liquid = fluid.is(FluidTags.LAVA) ? LAVA : WATER;
            return new int[] { liquid, liquid };
        }
        BlockRenderDispatcher blocks = Minecraft.getInstance().getBlockRenderer();
        BakedModel model = blocks.getBlockModel(state);
        RandomSource random = RandomSource.create(42L);
        int up = colour(state, model.getQuads(state, Direction.UP, random));
        int around = colour(state, model.getQuads(state, Direction.NORTH, random));
        if (up < 0 || around < 0) {
            int loose = colour(state, model.getQuads(state, null, random));
            if (loose < 0) {
                loose = average(blocks.getBlockModelShaper().getParticleIcon(state));
            }
            if (loose < 0) {
                MapColor map = state.getMapColor(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                loose = map == MapColor.NONE ? CLEAR : map.col;
            }
            up = up < 0 ? loose : up;
            around = around < 0 ? loose : around;
        }
        return new int[] { up, around };
    }

    private static int colour(BlockState state, List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return -1;
        }
        BakedQuad quad = quads.get(0);
        int colour = average(quad.getSprite());
        if (colour < 0 || !quad.isTinted()) {
            return colour;
        }
        int tint;
        try {
            tint = Minecraft.getInstance().getBlockColors().getColor(state, null, null, quad.getTintIndex());
        } catch (RuntimeException e) {
            tint = -1;
        }
        if (tint == -1) {
            return colour;
        }
        return (colour >> 16 & 0xFF) * (tint >> 16 & 0xFF) / 255 << 16 | (colour >> 8 & 0xFF) * (tint >> 8 & 0xFF)
                / 255 << 8 | (colour & 0xFF) * (tint & 0xFF) / 255;
    }

    // A texture's colour: its pixels averaged by how solid each is; -1 when it is wholly clear.
    private static int average(TextureAtlasSprite sprite) {
        return SPRITES.computeIfAbsent(sprite.contents(), contents -> {
            NativeImage image = sprite.contents().getOriginalImage();
            int width = Math.min(sprite.contents().width(), image.getWidth());
            int height = Math.min(sprite.contents().height(), image.getHeight());
            long r = 0L;
            long g = 0L;
            long b = 0L;
            long weight = 0L;
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int a = abgr >>> 24;
                    r += (long) (abgr & 0xFF) * a;
                    g += (long) (abgr >> 8 & 0xFF) * a;
                    b += (long) (abgr >> 16 & 0xFF) * a;
                    weight += a;
                }
            }
            if (weight == 0L) {
                return -1;
            }
            return (int) (r / weight) << 16 | (int) (g / weight) << 8 | (int) (b / weight);
        });
    }

    // A side of a cell, each corner darker the more of the cells beside it there are (as the game's own soft light).
    private static void face(BufferBuilder out, short[] kinds, int sx, int sy, int sz, int[] cell, Direction side,
            int color) {
        float shade = switch (side) {
            case UP -> 1.0F;
            case DOWN -> 0.45F;
            case NORTH, SOUTH -> 0.8F;
            case WEST, EAST -> 0.62F;
        };
        int x = cell[0];
        int y = cell[1];
        int z = cell[2];
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
        int[] normal = { side.getStepX(), side.getStepY(), side.getStepZ() };
        int[] base = { x + normal[0], y + normal[1], z + normal[2] };
        for (float[] corner : corners) {
            // The two ways across the side, towards this corner.
            int[] a = new int[3];
            int[] b = new int[3];
            boolean first = true;
            for (int axis = 0; axis < 3; axis++) {
                if (normal[axis] != 0) {
                    continue;
                }
                int toward = corner[axis] > cell[axis] ? 1 : -1;
                if (first) {
                    a[axis] = toward;
                    first = false;
                } else {
                    b[axis] = toward;
                }
            }
            boolean one = filled(kinds, sx, sy, sz, base[0] + a[0], base[1] + a[1], base[2] + a[2]);
            boolean two = filled(kinds, sx, sy, sz, base[0] + b[0], base[1] + b[1], base[2] + b[2]);
            boolean both = filled(kinds, sx, sy, sz, base[0] + a[0] + b[0], base[1] + a[1] + b[1], base[2] + a[2]
                    + b[2]);
            int open = one && two ? 0 : 3 - (one ? 1 : 0) - (two ? 1 : 0) - (both ? 1 : 0);
            float light = shade * (0.55F + 0.15F * open);
            int r = Math.round((color >> 16 & 0xFF) * light);
            int g = Math.round((color >> 8 & 0xFF) * light);
            int bl = Math.round((color & 0xFF) * light);
            out.addVertex(corner[0], corner[1], corner[2]).setColor(0xFF000000 | r << 16 | g << 8 | bl);
        }
    }

    // Draws it in a box of the screen, turned by `turn` (radians) and looked at a little from above.
    void draw(GuiGraphics graphics, int x, int y, int width, int height, float turn) {
        VertexBuffer model = this.buffer;
        int cx = this.cx;
        int cy = this.cy;
        int cz = this.cz;
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
