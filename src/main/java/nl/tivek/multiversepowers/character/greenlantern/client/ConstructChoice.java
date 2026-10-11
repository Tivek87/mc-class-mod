package nl.tivek.multiversepowers.character.greenlantern.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import nl.tivek.multiversepowers.character.greenlantern.client.body.heavy.ClientHeavy;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.greenlantern.client.body.flame.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.whip.WhipArms;
import nl.tivek.multiversepowers.character.greenlantern.construct.Construct;
import nl.tivek.multiversepowers.character.greenlantern.construct.ConstructHoldPayload;

public final class ConstructChoice {
    public static final long FLASH_MS = 420L;

    private static Construct held = Construct.NONE;
    private static Construct last = Construct.WHEEL.get(0);
    private static long changedAt = Long.MIN_VALUE / 2L;

    private ConstructChoice() {
    }

    // A weapon the server would not form or broke up (the ring ran dry) is let go here too, so it can be taken again.
    public static Construct held() {
        if (held != Construct.NONE && !shown(held)) {
            last = held;
            held = Construct.NONE;
        }
        return held;
    }

    private static boolean shown(Construct construct) {
        return switch (construct) {
            case SWORD_SHIELD -> SwordArms.present();
            case FLAMETHROWER -> FlameArms.present();
            case ENERGY_WHIP -> WhipArms.present();
            case BATTLEAXE, CHAINSAW, ROCKET_LAUNCHER, SHOTGUN, REVOLVERS, ARM_CANNON, MINIGUN ->
                    ClientHeavy.present(construct);
            default -> true;
        };
    }

    public static boolean take(Construct construct) {
        if (held() == construct) {
            return false;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && (construct.shut(ClientRing.flight(minecraft.player, 0.0F) >= 0.0F)
                || construct != Construct.NONE && ClientRing.recharge(minecraft.player, 0.0F) >= 0.0F)) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F, 0.5F));
            return false;
        }
        CharacterAbility wheel = GameCharacter.GREEN_LANTERN.byName("construct_wheel");
        if (construct != Construct.NONE && minecraft.player != null && wheel != null
                && ClientRing.power(minecraft.player) + 1.0E-4F < wheel.value("formPowerCost")) {
            ClientCharacter.noPower(minecraft.player, GameCharacter.GREEN_LANTERN);
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F, 0.5F));
            return false;
        }
        if (construct.summons()) {
            PacketDistributor.sendToServer(new ConstructHoldPayload(construct.ordinal()));
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.5F, 0.4F));
            return true;
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
        ClientHeavy.picked(construct);
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
        take(held() == Construct.NONE ? last : Construct.NONE);
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
