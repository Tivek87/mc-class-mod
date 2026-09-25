package nl.tivek.multiversepowers.character.docock;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * Where every tentacle wants its tip in each pose (resting, looking around, blocking, climbing,
 * carrying), and drawing the four tentacles for the clients.
 */
abstract class RigPoses extends RigMotion {
    RigPoses(ServerPlayer caster, ServerLevel home) {
        super(caster, home);
    }

    // ---- Poses ----

    /**
     * Resting: every tentacle arcs up out of your back and reaches out in front of you, claws ahead
     * where you can see them. Each one has a lane of its own: the shoulder pair higher and closer in,
     * the hip pair lower and wider, and the right two a little further ahead than the left two, so the
     * four never end up in the same place or look like one mirrored pair.
     */
    Vec3 restPose(Arm arm) {
        double sway = Math.sin(this.age * 0.05 + arm.index * 1.9);
        double drift = Math.cos(this.age * 0.037 + arm.index * 2.6);
        // Forward counts for much more than up: otherwise the claws hang above your head instead of
        // in front of you.
        double ahead = (arm.upper ? 1.7 : 1.2) + (arm.side > 0 ? 0.2 : 0.0) + 0.12 * sway;
        double high = (arm.upper ? 1.25 : -0.05) + 0.1 * sway;
        double out = (arm.upper ? 1.0 : 1.35) + 0.07 * drift;
        return this.mount(arm.index).add(this.forward().scale(ahead)).add(0, high, 0)
                .add(this.right().scale(arm.side * out)).add(this.idleGlance(arm));
    }

    /**
     * Standing still, a claw now and then turns to look somewhere for a moment: around itself, or back
     * at you over its own shoulder. It fades in and out, so nothing ever snaps.
     */
    private Vec3 idleGlance(Arm arm) {
        if (this.age >= arm.glanceUntil) {
            arm.aim = Vec3.ZERO;
            if (!this.standingStill() || !ParticleFx.chance(IDLE_CHANCE)) {
                return Vec3.ZERO;
            }
            arm.glanceFrom = this.age;
            arm.glanceUntil = this.age + 45 + ParticleFx.RANDOM.nextInt(50);
            arm.lookAtYou = ParticleFx.chance(0.35);
            if (arm.lookAtYou) {
                arm.glance = this.forward().scale(-0.8).add(0, 0.4, 0);
            } else {
                double angle = ParticleFx.RANDOM.nextDouble() * Math.PI * 2;
                arm.glance = this.right().scale(Math.cos(angle) * 0.75).add(0, Math.sin(angle) * 0.5, 0);
            }
        }
        // Fades in and out with no speed at either end, so a glance never starts or stops with a tug.
        double fade = Ease.smoother(Math.min(this.age - arm.glanceFrom, arm.glanceUntil - this.age) / 14.0);
        // The whole claw turns along with it: looking at you means the side the spikes come out of is
        // the side you see. Turning it is smoothed out on its own (see tickAims).
        arm.aim = arm.lookAtYou ? this.caster.getEyePosition().subtract(arm.tip) : arm.glance;
        return arm.glance.scale(fade);
    }

    /** True when the player is barely moving, so the tentacles have time to look around. */
    private boolean standingStill() {
        Vec3 motion = this.caster.position().subtract(this.caster.xo, this.caster.yo, this.caster.zo);
        return motion.horizontalDistanceSqr() < 0.0016 && !this.blocking && !this.climbing;
    }

    /**
     * Blocking: the four tentacles weave a cross in front of you, right behind the energy shield. Each
     * one takes its own corner of that cross, so no two ever end up in the same spot.
     */
    Vec3 blockPose(Arm arm) {
        Vec3 eye = this.caster.getEyePosition();
        // Each one takes the corner on its own side of the shield: shoulders high, hips low. Every
        // tentacle stays on the side it grows out of, so their paths never cross in front of you.
        return eye.add(this.forward().scale(arm.upper ? 1.2 : 1.0))
                .add(this.right().scale(arm.side * (arm.upper ? 0.62 : 0.88)))
                .add(0, arm.upper ? 0.42 : -0.55, 0);
    }

    /**
     * Climbing: every tentacle holds its own spot on the wall, in its own quarter around your body and
     * far away from the other three, preferably clamped on an edge or a corner. A grip is only let go
     * when it is out of reach or the block is gone, and only one tentacle reaches for a new one at a
     * time, so the others keep holding you.
     */
    Vec3 climbPose(ServerLevel level, Arm arm) {
        Vec3 body = this.body();
        if (arm.grip != null && (arm.grip.distanceTo(body) > GRIP_REACH || !this.stillThere(level, arm.grip))) {
            arm.grip = null;
        }
        if (arm.grip == null && Math.floorMod(this.age + arm.index, GRIP_EVERY) == 0) {
            arm.grip = this.findGrip(level, arm, body);
            if (arm.grip != null) {
                arm.gripAge = this.age;
                this.sound(arm.grip, SoundEvents.NETHERITE_BLOCK_HIT, 0.35F, 1.7F);
                ParticleFx.cloud(level, ParticleTypes.CRIT, arm.grip, 3, 0.1, 0.03);
            }
        }
        if (arm.grip != null) {
            // A small pull towards the body: the claw looks like it is really carrying you.
            double pull = Math.min(0.08, Math.max(0.0, (this.age - arm.gripAge) * 0.002));
            return arm.grip.add(body.subtract(arm.grip).normalize().scale(pull));
        }
        return body.add(this.sector(arm, 0.0).scale(CLIMB_SPREAD));
    }

    /** The best free spot on the surface for this tentacle: high, wide apart, and on an edge. */
    @Nullable
    private Vec3 findGrip(ServerLevel level, Arm arm, Vec3 body) {
        Vec3 best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 15; i++) {
            Vec3 direction = this.sector(arm, (i % 5 - 2) * 0.2).add(0, 0.9 - i / 5 * 0.9, 0);
            if (direction.lengthSqr() < 1.0E-6) {
                continue;
            }
            direction = direction.normalize();
            BlockHitResult hit = level.clip(new ClipContext(body, body.add(direction.scale(GRIP_REACH)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this.caster));
            if (hit.getType() == HitResult.Type.MISS) {
                continue;
            }
            Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            Vec3 point = hit.getLocation().add(normal.scale(0.08));
            double apart = this.apart(arm, point);
            if (apart < GRIP_MIN_APART) {
                continue;
            }
            // Higher is better (that is where you are going), wide apart is better, an edge is best.
            double score = (point.y - body.y) * 1.3 + Math.min(apart, 3.0) + (onEdge(level, hit) ? 1.6 : 0.0);
            if (score > bestScore) {
                bestScore = score;
                best = point;
            }
        }
        return best;
    }

    /** How far the nearest other tentacle's grip is; huge when the others hold nothing. */
    private double apart(Arm arm, Vec3 point) {
        double nearest = Double.MAX_VALUE;
        for (Arm other : this.arms) {
            if (other != arm && other.grip != null) {
                nearest = Math.min(nearest, other.grip.distanceTo(point));
            }
        }
        return nearest;
    }

    /** The direction of this tentacle's own quarter around the surface you hang on. */
    private Vec3 sector(Arm arm, double extra) {
        Vec3 into = Vec3.atLowerCornerOf(this.climbFace.getNormal()).scale(-1);
        Vec3 up = this.climbFace.getAxis().isVertical() ? this.forward() : new Vec3(0, 1, 0);
        Vec3 side = up.cross(into).normalize();
        // "side" always points to your own right, whether you hang on a wall or under a ceiling, so a
        // right tentacle stays right and a left one stays left instead of swapping over.
        if (side.dot(this.right()) < 0) {
            side = side.scale(-1);
        }
        // Its own quarter: the shoulder pair high, the hip pair low, each on the side it grows out of.
        double angle = (arm.upper ? Math.PI / 4 : Math.PI * 0.75) * arm.side + extra;
        return into.scale(0.55).add(up.scale(Math.cos(angle))).add(side.scale(Math.sin(angle))).normalize();
    }

    /** True when the block that was hit has open air beside it: an edge or a corner to clamp on. */
    private static boolean onEdge(ServerLevel level, BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        for (Direction side : Direction.values()) {
            if (side.getAxis() == hit.getDirection().getAxis()) {
                continue;
            }
            BlockPos next = pos.relative(side);
            if (level.getBlockState(next).getCollisionShape(level, next).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** True when there is still something solid where this tentacle holds on. */
    private boolean stillThere(ServerLevel level, Vec3 grip) {
        Vec3 body = this.body();
        Vec3 away = grip.subtract(body);
        if (away.lengthSqr() < 1.0E-6) {
            return false;
        }
        Vec3 end = grip.add(away.normalize().scale(0.35));
        BlockHitResult hit = level.clip(new ClipContext(body, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, this.caster));
        return hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceTo(grip) < 0.7;
    }

    private Vec3 body() {
        return this.caster.position().add(0, this.caster.getBbHeight() * 0.5, 0);
    }

    /** Blocks in the claw hang out beside you, where you can see them and aim with them. */
    Vec3 carryPose(Arm arm) {
        Vec3 eye = this.caster.getEyePosition();
        return eye.add(this.forward().scale(1.5)).add(this.right().scale(arm.side * 1.25))
                .add(0, arm.upper ? 0.2 : -0.7, 0);
    }

    /** The tentacle's line: out of the back, bending over, to its tip. */
    private List<Vec3> path(Arm arm) {
        Vec3 mount = this.mount(arm.index);
        Vec3 forward = this.forward();
        Vec3 right = this.right();
        Vec3 c1 = arm.upper
                ? mount.subtract(forward.scale(0.45)).add(0, 0.8, 0).add(right.scale(arm.side * 0.45))
                : mount.subtract(forward.scale(0.35)).add(right.scale(arm.side * 0.8)).add(0, 0.35, 0);
        double reach = mount.distanceTo(arm.tip);
        // The last piece comes in along the way the claw is really looking, so the whole tentacle
        // bends round with it. More points than the bend really needs, for a round line.
        Vec3 c2 = arm.aimShown.lengthSqr() > 1.0E-6
                ? arm.tip.subtract(arm.aimShown.scale(Math.min(1.7, 0.6 + reach * 0.25)))
                : this.elbow(arm, mount);
        return RobotArm.curve(mount, c1, c2, arm.tip, Math.max(18, (int) (reach * 4)));
    }

    void draw(ServerLevel level) {
        for (Arm arm : this.arms) {
            List<Vec3> path = this.path(arm);
            if (this.unfold < 1.0) {
                // Unfolding and folding: the tentacle grows out of (or shrinks into) its mount.
                Vec3 mount = path.get(0);
                double grown = Ease.smooth(this.unfold);
                List<Vec3> scaled = new ArrayList<>(path.size());
                for (Vec3 point : path) {
                    scaled.add(mount.add(point.subtract(mount).scale(grown)));
                }
                path = scaled;
            }
            RobotArm.Shape shape = RobotArm.arm(arm.id, path).claw(arm.claw).anchor(this.caster, arm.blend)
                    .holding(arm.held).cut(arm.cut).tipOffset(arm.tipOffset)
                    .tools(arm.spike, arm.thrust).carrying(carried(arm))
                    .lamps(this.rampage > 0 ? ArmPayload.LAMPS_RAGE : ArmPayload.LAMPS_NORMAL);
            if (arm.clipNormal.lengthSqr() > 1.0E-6) {
                shape.clip(arm.clipPoint, arm.clipNormal);
            }
            shape.send(level);
        }
        if (this.rampage > 0 && ParticleFx.chance(0.4)) {
            Arm arm = this.arms[ParticleFx.RANDOM.nextInt(this.arms.length)];
            ParticleFx.at(level, ParticleTypes.SMALL_FLAME, arm.tip);
        }
    }

    /** The blocks in this claw, as the client needs them: where each one sits and what it is. */
    private static List<ArmPayload.Carried> carried(Arm arm) {
        if (arm.load == null) {
            return List.of();
        }
        List<ArmPayload.Carried> blocks = new ArrayList<>(arm.load.size());
        for (TentacleBlocks.Piece piece : arm.load.pieces()) {
            blocks.add(new ArmPayload.Carried(
                    new Vec3(piece.offset().getX(), piece.offset().getY(), piece.offset().getZ()),
                    Block.getId(piece.state())));
        }
        return blocks;
    }
}
