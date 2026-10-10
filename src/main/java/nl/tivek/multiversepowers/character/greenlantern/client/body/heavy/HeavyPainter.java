package nl.tivek.multiversepowers.character.greenlantern.client.body.heavy;

import java.util.Map;
import java.util.function.DoubleFunction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.GunParts;
import nl.tivek.multiversepowers.character.greenlantern.client.render.weapon.WeaponShapes;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import static nl.tivek.multiversepowers.character.greenlantern.heavy.HeavyMoves.*;

// The heavy weapons drawn: each in its own frame between the hands holding it (the battleaxe's haft along +z, its
// blades along ±y; the chainsaw's bar along +z, its top handle up +y and its teeth running round the bar; the guns'
// barrels along +z, their tops up +y, the launcher's rocket gone once fired and grown back as it reloads), grown
// from the rear end out of the ring's light and broken into solid pieces; the light trail of each axe swing; and in the
// world the earthbreaker's split in the ground with solid shards bursting up along it, and the whirlwind's ring.
public final class HeavyPainter {
    // Where each hand holds each weapon, in its frame (blocks): right, then left.
    private static final Vec3[][] GRIPS = { { new Vec3(0.0, 0.0, -0.45), new Vec3(0.0, 0.0, -0.85) },
            { new Vec3(0.0, 0.14, -0.53), new Vec3(0.0, 0.29, -0.03) },
            { new Vec3(0.0, -0.172, -0.195), new Vec3(0.0, -0.145, 0.175) },
            { new Vec3(0.0, -0.05, -0.227), new Vec3(0.0, -0.01, 0.15) },
            { GunParts.REVOLVER_GRIP, GunParts.REVOLVER_GRIP },
            { new Vec3(0.0, -0.02, -0.3), new Vec3(0.0, -0.22, -0.21) },
            { new Vec3(-0.1, -0.025, -0.57), new Vec3(0.0, 0.205, -0.26) } };
    // How big each weapon is drawn.
    private static final double[] SIZE = { 1.0, 1.1, 1.45, 0.95, 0.85, 1.25, 1.3 };
    // How far each weapon reaches back and out along its length, for it to grow from one end to the other.
    private static final double[][] ENDS = { { -1.0, 0.8 }, { -0.72, 0.95 }, { -0.91, 1.053 }, { -0.45, 0.52 },
            { -0.3, 0.53 }, { -0.45, 0.37 }, { -0.6, 0.8 } };
    private static final Shape[] BODIES = { WeaponShapes.BATTLEAXE, WeaponShapes.CHAINSAW_BODY,
            WeaponShapes.ROCKET_TUBE, WeaponShapes.SHOTGUN, GunParts.REVOLVER_FRAME, GunParts.CANNON_BODY,
            GunParts.MINIGUN_BODY };
    // Where the guns' shots leave them, in their frames: the launcher's mouth and its back blast, the two muzzles.
    private static final Vec3 MOUTH = new Vec3(0.0, 0.0, 0.39);
    private static final Vec3 BACK_BLAST = new Vec3(0.0, 0.0, -0.94);
    private static final Vec3[] MUZZLES = { new Vec3(0.039, 0.05, 0.52), new Vec3(-0.039, 0.05, 0.52) };
    private static final double WARHEAD_BACK = 0.338;
    private static final double WARHEAD_TIP = 1.053;
    // The shotgun's reload (ticks in): its barrels swing down about the hinge pin as far as OPEN (radians), the spent
    // shells fly out at EJECT, each new shell grows in his left hand at FETCH and is pressed home at SEAT, and it
    // snaps shut by SHUT.
    private static final Vec3 HINGE = new Vec3(0.0, -0.01, -0.005);
    private static final double OPEN = 0.75;
    private static final double EJECT = 8.0;
    private static final double[] FETCH = { 9.0, 14.5 };
    private static final double[] SEAT = { 13.0, 18.0 };
    private static final double SHUT = 24.5;
    private static final double[] CHAMBER_X = { 0.039, -0.039 };
    private static final Vec3 POCKET = new Vec3(0.16, -0.26, -0.24);
    // The teeth round the bar at as many steps from one tooth to the next, and how fast they run (teeth a tick).
    private static final int STEPS = 8;
    private static final Shape[] TEETH = new Shape[STEPS];
    private static final double IDLE_RUN = 0.3;
    private static final double CUT_RUN = 1.6;
    // The axe head's middle, and the trail's inner and outer edge along the haft.
    private static final double TRAIL_IN = 0.02;
    private static final double TRAIL_OUT = 0.62;
    private static final int TRAIL_STEPS = 7;
    private static final double TRAIL_GAP = 0.55;
    // The split in the ground: how long it takes to run out, stays, and how its shards stand.
    private static final double RUN = 3.0;
    private static final double STAY = 26.0;
    // Lines lying on the ground fade into it: the crack runs this far above it.
    private static final double CRACK_UP = 0.12;
    private static final Shape SHARD = Shape.of(Mesh.prism(-0.18, 0.18, 1.1, -0.22, 0.0, 0.22, 0.0, 0.05, 1.0));

    static {
        for (int k = 0; k < STEPS; k++) {
            TEETH[k] = Shape.of(WeaponShapes.sawTeeth((double) k / STEPS));
        }
    }

    private HeavyPainter() {
    }

    static Vec3 grip(int weapon, boolean right) {
        return GRIPS[weapon][right ? 0 : 1];
    }

    // Where a hand holds the weapon `reload` ticks into its reload: the shotgun's left hand rides the fore-end as the
    // barrels break open, fetches each new shell from his belt and presses it into its chamber, then shuts it.
    static Vec3 grip(int weapon, boolean right, double reload) {
        if (weapon != SHOTGUN || right || reload < 0.0) {
            return grip(weapon, right);
        }
        double open = opening(reload);
        Vec3 fore = hinged(GRIPS[SHOTGUN][1], open);
        Vec3[] chamber = { hinged(new Vec3(CHAMBER_X[0], 0.05, -0.07), open),
                hinged(new Vec3(CHAMBER_X[1], 0.05, -0.07), open) };
        if (reload < 5.0) {
            return fore;
        }
        if (reload < FETCH[0]) {
            return between(fore, POCKET, (reload - 5.0) / 3.0);
        }
        if (reload < SEAT[0]) {
            return between(POCKET, chamber[0], (reload - FETCH[0] - 1.0) / 3.0);
        }
        if (reload < FETCH[1]) {
            return between(chamber[0], POCKET, (reload - SEAT[0] - 0.5) / 1.0);
        }
        if (reload < SEAT[1] + 1.5) {
            return between(POCKET, chamber[1], (reload - FETCH[1]) / 3.0);
        }
        return between(chamber[1], fore, (reload - SEAT[1] - 1.5) / 2.5);
    }

    private static Vec3 between(Vec3 from, Vec3 to, double u) {
        return from.lerp(to, Ease.smooth(Mth.clamp(u, 0.0, 1.0)));
    }

    // How far the shotgun's barrels are broken open, 0 shut to 1.
    private static double opening(double reload) {
        return reload < 0.0 ? 0.0 : Ease.smooth((reload - 2.0) / 3.0) * (1.0 - Ease.smooth((reload - 23.0) / 1.5));
    }

    // A point of the barrels in the shotgun's frame, swung down `open` of the way about the hinge pin.
    private static Vec3 hinged(Vec3 point, double open) {
        double angle = OPEN * open;
        double y = point.y - HINGE.y;
        double z = point.z - HINGE.z;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(point.x, HINGE.y + y * cos - z * sin, HINGE.z + y * sin + z * cos);
    }

    static double size(int weapon) {
        return SIZE[weapon];
    }

    static double[] ends(int weapon) {
        return ENDS[weapon];
    }

    static Vec3 middle(int weapon) {
        return GRIPS[weapon][0].add(GRIPS[weapon][1]).scale(0.5);
    }

    // The weapon's frame with its middle between the hands at `middle`, its length along `way`, up near `up`.
    static Frame placed(int weapon, Vec3 middle, Vec3 way, Vec3 up, double scale) {
        Vec3 z = way.normalize();
        Vec3 y = square(up, z);
        Vec3 x = y.cross(z);
        Vec3 mid = middle(weapon);
        Vec3 origin = middle.subtract(x.scale(mid.x * scale)).subtract(y.scale(mid.y * scale))
                .subtract(z.scale(mid.z * scale));
        return new Frame(origin, x, y, z, scale);
    }

    // `v` with its part along `along` taken out, made length 1 (any square way when nothing is left).
    private static Vec3 square(Vec3 v, Vec3 along) {
        Vec3 out = v.subtract(along.scale(v.dot(along)));
        if (out.lengthSqr() < 1.0E-8) {
            out = along.cross(Math.abs(along.y) < 0.9 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0));
        }
        return out.normalize();
    }

    // The weapon in `frame`: grown `formed` of the way from its rear end, or broken apart `apart` of the way; the
    // chainsaw's teeth running at `rev` (0 idling, 1 cutting) by `time` (ticks); a gun's shot flashing `flash` (0 to
    // 1), the launcher's rocket `loaded` of the way grown back, and the shotgun `reload` ticks into its reload.
    static void weapon(LanternPainter painter, int weapon, Frame frame, double formed, double apart, double rev,
            double time, int seed, double flash, double loaded, double reload) {
        Shape body = BODIES[weapon];
        boolean warhead = weapon == RPG && loaded > 0.0;
        Shape teeth = weapon == SAW ? TEETH[Math.floorMod((int) Math.floor(time * Mth.lerp(rev, IDLE_RUN,
                CUT_RUN) * STEPS), STEPS)] : null;
        if (apart >= 0.0) {
            if (apart < 1.0) {
                painter.shattered(body, frame, apart, 1.0, seed);
                if (teeth != null) {
                    painter.shattered(teeth, frame, apart, 1.0, seed + 1);
                }
                if (warhead) {
                    painter.shattered(WeaponShapes.ROCKET_WARHEAD, frame, apart, 1.0, seed + 2);
                }
            }
            return;
        }
        if (formed < 1.0) {
            double[] ends = ENDS[weapon];
            painter.clip(frame.at(0.0, 0.0, Mth.lerp(Ease.smooth(formed), ends[0], ends[1])),
                    frame.forward().scale(-1.0), 1.0);
        } else if (warhead && loaded < 1.0) {
            painter.clip(frame.at(0.0, 0.0, Mth.lerp(Ease.smooth(loaded), WARHEAD_BACK, WARHEAD_TIP)),
                    frame.forward().scale(-1.0), 1.0);
        }
        if (warhead) {
            painter.shape(WeaponShapes.ROCKET_WARHEAD, frame, 1.0, 1.0);
            if (formed >= 1.0) {
                painter.noClip();
            }
        }
        if (weapon == SHOTGUN && reload >= 0.0 && formed >= 1.0) {
            Frame barrels = frame.turned(HINGE.x, HINGE.y, HINGE.z, 1.0, 0.0, 0.0, OPEN * opening(reload));
            painter.shape(WeaponShapes.SHOTGUN_STOCK, frame, 1.0, 1.0);
            painter.shape(WeaponShapes.SHOTGUN_BARRELS, barrels, 1.0, 1.0);
            shells(painter, frame, barrels, reload, seed);
        } else {
            painter.shape(body, frame, 1.0, 1.0);
        }
        if (teeth != null) {
            painter.shape(teeth, frame, 1.0, 1.0);
        }
        painter.noClip();
        if (weapon == SAW && rev > 0.05) {
            painter.flare(frame.at(0.0, -0.02, 0.85), 0.25 + 0.2 * rev, 0.5 * rev);
        }
        if (flash > 0.01 && weapon == RPG) {
            painter.flare(frame.at(MOUTH.x, MOUTH.y, MOUTH.z), 0.2 + 0.25 * flash, flash);
            painter.flare(frame.at(BACK_BLAST.x, BACK_BLAST.y, BACK_BLAST.z), 0.3 + 0.4 * flash, flash);
        } else if (flash > 0.01 && weapon == SHOTGUN) {
            for (Vec3 muzzle : MUZZLES) {
                painter.flare(frame.at(muzzle.x, muzzle.y, muzzle.z), 0.12 + 0.18 * flash, flash);
            }
        }
    }

    // The shotgun's shells as it reloads: the spent pair pulled out as it opens and flung back over his shoulder,
    // breaking up; each new one grown in his left hand, carried to its chamber and pressed home.
    private static void shells(LanternPainter painter, Frame frame, Frame barrels, double reload, int seed) {
        for (int k = 0; k < 2; k++) {
            double x = CHAMBER_X[k];
            if (reload < EJECT + 5.0) {
                double s = Math.max(0.0, reload - EJECT);
                double pulled = 0.03 * opening(reload);
                Frame out = barrels.moved(x * (1.0 + 0.8 * s), 0.05 + 0.035 * s, -pulled - 0.07 * s);
                if (s < 2.0) {
                    painter.shape(WeaponShapes.SHOTGUN_SHELL, out, 1.0, 1.0);
                } else {
                    painter.shattered(WeaponShapes.SHOTGUN_SHELL, out, (s - 2.0) / 3.0, 1.0, seed + 11 + k);
                }
            }
            if (reload < FETCH[k] || reload > SHUT) {
                continue;
            }
            if (reload < SEAT[k]) {
                Vec3 hand = grip(SHOTGUN, false, reload);
                double grown = Ease.smooth((reload - FETCH[k]) / 2.0);
                Frame held = new Frame(frame.at(hand.x, hand.y - 0.03, hand.z), barrels.right(), barrels.up(),
                        barrels.forward(), frame.scale() * Math.max(0.05, grown));
                painter.shape(WeaponShapes.SHOTGUN_SHELL, held, 1.0, 1.0);
            } else {
                double home = Ease.smooth((reload - SEAT[k]) / 1.5);
                painter.shape(WeaponShapes.SHOTGUN_SHELL, barrels.moved(x, 0.05, -0.07 * (1.0 - home)), 1.0, 1.0);
            }
        }
    }

    // The light an axe swing leaves behind its head: a ribbon through where the head was these last ticks, fading
    // out behind it. `place` gives the weapon's frame at a moment so many ticks ago.
    static void trail(LanternPainter painter, DoubleFunction<Frame> place, double strength) {
        if (strength <= 0.01) {
            return;
        }
        Frame was = place.apply(0.0);
        for (int k = 1; k <= TRAIL_STEPS; k++) {
            Frame then = place.apply(k * TRAIL_GAP);
            double a0 = strength * (1.0 - (k - 1.0) / TRAIL_STEPS);
            double a1 = strength * (1.0 - (double) k / TRAIL_STEPS);
            painter.sheet(was.at(0.0, 0.0, TRAIL_IN), was.at(0.0, 0.0, TRAIL_OUT), then.at(0.0, 0.0, TRAIL_OUT),
                    then.at(0.0, 0.0, TRAIL_IN), a0 * 0.5, a0, a1, a1 * 0.5);
            was = then;
        }
    }

    // How strongly an axe move trails light now: through each swing's fast part.
    static double trailing(ClientHeavy.Held held, float partialTick) {
        if (held.weapon != AXE || held.brokeAt >= 0.0) {
            return 0.0;
        }
        double age = held.age(partialTick);
        int hit = hit(AXE, held.move);
        return switch (held.move) {
            case CHOP, CHOP_BACK, CLEAVE, HOOK, WHIRL_OUT -> window(age, hit - 3.0, hit + 3.0);
            case LEAP -> window(age, hit - 3.0, hit + 2.0);
            default -> 0.0;
        };
    }

    private static double window(double age, double from, double to) {
        return Ease.smooth((age - from) / 1.5) * (1.0 - Ease.smooth((age - to) / 2.0));
    }

    public static boolean any() {
        return !ClientHeavy.all().isEmpty() || GunTracers.any();
    }

    // Every heavy weapon's marks in the world: the earthbreaker's split and the whirlwind's ring.
    public static void drawAll(LanternPainter painter, ClientLevel level, float partialTick) {
        for (Map.Entry<Integer, ClientHeavy.Held> entry : ClientHeavy.all().entrySet()) {
            ClientHeavy.Held held = entry.getValue();
            Entity owner = level.getEntity(entry.getKey());
            if (owner != null && held.weapon >= REVOLVERS) {
                GunTracers.fired(level, owner, held, partialTick);
            }
            if (held.weapon != AXE) {
                continue;
            }
            double age = held.age(partialTick);
            if (held.move == LEAP) {
                int hit = hit(AXE, LEAP);
                if (held.impact == null && age >= hit && owner != null) {
                    held.impact = owner.position();
                    held.impactAt = ClientHeavy.now(partialTick) - (age - hit);
                }
            }
            if (held.impact != null) {
                split(painter, held, ClientHeavy.now(partialTick) - held.impactAt, entry.getKey());
            }
            if (owner != null && (held.move == WHIRL && age >= LOOP_FROM - 1.0
                    || held.move == WHIRL_OUT && age < 7.0)) {
                double fade = held.move == WHIRL ? Ease.smooth((age - LOOP_FROM + 1.0) / 3.0)
                        : 1.0 - Ease.smooth(age / 7.0);
                double radius = wheel().value("axeWhirlRadius") * (held.move == WHIRL_OUT ? Mth.lerp(Ease.smooth(
                        age / 5.0), 0.8, 1.3) : 0.75);
                painter.circle(owner.getPosition(partialTick).add(0.0, 0.85, 0.0), new Vec3(1.0, 0.0, 0.0),
                        new Vec3(0.0, 0.0, 1.0), radius, 0.05, 0.4, Colors.alpha(0.8 * fade), Colors.alpha(0.4 * fade));
            }
        }
        GunTracers.draw(painter, level, partialTick);
    }

    // The split running out ahead from where the axe struck: a jagged crack of light, solid shards bursting up along
    // it and sinking away again.
    private static void split(LanternPainter painter, ClientHeavy.Held held, double since, int seed) {
        if (since < 0.0 || since > RUN + STAY + 8.0) {
            return;
        }
        double yaw = Math.toRadians(held.yaw);
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3 side = ahead.cross(Vectors.UP).normalize();
        Vec3 start = held.impact.add(ahead.scale(1.2)).add(0.0, CRACK_UP, 0.0);
        double length = wheel().value("leapLength");
        double reached = length * Ease.smooth(since / RUN);
        double fade = 1.0 - Ease.smooth((since - RUN - STAY) / 8.0);
        int steps = (int) Math.ceil(length * 2.0);
        Vec3 last = start;
        for (int i = 1; i <= steps; i++) {
            double d = length * i / steps;
            if (d > reached) {
                break;
            }
            double jag = (hash(seed, i) - 0.5) * 0.5;
            Vec3 next = start.add(ahead.scale(d)).add(side.scale(jag));
            painter.edge(last, next, 0.07, fade);
            last = next;
        }
        for (int i = 0; i < steps; i += 2) {
            double d = length * (i + 0.5) / steps;
            double rise = Ease.smooth((since - d / length * RUN) / 2.0);
            double sink = Ease.smooth((since - RUN - 10.0 - i * 0.6) / 6.0);
            double up = rise - sink;
            if (up <= 0.0) {
                continue;
            }
            double tall = 0.8 + 0.9 * hash(seed + 7, i);
            double lean = (hash(seed + 3, i) - 0.5) * 0.7;
            Vec3 foot = start.add(ahead.scale(d)).add(side.scale((hash(seed + 5, i) - 0.5) * 0.9))
                    .add(0.0, -CRACK_UP, 0.0);
            Vec3 tilt = Vectors.UP.add(side.scale(lean)).normalize();
            Frame frame = new Frame(foot.add(tilt.scale((up - 1.0) * tall)), side, tilt.scale(tall), side.cross(tilt),
                    1.0);
            painter.clip(start.add(0.0, -CRACK_UP, 0.0), Vectors.UP, 1.0);
            painter.shape(SHARD, frame, 1.0, 1.0);
            painter.noClip();
        }
    }

    private static double hash(int seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + i * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h & 0xFFFFFF) / (double) 0xFFFFFF;
    }

    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }
}
