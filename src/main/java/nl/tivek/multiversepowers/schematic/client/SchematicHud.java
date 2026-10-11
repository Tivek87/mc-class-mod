package nl.tivek.multiversepowers.schematic.client;

import java.util.ArrayList;
import java.util.List;
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
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramRenderer;
import nl.tivek.multiversepowers.schematic.net.SchematicStatusPayload;

// A card at the top of the screen while a paste goes in (or is undone): how far it is, and, while the server waits
// for chunks it has not loaded, what to do about it. While a big hologram is still being drawn it says so too.
public final class SchematicHud {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "schematic_hud");
    private static final int WIDTH = 230;
    private static final int WARN = 0xFF000000 | NavScreen.WARN;

    private SchematicHud() {
    }

    public static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.BOSS_OVERLAY, ID, SchematicHud::render);
    }

    static Component line(SchematicStatusPayload status) {
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
        SchematicStatusPayload status = SchematicClient.status();
        boolean drawing = Hologram.shown() && HologramRenderer.progress() < 1.0F;
        if (status == null && !drawing && !SchematicUpload.busy()) {
            return;
        }
        Font font = minecraft.font;
        List<FormattedCharSequence> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        float progress;
        if (status != null) {
            add(font, lines, colors, line(status), 0xFFFFFFFF);
            progress = status.total() == 0 ? 1.0F : status.done() / (float) status.total();
            if (status.waiting() > 0 && !status.over()) {
                add(font, lines, colors, Component.translatable(SchematicClient.PREFIX + "hud.chunks"), WARN);
            }
        } else if (SchematicUpload.busy()) {
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

    // A line of the card, broken to fit inside it.
    private static void add(Font font, List<FormattedCharSequence> lines, List<Integer> colors, Component text,
            int color) {
        for (FormattedCharSequence wrapped : font.split(text, WIDTH - 16)) {
            lines.add(wrapped);
            colors.add(color);
        }
    }
}
