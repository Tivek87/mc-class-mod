package nl.tivek.multiversepowers.engine.entity.impact;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;

// A creature falling after a blow (Knockdowns) catches hold of what it passes: the edge of a ledge, or a branch (leaves
// or a log), at the height its hands reach, with room on top for its hands and a fall below worth stopping. It hangs
// there a while, its hands on the edge and its body against the wall, then lets go and falls on; an edge it let go of
// it does not catch again. The players near are told (LedgePayload), and its body hangs from its hands there.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Ledges {
    // Its hands reach this share of its height above its head, and this far out past its side (blocks).
    private static final double HANDS_UP = 0.22;
    private static final double SIDE = 0.7;
    // Ground this near below it (blocks) makes the fall not worth stopping.
    private static final int DROP = 3;
    // Only falling at least this fast (blocks a tick), after this many ticks of flight.
    private static final double FALLING = -0.12;
    private static final int AIRBORNE = 2;
    // How often it catches hold of a ledge, and of a branch.
    private static final float LEDGE_CHANCE = 0.85F;
    private static final float BRANCH_CHANCE = 0.6F;
    // How long it hangs (ticks), from a ledge and from a branch, plus up to as long again.
    private static final int LEDGE_HANG = 35;
    private static final int BRANCH_HANG = 12;
    // Letting go, it pushes off the wall this hard (blocks a tick). Hit after hanging this long, it lets go at once.
    private static final double PUSH_OFF = 0.04;
    private static final int STEADY = 6;

    private static final Map<Mob, Hold> HOLDS = new IdentityHashMap<>();

    private static final class Hold {
        // Where it hangs (its feet), where its hands hold, the way out of the wall, and ticks left.
        Vec3 at = Vec3.ZERO;
        Vec3 edge = Vec3.ZERO;
        double nx;
        double nz;
        boolean branch;
        int left;
        int held;
        // No edge from this height up is caught again.
        double below = Double.POSITIVE_INFINITY;
    }

    private Ledges() {
    }

    // Each tick a thrown creature is still in the air (`flight` ticks since it was thrown): whether it hangs from an
    // edge now, held there; false while it falls on.
    public static boolean hangs(ServerLevel level, Mob mob, int flight) {
        Hold hold = HOLDS.get(mob);
        if (hold != null && hold.left > 0) {
            if (++hold.held > STEADY && mob.hurtTime > 0) {
                hold.left = 1;
            }
            if (--hold.left > 0) {
                mob.setDeltaMovement(Vec3.ZERO);
                mob.setPos(hold.at);
                mob.fallDistance = 0.0F;
                return true;
            }
            mob.setDeltaMovement(hold.nx * PUSH_OFF, 0.0, hold.nz * PUSH_OFF);
            mob.hurtMarked = true;
            tell(mob, hold, 0);
            return false;
        }
        if (!PowerRules.grabLedges() || flight < AIRBORNE || mob.onGround() || mob.isInWater()
                || mob.getDeltaMovement().y > FALLING) {
            return false;
        }
        AABB box = mob.getBoundingBox();
        double tall = box.getYsize();
        double hands = box.maxY + HANDS_UP * tall;
        int top = Mth.floor(hands);
        if (top < hands + mob.getDeltaMovement().y || top >= (hold == null ? Double.POSITIVE_INFINITY : hold.below)
                || !deep(level, mob)) {
            return false;
        }
        Hold mine = hold != null ? hold : new Hold();
        HOLDS.put(mob, mine);
        mine.below = top;
        Vec3 v = mob.getDeltaMovement();
        for (Direction way : order(v.x, v.z)) {
            if (grab(level, mob, mine, way, top, tall)) {
                return true;
            }
        }
        return false;
    }

    // The ways to look for an edge: back where it came from first, then to its sides, last on ahead.
    private static Direction[] order(double vx, double vz) {
        Direction ahead = Math.abs(vx) > Math.abs(vz) ? vx > 0.0 ? Direction.EAST : Direction.WEST
                : vz > 0.0 ? Direction.SOUTH : Direction.NORTH;
        return new Direction[] { ahead.getOpposite(), ahead.getClockWise(), ahead.getCounterClockWise(), ahead };
    }

    private static boolean grab(ServerLevel level, Mob mob, Hold hold, Direction way, int top, double tall) {
        AABB box = mob.getBoundingBox();
        double half = box.getXsize() * 0.5;
        BlockPos edge = BlockPos.containing(mob.getX() + way.getStepX() * (half + SIDE), top - 0.5,
                mob.getZ() + way.getStepZ() * (half + SIDE));
        BlockPos own = mob.blockPosition();
        if (edge.getX() == own.getX() && edge.getZ() == own.getZ() || !level.isLoaded(edge)) {
            return false;
        }
        BlockState state = level.getBlockState(edge);
        boolean branch = state.is(BlockTags.LEAVES) || state.is(BlockTags.LOGS);
        BlockPos above = edge.above();
        if (!branch && !state.isFaceSturdy(level, edge, Direction.UP)
                || !level.getBlockState(above).getCollisionShape(level, above).isEmpty()) {
            return false;
        }
        if (mob.getRandom().nextFloat() > (branch ? BRANCH_CHANCE : LEDGE_CHANCE)) {
            return false;
        }
        // Its body against the wall's face below the edge, its hands on the edge.
        double face = way.getStepX() > 0 || way.getStepZ() > 0 ? way.getStepX() != 0 ? edge.getX() : edge.getZ()
                : (way.getStepX() != 0 ? edge.getX() : edge.getZ()) + 1.0;
        double x = way.getStepX() != 0 ? face - way.getStepX() * (half + 0.02) : mob.getX();
        double z = way.getStepZ() != 0 ? face - way.getStepZ() * (half + 0.02) : mob.getZ();
        double y = top - HANDS_UP * tall - tall;
        if (!level.noCollision(mob, box.move(x - mob.getX(), y - mob.getY(), z - mob.getZ()))) {
            return false;
        }
        hold.at = new Vec3(x, y, z);
        hold.edge = new Vec3(way.getStepX() != 0 ? face : x, top, way.getStepZ() != 0 ? face : z);
        hold.nx = -way.getStepX();
        hold.nz = -way.getStepZ();
        hold.branch = branch;
        int least = branch ? BRANCH_HANG : LEDGE_HANG;
        hold.left = least + mob.getRandom().nextInt(least + 1);
        hold.held = 0;
        mob.setPos(hold.at);
        mob.setDeltaMovement(Vec3.ZERO);
        mob.fallDistance = 0.0F;
        mob.hurtMarked = true;
        tell(mob, hold, hold.left);
        return true;
    }

    // Whether there is a fall below it worth stopping: no ground within DROP blocks.
    private static boolean deep(ServerLevel level, Mob mob) {
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int k = 1; k <= DROP; k++) {
            at.set(mob.getX(), mob.getY() - k, mob.getZ());
            if (!level.isLoaded(at) || !level.getBlockState(at).getCollisionShape(level, at).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private static void tell(Mob mob, Hold hold, int ticks) {
        PacketDistributor.sendToPlayersTrackingEntity(mob, new LedgePayload(mob.getId(), hold.edge, (float) hold.nx,
                (float) hold.nz, ticks, hold.branch));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Mob mob && event.getEntity() instanceof ServerPlayer player) {
            Hold hold = HOLDS.get(mob);
            if (hold != null && hold.left > 0) {
                PacketDistributor.sendToPlayer(player, new LedgePayload(mob.getId(), hold.edge, (float) hold.nx,
                        (float) hold.nz, hold.left, hold.branch));
            }
        }
    }

    public static void forget(Mob mob) {
        HOLDS.remove(mob);
    }

    public static void clear() {
        HOLDS.clear();
    }
}
