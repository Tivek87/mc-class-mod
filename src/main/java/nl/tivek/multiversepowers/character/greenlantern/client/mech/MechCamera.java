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
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBuild;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechHead;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.fx.ChaseCamera;
import nl.tivek.multiversepowers.engine.client.fx.Cinematic;
import nl.tivek.multiversepowers.engine.client.fx.FirstPersonEye;
import nl.tivek.multiversepowers.engine.math.Ease;

// Films the pilot's own mech while it builds, the pilot as much as the mech, in a few long shots flown like a drone and
// cut on its big beats: close on them gathering the ring's light, low behind them as the feet slam down, alongside and
// up through the leap, one long flight round the mech as its arms clap and in on them sitting down in its chest, low
// under the head falling, in on the pilot at the sticks and back out as the head is flung, and low in front as it locks
// on; then it hands them the view from the cockpit. Places are the mech's own (see MechScript): x to its right, y up,
// z ahead.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechCamera {
    private static final double HANDOVER = 10.0;
    private static final double END = MechScript.DONE + HANDOVER;
    private static final Vec3 PORT_EYE = new Vec3(0.0, MechBodyShapes.PORT_Y, MechBodyShapes.GLASS_Z - 0.3);
    private static final float COCKPIT_PITCH = 6.0F;
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
    // walled in the whole view. Building, their eye leans with the chest they ride (MechPilot.turnBody).
    @Nullable
    private static Vec3 eye(float partialTick) {
        LocalPlayer player = Minecraft.getInstance().player;
        ClientConstructs.Piloted pilot = player == null ? null : ClientConstructs.piloted(player.getId(), partialTick);
        if (pilot == null || pilot.broke() >= 0.0) {
            return null;
        }
        if (pilot.t() < END) {
            return pilot.feet().add(pilot.torso().up().scale(player.getEyeHeight()));
        }
        MechPose walk = MechWalk.pose(pilot.id(), partialTick);
        return (walk != null ? walk.torso() : pilot.torso()).point(PORT_EYE);
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
        Take take = take(stage, Math.min(t, MechScript.DONE));
        Vec3 eye = take.eye();
        Vec3 at = take.at();
        double fov = take.fov();
        double roll = take.roll();
        if (t > MechScript.DONE) {
            // The chest still settles here: end in its own port, where the pilot's view takes over.
            double u = Ease.smoother((t - MechScript.DONE) / HANDOVER);
            MechScript.Stage torso = pilot.torso();
            eye = eye.lerp(torso.point(PORT_EYE), u);
            at = at.lerp(torso.point(PORT_EYE.add(0.0, -Math.tan(Math.toRadians(COCKPIT_PITCH)) * 10.0, 10.0)), u);
            fov = Mth.lerp(u, fov, Minecraft.getInstance().options.fov().get());
            roll *= 1.0 - u;
        }
        return Cinematic.Shot.looking(Cinematic.clear(at, eye), at, (float) roll, fov);
    }

    // Where the camera stands and what it looks at (in the world), its roll (degrees) and how wide it sees: a few long
    // shots cut on the build's beats, each flown through without a stop, floating on the drone's hover and punching in
    // a little on each blow.
    private static Take take(MechScript.Stage stage, double t) {
        MechScript.Stage torso = MechBuild.torso(stage, t);
        Vec3 me = torso.point(MechScript.pilot(stage, t));
        Vec3 target = stage.target();
        Vec3 from = stage.pilotFrom();
        Vec3 up = stage.up();
        Vec3 drift = hover(t);
        double sway = 0.5 * Math.sin(t * 0.07);
        if (t < MechScript.FOOT_DROP) {
            // Close in front of the pilot gathering the ring's light and throwing it up, slowly closing in.
            double u = glide(t, 0.0, MechScript.FOOT_DROP);
            return new Take(round(stage, me.add(up.scale(1.2)), Mth.lerp(u, 0.5, 0.85), Mth.lerp(u, 2.5, 2.0),
                    Mth.lerp(u, 0.05, 0.15)).add(drift), near(stage, me, new Vec3(0.1, 1.45, 0.1)), sway,
                    Mth.lerp(u, 44.0, 38.0));
        }
        if (t < MechScript.LEAP - 2) {
            // Over their right shoulder, rising slowly round their back, past them at the feet slamming down onto the
            // target.
            double u = glide(t, MechScript.FOOT_DROP, MechScript.LEAP - 2);
            Vec3 past = near(stage, me, new Vec3(0.0, 3.2, Math.min(0.5 * (target.z - from.z), 4.5)));
            double kick = punch(t, MechScript.STOMP, 9.0) + punch(t, MechScript.STOMP2, 9.0);
            return new Take(round(stage, me.add(up), Mth.lerp(u, 2.45, 2.75), Mth.lerp(u, 3.0, 3.6),
                    Mth.lerp(u, 0.3, 1.1)).add(drift), past, sway, 58.0 - 4.0 * kick);
        }
        if (t < MechScript.HIPS + 2) {
            // Chasing the pilot from their left through the leap, up into the light of the chest.
            double u = glide(t, MechScript.LEAP - 2, MechScript.HIPS + 2);
            Vec3 eye = near(stage, pilot(stage, t - 4.0), new Vec3(-4.2, 0.6, Mth.lerp(u, -1.6, 0.6)));
            return new Take(eye.add(drift), pilot(stage, t - 2.0).add(up), sway, Mth.lerp(u, 56.0, 50.0));
        }
        if (t < MechScript.HEAD_FORM - 2) {
            // One long flight round the front of the mech as its arms fly in, spread and clap, then in on the pilot
            // sitting down in its chest as it closes round them and its elbows lock.
            double u = glide(t, MechScript.HIPS + 2, MechScript.HEAD_FORM - 2);
            double wide = glide(t, MechScript.HIPS + 2, MechScript.RELEASE);
            double in = Ease.smooth((t - MechScript.RELEASE + 4) / (MechScript.HEAD_FORM - MechScript.RELEASE + 2));
            Vec3 eye = round(stage, stage.point(0.0, 0.0, 1.6), Mth.lerp(u, -0.95, 0.3),
                    11.0 - 1.2 * wide - 5.4 * in, 5.0 + 0.6 * wide + 1.9 * in);
            Vec3 port = torso.point(0.0, MechBodyShapes.PORT_Y, MechBodyShapes.GLASS_Z);
            double kick = punch(t, MechScript.CLAP, 10.0);
            double jolt = punch(t, MechScript.ELBOWS, 6.0) + punch(t, MechScript.ELBOWS + 7, 6.0);
            return new Take(eye.add(drift), stage.point(0.0, 4.8, 2.0).lerp(port, in), sway + 1.5 * kick,
                    66.0 - 4.0 * wide - 16.0 * in - 5.0 * kick - 2.0 * jolt);
        }
        if (t < MechScript.REACH + 5) {
            // Low and far off its left front, sinking as the head forms high over it and drops into the ground.
            double u = glide(t, MechScript.HEAD_FORM - 2, MechScript.REACH + 5);
            double kick = punch(t, MechScript.CRASH, 12.0);
            Vec3 eye = stage.point(Mth.lerp(u, -9.0, -6.8), Mth.lerp(u, 4.8, 1.8), Mth.lerp(u, 15.5, 13.5));
            Vec3 fallen = stage.point(0.0, 9.5, 2.0).lerp(stage.point(0.0, 3.4, 3.0),
                    Ease.smooth((t - MechScript.HEAD_DROP + 3) / 14.0));
            return new Take(eye.add(drift), fallen, sway - 2.0 * kick, Mth.lerp(u, 66.0, 62.0) - 6.0 * kick);
        }
        if (t < MechScript.LOCK - 6) {
            // In on the pilot at the sticks as the mech lunges down and takes the head, then back and up and out wide
            // after the head as it is flung.
            double out = 0.2 * glide(t, MechScript.REACH + 5, MechScript.WIND + 4)
                    + 0.8 * Ease.smooth((t - MechScript.WIND - 2) / (MechScript.LOCK - MechScript.WIND - 8));
            MechScript.Stage was = MechBuild.torso(stage, t - 2.0);
            Vec3 port = was.point(0.0, MechBodyShapes.PORT_Y, MechBodyShapes.GLASS_Z);
            Vec3 flung = stage.point(0.0, 8.5, 1.0).lerp(MechHead.pose(stage, t - 3.0).at(), 0.55);
            double kick = punch(t, MechScript.GRAB, 8.0) + punch(t, MechScript.TOSS, 8.0);
            return new Take(stage.point(new Vec3(2.6, 5.0, 14.0).lerp(new Vec3(12.5, 8.6, 6.0), out)).add(drift),
                    port.lerp(flung, Ease.smooth((t - MechScript.TOSS + 4) / 10.0)), sway,
                    Mth.lerp(out, 42.0, 66.0) - 4.0 * kick);
        }
        // Low in front, swinging slowly round and up under the mech as its head drops onto its neck and it stands tall.
        double u = glide(t, MechScript.LOCK - 6, MechScript.DONE);
        double kick = punch(t, MechScript.LOCK, 10.0);
        return new Take(round(stage, stage.point(0.0, 0.0, 1.0), Mth.lerp(u, 0.55, 0.05), Mth.lerp(u, 10.5, 9.0),
                Mth.lerp(u, 1.6, 3.2)).add(drift), torso.point(0.0, 6.8, 0.9), sway + 2.0 * kick,
                64.0 - 5.0 * kick);
    }

    // A place `offset` (in the mech's axes) from the pilot's feet.
    private static Vec3 near(MechScript.Stage stage, Vec3 me, Vec3 offset) {
        return me.add(stage.dir(offset));
    }

    // A place round `center` (in the world), `radius` off it level, `angle` round from straight ahead of the mech
    // towards its right, and `high` above it.
    private static Vec3 round(MechScript.Stage stage, Vec3 center, double angle, double radius, double high) {
        return center.add(stage.dir(new Vec3(radius * Math.sin(angle), high, radius * Math.cos(angle))));
    }

    // Where the pilot's feet are `t` into the build.
    private static Vec3 pilot(MechScript.Stage stage, double t) {
        return MechBuild.torso(stage, t).point(MechScript.pilot(stage, t));
    }

    // How far a drone flying a shot has got through it, 0 to 1: already under way at the cut, quickest in the middle.
    private static double glide(double t, double from, double to) {
        double u = Mth.clamp((t - from) / (to - from), 0.0, 1.0);
        return Mth.lerp(0.6, u, Ease.smooth(u));
    }

    // The drone's hover: a slow drift of a few hundredths of a block.
    private static Vec3 hover(double t) {
        return new Vec3(0.07 * Math.sin(t * 0.11), 0.05 * Math.sin(t * 0.083 + 1.3), 0.07 * Math.sin(t * 0.097 + 2.1));
    }

    // A punch-in on a blow at `at`, from 1 dying away over `ticks`.
    private static double punch(double t, double at, double ticks) {
        double since = t - at;
        if (since < 0.0 || since >= ticks) {
            return 0.0;
        }
        double fade = 1.0 - since / ticks;
        return fade * fade;
    }

    private record Take(Vec3 eye, Vec3 at, double roll, double fov) {
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
