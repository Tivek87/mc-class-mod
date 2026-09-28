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
import nl.tivek.multiversepowers.character.thor.ThorPowers;
import nl.tivek.multiversepowers.config.Unit;

public enum GameCharacter {
    DOC_OCK("doc_ock", 0xA8AEB8, new DocOckPowers()) {
        @Override
        void fill(Map<AbilitySlot, CharacterAbility> abilities) {
            this.add(abilities, AbilitySlot.ABILITY_1, "grab").cooldown(60).damage(14.0)
                    .crouch(CharacterAbility.Crouch.UNDO)
                    .setting("rangeBlocks", 20.0, 2.0, 64.0, Unit.BLOCKS,
                            "How far a tentacle can reach out to grab, in blocks")
                    .setting("throwSpeed", 2.6, 0.5, 8.0, Unit.BLOCKS_PER_TICK,
                            "How fast a creature thrown out of the claws flies, in blocks a tick")
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
                    .setting("slamRadiusBlocks", 6.0, 2.0, 16.0, Unit.BLOCKS,
                            "How far round a slam on the ground everything is hit, in blocks")
                    .setting("airSlamRadiusBlocks", 8.0, 2.0, 20.0, Unit.BLOCKS,
                            "How far round a slam from the air everything is hit, in blocks")
                    .setting("airDamage", 10.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage when you slam down out of the air, in half hearts")
                    .setting("heldSlamDamage", 14.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage when you smash the creatures you hold into the ground, in half hearts");
            this.add(abilities, AbilitySlot.ABILITY_6, "portal").cooldown(400).damage(50.0)
                    .setting("homingRangeBlocks", 30.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far the tentacle hunts after it comes out of the second portal, in blocks")
                    .settingInt("huntTicks", 120, 20, 600, Unit.TICKS, "How long a tentacle hunts its creature")
                    .settingInt("dragTicks", 120, 20, 600, Unit.TICKS, "How long a tentacle drags its creature")
                    .setting("casterReachBlocks", 28.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far from him the first portal may open, in blocks")
                    .setting("portalSpreadBlocks", 14.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far apart the three portals open, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_7, "rampage").cooldown(1800).damage(3.0)
                    .setting("rangeBlocks", 8.0, 2.0, 24.0, Unit.BLOCKS, "How far away the rampage strikes, in blocks")
                    .settingInt("strikeEveryTicks", 12, 2, 60, Unit.TICKS, "How often each tentacle strikes")
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
    },
    THOR("thor", 0x6FC8FF, new ThorPowers()) {
        @Override
        void fill(Map<AbilitySlot, CharacterAbility> abilities) {
            // On the ground: right click dashes, holding it claps, a double space jumps high and holding space flies.
            // In flight the same buttons blink, dive and speed up.
            this.add(abilities, AbilitySlot.ABILITY_1, "thunderclap").input(CharacterAbility.Input.RIGHT)
                    .holdOnly(ThorPowers.CLAP_HOLD).when(CharacterAbility.When.GROUND).cooldown(200).damage(5.0)
                    .setting("radiusBlocks", 9.0, 2.0, 32.0, Unit.BLOCKS, "How far the thunderclap reaches, in blocks")
                    .setting("halfAngleDegrees", Math.toDegrees(0.8), 10.0, 90.0, Unit.DEGREES,
                            "How wide the thunderclap spreads, in degrees either side of where he aims")
                    .setting("waveSpeed", 2.25, 0.25, 10.0, Unit.BLOCKS_PER_TICK,
                            "How fast the thunderclap rolls out, in blocks a tick")
                    .setting("push", 1.6, 0.0, 6.0, Unit.STRENGTH, "How hard the thunderclap throws what it hits")
                    .setting("lift", 0.45, 0.0, 3.0, Unit.STRENGTH, "How high the thunderclap throws what it hits");
            this.add(abilities, AbilitySlot.ABILITY_2, "dash").input(CharacterAbility.Input.RIGHT)
                    .when(CharacterAbility.When.GROUND).cooldown(16)
                    .setting("shortestBlocks", 4.0, 1.0, 16.0, Unit.BLOCKS, "The shortest a dash goes, in blocks")
                    .setting("longestBlocks", 8.0, 1.0, 24.0, Unit.BLOCKS, "The longest a dash goes, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_3, "super_jump").input(CharacterAbility.Input.SPACE).doubleTap()
                    .when(CharacterAbility.When.GROUND).cooldown(50)
                    .setting("heightBlocks", 10.0, 2.0, 40.0, Unit.BLOCKS, "How high a super jump goes, in blocks")
                    .setting("floatSeconds", 2.5, 0.0, 10.0, Unit.SECONDS,
                            "How long he hangs in the air at the top of a super jump, in seconds");
            this.add(abilities, AbilitySlot.ABILITY_4, "flight").input(CharacterAbility.Input.SPACE)
                    .holdOnly(ThorPowers.FLIGHT_HOLD).when(CharacterAbility.When.GROUND)
                    .setting("speed", 18.0, 2.0, 80.0, Unit.BLOCKS_PER_SECOND, "How fast he flies, in blocks a second")
                    .setting("stunDamage", 5.0, 0.0, 100.0, Unit.HALF_HEARTS,
                            "A hit this hard knocks him out of the sky, in half hearts (0 = any hit)");
            this.add(abilities, AbilitySlot.ABILITY_5, "air_blink").input(CharacterAbility.Input.RIGHT)
                    .when(CharacterAbility.When.FLYING).cooldown(24)
                    .setting("distanceBlocks", 15.0, 2.0, 48.0, Unit.BLOCKS, "How far a blink takes him, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_6, "grab_dash_dive").input(CharacterAbility.Input.RIGHT)
                    .holdOnly(ThorPowers.DIVE_HOLD).when(CharacterAbility.When.FLYING).cooldown(160).damage(10.0)
                    .setting("reachBlocks", 32.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far away he can dive onto a creature, in blocks")
                    .setting("grabBlocks", 2.2, 0.5, 6.0, Unit.BLOCKS, "How close he must come to grab it, in blocks")
                    .settingInt("carryTicks", 80, 10, 400, Unit.TICKS, "How long he may carry it before he lets go")
                    .setting("slamRadiusBlocks", 4.5, 1.0, 16.0, Unit.BLOCKS,
                            "How far round the slam everything is hit and thrown, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_7, "lightning_flight").input(CharacterAbility.Input.SCROLL)
                    .holdOnly(ThorPowers.LIGHTNING_HOLD).held().when(CharacterAbility.When.FLYING).cooldown(40)
                    .setting("speed", 48.0, 10.0, 160.0, Unit.BLOCKS_PER_SECOND,
                            "How fast he flies at lightning speed, in blocks a second");
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
