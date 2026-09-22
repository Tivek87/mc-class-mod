package nl.tivek.welcomescreen.client.stamina;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.client.config.SettingsPages;
import nl.tivek.welcomescreen.client.config.SettingsScreen;
import nl.tivek.welcomescreen.config.StaminaConfig;

/**
 * Client-side stamina system with in-game configurable stats.
 * When exhausted: sprinting and jumping are blocked until stamina recovers.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class StaminaClient {
    private static final float MAX_FORWARD_TIRED = 0.79F;

    private static float stamina = 100.0F;
    private static float previousStamina = 100.0F;
    private static boolean exhausted;
    private static int regenDelay;
    private static LocalPlayer trackedPlayer;

    private StaminaClient() {
    }

    public static float getMax() {
        return StaminaConfig.getMaxStamina();
    }

    public static float getStamina(float partialTick) {
        return Mth.lerp(partialTick, previousStamina, stamina);
    }

    public static boolean isExhausted() {
        return exhausted;
    }

    /**
     * Spends {@code amount} stamina if there is that much (always fine in creative and spectator).
     *
     * @return false when too tired: nothing is spent
     */
    public static boolean tryUse(float amount) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !usesStamina(minecraft, player)) {
            return true;
        }
        if (exhausted || stamina < amount) {
            return false;
        }
        drain(amount, player);
        return true;
    }

    /** Spends up to {@code amount} stamina; running out makes you exhausted as usual. */
    public static void use(float amount) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && usesStamina(minecraft, player)) {
            drain(amount, player);
        }
    }

    public static void onConfigUpdated() {
        float max = getMax();
        if (stamina > max) {
            stamina = max;
        }
        if (previousStamina > max) {
            previousStamina = max;
        }
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("stamina")
                        .executes(ctx -> {
                            Minecraft mc = Minecraft.getInstance();
                            mc.tell(() -> mc.setScreen(new SettingsScreen(null, SettingsPages.stamina())));
                            return 1;
                        })
                        .then(Commands.literal("config")
                                .executes(ctx -> {
                                    Minecraft mc = Minecraft.getInstance();
                                    mc.tell(() -> mc.setScreen(new SettingsScreen(null, SettingsPages.stamina())));
                                    return 1;
                                }))
        );
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != trackedPlayer) {
            trackedPlayer = player;
            reset();
        }
        if (player == null) {
            return;
        }

        float max = getMax();
        previousStamina = stamina;
        if (!usesStamina(minecraft, player)) {
            stamina = max;
            exhausted = false;
            return;
        }

        // Als je uitgeput bent: blokkeer sprinten direct
        if (exhausted && player.isSprinting()) {
            player.setSprinting(false);
        }

        if (player.isSprinting()) {
            drain(StaminaConfig.getSprintDrain(), player);
        } else if (regenDelay > 0) {
            regenDelay--;
        } else if (stamina < max) {
            stamina = Math.min(max, stamina + StaminaConfig.getRegenRate());
        }

        if (exhausted && stamina >= StaminaConfig.getExhaustionThreshold()) {
            exhausted = false;
        }
    }

    @SubscribeEvent
    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || !usesStamina(minecraft, player)) {
            return;
        }

        Input input = event.getInput();
        if (exhausted) {
            // Blokkeer sprinten, maar laat normaal lopen toe
            player.setSprinting(false);
            if (input.forwardImpulse > MAX_FORWARD_TIRED) {
                input.forwardImpulse = MAX_FORWARD_TIRED;
            }
        }

        // Blokkeer springen als stamina op is of te laag voor een sprong
        boolean groundJump = input.jumping && !player.isInWater() && !player.isInLava() && !player.onClimbable();
        if (groundJump && (exhausted || stamina < StaminaConfig.getJumpCost())) {
            input.jumping = false;
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player != null && event.getEntity() == player && usesStamina(minecraft, player)) {
            drain(StaminaConfig.getJumpCost(), player);
        }
    }

    /** False in creative and spectator: no drain, no blocking, and no bar on screen. */
    static boolean usesStamina(Minecraft minecraft, LocalPlayer player) {
        if (player.isCreative() || player.isSpectator()) {
            return false;
        }
        MultiPlayerGameMode gameMode = minecraft.gameMode;
        return gameMode == null || !gameMode.isAlwaysFlying();
    }

    private static void drain(float amount, LocalPlayer player) {
        stamina = Math.max(0.0F, stamina - amount);
        regenDelay = StaminaConfig.getRegenDelay();
        if (stamina <= 0.0F) {
            exhausted = true;
            if (player != null) {
                player.setSprinting(false);
            }
        }
    }

    private static void reset() {
        float max = getMax();
        stamina = max;
        previousStamina = max;
        exhausted = false;
        regenDelay = 0;
    }
}
