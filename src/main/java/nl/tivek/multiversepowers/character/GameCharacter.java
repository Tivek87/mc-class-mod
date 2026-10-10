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
            this.add(abilities, AbilitySlot.ABILITY_8, "placeholder").spare();
            this.add(abilities, AbilitySlot.ABILITY_9, "stance").cooldown(6)
                    .crouch(CharacterAbility.Crouch.ALTERNATE);
            this.add(abilities, AbilitySlot.ABILITY_10, "ground_strike").cooldown(200).damage(25.0)
                    .crouch(CharacterAbility.Crouch.UNDO)
                    .setting("rangeBlocks", 32.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far you can mark a creature, and how far a tentacle travels under the ground")
                    .setting("knockUp", 0.55, 0.0, 3.0, Unit.STRENGTH,
                            "How hard the spike throws what it hits into the air");
            this.add(abilities, AbilitySlot.ABILITY_11, "placeholder_2").spare();
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
            // Without the hammer: left click throws the blows of his combo, holding it claps, right click dashes,
            // holding it grabs (running, a grab dash). With it: the combo swings it, holding left is its uppercut,
            // right click throws it (crouched, to stay where it stops), holding right draws it back and letting go
            // throws it and dashes after it. With the hammer on him the scroll wheel's click takes it up or puts it
            // away and holding it charges him (or the hammer); with it away the click calls it back and, while it
            // rests, holding it dashes to it. A double space jumps high, holding space flies (calling the hammer
            // first). In flight left click throws one-handed blows, holding it is a shockwave, right click blinks,
            // holding it dives, the scroll wheel's click calls down a bolt and holding shift is lightning speed.
            // His first key calls up his storm (pressed again, a bolt out of it; crouched, it ends), the second is the
            // lightning bomb, the third the Storm Throw (in flight only its hurl). His other keys are kept free for
            // abilities to come; his moves sit in the slots past them.
            for (AbilitySlot slot : AbilitySlot.values()) {
                if (slot.keyed() && slot != AbilitySlot.ABILITY_1 && slot != AbilitySlot.ABILITY_2
                        && slot != AbilitySlot.ABILITY_3) {
                    this.add(abilities, slot, "key_" + slot.getId()).spare();
                }
            }
            CharacterAbility.When ground = CharacterAbility.When.GROUND;
            CharacterAbility.When flying = CharacterAbility.When.FLYING;
            this.add(abilities, AbilitySlot.ABILITY_1, "storm").crouch(CharacterAbility.Crouch.UNDO).cooldown(600)
                    .damage(8.0)
                    .setting("seconds", 20.0, 5.0, 120.0, Unit.SECONDS, "How long the storm lasts, in seconds")
                    .setting("radius", 24.0, 8.0, 64.0, Unit.BLOCKS, "How far round him the storm reaches, in blocks")
                    .setting("strikeDamage", 5.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of the bolts the storm strikes foes with by itself");
            this.add(abilities, AbilitySlot.ABILITY_2, "lightning_bomb").when(ground).cooldown(600).damage(16.0)
                    .setting("radius", 10.0, 3.0, 24.0, Unit.BLOCKS, "How far round him the burst reaches, in blocks")
                    .was(8.0);
            this.add(abilities, AbilitySlot.ABILITY_3, "storm_throw").needs(ThorPowers.HOME).cooldown(200)
                    .damage(12.0).damageWas(6.0)
                    .setting("spreadBlocks", 6.0, 3.0, 16.0, Unit.BLOCKS,
                            "How far its lightning runs out over the ground from where the hammer strikes, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_13, "combo").input(CharacterAbility.Input.LEFT).damage(4.0);
            this.add(abilities, AbilitySlot.ABILITY_14, "thunderclap").input(CharacterAbility.Input.LEFT)
                    .holdOnly(ThorPowers.CLAP_HOLD).when(ground).needs(ThorPowers.UNARMED)
                    .cooldown(200).damage(5.0);
            this.add(abilities, AbilitySlot.ABILITY_15, "hammer_uppercut").input(CharacterAbility.Input.LEFT)
                    .holdOnly(ThorPowers.CLAP_HOLD).when(ground).needs(ThorPowers.ARMED)
                    .cooldown(120).damage(8.0);
            this.add(abilities, AbilitySlot.ABILITY_16, "dash").input(CharacterAbility.Input.RIGHT)
                    .when(ground).needs(ThorPowers.UNARMED).cooldown(16)
                    .setting("shortestBlocks", 4.0, 1.0, 16.0, Unit.BLOCKS, "The shortest a dash goes, in blocks")
                    .setting("longestBlocks", 8.0, 1.0, 24.0, Unit.BLOCKS, "The longest a dash goes, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_17, "grab").input(CharacterAbility.Input.RIGHT)
                    .holdOnly(ThorPowers.GRAB_HOLD).when(ground)
                    .needs(ThorPowers.UNARMED | ThorPowers.WALKING).cooldown(100).damage(6.0);
            this.add(abilities, AbilitySlot.ABILITY_18, "grab_dash").input(CharacterAbility.Input.RIGHT)
                    .holdOnly(ThorPowers.GRAB_DASH_HOLD).when(ground)
                    .needs(ThorPowers.UNARMED | ThorPowers.SPRINTING).cooldown(140).damage(6.0);
            this.add(abilities, AbilitySlot.ABILITY_19, "hammer_throw").input(CharacterAbility.Input.RIGHT)
                    .when(ground).needs(ThorPowers.ARMED).cooldown(30).damage(7.0)
                    .setting("throwBlocks", 24.0, 4.0, 64.0, Unit.BLOCKS, "How far the thrown hammer flies, in blocks")
                    .setting("stayBlocks", 128.0, 16.0, 512.0, Unit.BLOCKS,
                            "How far he may go from his resting hammer before it comes home by itself, in blocks")
                    .setting("arcBlocks", 5.0, 1.0, 16.0, Unit.BLOCKS,
                            "Out of his hands the hammer shoots lightning once in 5 seconds at the nearest foe this"
                                    + " near,"
                                    + " in blocks")
                    .setting("arcDamage", 3.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of the hammer's own lightning; it leaps on to two foes near the first, each"
                                    + " taking a little less");
            this.add(abilities, AbilitySlot.ABILITY_20, "hammer_leap").input(CharacterAbility.Input.RIGHT)
                    .holdOnly(ThorPowers.LEAP_HOLD).held().when(ground).needs(ThorPowers.ARMED).cooldown(100)
                    .damage(4.0)
                    .setting("throwBlocks", 14.0, 4.0, 24.0, Unit.BLOCKS, "How far a full draw throws it, in blocks")
                    .setting("dashSpeed", 20.0, 5.0, 60.0, Unit.BLOCKS_PER_SECOND,
                            "How fast he dashes to his hammer, in blocks a second");
            this.add(abilities, AbilitySlot.ABILITY_21, "mjolnir").input(CharacterAbility.Input.SCROLL).when(ground)
                    .needs(ThorPowers.HOME).cooldown(10);
            this.add(abilities, AbilitySlot.ABILITY_22, "charged").input(CharacterAbility.Input.SCROLL)
                    .holdOnly(ThorPowers.CHARGE_HOLD).when(ground).needs(ThorPowers.HOME).cooldown(900)
                    .setting("seconds", 20.0, 1.0, 120.0, Unit.SECONDS, "How long a charge lasts, in seconds");
            this.add(abilities, AbilitySlot.ABILITY_23, "super_jump").input(CharacterAbility.Input.SPACE).doubleTap()
                    .when(ground).cooldown(50)
                    .setting("heightBlocks", 10.0, 2.0, 40.0, Unit.BLOCKS, "How high a super jump goes, in blocks")
                    .setting("floatSeconds", 2.5, 0.0, 10.0, Unit.SECONDS,
                            "How long he hangs in the air at the top of a super jump, in seconds");
            this.add(abilities, AbilitySlot.ABILITY_24, "flight").input(CharacterAbility.Input.SPACE)
                    .holdOnly(ThorPowers.FLIGHT_HOLD).when(ground)
                    .setting("speed", 18.0, 2.0, 80.0, Unit.BLOCKS_PER_SECOND, "How fast he flies, in blocks a second");
            this.add(abilities, AbilitySlot.ABILITY_25, "air_shockwave").input(CharacterAbility.Input.LEFT)
                    .holdOnly(ThorPowers.SHOCK_HOLD).when(flying).needs(ThorPowers.HOME).cooldown(200).damage(3.0);
            this.add(abilities, AbilitySlot.ABILITY_26, "air_blink").input(CharacterAbility.Input.RIGHT)
                    .when(flying).needs(ThorPowers.HOME).cooldown(24);
            this.add(abilities, AbilitySlot.ABILITY_27, "grab_dash_dive").input(CharacterAbility.Input.RIGHT)
                    .holdOnly(ThorPowers.DIVE_HOLD).when(flying).needs(ThorPowers.HOME).cooldown(160).damage(10.0);
            this.add(abilities, AbilitySlot.ABILITY_28, "air_bolt").input(CharacterAbility.Input.SCROLL)
                    .when(flying).needs(ThorPowers.HOME).cooldown(30).damage(5.0);
            this.add(abilities, AbilitySlot.ABILITY_29, "lightning_flight").input(CharacterAbility.Input.SHIFT)
                    .holdOnly(ThorPowers.LIGHTNING_HOLD).when(flying).needs(ThorPowers.HOME).cooldown(400)
                    .cooldownWas(40).damage(3.0)
                    .setting("speed", 48.0, 10.0, 160.0, Unit.BLOCKS_PER_SECOND,
                            "How fast he flies at lightning speed, in blocks a second")
                    .setting("seconds", 15.0, 1.0, 120.0, Unit.SECONDS, "How long lightning speed lasts, in seconds")
                    .setting("landingDamage", 8.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage where he lands at lightning speed, half as much at the edge");
            this.add(abilities, AbilitySlot.ABILITY_30, "hammer_call").input(CharacterAbility.Input.SCROLL)
                    .when(ground).needs(ThorPowers.AWAY).cooldown(10).damage(4.0);
            this.add(abilities, AbilitySlot.ABILITY_31, "hammer_follow").input(CharacterAbility.Input.SCROLL)
                    .holdOnly(ThorPowers.FOLLOW_HOLD).when(ground).needs(ThorPowers.AWAY).cooldown(40)
                    .setting("reachBlocks", 128.0, 4.0, 512.0, Unit.BLOCKS,
                            "How far away his thrown hammer may be for him to dash to it, in blocks")
                    .was(24.0);
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
            if (character.id.equalsIgnoreCase(id)) {
                return character;
            }
        }
        return null;
    }
}
