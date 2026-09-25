package nl.tivek.multiversepowers.character.greenlantern.client.body;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import nl.tivek.multiversepowers.engine.math.Ease;
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
 * Built up in layers, each on the one before: {@link SwordStates} (what every body's sword and shield do),
 * {@link SwordSeen} (seen from outside), {@link SwordFirstPerson} (your own in first person); this class holds what the
 * mouse does with them and the game's events.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SwordArms extends SwordFirstPerson {
    // A charge widens your view this much.
    private static final float CHARGE_FOV = 1.12F;

    private SwordArms() {
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
}
