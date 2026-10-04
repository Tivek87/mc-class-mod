package nl.tivek.multiversepowers.character.thor;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;

// What Thor is doing, for every game that sees him: whether he flies, floats after a super jump, flies at lightning
// speed, carries someone, winds up a thunderclap, has his hammer in hand, thrown or resting in the world (drawn back
// to throw, flying back to him, pulling him to it), is charged or has a charged hammer, has his storm over him, and
// the move he just started (a dash, a jump, a blink, a dive, a slam, a blow, a grab and what follows it, a pull to his
// hammer and its catch, his storm, a bolt he calls, the lightning bomb) with what it needs.
public record ThorStatePayload(int entity, int flags, int move, int arg) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ThorStatePayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID, "thor_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThorStatePayload> STREAM_CODEC = CustomPacketPayload
            .codec(ThorStatePayload::write, ThorStatePayload::read);

    public static final int FLYING = 1;
    public static final int FLOATING = 2;
    public static final int LIGHTNING = 4;
    public static final int CARRYING = 8;
    public static final int CHARGING = 16;
    public static final int ARMED = 32;
    public static final int THROWN = 64;
    public static final int CHARGED = 128;
    public static final int HAMMER_CHARGED = 256;
    // His storm hangs over him.
    public static final int STORMING = 512;
    // His thrown hammer rests in the world (always with THROWN); he is pulled to it; it flies back to him and his arm
    // reaches for it; he draws it back to throw it and follow.
    public static final int RESTING = 1024;
    public static final int PULLING = 2048;
    public static final int CALLING = 4096;
    public static final int COCKED = 8192;

    // The move carried along: only when one starts, else NONE (a change of flags alone).
    public static final int NONE = 0;
    public static final int DASH = 1;
    public static final int JUMP = 2;
    public static final int BLINK = 3;
    public static final int DIVE = 4;
    public static final int SLAM = 5;
    public static final int TAKE_OFF = 6;
    // A take-off's arg when a knockdown let go of him in the air: his own game flies on as he is.
    public static final int CAUGHT = 1;
    public static final int TOUCH_DOWN = 7;
    // A blow of his combo: the arg is which (ThorBlow).
    public static final int BLOW = 8;
    // A blow the server throws for him (a grab's punches, the hammer's uppercut): shown in his own game too.
    public static final int STRIKE = 9;
    // He took hold of a creature (arg: ThorGrab.grabbed, its id and whether a dash brought him to it): his own game
    // stops the dash that brought him there.
    public static final int GRAB = 10;
    // He leaps high with it held over his head; then drops back down fast.
    public static final int HOIST = 11;
    public static final int DROP = 12;
    // He is pulled to his resting hammer: first the wait, then the dash (his own game moves him).
    public static final int PULL = 13;
    // How the grab ends, picked or not (arg: ThorGrab.Act): told apart from the move, as a blow is.
    public static final int GRAB_ACT = 14;
    // He calls up his storm, then calls a bolt down out of it.
    public static final int STORM = 15;
    public static final int CALL = 16;
    // He rises, charges and bursts as a lightning bomb (LightningBomb); with PUT_OUT, a knockdown stopped it.
    public static final int BOMB = 17;
    public static final int PUT_OUT = 1;
    // He caught his hammer: in his right hand, his left (flying) or onto his belt (his right hand holds a creature).
    public static final int CATCH = 18;
    public static final int RIGHT_HAND = 0;
    public static final int LEFT_HAND = 1;
    public static final int BELT = 2;

    public static void send(ServerPlayer player, int flags, int move, int arg) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                new ThorStatePayload(player.getId(), flags, move, arg));
    }

    public static void sendTo(ServerPlayer viewer, ServerPlayer thor, int flags) {
        PacketDistributor.sendToPlayer(viewer, new ThorStatePayload(thor.getId(), flags, NONE, 0));
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(this.entity);
        buf.writeVarInt(this.flags);
        buf.writeVarInt(this.move);
        buf.writeVarInt(this.arg);
    }

    private static ThorStatePayload read(RegistryFriendlyByteBuf buf) {
        return new ThorStatePayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<ThorStatePayload> type() {
        return TYPE;
    }
}
