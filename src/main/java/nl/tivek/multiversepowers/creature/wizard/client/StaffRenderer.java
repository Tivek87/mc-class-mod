package nl.tivek.multiversepowers.creature.wizard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.model.geom.ModelPart;
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
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.staff.StaffActs;
import nl.tivek.multiversepowers.creature.wizard.staff.WizardStaff;
import org.joml.Quaternionf;

// The Wizard's staff out of his hand (staff/WizardStaff), drawn as it is in his hand (StaffLayer's model) along the way
// its crystal points: flying, it turns end over end as it comes home; free, it bobs with its crystal blazing, a ring of
// runes turning round its head, kicks back as it casts, eases down flat to spin as a blade, and shakes and flashes red
// when struck. Broken, it is not drawn until it is whole in his hand again.
final class StaffRenderer extends EntityRenderer<WizardStaff> {
    // The staff's middle lies this far from the grip its model is built round (pixels, towards the crystal).
    private static final float MIDDLE = 5.0F;
    private static final Map<WizardStaff, float[]> SHOWN = new WeakHashMap<>();
    private static final int AGE = 0;
    private static final int ROLL = 1;
    private static final int TUMBLE = 2;
    private static final int FLAT = 3;
    private static final int BLADE = 4;
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
    public boolean shouldRender(WizardStaff staff, Frustum frustum, double x, double y, double z) {
        return staff.mode() != WizardStaff.BROKEN && super.shouldRender(staff, frustum, x, y, z);
    }

    @Override
    public void render(WizardStaff entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        int mode = entity.mode();
        if (mode == WizardStaff.BROKEN) {
            return;
        }
        float age = entity.tickCount + partialTick;
        Vec3 way = Vec3.directionFromRotation(entity.getViewXRot(partialTick), entity.getViewYRot(partialTick));
        int act = entity.act();
        boolean free = mode == WizardStaff.FREE;
        float[] shown = this.shown(entity, mode, act, age);
        float actAge = entity.actAge(partialTick);
        pose.pushPose();
        pose.translate(0.0, WizardStaff.MIDDLE + (free ? 0.12 * Mth.sin(age * 0.15F) : 0.0), 0.0);
        if (shown[FLAT] > 0.001F) {
            Vec3 blade = new Vec3(Mth.cos(shown[BLADE]), 0.0, Mth.sin(shown[BLADE]));
            Vec3 mixed = way.scale(1.0F - shown[FLAT]).add(blade.scale(shown[FLAT]));
            way = mixed.lengthSqr() > 1.0E-4 ? mixed.normalize() : blade;
        }
        if (free && act != 0 && act < StaffActs.SPIN) {
            // A cast kicks it back along itself; while it pours out it trembles.
            float kick = WizardRest.bump(Mth.clamp(actAge / 7.0F, 0.0F, 1.0F));
            float shake = 0.025F * Mth.sin(age * 2.3F) * Mth.clamp(actAge / 4.0F, 0.0F, 1.0F);
            pose.translate(-way.x * 0.2F * kick + shake, -way.y * 0.2F * kick, -way.z * 0.2F * kick - shake);
        }
        if (free && age - entity.hurtAt() < 6.0F) {
            float jolt = 0.08F * (1.0F - (age - entity.hurtAt()) / 6.0F);
            pose.translate(jolt * Mth.sin(age * 5.1F), jolt * Mth.sin(age * 4.3F), jolt * Mth.cos(age * 4.7F));
        }
        pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) way.x, (float) way.y, (float) way.z));
        pose.mulPose(Axis.XP.rotation(shown[TUMBLE]));
        pose.mulPose(Axis.YP.rotation(shown[ROLL]));
        pose.translate(0.0F, -MIDDLE / 16.0F, 0.0F);
        pose.scale(-1.0F, -1.0F, 1.0F);
        int phase = Math.max(1, entity.phase());
        ResourceLocation texture = WizardRenderer.texture(phase);
        boolean hurt = free && entity.tickCount - entity.hurtAt() < 6;
        int overlay = OverlayTexture.pack(0, hurt ? 3 : 10);
        this.staff.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        this.crystal.yRot = Mth.PI / 4.0F + age * (free ? 0.12F : 0.05F);
        this.crystal.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture)), light, overlay);
        int colour = Wizard.light(phase);
        float bright = Mth.clamp((free ? 0.85F : 0.55F) + (act != 0 ? 0.3F : 0.0F) + 0.1F * Mth.sin(age * 0.3F),
                0.0F, 1.0F);
        int r = (int) ((colour >> 16 & 255) * bright);
        int g = (int) ((colour >> 8 & 255) * bright);
        int b = (int) ((colour & 255) * bright);
        this.crystal.render(pose, buffers.getBuffer(RenderType.eyes(WizardRenderer.glow(phase))), 15728640,
                OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
        pose.popPose();
        if (free) {
            // A ring of runes turning round its head while it fights on its own.
            Vec3 tip = way.scale(WizardStaff.CRYSTAL).add(0.0, WizardStaff.MIDDLE, 0.0);
            pose.pushPose();
            pose.translate(tip.x, tip.y, tip.z);
            pose.mulPose(new Quaternionf().rotationTo(0.0F, 1.0F, 0.0F, (float) way.x, (float) way.y, (float) way.z));
            float strength = 0.45F + (act != 0 ? 0.35F : 0.0F);
            Additive.glyph(pose.last(), buffers.getBuffer(Additive.TYPE), 0.55F, age * 2.0F, colour, strength);
            pose.popPose();
        }
        super.render(entity, yaw, partialTick, pose, buffers, light);
    }

    // Its turns kept from frame to frame and eased, so a change of mode or act never snaps it to a new angle.
    private float[] shown(WizardStaff entity, int mode, int act, float age) {
        float[] shown = SHOWN.get(entity);
        if (shown == null) {
            shown = new float[] { age, 0.0F, 0.0F, 0.0F, 0.0F };
            SHOWN.put(entity, shown);
        }
        float dt = Mth.clamp(age - shown[AGE], 0.0F, 3.0F);
        shown[AGE] = age;
        boolean free = mode == WizardStaff.FREE;
        float roll = mode == WizardStaff.THROWN ? 0.5F : free ? (act != 0 ? 0.2F : 0.08F) : 0.0F;
        shown[ROLL] = (shown[ROLL] + roll * dt) % Mth.TWO_PI;
        if (mode == WizardStaff.RETURNING) {
            shown[TUMBLE] = (shown[TUMBLE] + 0.9F * dt) % Mth.TWO_PI;
        } else {
            // Out of its tumble it settles the short way round to lie straight again.
            float rest = shown[TUMBLE] > Mth.PI ? Mth.TWO_PI : 0.0F;
            shown[TUMBLE] = Mth.lerp(1.0F - (float) Math.exp(-0.35F * dt), shown[TUMBLE], rest);
        }
        boolean blade = free && StaffActs.spins(act);
        shown[FLAT] = Mth.lerp(1.0F - (float) Math.exp(-(blade ? 0.45F : 0.25F) * dt), shown[FLAT], blade ? 1.0F
                : 0.0F);
        shown[BLADE] = (shown[BLADE] + 1.4F * shown[FLAT] * dt) % Mth.TWO_PI;
        return shown;
    }

    @Override
    public ResourceLocation getTextureLocation(WizardStaff staff) {
        return WizardRenderer.texture(Math.max(1, staff.phase()));
    }
}
