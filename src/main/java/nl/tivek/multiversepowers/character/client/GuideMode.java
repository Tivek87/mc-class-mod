package nl.tivek.multiversepowers.character.client;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;

// A state of a character with binds of its own (on the ground, in flight, in the mech, a weapon in hand), for the
// guide: how it starts and ends and every bind that does something in it, nothing else (the game's own walking, jumping
// and looking are left out). Its words live in en_us.json under `guide.<mod>.<character>.<id>.` (`title`, `when`,
// `off`, and per control its short line and, unless the ability it fires names it, `<control>.name`); a heading's
// under `screen.<mod>.guide.part.<id>`.
public record GuideMode(GameCharacter character, String id, Predicate<LocalPlayer> active, List<Control> controls) {
    private static final String INPUT = "input." + MultiversePowers.MODID + ".";

    // `key` is drawn on a key cap and follows the player's own key settings; a heading has none. `ability` is what
    // the control fires, for its status (`hold`: its hold version); `describes` when that ability's own explanation
    // and cost are about this very move; `cost` names the setting it costs when that is not the ability's own.
    public record Control(@Nullable Supplier<Component> key, String id, @Nullable String ability, boolean hold,
            boolean describes, @Nullable Cost cost) {
        public boolean heading() {
            return this.key == null;
        }

        public Control fires(String ability) {
            return new Control(this.key, this.id, ability, false, true, this.cost);
        }

        public Control holds(String ability) {
            return new Control(this.key, this.id, ability, true, true, this.cost);
        }

        // Another move on that ability's button (a weapon's): its status, not its explanation.
        public Control moves(String ability, boolean hold) {
            return new Control(this.key, this.id, ability, hold, false, this.cost);
        }

        public Control costs(String ability, String setting) {
            return new Control(this.key, this.id, this.ability, this.hold, this.describes, new Cost(ability, setting));
        }
    }

    public record Cost(String ability, String setting) {
    }

    public Component title() {
        return Component.translatable(this.path() + "title");
    }

    @Nullable
    public CharacterAbility ability(Control control) {
        return control.ability() == null ? null : this.character.byName(control.ability());
    }

    // The short name the guide's list gives a control: its own, else the ability it fires (or that one's hold).
    public Component name(Control control) {
        if (control.heading()) {
            return Component.translatable("screen." + MultiversePowers.MODID + ".guide.part." + control.id());
        }
        String own = this.path() + control.id() + ".name";
        if (Language.getInstance().has(own)) {
            return Component.translatable(own);
        }
        CharacterAbility ability = this.ability(control);
        if (ability == null) {
            return Component.literal(control.id());
        }
        String held = "screen." + MultiversePowers.MODID + ".hold." + ability.id();
        return control.hold() && Language.getInstance().has(held) ? Component.translatable(held)
                : ability.getDisplayName();
    }

    public Component when() {
        return Component.translatable(this.path() + "when");
    }

    @Nullable
    public Component off() {
        String key = this.path() + "off";
        return Language.getInstance().has(key) ? Component.translatable(key) : null;
    }

    // What the control does in this mode, or null when the ability's own explanation says it all.
    @Nullable
    public Component does(Control control) {
        String key = this.path() + control.id();
        return Language.getInstance().has(key) ? Component.translatable(key) : null;
    }

    private String path() {
        return "guide." + MultiversePowers.MODID + "." + this.character.getId() + "." + this.id + ".";
    }

    private static Control control(Supplier<Component> key, String id) {
        return new Control(key, id, null, false, false, null);
    }

    // A small title over the controls after it, such as the mouse or the ability keys.
    public static Control heading(String id) {
        return new Control(null, id, null, false, false, null);
    }

    public static Control click(CharacterAbility.Input input, String id) {
        return control(() -> PowerInputs.keyName(PowerInputs.clickKey(input)), id);
    }

    public static Control hold(CharacterAbility.Input input, String id) {
        return control(() -> PowerInputs.holdLabel(input), id);
    }

    public static Control key(Supplier<KeyMapping> key, String id) {
        return control(() -> PowerInputs.keyName(key.get()), id);
    }

    public static Control holdKey(Supplier<KeyMapping> key, String id) {
        return control(() -> Component.translatable(INPUT + "hold", PowerInputs.keyName(key.get())), id);
    }

    public static Control doubleKey(Supplier<KeyMapping> key, String id) {
        return control(() -> Component.translatable(INPUT + "double", PowerInputs.keyName(key.get())), id);
    }

    // The key or gesture an ability sits on, as the panel shows it, firing that ability.
    public static Control ability(GameCharacter character, String ability, String id) {
        return control(() -> {
            CharacterAbility found = character.byName(ability);
            return found == null ? Component.literal("-") : PowerInputs.label(found);
        }, id).fires(ability);
    }

    // Holding the key of an ability that has a hold version of its own.
    public static Control abilityHold(GameCharacter character, String ability, String id) {
        return control(() -> {
            CharacterAbility found = character.byName(ability);
            return Component.translatable(INPUT + "hold",
                    found == null ? Component.literal("-") : PowerInputs.label(found));
        }, id).holds(ability);
    }

    // A key held down while another is pressed: "Shift + K".
    public static Control plus(Supplier<KeyMapping> held, Control control, String id) {
        Supplier<Component> key = control.key();
        return new Control(() -> Component.translatable(INPUT + "plus", PowerInputs.keyName(held.get()),
                key == null ? Component.empty() : key.get()), id, control.ability(), control.hold(),
                control.describes(), control.cost());
    }

    public static Control crouched(Control control, String id) {
        return plus(() -> Minecraft.getInstance().options.keyShift, control, id);
    }

    // A cap with words of its own rather than a key: `input.<mod>.<name>` (the mouse, the scroll wheel).
    public static Control text(String name, String id) {
        return control(() -> Component.translatable(INPUT + name), id);
    }

    public static Control forward(String id) {
        return key(() -> Minecraft.getInstance().options.keyUp, id);
    }

    public static Control jump(String id) {
        return key(() -> Minecraft.getInstance().options.keyJump, id);
    }

    public static Control crouch(String id) {
        return key(() -> Minecraft.getInstance().options.keyShift, id);
    }
}
