package nl.tivek.multiversepowers.character.greenlantern.client.mech;

import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechDrivePayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;

// The pilot walks their own mech in their own game, as a player walks, and tells the server where it went: W and S
// walk it on and back, A and D turn it, as its two levers do. It climbs no more than a step it can take, never
// walks off a drop too deep to see the bottom of, and stops where its cockpit would run into blocks.
public final class MechDrive {
    public static final double WALK = 0.2;
    public static final double TURN = Math.toRadians(3.0);
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
    private static double turn;
    private static double fall;

    private MechDrive() {
    }

    @Nullable
    public static MechScript.Stage stage(int id) {
        return id == mech ? MechScript.Stage.facing(base, yaw) : null;
    }

    public static void forget() {
        mech = -1;
    }

    public static void drive(LocalPlayer player, int id, MechScript.Stage server, Input input) {
        if (id != mech) {
            mech = id;
            base = server.base();
            yaw = server.yaw();
            speed = 0.0;
            turn = 0.0;
            fall = 0.0;
        }
        double want = input.forwardImpulse > 0.01F ? WALK : input.forwardImpulse < -0.01F ? -BACK : 0.0;
        speed += Mth.clamp(want - speed, -SLOW_DOWN, SPEED_UP);
        turn += Mth.clamp(input.leftImpulse * TURN - turn, -TURN_UP, TURN_UP);
        float degrees = (float) Math.toDegrees(turn);
        yaw = Mth.wrapDegrees(yaw - degrees);
        player.setYRot(player.getYRot() - degrees);
        player.setYHeadRot(player.getYHeadRot() - degrees);
        MechScript.Stage stage = MechScript.Stage.facing(base, yaw);
        ClientLevel level = player.clientLevel;
        Vec3 next = base.add(stage.ahead().scale(speed));
        Double ground = MechWalk.ground(level, next, base.y);
        if (Math.abs(speed) > MOVED) {
            Double front = MechWalk.ground(level, next.add(stage.ahead().scale(Math.signum(speed) * LOOK_AHEAD)),
                    base.y);
            if (ground == null || ground - base.y > STEP_UP || front != null && front - base.y > STEP_UP
                    || !clear(player, next)) {
                speed = 0.0;
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
        if (base.distanceToSqr(was) > MOVED * MOVED || Math.abs(degrees) > 1.0E-4F) {
            PacketDistributor.sendToServer(new MechDrivePayload(base, yaw));
        }
    }

    // Whether the cockpit fits where the mech would go: trees and plants give way, anything else solid stops it.
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
