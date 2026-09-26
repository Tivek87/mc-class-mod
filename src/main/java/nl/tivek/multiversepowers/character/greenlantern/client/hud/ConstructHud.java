package nl.tivek.multiversepowers.character.greenlantern.client.hud;

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
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RechargeAnimation;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.WhipArms;
import nl.tivek.multiversepowers.config.Unit;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;

@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class ConstructHud {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "lantern_construct");
    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0xCFFFDC;
    private static final int TEXT = 0xFFE8FFEE;
    private static final int MUTED = 0xFF8FA898;
    private static final int PENDING = 0xFFCFFFDC;
    private static final int LOW = 0xFFFF5A3A;
    private static final int PAID = 0xF2D98A;
    private static final int FLASH = 0xE8FFEE;
    private static final float BAR_THICK = 6.0F;
    // Ring counts as not draining below this power/s; the shield alone costs 0.08
    private static final float MIN_DRAIN = 0.02F;
    private static final float RISE_RATE = 9.0F;
    private static final long TRAIL_WAIT_MS = 450L;
    private static final float TRAIL_RATE = 5.0F;
    private static final float STRIPE_GAP = 3.0F;
    private static final float STRIPE_WIDTH = 1.2F;

    private static final int BAR_UP = 72;
    private static final int BAR_HEIGHT = 17;
    private static final int ICON_ROOM = 15;
    private static final int PADDING = 7;
    private static final float FLASH_RADIUS = 44.0F;
    private static final float HOLD_INNER = 13.0F;
    private static final float HOLD_OUTER = 17.5F;
    private static final float HOLD_SHOWN = 0.1F;
    private static final float HOLD_FLASH_MS = 350.0F;
    private static final long[] FULL_AT = new long[2];
    private static final float KEY_INNER = 21.0F;
    private static final float KEY_OUTER = 24.0F;
    private static final float KEY_KEPT_MS = 400.0F;
    private static long keyFullAt;
    private static float shown = -1.0F;
    private static float trail;
    private static long trailWaits;
    private static long lastDrawn;

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
        if (minecraft.screen instanceof ConstructWheelScreen) {
            return;
        }
        Construct held = ConstructChoice.held();
        renderFlash(graphics, ConstructChoice.since());
        renderHold(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        renderKeyHold(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        if (held != Construct.NONE) {
            renderBar(graphics, minecraft.font, held);
        }
    }

    private static void renderHold(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float middleX = graphics.guiWidth() * 0.5F;
        float middleY = graphics.guiHeight() * 0.5F;
        long now = Util.getMillis();
        List<Runnable> labels = new ArrayList<>();
        boolean drawn = false;
        for (CharacterAbility ability : GameCharacter.GREEN_LANTERN.abilities()) {
            // Sword and shield block the instant it's held; there's no fill to show
            if (ability.mouseButton() == CharacterAbility.Mouse.NONE
                    || SwordArms.holding() && ability.mouseButton() == CharacterAbility.Mouse.RIGHT) {
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
                float flash = (now - FULL_AT[button]) / HOLD_FLASH_MS;
                if (flash < 1.0F) {
                    float out = HOLD_OUTER + 8.0F * flash;
                    GuiShapes.arc(graphics, middleX, middleY, out - 2.0F, out, Math.min(from, end), Math.max(from, end),
                            GuiShapes.fade(0xE6FFEC, 1.0F - flash));
                }
            } else {
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

    // A key held for its hold version fills a whole ring round the crosshair, outside the two button arcs.
    private static void renderKeyHold(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = Util.getMillis();
        for (CharacterAbility ability : GameCharacter.GREEN_LANTERN.abilities()) {
            if (ability.mouseButton() != CharacterAbility.Mouse.NONE || ability.isHeld() || ability.holdTicks() <= 0) {
                continue;
            }
            float progress = ClientCharacter.keyHoldProgress(ability, partialTick);
            if (progress < 1.0F) {
                keyFullAt = 0L;
            } else if (keyFullAt == 0L) {
                keyFullAt = now;
            }
            float gone = keyFullAt == 0L ? 0.0F : Mth.clamp((now - keyFullAt - KEY_KEPT_MS) / HOLD_FLASH_MS, 0.0F, 1.0F);
            if (progress < HOLD_SHOWN || gone >= 1.0F) {
                continue;
            }
            float middleX = graphics.guiWidth() * 0.5F;
            float middleY = graphics.guiHeight() * 0.5F;
            float appear = Mth.clamp((progress - HOLD_SHOWN) * 8.0F, 0.0F, 1.0F) * (1.0F - gone);
            float filling = Mth.clamp((progress - HOLD_SHOWN) / (1.0F - HOLD_SHOWN), 0.0F, 1.0F);
            GuiShapes.arc(graphics, middleX, middleY, KEY_INNER - 1.0F, KEY_OUTER + 1.0F, 0.0F, 360.0F,
                    GuiShapes.fade(0x000000, 0.45F * appear));
            GuiShapes.arc(graphics, middleX, middleY, KEY_INNER, KEY_OUTER, 0.0F, 360.0F,
                    GuiShapes.fade(0x0E3A1E, 0.8F * appear));
            float end = 360.0F * filling;
            if (progress >= 1.0F) {
                GuiShapes.arc(graphics, middleX, middleY, KEY_INNER - 0.5F, KEY_OUTER + 0.5F, 0.0F, 360.0F,
                        GuiShapes.fade(BRIGHT, appear));
                float flash = (now - keyFullAt) / HOLD_FLASH_MS;
                if (flash < 1.0F) {
                    float out = KEY_OUTER + 10.0F * flash;
                    GuiShapes.arc(graphics, middleX, middleY, out - 2.0F, out, 0.0F, 360.0F,
                            GuiShapes.fade(0xE6FFEC, 1.0F - flash));
                }
            } else {
                GuiShapes.arc(graphics, middleX, middleY, KEY_INNER, KEY_OUTER, 0.0F, end,
                        GuiShapes.fade(GuiShapes.mix(GREEN, BRIGHT, filling), 0.95F * appear));
                GuiShapes.arc(graphics, middleX, middleY, KEY_INNER - 1.0F, KEY_OUTER + 1.0F, Math.max(0.0F,
                        end - 4.0F), end, GuiShapes.fade(0xE6FFEC, appear));
            }
            GuiShapes.flush(graphics);
            Component name = Component.translatable("screen." + MultiversePowers.MODID + ".hold." + ability.id());
            int width = minecraft.font.width(name);
            int color = GuiShapes.mix(GREEN, progress >= 1.0F ? 0xFFFFFF : BRIGHT, filling);
            graphics.drawString(minecraft.font, name, Mth.floor(middleX - width * 0.5F),
                    Mth.floor(middleY + KEY_OUTER + 4.0F), (int) (255 * appear) << 24 | color);
        }
    }

    private static Component holdName(CharacterAbility ability, @Nullable Player player) {
        String prefix = "screen." + MultiversePowers.MODID + ".hold.";
        if (SwordArms.holding()) {
            return Component.translatable(prefix + "flurry");
        }
        if (FlameArms.holding()) {
            return Component.translatable(prefix + (ability.mouseButton() == CharacterAbility.Mouse.LEFT ? "inferno"
                    : "vortex"));
        }
        if (WhipArms.holding()) {
            return Component.translatable(prefix + (ability.mouseButton() == CharacterAbility.Mouse.LEFT ? "whirlwind"
                    : "spinning_shield"));
        }
        if (ability.mouseButton() == CharacterAbility.Mouse.LEFT) {
            return Component.translatable(prefix + "beam");
        }
        boolean flying = player != null && ClientRing.flight(player, 0.0F) >= 0.0F;
        return Component.translatable(prefix + (flying ? "brake" : "dome"));
    }

    @Nullable
    public static Component status(CharacterAbility ability, Player player) {
        String prefix = "screen." + MultiversePowers.MODID + ".character.";
        if (SwordArms.holding() && ability.mouseButton() != CharacterAbility.Mouse.NONE) {
            return Component.translatable(prefix + (ability.mouseButton() == CharacterAbility.Mouse.LEFT ? "sword"
                    : "shield"));
        }
        if (FlameArms.holding() && ability.mouseButton() != CharacterAbility.Mouse.NONE) {
            return Component.translatable(prefix + (ability.mouseButton() == CharacterAbility.Mouse.LEFT ? "flames"
                    : "fire_wall"));
        }
        if (WhipArms.holding() && ability.mouseButton() != CharacterAbility.Mouse.NONE) {
            return Component.translatable(prefix + (ability.mouseButton() == CharacterAbility.Mouse.LEFT ? "whip"
                    : "lasso"));
        }
        return switch (ability.id()) {
            case "light_bolt" -> ClientRing.has(player, RingPayload.BEAM)
                    ? Component.translatable(prefix + "beam") : null;
            case "light_shield" -> ClientRing.has(player, RingPayload.DOME)
                    ? Component.translatable(prefix + "dome")
                    : ClientRing.has(player, RingPayload.SHIELD) ? Component.translatable(prefix + "on") : null;
            case "shockwave" -> ClientRing.has(player, RingPayload.DIVE)
                    ? Component.translatable(prefix + "diving") : null;
            case "flight" -> ClientRing.has(player, RingPayload.DESCENT) ? Component.translatable(prefix + "sinking")
                    : ClientRing.flight(player, 0.0F) >= 0.0F ? Component.translatable(prefix + "flying") : null;
            case "light_bubble" -> ClientConstructs.bubbleAge(player.getId(), 0.0F) >= 0.0F
                    ? Component.translatable(prefix + "trapped") : null;
            default -> null;
        };
    }

    private static void renderRechargeFlash(GuiGraphics graphics, float flash) {
        if (flash > 0.0F) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), GuiShapes.fade(FLASH, 0.7F * flash));
        }
    }

    public static void renderPower(GuiGraphics graphics, Font font, Player player, int left, int right, int y,
            float lowAt) {
        float power = ClientRing.power(player);
        float pending = Math.min(ClientRing.pending(player), power);
        boolean low = power + 1.0E-4F < lowAt;
        boolean flying = ClientRing.flight(player, 0.0F) >= 0.0F;
        boolean steady = flying || ClientRing.has(player, RingPayload.SHIELD)
                || ClientRing.has(player, RingPayload.DOME) || ClientRing.has(player, RingPayload.BEAM);
        float drain = steady ? ClientRing.drain() : 0.0F;
        String number = String.format(Locale.ROOT, "%.0f", power);
        String goes = "";
        int goesColor = PENDING;
        String time = "";
        if (pending > 0.0F) {
            goes = String.format(Locale.ROOT, " -%.1f", pending);
        } else if (drain >= MIN_DRAIN) {
            goes = " -" + Unit.number(drain) + "/s";
            goesColor = 0xFF000000 | PAID;
            if (flying) {
                time = " " + (int) Math.ceil(power / drain) + "s";
            }
        }
        Component label = Component.translatable("ring." + MultiversePowers.MODID + ".label");
        int labelWidth = font.width(label);
        int amountWidth = font.width(number) + font.width(goes) + font.width(time);
        graphics.drawString(font, label, left, y, MUTED, false);
        int x = right - amountWidth;
        x = graphics.drawString(font, number, x, y, low ? LOW : TEXT, false);
        x = graphics.drawString(font, goes, x, y, goesColor, false);
        graphics.drawString(font, time, x, y, MUTED, false);

        float barLeft = left + labelWidth + 5.0F;
        float barWidth = right - amountWidth - 5.0F - barLeft;
        if (barWidth < 8.0F) {
            return;
        }
        follow(power);
        float barTop = y + (font.lineHeight - 1 - BAR_THICK) * 0.5F;
        float radius = BAR_THICK * 0.5F;
        float nowX = barLeft + barWidth * Mth.clamp(shown / PowerRing.MAX_POWER, 0.0F, 1.0F);
        float trailX = barLeft + barWidth * Mth.clamp(trail / PowerRing.MAX_POWER, 0.0F, 1.0F);
        float afterX = Math.max(barLeft, nowX - barWidth * pending / PowerRing.MAX_POWER);
        GuiShapes.roundRect(graphics, barLeft - 1.0F, barTop - 1.0F, barWidth + 2.0F, BAR_THICK + 2.0F, radius + 1.0F,
                GuiShapes.fade(0x000000, 0.45F));
        GuiShapes.roundRect(graphics, barLeft, barTop, barWidth, BAR_THICK, radius, GuiShapes.fade(0x0B2E18, 0.95F));
        if (trailX - nowX > 0.3F) {
            GuiShapes.roundRect(graphics, barLeft, barTop, trailX - barLeft, BAR_THICK, radius,
                    GuiShapes.fade(PAID, 0.9F));
        }
        boolean costs = nowX - afterX > 0.3F;
        if (costs) {
            GuiShapes.roundRect(graphics, barLeft, barTop, nowX - barLeft, BAR_THICK, radius,
                    GuiShapes.fade(BRIGHT, 0.95F));
        }
        if (afterX - barLeft > 0.3F) {
            GuiShapes.roundRect(graphics, barLeft, barTop, afterX - barLeft, BAR_THICK, radius,
                    GuiShapes.fade(low ? 0xFF5A3A : GREEN, 1.0F));
            GuiShapes.roundRect(graphics, barLeft + 1.0F, barTop + 0.6F, Math.max(0.0F, afterX - barLeft - 2.0F),
                    BAR_THICK * 0.3F, BAR_THICK * 0.15F, GuiShapes.fade(0xFFFFFF, 0.25F));
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            float markX = barLeft + barWidth * quarter / 4.0F;
            GuiShapes.quad(graphics, markX - 0.25F, barTop + 1.0F, markX + 0.25F, barTop + 1.0F, markX + 0.25F,
                    barTop + BAR_THICK - 1.0F, markX - 0.25F, barTop + BAR_THICK - 1.0F, GuiShapes.fade(0x000000, 0.3F));
        }
        if (costs) {
            stripes(graphics, afterX, barTop, nowX - radius * 0.5F);
            GuiShapes.quad(graphics, afterX - 0.5F, barTop - 1.0F, afterX + 0.5F, barTop - 1.0F, afterX + 0.5F,
                    barTop + BAR_THICK + 1.0F, afterX - 0.5F, barTop + BAR_THICK + 1.0F, GuiShapes.fade(0xFFFFFF, 0.95F));
        }
        GuiShapes.flush(graphics);
    }

    private static void follow(float power) {
        long now = Util.getMillis();
        float seconds = Math.min(0.25F, (now - lastDrawn) / 1000.0F);
        if (shown < 0.0F || now - lastDrawn > 1000L) {
            shown = power;
            trail = power;
            seconds = 0.0F;
        }
        lastDrawn = now;
        if (power < shown) {
            // Only restart the trail's wait once it has fully caught up and stopped
            if (trail - shown < 0.05F) {
                trailWaits = now + TRAIL_WAIT_MS;
            }
            trail = Math.max(trail, shown);
            shown = power;
        } else {
            shown += (power - shown) * (1.0F - (float) Math.exp(-RISE_RATE * seconds));
        }
        if (now >= trailWaits) {
            trail += (shown - trail) * (1.0F - (float) Math.exp(-TRAIL_RATE * seconds));
        }
        trail = Math.max(trail, shown);
    }

    private static void stripes(GuiGraphics graphics, float from, float top, float to) {
        if (to - from < 0.5F) {
            return;
        }
        GuiShapes.flush(graphics);
        graphics.enableScissor(Mth.floor(from), Mth.floor(top), Mth.ceil(to), Mth.ceil(top + BAR_THICK));
        int color = GuiShapes.fade(GREEN, 0.55F);
        for (float x = from - BAR_THICK; x < to; x += STRIPE_GAP) {
            GuiShapes.quad(graphics, x, top + BAR_THICK, x + STRIPE_WIDTH, top + BAR_THICK,
                    x + STRIPE_WIDTH + BAR_THICK, top, x + BAR_THICK, top, color);
        }
        GuiShapes.flush(graphics);
        graphics.disableScissor();
    }

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

    private static void renderBar(GuiGraphics graphics, Font font, Construct held) {
        MutableComponent name = held.getDisplayName().copy();
        Component description = held.getDescription();
        MutableComponent about = description == null ? null : description.copy().withStyle(ChatFormatting.ITALIC);
        int nameWidth = font.width(name);
        int aboutWidth = about == null ? 0 : 6 + font.width(about);
        int width = PADDING + ICON_ROOM + 5 + nameWidth + aboutWidth + PADDING;

        float left = (graphics.guiWidth() - width) * 0.5F;
        float top = graphics.guiHeight() - BAR_UP - BAR_HEIGHT * 0.5F;
        GuiShapes.roundRect(graphics, left, top, width, BAR_HEIGHT, BAR_HEIGHT * 0.5F,
                GuiShapes.fade(0x04140A, 0.62F));
        GuiShapes.arc(graphics, left + BAR_HEIGHT * 0.5F, top + BAR_HEIGHT * 0.5F,
                BAR_HEIGHT * 0.5F - 1.0F, BAR_HEIGHT * 0.5F, 90.0F, 270.0F, GuiShapes.fade(GREEN, 0.5F));
        float iconX = left + PADDING + ICON_ROOM * 0.5F;
        float iconY = top + BAR_HEIGHT * 0.5F;
        ConstructIcons.draw(graphics, held, iconX, iconY, ICON_ROOM * 1.35F, 0.0F);
        GuiShapes.flush(graphics);

        int textX = Mth.floor(left) + PADDING + ICON_ROOM + 5;
        int textY = Mth.floor(top) + (BAR_HEIGHT - font.lineHeight) / 2 + 1;
        graphics.drawString(font, name, textX, textY, TEXT, false);
        if (about != null) {
            graphics.drawString(font, about, textX + nameWidth + 6, textY, MUTED, false);
        }
    }
}
