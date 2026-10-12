package nl.tivek.multiversepowers.creature.wizard.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import nl.tivek.multiversepowers.creature.MovingMonster;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.creature.wizard.WizardPastimes;
import nl.tivek.multiversepowers.creature.wizard.client.fx.Additive;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// What the Wizard's pastimes show besides himself (WizardPastimes), worked out by every game from the move's age: the
// book floating open before him, its pages turning (the enchanting table's own book); the orbs of light he juggles; the
// cauldron he conjures, its brew glowing and changing colour; the wisp that flits round him; the circle of runes he
// draws round himself, lighting up as his staff goes round. Each grows out of nothing and is gone again the same way.
final class PastimeProps {
    // The juggled orbs go round in this many ticks; hands this far apart.
    private static final float JUGGLE = 22.0F;
    private static final float HANDS = 0.32F;
    private static final int[] BREWS = { 0x7A3CFF, 0x3CFFB0, 0xFF5A8C, 0x5AC8FF };
    private final BookModel book;

    PastimeProps(EntityRendererProvider.Context context) {
        this.book = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    // Drawn in the frame at his feet (lifted by his hover), not yet turned with his body.
    void draw(MovingMonster caster, float partialTick, float hover, PoseStack pose, MultiBufferSource buffers,
            int light, Quaternionf camera) {
        int move = caster.move();
        float t = (float) caster.moveAge(partialTick);
        int release = WizardMove.release(move);
        int length = WizardMove.length(move);
        // Out of nothing as the pastime begins, back into nothing at its end.
        float shown = Mth.clamp((t - release * 0.5F) / 10.0F, 0.0F, 1.0F) * Mth.clamp((length - 8.0F - t) / 10.0F,
                0.0F, 1.0F);
        if (shown <= 0.0F) {
            return;
        }
        float body = Mth.rotLerp(partialTick, caster.yBodyRotO, caster.yBodyRot);
        Quaternionf turn = new Quaternionf().rotationY((180.0F - body) * Mth.DEG_TO_RAD);
        int colour = Wizard.light(WizardRenderer.phase(caster));
        switch (move) {
            case WizardMove.READ -> this.read(t, shown, turn, pose, buffers, light);
            case WizardMove.JUGGLE -> juggle(t, release, shown, turn, pose, buffers, camera, colour);
            case WizardMove.BREW -> brew(caster, t, shown, hover, turn, pose, buffers, light);
            case WizardMove.WISP -> wisp(caster, partialTick, t, shown, hover, pose, buffers, camera);
            case WizardMove.SCRIBE -> scribe(t, release, length, hover, pose, buffers, colour);
            default -> {
            }
        }
    }

    // The book floating open before his chest, bobbing, its pages turning now and then.
    private void read(float t, float shown, Quaternionf turn, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(turn);
        pose.translate(0.0F, 1.22F + 0.03F * Mth.sin(t * 0.08F), -0.62F);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.mulPose(Axis.ZP.rotationDegrees(68.0F));
        pose.scale(shown, shown, shown);
        float flip = (t % 70.0F) / 70.0F;
        float right = Mth.clamp(flip * 1.6F - 0.3F, 0.0F, 1.0F);
        float left = Mth.clamp(flip * 1.6F - 0.5F, 0.0F, 1.0F);
        this.book.setupAnim(t, right, left, shown);
        VertexConsumer buffer = EnchantTableRenderer.BOOK_LOCATION.buffer(buffers, RenderType::entitySolid);
        this.book.renderToBuffer(pose, buffer, light, OverlayTexture.NO_OVERLAY, -1);
        pose.popPose();
    }

    // Three orbs of light tossed hand to hand in a cascade over his hands.
    private static void juggle(float t, int release, float shown, Quaternionf turn, PoseStack pose,
            MultiBufferSource buffers, Quaternionf camera, int colour) {
        if (t < release) {
            return;
        }
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        int[] colours = { colour, Wizard.FIRE, Wizard.ICE };
        for (int k = 0; k < 3; k++) {
            float u = ((t - release + k * JUGGLE / 3.0F) % JUGGLE) / JUGGLE;
            // Up out of the right hand and across into the left, then a low pass back under.
            float x;
            float y;
            if (u < 0.7F) {
                float f = u / 0.7F;
                x = HANDS - 2.0F * HANDS * f;
                y = 1.3F + 0.85F * 4.0F * f * (1.0F - f);
            } else {
                float f = (u - 0.7F) / 0.3F;
                x = -HANDS + 2.0F * HANDS * f;
                y = 1.3F + 0.12F * 4.0F * f * (1.0F - f);
            }
            Vector3f at = turn.transform(new Vector3f(x, y, -0.48F));
            Additive.glow(pose, buffer, new Vec3(at.x, at.y, at.z), 0.13F * shown, Additive.rgb(colours[k], 0.95F),
                    camera);
            Additive.glow(pose, buffer, new Vec3(at.x, at.y, at.z), 0.06F * shown, Additive.rgb(0xFFFFFF, 0.9F),
                    camera);
        }
    }

    // The cauldron on the ground before him, its brew glowing in a colour that turns as he works.
    private static void brew(MovingMonster caster, float t, float shown, float hover, Quaternionf turn,
            PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.translate(0.0F, -hover, 0.0F);
        pose.mulPose(turn);
        pose.translate(0.0F, 0.0F, -1.3F);
        pose.scale(shown, shown, shown);
        pose.translate(-0.5F, 0.0F, -0.5F);
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(Blocks.CAULDRON.defaultBlockState(), pose,
                buffers, light, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
        int colour = BREWS[(caster.getId() + (int) (t / 120.0F)) % BREWS.length];
        float glow = 0.75F + 0.25F * Mth.sin(t * 0.3F);
        float[] lit = Additive.rgb(colour, glow);
        float y = 0.82F + 0.015F * Mth.sin(t * 0.5F);
        float in = 0.14F;
        float out = 0.86F;
        Additive.quad(pose.last(), buffers.getBuffer(Additive.TYPE), new float[] { in, y, in },
                new float[] { in, y, out }, new float[] { out, y, out }, new float[] { out, y, in }, lit, lit, lit,
                lit);
        pose.popPose();
    }

    // The wisp: a mote of light flitting round him with a short tail.
    private static void wisp(MovingMonster caster, float partialTick, float t, float shown, float hover,
            PoseStack pose, MultiBufferSource buffers, Quaternionf camera) {
        Vec3 origin = caster.getPosition(partialTick).add(0.0, hover, 0.0);
        VertexConsumer buffer = buffers.getBuffer(Additive.TYPE);
        for (int k = 4; k >= 0; k--) {
            Vec3 at = WizardPastimes.wisp(caster, t - k * 1.5F).subtract(origin);
            float fade = 1.0F - k / 5.0F;
            Additive.glow(pose, buffer, at, (0.14F - 0.02F * k) * shown, Additive.rgb(0xBFF6FF, 0.9F * fade),
                    camera);
        }
        Vec3 at = WizardPastimes.wisp(caster, t).subtract(origin);
        float twinkle = 0.5F + 0.5F * Mth.sin(t * 0.9F);
        Additive.glow(pose, buffer, at, 0.07F * shown * (0.8F + 0.4F * twinkle), Additive.rgb(0xFFFFFF, 1.0F),
                camera);
    }

    // The circle of runes on the ground round him, lighting up as far round as his staff has gone, flaring as it
    // closes.
    private static void scribe(float t, int release, int length, float hover, PoseStack pose,
            MultiBufferSource buffers, int colour) {
        float done = Mth.clamp((t - release) / (length - 40.0F - release), 0.0F, 1.0F);
        float flare = Mth.clamp(1.0F - Math.abs(t - (length - 39.0F)) / 8.0F, 0.0F, 1.0F);
        float end = Mth.clamp((length - t) / 12.0F, 0.0F, 1.0F);
        float strength = (0.25F + 0.55F * done + 0.6F * flare) * end;
        pose.pushPose();
        pose.translate(0.0F, -hover, 0.0F);
        Additive.glyph(pose.last(), buffers.getBuffer(Additive.TYPE), 1.7F * (0.6F + 0.4F * done), t, colour,
                strength);
        pose.popPose();
    }
}
