package nl.tivek.multiversepowers.faction.client.pick;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.client.gui.ScrollArea;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientFactions;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;
import nl.tivek.multiversepowers.faction.rules.Faction;
import nl.tivek.multiversepowers.faction.rules.FactionRules;
import org.lwjgl.glfw.GLFW;

// Creatures to choose from: every kind of the game in a list to search, each with a box to tick; beside it the one
// under the pointer (else the last one clicked) turning as a model, with its name, what it is and its numbers. With
// no world open there is no model to make: its spawn egg stands in.
public final class MobPickerScreen extends Screen {
    private static final String KEY = "mob." + MultiversePowers.MODID + ".";
    private static final int ROW = 18;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int BODY = 0xFFD2D2D2;
    private static final int MUTED = 0xFF8E8E8E;

    private final Screen parent;
    private final Consumer<List<String>> done;
    private final Set<String> picked = new LinkedHashSet<>();
    private final ScrollArea area = new ScrollArea();
    private final MobPreview preview = new MobPreview();
    private final int accent;
    private String query = "";
    private boolean onlyPicked;
    @Nullable
    private MobInfo.Info shown;
    @Nullable
    private EditBox search;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private int listWidth;
    private int cardX;
    private int cardWidth;

    // `picked`: the ids ticked to begin with; `done` gets the ids ticked when the player is done, not on a cancel.
    public MobPickerScreen(Screen parent, Component title, Collection<String> picked, int accent,
            Consumer<List<String>> done) {
        super(title);
        this.parent = parent;
        this.done = done;
        this.accent = accent;
        this.picked.addAll(picked);
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(460, this.width - 16);
        this.panelHeight = Math.min(270, this.height - 16);
        this.left = (this.width - this.panelWidth) / 2;
        this.top = (this.height - this.panelHeight) / 2;
        this.cardWidth = Mth.clamp(this.panelWidth * 2 / 5, 120, 190);
        this.listWidth = this.panelWidth - this.cardWidth - 22;
        this.cardX = this.left + 8 + this.listWidth + 6;
        this.search = new EditBox(this.font, this.left + 8, this.top + 22, this.listWidth, 14,
                Component.translatable(KEY + "pick.search"));
        this.search.setHint(Component.translatable(KEY + "pick.search").withStyle(ChatFormatting.DARK_GRAY));
        this.search.setMaxLength(40);
        this.search.setValue(this.query);
        this.search.setResponder(text -> {
            this.query = text;
            this.area.top();
        });
        this.addRenderableWidget(this.search);
        this.setInitialFocus(this.search);
        int y = this.top + this.panelHeight - 24;
        int each = Math.min(80, (this.panelWidth - 28) / 3);
        int x = this.left + this.panelWidth - 8 - each;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.finish())
                .bounds(x, y, each, 18).build());
        x -= each + 4;
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> this.onClose())
                .bounds(x, y, each, 18).build());
        x -= each + 4;
        this.addRenderableWidget(Button.builder(Component.translatable(KEY + "pick.clear"), button -> {
            this.picked.clear();
            this.onlyPicked = false;
        }).bounds(x, y, each, 18).build());
    }

    private void finish() {
        this.done.accept(new ArrayList<>(this.picked));
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
    }

    @Override
    public void removed() {
        this.preview.forget();
    }

    private List<MobInfo.Info> rows() {
        List<MobInfo.Info> rows = new ArrayList<>();
        for (MobInfo.Info info : MobInfo.all()) {
            if (info.matches(this.query) && (!this.onlyPicked || this.picked.contains(info.id()))) {
                rows.add(info);
            }
        }
        return rows;
    }

    private FormattedCharSequence fit(Component text, int room) {
        if (this.font.width(text) <= room) {
            return text.getVisualOrderText();
        }
        return Component.literal(this.font.plainSubstrByWidth(text.getString(), Math.max(0, room - 6)) + "...")
                .withStyle(text.getStyle()).getVisualOrderText();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawString(this.font, this.title.copy().withStyle(ChatFormatting.BOLD), this.left + 8, this.top + 7,
                0xFF000000 | this.accent, false);
        Component count = Component.translatable(KEY + (this.onlyPicked ? "pick.count.only" : "pick.count"),
                this.picked.size());
        int countX = this.left + 8 + this.listWidth - this.font.width(count);
        boolean overCount = mouseX >= countX - 2 && mouseX < countX + this.font.width(count) + 2
                && mouseY >= this.top + 5 && mouseY < this.top + 17;
        graphics.drawString(this.font, overCount ? count.copy().withStyle(ChatFormatting.UNDERLINE) : count, countX,
                this.top + 7, this.onlyPicked ? 0xFF000000 | this.accent : overCount ? TEXT : MUTED, false);
        int listTop = this.top + 40;
        int listHeight = this.panelHeight - 40 - 30;
        List<MobInfo.Info> rows = this.rows();
        ScreenAnchors.report("mobs.picker", this.left + 8, listTop, this.listWidth, listHeight);
        MobInfo.Info over = null;
        int start = this.area.begin(graphics, this.left + 8, listTop, this.listWidth, listHeight, rows.size() * ROW);
        int width = this.area.rowWidth();
        for (int i = 0; i < rows.size(); i++) {
            int y = start + i * ROW;
            if (!this.area.shows(y, ROW)) {
                continue;
            }
            MobInfo.Info info = rows.get(i);
            int x = this.left + 8;
            boolean on = this.area.over(mouseX, mouseY, x, y, width, ROW);
            boolean ticked = this.picked.contains(info.id());
            if (on) {
                over = info;
            }
            graphics.fill(x, y, x + width, y + ROW,
                    on ? 0x22FFFFFF : ticked ? 0x18FFFFFF : i % 2 == 0 ? 0x10FFFFFF : 0);
            graphics.fill(x + 3, y + 4, x + 13, y + 14, 0xFF000000);
            graphics.fill(x + 4, y + 5, x + 12, y + 13, ticked ? 0xFF000000 | this.accent : 0xFF2A2A2A);
            if (ticked) {
                graphics.drawString(this.font, "✔", x + 4, y + 5, 0xFF101010, false);
            }
            graphics.renderItem(info.icon(), x + 16, y + 1);
            int right = x + width - 4;
            Faction faction = FactionRules.of(info.type());
            Component tag = faction != null ? Component.literal(faction.name()).withColor(faction.color())
                    : info.modName().copy().withColor(MUTED & 0xFFFFFF);
            int tagWidth = Math.min(this.font.width(tag), width / 3);
            graphics.drawString(this.font, this.fit(tag, tagWidth), right - tagWidth, y + 5, MUTED, false);
            graphics.drawString(this.font, this.fit(info.name(), right - tagWidth - x - 42), x + 36, y + 5,
                    ticked || on ? TEXT : BODY, false);
            this.area.hit(x, y, width, ROW, () -> {
                if (!this.picked.remove(info.id())) {
                    this.picked.add(info.id());
                }
                this.shown = info;
            });
        }
        this.area.end(graphics);
        if (rows.isEmpty()) {
            Component none = Component.translatable(KEY + "pick.none");
            graphics.drawString(this.font, none, this.left + 8 + (this.listWidth - this.font.width(none)) / 2,
                    listTop + 8, MUTED, false);
        }
        MobInfo.Info info = over != null ? over : this.shown != null ? this.shown : rows.isEmpty() ? null : rows.get(0);
        if (info != null) {
            this.card(graphics, info, this.top + 22, this.panelHeight - 22 - 30);
        }
    }

    private void card(GuiGraphics graphics, MobInfo.Info info, int y, int height) {
        int x = this.cardX;
        int width = this.cardWidth;
        NavScreen.card(graphics, x, y, width, height, this.accent);
        List<FormattedCharSequence> about = this.font.split(info.about(), width - 12);
        List<Component> facts = this.facts(info);
        int text = 12 + Math.min(4, about.size()) * 10 + 4 + facts.size() * 10 + 4;
        int model = Math.max(40, height - text - 4);
        if (!this.preview.draw(graphics, info.type(), x + 3, y + 2, width - 4, model)) {
            float scale = Math.max(2.0F, Math.min(4.0F, (model - 8) / 16.0F));
            graphics.pose().pushPose();
            graphics.pose().translate(x + width / 2.0F - 8 * scale, y + 2 + model / 2.0F - 8 * scale, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.renderItem(info.icon(), 0, 0);
            graphics.pose().popPose();
        }
        int line = y + model + 4;
        graphics.drawString(this.font, this.fit(info.name().copy().withStyle(ChatFormatting.BOLD), width - 12), x + 6,
                line, TEXT, false);
        line += 12;
        for (int i = 0; i < Math.min(4, about.size()); i++) {
            graphics.drawString(this.font, about.get(i), x + 6, line, BODY, false);
            line += 10;
        }
        line += 4;
        for (Component fact : facts) {
            graphics.drawString(this.font, this.fit(fact, width - 12), x + 6, line, MUTED, false);
            line += 10;
        }
    }

    private List<Component> facts(MobInfo.Info info) {
        List<Component> facts = new ArrayList<>();
        if (!Double.isNaN(info.health())) {
            facts.add(Component.translatable(KEY + "fact.health", MobInfo.hearts(info.health()))
                    .withColor(BODY & 0xFFFFFF));
        }
        if (!Double.isNaN(info.damage()) && info.damage() > 0.0) {
            facts.add(Component.translatable(KEY + "fact.damage", MobInfo.hearts(info.damage()))
                    .withColor(BODY & 0xFFFFFF));
        }
        if (!Double.isNaN(info.armor()) && info.armor() > 0.0) {
            facts.add(Component.translatable(KEY + "fact.armor", String.valueOf((long) info.armor()))
                    .withColor(BODY & 0xFFFFFF));
        }
        facts.add(Component.translatable(KEY + "fact.sort", info.sort(), info.modName()));
        Faction faction = FactionRules.of(info.type());
        if (faction != null) {
            Standing toward = ClientFactions.toward(faction);
            Component stand = Component.translatable("faction." + MultiversePowers.MODID + ".stand."
                    + (toward == null ? "game" : toward.name().toLowerCase(Locale.ROOT)))
                    .withColor(toward == null ? 0xC8C8C8 : toward.rgb());
            facts.add(Component.translatable(KEY + "fact.faction",
                    Component.literal(faction.name()).withColor(faction.color()), stand));
        }
        return facts;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(this.left - 1, this.top - 1, this.left + this.panelWidth + 1, this.top + this.panelHeight + 1,
                0xFF000000);
        graphics.fill(this.left, this.top, this.left + this.panelWidth, this.top + this.panelHeight, 0xF2121212);
        graphics.fill(this.left, this.top, this.left + this.panelWidth, this.top + 2, 0xFF000000 | this.accent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && this.area.press(mouseX, mouseY)) {
            return true;
        }
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        Component count = Component.translatable(KEY + (this.onlyPicked ? "pick.count.only" : "pick.count"),
                this.picked.size());
        int countX = this.left + 8 + this.listWidth - this.font.width(count);
        if (mouseX >= countX - 2 && mouseX < countX + this.font.width(count) + 2 && mouseY >= this.top + 5
                && mouseY < this.top + 17) {
            this.onlyPicked = !this.onlyPicked && !this.picked.isEmpty();
            this.area.top();
            return true;
        }
        if (mouseX >= this.cardX && mouseX < this.cardX + this.cardWidth && mouseY >= this.top + 22
                && mouseY < this.top + this.panelHeight - 30) {
            this.preview.hold(true);
            return true;
        }
        return this.area.click(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.preview.held()) {
            this.preview.drag(dragX);
            return true;
        }
        return this.area.drag(mouseY) || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.preview.hold(false);
        this.area.release();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.area.scroll(scrollY, ROW);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) && hasControlDown()) {
            this.finish();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
