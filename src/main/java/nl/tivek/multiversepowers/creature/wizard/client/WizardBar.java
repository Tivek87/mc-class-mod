package nl.tivek.multiversepowers.creature.wizard.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;

// The Wizard's boss bar, in place of the game's own: as wide, but framed in his phase's colour, its fill shaded and a
// glint running along it, what a blow took lingering white a moment before it drains, notches where his next phases
// wake. Over it his name and the title of his phase; under it three gems, one lit for every phase he has reached. While
// a phase takes him the bar flashes and its frame burns gold; stunned, the frame blinks pale.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class WizardBar {
    private static final int WIDTH = 182;
    private static final int HEIGHT = 5;
    private static final int INCREMENT = 26;
    private static final int GOLD = 0xFFD27A;
    private static final long LAG_HOLD = 550L;
    private static final float LAG_DRAIN = 0.00035F;
    private static final Map<UUID, Lag> LAGS = new HashMap<>();

    private static final class Lag {
        float value;
        float last;
        long dropped;
        long tick;

        Lag(float value, long now) {
            this.value = value;
            this.last = value;
            this.tick = now;
        }
    }

    private WizardBar() {
    }

    @SubscribeEvent
    static void onBar(CustomizeGuiOverlayEvent.BossEventProgress event) {
        LerpingBossEvent bar = event.getBossEvent();
        Wizard wizard = find(bar.getId());
        if (wizard == null) {
            return;
        }
        event.setCanceled(true);
        event.setIncrement(INCREMENT);
        draw(event.getGuiGraphics(), event.getX(), event.getY(), bar, wizard);
    }

    private static Wizard find(UUID id) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || id == null) {
            return null;
        }
        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (entity instanceof Wizard wizard && id.equals(wizard.barId())) {
                return wizard;
            }
        }
        return null;
    }

    private static void draw(GuiGraphics graphics, int x, int y, LerpingBossEvent bar, Wizard wizard) {
        long now = Util.getMillis();
        float progress = Mth.clamp(bar.getProgress(), 0.0F, 1.0F);
        float lag = lag(bar.getId(), progress, now);
        int phase = wizard.phase();
        int colour = Wizard.light(phase);
        boolean changing = wizard.shielded();
        boolean stunned = wizard.move() == WizardMove.STUNNED;
        float pulse = 0.5F + 0.5F * Mth.sin(now / 70.0F);
        int frame = shade(colour, 0.45F);
        if (changing) {
            frame = mix(shade(GOLD, 0.7F), GOLD, pulse);
        } else if (stunned) {
            frame = mix(frame, 0xE8E8F0, 0.5F + 0.5F * Mth.sin(now / 120.0F));
        }
        graphics.fill(x - 3, y - 3, x + WIDTH + 3, y + HEIGHT + 3, 0xB0000000);
        graphics.fill(x - 2, y - 2, x + WIDTH + 2, y + HEIGHT + 2, 0xFF000000 | frame);
        graphics.fill(x - 1, y - 1, x + WIDTH + 1, y + HEIGHT + 1, 0xFF000000);
        graphics.fill(x, y, x + WIDTH, y + HEIGHT, 0xFF160D20);
        int filled = Math.round(WIDTH * progress);
        int lagged = Math.round(WIDTH * lag);
        if (lagged > filled) {
            graphics.fill(x + filled, y, x + lagged, y + HEIGHT, 0xFF000000 | mix(colour, 0xFFFFFF, 0.8F));
        }
        if (filled > 0) {
            graphics.fill(x, y, x + filled, y + 1, 0xFF000000 | mix(colour, 0xFFFFFF, 0.55F));
            graphics.fill(x, y + 1, x + filled, y + HEIGHT - 1, 0xFF000000 | colour);
            graphics.fill(x, y + HEIGHT - 1, x + filled, y + HEIGHT, 0xFF000000 | shade(colour, 0.55F));
            glint(graphics, x, y, filled, now);
            if (changing) {
                int alpha = (int) (170 * pulse);
                graphics.fill(x, y, x + filled, y + HEIGHT, alpha << 24 | 0xFFFFFF);
            }
        }
        for (int k = 1; k <= 2; k++) {
            int nx = x + Math.round(WIDTH * (3 - k) / 3.0F);
            boolean passed = phase > k;
            int gem = passed ? 0xFF5A5060 : 0xFF000000 | Wizard.light(k + 1);
            graphics.fill(nx - 1, y - 1, nx + 1, y + HEIGHT + 1, 0xFF000000);
            graphics.fill(nx - 1, y - 3, nx + 1, y - 1, gem);
            graphics.fill(nx - 1, y + HEIGHT + 1, nx + 1, y + HEIGHT + 3, gem);
        }
        Font font = Minecraft.getInstance().font;
        Component title = Component.translatable("boss." + MultiversePowers.MODID + ".wizard.phase." + phase)
                .withColor(mix(colour, 0xFFFFFF, 0.35F));
        Component line = bar.getName().copy().append(Component.literal("  ·  ").withColor(0x8A8094))
                .append(title);
        graphics.drawString(font, line, x + WIDTH / 2 - font.width(line) / 2, y - 12, 0xFFFFFF);
        for (int i = 0; i < 3; i++) {
            int cx = x + WIDTH / 2 + (i - 1) * 11;
            int cy = y + HEIGHT + 7;
            int lit = Wizard.light(i + 1);
            int gem;
            if (i + 1 < phase) {
                gem = lit;
            } else if (i + 1 == phase) {
                gem = mix(lit, 0xFFFFFF, 0.25F * pulse);
            } else {
                gem = 0x3A3040;
            }
            diamond(graphics, cx, cy, 3, 0xFF000000);
            diamond(graphics, cx, cy, 2, 0xFF000000 | gem);
        }
    }

    // A glint of light running along the fill every few seconds.
    private static void glint(GuiGraphics graphics, int x, int y, int filled, long now) {
        float run = (now % 2600L) / 2600.0F;
        int middle = Math.round(-20 + (WIDTH + 40) * run);
        for (int i = -10; i <= 10; i++) {
            int column = middle + i;
            if (column < 0 || column >= filled) {
                continue;
            }
            int alpha = (int) (95 * (1.0F - Math.abs(i) / 10.0F));
            graphics.fill(x + column, y, x + column + 1, y + HEIGHT - 1, alpha << 24 | 0xFFFFFF);
        }
    }

    private static void diamond(GuiGraphics graphics, int cx, int cy, int radius, int colour) {
        for (int row = -radius; row <= radius; row++) {
            int half = radius - Math.abs(row);
            graphics.fill(cx - half, cy + row, cx + half + 1, cy + row + 1, colour);
        }
    }

    // What a blow took stays white a moment, then drains down to the health that is left.
    private static float lag(UUID id, float progress, long now) {
        LAGS.values().removeIf(lag -> now - lag.tick > 10000L);
        Lag lag = LAGS.computeIfAbsent(id, key -> new Lag(progress, now));
        if (progress < lag.last - 1.0E-4F) {
            lag.dropped = now;
        }
        if (progress >= lag.value) {
            lag.value = progress;
        } else if (now - lag.dropped > LAG_HOLD) {
            lag.value = Math.max(progress, lag.value - (now - lag.tick) * LAG_DRAIN);
        }
        lag.last = progress;
        lag.tick = now;
        return lag.value;
    }

    private static int shade(int colour, float by) {
        return mix(0, colour, by);
    }

    private static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, a >> 16 & 255, b >> 16 & 255);
        int g = (int) Mth.lerp(t, a >> 8 & 255, b >> 8 & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return r << 16 | g << 8 | bl;
    }
}
