package nl.tivek.multiversepowers.character.thor.client;

import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;

// Thor's panel names his moves for the hammer: in hand, on his belt or thrown.
final class ThorPanel implements AbilityPanel.Rules {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".panel.thor.";

    @Override
    public boolean lists(CharacterAbility ability, LocalPlayer player) {
        return !ability.id().equals("mjolnir") || !ClientThor.has(player, ThorStatePayload.THROWN);
    }

    @Nullable
    @Override
    public Component name(CharacterAbility ability, boolean hold, LocalPlayer player) {
        if (!ClientThor.has(player, ThorStatePayload.ARMED) || ClientThor.has(player, ThorStatePayload.THROWN)) {
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
}
