package nl.tivek.multiversepowers.character.greenlantern.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientFlight.Motion;
import static nl.tivek.multiversepowers.character.greenlantern.client.ClientFlight.fast;
import static nl.tivek.multiversepowers.character.greenlantern.client.FlightSteering.BRACE_TICKS;
import static nl.tivek.multiversepowers.character.greenlantern.client.FlightSteering.SLAM_DOWN;
import static nl.tivek.multiversepowers.character.greenlantern.client.FlightSteering.SLAM_SPEED;
import static nl.tivek.multiversepowers.character.greenlantern.client.FlightSteering.fullSpeed;
import static nl.tivek.multiversepowers.character.greenlantern.client.FlightSteering.velocity;

final class FlyerTracker {
    private static final int TRAIL = 12;
    private static final double SKIM = 3.0;

    private FlyerTracker() {
    }

    static void track(ClientLevel level, AbstractClientPlayer player, Motion motion, boolean flying,
            boolean dropping, boolean own) {
        // Your own real flight velocity; other players send no velocity, so it is guessed from position deltas.
        Vec3 moved = own ? velocity : new Vec3(player.getX() - player.xo, player.getY() - player.yo,
                player.getZ() - player.zo);
        motion.velocityO = motion.velocity;
        motion.velocity = motion.velocity.lerp(moved, own ? 0.6 : 0.35);
        float yaw = player.getYRot();
        float turn = Float.isNaN(motion.lastYaw) ? 0.0F : Mth.wrapDegrees(yaw - motion.lastYaw);
        motion.lastYaw = yaw;
        double speed = motion.velocity.length();
        float lean = flying ? Mth.clamp(turn * 0.045F * (float) Math.min(1.0, speed / fast()), -0.75F, 0.75F)
                : 0.0F;
        motion.bank = Mth.lerp(0.18F, motion.bank, lean);
        if (flying) {
            motion.flew = true;
        }
        if (flying || dropping) {
            motion.sinceEnd = Integer.MAX_VALUE;
        } else if (motion.sinceEnd == Integer.MAX_VALUE) {
            motion.sinceEnd = 0;
        } else {
            motion.sinceEnd++;
        }
        Vec3 middle = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
        motion.trail.addFirst(middle);
        while (motion.trail.size() > TRAIL) {
            motion.trail.removeLast();
        }
        motion.ground = groundBelow(level, player);
        if (flying && speed > fast() * 0.6) {
            skim(level, player, motion, speed);
        }
        motion.braceO = motion.brace;
        float want = flying && diving(level, player, motion.velocity) || dropping ? 1.0F : 0.0F;
        motion.brace = Mth.lerp(want > motion.brace ? 0.5F : 0.3F, motion.brace, want);
    }

    private static boolean diving(ClientLevel level, Entity player, Vec3 velocity) {
        double speed = velocity.length();
        if (speed < fullSpeed() * SLAM_SPEED || -velocity.y < speed * SLAM_DOWN
                || ClientRing.has(player, RingPayload.DESCENT)) {
            return false;
        }
        Vec3 from = player.position();
        BlockHitResult hit = level.clip(new ClipContext(from, from.add(velocity.scale(BRACE_TICKS)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() != HitResult.Type.MISS;
    }

    private static double groundBelow(ClientLevel level, Entity player) {
        Vec3 feet = player.position();
        BlockHitResult hit = level.clip(new ClipContext(feet, feet.add(0.0, -SKIM - 1.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, player));
        return hit.getType() == HitResult.Type.MISS ? SKIM + 1.0 : feet.y - hit.getLocation().y;
    }

    private static void skim(ClientLevel level, Entity player, Motion motion, double speed) {
        if (motion.ground > SKIM) {
            return;
        }
        BlockPos below = BlockPos.containing(player.getX(), player.getY() - motion.ground - 0.2, player.getZ());
        BlockState state = level.getBlockState(below);
        double strength = (1.0 - motion.ground / SKIM) * Math.min(1.0, speed / (fullSpeed() * 1.15));
        int count = (int) (1 + strength * 5);
        double y = below.getY() + 1.05;
        for (int i = 0; i < count; i++) {
            double x = player.getX() + (level.random.nextDouble() - 0.5) * 1.6;
            double z = player.getZ() + (level.random.nextDouble() - 0.5) * 1.6;
            double out = 0.15 + 0.2 * strength;
            if (!state.getFluidState().isEmpty()) {
                level.addParticle(ParticleTypes.SPLASH, x, y, z, (level.random.nextDouble() - 0.5) * out, 0.25,
                        (level.random.nextDouble() - 0.5) * out);
                level.addParticle(ParticleTypes.BUBBLE_POP, x, y, z, 0.0, 0.05, 0.0);
            } else if (state.getRenderShape() != RenderShape.INVISIBLE && !state.isAir()) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), x, y, z,
                        -motion.velocity.x * 0.3, 0.15 + 0.2 * strength, -motion.velocity.z * 0.3);
            }
        }
    }
}
