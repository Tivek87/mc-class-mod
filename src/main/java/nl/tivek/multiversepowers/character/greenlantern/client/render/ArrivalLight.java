package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalRing.acrossOf;

/**
 * The light of the ring's arrival (see {@link ArrivalAnimation}): the rings of light it sends out, its scan, the
 * wave, the pillar and the aura as it slides on, and his eyes lighting up.
 */
final class ArrivalLight {
    // The scan: how far out the band of light round him is, in blocks, and how far over his head it starts.
    private static final double SCAN_RADIUS = 0.75;
    private static final double SCAN_TOP = 0.25;
    // The ring's light flaring up round him as it slides on: how long, in ticks, and how many tongues of light.
    private static final float AURA_TICKS = 30.0F;
    private static final int AURA_TONGUES = 18;
    // The pillar of light shooting up out of him as the ring slides on: how long it takes to shoot up, and how long it
    // lasts, in ticks.
    private static final float PILLAR_RISE = 3.0F;
    private static final float PILLAR_TICKS = 16.0F;
    // How long the ring's light takes to race out over the ground, and to sink away, in ticks.
    private static final float WAVE_TICKS = 10.0F;
    private static final float WAVE_FADE = 16.0F;

    private ArrivalLight() {
    }

    /**
     * The ring's light racing out over the ground from where he stands, as far as the creatures of the dark run from
     * it: a low wall of hard light and a line of light at its foot, sinking away as it goes.
     */
    static void wave(LanternPainter painter, Vec3 feet, float since) {
        if (since >= WAVE_FADE) {
            return;
        }
        double out = 1.0 - Math.pow(1.0 - Mth.clamp(since / WAVE_TICKS, 0.0F, 1.0F), 3.0);
        double radius = Math.max(0.4, Arrival.FEAR_RADIUS * out);
        double height = 0.7 * Math.pow(1.0 - since / WAVE_FADE, 1.3);
        int segments = 72;
        double half = Math.PI * radius / segments;
        Vec3 foot = feet.add(0.0, 0.03, 0.0);
        double[][] block = { { -half, 0.0, -0.1, half, height, 0.1, 1.2 } };
        for (int i = 0; i < segments; i++) {
            double angle = Math.PI * 2.0 * (i + 0.5) / segments;
            Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            painter.model(block, new ConstructPainter.Frame(foot.add(way.scale(radius)), way.cross(Vectors.UP),
                    Vectors.UP, way, 1.0), 1.0, 1.1);
        }
        double fade = Math.pow(1.0 - since / WAVE_FADE, 1.5);
        painter.circle(foot.add(0.0, 0.03, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius,
                0.14 + 0.1 * fade, 1.0, Colors.alpha(fade), Colors.alpha(0.6 * fade));
    }

    /**
     * A ring of light racing out of {@code at}, square to {@code face}, {@code reach} blocks out at the end: {@code u}
     * from 0 (just out) to 1 (gone).
     */
    static void burst(LanternPainter painter, Vec3 at, Vec3 face, float u, double reach) {
        double out = 1.0 - (1.0 - u) * (1.0 - u);
        double fade = 1.0 - u;
        double thick = 1.0 + 0.2 * reach;
        Vec3[] across = acrossOf(face);
        painter.circle(at, across[0], across[1], 0.15 + reach * out, 0.05 * thick, 0.3 * thick,
                Colors.alpha(fade), Colors.alpha(0.5 * fade));
    }

    /**
     * The ring's light shooting up out of him into the sky as it slides on: a pillar of light that is up in a moment,
     * then thins and dies down, with a wider sheath of light round its foot.
     */
    static void pillar(LanternPainter painter, Vec3 feet, float since) {
        if (since < 0.0F || since >= PILLAR_TICKS) {
            return;
        }
        double life = 1.0 - since / PILLAR_TICKS;
        double top = Arrival.PILLAR_HIGH * Ease.smooth(since / PILLAR_RISE);
        Vec3 base = feet.add(0.0, 0.05, 0.0);
        painter.edge(base, base.add(0.0, top, 0.0), 0.45 * Math.sqrt(life), life);
        painter.edge(base, base.add(0.0, top * 0.35, 0.0), 1.1 * life, 0.4 * life);
    }

    /**
     * The ring scanning him: a band of light round him sweeping down from over his head to his feet and back up, two
     * fainter ones trailing it, fed by a fan of light out of the ring.
     */
    static void scan(LanternPainter painter, AbstractClientPlayer player, Vec3 ring, float a,
            float partialTick) {
        float u = (a - Arrival.SCAN) / (Arrival.SCANNED - Arrival.SCAN);
        if (u < 0.0F || u > 1.0F) {
            return;
        }
        Vec3 feet = player.getPosition(partialTick);
        double fade = Math.min(1.0, Math.min(u, 1.0F - u) * 10.0);
        Vec3 band = feet;
        for (int k = 2; k >= 0; k--) {
            float behind = Math.max(0.0F, u - 0.04F * k);
            double down = behind < 0.5F ? Ease.smooth(behind * 2.0) : 1.0 - Ease.smooth(behind * 2.0 - 1.0);
            band = feet.add(0.0, Mth.lerp(down, player.getBbHeight() + SCAN_TOP, 0.05), 0.0);
            double strength = fade * (k == 0 ? 1.0 : 0.45 / k);
            painter.circle(band, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), SCAN_RADIUS, 0.03, 0.35,
                    Colors.alpha(0.95 * strength), Colors.alpha(0.45 * strength));
        }
        for (int k = 0; k < 8; k++) {
            double angle = k * Math.PI / 4.0 + a * 0.05;
            Vec3 edge = band.add(Math.cos(angle) * SCAN_RADIUS, 0.0, Math.sin(angle) * SCAN_RADIUS);
            painter.edge(ring, edge, 0.012, 0.35 * fade);
        }
    }

    /**
     * The ring's light flaring up round him as it slides on: tongues of light licking up round him from his feet and
     * a glowing ring of light at them, dying down.
     */
    static void aura(LanternPainter painter, Vec3 feet, double height, float since) {
        if (since < 0.0F || since >= AURA_TICKS) {
            return;
        }
        double life = 1.0 - since / AURA_TICKS;
        double grow = Math.min(1.0, since / 4.0);
        for (int k = 0; k < AURA_TONGUES; k++) {
            double angle = Math.PI * 2.0 * k / AURA_TONGUES + since * 0.04;
            double phase = (since * 0.09 + Noise.of(k, 171, 0)) % 1.0;
            Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            double out = 0.42 + 0.12 * Math.sin(since * 0.3 + k);
            Vec3 base = feet.add(way.scale(out)).add(0.0, height * (0.05 + 1.0 * phase) * grow, 0.0);
            Vec3 tip = base.add(way.scale(-0.06)).add(0.0,
                    height * (0.2 + 0.2 * Noise.of(k, 171, 1)) * grow, 0.0);
            painter.edge(base, tip, 0.05 * life, life * (1.0 - phase));
        }
        painter.circle(feet.add(0.0, 0.05, 0.0), new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 0.6, 0.06, 0.6,
                Colors.alpha(0.8 * life), Colors.alpha(0.4 * life));
    }

    /** His eyes glowing behind the mask, {@code glow} from 0 (out) to 1 (as bright as they get). */
    static void eyes(LanternPainter painter, AbstractClientPlayer player, double glow, float partialTick) {
        if (glow <= 0.0) {
            return;
        }
        Vec3 look = player.getViewVector(partialTick);
        Vec3 side = look.cross(Vectors.UP);
        side = side.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : side.normalize();
        Vec3 face = player.getEyePosition(partialTick).add(look.scale(0.27));
        for (int k = -1; k <= 1; k += 2) {
            painter.flare(face.add(side.scale(0.1 * k)), 0.1 + 0.3 * glow, glow);
        }
    }
}
