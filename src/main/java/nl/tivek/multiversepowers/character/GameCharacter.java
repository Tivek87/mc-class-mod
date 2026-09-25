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
            this.add(abilities, AbilitySlot.ABILITY_2, "construct_wheel").held().clientOnly()
                    .group("sword", "Sword (left click)")
                    .setting("swordDamage", 8.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of one cut or thrust, in half hearts; the heavy ones do more, the quick ones less")
                    .setting("swordReach", 3.2, 1.0, 8.0, Unit.BLOCKS, "How far the sword reaches, in blocks")
                    .setting("flurryDamage", 4.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of every one of the twelve stabs of the flurry (hold 2 seconds), in half hearts")
                    .setting("flurryPowerCost", 2.0, 0.0, 100.0, Unit.POWER, "Ring power one flurry costs")
                    .setting("guardDamageKept", 0.4, 0.0, 1.0, Unit.PART_KEPT,
                            "Part of a hit from the front that still gets through the shield held before the chest"
                                    + " during the flurry (0.4 = 40%, so it takes 60%)")
                    .group("wheel_shield", "Shield (right button)")
                    .setting("blockDamageKept", 0.15, 0.0, 1.0, Unit.PART_KEPT,
                            "Part of a hit from the front that still gets through the shield while you block (hold the"
                                    + " right button): 0.15 = 15%, so it takes 85%")
                    .setting("blockPowerPerSecond", 0.08, 0.0, 20.0, Unit.POWER_PER_SECOND,
                            "Ring power holding the shield up to block costs a second")
                    .setting("chargeSpeed", 16.0, 4.0, 40.0, Unit.BLOCKS_PER_SECOND,
                            "How fast the charge behind the shield runs (click the right button), in blocks per"
                                    + " second")
                    .setting("chargeSeconds", 1.4, 0.3, 10.0, Unit.SECONDS,
                            "How long a charge runs at most, in seconds; a wall or a second click ends it sooner")
                    .was(3.0)
                    .setting("chargeDamage", 3.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of a ram of the shield to every creature in the way of a charge, in half hearts;"
                                    + " the heavier rams do a little more")
                    .setting("bashKnockback", 1.4, 0.0, 5.0, Unit.STRENGTH,
                            "How hard a ram of the shield throws what stands in the way of a charge aside")
                    .setting("chargePowerCost", 2.0, 0.0, 100.0, Unit.POWER, "Ring power one charge costs")
                    .setting("slamDamage", 6.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage in the middle of the small shockwave that ends a charge, in half hearts; half at"
                                    + " its edge")
                    .setting("slamRadius", 3.5, 0.5, 10.0, Unit.BLOCKS,
                            "How far that shockwave reaches, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_3, "recharge").cooldown(60)
                    .setting("powerRestored", 50.0, 1.0, 100.0, Unit.POWER,
                            "How much power one touch of the lantern puts back in the ring (a full ring holds"
                                    + " 100)");
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
                    .setting("beamRangeBlocks", 40.0, 4.0, 128.0, Unit.BLOCKS, "How far the beam reaches, in blocks")
                    .setting("beamKnockback", 0.25, 0.0, 3.0, Unit.STRENGTH,
                            "How hard every hit of the beam drives what it hits back (0 = not at all)");
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
                    .setting("ramDamagePerSpeed", 10.5, 0.0, 200.0, Unit.HALF_HEARTS_PER_SPEED,
                            "Extra ram damage in half hearts for every block per tick you fly (about 0.96 at top"
                                    + " speed)")
                    .was(6.0)
                    .setting("ramKnockback", 1.4, 0.0, 6.0, Unit.STRENGTH,
                            "How hard a ram throws a creature away; the faster you fly, the further it goes")
                    .setting("ramGroundPowerPerSecond", 2.0, 0.0, 50.0, Unit.POWER_PER_SECOND,
                            "Extra ring power a second while you fly with the ram cone low along the ground or"
                                    + " scrape over it")
                    .setting("ramGroundBlocks", 1.5, 0.2, 6.0, Unit.BLOCKS,
                            "How close above the ground the ram cone counts as scraping along it, in blocks");
            this.add(abilities, AbilitySlot.ABILITY_6, "ring_scan").cooldown(160)
                    .setting("rangeBlocks", 56.0, 8.0, 128.0, Unit.BLOCKS,
                            "How far the scan reaches, through walls and all, in blocks")
                    .was(32.0)
                    .setting("markSeconds", 21.0, 2.0, 120.0, Unit.SECONDS,
                            "How long every creature the scan passed stays marked for you, in seconds")
                    .was(12.0)
                    .setting("powerCost", 2.0, 0.0, 100.0, Unit.POWER, "Ring power one scan costs");
            this.add(abilities, AbilitySlot.ABILITY_7, "air_strike").cooldown(1800).damage(48.0).damageWas(40.0)
                    .setting("attackSeconds", 20.0, 2.0, 60.0, Unit.SECONDS,
                            "How long the plane drones on and fires before it plunges down, in seconds; its engine"
                                    + " bursts and its jets race off in the last 1.8 seconds of it")
                    .was(10.0)
                    .setting("scanBlocks", 84.0, 8.0, 160.0, Unit.BLOCKS,
                            "How far its sensor scans the ground round it for creatures to fire at, in blocks (the"
                                    + " Ring Scan reaches 56)")
                    .setting("powerCost", 20.0, 0.0, 100.0, Unit.POWER, "Ring power the air strike costs")
                    .group("guns", "The miniguns")
                    .setting("gunTicks", 2.79, 1.0, 100.0, Unit.TICKS,
                            "Ticks between two rounds of one minigun; the two fire in turn, so together they fire"
                                    + " twice in that time (20 ticks = 1 second)")
                    .was(6.0, 4.6)
                    .setting("gunDamage", 3.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of one round that strikes, in half hearts")
                    .setting("gunSpread", 2.4, 0.0, 12.0, Unit.BLOCKS,
                            "How far round what they aim at the rounds spread, in blocks: the wider, the fewer strike")
                    .group("missiles", "Homing missiles")
                    .settingInt("missileTicks", 40, 4, 400, Unit.TICKS,
                            "Ticks between two missiles dropping out of the hatch in its belly (20 ticks = 1 second)")
                    .was(10.0)
                    .setting("missileDamage", 24.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of a missile to the creature it strikes, in half hearts; what else its blast"
                                    + " reaches takes less")
                    .was(8.0)
                    .setting("missileCraterRadius", 2.2, 0.0, 6.0, Unit.BLOCKS,
                            "How wide the small crater a missile blows out of the ground is, from its middle, in blocks"
                                    + " (0 = none; the crash's block hardness applies here too)")
                    .group("jets", "The jets")
                    .settingInt("jetMissileTicks", 24, 4, 400, Unit.TICKS,
                            "Ticks between two small missiles of one jet, fired at the creature out to hurt you nearest"
                                    + " to it (20 ticks = 1 second)")
                    .setting("jetMissileDamage", 5.0, 0.0, 2000.0, Unit.HALF_HEARTS,
                            "Damage of a jet's small missile to the creature it strikes, in half hearts; what else its"
                                    + " small blast reaches takes less")
                    .group("crash", "The crash")
                    .setting("crashRadius", 80.0, 2.0, 200.0, Unit.BLOCKS,
                            "How far the blast of the crash reaches, in blocks")
                    .was(12.0, 14.0, 16.0)
                    .setting("craterRadius", 8.0, 0.0, 16.0, Unit.BLOCKS,
                            "How wide the crater the crash blows out of the ground is, from its middle, in blocks (0 ="
                                    + " no crater)")
                    .was(7.0)
                    .setting("breakHardness", 3.0, -1.0, 100.0, Unit.HARDNESS,
                            "How hard a block may be for the crash to blow it away (dirt 0.5, stone 1.5, wood 2, iron"
                                    + " 5); -1 leaves the ground alone. Blocks that hold something, like chests, stay")
                    .settingInt("debrisBlocks", 90, 0, 400, Unit.BLOCK_COUNT,
                            "How many of the crater's blocks are hurled up and away, to come down all round it")
                    .was(40.0);
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
            this.add(abilities, AbilitySlot.ABILITY_9, "flight").cooldown(20)
                    .setting("powerCost", 0.8, 0.0, 100.0, Unit.POWER, "Ring power you need at least to take off")
                    .was(5.0, 2.0)
                    .setting("fullRingSeconds", 93.75, 1.0, 600.0, Unit.RING_SECONDS,
                            "Seconds a full ring keeps you in the air: flying costs 100 divided by this a second,"
                                    + " and what you shoot or hold up while flying comes on top")
                    .was(15.0, 37.5)
                    .setting("topSpeed", 6.25625, 5.0, 150.0, Unit.BLOCKS_PER_SECOND,
                            "Top speed in blocks per second (an elytra with firework rockets does about 33)")
                    .was(50.0, 35.0, 19.25, 9.625)
                    .setting("startSpeed", 4.16, 1.0, 150.0, Unit.BLOCKS_PER_SECOND,
                            "Speed you set off at, in blocks per second: the longer you fly on, the faster you go, up"
                                    + " to the top speed")
                    .was(11.7, 6.4)
                    .setting("cruiseSpeed", 5.2, 1.0, 150.0, Unit.BLOCKS_PER_SECOND,
                            "Cruising speed in blocks per second: flying on, you are up to it within a moment, and"
                                    + " from there you keep gaining, up to the top speed")
                    .was(13.0, 8.0)
                    .setting("cruiseSeconds", 0.5, 0.0, 60.0, Unit.SECONDS,
                            "Seconds of flying on from the speed you set off at to the cruising speed (0 = straight"
                                    + " away)")
                    .was(3.0)
                    .setting("speedUpSeconds", 7.0, 0.0, 300.0, Unit.SECONDS,
                            "Seconds of flying on from the cruising speed to the top speed (0 = straight away);"
                                    + " letting go of forward loses the speed again in a few seconds")
                    .was(12.0, 30.0, 5.6)
                    .group("chunks", "The world ahead")
                    .settingInt("chunkRadiusBlocks", 128, 0, 256, Unit.BLOCKS,
                            "How far around a flyer the server makes the world ready while he flies (made, loaded and"
                                    + " sent to him), in blocks, so he never flies into land that is not there yet;"
                                    + " 0 = only ahead of him")
                    .setting("chunkAheadSeconds", 8.0, 0.0, 30.0, Unit.SECONDS,
                            "How many seconds of flying ahead of a flyer the server makes the world ready as well, along"
                                    + " the way he flies and at the speed he flies (at most 512 blocks ahead)");
            this.add(abilities, AbilitySlot.ABILITY_10, "giant_hands").cooldown(600).damage(12.0)
                    .setting("radiusBlocks", 20.0, 4.0, 48.0, Unit.BLOCKS,
                            "How far round you the hands come up at creatures out to hurt you, every way, in blocks"
                                    + " (20 = an area 40 blocks across)")
                    .settingInt("fewestHands", 4, 1, 30, Unit.COUNT,
                            "The fewest hands that come up at every press: each press brings a number picked at random"
                                    + " from this to mostHands, one after another, at most five at once (a pair with"
                                    + " an axe counts as one)")
                    .settingInt("mostHands", 8, 1, 30, Unit.COUNT,
                            "The most hands that come up at every press")
                    .settingInt("handTicks", 10, 1, 200, Unit.TICKS,
                            "Ticks from one hand coming up to the next (20 ticks = 1 second); while five are up, the"
                                    + " next waits for one to go")
                    .setting("knockback", 2.0, 0.0, 5.0, Unit.STRENGTH,
                            "How hard the hands send a creature flying (a middle finger bursting out of the ground and"
                                    + " the axe of a pair far harder)")
                    .setting("powerCost", 8.0, 0.0, 100.0, Unit.POWER, "Ring power the hands cost");
            this.add(abilities, AbilitySlot.ABILITY_11, "light_bubble").cooldown(240).damage(12.0)
                    .crouch(CharacterAbility.Crouch.UNDO)
                    .setting("rangeBlocks", 24.0, 4.0, 64.0, Unit.BLOCKS,
                            "How far away a creature can be caught in a bubble, in blocks")
                    .setting("holdSeconds", 6.0, 1.0, 30.0, Unit.SECONDS,
                            "How long a bubble holds its creature before it bursts by itself, in seconds")
                    .setting("liftBlocks", 3.0, 0.0, 10.0, Unit.BLOCKS,
                            "How high a bubble lifts its creature off the ground, in blocks")
                    .setting("slamRadius", 4.5, 0.0, 10.0, Unit.BLOCKS,
                            "How far the shockwave of the last slam of a pound reaches, in blocks (the slams before it"
                                    + " reach less far): what else stands in it is thrown away and takes half the"
                                    + " damage")
                    .was(3.5)
                    .setting("powerCost", 4.0, 0.0, 100.0, Unit.POWER, "Ring power catching a creature costs");
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
                if (ability != null) {
                    character.ordered.add(ability);
                }
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
