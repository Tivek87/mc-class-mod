package nl.tivek.multiversepowers.character.greenlantern.client.render.hand.light;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.ability.light.LightBubble;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.handFrame;
import static nl.tivek.multiversepowers.character.greenlantern.client.render.hand.HandPainter.ringGem;

// The two hands that work with their ring: the hammer of hard light and the chains, both solid, and their light.
public final class HandRingLight {
    private static final Vec3 GRIP = HandPose.HAMMER_GRIP;
    private static final Vec3 HEAD = HandPose.HAMMER_HEAD;
    private static final ConstructPainter.Shape HAFT = ConstructPainter.Shape.of(
            Mesh.cylinder(14, 0.3, -1.5, HandPose.HAFT - 0.8, 1.0).alongX().moved(GRIP.x, GRIP.y, GRIP.z),
            Mesh.ball(12, 8, 0.48, 1.5).moved(GRIP.x - 1.65, GRIP.y, GRIP.z),
            Mesh.torus(16, 5, 0.36, 0.1, 1.7).alongX().moved(GRIP.x - 1.2, GRIP.y, GRIP.z),
            Mesh.torus(16, 5, 0.36, 0.1, 1.7).alongX().moved(GRIP.x + 1.25, GRIP.y, GRIP.z));
    private static final ConstructPainter.Shape HEAD_SHAPE = ConstructPainter.Shape.of(
            Mesh.box(HEAD.x - 0.95, HEAD.y - 1.95, HEAD.z - 1.05, HEAD.x + 0.95, HEAD.y + 1.95, HEAD.z + 1.05, 1.1),
            Mesh.box(HEAD.x - 1.1, HEAD.y - 2.15, HEAD.z - 1.2, HEAD.x + 1.1, HEAD.y - 1.75, HEAD.z + 1.2, 1.6),
            Mesh.box(HEAD.x - 1.1, HEAD.y + 1.75, HEAD.z - 1.2, HEAD.x + 1.1, HEAD.y + 2.15, HEAD.z + 1.2, 1.6),
            Mesh.box(HEAD.x - 1.05, HEAD.y - 0.35, HEAD.z - 1.15, HEAD.x + 1.05, HEAD.y + 0.35, HEAD.z + 1.15, 1.8));
    private static final ConstructPainter.Shape HAMMER = ConstructPainter.Shape.of(
            Mesh.cylinder(14, 0.3, -1.5, HandPose.HAFT - 0.8, 1.0).alongX().moved(GRIP.x, GRIP.y, GRIP.z),
            Mesh.box(HEAD.x - 0.95, HEAD.y - 1.95, HEAD.z - 1.05, HEAD.x + 0.95, HEAD.y + 1.95, HEAD.z + 1.05, 1.1),
            Mesh.box(HEAD.x - 1.05, HEAD.y - 0.35, HEAD.z - 1.15, HEAD.x + 1.05, HEAD.y + 0.35, HEAD.z + 1.15, 1.8));
    private static final double HAMMER_BREAK_TICKS = 16.0;
    private static final double CHAIN_BREAK_TICKS = 12.0;
    private static final double LINK = 1.4;
    private static final double QUAKE_TICKS = 16.0;
    private static final int WHITE = 0xF2FFF4;
    private static final int GLOW = 0x5CFF7A;

    private HandRingLight() {
    }

    // Solid parts, drawn with the hand (and cut where it is cut).
    public static void parts(LanternPainter painter, ConstructPayload hand, HandPose pose, HandPose.Place place, Vec3 base,
            Vec3 facing, double clock, double bright, double apart) {
        switch (HandPose.move(hand.variant())) {
            case HandPose.RINGHAMMER -> hammer(painter, place, clock, bright, apart);
            case HandPose.RINGCHAINS -> chains(painter, hand, pose, place, base, facing, clock, bright, apart);
            default -> {
            }
        }
    }

    public static void blows(LanternPainter painter, ConstructPayload hand, HandPose pose, HandPose.Place place, Vec3 base,
            Vec3 facing, double clock, double reach, double strength) {
        if (strength <= 0.01) {
            return;
        }
        switch (HandPose.move(hand.variant())) {
            case HandPose.RINGHAMMER -> hammerLight(painter, hand, pose, place, base, facing, clock, reach, strength);
            case HandPose.RINGCHAINS -> chainLight(painter, hand, pose, place, base, facing, clock, strength);
            default -> {
            }
        }
    }

    // The haft pours out of the fist first, then the head swells on its end; it breaks into solid pieces.
    private static void hammer(LanternPainter painter, HandPose.Place place, double clock, double bright,
            double apart) {
        double grown = HandPose.hammerGrown(clock);
        if (grown <= 0.01) {
            return;
        }
        ConstructPainter.Frame hand = handFrame(place, false);
        double broken = (clock - HandPose.HAMMER_BREAKS) / HAMMER_BREAK_TICKS;
        if (broken >= 1.0) {
            return;
        }
        if (broken > 0.0 || apart >= 0.0) {
            painter.fling(1.4);
            painter.shattered(HAMMER, hand, Math.max(broken, apart), bright * 1.1, 131);
            painter.fling(1.0);
            return;
        }
        double haft = Ease.smooth(grown / 0.6);
        double head = Ease.backOut(Math.max(0.0, (grown - 0.45) / 0.55));
        painter.glare(0.8 * (1.0 - grown));
        if (haft > 0.01) {
            ConstructPainter.Frame drawn = haft >= 1.0 ? hand
                    : hand.moved(GRIP.x, GRIP.y, GRIP.z).stretched(haft, 1.0, 1.0).moved(-GRIP.x, -GRIP.y, -GRIP.z);
            painter.shape(HAFT, drawn, 1.0, bright);
        }
        if (head > 0.01) {
            ConstructPainter.Frame drawn = head >= 1.0 ? hand
                    : hand.moved(HEAD.x, HEAD.y, HEAD.z).stretched(head, head, head).moved(-HEAD.x, -HEAD.y, -HEAD.z);
            painter.shape(HEAD_SHAPE, drawn, 1.0, bright);
        }
        painter.glare(0.0);
    }

    // Light on the ring as the hammer is called, a trail behind the head as it comes down, and the quake it makes.
    private static void hammerLight(LanternPainter painter, ConstructPayload hand, HandPose pose, HandPose.Place place,
            Vec3 base, Vec3 facing, double clock, double reach, double strength) {
        Vec3 gem = ringGem(pose, place);
        double calling = clock - HandPose.HAMMER_GLOWS;
        double grown = HandPose.hammerGrown(clock);
        if (calling >= 0.0 && grown < 1.0) {
            double on = Ease.smooth(calling / 3.0) * (1.0 - grown * grown);
            painter.flare(gem, 1.6 * on * place.scale(), on * strength);
            painter.beamOfLight(gem, place.at(HEAD), (1.0 - grown) * strength, clock, 0.4);
        }
        double swing = clock - HandPose.HAMMER_SWINGS;
        double smash = clock - HandPose.HAMMER_SMASHES;
        if (swing >= 0.0 && smash < 3.0) {
            Vec3 last = place.at(HEAD);
            for (int k = 1; k <= 5; k++) {
                double then = clock - 0.5 * k;
                if (then < HandPose.HAMMER_SWINGS - 1.0) {
                    break;
                }
                Vec3 was = HandPose.at(hand.variant(), then, reach).place(base, facing, place.scale()).at(HEAD);
                double fade = (1.0 - k / 6.0) * (smash < 0.0 ? 1.0 : 1.0 - smash / 3.0) * strength;
                painter.glowLine(last, was, 2.4 * place.scale(), GLOW, Colors.alpha(0.35 * fade));
                painter.lightLine(last, was, 0.25 * place.scale(), WHITE, Colors.alpha(0.8 * fade));
                last = was;
            }
        }
        if (smash < 0.0 || smash > QUAKE_TICKS) {
            return;
        }
        Vec3 head = HandPose.at(hand.variant(), HandPose.HAMMER_SMASHES, reach).place(base, facing, place.scale())
                .at(HEAD);
        Vec3 ground = new Vec3(head.x, base.y + 0.08, head.z);
        double u = smash / QUAKE_TICKS;
        double fade = strength * (1.0 - u) * (1.0 - u);
        if (smash < 4.0) {
            painter.flare(head, 6.0 * (1.0 - smash / 4.0) * place.scale(), strength);
            painter.glowDisc(ground.add(0.0, 0.3, 0.0), 4.0 * (1.0 - smash / 4.0), 0xB8FFC8, 0.7 * strength, 0.25,
                    hand.id());
        }
        for (int k = 0; k < 3; k++) {
            double run = u * (1.0 + 0.35 * k) - 0.12 * k;
            if (run <= 0.0 || run >= 1.0) {
                continue;
            }
            double radius = (0.8 + 7.0 * (1.0 - (1.0 - run) * (1.0 - run))) * place.scale();
            double ring = strength * (1.0 - run);
            painter.circle(ground, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), radius, 0.12, 0.9,
                    Colors.alpha(0.9 * ring), Colors.alpha(0.45 * ring));
        }
        for (int k = 0; k < 14; k++) {
            Vec3 way = Noise.direction(hand.id() * 7 + k, 81);
            way = new Vec3(way.x, Math.abs(way.y) + 0.4, way.z).normalize();
            double out = (1.0 + 4.0 * u) * (0.6 + 0.4 * Noise.of(hand.id(), k, 82));
            Vec3 tip = ground.add(way.scale(out)).add(0.0, -1.2 * u * u, 0.0);
            painter.edge(tip.subtract(way.scale(0.5)), tip, 0.08, fade);
        }
    }

    // Chains of hard light out of the ring: they shoot out to the creature, wind round it, drag it to the fist and
    // break into solid links as it is caught.
    private static void chains(LanternPainter painter, ConstructPayload hand, HandPose pose, HandPose.Place place,
            Vec3 base, Vec3 facing, double clock, double bright, double apart) {
        double out = HandPose.chainsOut(clock);
        double caught = (clock - HandPose.CHAINS_CATCH) / CHAIN_BREAK_TICKS;
        if (out <= 0.0 || caught >= 1.0) {
            return;
        }
        Vec3[] path = chainPath(hand, pose, place, base, facing, clock);
        double link = LINK * place.scale();
        double broken = Math.max(apart, caught);
        if (broken > 0.0) {
            painter.fling(1.3);
            painter.chain(path, link, 1.0, bright * 1.1, 1.0, broken);
            painter.fling(1.0);
            return;
        }
        painter.glare(0.6 * (1.0 - out));
        painter.chain(path, link, 1.0, bright, out, -1.0);
        painter.glare(0.0);
    }

    // From the ring to the creature: straight while it flies, then round it from the top down as it winds.
    private static Vec3[] chainPath(ConstructPayload hand, HandPose pose, HandPose.Place place, Vec3 base, Vec3 facing,
            double clock) {
        Vec3 gem = ringGem(pose, place);
        AABB box = held(hand);
        Vec3 middle = box != null ? box.getCenter() : base.add(facing).add(0.0, 1.0, 0.0);
        double wound = box == null ? 0.0 : HandPose.chainsWound(clock);
        if (wound <= 0.0) {
            return new Vec3[] { gem, middle };
        }
        double radius = Math.max(box.getXsize(), box.getZsize()) * 0.5 + 0.45;
        double high = box.getYsize();
        Vec3 in = gem.subtract(middle);
        double start = Math.atan2(in.z, in.x);
        int steps = 24;
        Vec3[] path = new Vec3[steps + 2];
        path[0] = gem;
        double turns = 2.5 * wound;
        for (int i = 0; i <= steps; i++) {
            double u = (double) i / steps;
            double angle = start + Math.PI * 2.0 * turns * u;
            double y = high * (0.35 - 0.7 * u * wound);
            path[i + 1] = middle.add(Math.cos(angle) * radius, y, Math.sin(angle) * radius);
        }
        return path;
    }

    @Nullable
    private static AABB held(ConstructPayload hand) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!hand.held() || minecraft.level == null) {
            return null;
        }
        Entity entity = minecraft.level.getEntity(LightBubble.caughtId(hand.charge()));
        if (entity == null) {
            return null;
        }
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        Vec3 now = entity.getPosition(partialTick);
        return entity.getBoundingBox().move(now.subtract(entity.position()));
    }

    // The ring charging before it shoots, a flash as it does, light running along the chains, a flash at the catch.
    private static void chainLight(LanternPainter painter, ConstructPayload hand, HandPose pose, HandPose.Place place,
            Vec3 base, Vec3 facing, double clock, double strength) {
        Vec3 gem = ringGem(pose, place);
        double shoot = clock - HandPose.CHAINS_SHOOT;
        if (shoot < 0.0 && shoot > -10.0) {
            double charge = Ease.smooth((shoot + 10.0) / 10.0);
            double flicker = 0.8 + 0.2 * Math.sin(clock * (3.0 + 6.0 * charge));
            painter.flare(gem, (0.4 + 1.4 * charge) * flicker * place.scale(), (0.4 + 0.6 * charge) * strength);
        } else if (shoot >= 0.0 && shoot < 4.0) {
            painter.flare(gem, 3.0 * (1.0 - shoot / 4.0) * place.scale(), strength);
        }
        double out = HandPose.chainsOut(clock);
        if (out > 0.0 && clock < HandPose.CHAINS_CATCH) {
            Vec3[] path = chainPath(hand, pose, place, base, facing, clock);
            Vec3 end = path[path.length - 1];
            Vec3 head = gem.lerp(path[1], Math.min(1.0, out));
            double run = (clock * 0.35) % 1.0;
            Vec3 pulse = gem.lerp(path.length == 2 ? head : path[1], run);
            painter.flare(pulse, 0.5 * place.scale(), 0.7 * strength);
            if (out < 1.0) {
                painter.flare(head, 0.9 * place.scale(), strength);
            } else {
                painter.glowDisc(end, 1.1, GLOW, 0.25 * strength, 0.2, hand.id());
            }
        }
        double caught = clock - HandPose.CHAINS_CATCH;
        if (caught >= 0.0 && caught < 5.0) {
            Vec3 grip = place.at(HandPose.GRIP);
            painter.flare(grip, 3.5 * (1.0 - caught / 5.0) * place.scale(), strength);
            Vec3[] across = Vectors.across(place.up().normalize());
            painter.circle(grip, across[0], across[1], (0.5 + 3.0 * caught / 5.0) * place.scale(), 0.08, 0.6,
                    Colors.alpha((1.0 - caught / 5.0) * strength), Colors.alpha(0.5 * (1.0 - caught / 5.0) * strength));
        }
    }
}
