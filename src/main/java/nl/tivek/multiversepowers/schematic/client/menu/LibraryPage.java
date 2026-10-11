package nl.tivek.multiversepowers.schematic.client.menu;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
import nl.tivek.multiversepowers.schematic.client.FileDialogs;
import nl.tivek.multiversepowers.schematic.client.SchematicClient;
import nl.tivek.multiversepowers.schematic.client.SchematicUpload;
import nl.tivek.multiversepowers.schematic.net.SchematicAskPayload;
import org.lwjgl.glfw.GLFW;

// The library page: one folder of the server's schematics at a time, folders first, searched and sorted. A click picks
// a file (it shows beside the list), a double click goes on to placing it; folders are made, files and folders renamed
// and deleted (asked twice), files copied, brought in from this computer or saved to it.
public final class LibraryPage extends SchematicScreen {
    private static final int ROW = 14;
    private static final int SURE = 3000;
    private static final int FOLDER = 1;
    private static final int RENAME = 2;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy")
            .withZone(ZoneId.systemDefault());
    private static final List<Comparator<SchematicLibrary.Entry>> SORTS = List.of(
            Comparator.comparing(entry -> entry.name().toLowerCase(Locale.ROOT)),
            Comparator.comparingLong(SchematicLibrary.Entry::modified).reversed(),
            Comparator.comparingLong(SchematicLibrary.Entry::size).reversed());

    @Nullable
    public static String selected;
    private static int sort;
    private static String search = "";
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private double scroll;
    private long seen;
    // What the name box is for: nothing, a new folder or the chosen file's new name.
    private int naming;
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
        int searchWidth = Math.min(96, this.listWidth / 2);
        EditBox find = new EditBox(this.font, this.listX + this.listWidth - searchWidth, this.contentY - 2,
                searchWidth, 12, Component.translatable(PREFIX + "search"));
        find.setHint(Component.translatable(PREFIX + "search").withStyle(ChatFormatting.DARK_GRAY));
        find.setMaxLength(48);
        find.setValue(search);
        find.setResponder(text -> {
            search = text;
            this.scroll = 0.0;
        });
        this.addRenderableWidget(find);
        if (this.naming != 0) {
            this.name = new EditBox(this.font, this.listX, y, this.listWidth - 112, 18,
                    Component.translatable(PREFIX + (this.naming == FOLDER ? "folder.name" : "rename.name")));
            this.name.setMaxLength(48);
            this.name.setValue(this.typed);
            this.name.setResponder(text -> this.typed = text);
            this.addRenderableWidget(this.name);
            this.setInitialFocus(this.name);
            this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + (this.naming == FOLDER
                    ? "folder.make" : "rename.done")), button -> this.named())
                    .bounds(this.listX + this.listWidth - 108, y - 1, 52, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
                this.naming = 0;
                this.rebuildWidgets();
            }).bounds(this.listX + this.listWidth - 52, y - 1, 52, 20).build());
        } else {
            int width = (this.listWidth - 6) / 3;
            this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "folder.new"), button -> {
                this.naming = FOLDER;
                this.typed = "";
                this.rebuildWidgets();
            }).tooltip(tip(Component.translatable(PREFIX + "folder.new.desc"))).bounds(this.listX, y - 1, width, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "import"), button ->
                    FileDialogs.pick(path -> SchematicUpload.send(path, SchematicClient.folder)))
                    .tooltip(tip(Component.translatable(PREFIX + "import.desc")))
                    .bounds(this.listX + width + 3, y - 1, width, 20).build());
            this.delete = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "delete"),
                    button -> this.delete()).tooltip(tip(Component.translatable(PREFIX + "delete.desc")))
                    .bounds(this.listX + (width + 3) * 2, y - 1, this.listWidth - (width + 3) * 2, 20).build());
        }
        int sideX = this.listX + this.listWidth + 8;
        int sideWidth = this.contentX + this.contentWidth - sideX;
        this.use = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "use"),
                button -> this.minecraft.setScreen(new PlacePage(this.root)))
                .bounds(sideX, y - 1, sideWidth, 20).build());
        this.use.active = SchematicClient.viewed != null;
        this.relabel();
    }

    // The name box's Make or Rename.
    private void named() {
        if (!this.typed.isBlank()) {
            if (this.naming == FOLDER) {
                SchematicClient.ask(SchematicAskPayload.FOLDER, SchematicClient.folder, this.typed);
            } else if (selected != null) {
                SchematicClient.ask(SchematicAskPayload.RENAME, selected, this.typed);
            }
        }
        this.naming = 0;
        this.typed = "";
        this.rebuildWidgets();
    }

    // The chosen file's name without its kind, or the folder's name, to start renaming from.
    private String stem() {
        if (selected == null) {
            return "";
        }
        String name = selected.substring(selected.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return this.file() != null && dot > 0 ? name.substring(0, dot) : name;
    }

    // The folder's entries as listed: those matching the search, folders first, then by the chosen order.
    private static List<SchematicLibrary.Entry> shown() {
        String looked = search.trim().toLowerCase(Locale.ROOT);
        List<SchematicLibrary.Entry> kept = new ArrayList<>();
        for (SchematicLibrary.Entry entry : SchematicClient.entries) {
            if (looked.isEmpty() || entry.name().toLowerCase(Locale.ROOT).contains(looked)) {
                kept.add(entry);
            }
        }
        kept.sort(Comparator.comparing((SchematicLibrary.Entry entry) -> !entry.folder()).thenComparing(SORTS.get(
                sort)));
        return kept;
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (this.naming != 0 && this.name != null && this.name.isFocused() && (key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_KP_ENTER)) {
            this.named();
            return true;
        }
        if (this.naming == 0 && !this.typing() && selected != null) {
            if (key == GLFW.GLFW_KEY_F2) {
                this.rename();
                return true;
            }
            if (key == GLFW.GLFW_KEY_DELETE) {
                this.delete();
                return true;
            }
        }
        return super.keyPressed(key, scan, modifiers);
    }

    private void rename() {
        this.naming = RENAME;
        this.typed = this.stem();
        this.rebuildWidgets();
    }

    // Rename, Copy and Download for the chosen row, small buttons under its facts; Download a row lower where the
    // three do not fit side by side.
    private void actions(GuiGraphics graphics, int x, int y, int width, int mouseX, int mouseY) {
        String file = this.file();
        boolean any = selected != null && this.naming == 0;
        boolean split = !this.actionsFit(width);
        int at = x;
        int row = split ? y - 12 : y;
        at += this.action(graphics, "rename", at, row, mouseX, mouseY, any, this::rename) + 3;
        at += this.action(graphics, "copy", at, row, mouseX, mouseY, any && file != null,
                () -> SchematicClient.ask(SchematicAskPayload.COPY, file, "")) + 3;
        this.action(graphics, "download", split ? x : at, y, mouseX, mouseY, any && file != null,
                () -> SchematicClient.ask(SchematicAskPayload.DOWNLOAD, file, ""));
    }

    // A small button with what it does under the pointer.
    private int action(GuiGraphics graphics, String id, int x, int y, int mouseX, int mouseY, boolean active,
            Runnable run) {
        int width = this.smallButton(graphics, Component.translatable(PREFIX + id), x, y, mouseX, mouseY, active, run);
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + 10) {
            this.setTooltipForNextRenderPass(this.font.split(Component.translatable(PREFIX + id + ".desc"), 200));
        }
        return width;
    }

    private boolean actionsFit(int width) {
        return this.font.width(Component.translatable(PREFIX + "rename")) + this.font.width(Component.translatable(
                PREFIX + "copy")) + this.font.width(Component.translatable(PREFIX + "download")) + 24 <= width;
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
        List<SchematicLibrary.Entry> entries = shown();
        int top = this.listY + 2 - (int) Math.round(this.scroll);
        if (entries.isEmpty()) {
            this.wrap(graphics, Component.translatable(SchematicClient.listed == 0L ? PREFIX + "listing"
                    : !SchematicClient.entries.isEmpty() ? PREFIX + "search.none" : PREFIX + "empty"),
                    this.listX + 6, this.listY + 6, this.listWidth - 12, MUTED, 4);
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
        boolean missing = SchematicClient.viewed != null && !SchematicClient.viewed.missing.isEmpty();
        int box = Math.min(sideWidth, this.listHeight - 52 - (missing ? 12 : 0) - (this.actionsFit(sideWidth) ? 0
                : 12));
        this.preview(graphics, sideX, this.listY, sideWidth, box);
        this.facts(graphics, sideX, this.listY + box + 5, sideWidth, mouseX, mouseY);
        this.actions(graphics, sideX, this.listY + this.listHeight - 10, sideWidth, mouseX, mouseY);
        this.status(graphics, this.contentX, this.contentY + this.contentHeight - 8, this.contentWidth);
    }

    // The folder's path, each part a link back up to it, the order the list is in, and where the library lies.
    private void crumbs(GuiGraphics graphics, int mouseX, int mouseY) {
        int x = this.contentX;
        int y = this.contentY;
        Component order = Component.translatable(PREFIX + "sort." + sort);
        int searchWidth = Math.min(96, this.listWidth / 2);
        int orderX = this.listX + this.listWidth - searchWidth - this.font.width(order) - 10;
        this.smallButton(graphics, order, orderX, y - 1, mouseX, mouseY, true, () -> {
            sort = (sort + 1) % SORTS.size();
            this.click(1.0F);
        });
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
        int sideX = this.listX + this.listWidth + 8;
        Component where = Component.translatable(PREFIX + "where", SchematicClient.root);
        int width = Math.min(this.font.width(where), this.contentX + this.contentWidth - sideX);
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
            List<SchematicLibrary.Entry> entries = shown();
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
            int most = Math.max(0, shown().size() * ROW + 4 - this.listHeight);
            this.scroll = Mth.clamp(this.scroll - scrollY * ROW * 2, 0.0, most);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
