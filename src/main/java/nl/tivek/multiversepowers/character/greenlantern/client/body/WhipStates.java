package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipLash;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.math.Ease;

abstract class WhipStates {
    static final float LOWER_FROM = 2.0F;
    static final float LOWER_TICKS = 9.0F;
    static final float LOOK_BACK = 6.0F;
    private static final float STALE = 10.0F;
    private static final float SPEED_STEP = 0.05F;
    // How far back a moment along the lash may look past the start of its move, going on the way the last one went.
    private static final float BACK_REACH = 1.5F;

    static final class Own {
        WhipMove move = WhipMove.EQUIP;
        float start;
        float taken;
        // negative: still whole; 0 or more: client tick it started breaking up
        float broke = -1.0F;
        boolean whirling;
        boolean spinning;
        // How long the whirl or spin ran that the current move ends.
        double before;
        // The server's own copy of this whip, and whether it has told of the whirl or spin since it began.
        int track = -1;
        boolean confirmed;
        // The creature the lasso was thrown at, as this game saw it: the server's own word takes over when it comes.
        int aimed = -1;
        @Nullable
        WhipMove lastAttack;
        int felt = -1;
        float heard = -1.0F;
    }

    @Nullable
    static Own own;

    static final class Blend {
        @Nullable
        WhipMove move;
        float start = Float.NaN;
        WhipCurves.Pose from = WhipKeys.GUARD;
        @Nullable
        float[] fromSpeed;
        float posedAt = Float.NaN;
        float brokeAt = Float.NaN;
        float formed;
        @Nullable
        WhipMove before;
        float switchedAt = Float.NaN;
        float beforeStart;
    }

    static final Map<Integer, Blend> BLENDS = new HashMap<>();
    static int clientTicks;

    record State(WhipMove move, float t, float age, float broken, boolean whirling, boolean spinning, double before) {
    }

    static float now(float partialTick) {
        return clientTicks + partialTick;
    }

    static float time(float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level == null ? 0.0F : (float) (minecraft.level.getGameTime() % 24000L) + partialTick;
    }

    @Nullable
    static State state(Entity player, float partialTick) {
        float now = now(partialTick);
        if (player == Minecraft.getInstance().player) {
            Own mine = own;
            if (mine == null) {
                return null;
            }
            return new State(mine.move, now - mine.start, now - mine.taken, mine.broke < 0.0F ? -1.0F
                    : now - mine.broke, mine.whirling, mine.spinning, mine.before);
        }
        ClientConstructs.Whip whip = ClientConstructs.whip(player.getId(), partialTick);
        if (whip == null) {
            return lingering(player, now);
        }
        WhipMove move = WhipMove.sent(whip.move());
        Blend blend = BLENDS.get(player.getId());
        double before = blend != null && blend.before != null && blend.before.held() && !Float.isNaN(blend.switchedAt)
                ? blend.switchedAt - blend.beforeStart : 0.0;
        return new State(move == null ? WhipMove.EQUIP : move, (float) (whip.clock() - whip.moveStart()),
                (float) whip.clock(), whip.broken(), (whip.move() & WhipMove.WHIRLING) != 0,
                (whip.move() & WhipMove.SPINNING) != 0, before);
    }

    @Nullable
    private static State lingering(Entity player, float now) {
        Blend blend = BLENDS.get(player.getId());
        if (blend == null || blend.move == null || Float.isNaN(blend.brokeAt)
                || now - blend.brokeAt >= LOWER_FROM + LOWER_TICKS) {
            return null;
        }
        return new State(blend.move, now - blend.start, blend.formed + now - blend.brokeAt, now - blend.brokeAt,
                false, false, 0.0);
    }

    static WhipCurves.Pose pose(Entity player, State state, float partialTick) {
        Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new Blend());
        float now = now(partialTick);
        float time = time(partialTick);
        float start = now - state.t();
        if (blend.move != state.move() || Float.isNaN(blend.start) || Math.abs(blend.start - start) > 1.5F) {
            if (blend.move == null || Float.isNaN(blend.posedAt) || now - blend.posedAt > STALE) {
                blend.from = state.move() == WhipMove.EQUIP ? WhipKeys.REST : WhipKeys.READY;
                blend.fromSpeed = null;
                blend.before = null;
            } else {
                float was = start - blend.start;
                WhipCurves.Pose then = curve(blend, was, start, time, state.before());
                WhipCurves.Pose earlier = curve(blend, was - SPEED_STEP, start - SPEED_STEP, time - SPEED_STEP,
                        state.before());
                float[] a = earlier.numbers();
                float[] b = then.numbers();
                float[] lashA = WhipCurves.lashOf(a);
                WhipLash.nearest(lashA, WhipCurves.lashOf(b));
                a[WhipCurves.Pose.BODY + WhipLash.YAW] = lashA[WhipLash.YAW];
                a[WhipCurves.Pose.BODY + WhipLash.PITCH] = lashA[WhipLash.PITCH];
                float[] speed = new float[b.length];
                for (int c = 0; c < b.length; c++) {
                    speed[c] = (b[c] - a[c]) / SPEED_STEP;
                }
                blend.before = blend.move;
                blend.beforeStart = blend.start;
                blend.switchedAt = start;
                WhipCurves.Pose unwound = then.unwound();
                if (WhipLash.nearest(then.lash().clone(), WhipMove.REST)) {
                    speed[WhipCurves.Pose.BODY + WhipLash.PITCH] = -speed[WhipCurves.Pose.BODY + WhipLash.PITCH];
                }
                blend.from = unwound;
                blend.fromSpeed = speed;
            }
            blend.move = state.move();
            blend.start = start;
            blend.brokeAt = Float.NaN;
        }
        if (state.broken() >= 0.0F) {
            float broke = now - state.broken();
            if (Float.isNaN(blend.brokeAt) || Math.abs(blend.brokeAt - broke) > 1.5F) {
                blend.brokeAt = broke;
                blend.formed = formed(state);
            }
        } else {
            blend.brokeAt = Float.NaN;
        }
        blend.posedAt = now;
        return curve(blend, state.t(), now, time, state.before());
    }

    static WhipCurves.Pose curve(Blend blend, float t, float now, float time, double before) {
        WhipMove move = blend.move == null ? WhipMove.EQUIP : blend.move;
        WhipCurves.Pose pose = WhipPoses.at(move, Math.max(0.0F, t), time, blend.from, blend.fromSpeed, before);
        if (Float.isNaN(blend.brokeAt)) {
            return pose;
        }
        float lower = (float) Ease.smooth((now - blend.brokeAt - LOWER_FROM) / LOWER_TICKS);
        return lower <= 0.0F ? pose : pose.unwound().mix(WhipKeys.REST, lower).lashed(pose.lash());
    }

    // The lash's aim at a moment of the current move; before the move began it goes on the way the last one went.
    static float[] lashAt(Blend blend, float t, float time, double before) {
        WhipMove move = blend.move == null ? WhipMove.EQUIP : blend.move;
        if (t >= 0.0F) {
            return WhipPoses.lash(move, t, time, blend.from, blend.fromSpeed, before);
        }
        float[] lash = blend.from.lash().clone();
        float[] speed = blend.fromSpeed;
        if (speed != null) {
            float back = Math.max(t, -BACK_REACH);
            for (int c = 0; c < lash.length; c++) {
                lash[c] += speed[WhipCurves.Pose.BODY + c] * back;
            }
        }
        return lash;
    }

    static float formed(State state) {
        return state.broken() < 0.0F ? state.age() : state.age() - state.broken();
    }

    // How far into the equip the eyes still follow it, and how strongly: the look eases back when it is cut short.
    @Nullable
    static float[] watching(@Nullable Blend blend, State state, float now) {
        if (state.move() == WhipMove.EQUIP) {
            float back = state.broken() < 0.0F ? 1.0F : 1.0F - (float) Ease.smooth(state.broken() / LOOK_BACK);
            return back <= 0.0F ? null : new float[] { state.t(), back };
        }
        if (blend == null || blend.before != WhipMove.EQUIP || Float.isNaN(blend.switchedAt)
                || now - blend.switchedAt >= LOOK_BACK) {
            return null;
        }
        float back = 1.0F - (float) Ease.smooth((now - blend.switchedAt) / LOOK_BACK);
        return new float[] { now - blend.beforeStart, back };
    }

    static float ours(WhipCurves.Pose pose) {
        return 1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F);
    }

    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }
}
