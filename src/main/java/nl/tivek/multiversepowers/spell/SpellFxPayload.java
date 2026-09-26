package nl.tivek.multiversepowers.spell;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

// One drawn spell effect for everyone near it: what it is, where it runs from and to, the entity it follows (or -1),
// a seed so every game draws the same shape, and how many ticks it lasts where that is not fixed.
public record SpellFxPayload(int kind, Vec3 from, Vec3 to, int entity, int seed, int ticks)
        implements CustomPacketPayload {
    public static final int FIREBALL = 0;
    public static final int FIRE_BURST = 1;
    public static final int STORM = 2;
    public static final int BOLT = 3;
    public static final int ARC = 4;
    public static final int VIAL = 5;
    public static final int POISON = 6;
    public static final int GUST = 7;
    public static final int VOID_IN = 8;
    public static final int VOID_OUT = 9;
    public static final int AMBUSH = 10;

    private static final double VIEW_RANGE = 128.0;

    public static final CustomPacketPayload.Type<SpellFxPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "spell_fx"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpellFxPayload> STREAM_CODEC = StreamCodec.of(
            (buf, fx) -> {
                buf.writeVarInt(fx.kind);
                buf.writeVec3(fx.from);
                buf.writeVec3(fx.to);
                buf.writeVarInt(fx.entity + 1);
                buf.writeInt(fx.seed);
                buf.writeVarInt(fx.ticks);
            },
            buf -> new SpellFxPayload(buf.readVarInt(), buf.readVec3(), buf.readVec3(), buf.readVarInt() - 1,
                    buf.readInt(), buf.readVarInt()));

    static void send(ServerLevel level, int kind, Vec3 from, Vec3 to, int entity, int ticks) {
        SpellFxPayload fx = new SpellFxPayload(kind, from, to, entity, level.getRandom().nextInt(), ticks);
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, VIEW_RANGE, fx);
    }

    @Override
    public CustomPacketPayload.Type<SpellFxPayload> type() {
        return TYPE;
    }
}
