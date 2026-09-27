package nl.tivek.multiversepowers.engine.client.ragdoll;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.TintedBuffers;

// A creature a power burns to ash: it goes black as it dies, sinks into a heap and blows away in ash and smoke. It
// never goes limp and leaves no body.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Ashes {
    private static final int CHAR = 0x2B2724;
    // Ticks it takes to fall in on itself, and how much of its height is left then.
    private static final float CRUMBLE = 18.0F;
    private static final float HEAP = 0.12F;
    private static final int KEEP = 60;
    private static final Int2IntOpenHashMap BURNING = new Int2IntOpenHashMap();
    private static final RandomSource RANDOM = RandomSource.create();
    private static boolean redrawing;
    private static int ticks;

    private Ashes() {
    }

    public static void burn(int entity) {
        BURNING.put(entity, ticks);
    }

    static boolean burning(int entity) {
        return BURNING.containsKey(entity);
    }

    // Instead of the creature, its charred shape, pressed down as it crumbles.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (redrawing || !entity.isDeadOrDying() || !BURNING.containsKey(entity.getId())) {
            return;
        }
        event.setCanceled(true);
        float gone = Mth.clamp((entity.deathTime + event.getPartialTick()) / CRUMBLE, 0.0F, 1.0F);
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        float wide = 1.0F + 0.35F * gone;
        pose.scale(wide, Mth.lerp(gone * gone, 1.0F, HEAP), wide);
        int deathTime = entity.deathTime;
        entity.deathTime = 0;
        redrawing = true;
        try {
            redraw(event, entity);
        } finally {
            redrawing = false;
            entity.deathTime = deathTime;
            pose.popPose();
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void redraw(RenderLivingEvent.Pre event, LivingEntity entity) {
        float partialTick = event.getPartialTick();
        event.getRenderer().render(entity, Mth.lerp(partialTick, entity.yRotO, entity.getYRot()), partialTick,
                event.getPoseStack(), new TintedBuffers(event.getMultiBufferSource(), CHAR), event.getPackedLight());
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || BURNING.isEmpty()) {
            return;
        }
        ticks++;
        BURNING.int2IntEntrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getIntKey());
            if (entity == null || ticks - entry.getIntValue() > KEEP) {
                return true;
            }
            if (entity instanceof LivingEntity living && living.isDeadOrDying()) {
                smoulder(level, living);
            }
            return false;
        });
    }

    private static void smoulder(ClientLevel level, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        float gone = Mth.clamp(entity.deathTime / CRUMBLE, 0.0F, 1.0F);
        double high = box.getYsize() * Mth.lerp(gone, 1.0F, HEAP);
        for (int i = 0; i < 6; i++) {
            double x = box.minX + RANDOM.nextDouble() * box.getXsize();
            double y = box.minY + RANDOM.nextDouble() * high;
            double z = box.minZ + RANDOM.nextDouble() * box.getZsize();
            level.addParticle(i % 3 == 0 ? ParticleTypes.LARGE_SMOKE : i % 3 == 1 ? ParticleTypes.ASH
                    : ParticleTypes.WHITE_ASH, x, y, z, (RANDOM.nextDouble() - 0.5) * 0.05, 0.02 + gone * 0.04,
                    (RANDOM.nextDouble() - 0.5) * 0.05);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BURNING.clear();
    }
}
