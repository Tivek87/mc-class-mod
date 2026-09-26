package nl.tivek.multiversepowers.engine.fx;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;

public final class Voices {
    public static final double HEARD = 24.0;

    private Voices() {
    }

    public static void say(Entity speaker, ResourceLocation sound) {
        if (speaker.level() instanceof ServerLevel level) {
            PacketDistributor.sendToPlayersNear(level, null, speaker.getX(), speaker.getY(), speaker.getZ(), HEARD,
                    new VoicePayload(speaker.getId(), sound));
        }
    }
}
