package nl.tivek.multiversepowers.engine.client.fx;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import org.joml.Quaternionf;

// Living energy painted by a shader of its own, added to the picture as light: a quad is given a pattern and a colour,
// and the shader paints that pattern moving on it, with no texture. Round patterns fill a square (turned to the eye or
// lying flat), the two bands run from one point to another.
public final class Energy {
    public static final int FLAME = 0;
    public static final int FROST = 1;
    public static final int SPARK = 2;
    public static final int GUST = 3;
    public static final int DUST = 4;
    public static final int STREAM = 5;
    public static final int BOLT = 6;
    public static final int RING = 7;
    @Nullable
    private static ShaderInstance shader;
    private static final RenderType TYPE = RenderType.create("welcomescreen_energy", DefaultVertexFormat.PARTICLE,
            VertexFormat.Mode.QUADS, 1536, false, true, RenderType.CompositeState.builder()
                    .setShaderState(new RenderStateShard.ShaderStateShard(() -> shader))
                    .setTransparencyState(RenderStateShard.ADDITIVE_TRANSPARENCY)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));

    private Energy() {
    }

    // Null while the shader could not be built: the caller then leaves its energy out.
    @Nullable
    public static VertexConsumer buffer(MultiBufferSource buffers) {
        return shader == null ? null : buffers.getBuffer(TYPE);
    }

    // A square of `pattern` round `at`, `half` across, turned to the eye.
    public static void puff(PoseStack pose, VertexConsumer buffer, Vec3 at, float half, Quaternionf facing, int pattern,
            int colour, float strength, int seed) {
        pose.pushPose();
        pose.translate(at.x, at.y, at.z);
        pose.mulPose(facing);
        PoseStack.Pose last = pose.last();
        vertex(last, buffer, -half, -half, 0.0F, -1.0F, -1.0F, pattern, colour, strength, seed);
        vertex(last, buffer, half, -half, 0.0F, 1.0F, -1.0F, pattern, colour, strength, seed);
        vertex(last, buffer, half, half, 0.0F, 1.0F, 1.0F, pattern, colour, strength, seed);
        vertex(last, buffer, -half, half, 0.0F, -1.0F, 1.0F, pattern, colour, strength, seed);
        pose.popPose();
    }

    // The same lying flat round `at`, seen from above and below.
    public static void flat(PoseStack.Pose pose, VertexConsumer buffer, Vec3 at, float half, int pattern, int colour,
            float strength, int seed) {
        float x = (float) at.x;
        float y = (float) at.y;
        float z = (float) at.z;
        vertex(pose, buffer, x - half, y, z - half, -1.0F, -1.0F, pattern, colour, strength, seed);
        vertex(pose, buffer, x + half, y, z - half, 1.0F, -1.0F, pattern, colour, strength, seed);
        vertex(pose, buffer, x + half, y, z + half, 1.0F, 1.0F, pattern, colour, strength, seed);
        vertex(pose, buffer, x - half, y, z + half, -1.0F, 1.0F, pattern, colour, strength, seed);
    }

    // A band of STREAM or BOLT from `from` to `to`, `half` to either side, its face turned to `eye`.
    public static void band(PoseStack.Pose pose, VertexConsumer buffer, Vec3 from, Vec3 to, Vec3 eye, float half,
            int pattern, int colour, float strength, int seed) {
        Vec3 way = to.subtract(from);
        double length = way.length();
        Vec3 side = way.cross(eye.subtract(from.add(way.scale(0.5))));
        if (length < 1.0E-3 || side.lengthSqr() < 1.0E-8) {
            return;
        }
        side = side.normalize().scale(half);
        float along = (float) (length / half * 0.5);
        vertex(pose, buffer, from.add(side), 0.0F, 1.0F, pattern, colour, strength, seed);
        vertex(pose, buffer, to.add(side), along, 1.0F, pattern, colour, strength, seed);
        vertex(pose, buffer, to.subtract(side), along, -1.0F, pattern, colour, strength, seed);
        vertex(pose, buffer, from.subtract(side), 0.0F, -1.0F, pattern, colour, strength, seed);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, Vec3 at, float u, float v, int pattern,
            int colour, float strength, int seed) {
        vertex(pose, buffer, (float) at.x, (float) at.y, (float) at.z, u, v, pattern, colour, strength, seed);
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer buffer, float x, float y, float z, float u, float v,
            int pattern, int colour, float strength, int seed) {
        buffer.addVertex(pose, x, y, z).setUv(u, v).setColor(colour >> 16 & 255, colour >> 8 & 255, colour & 255,
                Math.round(Mth.clamp(strength, 0.0F, 1.0F) * 255.0F)).setUv2(pattern, seed & 255);
    }

    public static void onRegisterShaders(RegisterShadersEvent event) {
        // A shader that will not build only leaves the energy out, never the game.
        try {
            ResourceLocation name = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "energy");
            event.registerShader(new ShaderInstance(event.getResourceProvider(), name, DefaultVertexFormat.PARTICLE),
                    loaded -> shader = loaded);
        } catch (Exception e) {
            shader = null;
            MultiversePowers.LOGGER.error("The energy shader could not be built", e);
        }
    }
}
