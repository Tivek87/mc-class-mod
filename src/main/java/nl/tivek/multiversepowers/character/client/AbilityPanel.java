package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.AttackIndicatorStatus;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.AbilityPanelRows.Line;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructHud;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheelScreen;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.client.gui.HudSpace;
import nl.tivek.multiversepowers.engine.client.gui.ScreenAnchors;
import nl.tivek.multiversepowers.engine.math.Ease;

// The panel in the bottom right corner, beside the hotbar: who you are and only what you can use right now, each with
// its key, and why when it cannot be used. It is drawn at the game's own GUI scale (smaller where the corner is too
// small) and keeps calm: while it is up its width only grows and rows glide to their places. It fades in gently when
// you use or try a power, hit something or get hit, stays while a move is held or running, and fades out quickly a few
// seconds after the fighting stops.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class AbilityPanel {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "character_abilities");
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".character.";
    private static final String POWER_COST = "powerCost";
    // In the panel's own pixels: room inside its edge, between a key and its name, at least between a name and its
    // status, and a key cap's height.
    private static final int PAD = 5;
    private static final int GAP = 6;
    private static final int STATUS_GAP = 12;
    private static final int CAP = 9;
    private static final Spacing ROOMY = new Spacing(11, 4, 15, 15);
    private static final Spacing TIGHT = new Spacing(10, 2, 14, 14);
    private static final int MARGIN = 2;
    private static final int BACK = 0x94000000;
    private static final int RULE = 0x1CFFFFFF;
    private static final int KEY_FILL = 0x2AFFFFFF;
    private static final int KEY_TEXT = 0xE6E6E6;
    private static final int KEY_DIM = 0x8A8A8A;
    private static final int NAME = 0xF4F4F4;
    private static final int NAME_DIM = 0x8C8C8C;
    private static final int SOFT = 0x9A9A9A;
    private static final long GONE_MS = 3000L;
    private static final float FADE_IN_SECONDS = 0.45F;
    private static final float FADE_OUT_SECONDS = 0.25F;
    private static final float GLIDE = 16.0F;
    private static final float LIGHT = 12.0F;
    private static final long ROW_FADE_MS = 150L;
    // The most of the screen's height the panel takes, unless that would bring it under two screen pixels to one of
    // its own.
    private static final float CALM = 0.6F;
    // Half the hotbar's width: the panel always stands in the corner right of it, going smaller rather than anywhere
    // else. Right of the hotbar the game may draw a left-handed player's off-hand slot, or a right-handed one's attack
    // indicator.
    private static final int HOTBAR = 91;
    private static final int OFF_HAND = 29;
    private static final int INDICATOR = 24;
    // Subtitles, whose lowest box the game draws down to this far above the bottom right, go up over the panel.
    private static final int SUBTITLES = 30;
    private static final Rules EVERY = (ability, player) -> true;
    private static final Map<GameCharacter, Rules> RULES = new EnumMap<>(GameCharacter.class);
    private static long awakeUntil;
    private static float shown;
    private static long lastFrame;
    @Nullable
    private static Up up;
    @Nullable
    private static GameCharacter seen;
    private static int lastHit;
    // How high the panel reached above the screen's bottom when last drawn, in GUI units; 0 while it is away.
    private static float drawnHeight;
    private static boolean subtitlesMoved;

    // What a character's panel lists, of what its buttons fire now and its keys allow (ClientCharacter.refusal).
    @FunctionalInterface
    public interface Rules {
        boolean lists(CharacterAbility ability, LocalPlayer player);

        // A row's own name for its ability (`hold`: for holding its button or key), or null for the ability's own.
        @Nullable
        default Component name(CharacterAbility ability, boolean hold, LocalPlayer player) {
            return null;
        }

        // What the ability does right now, said in green in place of ready, or null.
        @Nullable
        default Component running(CharacterAbility ability, LocalPlayer player) {
            return null;
        }

        // Why the ability is not usable in the state the character is in (CharacterAbility.needs), or null.
        @Nullable
        default Component unavailable(CharacterAbility ability, LocalPlayer player) {
            return null;
        }

        // Whether there is enough power for the row's move now (`hold`: for holding its button or key).
        default boolean affords(CharacterAbility ability, boolean hold, LocalPlayer player) {
            return ClientCharacter.canPay(player, ability);
        }

        // Whether holding a mouse button that also clicks does something now, so it gets a row of its own.
        default boolean holds(CharacterAbility ability, LocalPlayer player) {
            return true;
        }
    }

    // How far apart the rows sit, in the panel's own pixels: from one row to the next, between the mouse, key and
    // gesture rows, and the title and footer lines each with the room between them and the rows.
    private record Spacing(int step, int group, int title, int foot) {
    }

    // The panel's parts and its size in its own pixels; `corner` is the ultimate's countdown, else the guide's key;
    // `keys` the key column's width, `tops` each row's top from the bottom.
    private record Layout(Spacing spacing, Component title, @Nullable Component corner, @Nullable Component footer,
            boolean lantern, Component guideKey, Component guide, int guideWidth, int keys, int foot, int[] tops,
            int width, int height) {
    }

    // A row as drawn: its top gliding to its place, when it came, how lit it is and how far its status shows, keeping
    // its last status so that one fades out rather than going at once.
    private static final class Shown {
        private final long came;
        private float top;
        private float lit;
        private float said;
        @Nullable
        private Component status;
        private int statusColor;

        Shown(Line line, float top, long came) {
            this.came = came;
            this.top = top;
            this.lit = line.ready() ? 1.0F : 0.0F;
            this.said = line.status() == null ? 0.0F : 1.0F;
            this.status = line.status();
            this.statusColor = line.statusColor();
        }

        void follow(Line line, float top, float ease, float light) {
            this.top = glide(this.top, top, ease, 0.05F);
            this.lit = glide(this.lit, line.ready() ? 1.0F : 0.0F, light, 0.01F);
            this.said = glide(this.said, line.status() == null ? 0.0F : 1.0F, light, 0.01F);
            if (line.status() != null) {
                this.status = line.status();
                this.statusColor = line.statusColor();
            } else if (this.said <= 0.0F) {
                this.status = null;
            }
        }

        float alpha(long millis) {
            return Math.min(1.0F, (millis - this.came) / (float) ROW_FADE_MS);
        }
    }

    // The panel while it is up: the width and key column it keeps, its size gliding to where it is going, its rows, how
    // many screen pixels make one of its own and whether its rows sit tight.
    private static final class Up {
        private final Map<Integer, Shown> rows = new HashMap<>();
        private boolean fresh = true;
        private int width;
        private int keys;
        private float shownWidth;
        private float shownHeight;
        private int pixels;
        private boolean tight;
        private double guiScale;
        private int screenWidth;
        private int screenHeight;
        private float room;
        private float squeeze = 1.0F;

        void follow(Layout layout, List<Line> lines, float seconds, long millis) {
            if (this.fresh) {
                this.fresh = false;
                this.width = layout.width();
                this.keys = layout.keys();
                this.shownWidth = layout.width();
                this.shownHeight = layout.height();
                for (int i = 0; i < lines.size(); i++) {
                    this.rows.put(lines.get(i).id(), new Shown(lines.get(i), layout.tops()[i], 0L));
                }
                return;
            }
            float ease = 1.0F - (float) Math.exp(-GLIDE * seconds);
            float light = 1.0F - (float) Math.exp(-LIGHT * seconds);
            this.width = Math.max(this.width, layout.width());
            this.keys = Math.max(this.keys, layout.keys());
            this.shownWidth = glide(this.shownWidth, this.width, ease, 0.05F);
            this.shownHeight = glide(this.shownHeight, layout.height(), ease, 0.05F);
            Map<Integer, Shown> now = new HashMap<>();
            for (int i = 0; i < lines.size(); i++) {
                Line line = lines.get(i);
                Shown row = this.rows.get(line.id());
                if (row == null) {
                    row = new Shown(line, layout.tops()[i], millis);
                } else {
                    row.follow(line, layout.tops()[i], ease, light);
                }
                now.put(line.id(), row);
            }
            this.rows.clear();
            this.rows.putAll(now);
        }

        // The GUI scale itself with roomy rows. Where the panel would not fit in the `room` right of the hotbar, would
        // take more than CALM of the screen's height (never under two screen pixels to one of its own for that) or
        // the screen cannot hold it, tight rows first, then a step less: whatever the GUI scale it stays in the corner.
        // While up on the same screen it never grows or loosens again. Returns the layout to draw.
        Layout place(Layout roomy, Layout tight, double guiScale, int screenWidth, int screenHeight, float room) {
            if (guiScale != this.guiScale || screenWidth != this.screenWidth || screenHeight != this.screenHeight
                    || room != this.room) {
                this.guiScale = guiScale;
                this.screenWidth = screenWidth;
                this.screenHeight = screenHeight;
                this.room = room;
                this.pixels = 0;
                this.tight = false;
            }
            int most = Math.max(1, (int) Math.round(guiScale));
            if (this.pixels > 0) {
                most = Math.min(most, this.pixels);
            }
            Layout chosen = null;
            for (int pixels = most; chosen == null; pixels--) {
                float scale = (float) (pixels / guiScale);
                for (Layout layout : this.tight ? List.of(tight) : List.of(roomy, tight)) {
                    float wide = (Math.max(this.width, layout.width()) + PAD * 2) * scale;
                    float tall = (layout.height() + PAD * 2) * scale;
                    if (pixels == 1 || wide <= room && tall <= screenHeight - MARGIN * 2
                            && (pixels <= 2 || tall <= screenHeight * CALM)) {
                        chosen = layout;
                        this.pixels = pixels;
                        this.tight = layout == tight;
                        // Too narrow a window for even one screen pixel to one of its own: smaller still, in the
                        // corner all the same.
                        this.squeeze = wide > room ? Math.max(0.5F, room / wide) : 1.0F;
                        break;
                    }
                }
            }
            return chosen;
        }

        float scale() {
            return (float) (this.pixels / this.guiScale) * this.squeeze;
        }
    }

    private static float glide(float from, float to, float ease, float snap) {
        float next = from + (to - from) * ease;
        return Math.abs(to - next) < snap ? to : next;
    }

    // The colour's own alpha times `alpha`.
    private static int faded(int argb, float alpha) {
        return GuiShapes.fade(argb, (argb >>> 24) / 255.0F * alpha);
    }

    private AbilityPanel() {
    }

    public static void rules(GameCharacter character, Rules rules) {
        RULES.put(character, rules);
    }

    // Shows the panel now and keeps it, fading out included, at most 3 seconds: a power used or refused, a blow given or
    // taken.
    public static void wake() {
        awakeUntil = Util.getMillis() + GONE_MS - (long) (FADE_OUT_SECONDS * 1000.0F);
    }

    static void tick(Minecraft minecraft, LocalPlayer player, @Nullable GameCharacter now) {
        if (now != seen) {
            seen = now;
            wake();
        }
        if (now == null) {
            return;
        }
        int hit = player.getLastHurtMobTimestamp();
        if (player.hurtTime > 0 || hit != lastHit) {
            lastHit = hit;
            wake();
        }
        if (minecraft.screen != null) {
            return;
        }
        boolean trying = Gestures.takesMouse(player)
                && (minecraft.options.keyAttack.isDown() || minecraft.options.keyUse.isDown());
        for (AbilitySlot slot : AbilitySlot.values()) {
            KeyMapping key = AbilityKeys.of(slot);
            trying |= key != null && key.isDown();
        }
        if (trying) {
            wake();
        }
    }

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, AbilityPanel::render);
    }

    static boolean affords(CharacterAbility ability, boolean hold, LocalPlayer player) {
        return RULES.getOrDefault(ability.character(), EVERY).affords(ability, hold, player);
    }

    // The ability as a whole: enough power for its click or for its hold.
    static boolean affords(CharacterAbility ability, LocalPlayer player) {
        if (!AbilityPanelRows.split(ability)) {
            return affords(ability, ability.tapWhen() == CharacterAbility.Tap.NEVER, player);
        }
        return affords(ability, false, player) || affords(ability, true, player);
    }

    // Key names short enough for the panel: "Left Alt" is "LAlt", "Right Control" is "RCtrl", "Double Space" "2×Space".
    public static Component brief(Component key) {
        String name = key.getString();
        String brief = name.replace("Double ", "2×").replace("Left ", "L").replace("Right ", "R")
                .replace("Control", "Ctrl")
                .replace("Page Up", "PgUp").replace("Page Down", "PgDn").replace("Caps Lock", "Caps")
                .replace("Backspace", "Bksp").replace("Insert", "Ins").replace("Delete", "Del")
                .replace("Keypad ", "Num");
        return brief.equals(name) ? key : Component.literal(brief);
    }

    @Nullable
    static Component unavailable(CharacterAbility ability, LocalPlayer player) {
        return RULES.getOrDefault(ability.character(), EVERY).unavailable(ability, player);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        GameCharacter now = ClientCharacter.active();
        LocalPlayer player = minecraft.player;
        long millis = Util.getMillis();
        float seconds = Math.min(0.1F, (millis - lastFrame) / 1000.0F);
        lastFrame = millis;
        drawnHeight = 0.0F;
        if (now == null || player == null || minecraft.options.hideGui
                || minecraft.screen instanceof ConstructWheelScreen || minecraft.screen instanceof PowerWheelScreen) {
            shown = 0.0F;
            up = null;
            return;
        }
        int ultimate = ClientCharacter.ultimate();
        List<Line> lines = AbilityPanelRows.lines(now, player, RULES.getOrDefault(now, EVERY));
        boolean busy = ultimate > 0;
        for (Line line : lines) {
            busy |= line.busy();
        }
        if (busy) {
            wake();
        }
        shown = millis < awakeUntil ? Math.min(1.0F, shown + seconds / FADE_IN_SECONDS)
                : Math.max(0.0F, shown - seconds / FADE_OUT_SECONDS);
        if (shown <= 0.0F) {
            up = null;
            return;
        }
        Font font = minecraft.font;
        if (up == null) {
            up = new Up();
        }
        double guiScale = minecraft.getWindow().getGuiScale();
        float room = graphics.guiWidth() / 2.0F - HOTBAR - beside(minecraft) - MARGIN * 2;
        Layout layout = up.place(layout(font, now, lines, ultimate, up.keys, ROOMY),
                layout(font, now, lines, ultimate, up.keys, TIGHT), guiScale, graphics.guiWidth(),
                graphics.guiHeight(), room);
        up.follow(layout, lines, seconds, millis);
        float scale = up.scale();
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, (float) Ease.smooth(shown));
        PoseStack pose = graphics.pose();
        pose.pushPose();
        float x = onPixel(graphics.guiWidth() - MARGIN, guiScale);
        float y = onPixel(graphics.guiHeight() - MARGIN, guiScale);
        pose.translate(x, y, 0.0F);
        pose.scale(scale, scale, 1.0F);
        draw(graphics, font, now, player, lines, layout, up, millis);
        graphics.flush();
        pose.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        anchors(layout, now, x, y, scale);
    }

    // What the game itself may draw right of the hotbar, kept free whether it shows now or not, so the panel never
    // jumps: a left-handed player's off-hand slot, or the attack indicator of a right-handed one who shows it there.
    private static int beside(Minecraft minecraft) {
        if (minecraft.options.mainHand().get() == HumanoidArm.LEFT) {
            return OFF_HAND;
        }
        return minecraft.options.attackIndicator().get() == AttackIndicatorStatus.HOTBAR ? INDICATOR : 0;
    }

    // The game's subtitles stand in the bottom right as well: while the panel is up they go up over it.
    @SubscribeEvent
    public static void onLayerPre(RenderGuiLayerEvent.Pre event) {
        if (!event.getName().equals(VanillaGuiLayers.SUBTITLE_OVERLAY)) {
            return;
        }
        float lift = drawnHeight + HudSpace.GAP - SUBTITLES;
        subtitlesMoved = lift > 0.0F;
        if (subtitlesMoved) {
            event.getGuiGraphics().pose().pushPose();
            event.getGuiGraphics().pose().translate(0.0F, -lift, 0.0F);
        }
    }

    @SubscribeEvent
    public static void onLayerPost(RenderGuiLayerEvent.Post event) {
        if (subtitlesMoved && event.getName().equals(VanillaGuiLayers.SUBTITLE_OVERLAY)) {
            subtitlesMoved = false;
            event.getGuiGraphics().pose().popPose();
        }
    }

    // Where the panel, its guide key, its rows (also as the character's own, `game.panel.rows.<character>`) and its foot
    // (the ring's power, what Doctor Octopus stands on) ended up on the screen, for the tour to point at; the corner it
    // takes stays free of the rest of the HUD.
    private static void anchors(Layout layout, GameCharacter now, float x, float y, float scale) {
        int width = Math.round(up.shownWidth) + PAD * 2;
        int height = Math.round(up.shownHeight) + PAD * 2;
        ScreenAnchors.report("game.panel", x - width * scale, y - height * scale, width * scale, height * scale);
        HudSpace.claim(x - width * scale, y - height * scale, width * scale, height * scale);
        drawnHeight = height * scale + MARGIN;
        if (layout.corner() == null && layout.guideWidth() > 0) {
            float top = y - (height - PAD + 3) * scale;
            ScreenAnchors.report("game.panel.guide", x - (PAD + layout.guideWidth() + 2) * scale, top,
                    (layout.guideWidth() + 4) * scale, 15 * scale);
        }
        float left = x - (width - PAD + 2) * scale;
        float rowsTop = y - (height - PAD - layout.spacing().title() + 3) * scale;
        float foot = y - (PAD + layout.foot() - 2) * scale;
        ScreenAnchors.report("game.panel.rows", left, rowsTop, (width - PAD * 2 + 4) * scale, foot - rowsTop);
        ScreenAnchors.report("game.panel.rows." + now.getId(), left, rowsTop, (width - PAD * 2 + 4) * scale,
                foot - rowsTop);
        if (layout.foot() > 0) {
            String name = layout.lantern() ? "game.panel.power" : "game.panel.foot";
            ScreenAnchors.report(name, left, foot, (width - PAD * 2 + 4) * scale, y - foot - 3 * scale);
        }
    }

    // On a whole screen pixel, so the text stays sharp.
    private static float onPixel(float guiUnits, double guiScale) {
        return (float) (Math.round(guiUnits * guiScale) / guiScale);
    }

    // Its width fits the widest row of keys, names and the longest status, so a status coming or going never moves
    // anything; `keys` is the narrowest the key column may get.
    private static Layout layout(Font font, GameCharacter now, List<Line> lines, int ultimate, int keys,
            Spacing spacing) {
        Component title = now.getDisplayName();
        Component corner = ultimate > 0
                ? Component.translatable(PREFIX + "ultimate." + now.getId(), (ultimate + 19) / 20) : null;
        boolean lantern = now == GameCharacter.GREEN_LANTERN;
        Component footer = lantern ? null : footer();
        Component guideKey = PowerInputs.keyName(AbilityGuide.KEY);
        Component guide = Component.translatable(PREFIX + "guide");
        int guideWidth = AbilityGuide.KEY.isUnbound() ? 0 : KeyCap.width(font, guideKey) + 4 + font.width(guide);
        int names = 0;
        int statuses = font.width(Component.translatable(PREFIX + "no_power"));
        for (Line line : lines) {
            keys = Math.max(keys, KeyCap.width(font, line.key())
                    + (line.prefix() == null ? 0 : font.width(line.prefix()) + 3));
            names = Math.max(names, font.width(line.name()));
            if (line.status() != null) {
                statuses = Math.max(statuses, font.width(line.status()));
            }
        }
        int width = Math.max(font.width(title) + GAP * 2 + (corner == null ? guideWidth : font.width(corner)),
                keys + GAP + names + STATUS_GAP + statuses);
        if (footer != null) {
            width = Math.max(width, font.width(footer));
        }
        if (lantern) {
            width = Math.max(width, ConstructHud.powerWidth(font));
        }
        int foot = lantern || footer != null ? spacing.foot() : 0;
        int[] tops = new int[lines.size()];
        int below = 0;
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) {
                below += spacing.step() + (lines.get(i).group() != lines.get(i - 1).group() ? spacing.group() : 0);
            }
            tops[i] = below;
        }
        int rows = lines.isEmpty() ? 0 : below + CAP;
        for (int i = 0; i < tops.length; i++) {
            tops[i] -= rows + foot;
        }
        int height = spacing.title() + rows + foot;
        return new Layout(spacing, title, corner, footer, lantern, guideKey, guide, guideWidth, keys, foot, tops,
                width, height);
    }

    // Laid out from the bottom right corner at 0, 0, growing up and to the left.
    private static void draw(GuiGraphics graphics, Font font, GameCharacter now, LocalPlayer player, List<Line> lines,
            Layout layout, Up up, long millis) {
        int width = Math.round(up.shownWidth);
        int height = Math.round(up.shownHeight);
        int right = -PAD;
        int left = right - width;
        int bottom = -PAD;
        int top = bottom - height;
        int color = now.getColor();

        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        KeyCap.pill(graphics, left - PAD, top - PAD, width + PAD * 2, height + PAD * 2, BACK);
        GuiShapes.roundRect(graphics, left - PAD + 1, top - PAD, width + PAD * 2 - 2, 1, 0.0F,
                GuiShapes.fade(color, 0.85F));
        layer.shadowed(layout.title(), left, top, 0xFF000000 | color);
        if (layout.corner() != null) {
            layer.shadowed(layout.corner(), right - font.width(layout.corner()), top, 0xFF000000 | color);
        } else if (layout.guideWidth() > 0) {
            int x = right - layout.guideWidth();
            x += cap(layer, font, layout.guideKey(), x, top - 1, KEY_TEXT, 1.0F) + 4;
            layer.text(layout.guide(), x, top, 0xFF000000 | SOFT);
        }
        if (!lines.isEmpty()) {
            GuiShapes.roundRect(graphics, left, top + layout.spacing().title() - 4, width, 1, 0.0F, RULE);
        }
        int keysRight = left + up.keys;
        int nameX = keysRight + GAP;
        for (Line line : lines) {
            Shown row = up.rows.get(line.id());
            if (row == null || row.alpha(millis) < 0.05F) {
                continue;
            }
            float alpha = row.alpha(millis);
            int y = bottom + Math.round(row.top);
            int capX = keysRight - KeyCap.width(font, line.key());
            cap(layer, font, line.key(), capX, y, GuiShapes.mix(KEY_DIM, KEY_TEXT, row.lit), alpha);
            if (line.prefix() != null) {
                layer.text(line.prefix(), capX - 3 - font.width(line.prefix()), y + 1, GuiShapes.fade(SOFT, alpha));
            }
            layer.text(line.name(), nameX, y + 1, GuiShapes.fade(GuiShapes.mix(NAME_DIM, NAME, row.lit), alpha));
            float said = alpha * row.said;
            if (row.status != null && said >= 0.05F) {
                layer.text(row.status, right - font.width(row.status), y + 1, GuiShapes.fade(row.statusColor, said));
            }
        }
        if (layout.foot() > 0 && !lines.isEmpty()) {
            GuiShapes.roundRect(graphics, left, bottom - layout.foot() + 3, width, 1, 0.0F, RULE);
        }
        layer.finish();
        int footY = bottom - 8;
        CharacterAbility train = now.byName("emerald_express");
        if (layout.lantern()) {
            ConstructHud.renderPower(graphics, font, player, left, right, footY,
                    train == null || !train.has(POWER_COST) ? 0.0F : (float) train.value(POWER_COST));
        } else if (layout.footer() != null) {
            graphics.drawString(font, layout.footer(), left, footY, 0xFF000000 | KEY_TEXT, false);
        }
    }

    // A flat key cap with cut corners and its key in it; returns its width.
    private static int cap(KeyCap.Layer layer, Font font, Component key, int x, int y, int color, float alpha) {
        int width = KeyCap.width(font, key);
        KeyCap.pill(layer.graphics(), x, y, width, CAP, faded(KEY_FILL, alpha));
        layer.text(key, x + 3, y + 1, GuiShapes.fade(color, alpha));
        return width;
    }

    // What Doctor Octopus stands on and how many he has marked, while that is anything.
    @Nullable
    private static Component footer() {
        int legs = ClientCharacter.legs();
        int marked = ClientCharacter.marked();
        if (legs <= 0 && marked <= 0) {
            return null;
        }
        MutableComponent status = Component.translatable(PREFIX + (legs > 0 ? "on_legs" : "on_feet"), legs);
        if (marked > 0) {
            status.append(" + ").append(Component.translatable(PREFIX + "marked", marked));
        }
        return status;
    }
}
