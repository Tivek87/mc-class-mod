package nl.tivek.multiversepowers.engine.client.pose;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// Feet on the ground they stand on. Legs have no knees, so a leg can only turn: a foot that would sink into a step
// or a slab turns up out of it, and a leg spread out to the side (a spider's) turns down till its tip rests on the
// ground when the creature stands still. Only near the camera, and eased in and out so nothing snaps.
final class FootPlanting {
    // How far from the camera feet are planted, in blocks.
    private static final double NEAR = 24.0;
    // How far up or down a foot looks for ground, and how far from it a foot counts as on it, in blocks.
    private static final double LOOK = 0.6;
    private static final double ON = 0.03;
    // The most a leg is turned, in radians, and how much of the way to its new turn it goes each frame at 60 frames a
    // second (FRAMES_A_TICK frames a tick).
    private static final float FURTHEST = 0.8F;
    private static final float EASE = 0.25F;
    private static final float FRAMES_A_TICK = 3.0F;
    private static final int WRAP = 100000;
    // Below this walking speed a creature stands still, and its spread legs reach down.
    private static final float STILL = 0.1F;
    private static final float TRY = 0.01F;

    private record Leg(ModelParts.Part limb, float[] tip) {
    }

    private static final Map<EntityModel<?>, Leg[]> LEGS = new WeakHashMap<>();
    private static final Int2ObjectOpenHashMap<float[]> TURNS = new Int2ObjectOpenHashMap<>();
    private static final Matrix4f FRAME = new Matrix4f();
    private static final Vector3f TIP = new Vector3f();
    private static final BlockPos.MutableBlockPos AT = new BlockPos.MutableBlockPos();

    private FootPlanting() {
    }

    static void plant(EntityModel<?> model, LivingEntity entity, Matrix4f drawn, float partialTick, int tick) {
        Leg[] legs = LEGS.computeIfAbsent(model, FootPlanting::legs);
        if (legs.length == 0) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Level level = entity.level();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        // An armor stand is a statue: its legs stay as they were posed.
        boolean standing = entity.isAlive() && !(entity instanceof ArmorStand) && !entity.isPassenger()
                && !entity.isSleeping() && !entity.isFallFlying() && !entity.isInWater() && !entity.isSwimming()
                && entity.getPose() != Pose.SPIN_ATTACK && entity.distanceToSqr(camera) < NEAR * NEAR;
        float[] turns = TURNS.get(entity.getId());
        if (!standing && turns == null) {
            return;
        }
        if (turns == null || turns.length != legs.length * 2 + 1) {
            turns = new float[legs.length * 2 + 1];
            turns[legs.length * 2] = clock(tick, partialTick);
            TURNS.put(entity.getId(), turns);
        }
        // The last slot keeps when these feet were last drawn (clock); they ease towards where they go by the time
        // since.
        float now = clock(tick, partialTick);
        float since = Mth.clamp(sinceThen(now, turns[legs.length * 2]), 0.0F, 2.0F);
        turns[legs.length * 2] = now;
        float ease = 1.0F - (float) Math.pow(1.0F - EASE, since * FRAMES_A_TICK);
        boolean still = entity.walkAnimation.speed(partialTick) < STILL;
        boolean moved = false;
        for (int i = 0; i < legs.length; i++) {
            Leg leg = legs[i];
            ModelPart part = leg.limb().part();
            float wantX = 0.0F;
            float wantZ = 0.0F;
            // A leg a pose bent (Stance) was placed on purpose, and its straight tip is not where its foot is: it
            // keeps just that place.
            if (Limbs.bent(model, part)) {
                turns[i * 2] = 0.0F;
                turns[i * 2 + 1] = 0.0F;
                continue;
            }
            if (standing && part.visible) {
                double foot = height(model, drawn, leg, camera);
                double ground = ground(level, TIP.x + camera.x, TIP.z + camera.z, foot);
                double gap = ground - foot;
                if (!Double.isNaN(ground) && (gap > ON || still && gap < -ON)) {
                    float[] turn = reach(model, drawn, leg, camera, gap);
                    wantX = turn[0];
                    wantZ = turn[1];
                }
            }
            turns[i * 2] += (wantX - turns[i * 2]) * ease;
            turns[i * 2 + 1] += (wantZ - turns[i * 2 + 1]) * ease;
            part.xRot += turns[i * 2];
            part.zRot += turns[i * 2 + 1];
            moved |= Math.abs(turns[i * 2]) > 1.0E-4F || Math.abs(turns[i * 2 + 1]) > 1.0E-4F;
        }
        if (!moved && !standing) {
            TURNS.remove(entity.getId());
        }
    }

    // The least turn about the leg's own x or z axis (whichever moves its tip up or down the most) that brings its tip
    // `gap` blocks higher; none when no turn within FURTHEST does (a leg hanging straight down only lifts its tip,
    // whichever way it turns).
    private static float[] reach(EntityModel<?> model, Matrix4f drawn, Leg leg, Vec3 camera, double gap) {
        ModelPart part = leg.limb().part();
        float x = part.xRot;
        float z = part.zRot;
        double base = height(model, drawn, leg, camera);
        double[] byX = slopes(model, drawn, leg, camera, base, true);
        part.xRot = x;
        double[] byZ = slopes(model, drawn, leg, camera, base, false);
        part.zRot = z;
        boolean alongX = Math.abs(byX[0]) >= Math.abs(byZ[0]);
        double[] by = alongX ? byX : byZ;
        float[] turn = new float[2];
        double angle = turnFor(by[0], by[1], gap);
        if (!Double.isNaN(angle)) {
            turn[alongX ? 0 : 1] = (float) angle;
        }
        return turn;
    }

    // The least turn that lifts a tip `gap` higher when turning it by t lifts it a sin t + b (1 - cos t), as it goes
    // round a circle; NaN when none within FURTHEST does.
    static double turnFor(double a, double b, double gap) {
        double r = Math.sqrt(a * a + b * b);
        if (r < 1.0E-4 || Math.abs(gap - b) > r) {
            return Double.NaN;
        }
        double phase = Math.atan2(b, a);
        double reached = Math.asin((gap - b) / r);
        double first = Mth.wrapDegrees(Math.toDegrees(phase + reached)) * Mth.DEG_TO_RAD;
        double second = Mth.wrapDegrees(Math.toDegrees(phase + Math.PI - reached)) * Mth.DEG_TO_RAD;
        double angle = Math.abs(first) <= Math.abs(second) ? first : second;
        return Math.abs(angle) > FURTHEST ? Double.NaN : angle;
    }

    // How fast the tip rises as the leg turns about its x (or z) axis, and how much a turn either way lifts it beyond
    // that: a and b of the circle it goes round.
    private static double[] slopes(EntityModel<?> model, Matrix4f drawn, Leg leg, Vec3 camera, double base,
            boolean aboutX) {
        ModelPart part = leg.limb().part();
        float was = aboutX ? part.xRot : part.zRot;
        set(part, aboutX, was + TRY);
        double up = height(model, drawn, leg, camera);
        set(part, aboutX, was - TRY);
        double down = height(model, drawn, leg, camera);
        set(part, aboutX, was);
        double a = (up - down) / (2.0 * TRY);
        double b = (up + down - 2.0 * base) / (TRY * TRY);
        return new double[] { a, b };
    }

    private static void set(ModelPart part, boolean aboutX, float angle) {
        if (aboutX) {
            part.xRot = angle;
        } else {
            part.zRot = angle;
        }
    }

    // How high (world, but less the camera's y) the leg's tip is as the model stands now; leaves the tip in TIP.
    private static double height(EntityModel<?> model, Matrix4f drawn, Leg leg, Vec3 camera) {
        ModelParts.frame(model, drawn, leg.limb(), FRAME);
        FRAME.transformPosition(TIP.set(leg.tip()[0], leg.tip()[1], leg.tip()[2]));
        return TIP.y + camera.y;
    }

    // The top of the highest solid thing in the block column under (x, z) within reach of a foot at y; NaN if none.
    private static double ground(Level level, double x, double z, double y) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        for (int by = Mth.floor(y + LOOK); by >= Mth.floor(y - LOOK); by--) {
            AT.set(bx, by, bz);
            BlockState state = level.getBlockState(AT);
            if (state.isAir()) {
                continue;
            }
            VoxelShape shape = state.getCollisionShape(level, AT);
            if (shape.isEmpty()) {
                continue;
            }
            double top = by + shape.max(Direction.Axis.Y, z - bz, x - bx);
            if (top <= y + LOOK && top >= y - LOOK) {
                return top;
            }
        }
        return Double.NaN;
    }

    // The legs of a model: its parts named or known as legs, each with its sole.
    private static Leg[] legs(EntityModel<?> model) {
        List<Leg> legs = new ArrayList<>();
        List<ModelParts.Part> parts = ModelParts.of(model);
        if (parts == null) {
            return new Leg[0];
        }
        for (ModelParts.Part part : parts) {
            if (part.role() == ModelParts.Role.LEG) {
                legs.add(new Leg(part, sole(part)));
            }
        }
        return legs.toArray(new Leg[0]);
    }

    // Where a leg stands, in blocks in its own frame: the far end of its longest side. A leg built reaching below the
    // floor its creature stands on (an enderman's, by a pixel) stands on that floor, as far up the leg: else its foot
    // would always seem sunk into a step and the leg would turn out of it.
    static float[] sole(ModelParts.Part part) {
        float[] b = part.bounds();
        int longest = 0;
        for (int a = 1; a < 3; a++) {
            if (b[a + 3] - b[a] > b[longest + 3] - b[longest]) {
                longest = a;
            }
        }
        float sign = Math.abs(b[longest + 3]) > Math.abs(b[longest]) ? 1.0F : -1.0F;
        float[] tip = new float[3];
        for (int a = 0; a < 3; a++) {
            tip[a] = (b[a] + b[a + 3]) * 0.5F;
        }
        tip[longest] = sign > 0.0F ? b[longest + 3] : b[longest];
        Matrix4f rest = ModelParts.rest(part, new Matrix4f());
        float below = rest.transformPosition(new Vector3f(tip[0], tip[1], tip[2])).y - Stance.GROUND;
        Vector3f way = new Vector3f();
        way.setComponent(longest, sign);
        float down = rest.transformDirection(way).y;
        if (below > 0.0F && down > 0.5F) {
            tip[longest] -= sign * below / down;
        }
        for (int a = 0; a < 3; a++) {
            tip[a] /= 16.0F;
        }
        return tip;
    }

    static void forget() {
        TURNS.clear();
    }

    static void forgetOld(int tick, int unseen) {
        float now = clock(tick, 0.0F);
        TURNS.values().removeIf(turns -> sinceThen(now, turns[turns.length - 1]) > unseen);
    }

    // The time (ticks) going round every WRAP ticks, so a float keeps it to a fraction of a tick however long the game
    // has run; and how long ago a time of it was.
    private static float clock(int tick, float partialTick) {
        return Math.floorMod(tick, WRAP) + partialTick;
    }

    private static float sinceThen(float now, float then) {
        float since = now - then;
        return since < 0.0F ? since + WRAP : since;
    }
}
