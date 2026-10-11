package nl.tivek.multiversepowers.character.greenlantern.summon.client;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.summon.SummonPayload;
import nl.tivek.multiversepowers.engine.client.render.entity.TintedBuffers;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

// Every summon a player sees, drawn as what it is: the creature's own shape in solid hard light, one green all over
// (a dark creature's texture would leave it black), flaring white as it forms out of the ring's light.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientSummons {
    private static final int GREEN = 0x4CEB6A;
    // Ticks it takes to form.
    private static final float FORM = 16.0F;
    // When each summon came, in its own ticks on this game: entity id to the tick count it would have had at 0.
    private static final Int2IntOpenHashMap BORN = new Int2IntOpenHashMap();
    private static boolean redrawing;

    private ClientSummons() {
    }

    public static void told(SummonPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        var entity = minecraft.level.getEntity(payload.entity());
        BORN.put(payload.entity(), entity == null ? -payload.age() : entity.tickCount - payload.age());
    }

    @SubscribeEvent
    public static void onRender(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (redrawing || !BORN.containsKey(entity.getId())) {
            return;
        }
        event.setCanceled(true);
        float partialTick = event.getPartialTick();
        float form = Mth.clamp((entity.tickCount - BORN.get(entity.getId()) + partialTick) / FORM, 0.0F, 1.0F);
        int tint = Colors.mix(0xFFFFFF, GREEN, (float) Ease.smooth(form));
        redrawing = true;
        try {
            draw(event, entity, tint);
        } finally {
            redrawing = false;
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void draw(RenderLivingEvent.Pre event, LivingEntity entity, int tint) {
        float partialTick = event.getPartialTick();
        event.getRenderer().render(entity, Mth.lerp(partialTick, entity.yRotO, entity.getYRot()), partialTick,
                event.getPoseStack(), new TintedBuffers(event.getMultiBufferSource(), tint),
                event.getPackedLight());
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            BORN.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        BORN.clear();
    }
}
