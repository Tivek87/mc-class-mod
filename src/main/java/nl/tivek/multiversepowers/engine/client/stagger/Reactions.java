package nl.tivek.multiversepowers.engine.client.stagger;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.entity.impact.ImpactPayload;

// The creatures near reeling from a blow they lived through (the server tells how each took it, Staggers): each one's
// body answers the blow (Reaction) and is drawn so (StaggerPose), a pose layer of the creature stage. One that goes
// limp, is held or posed by a power is left to that.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Reactions {
    private static final Int2ObjectOpenHashMap<Reaction> ALL = new Int2ObjectOpenHashMap<>();

    private Reactions() {
    }

    public static void told(ImpactPayload payload) {
        ImpactPayload.Reaction kind = payload.reaction();
        if (kind != ImpactPayload.Reaction.FLINCH && kind != ImpactPayload.Reaction.STAGGER) {
            ALL.remove(payload.entity());
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        Entity entity = minecraft.level == null ? null : minecraft.level.getEntity(payload.entity());
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            return;
        }
        Reaction had = ALL.get(payload.entity());
        // A flinch does not cut short a stagger under way.
        if (had != null && kind == ImpactPayload.Reaction.FLINCH && had.kind == ImpactPayload.Reaction.STAGGER
                && had.age < had.ticks) {
            return;
        }
        ALL.put(payload.entity(), new Reaction(living, payload, had != null && had.entity == living ? had : null));
    }

    // The creature stage's layer: poses a creature reeling now.
    public static boolean pose(EntityModel<?> model, LivingEntity entity, float partialTick) {
        Reaction reaction = ALL.get(entity.getId());
        if (reaction == null || reaction.entity != entity || !EntityPass.inWorld() || entity.isPassenger()
                || entity.isSleeping() || Ragdolls.taken(entity)) {
            return false;
        }
        return StaggerPose.pose(model, reaction, partialTick);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            ALL.clear();
            return;
        }
        if (minecraft.isPaused() || ALL.isEmpty()) {
            return;
        }
        ALL.values().removeIf(reaction -> !reaction.tick());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            ALL.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ALL.clear();
    }
}
