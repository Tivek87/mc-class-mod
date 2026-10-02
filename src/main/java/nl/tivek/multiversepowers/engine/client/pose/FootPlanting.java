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
    // The most a leg is turned, in radians, and how much of the way to its new turn it goes each frame.
    private static final float FURTHEST = 0.8F;
    private static final float EASE = 0.25F;
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
        boolean standing = entity.isAlive() && !entity.isPassenger() && !entity.isSleeping() && !entity.isFallFlying()
                && !entity.isInWater() && !entity.isSwimming() && entity.getPose() != Pose.SPIN_ATTACK
                && entity.distanceToSqr(camera) < NEAR * NEAR;
        float[] turns = TURNS.get(entity.getId());
        if (!standing && turns == null) {
            return;
        }
        if (turns == null || turns.length != legs.length * 2 + 1) {
            turns = new float[legs.length * 2 + 1];
            TURNS.put(entity.getId(), turns);
        }
        // The last slot keeps when these feet were last drawn.
        turns[legs.length * 2] = tick;
        boolean still = entity.walkAnimation.speed(partialTick) < STILL;
        boolean moved = false;
        for (int i = 0; i < legs.length; i++) {
            Leg leg = legs[i];
            ModelPart part = leg.limb().part();
            float wantX = 0.0F;
            float wantZ = 0.0F;
            // A leg a pose bent (Stance) was placed on purpose, and its straight tip is not where its foot is.
            if (standing && part.visible && !Limbs.bent(model, part)) {
                double foot = height(model, drawn, leg, camera);
                double ground = ground(level, TIP.x + camera.x, TIP.z + camera.z, foot);
                double gap = ground - foot;
                if (!Double.isNaN(ground) && (gap > ON || still && gap < -ON)) {
                    float[] turn = reach(model, drawn, leg, camera, gap);
                    wantX = turn[0];
                    wantZ = turn[1];
                }
            }
            turns[i * 2] += (wantX - turns[i * 2]) * EASE;
            turns[i * 2 + 1] += (wantZ - turns[i * 2 + 1]) * EASE;
            part.xRot += turns[i * 2];
            part.zRot += turns[i * 2 + 1];
            moved |= Math.abs(turns[i * 2]) > 1.0E-4F || Math.abs(turns[i * 2 + 1]) > 1.0E-4F;
        }
        if (!moved && !standing) {
            TURNS.remove(entity.getId());
        }
    }

    // The turn about the leg's own x or z axis (whichever moves its tip up or down the most) that brings its tip
    // `gap` blocks higher, within reach.
    private static float[] reach(EntityModel<?> model, Matrix4f drawn, Leg leg, Vec3 camera, double gap) {
        ModelPart part = leg.limb().part();
        float x = part.xRot;
        float z = part.zRot;
        double base = height(model, drawn, leg, camera);
        part.xRot = x + TRY;
        double byX = (height(model, drawn, leg, camera) - base) / TRY;
        part.xRot = x;
        part.zRot = z + TRY;
        double byZ = (height(model, drawn, leg, camera) - base) / TRY;
        part.zRot = z;
        boolean alongX = Math.abs(byX) >= Math.abs(byZ);
        double rate = alongX ? byX : byZ;
        float[] turn = new float[2];
        if (Math.abs(rate) < 1.0E-3) {
            return turn;
        }
        // A straight leg lifts its foot whichever way it turns: turn the way it is already turned, so a step forward
        // becomes a step up, not a kick back.
        float angle = (float) Mth.clamp(gap / rate, -FURTHEST, FURTHEST);
        if (gap > 0.0 && Math.signum(rate) != Math.signum(gap)) {
            angle = -angle;
        }
        if (alongX) {
            turn[0] = angle;
        } else {
            turn[1] = angle;
        }
        return turn;
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
        TURNS.values().removeIf(turns -> tick - turns[turns.length - 1] > unseen);
    }
}
