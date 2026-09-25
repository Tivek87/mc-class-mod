package nl.tivek.multiversepowers.character.client;

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

    private static final int[] DOWN = { -1, -1 };
    private static final boolean[] HOLDING = new boolean[2];

    private MouseHold() {
    }

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
        if (ability.tapWhen() == CharacterAbility.Tap.PRESS) {
            return ability.holdTicks() > 0 ? Step.LET_GO : Step.NOTHING;
        }
        return !cancelled ? Step.TAP : Step.NOTHING;
    }

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

    public static boolean holding(CharacterAbility.Mouse button) {
        int i = index(button);
        return i >= 0 && HOLDING[i];
    }

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
