package nl.tivek.multiversepowers.testfight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A player asks to start a test fight with the creature they held the scroll wheel on; the server checks it all again.
public record TestFightRequest(int target) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<TestFightRequest> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "test_fight_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TestFightRequest> STREAM_CODEC = CustomPacketPayload
            .codec(TestFightRequest::write, TestFightRequest::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.target);
    }

    private static TestFightRequest read(RegistryFriendlyByteBuf buf) {
        return new TestFightRequest(buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<TestFightRequest> type() {
        return TYPE;
    }
}
