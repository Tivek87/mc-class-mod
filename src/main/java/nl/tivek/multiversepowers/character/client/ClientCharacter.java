package nl.tivek.multiversepowers.character.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
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
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheel;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientCharacter {
    private static final String STAMINA_COST = "staminaCost";
    private static final String STAMINA_PER_TICK = "staminaPerTick";
    static final String POWER_COST = "powerCost";
    private static final String RECHARGE = "recharge";
    // A key with a hold version pressed while it is shut, until it is let go.
    private static final int SHUT = -2;

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
    private static final Map<GameCharacter, Refusal> REFUSALS = new EnumMap<>(GameCharacter.class);
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

    public static int ultimate() {
        return ultimate;
    }

    public static int legs() {
        return legs;
    }

    public static int marked() {
        return marked;
    }

    // A gesture that may only fire at some moments (take off only when standing free) says when, here.
    public static void gate(CharacterAbility ability, Predicate<LocalPlayer> may) {
        Gestures.gate(ability, may);
    }

    // A character whose gestures change in flight says here when it flies.
    public static void flying(GameCharacter character, Predicate<LocalPlayer> flying) {
        Gestures.flying(character, flying);
    }

    public static boolean flies(GameCharacter character, LocalPlayer player) {
        return Gestures.flies(character, player);
    }

    // Whether a gesture's ability is the one its button fires now (on the ground or in flight, in the right state).
    public static boolean inPlay(CharacterAbility ability, LocalPlayer player) {
        return Gestures.active(ability, player);
    }

    // A character with gestures that need a state of its own (CharacterAbility.needs) reports that state here.
    public static void state(GameCharacter character, ToIntFunction<LocalPlayer> state) {
        Gestures.state(character, state);
    }

    // A character whose keys are shut at some moments (Green Lantern's while a construct weapon is in his hands) says
    // here why one does not work now, or null when it does. The panel leaves a shut one off.
    @FunctionalInterface
    public interface Refusal {
        @Nullable
        Component why(CharacterAbility ability, LocalPlayer player);
    }

    public static void refusal(GameCharacter character, Refusal refusal) {
        REFUSALS.put(character, refusal);
    }

    @Nullable
    static Component refused(CharacterAbility ability, LocalPlayer player) {
        Refusal refusal = REFUSALS.get(ability.character());
        return refusal == null ? null : refusal.why(ability, player);
    }

    static void held(AbilitySlot slot, boolean held) {
        HELD[slot.ordinal()] = held;
    }

    public static boolean isHeld(AbilitySlot slot) {
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
            if (key == null) {
                continue;
            }
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
        Component why = want && !HELD[index] ? refused(ability, player) : null;
        if (why != null) {
            player.displayClientMessage(why, true);
            want = false;
        }
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
            if (KEY_DOWN[index] == SHUT) {
                return;
            }
            Component why = KEY_DOWN[index] < 0 ? refused(ability, player) : null;
            if (why != null) {
                // Said once, then nothing until it is let go: neither the hold nor the tap.
                player.displayClientMessage(why, true);
                KEY_DOWN[index] = SHUT;
            } else if (KEY_DOWN[index] < 0) {
                KEY_DOWN[index] = 0;
            } else if (KEY_DOWN[index] < ability.holdTicks() && ++KEY_DOWN[index] >= ability.holdTicks()) {
                send(index, true, data(player) | Characters.HOLD);
            }
            return;
        }
        int held = KEY_DOWN[index];
        KEY_DOWN[index] = -1;
        if (held != SHUT && (held >= 0 ? held < ability.holdTicks() : clicked)) {
            press(player, ability.slot());
        }
    }

    static void press(LocalPlayer player, AbilitySlot slot) {
        KeyMapping own = AbilityKeys.of(slot);
        boolean quiet = own == null || AbilityKeys.sharesGameKey(own);
        if (character == null) {
            if (!quiet) {
                player.displayClientMessage(
                        Component.translatable("character." + MultiversePowers.MODID + ".none"), true);
            }
            return;
        }
        CharacterAbility ability = character.ability(slot);
        if (ability == null || ability.isPlaceholder()) {
            if (!quiet && (ability == null || !ability.isSpare())) {
                player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID
                        + ".empty", character.getDisplayName(), slot.getDisplayName()), true);
            }
            return;
        }
        Component why = refused(ability, player);
        if (why != null) {
            player.displayClientMessage(why, true);
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

    static boolean canPay(LocalPlayer player, CharacterAbility ability) {
        return !ability.has(POWER_COST) || ClientRing.power(player) + 1.0E-4F >= ability.value(POWER_COST);
    }

    private static void noPower(LocalPlayer player, GameCharacter character) {
        CharacterAbility recharge = character.byName(RECHARGE);
        player.displayClientMessage(recharge == null
                ? Component.translatable("ring." + MultiversePowers.MODID + ".no_power")
                : Component.translatable("ring." + MultiversePowers.MODID + ".no_power_key",
                        PowerInputs.label(recharge)), true);
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
}
