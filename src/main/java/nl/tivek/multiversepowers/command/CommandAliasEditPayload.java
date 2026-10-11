package nl.tivek.multiversepowers.command;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A host's change on the Commands page, as `CommandAliases.edit` takes it.
public record CommandAliasEditPayload(String name, String command) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CommandAliasEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "command_alias_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandAliasEditPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(64), CommandAliasEditPayload::name,
                    ByteBufCodecs.stringUtf8(CommandAliases.LONGEST + 8), CommandAliasEditPayload::command,
                    CommandAliasEditPayload::new);

    @Override
    public CustomPacketPayload.Type<CommandAliasEditPayload> type() {
        return TYPE;
    }
}
