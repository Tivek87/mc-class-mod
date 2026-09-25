package nl.tivek.multiversepowers.character.docock.client;

import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;

public final class ClimbControl {
    private static final double SPEED = 0.16;
    private static final double GAP = 0.85;
    private static final double FLAT_MARGIN = 0.2;
    private static final double PULL = 0.3;
    private static final double FLOW = 0.35;
    private static final double REACH = 2.0;
    private static final double EDGE_PROBE = 0.45;
    private static final double CORNER_STEP = 1.0;
    private static final double GRAB_UP = 1.1;
    private static final double LEDGE_UP = 0.45;
    private static final double LEDGE_FORWARD = 0.3;
    private static final double PUSH_OFF = 0.45;
    private static final float DRAIN = 0.12F;

    @Nullable
    private static Direction face;
    private static Vec3 flow = Vec3.ZERO;
    private static boolean wasJumping;

    private ClimbControl() {
    }

    public static boolean climbing() {
        return face != null;
    }

    public static Direction face() {
        return face == null ? Direction.NORTH : face;
    }

    public static void stop() {
        face = null;
        flow = Vec3.ZERO;
        wasJumping = false;
    }

    public static boolean tick(LocalPlayer player, boolean active) {
        boolean jump = player.input.jumping;
        boolean tapped = jump && !wasJumping;
        wasJumping = jump;
        if (!active || player.getAbilities().flying || player.isPassenger() || player.isInWater()
                || player.isInLava() || StaminaClient.isExhausted()) {
            face = null;
            return false;
        }
        boolean pushing = player.input.forwardImpulse > 0.1F || Math.abs(player.input.leftImpulse) > 0.1F;
        if (face == null) {
            if (pushing && player.horizontalCollision) {
                face = surfaceAhead(player);
            }
            if (face == null && tapped) {
                face = roofAbove(player);
            }
            if (face == null) {
                return false;
            }
            flow = Vec3.ZERO;
            // So the jump that just grabbed this doesn't also let go of it below.
            tapped = false;
        }
        double gap = gapTo(player, face);
        if (Double.isNaN(gap)) {
            Direction next = nextSurface(player, face);
            if (next == null || next == Direction.UP) {
                ledgeHop(player);
                face = null;
                return false;
            }
            face = next;
            gap = gapTo(player, face);
            if (Double.isNaN(gap)) {
                face = null;
                return false;
            }
        }
        if (tapped) {
            Vec3 off = Vec3.atLowerCornerOf(face.getNormal()).scale(PUSH_OFF).add(0, 0.35, 0);
            player.setDeltaMovement(off);
            face = null;
            return false;
        }
        if (stuck(player)) {
            Direction around = otherSurface(player);
            if (around == Direction.UP) {
                ledgeHop(player);
                face = null;
                return false;
            }
            if (around != null && around != face) {
                double next = gapTo(player, around);
                if (!Double.isNaN(next)) {
                    face = around;
                    gap = next;
                }
            }
        }
        move(player, gap);
        return true;
    }

    private static boolean stuck(LocalPlayer player) {
        Direction held = face;
        if (held == null || !held.getAxis().isHorizontal() || player.input.forwardImpulse <= 0.1F) {
            return false;
        }
        Vec3 head = player.position().add(0, player.getBbHeight(), 0);
        return clip(player, head, new Vec3(0, 0.55, 0)).getType() != HitResult.Type.MISS;
    }

    private static void ledgeHop(LocalPlayer player) {
        Vec3 ahead = flatLook(player).scale(LEDGE_FORWARD);
        player.setDeltaMovement(ahead.x, LEDGE_UP, ahead.z);
    }

    private static void move(LocalPlayer player, double gap) {
        Direction held = face();
        Vec3 normal = Vec3.atLowerCornerOf(held.getNormal());
        Vec3 into = normal.scale(-1);
        Vec3 along = held.getAxis().isVertical() ? flatLook(player) : new Vec3(0, 1, 0);
        Vec3 side = along.cross(into).normalize();
        boolean hold = player.input.shiftKeyDown;
        double forward = hold ? 0.0 : player.input.forwardImpulse * SPEED;
        double strafe = hold ? 0.0 : player.input.leftImpulse * SPEED;
        Vec3 motion = along.scale(forward).add(side.scale(strafe))
                .add(into.scale((gap - wantedGap(player, held)) * PULL));
        flow = flow.add(motion.subtract(flow).scale(FLOW));
        player.setDeltaMovement(flow);
        player.resetFallDistance();
        player.setOnGround(false);
        if (!hold && (Math.abs(forward) > 1.0E-4 || Math.abs(strafe) > 1.0E-4)) {
            StaminaClient.use(DRAIN);
        }
    }

    private static double wantedGap(LocalPlayer player, Direction held) {
        return held.getAxis().isHorizontal() ? GAP : player.getBbHeight() * 0.5 + FLAT_MARGIN;
    }

    private static Vec3 flatLook(LocalPlayer player) {
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        return flat.lengthSqr() < 1.0E-4 ? new Vec3(0, 0, 1) : flat.normalize();
    }

    private static Vec3 centre(LocalPlayer player) {
        return player.position().add(0, player.getBbHeight() * 0.5, 0);
    }

    @Nullable
    private static Direction surfaceAhead(LocalPlayer player) {
        Vec3 wanted = moveDirection(player);
        if (wanted.lengthSqr() < 1.0E-4) {
            return null;
        }
        BlockHitResult hit = clip(player, centre(player), wanted.normalize().scale(1.4));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getDirection();
    }

    @Nullable
    private static Direction roofAbove(LocalPlayer player) {
        Vec3 head = player.position().add(0, player.getBbHeight(), 0);
        BlockHitResult hit = clip(player, head, new Vec3(0, GRAB_UP, 0));
        return hit.getType() != HitResult.Type.MISS && hit.getDirection() == Direction.DOWN
                ? Direction.DOWN
                : null;
    }

    @Nullable
    private static Direction nextSurface(LocalPlayer player, Direction held) {
        Direction straight = otherSurface(player);
        if (straight != null && straight != Direction.UP) {
            return straight;
        }
        Direction corner = aroundCorner(player, held);
        return corner != null ? corner : straight;
    }

    @Nullable
    private static Direction aroundCorner(LocalPlayer player, Direction held) {
        Vec3 out = Vec3.atLowerCornerOf(held.getNormal());
        Vec3 depth = out.scale(-(GAP + 0.25));
        for (Vec3 offset : spread(player, held)) {
            if (offset.lengthSqr() < 1.0E-6) {
                continue;
            }
            Vec3 way = offset.normalize();
            Vec3 past = centre(player).add(way.scale(CORNER_STEP)).add(depth);
            BlockHitResult hit = clip(player, past, way.scale(-(CORNER_STEP + 0.5)));
            if (hit.getType() == HitResult.Type.MISS || hit.getDirection() == held) {
                continue;
            }
            if (Vec3.atLowerCornerOf(hit.getDirection().getNormal()).dot(way) > 0.5) {
                return hit.getDirection();
            }
        }
        return null;
    }

    private static Vec3[] spread(LocalPlayer player, Direction held) {
        Vec3 along = held.getAxis().isVertical() ? flatLook(player) : new Vec3(0, 1, 0);
        Vec3 side = along.cross(Vec3.atLowerCornerOf(held.getNormal())).normalize();
        return new Vec3[] { Vec3.ZERO, along.scale(EDGE_PROBE), along.scale(-EDGE_PROBE),
                side.scale(EDGE_PROBE), side.scale(-EDGE_PROBE) };
    }

    @Nullable
    private static Direction otherSurface(LocalPlayer player) {
        for (Direction direction : new Direction[] { Direction.UP, Direction.NORTH, Direction.EAST,
                Direction.SOUTH, Direction.WEST, Direction.DOWN }) {
            Vec3 towards = Vec3.atLowerCornerOf(direction.getNormal());
            BlockHitResult hit = clip(player, centre(player), towards.scale(REACH));
            if (hit.getType() != HitResult.Type.MISS && hit.getDirection() == direction.getOpposite()) {
                return hit.getDirection();
            }
        }
        return null;
    }

    private static double gapTo(LocalPlayer player, Direction held) {
        Vec3 from = centre(player);
        Vec3 into = Vec3.atLowerCornerOf(held.getNormal()).scale(-REACH);
        double best = Double.NaN;
        for (Vec3 offset : spread(player, held)) {
            Vec3 start = from.add(offset);
            BlockHitResult hit = clip(player, start, into);
            if (hit.getType() == HitResult.Type.MISS || hit.getDirection() != held) {
                continue;
            }
            double distance = hit.getLocation().distanceTo(start);
            if (Double.isNaN(best) || distance < best) {
                best = distance;
            }
        }
        return Double.isNaN(best) ? Double.NaN : Mth.clamp(best, 0.0, REACH);
    }

    private static BlockHitResult clip(LocalPlayer player, Vec3 from, Vec3 offset) {
        return player.level().clip(new ClipContext(from, from.add(offset), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
    }

    private static Vec3 moveDirection(LocalPlayer player) {
        Vec3 look = flatLook(player);
        Vec3 right = new Vec3(-look.z, 0, look.x);
        return look.scale(player.input.forwardImpulse).subtract(right.scale(player.input.leftImpulse));
    }
}
