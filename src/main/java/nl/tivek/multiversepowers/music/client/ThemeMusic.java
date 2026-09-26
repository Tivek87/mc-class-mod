package nl.tivek.multiversepowers.music.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.Music;
import net.minecraft.sounds.Musics;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThemeMusic {
    // 0, 0, true: starts at once, no delay, and cuts any track still playing.
    private static final Music THEME = new Music(Holder.direct(SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "music.multiverse_theme"))), 0, 0, true);

    private ThemeMusic() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onSelectMusic(SelectMusicEvent event) {
        if (event.getMusic() != null && event.getOriginalMusic() == Musics.MENU && ClientSettings.themeMusic()) {
            event.setMusic(audible() ? THEME : null);
        }
    }

    // Minecraft never restarts a track it still thinks is playing: go silent instead.
    private static boolean audible() {
        Options options = Minecraft.getInstance().options;
        return options.getSoundSourceVolume(SoundSource.MASTER) > 0.0F
                && options.getSoundSourceVolume(SoundSource.MUSIC) > 0.0F;
    }
}
