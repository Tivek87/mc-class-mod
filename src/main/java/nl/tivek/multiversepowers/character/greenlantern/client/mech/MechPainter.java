package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechArmShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechBodyShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechHeadShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechLegShapes;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechArmRig;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechFingers;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechHandRig;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.touch.MechTouch;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechPose;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.walk.MechWalk;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechBuild;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechHead;
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
    // How far from the knuckles the blocks a hand's fingers may touch are looked for.
    private static final double FINGERS_REACH = 2.2;
    private static final int PIECES = 40;
    private static final double STOMP_SHAKE = 0.75;
    private static final double STEP_SHAKE = 0.3;
    private static final double CLAP_SHAKE = 0.7;
    private static final double LOCK_SHAKE = 0.45;
    private static final double CRASH_SHAKE = 1.0;
    private static final double SHAKE_RANGE = 40.0;
    // A shoulder turns about its collar, where the shoulder piece meets the chest: it shrugs SHRUG_RAISED as its elbow
    // rises from RAISED_FROM under the shoulder to an upper arm's length above that, and rolls ROLL_AHEAD as its elbow
    // goes an upper arm's length ahead (radians).
    private static final Vec3 COLLAR = new Vec3(1.4, MechScript.SHOULDER.y + 0.3, 0.0);
    private static final double RAISED_FROM = 0.3;
    private static final double SHRUG_RAISED = 0.3;
    private static final double ROLL_AHEAD = 0.15;
    private static final double[] STILL = { 0.0, 0.0 };

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
        boolean walking = walk != null && pose == walk;
        parts(painter, now.id(), pose, t, apart, own, walking, caught(now, pose));
        if (apart < 0.0) {
            MechLight.lights(painter, pose, t, ring, own);
            if (walking) {
                MechExhaust.flames(painter, pose, t);
                MechBeamFx.draw(painter, pose, t, now.owner(), own, partialTick);
            }
        }
        MechExhaust.forget();
    }

    public static boolean breaks(ConstructPayload mech, double clock) {
        return !MechScript.breaking(mech.variant()) && clock > MechScript.FOOT_FORM + 2;
    }

    public static void broken(LanternPainter painter, ConstructPayload mech, double clock, double since) {
        double apart = since / MechScript.BREAK_TICKS;
        if (apart < 1.0) {
            MechPose walk = MechWalk.pose(mech.id(), 0.0F);
            parts(painter, mech.id(), walk != null ? walk : scripted(MechScript.Stage.of(mech), clock), clock, apart,
                    false, walk != null, null);
        }
    }

    // The creature a throw reaches for or holds, where it is drawn this frame.
    @Nullable
    private static Entity caught(ConstructPayload mech, MechPose pose) {
        int kind = pose.blow().kind();
        Minecraft minecraft = Minecraft.getInstance();
        if (kind != MechAttacks.THROW || minecraft.level == null) {
            return null;
        }
        Entity caught = minecraft.level.getEntity(LightBubble.caughtId(mech.charge()));
        return caught instanceof LivingEntity ? caught : null;
    }

    public static MechAttacks.Held held(Entity caught, float partialTick) {
        AABB box = drawn(caught, partialTick);
        return new MechAttacks.Held(box.getCenter(), caught.getBbWidth() * 0.5, caught.getBbHeight() * 0.5);
    }

    private static AABB drawn(Entity caught, float partialTick) {
        return caught.getBoundingBox().move(caught.getPosition(partialTick).subtract(caught.position()));
    }

    // The mech as its build places it: on its ground spot, the feet where the stomps and steps put them and the body as
    // its weight moves it (MechBuilding).
    private static MechPose scripted(MechScript.Stage stage, double t) {
        return MechBuilding.pose(stage, t);
    }

    private static void parts(LanternPainter painter, int id, MechPose pose, double t, double apart, boolean own,
            boolean walking, @Nullable Entity caught) {
        Material was = painter.material();
        painter.material(LanternPainter.MECH_LIGHT);
        painter.batch();
        painter.ambient(GLOWS);
        painter.fling(FLING);
        painter.creases(CREASES);
        // Built and whole, its legs keep out of the blocks round them.
        Level level = walking && apart < 0.0 ? Minecraft.getInstance().level : null;
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            int seed = side * PIECES * 20;
            if (walking) {
                MechLegs.leg(painter, id, pose, right, apart, seed, t, level);
            } else {
                MechBuilding.leg(painter, pose, right, t, apart, seed);
            }
            double[] turn = arm(painter, id, pose, right, t, apart, seed + PIECES * 3, walking, right ? caught : null);
            shoulder(painter, pose.torso(), right, t, apart, seed + PIECES * 8, turn);
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

    // For the developer's view: hips and collars off the spine, the spine from the pelvis to the chest, the neck.
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
            BoneView.bone(chest, torso.point(side(COLLAR, s == 0)), BoneView.CONSTRUCT);
        }
        if (t >= MechScript.HEAD_FORM) {
            BoneView.bone(chest, head(pose, t, apart, walking).center(), BoneView.CONSTRUCT);
        }
    }

    public static Vec3 side(Vec3 local, boolean right) {
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

    // `turn`: how far it shrugs and rolls ahead about its collar (shoulderTurn).
    private static void shoulder(LanternPainter painter, MechScript.Stage stage, boolean right, double t,
            double apart, int seed, double[] turn) {
        if (t < MechScript.SHOULDERS) {
            return;
        }
        double grown = Mth.clamp((t - MechScript.SHOULDERS) / 12.0, 0.0, 1.0);
        if (grown < 1.0 && apart < 0.0) {
            Vec3 out = stage.dir(side(new Vec3(1.0, 0.0, 0.0), right));
            painter.clip(stage.point(side(new Vec3(1.8 + 3.4 * Ease.smooth(grown), 0.0, 0.0), right)), out.scale(-1.0),
                    SEAM);
        }
        MechParts.draw(painter, right ? MechBodyShapes.SHOULDER : MechBodyShapes.SHOULDER_LEFT,
                shrugged(stage, right, turn, body(stage)), 1.0, apart, seed);
        painter.noClip();
        if (BoneView.shown()) {
            BoneView.bone(stage.point(side(COLLAR, right)), shrugged(stage, right, turn,
                    stage.point(side(MechScript.SHOULDER, right))), BoneView.CONSTRUCT);
        }
    }

    // How far a shoulder shrugs and rolls ahead about its collar (radians): up as its arm rises above it and ahead as
    // its elbow goes out in front, on top of how the body's weight swings it; only as far as its upper arm is there
    // (a forearm still flying in loose moves no shoulder).
    private static double[] shoulderTurn(MechPose pose, MechMoves.Arm arm, boolean right) {
        Vec3 elbow = arm.elbow();
        double raised = Mth.clamp((elbow.y - MechScript.SHOULDER.y + RAISED_FROM) / MechScript.UPPER_ARM, 0.0, 1.0);
        double ahead = Mth.clamp(elbow.z / MechScript.UPPER_ARM, 0.0, 1.0);
        double there = arm.upper();
        return new double[] { (SHRUG_RAISED * raised + pose.shrug[right ? 0 : 1]) * there, ROLL_AHEAD * ahead * there };
    }

    // A point of a shoulder turned about its collar by `turn`: shrugged (its outer end up), then rolled ahead.
    private static Vec3 shrugged(MechScript.Stage stage, boolean right, double[] turn, Vec3 point) {
        if (turn[0] == 0.0 && turn[1] == 0.0) {
            return point;
        }
        Vec3 pivot = stage.point(side(COLLAR, right));
        return pivot.add(shrugWay(stage, right, turn, point.subtract(pivot)));
    }

    private static Frame shrugged(MechScript.Stage stage, boolean right, double[] turn, Frame frame) {
        if (turn[0] == 0.0 && turn[1] == 0.0) {
            return frame;
        }
        return new Frame(shrugged(stage, right, turn, frame.center()), shrugWay(stage, right, turn, frame.right()),
                shrugWay(stage, right, turn, frame.up()), shrugWay(stage, right, turn, frame.forward()),
                frame.scale());
    }

    // A way turned as the shoulder turns: about the line that lifts its outward way up, then the one that takes it
    // ahead.
    private static Vec3 shrugWay(MechScript.Stage stage, boolean right, double[] turn, Vec3 way) {
        Vec3 out = stage.dir(side(new Vec3(1.0, 0.0, 0.0), right));
        Vec3 lift = out.cross(stage.up()).normalize();
        Vec3 roll = out.cross(stage.ahead()).normalize();
        return Vectors.spin(Vectors.spin(way, lift, turn[0]), roll, turn[1]);
    }

    // An arm's hand in the world, turned at its wrist as the arm has it and by `fold` and `tilt` more.
    public static Frame hand(MechScript.Stage torso, MechMoves.Arm arm, double fold, double tilt) {
        return MechArmRig.hand(torso, arm, fold, tilt);
    }

    // The forearm and hand of an arm whose shoulder its shrug has moved (see MechArmRig.reattached); as they were
    // without a shrug.
    private static Frame[] reattached(MechScript.Stage torso, MechMoves.Arm arm, Frame hand, Vec3 shoulder,
            boolean right, double[] turn) {
        if (turn[0] == 0.0 && turn[1] == 0.0 || arm.upper() <= 0.0) {
            return new Frame[] { MechArmRig.forearm(torso, arm), hand };
        }
        return MechArmRig.reattached(torso, arm, hand, shoulder, torso.point(side(MechScript.SHOULDER, right)));
    }

    // Draws an arm; returns how far its shoulder turns (shoulderTurn).
    private static double[] arm(LanternPainter painter, int id, MechPose pose, boolean right, double t, double apart,
            int seed, boolean walking, @Nullable Entity caught) {
        if (t < MechScript.ARMS_FORM) {
            return STILL;
        }
        MechScript.Stage stage = pose.torso();
        // Building, an arm moves round the ground until its upper arm has it (MechBuild.arms).
        MechScript.Stage frame = walking ? stage : MechBuild.arms(pose.stage(), stage, t);
        Minecraft minecraft = Minecraft.getInstance();
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        // Built and whole, an arm stays out of the blocks round it; climbing, its hands rest on the ledge.
        boolean touches = walking && apart < 0.0 && minecraft.level != null;
        Vec3 spot = pose.ledge[right ? 0 : 1];
        // Round a climbing hand, its fingers and its forearm.
        MechHandRig.Ground ledge = touches && pose.climbing() && spot != null
                && MechTouch.near(minecraft.level, spot, FINGERS_REACH + 1.0) ? MechTouch::depth : null;
        MechMoves.Arm arm = walking ? pose.arm(right, t, caught == null ? null : held(caught, partialTick), ledge)
                : MechMoves.arm(right, pose.stage(), t);
        if (touches && !pose.climbing()) {
            arm = MechTouch.clear(minecraft.level, id, stage, arm, right, t);
        }
        int s = right ? 0 : 1;
        double[] turn = shoulderTurn(pose, arm, right);
        Vec3 shoulder = shrugged(stage, right, turn, stage.point(side(MechScript.SHOULDER, right)));
        Frame[] joined = reattached(frame, arm, hand(frame, arm, pose.fold[s], pose.tilt[s]), shoulder, right, turn);
        Frame forearm = joined[0];
        Frame hand = joined[1];
        double grown = Mth.clamp((t - MechScript.ARMS_FORM) / (MechScript.ARMS_IN - MechScript.ARMS_FORM), 0.0, 1.0);
        if (grown < 1.0 && apart < 0.0) {
            // Out of the light from the finger tips back to the elbow.
            painter.clip(forearm.at(0.0, Mth.lerp(Ease.smooth(grown), MechArmShapes.KNUCKLES + 1.1, -0.6), 0.0),
                    forearm.up(), SEAM);
        }
        // The hand's frame is left-handed (Frame.of): drawn with the other side's shapes and bones, each hand's thumb
        // comes out on the side a hand of its own has it, not mirrored.
        boolean own = !right;
        MechParts.draw(painter, own ? MechArmShapes.FOREARM : MechArmShapes.FOREARM_LEFT, forearm, 1.0, apart, seed);
        MechParts.draw(painter, own ? MechArmShapes.HAND : MechArmShapes.HAND_LEFT, hand, 1.0, apart, seed + 5);
        if (walking && apart < 0.0) {
            MechExhaust.pipes(forearm);
        }
        // Clapping, the two hands meet in the middle: their fingers stop there against each other.
        boolean clapping = !walking && t > MechScript.SWING && t < MechScript.RISE;
        Vec3 side = right ? frame.right() : frame.right().scale(-1.0);
        double[] wall = clapping ? MechHandRig.wall(hand, frame.base(), side) : null;
        // Its fingers rest on the blocks round the hand, and wrap round what a gripping hand holds.
        MechHandRig.Ground ground = touches && MechTouch.near(minecraft.level, hand.at(0.0, MechArmShapes.KNUCKLES,
                0.0), FINGERS_REACH) ? MechTouch::depth : null;
        // Built and whole, its fingers take hold and let go smoothly (MechFingers); building, the right hand's close
        // round the head it digs out and throws.
        AABB grips = caught == null ? null : drawn(caught, partialTick);
        if (!walking && right && t > MechScript.GRAB - 2 && t < MechScript.TOSS) {
            Vec3 head = MechHead.pose(pose.stage(), t).at();
            grips = new AABB(head.subtract(MechHead.HALF), head.add(MechHead.HALF));
        }
        Frame[] bones = touches ? MechFingers.frames(id, right, t, hand, arm, own, wall, grips, ground)
                : MechHandRig.frames(hand, arm, own, wall, grips, ground);
        fingers(painter, hand, bones, own, apart, seed + 10);
        painter.noClip();
        if (BoneView.shown()) {
            BoneView.bone(forearm.center(), forearm.at(0.0, MechArmShapes.WRIST, 0.0), BoneView.CONSTRUCT);
            if (arm.upper() > 0.0) {
                BoneView.bone(shoulder, forearm.center(), BoneView.CONSTRUCT);
            }
        }
        if (arm.upper() > 0.0) {
            Vec3 elbow = forearm.center();
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
        return turn;
    }

    private static void fingers(LanternPainter painter, Frame hand, Frame[] bones, boolean right, double apart,
            int seed) {
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

    // The head's frame round its middle: made, dropped, dug out, thrown and caught by the build (MechHead), then riding
    // the body's neck.
    static Frame head(MechPose pose, double t, double apart, boolean walking) {
        double scale = MechScript.HEAD_SCALE * Math.max(0.05, apart >= 0.0 ? 1.0
                : Ease.backOut(Mth.clamp((t - MechScript.HEAD_FORM) / 4.0, 0.0, 1.0)));
        if (!walking) {
            MechHead.Pose head = MechHead.pose(pose.stage(), t);
            return Frame.of(head.at(), head.ahead(), head.up(), scale);
        }
        MechScript.Stage stage = pose.torso();
        Vec3 at = stage.point(MechScript.NECK.add(0.0, MechScript.HEAD_UP, 0.0));
        MechScript.Turn turn = new MechScript.Turn(pose.headYaw, pose.headPitch, 0.0);
        return Frame.of(at, stage.ahead(), stage.up(), scale)
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
        most = Math.max(most, jolt(clock - MechScript.GRAB, 6.0, LOCK_SHAKE * 0.8));
        most = Math.max(most, jolt(clock - MechScript.TOSS, 5.0, LOCK_SHAKE * 0.6));
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
