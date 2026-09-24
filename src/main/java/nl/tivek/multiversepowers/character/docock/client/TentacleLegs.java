package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Standing in the air on Doctor Octopus's tentacles, on this player's own client. The legs hold you at
 * standing height above the ground: they step over everything up to a block high, they bend under a low
 * ceiling, and they catch every landing, so you never take fall damage while they carry you.
 *
 * <p>Only the player's own client does this, because only a client may move its own player.
 */
public final class TentacleLegs {
    // How high the tentacles carry you, crouched down, and how hard they pull you back to that height.
    private static final double LIFT = 1.1;
    private static final double LIFT_PER_LEG = 0.12;
    private static final double CROUCH_LIFT = 0.55;
    private static final double CATCH = 0.28;
    private static final double MAX_CATCH = 0.55;
    private static final double MAX_DROP = 0.6;
    // How much higher every tentacle under you throws you when you jump off them.
    private static final double JUMP_PER_LEG = 0.09;

    // Whether the jump you are in was already given its push, so it only gets it once.
    private static boolean pushed;

    private TentacleLegs() {
    }

    /** The legs hold you up: a spring towards standing height, which also softens every landing. */
    public static void carry(LocalPlayer player, int legs) {
        if (legs <= 0 || player.getAbilities().flying || player.isPassenger() || player.isInWater()
                || player.isInLava() || player.isFallFlying() || player.isSleeping()) {
            return;
        }
        double ground = groundBelow(player, LIFT + LIFT_PER_LEG * legs + 3.0);
        if (Double.isNaN(ground)) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        if (motion.y > 0.1) {
            // Jumping, dashing or thrown: let it fly, the legs catch you on the way down. A jump is
            // really made by the tentacles, so they push off properly: the more of them carry you,
            // the higher you go.
            if (!pushed) {
                pushed = true;
                player.setDeltaMovement(motion.x, motion.y + JUMP_PER_LEG * legs, motion.z);
            }
            return;
        }
        pushed = false;
        double stand = LIFT + LIFT_PER_LEG * legs;
        double target = ground + (player.isShiftKeyDown() ? CROUCH_LIFT : stand);
        // Under a low ceiling the legs bend: never push the player's head into the blocks above.
        double ceiling = ceilingAbove(player, stand);
        if (!Double.isNaN(ceiling)) {
            target = Math.min(target, ceiling - player.getBbHeight() - 0.1);
        }
        if (target < ground) {
            target = ground;
        }
        double lift = Mth.clamp((target - player.getY()) * CATCH, -MAX_DROP, MAX_CATCH);
        player.setDeltaMovement(motion.x, lift, motion.z);
        player.setOnGround(true);
        player.resetFallDistance();
    }

    /** The blocks right above the player, or NaN when the sky is free for the next few blocks. */
    private static double ceilingAbove(LocalPlayer player, double stand) {
        Vec3 from = player.position().add(0, player.getBbHeight(), 0);
        BlockHitResult hit = player.level().clip(new ClipContext(from, from.add(0, stand + 1.0, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? Double.NaN : hit.getLocation().y;
    }

    /** The highest ground under the player's feet within {@code maxDrop}, or NaN when there is none. */
    private static double groundBelow(LocalPlayer player, double maxDrop) {
        AABB box = player.getBoundingBox();
        double inset = 0.15;
        double best = Double.NaN;
        double[][] spots = {
                { box.minX + inset, box.minZ + inset }, { box.maxX - inset, box.minZ + inset },
                { box.minX + inset, box.maxZ - inset }, { box.maxX - inset, box.maxZ - inset },
                { (box.minX + box.maxX) / 2, (box.minZ + box.maxZ) / 2 } };
        for (double[] spot : spots) {
            Vec3 from = new Vec3(spot[0], player.getY() + 0.1, spot[1]);
            BlockHitResult hit = player.level().clip(new ClipContext(from, from.subtract(0, maxDrop, 0),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() != HitResult.Type.MISS && (Double.isNaN(best) || hit.getLocation().y > best)) {
                best = hit.getLocation().y;
            }
        }
        return best;
    }
}
