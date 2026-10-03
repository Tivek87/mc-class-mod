package nl.tivek.multiversepowers.engine.client.ragdoll;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.fx.FirstPersonEye;
import nl.tivek.multiversepowers.engine.math.Ease;

// Your own player knocked down (PlayerKnockdowns): from the blow until the server lets go they cannot walk, jump,
// attack, use anything or call on a power, and in first person the eye drops to the ground with the body and rises
// as it gets up. Only what the server says counts, whether ragdolls are shown or not.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Downed {
    private static final int FLYING = Integer.MAX_VALUE;
    // Without word of landing, free again after this long all the same.
    private static final int LONGEST = 300;
    // The eye lying this high above the feet; it drops there in DROP ticks and rises in the RISE before the server
    // lets go, as the body gets up (Knocked: it stands MARGIN ticks before).
    private static final double LYING = 0.3;
    private static final int DROP = 6;
    private static final int RISE = 45;
    private static final int MARGIN = 5;

    // Ticks left down (FLYING while in the air, 0 when free), ticks since the blow and ticks lain on the ground.
    private static int left;
    private static int down;
    private static int lain = -1;

    static {
        FirstPersonEye.add(Downed::eye);
    }

    private Downed() {
    }

    public static boolean now() {
        return left != 0;
    }

    public static void told(int entity, int ticks) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getId() != entity) {
            return;
        }
        if (ticks == 0) {
            free();
        } else if (ticks < 0) {
            if (left == 0) {
                down = 0;
            }
            left = FLYING;
            if (!player.onGround()) {
                lain = -1;
            }
        } else {
            left = ticks;
        }
    }

    private static void free() {
        left = 0;
        down = 0;
        lain = -1;
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (left == 0 || minecraft.isPaused()) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isAlive()) {
            free();
            return;
        }
        down++;
        if (lain >= 0) {
            lain++;
        } else if (player.onGround()) {
            lain = 0;
        }
        if (left == FLYING ? down > LONGEST : --left <= 0) {
            free();
        }
    }

    // How far down the eye is, 0 standing to 1 lying.
    private static double low(float partialTick) {
        if (lain < 0) {
            return 0.0;
        }
        double dropped = Ease.smooth((lain + partialTick) / DROP);
        if (left == FLYING) {
            return dropped;
        }
        return dropped * (1.0 - Ease.smooth((RISE + MARGIN - left + partialTick) / RISE));
    }

    @Nullable
    private static Vec3 eye(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (left == 0 || player == null || minecraft.getCameraEntity() != player) {
            return null;
        }
        double low = low(partialTick);
        if (low <= 0.0) {
            return null;
        }
        return player.getPosition(partialTick).add(0.0, Mth.lerp(low, player.getEyeHeight(), LYING), 0.0);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onInput(MovementInputUpdateEvent event) {
        if (left == 0) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (left != 0) {
            event.setCanceled(true);
            event.setSwingHand(false);
        }
    }

    // Lying on the ground, nothing is held up before the eye.
    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (low(event.getPartialTick()) > 0.0) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        free();
    }
}
