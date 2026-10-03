package nl.tivek.multiversepowers.character.thor.client;

import static nl.tivek.multiversepowers.character.client.GuideMode.click;
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

// Thor's modes for the ability guide: his fists, the hammer in hand and flight, every bind that does something there
// beyond the game's own walking, jumping and looking.
final class ThorGuide {
    private ThorGuide() {
    }

    static void register() {
        AbilityGuide.modes(GameCharacter.THOR,
                mode("fists", player -> !ThorPanel.armed(player) && !flies(player),
                        heading("mouse"), click(Input.LEFT, "combo").fires("combo"),
                        hold(Input.LEFT, "clap").fires("thunderclap"),
                        click(Input.RIGHT, "dash").fires("dash"), hold(Input.RIGHT, "grab").fires("grab"),
                        plus(ThorGuide::sprintKey, hold(Input.RIGHT, "grab_dash"), "grab_dash").fires("grab_dash"),
                        click(Input.SCROLL, "hammer").fires("mjolnir"), hold(Input.SCROLL, "charge").fires("charged"),
                        heading("move"), doubleKey(ThorGuide::jumpKey, "jump").fires("super_jump"),
                        holdKey(ThorGuide::jumpKey, "fly").fires("flight")),
                mode("hammer", player -> ThorPanel.armed(player) && !flies(player),
                        heading("mouse"), click(Input.LEFT, "combo").fires("combo"),
                        hold(Input.LEFT, "uppercut").fires("hammer_uppercut"),
                        click(Input.RIGHT, "throw").fires("hammer_throw"),
                        hold(Input.RIGHT, "follow").fires("hammer_leap"),
                        click(Input.SCROLL, "away").fires("mjolnir"), hold(Input.SCROLL, "charge").fires("charged"),
                        heading("move"), doubleKey(ThorGuide::jumpKey, "jump").fires("super_jump"),
                        holdKey(ThorGuide::jumpKey, "fly").fires("flight")),
                mode("flight", ThorGuide::flies,
                        heading("mouse"), click(Input.LEFT, "blows").fires("combo"),
                        hold(Input.LEFT, "shockwave").fires("air_shockwave"),
                        click(Input.RIGHT, "blink").fires("air_blink"), hold(Input.RIGHT, "dive").fires("grab_dash_dive"),
                        click(Input.SCROLL, "bolt").fires("air_bolt"),
                        hold(Input.SHIFT, "lightning").fires("lightning_flight")));
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
