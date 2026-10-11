package nl.tivek.multiversepowers.faction.mob;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A host turning a creature's own spawning on or off on the Mobs page, as `MobRules.editSpawn` takes it.
public record MobSpawnEditPayload(String creature, boolean spawns) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobSpawnEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_spawn_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobSpawnEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256), MobSpawnEditPayload::creature,
            ByteBufCodecs.BOOL, MobSpawnEditPayload::spawns, MobSpawnEditPayload::new);

    @Override
    public CustomPacketPayload.Type<MobSpawnEditPayload> type() {
        return TYPE;
    }
}
