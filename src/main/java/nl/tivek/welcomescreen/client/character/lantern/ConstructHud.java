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
import nl.tivek.welcomescreen.config.Unit;
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
    /** What the ring pays: the trail it leaves on the bar, and how fast it drains in the text. */
    private static final int PAID = 0xF2D98A;
    /** The light of the lantern flashing over your screen. */
    private static final int FLASH = 0xE8FFEE;
    /** How thick the ring's power bar is. */
    private static final float BAR_THICK = 6.0F;
    // Below this, in power a second, the ring counts as not draining: the cheapest drain, the shield, is 0.08.
    private static final float MIN_DRAIN = 0.02F;
    // How the bar moves: it drops at once when the ring pays and glides up as it fills (this fast, per second);
    // what was just paid stays behind it as a gold trail this long, then runs out (this fast).
    private static final float RISE_RATE = 9.0F;
    private static final long TRAIL_WAIT_MS = 450L;
    private static final float TRAIL_RATE = 5.0F;
    // The stripes over the part the fist you charge will take: how far apart, and how wide.
    private static final float STRIPE_GAP = 3.0F;
    private static final float STRIPE_WIDTH = 1.2F;

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
    // The power bar as drawn, in power: where it is, where its trail is and until when that trail waits, and when
    // the bar was drawn last.
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
            // With the sword and shield the defend button blocks the moment it is held: there is nothing to fill up.
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

    /**
     * What holding this button leads to: the laser, the dome, or in the air the brake; with the sword and shield the
     * flurry of stabs.
     */
    private static Component holdName(CharacterAbility ability, @Nullable Player player) {
        String prefix = "screen." + WelcomeScreenMod.MODID + ".hold.";
        if (SwordArms.holding()) {
            return Component.translatable(prefix + "flurry");
        }
        if (ability.mouseButton() == CharacterAbility.Mouse.LEFT) {
            return Component.translatable(prefix + "beam");
        }
        boolean flying = player != null && ClientRing.flight(player, 0.0F) >= 0.0F;
        return Component.translatable(prefix + (flying ? "brake" : "dome"));
    }

    /**
     * What an ability of Green Lantern is doing right now, for the panel with your abilities, or null to let the
     * panel say it itself: the shield is on, the dome or the beam is up, you go down to a slam, you fly or sink.
     */
    @Nullable
    public static Component status(CharacterAbility ability, Player player) {
        String prefix = "screen." + WelcomeScreenMod.MODID + ".character.";
        // With the sword and shield of the construct wheel in his hands the mouse is theirs.
        if (SwordArms.holding() && ability.mouseButton() != CharacterAbility.Mouse.NONE) {
            return Component.translatable(prefix + (ability.mouseButton() == CharacterAbility.Mouse.LEFT ? "sword"
                    : "shield"));
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

    /** The light of the lantern when your fist hits it, over your whole screen for a moment. */
    private static void renderRechargeFlash(GuiGraphics graphics, float flash) {
        if (flash > 0.0F) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), GuiShapes.fade(FLASH, 0.7F * flash));
        }
    }

    /**
     * The ring's power, as the bottom line of the panel with your abilities. Nothing on it blinks:
     * <ul>
     * <li>the bar drops at once when the ring pays and glides up as it fills, and what it just paid stays behind it
     * for a moment as a gold trail that then runs out, so every cost shows how big it was;</li>
     * <li>while you charge a fist, the part letting go will cost is a steady striped piece at the end of the bar, with
     * a line where the ring will end up;</li>
     * <li>the text says how much is left, what the fist will cost, or how fast the ring drains by itself (flying, the
     * shield, the dome, the beam) and in the air how many seconds that leaves;</li>
     * <li>red once it cannot pay for the smallest fist.</li>
     * </ul>
     *
     * @param lowAt below this much power the ring is shown as too low
     */
    public static void renderPower(GuiGraphics graphics, Font font, Player player, int left, int right, int y,
            float lowAt) {
        float power = ClientRing.power(player);
        float pending = Math.min(ClientRing.pending(player), power);
        boolean low = power + 1.0E-4F < lowAt;
        boolean flying = ClientRing.flight(player, 0.0F) >= 0.0F;
        boolean steady = flying || ClientRing.has(player, RingPayload.SHIELD)
                || ClientRing.has(player, RingPayload.DOME) || ClientRing.has(player, RingPayload.BEAM);
        float drain = steady ? ClientRing.drain() : 0.0F;
        // The text in up to three parts, each in its own colour: what is left, what goes off, and the time left.
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
        Component label = Component.translatable("ring." + WelcomeScreenMod.MODID + ".label");
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
        // The track with a thin rim, then from the back to the front: the trail of what was just paid, the part the
        // fist will take, and what is left after that. Each lies over the start of the one behind it.
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
            // A lighter top edge, so the bar reads as a tube of light.
            GuiShapes.roundRect(graphics, barLeft + 1.0F, barTop + 0.6F, Math.max(0.0F, afterX - barLeft - 2.0F),
                    BAR_THICK * 0.3F, BAR_THICK * 0.15F, GuiShapes.fade(0xFFFFFF, 0.25F));
        }
        // Marks at a quarter, half and three quarters of a full ring.
        for (int quarter = 1; quarter < 4; quarter++) {
            float markX = barLeft + barWidth * quarter / 4.0F;
            GuiShapes.quad(graphics, markX - 0.25F, barTop + 1.0F, markX + 0.25F, barTop + 1.0F, markX + 0.25F,
                    barTop + BAR_THICK - 1.0F, markX - 0.25F, barTop + BAR_THICK - 1.0F, GuiShapes.fade(0x000000, 0.3F));
        }
        if (costs) {
            stripes(graphics, afterX, barTop, nowX - radius * 0.5F);
            // Where the ring will end up once the fist flies.
            GuiShapes.quad(graphics, afterX - 0.5F, barTop - 1.0F, afterX + 0.5F, barTop - 1.0F, afterX + 0.5F,
                    barTop + BAR_THICK + 1.0F, afterX - 0.5F, barTop + BAR_THICK + 1.0F, GuiShapes.fade(0xFFFFFF, 0.95F));
        }
        GuiShapes.flush(graphics);
    }

    /**
     * Moves the bar as drawn one frame towards what the ring holds: down at once, up with a glide, and the trail
     * of what was just paid runs out after a moment. After a while without the bar it starts over where the ring is.
     */
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
            // A trail that had run out starts to wait again; one that is still there keeps running out.
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

    /** Slanted stripes over the part of the bar from {@code from} to {@code to}: the part the fist will take. */
    private static void stripes(GuiGraphics graphics, float from, float top, float to) {
        if (to - from < 0.5F) {
            return;
        }
        // Only inside that part: the stripes run on past both ends and are cut off there.
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

    /**
     * The bar itself: the picture of what you hold (see {@link ConstructIcons}), its name, and what it does when it has
     * a line for that.
     */
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
        // The picture of what you hold, a little bigger than the bar so it stands out of it.
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
