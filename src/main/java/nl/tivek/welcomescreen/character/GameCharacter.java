package nl.tivek.welcomescreen.character;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.network.chat.Component;
import nl.tivek.welcomescreen.WelcomeScreenMod;

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
                    .setting("rangeBlocks", 20.0, 2.0, 64.0,
                            "How far a tentacle can reach out to grab, in blocks")
                    .setting("smashSpeed", 0.35, 0.05, 3.0,
                            "How fast a held creature must hit a wall or the ground to be hurt");
            this.add(abilities, AbilitySlot.ABILITY_2, "multi_tentacle").cooldown(120).damage(4.0)
                    .setting("rangeBlocks", 8.0, 2.0, 32.0,
                            "How far the tentacles look for a target, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_3, "dash").cooldown(60)
                    .setting("staminaCost", 30.0, 0.0, 200.0, "Stamina one dash costs")
                    .setting("speed", 2.6, 0.5, 8.0,
                            "How hard the tentacles throw you: higher is a longer dash");
            this.add(abilities, AbilitySlot.ABILITY_4, "block").held()
                    .setting("damageKept", 0.15, 0.0, 1.0,
                            "Part of a blocked hit that still gets through (0.15 = 15%)")
                    .setting("staminaPerTick", 0.6, 0.0, 20.0, "Stamina blocking costs per tick");
            this.add(abilities, AbilitySlot.ABILITY_5, "ground_slam").cooldown(160).damage(7.0)
                    .crouch(CharacterAbility.Crouch.ALTERNATE)
                    .setting("airDamage", 10.0, 0.0, 2000.0, "Damage when you slam down out of the air")
                    .setting("heldSlamDamage", 14.0, 0.0, 2000.0,
                            "Damage when you smash the creatures you hold into the ground");
            this.add(abilities, AbilitySlot.ABILITY_6, "portal").cooldown(400).damage(50.0)
                    .setting("homingRangeBlocks", 30.0, 4.0, 64.0,
                            "How far the tentacle hunts after it comes out of the second portal, in blocks")
                    .setting("portalSpreadBlocks", 14.0, 4.0, 64.0,
                            "How far apart the three portals open, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_7, "rampage").cooldown(1800).damage(3.0)
                    .settingInt("durationTicks", 400, 20, 6000, "How long the rampage lasts, in ticks");
            // Kept free on purpose: the next ability goes here.
            this.add(abilities, AbilitySlot.ABILITY_8, "placeholder");
            this.add(abilities, AbilitySlot.ABILITY_9, "stance").cooldown(6)
                    .crouch(CharacterAbility.Crouch.ALTERNATE);
            this.add(abilities, AbilitySlot.ABILITY_10, "ground_strike").cooldown(200).damage(25.0)
                    .crouch(CharacterAbility.Crouch.UNDO)
                    .setting("rangeBlocks", 32.0, 4.0, 64.0,
                            "How far you can mark a creature, and how far a tentacle travels under the ground")
                    .setting("knockUp", 0.55, 0.0, 3.0,
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
                    .setting("fullChargeDamage", 28.0, 0.0, 2000.0,
                            "Damage of a fully charged fist; a smaller one does less, in step with its width,"
                                    + " down to the normal damage for a tap")
                    .setting("rangeBlocks", 32.0, 4.0, 64.0,
                            "How far the fist flies before it falls apart, in blocks")
                    .setting("knockback", 1.6, 0.0, 5.0, "How hard the fist throws what it hits")
                    .setting("maxSize", 5.7, 1.0, 32.0,
                            "How wide the fist gets when you charge it all the way, in blocks (a tap gives 1)")
                    .setting("chargeSeconds", 4.1, 0.5, 20.0,
                            "Seconds of holding the key before the fist is as big as it gets")
                    .setting("powerCost", 10.0, 0.0, 100.0,
                            "Ring power the smallest fist costs (a full ring holds 100)")
                    .setting("fullChargePowerCost", 20.0, 0.0, 100.0,
                            "Ring power a fully charged fist costs: every half second of charging adds an"
                                    + " equal share of the difference")
                    .setting("breakHardness", 3.0, -1.0, 100.0,
                            "How hard a block may be for the fist to smash it (dirt 0.5, stone 1.5, wood 2,"
                                    + " iron 5); -1 smashes nothing. Blocks that hold something, like chests,"
                                    + " never break")
                    .settingInt("maxBlocksBroken", 150, 0, 4000,
                            "How many blocks one fist can smash at most before it only pushes through");
            // Hold the key for the wheel of hard-light weapons. Only the menu exists so far, so this one
            // never leaves your own game.
            this.add(abilities, AbilitySlot.ABILITY_2, "construct_wheel").held().clientOnly();
            // Hold the lantern up and smack the ring against it: the ring drinks part of its light.
            this.add(abilities, AbilitySlot.ABILITY_3, "recharge").cooldown(60)
                    .setting("powerRestored", 50.0, 1.0, 100.0,
                            "How much power one touch of the lantern puts back in the ring (a full ring holds"
                                    + " 100)");
            // What the ring always does, on the mouse: the right hand attacks (left click) and the left hand
            // defends (right click). A tap does the quick version; holding the button 2 seconds the lasting one.
            this.add(abilities, AbilitySlot.ABILITY_4, "light_bolt").held().mouse(CharacterAbility.Mouse.LEFT)
                    .holdVersion(40, CharacterAbility.Tap.PRESS).damage(6.0)
                    .settingInt("shotTicks", 6, 1, 100, "Ticks before the next bolt can be shot (20 ticks = 1 second)")
                    .setting("powerCost", 1.0, 0.0, 100.0, "Ring power one bolt costs")
                    .setting("speedBlocks", 2.4, 0.5, 10.0,
                            "How far a bolt flies per tick, in blocks; while you fly, your own speed comes on top")
                    .setting("rangeBlocks", 48.0, 4.0, 128.0,
                            "How far a bolt flies before it fades, in blocks")
                    .setting("beamDamage", 5.0, 0.0, 2000.0,
                            "Damage the beam (hold the button) does to everything in it, once every beamTicks")
                    .settingInt("beamTicks", 5, 1, 40, "Ticks between two hits of the beam")
                    .setting("beamPowerPerSecond", 5.0, 0.0, 100.0, "Ring power the beam costs a second")
                    .setting("beamRangeBlocks", 40.0, 4.0, 128.0, "How far the beam reaches, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_5, "light_shield").held().mouse(CharacterAbility.Mouse.RIGHT)
                    .holdVersion(40, CharacterAbility.Tap.RELEASE)
                    .setting("damageKept", 0.3, 0.0, 1.0,
                            "Part of a hit from the front that still gets through the shield (0.3 = 30%, so it"
                                    + " takes 70%). Damage that goes straight through armour, and arrows that pierce,"
                                    + " are not stopped")
                    .setting("powerPerSecond", 0.5, 0.0, 20.0, "Ring power the shield costs a second while it is up")
                    .setting("domeDamageKept", 0.6, 0.0, 1.0,
                            "Part of a hit from any side that still gets through the dome (hold the button): 0.6"
                                    + " = 60%, so it takes 40%")
                    .setting("domePowerPerSecond", 1.5, 0.0, 20.0, "Ring power the dome costs a second")
                    .setting("ramDamage", 4.0, 0.0, 2000.0,
                            "Damage of flying into a creature with the shield up, at the slowest speed that rams")
                    .setting("ramDamagePerSpeed", 6.0, 0.0, 200.0,
                            "Extra ram damage for every block per tick you fly (2.5 at top speed)")
                    .setting("ramKnockback", 1.4, 0.0, 6.0,
                            "How hard a ram throws a creature away; the faster you fly, the further it goes");
            // Fly: fists to the chest, arms down along the sides and up you go. The ring pays for every second of
            // it, so a full ring lasts a set time in the air.
            this.add(abilities, AbilitySlot.ABILITY_9, "flight").cooldown(20)
                    .setting("powerCost", 5.0, 0.0, 100.0, "Ring power you need at least to take off")
                    .setting("fullRingSeconds", 15.0, 1.0, 600.0,
                            "Seconds a full ring keeps you in the air: flying costs 100 divided by this a second,"
                                    + " and what you shoot or hold up while flying comes on top")
                    .setting("topSpeed", 50.0, 5.0, 150.0,
                            "Top speed in blocks per second (an elytra with firework rockets does about 33)");
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
