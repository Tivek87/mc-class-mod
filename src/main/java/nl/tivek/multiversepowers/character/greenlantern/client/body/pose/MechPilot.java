package nl.tivek.multiversepowers.character.greenlantern.client.body.pose;

import javax.annotation.Nullable;
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
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechDrive;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechWalk;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.fx.Cinematic;
import nl.tivek.multiversepowers.engine.client.render.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

// The mech's pilot: standing in its chest, their arms doing what its arms do until they take hold of its sticks.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechPilot {
    private static final float HEAD_TURN = 1.25F;
    private static final float HAND_AHEAD = 0.2F;
    private static final float ARM_OUT = 0.25F;
    private static final float ARM_DOWN = 0.55F;
    private static final float ARM_BACK = 0.5F;
    private static final double IDLE = MechScript.SETTLED + 20.0;
    private static final float SIT_LEGS = 1.41F;
    private static final float SIT_SPREAD = 0.2F;
    private static final double SHOULDER_X = 0.3125;
    private static final double SHOULDER_Y = 1.375;
    private static final double BUTTON_TOP = 0.08;
    private static final double BUTTON_PUSH = 0.05;
    private static final int DRIVE_AFTER = 2;

    private MechPilot() {
    }

    public static boolean pose(PlayerModel<?> model, LivingEntity entity) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(entity.getId(), partialTick);
        if (pilot == null) {
            return false;
        }
        double t = pilot.broke() >= 0.0 ? IDLE : pilot.t();
        MechPose walk = walking(pilot, partialTick);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            Vec3 way = walk != null ? toHand(walk, right ? 1 : -1) : MechMoves.pilotArm(right, t).way();
            float[] turn = reach(way);
            ModelPart arm = right ? model.rightArm : model.leftArm;
            arm.xRot = turn[0];
            arm.yRot = 0.0F;
            arm.zRot = right ? turn[1] : -turn[1];
        }
        float leap = (float) (Ease.smooth((t - MechScript.LEAP) / 4.0)
                * (1.0 - Ease.smooth((t - MechScript.ABOARD + 6.0) / 6.0)));
        float sit = pilot.broke() >= 0.0 ? 0.0F : (float) Ease.smooth((t - MechScript.SIT) / 6.0);
        float stance = (t < MechScript.LEAP ? 0.14F : 0.06F) * (1.0F - sit);
        model.rightLeg.xRot = -0.95F * leap - SIT_LEGS * sit;
        model.leftLeg.xRot = -0.3F * leap - SIT_LEGS * sit;
        model.rightLeg.yRot = SIT_SPREAD * sit;
        model.leftLeg.yRot = -SIT_SPREAD * sit;
        model.rightLeg.zRot = stance;
        model.leftLeg.zRot = -stance;
        model.body.xRot = 0.0F;
        model.head.yRot = Mth.clamp(model.head.yRot, -HEAD_TURN, HEAD_TURN);
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
        return true;
    }

    // The arm's own turn from hanging down to pointing along way (the mech's places, x outwards): x first, then z.
    private static float[] reach(Vec3 way) {
        return new float[] { (float) Math.asin(Mth.clamp(-way.z, -1.0, 1.0)), (float) Math.atan2(way.x, -way.y) };
    }

    @Nullable
    private static MechPose walking(ClientConstructs.Piloted pilot, float partialTick) {
        return pilot.t() >= MechScript.SETTLED && pilot.broke() < 0.0 ? MechWalk.pose(pilot.id(), partialTick) : null;
    }

    // Where a hand is (side 1 right, -1 left): on its lever, or on its way to a button and pressing it.
    private static Vec3 hand(MechPose pose, int side) {
        Vec3 grip = pose.grip(side);
        int button = pose.button(side);
        if (button < 0) {
            return grip;
        }
        double press = pose.press();
        Vec3 top = pose.torso().point(MechScript.BUTTONS[button].add(0.0, BUTTON_TOP, 0.0));
        Vec3 at = grip.lerp(top, Ease.smooth(Math.min(1.0, press / 0.8)));
        return at.subtract(pose.torso().up().scale(BUTTON_PUSH * Math.max(0.0, press - 0.8) / 0.2));
    }

    // Which way a seated pilot's arm points to reach its hand, in the mech's places with x outwards for either arm.
    private static Vec3 toHand(MechPose pose, int side) {
        Vec3 shoulder = MechScript.COCKPIT.add(side * SHOULDER_X, SHOULDER_Y, 0.0);
        Vec3 way = pose.torso().local(hand(pose, side)).subtract(shoulder);
        return new Vec3(way.x * side, way.y, way.z).normalize();
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), event.getPartialTick());
        if (pilot == null) {
            return;
        }
        MechPose walk = walking(pilot, event.getPartialTick());
        float yaw = walk != null ? walk.torso().yaw() : pilot.stage().yaw();
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
            MechDrive.forget();
            return;
        }
        Input input = event.getInput();
        Vec3 feet = pilot.feet();
        if (pilot.broke() < 0.0 && pilot.t() >= MechScript.SETTLED + DRIVE_AFTER) {
            MechDrive.drive(player, pilot.id(), pilot.stage(), input, pilot.blow().striking());
            MechWalk.step(pilot.id(), MechDrive.stage(pilot.id()), player.getId(), player.getYRot(),
                    player.getXRot(), pilot.blow());
            MechPose walk = MechWalk.latest(pilot.id());
            if (walk != null) {
                feet = walk.seat();
            }
        } else {
            MechDrive.forget();
        }
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.jumping = false;
        input.shiftKeyDown = false;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        player.setPos(feet.x, feet.y, feet.z);
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
        if (pilot == null) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND || Cinematic.rolling()) {
            return;
        }
        double t = pilot.broke() >= 0.0 ? IDLE : pilot.t();
        MechPose walk = walking(pilot, event.getPartialTick());
        Camera camera = minecraft.gameRenderer.getMainCamera();
        // Hands draw with a fixed 70 degree view; stretched by this, they meet the levers the world draws.
        double stretch = Math.tan(Math.toRadians(35.0)) / Math.tan(Math.toRadians(minecraft.options.fov().get() * 0.5));
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        for (int side = -1; side <= 1; side += 2) {
            if (walk == null && MechMoves.pilotArm(side > 0, t).onStick() < 0.5) {
                continue;
            }
            Vec3 grip = walk != null ? hand(walk, side) : pilot.stage().point(side * MechScript.LEVER.x,
                    MechScript.LEVER.y + MechScript.LEVER_LENGTH, MechScript.LEVER.z);
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
