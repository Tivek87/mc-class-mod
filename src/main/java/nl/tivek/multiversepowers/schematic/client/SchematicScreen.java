package nl.tivek.multiversepowers.schematic.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.net.SchematicStatusPayload;

// The Schematic Wand's menu: Library (the server's schematic files and folders, brought in from or saved to this
// computer), Place (how the chosen one turns, follows and what it replaces), with the chosen one turning slowly, and
// Blueprints (the plans set down in the world and what each still needs).
public abstract class SchematicScreen extends NavScreen {
    public static final int COLOR = 0x7FD3FF;
    static final String LIBRARY = "library";
    static final String PLACE = "place";
    static final String BLUEPRINTS = "blueprints";
    static final String PREFIX = SchematicClient.PREFIX;
    private static final int GOOD = 0xFF8CE08C;
    private static final int BAD = 0xFFEF7B6E;

    protected SchematicScreen(@Nullable Screen root, String page) {
        super(Component.translatable(PREFIX + page).withColor(COLOR), root, page);
    }

    // From the wand: the library, or the placing options while a schematic is chosen and shown.
    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !SchematicClient.allowed(minecraft.player)) {
            return;
        }
        SchematicClient.list(SchematicClient.folder);
        minecraft.setScreen(Hologram.shown() ? new PlacePage(null) : new LibraryPage(null));
    }

    @Override
    protected List<Item> items() {
        boolean chosen = SchematicClient.viewed != null;
        return List.of(new Item(LIBRARY, Component.translatable(PREFIX + LIBRARY), PixelIcons.Icon.FOLDER,
                () -> this.minecraft.setScreen(new LibraryPage(this.root)), false),
                new Item(PLACE, Component.translatable(PREFIX + PLACE), PixelIcons.Icon.CUBE,
                        chosen ? () -> this.minecraft.setScreen(new PlacePage(this.root)) : null, false, false, null, 0,
                        chosen ? null : Component.translatable(PREFIX + "place.none")),
                new Item(BLUEPRINTS, Component.translatable(PREFIX + BLUEPRINTS), PixelIcons.Icon.PLAN,
                        () -> this.minecraft.setScreen(new BlueprintPage(this.root)), false));
    }

    @Override
    protected Component brand() {
        return Component.translatable(PREFIX + "brand");
    }

    @Override
    protected PixelIcons.Icon brandIcon() {
        return PixelIcons.Icon.CUBE;
    }

    @Override
    protected int accent() {
        return COLOR;
    }

    // The chosen schematic slowly turning in a framed box, or why it is not there.
    protected void preview(GuiGraphics graphics, int x, int y, int width, int height) {
        card(graphics, x, y, width, height, COLOR);
        Component note = null;
        if (SchematicClient.loadingPath != null) {
            note = Component.translatable(PREFIX + "loading", Math.round(SchematicClient.loading * 100.0F));
        } else if (SchematicClient.viewError != null) {
            note = SchematicClient.viewError;
        } else if (SchematicClient.viewed == null) {
            note = Component.translatable(PREFIX + "preview.none");
        } else if (PreviewModel.LIBRARY.building()) {
            note = Component.translatable(PREFIX + "preview.building");
        }
        if (note != null) {
            this.wrap(graphics, note, x + 6, y + height / 2 - 8, width - 12,
                    SchematicClient.viewError != null ? BAD : MUTED, 3);
            return;
        }
        turning(graphics, PreviewModel.LIBRARY, x, y, width, height);
    }

    // A preview slowly turning inside a box.
    static void turning(GuiGraphics graphics, PreviewModel model, int x, int y, int width, int height) {
        float turn = (Util.getMillis() % 24000L) / 24000.0F * (float) (Math.PI * 2.0);
        model.draw(graphics, x + 2, y + 2, width - 4, height - 4, turn);
    }

    // Name, size and how many blocks of the chosen schematic.
    protected int facts(GuiGraphics graphics, int x, int y, int width) {
        Schematic shown = SchematicClient.viewed;
        if (shown == null || SchematicClient.loadingPath != null) {
            return y;
        }
        graphics.drawString(this.font, this.fit(Component.literal(shown.name), width), x, y, TEXT, false);
        y += 11;
        graphics.drawString(this.font, Component.translatable(PREFIX + "size", shown.sizeX, shown.sizeY, shown.sizeZ),
                x, y, MUTED, false);
        y += 10;
        graphics.drawString(this.font, Component.translatable(PREFIX + "blocks", String.format("%,d", shown.blocks())),
                x, y, MUTED, false);
        y += 10;
        if (shown.unknown > 0) {
            y = this.wrap(graphics, Component.translatable(PREFIX + "unknown", shown.unknown), x, y, width,
                    0xFF000000 | WARN, 2);
        }
        return y;
    }

    // What the last paste or notice said, along the bottom.
    protected void status(GuiGraphics graphics, int x, int y, int width) {
        SchematicStatusPayload paste = SchematicClient.status();
        if (paste != null && !paste.over()) {
            float done = paste.total() == 0 ? 1.0F : paste.done() / (float) paste.total();
            bar(graphics, x, y + 3, Math.min(80, width / 3), done, COLOR);
            graphics.drawString(this.font, this.fit(SchematicHud.line(paste), width - 86), x + Math.min(80, width / 3)
                    + 6, y, BODY, false);
            return;
        }
        Component notice = SchematicClient.notice;
        if (notice != null && Util.getMillis() - SchematicClient.noticeAt < 8000L) {
            graphics.drawString(this.font, this.fit(notice, width), x, y, SchematicClient.noticeBad ? BAD : GOOD,
                    false);
        }
    }
}
