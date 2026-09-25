package nl.tivek.multiversepowers.character.greenlantern.client.render;

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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.ability.RingScan;
import nl.tivek.multiversepowers.character.greenlantern.ability.ScanGlow;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.faction.Standing;
import nl.tivek.multiversepowers.faction.client.ClientStandings;
import org.joml.Matrix4f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class RingSight {
    private static final RenderType MARKS = RenderType.create("welcomescreen_scan_marks",
            DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 4096, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    private static final float SNAP = 5.0F;
    private static final float GONE = 10.0F;
    private static final float WAVE_FADE = 10.0F;
    private static final int TICKS_AT_ONCE = 3;
    // Must match the plane's scan height on the server (AirStrike), or marking would disagree with hits.
    private static final double PLANE_SCAN_HIGH = 48.0;

    private record Mark(int from, int until) {
    }

    private static final Map<Integer, Mark> MARKED = new HashMap<>();
    private static final Map<Integer, Set<Integer>> SEEN = new HashMap<>();
    private static int clientTicks;

    private RingSight() {
    }

    public static void wave(LanternPainter painter, Vec3 center, double radius, double clock) {
        double reached = Math.min(radius, RingScan.SPEED * clock);
        double fade = 1.0 - Mth.clamp((clock - radius / RingScan.SPEED) / WAVE_FADE, 0.0, 1.0);
        if (fade <= 0.0 || reached < 0.2) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        painter.circle(center, east, south, reached, 0.12, 0.9, Colors.alpha(fade),
                Colors.alpha(0.5 * fade));
        for (int k = -1; k <= 1; k += 2) {
            painter.circle(center.add(0.0, 1.1 * k, 0.0), east, south, reached, 0.05, 0.4,
                    Colors.alpha(0.45 * fade), Colors.alpha(0.2 * fade));
        }
        int lines = 48;
        for (int i = 0; i < lines; i++) {
            double angle = Math.PI * 2.0 * i / lines;
            Vec3 foot = center.add(Math.cos(angle) * reached, -1.1, Math.sin(angle) * reached);
            painter.edge(foot, foot.add(0.0, 2.2, 0.0), 0.04, 0.35 * fade);
        }
    }

    public static void ringLight(LanternPainter painter, Vec3 ring, double clock, boolean own) {
        double strength = Ease.smooth(clock / 4.0) * (1.0 - Ease.smooth((clock - 30.0) / 8.0));
        if (strength <= 0.0) {
            return;
        }
        double size = own ? 0.06 : 0.22;
        double pulse = 0.8 + 0.2 * Math.sin(clock * 1.3);
        painter.flare(ring, size * pulse, strength);
        if (clock < 8.0) {
            Vec3 view = painter.camera().subtract(ring);
            if (view.lengthSqr() > 1.0E-6) {
                Vec3 facing = view.normalize();
                Vec3 side = facing.cross(Vectors.UP);
                side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
                double burst = clock / 8.0;
                painter.circle(ring, side, side.cross(facing), size * (1.0 + 5.0 * burst), size * 0.08, size * 0.5,
                        Colors.alpha(1.0 - burst), Colors.alpha(0.5 * (1.0 - burst)));
            }
        }
    }

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
        glow(level);
        Set<Integer> going = new HashSet<>();
        int ticked = 0;
        for (ClientConstructs.Scan scan : ClientConstructs.scans(0.0F)) {
            going.add(scan.id());
            if (scan.owner() != player.getId()) {
                continue;
            }
            Set<Integer> seen = SEEN.computeIfAbsent(scan.id(), id -> new HashSet<>());
            double high = scan.hostileOnly() ? PLANE_SCAN_HIGH : scan.reached();
            AABB area = new AABB(scan.center(), scan.center()).inflate(scan.reached(), high, scan.reached());
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, area,
                    entity -> entity != player && entity.isAlive() && !entity.isSpectator()
                            && !(entity instanceof ArmorStand))) {
                double dx = living.getX() - scan.center().x;
                double dz = living.getZ() - scan.center().z;
                double far = scan.hostileOnly() ? dx * dx + dz * dz : living.distanceToSqr(scan.center());
                if (far > scan.reached() * scan.reached() || scan.hostileOnly() && !hostile(living)
                        || !seen.add(living.getId())) {
                    continue;
                }
                int until = clientTicks + (int) Math.round(scan.seconds() * 20.0);
                MARKED.put(living.getId(), new Mark(clientTicks, until));
                if (ticked++ < TICKS_AT_ONCE) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_CLUSTER_HIT,
                            hostile(living) ? 1.6F : 2.0F, 0.35F));
                }
            }
        }
        SEEN.keySet().retainAll(going);
    }

    private static void glow(ClientLevel level) {
        Map<Integer, Integer> glowing = new HashMap<>();
        for (Map.Entry<Integer, Mark> entry : MARKED.entrySet()) {
            if (clientTicks <= entry.getValue().until()
                    && level.getEntity(entry.getKey()) instanceof LivingEntity living && living.isAlive()) {
                glowing.put(entry.getKey(), colour(living));
            }
        }
        ScanGlow.set(glowing);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MARKED.clear();
        SEEN.clear();
        ScanGlow.clear();
    }

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

    private record Label(LivingEntity living, Vec3 at, int rgb, float strength) {
    }

    private static int colour(LivingEntity living) {
        return ClientStandings.of(living).rgb();
    }

    private static boolean hostile(LivingEntity living) {
        return ClientStandings.of(living) == Standing.HOSTILE;
    }

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
