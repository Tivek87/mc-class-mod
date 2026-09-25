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

abstract class FlightSteering {
    public static final float GATHER = 7.0F;
    public static final float SWEEP = 13.0F;
    public static final float ARISE = Flight.ARISE_TICKS;

    private static final double HOVER = 0.32;
    private static final double CLIMB = 0.38;
    private static final double SPEED_UP = 0.20825;
    private static final double SLOW_DOWN = 0.055;
    private static final double BRAKE = 0.13;
    private static final double GAIN_FROM = 0.85;
    private static final double GAIN_FROM_CRUISE = 0.55;
    private static final double LOSE_SECONDS = 3.0;
    private static final double RISE = 0.46;
    static final double SINK = 0.18;
    static final double LAND_SPEED = 0.2;
    // How many ticks ahead you slow down before the edge of loaded chunks.
    static final int EDGE_LOOK = 30;

    static boolean steering;
    static Vec3 velocity = Vec3.ZERO;
    static Vec3 velocityO = Vec3.ZERO;
    // The speed right after you moved: a wall stops part of it, any later
    // change is a knock from outside, not your own steering.
    @Nullable
    static Vec3 afterMove;
    // Whether ground or ceiling stopped you; gravity added after won't show it.
    static boolean stoppedDown;
    static boolean stoppedUp;
    static boolean airborne;
    static boolean landing;
    static double momentum;
    @Nullable
    static WindSound wind;
    @Nullable
    static Vec3 landedWith;

    static final double SLAM_SPEED = 0.9;
    static final double SLAM_DOWN = 0.35;
    static final double DIVE_LAND = 0.15;
    static final float DIVE_LOOK = 20.0F;
    static final double BRACE_TICKS = 5.0;
    static final double DIVE_TURN = 0.4;
    static final double DIVE_SPEED = 9.625 / 20.0;
    static final int SLAM_ROOT = 17;
    static int slamTick = Integer.MIN_VALUE;

    private static final int DOUBLE_JUMP = 7;
    private static boolean jumpWasDown;
    static int lastJump = Integer.MIN_VALUE;

    FlightSteering() {
    }

    private static double topSpeed(LocalPlayer player) {
        double full = fullSpeed();
        double start = Math.min(full, flightSetting("startSpeed", 6.0) / 20.0);
        double cruise = Mth.clamp(flightSetting("cruiseSpeed", 7.5) / 20.0, start, full);
        double quick = cruiseSeconds();
        double slow = Math.max(0.0, flightSetting("speedUpSeconds", 3.0));
        double top;
        if (momentum < quick) {
            double u = momentum / quick;
            top = Mth.lerp(1.0 - (1.0 - u) * (1.0 - u), start, cruise);
        } else {
            top = slow <= 0.0 ? full : Mth.lerp(Math.min(1.0, (momentum - quick) / slow), cruise, full);
        }
        return ClientRing.has(player, RingPayload.DOME) ? top * 0.5 : top;
    }

    static void gainSpeed(LocalPlayer player, boolean forward) {
        double total = cruiseSeconds() + Math.max(0.0, flightSetting("speedUpSeconds", 3.0));
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

    private static double cruiseSeconds() {
        return Math.max(0.0, flightSetting("cruiseSeconds", 0.5));
    }

    public static double fullSpeed() {
        return flightSetting("topSpeed", 9.0) / 20.0;
    }

    private static double flightSetting(String key, double fallback) {
        CharacterAbility flight = GameCharacter.GREEN_LANTERN.byName("flight");
        return flight == null ? fallback : flight.value(key);
    }

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

    static void stop() {
        steering = false;
        afterMove = null;
        landing = false;
        momentum = 0.0;
    }

    static void doubleJump(Minecraft minecraft, LocalPlayer player) {
        boolean down = minecraft.options.keyJump.isDown();
        if (down && !jumpWasDown && minecraft.screen == null) {
            boolean lantern = ClientCharacter.active() == GameCharacter.GREEN_LANTERN && !player.mayFly()
                    && !player.isPassenger() && !player.isFallFlying() && !player.isSpectator();
            float t = ClientRing.flight(player, 0.0F);
            boolean free = lantern && t < 0.0F && ClientRing.arrival(player, 0.0F) < 0.0F;
            boolean flying = lantern && t >= ARISE && !ClientRing.has(player, RingPayload.DESCENT);
            // No earlier tap is no double tap; MIN_VALUE would make the gap below overflow instead.
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
