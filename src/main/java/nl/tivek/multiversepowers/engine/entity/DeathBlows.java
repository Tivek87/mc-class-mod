package nl.tivek.multiversepowers.engine.entity;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.effect.Effects;
import nl.tivek.multiversepowers.engine.entity.impact.Impacts;

// The blow that kills a creature, told to the players near it at the end of the tick it died in: from where it came,
// where on the body it landed (Impacts) and how hard it pushed, so their games throw the body the way the blow went,
// hardest where it struck. By the tick's end a power that pushes what it hits after the blow (as most do) has pushed
// it too.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class DeathBlows {
    private static final List<Death> DEATHS = new ArrayList<>();

    private record Death(LivingEntity victim, @Nullable Vec3 from, Vec3 push, @Nullable Vec3 at) {
    }

    static {
        Effects.atTickEnd(DeathBlows::send);
    }

    private DeathBlows() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (event.isCanceled() || victim.level().isClientSide()) {
            return;
        }
        Impacts.Impact impact = Impacts.of(victim);
        DEATHS.add(new Death(victim, from(event.getSource()), victim.getDeltaMovement(),
                impact == null ? null : impact.at()));
    }

    // A shot comes from where it hit, a blow from the eyes of who struck it, a blast from its middle; a fall, fire or
    // hunger from nowhere in particular.
    @Nullable
    private static Vec3 from(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Projectile) {
            return direct.getBoundingBox().getCenter();
        }
        if (direct != null) {
            return direct.getEyePosition();
        }
        return source.getSourcePosition();
    }

    // The push it had as it died (the game's own knockback) or has now, whichever is harder.
    private static void send() {
        if (DEATHS.isEmpty()) {
            return;
        }
        for (Death death : DEATHS) {
            LivingEntity victim = death.victim();
            Vec3 now = victim.getDeltaMovement();
            Vec3 push = now.lengthSqr() > death.push().lengthSqr() ? now : death.push();
            if (victim.isRemoved() || death.from() == null && push.lengthSqr() < 1.0E-6) {
                continue;
            }
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(victim, new DeathBlowPayload(victim.getId(),
                    death.from(), push, death.at()));
        }
        DEATHS.clear();
    }

    public static void clear() {
        DEATHS.clear();
    }
}
