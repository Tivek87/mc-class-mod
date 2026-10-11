package nl.tivek.multiversepowers.engine.client.fx;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.opengl.GL11;

// A box of one of the game's own textures, unlit and drawn at once into the world as drawn so far, writing its depth:
// every face shows the same square of the texture, the faces turned from the eye a little darker. Outside the world's
// drawing none is drawn.
public final class TexturedBox {
    // Each face's corners, as indices into the box's corners (x the lowest bit, then y, then z), in the order the
    // square's corners run: (u0, v0), (u1, v0), (u1, v1), (u0, v1).
    private static final int[][] FACES = { { 0, 1, 3, 2 }, { 4, 5, 7, 6 }, { 0, 4, 6, 2 }, { 1, 5, 7, 3 },
            { 0, 1, 5, 4 }, { 2, 3, 7, 6 } };

    private TexturedBox() {
    }

    // The eight corners in world space, corner i at x = i & 1, y = (i >> 1) & 1, z = (i >> 2) & 1 of the box's own
    // axes; the square runs from (u0, v0) to (u1, v1), u along x and v along y on the faces square to z.
    public static void draw(ResourceLocation texture, float u0, float v0, float u1, float v1, Vec3[] corners) {
        if (!Cosmos.drawingLevel() || corners.length != 8) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Vec3 middle = Vec3.ZERO;
        for (Vec3 corner : corners) {
            middle = middle.add(corner.scale(0.125));
        }
        Vec3 toEye = camera.subtract(middle).normalize();
        float[] us = { u0, u1, u1, u0 };
        float[] vs = { v0, v0, v1, v1 };
        BufferBuilder quads = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS,
                DefaultVertexFormat.POSITION_TEX_COLOR);
        for (int[] face : FACES) {
            Vec3 center = Vec3.ZERO;
            for (int i : face) {
                center = center.add(corners[i].scale(0.25));
            }
            Vec3 out = center.subtract(middle);
            float shade = (float) (0.55 + 0.45 * Math.max(0.0, out.lengthSqr() < 1.0E-12 ? 0.0
                    : out.normalize().dot(toEye)));
            for (int k = 0; k < 4; k++) {
                Vec3 at = corners[face[k]].subtract(camera);
                quads.addVertex((float) at.x, (float) at.y, (float) at.z).setUv(us[k], vs[k])
                        .setColor(shade, shade, shade, 1.0F);
            }
        }
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(quads.buildOrThrow());
        RenderSystem.enableCull();
    }
}
