package nl.tivek.multiversepowers.character.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
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
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilityActionPayload;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterLookPayload;
import nl.tivek.multiversepowers.character.CharacterStatePayload;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.docock.client.ClimbControl;
import nl.tivek.multiversepowers.character.docock.client.TentacleLegs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructHud;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheel;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheelScreen;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientCharacter {
    private static final ResourceLocation LAYER_ID = ResourceLocation.fromNamespaceAndPath(MultiversePowers.MODID,
            "character_abilities");
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
    private static int marked;
    private static final int[] COOLDOWNS = new int[AbilitySlot.values().length];
    private static final boolean[] HELD = new boolean[AbilitySlot.values().length];
    private static int clock;
    private static final int[] TAPPED = never(AbilitySlot.values().length);
    private static final int[] KEY_DOWN = up(AbilitySlot.values().length);
    private static boolean climbing;
    private static final Map<GameCharacter, Local> LOCAL = new EnumMap<>(GameCharacter.class);
    private static final Int2ObjectOpenHashMap<GameCharacter> WORN = new Int2ObjectOpenHashMap<>();

    private ClientCharacter() {
    }

    private static int[] never(int size) {
        int[] ticks = new int[size];
        Arrays.fill(ticks, Integer.MIN_VALUE);
        return ticks;
    }

    private static int[] up(int size) {
        int[] ticks = new int[size];
        Arrays.fill(ticks, -1);
        return ticks;
    }

    public static void set(CharacterStatePayload payload) {
        GameCharacter[] all = GameCharacter.values();
        GameCharacter before = character;
        character = payload.character() >= 0 && payload.character() < all.length ? all[payload.character()] : null;
        ultimate = payload.ultimate();
        legs = payload.stance();
        marked = payload.marks();
        for (int i = 0; i < COOLDOWNS.length; i++) {
            COOLDOWNS[i] = i < payload.cooldowns().length ? payload.cooldowns()[i] : 0;
        }
        if (character != before) {
            Arrays.fill(HELD, false);
            Arrays.fill(TAPPED, Integer.MIN_VALUE);
            Arrays.fill(KEY_DOWN, -1);
            climbing = false;
            ClimbControl.stop();
            ConstructWheel.stop();
            ConstructChoice.forget();
            Gestures.reset();
        }
    }

    @Nullable
    public static GameCharacter active() {
        return character;
    }

    // Which character a player in sight is, as the server says.
    public static void seen(CharacterLookPayload payload) {
        GameCharacter[] all = GameCharacter.values();
        if (payload.character() >= 0 && payload.character() < all.length) {
            WORN.put(payload.entity(), all[payload.character()]);
        } else {
            WORN.remove(payload.entity());
        }
    }

    @Nullable
    public static GameCharacter of(Entity entity) {
        return WORN.get(entity.getId());
    }

    public static float sinceTap(@Nullable CharacterAbility ability, float partialTick) {
        if (ability == null || TAPPED[ability.slot().ordinal()] == Integer.MIN_VALUE) {
            return -1.0F;
        }
        return clock - TAPPED[ability.slot().ordinal()] + partialTick;
    }

    public static boolean carried() {
        return legs > 0;
    }

    static int clock() {
        return clock;
    }

    // A gesture that may only fire at some moments (take off only when standing free) says when, here.
    public static void gate(CharacterAbility ability, Predicate<LocalPlayer> may) {
        Gestures.gate(ability, may);
    }

    // A character whose gestures change in flight says here when it flies.
    public static void flying(GameCharacter character, Predicate<LocalPlayer> flying) {
        Gestures.flying(character, flying);
    }

    static void held(AbilitySlot slot, boolean held) {
        HELD[slot.ordinal()] = held;
    }

    static boolean isHeld(AbilitySlot slot) {
        return HELD[slot.ordinal()];
    }

    // Off cooldown and paid for; else the player is told why not.
    static boolean ready(LocalPlayer player, CharacterAbility ability) {
        int left = COOLDOWNS[ability.slot().ordinal()];
        if (left > 0) {
            tell(player, "not_ready", ability.getDisplayName(), (left + 19) / 20);
            return false;
        }
        if (!canPay(player, ability)) {
            noPower(player, ability.character());
            return false;
        }
        return true;
    }

    // A character that moves its player itself (a dash) acts at once in its own game, and may add to what is sent.
    @FunctionalInterface
    public interface Local {
        // The data to send on, or -1 to send nothing.
        int act(LocalPlayer player, CharacterAbility ability, boolean on, int data);
    }

    public static void local(GameCharacter character, Local local) {
        LOCAL.put(character, local);
    }

    // A move of the character's own that goes on after its button (a dive's slam), told to the server.
    public static void sendAction(CharacterAbility ability, boolean on, int data) {
        send(ability.slot().ordinal(), on, data);
    }

    static void send(int action, boolean on, int data) {
        LocalPlayer player = Minecraft.getInstance().player;
        AbilitySlot slot = AbilitySlot.byIndex(action);
        CharacterAbility ability = character == null || slot == null ? null : character.ability(slot);
        Local local = character == null ? null : LOCAL.get(character);
        if (local != null && ability != null && player != null) {
            data = local.act(player, ability, on, data);
            if (data < 0) {
                return;
            }
        }
        PacketDistributor.sendToServer(new AbilityActionPayload(action, on, data));
    }

    private static void tell(LocalPlayer player, String key, Object... args) {
        player.displayClientMessage(Component.translatable("octopus." + MultiversePowers.MODID + "." + key, args),
                true);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        if (!minecraft.isPaused()) {
            clock++;
            for (int i = 0; i < COOLDOWNS.length; i++) {
                COOLDOWNS[i] = Math.max(0, COOLDOWNS[i] - 1);
            }
            ultimate = Math.max(0, ultimate - 1);
        }
        GameCharacter now = character;
        Gestures.tick(player, minecraft, now);
        for (AbilitySlot slot : AbilitySlot.values()) {
            CharacterAbility ability = now == null ? null : now.ability(slot);
            KeyMapping key = AbilityKeys.of(slot);
            if (ability != null && ability.onGesture()) {
                while (key.consumeClick()) {
                }
                continue;
            }
            if (ability != null && ability.isClientOnly()) {
                ConstructWheel.tick(minecraft, key);
                while (key.consumeClick()) {
                }
                continue;
            }
            if (ability != null && ability.isHeld()) {
                hold(player, ability, key, minecraft.screen == null);
                while (key.consumeClick()) {
                }
            } else if (ability != null && ability.holdTicks() > 0) {
                boolean clicked = false;
                while (key.consumeClick()) {
                    clicked = true;
                }
                tapOrHold(player, ability, key.isDown(), clicked, minecraft.screen == null);
            } else {
                while (key.consumeClick()) {
                    press(player, slot);
                }
            }
        }
    }

    static void tap(LocalPlayer player, CharacterAbility ability) {
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
        TAPPED[index] = clock;
    }

    private static void hold(LocalPlayer player, CharacterAbility ability, KeyMapping key, boolean inGame) {
        int index = ability.slot().ordinal();
        boolean want = key.isDown() && inGame && !StaminaClient.isExhausted();
        int left = COOLDOWNS[index];
        if (want && !HELD[index] && left > 0) {
            tell(player, "not_ready", ability.getDisplayName(), (left + 19) / 20);
            want = false;
        }
        if (want && !HELD[index] && !canPay(player, ability)) {
            noPower(player, ability.character());
            want = false;
        }
        if (want && ability.has(STAMINA_PER_TICK)) {
            StaminaClient.use((float) ability.value(STAMINA_PER_TICK));
        }
        if (want != HELD[index]) {
            HELD[index] = want;
            send(index, want, data(player));
        }
    }

    public static int cooldownLeft(AbilitySlot slot) {
        return COOLDOWNS[slot.ordinal()];
    }

    public static float keyHoldProgress(CharacterAbility ability, float partialTick) {
        int down = KEY_DOWN[ability.slot().ordinal()];
        if (down < 0 || ability.holdTicks() <= 0) {
            return -1.0F;
        }
        return down >= ability.holdTicks() ? 1.0F : Math.min(1.0F, (down + partialTick) / ability.holdTicks());
    }

    // A key with a hold version: letting go before its hold time is a tap, holding on for it the other move.
    private static void tapOrHold(LocalPlayer player, CharacterAbility ability, boolean down, boolean clicked,
            boolean inGame) {
        int index = ability.slot().ordinal();
        if (!inGame) {
            KEY_DOWN[index] = -1;
            return;
        }
        if (down) {
            if (KEY_DOWN[index] < 0) {
                KEY_DOWN[index] = 0;
            } else if (KEY_DOWN[index] < ability.holdTicks() && ++KEY_DOWN[index] >= ability.holdTicks()) {
                send(index, true, data(player) | Characters.HOLD);
            }
            return;
        }
        int held = KEY_DOWN[index];
        KEY_DOWN[index] = -1;
        if (held >= 0 ? held < ability.holdTicks() : clicked) {
            press(player, ability.slot());
        }
    }

    static void press(LocalPlayer player, AbilitySlot slot) {
        boolean quiet = AbilityKeys.sharesGameKey(AbilityKeys.of(slot));
        if (character == null) {
            if (!quiet) {
                player.displayClientMessage(
                        Component.translatable("character." + MultiversePowers.MODID + ".none"), true);
            }
            return;
        }
        CharacterAbility ability = character.ability(slot);
        if (ability == null || ability.isPlaceholder()) {
            if (!quiet) {
                player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID
                        + ".empty", character.getDisplayName(), slot.getDisplayName()), true);
            }
            return;
        }
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

    static int data(LocalPlayer player) {
        return player.isShiftKeyDown() ? Characters.SNEAKING : 0;
    }

    private static boolean canPay(LocalPlayer player, CharacterAbility ability) {
        return !ability.has(POWER_COST) || ClientRing.power(player) + 1.0E-4F >= ability.value(POWER_COST);
    }

    private static void noPower(LocalPlayer player, GameCharacter character) {
        CharacterAbility recharge = character.byName(RECHARGE);
        player.displayClientMessage(recharge == null
                ? Component.translatable("ring." + MultiversePowers.MODID + ".no_power")
                : Component.translatable("ring." + MultiversePowers.MODID + ".no_power_key",
                        AbilityKeys.of(recharge.slot()).getTranslatedKeyMessage()), true);
    }

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
        if (active) {
            TentacleLegs.carry(player, legs);
        } else {
            legs = 0;
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        character = null;
        WORN.clear();
        ultimate = 0;
        legs = 0;
        marked = 0;
        climbing = false;
        ClimbControl.stop();
        ConstructWheel.stop();
        ConstructChoice.forget();
        Gestures.reset();
        Arrays.fill(COOLDOWNS, 0);
        Arrays.fill(HELD, false);
        Arrays.fill(TAPPED, Integer.MIN_VALUE);
    }

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ClientCharacter::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        GameCharacter now = character;
        if (now == null || minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen instanceof ConstructWheelScreen || minecraft.screen instanceof PowerWheelScreen
                || !ClientSettings.on(ClientSettings.ABILITY_PANEL)) {
            return;
        }
        // Scaled round the bottom right corner it stands in.
        float scale = ClientSettings.factor(ClientSettings.PANEL_SCALE);
        graphics.pose().pushPose();
        graphics.pose().translate(graphics.guiWidth(), graphics.guiHeight(), 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.pose().translate(-graphics.guiWidth(), -graphics.guiHeight(), 0.0F);
        panel(graphics, minecraft, now);
        graphics.pose().popPose();
    }

    private static void panel(GuiGraphics graphics, Minecraft minecraft, GameCharacter now) {
        Font font = minecraft.font;
        String prefix = "screen." + MultiversePowers.MODID + ".character.";
        int line = font.lineHeight + 2;
        int rows = Math.max(1, (int) now.abilities().stream().filter(ability -> !ability.isPlaceholder()).count());
        int width = 168;
        int height = line * (rows + 2) + 4;
        int right = graphics.guiWidth() - 4;
        int left = right - width;
        int top = graphics.guiHeight() - 4 - height;
        graphics.fill(left - 3, top - 3, right + 3, top + height, PANEL);

        Component title = ultimate > 0
                ? Component.translatable(prefix + "ultimate." + now.getId(), now.getDisplayName(),
                        (ultimate + 19) / 20)
                : now.getDisplayName();
        graphics.drawString(font, title, left, top, ultimate > 0 ? RED : 0xFF000000 | now.getColor());
        int y = top + line;
        if (now.abilities().isEmpty()) {
            graphics.drawString(font, Component.translatable(prefix + "no_abilities"), left, y, GRAY);
            y += line;
        }
        for (CharacterAbility ability : now.abilities()) {
            if (ability.isPlaceholder()) {
                continue;
            }
            AbilitySlot slot = ability.slot();
            Component key = PowerInputs.label(ability);
            int cooldown = COOLDOWNS[slot.ordinal()];
            boolean usable = Gestures.active(ability, minecraft.player);
            graphics.drawString(font, Component.literal("[").append(key).append("] ")
                    .append(ability.getDisplayName()), left, y, cooldown > 0 || !usable ? GRAY : WHITE);
            Component status;
            int color;
            Component running = now == GameCharacter.GREEN_LANTERN ? ConstructHud.status(ability, minecraft.player)
                    : null;
            if (!usable) {
                status = Component.translatable(prefix + (ability.when() == CharacterAbility.When.FLYING
                        ? "in_flight" : "on_ground"));
                color = GRAY;
            } else if (running != null) {
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
        String passive = "character." + MultiversePowers.MODID + "." + now.getId() + ".passive";
        CharacterAbility train = now.byName("emerald_express");
        if (now == GameCharacter.GREEN_LANTERN && ClientSettings.on(ClientSettings.POWER_BAR)) {
            ConstructHud.renderPower(graphics, font, minecraft.player, left, right, y + 2,
                    train == null || !train.has(POWER_COST) ? 0.0F : (float) train.value(POWER_COST));
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
