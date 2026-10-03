package nl.tivek.multiversepowers.character.client;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;

// A state of a character in which its keys and buttons do other things (piloting a mech, a weapon in hand), for the
// guide: when it starts and ends, what every input does in it and what is off. Its words live in en_us.json under
// `guide.<mod>.<character>.<id>.` (`title`, `when`, `off` and one line per control).
public record GuideMode(GameCharacter character, String id, Predicate<LocalPlayer> active, List<Control> controls) {
    private static final String INPUT = "input." + MultiversePowers.MODID + ".";

    // `key` is drawn on a key cap and follows the player's own key settings.
    public record Control(Supplier<Component> key, String id) {
    }

    public Component title() {
        return Component.translatable(this.path() + "title");
    }

    public Component when() {
        return Component.translatable(this.path() + "when");
    }

    @Nullable
    public Component off() {
        String key = this.path() + "off";
        return Language.getInstance().has(key) ? Component.translatable(key) : null;
    }

    public Component does(Control control) {
        return Component.translatable(this.path() + control.id());
    }

    private String path() {
        return "guide." + MultiversePowers.MODID + "." + this.character.getId() + "." + this.id + ".";
    }

    public static Control click(CharacterAbility.Input input, String id) {
        return new Control(() -> PowerInputs.keyName(PowerInputs.clickKey(input)), id);
    }

    public static Control hold(CharacterAbility.Input input, String id) {
        return new Control(() -> PowerInputs.holdLabel(input), id);
    }

    public static Control key(Supplier<KeyMapping> key, String id) {
        return new Control(() -> PowerInputs.keyName(key.get()), id);
    }

    public static Control holdKey(Supplier<KeyMapping> key, String id) {
        return new Control(() -> Component.translatable(INPUT + "hold", PowerInputs.keyName(key.get())), id);
    }

    public static Control doubleKey(Supplier<KeyMapping> key, String id) {
        return new Control(() -> Component.translatable(INPUT + "double", PowerInputs.keyName(key.get())), id);
    }

    // The key or gesture an ability sits on, as the panel shows it.
    public static Control ability(GameCharacter character, String ability, String id) {
        return new Control(() -> {
            CharacterAbility found = character.byName(ability);
            return found == null ? Component.literal("-") : PowerInputs.label(found);
        }, id);
    }

    // A key held down while another is pressed: "Shift + K".
    public static Control plus(Supplier<KeyMapping> held, Control control, String id) {
        return new Control(() -> Component.translatable(INPUT + "plus", PowerInputs.keyName(held.get()),
                control.key().get()), id);
    }

    public static Control crouched(Control control, String id) {
        return plus(() -> Minecraft.getInstance().options.keyShift, control, id);
    }

    // Several keys on one cap, such as the four walking keys.
    @SafeVarargs
    public static Control keys(String id, Supplier<KeyMapping>... keys) {
        return new Control(() -> {
            MutableComponent all = Component.empty();
            for (int i = 0; i < keys.length; i++) {
                if (i > 0) {
                    all.append(" ");
                }
                all.append(PowerInputs.keyName(keys[i].get()));
            }
            return all;
        }, id);
    }

    // A cap with words of its own rather than a key: `input.<mod>.<name>` (the mouse, the scroll wheel).
    public static Control text(String name, String id) {
        return new Control(() -> Component.translatable(INPUT + name), id);
    }

    public static Control walk(String id) {
        return keys(id, () -> options().keyUp, () -> options().keyLeft, () -> options().keyDown,
                () -> options().keyRight);
    }

    public static Control forward(String id) {
        return key(() -> Minecraft.getInstance().options.keyUp, id);
    }

    public static Control back(String id) {
        return key(() -> Minecraft.getInstance().options.keyDown, id);
    }

    public static Control sides(String id) {
        return keys(id, () -> options().keyLeft, () -> options().keyRight);
    }

    private static Options options() {
        return Minecraft.getInstance().options;
    }

    public static Control jump(String id) {
        return key(() -> Minecraft.getInstance().options.keyJump, id);
    }

    public static Control sprint(String id) {
        return key(() -> Minecraft.getInstance().options.keySprint, id);
    }

    public static Control crouch(String id) {
        return key(() -> Minecraft.getInstance().options.keyShift, id);
    }
}
