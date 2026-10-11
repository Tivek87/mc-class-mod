package nl.tivek.multiversepowers.schematic.client;

import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.client.AbilityKeys;
import nl.tivek.multiversepowers.schematic.client.hologram.Hologram;
import nl.tivek.multiversepowers.schematic.net.SchematicAskPayload;

// The wand in hand: the ability keys turn, mirror and move the hologram, the scroll wheel brings it nearer or farther,
// a left click places it; holding the move key, the walking keys move it a block at a time instead of the player.
// The keys are read before a character's would be, and only while the wand is held with no character.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class WandInput {
    static final int ROTATE = 0;
    static final int MIRROR = 1;
    static final int FOLLOW = 2;
    static final int REPLACE = 3;
    static final int UP = 4;
    static final int DOWN = 5;
    static final int UNDO = 6;
    static final int HIDE = 7;
    static final int CANCEL = 8;
    static final int MOVE = 9;
    static final int MENU = 10;
    private static final int REPEAT_AFTER = 8;
    private static final int REPEAT_EVERY = 2;

    // How long each way (forward, back, left, right, up, down) has been held with the move key.
    private static final int[] HELD = new int[6];

    private WandInput() {
    }

    static boolean ready(LocalPlayer player) {
        return SchematicClient.holding(player) && SchematicClient.allowed(player);
    }

    // A key the wand uses: bound, and on no key of the game's own (that one keeps doing what the game does).
    static boolean usable(int slot) {
        KeyMapping key = AbilityKeys.SLOTS[slot];
        return !key.isUnbound() && !AbilityKeys.sharesGameKey(key);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !ready(player)) {
            return;
        }
        for (int slot = 0; slot <= MENU; slot++) {
            KeyMapping key = AbilityKeys.SLOTS[slot];
            while (key.consumeClick()) {
                if (usable(slot)) {
                    act(slot, player);
                }
            }
        }
    }

    private static void act(int slot, LocalPlayer player) {
        switch (slot) {
            case MENU -> {
                SchematicScreen.open();
                return;
            }
            case UNDO -> {
                SchematicClient.ask(SchematicAskPayload.UNDO, "", "");
                return;
            }
            case CANCEL -> {
                SchematicClient.ask(SchematicAskPayload.CANCEL, "", "");
                return;
            }
            default -> {
            }
        }
        if (!Hologram.shown()) {
            if (slot != MOVE) {
                SchematicClient.say(Component.translatable(SchematicClient.PREFIX + "error.none"), true);
            }
            return;
        }
        switch (slot) {
            case ROTATE -> Hologram.rotate();
            case MIRROR -> Hologram.mirror();
            case FOLLOW -> Hologram.follow = 1 - Hologram.follow;
            case REPLACE -> Hologram.mode = (Hologram.mode + 1) % 3;
            case UP -> Hologram.nudge(0, 1, 0);
            case DOWN -> Hologram.nudge(0, -1, 0);
            case HIDE -> Hologram.hide();
            default -> {
                return;
            }
        }
        click();
    }

    static void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(),
                1.6F, 0.2F));
    }

    static boolean moving(LocalPlayer player) {
        return Minecraft.getInstance().screen == null && ready(player) && Hologram.shown() && usable(MOVE)
                && AbilityKeys.isDown(AbilityKeys.SLOTS[MOVE]);
    }

    // With the move key held the player stands still and the walking keys move the hologram, a block a press and on
    // and on while held.
    @SubscribeEvent(priority = EventPriority.HIGH)
    static void onMovementInput(MovementInputUpdateEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer player) || !moving(player)) {
            Arrays.fill(HELD, 0);
            return;
        }
        Input input = event.getInput();
        boolean[] ways = { input.forwardImpulse > 0.1F, input.forwardImpulse < -0.1F, input.leftImpulse > 0.1F,
                input.leftImpulse < -0.1F, input.jumping, input.shiftKeyDown };
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
        Direction facing = player.getDirection();
        Direction[] towards = { facing, facing.getOpposite(), facing.getCounterClockWise(), facing.getClockWise(),
                Direction.UP, Direction.DOWN };
        for (int k = 0; k < ways.length; k++) {
            if (!ways[k]) {
                HELD[k] = 0;
                continue;
            }
            int held = HELD[k]++;
            if (held == 0 || held >= REPEAT_AFTER && (held - REPEAT_AFTER) % REPEAT_EVERY == 0) {
                Hologram.nudge(towards[k].getStepX(), towards[k].getStepY(), towards[k].getStepZ());
            }
        }
    }

    @SubscribeEvent
    static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null || !ready(player) || !Hologram.shown()
                || event.getScrollDeltaY() == 0.0) {
            return;
        }
        Hologram.scroll(player, event.getScrollDeltaY() > 0.0 ? 1 : -1);
        event.setCanceled(true);
    }

    // A left click places the hologram; the wand never breaks or hits anything.
    @SubscribeEvent
    static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !event.isAttack() || !SchematicClient.holding(player)) {
            return;
        }
        event.setCanceled(true);
        event.setSwingHand(false);
        if (!SchematicClient.allowed(player)) {
            SchematicClient.say(Component.translatable(SchematicClient.PREFIX + (SchematicClient.trusted(player)
                    ? "error.character" : "error.not_allowed")), true);
            return;
        }
        if (!Hologram.shown()) {
            SchematicClient.say(Component.translatable(SchematicClient.PREFIX + "error.none"), true);
            return;
        }
        Hologram.place();
        player.swing(InteractionHand.MAIN_HAND);
    }

    // Where the hologram stands is worked out each frame, so it follows the look smoothly.
    @SubscribeEvent
    static void onFrame(RenderFrameEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && Hologram.shown()) {
            if (!SchematicClient.holding(player) && !Hologram.pinned) {
                return;
            }
            Hologram.update(player, event.getPartialTick().getGameTimeDeltaPartialTick(true));
        }
    }

    @SubscribeEvent
    static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        SchematicClient.clear();
    }

    // Right click: sneaking opens the menu, else pins the hologram where it is or lets it follow again.
    public static void use(Player user, boolean sneaking) {
        if (!(user instanceof LocalPlayer player)) {
            return;
        }
        if (!SchematicClient.allowed(player)) {
            SchematicClient.say(Component.translatable(SchematicClient.PREFIX + (SchematicClient.trusted(player)
                    ? "error.character" : "error.not_allowed")), true);
            return;
        }
        if (sneaking || !Hologram.shown()) {
            SchematicScreen.open();
            return;
        }
        Hologram.pin();
        click();
    }
}
