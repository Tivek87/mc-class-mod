package nl.tivek.multiversepowers.engine.client.fx;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;

// A spoken line, heard from where its speaker stands and following it. Each speaker says one line at a time: a new one
// cuts the one before short, whatever said it (see claim).
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class VoiceLine extends AbstractTickableSoundInstance implements Voice {
    private static final float CUT_TICKS = 5.0F;

    private static final Map<Integer, Voice> SPEAKING = new HashMap<>();

    private final Entity speaker;
    private float cut = -1.0F;

    private VoiceLine(Entity speaker, ResourceLocation sound) {
        super(SoundEvent.createVariableRangeEvent(sound), SoundSource.VOICE, SoundInstance.createUnseededRandom());
        this.speaker = speaker;
        this.delay = 0;
        // Mono clips only: the game places those in the world, a stereo one would play at your ears.
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.follow();
        this.volume = loudness();
    }

    // The player's own volume for spoken lines, 0 with them off. Never above 1, where a volume only widens how far a
    // sound is heard: a voice carries Voices.HEARD blocks by its attenuation_distance in sounds.json.
    public static float loudness() {
        return ClientSettings.VOICE_LINES.get() == 0 ? 0.0F : ClientSettings.VOICE_VOLUME.get().floatValue();
    }

    // The one voice of a speaker: whatever it said before is cut short.
    public static void claim(int speaker, Voice voice) {
        Voice was = SPEAKING.put(speaker, voice);
        if (was != null && was != voice) {
            was.cut();
        }
    }

    public static void release(int speaker, Voice voice) {
        SPEAKING.remove(speaker, voice);
    }

    public static void say(int speakerId, ResourceLocation sound) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Entity speaker = level == null ? null : level.getEntity(speakerId);
        if (speaker == null || loudness() <= 0.0F) {
            return;
        }
        VoiceLine line = new VoiceLine(speaker, sound);
        claim(speakerId, line);
        minecraft.getSoundManager().play(line);
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.speaker.isRemoved()) {
            this.end();
            return;
        }
        if (this.cut >= 0.0F && ++this.cut >= CUT_TICKS) {
            this.end();
            return;
        }
        this.follow();
        this.volume = loudness() * (this.cut < 0.0F ? 1.0F : 1.0F - this.cut / CUT_TICKS);
    }

    private void follow() {
        this.x = this.speaker.getX();
        this.y = this.speaker.getEyeY();
        this.z = this.speaker.getZ();
    }

    @Override
    public void cut() {
        if (this.cut < 0.0F) {
            this.cut = 0.0F;
        }
    }

    private void end() {
        this.stop();
        release(this.speaker.getId(), this);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SPEAKING.clear();
    }
}
