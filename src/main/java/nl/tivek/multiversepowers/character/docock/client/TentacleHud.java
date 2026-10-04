package nl.tivek.multiversepowers.character.docock.client;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.docock.RobotArm;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.HudSpace;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.math.Ease;

// Doctor Octopus's four tentacles round the crosshair, each a corner where it sits on his back (upper ones on top, his
// right on the right): bright while free, amber while it holds something, dim while it is a leg or busy; a corner
// flashes when its tentacle changes.
public final class TentacleHud {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "tentacles");
    public static final String ANCHOR = "tentacles";
    private static final int ARMS = 4;
    private static final float CORNER = 7.0F;
    private static final float ARM = 3.5F;
    private static final float FLASH_MS = 350.0F;
    private static final int FREE = 0xF4F4F4;
    private static final int HOLDING = 0xFFB547;
    private static final int LEG = 0x9AA3AD;
    private static final int BUSY = 0x6E747A;
    private static final int[] SHOWN = { -1, -1, -1, -1 };
    private static final long[] CHANGED = new long[ARMS];

    private TentacleHud() {
    }

    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CROSSHAIR, LAYER_ID, TentacleHud::render);
    }

    // Whether the marks are drawn now: as Doctor Octopus, seen from his own eyes.
    public static boolean shown() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player != null && !minecraft.options.hideGui && ClientCharacter.limbs() >= 0
                && ClientCharacter.active() == GameCharacter.DOC_OCK
                && minecraft.options.getCameraType().isFirstPerson();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (!shown()) {
            return;
        }
        int limbs = ClientCharacter.limbs();
        float cx = graphics.guiWidth() / 2.0F;
        float cy = graphics.guiHeight() / 2.0F;
        long now = Util.getMillis();
        // The ring the corners take, from their inner tips to their flashed-out corners.
        float near = Mth.sqrt(CORNER * CORNER + (CORNER - ARM) * (CORNER - ARM)) - 1.0F;
        float out = CORNER + HudSpace.ring(cx, cy, near, (CORNER + 2.5F) * Mth.SQRT_OF_TWO - near, 0.0F, 360.0F)
                - near;
        for (int arm = 0; arm < ARMS; arm++) {
            int state = limbs >> arm * 2 & 3;
            if (SHOWN[arm] != state) {
                CHANGED[arm] = SHOWN[arm] < 0 ? 0L : now;
                SHOWN[arm] = state;
            }
            float flash = 1.0F - (float) Ease.smooth((now - CHANGED[arm]) / FLASH_MS);
            // Arms 0 and 1 sit high on his back, even ones on his right.
            float sx = arm % 2 == 0 ? 1.0F : -1.0F;
            float sy = arm < 2 ? -1.0F : 1.0F;
            float reach = out + 1.5F * flash;
            corner(graphics, cx + sx * reach, cy + sy * reach, sx, sy, state, flash);
        }
        ScreenAnchors.report(ANCHOR, cx - out - 1.0F, cy - out - 1.0F, out * 2.0F + 2.0F,
                out * 2.0F + 2.0F);
        GuiShapes.flush(graphics);
    }

    // An L opening toward the crosshair, its corner at (x, y).
    private static void corner(GuiGraphics graphics, float x, float y, float sx, float sy, int state, float flash) {
        int rgb = switch (state) {
            case RobotArm.HOLDING -> HOLDING;
            case RobotArm.LEG -> LEG;
            case RobotArm.BUSY -> BUSY;
            default -> FREE;
        };
        float alpha = switch (state) {
            case RobotArm.FREE -> 0.92F;
            case RobotArm.HOLDING -> 0.95F;
            case RobotArm.LEG -> 0.5F;
            default -> 0.32F;
        };
        float arm = state == RobotArm.BUSY ? ARM - 1.0F : ARM;
        int color = GuiShapes.fade(GuiShapes.mix(rgb, 0xFFFFFF, flash * 0.6F), Math.min(1.0F, alpha + flash * 0.4F));
        // A dark edge under each stroke keeps it readable on snow and sky.
        int edge = GuiShapes.fade(0x000000, 0.35F * alpha);
        float x0 = Math.min(x, x - sx * arm);
        float y0 = Math.min(y, y - sy * arm);
        GuiShapes.roundRect(graphics, x0 - 0.5F, (sy < 0 ? y : y - 1.0F) - 0.5F, arm + 1.0F, 2.0F, 0.0F, edge);
        GuiShapes.roundRect(graphics, (sx < 0 ? x : x - 1.0F) - 0.5F, y0 - 0.5F, 2.0F, arm + 1.0F, 0.0F, edge);
        GuiShapes.roundRect(graphics, x0, sy < 0 ? y : y - 1.0F, arm, 1.0F, 0.0F, color);
        GuiShapes.roundRect(graphics, sx < 0 ? x : x - 1.0F, y0, 1.0F, arm, 0.0F, color);
    }
}
