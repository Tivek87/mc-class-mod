package nl.tivek.multiversepowers.schematic.client.menu;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.client.AreaSelection;
import nl.tivek.multiversepowers.schematic.client.SchematicClient;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.net.SchematicAskPayload;
import nl.tivek.multiversepowers.schematic.paste.SchematicPastes;

// The place page: how the chosen schematic goes into the world (turned, mirrored, how its hologram follows, what it
// replaces and what comes along), then Show hologram brings it into the world to be placed with a left click.
final class PlacePage extends SchematicScreen {
    private int optionsX;
    private int optionsWidth;
    private int optionsHeight;
    private int step = 21;
    private boolean tight;

    PlacePage(@Nullable Screen root) {
        super(root, PLACE);
    }

    @Override
    protected Component subtitle() {
        return Component.translatable(PREFIX + "place.about");
    }

    @Override
    protected void initPage() {
        int side = Math.max(120, this.contentWidth / 2);
        this.optionsX = this.contentX + this.contentWidth - side;
        this.optionsWidth = side;
        this.step = Math.max(17, Math.min(21, (this.contentHeight - 32) / 7));
        this.tight = 7 * this.step + 48 > this.contentHeight - 8;
        int y = this.contentY;
        int half = (this.optionsWidth - 3) / 2;
        this.option(this.optionsX, y, half, "as", () -> Component.translatable(PREFIX + "as." + (Hologram.blueprint
                ? 1 : 0)), () -> Hologram.blueprint = !Hologram.blueprint);
        y = this.option(this.optionsX + half + 3, y, this.optionsWidth - half - 3, "grid",
                Hologram::gridName, Hologram::nextGrid);
        y = this.option(y, "turn", () -> Component.translatable(PREFIX + "turn." + Hologram.rotation.ordinal()),
                () -> Hologram.rotate());
        y = this.option(y, "mirror", () -> Component.translatable(PREFIX + "mirror." + Hologram.mirror.ordinal()),
                () -> Hologram.mirror());
        y = this.option(y, "follow", () -> Component.translatable(PREFIX + "follow." + Hologram.follow),
                () -> Hologram.follow = 1 - Hologram.follow);
        y = this.option(y, "replace", () -> Component.translatable(PREFIX + "replace." + Hologram.mode),
                () -> Hologram.mode = (Hologram.mode + 1) % 3);
        int third = (this.optionsWidth - 6) / 3;
        this.flag(this.optionsX, y, third, "creatures", SchematicPastes.CREATURES);
        this.flag(this.optionsX + third + 3, y, third, "contents", SchematicPastes.CONTENTS);
        y = this.flag(this.optionsX + (third + 3) * 2, y, this.optionsWidth - (third + 3) * 2, "physics",
                SchematicPastes.PHYSICS);
        y = this.layers(y);
        this.optionsHeight = y - this.contentY;
        int bottom = this.contentY + this.contentHeight - 30;
        Schematic chosen = SchematicClient.viewed;
        boolean same = chosen != null && chosen == Hologram.schematic;
        Button show = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + (same ? "show.back"
                : "show")), button -> this.show(false)).tooltip(tip(Component.translatable(PREFIX + "show.desc")))
                .bounds(this.optionsX, bottom - 1, half, 20).build());
        show.active = chosen != null && SchematicClient.viewedPath != null;
        Button last = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "show.last"),
                button -> this.show(true)).tooltip(tip(Component.translatable(PREFIX + "show.last.desc")))
                .bounds(this.optionsX + half + 3, bottom - 1, this.optionsWidth - half - 3, 20).build());
        last.active = show.active && Hologram.placedBefore(SchematicClient.viewedPath);
        // Where the options leave no room above Show, Undo and Stop go under the preview.
        int left = this.optionsX - 8 - this.contentX;
        int undoX = this.tight ? this.contentX : this.optionsX;
        int undoY = this.tight ? bottom - 1 : bottom - 24;
        int undoWidth = this.tight ? (left - 3) / 2 : half;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "undo"),
                button -> SchematicClient.ask(SchematicAskPayload.UNDO, "", ""))
                .tooltip(tip(Component.translatable(PREFIX + "undo.desc"))).bounds(undoX, undoY, undoWidth, 20)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "cancel"),
                button -> SchematicClient.ask(SchematicAskPayload.CANCEL, "", ""))
                .tooltip(tip(Component.translatable(PREFIX + "cancel.desc"))).bounds(undoX + undoWidth + 3, undoY,
                        undoWidth, 20).build());
    }

    // A button that steps through the choices of one option, saying the one chosen.
    private int option(int y, String id, Supplier<Component> now, Runnable next) {
        return this.option(this.optionsX, y, this.optionsWidth, id, now, next);
    }

    private int option(int x, int y, int width, String id, Supplier<Component> now, Runnable next) {
        Button button = Button.builder(this.label(id, now.get()), pressed -> {
            next.run();
            pressed.setMessage(this.label(id, now.get()));
        }).tooltip(tip(Component.translatable(PREFIX + id + ".desc"))).bounds(x, y, width, this.step - 3).build();
        this.addRenderableWidget(button);
        return y + this.step;
    }

    // What comes along, a short name lit green while on.
    private int flag(int x, int y, int width, String id, int bit) {
        Supplier<Component> name = () -> Component.translatable(PREFIX + id + ".short").withColor((Hologram.flags
                & bit) != 0 ? 0x8CE08C : 0x8A8A8A);
        Button button = Button.builder(name.get(), pressed -> {
            Hologram.flags ^= bit;
            pressed.setMessage(name.get());
        }).tooltip(tip(Component.translatable(PREFIX + id + ".desc"))).bounds(x, y, width, this.step - 3).build();
        this.addRenderableWidget(button);
        return y + this.step;
    }

    private Component label(String id, Component value) {
        return Component.translatable(PREFIX + id, value);
    }

    // How many layers the hologram shows, a layer less or more on each side; only while it is shown.
    private int layers(int y) {
        int side = 20;
        Button label = Button.builder(this.label("layers", Hologram.layerName()), pressed -> {
            Hologram.layer(-1);
            pressed.setMessage(this.label("layers", Hologram.layerName()));
        }).tooltip(tip(Component.translatable(PREFIX + "layers.desc"))).bounds(this.optionsX + side + 3, y,
                this.optionsWidth - (side + 3) * 2, this.step - 3).build();
        Button down = Button.builder(Component.literal("-"), pressed -> {
            Hologram.stepLayer(-1);
            label.setMessage(this.label("layers", Hologram.layerName()));
        }).bounds(this.optionsX, y, side, this.step - 3).build();
        Button up = Button.builder(Component.literal("+"), pressed -> {
            Hologram.stepLayer(1);
            label.setMessage(this.label("layers", Hologram.layerName()));
        }).bounds(this.optionsX + this.optionsWidth - side, y, side, this.step - 3).build();
        boolean shown = Hologram.shown() && Hologram.schematic == SchematicClient.viewed;
        down.active = shown;
        up.active = shown;
        label.active = shown;
        this.addRenderableWidget(down);
        this.addRenderableWidget(label);
        this.addRenderableWidget(up);
        return y + this.step;
    }

    // Brings the hologram into the world, following the look, or pinned where this schematic was placed last.
    private void show(boolean last) {
        Schematic chosen = SchematicClient.viewed;
        String path = SchematicClient.viewedPath;
        if (chosen == null || path == null) {
            return;
        }
        AreaSelection.stop();
        if (chosen != Hologram.schematic) {
            Hologram.show(path, chosen);
        }
        if (last) {
            Hologram.toLastSpot();
        }
        this.minecraft.setScreen(null);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int width = this.optionsX - 8 - this.contentX;
        int box = Math.min(width, this.contentHeight - (this.tight ? 84 : 60));
        this.preview(graphics, this.contentX, this.contentY, width, box);
        this.facts(graphics, this.contentX, this.contentY + box + 5, width, mouseX, mouseY);
        ScreenAnchors.report("schematic.options", this.optionsX, this.contentY, this.optionsWidth,
                this.optionsHeight);
        this.status(graphics, this.contentX, this.contentY + this.contentHeight - 8, this.contentWidth);
    }
}
