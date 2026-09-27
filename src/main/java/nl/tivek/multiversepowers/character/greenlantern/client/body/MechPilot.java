package nl.tivek.multiversepowers.character.greenlantern.client.body;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.client.render.FirstPersonArm;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechPilot {
    private static final double SHOULDER_UP = 22.0 / 16.0;
    private static final double SHOULDER_OUT = 5.0 / 16.0;
    private static final float HEAD_TURN = 1.25F;
    private static final float FLOAT_ARM_X = -0.3F;
    private static final float FLOAT_ARM_Z = 0.8F;
    private static final float SIT_LEG_X = -1.4137167F;
    private static final float SIT_LEG_Y = 0.31415927F;
    private static final float SIT_LEG_Z = 0.07853982F;
    private static final float[] GRIP_TURN = gripTurn();
    private static final float HAND_AHEAD = 0.2F;
    private static final float ARM_OUT = 0.25F;
    private static final float ARM_DOWN = 0.55F;
    private static final float ARM_BACK = 0.5F;

    private MechPilot() {
    }

    // The arm's own turn from hanging down to reaching the lever's grip: x first, then z (see LanternArms.reach).
    private static float[] gripTurn() {
        Vec3 way = new Vec3(MechScript.GRIP.x - SHOULDER_OUT, MechScript.GRIP.y - MechScript.SEAT_Y - SHOULDER_UP,
                MechScript.GRIP.z).normalize();
        double x = -way.x;
        double y = -way.y;
        double z = -way.z;
        return new float[] { (float) Math.asin(Mth.clamp(z, -1.0, 1.0)), (float) Math.atan2(-x, y) };
    }

    public static void pose(PlayerModel<?> model, LivingEntity entity) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(entity.getId(), partialTick);
        if (pilot == null) {
            return;
        }
        float sit = (float) pilot.sit();
        turn(model.rightArm, Mth.lerp(sit, FLOAT_ARM_X, GRIP_TURN[0]), Mth.lerp(sit, FLOAT_ARM_Z, GRIP_TURN[1]));
        turn(model.leftArm, Mth.lerp(sit, FLOAT_ARM_X, GRIP_TURN[0]), -Mth.lerp(sit, FLOAT_ARM_Z, GRIP_TURN[1]));
        model.rightLeg.xRot = Mth.lerp(sit, 0.15F, SIT_LEG_X);
        model.rightLeg.yRot = Mth.lerp(sit, 0.0F, SIT_LEG_Y);
        model.rightLeg.zRot = Mth.lerp(sit, 0.06F, SIT_LEG_Z);
        model.leftLeg.xRot = Mth.lerp(sit, -0.1F, SIT_LEG_X);
        model.leftLeg.yRot = Mth.lerp(sit, 0.0F, -SIT_LEG_Y);
        model.leftLeg.zRot = Mth.lerp(sit, -0.06F, -SIT_LEG_Z);
        model.body.xRot = 0.0F;
        model.head.yRot = Mth.clamp(model.head.yRot, -HEAD_TURN, HEAD_TURN);
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    private static void turn(ModelPart arm, float x, float z) {
        arm.xRot = x;
        arm.yRot = 0.0F;
        arm.zRot = z;
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), event.getPartialTick());
        if (pilot == null) {
            return;
        }
        Vec3 ahead = pilot.stage().ahead();
        float yaw = (float) Math.toDegrees(Math.atan2(-ahead.x, ahead.z));
        player.yBodyRot = yaw;
        player.yBodyRotO = yaw;
    }

    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), 0.0F);
        if (pilot == null) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
        Vec3 seat = pilot.seat();
        player.setPos(seat.x, seat.y, seat.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.resetFallDistance();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), event.getPartialTick());
        if (pilot == null || pilot.sit() < 0.5) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Camera camera = minecraft.gameRenderer.getMainCamera();
        // Hands draw with a fixed 70 degree view; stretched by this, they meet the levers the world draws.
        double stretch = Math.tan(Math.toRadians(35.0)) / Math.tan(Math.toRadians(minecraft.options.fov().get() * 0.5));
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        for (int side = -1; side <= 1; side += 2) {
            Vec3 grip = pilot.stage().point(side * MechScript.GRIP.x, MechScript.GRIP.y, MechScript.GRIP.z);
            Vector3f hand = seen(camera, grip, stretch);
            if (hand.z() > -HAND_AHEAD) {
                continue;
            }
            // From the grip down out of view, as first-person arms go: from the real shoulder it would fill the screen.
            Vector3f from = new Vector3f(hand).add(side * ARM_OUT, -ARM_DOWN, ARM_BACK);
            FirstPersonArm.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                    renderer, side, hand, from);
        }
    }

    private static Vector3f seen(Camera camera, Vec3 world, double stretch) {
        Vec3 way = world.subtract(camera.getPosition());
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        double x = -(way.x * left.x() + way.y * left.y() + way.z * left.z());
        double y = way.x * up.x() + way.y * up.y() + way.z * up.z();
        double z = -(way.x * look.x() + way.y * look.y() + way.z * look.z());
        return new Vector3f((float) (x * stretch), (float) (y * stretch), (float) z);
    }
}
