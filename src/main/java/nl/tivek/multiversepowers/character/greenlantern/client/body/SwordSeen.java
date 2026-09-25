package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordShield;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * The sword and shield seen from outside (see {@link SwordArms}): the arms and body posed for the move, the whole body
 * turned round for the spinning cut, and the sword and shield drawn where his hands are, with the streaks, sparks and
 * light that go with them.
 */
abstract class SwordSeen extends SwordStates {
    // Whether your own arms are being drawn in first person right now (the game poses them as it draws them).
    static boolean ownArms;
    // How many earlier moments the streak of a swung blade reaches back over, and how far apart they are, in ticks.
    private static final int TRAIL = 10;
    private static final float TRAIL_STEP = 0.45F;
    // How far apart the moments of the blur of the tossed sword's spin are, in ticks.
    private static final float TOSS_TRAIL_STEP = 0.2F;
    // The bodies turned round for the spinning cut while their model is drawn, by how far (see turnModel).
    static final Map<Integer, Float> SPUN = new HashMap<>();

    // ---- Seen from outside ----

    /** True while this body's pose is the sword and shield's (for the pose of the arms, see LanternArms). */
    static boolean posing(Entity player, float partialTick) {
        return state(player, partialTick) != null;
    }

    /**
     * Just before a body is drawn: for the spinning cut the whole body is turned round (about the upright line through its
     * feet) as far as the cut has spun it. Turned back once it is drawn (see {@link #unspin}).
     */
    static void spin(RenderPlayerEvent.Pre event) {
        State state = state(event.getEntity(), event.getPartialTick());
        if (state == null) {
            return;
        }
        float orbit = pose(event.getEntity(), state, event.getPartialTick()).orbit();
        if (Math.abs(orbit) < 1.0E-3F) {
            return;
        }
        SPUN.put(event.getEntity().getId(), orbit);
    }

    /**
     * While the game draws the model of the player being drawn (see PlayerRendererMixin): the body turns round for the
     * spinning cut. Only the model: his name over his head, drawn after it, stays where it is.
     */
    static void turnModel(AbstractClientPlayer player, PoseStack pose) {
        Float orbit = SPUN.get(player.getId());
        if (orbit != null) {
            pose.mulPose(Axis.YP.rotation(orbit));
        }
    }

    /** A body turned round for the spinning cut is done with once it is drawn. */
    static void unspin(RenderPlayerEvent.Post event) {
        SPUN.remove(event.getEntity().getId());
    }

    /**
     * One arm while the game poses the model: pointed at where the pose puts its hand (the sword hand, or the middle of
     * the shield), seen from its shoulder on the upper body as that turns into the move, and before the game bends the
     * upper body forward (see {@link #lean}), which it then goes along with. As the sword and shield take shape the
     * arms come out of the game's own pose, and once they have broken up they go back into it.
     */
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        SwordPoses.Pose pose = pose(entity, state, partialTick);
        float ours = 1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F);
        if (ours <= 0.0F) {
            return;
        }
        boolean right = arm == HumanoidArm.RIGHT;
        float twist = pose.twist();
        float[] aim = SwordPoses.aim(pose, right);
        ModelPart limb = right ? model.rightArm : model.leftArm;
        float sway = limb.xRot * 0.15F;
        limb.xRot = Mth.lerp(ours, limb.xRot, aim[0] + sway);
        limb.yRot = Mth.lerp(ours, limb.yRot, aim[1]);
        limb.zRot = Mth.lerp(ours, limb.zRot, 0.0F);
        model.body.yRot = twist * ours;
        model.rightArm.z = Mth.sin(twist) * 5.0F * ours;
        model.rightArm.x = Mth.lerp(ours, -5.0F, -Mth.cos(twist) * 5.0F);
        model.leftArm.z = -Mth.sin(twist) * 5.0F * ours;
        model.leftArm.x = Mth.lerp(ours, 5.0F, Mth.cos(twist) * 5.0F);
    }

    /**
     * Once the game has posed the whole body: the upper body bends forward into the move (or back a little), the arms
     * and head going with it, the hips going back and the legs stepping into a lunge; the layers of the skin follow.
     * While he takes them out his head follows the sword.
     */
    public static void lean(PlayerModel<?> model, LivingEntity entity) {
        if (ownArms) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        SwordPoses.Pose pose = pose(entity, state, partialTick);
        float[] watching = watching(BLENDS.get(entity.getId()), state, now(partialTick));
        if (watching != null) {
            float t = watching[0];
            float[] head = SwordPoses.head(t, state.move() == SwordMove.EQUIP ? pose : taking(t, now(partialTick)));
            float follow = head[2] * watching[1] * (1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F));
            model.head.xRot = Mth.lerp(follow, model.head.xRot, head[0]);
            model.head.yRot = Mth.lerp(follow, model.head.yRot, head[1]);
        }
        float lean = Mth.clamp(pose.lean(), -0.35F, 1.0F);
        float down = Math.max(0.0F, lean);
        float tilt = SwordPoses.TILT * lean;
        model.body.xRot += tilt;
        model.body.y += 3.2F * down;
        model.head.y += 4.2F * down;
        model.rightArm.y += 3.2F * down;
        model.leftArm.y += 3.2F * down;
        model.rightArm.xRot += tilt;
        model.leftArm.xRot += tilt;
        model.rightLeg.z += 3.9F * down;
        model.leftLeg.z += 3.9F * down;
        model.rightLeg.y += 0.2F * down;
        model.leftLeg.y += 0.2F * down;
        float step = Mth.clamp(pose.step(), 0.0F, 1.0F);
        if (step > 0.0F) {
            model.rightLeg.xRot = Mth.lerp(step, model.rightLeg.xRot, -0.7F);
            model.leftLeg.xRot = Mth.lerp(step, model.leftLeg.xRot, 0.5F);
        }
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    /**
     * The sword and shield of someone seen from outside (you too, from behind), where his hands were drawn: the sword
     * in his fist (or flying over his head, tossed up), the shield on his forearm, the streak of a swung blade, the
     * sparks of a bang on the face of the shield, and the light of a slam. While they take shape the ring feeds the
     * growing shield a beam of its light.
     */
    public static void draw(LanternPainter painter, Entity player, @Nullable Vec3 ring, float partialTick) {
        State state = state(player, partialTick);
        SwordSpot.Spot spot = SwordSpot.of(player);
        if (state == null || spot == null) {
            return;
        }
        SwordPoses.Pose pose = leading(player, state, partialTick);
        double apart = apart(state);
        if (apart >= 1.0) {
            return;
        }
        Vec3 bladeWay = SwordPoses.way(pose.blade());
        Vec3 edgeWay = SwordPoses.way(pose.edge());
        Blend blend = BLENDS.get(player.getId());
        float now = now(partialTick);
        float[] knock = watching(blend, state, now);
        if (knock != null) {
            // Swung right onto the face of the shield at each bang (and off it over a moment when a move cuts that
            // short).
            Vec3 turn = SwordPoses.bodyKnockTurn(knock[0]).scale(knock[1]);
            bladeWay = SwordPoses.turnedBy(bladeWay, turn);
            edgeWay = SwordPoses.turnedBy(edgeWay, turn);
        }
        Vec3 forward = spot.world(bladeWay, 0.0F);
        Vec3 edge = spot.world(edgeWay, 0.0F);
        float onto = knock == null ? 0.0F : SwordPoses.bodyKnocking(knock[0]) * knock[1];
        if (onto > 0.0F) {
            // His arms are drawn a little off from where the pose works them out (the game's own swing of the arms as
            // he walks, slim arms): the blade turns that much further, so it still lands on the face as drawn.
            Vec3 aim = spot.world(SwordPoses.bodyStrike(pose)[1], 0.0F);
            Vec3 off = spot.mount().subtract(spot.at(SwordPoses.drawn(pose, false)))
                    .subtract(spot.grip().subtract(spot.at(SwordPoses.drawn(pose, true))));
            Vec3 turn = SwordPoses.turnOnto(aim, aim.add(off.scale(onto)));
            forward = SwordPoses.turnedBy(forward, turn);
            edge = SwordPoses.turnedBy(edge, turn);
        }
        Vec3 grip = spot.grip();
        float flying = flying(state);
        if (flying >= 0.0F) {
            // Tossed up: it flies freely over his head and comes back down into his fist. The fist is drawn a little
            // off from where the pose works it out (the game's own swing of the arms as he walks): the flight takes
            // that along only where it leaves the fist and where it comes back into it.
            Vec3 off = spot.grip().subtract(spot.at(SwordPoses.drawn(pose, true)));
            Flying seen = at -> {
                SwordPoses.Flight flight = SwordPoses.bodyFlight(at);
                double u = Math.min(1.0, (at - SwordMove.TOSS) / (SwordMove.CATCH - SwordMove.TOSS));
                double along = Math.cos(Math.PI * u);
                return new SwordPoses.Flight(spot.at(flight.grip()).add(off.scale(along * along)),
                        spot.world(flight.blade(), 0.0F), spot.world(flight.edge(), 0.0F));
            };
            SwordPoses.Flight flight = seen.at(flying);
            if (apart <= 0.0) {
                tossTrail(painter, flying, SwordPoses.SWORD_SCALE, seen);
            }
            grip = flight.grip();
            forward = flight.blade();
            edge = flight.edge();
        }
        SwordPainter.sword(painter, grip, forward, edge, SwordPoses.SWORD_SCALE,
                SwordPoses.swordGrown(state.move(), formed(state)), apart);
        if (apart <= 0.0) {
            gleam(painter, state, grip, forward, SwordPoses.SWORD_SCALE);
        }
        Vec3 face = spot.world(SwordPoses.way(pose.face()), 0.0F);
        Vec3 top = spot.world(SwordPoses.way(pose.top()), 0.0F);
        Vec3 middle = spot.mount().add(face.scale(SwordPoses.SHIELD_OUT));
        double shieldGrown = SwordPoses.shieldGrown(state.move(), formed(state));
        SwordPainter.shield(painter, middle, face, top, SwordPoses.SHIELD_SCALE, shieldGrown, apart);
        if (apart > 0.0) {
            return;
        }
        if (ring != null && shieldGrown < 1.0) {
            painter.beam(ring, middle, feeding(shieldGrown), 0.6);
        }
        clang(painter, state, middle, face, top, SwordPoses.SHIELD_SCALE);
        // Where the middle of his chest is: worked back from where his fist was drawn.
        Vec3 chest = spot.grip().subtract(spot.world(SwordPoses.body(pose.hand()), 0.0F));
        trail(painter, state, (ago, from) -> {
            SwordPoses.Pose at = earlier(blend, state, now, ago);
            float turn = pose.orbit() - at.orbit();
            return chest.add(spot.world(SwordPoses.body(at.hand()).add(SwordPoses.way(at.blade())
                    .scale(from * SwordPoses.SWORD_SCALE)), turn));
        });
        slamLight(painter, state, chest.add(spot.world(new Vec3(0.0, 0.0, 1.3), 0.0F)).subtract(0.0,
                chest.y - player.getY(), 0.0));
    }

    /** How far the sword and shield have broken up: 0 while whole, gone at 1. */
    static double apart(State state) {
        return state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / SwordShield.BREAK_TICKS);
    }

    /**
     * How far into taking them out the tossed sword is flying right now (on past the catch when it broke up in the air:
     * its pieces fly on), or -1 while it is in the hand.
     */
    static float flying(State state) {
        if (state.move() != SwordMove.EQUIP) {
            return -1.0F;
        }
        if (state.broken() < 0.0F) {
            return SwordPoses.tossed(SwordMove.EQUIP, state.t()) >= 0.0F ? state.t() : -1.0F;
        }
        float broke = state.t() - state.broken();
        return broke > SwordMove.TOSS && broke < SwordMove.CATCH ? state.t() : -1.0F;
    }

    /** How strongly the ring's beam feeds the shield taking shape: fully until its rim runs round, then dying down. */
    static double feeding(double grown) {
        return 1.0 - Ease.smooth((grown - 0.55) / 0.45);
    }

    /**
     * The sparks of the blade banged on the face of the shield as they take shape, on the shield as it is drawn: they
     * spray up off the face, along the blade lying on it either way.
     */
    static void clang(LanternPainter painter, State state, Vec3 middle, Vec3 face, Vec3 top, double scale) {
        if (state.move() != SwordMove.EQUIP || state.t() < SwordMove.KNOCK) {
            return;
        }
        Vec3 ahead = face.normalize();
        Vec3 up = SwordPoses.square(top, ahead);
        Vec3 right = ahead.cross(up).normalize();
        Vec3 at = SwordPoses.onFace(middle, right, up, ahead, scale);
        SwordPainter.clang(painter, at, right, ahead, state.t() - SwordMove.KNOCK, scale);
        SwordPainter.clang(painter, at, right, ahead, state.t() - SwordMove.KNOCK_AGAIN, scale * 0.8);
    }

    /** Where a point along the blade was {@code ago} ticks back: {@code from} (in blocks at scale 1) out from the grip. */
    interface Blade {
        Vec3 at(float ago, double from);
    }

    /** Where the tossed sword is {@code t} ticks into taking them out. */
    interface Flying {
        SwordPoses.Flight at(float t);
    }

    /**
     * The blur of the tossed sword turning over in the air: a streak of light where its tip and blade were over the
     * last moments, strongest high in its flight.
     */
    static void tossTrail(LanternPainter painter, float t, double scale, Flying flying) {
        double u = Mth.clamp((t - SwordMove.TOSS) / (SwordMove.CATCH - SwordMove.TOSS), 0.0, 1.0);
        double strength = 0.7 * Math.sin(u * Math.PI);
        if (strength <= 0.02) {
            return;
        }
        List<Vec3> tips = new ArrayList<>(TRAIL);
        List<Vec3> roots = new ArrayList<>(TRAIL);
        for (int k = 0; k < TRAIL; k++) {
            SwordPoses.Flight at = flying.at(Math.max(SwordMove.TOSS, t - k * TOSS_TRAIL_STEP));
            tips.add(at.grip().add(at.blade().scale(SwordPainter.TIP * scale)));
            roots.add(at.grip().add(at.blade().scale(0.5 * scale)));
        }
        SwordPainter.trail(painter, tips, roots, strength);
    }

    /**
     * As he looks the sword over, a gleam of light runs up its blade from the guard to the tip: a bright spark with a
     * glint along the blade round it.
     */
    static void gleam(LanternPainter painter, State state, Vec3 grip, Vec3 blade, double scale) {
        float u = SwordPoses.gleam(state.move(), state.t());
        if (u < 0.0F) {
            return;
        }
        double shine = Math.sin(u * Math.PI);
        Vec3 way = blade.normalize();
        Vec3 at = grip.add(way.scale(Mth.lerp(u, 0.35, SwordPainter.TIP * 0.95) * scale));
        painter.flare(at, 0.22 * scale * (0.6 + 0.4 * shine), shine);
        painter.edge(at.subtract(way.scale(0.22 * scale)), at.add(way.scale(0.22 * scale)), 0.03 * scale, shine);
    }

    /**
     * The streak a swung blade leaves: while a cut or thrust whips through, a sheet of light between where the tip and
     * the root of the blade were over the last few moments.
     */
    static void trail(LanternPainter painter, State state, Blade blade) {
        SwordMove move = state.move();
        if (move.kind() != SwordMove.Kind.ATTACK && move != SwordMove.FLURRY) {
            return;
        }
        int[] hits = move.hits();
        float first = move == SwordMove.FLURRY ? SwordMove.FIRST_STAB : hits[0];
        float last = move == SwordMove.FLURRY ? SwordMove.FIRST_STAB + (SwordMove.STABS - 1) * SwordMove.STAB_EVERY
                : hits[hits.length - 1];
        float t = state.t();
        if (t < first - 3.0F || t > last + 3.0F) {
            return;
        }
        double strength = 1.0 - Math.max(0.0, Math.abs(t - Mth.clamp(t, first - 1.5F, last + 0.8F)) / 1.8);
        List<Vec3> tips = new ArrayList<>(TRAIL);
        List<Vec3> roots = new ArrayList<>(TRAIL);
        for (int k = 0; k < TRAIL; k++) {
            float ago = Math.min(t, k * TRAIL_STEP);
            tips.add(blade.at(ago, SwordPainter.TIP));
            roots.add(blade.at(ago, 0.5));
        }
        SwordPainter.trail(painter, tips, roots, Mth.clamp(strength, 0.0, 1.0) * (move == SwordMove.FLURRY ? 0.6 : 1.0));
    }

    /** The end of a charge: rings of light running out over the ground from where the shield struck it. */
    private static void slamLight(LanternPainter painter, State state, Vec3 at) {
        if (state.move() != SwordMove.SLAM) {
            return;
        }
        float since = state.t() - SwordMove.SLAM.hits()[0];
        if (since < 0.0F || since > 14.0F) {
            return;
        }
        double radius = wheel().value("slamRadius");
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        for (int k = 0; k < 2; k++) {
            double ring = since - k * 3.0;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 9.0), 2.0);
            double fade = Math.max(0.0, 1.0 - ring / 11.0);
            painter.circle(at.add(0.0, 0.1, 0.0), east, south, 0.4 + radius * wave, 0.06, 0.45,
                    Colors.alpha(fade), Colors.alpha(0.5 * fade));
        }
        if (since < 3.0F) {
            painter.flare(at.add(0.0, 0.3, 0.0), 0.8 * (1.0 - since / 3.0F), 1.0 - since / 3.0F);
        }
    }
}
