package nl.tivek.multiversepowers.character.greenlantern.client.hud;

import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.client.ClientCharacter;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.character.greenlantern.summon.SummonOrderPayload;
import nl.tivek.multiversepowers.character.greenlantern.summon.client.ClientSummons;

// The construct wheel's key: held it opens the wheel, tapped it swaps to the last construct, or after a summoning
// switches the summons between staying and following; held on one of your own summons it sends that one away.
public final class ConstructWheel {
    private static final long TAP_MS = 160L;
    private static final long DISMISS_MS = 3000L;

    private static long downSince = -1L;
    private static boolean piloting;
    // The summon the key is held on to send it away, and since when; `spent` while the key stays down after it.
    private static int dismissing = -1;
    private static boolean spent;

    private ConstructWheel() {
    }

    public static void tick(Minecraft minecraft, KeyMapping key) {
        boolean down = minecraft.screen == null && key.isDown();
        if (minecraft.player != null && ClientConstructs.piloted(minecraft.player.getId(), 0.0F) != null) {
            // In the mech the key works its flamethrower (MechAssembly).
            CharacterAbility wheel = GameCharacter.GREEN_LANTERN.byName("construct_wheel");
            if (down && !piloting && wheel != null) {
                ClientCharacter.sendAction(wheel, true, 0);
            }
            piloting = down;
            downSince = -1L;
            return;
        }
        piloting = false;
        long now = Util.getMillis();
        if (down) {
            if (spent) {
                return;
            }
            if (downSince < 0L) {
                downSince = now;
                Entity aimed = minecraft.crosshairPickEntity;
                dismissing = ClientSummons.mine(aimed) ? aimed.getId() : -1;
            }
            if (dismissing >= 0) {
                Entity aimed = minecraft.crosshairPickEntity;
                if (aimed == null || aimed.getId() != dismissing) {
                    dismissing = -1;
                    spent = now - downSince >= TAP_MS;
                } else if (now - downSince >= DISMISS_MS) {
                    PacketDistributor.sendToServer(new SummonOrderPayload(dismissing));
                    minecraft.getSoundManager().play(
                            SimpleSoundInstance.forUI(SoundEvents.AMETHYST_CLUSTER_BREAK, 1.2F, 0.8F));
                    dismissing = -1;
                    spent = true;
                }
                return;
            }
            if (now - downSince >= TAP_MS) {
                downSince = -1L;
                minecraft.setScreen(new ConstructWheelScreen(key));
            }
            return;
        }
        boolean tapped = downSince >= 0L && !spent && now - downSince < TAP_MS;
        downSince = -1L;
        dismissing = -1;
        spent = false;
        if (tapped) {
            if (ConstructChoice.summoned() && ClientSummons.anyMine()) {
                PacketDistributor.sendToServer(new SummonOrderPayload(SummonOrderPayload.SWITCH));
            } else {
                ConstructChoice.swap();
            }
        }
    }

    // How far the key has been held on a summon to send it away, 0 to 1; 0 while it is not.
    public static float dismissing() {
        return dismissing < 0 || downSince < 0L ? 0.0F
                : Math.min(1.0F, (Util.getMillis() - downSince) / (float) DISMISS_MS);
    }

    public static void stop() {
        downSince = -1L;
        dismissing = -1;
    }
}
