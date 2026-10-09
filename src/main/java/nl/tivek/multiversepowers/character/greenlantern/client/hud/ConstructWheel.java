package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;

public final class ConstructWheel {
    private static final long TAP_MS = 160L;

    private static long downSince = -1L;
    private static boolean piloting;

    private ConstructWheel() {
    }

    public static void tick(Minecraft minecraft, KeyMapping key) {
        boolean down = minecraft.screen == null && key.isDown();
        if (minecraft.player != null && ClientConstructs.piloted(minecraft.player.getId(), 0.0F) != null) {
            // In the mech the key works its flamethrower (MechAssembly).
            CharacterAbility wheel = GameCharacter.GREEN_LANTERN.byName("construct_wheel");
            if (down && !piloting && wheel != null) {
                ClientCharacter.sendAction(wheel, true, 0);
            }
            piloting = down;
            downSince = -1L;
            return;
        }
        piloting = false;
        if (down) {
            if (downSince < 0L) {
                downSince = Util.getMillis();
            } else if (Util.getMillis() - downSince >= TAP_MS) {
                downSince = -1L;
                minecraft.setScreen(new ConstructWheelScreen(key));
            }
            return;
        }
        if (downSince >= 0L) {
            downSince = -1L;
            ConstructChoice.swap();
        }
    }

    public static void stop() {
        downSince = -1L;
    }
}
