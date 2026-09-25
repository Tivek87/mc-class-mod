package nl.tivek.multiversepowers.character.greenlantern.client;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.AbilityActionPayload;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flight;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * Your own flight on this side (see {@link ClientFlight}, which builds on this): the take-off, where you are in your
 * steering, how your keys make your speed and how that speed grows the longer you fly on, a slam into the ground,
 * taking off with a double tap of jump, and the wind you hear.
 */
abstract class FlightSteering {
    /** The take-off, in ticks: until here the fists come to the chest, then the arms sweep down and you rise. */
    public static final float GATHER = 7.0F;
    /** Until here the arms go down along the sides; the rise goes on to the end of the take-off. */
    public static final float SWEEP = 13.0F;
    /** The end of the take-off. */
    public static final float ARISE = Flight.ARISE_TICKS;

    // Your own steering, in blocks per tick: how fast you slide and rise while hovering, how quickly you pick
    // up speed and lose it again, and how hard the brake bites.
    private static final double HOVER = 0.32;
    private static final double CLIMB = 0.38;
    private static final double SPEED_UP = 0.119;
    private static final double SLOW_DOWN = 0.055;
    private static final double BRAKE = 0.13;
    // You pick up speed only while you fly at least this much of the top speed you have by now (less while you are
    // still getting up to the cruising speed, which goes quickly); letting go of forward loses all of it in this many
    // seconds.
    private static final double GAIN_FROM = 0.85;
    private static final double GAIN_FROM_CRUISE = 0.55;
    private static final double LOSE_SECONDS = 3.0;
    // How fast you rise at the height of the take-off, and how fast an empty ring lets you sink.
    private static final double RISE = 0.46;
    static final double SINK = 0.18;
    // You land by yourself when you touch ground slower than this.
    static final double LAND_SPEED = 0.2;
    // You slow down before the edge of the chunks your own game has, as far ahead as you fly in this many ticks.
    static final int EDGE_LOOK = 30;

    // ---- Your own flight ----
    static boolean steering;
    static Vec3 velocity = Vec3.ZERO;
    // Your speed one tick earlier, so what hangs on it can glide between two ticks instead of stepping.
    static Vec3 velocityO = Vec3.ZERO;
    // What the game made of the speed you gave it, right after you moved: a wall stops one part of it, and any
    // change after that and before the next tick is a knock from outside.
    @Nullable
    static Vec3 afterMove;
    // Whether the ground (or a ceiling) stopped you as you moved. The game adds its gravity after that, so the speed
    // it leaves you with never shows it by itself.
    static boolean stoppedDown;
    static boolean stoppedUp;
    static boolean airborne;
    static boolean landing;
    // How long you have flown on at speed, in seconds: up to the cruising speed and on to the top speed (see topSpeed).
    static double momentum;
    @Nullable
    static WindSound wind;
    // The speed you had when the ground stopped you last tick, or null when it did not.
    @Nullable
    static Vec3 landedWith;

    // ---- Your own landing slam ----
    // How much of your top speed you must fly into the ground with, and how much of that must go down, for a slam.
    static final double SLAM_SPEED = 0.9;
    static final double SLAM_DOWN = 0.35;
    // Flying into the ground slower than a slam but still going down this fast, in blocks per tick, and looking at
    // least this far down, in degrees: you land.
    static final double DIVE_LAND = 0.15;
    static final float DIVE_LOOK = 20.0F;
    // How many ticks before a slam a diving flyer starts to swing upright for it, fist cocked.
    static final double BRACE_TICKS = 5.0;
    // On a dive for a slam (the shockwave key): how much of the way to straight down at dive speed you swing each tick,
    // and that speed in blocks per tick (the top speed instead, when that is set higher).
    static final double DIVE_TURN = 0.4;
    static final double DIVE_SPEED = 19.25 / 20.0;
    /**
     * Ticks you stay down after a slam, crouched on your fist: you cannot move meanwhile. Counted at the pace the
     * constructs were made for, like {@link ClientFlight#slam}; only Green Lantern smashes his fist into the ground.
     */
    static final int SLAM_ROOT = 17;
    // The tick (of your own player) you last slammed into the ground, or MIN_VALUE.
    static int slamTick = Integer.MIN_VALUE;

    // ---- Taking off with a double jump ----
    // Two taps of jump at most this many ticks apart take off, as the flight key does.
    private static final int DOUBLE_JUMP = 7;
    private static boolean jumpWasDown;
    static int lastJump = Integer.MIN_VALUE;

    FlightSteering() {
    }

    /**
     * Your own top speed right now, in blocks per tick: the speed you set off at, rising quickly to the cruising speed of
     * the settings in the first seconds you fly on ({@code cruiseSeconds}), easing into it, and from there slowly on to
     * the top speed over {@code speedUpSeconds} more (see {@link #gainSpeed}); halved while the dome brakes you.
     */
    private static double topSpeed(LocalPlayer player) {
        double full = fullSpeed();
        double start = Math.min(full, flightSetting("startSpeed", 6.4) / 20.0);
        double cruise = Mth.clamp(flightSetting("cruiseSpeed", 8.0) / 20.0, start, full);
        double quick = cruiseSeconds();
        double slow = Math.max(0.0, flightSetting("speedUpSeconds", 5.6));
        double top;
        if (momentum < quick) {
            double u = momentum / quick;
            top = Mth.lerp(1.0 - (1.0 - u) * (1.0 - u), start, cruise);
        } else {
            top = slow <= 0.0 ? full : Mth.lerp(Math.min(1.0, (momentum - quick) / slow), cruise, full);
        }
        return ClientRing.has(player, RingPayload.DOME) ? top * 0.5 : top;
    }

    /**
     * The longer you fly on, the faster you go: while you fly forward about as fast as you can go by now, the seconds
     * count up, first to the cruising speed and then on to the top speed, and never further. Letting go of forward
     * loses it all again in a few seconds; pushing against a wall keeps what you have.
     */
    static void gainSpeed(LocalPlayer player, boolean forward) {
        double total = cruiseSeconds() + Math.max(0.0, flightSetting("speedUpSeconds", 5.6));
        if (!forward) {
            momentum = Math.max(0.0, momentum - total / (LOSE_SECONDS * 20.0));
            return;
        }
        double from = momentum < cruiseSeconds() ? GAIN_FROM_CRUISE : GAIN_FROM;
        if (velocity.length() < topSpeed(player) * from) {
            return;
        }
        momentum = Math.min(total, momentum + 1.0 / 20.0);
    }

    /** How long flying on takes to get from the speed you set off at to the cruising speed, in seconds. */
    private static double cruiseSeconds() {
        return Math.max(0.0, flightSetting("cruiseSeconds", 0.5));
    }

    /** The top speed of a flight from the settings, in blocks per tick. */
    public static double fullSpeed() {
        return flightSetting("topSpeed", 9.625) / 20.0;
    }

    /** One of the flight's settings, or {@code fallback} while Green Lantern has no flight. */
    private static double flightSetting(String key, double fallback) {
        CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
        return flight == null ? fallback : flight.value(key);
    }

    /**
     * Since the last tick: a wall or the ground that stopped part of your speed takes that part away, and a knock
     * from outside (a hit, a blast) is added to it, as it would be for anyone.
     */
    static void absorb(LocalPlayer player) {
        Vec3 before = afterMove;
        afterMove = null;
        if (before == null) {
            return;
        }
        boolean down = stoppedDown && velocity.y < -1.0E-3;
        boolean up = stoppedUp && velocity.y > 1.0E-3;
        if (down) {
            landedWith = velocity;
        }
        Vec3 now = player.getDeltaMovement();
        Vec3 knock = now.subtract(before);
        double x = before.x == 0.0 && Math.abs(velocity.x) > 1.0E-3 ? 0.0 : velocity.x;
        double y = down || up ? 0.0 : velocity.y;
        double z = before.z == 0.0 && Math.abs(velocity.z) > 1.0E-3 ? 0.0 : velocity.z;
        velocity = new Vec3(x, y, z);
        if (knock.lengthSqr() > 1.0E-4) {
            velocity = velocity.add(knock);
        }
    }

    /**
     * The take-off: first you stop where you are (even in the middle of a fall) while your fists come to your
     * chest; then you shoot up and ease off at the top, and your own steering fades in towards the end, so the
     * rise flows straight into the flight.
     */
    static Vec3 arise(LocalPlayer player, float t, float forward, float strafe, boolean up, boolean down) {
        if (t < GATHER) {
            return velocity.scale(0.55);
        }
        double x = (t - GATHER) / (ARISE - GATHER);
        double lift = RISE * (x < 0.15 ? x / 0.15 : Math.pow(1.0 - (x - 0.15) / 0.85, 1.2));
        double control = Ease.smooth((t - SWEEP) / (ARISE - SWEEP));
        Vec3 steered = control > 0.0 ? steer(player, forward, strafe, up, down, control) : Vec3.ZERO;
        return new Vec3(steered.x * control, Math.max(lift, steered.y * control), steered.z * control);
    }

    /**
     * Flying: forward picks up speed the way you look; without it you hover, sliding and rising with the other
     * keys. Your speed swings round towards where you want to go instead of jumping there.
     *
     * @param grip how much of the steering you have yet, 0 to 1 (it fades in at the end of the take-off)
     */
    static Vec3 steer(LocalPlayer player, float forward, float strafe, boolean up, boolean down,
            double grip) {
        double top = topSpeed(player);
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        flat = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
        double vertical = (up ? 1.0 : 0.0) - (down ? 1.0 : 0.0);
        Vec3 target;
        if (forward > 0.01F) {
            target = look.scale(top * forward).add(right.scale(-strafe * top * 0.25))
                    .add(0.0, vertical * top * 0.25, 0.0);
        } else {
            target = right.scale(-strafe * HOVER).add(flat.scale(forward * HOVER * 0.8))
                    .add(0.0, vertical * CLIMB, 0.0);
        }
        double rate = target.lengthSqr() > velocity.lengthSqr() ? SPEED_UP : SLOW_DOWN;
        if (ClientRing.has(player, RingPayload.DOME) && velocity.length() > top) {
            rate = BRAKE;
        }
        return velocity.lerp(target, rate * grip);
    }

    /**
     * You slam into the ground: you stop dead on your fist, and the server hears of it (it lands you and has the
     * ring throw up a construct, see LandingSlam).
     */
    static void slamDown(LocalPlayer player) {
        landing = true;
        slamTick = player.tickCount;
        velocity = Vec3.ZERO;
        player.setDeltaMovement(Vec3.ZERO);
        CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
        if (flight != null) {
            PacketDistributor.sendToServer(new AbilityActionPayload(flight.slot().ordinal(), true, Characters.SLAM));
        }
    }

    /** Your flight is over (you landed, turned it off, or are no longer Green Lantern): the game has you again. */
    static void stop() {
        steering = false;
        afterMove = null;
        landing = false;
        momentum = 0.0;
    }

    /**
     * Tapping jump twice works the flight key: on the ground (the first tap jumps) or in the middle of a jump or a fall
     * it takes off, and in the air on the ring, once the take-off is over, it turns the flight off and you fall. Not
     * while the game lets you fly by itself (creative), where the double tap is the game's own.
     */
    static void doubleJump(Minecraft minecraft, LocalPlayer player) {
        boolean down = minecraft.options.keyJump.isDown();
        if (down && !jumpWasDown && minecraft.screen == null) {
            boolean lantern = ClientCharacter.active() == GameCharacter.GREEN_LANTERN && !player.mayFly()
                    && !player.isPassenger() && !player.isFallFlying() && !player.isSpectator();
            float t = ClientRing.flight(player, 0.0F);
            boolean free = lantern && t < 0.0F && ClientRing.arrival(player, 0.0F) < 0.0F;
            // An empty ring letting you down cannot be turned off (the flight key cannot either).
            boolean flying = lantern && t >= ARISE && !ClientRing.has(player, RingPayload.DESCENT);
            // No earlier tap is no double tap (and the gap is only counted once there is one: the sum would run over).
            int gap = lastJump == Integer.MIN_VALUE ? Integer.MAX_VALUE : player.tickCount - lastJump;
            if ((free || flying) && gap >= 0 && gap <= DOUBLE_JUMP) {
                lastJump = Integer.MIN_VALUE;
                CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
                if (flight != null) {
                    PacketDistributor.sendToServer(new AbilityActionPayload(flight.slot().ordinal(), true, 0));
                }
            } else {
                lastJump = player.tickCount;
            }
        }
        jumpWasDown = down;
    }

    /** The wind of your own flight: louder and higher the faster you go, gone once you land. */
    static final class WindSound extends AbstractTickableSoundInstance {
        private final LocalPlayer player;

        WindSound(LocalPlayer player) {
            super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.player = player;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.0F;
        }

        @Override
        public void tick() {
            if (this.player.isRemoved() || !steering) {
                this.stop();
                return;
            }
            this.x = this.player.getX();
            this.y = this.player.getY();
            this.z = this.player.getZ();
            double speed = velocity.length() / fullSpeed();
            this.volume = (float) Mth.clamp((speed - 0.17) * 0.8, 0.0, 0.85);
            this.pitch = 0.9F + (float) Math.min(0.5, speed * 0.32);
        }
    }
}
