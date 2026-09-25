package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;

public final class ConstructWheel {
    private static final long TAP_MS = 160L;

    private static long downSince = -1L;

    private ConstructWheel() {
    }

    public static void tick(Minecraft minecraft, KeyMapping key) {
        boolean down = minecraft.screen == null && key.isDown();
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
