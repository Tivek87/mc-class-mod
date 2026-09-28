package nl.tivek.multiversepowers.config.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;
import nl.tivek.multiversepowers.config.ModConfigs;
import nl.tivek.multiversepowers.config.Unit;

public final class ClientSettings {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue CAMERA_SHAKE;
    public static final ModConfigSpec.DoubleValue RAM_GROUND_SHAKE;
    public static final ModConfigSpec.DoubleValue MECH_STEP_SHAKE;
    public static final ModConfigSpec.DoubleValue SCREEN_FLASH;
    public static final ModConfigSpec.DoubleValue SPEED_FOV;
    public static final ModConfigSpec.DoubleValue VIEW_TILT;
    public static final ModConfigSpec.IntValue MECH_CINEMATIC;
    public static final ModConfigSpec.DoubleValue MECH_CHASE;
    public static final ModConfigSpec.IntValue MECH_LAMP;
    public static final ModConfigSpec.DoubleValue MECH_LAMP_BRIGHTNESS;
    public static final ModConfigSpec.DoubleValue MECH_LAMP_REACH;
    public static final ModConfigSpec.DoubleValue PARTICLE_AMOUNT;
    public static final ModConfigSpec.DoubleValue GLOW_STRENGTH;
    public static final ModConfigSpec.DoubleValue FLARE_STRENGTH;
    public static final ModConfigSpec.IntValue LENS;
    public static final ModConfigSpec.IntValue EFFECT_DETAIL;
    public static final ModConfigSpec.IntValue RAGDOLLS;
    public static final ModConfigSpec.IntValue RAGDOLL_MOST;
    public static final ModConfigSpec.DoubleValue CORPSE_SECONDS;
    public static final ModConfigSpec.IntValue RAGDOLL_REACH;
    public static final ModConfigSpec.DoubleValue LIE_SECONDS;
    public static final ModConfigSpec.IntValue BLAST_BODIES;
    public static final ModConfigSpec.IntValue RAGDOLL_STEPS;
    public static final ModConfigSpec.IntValue ASHES;
    public static final ModConfigSpec.IntValue FOOT_PLANTING;
    public static final ModConfigSpec.IntValue CAPE_CLOTH;
    public static final ModConfigSpec.IntValue CAPE_STEPS;
    public static final ModConfigSpec.DoubleValue CAPE_REACH;
    public static final ModConfigSpec.IntValue THEME_MUSIC;
    public static final ModConfigSpec.DoubleValue MOD_SOUNDS;
    public static final ModConfigSpec.DoubleValue FLIGHT_SOUNDS;
    public static final ModConfigSpec.DoubleValue POWER_HUM;
    public static final ModConfigSpec.DoubleValue MECH_STEPS;
    public static final ModConfigSpec.IntValue UI_SOUNDS;
    public static final ModConfigSpec.IntValue ABILITY_PANEL;
    public static final ModConfigSpec.DoubleValue PANEL_SCALE;
    public static final ModConfigSpec.IntValue POWER_BAR;
    public static final ModConfigSpec.IntValue HOLD_RINGS;
    public static final ModConfigSpec.IntValue GAUGES;
    public static final ModConfigSpec.IntValue STAMINA_BAR;
    public static final ModConfigSpec.IntValue RING_SIGHT;
    public static final ModConfigSpec.DoubleValue CONSTRUCT_DISTANCE;
    public static final ModConfigSpec.IntValue RENDER_THREADS;
    public static final ModConfigSpec.IntValue UPDATE_CHECK;
    public static final ModConfigSpec.DoubleValue UPDATE_POPUP;

    private static final List<Entry> ENTRIES = new ArrayList<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment(ModConfigs.WIP, "Your own settings: only what you see and feel, in your own game, whatever world or server you"
                + " play on.", "The same numbers can be changed in the game: Mods > this mod > Config.").push("view");
        Sheet sheet = new Sheet(builder, "view");
        CAMERA_SHAKE = sheet.number("cameraShake", "How hard your view shakes and jolts from the powers: a landing slam,"
                + " a crash or a blast nearby, a blow of the sword, the beam breaking loose, the ring arriving (1 = as the"
                + " mod makes it, 0 = never)", 1.0, 0.0, 2.0, Unit.STRENGTH, 0.1);
        RAM_GROUND_SHAKE = sheet.number("ramGroundShake", "How hard your view shakes while you fly with the ram cone low"
                + " along the ground (0 = not at all); the camera shake above scales it too", 1.0, 0.0, 3.0,
                Unit.STRENGTH, 0.1);
        MECH_STEP_SHAKE = sheet.number("mechStepShake", "How hard a mech's footsteps shake your view (0 = not at all);"
                + " the camera shake above scales it too", 1.0, 0.0, 2.0, Unit.STRENGTH, 0.1);
        SCREEN_FLASH = sheet.number("screenFlash", "How bright the screen flashes when a blast or a thunderclap goes"
                + " off near you (1 = as the mod makes it, 0 = never)", 1.0, 0.0, 2.0, Unit.PERCENT, 0.05);
        SPEED_FOV = sheet.number("speedFov", "How far your view widens at speed: flying fast, charging a sword blow"
                + " (1 = as the mod makes it, 0 = never; the game's own FOV effects setting scales it too)", 1.0, 0.0,
                2.0, Unit.PERCENT, 0.05);
        VIEW_TILT = sheet.number("viewTilt", "How far your view dips and tilts with a landing slam or a take-off (1 ="
                + " as the mod makes it, 0 = never)", 1.0, 0.0, 2.0, Unit.PERCENT, 0.05);
        MECH_CINEMATIC = sheet.toggle("mechCinematic", "Film your own mech while it builds itself, shot after shot like"
                + " a movie, before you look out of its cockpit (1 = yes, 0 = keep your own view)", true);
        MECH_CHASE = sheet.number("mechChaseDistance", "How far behind your mech the camera stands in third person,"
                + " in blocks", 14.0, 6.0, 40.0, Unit.BLOCKS, 1.0);
        MECH_LAMP = sheet.choice("mechLamp", "The lamp on a mech's chest: 0 = never on, 1 = on in the dark, 2 ="
                + " always on", 1, 3);
        MECH_LAMP_BRIGHTNESS = sheet.number("mechLampBrightness", "How bright the mech's lamp shines",
                1.0, 0.2, 2.0, Unit.PERCENT, 0.05);
        MECH_LAMP_REACH = sheet.number("mechLampReach", "How far the mech's lamp reaches, in blocks", 40.0, 10.0,
                64.0, Unit.BLOCKS, 2.0);
        sheet.section("effects");
        PARTICLE_AMOUNT = sheet.number("particleAmount", "How many particles the powers throw up: sparks, dust, smoke,"
                + " splashes (1 = as the mod makes them, 0 = none)", 1.0, 0.0, 2.0, Unit.PERCENT, 0.05);
        GLOW_STRENGTH = sheet.number("glowStrength", "How strongly the soft glow round light and constructs shines"
                + " (1 = as the mod makes it, 0 = no glow)", 1.0, 0.0, 2.0, Unit.PERCENT, 0.05);
        FLARE_STRENGTH = sheet.number("flareStrength", "How bright the flares are: the stars of light where a power"
                + " bursts, lands or forms (1 = as the mod makes them, 0 = none)", 1.0, 0.0, 2.0, Unit.PERCENT, 0.05);
        LENS = sheet.toggle("lensDistortion", "Blasts and bubbles bend the view behind them like a lens (1 = yes,"
                + " 0 = never)", true);
        EFFECT_DETAIL = sheet.choice("effectDetail", "How much detail the biggest effects have: the thunderclap's"
                + " streaks, the flamethrower's flames, a lamp's patch of light (0 = low, 1 = medium, 2 = full)", 2, 3);
        sheet.section("bodies");
        RAGDOLLS = sheet.toggle("ragdolls", "Creatures go limp: they fall, tumble and hang from what holds them, and a"
                + " body stays where it fell before it sinks away (1 = yes, 0 = as in the plain game)", true);
        RAGDOLL_MOST = sheet.whole("ragdollMost", "How many limp creatures and bodies there may be at once; the"
                + " furthest go first", 24, 1, 64, Unit.COUNT, 1.0);
        CORPSE_SECONDS = sheet.number("corpseSeconds", "How long a body stays where it fell before it sinks into the"
                + " ground, in seconds", 10.0, 0.0, 120.0, Unit.SECONDS, 1.0);
        RAGDOLL_REACH = sheet.whole("ragdollReach", "How far away a creature may be and still go limp, in blocks", 48,
                8, 128, Unit.BLOCKS, 4.0);
        LIE_SECONDS = sheet.number("ragdollLieSeconds", "How long a creature thrown limp lies still on the ground before"
                + " it gets back up, in seconds", 1.5, 0.5, 6.0, Unit.SECONDS, 0.5);
        BLAST_BODIES = sheet.toggle("blastThrowsBodies", "Explosions throw limp creatures and bodies away by their"
                + " weight (1 = yes, 0 = they stay where they lie)", true);
        RAGDOLL_STEPS = sheet.whole("ragdollSteps", "How finely a limp body's fall is worked out, in steps a tick: more"
                + " is smoother and costs more", 20, 6, 40, Unit.COUNT, 1.0);
        ASHES = sheet.toggle("ashBodies", "A creature a fire power kills burns to ash: it glows, chars, greys and falls"
                + " apart (1 = yes, 0 = it dies as usual)", true);
        FOOT_PLANTING = sheet.toggle("footPlanting", "Feet rest on the ground they stand on: a foot never sinks into a"
                + " step, and a spider's legs reach down to the ground (1 = yes, 0 = as in the plain game)", true);
        CAPE_CLOTH = sheet.toggle("capeCloth", "Capes are cloth: they hang, trail and swing as players run and turn,"
                + " and fold against their back and legs (1 = yes, 0 = the game's stiff cape)", true);
        CAPE_STEPS = sheet.whole("capeSteps", "How finely a cape's cloth is worked out, in steps a tick: more is"
                + " smoother and costs more", 8, 2, 16, Unit.COUNT, 1.0);
        CAPE_REACH = sheet.number("capeReach", "How far away a player's cape is still cloth, in blocks", 32.0, 8.0,
                96.0, Unit.BLOCKS, 4.0);
        sheet.section("sound");
        THEME_MUSIC = sheet.toggle("themeMusic", "Play the multiverse theme in the main menu (1 = yes, 0 = the game's"
                + " own menu music)", true);
        MOD_SOUNDS = sheet.number("modSounds", "How loud the mod's own sounds are: the Emerald Express, the mech, the"
                + " ring (1 = as made, 0 = silent)", 1.0, 0.0, 2.0, Unit.PERCENT, 0.05);
        FLIGHT_SOUNDS = sheet.number("flightSounds", "How loud the wind and jets of flight are (1 = as made, 0 ="
                + " silent)", 1.0, 0.0, 2.0, Unit.PERCENT, 0.05);
        POWER_HUM = sheet.number("powerHum", "How loud the hums and roars that go on while a power lasts are: the"
                + " beam charging, the flamethrower, the planes, the train's rumble (1 = as made, 0 = silent)", 1.0,
                0.0, 2.0, Unit.PERCENT, 0.05);
        MECH_STEPS = sheet.number("mechSteps", "How loud a mech's footsteps are (1 = as made, 0 = silent)", 1.0, 0.0,
                2.0, Unit.PERCENT, 0.05);
        UI_SOUNDS = sheet.toggle("uiSounds", "The clicks of the power wheel and the construct wheel (1 = on, 0 ="
                + " silent)", true);
        sheet.section("hud");
        ABILITY_PANEL = sheet.toggle("abilityPanel", "The panel with your abilities, keys and cooldowns in the bottom"
                + " right (1 = shown, 0 = hidden)", true);
        PANEL_SCALE = sheet.number("abilityPanelScale", "How big the ability panel is drawn", 1.0, 0.5, 1.5,
                Unit.PERCENT, 0.05);
        POWER_BAR = sheet.toggle("powerBar", "The ring's power bar (1 = shown, 0 = hidden)", true);
        HOLD_RINGS = sheet.toggle("holdRings", "The rings round the crosshair that fill while you hold a key (1 ="
                + " shown, 0 = hidden)", true);
        GAUGES = sheet.toggle("gauges", "The gauges on held constructs and the beam: heat, stage and lock (1 = shown,"
                + " 0 = hidden)", true);
        STAMINA_BAR = sheet.toggle("staminaBar", "The stamina bar (1 = shown, 0 = hidden)", true);
        RING_SIGHT = sheet.toggle("ringSight", "The marks the ring scan puts on creatures (1 = shown, 0 = hidden)",
                true);
        sheet.section("performance");
        CONSTRUCT_DISTANCE = sheet.number("constructDistance", "How far away constructs are still drawn, in blocks",
                256.0, 32.0, 512.0, Unit.BLOCKS, 16.0);
        RENDER_THREADS = sheet.whole("renderThreads", "How many of your processor's cores may work out big"
                + " constructs at once (0 = as many as help, up to 8)", 0, 0, 16, Unit.COUNT, 1.0);
        sheet.section("updates");
        UPDATE_CHECK = sheet.whole("updateCheckMinutes", "How often the game looks for a new version of the mod, in"
                + " minutes (0 = never)", 5, 0, 120, Unit.MINUTES, 1.0);
        UPDATE_POPUP = sheet.number("updatePopupSeconds", "How long the note about a new version stays on screen while"
                + " you play, in seconds (0 = only in the menus)", 15.0, 0.0, 120.0, Unit.SECONDS, 1.0);
        builder.pop();
        SPEC = builder.build();
    }

    // One line of the settings screen: the file's section and key, the value, how it reads and how far one step
    // moves it; choices counts the named choices of a CHOICE.
    public record Entry(String section, String key, ModConfigSpec.ConfigValue<? extends Number> value, Unit unit,
            double step, int choices) {
    }

    public static List<Entry> entries() {
        return Collections.unmodifiableList(ENTRIES);
    }

    // Defines each setting once, in the order the settings screen lists them.
    private static final class Sheet {
        private final ModConfigSpec.Builder builder;
        private String section;

        Sheet(ModConfigSpec.Builder builder, String section) {
            this.builder = builder;
            this.section = section;
        }

        void section(String name) {
            this.builder.pop();
            this.builder.push(name);
            this.section = name;
        }

        ModConfigSpec.DoubleValue number(String key, String comment, double value, double min, double max, Unit unit,
                double step) {
            ModConfigSpec.DoubleValue defined = this.builder.comment(comment).defineInRange(key, value, min, max);
            ENTRIES.add(new Entry(this.section, key, defined, unit, step, 0));
            return defined;
        }

        ModConfigSpec.IntValue whole(String key, String comment, int value, int min, int max, Unit unit,
                double step) {
            ModConfigSpec.IntValue defined = this.builder.comment(comment).defineInRange(key, value, min, max);
            ENTRIES.add(new Entry(this.section, key, defined, unit, step, 0));
            return defined;
        }

        ModConfigSpec.IntValue toggle(String key, String comment, boolean on) {
            return this.whole(key, comment, on ? 1 : 0, 0, 1, Unit.SWITCH, 1.0);
        }

        ModConfigSpec.IntValue choice(String key, String comment, int value, int count) {
            ModConfigSpec.IntValue defined = this.builder.comment(comment).defineInRange(key, value, 0, count - 1);
            ENTRIES.add(new Entry(this.section, key, defined, Unit.CHOICE, 1.0, count));
            return defined;
        }
    }

    private ClientSettings() {
    }

    public static double get(ModConfigSpec.DoubleValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static int get(ModConfigSpec.IntValue value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean themeMusic() {
        return get(THEME_MUSIC) != 0;
    }

    public static long updateCheckMs() {
        return get(UPDATE_CHECK) * 60_000L;
    }

    public static long updatePopupMs() {
        return Math.round(get(UPDATE_POPUP) * 1000.0);
    }

    public static float cameraShake() {
        return (float) get(CAMERA_SHAKE);
    }

    public static boolean mechCinematic() {
        return get(MECH_CINEMATIC) != 0;
    }

    public static int mechLamp() {
        return get(MECH_LAMP);
    }

    public static boolean ragdolls() {
        return get(RAGDOLLS) != 0;
    }

    public static boolean footPlanting() {
        return get(FOOT_PLANTING) != 0;
    }

    public static boolean capeCloth() {
        return get(CAPE_CLOTH) != 0;
    }

    // A switch of this file (see Sheet.toggle).
    public static boolean on(ModConfigSpec.IntValue value) {
        return get(value) != 0;
    }

    public static float factor(ModConfigSpec.DoubleValue value) {
        return (float) get(value);
    }

    // Every how many of a big effect's many pieces is drawn: 1 at full detail, 2 at medium, 3 at low.
    public static int detailStep() {
        return 3 - get(EFFECT_DETAIL);
    }
}
