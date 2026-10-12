package nl.tivek.multiversepowers.character.docock;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// What Doctor Octopus's player does with his tentacles: `action`, the tentacles it is for (`mask`, a bit per
// tentacle; 0 for whichever the rules pick), the creature it aims at (-1 for none) and, for a slam, its way.
public record OctoInputPayload(int action, int mask, int target, float x, float y, float z)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OctoInputPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "octo_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OctoInputPayload> STREAM_CODEC = CustomPacketPayload
            .codec(OctoInputPayload::write, OctoInputPayload::read);

    public static final int GRAB = 0;
    public static final int SQUEEZE = 1;
    public static final int SQUEEZE_END = 2;
    public static final int SLAM = 3;
    public static final int THROW = 4;
    public static final int RELEASE = 5;
    public static final int STRIKE = 6;

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.action);
        buf.writeVarInt(this.mask);
        buf.writeVarInt(this.target + 1);
        buf.writeFloat(this.x);
        buf.writeFloat(this.y);
        buf.writeFloat(this.z);
    }

    private static OctoInputPayload read(RegistryFriendlyByteBuf buf) {
        return new OctoInputPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt() - 1, buf.readFloat(),
                buf.readFloat(), buf.readFloat());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
