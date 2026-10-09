package nl.tivek.multiversepowers.character.thor.client;

import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorPowers;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.hammer.ThrownHammer;

// Thor's panel names his moves for the hammer (in hand, on his belt or thrown) and his storm, and says what a move
// waits for.
final class ThorPanel implements AbilityPanel.Rules {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".panel.thor.";

    @Override
    public boolean lists(CharacterAbility ability, LocalPlayer player) {
        return true;
    }

    @Nullable
    @Override
    public Component unavailable(CharacterAbility ability, LocalPlayer player) {
        int needs = ability.needs();
        boolean armed = armed(player);
        boolean away = ClientThor.has(player, ThorStatePayload.THROWN);
        if ((needs & ThorPowers.HOME) != 0 && away) {
            return Component.translatable(PREFIX + "hammer_thrown");
        }
        if ((needs & ThorPowers.AWAY) != 0 && !away) {
            return Component.translatable(PREFIX + "hammer_on_you");
        }
        if (ability.id().equals("hammer_follow") && tooFar(ability, player)) {
            return Component.translatable(PREFIX + "too_far");
        }
        if ((needs & ThorPowers.ARMED) != 0 && !armed) {
            return Component.translatable(PREFIX + "needs_hammer");
        }
        if ((needs & ThorPowers.UNARMED) != 0 && armed) {
            return Component.translatable(PREFIX + "hammer_away");
        }
        if ((needs & ThorPowers.SPRINTING) != 0 && !player.isSprinting()) {
            return Component.translatable(PREFIX + "running");
        }
        if ((needs & ThorPowers.WALKING) != 0 && player.isSprinting()) {
            return Component.translatable(PREFIX + "walking");
        }
        return null;
    }

    // His thrown hammer further off than he may dash to it, measured from his feet as the server does.
    private static boolean tooFar(CharacterAbility follow, LocalPlayer player) {
        ThrownHammer hammer = ClientThor.hammer(player);
        return hammer != null && player.position().distanceTo(hammer.position()) > follow.value("reachBlocks");
    }

    @Nullable
    @Override
    public Component name(CharacterAbility ability, boolean hold, LocalPlayer player) {
        if (ability.id().equals("storm") && ClientThor.has(player, ThorStatePayload.STORMING)) {
            return Component.translatable(PREFIX + "call_lightning");
        }
        if (!armed(player)) {
            return null;
        }
        return switch (ability.id()) {
            case "mjolnir" -> Component.translatable(PREFIX + "put_away");
            case "charged" -> Component.translatable(PREFIX + "charge_hammer");
            case "combo" -> ClientCharacter.flies(GameCharacter.THOR, player) ? null
                    : Component.translatable(PREFIX + "swings");
            default -> null;
        };
    }

    static boolean armed(LocalPlayer player) {
        return ClientThor.has(player, ThorStatePayload.ARMED) && !ClientThor.has(player, ThorStatePayload.THROWN);
    }
}
