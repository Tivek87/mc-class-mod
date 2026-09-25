package nl.tivek.multiversepowers.character.greenlantern.client.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientLooks;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.EYES_OUT;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.FINGER_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.HOVER_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.OWN_FINGER_SIZE;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.TRAIL;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.ahead;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation.finger;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalLight.eyes;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalRing.ring;

final class ArrivalDeparture {
    private static final float RISEN = 0.25F;
    static final float LAUNCH = 0.45F;
    private static final double AWAY_HIGH = 60.0;
    private static final double AWAY_AHEAD = 30.0;
    private static final int SPECKS = 26;

    private ArrivalDeparture() {
    }

    static void departing(RenderLevelStageEvent event, LanternPainter painter,
            MultiBufferSource.BufferSource buffers, AbstractClientPlayer player, float d, float partialTick) {
        boolean own = player == Minecraft.getInstance().player && !event.getCamera().isDetached();
        Vec3 finger = finger(event, player, partialTick);
        if (d < ClientLooks.UNDRESS_TICKS) {
            if (!own && d < EYES_OUT) {
                eyes(painter, player, 1.0 - d / EYES_OUT, partialTick);
            }
            specks(painter, player, finger, d, partialTick);
            return;
        }
        float e = (d - ClientLooks.UNDRESS_TICKS) / (ClientLooks.DEPART_TICKS - ClientLooks.UNDRESS_TICKS);
        Vec3 above = player.getEyePosition(partialTick).add(0.0, 1.0, 0.0);
        Vec3 ahead = ahead(player, partialTick);
        Vec3 at = departAt(finger, above, ahead, e);
        double size = Mth.lerp(Ease.smooth(e / RISEN), own ? OWN_FINGER_SIZE : FINGER_SIZE, HOVER_SIZE);
        if (e >= RISEN && e < LAUNCH) {
            float wave = (e - RISEN) / (LAUNCH - RISEN);
            painter.circle(at, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 0.2 + 2.6 * wave, 0.05, 0.3,
                    Colors.alpha(0.9 * (1.0 - wave)), Colors.alpha(0.45 * (1.0 - wave)));
        }
        if (e >= LAUNCH) {
            Vec3 last = at;
            for (int k = 1; k <= TRAIL; k++) {
                Vec3 next = departAt(finger, above, ahead, Math.max(LAUNCH, e - 0.012F * k));
                double fade = 1.0 - (double) k / (TRAIL + 1);
                painter.edge(last, next, 0.3 * fade, 0.95 * fade);
                last = next;
            }
        }
        Vec3 toCamera = event.getCamera().getPosition().subtract(at);
        Vec3 face = toCamera.lengthSqr() < 1.0E-6 ? ahead : toCamera.normalize();
        if (e < 0.96F) {
            ring(event, buffers, at, face, d * (e < LAUNCH ? 0.2 : 0.5), size);
            painter.flare(at, 0.35 + 0.25 * Mth.sin(d * 0.8F) + 1.2 * Math.max(0.0, e - LAUNCH), 0.9);
        } else {
            double twinkle = (1.0F - e) / 0.04F;
            painter.flare(at, 5.0 * twinkle, twinkle);
            painter.edge(at.add(-3.0 * twinkle, 0.0, 0.0), at.add(3.0 * twinkle, 0.0, 0.0), 0.15, twinkle);
            painter.edge(at.add(0.0, -3.0 * twinkle, 0.0), at.add(0.0, 3.0 * twinkle, 0.0), 0.15, twinkle);
        }
    }

    private static Vec3 departAt(Vec3 finger, Vec3 above, Vec3 ahead, float e) {
        if (e < RISEN) {
            return finger.lerp(above, Ease.smooth(e / RISEN));
        }
        if (e < LAUNCH) {
            return above.add(0.0, 0.08 * Math.sin((e - RISEN) * 40.0), 0.0);
        }
        double t = (e - LAUNCH) / (1.0 - LAUNCH);
        double climb = t * t;
        Vec3 side = new Vec3(-ahead.z, 0.0, ahead.x);
        double swirl = 2.5 * Math.sqrt(t);
        double turn = t * Math.PI * 4.0;
        return above.add(0.0, AWAY_HIGH * climb, 0.0).add(ahead.scale(AWAY_AHEAD * climb))
                .add(side.scale(swirl * Math.cos(turn))).add(ahead.scale(swirl * Math.sin(turn)));
    }

    private static void specks(LanternPainter painter, AbstractClientPlayer player, Vec3 finger, float d,
            float partialTick) {
        double strength = Math.sin(Math.PI * d / ClientLooks.UNDRESS_TICKS);
        if (strength <= 0.0) {
            return;
        }
        Vec3 feet = player.getPosition(partialTick);
        double height = player.getBbHeight();
        for (int k = 0; k < SPECKS; k++) {
            double cycle = d / 12.0 + Noise.of(k, 181, 0);
            int round = (int) Math.floor(cycle);
            double phase = cycle - round;
            double angle = Noise.of(k, 181 + round, 1) * Math.PI * 2.0;
            Vec3 start = feet.add(0.32 * Math.cos(angle), height * (0.1 + 0.8 * Noise.of(k,
                    181 + round, 2)), 0.32 * Math.sin(angle));
            Vec3 head = start.lerp(finger, phase * phase);
            Vec3 tail = start.lerp(finger, Math.max(0.0, phase - 0.15) * Math.max(0.0, phase - 0.15));
            painter.edge(tail, head, 0.055, Math.min(1.0, strength * (0.6 + 0.6 * phase)));
        }
    }
}
