package nl.tivek.multiversepowers.character.docock;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

/**
 * The state of one Portal run (see {@link PortalRun}): its timing and sizes, its phase, the three
 * portals it opens and the far part of the tentacle.
 */
abstract class PortalState {
    // Everything is deliberately unhurried, so you can follow the whole trick with your eyes.
    static final int SEARCH_TIME = 34;
    static final int SPIKE_TIME = 16;
    static final int THRUST_TIME = 18;
    private static final int PORTAL_OPEN = 52;
    private static final int SKY_OPEN = 44;
    private static final int CLOSE_TIME = 34;
    static final int GRIP_TIME = 9;
    static final int MAX_HUNT = 120;
    static final int MAX_DRAG = 120;
    static final int SLAM_WAIT = 10;

    static final double DIVE_SPEED = 0.3;
    static final double DIVE_ACCEL = 0.2;
    static final double HUNT_SPEED = 1.15;
    static final double STEERING = 0.3;
    static final double DRAG_SPEED = 0.95;
    static final double SLAM_SPEED = 1.2;
    static final double SLAM_ACCEL = 0.25;
    // Never so fast that a watching client has to jump the creature instead of sliding it down.
    static final double SLAM_MAX = 2.4;
    static final double OUT_SPEED = 0.55;
    static final double CANCEL_OUT_SPEED = 1.2;

    static final double MAX_TRAIL = 60.0;
    private static final double PORTAL_A_DISTANCE = 3.2;
    private static final double RADIUS_A = 1.5;
    static final double RADIUS_B = 1.8;
    static final double RADIUS_C = 2.3;
    static final double SKY_HEIGHT = 26.0;
    static final double MIN_SKY = 7.0;
    static final double SKY_RADIUS = 10.0;
    // Nothing of this ability may ever open further than this from the player who started it.
    static final double MAX_FROM_CASTER = 28.0;
    // How far a tentacle starts behind the portal it comes out of, so no gap shows at the ring.
    static final double PORTAL_DEPTH = 0.3;
    static final double CLAW_OPEN = 0.7;

    static final int CRACK = 0x1E2226;
    static final int DARK = 0x3A3F46;
    static final int HEAT = 0xFF8A3A;
    static final Vec3 DOWN = new Vec3(0, -1, 0);

    /**
     * What the tentacle is doing right now. It looks around first, then the sharp point slides out
     * between its claws, then the thrusters fold out and light up; only then does it dive.
     */
    enum Phase {
        SEARCH, SPIKE, THRUST, DIVE, HUNT, GRIP, DRAG, SLAM, RETRACT_OUT, RETRACT_BACK, CLOSE
    }

    final ServerPlayer caster;
    final LivingEntity target;
    final Vec3 look;
    Vec3 portalA;
    final Gate gateA = new Gate(RADIUS_A, PORTAL_OPEN);
    final Gate gateB = new Gate(RADIUS_B, PORTAL_OPEN);
    final Gate gateC = new Gate(RADIUS_C, SKY_OPEN);
    // The far part of the tentacle: out of portal B or C, with its own id. The near part is the arm on
    // the player's back, which the rig draws.
    final int farArm = RobotArm.newId();
    final List<Vec3> trail = new ArrayList<>();
    boolean farShown;

    Phase phase = Phase.SEARCH;
    int phaseAge = -1;
    // How far the sharp point and the thrusters are out (0 .. 1), for the client to draw.
    double spike;
    double thrust;
    boolean dived;
    double diveTravel;
    double diveLength = 1.0;
    Vec3 diveFrom = Vec3.ZERO;
    @Nullable
    Gate exit;
    double shown;
    Vec3 velocity = new Vec3(0, 1, 0);
    double retractSpeed = OUT_SPEED;
    double groundC;
    double clawOpen = CLAW_OPEN;
    boolean held;
    boolean clamped;
    int cut;

    PortalState(ServerPlayer caster, LivingEntity target) {
        this.caster = caster;
        this.target = target;
        this.look = caster.getLookAngle();
        this.portalA = spotInFront(caster, this.look);
    }

    /** Right in front of the player, or closer when a wall is in the way. */
    static Vec3 spotInFront(ServerPlayer caster, Vec3 look) {
        Vec3 eye = caster.getEyePosition();
        BlockHitResult wall = caster.level().clip(new ClipContext(eye, eye.add(look.scale(PORTAL_A_DISTANCE)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        double distance = wall.getType() == HitResult.Type.MISS ? PORTAL_A_DISTANCE
                : Math.max(1.6, wall.getLocation().distanceTo(eye) - 0.6);
        return eye.add(look.scale(distance)).add(0, -0.2, 0);
    }

    // ---- Helpers ----

    static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** One tech portal: opens slowly, and shrinks shut backwards when told to. */
    static final class Gate {
        private final int id = RobotArm.newId();
        private final double size;
        private final int openTime;
        Vec3 center = Vec3.ZERO;
        Vec3 normal = new Vec3(0, 1, 0);
        private int age = -1;
        private int closing = -1;
        private boolean gone;

        private Gate(double size, int openTime) {
            this.size = size;
            this.openTime = openTime;
        }

        void open(ServerLevel level, Vec3 center, Vec3 normal) {
            this.center = center;
            this.normal = normal.normalize();
            this.age = 0;
            ParticleFx.sphereOut(level, ParticleTypes.ELECTRIC_SPARK, center, 24, 0.35);
            sound(level, center, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
            sound(level, center, SoundEvents.PISTON_EXTEND, 0.8F, 0.6F);
        }

        boolean isOpen() {
            return this.age >= 0 && !this.gone;
        }

        /** Open far enough to go through: the energy field is (almost) all there. */
        boolean ready() {
            return this.isOpen() && this.closing < 0 && this.age >= this.openTime * 0.9;
        }

        void close() {
            if (this.isOpen() && this.closing < 0) {
                this.closing = 0;
            }
        }

        void tick(ServerLevel level) {
            if (!this.isOpen()) {
                return;
            }
            double open;
            if (this.closing >= 0) {
                if (this.closing == 0) {
                    sound(level, this.center, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.6F);
                    sound(level, this.center, SoundEvents.PISTON_CONTRACT, 0.7F, 0.6F);
                }
                this.closing++;
                if (this.closing >= CLOSE_TIME) {
                    ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.center, 14, 0.3, 0.1);
                    sound(level, this.center, SoundEvents.IRON_DOOR_CLOSE, 0.7F, 0.6F);
                    this.remove(level);
                    return;
                }
                open = Math.min(1.0, (double) this.age / this.openTime) * (1.0 - (double) this.closing / CLOSE_TIME);
            } else {
                open = Math.min(1.0, (this.age + 1.0) / this.openTime);
                this.openSounds(level);
            }
            this.age++;
            RobotArm.portal(level, this.id, this.center, this.normal, this.size, open);
        }

        /** Clanks while the ring assembles, a ticking lamp at a time, and a surge when the energy opens. */
        private void openSounds(ServerLevel level) {
            if (this.age > this.openTime) {
                return;
            }
            double p = (double) this.age / this.openTime;
            if (p < 0.45 && this.age % 4 == 0) {
                sound(level, this.center, SoundEvents.CHAIN_PLACE, 0.6F, 0.7F + (float) p);
            } else if (p >= 0.45 && p < 0.7 && this.age % 2 == 0) {
                sound(level, this.center, SoundEvents.STONE_BUTTON_CLICK_ON, 0.5F, 1.2F + (float) p);
            }
            if (this.age == (int) (this.openTime * 0.55)) {
                sound(level, this.center, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.4F);
                sound(level, this.center, SoundEvents.RESPAWN_ANCHOR_CHARGE, 0.8F, 1.2F);
            }
        }

        void remove(ServerLevel level) {
            if (this.isOpen()) {
                this.gone = true;
                RobotArm.removePortal(level, this.id);
            }
        }
    }
}
