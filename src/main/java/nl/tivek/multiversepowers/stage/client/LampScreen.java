package nl.tivek.multiversepowers.stage.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.engine.client.gui.NavScreen;
import nl.tivek.multiversepowers.engine.client.gui.PixelIcons;
import nl.tivek.multiversepowers.stage.LampEditPayload;
import nl.tivek.multiversepowers.stage.LampKind;
import nl.tivek.multiversepowers.stage.LampLooks;
import nl.tivek.multiversepowers.stage.LampSettings;
import nl.tivek.multiversepowers.stage.StageLightBlockEntity;

// A stage light's settings (sneak + right click on it, for anyone): a page each for its light, its colours, how it
// moves and what it aims at, and its show (strobe, channel, redstone). Every change shows in the world at once, as
// the game keeps running behind it.
public final class LampScreen extends NavScreen {
    private static final String P = "stage.welcomescreen.";
    private static final int ROW = 22;
    private static final int LEAST_ROW = 16;
    private static final int SEND_EVERY = 3;
    private static final int[] QUICK = { 0xFF2020, 0xFF7A00, 0xFFE100, 0x8CFF00, 0x00FF5A, 0x00F0FF, 0x2050FF,
            0x8A2BFF, 0xFF20E0, 0xFF6FA8, 0xFFFFFF, 0xFFD9A0 };
    private static final int[][] PRESETS = { { 0xFF2BD6, 0x3C6BFF, 0x2BFFF1, 0xFFE100 },
            { 0xFF2000, 0xFF7A00, 0xFFC400 }, { 0xFFFFFF, 0x9FD8FF, 0x2E6BFF }, { 0x39FF14, 0xFF00FF, 0x00FFFF },
            { 0xFF4E50, 0xF9D423, 0xFF8C42, 0xB6229E }, { 0xFFFFFF } };
    private static final String[] PRESET_NAMES = { "party", "fire", "ice", "neon", "sunset", "white" };

    enum Page {
        LIGHT(PixelIcons.Icon.IDEA), COLOR(PixelIcons.Icon.SPARK), MOTION(PixelIcons.Icon.PLAY),
        SHOW(PixelIcons.Icon.GLOBE);

        final PixelIcons.Icon icon;

        Page(PixelIcons.Icon icon) {
            this.icon = icon;
        }

        String id() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    private record Label(Component text, int y) {
    }

    private record Swatch(int x, int y, int size, Runnable click) {
    }

    private final BlockPos pos;
    private final LampKind kind;
    private final LampSettings edit;
    private final Page shown;
    private int pickedColor;
    private boolean dirty;
    private int wait;
    private final List<Label> labels = new ArrayList<>();
    private final List<Swatch> swatches = new ArrayList<>();
    private final List<AbstractWidget> pageWidgets = new ArrayList<>();
    private int rowY;
    private int rowHeight;
    private int labelWidth;
    @Nullable
    private EditBox hex;
    private int paletteY = -1;
    private int quickY = -1;

    private LampScreen(BlockPos pos, LampKind kind, LampSettings edit, Page shown, int pickedColor) {
        super(title(kind, edit), null, shown.id());
        this.pos = pos;
        this.kind = kind;
        this.edit = edit;
        this.shown = shown;
        this.pickedColor = Mth.clamp(pickedColor, 0, edit.colors.length - 1);
    }

    private static Component title(LampKind kind, LampSettings settings) {
        return settings.name.isBlank() ? Component.translatable("block.welcomescreen." + kind.id())
                : Component.literal(settings.name);
    }

    public static void open(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.level.getBlockEntity(pos) instanceof StageLightBlockEntity lamp) {
            minecraft.setScreen(new LampScreen(pos, lamp.kind(), lamp.settings().copy(), Page.LIGHT, 0));
        }
    }

    // A strobe and a mirror ball do not aim or move as a head does: they have no page for it.
    static List<Page> pages(LampKind kind) {
        return kind == LampKind.STROBE || kind == LampKind.DISCO_BALL ? List.of(Page.LIGHT, Page.COLOR, Page.SHOW)
                : List.of(Page.values());
    }

    // Each kind names its first page for what it is: its beam, its beams, its flash, its ball.
    private Component pageName(Page page) {
        return Component.translatable(page == Page.LIGHT ? P + "page.light." + this.kind.id() : P + "page." + page.id());
    }

    @Override
    protected List<Item> items() {
        List<Item> items = new ArrayList<>();
        for (Page page : pages(this.kind)) {
            items.add(new Item(page.id(), this.pageName(page), page.icon, () -> this.go(page), false));
        }
        return items;
    }

    private void go(Page page) {
        this.send();
        this.minecraft.setScreen(new LampScreen(this.pos, this.kind, this.edit, page, this.pickedColor));
    }

    @Override
    protected Component brand() {
        return Component.translatable(P + "brand");
    }

    @Override
    protected PixelIcons.Icon brandIcon() {
        return PixelIcons.Icon.IDEA;
    }

    @Override
    protected int accent() {
        return this.edit.colors[0];
    }

    @Override
    protected Component subtitle() {
        if (this.shown == Page.MOTION && this.edit.aim != LampSettings.Aim.PATTERN) {
            return Component.translatable(P + "page.motion.aiming").withColor(WARN);
        }
        return Component.translatable(this.shown == Page.LIGHT ? P + "page.light." + this.kind.id() + ".about"
                : P + "page." + this.shown.id() + ".about");
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // Laid out twice: once to count the rows this light shows, then for real with rows that fit the window.
    @Override
    protected void initPage() {
        this.rowHeight = ROW;
        this.labelWidth = Math.min(130, (int) (this.contentWidth * 0.4));
        this.layOut();
        int rows = this.labels.size();
        for (AbstractWidget widget : List.copyOf(this.pageWidgets)) {
            this.removeWidget(widget);
        }
        this.pageWidgets.clear();
        this.rowHeight = Mth.clamp(this.contentHeight / Math.max(1, rows), LEAST_ROW, ROW);
        this.layOut();
    }

    private void layOut() {
        this.labels.clear();
        this.hex = null;
        this.paletteY = -1;
        this.quickY = -1;
        this.rowY = this.contentY;
        switch (this.shown) {
            case LIGHT -> this.light();
            case COLOR -> this.color();
            case MOTION -> this.motion();
            case SHOW -> this.show();
        }
    }

    // ---- pages ----

    // Ready-made looks first, then only what this kind of light has: a head's beam, a laser's lines, a wash light's
    // cone, a strobe's flashes, a mirror ball's dots.
    private void light() {
        LampSettings s = this.edit;
        this.looks();
        this.toggle("on", () -> s.on, value -> s.on = value);
        this.slider("brightness", s.brightness, 0.0, 3.0, 0.05, false, LampScreen::percent, v -> s.brightness = v);
        switch (this.kind) {
            case MOVING_HEAD, SEARCHLIGHT -> {
                this.slider("width", s.width, 0.005, 4.0, 0.005, true, LampScreen::thickness, v -> s.width = v);
                this.slider("length", s.length, 1.0, LampSettings.MOST_LENGTH, 1.0, true, LampScreen::blocks,
                        v -> s.length = v);
            }
            case LASER -> {
                this.slider("beams", s.beams, 1, 64, 1, false, LampScreen::count, v -> s.beams = (int) v);
                this.slider("fan", s.fan, 0.0, 180.0, 1.0, false, LampScreen::degrees, v -> s.fan = v);
                this.slider("thickness", s.width, 0.005, 0.5, 0.005, true, LampScreen::thickness, v -> s.width = v);
                this.slider("length", s.length, 1.0, LampSettings.MOST_LENGTH, 1.0, true, LampScreen::blocks,
                        v -> s.length = v);
            }
            case PAR -> {
                this.slider("cone", s.spread, 0.0, 60.0, 0.5, false, LampScreen::degrees, v -> s.spread = v);
                this.slider("length", s.length, 1.0, 64.0, 1.0, false, LampScreen::blocks, v -> s.length = v);
            }
            case STROBE -> {
                this.slider("flashes", s.strobe, 0.0, 25.0, 0.25, false,
                        v -> v <= 0.0 ? Component.translatable(P + "strobe.off") : perSecond(v), v -> s.strobe = v);
                this.slider("flash_length", s.duty, 0.05, 0.95, 0.01, false, LampScreen::percent, v -> s.duty = v);
                this.slider("burst", s.length, 1.0, 32.0, 1.0, false, LampScreen::blocks, v -> s.length = v);
            }
            case DISCO_BALL -> {
                this.slider("dots", s.beams, 1, 64, 1, false, LampScreen::count, v -> s.beams = (int) v);
                this.slider("dot_size", s.width, 0.005, 0.2, 0.005, true, LampScreen::thickness, v -> s.width = v);
                this.slider("spin", s.speed, 0.0, 2.0, 0.01, false, LampScreen::perSecond, v -> s.speed = v);
                this.slider("length", s.length, 1.0, 64.0, 1.0, false, LampScreen::blocks, v -> s.length = v);
            }
        }
        this.slider("light", s.light, 0, 15, 1, false, v -> Component.literal(Integer.toString((int) v)),
                v -> s.light = (int) v);
        if (this.kind != LampKind.STROBE) {
            this.toggle("stop", () -> s.stopAtBlocks, value -> s.stopAtBlocks = value);
        }
    }

    // A button for each of this kind's ready-made looks.
    private void looks() {
        List<LampLooks.Look> looks = LampLooks.of(this.kind);
        int y = this.label("looks");
        int x = this.controlX();
        int width = this.controlWidth();
        int each = (width - 2 * (looks.size() - 1)) / looks.size();
        for (int i = 0; i < looks.size(); i++) {
            LampLooks.Look look = looks.get(i);
            String key = P + "look." + this.kind.id() + "." + look.id();
            this.add(Button.builder(Component.translatable(key), b -> {
                look.apply().accept(this.edit);
                this.pickedColor = 0;
                this.changed();
                this.rebuildWidgets();
            }).bounds(x + i * (each + 2), y, each, this.rowHeight - 4)
                    .tooltip(tip(Component.translatable(key + ".desc"))).build());
        }
    }

    private void color() {
        LampSettings s = this.edit;
        this.cycle("color_mode", LampKind.ColorMode.values(), s.colorMode, mode -> s.colorMode = mode);
        this.slider("color_speed", s.colorSpeed, 0.0, 20.0, 0.05, false, LampScreen::perSecond,
                v -> s.colorSpeed = v);
        this.paletteY = this.label("palette");
        int x = this.controlX();
        int width = this.controlWidth();
        int y = this.label("hex");
        this.hex = new EditBox(this.font, x, y, Math.min(70, width), this.rowHeight - 4,
                Component.translatable(P + "row.hex"));
        this.hex.setMaxLength(7);
        this.hex.setValue(hexOf(s.colors[this.pickedColor]));
        this.hex.setTooltip(tip(Component.translatable(P + "row.hex.desc")));
        this.hex.setResponder(text -> {
            Integer rgb = parse(text);
            if (rgb != null) {
                s.colors[this.pickedColor] = rgb;
                this.changed();
            }
        });
        this.add(this.hex);
        int buttons = x + Math.min(70, width) + 4;
        int each = Math.max(20, (x + width - buttons - 4) / 2);
        Button add = Button.builder(Component.translatable(P + "color.add"), button -> this.addColor())
                .bounds(buttons, y, each, this.rowHeight - 4)
                .tooltip(tip(Component.translatable(P + "color.add.desc"))).build();
        add.active = s.colors.length < LampSettings.MOST_COLORS;
        this.add(add);
        Button remove = Button.builder(Component.translatable(P + "color.remove"), button -> this.removeColor())
                .bounds(buttons + each + 4, y, each, this.rowHeight - 4)
                .tooltip(tip(Component.translatable(P + "color.remove.desc"))).build();
        remove.active = s.colors.length > 1;
        this.add(remove);
        float[] hsv = hsv(s.colors[this.pickedColor]);
        this.slider("hue", hsv[0] * 360.0, 0.0, 360.0, 1.0, false, LampScreen::degrees, v -> {
            float[] now = hsv(s.colors[this.pickedColor]);
            this.setPicked(Mth.hsvToRgb((float) (v / 360.0) % 1.0F, now[1], Math.max(0.05F, now[2])));
        });
        this.slider("saturation", hsv[1], 0.0, 1.0, 0.01, false, LampScreen::percent, v -> {
            float[] now = hsv(s.colors[this.pickedColor]);
            this.setPicked(Mth.hsvToRgb(now[0], (float) v, Math.max(0.05F, now[2])));
        });
        this.quickY = this.label("quick");
        int presetY = this.label("presets");
        int count = PRESETS.length;
        int presetWidth = (width - 2 * (count - 1)) / count;
        for (int i = 0; i < count; i++) {
            int[] preset = PRESETS[i];
            Button button = Button.builder(Component.translatable(P + "preset." + PRESET_NAMES[i]), b -> {
                s.colors = preset.clone();
                this.pickedColor = 0;
                this.changed();
                this.rebuildWidgets();
            }).bounds(x + i * (presetWidth + 2), presetY, presetWidth, this.rowHeight - 4).build();
            this.add(button);
        }
    }

    // What decides where it points, and only the rows that work for that: a chosen spot or creature, how far it looks
    // for the nearest, or its own pattern and how far that swings.
    private void motion() {
        LampSettings s = this.edit;
        this.cycle("aim", LampSettings.Aim.values(), s.aim, aim -> s.aim = aim);
        if (s.aim == LampSettings.Aim.SPOT || s.aim == LampSettings.Aim.FOLLOW) {
            int y = this.label("pick");
            this.add(Button.builder(Component.translatable(P + "pick"), button -> {
                this.send();
                LampPicker.start(this.pos);
            }).bounds(this.controlX(), y, this.controlWidth(), this.rowHeight - 4)
                    .tooltip(tip(Component.translatable(P + "row.pick.desc"))).build());
        }
        if (s.aim == LampSettings.Aim.MOB || s.aim == LampSettings.Aim.PLAYER || s.aim == LampSettings.Aim.HOSTILE) {
            this.slider("aim_range", s.aimRange, 4.0, 128.0, 1.0, false, LampScreen::blocks, v -> s.aimRange = v);
        }
        if (s.aim != LampSettings.Aim.PATTERN) {
            return;
        }
        LampKind.Pattern[] patterns = patterns(this.kind);
        this.cycle("pattern", patterns, s.pattern, value -> s.pattern = value);
        if (s.pattern != LampKind.Pattern.STILL) {
            this.slider("speed", s.speed, 0.0, 5.0, 0.01, false, LampScreen::perSecond, v -> s.speed = v);
            this.slider("side", s.panRange, 0.0, 180.0, 1.0, false, LampScreen::degrees, v -> s.panRange = v);
            this.slider("updown", s.tiltRange, 0.0, 90.0, 1.0, false, LampScreen::degrees, v -> s.tiltRange = v);
        }
        this.slider("turn", s.pan, -180.0, 180.0, 1.0, false, LampScreen::degrees, v -> s.pan = v);
        this.slider("lean", s.tilt, -90.0, 90.0, 1.0, false, LampScreen::degrees, v -> s.tilt = v);
    }

    // The patterns that suit each kind that moves.
    private static LampKind.Pattern[] patterns(LampKind kind) {
        return switch (kind) {
            case SEARCHLIGHT -> new LampKind.Pattern[] { LampKind.Pattern.STILL, LampKind.Pattern.SWEEP,
                    LampKind.Pattern.CIRCLE, LampKind.Pattern.FIGURE_EIGHT, LampKind.Pattern.RANDOM };
            case PAR -> new LampKind.Pattern[] { LampKind.Pattern.STILL, LampKind.Pattern.SWEEP,
                    LampKind.Pattern.CIRCLE, LampKind.Pattern.BOUNCE };
            case LASER -> new LampKind.Pattern[] { LampKind.Pattern.STILL, LampKind.Pattern.SWEEP,
                    LampKind.Pattern.WAVE, LampKind.Pattern.CIRCLE, LampKind.Pattern.BOUNCE, LampKind.Pattern.RANDOM };
            default -> new LampKind.Pattern[] { LampKind.Pattern.STILL, LampKind.Pattern.SWEEP,
                    LampKind.Pattern.CIRCLE, LampKind.Pattern.FIGURE_EIGHT, LampKind.Pattern.WAVE,
                    LampKind.Pattern.RANDOM, LampKind.Pattern.BOUNCE };
        };
    }

    private void show() {
        LampSettings s = this.edit;
        int y = this.label("name");
        EditBox name = new EditBox(this.font, this.controlX(), y, this.controlWidth(), this.rowHeight - 4,
                Component.translatable(P + "row.name"));
        name.setMaxLength(32);
        name.setValue(s.name);
        name.setHint(Component.translatable("block.welcomescreen." + this.kind.id())
                .withStyle(ChatFormatting.DARK_GRAY));
        name.setTooltip(tip(Component.translatable(P + "row.name.desc")));
        name.setResponder(text -> {
            s.name = text;
            this.changed();
        });
        this.add(name);
        this.slider("channel", s.channel, 0, LampSettings.CHANNELS, 1, false,
                v -> v < 1 ? Component.translatable(P + "channel.none")
                        : Component.translatable(P + "channel.number", (int) v),
                v -> s.channel = (int) v);
        y = this.label("send");
        Button send = Button.builder(Component.translatable(P + "send"), button -> {
            this.dirty = false;
            PacketDistributor.sendToServer(new LampEditPayload(this.pos, this.edit.write(), true));
            this.click(1.2F);
        }).bounds(this.controlX(), y, this.controlWidth(), this.rowHeight - 4)
                .tooltip(tip(Component.translatable(P + "row.send.desc"))).build();
        this.add(send);
        // A strobe sets its flashes on its own first page.
        if (this.kind != LampKind.STROBE) {
            this.slider("strobe", s.strobe, 0.0, 25.0, 0.25, false,
                    v -> v <= 0.0 ? Component.translatable(P + "strobe.off") : perSecond(v), v -> s.strobe = v);
            if (s.strobe > 0.0) {
                this.slider("duty", s.duty, 0.05, 0.95, 0.01, false, LampScreen::percent, v -> s.duty = v);
            }
        }
        this.slider("phase", s.phase, 0.0, 1.0, 0.01, false, LampScreen::percent, v -> s.phase = v);
        this.cycle("redstone", LampSettings.Redstone.values(), s.redstone, value -> s.redstone = value);
        y = this.label("reset");
        this.row("reset", Button.builder(Component.translatable(P + "reset"), button -> {
            LampSettings fresh = LampSettings.of(this.kind);
            fresh.name = s.name;
            fresh.channel = s.channel;
            this.dirty = false;
            PacketDistributor.sendToServer(new LampEditPayload(this.pos, fresh.write(), false));
            this.minecraft.setScreen(new LampScreen(this.pos, this.kind, fresh, this.shown, 0));
        }).bounds(this.controlX(), y, this.controlWidth(), this.rowHeight - 4).build());
    }

    // ---- rows ----

    private int controlX() {
        return this.contentX + this.labelWidth;
    }

    private int controlWidth() {
        return this.contentWidth - this.labelWidth;
    }

    // A row's name on the left; returns the y its control goes at.
    private int label(String id) {
        int y = this.rowY;
        this.labels.add(new Label(Component.translatable(P + "row." + id), y));
        this.rowY += this.rowHeight;
        return y + 2;
    }

    private <W extends AbstractWidget> W row(String id, W widget) {
        widget.setTooltip(tip(Component.translatable(P + "row." + id + ".desc")));
        return this.add(widget);
    }

    private <W extends AbstractWidget> W add(W widget) {
        this.pageWidgets.add(widget);
        return this.addRenderableWidget(widget);
    }

    private void toggle(String id, BooleanSupplier get, Consumer<Boolean> set) {
        int y = this.label(id);
        this.row(id, Button.builder(onOff(get.getAsBoolean()), button -> {
            set.accept(!get.getAsBoolean());
            button.setMessage(onOff(get.getAsBoolean()));
            this.changed();
        }).bounds(this.controlX(), y, this.controlWidth(), this.rowHeight - 4).build());
    }

    private <E extends Enum<E>> Button cycle(String id, E[] values, E now, Consumer<E> set) {
        int y = this.label(id);
        String key = P + id + ".";
        return this.row(id, Button.builder(Component.translatable(key + now.name().toLowerCase(Locale.ROOT)),
                button -> {
                    // Shift + click goes back a step.
                    set.accept(values[Math.floorMod(Arrays.asList(values).indexOf(now) + (hasShiftDown() ? -1 : 1),
                            values.length)]);
                    this.changed();
                    this.rebuildWidgets();
                }).bounds(this.controlX(), y, this.controlWidth(), this.rowHeight - 4).build());
    }

    private LampSlider slider(String id, double value, double least, double most, double step, boolean curved,
            DoubleFunction<Component> shown, DoubleConsumer set) {
        int y = this.label(id);
        return this.row(id, new LampSlider(this.controlX(), y, this.controlWidth(), this.rowHeight - 4, value, least,
                most, step, curved, shown, v -> {
                    set.accept(v);
                    this.changed();
                }));
    }

    // ---- colours ----

    private void setPicked(int rgb) {
        this.edit.colors[this.pickedColor] = rgb & 0xFFFFFF;
        if (this.hex != null) {
            this.hex.setValue(hexOf(rgb));
        }
        this.changed();
    }

    private void addColor() {
        LampSettings s = this.edit;
        if (s.colors.length >= LampSettings.MOST_COLORS) {
            return;
        }
        int[] more = Arrays.copyOf(s.colors, s.colors.length + 1);
        more[more.length - 1] = s.colors[this.pickedColor];
        s.colors = more;
        this.pickedColor = more.length - 1;
        this.changed();
        this.rebuildWidgets();
    }

    private void removeColor() {
        LampSettings s = this.edit;
        if (s.colors.length <= 1) {
            return;
        }
        int[] fewer = new int[s.colors.length - 1];
        for (int i = 0, j = 0; i < s.colors.length; i++) {
            if (i != this.pickedColor) {
                fewer[j++] = s.colors[i];
            }
        }
        s.colors = fewer;
        this.pickedColor = Math.min(this.pickedColor, fewer.length - 1);
        this.changed();
        this.rebuildWidgets();
    }

    private static String hexOf(int rgb) {
        return String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    @Nullable
    private static Integer parse(String text) {
        String digits = text.startsWith("#") ? text.substring(1) : text;
        if (digits.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(digits, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Hue, saturation and value, each 0 to 1.
    private static float[] hsv(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255.0F;
        float g = (rgb >> 8 & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float most = Math.max(r, Math.max(g, b));
        float range = most - Math.min(r, Math.min(g, b));
        float hue = 0.0F;
        if (range > 0.0F) {
            hue = most == r ? (g - b) / range : most == g ? 2.0F + (b - r) / range : 4.0F + (r - g) / range;
            hue = (hue / 6.0F + 1.0F) % 1.0F;
        }
        return new float[] { hue, most <= 0.0F ? 0.0F : range / most, most };
    }

    // ---- words ----

    private static Component onOff(boolean on) {
        return Component.translatable(on ? P + "on" : P + "off").withStyle(on ? ChatFormatting.GREEN
                : ChatFormatting.GRAY);
    }

    private static Component percent(double value) {
        return Component.literal(Math.round(value * 100.0) + "%");
    }

    private static Component degrees(double value) {
        return Component.literal(Math.round(value) + "°");
    }

    private static Component blocks(double value) {
        return Component.translatable(P + "unit.blocks", Long.toString(Math.round(value)));
    }

    private static Component count(double value) {
        return Component.literal(Integer.toString((int) Math.round(value)));
    }

    private static Component thickness(double value) {
        return Component.translatable(P + "unit.blocks", String.format(Locale.ROOT, "%.3f", value));
    }

    private static Component perSecond(double value) {
        return Component.translatable(P + "unit.per_second", String.format(Locale.ROOT, "%.2f", value));
    }

    // ---- sending ----

    private void changed() {
        this.dirty = true;
        this.wait = SEND_EVERY;
    }

    private void send() {
        if (this.dirty) {
            this.dirty = false;
            PacketDistributor.sendToServer(new LampEditPayload(this.pos, this.edit.write(), false));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.minecraft.level == null
                || !(this.minecraft.level.getBlockEntity(this.pos) instanceof StageLightBlockEntity)) {
            this.minecraft.setScreen(null);
            return;
        }
        if (this.dirty && --this.wait <= 0) {
            this.send();
        }
    }

    @Override
    public void onClose() {
        this.send();
        super.onClose();
    }

    // ---- drawing ----

    @Override
    protected void renderPage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        for (Label label : this.labels) {
            graphics.drawString(this.font, this.fit(label.text, this.labelWidth - 6), this.contentX,
                    label.y + (this.rowHeight - 8) / 2, BODY, false);
        }
        this.swatches.clear();
        if (this.paletteY >= 0) {
            this.palette(graphics, mouseX, mouseY);
        }
        if (this.quickY >= 0) {
            this.quick(graphics, mouseX, mouseY);
        }
    }

    private void palette(GuiGraphics graphics, int mouseX, int mouseY) {
        int size = this.rowHeight - 6;
        int x = this.controlX();
        int y = this.paletteY + 1;
        int[] colors = this.edit.colors;
        for (int i = 0; i < colors.length; i++) {
            int at = x + i * (size + 4);
            boolean picked = i == this.pickedColor;
            boolean over = mouseX >= at && mouseX < at + size && mouseY >= y && mouseY < y + size;
            graphics.fill(at - 2, y - 2, at + size + 2, y + size + 2, picked ? 0xFFFFFFFF : over ? 0xFF808080
                    : 0xFF000000);
            graphics.fill(at, y, at + size, y + size, 0xFF000000 | colors[i]);
            int index = i;
            this.swatches.add(new Swatch(at, y, size, () -> {
                this.pickedColor = index;
                this.rebuildWidgets();
            }));
        }
    }

    private void quick(GuiGraphics graphics, int mouseX, int mouseY) {
        int width = this.controlWidth();
        int size = Math.min(this.rowHeight - 6, (width - 2 * (QUICK.length - 1)) / QUICK.length);
        int y = this.quickY + 1;
        for (int i = 0; i < QUICK.length; i++) {
            int at = this.controlX() + i * (size + 2);
            boolean over = mouseX >= at && mouseX < at + size && mouseY >= y && mouseY < y + size;
            graphics.fill(at - 1, y - 1, at + size + 1, y + size + 1, over ? 0xFFFFFFFF : 0xFF000000);
            graphics.fill(at, y, at + size, y + size, 0xFF000000 | QUICK[i]);
            int rgb = QUICK[i];
            this.swatches.add(new Swatch(at, y, size, () -> {
                this.setPicked(rgb);
                this.rebuildWidgets();
            }));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (Swatch swatch : List.copyOf(this.swatches)) {
            if (mouseX >= swatch.x && mouseX < swatch.x + swatch.size && mouseY >= swatch.y
                    && mouseY < swatch.y + swatch.size) {
                this.click(1.0F);
                swatch.click.run();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
