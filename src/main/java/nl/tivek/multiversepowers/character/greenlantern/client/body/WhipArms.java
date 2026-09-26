package nl.tivek.multiversepowers.character.greenlantern.client.body;

import java.util.Optional;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.Construct;
import nl.tivek.multiversepowers.character.greenlantern.ability.EnergyWhip;
import nl.tivek.multiversepowers.character.greenlantern.ability.WhipMove;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class WhipArms extends WhipFirstPerson {
    private static final float FOLLOW_AFTER = 10.0F;
    private static final float REFUSED_AFTER = 40.0F;
    private static final double AIM_WIDE = 0.6;

    private WhipArms() {
    }

    public static boolean holding() {
        return own != null && own.broke < 0.0F;
    }

    static boolean present() {
        return own != null;
    }

    private static boolean ready() {
        Own mine = own;
        return mine != null && mine.broke < 0.0F && !mine.whirling && !mine.spinning
                && now(0.0F) - mine.start >= mine.move.ready();
    }

    private static void begin(WhipMove move, double before) {
        if (own != null) {
            own.move = move;
            own.start = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            own.felt = -1;
            own.before = before;
        }
    }

    private static boolean canPay(LocalPlayer player, double cost) {
        return ClientRing.power(player) + 1.0E-4F >= cost;
    }

    @Nullable
    public static WhipMove lash(LocalPlayer player) {
        if (!ready()) {
            return null;
        }
        WhipMove move = WhipMove.randomAttack(player.getRandom(), own.lastAttack);
        own.lastAttack = move;
        begin(move, 0.0);
        return move;
    }

    public static boolean whirl(LocalPlayer player) {
        if (!ready() || !canPay(player, wheel().value("whirlPowerPerSecond") / 20.0)) {
            return false;
        }
        begin(WhipMove.WHIRL, 0.0);
        own.whirling = true;
        own.confirmed = false;
        return true;
    }

    public static void stopWhirl() {
        Own mine = own;
        if (mine != null && mine.whirling) {
            mine.whirling = false;
            begin(WhipMove.WHIRL_CRACK, now(0.0F) - mine.start);
        }
    }

    public static boolean spin(LocalPlayer player) {
        if (!ready() || !canPay(player, wheel().value("spinPowerPerSecond") / 20.0)) {
            return false;
        }
        begin(WhipMove.SPIN_SHIELD, 0.0);
        own.spinning = true;
        own.confirmed = false;
        return true;
    }

    public static void stopSpin() {
        Own mine = own;
        if (mine != null && mine.spinning) {
            mine.spinning = false;
            begin(WhipMove.SPIN_END, now(0.0F) - mine.start);
        }
    }

    public static boolean lasso(LocalPlayer player) {
        if (!ready()) {
            return false;
        }
        int aimed = aimed(player, wheel().value("lassoRange"));
        if (aimed >= 0 && !canPay(player, wheel().value("lassoPowerCost"))) {
            return false;
        }
        begin(WhipMove.LASSO, 0.0);
        own.aimed = aimed;
        return true;
    }

    // The creature under the crosshair the lasso will fly at, as this game sees it; the server has the last word.
    private static int aimed(LocalPlayer player, double range) {
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getLookAngle().scale(range));
        BlockHitResult wall = player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (wall.getType() != HitResult.Type.MISS) {
            end = wall.getLocation();
        }
        Entity best = null;
        double nearest = Double.MAX_VALUE;
        for (Entity entity : player.level().getEntities(player, new AABB(eye, end).inflate(AIM_WIDE + 1.0),
                entity -> entity instanceof LivingEntity && entity.isAlive() && !entity.isSpectator())) {
            Optional<Vec3> hit = entity.getBoundingBox().inflate(AIM_WIDE).clip(eye, end);
            if (hit.isPresent() && eye.distanceToSqr(hit.get()) < nearest) {
                nearest = eye.distanceToSqr(hit.get());
                best = entity;
            }
        }
        return best == null ? -1 : best.getId();
    }

    public static void picked(Construct construct) {
        float now = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (construct == Construct.ENERGY_WHIP) {
            if (own == null || own.broke >= 0.0F) {
                if (own != null && drawn != null && now - own.broke < EnergyWhip.BREAK_TICKS) {
                    shards = drawn;
                    shardsSince = own.broke;
                }
                own = new Own();
                own.start = now;
                own.taken = now;
            }
        } else if (own != null && own.broke < 0.0F) {
            own.broke = now;
            own.whirling = false;
            own.spinning = false;
        }
    }

    public static void forget() {
        own = null;
        drawn = null;
        shards = null;
        ROOT.forget();
        ALONG.forget();
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
            ClientConstructs.Whip told = ClientConstructs.whip(player.getId(), 0.0F);
            if (told != null && mine.track < 0 && told.broken() < 0.0F && mine.broke < 0.0F) {
                mine.track = told.id();
            }
            boolean ours = told != null && told.id() == mine.track;
            if (mine.broke >= 0.0F && now - mine.broke >= LOWER_FROM + LOWER_TICKS) {
                own = null;
                drawn = null;
            } else if (!ours && now - mine.taken > 30.0F && mine.broke < 0.0F) {
                own = null;
            } else if (ours && told.broken() >= 0.0F && mine.broke < 0.0F) {
                mine.broke = now;
                mine.whirling = false;
                mine.spinning = false;
            } else if (ours) {
                follow(mine, told, now);
            }
            if (own != null && mine.broke < 0.0F && mine.move != WhipMove.EQUIP) {
                feel(mine, now);
            }
        }
        if (shards != null && now(0.0F) - shardsSince >= EnergyWhip.BREAK_TICKS) {
            shards = null;
        }
        BLENDS.keySet().removeIf(id -> minecraft.level.getEntity(id) == null);
    }

    // The server stops a whirl or spin by itself when the ring runs dry: the own hands follow it then. Until it has
    // told of the whirl at all (a slow connection), only a long wait counts as a no.
    private static void follow(Own mine, ClientConstructs.Whip told, float now) {
        if (!mine.whirling && !mine.spinning) {
            mine.confirmed = false;
            return;
        }
        boolean going = (told.move() & (mine.whirling ? WhipMove.WHIRLING : WhipMove.SPINNING)) != 0;
        if (going) {
            mine.confirmed = true;
            return;
        }
        if (now - mine.start < (mine.confirmed ? FOLLOW_AFTER : REFUSED_AFTER)) {
            return;
        }
        if (mine.whirling) {
            stopWhirl();
        } else {
            stopSpin();
        }
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        Own mine = own;
        LocalPlayer player = minecraft.player;
        if (mine == null || player == null || minecraft.level == null || minecraft.isPaused()) {
            return;
        }
        if (mine.move != WhipMove.EQUIP || mine.broke >= 0.0F) {
            mine.heard = Float.POSITIVE_INFINITY;
            return;
        }
        float t = now(event.getPartialTick().getGameTimeDeltaPartialTick(false)) - mine.start;
        float was = mine.heard;
        mine.heard = t;
        if (!(t > was)) {
            return;
        }
        EnergyWhip.equipSounds(was, t, (sound, volume, pitch) -> minecraft.level.playLocalSound(player.getX(),
                player.getY() + 1.0, player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false));
        WhipMove.Crack[] cracks = WhipMove.EQUIP.cracks();
        float[][] beats = { { WhipMove.FORMED, 0.25F }, { WhipMove.RISE, 0.2F },
                { cracks.length > 0 ? cracks[0].tick() : WhipMove.THROW + 4, 1.0F } };
        for (float[] beat : beats) {
            if (was < beat[0] && t >= beat[0]) {
                kick(mine.start + beat[0], beat[1], beat[1] >= 1.0F ? 1.0F : -1.0F);
            }
        }
    }

    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || event.getCamera().getEntity() != player || event.getCamera().isDetached()) {
            return;
        }
        float partialTick = (float) event.getPartialTick();
        float now = now(partialTick);
        float step = Float.isNaN(shownAt) ? 1.0F : Mth.clamp(now - shownAt, 0.0F, LOOK_BACK) / LOOK_BACK;
        shownAt = now;
        shown = own != null && SwordFirstPerson.handsFree(player) ? Math.min(1.0F, shown + step)
                : Math.max(0.0F, shown - step);
        if (own == null) {
            return;
        }
        float[] look = ownLook(player, partialTick);
        if (look != null) {
            SwordFirstPerson.turn(event, look);
        }
        float[] shake = shake(player, partialTick);
        event.setRoll(event.getRoll() + shake[0]);
        event.setPitch(Mth.clamp(event.getPitch() + shake[1], -90.0F, 90.0F));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        forget();
        shown = 0.0F;
        shownAt = Float.NaN;
        BLENDS.clear();
    }

    // Also when the sword's or flamethrower's hands took the event: while one construct breaks up and the other
    // forms, both draw.
    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || own == null || !SwordFirstPerson.handsFree(player)
                || event.isCanceled() && !SwordArms.present() && !FlameArms.present()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            drawHands(event, player);
        }
    }

    // Before the server has told of the whip, the own game draws it by itself.
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Camera camera = event.getCamera();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || own == null || player == null
                || minecraft.level == null || ClientConstructs.whip(player.getId(), 0.0F) != null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(), time(partialTick),
                event.getFrustum());
        if (camera.getEntity() == player && !camera.isDetached()) {
            drawOwn(painter, player, camera, event.getProjectionMatrix(), event.getModelViewMatrix(), partialTick);
        } else {
            draw(painter, player, RingSpot.of(player, camera, event.getProjectionMatrix(), event.getModelViewMatrix()),
                    partialTick);
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }
}
