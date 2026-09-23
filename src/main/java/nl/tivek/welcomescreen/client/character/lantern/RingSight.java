package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.lantern.Fear;
import nl.tivek.welcomescreen.character.lantern.RingScan;
import org.joml.Matrix4f;

/**
 * What the ring's scan shows (see {@link RingScan}). Everyone sees its wave roll out: a band of light spreading out
 * from where it set out, a curtain of light under and over it, through walls and all. Only its maker sees what it
 * found: every creature it passed gets a frame of light round it, its corners marked, that shows through walls, with
 * its name and its health over it, for as long as the scan said. The frame snaps in from wide as the wave reaches it,
 * with a tick you can hear. Its colour says what it is: red for what is out to hurt you, violet for the creatures of
 * the dark (see {@link Fear}), blue for other players, green for everything else.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class RingSight {
    /** The frames of light: seen through everything, never hiding anything. */
    private static final RenderType MARKS = RenderType.create("welcomescreen_scan_marks",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final int HOSTILE = 0xFF5A3C;
    private static final int DARK = 0xC070FF;
    private static final int PLAYER = 0x7FD4FF;
    private static final int OTHER = 0x6CFF8E;
    // How long a frame takes to snap in, and to die away at the end, in ticks; how long the wave dies away.
    private static final float SNAP = 5.0F;
    private static final float GONE = 10.0F;
    private static final float WAVE_FADE = 10.0F;
    // How many new marks may tick at once, so a crowded place does not rattle.
    private static final int TICKS_AT_ONCE = 3;

    /** One creature the scan marked: from when, until when, in client ticks. */
    private record Mark(int from, int until) {
    }

    private static final Map<Integer, Mark> MARKED = new HashMap<>();
    // Which scans have marked which creatures already, so a creature is marked once per scan.
    private static final Map<Integer, Set<Integer>> SEEN = new HashMap<>();
    private static int clientTicks;

    private RingSight() {
    }

    /**
     * The wave of a scan, as everyone sees it: a band of light round where it set out, as far as it has rolled, with a
     * curtain of light under and over it, dying away once it reached its end. Light, not a construct.
     */
    static void wave(ConstructPainter painter, Vec3 center, double radius, double clock) {
        double reached = Math.min(radius, RingScan.SPEED * clock);
        double fade = 1.0 - Mth.clamp((clock - radius / RingScan.SPEED) / WAVE_FADE, 0.0, 1.0);
        if (fade <= 0.0 || reached < 0.2) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        painter.circle(center, east, south, reached, 0.12, 0.9, ConstructPainter.alpha(fade),
                ConstructPainter.alpha(0.5 * fade));
        for (int k = -1; k <= 1; k += 2) {
            painter.circle(center.add(0.0, 1.1 * k, 0.0), east, south, reached, 0.05, 0.4,
                    ConstructPainter.alpha(0.45 * fade), ConstructPainter.alpha(0.2 * fade));
        }
        int lines = 48;
        for (int i = 0; i < lines; i++) {
            double angle = Math.PI * 2.0 * i / lines;
            Vec3 foot = center.add(Math.cos(angle) * reached, -1.1, Math.sin(angle) * reached);
            painter.edge(foot, foot.add(0.0, 2.2, 0.0), 0.04, 0.35 * fade);
        }
    }

    // ---- Marking ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        LocalPlayer player = minecraft.player;
        if (level == null || player == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        MARKED.values().removeIf(mark -> clientTicks > mark.until() + GONE);
        Set<Integer> going = new HashSet<>();
        int ticked = 0;
        for (ClientConstructs.Scan scan : ClientConstructs.scans(0.0F)) {
            going.add(scan.id());
            if (scan.owner() != player.getId()) {
                continue;
            }
            Set<Integer> seen = SEEN.computeIfAbsent(scan.id(), id -> new HashSet<>());
            AABB area = new AABB(scan.center(), scan.center()).inflate(scan.reached());
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                            && !(entity instanceof ArmorStand))) {
                if (living.distanceToSqr(scan.center()) > scan.reached() * scan.reached()
                        || !seen.add(living.getId())) {
                    continue;
                }
                int until = clientTicks + (int) Math.round(scan.seconds() * 20.0);
                MARKED.put(living.getId(), new Mark(clientTicks, until));
                if (ticked++ < TICKS_AT_ONCE) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_CLUSTER_HIT,
                            living instanceof Enemy ? 1.6F : 2.0F, 0.35F));
                }
            }
        }
        SEEN.keySet().retainAll(going);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MARKED.clear();
        SEEN.clear();
    }

    // ---- Drawing the marks ----

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER || MARKED.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(MARKS);
        Matrix4f matrix = pose.last().pose();
        float now = clientTicks + partialTick;
        // The frames first, all in one go, and only then the names: writing text takes buffers of its own, which
        // would end the frames' buffer halfway.
        List<Label> labels = new ArrayList<>();
        for (Map.Entry<Integer, Mark> entry : MARKED.entrySet()) {
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
                continue;
            }
            Mark mark = entry.getValue();
            float age = now - mark.from();
            float strength = Mth.clamp((mark.until() + GONE - now) / GONE, 0.0F, 1.0F);
            if (strength <= 0.0F) {
                continue;
            }
            // Snapping in from wide as the wave reaches it.
            float snap = Mth.clamp(age / SNAP, 0.0F, 1.0F);
            double widen = 1.0 + 0.6 * (1.0 - snap) * (1.0 - snap);
            AABB box = living.getBoundingBox().move(living.getPosition(partialTick).subtract(living.position()));
            Vec3 middle = box.getCenter();
            box = box.inflate(0.08 + (widen - 1.0) * box.getXsize(), 0.08 + (widen - 1.0) * box.getYsize() * 0.5,
                    0.08 + (widen - 1.0) * box.getZsize());
            int rgb = colour(living);
            float alpha = strength * (0.75F + 0.25F * (1.0F - snap));
            corners(buffer, matrix, eye, box, rgb, alpha);
            if (age < SNAP * 2.0F) {
                // A ping of light as it is found.
                float ping = 1.0F - age / (SNAP * 2.0F);
                corners(buffer, matrix, eye, box.inflate(0.4 * (1.0 - ping)), 0xFFFFFF, 0.6F * ping * strength);
            }
            labels.add(new Label(living, middle.add(0.0, box.getYsize() * 0.5 + 0.35, 0.0), rgb, strength));
        }
        buffers.endBatch(MARKS);
        for (Label label : labels) {
            label(pose, buffers, eye, camera, label.living(), label.at(), label.rgb(), label.strength());
        }
        buffers.endBatch();
    }

    /** A name to write over a frame, once all the frames are drawn. */
    private record Label(LivingEntity living, Vec3 at, int rgb, float strength) {
    }

    /** What colour a creature's frame is: what it is to you. */
    private static int colour(LivingEntity living) {
        if (living.getType().is(Fear.FEARS_THE_LIGHT)) {
            return DARK;
        }
        if (living instanceof Enemy) {
            return HOSTILE;
        }
        return living instanceof Player ? PLAYER : OTHER;
    }

    /** The eight corners of a box marked with short lines of light, three at each corner along its edges. */
    private static void corners(VertexConsumer buffer, Matrix4f matrix, Vec3 eye, AABB box, int rgb, float alpha) {
        double[] xs = { box.minX, box.maxX };
        double[] ys = { box.minY, box.maxY };
        double[] zs = { box.minZ, box.maxZ };
        double lx = Math.min(0.35, box.getXsize() * 0.3);
        double ly = Math.min(0.35, box.getYsize() * 0.25);
        double lz = Math.min(0.35, box.getZsize() * 0.3);
        for (int i = 0; i < 8; i++) {
            Vec3 c = new Vec3(xs[i & 1], ys[i >> 1 & 1], zs[i >> 2 & 1]);
            double sx = (i & 1) == 0 ? 1.0 : -1.0;
            double sy = (i >> 1 & 1) == 0 ? 1.0 : -1.0;
            double sz = (i >> 2 & 1) == 0 ? 1.0 : -1.0;
            line(buffer, matrix, eye, c, c.add(sx * lx, 0.0, 0.0), rgb, alpha);
            line(buffer, matrix, eye, c, c.add(0.0, sy * ly, 0.0), rgb, alpha);
            line(buffer, matrix, eye, c, c.add(0.0, 0.0, sz * lz), rgb, alpha);
        }
    }

    /** A line of light from {@code a} to {@code b} that faces the camera, a few pixels wide however far it is. */
    private static void line(VertexConsumer buffer, Matrix4f matrix, Vec3 eye, Vec3 a, Vec3 b, int rgb, float alpha) {
        Vec3 middle = a.add(b).scale(0.5);
        Vec3 side = b.subtract(a).cross(eye.subtract(middle));
        double length = side.length();
        if (length < 1.0E-6) {
            return;
        }
        double width = 0.028 * Math.max(1.0, eye.distanceTo(middle) / 6.0);
        side = side.scale(width / length);
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int bl = rgb & 0xFF;
        int full = (int) (255 * Mth.clamp(alpha, 0.0F, 1.0F));
        vertex(buffer, matrix, eye, a.add(side), r, g, bl, full);
        vertex(buffer, matrix, eye, b.add(side), r, g, bl, full);
        vertex(buffer, matrix, eye, b.subtract(side), r, g, bl, full);
        vertex(buffer, matrix, eye, a.subtract(side), r, g, bl, full);
    }

    private static void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 eye, Vec3 at, int r, int g, int b,
            int alpha) {
        buffer.addVertex(matrix, (float) (at.x - eye.x), (float) (at.y - eye.y), (float) (at.z - eye.z))
                .setColor(r, g, b, alpha);
    }

    /** The creature's name and health over its frame, turned to you, seen through walls. */
    private static void label(PoseStack pose, MultiBufferSource buffers, Vec3 eye, Camera camera, LivingEntity living,
            Vec3 at, int rgb, float strength) {
        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        Component text = Component.literal("").append(living.getDisplayName()).append(Component.literal(String.format(
                java.util.Locale.ROOT, "  %.0f/%.0f ❤", Math.max(0.0F, living.getHealth()),
                living.getMaxHealth())));
        double distance = eye.distanceTo(at);
        float scale = 0.025F * (float) Math.max(1.0, distance / 8.0);
        pose.pushPose();
        pose.translate(at.x - eye.x, at.y - eye.y, at.z - eye.z);
        pose.mulPose(camera.rotation());
        pose.scale(scale, -scale, scale);
        int alpha = (int) (255 * Mth.clamp(strength, 0.1F, 1.0F));
        float x = -font.width(text) / 2.0F;
        font.drawInBatch(text, x, 0.0F, alpha << 24 | rgb, false, pose.last().pose(), buffers,
                Font.DisplayMode.SEE_THROUGH, (int) (0.4F * alpha) << 24, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }
}
