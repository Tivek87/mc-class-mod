package nl.tivek.multiversepowers.engine.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collection;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Vectors;

abstract class PainterLight extends PainterCore {
    PainterLight(PoseStack pose, Vec3 camera, float time, @Nullable Frustum frustum, Material material,
            boolean hand) {
        super(pose, camera, time, frustum, material, hand);
    }

    public void trail(Vec3 head, Collection<Vec3> points, double strength, boolean own) {
        double reach = 7.0;
        double gone = 0.0;
        Vec3 last = head;
        int i = 0;
        for (Vec3 point : points) {
            if (i++ == 0) {
                continue;
            }
            double step = last.distanceTo(point);
            if (gone + step > reach) {
                point = last.add(point.subtract(last).scale((reach - gone) / Math.max(step, 1.0E-6)));
                step = reach - gone;
            }
            double t0 = gone / reach;
            gone += step;
            double t = gone / reach;
            double fade = strength * Math.pow(1.0 - t, 1.3);
            if (this.camera.distanceTo(point) > (own ? 2.5 : 1.5)) {
                this.line(this.glow, last, point, 0.32 * (1.0 - t0) + 0.05, this.material.glow(),
                        Colors.alpha(0.5 * fade));
                this.line(this.light, last, point, 0.07 * (1.0 - t0) + 0.02, this.material.edge(),
                        Colors.alpha(0.75 * fade));
            }
            if (gone >= reach) {
                return;
            }
            last = point;
        }
    }

    public void exhaust(Vec3 from, Vec3 way, double length, double radius, double thrust) {
        double power = Mth.clamp(thrust, 0.0, 1.0);
        if (power <= 0.01 || way.lengthSqr() < 1.0E-8 || !this.visible(from, length + radius * 4.0)) {
            return;
        }
        Vec3 along = way.normalize();
        double flicker = 0.85 + 0.15 * Math.sin(this.time * 3.1) * Math.sin(this.time * 1.7 + 0.6);
        double reach = length * power * flicker;
        this.line(this.glow, from, from.add(along.scale(reach)), radius * 4.0 * (0.6 + 0.4 * power),
                this.material.glow(),
                Colors.alpha(0.55 * power));
        this.line(this.light, from, from.add(along.scale(reach * 0.8)), radius * 1.9, this.material.edge(),
                Colors.alpha(0.75 * power));
        this.line(this.light, from, from.add(along.scale(reach * 0.45)), radius * 0.9, this.material.hot(),
                Colors.alpha(0.95 * power));
        Vec3[] across = Vectors.across(along);
        int diamonds = 4;
        for (int k = 1; k <= diamonds; k++) {
            double t = k / (diamonds + 1.0);
            Vec3 at = from.add(along.scale(reach * t * 0.9));
            double ring = radius * (1.0 - 0.6 * t) * (0.9 + 0.1 * Math.sin(this.time * 2.3 + k));
            this.circle(at, across[0], across[1], ring, radius * 0.12, radius * 0.6,
                    Colors.alpha(0.8 * power * (1.0 - t)), Colors.alpha(0.4 * power * (1.0 - t)));
        }
        this.flare(from, radius * 1.6 * (0.7 + 0.3 * power), 0.8 * power);
    }

    public void flare(Vec3 at, double size, double strength) {
        if (strength <= 0.0) {
            return;
        }
        Vec3 view = this.camera.subtract(at);
        if (view.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3[] across = Vectors.across(view.normalize());
        int sides = 14;
        for (int i = 0; i < sides; i++) {
            double a0 = Math.PI * 2 * i / sides;
            double a1 = Math.PI * 2 * (i + 1) / sides;
            Vec3 r0 = across[0].scale(Math.cos(a0)).add(across[1].scale(Math.sin(a0)));
            Vec3 r1 = across[0].scale(Math.cos(a1)).add(across[1].scale(Math.sin(a1)));
            this.fan(this.glow, at, r0, r1, size, this.material.glow(), Colors.alpha(0.7 * strength));
            this.fan(this.light, at, r0, r1, size * 0.35, this.material.hot(), Colors.alpha(0.9 * strength));
        }
        for (int k = 0; k < 4; k++) {
            double angle = this.time * 0.05 + Math.PI * 0.5 * k;
            Vec3 ray = across[0].scale(Math.cos(angle)).add(across[1].scale(Math.sin(angle))).scale(size * 1.6);
            this.line(this.light, at.subtract(ray), at.add(ray), size * 0.12, this.material.edge(),
                    Colors.alpha(0.8 * strength));
        }
    }

    private void fan(Layer layer, Vec3 at, Vec3 r0, Vec3 r1, double size, int rgb, int alpha) {
        if (alpha <= 0) {
            return;
        }
        this.put(layer, at.x, at.y, at.z, rgb, alpha);
        this.put(layer, at.x + r0.x * size, at.y + r0.y * size, at.z + r0.z * size, rgb, 0);
        this.put(layer, at.x + r1.x * size, at.y + r1.y * size, at.z + r1.z * size, rgb, 0);
        this.put(layer, at.x, at.y, at.z, rgb, alpha);
    }

    public void band(Vec3 center, double radius, double strength) {
        this.circle(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1), radius, 0.05, 0.3, Colors.alpha(0.95 * strength),
                Colors.alpha(0.45 * strength));
    }

    public void circle(Vec3 center, Vec3 a, Vec3 b, double radius, double width, double glowWidth, int edge,
            int halo) {
        int segments = 24;
        Vec3 last = center.add(a.scale(radius));
        for (int i = 1; i <= segments; i++) {
            double angle = Math.PI * 2 * i / segments;
            Vec3 next = center.add(a.scale(Math.cos(angle) * radius)).add(b.scale(Math.sin(angle) * radius));
            this.line(this.light, last, next, width, this.material.edge(), edge);
            this.line(this.glow, last, next, glowWidth, this.material.glow(), halo);
            last = next;
        }
    }

    public void haze(Vec3 center, Vec3 a, Vec3 b, Vec3 c, int rgb, double strength) {
        double reach = Math.max(a.length(), Math.max(b.length(), c.length()));
        if (strength <= 0.0 || reach <= 1.0E-3 || !this.visible(center, reach)) {
            return;
        }
        int rings = 9;
        int slices = 16;
        Vec3[][] points = new Vec3[rings + 1][slices + 1];
        int[][] alphas = new int[rings + 1][slices + 1];
        for (int i = 0; i <= rings; i++) {
            double polar = Math.PI * i / rings;
            for (int j = 0; j <= slices; j++) {
                double around = Math.PI * 2.0 * j / slices;
                Vec3 out = a.scale(Math.sin(polar) * Math.cos(around)).add(b.scale(Math.cos(polar)))
                        .add(c.scale(Math.sin(polar) * Math.sin(around)));
                Vec3 at = center.add(out);
                points[i][j] = at;
                Vec3 toEye = this.camera.subtract(at);
                double away = toEye.length();
                double length = out.length();
                double facing = away < 1.0E-6 || length < 1.0E-9 ? 1.0 : Math.abs(out.dot(toEye)) / (length * away);
                alphas[i][j] = Colors.alpha(strength * Math.pow(facing, 1.6));
            }
        }
        for (int i = 0; i < rings; i++) {
            for (int j = 0; j < slices; j++) {
                Vec3 p0 = points[i][j];
                Vec3 p1 = points[i + 1][j];
                Vec3 p2 = points[i + 1][j + 1];
                Vec3 p3 = points[i][j + 1];
                this.put(this.glow, p0.x, p0.y, p0.z, rgb, alphas[i][j]);
                this.put(this.glow, p1.x, p1.y, p1.z, rgb, alphas[i + 1][j]);
                this.put(this.glow, p2.x, p2.y, p2.z, rgb, alphas[i + 1][j + 1]);
                this.put(this.glow, p3.x, p3.y, p3.z, rgb, alphas[i][j + 1]);
            }
        }
    }
}
