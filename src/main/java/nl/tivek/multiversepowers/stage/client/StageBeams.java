package nl.tivek.multiversepowers.stage.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.stage.LampKind;
import nl.tivek.multiversepowers.stage.LampSettings;
import nl.tivek.multiversepowers.stage.StageLightBlockEntity;

// The light out of every stage light drawn this frame: beams, laser lines, wash cones, strobe flashes and the mirror
// ball's dots. A beam passes glass and other blocks light passes, and ends on the first block it meets.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class StageBeams {
    private record Queued(StageLightBlockEntity lamp, LampView view) {
    }

    private static final List<Queued> FRAME = new ArrayList<>();
    private static final Material WHITE = new Material(0xFFFFFF, 0xFFFFFF, 0xFFFFFF, 0xFFFFFF);
    // A beam never draws thinner than this share of its distance, so far ones still read.
    private static final double LEAST_WIDTH = 0.0016;
    private static final int SEGMENTS = 8;

    private StageBeams() {
    }

    static void queue(StageLightBlockEntity lamp, LampView view) {
        FRAME.add(new Queued(lamp, view));
    }

    // How strong its light is now: 0 off, up to 3.
    static double strength(StageLightBlockEntity lamp, LampView view) {
        return lamp.lit() ? LampMotion.flash(lamp.settings(), view.seconds) * lamp.settings().brightness : 0.0;
    }

    static int color(StageLightBlockEntity lamp, LampView view, int beam) {
        return LampMotion.color(lamp.settings(), view.seconds, beam);
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || FRAME.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        List<Queued> drawn = new ArrayList<>(FRAME);
        FRAME.clear();
        if (level == null || ClientSettings.STAGE_BEAMS.get() == 0) {
            return;
        }
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        double reach = ClientSettings.STAGE_REACH.get();
        drawn.removeIf(q -> q.lamp.isRemoved() || q.lamp.getLevel() != level || q.view.lens.distanceToSqr(eye) > reach * reach);
        drawn.sort(Comparator.comparingDouble(q -> q.view.lens.distanceToSqr(eye)));
        int most = ClientSettings.STAGE_MOST.get();
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        ConstructPainter painter = new ConstructPainter(event.getPoseStack(), eye,
                (float) ((level.getGameTime() + partialTick) % 24000.0), event.getFrustum(), WHITE);
        long tick = level.getGameTime();
        for (int i = 0; i < Math.min(most, drawn.size()); i++) {
            draw(painter, level, drawn.get(i), eye, tick);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    private static void draw(ConstructPainter painter, ClientLevel level, Queued queued, Vec3 eye, long tick) {
        StageLightBlockEntity lamp = queued.lamp;
        LampView view = queued.view;
        double strength = strength(lamp, view);
        if (strength <= 0.0) {
            return;
        }
        LampSettings settings = lamp.settings();
        LampKind.Look look = lamp.kind().look;
        Vec3[] dirs = dirs(look, settings, view);
        Vec3[] starts = new Vec3[dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            starts[i] = look == LampKind.Look.DOTS ? view.lens.add(dirs[i].scale(0.29)) : view.lens;
        }
        clip(level, lamp, view, starts, dirs, settings, tick);
        for (int i = 0; i < dirs.length; i++) {
            int rgb = color(lamp, view, i);
            Vec3 end = starts[i].add(dirs[i].scale(view.reach[i]));
            switch (look) {
                case BEAM, CONE -> beam(painter, starts[i], dirs[i], end, view.reach[i], view.struck[i], settings,
                        rgb, strength, look == LampKind.Look.CONE, eye);
                case LASER, DOTS -> line(painter, starts[i], end, view.struck[i], settings, rgb, strength, eye,
                        look == LampKind.Look.DOTS);
                case FLASH -> flash(painter, starts[i], dirs[i], end, settings, rgb, strength);
            }
        }
    }

    // The ways its light goes: one beam, a laser's fan, or a mirror ball's rays.
    private static Vec3[] dirs(LampKind.Look look, LampSettings settings, LampView view) {
        int count = look == LampKind.Look.LASER || look == LampKind.Look.DOTS ? settings.beams : 1;
        if (look == LampKind.Look.DOTS) {
            return LampMotion.rays(count, view.axes, view.roll);
        }
        if (count == 1) {
            return new Vec3[] { view.dir };
        }
        Vec3[] around = LampMotion.around(view.dir, view.axes);
        Vec3 side = around[0].scale(Math.cos(view.roll)).add(around[1].scale(Math.sin(view.roll)));
        double fan = Math.toRadians(settings.fan);
        Vec3[] dirs = new Vec3[count];
        for (int i = 0; i < count; i++) {
            double angle = fan * (i / (count - 1.0) - 0.5);
            dirs[i] = view.dir.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
        }
        return dirs;
    }

    // How far each beam reaches, worked out once a tick: its whole length, or to the first block light does not
    // pass.
    private static void clip(ClientLevel level, StageLightBlockEntity lamp, LampView view, Vec3[] starts, Vec3[] dirs,
            LampSettings settings, long tick) {
        if (view.clipTick == tick && view.reach.length == dirs.length) {
            return;
        }
        view.clipTick = tick;
        view.reach = new double[dirs.length];
        view.struck = new boolean[dirs.length];
        BlockPos own = lamp.getBlockPos();
        for (int i = 0; i < dirs.length; i++) {
            Vec3 to = starts[i].add(dirs[i].scale(settings.length));
            BlockHitResult hit = settings.stopAtBlocks ? hit(level, own, starts[i], to) : null;
            view.reach[i] = hit == null ? settings.length : Math.max(0.05, hit.getLocation().distanceTo(starts[i]));
            view.struck[i] = hit != null;
        }
    }

    private static BlockHitResult hit(ClientLevel level, BlockPos own, Vec3 from, Vec3 to) {
        return BlockGetter.traverseBlocks(from, to, level, (world, pos) -> {
            if (pos.equals(own) || !world.isLoaded(pos)) {
                return null;
            }
            BlockState state = world.getBlockState(pos);
            if (state.isAir() || state.propagatesSkylightDown(world, pos)) {
                return null;
            }
            return world.clipWithInteractionOverride(from, to, pos, state.getShape(world, pos), state);
        }, world -> null);
    }

    private static void beam(ConstructPainter painter, Vec3 start, Vec3 dir, Vec3 end, double length, boolean struck,
            LampSettings settings, int rgb, double strength, boolean wash, Vec3 eye) {
        double spread = Math.tan(Math.toRadians(settings.spread) * 0.5) * 2.0;
        double alpha = Math.min(1.0, (wash ? 0.22 : 0.4) * strength);
        for (int s = 0; s < SEGMENTS; s++) {
            double t0 = (double) s / SEGMENTS;
            double t1 = (double) (s + 1) / SEGMENTS;
            Vec3 a = start.add(dir.scale(length * t0));
            Vec3 b = start.add(dir.scale(length * t1));
            double w0 = width(settings.width + spread * length * t0, a, eye);
            double w1 = width(settings.width + spread * length * t1, b, eye);
            double fade0 = fade(t0, struck);
            double fade1 = fade(t1, struck);
            painter.glowTaper(a, b, w0 * 2.4, w1 * 2.4, rgb, alpha * 0.55 * fade0, alpha * 0.55 * fade1);
            painter.lightTaper(a, b, w0 * 0.7, w1 * 0.7, Colors.mix(rgb, 0xFFFFFF, 0.35F), alpha * fade0,
                    alpha * fade1);
        }
        Vec3 toEye = eye.subtract(start);
        double facing = toEye.lengthSqr() < 1.0E-6 ? 1.0 : Math.max(0.0, dir.dot(toEye.normalize()));
        painter.material(new Material(rgb, Colors.mix(rgb, 0xFFFFFF, 0.5F), rgb, 0xFFFFFF));
        painter.flare(start, settings.width * 2.0 + 0.12 + facing * facing * (0.6 + settings.width * 4.0),
                Math.min(1.0, 0.35 * strength + Math.pow(facing, 6.0) * strength));
        if (struck) {
            double radius = (settings.width + spread * length) * 0.7 + 0.15;
            painter.glowDisc(end.subtract(dir.scale(0.05)), radius, rgb, Math.min(1.0, 0.5 * strength), 0.12,
                    (int) (length * 13.0));
        }
    }

    private static void line(ConstructPainter painter, Vec3 start, Vec3 end, boolean struck, LampSettings settings,
            int rgb, double strength, Vec3 eye, boolean dot) {
        Vec3 middle = start.add(end).scale(0.5);
        double width = width(settings.width, middle, eye);
        int alpha = Colors.alpha(Math.min(1.0, (dot ? 0.35 : 0.85) * strength));
        painter.lightLine(start, end, width, Colors.mix(rgb, 0xFFFFFF, 0.3F), alpha);
        painter.glowLine(start, end, width * 4.0, rgb, Colors.alpha(Math.min(1.0, 0.35 * strength)));
        if (struck) {
            painter.lightDisc(end, dot ? 0.12 + settings.width * 2.0 : 0.06 + settings.width * 2.0,
                    Colors.mix(rgb, 0xFFFFFF, 0.4F), Math.min(1.0, 0.9 * strength), 0.1, (int) (end.x * 7 + end.z));
            painter.glowDisc(end, dot ? 0.35 : 0.25, rgb, Math.min(1.0, 0.5 * strength), 0.1, 3);
        }
    }

    private static void flash(ConstructPainter painter, Vec3 start, Vec3 dir, Vec3 end, LampSettings settings,
            int rgb, double strength) {
        painter.material(new Material(rgb, 0xFFFFFF, rgb, 0xFFFFFF));
        painter.flare(start, 0.9 + settings.width, Math.min(1.0, strength));
        painter.glowDisc(start, 2.0 + settings.width * 2.0, rgb, Math.min(1.0, 0.45 * strength), 0.05, 1);
        double length = end.distanceTo(start);
        double spread = Math.tan(Math.toRadians(settings.spread) * 0.5) * 2.0;
        painter.glowTaper(start, end, settings.width * 2.0, settings.width * 2.0 + spread * length, rgb,
                Math.min(1.0, 0.5 * strength), 0.0);
    }

    private static double width(double width, Vec3 at, Vec3 eye) {
        return Math.max(width, at.distanceTo(eye) * LEAST_WIDTH);
    }

    // Light thinning out towards the end of its reach, unless a block it lights stops it first.
    private static double fade(double t, boolean struck) {
        double gone = 1.0 - t;
        return struck ? 0.55 + 0.45 * gone : Math.pow(gone, 0.8);
    }
}
