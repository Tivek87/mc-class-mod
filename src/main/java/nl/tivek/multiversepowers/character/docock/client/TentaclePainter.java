package nl.tivek.multiversepowers.character.docock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.render.LitShapes;
import nl.tivek.multiversepowers.engine.client.render.LitShapes.Ring;
import nl.tivek.multiversepowers.engine.client.render.LitShapes.Skin;

// Doctor Octopus's harness and tentacles, a model of their own in the metal of `OctoSkin`, as in the films: each
// tentacle a thick black hose ringed with dark steel, always TentacleReach.LENGTH long (TentacleShapes), thinning a
// little to a claw of three long hooked talons (three short ones inside them) round a red lamp; on his back a spine
// of steel vertebrae from his neck down to a corset of five steel bands round his belly, the tentacles coming out of
// round mounts on its back. Drawn twice: the metal, then the lamps' light (`phase`). What lies right at the
// first-person eye is left out.
final class TentaclePainter extends LineFrames {
    private static final double BASE_RADIUS = 0.21;
    private static final double TIP_RADIUS = 0.14;
    private static final double TAPER = 4.0;
    // One rubber stretch and one steel ring, and the share of it the rubber takes.
    private static final double RIB = 0.3;
    private static final double[] RIB_AT = { 0.0, 0.5, 0.58, 0.92, 1.0 };
    private static final double[] RIB_SIZE = { 0.92, 0.92, 1.12, 1.12, 0.92 };
    // The claw is drawn this many times its first size, to suit the thicker hose.
    private static final double CLAW = 2.0;
    private static final double WRIST = 0.1 * CLAW;
    private static final int SIDES = 12;
    private static final double CORSET_LOW = 0.72;
    private static final double CORSET_STEP = 0.068;
    private static final double NEAR_EYE = 0.55;

    private final LitShapes shapes;
    private final ClientLevel level;
    private final Vec3 camera;
    private final boolean firstPerson;
    private boolean glow;

    TentaclePainter(PoseStack pose, ClientLevel level, Vec3 camera, boolean firstPerson) {
        this.shapes = new LitShapes(pose.last(), camera, null);
        this.level = level;
        this.camera = camera;
        this.firstPerson = firstPerson;
    }

    // Draws the metal into `buffer` from here on, or with `glow` only the lamps' light.
    void phase(VertexConsumer buffer, boolean glow) {
        this.shapes.into(buffer);
        this.glow = glow;
    }

    private boolean hidden(Vec3 at) {
        return this.firstPerson && at.distanceToSqr(this.camera) < NEAR_EYE * NEAR_EYE;
    }

    private int light(Vec3 at) {
        return LevelRenderer.getLightColor(this.level, BlockPos.containing(at));
    }

    private static double radiusAt(double fromTip) {
        double t = Mth.clamp(fromTip / TAPER, 0.0, 1.0);
        return Mth.lerp(t * t * (3 - 2 * t), TIP_RADIUS, BASE_RADIUS);
    }

    // A ring square to `d`, its first axis as close to `side` as it can be.
    private static Ring across(Vec3 center, Vec3 d, Vec3 side, double ra, double rb) {
        Vec3 a = square(side, d);
        if (a == null) {
            a = anySquare(d);
        }
        return new Ring(center, a, d.cross(a), ra, rb, 2.0);
    }

    private void band(Ring from, Ring to, int sides, double twist, Skin skin, int light) {
        if (!this.glow && !this.hidden(from.center().lerp(to.center(), 0.5))) {
            this.shapes.band(from, to, sides, twist, skin, light, light);
        }
    }

    private void box(Vec3 center, Vec3 x, Vec3 y, double hx, double hy, double hz, Skin skin, int light) {
        if (!this.glow && !this.hidden(center)) {
            this.shapes.box(center, x, y, x.cross(y), hx, hy, hz, skin, light);
        }
    }

    // A lamp: opaque red with the metal, its light over it in the glow phase.
    private void lamp(Vec3 center, Vec3 x, Vec3 y, double half, float bright) {
        if (this.hidden(center)) {
            return;
        }
        if (this.glow) {
            int c = Mth.clamp((int) (255 * bright), 0, 255);
            this.shapes.color(0xFF000000 | c << 16 | c << 8 | c);
            this.shapes.box(center, x, y, x.cross(y), half * 1.12, half * 1.12, half * 1.12, OctoSkin.LAMP,
                    LightTexture.FULL_BRIGHT);
            this.shapes.color(0xFFFFFFFF);
        } else {
            this.shapes.box(center, x, y, x.cross(y), half, half, half, OctoSkin.LAMP, LightTexture.FULL_BRIGHT);
        }
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
        double end = Math.max(0.0, length - WRIST);
        Ring last = null;
        int ribs = Math.max(1, (int) Math.ceil(end / RIB));
        for (int r = 0; r < ribs; r++) {
            double start = r * RIB;
            int light = this.light(frames.pointAt(start));
            for (int k = 0; k < RIB_AT.length; k++) {
                double s = Math.min(end, start + RIB_AT[k] * RIB);
                double radius = radiusAt(length - s) * RIB_SIZE[k];
                Ring ring = across(frames.pointAt(s), frames.smoothDirectionAt(s), frames.smoothSideAt(s), radius,
                        radius);
                if (last != null && k > 0) {
                    Skin skin = k == 1 ? OctoSkin.RUBBER : k == 2 ? OctoSkin.RING.rows(0.0F, 0.3F)
                            : k == 3 ? OctoSkin.RING : OctoSkin.RING.rows(0.7F, 1.0F);
                    this.band(last, ring, SIDES, 0.0, skin, light);
                }
                last = ring;
                if (s >= end) {
                    break;
                }
            }
        }
        this.claw(frames.pointAt(end), frames.smoothDirectionAt(length), frames.smoothSideAt(length),
                radiusAt(WRIST), open, hot);
    }

    // The claw at the hose's end `base`: a steel wrist, a six-sided housing with the lamp in its face, three long
    // talons hooking in at their points with a light on each knuckle, three short ones between them.
    private void claw(Vec3 base, Vec3 d, Vec3 side, double radius, double open, boolean hot) {
        Vec3 across = d.cross(side);
        int light = this.light(base);
        double c = CLAW;
        Vec3 wrist = base.add(d.scale(WRIST));
        this.band(across(base, d, side, radius * 1.3, radius * 1.3), across(base.add(d.scale(0.04 * c)), d, side,
                radius * 1.5, radius * 1.5), SIDES, 0.0, OctoSkin.STEEL, light);
        this.band(across(base.add(d.scale(0.04 * c)), d, side, radius * 1.5, radius * 1.5), across(wrist, d, side,
                0.085 * c, 0.085 * c), 6, Math.PI / 6, OctoSkin.STEEL, light);
        Ring face = across(wrist.add(d.scale(0.12 * c)), d, side, 0.06 * c, 0.06 * c);
        this.band(across(wrist, d, side, 0.085 * c, 0.085 * c), face, 6, Math.PI / 6, OctoSkin.DARK, light);
        if (!this.glow && !this.hidden(face.center())) {
            this.shapes.cap(face, 6, Math.PI / 6, d, OctoSkin.DARK, light);
        }
        float time = this.level.getGameTime() % 1000L;
        float bright = hot ? 0.85F + 0.15F * Mth.sin(time * 1.3F) : 0.6F;
        this.lamp(face.center().add(d.scale(0.02 * c)), side, d, 0.035 * c, bright);
        double spread = Mth.clamp(open, 0.0, 1.2);
        for (int k = 0; k < 3; k++) {
            double angle = k * Math.PI * 2 / 3;
            Vec3 out = side.scale(Math.cos(angle)).add(across.scale(Math.sin(angle)));
            Vec3 root = wrist.add(d.scale(0.04 * c)).add(out.scale(0.07 * c));
            Vec3 knuckle = root.add(d.scale(0.2 * c)).add(out.scale((0.06 + spread * 0.36) * c));
            Vec3 point = knuckle.add(d.scale(0.22 * c)).subtract(out.scale((spread * 0.25 + 0.07) * c));
            this.talon(root, knuckle, out, 0.036 * c, 0.056 * c, 0.03 * c, 0.048 * c, OctoSkin.DARK, light);
            this.talon(knuckle, point, out, 0.03 * c, 0.048 * c, 0.004 * c, 0.008 * c, OctoSkin.STEEL, light);
            this.box(knuckle, out, d, 0.04 * c, 0.04 * c, 0.04 * c, OctoSkin.STEEL, light);
            this.lamp(knuckle.add(out.scale(0.042 * c)), out, d, 0.018 * c, bright);
            Vec3 inner = side.scale(Math.cos(angle + Math.PI / 3)).add(across.scale(Math.sin(angle + Math.PI / 3)));
            Vec3 innerRoot = wrist.add(d.scale(0.1 * c)).add(inner.scale(0.035 * c));
            Vec3 innerBend = innerRoot.add(d.scale(0.1 * c)).add(inner.scale((0.03 + spread * 0.08) * c));
            this.talon(innerRoot, innerBend, inner, 0.012 * c, 0.02 * c, 0.01 * c, 0.016 * c, OctoSkin.STEEL, light);
            this.talon(innerBend, innerBend.add(d.scale(0.06 * c)).subtract(inner.scale(0.04 * c)), inner, 0.01 * c,
                    0.016 * c, 0.002 * c, 0.004 * c, OctoSkin.STEEL, light);
        }
    }

    // A flat, four-sided blade from `from` to `to`, thin across `out` and wider beside it, narrowing as it goes.
    private void talon(Vec3 from, Vec3 to, Vec3 out, double thin0, double wide0, double thin1, double wide1, Skin skin,
            int light) {
        Vec3 delta = to.subtract(from);
        double length = delta.length();
        if (length < 1.0E-4) {
            return;
        }
        Vec3 d = delta.scale(1.0 / length);
        this.band(across(from, d, out, thin0, wide0), across(to, d, out, thin1, wide1), 4, Math.PI / 4, skin, light);
    }

    // The harness on his body, as it stands or crouches.
    void harness(TentacleShapes.Body body) {
        Vec3 up = body.up();
        Vec3 left = body.left();
        int light = this.light(body.at(0.0, 1.1, 0.0));
        // The corset: five steel bands with rounded edges round his belly, a dark sleeve showing between them.
        this.corset(body, CORSET_LOW - 0.01, CORSET_LOW + 5 * CORSET_STEP, 0.95, OctoSkin.DARK, light);
        for (int k = 0; k < 5; k++) {
            double y0 = CORSET_LOW + k * CORSET_STEP;
            double y1 = y0 + CORSET_STEP - 0.009;
            this.corset(body, y0, y0 + 0.01, 0.97, 1.0, OctoSkin.STEEL.rows(0.0F, 0.25F), light);
            this.corset(body, y0 + 0.01, y1 - 0.01, 1.0, OctoSkin.STEEL, light);
            this.corset(body, y1 - 0.01, y1, 1.0, 0.97, OctoSkin.STEEL.rows(0.75F, 1.0F), light);
        }
        // The spine: steel vertebrae from his neck down to the corset, each with a wing to either side, on a dark rod.
        this.box(body.at(0.0, 1.27, -0.14), left, up, 0.018, 0.21, 0.02, OctoSkin.DARK, light);
        for (int k = 0; k < 9; k++) {
            double y = 1.47 - k * 0.05;
            this.box(body.at(0.0, y, -0.155), left, up, 0.035, 0.019, 0.03, OctoSkin.STEEL, light);
            this.box(body.at(0.0, y - 0.004, -0.142), left, up, 0.088 - k * 0.003, 0.009, 0.018, OctoSkin.STEEL,
                    light);
        }
        // The plate on the corset's back the tentacles are mounted on, and a round mount for each.
        this.box(body.at(0.0, 0.95, -0.2), left, up, 0.26, 0.26, 0.04, OctoSkin.DARK, light);
        for (int i = 0; i < 4; i++) {
            Vec3 socket = body.socket(i);
            Vec3 out = body.out(i);
            Ring back = across(socket.subtract(out.scale(0.08)), out, up, 0.17, 0.17);
            Ring rim = across(socket.subtract(out.scale(0.04)), out, up, 0.23, 0.23);
            Ring front = across(socket.add(out.scale(0.07)), out, up, 0.23, 0.23);
            Ring lip = across(socket.add(out.scale(0.1)), out, up, 0.2, 0.2);
            this.band(back, rim, 14, 0.0, OctoSkin.STEEL.rows(0.0F, 0.3F), light);
            this.band(rim, front, 14, 0.0, OctoSkin.STEEL, light);
            this.band(front, lip, 14, 0.0, OctoSkin.STEEL.rows(0.7F, 1.0F), light);
        }
    }

    // One band of the corset from height `y0` to `y1`, `size0` to `size1` of its full size.
    private void corset(TentacleShapes.Body body, double y0, double y1, double size0, double size1, Skin skin,
            int light) {
        this.band(this.waist(body, y0, size0), this.waist(body, y1, size1), 24, 0.0, skin, light);
    }

    private void corset(TentacleShapes.Body body, double y0, double y1, double size, Skin skin, int light) {
        this.corset(body, y0, y1, size, size, skin, light);
    }

    private Ring waist(TentacleShapes.Body body, double y, double size) {
        return new Ring(body.at(0.0, y, 0.0), body.left(), body.forward(), 0.285 * size, 0.165 * size, 5.0);
    }
}
