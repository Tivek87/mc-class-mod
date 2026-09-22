package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * The client presses one of the ten ability keys, or tells that a key it holds down went up or down.
 * The key is only a number (see AbilitySlot); which ability it is depends on the character the player
 * is, and the server looks that up and checks everything itself.
 *
 * @param action an AbilitySlot index, {@link #CLIMB} or {@link #PLACE}
 * @param on     for a key you hold down: pressed or let go
 * @param data   extra: {@code Characters.SNEAKING} while crouching, or for {@link #CLIMB} the
 *               Direction ordinal of the surface
 */
public record AbilityActionPayload(int action, boolean on, int data) implements CustomPacketPayload {
    public static final int CLIMB = 100;
    /** Right click while the tentacles carry blocks: put them down where you aim. */
    public static final int PLACE = 101;

    public static final CustomPacketPayload.Type<AbilityActionPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "ability_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityActionPayload> STREAM_CODEC = CustomPacketPayload
            .codec(AbilityActionPayload::write, AbilityActionPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.action);
        buf.writeBoolean(this.on);
        buf.writeVarInt(this.data);
    }

    private static AbilityActionPayload read(RegistryFriendlyByteBuf buf) {
        return new AbilityActionPayload(buf.readVarInt(), buf.readBoolean(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<AbilityActionPayload> type() {
        return TYPE;
    }
}
