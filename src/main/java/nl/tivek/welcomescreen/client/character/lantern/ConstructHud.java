package nl.tivek.welcomescreen.client.character.lantern;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.character.lantern.Construct;
import nl.tivek.welcomescreen.character.lantern.PowerRing;
import nl.tivek.welcomescreen.client.GuiShapes;
import nl.tivek.welcomescreen.client.character.ClientCharacter;
import nl.tivek.welcomescreen.client.character.MouseHold;
import nl.tivek.welcomescreen.network.RingPayload;

/**
 * What Green Lantern has in his hands, just above the hotbar: the construct's picture and its name. Picking
 * one also sends a ring of green light out from your crosshair, so you see the change happen where you are
 * looking. Nothing here fights: the weapons still have to be built.
 *
 * <p>Also the ring's power (drawn in the panel with your abilities, see {@link #renderPower}), and the flash
 * over your screen when your fist hits the lantern.
 */
@Mod(value = WelcomeScreenMod.MODID, dist = Dist.CLIENT)
public final class ConstructHud {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "lantern_construct");
    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0xCFFFDC;
    private static final int TEXT = 0xFFE8FFEE;
    private static final int MUTED = 0xFF8FA898;
    private static final int PENDING = 0xFFCFFFDC;
    private static final int LOW = 0xFFFF5A3A;
    /** The light of the lantern flashing over your screen. */
    private static final int FLASH = 0xE8FFEE;
    /** How thick the ring's power bar is. */
    private static final float BAR_THICK = 5.0F;

    /** The middle of the bar, this far up from the bottom of the screen. */
    private static final int BAR_UP = 72;
    private static final int BAR_HEIGHT = 17;
    private static final int ICON_ROOM = 15;
    private static final int PADDING = 7;
    /** How wide the ring around your crosshair grows, in screen points. */
    private static final float FLASH_RADIUS = 44.0F;
    /**
     * The arcs of a held mouse button: how far from your crosshair they run, how thick they are, and from how far
     * along they show (a quick tap never shows one).
     */
    private static final float HOLD_INNER = 13.0F;
    private static final float HOLD_OUTER = 17.5F;
    private static final float HOLD_SHOWN = 0.1F;
    // How long the flash lasts once an arc is full, in milliseconds.
    private static final float HOLD_FLASH_MS = 350.0F;
    // When each button's arc filled up (left, right), or 0 while it is not full.
    private static final long[] FULL_AT = new long[2];

    public ConstructHud(IEventBus modEventBus) {
        modEventBus.addListener(ConstructHud::onRegisterLayers);
    }

    private static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ConstructHud::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
                || ClientCharacter.active() != GameCharacter.GREEN_LANTERN) {
            return;
        }
        renderRechargeFlash(graphics, RechargeAnimation.flash(
                ClientRing.recharge(minecraft.player, deltaTracker.getGameTimeDeltaPartialTick(false))));
        // The wheel shows what you hold by itself, and nothing may sit under it while it is open.
        if (minecraft.screen instanceof ConstructWheelScreen) {
            return;
        }
        Construct held = ConstructChoice.held();
        renderFlash(graphics, ConstructChoice.since());
        renderHold(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        // Empty hands are how you normally walk around, so nothing is shown for them.
        if (held != Construct.NONE) {
            renderBar(graphics, minecraft.font, held);
        }
    }

    /**
     * Holding a mouse button on its way to its hold version: a thick arc beside your crosshair fills up, on the
     * right for the hand that attacks and on the left for the hand that defends, with the name of what is coming
     * next to it (the laser, the dome, the air brake). The moment it is full it flashes, and while the hold version
     * runs it stays full and throbs. A quick tap never shows it.
     */
    private static void renderHold(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float middleX = graphics.guiWidth() * 0.5F;
        float middleY = graphics.guiHeight() * 0.5F;
        long now = Util.getMillis();
        List<Runnable> labels = new ArrayList<>();
        boolean drawn = false;
        for (CharacterAbility ability : GameCharacter.GREEN_LANTERN.abilities()) {
            if (ability.mouseButton() == CharacterAbility.Mouse.NONE) {
                continue;
            }
            boolean right = ability.mouseButton() == CharacterAbility.Mouse.LEFT;
            int button = right ? 0 : 1;
            float progress = MouseHold.progress(ability, partialTick);
            if (progress < 1.0F) {
                FULL_AT[button] = 0L;
            } else if (FULL_AT[button] == 0L) {
                FULL_AT[button] = now;
            }
            if (progress < HOLD_SHOWN) {
                continue;
            }
            float filling = Mth.clamp((progress - HOLD_SHOWN) / (1.0F - HOLD_SHOWN), 0.0F, 1.0F);
            float from = right ? 30.0F : 330.0F;
            float span = right ? 120.0F : -120.0F;
            float appear = Mth.clamp((progress - HOLD_SHOWN) * 8.0F, 0.0F, 1.0F);
            // A dark track with a thin rim, so the arc reads on any background.
            GuiShapes.arc(graphics, middleX, middleY, HOLD_INNER - 1.0F, HOLD_OUTER + 1.0F, Math.min(from, from + span),
                    Math.max(from, from + span), GuiShapes.fade(0x000000, 0.45F * appear));
            GuiShapes.arc(graphics, middleX, middleY, HOLD_INNER, HOLD_OUTER, Math.min(from, from + span),
                    Math.max(from, from + span), GuiShapes.fade(0x0E3A1E, 0.8F * appear));
            float end = from + span * filling;
            boolean full = progress >= 1.0F;
            if (full) {
                float throb = 0.75F + 0.25F * Mth.sin((now % 100000L) / 90.0F);
                GuiShapes.arc(graphics, middleX, middleY, HOLD_INNER - 0.5F, HOLD_OUTER + 0.5F, Math.min(from, end),
                        Math.max(from, end), GuiShapes.fade(BRIGHT, throb));
                // The flash the moment it filled up: a band of light that swells out and fades.
                float flash = (now - FULL_AT[button]) / HOLD_FLASH_MS;
                if (flash < 1.0F) {
                    float out = HOLD_OUTER + 8.0F * flash;
                    GuiShapes.arc(graphics, middleX, middleY, out - 2.0F, out, Math.min(from, end), Math.max(from, end),
                            GuiShapes.fade(0xE6FFEC, 1.0F - flash));
                }
            } else {
                // The filled part grows brighter as it fills, with a bright head where it is now.
                int color = GuiShapes.mix(GREEN, BRIGHT, filling);
                GuiShapes.arc(graphics, middleX, middleY, HOLD_INNER, HOLD_OUTER, Math.min(from, end),
                        Math.max(from, end), GuiShapes.fade(color, 0.95F * appear));
                GuiShapes.arc(graphics, middleX, middleY, HOLD_INNER - 1.0F, HOLD_OUTER + 1.0F,
                        right ? end - 3.0F : end, right ? end : end + 3.0F, GuiShapes.fade(0xE6FFEC, appear));
            }
            Component name = holdName(ability, minecraft.player);
            float fullness = full ? 1.0F : filling;
            labels.add(() -> {
                int width = minecraft.font.width(name);
                int x = right ? Mth.floor(middleX + HOLD_OUTER + 5.0F) : Mth.ceil(middleX - HOLD_OUTER - 5.0F) - width;
                int alpha = (int) (255 * appear);
                int color = GuiShapes.mix(GREEN, full ? 0xFFFFFF : BRIGHT, fullness);
                graphics.drawString(minecraft.font, name, x, Mth.floor(middleY) - 4, alpha << 24 | color);
            });
            drawn = true;
        }
        if (drawn) {
            GuiShapes.flush(graphics);
        }
        labels.forEach(Runnable::run);
    }

    /** What holding this button leads to: the laser, the dome, or in the air the brake. */
    private static Component holdName(CharacterAbility ability, @Nullable Player player) {
        String prefix = "screen." + WelcomeScreenMod.MODID + ".hold.";
        if (ability.mouseButton() == CharacterAbility.Mouse.LEFT) {
            return Component.translatable(prefix + "beam");
        }
        boolean flying = player != null && ClientRing.flight(player, 0.0F) >= 0.0F;
        return Component.translatable(prefix + (flying ? "brake" : "dome"));
    }

    /**
     * What an ability of Green Lantern is doing right now, for the panel with your abilities, or null to let the
     * panel say it itself: the shield is on, the dome or the beam is up, you fly or sink.
     */
    @Nullable
    public static Component status(CharacterAbility ability, Player player) {
        String prefix = "screen." + WelcomeScreenMod.MODID + ".character.";
        return switch (ability.id()) {
            case "light_bolt" -> ClientRing.has(player, RingPayload.BEAM)
                    ? Component.translatable(prefix + "beam") : null;
            case "light_shield" -> ClientRing.has(player, RingPayload.DOME)
                    ? Component.translatable(prefix + "dome")
                    : ClientRing.has(player, RingPayload.SHIELD) ? Component.translatable(prefix + "on") : null;
            case "flight" -> ClientRing.has(player, RingPayload.DESCENT) ? Component.translatable(prefix + "sinking")
                    : ClientRing.flight(player, 0.0F) >= 0.0F ? Component.translatable(prefix + "flying") : null;
            default -> null;
        };
    }

    /** The light of the lantern when your fist hits it, over your whole screen for a moment. */
    private static void renderRechargeFlash(GuiGraphics graphics, float flash) {
        if (flash > 0.0F) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), GuiShapes.fade(FLASH, 0.7F * flash));
        }
    }

    /**
     * The ring's power, as the bottom line of the panel with your abilities: how full it is, and while you
     * charge a fist, the part that letting go will cost. Red once it cannot pay for the smallest fist.
     *
     * @param lowAt below this much power the ring is shown as too low
     */
    public static void renderPower(GuiGraphics graphics, Font font, Player player, int left, int right, int y,
            float lowAt) {
        float power = ClientRing.power(player);
        float pending = Math.min(ClientRing.pending(player), power);
        boolean low = power + 1.0E-4F < lowAt;
        Component label = Component.translatable("ring." + WelcomeScreenMod.MODID + ".label");
        // While the ring drains by itself (flying, the shield, the beam) it says how fast, and in the air how many
        // seconds that leaves.
        boolean steady = ClientRing.flight(player, 0.0F) >= 0.0F || ClientRing.has(player, RingPayload.SHIELD)
                || ClientRing.has(player, RingPayload.DOME) || ClientRing.has(player, RingPayload.BEAM);
        float drain = steady ? ClientRing.drain() : 0.0F;
        String amount;
        if (pending > 0.0F) {
            amount = String.format(Locale.ROOT, "%.0f -%.1f", power, pending);
        } else if (drain >= 0.2F && ClientRing.flight(player, 0.0F) >= 0.0F) {
            amount = String.format(Locale.ROOT, "%.0f -%.1f/s %ds", power, drain, (int) Math.ceil(power / drain));
        } else if (drain >= 0.2F) {
            amount = String.format(Locale.ROOT, "%.0f -%.1f/s", power, drain);
        } else {
            amount = String.format(Locale.ROOT, "%.0f", power);
        }
        int labelWidth = font.width(label);
        int amountWidth = font.width(amount);
        graphics.drawString(font, label, left, y, MUTED, false);
        graphics.drawString(font, amount, right - amountWidth, y, low ? LOW : pending > 0.0F ? PENDING : TEXT, false);

        float barLeft = left + labelWidth + 5.0F;
        float barWidth = right - amountWidth - 5.0F - barLeft;
        if (barWidth < 8.0F) {
            return;
        }
        float barTop = y + (font.lineHeight - 1 - BAR_THICK) * 0.5F;
        float full = barWidth * Mth.clamp(power / PowerRing.MAX_POWER, 0.0F, 1.0F);
        float cost = barWidth * pending / PowerRing.MAX_POWER;
        GuiShapes.roundRect(graphics, barLeft, barTop, barWidth, BAR_THICK, BAR_THICK * 0.5F,
                GuiShapes.fade(0x0B2E18, 0.9F));
        if (full > 0.5F) {
            GuiShapes.roundRect(graphics, barLeft, barTop, full, BAR_THICK, BAR_THICK * 0.5F,
                    GuiShapes.fade(low ? 0xFF5A3A : GREEN, 1.0F));
        }
        // The part the fist you charge will take, blinking at the end of what is left.
        if (cost > 0.5F) {
            float blink = 0.55F + 0.45F * Mth.sin((Util.getMillis() % 100000L) / 120.0F);
            GuiShapes.roundRect(graphics, barLeft + full - cost, barTop, cost, BAR_THICK, BAR_THICK * 0.5F,
                    GuiShapes.fade(BRIGHT, blink));
        }
        GuiShapes.flush(graphics);
    }

    /** A ring of green light that flares out of your crosshair the moment your hands change. */
    private static void renderFlash(GuiGraphics graphics, long since) {
        if (since < 0L || since > ConstructChoice.FLASH_MS) {
            return;
        }
        float part = (float) since / ConstructChoice.FLASH_MS;
        float eased = 1.0F - (1.0F - part) * (1.0F - part);
        float middleX = graphics.guiWidth() * 0.5F;
        float middleY = graphics.guiHeight() * 0.5F;
        GuiShapes.ring(graphics, middleX, middleY, 5.0F + FLASH_RADIUS * eased,
                1.0F + 3.0F * (1.0F - eased), GuiShapes.fade(BRIGHT, 0.85F * (1.0F - part)));
        GuiShapes.ring(graphics, middleX, middleY, 5.0F + FLASH_RADIUS * eased * 0.78F,
                1.0F + 5.0F * (1.0F - eased), GuiShapes.fade(GREEN, 0.4F * (1.0F - part)));
        GuiShapes.flush(graphics);
    }

    /** The bar itself: an empty picture frame, the slot's name, and that it is still to come. */
    private static void renderBar(GuiGraphics graphics, Font font, Construct held) {
        MutableComponent name = held.getDisplayName().copy();
        MutableComponent about = held.getDescription().copy().withStyle(ChatFormatting.ITALIC);
        int nameWidth = font.width(name);
        int aboutWidth = font.width(about);
        int width = PADDING + ICON_ROOM + 5 + nameWidth + 6 + aboutWidth + PADDING;

        float left = (graphics.guiWidth() - width) * 0.5F;
        float top = graphics.guiHeight() - BAR_UP - BAR_HEIGHT * 0.5F;
        GuiShapes.roundRect(graphics, left, top, width, BAR_HEIGHT, BAR_HEIGHT * 0.5F,
                GuiShapes.fade(0x04140A, 0.62F));
        GuiShapes.arc(graphics, left + BAR_HEIGHT * 0.5F, top + BAR_HEIGHT * 0.5F,
                BAR_HEIGHT * 0.5F - 1.0F, BAR_HEIGHT * 0.5F, 90.0F, 270.0F, GuiShapes.fade(GREEN, 0.5F));
        // The construct drawings are not made yet, so the frame stays empty.
        float iconX = left + PADDING + ICON_ROOM * 0.5F;
        float iconY = top + BAR_HEIGHT * 0.5F;
        float half = ICON_ROOM * 0.32F;
        GuiShapes.arc(graphics, iconX, iconY, half - 1.4F, half, 0.0F, 360.0F, GuiShapes.fade(BRIGHT, 0.5F));
        GuiShapes.disc(graphics, iconX, iconY, 1.6F, GuiShapes.fade(BRIGHT, 0.95F));
        GuiShapes.flush(graphics);

        int textX = Mth.floor(left) + PADDING + ICON_ROOM + 5;
        int textY = Mth.floor(top) + (BAR_HEIGHT - font.lineHeight) / 2 + 1;
        graphics.drawString(font, name, textX, textY, TEXT, false);
        graphics.drawString(font, about, textX + nameWidth + 6, textY, MUTED, false);
    }
}
