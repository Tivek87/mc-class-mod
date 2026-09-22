package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/** Server tells a player to spend stamina (a hit taken while blocking). Stamina itself lives on the client. */
public record StaminaCostPayload(float amount) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StaminaCostPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "stamina_cost"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StaminaCostPayload> STREAM_CODEC = CustomPacketPayload
            .codec(StaminaCostPayload::write, StaminaCostPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeFloat(this.amount);
    }

    private static StaminaCostPayload read(RegistryFriendlyByteBuf buf) {
        return new StaminaCostPayload(buf.readFloat());
    }

    @Override
    public CustomPacketPayload.Type<StaminaCostPayload> type() {
        return TYPE;
    }
}
