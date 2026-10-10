package nl.tivek.multiversepowers.character.greenlantern.heavy;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A heavy weapon in a player's hands, told to the players near at every new move and once a second: which weapon, its
// move (HeavyMoves, IDLE between moves, BREAK while it breaks up), how many ticks into it, and which way he faced as
// it began, and the shots left in a gun.
public record HeavyPayload(int owner, int weapon, int move, int age, float yaw, int ammo)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<HeavyPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "heavy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeavyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HeavyPayload::owner, ByteBufCodecs.VAR_INT, HeavyPayload::weapon,
            ByteBufCodecs.VAR_INT, HeavyPayload::move, ByteBufCodecs.VAR_INT, HeavyPayload::age, ByteBufCodecs.FLOAT,
            HeavyPayload::yaw, ByteBufCodecs.VAR_INT, HeavyPayload::ammo, HeavyPayload::new);

    @Override
    public CustomPacketPayload.Type<HeavyPayload> type() {
        return TYPE;
    }
}
