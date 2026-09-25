package nl.tivek.multiversepowers.character;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record AbilityActionPayload(int action, boolean on, int data) implements CustomPacketPayload {
    public static final int CLIMB = 100;
    public static final int PLACE = 101;

    public static final CustomPacketPayload.Type<AbilityActionPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "ability_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityActionPayload> STREAM_CODEC = CustomPacketPayload
            .codec(AbilityActionPayload::write, AbilityActionPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.action);
        buf.writeBoolean(this.on);
        buf.writeVarInt(this.data);
    }

    private static AbilityActionPayload read(RegistryFriendlyByteBuf buf) {
        return new AbilityActionPayload(buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<AbilityActionPayload> type() {
        return TYPE;
    }
}
