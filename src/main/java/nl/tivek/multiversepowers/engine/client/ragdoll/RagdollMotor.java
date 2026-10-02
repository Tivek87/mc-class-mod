package nl.tivek.multiversepowers.engine.client.ragdoll;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.ragdoll.getup.GetUp;
import nl.tivek.multiversepowers.engine.physics.Quat;
import nl.tivek.multiversepowers.engine.physics.joint.Pin;

// A limp body's muscles and hands at work: how hard it holds itself each tick as it dies, braces in the air or hangs
// (RagdollMuscles), and its hands' holds on an edge it caught on the way down.
abstract class RagdollMotor extends RagdollBody {
    // Hanging this many ticks past how long the server said, with no word it let go, it lets go all the same.
    private static final int LATE = 10;
    // Its hands hold the edge this far out past its chest's sides, onto the edge and above its top (blocks).
    private static final double GRIP_WIDE = 0.04;
    private static final double GRIP_ON = 0.08;
    private static final double GRIP_UP = 0.04;
    // Drawn out past an edge it lay on, its chest clears the wall by this (blocks).
    private static final double CLEAR = 0.05;
    // Moving this fast along the ground (blocks a second) it falls the way it moves; slower, the way it leans.
    private static final double MOVING = 0.5;
    private static final double LEANING = 0.3;
    // A creature knocked limp along the ground goes down after this many ticks; the longest it may fly limp.
    private static final int SHRUG = 3;
    private static final int LONGEST_FLIGHT = 200;
    // Flying along the ground faster than this (blocks a tick), the way it flies is kept: against a wall it slumps.
    private static final double THROWN_WAY = 0.05;
    private static final double GO_LIMP = 0.25;

    final RagdollMuscles muscles = new RagdollMuscles();
    // Its hands' holds on an edge: right, then left (null without such a hand); ticks into the hang (-1 when it does
    // not hang) and how long the hang lasts.
    final Pin[] grips = new Pin[2];
    int hangAge = -1;
    private int hangTicks;
    private final double[] pose = new double[7];
    private final double[] speed = new double[6];
    private final double[] front = new double[3];

    RagdollMotor(LivingEntity entity, EntityModel<?> model, List<ModelParts.Part> parts, ModelBends.Bend[][] chains,
            int[] hang, float[][] blades, int core, State state, @Nullable GetUp.Kind rise) {
        super(entity, model, parts, chains, hang, blades, core, state, rise);
    }

    // How hard its muscles hold this tick (`now`: Ragdolls' tick count).
    void tone(int now) {
        if (!this.muscles.any()) {
            return;
        }
        if (this.hangAge >= 0) {
            if (++this.hangAge <= this.hangTicks + LATE) {
                this.muscles.hang(this.hangAge);
                return;
            }
            this.letGo(this.entity);
        }
        if (this.state == State.DEAD) {
            this.muscles.dying(now - this.dead);
        } else if (this.state == State.FLYING && this.phase == Phase.AIR && this.kind.person() && this.age > 0) {
            this.muscles.brace(this.forward(), 1.0);
        } else {
            this.muscles.slacken();
        }
    }

    // Whether it falls on its face: its chest's front leads the way it moves, or it leans forward.
    private boolean forward() {
        int chest = this.body[this.core];
        this.world.pose(chest, this.pose);
        this.world.velocity(chest, this.speed);
        Quat.rotate(this.pose, 3, 0.0, 0.0, -1.0, this.front, 0);
        double along = this.front[0] * this.speed[0] + this.front[2] * this.speed[2];
        double moving = Math.sqrt(this.speed[0] * this.speed[0] + this.speed[2] * this.speed[2]);
        return moving > MOVING ? along > 0.0 : this.front[1] < -LEANING;
    }

    // Its hands catch hold of the edge at `edge` (the middle between them, on its top) on a wall facing out (nx, nz),
    // for `ticks`: the body hangs from them instead of being carried along with its creature. A branch is caught with
    // one hand.
    void grab(Vec3 edge, double nx, double nz, int ticks, boolean branch) {
        if (this.grips[0] == null && this.grips[1] == null) {
            return;
        }
        if (this.phase != Phase.AIR) {
            // It had gone down on the edge as it toppled off: it hangs in the air again, and once it lets go it is
            // carried down with its creature.
            this.lift(this.entity);
        }
        this.hold.release();
        // Lying on top of the edge, its chest is drawn out past the wall, away from it: it slips over and hangs.
        int chest = this.body[this.core];
        this.world.pose(chest, this.pose);
        double hx = this.world.half(chest, 0);
        double hy = this.world.half(chest, 1);
        double hz = this.world.half(chest, 2);
        double out = (this.pose[0] - edge.x) * nx + (this.pose[2] - edge.z) * nz;
        double clear = Math.sqrt(hx * hx + hy * hy + hz * hz) + CLEAR;
        if (out < clear) {
            this.world.shift(nx * (clear - out), 0.0, nz * (clear - out));
        }
        double half = hx + GRIP_WIDE;
        // Facing the wall, its right hand is to the right of the edge's middle.
        double rx = nz;
        double rz = -nx;
        for (int k = 0; k < 2; k++) {
            Pin grip = this.grips[k];
            if (grip == null) {
                continue;
            }
            if (branch && k == 1 && this.grips[0] != null) {
                grip.release();
                continue;
            }
            double side = branch ? 0.0 : k == 0 ? half : -half;
            grip.to(edge.x + rx * side - nx * GRIP_ON, edge.y + GRIP_UP, edge.z + rz * side - nz * GRIP_ON);
        }
        this.hangAge = 0;
        this.hangTicks = ticks;
        this.world.wake();
    }

    // It lets go of the edge: carried along with its creature again, from where it hangs.
    void letGo(LivingEntity entity) {
        if (this.hangAge < 0) {
            return;
        }
        this.release();
        System.arraycopy(this.now, this.core * 7, this.target, 0, 3);
        this.offset[0] = this.target[0] - entity.getX();
        this.offset[1] = this.target[1] - entity.getY();
        this.offset[2] = this.target[2] - entity.getZ();
        this.hold.to(this.target[0], this.target[1], this.target[2]);
        this.world.wake();
    }

    private void release() {
        for (Pin grip : this.grips) {
            if (grip != null) {
                grip.release();
            }
        }
        this.hangAge = -1;
    }

    // A living limp creature hangs from what holds it (`held`); one thrown flies along with its creature until it comes
    // down, then lies where it fell (never landing on its feet), kept where its creature really is, at least 3 seconds
    // on the ground, and gets up in time to stand before the server lets its creature move again. `again`: thrown
    // again; `downed`: knocked down again as it gets up. False once it stands again.
    boolean carry(LivingEntity entity, boolean held, boolean again, boolean downed) {
        if (held) {
            if (this.phase != Phase.AIR) {
                this.lift(entity);
            }
            this.state = State.HELD;
            this.follow(entity, false);
            this.limp = Math.min(1.0, this.limp + GO_LIMP);
            return true;
        }
        this.state = State.FLYING;
        switch (this.phase) {
            case AIR -> {
                if (this.hangAge >= 0) {
                    // Hanging from its hands on an edge: neither carried along nor down.
                    this.limp = Math.min(1.0, this.limp + GO_LIMP);
                    return true;
                }
                this.follow(entity, true);
                this.limp = Math.min(1.0, this.limp + GO_LIMP);
                Vec3 push = entity.getDeltaMovement();
                if (push.horizontalDistanceSqr() > THROWN_WAY * THROWN_WAY) {
                    this.wayX = push.x;
                    this.wayZ = push.z;
                }
                boolean grounded = RagdollCauses.grounded(entity) || entity.isInWater() || entity.isInLava();
                this.flew |= !grounded;
                // Down once its creature is on the ground again (or never left it), or after the longest flight; at
                // once when it is thrown back against a wall.
                if (this.age > 1 && RagdollFalls.pinned((Ragdoll) this)) {
                    this.fall();
                } else if (grounded && this.age > (this.flew ? 1 : SHRUG) || this.age > LONGEST_FLIGHT) {
                    this.fall();
                    RagdollFalls.pinned((Ragdoll) this);
                }
            }
            case DOWN -> {
                if (again) {
                    this.lift(entity);
                } else {
                    RagdollFalls.slide((Ragdoll) this);
                    this.down++;
                    if (this.world.touching()) {
                        this.lain++;
                    }
                    this.keepNear(entity);
                    if (Knocked.getsUp(entity.getId(), this.down, this.lain, this.kind().ticks)) {
                        this.getUp();
                    }
                }
            }
            case UP -> {
                if (again) {
                    this.lift(entity);
                } else if (downed) {
                    this.knockedDown();
                    DamageSource source = entity.getLastDamageSource();
                    RagdollFalls.knockBack((Ragdoll) this, entity.getDeltaMovement(),
                            source == null ? null : source.getSourcePosition());
                } else if (this.up >= this.kind().ticks) {
                    Knocked.forget(entity.getId());
                    Facings.rose(entity, this.riseYaw);
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    void die(int tick) {
        this.release();
        super.die(tick);
        // It dies holding the pose it is in now, not the one it went limp in.
        this.muscles.rebase(this.world);
    }

    @Override
    void lift(LivingEntity entity) {
        this.release();
        super.lift(entity);
    }
}
