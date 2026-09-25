package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.greenlantern.PlanePath;

/**
 * The sound of one plane: the deep, droning buzz of its four propellers and the rush of the air past it, following
 * it; as it plunges the rush rises into a scream. Its jets each whoosh by with a rush of their own, higher the faster
 * they go.
 */
final class PlaneSound {
    // The drone of every plane in the air, by the id of its construct.
    static final Map<Integer, PlaneSound> SOUNDS = new HashMap<>();

    private final Loop drone;
    private final Loop wind;
    private final Loop[] jets = new Loop[PlanePath.JETS];

    PlaneSound(Vec3 at) {
        this.drone = new Loop(SoundEvents.BEE_LOOP, at);
        this.wind = new Loop(SoundEvents.ELYTRA_FLYING, at);
        Minecraft.getInstance().getSoundManager().play(this.drone);
        Minecraft.getInstance().getSoundManager().play(this.wind);
    }

    /**
     * Keeps the drone of this plane where it is. {@code down} is how far it has plunged (0 to 1), or below 0 once it has
     * crashed: the drone stops.
     */
    static void sound(int id, PlanePath path, double t, double down) {
        if (down < 0.0) {
            PlaneSound gone = SOUNDS.remove(id);
            if (gone != null) {
                gone.stopAll();
            }
            return;
        }
        Vec3 at = path.at(t);
        PlaneSound sound = SOUNDS.get(id);
        if (sound == null || sound.stopped()) {
            // Drones of planes that are gone some other way (broken up in the air, out of reach) have stopped by now.
            SOUNDS.values().removeIf(PlaneSound::stopped);
            sound = new PlaneSound(at);
            SOUNDS.put(id, sound);
        }
        sound.update(path, t, at, down);
    }

    void update(PlanePath path, double t, Vec3 at, double down) {
        long seen = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        this.drone.follow(at, seen, 7.0F, (float) (0.5 - 0.1 * down), 1.0F - (float) down * 0.5F);
        this.wind.follow(at, seen, (float) (2.0 + 5.0 * down), (float) (0.5 + 0.9 * down * down), 1.0F);
        double fled = path.jetsFled(t);
        for (int k = 0; k < PlanePath.JETS; k++) {
            if (!path.hasJet(k) || t < path.jetFrom(k) || fled >= PlanePath.JET_GONE) {
                if (this.jets[k] != null) {
                    this.jets[k].done = true;
                }
                continue;
            }
            Vec3 jet = path.jetAt(k, t);
            if (this.jets[k] == null) {
                this.jets[k] = new Loop(SoundEvents.ELYTRA_FLYING, jet);
                Minecraft.getInstance().getSoundManager().play(this.jets[k]);
            }
            double speed = path.jetSpeed(k, t);
            this.jets[k].follow(jet, seen, fled > 0.0 ? 7.0F : 4.0F, (float) Mth.clamp(1.1 + 0.05 * speed, 1.1, 2.0),
                    1.0F);
        }
    }

    boolean stopped() {
        return this.drone.isStopped() && this.wind.isStopped();
    }

    void stopAll() {
        this.drone.done = true;
        this.wind.done = true;
        for (Loop jet : this.jets) {
            if (jet != null) {
                jet.done = true;
            }
        }
    }

    /** One looping sound that follows a plane about. */
    private static final class Loop extends AbstractTickableSoundInstance {
        private Vec3 at;
        private long seen;
        private float loud;
        private float tone;
        private float share;
        private boolean done;

        Loop(SoundEvent event, Vec3 at) {
            super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.at = at;
            this.looping = true;
            this.delay = 0;
            this.volume = 0.05F;
            this.pitch = 0.5F;
            this.x = at.x;
            this.y = at.y;
            this.z = at.z;
        }

        void follow(Vec3 at, long seen, float loud, float tone, float share) {
            this.at = at;
            this.seen = seen;
            this.loud = loud;
            this.tone = tone;
            this.share = share;
        }

        @Override
        public void tick() {
            Minecraft minecraft = Minecraft.getInstance();
            if (this.done || minecraft.level == null || minecraft.level.getGameTime() - this.seen > 10L) {
                this.stop();
                return;
            }
            this.x = this.at.x;
            this.y = this.at.y;
            this.z = this.at.z;
            this.volume = Mth.lerp(0.08F, this.volume, this.loud * this.share);
            this.pitch = Mth.lerp(0.1F, this.pitch, this.tone);
        }
    }
}
