package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import com.mojang.logging.LogUtils;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.MechMoves;
import nl.tivek.multiversepowers.character.greenlantern.MechScript;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import org.slf4j.Logger;

public final class MechPainter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final double GLOWS = 0.22;
    private static final double CREASES = 0.7;
    private static final double SEAM = 1.0;
    private static final double FLING = 2.0;
    private static final double REACH = 20.0;
    private static final int PIECES = 40;
    private static final double[] BENDS = { 0.95, 1.1, 0.85 };
    private static final double[] SPREADS = { -0.2, -0.06, 0.08, 0.22 };
    private static final Vec3 DOWN = new Vec3(0.0, -1.0, 0.0);
    private static final double BUBBLE_FAINT = 0.12;
    private static final double BUBBLE_FAINT_OWN = 0.05;
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
        if (!painter.visible(stage.point(0.0, 7.0, MechScript.TARGET_AHEAD * 0.5), REACH)) {
            return;
        }
        parts(painter, stage, t, apart, own);
        if (apart < 0.0) {
            MechLight.lights(painter, stage, t, ring, own);
        }
    }

    public static boolean breaks(ConstructPayload mech, double clock) {
        return !MechScript.breaking(mech.variant()) && clock > MechScript.FOOT_FORM + 2;
    }

    public static void broken(LanternPainter painter, ConstructPayload mech, double clock, double since) {
        double apart = since / MechScript.BREAK_TICKS;
        if (apart < 1.0) {
            parts(painter, MechScript.Stage.of(mech), clock, apart, false);
        }
    }

    private static void parts(LanternPainter painter, MechScript.Stage stage, double t, double apart, boolean own) {
        Material was = painter.material();
        painter.material(LanternPainter.MECH_LIGHT);
        painter.ambient(GLOWS);
        painter.fling(FLING);
        painter.creases(CREASES);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            int seed = side * PIECES * 20;
            lowerLeg(painter, stage, right, t, apart, seed);
            upperLeg(painter, stage, right, t, apart, seed + PIECES);
            arm(painter, stage, right, t, apart, seed + PIECES * 3);
            shoulder(painter, stage, right, t, apart, seed + PIECES * 8);
        }
        body(painter, stage, t, apart, own);
        head(painter, stage, t, apart);
        painter.creases(0.0);
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.material(was);
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

    private static Frame body(MechScript.Stage stage) {
        return Frame.of(stage.base(), stage.ahead(), Vectors.UP, 1.0);
    }

    // Grows up out of the light: only what lies below the rising plane shows, cut with a bright seam.
    private static void rising(LanternPainter painter, MechScript.Stage stage, double from, double to, double grown,
            double apart) {
        if (grown < 1.0 && apart < 0.0) {
            painter.clip(stage.point(0.0, Mth.lerp(Ease.smooth(grown), from, to), 0.0), DOWN, SEAM);
        }
    }

    private static void lowerLeg(LanternPainter painter, MechScript.Stage stage, boolean right, double t,
            double apart, int seed) {
        double form = right ? MechScript.FOOT_FORM : MechScript.FOOT2_FORM;
        if (t < form) {
            return;
        }
        Vec3 ankle = stage.point(MechScript.ankle(right, stage, t));
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
    }

    private static void body(LanternPainter painter, MechScript.Stage stage, double t, double apart, boolean own) {
        Frame body = body(stage);
        if (t >= MechScript.HIPS) {
            rising(painter, stage, 4.2, 6.15, Mth.clamp((t - MechScript.HIPS) / 6.0, 0.0, 1.0), apart);
            MechParts.draw(painter, MechBodyShapes.PELVIS, body, 1.0, apart, PIECES * 50);
            MechParts.draw(painter, MechBodyShapes.WAIST, body, 1.0, apart, PIECES * 51);
            painter.noClip();
        }
        if (t >= MechScript.ARMOR) {
            rising(painter, stage, 5.6, MechBodyShapes.CHEST_TOP + 0.3,
                    Mth.clamp((t - MechScript.ARMOR) / 16.0, 0.0, 1.0), apart);
            MechParts.draw(painter, MechBodyShapes.CHEST, body, 1.0, apart, PIECES * 52);
            MechParts.draw(painter, MechBodyShapes.ARCH, body, 1.0, apart, PIECES * 54);
            painter.noClip();
        }
        if (t >= MechScript.STICKS) {
            double grown = Mth.clamp((t - MechScript.STICKS) / 6.0, 0.0, 1.0);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 foot = stage.point(side * MechScript.STICK.x, MechScript.COCKPIT.y, MechScript.STICK.z);
                if (grown < 1.0 && apart < 0.0) {
                    painter.clip(foot.add(0.0, grown, 0.0), DOWN, SEAM * 0.6);
                }
                MechParts.draw(painter, MechBodyShapes.STICK, Frame.of(foot, stage.ahead(), Vectors.UP, 1.0), 1.0,
                        apart, PIECES * 55 + side);
                painter.noClip();
            }
        }
        if (t >= MechScript.CORE && apart < 0.0) {
            double grown = Ease.backOut(Mth.clamp((t - MechScript.CORE) / MechScript.FORM_TICKS, 0.0, 1.0));
            Frame bubble = Frame.of(stage.point(MechBodyShapes.CORE), stage.ahead(), Vectors.UP, Math.max(0.02,
                    grown));
            painter.creases(0.0);
            painter.seeThrough(MechBodyShapes.BUBBLE, bubble, own ? BUBBLE_FAINT_OWN : BUBBLE_FAINT, 1.25);
            painter.creases(CREASES);
        }
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

    private static void arm(LanternPainter painter, MechScript.Stage stage, boolean right, double t, double apart,
            int seed) {
        if (t < MechScript.ARMS_FORM) {
            return;
        }
        MechMoves.Arm arm = MechMoves.arm(right, stage, t);
        Frame hand = Frame.of(stage.point(arm.elbow()), stage.dir(arm.palm()), stage.dir(arm.way()), 1.0);
        double grown = Mth.clamp((t - MechScript.ARMS_FORM) / (MechScript.ARMS_IN - MechScript.ARMS_FORM), 0.0, 1.0);
        if (grown < 1.0 && apart < 0.0) {
            // Out of the light from the finger tips back to the elbow.
            painter.clip(hand.at(0.0, Mth.lerp(Ease.smooth(grown), MechArmShapes.KNUCKLES + 1.1, -0.6), 0.0),
                    hand.up(), SEAM);
        }
        MechParts.draw(painter, right ? MechArmShapes.FOREARM : MechArmShapes.FOREARM_LEFT, hand, 1.0, apart, seed);
        fingers(painter, hand, arm, right, apart, seed + 10);
        painter.noClip();
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
            int seed) {
        double flip = right ? 1.0 : -1.0;
        for (int k = 0; k < 4; k++) {
            Frame joint = hand.moved(flip * MechArmShapes.FINGER_X[k], MechArmShapes.KNUCKLES, 0.0)
                    .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, flip * SPREADS[k] * arm.spread());
            for (int j = 0; j < 3; j++) {
                joint = joint.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.08 + arm.curl() * BENDS[j] * 1.45);
                Shape segment = right ? MechArmShapes.FINGERS[k][j] : MechArmShapes.FINGERS_LEFT[k][j];
                MechParts.draw(painter, segment, joint, 1.0, apart, seed + k * 3 + j);
                joint = joint.moved(0.0, MechArmShapes.FINGER_LENGTHS[k][j], 0.0);
            }
        }
        Vec3 root = MechArmShapes.THUMB_ROOT;
        Frame thumb = hand.moved(flip * root.x, root.y, root.z).turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                flip * (-0.85 + 0.45 * arm.curl())).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.35 + 0.5 * arm.curl());
        for (int j = 0; j < 3; j++) {
            if (j > 0) {
                thumb = thumb.turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.12 + arm.curl() * 0.7);
            }
            MechParts.draw(painter, right ? MechArmShapes.THUMB[j] : MechArmShapes.THUMB_LEFT[j], thumb, 1.0, apart,
                    seed + 20 + j);
            thumb = thumb.moved(0.0, MechArmShapes.THUMB_LENGTHS[j], 0.0);
        }
    }

    private static void head(LanternPainter painter, MechScript.Stage stage, double t, double apart) {
        if (t < MechScript.HEAD_FORM) {
            return;
        }
        double grown = apart >= 0.0 ? 1.0 : Ease.backOut(Mth.clamp((t - MechScript.HEAD_FORM) / 4.0, 0.0, 1.0));
        MechScript.Turn turn = MechScript.headTurn(t);
        Frame frame = Frame.of(stage.point(MechScript.head(stage, t)), stage.ahead(), Vectors.UP,
                MechScript.HEAD_SCALE * Math.max(0.05, grown)).turned(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, turn.yaw())
                .turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, turn.pitch()).turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, turn.roll());
        MechParts.draw(painter, MechHeadShapes.HEAD, frame, 1.0, apart, PIECES * 60);
    }

    public static float shake(ConstructPayload mech, double clock, Vec3 from) {
        if (MechScript.breaking(mech.variant())) {
            return 0.0F;
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
