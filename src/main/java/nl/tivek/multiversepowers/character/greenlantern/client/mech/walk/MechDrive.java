package nl.tivek.multiversepowers.character.greenlantern.client.mech.walk;

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
import nl.tivek.multiversepowers.character.greenlantern.client.mech.MechPainter;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechDrivePayload;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechScript;

// The pilot walks their own mech in their own game, as a player walks, and tells the server where it went: W and S
// walk it on and back (the sprint key with W runs), A and D step it aside, and its legs turn after where the pilot looks.
// It stands on whatever its feet find, so a hole narrower than its stance never drops it in, but its middle never hangs
// out over an edge (MechGround.support). It steps up no higher than its legs can, never walks off a drop too deep to
// see the bottom of or down into a hole its cockpit would not fit in, and stops where its cockpit or its chest would
// run into blocks (unless it already stands among them, so it can always get out). Walked against a ledge too high to
// step onto for half a second, its pilot looking at it, it climbs it (MechClimb); pushing against it, its legs still
// turn after the look.
public final class MechDrive {
    public static final double WALK = 0.2;
    // The sprint key held with W runs: almost three and a half times as fast, working up to it slowly.
    public static final double RUN = 0.675;
    private static final double RUN_UP = 0.018;
    // Run on and the exhaust charges up over BOOST_TICKS: its flames grow huge and it runs up to BOOST faster again.
    // Easing off, or anything that stops it, loses the charge quickly.
    public static final double BOOST = 0.6;
    private static final double BOOST_TICKS = 100.0;
    private static final double BOOST_LOSS = 0.04;
    private static final double BOOST_FROM = 0.9;
    // Standing still, the legs step round only when the look is this far (degrees)
    // off where they face: the torso twists that far over them first.
    private static final double STEP_ROUND = 90.0;
    private static final double TURN_DEGREES = 5.5;
    public static final double TURN = Math.toRadians(TURN_DEGREES);
    // How much of the way still to turn the legs take on each tick, and how near
    // they count as there.
    private static final double TURN_GAIN = 0.2;
    private static final double SETTLED_DEGREES = 1.5;
    private static final double SIDESTEP = 0.1;
    private static final double BACK = 0.11;
    private static final double SPEED_UP = 0.012;
    private static final double SLOW_DOWN = 0.024;
    private static final double STRIKE_BRAKE = 0.1;
    private static final double TURN_UP = Math.toRadians(0.8);
    // The highest step its legs take; a ledge higher than that it climbs.
    static final double STEP_UP = 3.4;
    private static final double CLIMB = 0.3;
    private static final double FALL = 0.06;
    private static final double FASTEST_FALL = 1.2;
    private static final double MOVED = 1.0E-5;
    // How far ahead of an ankle its foot meets a wall, and how many ticks (half a second) it must be walked against a
    // ledge, its pilot looking at it (no further off than FACING degrees), before it climbs it.
    private static final double TOE = MechGround.SOLE_AHEAD + 0.6;
    private static final int CLIMB_AFTER = 10;
    private static final double FACING = 45.0;
    // Where the front of its chest meets a wall: across, up and ahead of its base.
    private static final double[] CHEST_ACROSS = { -1.3, 0.0, 1.3 };
    private static final double[] CHEST_UP = { 8.6, 9.6, 10.6 };
    private static final double CHEST_AHEAD = MechClimb.CHEST + 0.15;
    // The climb's ledge is looked for along the middle and along lanes this far to either side.
    private static final double LANE = 1.1;
    // The top may stand this much lower or higher where the climb ends than at its edge: the walk takes it from there.
    private static final double END_BELOW = 1.5;
    private static final double END_ABOVE = 2.0;
    // Along a climb the cockpit is tried this many times for room.
    private static final int CLIMB_TRIES = 12;
    // On its rocket boots: it leaps up LAUNCH a tick as they ignite, then flies FLY on (FLY_FAST with the sprint key),
    // FLY_SIDE aside and FLY_BACK back, climbing RISE with the jump key held and sinking SINK with the sneak key, its
    // legs turning after the look; out of thrust it falls. Diving it hangs, wound up, then plunges DIVE_DROP down and
    // DIVE_AHEAD on.
    private static final double LAUNCH = 1.1;
    private static final int LAUNCH_TICKS = 6;
    public static final double FLY = 0.75;
    private static final double FLY_FAST = 1.15;
    public static final double FLY_SIDE = 0.4;
    private static final double FLY_BACK = 0.3;
    private static final double FLY_SPEED_UP = 0.05;
    private static final double FLY_BRAKE = 0.06;
    private static final double RISE = 0.6;
    private static final double SINK = 0.45;
    private static final double RISE_UP = 0.08;
    private static final double FLY_TURN = 7.0;
    private static final double FLY_TURN_GAIN = 0.25;
    private static final double DIVE_WIND = 0.12;
    private static final double DIVE_DROP = 2.4;
    private static final double DIVE_AHEAD = 0.6;
    private static final double DIVE_SPEED_UP = 0.6;
    // A jump leaps up JUMP_UP a tick and falls back as anything falls, keeping the speed it had.
    private static final double JUMP_UP = 0.78;
    // Where its body would run into blocks in flight (its feet, knees, hips, chest, shoulders and head).
    private static final Vec3[] BODY = { new Vec3(1.5, 0.5, 0.2), new Vec3(-1.5, 0.5, 0.2), new Vec3(1.6, 4.0, 0.4),
            new Vec3(-1.6, 4.0, 0.4), new Vec3(0.0, 7.1, 0.0), new Vec3(0.0, 9.4, 1.5), new Vec3(2.6, 9.8, 0.0),
            new Vec3(-2.6, 9.8, 0.0), new Vec3(0.0, 12.6, 0.6) };

    private static int mech = -1;
    private static Vec3 base = Vec3.ZERO;
    private static float yaw;
    private static double speed;
    private static double side;
    private static double turn;
    private static double fall;
    private static double rise;
    private static double charge;
    private static boolean steppingRound;
    private static int pressed;
    private static boolean blocked;
    private static boolean jumpHeld;
    private static boolean leapt;
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

    // `blow`: the move it makes. Striking it stands its ground, its legs turning no more (MechAttacks.plants); aiming
    // or firing its eyes it walks but never runs; on its rocket boots or jumping it flies (fly). Pressing jump with no
    // move under way tells the server, which jumps it.
    public static void drive(LocalPlayer player, int id, MechScript.Stage server, Input input, MechAttacks.Blow blow) {
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
            rise = 0.0;
            charge = 0.0;
            steppingRound = false;
            pressed = 0;
            blocked = false;
            jumpHeld = true;
            climbFrom = null;
        }
        boolean jump = input.jumping && !jumpHeld;
        jumpHeld = input.jumping;
        if (climbFrom != null) {
            climbing();
            return;
        }
        if (MechAttacks.airborne(blow)) {
            fly(player, input, blow);
            return;
        }
        rise = 0.0;
        leapt = false;
        boolean still = MechAttacks.plants(blow);
        boolean ahead = !still && input.forwardImpulse > 0.01F;
        boolean run = ahead && !blow.striking() && Minecraft.getInstance().options.keySprint.isDown();
        charge = run && speed > RUN * BOOST_FROM ? Math.min(1.0, charge + 1.0 / BOOST_TICKS)
                : Math.max(0.0, charge - BOOST_LOSS);
        double want = ahead ? run ? RUN * (1.0 + BOOST * charge) : WALK
                : !still && input.forwardImpulse < -0.01F ? -BACK : 0.0;
        // Striking a blow it plants its feet: it stops within a few strides, not slowly, or its feet stay behind.
        double down = still ? STRIKE_BRAKE : SLOW_DOWN;
        speed += Mth.clamp(want - speed, -down, still ? STRIKE_BRAKE : run ? RUN_UP : SPEED_UP);
        side += Mth.clamp((still ? 0.0 : input.leftImpulse * SIDESTEP) - side, -down,
                still ? STRIKE_BRAKE : SPEED_UP);
        // The legs turn after where the pilot looks, no faster than they can, while the
        // torso swings there first.
        // Standing, they only step round once it has twisted far over them, then all
        // the way; walking, they keep up.
        double behind = Mth.wrapDegrees(player.getYRot() - yaw);
        // Walked against a wall it stands still, but its legs keep turning after the look, so it can face what it
        // climbs.
        boolean moving = Math.abs(speed) > 0.02 || Math.abs(side) > 0.02 || ahead && blocked;
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
        blocked = against;
        pressed = ahead && against && Math.abs(behind) < FACING ? pressed + 1 : 0;
        // Held on against what it cannot climb, it looks again now and then, as it turns.
        if (pressed >= CLIMB_AFTER && (pressed - CLIMB_AFTER) % 4 == 0 && climb(player, stage)) {
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
        jump &= blow.kind() == MechAttacks.NONE && fall == 0.0;
        if (jump || base.distanceToSqr(was) > MOVED * MOVED || Math.abs(turn) > 1.0E-6) {
            send(0, jump);
        }
    }

    // Whether it would run into a wall going on by step: something in a foot's way higher than a step, or in the way of
    // its chest while its chest is out of the blocks now.
    private static boolean runsInto(ClientLevel level, Vec3 next, Vec3 step, double top) {
        MechScript.Stage there = MechScript.Stage.facing(next, yaw);
        Vec3 toe = step.normalize().scale(TOE);
        for (int s = 0; s < 2; s++) {
            if (MechGround.wall(level, there.point(MechPainter.side(MechScript.ANKLE, s == 0)).add(toe), top)) {
                return true;
            }
        }
        return chestIn(level, there) && !chestIn(level, MechScript.Stage.facing(base, yaw));
    }

    // Whether the front of the chest stands in a block with the mech at `stage`.
    private static boolean chestIn(ClientLevel level, MechScript.Stage stage) {
        for (int i = 0; i < CHEST_ACROSS.length; i++) {
            double ahead = i == 1 ? CHEST_AHEAD : CHEST_AHEAD - MechClimb.CHEST_ROUND;
            for (double up : CHEST_UP) {
                Vec3 at = stage.point(CHEST_ACROSS[i], up, ahead);
                if (MechGround.solid(level, at.x, at.y, at.z)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Held against a ledge too high to step onto, it climbs it if its top is within reach, along the middle or a lane
    // to either side, with room to stand on where it ends (a top a little higher or lower the walk takes on from
    // there), and its cockpit has room all along the way up (or stands among blocks already, so it can get out).
    private static boolean climb(LocalPlayer player, MechScript.Stage stage) {
        ClientLevel level = player.clientLevel;
        double[] ledge = null;
        for (int k = 0; k < 3 && ledge == null; k++) {
            Vec3 from = stage.point(k == 0 ? 0.0 : k == 1 ? LANE : -LANE, 0.0, 0.0);
            ledge = MechGround.ledge(level, from, stage.ahead(), base.y + STEP_UP, base.y + MechClimb.HIGHEST,
                    MechClimb.FARTHEST);
        }
        if (ledge == null) {
            return false;
        }
        double edge = MechClimb.quantizedEdge(ledge[0]);
        double height = MechClimb.quantizedHeight(ledge[1] - base.y);
        int ticks = MechClimb.ticks(height);
        Vec3 end = stage.point(MechClimb.path(ticks, height, edge));
        Double stand = MechGround.support(level, MechScript.Stage.facing(end, yaw), end.y + END_ABOVE);
        if (stand == null || stand < end.y - END_BELOW || !clear(player, end)) {
            return false;
        }
        if (clear(player, base)) {
            for (int k = 1; k < CLIMB_TRIES; k++) {
                if (!clear(player, stage.point(MechClimb.path(ticks * k / (double) CLIMB_TRIES, height, edge)))) {
                    return false;
                }
            }
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
        send(climb, false);
    }

    private static void send(int climb, boolean jump) {
        PacketDistributor.sendToServer(new MechDrivePayload(base, yaw, climb, jump));
    }

    // On its rocket boots (MechAttacks.FLY): crouched, it leaps up as they ignite; while they thrust it flies where its
    // pilot steers it, climbing with the jump key held, sinking with the sneak key, else hovering; out of thrust it
    // falls. Diving (MechAttacks.DIVE) it hangs a moment, wound up, then plunges ahead and down. Jumping
    // (MechAttacks.JUMP) it leaps up once and falls, flying on as fast as it went. It stops short of blocks its body
    // would run into and comes down on the ground under it.
    private static void fly(LocalPlayer player, Input input, MechAttacks.Blow blow) {
        ClientLevel level = player.clientLevel;
        double age = blow.age();
        boolean diving = blow.kind() == MechAttacks.DIVE;
        boolean plunge = diving && age >= MechAttacks.DIVE_PLUNGE;
        boolean thrust = MechAttacks.thrusting(blow);
        double behind = Mth.wrapDegrees(player.getYRot() - yaw);
        turn = 0.0;
        yaw = Mth.wrapDegrees(yaw + (float) Mth.clamp(behind * FLY_TURN_GAIN, -FLY_TURN, FLY_TURN));
        MechScript.Stage stage = MechScript.Stage.facing(base, yaw);
        boolean jumping = blow.kind() == MechAttacks.JUMP;
        double want = 0.0;
        double wantSide = 0.0;
        if (plunge) {
            want = DIVE_AHEAD;
        } else if (jumping) {
            want = speed;
            wantSide = side;
        } else if (!diving) {
            boolean fast = Minecraft.getInstance().options.keySprint.isDown();
            want = input.forwardImpulse > 0.01F ? fast ? FLY_FAST : FLY : input.forwardImpulse < -0.01F ? -FLY_BACK
                    : 0.0;
            wantSide = input.leftImpulse * FLY_SIDE;
        }
        speed += Mth.clamp(want - speed, -FLY_BRAKE, plunge ? DIVE_SPEED_UP : FLY_SPEED_UP);
        side += Mth.clamp(wantSide - side, -FLY_BRAKE, FLY_SPEED_UP);
        if (plunge) {
            rise = Math.max(-DIVE_DROP, rise - DIVE_SPEED_UP);
        } else if (diving) {
            rise += Mth.clamp(DIVE_WIND - rise, -RISE_UP, RISE_UP);
        } else if (blow.kind() == MechAttacks.FLY && age < MechAttacks.FLY_LAUNCH) {
            rise = 0.0;
        } else if (jumping) {
            rise = leapt ? Math.max(-FASTEST_FALL, rise - FALL) : JUMP_UP;
            leapt = true;
        } else if (thrust && age < MechAttacks.FLY_LAUNCH + LAUNCH_TICKS) {
            rise = LAUNCH;
        } else if (thrust) {
            double wantRise = input.jumping ? RISE : input.shiftKeyDown ? -SINK : 0.0;
            rise += Mth.clamp(wantRise - rise, -RISE_UP, RISE_UP);
        } else {
            rise = Math.max(-FASTEST_FALL, rise - FALL);
        }
        Vec3 step = stage.ahead().scale(speed).subtract(stage.right().scale(side));
        Vec3 next = base.add(step.x, 0.0, step.z);
        boolean stuck = bodyIn(level, base);
        if (step.lengthSqr() > MOVED * MOVED && !stuck && bodyIn(level, next)) {
            speed = 0.0;
            side = 0.0;
            next = base;
        }
        double y = next.y + rise;
        if (rise > 0.0 && !stuck && bodyIn(level, new Vec3(next.x, y, next.z))) {
            rise = 0.0;
            y = next.y;
        }
        Double ground = MechGround.support(level, MechScript.Stage.facing(next, yaw), base.y + 0.5);
        if (ground != null && y <= ground) {
            y = ground;
            rise = 0.0;
        }
        fall = 0.0;
        base = new Vec3(next.x, y, next.z);
        send(0);
    }

    // Whether its body, standing at `at`, is in blocks anywhere.
    private static boolean bodyIn(ClientLevel level, Vec3 at) {
        MechScript.Stage there = MechScript.Stage.facing(at, yaw);
        for (Vec3 spot : BODY) {
            Vec3 point = there.point(spot);
            if (MechGround.solid(level, point.x, point.y, point.z)) {
                return true;
            }
        }
        return false;
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
