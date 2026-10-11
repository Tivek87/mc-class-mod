package nl.tivek.multiversepowers.stage;

import java.util.Arrays;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// One lamp's own settings, set with sneak + right click: kept on its block, sent to every player near, and read back
// from anything (a file, a player's change) only through `read`, which keeps every number within its range.
public final class LampSettings {
    public static final int MOST_COLORS = 8;
    public static final int CHANNELS = 16;
    public static final double MOST_LENGTH = 512.0;

    // What decides where it points: its own pattern, a fixed spot, one chosen creature, or the nearest creature,
    // player or foe.
    public enum Aim {
        PATTERN, SPOT, FOLLOW, MOB, PLAYER, HOSTILE
    }

    // How redstone works it: not at all, on only while powered, or off while powered.
    public enum Redstone {
        IGNORE, ON_WHEN_POWERED, OFF_WHEN_POWERED
    }

    public String name = "";
    public boolean on = true;
    public Redstone redstone = Redstone.IGNORE;
    public int light;
    public double brightness = 1.0;
    public double length;
    public double width;
    public double spread;
    public int beams;
    public double fan;
    public boolean stopAtBlocks = true;
    public int[] colors;
    public LampKind.ColorMode colorMode;
    public double colorSpeed;
    public double strobe;
    public double duty = 0.35;
    public LampKind.Pattern pattern;
    public double speed;
    public double panRange;
    public double tiltRange;
    public double pan;
    public double tilt;
    public double phase;
    public Aim aim = Aim.PATTERN;
    public Vec3 spot = Vec3.ZERO;
    @Nullable
    public UUID follow;
    public double aimRange = 32.0;
    public int channel;

    private LampSettings() {
    }

    public static LampSettings of(LampKind kind) {
        LampSettings settings = new LampSettings();
        settings.light = kind.light;
        settings.length = kind.length;
        settings.width = kind.width;
        settings.spread = kind.spread;
        settings.beams = kind.beams;
        settings.fan = kind.fan;
        settings.colors = kind.colors.clone();
        settings.colorMode = kind.colorMode;
        settings.colorSpeed = kind.colorSpeed;
        settings.strobe = kind.strobe;
        settings.pattern = kind.pattern;
        settings.speed = kind.speed;
        settings.panRange = kind.panRange;
        settings.tiltRange = kind.tiltRange;
        return settings;
    }

    public LampSettings copy() {
        LampSettings copy = new LampSettings();
        copy.read(this.write());
        return copy;
    }

    public CompoundTag write() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", this.name);
        tag.putBoolean("on", this.on);
        tag.putInt("redstone", this.redstone.ordinal());
        tag.putInt("light", this.light);
        tag.putDouble("brightness", this.brightness);
        tag.putDouble("length", this.length);
        tag.putDouble("width", this.width);
        tag.putDouble("spread", this.spread);
        tag.putInt("beams", this.beams);
        tag.putDouble("fan", this.fan);
        tag.putBoolean("stopAtBlocks", this.stopAtBlocks);
        tag.putIntArray("colors", this.colors);
        tag.putInt("colorMode", this.colorMode.ordinal());
        tag.putDouble("colorSpeed", this.colorSpeed);
        tag.putDouble("strobe", this.strobe);
        tag.putDouble("duty", this.duty);
        tag.putInt("pattern", this.pattern.ordinal());
        tag.putDouble("speed", this.speed);
        tag.putDouble("panRange", this.panRange);
        tag.putDouble("tiltRange", this.tiltRange);
        tag.putDouble("pan", this.pan);
        tag.putDouble("tilt", this.tilt);
        tag.putDouble("phase", this.phase);
        tag.putInt("aim", this.aim.ordinal());
        tag.putDouble("spotX", this.spot.x);
        tag.putDouble("spotY", this.spot.y);
        tag.putDouble("spotZ", this.spot.z);
        if (this.follow != null) {
            tag.putUUID("follow", this.follow);
        }
        tag.putDouble("aimRange", this.aimRange);
        tag.putInt("channel", this.channel);
        return tag;
    }

    // Whatever `tag` holds, kept within range; what it lacks stays as it was.
    public void read(CompoundTag tag) {
        if (tag.contains("name")) {
            String text = tag.getString("name");
            this.name = text.length() > 32 ? text.substring(0, 32) : text;
        }
        if (tag.contains("on")) {
            this.on = tag.getBoolean("on");
        }
        if (tag.contains("redstone")) {
            this.redstone = Redstone.values()[Mth.clamp(tag.getInt("redstone"), 0, Redstone.values().length - 1)];
        }
        this.light = whole(tag, "light", this.light, 0, 15);
        this.brightness = number(tag, "brightness", this.brightness, 0.0, 3.0);
        this.length = number(tag, "length", this.length, 1.0, MOST_LENGTH);
        this.width = number(tag, "width", this.width, 0.005, 4.0);
        this.spread = number(tag, "spread", this.spread, 0.0, 60.0);
        this.beams = whole(tag, "beams", this.beams, 1, 64);
        this.fan = number(tag, "fan", this.fan, 0.0, 180.0);
        if (tag.contains("stopAtBlocks")) {
            this.stopAtBlocks = tag.getBoolean("stopAtBlocks");
        }
        if (tag.contains("colors")) {
            int[] read = tag.getIntArray("colors");
            if (read.length > 0) {
                int[] kept = Arrays.copyOf(read, Math.min(read.length, MOST_COLORS));
                for (int i = 0; i < kept.length; i++) {
                    kept[i] &= 0xFFFFFF;
                }
                this.colors = kept;
            }
        }
        if (tag.contains("colorMode")) {
            this.colorMode = LampKind.ColorMode.values()[Mth.clamp(tag.getInt("colorMode"), 0,
                    LampKind.ColorMode.values().length - 1)];
        }
        this.colorSpeed = number(tag, "colorSpeed", this.colorSpeed, 0.0, 20.0);
        this.strobe = number(tag, "strobe", this.strobe, 0.0, 25.0);
        this.duty = number(tag, "duty", this.duty, 0.05, 0.95);
        if (tag.contains("pattern")) {
            this.pattern = LampKind.Pattern.values()[Mth.clamp(tag.getInt("pattern"), 0,
                    LampKind.Pattern.values().length - 1)];
        }
        this.speed = number(tag, "speed", this.speed, 0.0, 5.0);
        this.panRange = number(tag, "panRange", this.panRange, 0.0, 180.0);
        this.tiltRange = number(tag, "tiltRange", this.tiltRange, 0.0, 90.0);
        this.pan = number(tag, "pan", this.pan, -180.0, 180.0);
        this.tilt = number(tag, "tilt", this.tilt, -90.0, 90.0);
        this.phase = number(tag, "phase", this.phase, 0.0, 1.0);
        if (tag.contains("aim")) {
            this.aim = Aim.values()[Mth.clamp(tag.getInt("aim"), 0, Aim.values().length - 1)];
        }
        if (tag.contains("spotX")) {
            double x = tag.getDouble("spotX");
            double y = tag.getDouble("spotY");
            double z = tag.getDouble("spotZ");
            if (Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                this.spot = new Vec3(Mth.clamp(x, -3.0E7, 3.0E7), Mth.clamp(y, -2048.0, 4096.0),
                        Mth.clamp(z, -3.0E7, 3.0E7));
            }
        }
        if (tag.hasUUID("follow")) {
            this.follow = tag.getUUID("follow");
        }
        this.aimRange = number(tag, "aimRange", this.aimRange, 4.0, 128.0);
        this.channel = whole(tag, "channel", this.channel, 0, CHANNELS);
    }

    // The show part only: what a lamp passes to the others on its channel, so they move and flash as one.
    public CompoundTag show() {
        CompoundTag tag = this.write();
        for (String own : new String[] { "name", "redstone", "light", "pan", "tilt", "phase" }) {
            tag.remove(own);
        }
        return tag;
    }

    private static double number(CompoundTag tag, String key, double was, double least, double most) {
        if (!tag.contains(key)) {
            return was;
        }
        double value = tag.getDouble(key);
        return Double.isFinite(value) ? Mth.clamp(value, least, most) : was;
    }

    private static int whole(CompoundTag tag, String key, int was, int least, int most) {
        return tag.contains(key) ? Mth.clamp(tag.getInt(key), least, most) : was;
    }
}
