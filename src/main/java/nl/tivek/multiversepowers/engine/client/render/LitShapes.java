package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// Solid shapes in the world, lit by the world's light and shaded by the sun as creatures are, drawn with a texture
// (a `PaintedTexture`): bands between two rings across a tube, round or squarish, caps, boxes and single quads. Points
// are world positions; the camera is taken off here.
public final class LitShapes {
    // A part of the texture, in 0..1.
    public record Skin(float u0, float v0, float u1, float v1) {
        public Skin rows(float from, float to) {
            return new Skin(this.u0, Mth.lerp(from, this.v0, this.v1), this.u1, Mth.lerp(to, this.v0, this.v1));
        }
    }

    // A ring round `center` across the axes `a` and `b` (unit, square to each other), `ra` and `rb` out along them;
    // `square` 2 for an ellipse, higher for a rounded box.
    public record Ring(Vec3 center, Vec3 a, Vec3 b, double ra, double rb, double square) {
        public static Ring round(Vec3 center, Vec3 a, Vec3 b, double radius) {
            return new Ring(center, a, b, radius, radius, 2.0);
        }

        public Ring at(Vec3 center) {
            return new Ring(center, this.a, this.b, this.ra, this.rb, this.square);
        }

        public Ring scaled(double factor) {
            return new Ring(this.center, this.a, this.b, this.ra * factor, this.rb * factor, this.square);
        }

        Vec3 point(double angle) {
            double c = Math.cos(angle);
            double s = Math.sin(angle);
            double e = 2.0 / this.square;
            return this.center.add(this.a.scale(this.ra * signed(c, e))).add(this.b.scale(this.rb * signed(s, e)));
        }

        Vec3 normal(double angle) {
            double c = Math.cos(angle);
            double s = Math.sin(angle);
            double e = 2.0 - 2.0 / this.square;
            Vec3 n = this.a.scale(signed(c, e) / Math.max(this.ra, 1.0E-4))
                    .add(this.b.scale(signed(s, e) / Math.max(this.rb, 1.0E-4)));
            double size = n.length();
            return size < 1.0E-6 ? this.a : n.scale(1.0 / size);
        }

        private static double signed(double v, double power) {
            return Math.copySign(Math.pow(Math.abs(v), power), v);
        }
    }

    private final PoseStack.Pose pose;
    private final Vec3 camera;
    private VertexConsumer buffer;
    private int color = 0xFFFFFFFF;

    public LitShapes(PoseStack.Pose pose, Vec3 camera, VertexConsumer buffer) {
        this.pose = pose;
        this.camera = camera;
        this.buffer = buffer;
    }

    public LitShapes into(VertexConsumer buffer) {
        this.buffer = buffer;
        return this;
    }

    // The colour (ARGB) the texture is multiplied by from here on.
    public LitShapes color(int argb) {
        this.color = argb;
        return this;
    }

    // The side of a tube from ring `from` to ring `to` in `sides` quads, the skin wrapped once round it (u) and laid
    // along it (v). `twist` turns where the quads start (radians).
    public void band(Ring from, Ring to, int sides, double twist, Skin skin, int lightFrom, int lightTo) {
        Vec3 along = to.center.subtract(from.center);
        double length = along.length();
        Vec3 axis = length < 1.0E-6 ? Vec3.ZERO : along.scale(1.0 / length);
        double slope = length < 1.0E-6 ? 0.0 : ((from.ra + from.rb) - (to.ra + to.rb)) * 0.5 / length;
        for (int k = 0; k < sides; k++) {
            double a0 = twist + Math.PI * 2 * k / sides;
            double a1 = twist + Math.PI * 2 * (k + 1) / sides;
            float u0 = Mth.lerp((float) k / sides, skin.u0, skin.u1);
            float u1 = Mth.lerp((float) (k + 1) / sides, skin.u0, skin.u1);
            Vec3 n0 = tilted(from.normal(a0).add(to.normal(a0)), axis, slope);
            Vec3 n1 = tilted(from.normal(a1).add(to.normal(a1)), axis, slope);
            this.vertex(from.point(a0), u0, skin.v0, lightFrom, n0);
            this.vertex(from.point(a1), u1, skin.v0, lightFrom, n1);
            this.vertex(to.point(a1), u1, skin.v1, lightTo, n1);
            this.vertex(to.point(a0), u0, skin.v1, lightTo, n0);
        }
    }

    private static Vec3 tilted(Vec3 radial, Vec3 axis, double slope) {
        Vec3 n = radial.normalize().add(axis.scale(slope));
        double size = n.length();
        return size < 1.0E-6 ? radial : n.scale(1.0 / size);
    }

    // A flat cap closing a ring, facing `facing`.
    public void cap(Ring ring, int sides, double twist, Vec3 facing, Skin skin, int light) {
        float um = (skin.u0 + skin.u1) / 2;
        float vm = (skin.v0 + skin.v1) / 2;
        for (int k = 0; k < sides; k++) {
            double a0 = twist + Math.PI * 2 * k / sides;
            double a1 = twist + Math.PI * 2 * (k + 1) / sides;
            float u0 = Mth.lerp((float) k / sides, skin.u0, skin.u1);
            float u1 = Mth.lerp((float) (k + 1) / sides, skin.u0, skin.u1);
            this.vertex(ring.center, um, vm, light, facing);
            this.vertex(ring.point(a0), u0, skin.v0, light, facing);
            this.vertex(ring.point(a1), u1, skin.v0, light, facing);
            this.vertex(ring.center, um, vm, light, facing);
        }
    }

    // A box round `center` with half sizes `hx`, `hy`, `hz` along the axes `x`, `y`, `z` (unit, square to each other).
    public void box(Vec3 center, Vec3 x, Vec3 y, Vec3 z, double hx, double hy, double hz, Skin skin, int light) {
        Vec3 sx = x.scale(hx);
        Vec3 sy = y.scale(hy);
        Vec3 sz = z.scale(hz);
        this.face(center.add(sx), sy, sz, x, skin, light);
        this.face(center.subtract(sx), sz, sy, x.scale(-1.0), skin, light);
        this.face(center.add(sy), sz, sx, y, skin, light);
        this.face(center.subtract(sy), sx, sz, y.scale(-1.0), skin, light);
        this.face(center.add(sz), sx, sy, z, skin, light);
        this.face(center.subtract(sz), sy, sx, z.scale(-1.0), skin, light);
    }

    private void face(Vec3 middle, Vec3 u, Vec3 v, Vec3 normal, Skin skin, int light) {
        this.vertex(middle.subtract(u).subtract(v), skin.u0, skin.v0, light, normal);
        this.vertex(middle.add(u).subtract(v), skin.u1, skin.v0, light, normal);
        this.vertex(middle.add(u).add(v), skin.u1, skin.v1, light, normal);
        this.vertex(middle.subtract(u).add(v), skin.u0, skin.v1, light, normal);
    }

    // One quad through four points in order, its normal from their turn.
    public void quad(Vec3 a, Vec3 b, Vec3 c, Vec3 d, Skin skin, int light) {
        Vec3 n = c.subtract(a).cross(d.subtract(b));
        double size = n.length();
        n = size < 1.0E-9 ? new Vec3(0.0, 1.0, 0.0) : n.scale(1.0 / size);
        this.vertex(a, skin.u0, skin.v0, light, n);
        this.vertex(b, skin.u1, skin.v0, light, n);
        this.vertex(c, skin.u1, skin.v1, light, n);
        this.vertex(d, skin.u0, skin.v1, light, n);
    }

    private void vertex(Vec3 at, float u, float v, int light, Vec3 normal) {
        this.buffer.addVertex(this.pose, (float) (at.x - this.camera.x), (float) (at.y - this.camera.y),
                (float) (at.z - this.camera.z)).setColor(this.color >> 16 & 0xFF, this.color >> 8 & 0xFF,
                        this.color & 0xFF, this.color >>> 24).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(this.pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}
