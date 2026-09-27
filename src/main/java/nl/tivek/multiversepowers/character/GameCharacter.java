package nl.tivek.multiversepowers.character;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.docock.DocOckPowers;
import nl.tivek.multiversepowers.character.greenlantern.GreenLanternPowers;
import nl.tivek.multiversepowers.config.Unit;

public enum GameCharacter {
    DOC_OCK("doc_ock", 0xA8AEB8, new DocOckPowers()) {
        @Override
        void fill(Map<AbilitySlot, CharacterAbility> abilities) {
            this.add(abilities, AbilitySlot.ABILITY_1, "grab").cooldown(60).damage(14.0)
                    .crouch(CharacterAbility.Crouch.UNDO)
                    .setting("rangeBlocks", 20.0, 2.0, 64.0, Unit.BLOCKS,
                            "How far a tentacle can reach out to grab, in blocks")
                    .setting("smashSpeed", 0.35, 0.05, 3.0, Unit.BLOCKS_PER_TICK,
                            "How fast a held creature must hit a wall or the ground to be hurt, in blocks per tick");
            this.add(abilities, AbilitySlot.ABILITY_2, "multi_tentacle").cooldown(120).damage(4.0)
                    .setting("rangeBlocks", 8.0, 2.0, 32.0, Unit.BLOCKS,
                            "How far the tentacles look for a target, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_3, "dash").cooldown(60)
                    .setting("staminaCost", 30.0, 0.0, 200.0, Unit.STAMINA, "Stamina one dash costs")
                    .setting("speed", 2.6, 0.5, 8.0, Unit.STRENGTH,
                            "How hard the tentacles throw you: higher is a longer dash");
            this.add(abilities, AbilitySlot.ABILITY_4, "block").held()
                    .setting("damageKept", 0.15, 0.0, 1.0, Unit.PART_KEPT,
                            "Part of a blocked hit that still gets through (0.15 = 15%)")
                    .setting("staminaPerTick", 0.6, 0.0, 20.0, Unit.STAMINA_PER_TICK,
                            "Stamina blocking costs per tick (20 ticks = 1 second)");
            this.add(abilities, AbilitySlot.ABILITY_5, "ground_slam").cooldown(160).damage(7.0)
                    .crouch(CharacterAbility.Crouch.ALTERNATE)
                    .setting("airDamage", 10.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage when you slam down out of the air, in half hearts")
                    .setting("heldSlamDamage", 14.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage when you smash the creatures you hold into the ground, in half hearts");
            this.add(abilities, AbilitySlot.ABILITY_6, "portal").cooldown(400).damage(50.0)
                    .setting("homingRangeBlocks", 30.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far the tentacle hunts after it comes out of the second portal, in blocks")
                    .setting("portalSpreadBlocks", 14.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far apart the three portals open, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_7, "rampage").cooldown(1800).damage(3.0)
                    .settingInt("durationTicks", 400, 20, 6000, Unit.TICKS,
                            "How long the rampage lasts, in ticks (20 ticks = 1 second)");
            this.add(abilities, AbilitySlot.ABILITY_8, "placeholder");
            this.add(abilities, AbilitySlot.ABILITY_9, "stance").cooldown(6)
                    .crouch(CharacterAbility.Crouch.ALTERNATE);
            this.add(abilities, AbilitySlot.ABILITY_10, "ground_strike").cooldown(200).damage(25.0)
                    .crouch(CharacterAbility.Crouch.UNDO)
                    .setting("rangeBlocks", 32.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far you can mark a creature, and how far a tentacle travels under the ground")
                    .setting("knockUp", 0.55, 0.0, 3.0, Unit.STRENGTH,
                            "How hard the spike throws what it hits into the air");
            this.add(abilities, AbilitySlot.ABILITY_11, "placeholder_2");
        }
    },
    GREEN_LANTERN("green_lantern", 0x3CE86A, new GreenLanternPowers()) {
        @Override
        void fill(Map<AbilitySlot, CharacterAbility> abilities) {
            LanternAbilities.fill(this, abilities);
        }
    };

    private final String id;
    private final int color;
    private final CharacterPowers powers;
    private final Map<AbilitySlot, CharacterAbility> abilities = new EnumMap<>(AbilitySlot.class);
    private final List<CharacterAbility> ordered = new ArrayList<>();

    GameCharacter(String id, int color, CharacterPowers powers) {
        this.id = id;
        this.color = color;
        this.powers = powers;
    }

    // Enum constants cannot use their own fields in their constructor, so abilities are filled in here instead.
    static {
        for (GameCharacter character : values()) {
            character.fill(character.abilities);
            for (AbilitySlot slot : AbilitySlot.values()) {
                CharacterAbility ability = character.abilities.get(slot);
                if (ability == null) {
                    ability = character.add(character.abilities, slot, "free_" + slot.getId()).placeholder();
                }
                character.ordered.add(ability);
            }
        }
    }

    abstract void fill(Map<AbilitySlot, CharacterAbility> abilities);

    CharacterAbility add(Map<AbilitySlot, CharacterAbility> abilities, AbilitySlot slot, String id) {
        CharacterAbility ability = new CharacterAbility(this, slot, id);
        abilities.put(slot, ability);
        return ability;
    }

    public String getId() {
        return this.id;
    }

    public int getColor() {
        return this.color;
    }

    public CharacterPowers powers() {
        return this.powers;
    }

    public Component getDisplayName() {
        return Component.translatable("character." + MultiversePowers.MODID + "." + this.id);
    }

    @Nullable
    public CharacterAbility ability(AbilitySlot slot) {
        return this.abilities.get(slot);
    }

    public List<CharacterAbility> abilities() {
        return this.ordered;
    }

    @Nullable
    public CharacterAbility byName(String id) {
        for (CharacterAbility ability : this.ordered) {
            if (ability.id().equals(id)) {
                return ability;
            }
        }
        return null;
    }

    @Nullable
    public static GameCharacter byId(String id) {
        for (GameCharacter character : values()) {
            if (character.id.equals(id)) {
                return character;
            }
        }
        return null;
    }
}
