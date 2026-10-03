package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import static nl.tivek.multiversepowers.character.client.GuideMode.ability;
import static nl.tivek.multiversepowers.character.client.GuideMode.back;
import static nl.tivek.multiversepowers.character.client.GuideMode.click;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouch;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouched;
import static nl.tivek.multiversepowers.character.client.GuideMode.doubleKey;
import static nl.tivek.multiversepowers.character.client.GuideMode.forward;
import static nl.tivek.multiversepowers.character.client.GuideMode.hold;
import static nl.tivek.multiversepowers.character.client.GuideMode.jump;
import static nl.tivek.multiversepowers.character.client.GuideMode.sides;
import static nl.tivek.multiversepowers.character.client.GuideMode.sprint;
import static nl.tivek.multiversepowers.character.client.GuideMode.text;
import static nl.tivek.multiversepowers.character.client.GuideMode.walk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility.Input;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.GuideMode;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.flame.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordArms;

// Green Lantern's modes for the ability guide: what every key and button does in each.
final class LanternGuide {
    private static final GameCharacter GL = GameCharacter.GREEN_LANTERN;

    private LanternGuide() {
    }

    static void register() {
        AbilityGuide.modes(GL,
                mode("ring", LanternGuide::bare,
                        click(Input.LEFT, "bolt"), hold(Input.LEFT, "beam"),
                        click(Input.RIGHT, "shield"), hold(Input.RIGHT, "dome"),
                        hold(Input.SCROLL, "mech"), doubleKey(LanternGuide::jumpKey, "fly"),
                        ability(GL, "construct_wheel", "wheel"), ability(GL, "recharge", "recharge"),
                        ability(GL, "emerald_express", "train"), ability(GL, "ring_scan", "scan"),
                        ability(GL, "shockwave", "shockwave"), ability(GL, "giant_hands", "hands"),
                        ability(GL, "light_bubble", "cage"), ability(GL, "air_strike", "strike"),
                        walk("walk")),
                mode("flight", player -> flying(player) && !ClientRing.has(player, RingPayload.DESCENT),
                        forward("forward"), back("back"), sides("sides"), jump("rise"), crouch("sink"),
                        click(Input.LEFT, "bolt"), hold(Input.LEFT, "beam"),
                        click(Input.RIGHT, "ram"), hold(Input.RIGHT, "brake"),
                        doubleKey(LanternGuide::jumpKey, "stop"), ability(GL, "shockwave", "dive"),
                        ability(GL, "recharge", "recharge")),
                mode("beam", player -> ClientRing.has(player, RingPayload.BEAM),
                        hold(Input.LEFT, "grow"), ability(GL, "beam_lock", "lock"), walk("walk")),
                mode("sword", player -> SwordArms.holding(),
                        click(Input.LEFT, "cut"), hold(Input.LEFT, "flurry"),
                        click(Input.RIGHT, "charge"), hold(Input.RIGHT, "block"),
                        ability(GL, "construct_wheel", "wheel")),
                mode("flamethrower", player -> FlameArms.holding(),
                        click(Input.LEFT, "sweep"), hold(Input.LEFT, "inferno"),
                        click(Input.RIGHT, "wall"), hold(Input.RIGHT, "vortex"),
                        ability(GL, "construct_wheel", "wheel")),
                mode("wheel", player -> false,
                        text("look", "point"), text("scroll", "step"), ability(GL, "construct_wheel", "take"),
                        click(Input.LEFT, "click"), click(Input.RIGHT, "cancel")),
                mode("mech", LanternPanel::piloting,
                        forward("walk"), sprint("run"), back("back"), sides("step"), text("look", "look"),
                        forward("climb"), click(Input.LEFT, "blow"), hold(Input.SCROLL, "leave")),
                mode("cage", player -> false,
                        text("look", "aim"), ability(GL, "light_bubble", "pound"),
                        crouched(ability(GL, "light_bubble", "free"), "free")),
                mode("item", player -> !player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty(),
                        click(Input.LEFT, "attack"), click(Input.RIGHT, "use")),
                mode("dry", player -> ClientRing.has(player, RingPayload.DESCENT),
                        walk("steer"), ability(GL, "recharge", "recharge")));
    }

    // On foot with both hands empty and no construct in them: the ring's own state.
    private static boolean bare(LocalPlayer player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty() && !flying(player)
                && !LanternPanel.piloting(player) && LanternPanel.weapon() == null;
    }

    private static boolean flying(LocalPlayer player) {
        return ClientRing.flight(player, 0.0F) >= 0.0F;
    }

    private static KeyMapping jumpKey() {
        return Minecraft.getInstance().options.keyJump;
    }

    private static GuideMode mode(String id, Predicate<LocalPlayer> active,
            GuideMode.Control... controls) {
        return new GuideMode(GL, id, active, List.of(controls));
    }
}
