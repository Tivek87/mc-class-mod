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

@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class AbilityKeys {
    public static final String CATEGORY = "key.categories." + MultiversePowers.MODID;

    public static final KeyMapping SPELL_WHEEL = new KeyMapping(
            "key." + MultiversePowers.MODID + ".spell_wheel",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            CATEGORY);

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

    public static KeyMapping of(AbilitySlot slot) {
        return SLOTS[slot.ordinal()];
    }

    public static KeyMapping of(CharacterAbility ability) {
        Minecraft minecraft = Minecraft.getInstance();
        return switch (ability.mouseButton()) {
            case LEFT -> minecraft.options.keyAttack;
            case RIGHT -> minecraft.options.keyUse;
            case NONE -> of(ability.slot());
        };
    }

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
