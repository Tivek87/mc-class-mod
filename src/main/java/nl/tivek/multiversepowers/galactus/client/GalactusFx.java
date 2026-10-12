package nl.tivek.multiversepowers.galactus.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// The light round Galactus, drawn added onto the sky in its coordinates (the eye in the middle): soft discs for his
// aura and the glow in his hand, rings and flashes for the struck moon, a star for the speck he is far off, and
// crackling lightning, jagged bolts that branch and flicker.
final class GalactusFx {
    static final int VIOLET = 0x8A3CFF;
    static final int TEAL = 0x3FD8E8;
    static final int SPARK = 0xE4F4FF;
    static final int BOLT = 0xB78CFF;

    private GalactusFx() {
    }

    static void additive() {
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
    }

    // A soft disc facing the eye at `at`, `rgb` at its middle fading to nothing at its edge.
    static void disc(Matrix4f base, Vec3 at, double radius, int rgb, float alpha) {
        if (alpha <= 0.002F) {
            return;
        }
        Vec3[] face = facing(at);
        BufferBuilder fan = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        int sides = 40;
        for (int i = 0; i < sides; i++) {
            Vec3 a = around(at, face, radius, i, sides);
            Vec3 b = around(at, face, radius, i + 1, sides);
            fan.addVertex(base, (float) at.x, (float) at.y, (float) at.z).setColor(color(rgb, alpha));
            fan.addVertex(base, (float) a.x, (float) a.y, (float) a.z).setColor(color(rgb, 0.0F));
            fan.addVertex(base, (float) b.x, (float) b.y, (float) b.z).setColor(color(rgb, 0.0F));
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(fan.buildOrThrow());
    }

    // A thin ring facing the eye, brightest along its line.
    static void ring(Matrix4f base, Vec3 at, double radius, double width, int rgb, float alpha) {
        if (alpha <= 0.002F) {
            return;
        }
        Vec3[] face = facing(at);
        BufferBuilder band = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        int sides = 64;
        for (int i = 0; i < sides; i++) {
            for (int half = -1; half <= 1; half += 2) {
                Vec3 a = around(at, face, radius, i, sides);
                Vec3 b = around(at, face, radius, i + 1, sides);
                Vec3 c = around(at, face, radius + half * width, i + 1, sides);
                Vec3 d = around(at, face, radius + half * width, i, sides);
                band.addVertex(base, (float) a.x, (float) a.y, (float) a.z).setColor(color(rgb, alpha));
                band.addVertex(base, (float) b.x, (float) b.y, (float) b.z).setColor(color(rgb, alpha));
                band.addVertex(base, (float) c.x, (float) c.y, (float) c.z).setColor(color(rgb, 0.0F));
                band.addVertex(base, (float) d.x, (float) d.y, (float) d.z).setColor(color(rgb, 0.0F));
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(band.buildOrThrow());
    }

    // A four-pointed star of light facing the eye.
    static void star(Matrix4f base, Vec3 at, double size, int rgb, float alpha) {
        if (alpha <= 0.002F) {
            return;
        }
        Vec3[] face = facing(at);
        BufferBuilder rays = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR);
        for (int k = 0; k < 4; k++) {
            double angle = k * Math.PI / 2.0;
            Vec3 out = face[0].scale(Math.cos(angle)).add(face[1].scale(Math.sin(angle)));
            Vec3 side = face[0].scale(-Math.sin(angle)).add(face[1].scale(Math.cos(angle)));
            Vec3 point = at.add(out.scale(size * 3.0));
            Vec3 a = at.add(side.scale(size * 0.3));
            Vec3 b = at.subtract(side.scale(size * 0.3));
            rays.addVertex(base, (float) a.x, (float) a.y, (float) a.z).setColor(color(rgb, alpha));
            rays.addVertex(base, (float) b.x, (float) b.y, (float) b.z).setColor(color(rgb, alpha));
            rays.addVertex(base, (float) point.x, (float) point.y, (float) point.z).setColor(color(rgb, 0.0F));
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(rays.buildOrThrow());
        disc(base, at, size * 1.2, 0xFFFFFF, alpha);
    }

    // Lightning from `from` to `to`: a jagged path that forks, a white core inside a violet glow, `width` across.
    static void bolt(Matrix4f base, Vec3 from, Vec3 to, double width, float alpha, Random random) {
        if (alpha <= 0.002F) {
            return;
        }
        List<Vec3[]> strokes = new ArrayList<>();
        List<Vec3> path = new ArrayList<>();
        path.add(from);
        path.add(to);
        double length = to.subtract(from).length();
        double jag = 0.22;
        for (int level = 0; level < 5; level++) {
            List<Vec3> finer = new ArrayList<>();
            for (int i = 0; i + 1 < path.size(); i++) {
                Vec3 a = path.get(i);
                Vec3 b = path.get(i + 1);
                Vec3 middle = a.add(b).scale(0.5).add(jitter(random, b.subtract(a), length * jag));
                finer.add(a);
                finer.add(middle);
                if (level == 2 && random.nextFloat() < 0.3F) {
                    Vec3 way = b.subtract(a);
                    Vec3 end = middle.add(way.scale(0.8 + random.nextDouble()))
                            .add(jitter(random, way, length * 0.25));
                    strokes.add(new Vec3[] { middle, end });
                }
            }
            finer.add(path.get(path.size() - 1));
            path = finer;
            jag *= 0.55;
        }
        ribbon(base, path, width * 4.0, BOLT, alpha * 0.35F);
        ribbon(base, path, width, SPARK, alpha);
        for (Vec3[] fork : strokes) {
            List<Vec3> branch = new ArrayList<>();
            branch.add(fork[0]);
            Vec3 step = fork[1].subtract(fork[0]);
            for (int i = 1; i <= 6; i++) {
                branch.add(fork[0].add(step.scale(i / 6.0)).add(jitter(random, step, step.length() * 0.12)));
            }
            ribbon(base, branch, width * 2.5, BOLT, alpha * 0.25F);
            ribbon(base, branch, width * 0.6, SPARK, alpha * 0.7F);
        }
    }

    // A band along `path` facing the eye, its colour at the middle line fading out to both edges.
    private static void ribbon(Matrix4f base, List<Vec3> path, double width, int rgb, float alpha) {
        BufferBuilder band = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_COLOR);
        for (int i = 0; i + 1 < path.size(); i++) {
            Vec3 a = path.get(i);
            Vec3 b = path.get(i + 1);
            Vec3 side = b.subtract(a).cross(a.add(b));
            if (side.lengthSqr() < 1.0E-12) {
                continue;
            }
            side = side.normalize().scale(width * 0.5);
            for (int half = -1; half <= 1; half += 2) {
                Vec3 c = b.add(side.scale(half));
                Vec3 d = a.add(side.scale(half));
                band.addVertex(base, (float) a.x, (float) a.y, (float) a.z).setColor(color(rgb, alpha));
                band.addVertex(base, (float) b.x, (float) b.y, (float) b.z).setColor(color(rgb, alpha));
                band.addVertex(base, (float) c.x, (float) c.y, (float) c.z).setColor(color(rgb, 0.0F));
                band.addVertex(base, (float) d.x, (float) d.y, (float) d.z).setColor(color(rgb, 0.0F));
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(band.buildOrThrow());
    }

    // A random step square to `way`, up to `size` long.
    private static Vec3 jitter(Random random, Vec3 way, double size) {
        Vec3 n = way.normalize();
        Vec3 a = Math.abs(n.y) < 0.9 ? n.cross(new Vec3(0, 1, 0)).normalize() : n.cross(new Vec3(1, 0, 0)).normalize();
        Vec3 b = n.cross(a);
        double angle = random.nextDouble() * Math.PI * 2.0;
        double r = (random.nextDouble() - 0.5) * 2.0 * size;
        return a.scale(Math.cos(angle) * r).add(b.scale(Math.sin(angle) * r));
    }

    // Two axes square to the way from the eye to `at`.
    static Vec3[] facing(Vec3 at) {
        Vec3 view = at.normalize();
        Vec3 a = view.cross(new Vec3(0.0, 1.0, 0.0));
        a = a.lengthSqr() < 1.0E-4 ? new Vec3(1.0, 0.0, 0.0) : a.normalize();
        return new Vec3[] { a, a.cross(view) };
    }

    private static Vec3 around(Vec3 at, Vec3[] face, double radius, int i, int sides) {
        double angle = Math.PI * 2.0 * i / sides;
        return at.add(face[0].scale(Math.cos(angle) * radius)).add(face[1].scale(Math.sin(angle) * radius));
    }

    private static int color(int rgb, float alpha) {
        return Mth.clamp(Math.round(alpha * 255.0F), 0, 255) << 24 | rgb & 0xFFFFFF;
    }
}
