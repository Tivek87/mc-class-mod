package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
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
import net.neoforged.neoforge.client.event.RenderHandEvent;
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
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.client.render.SwordPainter;
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
 * else's play what the server tells. The rams of a charge are the server's to throw: yours play as it tells of them.</li>
 * <li>In first person you see the poses as they are made: your right fist round the grip of the sword, which stands
 * upright in the guard, and your left forearm on the back of the shield, the fist round its grip; both arms reach in
 * from below the edges of the screen. A blow that lands jolts your view a little.</li>
 * <li>Seen from outside, each arm points at where the pose puts its hand, and the upper body turns, bends and steps into
 * the move (the spinning cut turns the whole body round); the sword and shield hang where his hands were really drawn
 * (see {@link SwordSpot}).</li>
 * <li>While you charge behind the shield your own game runs you straight ahead, and stops you at a wall.</li>
 * </ul>
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SwordArms {
    // How big the sword and shield are on a body seen from outside, and in your own hands in first person.
    private static final double SWORD_SCALE = 0.9;
    private static final double SHIELD_SCALE = 0.76;
    private static final double OWN_SWORD = 0.74;
    private static final double OWN_SHIELD = 0.62;
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
    /**
     * Seen from outside, a place before your eyes in first person is a place before the body, measured from the middle of
     * the chest at the height of the shoulders (x to his right, y up, z ahead): across and up it is squeezed and raised
     * this much, and ahead it is this much of the depth, less this much.
     */
    private static final double BODY_ACROSS = 0.65;
    private static final double BODY_UP = 1.12;
    private static final double BODY_RAISE = 0.16;
    private static final double BODY_AHEAD = 0.9;
    private static final double BODY_BACK = 0.46;
    // How far the shoulders are from the middle of the body, in blocks.
    private static final double SHOULDER = 0.31;
    // How far the shield sits out in front of the forearm seen from outside.
    private static final double SHIELD_OUT = 0.07;
    // How far the upper body bends forward at a lean of 1, in radians, the way it does when crouching.
    private static final float TILT = 0.5F;
    // How many earlier moments the streak of a swung blade reaches back over, and how far apart they are, in ticks.
    private static final int TRAIL = 10;
    private static final float TRAIL_STEP = 0.45F;
    // How long ago the blade is looked back at to tell which way it sweeps (it leads with its edge), in ticks.
    private static final float LEAD_STEP = 0.35F;
    // How long the shield takes to come up to block, or to go back down, in ticks.
    private static final float BLOCK_TICKS = 1.8F;
    // A charge widens your view this much.
    private static final float CHARGE_FOV = 1.12F;
    // A blow that lands jolts your own view: for this many ticks, rolled this far with the blow and dipped this far, in
    // degrees; a slam of the shield twice as hard.
    private static final float KICK_TICKS = 5.0F;
    private static final float KICK_ROLL = 1.6F;
    private static final float KICK_DIP = 0.8F;

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
    }

    @Nullable
    private static Own own;
    // The jolt of the last blow that landed: when (client ticks), and which way it rolls and how hard.
    private static float kickAt = -100.0F;
    private static float kickRoll;
    private static float kickHard;

    /** What one body's arms did last, to come in from when a new move starts, and how far its shield is up. */
    private static final class Blend {
        @Nullable
        SwordMove move;
        float start = Float.NaN;
        SwordPoses.Pose from = SwordPoses.GUARD;
        SwordPoses.Pose now = SwordPoses.GUARD;
        float block;
        float blockAt = Float.NaN;
    }

    private static final Map<Integer, Blend> BLENDS = new HashMap<>();
    // The bodies turned round for the spinning cut while they are drawn, to turn back once they are.
    private static final Map<Integer, Boolean> SPUN = new HashMap<>();
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
            return null;
        }
        SwordMove move = SwordMove.sent(sword.move());
        return new State(move == null ? SwordMove.EQUIP : move, (float) (sword.clock() - sword.moveStart()),
                (float) sword.clock(), sword.broken(), sword.way(), (sword.move() & SwordMove.BLOCKING) != 0,
                (sword.move() & SwordMove.CHARGING) != 0);
    }

    /**
     * The pose of this player right now: the move, coming in from wherever the arms were as it began, with the shield held
     * up to block laid over it. Remembers it, so the next move can come in from here.
     */
    static SwordPoses.Pose pose(Entity player, State state, float partialTick) {
        Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new Blend());
        float now = now(partialTick);
        float start = now - state.t();
        if (blend.move != state.move() || Float.isNaN(blend.start) || Math.abs(blend.start - start) > 1.5F) {
            blend.from = blend.now.unwound();
            blend.move = state.move();
            blend.start = start;
        }
        float step = Float.isNaN(blend.blockAt) ? 1.0F : Mth.clamp(now - blend.blockAt, 0.0F, 5.0F) / BLOCK_TICKS;
        blend.blockAt = now;
        float want = state.blocking() && !state.charging() && state.broken() < 0.0F ? 1.0F : 0.0F;
        blend.block = want > blend.block ? Math.min(want, blend.block + step) : Math.max(want, blend.block - step);
        SwordPoses.Pose pose = earlier(blend, state, now, 0.0F);
        blend.now = pose;
        return pose;
    }

    /** The pose of a body {@code ago} ticks before now, from the move it is doing now (for streaks and edges). */
    private static SwordPoses.Pose earlier(Blend blend, State state, float now, float ago) {
        SwordPoses.Pose pose = SwordPoses.at(state.move(), Math.max(0.0F, state.t() - ago), now - ago, blend.from);
        return SwordPoses.block(pose, (float) Ease.smooth(blend.block));
    }

    /** The pose right now with the edge of the blade leading the way it sweeps (see {@link SwordPoses#led}). */
    private static SwordPoses.Pose leading(Entity player, State state, float partialTick) {
        SwordPoses.Pose pose = pose(player, state, partialTick);
        Blend blend = BLENDS.get(player.getId());
        return SwordPoses.led(pose, earlier(blend, state, now(partialTick), LEAD_STEP));
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

    /** You picked something on the construct wheel: the sword and shield take shape, or break up. */
    public static void picked(Construct construct) {
        float now = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (construct == Construct.SWORD_SHIELD) {
            if (own == null || own.broke >= 0.0F) {
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
        own = null;
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
            if (mine.broke >= 0.0F && now - mine.broke > SwordShield.BREAK_TICKS) {
                own = null;
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
            if (own != null && mine.broke < 0.0F) {
                feel(player, mine, now);
            }
        }
        BLENDS.keySet().removeIf(id -> minecraft.level.getEntity(id) == null);
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
     * A blow of your own lands (a cut or thrust, a stab of the flurry, a ram, the slam, a knock on the shield as they
     * take shape): your view is jolted a little, rolled the way the blade swept.
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
            case EQUIP -> t == SwordMove.KNOCK || t == SwordMove.KNOCK + 3;
            case CHARGE -> false;
        };
        if (!blow) {
            return;
        }
        mine.felt = t;
        Blend blend = BLENDS.get(player.getId());
        State state = state(player, 0.0F);
        if (blend == null || state == null) {
            return;
        }
        SwordPoses.Pose at = earlier(blend, state, now, 0.0F);
        SwordPoses.Pose was = earlier(blend, state, now, 1.0F);
        double across = at.hand().add(at.blade()).x - was.hand().add(was.blade()).x;
        kickAt = now;
        kickRoll = (float) Math.signum(across);
        kickHard = switch (mine.move.kind()) {
            case SLAM -> 2.0F;
            case BASH -> 1.3F;
            case FLURRY, EQUIP -> 0.45F;
            default -> mine.move == SwordMove.OVERHEAD || mine.move == SwordMove.LUNGE ? 1.4F : 1.0F;
        };
    }

    private static boolean contains(int[] ticks, int t) {
        for (int tick : ticks) {
            if (tick == t) {
                return true;
            }
        }
        return false;
    }

    /** The jolt of a blow that landed, on your own view in first person. */
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (own == null || minecraft.player == null || event.getCamera().getEntity() != minecraft.player
                || event.getCamera().isDetached()) {
            return;
        }
        float since = now((float) event.getPartialTick()) - kickAt;
        if (since < 0.0F || since >= KICK_TICKS) {
            return;
        }
        float fade = 1.0F - since / KICK_TICKS;
        float kick = kickHard * fade * fade;
        event.setRoll(event.getRoll() + KICK_ROLL * kickRoll * kick);
        event.setPitch(event.getPitch() + KICK_DIP * kick);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        own = null;
        BLENDS.clear();
        SPUN.clear();
        SwordSpot.clear();
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
        event.getPoseStack().pushPose();
        event.getPoseStack().mulPose(Axis.YP.rotation(orbit));
        SPUN.put(event.getEntity().getId(), Boolean.TRUE);
    }

    /** A body turned round for the spinning cut is turned back once it is drawn. */
    static void unspin(RenderPlayerEvent.Post event) {
        if (SPUN.remove(event.getEntity().getId()) != null) {
            event.getPoseStack().popPose();
        }
    }

    /**
     * One arm while the game poses the model: pointed at where the pose puts its hand (the sword hand, or the middle of
     * the shield), seen from its shoulder on the upper body as that turns into the move, and before the game bends the
     * upper body forward (see {@link #lean}), which it then goes along with.
     */
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        SwordPoses.Pose pose = pose(entity, state, partialTick);
        boolean right = arm == HumanoidArm.RIGHT;
        float twist = pose.twist();
        float tilt = TILT * Mth.clamp(pose.lean(), -0.35F, 1.0F);
        Vec3 target = unbent(unturned(body(right ? pose.hand() : pose.shield()), twist), tilt);
        Vec3 reach = target.subtract(right ? SHOULDER : -SHOULDER, 0.0, 0.0);
        reach = reach.lengthSqr() < 1.0E-6 ? new Vec3(0.0, -1.0, 0.0) : reach.normalize();
        float pitch = (float) Math.asin(Mth.clamp(reach.y, -1.0, 1.0));
        float yaw = (float) Mth.atan2(reach.x, reach.z);
        ModelPart limb = right ? model.rightArm : model.leftArm;
        float sway = limb.xRot * 0.15F;
        limb.xRot = -(Mth.HALF_PI + pitch) + sway;
        limb.yRot = yaw + twist;
        limb.zRot = 0.0F;
        model.body.yRot = twist;
        model.rightArm.z = Mth.sin(twist) * 5.0F;
        model.rightArm.x = -Mth.cos(twist) * 5.0F;
        model.leftArm.z = -Mth.sin(twist) * 5.0F;
        model.leftArm.x = Mth.cos(twist) * 5.0F;
    }

    /**
     * Once the game has posed the whole body: the upper body bends forward into the move (or back a little), the arms and
     * head going with it, the hips going back and the legs stepping into a lunge; the layers of the skin follow.
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
        float lean = Mth.clamp(pose.lean(), -0.35F, 1.0F);
        float down = Math.max(0.0F, lean);
        float tilt = TILT * lean;
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
     * The sword and shield of someone seen from outside (you too, from behind), where his hands were drawn: the sword in
     * his fist, the shield on his forearm, the streak of a swung blade, and the light of a slam. While they take shape the
     * ring feeds the growing shield a beam of its light.
     */
    public static void draw(LanternPainter painter, Entity player, @Nullable Vec3 ring, float partialTick) {
        State state = state(player, partialTick);
        SwordSpot.Spot spot = SwordSpot.of(player);
        if (state == null || spot == null) {
            return;
        }
        SwordPoses.Pose pose = leading(player, state, partialTick);
        double apart = state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / SwordShield.BREAK_TICKS);
        Vec3 forward = spot.world(way(pose.blade()), 0.0F);
        Vec3 edge = spot.world(way(pose.edge()), 0.0F);
        SwordPainter.sword(painter, spot.grip(), forward, edge, SWORD_SCALE,
                SwordPoses.swordGrown(state.move(), state.age()), apart);
        Vec3 face = spot.world(way(pose.face()), 0.0F);
        Vec3 top = spot.world(way(pose.top()), 0.0F);
        Vec3 middle = spot.mount().add(face.scale(SHIELD_OUT));
        double shieldGrown = SwordPoses.shieldGrown(state.move(), state.age());
        SwordPainter.shield(painter, middle, face, top, SHIELD_SCALE, shieldGrown, apart);
        if (apart > 0.0) {
            return;
        }
        if (ring != null && shieldGrown < 1.0) {
            painter.beam(ring, middle, 1.0 - shieldGrown * 0.6, 0.6);
        }
        // Where the middle of his chest is: worked back from where his fist was drawn.
        Vec3 chest = spot.grip().subtract(spot.world(body(pose.hand()), 0.0F));
        Blend blend = BLENDS.get(player.getId());
        float now = now(partialTick);
        trail(painter, state, (ago, from) -> {
            SwordPoses.Pose at = earlier(blend, state, now, ago);
            float turn = pose.orbit() - at.orbit();
            return chest.add(spot.world(body(at.hand()).add(way(at.blade()).scale(from * SWORD_SCALE)), turn));
        });
        slamLight(painter, state, chest.add(spot.world(new Vec3(0.0, 0.0, 1.3), 0.0F)).subtract(0.0,
                chest.y - player.getY(), 0.0));
    }

    /** Where a point along the blade was {@code ago} ticks back: {@code from} (in blocks at scale 1) out from the grip. */
    private interface Blade {
        Vec3 at(float ago, double from);
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
     * Your own two arms with the sword and the shield, instead of your empty hands: both drawn along with the main hand,
     * in the pose as it is made, turned round with the body for the spinning cut.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || own == null || player.isInvisible() || !player.getMainHandItem().isEmpty()
                || !player.getOffhandItem().isEmpty()) {
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
        Vec3 grip = pose.hand();
        Vec3 shieldGrip = pose.shieldGrip(OWN_SHIELD);
        PoseStack stack = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        arm(stack, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, 1.0F, grip,
                shoulder(OWN_SHOULDER_RIGHT, made.hand().subtract(SwordPoses.GUARD.hand()), orbit));
        arm(stack, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, -1.0F, shieldGrip,
                shoulder(OWN_SHOULDER_LEFT, made.shieldGrip(OWN_SHIELD).subtract(SwordPoses.GUARD.shieldGrip(OWN_SHIELD)),
                        orbit));
        float time = player.tickCount + partialTick;
        LanternPainter painter = LanternPainter.hand(stack, time);
        double apart = state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / SwordShield.BREAK_TICKS);
        SwordPainter.sword(painter, grip, pose.blade(), pose.edge(), OWN_SWORD,
                SwordPoses.swordGrown(state.move(), state.age()), apart);
        SwordPainter.shield(painter, pose.shield(), pose.face(), pose.top(), OWN_SHIELD,
                SwordPoses.shieldGrown(state.move(), state.age()), apart);
        if (apart <= 0.0) {
            Blend blend = BLENDS.get(player.getId());
            float now = now(partialTick);
            trail(painter, state, (ago, from) -> {
                SwordPoses.Pose at = earlier(blend, state, now, ago);
                SwordPoses.Pose seen = at.turned(at.orbit());
                return seen.hand().add(seen.blade().scale(from * OWN_SWORD));
            });
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    /**
     * One of your own arms in first person, drawn slim: thinner than it is, and as long as it takes to run from its fist
     * towards {@code from} out of sight.
     */
    private static void arm(PoseStack stack, MultiBufferSource buffers, int light, LocalPlayer player,
            PlayerRenderer renderer, float side, Vec3 hand, Vector3f from) {
        Vec3 back = new Vec3(from.x - hand.x, from.y - hand.y, from.z - hand.z);
        if (back.lengthSqr() < 1.0E-6) {
            return;
        }
        back = back.normalize();
        float length = (float) Mth.clamp((outOfSight(hand, back) + OWN_ARM_PAST) / ARM_BACK, OWN_ARM_SHORTEST,
                OWN_ARM_LONGEST);
        Quaternionf along = new Quaternionf().rotationTo(new Vector3f(0.0F, 0.0F, 1.0F), vector(back));
        stack.pushPose();
        stack.translate(hand.x, hand.y, hand.z);
        stack.mulPose(along);
        stack.scale(OWN_ARM, OWN_ARM, length);
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

    // ---- From your own eyes to the body seen from outside ----

    /** A place before your eyes in first person as a place before the body (see {@link #BODY_ACROSS}). */
    private static Vec3 body(Vec3 view) {
        return new Vec3(view.x * BODY_ACROSS, view.y * BODY_UP + BODY_RAISE, -view.z * BODY_AHEAD - BODY_BACK);
    }

    /** A way before your eyes in first person as a way seen from the body (x to his right, y up, z ahead). */
    private static Vec3 way(Vec3 view) {
        return new Vec3(view.x, view.y, -view.z);
    }

    /** A place before the body as seen from the upper body turned {@code twist} to his right. */
    private static Vec3 unturned(Vec3 at, float twist) {
        double cos = Mth.cos(twist);
        double sin = Mth.sin(twist);
        return new Vec3(at.x * cos - at.z * sin, at.y, at.x * sin + at.z * cos);
    }

    /** A place before the upper body as it is before the game bends it forward by {@code tilt} (radians). */
    private static Vec3 unbent(Vec3 at, float tilt) {
        double cos = Mth.cos(tilt);
        double sin = Mth.sin(tilt);
        return new Vec3(at.x, at.y * cos + at.z * sin, -at.y * sin + at.z * cos);
    }
}
