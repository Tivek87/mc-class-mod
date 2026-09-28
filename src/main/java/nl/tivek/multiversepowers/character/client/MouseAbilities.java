package nl.tivek.multiversepowers.character.client;

import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class MouseAbilities {
    private MouseAbilities() {
    }

    private static boolean ours(CharacterAbility.Input button, @Nullable Entity who) {
        LocalPlayer player = Minecraft.getInstance().player;
        GameCharacter now = ClientCharacter.active();
        // Only ever about your own hands: the same event also reaches the server, for everyone on it.
        return player != null && now != null && (who == null || who == player) && Gestures.takesMouse(player)
                && Gestures.bound(now, button, player);
    }

    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        CharacterAbility.Input button = event.isAttack() ? CharacterAbility.Input.LEFT
                : event.isUseItem() ? CharacterAbility.Input.RIGHT : CharacterAbility.Input.KEY;
        if (button != CharacterAbility.Input.KEY && ours(button, null)) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
        if (event.isPickBlock() && takesPickBlock()) {
            event.setCanceled(true);
        }
    }

    // An ability on the same button as pick block wins; a placeholder leaves pick block alone.
    private static boolean takesPickBlock() {
        GameCharacter now = ClientCharacter.active();
        if (now == null) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        KeyMapping pick = minecraft.options.keyPickItem;
        for (CharacterAbility ability : now.abilities()) {
            if (ability.isPlaceholder()) {
                continue;
            }
            KeyMapping key = ability.onGesture() ? AbilityKeys.of(ability) : AbilityKeys.of(ability.slot());
            if (ability.input() != CharacterAbility.Input.LEFT && ability.input() != CharacterAbility.Input.RIGHT
                    && key.getKey().equals(pick.getKey())
                    && (minecraft.player == null || Gestures.active(ability, minecraft.player))) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (ours(CharacterAbility.Input.LEFT, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ours(CharacterAbility.Input.RIGHT, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (ours(CharacterAbility.Input.RIGHT, event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
