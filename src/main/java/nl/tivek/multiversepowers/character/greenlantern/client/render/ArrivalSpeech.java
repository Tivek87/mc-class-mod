package nl.tivek.multiversepowers.character.greenlantern.client.render;

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
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ArrivalSpeech extends AbstractTickableSoundInstance {
    private static final SoundEvent SPEECH = SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "ring.arrival_speech"));
    // A sound cannot start halfway: only an arrival seen from its start speaks.
    private static final float LATE = 10.0F;
    private static final float NEAR_END = 8.0F;
    private static final float FADE_TICKS = 14.0F;
    private static final double HEARD = 16.0;

    private static final Map<Integer, ArrivalSpeech> SPEAKING = new HashMap<>();

    private final AbstractClientPlayer speaker;
    private float seen;
    private float fading = -1.0F;

    private ArrivalSpeech(AbstractClientPlayer speaker) {
        super(SPEECH, SoundSource.VOICE, SoundInstance.createUnseededRandom());
        this.speaker = speaker;
        this.delay = 0;
        // The game never places a stereo sound in the world: it plays at your ears, fainter further off.
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.volume = this.heard();
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
        if (cut && this.fading < 0.0F) {
            this.fading = 0.0F;
            SPEAKING.remove(this.speaker.getId(), this);
        }
        if (a >= 0.0F && this.fading < 0.0F) {
            this.seen = a;
        }
        if (this.fading >= 0.0F && ++this.fading >= FADE_TICKS) {
            this.end();
            return;
        }
        this.volume = this.heard() * (this.fading < 0.0F ? 1.0F : 1.0F - this.fading / FADE_TICKS);
    }

    private float heard() {
        Minecraft minecraft = Minecraft.getInstance();
        if (this.speaker == minecraft.player) {
            return 1.0F;
        }
        double far = minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(this.speaker.getEyePosition());
        return (float) Mth.clamp(1.0 - far / HEARD, 0.0, 1.0);
    }

    private void end() {
        this.stop();
        SPEAKING.remove(this.speaker.getId(), this);
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
                minecraft.getSoundManager().play(speech);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SPEAKING.clear();
    }
}
