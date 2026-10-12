package nl.tivek.multiversepowers.character.docock.client;

import static nl.tivek.multiversepowers.character.docock.TentacleReach.COUNT;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.docock.TentacleReach;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// Doctor Octopus's tentacles round the crosshair, each a corner where it sits on his back (1 top left, 2 top right, 3
// bottom left, 4 bottom right) in the colour of what it does, with its number and word: on the ground, free, holding,
// squeezing. In Selection Mode the picked ones light up and the mode is named under the crosshair; a short message
// shows there for a moment when a pick or a key cannot be done.
final class OctoHud {
    private static final String PREFIX = "octopus." + MultiversePowers.MODID + ".";
    private static final int GROUND = 0xFF8A7356;
    private static final int FREE = 0xFFD8DEE6;
    private static final int HOLDING = 0xFFFFB547;
    private static final int SQUEEZING = 0xFFFF4A3A;
    private static final int PICKED = 0xFF59E1FF;
    private static final int MESSAGE_TICKS = 40;
    private static Component message = Component.empty();
    private static long messageAt = Long.MIN_VALUE / 2;

    private OctoHud() {
    }

    static void tell(String key, Object... args) {
        message = Component.translatable(PREFIX + "hud." + key, args);
        messageAt = time();
    }

    private static long time() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0L : minecraft.level.getGameTime();
    }

    static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null
                || ClientCharacter.active() != GameCharacter.DOC_OCK) {
            return;
        }
        ClientOcto.View view = ClientOcto.of(minecraft.player);
        if (view == null) {
            return;
        }
        Font font = minecraft.font;
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        boolean selecting = OctoControls.selecting();
        int picked = OctoControls.picked();
        ScreenAnchors.report("game.tentacles", cx - 44, cy - 22, 88, 44);
        for (int i = 0; i < COUNT; i++) {
            int sx = TentacleReach.side(i) > 0 ? -1 : 1;
            int sy = TentacleReach.upper(i) ? -1 : 1;
            int x = cx + sx * 16;
            int y = cy + sy * 11;
            int color = view.ground(i) ? GROUND : view.squeezing(i) ? SQUEEZING : view.holding(i) ? HOLDING : FREE;
            boolean lit = selecting && (picked & 1 << i) != 0;
            int thick = lit ? 2 : 1;
            int arm = 7;
            // A corner opening towards the crosshair.
            int ex = sx > 0 ? x + arm : x - arm;
            int ey = sy > 0 ? y + arm : y - arm;
            graphics.fill(Math.min(x, ex), sy > 0 ? y + arm - thick : y - arm, Math.max(x, ex),
                    sy > 0 ? y + arm : y - arm + thick, lit ? PICKED : color);
            graphics.fill(sx > 0 ? x + arm - thick : x - arm, Math.min(y, ey), sx > 0 ? x + arm : x - arm + thick,
                    Math.max(y, ey), lit ? PICKED : color);
            String state = view.ground(i) ? "ground" : view.squeezing(i) ? "squeezing" : view.holding(i) ? "holding"
                    : "free";
            Component label = Component.literal((i + 1) + " ").append(Component.translatable(PREFIX + "state." + state));
            graphics.pose().pushPose();
            float half = 0.5F;
            int width = font.width(label);
            float lx = sx > 0 ? ex + 2 : ex - 2 - width * half;
            float ly = sy > 0 ? ey - 4 : ey;
            graphics.pose().translate(lx, ly, 0.0F);
            graphics.pose().scale(half, half, 1.0F);
            graphics.drawString(font, label, 0, 0, lit ? PICKED : color, true);
            graphics.pose().popPose();
        }
        int y = cy + 22;
        if (selecting) {
            Component mode = Component.translatable(PREFIX + "selection");
            graphics.drawString(font, mode, cx - font.width(mode) / 2, y, PICKED, true);
            y += 11;
        }
        long age = time() - messageAt;
        if (age >= 0 && age < MESSAGE_TICKS) {
            int alpha = (int) (255 * Math.min(1.0, (MESSAGE_TICKS - age) / 10.0));
            if (alpha > 8) {
                graphics.drawString(font, message, cx - font.width(message) / 2, y, alpha << 24 | 0xFFFFFF, true);
            }
        }
    }
}
