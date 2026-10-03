package nl.tivek.multiversepowers.testfight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Method;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.math.Keyframes;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// The test fight's moves for both bodies, key by key, posed whole through Stance by every game that draws them. Model
// space, in pixels: y runs down, -z is ahead (towards the other), +x the body's own left. A hand is placed from its own
// neck and turned with its chest (a guard), or, where a blow lands, from the other's neck in its own axes; a foot
// stands where its key says, or kicks from the other's neck the same way. The head looks at the other's face. The
// fighter stands orthodox: left foot and hand leading, so a left turn of the trunk (+twist) brings the left side on.
final class FightPoses {
    // Hips: sideways, down, back. Trunk: forward, to its left, turned to its right (its left side ahead); the pelvis
    // takes part of it (Stance), the chest all of it and its own waist bend and turn on top.
    private static final int HX = 0;
    private static final int HY = 1;
    private static final int HZ = 2;
    private static final int PITCH = 3;
    private static final int ROLL = 4;
    private static final int TWIST = 5;
    private static final int WAIST = 6;
    private static final int WAIST_TWIST = 7;
    // Each hand and foot: x, y (a foot's lift off the ground) and z, then how far it is placed from the other.
    private static final int RIGHT = 8;
    private static final int LEFT = 12;
    private static final int RIGHT_FOOT = 16;
    private static final int LEFT_FOOT = 20;
    // The head's turn, nod and tilt on top of looking at the other's face; each elbow raised out for a hook (+1) or
    // tucked down for an uppercut (-1); the right knee lifted out for a kick.
    private static final int LOOK = 24;
    private static final int ELBOWS = 27;
    private static final int KNEE = 29;
    private static final int VALUES = 30;

    private static final Vector3f KNEE_AHEAD = new Vector3f(0.0F, -0.5F, -1.0F);
    private static final Vector3f KNEE_OUT = new Vector3f(-0.6F, -1.0F, -0.3F);
    private static final Vector3f ELBOW_DOWN = new Vector3f(-0.7F, 0.3F, 0.8F);
    private static final Vector3f ELBOW_OUT = new Vector3f(-1.0F, -0.45F, 0.1F);
    private static final Vector3f ELBOW_IN = new Vector3f(-0.25F, 1.0F, 0.25F);
    // From the other's neck to its face, in its own axes turned to face this body (+z towards it).
    private static final Vector3f FACE = new Vector3f(0.0F, -3.5F, 3.0F);
    // The eyes above the neck.
    private static final float EYES = 4.0F;

    private static final Keyframes.Key[] FIGHTER = fighter();
    private static final Keyframes.Key[] TARGET = target();

    private static final Vector3f HIPS = new Vector3f();
    private static final Vector3f NECK = new Vector3f();
    private static final Vector3f OTHER = new Vector3f();
    private static final Vector3f AT = new Vector3f();
    private static final Vector3f OWN = new Vector3f();
    private static final Vector3f POLE = new Vector3f();
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf BEND = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    @Nullable
    private static Method scaleOf;
    private static boolean scaleMissing;

    private FightPoses() {
    }

    // Poses `model` as `body`'s side of the fight against `other`, `t` ticks in.
    static boolean pose(HumanoidModel<?> model, LivingEntity body, LivingEntity other, boolean fighter, float t,
            float partialTick) {
        if (!Stance.person(model)) {
            return false;
        }
        float[] mine = Keyframes.at(fighter ? FIGHTER : TARGET, t);
        float[] theirs = Keyframes.at(fighter ? TARGET : FIGHTER, t);
        Vec3 here = body.getPosition(partialTick);
        float yaw = Mth.rotLerp(partialTick, body.yBodyRotO, body.yBodyRot);
        float scale = scale(body);
        // The other's neck as its keys put it, through the world into this body's model.
        Vec3 there = other.getPosition(partialTick);
        float otherYaw = Mth.rotLerp(partialTick, other.yBodyRotO, other.yBodyRot);
        Stance.neck(HIPS.set(theirs[HX], Stance.HIP_Y + theirs[HY], theirs[HZ]), lean(theirs, LEAN),
                bend(theirs, BEND), AT);
        toModel(here, yaw, scale, toWorld(there, otherYaw, scale(other), AT), OTHER);

        Stance.trunk(model, HIPS.set(mine[HX], Stance.HIP_Y + mine[HY], mine[HZ]), lean(mine, LEAN),
                bend(mine, BEND), false);
        Stance.neck(NECK);
        Stance.chest(CHEST);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            int o = right ? RIGHT_FOOT : LEFT_FOOT;
            OWN.set(mine[o], Stance.GROUND - mine[o + 1], mine[o + 2]);
            AT.set(OTHER).add(mine[o], mine[o + 1], mine[o + 2]);
            OWN.lerp(AT, Mth.clamp(mine[o + 3], 0.0F, 1.0F));
            POLE.set(KNEE_AHEAD);
            if (right) {
                POLE.lerp(KNEE_OUT, Mth.clamp(mine[KNEE], 0.0F, 1.0F));
            }
            Stance.leg(model, right, OWN, POLE.normalize());
        }
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            int o = right ? RIGHT : LEFT;
            CHEST.transform(OWN.set(mine[o], mine[o + 1], mine[o + 2])).add(NECK);
            AT.set(OTHER).add(mine[o], mine[o + 1], mine[o + 2]);
            OWN.lerp(AT, Mth.clamp(mine[o + 3], 0.0F, 1.0F));
            float out = Mth.clamp(mine[ELBOWS + side], -1.0F, 1.0F);
            POLE.set(ELBOW_DOWN).lerp(out >= 0.0F ? ELBOW_OUT : ELBOW_IN, Math.abs(out));
            if (!right) {
                POLE.x = -POLE.x;
            }
            Stance.arm(model, right, OWN, POLE.normalize());
        }
        // The eyes on the other's face, the key's turn on top.
        AT.set(OTHER).add(FACE).sub(NECK.x, NECK.y - EYES, NECK.z);
        float flat = (float) Math.sqrt(AT.x * AT.x + AT.z * AT.z);
        model.head.yRot = (float) Math.atan2(-AT.x, -AT.z) + mine[LOOK];
        model.head.xRot = (float) Math.atan2(AT.y, flat) + mine[LOOK + 1];
        model.head.zRot = mine[LOOK + 2];
        model.hat.copyFrom(model.head);
        return true;
    }

    private static Quaternionf lean(float[] v, Quaternionf out) {
        return out.rotationZYX(v[ROLL] * 0.6F, v[TWIST], v[PITCH] * 0.6F);
    }

    private static Quaternionf bend(float[] v, Quaternionf out) {
        return out.rotationZYX(v[ROLL] * 0.4F, v[WAIST_TWIST], v[PITCH] * 0.4F + v[WAIST]);
    }

    // A point of `entity`'s drawn model (pixels, as above) in the world, the entity standing at `at` turned `yaw`.
    private static Vec3 toWorld(Vec3 at, float yaw, float scale, Vector3f model) {
        float f = scale / 16.0F;
        double x = -model.x * f;
        double z = model.z * f;
        double turn = Math.toRadians(yaw - 180.0F);
        double c = Math.cos(turn);
        double s = Math.sin(turn);
        return at.add(x * c - z * s, (Stance.GROUND - model.y) * f, x * s + z * c);
    }

    // A point of the world in the drawn model of an entity standing at `at` turned `yaw` (see LivingEntityRenderer:
    // the body turned by 180 - its yaw and flipped on x and y, its feet at y 24).
    private static Vector3f toModel(Vec3 at, float yaw, float scale, Vec3 world, Vector3f out) {
        double dx = world.x - at.x;
        double dz = world.z - at.z;
        double turn = Math.toRadians(yaw - 180.0F);
        double c = Math.cos(turn);
        double s = Math.sin(turn);
        float f = 16.0F / scale;
        return out.set((float) (-(dx * c + dz * s) * f), (float) (Stance.GROUND - (world.y - at.y) * f),
                (float) ((-dx * s + dz * c) * f));
    }

    // How big `entity` is drawn: its own size and its renderer's (a player 15/16, a husk 17/16).
    static float scale(LivingEntity entity) {
        float own = 1.0F;
        EntityRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
        if (renderer instanceof LivingEntityRenderer<?, ?> && !scaleMissing) {
            try {
                if (scaleOf == null) {
                    scaleOf = LivingEntityRenderer.class.getDeclaredMethod("scale", LivingEntity.class,
                            PoseStack.class, float.class);
                    scaleOf.setAccessible(true);
                }
                PoseStack pose = new PoseStack();
                scaleOf.invoke(renderer, entity, pose, 0.0F);
                own = pose.last().pose().m11();
            } catch (ReflectiveOperationException | RuntimeException e) {
                scaleMissing = true;
            }
        }
        return entity.getScale() * (own > 0.01F ? own : 1.0F);
    }

    private static final class Pose {
        private final float[] v = new float[VALUES];

        private Pose copy() {
            Pose pose = new Pose();
            System.arraycopy(this.v, 0, pose.v, 0, VALUES);
            return pose;
        }

        private Pose hips(float x, float drop, float back) {
            return this.set(HX, x, drop, back);
        }

        private Pose lean(float pitch, float roll, float twist) {
            return this.set(PITCH, pitch, roll, twist);
        }

        private Pose waist(float pitch, float twist) {
            this.v[WAIST] = pitch;
            this.v[WAIST_TWIST] = twist;
            return this;
        }

        private Pose right(float x, float y, float z) {
            return this.set(RIGHT, x, y, z).at(RIGHT, 0.0F);
        }

        private Pose rightAt(float x, float y, float z) {
            return this.set(RIGHT, x, y, z).at(RIGHT, 1.0F);
        }

        private Pose left(float x, float y, float z) {
            return this.set(LEFT, x, y, z).at(LEFT, 0.0F);
        }

        private Pose leftAt(float x, float y, float z) {
            return this.set(LEFT, x, y, z).at(LEFT, 1.0F);
        }

        private Pose rightFoot(float x, float lift, float z) {
            return this.set(RIGHT_FOOT, x, lift, z).at(RIGHT_FOOT, 0.0F);
        }

        // Kicking: from the other's neck, y down from it.
        private Pose rightFootAt(float x, float y, float z) {
            return this.set(RIGHT_FOOT, x, y, z).at(RIGHT_FOOT, 1.0F);
        }

        private Pose leftFoot(float x, float lift, float z) {
            return this.set(LEFT_FOOT, x, lift, z).at(LEFT_FOOT, 0.0F);
        }

        private Pose look(float yaw, float nod, float tilt) {
            return this.set(LOOK, yaw, nod, tilt);
        }

        private Pose elbows(float right, float left) {
            this.v[ELBOWS] = right;
            this.v[ELBOWS + 1] = left;
            return this;
        }

        private Pose knee(float out) {
            this.v[KNEE] = out;
            return this;
        }

        private Pose set(int at, float a, float b, float c) {
            this.v[at] = a;
            this.v[at + 1] = b;
            this.v[at + 2] = c;
            return this;
        }

        private Pose at(int at, float other) {
            this.v[at + 3] = other;
            return this;
        }
    }

    private static Keyframes.Key key(float tick, Pose pose) {
        return new Keyframes.Key(tick, false, pose.v.clone());
    }

    // A blow landing, or a foot set down: still for that moment.
    private static Keyframes.Key stop(float tick, Pose pose) {
        return new Keyframes.Key(tick, true, pose.v.clone());
    }

    // The player: a guard, its hook ducked under, a left hook to the body, a right uppercut, its jab slapped aside,
    // a right cross, a step in and a right roundhouse kick, set down again and back to a guard.
    private static Keyframes.Key[] fighter() {
        Pose easy = new Pose().hips(0.0F, 0.3F, 0.0F).lean(0.04F, 0.0F, 0.15F).waist(0.0F, 0.05F)
                .right(-5.5F, 11.5F, -0.5F).left(5.5F, 11.5F, -0.5F).rightFoot(-2.2F, 0.0F, 1.5F)
                .leftFoot(2.2F, 0.0F, -1.5F);
        Pose guard = new Pose().hips(0.0F, 1.5F, 0.5F).lean(0.12F, 0.0F, 0.35F).waist(0.05F, 0.1F)
                .right(-2.2F, 0.5F, -4.5F).left(2.8F, 0.0F, -7.0F).rightFoot(-2.6F, 0.0F, 3.5F)
                .leftFoot(2.4F, 0.0F, -3.5F).look(0.0F, 0.1F, 0.0F);
        Pose low = guard.copy().hips(-1.0F, 5.0F, 0.0F).lean(0.45F, -0.15F, 0.2F).waist(0.1F, 0.05F)
                .right(-1.8F, 0.0F, -4.0F).left(2.2F, 0.0F, -5.0F).look(0.0F, 0.15F, 0.0F);
        Pose stepped = guard.copy().leftFoot(1.4F, 0.0F, -8.0F);
        Pose chamber = stepped.copy().hips(-0.4F, 1.4F, -4.5F).lean(-0.12F, 0.1F, -0.5F).waist(0.0F, 0.3F)
                .rightFoot(-3.5F, 9.0F, -4.0F).knee(1.0F).right(-6.0F, 6.0F, 2.0F).left(2.0F, -1.0F, -5.0F);
        return new Keyframes.Key[] {
                key(0.0F, easy),
                key(9.0F, guard),
                key(14.0F, guard.copy().hips(0.0F, 2.1F, 0.5F)),
                key(18.0F, guard),
                key(21.0F, guard.copy().hips(-0.5F, 3.0F, 0.2F).lean(0.25F, -0.05F, 0.3F).look(0.0F, 0.2F, 0.0F)),
                key(24.0F, low),
                key(27.0F, low.copy().hips(-0.8F, 4.8F, -0.3F).lean(0.4F, -0.08F, 0.15F)),
                key(29.0F, low.copy().hips(-0.3F, 3.6F, -0.8F).lean(0.3F, 0.05F, 0.0F).left(6.0F, 4.5F, -3.0F)
                        .elbows(0.0F, 1.0F)),
                stop(32.0F, low.copy().hips(-0.3F, 3.0F, -1.5F).lean(0.25F, 0.08F, 0.95F).waist(0.05F, 0.1F)
                        .leftAt(2.5F, 5.5F, 3.0F).right(-2.0F, 0.0F, -4.0F).elbows(0.0F, 1.0F)),
                key(35.0F, guard.copy().hips(0.0F, 2.6F, -1.0F).lean(0.2F, 0.0F, 0.5F).left(3.0F, 1.0F, -6.0F)
                        .elbows(0.0F, 0.3F)),
                key(38.0F, guard.copy().hips(0.3F, 4.0F, -1.0F).lean(0.22F, -0.12F, 0.6F).right(-3.5F, 7.5F, -3.5F)
                        .elbows(-1.0F, 0.0F)),
                stop(42.0F, guard.copy().hips(0.2F, 1.0F, -1.8F).lean(0.02F, 0.05F, -0.35F)
                        .rightAt(0.0F, -0.5F, 4.5F).left(2.2F, 0.0F, -4.5F).elbows(-1.0F, 0.0F)),
                key(45.0F, guard.copy().hips(0.1F, 0.8F, -1.4F).lean(-0.02F, 0.02F, -0.2F).right(-1.5F, -4.0F, -7.0F)
                        .elbows(-0.4F, 0.0F)),
                key(50.0F, guard.copy().hips(0.0F, 1.8F, -0.5F)),
                key(55.0F, guard.copy().hips(0.0F, 1.6F, 0.0F)),
                stop(58.0F, guard.copy().hips(0.6F, 1.8F, 0.0F).right(0.5F, -0.5F, -7.0F).look(0.12F, 0.08F, 0.0F)),
                key(60.0F, guard.copy().hips(0.4F, 1.8F, 0.0F).lean(0.12F, 0.0F, 0.5F).right(2.0F, 0.5F, -6.0F)),
                stop(64.0F, guard.copy().hips(0.0F, 1.6F, -2.0F).lean(0.22F, 0.0F, -0.45F).rightAt(0.0F, -3.5F, 4.5F)
                        .left(2.2F, 0.0F, -4.5F)),
                key(67.0F, guard.copy().hips(0.0F, 1.7F, -1.0F).lean(0.12F, 0.0F, 0.0F).right(-2.2F, 0.5F, -5.0F)),
                key(72.0F, guard.copy().hips(0.0F, 1.8F, 0.0F)),
                key(76.0F, guard.copy().hips(0.0F, 2.0F, -1.5F).lean(0.1F, 0.0F, 0.45F).leftFoot(2.0F, 1.5F, -5.5F)),
                stop(80.0F, stepped.copy().hips(0.0F, 1.8F, -3.5F).lean(0.08F, 0.0F, 0.6F)),
                key(84.0F, stepped.copy().hips(-0.2F, 1.6F, -4.2F).lean(0.0F, 0.05F, 0.2F)
                        .rightFoot(-2.6F, 1.0F, 1.5F)),
                key(88.0F, chamber),
                key(91.0F, chamber.copy().hips(-0.5F, 1.3F, -4.6F).lean(-0.28F, 0.15F, -1.1F).waist(0.0F, 0.45F)
                        .rightFoot(-3.0F, 8.0F, -9.0F)),
                stop(94.0F, chamber.copy().hips(-0.6F, 1.2F, -4.6F).lean(-0.35F, 0.15F, -1.4F).waist(0.0F, 0.5F)
                        .rightFootAt(-2.0F, 6.0F, 3.0F).right(-6.0F, 7.0F, 3.0F)),
                key(97.0F, chamber.copy().hips(-0.6F, 1.2F, -4.6F).lean(-0.3F, 0.1F, -1.5F).waist(0.0F, 0.5F)
                        .rightFoot(-0.5F, 9.0F, -13.0F).right(-6.0F, 7.0F, 3.0F)),
                key(101.0F, chamber.copy().hips(-0.3F, 1.5F, -4.0F).lean(-0.1F, 0.05F, -0.6F).waist(0.0F, 0.2F)
                        .rightFoot(-2.5F, 8.0F, -5.0F).knee(0.6F)),
                stop(107.0F, stepped.copy().hips(0.0F, 2.0F, -3.0F).lean(0.08F, 0.0F, 0.0F)
                        .rightFoot(-2.6F, 0.0F, -0.5F)),
                key(114.0F, stepped.copy().hips(0.0F, 1.8F, -2.5F).lean(0.1F, 0.0F, 0.3F)
                        .rightFoot(-2.6F, 0.0F, -0.5F)),
                key(119.0F, guard.copy().hips(0.0F, 1.8F, -1.0F).leftFoot(2.0F, 1.2F, -5.5F)
                        .rightFoot(-2.6F, 0.0F, 1.5F)),
                key(123.0F, guard),
                key(133.0F, guard.copy().hips(0.0F, 1.4F, 0.5F)),
                key(146.0F, easy),
                key(150.0F, easy)
        };
    }

    // The creature: a brawler's guard, a wild right hook the player ducks, doubled over by the body shot, its head
    // snapped up by the uppercut, a step back, a jab slapped aside, the cross spinning its head round, then dazed on its
    // feet until the kick throws it (there it goes limp, and its keys stop).
    private static Keyframes.Key[] target() {
        Pose easy = new Pose().hips(0.0F, 0.2F, 0.0F).lean(0.06F, 0.0F, 0.1F).right(-5.5F, 11.5F, -0.5F)
                .left(5.5F, 11.5F, -0.5F).rightFoot(-2.2F, 0.0F, 1.2F).leftFoot(2.2F, 0.0F, -1.2F);
        Pose guard = new Pose().hips(0.0F, 1.0F, 0.5F).lean(0.18F, 0.0F, 0.3F).waist(0.06F, 0.05F)
                .right(-2.6F, 1.5F, -4.0F).left(2.6F, 1.0F, -5.5F).rightFoot(-2.4F, 0.0F, 3.0F)
                .leftFoot(2.4F, 0.0F, -3.0F).look(0.0F, 0.12F, 0.0F);
        Pose bent = guard.copy().hips(0.8F, 2.4F, -0.6F).lean(0.45F, -0.15F, 0.0F).waist(0.12F, 0.0F)
                .right(-3.0F, 4.5F, -2.0F).left(2.0F, 3.5F, -4.0F).look(0.0F, 0.3F, 0.0F);
        Pose back = guard.copy().hips(0.0F, 1.6F, 1.5F).rightFoot(-2.4F, 0.0F, 5.0F);
        Pose dazed = back.copy().hips(0.0F, 2.8F, 2.0F).lean(0.18F, 0.06F, 0.0F).leftFoot(2.4F, 0.0F, -1.0F)
                .right(-5.0F, 8.0F, -1.5F).left(5.0F, 7.5F, -1.5F).look(0.1F, 0.25F, 0.15F);
        return new Keyframes.Key[] {
                key(0.0F, easy),
                key(10.0F, guard),
                key(15.0F, guard.copy().hips(0.0F, 1.4F, 0.5F)),
                key(18.0F, guard.copy().hips(0.2F, 1.6F, 0.8F).lean(0.12F, 0.05F, 0.75F).waist(0.04F, 0.1F)
                        .right(-7.0F, 2.0F, 1.5F).elbows(1.0F, 0.0F)),
                key(21.0F, guard.copy().hips(0.0F, 1.8F, -0.8F).lean(0.25F, 0.0F, 0.2F).right(-4.5F, -1.0F, -7.5F)
                        .elbows(1.0F, 0.0F)),
                key(24.0F, guard.copy().hips(-0.2F, 2.0F, -2.0F).lean(0.32F, -0.05F, -0.35F)
                        .right(-0.5F, -1.5F, -9.0F).elbows(1.0F, 0.0F)),
                key(27.0F, guard.copy().hips(-0.3F, 2.2F, -2.2F).lean(0.36F, 0.1F, -0.85F).right(2.5F, 0.5F, -6.0F)
                        .elbows(0.6F, 0.0F)),
                stop(32.0F, guard.copy().hips(0.0F, 1.8F, -1.2F).lean(0.24F, 0.0F, -0.2F).right(-2.0F, 2.0F, -5.0F)),
                key(34.0F, bent.copy().hips(1.0F, 2.8F, -0.6F).lean(0.55F, -0.22F, -0.1F).look(0.1F, 0.35F, -0.1F)),
                key(38.0F, bent),
                stop(42.0F, bent.copy().hips(0.6F, 2.2F, -0.6F).lean(0.42F, -0.1F, 0.0F)),
                key(44.0F, guard.copy().hips(0.4F, 1.8F, 0.3F).lean(-0.1F, 0.0F, 0.05F).waist(-0.4F, 0.0F)
                        .right(-5.5F, -2.5F, -2.0F).left(5.5F, -2.0F, -2.0F).look(0.0F, -0.8F, 0.05F)),
                key(48.0F, guard.copy().hips(0.2F, 1.4F, 2.0F).lean(-0.08F, 0.05F, 0.15F).right(-4.5F, 3.0F, -3.0F)
                        .left(4.0F, 2.5F, -4.0F).look(0.0F, -0.3F, 0.0F).rightFoot(-2.4F, 1.5F, 4.5F)),
                stop(51.0F, back),
                key(54.0F, back.copy().lean(0.15F, 0.0F, 0.45F).left(2.8F, 0.5F, -4.5F)),
                stop(58.0F, back.copy().hips(0.0F, 1.6F, 0.5F).lean(0.22F, 0.0F, 0.6F).leftAt(-0.5F, -3.0F, 7.0F)),
                key(60.0F, back.copy().hips(0.0F, 1.6F, 0.5F).lean(0.25F, -0.08F, 0.0F).left(-2.5F, 0.5F, -7.5F)),
                stop(64.0F, back.copy().hips(0.0F, 1.6F, 0.6F).lean(0.22F, -0.05F, 0.05F).left(-1.5F, 1.0F, -6.5F)),
                key(66.0F, back.copy().hips(-0.5F, 1.8F, 2.0F).lean(-0.18F, 0.12F, -0.5F).right(-6.0F, 7.5F, 0.0F)
                        .left(5.0F, 6.5F, -2.0F).look(0.65F, -0.3F, 0.25F)),
                key(70.0F, dazed.copy().hips(-0.3F, 2.6F, 2.4F).lean(0.05F, 0.08F, -0.25F)
                        .leftFoot(2.4F, 1.2F, -1.5F).look(0.3F, 0.0F, 0.2F)),
                key(73.0F, dazed),
                key(80.0F, dazed.copy().hips(0.4F, 2.5F, 1.6F).lean(0.22F, -0.08F, 0.1F).look(-0.1F, 0.3F, -0.12F)),
                key(87.0F, dazed.copy().hips(-0.3F, 2.7F, 1.4F).lean(0.26F, 0.08F, -0.05F).look(0.1F, 0.32F, 0.1F)),
                key(94.0F, dazed.copy().hips(0.0F, 2.6F, 1.2F).lean(0.28F, 0.0F, 0.0F))
        };
    }
}
