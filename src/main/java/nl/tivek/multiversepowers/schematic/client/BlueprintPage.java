package nl.tivek.multiversepowers.schematic.client;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprint;
import nl.tivek.multiversepowers.schematic.client.blueprint.ClientBlueprints;
import nl.tivek.multiversepowers.schematic.net.BlueprintAskPayload;

// The blueprints page: every blueprint set down in this world. A click picks one: its plan small and turning, where it
// stands, how far it is built and, per kind of block, how many it needs, how many are still to be placed and how many
// you carry. It can be hidden for yourself, built for real, or taken away (asked twice).
final class BlueprintPage extends SchematicScreen {
    private static final int ROW = 14;
    private static final int LINE = 18;
    private static final int SURE = 3000;
    private static final int GOOD = 0xFF8CE08C;
    private static final int BAD = 0xFFEF7B6E;
    private static final int SOFT = 0xFFE9C46A;

    @Nullable
    private static Integer selected;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int sideX;
    private int sideWidth;
    private int linesY;
    private int linesHeight;
    private double scroll;
    private double linesScroll;
    private long seen;
    private long askedAt;
    @Nullable
    private Button hide;
    @Nullable
    private Button build;
    @Nullable
    private Button remove;

    BlueprintPage(@Nullable Screen root) {
        super(root, BLUEPRINTS);
    }

    @Override
    protected Component subtitle() {
        return Component.translatable(PREFIX + "blueprints.about");
    }

    @Override
    protected void initPage() {
        this.seen = ClientBlueprints.listed();
        if (this.chosen() == null) {
            List<Blueprint> all = ClientBlueprints.list();
            selected = all.isEmpty() ? null : all.get(0).id();
        }
        int side = Math.max(150, this.contentWidth * 3 / 5);
        this.listX = this.contentX;
        this.listY = this.contentY;
        this.listWidth = this.contentWidth - side - 8;
        int bottom = this.contentY + this.contentHeight - 34;
        this.listHeight = bottom - this.listY;
        this.sideX = this.listX + this.listWidth + 8;
        this.sideWidth = this.contentX + this.contentWidth - this.sideX;
        int y = this.contentY + this.contentHeight - 30;
        int third = (this.sideWidth - 6) / 3;
        this.hide = this.addRenderableWidget(Button.builder(Component.empty(), button -> {
            Integer id = selected;
            if (id != null) {
                ClientBlueprints.hide(id, !ClientBlueprints.hidden(id));
                this.relabel();
            }
        }).tooltip(tip(Component.translatable(PREFIX + "blueprint.hide.desc"))).bounds(this.sideX, y - 1, third, 20)
                .build());
        this.build = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "blueprint.build"),
                button -> this.ask(BlueprintAskPayload.BUILD)).tooltip(tip(Component.translatable(PREFIX
                        + "blueprint.build.desc"))).bounds(this.sideX + third + 3, y - 1, third, 20).build());
        this.remove = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "blueprint.remove"),
                button -> this.remove()).tooltip(tip(Component.translatable(PREFIX + "blueprint.remove.desc")))
                .bounds(this.sideX + (third + 3) * 2, y - 1, this.sideWidth - (third + 3) * 2, 20).build());
        this.relabel();
    }

    @Nullable
    private Blueprint chosen() {
        Integer id = selected;
        if (id == null) {
            return null;
        }
        for (Blueprint blueprint : ClientBlueprints.list()) {
            if (blueprint.id() == id) {
                return blueprint;
            }
        }
        return null;
    }

    private void ask(int action) {
        Blueprint blueprint = this.chosen();
        if (blueprint != null) {
            PacketDistributor.sendToServer(new BlueprintAskPayload(action, blueprint.id()));
        }
    }

    private void remove() {
        long now = Util.getMillis();
        if (now - this.askedAt < SURE) {
            this.ask(BlueprintAskPayload.REMOVE);
            this.askedAt = 0L;
        } else {
            this.askedAt = now;
        }
        this.relabel();
    }

    private void relabel() {
        Blueprint blueprint = this.chosen();
        boolean any = blueprint != null;
        if (this.hide != null) {
            this.hide.active = any;
            this.hide.setMessage(Component.translatable(PREFIX + (any && ClientBlueprints.hidden(blueprint.id())
                    ? "blueprint.show" : "blueprint.hide")));
        }
        if (this.build != null) {
            this.build.active = any;
        }
        if (this.remove != null) {
            boolean sure = Util.getMillis() - this.askedAt < SURE;
            this.remove.active = any;
            this.remove.setMessage(sure ? Component.translatable(PREFIX + "delete.sure").withStyle(
                    ChatFormatting.YELLOW) : Component.translatable(PREFIX + "blueprint.remove"));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (ClientBlueprints.listed() != this.seen) {
            this.rebuildWidgets();
            return;
        }
        Blueprint blueprint = this.chosen();
        if (blueprint != null) {
            ClientBlueprints.want(blueprint.id());
        }
        if (this.askedAt != 0L && Util.getMillis() - this.askedAt >= SURE) {
            this.askedAt = 0L;
            this.relabel();
        }
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ScreenAnchors.report("schematic.blueprints", this.listX, this.listY, this.listWidth, this.listHeight);
        graphics.fill(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.listHeight, 0x40000000);
        List<Blueprint> all = ClientBlueprints.list();
        if (all.isEmpty()) {
            this.wrap(graphics, Component.translatable(PREFIX + "blueprints.empty"), this.listX + 6, this.listY + 6,
                    this.listWidth - 12, MUTED, 8);
        }
        graphics.enableScissor(this.listX, this.listY, this.listX + this.listWidth, this.listY + this.listHeight);
        int top = this.listY + 2 - (int) Math.round(this.scroll);
        for (int k = 0; k < all.size(); k++) {
            int y = top + k * ROW;
            if (y + ROW < this.listY || y > this.listY + this.listHeight) {
                continue;
            }
            Blueprint blueprint = all.get(k);
            boolean picked = selected != null && selected == blueprint.id();
            boolean hover = mouseX >= this.listX && mouseX < this.listX + this.listWidth && mouseY >= y
                    && mouseY < y + ROW && mouseY >= this.listY && mouseY < this.listY + this.listHeight;
            if (picked || hover) {
                graphics.fill(this.listX + 1, y, this.listX + this.listWidth - 1, y + ROW, picked ? 0x507FD3FF
                        : 0x20FFFFFF);
            }
            boolean hidden = ClientBlueprints.hidden(blueprint.id());
            PixelIcons.draw(graphics, PixelIcons.Icon.PLAN, this.listX + 4, y + 1, 1, hidden ? 0xFF5E5E5E
                    : 0xFF7FD3FF, false);
            ClientBlueprints.Progress progress = ClientBlueprints.progress(blueprint.id());
            String done = progress.needed() > 0 ? Math.round(100.0F * progress.placed() / progress.needed()) + "%"
                    : "";
            int doneWidth = this.font.width(done);
            graphics.drawString(this.font, this.fit(Component.literal(blueprint.name()), this.listWidth - doneWidth
                    - 26), this.listX + 19, y + 3, hidden ? MUTED : picked ? TEXT : BODY, false);
            graphics.drawString(this.font, done, this.listX + this.listWidth - doneWidth - 4, y + 3,
                    progress.needed() > 0 && progress.placed() >= progress.needed() ? GOOD : DIM, false);
        }
        graphics.disableScissor();
        Blueprint blueprint = this.chosen();
        if (blueprint != null) {
            this.details(graphics, blueprint, mouseX, mouseY);
        }
        this.status(graphics, this.contentX, this.contentY + this.contentHeight - 8, this.contentWidth);
    }

    // The chosen one: its plan turning, what and where it is, how far it is built, and its blocks.
    private void details(GuiGraphics graphics, Blueprint blueprint, int mouseX, int mouseY) {
        ClientBlueprints.Progress progress = ClientBlueprints.progress(blueprint.id());
        int box = Math.min(this.sideWidth / 2 - 4, Math.max(60, (this.listHeight - 40) / 2));
        card(graphics, this.sideX, this.listY, box, box, COLOR);
        if (progress.schematic() != null) {
            PreviewModel.BLUEPRINT.build(progress.schematic());
        }
        Component note = progress.failed() ? Component.translatable(PREFIX + "blueprint.failed")
                : progress.schematic() == null ? Component.translatable(PREFIX + "blueprint.loading")
                        : PreviewModel.BLUEPRINT.building() ? Component.translatable(PREFIX + "preview.building")
                                : null;
        if (note != null) {
            this.wrap(graphics, note, this.sideX + 6, this.listY + box / 2 - 8, box - 12, progress.failed() ? BAD
                    : MUTED, 3);
        } else if (PreviewModel.BLUEPRINT.shown() == progress.schematic()) {
            turning(graphics, PreviewModel.BLUEPRINT, this.sideX, this.listY, box, box);
        }
        int x = this.sideX + box + 8;
        int width = this.sideX + this.sideWidth - x;
        int y = this.listY;
        graphics.drawString(this.font, this.fit(Component.literal(blueprint.name()), width), x, y, TEXT, false);
        y += 11;
        graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "blueprint.by", blueprint.owner()),
                width), x, y, MUTED, false);
        y += 10;
        graphics.drawString(this.font, Component.translatable(PREFIX + "size", blueprint.wideX(), blueprint.sizeY(),
                blueprint.wideZ()), x, y, MUTED, false);
        y += 10;
        BlockPos at = blueprint.origin();
        graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "blueprint.at", at.getX(),
                at.getY(), at.getZ()), width), x, y, MUTED, false);
        y += 10;
        if (this.minecraft.level != null && !this.minecraft.level.dimension().location().equals(blueprint.level())) {
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "blueprint.elsewhere",
                    blueprint.level().getPath()), width), x, y, 0xFF000000 | WARN, false);
            y += 10;
        }
        if (progress.needed() > 0) {
            float done = progress.placed() / (float) progress.needed();
            y += 3;
            bar(graphics, x, y, width, done, done >= 1.0F ? 0x8CE08C : COLOR);
            y += 7;
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "blueprint.done",
                    String.format("%,d", progress.placed()), String.format("%,d", progress.needed()),
                    Math.round(done * 100.0F)), width), x, y, BODY, false);
            y += 11;
            y = this.issue(graphics, x, y, width, progress.wrong(), "blueprint.wrong", BAD);
            y = this.issue(graphics, x, y, width, progress.turned(), "blueprint.turned", SOFT);
            y = this.issue(graphics, x, y, width, progress.inTheWay(), "blueprint.in_the_way", BAD);
            if (progress.unseen() != 0) {
                this.issue(graphics, x, y, width, progress.unseen(), progress.unseen() < 0 ? "blueprint.checking"
                        : "blueprint.unseen", MUTED);
            }
        }
        this.linesY = this.listY + box + 6;
        this.linesHeight = this.listY + this.listHeight - this.linesY;
        this.lines(graphics, progress, mouseX, mouseY);
    }

    private int issue(GuiGraphics graphics, int x, int y, int width, int count, String key, int color) {
        if (count == 0) {
            return y;
        }
        graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + key, String.format("%,d",
                Math.abs(count))), width), x, y, color, false);
        return y + 10;
    }

    // Per kind of block: its item, how many are still to place of how many, and how many you carry.
    private void lines(GuiGraphics graphics, ClientBlueprints.Progress progress, int mouseX, int mouseY) {
        int x = this.sideX;
        int y = this.linesY;
        int width = this.sideWidth;
        ScreenAnchors.report("schematic.materials", x, y, width, this.linesHeight);
        graphics.fill(x, y, x + width, y + this.linesHeight, 0x40000000);
        Component head = Component.translatable(PREFIX + "blueprint.materials");
        graphics.drawString(this.font, head, x + 4, y + 3, MUTED, false);
        Component columns = Component.translatable(PREFIX + "blueprint.columns");
        graphics.drawString(this.font, columns, x + width - this.font.width(columns) - 6, y + 3, DIM, false);
        int listTop = y + 13;
        int listHeight = this.linesHeight - 13;
        if (progress.lines().isEmpty()) {
            return;
        }
        graphics.enableScissor(x, listTop, x + width, listTop + listHeight);
        int top = listTop + 1 - (int) Math.round(this.linesScroll);
        int carried;
        for (int k = 0; k < progress.lines().size(); k++) {
            int row = top + k * LINE;
            if (row + LINE < listTop || row > listTop + listHeight) {
                continue;
            }
            ClientBlueprints.Line line = progress.lines().get(k);
            carried = this.minecraft.player == null ? 0 : this.minecraft.player.getInventory().countItem(line.item());
            if (mouseX >= x && mouseX < x + width && mouseY >= row && mouseY < row + LINE && mouseY >= listTop) {
                graphics.fill(x + 1, row, x + width - 1, row + LINE, 0x20FFFFFF);
            }
            graphics.renderItem(new ItemStack(line.item()), x + 3, row + 1);
            String counts = String.format("%,d / %,d", line.left(), line.needed());
            String have = String.format("%,d", carried);
            int haveWidth = Math.max(this.font.width(have), 24);
            int countsX = x + width - haveWidth - 10 - this.font.width(counts);
            graphics.drawString(this.font, this.fit(line.item().getDescription(), countsX - x - 30), x + 23, row + 5,
                    line.left() == 0 ? MUTED : BODY, false);
            graphics.drawString(this.font, counts, countsX, row + 5, line.left() == 0 ? GOOD : TEXT, false);
            graphics.drawString(this.font, have, x + width - this.font.width(have) - 5, row + 5, line.left() == 0
                    ? DIM : carried >= line.left() ? GOOD : SOFT, false);
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= this.listX && mouseX < this.listX + this.listWidth && mouseY >= this.listY
                && mouseY < this.listY + this.listHeight) {
            int k = (int) ((mouseY - this.listY - 2 + this.scroll) / ROW);
            List<Blueprint> all = ClientBlueprints.list();
            if (k >= 0 && k < all.size()) {
                selected = all.get(k).id();
                this.linesScroll = 0.0;
                this.askedAt = 0L;
                this.click(1.0F);
                this.relabel();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= this.listX && mouseX < this.listX + this.listWidth) {
            int most = Math.max(0, ClientBlueprints.list().size() * ROW + 4 - this.listHeight);
            this.scroll = Mth.clamp(this.scroll - scrollY * ROW * 2, 0.0, most);
            return true;
        }
        Blueprint blueprint = this.chosen();
        if (blueprint != null && mouseX >= this.sideX && mouseY >= this.linesY) {
            int lines = ClientBlueprints.progress(blueprint.id()).lines().size();
            int most = Math.max(0, lines * LINE + 2 - (this.linesHeight - 13));
            this.linesScroll = Mth.clamp(this.linesScroll - scrollY * LINE * 2, 0.0, most);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
