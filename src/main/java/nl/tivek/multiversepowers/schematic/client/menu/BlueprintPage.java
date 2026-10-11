package nl.tivek.multiversepowers.schematic.client.menu;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.blueprint.Blueprint;
import nl.tivek.multiversepowers.schematic.client.blueprint.BlueprintViews;
import nl.tivek.multiversepowers.schematic.client.blueprint.ClientBlueprints;
import nl.tivek.multiversepowers.schematic.net.BlueprintAskPayload;
import org.lwjgl.glfw.GLFW;

// The blueprints page: every blueprint set down in this world. A click picks one: on Plan its plan small and turning,
// where it stands, how far it is built and how you see it (shown, its box, a layer at a time, a guide to the next
// block: only for you); on Materials what it still needs (BlueprintMaterials). It can be renamed, built for real, or
// taken away (asked twice).
final class BlueprintPage extends BlueprintMaterials {
    private static final int ROW = 14;
    private static final int SURE = 3000;
    private static final int PLAN = 0;
    private static final int MATERIALS = 1;

    @Nullable
    private static Integer selected;
    private static int tab;
    private int listX;
    private int listY;
    private int listWidth;
    private int listHeight;
    private int sideX;
    private int sideWidth;
    private int viewsY;
    private double scroll;
    private long seen;
    private long askedAt;
    private boolean naming;
    private String typed = "";
    @Nullable
    private EditBox name;
    @Nullable
    private Button build;
    @Nullable
    private Button remove;
    private final List<View> views = new ArrayList<>();

    BlueprintPage(@Nullable Screen root) {
        super(root, BLUEPRINTS);
    }

    @Override
    protected Component subtitle() {
        return Component.translatable(PREFIX + "blueprints.about");
    }

    @Override
    protected List<Tab> tabs() {
        return List.of(this.tab(PLAN, "plan"), this.tab(MATERIALS, "materials"));
    }

    private Tab tab(int which, String id) {
        return new Tab(id, Component.translatable(PREFIX + "blueprints.tab." + id), null, tab == which, () -> {
            tab = which;
            this.naming = false;
            this.click(1.0F);
            this.rebuildWidgets();
        });
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
        this.views.clear();
        this.build = null;
        this.remove = null;
        this.name = null;
        if (tab == MATERIALS) {
            return;
        }
        int y = this.contentY + this.contentHeight - 30;
        if (this.naming) {
            this.name = new EditBox(this.font, this.sideX, y, this.sideWidth - 112, 18,
                    Component.translatable(PREFIX + "rename.name"));
            this.name.setMaxLength(BlueprintAskPayload.NAME);
            this.name.setValue(this.typed);
            this.name.setResponder(text -> this.typed = text);
            this.addRenderableWidget(this.name);
            this.setInitialFocus(this.name);
            this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "rename.done"),
                    button -> this.named()).bounds(this.sideX + this.sideWidth - 108, y - 1, 52, 20).build());
            this.addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), button -> {
                this.naming = false;
                this.rebuildWidgets();
            }).bounds(this.sideX + this.sideWidth - 52, y - 1, 52, 20).build());
        } else {
            int third = (this.sideWidth - 6) / 3;
            Button rename = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "rename"),
                    button -> {
                        Blueprint blueprint = this.chosen();
                        if (blueprint != null) {
                            this.naming = true;
                            this.typed = blueprint.name();
                            this.rebuildWidgets();
                        }
                    }).tooltip(tip(Component.translatable(PREFIX + "blueprint.rename.desc")))
                    .bounds(this.sideX, y - 1, third, 20).build());
            rename.active = this.chosen() != null;
            this.build = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "blueprint.build"),
                    button -> this.ask(BlueprintAskPayload.BUILD)).tooltip(tip(Component.translatable(PREFIX
                            + "blueprint.build.desc"))).bounds(this.sideX + third + 3, y - 1, third, 20).build());
            this.remove = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "blueprint.remove"),
                    button -> this.remove()).tooltip(tip(Component.translatable(PREFIX + "blueprint.remove.desc")))
                    .bounds(this.sideX + (third + 3) * 2, y - 1, this.sideWidth - (third + 3) * 2, 20).build());
        }
        this.viewsY = this.listY + this.listHeight - 38;
        int half = (this.sideWidth - 3) / 2;
        this.view(this.sideX, this.viewsY, half, "shown", BlueprintViews.HIDDEN, true);
        this.view(this.sideX + half + 3, this.viewsY, this.sideWidth - half - 3, "box", BlueprintViews.NO_BOX, true);
        this.view(this.sideX, this.viewsY + 20, half, "layers", BlueprintViews.LAYERS, false);
        this.view(this.sideX + half + 3, this.viewsY + 20, this.sideWidth - half - 3, "guide", BlueprintViews.GUIDE,
                false);
        this.relabel();
    }

    // The preview's size: half the side, at most as tall as half the list.
    private int box() {
        return Math.min(this.sideWidth / 2 - 4, Math.max(60, (this.listHeight - 60) / 2));
    }

    // A switch of how this player sees the chosen blueprint; `inverted`: the flag is set while the switch is off.
    private void view(int x, int y, int width, String id, int flag, boolean inverted) {
        Button button = Button.builder(Component.empty(), pressed -> {
            Blueprint blueprint = this.chosen();
            if (blueprint != null) {
                BlueprintViews.toggle(blueprint.id(), flag);
                this.click(1.0F);
                this.relabel();
            }
        }).tooltip(tip(Component.translatable(PREFIX + "view." + id + ".desc"))).bounds(x, y, width, 18).build();
        this.views.add(new View(this.addRenderableWidget(button), id, flag, inverted));
    }

    private record View(Button button, String id, int flag, boolean inverted) {
    }

    private void named() {
        Blueprint blueprint = this.chosen();
        if (blueprint != null && !this.typed.isBlank() && !this.typed.trim().equals(blueprint.name())) {
            PacketDistributor.sendToServer(new BlueprintAskPayload(BlueprintAskPayload.RENAME, blueprint.id(),
                    this.typed.trim()));
        }
        this.naming = false;
        this.rebuildWidgets();
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (this.naming && this.name != null && this.name.isFocused() && (key == GLFW.GLFW_KEY_ENTER
                || key == GLFW.GLFW_KEY_KP_ENTER)) {
            this.named();
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
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
        for (View view : this.views) {
            boolean on = any && BlueprintViews.has(blueprint.id(), view.flag()) != view.inverted();
            view.button().active = any;
            view.button().setMessage(Component.translatable(PREFIX + "view." + view.id(), Component.translatable(
                    PREFIX + "view." + (on ? "on" : "off"))).withColor(on ? 0x8CE08C : 0xA0A0A0));
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
        if (tab == MATERIALS) {
            Blueprint blueprint = this.chosen();
            if (blueprint == null) {
                this.wrap(graphics, Component.translatable(PREFIX + "blueprints.empty"), this.contentX, this.contentY,
                        this.contentWidth, MUTED, 4);
            } else {
                this.materials(graphics, blueprint, ClientBlueprints.progress(blueprint.id()), this.contentX,
                        this.contentY, this.contentWidth, this.contentHeight - 14, mouseX, mouseY);
            }
            this.status(graphics, this.contentX, this.contentY + this.contentHeight - 8, this.contentWidth);
            return;
        }
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
            boolean hidden = BlueprintViews.has(blueprint.id(), BlueprintViews.HIDDEN);
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
        int box = this.box();
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
        // Most needed first: what does not fit above the view switches is left out.
        int limit = this.viewsY - 12;
        int y = this.listY;
        graphics.drawString(this.font, this.fit(Component.literal(blueprint.name()), width), x, y, TEXT, false);
        y += 11;
        if (this.minecraft.level != null && !this.minecraft.level.dimension().location().equals(blueprint.level())) {
            y = this.line(graphics, Component.translatable(PREFIX + "blueprint.elsewhere", blueprint.level().getPath()),
                    x, y, width, 0xFF000000 | WARN, limit);
        }
        if (progress.needed() > 0 && y + 21 <= limit) {
            float done = progress.placed() / (float) progress.needed();
            y += 3;
            bar(graphics, x, y, width, done, done >= 1.0F ? 0x8CE08C : COLOR);
            y += 7;
            graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "blueprint.done",
                    String.format("%,d", progress.placed()), String.format("%,d", progress.needed()),
                    Math.round(done * 100.0F)), width), x, y, BODY, false);
            y += 11;
            int layer = ClientBlueprints.layer(blueprint.id());
            if (BlueprintViews.has(blueprint.id(), BlueprintViews.LAYERS) && layer >= 0) {
                y = this.line(graphics, layer >= blueprint.sizeY() ? Component.translatable(PREFIX
                        + "blueprint.layers_done") : Component.translatable(PREFIX + "blueprint.layer", layer + 1,
                                blueprint.sizeY()), x, y, width, 0xFF000000 | COLOR, limit);
            }
        }
        if (progress.schematic() != null && y + 14 <= limit) {
            y = this.missing(graphics, progress.schematic(), x, y + 2, width, mouseX, mouseY);
        }
        if (progress.needed() > 0) {
            y = this.issue(graphics, x, y, width, progress.wrong(), "blueprint.wrong", BAD, limit);
            y = this.issue(graphics, x, y, width, progress.turned(), "blueprint.turned", SOFT, limit);
            y = this.issue(graphics, x, y, width, progress.inTheWay(), "blueprint.in_the_way", BAD, limit);
            if (progress.unseen() != 0) {
                y = this.issue(graphics, x, y, width, progress.unseen(), progress.unseen() < 0
                        ? "blueprint.checking" : "blueprint.unseen", MUTED, limit);
            }
        }
        y = this.line(graphics, Component.translatable(PREFIX + "blueprint.by", blueprint.owner()), x, y, width,
                MUTED, limit);
        y = this.line(graphics, Component.translatable(PREFIX + "size", blueprint.wideX(), blueprint.sizeY(),
                blueprint.wideZ()), x, y, width, MUTED, limit);
        BlockPos at = blueprint.origin();
        y = this.line(graphics, Component.translatable(PREFIX + "blueprint.at", at.getX(), at.getY(), at.getZ()), x,
                y, width, MUTED, limit);
        if (progress.schematic() != null && !progress.lines().isEmpty() && y + 12 <= limit) {
            this.link(graphics, Component.translatable(PREFIX + "blueprint.see_materials"), x, y + 2, mouseX, mouseY,
                    0xFF000000 | COLOR, () -> {
                        tab = MATERIALS;
                        this.click(1.0F);
                        this.rebuildWidgets();
                    });
        }
        graphics.drawString(this.font, Component.translatable(PREFIX + "view.title"), this.sideX, this.viewsY - 10,
                MUTED, false);
    }

    private int issue(GuiGraphics graphics, int x, int y, int width, int count, String key, int color, int limit) {
        if (count == 0) {
            return y;
        }
        return this.line(graphics, Component.translatable(PREFIX + key, String.format("%,d", Math.abs(count))), x, y,
                width, color, limit);
    }

    // One line of facts, while it fits above `limit`.
    private int line(GuiGraphics graphics, Component text, int x, int y, int width, int color, int limit) {
        if (y + 10 > limit) {
            return y;
        }
        graphics.drawString(this.font, this.fit(text, width), x, y, color, false);
        return y + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (tab == PLAN && button == 0 && mouseX >= this.listX && mouseX < this.listX + this.listWidth
                && mouseY >= this.listY && mouseY < this.listY + this.listHeight) {
            int k = (int) ((mouseY - this.listY - 2 + this.scroll) / ROW);
            List<Blueprint> all = ClientBlueprints.list();
            if (k >= 0 && k < all.size()) {
                selected = all.get(k).id();
                this.resetMaterials();
                this.askedAt = 0L;
                this.naming = false;
                this.click(1.0F);
                this.rebuildWidgets();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        Blueprint blueprint = this.chosen();
        if (tab == MATERIALS) {
            if (blueprint != null && this.scrollMaterials(ClientBlueprints.progress(blueprint.id()), mouseY, scrollY)) {
                return true;
            }
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        if (mouseX >= this.listX && mouseX < this.listX + this.listWidth) {
            int most = Math.max(0, ClientBlueprints.list().size() * ROW + 4 - this.listHeight);
            this.scroll = Mth.clamp(this.scroll - scrollY * ROW * 2, 0.0, most);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
