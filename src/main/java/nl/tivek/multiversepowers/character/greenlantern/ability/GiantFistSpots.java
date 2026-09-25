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

abstract class GiantFistSpots implements Effect {
    private static final double HEIGHT = 0.65;
    static final double FRONT = 0.5;
    private static final double BACK = 0.7;
    static final float MAX_HELD_PITCH = 25.0F;
    private static final int RETURN_TICKS = 10;
    private static final double ROOM = 0.3;
    private static final int MAX_COUNT = 4096;
    private static final Vec3 UP = new Vec3(0.0, 1.0, 0.0);

    private enum Spot {
        RIGHT, RIGHT_HIGH, ABOVE, LEFT, LEFT_HIGH
    }

    private static final Spot[] SPOTS = Spot.values();

    final ServerPlayer owner;
    Spot spot = Spot.RIGHT;
    private int waiting;
    Vec3 center;
    Vec3 facing;

    GiantFistSpots(ServerPlayer owner) {
        this.owner = owner;
    }

    Vec3 heldFacing() {
        return Vec3.directionFromRotation(Mth.clamp(this.owner.getXRot(), -MAX_HELD_PITCH, MAX_HELD_PITCH),
                this.owner.getYRot());
    }

    Vec3 spotOffset(Spot spot, double size) {
        double half = HEIGHT * size * 0.5;
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

    Vec3 worldPoint(Vec3 local) {
        Vec3 ahead = Vec3.directionFromRotation(0.0F, this.owner.getYRot());
        Vec3 right = new Vec3(-ahead.z, 0.0, ahead.x);
        return this.owner.getEyePosition().add(right.scale(local.x)).add(0.0, local.y, 0.0)
                .add(ahead.scale(local.z));
    }

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

    private int blocksIn(ServerLevel level, Spot spot, double size, int limit) {
        Vec3 forward = this.facing;
        Vec3 right = forward.cross(UP);
        right = right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : right.normalize();
        Vec3 up = right.cross(forward);
        double halfWidth = size * 0.5 + 0.5 - ROOM;
        double halfHeight = HEIGHT * size * 0.5 + 0.5 - ROOM;
        double halfDepth = (FRONT + BACK) * size * 0.5 + 0.5 - ROOM;
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
