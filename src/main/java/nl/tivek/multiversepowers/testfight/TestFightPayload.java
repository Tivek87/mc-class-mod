package nl.tivek.multiversepowers.testfight;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Tells the players near a test fight that it starts (their games play it from now on), that the kick lets its
// creature go (it flies limp from here) or that it is over. `fighter` is -1 once the player is gone.
public record TestFightPayload(int fighter, int target, int phase) implements CustomPacketPayload {
    public static final int END = 0;
    public static final int START = 1;
    public static final int LET_GO = 2;

    public static final CustomPacketPayload.Type<TestFightPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "test_fight"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TestFightPayload> STREAM_CODEC = CustomPacketPayload
            .codec(TestFightPayload::write, TestFightPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.fighter + 1);
        buf.writeVarInt(this.target);
        buf.writeVarInt(this.phase);
    }

    private static TestFightPayload read(RegistryFriendlyByteBuf buf) {
        return new TestFightPayload(buf.readVarInt() - 1, buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<TestFightPayload> type() {
        return TYPE;
    }
}
