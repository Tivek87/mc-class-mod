package nl.tivek.multiversepowers.character.docock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// Doctor Octopus's harness and tentacles drawn from the game's own block textures, as the game's own things are: dark
// netherite segments ringed with iron, three claws round an amber lamp at each tip (red while it squeezes); the harness
// a spine of iron plates down his back with a socket for each tentacle, straps over his chest and a plated belt round
// his waist. What lies right at the first-person eye is left out.
final class TentaclePainter extends LineFrames {
    private static final BlockState BODY = Blocks.NETHERITE_BLOCK.defaultBlockState();
    private static final BlockState STEEL = Blocks.IRON_BLOCK.defaultBlockState();
    private static final BlockState DARK = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
    private static final BlockState LAMP = Blocks.OCHRE_FROGLIGHT.defaultBlockState();
    private static final BlockState HOT = Blocks.REDSTONE_BLOCK.defaultBlockState();

    static final double SEGMENT = 0.3;
    private static final double BASE_WIDTH = 0.24;
    private static final double TIP_WIDTH = 0.12;
    private static final double TAPER = 2.5;
    private static final double NEAR_EYE = 0.55;

    private final PoseStack pose;
    private final MultiBufferSource buffers;
    private final BlockRenderDispatcher blocks;
    private final ClientLevel level;
    private final Vec3 camera;
    private final boolean firstPerson;

    TentaclePainter(PoseStack pose, MultiBufferSource buffers, BlockRenderDispatcher blocks, ClientLevel level,
            Vec3 camera, boolean firstPerson) {
        this.pose = pose;
        this.buffers = buffers;
        this.blocks = blocks;
        this.level = level;
        this.camera = camera;
        this.firstPerson = firstPerson;
    }

    private boolean hidden(Vec3 at) {
        return this.firstPerson && at.distanceToSqr(this.camera) < NEAR_EYE * NEAR_EYE;
    }

    private int light(Vec3 at) {
        return LevelRenderer.getLightColor(this.level, BlockPos.containing(at));
    }

    private static double widthAt(double fromTip) {
        double t = Mth.clamp(fromTip / TAPER, 0.0, 1.0);
        return Mth.lerp(t * t * (3 - 2 * t), TIP_WIDTH, BASE_WIDTH);
    }

    // A block stretched into a box `wide` x `long` x `deep` along (side, d, side x d), from `start` along d.
    private void box(BlockState state, Vec3 start, Vec3 d, Vec3 side, double wide, double length, double deep,
            double roll, int light) {
        if (length < 1.0E-3 || wide < 1.0E-3 || this.hidden(start.add(d.scale(length / 2)))) {
            return;
        }
        Vec3 x = square(side, d);
        if (x == null) {
            x = anySquare(d);
        }
        Vector3f xAxis = new Vector3f((float) x.x, (float) x.y, (float) x.z);
        Vector3f yAxis = new Vector3f((float) d.x, (float) d.y, (float) d.z);
        Vector3f zAxis = new Vector3f(xAxis).cross(yAxis);
        this.pose.pushPose();
        this.pose.translate(start.x - this.camera.x, start.y - this.camera.y, start.z - this.camera.z);
        this.pose.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(xAxis, yAxis, zAxis)));
        this.pose.mulPose(Axis.YP.rotation((float) roll));
        this.pose.scale((float) wide, (float) length, (float) deep);
        this.pose.translate(-0.5F, 0.0F, -0.5F);
        this.blocks.renderSingleBlock(state, this.pose, this.buffers, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
        this.pose.popPose();
    }

    private void bar(BlockState state, Vec3 start, Vec3 d, Vec3 side, double length, double width, double roll,
            int light) {
        this.box(state, start, d, side, width, length, width, roll, light);
    }

    private void cube(BlockState state, Vec3 at, Vec3 d, Vec3 side, double size, int light) {
        this.bar(state, at.subtract(d.scale(size / 2)), d, side, size, size, 0.0, light);
    }

    // A tentacle along `points`, socket first: segments thinning to the tip, every third ringed, its claw `open`
    // (0 shut, 1 spread wide).
    void tentacle(List<Vec3> points, double open, boolean hot) {
        if (points.size() < 2) {
            return;
        }
        Frames frames = new Frames(points, new Vec3(0.0, 1.0, 0.0));
        double length = frames.length;
        if (length < 1.0E-3) {
            return;
        }
        List<Double> joints = new ArrayList<>();
        joints.add(0.0);
        for (int m = (int) Math.ceil(length / SEGMENT) - 1; m >= 1; m--) {
            joints.add(length - m * SEGMENT);
        }
        joints.add(length);
        for (int j = 0; j < joints.size() - 1; j++) {
            double s0 = joints.get(j);
            double s1 = joints.get(j + 1);
            Vec3 a = frames.pointAt(s0);
            Vec3 b = frames.pointAt(s1);
            Vec3 delta = b.subtract(a);
            double span = delta.length();
            if (span < 1.0E-3) {
                continue;
            }
            Vec3 d = delta.scale(1.0 / span);
            Vec3 side = frames.sideAt((s0 + s1) / 2);
            int id = (int) Math.floor((length - (s0 + s1) / 2) / SEGMENT);
            double width = widthAt(length - (s0 + s1) / 2);
            int light = this.light(a.lerp(b, 0.5));
            this.bar(BODY, a, d, side, span * 1.04, width, id * 0.35, light);
            if (Math.floorMod(id, 3) == 0) {
                double ring = 0.06 + width * 0.2;
                this.bar(STEEL, a.subtract(d.scale(ring / 2)), d, side, ring, width * 1.18, 0.0, light);
            }
        }
        Vec3 tip = points.get(points.size() - 1);
        this.claw(tip, frames.directionAt(length), frames.sideAt(length), open, hot ? HOT : LAMP);
    }

    private void claw(Vec3 tip, Vec3 d, Vec3 side, double open, BlockState lamp) {
        Vec3 across = d.cross(side);
        int light = this.light(tip);
        this.bar(BODY, tip.subtract(d.scale(0.06)), d, side, 0.22, 0.24, 0.0, light);
        this.cube(lamp, tip.add(d.scale(0.18)), d, side, 0.1, LightTexture.FULL_BRIGHT);
        double spread = Mth.clamp(open, 0.0, 1.2);
        for (int k = 0; k < 3; k++) {
            double angle = k * Math.PI * 2 / 3;
            Vec3 out = side.scale(Math.cos(angle)).add(across.scale(Math.sin(angle)));
            Vec3 base = tip.add(d.scale(0.13)).add(out.scale(0.09));
            Vec3 knuckle = base.add(d.scale(0.22)).add(out.scale(0.05 + spread * 0.4));
            Vec3 point = knuckle.add(d.scale(0.2)).subtract(out.scale(spread * 0.27 + 0.04));
            this.bar(STEEL, base, knuckle.subtract(base).normalize(), out, base.distanceTo(knuckle), 0.07, 0.0, light);
            this.bar(BODY, knuckle, point.subtract(knuckle).normalize(), out, knuckle.distanceTo(point), 0.05, 0.0,
                    light);
            this.cube(lamp, knuckle, d, out, 0.065, LightTexture.FULL_BRIGHT);
        }
    }

    // The harness on his body, as it stands or crouches.
    void harness(TentacleShapes.Body body) {
        Vec3 up = body.up();
        Vec3 left = body.left();
        Vec3 back = body.forward().scale(-1.0);
        int light = this.light(body.at(0.0, 1.1, 0.0));
        // The spine: five plates from the neck down to the small of the back.
        for (int k = 0; k < 5; k++) {
            double y = 1.42 - k * 0.15;
            double wide = k == 0 || k == 4 ? 0.2 : 0.26;
            this.box(STEEL, body.at(0.0, y - 0.065, -0.15), up, left, wide, 0.13, 0.06, 0.0, light);
        }
        this.box(DARK, body.at(0.0, 0.8, -0.13), up, left, 0.08, 0.62, 0.04, 0.0, light);
        // The four sockets the tentacles come out of, each with its lamp.
        for (int i = 0; i < 4; i++) {
            Vec3 socket = body.socket(i);
            this.cube(DARK, socket, back, left, 0.17, light);
            this.cube(LAMP, socket.add(back.scale(0.09)), back, left, 0.06, LightTexture.FULL_BRIGHT);
        }
        // Straps over the shoulders down to the belt, front and back.
        for (int s = -1; s <= 1; s += 2) {
            Vec3 shoulder = body.at(s * 0.17, 1.47, 0.0);
            this.box(STEEL, body.at(s * 0.15, 0.82, 0.15), up, left, 0.06, 0.62, 0.03, 0.0, light);
            this.box(STEEL, shoulder.add(back.scale(0.15)).subtract(up.scale(0.02)), back.scale(-1.0), left, 0.06,
                    0.3, 0.04, 0.0, light);
        }
        // The belt round the waist: plates front, back and on either side, a buckle before.
        this.box(STEEL, body.at(0.0, 0.72, 0.15), up, left, 0.56, 0.13, 0.04, 0.0, light);
        this.box(STEEL, body.at(0.0, 0.72, -0.15), up, left, 0.56, 0.13, 0.04, 0.0, light);
        for (int s = -1; s <= 1; s += 2) {
            this.box(STEEL, body.at(s * 0.27, 0.72, 0.0), up, back, 0.32, 0.13, 0.04, 0.0, light);
        }
        this.box(DARK, body.at(0.0, 0.7, 0.18), up, left, 0.16, 0.17, 0.04, 0.0, light);
        this.cube(LAMP, body.at(0.0, 0.785, 0.2), up, left, 0.05, LightTexture.FULL_BRIGHT);
    }
}
