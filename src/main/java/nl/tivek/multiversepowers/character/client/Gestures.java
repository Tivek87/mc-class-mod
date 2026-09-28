package nl.tivek.multiversepowers.character.client;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import nl.tivek.multiversepowers.character.AbilitySlot;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.RingPayload;
import nl.tivek.multiversepowers.character.greenlantern.ability.flame.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.flight.Flight;
import nl.tivek.multiversepowers.character.greenlantern.ability.sword.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.whip.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.body.arm.CallArm;
import nl.tivek.multiversepowers.character.greenlantern.client.body.flame.FlameArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.sword.SwordArms;
import nl.tivek.multiversepowers.character.greenlantern.client.body.whip.WhipArms;
import nl.tivek.multiversepowers.stamina.client.StaminaClient;

// The mouse and space gestures: per button the ability its click fires and the one its hold fires, which may be two
// different ones (Thor dashes on a click and claps on a hold) and may change once the character flies.
final class Gestures {
    // Two presses of space this close together, in ticks, are a double press (as the game's own creative flight).
    private static final int DOUBLE_WINDOW = 7;
    private static final int BLOCK_AFTER = 5;
    private static final CharacterAbility.Input[] BUTTONS = { CharacterAbility.Input.LEFT,
            CharacterAbility.Input.RIGHT, CharacterAbility.Input.SCROLL, CharacterAbility.Input.SPACE };
    private static final Map<GameCharacter, Predicate<LocalPlayer>> FLYING = new EnumMap<>(GameCharacter.class);
    private static final Map<CharacterAbility, Predicate<LocalPlayer>> GATES = new HashMap<>();

    @Nullable
    private static final CharacterAbility[] STARTED = new CharacterAbility[5];
    private static int defendDown = -1;
    private static boolean endedCharge;
    private static boolean spaceWas;
    private static int lastSpace = Integer.MIN_VALUE / 2;
    private static boolean spaceSpent;

    private Gestures() {
    }

    // A character whose gestures change in flight tells here when it flies.
    static void flying(GameCharacter character, Predicate<LocalPlayer> flying) {
        FLYING.put(character, flying);
    }

    static void gate(CharacterAbility ability, Predicate<LocalPlayer> may) {
        GATES.put(ability, may);
    }

    private static boolean allowed(CharacterAbility ability, LocalPlayer player) {
        Predicate<LocalPlayer> may = GATES.get(ability);
        return may == null || may.test(player);
    }

    static boolean active(CharacterAbility ability, LocalPlayer player) {
        if (ability.when() == CharacterAbility.When.ALWAYS) {
            return true;
        }
        Predicate<LocalPlayer> check = FLYING.get(ability.character());
        boolean flying = check != null && check.test(player);
        return flying == (ability.when() == CharacterAbility.When.FLYING);
    }

    @Nullable
    static CharacterAbility clickOf(@Nullable GameCharacter now, CharacterAbility.Input input, LocalPlayer player) {
        if (now != null) {
            for (CharacterAbility ability : now.abilities()) {
                if (!ability.isPlaceholder() && ability.input() == input
                        && ability.tapWhen() != CharacterAbility.Tap.NEVER && active(ability, player)) {
                    return ability;
                }
            }
        }
        return null;
    }

    @Nullable
    static CharacterAbility holdOf(@Nullable GameCharacter now, CharacterAbility.Input input, LocalPlayer player) {
        if (now != null) {
            for (CharacterAbility ability : now.abilities()) {
                if (!ability.isPlaceholder() && ability.input() == input && ability.holdTicks() > 0
                        && active(ability, player)) {
                    return ability;
                }
            }
        }
        return null;
    }

    static boolean bound(@Nullable GameCharacter now, CharacterAbility.Input input, LocalPlayer player) {
        return clickOf(now, input, player) != null || holdOf(now, input, player) != null;
    }

    static boolean scrollOnOneKey() {
        return PowerInputs.SCROLL_CLICK.getKey().equals(PowerInputs.SCROLL_HOLD.getKey())
                && PowerInputs.SCROLL_CLICK.getKeyModifier() == PowerInputs.SCROLL_HOLD.getKeyModifier();
    }

    static void tick(LocalPlayer player, Minecraft minecraft, @Nullable GameCharacter now) {
        for (CharacterAbility.Input input : BUTTONS) {
            CharacterAbility click = clickOf(now, input, player);
            CharacterAbility hold = holdOf(now, input, player);
            switch (input) {
                case LEFT, RIGHT -> mouse(player, minecraft, now, input, click, hold);
                case SCROLL -> scroll(player, minecraft, click, hold);
                case SPACE -> space(player, minecraft, click, hold);
                case KEY -> {
                }
            }
        }
    }

    static void reset() {
        MouseHold.reset();
        Arrays.fill(STARTED, null);
        defendDown = -1;
        spaceWas = false;
        spaceSpent = false;
        lastSpace = Integer.MIN_VALUE / 2;
    }

    // The click fires on letting go when a hold waits on the same button, else as the ability says.
    private static CharacterAbility.Tap tapMode(@Nullable CharacterAbility click, @Nullable CharacterAbility hold) {
        if (click == null) {
            return CharacterAbility.Tap.NEVER;
        }
        return hold != null && hold != click ? CharacterAbility.Tap.RELEASE : click.tapWhen();
    }

    private static void mouse(LocalPlayer player, Minecraft minecraft, @Nullable GameCharacter now,
            CharacterAbility.Input input, @Nullable CharacterAbility click, @Nullable CharacterAbility hold) {
        int channel = MouseHold.channel(input);
        KeyMapping key = PowerInputs.clickKey(input);
        if (now == null || click == null && hold == null) {
            letGo(player, channel);
            return;
        }
        boolean ours = takesMouse(player) && !handBusy(player, now, input);
        boolean free = ours && minecraft.screen == null && !StaminaClient.isExhausted();
        boolean down = free && key.isDown();
        MouseHold.Step step = MouseHold.tick(channel, hold == null ? 0 : hold.holdTicks(), tapMode(click, hold),
                down, !free);
        CharacterAbility one = click != null ? click : hold;
        if (click == hold || click == null || hold == null) {
            int index = one.slot().ordinal();
            if (SwordArms.holding()) {
                sword(player, one, index, step, down);
            } else if (FlameArms.holding()) {
                defendDown = -1;
                flame(player, one, index, step);
            } else if (WhipArms.holding()) {
                defendDown = -1;
                whip(player, one, index, step);
            } else {
                defendDown = -1;
                plain(player, channel, click, hold, step);
            }
        } else {
            defendDown = -1;
            plain(player, channel, click, hold, step);
        }
        if (hold != null) {
            ClientCharacter.held(hold.slot(), MouseHold.holding(input));
        }
        if (ours) {
            while (key.consumeClick()) {
            }
        }
    }

    private static void plain(LocalPlayer player, int channel, @Nullable CharacterAbility click,
            @Nullable CharacterAbility hold, MouseHold.Step step) {
        switch (step) {
            case TAP -> {
                if (click != null) {
                    ClientCharacter.tap(player, click);
                }
            }
            case HOLD -> {
                if (hold != null) {
                    startHold(player, channel, hold);
                }
            }
            case RELEASE -> letGo(player, channel);
            case LET_GO -> {
                if (click != null) {
                    ClientCharacter.send(click.slot().ordinal(), false, ClientCharacter.data(player));
                }
            }
            case NOTHING -> {
            }
        }
    }

    // A hold of its own (not the hold version of the click) waits for its cooldown and power like a key.
    private static void startHold(LocalPlayer player, int channel, CharacterAbility hold) {
        if (!allowed(hold, player)
                || hold.tapWhen() == CharacterAbility.Tap.NEVER && !ClientCharacter.ready(player, hold)) {
            return;
        }
        STARTED[channel] = hold;
        ClientCharacter.send(hold.slot().ordinal(), true, ClientCharacter.data(player) | Characters.HOLD);
    }

    private static void letGo(LocalPlayer player, int channel) {
        CharacterAbility started = STARTED[channel];
        STARTED[channel] = null;
        if (started != null) {
            ClientCharacter.send(started.slot().ordinal(), false, ClientCharacter.data(player));
            ClientCharacter.held(started.slot(), false);
        }
        MouseHold.reset(channel);
    }

    private static void scroll(LocalPlayer player, Minecraft minecraft, @Nullable CharacterAbility click,
            @Nullable CharacterAbility hold) {
        boolean inGame = minecraft.screen == null;
        if (scrollOnOneKey()) {
            button(player, MouseHold.SCROLL, PowerInputs.SCROLL_CLICK, click, hold, inGame);
            return;
        }
        button(player, MouseHold.SCROLL, PowerInputs.SCROLL_CLICK, click, null, inGame);
        button(player, MouseHold.SCROLL_HOLD, PowerInputs.SCROLL_HOLD, null, hold, inGame);
    }

    // A button besides the mouse's own two: its click goes through the key checks, as a key press.
    private static void button(LocalPlayer player, int channel, KeyMapping key, @Nullable CharacterAbility click,
            @Nullable CharacterAbility hold, boolean inGame) {
        while (key.consumeClick()) {
        }
        if (click == null && hold == null) {
            letGo(player, channel);
            return;
        }
        boolean down = inGame && key.isDown();
        MouseHold.Step step = MouseHold.tick(channel, hold == null ? 0 : hold.holdTicks(), tapMode(click, hold),
                down, !inGame);
        switch (step) {
            case TAP -> {
                if (click != null) {
                    ClientCharacter.press(player, click.slot());
                }
            }
            case HOLD -> {
                if (hold != null) {
                    startHold(player, channel, hold);
                }
            }
            case RELEASE -> letGo(player, channel);
            case LET_GO, NOTHING -> {
            }
        }
    }

    // Space still jumps as always: a second press quickly after the first is the double press, and holding it on
    // after one press is the hold.
    private static void space(LocalPlayer player, Minecraft minecraft, @Nullable CharacterAbility click,
            @Nullable CharacterAbility hold) {
        int clock = ClientCharacter.clock();
        boolean down = minecraft.screen == null && minecraft.options.keyJump.isDown();
        boolean pressed = down && !spaceWas;
        spaceWas = down;
        CharacterAbility twice = click != null && click.tapWhen() == CharacterAbility.Tap.DOUBLE ? click : null;
        if (pressed) {
            if (twice != null && clock - lastSpace <= DOUBLE_WINDOW && allowed(twice, player)) {
                lastSpace = Integer.MIN_VALUE / 2;
                spaceSpent = true;
                ClientCharacter.press(player, twice.slot());
            } else {
                lastSpace = clock;
                spaceSpent = false;
            }
        }
        if (hold == null || spaceSpent) {
            letGo(player, MouseHold.SPACE);
            return;
        }
        MouseHold.Step step = MouseHold.tick(MouseHold.SPACE, hold.holdTicks(), CharacterAbility.Tap.NEVER, down,
                true);
        if (step == MouseHold.Step.HOLD) {
            startHold(player, MouseHold.SPACE, hold);
        } else if (step == MouseHold.Step.RELEASE) {
            letGo(player, MouseHold.SPACE);
        }
    }

    static boolean takesMouse(LocalPlayer player) {
        return player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    private static boolean handBusy(LocalPlayer player, GameCharacter now, CharacterAbility.Input button) {
        if (ClientRing.recharge(player, 1.0F) >= 0.0F) {
            return true;
        }
        // The defend hand always stays free, so a shield can go up while a fist charges.
        if (button == CharacterAbility.Input.RIGHT) {
            return false;
        }
        float flight = ClientRing.flight(player, 0.0F);
        if (flight >= 0.0F && flight < Flight.ARISE_TICKS) {
            return true;
        }
        boolean attackHand = ClientConstructs.wave(player.getId(), 1.0F) != null || CallArm.up(player, 1.0F) > 0.0F;
        for (AbilitySlot slot : AbilitySlot.values()) {
            CharacterAbility other = now.ability(slot);
            if (other != null && !other.onGesture() && other.isHeld() && !other.isClientOnly()
                    && ClientCharacter.isHeld(slot)) {
                attackHand = true;
            }
        }
        // One free hand is enough: the attack button waits only while the defend hand is busy too.
        return attackHand && (ClientRing.has(player, RingPayload.SHIELD) || ClientRing.has(player, RingPayload.DOME));
    }

    private static void sword(LocalPlayer player, CharacterAbility ability, int index, MouseHold.Step step,
            boolean down) {
        int data = ClientCharacter.data(player);
        if (ability.input() == CharacterAbility.Input.LEFT) {
            switch (step) {
                case TAP -> {
                    SwordMove move = SwordArms.attack(player);
                    if (move != null) {
                        ClientCharacter.send(index, true, data | Characters.TAP | move.ordinal() << Characters.MOVE_SHIFT);
                    }
                }
                case HOLD -> {
                    if (SwordArms.flurry(player)) {
                        ClientCharacter.send(index, true, data | Characters.HOLD);
                    }
                }
                case RELEASE, LET_GO -> {
                    SwordArms.stopFlurry();
                    ClientCharacter.send(index, false, data);
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
                ClientCharacter.send(index, true, data | Characters.HOLD);
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
                ClientCharacter.send(index, false, data);
            }
        } else if (!endedCharge && SwordArms.charge(player)) {
            ClientCharacter.send(index, true, data | Characters.TAP);
        }
    }

    private static void flame(LocalPlayer player, CharacterAbility ability, int index, MouseHold.Step step) {
        boolean attack = ability.input() == CharacterAbility.Input.LEFT;
        int data = ClientCharacter.data(player);
        switch (step) {
            case TAP -> {
                if (attack) {
                    FlameMove sweep = FlameArms.sweep(player);
                    if (sweep != null) {
                        ClientCharacter.send(index, true, data | Characters.TAP | sweep.ordinal() << Characters.MOVE_SHIFT);
                    }
                } else if (FlameArms.wall(player)) {
                    ClientCharacter.send(index, true, data | Characters.TAP);
                }
            }
            case HOLD -> {
                if (attack ? FlameArms.pour(player) : FlameArms.swirl(player)) {
                    ClientCharacter.send(index, true, data | Characters.HOLD);
                }
            }
            case RELEASE, LET_GO -> {
                if (attack) {
                    FlameArms.stopPouring();
                } else {
                    FlameArms.stopSwirling();
                }
                ClientCharacter.send(index, false, data);
            }
            case NOTHING -> {
            }
        }
    }

    private static void whip(LocalPlayer player, CharacterAbility ability, int index, MouseHold.Step step) {
        boolean attack = ability.input() == CharacterAbility.Input.LEFT;
        int data = ClientCharacter.data(player);
        switch (step) {
            case TAP -> {
                if (attack) {
                    WhipMove lash = WhipArms.lash(player);
                    if (lash != null) {
                        ClientCharacter.send(index, true, data | Characters.TAP | lash.ordinal() << Characters.MOVE_SHIFT);
                    }
                } else if (WhipArms.lasso(player)) {
                    ClientCharacter.send(index, true, data | Characters.TAP);
                }
            }
            case HOLD -> {
                if (attack ? WhipArms.whirl(player) : WhipArms.spin(player)) {
                    ClientCharacter.send(index, true, data | Characters.HOLD);
                }
            }
            case RELEASE, LET_GO -> {
                if (attack) {
                    WhipArms.stopWhirl();
                } else {
                    WhipArms.stopSpin();
                }
                ClientCharacter.send(index, false, data);
            }
            case NOTHING -> {
            }
        }
    }
}
