package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechBodyShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechWalk;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.fx.ChaseCamera;
import nl.tivek.multiversepowers.engine.client.fx.Cinematic;
import nl.tivek.multiversepowers.engine.client.fx.FirstPersonEye;
import nl.tivek.multiversepowers.engine.math.Ease;

// Films the pilot's own mech while it builds, cut for cut after the clip, then hands them the view from its cockpit.
// Places are the mech's own (see MechScript): x to its right, y up, z ahead.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechCamera {
    private static final double HANDOVER = 10.0;
    private static final double END = MechScript.DONE + HANDOVER;
    private static final Vec3 PORT_EYE = new Vec3(0.0, MechBodyShapes.PORT_Y, MechBodyShapes.GLASS_Z - 0.3);
    private static final float COCKPIT_PITCH = 6.0F;
    private static final double FOV = 70.0;
    private static final Vec3 CHASE_PIVOT = new Vec3(0.0, 11.5, 0.0);
    private static final double CHASE_DISTANCE = 14.0;

    static {
        Cinematic.add(MechCamera::shot);
        ChaseCamera.add(MechCamera::chase);
        FirstPersonEye.add(MechCamera::eye);
    }

    private MechCamera() {
    }

    // Built, the pilot looks out from just inside the glass of the port: from the seat, the chest round the port
    // walled in the whole view.
    @Nullable
    private static Vec3 eye(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientConstructs.Piloted pilot = player == null ? null : ClientConstructs.piloted(player.getId(), partialTick);
        if (pilot == null || pilot.broke() >= 0.0 || pilot.t() < END) {
            return null;
        }
        MechPose walk = MechWalk.pose(pilot.id(), partialTick);
        return (walk != null ? walk.torso() : pilot.stage()).point(PORT_EYE);
    }

    // In third person the pilot's camera circles the whole mech from above its head, not their own body in its chest.
    @Nullable
    private static ChaseCamera.Rig chase(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return null;
        }
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), partialTick);
        if (pilot == null) {
            return null;
        }
        MechPose walk = MechWalk.pose(pilot.id(), partialTick);
        MechScript.Stage stage = walk != null ? walk.stage() : pilot.stage();
        return new ChaseCamera.Rig(stage.point(CHASE_PIVOT), CHASE_DISTANCE);
    }

    @Nullable
    private static Cinematic.Shot shot(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ClientSettings.mechCinematic()) {
            return null;
        }
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), partialTick);
        if (pilot == null || pilot.broke() >= 0.0 || pilot.t() >= END) {
            return null;
        }
        MechScript.Stage stage = pilot.stage();
        double t = pilot.t();
        Vec3[] take = take(stage, Math.min(t, MechScript.DONE));
        Vec3 eye = take[0];
        Vec3 look = take[1];
        double fov = FOV;
        if (t > MechScript.DONE) {
            double u = Ease.smoother((t - MechScript.DONE) / HANDOVER);
            Vec3 own = PORT_EYE;
            eye = eye.lerp(own, u);
            look = look.lerp(own.add(0.0, -Math.tan(Math.toRadians(COCKPIT_PITCH)) * 10.0, 10.0), u);
            fov = Mth.lerp(u, FOV, Minecraft.getInstance().options.fov().get());
        }
        Vec3 at = stage.point(look);
        return Cinematic.Shot.looking(Cinematic.clear(at, stage.point(eye)), at, 0.0F, fov);
    }

    // Where the camera stands and what it looks at, both in the mech's places.
    private static Vec3[] take(MechScript.Stage stage, double t) {
        Vec3 target = stage.target();
        Vec3 from = stage.pilotFrom();
        if (t < MechScript.STOMP2 + 2) {
            // Side on, the pilot to the left and the target to the right, as the giant foot comes down.
            double u = t / (MechScript.STOMP2 + 2.0);
            Vec3 middle = from.lerp(target, 0.5);
            double far = 3.2 + 0.35 * Math.abs(target.z - from.z);
            return shot(middle.add(far, 1.3, -0.4 * u), middle.add(0.0, 2.0, 0.0));
        }
        if (t < MechScript.STEPS[0] + 2) {
            // Between the pilot and the target: the giant lower legs standing on it, the pilot leaping past.
            double u = (t - MechScript.STOMP2 - 2.0) / (MechScript.STEPS[0] - MechScript.STOMP2);
            return shot(target.add(2.2, 1.4, -5.2 + 0.4 * u), target.add(-0.3, 2.1, 0.0));
        }
        if (t < MechScript.THIGHS) {
            // Low beside the feet as they step off the target.
            double u = (t - MechScript.STEPS[0] - 2.0) / (MechScript.THIGHS - MechScript.STEPS[0] - 2.0);
            return shot(target.add(6.0 - 0.6 * u, 1.2, -2.0 + 1.6 * u), target.scale(0.55).add(0.0, 1.2, 0.0));
        }
        if (t < MechScript.SPREAD) {
            // Low in front: the legs build up, the pilot's light flares, the arms come in and reach down.
            double u = (t - MechScript.THIGHS) / (MechScript.SPREAD - MechScript.THIGHS);
            return shot(new Vec3(2.2 - 0.5 * u, target.y + 1.2, target.z + 6.5 - 0.8 * u),
                    new Vec3(0.0, 4.9 + 0.8 * u, 0.0));
        }
        if (t < MechScript.CLAP - 1) {
            // From high above the pilot: the hands spread wide round the target and swing in.
            double u = (t - MechScript.SPREAD) / (MechScript.CLAP - 1.0 - MechScript.SPREAD);
            return shot(new Vec3(-3.0 + 0.4 * u, 11.5, 6.0), new Vec3(0.0, 3.0, 2.2));
        }
        if (t < MechScript.RISE) {
            // Low and close behind the target: the clap, the squeeze and the hands letting go.
            double u = (t - MechScript.CLAP + 1.0) / (MechScript.RISE - MechScript.CLAP + 1.0);
            return shot(target.add(0.9, 1.3 + 0.3 * u, 2.9 + 1.6 * u), target.add(-0.2, 1.4 + 0.9 * u, -1.0));
        }
        if (t < MechScript.RISE + 11) {
            // Low behind the target, looking up while the arms rise and the shoulders build.
            double u = (t - MechScript.RISE) / 11.0;
            return shot(target.add(-0.9, 0.6, 2.4 + 0.5 * u), new Vec3(0.0, 6.9, 0.0));
        }
        if (t < MechScript.HEAD_FORM) {
            // Close on the chest: the pilot in the cockpit while the arms join the shoulders.
            double u = (t - MechScript.RISE - 11.0) / (MechScript.HEAD_FORM - MechScript.RISE - 11.0);
            return shot(new Vec3(0.0, 7.8, 4.7 + 0.4 * u), new Vec3(0.0, 7.7, 1.2));
        }
        if (t < MechScript.CRASH + 1) {
            // Low in front: the head forms high above the mech with its arms spread, then drops.
            double u = (t - MechScript.HEAD_FORM) / (MechScript.CRASH + 1.0 - MechScript.HEAD_FORM);
            double down = Ease.smooth((t - MechScript.HEAD_DROP) / (MechScript.CRASH - MechScript.HEAD_DROP));
            return shot(new Vec3(-1.6, target.y + 1.5, target.z + 7.0 - 0.4 * u),
                    new Vec3(0.0, 6.8 - 2.4 * down, 1.8));
        }
        if (t < MechScript.HEAD_LIFT + 1) {
            // Close and low by the crater, the mech's legs beyond it, until the head tumbles up out of it.
            double u = (t - MechScript.CRASH - 1.0) / (MechScript.HEAD_LIFT - MechScript.CRASH);
            return shot(target.add(3.4, 1.3, 4.2 - 0.3 * u), target.add(0.6, 2.4 + 0.8 * u, -2.2));
        }
        if (t < MechScript.HEAD_LAND + 1) {
            // Low in front looking up: the head comes tumbling down onto the mech.
            double u = (t - MechScript.HEAD_LIFT - 1.0) / (MechScript.HEAD_LAND - MechScript.HEAD_LIFT);
            return shot(new Vec3(-1.2, target.y + 0.8, target.z + 4.8 + 0.3 * u), new Vec3(0.0, 8.2 + 1.4 * u, 1.0));
        }
        // Close on the chest from its right: the head locks on and the arms spread wide.
        double u = (t - MechScript.HEAD_LAND - 1.0) / (MechScript.DONE - MechScript.HEAD_LAND - 1.0);
        return shot(new Vec3(1.4 - 0.2 * u, 6.8, 5.9 + 0.3 * u), new Vec3(0.1, 7.9, 1.0));
    }

    private static Vec3[] shot(Vec3 eye, Vec3 look) {
        return new Vec3[] { eye, look };
    }

    // Through the handover the pilot is turned to look straight out of the cockpit, so their own view takes over there.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !ClientSettings.mechCinematic()) {
            return;
        }
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), 0.0F);
        if (pilot == null || pilot.broke() >= 0.0 || pilot.t() < MechScript.DONE || pilot.t() > END + 1.0) {
            return;
        }
        Vec3 ahead = pilot.stage().ahead();
        float yaw = (float) Math.toDegrees(Math.atan2(-ahead.x, ahead.z));
        player.setYRot(yaw);
        player.setXRot(COCKPIT_PITCH);
        player.yRotO = yaw;
        player.xRotO = COCKPIT_PITCH;
        player.yHeadRot = yaw;
        player.yHeadRotO = yaw;
    }
}
