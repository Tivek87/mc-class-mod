package nl.tivek.multiversepowers.character.greenlantern.client.minion;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.mech.shape.MechParts;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.minion.MechMinion;
import nl.tivek.multiversepowers.character.greenlantern.minion.MinionMoves;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter.Frame;
import nl.tivek.multiversepowers.engine.client.render.Material;
import nl.tivek.multiversepowers.engine.client.render.mesh.Mesh;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.faction.mob.client.MobPreview;

// The mech's helpers as every game draws them: robots of solid hard light twice a player's height, built like the
// big mech in glowing tiles: a broad V of a chest with the Lantern's sign round a burning core, a pack with two
// nozzles on its back, a finned helmet with a V-shaped visor, layered plates over the shoulders, hands with jointed
// fingers and a ringed cannon along the right forearm (MinionShapes). They walk heavily heel to toe, jab and hook,
// brace and fire the cannon, leap on their pack's flames and hammer both fists into the ground (MinionPose). Dropped
// out of the hatch they form from the head down; from a spawn egg they are built up out of a ring of light on the
// ground. Worn down they crack all over and leak light (MinionCracks); dead they break into solid pieces. Each part
// is drawn in the frame of the joint it turns on (x its right, y up, z ahead).
public final class MinionPainter {
    private static final double GROW = 10.0;
    private static final double BUILD = MinionPose.RISE * 0.75;
    private static final double GLOWS = 0.22;
    private static final double CREASES = 0.7;
    private static final double FLING = 2.0;
    private static final int PIECES = 16;
    // How far each finger joint folds in a full fist, and the thumb.
    private static final double[] FOLD = { 1.5, 1.35 };
    private static final double THUMB_FOLD = 0.9;
    // A spike of the ground spikes: a crystal of hard light with two smaller ones at its foot.
    private static final ConstructPainter.Shape SPIKE = ConstructPainter.Shape.of(
            Mesh.cone(6, 0.42, 0.0, -0.3, 2.0, 1.2),
            Mesh.cone(5, 0.2, 0.0, -0.2, 0.9, 1.4).turned(0.0, 0.0, 1.0, 28.0).moved(0.36, 0.0, 0.08),
            Mesh.cone(5, 0.17, 0.0, -0.2, 0.7, 1.4).turned(1.0, 0.0, 0.0, -30.0).moved(-0.1, 0.0, -0.33));
    // The painter's material while the helpers are drawn, given back after.
    private static Material was = LanternPainter.MECH_LIGHT;

    private MinionPainter() {
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        MinionShapes.onClientSetup(event);
        event.enqueueWork(() -> {
            EntityType<MechMinion> type = MechMinion.TYPE.get();
            MobPreview.drawWith(type, MinionPainter::preview);
            MobPreview.move(type, MinionMoves.PUNCH_END + 8, m -> m.play(MinionMoves.PUNCH), m -> m.play(0));
            MobPreview.move(type, MinionMoves.SPIN_END + 8, m -> m.play(MinionMoves.SPIN), m -> m.play(0));
            MobPreview.move(type, MinionMoves.CANNON_END + 8, m -> m.play(MinionMoves.CANNON), m -> m.play(0));
        });
    }

    // One helper alone on the Mobs page.
    private static void preview(PoseStack pose, Vec3 camera, LivingEntity living, float partialTick,
            MultiBufferSource.BufferSource buffers) {
        if (!(living instanceof MechMinion minion) || !(minion.level() instanceof ClientLevel level)) {
            return;
        }
        LanternPainter painter = LanternPainter.gui(pose, camera, minion.tickCount + partialTick);
        begin(painter);
        draw(painter, level, minion, partialTick);
        end(painter);
        painter.finish(buffers);
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
        double yaw = Math.toRadians(Mth.rotLerp(partialTick, minion.yBodyRotO, minion.yBodyRot)) + pose.turn;
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Frame stand = Frame.of(minion.getPosition(partialTick).add(0.0, -pose.sink, 0.0), ahead, Vectors.UP, 1.0);
        Frame body = stand.turned(0.0, MinionShapes.HIP_Y, 0.0, 0.0, 0.0, 1.0, pose.roll);
        boolean ground = minion.came() == MechMinion.GROUND;
        double since = minion.since(partialTick);
        double grown = Mth.clamp(since / (ground ? BUILD : GROW), 0.0, 1.0);
        double cut = 0.0;
        if (grown < 1.0 && apart < 0.0) {
            double top = MinionShapes.TOP + 0.1;
            cut = ground ? Mth.lerp(Ease.smooth(grown), -0.2, top) : Mth.lerp(Ease.smooth(grown), top, -0.2);
            painter.clip(stand.at(0.0, cut, 0.0), ground ? Vectors.UP.scale(-1.0) : Vectors.UP, 1.0);
        }
        int seed = minion.getId() * PIECES * 24;
        double waist = MinionShapes.WAIST_Y;
        Frame belly = body.turned(0.0, waist, 0.0, 0.0, 1.0, 0.0, pose.twist * 0.5).turned(0.0, waist, 0.0, 1.0, 0.0,
                0.0, pose.lean * 0.5);
        Frame upper = body.turned(0.0, waist, 0.0, 0.0, 1.0, 0.0, pose.twist).turned(0.0, waist, 0.0, 1.0, 0.0, 0.0,
                pose.lean);
        Frame head = upper.turned(0.0, MinionShapes.NECK_Y, 0.0, 0.0, 1.0, 0.0, pose.look)
                .turned(0.0, MinionShapes.NECK_Y, 0.0, 1.0, 0.0, 0.0, pose.nod);
        MechParts.draw(painter, MinionShapes.PELVIS, body, 1.0, apart, seed);
        MechParts.draw(painter, MinionShapes.ABDOMEN, belly, 1.0, apart, seed + PIECES);
        MechParts.draw(painter, MinionShapes.TRUNK, upper, 1.0, apart, seed + PIECES * 2);
        MechParts.draw(painter, MinionShapes.HEAD, head, 1.0, apart, seed + PIECES * 3);
        Frame[] parts = new Frame[MinionCracks.Part.values().length];
        parts[MinionCracks.Part.PELVIS.ordinal()] = body;
        parts[MinionCracks.Part.TRUNK.ordinal()] = upper;
        parts[MinionCracks.Part.HEAD.ordinal()] = head;
        Vec3[] soles = new Vec3[2];
        for (int s = 0; s < 2; s++) {
            boolean right = s == 0;
            double side = right ? 1.0 : -1.0;
            Frame thigh = body.moved(side * MinionShapes.HIP_X, MinionShapes.HIP_Y, 0.0).turned(0.0, 0.0, 0.0, 1.0,
                    0.0, 0.0, pose.hip[s]);
            Frame shin = thigh.moved(0.0, -MinionShapes.THIGH, 0.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                    pose.knee[s]);
            Frame foot = new Frame(shin.at(0.0, -MinionShapes.SHIN, 0.0), stand.right(), stand.up(), stand.forward(),
                    stand.scale()).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, pose.ankle[s]);
            MechParts.draw(painter, right ? MinionShapes.THIGH_RIGHT : MinionShapes.THIGH_LEFT, thigh, 1.0, apart,
                    seed + PIECES * (4 + s));
            MechParts.draw(painter, right ? MinionShapes.SHIN_RIGHT : MinionShapes.SHIN_LEFT, shin, 1.0, apart,
                    seed + PIECES * (6 + s));
            MechParts.draw(painter, right ? MinionShapes.FOOT_RIGHT : MinionShapes.FOOT_LEFT, foot, 1.0, apart,
                    seed + PIECES * (8 + s));
            soles[s] = foot.at(0.0, -0.15, 0.04);
            Frame arm = upper.moved(side * MinionShapes.SHOULDER_X, MinionShapes.SHOULDER_Y, 0.0).turned(0.0, 0.0,
                    0.0, 0.0, 0.0, 1.0, side * pose.spread[s]).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, pose.arm[s]);
            Frame forearm = arm.moved(0.0, -MinionShapes.UPPER, 0.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                    pose.elbow[s]);
            MechParts.draw(painter, right ? MinionShapes.UPPER_RIGHT : MinionShapes.UPPER_LEFT, arm, 1.0, apart,
                    seed + PIECES * (10 + s));
            MechParts.draw(painter, right ? MinionShapes.FOREARM_RIGHT : MinionShapes.FOREARM_LEFT, forearm, 1.0,
                    apart, seed + PIECES * (12 + s));
            Frame hand = forearm.moved(0.0, -MinionShapes.FOREARM, 0.0).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0,
                    pose.wrist[s]);
            hand(painter, hand, side, pose.curl[s], apart, seed + PIECES * (14 + s));
            if (right) {
                MechParts.draw(painter, MinionShapes.CANNON, forearm, 1.0, apart, seed + PIECES * 16);
                pose.muzzle = forearm.at(MinionShapes.CANNON_X, -MinionShapes.MUZZLE - 0.05, 0.0);
            }
            parts[(right ? MinionCracks.Part.THIGH_RIGHT : MinionCracks.Part.THIGH_LEFT).ordinal()] = thigh;
            parts[(right ? MinionCracks.Part.SHIN_RIGHT : MinionCracks.Part.SHIN_LEFT).ordinal()] = shin;
            parts[(right ? MinionCracks.Part.UPPER_RIGHT : MinionCracks.Part.UPPER_LEFT).ordinal()] = arm;
            parts[(right ? MinionCracks.Part.FOREARM_RIGHT : MinionCracks.Part.FOREARM_LEFT).ordinal()] = forearm;
        }
        painter.noClip();
        if (apart >= 0.0) {
            return;
        }
        if (grown < 1.0) {
            arrival(painter, stand, ground, grown, cut);
        }
        if (grown > 0.5) {
            MinionCracks.draw(painter, minion.getId(), MinionPose.lost(minion), parts);
            thrusters(painter, upper, stand, soles, pose.thrust);
            spikes(painter, level, minion, partialTick);
            whirl(painter, minion, stand, upper, partialTick);
            lights(painter, level, minion, pose, head, upper, partialTick);
        }
    }

    // The ground spikes: each breaks out of the ground in its turn, overshooting its height, stands while the rest
    // follow and sinks back into the ground, all solid hard light.
    private static void spikes(LanternPainter painter, ClientLevel level, MechMinion minion, float partialTick) {
        if (minion.move() != MinionMoves.SPIKES) {
            return;
        }
        double age = minion.moveAge(partialTick);
        double yaw = Math.toRadians(minion.yBodyRot);
        Vec3 ahead = new Vec3(-Math.sin(yaw), 0.0, Math.cos(yaw));
        for (int k = 0; k < MinionMoves.SPIKE_COUNT; k++) {
            double since = age - MinionMoves.PLANT - MinionMoves.SPIKE_EVERY * k;
            double sinking = (since - MinionMoves.SPIKE_STANDS) / 5.0;
            if (since < 0.0 || sinking >= 1.0) {
                continue;
            }
            Vec3 at = MinionMoves.spikeAt(level, minion.position(), minion.yBodyRot, k);
            if (at == null) {
                continue;
            }
            double tall = Ease.backOut(Math.min(1.0, since / 2.5)) * (0.8 + 0.08 * k);
            double down = Ease.smooth(Math.max(0.0, sinking)) * (2.0 * tall + 0.4);
            Frame frame = Frame.of(at.add(0.0, -down, 0.0), ahead, Vectors.UP, 1.0)
                    .turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, 0.3)
                    .turned(0.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.7 * k)
                    .stretched(1.0, Math.max(0.01, tall), 1.0);
            painter.clip(at, Vectors.UP, 1.0);
            painter.shape(SPIKE, frame, 1.0, 1.2);
            painter.noClip();
            if (since < 4.0) {
                painter.flare(at.add(0.0, 0.3, 0.0), 0.9 * (1.0 - since / 4.0), 0.8);
            }
        }
    }

    // Whirling, its open hands leave a ring of light round it at shoulder height; rushing, streaks of light trail
    // back from its shoulders.
    private static void whirl(LanternPainter painter, MechMinion minion, Frame stand, Frame upper,
            float partialTick) {
        double age = minion.moveAge(partialTick);
        if (minion.move() == MinionMoves.SPIN) {
            double fast = Ease.smooth((age - MinionMoves.SPIN_WIND) / 3.0)
                    * (1.0 - Ease.smooth((age - MinionMoves.SPIN_WIND - 9.0) / 4.0));
            if (fast > 0.01) {
                Vec3 middle = stand.at(0.0, MinionShapes.SHOULDER_Y - 0.2, 0.0);
                painter.circle(middle, stand.right(), stand.forward(), 2.3, 0.07, 0.6, Colors.alpha(0.8 * fast),
                        Colors.alpha(0.4 * fast));
                painter.circle(middle.add(0.0, -0.15, 0.0), stand.right(), stand.forward(), 1.9, 0.04, 0.4,
                        Colors.alpha(0.5 * fast), Colors.alpha(0.25 * fast));
            }
        } else if (minion.move() == MinionMoves.CHARGE) {
            double rush = Ease.smooth((age - MinionMoves.RUSH) / 2.0)
                    * (1.0 - Ease.smooth((age - MinionMoves.RUSH - MinionMoves.RUSH_TICKS) / 3.0));
            if (rush > 0.01) {
                Vec3 back = stand.forward().scale(-2.4);
                for (int k = -1; k <= 1; k++) {
                    Vec3 from = upper.at(0.85 * k, MinionShapes.SHOULDER_Y - 0.4 * Math.abs(k), -0.2);
                    painter.glowLine(from, from.add(back), 0.5, LanternPainter.GREEN, Colors.alpha(0.5 * rush));
                    painter.lightLine(from, from.add(back), 0.06, 0xE4FFEA, Colors.alpha(0.8 * rush));
                }
            }
        }
    }

    // A hand from its wrist: the palm, four fingers of two joints hanging from its lower edge and a thumb at its
    // front, all folding towards the palm (`side`'s inside) as far as `curl` says, 1 a fist.
    private static void hand(LanternPainter painter, Frame wrist, double side, double curl, double apart, int seed) {
        MechParts.draw(painter, side > 0.0 ? MinionShapes.PALM_RIGHT : MinionShapes.PALM_LEFT, wrist, 1.0, apart,
                seed);
        for (int k = 0; k < MinionShapes.FINGER_Z.length; k++) {
            // The outer fingers fold a little further, as a real fist's do.
            double more = 1.0 + 0.06 * Math.abs(k - 1.5);
            Frame joint = wrist.moved(0.0, -MinionShapes.PALM, MinionShapes.FINGER_Z[k]);
            for (int j = 0; j < 2; j++) {
                joint = joint.turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -side * curl * FOLD[j] * more);
                MechParts.draw(painter, MinionShapes.FINGERS[j], joint, 1.0, apart, seed + 1 + k * 2 + j);
                joint = joint.moved(0.0, -MinionShapes.FINGER[j], 0.0);
            }
        }
        Frame thumb = wrist.moved(-side * 0.01, -0.06, 0.13).turned(0.0, 0.0, 0.0, 1.0, 0.0, 0.0, -0.55)
                .turned(0.0, 0.0, 0.0, 0.0, 0.0, 1.0, -side * (0.25 + curl * THUMB_FOLD));
        MechParts.draw(painter, MinionShapes.THUMB, thumb, 1.0, apart, seed + 9);
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

    // Off the ground its pack's two nozzles and its soles burn, the flames pointing down and a little back.
    private static void thrusters(LanternPainter painter, Frame upper, Frame stand, Vec3[] soles, double thrust) {
        if (thrust < 0.05) {
            return;
        }
        Vec3 down = stand.up().scale(-1.0);
        Vec3 back = upper.up().scale(-1.0).add(upper.forward().scale(-0.35));
        for (double x : MinionShapes.NOZZLE_X) {
            painter.exhaust(upper.at(x, MinionShapes.NOZZLE_Y - 0.15, MinionShapes.NOZZLE_Z + 0.06), back, 1.1,
                    0.09, thrust);
        }
        for (Vec3 sole : soles) {
            painter.exhaust(sole, down, 0.75, 0.08, thrust * 0.85);
        }
    }

    // The light it gives off: its V-shaped visor and the core in the Lantern's sign on its chest, beating together,
    // and the cannon: rings of light drawn in along it as it charges, the bolt, and the glow left in the muzzle.
    private static void lights(LanternPainter painter, ClientLevel level, MechMinion minion, MinionPose pose,
            Frame head, Frame upper, float partialTick) {
        double time = painter.time();
        double pulse = 0.85 + 0.15 * Math.sin(time * 0.3 + minion.getId());
        int visor = Colors.alpha(0.55 * pulse);
        Vec3 middle = head.at(0.0, 3.235, 0.36);
        painter.glowLine(middle, head.at(0.23, 3.29, 0.35), 0.3, LanternPainter.GREEN, visor);
        painter.glowLine(middle, head.at(-0.23, 3.29, 0.35), 0.3, LanternPainter.GREEN, visor);
        painter.flare(upper.at(0.0, MinionShapes.CORE_Y, MinionShapes.CORE_Z + 0.04), 0.24, 0.65 * pulse);
        if (minion.move() != MinionMoves.CANNON || pose.muzzle == null) {
            return;
        }
        double age = minion.moveAge(partialTick);
        if (age < MinionMoves.FIRE) {
            double charge = Ease.smooth(age / MinionMoves.FIRE);
            painter.flare(pose.muzzle, 0.1 + 0.4 * charge, 0.85);
            Vec3 along = pose.muzzle.subtract(head.at(0.0, 3.0, 0.0)).normalize();
            Vec3[] across = Vectors.across(along);
            for (int k = 0; k < 3; k++) {
                double t = (age * 0.25 + k / 3.0) % 1.0;
                Vec3 at = pose.muzzle.subtract(along.scale(0.7 * (1.0 - t)));
                double ring = 0.32 * (1.0 - t) + 0.06;
                painter.circle(at, across[0], across[1], ring, 0.025, 0.12, Colors.alpha(0.8 * charge * t),
                        Colors.alpha(0.4 * charge * t));
            }
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
        if (after < 8.0) {
            painter.flare(pose.muzzle, 0.12, 0.5 * (1.0 - after / 8.0));
        }
    }
}
