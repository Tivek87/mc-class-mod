package nl.tivek.welcomescreen.client.character.docock;

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
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.docock.OctopusArms;

/**
 * Keeps the view still while the tentacles change how fast you walk. Minecraft widens the view as
 * soon as a player's walking speed changes, the way a speed potion does. Block slows you down, so
 * without this the whole screen would stretch and shrink around you.
 *
 * <p>Here the view is worked out again as if the tentacles were not there: the speed itself still
 * changes, only the camera no longer reacts to it.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
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
        // How much of the current speed comes from the tentacles (1.0 = nothing of it).
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
        // Minecraft's own sum: (speed / walking speed + 1) / 2. Take out the tentacles' part of it.
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
