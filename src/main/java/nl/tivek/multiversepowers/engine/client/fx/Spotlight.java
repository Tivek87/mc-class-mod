package nl.tivek.multiversepowers.engine.client.fx;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// A lamp's light thrown on what it shines at: each tick rays in rings round the beam find the blocks they hit, and the
// patch of light over those hits is drawn eased between ticks, so it follows steps, slopes and walls.
public final class Spotlight {
    private static final int STRIDE = 7;
    // How far above a surface the patch lies, so it never flickers into the block.
    private static final double LIFT = 0.035;
    // How much a patch may jump in depth between neighbouring rays (a wall's edge) and still be joined up.
    private static final double JOIN = 0.45;
    private static final double FADE_IN = 0.12;

    private final int rings;
    private final int spokes;
    private final double[] was;
    private final double[] now;
    private Vec3 fromWas = Vec3.ZERO;
    private Vec3 fromNow = Vec3.ZERO;
    private double reach = 1.0;
    private double strengthWas;
    private double strength;
    private boolean cast;

    public Spotlight(int rings, int spokes) {
        this.rings = rings;
        this.spokes = spokes;
        this.was = new double[this.rays() * STRIDE];
        this.now = new double[this.rays() * STRIDE];
    }

    public int spokes() {
        return this.spokes;
    }

    private int rays() {
        return 1 + this.rings * this.spokes;
    }

    private int ray(int ring, int spoke) {
        return ring == 0 ? 0 : 1 + (ring - 1) * this.spokes + Math.floorMod(spoke, this.spokes);
    }

    // The light level at pos as the server counts it (0 to 15): a client's level never moves its own sky darkening on
    // with the time of day, so it is worked out here the way the server does.
    public static int lightAt(Level level, BlockPos pos) {
        double day = 0.5 + 2.0 * Mth.clamp(Mth.cos(level.getTimeOfDay(1.0F) * ((float) Math.PI * 2.0F)), -0.25, 0.25);
        double rain = 1.0 - level.getRainLevel(1.0F) * 5.0 / 16.0;
        double thunder = 1.0 - level.getThunderLevel(1.0F) * 5.0 / 16.0;
        int darken = (int) ((1.0 - day * rain * thunder) * 11.0);
        return Math.max(level.getBrightness(LightLayer.BLOCK, pos), level.getBrightness(LightLayer.SKY, pos) - darken);
    }

    // How far the light is on (0 off, 1 fully on), eased between ticks.
    public double strength(float partialTick) {
        return Mth.lerp(partialTick, this.strengthWas, this.strength);
    }

    public boolean dark() {
        return this.strength <= 0.0 && this.strengthWas <= 0.0;
    }

    // Once a tick: the rays from `from` along `dir`, the widest halfAngle off it, out to `reach`, on the client's own
    // blocks. Off, the light fades out instead of snapping off.
    public void cast(Level level, Vec3 from, Vec3 dir, double halfAngle, double reach, boolean on) {
        this.strengthWas = this.strength;
        this.strength = Mth.clamp(this.strength + (on ? FADE_IN : -FADE_IN), 0.0, 1.0);
        if (this.dark()) {
            this.cast = false;
            return;
        }
        System.arraycopy(this.now, 0, this.was, 0, this.now.length);
        this.fromWas = this.cast ? this.fromNow : from;
        this.fromNow = from;
        this.reach = reach;
        Vec3 way = dir.normalize();
        Vec3[] across = Vectors.across(way);
        for (int ring = 0; ring <= this.rings; ring++) {
            double off = halfAngle * ring / this.rings;
            for (int spoke = 0; spoke < (ring == 0 ? 1 : this.spokes); spoke++) {
                double around = Math.PI * 2.0 * spoke / this.spokes;
                Vec3 d = way.scale(Math.cos(off)).add(across[0].scale(Math.cos(around) * Math.sin(off)))
                        .add(across[1].scale(Math.sin(around) * Math.sin(off)));
                this.trace(level, from, d, reach, this.ray(ring, spoke) * STRIDE);
            }
        }
        if (!this.cast) {
            System.arraycopy(this.now, 0, this.was, 0, this.now.length);
            this.cast = true;
        }
    }

    private void trace(Level level, Vec3 from, Vec3 d, double reach, int o) {
        BlockHitResult hit = level.clip(new ClipContext(from, from.add(d.scale(reach)), ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY, CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.BLOCK || hit.isInside()) {
            this.now[o + 6] = 0.0;
            return;
        }
        Direction face = hit.getDirection();
        Vec3 at = hit.getLocation();
        this.now[o] = at.x + face.getStepX() * LIFT;
        this.now[o + 1] = at.y + face.getStepY() * LIFT;
        this.now[o + 2] = at.z + face.getStepZ() * LIFT;
        // How squarely the light meets the face (1 head on), folded into the patch's strength.
        this.now[o + 3] = Math.max(0.0, -(d.x * face.getStepX() + d.y * face.getStepY() + d.z * face.getStepZ()));
        this.now[o + 4] = at.distanceTo(from);
        this.now[o + 6] = 1.0;
    }

    // The patch of light, `brightness` at its middle, fading to nothing at its rim and with distance.
    public void draw(ConstructPainter painter, float partialTick, int rgb, double brightness) {
        double strength = Mth.lerp(partialTick, this.strengthWas, this.strength) * brightness;
        if (!this.cast || strength <= 0.01) {
            return;
        }
        int rays = this.rays();
        Vec3[] points = new Vec3[rays];
        double[] alphas = new double[rays];
        double[] far = new double[rays];
        for (int i = 0; i < rays; i++) {
            int o = i * STRIDE;
            if (this.now[o + 6] == 0.0) {
                continue;
            }
            boolean both = this.was[o + 6] != 0.0;
            double u = both ? partialTick : 1.0;
            points[i] = new Vec3(Mth.lerp(u, this.was[o], this.now[o]), Mth.lerp(u, this.was[o + 1], this.now[o + 1]),
                    Mth.lerp(u, this.was[o + 2], this.now[o + 2]));
            far[i] = Mth.lerp(u, this.was[o + 4], this.now[o + 4]);
            int ring = i == 0 ? 0 : 1 + (i - 1) / this.spokes;
            double rim = 1.0 - Ease.smooth((double) ring / this.rings);
            double away = 1.0 - Ease.smooth(far[i] / this.reach);
            double facing = 0.35 + 0.65 * Math.sqrt(Mth.lerp(u, this.was[o + 3], this.now[o + 3]));
            alphas[i] = strength * rim * away * facing;
        }
        for (int spoke = 0; spoke < this.spokes; spoke++) {
            this.patch(painter, points, alphas, far, 0, this.ray(1, spoke), this.ray(1, spoke + 1), rgb);
        }
        for (int ring = 1; ring < this.rings; ring++) {
            for (int spoke = 0; spoke < this.spokes; spoke++) {
                int a = this.ray(ring, spoke);
                int b = this.ray(ring, spoke + 1);
                int c = this.ray(ring + 1, spoke + 1);
                int d = this.ray(ring + 1, spoke);
                this.patch(painter, points, alphas, far, a, b, c, rgb);
                this.patch(painter, points, alphas, far, a, c, d, rgb);
            }
        }
    }

    // The beam itself through the air, from the lamp to where its middle lands (or as far as it reaches).
    public void beam(ConstructPainter painter, float partialTick, int rgb, double width, double halfAngle,
            double brightness, Vec3 dir) {
        double strength = Mth.lerp(partialTick, this.strengthWas, this.strength) * brightness;
        if (!this.cast || strength <= 0.01) {
            return;
        }
        Vec3 from = this.fromWas.lerp(this.fromNow, partialTick);
        double length = this.now[6] != 0.0 ? Mth.lerp(this.was[6] != 0.0 ? partialTick : 1.0, this.was[4],
                this.now[4]) : this.reach;
        Vec3 to = from.add(dir.normalize().scale(length));
        double spread = width + 2.0 * length * Math.tan(halfAngle);
        painter.glowTaper(from, to, width, spread, rgb, 0.22 * strength, 0.0);
        painter.glowTaper(from, from.add(dir.normalize().scale(length * 0.45)), width * 0.5, spread * 0.4, rgb,
                0.3 * strength, 0.0);
    }

    private void patch(ConstructPainter painter, Vec3[] points, double[] alphas, double[] far, int a, int b, int c,
            int rgb) {
        if (points[a] == null || points[b] == null || points[c] == null) {
            return;
        }
        double near = Math.min(far[a], Math.min(far[b], far[c]));
        double most = JOIN * near + 0.5;
        if (points[a].distanceTo(points[b]) > most || points[b].distanceTo(points[c]) > most
                || points[c].distanceTo(points[a]) > most) {
            return;
        }
        painter.glowTriangle(points[a], alphas[a], points[b], alphas[b], points[c], alphas[c], rgb);
    }
}
