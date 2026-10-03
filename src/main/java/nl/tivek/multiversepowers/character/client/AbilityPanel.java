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
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructHud;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheelScreen;

// The panel in the bottom right: who you are and only what you can use right now, each with its key and cooldown.
public final class AbilityPanel {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "character_abilities");
    private static final String PREFIX = "screen." + MultiversePowers.MODID + ".character.";
    private static final String POWER_COST = "powerCost";
    private static final int WIDTH = 168;
    private static final int GAP = 8;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF9AA2AC;
    private static final int GREEN = 0xFF7FD46B;
    private static final int RED = 0xFFFF5A3A;
    private static final int PANEL = 0x90101418;
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
    }

    // A mouse button with a click and a hold gets a row for each (`split`); `hold` says which.
    private record Row(CharacterAbility ability, Component key, Component name, boolean split, boolean hold) {
    }

    private record Line(Component text, int color, Component status, int statusColor) {
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
        Component guide = AbilityGuide.KEY.isUnbound() ? Component.empty()
                : Component.translatable(PREFIX + "guide", PowerInputs.keyName(AbilityGuide.KEY));
        List<Line> lines = new ArrayList<>();
        int width = Math.max(WIDTH, Math.max(font.width(title) + GAP + font.width(guide),
                footer == null ? 0 : font.width(footer)));
        for (Row row : rows(now, player, rules)) {
            Line line = line(row, rules, player);
            lines.add(line);
            width = Math.max(width, font.width(line.text()) + GAP + font.width(line.status()));
        }
        int step = font.lineHeight + 2;
        int height = step * (lines.size() + 2) + 4;
        int right = graphics.guiWidth() - 4;
        int left = right - width;
        int top = graphics.guiHeight() - 4 - height;
        graphics.fill(left - 3, top - 3, right + 3, top + height, PANEL);
        graphics.drawString(font, title, left, top, ultimate > 0 ? RED : 0xFF000000 | now.getColor());
        graphics.drawString(font, guide, right - font.width(guide), top, GRAY);
        int y = top + step;
        for (Line line : lines) {
            graphics.drawString(font, line.text(), left, y, line.color());
            graphics.drawString(font, line.status(), right - font.width(line.status()), y, line.statusColor());
            y += step;
        }
        CharacterAbility train = now.byName("emerald_express");
        if (now == GameCharacter.GREEN_LANTERN) {
            ConstructHud.renderPower(graphics, font, player, left, right, y + 2,
                    train == null || !train.has(POWER_COST) ? 0.0F : (float) train.value(POWER_COST));
        } else if (footer != null) {
            graphics.drawString(font, footer, left, y + 2,
                    ClientCharacter.legs() > 0 || ClientCharacter.marked() > 0 ? GREEN : GRAY);
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
        int cooldown = ClientCharacter.cooldownLeft(ability.slot());
        Component text = Component.literal("[").append(row.key()).append("] ").append(row.name());
        int color = cooldown > 0 ? GRAY : WHITE;
        Component running = row.split() ? null : rules.running(ability, player);
        boolean on = row.split() ? row.hold() && MouseHold.holding(ability.input())
                : ability.isHeld() && ClientCharacter.isHeld(ability.slot());
        if (running != null) {
            return new Line(text, color, running, GREEN);
        }
        if (on) {
            return new Line(text, color, Component.translatable(PREFIX + "holding"), GREEN);
        }
        if (cooldown > 0) {
            return new Line(text, color, Component.literal((cooldown + 19) / 20 + "s"), GRAY);
        }
        if (!ClientCharacter.canPay(player, ability)) {
            return new Line(text, color, Component.translatable(PREFIX + "no_power"), RED);
        }
        return new Line(text, color, Component.translatable(PREFIX + "ready"), GREEN);
    }
}
