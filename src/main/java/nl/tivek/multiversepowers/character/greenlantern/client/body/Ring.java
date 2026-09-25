package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.math.Colors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Ring {
    private static final ResourceLocation METAL = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/ring_metal.png");
    public static final RenderType BAND = RenderType.create("welcomescreen_ring", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 2048, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    public static final RenderType HALO = RenderType.create("welcomescreen_ring_halo",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(RenderStateShard.LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final int SILVER = 0xD2DBD6;
    private static final int SETTING = 0x4B5652;
    private static final int STONE_FULL = 0x2EF566;
    private static final int STONE_EMPTY = 0x143A20;
    private static final int GLINT_FULL = 0xE6FFEC;
    private static final int GLINT_EMPTY = 0x3F6A4C;
    private static final int FLARE = 0x5CFF86;
    private static final float LOW = 0.2F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private Ring() {
    }

    public static Vector3f stone(boolean slim) {
        return new Vector3f((surface(slim) - 0.8F) / 16.0F, 8.8F / 16.0F, -0.5F / 16.0F);
    }

    private static float surface(boolean slim) {
        return (slim ? -2.0F : -3.0F) - 0.3F;
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, boolean slim, float strength,
            float charge, float glow) {
        if (strength <= 0.0F) {
            return;
        }
        float power = Mth.clamp(charge, 0.0F, 1.0F);
        if (power > 0.0F && power < LOW) {
            float time = (Util.getMillis() % 100000L) / 50.0F;
            power *= 0.6F + 0.4F * Math.abs(Mth.sin(time * 0.9F) * Mth.sin(time * 0.37F + 1.0F));
        }
        float burn = Mth.clamp(glow, 0.0F, 1.0F) * power;
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer metal = buffers.getBuffer(RenderType.entitySolid(METAL));
        float g = surface(slim);
        solid(metal, pose, g - 0.22F, 8.1F, -1.5F, g + 0.05F, 9.5F, 0.5F, SILVER, light);
        solid(metal, pose, g - 0.6F, 8.28F, -1.02F, g - 0.22F, 9.32F, 0.02F, SETTING, light);
        solid(metal, pose, g - 0.46F, 7.98F, -0.82F, g - 0.22F, 8.28F, -0.18F, SILVER, light);
        solid(metal, pose, g - 0.46F, 9.32F, -0.82F, g - 0.22F, 9.62F, -0.18F, SILVER, light);
        int stone = Colors.mix(Colors.mix(STONE_EMPTY, STONE_FULL, power), GLINT_FULL, 0.35F * burn);
        solid(metal, pose, g - 0.9F, 8.42F, -0.88F, g - 0.6F, 9.18F, -0.12F, stone, FULL_BRIGHT);
        solid(metal, pose, g - 0.7F, 8.34F, -0.96F, g - 0.6F, 8.42F, -0.04F, SILVER, light);
        solid(metal, pose, g - 0.7F, 9.18F, -0.96F, g - 0.6F, 9.26F, -0.04F, SILVER, light);
        solid(metal, pose, g - 0.97F, 8.56F, -0.66F, g - 0.9F, 8.8F, -0.42F,
                Colors.mix(GLINT_EMPTY, GLINT_FULL, power), FULL_BRIGHT);
        if (burn > 0.01F) {
            flare(buffers.getBuffer(HALO), pose.pose(), g - 0.98F, burn);
        }
    }

    private static void flare(VertexConsumer glow, Matrix4f matrix, float x, float burn) {
        float cy = 8.8F;
        float cz = -0.5F;
        float inner = 0.42F;
        float outer = inner + 0.5F + 2.4F * burn;
        int sides = 20;
        float spin = (Util.getMillis() % 100000L) / 1000.0F;
        for (int i = 0; i < sides; i++) {
            float a0 = Mth.TWO_PI * i / sides;
            float a1 = Mth.TWO_PI * (i + 1) / sides;
            vertex(glow, matrix, x, cy + Mth.sin(a0) * inner, cz + Mth.cos(a0) * inner, FLARE, 0.85F * burn);
            vertex(glow, matrix, x, cy + Mth.sin(a0) * outer, cz + Mth.cos(a0) * outer, FLARE, 0.0F);
            vertex(glow, matrix, x, cy + Mth.sin(a1) * outer, cz + Mth.cos(a1) * outer, FLARE, 0.0F);
            vertex(glow, matrix, x, cy + Mth.sin(a1) * inner, cz + Mth.cos(a1) * inner, FLARE, 0.85F * burn);
        }
        int rays = 6;
        for (int i = 0; i < rays; i++) {
            float a = spin * 0.8F + Mth.TWO_PI * i / rays;
            float length = inner + 1.0F + 3.8F * burn * (0.7F + 0.3F * Mth.sin(spin * 3.0F + i * 1.7F));
            float width = 0.16F;
            float dy = Mth.sin(a);
            float dz = Mth.cos(a);
            float sy = -dz * width;
            float sz = dy * width;
            vertex(glow, matrix, x, cy + dy * inner + sy, cz + dz * inner + sz, FLARE, 0.9F * burn);
            vertex(glow, matrix, x, cy + dy * length, cz + dz * length, FLARE, 0.0F);
            vertex(glow, matrix, x, cy + dy * length, cz + dz * length, FLARE, 0.0F);
            vertex(glow, matrix, x, cy + dy * inner - sy, cz + dz * inner - sz, FLARE, 0.9F * burn);
        }
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, int rgb,
            float alpha) {
        buffer.addVertex(matrix, x / 16.0F, y / 16.0F, z / 16.0F).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF,
                rgb & 0xFF, (int) (255 * Mth.clamp(alpha, 0.0F, 1.0F)));
    }

    private static void solid(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float z0, float x1,
            float y1, float z1, int rgb, int light) {
        float[] x = { x0 / 16, x1 / 16 };
        float[] y = { y0 / 16, y1 / 16 };
        float[] z = { z0 / 16, z1 / 16 };
        // corner bits: 1 = far x, 2 = far y, 4 = far z; picks into x[]/y[]/z[] above
        int[][] sides = { { 0, 4, 6, 2 }, { 1, 3, 7, 5 }, { 0, 1, 5, 4 }, { 2, 6, 7, 3 }, { 0, 2, 3, 1 },
                { 4, 5, 7, 6 } };
        float[][] normals = { { -1, 0, 0 }, { 1, 0, 0 }, { 0, -1, 0 }, { 0, 1, 0 }, { 0, 0, -1 }, { 0, 0, 1 } };
        for (int s = 0; s < sides.length; s++) {
            for (int corner : sides[s]) {
                buffer.addVertex(pose, x[corner & 1], y[corner >> 1 & 1], z[corner >> 2 & 1])
                        .setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, 255)
                        .setUv((corner & 1) == 0 ? 0.0F : 1.0F, (corner >> 1 & 1) == 0 ? 0.0F : 1.0F)
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                        .setNormal(pose, normals[s][0], normals[s][1], normals[s][2]);
            }
        }
    }
}
