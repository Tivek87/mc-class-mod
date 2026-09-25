package nl.tivek.multiversepowers.character.greenlantern.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.effect.Effect;

/**
 * Where the {@link GiantFist} hangs while he charges it: the spots around him it may take, which of them has room for
 * it, and the way it points meanwhile; with whose it is and where it is right now.
 */
abstract class GiantFistSpots implements Effect {
    // Its shape, as parts of how wide it is: how tall, from its middle to the front of the knuckles, and
    // from its middle to the back of the wrist (the forearm behind that is only a fading trail of light).
    private static final double HEIGHT = 0.65;
    static final double FRONT = 0.5;
    private static final double BACK = 0.7;
    // While you charge it, it only tips this far up or down with your view, so it keeps to its spot.
    static final float MAX_HELD_PITCH = 25.0F;
    // A better spot that came free must stay free this many ticks before the fist moves back to it, so it
    // does not dart to and fro along a wall.
    private static final int RETURN_TICKS = 10;
    // How deep a block may poke into the fist before it is in the way: the fist's edges are only light.
    private static final double ROOM = 0.3;
    // Counting blocks in the way stops here; more than this is simply "very much in the way".
    private static final int MAX_COUNT = 4096;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    /** The spots around you where it can charge, the best one first. */
    private enum Spot {
        RIGHT, RIGHT_HIGH, ABOVE, LEFT, LEFT_HIGH
    }

    private static final Spot[] SPOTS = Spot.values();

    final ServerPlayer owner;
    Spot spot = Spot.RIGHT;
    // How long a better spot has been free (see RETURN_TICKS), in checks.
    private int waiting;
    Vec3 center;
    Vec3 facing;

    GiantFistSpots(ServerPlayer owner) {
        this.owner = owner;
    }

    /** The way it points while you charge it: where you look, but tipped no further than MAX_HELD_PITCH. */
    Vec3 heldFacing() {
        return Vec3.directionFromRotation(Mth.clamp(this.owner.getXRot(), -MAX_HELD_PITCH, MAX_HELD_PITCH),
                this.owner.getYRot());
    }

    /**
     * Where a spot is for a fist this wide, around your eyes (x to your right, y up, z ahead). Beside you it
     * stands on the ground rather than sinking into it; the high spots hang from the height of your head up,
     * and above your head it floats a little ahead, so you still see it.
     */
    Vec3 spotOffset(Spot spot, double size) {
        double half = HEIGHT * size * 0.5;
        // Far enough out and ahead that even its forearm stays clear of your eyes.
        double side = 1.0 + 0.62 * size;
        double ahead = 2.3 + 0.1 * size;
        double low = Math.max(-0.35, 0.2 - this.owner.getEyeHeight() + half);
        double high = Math.max(low, 0.2 + half);
        return switch (spot) {
            case RIGHT -> new Vec3(side, low, ahead);
            case RIGHT_HIGH -> new Vec3(side, high, ahead);
            case ABOVE -> new Vec3(0.0, 0.9 + 0.1 * size + half, 0.2 * size);
            case LEFT -> new Vec3(-side, low, ahead);
            case LEFT_HIGH -> new Vec3(-side, high, ahead);
        };
    }

    /** A point around your eyes out in the world (x to your right, y up, z ahead); it turns along with you. */
    Vec3 worldPoint(Vec3 local) {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, this.owner.getYRot());
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        return this.owner.getEyePosition().add(right.scale(local.x)).add(0.0, local.y, 0.0)
                .add(ahead.scale(local.z));
    }

    /**
     * Picks the spot to charge in: the first one in {@link Spot}'s order with no block in the way, or else the
     * one with the fewest. A spot that is in the way is left at once; a better one that came free is only
     * taken back once it stays free a moment.
     */
    void findRoom(ServerLevel level, double size) {
        int best = 0;
        int bestCount = Integer.MAX_VALUE;
        for (int i = 0; i < SPOTS.length && bestCount > 0; i++) {
            int count = this.blocksIn(level, SPOTS[i], size, Math.min(bestCount, MAX_COUNT));
            if (count < bestCount) {
                best = i;
                bestCount = count;
            }
        }
        if (SPOTS[best] == this.spot) {
            this.waiting = 0;
            return;
        }
        int here = this.blocksIn(level, this.spot, size, MAX_COUNT);
        if (here == 0) {
            if (++this.waiting < RETURN_TICKS / 2) {
                return;
            }
        } else if (bestCount > 0 && here <= bestCount + 2 + bestCount / 2) {
            // Nowhere has room: only move for a spot that is clearly better, not for a block or two.
            this.waiting = 0;
            return;
        }
        this.waiting = 0;
        this.spot = SPOTS[best];
        this.sound(level, SoundEvents.PHANTOM_FLAP, 0.5F, 1.4F);
    }

    /**
     * How many solid blocks the fist would be in at that spot, counting no further than {@code limit}. Air and
     * everything you can walk through never count, and a block has to poke into it a little ({@link #ROOM}).
     */
    private int blocksIn(ServerLevel level, Spot spot, double size, int limit) {
        Vec3 forward = this.facing;
        Vec3 right = forward.cross(UP);
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 up = right.cross(forward);
        double halfWidth = size * 0.5 + 0.5 - ROOM;
        double halfHeight = HEIGHT * size * 0.5 + 0.5 - ROOM;
        double halfDepth = (FRONT + BACK) * size * 0.5 + 0.5 - ROOM;
        // The fist reaches further back (to its wrist) than forward (to its knuckles).
        Vec3 middle = this.worldPoint(this.spotOffset(spot, size)).add(forward.scale((FRONT - BACK) * size * 0.5));
        double reachX = Math.abs(right.x) * halfWidth + Math.abs(up.x) * halfHeight + Math.abs(forward.x) * halfDepth;
        double reachY = Math.abs(right.y) * halfWidth + Math.abs(up.y) * halfHeight + Math.abs(forward.y) * halfDepth;
        double reachZ = Math.abs(right.z) * halfWidth + Math.abs(up.z) * halfHeight + Math.abs(forward.z) * halfDepth;
        int minX = Mth.floor(middle.x - reachX);
        int maxX = Mth.floor(middle.x + reachX);
        int minY = Math.max(level.getMinBuildHeight(), Mth.floor(middle.y - reachY));
        int maxY = Math.min(level.getMaxBuildHeight() - 1, Mth.floor(middle.y + reachY));
        int minZ = Mth.floor(middle.z - reachZ);
        int maxZ = Mth.floor(middle.z + reachZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int count = 0;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++) {
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null) {
                    continue;
                }
                for (int sectionY = minY >> 4; sectionY <= maxY >> 4; sectionY++) {
                    LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(sectionY));
                    // Most of the room around you is open air: whole sections of it are skipped at once.
                    if (section.hasOnlyAir()) {
                        continue;
                    }
                    for (int x = Math.max(minX, chunkX << 4); x <= Math.min(maxX, (chunkX << 4) + 15); x++) {
                        for (int y = Math.max(minY, sectionY << 4); y <= Math.min(maxY, (sectionY << 4) + 15); y++) {
                            for (int z = Math.max(minZ, chunkZ << 4); z <= Math.min(maxZ, (chunkZ << 4) + 15); z++) {
                                BlockState state = section.getBlockState(x & 15, y & 15, z & 15);
                                if (state.isAir()) {
                                    continue;
                                }
                                double dx = x + 0.5 - middle.x;
                                double dy = y + 0.5 - middle.y;
                                double dz = z + 0.5 - middle.z;
                                if (Math.abs(dx * right.x + dy * right.y + dz * right.z) > halfWidth
                                        || Math.abs(dx * up.x + dy * up.y + dz * up.z) > halfHeight
                                        || Math.abs(dx * forward.x + dy * forward.y + dz * forward.z) > halfDepth) {
                                    continue;
                                }
                                if (!state.getCollisionShape(level, pos.set(x, y, z)).isEmpty() && ++count >= limit) {
                                    return count;
                                }
                            }
                        }
                    }
                }
            }
        }
        return count;
    }

    void sound(ServerLevel level, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, this.center.x, this.center.y, this.center.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
