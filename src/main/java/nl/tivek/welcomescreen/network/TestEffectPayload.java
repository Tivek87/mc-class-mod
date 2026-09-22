package nl.tivek.welcomescreen.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import nl.tivek.welcomescreen.WelcomeScreenMod;

/**
 * Developer tool: client asks the server to play an effect on the player, whatever class they have.
 *
 * @param kind      {@link #CEREMONY}, {@link #DEATH} or {@link #LEVEL_UP}
 * @param id        class id for a ceremony, group id for a death, unused for a level-up
 * @param showTitle ceremony only: play it as the first time, with the class name on screen
 */
public record TestEffectPayload(String kind, String id, boolean showTitle) implements CustomPacketPayload {
    public static final String CEREMONY = "ceremony";
    public static final String DEATH = "death";
    public static final String LEVEL_UP = "level_up";

    public static final CustomPacketPayload.Type<TestEffectPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID, "test_effect"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TestEffectPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TestEffectPayload::kind,
            ByteBufCodecs.STRING_UTF8, TestEffectPayload::id,
            ByteBufCodecs.BOOL, TestEffectPayload::showTitle,
            TestEffectPayload::new);

    @Override
    public CustomPacketPayload.Type<TestEffectPayload> type() {
        return TYPE;
    }
}
