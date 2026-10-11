package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.field.ArcaneTornado;

// The arcane tornado: a funnel of violet wind, narrow at the ground and wide at the top, its bands whirling round
// faster low down and its body swaying as it roams; sheets of pale wind wind up round it, streaks of light race up its
// sides, and a ring of dust whirls where it touches the ground. It gathers out of a whirl and dies down the same way.
public final class TornadoRenderer extends EntityRenderer<ArcaneTornado> {
    private static final int BANDS = 18;
    private static final int SIDES = 28;

    public TornadoRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ArcaneTornado tornado, Frustum frustum, double x, double y, double z) {
        return true;
    }

    // How wide the funnel is at `share` of its height, and how far it sways there at `age`.
    private static float wide(float share) {
        return 0.5F + 2.6F * (float) Math.pow(share, 1.6);
    }

    private static Vec3 sway(float share, float age) {
        return new Vec3(0.5F * share * Mth.sin(age * 0.07F + share * 2.0F), 0.0,
                0.5F * share * Mth.cos(age * 0.06F + share * 2.4F));
    }

    @Override
    public void render(ArcaneTornado tornado, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = tornado.tickCount + partialTick;
        float strength = ArcaneTornado.strength(age);
        if (strength <= 0.01F) {
            return;
        }
        float tall = (float) ArcaneTornado.TALL * (0.4F + 0.6F * strength);
        // Sheets of wind wound round it, each a ribbon up a helix.
        Vec3 eye = this.entityRenderDispatcher.camera.getPosition().subtract(tornado.getPosition(partialTick));
        VertexConsumer wind = buffers.getBuffer(Additive.MIST);
        for (int s = 0; s < 4; s++) {
            for (int k = 1; k <= 16; k++) {
                float thick = 0.25F + 0.5F * k / 16.0F;
                Additive.ribbon(pose.last(), wind, helix(s, k - 1, age, tall), helix(s, k, age, tall), eye, thick,
                        Additive.mist(0xDCCFF2, 0.18F * strength), Additive.mist(0xDCCFF2, 0.12F * strength));
            }
        }
        // Taking another buffer ends the one before, so each kind is drawn whole before the next is taken.
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        for (int s = 0; s < 4; s++) {
            for (int k = 2; k <= 16; k += 2) {
                Additive.ribbon(pose.last(), glow, helix(s, k - 1, age, tall), helix(s, k, age, tall), eye, 0.06F,
                        Additive.rgb(0xFFFFFF, 0.3F * strength), Additive.rgb(Wizard.VIOLET, 0.2F * strength));
            }
        }
        for (int b = 0; b < BANDS; b++) {
            float share = b / (float) (BANDS - 1);
            float y = share * tall;
            float r = wide(share) * (0.6F + 0.4F * strength);
            Vec3 off = sway(share, age);
            float spin = age * (0.55F - 0.3F * share) + share * 3.0F;
            pose.pushPose();
            pose.translate(off.x, y, off.z);
            pose.mulPose(Axis.YP.rotation(spin));
            float lit = strength * (0.25F + 0.35F * (b % 3 == 0 ? 1.0F : 0.4F));
            Additive.ring(pose.last(), glow, r * 0.92F, r, 0.0F, SIDES, Additive.rgb(Wizard.VIOLET, lit),
                    Additive.rgb(0xE8D8FF, lit * 0.8F));
            Additive.ring(pose.last(), glow, r, r * 1.25F, 0.0F, SIDES, Additive.rgb(Wizard.VIOLET, lit * 0.5F),
                    Additive.rgb(0, 0.0F));
            pose.popPose();
        }
        Additive.ring(pose.last(), glow, 0.6F, 2.6F, 0.05F, SIDES, Additive.rgb(Wizard.VIOLET, 0.35F * strength),
                Additive.rgb(0, 0.0F));
    }

    // Point `k` of 16 up the helix of sheet `s`.
    private static Vec3 helix(int s, int k, float age, float tall) {
        float share = k / 16.0F;
        float angle = age * 0.4F + s * Mth.HALF_PI + share * 5.0F;
        float r = wide(share) * 0.9F;
        return sway(share, age).add(Mth.cos(angle) * r, share * tall, Mth.sin(angle) * r);
    }

    @Override
    public ResourceLocation getTextureLocation(ArcaneTornado tornado) {
        return Additive.ORB;
    }
}
