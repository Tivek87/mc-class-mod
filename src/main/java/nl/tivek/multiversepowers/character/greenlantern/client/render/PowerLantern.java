package nl.tivek.multiversepowers.character.greenlantern.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.body.suit.Ring;
import nl.tivek.multiversepowers.engine.client.render.StandaloneModel;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Noise;
import org.joml.Matrix4f;

// The Power Battery: the green lantern with a gold ring on top, gold and stone bands at both ends, ribbed green glass
// above and below its body, two gold hoops round its middle and the emblem on its front and back. A block model in the
// game's own style (models/greenlantern/lantern, built by scripts/models/lantern), half a model pixel to its pixel, held
// at the ring's top with y up. As it charges its glass lights from inside and crackles: two models of cracks of light
// over the glass's even light, each flaring in turn.
public final class PowerLantern {
    private static final StandaloneModel BODY = model("greenlantern/lantern");
    private static final StandaloneModel LIGHT = model("greenlantern/lantern_light");
    private static final StandaloneModel CRACKS_A = model("greenlantern/lantern_glass");
    private static final StandaloneModel CRACKS_B = model("greenlantern/lantern_glass_b");
    // Model pixels per pixel of the model, and the ring's top in its pixels.
    private static final float UNIT = 0.5F;
    private static final float TOP = 26.0F;
    private static final int LIT = 0x5CFF8E;
    private static final int BRIGHT = 0x9CFFB8;
    private static final int HOT = 0xF0FFF4;
    // Where the emblem's middle is, in model pixels: the ring is pressed to it to charge.
    public static final Vec3 EMBLEM = new Vec3(0.0, (9.0 - TOP) * UNIT, 5.5 * UNIT);
    // The haze round the glass, in model pixels.
    private static final Mesh GLOW = Mesh.lathe(16, 1.0, 0.0, -11.5, 2.4, -11.5, 2.6, -8.5, 2.4, -5.5, 0.0, -5.5);

    private PowerLantern() {
    }

    private static StandaloneModel model(String path) {
        return new StandaloneModel(ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, path));
    }

    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        BODY.register(event);
        LIGHT.register(event);
        CRACKS_A.register(event);
        CRACKS_B.register(event);
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float glow, float burst) {
        draw(poseStack, buffers, glow, burst, 0.0F, LightTexture.FULL_BRIGHT);
    }

    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float glow, float burst, int light) {
        draw(poseStack, buffers, glow, burst, 0.0F, light);
    }

    // `glow` 0 dark to 1 fully charged, past 1 a flash; `burst` a flash at the emblem; `hot` the metal lit up.
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, float glow, float burst, float hot,
            int light) {
        float heat = Mth.clamp(hot, 0.0F, 1.0F);
        float burn = Mth.clamp(glow, 0.0F, 1.0F);
        float flash = Mth.clamp(glow - 1.0F, 0.0F, 1.0F);
        double time = Util.getMillis() / 50.0;
        poseStack.pushPose();
        poseStack.scale(UNIT, UNIT, UNIT);
        poseStack.translate(-0.5F, -TOP / 16.0F, -0.5F);
        // Its own light keeps the lantern from going dark in the night as its glass lights up.
        int own = (int) (15 * Math.max(burn, heat));
        BODY.draw(poseStack, buffers, Math.max(light & 0xFFFF, own << 4) | (light & 0xFFFF0000));
        if (heat > 0.0F) {
            BODY.glow(poseStack, buffers, BRIGHT, 0.6F * heat);
        }
        LIGHT.glow(poseStack, buffers, Colors.mix(LIT, HOT, flash * 0.7F), 0.08F + 0.8F * burn + 0.12F * flash);
        float crackle = burn * (float) Math.sqrt(burn);
        CRACKS_A.glow(poseStack, buffers, HOT, crackle * flare(1, time) + flash);
        CRACKS_B.glow(poseStack, buffers, HOT, crackle * flare(2, time) + flash);
        poseStack.popPose();

        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer halo = buffers.getBuffer(Ring.HALO);
        float strength = Math.min(1.5F, 0.15F + 0.85F * burn + flash);
        haze(halo, matrix, 1.12, (int) (70 * strength));
        haze(halo, matrix, 1.45 + 0.5 * flash, (int) (28 * strength));
        float blast = Mth.clamp(burst, 0.0F, 1.0F);
        if (blast > 0.0F) {
            float r = 0.5F + 1.1F * blast;
            float z = (float) EMBLEM.z;
            float y = (float) EMBLEM.y;
            for (int k = 0; k < 3; k++) {
                float wide = r * (1.0F + 0.8F * k);
                square(halo, matrix, y, z + 0.05F * k, wide, (int) (110 * blast / (1 + k)));
            }
        }
    }

    // How bright one set of cracks burns now: mostly low, flaring up now and then, never both together for long.
    private static float flare(int seed, double time) {
        double wander = Noise.smooth(seed, time * 0.8);
        double snap = Noise.smooth(seed + 10, time * 3.1);
        return (float) Math.min(1.0, 0.1 + 1.2 * wander * wander * (0.55 + 0.45 * snap));
    }

    // The glass's glow: its shape again, made bigger, in see-through light.
    private static void haze(VertexConsumer buffer, Matrix4f matrix, double grow, int alpha) {
        if (alpha <= 0) {
            return;
        }
        double middle = -8.5;
        for (int[] side : GLOW.sides) {
            for (int corner : side) {
                Vec3 p = GLOW.points[corner];
                buffer.addVertex(matrix, (float) (p.x * grow / 16.0), (float) ((middle + (p.y - middle) * grow) / 16.0),
                        (float) (p.z * grow / 16.0)).setColor(0x3C, 0xE8, 0x6A, Math.min(255, alpha));
            }
        }
    }

    private static void square(VertexConsumer buffer, Matrix4f matrix, float y, float z, float half, int alpha) {
        float[][] corners = { { -half, y - half }, { half, y - half }, { half, y + half }, { -half, y + half } };
        for (float[] c : corners) {
            buffer.addVertex(matrix, c[0] / 16.0F, c[1] / 16.0F, z / 16.0F).setColor(0x3C, 0xE8, 0x6A,
                    Math.min(255, alpha));
        }
    }
}
