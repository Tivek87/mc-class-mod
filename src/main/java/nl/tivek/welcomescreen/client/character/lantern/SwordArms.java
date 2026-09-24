package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
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
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.CharacterAbility;
import nl.tivek.welcomescreen.character.Characters;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.character.lantern.Construct;
import nl.tivek.welcomescreen.character.lantern.SwordMove;
import nl.tivek.welcomescreen.character.lantern.SwordShield;
import nl.tivek.welcomescreen.network.AbilityActionPayload;
import org.joml.Vector3f;

/**
 * The sword and shield of the construct wheel in the hands of every Green Lantern around you (see
 * {@link SwordShield}): which move each is doing and how far into it, the pose of his arms and body that goes with it
 * (see {@link SwordPoses}), and where the sword and shield are drawn.
 * <ul>
 * <li>Your own sword and shield play every move the moment you click, before the server has even heard of it; everyone
 * else's play what the server tells.</li>
 * <li>Seen from outside, his arms, his upper body and his legs are posed, and the sword and shield hang where his hands
 * were really drawn (see {@link SwordSpot}).</li>
 * <li>In first person your own two arms are drawn with the sword and the shield in your hands, moving through the very
 * same poses.</li>
 * <li>While you charge behind the shield your own game runs you straight ahead, and stops you at a wall.</li>
 * </ul>
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class SwordArms {
    // How big the sword and shield are on a body seen from outside, and in your own hands in first person.
    private static final double SWORD_SCALE = 0.95;
    private static final double SHIELD_SCALE = 0.92;
    private static final double OWN_SWORD = 0.6;
    private static final double OWN_SHIELD = 0.55;
    // First person: where your hands are while you stand in the guard, in blocks before your eyes (x to the right, y up,
    // -z ahead). Every other pose moves them from there as far as the pose moves them.
    private static final Vec3 OWN_RIGHT = new Vec3(0.56, -0.46, -0.95);
    private static final Vec3 OWN_LEFT = new Vec3(-0.58, -0.5, -0.92);
    // Your own arms in first person reach in from the game's shoulders out of view below, which follow a hand this much of
    // the way it moves.
    private static final double OWN_FOLLOW = 0.35;
    // To work out where a hand is from a pose: how far the shoulders are from the middle of the body, how long an arm is,
    // and how far a step of a lunge carries the body forward, in blocks.
    private static final double SHOULDER = 0.31;
    private static final double ARM = 0.6;
    private static final double STEP = 0.35;
    // How far the shield sits out in front of the forearm, and how high the sword is tossed while it takes shape.
    private static final double SHIELD_OUT = 0.06;
    private static final double TOSS_HIGH = 1.35;
    // How many earlier moments the streak of a swung blade reaches back over, and how far apart they are, in ticks.
    private static final int TRAIL = 8;
    private static final float TRAIL_STEP = 0.5F;
    // A charge widens your view this much.
    private static final float CHARGE_FOV = 1.12F;

    /** Your own sword and shield: played the moment you click, before the server has heard of it. */
    private static final class Own {
        SwordMove move = SwordMove.EQUIP;
        float start;
        float taken;
        float broke = -1.0F;
        boolean charging;
        int charged;
        Vec3 way = new Vec3(0.0, 0.0, 1.0);
        @Nullable
        SwordMove lastAttack;
        @Nullable
        SwordMove lastBash;
    }

    @Nullable
    private static Own own;

    /** What one body's arms did last, to come in from when a new move starts. */
    private static final class Blend {
        @Nullable
        SwordMove move;
        float start = Float.NaN;
        SwordPoses.Pose from = SwordPoses.GUARD;
        SwordPoses.Pose now = SwordPoses.GUARD;
    }

    private static final Map<Integer, Blend> BLENDS = new HashMap<>();
    private static int clientTicks;

    /**
     * Someone's sword and shield right now: the move, how many ticks into it, how many ticks ago they took shape, how many
     * ticks ago they began to break up (-1 while whole), and the way a charge runs.
     */
    record State(SwordMove move, float t, float age, float broken, Vec3 way) {
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
                    mine.way);
        }
        ClientConstructs.Sword sword = ClientConstructs.sword(player.getId(), partialTick);
        if (sword == null) {
            return null;
        }
        SwordMove move = SwordMove.byIndex(sword.move());
        return new State(move == null ? SwordMove.EQUIP : move, (float) (sword.clock() - sword.moveStart()),
                (float) sword.clock(), sword.broken(), sword.way());
    }

    /** The pose of this player's arms and body right now, coming in from the last one as a new move starts. */
    static SwordPoses.Pose pose(Entity player, State state, float partialTick) {
        Blend blend = BLENDS.computeIfAbsent(player.getId(), id -> new Blend());
        float now = now(partialTick);
        float start = now - state.t();
        if (blend.move != state.move() || Float.isNaN(blend.start) || Math.abs(blend.start - start) > 1.5F) {
            blend.from = blend.now.unwound();
            blend.move = state.move();
            blend.start = start;
        }
        SwordPoses.Pose target = SwordPoses.at(state.move(), state.t(), now);
        float in = SwordPoses.blendIn(state.move());
        SwordPoses.Pose pose = state.t() < in ? blend.from.mix(target, SwordPoses.smooth(state.t() / in)) : target;
        blend.now = pose;
        return pose;
    }

    // ---- Your own: what the mouse does with them ----

    /** True while you hold the sword and shield, whole: the mouse is theirs. */
    public static boolean holding() {
        return own != null && own.broke < 0.0F;
    }

    /** True while you may start a new move: the last one has come far enough, and you are not charging. */
    private static boolean ready() {
        Own mine = own;
        return mine != null && mine.broke < 0.0F && !mine.charging
                && now(0.0F) - mine.start >= mine.move.ready();
    }

    private static void begin(SwordMove move) {
        if (own != null) {
            own.move = move;
            own.start = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
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

    /** A tap of the defend button: one of the shield's bashes at random, or null when the last move is not done yet. */
    @Nullable
    public static SwordMove bash(LocalPlayer player) {
        if (!ready()) {
            return null;
        }
        SwordMove move = SwordMove.randomBash(player.getRandom(), own.lastBash);
        own.lastBash = move;
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

    /** The defend button held long enough: the charge, on your feet, as long as you are free and the ring can pay. */
    public static boolean charge(LocalPlayer player) {
        if (!ready() || ClientRing.flight(player, 0.0F) >= 0.0F || !canPay(player, "chargePowerCost")) {
            return false;
        }
        begin(SwordMove.CHARGE);
        own.charging = true;
        own.charged = 0;
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
    static void picked(Construct construct) {
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
        }
    }

    /** They are gone at once, without breaking up: you are no longer Green Lantern, or left the world. */
    static void forget() {
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
            }
        }
        BLENDS.keySet().removeIf(id -> minecraft.level.getEntity(id) == null);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        own = null;
        BLENDS.clear();
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
        Vec3 moving = player.getDeltaMovement();
        player.setDeltaMovement(mine.way.x * speed, moving.y, mine.way.z * speed);
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
        if (player.horizontalCollision || mine.charged >= wheel().value("chargeSeconds") * 20.0) {
            stopCharge();
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

    /** True while this body crouches for its move: down low for a sweep, behind the shield in a charge, in a slam. */
    static boolean crouching(Entity player, float partialTick) {
        State state = state(player, partialTick);
        return state != null && pose(player, state, partialTick).crouch() > 0.5F;
    }

    /**
     * One arm (and the upper body and legs with it) while the game poses the model: where the pose points the arm, the
     * upper body turned on the hips with the shoulders going round with it, and the legs apart in a lunge.
     */
    static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        State state = state(entity, partialTick);
        if (state == null) {
            return;
        }
        SwordPoses.Pose pose = pose(entity, state, partialTick);
        // Crouching, the game tips both arms a little further forward afterwards: taken off here beforehand.
        float crouch = model.crouching ? 0.4F : 0.0F;
        if (arm == HumanoidArm.RIGHT) {
            float sway = model.rightArm.xRot * 0.2F;
            model.rightArm.xRot = -(Mth.HALF_PI + pose.armPitch()) + sway - crouch;
            model.rightArm.yRot = pose.armYaw() + pose.twist();
            model.rightArm.zRot = 0.0F;
        } else {
            float sway = model.leftArm.xRot * 0.2F;
            model.leftArm.xRot = -(Mth.HALF_PI + pose.shieldArmPitch()) + sway - crouch;
            model.leftArm.yRot = pose.shieldArmYaw() + pose.twist();
            model.leftArm.zRot = 0.0F;
        }
        float twist = pose.twist();
        model.body.yRot = twist;
        model.rightArm.z = Mth.sin(twist) * 5.0F;
        model.rightArm.x = -Mth.cos(twist) * 5.0F;
        model.leftArm.z = -Mth.sin(twist) * 5.0F;
        model.leftArm.x = Mth.cos(twist) * 5.0F;
        if (pose.step() > 0.0F) {
            model.rightLeg.xRot = Mth.lerp(pose.step(), model.rightLeg.xRot, -0.75F);
            model.leftLeg.xRot = Mth.lerp(pose.step(), model.leftLeg.xRot, 0.55F);
        }
    }

    /**
     * The sword and shield of someone seen from outside (you too, from behind), where his hands were drawn: the sword in
     * his fist (or tossed up, spinning, while they take shape), the shield on his forearm, the streak of a swung blade,
     * and the light of a slam. While they take shape the ring feeds the growing shield a beam of its light.
     */
    static void draw(ConstructPainter painter, Entity player, @Nullable Vec3 ring, float partialTick) {
        State state = state(player, partialTick);
        SwordSpot.Spot spot = SwordSpot.of(player);
        if (state == null || spot == null) {
            return;
        }
        SwordPoses.Pose pose = pose(player, state, partialTick);
        float twist = pose.twist();
        double apart = state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / SwordShield.BREAK_TICKS);
        // Where the middle of his body is: worked back from where his fist was drawn.
        Vec3 origin = spot.grip().subtract(spot.world(hand(pose, true), 0.0F));
        Vec3[] blade = SwordPoses.frame(pose.bladeYaw(), pose.bladePitch(), pose.bladeRoll());
        Vec3 forward = spot.world(blade[0], twist);
        Vec3 edge = spot.world(blade[1], twist);
        Vec3 grip = spot.grip();
        float[] toss = SwordPoses.toss(state.move(), state.t());
        if (toss != null) {
            grip = origin.add(spot.world(tossed(toss[0]), 0.0F));
            Vec3 across = forward.cross(edge).normalize();
            forward = ConstructPainter.spin(forward, across, toss[1]);
            edge = ConstructPainter.spin(edge, across, toss[1]);
        }
        double swordGrown = SwordPoses.swordGrown(state.move(), state.age());
        SwordPainter.sword(painter, grip, forward, edge, SWORD_SCALE, swordGrown, apart);
        Vec3[] shield = SwordPoses.frame(pose.shieldYaw(), pose.shieldPitch(), pose.shieldRoll());
        Vec3 face = spot.world(shield[0], twist);
        Vec3 top = spot.world(shield[1], twist);
        Vec3 middle = spot.mount().add(face.scale(SHIELD_OUT));
        double shieldGrown = SwordPoses.shieldGrown(state.move(), state.age());
        SwordPainter.shield(painter, middle, face, top, SHIELD_SCALE, shieldGrown, apart);
        if (apart > 0.0) {
            return;
        }
        if (ring != null && shieldGrown < 1.0) {
            painter.beam(ring, middle, 1.0 - shieldGrown * 0.6, 0.6);
        }
        trail(painter, state, pose, body -> origin.add(spot.world(body, 0.0F)), SWORD_SCALE);
        slamLight(painter, state, origin.add(spot.world(new Vec3(0.0, 0.0, 1.3), 0.0F)).subtract(0.0,
                origin.y - player.getY(), 0.0));
    }

    /** Turns a point of the body (x to his right, y up, z ahead of his middle) into a point to draw at. */
    private interface Placing {
        Vec3 at(Vec3 body);
    }

    /**
     * The streak a swung blade leaves: while a cut or thrust whips through, a sheet of light between where the tip and
     * the root of the blade were over the last few moments.
     */
    private static void trail(ConstructPainter painter, State state, SwordPoses.Pose pose, Placing placing,
            double scale) {
        SwordMove move = state.move();
        if (move.kind() != SwordMove.Kind.ATTACK && move != SwordMove.FLURRY) {
            return;
        }
        int[] hits = move.hits();
        float first = move == SwordMove.FLURRY ? SwordMove.FIRST_STAB : hits[0];
        float last = move == SwordMove.FLURRY ? SwordMove.FIRST_STAB + (SwordMove.STABS - 1) * SwordMove.STAB_EVERY
                : hits[hits.length - 1];
        float t = state.t();
        if (t < first - 2.5F || t > last + 2.5F) {
            return;
        }
        double strength = 1.0 - Math.max(0.0, Math.abs(t - Mth.clamp(t, first - 1.0F, last + 0.5F)) / 1.5);
        List<Vec3> tips = new ArrayList<>(TRAIL);
        List<Vec3> roots = new ArrayList<>(TRAIL);
        for (int k = 0; k < TRAIL; k++) {
            float then = t - k * TRAIL_STEP;
            SwordPoses.Pose at = k == 0 ? pose : SwordPoses.at(move, Math.max(0.0F, then), then);
            Vec3 hand = hand(at, true);
            Vec3 way = SwordPoses.way(at.bladeYaw() + at.twist(), at.bladePitch());
            tips.add(placing.at(hand.add(way.scale(SwordPainter.TIP * scale))));
            roots.add(placing.at(hand.add(way.scale(0.45 * scale))));
        }
        SwordPainter.trail(painter, tips, roots, Mth.clamp(strength, 0.0, 1.0) * (move == SwordMove.FLURRY ? 0.6 : 1.0));
    }

    /** The end of a charge: rings of light running out over the ground from where the shield struck it. */
    private static void slamLight(ConstructPainter painter, State state, Vec3 at) {
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
                    ConstructPainter.alpha(fade), ConstructPainter.alpha(0.5 * fade));
        }
        if (since < 3.0F) {
            painter.flare(at.add(0.0, 0.3, 0.0), 0.8 * (1.0 - since / 3.0F), 1.0 - since / 3.0F);
        }
    }

    // ---- First person ----

    /**
     * Your own two arms with the sword and the shield, instead of your empty hands: both drawn along with the main hand,
     * moving through the very same poses as your body seen from outside.
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
        SwordPoses.Pose pose = pose(player, state, partialTick);
        SwordPoses.Pose guard = SwordPoses.GUARD;
        Vec3 right = OWN_RIGHT.add(view(hand(pose, true).subtract(hand(guard, true))));
        Vec3 left = OWN_LEFT.add(view(hand(pose, false).subtract(hand(guard, false))));
        Vec3 leftWay = view(SwordPoses.way(pose.shieldArmYaw() + pose.twist(), pose.shieldArmPitch()));
        PoseStack stack = event.getPoseStack();
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        RechargeAnimation.arm(stack, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, 1.0F,
                vector(right), shoulder(RechargeAnimation.SHOULDER_RIGHT, right.subtract(OWN_RIGHT)));
        RechargeAnimation.arm(stack, event.getMultiBufferSource(), event.getPackedLight(), player, renderer, -1.0F,
                vector(left), shoulder(RechargeAnimation.SHOULDER_LEFT, left.subtract(OWN_LEFT)));
        float time = player.tickCount + partialTick;
        ConstructPainter painter = ConstructPainter.hand(stack, time);
        double apart = state.broken() < 0.0F ? 0.0 : Math.max(1.0E-3, state.broken() / SwordShield.BREAK_TICKS);
        Vec3[] blade = SwordPoses.frame(pose.bladeYaw() + pose.twist(), pose.bladePitch(), pose.bladeRoll());
        Vec3 forward = view(blade[0]);
        Vec3 edge = view(blade[1]);
        Vec3 grip = right;
        float[] toss = SwordPoses.toss(state.move(), state.t());
        if (toss != null) {
            grip = OWN_RIGHT.add(view(tossed(toss[0]).subtract(hand(guard, true))));
            Vec3 across = forward.cross(edge).normalize();
            forward = ConstructPainter.spin(forward, across, toss[1]);
            edge = ConstructPainter.spin(edge, across, toss[1]);
        }
        SwordPainter.sword(painter, grip, forward, edge, OWN_SWORD, SwordPoses.swordGrown(state.move(), state.age()),
                apart);
        Vec3[] shield = SwordPoses.frame(pose.shieldYaw() + pose.twist(), pose.shieldPitch(), pose.shieldRoll());
        Vec3 face = view(shield[0]);
        SwordPainter.shield(painter, left.add(face.scale(SHIELD_OUT)).subtract(leftWay.scale(0.12)), face,
                view(shield[1]), OWN_SHIELD, SwordPoses.shieldGrown(state.move(), state.age()), apart);
        if (apart <= 0.0) {
            trail(painter, state, pose, body -> OWN_RIGHT.add(view(body.subtract(hand(guard, true)))), OWN_SWORD);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    // ---- Where things are, from a pose ----

    /**
     * Where a hand is, seen from the middle of his body at the height of his shoulders (x to his right, y up, z ahead):
     * at the end of the arm, from a shoulder that goes round as the upper body turns, and further forward in a lunge.
     */
    static Vec3 hand(SwordPoses.Pose pose, boolean right) {
        double side = right ? 1.0 : -1.0;
        double cos = Mth.cos(pose.twist());
        double sin = Mth.sin(pose.twist());
        Vec3 shoulder = new Vec3(side * SHOULDER * cos, 0.0, -side * SHOULDER * sin);
        Vec3 way = right ? SwordPoses.way(pose.armYaw() + pose.twist(), pose.armPitch())
                : SwordPoses.way(pose.shieldArmYaw() + pose.twist(), pose.shieldArmPitch());
        return shoulder.add(way.scale(ARM)).add(0.0, 0.0, STEP * pose.step());
    }

    /**
     * Where the sword is while it is tossed up as it takes shape: from where the fist let go of it, up high and back down
     * into where the fist catches it, {@code u} (0 to 1) of the way.
     */
    private static Vec3 tossed(float u) {
        Vec3 from = hand(SwordPoses.at(SwordMove.EQUIP, 10.0F, 0.0F), true);
        Vec3 to = hand(SwordPoses.at(SwordMove.EQUIP, 20.0F, 0.0F), true);
        return from.lerp(to, u).add(0.0, TOSS_HIGH * 4.0 * u * (1.0 - u), 0.25 * Math.sin(Math.PI * u));
    }

    /** A way seen from the body (x to his right, y up, z ahead) as a way before your eyes in first person. */
    private static Vec3 view(Vec3 body) {
        return new Vec3(body.x, body.y, -body.z);
    }

    /** Where one of your own arms reaches in from in first person, with its hand moved {@code moved} from the guard. */
    private static Vector3f shoulder(Vector3f rest, Vec3 moved) {
        return new Vector3f(rest).add(vector(moved.scale(OWN_FOLLOW)));
    }

    private static Vector3f vector(Vec3 at) {
        return new Vector3f((float) at.x, (float) at.y, (float) at.z);
    }
}
