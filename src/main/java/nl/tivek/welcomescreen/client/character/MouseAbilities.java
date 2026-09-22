package nl.tivek.welcomescreen.client.character;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;

/**
 * Keeps the game's own mouse out of the way of a character that always does something of its own with it
 * (see {@link CharacterAbility#mouseButton()}). While such a character has both hands empty, clicking does
 * not mine, hit, place or use anything any more: that button belongs to the character.
 *
 * <p>Pick anything up and the mouse goes back to normal at once, so you can still build, dig and eat.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class MouseAbilities {
    private MouseAbilities() {
    }

    /** True while this button belongs to the character you are, and not to the game. */
    private static boolean ours(CharacterAbility.Mouse button, @Nullable Entity who) {
        LocalPlayer player = Minecraft.getInstance().player;
        GameCharacter now = ClientCharacter.active();
        // Only ever about your own hands: the same event also reaches the server, for everyone on it.
        if (player == null || now == null || (who != null && who != player)
                || !ClientCharacter.takesMouse(player)) {
            return false;
        }
        for (CharacterAbility ability : now.abilities()) {
            if (ability.mouseButton() == button) {
                return true;
            }
        }
        return false;
    }

    /** A click of the mouse: the character answers it, so the game may not. */
    @SubscribeEvent
    public static void onClick(InputEvent.InteractionKeyMappingTriggered event) {
        CharacterAbility.Mouse button = event.isAttack() ? CharacterAbility.Mouse.LEFT
                : event.isUseItem() ? CharacterAbility.Mouse.RIGHT : CharacterAbility.Mouse.NONE;
        if (button != CharacterAbility.Mouse.NONE && ours(button, null)) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    /** Holding the left button down keeps digging by itself, whatever became of the click: stop that too. */
    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (ours(CharacterAbility.Mouse.LEFT, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (ours(CharacterAbility.Mouse.RIGHT, event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (ours(CharacterAbility.Mouse.RIGHT, event.getEntity())) {
            event.setCanceled(true);
        }
    }
}
