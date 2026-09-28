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
// walk it on and back (the sprint key with W runs), A and D step it aside, and its legs turn after where the pilot looks. It
// climbs no more than a step it can take, never walks off a drop too deep to see the bottom of, and stops where its
// cockpit would run into blocks.
public final class MechDrive {
    public static final double WALK = 0.2;
    // The sprint key held with W runs: nearly four times as fast, getting up to it quicker.
    public static final double RUN = 0.75;
    private static final double RUN_UP = 0.03;
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
    private static final double STEP_UP = 3.2;
    private static final double CLIMB = 0.3;
    private static final double FALL = 0.06;
    private static final double FASTEST_FALL = 1.2;
    private static final double LOOK_AHEAD = 1.6;
    private static final double MOVED = 1.0E-5;

    private static int mech = -1;
    private static Vec3 base = Vec3.ZERO;
    private static float yaw;
    private static double speed;
    private static double side;
    private static double turn;
    private static double fall;
    private static boolean steppingRound;

    private MechDrive() {
    }

    @Nullable
    public static MechScript.Stage stage(int id) {
        return id == mech ? MechScript.Stage.facing(base, yaw) : null;
    }

    public static void forget() {
        mech = -1;
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
        Double ground = MechWalk.ground(level, next, base.y);
        if (step.lengthSqr() > MOVED * MOVED) {
            Double front = MechWalk.ground(level, next.add(step.normalize().scale(LOOK_AHEAD)), base.y);
            if (ground == null || ground - base.y > STEP_UP || front != null && front - base.y > STEP_UP
                    || !clear(player, next)) {
                speed = 0.0;
                side = 0.0;
                next = base;
                ground = MechWalk.ground(level, base, base.y);
            }
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
            PacketDistributor.sendToServer(new MechDrivePayload(base, yaw));
        }
    }

    // Whether the cockpit fits where the mech would go: trees and plants give way,
    // anything else solid stops it.
    private static boolean clear(LocalPlayer player, Vec3 next) {
        Vec3 seat = MechScript.Stage.facing(next, yaw).point(MechScript.COCKPIT);
        AABB box = player.getDimensions(player.getPose()).makeBoundingBox(seat).deflate(0.05);
        ClientLevel level = player.clientLevel;
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            BlockState block = level.getBlockState(pos);
            if (MechWalk.wades(block)) {
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
