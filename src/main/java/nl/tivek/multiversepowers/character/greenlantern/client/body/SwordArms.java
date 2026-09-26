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

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class SwordArms extends SwordFirstPerson {
    private static final float CHARGE_FOV = 1.12F;

    private SwordArms() {
    }

    public static boolean holding() {
        return own != null && own.broke < 0.0F;
    }

    static boolean present() {
        return own != null;
    }

    public static boolean charging() {
        return own != null && own.charging;
    }

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

    public static boolean flurry(LocalPlayer player) {
        if (!ready() || !canPay(player, "flurryPowerCost")) {
            return false;
        }
        begin(SwordMove.FLURRY);
        return true;
    }

    public static void stopFlurry() {
        Own mine = own;
        if (mine != null && mine.move == SwordMove.FLURRY && now(0.0F) - mine.start < SwordMove.FLURRY.ticks()) {
            mine.start = now(0.0F) - SwordMove.FLURRY.ticks();
        }
    }

    public static boolean block(boolean up) {
        Own mine = own;
        if (mine == null || mine.broke >= 0.0F || mine.charging || mine.blocking == up) {
            return false;
        }
        mine.blocking = up;
        mine.blockSince = now(0.0F);
        return true;
    }

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

    public static boolean stopCharge() {
        Own mine = own;
        if (mine == null || !mine.charging) {
            return false;
        }
        mine.charging = false;
        begin(SwordMove.SLAM);
        return true;
    }

    private static boolean canPay(LocalPlayer player, String cost) {
        return ClientRing.power(player) + 1.0E-4F >= wheel().value(cost);
    }

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

    public static void forget() {
        if (own != null && looked != null) {
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
                own = null;
                drawn = null;
            } else if (told == null && now - mine.taken > 30.0F && mine.broke < 0.0F) {
                own = null;
            } else if (told != null && told.broken() >= 0.0F && mine.broke < 0.0F) {
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
        HandSpot.clear();
    }

    @SubscribeEvent
    public static void onClone(ClientPlayerNetworkEvent.Clone event) {
        ConstructChoice.forget();
    }

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
        double going = Math.min(1.0, (mine.charged + 1) / 3.0);
        Vec3 moving = player.getDeltaMovement();
        player.setDeltaMovement(mine.way.x * speed * going, moving.y, mine.way.z * speed * going);
        player.setSprinting(true);
    }

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

    // Also when the flamethrower's or whip's hands took the event: while one construct breaks up and the other forms,
    // both draw.
    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || own == null || !handsFree(player)
                || event.isCanceled() && !FlameArms.present() && !WhipArms.present()) {
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
        // Swapped for the flamethrower or whip: its hands are already on their way up, only the pieces are left to fly.
        boolean handsTaken = FlameArms.holding() || WhipArms.holding();
        if (!handsTaken) {
            arm(stack, buffers, event.getPackedLight(), player, renderer, 1.0F, pose.hand(), shoulder, rest);
        }
        if (rest < 1.0F && !handsTaken) {
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
