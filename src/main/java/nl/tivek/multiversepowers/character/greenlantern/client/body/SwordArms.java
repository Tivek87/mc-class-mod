package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.AbilityActionPayload;
import nl.tivek.multiversepowers.character.CharacterAbility;
import nl.tivek.multiversepowers.character.Characters;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.SwordShield;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.ConstructChoice;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * The sword and shield of the construct wheel in the hands of every Green Lantern around you (see
 * {@link SwordShield}): which move each is doing and how far into it, whether he holds the shield up or charges, the
 * pose that goes with it (see {@link SwordPoses}), and where the sword and shield are drawn.
 * <ul>
 * <li>Your own sword and shield play every move the moment you click, before the server has even heard of it; everyone
 * else's play what the server tells. The rams of a charge are the server's to throw: yours play as it tells of them.
 * Taking them out, you hear it the moment it happens; everyone else hears it from the server.</li>
 * <li>In first person you see the poses as they are made: your right fist round the grip of the sword, which stands
 * upright in the guard, and your left forearm on the back of the shield, the fist round its grip; both arms reach in
 * from below the edges of the screen. They come out of the game's own resting hand and go back into it after they break
 * up. A blow that lands jolts your view a little, and while you take them out your eyes follow the sword.</li>
 * <li>Seen from outside, each arm points at where the pose puts its hand, and the upper body turns, bends and steps into
 * the move (the spinning cut turns the whole body round); the sword and shield hang where his hands were really drawn
 * (see {@link SwordSpot}), and while he takes them out his head follows the sword.</li>
 * <li>While you charge behind the shield your own game runs you straight ahead, and stops you at a wall.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SwordArms {
    // First person: where your arms reach in from, below the bottom corners of the screen (x to the right, y up, -z
    // ahead), which follow the hands this much of the way they move. The arms are drawn this much of their thickness,
    // so they stay slim, and always long enough to run out of sight (never a stump hanging in view): as far as that
    // takes, and a little more, at least this much of their own length and at most this much.
    private static final Vec3 OWN_SHOULDER_RIGHT = new Vec3(0.8, -1.3, 0.1);
    private static final Vec3 OWN_SHOULDER_LEFT = new Vec3(-0.86, -1.32, 0.12);
    private static final double OWN_FOLLOW = 0.3;
    private static final float OWN_ARM = 0.66F;
    private static final double OWN_ARM_PAST = 0.1;
    private static final float OWN_ARM_SHORTEST = 0.66F;
    private static final float OWN_ARM_LONGEST = 1.3F;
    // How far an arm runs back from its fist to its shoulder at its own size, in blocks (from the fist, 9 pixels along
    // it, to its top at -2), and the field of view the game draws your hands with, whatever yours is set to (degrees).
    private static final double ARM_BACK = 11.0 / 16.0;
    private static final double HAND_FOV = 70.0;
    // Whether your own arms are being drawn in first person right now (the game poses them as it draws them).
    private static boolean ownArms;
    // How many earlier moments the streak of a swung blade reaches back over, and how far apart they are, in ticks.
    private static final int TRAIL = 10;
    private static final float TRAIL_STEP = 0.45F;
    // How long ago the blade is looked back at to tell which way it sweeps (it leads with its edge), in ticks; and how
    // long a move that cut taking them out short takes before its edge fully leads (taking them out, it never does).
    private static final float LEAD_STEP = 0.35F;
    private static final float LEAD_IN = 3.0F;
    // How long the shield takes to come up to block, or to go back down, in ticks.
    private static final float BLOCK_TICKS = 1.8F;
    // A charge widens your view this much.
    private static final float CHARGE_FOV = 1.12F;
    // A blow that lands jolts your own view: for this many ticks, rolled this far with the blow and dipped this far, in
    // degrees; a slam of the shield twice as hard.
    private static final float KICK_TICKS = 5.0F;
    private static final float KICK_ROLL = 1.6F;
    private static final float KICK_DIP = 0.8F;
    // How far apart the moments of the blur of the tossed sword's spin are, in ticks.
    private static final float TOSS_TRAIL_STEP = 0.2F;
    // Once they begin to break up, how long the arms wait and then take to go back into the game's own, in ticks: they
    // lower while the pieces fly.
    private static final float LOWER_FROM = 2.0F;
    private static final float LOWER_TICKS = 9.0F;
    // A body whose arms were posed last longer ago than this starts a new move from the guard (or, taking them out,
    // from the game's own arms), not from where they were then, in ticks.
    private static final float STALE = 10.0F;
    // How long the eyes (or the head seen from outside) take to come back from following the sword when a move or a
    // break cuts taking them out short (or they are gone at once), and the blade seen from outside off the rim of the
    // shield, in ticks.
    private static final float LOOK_BACK = 6.0F;
    // How far back in time the arms are looked at to tell how fast they move, in ticks.
    private static final float SPEED_STEP = 0.05F;
    // How thick the ring's beam that feeds the growing shield is in first person.
    private static final double OWN_BEAM = 0.4;

    /** Your own sword and shield: played the moment you click, before the server has heard of it. */
    private static final class Own {
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
    private static Own own;
    /** Where your own sword and shield were drawn last, in first person: to break up from if you take new ones out. */
    private record Drawn(Vec3 grip, Vec3 blade, Vec3 edge, Vec3 shield, Vec3 face, Vec3 top, float sword,
            float shieldGrown) {
    }

    @Nullable
    private static Drawn drawn;
    // The pieces of your own sword and shield that were still flying apart as you took new ones out, and since when
    // they break up (client ticks).
    @Nullable
    private static Drawn shards;
    private static float shardsSince;
    // The jolt of the last blow that landed: when (client ticks), and which way it rolls and how hard.
    private static float kickAt = -100.0F;
    private static float kickRoll;
    private static float kickHard;
    // How much your eyes follow the sword and a blow jolts your view (0 to 1): all of it while the sword and shield are
    // drawn in your hands, easing in and out as they come and go there (see handsFree), and when that was worked out
    // last (client ticks). How far your eyes followed the sword last (see ownLook), and, once the sword and shield were
    // gone at once (see forget), from then on and from how far they come back.
    private static float shown;
    private static float shownAt = Float.NaN;
    @Nullable
    private static float[] looked;
    @Nullable
    private static float[] lookGone;
    private static float lookGoneAt;

    /**
     * What one body's arms are doing: the move and when it began (client ticks), where the arms were as it began and
     * how fast they moved then, when they were posed last, when the sword and shield began to break up and how long
     * after they took shape that was, the move before (while the eyes come back from it) and how far its shield is up.
     */
    private static final class Blend {
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

    private static final Map<Integer, Blend> BLENDS = new HashMap<>();
    // The bodies turned round for the spinning cut while their model is drawn, by how far (see turnModel).
    private static final Map<Integer, Float> SPUN = new HashMap<>();
    private static int clientTicks;

    /**
     * Someone's sword and shield right now: the move, how many ticks into it, how many ticks ago they took shape, how many
     * ticks ago they began to break up (-1 while whole), the way a charge runs, and whether he blocks or charges.
     */
    record State(SwordMove move, float t, float age, float broken, Vec3 way, boolean blocking, boolean charging) {
    }

    private SwordArms() {
    }

    private static float now(float partialTick) {
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
    private static SwordPoses.Pose curve(Blend blend, float t, float time) {
        SwordMove move = blend.move == null ? SwordMove.EQUIP : blend.move;
        SwordPoses.Pose pose = SwordPoses.at(move, Math.max(0.0F, t), time, blend.from, blend.fromSpeed);
        if (Float.isNaN(blend.brokeAt)) {
            return pose;
        }
        float lower = (float) Ease.smooth((time - blend.brokeAt - LOWER_FROM) / LOWER_TICKS);
        return lower <= 0.0F ? pose : pose.mix(SwordPoses.REST, lower);
    }

    /** The pose of a body {@code ago} ticks before now, from the move it is doing now (for streaks and edges). */
    private static SwordPoses.Pose earlier(Blend blend, State state, float now, float ago) {
        SwordPoses.Pose pose = curve(blend, state.t() - ago, now - ago);
        return SwordPoses.block(pose, (float) Ease.smooth(blend.block));
    }

    /**
     * The pose right now with the edge of the blade leading the way it sweeps (see {@link SwordPoses#led}); taking them
     * out, the blade is turned exactly as the moves of the wrist say, and a move that cut that short only comes to lead
     * with its edge over a moment, not all at once.
     */
    private static SwordPoses.Pose leading(Entity player, State state, float partialTick) {
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
    private static float formed(State state) {
        return state.broken() < 0.0F ? state.age() : state.age() - state.broken();
    }

    /**
     * How far into taking them out the eyes (or the head seen from outside) follow the sword right now, and how much:
     * while he takes them out, and for a moment after a move or a break cut that short, as they come back. Null when
     * they do not. The blade seen from outside comes off the rim of the shield the same way.
     */
    @Nullable
    private static float[] watching(Blend blend, State state, float now) {
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
    private static SwordPoses.Pose taking(float t, float time) {
        return SwordPoses.at(SwordMove.EQUIP, t, time, SwordPoses.REST, null);
    }

    // ---- Your own: what the mouse does with them ----

    /** True while you hold the sword and shield, whole: the mouse is theirs. */
    public static boolean holding() {
        return own != null && own.broke < 0.0F;
    }

    /** True while you charge behind the shield. */
    public static boolean charging() {
        return own != null && own.charging;
    }

    /** True while you may start a new move: the last one has come far enough, and you are not charging. */
    private static boolean ready() {
        Own mine = own;
        return mine != null && mine.broke < 0.0F && !mine.charging && now(0.0F) - mine.start >= mine.move.ready();
    }

    private static void begin(SwordMove move) {
        if (own != null) {
            own.move = move;
            own.start = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            own.felt = -1;
        }
    }

    /** A tap of the attack button: one of the sword's moves at random, or null when the last one is not done yet. */
    @Nullable
    public static SwordMove attack(LocalPlayer player) {
        if (!ready()) {
            return null;
        }
        SwordMove move = SwordMove.randomAttack(player.getRandom(), own.lastAttack);
        own.lastAttack = move;
        begin(move);
        return move;
    }

    /** The attack button held long enough: the flurry, as long as you are free for it and the ring can pay. */
    public static boolean flurry(LocalPlayer player) {
        if (!ready() || !canPay(player, "flurryPowerCost")) {
            return false;
        }
        begin(SwordMove.FLURRY);
        return true;
    }

    /** The attack button let go: a flurry that was still going ends there and then. */
    public static void stopFlurry() {
        Own mine = own;
        if (mine != null && mine.move == SwordMove.FLURRY && now(0.0F) - mine.start < SwordMove.FLURRY.ticks()) {
            mine.start = now(0.0F) - SwordMove.FLURRY.ticks();
        }
    }

    /**
     * The defend button held: the shield comes up to block ({@code up}), or goes back down. True when that changed
     * anything.
     */
    public static boolean block(boolean up) {
        Own mine = own;
        if (mine == null || mine.broke >= 0.0F || mine.charging || mine.blocking == up) {
            return false;
        }
        mine.blocking = up;
        mine.blockSince = now(0.0F);
        return true;
    }

    /** The defend button clicked: the charge, on your feet, as long as you are free and the ring can pay. */
    public static boolean charge(LocalPlayer player) {
        if (!ready() || own.blocking || ClientRing.flight(player, 0.0F) >= 0.0F
                || !canPay(player, "chargePowerCost")) {
            return false;
        }
        begin(SwordMove.CHARGE);
        own.charging = true;
        own.charged = 0;
        own.ram = Double.NaN;
        Vec3 look = player.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0.0, look.z);
        own.way = flat.lengthSqr() < 1.0E-6 ? new Vec3(0.0, 0.0, 1.0) : flat.normalize();
        return true;
    }

    /** The charge ends: you slam the shield into the ground. True when a charge was going. */
    public static boolean stopCharge() {
        Own mine = own;
        if (mine == null || !mine.charging) {
            return false;
        }
        mine.charging = false;
        begin(SwordMove.SLAM);
        return true;
    }

    /** True unless the ring has too little power for this setting of the sword and shield. */
    private static boolean canPay(LocalPlayer player, String cost) {
        return ClientRing.power(player) + 1.0E-4F >= wheel().value(cost);
    }

    /** The settings of the sword and shield: those of the Construct Wheel. */
    private static CharacterAbility wheel() {
        return GameCharacter.GREEN_LANTERN.byName("construct_wheel");
    }

    /**
     * You picked something on the construct wheel: the sword and shield take shape, or break up. Taken out again while
     * the last ones still break up, those fly apart to the end where they were.
     */
    public static void picked(Construct construct) {
        float now = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (construct == Construct.SWORD_SHIELD) {
            if (own == null || own.broke >= 0.0F) {
                if (own != null && drawn != null && now - own.broke < SwordShield.BREAK_TICKS) {
                    shards = drawn;
                    shardsSince = own.broke;
                }
                own = new Own();
                own.start = now;
                own.taken = now;
            }
        } else if (own != null && own.broke < 0.0F) {
            own.broke = now;
            own.charging = false;
            own.blocking = false;
        }
    }

    /** They are gone at once, without breaking up: you are no longer Green Lantern, or left the world. */
    public static void forget() {
        if (own != null && looked != null) {
            // Your eyes still come back from following the sword, the way they do when it breaks up.
            lookGone = looked;
            lookGoneAt = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        }
        own = null;
        looked = null;
        shards = null;
        drawn = null;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        Own mine = own;
        LocalPlayer player = minecraft.player;
        if (mine != null && player != null) {
            float now = now(0.0F);
            ClientConstructs.Sword told = ClientConstructs.sword(player.getId(), 0.0F);
            if (mine.broke >= 0.0F && now - mine.broke >= LOWER_FROM + LOWER_TICKS) {
                // Broken up, and the arms are back where the game holds them: from here on they are the game's again.
                own = null;
                drawn = null;
            } else if (told == null && now - mine.taken > 30.0F && mine.broke < 0.0F) {
                // The server never made them (you cannot hold them right now): they are not there.
                own = null;
            } else if (told != null && told.broken() >= 0.0F && mine.broke < 0.0F) {
                // The server let them break up (the ring gave out): so do they here.
                mine.broke = now;
                mine.charging = false;
                mine.blocking = false;
            } else if (told != null) {
                follow(mine, told, now);
            }
            if (own != null && mine.broke < 0.0F && mine.move != SwordMove.EQUIP) {
                feel(player, mine, now);
            }
        }
        if (shards != null && now(0.0F) - shardsSince >= SwordShield.BREAK_TICKS) {
            shards = null;
        }
        BLENDS.keySet().removeIf(id -> minecraft.level.getEntity(id) == null);
    }

    /**
     * Every frame while you take them out: what goes with each moment of it happens on its own frame, not a tick later
     * (the server's sounds of it are for everyone else): the strap of the shield closing, the flick and the whir of the
     * sword in the air, the catch, the gleam and the bangs, and the jolt of your view with the catch and the bangs.
     */
    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Own mine = own;
        LocalPlayer player = minecraft.player;
        if (mine == null || player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        if (mine.move != SwordMove.EQUIP || mine.broke >= 0.0F) {
            mine.heard = Float.POSITIVE_INFINITY;
            return;
        }
        float t = now(event.getPartialTick().getGameTimeDeltaPartialTick(false)) - mine.start;
        float was = mine.heard;
        mine.heard = t;
        if (!(t > was)) {
            return;
        }
        SwordShield.equipSounds(was, t, (sound, volume, pitch) -> minecraft.level.playLocalSound(player.getX(),
                player.getY() + 1.0, player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false));
        for (int beat : new int[] { SwordMove.CATCH, SwordMove.KNOCK, SwordMove.KNOCK_AGAIN }) {
            if (was < beat && t >= beat) {
                float hard = beat == SwordMove.CATCH ? 0.25F : beat == SwordMove.KNOCK ? 0.7F : 0.45F;
                kick(player, mine.start + beat, hard);
            }
        }
    }

    /**
     * What the server tells that your own game cannot know by itself: a ram it threw at something in the way of your
     * charge (played from where it is by now), and a shield it let drop because the ring could not hold it up.
     */
    private static void follow(Own mine, ClientConstructs.Sword told, float now) {
        SwordMove move = SwordMove.sent(told.move());
        double into = told.clock() - told.moveStart();
        if (mine.charging && move != null && move.kind() == SwordMove.Kind.BASH && into < move.ticks()
                && told.moveStart() != mine.ram) {
            mine.ram = told.moveStart();
            mine.move = move;
            mine.start = now - (float) Math.max(0.0, into);
            mine.felt = -1;
        }
        if (mine.blocking && (told.move() & SwordMove.BLOCKING) == 0 && now - mine.blockSince > 10.0F) {
            mine.blocking = false;
        }
    }

    /**
     * A blow of your own lands (a cut or thrust, a stab of the flurry, a ram, the slam): your view is jolted a little,
     * from the moment it landed. (The catch and the bangs of taking them out are felt on their own frame, see
     * {@link #onFrame}.)
     */
    private static void feel(LocalPlayer player, Own mine, float now) {
        int t = (int) Math.floor(now - mine.start);
        if (t == mine.felt) {
            return;
        }
        boolean blow = switch (mine.move.kind()) {
            case ATTACK, BASH, SLAM -> contains(mine.move.hits(), t);
            case FLURRY -> t >= SwordMove.FIRST_STAB && (t - SwordMove.FIRST_STAB) % SwordMove.STAB_EVERY == 0
                    && (t - SwordMove.FIRST_STAB) / SwordMove.STAB_EVERY < SwordMove.STABS;
            case EQUIP, CHARGE -> false;
        };
        if (!blow) {
            return;
        }
        mine.felt = t;
        kick(player, mine.start + t, switch (mine.move.kind()) {
            case SLAM -> 2.0F;
            case BASH -> 1.3F;
            case FLURRY -> 0.45F;
            default -> mine.move == SwordMove.OVERHEAD || mine.move == SwordMove.LUNGE ? 1.4F : 1.0F;
        });
    }

    /** Your view jolts from a blow that landed at {@code at} (client ticks), rolled the way the blade swept then. */
    private static void kick(LocalPlayer player, float at, float hard) {
        Blend blend = BLENDS.get(player.getId());
        double across = 1.0;
        if (blend != null && blend.move != null) {
            SwordPoses.Pose then = curve(blend, at - blend.start, at);
            SwordPoses.Pose was = curve(blend, at - 1.0F - blend.start, at - 1.0F);
            across = then.hand().add(then.blade()).x - was.hand().add(was.blade()).x;
        }
        kickAt = at;
        kickRoll = across < 0.0 ? -1.0F : 1.0F;
        kickHard = hard;
    }

    private static boolean contains(int[] ticks, int t) {
        for (int tick : ticks) {
            if (tick == t) {
                return true;
            }
        }
        return false;
    }

    /**
     * Your own view in first person: while you take them out your eyes follow the sword (up after it as it flies, at
     * the blade as you look it over, down at the shield as you bang it), and a blow that landed jolts it. Only while
     * the sword and shield are drawn in your hands (see {@link #handsFree}), easing in and out as they come and go
     * there; gone at once, your eyes still come back over a moment. Never over the top or under your feet.
     */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getCamera().getEntity() != player || event.getCamera().isDetached()) {
            looked = null;
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float now = now(partialTick);
        float step = Float.isNaN(shownAt) ? 1.0F : Mth.clamp(now - shownAt, 0.0F, LOOK_BACK) / LOOK_BACK;
        shownAt = now;
        shown = own != null && handsFree(player) ? Math.min(1.0F, shown + step) : Math.max(0.0F, shown - step);
        if (own == null) {
            looked = null;
            float[] gone = lookGone(now);
            if (gone != null) {
                turn(event, gone);
            }
            return;
        }
        float[] look = ownLook(player, partialTick);
        looked = look;
        if (look != null) {
            turn(event, look);
        }
        float since = now - kickAt;
        if (since < 0.0F || since >= KICK_TICKS) {
            return;
        }
        float fade = 1.0F - since / KICK_TICKS;
        float kick = kickHard * fade * fade * ClientSettings.cameraShake() * (float) Ease.smooth(shown);
        event.setRoll(event.getRoll() + KICK_ROLL * kickRoll * kick);
        event.setPitch(Mth.clamp(event.getPitch() + KICK_DIP * kick, -90.0F, 90.0F));
    }

    /** Turns the view up and to the right by {@code look} (radians), never over the top or under your feet. */
    private static void turn(ViewportEvent.ComputeCameraAngles event, float[] look) {
        event.setPitch(event.getPitch() - lookUp(event.getPitch(), look[0]) * Mth.RAD_TO_DEG);
        event.setYaw(event.getYaw() + look[1] * Mth.RAD_TO_DEG);
    }

    /**
     * How far (radians) the view looking {@code pitch} (degrees, the game's own: up below 0) really turns up to follow
     * the sword {@code up} radians: never past straight up or straight down.
     */
    private static float lookUp(float pitch, float up) {
        return (pitch - Mth.clamp(pitch - up * Mth.RAD_TO_DEG, -90.0F, 90.0F)) * Mth.DEG_TO_RAD;
    }

    /**
     * How far your eyes still follow the sword and shield that were gone at once (see {@link #forget}): coming back
     * over a moment, the way they do when they break up. Null once they are back.
     */
    @Nullable
    private static float[] lookGone(float now) {
        float[] gone = lookGone;
        if (gone == null) {
            return null;
        }
        float back = 1.0F - (float) Ease.smooth((now - lookGoneAt) / LOOK_BACK);
        if (back <= 0.0F) {
            lookGone = null;
            return null;
        }
        return new float[] { gone[0] * back, gone[1] * back };
    }

    /**
     * True while your own sword and shield can be drawn in your hands in first person: nothing else is in them, and you
     * are not invisible.
     */
    private static boolean handsFree(LocalPlayer player) {
        return !player.isInvisible() && player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty();
    }

    /**
     * How far your own eyes follow the sword right now, as a turn up and a turn to the right (radians), or null when
     * they do not: as far as your camera shake setting lets them (never more than the mod makes it), and only while
     * the sword and shield are drawn in your hands (see {@link #onCameraAngles}).
     */
    @Nullable
    private static float[] ownLook(LocalPlayer player, float partialTick) {
        State state = state(player, partialTick);
        float feel = Math.min(1.0F, ClientSettings.cameraShake());
        if (state == null || feel <= 0.0F) {
            return null;
        }
        // Posed first: that is where a new move is noticed.
        SwordPoses.Pose posed = pose(player, state, partialTick);
        Blend blend = BLENDS.get(player.getId());
        float now = now(partialTick);
        float[] watching = watching(blend, state, now);
        if (watching == null) {
            return null;
        }
        float t = watching[0];
        SwordPoses.Pose pose = state.move() == SwordMove.EQUIP ? posed : taking(t, now);
        float[] look = SwordPoses.look(t, pose);
        float amount = watching[1] * feel * (float) Ease.smooth(shown);
        return new float[] { look[0] * amount, look[1] * amount };
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        own = null;
        shown = 0.0F;
        shownAt = Float.NaN;
        looked = null;
        lookGone = null;
        shards = null;
        drawn = null;
        BLENDS.clear();
        SPUN.clear();
        SwordSpot.clear();
    }

    /**
     * You went to another world (or respawned): the server let go of your sword and shield, which never come along,
     * so your own hands are empty again too.
     */
    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        ConstructChoice.forget();
    }

    // ---- The charge: your own game runs you ----

    /** While you charge, your keys do not steer you: you run straight ahead. */
    @SubscribeEvent
    public static void onInput(MovementInputUpdateEvent event) {
        Own mine = own;
        if (mine == null || !mine.charging || event.getEntity() != Minecraft.getInstance().player) {
            return;
        }
        Input input = event.getInput();
        input.forwardImpulse = 0.0F;
        input.leftImpulse = 0.0F;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Pre event) {
        Own mine = own;
        if (mine == null || !mine.charging || !(event.getEntity() instanceof LocalPlayer player)
                || player != Minecraft.getInstance().player) {
            return;
        }
        double speed = wheel().value("chargeSpeed") / 20.0;
        // It takes a moment to get going.
        double going = Math.min(1.0, (mine.charged + 1) / 3.0);
        Vec3 moving = player.getDeltaMovement();
        player.setDeltaMovement(mine.way.x * speed * going, moving.y, mine.way.z * speed * going);
        player.setSprinting(true);
    }

    /** Running into a wall, or running out of time, ends the charge: you slam the shield down, and tell the server. */
    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        Own mine = own;
        if (mine == null || !mine.charging || !(event.getEntity() instanceof LocalPlayer player)
                || player != Minecraft.getInstance().player) {
            return;
        }
        mine.charged++;
        if (mine.charged > 2 && player.horizontalCollision || mine.charged >= wheel().value("chargeSeconds") * 20.0) {
            endCharge();
        }
    }

    /** Your charge ends here and now (a wall, its time, or a second click): the slam, and the server is told. */
    public static void endCharge() {
        if (stopCharge()) {
            CharacterAbility shield = GameCharacter.GREEN_LANTERN.byName("light_shield");
            if (shield != null) {
                PacketDistributor.sendToServer(new AbilityActionPayload(shield.slot().ordinal(), false,
                        Characters.WALL));
            }
        }
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        if (own != null && own.charging) {
            event.setNewFovModifier(event.getNewFovModifier() * CHARGE_FOV);
        }
    }

    // ---- Seen from outside ----

    /** True while this body's pose is the sword and shield's (for the pose of the arms, see LanternArms). */
    static boolean posing(Entity player, float partialTick) {
        return state(player, partialTick) != null;
    }

    /**
     * Just before a body is drawn: for the spinning cut the whole body is turned round (about the upright line through its
     * feet) as far as the cut has spun it. Turned back once it is drawn (see {@link #unspin}).
     */
    static void spin(RenderPlayerEvent.Pre event) {
        State state = state(event.getEntity(), event.getPartialTick());
        if (state == null) {
            return;
        }
        float orbit = pose(event.getEntity(), state, event.getPartialTick()).orbit();
        if (Math.abs(orbit) < 1.0E-3F) {
            return;
        }
        SPUN.put(event.getEntity().getId(), orbit);
    }

    /**
     * While the game draws the model of the player being drawn (see PlayerRendererMixin): the body turns round for the
     * spinning cut. Only the model: his name over his head, drawn after it, stays where it is.
     */
    static void turnModel(AbstractClientPlayer player, PoseStack pose) {
        Float orbit = SPUN.get(player.getId());
        if (orbit != null) {
            pose.mulPose(Axis.YP.rotation(orbit));
        }
    }

    /** A body turned round for the spinning cut is done with once it is drawn. */
    static void unspin(RenderPlayerEvent.Post event) {
        SPUN.remove(event.getEntity().getId());
    }

    /**
     * One arm while the game poses the model: pointed at where the pose puts its hand (the sword hand, or the middle of
     * the shield), seen from its shoulder on the upper body as that turns into the move, and before the game bends the
     * upper body forward (see {@link #lean}), which it then goes along with. As the sword and shield take shape the
     * arms come out of the game's own pose, and once they have broken up they go back into it.
     */
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        SwordPoses.Pose pose = pose(entity, state, partialTick);
        float ours = 1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F);
        if (ours <= 0.0F) {
            return;
        }
        boolean right = arm == HumanoidArm.RIGHT;
        float twist = pose.twist();
        float[] aim = SwordPoses.aim(pose, right);
        ModelPart limb = right ? model.rightArm : model.leftArm;
        float sway = limb.xRot * 0.15F;
        limb.xRot = Mth.lerp(ours, limb.xRot, aim[0] + sway);
        limb.yRot = Mth.lerp(ours, limb.yRot, aim[1]);
        limb.zRot = Mth.lerp(ours, limb.zRot, 0.0F);
        model.body.yRot = twist * ours;
        model.rightArm.z = Mth.sin(twist) * 5.0F * ours;
        model.rightArm.x = Mth.lerp(ours, -5.0F, -Mth.cos(twist) * 5.0F);
        model.leftArm.z = -Mth.sin(twist) * 5.0F * ours;
        model.leftArm.x = Mth.lerp(ours, 5.0F, Mth.cos(twist) * 5.0F);
    }

    /**
     * Once the game has posed the whole body: the upper body bends forward into the move (or back a little), the arms
     * and head going with it, the hips going back and the legs stepping into a lunge; the layers of the skin follow.
     * While he takes them out his head follows the sword.
     */
    public static void lean(PlayerModel<?> model, LivingEntity entity) {
        if (ownArms) {
            return;
        }
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        SwordPoses.Pose pose = pose(entity, state, partialTick);
        float[] watching = watching(BLENDS.get(entity.getId()), state, now(partialTick));
        if (watching != null) {
            float t = watching[0];
            float[] head = SwordPoses.head(t, state.move() == SwordMove.EQUIP ? pose : taking(t, now(partialTick)));
            float follow = head[2] * watching[1] * (1.0F - Mth.clamp(pose.rest(), 0.0F, 1.0F));
            model.head.xRot = Mth.lerp(follow, model.head.xRot, head[0]);
            model.head.yRot = Mth.lerp(follow, model.head.yRot, head[1]);
        }
        float lean = Mth.clamp(pose.lean(), -0.35F, 1.0F);
        float down = Math.max(0.0F, lean);
        float tilt = SwordPoses.TILT * lean;
        model.body.xRot += tilt;
        model.body.y += 3.2F * down;
        model.head.y += 4.2F * down;
        model.rightArm.y += 3.2F * down;
        model.leftArm.y += 3.2F * down;
        model.rightArm.xRot += tilt;
        model.leftArm.xRot += tilt;
        model.rightLeg.z += 3.9F * down;
        model.leftLeg.z += 3.9F * down;
        model.rightLeg.y += 0.2F * down;
        model.leftLeg.y += 0.2F * down;
        float step = Mth.clamp(pose.step(), 0.0F, 1.0F);
        if (step > 0.0F) {
            model.rightLeg.xRot = Mth.lerp(step, model.rightLeg.xRot, -0.7F);
            model.leftLeg.xRot = Mth.lerp(step, model.leftLeg.xRot, 0.5F);
        }
        model.hat.copyFrom(model.head);
        model.jacket.copyFrom(model.body);
        model.rightSleeve.copyFrom(model.rightArm);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightPants.copyFrom(model.rightLeg);
        model.leftPants.copyFrom(model.leftLeg);
    }

    /**
     * The sword and shield of someone seen from outside (you too, from behind), where his hands were drawn: the sword
     * in his fist (or flying over his head, tossed up), the shield on his forearm, the streak of a swung blade, the
     * sparks of a bang on the rim, and the light of a slam. While they take shape the ring feeds the growing shield a
     * beam of its light.
     */
    public static void draw(LanternPainter painter, Entity player, @Nullable Vec3 ring, float partialTick) {
        State state = state(player, partialTick);
        SwordSpot.Spot spot = SwordSpot.of(player);
        if (state == null || spot == null) {
            return;
        }
        SwordPoses.Pose pose = leading(player, state, partialTick);
        double apart = apart(state);
        if (apart >= 1.0) {
            return;
        }
        Vec3 bladeWay = SwordPoses.way(pose.blade());
        Vec3 edgeWay = SwordPoses.way(pose.edge());
        Blend blend = BLENDS.get(player.getId());
        float now = now(partialTick);
        float[] knock = watching(blend, state, now);
        if (knock != null) {
            // Brought down right onto the rim of the shield at each bang (and off it over a moment when a move cuts
            // that short).
            Vec3 turn = SwordPoses.bodyKnockTurn(knock[0]).scale(knock[1]);
            bladeWay = SwordPoses.turnedBy(bladeWay, turn);
            edgeWay = SwordPoses.turnedBy(edgeWay, turn);
        }
        Vec3 forward = spot.world(bladeWay, 0.0F);
        Vec3 edge = spot.world(edgeWay, 0.0F);
        float onto = knock == null ? 0.0F : SwordPoses.bodyKnocking(knock[0]) * knock[1];
        if (onto > 0.0F) {
            // His arms are drawn a little off from where the pose works them out (the game's own swing of the arms as
            // he walks, slim arms): the blade turns that much further, so it still comes down on the rim as drawn.
            Vec3 aim = spot.world(SwordPoses.bodyStrike(pose)[1], 0.0F);
            Vec3 off = spot.mount().subtract(spot.at(SwordPoses.drawn(pose, false)))
                    .subtract(spot.grip().subtract(spot.at(SwordPoses.drawn(pose, true))));
            Vec3 turn = SwordPoses.turnOnto(aim, aim.add(off.scale(onto)));
            forward = SwordPoses.turnedBy(forward, turn);
            edge = SwordPoses.turnedBy(edge, turn);
        }
        Vec3 grip = spot.grip();
        float flying = flying(state);
        if (flying >= 0.0F) {
            // Tossed up: it flies freely over his head and comes back down into his fist. The fist is drawn a little
            // off from where the pose works it out (the game's own swing of the arms as he walks): the flight takes
            // that along only where it leaves the fist and where it comes back into it.
            Vec3 off = spot.grip().subtract(spot.at(SwordPoses.drawn(pose, true)));
            Flying seen = at -> {
                SwordPoses.Flight flight = SwordPoses.bodyFlight(at);
                double u = Math.min(1.0, (at - SwordMove.TOSS) / (SwordMove.CATCH - SwordMove.TOSS));
                double along = Math.cos(Math.PI * u);
                return new SwordPoses.Flight(spot.at(flight.grip()).add(off.scale(along * along)),
                        spot.world(flight.blade(), 0.0F), spot.world(flight.edge(), 0.0F));
            };
            SwordPoses.Flight flight = seen.at(flying);
            if (apart <= 0.0) {
                tossTrail(painter, flying, SwordPoses.SWORD_SCALE, seen);
            }
            grip = flight.grip();
            forward = flight.blade();
            edge = flight.edge();
        }
        SwordPainter.sword(painter, grip, forward, edge, SwordPoses.SWORD_SCALE,
                SwordPoses.swordGrown(state.move(), formed(state)), apart);
        if (apart <= 0.0) {
            gleam(painter, state, grip, forward, SwordPoses.SWORD_SCALE);
        }
        Vec3 face = spot.world(SwordPoses.way(pose.face()), 0.0F);
        Vec3 top = spot.world(SwordPoses.way(pose.top()), 0.0F);
        Vec3 middle = spot.mount().add(face.scale(SwordPoses.SHIELD_OUT));
        double shieldGrown = SwordPoses.shieldGrown(state.move(), formed(state));
        SwordPainter.shield(painter, middle, face, top, SwordPoses.SHIELD_SCALE, shieldGrown, apart);
        if (apart > 0.0) {
            return;
        }
        if (ring != null && shieldGrown < 1.0) {
            painter.beam(ring, middle, feeding(shieldGrown), 0.6);
        }
        clang(painter, state, middle, face, top, SwordPoses.SHIELD_SCALE);
        // Where the middle of his chest is: worked back from where his fist was drawn.
        Vec3 chest = spot.grip().subtract(spot.world(SwordPoses.body(pose.hand()), 0.0F));
        trail(painter, state, (ago, from) -> {
            SwordPoses.Pose at = earlier(blend, state, now, ago);
            float turn = pose.orbit() - at.orbit();
            return chest.add(spot.world(SwordPoses.body(at.hand()).add(SwordPoses.way(at.blade())
                    .scale(from * SwordPoses.SWORD_SCALE)), turn));
        });
        slamLight(painter, state, chest.add(spot.world(new Vec3(0.0, 0.0, 1.3), 0.0F)).subtract(0.0,
                chest.y - player.getY(), 0.0));
    }

    /** How far the sword and shield have broken up: 0 while whole, gone at 1. */
    private static double apart(State state) {
        return state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / SwordShield.BREAK_TICKS);
    }

    /**
     * How far into taking them out the tossed sword is flying right now (on past the catch when it broke up in the air:
     * its pieces fly on), or -1 while it is in the hand.
     */
    private static float flying(State state) {
        if (state.move() != SwordMove.EQUIP) {
            return -1.0F;
        }
        if (state.broken() < 0.0F) {
            return SwordPoses.tossed(SwordMove.EQUIP, state.t()) >= 0.0F ? state.t() : -1.0F;
        }
        float broke = state.t() - state.broken();
        return broke > SwordMove.TOSS && broke < SwordMove.CATCH ? state.t() : -1.0F;
    }

    /** How strongly the ring's beam feeds the shield taking shape: fully until its rim runs round, then dying down. */
    private static double feeding(double grown) {
        return 1.0 - Ease.smooth((grown - 0.55) / 0.45);
    }

    /** The sparks of the blade banged on the rim of the shield as they take shape, on the shield as it is drawn. */
    private static void clang(LanternPainter painter, State state, Vec3 middle, Vec3 face, Vec3 top, double scale) {
        if (state.move() != SwordMove.EQUIP || state.t() < SwordMove.KNOCK) {
            return;
        }
        Vec3 ahead = face.normalize();
        Vec3 up = SwordPoses.square(top, ahead);
        Vec3 right = ahead.cross(up).normalize();
        Vec3 at = SwordPoses.rim(middle, right, up, scale);
        SwordPainter.clang(painter, at, right, up, state.t() - SwordMove.KNOCK, scale);
        SwordPainter.clang(painter, at, right, up, state.t() - SwordMove.KNOCK_AGAIN, scale * 0.8);
    }

    /** Where a point along the blade was {@code ago} ticks back: {@code from} (in blocks at scale 1) out from the grip. */
    private interface Blade {
        Vec3 at(float ago, double from);
    }

    /** Where the tossed sword is {@code t} ticks into taking them out. */
    private interface Flying {
        SwordPoses.Flight at(float t);
    }

    /**
     * The blur of the tossed sword turning over in the air: a streak of light where its tip and blade were over the
     * last moments, strongest high in its flight.
     */
    private static void tossTrail(LanternPainter painter, float t, double scale, Flying flying) {
        double u = Mth.clamp((t - SwordMove.TOSS) / (SwordMove.CATCH - SwordMove.TOSS), 0.0, 1.0);
        double strength = 0.7 * Math.sin(u * Math.PI);
        if (strength <= 0.02) {
            return;
        }
        List<Vec3> tips = new ArrayList<>(TRAIL);
        List<Vec3> roots = new ArrayList<>(TRAIL);
        for (int k = 0; k < TRAIL; k++) {
            SwordPoses.Flight at = flying.at(Math.max(SwordMove.TOSS, t - k * TOSS_TRAIL_STEP));
            tips.add(at.grip().add(at.blade().scale(SwordPainter.TIP * scale)));
            roots.add(at.grip().add(at.blade().scale(0.5 * scale)));
        }
        SwordPainter.trail(painter, tips, roots, strength);
    }

    /**
     * As he looks the sword over, a gleam of light runs up its blade from the guard to the tip: a bright spark with a
     * glint along the blade round it.
     */
    private static void gleam(LanternPainter painter, State state, Vec3 grip, Vec3 blade, double scale) {
        float u = SwordPoses.gleam(state.move(), state.t());
        if (u < 0.0F) {
            return;
        }
        double shine = Math.sin(u * Math.PI);
        Vec3 way = blade.normalize();
        Vec3 at = grip.add(way.scale(Mth.lerp(u, 0.35, SwordPainter.TIP * 0.95) * scale));
        painter.flare(at, 0.22 * scale * (0.6 + 0.4 * shine), shine);
        painter.edge(at.subtract(way.scale(0.22 * scale)), at.add(way.scale(0.22 * scale)), 0.03 * scale, shine);
    }

    /**
     * The streak a swung blade leaves: while a cut or thrust whips through, a sheet of light between where the tip and
     * the root of the blade were over the last few moments.
     */
    private static void trail(LanternPainter painter, State state, Blade blade) {
        SwordMove move = state.move();
        if (move.kind() != SwordMove.Kind.ATTACK && move != SwordMove.FLURRY) {
            return;
        }
        int[] hits = move.hits();
        float first = move == SwordMove.FLURRY ? SwordMove.FIRST_STAB : hits[0];
        float last = move == SwordMove.FLURRY ? SwordMove.FIRST_STAB + (SwordMove.STABS - 1) * SwordMove.STAB_EVERY
                : hits[hits.length - 1];
        float t = state.t();
        if (t < first - 3.0F || t > last + 3.0F) {
            return;
        }
        double strength = 1.0 - Math.max(0.0, Math.abs(t - Mth.clamp(t, first - 1.5F, last + 0.8F)) / 1.8);
        List<Vec3> tips = new ArrayList<>(TRAIL);
        List<Vec3> roots = new ArrayList<>(TRAIL);
        for (int k = 0; k < TRAIL; k++) {
            float ago = Math.min(t, k * TRAIL_STEP);
            tips.add(blade.at(ago, SwordPainter.TIP));
            roots.add(blade.at(ago, 0.5));
        }
        SwordPainter.trail(painter, tips, roots, Mth.clamp(strength, 0.0, 1.0) * (move == SwordMove.FLURRY ? 0.6 : 1.0));
    }

    /** The end of a charge: rings of light running out over the ground from where the shield struck it. */
    private static void slamLight(LanternPainter painter, State state, Vec3 at) {
        if (state.move() != SwordMove.SLAM) {
            return;
        }
        float since = state.t() - SwordMove.SLAM.hits()[0];
        if (since < 0.0F || since > 14.0F) {
            return;
        }
        double radius = wheel().value("slamRadius");
        Vec3 east = new Vec3(1.0, 0.0, 0.0);
        Vec3 south = new Vec3(0.0, 0.0, 1.0);
        for (int k = 0; k < 2; k++) {
            double ring = since - k * 3.0;
            if (ring < 0.0) {
                continue;
            }
            double wave = 1.0 - Math.pow(1.0 - Math.min(1.0, ring / 9.0), 2.0);
            double fade = Math.max(0.0, 1.0 - ring / 11.0);
            painter.circle(at.add(0.0, 0.1, 0.0), east, south, 0.4 + radius * wave, 0.06, 0.45,
                    Colors.alpha(fade), Colors.alpha(0.5 * fade));
        }
        if (since < 3.0F) {
            painter.flare(at.add(0.0, 0.3, 0.0), 0.8 * (1.0 - since / 3.0F), 1.0 - since / 3.0F);
        }
    }

    // ---- First person ----

    /**
     * Your own two arms with the sword and the shield, instead of your empty hands: both drawn along with the main
     * hand, in the pose as it is made, turned round with the body for the spinning cut. As they take shape the right
     * arm comes out of the game's own resting hand (and goes back into it once they have broken up), and while your
     * eyes follow the sword the arms stay where they are: the view turns over them.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || own == null || !handsFree(player)) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        float partialTick = event.getPartialTick();
        State state = state(player, partialTick);
        if (state == null) {
            return;
        }
        SwordPoses.Pose made = leading(player, state, partialTick);
        float orbit = made.orbit();
        SwordPoses.Pose pose = made.turned(orbit);
        float rest = Mth.clamp(pose.rest(), 0.0F, 1.0F);
        PoseStack stack = event.getPoseStack();
        stack.pushPose();
        float[] look = ownLook(player, partialTick);
        if (look != null) {
            stack.mulPose(Axis.XP.rotation(-lookUp(player.getViewXRot(partialTick), look[0])));
            stack.mulPose(Axis.YP.rotation(look[1]));
        }
        MultiBufferSource buffers = event.getMultiBufferSource();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        Vector3f shoulder = shoulder(OWN_SHOULDER_RIGHT, made.hand().subtract(SwordPoses.GUARD.hand()), orbit)
                .lerp(RechargeAnimation.SHOULDER_RIGHT, rest);
        arm(stack, buffers, event.getPackedLight(), player, renderer, 1.0F, pose.hand(), shoulder, rest);
        if (rest < 1.0F) {
            Vec3 moved = made.shieldGrip(SwordPoses.OWN_SHIELD)
                    .subtract(SwordPoses.GUARD.shieldGrip(SwordPoses.OWN_SHIELD));
            arm(stack, buffers, event.getPackedLight(), player, renderer, -1.0F, pose.shieldGrip(SwordPoses.OWN_SHIELD),
                    shoulder(OWN_SHOULDER_LEFT, moved, orbit), 0.0F);
        }
        float time = player.tickCount + partialTick;
        LanternPainter painter = LanternPainter.hand(stack, time);
        float now = now(partialTick);
        Drawn old = shards;
        if (old != null) {
            double apart = Math.max(1.0E-3, (now - shardsSince) / SwordShield.BREAK_TICKS);
            SwordPainter.sword(painter, old.grip(), old.blade(), old.edge(), SwordPoses.OWN_SWORD, old.sword(), apart);
            SwordPainter.shield(painter, old.shield(), old.face(), old.top(), SwordPoses.OWN_SHIELD, old.shieldGrown(),
                    apart);
        }
        double apart = apart(state);
        if (apart < 1.0) {
            drawOwn(painter, player, state, pose, apart, now);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
        stack.popPose();
    }

    /**
     * Your own sword and shield in first person, in your hands (or the sword tossed up, flying before your eyes); while
     * they take shape the ring's beam feeds the shield, and banged on its rim the blade throws sparks.
     */
    private static void drawOwn(LanternPainter painter, LocalPlayer player, State state, SwordPoses.Pose pose,
            double apart, float now) {
        Vec3 grip = pose.hand();
        Vec3 blade = pose.blade();
        Vec3 edge = pose.edge();
        float flying = flying(state);
        if (flying >= 0.0F) {
            // Tossed up before your eyes: it turns over up there and drops back into your fist.
            SwordPoses.Flight flight = SwordPoses.flight(flying);
            if (apart <= 0.0) {
                tossTrail(painter, flying, SwordPoses.OWN_SWORD, SwordPoses::flight);
            }
            grip = flight.grip();
            blade = flight.blade();
            edge = flight.edge();
        }
        float swordGrown = SwordPoses.swordGrown(state.move(), formed(state));
        float shieldGrown = SwordPoses.shieldGrown(state.move(), formed(state));
        SwordPainter.sword(painter, grip, blade, edge, SwordPoses.OWN_SWORD, swordGrown, apart);
        SwordPainter.shield(painter, pose.shield(), pose.face(), pose.top(), SwordPoses.OWN_SHIELD, shieldGrown, apart);
        if (apart > 0.0) {
            return;
        }
        drawn = new Drawn(grip, blade, edge, pose.shield(), pose.face(), pose.top(), swordGrown, shieldGrown);
        gleam(painter, state, grip, blade, SwordPoses.OWN_SWORD);
        if (shieldGrown < 1.0F) {
            // The ring on your fist feeds the shield growing on your other arm a beam of its light.
            painter.beam(pose.hand(), pose.shield().add(pose.face().scale(0.06 * SwordPoses.OWN_SHIELD)),
                    feeding(shieldGrown), OWN_BEAM);
        }
        clang(painter, state, pose.shield(), pose.face(), pose.top(), SwordPoses.OWN_SHIELD);
        Blend blend = BLENDS.get(player.getId());
        trail(painter, state, (ago, from) -> {
            SwordPoses.Pose at = earlier(blend, state, now, ago);
            SwordPoses.Pose seen = at.turned(at.orbit());
            return seen.hand().add(seen.blade().scale(from * SwordPoses.OWN_SWORD));
        });
    }

    /**
     * Your own sword and shield seen from outside before the server has told of them (that takes a moment): drawn from
     * your own game, so you see them take shape from the very start. Once the server tells of them they are drawn with
     * every other construct.
     */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Camera camera = event.getCamera();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || own == null || player == null
                || minecraft.level == null || camera.getEntity() == player && !camera.isDetached()
                || ClientConstructs.sword(player.getId(), 0.0F) != null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = (float) (minecraft.level.getGameTime() % 24000L) + partialTick;
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(), time,
                event.getFrustum());
        draw(painter, player, RingSpot.of(player, camera, event.getProjectionMatrix(), event.getModelViewMatrix()),
                partialTick);
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    /**
     * One of your own arms in first person, drawn slim: thinner than it is, and as long as it takes to run from its fist
     * towards {@code from} out of sight; {@code rest} of the way (0 to 1) it is the game's own arm just as it is.
     */
    private static void arm(PoseStack stack, MultiBufferSource buffers, int light, LocalPlayer player,
            PlayerRenderer renderer, float side, Vec3 hand, Vector3f from, float rest) {
        Vec3 back = new Vec3(from.x - hand.x, from.y - hand.y, from.z - hand.z);
        if (back.lengthSqr() < 1.0E-6) {
            return;
        }
        back = back.normalize();
        float length = Mth.lerp(rest, (float) Mth.clamp((outOfSight(hand, back) + OWN_ARM_PAST) / ARM_BACK,
                OWN_ARM_SHORTEST, OWN_ARM_LONGEST), 1.0F);
        float thick = Mth.lerp(rest, OWN_ARM, 1.0F);
        Quaternionf along = new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), vector(back));
        stack.pushPose();
        stack.translate(hand.x, hand.y, hand.z);
        stack.mulPose(along);
        stack.scale(thick, thick, length);
        stack.mulPose(new Quaternionf(along).conjugate());
        stack.translate(-hand.x, -hand.y, -hand.z);
        // The game poses the arm afresh before it draws it: the body bending into a move must not shift it.
        ownArms = true;
        try {
            RechargeAnimation.arm(stack, buffers, light, player, renderer, side, vector(hand), from);
        } finally {
            ownArms = false;
        }
        stack.popPose();
    }

    /**
     * How far a line from {@code at} before your eyes (in first person) runs along {@code way} before you no longer see
     * it: out over an edge of the screen, or past your eyes; 0 if you do not see {@code at} to begin with.
     */
    private static double outOfSight(Vec3 at, Vec3 way) {
        Minecraft minecraft = Minecraft.getInstance();
        double up = Math.tan(HAND_FOV * 0.5 * Mth.DEG_TO_RAD);
        double across = up * minecraft.getWindow().getWidth() / Math.max(1, minecraft.getWindow().getHeight());
        double out = past(-at.y + at.z * up, -way.y + way.z * up);
        out = Math.min(out, past(at.y + at.z * up, way.y + way.z * up));
        out = Math.min(out, past(at.x + at.z * across, way.x + way.z * across));
        out = Math.min(out, past(-at.x + at.z * across, -way.x + way.z * across));
        out = Math.min(out, past(at.z, way.z));
        return out == Double.MAX_VALUE ? 0.0 : out;
    }

    /** Where {@code a + b t} first comes above 0: at once if it already is, never (MAX_VALUE) if it never does. */
    private static double past(double a, double b) {
        if (a >= 0.0) {
            return 0.0;
        }
        return b > 0.0 ? -a / b : Double.MAX_VALUE;
    }

    /**
     * Where one of your own arms reaches in from in first person: below a bottom corner of the screen, following its hand
     * a little as that moves from the guard, and going round with the body for the spinning cut.
     */
    private static Vector3f shoulder(Vec3 rest, Vec3 moved, float orbit) {
        return vector(SwordPoses.spin(rest.add(moved.scale(OWN_FOLLOW)), orbit));
    }

    private static Vector3f vector(Vec3 at) {
        return new Vector3f((float) at.x, (float) at.y, (float) at.z);
    }
}
