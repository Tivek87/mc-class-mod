package nl.tivek.multiversepowers.creature.wizard.rift;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.creature.wizard.Wizard;
import nl.tivek.multiversepowers.creature.wizard.WizardMove;
import nl.tivek.multiversepowers.engine.fx.ParticleFx;

// The rifts round the Wizard's coming and going. Coming: four rifts of four kinds open round where he will land, each
// pouring its light into him as he sinks, and a golden ring opens on the ground to take his feet; they shut as he lands.
// Going: small rifts of every kind flicker open round him as his light leaks out, and at the end a rift of the void
// opens over him and swallows what is left.
public final class RiftShows {
    private static final int[] AROUND = { WizardPortal.ARCANE, WizardPortal.FROST, WizardPortal.EMBER,
            WizardPortal.STARRY };
    // How far from where he lands the four rifts stand, and how high (blocks).
    private static final double RING = 5.0;
    private static final double HIGH = 4.0;
    private static final int FLOOR_AT = WizardMove.LAND - 24;
    private static final int SWALLOW_AT = WizardMove.GONE - 26;

    private RiftShows() {
    }

    public static void arrival(ServerLevel level, Wizard wizard, Vec3 ground, int t) {
        Vec3 body = wizard.position().add(0.0, 1.2, 0.0);
        float turn = Math.floorMod(wizard.getId() * 37, 90);
        if (t == 1) {
            for (int i = 0; i < AROUND.length; i++) {
                float a = (turn + 90.0F * i) * Mth.DEG_TO_RAD;
                Vec3 at = ground.add(Mth.sin(a) * RING, HIGH, Mth.cos(a) * RING);
                WizardPortal.open(level, at, facing(at, ground), 1.3F, WizardMove.LAND + 6, AROUND[i], true);
            }
        }
        if (t > 8 && t < WizardMove.LAND && t % 3 == 0) {
            for (int i = 0; i < AROUND.length; i++) {
                float a = (turn + 90.0F * i) * Mth.DEG_TO_RAD;
                Vec3 from = ground.add(Mth.sin(a) * RING, HIGH, Mth.cos(a) * RING);
                ParticleFx.line(level, Wizard.dust(WizardPortal.rim(AROUND[i]), 0.9F), from, body, 0.6);
            }
        }
        if (t == FLOOR_AT) {
            WizardPortal.open(level, ground.add(0.0, 0.08, 0.0), 0.0F, 2.6F, WizardMove.LAND - FLOOR_AT + 14,
                    WizardPortal.GOLDEN, false);
        }
    }

    public static void death(ServerLevel level, Wizard wizard, int t) {
        Vec3 body = wizard.position().add(0.0, 1.2, 0.0);
        RandomSource random = wizard.getRandom();
        if (t > 14 && t < SWALLOW_AT && t % 9 == 0) {
            float a = random.nextFloat() * Mth.TWO_PI;
            double far = 1.8 + random.nextDouble() * 1.4;
            Vec3 at = body.add(Mth.sin(a) * far, (random.nextDouble() - 0.3) * 1.6, Mth.cos(a) * far);
            WizardPortal.open(level, at, facing(at, body), 0.55F + random.nextFloat() * 0.35F, 18 + random.nextInt(8),
                    random.nextInt(WizardPortal.STYLES), true);
        }
        if (t == SWALLOW_AT) {
            WizardPortal.open(level, body.add(0.0, 2.6, 0.0), 0.0F, 2.2F, WizardMove.GONE - SWALLOW_AT + 8,
                    WizardPortal.VOID, false);
            level.playSound(null, body.x, body.y, body.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.HOSTILE, 1.4F,
                    0.6F);
        }
        if (t > SWALLOW_AT + 6 && t < WizardMove.GONE && t % 2 == 0) {
            ParticleFx.line(level, Wizard.dust(WizardPortal.rim(WizardPortal.VOID), 1.2F), body,
                    body.add(0.0, 2.6, 0.0), 0.4);
        }
    }

    // The yaw an upright rift at `at` takes to face `toward`.
    private static float facing(Vec3 at, Vec3 toward) {
        Vec3 way = toward.subtract(at);
        return (float) (Mth.atan2(-way.x, way.z) * Mth.RAD_TO_DEG);
    }
}
