package nl.tivek.multiversepowers.character.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructHud;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheelScreen;
import nl.tivek.multiversepowers.engine.client.gui.GuiShapes;
import nl.tivek.multiversepowers.engine.math.Ease;

// The panel in the bottom right: who you are and only what you can use right now, each with its key and cooldown. It
// stays small and out of the way: it fades in when you use or try a power, hit something or get hit, stays while a
// move is held or running, and fades out a few seconds after the fighting stops.
public final class AbilityPanel {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "character_abilities");
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".character.";
    private static final String POWER_COST = "powerCost";
    private static final int GAP = 8;
    private static final int STEP = 11;
    private static final int HEADER = STEP + 2;
    private static final int CAP = 9;
    private static final int PAD = 4;
    private static final int MARGIN = 4;
    private static final int WHITE = 0xFFE9EDF2;
    private static final int GRAY = 0xFF8D96A2;
    private static final int GREEN = 0xFF72D96E;
    private static final int AMBER = 0xFFE9B44C;
    private static final int RED = 0xFFFF6A50;
    private static final int FILL = 0xB80B0E14;
    private static final int EDGE = 0x26FFFFFF;
    private static final int BAR = 0x55FFFFFF;
    private static final long LINGER_MS = 5000L;
    private static final long FLASH_MS = 450L;
    private static final float FADE_IN_SECONDS = 0.18F;
    private static final float FADE_OUT_SECONDS = 0.8F;
    private static final float SLIDE = 8.0F;
    // Half the hotbar's width: the panel keeps right of it and the bars above it.
    private static final int HOTBAR = 91;
    // The longest each slot's cooldown has been since it was last ready, so its bar can empty.
    private static final int[] FULL = new int[AbilitySlot.values().length];
    // When each slot was last used (and whether by a hold) and last came off cooldown, for a short flash.
    private static final long[] USED = new long[AbilitySlot.values().length];
    private static final boolean[] HELD = new boolean[AbilitySlot.values().length];
    private static final long[] READY = new long[AbilitySlot.values().length];
    private static final Rules EVERY = (ability, player) -> true;
    private static final Map<GameCharacter, Rules> RULES = new EnumMap<>(GameCharacter.class);
    private static long awakeUntil;
    private static float shown;
    private static long lastFrame;
    // Screen pixels per panel pixel last chosen, and for which screen.
    private static int fitPixels;
    private static double fitScale;
    private static int fitWidth;
    private static int fitHeight;
    @Nullable
    private static GameCharacter seen;
    private static int lastHit;

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
    }

    // A mouse button with a click and a hold gets a row for each (`split`); `hold` says which.
    private record Row(CharacterAbility ability, Component key, Component name, boolean split, boolean hold) {
    }

    // `status` null shows a ready pip; `cooldown`: the part of the cooldown still to go, 0 when ready; `busy` while the
    // move is held or running, which keeps the panel up; `used`: when this row's move was last used, for its flash.
    private record Line(AbilitySlot slot, Component key, Component name, int color, @Nullable Component status,
            int statusColor, float cooldown, boolean busy, long used) {
    }

    // The panel's parts and its size in its own pixels, before it is scaled to fit.
    private record Layout(Component title, @Nullable Component footer, boolean lantern, Component guideKey,
            Component guide, int guideWidth, int caps, int width, int height) {
    }

    private AbilityPanel() {
    }

    public static void rules(GameCharacter character, Rules rules) {
        RULES.put(character, rules);
    }

    // Shows the panel now and keeps it a few seconds: a power used or refused, a blow given or taken.
    public static void wake() {
        awakeUntil = Util.getMillis() + LINGER_MS;
    }

    static void used(AbilitySlot slot, boolean hold) {
        USED[slot.ordinal()] = Util.getMillis();
        HELD[slot.ordinal()] = hold;
        wake();
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

    private static boolean onMouse(CharacterAbility ability) {
        return ability.input() == CharacterAbility.Input.LEFT || ability.input() == CharacterAbility.Input.RIGHT;
    }

    private static boolean split(CharacterAbility ability) {
        return onMouse(ability) && ability.holdTicks() > 0 && ability.tapWhen() != CharacterAbility.Tap.NEVER;
    }

    static boolean affords(CharacterAbility ability, boolean hold, LocalPlayer player) {
        return RULES.getOrDefault(ability.character(), EVERY).affords(ability, hold, player);
    }

    // The ability as a whole: enough power for its click or for its hold.
    static boolean affords(CharacterAbility ability, LocalPlayer player) {
        if (!split(ability)) {
            return affords(ability, ability.tapWhen() == CharacterAbility.Tap.NEVER, player);
        }
        return affords(ability, false, player) || affords(ability, true, player);
    }

    private static List<Row> rows(GameCharacter now, LocalPlayer player, Rules rules) {
        List<Row> rows = new ArrayList<>();
        for (CharacterAbility ability : now.abilities()) {
            if (ability.isPlaceholder() || !PowerInputs.bound(ability) || !Gestures.active(ability, player)
                    || onMouse(ability) && !Gestures.takesMouse(player)
                    || ClientCharacter.refused(ability, player) != null || !rules.lists(ability, player)) {
                continue;
            }
            if (split(ability)) {
                rows.add(new Row(ability, brief(PowerInputs.keyName(PowerInputs.clickKey(ability.input()))),
                        name(rules, ability, false, true, player), true, false));
                if (!PowerInputs.holdKey(ability.input()).isUnbound()) {
                    rows.add(new Row(ability, brief(PowerInputs.holdLabel(ability.input())),
                            name(rules, ability, true, true, player), true, true));
                }
                continue;
            }
            boolean hold = ability.tapWhen() == CharacterAbility.Tap.NEVER;
            rows.add(new Row(ability, brief(PowerInputs.label(ability)), name(rules, ability, hold, false, player),
                    false, hold));
        }
        return rows;
    }

    // Key names short enough for the panel: "Left Alt" is "LAlt", "Right Control" is "RCtrl", "Double Space" "2×Space".
    static Component brief(Component key) {
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

    private static Component name(Rules rules, CharacterAbility ability, boolean hold, boolean split,
            LocalPlayer player) {
        Component own = rules.name(ability, hold, player);
        if (own != null) {
            return own;
        }
        String held = "screen." + MultiversePowers.MODID + ".hold." + ability.id();
        return split && hold && Language.getInstance().has(held) ? Component.translatable(held)
                : ability.getDisplayName();
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        GameCharacter now = ClientCharacter.active();
        LocalPlayer player = minecraft.player;
        long millis = Util.getMillis();
        float seconds = Math.min(0.1F, (millis - lastFrame) / 1000.0F);
        lastFrame = millis;
        if (now == null || player == null || minecraft.options.hideGui
                || minecraft.screen instanceof ConstructWheelScreen || minecraft.screen instanceof PowerWheelScreen) {
            shown = 0.0F;
            return;
        }
        Rules rules = RULES.getOrDefault(now, EVERY);
        int ultimate = ClientCharacter.ultimate();
        List<Line> lines = new ArrayList<>();
        boolean busy = ultimate > 0;
        for (Row row : rows(now, player, rules)) {
            Line line = line(row, rules, player, millis);
            lines.add(line);
            busy |= line.busy();
        }
        if (busy) {
            wake();
        }
        boolean visible = shown > 0.0F;
        shown = millis < awakeUntil ? Math.min(1.0F, shown + seconds / FADE_IN_SECONDS)
                : Math.max(0.0F, shown - seconds / FADE_OUT_SECONDS);
        if (shown <= 0.0F) {
            return;
        }
        float alpha = (float) Ease.smooth(shown);
        Layout layout = layout(minecraft.font, now, lines, ultimate);
        float scale = scale(minecraft.getWindow().getGuiScale(), layout, graphics.guiWidth(), graphics.guiHeight(),
                visible);
        graphics.flush();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(graphics.guiWidth() - MARGIN + (1.0F - alpha) * SLIDE, graphics.guiHeight() - MARGIN, 0.0F);
        pose.scale(scale, scale, 1.0F);
        draw(graphics, minecraft.font, now, player, lines, layout, ultimate, millis);
        graphics.flush();
        pose.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    // Whole screen pixels per panel pixel, so it stays crisp: three quarters of the HUD's size, less (down to half) where
    // it would cover over half the screen's height or reach the hotbar. While shown it only gets smaller, so a row
    // coming and going never makes it jump.
    private static float scale(double guiScale, Layout layout, int screenWidth, int screenHeight, boolean visible) {
        int most = (int) Math.max(1.0, Math.min(guiScale, Math.max(2.0, Math.floor(guiScale * 0.75))));
        int least = (int) Math.max(1.0, Math.ceil(guiScale / 2.0));
        int pixels = visible && fitScale == guiScale && fitWidth == screenWidth && fitHeight == screenHeight
                ? Math.min(most, fitPixels) : most;
        double roomX = screenWidth / 2.0 - HOTBAR - MARGIN * 2;
        double roomY = screenHeight * 0.5 - MARGIN;
        int width = layout.width() + PAD * 2 + 1;
        int height = layout.height() + PAD * 2 + 1;
        while (pixels > least && (width * pixels / guiScale > roomX || height * pixels / guiScale > roomY)) {
            pixels--;
        }
        fitPixels = pixels;
        fitScale = guiScale;
        fitWidth = screenWidth;
        fitHeight = screenHeight;
        return (float) (pixels / guiScale);
    }

    private static Layout layout(Font font, GameCharacter now, List<Line> lines, int ultimate) {
        Component title = ultimate > 0
                ? Component.translatable(PREFIX + "ultimate." + now.getId(), now.getDisplayName(),
                        (ultimate + 19) / 20)
                : now.getDisplayName();
        boolean lantern = now == GameCharacter.GREEN_LANTERN;
        Component footer = lantern ? null : footer(now);
        Component guideKey = PowerInputs.keyName(AbilityGuide.KEY);
        Component guide = Component.translatable(PREFIX + "guide");
        int guideWidth = AbilityGuide.KEY.isUnbound() ? 0 : KeyCap.width(font, guideKey) + 3 + font.width(guide);
        int caps = 0;
        for (Line line : lines) {
            caps = Math.max(caps, KeyCap.width(font, line.key()));
        }
        int width = Math.max(font.width(title) + GAP + guideWidth, footer == null ? 0 : font.width(footer));
        if (lantern) {
            width = Math.max(width, 96);
        }
        for (Line line : lines) {
            int status = line.status() == null ? 5 : font.width(line.status());
            width = Math.max(width, caps + 4 + font.width(line.name()) + GAP + status);
        }
        int height = HEADER + STEP * lines.size() + (lantern || footer != null ? STEP + 1 : 0) - 2;
        return new Layout(title, footer, lantern, guideKey, guide, guideWidth, caps, width, height);
    }

    // Laid out from the bottom right corner at 0, 0, growing up and to the left.
    private static void draw(GuiGraphics graphics, Font font, GameCharacter now, LocalPlayer player, List<Line> lines,
            Layout layout, int ultimate, long millis) {
        int width = layout.width();
        int height = layout.height();
        int right = -PAD;
        int left = right - width;
        int top = -PAD - height;
        int color = now.getColor();

        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        GuiShapes.roundRect(graphics, left - PAD - 1, top - PAD - 1, width + PAD * 2 + 2, height + PAD * 2 + 2, 5.0F,
                EDGE);
        GuiShapes.roundRect(graphics, left - PAD, top - PAD, width + PAD * 2, height + PAD * 2, 4.0F, FILL);
        layer.shadowed(layout.title(), left, top, ultimate > 0 ? RED : 0xFF000000 | color);
        if (layout.guideWidth() > 0) {
            int x = right - layout.guideWidth();
            x += KeyCap.draw(layer, font, layout.guideKey(), x, top - 1, CAP) + 3;
            layer.text(layout.guide(), x, top, GRAY);
        }
        GuiShapes.roundRect(graphics, left, top + HEADER - 4, width, 1, 0.0F, GuiShapes.fade(color, 0.45F));
        int y = top + HEADER;
        int nameX = left + layout.caps() + 4;
        for (Line line : lines) {
            float used = (millis - line.used()) / (float) FLASH_MS;
            if (used < 1.0F) {
                GuiShapes.roundRect(graphics, left - 2, y - 1, width + 4, CAP + 2, 2.0F,
                        GuiShapes.fade(color, 0.35F * (1.0F - used) * (1.0F - used)));
            }
            KeyCap.draw(layer, font, line.key(), left, y, CAP);
            layer.text(line.name(), nameX, y + 1, line.color());
            if (line.status() == null) {
                float ready = (millis - READY[line.slot().ordinal()]) / (float) FLASH_MS;
                float pop = ready < 1.0F ? (1.0F - ready) * (1.0F - ready) : 0.0F;
                KeyCap.dot(graphics, right - 2.5F, y + CAP * 0.5F, 2.0F + 1.8F * pop, GREEN);
            } else {
                layer.text(line.status(), right - font.width(line.status()), y + 1, line.statusColor());
            }
            if (line.cooldown() > 0.0F) {
                GuiShapes.roundRect(graphics, nameX, y + CAP, (right - nameX) * line.cooldown(), 1, 0.0F, BAR);
            }
            y += STEP;
        }
        if (layout.lantern() || layout.footer() != null) {
            GuiShapes.roundRect(graphics, left, y - 1, width, 1, 0.0F, EDGE);
        }
        layer.finish();
        CharacterAbility train = now.byName("emerald_express");
        if (layout.lantern()) {
            ConstructHud.renderPower(graphics, font, player, left, right, y + 2,
                    train == null || !train.has(POWER_COST) ? 0.0F : (float) train.value(POWER_COST));
        } else if (layout.footer() != null) {
            graphics.drawString(font, layout.footer(), left, y + 2,
                    ClientCharacter.legs() > 0 || ClientCharacter.marked() > 0 ? GREEN : GRAY, false);
        }
    }

    @Nullable
    private static Component footer(GameCharacter now) {
        int legs = ClientCharacter.legs();
        int marked = ClientCharacter.marked();
        if (legs > 0 || marked > 0) {
            MutableComponent status = Component.translatable(PREFIX + (legs > 0 ? "on_legs" : "on_feet"), legs);
            if (marked > 0) {
                status.append(" + ").append(Component.translatable(PREFIX + "marked", marked));
            }
            return status;
        }
        String passive = "character." + MultiversePowers.MODID + "." + now.getId() + ".passive";
        return Language.getInstance().has(passive) ? Component.translatable(passive) : null;
    }

    private static Line line(Row row, Rules rules, LocalPlayer player, long millis) {
        CharacterAbility ability = row.ability();
        AbilitySlot slot = ability.slot();
        int index = slot.ordinal();
        int cooldown = ClientCharacter.cooldownLeft(slot);
        if (cooldown <= 0 && FULL[index] > 0) {
            READY[index] = millis;
        }
        FULL[index] = cooldown <= 0 ? 0 : Math.max(FULL[index], cooldown);
        float left = cooldown > 0 ? cooldown / (float) FULL[index] : 0.0F;
        Component key = row.key();
        Component name = row.name();
        // A button's click and hold share a slot: only the row of the one used flashes.
        long used = !row.split() || row.hold() == HELD[index] ? USED[index] : 0L;
        Component running = row.split() ? null : rules.running(ability, player);
        boolean on = row.split() ? row.hold() && MouseHold.holding(ability.input())
                : ability.isHeld() && ClientCharacter.isHeld(slot);
        if (running != null) {
            return new Line(slot, key, name, WHITE, running, GREEN, left, true, used);
        }
        if (on) {
            return new Line(slot, key, name, WHITE, Component.translatable(PREFIX + "holding"), GREEN, left, true,
                    used);
        }
        if (cooldown > 0) {
            return new Line(slot, key, name, GRAY, Component.literal((cooldown + 19) / 20 + "s"), AMBER, left,
                    false, used);
        }
        if (ClientCharacter.tired(ability)) {
            return new Line(slot, key, name, GRAY, Component.translatable(PREFIX + "tired"), AMBER, left, false,
                    used);
        }
        if (!rules.affords(ability, row.hold(), player)) {
            return new Line(slot, key, name, GRAY, Component.translatable(PREFIX + "no_power"), RED, left, false,
                    used);
        }
        return new Line(slot, key, name, WHITE, null, GREEN, left, false, used);
    }
}
