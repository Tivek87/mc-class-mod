package nl.tivek.multiversepowers.creature.wizard.client;

import static nl.tivek.multiversepowers.creature.wizard.client.WizardKeys.*;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Vector3f;

// The Wizard's staff in his right hand: a long shaft of dark twisted wood shod with iron, its head four claws round
// a crystal that glows in his phase's colour, brighter as he casts (GLOW), the staff tilted (STAFF) and twirled about
// its own length (SPIN) by his moves. An image's crystal is pale, the one sure sign it is not him. Where the crystal
// and his left hand are drawn is told to the Wizard, so his spells' sparks gather there (Wizard.drawnAt).
final class StaffLayer<T extends MovingMonster> extends RenderLayer<T, WizardModel<T>> {
    static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(
            MultiversePowers.MODID, "wizard_staff"), "main");
    // The grip down the arm from the shoulder, and the crystal's middle up the staff from the grip, in pixels.
    private static final float GRIP = 9.0F;
    private static final float CRYSTAL = -23.5F;
    private static final int PALE = 0xC8C4D0;
    private final ModelPart staff;
    private final ModelPart crystal;

    StaffLayer(RenderLayerParent<T, WizardModel<T>> parent, ModelPart root) {
        super(parent);
        this.staff = root.getChild("staff");
        this.crystal = root.getChild("crystal");
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder staff = CubeListBuilder.create()
                .texOffs(0, 64).addBox(-0.75F, -20.0F, -0.75F, 1.5F, 35.0F, 1.5F)
                .texOffs(8, 64).addBox(-1.5F, -21.0F, -1.5F, 3.0F, 2.0F, 3.0F)
                .texOffs(32, 64).addBox(-1.0F, 14.0F, -1.0F, 2.0F, 2.0F, 2.0F)
                .texOffs(8, 72).addBox(-1.0F, 1.5F, -1.0F, 2.0F, 1.0F, 2.0F)
                .texOffs(8, 72).addBox(-1.0F, -5.5F, -1.0F, 2.0F, 1.0F, 2.0F);
        for (int k = 0; k < 4; k++) {
            float x = (k & 1) == 0 ? -1.6F : 0.6F;
            float z = (k & 2) == 0 ? -1.6F : 0.6F;
            staff.texOffs(20, 64).addBox(x, -26.0F, z, 1.0F, 5.0F, 1.0F);
        }
        root.addOrReplaceChild("staff", staff, PartPose.ZERO);
        root.addOrReplaceChild("crystal", CubeListBuilder.create()
                .texOffs(24, 64).addBox(-1.0F, -2.5F, -1.0F, 2.0F, 5.0F, 2.0F)
                .texOffs(24, 72).addBox(-0.5F, -3.5F, -0.5F, 1.0F, 1.0F, 1.0F),
                PartPose.offsetAndRotation(0.0F, CRYSTAL, 0.0F, 0.0F, Mth.PI / 4.0F, 0.0F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T caster, float limbSwing, float amount,
            float partialTick, float age, float headYaw, float headPitch) {
        WizardModel<T> model = this.getParentModel();
        if (caster instanceof Wizard wizard && wizard.staffOut()) {
            this.bareHand(pose, buffers, wizard, age);
            return;
        }
        float[] c = model.pose.c;
        boolean faded = model.alpha < 1.0F;
        ResourceLocation texture = WizardRenderer.texture(WizardRenderer.phase(caster));
        RenderType wood = faded ? RenderType.entityTranslucent(texture) : RenderType.entityCutoutNoCull(texture);
        int tint = faded ? (int) (255 * model.alpha) << 24 | 0xFFFFFF : -1;
        pose.pushPose();
        model.translateToHand(HumanoidArm.RIGHT, pose);
        pose.translate(0.0F, GRIP / 16.0F, 0.0F);
        pose.mulPose(Axis.XP.rotation(Mth.HALF_PI + c[STAFF]));
        pose.mulPose(Axis.YP.rotation(c[SPIN]));
        this.staff.render(pose, buffers.getBuffer(wood), light, OverlayTexture.NO_OVERLAY, tint);
        this.crystal.yRot = Mth.PI / 4.0F + age * 0.05F;
        this.crystal.render(pose, buffers.getBuffer(wood), light, OverlayTexture.NO_OVERLAY, tint);
        boolean real = caster instanceof Wizard;
        float bright = Mth.clamp(0.45F + 0.35F * c[GLOW] + 0.08F * Mth.sin(age * 0.2F), 0.0F, 1.0F);
        if (!real) {
            bright *= 0.55F;
        }
        int colour = real ? Wizard.light(WizardRenderer.phase(caster)) : PALE;
        int r = (int) ((colour >> 16 & 255) * bright);
        int g = (int) ((colour >> 8 & 255) * bright);
        int b = (int) ((colour & 255) * bright);
        this.crystal.render(pose, buffers.getBuffer(RenderType.eyes(WizardRenderer.glow(WizardRenderer.phase(
                caster)))), light,
                OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
        Vec3 tip = world(pose, 0.0F, CRYSTAL / 16.0F, 0.0F);
        pose.popPose();
        pose.pushPose();
        model.translateToHand(HumanoidArm.LEFT, pose);
        Vec3 hand = world(pose, 0.0F, 9.0F / 16.0F, -0.1F);
        pose.popPose();
        if (caster instanceof Wizard wizard && EntityPass.inWorld()) {
            wizard.drawnAt(tip, hand);
        }
    }

    // His staff away, he casts from his bare right hand: a mote of his phase's light turns in his palm, where his
    // spells now leave from.
    private void bareHand(PoseStack pose, MultiBufferSource buffers, Wizard wizard, float age) {
        WizardModel<T> model = this.getParentModel();
        int phase = wizard.phase();
        pose.pushPose();
        model.translateToHand(HumanoidArm.RIGHT, pose);
        pose.translate(0.0F, 10.5F / 16.0F, -0.05F);
        pose.scale(0.55F, 0.55F, 0.55F);
        this.crystal.yRot = age * 0.2F;
        this.crystal.xRot = age * 0.13F;
        float bright = Mth.clamp(0.7F + 0.4F * model.pose.c[GLOW] + 0.1F * Mth.sin(age * 0.4F), 0.0F, 1.0F);
        int colour = Wizard.light(phase);
        int r = (int) ((colour >> 16 & 255) * bright);
        int g = (int) ((colour >> 8 & 255) * bright);
        int b = (int) ((colour & 255) * bright);
        pose.translate(0.0F, -CRYSTAL / 16.0F, 0.0F);
        this.crystal.render(pose, buffers.getBuffer(RenderType.eyes(WizardRenderer.glow(phase))), 15728640,
                OverlayTexture.NO_OVERLAY, 0xFF000000 | r << 16 | g << 8 | b);
        Vec3 tip = world(pose, 0.0F, CRYSTAL / 16.0F, 0.0F);
        this.crystal.xRot = 0.0F;
        pose.popPose();
        pose.pushPose();
        model.translateToHand(HumanoidArm.LEFT, pose);
        Vec3 hand = world(pose, 0.0F, 9.0F / 16.0F, -0.1F);
        pose.popPose();
        if (EntityPass.inWorld()) {
            wizard.drawnAt(tip, hand);
        }
    }

    // A point in the pose's frame, in the world: the frame is the camera's place plus that.
    private static Vec3 world(PoseStack pose, float x, float y, float z) {
        Vector3f at = pose.last().pose().transformPosition(x, y, z, new Vector3f());
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        return camera.add(at.x, at.y, at.z);
    }
}
