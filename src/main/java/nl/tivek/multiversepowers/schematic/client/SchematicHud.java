package nl.tivek.multiversepowers.schematic.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramRenderer;
import nl.tivek.multiversepowers.schematic.client.menu.SchematicScreen;
import nl.tivek.multiversepowers.schematic.net.SchematicStatusPayload;

// A card at the top of the screen while a paste goes in (or is undone): how far it is, and, while the server waits
// for chunks it has not loaded, what to do about it. While a big hologram is still being drawn it says so too.
public final class SchematicHud {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "schematic_hud");
    private static final int WIDTH = 230;
    private static final int PASTE_WIDTH = 260;
    private static final int WARN = 0xFF000000 | NavScreen.WARN;
    // Where the pace is counted from: the paste's name, whether it undoes, the blocks done and the time then.
    private static String rateName = "";
    private static boolean rateUndo;
    private static int rateDone;
    private static long rateAt;

    private SchematicHud() {
    }

    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ID, SchematicHud::render);
    }

    public static Component line(SchematicStatusPayload status) {
        String done = String.format("%,d", status.done());
        String total = String.format("%,d", status.total());
        return switch (status.state()) {
            case SchematicStatusPayload.WAITING -> Component.translatable(SchematicClient.PREFIX + "hud.waiting",
                    status.name(), done, total, status.waiting());
            case SchematicStatusPayload.DONE -> Component.translatable(SchematicClient.PREFIX + "hud.done",
                    status.name(), total);
            case SchematicStatusPayload.CANCELLED -> Component.translatable(SchematicClient.PREFIX + "hud.cancelled",
                    status.name(), done);
            case SchematicStatusPayload.UNDOING -> status.waiting() > 0 ? Component.translatable(
                    SchematicClient.PREFIX + "hud.undo_waiting", status.name(), done, total, status.waiting())
                    : Component.translatable(SchematicClient.PREFIX + "hud.undoing", status.name(), done, total);
            case SchematicStatusPayload.UNDONE -> Component.translatable(SchematicClient.PREFIX + "hud.undone",
                    status.name());
            default -> Component.translatable(SchematicClient.PREFIX + "hud.placing", status.name(), done, total);
        };
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.options.hideGui || minecraft.player == null) {
            return;
        }
        SchematicGuide.render(graphics, minecraft);
        SchematicStatusPayload status = SchematicClient.status();
        if (status != null) {
            paste(graphics, minecraft.font, status);
            return;
        }
        if (AreaSelection.on() && SchematicClient.holding(minecraft.player)) {
            selecting(graphics, minecraft.font);
            return;
        }
        boolean drawing = Hologram.shown() && HologramRenderer.progress() < 1.0F;
        if (!drawing && !SchematicUpload.busy()) {
            return;
        }
        Font font = minecraft.font;
        List<FormattedCharSequence> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        float progress;
        if (SchematicUpload.busy()) {
            add(font, lines, colors, Component.translatable(SchematicClient.PREFIX + "hud.importing"), 0xFFFFFFFF);
            progress = SchematicUpload.progress();
        } else {
            add(font, lines, colors, Component.translatable(SchematicClient.PREFIX + "hud.drawing",
                    Math.round(HologramRenderer.progress() * 100.0F)), 0xFFD2D2D2);
            progress = HologramRenderer.progress();
        }
        int height = 14 + lines.size() * 10;
        int x = (graphics.guiWidth() - WIDTH) / 2;
        int y = 4;
        NavScreen.card(graphics, x, y, WIDTH, height, SchematicScreen.COLOR);
        for (int k = 0; k < lines.size(); k++) {
            graphics.drawString(font, lines.get(k), x + 8, y + 5 + k * 10, colors.get(k), false);
        }
        NavScreen.bar(graphics, x + 8, y + height - 7, WIDTH - 16, progress, SchematicScreen.COLOR);
        ScreenAnchors.report("game.schematic", x, y, WIDTH, height);
    }

    // A paste's card: what it does and to which structure, a broad bar, the blocks placed of all and the share, how
    // fast it goes and how long it has left, and what to do while it waits for chunks.
    private static void paste(GuiGraphics graphics, Font font, SchematicStatusPayload status) {
        long now = Util.getMillis();
        boolean undo = status.state() == SchematicStatusPayload.UNDOING || status.state()
                == SchematicStatusPayload.UNDONE;
        if (!status.name().equals(rateName) || undo != rateUndo || status.done() < rateDone) {
            rateName = status.name();
            rateUndo = undo;
            rateDone = status.done();
            rateAt = now;
        }
        int x = (graphics.guiWidth() - PASTE_WIDTH) / 2;
        int y = 4;
        boolean waiting = status.waiting() > 0 && !status.over();
        List<FormattedCharSequence> hint = waiting ? font.split(Component.translatable(SchematicClient.PREFIX
                + "hud.chunks"), PASTE_WIDTH - 16) : List.of();
        int height = 48 + hint.size() * 10;
        NavScreen.card(graphics, x, y, PASTE_WIDTH, height, SchematicScreen.COLOR);
        int stateColor = switch (status.state()) {
            case SchematicStatusPayload.DONE, SchematicStatusPayload.UNDONE -> 0x3F8F4F;
            case SchematicStatusPayload.CANCELLED -> 0x8F3F3F;
            case SchematicStatusPayload.WAITING -> 0x8F7A2F;
            default -> 0x2F6F8F;
        };
        Component state = Component.translatable(SchematicClient.PREFIX + "hud.state." + (waiting
                ? SchematicStatusPayload.WAITING : status.state()));
        int chip = font.width(state) + 6;
        graphics.fill(x + 8, y + 5, x + 8 + chip, y + 15, 0xFF000000 | stateColor);
        graphics.drawString(font, state, x + 11, y + 6, 0xFFFFFFFF, false);
        String name = font.plainSubstrByWidth(status.name(), PASTE_WIDTH - chip - 24);
        graphics.drawString(font, name, x + 14 + chip, y + 6, 0xFFFFFFFF, false);
        float progress = status.total() == 0 ? 1.0F : status.done() / (float) status.total();
        int barX = x + 8;
        int barY = y + 20;
        int barWidth = PASTE_WIDTH - 16;
        graphics.fill(barX - 1, barY - 1, barX + barWidth + 1, barY + 7, 0xFF000000);
        graphics.fill(barX, barY, barX + barWidth, barY + 6, 0xFF2A2A2A);
        int filled = Math.round(barWidth * Math.max(0.0F, Math.min(1.0F, progress)));
        graphics.fill(barX, barY, barX + filled, barY + 6, 0xFF000000 | (status.over() ? 0x8CE08C
                : SchematicScreen.COLOR));
        graphics.fill(barX, barY, barX + filled, barY + 2, 0x40FFFFFF);
        Component count = Component.translatable(SchematicClient.PREFIX + "hud.count", String.format("%,d",
                status.done()), String.format("%,d", status.total()));
        graphics.drawString(font, count, x + 8, y + 30, 0xFFD2D2D2, false);
        String share = Math.round(progress * 100.0F) + "%";
        graphics.drawString(font, share, x + PASTE_WIDTH - 8 - font.width(share), y + 30, 0xFFFFFFFF, false);
        Component pace = pace(status, now);
        if (pace != null) {
            graphics.drawString(font, pace, x + 8, y + 39, 0xFF8E8E8E, false);
        }
        for (int k = 0; k < hint.size(); k++) {
            graphics.drawString(font, hint.get(k), x + 8, y + 49 + k * 10, WARN, false);
        }
        ScreenAnchors.report("game.schematic", x, y, PASTE_WIDTH, height);
    }

    // While an area is picked: its size, and which click does what.
    private static void selecting(GuiGraphics graphics, Font font) {
        int x = (graphics.guiWidth() - PASTE_WIDTH) / 2;
        int y = 4;
        List<FormattedCharSequence> help = font.split(Component.translatable(SchematicClient.PREFIX + "select.help",
                AbilityKeys.SLOTS[WandInput.HIDE].getTranslatedKeyMessage()), PASTE_WIDTH - 16);
        int height = 28 + help.size() * 10;
        NavScreen.card(graphics, x, y, PASTE_WIDTH, height, 0xFFD54A);
        Component title = Component.translatable(SchematicClient.PREFIX + "select.title");
        graphics.drawString(font, title, x + 8, y + 5, 0xFFFFD54A, false);
        int[] size = AreaSelection.size();
        boolean big = AreaSelection.tooBig();
        Component what = AreaSelection.complete() ? Component.translatable(SchematicClient.PREFIX + "select.size",
                size[0], size[1], size[2], String.format("%,d", AreaSelection.volume())) : Component.translatable(
                        SchematicClient.PREFIX + (AreaSelection.first == null ? "select.need_first"
                                : "select.need_second"));
        graphics.drawString(font, what, x + 8, y + 16, big ? 0xFFEF7B6E : 0xFFFFFFFF, false);
        for (int k = 0; k < help.size(); k++) {
            graphics.drawString(font, help.get(k), x + 8, y + 27 + k * 10, 0xFFB0B0B0, false);
        }
        ScreenAnchors.report("game.select", x, y, PASTE_WIDTH, height);
    }

    // Blocks a second and the time left, once it has gone a moment; null before.
    @Nullable
    private static Component pace(SchematicStatusPayload status, long now) {
        if (status.over() || status.waiting() > 0) {
            return status.state() == SchematicStatusPayload.DONE ? Component.translatable(SchematicClient.PREFIX
                    + "hud.finished") : null;
        }
        float seconds = (now - rateAt) / 1000.0F;
        int gone = status.done() - rateDone;
        if (seconds < 1.0F || gone <= 0) {
            return Component.translatable(SchematicClient.PREFIX + "hud.starting");
        }
        float rate = gone / seconds;
        int left = Math.round((status.total() - status.done()) / rate);
        String time = left >= 60 ? left / 60 + " min " + left % 60 + " s" : left + " s";
        return Component.translatable(SchematicClient.PREFIX + "hud.pace", String.format("%,d", Math.round(rate)),
                time);
    }

    // A line of the card, broken to fit inside it.
    private static void add(Font font, List<FormattedCharSequence> lines, List<Integer> colors, Component text,
            int color) {
        for (FormattedCharSequence wrapped : font.split(text, WIDTH - 16)) {
            lines.add(wrapped);
            colors.add(color);
        }
    }
}
