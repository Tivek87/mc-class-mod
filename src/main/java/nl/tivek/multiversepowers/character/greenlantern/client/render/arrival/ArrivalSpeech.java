package nl.tivek.multiversepowers.character.greenlantern.client.render.arrival;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.engine.client.fx.Voice;
import nl.tivek.multiversepowers.engine.client.fx.VoiceLine;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ArrivalSpeech extends AbstractTickableSoundInstance implements Voice {
    private static final SoundEvent SPEECH = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "ring.arrival_speech"));
    // A sound cannot start halfway: only an arrival seen from its start speaks.
    private static final float LATE = 10.0F;
    private static final float NEAR_END = 8.0F;
    private static final float FADE_TICKS = 14.0F;

    private static final Map<Integer, ArrivalSpeech> SPEAKING = new HashMap<>();

    private final AbstractClientPlayer speaker;
    private float seen;
    private float fading = -1.0F;

    private ArrivalSpeech(AbstractClientPlayer speaker) {
        super(SPEECH, SoundSource.VOICE, SoundInstance.createUnseededRandom());
        this.speaker = speaker;
        this.delay = 0;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.LINEAR;
        this.follow();
        this.volume = VoiceLine.loudness();
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
        float a = ClientRing.arrival(this.speaker, 0.0F);
        boolean cut = a < 0.0F ? this.seen < Arrival.TICKS - NEAR_END : a < LATE && a + LATE < this.seen;
        if (cut) {
            this.cut();
        }
        if (a >= 0.0F && this.fading < 0.0F) {
            this.seen = a;
        }
        if (this.fading >= 0.0F && ++this.fading >= FADE_TICKS) {
            this.end();
            return;
        }
        this.follow();
        this.volume = VoiceLine.loudness() * (this.fading < 0.0F ? 1.0F : 1.0F - this.fading / FADE_TICKS);
    }

    private void follow() {
        this.x = this.speaker.getX();
        this.y = this.speaker.getEyeY();
        this.z = this.speaker.getZ();
    }

    @Override
    public void cut() {
        if (this.fading < 0.0F) {
            this.fading = 0.0F;
            SPEAKING.remove(this.speaker.getId(), this);
            VoiceLine.release(this.speaker.getId(), this);
        }
    }

    private void end() {
        this.stop();
        SPEAKING.remove(this.speaker.getId(), this);
        VoiceLine.release(this.speaker.getId(), this);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        SPEAKING.values().removeIf(speech -> !minecraft.getSoundManager().isActive(speech));
        for (AbstractClientPlayer player : level.players()) {
            float a = ClientRing.arrival(player, 0.0F);
            if (a >= 0.0F && a < LATE && !SPEAKING.containsKey(player.getId())) {
                ArrivalSpeech speech = new ArrivalSpeech(player);
                SPEAKING.put(player.getId(), speech);
                VoiceLine.claim(player.getId(), speech);
                minecraft.getSoundManager().play(speech);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SPEAKING.clear();
    }
}
