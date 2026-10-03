package nl.tivek.multiversepowers.character.greenlantern.client.hud;

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

// Green Lantern's panel: in his mech only the hold that leaves it; with a construct weapon in his hands its four moves
// and the wheel that puts it away, his other keys shut till then.
final class LanternPanel implements AbilityPanel.Rules {
    private static final String MOVE = "screen." + MultiversePowers.MODID + ".move.";

    private LanternPanel() {
    }

    static void register() {
        AbilityPanel.rules(GameCharacter.GREEN_LANTERN, new LanternPanel());
        ClientCharacter.refusal(GameCharacter.GREEN_LANTERN, LanternPanel::refusal);
    }

    @Nullable
    private static String weapon() {
        return SwordArms.holding() ? "sword" : FlameArms.holding() ? "flamethrower" : WhipArms.holding() ? "whip"
                : null;
    }

    private static boolean onMouse(CharacterAbility ability) {
        return ability.input() == CharacterAbility.Input.LEFT || ability.input() == CharacterAbility.Input.RIGHT;
    }

    private static boolean piloting(LocalPlayer player) {
        return ClientConstructs.piloted(player.getId(), 0.0F) != null;
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
            return ability.id().equals("mech");
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

    @Nullable
    @Override
    public Component running(CharacterAbility ability, LocalPlayer player) {
        return ConstructHud.status(ability, player);
    }
}
