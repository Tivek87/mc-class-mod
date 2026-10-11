package nl.tivek.multiversepowers.stage.client;

import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.stage.LampKind;
import nl.tivek.multiversepowers.stage.LampSettings;
import nl.tivek.multiversepowers.stage.StageLightBlock;
import nl.tivek.multiversepowers.stage.StageLightBlockEntity;

// How a stage light stands this frame in this game: where its head points (following where it should point a
// little behind, so a jump of its aim swings rather than snaps), its pan and tilt, its spin and where its light
// comes out.
final class LampView {
    private static final Map<StageLightBlockEntity, LampView> VIEWS = new WeakHashMap<>();
    // How quickly the head catches up with where it should point, in seconds.
    private static final double CATCH_UP = 0.06;

    // Where each kind's head turns (in pixels up from its mount) and how far out its lens sits from there.
    static double pivot(LampKind kind) {
        return switch (kind) {
            case MOVING_HEAD, PAR -> 7.5;
            case SEARCHLIGHT -> 6.5;
            case LASER -> 6.0;
            case STROBE -> 5.0;
            case DISCO_BALL -> 9.0;
        };
    }

    static double lensOut(LampKind kind) {
        return switch (kind) {
            case MOVING_HEAD -> 7.1;
            case SEARCHLIGHT -> 9.1;
            case PAR -> 6.1;
            case LASER, STROBE -> 0.1;
            case DISCO_BALL -> 4.5;
        };
    }

    Vec3[] axes;
    Vec3 dir;
    double pan;
    double tilt;
    double roll;
    Vec3 lens = Vec3.ZERO;
    double seconds;
    private long nanos;
    // Where its beams ended at the tick they were last worked out, per beam.
    long clipTick = Long.MIN_VALUE;
    double[] reach = new double[0];
    boolean[] struck = new boolean[0];

    private LampView(Direction facing) {
        this.axes = LampMotion.axes(facing);
        this.dir = this.axes[1];
    }

    static LampView of(StageLightBlockEntity lamp, float partialTick) {
        Direction facing = lamp.getBlockState().hasProperty(StageLightBlock.FACING)
                ? lamp.getBlockState().getValue(StageLightBlock.FACING) : Direction.UP;
        LampView view = VIEWS.computeIfAbsent(lamp, key -> new LampView(facing));
        view.axes = LampMotion.axes(facing);
        view.update(lamp, partialTick);
        return view;
    }

    private void update(StageLightBlockEntity lamp, float partialTick) {
        Level level = lamp.getLevel();
        LampSettings settings = lamp.settings();
        this.seconds = level == null ? 0.0 : (level.getGameTime() + partialTick) / 20.0;
        BlockPos pos = lamp.getBlockPos();
        LampKind kind = lamp.kind();
        Vec3 pivot = Vec3.atCenterOf(pos).add(this.axes[1].scale((pivot(kind) - 8.0) / 16.0));
        Vec3 want = this.aim(lamp, settings, level, pivot, partialTick);
        want = LampMotion.kept(want, this.axes);
        long now = System.nanoTime();
        double dt = this.nanos == 0L ? 1.0 : Math.min(1.0, (now - this.nanos) / 1.0E9);
        this.nanos = now;
        double k = 1.0 - Math.exp(-dt / CATCH_UP);
        Vec3 mixed = this.dir.add(want.subtract(this.dir).scale(k));
        this.dir = mixed.lengthSqr() < 1.0E-8 ? want : mixed.normalize();
        double[] turn = LampMotion.panTilt(this.dir, this.axes, this.pan);
        this.pan = turn[0];
        this.tilt = turn[1];
        this.roll = LampMotion.roll(settings, this.seconds);
        this.lens = pivot.add(this.dir.scale(lensOut(kind) / 16.0));
    }

    private Vec3 aim(StageLightBlockEntity lamp, LampSettings settings, Level level, Vec3 pivot, float partialTick) {
        Vec3 rest = LampMotion.rest(this.axes, settings);
        Vec3 at = null;
        switch (settings.aim) {
            case SPOT -> at = settings.spot;
            case FOLLOW, MOB, PLAYER, HOSTILE -> {
                Entity target = level == null || lamp.target() < 0 ? null : level.getEntity(lamp.target());
                if (target != null) {
                    at = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.6, 0.0);
                }
            }
            default -> {
            }
        }
        if (at != null && at.distanceToSqr(pivot) > 1.0E-4) {
            return at.subtract(pivot).normalize();
        }
        double[] swing = LampMotion.swing(settings, this.seconds);
        return LampMotion.swung(rest, this.axes, swing[0], swing[1]);
    }
}
