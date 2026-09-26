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
import nl.tivek.multiversepowers.character.greenlantern.ability.EnergyWhip;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.WhipPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;

abstract class WhipSeen extends WhipStates {
    private static final float MODEL_PIXEL = 0.9375F / 16.0F;
    private static final Map<Integer, Turned> TURNED = new HashMap<>();
    private static final int TRAIL = 6;
    private static final float TRAIL_STEP = 0.3F;
    // How much of the look up or down the handle takes on in third person, where the arms keep level.
    private static final double PITCH_SHARE = 0.6;
    private static final float CRACK_TICKS = 5.0F;

    private record Turned(float orbit, float drop, @Nullable float[] knees) {
    }

    static boolean posing(Entity player, float partialTick) {
        return state(player, partialTick) != null;
    }

    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        WhipCurves.Pose pose = pose(entity, state, partialTick);
        float ours = ours(pose);
        if (ours <= 0.0F) {
            return;
        }
        boolean right = arm == HumanoidArm.RIGHT;
        FlameBody.Torso torso = WhipPoses.torso(pose, ours);
        float weight = right ? ours : ours * pose.leftOn();
        if (weight > 0.0F) {
            Vec3 shoulder = torso.shoulder(right);
            float[] aim = FlameBody.reach(shoulder, FlameBody.model(WhipPoses.body(right ? pose.grip()
                    : pose.left())), torso.twist());
            ModelPart limb = right ? model.rightArm : model.leftArm;
            float sway = limb.xRot * 0.1F;
            limb.xRot = Mth.lerp(weight, limb.xRot, aim[0] + sway);
            limb.yRot = Mth.lerp(weight, limb.yRot, aim[1]);
            limb.zRot = Mth.lerp(weight, limb.zRot, 0.0F);
        }
        shoulders(model, torso);
    }

    private static void shoulders(HumanoidModel<?> model, FlameBody.Torso torso) {
        Vec3 right = torso.shoulder(true);
        Vec3 left = torso.shoulder(false);
        model.rightArm.setPos((float) right.x, (float) right.y, (float) right.z);
        model.leftArm.setPos((float) left.x, (float) left.y, (float) left.z);
    }

    public static void lean(PlayerModel<?> model, LivingEntity entity) {
        if (SwordSeen.ownArms) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        WhipCurves.Pose pose = pose(entity, state, partialTick);
        float ours = ours(pose);
        if (ours <= 0.0F) {
            return;
        }
        float[] watching = watching(BLENDS.get(entity.getId()), state, now(partialTick));
        if (watching != null) {
            WhipCurves.Pose equip = state.move() == WhipMove.EQUIP ? pose
                    : WhipPoses.at(WhipMove.EQUIP, watching[0], time(partialTick), WhipKeys.REST, null, 0.0);
            float[] head = WhipPoses.head(watching[0], equip);
            float follow = head[2] * watching[1] * ours;
            model.head.xRot = Mth.lerp(follow, model.head.xRot, head[0]);
            model.head.yRot = Mth.lerp(follow, model.head.yRot, head[1]);
        }
        FlameBody.Torso torso = WhipPoses.torso(pose, ours);
        Vec3 neck = torso.neck();
        model.body.setPos((float) neck.x, (float) neck.y, (float) neck.z);
        model.body.setRotation(torso.bend(), torso.twist(), -torso.roll());
        model.head.setPos((float) neck.x, (float) neck.y, (float) neck.z);
        shoulders(model, torso);
        float stand = ours * standing(entity, partialTick);
        FlameBody.Legs legs = WhipPoses.legs(pose, stand);
        float hold = stand * Mth.clamp(Math.max(Math.max(pose.squat(), pose.kneel()), 2.0F * Math.max(
                Math.abs(pose.step()), pose.wide())), 0.0F, 1.0F);
        legs(model.rightLeg, hold, legs.rightThigh(), legs.spread(), FlameBody.hip(true), ours);
        legs(model.leftLeg, hold, legs.leftThigh(), -legs.spread(), FlameBody.hip(false), ours);
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    // Walking takes the legs back: the stance only shows while standing still, so the feet never slide.
    private static float standing(LivingEntity entity, float partialTick) {
        return 1.0F - (float) Ease.smooth(Math.min(1.0F, entity.walkAnimation.speed(partialTick) * 3.0F));
    }

    private static void legs(ModelPart leg, float hold, float thigh, float spread, Vec3 hip, float ours) {
        leg.xRot = Mth.lerp(hold, leg.xRot, thigh);
        leg.yRot = Mth.lerp(hold, leg.yRot, 0.0F);
        leg.zRot = Mth.lerp(hold, leg.zRot, spread);
        leg.setPos(Mth.lerp(ours, leg.x, (float) hip.x), Mth.lerp(ours, leg.y, (float) hip.y),
                Mth.lerp(ours, leg.z, (float) hip.z));
    }

    public static void draw(LanternPainter painter, Entity player, @Nullable Vec3 ring, float partialTick) {
        State state = state(player, partialTick);
        HandSpot.Spot spot = HandSpot.of(player);
        if (state == null || spot == null) {
            return;
        }
        WhipCurves.Pose pose = pose(player, state, partialTick);
        double apart = apart(state);
        if (apart >= 1.0) {
            return;
        }
        Vec3 view = WhipLine.tipped(pose.handle(), player.getViewXRot(partialTick), PITCH_SHARE);
        Vec3 forward = spot.world(WhipPoses.way(view), 0.0F);
        Vec3 up = spot.world(WhipPoses.way(pose.top()), 0.0F);
        ConstructPainter.Frame handle = WhipPainter.held(spot.grip(), forward, up, WhipCurves.WHIP_SCALE);
        WhipPainter.handle(painter, handle, grown(state), apart);
        if (ring != null && grown(state) < 1.0 && apart <= 0.0) {
            painter.beam(ring, spot.grip(), 1.0 - grown(state), 0.5);
        }
        lash(painter, player, state, handle.at(0.0, 0.0, WhipPainter.TIP), forward, WhipCurves.WHIP_SCALE,
                partialTick, false, apart);
    }

    static double apart(State state) {
        return state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / EnergyWhip.BREAK_TICKS);
    }

    static double grown(State state) {
        return state.move() != WhipMove.EQUIP ? 1.0 : Ease.smooth(formed(state) / WhipMove.FORMED);
    }

    // The lash and all its light: the ember at the tip, the streak behind a fast lash, the cracks, the whirl's ring,
    // the spinning shield's disc and the lasso's grip on a creature.
    static void lash(LanternPainter painter, Entity player, State state, Vec3 root, Vec3 handle, double scale,
            float partialTick, boolean own, double apart) {
        // In first person the world is drawn before the hands: the blend must know of a new move already.
        pose(player, state, partialTick);
        Blend blend = BLENDS.get(player.getId());
        float time = time(partialTick);
        WhipLine.Hold hold = WhipLine.hold(player, root, handle.normalize(), partialTick, time);
        float t = state.t();
        Vec3[] points = WhipLine.at(player, blend, state, hold, t, time);
        Vec3 normal = hold.look().right();
        double hot = state.move() == WhipMove.EQUIP ? 1.0 - Ease.smooth((formed(state) - WhipMove.FORMED)
                / (WhipMove.POURED + 4.0 - WhipMove.FORMED)) : 0.0;
        WhipPainter.lash(painter, points, normal, scale, hot, apart, own ? painter.camera() : null);
        if (apart > 0.0) {
            return;
        }
        Vec3 tip = points[points.length - 1];
        WhipPainter.ember(painter, tip, scale, 0.25 + 0.75 * hot);
        streak(painter, player, blend, state, hold, t, time);
        cracks(painter, player, blend, state, hold, t, time);
        WhipMove move = state.move();
        if (move == WhipMove.WHIRL || move == WhipMove.WHIRL_CRACK && t < 8.0F) {
            double spun = move == WhipMove.WHIRL ? Ease.smooth(t / WhipMove.WHIRL_UP) : 1.0 - Ease.smooth(t / 8.0);
            double radius = Math.sqrt((tip.x - root.x) * (tip.x - root.x) + (tip.z - root.z) * (tip.z - root.z));
            WhipPainter.ring(painter, new Vec3(root.x, tip.y, root.z), radius, 0.8 * spun);
        }
        if (move == WhipMove.SPIN_SHIELD || move == WhipMove.SPIN_END && t < 6.0F) {
            double spun = move == WhipMove.SPIN_SHIELD ? Ease.smooth(t / WhipMove.SPIN_UP) : 1.0 - Ease.smooth(t / 6.0);
            double radius = hold.length() * WhipMove.SPIN_REACH * Math.sin(WhipMove.SPIN_CONE);
            Vec3 ahead = hold.look().ahead();
            Vec3 center = root.add(ahead.scale(hold.length() * WhipMove.SPIN_REACH * Math.cos(WhipMove.SPIN_CONE)
                    * 0.7));
            WhipPainter.disc(painter, center, ahead, radius * 0.95, -WhipMove.spinTurn(t), spun, own);
        }
        if (move == WhipMove.LASSO) {
            lasso(painter, player, t, partialTick);
        }
    }

    // A fast lash leaves a streak of light behind its end.
    private static void streak(LanternPainter painter, Entity player, Blend blend, State state, WhipLine.Hold hold,
            float t, float time) {
        WhipMove move = state.move();
        boolean fast = switch (move.kind()) {
            case ATTACK -> t > 2.0F && t < move.ticks();
            case WHIRL, WHIRL_CRACK -> true;
            case EQUIP -> t > WhipMove.TWIRL - 2.0F && t < WhipMove.THROW + 8.0F;
            case LASSO -> t > 3.0F && t < WhipMove.LASSO_REACH + 1.0F;
            default -> false;
        };
        if (!fast) {
            return;
        }
        List<Vec3> tips = new ArrayList<>(TRAIL);
        List<Vec3> roots = new ArrayList<>(TRAIL);
        for (int k = 0; k < TRAIL; k++) {
            Vec3[] at = WhipLine.at(player, blend, state, hold, t - k * TRAIL_STEP, time - k * TRAIL_STEP);
            tips.add(at[at.length - 1]);
            roots.add(at[(int) (at.length * 0.8)]);
        }
        double speed = tips.get(0).distanceTo(tips.get(1)) / TRAIL_STEP / Math.max(0.5, hold.length());
        WhipPainter.trail(painter, tips, roots, 0.7 * (speed - 0.2) / 0.5);
    }

    private static void cracks(LanternPainter painter, Entity player, Blend blend, State state, WhipLine.Hold hold,
            float t, float time) {
        for (WhipMove.Crack crack : state.move().cracks()) {
            float since = t - crack.tick();
            if (since >= 0.0F && since <= CRACK_TICKS) {
                crack(painter, WhipLine.at(player, blend, state, hold, crack.tick(), time - since), since,
                        crack.strength());
            }
        }
        if (state.move() == WhipMove.LASSO) {
            float since = t - WhipMove.LASSO_REACH;
            if (since >= 0.0F && since <= CRACK_TICKS) {
                crack(painter, WhipLine.at(player, blend, state, hold, WhipMove.LASSO_REACH, time - since), since,
                        0.7F);
            }
        }
    }

    private static void crack(LanternPainter painter, Vec3[] at, float since, float strength) {
        Vec3 tip = at[at.length - 1];
        WhipPainter.crack(painter, tip, tip.subtract(at[at.length - 3]), since, strength);
    }

    // The coils flash as they close on the creature, and a ring of light runs out where it smacks down.
    private static void lasso(LanternPainter painter, Entity player, float t, float partialTick) {
        Entity target = WhipLine.caught(player);
        if (target == null) {
            return;
        }
        Vec3 middle = target.getPosition(partialTick).add(0.0, target.getBbHeight() * 0.5, 0.0);
        float closed = t - WhipMove.LASSO_WRAPPED;
        if (closed >= 0.0F && closed < 4.0F) {
            painter.flare(middle, 0.5 + target.getBbWidth() * 0.4, 1.0 - closed / 4.0F);
        }
        float landed = t - WhipMove.LASSO_LAND;
        if (landed >= 0.0F && landed < 10.0F) {
            double u = landed / 10.0;
            double fade = (1.0 - u) * (1.0 - u);
            Vec3 feet = target.getPosition(partialTick).add(0.0, 0.08, 0.0);
            painter.circle(feet, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 0.4 + 2.2 * Math.sqrt(u), 0.05,
                    0.35, Colors.alpha(0.9 * fade), Colors.alpha(0.45 * fade));
            if (landed < 3.0F) {
                painter.flare(feet.add(0.0, 0.3, 0.0), 0.8 * (1.0 - landed / 3.0F), 1.0 - landed / 3.0F);
            }
        }
    }

    // The spin turns the whole body round and bent knees sink it: stored before the model is drawn, used as it is
    // turned and as its legs are drawn.
    static void spin(RenderPlayerEvent.Pre event) {
        State state = state(event.getEntity(), event.getPartialTick());
        if (state == null) {
            return;
        }
        WhipCurves.Pose pose = pose(event.getEntity(), state, event.getPartialTick());
        float ours = ours(pose);
        FlameBody.Legs legs = WhipPoses.legs(pose, ours * standing(event.getEntity(), event.getPartialTick()));
        boolean bent = FlameBody.bent(legs) && !event.getEntity().isInvisible();
        if (Math.abs(pose.orbit()) >= 1.0E-3F || Math.abs(legs.drop()) >= 1.0E-3F || bent) {
            TURNED.put(event.getEntity().getId(), new Turned(pose.orbit(), legs.drop(), bent ? FlameBody.knees(legs)
                    : null));
        }
        if (bent) {
            // The game's legs cannot bend at the knee: hidden here and drawn in two halves instead (see KneelLegs).
            PlayerModel<AbstractClientPlayer> model = event.getRenderer().getModel();
            model.rightLeg.visible = false;
            model.leftLeg.visible = false;
            model.rightPants.visible = false;
            model.leftPants.visible = false;
            model.rightLeg.yScale = 0.5F;
            model.leftLeg.yScale = 0.5F;
        }
    }

    static void turnModel(AbstractClientPlayer player, PoseStack pose) {
        Turned turned = TURNED.get(player.getId());
        if (turned != null) {
            pose.translate(0.0F, -turned.drop() * MODEL_PIXEL, 0.0F);
            pose.mulPose(Axis.YP.rotation(turned.orbit()));
        }
    }

    @Nullable
    static float[] knees(LivingEntity entity) {
        Turned turned = TURNED.get(entity.getId());
        return turned == null ? null : turned.knees();
    }

    static void unspin(RenderPlayerEvent.Post event) {
        TURNED.remove(event.getEntity().getId());
    }
}
