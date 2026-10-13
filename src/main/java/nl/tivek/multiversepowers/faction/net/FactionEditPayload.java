package nl.tivek.multiversepowers.faction.net;

import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.multiversepowers.MultiversePowers;

// Something done on the Factions page, as `FactionEdits.page` takes it: what (`action`), to which faction, and what
// with (`key` and `values`, by the action).
public record FactionEditPayload(int action, String faction, String key, List<String> values)
        implements CustomPacketPayload {
    // `key`: its name.
    public static final int CREATE = 0;
    public static final int DELETE = 1;
    // One of a faction's words or numbers: `key` and the first value.
    public static final int WORD = 2;
    // `values`: kinds of creature put in the faction, or out of every faction.
    public static final int ADD = 3;
    public static final int REMOVE = 4;
    // Toward the faction `key`: the first value friendly, neutral, hostile or game.
    public static final int RELATION = 5;
    // One of the numbers of reputation: `key` and the first value.
    public static final int RULE = 6;
    public static final int UNDO = 7;
    public static final int RESET = 8;
    // One creature of its own, `key` its entity id: its faction (`faction`: an id, "-" for none, "" for its kind's)
    // or how it stands to players (the first value, game for as its faction does).
    public static final int MOB_FACTION = 9;
    public static final int MOB_STANCE = 10;
    // The player themselves, into a faction open to anyone, or out of theirs.
    public static final int JOIN = 11;
    public static final int LEAVE = 12;
    // The sender's own reputation with the faction, to try it out: `key` set or add, the first value the number.
    public static final int REPUTATION = 13;
    public static final int MOST = 64;

    public static final CustomPacketPayload.Type<FactionEditPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "faction_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FactionEditPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FactionEditPayload::action,
            ByteBufCodecs.stringUtf8(32), FactionEditPayload::faction,
            ByteBufCodecs.stringUtf8(64), FactionEditPayload::key,
            ByteBufCodecs.stringUtf8(400).apply(ByteBufCodecs.list(MOST)), FactionEditPayload::values,
            FactionEditPayload::new);

    public FactionEditPayload(int action, String faction, String key, String value) {
        this(action, faction, key, List.of(value));
    }

    public FactionEditPayload(int action, String faction) {
        this(action, faction, "", List.of());
    }

    public String value() {
        return this.values.isEmpty() ? "" : this.values.get(0);
    }

    @Override
    public CustomPacketPayload.Type<FactionEditPayload> type() {
        return TYPE;
    }
}
