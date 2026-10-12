package nl.tivek.multiversepowers.creature.wizard.client.field;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.creature.wizard.field.GlacialSpikes;

// Glacial spikes: a line of jagged ice bursting up out of the ground behind its running head, the spikes leaning out
// along the way it runs and to either side, some tall, some short, each standing on the ground where it rose; a trail
// of frost glowing under them and a crown of great spikes where the line ends. They stand, then sink back.
public final class SpikesRenderer extends EntityRenderer<GlacialSpikes> {
    private static final float STEP = 0.42F;
    private static final int CROWN = 11;

    public SpikesRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(GlacialSpikes spikes, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(GlacialSpikes spikes, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        float age = spikes.tickCount + partialTick;
        float length = spikes.length();
        float head = (float) spikes.head(age);
        float sink = Mth.clamp((age - (spikes.life() - 8)) / 8.0F, 0.0F, 1.0F);
        float fade = 1.0F - sink;
        int count = Mth.ceil(head / STEP);
        float[] ground = this.ground(spikes, count, length);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-spikes.getYRot()));
        VertexConsumer glow = buffers.getBuffer(Additive.TYPE);
        Additive.line(pose.last(), glow, 0.0F, 0.0F, 0.0F, head, 1.6F, 0.03F, Additive.rgb(Wizard.ICE, 0.22F * fade));
        Additive.line(pose.last(), glow, 0.0F, 0.0F, 0.0F, head, 0.5F, 0.035F, Additive.rgb(0xFFFFFF,
                0.3F * fade));
        this.line(pose, glow, spikes, count, ground, age, sink, false);
        this.crown(pose, glow, spikes, length, ground, age, sink, false);
        VertexConsumer ice = buffers.getBuffer(Additive.MIST);
        this.line(pose, ice, spikes, count, ground, age, sink, true);
        this.crown(pose, ice, spikes, length, ground, age, sink, true);
        pose.popPose();
    }

    // The spikes along the line, each rising as the head passes it.
    private void line(PoseStack pose, VertexConsumer buffer, GlacialSpikes spikes, int count, float[] ground,
            float age, float sink, boolean ice) {
        int seed = spikes.getId() * 31;
        for (int k = 0; k < count; k++) {
            float along = (k + 0.5F) * STEP;
            float risen = Mth.clamp((age - along / (float) GlacialSpikes.SPEED) / 2.5F, 0.0F, 1.0F);
            float grow = 1.0F - (1.0F - risen) * (1.0F - risen);
            float tall = (0.55F + 0.85F * hash(seed + k, 1)) * grow * (1.0F - sink);
            if (tall <= 0.02F) {
                continue;
            }
            for (int side = -1; side <= 1; side += 2) {
                if (hash(seed + k, 7 + side) < 0.25F) {
                    continue;
                }
                float off = side * (0.25F + 0.45F * hash(seed + k, 3 + side));
                float h = tall * (0.7F + 0.5F * hash(seed + k, 5 + side));
                pose.pushPose();
                pose.translate(off, ground[Math.min(k, ground.length - 1)] - 0.2F - h * sink, along);
                pose.mulPose(Axis.ZP.rotation(-side * (0.25F + 0.3F * hash(seed + k, 9 + side))));
                pose.mulPose(Axis.XP.rotation(0.35F + 0.25F * hash(seed + k, 11 + side)));
                spike(pose, buffer, h * 1.5F, 0.13F + 0.08F * hash(seed + k, 13 + side), ice, 1.0F - sink);
                pose.popPose();
            }
        }
    }

    // The crown of great spikes where the line ends, bursting up once it is there.
    private void crown(PoseStack pose, VertexConsumer buffer, GlacialSpikes spikes, float length, float[] ground,
            float age, float sink, boolean ice) {
        float since = age - spikes.ran();
        if (since <= 0.0F) {
            return;
        }
        float grow = Mth.clamp(since / 3.0F, 0.0F, 1.0F);
        grow = 1.0F - (1.0F - grow) * (1.0F - grow) * (1.0F - grow);
        float y = ground[ground.length - 1];
        int seed = spikes.getId() * 17;
        for (int k = 0; k < CROWN; k++) {
            float angle = k * Mth.TWO_PI / CROWN + 0.3F * hash(seed + k, 2);
            float out = k == 0 ? 0.0F : 0.6F + 1.2F * hash(seed + k, 4);
            float tall = (k == 0 ? 3.2F : 1.4F + 1.4F * hash(seed + k, 6)) * grow * (1.0F - sink);
            pose.pushPose();
            pose.translate(Mth.cos(angle) * out, y - 0.3F - tall * sink, length + Mth.sin(angle) * out);
            pose.mulPose(Axis.YP.rotation(-angle));
            pose.mulPose(Axis.ZP.rotation(k == 0 ? 0.0F : -0.3F - 0.35F * hash(seed + k, 8)));
            spike(pose, buffer, tall * 1.4F, k == 0 ? 0.45F : 0.22F + 0.12F * hash(seed + k, 10), ice, 1.0F - sink);
            pose.popPose();
        }
        if (!ice) {
            float flash = Mth.clamp(1.0F - since / 8.0F, 0.0F, 1.0F);
            pose.pushPose();
            pose.translate(0.0F, y, length);
            Additive.ring(pose.last(), buffer, 0.0F, (float) GlacialSpikes.CROWN * (0.6F + 0.4F * grow), 0.05F, 36,
                    Additive.rgb(0xFFFFFF, 0.6F * flash + 0.15F * (1.0F - sink)), Additive.rgb(Wizard.ICE, 0.0F));
            pose.popPose();
        }
    }

    // A spike of ice: its body laid over the world, or the cold light in it.
    private static void spike(PoseStack pose, VertexConsumer buffer, float length, float radius, boolean ice,
            float strength) {
        if (ice) {
            Additive.spindle(pose.last(), buffer, length, radius, 0.22F, 5, Additive.mist(0xC4EEFF, 0.86F),
                    Additive.mist(0xF2FBFF, 0.96F));
        } else {
            Additive.spindle(pose.last(), buffer, length, radius * 0.6F, 0.22F, 5,
                    Additive.rgb(Wizard.ICE, 0.32F * strength), Additive.rgb(0xFFFFFF, 0.55F * strength));
        }
    }

    // Where the ground is under each spike of the line and under its end, as heights over where it began: each spike
    // stands on the top of the block under it, a step up or down from its neighbour.
    private float[] ground(GlacialSpikes spikes, int count, float length) {
        Level level = spikes.level();
        Vec3 way = spikes.way();
        Vec3 from = spikes.position();
        float[] heights = new float[Math.max(1, count) + 1];
        for (int k = 0; k < heights.length; k++) {
            float along = k < count ? (k + 0.5F) * STEP : length;
            heights[k] = top(level, from.add(way.scale(along)), from.y);
        }
        return heights;
    }

    private static float top(Level level, Vec3 at, double base) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dy = 2; dy >= -3; dy--) {
            pos.set(at.x, base + dy - 0.5, at.z);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                return (float) (pos.getY() + 1 - base);
            }
        }
        return 0.0F;
    }

    private static float hash(int a, int b) {
        int h = a * 73856093 ^ b * 19349663;
        h ^= h >>> 13;
        h *= 0x5BD1E995;
        h ^= h >>> 15;
        return (h & 0xFFFF) / 65535.0F;
    }

    @Override
    public ResourceLocation getTextureLocation(GlacialSpikes spikes) {
        return Additive.ORB;
    }
}
