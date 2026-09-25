package nl.tivek.multiversepowers.character.greenlantern.client.render;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public final class FlameSound extends AbstractTickableSoundInstance {
    private static final Map<Integer, FlameSound> ROARS = new HashMap<>();
    private static final float STREAM_LOUD = 0.9F;
    private static final float STREAM_TONE = 0.55F;
    private static final float SWIRL_LOUD = 0.7F;
    private static final float SWIRL_TONE = 0.8F;
    private static final int UNHEARD = 5;

    private final Entity owner;
    private final boolean swirl;
    private boolean on = true;
    private int unheard;

    private FlameSound(Entity owner, boolean swirl) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        this.owner = owner;
        this.swirl = swirl;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.05F;
        this.pitch = swirl ? SWIRL_TONE : STREAM_TONE;
        this.x = owner.getX();
        this.y = owner.getEyeY();
        this.z = owner.getZ();
    }

    public static void keep(Entity owner, boolean stream, boolean swirl) {
        boolean want = stream || swirl;
        FlameSound sound = ROARS.get(owner.getId());
        if (sound != null && (!want || sound.swirl != swirl || sound.isStopped())) {
            sound.on = false;
            ROARS.remove(owner.getId());
            sound = null;
        }
        if (sound != null) {
            sound.unheard = 0;
        } else if (want) {
            FlameSound roar = new FlameSound(owner, swirl);
            ROARS.put(owner.getId(), roar);
            Minecraft.getInstance().getSoundManager().play(roar);
        }
    }

    public static void clear() {
        ROARS.values().forEach(sound -> sound.on = false);
        ROARS.clear();
    }

    @Override
    public void tick() {
        if (this.owner.isRemoved()) {
            this.stop();
            return;
        }
        if (++this.unheard > UNHEARD) {
            this.on = false;
        }
        this.x = this.owner.getX();
        this.y = this.owner.getEyeY();
        this.z = this.owner.getZ();
        float loud = this.on ? (this.swirl ? SWIRL_LOUD : STREAM_LOUD) : 0.0F;
        this.volume = Mth.lerp(this.on ? 0.35F : 0.25F, this.volume, loud);
        float tone = this.swirl ? SWIRL_TONE + 0.05F * Mth.sin(this.owner.tickCount * 0.42F) : STREAM_TONE;
        this.pitch = Mth.lerp(0.2F, this.pitch, tone);
        if (!this.on && this.volume < 0.02F) {
            this.stop();
            ROARS.remove(this.owner.getId(), this);
        }
    }
}
