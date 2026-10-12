package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import org.lwjgl.glfw.GLFW;

// The mouse, space and shift gestures powers use, Ability 13 to 23 in Options > Controls, right after the ability keys:
// each fixed to its button, as left, right and space follow the game's own attack, use and jump keys.
public final class PowerInputs {
    public static final KeyMapping LEFT_CLICK = locked("left_click", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT, CharacterAbility.Input.LEFT, 13);
    public static final KeyMapping LEFT_HOLD = locked("left_hold", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT, CharacterAbility.Input.LEFT, 14);
    public static final KeyMapping RIGHT_CLICK = locked("right_click", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT, CharacterAbility.Input.RIGHT, 15);
    public static final KeyMapping RIGHT_HOLD = locked("right_hold", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT, CharacterAbility.Input.RIGHT, 16);
    public static final KeyMapping SCROLL_CLICK = locked("scroll_click", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE, null, 17);
    public static final KeyMapping SCROLL_HOLD = locked("scroll_hold", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_MIDDLE, null, 18);
    public static final KeyMapping SINGLE_SPACE = locked("single_space", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE, CharacterAbility.Input.SPACE, 19);
    public static final KeyMapping DOUBLE_SPACE = locked("double_space", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE, CharacterAbility.Input.SPACE, 20);
    public static final KeyMapping HOLD_SPACE = locked("hold_space", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE, CharacterAbility.Input.SPACE, 21);
    // Shift's own keys, not the game's sneak: many play with shift on sprint and sneak elsewhere.
    public static final KeyMapping CLICK_SHIFT = locked("click_shift", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT, null, 22);
    public static final KeyMapping HOLD_SHIFT = locked("hold_shift", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_SHIFT, null, 23);

    private static final KeyMapping[] ALL = { LEFT_CLICK, LEFT_HOLD, RIGHT_CLICK, RIGHT_HOLD, SCROLL_CLICK,
            SCROLL_HOLD, SINGLE_SPACE, DOUBLE_SPACE, HOLD_SPACE, CLICK_SHIFT, HOLD_SHIFT };
    private static final String INPUT = "input." + MultiversePowers.MODID + ".";

    private PowerInputs() {
    }

    static void register(RegisterKeyMappingsEvent event) {
        for (KeyMapping key : ALL) {
            event.register(key);
        }
    }

    // The key a gesture's click is read from.
    public static KeyMapping clickKey(CharacterAbility.Input input) {
        Minecraft minecraft = Minecraft.getInstance();
        return switch (input) {
            case LEFT -> minecraft.options.keyAttack;
            case RIGHT -> minecraft.options.keyUse;
            case SCROLL -> SCROLL_CLICK;
            case SHIFT -> CLICK_SHIFT;
            case SPACE, KEY -> minecraft.options.keyJump;
        };
    }

    // The key a gesture's hold is read from: the scroll wheel's and shift's are entries of their own.
    public static KeyMapping holdKey(CharacterAbility.Input input) {
        return switch (input) {
            case SCROLL -> SCROLL_HOLD;
            case SHIFT -> HOLD_SHIFT;
            default -> clickKey(input);
        };
    }

    // The entry in Options > Controls that fires an ability, by its number (`CharacterAbility.binding`).
    public static KeyMapping binding(CharacterAbility ability) {
        int number = ability.binding();
        return number <= AbilityKeys.SLOTS.length ? AbilityKeys.SLOTS[number - 1]
                : ALL[number - AbilityKeys.SLOTS.length - 1];
    }

    // An ability's entry as Options > Controls names it, with what it is bound to: "Ability 20: Double Space (Space)".
    public static Component bindingName(CharacterAbility ability) {
        return Component.translatable(INPUT + "binding", Component.translatable(binding(ability).getName()),
                label(ability));
    }

    // What the ability panel and the settings show for an ability's gesture.
    public static Component label(CharacterAbility ability) {
        CharacterAbility.Input input = ability.input();
        if (input == CharacterAbility.Input.KEY) {
            KeyMapping key = AbilityKeys.of(ability.slot());
            return key == null ? Component.literal("-") : keyName(key);
        }
        if (ability.tapWhen() == CharacterAbility.Tap.DOUBLE) {
            return Component.translatable(INPUT + "double", keyName(clickKey(input)));
        }
        return ability.tapWhen() == CharacterAbility.Tap.NEVER ? holdLabel(input) : keyName(clickKey(input));
    }

    // A gesture's hold on its own, beside its click.
    public static Component holdLabel(CharacterAbility.Input input) {
        return Component.translatable(INPUT + "hold", keyName(holdKey(input)));
    }

    // Whether a key or button fires the ability at all.
    public static boolean bound(CharacterAbility ability) {
        if (ability.input() == CharacterAbility.Input.KEY) {
            KeyMapping key = AbilityKeys.of(ability.slot());
            return key != null && !key.isUnbound();
        }
        KeyMapping key = ability.tapWhen() == CharacterAbility.Tap.NEVER ? holdKey(ability.input())
                : clickKey(ability.input());
        return !key.isUnbound();
    }

    // A key as the game names it, a mouse button short: LMB, RMB, MMB, M4.
    public static Component keyName(KeyMapping key) {
        InputConstants.Key bound = key.getKey();
        if (bound.getType() != InputConstants.Type.MOUSE || key.getKeyModifier() != KeyModifier.NONE) {
            return key.getTranslatedKeyMessage();
        }
        int button = bound.getValue();
        String mouse = INPUT + "mouse";
        return button <= GLFW.GLFW_MOUSE_BUTTON_MIDDLE ? Component.translatable(mouse + "." + button)
                : Component.translatable(mouse, button + 1);
    }

    public static boolean isLocked(KeyMapping key) {
        return key instanceof Locked;
    }

    // A gesture shares its button with the game's own key on purpose: that is no clash to show in red.
    public static boolean sharedOnPurpose(KeyMapping one, KeyMapping other) {
        if (AbilityGuide.sharesWithSocial(one, other)) {
            return true;
        }
        if (mine(one) && mine(other)) {
            // A click and a hold of the same button, as they come.
            return one.isDefault() && other.isDefault();
        }
        return mine(one) && one.isDefault() && !mine(other) || mine(other) && other.isDefault() && !mine(one);
    }

    private static boolean mine(KeyMapping key) {
        for (KeyMapping gesture : ALL) {
            if (gesture == key) {
                return true;
            }
        }
        return false;
    }

    private static String name(String id) {
        return "key." + MultiversePowers.MODID + ".input." + id;
    }

    private static KeyMapping locked(String id, InputConstants.Type type, int code,
            @Nullable CharacterAbility.Input follows, int order) {
        return new Locked(name(id), type, code, follows, order);
    }

    private static final class Locked extends OrderedKey {
        @Nullable
        private final CharacterAbility.Input follows;

        Locked(String name, InputConstants.Type type, int code, @Nullable CharacterAbility.Input follows, int order) {
            super(name, type, code, order);
            this.follows = follows;
        }

        // Shows the game's own key it follows, in case that one was moved.
        @Override
        public Component getTranslatedKeyMessage() {
            return this.follows == null ? super.getTranslatedKeyMessage()
                    : clickKey(this.follows).getTranslatedKeyMessage();
        }

        @Override
        public void setKey(InputConstants.Key key) {
            super.setKey(this.getDefaultKey());
        }

        @Override
        public void setKeyModifierAndCode(KeyModifier keyModifier, InputConstants.Key keyCode) {
            super.setKeyModifierAndCode(KeyModifier.NONE, this.getDefaultKey());
        }
    }
}
