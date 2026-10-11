package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.GunParts;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The guns' rounds in flight as every game sees them, leaving from the muzzle as it was last drawn: a streak of light
// for each revolver round racing to where it strikes, each minigun round a solid bullet of light with a short glowing
// trail, and the cannon's plasma, a blazing ball with a tail flying until it bursts.
final class GunTracers {
    // How fast a streak races (blocks a tick), how long it is, and the plasma's speed, as the server flies it.
    private static final double STREAK_SPEED = 45.0;
    private static final double STREAK_LENGTH = 3.5;
    private static final double PLASMA_SPEED = 2.4;
    private static final double PLASMA_LIFE = 40.0;
    // A minigun round is drawn this fast (slower than a real one, to be seen), its trail this long.
    private static final double BULLET_SPEED = 6.0;
    private static final double BULLET_TRAIL = 3.5;
    private static final double BULLET_SIZE = 2.6;

    private record Shot(Vec3 from, Vec3 to, double born, double charge, boolean plasma, boolean bullet, int owner) {
    }

    private static final List<Shot> SHOTS = new ArrayList<>();

    private GunTracers() {
    }

    // The rounds `held` has fired since last seen, sent on their way from where its owner stands now.
    static void fired(ClientLevel level, Entity owner, ClientHeavy.Held held, float partialTick) {
        if (!GunFire.of(held)) {
            return;
        }
        double age = held.age(partialTick);
        if (held.shotsMove != held.moves) {
            held.shotsMove = held.moves;
            held.shotsSeen = GunFire.count(held, age - 2.0);
        }
        int count = GunFire.count(held, age);
        for (int k = held.shotsSeen; k < count; k++) {
            Vec3 eye = owner.getEyePosition(partialTick);
            Vec3 look = owner.getViewVector(partialTick);
            Vec3 right = look.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
            int side = GunFire.side(held, k);
            boolean plasma = held.weapon == CANNON;
            boolean drawn = side < 2 && held.muzzle[side] != null
                    && held.muzzleAt >= ClientHeavy.now(partialTick) - 1.0;
            Vec3 from = drawn ? held.muzzle[side] : eye.add(look.scale(plasma ? 1.3 : 0.9))
                    .add(right.scale(side == 0 ? 0.28 : -0.28)).add(0.0, -0.18, 0.0);
            double range = plasma ? PLASMA_SPEED * PLASMA_LIFE : HeavyPainter.wheel().value(held.weapon == REVOLVERS
                    ? "revolverRange" : "minigunRange");
            Vec3 aim = eye.add(look.scale(range));
            HitResult block = level.clip(new ClipContext(eye, aim, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                    owner));
            Vec3 to = block.getType() == HitResult.Type.MISS ? aim : block.getLocation();
            double charge = plasma ? GunPainter.charged(held) : 0.0;
            SHOTS.add(new Shot(from, to, ClientHeavy.now(partialTick), charge, plasma, held.weapon == MINIGUN,
                    owner.getId()));
        }
        held.shotsSeen = count;
    }

    static void draw(LanternPainter painter, ClientLevel level, float partialTick) {
        double now = ClientHeavy.now(partialTick);
        SHOTS.removeIf(shot -> !alive(shot, now));
        for (Shot shot : SHOTS) {
            Vec3 path = shot.to.subtract(shot.from);
            double length = path.length();
            if (length < 1.0E-3) {
                continue;
            }
            Vec3 way = path.scale(1.0 / length);
            double s = now - shot.born;
            if (shot.plasma) {
                double gone = s * PLASMA_SPEED;
                double end = Math.min(length, struck(level, shot, way, length));
                if (gone > end) {
                    continue;
                }
                Vec3 at = shot.from.add(way.scale(gone));
                double size = 0.22 + 0.45 * shot.charge;
                painter.flare(at, size * (1.0 + 0.15 * Math.sin(s * 3.1)), 1.0);
                painter.flare(at, size * 0.45, 1.0);
                Vec3 tail = shot.from.add(way.scale(Math.max(0.0, gone - 0.8 - 0.8 * shot.charge)));
                painter.edge(tail, at, 0.04 + 0.06 * shot.charge, 0.7);
                continue;
            }
            if (shot.bullet) {
                bullet(painter, shot, way, length, s);
                continue;
            }
            double head = Math.min(length, s * STREAK_SPEED);
            double tail = Math.max(0.0, head - STREAK_LENGTH);
            double fade = 1.0 - Ease.smooth((s * STREAK_SPEED - length) / STREAK_SPEED);
            if (head - tail > 0.05 && fade > 0.01) {
                painter.edge(shot.from.add(way.scale(tail)), shot.from.add(way.scale(head)), 0.025, fade);
            }
            if (s * STREAK_SPEED >= length) {
                painter.flare(shot.to, 0.12, fade);
            }
        }
    }

    // A minigun round: a bullet of light flying nose first with its trail behind, a spark where it strikes.
    private static void bullet(LanternPainter painter, Shot shot, Vec3 way, double length, double s) {
        double gone = s * BULLET_SPEED;
        if (gone < length) {
            Vec3 at = shot.from.add(way.scale(gone));
            painter.shape(GunParts.ROUND, Frame.of(at, way, Vectors.UP, BULLET_SIZE), 1.0, 1.6);
            painter.flare(at, 0.15, 0.9);
            painter.flare(at, 0.06, 1.0);
            Vec3 tail = shot.from.add(way.scale(Math.max(0.0, gone - BULLET_TRAIL)));
            painter.edge(tail, at, 0.05, 0.9);
            return;
        }
        double fade = 1.0 - Ease.smooth((gone - length) / BULLET_SPEED);
        painter.flare(shot.to, 0.12, fade);
    }

    // How far along its path the plasma meets a creature (not its owner), or the whole length.
    private static double struck(ClientLevel level, Shot shot, Vec3 way, double length) {
        Vec3 end = shot.from.add(way.scale(length));
        double best = length;
        for (Entity entity : level.getEntities((Entity) null, new AABB(shot.from, end).inflate(1.0),
                entity -> entity instanceof LivingEntity && entity.getId() != shot.owner && entity.isAlive())) {
            var hit = entity.getBoundingBox().inflate(0.3).clip(shot.from, end);
            if (hit.isPresent()) {
                best = Math.min(best, hit.get().distanceTo(shot.from));
            }
        }
        return best;
    }

    private static boolean alive(Shot shot, double now) {
        double s = now - shot.born;
        if (shot.plasma) {
            return s * PLASMA_SPEED < shot.to.distanceTo(shot.from) + 0.5 && s < PLASMA_LIFE;
        }
        if (shot.bullet) {
            return s * BULLET_SPEED < shot.to.distanceTo(shot.from) + BULLET_SPEED;
        }
        return s * STREAK_SPEED < shot.to.distanceTo(shot.from) + 2.0 * STREAK_SPEED;
    }

    static boolean any() {
        return !SHOTS.isEmpty();
    }

    static void clear() {
        SHOTS.clear();
    }
}
