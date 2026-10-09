package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
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
import nl.tivek.multiversepowers.character.client.KeyCap;
import nl.tivek.multiversepowers.character.client.MouseHold;
import nl.tivek.multiversepowers.character.greenlantern.PowerRing;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.character.greenlantern.client.body.flame.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.pose.RechargeAnimation;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.whip.WhipArms;
import nl.tivek.multiversepowers.character.greenlantern.mech.MechAttacks;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.HudSpace;

@Mod(value = MultiversePowers.MODID, dist = Dist.CLIENT)
public final class ConstructHud {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "lantern_construct");
    private static final int GREEN = 0x3CE86A;
    private static final int BRIGHT = 0xCFFFDC;
    private static final int TEXT = 0xFFE8FFEE;
    private static final int MUTED = 0xFF8FA898;
    private static final int LOW = 0xFFFF5A3A;
    private static final int PAID = 0xF2D98A;
    private static final int FLASH = 0xE8FFEE;
    private static final int BAR = 6;
    // Ring counts as not draining below this power/s; the shield alone costs 0.08
    private static final float MIN_DRAIN = 0.05F;
    private static final float DRAIN_RATE = 3.0F;
    private static final int BAR_LEAST = 40;
    private static final String MOST_POWER = "100";
    private static final String MOST_DRAIN = "-99.9/s";
    private static final float RISE_RATE = 9.0F;
    private static final long TRAIL_WAIT_MS = 450L;
    private static final float TRAIL_RATE = 5.0F;

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
    private static float shownDrain;
    private static long trailWaits;
    private static long lastDrawn;

    public ConstructHud(IEventBus modEventBus) {
        modEventBus.addListener(ConstructHud::onRegisterLayers);
        LanternPanel.register();
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
        renderFlash(graphics, ConstructChoice.since());
        renderHold(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
        renderKeyHold(graphics, deltaTracker.getGameTimeDeltaPartialTick(false));
    }

    private static void renderHold(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        float middleX = graphics.guiWidth() * 0.5F;
        float middleY = graphics.guiHeight() * 0.5F;
        long now = Util.getMillis();
        List<Runnable> labels = new ArrayList<>();
        boolean flame = FlameArms.holding();
        boolean beam = !flame && !SwordArms.holding() && !WhipArms.holding();
        // The gauge first: its band and its words keep their place, and the rings and words after it keep off them.
        boolean drawn = flame && HeatGauge.render(graphics, middleX, middleY, partialTick, labels);
        CharacterAbility bolt = GameCharacter.GREEN_LANTERN.byName("light_bolt");
        if (beam && bolt != null) {
            drawn |= BeamGauge.render(graphics, minecraft.player, bolt, middleX, middleY, partialTick, labels);
        }
        for (CharacterAbility ability : GameCharacter.GREEN_LANTERN.abilities()) {
            // Sword and shield block the instant it's held; there's no fill to show
            if (ability.input() != CharacterAbility.Input.LEFT && ability.input() != CharacterAbility.Input.RIGHT
                    || SwordArms.holding() && ability.input() == CharacterAbility.Input.RIGHT
                    || beam && ability == bolt) {
                continue;
            }
            boolean right = ability.input() == CharacterAbility.Input.LEFT;
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
            boolean full = progress >= 1.0F;
            // A flamethrower move takes over once its hold is full: the ring flashes and goes.
            float gone = flame && full ? Mth.clamp((now - FULL_AT[button]) / HOLD_FLASH_MS, 0.0F, 1.0F) : 0.0F;
            if (gone >= 1.0F) {
                continue;
            }
            float filling = Mth.clamp((progress - HOLD_SHOWN) / (1.0F - HOLD_SHOWN), 0.0F, 1.0F);
            float from = right ? 30.0F : 330.0F;
            float span = right ? 120.0F : -120.0F;
            float appear = Mth.clamp((progress - HOLD_SHOWN) * 8.0F, 0.0F, 1.0F) * (1.0F - gone);
            float low = Math.min(from, from + span);
            float high = Math.max(from, from + span);
            float inner = HudSpace.ring(middleX, middleY, HOLD_INNER - 1.0F, HOLD_OUTER - HOLD_INNER + 2.0F, low, high)
                    + 1.0F;
            float outer = inner + HOLD_OUTER - HOLD_INNER;
            GuiShapes.arc(graphics, middleX, middleY, inner - 1.0F, outer + 1.0F, low, high,
                    GuiShapes.fade(0x000000, 0.45F * appear));
            GuiShapes.arc(graphics, middleX, middleY, inner, outer, low, high, GuiShapes.fade(0x0E3A1E, 0.8F * appear));
            float end = from + span * filling;
            if (full) {
                float throb = 0.75F + 0.25F * Mth.sin((now % 100000L) / 90.0F);
                GuiShapes.arc(graphics, middleX, middleY, inner - 0.5F, outer + 0.5F, Math.min(from, end),
                        Math.max(from, end), GuiShapes.fade(BRIGHT, throb * appear));
                float flash = (now - FULL_AT[button]) / HOLD_FLASH_MS;
                if (flash < 1.0F) {
                    float out = outer + 8.0F * flash;
                    GuiShapes.arc(graphics, middleX, middleY, out - 2.0F, out, Math.min(from, end), Math.max(from, end),
                            GuiShapes.fade(0xE6FFEC, 1.0F - flash));
                }
            } else {
                int color = GuiShapes.mix(GREEN, BRIGHT, filling);
                GuiShapes.arc(graphics, middleX, middleY, inner, outer, Math.min(from, end),
                        Math.max(from, end), GuiShapes.fade(color, 0.95F * appear));
                GuiShapes.arc(graphics, middleX, middleY, inner - 1.0F, outer + 1.0F,
                        right ? end - 3.0F : end, right ? end : end + 3.0F, GuiShapes.fade(0xE6FFEC, appear));
            }
            Component name = holdName(ability, minecraft.player);
            float fullness = full ? 1.0F : filling;
            labels.add(() -> {
                int width = minecraft.font.width(name);
                float wanted = right ? Mth.floor(middleX + outer + 5.0F) : Mth.ceil(middleX - outer - 5.0F) - width;
                HudSpace.Box box = HudSpace.place(wanted, Mth.floor(middleY) - 4, width, 9, HudSpace.Way.UP,
                        HudSpace.Way.DOWN);
                int alpha = (int) (255 * appear);
                int color = GuiShapes.mix(GREEN, full ? 0xFFFFFF : BRIGHT, fullness);
                graphics.drawString(minecraft.font, name, Mth.floor(box.x()), Mth.floor(box.y()), alpha << 24 | color);
            });
            drawn = true;
        }
        if (drawn) {
            GuiShapes.flush(graphics);
        }
        labels.forEach(Runnable::run);
    }

    // A key or the scroll wheel held for a hold of its own fills a whole ring round the crosshair, outside the two
    // button arcs.
    private static void renderKeyHold(GuiGraphics graphics, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        long now = Util.getMillis();
        CharacterAbility ability = null;
        float progress = -1.0F;
        for (CharacterAbility each : GameCharacter.GREEN_LANTERN.abilities()) {
            float held = each.input() == CharacterAbility.Input.KEY && !each.isHeld() && each.holdTicks() > 0
                    ? ClientCharacter.keyHoldProgress(each, partialTick)
                    : each.input() == CharacterAbility.Input.SCROLL && each.tapWhen() == CharacterAbility.Tap.NEVER
                            ? MouseHold.progress(each, partialTick) : -1.0F;
            if (held > progress) {
                ability = each;
                progress = held;
            }
        }
        if (progress < 1.0F) {
            keyFullAt = 0L;
        } else if (keyFullAt == 0L) {
            keyFullAt = now;
        }
        float gone = keyFullAt == 0L ? 0.0F : Mth.clamp((now - keyFullAt - KEY_KEPT_MS) / HOLD_FLASH_MS, 0.0F, 1.0F);
        if (ability != null && progress >= HOLD_SHOWN && gone < 1.0F) {
            float middleX = graphics.guiWidth() * 0.5F;
            float middleY = graphics.guiHeight() * 0.5F;
            float appear = Mth.clamp((progress - HOLD_SHOWN) * 8.0F, 0.0F, 1.0F) * (1.0F - gone);
            float filling = Mth.clamp((progress - HOLD_SHOWN) / (1.0F - HOLD_SHOWN), 0.0F, 1.0F);
            // Out past the button arcs and any gauge drawn this frame.
            float inner = HudSpace.ring(middleX, middleY, KEY_INNER - 1.0F, KEY_OUTER - KEY_INNER + 2.0F, 0.0F, 360.0F)
                    + 1.0F;
            float outer = inner + KEY_OUTER - KEY_INNER;
            GuiShapes.arc(graphics, middleX, middleY, inner - 1.0F, outer + 1.0F, 0.0F, 360.0F,
                    GuiShapes.fade(0x000000, 0.45F * appear));
            GuiShapes.arc(graphics, middleX, middleY, inner, outer, 0.0F, 360.0F,
                    GuiShapes.fade(0x0E3A1E, 0.8F * appear));
            float end = 360.0F * filling;
            if (progress >= 1.0F) {
                GuiShapes.arc(graphics, middleX, middleY, inner - 0.5F, outer + 0.5F, 0.0F, 360.0F,
                        GuiShapes.fade(BRIGHT, appear));
                float flash = (now - keyFullAt) / HOLD_FLASH_MS;
                if (flash < 1.0F) {
                    float out = outer + 10.0F * flash;
                    GuiShapes.arc(graphics, middleX, middleY, out - 2.0F, out, 0.0F, 360.0F,
                            GuiShapes.fade(0xE6FFEC, 1.0F - flash));
                }
            } else {
                GuiShapes.arc(graphics, middleX, middleY, inner, outer, 0.0F, end,
                        GuiShapes.fade(GuiShapes.mix(GREEN, BRIGHT, filling), 0.95F * appear));
                GuiShapes.arc(graphics, middleX, middleY, inner - 1.0F, outer + 1.0F, Math.max(0.0F,
                        end - 4.0F), end, GuiShapes.fade(0xE6FFEC, appear));
            }
            GuiShapes.flush(graphics);
            String key = "screen." + MultiversePowers.MODID + ".hold." + ability.id();
            boolean leaves = ability.id().equals("mech") && minecraft.player != null
                    && ClientConstructs.piloted(minecraft.player.getId(), partialTick) != null;
            Component name = Component.translatable(leaves ? key + ".leave" : key);
            int width = minecraft.font.width(name);
            int color = GuiShapes.mix(GREEN, progress >= 1.0F ? 0xFFFFFF : BRIGHT, filling);
            HudSpace.Box box = HudSpace.place(Mth.floor(middleX - width * 0.5F), Mth.floor(middleY + outer + 4.0F),
                    width, 9, HudSpace.Way.DOWN);
            graphics.drawString(minecraft.font, name, Mth.floor(box.x()), Mth.floor(box.y()),
                    (int) (255 * appear) << 24 | color);
        }
    }

    static Component holdName(CharacterAbility ability, @Nullable Player player) {
        String prefix = "screen." + MultiversePowers.MODID + ".hold.";
        if (SwordArms.holding()) {
            return Component.translatable(prefix + "flurry");
        }
        if (WhipArms.holding()) {
            return Component.translatable(prefix + (ability.input() == CharacterAbility.Input.LEFT ? "whirlwind"
                    : "spinning_shield"));
        }
        if (FlameArms.holding()) {
            return Component.translatable("screen." + MultiversePowers.MODID + ".move.flamethrower."
                    + (ability.input() == CharacterAbility.Input.LEFT ? "attack_hold" : "defend_hold"));
        }
        if (ability.input() == CharacterAbility.Input.LEFT) {
            return Component.translatable(prefix + "beam");
        }
        if (player instanceof LocalPlayer local && LanternPanel.piloting(local)) {
            return Component.translatable("screen." + MultiversePowers.MODID + ".panel.green_lantern.mech.glare");
        }
        boolean flying = player != null && ClientRing.flight(player, 0.0F) >= 0.0F;
        return Component.translatable(prefix + (flying ? "brake" : "dome"));
    }

    @Nullable
    static Component status(CharacterAbility ability, Player player) {
        String prefix = "screen." + MultiversePowers.MODID + ".character.";
        if (player instanceof LocalPlayer local && !ability.id().equals("mech") && LanternPanel.piloting(local)) {
            return mechStatus(ability, LanternPanel.mechMove(local), prefix);
        }
        return switch (ability.id()) {
            case "shockwave" -> ClientRing.has(player, RingPayload.DIVE)
                    ? Component.translatable(prefix + "diving") : null;
            case "flight" -> ClientRing.has(player, RingPayload.DESCENT) ? Component.translatable(prefix + "sinking")
                    : ClientRing.flight(player, 0.0F) >= 0.0F ? Component.translatable(prefix + "flying") : null;
            case "light_bubble" -> ClientConstructs.bubbleAge(player.getId(), 0.0F) >= 0.0F
                    ? Component.translatable(prefix + "trapped") : null;
            case "beam_lock" -> ClientRing.has(player, RingPayload.BEAM)
                    && LightBeam.lockedIn(ClientConstructs.beamClock(player.getId()))
                    ? Component.translatable(prefix + "locked") : null;
            case "mech" -> mechLeft(ability, player, prefix);
            default -> null;
        };
    }

    // In the mech, what its own move on that key does now: the salvos the missile arm has left, flying, spinning,
    // diving.
    @Nullable
    private static Component mechStatus(CharacterAbility ability, MechAttacks.Blow move, String prefix) {
        return switch (ability.id()) {
            case "emerald_express" -> move.kind() == MechAttacks.AIM ? Component.translatable(prefix + "salvos_left",
                    MechAttacks.SALVOS - MechAttacks.fired(move)) : null;
            case "flight" -> move.kind() == MechAttacks.FLY && MechAttacks.airborne(move)
                    ? Component.translatable(prefix + "flying") : null;
            case "shockwave" -> move.kind() == MechAttacks.SPIN ? Component.translatable(prefix + "spinning")
                    : move.kind() == MechAttacks.DIVE ? Component.translatable(prefix + "diving") : null;
            default -> null;
        };
    }

    // How long the pilot may still stay in their mech, in minutes and seconds.
    @Nullable
    private static Component mechLeft(CharacterAbility ability, Player player, String prefix) {
        ClientConstructs.Piloted pilot = ClientConstructs.piloted(player.getId(), 0.0F);
        if (pilot == null || pilot.broke() >= 0.0) {
            return null;
        }
        int left = Math.max(0, (int) Math.ceil((ability.intValue("mechTime") - pilot.t()) / 20.0));
        return Component.translatable(prefix + "mech_left",
                String.format(Locale.ROOT, "%d:%02d", left / 60, left % 60));
    }

    private static void renderRechargeFlash(GuiGraphics graphics, float flash) {
        if (flash > 0.0F) {
            graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), GuiShapes.fade(FLASH, 0.7F * flash));
        }
    }

    // The narrowest the ring's line can be: label, a short bar, the power left and room for what it drains.
    public static int powerWidth(Font font) {
        return font.width(ringLabel()) + 4 + BAR_LEAST + 4 + font.width(MOST_POWER) + 3 + font.width(MOST_DRAIN);
    }

    private static Component ringLabel() {
        return Component.translatable("ring." + MultiversePowers.MODID + ".label");
    }

    // The ring's power in one line whose parts each keep their own place: the bar never resizes as the numbers change,
    // and what the ring drains a second shows, eased and to one decimal, in a room kept for it.
    public static void renderPower(GuiGraphics graphics, Font font, Player player, int left, int right, int y,
            float lowAt) {
        float power = ClientRing.power(player);
        boolean low = power + 1.0E-4F < lowAt;
        boolean steady = ClientRing.flight(player, 0.0F) >= 0.0F || ClientRing.has(player, RingPayload.SHIELD)
                || ClientRing.has(player, RingPayload.DOME) || ClientRing.has(player, RingPayload.BEAM);
        follow(power, steady ? ClientRing.drain() : 0.0F);
        Component label = ringLabel();
        int drainX = right - font.width(MOST_DRAIN);
        int numberRight = drainX - 3;
        String number = String.format(Locale.ROOT, "%.0f", power);
        graphics.drawString(font, label, left, y, MUTED, false);
        graphics.drawString(font, number, numberRight - font.width(number), y, low ? LOW : TEXT, false);
        if (steady && shownDrain >= MIN_DRAIN) {
            graphics.drawString(font, String.format(Locale.ROOT, "-%.1f/s", shownDrain), drainX, y,
                    0xFF000000 | PAID, false);
        }

        int barLeft = left + font.width(label) + 4;
        int barWidth = numberRight - font.width(MOST_POWER) - 4 - barLeft;
        if (barWidth < 8) {
            return;
        }
        int barTop = y + (font.lineHeight - 1 - BAR) / 2;
        float nowX = barLeft + barWidth * Mth.clamp(shown / PowerRing.MAX_POWER, 0.0F, 1.0F);
        float trailX = barLeft + barWidth * Mth.clamp(trail / PowerRing.MAX_POWER, 0.0F, 1.0F);
        KeyCap.pill(graphics, barLeft - 1, barTop - 1, barWidth + 2, BAR + 2, 0xC0000000);
        GuiShapes.roundRect(graphics, barLeft, barTop, barWidth, BAR, 0.0F, 0xFF0B2E18);
        if (trailX - nowX > 0.3F) {
            GuiShapes.roundRect(graphics, barLeft, barTop, trailX - barLeft, BAR, 0.0F,
                    GuiShapes.fade(PAID, 0.9F));
        }
        if (nowX - barLeft > 0.3F) {
            GuiShapes.roundRect(graphics, barLeft, barTop, nowX - barLeft, BAR, 0.0F,
                    GuiShapes.fade(low ? 0xFF5A3A : GREEN, 1.0F));
            GuiShapes.roundRect(graphics, barLeft, barTop, nowX - barLeft, 1.0F, 0.0F,
                    GuiShapes.fade(0xFFFFFF, 0.3F));
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            float markX = Math.round(barLeft + barWidth * quarter / 4.0F);
            GuiShapes.roundRect(graphics, markX, barTop, 1.0F, BAR, 0.0F, GuiShapes.fade(0x000000, 0.35F));
        }
        GuiShapes.flush(graphics);
    }

    private static void follow(float power, float drain) {
        long now = Util.getMillis();
        float seconds = Math.min(0.25F, (now - lastDrawn) / 1000.0F);
        if (shown < 0.0F || now - lastDrawn > 1000L) {
            shown = power;
            trail = power;
            shownDrain = drain;
            seconds = 0.0F;
        }
        shownDrain = drain <= 0.0F ? 0.0F : shownDrain + (drain - shownDrain) * (1.0F - (float) Math.exp(-DRAIN_RATE
                * seconds));
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
}
