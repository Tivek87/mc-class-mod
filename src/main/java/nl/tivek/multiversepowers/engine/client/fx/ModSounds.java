package nl.tivek.multiversepowers.engine.client.fx;

import java.util.concurrent.CompletableFuture;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;

// The mod's own sounds, the server's too, as loud as the player has set them (ClientSettings.MOD_SOUNDS); music keeps
// its own slider.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ModSounds {
    private ModSounds() {
    }

    @SubscribeEvent
    public static void onPlay(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        float share = ClientSettings.factor(ClientSettings.MOD_SOUNDS);
        if (sound == null || share == 1.0F || sound instanceof TickableSoundInstance
                || sound.getSource() == SoundSource.MUSIC
                || !MultiversePowers.MODID.equals(sound.getLocation().getNamespace())) {
            return;
        }
        event.setSound(share <= 0.0F ? null : new Scaled(sound, share));
    }

    private record Scaled(SoundInstance sound, float share) implements SoundInstance {
        @Override
        public ResourceLocation getLocation() {
            return this.sound.getLocation();
        }

        @Override
        public WeighedSoundEvents resolve(SoundManager manager) {
            return this.sound.resolve(manager);
        }

        @Override
        public Sound getSound() {
            return this.sound.getSound();
        }

        @Override
        public SoundSource getSource() {
            return this.sound.getSource();
        }

        @Override
        public boolean isLooping() {
            return this.sound.isLooping();
        }

        @Override
        public boolean isRelative() {
            return this.sound.isRelative();
        }

        @Override
        public int getDelay() {
            return this.sound.getDelay();
        }

        @Override
        public float getVolume() {
            return this.sound.getVolume() * this.share;
        }

        @Override
        public float getPitch() {
            return this.sound.getPitch();
        }

        @Override
        public double getX() {
            return this.sound.getX();
        }

        @Override
        public double getY() {
            return this.sound.getY();
        }

        @Override
        public double getZ() {
            return this.sound.getZ();
        }

        @Override
        public Attenuation getAttenuation() {
            return this.sound.getAttenuation();
        }

        @Override
        public boolean canStartSilent() {
            return this.sound.canStartSilent();
        }

        @Override
        public boolean canPlaySound() {
            return this.sound.canPlaySound();
        }

        @Override
        public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
            return this.sound.getStream(buffers, sound, looping);
        }
    }
}
