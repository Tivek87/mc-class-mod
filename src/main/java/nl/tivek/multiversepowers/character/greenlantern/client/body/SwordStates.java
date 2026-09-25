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

/**
 * What the sword and shield of every Green Lantern around you are doing (see {@link SwordArms}): your own as your own
 * game plays them, everyone else's as the server tells, and the pose each body is in right now, come in from the move
 * before.
 */
abstract class SwordStates {
    // How long ago the blade is looked back at to tell which way it sweeps (it leads with its edge), in ticks; and how
    // long a move that cut taking them out short takes before its edge fully leads (taking them out, it never does).
    private static final float LEAD_STEP = 0.35F;
    private static final float LEAD_IN = 3.0F;
    // How long the shield takes to come up to block, or to go back down, in ticks.
    private static final float BLOCK_TICKS = 1.8F;
    // Once they begin to break up, how long the arms wait and then take to go back into the game's own, in ticks: they
    // lower while the pieces fly.
    static final float LOWER_FROM = 2.0F;
    static final float LOWER_TICKS = 9.0F;
    // A body whose arms were posed last longer ago than this starts a new move from the guard (or, taking them out,
    // from the game's own arms), not from where they were then, in ticks.
    private static final float STALE = 10.0F;
    // How long the eyes (or the head seen from outside) take to come back from following the sword when a move or a
    // break cuts taking them out short (or they are gone at once), and the blade seen from outside off the rim of the
    // shield, in ticks.
    static final float LOOK_BACK = 6.0F;
    // How far back in time the arms are looked at to tell how fast they move, in ticks.
    private static final float SPEED_STEP = 0.05F;

    /** Your own sword and shield: played the moment you click, before the server has heard of it. */
    static final class Own {
        SwordMove move = SwordMove.EQUIP;
        float start;
        float taken;
        float broke = -1.0F;
        boolean charging;
        boolean blocking;
        float blockSince;
        int charged;
        // The server's tick the last ram it told of began on, so each one is played once.
        double ram = Double.NaN;
        Vec3 way = new Vec3(0.0, 0.0, 1.0);
        @Nullable
        SwordMove lastAttack;
        // The last tick of the move a blow was felt on, so each blow jolts the view once.
        int felt = -1;
        // How far into taking them out the last frame was, so each of its sounds and jolts comes once, on its frame.
        float heard = -1.0F;
    }

    @Nullable
    static Own own;

    /**
     * What one body's arms are doing: the move and when it began (client ticks), where the arms were as it began and
     * how fast they moved then, when they were posed last, when the sword and shield began to break up and how long
     * after they took shape that was, the move before (while the eyes come back from it) and how far its shield is up.
     */
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

    /**
     * Someone's sword and shield right now: the move, how many ticks into it, how many ticks ago they took shape, how many
     * ticks ago they began to break up (-1 while whole), the way a charge runs, and whether he blocks or charges.
     */
    record State(SwordMove move, float t, float age, float broken, Vec3 way, boolean blocking, boolean charging) {
    }

    static float now(float partialTick) {
        return clientTicks + partialTick;
    }

    /** How this player's sword and shield stand, or null when he holds none. */
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

    /**
     * Someone else's sword and shield once the server has let go of their pieces: his arms still go on lowering back
     * into his own for a moment. Null once they are there (or when they were never breaking up).
     */
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

    /**
     * The pose of this player right now: the move, coming in from wherever the arms were as it began and as fast as
     * they moved then, with the shield held up to block laid over it, and lowered back into his own arms once the sword
     * and shield break up. Remembers the move, so the next one can come in from it.
     */
    static SwordPoses.Pose pose(Entity player, State state, float partialTick) {
        Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new Blend());
        float now = now(partialTick);
        float start = now - state.t();
        if (blend.move != state.move() || Float.isNaN(blend.start) || Math.abs(blend.start - start) > 1.5F) {
            if (blend.move == null || Float.isNaN(blend.posedAt) || now - blend.posedAt > STALE) {
                // Nothing to come in from: taking them out starts from the game's own arms, all else from the guard.
                blend.from = state.move() == SwordMove.EQUIP ? SwordPoses.REST : SwordPoses.GUARD;
                blend.fromSpeed = null;
                blend.before = null;
            } else {
                // On from where the arms were the moment the new move began, and as fast as they were moving then.
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
        // A cut, a thrust or the flurry lowers the shield for as long as the move lasts.
        float want = state.blocking() && !state.charging() && state.broken() < 0.0F && !state.move().swings(state.t())
                ? 1.0F : 0.0F;
        blend.block = want > blend.block ? Math.min(want, blend.block + step) : Math.max(want, blend.block - step);
        return earlier(blend, state, now, 0.0F);
    }

    /**
     * The arms of a body {@code t} ticks into the move it is doing, at {@code time} (client ticks): along the move, and
     * lowered back into his own arms once the sword and shield break up.
     */
    static SwordPoses.Pose curve(Blend blend, float t, float time) {
        SwordMove move = blend.move == null ? SwordMove.EQUIP : blend.move;
        SwordPoses.Pose pose = SwordPoses.at(move, Math.max(0.0F, t), time, blend.from, blend.fromSpeed);
        if (Float.isNaN(blend.brokeAt)) {
            return pose;
        }
        float lower = (float) Ease.smooth((time - blend.brokeAt - LOWER_FROM) / LOWER_TICKS);
        return lower <= 0.0F ? pose : pose.mix(SwordPoses.REST, lower);
    }

    /** The pose of a body {@code ago} ticks before now, from the move it is doing now (for streaks and edges). */
    static SwordPoses.Pose earlier(Blend blend, State state, float now, float ago) {
        SwordPoses.Pose pose = curve(blend, state.t() - ago, now - ago);
        return SwordPoses.block(pose, (float) Ease.smooth(blend.block));
    }

    /**
     * The pose right now with the edge of the blade leading the way it sweeps (see {@link SwordPoses#led}); taking them
     * out, the blade is turned exactly as the moves of the wrist say, and a move that cut that short only comes to lead
     * with its edge over a moment, not all at once.
     */
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

    /**
     * How many ticks after they began to take shape the sword and shield were as they began to break up (or are now,
     * while whole): what had grown by then is all that breaks up.
     */
    static float formed(State state) {
        return state.broken() < 0.0F ? state.age() : state.age() - state.broken();
    }

    /**
     * How far into taking them out the eyes (or the head seen from outside) follow the sword right now, and how much:
     * while he takes them out, and for a moment after a move or a break cut that short, as they come back. Null when
     * they do not. The blade seen from outside comes off the rim of the shield the same way.
     */
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

    /** The pose of taking them out {@code t} ticks in, for where the eyes look once a move has cut it short. */
    static SwordPoses.Pose taking(float t, float time) {
        return SwordPoses.at(SwordMove.EQUIP, t, time, SwordPoses.REST, null);
    }

    /** The settings of the sword and shield: those of the Construct Wheel. */
    static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }
}
