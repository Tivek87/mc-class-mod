package nl.tivek.multiversepowers.character.client;

import java.util.Arrays;
import javax.annotation.Nullable;
import nl.tivek.multiversepowers.character.CharacterAbility;

public final class MouseHold {
    public enum Step {
        NOTHING,
        TAP,
        HOLD,
        RELEASE,
        LET_GO
    }

    // One channel per button: left, right, the scroll click, the scroll hold when it sits on a key of its own, space,
    // shift.
    static final int LEFT = 0;
    static final int RIGHT = 1;
    static final int SCROLL = 2;
    static final int SCROLL_HOLD = 3;
    static final int SPACE = 4;
    static final int SHIFT = 5;
    static final int CHANNELS = 6;

    private static final int[] DOWN = new int[CHANNELS];
    private static final boolean[] HOLDING = new boolean[CHANNELS];
    // The hold time each button went by last (a character may shorten one, Gestures.holdTime).
    private static final int[] TICKS = new int[CHANNELS];

    static {
        reset();
    }

    private MouseHold() {
    }

    // A button with a hold time: letting go before it is a click, holding on for it the hold.
    static Step tick(int i, int holdTicks, CharacterAbility.Tap tap, boolean down, boolean cancelled) {
        TICKS[i] = holdTicks;
        if (down) {
            if (DOWN[i] < 0) {
                DOWN[i] = 0;
                return tap == CharacterAbility.Tap.PRESS ? Step.TAP : Step.NOTHING;
            }
            DOWN[i]++;
            if (holdTicks > 0 && !HOLDING[i] && DOWN[i] >= holdTicks) {
                HOLDING[i] = true;
                return Step.HOLD;
            }
            return Step.NOTHING;
        }
        if (DOWN[i] < 0) {
            return Step.NOTHING;
        }
        boolean held = HOLDING[i];
        DOWN[i] = -1;
        HOLDING[i] = false;
        if (held) {
            return Step.RELEASE;
        }
        if (tap == CharacterAbility.Tap.PRESS) {
            return holdTicks > 0 ? Step.LET_GO : Step.NOTHING;
        }
        return tap == CharacterAbility.Tap.RELEASE && !cancelled ? Step.TAP : Step.NOTHING;
    }

    public static float progress(@Nullable CharacterAbility ability, float partialTick) {
        if (ability == null || ability.holdTicks() <= 0) {
            return -1.0F;
        }
        int i = channel(ability);
        // A button held with no hold on it now (AbilityPanel.Rules.holds) fills nothing.
        if (i < 0 || DOWN[i] < 0 || TICKS[i] <= 0) {
            return -1.0F;
        }
        return HOLDING[i] ? 1.0F : Math.min(1.0F, (DOWN[i] + partialTick) / TICKS[i]);
    }

    public static boolean holding(CharacterAbility.Input button) {
        int i = channel(button);
        return i >= 0 && HOLDING[i];
    }

    public static void reset() {
        Arrays.fill(DOWN, -1);
        Arrays.fill(HOLDING, false);
    }

    static void reset(int i) {
        DOWN[i] = -1;
        HOLDING[i] = false;
    }

    private static int channel(CharacterAbility ability) {
        if (ability.input() == CharacterAbility.Input.SCROLL && ability.tapWhen() == CharacterAbility.Tap.NEVER
                && !Gestures.scrollOnOneKey()) {
            return SCROLL_HOLD;
        }
        return channel(ability.input());
    }

    static int channel(CharacterAbility.Input button) {
        return switch (button) {
            case LEFT -> LEFT;
            case RIGHT -> RIGHT;
            case SCROLL -> SCROLL;
            case SPACE -> SPACE;
            case SHIFT -> SHIFT;
            case KEY -> -1;
        };
    }
}
