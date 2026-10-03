package nl.tivek.multiversepowers.character.docock.client;

import static nl.tivek.multiversepowers.character.client.GuideMode.ability;
import static nl.tivek.multiversepowers.character.client.GuideMode.abilityHold;
import static nl.tivek.multiversepowers.character.client.GuideMode.back;
import static nl.tivek.multiversepowers.character.client.GuideMode.click;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouch;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouched;
import static nl.tivek.multiversepowers.character.client.GuideMode.forward;
import static nl.tivek.multiversepowers.character.client.GuideMode.heading;
import static nl.tivek.multiversepowers.character.client.GuideMode.jump;
import static nl.tivek.multiversepowers.character.client.GuideMode.sides;
import static nl.tivek.multiversepowers.character.client.GuideMode.text;
import static nl.tivek.multiversepowers.character.client.GuideMode.walk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility.Input;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.GuideMode;

// Doctor Octopus's modes for the ability guide: on the ground (on his feet or his tentacles), climbing and holding
// creatures, every bind that does something there. His ability keys work in all three, with the tentacles still free.
final class OctoGuide {
    private static final GameCharacter OCK = GameCharacter.DOC_OCK;

    private OctoGuide() {
    }

    static void register() {
        AbilityGuide.modes(OCK,
                mode("ground", player -> !ClimbControl.climbing() && !ClientGrabState.holding(),
                        heading("mouse"), click(Input.LEFT, "hit"), click(Input.RIGHT, "use"),
                        heading("keys"), ability(OCK, "grab", "grab"), ability(OCK, "multi_tentacle", "multi"),
                        ability(OCK, "dash", "dash"), block(), ability(OCK, "ground_slam", "slam"),
                        ability(OCK, "portal", "portal"), ability(OCK, "rampage", "rampage"),
                        ability(OCK, "stance", "stance"),
                        crouched(ability(OCK, "stance", "back").moves("stance", false), "back"),
                        ability(OCK, "ground_strike", "strike"), clear(),
                        heading("move"), walk("walk"), forward("climb")),
                mode("climb", player -> ClimbControl.climbing(),
                        heading("move"), forward("up"), back("down"), sides("sides"), crouch("hold"), jump("push"),
                        heading("keys"), ability(OCK, "grab", "grab"), ability(OCK, "multi_tentacle", "multi"),
                        ability(OCK, "dash", "dash"), block(), ability(OCK, "ground_slam", "slam"),
                        ability(OCK, "portal", "portal"), ability(OCK, "rampage", "rampage"),
                        ability(OCK, "ground_strike", "strike"), clear()),
                mode("holding", player -> ClientGrabState.holding(),
                        heading("mouse"), text("look", "swing"), click(Input.LEFT, "throw"),
                        heading("keys"), ability(OCK, "grab", "more").moves("grab", false),
                        crouched(ability(OCK, "grab", "free").moves("grab", false), "free"),
                        ability(OCK, "ground_slam", "slam").moves("ground_slam", false),
                        crouched(ability(OCK, "ground_slam", "normal"), "normal"),
                        ability(OCK, "multi_tentacle", "multi"), ability(OCK, "dash", "dash"), block(),
                        ability(OCK, "portal", "portal"), ability(OCK, "rampage", "rampage"),
                        ability(OCK, "stance", "stance"), ability(OCK, "ground_strike", "strike"), clear()));
    }

    private static GuideMode.Control block() {
        return abilityHold(OCK, "block", "block");
    }

    private static GuideMode.Control clear() {
        return crouched(ability(OCK, "ground_strike", "clear").moves("ground_strike", false), "clear");
    }

    private static GuideMode mode(String id, Predicate<LocalPlayer> active, GuideMode.Control... controls) {
        return new GuideMode(OCK, id, active, List.of(controls));
    }
}
