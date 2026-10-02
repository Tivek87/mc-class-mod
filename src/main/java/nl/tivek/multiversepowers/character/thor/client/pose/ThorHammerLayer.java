package nl.tivek.multiversepowers.character.thor.client.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.ClientThor;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.render.StandaloneModel;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;

// Mjolnir on Thor: it hangs from his belt on his left hip, his right hand holds it once he takes it up, in flight his
// left hand holds it out ahead of him, and thrown it flies on its own (ThrownHammerRenderer). Never an item: only
// its model (models/thor/mjolnir, built by scripts/models/mjolnir). Charged, its runes glow.
public final class ThorHammerLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final StandaloneModel MJOLNIR = new StandaloneModel(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "thor/mjolnir"));
    private static final StandaloneModel RUNES = new StandaloneModel(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "thor/mjolnir_runes"));
    private static final float SIZE = 0.75F;
    // The model's length in blocks: 32 of its pixels.
    private static final float SPAN = 2.0F;
    private static final int RUNE_LIGHT = 0x4FB4FF;
    private static final float LIGHT_UP = 6.0F;
    // Heights along the model, pommel 0 to peak 1: where a fist holds it, the collar under its head, and the middle
    // of its weight, in the head, which it turns about when thrown.
    public static final float GRIP = 0.4F;
    private static final float COLLAR = 0.66F;
    public static final float HEFT = 0.8F;
    private static final float LEAD = 20.0F;

    private ThorHammerLayer(PlayerRenderer renderer) {
        super(renderer);
    }

    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model model : event.getSkins()) {
            EntityRenderer<? extends Player> renderer = event.getSkin(model);
            if (renderer instanceof PlayerRenderer player) {
                player.addLayer(new ThorHammerLayer(player));
            }
        }
    }

    public static void onRegisterModels(ModelEvent.RegisterAdditional event) {
        MJOLNIR.register(event);
        RUNES.register(event);
    }

    // Mjolnir as it is modelled (pommel to peak along +y, its rune faces along z), the point of its handle at the given
    // height on the pose's origin, its runes glowing as brightly as `glow` (0 to 1).
    public static void draw(float at, float glow, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.scale(SIZE / SPAN, SIZE / SPAN, SIZE / SPAN);
        pose.translate(-0.5F, -at * SPAN, -0.5F);
        MJOLNIR.draw(pose, buffers, light);
        if (glow > 0.0F) {
            RUNES.glow(pose, buffers, RUNE_LIGHT, glow);
        }
        pose.popPose();
    }

    // The glow of this Thor's runes: lighting up as his hammer is charged, flickering while it lasts.
    public static float glow(Entity thor, float partialTick) {
        ClientThor.View view = ClientThor.view(thor);
        if (view == null || !view.has(ThorStatePayload.HAMMER_CHARGED)) {
            return 0.0F;
        }
        return (float) Ease.smooth(view.litAge(partialTick) / LIGHT_UP)
                * flicker(thor.getId(), thor.tickCount + partialTick);
    }

    public static float flicker(int seed, float time) {
        return 0.75F + 0.25F * (float) Noise.smooth(seed, time * 0.3);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw,
            float headPitch) {
        if (ClientCharacter.of(player) != GameCharacter.THOR || player.isInvisible()
                || ClientThor.has(player, ThorStatePayload.THROWN)) {
            return;
        }
        PlayerModel<AbstractClientPlayer> model = this.getParentModel();
        float glow = glow(player, partialTick);
        pose.pushPose();
        if (ThorPoses.hammerInLeftHand(player)) {
            // In flight his left fist leads with it: the handle on along his arm, the head out past the fist and
            // leaning ahead of him, a rune face to the front, as if it pulled him along.
            model.translateToHand(HumanoidArm.LEFT, pose);
            pose.translate(1.0F / 16.0F, 0.625F, -0.125F);
            pose.mulPose(Axis.XP.rotationDegrees(LEAD));
            draw(GRIP, glow, pose, buffers, light);
        } else if (ClientThor.has(player, ThorStatePayload.ARMED)) {
            // The fist as the game holds an item in it, then the handle ahead out of it as a sword's blade is, tipped
            // 10 degrees down, the head upright so its striking face leads a blow.
            model.translateToHand(HumanoidArm.RIGHT, pose);
            pose.mulPose(Axis.XP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.YP.rotationDegrees(180.0F));
            pose.translate(1.0F / 16.0F, 0.125F, -0.625F);
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.ZP.rotationDegrees(10.0F));
            draw(GRIP, glow, pose, buffers, light);
        } else {
            model.body.translateAndRotate(pose);
            // The belt sits below the waist, so it turns with the trunk's lower half.
            BentParts.farHalf(model.body, pose);
            pose.translate(ThorPoses.BELT.x / 16.0F, ThorPoses.BELT.y / 16.0F, ThorPoses.BELT.z / 16.0F);
            // Hung by its collar in the belt: head up at the hip, rune faces to the sides, handle down the thigh and
            // a little out from it.
            pose.mulPose(Axis.ZP.rotationDegrees(174.0F));
            pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            draw(COLLAR, glow, pose, buffers, light);
        }
        pose.popPose();
    }
}
