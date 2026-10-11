package nl.tivek.multiversepowers.schematic.client;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.SchematicLibrary;
import nl.tivek.multiversepowers.schematic.net.SchematicAskPayload;

// The library page: one folder of the server's schematics at a time, folders first. A click picks a file (it shows
// beside the list), a double click goes on to placing it; folders are made, files and folders deleted (asked twice),
// files brought in from this computer or saved to it.
final class LibraryPage extends SchematicScreen {
    private static final int ROW = 14;
    private static final int SURE = 3000;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy")
            .withZone(ZoneId.systemDefault());

    @Nullable
    private static String selected;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private double scroll;
    private long seen;
    private boolean naming;
    @Nullable
    private EditBox name;
    private String typed = "";
    @Nullable
    private String asked;
    private long askedAt;
    private long lastClick;
    @Nullable
    private Button delete;
    @Nullable
    private Button use;

    LibraryPage(@Nullable Screen root) {
        super(root, LIBRARY);
    }

    @Override
    protected Component subtitle() {
        return Component.translatable(PREFIX + "library.about");
    }

    @Override
    protected void initPage() {
        this.seen = SchematicClient.listed;
        int side = Math.max(110, this.contentWidth * 2 / 5);
        this.listX = this.contentX;
        this.listY = this.contentY + 14;
        this.listWidth = this.contentWidth - side - 8;
        int bottom = this.contentY + this.contentHeight - 34;
        this.listHeight = bottom - this.listY;
        int y = this.contentY + this.contentHeight - 30;
        if (this.naming) {
            this.name = new EditBox(this.font, this.listX, y, this.listWidth - 112, 18,
                    Component.translatable(PREFIX + "folder.name"));
            this.name.setMaxLength(48);
            this.name.setValue(this.typed);
            this.name.setResponder(text -> this.typed = text);
            this.addRenderableWidget(this.name);
            this.setInitialFocus(this.name);
            this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "folder.make"), button -> {
                if (!this.typed.isBlank()) {
                    SchematicClient.ask(SchematicAskPayload.FOLDER, SchematicClient.folder, this.typed);
                }
                this.naming = false;
                this.typed = "";
                this.rebuildWidgets();
            }).bounds(this.listX + this.listWidth - 108, y - 1, 52, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
                this.naming = false;
                this.rebuildWidgets();
            }).bounds(this.listX + this.listWidth - 52, y - 1, 52, 20).build());
        } else {
            int width = (this.listWidth - 6) / 3;
            this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "folder.new"), button -> {
                this.naming = true;
                this.rebuildWidgets();
            }).tooltip(tip(Component.translatable(PREFIX + "folder.new.desc"))).bounds(this.listX, y - 1, width, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "import"), button ->
                    FileDialogs.pick(path -> SchematicUpload.send(path, SchematicClient.folder)))
                    .tooltip(tip(Component.translatable(PREFIX + "import.desc")))
                    .bounds(this.listX + width + 3, y - 1, width, 20).build());
            this.delete = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "delete"),
                    button -> this.delete()).tooltip(tip(Component.translatable(PREFIX + "delete.desc")))
                    .bounds(this.listX + (width + 3) * 2, y - 1, width, 20).build());
        }
        int sideX = this.listX + this.listWidth + 8;
        int sideWidth = this.contentX + this.contentWidth - sideX;
        Button download = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "download"),
                button -> {
                    String file = this.file();
                    if (file != null) {
                        SchematicClient.ask(SchematicAskPayload.DOWNLOAD, file, "");
                    }
                }).tooltip(tip(Component.translatable(PREFIX + "download.desc")))
                .bounds(sideX, y - 1, (sideWidth - 3) / 2, 20).build());
        this.use = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "use"),
                button -> this.minecraft.setScreen(new PlacePage(this.root)))
                .bounds(sideX + (sideWidth - 3) / 2 + 3, y - 1, (sideWidth - 3) / 2, 20).build());
        download.active = this.file() != null;
        this.use.active = SchematicClient.viewed != null;
        this.relabel();
    }

    // The chosen row, when it is a file of this folder.
    @Nullable
    private String file() {
        if (selected == null) {
            return null;
        }
        for (SchematicLibrary.Entry entry : SchematicClient.entries) {
            if (!entry.folder() && path(entry).equals(selected)) {
                return selected;
            }
        }
        return null;
    }

    private static String path(SchematicLibrary.Entry entry) {
        return SchematicClient.folder.isEmpty() ? entry.name() : SchematicClient.folder + "/" + entry.name();
    }

    private void delete() {
        if (selected == null) {
            return;
        }
        long now = Util.getMillis();
        if (selected.equals(this.asked) && now - this.askedAt < SURE) {
            SchematicClient.ask(SchematicAskPayload.DELETE, selected, "");
            this.asked = null;
            selected = null;
        } else {
            this.asked = selected;
            this.askedAt = now;
        }
        this.relabel();
    }

    private void relabel() {
        if (this.delete == null) {
            return;
        }
        boolean sure = this.asked != null && this.asked.equals(selected) && Util.getMillis() - this.askedAt < SURE;
        this.delete.setMessage(sure ? Component.translatable(PREFIX + "delete.sure").withStyle(ChatFormatting.YELLOW)
                : Component.translatable(PREFIX + "delete"));
        this.delete.active = selected != null;
    }

    @Override
    public void tick() {
        super.tick();
        if (SchematicClient.listed != this.seen) {
            this.rebuildWidgets();
        }
        if (this.use != null) {
            this.use.active = SchematicClient.viewed != null;
        }
        if (this.asked != null && Util.getMillis() - this.askedAt >= SURE) {
            this.asked = null;
            this.relabel();
        }
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.crumbs(graphics, mouseX, mouseY);
        ScreenAnchors.report("schematic.library", this.listX, this.listY, this.listWidth, this.listHeight);
        graphics.fill(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.listHeight, 0x40000000);
        graphics.enableScissor(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.listHeight);
        List<SchematicLibrary.Entry> entries = SchematicClient.entries;
        int top = this.listY + 2 - (int) Math.round(this.scroll);
        if (entries.isEmpty()) {
            this.wrap(graphics, Component.translatable(SchematicClient.listed == 0L ? PREFIX + "listing"
                    : PREFIX + "empty"), this.listX + 6, this.listY + 6, this.listWidth - 12, MUTED, 4);
        }
        for (int k = 0; k < entries.size(); k++) {
            int y = top + k * ROW;
            if (y + ROW < this.listY || y > this.listY + this.listHeight) {
                continue;
            }
            SchematicLibrary.Entry entry = entries.get(k);
            boolean picked = path(entry).equals(selected);
            boolean hover = mouseX >= this.listX && mouseX < this.listX + this.listWidth && mouseY >= y
                    && mouseY < y + ROW && mouseY >= this.listY && mouseY < this.listY + this.listHeight;
            if (picked || hover) {
                graphics.fill(this.listX + 1, y, this.listX + this.listWidth - 1, y + ROW, picked
                        ? 0x507FD3FF : 0x20FFFFFF);
            }
            PixelIcons.draw(graphics, entry.folder() ? PixelIcons.Icon.FOLDER : PixelIcons.Icon.CUBE, this.listX + 4,
                    y + 1, 1, entry.folder() ? 0xFFF2C84B : 0xFF7FD3FF, false);
            String detail = entry.folder() ? "" : size(entry.size()) + "  " + WHEN.format(Instant.ofEpochMilli(
                    entry.modified()));
            int detailWidth = this.font.width(detail);
            graphics.drawString(this.font, this.fit(Component.literal(entry.name()), this.listWidth - detailWidth - 26),
                    this.listX + 19, y + 3, picked ? TEXT : BODY, false);
            graphics.drawString(this.font, detail, this.listX + this.listWidth - detailWidth - 4, y + 3, DIM, false);
        }
        graphics.disableScissor();
        int sideX = this.listX + this.listWidth + 8;
        int sideWidth = this.contentX + this.contentWidth - sideX;
        int box = Math.min(sideWidth, this.listHeight - 46);
        this.preview(graphics, sideX, this.listY, sideWidth, box);
        this.facts(graphics, sideX, this.listY + box + 5, sideWidth);
        this.status(graphics, this.contentX, this.contentY + this.contentHeight - 8, this.contentWidth);
    }

    // The folder's path, each part a link back up to it, and where the library lies.
    private void crumbs(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.contentX;
        int y = this.contentY;
        x += this.link(graphics, Component.translatable(PREFIX + "root"), x, y, mouseX, mouseY, 0xFF000000 | COLOR,
                () -> this.go("")) + 2;
        String walked = "";
        if (!SchematicClient.folder.isEmpty()) {
            for (String part : SchematicClient.folder.split("/")) {
                walked = walked.isEmpty() ? part : walked + "/" + part;
                String to = walked;
                graphics.drawString(this.font, "/", x, y, DIM, false);
                x += this.font.width("/") + 2;
                x += this.link(graphics, Component.literal(part), x, y, mouseX, mouseY, 0xFF000000 | COLOR,
                        () -> this.go(to)) + 2;
            }
        }
        Component where = Component.translatable(PREFIX + "where", SchematicClient.root);
        int width = Math.min(this.font.width(where), this.contentX + this.contentWidth - x - 8);
        if (width > 40) {
            graphics.drawString(this.font, this.fit(where, width), this.contentX + this.contentWidth - width, y, DIM,
                    false);
        }
    }

    private void go(String folder) {
        selected = null;
        this.scroll = 0.0;
        SchematicClient.list(folder);
    }

    private static String size(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return bytes / 1024L + " kB";
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= this.listX && mouseX < this.listX + this.listWidth && mouseY >= this.listY
                && mouseY < this.listY + this.listHeight) {
            int k = (int) ((mouseY - this.listY - 2 + this.scroll) / ROW);
            List<SchematicLibrary.Entry> entries = SchematicClient.entries;
            if (k >= 0 && k < entries.size()) {
                SchematicLibrary.Entry entry = entries.get(k);
                String path = path(entry);
                long now = Util.getMillis();
                boolean twice = path.equals(selected) && now - this.lastClick < 350L;
                this.lastClick = now;
                if (entry.folder()) {
                    if (twice || path.equals(selected)) {
                        this.go(path);
                    } else {
                        selected = path;
                    }
                } else {
                    selected = path;
                    SchematicClient.view(path);
                    if (twice) {
                        this.minecraft.setScreen(new PlacePage(this.root));
                        return true;
                    }
                }
                this.click(1.0F);
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listWidth) {
            int most = Math.max(0, SchematicClient.entries.size() * ROW + 4 - this.listHeight);
            this.scroll = Mth.clamp(this.scroll - scrollY * ROW * 2, 0.0, most);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
