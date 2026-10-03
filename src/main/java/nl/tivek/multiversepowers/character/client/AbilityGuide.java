package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.platform.InputConstants;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import org.lwjgl.glfw.GLFW;

// The key that opens the guide to every ability of the character you play. It shares P with the game's social
// interactions on purpose: while you play a character the guide takes the press (opening it before the game reads its
// keys drops the other click), any other time the game's screen opens as always.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class AbilityGuide {
    public static final KeyMapping KEY = new KeyMapping("key." + MultiversePowers.MODID + ".ability_guide",
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_P, AbilityKeys.CATEGORY);

    private AbilityGuide() {
    }

    // What an ability does, in a few plain sentences, or null while it has none.
    @Nullable
    public static Component about(CharacterAbility ability) {
        String key = "ability." + MultiversePowers.MODID + "." + ability.path() + ".desc";
        return Language.getInstance().has(key) ? Component.translatable(key) : null;
    }

    static boolean sharesWithSocial(KeyMapping one, KeyMapping other) {
        KeyMapping social = Minecraft.getInstance().options.keySocialInteractions;
        return one == KEY && other == social || one == social && other == KEY;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        GameCharacter now = ClientCharacter.active();
        while (KEY.consumeClick()) {
            if (now != null && minecraft.screen == null && minecraft.player != null) {
                minecraft.setScreen(new AbilityGuideScreen(now));
            }
        }
    }
}
