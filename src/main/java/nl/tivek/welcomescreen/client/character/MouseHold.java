package nl.tivek.welcomescreen.client.character;

import javax.annotation.Nullable;
import nl.tivek.welcomescreen.character.CharacterAbility;

/**
 * Click or hold, the rule every ability on a mouse button follows (see {@link CharacterAbility#holdVersion}),
 * with empty hands and with every construct alike: a tap of the button does the quick version, and holding it
 * down long enough does the hold version instead, for as long as the button stays down.
 *
 * <p>Only the timing lives here. What each step means is the ability's own business; this only says which step
 * the button has reached.
 */
public final class MouseHold {
    /** What the button just did. */
    public enum Step {
        NOTHING,
        /** The quick version goes off: as the button went down, or as it came up again in time. */
        TAP,
        /** The button has been down long enough: the hold version starts. */
        HOLD,
        /** A button that was held long enough came up: the hold version stops. */
        RELEASE
    }

    // Per button (left, right): how many ticks it has been down, -1 while it is up, and whether it got as far as
    // the hold version.
    private static final int[] DOWN = { -1, -1 };
    private static final boolean[] HOLDING = new boolean[2];

    private MouseHold() {
    }

    /**
     * One client tick of one ability on a mouse button.
     *
     * @param down      whether its button is down and belongs to the ability right now
     * @param cancelled true when the button was taken away from the ability (a screen opened, something came into
     *                  your hands): a button that goes up that way never counts as a tap
     */
    public static Step tick(CharacterAbility ability, boolean down, boolean cancelled) {
        int i = index(ability.mouseButton());
        if (i < 0) {
            return Step.NOTHING;
        }
        if (down) {
            if (DOWN[i] < 0) {
                DOWN[i] = 0;
                return ability.tapWhen() == CharacterAbility.Tap.PRESS ? Step.TAP : Step.NOTHING;
            }
            DOWN[i]++;
            if (ability.holdTicks() > 0 && !HOLDING[i] && DOWN[i] >= ability.holdTicks()) {
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
        return !cancelled && ability.tapWhen() == CharacterAbility.Tap.RELEASE ? Step.TAP : Step.NOTHING;
    }

    /**
     * How far this button is on its way to the hold version of this ability: 0 as it goes down, 1 once the hold
     * version runs, or -1 while the button is up (or the ability has no hold version).
     */
    public static float progress(@Nullable CharacterAbility ability, float partialTick) {
        if (ability == null || ability.holdTicks() <= 0) {
            return -1.0F;
        }
        int i = index(ability.mouseButton());
        if (i < 0 || DOWN[i] < 0) {
            return -1.0F;
        }
        return HOLDING[i] ? 1.0F : Math.min(1.0F, (DOWN[i] + partialTick) / ability.holdTicks());
    }

    /** True while this button is held long enough for its hold version. */
    public static boolean holding(CharacterAbility.Mouse button) {
        int i = index(button);
        return i >= 0 && HOLDING[i];
    }

    /** Forgets both buttons, without any step: you turned into someone else, or left the world. */
    public static void reset() {
        DOWN[0] = DOWN[1] = -1;
        HOLDING[0] = HOLDING[1] = false;
    }

    private static int index(CharacterAbility.Mouse button) {
        return switch (button) {
            case LEFT -> 0;
            case RIGHT -> 1;
            case NONE -> -1;
        };
    }
}
