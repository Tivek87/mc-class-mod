package nl.tivek.multiversepowers.schematic.client.menu;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.schematic.client.SchematicClient;

// The blocks a schematic holds that this game lacks, by the mod they come from: how many of each, and a search for the
// mod on Modrinth (the game asks before it opens the browser). A "minecraft" block comes from a newer game.
public final class MissingBlocksScreen extends Screen {
    private static final String PREFIX = SchematicClient.PREFIX + "missing.";
    private static final int WIDTH = 300;
    private static final int LINE = 11;
    private static final int SHOWN = 5;
    private static final int BAD = 0xFFEF7B6E;

    // One mod's missing blocks, most used first.
    public record Mod(String id, List<Map.Entry<String, Integer>> blocks, int cells) {
    }

    private final Screen back;
    private final String name;
    private final List<Mod> mods;
    private int top;
    private int boxY;
    private int boxHeight;
    private double scroll;
    private int contentHeight;

    public MissingBlocksScreen(Screen back, String name, Map<String, Integer> missing) {
        super(Component.translatable(PREFIX + "title"));
        this.back = back;
        this.name = name;
        this.mods = mods(missing);
    }

    // The missing blocks by mod, the mod missing most blocks first.
    public static List<Mod> mods(Map<String, Integer> missing) {
        Map<String, List<Map.Entry<String, Integer>>> by = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> block : missing.entrySet()) {
            int colon = block.getKey().indexOf(':');
            String mod = colon < 0 ? "minecraft" : block.getKey().substring(0, colon);
            by.computeIfAbsent(mod, key -> new ArrayList<>()).add(Map.entry(colon < 0 ? block.getKey()
                    : block.getKey().substring(colon + 1), block.getValue()));
        }
        List<Mod> mods = new ArrayList<>();
        for (Map.Entry<String, List<Map.Entry<String, Integer>>> mod : by.entrySet()) {
            List<Map.Entry<String, Integer>> blocks = mod.getValue();
            blocks.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
            mods.add(new Mod(mod.getKey(), blocks, blocks.stream().mapToInt(Map.Entry::getValue).sum()));
        }
        mods.sort((a, b) -> Integer.compare(b.cells(), a.cells()));
        return mods;
    }

    // The one-line warning: which mods it needs (or how many, where their names do not fit), or that it needs a
    // newer game.
    public static Component line(Map<String, Integer> missing, Font font, int width) {
        List<String> names = new ArrayList<>();
        for (Mod mod : mods(missing)) {
            if (!mod.id().equals("minecraft")) {
                names.add(mod.id());
            }
        }
        if (names.isEmpty()) {
            return Component.translatable(PREFIX + "line.newer");
        }
        Component named = Component.translatable(PREFIX + "line", String.join(", ", names));
        return font.width(named) <= width ? named : Component.translatable(PREFIX + "line.many", names.size());
    }

    // A number with its word, one or many.
    private static Component counted(String key, int count) {
        return Component.translatable(PREFIX + key + (count == 1 ? ".one" : ""), String.format("%,d", count));
    }

    @Override
    protected void init() {
        this.boxHeight = Math.min(this.height - 40, 240);
        this.boxY = (this.height - this.boxHeight) / 2;
        this.top = this.boxY + 34;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> this.onClose())
                .bounds(this.width / 2 - 50, this.boxY + this.boxHeight - 24, 100, 20).build());
        this.contentHeight = 0;
        for (Mod mod : this.mods) {
            this.contentHeight += height(mod);
        }
    }

    // How tall a mod's part of the list is, as drawn.
    private static int height(Mod mod) {
        int blocks = mod.blocks().size();
        return LINE + 4 + (mod.id().equals("minecraft") ? LINE : 0) + Math.min(blocks, SHOWN) * LINE
                + (blocks > SHOWN ? LINE : 0) + 6;
    }

    private int listHeight() {
        return this.boxHeight - 34 - 30;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - WIDTH) / 2;
        graphics.drawString(this.font, this.title, x + 8, this.boxY + 8, BAD, false);
        graphics.drawString(this.font, this.font.plainSubstrByWidth(this.name, WIDTH - 16), x + 8, this.boxY + 20,
                0xFF8E8E8E, false);
        int bottom = this.top + this.listHeight();
        graphics.enableScissor(x, this.top, x + WIDTH, bottom);
        int y = this.top - (int) this.scroll;
        for (Mod mod : this.mods) {
            boolean game = mod.id().equals("minecraft");
            graphics.drawString(this.font, Component.literal(mod.id()), x + 10, y + 2, 0xFFFFFFFF, false);
            Component count = Component.translatable(PREFIX + "count", counted("kinds", mod.blocks().size()),
                    counted("blocks", mod.cells()));
            graphics.drawString(this.font, count, x + 14 + this.font.width(mod.id()), y + 2, 0xFF8E8E8E, false);
            if (!game) {
                Component find = Component.translatable(PREFIX + "find");
                int width = this.font.width(find) + 8;
                int bx = x + WIDTH - 10 - width;
                boolean over = mouseY >= this.top && mouseY < bottom && mouseX >= bx && mouseX < bx + width
                        && mouseY >= y && mouseY < y + 12;
                graphics.fill(bx, y, bx + width, y + 12, over ? 0xFF2F9E5B : 0xFF1F6B3D);
                graphics.drawString(this.font, find, bx + 4, y + 2, 0xFFFFFFFF, false);
            }
            y += LINE + 4;
            if (game) {
                graphics.drawString(this.font, Component.translatable(PREFIX + "newer.note"), x + 16, y, 0xFFF2C84B,
                        false);
                y += LINE;
            }
            List<Map.Entry<String, Integer>> blocks = mod.blocks();
            for (int k = 0; k < Math.min(blocks.size(), SHOWN); k++) {
                graphics.drawString(this.font, this.font.plainSubstrByWidth(blocks.get(k).getKey(), WIDTH - 90),
                        x + 16, y, 0xFFD2D2D2, false);
                String many = String.format("%,d", blocks.get(k).getValue());
                graphics.drawString(this.font, many, x + WIDTH - 12 - this.font.width(many), y, BAD, false);
                y += LINE;
            }
            if (blocks.size() > SHOWN) {
                graphics.drawString(this.font, counted("more", blocks.size() - SHOWN), x + 16, y, 0xFF8E8E8E,
                        false);
                y += LINE;
            }
            y += 6;
        }
        graphics.disableScissor();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int x = (this.width - WIDTH) / 2;
        graphics.fill(x - 1, this.boxY - 1, x + WIDTH + 1, this.boxY + this.boxHeight + 1, 0xFF000000);
        graphics.fill(x, this.boxY, x + WIDTH, this.boxY + this.boxHeight, 0xEE121212);
        NavScreen.card(graphics, x + 4, this.top - 2, WIDTH - 8, this.listHeight() + 4, BAD & 0xFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Mod clicked = this.findAt(mouseX, mouseY);
        if (button == 0 && clicked != null) {
            String url = "https://modrinth.com/mods?q=" + URLEncoder.encode(clicked.id(), StandardCharsets.UTF_8);
            ConfirmLinkScreen.confirmLinkNow(this, url);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    // The mod whose Modrinth button is under the pointer.
    @Nullable
    private Mod findAt(double mouseX, double mouseY) {
        if (mouseY < this.top || mouseY >= this.top + this.listHeight()) {
            return null;
        }
        int x = (this.width - WIDTH) / 2;
        int width = this.font.width(Component.translatable(PREFIX + "find")) + 8;
        int bx = x + WIDTH - 10 - width;
        int y = this.top - (int) this.scroll;
        for (Mod mod : this.mods) {
            if (!mod.id().equals("minecraft") && mouseX >= bx && mouseX < bx + width && mouseY >= y
                    && mouseY < y + 12) {
                return mod;
            }
            y += height(mod);
        }
        return null;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scroll = Mth.clamp(this.scroll - scrollY * LINE * 2, 0.0, Math.max(0, this.contentHeight
                - this.listHeight()));
        return true;
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.back);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
