package nl.tivek.multiversepowers.character.greenlantern.ability;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import nl.tivek.multiversepowers.character.greenlantern.HandDuo;
import nl.tivek.multiversepowers.character.greenlantern.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.HandPose;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.engine.world.LoadedWorld;
import static nl.tivek.multiversepowers.character.greenlantern.ability.GiantHands.SCALE;

// Where a hand of the Giant Hands can come: out of the ground, a wall or a portal, clear of every other hand.
abstract class GiantHandPlaces {
    private static final double[] PAIR_TURNS = { 0.0, Math.PI * 0.5, -Math.PI * 0.5, Math.PI };
    private static final double[] PUSH_TURNS = { 0.0, 0.6, -0.6, 1.1, -1.1, Math.PI * 0.5, -Math.PI * 0.5 };
    private static final double OWNER_ROOM = 1.8;
    // Hands that may come out of a wall next to the creature instead of the ground, and how often when one is there.
    private static final Set<Integer> WALLED = Set.of(HandPose.SMACK, HandPose.GRAB, HandPose.FINGER, HandPose.SLAM,
            HandPose.POUND, HandPose.SNAP, HandPose.RAKE, HandPose.RAGDOLL, HandPose.RINGBEAM, HandPose.SCOOP,
            HandPose.EYE, HandPose.MEGAPHONE, HandPose.PUPPETEER);
    // Hands that act from afar and so may stand higher or lower than the creature.
    private static final Set<Integer> FAR = Set.of(HandPose.RINGBEAM, HandPose.EYE, HandPose.MEGAPHONE,
            HandPose.PUPPETEER, HandPose.RINGCHAINS);
    private static final double WALL_CHANCE = 0.6;
    private static final double WALL_NEAR = 4.5;
    private static final int GROUND_TURNS = 8;
    // How far above the creature a hand out of a wall stands at most, so low walls serve too.
    private static final double WALL_HIGH = 4.0;
    private static final int WALL_COLUMN = (int) Math.floor(WALL_HIGH * SCALE + 1.0) + 1;
    // How far out of its ground or wall a hand looks for its creature from.
    private static final double SEES_FROM = 1.5;

    private record WallSpot(Vec3 hit, Vec3 out, double far, int column) {
    }

    final ServerPlayer owner;
    boolean crowded;
    @Nullable
    List<GiantHand> taken;
    private final Map<Integer, List<WallSpot>> wallsNear = new HashMap<>();
    private long wallsAt = -1L;

    GiantHandPlaces(ServerPlayer owner) {
        this.owner = owner;
    }

    abstract GiantHands storm();

    @Nullable
    GiantHand spawn(ServerLevel level, LivingEntity target, int move) {
        Vec3 away = new Vec3(target.getX() - this.owner.getX(), 0.0, target.getZ() - this.owner.getZ());
        away = away.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.owner.getYRot()) : away.normalize();
        if (move == HandPose.AXE) {
            return this.pairFor(level, target, away);
        }
        if (HandPose.portal(move)) {
            return this.portalHand(level, target, move, away);
        }
        if (move == HandPose.EYE) {
            return this.eyePair(level, target, away);
        }
        RandomSource random = this.owner.getRandom();
        int extra = move == HandPose.RAGDOLL ? random.nextInt(4) | random.nextInt(1024) << 2 : 0;
        return this.single(level, target, move, extra, away);
    }

    // The evil eye comes up together with its puppeteer beside it, or not at all.
    @Nullable
    GiantHand eyePair(ServerLevel level, LivingEntity target, Vec3 away) {
        GiantHand eye = this.single(level, target, HandPose.EYE, 0, away);
        if (eye == null) {
            return null;
        }
        if (this.taken != null) {
            this.taken.add(eye);
        }
        GiantHand puppeteer = this.single(level, target, HandPose.PUPPETEER, 0, away);
        if (this.taken != null) {
            this.taken.remove(eye);
        }
        if (puppeteer == null) {
            return null;
        }
        eye.partner = puppeteer;
        return eye;
    }

    @Nullable
    GiantHand single(ServerLevel level, LivingEntity target, int move, int extra, Vec3 away) {
        boolean triedWall = WALLED.contains(move) && this.owner.getRandom().nextDouble() < WALL_CHANCE;
        if (triedWall) {
            GiantHand walled = this.wallHand(level, target, move, extra);
            if (walled != null) {
                return walled;
            }
        }
        GiantHand grounded = this.groundHand(level, target, move, extra, away);
        if (grounded != null || !WALLED.contains(move) || triedWall) {
            return grounded;
        }
        // No room on the ground: a wall next to it will do, whatever the chance said.
        return this.wallHand(level, target, move, extra);
    }

    // The first spot round the creature where the ground is level and the whole move stays clear of blocks: the way
    // the move likes best first, then turned further and further round.
    @Nullable
    GiantHand groundHand(ServerLevel level, LivingEntity target, int move, int extra, Vec3 away) {
        double side = this.owner.getRandom().nextBoolean() ? 1.0 : -1.0;
        // A scoop and the evil eye come up on the far side, facing the caster's way; a ring beam and a megaphone stand
        // off to one side and fire across.
        Vec3 liked = move == HandPose.SCOOP || move == HandPose.EYE ? away.scale(-1.0)
                : move == HandPose.RINGBEAM || move == HandPose.MEGAPHONE || move == HandPose.SMACK
                        || move == HandPose.PUPPETEER || move == HandPose.RINGHAMMER || move == HandPose.RINGCHAINS
                        ? Vectors.spin(away, Vectors.UP, side * Math.PI * 0.5) : away;
        for (int k = 0; k < GROUND_TURNS; k++) {
            int step = (k + 1) / 2;
            double turn = (k % 2 == 1 ? side : -side) * step * Math.PI * 2.0 / GROUND_TURNS;
            Vec3 reach = Vectors.spin(liked, Vectors.UP, turn);
            int variant = HandPose.variant(move, move == HandPose.SNAP && side < 0.0, false, extra);
            if (move == HandPose.SMACK) {
                variant = reach.cross(Vectors.UP).dot(away) > 0.0 ? move : move + HandPose.MOVES;
            }
            Vec3 spot = target.position().subtract(reach.scale(HandPose.spot(move) * SCALE));
            Vec3 base = GiantHandSpots.ground(level, spot, target.getY());
            if (base == null || !GiantHandSpots.standing(level, base, target.getY(), FAR.contains(move))
                    || !GiantHandSpots.sees(level, base.add(0.0, SEES_FROM, 0.0), target)) {
                continue;
            }
            GiantHand hand = new GiantHand(this.storm(), variant, base, target);
            if (GiantHandSpots.clear(level, hand) && this.fits(hand)) {
                return hand;
            }
        }
        return null;
    }

    boolean fits(GiantHand hand) {
        if (this.taken == null) {
            this.taken = GiantHands.handsIn(this.owner.level());
        }
        GiantHandRoom room = null;
        for (GiantHand other : this.taken) {
            if (!GiantHandRoom.near(hand, other)) {
                continue;
            }
            if (room == null) {
                room = GiantHandRoom.of(hand);
            }
            if (room.clashes(GiantHandRoom.of(other))) {
                this.crowded = true;
                return false;
            }
        }
        return true;
    }

    // A wall right beside the creature: the hand comes out of it instead of the ground, the wall as its ground, so it
    // stands spot above the creature on the wall as it would stand spot away from it on the ground. The nearest wall
    // with room wins; the costly check of the whole move only runs until one passes.
    @Nullable
    GiantHand wallHand(ServerLevel level, LivingEntity target, int move, int extra) {
        double high = Math.min(HandPose.spot(move), WALL_HIGH) * SCALE;
        int needs = (int) Math.floor(high + 1.0) + 1;
        for (WallSpot wall : this.walls(level, target)) {
            if (wall.column() < needs) {
                continue;
            }
            Vec3 base = wall.hit().add(0.0, high, 0.0);
            if (!GiantHandSpots.sees(level, base.add(wall.out().scale(SEES_FROM)), target)) {
                continue;
            }
            GiantHand hand = new GiantHand(this.storm(), HandPose.variant(move, false, true, extra), base, target,
                    wall.out());
            if (GiantHandSpots.clear(level, hand) && this.fits(hand)) {
                return hand;
            }
        }
        return null;
    }

    // The walls round a creature, nearest first, looked up once a tick however many hands try them.
    List<WallSpot> walls(ServerLevel level, LivingEntity target) {
        long now = level.getGameTime();
        if (now != this.wallsAt) {
            this.wallsAt = now;
            this.wallsNear.clear();
        }
        return this.wallsNear.computeIfAbsent(target.getId(), id -> findWalls(level, target));
    }

    private static List<WallSpot> findWalls(ServerLevel level, LivingEntity target) {
        Vec3 middle = target.getBoundingBox().getCenter();
        List<WallSpot> found = new ArrayList<>();
        for (int k = 0; k < 8; k++) {
            Vec3 way = Vectors.spin(new Vec3(0.0, 0.0, 1.0), Vectors.UP, k * Math.PI * 0.25);
            BlockHitResult hit = LoadedWorld.clip(level, new ClipContext(middle, middle.add(way.scale(WALL_NEAR)),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.MISS || hit.getDirection().getAxis().isVertical()) {
                continue;
            }
            Vec3 out = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
            // How many blocks up from the hit the wall stays solid with open air in front of it.
            int column = 0;
            while (column < WALL_COLUMN) {
                Vec3 at = hit.getLocation().add(0.0, column, 0.0);
                if (!GiantHandSpots.solid(level, at.subtract(out.scale(0.3))) || !open(level, at.add(out.scale(0.7)))) {
                    break;
                }
                column++;
            }
            if (column > 0) {
                found.add(new WallSpot(hit.getLocation(), out, hit.getLocation().distanceTo(middle), column));
            }
        }
        found.sort(Comparator.comparingDouble(WallSpot::far));
        return found;
    }

    @Nullable
    GiantHand pairFor(ServerLevel level, LivingEntity target, Vec3 away) {
        Vec3 base = GiantHandSpots.ground(level, target.position(), target.getY());
        if (base == null) {
            return null;
        }
        Vec3 aim = inReach(base, new Vec3(target.getX(), base.y, target.getZ()));
        for (double turn : PAIR_TURNS) {
            int variant = HandPose.axeVariant(Vectors.spin(away, Vectors.UP, turn));
            if (GiantHandSpots.room(level, base, variant, aim)) {
                GiantHand pair = new GiantHand(this.storm(), variant, base, target);
                if (this.fits(pair)) {
                    return pair;
                }
            }
        }
        return null;
    }

    // A portal opens beside the creature for a flick (on the caster's side, so it flies away from him) or over it for
    // a pinch; failing that, turned the other ways round it. A hand that pushes its creature never comes from behind
    // it, and no hand reaches out of or through its caster.
    @Nullable
    GiantHand portalHand(ServerLevel level, LivingEntity target, int move, Vec3 away) {
        // A drag reaches back from the far side, so the creature is dragged away from the caster, never at him.
        boolean drag = move == HandPose.DRAG;
        boolean pushes = move == HandPose.FLICK || move == HandPose.POKE;
        for (double turn : pushes ? PUSH_TURNS : PAIR_TURNS) {
            if (drag && turn == Math.PI) {
                continue;
            }
            Vec3 facing = Vectors.spin(drag ? away.scale(-1.0) : away, Vectors.UP, turn);
            Vec3 base = portalFor(target, move, facing);
            if (this.clearOfOwner(base, target) && GiantHandSpots.portalRoom(level, target, move, base, facing)) {
                GiantHand hand = new GiantHand(this.storm(), move, base, target, facing);
                if ((HandGroup.is(move) || GiantHandSpots.clear(level, hand)) && this.fits(hand)) {
                    return hand;
                }
            }
        }
        return null;
    }

    boolean clearOfOwner(Vec3 base, LivingEntity target) {
        Vec3 from = new Vec3(base.x, 0.0, base.z);
        Vec3 to = new Vec3(target.getX(), 0.0, target.getZ());
        Vec3 owner = new Vec3(this.owner.getX(), 0.0, this.owner.getZ());
        Vec3 line = to.subtract(from);
        double along = line.lengthSqr() < 1.0E-6 ? 0.0 : Mth.clamp(owner.subtract(from).dot(line) / line.lengthSqr(),
                0.0, 1.0);
        return owner.distanceTo(from.add(line.scale(along))) >= OWNER_ROOM;
    }

    static Vec3 portalFor(LivingEntity target, int variant, Vec3 facing) {
        int move = HandPose.move(variant);
        Vec3 spot = move == HandPose.HAMMER ? target.position() : target.getBoundingBox().getCenter();
        Vec3 base = spot.subtract(HandPose.workOffset(variant, facing, SCALE));
        // The swallow's portal lies on the ground the creature stands on.
        return move == HandPose.SWALLOW ? new Vec3(base.x, target.getY() + 0.05, base.z) : base;
    }

    // Open air: no block, and no water either, which is drawn over a construct behind or inside it and hides it.
    static boolean open(ServerLevel level, Vec3 at) {
        BlockPos pos = BlockPos.containing(at);
        if (pos.getY() >= level.getMaxBuildHeight()) {
            return true;
        }
        return level.isLoaded(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getFluidState(pos).isEmpty();
    }

    static Vec3 inReach(Vec3 base, Vec3 spot) {
        double dx = spot.x - base.x;
        double dz = spot.z - base.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        double reach = HandDuo.REACH * SCALE;
        return flat <= reach ? spot : new Vec3(base.x + dx * reach / flat, spot.y, base.z + dz * reach / flat);
    }
}
