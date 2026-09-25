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

final class PlaneSound {
    static final Map<Integer, PlaneSound> SOUNDS = new HashMap<>();

    private final Loop drone;

    PlaneSound(Vec3 at) {
        this.drone = new Loop(SoundEvents.BEE_LOOP, at);
        Minecraft.getInstance().getSoundManager().play(this.drone);
    }

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
            SOUNDS.values().removeIf(PlaneSound::stopped);
            sound = new PlaneSound(at);
            SOUNDS.put(id, sound);
        }
        sound.update(at, down);
    }

    void update(Vec3 at, double down) {
        long seen = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
        this.drone.follow(at, seen, 7.0F, (float) (0.5 - 0.1 * down), 1.0F - (float) down * 0.5F);
    }

    boolean stopped() {
        return this.drone.isStopped();
    }

    void stopAll() {
        this.drone.done = true;
    }

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
