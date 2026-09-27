package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;
import nl.tivek.multiversepowers.engine.fx.Sounds;
import nl.tivek.multiversepowers.engine.math.Ease;

// A built mech's walk, worked out on every client from where it stands tick by tick: each foot stays where it came
// down until its turn to swing, lands where the body will be over it, and the body bobs, sways and twists above the
// legs. Phases count in strides: a full one is both legs, the right lifting at 0 and the left at a half.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MechWalk {
    static final double CYCLE = 4.6;
    private static final double SWING = 0.42;
    private static final double[] LIFTS = { 0.0, 0.5 };
    private static final double TURN_ARC = 4.0;
    private static final double SLOWEST = 1.0 / 40.0;
    private static final double SETTLE = 1.0 / 20.0;
    private static final double HOME = 0.35;
    private static final double HOME_TURN = 0.22;
    private static final double LOST = CYCLE * 1.5;
    private static final double LIFT = 1.05;
    private static final double CROUCH = 0.45;
    private static final double BOB = 0.12;
    private static final double SWAY = 0.15;
    private static final double ROLL = 0.035;
    private static final double TWIST = 0.07;
    private static final double LEAN = 0.05;
    private static final double BRACE = 2.5;
    private static final double LEVER_PACE = 0.25;
    private static final int IDLE_BEFORE = 20;
    private static final int PRESS_EVERY = 46;
    private static final int PRESS_TICKS = 18;
    private static final double GROUND_ABOVE = 4.0;
    private static final double GROUND_BELOW = 9.0;
    private static final int STEP_SHAKE_TICKS = 7;
    private static final double STEP_SHAKE_RANGE = 36.0;
    private static final int KEEP_BROKEN = 60;
    private static final Vec3 HIPS = new Vec3(0.0, MechScript.HIP.y, 0.0);
    private static final SoundEvent STEP = Sounds.of("mech.step");
    private static final Map<Integer, MechWalk> WALKS = new HashMap<>();
    private static final Map<Integer, Kept> BROKEN = new HashMap<>();
    private static int ticks;

    private record Kept(MechPose pose, int since) {
    }

    private static final class Leg {
        Vec3 planted;
        Vec3 toes;
        Vec3 from;
        Vec3 fromToes;
        boolean swinging;
        double lift;
        int landed = -100;
    }

    private final int seed;
    private final Leg[] legs = { new Leg(), new Leg() };
    private MechScript.Stage last;
    private MechPose was;
    private MechPose now;
    private int ticked = Integer.MIN_VALUE;
    private double phase;
    private double speed;
    private double turn;
    private double brace;
    private double walking;
    private double crouch;
    private double settleY;
    private int idle;

    private MechWalk(int seed, MechScript.Stage stage) {
        this.seed = seed;
        this.reset(stage);
    }

    private void reset(MechScript.Stage stage) {
        this.last = stage;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            leg.planted = home(stage, side);
            leg.toes = stage.ahead();
            leg.swinging = false;
        }
        this.now = this.pose(stage, 0.0);
        this.was = this.now;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        ticks++;
        Iterator<Map.Entry<Integer, Kept>> kept = BROKEN.entrySet().iterator();
        while (kept.hasNext()) {
            if (ticks - kept.next().getValue().since() > KEEP_BROKEN) {
                kept.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WALKS.clear();
        BROKEN.clear();
    }

    // Moves a mech's walk on to where it stands this tick; a second call in the same tick does nothing.
    public static void step(int id, MechScript.Stage stage) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        MechWalk walk = WALKS.computeIfAbsent(id, key -> new MechWalk(key, stage));
        if (walk.ticked == ticks) {
            return;
        }
        walk.ticked = ticks;
        walk.tick(level, stage);
    }

    @Nullable
    public static MechPose pose(int id, float partialTick) {
        MechWalk walk = WALKS.get(id);
        if (walk == null) {
            Kept kept = BROKEN.get(id);
            return kept == null ? null : kept.pose();
        }
        // A walk not yet moved on this tick shows where it got to last.
        double u = walk.ticked == ticks ? partialTick : 1.0;
        return MechPose.between(walk.was, walk.now, u);
    }

    @Nullable
    public static MechPose latest(int id) {
        MechWalk walk = WALKS.get(id);
        return walk == null ? null : walk.now;
    }

    // The mech breaks up: its last pose stays a while for its pieces to fly from.
    public static void stop(int id) {
        MechWalk walk = WALKS.remove(id);
        if (walk != null) {
            BROKEN.put(id, new Kept(walk.now, ticks));
        }
    }

    // How hard the last footfall of a mech shakes the view at from.
    static double shake(int id, Vec3 from, float partialTick, boolean inside) {
        MechWalk walk = WALKS.get(id);
        if (walk == null) {
            return 0.0;
        }
        double most = 0.0;
        for (int side = 0; side < 2; side++) {
            double since = ticks - walk.legs[side].landed + partialTick - 1.0;
            if (since < 0.0 || since >= STEP_SHAKE_TICKS) {
                continue;
            }
            double near = inside ? 1.0 : 1.0 - from.distanceTo(walk.legs[side].planted) / STEP_SHAKE_RANGE;
            if (near <= 0.0) {
                continue;
            }
            double fade = 1.0 - since / STEP_SHAKE_TICKS;
            most = Math.max(most, (inside ? 0.22 : 0.4) * fade * fade * Math.min(1.0, near * 1.3));
        }
        return most;
    }

    private void tick(ClientLevel level, MechScript.Stage stage) {
        Vec3 moved = stage.base().subtract(this.last.base());
        if (moved.lengthSqr() > LOST * LOST) {
            this.reset(stage);
            return;
        }
        double forward = moved.dot(stage.ahead());
        Vec3 across = this.last.ahead().cross(stage.ahead());
        double angle = Math.atan2(across.y, this.last.ahead().dot(stage.ahead()));
        double before = this.speed;
        this.speed = Mth.lerp(0.4, this.speed, forward);
        this.turn = Mth.lerp(0.4, this.turn, angle);
        this.brace = Mth.lerp(0.25, this.brace, this.speed - before);
        this.last = stage;
        double effort = Math.abs(forward) + TURN_ARC * Math.abs(angle);
        boolean moving = effort > 2.0E-3;
        boolean busy = this.legs[0].swinging || this.legs[1].swinging || this.away(stage, 0) || this.away(stage, 1);
        double rate = moving ? Math.max(effort / CYCLE, SLOWEST) : busy ? SETTLE : 0.0;
        double wanted = moving ? Math.min(1.0, effort / (MechDrive.WALK * 0.85)) : rate > 0.0 ? 0.3 : 0.0;
        this.walking += Mth.clamp(wanted - this.walking, -0.06, 0.08);
        this.crouch += Mth.clamp((moving || rate > 0.0 ? 1.0 : 0.0) - this.crouch, -0.04, 0.07);
        double from = this.phase;
        this.phase += rate;
        for (int side = 0; side < 2; side++) {
            this.leg(level, stage, side, from, rate);
        }
        this.was = this.now;
        this.now = this.pose(stage, rate);
    }

    private boolean away(MechScript.Stage stage, int side) {
        Leg leg = this.legs[side];
        Vec3 home = home(stage, side);
        double dx = leg.planted.x - home.x;
        double dz = leg.planted.z - home.z;
        return dx * dx + dz * dz > HOME * HOME || leg.toes.dot(stage.ahead()) < Math.cos(HOME_TURN);
    }

    private void leg(ClientLevel level, MechScript.Stage stage, int side, double from, double rate) {
        Leg leg = this.legs[side];
        double start = LIFTS[side];
        if (!leg.swinging && rate > 0.0 && Math.floor(this.phase - start) > Math.floor(from - start)) {
            leg.swinging = true;
            leg.from = leg.planted;
            leg.fromToes = leg.toes;
            leg.lift = LIFT * (0.55 + 0.45 * Math.min(1.0, this.walking * 1.4));
        }
        if (!leg.swinging) {
            return;
        }
        double into = this.phase - start - Math.floor(this.phase - start);
        boolean done = into >= SWING;
        double u = done ? 1.0 : into / SWING;
        double left = rate > 0.0 ? ((1.0 - u) * SWING + (1.0 - SWING) * 0.5) / rate : 0.0;
        double ahead = Mth.clamp(this.speed * left, -CYCLE * 0.45, CYCLE * 0.45);
        MechScript.Stage then = stage.turned(Vec3.ZERO, new Vec3(0.0, 0.0, ahead),
                Mth.clamp(this.turn * left, -0.9, 0.9), 0.0, 0.0);
        Vec3 target = home(then, side);
        Double ground = ground(level, target, stage.base().y);
        target = new Vec3(target.x, (ground == null ? stage.base().y : ground) + MechScript.ANKLE.y, target.z);
        double h = Ease.smooth(u);
        Vec3 at = leg.from.lerp(target, h);
        leg.toes = leg.fromToes.lerp(then.ahead(), h).normalize();
        leg.planted = done ? target : at.add(0.0, leg.lift * Math.sin(Math.PI * Math.pow(u, 0.8)), 0.0);
        if (done) {
            leg.swinging = false;
            leg.toes = then.ahead();
            leg.landed = ticks;
            footfall(level, target.subtract(0.0, MechScript.ANKLE.y, 0.0), this.walking);
        }
    }

    // Where a foot stands at rest under the mech, before the ground is found.
    private static Vec3 home(MechScript.Stage stage, int side) {
        return stage.point(MechPainter.side(MechScript.ANKLE, side == 0));
    }

    private static double tip(Leg leg, double phase, double start) {
        if (!leg.swinging) {
            return 0.0;
        }
        double u = Math.min(1.0, (phase - start - Math.floor(phase - start)) / SWING);
        return u < 0.3 ? -0.35 * Math.sin(Math.PI * u / 0.3) : 0.22 * Math.sin(Math.PI * (u - 0.3) / 0.7);
    }

    private MechPose pose(MechScript.Stage stage, double rate) {
        MechPose pose = new MechPose(stage);
        double w = this.walking;
        double p = this.phase;
        double feet = 0.0;
        for (int side = 0; side < 2; side++) {
            Leg leg = this.legs[side];
            pose.ankle[side] = leg.planted;
            pose.toes[side] = leg.toes;
            pose.tip[side] = tip(leg, p, LIFTS[side]);
            feet += (leg.swinging ? leg.from.y : leg.planted.y) - MechScript.ANKLE.y;
        }
        double settle = Mth.clamp(feet * 0.5 - stage.base().y, -1.5, 1.5);
        this.settleY = Mth.lerp(0.25, this.settleY, settle);
        double bob = BOB * w * Math.cos(4.0 * Math.PI * (p - 0.2));
        double sway = SWAY * w * Math.sin(2.0 * Math.PI * (p - 0.46));
        double roll = ROLL * w * Math.sin(2.0 * Math.PI * (p - 0.46));
        double twist = TWIST * w * Math.sin(2.0 * Math.PI * (p + 0.04));
        double pitch = -LEAN * this.speed / MechDrive.WALK + Mth.clamp(BRACE * this.brace, -0.05, 0.05);
        double low = -CROUCH * Ease.smooth(this.crouch) + bob + this.settleY * 0.6;
        pose.torso = stage.turned(HIPS, new Vec3(sway, low, 0.0), twist, pitch, roll);
        pose.walking = w;
        pose.swing = -Math.sin(2.0 * Math.PI * (p + 0.04)) * w;
        pose.headYaw = 0.6 * twist;
        pose.headPitch = 0.5 * pitch;
        this.levers(pose, rate);
        return pose;
    }

    private void levers(MechPose pose, double rate) {
        double push = this.speed / MechDrive.WALK;
        double steer = 0.8 * this.turn / MechDrive.TURN;
        MechPose before = this.now;
        double right = Mth.clamp(push + steer, -1.0, 1.0);
        double left = Mth.clamp(push - steer, -1.0, 1.0);
        pose.leverRight = before == null ? right : before.leverRight + Mth.clamp(right - before.leverRight,
                -LEVER_PACE, LEVER_PACE);
        pose.leverLeft = before == null ? left : before.leverLeft + Mth.clamp(left - before.leverLeft, -LEVER_PACE,
                LEVER_PACE);
        boolean still = rate <= 0.0 && Math.abs(pose.leverRight) < 0.05 && Math.abs(pose.leverLeft) < 0.05;
        this.idle = still ? this.idle + 1 : 0;
        if (this.idle < IDLE_BEFORE) {
            return;
        }
        // Standing still, the pilot keeps busy at the console: one button every few seconds.
        int since = this.idle - IDLE_BEFORE;
        int within = since % PRESS_EVERY;
        if (within < PRESS_TICKS) {
            pose.button = Math.floorMod(this.seed * 7 + since / PRESS_EVERY * 3, MechScript.BUTTONS.length);
            pose.press = within < 6 ? Ease.smooth(within / 6.0) * 0.8 : within < 9 ? 0.8 + 0.2 * Ease.bump(
                    (within - 6) / 3.0) : 0.8 * (1.0 - Ease.smooth((within - 9) / 9.0));
        }
    }

    // The top of the ground under at, looking from a little above from down: water counts, the trees and plants a mech
    // wades through do not.
    @Nullable
    static Double ground(ClientLevel level, Vec3 at, double from) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(Mth.floor(at.x), 0, Mth.floor(at.z));
        if (!level.isLoaded(pos.setY(Mth.floor(from)))) {
            return null;
        }
        for (int y = Mth.floor(from + GROUND_ABOVE); y >= Mth.floor(from - GROUND_BELOW); y--) {
            pos.setY(y);
            BlockState block = level.getBlockState(pos);
            if (wades(block)) {
                continue;
            }
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.isEmpty()) {
                return y + (double) fluid.getHeight(level, pos);
            }
            VoxelShape shape = block.getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return y + shape.max(Direction.Axis.Y);
            }
        }
        return null;
    }

    static boolean wades(BlockState block) {
        return block.is(BlockTags.LEAVES) || block.is(BlockTags.LOGS) || block.is(BlockTags.REPLACEABLE)
                || block.is(BlockTags.FLOWERS) || block.is(BlockTags.SAPLINGS);
    }

    private static void footfall(ClientLevel level, Vec3 ground, double walking) {
        float loud = (float) (0.6 + 0.6 * walking);
        float pitch = 0.9F + 0.2F * level.random.nextFloat();
        level.playLocalSound(ground.x, ground.y, ground.z, STEP, SoundSource.PLAYERS, 1.6F * loud, pitch, false);
        level.playLocalSound(ground.x, ground.y, ground.z, SoundEvents.IRON_GOLEM_STEP, SoundSource.PLAYERS,
                1.4F * loud, 0.5F, false);
    }
}
