package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.PowerInputs;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.flame.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.whip.WhipArms;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;

// Green Lantern's panel: in his mech its blows, its eyes, its missile arm, its rocket boots, its spin and the hold that
// leaves it; with a construct weapon in his hands its four moves and the wheel that puts it away, his other keys shut
// till then.
final class LanternPanel implements AbilityPanel.Rules {
    private static final String MOVE = "screen." + MultiversePowers.MODID + ".move.";
    private static final String MECH = "screen." + MultiversePowers.MODID + ".panel.green_lantern.mech.";
    // The keys the mech's own moves take over, and how long right click is held for its eye beam.
    private static final Set<String> MECH_KEYS = Set.of("emerald_express", "flight", "shockwave");
    private static final int MECH_GLARE_HOLD = 6;
    // Each weapon's click and hold of the left button, then of the right, and the wheel's setting saying what each
    // costs: one per second is paid every tick.
    private static final Map<String, String[]> COSTS = Map.of(
            "sword", new String[] { "swordPowerCost", "flurryPowerCost", "chargePowerCost", "blockPowerPerSecond" },
            "flamethrower", new String[] { "sweepPowerCost", "infernoPowerPerSecond", "wallPowerCost",
                    "vortexPowerPerSecond" },
            "whip", new String[] { "whipPowerCost", "whirlPowerPerSecond", "lassoPowerCost", "spinPowerPerSecond" });

    private LanternPanel() {
    }

    static void register() {
        AbilityPanel.rules(GameCharacter.GREEN_LANTERN, new LanternPanel());
        ClientCharacter.refusal(GameCharacter.GREEN_LANTERN, LanternPanel::refusal);
        CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
        if (shield != null) {
            ClientCharacter.holdTime(shield, player -> piloting(player) ? MECH_GLARE_HOLD : 0);
        }
        LanternGuide.register();
    }

    @Nullable
    static String weapon() {
        return SwordArms.holding() ? "sword" : FlameArms.holding() ? "flamethrower" : WhipArms.holding() ? "whip"
                : null;
    }

    private static boolean onMouse(CharacterAbility ability) {
        return ability.input() == CharacterAbility.Input.LEFT || ability.input() == CharacterAbility.Input.RIGHT;
    }

    static boolean piloting(LocalPlayer player) {
        return ClientConstructs.piloted(player.getId(), 0.0F) != null;
    }

    // The move the pilot's mech makes now.
    static MechAttacks.Blow mechMove(LocalPlayer player) {
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), 0.0F);
        return pilot == null ? MechAttacks.Blow.NONE : pilot.blow();
    }

    @Nullable
    private static Component refusal(CharacterAbility ability, LocalPlayer player) {
        if (ability.input() != CharacterAbility.Input.KEY || ability.isClientOnly() || weapon() == null) {
            return null;
        }
        CharacterAbility wheel = GameCharacter.GREEN_LANTERN.byName("construct_wheel");
        return Component.translatable("ring." + MultiversePowers.MODID + ".weapon_busy",
                wheel == null ? Component.literal("-") : PowerInputs.label(wheel));
    }

    @Override
    public boolean lists(CharacterAbility ability, LocalPlayer player) {
        if (piloting(player)) {
            return ability.id().equals("mech") || onMouse(ability) || MECH_KEYS.contains(ability.id());
        }
        if (weapon() != null) {
            return onMouse(ability) || ability.isClientOnly();
        }
        return !ability.id().equals("beam_lock") || ClientRing.has(player, RingPayload.BEAM);
    }

    @Nullable
    @Override
    public Component name(CharacterAbility ability, boolean hold, LocalPlayer player) {
        if (ability.id().equals("mech")) {
            return Component.translatable("screen." + MultiversePowers.MODID + ".hold.mech."
                    + (piloting(player) ? "leave" : "short"));
        }
        if (piloting(player)) {
            MechAttacks.Blow move = mechMove(player);
            boolean aiming = move.kind() == MechAttacks.AIM;
            boolean airborne = MechAttacks.airborne(move);
            String name = switch (ability.id()) {
                case "light_bolt" -> aiming ? "fire" : "blow";
                case "light_shield" -> hold ? "glare" : "eye";
                case "emerald_express" -> aiming ? "lower" : "missiles";
                case "flight" -> airborne && move.kind() == MechAttacks.FLY ? "cut" : "rockets";
                case "shockwave" -> airborne ? "dive" : "spin";
                default -> null;
            };
            return name == null ? null : Component.translatable(MECH + name);
        }
        if (ability.id().equals("flight") && ClientRing.flight(player, 0.0F) >= 0.0F) {
            return Component.translatable("screen." + MultiversePowers.MODID + ".panel.green_lantern.flight.stop");
        }
        if (!onMouse(ability)) {
            return null;
        }
        String weapon = weapon();
        if (weapon == null) {
            return hold ? ConstructHud.holdName(ability, player) : null;
        }
        String move = ability.input() == CharacterAbility.Input.LEFT ? "attack" : "defend";
        return Component.translatable(MOVE + weapon + "." + move + (hold ? "_hold" : ""));
    }

    // In the mech, holding the left button does nothing more than a click.
    @Override
    public boolean holds(CharacterAbility ability, LocalPlayer player) {
        return !(ability.input() == CharacterAbility.Input.LEFT && piloting(player));
    }

    @Nullable
    @Override
    public Component running(CharacterAbility ability, LocalPlayer player) {
        return ConstructHud.status(ability, player);
    }

    // The ring's own rules: some moves cost a setting other than powerCost, some only want the ring not empty, and
    // ending what runs (flight, the shield, a weapon, the mech) is free.
    @Override
    public boolean affords(CharacterAbility ability, boolean hold, LocalPlayer player) {
        String weapon = weapon();
        CharacterAbility wheel = GameCharacter.GREEN_LANTERN.byName("construct_wheel");
        if (weapon != null && onMouse(ability) && wheel != null) {
            String cost = COSTS.get(weapon)[(ability.input() == CharacterAbility.Input.LEFT ? 0 : 2) + (hold ? 1 : 0)];
            return pays(player, wheel.value(cost) / (cost.endsWith("PerSecond") ? 20.0 : 1.0));
        }
        CharacterAbility mech = GameCharacter.GREEN_LANTERN.byName("mech");
        if (mech != null && piloting(player)) {
            MechAttacks.Blow move = mechMove(player);
            boolean airborne = MechAttacks.airborne(move);
            return switch (ability.id()) {
                case "light_bolt" -> !hold && pays(player, mech.value(move.kind() == MechAttacks.AIM
                        ? "mechMissilePowerCost" : "mechBlowPowerCost"));
                case "light_shield" -> pays(player, mech.value(hold ? "mechGlarePowerPerSecond" : "mechEyePowerCost"));
                case "flight" -> airborne || pays(player, mech.value("mechRocketPowerCost"));
                case "shockwave" -> pays(player, mech.value(airborne ? "mechDivePowerCost" : "mechSpinPowerCost"));
                default -> true;
            };
        }
        float power = ClientRing.power(player);
        return switch (ability.id()) {
            case "construct_wheel" -> weapon != null || pays(player, ability.value("formPowerCost"));
            case "light_bolt" -> hold ? power > 0.0F : ClientCharacter.canPay(player, ability);
            case "light_shield" -> power > 0.0F || !hold && ClientRing.has(player, RingPayload.SHIELD);
            case "mech" -> piloting(player) || pays(player, ability.value("mechPowerCost"));
            case "flight" -> ClientRing.flight(player, 0.0F) >= 0.0F || ClientCharacter.canPay(player, ability);
            case "light_bubble" -> ClientConstructs.bubbleAge(player.getId(), 0.0F) >= 0.0F
                    ? pays(player, ability.value("poundPowerCost")) : ClientCharacter.canPay(player, ability);
            default -> ClientCharacter.canPay(player, ability);
        };
    }

    private static boolean pays(LocalPlayer player, double cost) {
        return ClientRing.power(player) + 1.0E-4F >= cost;
    }
}
