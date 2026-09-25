package nl.tivek.multiversepowers.engine.fx;

import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

public record ParticlesPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final int MAX_ENTRIES = 8192;
    private static final int MAX_KINDS = 1024;

    public static final CustomPacketPayload.Type<ParticlesPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "particles"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ParticlesPayload> STREAM_CODEC = StreamCodec.of(
            ParticlesPayload::write, ParticlesPayload::read);

    public record Entry(ParticleOptions options, boolean force, double x, double y, double z, float dx, float dy,
            float dz, float speed, int count) {
    }

    private static void write(RegistryFriendlyByteBuf buffer, ParticlesPayload payload) {
        Map<ParticleOptions, Integer> index = new IdentityHashMap<>();
        List<ParticleOptions> kinds = new ArrayList<>();
        for (Entry entry : payload.entries) {
            index.computeIfAbsent(entry.options, options -> {
                kinds.add(options);
                return kinds.size() - 1;
            });
        }
        buffer.writeVarInt(kinds.size());
        for (ParticleOptions options : kinds) {
            ParticleTypes.STREAM_CODEC.encode(buffer, options);
        }
        buffer.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buffer.writeVarInt(index.get(entry.options));
            buffer.writeBoolean(entry.force);
            buffer.writeDouble(entry.x);
            buffer.writeDouble(entry.y);
            buffer.writeDouble(entry.z);
            buffer.writeFloat(entry.dx);
            buffer.writeFloat(entry.dy);
            buffer.writeFloat(entry.dz);
            buffer.writeFloat(entry.speed);
            buffer.writeVarInt(entry.count);
        }
    }

    private static ParticlesPayload read(RegistryFriendlyByteBuf buffer) {
        int kindCount = buffer.readVarInt();
        if (kindCount < 0 || kindCount > MAX_KINDS) {
            throw new DecoderException("Too many kinds of particle: " + kindCount);
        }
        List<ParticleOptions> kinds = new ArrayList<>(kindCount);
        for (int i = 0; i < kindCount; i++) {
            kinds.add(ParticleTypes.STREAM_CODEC.decode(buffer));
        }
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_ENTRIES) {
            throw new DecoderException("Too many particles: " + count);
        }
        List<Entry> entries = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int kind = buffer.readVarInt();
            if (kind < 0 || kind >= kinds.size()) {
                throw new DecoderException("No such kind of particle: " + kind);
            }
            entries.add(new Entry(kinds.get(kind), buffer.readBoolean(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readVarInt()));
        }
        return new ParticlesPayload(entries);
    }

    @Override
    public CustomPacketPayload.Type<ParticlesPayload> type() {
        return TYPE;
    }
}
