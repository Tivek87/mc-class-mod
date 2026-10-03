package nl.tivek.multiversepowers.character.docock.client;

import static nl.tivek.multiversepowers.character.client.GuideMode.ability;
import static nl.tivek.multiversepowers.character.client.GuideMode.back;
import static nl.tivek.multiversepowers.character.client.GuideMode.click;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouch;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouched;
import static nl.tivek.multiversepowers.character.client.GuideMode.forward;
import static nl.tivek.multiversepowers.character.client.GuideMode.jump;
import static nl.tivek.multiversepowers.character.client.GuideMode.sides;
import static nl.tivek.multiversepowers.character.client.GuideMode.sprint;
import static nl.tivek.multiversepowers.character.client.GuideMode.text;
import static nl.tivek.multiversepowers.character.client.GuideMode.walk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterAbility.Input;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.GuideMode;

// Doctor Octopus's modes for the ability guide: his feet or his tentacles, holding, climbing, blocking, marks, rampage.
final class OctoGuide {
    private static final GameCharacter OCK = GameCharacter.DOC_OCK;

    private OctoGuide() {
    }

    static void register() {
        AbilityGuide.modes(OCK,
                mode("feet", player -> ClientCharacter.legs() == 0 && !ClimbControl.climbing(),
                        click(Input.LEFT, "hit"), click(Input.RIGHT, "use"),
                        ability(OCK, "stance", "stance"), crouched(ability(OCK, "stance", "back"), "back"),
                        forward("climb")),
                mode("legs", player -> ClientCharacter.legs() > 0 && !ClimbControl.climbing(),
                        walk("walk"), sprint("run"), jump("jump"), crouch("crouch"),
                        ability(OCK, "stance", "stance")),
                mode("holding", player -> ClientGrabState.holding(),
                        text("look", "swing"), click(Input.LEFT, "throw"), ability(OCK, "grab", "more"),
                        crouched(ability(OCK, "grab", "free"), "free"), ability(OCK, "ground_slam", "slam"),
                        crouched(ability(OCK, "ground_slam", "normal"), "normal")),
                mode("climb", player -> ClimbControl.climbing(),
                        forward("up"), back("down"), sides("sides"), crouch("hold"), jump("push")),
                mode("block", OctoGuide::blocking,
                        ability(OCK, "block", "hold"), walk("walk")),
                mode("marks", player -> ClientCharacter.marked() > 0,
                        ability(OCK, "ground_strike", "mark"), ability(OCK, "ground_strike", "launch"),
                        crouched(ability(OCK, "ground_strike", "clear"), "clear")),
                mode("rampage", player -> ClientCharacter.ultimate() > 0,
                        click(Input.LEFT, "hit")));
    }

    private static boolean blocking(LocalPlayer player) {
        CharacterAbility block = OCK.byName("block");
        return block != null && ClientCharacter.isHeld(block.slot());
    }

    private static GuideMode mode(String id, Predicate<LocalPlayer> active, GuideMode.Control... controls) {
        return new GuideMode(OCK, id, active, List.of(controls));
    }
}
