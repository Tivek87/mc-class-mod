package nl.tivek.multiversepowers.character.thor.client.motion;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

// Your own Thor flying after his thrown hammer once it comes to rest: pulled to it at great speed in a streak of
// lightning, until he is there (the server has him catch it) or it is gone.
public final class ThorPull {
    private static final double SPEED = 2.8;
    private static final double THERE = 1.4;
    private static final int LONGEST = 40;

    private static int hammer = -1;
    private static int age;

    private ThorPull() {
    }

    public static void start(int entity) {
        hammer = entity;
        age = 0;
    }

    static void stop() {
        hammer = -1;
    }

    // Moves him on towards it; false once he is not being pulled.
    static boolean pulling(LocalPlayer player) {
        if (hammer < 0) {
            return false;
        }
        Entity target = player.level().getEntity(hammer);
        if (target == null || target.isRemoved() || ++age > LONGEST) {
            stop();
            return false;
        }
        double size = player.getScale();
        Vec3 to = target.position().subtract(player.position().add(0.0, 0.9 * size, 0.0));
        double gap = to.length();
        if (gap < THERE * size) {
            player.setDeltaMovement(to.scale(0.3));
            stop();
            return false;
        }
        player.setDeltaMovement(to.scale(Math.min(SPEED, gap) / gap));
        player.resetFallDistance();
        player.level().addParticle(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + size, player.getZ(),
                0.0, 0.0, 0.0);
        return true;
    }
}
