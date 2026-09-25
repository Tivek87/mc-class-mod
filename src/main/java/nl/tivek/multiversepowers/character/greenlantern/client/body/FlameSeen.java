package nl.tivek.multiversepowers.character.greenlantern.client.body;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
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
        float twist = pose.twist();
        Vec3 target = right ? FlamePoses.body(pose.grip()) : FlamePoses.leftTarget(pose);
        float[] aim = FlamePoses.aim(target, twist, pose.lean(), right);
        ModelPart limb = right ? model.rightArm : model.leftArm;
        float sway = limb.xRot * 0.1F;
        limb.xRot = Mth.lerp(ours, limb.xRot, aim[0] + sway);
        limb.yRot = Mth.lerp(ours, limb.yRot, aim[1]);
        limb.zRot = Mth.lerp(ours, limb.zRot, 0.0F);
        model.body.yRot = twist * ours;
        model.rightArm.z = Mth.sin(twist) * 5.0F * ours;
        model.rightArm.x = Mth.lerp(ours, -5.0F, -Mth.cos(twist) * 5.0F);
        model.leftArm.z = -Mth.sin(twist) * 5.0F * ours;
        model.leftArm.x = Mth.lerp(ours, 5.0F, Mth.cos(twist) * 5.0F);
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
        float[] watching = watching(BLENDS.get(entity.getId()), state, now(partialTick));
        if (watching != null) {
            float[] head = FlamePoses.head(watching[0], pose);
            float follow = head[2] * watching[1] * ours;
            model.head.xRot = Mth.lerp(follow, model.head.xRot, head[0]);
            model.head.yRot = Mth.lerp(follow, model.head.yRot, head[1]);
        }
        float lean = Mth.clamp(pose.lean(), -0.35F, 1.0F) * ours;
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
        float step = Mth.clamp(pose.step(), 0.0F, 1.0F) * ours;
        if (step > 0.0F) {
            model.rightLeg.xRot = Mth.lerp(step, model.rightLeg.xRot, 0.45F);
            model.leftLeg.xRot = Mth.lerp(step, model.leftLeg.xRot, -0.5F);
            model.leftLeg.zRot = Mth.lerp(step, model.leftLeg.zRot, -0.08F);
            model.rightLeg.zRot = Mth.lerp(step, model.rightLeg.zRot, 0.1F);
        }
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
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
        Vec3 forward = spot.world(SwordPoses.way(pose.aim(1.0)), 0.0F);
        Vec3 up = spot.world(SwordPoses.way(pose.up(1.0)), 0.0F);
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
            case SWEEP -> {
                if (FlameMove.spraying(move, t)) {
                    FireStream.feed(player, FireStream.Kind.SWEEP, nozzle, turned(look,
                            FlameMove.sweepYaw(move, t)), now);
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

    // A world way turned right by the degrees about the world's up.
    static Vec3 turned(Vec3 look, double degrees) {
        double angle = Math.toRadians(degrees);
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        if (flat.lengthSqr() < 1.0E-8) {
            return look;
        }
        Vec3 right = new Vec3(-flat.z, 0.0, flat.x).normalize().scale(flat.length());
        Vec3 swung = flat.scale(Math.cos(angle)).add(right.scale(Math.sin(angle)));
        return new Vec3(swung.x, look.y, swung.z);
    }
}
