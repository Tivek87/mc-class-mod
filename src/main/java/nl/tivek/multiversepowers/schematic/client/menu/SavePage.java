package nl.tivek.multiversepowers.schematic.client.menu;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.schematic.SchematicRules;
import nl.tivek.multiversepowers.schematic.client.AreaSelection;
import nl.tivek.multiversepowers.schematic.client.SchematicClient;
import nl.tivek.multiversepowers.schematic.net.SchematicSavePayload;

// The save page: an area of the world made a schematic of your own. Select in world lets the wand pick its two
// corners; back here (sneak and right click), it is named and saved in the library folder open now, and may be set
// down as a blueprint where it stands at once.
final class SavePage extends SchematicScreen {
    private static String typed = "";
    private static boolean blueprint;
    private static boolean creatures;
    private int sideX;
    private int sideWidth;
    @Nullable
    private Button save;

    SavePage(@Nullable Screen root) {
        super(root, SAVE);
    }

    @Override
    protected Component subtitle() {
        return Component.translatable(PREFIX + "save.about");
    }

    @Override
    protected void initPage() {
        int side = Math.max(140, this.contentWidth / 2);
        this.sideX = this.contentX + this.contentWidth - side;
        this.sideWidth = side;
        int y = this.contentY + 11;
        EditBox name = new EditBox(this.font, this.sideX, y, this.sideWidth, 18, Component.translatable(PREFIX
                + "save.name"));
        name.setHint(Component.translatable(PREFIX + "save.name").withStyle(ChatFormatting.DARK_GRAY));
        name.setMaxLength(48);
        name.setValue(typed);
        name.setResponder(text -> {
            typed = text;
            this.relabel();
        });
        this.addRenderableWidget(name);
        y += 34;
        this.toggle(y, "save.blueprint", () -> blueprint, () -> blueprint = !blueprint);
        y += 21;
        this.toggle(y, "save.creatures", () -> creatures, () -> creatures = !creatures);
        int bottom = this.contentY + this.contentHeight - 30;
        int half = (this.sideWidth - 3) / 2;
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + (AreaSelection.on()
                ? "save.selecting" : "save.select")), button -> {
                    AreaSelection.start();
                    this.minecraft.setScreen(null);
                }).tooltip(tip(Component.translatable(PREFIX + "save.select.desc")))
                .bounds(this.sideX, bottom - 24, half, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save.clear"), button -> {
            AreaSelection.clear();
            this.relabel();
        }).bounds(this.sideX + half + 3, bottom - 24, this.sideWidth - half - 3, 20).build());
        this.save = this.addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save.save"),
                button -> this.save()).tooltip(tip(Component.translatable(PREFIX + "save.save.desc")))
                .bounds(this.sideX, bottom - 1, this.sideWidth, 20).build());
        this.relabel();
    }

    private void toggle(int y, String id, BooleanSupplier now, Runnable flip) {
        Supplier<Component> label = () -> Component.translatable(PREFIX + id, Component.translatable(
                PREFIX + "view." + (now.getAsBoolean() ? "on" : "off"))).withColor(now.getAsBoolean() ? 0x8CE08C
                        : 0xA0A0A0);
        Button button = Button.builder(label.get(), pressed -> {
            flip.run();
            pressed.setMessage(label.get());
        }).tooltip(tip(Component.translatable(PREFIX + id + ".desc"))).bounds(this.sideX, y, this.sideWidth, 18)
                .build();
        this.addRenderableWidget(button);
    }

    private void relabel() {
        if (this.save != null) {
            this.save.active = AreaSelection.complete() && !AreaSelection.tooBig() && !typed.isBlank();
        }
    }

    private void save() {
        if (!AreaSelection.complete() || typed.isBlank()) {
            return;
        }
        PacketDistributor.sendToServer(new SchematicSavePayload(AreaSelection.first, AreaSelection.second,
                SchematicClient.folder, typed.trim(), (blueprint ? SchematicSavePayload.BLUEPRINT : 0) | (creatures
                        ? SchematicSavePayload.CREATURES : 0)));
        AreaSelection.stop();
        this.click(1.2F);
        this.rebuildWidgets();
    }

    @Override
    public void tick() {
        super.tick();
        this.relabel();
    }

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = this.contentX;
        int width = this.sideX - 10 - x;
        int y = this.contentY;
        int lines = 0;
        for (int step = 1; step <= 4; step++) {
            lines += Math.min(2, this.font.split(Component.translatable(PREFIX + "save.step." + step), width - 12)
                    .size());
        }
        int height = lines * 10 + 13;
        card(graphics, x, y, width, height, COLOR);
        int line = y + 5;
        for (int step = 1; step <= 4; step++) {
            line = this.wrap(graphics, Component.translatable(PREFIX + "save.step." + step), x + 7, line, width - 12,
                    BODY, 2) + 1;
        }
        y += height + 7;
        graphics.drawString(this.font, Component.translatable(PREFIX + "save.area"), x, y, MUTED, false);
        y += 11;
        y = this.corner(graphics, "save.first", AreaSelection.first, x, y, 0xFF66FF73);
        y = this.corner(graphics, "save.second", AreaSelection.second, x, y, 0xFF66B3FF);
        if (AreaSelection.complete()) {
            int[] size = AreaSelection.size();
            graphics.drawString(this.font, Component.translatable(PREFIX + "size", size[0], size[1], size[2]), x, y,
                    BODY, false);
            y += 10;
            boolean big = AreaSelection.tooBig();
            y = this.wrap(graphics, Component.translatable(PREFIX + "save.blocks", String.format("%,d",
                    AreaSelection.volume()), String.format("%,d", SchematicRules.largestSave())), x, y, width, big
                            ? 0xFFEF7B6E : MUTED, 2);
            if (big) {
                this.wrap(graphics, Component.translatable(PREFIX + "save.too_big"), x, y, width, 0xFFEF7B6E, 2);
            }
        }
        ScreenAnchors.report("schematic.save", this.sideX, this.contentY, this.sideWidth, 100);
        graphics.drawString(this.font, Component.translatable(PREFIX + "save.name"), this.sideX, this.contentY,
                MUTED, false);
        String folder = SchematicClient.folder.isEmpty() ? Component.translatable(PREFIX + "root").getString()
                : Component.translatable(PREFIX + "root").getString() + "/" + SchematicClient.folder;
        graphics.drawString(this.font, this.fit(Component.translatable(PREFIX + "save.folder", folder),
                this.sideWidth), this.sideX, this.contentY + 32, DIM, false);
        this.status(graphics, this.contentX, this.contentY + this.contentHeight - 8, this.contentWidth);
    }

    private int corner(GuiGraphics graphics, String key, @Nullable BlockPos at, int x, int y, int color) {
        Component text = at == null ? Component.translatable(PREFIX + key, Component.translatable(PREFIX
                + "save.unset")) : Component.translatable(PREFIX + key, at.getX() + " " + at.getY() + " " + at.getZ());
        graphics.fill(x, y + 1, x + 6, y + 7, color);
        graphics.drawString(this.font, text, x + 10, y, at == null ? MUTED : BODY, false);
        return y + 10;
    }
}
