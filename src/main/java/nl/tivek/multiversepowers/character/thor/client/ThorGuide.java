package nl.tivek.multiversepowers.character.thor.client;

import static nl.tivek.multiversepowers.character.client.GuideMode.back;
import static nl.tivek.multiversepowers.character.client.GuideMode.click;
import static nl.tivek.multiversepowers.character.client.GuideMode.doubleKey;
import static nl.tivek.multiversepowers.character.client.GuideMode.forward;
import static nl.tivek.multiversepowers.character.client.GuideMode.hold;
import static nl.tivek.multiversepowers.character.client.GuideMode.holdKey;
import static nl.tivek.multiversepowers.character.client.GuideMode.jump;
import static nl.tivek.multiversepowers.character.client.GuideMode.plus;
import static nl.tivek.multiversepowers.character.client.GuideMode.sides;
import static nl.tivek.multiversepowers.character.client.GuideMode.walk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility.Input;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.GuideMode;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;

// Thor's modes for the ability guide: fists, the hammer in hand, flight and its lightning, the super jump.
final class ThorGuide {
    private ThorGuide() {
    }

    static void register() {
        AbilityGuide.modes(GameCharacter.THOR,
                mode("fists", player -> !ThorPanel.armed(player) && !flies(player),
                        click(Input.LEFT, "combo"), hold(Input.LEFT, "clap"),
                        click(Input.RIGHT, "dash"), hold(Input.RIGHT, "grab"),
                        plus(ThorGuide::sprintKey, hold(Input.RIGHT, "grab_dash"), "grab_dash"),
                        click(Input.SCROLL, "hammer"), hold(Input.SCROLL, "charge"),
                        doubleKey(ThorGuide::jumpKey, "jump"), holdKey(ThorGuide::jumpKey, "fly"), walk("walk")),
                mode("hammer", player -> ThorPanel.armed(player) && !flies(player),
                        click(Input.LEFT, "combo"), hold(Input.LEFT, "uppercut"),
                        click(Input.RIGHT, "throw"), hold(Input.RIGHT, "follow"),
                        click(Input.SCROLL, "away"), hold(Input.SCROLL, "charge"),
                        doubleKey(ThorGuide::jumpKey, "jump"), holdKey(ThorGuide::jumpKey, "fly")),
                mode("flight", player -> flies(player) && !ClientThor.has(player, ThorStatePayload.LIGHTNING),
                        forward("forward"), back("back"), sides("sides"), jump("rise"),
                        click(Input.LEFT, "blows"), hold(Input.LEFT, "shockwave"),
                        click(Input.RIGHT, "blink"), hold(Input.RIGHT, "dive"),
                        click(Input.SCROLL, "bolt"), hold(Input.SHIFT, "lightning")),
                mode("lightning", player -> ClientThor.has(player, ThorStatePayload.LIGHTNING),
                        walk("steer"), jump("rise"), hold(Input.RIGHT, "dive")),
                mode("jump", player -> false,
                        walk("drift"), holdKey(ThorGuide::jumpKey, "fly")));
    }

    private static boolean flies(LocalPlayer player) {
        return ClientCharacter.flies(GameCharacter.THOR, player);
    }

    private static KeyMapping jumpKey() {
        return Minecraft.getInstance().options.keyJump;
    }

    private static KeyMapping sprintKey() {
        return Minecraft.getInstance().options.keySprint;
    }

    private static GuideMode mode(String id, Predicate<LocalPlayer> active, GuideMode.Control... controls) {
        return new GuideMode(GameCharacter.THOR, id, active, List.of(controls));
    }
}
