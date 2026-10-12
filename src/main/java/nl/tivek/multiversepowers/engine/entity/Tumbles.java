package nl.tivek.multiversepowers.engine.entity;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

// A blow that sets a creature tumbling instead of throwing it far: a small push (blocks a tick) sends it up off its
// feet, it is knocked down where it flies, and every player near sees its limp body spin through the air about
// `spin` (radians a second) before it drops and lies. Call it after the blow's hurt(), which pushes by itself.
public final class Tumbles {
    private Tumbles() {
    }

    public static void tumble(LivingEntity target, Vec3 push, Vec3 spin) {
        if (!target.isAlive() || HeldMobs.isHeld(target)) {
            return;
        }
        target.setDeltaMovement(push);
        target.hurtMarked = true;
        Knockdowns.knock(target);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                new TumblePayload(target.getId(), (float) spin.x, (float) spin.y, (float) spin.z));
    }
}
