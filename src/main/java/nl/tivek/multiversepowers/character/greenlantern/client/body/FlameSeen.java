package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.HashMap;
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
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameWall;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flamethrower;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FirePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FireStream;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FlamePainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.math.Ease;

abstract class FlameSeen extends FlameStates {
    private static final float MODEL_PIXEL = 0.9375F / 16.0F;
    private static final Map<Integer, Turned> TURNED = new HashMap<>();

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
        FlameCurves.Pose pose = pose(entity, state, partialTick);
        float ours = 1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F);
        if (ours <= 0.0F) {
            return;
        }
        boolean right = arm == HumanoidArm.RIGHT;
        FlameBody.Torso torso = FlameBody.torso(pose, ours);
        float[][] arms = arms(pose, torso);
        float[] aim = right ? arms[0] : arms[1];
        ModelPart limb = right ? model.rightArm : model.leftArm;
        float sway = limb.xRot * 0.1F;
        limb.xRot = Mth.lerp(ours, limb.xRot, aim[0] + sway);
        limb.yRot = Mth.lerp(ours, limb.yRot, aim[1]);
        limb.zRot = Mth.lerp(ours, limb.zRot, 0.0F);
        shoulders(model, torso);
    }

    // Both arms, right then left: the right fist to the grip, the left hand to wherever it goes from there.
    static float[][] arms(FlameCurves.Pose pose, FlameBody.Torso torso) {
        Vec3 shoulder = torso.shoulder(true);
        float[] right = FlameBody.reach(shoulder, FlameBody.model(FlamePoses.body(pose.grip())), torso.twist());
        Vec3 fist = FlameBody.body(FlameBody.fist(shoulder, right, true));
        float[] left = FlameBody.reach(torso.shoulder(false), FlameBody.model(FlamePoses.leftTarget(pose, fist)),
                torso.twist());
        return new float[][] { right, left };
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
        FlameCurves.Pose pose = pose(entity, state, partialTick);
        float ours = 1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F);
        if (ours <= 0.0F) {
            return;
        }
        float[] watching = watching(BLENDS.get(entity.getId()), state, now(partialTick));
        if (watching != null) {
            float[] head = FlamePoses.head(watching[0], pose);
            float follow = head[2] * watching[1] * ours;
            model.head.xRot = Mth.lerp(follow, model.head.xRot, head[0]);
            model.head.yRot = Mth.lerp(follow, model.head.yRot, head[1]);
        }
        FlameBody.Torso torso = FlameBody.torso(pose, ours);
        Vec3 neck = torso.neck();
        model.body.setPos((float) neck.x, (float) neck.y, (float) neck.z);
        model.body.setRotation(torso.bend(), torso.twist(), -torso.roll());
        model.head.setPos((float) neck.x, (float) neck.y, (float) neck.z);
        shoulders(model, torso);
        FlameBody.Legs legs = FlameBody.legs(pose, ours);
        float hold = ours * Mth.clamp(Math.max(Math.max(pose.squat(), pose.kneel()), 2.0F * Math.max(
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
        FlameCurves.Pose pose = pose(player, state, partialTick);
        double apart = apart(state);
        if (apart >= 1.0) {
            return;
        }
        Vec3 forward = spot.world(FlamePoses.way(pose.aim(1.0)), 0.0F);
        Vec3 up = spot.world(FlamePoses.way(pose.up(1.0)), 0.0F);
        ConstructPainter.Frame gun = FlamePainter.held(spot.grip(), forward, up, FlameCurves.GUN_SCALE);
        FlamePainter.gun(painter, gun, formed(state), FlamePoses.glow(state.move(), state.t(), heat(player),
                state.firing()), apart, ring);
        if (apart > 0.0) {
            return;
        }
        Vec3 nozzle = gun.at(FlamePainter.NOZZLE.x, FlamePainter.NOZZLE.y, FlamePainter.NOZZLE.z);
        feed(player, state, nozzle, gun.forward().normalize(), partialTick);
        around(painter, player, state, partialTick, false);
    }

    static double apart(State state) {
        return state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / Flamethrower.BREAK_TICKS);
    }

    static void feed(Entity player, State state, Vec3 nozzle, Vec3 muzzle, float partialTick) {
        double now = FireStream.now(partialTick);
        FlameMove move = state.move();
        float t = state.t();
        Vec3 look = player.getViewVector(partialTick);
        switch (move.kind()) {
            case ATTACK -> {
                FlameMove.Aim aim = move.aim(t);
                if (aim != null) {
                    FireStream.feed(player, FireStream.Kind.SWEEP, nozzle, FlameMove.way(look, aim), now,
                            FireStream.Kind.SWEEP.speed * move.reach());
                }
            }
            case INFERNO -> {
                if (state.firing() && t >= FlameMove.BRACE) {
                    double range = wheel().value("infernoRange");
                    Vec3 aim = player.getEyePosition(partialTick).add(look.scale(range));
                    FireStream.feed(player, FireStream.Kind.STREAM, nozzle, aim.subtract(nozzle), now,
                            FireStream.Kind.STREAM.speed * range / 10.0);
                }
            }
            case EQUIP -> {
                if (t >= FlameMove.TEST && t < FlameMove.TEST + FlameMove.TEST_TICKS) {
                    FireStream.feed(player, FireStream.Kind.BLAST, nozzle, muzzle, now);
                } else if (t >= FlameMove.HISS && t < FlameMove.HISS + 10) {
                    FireStream.feed(player, FireStream.Kind.SMOKE, nozzle, muzzle.add(0.0, 1.5, 0.0), now);
                }
            }
            case VENT -> {
                if (t < 10.0F) {
                    FireStream.feed(player, FireStream.Kind.SMOKE, nozzle, muzzle.add(0.0, 1.5, 0.0), now);
                }
            }
            case WALL -> {
                if (t >= FlameMove.LAY_FROM && t < FlameMove.LAY_TO) {
                    Vec3 target = laying(player, t, look);
                    if (target != null) {
                        Vec3 way = target.subtract(nozzle);
                        FireStream.feed(player, FireStream.Kind.LAY, nozzle, way, now,
                                way.length() / (FireStream.Kind.LAY.life * 0.55));
                    }
                }
            }
            case VORTEX -> {
                if (state.swirling()) {
                    double around = time(partialTick) * FirePainter.VORTEX_SPIN;
                    double radius = wheel().value("vortexRadius") * 0.6;
                    Vec3 feet = player.getPosition(partialTick);
                    Vec3 target = feet.add(Math.cos(around) * radius, 0.2, Math.sin(around) * radius);
                    Vec3 way = target.subtract(nozzle);
                    FireStream.feed(player, FireStream.Kind.FEED, nozzle, way, now,
                            way.length() / (FireStream.Kind.FEED.life * 0.55));
                }
            }
            case BURST -> {
            }
        }
    }

    @Nullable
    private static Vec3 laying(Entity player, float t, Vec3 look) {
        ClientConstructs.Wall wall = ClientConstructs.wall(player.getId());
        Vec3 center;
        Vec3 normal;
        double width;
        if (wall != null) {
            center = wall.center();
            normal = wall.normal();
            width = wall.width();
        } else {
            center = FlameWall.base(player.level(), player, look);
            if (center == null) {
                return null;
            }
            Vec3 flat = new Vec3(look.x, 0.0, look.z);
            normal = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
            width = wheel().value("wallWidth");
        }
        Vec3 along = new Vec3(normal.z, 0.0, -normal.x);
        return center.add(along.scale((FlameMove.laid(t + 1.0) - 0.5) * width)).add(0.0, 0.2, 0.0);
    }

    static void around(LanternPainter painter, Entity player, State state, float partialTick, boolean inside) {
        FlameMove move = state.move();
        double radius = wheel().value("vortexRadius");
        Vec3 feet = player.getPosition(partialTick);
        if (move == FlameMove.VORTEX) {
            FirePainter.vortex(painter, feet, radius, Math.min(1.0, state.t() / FlameMove.SPIN_UP),
                    state.swirling() ? 1.0 : 0.6, inside);
        } else if (move == FlameMove.BURST) {
            double fade = 1.0 - Ease.smooth(state.t() / (FlameMove.BLAST + 3.0));
            FirePainter.vortex(painter, feet, radius * (1.0 + 0.3 * (1.0 - fade)), 1.0, fade, inside);
            FirePainter.burst(painter, feet, radius, state.t() - FlameMove.BLAST);
        }
    }

    // The spin turns the whole body round and bent knees sink it: stored before the model is drawn, used as it is
    // turned and as its legs are drawn.
    static void spin(RenderPlayerEvent.Pre event) {
        State state = state(event.getEntity(), event.getPartialTick());
        if (state == null) {
            return;
        }
        FlameCurves.Pose pose = pose(event.getEntity(), state, event.getPartialTick());
        float ours = 1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F);
        FlameBody.Legs legs = FlameBody.legs(pose, ours);
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
