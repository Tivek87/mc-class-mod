package nl.tivek.multiversepowers.sky.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.space.Space;
import nl.tivek.multiversepowers.space.client.SpaceView;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector3f;

// Where the eye is on the way between the world and the moon (Space), and what that makes of the two in the sky. Up
// from the ground the moon swings from its place in the sky to straight overhead, comes nearer and turns one flat
// face to the eye, while the Earth shows below as a cube, first hidden in the haze of its own air. On the moon it is
// the other way round: the moon is the ground's own far picture below, the Earth hangs overhead. `haze` is how much
// of what lies below is still only the colour the far land fades to; `orbit` turns the whole sky, in its own frame,
// as the eye goes round the world below (SpaceView.orbit).
record SkyWay(boolean onMoon, Quaternionfc orbit, double thin, double swing, double square, double earthShown,
        double earthTilt, double haze, double rim, double black, double moonFace, double earthFace, double moonTurn) {
    private static final double SWING_FROM = 300.0;
    private static final double SWING_TO = 2200.0;
    private static final double SQUARE_FROM = 1500.0;
    private static final double SQUARE_TO = 3300.0;
    private static final double EARTH_FROM = 120.0;
    private static final double TILT_FROM = 4500.0;
    private static final double TILT_TO = 6200.0;
    // Where the Earth's rim comes out of the haze its middle left long before.
    private static final double RIM_FROM = 900.0;
    private static final double RIM_TO = 2400.0;
    // Where the game's own sky, by then black, is covered over: its stars are not the far sky's.
    private static final double BLACK_FROM = 1700.0;
    private static final double BLACK_TO = 2600.0;
    // How far under its ground each one's far picture lies while the eye is near it, so no land sinks below it.
    private static final double SUNK = 48.0;
    private static final double SUNK_OVER = 900.0;
    static final SkyWay GROUND = new SkyWay(false, new Quaternionf(), 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 1.0, 0.0,
            Space.DISTANCE, 0.0, 0.0);

    static SkyWay now(ClientLevel level, float partialTick) {
        if (!Space.reaches(level)) {
            return GROUND;
        }
        Minecraft minecraft = Minecraft.getInstance();
        boolean onMoon = Space.onMoon(level);
        double height = Space.height(level, minecraft.gameRenderer.getMainCamera().getPosition().y);
        double up = onMoon ? Space.DISTANCE - height : height;
        double sunk = SUNK * (1.0 - Ease.smooth(height / SUNK_OVER));
        // Straight up from the world's ground (down to the moon's), as the sky's own frame has it, and the turn about
        // its x axis that takes the moon there from where it stands in the sky.
        double hour = CosmicSky.hour(level, partialTick) * Math.PI * 2.0;
        Vec3 over = new Vec3(0.0, Math.cos(hour), -Math.sin(hour));
        // The eye gone round the world below, as a turn of the sky's own frame the other way.
        Vec3 round = SpaceView.orbit(level);
        Quaternionf orbit = new Quaternionf();
        if (round.lengthSqr() > 1.0E-12) {
            Vector3f axis = CosmicSky.turn(level, partialTick).conjugate().transform(round.toVector3f());
            float angle = axis.length();
            orbit.rotationAxis(-angle, axis.div(angle));
        }
        return new SkyWay(onMoon, orbit, Space.thin(level, height),
                onMoon ? 1.0 : Ease.smooth((up - SWING_FROM) / (SWING_TO - SWING_FROM)),
                onMoon ? 1.0 : Ease.smooth((up - SQUARE_FROM) / (SQUARE_TO - SQUARE_FROM)),
                onMoon ? 1.0 : Ease.smooth((up - EARTH_FROM) / 60.0),
                Ease.smooth((up - TILT_FROM) / (TILT_TO - TILT_FROM)),
                SpaceView.haze(level, height), onMoon ? 0.0 : 1.0 - Ease.smooth((up - RIM_FROM) / (RIM_TO - RIM_FROM)),
                onMoon ? 1.0 : Ease.smooth((up - BLACK_FROM) / (BLACK_TO - BLACK_FROM)),
                Space.DISTANCE - up + (onMoon ? sunk : 0.0), up + (onMoon ? 0.0 : sunk),
                Math.atan2(-over.z, -over.y));
    }
}
