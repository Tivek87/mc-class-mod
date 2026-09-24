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

/**
 * The power ring on Green Lantern's right middle finger (the hand that attacks), made like the real one: a
 * silver band, a dark angular setting on the back of the hand, and a big round green stone in it with a light
 * glint on top. Band and setting are metal that catches the light like the rest of the body; the stone glows
 * by itself. All of it is solid: nothing of the ring is ever see-through, and its glow only ever lies around the
 * stone, never over it.
 *
 * <p>The stone's colour shows how much power is left: a full ring is bright green, an empty one dull and dark,
 * and one that is nearly empty sputters. While the ring works (see {@link SuitGlow}) its light flares out around
 * the stone, the harder the more it works.
 */
public final class Ring {
    /** The band and its setting: solid metal, lit like the body it sits on. */
    private static final ResourceLocation METAL = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "textures/entity/ring_metal.png");
    /** Unlit solid colour: the lantern is drawn with it. */
    public static final RenderType BAND = RenderType.create("welcomescreen_ring", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 2048, false, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    /**
     * Glow: added on top of what is behind it, like light. Drawn straight into the picture, so it also shows on
     * your own hand in first person (the constructs' glow layer does not reach that far).
     */
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
    // Below this much power the light sputters.
    private static final float LOW = 0.2F;
    private static final int FULL_BRIGHT = 0xF000F0;

    private Ring() {
    }

    /**
     * The middle of the stone, in blocks on the arm (see {@link #draw} for how the arm lies): where the ring's
     * light leaves it.
     */
    public static Vector3f stone(boolean slim) {
        return new Vector3f((surface(slim) - 0.8F) / 16.0F, 8.8F / 16.0F, -0.5F / 16.0F);
    }

    /** Where the uniform's glove lies on the back of the hand, in pixels along x. */
    private static float surface(boolean slim) {
        return (slim ? -2.0F : -3.0F) - 0.3F;
    }

    /**
     * Draws the ring on a right arm; the pose stack must already stand on that arm (its own pixels, like
     * the arm's box: x from -3 to 1, or from -2 for slim arms, the shoulder at y -2 and the fingertips at
     * y 10). The hand hangs with the thumb forward, at z -2, so the back of the hand is the outer side of
     * the arm, at the lowest x, and the fingers lie side by side along z.
     *
     * <p>It sits on top of the middle finger: on the back of the hand, the second finger from the thumb (z
     * around -0.5), close to the fingertips, on top of the uniform's glove. In first person that side faces up,
     * so you see the ring on top of your fist.
     *
     * @param light    the light at the arm, for the metal
     * @param slim     true for slim arms, which are a pixel narrower on the outer side
     * @param strength 0 = not there, 1 = fully there
     * @param charge   how full the ring is: 0 = empty, 1 = full
     * @param glow     how hard the ring works right now: 0 = resting, 1 = as hard as it goes
     */
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
        // The band across the back of the hand, just sunk into the glove so no gap shows under it.
        solid(metal, pose, g - 0.22F, 8.1F, -1.5F, g + 0.05F, 9.5F, 0.5F, SILVER, light);
        // The dark setting that holds the stone, with angular shoulders along the finger.
        solid(metal, pose, g - 0.6F, 8.28F, -1.02F, g - 0.22F, 9.32F, 0.02F, SETTING, light);
        solid(metal, pose, g - 0.46F, 7.98F, -0.82F, g - 0.22F, 8.28F, -0.18F, SILVER, light);
        solid(metal, pose, g - 0.46F, 9.32F, -0.82F, g - 0.22F, 9.62F, -0.18F, SILVER, light);
        // The stone itself: it glows by itself, brighter the fuller the ring and the harder it works.
        int stone = Colors.mix(Colors.mix(STONE_EMPTY, STONE_FULL, power), GLINT_FULL, 0.35F * burn);
        solid(metal, pose, g - 0.9F, 8.42F, -0.88F, g - 0.6F, 9.18F, -0.12F, stone, FULL_BRIGHT);
        // A thin silver rim around the top of the stone, and the light glint on it.
        solid(metal, pose, g - 0.7F, 8.34F, -0.96F, g - 0.6F, 8.42F, -0.04F, SILVER, light);
        solid(metal, pose, g - 0.7F, 9.18F, -0.96F, g - 0.6F, 9.26F, -0.04F, SILVER, light);
        solid(metal, pose, g - 0.97F, 8.56F, -0.66F, g - 0.9F, 8.8F, -0.42F,
                Colors.mix(GLINT_EMPTY, GLINT_FULL, power), FULL_BRIGHT);
        if (burn > 0.01F) {
            flare(buffers.getBuffer(HALO), pose.pose(), g - 0.98F, burn);
        }
    }

    /**
     * The ring's light flaring out around the stone while it works: a soft ring of light and a few rays, lying
     * just above the stone but leaving the stone itself clear, so it never washes it out.
     */
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
        // Rays: longer and turning slowly while the ring works hard.
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

    /**
     * A solid box between two corners, in pixels (16 to a block), for the entity shader: every side faces its own
     * way, so the game shades it like any other part of the body. {@code light} is the light it stands in (full
     * for the stone, which glows by itself).
     */
    private static void solid(VertexConsumer buffer, PoseStack.Pose pose, float x0, float y0, float z0, float x1,
            float y1, float z1, int rgb, int light) {
        float[] x = { x0 / 16, x1 / 16 };
        float[] y = { y0 / 16, y1 / 16 };
        float[] z = { z0 / 16, z1 / 16 };
        // The four corners of each side (counter-clockwise from outside) and the way the side faces; a corner is
        // three bits: 1 = far x, 2 = far y, 4 = far z.
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
