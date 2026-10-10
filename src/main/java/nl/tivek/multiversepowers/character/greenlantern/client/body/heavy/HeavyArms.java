package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.body.fist.ClientFists;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordFirstPerson;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.client.render.entity.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// A heavy weapon on the body: as others see him, his trunk leaning and turning into each move, both hands on the
// weapon where its keys put it (Stance), the whole body spinning in a whirlwind; from his own eyes the weapon swung
// before him with both arms on it. A revving chainsaw shakes his hands and view, and holds him where he stands while
// it grinds into something.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HeavyArms {
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f[] POLES = { new Vector3f(-0.6F, 0.6F, 0.5F), new Vector3f(0.6F, 0.6F, 0.5F) };
    private static final Vector3f HIPS = new Vector3f();
    private static final Vector3f NECK = new Vector3f();
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final HeavyPoses.Pose PAST = new HeavyPoses.Pose();
    // From his own eyes: a chest pixel this far in view space, raised this much less, and the whole moved up and in.
    private static final float VIEW = 1.3F / 16.0F;
    private static final float VIEW_Y = 0.85F;
    private static final float VIEW_UP = 0.03F;
    private static final float VIEW_IN = -0.26F;
    private static final double VIEW_SIZE = 0.85;
    private static final double VIEW_TOP = -0.12;
    private static final double VIEW_NEAR = -0.62;
    // The chainsaw, held low with its bar straight ahead, is lifted into sight, moved right and turned across a
    // little, so his hands do not hide it.
    private static final double[] VIEW_LIFT = { 0.0, 0.2 };
    private static final double WHIRL_SEEN = 0.75;
    private static final double[] VIEW_SIDE = { 0.0, 0.16 };
    private static final float[] VIEW_TURN = { 0.0F, 0.4F };
    private static final Vector3f REST_FROM = new Vector3f(0.75F, -1.1F, -0.15F);

    private HeavyArms() {
    }

    // A Poses layer: the weapon's pose over the game's own.
    public static boolean pose(PlayerModel<?> model, LivingEntity entity) {
        if (!EntityPass.inWorld()) {
            return false;
        }
        ClientHeavy.Held held = ClientHeavy.view(entity);
        if (held == null) {
            return false;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        HeavyPoses.Pose pose = HeavyPoses.of(held, partialTick);
        if (pose.weight < 1.0E-3F) {
            held.posed = held.posed && held.brokeAt >= 0.0;
            return false;
        }
        float w = pose.weight;
        Vector3f[] feet = { Stance.foot(model, true, new Vector3f()), Stance.foot(model, false, new Vector3f()) };
        HIPS.set(0.0F, Stance.HIP_Y + pose.drop * w, 0.0F);
        LEAN.rotationZYX(pose.roll * 0.6F * w, pose.twist * 0.4F * w, pose.pitch * w);
        WAIST.rotationZYX(pose.roll * 0.4F * w, pose.twist * 0.6F * w, 0.0F);
        Stance.trunk(model, HIPS, LEAN, WAIST);
        for (int side = 0; side < 2; side++) {
            Stance.leg(model, side == 0, feet[side], KNEE);
        }
        Stance.neck(NECK);
        Stance.chest(CHEST);
        Frame frame = chestFrame(held.weapon, pose);
        float time = (float) ClientHeavy.now(partialTick);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            Vec3 grip = frame.at(HeavyPainter.grip(held.weapon, right).x, HeavyPainter.grip(held.weapon, right).y,
                    HeavyPainter.grip(held.weapon, right).z);
            Vector3f target = CHEST.transform(new Vector3f((float) grip.x, (float) grip.y, (float) grip.z)).add(NECK);
            target.add(pose.shake * Mth.sin(time * 2.9F + side), pose.shake * Mth.cos(time * 3.7F), 0.0F);
            Vector3f hand = Stance.hand(model, right, new Vector3f());
            hand.lerp(target, w);
            Stance.arm(model, right, hand, POLES[side]);
        }
        // Drawn the way its keys turn it, in his right hand where that really is.
        Frame seen = toModel(frame, CHEST, NECK);
        Vec3 grip = HeavyPainter.grip(held.weapon, true);
        Vector3f hand = Stance.hand(model, true, new Vector3f()).div(16.0F);
        Vec3 off = vec(hand).subtract(seen.at(grip.x, grip.y, grip.z));
        held.frame = new Frame(seen.center().add(off), seen.right(), seen.up(), seen.forward(), seen.scale());
        held.chest.set(CHEST);
        held.neck.set(NECK);
        held.posed = true;
        return true;
    }

    // The weapon where a pose puts it, in the chest's frame (pixels).
    static Frame chestFrame(int weapon, HeavyPoses.Pose pose) {
        Vector3f way = pose.way(new Vector3f());
        return HeavyPainter.placed(weapon, new Vec3(pose.middle.x, pose.middle.y, pose.middle.z),
                new Vec3(way.x, way.y, way.z), new Vec3(pose.up.x, pose.up.y, pose.up.z),
                16.0 * HeavyPainter.size(weapon));
    }

    // A frame in the chest's pixels as his model's space draws it (blocks).
    static Frame toModel(Frame frame, Quaternionf chest, Vector3f neck) {
        Vector3f center = chest.transform(vec(frame.center())).add(neck).div(16.0F);
        return new Frame(vec(center), vec(chest.transform(vec(frame.right()))), vec(chest.transform(vec(frame.up()))),
                vec(chest.transform(vec(frame.forward()))), frame.scale() / 16.0);
    }

    // Where the weapon was `back` ticks ago, in his model's space.
    static Frame past(ClientHeavy.Held held, float partialTick, double back) {
        HeavyPoses.at(held.weapon, held.move, held.age(partialTick) - back, PAST);
        return toModel(chestFrame(held.weapon, PAST), held.chest, held.neck);
    }

    // Registered with BodyTurns: a whirlwind spins his whole body round and round.
    public static void turnBody(AbstractClientPlayer player, PoseStack pose, float scale) {
        ClientHeavy.Held held = ClientHeavy.view(player);
        if (held == null) {
            return;
        }
        float spin = HeavyPoses.spin(held, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (spin != 0.0F) {
            pose.mulPose(Axis.YP.rotation(spin % (float) (Math.PI * 2.0)));
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !SwordFirstPerson.handsFree(player)) {
            return;
        }
        ClientHeavy.Held held = ClientHeavy.view(player);
        if (held == null) {
            return;
        }
        float partialTick = event.getPartialTick();
        HeavyPoses.Pose pose = HeavyPoses.of(held, partialTick);
        double apart = held.apart(partialTick);
        if (pose.weight < 1.0E-3F && apart < 0.0) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        float time = (float) ClientHeavy.now(partialTick);
        float turn = whirlSeen(held, partialTick);
        Frame frame = viewFrame(held.weapon, pose, time, turn);
        PoseStack stack = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        float w = pose.weight;
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? 1.0F : -1.0F;
            Vec3 grip = HeavyPainter.grip(held.weapon, side == 0);
            Vec3 at = frame.at(grip.x, grip.y, grip.z);
            Vector3f hand = new Vector3f(side == 0 ? FirstPersonArm.HAND_RIGHT : FirstPersonArm.HAND_LEFT);
            hand.lerp(new Vector3f((float) at.x, (float) at.y, (float) at.z), w);
            FirstPersonArm.arm(stack, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, sign,
                    hand, new Vector3f(REST_FROM.x * sign, REST_FROM.y, REST_FROM.z));
        }
        LanternPainter painter = LanternPainter.hand(stack, player.tickCount + partialTick);
        HeavyPainter.weapon(painter, held.weapon, frame, held.formed(partialTick), apart,
                HeavyPoses.revving(held, partialTick), time, player.getId());
        HeavyPainter.trail(painter, back -> {
            HeavyPoses.at(held.weapon, held.move, held.age(partialTick) - back, PAST);
            return viewFrame(held.weapon, PAST, time, turn);
        }, HeavyPainter.trailing(held, partialTick));
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    // The weapon in his own view: the pose's chest frame turned as his trunk turns, then laid out before his eyes.
    private static Frame viewFrame(int weapon, HeavyPoses.Pose pose, float time, float turn) {
        Quaternionf body = new Quaternionf().rotationZYX(pose.roll * 0.5F, pose.twist * 0.7F, pose.pitch * 0.6F);
        Vector3f middle = body.transform(new Vector3f(pose.middle));
        middle.add(pose.shake * Mth.sin(time * 2.9F), pose.shake * Mth.cos(time * 3.7F), 0.0F);
        Vector3f way = body.transform(pose.way(new Vector3f()));
        Vector3f up = body.transform(new Vector3f(pose.up));
        // Kept out before his eyes and low: a weapon raised over his head would bring his arms into his face.
        Vec3 seenMiddle = new Vec3(-middle.x * VIEW, Math.min(VIEW_TOP, -middle.y * VIEW * VIEW_Y + VIEW_UP)
                + VIEW_LIFT[weapon],
                Math.min(VIEW_NEAR, middle.z * VIEW + VIEW_IN)).add(VIEW_SIDE[weapon], 0.0, 0.0);
        Vec3 seenWay = new Vec3(-way.x, -way.y * VIEW_Y, way.z).yRot(VIEW_TURN[weapon] + turn);
        Vec3 seenUp = new Vec3(-up.x, -up.y, up.z).yRot(VIEW_TURN[weapon] + turn);
        seenMiddle = seenMiddle.yRot(turn);
        return HeavyPainter.placed(weapon, seenMiddle, seenWay, seenUp, VIEW_SIZE * HeavyPainter.size(weapon));
    }

    // Whirling, the axe swung out at his side is turned forward into his own view.
    private static float whirlSeen(ClientHeavy.Held held, float partialTick) {
        if (held.weapon != AXE || held.move != WHIRL && held.move != WHIRL_OUT) {
            return 0.0F;
        }
        double age = held.age(partialTick);
        double on = held.move == WHIRL ? Ease.smooth(age / 4.0) : 1.0 - Ease.smooth(age / 4.0);
        return (float) (WHIRL_SEEN * on);
    }

    // A whirlwind turns his own view round with his body; a revving chainsaw shakes it.
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || event.getCamera().getEntity() != player) {
            return;
        }
        ClientHeavy.Held held = ClientHeavy.view(player);
        if (held == null) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float spin = HeavyPoses.spin(held, partialTick);
        if (spin != 0.0F && !event.getCamera().isDetached()) {
            event.setYaw(event.getYaw() - (float) Math.toDegrees(spin % (Math.PI * 2.0)));
        }
        if (held.weapon != SAW) {
            return;
        }
        float rev = (float) HeavyPoses.revving(held, partialTick);
        if (rev <= 0.0F) {
            return;
        }
        float time = (float) ClientHeavy.now(partialTick);
        event.setRoll(event.getRoll() + 0.5F * rev * Mth.sin(time * 2.7F));
        event.setPitch(Mth.clamp(event.getPitch() + 0.35F * rev * Mth.cos(time * 3.3F), -90.0F, 90.0F));
    }

    // While the chainsaw grinds into something he stands his ground.
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onMovement(MovementInputUpdateEvent event) {
        ClientHeavy.Held held = ClientHeavy.view(event.getEntity());
        if (held == null || held.weapon != SAW || held.brokeAt >= 0.0) {
            return;
        }
        double age = held.age(0.0F);
        if (held.move == REND && age >= LOOP_FROM - 1 || held.move == IMPALE && age >= 4.0 && age < EJECT) {
            event.getInput().forwardImpulse = 0.0F;
            event.getInput().leftImpulse = 0.0F;
        }
    }

    // Times count from the world's own clock: another world starts it over.
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientHeavy.clear();
        ClientFists.clear();
    }

    private static Vector3f vec(Vec3 v) {
        return new Vector3f((float) v.x, (float) v.y, (float) v.z);
    }

    private static Vec3 vec(Vector3f v) {
        return new Vec3(v.x, v.y, v.z);
    }
}
