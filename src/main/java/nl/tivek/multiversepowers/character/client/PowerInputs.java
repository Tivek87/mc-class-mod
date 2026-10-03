package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import org.lwjgl.glfw.GLFW;

// The mouse, space and shift gestures powers use, listed under Controls. Left, right and space follow the game's own
// attack, use and jump keys and cannot be changed here; the scroll wheel's click and hold and the held shift can.
public final class PowerInputs {
    public static final String CATEGORY = "key.categories." + MultiversePowers.MODID + ".inputs";

    public static final KeyMapping LEFT_CLICK = locked("left_click", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT, CharacterAbility.Input.LEFT);
    public static final KeyMapping LEFT_HOLD = locked("left_hold", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT, CharacterAbility.Input.LEFT);
    public static final KeyMapping RIGHT_CLICK = locked("right_click", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT, CharacterAbility.Input.RIGHT);
    public static final KeyMapping RIGHT_HOLD = locked("right_hold", InputConstants.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT, CharacterAbility.Input.RIGHT);
    public static final KeyMapping SCROLL_CLICK = new KeyMapping(name("scroll_click"), KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, CATEGORY);
    public static final KeyMapping SCROLL_HOLD = new KeyMapping(name("scroll_hold"), KeyConflictContext.IN_GAME,
            InputConstants.Type.MOUSE, GLFW.GLFW_MOUSE_BUTTON_MIDDLE, CATEGORY);
    public static final KeyMapping DOUBLE_SPACE = locked("double_space", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE, CharacterAbility.Input.SPACE);
    public static final KeyMapping HOLD_SPACE = locked("hold_space", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_SPACE, CharacterAbility.Input.SPACE);
    // Its own key, not the game's sneak: many play with shift on sprint and sneak elsewhere.
    public static final KeyMapping HOLD_SHIFT = new KeyMapping(name("hold_shift"), KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, CATEGORY);

    private static final KeyMapping[] ALL = { LEFT_CLICK, LEFT_HOLD, RIGHT_CLICK, RIGHT_HOLD, SCROLL_CLICK,
            SCROLL_HOLD, DOUBLE_SPACE, HOLD_SPACE, HOLD_SHIFT };

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
            case SHIFT -> HOLD_SHIFT;
            case SPACE, KEY -> minecraft.options.keyJump;
        };
    }

    // The key a gesture's hold is read from: only the scroll wheel's can differ from its click.
    public static KeyMapping holdKey(CharacterAbility.Input input) {
        return input == CharacterAbility.Input.SCROLL ? SCROLL_HOLD : clickKey(input);
    }

    // What the ability panel and the settings show for an ability's gesture.
    public static Component label(CharacterAbility ability) {
        CharacterAbility.Input input = ability.input();
        if (input == CharacterAbility.Input.KEY) {
            KeyMapping key = AbilityKeys.of(ability.slot());
            return key == null ? Component.literal("-") : keyName(key);
        }
        if (ability.tapWhen() == CharacterAbility.Tap.DOUBLE) {
            return Component.translatable("input." + MultiversePowers.MODID + ".double", keyName(clickKey(input)));
        }
        return ability.tapWhen() == CharacterAbility.Tap.NEVER ? holdLabel(input) : keyName(clickKey(input));
    }

    // A gesture's hold on its own, beside its click.
    public static Component holdLabel(CharacterAbility.Input input) {
        return Component.translatable("input." + MultiversePowers.MODID + ".hold", keyName(holdKey(input)));
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
        String mouse = "input." + MultiversePowers.MODID + ".mouse";
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

    private static KeyMapping locked(String id, InputConstants.Type type, int code, CharacterAbility.Input follows) {
        return new Locked(name(id), type, code, follows);
    }

    private static final class Locked extends KeyMapping {
        private final CharacterAbility.Input follows;

        Locked(String name, InputConstants.Type type, int code, CharacterAbility.Input follows) {
            super(name, KeyConflictContext.IN_GAME, type, code, CATEGORY);
            this.follows = follows;
        }

        // Shows the game's own key it follows, in case that one was moved.
        @Override
        public Component getTranslatedKeyMessage() {
            return clickKey(this.follows).getTranslatedKeyMessage();
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
