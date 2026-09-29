package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechDrivePayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;

// The pilot walks their own mech in their own game, as a player walks, and tells the server where it went: W and S
// walk it on and back (the sprint key with W runs), A and D step it aside, and its legs turn after where the pilot looks.
// It stands on whatever its feet find, so a hole narrower than its stance never drops it in. It steps up no higher than
// its legs can, never walks off a drop too deep to see the bottom of or down into a hole its cockpit would not fit in,
// and stops where its cockpit would run into blocks (unless it already stands among them, so it can always get out).
// Held against a ledge too high to step onto, it climbs it (MechClimb).
public final class MechDrive {
    public static final double WALK = 0.2;
    // The sprint key held with W runs: two and a half times as fast, working up to it slowly.
    public static final double RUN = 0.5;
    private static final double RUN_UP = 0.018;
    // Standing still, the legs step round only when the look is this far (degrees)
    // off where they face.
    private static final double STEP_ROUND = 55.0;
    public static final double TURN = Math.toRadians(3.0);
    private static final double TURN_DEGREES = 3.0;
    // How much of the way still to turn the legs take on each tick, and how near
    // they count as there.
    private static final double TURN_GAIN = 0.12;
    private static final double SETTLED_DEGREES = 1.5;
    private static final double SIDESTEP = 0.1;
    private static final double BACK = 0.11;
    private static final double SPEED_UP = 0.012;
    private static final double SLOW_DOWN = 0.024;
    private static final double TURN_UP = Math.toRadians(0.35);
    // The highest step its legs take; a ledge higher than that it climbs.
    static final double STEP_UP = 2.6;
    private static final double CLIMB = 0.3;
    private static final double FALL = 0.06;
    private static final double FASTEST_FALL = 1.2;
    private static final double MOVED = 1.0E-5;
    // How far ahead of an ankle its foot meets a wall, and how many ticks it must be held against a ledge to climb it.
    private static final double TOE = MechGround.SOLE_AHEAD + 0.6;
    private static final int CLIMB_AFTER = 6;

    private static int mech = -1;
    private static Vec3 base = Vec3.ZERO;
    private static float yaw;
    private static double speed;
    private static double side;
    private static double turn;
    private static double fall;
    private static boolean steppingRound;
    private static int pressed;
    @Nullable
    private static MechScript.Stage climbFrom;
    private static double climbHeight;
    private static double climbEdge;
    private static int climbAge;

    private MechDrive() {
    }

    @Nullable
    public static MechScript.Stage stage(int id) {
        return id == mech ? MechScript.Stage.facing(base, yaw) : null;
    }

    // How far the pilot's own mech has got climbing, packed as the server passes it on; 0 when it does not climb.
    public static int climb(int id) {
        return id == mech && climbFrom != null ? MechClimb.pack(climbAge, climbHeight, climbEdge) : 0;
    }

    public static void forget() {
        mech = -1;
        climbFrom = null;
    }

    // `still`: it strikes a blow and stands its ground, its legs turning no more.
    public static void drive(LocalPlayer player, int id, MechScript.Stage server, Input input, boolean still) {
        if (id != mech) {
            player.displayClientMessage(Component.translatable("ring." + MultiversePowers.MODID + ".mech_drive",
                    Minecraft.getInstance().options.keySprint.getTranslatedKeyMessage()), true);
            mech = id;
            base = server.base();
            yaw = server.yaw();
            speed = 0.0;
            side = 0.0;
            turn = 0.0;
            fall = 0.0;
            steppingRound = false;
            pressed = 0;
            climbFrom = null;
        }
        if (climbFrom != null) {
            climbing();
            return;
        }
        boolean ahead = !still && input.forwardImpulse > 0.01F;
        boolean run = ahead && Minecraft.getInstance().options.keySprint.isDown();
        double want = ahead ? run ? RUN : WALK : !still && input.forwardImpulse < -0.01F ? -BACK : 0.0;
        speed += Mth.clamp(want - speed, -SLOW_DOWN, run ? RUN_UP : SPEED_UP);
        side += Mth.clamp((still ? 0.0 : input.leftImpulse * SIDESTEP) - side, -SLOW_DOWN, SPEED_UP);
        // The legs turn after where the pilot looks, no faster than they can, while the
        // torso swings there first.
        // Standing, they only step round once it has twisted far over them, then all
        // the way; walking, they keep up.
        double behind = Mth.wrapDegrees(player.getYRot() - yaw);
        boolean moving = Math.abs(speed) > 0.02 || Math.abs(side) > 0.02;
        if (Math.abs(behind) > STEP_ROUND) {
            steppingRound = true;
        } else if (Math.abs(behind) < SETTLED_DEGREES) {
            steppingRound = false;
        }
        double most = TURN_DEGREES * (1.0 - 0.4 * Mth.clamp((speed - WALK) / (RUN - WALK), 0.0, 1.0));
        double wanted = still || !moving && !steppingRound || Math.abs(behind) < SETTLED_DEGREES ? 0.0
                : Mth.clamp(behind * TURN_GAIN, -most, most);
        turn += Mth.clamp(-Math.toRadians(wanted) - turn, -TURN_UP, TURN_UP);
        yaw = Mth.wrapDegrees(yaw - (float) Math.toDegrees(turn));
        MechScript.Stage stage = MechScript.Stage.facing(base, yaw);
        ClientLevel level = player.clientLevel;
        Vec3 step = stage.ahead().scale(speed).subtract(stage.right().scale(side));
        Vec3 next = base.add(step);
        double top = base.y + STEP_UP;
        Double ground = MechGround.support(level, MechScript.Stage.facing(next, yaw), top);
        boolean against = false;
        if (step.lengthSqr() > MOVED * MOVED) {
            against = runsInto(level, next, step, top);
            boolean cramped = !clear(player, base);
            boolean drops = ground != null && ground < base.y - STEP_UP;
            if (ground == null || against || !cramped && (!clear(player, next)
                    || drops && !clear(player, new Vec3(next.x, ground, next.z)))) {
                speed = 0.0;
                side = 0.0;
                next = base;
                ground = MechGround.support(level, stage, top);
            }
        }
        pressed = ahead && against ? pressed + 1 : 0;
        if (pressed >= CLIMB_AFTER && climb(player, stage)) {
            return;
        }
        double y = base.y;
        if (ground != null && ground >= y) {
            y = Math.min(ground, y + CLIMB);
            fall = 0.0;
        } else if (ground != null) {
            fall = Math.min(fall + FALL, FASTEST_FALL);
            y = Math.max(ground, y - fall);
        }
        Vec3 was = base;
        base = new Vec3(next.x, y, next.z);
        if (base.distanceToSqr(was) > MOVED * MOVED || Math.abs(turn) > 1.0E-6) {
            send(0);
        }
    }

    // Whether a foot would run into a wall going on by step: something in its way higher than a step.
    private static boolean runsInto(ClientLevel level, Vec3 next, Vec3 step, double top) {
        MechScript.Stage there = MechScript.Stage.facing(next, yaw);
        Vec3 toe = step.normalize().scale(TOE);
        for (int s = 0; s < 2; s++) {
            if (MechGround.wall(level, there.point(MechPainter.side(MechScript.ANKLE, s == 0)).add(toe), top)) {
                return true;
            }
        }
        return false;
    }

    // Held against a ledge too high to step onto, it climbs it if its top is within reach and has room to stand on,
    // and there is room to rise straight up to it.
    private static boolean climb(LocalPlayer player, MechScript.Stage stage) {
        ClientLevel level = player.clientLevel;
        double[] ledge = MechGround.ledge(level, stage.base(), stage.ahead(), base.y + STEP_UP,
                base.y + MechClimb.HIGHEST, MechClimb.FARTHEST);
        if (ledge == null) {
            return false;
        }
        double edge = MechClimb.quantized(ledge[0]);
        double height = MechClimb.quantized(ledge[1] - base.y);
        Vec3 end = stage.point(MechClimb.path(MechClimb.ticks(height), height, edge));
        Double stand = MechGround.support(level, MechScript.Stage.facing(end, yaw), end.y + STEP_UP);
        if (stand == null || Math.abs(stand - end.y) > 0.6 || !clear(player, new Vec3(base.x, end.y, base.z))
                || !clear(player, end)) {
            return false;
        }
        climbFrom = stage;
        climbHeight = height;
        climbEdge = edge;
        climbAge = 0;
        speed = 0.0;
        side = 0.0;
        turn = 0.0;
        fall = 0.0;
        pressed = 0;
        send(MechClimb.pack(0, height, edge));
        return true;
    }

    private static void climbing() {
        climbAge++;
        int total = MechClimb.ticks(climbHeight);
        base = climbFrom.point(MechClimb.path(Math.min(climbAge, total), climbHeight, climbEdge));
        if (climbAge >= total) {
            climbFrom = null;
            send(0);
            return;
        }
        send(MechClimb.pack(climbAge, climbHeight, climbEdge));
    }

    private static void send(int climb) {
        PacketDistributor.sendToServer(new MechDrivePayload(base, yaw, climb));
    }

    // Whether the cockpit fits with the mech's base at `at`: trees and plants give way, anything else solid stops it.
    private static boolean clear(LocalPlayer player, Vec3 at) {
        Vec3 seat = MechScript.Stage.facing(at, yaw).point(MechScript.COCKPIT);
        AABB box = player.getDimensions(player.getPose()).makeBoundingBox(seat).deflate(0.05);
        ClientLevel level = player.clientLevel;
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            BlockState block = level.getBlockState(pos);
            if (MechGround.wades(block)) {
                continue;
            }
            VoxelShape shape = block.getCollisionShape(level, pos);
            if (!shape.isEmpty() && shape.move(pos.getX(), pos.getY(), pos.getZ()).bounds().intersects(box)) {
                return false;
            }
        }
        return true;
    }
}
