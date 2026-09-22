package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Server tells the player what the tentacles have in their claws, so the mouse can do something else
 * than usual: a left click throws what they hold, and a right click puts blocks back down.
 *
 * @param holding a creature or blocks: a left click throws it instead of attacking
 * @param blocks  blocks only: a right click puts them down instead of using your item
 */
public record GrabStatePayload(boolean holding, boolean blocks) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GrabStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "grab_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GrabStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GrabStatePayload::holding,
            ByteBufCodecs.BOOL, GrabStatePayload::blocks,
            GrabStatePayload::new);

    @Override
    public CustomPacketPayload.Type<GrabStatePayload> type() {
        return TYPE;
    }
}
