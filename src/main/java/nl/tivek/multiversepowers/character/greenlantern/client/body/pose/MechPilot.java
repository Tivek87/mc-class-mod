package nl.tivek.multiversepowers.character.greenlantern.client.body.pose;

import com.mojang.blaze3d.vertex.PoseStack;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
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
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechDrive;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechWalk;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBuild;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.fx.Cinematic;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The mech's pilot: casting it and leaping into its chest, working its arms with their own there, then sitting down
// to take its sticks (PilotBody poses the whole body).
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechPilot {
    private static final float HEAD_TURN = 1.25F;
    private static final float HAND_AHEAD = 0.2F;
    private static final float ARM_OUT = 0.25F;
    private static final float ARM_DOWN = 0.55F;
    private static final float ARM_BACK = 0.5F;
    private static final double IDLE = MechScript.SETTLED + 20.0;
    private static final double BUTTON_TOP = 0.08;
    private static final double BUTTON_PUSH = 0.05;
    private static final int DRIVE_AFTER = 2;
    // The left hand takes its stick a little after the right; the ring fist aims at the middle of a foot's shin.
    private static final double LEFT_LATER = 2.5;
    private static final double AIM_UP = 1.2;
    private static final Vector3f[] GRIPS = { new Vector3f(), new Vector3f() };
    private static final Vector3f AIM = new Vector3f();
    private static final double[] STICKS = new double[2];
    private static final Matrix3f TO = new Matrix3f();
    private static final Matrix3f FROM = new Matrix3f();
    private static final Quaternionf TILT = new Quaternionf();

    private MechPilot() {
    }

    public static boolean pose(PlayerModel<?> model, LivingEntity entity) {
        if (!EntityPass.inWorld()) {
            return false;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(entity.getId(), partialTick);
        if (pilot == null) {
            return false;
        }
        double t = pilot.t();
        MechPose walk = walking(pilot, partialTick);
        MechScript.Stage torso = pilot.torso();
        for (int side = 0; side < 2; side++) {
            int way = side == 0 ? 1 : -1;
            Vec3 grip = walk != null ? hand(walk, way) : lever(torso, way, t);
            // The body is drawn turned with the chest (turnBody): its hands go where the grips are before that turn.
            PilotBody.toModel(entity, partialTick, walk != null ? grip
                    : untilted(pilot, entity.getPosition(partialTick), grip), GRIPS[side]);
        }
        Vec3 aim = aim(pilot);
        double push = walk != null ? (walk.push(1) + walk.push(-1)) * 0.5
                : (MechBuild.lever(true, t) + MechBuild.lever(false, t)) * 0.5;
        PilotBody.pose(model, t, pilot.broke(), GRIPS, onStick(t, STICKS), raised(t), push,
                aim == null ? null : PilotBody.toModel(entity, partialTick, aim, AIM));
        model.head.yRot = Mth.clamp(model.head.yRot, -HEAD_TURN, HEAD_TURN);
        model.hat.copyFrom(model.head);
        return true;
    }

    // How far each hand (right, left) has taken hold of its stick, `t` into the build: the right one first.
    private static double[] onStick(double t, double[] out) {
        out[0] = Ease.smooth((t - MechScript.GRIP) / 5.0);
        out[1] = Ease.smooth((t - MechScript.GRIP - LEFT_LATER) / 5.0);
        return out;
    }

    // How far the ring fist is thrown up with the mech's own as its head locks on: fast, held, then back to its stick.
    private static double raised(double t) {
        return t < MechScript.LOCK - 1 ? 0.0
                : Ease.backOut((t - (MechScript.LOCK - 1)) / 3.5) * (1.0 - Ease.smooth((t - MechScript.DONE) / 6.0));
    }

    // The grip of a stick as the build throws it (MechBuild.lever).
    private static Vec3 lever(MechScript.Stage torso, int side, double t) {
        return MechPose.grip(torso, side, MechBuild.lever(side > 0, t));
    }

    // How the chest the pilot rides is turned from the ground it is built on, in the world: the whole body is drawn
    // turned so about its feet (turnBody).
    private static Quaternionf tilt(ClientConstructs.Piloted pilot, Quaternionf out) {
        MechScript.Stage stage = pilot.stage();
        MechScript.Stage torso = pilot.torso();
        TO.set(axes(torso.right()), axes(torso.up()), axes(torso.ahead()));
        FROM.set(axes(stage.right()), axes(stage.up()), axes(stage.ahead()));
        return out.setFromNormalized(TO.mul(FROM.transpose()));
    }

    private static Vector3f axes(Vec3 way) {
        return new Vector3f((float) way.x, (float) way.y, (float) way.z);
    }

    // A point of the world taken back round the drawn feet against the chest's turn: where it lies for the body before
    // turnBody turns it.
    private static Vec3 untilted(ClientConstructs.Piloted pilot, Vec3 feet, Vec3 world) {
        Vector3f way = new Vector3f((float) (world.x - feet.x), (float) (world.y - feet.y), (float) (world.z - feet.z));
        tilt(pilot, TILT).conjugate().transform(way);
        return feet.add(way.x(), way.y(), way.z());
    }

    // Building, the pilot leans with the chest they ride in: the whole body turned about the feet as the torso is
    // turned from the ground.
    public static void turnBody(AbstractClientPlayer player, PoseStack pose, float scale) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), partialTick);
        if (pilot == null || pilot.broke() >= 0.0 || pilot.t() >= MechScript.SETTLED) {
            return;
        }
        pose.mulPose(tilt(pilot, TILT));
    }

    // What the ring fist is thrown at while the feet come down: each foot as it forms high above the target and falls
    // onto it, the right one and then the left; null once both are down.
    @Nullable
    private static Vec3 aim(ClientConstructs.Piloted pilot) {
        double t = pilot.t();
        if (pilot.broke() >= 0.0 || t > MechScript.STOMP2 + 4.0) {
            return null;
        }
        MechScript.Stage stage = pilot.stage();
        Vec3 right = foot(stage, true, t);
        if (t < MechScript.STOMP + 1.0) {
            return right;
        }
        return right.lerp(foot(stage, false, t), Ease.smooth((t - MechScript.STOMP - 1.0) / 3.0));
    }

    private static Vec3 foot(MechScript.Stage stage, boolean right, double t) {
        return stage.point(MechScript.ankle(right, stage, t)).add(stage.up().scale(AIM_UP));
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
                    player.getXRot(), pilot.blow(), MechDrive.climb(pilot.id()));
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
        double[] sticks = onStick(t, STICKS);
        for (int side = -1; side <= 1; side += 2) {
            if (walk == null && (side > 0 ? sticks[0] * (1.0 - raised(t)) : sticks[1]) < 0.5) {
                continue;
            }
            Vec3 grip = walk != null ? hand(walk, side) : lever(pilot.torso(), side, t);
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
