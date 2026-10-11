package nl.tivek.multiversepowers.command;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// A host's change on the Commands page, as `CommandAliases.change` takes it: SAVE (a: old name or "", b: name, c:
// command, d: category), DELETE (a: name), BUILT_IN (a: its own name, b: new name, d: category), REMOVE and RESTORE
// (a: its own name; RESTORE "" gives every one back), CATEGORY (a: id or "" for a new one, b: label or "" to take it
// away) and CATEGORY_RESET (a: a built-in category's id).
public record CommandEditPayload(int action, String a, String b, String c, String d) implements CustomPacketPayload {
    public static final int SAVE = 0;
    public static final int DELETE = 1;
    public static final int BUILT_IN = 2;
    public static final int REMOVE = 3;
    public static final int RESTORE = 4;
    public static final int CATEGORY = 5;
    public static final int CATEGORY_RESET = 6;

    public static final CustomPacketPayload.Type<CommandEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "command_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CommandEditPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_INT, CommandEditPayload::action,
                    ByteBufCodecs.stringUtf8(64), CommandEditPayload::a,
                    ByteBufCodecs.stringUtf8(64), CommandEditPayload::b,
                    ByteBufCodecs.stringUtf8(CommandAliases.LONGEST + 8), CommandEditPayload::c,
                    ByteBufCodecs.stringUtf8(64), CommandEditPayload::d,
                    CommandEditPayload::new);

    public static CommandEditPayload of(int action, String a) {
        return new CommandEditPayload(action, a, "", "", "");
    }

    @Override
    public CustomPacketPayload.Type<CommandEditPayload> type() {
        return TYPE;
    }
}
