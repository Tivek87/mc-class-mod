package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.rig.BoneView;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import org.slf4j.Logger;

public final class MechPainter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double GLOWS = 0.22;
    static final double CREASES = 0.7;
    private static final double SEAM = 1.0;
    private static final double FLING = 2.0;
    private static final double REACH = 20.0;
    private static final int PIECES = 40;
    private static final double STOMP_SHAKE = 0.75;
    private static final double STEP_SHAKE = 0.3;
    private static final double CLAP_SHAKE = 0.7;
    private static final double LOCK_SHAKE = 0.45;
    private static final double CRASH_SHAKE = 1.0;
    private static final double SHAKE_RANGE = 40.0;

    private MechPainter() {
    }

    // Builds the mech's tiled parts off the render thread once the game has started, so its first build does not stall.
    public static void onClientSetup(FMLClientSetupEvent event) {
        Thread.ofVirtual().name("mech-parts").start(() -> {
            int parts = MechLegShapes.FOOT.meshes().length + MechBodyShapes.CHEST.meshes().length
                    + MechArmShapes.UPPER.meshes().length + MechHeadShapes.HEAD.meshes().length;
            LOGGER.debug("Mech parts ready ({} meshes)", parts);
        });
    }

    public static void draw(LanternPainter painter, ConstructPayload was, ConstructPayload now, double clock,
            float partialTick, @Nullable Vec3 ring, boolean own) {
        MechScript.Stage stage = MechScript.Stage.of(now);
        boolean breaking = MechScript.breaking(now.variant());
        double t = breaking ? now.age() : clock;
        double apart = breaking ? Mth.clamp(Mth.lerp(partialTick, Math.max(0.0F, was.charge()), now.charge())
                / MechScript.BREAK_TICKS, 0.0, 1.0) : -1.0;
        MechPose walk = MechWalk.pose(now.id(), partialTick);
        MechPose pose = walk != null && (breaking || t >= MechScript.SETTLED) ? walk : scripted(stage, t);
        if (!painter.visible(pose.stage().point(0.0, 7.0, MechScript.TARGET_AHEAD * 0.5), REACH)) {
            return;
        }
        parts(painter, pose, t, apart, own, walk != null && pose == walk);
        if (apart < 0.0) {
            MechLight.lights(painter, pose, t, ring, own);
        }
    }

    public static boolean breaks(ConstructPayload mech, double clock) {
        return !MechScript.breaking(mech.variant()) && clock > MechScript.FOOT_FORM + 2;
    }

    public static void broken(LanternPainter painter, ConstructPayload mech, double clock, double since) {
        double apart = since / MechScript.BREAK_TICKS;
        if (apart < 1.0) {
            MechPose walk = MechWalk.pose(mech.id(), 0.0F);
            parts(painter, walk != null ? walk : scripted(MechScript.Stage.of(mech), clock), clock, apart, false,
                    walk != null);
        }
    }

    // The mech as its build places it: upright on its ground spot, the feet where the stomps and steps put them.
    private static MechPose scripted(MechScript.Stage stage, double t) {
        MechPose pose = new MechPose(stage);
        for (int side = 0; side < 2; side++) {
            pose.ankle[side] = stage.point(MechScript.ankle(side == 0, stage, t));
        }
        return pose;
    }

    private static void parts(LanternPainter painter, MechPose pose, double t, double apart, boolean own,
            boolean walking) {
        Material was = painter.material();
        painter.material(LanternPainter.MECH_LIGHT);
        painter.batch();
        painter.ambient(GLOWS);
        painter.fling(FLING);
        painter.creases(CREASES);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            int seed = side * PIECES * 20;
            if (walking) {
                MechLegs.leg(painter, pose, right, apart, seed);
            } else {
                lowerLeg(painter, pose.stage(), pose.ankle[side], right, t, apart, seed);
                upperLeg(painter, pose.stage(), right, t, apart, seed + PIECES);
            }
            arm(painter, pose, right, t, apart, seed + PIECES * 3, walking);
            shoulder(painter, pose.torso(), right, t, apart, seed + PIECES * 8);
        }
        body(painter, pose, t, apart, own);
        head(painter, pose, t, apart, walking);
        spine(pose, t, apart, walking);
        painter.flush();
        painter.creases(0.0);
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.material(was);
    }

    // For the developer's view: hips and shoulders off the spine, the spine from the pelvis to the chest, the neck.
    private static void spine(MechPose pose, double t, double apart, boolean walking) {
        if (!BoneView.shown() || t < MechScript.HIPS) {
            return;
        }
        MechScript.Stage torso = pose.torso();
        Vec3 pelvis = pose.hips().point(MechScript.WAIST);
        Vec3 chest = torso.point(0.0, MechScript.SHOULDER.y, 0.0);
        BoneView.bone(pelvis, chest, BoneView.CONSTRUCT);
        for (int s = 0; s < 2; s++) {
            BoneView.bone(pelvis, pose.hips().point(side(MechScript.HIP, s == 0)), BoneView.CONSTRUCT);
            BoneView.bone(chest, torso.point(side(MechScript.SHOULDER, s == 0)), BoneView.CONSTRUCT);
        }
        if (t >= MechScript.HEAD_FORM) {
            BoneView.bone(chest, head(pose, t, apart, walking).center(), BoneView.CONSTRUCT);
        }
    }

    static Vec3 side(Vec3 local, boolean right) {
        return right ? local : MechScript.mirror(local);
    }

    // A frame at from with its y along up and its z as near to facing as that leaves.
    static Frame limb(Vec3 from, Vec3 up, Vec3 facing) {
        Vec3 ahead = facing.subtract(up.scale(facing.dot(up)));
        ahead = ahead.lengthSqr() < 1.0E-8 ? Vectors.across(up)[0] : ahead.normalize();
        return new Frame(from, ahead.cross(up).normalize(), up, ahead, 1.0);
    }

    static Frame body(MechScript.Stage stage) {
        return Frame.of(stage.base(), stage.ahead(), stage.up(), 1.0);
    }

    // Grows up out of the light: only what lies below the rising plane shows, cut with a bright seam.
    static void rising(LanternPainter painter, MechScript.Stage stage, double from, double to, double grown,
            double apart) {
        if (grown < 1.0 && apart < 0.0) {
            painter.clip(stage.point(0.0, Mth.lerp(Ease.smooth(grown), from, to), 0.0), stage.up().scale(-1.0), SEAM);
        }
    }

    private static void lowerLeg(LanternPainter painter, MechScript.Stage stage, Vec3 ankle, boolean right, double t,
            double apart, int seed) {
        double form = right ? MechScript.FOOT_FORM : MechScript.FOOT2_FORM;
        if (t < form) {
            return;
        }
        Frame foot = Frame.of(ankle, stage.ahead(), Vectors.UP, 1.0);
        Frame shin = limb(ankle, stage.dir(side(MechScript.KNEE.subtract(MechScript.ANKLE), right)).normalize(),
                stage.ahead());
        double grown = MechScript.grown(t, form);
        if (grown < 1.0 && apart < 0.0) {
            // It grows down out of the light, from the open top of the shin to the sole.
            double y = Mth.lerp(Ease.smooth(grown), MechLegShapes.SHIN + 0.1, MechLegShapes.SOLE - 0.05);
            painter.clip(ankle.add(0.0, y, 0.0), Vectors.UP, SEAM);
        }
        MechParts.draw(painter, right ? MechLegShapes.FOOT : MechLegShapes.FOOT_LEFT, foot, 1.0, apart, seed);
        MechParts.draw(painter, right ? MechLegShapes.SHIN_PART : MechLegShapes.SHIN_LEFT, shin, 1.0, apart,
                seed + 20);
        painter.noClip();
        if (BoneView.shown()) {
            BoneView.bone(stage.point(side(MechScript.KNEE, right)), ankle, BoneView.CONSTRUCT);
        }
    }

    private static void upperLeg(LanternPainter painter, MechScript.Stage stage, boolean right, double t,
            double apart, int seed) {
        if (t < MechScript.THIGHS) {
            return;
        }
        Vec3 knee = stage.point(side(MechScript.KNEE, right));
        Vec3 hip = stage.point(side(MechScript.HIP, right));
        rising(painter, stage, MechScript.KNEE.y - 0.7, MechScript.HIP.y + 0.6, MechScript.grown(t, MechScript.THIGHS),
                apart);
        MechParts.draw(painter, right ? MechLegShapes.KNEE : MechLegShapes.KNEE_LEFT,
                Frame.of(knee, stage.ahead(), Vectors.UP, 1.0), 1.0, apart, seed);
        MechParts.draw(painter, right ? MechLegShapes.THIGH_PART : MechLegShapes.THIGH_LEFT,
                limb(knee, hip.subtract(knee).normalize(), stage.ahead()), 1.0, apart, seed + 10);
        painter.noClip();
        if (BoneView.shown()) {
            BoneView.bone(hip, knee, BoneView.CONSTRUCT);
        }
    }

    private static void body(LanternPainter painter, MechPose pose, double t, double apart, boolean own) {
        MechScript.Stage torso = pose.torso();
        Frame body = body(torso);
        if (t >= MechScript.HIPS) {
            rising(painter, torso, 4.2, 6.15, Mth.clamp((t - MechScript.HIPS) / 6.0, 0.0, 1.0), apart);
            MechParts.draw(painter, MechBodyShapes.PELVIS, body(pose.hips()), 1.0, apart, PIECES * 50);
            MechParts.draw(painter, MechBodyShapes.WAIST, body, 1.0, apart, PIECES * 51);
            painter.noClip();
        }
        if (t >= MechScript.ARMOR) {
            rising(painter, torso, 5.6, MechBodyShapes.CHEST_TOP + 0.3,
                    Mth.clamp((t - MechScript.ARMOR) / 16.0, 0.0, 1.0), apart);
            MechParts.draw(painter, MechBodyShapes.CHEST, body, 1.0, apart, PIECES * 52);
            MechParts.draw(painter, MechBodyShapes.RIM, body, 1.0, apart, PIECES * 54);
            painter.noClip();
        }
        MechCockpit.draw(painter, pose, body, t, apart, own, PIECES * 56);
    }

    private static void shoulder(LanternPainter painter, MechScript.Stage stage, boolean right, double t,
            double apart, int seed) {
        if (t < MechScript.SHOULDERS) {
            return;
        }
        double grown = Mth.clamp((t - MechScript.SHOULDERS) / 12.0, 0.0, 1.0);
        if (grown < 1.0 && apart < 0.0) {
            Vec3 out = stage.dir(side(new Vec3(1.0, 0.0, 0.0), right));
            painter.clip(stage.point(side(new Vec3(1.8 + 3.4 * Ease.smooth(grown), 0.0, 0.0), right)), out.scale(-1.0),
                    SEAM);
        }
        MechParts.draw(painter, right ? MechBodyShapes.SHOULDER : MechBodyShapes.SHOULDER_LEFT, body(stage), 1.0,
                apart, seed);
        painter.noClip();
    }

    private static void arm(LanternPainter painter, MechPose pose, boolean right, double t, double apart, int seed,
            boolean walking) {
        if (t < MechScript.ARMS_FORM) {
            return;
        }
        MechScript.Stage stage = pose.torso();
        // Running, the forearms come up and pump instead of hanging.
        double hang = pose.walking * (1.0 - 0.8 * pose.running);
        MechMoves.Arm arm = walking ? MechMoves.walking(right, t, pose.swing, hang) : MechMoves.arm(right, stage, t);
        Frame hand = Frame.of(stage.point(arm.elbow()), stage.dir(arm.palm()), stage.dir(arm.way()), 1.0);
        double grown = Mth.clamp((t - MechScript.ARMS_FORM) / (MechScript.ARMS_IN - MechScript.ARMS_FORM), 0.0, 1.0);
        if (grown < 1.0 && apart < 0.0) {
            // Out of the light from the finger tips back to the elbow.
            painter.clip(hand.at(0.0, Mth.lerp(Ease.smooth(grown), MechArmShapes.KNUCKLES + 1.1, -0.6), 0.0),
                    hand.up(), SEAM);
        }
        // The hand's frame is left-handed (Frame.of): drawn with the other side's shapes and bones, each hand's thumb
        // comes out on the side a hand of its own has it, not mirrored.
        boolean own = !right;
        MechParts.draw(painter, own ? MechArmShapes.FOREARM : MechArmShapes.FOREARM_LEFT, hand, 1.0, apart, seed);
        // Clapping, the two hands meet in the middle: their fingers stop there against each other.
        boolean clapping = !walking && t > MechScript.SWING && t < MechScript.RISE;
        Vec3 side = right ? stage.right() : stage.right().scale(-1.0);
        double[] wall = clapping ? MechHandRig.wall(hand, stage.base(), side) : null;
        fingers(painter, hand, arm, own, apart, seed + 10, wall);
        painter.noClip();
        if (BoneView.shown()) {
            BoneView.bone(hand.center(), hand.at(0.0, MechArmShapes.WRIST, 0.0), BoneView.CONSTRUCT);
            if (arm.upper() > 0.0) {
                BoneView.bone(stage.point(side(MechScript.SHOULDER, right)), hand.center(), BoneView.CONSTRUCT);
            }
        }
        if (arm.upper() > 0.0) {
            Vec3 shoulder = stage.point(side(MechScript.SHOULDER, right));
            Vec3 elbow = stage.point(arm.elbow());
            Vec3 along = elbow.subtract(shoulder);
            double far = along.length();
            Frame upper = limb(shoulder, along.scale(1.0 / far), stage.ahead()).stretched(1.0,
                    far / MechScript.UPPER_ARM, 1.0);
            if (arm.upper() < 1.0 && apart < 0.0) {
                Vec3 way = along.scale(1.0 / far);
                painter.clip(shoulder.add(way.scale(far * Ease.smooth(arm.upper()) + 0.3)), way.scale(-1.0), SEAM);
            }
            MechParts.draw(painter, right ? MechArmShapes.UPPER : MechArmShapes.UPPER_LEFT, upper, 1.0, apart,
                    seed + 40);
            painter.noClip();
        }
    }

    private static void fingers(LanternPainter painter, Frame hand, MechMoves.Arm arm, boolean right, double apart,
            int seed, @Nullable double[] wall) {
        Frame[] bones = MechHandRig.frames(hand, arm, right, wall);
        for (int k = 0; k < 4; k++) {
            for (int j = 0; j < 3; j++) {
                Shape segment = right ? MechArmShapes.FINGERS[k][j] : MechArmShapes.FINGERS_LEFT[k][j];
                MechParts.draw(painter, segment, bones[MechHandRig.bone(k, j)], 1.0, apart, seed + k * 3 + j);
            }
        }
        for (int j = 0; j < 3; j++) {
            MechParts.draw(painter, right ? MechArmShapes.THUMB[j] : MechArmShapes.THUMB_LEFT[j],
                    bones[MechHandRig.bone(4, j)], 1.0, apart, seed + 20 + j);
        }
        if (BoneView.shown()) {
            Vec3 wrist = hand.at(0.0, MechArmShapes.WRIST, 0.0);
            for (int k = 0; k < 5; k++) {
                BoneView.bone(wrist, bones[MechHandRig.bone(k, 0)].center(), BoneView.CONSTRUCT);
                for (int j = 0; j < 3; j++) {
                    BoneView.bone(bones[MechHandRig.bone(k, j)], MechHandRig.length(k, j), BoneView.CONSTRUCT);
                }
            }
        }
    }

    private static void head(LanternPainter painter, MechPose pose, double t, double apart, boolean walking) {
        if (t < MechScript.HEAD_FORM) {
            return;
        }
        Frame frame = head(pose, t, apart, walking);
        MechParts.draw(painter, MechHeadShapes.HEAD, frame, 1.0, apart, PIECES * 60);
    }

    // The head's frame round its middle: dropped and tumbled by the build, then riding the body's neck.
    static Frame head(MechPose pose, double t, double apart, boolean walking) {
        MechScript.Stage stage = pose.torso();
        double grown = apart >= 0.0 ? 1.0 : Ease.backOut(Mth.clamp((t - MechScript.HEAD_FORM) / 4.0, 0.0, 1.0));
        MechScript.Turn turn = walking ? new MechScript.Turn(pose.headYaw, pose.headPitch, 0.0)
                : MechScript.headTurn(t);
        Vec3 at = walking ? stage.point(MechScript.NECK.add(0.0, MechScript.HEAD_UP, 0.0))
                : stage.point(MechScript.head(stage, t));
        return Frame.of(at, stage.ahead(), stage.up(), MechScript.HEAD_SCALE * Math.max(0.05, grown))
                .turned(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, turn.yaw()).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, turn.pitch())
                .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, turn.roll());
    }

    public static float shake(ConstructPayload mech, double clock, Vec3 from) {
        if (MechScript.breaking(mech.variant())) {
            return 0.0F;
        }
        if (clock >= MechScript.SETTLED) {
            Minecraft minecraft = Minecraft.getInstance();
            boolean inside = minecraft.player != null && minecraft.player.getId() == mech.owner();
            return (float) MechWalk.shake(mech.id(), from, minecraft.getTimer().getGameTimeDeltaPartialTick(false),
                    inside);
        }
        MechScript.Stage stage = MechScript.Stage.of(mech);
        double near = 1.0 - from.distanceTo(stage.point(0.0, 3.0, MechScript.TARGET_AHEAD * 0.5)) / SHAKE_RANGE;
        if (near <= 0.0) {
            return 0.0F;
        }
        double most = 0.0;
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            double landed = MechScript.landed(right, clock);
            boolean stomp = MechScript.stomping(right, clock);
            most = Math.max(most, jolt(landed, stomp ? 10.0 : 6.0, stomp ? STOMP_SHAKE * (right ? 1.0 : 0.6)
                    : STEP_SHAKE));
        }
        most = Math.max(most, jolt(clock - MechScript.CLAP, 9.0, CLAP_SHAKE));
        most = Math.max(most, jolt(clock - (MechScript.THIGHS + MechScript.FORM_TICKS), 5.0, LOCK_SHAKE * 0.6));
        most = Math.max(most, jolt(clock - MechScript.ELBOWS, 6.0, LOCK_SHAKE));
        most = Math.max(most, jolt(clock - MechScript.CRASH, 14.0, CRASH_SHAKE));
        most = Math.max(most, jolt(clock - MechScript.LOCK, 8.0, LOCK_SHAKE * 1.3));
        return (float) Math.min(1.0, most * Math.min(1.0, near * 1.3));
    }

    private static double jolt(double since, double ticks, double hard) {
        if (since < 0.0 || since >= ticks) {
            return 0.0;
        }
        double fade = 1.0 - since / ticks;
        return hard * fade * fade;
    }
}
