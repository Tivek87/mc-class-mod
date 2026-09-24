package nl.tivek.multiversepowers.character.greenlantern.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.ConstructHoldPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;

/**
 * Which construct Green Lantern is holding right now. Every pick is told to the server, which makes the construct and
 * shows it to everyone (the sword and shield, see {@link SwordArms}); your own game plays its taking shape at once. This
 * also remembers your pick, so the wheel and the bar above your hotbar can show it, and notes when it changed so both
 * can flash.
 */
public final class ConstructChoice {
    /** How long the flash around your crosshair lasts after a pick, in milliseconds. */
    public static final long FLASH_MS = 420L;

    private static Construct held = Construct.NONE;
    /** The last real construct you had out, so a tap of the key can put it straight back. */
    private static Construct last = Construct.WHEEL.get(0);
    private static long changedAt = Long.MIN_VALUE / 2L;

    private ConstructChoice() {
    }

    /** What is in your hands right now; {@link Construct#NONE} means only the ring. */
    public static Construct held() {
        return held;
    }

    /**
     * Takes a construct out, or puts the one you had away. Does nothing when you already hold it.
     *
     * @return true when this really changed what you hold
     */
    public static boolean take(Construct construct) {
        if (held == construct) {
            return false;
        }
        if (held != Construct.NONE) {
            last = held;
        }
        held = construct;
        changedAt = Util.getMillis();
        PacketDistributor.sendToServer(new ConstructHoldPayload(construct.ordinal()));
        SwordArms.picked(construct);
        Minecraft minecraft = Minecraft.getInstance();
        if (construct == Construct.NONE) {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_BREAK, 1.4F, 0.5F));
        } else {
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.8F));
            minecraft.getSoundManager().play(
                    SimpleSoundInstance.forUI(SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.4F));
        }
        return true;
    }

    /**
     * Swaps between empty hands and the last construct you had out. A tap of the construct key does this,
     * so putting something away and picking it up again never costs you the whole wheel.
     */
    public static void swap() {
        take(held == Construct.NONE ? last : Construct.NONE);
    }

    /**
     * Puts everything away without a sound: you stopped being Green Lantern, or left the world. The server lets go of
     * the construct by itself.
     */
    public static void forget() {
        held = Construct.NONE;
        changedAt = Long.MIN_VALUE / 2L;
        SwordArms.forget();
    }

    /** How long ago your pick changed, in milliseconds. */
    public static long since() {
        return Util.getMillis() - changedAt;
    }
}
