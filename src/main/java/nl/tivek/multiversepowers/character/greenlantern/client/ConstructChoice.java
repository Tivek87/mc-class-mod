package nl.tivek.multiversepowers.character.greenlantern.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.ConstructHoldPayload;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.WhipArms;

public final class ConstructChoice {
    public static final long FLASH_MS = 420L;

    private static Construct held = Construct.NONE;
    private static Construct last = Construct.WHEEL.get(0);
    private static long changedAt = Long.MIN_VALUE / 2L;

    private ConstructChoice() {
    }

    public static Construct held() {
        return held;
    }

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
        FlameArms.picked(construct);
        WhipArms.picked(construct);
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

    public static void swap() {
        take(held == Construct.NONE ? last : Construct.NONE);
    }

    public static void forget() {
        held = Construct.NONE;
        changedAt = Long.MIN_VALUE / 2L;
        SwordArms.forget();
        FlameArms.forget();
        WhipArms.forget();
    }

    public static long since() {
        return Util.getMillis() - changedAt;
    }
}
