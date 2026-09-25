package nl.tivek.multiversepowers.character.docock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import nl.tivek.multiversepowers.character.docock.ArmPayload;
import nl.tivek.multiversepowers.character.docock.PortalPayload;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the robot tentacles, the tech portals and the energy shield as real 3D models, built from
 * scaled block models: slim dark segments with thin steel rings, small swept-back spikes, and a
 * three-fingered claw with glowing lamps.
 *
 * <p>Every piece is turned with a frame that is carried smoothly along the tentacle (never recomputed
 * from a fixed "up"), so nothing flips or spins when one points straight up or down. Segments are
 * counted from the tip, so they slide along with the tentacle instead of jumping when it grows.
 */
final class ArmPainter extends LineFrames {
    private static final BlockState BODY = Blocks.NETHERITE_BLOCK.defaultBlockState();
    private static final BlockState STEEL = Blocks.IRON_BLOCK.defaultBlockState();
    private static final BlockState LAMP = Blocks.OCHRE_FROGLIGHT.defaultBlockState();
    private static final BlockState RAGE_LAMP = Blocks.REDSTONE_BLOCK.defaultBlockState();
    private static final BlockState HOT = Blocks.SHROOMLIGHT.defaultBlockState();
    private static final BlockState DIM = Blocks.POLISHED_BLACKSTONE.defaultBlockState();

    // Short, slim segments: the tentacle reads as one smooth tube instead of a row of boxes.
    private static final double SEGMENT = 0.3;
    private static final double BASE_WIDTH = 0.27;
    private static final double TIP_WIDTH = 0.13;
    // Over this length before the tip, the tentacle thins from BASE_WIDTH to TIP_WIDTH.
    private static final double TAPER = 2.5;
    // In first person, pieces this close to the eye are left out instead of filling the screen.
    private static final double NEAR_EYE = 0.55;

    private static final int ENERGY_CORE = 0xBFF6FF;
    private static final int ENERGY = 0x3AD8FF;
    private static final int SHIELD_CORE = 0xDFFBFF;
    private static final int SHIELD = 0x2FA8FF;

    private final PoseStack pose;
    private final MultiBufferSource buffers;
    private final BlockRenderDispatcher blocks;
    private final ClientLevel level;
    private final Vec3 camera;
    private final boolean firstPerson;
    private final float time;
    private final List<Glow> glows = new ArrayList<>();
    @Nullable
    private Vec3 clipPoint;
    private Vec3 clipNormal = Vec3.ZERO;

    ArmPainter(PoseStack pose, MultiBufferSource buffers, BlockRenderDispatcher blocks, ClientLevel level,
            Vec3 camera, boolean firstPerson, float time) {
        this.pose = pose;
        this.buffers = buffers;
        this.blocks = blocks;
        this.level = level;
        this.camera = camera;
        this.firstPerson = firstPerson;
        this.time = time;
    }

    /**
     * A glowing energy field, drawn after all solid pieces: {@code radius} wide along {@code side} and
     * {@code radius2} across it.
     */
    private record Glow(Vec3 base, Vec3 direction, Vec3 side, double radius, double radius2, int inner,
            int outer, double strength, double phase, boolean shield) {
    }

    /** From now on, leave out everything past this plane (null: nothing). */
    void clip(@Nullable Vec3 point, Vec3 normal) {
        this.clipPoint = point;
        this.clipNormal = normal;
    }

    private boolean hidden(Vec3 at) {
        if (this.clipPoint != null && at.subtract(this.clipPoint).dot(this.clipNormal) > 0.0) {
            return true;
        }
        return this.firstPerson && at.distanceToSqr(this.camera) < NEAR_EYE * NEAR_EYE;
    }

    private int light(Vec3 at) {
        return LevelRenderer.getLightColor(this.level, BlockPos.containing(at));
    }

    private static double widthAt(double fromTip, double thickness) {
        double t = Mth.clamp(fromTip / TAPER, 0.0, 1.0);
        return Mth.lerp(t * t * (3 - 2 * t), TIP_WIDTH, BASE_WIDTH) * thickness;
    }

    // ---- Pieces ----

    /**
     * One block model stretched into a bar: from {@code start}, {@code length} long along the unit
     * vector {@code d}, {@code width} thick, its faces turned to {@code side} and then {@code roll}
     * radians further around its own length.
     */
    private void bar(BlockState state, Vec3 start, Vec3 d, Vec3 side, double length, double width, double roll,
            int light) {
        if (length < 1.0E-3 || width < 1.0E-3 || this.hidden(start.add(d.scale(length / 2)))) {
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
        this.pose.scale((float) width, (float) length, (float) width);
        this.pose.translate(-0.5F, 0.0F, -0.5F);
        this.blocks.renderSingleBlock(state, this.pose, this.buffers, light, OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY, null);
        this.pose.popPose();
    }

    /** A small cube centred on {@code at}. */
    private void cube(BlockState state, Vec3 at, Vec3 d, Vec3 side, double size, int light) {
        this.bar(state, at.subtract(d.scale(size / 2)), d, side, size, size, 0.0, light);
    }

    // ---- Tentacles ----

    /**
     * Short segments that thin towards the tip, a steel ring every third joint, small swept-back spikes
     * every fourth, and the claw when the tip is on this part of the tentacle.
     */
    void arm(List<Vec3> points, @Nullable Vec3 reference, float claw, float thickness, float tipOffset,
            boolean rage, float spike, float thrust, List<ArmPayload.Carried> carried) {
        if (points.size() < 2) {
            return;
        }
        Frames frames = new Frames(points, reference);
        double length = frames.length;
        if (length < 1.0E-3) {
            return;
        }
        double segment = SEGMENT * thickness;
        double end = length + tipOffset;
        // Joints sit a whole number of segments back from the real tip (which may be past a portal).
        List<Double> joints = new ArrayList<>();
        joints.add(0.0);
        int far = (int) Math.ceil(end / segment) - 1;
        int near = (int) Math.floor(tipOffset / segment) + 1;
        for (int m = far; m >= near; m--) {
            double s = end - m * segment;
            if (s > 1.0E-3 && s < length - 1.0E-3) {
                joints.add(s);
            }
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
            double middle = (s0 + s1) / 2;
            // Counted from the real tip, so each segment keeps its look while the tentacle moves.
            int id = (int) Math.floor((end - middle) / segment);
            double width = widthAt(end - middle, thickness);
            int light = this.light(a.lerp(b, 0.5));
            this.bar(BODY, a, d, side, span * 1.04, width, id * 0.35, light);
            if (Math.floorMod(id, 3) == 0) {
                // A thin ring over the joint, so bends never show a gap.
                double ring = 0.07 * thickness + width * 0.2;
                this.bar(STEEL, a.subtract(d.scale(ring / 2)), d, side, ring, width * 1.18, 0.0, light);
            }
            if (Math.floorMod(id, 4) == 2) {
                Vec3 across = d.cross(side);
                for (int k = -1; k <= 1; k += 2) {
                    Vec3 out = across.scale(k);
                    Vec3 barb = out.subtract(d.scale(0.8)).normalize();
                    this.bar(STEEL, a.add(d.scale(span * 0.5)).add(out.scale(width * 0.45)), barb, d,
                            0.17 * thickness, 0.05 * thickness, 0.0, light);
                }
            }
        }
        if (tipOffset < 0.01F) {
            Vec3 tip = points.get(points.size() - 1);
            Vec3 d = frames.directionAt(length);
            Vec3 side = frames.sideAt(length);
            if (claw >= 0.0F) {
                this.claw(tip, d, side, claw, thickness, rage ? RAGE_LAMP : LAMP);
            }
            if (spike > 0.001F) {
                this.spike(tip, d, side, spike, thickness);
            }
            if (thrust > 0.001F) {
                this.thrusters(tip, d, side, thrust, thickness);
            }
            if (!carried.isEmpty()) {
                this.carried(tip, carried);
            }
        }
    }

    /**
     * The sharp point of the Portal ability: it slides out of the middle of the claw, between the
     * three fingers, and ends in a needle.
     */
    private void spike(Vec3 tip, Vec3 d, Vec3 side, double out, float thickness) {
        int light = this.light(tip);
        double length = (0.2 + 0.85 * out) * thickness;
        Vec3 base = tip.add(d.scale(0.1 * thickness));
        this.bar(STEEL, base, d, side, length * 0.72, 0.085 * thickness, 0.0, light);
        this.bar(DIM, base.add(d.scale(length * 0.72)), d, side, length * 0.28, 0.045 * thickness, 0.0, light);
        this.cube(HOT, base.add(d.scale(length * 0.6)), d, side, 0.05 * thickness, LightTexture.FULL_BRIGHT);
    }

    /**
     * The thrusters of the Portal ability: two slim pods lie along the tentacle a little behind the
     * claw, with a short flame licking backwards out of each. Small on purpose, so the tentacle keeps
     * its own shape instead of turning into a bundle of glowing bars.
     */
    private void thrusters(Vec3 tip, Vec3 d, Vec3 side, double out, float thickness) {
        Vec3 across = d.cross(side);
        Vec3 back = d.scale(-1);
        int light = this.light(tip);
        double grown = ease(Mth.clamp(out, 0.0, 1.0));
        for (int k = -1; k <= 1; k += 2) {
            Vec3 outward = across.scale(k);
            Vec3 root = tip.subtract(d.scale(1.05 * thickness))
                    .add(outward.scale((0.05 + 0.06 * grown) * thickness));
            // The pod lies flat against the tentacle; only its nozzle sticks out at the back.
            this.bar(DIM, root, back, outward, 0.3 * thickness, 0.09 * thickness, 0.0, light);
            this.bar(STEEL, root.add(back.scale(0.3 * thickness)), back, outward, 0.05 * thickness,
                    0.11 * thickness, 0.0, light);
            double flicker = 0.8 + 0.2 * Math.sin(this.time * 1.3 + k);
            double flame = 0.42 * thickness * grown * flicker;
            if (flame > 0.01) {
                this.bar(HOT, root.add(back.scale(0.35 * thickness)), back, outward, flame,
                        0.06 * thickness, 0.0, LightTexture.FULL_BRIGHT);
            }
        }
    }

    /** The blocks a tentacle carries, each one whole, exactly as it stood in the world. */
    private void carried(Vec3 tip, List<ArmPayload.Carried> carried) {
        for (ArmPayload.Carried block : carried) {
            BlockState state = Block.stateById(block.state());
            if (state.isAir()) {
                continue;
            }
            Vec3 at = tip.add(block.offset());
            Vec3 corner = at.subtract(0.5, 0.5, 0.5);
            if (this.hidden(at)) {
                continue;
            }
            this.pose.pushPose();
            this.pose.translate(corner.x - this.camera.x, corner.y - this.camera.y, corner.z - this.camera.z);
            this.blocks.renderSingleBlock(state, this.pose, this.buffers, this.light(at),
                    OverlayTexture.NO_OVERLAY, ModelData.EMPTY, null);
            this.pose.popPose();
        }
    }

    /** A slim hub with a glowing eye, and three curved two-part fingers with a lamp at each knuckle. */
    private void claw(Vec3 tip, Vec3 d, Vec3 side, double open, float thickness, BlockState lamp) {
        Vec3 across = d.cross(side);
        int light = this.light(tip);
        this.bar(BODY, tip.subtract(d.scale(0.06 * thickness)), d, side, 0.24 * thickness, 0.26 * thickness, 0.0,
                light);
        this.cube(lamp, tip.add(d.scale(0.2 * thickness)), d, side, 0.11 * thickness, LightTexture.FULL_BRIGHT);
        double spread = Mth.clamp(open, 0.0, 1.2);
        for (int k = 0; k < 3; k++) {
            double angle = k * Math.PI * 2 / 3;
            Vec3 out = side.scale(Math.cos(angle)).add(across.scale(Math.sin(angle)));
            Vec3 base = tip.add(d.scale(0.14 * thickness)).add(out.scale(0.1 * thickness));
            Vec3 knuckle = base.add(d.scale(0.24 * thickness)).add(out.scale(0.06 * thickness + spread * 0.45));
            Vec3 point = knuckle.add(d.scale(0.22 * thickness)).subtract(out.scale(spread * 0.3 + 0.04));
            this.bar(STEEL, base, knuckle.subtract(base).normalize(), out, base.distanceTo(knuckle),
                    0.075 * thickness, 0.0, light);
            this.bar(BODY, knuckle, point.subtract(knuckle).normalize(), out, knuckle.distanceTo(point),
                    0.055 * thickness, 0.0, light);
            this.cube(lamp, knuckle, d, out, 0.07 * thickness, LightTexture.FULL_BRIGHT);
        }
    }

    // ---- Portals and shields ----

    /**
     * A tech portal, played from {@code open} (0 shut, 1 open): first the steel ring assembles, its
     * segments flying in one after another while the ring turns; then clamps lock on and the lamps
     * light up one by one; last the energy tears open, from a thin bright line to a full swirling
     * field with a tunnel going back into it. A shield is only the energy: a dome of light in front of
     * you. Closing plays it all backwards.
     */
    void portal(Vec3 center, Vec3 normal, double size, double open, int style) {
        if (open <= 0.001 || size < 0.05) {
            return;
        }
        Vec3 n = normal.normalize();
        Vec3 e1 = anySquare(n);
        if (style == PortalPayload.STYLE_SHIELD) {
            this.shield(center, n, e1, size, open);
            return;
        }
        Vec3 e2 = n.cross(e1);
        int light = this.light(center);
        double frame = 0.16 + 0.06 * size;
        double ring = Mth.clamp(open / 0.45, 0.0, 1.0);
        double lamps = Mth.clamp((open - 0.4) / 0.3, 0.0, 1.0);
        double energy = Mth.clamp((open - 0.55) / 0.45, 0.0, 1.0);
        double spin = this.time * 0.01 + (1.0 - ease(ring)) * 2.5;

        int count = Math.max(16, (int) (size * 12));
        for (int i = 0; i < count; i++) {
            double arrive = Mth.clamp((ring - 0.8 * i / count) / 0.2, 0.0, 1.0);
            if (arrive <= 0.0) {
                continue;
            }
            double e = ease(arrive);
            double radius = size * (1.0 + 0.8 * (1.0 - e));
            double a0 = spin + Math.PI * 2 * i / count + (1.0 - e) * 1.2;
            double a1 = a0 + Math.PI * 2 / count;
            Vec3 p0 = around(center, e1, e2, a0, radius);
            Vec3 p1 = around(center, e1, e2, a1, radius);
            Vec3 d = p1.subtract(p0).normalize();
            double width = frame * (0.4 + 0.6 * e);
            this.bar(BODY, p0.subtract(d.scale(width * 0.3)), d, n, p0.distanceTo(p1) + width * 0.6, width, 0.0,
                    light);
            if (e > 0.5) {
                Vec3 q0 = center.lerp(p0, 0.93);
                Vec3 q1 = center.lerp(p1, 0.93);
                this.bar(STEEL, q0, q1.subtract(q0).normalize(), n, q0.distanceTo(q1) * 1.05, width * 0.45, 0.0,
                        light);
            }
        }
        if (ring >= 1.0) {
            // Toothed outer ring, turning the other way.
            for (int k = 0; k < 24; k++) {
                Vec3 out = around(Vec3.ZERO, e1, e2, -this.time * 0.02 + Math.PI * 2 * k / 24, 1.0);
                this.cube(STEEL, center.add(out.scale(size + frame * 0.6)), n, out, frame * 0.35, light);
            }
        }
        double clamps = ease(Mth.clamp((ring - 0.7) / 0.3, 0.0, 1.0));
        if (clamps > 0.0) {
            for (int k = 0; k < 3; k++) {
                Vec3 out = around(Vec3.ZERO, e1, e2, spin + Math.PI / 2 + k * Math.PI * 2 / 3, 1.0);
                double at = size - frame * 0.4 + (1.0 - clamps) * 1.2;
                this.bar(STEEL, center.add(out.scale(at)), out, n, frame * 1.5, frame * 1.4, 0.0, light);
            }
        }
        int blink = (int) (this.time / 4);
        for (int k = 0; k < 6 && ring >= 0.95; k++) {
            Vec3 out = around(Vec3.ZERO, e1, e2, spin + k * Math.PI / 3 + Math.PI / 6, 1.0);
            // Lit one by one while opening; once open they blink in turns.
            boolean lit = open >= 0.999 ? (k + blink) % 2 == 0 : lamps > (k + 1) / 6.5;
            for (int face = -1; face <= 1; face += 2) {
                this.cube(lit ? HOT : DIM, center.add(out.scale(size)).add(n.scale(face * frame * 0.55)), n, out,
                        frame * 0.5, lit ? LightTexture.FULL_BRIGHT : light);
            }
        }
        if (energy <= 0.0) {
            return;
        }
        // Tears open as a thin line first, then widens into a disc; brightest while tearing.
        double wide = size * 0.97 * ease(Math.min(1.0, energy * 2.5));
        double tall = size * 0.97 * Math.max(0.04, ease(energy));
        double flash = 1.0 + 0.8 * (1.0 - energy);
        // A tunnel of fainter, smaller fields behind it. Farthest first: glowing fields hide what is
        // drawn behind them afterwards.
        double away = this.camera.subtract(center).dot(n) > 0.0 ? -1.0 : 1.0;
        for (int layer = 3; layer >= 1; layer--) {
            double shrink = 1.0 - 0.2 * layer;
            this.glows.add(new Glow(center.add(n.scale(away * 0.22 * layer * size * 0.5)), n, e1, wide * shrink,
                    tall * shrink, ENERGY_CORE, ENERGY, flash * Math.pow(0.55, layer), layer * 0.9, false));
        }
        this.glows.add(new Glow(center, n, e1, wide, tall, ENERGY_CORE, ENERGY, flash, 0.0, false));
    }

    /** The Block shield: a ring of light that flares open, with ripples running out of the middle. */
    private void shield(Vec3 center, Vec3 n, Vec3 e1, double size, double open) {
        double grown = ease(Mth.clamp(open, 0.0, 1.0));
        this.glows.add(new Glow(center, n, e1, size * grown, size * grown, SHIELD_CORE, SHIELD, 0.85 + 0.4 * grown,
                0.0, true));
    }

    private static Vec3 around(Vec3 center, Vec3 e1, Vec3 e2, double angle, double radius) {
        return center.add(e1.scale(Math.cos(angle) * radius)).add(e2.scale(Math.sin(angle) * radius));
    }

    private static double ease(double t) {
        return t * t * (3 - 2 * t);
    }

    // ---- Glowing parts ----

    /** Draws every energy field of this frame, glowing (added on top of what is behind). */
    void finish() {
        if (this.glows.isEmpty()) {
            return;
        }
        this.clipPoint = null;
        VertexConsumer buffer = this.buffers.getBuffer(RenderType.lightning());
        Matrix4f matrix = this.pose.last().pose();
        for (Glow glow : this.glows) {
            this.disc(buffer, matrix, glow);
        }
        this.glows.clear();
    }

    private void disc(VertexConsumer buffer, Matrix4f matrix, Glow glow) {
        Vec3 n = glow.direction;
        Vec3 e1 = glow.side;
        Vec3 e2 = n.cross(e1);
        int rings = 6;
        int slices = 36;
        for (int r = 0; r < rings; r++) {
            double r0 = (double) r / rings;
            double r1 = (double) (r + 1) / rings;
            for (int i = 0; i < slices; i++) {
                double a0 = Math.PI * 2 * i / slices;
                double a1 = Math.PI * 2 * (i + 1) / slices;
                this.quad(buffer, matrix,
                        discPoint(glow, e1, e2, r0, a0), this.color(glow, r0, a0), this.alpha(glow, r0, a0),
                        discPoint(glow, e1, e2, r1, a0), this.color(glow, r1, a0), this.alpha(glow, r1, a0),
                        discPoint(glow, e1, e2, r1, a1), this.color(glow, r1, a1), this.alpha(glow, r1, a1),
                        discPoint(glow, e1, e2, r0, a1), this.color(glow, r0, a1), this.alpha(glow, r0, a1));
            }
        }
    }

    private static Vec3 discPoint(Glow glow, Vec3 e1, Vec3 e2, double r, double a) {
        return glow.base.add(e1.scale(Math.cos(a) * r * glow.radius)).add(e2.scale(Math.sin(a) * r * glow.radius2));
    }

    /** Portals: three spiral arms turning inwards. Shields: ripples running out of the middle. */
    private double pattern(Glow glow, double r, double a) {
        if (glow.shield) {
            return 0.5 + 0.5 * Math.sin(r * 14 - this.time * 0.5 + Math.sin(a * 3) * 0.6);
        }
        return 0.5 + 0.5 * Math.sin(3 * a - 8 * r + this.time * 0.35 + glow.phase);
    }

    private int color(Glow glow, double r, double a) {
        double mix = Mth.clamp(this.pattern(glow, r, a) * (1.0 - r * 0.5) + (1.0 - r) * 0.3, 0.0, 1.0);
        return mixColor(glow.outer, glow.inner, mix);
    }

    private int alpha(Glow glow, double r, double a) {
        double rim = r > 0.85 ? (r - 0.85) / 0.15 * (glow.shield ? 0.5 : 0.35) : 0.0;
        double base = glow.shield ? 0.1 : 0.22;
        double alpha = (base + 0.4 * this.pattern(glow, r, a) * (1.0 - r * 0.6) + rim) * glow.strength;
        return (int) (255 * Mth.clamp(alpha, 0.0, 1.0));
    }

    private static int mixColor(int from, int to, double t) {
        int r = (int) Mth.lerp(t, from >> 16 & 0xFF, to >> 16 & 0xFF);
        int g = (int) Mth.lerp(t, from >> 8 & 0xFF, to >> 8 & 0xFF);
        int b = (int) Mth.lerp(t, from & 0xFF, to & 0xFF);
        return r << 16 | g << 8 | b;
    }

    /** One quad, drawn from both sides. */
    private void quad(VertexConsumer buffer, Matrix4f matrix, Vec3 p0, int c0, int a0, Vec3 p1, int c1, int a1,
            Vec3 p2, int c2, int a2, Vec3 p3, int c3, int a3) {
        if (this.hidden(p0.add(p1).add(p2).add(p3).scale(0.25))) {
            return;
        }
        this.vertex(buffer, matrix, p0, c0, a0);
        this.vertex(buffer, matrix, p1, c1, a1);
        this.vertex(buffer, matrix, p2, c2, a2);
        this.vertex(buffer, matrix, p3, c3, a3);
        this.vertex(buffer, matrix, p3, c3, a3);
        this.vertex(buffer, matrix, p2, c2, a2);
        this.vertex(buffer, matrix, p1, c1, a1);
        this.vertex(buffer, matrix, p0, c0, a0);
    }

    private void vertex(VertexConsumer buffer, Matrix4f matrix, Vec3 p, int rgb, int alpha) {
        buffer.addVertex(matrix, (float) (p.x - this.camera.x), (float) (p.y - this.camera.y),
                (float) (p.z - this.camera.z)).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha);
    }
}
