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

// Thor's panel names his moves for the hammer (in hand, on his belt or thrown) and says what a move waits for.
final class ThorPanel implements AbilityPanel.Rules {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".panel.thor.";

    @Override
    public boolean lists(CharacterAbility ability, LocalPlayer player) {
        return !ability.id().equals("mjolnir") || !ClientThor.has(player, ThorStatePayload.THROWN);
    }

    @Nullable
    @Override
    public Component unavailable(CharacterAbility ability, LocalPlayer player) {
        int needs = ability.needs();
        boolean armed = armed(player);
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

    @Nullable
    @Override
    public Component name(CharacterAbility ability, boolean hold, LocalPlayer player) {
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
