package nl.tivek.multiversepowers.galactus.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Galactus's hulls as buffers kept on the graphics card, one per bone, made once and posed by matrices each frame, and
// the shader that lights them face by face: a key light (the moon's, or the sun's by day), the cosmos's violet and teal
// on the faces turned to it, the cracks of energy pulsing over his plates and what glows (eyes, emblem).
final class GalactusMesh {
    // The lights, every direction in view space (towards the light).
    record Lights(Vector3f key, Vector3f keyColor, Vector3f ambient, Vector3f rimA, Vector3f rimAColor, Vector3f rimB,
            Vector3f rimBColor) {
    }

    @Nullable
    private static ShaderInstance shader;
    private final VertexBuffer[] buffers = new VertexBuffer[GalactusRig.COUNT];
    private boolean made;

    static void register(RegisterShadersEvent event) {
        // A shader that will not build leaves him undrawn, never the game.
        try {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "galactus");
            event.registerShader(new ShaderInstance(event.getResourceProvider(), name,
                    DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL), loaded -> shader = loaded);
        } catch (Exception e) {
            shader = null;
            MultiversePowers.LOGGER.error("The galactus shader could not be built", e);
        }
    }

    // Every bone drawn in its posed frame (`body` takes model pixels to the view), as solid as `alpha`; `clock` runs
    // the pulses, `energy` lights the cracks all over him and `charge` his reaching right arm the more.
    void draw(GalactusRig rig, Matrix4f body, Matrix4f projection, Lights lights, float clock, float energy,
            float charge, float alpha) {
        ShaderInstance lit = shader;
        if (lit == null) {
            return;
        }
        if (!this.made) {
            this.make(rig);
        }
        set(lit, "KeyDir", lights.key());
        set(lit, "KeyColor", lights.keyColor());
        set(lit, "Ambient", lights.ambient());
        set(lit, "RimDirA", lights.rimA());
        set(lit, "RimColorA", lights.rimAColor());
        set(lit, "RimDirB", lights.rimB());
        set(lit, "RimColorB", lights.rimBColor());
        lit.safeGetUniform("Clock").set(clock);
        lit.safeGetUniform("Energy").set(energy);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        Matrix4f pose = new Matrix4f();
        for (int b = 0; b < GalactusRig.COUNT; b++) {
            VertexBuffer buffer = this.buffers[b];
            if (buffer == null) {
                continue;
            }
            lit.safeGetUniform("Charge").set(reaching(b) ? charge : 0.0F);
            buffer.bind();
            buffer.drawWithShader(pose.set(body).mul(rig.world[b]), projection, lit);
        }
        VertexBuffer.unbind();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static boolean reaching(int bone) {
        return bone == GalactusRig.FORE + GalactusRig.RIGHT || bone == GalactusRig.HAND + GalactusRig.RIGHT
                || bone >= GalactusRig.DIGITS && bone < GalactusRig.DIGITS + 15;
    }

    private static void set(ShaderInstance lit, String name, Vector3f value) {
        lit.safeGetUniform(name).set(value.x, value.y, value.z);
    }

    private void make(GalactusRig rig) {
        this.made = true;
        GalactusShape shape = new GalactusShape(rig);
        for (int b = 0; b < GalactusRig.COUNT; b++) {
            this.buffers[b] = build(shape.hulls[b]);
        }
    }

    @Nullable
    private static VertexBuffer build(GalactusHull hull) {
        if (hull.vertices() == 0) {
            return null;
        }
        ByteBufferBuilder memory = new ByteBufferBuilder(hull.vertices() * 28 + 256);
        BufferBuilder buffer = new BufferBuilder(memory, VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_TEX_COLOR_NORMAL);
        float[] d = hull.data();
        for (int i = 0; i < hull.vertices() * GalactusHull.STRIDE; i += GalactusHull.STRIDE) {
            buffer.addVertex(d[i], d[i + 1], d[i + 2]).setUv(d[i + 10], d[i + 11])
                    .setColor(d[i + 6], d[i + 7], d[i + 8], d[i + 9]).setNormal(d[i + 3], d[i + 4], d[i + 5]);
        }
        MeshData mesh = buffer.build();
        VertexBuffer made = null;
        if (mesh != null) {
            made = new VertexBuffer(VertexBuffer.Usage.STATIC);
            made.bind();
            made.upload(mesh);
            VertexBuffer.unbind();
        }
        memory.close();
        return made;
    }
}
