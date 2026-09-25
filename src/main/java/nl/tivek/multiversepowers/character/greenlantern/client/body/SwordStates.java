package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.engine.math.Ease;

abstract class SwordStates {
    private static final float LEAD_STEP = 0.35F;
    private static final float LEAD_IN = 3.0F;
    private static final float BLOCK_TICKS = 1.8F;
    static final float LOWER_FROM = 2.0F;
    static final float LOWER_TICKS = 9.0F;
    private static final float STALE = 10.0F;
    static final float LOOK_BACK = 6.0F;
    private static final float SPEED_STEP = 0.05F;

    static final class Own {
        SwordMove move = SwordMove.EQUIP;
        float start;
        float taken;
        // negative: still whole; 0 or more: client tick it started breaking up
        float broke = -1.0F;
        boolean charging;
        boolean blocking;
        float blockSince;
        int charged;
        double ram = Double.NaN;
        Vec3 way = new Vec3(0.0, 0.0, 1.0);
        @Nullable
        SwordMove lastAttack;
        int felt = -1;
        float heard = -1.0F;
    }

    @Nullable
    static Own own;

    static final class Blend {
        @Nullable
        SwordMove move;
        float start = Float.NaN;
        SwordPoses.Pose from = SwordPoses.GUARD;
        @Nullable
        float[] fromSpeed;
        float posedAt = Float.NaN;
        float brokeAt = Float.NaN;
        float formed;
        @Nullable
        SwordMove before;
        float beforeStart;
        float switchedAt = Float.NaN;
        float block;
        float blockAt = Float.NaN;
    }

    static final Map<Integer, Blend> BLENDS = new HashMap<>();
    static int clientTicks;

    record State(SwordMove move, float t, float age, float broken, Vec3 way, boolean blocking, boolean charging) {
    }

    static float now(float partialTick) {
        return clientTicks + partialTick;
    }

    @Nullable
    static State state(Entity player, float partialTick) {
        float now = now(partialTick);
        if (player == Minecraft.getInstance().player) {
            Own mine = own;
            if (mine == null) {
                return null;
            }
            return new State(mine.move, now - mine.start, now - mine.taken, mine.broke < 0.0F ? -1.0F : now - mine.broke,
                    mine.way, mine.blocking, mine.charging);
        }
        ClientConstructs.Sword sword = ClientConstructs.sword(player.getId(), partialTick);
        if (sword == null) {
            return lingering(player, now);
        }
        SwordMove move = SwordMove.sent(sword.move());
        return new State(move == null ? SwordMove.EQUIP : move, (float) (sword.clock() - sword.moveStart()),
                (float) sword.clock(), sword.broken(), sword.way(), (sword.move() & SwordMove.BLOCKING) != 0,
                (sword.move() & SwordMove.CHARGING) != 0);
    }

    @Nullable
    private static State lingering(Entity player, float now) {
        Blend blend = BLENDS.get(player.getId());
        if (blend == null || blend.move == null || Float.isNaN(blend.brokeAt)
                || now - blend.brokeAt >= LOWER_FROM + LOWER_TICKS) {
            return null;
        }
        return new State(blend.move, now - blend.start, blend.formed + now - blend.brokeAt, now - blend.brokeAt,
                new Vec3(0.0, 0.0, 1.0), false, false);
    }

    static SwordPoses.Pose pose(Entity player, State state, float partialTick) {
        Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new Blend());
        float now = now(partialTick);
        float start = now - state.t();
        if (blend.move != state.move() || Float.isNaN(blend.start) || Math.abs(blend.start - start) > 1.5F) {
            if (blend.move == null || Float.isNaN(blend.posedAt) || now - blend.posedAt > STALE) {
                blend.from = state.move() == SwordMove.EQUIP ? SwordPoses.REST : SwordPoses.GUARD;
                blend.fromSpeed = null;
                blend.before = null;
            } else {
                float was = start - blend.start;
                SwordPoses.Pose then = curve(blend, was, start);
                SwordPoses.Pose before = curve(blend, was - SPEED_STEP, start - SPEED_STEP);
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
        blend.posedAt = now;
        float step = Float.isNaN(blend.blockAt) ? 1.0F : Mth.clamp(now - blend.blockAt, 0.0F, 5.0F) / BLOCK_TICKS;
        blend.blockAt = now;
        float want = state.blocking() && !state.charging() && state.broken() < 0.0F && !state.move().swings(state.t())
                ? 1.0F : 0.0F;
        blend.block = want > blend.block ? Math.min(want, blend.block + step) : Math.max(want, blend.block - step);
        return earlier(blend, state, now, 0.0F);
    }

    static SwordPoses.Pose curve(Blend blend, float t, float time) {
        SwordMove move = blend.move == null ? SwordMove.EQUIP : blend.move;
        SwordPoses.Pose pose = SwordPoses.at(move, Math.max(0.0F, t), time, blend.from, blend.fromSpeed);
        if (Float.isNaN(blend.brokeAt)) {
            return pose;
        }
        float lower = (float) Ease.smooth((time - blend.brokeAt - LOWER_FROM) / LOWER_TICKS);
        return lower <= 0.0F ? pose : pose.mix(SwordPoses.REST, lower);
    }

    static SwordPoses.Pose earlier(Blend blend, State state, float now, float ago) {
        SwordPoses.Pose pose = curve(blend, state.t() - ago, now - ago);
        return SwordPoses.block(pose, (float) Ease.smooth(blend.block));
    }

    static SwordPoses.Pose leading(Entity player, State state, float partialTick) {
        SwordPoses.Pose pose = pose(player, state, partialTick);
        if (state.move() == SwordMove.EQUIP) {
            return pose;
        }
        Blend blend = BLENDS.get(player.getId());
        float now = now(partialTick);
        boolean cutShort = blend.before == SwordMove.EQUIP && !Float.isNaN(blend.switchedAt)
                && blend.switchedAt - blend.beforeStart < SwordMove.EQUIP.ticks();
        float amount = cutShort ? (float) Ease.smoother((now - blend.switchedAt) / LEAD_IN) : 1.0F;
        return SwordPoses.led(pose, earlier(blend, state, now, LEAD_STEP), amount);
    }

    static float formed(State state) {
        return state.broken() < 0.0F ? state.age() : state.age() - state.broken();
    }

    @Nullable
    static float[] watching(Blend blend, State state, float now) {
        if (state.move() == SwordMove.EQUIP) {
            float back = state.broken() < 0.0F ? 1.0F : 1.0F - (float) Ease.smooth(state.broken() / LOOK_BACK);
            return back <= 0.0F ? null : new float[] { state.t(), back };
        }
        if (blend.before != SwordMove.EQUIP || Float.isNaN(blend.switchedAt) || now - blend.switchedAt >= LOOK_BACK) {
            return null;
        }
        float back = 1.0F - (float) Ease.smooth((now - blend.switchedAt) / LOOK_BACK);
        return new float[] { now - blend.beforeStart, back };
    }

    static SwordPoses.Pose taking(float t, float time) {
        return SwordPoses.at(SwordMove.EQUIP, t, time, SwordPoses.REST, null);
    }

    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }
}
