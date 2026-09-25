package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.docock.OctopusArms;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class TentacleFov {
    private TentacleFov() {
    }

    @SubscribeEvent
    public static void onComputeFov(ComputeFovModifierEvent event) {
        Player player = event.getPlayer();
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        float walking = player.getAbilities().getWalkingSpeed();
        if (speed == null || walking <= 0.0F) {
            return;
        }
        // A multiplier, not a fraction: 1.0 means none of the speed is theirs.
        double fromTentacles = 1.0;
        for (ResourceLocation id : new ResourceLocation[] { OctopusArms.BLOCKING_ID }) {
            AttributeModifier modifier = speed.getModifier(id);
            if (modifier != null && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                fromTentacles *= 1.0 + modifier.amount();
            }
        }
        if (fromTentacles == 1.0) {
            return;
        }
        double real = speed.getValue();
        double own = real / fromTentacles;
        // Mirrors vanilla's own FOV formula, to undo just the tentacles' part of it.
        float with = (float) (real / walking + 1.0) / 2.0F;
        float without = (float) (own / walking + 1.0) / 2.0F;
        if (with <= 0.0F || !Float.isFinite(with) || !Float.isFinite(without)) {
            return;
        }
        float fov = event.getFovModifier() * (without / with);
        event.setNewFovModifier(
                Mth.lerp(Minecraft.getInstance().options.fovEffectScale().get().floatValue(), 1.0F, fov));
    }
}
