package nl.tivek.multiversepowers.character.docock.client;

import static nl.tivek.multiversepowers.character.client.GuideMode.ability;
import static nl.tivek.multiversepowers.character.client.GuideMode.click;
import static nl.tivek.multiversepowers.character.client.GuideMode.heading;
import static nl.tivek.multiversepowers.character.client.GuideMode.key;
import static nl.tivek.multiversepowers.character.client.GuideMode.plus;
import static nl.tivek.multiversepowers.character.client.GuideMode.text;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility.Input;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.GuideMode;
import nl.tivek.multiversepowers.character.client.PowerInputs;

// Doctor Octopus's modes for the ability guide: his tentacles as the rules pick them, and Selection Mode, where only
// the picked ones act and the number keys and scroll wheel pick them.
final class OctoGuide {
    private OctoGuide() {
    }

    static void register() {
        AbilityGuide.modes(GameCharacter.DOC_OCK,
                mode("default", player -> !OctoControls.selecting(),
                        heading("mouse"), own("grab"), own("squeeze"), text("flick", "slam").moves("grab", false),
                        plus(OctoGuide::shift, click(Input.RIGHT, "interact"), "interact"), own("slap"),
                        heading("keys"), own("release"), own("stance"), own("throw"), own("selection")),
                mode("selection", player -> OctoControls.selecting(),
                        heading("mouse"), own("grab"), own("squeeze"), text("flick", "slam").moves("grab", false),
                        ability(GameCharacter.DOC_OCK, "slap", "strike"), text("scroll", "cycle"),
                        heading("keys"), key(OctoGuide::one, "pick").moves("selection", false),
                        key(OctoGuide::five, "all").moves("selection", false), own("release"), own("stance"),
                        own("throw"), own("selection")));
    }

    private static GuideMode.Control own(String ability) {
        return ability(GameCharacter.DOC_OCK, ability, ability);
    }

    private static KeyMapping shift() {
        return PowerInputs.HOLD_SHIFT;
    }

    private static KeyMapping one() {
        return Minecraft.getInstance().options.keyHotbarSlots[0];
    }

    private static KeyMapping five() {
        return Minecraft.getInstance().options.keyHotbarSlots[4];
    }

    private static GuideMode mode(String id, Predicate<LocalPlayer> active, GuideMode.Control... controls) {
        return new GuideMode(GameCharacter.DOC_OCK, id, active, List.of(controls));
    }
}
