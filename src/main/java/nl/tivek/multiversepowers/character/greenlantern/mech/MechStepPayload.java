package nl.tivek.multiversepowers.character.greenlantern.mech;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.MultiversePowers;

// Where a foot of the pilot's mech came down this tick, on the ground under its ankle.
public record MechStepPayload(Vec3 sole) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<MechStepPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "mech_step"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MechStepPayload> STREAM_CODEC = CustomPacketPayload
            .codec(MechStepPayload::write, MechStepPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeDouble(this.sole.x);
        buf.writeDouble(this.sole.y);
        buf.writeDouble(this.sole.z);
    }

    private static MechStepPayload read(RegistryFriendlyByteBuf buf) {
        return new MechStepPayload(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
    }

    @Override
    public CustomPacketPayload.Type<MechStepPayload> type() {
        return TYPE;
    }
}
