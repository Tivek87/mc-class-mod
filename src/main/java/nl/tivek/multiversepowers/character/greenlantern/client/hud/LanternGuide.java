package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import static nl.tivek.multiversepowers.character.client.GuideMode.ability;
import static nl.tivek.multiversepowers.character.client.GuideMode.abilityHold;
import static nl.tivek.multiversepowers.character.client.GuideMode.click;
import static nl.tivek.multiversepowers.character.client.GuideMode.crouched;
import static nl.tivek.multiversepowers.character.client.GuideMode.doubleKey;
import static nl.tivek.multiversepowers.character.client.GuideMode.forward;
import static nl.tivek.multiversepowers.character.client.GuideMode.heading;
import static nl.tivek.multiversepowers.character.client.GuideMode.hold;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.CharacterAbility.Input;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.GuideMode;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.flame.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.whip.WhipArms;
import nl.tivek.multiversepowers.character.greenlantern.construct.Construct;

// Green Lantern's modes for the ability guide: on the ground, in flight, in the mech and with each construct weapon,
// every bind that does something there beyond the game's own walking, jumping and looking.
final class LanternGuide {
    private static final GameCharacter GL = GameCharacter.GREEN_LANTERN;

    private LanternGuide() {
    }

    static void register() {
        List<GuideMode> modes = new ArrayList<>(List.of(
                mode("ground", player -> !flying(player) && !LanternPanel.piloting(player)
                        && LanternPanel.weapon() == null,
                        heading("mouse"), bolt(), beam(), lock(),
                        click(Input.RIGHT, "shield").fires("light_shield").costs("light_shield", "powerPerSecond"),
                        hold(Input.RIGHT, "dome").holds("light_shield").costs("light_shield", "domePowerPerSecond"),
                        hold(Input.SCROLL, "mech").fires("mech").costs("mech", "mechPowerCost"),
                        heading("keys"), wheel("wheel"), ability(GL, "recharge", "recharge"),
                        ability(GL, "emerald_express", "train"), ability(GL, "ring_scan", "scan"),
                        ability(GL, "shockwave", "shockwave"), ability(GL, "giant_hands", "hands"), revolver(),
                        cage(), pound(), free(), ability(GL, "air_strike", "strike"),
                        heading("move"), doubleKey(LanternGuide::jumpKey, "fly").fires("flight")
                                .costs("flight", "fullRingSeconds")),
                mode("flight", LanternGuide::flying,
                        heading("mouse"), bolt(), beam(), lock(),
                        click(Input.RIGHT, "ram").fires("light_shield").costs("light_shield", "powerPerSecond"),
                        hold(Input.RIGHT, "brake").holds("light_shield").costs("light_shield", "domePowerPerSecond"),
                        hold(Input.SCROLL, "mech").fires("mech").costs("mech", "mechPowerCost"),
                        heading("keys"), ability(GL, "shockwave", "dive"), ability(GL, "recharge", "recharge"),
                        wheel("wheel"), ability(GL, "emerald_express", "train"), ability(GL, "ring_scan", "scan"),
                        ability(GL, "giant_hands", "hands"), revolver(), cage(), pound(), free(),
                        ability(GL, "air_strike", "strike"),
                        heading("move"), doubleKey(LanternGuide::jumpKey, "stop").moves("flight", false)),
                mode("mech", LanternPanel::piloting,
                        heading("mouse"), click(Input.LEFT, "blow").moves("light_bolt", false)
                                .costs("mech", "mechBlowPowerCost"),
                        click(Input.RIGHT, "eye").moves("light_shield", false).costs("mech", "mechEyePowerCost"),
                        hold(Input.RIGHT, "glare").moves("light_shield", true)
                                .costs("mech", "mechGlarePowerPerSecond"),
                        hold(Input.SCROLL, "leave").fires("mech"),
                        heading("keys"), ability(GL, "emerald_express", "missiles").moves("emerald_express", false),
                        click(Input.LEFT, "salvo").moves("light_bolt", false).costs("mech", "mechMissilePowerCost")
                                .under("missiles"),
                        ability(GL, "emerald_express", "lower").moves("emerald_express", false).again("missiles"),
                        ability(GL, "shockwave", "spin").moves("shockwave", false).costs("mech", "mechSpinPowerCost"),
                        heading("move"), forward("climb"),
                        doubleKey(LanternGuide::jumpKey, "rockets").moves("flight", false)
                                .costs("mech", "mechRocketPowerCost"),
                        ability(GL, "shockwave", "dive").moves("shockwave", false).costs("mech", "mechDivePowerCost")
                                .under("rockets"),
                        doubleKey(LanternGuide::jumpKey, "cut").moves("flight", false).again("rockets")),
                weapon("sword", player -> SwordArms.holding(), "swordPowerCost", "flurryPowerCost", "chargePowerCost",
                        "blockPowerPerSecond", "cut", "flurry", "charge", "block"),
                weapon("flamethrower", player -> FlameArms.holding(), "sweepPowerCost", "infernoPowerPerSecond",
                        "wallPowerCost", "vortexPowerPerSecond", "sweep", "inferno", "wall", "vortex")));
        if (!Construct.ENERGY_WHIP.locked()) {
            modes.add(weapon("whip", player -> WhipArms.holding(), "whipPowerCost", "whirlPowerPerSecond",
                    "lassoPowerCost", "spinPowerPerSecond", "lash", "whirl", "lasso", "spin"));
        }
        AbilityGuide.modes(GL, modes.toArray(GuideMode[]::new));
    }

    private static GuideMode.Control bolt() {
        return click(Input.LEFT, "bolt").fires("light_bolt");
    }

    private static GuideMode.Control beam() {
        return hold(Input.LEFT, "beam").holds("light_bolt").costs("light_bolt", "beamPowerPerSecond");
    }

    private static GuideMode.Control lock() {
        return click(Input.SCROLL, "lock").fires("beam_lock");
    }

    private static GuideMode.Control wheel(String id) {
        return ability(GL, "construct_wheel", id).costs("construct_wheel", "formPowerCost");
    }

    private static GuideMode.Control revolver() {
        return abilityHold(GL, "giant_hands", "revolver").costs("giant_hands", "revolverPowerCost");
    }

    private static GuideMode.Control cage() {
        return ability(GL, "light_bubble", "cage");
    }

    private static GuideMode.Control pound() {
        return ability(GL, "light_bubble", "pound").moves("light_bubble", false)
                .costs("light_bubble", "poundPowerCost").again("cage");
    }

    private static GuideMode.Control free() {
        return crouched(ability(GL, "light_bubble", "free").moves("light_bubble", false), "free").under("cage");
    }

    // A construct weapon: its click and hold of each mouse button, each with the wheel's setting saying what it costs,
    // and the wheel key that puts it away (free, so it shows no cost).
    private static GuideMode weapon(String id, Predicate<LocalPlayer> active, String cut, String hold, String right,
            String rightHold, String... moves) {
        return mode(id, active,
                heading("mouse"),
                click(Input.LEFT, moves[0]).moves("light_bolt", false).costs("construct_wheel", cut),
                hold(Input.LEFT, moves[1]).moves("light_bolt", true).costs("construct_wheel", hold),
                click(Input.RIGHT, moves[2]).moves("light_shield", false).costs("construct_wheel", right),
                hold(Input.RIGHT, moves[3]).moves("light_shield", true).costs("construct_wheel", rightHold),
                heading("keys"), ability(GL, "construct_wheel", "wheel").moves("construct_wheel", false));
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
