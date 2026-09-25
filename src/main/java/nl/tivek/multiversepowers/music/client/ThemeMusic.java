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

/**
 * The multiverse theme plays in the main menu in place of Minecraft's menu music, and starts again as soon as it
 * ends. In a world Minecraft's own music plays as always; the music slider sets the volume of both.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ThemeMusic {
    /** No wait before it starts or starts again, and it cuts off any other track that is still playing. */
    private static final Music THEME = new Music(Holder.direct(SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "music.multiverse_theme"))), 0, 0, true);

    private ThemeMusic() {
    }

    /**
     * Only where Minecraft would play its menu music. Last in line and even after a cancel, so no other menu track
     * outlasts it; silence asked for stays silent.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onSelectMusic(SelectMusicEvent event) {
        if (event.getMusic() != null && event.getOriginalMusic() == Musics.MENU) {
            event.setMusic(audible() ? THEME : null);
        }
    }

    /**
     * A slider at 0 cuts the theme, but Minecraft keeps counting the cut track as playing and would never start it
     * again; choosing no music while nothing can be heard lets it start afresh once the slider goes back up.
     */
    private static boolean audible() {
        Options options = Minecraft.getInstance().options;
        return options.getSoundSourceVolume(SoundSource.MASTER) > 0.0F
                && options.getSoundSourceVolume(SoundSource.MUSIC) > 0.0F;
    }
}
