package nl.tivek.multiversepowers.character.thor.hammer;

import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import nl.tivek.multiversepowers.character.thor.ThorCharge;
import nl.tivek.multiversepowers.character.thor.ThorMoves;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;
import nl.tivek.multiversepowers.engine.world.ChunkPreloader;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;

// The hammer at rest in the world: lying on a floor, stuck in a wall or ceiling, or hanging in the air. It stays while
// its Thor is near, keeps its own chunk loaded and crackles now and then; only the world moves it: it falls when what
// holds it goes, sinks in water and lava, and rides up onto a block put where it is.
abstract class MjolnirRest extends MjolnirFlight {
    private static final int CRACKLE = 10;
    private static final int LEASH = 20;
    private static final double SINK = 0.1;
    private static final double GRAVITY = 0.08;
    private static final double FASTEST = 3.0;
    // Looking this close past a face tells which block it touches.
    private static final double PROBE = 0.05;

    byte rest = ThrownHammer.HANGING;
    Direction restFace = Direction.UP;
    private double falling;
    private int restAge;

    MjolnirRest(UUID owner) {
        super(owner);
    }

    // How far away he may go before it comes home by itself.
    abstract double stays();

    @Override
    final void settle(ServerLevel level, ServerPlayer owner, @Nullable Direction face) {
        this.state = State.RESTING;
        this.rest = HammerRules.restOn(face);
        this.restFace = face == null ? Direction.UP : face;
        this.falling = 0.0;
        this.restAge = 0;
        this.shown();
        if (this.rest == ThrownHammer.HANGING) {
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, this.at, 14, 0.25, 0.12);
            level.playSound(null, this.at.x, this.at.y, this.at.z, SoundEvents.BEACON_AMBIENT, SoundSource.PLAYERS,
                    0.6F, 1.8F);
        } else {
            this.dust(level, 18);
            level.playSound(null, this.at.x, this.at.y, this.at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS,
                    0.9F, 0.9F);
        }
        ChunkPreloader.hold(level, this.at, owner.getId());
        ThorMoves.tell(owner, ThorStatePayload.NONE, 0);
    }

    // Its entity rests as it does now: how, against which face, turned the way it flew.
    private void shown() {
        if (this.shown != null) {
            this.shown.setRest(this.rest);
            this.shown.setFace(this.restFace);
            this.shown.moveTo(this.at.x, this.at.y, this.at.z, yaw(this.way), 0.0F);
        }
    }

    @Override
    final void rest(ServerLevel level, ServerPlayer owner) {
        this.restAge++;
        if (this.restAge % ChunkPreloader.EVERY_TICKS == 0) {
            ChunkPreloader.hold(level, this.at, owner.getId());
        }
        if (this.at.y < level.getMinBuildHeight() - 16) {
            this.home(owner, Hand.BELT, false);
            return;
        }
        // Left too far behind, it flies back to him by itself.
        if (this.restAge % LEASH == 0 && owner.position().distanceTo(this.at) > this.stays()) {
            this.turnBack(owner, false);
            return;
        }
        this.world(level);
        if (this.restAge % CRACKLE == 0) {
            Vec3 head = this.head(owner.getScale());
            ParticleFx.cloud(level, ParticleTypes.ELECTRIC_SPARK, head, 3, 0.15, 0.06);
            if (this.restAge % (CRACKLE * 4) == 0) {
                level.playSound(null, head.x, head.y, head.z, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.PLAYERS,
                        0.35F, 1.7F);
            }
        }
        if (ThorCharge.hammer(owner) > 1.0F && this.restAge % 3 == 0) {
            ParticleFx.at(level, ParticleFx.dust(ThorMoves.GLOW, 1.0F), this.head(owner.getScale()));
        }
    }

    // About where its head is, for its crackle: in the floor or wall it rests against, else at its middle.
    private Vec3 head(double size) {
        return this.at.add(Vec3.atLowerCornerOf(this.restFace.getNormal()).scale(0.15 * size));
    }

    // What the world does to it: a block put where it is lifts it on top; what held it gone, it falls; in water or lava
    // it sinks; landing on a floor it lies.
    private void world(ServerLevel level) {
        // Where the world is not loaded nothing moves it.
        if (!level.isLoaded(BlockPos.containing(this.at))) {
            return;
        }
        Vec3 normal = Vec3.atLowerCornerOf(this.restFace.getNormal());
        Vec3 own = this.rest == ThrownHammer.HANGING ? this.at : this.at.add(normal.scale(PROBE));
        if (solid(level, own)) {
            this.lift(level, own);
            return;
        }
        boolean fluid = !level.getFluidState(BlockPos.containing(own)).isEmpty();
        boolean held = switch (this.rest) {
            case ThrownHammer.LYING, ThrownHammer.STUCK -> solid(level, this.at.subtract(normal.scale(PROBE)));
            default -> !fluid;
        };
        if (held && this.falling <= 0.0) {
            return;
        }
        double speed = fluid ? SINK : Math.min(FASTEST, this.falling + GRAVITY);
        Vec3 next = this.at.add(0.0, -speed, 0.0);
        BlockHitResult floor = LoadedWorld.clip(level, new ClipContext(this.at, next, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (floor.getType() != HitResult.Type.MISS && floor.getDirection() == Direction.UP) {
            this.at = floor.getLocation();
            this.falling = 0.0;
            this.rest = ThrownHammer.LYING;
            this.restFace = Direction.UP;
            this.dust(level, 10);
            level.playSound(null, this.at.x, this.at.y, this.at.z, SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS,
                    0.6F, 1.0F);
        } else {
            this.at = next;
            this.falling = fluid ? 0.0 : speed;
            this.rest = fluid ? ThrownHammer.HANGING : ThrownHammer.LYING;
            this.restFace = Direction.UP;
        }
        this.shown();
    }

    // Ridden up onto the top of the block put where it was (and any stacked on it).
    private void lift(ServerLevel level, Vec3 own) {
        BlockPos top = BlockPos.containing(own);
        for (int i = 0; i < 8 && solid(level, new Vec3(own.x, top.getY() + 1.01, own.z)); i++) {
            top = top.above();
        }
        VoxelShape shape = level.isLoaded(top) ? level.getBlockState(top).getCollisionShape(level, top) : null;
        double height = shape == null || shape.isEmpty() ? 1.0 : shape.max(Direction.Axis.Y);
        this.at = new Vec3(own.x, top.getY() + height, own.z);
        this.rest = ThrownHammer.LYING;
        this.restFace = Direction.UP;
        this.falling = 0.0;
        this.shown();
    }

    // Whether a point lies inside a block's solid shape; an unloaded one counts as solid, so it stays still there.
    static boolean solid(ServerLevel level, Vec3 point) {
        BlockPos pos = BlockPos.containing(point);
        if (!level.isLoaded(pos)) {
            return true;
        }
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        if (shape.isEmpty()) {
            return false;
        }
        for (AABB box : shape.toAabbs()) {
            if (box.move(pos).inflate(1.0E-4).contains(point)) {
                return true;
            }
        }
        return false;
    }

    // A puff of the dust of the block it rests in or on.
    final void dust(ServerLevel level, int count) {
        BlockPos pos = BlockPos.containing(this.at.subtract(Vec3.atLowerCornerOf(this.restFace.getNormal())
                .scale(PROBE)));
        BlockState state = level.isLoaded(pos) ? level.getBlockState(pos) : null;
        if (state != null && !state.isAir()) {
            ParticleFx.cloud(level, new BlockParticleOption(ParticleTypes.BLOCK, state), this.at, count, 0.25, 0.12);
        }
    }

    // How it lies or sticks while it rests (ThrownHammer's LYING, STUCK, HANGING).
    final boolean stuck() {
        return this.state == State.RESTING && this.rest == ThrownHammer.STUCK;
    }
}
