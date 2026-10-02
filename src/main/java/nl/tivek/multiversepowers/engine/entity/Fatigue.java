package nl.tivek.multiversepowers.engine.entity;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.PowerRules;
import nl.tivek.multiversepowers.engine.effect.Effects;

// Blows one after another wear a creature down: every hit from someone or something counts one, and a creature left
// alone REST ticks gets its breath back, a count every RECOVER ticks. The players near see how worn it is (it sags more
// with every count); the HITS-th count running drops it where it stands as if thrown (Knockdowns), when the world lets
// it, and it gets up rested. While down, blows do not count.
@EventBusSubscriber(modid = MultiversePowers.MODID)
public final class Fatigue {
    public static final int HITS = 5;
    private static final int REST = 50;
    private static final int RECOVER = 30;

    private static final Map<Mob, Worn> WORN = new IdentityHashMap<>();

    private static final class Worn {
        int hits;
        int calm;
    }

    private Fatigue() {
    }

    @SubscribeEvent
    public static void onHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Mob mob) || !(mob.level() instanceof ServerLevel level) || !mob.isAlive()
                || !Knockdowns.falls(mob) || Knockdowns.isDown(mob) || !blow(event.getSource())) {
            return;
        }
        Worn worn = WORN.get(mob);
        if (worn == null) {
            Worn fresh = new Worn();
            WORN.put(mob, fresh);
            Effects.start(level, (lvl, age) -> rest(mob, fresh));
            worn = fresh;
        }
        worn.calm = 0;
        worn.hits = Math.min(HITS, worn.hits + 1);
        if (worn.hits == HITS && PowerRules.fatigueKnockdown() && Knockdowns.mayFly(mob)) {
            worn.hits = 0;
            Knockdowns.drop(mob);
        }
        tell(mob, worn.hits);
    }

    // A hit by someone or something, or a blast; not fire, a fall or poison wearing on it.
    private static boolean blow(DamageSource source) {
        return source.getEntity() != null || source.getDirectEntity() != null
                || source.is(DamageTypeTags.IS_EXPLOSION);
    }

    // Every tick while it is worn: left alone long enough it gets its breath back; false once it is rested or gone.
    private static boolean rest(Mob mob, Worn worn) {
        if (WORN.get(mob) != worn) {
            return false;
        }
        if (!mob.isAlive() || mob.isRemoved()) {
            WORN.remove(mob);
            return false;
        }
        if (++worn.calm >= REST && (worn.calm - REST) % RECOVER == 0 && worn.hits > 0) {
            tell(mob, --worn.hits);
        }
        if (worn.hits <= 0) {
            WORN.remove(mob);
            return false;
        }
        return true;
    }

    private static void tell(Mob mob, int hits) {
        PacketDistributor.sendToPlayersTrackingEntity(mob, new FatiguePayload(mob.getId(), hits));
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof Mob mob && event.getEntity() instanceof ServerPlayer player) {
            Worn worn = WORN.get(mob);
            if (worn != null && worn.hits > 0) {
                PacketDistributor.sendToPlayer(player, new FatiguePayload(mob.getId(), worn.hits));
            }
        }
    }

    public static void clear() {
        WORN.clear();
    }
}
