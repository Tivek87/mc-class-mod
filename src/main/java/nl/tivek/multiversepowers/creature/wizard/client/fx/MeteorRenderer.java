package nl.tivek.multiversepowers.creature.wizard.client.fx;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.spell.GreatMeteor;
import org.joml.Vector3f;

// The great meteor: a lumpy rock of dark basalt, tumbling, glowing veins of fire across it and a burning halo round it,
// and below on the ground a circle of runes in fire where it will land, a ring inside it closing in as it falls and a
// faint shaft of its light between them.
public final class MeteorRenderer extends EntityRenderer<GreatMeteor> {
    private static final RenderType ROCK = RenderType.entitySolid(Additive.ORB);
    private static final float SIZE = 1.25F;
    private static final int BASALT = 0x2E2320;
    // The rock, built once: its corners (unit sphere pushed in and out) and its triangles.
    private static final List<Vector3f> CORNERS = new ArrayList<>();
    private static final List<int[]> FACES = new ArrayList<>();

    static {
        build();
    }

    public MeteorRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(GreatMeteor meteor, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(GreatMeteor meteor, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = meteor.tickCount + partialTick;
        Vec3 origin = meteor.getPosition(partialTick);
        float fallen = meteor.fallen(partialTick);
        this.marker(pose, buffers.getBuffer(Additive.TYPE), meteor.target().subtract(origin), age, fallen);
        pose.pushPose();
        pose.translate(0.0F, meteor.getBbHeight() * 0.5F, 0.0F);
        pose.mulPose(Axis.YP.rotation(age * 0.05F));
        pose.mulPose(Axis.XP.rotation(age * 0.08F));
        pose.scale(SIZE, SIZE, SIZE);
        this.rock(pose.last(), buffers.getBuffer(ROCK), 1.0F, false, age);
        this.rock(pose.last(), buffers.getBuffer(Additive.TYPE), 1.02F, true, age);
        pose.popPose();
        var facing = this.entityRenderDispatcher.cameraOrientation();
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        Vec3 middle = new Vec3(0.0, meteor.getBbHeight() * 0.5, 0.0);
        float flicker = 0.85F + 0.15F * Mth.sin(age * 1.3F);
        Additive.glow(pose, glow, middle, 2.6F * flicker, Additive.rgb(Wizard.FIRE, 0.7F), facing);
        Additive.glow(pose, glow, middle, 4.5F, Additive.rgb(0xFF4A1A, 0.35F), facing);
        Vec3 trail = meteor.getDeltaMovement().normalize().scale(-1.0);
        for (int k = 1; k <= 5; k++) {
            Additive.glow(pose, glow, middle.add(trail.scale(k * 1.1)), 2.2F - k * 0.3F,
                    Additive.rgb(Wizard.FIRE, 0.5F - k * 0.08F), facing);
        }
    }

    // The circle on the ground where it will land.
    private void marker(PoseStack pose, VertexConsumer buffer, Vec3 ground, float age, float fallen) {
        pose.pushPose();
        pose.translate(ground.x, ground.y, ground.z);
        PoseStack.Pose at = pose.last();
        float pulse = 0.75F + 0.25F * Mth.sin(age * (0.3F + fallen * 0.9F));
        float radius = (float) GreatMeteor.REACH;
        Additive.glyph(at, buffer, radius, age, Wizard.FIRE, 0.8F * pulse);
        float closing = radius * Math.max(0.05F, 1.0F - fallen);
        Additive.ring(at, buffer, closing * 0.95F, closing, 0.05F, 48, Additive.rgb(0xFF3A1A, pulse),
                Additive.rgb(0xFF3A1A, pulse));
        Additive.ring(at, buffer, 0.0F, closing, 0.04F, 40, Additive.rgb(0xFF3A1A, 0.12F * pulse),
                Additive.rgb(0xFF3A1A, 0.05F * pulse));
        pose.popPose();
    }

    // The rock: dark basalt, or (`veins`) only its glowing cracks a hair larger over it.
    private void rock(PoseStack.Pose at, VertexConsumer buffer, float scale, boolean veins, float age) {
        Vector3f light = new Vector3f(0.4F, 0.8F, 0.3F).normalize();
        for (int[] face : FACES) {
            Vector3f a = CORNERS.get(face[0]);
            Vector3f b = CORNERS.get(face[1]);
            Vector3f c = CORNERS.get(face[2]);
            Vector3f normal = new Vector3f(b).sub(a).cross(new Vector3f(c).sub(a)).normalize();
            float[][] colours = new float[3][];
            for (int k = 0; k < 3; k++) {
                Vector3f p = CORNERS.get(face[k]);
                if (veins) {
                    float crack = vein(p, age);
                    colours[k] = Additive.rgb(Wizard.FIRE, crack);
                } else {
                    float shade = 0.45F + 0.55F * Math.max(0.0F, normal.dot(light));
                    colours[k] = Additive.rgb(BASALT, shade * 1.6F);
                }
            }
            vertex(at, buffer, a, scale, normal, colours[0]);
            vertex(at, buffer, b, scale, normal, colours[1]);
            vertex(at, buffer, c, scale, normal, colours[2]);
            vertex(at, buffer, c, scale, normal, colours[2]);
        }
    }

    // How bright a crack glows at a point of the rock: thin bright lines where two waves cross, breathing.
    private static float vein(Vector3f p, float age) {
        float w = Mth.sin(p.x * 7.0F + p.y * 3.0F) + Mth.sin(p.z * 6.0F - p.y * 5.0F + 1.3F);
        float line = Mth.clamp(1.0F - Math.abs(w) * 1.6F, 0.0F, 1.0F);
        return line * (0.7F + 0.3F * Mth.sin(age * 0.4F + p.x * 4.0F));
    }

    private static void vertex(PoseStack.Pose at, VertexConsumer buffer, Vector3f p, float scale, Vector3f normal,
            float[] colour) {
        buffer.addVertex(at.pose(), p.x * scale, p.y * scale, p.z * scale)
                .setColor(Math.min(1.0F, colour[0]), Math.min(1.0F, colour[1]), Math.min(1.0F, colour[2]), 1.0F)
                .setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT)
                .setNormal(at, normal.x, normal.y, normal.z);
    }

    // A twice-split icosahedron pushed in and out into a lump of rock.
    private static void build() {
        float t = (1.0F + Mth.sqrt(5.0F)) / 2.0F;
        float[][] ico = { { -1, t, 0 }, { 1, t, 0 }, { -1, -t, 0 }, { 1, -t, 0 }, { 0, -1, t }, { 0, 1, t },
                { 0, -1, -t }, { 0, 1, -t }, { t, 0, -1 }, { t, 0, 1 }, { -t, 0, -1 }, { -t, 0, 1 } };
        for (float[] p : ico) {
            CORNERS.add(new Vector3f(p[0], p[1], p[2]).normalize());
        }
        int[][] faces = { { 0, 11, 5 }, { 0, 5, 1 }, { 0, 1, 7 }, { 0, 7, 10 }, { 0, 10, 11 }, { 1, 5, 9 },
                { 5, 11, 4 }, { 11, 10, 2 }, { 10, 7, 6 }, { 7, 1, 8 }, { 3, 9, 4 }, { 3, 4, 2 }, { 3, 2, 6 },
                { 3, 6, 8 }, { 3, 8, 9 }, { 4, 9, 5 }, { 2, 4, 11 }, { 6, 2, 10 }, { 8, 6, 7 }, { 9, 8, 1 } };
        List<int[]> now = new ArrayList<>(List.of(faces));
        for (int split = 0; split < 2; split++) {
            Map<Long, Integer> middles = new HashMap<>();
            List<int[]> next = new ArrayList<>();
            for (int[] f : now) {
                int ab = middle(f[0], f[1], middles);
                int bc = middle(f[1], f[2], middles);
                int ca = middle(f[2], f[0], middles);
                next.add(new int[] { f[0], ab, ca });
                next.add(new int[] { f[1], bc, ab });
                next.add(new int[] { f[2], ca, bc });
                next.add(new int[] { ab, bc, ca });
            }
            now = next;
        }
        FACES.addAll(now);
        for (Vector3f p : CORNERS) {
            float lump = 1.0F + 0.16F * Mth.sin(p.x * 3.1F + 0.7F) * Mth.cos(p.z * 2.7F)
                    + 0.09F * Mth.sin(p.y * 5.3F + p.x * 2.0F);
            p.mul(lump);
        }
    }

    private static int middle(int a, int b, Map<Long, Integer> middles) {
        long key = Math.min(a, b) * 100000L + Math.max(a, b);
        Integer known = middles.get(key);
        if (known != null) {
            return known;
        }
        Vector3f m = new Vector3f(CORNERS.get(a)).add(CORNERS.get(b)).normalize();
        CORNERS.add(m);
        middles.put(key, CORNERS.size() - 1);
        return CORNERS.size() - 1;
    }

    @Override
    public ResourceLocation getTextureLocation(GreatMeteor meteor) {
        return Additive.ORB;
    }
}
