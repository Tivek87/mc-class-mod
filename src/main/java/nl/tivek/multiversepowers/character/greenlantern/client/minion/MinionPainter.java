package nl.tivek.multiversepowers.character.greenlantern.client.minion;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;
import nl.tivek.multiversepowers.character.greenlantern.minion.MinionMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Shape;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;

// The mech's helpers as every game draws them: robots of solid hard light, twice a player's height, a boxy trunk with
// a glowing core, a head with a visor slit, a cannon along the right forearm. They walk heavily, jab with the left and
// hook with the right, aim the cannon and fire it, crouch and leap and hammer both fists into the ground. Dropped out
// of the hatch they form from the head down; from a spawn egg they are built up out of a ring of light on the ground.
// Off the ground their feet burn; worn down they crack; dead they break into solid pieces. Each part is drawn in the
// frame of the joint it turns on (x its right, y up, z ahead).
public final class MinionPainter {
    private static final double HIP_Y = 1.55;
    private static final double HIP_X = 0.3;
    private static final double THIGH = 0.72;
    private static final double SHIN = 0.7;
    private static final double WAIST_Y = 1.98;
    private static final double SHOULDER_X = 0.8;
    private static final double SHOULDER_Y = 2.78;
    private static final double UPPER = 0.78;
    private static final double FOREARM = 0.72;
    private static final double TOP = 3.62;
    private static final double MUZZLE = FOREARM + 0.58;
    private static final double CANNON_X = 0.26;
    private static final double GROW = 10.0;
    private static final double BUILD = MinionPose.RISE * 0.75;
    // Its fissures for each crack, as paths on a part's face: the part (0 trunk, 1 head, 2 right thigh, 3 left upper
    // arm), then points in its frame.
    private static final double[][][] CRACKS = {
            { { 0, 0.3, 2.84, 0.515, 0.18, 2.7, 0.515, 0.26, 2.56, 0.515, 0.12, 2.4, 0.515, 0.18, 2.3, 0.515 },
                    { 1, -0.24, 3.42, 0.31, -0.16, 3.36, 0.31, -0.2, 3.3, 0.31 } },
            { { 0, -0.44, 2.5, 0.515, -0.3, 2.6, 0.515, -0.34, 2.74, 0.515, -0.18, 2.82, 0.515 },
                    { 0, 0.1, 2.26, 0.43, 0.0, 2.14, 0.43, 0.12, 2.04, 0.43 },
                    { 2, -0.05, -0.05, 0.2, 0.06, -0.25, 0.2, -0.02, -0.45, 0.2 } },
            { { 1, 0.22, 3.14, 0.31, 0.12, 3.08, 0.31, 0.18, 3.03, 0.31 },
                    { 0, -0.1, 2.84, 0.515, 0.02, 2.68, 0.515, -0.06, 2.5, 0.515, 0.08, 2.32, 0.515 },
                    { 3, -0.17, -0.1, 0.05, -0.17, -0.3, -0.04, -0.17, -0.52, 0.06 } } };
    private static final double GLOWS = 0.22;
    private static final double CREASES = 0.7;
    private static final double FLING = 2.0;
    private static final int PIECES = 16;

    private static final Shape PELVIS = Shape.of(Mesh.bevel(-0.46, HIP_Y - 0.14, -0.3, 0.46, WAIST_Y + 0.06, 0.3, 0.06,
            1.0));
    private static final Shape TRUNK = Shape.of(trunk());
    private static final Shape HEAD = Shape.of(head());
    private static final Shape THIGH_PART = Shape.of(Mesh.bevel(-0.18, -THIGH, -0.19, 0.18, 0.08, 0.19, 0.05, 1.0),
            Mesh.ball(10, 6, 0.17, 1.1).moved(0.0, -THIGH, 0.0));
    private static final Shape SHIN_PART = Shape.of(Mesh.bevel(-0.17, -SHIN, -0.18, 0.17, 0.0, 0.2, 0.05, 1.0),
            Mesh.bevel(-0.13, -SHIN + 0.15, 0.18, 0.13, -0.1, 0.25, 0.03, 1.2));
    private static final Shape FOOT = Shape.of(Mesh.bevel(-0.21, -0.14, -0.25, 0.21, 0.08, 0.42, 0.05, 1.0));
    private static final Shape UPPER_PART = Shape.of(Mesh.bevel(-0.16, -UPPER, -0.16, 0.16, 0.05, 0.16, 0.05, 1.0),
            Mesh.ball(10, 6, 0.15, 1.1).moved(0.0, -UPPER, 0.0));
    private static final Shape FOREARM_PART = Shape.of(Mesh.bevel(-0.15, -FOREARM, -0.16, 0.15, 0.0, 0.16, 0.05, 1.0),
            Mesh.bevel(-0.2, -FOREARM - 0.32, -0.2, 0.2, -FOREARM + 0.02, 0.2, 0.06, 1.05),
            Mesh.bevel(-0.17, -FOREARM - 0.34, 0.12, 0.17, -FOREARM - 0.2, 0.22, 0.03, 1.2));
    private static final Shape CANNON = Shape.of(Mesh.cylinder(12, 0.13, -MUZZLE + 0.1, -0.08, 1.05).moved(CANNON_X,
            0.0, 0.0), Mesh.cone(12, 0.13, 0.18, -MUZZLE, -MUZZLE + 0.12, 1.2).moved(CANNON_X, 0.0, 0.0),
            Mesh.torus(12, 5, 0.17, 0.04, 1.5).moved(CANNON_X, -MUZZLE, 0.0),
            Mesh.torus(12, 5, 0.15, 0.035, 1.4).moved(CANNON_X, -0.35, 0.0));
    // The painter's material while the helpers are drawn, given back after.
    private static Material was = LanternPainter.MECH_LIGHT;

    private MinionPainter() {
    }

    // The trunk over the waist: a broad chest with a plate on its front, shoulder pads, a narrower belly, and a ring
    // round the core on its chest.
    private static Mesh[] trunk() {
        return new Mesh[] { Mesh.bevel(-0.64, WAIST_Y, -0.4, 0.64, 2.98, 0.42, 0.08, 1.0),
                Mesh.bevel(-0.48, 2.28, 0.4, 0.48, 2.86, 0.5, 0.04, 1.15),
                Mesh.bevel(-0.42, WAIST_Y - 0.1, -0.3, 0.42, WAIST_Y + 0.12, 0.32, 0.04, 0.95),
                Mesh.ball(12, 8, 0.3, 1.05).moved(SHOULDER_X, 2.86, 0.0),
                Mesh.ball(12, 8, 0.3, 1.05).moved(-SHOULDER_X, 2.86, 0.0),
                Mesh.torus(14, 5, 0.17, 0.05, 1.5).turned(1.0, 0.0, 0.0, 90.0).moved(0.0, 2.58, 0.5),
                Mesh.cylinder(10, 0.14, 2.94, 3.06, 1.0) };
    }

    // The head: a block with a visor slit across its face and an antenna on its right.
    private static Mesh[] head() {
        return new Mesh[] { Mesh.bevel(-0.3, 3.02, -0.3, 0.3, 3.44, 0.3, 0.06, 1.0),
                Mesh.bevel(-0.26, 3.16, 0.27, 0.26, 3.28, 0.34, 0.02, 1.6),
                Mesh.cylinder(6, 0.03, 3.42, TOP - 0.06, 1.3).moved(0.18, 0.0, -0.1),
                Mesh.ball(8, 5, 0.06, 1.6).moved(0.18, TOP - 0.04, -0.1) };
    }

    // Whether any helper is about to be drawn: the world pass runs for them alone too.
    public static boolean any(ClientLevel level) {
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof MechMinion) {
                return true;
            }
        }
        return false;
    }

    // Every helper in the world.
    public static void drawAll(LanternPainter painter, ClientLevel level, float partialTick) {
        boolean any = false;
        for (Entity entity : level.entitiesForRendering()) {
            if (entity instanceof MechMinion minion && painter.visible(minion.getPosition(partialTick).add(0.0, 1.7,
                    0.0), 3.0)) {
                if (!any) {
                    any = true;
                    begin(painter);
                }
                draw(painter, level, minion, partialTick);
            }
        }
        if (any) {
            end(painter);
        }
    }

    private static void begin(LanternPainter painter) {
        was = painter.material();
        painter.material(LanternPainter.MECH_LIGHT);
        painter.batch();
        painter.ambient(GLOWS);
        painter.fling(FLING);
        painter.creases(CREASES);
    }

    private static void end(LanternPainter painter) {
        painter.flush();
        painter.creases(0.0);
        painter.fling(1.0);
        painter.ambient(0.0);
        painter.material(was);
    }

    private static void draw(LanternPainter painter, ClientLevel level, MechMinion minion, float partialTick) {
        MinionPose pose = MinionPose.of(level, minion, partialTick);
        double apart = minion.deathTime > 0 ? Mth.clamp((minion.deathTime + partialTick) / MechMinion.BREAK, 0.0, 1.0)
                : -1.0;
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, minion.yBodyRotO, minion.yBodyRot));
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Frame body = Frame.of(minion.getPosition(partialTick).add(0.0, -pose.sink, 0.0), ahead, Vectors.UP, 1.0);
        boolean ground = minion.came() == MechMinion.GROUND;
        double since = minion.since(partialTick);
        double grown = Mth.clamp(since / (ground ? BUILD : GROW), 0.0, 1.0);
        double cut = 0.0;
        if (grown < 1.0 && apart < 0.0) {
            cut = ground ? Mth.lerp(Ease.smooth(grown), -0.2, TOP + 0.1) : Mth.lerp(Ease.smooth(grown), TOP + 0.1, -0.2);
            painter.clip(body.at(0.0, cut, 0.0), ground ? Vectors.UP.scale(-1.0) : Vectors.UP, 1.0);
        }
        int seed = minion.getId() * PIECES * 20;
        Frame upper = body.turned(0.0, WAIST_Y, 0.0, 0.0, 1.0, 0.0, pose.twist).turned(0.0, WAIST_Y, 0.0, 1.0, 0.0,
                0.0, pose.lean);
        MechParts.draw(painter, PELVIS, body, 1.0, apart, seed);
        MechParts.draw(painter, TRUNK, upper, 1.0, apart, seed + PIECES);
        Frame head = upper.turned(0.0, 3.0, 0.0, 0.0, 1.0, 0.0, pose.look).turned(0.0, 3.0, 0.0, 1.0, 0.0, 0.0,
                pose.nod);
        MechParts.draw(painter, HEAD, head, 1.0, apart, seed + PIECES * 2);
        Frame[] thighs = new Frame[2];
        Frame[] arms = new Frame[2];
        Vec3[] feet = new Vec3[2];
        for (int s = 0; s < 2; s++) {
            boolean right = s == 0;
            double side = right ? 1.0 : -1.0;
            thighs[s] = body.moved(side * HIP_X, HIP_Y, 0.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, pose.hip[s]);
            feet[s] = leg(painter, body, thighs[s], pose.knee[s], apart, seed + PIECES * (3 + s));
            Frame arm = upper.moved(side * SHOULDER_X, SHOULDER_Y, 0.0).turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0,
                    side * pose.spread[s]).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, pose.arm[s]);
            arms[s] = arm;
            MechParts.draw(painter, UPPER_PART, arm, 1.0, apart, seed + PIECES * (5 + s));
            Frame forearm = arm.moved(0.0, -UPPER, 0.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, pose.elbow[s]);
            MechParts.draw(painter, FOREARM_PART, forearm, 1.0, apart, seed + PIECES * (7 + s));
            if (right) {
                MechParts.draw(painter, CANNON, forearm, 1.0, apart, seed + PIECES * 9);
                pose.muzzle = forearm.at(CANNON_X, -MUZZLE - 0.05, 0.0);
            }
        }
        painter.noClip();
        if (apart >= 0.0) {
            return;
        }
        if (grown < 1.0) {
            arrival(painter, body, ground, grown, cut);
        }
        if (grown > 0.5) {
            cracks(painter, minion.cracks(), upper, head, thighs[0], arms[1]);
            thrusters(painter, body, feet, pose.thrust);
            lights(painter, level, minion, pose, head, upper, partialTick);
        }
    }

    // A leg from its hip, turned already at the hip: the thigh, the shin under the knee, and the foot kept flat under
    // the ankle; gives the ankle.
    private static Vec3 leg(LanternPainter painter, Frame body, Frame thigh, double knee, double apart, int seed) {
        MechParts.draw(painter, THIGH_PART, thigh, 1.0, apart, seed);
        Frame shin = thigh.moved(0.0, -THIGH, 0.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, knee);
        MechParts.draw(painter, SHIN_PART, shin, 1.0, apart, seed + 4);
        Vec3 ankle = shin.at(0.0, -SHIN, 0.0);
        MechParts.draw(painter, FOOT, new Frame(ankle, body.right(), body.up(), body.forward(), body.scale()), 1.0,
                apart, seed + 8);
        return ankle;
    }

    // Being formed: up out of the ground a ring of light lies round its feet and a second rides the line it is built
    // up to; out of the hatch a ring rides the line it forms down to.
    private static void arrival(LanternPainter painter, Frame body, boolean ground, double grown, double cut) {
        double fade = 1.0 - Ease.smooth((grown - 0.85) / 0.15);
        Vec3 x = body.right();
        Vec3 z = body.forward();
        if (ground) {
            double open = Ease.smooth(grown / 0.2);
            painter.circle(body.at(0.0, 0.05, 0.0), x, z, 1.4 * open, 0.06, 0.45, Colors.alpha(0.9 * fade),
                    Colors.alpha(0.5 * fade));
            painter.circle(body.at(0.0, 0.05, 0.0), x, z, 0.9 * open, 0.04, 0.3, Colors.alpha(0.6 * fade),
                    Colors.alpha(0.3 * fade));
        }
        painter.circle(body.at(0.0, cut, 0.0), x, z, 0.85, 0.05, 0.4, Colors.alpha(0.95 * fade),
                Colors.alpha(0.55 * fade));
    }

    // Like an iron golem it cracks as it is worn down: bright fissures open on its chest and head first, then its
    // belly and thigh, then its face and arm.
    private static void cracks(LanternPainter painter, int cracks, Frame upper, Frame head, Frame thigh, Frame arm) {
        for (int c = 0; c < cracks; c++) {
            for (double[] path : CRACKS[c]) {
                Frame on = switch ((int) path[0]) {
                    case 1 -> head;
                    case 2 -> thigh;
                    case 3 -> arm;
                    default -> upper;
                };
                Vec3 last = on.at(path[1], path[2], path[3]);
                for (int i = 4; i + 2 < path.length; i += 3) {
                    Vec3 next = on.at(path[i], path[i + 1], path[i + 2]);
                    painter.edge(last, next, 0.035, 1.0);
                    last = next;
                }
            }
        }
    }

    // Off the ground its feet burn: a flare under each sole and a short flame down from it.
    private static void thrusters(LanternPainter painter, Frame body, Vec3[] feet, double thrust) {
        if (thrust < 0.05) {
            return;
        }
        double flicker = 0.85 + 0.15 * Math.sin(painter.time() * 2.3);
        for (Vec3 ankle : feet) {
            Vec3 sole = ankle.add(body.up().scale(-0.16));
            painter.flare(sole, 0.35 * thrust * flicker, 0.9 * thrust);
            painter.edge(sole, sole.add(body.up().scale(-0.7 * thrust * flicker)), 0.09 * thrust, thrust);
        }
    }

    // The light it gives off: its visor and core, the cannon's charge and its bolt.
    private static void lights(LanternPainter painter, ClientLevel level, MechMinion minion, MinionPose pose,
            Frame head, Frame upper, float partialTick) {
        double time = painter.time();
        double pulse = 0.85 + 0.15 * Math.sin(time * 0.3 + minion.getId());
        painter.glowLine(head.at(-0.22, 3.22, 0.36), head.at(0.22, 3.22, 0.36), 0.9, LanternPainter.GREEN,
                Colors.alpha(0.5 * pulse));
        painter.flare(upper.at(0.0, 2.58, 0.56), 0.22, 0.6 * pulse);
        if (minion.move() != MinionMoves.CANNON || pose.muzzle == null) {
            return;
        }
        double age = minion.moveAge(partialTick);
        if (age < MinionMoves.FIRE) {
            painter.flare(pose.muzzle, 0.1 + 0.35 * Ease.smooth(age / MinionMoves.FIRE), 0.8);
            return;
        }
        double after = age - MinionMoves.FIRE;
        Entity target = level.getEntity(minion.aimed());
        if (after < 3.0 && target != null) {
            double fade = 1.0 - after / 3.0;
            Vec3 to = target.getBoundingBox().getCenter();
            painter.glowLine(pose.muzzle, to, 1.4 * fade, LanternPainter.GREEN, Colors.alpha(0.8 * fade));
            painter.flare(pose.muzzle, 0.6 * fade, fade);
            painter.flare(to, 0.5 * fade, fade);
        }
    }
}
