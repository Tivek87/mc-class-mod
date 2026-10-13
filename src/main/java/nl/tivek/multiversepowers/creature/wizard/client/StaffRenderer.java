package nl.tivek.multiversepowers.creature.wizard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;
import org.joml.Quaternionf;

// The Wizard's staff out of his hand (staff/WizardStaff), drawn as it is in his hand (StaffLayer's model) along the way
// its crystal points: flying it rolls, and it turns end over end as it comes home.
final class StaffRenderer extends EntityRenderer<WizardStaff> {
    // The staff's middle lies this far from the grip its model is built round (pixels, towards the crystal).
    private static final float MIDDLE = 5.0F;
    private static final Map<WizardStaff, float[]> SHOWN = new WeakHashMap<>();
    private static final int AGE = 0;
    private static final int ROLL = 1;
    private static final int TUMBLE = 2;
    private final ModelPart staff;
    private final ModelPart crystal;

    StaffRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(StaffLayer.LAYER);
        this.staff = root.getChild("staff");
        this.crystal = root.getChild("crystal");
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(WizardStaff entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        int mode = entity.mode();
        float age = entity.tickCount + partialTick;
        Vec3 way = Vec3.directionFromRotation(entity.getViewXRot(partialTick), entity.getViewYRot(partialTick));
        float[] shown = this.shown(entity, mode, age);
        pose.pushPose();
        pose.translate(0.0, WizardStaff.MIDDLE, 0.0);
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) way.x, (float) way.y, (float) way.z));
        pose.mulPose(Axis.XP.rotation(shown[TUMBLE]));
        pose.mulPose(Axis.YP.rotation(shown[ROLL]));
        pose.translate(0.0F, -MIDDLE / 16.0F, 0.0F);
        pose.scale(-1.0F, -1.0F, 1.0F);
        int phase = Math.max(1, entity.phase());
        ResourceLocation texture = WizardRenderer.texture(phase);
        int overlay = OverlayTexture.NO_OVERLAY;
        this.staff.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        this.crystal.yRot = Mth.PI / 4.0F + age * 0.05F;
        this.crystal.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        int colour = Wizard.light(phase);
        float bright = Mth.clamp(0.55F + 0.1F * Mth.sin(age * 0.3F), 0.0F, 1.0F);
        int r = (int) ((colour >> 16 & 255) * bright);
        int g = (int) ((colour >> 8 & 255) * bright);
        int b = (int) ((colour & 255) * bright);
        this.crystal.render(pose, buffers.getBuffer(RenderType.eyes(WizardRenderer.glow(phase))), 15728640,
                OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
        pose.popPose();
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    // Its turns kept from frame to frame and eased, so a change of mode never snaps it to a new angle.
    private float[] shown(WizardStaff entity, int mode, float age) {
        float[] shown = SHOWN.get(entity);
        if (shown == null) {
            shown = new float[] { age, 0.0F, 0.0F };
            SHOWN.put(entity, shown);
        }
        float dt = Mth.clamp(age - shown[AGE], 0.0F, 3.0F);
        shown[AGE] = age;
        float roll = mode == WizardStaff.THROWN ? 0.5F : 0.0F;
        shown[ROLL] = (shown[ROLL] + roll * dt) % Mth.TWO_PI;
        if (mode == WizardStaff.RETURNING) {
            shown[TUMBLE] = (shown[TUMBLE] + 0.9F * dt) % Mth.TWO_PI;
        } else {
            // Out of its tumble it settles the short way round to lie straight again.
            float rest = shown[TUMBLE] > Mth.PI ? Mth.TWO_PI : 0.0F;
            shown[TUMBLE] = Mth.lerp(1.0F - (float) Math.exp(-0.35F * dt), shown[TUMBLE], rest);
        }
        return shown;
    }

    @Override
    public ResourceLocation getTextureLocation(WizardStaff staff) {
        return WizardRenderer.texture(Math.max(1, staff.phase()));
    }
}
