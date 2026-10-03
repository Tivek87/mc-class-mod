package nl.tivek.multiversepowers.character.greenlantern.client.render.hand;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The developer's test hand (power wheel, Developer): a plain flesh-coloured hand on the Giant Hands' bones rises out
// of a portal a few blocks ahead, shows off every joint it has (wrist, each finger on its own, a fist, a spread, a
// claw, a wave, a count and a point) while it idles, and sinks back after ten seconds. Only this game sees it.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class DevHand {
    private static final int LIFE = 200;
    private static final double SCALE = 0.24;
    private static final double AHEAD = 5.0;
    private static final double BURIED = -8.0;
    private static final double STAND = 5.5;
    private static final double RISE = 18.0;
    private static final double SINK = 185.0;
    private static final double PORTAL = 1.5;
    private static final Material SKIN = new Material(0xE0A585, 0xA8705A, 0x000000, 0xF3CDB6);
    private static final Material RIFT = new Material(0x1A0A2E, 0xB07CFF, 0x6A2FD8, 0xF0D8FF);

    @Nullable
    private static Vec3 base;
    private static Vec3 facing = new Vec3(0.0, 0.0, 1.0);
    private static long born;

    private DevHand() {
    }

    public static boolean showing() {
        return base != null;
    }

    // A few blocks ahead of the player, on the ground there, turned towards them.
    public static void spawn() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (player == null || level == null) {
            return;
        }
        Vec3 flat = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 over = player.position().add(flat.scale(AHEAD)).add(0.0, 3.0, 0.0);
        BlockHitResult ground = level.clip(new ClipContext(over, over.subtract(0.0, 12.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        base = ground.getType() == HitResult.Type.MISS ? player.position().add(flat.scale(AHEAD))
                : ground.getLocation();
        facing = flat.scale(-1.0);
        born = level.getGameTime();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Vec3 at = base;
        Minecraft minecraft = Minecraft.getInstance();
        if (at == null || event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS
                || minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        double age = minecraft.level.getGameTime() - born + partialTick;
        if (age >= LIFE || age < 0.0) {
            base = null;
            return;
        }
        Camera camera = event.getCamera();
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(),
                (float) (age % 24000.0), event.getFrustum());
        portal(painter, at, age);
        painter.material(SKIN);
        painter.creases(0.0);
        painter.ambient(0.12);
        painter.clip(at.add(0.0, 0.02, 0.0), Vectors.UP, 0.06);
        HandPose pose = pose(age);
        HandPainter.drawHand(painter, pose, pose.place(at, facing, SCALE), false, 1.0, -1.0, 17, true);
        painter.noClip();
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    // A dark rift on the ground with a bright rim, opening before the hand and closing after it.
    private static void portal(LanternPainter painter, Vec3 at, double age) {
        double open = Ease.smooth(age / 8.0) * (1.0 - Ease.smooth((age - LIFE + 10.0) / 10.0));
        if (open <= 0.0) {
            return;
        }
        painter.material(RIFT);
        Vec3 center = at.add(0.0, 0.03, 0.0);
        Vec3 a = new Vec3(1.0, 0.0, 0.0);
        Vec3 b = new Vec3(0.0, 0.0, 1.0);
        double radius = PORTAL * open;
        painter.lightDisc(center, radius * 0.95, 0x0C0418, 0.9 * open, 0.2, 3);
        painter.circle(center, a, b, radius, 0.06, 0.35, 0xE6, 0x90);
        double spin = age * 0.15;
        painter.circle(center, Vectors.spin(a, Vectors.UP, spin), Vectors.spin(b, Vectors.UP, spin), radius * 0.72,
                0.03, 0.2, 0x80, 0x50);
    }

    // The show, tick by tick, over an idle that never stops.
    static HandPose pose(double t) {
        HandPose pose = new HandPose();
        double up = Ease.smoother(t / RISE) * (1.0 - Ease.smoother((t - SINK) / (LIFE - SINK)));
        pose.length = Mth.lerp(up, BURIED, STAND);
        double idle = Math.sin(t * 0.09);
        for (int k = 0; k < 5; k++) {
            pose.curl[k] = 0.08 + 0.05 * Math.sin(t * 0.11 + k * 0.9) + 0.5 * (1.0 - up);
        }
        pose.lean = 0.05 * idle;
        // The wrist: bending back and forth, then turning.
        double wrist = window(t, 18.0, 40.0);
        pose.flex = 0.75 * Math.sin((t - 18.0) / 22.0 * Math.PI * 2.0) * wrist;
        pose.twist = 0.9 * Math.sin((t - 18.0) / 22.0 * Math.PI) * wrist;
        // Each finger curling and opening on its own, the thumb last.
        for (int k = 0; k < 5; k++) {
            double start = 40.0 + k * 10.0;
            pose.curl[k] += Ease.bump((t - start) / 5.0 - 1.0);
        }
        // A fist, then the hand thrown wide open, then a claw.
        double fist = window(t, 90.0, 104.0);
        for (int k = 0; k < 5; k++) {
            pose.curl[k] = Mth.lerp(fist, pose.curl[k], 1.0);
        }
        double wide = window(t, 104.0, 116.0);
        pose.spread = wide;
        pose.thumbOut = 0.5 * wide;
        double claw = window(t, 116.0, 130.0);
        for (int k = 0; k < 5; k++) {
            pose.hook[k] = claw;
            pose.curl[k] = Mth.lerp(claw, pose.curl[k], 0.25);
        }
        // A wave.
        double wave = window(t, 130.0, 150.0);
        pose.spread = Math.max(pose.spread, 0.6 * wave);
        pose.sweep = 0.45 * Math.sin((t - 130.0) / 20.0 * Math.PI * 3.0) * wave;
        pose.lean += 0.2 * Math.sin((t - 130.0) / 20.0 * Math.PI * 3.0 + 0.6) * wave;
        // Counting from a fist, one finger after another.
        double count = window(t, 150.0, 175.0);
        for (int k = 0; k < 5; k++) {
            double out = Ease.smooth((t - 152.0 - k * 4.0) / 3.0);
            pose.curl[k] = Mth.lerp(count, pose.curl[k], 1.0 - out);
        }
        // Pointing at the player.
        double point = window(t, 175.0, 186.0);
        for (int k = 0; k < 5; k++) {
            pose.curl[k] = Mth.lerp(point, pose.curl[k], k == 0 ? 0.0 : 1.0);
        }
        pose.lean += 0.35 * point;
        pose.flex += 0.25 * point;
        return pose;
    }

    // 0 outside [from, to], 1 inside, eased in and out over a few ticks at each end.
    private static double window(double t, double from, double to) {
        return Ease.smooth((t - from) / 3.0) * (1.0 - Ease.smooth((t - to + 3.0) / 3.0));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        base = null;
    }
}
