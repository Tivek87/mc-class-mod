package nl.tivek.multiversepowers.character.docock.client;

import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.ClientCharacter;

// Doctor Octopus's panel says where his stance key takes him next.
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class OctoPanel {
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".panel.doc_ock.stance.";
    private static final int MOST_LEGS = 4;

    public OctoPanel() {
        OctoGuide.register();
        AbilityPanel.rules(GameCharacter.DOC_OCK, new AbilityPanel.Rules() {
            @Override
            public boolean lists(CharacterAbility ability, LocalPlayer player) {
                return true;
            }

            @Nullable
            @Override
            public Component name(CharacterAbility ability, boolean hold, LocalPlayer player) {
                if (!ability.id().equals("stance")) {
                    return null;
                }
                int legs = ClientCharacter.legs();
                return Component.translatable(PREFIX + (legs == 0 ? "legs" : legs >= MOST_LEGS ? "feet" : "more"));
            }
        });
    }
}
