package nl.tivek.multiversepowers.creature.wizard.client.rift;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.rift.WizardPortal;
import nl.tivek.multiversepowers.engine.client.fx.Cosmos;

// A rift of the Wizard's (rift/WizardPortal): an oval window into a deep sky in its kind's colours (Cosmos), seen from
// whichever side the eye is on, its rim burning in its light with what marks its kind turning round it: rune ticks for
// the arcane, ragged spikes of dark for the void, a clock's twelve marks and a second ring for the golden, icicles for
// the frost, twinkling points for the starry, tongues of flame for the embers. It swirls open from a slit and closes
// the same way.
public final class PortalRenderer extends EntityRenderer<WizardPortal> {
    private static final int SIDES = 40;
    // An upright rift is this much narrower than it is tall.
    private static final float NARROW = 0.72F;

    public PortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(WizardPortal portal, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(WizardPortal portal, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float open = portal.openness(partialTick);
        if (open <= 0.001F) {
            return;
        }
        int style = portal.style();
        float age = portal.tickCount + partialTick;
        Vec3 center = portal.getPosition(partialTick);
        Vec3 facing = portal.facing();
        Vec3 side = portal.upright() ? new Vec3(facing.z, 0.0, -facing.x) : new Vec3(1.0, 0.0, 0.0);
        Vec3 up = portal.upright() ? new Vec3(0.0, 1.0, 0.0) : new Vec3(0.0, 0.0, 1.0);
        float radius = portal.radius();
        // Opening, it widens from a slit: tall at once, wide as it opens.
        float wide = radius * (portal.upright() ? NARROW : 1.0F) * open * open;
        float tall = radius * Math.min(1.0F, 0.3F + open);
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition();
        Vec3 normal = eye.subtract(center).dot(facing) >= 0.0 ? facing : facing.scale(-1.0);
        List<Vec3> outline = new ArrayList<>(SIDES);
        float[][] rim = new float[SIDES][];
        for (int i = 0; i < SIDES; i++) {
            double a = i * Mth.TWO_PI / SIDES;
            double ragged = 1.0 + 0.05 * Math.sin(5.0 * a + age * 0.13) + 0.03 * Math.sin(9.0 * a - age * 0.21);
            Vec3 at = side.scale(Math.cos(a) * wide * ragged).add(up.scale(Math.sin(a) * tall * ragged));
            outline.add(center.add(at));
            rim[i] = new float[] { (float) at.x, (float) at.y, (float) at.z };
        }
        Cosmos.window(center, normal, side, outline, 3.0, WizardPortal.deep(style), WizardPortal.glow(style),
                0.7 + 0.5 * open, portal.getId());
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        PoseStack.Pose at = pose.last();
        Vec3 lift = normal.scale(0.02);
        int colour = WizardPortal.rim(style);
        float[] none = Additive.rgb(0, 0.0F);
        for (int i = 0; i < SIDES; i++) {
            float[] p0 = rim[i];
            float[] p1 = rim[(i + 1) % SIDES];
            float flicker = 0.75F + 0.25F * Mth.sin(age * 0.7F + i * 1.9F);
            float[] white = Additive.rgb(0xFFFFFF, 0.85F * open * flicker);
            float[] lit = Additive.rgb(colour, 0.9F * open);
            float[] soft = Additive.rgb(colour, 0.6F * open * flicker);
            Additive.quad(at, buffer, scaled(p0, 0.93F, lift), scaled(p1, 0.93F, lift), scaled(p1, 1.04F, lift),
                    scaled(p0, 1.04F, lift), white, white, lit, lit);
            Additive.quad(at, buffer, scaled(p0, 1.04F, lift), scaled(p1, 1.04F, lift), scaled(p1, 1.4F, lift),
                    scaled(p0, 1.4F, lift), soft, soft, none, none);
        }
        this.marks(at, buffer, style, rim, age, open, colour, lift, side, up, wide, tall);
        super.render(portal, yaw, partialTick, pose, buffers, light);
    }

    // What marks each kind turning round its rim.
    private void marks(PoseStack.Pose at, VertexConsumer buffer, int style, float[][] rim, float age, float open,
            int colour, Vec3 lift, Vec3 side, Vec3 up, float wide, float tall) {
        int count = switch (style) {
            case WizardPortal.GOLDEN -> 12;
            case WizardPortal.VOID, WizardPortal.EMBER -> 14;
            default -> 10;
        };
        float turn = switch (style) {
            case WizardPortal.GOLDEN -> age * 0.02F;
            case WizardPortal.VOID -> -age * 0.04F;
            default -> age * 0.05F;
        };
        float[] bright = Additive.rgb(colour, 0.9F * open);
        float[] white = Additive.rgb(0xFFFFFF, 0.7F * open);
        float[] none = Additive.rgb(0, 0.0F);
        for (int k = 0; k < count; k++) {
            double a = turn + k * Mth.TWO_PI / count;
            Vec3 out = side.scale(Math.cos(a) * wide).add(up.scale(Math.sin(a) * tall));
            Vec3 across = side.scale(-Math.sin(a) * wide).add(up.scale(Math.cos(a) * tall)).normalize();
            Vec3 way = out.lengthSqr() < 1.0E-6 ? up : out.normalize();
            float twinkle = 0.5F + 0.5F * Mth.sin(age * 0.5F + k * 2.3F);
            float reach = switch (style) {
                case WizardPortal.VOID -> 0.35F + 0.25F * twinkle;
                case WizardPortal.FROST -> 0.3F + 0.08F * (k % 3);
                case WizardPortal.EMBER -> 0.2F + 0.25F * twinkle;
                case WizardPortal.STARRY -> 0.1F;
                default -> 0.16F;
            };
            float half = style == WizardPortal.STARRY ? 0.08F * (0.4F + twinkle) : 0.05F;
            Vec3 base = out.scale(1.06).add(lift);
            Vec3 tip = base.add(way.scale(reach));
            float[] b0 = point(base.add(across.scale(half)));
            float[] b1 = point(base.subtract(across.scale(half)));
            float[] t0 = point(tip.add(across.scale(style == WizardPortal.GOLDEN ? half : 0.0)));
            float[] t1 = point(tip.subtract(across.scale(style == WizardPortal.GOLDEN ? half : 0.0)));
            float[] tipColour = style == WizardPortal.VOID ? none : style == WizardPortal.STARRY ? white : bright;
            Additive.quad(at, buffer, b0, b1, t1, t0, bright, bright, tipColour, tipColour);
        }
        if (style == WizardPortal.GOLDEN) {
            // The clock's second ring, turning against the first.
            for (int i = 0; i < SIDES; i++) {
                float[] p0 = rim[i];
                float[] p1 = rim[(i + 1) % SIDES];
                float pulse = 0.5F + 0.5F * Mth.sin(-age * 0.3F + i * 0.6F);
                float[] gold = Additive.rgb(colour, 0.6F * open * pulse);
                Additive.quad(at, buffer, scaled(p0, 0.78F, lift), scaled(p1, 0.78F, lift), scaled(p1, 0.82F, lift),
                        scaled(p0, 0.82F, lift), gold, gold, gold, gold);
            }
        }
    }

    private static float[] scaled(float[] p, float by, Vec3 lift) {
        return new float[] { p[0] * by + (float) lift.x, p[1] * by + (float) lift.y, p[2] * by + (float) lift.z };
    }

    private static float[] point(Vec3 at) {
        return new float[] { (float) at.x, (float) at.y, (float) at.z };
    }

    @Override
    public ResourceLocation getTextureLocation(WizardPortal portal) {
        return Additive.ORB;
    }
}
