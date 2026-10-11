package nl.tivek.multiversepowers.schematic.client;

import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.Schematic;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.net.SchematicAskPayload;
import nl.tivek.multiversepowers.schematic.paste.SchematicPastes;

// The place page: how the chosen schematic goes into the world (turned, mirrored, how its hologram follows, what it
// replaces and what comes along), then Show hologram brings it into the world to be placed with a left click.
final class PlacePage extends SchematicScreen {
    private int optionsX;
    private int optionsWidth;

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
        int y = this.contentY;
        y = this.option(y, "turn", () -> Component.translatable(PREFIX + "turn." + Hologram.rotation.ordinal()),
                () -> Hologram.rotate());
        y = this.option(y, "mirror", () -> Component.translatable(PREFIX + "mirror." + Hologram.mirror.ordinal()),
                () -> Hologram.mirror());
        y = this.option(y, "follow", () -> Component.translatable(PREFIX + "follow." + Hologram.follow),
                () -> Hologram.follow = 1 - Hologram.follow);
        y = this.option(y, "replace", () -> Component.translatable(PREFIX + "replace." + Hologram.mode),
                () -> Hologram.mode = (Hologram.mode + 1) % 3);
        y = this.flag(y, "creatures", SchematicPastes.CREATURES);
        y = this.flag(y, "contents", SchematicPastes.CONTENTS);
        y = this.flag(y, "physics", SchematicPastes.PHYSICS);
        int bottom = this.contentY + this.contentHeight - 30;
        Schematic chosen = SchematicClient.viewed;
        boolean same = chosen != null && chosen == Hologram.schematic;
        Button show = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + (same ? "show.back"
                : "show")), button -> this.show()).tooltip(tip(Component.translatable(PREFIX + "show.desc")))
                .bounds(this.optionsX, bottom - 1, this.optionsWidth, 20).build());
        show.active = chosen != null && SchematicClient.viewedPath != null;
        int half = (this.optionsWidth - 3) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "undo"),
                button -> SchematicClient.ask(SchematicAskPayload.UNDO, "", ""))
                .tooltip(tip(Component.translatable(PREFIX + "undo.desc"))).bounds(this.optionsX, bottom - 24, half,
                        20).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "cancel"),
                button -> SchematicClient.ask(SchematicAskPayload.CANCEL, "", ""))
                .tooltip(tip(Component.translatable(PREFIX + "cancel.desc"))).bounds(this.optionsX + half + 3,
                        bottom - 24, half, 20).build());
    }

    // A button that steps through the choices of one option, saying the one chosen.
    private int option(int y, String id, Supplier<Component> now, Runnable next) {
        Button button = Button.builder(this.label(id, now.get()), pressed -> {
            next.run();
            pressed.setMessage(this.label(id, now.get()));
        }).tooltip(tip(Component.translatable(PREFIX + id + ".desc"))).bounds(this.optionsX, y, this.optionsWidth,
                18).build();
        this.addRenderableWidget(button);
        return y + 21;
    }

    private int flag(int y, String id, int bit) {
        return this.option(y, id, () -> Component.translatable((Hologram.flags & bit) != 0 ? "options.on"
                : "options.off"), () -> Hologram.flags ^= bit);
    }

    private Component label(String id, Component value) {
        return Component.translatable(PREFIX + id, value);
    }

    private void show() {
        Schematic chosen = SchematicClient.viewed;
        String path = SchematicClient.viewedPath;
        if (chosen == null || path == null) {
            return;
        }
        if (chosen != Hologram.schematic) {
            Hologram.show(path, chosen);
        }
        this.minecraft.setScreen(null);
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int width = this.optionsX - 8 - this.contentX;
        int box = Math.min(width, this.contentHeight - 60);
        this.preview(graphics, this.contentX, this.contentY, width, box);
        this.facts(graphics, this.contentX, this.contentY + box + 5, width);
        ScreenAnchors.report("schematic.options", this.optionsX, this.contentY, this.optionsWidth, 21 * 7);
        this.status(graphics, this.contentX, this.contentY + this.contentHeight - 8, this.contentWidth);
    }
}
