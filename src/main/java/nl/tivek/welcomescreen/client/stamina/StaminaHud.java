package nl.tivek.welcomescreen.client.stamina;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Draws the stamina icons on the right, stacked on top of the hunger bar.
 */
@Mod(value = WelcomeScreenMod.MODID, dist = Dist.CLIENT)
public final class StaminaHud {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "stamina_bar");

    private static final ResourceLocation SPRITE_FULL =
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "hud/stamina/full");
    private static final ResourceLocation SPRITE_HALF =
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "hud/stamina/half");
    private static final ResourceLocation SPRITE_EMPTY =
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "hud/stamina/empty");

    private static final int ICON_COUNT = 10;
    private static final int ICON_SIZE = 9;
    private static final int ICON_SPACING = 8;
    private static final int ROW_HEIGHT = 10;
    private static final float LOW_FRACTION = 0.3F;

    public StaminaHud(IEventBus modEventBus) {
        modEventBus.addListener(StaminaHud::onRegisterLayers);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        // Stacked on the right side above the hunger bar (and above air bubbles when underwater)
        event.registerAbove(VanillaGuiLayers.AIR_LEVEL, LAYER_ID, StaminaHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.getAbilities().flying || !StaminaClient.usesStamina(minecraft, player)) {
            return;
        }

        Gui gui = minecraft.gui;
        int right = graphics.guiWidth() / 2 + 91;
        int top = graphics.guiHeight() - gui.rightHeight;
        gui.rightHeight += ROW_HEIGHT;

        float stamina = StaminaClient.getStamina(deltaTracker.getGameTimeDeltaPartialTick(false));
        float maxStamina = StaminaClient.getMax();
        float fraction = maxStamina > 0.0F ? stamina / maxStamina : 1.0F;
        boolean exhausted = StaminaClient.isExhausted();

        // 10 icons * 2 = 20 half-steps
        int staminaPoints = Mth.ceil(fraction * 20.0F);

        boolean redFlash = exhausted && ((player.tickCount / 5) % 2 == 0);
        if (redFlash) {
            graphics.setColor(1.0F, 0.35F, 0.35F, 1.0F);
        }

        // Render from right to left, matching vanilla hunger bar alignment
        for (int i = 0; i < ICON_COUNT; i++) {
            int x = right - i * ICON_SPACING - ICON_SIZE;
            int y = top;

            // Subtle shake when low stamina or exhausted (like vanilla low hunger)
            if (exhausted) {
                if ((player.tickCount + i) % 3 == 0) {
                    y += 1;
                }
            } else if (fraction < LOW_FRACTION) {
                if ((player.tickCount + i) % 4 == 0) {
                    y += 1;
                }
            }

            // Draw empty container background
            graphics.blitSprite(SPRITE_EMPTY, x, y, ICON_SIZE, ICON_SIZE);

            // Depletes from left to right (from center outward, matching vanilla hunger)
            int point = i * 2 + 1;
            if (point < staminaPoints) {
                graphics.blitSprite(SPRITE_FULL, x, y, ICON_SIZE, ICON_SIZE);
            } else if (point == staminaPoints) {
                graphics.blitSprite(SPRITE_HALF, x, y, ICON_SIZE, ICON_SIZE);
            }
        }

        if (redFlash) {
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
