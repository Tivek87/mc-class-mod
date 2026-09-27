package nl.tivek.multiversepowers.engine.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

public final class Sounds {
    private Sounds() {
    }

    // The mod's own sounds are named in sounds.json only, not registered: they travel to clients by their name.
    public static SoundEvent of(String name) {
        return SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, name));
    }

    public static void play(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }
}
