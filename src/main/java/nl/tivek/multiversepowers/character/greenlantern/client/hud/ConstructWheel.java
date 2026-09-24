package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;

/**
 * What the construct key does before the wheel is even on screen.
 *
 * <ul>
 * <li><b>Tap it</b> and nothing opens at all: you swap straight between empty hands and the last
 * construct you had out. That is the quick one, for in a fight.</li>
 * <li><b>Hold it</b> a moment longer and the wheel opens (see {@link ConstructWheelScreen}), where you
 * pick by flicking the mouse.</li>
 * </ul>
 *
 * <p>Waiting that moment before opening is what keeps a tap from flashing the wheel on screen.
 */
public final class ConstructWheel {
    /** Let go of the key within this many milliseconds and it counts as a tap, not as holding it. */
    private static final long TAP_MS = 160L;

    /** When the key went down, or -1 while it is up. */
    private static long downSince = -1L;

    private ConstructWheel() {
    }

    /** Called every client tick while you are someone whose key this is. */
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
            // Up again before the wheel opened: a tap.
            downSince = -1L;
            ConstructChoice.swap();
        }
    }

    /** You are not that character any more, or the world went away: forget a key that was down. */
    public static void stop() {
        downSince = -1L;
    }
}
