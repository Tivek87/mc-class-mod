package nl.tivek.multiversepowers.schematic.client;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.client.AbilityGuide;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.character.client.AbilityPanel;
import nl.tivek.multiversepowers.character.client.GuideMode;
import nl.tivek.multiversepowers.character.client.GuideOwner;
import nl.tivek.multiversepowers.character.client.PowerInputs;
import nl.tivek.multiversepowers.schematic.SchematicWandItem;
import nl.tivek.multiversepowers.schematic.client.blueprint.ClientBlueprints;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.client.hologram.HologramReplaced;
import nl.tivek.multiversepowers.schematic.client.menu.SchematicScreen;

// The wand's place in the ability panel (its controls and how each stands now, while it is held) and in the ability
// guide (P), as a character has them.
public final class WandPanel implements AbilityPanel.Tool {
    private static final String PREFIX = SchematicClient.PREFIX + "panel.";
    private static final int GREEN = 0x92E08C;
    private static final int AMBER = 0xE9C46A;
    private static final int SOFT = 0xB8C8D8;
    static final GuideOwner OWNER = new GuideOwner("schematic_wand",
            Component.translatable("item.welcomescreen.schematic_wand"), SchematicScreen.COLOR, null);

    private WandPanel() {
    }

    // Called once as the game's own client starts.
    public static void register() {
        AbilityPanel.tool(new WandPanel());
        SchematicWandItem.clientUse = WandInput::use;
        ClientBlueprints.register();
        AbilityGuide.tool(OWNER, WandInput::ready, new GuideMode(OWNER, "wand", WandInput::ready, List.of(
                GuideMode.heading("mouse"),
                GuideMode.click(CharacterAbility.Input.LEFT, "place"),
                GuideMode.click(CharacterAbility.Input.RIGHT, "pin"),
                GuideMode.crouched(GuideMode.click(CharacterAbility.Input.RIGHT, "menu"), "menu"),
                GuideMode.text("scroll", "scroll"),
                GuideMode.heading("keys"),
                slot(WandInput.ROTATE, "rotate"),
                slot(WandInput.MIRROR, "mirror"),
                slot(WandInput.FOLLOW, "follow"),
                slot(WandInput.REPLACE, "replace"),
                slot(WandInput.UP, "up"),
                slot(WandInput.DOWN, "down"),
                slot(WandInput.UNDO, "undo"),
                slot(WandInput.CANCEL, "cancel"),
                slot(WandInput.HIDE, "hide"),
                slot(WandInput.MENU, "open"),
                slot(WandInput.BLUEPRINT, "blueprint"),
                GuideMode.heading("move"),
                GuideMode.holdKey(() -> AbilityKeys.SLOTS[WandInput.MOVE], "move"),
                slot(WandInput.MOVE, "grid"),
                GuideMode.text("scroll", "layers"))), new GuideMode(OWNER, "select", player -> WandInput.ready(player)
                        && AreaSelection.on(), List.of(
                                GuideMode.heading("mouse"),
                                GuideMode.click(CharacterAbility.Input.LEFT, "corner1"),
                                GuideMode.click(CharacterAbility.Input.RIGHT, "corner2"),
                                GuideMode.crouched(GuideMode.click(CharacterAbility.Input.RIGHT, "save"), "save"),
                                GuideMode.heading("keys"),
                                slot(WandInput.HIDE, "stop"),
                                slot(WandInput.MENU, "open"))));
    }

    private static GuideMode.Control slot(int slot, String id) {
        return GuideMode.key(() -> AbilityKeys.SLOTS[slot], id);
    }

    @Override
    public boolean active(LocalPlayer player) {
        return WandInput.ready(player);
    }

    @Override
    public Component title() {
        return Component.translatable("item.welcomescreen.schematic_wand");
    }

    @Override
    public int color() {
        return SchematicScreen.COLOR;
    }

    @Override
    public List<AbilityPanel.ToolRow> rows(LocalPlayer player) {
        List<AbilityPanel.ToolRow> rows = new ArrayList<>();
        if (AreaSelection.on()) {
            rows.add(row(0, mouse(CharacterAbility.Input.LEFT), "corner1", AreaSelection.first == null
                    ? say("unset") : null, GREEN, true));
            rows.add(row(0, mouse(CharacterAbility.Input.RIGHT), "corner2", AreaSelection.second == null
                    ? say("unset") : null, GREEN, true));
            rows.add(row(0, Component.translatable(PREFIX + "key_save", mouse(CharacterAbility.Input.RIGHT)),
                    "save_area", AreaSelection.complete() ? Component.translatable(PREFIX + "status.size",
                            String.format("%,d", AreaSelection.volume())) : say("unset"), AMBER,
                    AreaSelection.complete()));
            key(rows, WandInput.HIDE, "stop_select", null);
            key(rows, WandInput.MENU, "open", null);
            return rows;
        }
        boolean shown = Hologram.shown();
        int replaced = HologramReplaced.count();
        rows.add(row(0, mouse(CharacterAbility.Input.LEFT), "place", !shown ? say("none") : replaced > 0
                ? Component.translatable(PREFIX + "status.replaces", String.format("%,d", replaced)
                        + (HologramReplaced.complete() ? "" : "+")).withColor(0xEF7B6E) : Hologram.blueprint
                                ? say("blueprint") : null, AMBER, shown));
        rows.add(row(0, mouse(CharacterAbility.Input.RIGHT), shown ? "pin" : "menu", shown
                ? say(Hologram.pinned ? "pinned" : "following") : null, Hologram.pinned ? AMBER : GREEN, true));
        if (shown) {
            rows.add(row(0, Component.translatable("input.welcomescreen.scroll"), "farther", null, GREEN, true));
            key(rows, WandInput.ROTATE, "rotate", Component.translatable(SchematicClient.PREFIX + "turn."
                    + Hologram.rotation.ordinal()));
            key(rows, WandInput.MIRROR, "mirror", Component.translatable(SchematicClient.PREFIX + "mirror."
                    + Hologram.mirror.ordinal()));
            key(rows, WandInput.FOLLOW, "follow", Component.translatable(SchematicClient.PREFIX + "follow."
                    + Hologram.follow));
            key(rows, WandInput.REPLACE, "replace", Component.translatable(SchematicClient.PREFIX + "replace."
                    + Hologram.mode));
            key(rows, WandInput.UP, "up", null);
            key(rows, WandInput.DOWN, "down", null);
            if (WandInput.usable(WandInput.MOVE)) {
                rows.add(row(2, Component.translatable("input.welcomescreen.hold", brief(WandInput.MOVE)), "move",
                        null, GREEN, true));
                rows.add(row(2, Component.translatable("input.welcomescreen.tap", brief(WandInput.MOVE)), "grid",
                        Hologram.gridName(), GREEN, true));
                rows.add(row(2, Component.translatable(PREFIX + "key_layers", brief(WandInput.MOVE)), "layers",
                        Hologram.layerName(), GREEN, true));
            }
            key(rows, WandInput.HIDE, "hide", null);
        }
        if (WandInput.usable(WandInput.BLUEPRINT)) {
            key(rows, WandInput.BLUEPRINT, "blueprint", Component.translatable(SchematicClient.PREFIX + "as."
                    + (Hologram.blueprint ? 1 : 0)));
        }
        key(rows, WandInput.UNDO, "undo", null);
        if (SchematicClient.status() != null && !SchematicClient.status().over()) {
            key(rows, WandInput.CANCEL, "cancel", null);
        }
        key(rows, WandInput.MENU, "open", null);
        return rows;
    }

    private static void key(List<AbilityPanel.ToolRow> rows, int slot, String name, @Nullable Component status) {
        if (WandInput.usable(slot)) {
            rows.add(row(1, brief(slot), name, status, SOFT, true));
        }
    }

    private static AbilityPanel.ToolRow row(int group, Component key, String name, @Nullable Component status,
            int color, boolean ready) {
        return new AbilityPanel.ToolRow(group, key, Component.translatable(PREFIX + name), status, color, ready);
    }

    private static Component brief(int slot) {
        KeyMapping key = AbilityKeys.SLOTS[slot];
        return AbilityPanel.brief(PowerInputs.keyName(key));
    }

    private static Component mouse(CharacterAbility.Input input) {
        return AbilityPanel.brief(PowerInputs.keyName(PowerInputs.clickKey(input)));
    }

    private static Component say(String what) {
        return Component.translatable(PREFIX + "status." + what);
    }
}
