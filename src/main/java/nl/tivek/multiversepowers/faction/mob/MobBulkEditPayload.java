package nl.tivek.multiversepowers.faction.mob;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Many pairs at once from the Mobs page, as `MobRules.bulk` takes them.
public record MobBulkEditPayload(List<String> actors, List<String> targets, int standing) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MobBulkEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mob_bulk_edit"));
    public static final int MOST = 1000;

    public static final StreamCodec<RegistryFriendlyByteBuf, MobBulkEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(MOST)), MobBulkEditPayload::actors,
            ByteBufCodecs.stringUtf8(256).apply(ByteBufCodecs.list(MOST)), MobBulkEditPayload::targets,
            ByteBufCodecs.VAR_INT, MobBulkEditPayload::standing, MobBulkEditPayload::new);

    @Override
    public CustomPacketPayload.Type<MobBulkEditPayload> type() {
        return TYPE;
    }
}
