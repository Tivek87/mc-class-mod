package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.CharacterAbility;
import org.lwjgl.glfw.GLFW;

/**
 * The mod's own category in Options &gt; Controls: the wheel key (G by default) and one key per
 * ability slot, all changeable there.
 *
 * <p>A key is only a number, never a kind of ability: R is "ability 1", V is "ability 2", and so on.
 * What that ability is depends purely on the character you are.
 */
@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class AbilityKeys {
    public static final String CATEGORY = "key.categories." + MultiversePowers.MODID;

    public static final KeyMapping SPELL_WHEEL = new KeyMapping(
            "key." + MultiversePowers.MODID + ".spell_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY);

    /** One key per ability slot, in order: ability 1 up to ability 11. */
    public static final KeyMapping[] SLOTS = {
            key(AbilitySlot.ABILITY_1, GLFW.GLFW_KEY_R),
            key(AbilitySlot.ABILITY_2, GLFW.GLFW_KEY_V),
            key(AbilitySlot.ABILITY_3, GLFW.GLFW_KEY_Z),
            key(AbilitySlot.ABILITY_4, GLFW.GLFW_KEY_B),
            key(AbilitySlot.ABILITY_5, GLFW.GLFW_KEY_H),
            key(AbilitySlot.ABILITY_6, GLFW.GLFW_KEY_N),
            key(AbilitySlot.ABILITY_7, GLFW.GLFW_KEY_Y),
            key(AbilitySlot.ABILITY_8, GLFW.GLFW_KEY_X),
            key(AbilitySlot.ABILITY_9, GLFW.GLFW_KEY_C),
            key(AbilitySlot.ABILITY_10, GLFW.GLFW_KEY_LEFT_ALT),
            key(AbilitySlot.ABILITY_11, GLFW.GLFW_KEY_K) };

    public AbilityKeys(IEventBus modEventBus) {
        modEventBus.addListener(AbilityKeys::onRegisterKeys);
        modEventBus.addListener(ClientCharacter::onRegisterLayers);
    }

    /** The key of one slot. */
    public static KeyMapping of(AbilitySlot slot) {
        return SLOTS[slot.ordinal()];
    }

    /**
     * The key one ability listens to: its own slot key, or the mouse button it hangs on instead (see
     * {@link CharacterAbility#mouseButton()}).
     */
    public static KeyMapping of(CharacterAbility ability) {
        Minecraft minecraft = Minecraft.getInstance();
        return switch (ability.mouseButton()) {
            case LEFT -> minecraft.options.keyAttack;
            case RIGHT -> minecraft.options.keyUse;
            case NONE -> of(ability.slot());
        };
    }

    /**
     * Whether that key is held down right now, also while one of the mod's own screens is open (a key
     * mapping itself stops answering then), and also when it was bound to a mouse button.
     */
    public static boolean isDown(KeyMapping key) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        InputConstants.Key bound = key.getKey();
        if (bound.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(window, bound.getValue()) == GLFW.GLFW_PRESS;
        }
        return bound.getValue() != InputConstants.UNKNOWN.getValue()
                && InputConstants.isKeyDown(window, bound.getValue());
    }

    private static KeyMapping key(AbilitySlot slot, int defaultKey) {
        return new KeyMapping("key." + MultiversePowers.MODID + ".ability_" + slot.getId(),
                KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, defaultKey, CATEGORY);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(SPELL_WHEEL);
        for (KeyMapping key : SLOTS) {
            event.register(key);
        }
    }
}
