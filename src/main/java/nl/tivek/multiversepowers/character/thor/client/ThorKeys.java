package nl.tivek.multiversepowers.character.thor.client;

import nl.tivek.multiversepowers.engine.math.Keyframes;
import nl.tivek.multiversepowers.engine.math.Keyframes.Key;

// Thor's scripted moves as keyframes, after the reference clip (docs/reference/thor-dash.mp4): he sinks deep into his
// knees, bursts off low with his chest thrown into the run and his arms driving back, skims the ground, then plants
// his lead foot, slides to a stop leaning back against it and rises. Ticks, pixels and radians.
final class ThorKeys {
    // Dash channels.
    static final int DROP = 0;
    static final int PITCH = 1;
    static final int WAIST = 2;
    static final int ROLL = 3;
    static final int LEAD = 4;
    static final int LEAD_LIFT = 5;
    static final int TRAIL = 6;
    static final int TRAIL_LIFT = 7;
    static final int ARMS = 8;
    static final int WEIGHT = 9;

    static final Key[] DASH = {
            key(0.0F, false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
            key(1.2F, false, 4.8F, 0.3F, 0.15F, 0.15F, 1.0F, 0, 1.5F, 0, 0.6F, 1),
            key(3.0F, false, 3.2F, 0.55F, 0.25F, 0.48F, 4.5F, 1.2F, 5.5F, 2.5F, 1, 1),
            key(7.0F, false, 2.8F, 0.45F, 0.2F, 0.4F, 4.0F, 0.3F, 5.0F, 1.2F, 1, 1),
            key(9.5F, false, 4.5F, -0.15F, -0.1F, -0.15F, 5.0F, 0, 2.5F, 0, 0.7F, 1),
            key(12.0F, false, 1.0F, 0.05F, 0, 0, 1.5F, 0, 1.0F, 0, 0.2F, 0.6F),
            key(15.0F, true, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) };

    // Super jump channels: sinking into it, the drive of the legs, the arms thrown up, the knees tucked in the rise.
    static final int J_DROP = 0;
    static final int J_PITCH = 1;
    static final int J_ARMS_UP = 2;
    static final int J_ARMS_BACK = 3;
    static final int J_TUCK = 4;
    static final int J_WEIGHT = 5;

    // Ticks from the launch: negative while he sinks into it.
    static final Key[] JUMP = {
            key(-3.0F, false, 0, 0, 0, 0, 0, 0),
            key(-1.0F, false, 5.5F, 0.4F, 0, 1, 0, 1),
            key(0.0F, false, 4.0F, 0.3F, 0.2F, 0.6F, 0, 1),
            key(2.0F, false, 0, -0.1F, 1, 0, 0, 1),
            key(6.0F, false, 0, 0.05F, 0.8F, 0, 0.8F, 1),
            key(12.0F, true, 0, 0.1F, 0.5F, 0, 0.5F, 1) };

    // Slam channels: the drop to one knee, the lean over it, the fist on the ground.
    static final int S_DROP = 0;
    static final int S_PITCH = 1;
    static final int S_FIST = 2;
    static final int S_WEIGHT = 3;

    static final Key[] SLAM = {
            key(0.0F, false, 3.0F, 0.2F, 0.3F, 1),
            key(1.5F, false, 8.5F, 0.55F, 1, 1),
            key(8.0F, false, 8.0F, 0.5F, 1, 1),
            key(13.0F, false, 2.0F, 0.1F, 0.2F, 0.6F),
            key(17.0F, true, 0, 0, 0, 0) };

    private ThorKeys() {
    }

    private static Key key(float tick, boolean stop, float... values) {
        return new Key(tick, stop, values);
    }

    static float[] at(Key[] keys, float t) {
        return Keyframes.at(keys, t);
    }

    static float end(Key[] keys) {
        return Keyframes.end(keys);
    }
}
