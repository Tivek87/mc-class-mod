package nl.tivek.multiversepowers.engine.client.rig;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.model.BentParts;
import nl.tivek.multiversepowers.engine.client.model.ModelBends;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// The developer's view of bones: every creature's model parts and every rigged construct as lines and joints, drawn
// through whatever covers them. Only the host of a world or an operator sees it: it shows creatures behind walls.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class BoneView {
    public static final int CREATURE = 0xFFE066;
    // Magenta: it stands out on the green light constructs are made of.
    public static final int CONSTRUCT = 0xFF3CF0;
    private static final int JOINT = 0xFF4A4A;
    private static final RenderType LINES = RenderType.create("welcomescreen_bones", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 4096, false, true, RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false));
    // Half the width of a bone and of a joint's dot, in blocks, and per block away from the eye: far off (a mech, a
    // Giant Hand) they keep a few pixels wide instead of thinning out of sight.
    private static final double WIDTH = 0.012;
    private static final double DOT = 0.03;
    private static final double WIDTH_AWAY = 0.0022;
    private static final double DOT_AWAY = 0.0045;
    private static final int MOST = 20000;
    private static final Matrix4f FRAME = new Matrix4f();
    private static final Vector3f POINT = new Vector3f();
    private static final Vector3f BENT = new Vector3f();
    private static final float[] END = new float[3];
    private static boolean on;
    private static double[] bones = new double[7 * 512];
    private static int boneCount;

    private BoneView() {
    }

    // The switch as the menu shows it.
    public static boolean on() {
        return on;
    }

    public static void toggle() {
        on = !on;
    }

    public static boolean allowed() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.hasSingleplayerServer() || minecraft.player != null && minecraft.player.hasPermissions(2);
    }

    public static boolean shown() {
        return on && allowed();
    }

    public static void bone(Vec3 from, Vec3 to) {
        bone(from, to, CREATURE);
    }

    // A bone from one joint to the next, world coordinates; a joint is marked where it starts.
    public static void bone(Vec3 from, Vec3 to, int rgb) {
        add(from.x, from.y, from.z, to.x, to.y, to.z, rgb);
    }

    // A painter's frame as a bone along its own y, as the rigs of constructs build their fingers.
    public static void bone(ConstructPainter.Frame frame, double length, int rgb) {
        bone(frame.center(), frame.at(0.0, length, 0.0), rgb);
    }

    public static void chain(List<Vec3> points, int rgb) {
        for (int i = 1; i < points.size(); i++) {
            bone(points.get(i - 1), points.get(i), rgb);
        }
    }

    // A creature's model as it is drawn this frame (drawn: its pose stack right after it took its pose): each moving
    // part a bone from its pivot to the far end of its box.
    public static void model(EntityModel<?> model, Matrix4f drawn) {
        if (!shown() || !EntityPass.inWorld()) {
            return;
        }
        List<ModelParts.Part> parts = ModelParts.of(model);
        if (parts == null) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        int core = ModelBends.core(parts);
        ModelBends.Bend waist = ModelBends.waist(parts, core);
        for (ModelParts.Part part : parts) {
            float[] box = part.bounds();
            if (waist != null && part == parts.get(core)) {
                // A trunk that bends is two bones, from the end its head is on to the waist and on to the far end.
                trunk(model, drawn, part, waist, camera);
                continue;
            }
            int longest = 0;
            for (int axis = 1; axis < 3; axis++) {
                if (box[axis + 3] - box[axis] > box[longest + 3] - box[longest]) {
                    longest = axis;
                }
            }
            float[] end = END;
            for (int axis = 0; axis < 3; axis++) {
                end[axis] = axis != longest ? (box[axis] + box[axis + 3]) * 0.5F
                        : Math.abs(box[axis]) > Math.abs(box[axis + 3]) ? box[axis] : box[axis + 3];
            }
            ModelParts.frame(model, drawn, part, FRAME);
            FRAME.transformPosition(0.0F, 0.0F, 0.0F, POINT);
            double ax = camera.x + POINT.x;
            double ay = camera.y + POINT.y;
            double az = camera.z + POINT.z;
            ModelBends.Bend bend = ModelBends.bend(part);
            if (bend != null) {
                // An arm or a leg is two bones, joined at its elbow or knee and bent as a limp body bends it.
                float[] knee = bend.knee();
                FRAME.transformPosition(knee[0] / 16.0F, knee[1] / 16.0F, knee[2] / 16.0F, POINT);
                double kx = camera.x + POINT.x;
                double ky = camera.y + POINT.y;
                double kz = camera.z + POINT.z;
                add(ax, ay, az, kx, ky, kz, CREATURE);
                Matrix3f turn = BentParts.turn(part.part());
                BENT.set(end[0] - knee[0], end[1] - knee[1], end[2] - knee[2]);
                if (turn != null) {
                    turn.transform(BENT);
                }
                BENT.add(knee[0], knee[1], knee[2]);
                FRAME.transformPosition(BENT.x / 16.0F, BENT.y / 16.0F, BENT.z / 16.0F, POINT);
                add(kx, ky, kz, camera.x + POINT.x, camera.y + POINT.y, camera.z + POINT.z, CREATURE);
                continue;
            }
            FRAME.transformPosition(end[0] / 16.0F, end[1] / 16.0F, end[2] / 16.0F, POINT);
            add(ax, ay, az, camera.x + POINT.x, camera.y + POINT.y, camera.z + POINT.z, CREATURE);
        }
    }

    private static void trunk(EntityModel<?> model, Matrix4f drawn, ModelParts.Part part, ModelBends.Bend waist,
            Vec3 camera) {
        float[] box = part.bounds();
        float[] knee = waist.knee();
        int axis = waist.axis();
        ModelParts.frame(model, drawn, part, FRAME);
        END[0] = knee[0];
        END[1] = knee[1];
        END[2] = knee[2];
        END[axis] = waist.farSign() > 0.0F ? box[axis] : box[axis + 3];
        FRAME.transformPosition(END[0] / 16.0F, END[1] / 16.0F, END[2] / 16.0F, POINT);
        double ax = camera.x + POINT.x;
        double ay = camera.y + POINT.y;
        double az = camera.z + POINT.z;
        FRAME.transformPosition(knee[0] / 16.0F, knee[1] / 16.0F, knee[2] / 16.0F, POINT);
        double kx = camera.x + POINT.x;
        double ky = camera.y + POINT.y;
        double kz = camera.z + POINT.z;
        add(ax, ay, az, kx, ky, kz, CREATURE);
        END[axis] = waist.farSign() > 0.0F ? box[axis + 3] : box[axis];
        BentParts.place(part.part(), END[0], END[1], END[2], BENT);
        FRAME.transformPosition(BENT.x / 16.0F, BENT.y / 16.0F, BENT.z / 16.0F, POINT);
        add(kx, ky, kz, camera.x + POINT.x, camera.y + POINT.y, camera.z + POINT.z, CREATURE);
    }

    private static void add(double ax, double ay, double az, double bx, double by, double bz, int rgb) {
        if (!on || boneCount >= MOST) {
            return;
        }
        if (bones.length < (boneCount + 1) * 7) {
            bones = Arrays.copyOf(bones, bones.length * 2);
        }
        int o = boneCount++ * 7;
        bones[o] = ax;
        bones[o + 1] = ay;
        bones[o + 2] = az;
        bones[o + 3] = bx;
        bones[o + 4] = by;
        bones[o + 5] = bz;
        bones[o + 6] = rgb;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }
        if (boneCount == 0 || !shown()) {
            boneCount = 0;
            return;
        }
        Camera camera = event.getCamera();
        Vec3 eye = camera.getPosition();
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        Matrix4f matrix = event.getPoseStack().last().pose();
        MultiBufferSource.BufferSource buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        VertexConsumer buffer = buffers.getBuffer(LINES);
        for (int i = 0; i < boneCount; i++) {
            int o = i * 7;
            float ax = (float) (bones[o] - eye.x);
            float ay = (float) (bones[o + 1] - eye.y);
            float az = (float) (bones[o + 2] - eye.z);
            float bx = (float) (bones[o + 3] - eye.x);
            float by = (float) (bones[o + 4] - eye.y);
            float bz = (float) (bones[o + 5] - eye.z);
            int rgb = (int) bones[o + 6];
            line(buffer, matrix, ax, ay, az, bx, by, bz, rgb);
            dot(buffer, matrix, ax, ay, az, left, up);
        }
        buffers.endBatch(LINES);
        boneCount = 0;
    }

    // A thin strip from a to b turned to face the eye (the eye is at the origin here).
    private static void line(VertexConsumer buffer, Matrix4f matrix, float ax, float ay, float az, float bx, float by,
            float bz, int rgb) {
        float dx = bx - ax;
        float dy = by - ay;
        float dz = bz - az;
        float mx = (ax + bx) * 0.5F;
        float my = (ay + by) * 0.5F;
        float mz = (az + bz) * 0.5F;
        float sx = dy * mz - dz * my;
        float sy = dz * mx - dx * mz;
        float sz = dx * my - dy * mx;
        float length = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
        if (length < 1.0E-6F) {
            return;
        }
        float away = (float) Math.sqrt(mx * mx + my * my + mz * mz);
        float scale = (float) Math.max(WIDTH, away * WIDTH_AWAY) / length;
        sx *= scale;
        sy *= scale;
        sz *= scale;
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int b = rgb & 0xFF;
        buffer.addVertex(matrix, ax - sx, ay - sy, az - sz).setColor(r, g, b, 230);
        buffer.addVertex(matrix, ax + sx, ay + sy, az + sz).setColor(r, g, b, 230);
        buffer.addVertex(matrix, bx + sx, by + sy, bz + sz).setColor(r, g, b, 230);
        buffer.addVertex(matrix, bx - sx, by - sy, bz - sz).setColor(r, g, b, 230);
    }

    private static void dot(VertexConsumer buffer, Matrix4f matrix, float x, float y, float z, Vector3f left,
            Vector3f up) {
        float d = (float) Math.max(DOT, Math.sqrt(x * x + y * y + z * z) * DOT_AWAY);
        int r = JOINT >> 16 & 0xFF;
        int g = JOINT >> 8 & 0xFF;
        int b = JOINT & 0xFF;
        buffer.addVertex(matrix, x + (left.x + up.x) * d, y + (left.y + up.y) * d, z + (left.z + up.z) * d)
                .setColor(r, g, b, 255);
        buffer.addVertex(matrix, x + (left.x - up.x) * d, y + (left.y - up.y) * d, z + (left.z - up.z) * d)
                .setColor(r, g, b, 255);
        buffer.addVertex(matrix, x - (left.x + up.x) * d, y - (left.y + up.y) * d, z - (left.z + up.z) * d)
                .setColor(r, g, b, 255);
        buffer.addVertex(matrix, x - (left.x - up.x) * d, y - (left.y - up.y) * d, z - (left.z - up.z) * d)
                .setColor(r, g, b, 255);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        on = false;
        boneCount = 0;
    }
}
