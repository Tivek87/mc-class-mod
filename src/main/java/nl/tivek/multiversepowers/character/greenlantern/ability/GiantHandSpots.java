package nl.tivek.multiversepowers.character.greenlantern.ability;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.SCALE;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.head;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.open;

// Where a hand can stand: the ground under a spot, and whether there is room for a portal hand or an axe pair.
final class GiantHandSpots {
    private GiantHandSpots() {
    }

    @Nullable
    static Vec3 ground(ServerLevel level, Vec3 spot, double near) {
        Vec3 from = new Vec3(spot.x, near + 3.0, spot.z);
        if (!level.isLoaded(BlockPos.containing(from))) {
            return null;
        }
        if (solid(level, from)) {
            from = new Vec3(spot.x, near + 1.2, spot.z);
            if (solid(level, from)) {
                return null;
            }
        }
        BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(from, from.subtract(0.0, 9.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, CollisionContext.empty()));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    static boolean solid(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    // Open air all the way: the portal, the line the hand reaches along, and for a pinch the height it lifts to.
    static boolean portalRoom(ServerLevel level, LivingEntity target, int move, Vec3 base, Vec3 facing) {
        Vec3 middle = target.getBoundingBox().getCenter();
        if (!open(level, base) || !level.isLoaded(BlockPos.containing(base))) {
            return false;
        }
        if (HandGroup.is(move)) {
            for (int t : new int[] { HandPose.firstAct(move), HandGroup.RING_PRESSES }) {
                for (HandGroup.Sub sub : HandGroup.at(move, base, facing, t)) {
                    if (!open(level, sub.portal().center()) || !open(level, sub.place().wrist())) {
                        return false;
                    }
                }
            }
            return true;
        }
        if (move == HandPose.SWALLOW) {
            // Ground under its portal, and open sky above for the creature to fall out of.
            for (double up = 2.0; up <= HandPose.SKY_HEIGHT + 2.0; up += 2.0) {
                if (!open(level, middle.add(0.0, up, 0.0))) {
                    return false;
                }
            }
            return solid(level, base.subtract(0.0, 0.3, 0.0));
        }
        HandPose.Place place = HandPose.at(move, HandPose.firstAct(move), 0.0).place(base, facing, SCALE);
        Vec3 wrist = place.wrist();
        for (int k = 1; k <= 4; k++) {
            if (!open(level, base.lerp(wrist, k / 4.0)) || !open(level, wrist.lerp(middle, k / 4.0))) {
                return false;
            }
        }
        if (move == HandPose.PINCH) {
            for (double up = 1.0; up <= HandPose.PINCH_LIFT; up += 1.0) {
                if (!open(level, middle.add(0.0, up, 0.0))) {
                    return false;
                }
            }
        }
        if (move == HandPose.DRAG) {
            // The drag needs a clear run away from the caster for the portal and the creature both.
            Vec3 away = new Vec3(-facing.x, 0.0, -facing.z).normalize();
            Vec3 feet = target.position().add(0.0, 0.6, 0.0);
            for (double far = 2.0; far <= HandPose.DRAG_DISTANCE; far += 2.0) {
                if (!open(level, base.add(away.scale(far))) || !open(level, feet.add(away.scale(far)))) {
                    return false;
                }
            }
        }
        return open(level, place.at(new Vec3(0.0, 3.0, 0.0)));
    }

    static boolean room(ServerLevel level, Vec3 base, int variant, Vec3 aim) {
        for (int t : new int[] { HandDuo.OUT, HandDuo.GRAB, HandDuo.AXE_FREE, HandDuo.RAISED, HandDuo.IMPACT }) {
            HandDuo duo = HandDuo.at(base, variant, aim, t, SCALE);
            if (!open(level, duo.leftPortal.center()) || !open(level, duo.rightPortal.center())
                    || !open(level, duo.axePortal.center()) || !open(level, duo.leftPlace.wrist())
                    || !open(level, duo.rightPlace.wrist())) {
                return false;
            }
            // The head only counts once it is out of its portal: behind it, it is not there yet.
            if (duo.axeThere && !duo.axeCut && !open(level, head(duo))) {
                return false;
            }
        }
        return true;
    }
}
