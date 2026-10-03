package nl.tivek.multiversepowers.character.client;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
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

// The panel in the bottom right: who you are and only what you can use right now, each with its key and cooldown.
public final class AbilityPanel {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "character_abilities");
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".character.";
    private static final String POWER_COST = "powerCost";
    private static final int WIDTH = 160;
    private static final int GAP = 10;
    private static final int STEP = 13;
    private static final int CAP = 11;
    private static final int PAD = 5;
    private static final int WHITE = 0xFFE9EDF2;
    private static final int GRAY = 0xFF8D96A2;
    private static final int GREEN = 0xFF72D96E;
    private static final int AMBER = 0xFFE9B44C;
    private static final int RED = 0xFFFF6A50;
    private static final int FILL = 0xB80B0E14;
    private static final int EDGE = 0x26FFFFFF;
    private static final int BAR = 0x55FFFFFF;
    // The longest each slot's cooldown has been since it was last ready, so its bar can empty.
    private static final int[] FULL = new int[AbilitySlot.values().length];
    private static final Rules EVERY = (ability, player) -> true;
    private static final Map<GameCharacter, Rules> RULES = new EnumMap<>(GameCharacter.class);

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
    }

    // A mouse button with a click and a hold gets a row for each (`split`); `hold` says which.
    private record Row(CharacterAbility ability, Component key, Component name, boolean split, boolean hold) {
    }

    // `cooldown`: the part of the cooldown still to go, 0 when ready.
    private record Line(Component key, Component name, int color, Component status, int statusColor,
            float cooldown) {
    }

    private AbilityPanel() {
    }

    public static void rules(GameCharacter character, Rules rules) {
        RULES.put(character, rules);
    }

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, AbilityPanel::render);
    }

    private static boolean onMouse(CharacterAbility ability) {
        return ability.input() == CharacterAbility.Input.LEFT || ability.input() == CharacterAbility.Input.RIGHT;
    }

    private static List<Row> rows(GameCharacter now, LocalPlayer player, Rules rules) {
        List<Row> rows = new ArrayList<>();
        for (CharacterAbility ability : now.abilities()) {
            if (ability.isPlaceholder() || !PowerInputs.bound(ability) || !Gestures.active(ability, player)
                    || ClientCharacter.refused(ability, player) != null || !rules.lists(ability, player)) {
                continue;
            }
            if (onMouse(ability) && ability.holdTicks() > 0 && ability.tapWhen() != CharacterAbility.Tap.NEVER) {
                rows.add(new Row(ability, PowerInputs.keyName(PowerInputs.clickKey(ability.input())),
                        name(rules, ability, false, true, player), true, false));
                if (!PowerInputs.holdKey(ability.input()).isUnbound()) {
                    rows.add(new Row(ability, PowerInputs.holdLabel(ability.input()),
                            name(rules, ability, true, true, player), true, true));
                }
                continue;
            }
            boolean hold = ability.tapWhen() == CharacterAbility.Tap.NEVER;
            rows.add(new Row(ability, PowerInputs.label(ability), name(rules, ability, hold, false, player), false,
                    hold));
        }
        return rows;
    }

    @Nullable
    static Component unavailable(CharacterAbility ability, LocalPlayer player) {
        return RULES.getOrDefault(ability.character(), EVERY).unavailable(ability, player);
    }

    // What a mouse button's click or hold does now, as the panel names it.
    static Component rowName(CharacterAbility ability, boolean hold, LocalPlayer player) {
        return name(RULES.getOrDefault(ability.character(), EVERY), ability, hold, true, player);
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
        if (now == null || player == null || minecraft.options.hideGui
                || minecraft.screen instanceof ConstructWheelScreen || minecraft.screen instanceof PowerWheelScreen) {
            return;
        }
        Rules rules = RULES.getOrDefault(now, EVERY);
        Font font = minecraft.font;
        int ultimate = ClientCharacter.ultimate();
        Component title = ultimate > 0
                ? Component.translatable(PREFIX + "ultimate." + now.getId(), now.getDisplayName(),
                        (ultimate + 19) / 20)
                : now.getDisplayName();
        Component footer = now == GameCharacter.GREEN_LANTERN ? null : footer(now);
        boolean lantern = now == GameCharacter.GREEN_LANTERN;
        Component guideKey = PowerInputs.keyName(AbilityGuide.KEY);
        Component guide = Component.translatable(PREFIX + "guide");
        int guideWidth = AbilityGuide.KEY.isUnbound() ? 0 : KeyCap.width(font, guideKey) + 3 + font.width(guide);
        List<Line> lines = new ArrayList<>();
        int caps = 0;
        for (Row row : rows(now, player, rules)) {
            Line line = line(row, rules, player);
            lines.add(line);
            caps = Math.max(caps, KeyCap.width(font, line.key()));
        }
        int width = Math.max(WIDTH, Math.max(font.width(title) + GAP + guideWidth,
                footer == null ? 0 : font.width(footer)));
        for (Line line : lines) {
            width = Math.max(width, caps + 5 + font.width(line.name()) + GAP + font.width(line.status()));
        }
        int header = STEP + 3;
        int height = header + STEP * lines.size() + (lantern || footer != null ? STEP + 2 : 0) - 2;
        int right = graphics.guiWidth() - 4 - PAD;
        int left = right - width;
        int bottom = graphics.guiHeight() - 4 - PAD;
        int top = bottom - height;
        int color = now.getColor();

        KeyCap.Layer layer = new KeyCap.Layer(graphics, font);
        GuiShapes.roundRect(graphics, left - PAD - 1, top - PAD - 1, width + PAD * 2 + 2, height + PAD * 2 + 2, 5.0F,
                EDGE);
        GuiShapes.roundRect(graphics, left - PAD, top - PAD, width + PAD * 2, height + PAD * 2, 4.0F, FILL);
        layer.shadowed(title, left, top + 1, ultimate > 0 ? RED : 0xFF000000 | color);
        if (guideWidth > 0) {
            int x = right - guideWidth;
            x += KeyCap.draw(layer, font, guideKey, x, top - 1, CAP) + 3;
            layer.text(guide, x, top + 1, GRAY);
        }
        GuiShapes.roundRect(graphics, left, top + header - 4, width, 1, 0.0F, GuiShapes.fade(color, 0.45F));
        int y = top + header;
        int nameX = left + caps + 5;
        for (Line line : lines) {
            KeyCap.draw(layer, font, line.key(), left, y, CAP);
            layer.text(line.name(), nameX, y + 2, line.color());
            layer.text(line.status(), right - font.width(line.status()), y + 2, line.statusColor());
            if (line.cooldown() > 0.0F) {
                GuiShapes.roundRect(graphics, nameX, y + CAP, (right - nameX) * line.cooldown(), 1, 0.0F, BAR);
            }
            y += STEP;
        }
        if (lantern || footer != null) {
            GuiShapes.roundRect(graphics, left, y, width, 1, 0.0F, EDGE);
        }
        layer.finish();
        CharacterAbility train = now.byName("emerald_express");
        if (lantern) {
            ConstructHud.renderPower(graphics, font, player, left, right, y + 4,
                    train == null || !train.has(POWER_COST) ? 0.0F : (float) train.value(POWER_COST));
        } else if (footer != null) {
            graphics.drawString(font, footer, left, y + 4,
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

    private static Line line(Row row, Rules rules, LocalPlayer player) {
        CharacterAbility ability = row.ability();
        int slot = ability.slot().ordinal();
        int cooldown = ClientCharacter.cooldownLeft(ability.slot());
        FULL[slot] = cooldown <= 0 ? 0 : Math.max(FULL[slot], cooldown);
        float left = cooldown > 0 ? cooldown / (float) FULL[slot] : 0.0F;
        Component key = row.key();
        Component name = row.name();
        int color = cooldown > 0 ? GRAY : WHITE;
        Component running = row.split() ? null : rules.running(ability, player);
        boolean on = row.split() ? row.hold() && MouseHold.holding(ability.input())
                : ability.isHeld() && ClientCharacter.isHeld(ability.slot());
        if (running != null) {
            return new Line(key, name, color, running, GREEN, left);
        }
        if (on) {
            return new Line(key, name, color, Component.translatable(PREFIX + "holding"), GREEN, left);
        }
        if (cooldown > 0) {
            return new Line(key, name, color, Component.literal((cooldown + 19) / 20 + "s"), AMBER, left);
        }
        if (!ClientCharacter.canPay(player, ability)) {
            return new Line(key, name, color, Component.translatable(PREFIX + "no_power"), RED, left);
        }
        return new Line(key, name, color, Component.translatable(PREFIX + "ready"), GREEN, left);
    }
}
