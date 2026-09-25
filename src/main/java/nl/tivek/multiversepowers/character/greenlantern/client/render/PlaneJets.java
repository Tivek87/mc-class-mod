package nl.tivek.multiversepowers.character.greenlantern.client.render;

import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;
import nl.tivek.multiversepowers.character.greenlantern.ability.AirStrike;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter.GLOWS;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlanePainter.missile;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneParts.falling;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.JET;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.MISSILE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.PlaneShapes.SMALL_MISSILE_SCALE;

/** The two jets that fly with the plane (see {@link PlanePainter}), and how they break away and are gone. */
final class PlaneJets {
    // How fast a jet's pieces fly on once it is gone, and how soon they slow down (ticks' worth of its speed).
    private static final double JET_FLIES_ON = 5.0;
    // A jet's size, next to the blocks its model is made in; how long its pieces take to fly apart as it goes; and how
    // long the star of light it goes in shines, in ticks.
    static final double JET_SCALE = 1.0;
    private static final double JET_BREAKS = 9.0;
    private static final double STAR_TICKS = 18.0;

    private PlaneJets() {
    }

    /**
     * The two jets: each grows out of a thread of the ring's light beside the plane, white-hot and cooling to green, and
     * races round it, banking into its turns, its flames burning, the air streaming off its wingtips and a small missile
     * under each wing (the next one growing out of the light once it has fired). Once the plane's engine bursts they
     * break away: their flames roar out, a cone of mist forms round them as they break the sound barrier with a ring of
     * light, and a moment later they are gone in a star of light, breaking into solid pieces.
     */
    static void jets(LanternPainter painter, int owner, PlanePath path, double t, @Nullable Vec3 ring,
            float partialTick) {
        double fled = path.jetsFled(t);
        for (int k = 0; k < PlanePath.JETS; k++) {
            if (!path.hasJet(k) || t < path.jetFrom(k)) {
                continue;
            }
            if (fled >= PlanePath.JET_GONE) {
                gone(painter, path, k, fled - PlanePath.JET_GONE);
                continue;
            }
            double since = t - path.jetFrom(k);
            double grown = path.jetGrown(k, t);
            Vec3 at = path.jetAt(k, t);
            Vec3[] axes = path.jetAxes(k, t);
            ConstructPainter.Frame frame = ConstructPainter.Frame.of(at, axes[0], axes[1],
                    JET_SCALE * Math.max(grown, 1.0E-3));
            if (since < PlanePath.JET_GROWS + 6.0) {
                // The ring's light pours into it as it takes shape (the plane's own, with its maker out of sight).
                double fade = 1.0 - Ease.smooth((since - PlanePath.JET_GROWS) / 6.0);
                Vec3 from = ring != null ? ring : path.point(t, (k == 0 ? -1.0 : 1.0) * PlanePath.JET_WING, 3.2, 0.4);
                painter.beamOfLight(from, at, fade, since, 1.3);
            }
            double speed = path.jetSpeed(k, t);
            if (painter.visible(at, 12.0 + 2.0 * speed)) {
                double hot = 0.65 * (1.0 - Ease.smooth(since / (PlanePath.JET_GROWS + 4.0)));
                painter.glare(hot);
                painter.ambient(GLOWS);
                painter.shape(JET, frame, 1.0, 1.0);
                pylons(painter, owner, path, k, frame, t, hot, partialTick);
                painter.ambient(0.0);
                painter.glare(0.0);
            }
            flames(painter, frame, speed, fled, grown);
            if (fled > -4.0) {
                soundBarrier(painter, path, k, frame, fled);
            }
        }
    }

    /**
     * The small missile under each wing of a jet. One it fires drops off its pylon and falls away under it with its
     * motor dead, the way the server moves it (see {@link PlanePath#fall}), until its motor fires (see
     * {@link ClientConstructs#launch}); meanwhile the next one grows out of the light on the pylon, timed from when the
     * last one was fired however soon that one struck.
     */
    private static void pylons(LanternPainter painter, int owner, PlanePath path, int k, ConstructPainter.Frame frame,
            double t, double hot, float partialTick) {
        for (int side = 0; side < 2; side++) {
            int variant = AirStrike.JET_MISSILE + 2 * k + side;
            int fired = ClientConstructs.launched(owner, variant);
            ClientConstructs.Launch launch = fired < 0 ? null
                    : ClientConstructs.launch(owner, variant, fired, partialTick);
            double since = fired < 0 ? -1.0 : launch == null ? t - fired : launch.since();
            if (since >= 0.0 && since <= (launch == null ? AirStrike.SMALL_IGNITES : launch.leaves())) {
                Vec3[] state = falling(path.firedOff(k, side, fired), true, since);
                painter.glare(0.0);
                missile(painter, true, state[0], state[1], state[2], -1.0, -1.0);
            }
            double load = since < 0.0 ? 1.0 : Ease.backOut((since - 2.0) / AirStrike.RELOAD_TICKS);
            if (load <= 0.01) {
                continue;
            }
            Vec3 at = frame.at((side == 0 ? -1.0 : 1.0) * AirStrike.PYLON_X, AirStrike.PYLON_Y, AirStrike.PYLON_Z);
            ConstructPainter.Frame missile = new ConstructPainter.Frame(at, frame.right(), frame.up(),
                    frame.forward(), frame.scale() * load * SMALL_MISSILE_SCALE);
            boolean growing = since >= 0.0 && since < AirStrike.RELOAD_TICKS + 2.0;
            painter.glare(growing ? Math.max(hot, 0.6 * (1.0 - load)) : hot);
            painter.ambient(GLOWS);
            painter.shape(MISSILE, missile, 1.0, 1.0);
        }
        painter.glare(hot);
        painter.ambient(GLOWS);
    }

    /**
     * The flames out of a jet's two nozzles, longer the faster it goes and roaring out once it breaks away; the air
     * streaming off its wingtips as it banks; and a long streak of light behind it once it races off.
     */
    private static void flames(LanternPainter painter, ConstructPainter.Frame frame, double speed, double fled,
            double grown) {
        double s = frame.scale();
        Vec3 back = frame.forward().scale(-1.0);
        double burn = (fled > 0.0 ? Math.min(1.0, 0.6 + fled / 6.0) : 0.55) * Math.min(1.0, grown);
        double length = (3.0 + 1.6 * Math.min(speed, 12.0)) * s;
        for (int side = -1; side <= 1; side += 2) {
            painter.exhaust(frame.at(side * 0.55, 0.0, -6.5), back, length, 0.4 * s, burn);
        }
        double bank = Mth.clamp(1.0 - frame.up().y, 0.0, 1.0);
        if (bank > 0.05 && fled < 0.0) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3 tip = frame.at(side * 5.42, -0.2, -3.6);
                painter.edge(tip, tip.add(back.scale((4.0 + 10.0 * bank) * s)), 0.07 * s, Math.min(0.7, 1.4 * bank));
            }
        }
        if (fled > 0.0) {
            Vec3 tail = frame.at(0.0, 0.0, -6.5);
            double streak = Math.min(60.0, 2.5 * speed * Math.min(1.0, fled / 4.0));
            painter.edge(tail, tail.add(back.scale(streak)), 0.5 * s, 0.7);
            painter.edge(tail, tail.add(back.scale(streak * 0.5)), 1.1 * s, 0.35);
        }
    }

    /**
     * A jet breaking the sound barrier: a cone of mist builds round its body as it nears the speed of sound and is torn
     * off it with a boom, a flash where it broke through and rings of light racing out from there.
     *
     * @param fled ticks since it broke away
     */
    private static void soundBarrier(LanternPainter painter, PlanePath path, int k, ConstructPainter.Frame frame,
            double fled) {
        double since = fled - PlanePath.JET_BOOM;
        if (since < -4.0 || since > 12.0) {
            return;
        }
        double s = frame.scale();
        double mist = since < 0.0 ? Ease.smooth((since + 4.0) / 4.0) : 1.0 - Ease.smooth(since / 5.0);
        if (mist > 0.01) {
            for (int c = 0; c < 7; c++) {
                double fade = mist * (1.0 - c / 8.0);
                painter.circle(frame.at(0.0, 0.0, 2.4 - c * 0.95), frame.right(), frame.up(), (1.1 + c * 0.42) * s,
                        0.06 * s, 0.9 * s, Colors.alpha(0.55 * fade), Colors.alpha(0.25 * fade));
            }
            painter.haze(frame.at(0.0, 0.0, -0.6), frame.right().scale(2.8 * s), frame.up().scale(2.8 * s),
                    frame.forward().scale(3.6 * s), LanternPainter.HOT, 0.35 * mist);
        }
        if (since >= 0.0) {
            double boom = path.failTick() + PlanePath.JET_BOOM;
            Vec3 where = path.jetAt(k, boom);
            Vec3[] across = Vectors.across(path.jetAxes(k, boom)[0]);
            double ring = 1.0 - Math.pow(1.0 - Math.min(1.0, since / 9.0), 2.0);
            double fade = 1.0 - since / 12.0;
            painter.circle(where, across[0], across[1], 2.0 + 26.0 * ring, 0.35, 2.6, Colors.alpha(0.9 * fade),
                    Colors.alpha(0.4 * fade));
            painter.circle(where, across[0], across[1], 1.0 + 16.0 * ring, 0.2, 1.6, Colors.alpha(0.6 * fade),
                    Colors.alpha(0.3 * fade));
            painter.flare(where, (10.0 + 14.0 * ring) * fade, fade);
        }
    }

    /**
     * A jet that raced off is gone: it breaks into solid pieces in a star of light, and the pieces and the star fly on
     * the way it went, slowing down, instead of stopping dead where it broke.
     *
     * @param since ticks since it went
     */
    private static void gone(LanternPainter painter, PlanePath path, int k, double since) {
        double t = path.failTick() + PlanePath.JET_GONE;
        Vec3 at = path.jetAt(k, t).add(path.jetVelocity(k, t).scale(JET_FLIES_ON
                * (1.0 - Math.exp(-since / JET_FLIES_ON))));
        if (since < JET_BREAKS) {
            Vec3[] axes = path.jetAxes(k, t);
            painter.glare(0.9 - 0.5 * since / JET_BREAKS);
            painter.fling(2.5);
            painter.shattered(JET, ConstructPainter.Frame.of(at, axes[0], axes[1], JET_SCALE), since / JET_BREAKS,
                    1.4);
            painter.fling(1.0);
            painter.glare(0.0);
        }
        star(painter, at, since);
    }

    /**
     * A star of light that flashes up sharp and bright and dies away, turning a little: four long spikes and four short
     * ones, a flare and a ring widening round it. It grows with how far off it is, so it shows from afar too.
     */
    private static void star(LanternPainter painter, Vec3 at, double since) {
        double life = 1.0 - since / STAR_TICKS;
        if (life <= 0.0) {
            return;
        }
        Vec3 view = at.subtract(painter.camera());
        double far = view.length();
        Vec3[] across = Vectors.across(far < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : view.scale(1.0 / far));
        double size = 3.0 + far * 0.045;
        double pop = Ease.smooth(since / 1.5);
        double shine = pop * life * life;
        double turn = since * 0.04;
        Vec3 a = across[0].scale(Math.cos(turn)).add(across[1].scale(Math.sin(turn)));
        Vec3 b = across[0].scale(-Math.sin(turn)).add(across[1].scale(Math.cos(turn)));
        double spike = size * 3.2 * pop * (0.4 + 0.6 * life);
        for (Vec3 way : new Vec3[] { a, b }) {
            painter.edge(at.subtract(way.scale(spike)), at.add(way.scale(spike)), size * 0.12 * shine, shine);
        }
        for (Vec3 way : new Vec3[] { a.add(b).normalize(), a.subtract(b).normalize() }) {
            painter.edge(at.subtract(way.scale(spike * 0.4)), at.add(way.scale(spike * 0.4)), size * 0.07 * shine,
                    0.8 * shine);
        }
        painter.flare(at, size * (1.2 + 1.4 * life), shine);
        painter.circle(at, a, b, size * (0.6 + 2.2 * (1.0 - life)), 0.1 * size, 0.6 * size, Colors.alpha(0.7 * shine),
                Colors.alpha(0.3 * shine));
    }
}
