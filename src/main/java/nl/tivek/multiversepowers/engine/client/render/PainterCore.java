package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayDeque;
import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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

    static final class Layer {
        private float[] at = new float[3 * 1024];
        private int[] color = new int[1024];
        private int count;

        void add(double x, double y, double z, int rgb, int alpha) {
            if (this.count == this.color.length) {
                this.at = Arrays.copyOf(this.at, this.at.length * 2);
                this.color = Arrays.copyOf(this.color, this.color.length * 2);
            }
            int i = this.count++;
            this.at[3 * i] = (float) x;
            this.at[3 * i + 1] = (float) y;
            this.at[3 * i + 2] = (float) z;
            this.color[i] = alpha << 24 | rgb & 0xFFFFFF;
        }
    }

    private static final ArrayDeque<Layer> SPARE = new ArrayDeque<>();

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
    private double glare;
    double ambient;

    PainterCore(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material, boolean hand) {
        this.matrix = pose.last().pose();
        this.camera = camera;
        this.time = time;
        this.frustum = frustum;
        this.material = material;
        this.hand = hand;
    }

    public Material material() {
        return this.material;
    }

    public void material(Material material) {
        this.material = material;
    }

    public void finish(MultiBufferSource.BufferSource buffers) {
        this.draw(buffers, MASS, this.mass);
        this.draw(buffers, this.hand ? HAND_LIGHT : LIGHT, this.light);
        this.draw(buffers, this.hand ? HAND_GLOW : GLOW, this.glow);
    }

    private void draw(MultiBufferSource.BufferSource buffers, RenderType type, Layer layer) {
        if (layer.count > 0) {
            VertexConsumer buffer = buffers.getBuffer(type);
            float[] at = layer.at;
            int[] color = layer.color;
            Vector3f vertex = this.vertex;
            for (int i = 0; i < layer.count; i++) {
                int argb = color[i];
                this.matrix.transformPosition(at[3 * i], at[3 * i + 1], at[3 * i + 2], vertex);
                buffer.addVertex(vertex.x, vertex.y, vertex.z).setColor(argb >> 16 & 0xFF, argb >> 8 & 0xFF,
                        argb & 0xFF, argb >>> 24);
            }
            buffers.endBatch(type);
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
        this.put(layer, ax, ay, az, rgb, alphaA);
        this.put(layer, bx, by, bz, rgb, alphaB);
        this.put(layer, bx + sx, by + sy, bz + sz, rgb, 0);
        this.put(layer, ax + sx, ay + sy, az + sz, rgb, 0);
        this.put(layer, ax, ay, az, rgb, alphaA);
        this.put(layer, bx, by, bz, rgb, alphaB);
        this.put(layer, bx - sx, by - sy, bz - sz, rgb, 0);
        this.put(layer, ax - sx, ay - sy, az - sz, rgb, 0);
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
        layer.add(x - this.camera.x, y - this.camera.y, z - this.camera.z, rgb, alpha);
    }

    int faded(double x, double y, double z, int alpha) {
        if (!this.nearFade || this.hand) {
            return alpha;
        }
        double away = Math.sqrt(sq(x - this.camera.x) + sq(y - this.camera.y) + sq(z - this.camera.z));
        return (int) (alpha * Ease.smooth((away - NEAR_GONE) / (NEAR_CLEAR - NEAR_GONE)));
    }
}
