package nl.tivek.multiversepowers.engine.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

// A body hitting the ground or a wall: a dull thud as loud as the hit was hard, the block's own sound under it, and a
// puff of the block's dust thrown out of the face it struck. Only in the player's own game.
public final class Thuds {
    // Slower than SOFT (blocks a second into the block) a body only settles; from HARD on a thud is as loud as it gets,
    // and a hit past BIG sounds like a heavy fall.
    public static final double SOFT = 3.0;
    private static final double HARD = 11.0;
    private static final double BIG = 0.7;
    private static final int MOST_DUST = 12;

    private Thuds() {
    }

    // A hit at (x, y, z), just inside the block struck, its face turned (nx, ny, nz), at `speed` into it.
    public static void hit(ClientLevel level, double x, double y, double z, double nx, double ny, double nz,
            double speed, SoundSource source, RandomSource random) {
        if (speed < SOFT) {
            return;
        }
        BlockPos pos = BlockPos.containing(x - nx * 0.02, y - ny * 0.02, z - nz * 0.02);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        double hard = Mth.clamp((speed - SOFT) / (HARD - SOFT), 0.0, 1.0);
        float volume = (float) (0.3 + 0.7 * hard);
        SoundType type = state.getSoundType(level, pos, null);
        level.playLocalSound(x, y, z, hard > BIG ? SoundEvents.GENERIC_BIG_FALL : SoundEvents.GENERIC_SMALL_FALL,
                source, volume, 0.85F + random.nextFloat() * 0.25F, false);
        level.playLocalSound(x, y, z, type.getFallSound(), source, type.getVolume() * volume * 0.8F,
                type.getPitch() * (0.7F + random.nextFloat() * 0.15F), false);
        BlockParticleOption dust = new BlockParticleOption(ParticleTypes.BLOCK, state);
        int count = ParticleAmount.count(2 + (int) Math.round(MOST_DUST * hard), random);
        double out = 0.04 + 0.1 * hard;
        for (int i = 0; i < count; i++) {
            double sx = random.nextGaussian() * 0.12;
            double sy = random.nextGaussian() * 0.12;
            double sz = random.nextGaussian() * 0.12;
            double along = sx * nx + sy * ny + sz * nz;
            // Spread over the face, never into it.
            level.addParticle(dust, x + nx * 0.05 + sx - nx * along, y + ny * 0.05 + sy - ny * along,
                    z + nz * 0.05 + sz - nz * along, nx * out + random.nextGaussian() * 0.05,
                    ny * out + random.nextGaussian() * 0.05, nz * out + random.nextGaussian() * 0.05);
        }
    }
}
