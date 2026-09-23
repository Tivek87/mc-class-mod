package nl.tivek.welcomescreen.client.character.lantern;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.welcomescreen.character.lantern.ConstructStorm;
import nl.tivek.welcomescreen.network.ConstructPayload;

/**
 * What the Construct Storm looks like (see {@link ConstructStorm}): he throws his ring fist up at the sky and a pillar
 * of light shoots out of the ring, straight up, and bursts open into a great ring of light high over his head. It
 * turns, with a second ring inside it turning the other way, spokes of light between the two and the lantern emblem in
 * the middle, and a thread of light keeps it tied to his ring while it rains constructs (those are drawn as slams, see
 * {@link SlamPainter#drop}). Once the rain is over it bursts, its light flying apart. Light, not a construct.
 */
final class StormLight {
    // The pillar: when it leaves the ring and how thick it is while he calls the storm, and the thread it thins to.
    private static final double PILLAR_FROM = 3.0;
    private static final double PILLAR = 1.8;
    private static final double THREAD = 0.25;
    // How long the ring takes to open, in ticks, how wide its inner ring is next to the outer one, and how many
    // spokes run between them.
    private static final double OPENING = 9.0;
    private static final double INNER = 0.7;
    private static final int SPOKES = 12;

    private StormLight() {
    }

    /**
     * One storm.
     *
     * @param clock  ticks since he called it, by the client's own clock
     * @param ring   where its maker's ring is, or null when he is out of sight
     * @param center where its ring of light hangs
     */
    static void draw(ConstructPainter painter, ConstructPayload storm, double clock, @Nullable Vec3 ring,
            Vec3 center) {
        double burstAt = storm.charge() - ConstructStorm.BURST_TICKS;
        double burst = Mth.clamp((clock - burstAt) / ConstructStorm.BURST_TICKS, 0.0, 1.0);
        double fade = 1.0 - burst;
        // The pillar shoots up out of his ring; once the ring is open it thins to a thread that feeds it.
        if (ring != null && fade > 0.0) {
            double rise = ConstructPainter.smooth((clock - PILLAR_FROM) / (ConstructStorm.OPEN_TICK - PILLAR_FROM));
            if (rise > 0.0) {
                Vec3 head = ring.lerp(center, rise);
                double thin = ConstructPainter.smooth((clock - ConstructStorm.CALL_TICKS + 6.0) / 8.0);
                painter.beamOfLight(ring, head, fade, 100.0, Mth.lerp(thin, PILLAR, THREAD));
            }
        }
        double open = clock - ConstructStorm.OPEN_TICK;
        if (open < 0.0) {
            return;
        }
        double radius = storm.size() * SlamPainter.backOut(open / OPENING) * (1.0 + 0.7 * burst * burst);
        if (radius <= 0.05) {
            return;
        }
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        // The flash as it bursts open.
        double flash = Math.max(0.0, 1.0 - open / 6.0);
        painter.flare(center, 1.2 + 4.0 * flash + 2.5 * burst, Math.max(0.6 * fade, flash));
        // The outer ring and the inner one, turning against each other.
        double spin = clock * 0.035;
        double pulse = 0.8 + 0.2 * Math.sin(clock * 0.3);
        painter.circle(center, east, south, radius, 0.28, 1.6, ConstructPainter.alpha(0.95 * fade),
                ConstructPainter.alpha(0.5 * pulse * fade));
        painter.circle(center, east, south, radius * 1.06, 0.08, 0.5, ConstructPainter.alpha(0.6 * fade),
                ConstructPainter.alpha(0.25 * fade));
        Vec3 turnedA = ConstructPainter.spin(east, ConstructPainter.UP, -spin * 1.6);
        Vec3 turnedB = ConstructPainter.spin(south, ConstructPainter.UP, -spin * 1.6);
        painter.circle(center, turnedA, turnedB, radius * INNER, 0.18, 1.0, ConstructPainter.alpha(0.85 * fade),
                ConstructPainter.alpha(0.4 * pulse * fade));
        // Spokes of light between the two, and a few running on outside, like rays.
        for (int k = 0; k < SPOKES; k++) {
            double angle = spin + Math.PI * 2.0 * k / SPOKES;
            Vec3 way = new Vec3(Math.cos(angle), 0.0, Math.sin(angle));
            painter.edge(center.add(way.scale(radius * INNER)), center.add(way.scale(radius)), 0.12, 0.8 * fade);
            if (k % 3 == 0) {
                double reach = radius * (1.25 + 0.15 * Math.sin(clock * 0.2 + k));
                painter.edge(center.add(way.scale(radius * 1.08)), center.add(way.scale(reach)), 0.08, 0.5 * fade);
            }
        }
        // The lantern emblem in the middle, turning slowly: its ring and the bar above and below it.
        Vec3 along = ConstructPainter.spin(east, ConstructPainter.UP, spin * 0.5);
        Vec3 over = ConstructPainter.spin(south, ConstructPainter.UP, spin * 0.5);
        double emblem = radius * 0.3;
        painter.circle(center, along, over, emblem, 0.22, 0.9, ConstructPainter.alpha(0.95 * fade),
                ConstructPainter.alpha(0.45 * fade));
        for (int side = -1; side <= 1; side += 2) {
            Vec3 bar = center.add(over.scale(side * emblem * 1.45));
            painter.edge(bar.subtract(along.scale(emblem * 0.8)), bar.add(along.scale(emblem * 0.8)), 0.3,
                    0.9 * fade);
        }
        // Bursting at the end: its light flies apart.
        if (burst > 0.0) {
            for (int k = 0; k < 24; k++) {
                Vec3 way = ConstructPainter.direction(k, 97);
                way = new Vec3(way.x, way.y * 0.4, way.z).normalize();
                Vec3 from = center.add(way.scale(radius * (0.6 + 0.6 * burst)));
                painter.edge(from, from.add(way.scale(1.5 + 3.0 * burst)), 0.15 * fade, fade);
            }
        }
    }

    /**
     * How far this player's ring fist is up to call a storm, 0 to 1: up it goes as he calls it, and back down once the
     * ring is open and the rain begins.
     */
    static float raised(Entity player, float partialTick) {
        float age = ClientConstructs.stormAge(player.getId(), partialTick);
        if (age < 0.0F) {
            return 0.0F;
        }
        return (float) (ConstructPainter.smooth(age / 3.0)
                * (1.0 - ConstructPainter.smooth((age - ConstructStorm.CALL_TICKS + 2.0) / 6.0)));
    }
}
