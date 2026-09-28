package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.lang.ref.Cleaner;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayDeque;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

abstract class PainterCore {
    private static final RenderType LIGHT = RenderType.create("welcomescreen_hard_light",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.WEATHER_TARGET)
                    .createCompositeState(false));
    private static final RenderType MASS = RenderType.create("welcomescreen_hard_light_mass",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final RenderType GLOW = RenderType.create("welcomescreen_hard_light_glow",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setOutputState(RenderStateShard.WEATHER_TARGET)
                    .createCompositeState(false));
    // No WEATHER_TARGET: first-person hand constructs draw after the world, once that target is already on screen.
    private static final RenderType HAND_LIGHT = RenderType.create("welcomescreen_hard_light_hand",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final RenderType HAND_GLOW = RenderType.create("welcomescreen_hard_light_hand_glow",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private static final double NEAR_GONE = 0.2;
    private static final double NEAR_CLEAR = 0.75;
    // Past this from the eye nothing fades, with room for the edges lifted towards it.
    static final double FADE_REACH = NEAR_CLEAR + 1.0;
    protected static final double EDGE = 0.95;
    protected static final double HALO = 0.28;
    static final double EDGE_WIDTH = 0.035;
    static final double HALO_WIDTH = 0.1;
    static final double WIDTH_CAP = 3.0;
    private static final double SKY_FLOOR = 0.42;
    private static final double SKY = 0.3;
    private static final double SUN_LIGHT = 0.3;
    private static final Vec3 SUN = new Vec3(0.45, 0.75, -0.5).normalize();

    private static final double TINY = 0.012;

    // Vertices go straight into memory as they are painted, the bytes a BufferBuilder would write: a big construct has
    // hundreds of thousands, and a BufferBuilder's checks on each, or a copy of them all, cost more than the rest.
    static final class Layer {
        private static final boolean LITTLE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
        private final ByteBufferBuilder bytes = new ByteBufferBuilder(1 << 16);
        private int count;

        Layer() {
            ByteBufferBuilder owned = this.bytes;
            // A painter dropped before it finished never hands its layers back: their memory goes with them.
            FREE.register(this, owned::close);
        }

        // (x, y, z) from the camera, where the view's matrix puts it.
        void add(Matrix4f matrix, Vector3f vertex, double x, double y, double z, int rgb, int alpha) {
            matrix.transformPosition((float) x, (float) y, (float) z, vertex);
            write(this.bytes.reserve(VERTEX_BYTES), vertex.x, vertex.y, vertex.z, rgb, alpha);
            this.count++;
        }

        // Room for n vertices at once, written with write(): one check of the memory instead of one a vertex.
        long reserve(int n) {
            this.count += n;
            return this.bytes.reserve(n * VERTEX_BYTES);
        }

        static void write(long p, float x, float y, float z, int rgb, int alpha) {
            MemoryUtil.memPutFloat(p, x);
            MemoryUtil.memPutFloat(p + 4L, y);
            MemoryUtil.memPutFloat(p + 8L, z);
            if (LITTLE) {
                // Red, green, blue and alpha in one store: the same four bytes in the same order.
                MemoryUtil.memPutInt(p + 12L, rgb >> 16 & 0xFF | rgb & 0xFF00 | (rgb & 0xFF) << 16 | alpha << 24);
                return;
            }
            MemoryUtil.memPutByte(p + 12L, (byte) (rgb >> 16));
            MemoryUtil.memPutByte(p + 13L, (byte) (rgb >> 8));
            MemoryUtil.memPutByte(p + 14L, (byte) rgb);
            MemoryUtil.memPutByte(p + 15L, (byte) alpha);
        }
    }

    private static final Cleaner FREE = Cleaner.create();
    private static final ArrayDeque<Layer> SPARE = new ArrayDeque<>();
    private static final int VERTEX_BYTES = 16;

    private static Layer take() {
        Layer layer = SPARE.poll();
        return layer == null ? new Layer() : layer;
    }

    private final Matrix4f matrix;
    final Vec3 camera;
    final float time;
    @Nullable
    private final Frustum frustum;
    private final boolean hand;
    Material material;
    private final Vector3f vertex = new Vector3f();
    final Layer mass = take();
    final Layer light = take();
    final Layer glow = take();
    boolean nearFade;
    // The player's own strength of the glow layer (ClientSettings.GLOW_STRENGTH), read once per painter.
    private final float glowShare;
    private double glare;
    double ambient;
    boolean waiting;

    PainterCore(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material, boolean hand) {
        this.matrix = pose.last().pose();
        this.camera = camera;
        this.time = time;
        this.frustum = frustum;
        this.material = material;
        this.hand = hand;
        this.glowShare = ClientSettings.factor(ClientSettings.GLOW_STRENGTH);
    }

    public Material material() {
        return this.material;
    }

    public void material(Material material) {
        this.material = material;
    }

    public void finish(MultiBufferSource.BufferSource buffers) {
        if (this.waiting) {
            this.settle();
        }
        this.draw(buffers, MASS, this.mass);
        this.draw(buffers, this.hand ? HAND_LIGHT : LIGHT, this.light);
        this.draw(buffers, this.hand ? HAND_GLOW : GLOW, this.glow);
    }

    private void draw(MultiBufferSource.BufferSource buffers, RenderType type, Layer layer) {
        int count = layer.count;
        if (count > 0) {
            buffers.endLastBatch();
            ByteBufferBuilder.Result written = layer.bytes.build();
            if (written != null) {
                VertexFormat.Mode mode = type.mode();
                type.draw(new MeshData(written, new MeshData.DrawState(type.format(), count, mode.indexCount(count),
                        mode, VertexFormat.IndexType.least(count))));
            }
        }
        layer.count = 0;
        SPARE.push(layer);
    }

    public boolean visible(Vec3 center, double radius) {
        return this.frustum == null || this.frustum.isVisible(new AABB(center.x - radius, center.y - radius,
                center.z - radius, center.x + radius, center.y + radius, center.z + radius));
    }

    boolean tiny(Vec3 center, double radius) {
        double away = center.distanceTo(this.camera);
        return away > 1.0 && radius / away < TINY;
    }

    public void side(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, double solid) {
        this.quad(this.mass, p0, p1, p2, p3, rgb, Colors.alpha(solid));
    }

    public void lightLine(Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        this.line(this.light, a, b, width, rgb, alpha);
    }

    public void glowLine(Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        this.line(this.glow, a, b, width, rgb, alpha);
    }

    public void massQuad(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, int alpha) {
        this.quad(this.mass, p0, p1, p2, p3, rgb, alpha);
    }

    public void lightQuad(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, int alpha) {
        this.quad(this.light, p0, p1, p2, p3, rgb, alpha);
    }

    protected void nearFade(boolean on) {
        this.nearFade = on;
    }

    public void edge(Vec3 a, Vec3 b, double width, double strength) {
        this.line(this.light, a, b, width, this.material.edge(), Colors.alpha(EDGE * strength));
        this.line(this.glow, a, b, width * 3.0, this.material.glow(), Colors.alpha(HALO * strength));
    }

    public void sheet(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double a0, double a1, double a2, double a3) {
        this.put(this.glow, p0.x, p0.y, p0.z, this.material.glow(), Colors.alpha(0.6 * a0));
        this.put(this.glow, p1.x, p1.y, p1.z, this.material.glow(), Colors.alpha(0.6 * a1));
        this.put(this.glow, p2.x, p2.y, p2.z, this.material.glow(), Colors.alpha(0.6 * a2));
        this.put(this.glow, p3.x, p3.y, p3.z, this.material.glow(), Colors.alpha(0.6 * a3));
        this.put(this.light, p0.x, p0.y, p0.z, this.material.edge(), Colors.alpha(0.3 * a0));
        this.put(this.light, p1.x, p1.y, p1.z, this.material.edge(), Colors.alpha(0.3 * a1));
        this.put(this.light, p2.x, p2.y, p2.z, this.material.edge(), Colors.alpha(0.3 * a2));
        this.put(this.light, p3.x, p3.y, p3.z, this.material.edge(), Colors.alpha(0.3 * a3));
    }

    static double sq(double value) {
        return value * value;
    }

    static double sheen(double face) {
        return 0.88 + 0.24 * (1.0 - Mth.clamp(face, 0.0, 1.0));
    }

    public Vec3 camera() {
        return this.camera;
    }

    public float time() {
        return this.time;
    }

    public static double lit(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 middle) {
        Vec3 normal = p1.subtract(p0).cross(p2.subtract(p0));
        return lit(normal.x, normal.y, normal.z, p0.x - middle.x, p0.y - middle.y, p0.z - middle.z);
    }

    static double lit(double nx, double ny, double nz, double dx, double dy, double dz) {
        double lengthSqr = nx * nx + ny * ny + nz * nz;
        if (lengthSqr < 1.0E-12) {
            return 0.8;
        }
        double length = Math.sqrt(lengthSqr);
        double x = length < 1.0E-4 ? 0.0 : nx / length;
        double y = length < 1.0E-4 ? 0.0 : ny / length;
        double z = length < 1.0E-4 ? 0.0 : nz / length;
        if (x * dx + y * dy + z * dz < 0.0) {
            x *= -1.0;
            y *= -1.0;
            z *= -1.0;
        }
        return light(x, y, z);
    }

    public static double light(Vec3 normal) {
        return light(normal.x, normal.y, normal.z);
    }

    public static double light(double x, double y, double z) {
        return SKY_FLOOR + SKY * (y * 0.5 + 0.5) + SUN_LIGHT * Math.max(0.0, x * SUN.x + y * SUN.y + z * SUN.z);
    }

    int mass(double light) {
        int rgb = Colors.shade(this.material.mass(), Math.min(1.0, light));
        return this.glare > 0.0 ? Colors.mix(rgb, this.material.hot(), (float) Math.min(0.75, this.glare
                * Math.min(1.0, light))) : rgb;
    }

    public void glare(double amount) {
        this.glare = Mth.clamp(amount, 0.0, 1.0);
    }

    public void ambient(double amount) {
        this.ambient = Math.max(0.0, amount);
    }

    void line(Layer layer, Vec3 a, Vec3 b, double width, int rgb, int alpha) {
        this.line(layer, a.x, a.y, a.z, b.x, b.y, b.z, width, rgb, alpha);
    }

    void line(Layer layer, double ax, double ay, double az, double bx, double by, double bz, double width,
            int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double tx = this.camera.x - (ax + bx) * 0.5;
        double ty = this.camera.y - (ay + by) * 0.5;
        double tz = this.camera.z - (az + bz) * 0.5;
        double sx = dy * tz - dz * ty;
        double sy = dz * tx - dx * tz;
        double sz = dx * ty - dy * tx;
        double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (length < 1.0E-6) {
            return;
        }
        double k = width * 0.5 / length;
        sx *= k;
        sy *= k;
        sz *= k;
        int alphaA = this.faded(ax, ay, az, alpha);
        int alphaB = this.faded(bx, by, bz, alpha);
        if (this.waiting) {
            this.settle();
        }
        alphaA = this.shared(layer, alphaA);
        alphaB = this.shared(layer, alphaB);
        // The two ends are each in both halves of the line: worked out once, written twice.
        Vector3f v = this.vertex;
        this.matrix.transformPosition((float) (ax - this.camera.x), (float) (ay - this.camera.y),
                (float) (az - this.camera.z), v);
        float x0 = v.x;
        float y0 = v.y;
        float z0 = v.z;
        this.matrix.transformPosition((float) (bx - this.camera.x), (float) (by - this.camera.y),
                (float) (bz - this.camera.z), v);
        this.halves(layer, ax, ay, az, bx, by, bz, sx, sy, sz, x0, y0, z0, v.x, v.y, v.z, rgb, alphaA, alphaB);
    }

    // A lit line and its glow along the same edge, each as line() draws it: the edge's two ends, their fading and the
    // turn towards the eye are worked out once for both.
    void linePair(double ax, double ay, double az, double bx, double by, double bz, double lightWidth, int lightRgb,
            int lightAlpha, double glowWidth, int glowRgb, int glowAlpha) {
        if (lightAlpha <= 0 && glowAlpha <= 0) {
            return;
        }
        double dx = bx - ax;
        double dy = by - ay;
        double dz = bz - az;
        double tx = this.camera.x - (ax + bx) * 0.5;
        double ty = this.camera.y - (ay + by) * 0.5;
        double tz = this.camera.z - (az + bz) * 0.5;
        double sx = dy * tz - dz * ty;
        double sy = dz * tx - dx * tz;
        double sz = dx * ty - dy * tx;
        double length = Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (length < 1.0E-6) {
            return;
        }
        double fadeA = this.fade(ax, ay, az);
        double fadeB = this.fade(bx, by, bz);
        if (this.waiting) {
            this.settle();
        }
        Vector3f v = this.vertex;
        Matrix4f m = this.matrix;
        double cx = this.camera.x;
        double cy = this.camera.y;
        double cz = this.camera.z;
        m.transformPosition((float) (ax - cx), (float) (ay - cy), (float) (az - cz), v);
        float x0 = v.x;
        float y0 = v.y;
        float z0 = v.z;
        m.transformPosition((float) (bx - cx), (float) (by - cy), (float) (bz - cz), v);
        float x1 = v.x;
        float y1 = v.y;
        float z1 = v.z;
        if (lightAlpha > 0) {
            double k = lightWidth * 0.5 / length;
            this.halves(this.light, ax, ay, az, bx, by, bz, sx * k, sy * k, sz * k, x0, y0, z0, x1, y1, z1, lightRgb,
                    this.shared(this.light, fadeA < 0.0 ? lightAlpha : (int) (lightAlpha * fadeA)),
                    this.shared(this.light, fadeB < 0.0 ? lightAlpha : (int) (lightAlpha * fadeB)));
        }
        if (glowAlpha > 0) {
            double k = glowWidth * 0.5 / length;
            this.halves(this.glow, ax, ay, az, bx, by, bz, sx * k, sy * k, sz * k, x0, y0, z0, x1, y1, z1, glowRgb,
                    this.shared(this.glow, fadeA < 0.0 ? glowAlpha : (int) (glowAlpha * fadeA)),
                    this.shared(this.glow, fadeB < 0.0 ? glowAlpha : (int) (glowAlpha * fadeB)));
        }
    }

    // The eight vertices of a line whose ends a and b are already placed at (x0, y0, z0) and (x1, y1, z1).
    private void halves(Layer layer, double ax, double ay, double az, double bx, double by, double bz, double sx,
            double sy, double sz, float x0, float y0, float z0, float x1, float y1, float z1, int rgb, int alphaA,
            int alphaB) {
        Vector3f v = this.vertex;
        Matrix4f m = this.matrix;
        double cx = this.camera.x;
        double cy = this.camera.y;
        double cz = this.camera.z;
        long p = layer.reserve(8);
        Layer.write(p, x0, y0, z0, rgb, alphaA);
        Layer.write(p + 16L, x1, y1, z1, rgb, alphaB);
        m.transformPosition((float) (bx + sx - cx), (float) (by + sy - cy), (float) (bz + sz - cz), v);
        Layer.write(p + 32L, v.x, v.y, v.z, rgb, 0);
        m.transformPosition((float) (ax + sx - cx), (float) (ay + sy - cy), (float) (az + sz - cz), v);
        Layer.write(p + 48L, v.x, v.y, v.z, rgb, 0);
        Layer.write(p + 64L, x0, y0, z0, rgb, alphaA);
        Layer.write(p + 80L, x1, y1, z1, rgb, alphaB);
        m.transformPosition((float) (bx - sx - cx), (float) (by - sy - cy), (float) (bz - sz - cz), v);
        Layer.write(p + 96L, v.x, v.y, v.z, rgb, 0);
        m.transformPosition((float) (ax - sx - cx), (float) (ay - sy - cy), (float) (az - sz - cz), v);
        Layer.write(p + 112L, v.x, v.y, v.z, rgb, 0);
    }

    // What faded() multiplies an alpha by at this point, or -1 where nothing fades.
    double fade(double x, double y, double z) {
        if (!this.nearFade || this.hand) {
            return -1.0;
        }
        double away = Math.sqrt(sq(x - this.camera.x) + sq(y - this.camera.y) + sq(z - this.camera.z));
        return Ease.smooth((away - NEAR_GONE) / (NEAR_CLEAR - NEAR_GONE));
    }

    // A glowing vertex's alpha as the player's glow strength has it.
    int shared(Layer layer, int alpha) {
        return layer == this.glow && this.glowShare != 1.0F ? Math.min(255, (int) (alpha * this.glowShare)) : alpha;
    }

    // Four vertices of one quad, from the camera the way put() places them, room made for all four at once.
    void quad4(Layer layer, double x0, double y0, double z0, int a0, double x1, double y1, double z1, int a1,
            double x2, double y2, double z2, int a2, double x3, double y3, double z3, int a3, int rgb) {
        if (this.waiting) {
            this.settle();
        }
        Vector3f v = this.vertex;
        Matrix4f m = this.matrix;
        double cx = this.camera.x;
        double cy = this.camera.y;
        double cz = this.camera.z;
        long p = layer.reserve(4);
        m.transformPosition((float) (x0 - cx), (float) (y0 - cy), (float) (z0 - cz), v);
        Layer.write(p, v.x, v.y, v.z, rgb, this.shared(layer, a0));
        m.transformPosition((float) (x1 - cx), (float) (y1 - cy), (float) (z1 - cz), v);
        Layer.write(p + 16L, v.x, v.y, v.z, rgb, this.shared(layer, a1));
        m.transformPosition((float) (x2 - cx), (float) (y2 - cy), (float) (z2 - cz), v);
        Layer.write(p + 32L, v.x, v.y, v.z, rgb, this.shared(layer, a2));
        m.transformPosition((float) (x3 - cx), (float) (y3 - cy), (float) (z3 - cz), v);
        Layer.write(p + 48L, v.x, v.y, v.z, rgb, this.shared(layer, a3));
    }

    private void quad(Layer layer, Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        this.put(layer, p0.x, p0.y, p0.z, rgb, this.faded(p0.x, p0.y, p0.z, alpha));
        this.put(layer, p1.x, p1.y, p1.z, rgb, this.faded(p1.x, p1.y, p1.z, alpha));
        this.put(layer, p2.x, p2.y, p2.z, rgb, this.faded(p2.x, p2.y, p2.z, alpha));
        this.put(layer, p3.x, p3.y, p3.z, rgb, this.faded(p3.x, p3.y, p3.z, alpha));
    }

    void put(Layer layer, double x, double y, double z, int rgb, int alpha) {
        if (this.waiting) {
            this.settle();
        }
        if (layer == this.glow && this.glowShare != 1.0F) {
            alpha = Math.min(255, (int) (alpha * this.glowShare));
        }
        layer.add(this.matrix, this.vertex, x - this.camera.x, y - this.camera.y, z - this.camera.z, rgb, alpha);
    }

    // Shapes held back to be worked out together (see ConstructPainter.batch) go down before anything drawn after
    // them.
    void settle() {
    }

    // What decides how a shape is drawn, beyond the shape itself, kept for a painter on another thread to take up.
    void saveState(double[] state) {
        state[0] = this.glare;
        state[1] = this.ambient;
    }

    void loadState(double[] state) {
        this.glare = state[0];
        this.ambient = state[1];
    }

    Matrix4f matrix() {
        return this.matrix;
    }

    @Nullable
    Frustum frustum() {
        return this.frustum;
    }

    boolean hand() {
        return this.hand;
    }

    // Another painter's layers joined on after this one's, and that painter's layers handed back.
    void append(PainterCore other) {
        appendLayer(this.mass, other.mass);
        appendLayer(this.light, other.light);
        appendLayer(this.glow, other.glow);
        SPARE.push(other.mass);
        SPARE.push(other.light);
        SPARE.push(other.glow);
    }

    private static void appendLayer(Layer to, Layer from) {
        ByteBufferBuilder.Result written = from.count == 0 ? null : from.bytes.build();
        if (written != null) {
            ByteBuffer bytes = written.byteBuffer();
            int length = bytes.remaining();
            MemoryUtil.memCopy(MemoryUtil.memAddress(bytes), to.bytes.reserve(length), length);
            to.count += from.count;
            written.close();
        }
        from.count = 0;
    }

    int faded(double x, double y, double z, int alpha) {
        if (!this.nearFade || this.hand) {
            return alpha;
        }
        double away = Math.sqrt(sq(x - this.camera.x) + sq(y - this.camera.y) + sq(z - this.camera.z));
        return (int) (alpha * Ease.smooth((away - NEAR_GONE) / (NEAR_CLEAR - NEAR_GONE)));
    }
}
