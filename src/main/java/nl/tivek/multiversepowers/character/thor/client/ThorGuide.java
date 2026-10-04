package nl.tivek.multiversepowers.character.thor.client;

import static nl.tivek.multiversepowers.character.client.GuideMode.ability;
import static nl.tivek.multiversepowers.character.client.GuideMode.click;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouched;
import static nl.tivek.multiversepowers.character.client.GuideMode.doubleKey;
import static nl.tivek.multiversepowers.character.client.GuideMode.heading;
import static nl.tivek.multiversepowers.character.client.GuideMode.hold;
import static nl.tivek.multiversepowers.character.client.GuideMode.holdKey;
import static nl.tivek.multiversepowers.character.client.GuideMode.plus;

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
import nl.tivek.multiversepowers.character.thor.client.motion.ThorMotion;

// Thor's modes for the ability guide: his fists, the hammer in hand, the hammer thrown and flight, every bind that does
// something there beyond the game's own walking, jumping and looking.
final class ThorGuide {
    private ThorGuide() {
    }

    static void register() {
        AbilityGuide.modes(GameCharacter.THOR,
                mode("fists", player -> !ThorPanel.armed(player) && !ThorMotion.away(player) && !flies(player)
                        && !ThorGrabChoice.holds(player),
                        heading("mouse"), click(Input.LEFT, "combo").fires("combo"),
                        hold(Input.LEFT, "clap").fires("thunderclap"),
                        click(Input.RIGHT, "dash").fires("dash"), hold(Input.RIGHT, "grab").fires("grab"),
                        plus(ThorGuide::sprintKey, hold(Input.RIGHT, "grab_dash"), "grab_dash").fires("grab_dash"),
                        click(Input.SCROLL, "hammer").fires("mjolnir"), hold(Input.SCROLL, "charge").fires("charged"),
                        heading("move"), doubleKey(ThorGuide::jumpKey, "jump").fires("super_jump"),
                        holdKey(ThorGuide::jumpKey, "fly").fires("flight"),
                        heading("keys"), storm(), call(), calm(), bomb()),
                mode("hammer", player -> ThorPanel.armed(player) && !flies(player),
                        heading("mouse"), click(Input.LEFT, "combo").fires("combo"),
                        hold(Input.LEFT, "uppercut").fires("hammer_uppercut"),
                        click(Input.RIGHT, "throw").fires("hammer_throw"),
                        crouched(click(Input.RIGHT, "stay"), "stay").fires("hammer_throw").under("throw"),
                        hold(Input.RIGHT, "follow").fires("hammer_leap"),
                        click(Input.SCROLL, "away").fires("mjolnir"), hold(Input.SCROLL, "charge").fires("charged"),
                        heading("move"), doubleKey(ThorGuide::jumpKey, "jump").fires("super_jump"),
                        holdKey(ThorGuide::jumpKey, "fly").fires("flight"),
                        heading("keys"), storm(), call(), calm(), bomb()),
                mode("thrown", player -> ThorMotion.away(player) && !flies(player) && !ThorGrabChoice.holds(player),
                        heading("mouse"), click(Input.LEFT, "combo").fires("combo"),
                        hold(Input.LEFT, "clap").fires("thunderclap"),
                        click(Input.RIGHT, "dash").fires("dash"), hold(Input.RIGHT, "grab").fires("grab"),
                        plus(ThorGuide::sprintKey, hold(Input.RIGHT, "grab_dash"), "grab_dash").fires("grab_dash"),
                        click(Input.SCROLL, "recall").fires("hammer_call"),
                        hold(Input.SCROLL, "follow").fires("hammer_follow"),
                        heading("move"), doubleKey(ThorGuide::jumpKey, "jump").fires("super_jump"),
                        holdKey(ThorGuide::jumpKey, "fly").fires("flight"),
                        heading("keys"), storm(), call(), calm(), bomb()),
                mode("held", ThorGrabChoice::holds,
                        heading("mouse"), click(Input.LEFT, "punches").moves("grab", false),
                        click(Input.RIGHT, "throw").moves("grab", false),
                        click(Input.SCROLL, "slam").moves("grab", false)),
                mode("flight", ThorGuide::flies,
                        heading("mouse"), click(Input.LEFT, "blows").fires("combo"),
                        hold(Input.LEFT, "shockwave").fires("air_shockwave"),
                        click(Input.RIGHT, "blink").fires("air_blink"), hold(Input.RIGHT, "dive").fires("grab_dash_dive"),
                        click(Input.SCROLL, "bolt").fires("air_bolt"),
                        hold(Input.SCROLL, "storm_throw").fires("storm_throw"),
                        hold(Input.SHIFT, "lightning").fires("lightning_flight"),
                        heading("keys"), storm(), call(), calm()));
    }

    private static GuideMode.Control storm() {
        return ability(GameCharacter.THOR, "storm", "storm");
    }

    private static GuideMode.Control call() {
        return ability(GameCharacter.THOR, "storm", "call").moves("storm", false).again("storm");
    }

    private static GuideMode.Control calm() {
        return crouched(ability(GameCharacter.THOR, "storm", "calm").moves("storm", false), "calm").under("storm");
    }

    private static GuideMode.Control bomb() {
        return ability(GameCharacter.THOR, "lightning_bomb", "bomb");
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
