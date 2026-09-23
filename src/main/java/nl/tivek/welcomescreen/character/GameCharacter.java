package nl.tivek.welcomescreen.character;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.config.Unit;

/**
 * Everyone you can turn into from the wheel (hold G). Picking a character changes you into them; the
 * ability keys stay the same and simply do that character's abilities (see {@link AbilitySlot}).
 *
 * <p>Adding a character is one entry here plus its abilities; the wheel, the key handling, the HUD and
 * the config sections follow by themselves.
 */
public enum GameCharacter {
    /** Doctor Octopus: four robot tentacles on your back. */
    DOC_OCK("doc_ock", 0xA8AEB8) {
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
            // Kept free on purpose: the next ability goes here.
            this.add(abilities, AbilitySlot.ABILITY_8, "placeholder");
            this.add(abilities, AbilitySlot.ABILITY_9, "stance").cooldown(6)
                    .crouch(CharacterAbility.Crouch.ALTERNATE);
            this.add(abilities, AbilitySlot.ABILITY_10, "ground_strike").cooldown(200).damage(25.0)
                    .crouch(CharacterAbility.Crouch.UNDO)
                    .setting("rangeBlocks", 32.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far you can mark a creature, and how far a tentacle travels under the ground")
                    .setting("knockUp", 0.55, 0.0, 3.0, Unit.STRENGTH,
                            "How hard the spike throws what it hits into the air");
            // Kept free as well; its own id because the config has one section per id.
            this.add(abilities, AbilitySlot.ABILITY_11, "placeholder_2");
        }
    },
    /** Green Lantern: a power ring that shapes green hard light into whatever he wills. */
    GREEN_LANTERN("green_lantern", 0x3CE86A) {
        @Override
        void fill(Map<AbilitySlot, CharacterAbility> abilities) {
            // His right hand attacks and his left hand defends: the ring sits on his right middle finger, the
            // fist charges on his right, and the lantern goes in his left hand.
            this.add(abilities, AbilitySlot.ABILITY_1, "giant_fist").held().cooldown(80).damage(12.0)
                    .group("hit", "The hit")
                    .setting("fullChargeDamage", 28.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of a fully charged fist, in half hearts; a smaller one does less, in step with"
                                    + " its width, down to the normal damage for a tap")
                    .was(14.0)
                    .setting("knockback", 1.6, 0.0, 5.0, Unit.STRENGTH, "How hard the fist throws what it hits")
                    .setting("rangeBlocks", 32.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far the fist flies before it falls apart, in blocks")
                    .group("size", "Size and charging")
                    .setting("maxSize", 5.7, 1.0, 32.0, Unit.BLOCKS,
                            "How wide the fist gets when you charge it all the way, in blocks (a tap gives 1)")
                    .was(8.8)
                    .setting("chargeSeconds", 4.1, 0.5, 20.0, Unit.SECONDS,
                            "Seconds of holding the key before the fist is as big as it gets")
                    .group("power", "Ring power")
                    .setting("powerCost", 1.6, 0.0, 100.0, Unit.POWER,
                            "Ring power the smallest fist costs (a full ring holds 100)")
                    .was(10.0, 4.0)
                    .setting("fullChargePowerCost", 3.2, 0.0, 100.0, Unit.POWER,
                            "Ring power a fully charged fist costs: every half second of charging adds an"
                                    + " equal share of the difference")
                    .was(20.0, 11.0, 8.0)
                    .group("blocks", "Smashing blocks")
                    .setting("breakHardness", 3.0, -1.0, 100.0, Unit.HARDNESS,
                            "How hard a block may be for the fist to smash it (dirt 0.5, stone 1.5, wood 2,"
                                    + " iron 5); -1 smashes nothing. Blocks that hold something, like chests,"
                                    + " never break")
                    .settingInt("maxBlocksBroken", 150, 0, 4000, Unit.BLOCK_COUNT,
                            "How many blocks one fist can smash at most before it only pushes through");
            // Hold the key for the wheel of hard-light weapons. Only the menu exists so far, so this one
            // never leaves your own game.
            this.add(abilities, AbilitySlot.ABILITY_2, "construct_wheel").held().clientOnly();
            // Hold the lantern up and smack the ring against it: the ring drinks part of its light.
            this.add(abilities, AbilitySlot.ABILITY_3, "recharge").cooldown(60)
                    .setting("powerRestored", 50.0, 1.0, 100.0, Unit.POWER,
                            "How much power one touch of the lantern puts back in the ring (a full ring holds"
                                    + " 100)");
            // What the ring always does, on the mouse: the right hand attacks (left click) and the left hand
            // defends (right click). A tap does the quick version; holding the button 2 seconds the lasting one.
            this.add(abilities, AbilitySlot.ABILITY_4, "light_bolt").held().mouse(CharacterAbility.Mouse.LEFT)
                    .holdVersion(40, CharacterAbility.Tap.PRESS).damage(6.0)
                    .group("bolt", "Light Bolt (tap the button)")
                    .settingInt("shotTicks", 6, 1, 100, Unit.TICKS,
                            "Ticks before the next bolt can be shot (20 ticks = 1 second)")
                    .setting("powerCost", 0.16, 0.0, 100.0, Unit.POWER, "Ring power one bolt costs")
                    .was(1.0, 0.4)
                    .setting("speedBlocks", 2.4, 0.5, 10.0, Unit.BLOCKS_PER_TICK,
                            "How far a bolt flies per tick, in blocks; while you fly, your own speed comes on top")
                    .setting("rangeBlocks", 48.0, 4.0, 128.0, Unit.BLOCKS,
                            "How far a bolt flies before it fades, in blocks")
                    .group("beam", "Light Beam (hold the button 2 seconds)")
                    .setting("beamDamage", 5.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage the beam does to everything in it, in half hearts, once every beamTicks")
                    .settingInt("beamTicks", 5, 1, 40, Unit.TICKS, "Ticks between two hits of the beam")
                    .setting("beamPowerPerSecond", 0.8, 0.0, 100.0, Unit.POWER_PER_SECOND,
                            "Ring power the beam costs a second")
                    .was(5.0, 2.0)
                    .setting("beamRangeBlocks", 40.0, 4.0, 128.0, Unit.BLOCKS, "How far the beam reaches, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_5, "light_shield").held().mouse(CharacterAbility.Mouse.RIGHT)
                    .holdVersion(40, CharacterAbility.Tap.RELEASE)
                    .group("shield", "Light Shield (tap the button)")
                    .setting("damageKept", 0.3, 0.0, 1.0, Unit.PART_KEPT,
                            "Part of a hit from the front that still gets through the shield (0.3 = 30%, so it"
                                    + " takes 70%). Damage that goes straight through armour, and arrows that pierce,"
                                    + " are not stopped")
                    .was(0.35)
                    .setting("powerPerSecond", 0.08, 0.0, 20.0, Unit.POWER_PER_SECOND,
                            "Ring power the shield costs a second while it is up")
                    .was(0.5, 0.2)
                    .group("dome", "Light Dome (hold the button 2 seconds)")
                    .setting("domeDamageKept", 0.6, 0.0, 1.0, Unit.PART_KEPT,
                            "Part of a hit from any side that still gets through the dome: 0.6 = 60%, so it takes"
                                    + " 40%")
                    .setting("domePowerPerSecond", 0.24, 0.0, 20.0, Unit.POWER_PER_SECOND,
                            "Ring power the dome costs a second")
                    .was(1.5, 0.6)
                    .group("ram", "Ram cone (the shield while you fly)")
                    .setting("ramDamage", 4.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of flying into a creature with the shield up, in half hearts, at the slowest"
                                    + " speed that rams")
                    .setting("ramDamagePerSpeed", 6.0, 0.0, 200.0, Unit.HALF_HEARTS_PER_SPEED,
                            "Extra ram damage in half hearts for every block per tick you fly (1.75 at top speed)")
                    .setting("ramKnockback", 1.4, 0.0, 6.0, Unit.STRENGTH,
                            "How hard a ram throws a creature away; the faster you fly, the further it goes")
                    .setting("ramGroundPowerPerSecond", 2.0, 0.0, 50.0, Unit.POWER_PER_SECOND,
                            "Extra ring power a second while you fly with the ram cone low along the ground or"
                                    + " scrape over it")
                    .setting("ramGroundBlocks", 1.5, 0.2, 6.0, Unit.BLOCKS,
                            "How close above the ground the ram cone counts as scraping along it, in blocks")
                    .setting("ramGroundShake", 1.0, 0.0, 3.0, Unit.STRENGTH,
                            "How hard your view shakes while the ram cone scrapes along the ground (0 = not at"
                                    + " all)");
            // Smash the ring fist into the ground: the ring throws up a construct in front of him that strikes and
            // sends a shockwave over it. In the air he dives down to the ground first. Flying into the ground at full
            // speed does the same by itself, with these same numbers.
            this.add(abilities, AbilitySlot.ABILITY_8, "shockwave").cooldown(100).damage(12.0)
                    .setting("radiusBlocks", 5.0, 1.0, 16.0, Unit.BLOCKS, "How far the shockwave reaches, in blocks")
                    .setting("knockback", 1.2, 0.0, 5.0, Unit.STRENGTH,
                            "How hard the shockwave throws creatures away from where it strikes")
                    .setting("powerCost", 1.6, 0.0, 100.0, Unit.POWER,
                            "Ring power one shockwave costs, also the one of a landing at full speed; without it you"
                                    + " just land")
                    .group("constructs", "The constructs")
                    .setting("constructScale", 1.35, 0.5, 3.0, Unit.STRENGTH,
                            "How big the constructs are: 1 = the size they were made at, 1.35 = 35% bigger. The"
                                    + " bigger they are, the further in front of you they strike")
                    .setting("slowMotion", 1.5, 0.5, 4.0, Unit.STRENGTH,
                            "How slowly the constructs play: 1 = the old pace, 1.5 = half again as slow, 2 = twice"
                                    + " as slow. The shockwave strikes that much later too");
            // Fly: fists to the chest, arms down along the sides and up you go. The ring pays for every second of
            // it, so a full ring lasts a set time in the air.
            this.add(abilities, AbilitySlot.ABILITY_9, "flight").cooldown(20)
                    .setting("powerCost", 0.8, 0.0, 100.0, Unit.POWER, "Ring power you need at least to take off")
                    .was(5.0, 2.0)
                    .setting("fullRingSeconds", 93.75, 1.0, 600.0, Unit.RING_SECONDS,
                            "Seconds a full ring keeps you in the air: flying costs 100 divided by this a second,"
                                    + " and what you shoot or hold up while flying comes on top")
                    .was(15.0, 37.5)
                    .setting("topSpeed", 35.0, 5.0, 150.0, Unit.BLOCKS_PER_SECOND,
                            "Top speed in blocks per second (an elytra with firework rockets does about 33)")
                    .was(50.0);
        }
    };

    private final String id;
    private final int color;
    private final Map<AbilitySlot, CharacterAbility> abilities = new EnumMap<>(AbilitySlot.class);
    private final List<CharacterAbility> ordered = new ArrayList<>();

    GameCharacter(String id, int color) {
        this.id = id;
        this.color = color;
    }

    // Enum constants cannot use their own fields in their constructor, so the abilities are filled in
    // right after all constants exist.
    static {
        for (GameCharacter character : values()) {
            character.fill(character.abilities);
            for (AbilitySlot slot : AbilitySlot.values()) {
                CharacterAbility ability = character.abilities.get(slot);
                if (ability != null) {
                    character.ordered.add(ability);
                }
            }
        }
    }

    abstract void fill(Map<AbilitySlot, CharacterAbility> abilities);

    /** Puts a new ability in a slot and hands it back, so its numbers can be described right after. */
    CharacterAbility add(Map<AbilitySlot, CharacterAbility> abilities, AbilitySlot slot, String id) {
        CharacterAbility ability = new CharacterAbility(this, slot, id);
        abilities.put(slot, ability);
        return ability;
    }

    public String getId() {
        return this.id;
    }

    /** The colour of this character in the wheel and the HUD. */
    public int getColor() {
        return this.color;
    }

    public Component getDisplayName() {
        return Component.translatable("character." + WelcomeScreenMod.MODID + "." + this.id);
    }

    /** What this character does with that key, or null when they have nothing in that slot. */
    @Nullable
    public CharacterAbility ability(AbilitySlot slot) {
        return this.abilities.get(slot);
    }

    /** Every ability this character has, in slot order. */
    public List<CharacterAbility> abilities() {
        return this.ordered;
    }

    /** One of this character's abilities by its own name ("portal"), or null when they have no such thing. */
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
