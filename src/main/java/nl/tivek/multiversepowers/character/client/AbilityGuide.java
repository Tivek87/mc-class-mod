package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
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

    // What a guided tour asks the open guide to bring into view: the overview's row, or the first binds that belong
    // together.
    public static final String HOW = "how";
    public static final String PAIR = "pair";
    private static final long REVEAL_MS = 250L;

    private static final Map<GameCharacter, List<GuideMode>> MODES = new EnumMap<>(GameCharacter.class);
    @Nullable
    private static String reveal;
    private static long revealUntil;

    private AbilityGuide() {
    }

    // Asked every frame while wanted, so it lapses by itself.
    public static void reveal(String what) {
        reveal = what;
        revealUntil = Util.getMillis() + REVEAL_MS;
    }

    @Nullable
    static String revealing() {
        return Util.getMillis() < revealUntil ? reveal : null;
    }

    // Whether the character you play has binds that belong together (`GuideMode.Control.parent`).
    public static boolean grouped() {
        GameCharacter now = ClientCharacter.active();
        return now != null && modes(now).stream()
                .anyMatch(mode -> mode.controls().stream().anyMatch(GuideMode.Control::child));
    }

    // The states in which a character's keys do other things, in the order the guide lists them.
    public static void modes(GameCharacter character, GuideMode... modes) {
        MODES.put(character, List.of(modes));
    }

    static List<GuideMode> modes(GameCharacter character) {
        return MODES.getOrDefault(character, List.of());
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

    // Opens the guide to the character you play, from wherever you are; false while you play none.
    public static boolean open() {
        Minecraft minecraft = Minecraft.getInstance();
        GameCharacter now = ClientCharacter.active();
        if (now == null || minecraft.player == null) {
            return false;
        }
        minecraft.setScreen(new AbilityGuideScreen(now));
        return true;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        while (KEY.consumeClick()) {
            if (minecraft.screen == null) {
                open();
            }
        }
    }
}
