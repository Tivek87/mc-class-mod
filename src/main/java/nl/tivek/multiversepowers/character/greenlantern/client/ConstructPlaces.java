package nl.tivek.multiversepowers.character.greenlantern.client;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPath;
import nl.tivek.multiversepowers.character.greenlantern.ConstructPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightBeam;
import nl.tivek.multiversepowers.character.greenlantern.ability.LightShield;
import nl.tivek.multiversepowers.character.greenlantern.client.body.LanternArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RingSpot;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Vector3f;

final class ConstructPlaces {
    // Nearer than the hand than it looks from outside, so the beam never starts inside a wall.
    private static final double RING_NEAR = 0.45;
    private static final double RAM_LOOK = 0.08;
    private static final double RAM_ALONG = 0.3;
    static final double RAM_OWN_AHEAD = 0.85;

    private ConstructPlaces() {
    }

    static Vec3 hung(Entity owner, Vec3 spot, float partialTick) {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, owner.getViewYRot(partialTick));
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        return owner.getEyePosition(partialTick).add(right.scale(spot.x)).add(0.0, spot.y, 0.0)
                .add(ahead.scale(spot.z));
    }

    static Vec3 pane(Entity owner, float partialTick) {
        return owner.getEyePosition(partialTick).add(owner.getViewVector(partialTick).scale(LightShield.AHEAD));
    }

    static void on(Track track, @Nullable Entity owner, float partialTick) {
        ConstructPath path = track.path;
        ConstructPayload latest = track.latest;
        boolean moving = latest.path() != null;
        if (path == null) {
            return;
        }
        if (!path.steered()) {
            double travelled = path.travelled(moving ? track.clock(partialTick) : track.told);
            track.lastCenter = moving ? path.along(travelled, null) : latest.center();
            track.lastWay = path.way(travelled, null);
            return;
        }
        if (moving && owner != null) {
            ConstructPath.Sight sight = ConstructPath.Sight.of(owner.getEyePosition(partialTick),
                    owner.getViewYRot(partialTick), owner.getViewXRot(partialTick));
            double travelled = path.travelled(track.clock(partialTick));
            track.lastCenter = path.along(travelled, sight);
            track.lastWay = path.way(travelled, sight);
        } else if (track.lastCenter == null || moving) {
            track.lastCenter = track.previous.center().lerp(track.current.center(), partialTick);
            track.lastWay = latest.facing();
        }
    }

    static Vec3 where(ConstructPayload was, ConstructPayload now, @Nullable Entity owner, float partialTick) {
        if (now.held() && now.shape() == ConstructPayload.FIST) {
            Vec3 spot = was.held() ? was.center().lerp(now.center(), partialTick) : now.center();
            return owner == null ? now.center() : hung(owner, spot, partialTick);
        }
        return was.center().lerp(now.center(), partialTick);
    }

    static Vec3 heldFacing(Entity owner, float partialTick) {
        return Vec3.directionFromRotation(Mth.clamp(owner.getViewXRot(partialTick), -25.0F, 25.0F),
                owner.getViewYRot(partialTick));
    }

    static Vec3 ramWay(Entity owner, float partialTick) {
        Vec3 look = owner.getViewVector(partialTick);
        Vec3 moving = ClientFlight.velocity(owner, partialTick);
        double speed = moving.length();
        double along = Ease.smooth((speed - RAM_LOOK) / (RAM_ALONG - RAM_LOOK));
        if (along <= 0.0) {
            return look;
        }
        Vec3 way = look.scale(1.0 - along).add(moving.scale(along / speed));
        return way.lengthSqr() < 1.0E-6 ? look : way.normalize();
    }

    static Vec3 beamEnd(ClientLevel level, Entity owner, Vec3 way, ConstructPayload now, float partialTick) {
        Vec3 eye = owner.getEyePosition(partialTick);
        if (owner == Minecraft.getInstance().player) {
            CharacterAbility bolt = GameCharacter.GREEN_LANTERN.byName("light_bolt");
            double range = bolt == null ? 40.0 : LightBeam.range(bolt, now.variant());
            Vec3 far = eye.add(way.scale(range));
            BlockHitResult hit = level.clip(new ClipContext(eye, far, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, owner));
            return hit.getType() == HitResult.Type.MISS ? far : hit.getLocation();
        }
        return eye.add(way.scale(Math.max(0.5, now.size())));
    }

    static Vec3 ringHand(Minecraft minecraft, Camera camera, Entity owner, float partialTick,
            RenderLevelStageEvent event) {
        Vec3 seen = RingSpot.of(owner, camera, event.getProjectionMatrix(), event.getModelViewMatrix());
        if (seen != null) {
            return seen;
        }
        if (owner == minecraft.player && camera.getEntity() == owner && !camera.isDetached()) {
            Vector3f hand = LanternArms.handPoint(minecraft.player, partialTick);
            Vec3 forward = new Vec3(camera.getLookVector());
            Vec3 up = new Vec3(camera.getUpVector());
            Vec3 left = new Vec3(camera.getLeftVector());
            // hand.z() is negative forward (model space), hand.x() positive right: signs below undo that.
            return camera.getPosition().add(forward.scale(-hand.z() * RING_NEAR))
                    .subtract(left.scale(hand.x() * RING_NEAR)).add(up.scale(hand.y() * RING_NEAR));
        }
        if (owner instanceof LivingEntity living) {
            return LanternArms.ringPoint(living, partialTick);
        }
        double yaw = Math.toRadians(owner.getViewYRot(partialTick));
        Vec3 forward = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
        return owner.getPosition(partialTick).add(0, owner.getBbHeight() * 0.72, 0).add(forward.scale(0.3));
    }
}
