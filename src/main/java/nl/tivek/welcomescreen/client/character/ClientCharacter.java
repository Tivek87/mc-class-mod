package nl.tivek.welcomescreen.client.character;

import java.util.Arrays;
import javax.annotation.Nullable;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.AbilitySlot;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.client.character.docock.ClimbControl;
import nl.tivek.welcomescreen.client.character.docock.TentacleLegs;
import nl.tivek.welcomescreen.client.character.lantern.ClientRing;
import nl.tivek.welcomescreen.client.character.lantern.ConstructChoice;
import nl.tivek.welcomescreen.client.character.lantern.ConstructHud;
import nl.tivek.welcomescreen.client.character.lantern.ConstructWheel;
import nl.tivek.welcomescreen.client.character.lantern.ConstructWheelScreen;
import nl.tivek.welcomescreen.client.stamina.StaminaClient;
import nl.tivek.welcomescreen.network.AbilityActionPayload;
import nl.tivek.welcomescreen.network.CharacterStatePayload;

/**
 * The client side of being a character:
 * <ul>
 * <li>it sends the ability keys to the server (the same keys for everyone, see AbilitySlot) and keeps
 * the keys you hold down going;</li>
 * <li>while tentacles carry you, you stand in the air on them: they step over everything up to a
 * block high and they catch your falls;</li>
 * <li>Wall Climb (see ClimbControl);</li>
 * <li>Dash, Block and climbing spend stamina;</li>
 * <li>it shows who you are and your abilities with their keys and cooldowns, bottom right.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClientCharacter {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(WelcomeScreenMod.MODID,
            "character_abilities");
    // Settings an ability may have that this side needs: stamina is spent on the client, and an ability that
    // costs ring power is not even started on a ring that is too low (the ability that recharges it is named).
    private static final String STAMINA_COST = "staminaCost";
    private static final String STAMINA_PER_TICK = "staminaPerTick";
    private static final String POWER_COST = "powerCost";
    private static final String RECHARGE = "recharge";

    private static final int WHITE = 0xFFFFFFFF;
    private static final int GRAY = 0xFF9AA2AC;
    private static final int GREEN = 0xFF7FD46B;
    private static final int RED = 0xFFFF5A3A;
    private static final int PANEL = 0x90101418;

    @Nullable
    private static GameCharacter character;
    private static int ultimate;
    private static int legs;
    // How many creatures are marked for Doctor Octopus's Ground Strike.
    private static int marked;
    private static final int[] COOLDOWNS = new int[AbilitySlot.values().length];
    // Which keys you hold down right now, so a start and a stop are sent exactly once.
    private static final boolean[] HELD = new boolean[AbilitySlot.values().length];
    private static boolean climbing;

    private ClientCharacter() {
    }

    /** The server tells this client who they are and how their abilities stand. */
    public static void set(CharacterStatePayload payload) {
        GameCharacter[] all = GameCharacter.values();
        GameCharacter before = character;
        character = payload.character() >= 0 && payload.character() < all.length ? all[payload.character()] : null;
        ultimate = payload.ultimate();
        legs = payload.legs();
        marked = payload.marked();
        for (int i = 0; i < COOLDOWNS.length; i++) {
            COOLDOWNS[i] = i < payload.cooldowns().length ? payload.cooldowns()[i] : 0;
        }
        // Turning into someone else starts with empty hands: keys you were holding down are forgotten,
        // so the new character's own ability starts fresh when you keep the key down.
        if (character != before) {
            Arrays.fill(HELD, false);
            climbing = false;
            ClimbControl.stop();
            ConstructWheel.stop();
            ConstructChoice.forget();
            MouseHold.reset();
        }
    }

    /** Who this client is right now, or null when they are just themselves. */
    @Nullable
    public static GameCharacter active() {
        return character;
    }

    /** True while tentacles (or whatever the character walks on) carry the player. */
    public static boolean carried() {
        return legs > 0;
    }

    private static void send(int action, boolean on, int data) {
        PacketDistributor.sendToServer(new AbilityActionPayload(action, on, data));
    }

    private static void tell(LocalPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("octopus." + WelcomeScreenMod.MODID + "." + key, args),
                true);
    }

    // ---- Keys ----

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        if (!minecraft.isPaused()) {
            for (int i = 0; i < COOLDOWNS.length; i++) {
                COOLDOWNS[i] = Math.max(0, COOLDOWNS[i] - 1);
            }
            ultimate = Math.max(0, ultimate - 1);
        }
        GameCharacter now = character;
        for (AbilitySlot slot : AbilitySlot.values()) {
            CharacterAbility ability = now == null ? null : now.ability(slot);
            // What the character always does on a mouse button, instead of on a key of its own.
            if (ability != null && ability.mouseButton() != CharacterAbility.Mouse.NONE) {
                mouse(player, minecraft, ability, now);
                continue;
            }
            KeyMapping key = AbilityKeys.of(slot);
            // An ability this side does by itself: the construct wheel, which is still only a menu.
            if (ability != null && ability.isClientOnly()) {
                ConstructWheel.tick(minecraft, key);
                while (key.consumeClick()) {
                    // Tapping and holding are told apart by ConstructWheel, not by the presses.
                }
                continue;
            }
            // Whether a key is held down or pressed once is the ability's own business, not the key's.
            if (ability != null && ability.isHeld()) {
                hold(player, ability, key, minecraft.screen == null);
                while (key.consumeClick()) {
                    // A key you hold down does nothing extra with its presses.
                }
            } else {
                while (key.consumeClick()) {
                    press(player, slot);
                }
            }
        }
    }

    /**
     * What the character always does on a mouse button, click or hold (see {@link MouseHold}): a tap does the
     * quick version, holding the button long enough the hold version. It only takes the button over while both
     * your hands are empty and the hand behind that button is not busy with an ability of its own; any other
     * time the mouse keeps doing what it normally does, so you can still mine, build and eat.
     */
    private static void mouse(LocalPlayer player, Minecraft minecraft, CharacterAbility ability,
            GameCharacter now) {
        KeyMapping key = AbilityKeys.of(ability);
        boolean ours = takesMouse(player) && !handBusy(player, now, ability.mouseButton());
        boolean free = ours && minecraft.screen == null && !StaminaClient.isExhausted();
        int index = ability.slot().ordinal();
        switch (MouseHold.tick(ability, free && key.isDown(), !free)) {
            case TAP -> tap(player, ability);
            case HOLD -> send(index, true, data(player) | Characters.HOLD);
            case RELEASE -> send(index, false, data(player));
            case NOTHING -> {
                // Still down and not held long enough yet, or up and nothing to tell.
            }
        }
        HELD[index] = MouseHold.holding(ability.mouseButton());
        if (ours) {
            while (key.consumeClick()) {
                // The ring answers the button itself; the game must not swing or use anything as well.
            }
        }
    }

    /** The quick version of a mouse ability, as long as it is ready and the ring can pay for it. */
    private static void tap(LocalPlayer player, CharacterAbility ability) {
        int index = ability.slot().ordinal();
        if (COOLDOWNS[index] > 0) {
            tell(player, "not_ready", ability.getDisplayName(), (COOLDOWNS[index] + 19) / 20);
            return;
        }
        if (!canPay(player, ability)) {
            noPower(player, ability.character());
            return;
        }
        send(index, true, data(player) | Characters.TAP);
    }

    /** True while the mouse belongs to the character: no item in either hand. */
    static boolean takesMouse(LocalPlayer player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    /**
     * True while the hand behind this button is busy, so the button has to wait: recharging takes both hands, and
     * an ability you hold a key down for (the Giant Fist) takes the hand that attacks. The hand that defends stays
     * free for it, so a shield can go up while a fist charges.
     */
    private static boolean handBusy(LocalPlayer player, GameCharacter now, CharacterAbility.Mouse button) {
        if (ClientRing.recharge(player, 1.0F) >= 0.0F) {
            return true;
        }
        if (button == CharacterAbility.Mouse.RIGHT) {
            return false;
        }
        for (AbilitySlot slot : AbilitySlot.values()) {
            CharacterAbility other = now.ability(slot);
            if (other != null && other.mouseButton() == CharacterAbility.Mouse.NONE && other.isHeld()
                    && !other.isClientOnly() && HELD[slot.ordinal()]) {
                return true;
            }
        }
        return false;
    }

    /** A key you hold down: it starts when you press it and stops the moment you let go. */
    private static void hold(LocalPlayer player, CharacterAbility ability, KeyMapping key, boolean inGame) {
        int index = ability.slot().ordinal();
        boolean want = key.isDown() && inGame && !StaminaClient.isExhausted();
        // Still on cooldown: it starts by itself once it is ready, as long as you keep the key down.
        int left = COOLDOWNS[index];
        if (want && !HELD[index] && left > 0) {
            tell(player, "not_ready", ability.getDisplayName(), (left + 19) / 20);
            want = false;
        }
        // An ability that costs ring power does not start on a ring that cannot pay for it.
        if (want && !HELD[index] && !canPay(player, ability)) {
            noPower(player, ability.character());
            want = false;
        }
        // An ability that says it costs stamina per tick pays for itself while you hold it.
        if (want && ability.has(STAMINA_PER_TICK)) {
            StaminaClient.use((float) ability.value(STAMINA_PER_TICK));
        }
        if (want != HELD[index]) {
            HELD[index] = want;
            send(index, want, data(player));
        }
    }

    private static void press(LocalPlayer player, AbilitySlot slot) {
        if (character == null) {
            player.displayClientMessage(
                    Component.translatable("character." + WelcomeScreenMod.MODID + ".none"), true);
            return;
        }
        CharacterAbility ability = character.ability(slot);
        if (ability == null) {
            player.displayClientMessage(Component.translatable("character." + WelcomeScreenMod.MODID + ".empty",
                    character.getDisplayName(), slot.getDisplayName()), true);
            return;
        }
        // Only an ability that says crouching is its undo may be used while it is on cooldown.
        boolean undo = player.isShiftKeyDown() && ability.crouchDoes() == CharacterAbility.Crouch.UNDO;
        int left = COOLDOWNS[slot.ordinal()];
        if (left > 0 && !undo) {
            tell(player, "not_ready", ability.getDisplayName(), (left + 19) / 20);
            return;
        }
        if (!undo && ability.has(STAMINA_COST)
                && !StaminaClient.tryUse((float) ability.value(STAMINA_COST))) {
            tell(player, "too_tired");
            return;
        }
        send(slot.ordinal(), true, data(player));
    }

    /** What the server needs to know about the press itself: crouching can change what a key does. */
    private static int data(LocalPlayer player) {
        return player.isShiftKeyDown() ? Characters.SNEAKING : 0;
    }

    /** True unless this ability costs ring power (see its "powerCost" setting) and the ring has too little. */
    private static boolean canPay(LocalPlayer player, CharacterAbility ability) {
        return !ability.has(POWER_COST) || ClientRing.power(player) + 1.0E-4F >= ability.value(POWER_COST);
    }

    /** The ring is too low: says so, and which key recharges it. */
    private static void noPower(LocalPlayer player, GameCharacter character) {
        CharacterAbility recharge = character.byName(RECHARGE);
        player.displayClientMessage(recharge == null
                ? Component.translatable("ring." + WelcomeScreenMod.MODID + ".no_power")
                : Component.translatable("ring." + WelcomeScreenMod.MODID + ".no_power_key",
                        AbilityKeys.of(recharge.slot()).getTranslatedKeyMessage()), true);
    }

    // ---- Moving ----

    /**
     * Your own movement while you are carried: climbing a surface, or standing in the air on the
     * tentacles. Decided here, before the player moves this tick.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(event.getEntity() instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        boolean active = character == GameCharacter.DOC_OCK;
        boolean onSurface = ClimbControl.tick(player, active);
        if (onSurface != climbing) {
            climbing = onSurface;
            send(AbilityActionPayload.CLIMB, onSurface, ClimbControl.face().ordinal());
        }
        if (onSurface) {
            return;
        }
        // Only the character that walks on tentacles is ever carried; anything left over is dropped.
        if (active) {
            TentacleLegs.carry(player, legs);
        } else {
            legs = 0;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        character = null;
        ultimate = 0;
        legs = 0;
        marked = 0;
        climbing = false;
        ClimbControl.stop();
        ConstructWheel.stop();
        ConstructChoice.forget();
        MouseHold.reset();
        Arrays.fill(COOLDOWNS, 0);
        Arrays.fill(HELD, false);
    }

    // ---- HUD ----

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ClientCharacter::render);
    }

    /** Who you are and what your keys do, in a small panel in the bottom right corner. */
    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        GameCharacter now = character;
        // The construct wheel and the power screen are picked from on their own: nothing may sit under them.
        if (now == null || minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen instanceof ConstructWheelScreen || minecraft.screen instanceof PowerWheelScreen) {
            return;
        }
        Font font = minecraft.font;
        String prefix = "screen." + WelcomeScreenMod.MODID + ".character.";
        int line = font.lineHeight + 2;
        int rows = Math.max(1, now.abilities().size());
        int width = 168;
        int height = line * (rows + 2) + 4;
        int right = graphics.guiWidth() - 4;
        int left = right - width;
        int top = graphics.guiHeight() - 4 - height;
        graphics.fill(left - 3, top - 3, right + 3, top + height, PANEL);

        Component title = ultimate > 0
                ? Component.translatable(prefix + "ultimate", now.getDisplayName(), (ultimate + 19) / 20)
                : now.getDisplayName();
        graphics.drawString(font, title, left, top, ultimate > 0 ? RED : 0xFF000000 | now.getColor());
        int y = top + line;
        if (now.abilities().isEmpty()) {
            graphics.drawString(font, Component.translatable(prefix + "no_abilities"), left, y, GRAY);
            y += line;
        }
        for (CharacterAbility ability : now.abilities()) {
            AbilitySlot slot = ability.slot();
            Component key = AbilityKeys.of(ability).getTranslatedKeyMessage();
            int cooldown = COOLDOWNS[slot.ordinal()];
            graphics.drawString(font, Component.literal("[").append(key).append("] ")
                    .append(ability.getDisplayName()), left, y, cooldown > 0 ? GRAY : WHITE);
            Component status;
            int color;
            // A character can say more about an ability that is running than "holding" (the shield is on, you fly).
            Component running = now == GameCharacter.GREEN_LANTERN ? ConstructHud.status(ability, minecraft.player)
                    : null;
            if (running != null) {
                status = running;
                color = GREEN;
            } else if (ability.isHeld() && HELD[slot.ordinal()]) {
                status = Component.translatable(prefix + "holding");
                color = GREEN;
            } else if (cooldown > 0) {
                status = Component.literal((cooldown + 19) / 20 + "s");
                color = GRAY;
            } else if (!canPay(minecraft.player, ability)) {
                status = Component.translatable(prefix + "no_power");
                color = RED;
            } else {
                status = Component.translatable(prefix + "ready");
                color = GREEN;
            }
            graphics.drawString(font, status, right - font.width(status), y, color);
            y += line;
        }
        // The line at the bottom: Green Lantern's ring power; how you are carried and what you have marked;
        // else what this character can always do.
        String passive = "character." + WelcomeScreenMod.MODID + "." + now.getId() + ".passive";
        CharacterAbility fist = now.byName("giant_fist");
        if (now == GameCharacter.GREEN_LANTERN) {
            ConstructHud.renderPower(graphics, font, minecraft.player, left, right, y + 2,
                    fist == null || !fist.has(POWER_COST) ? 0.0F : (float) fist.value(POWER_COST));
        } else if (legs > 0 || marked > 0) {
            MutableComponent status = Component.translatable(prefix + (legs > 0 ? "on_legs" : "on_feet"), legs);
            if (marked > 0) {
                status.append(" + ").append(Component.translatable(prefix + "marked", marked));
            }
            graphics.drawString(font, status, left, y + 2, GREEN);
        } else if (Language.getInstance().has(passive)) {
            graphics.drawString(font, Component.translatable(passive), left, y + 2, GRAY);
        }
    }
}
