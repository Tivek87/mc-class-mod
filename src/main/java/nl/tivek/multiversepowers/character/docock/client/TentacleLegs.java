package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class TentacleLegs {
    private static final double LIFT = 1.1;
    private static final double LIFT_PER_LEG = 0.12;
    private static final double CROUCH_LIFT = 0.55;
    private static final double CATCH = 0.28;
    private static final double MAX_CATCH = 0.55;
    private static final double MAX_DROP = 0.6;
    private static final double JUMP_PER_LEG = 0.09;

    private static boolean pushed;

    private TentacleLegs() {
    }

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
            // Without this flag the boost would stack every tick of the whole jump.
            if (!pushed) {
                pushed = true;
                player.setDeltaMovement(motion.x, motion.y + JUMP_PER_LEG * legs, motion.z);
            }
            return;
        }
        pushed = false;
        double stand = LIFT + LIFT_PER_LEG * legs;
        double target = ground + (player.isShiftKeyDown() ? CROUCH_LIFT : stand);
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

    private static double ceilingAbove(LocalPlayer player, double stand) {
        Vec3 from = player.position().add(0, player.getBbHeight(), 0);
        BlockHitResult hit = player.level().clip(new ClipContext(from, from.add(0, stand + 1.0, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? Double.NaN : hit.getLocation().y;
    }

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
