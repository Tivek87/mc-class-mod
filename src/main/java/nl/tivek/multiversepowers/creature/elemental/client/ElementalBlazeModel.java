package nl.tivek.multiversepowers.creature.elemental.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.creature.elemental.RodOrbit;

// The Elemental Blaze: a blaze's head and its rods in three rings, each rod in the look of its element: a pillar of
// stone with a boulder knot (earth), a burning rod with a tongue of flame on top (fire), a rod of ice with an icicle
// hanging from it (ice), a pale rod with a ring of wind round it (wind) and a rod forking with sparks (storm). Where
// every rod is comes from RodOrbit; its head looks at what it watches and is thrown back as it casts.
public final class ElementalBlazeModel extends HierarchicalModel<ElementalBlaze> {
    private final ModelPart root;
    final ModelPart head;
    final ModelPart[] rods = new ModelPart[RodOrbit.RODS];
    // How brightly each rod glows now, and the head's eyes: for the glow layer.
    final float[] glow = new float[RodOrbit.RODS];
    private final float[] place = new float[6];

    ElementalBlazeModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        for (int i = 0; i < RodOrbit.RODS; i++) {
            this.rods[i] = root.getChild("rod" + i);
        }
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F,
                8.0F), PartPose.ZERO);
        for (int i = 0; i < RodOrbit.RODS; i++) {
            boolean low = RodOrbit.ring(i) == 2;
            root.addOrReplaceChild("rod" + i, rod(RodOrbit.element(i), low), PartPose.ZERO);
        }
        return LayerDefinition.create(mesh, 64, 64);
    }

    // A rod of `element`'s look: the upper rings' long ones, or the lowest ring's short ones.
    private static CubeListBuilder rod(Element element, boolean low) {
        int u = element.ordinal() * 12;
        int v = low ? 34 : 16;
        int w = low ? 42 : 26;
        CubeListBuilder rod = CubeListBuilder.create().texOffs(u, v);
        float half = low ? 3.0F : 4.0F;
        rod.addBox(-1.0F, -half, -1.0F, 2.0F, half * 2.0F, 2.0F);
        rod.texOffs(u, w);
        switch (element) {
            case EARTH -> rod.addBox(low ? -1.0F : -1.5F, low ? -1.0F : -1.5F, low ? -1.0F : -1.5F, low ? 2.0F : 3.0F,
                    low ? 2.0F : 3.0F, low ? 2.0F : 3.0F);
            case FIRE -> rod.addBox(-0.5F, -half - 2.0F, -0.5F, 1.0F, 2.0F, 1.0F);
            case ICE -> rod.addBox(-0.5F, half, -0.5F, 1.0F, low ? 2.0F : 3.0F, 1.0F);
            case WIND -> rod.addBox(-1.5F, -0.5F, -1.5F, 3.0F, 1.0F, 3.0F);
            case STORM -> {
                rod.addBox(1.0F, -half * 0.5F, -0.5F, 1.0F, 2.0F, 1.0F);
                rod.texOffs(u + 4, w).addBox(-2.0F, half * 0.25F, -0.5F, 1.0F, 2.0F, 1.0F);
            }
        }
        return rod;
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public void setupAnim(ElementalBlaze blaze, float limbSwing, float limbSwingAmount, float ageInTicks,
            float netHeadYaw, float headPitch) {
        float partialTick = ageInTicks - blaze.tickCount;
        RodOrbit.State state = blaze.orbit(partialTick);
        float front = RodOrbit.front(state);
        Element casting = blaze.move() == 0 ? null : blaze.element();
        for (int i = 0; i < RodOrbit.RODS; i++) {
            RodOrbit.place(state, i, this.place);
            ModelPart rod = this.rods[i];
            rod.x = this.place[0];
            rod.y = this.place[1];
            rod.z = this.place[2];
            rod.xRot = this.place[3];
            rod.yRot = this.place[4];
            rod.zRot = this.place[5];
            Element own = RodOrbit.element(i);
            if (RodOrbit.ring(i) == 0 && own == casting) {
                rod.xRot += headPitch * Mth.DEG_TO_RAD * front;
            }
            this.glow[i] = RodOrbit.glow(state, own, RodOrbit.ring(i));
        }
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD - RodOrbit.headKick(state);
        this.head.y = Mth.sin(ageInTicks * 0.1F) * 0.4F + (state.dying() > 0.0F ? state.dying() * 0.4F : 0.0F);
    }
}
