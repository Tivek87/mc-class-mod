package nl.tivek.multiversepowers.faction.mob;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Every creature of the game and how each stands to the others by the game's own rules: `standings` holds a
// `Standing` ordinal per creature in `types` and target, the targets being the player first, then `types` in order.
public record MobTablePayload(List<String> types, byte[] standings) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobTablePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_table"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MobTablePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(4096)), MobTablePayload::types,
            ByteBufCodecs.byteArray(1 << 20), MobTablePayload::standings, MobTablePayload::new);

    public int at(int actor, int target) {
        return actor * (this.types.size() + 1) + target;
    }

    @Override
    public CustomPacketPayload.Type<MobTablePayload> type() {
        return TYPE;
    }
}
