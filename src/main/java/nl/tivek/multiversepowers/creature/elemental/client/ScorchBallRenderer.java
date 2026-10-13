package nl.tivek.multiversepowers.creature.elemental.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.elemental.Element;
import nl.tivek.multiversepowers.creature.elemental.spell.ScorchBall;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import nl.tivek.multiversepowers.engine.client.fx.Energy;

// A scorch ball: a ball of living flame, and a tail of flames each smaller than the last, torn
// off behind it along the way it flies.
final class ScorchBallRenderer extends EntityRenderer<ScorchBall> {
    private static final int TAIL = 5;

    ScorchBallRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public boolean shouldRender(ScorchBall ball, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    protected int getBlockLightLevel(ScorchBall ball, BlockPos pos) {
        return 15;
    }

    @Override
    public void render(ScorchBall ball, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
            int light) {
        var facing = this.entityRenderDispatcher.cameraOrientation();
        float age = ball.tickCount + partialTick;
        Vec3 middle = new Vec3(0.0, ball.getBbHeight() * 0.5, 0.0);
        float grown = Math.min(1.0F, age / 3.0F);
        VertexConsumer energy = Energy.buffer(buffers);
        if (energy == null) {
            return;
        }
        int seed = ball.getId();
        Vec3 way = ball.getDeltaMovement();
        double speed = way.length();
        Vec3 back = speed < 1.0E-3 ? Vec3.ZERO : way.scale(-1.0 / speed);
        for (int k = TAIL; k >= 1; k--) {
            float along = k / (float) TAIL;
            Vec3 at = middle.add(back.scale(along * 1.5 * grown)).add(0.0, 0.07 * Mth.sin(age * 0.9F + k * 1.7F), 0.0);
            Energy.puff(pose, energy, at, (0.55F - 0.3F * along) * grown, facing, Energy.FLAME, Element.FIRE.colour,
                    0.75F * (1.0F - along * 0.8F), seed + k);
        }
        Energy.puff(pose, energy, middle, 0.7F * grown, facing, Energy.FLAME, Element.FIRE.colour, 1.0F, seed);
    }

    @Override
    public ResourceLocation getTextureLocation(ScorchBall ball) {
        return Additive.ORB;
    }
}
