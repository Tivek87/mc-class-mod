package nl.tivek.multiversepowers.character.client;

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
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilityActionPayload;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.CharacterStatePayload;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.docock.client.ClimbControl;
import nl.tivek.multiversepowers.character.docock.client.TentacleLegs;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.character.greenlantern.client.body.CallArm;
import nl.tivek.multiversepowers.character.greenlantern.client.body.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.WhipArms;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructHud;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheel;
import nl.tivek.multiversepowers.character.greenlantern.client.hud.ConstructWheelScreen;
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
    private static final int BLOCK_AFTER = 5;
    private static int defendDown = -1;
    private static boolean endedCharge;

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
            MouseHold.reset();
        }
    }

    @Nullable
    public static GameCharacter active() {
        return character;
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

    private static void send(int action, boolean on, int data) {
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
        for (AbilitySlot slot : AbilitySlot.values()) {
            CharacterAbility ability = now == null ? null : now.ability(slot);
            if (ability != null && ability.mouseButton() != CharacterAbility.Mouse.NONE) {
                mouse(player, minecraft, ability, now);
                continue;
            }
            KeyMapping key = AbilityKeys.of(slot);
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

    private static void mouse(LocalPlayer player, Minecraft minecraft, CharacterAbility ability,
            GameCharacter now) {
        KeyMapping key = AbilityKeys.of(ability);
        boolean ours = takesMouse(player) && !handBusy(player, now, ability.mouseButton());
        boolean free = ours && minecraft.screen == null && !StaminaClient.isExhausted();
        int index = ability.slot().ordinal();
        MouseHold.Step step = MouseHold.tick(ability, free && key.isDown(), !free);
        if (SwordArms.holding()) {
            sword(player, ability, index, step, free && key.isDown());
        } else if (FlameArms.holding()) {
            defendDown = -1;
            flame(player, ability, index, step);
        } else if (WhipArms.holding()) {
            defendDown = -1;
            whip(player, ability, index, step);
        } else {
            defendDown = -1;
            switch (step) {
                case TAP -> tap(player, ability);
                case HOLD -> send(index, true, data(player) | Characters.HOLD);
                case RELEASE, LET_GO -> send(index, false, data(player));
                case NOTHING -> {
                }
            }
        }
        HELD[index] = MouseHold.holding(ability.mouseButton());
        if (ours) {
            while (key.consumeClick()) {
            }
        }
    }

    private static void sword(LocalPlayer player, CharacterAbility ability, int index, MouseHold.Step step,
            boolean down) {
        if (ability.mouseButton() == CharacterAbility.Mouse.LEFT) {
            switch (step) {
                case TAP -> {
                    SwordMove move = SwordArms.attack(player);
                    if (move != null) {
                        send(index, true, data(player) | Characters.TAP | move.ordinal() << Characters.MOVE_SHIFT);
                    }
                }
                case HOLD -> {
                    if (SwordArms.flurry(player)) {
                        send(index, true, data(player) | Characters.HOLD);
                    }
                }
                case RELEASE, LET_GO -> {
                    SwordArms.stopFlurry();
                    send(index, false, data(player));
                }
                case NOTHING -> {
                }
            }
            return;
        }
        if (down) {
            if (defendDown < 0) {
                defendDown = 0;
                endedCharge = SwordArms.charging();
                if (endedCharge) {
                    SwordArms.endCharge();
                }
            } else {
                defendDown++;
            }
            if (defendDown == BLOCK_AFTER && SwordArms.block(true)) {
                send(index, true, data(player) | Characters.HOLD);
            }
            return;
        }
        if (defendDown < 0) {
            return;
        }
        int held = defendDown;
        defendDown = -1;
        if (held >= BLOCK_AFTER) {
            if (SwordArms.block(false)) {
                send(index, false, data(player));
            }
        } else if (!endedCharge && SwordArms.charge(player)) {
            send(index, true, data(player) | Characters.TAP);
        }
    }

    private static void flame(LocalPlayer player, CharacterAbility ability, int index, MouseHold.Step step) {
        boolean attack = ability.mouseButton() == CharacterAbility.Mouse.LEFT;
        switch (step) {
            case TAP -> {
                if (attack) {
                    FlameMove sweep = FlameArms.sweep(player);
                    if (sweep != null) {
                        send(index, true, data(player) | Characters.TAP | sweep.ordinal() << Characters.MOVE_SHIFT);
                    }
                } else if (FlameArms.wall(player)) {
                    send(index, true, data(player) | Characters.TAP);
                }
            }
            case HOLD -> {
                if (attack ? FlameArms.pour(player) : FlameArms.swirl(player)) {
                    send(index, true, data(player) | Characters.HOLD);
                }
            }
            case RELEASE, LET_GO -> {
                if (attack) {
                    FlameArms.stopPouring();
                } else {
                    FlameArms.stopSwirling();
                }
                send(index, false, data(player));
            }
            case NOTHING -> {
            }
        }
    }

    private static void whip(LocalPlayer player, CharacterAbility ability, int index, MouseHold.Step step) {
        boolean attack = ability.mouseButton() == CharacterAbility.Mouse.LEFT;
        switch (step) {
            case TAP -> {
                if (attack) {
                    WhipMove lash = WhipArms.lash(player);
                    if (lash != null) {
                        send(index, true, data(player) | Characters.TAP | lash.ordinal() << Characters.MOVE_SHIFT);
                    }
                } else if (WhipArms.lasso(player)) {
                    send(index, true, data(player) | Characters.TAP);
                }
            }
            case HOLD -> {
                if (attack ? WhipArms.whirl(player) : WhipArms.spin(player)) {
                    send(index, true, data(player) | Characters.HOLD);
                }
            }
            case RELEASE, LET_GO -> {
                if (attack) {
                    WhipArms.stopWhirl();
                } else {
                    WhipArms.stopSpin();
                }
                send(index, false, data(player));
            }
            case NOTHING -> {
            }
        }
    }

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
        TAPPED[index] = clock;
    }

    static boolean takesMouse(LocalPlayer player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    private static boolean handBusy(LocalPlayer player, GameCharacter now, CharacterAbility.Mouse button) {
        if (ClientRing.recharge(player, 1.0F) >= 0.0F) {
            return true;
        }
        // The defend hand always stays free, so a shield can go up while a fist charges.
        if (button == CharacterAbility.Mouse.RIGHT) {
            return false;
        }
        float flight = ClientRing.flight(player, 0.0F);
        if (flight >= 0.0F && flight < Flight.ARISE_TICKS) {
            return true;
        }
        boolean attackHand = ClientConstructs.wave(player.getId(), 1.0F) != null || CallArm.up(player, 1.0F) > 0.0F;
        for (AbilitySlot slot : AbilitySlot.values()) {
            CharacterAbility other = now.ability(slot);
            if (other != null && other.mouseButton() == CharacterAbility.Mouse.NONE && other.isHeld()
                    && !other.isClientOnly() && HELD[slot.ordinal()]) {
                attackHand = true;
            }
        }
        // One free hand is enough: the attack button waits only while the defend hand is busy too.
        return attackHand && (ClientRing.has(player, RingPayload.SHIELD) || ClientRing.has(player, RingPayload.DOME));
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

    private static void press(LocalPlayer player, AbilitySlot slot) {
        if (character == null) {
            player.displayClientMessage(
                    Component.translatable("character." + MultiversePowers.MODID + ".none"), true);
            return;
        }
        CharacterAbility ability = character.ability(slot);
        if (ability == null) {
            player.displayClientMessage(Component.translatable("character." + MultiversePowers.MODID + ".empty",
                    character.getDisplayName(), slot.getDisplayName()), true);
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

    private static int data(LocalPlayer player) {
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
        Arrays.fill(TAPPED, Integer.MIN_VALUE);
    }

    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER_ID, ClientCharacter::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        GameCharacter now = character;
        if (now == null || minecraft.player == null || minecraft.options.hideGui
                || minecraft.screen instanceof ConstructWheelScreen || minecraft.screen instanceof PowerWheelScreen) {
            return;
        }
        Font font = minecraft.font;
        String prefix = "screen." + MultiversePowers.MODID + ".character.";
        int line = font.lineHeight + 2;
        int rows = Math.max(1, now.abilities().size());
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
            AbilitySlot slot = ability.slot();
            Component key = AbilityKeys.of(ability).getTranslatedKeyMessage();
            int cooldown = COOLDOWNS[slot.ordinal()];
            graphics.drawString(font, Component.literal("[").append(key).append("] ")
                    .append(ability.getDisplayName()), left, y, cooldown > 0 ? GRAY : WHITE);
            Component status;
            int color;
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
        String passive = "character." + MultiversePowers.MODID + "." + now.getId() + ".passive";
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
