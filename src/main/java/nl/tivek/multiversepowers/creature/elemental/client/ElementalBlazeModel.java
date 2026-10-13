package nl.tivek.multiversepowers.creature.elemental.client;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.creature.elemental.BlazeTimeline;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.ElementalBlaze;
import nl.tivek.multiversepowers.creature.elemental.RodOrbit;
import nl.tivek.multiversepowers.engine.math.Ease;

// The Elemental Blaze: a blaze's head and its rods in three rings, each rod in the look of its element: a pillar of
// stone with a boulder knot (earth), a burning rod with a tongue of flame on top (fire), a rod of ice with an icicle
// hanging from it (ice), a pale rod with a ring of wind round it (wind) and a rod forking with sparks (storm). Where
// every rod is comes from RodOrbit; its head looks at what it watches and is thrown back as it casts. On the head
// stands a crown of five shards, one of each element, and under it hangs its heart, a crystal turning on its corner.
public final class ElementalBlazeModel extends HierarchicalModel<ElementalBlaze> {
    // How far from the head's middle the crown's shards stand, and how far under it the heart hangs, in pixels.
    private static final float CROWN = 2.6F;
    private static final float CORE = 10.0F;
    private final ModelPart root;
    final ModelPart head;
    final ModelPart[] rods = new ModelPart[RodOrbit.RODS];
    // How brightly each rod glows now, and the head's eyes: for the glow layer.
    final float[] glow = new float[RodOrbit.RODS];
    // The crown's shard of each element, on the head, and how brightly each glows.
    final ModelPart[] crown = new ModelPart[Element.ALL.length];
    final float[] crownGlow = new float[Element.ALL.length];
    // The heart hanging under the head, and how brightly it burns in the colour of the element in use.
    final ModelPart core;
    private final ModelPart gem;
    float coreGlow;
    private final float[] place = new float[6];

    ElementalBlazeModel(ModelPart root) {
        this.root = root;
        this.head = root.getChild("head");
        this.core = root.getChild("core");
        this.gem = this.core.getChild("gem");
        for (int i = 0; i < RodOrbit.RODS; i++) {
            this.rods[i] = root.getChild("rod" + i);
        }
        for (int e = 0; e < this.crown.length; e++) {
            this.crown[e] = this.head.getChild("crown" + e);
        }
    }

    static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F,
                -4.0F, -4.0F, 8.0F, 8.0F, 8.0F), PartPose.ZERO);
        for (int e = 0; e < Element.ALL.length; e++) {
            float angle = crownAngle(e);
            head.addOrReplaceChild("crown" + e, CubeListBuilder.create().texOffs(e * 8, 50).addBox(-1.0F, -5.0F,
                    -1.0F, 2.0F, 5.0F, 2.0F), PartPose.offset(Mth.cos(angle) * CROWN, -3.5F, Mth.sin(angle) * CROWN));
        }
        root.addOrReplaceChild("core", CubeListBuilder.create(), PartPose.offset(0.0F, CORE, 0.0F))
                .addOrReplaceChild("gem", CubeListBuilder.create().texOffs(32, 0).addBox(-2.0F, -3.0F, -2.0F, 4.0F,
                        6.0F, 4.0F), PartPose.ZERO);
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
        float brace = RodOrbit.brace(state);
        float release = RodOrbit.release(state);
        float fierce = state.awake() >= 0.0F ? 1.0F : 0.0F;
        // Its head sways as it hovers, draws back and down as it braces, snaps and swells as the spell leaves, shivers
        // while it holds the frost ray or gathers a storm, and flinches from a blow.
        Element held = casting != null && brace > 0.9F && release <= 0.0F ? casting : null;
        float shiver = held == Element.ICE ? 0.05F : held == Element.STORM ? 0.09F : 0.0F;
        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD + Mth.sin(ageInTicks * 0.07F) * 0.06F
                + Mth.sin(ageInTicks * 3.9F) * shiver;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD - RodOrbit.headKick(state) - 0.22F * brace + 0.3F * state.hurt()
                + Mth.sin(ageInTicks * 4.7F) * shiver;
        this.head.zRot = Mth.sin(ageInTicks * 0.053F + 1.3F) * 0.07F + 0.25F * state.hurt()
                * Mth.sin(ageInTicks * 2.6F);
        this.head.y = Mth.sin(ageInTicks * 0.1F) * 0.6F + 0.8F * brace - 1.2F * release
                + (state.dying() > 0.0F ? state.dying() * 0.4F : 0.0F);
        float swell = 1.0F + 0.14F * release - 0.05F * brace;
        this.head.xScale = swell;
        this.head.yScale = swell;
        this.head.zScale = swell;
        for (int e = 0; e < this.crown.length; e++) {
            Element own = Element.ALL[e];
            float lit = RodOrbit.glow(state, own, 0);
            float angle = crownAngle(e);
            // Each shard leans out from the head, flares wide and grows as its element is cast, and all of them rattle
            // when it is hit.
            float lean = 0.3F + 0.08F * Mth.sin(ageInTicks * 0.13F + e * 1.7F) + 0.2F * fierce + 0.55F * lit
                    + 0.35F * state.hurt() * Mth.sin(ageInTicks * 3.1F + e);
            ModelPart shard = this.crown[e];
            shard.zRot = lean * Mth.cos(angle);
            shard.xRot = -lean * Mth.sin(angle);
            shard.yScale = 1.0F + 0.25F * fierce + 0.6F * lit;
            this.crownGlow[e] = Mth.clamp(0.25F * fierce + lit, 0.0F, 1.0F);
        }
        // The heart turns under the head, bobbing against it; it spins up and shrinks as the cast is braced and
        // bursts wide as the spell leaves.
        this.core.y = CORE - Mth.sin(ageInTicks * 0.1F) * 0.9F + 1.5F * brace
                + (state.dying() > 0.0F ? state.dying() * 0.9F : 0.0F);
        this.core.yRot = ageInTicks * (0.09F + 0.07F * fierce) + this.spun(state);
        this.gem.xRot = 0.7854F;
        this.gem.zRot = 0.6155F + Mth.sin(ageInTicks * 0.17F) * 0.1F;
        float size = (1.0F + 0.08F * Mth.sin(ageInTicks * 0.23F) - 0.3F * brace + 0.7F * release)
                * (state.dying() > 0.0F ? Math.max(0.0F, 1.0F - state.dying() / 14.0F) : 1.0F);
        this.core.xScale = size;
        this.core.yScale = size;
        this.core.zScale = size;
        this.coreGlow = Mth.clamp(0.35F + 0.15F * fierce + 0.5F * brace + 0.6F * release, 0.0F, 1.0F);
    }

    // The extra turns the heart makes over a move, whole ones, so it is back where it was as the move ends.
    private float spun(RodOrbit.State state) {
        int length = BlazeTimeline.length(state.move());
        return length <= 0 ? 0.0F : Mth.TWO_PI * 3.0F * (float) Ease.smoother(state.moveAge() / length);
    }

    private static float crownAngle(int element) {
        return element * Mth.TWO_PI / Element.ALL.length + 0.3F;
    }
}
