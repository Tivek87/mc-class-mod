package nl.tivek.multiversepowers.engine.client.fx;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.util.RandomSource;
import nl.tivek.multiversepowers.config.client.ClientSettings;

// The player's own share of the powers' particles (ClientSettings.PARTICLE_AMOUNT): a count scaled by it, rounded up
// or down at random so a half share still shows every other one. At the full share nothing changes, not even the
// random numbers drawn.
public final class ParticleAmount {
    private ParticleAmount() {
    }

    public static int count(int count, RandomSource random) {
        double share = ClientSettings.get(ClientSettings.PARTICLE_AMOUNT);
        if (share == 1.0 || count <= 0) {
            return count;
        }
        double wanted = count * share;
        int whole = (int) wanted;
        return whole + (random.nextDouble() < wanted - whole ? 1 : 0);
    }

    public static void add(ClientLevel level, RandomSource random, ParticleOptions particle, double x, double y,
            double z, double vx, double vy, double vz) {
        for (int k = count(1, random); k > 0; k--) {
            level.addParticle(particle, x, y, z, vx, vy, vz);
        }
    }
}
