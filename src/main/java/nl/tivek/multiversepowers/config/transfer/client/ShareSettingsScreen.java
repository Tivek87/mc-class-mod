package nl.tivek.multiversepowers.config.transfer.client;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.SettingsPages;
import nl.tivek.multiversepowers.config.client.SettingsScreen;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;

// The settings window's Import & export page: this world's settings saved to a file on this computer under a name, and
// every saved file listed to load into this world (asked twice, as it overwrites what is set here). The files sit in
// the game folder's world_settings, which a button opens, so they can be copied to another computer.
public final class ShareSettingsScreen extends NavScreen {
    public static final int COLOR = 0x8FE3B0;
    private static final String PREFIX = "config." + MultiversePowers.MODID + ".";
    private static final String KEY = PREFIX + "share.";
    private static final int ROW = 22;
    private static final int SURE = 3000;
    private static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
            .withZone(ZoneId.systemDefault());

    private record Row(SavedSettings.Saved saved, int y, Button button) {
    }

    private final List<Row> rows = new ArrayList<>();
    @Nullable
    private EditBox name;
    private String typed;
    @Nullable
    private SavedSettings.Saved asked;
    private long askedAt;
    private long seen;
    private int more;
    private int exportY;
    private int importY;

    public ShareSettingsScreen(@Nullable Screen root) {
        super(Component.translatable(PREFIX + "share").withColor(COLOR), root, SettingsPages.SHARE);
        this.typed = worldName();
    }

    private static String worldName() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.getSingleplayerServer() != null) {
            return SavedSettings.clean(minecraft.getSingleplayerServer().getWorldData().getLevelName());
        }
        return SavedSettings.clean(minecraft.getCurrentServer() != null ? minecraft.getCurrentServer().name
                : "server");
    }

    @Override
    protected List<Item> items() {
        return SettingsScreen.sidebar(SettingsPages.all(),
                page -> this.minecraft.setScreen(SettingsScreen.create(this.root, page.id())));
    }

    @Override
    protected Component brand() {
        return Component.translatable(PREFIX + "brand");
    }

    @Override
    protected PixelIcons.Icon brandIcon() {
        return PixelIcons.Icon.GEAR;
    }

    @Override
    protected int accent() {
        return COLOR;
    }

    @Override
    protected Component subtitle() {
        if (!SettingsPages.serverEditable()) {
            return Component.translatable(PREFIX + "locked").withColor(WARN);
        }
        return Component.translatable(KEY + "about");
    }

    @Override
    protected void initPage() {
        boolean editable = SettingsPages.serverEditable();
        this.seen = SavedSettings.changed();
        this.exportY = this.contentY + 4;
        int right = this.contentX + this.contentWidth;
        this.name = new EditBox(this.font, this.contentX, this.exportY + 13, this.contentWidth - 92, 18,
                Component.translatable(KEY + "name"));
        this.name.setMaxLength(40);
        this.name.setValue(this.typed);
        this.name.setResponder(text -> this.typed = text);
        this.addRenderableWidget(this.name);
        Button export = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "export"),
                button -> SavedSettings.export(this.typed)).tooltip(Tooltip.create(Component.translatable(KEY
                        + "export.desc"))).bounds(right - 86, this.exportY + 12, 86, 20).build());
        export.active = editable;
        this.importY = this.exportY + 44;
        int bottom = this.contentY + this.contentHeight - 24;
        int fits = Math.max(0, (bottom - (this.importY + 14)) / ROW);
        List<SavedSettings.Saved> saved = SavedSettings.list();
        this.rows.clear();
        for (int k = 0; k < Math.min(fits, saved.size()); k++) {
            SavedSettings.Saved file = saved.get(k);
            int y = this.importY + 14 + k * ROW;
            Button load = this.addRenderableWidget(Button.builder(Component.translatable(KEY + "import"),
                    button -> this.load(file)).tooltip(Tooltip.create(Component.translatable(KEY + "import.desc")))
                    .bounds(right - 86, y, 86, 20).build());
            load.active = editable;
            this.rows.add(new Row(file, y, load));
        }
        this.more = Math.max(0, saved.size() - this.rows.size());
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "folder"),
                button -> SavedSettings.openFolder()).tooltip(Tooltip.create(Component.translatable(KEY
                        + "folder.desc"))).bounds(this.contentX, this.contentY + this.contentHeight - 20, 96, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "refresh"),
                button -> this.rebuildWidgets()).bounds(this.contentX + 100, this.contentY + this.contentHeight - 20,
                        64, 20).build());
        this.relabel();
    }

    // A first click asks, a second within SURE loads it.
    private void load(SavedSettings.Saved file) {
        long now = Util.getMillis();
        if (this.asked != null && this.asked.path().equals(file.path()) && now - this.askedAt < SURE) {
            this.asked = null;
            SavedSettings.send(file);
        } else {
            this.asked = file;
            this.askedAt = now;
        }
        this.relabel();
    }

    private void relabel() {
        boolean waiting = this.asked != null && Util.getMillis() - this.askedAt < SURE;
        for (Row row : this.rows) {
            boolean sure = waiting && row.saved().path().equals(this.asked.path());
            row.button().setMessage(sure ? Component.translatable(KEY + "sure").withStyle(ChatFormatting.YELLOW)
                    : Component.translatable(KEY + "import"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (SavedSettings.changed() != this.seen) {
            this.rebuildWidgets();
        }
        if (this.asked != null && Util.getMillis() - this.askedAt >= SURE) {
            this.asked = null;
            this.relabel();
        }
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.drawString(this.font, Component.translatable(KEY + "export.title").withStyle(ChatFormatting.BOLD),
                this.contentX, this.exportY, COLOR, false);
        graphics.drawString(this.font, Component.translatable(KEY + "import.title").withStyle(ChatFormatting.BOLD),
                this.contentX, this.importY, COLOR, false);
        ScreenAnchors.report("share.export", this.contentX, this.exportY, this.contentWidth, 34);
        ScreenAnchors.report("share.import", this.contentX, this.importY, this.contentWidth,
                Math.max(14, this.rows.size() * ROW + 14));
        int textWidth = this.contentWidth - 96;
        if (this.rows.isEmpty()) {
            this.wrap(graphics, Component.translatable(KEY + "none"), this.contentX, this.importY + 16, textWidth,
                    MUTED, 3);
        }
        for (Row row : this.rows) {
            graphics.drawString(this.font, this.fit(Component.literal(row.saved().name()), textWidth), this.contentX,
                    row.y() + 2, BODY, false);
            graphics.drawString(this.font, WHEN.format(row.saved().when().toInstant()), this.contentX, row.y() + 12,
                    DIM, false);
        }
        int bottom = this.contentY + this.contentHeight - 20;
        if (this.more > 0) {
            graphics.drawString(this.font, Component.translatable(KEY + "more", this.more), this.contentX,
                    bottom - 12, MUTED, false);
        }
        Component status = SavedSettings.status();
        if (status != null) {
            graphics.drawString(this.font, this.fit(status, this.contentWidth - 172), this.contentX + 170, bottom + 6,
                    BODY, false);
        }
    }
}
