package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.math.Ease;

abstract class FlameStates {
    static final float LOWER_FROM = 2.0F;
    static final float LOWER_TICKS = 9.0F;
    static final float LOOK_BACK = 6.0F;
    private static final float STALE = 10.0F;
    private static final float SPEED_STEP = 0.05F;
    private static final float HEAT_UP = 0.12F;
    private static final float HEAT_DOWN = 0.025F;

    static final class Own {
        FlameMove move = FlameMove.EQUIP;
        float start;
        float taken;
        // negative: still whole; 0 or more: client tick it started breaking up
        float broke = -1.0F;
        boolean firing;
        boolean swirling;
        // The server's own copy of this gun, and whether it has told of the stream or vortex since it began.
        int track = -1;
        boolean confirmed;
        @Nullable
        FlameMove lastSweep;
        int felt = -1;
        float heard = -1.0F;
    }

    @Nullable
    static Own own;

    static final class Blend {
        @Nullable
        FlameMove move;
        float start = Float.NaN;
        FlameCurves.Pose from = FlameKeys.GUARD;
        @Nullable
        float[] fromSpeed;
        float posedAt = Float.NaN;
        float brokeAt = Float.NaN;
        float formed;
        @Nullable
        FlameMove before;
        float switchedAt = Float.NaN;
        float beforeStart;
        float heat;
        float heatAt = Float.NaN;
    }

    static final Map<Integer, Blend> BLENDS = new HashMap<>();
    static int clientTicks;

    record State(FlameMove move, float t, float age, float broken, boolean firing, boolean swirling) {
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
                    : now - mine.broke, mine.firing, mine.swirling);
        }
        ClientConstructs.Flame flame = ClientConstructs.flame(player.getId(), partialTick);
        if (flame == null) {
            return lingering(player, now);
        }
        FlameMove move = FlameMove.sent(flame.move());
        return new State(move == null ? FlameMove.EQUIP : move, (float) (flame.clock() - flame.moveStart()),
                (float) flame.clock(), flame.broken(), (flame.move() & FlameMove.FIRING) != 0,
                (flame.move() & FlameMove.SWIRLING) != 0);
    }

    @Nullable
    private static State lingering(Entity player, float now) {
        Blend blend = BLENDS.get(player.getId());
        if (blend == null || blend.move == null || Float.isNaN(blend.brokeAt)
                || now - blend.brokeAt >= LOWER_FROM + LOWER_TICKS) {
            return null;
        }
        return new State(blend.move, now - blend.start, blend.formed + now - blend.brokeAt, now - blend.brokeAt,
                false, false);
    }

    static FlameCurves.Pose pose(Entity player, State state, float partialTick) {
        Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new Blend());
        float now = now(partialTick);
        float time = time(partialTick);
        float start = now - state.t();
        if (blend.move != state.move() || Float.isNaN(blend.start) || Math.abs(blend.start - start) > 1.5F) {
            if (blend.move == null || Float.isNaN(blend.posedAt) || now - blend.posedAt > STALE) {
                blend.from = state.move() == FlameMove.EQUIP ? FlameKeys.REST : FlameKeys.GUARD;
                blend.fromSpeed = null;
                blend.before = null;
            } else {
                float was = start - blend.start;
                FlameCurves.Pose then = curve(blend, was, start, time);
                FlameCurves.Pose before = curve(blend, was - SPEED_STEP, start - SPEED_STEP, time - SPEED_STEP);
                float[] a = before.numbers();
                float[] b = then.numbers();
                float[] speed = new float[b.length];
                for (int c = 0; c < b.length; c++) {
                    speed[c] = (b[c] - a[c]) / SPEED_STEP;
                }
                blend.before = blend.move;
                blend.beforeStart = blend.start;
                blend.switchedAt = start;
                blend.from = then.unwound();
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
        float step = Float.isNaN(blend.heatAt) ? 0.0F : Mth.clamp(now - blend.heatAt, 0.0F, 5.0F);
        blend.heatAt = now;
        float want = FlamePoses.heatWanted(state.move(), state.t(), state.firing(), state.swirling());
        blend.heat = want > blend.heat ? Math.min(want, blend.heat + HEAT_UP * step)
                : Math.max(want, blend.heat - HEAT_DOWN * step);
        blend.posedAt = now;
        return curve(blend, state.t(), now, time);
    }

    static FlameCurves.Pose curve(Blend blend, float t, float now, float time) {
        FlameMove move = blend.move == null ? FlameMove.EQUIP : blend.move;
        FlameCurves.Pose pose = FlamePoses.at(move, Math.max(0.0F, t), time, blend.from, blend.fromSpeed);
        if (Float.isNaN(blend.brokeAt)) {
            return pose;
        }
        float lower = (float) Ease.smooth((now - blend.brokeAt - LOWER_FROM) / LOWER_TICKS);
        return lower <= 0.0F ? pose : pose.unwound().mix(FlameKeys.REST, lower);
    }

    static float heat(Entity player) {
        Blend blend = BLENDS.get(player.getId());
        return blend == null ? 0.0F : blend.heat;
    }

    static float formed(State state) {
        return state.broken() < 0.0F ? state.age() : state.age() - state.broken();
    }

    // How far into the equip the eyes still follow it, and how strongly: the look eases back when it is cut short.
    @Nullable
    static float[] watching(@Nullable Blend blend, State state, float now) {
        if (state.move() == FlameMove.EQUIP) {
            float back = state.broken() < 0.0F ? 1.0F : 1.0F - (float) Ease.smooth(state.broken() / LOOK_BACK);
            return back <= 0.0F ? null : new float[] { state.t(), back };
        }
        if (blend == null || blend.before != FlameMove.EQUIP || Float.isNaN(blend.switchedAt)
                || now - blend.switchedAt >= LOOK_BACK) {
            return null;
        }
        float back = 1.0F - (float) Ease.smooth((now - blend.switchedAt) / LOOK_BACK);
        return new float[] { now - blend.beforeStart, back };
    }

    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }
}
