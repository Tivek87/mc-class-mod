package nl.tivek.multiversepowers.character.docock;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Doctor Octopus's four tentacles for every game that sees him: his stance, and per tentacle whether it stands on the
// ground, holds a creature (which) and squeezes it, the move it last started (counted on by `seq`), the way of a slam and
// what a blow goes at. `held` and `target` are entity ids, -1 for none; an empty `flags` takes his tentacles in.
public record OctoStatePayload(int player, int stance, int[] flags, int[] held, int[] move, int[] seq, float[] way,
        int[] target) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OctoStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "octo_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OctoStatePayload> STREAM_CODEC = CustomPacketPayload
            .codec(OctoStatePayload::write, OctoStatePayload::read);

    public static final int GROUND = 1;
    public static final int HOLDING = 2;
    public static final int SQUEEZING = 4;

    public static OctoStatePayload gone(int player) {
        return new OctoStatePayload(player, 0, new int[0], new int[0], new int[0], new int[0], new float[0],
                new int[0]);
    }

    public boolean out() {
        return this.flags.length == TentacleReach.COUNT;
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.player);
        buf.writeVarInt(this.stance);
        buf.writeVarInt(this.flags.length);
        for (int i = 0; i < this.flags.length; i++) {
            buf.writeVarInt(this.flags[i]);
            buf.writeVarInt(this.held[i] + 1);
            buf.writeVarInt(this.move[i]);
            buf.writeVarInt(this.seq[i]);
            buf.writeFloat(this.way[i * 3]);
            buf.writeFloat(this.way[i * 3 + 1]);
            buf.writeFloat(this.way[i * 3 + 2]);
            buf.writeVarInt(this.target[i] + 1);
        }
    }

    private static OctoStatePayload read(RegistryFriendlyByteBuf buf) {
        int player = buf.readVarInt();
        int stance = buf.readVarInt();
        int count = Math.min(TentacleReach.COUNT, buf.readVarInt());
        int[] flags = new int[count];
        int[] held = new int[count];
        int[] move = new int[count];
        int[] seq = new int[count];
        float[] way = new float[count * 3];
        int[] target = new int[count];
        for (int i = 0; i < count; i++) {
            flags[i] = buf.readVarInt();
            held[i] = buf.readVarInt() - 1;
            move[i] = buf.readVarInt();
            seq[i] = buf.readVarInt();
            way[i * 3] = buf.readFloat();
            way[i * 3 + 1] = buf.readFloat();
            way[i * 3 + 2] = buf.readFloat();
            target[i] = buf.readVarInt() - 1;
        }
        return new OctoStatePayload(player, stance, flags, held, move, seq, way, target);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
