package nl.tivek.multiversepowers.character.greenlantern.client.body;

import javax.annotation.Nullable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameMove;
import nl.tivek.multiversepowers.character.greenlantern.ability.FlameWall;
import nl.tivek.multiversepowers.character.greenlantern.ability.Flamethrower;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientConstructs;
import nl.tivek.multiversepowers.character.greenlantern.client.ClientRing;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FireStream;
import nl.tivek.multiversepowers.character.greenlantern.client.render.FlameSound;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class FlameArms extends FlameFirstPerson {
    private static final float FOLLOW_AFTER = 10.0F;
    private static final float REFUSED_AFTER = 40.0F;

    private FlameArms() {
    }

    public static boolean holding() {
        return own != null && own.broke < 0.0F;
    }

    static boolean present() {
        return own != null;
    }

    private static boolean ready() {
        Own mine = own;
        return mine != null && mine.broke < 0.0F && !mine.firing && !mine.swirling
                && now(0.0F) - mine.start >= mine.move.ready();
    }

    private static void begin(FlameMove move) {
        if (own != null) {
            own.move = move;
            own.start = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
            own.felt = -1;
        }
    }

    private static boolean canPay(LocalPlayer player, double cost) {
        return ClientRing.power(player) + 1.0E-4F >= cost;
    }

    @Nullable
    public static FlameMove sweep(LocalPlayer player) {
        if (!ready() || !canPay(player, wheel().value("sweepPowerCost"))) {
            return null;
        }
        FlameMove move = FlameMove.randomAttack(player.getRandom(), own.lastSweep);
        own.lastSweep = move;
        begin(move);
        return move;
    }

    public static boolean pour(LocalPlayer player) {
        if (!ready() || !canPay(player, wheel().value("infernoPowerPerSecond") / 20.0)) {
            return false;
        }
        begin(FlameMove.INFERNO);
        own.firing = true;
        own.confirmed = false;
        return true;
    }

    public static boolean stopPouring() {
        Own mine = own;
        if (mine == null || !mine.firing) {
            return false;
        }
        mine.firing = false;
        begin(FlameMove.VENT);
        return true;
    }

    public static boolean swirl(LocalPlayer player) {
        if (!ready() || !canPay(player, wheel().value("vortexPowerPerSecond") / 20.0)) {
            return false;
        }
        begin(FlameMove.VORTEX);
        own.swirling = true;
        own.confirmed = false;
        return true;
    }

    public static boolean stopSwirling() {
        Own mine = own;
        if (mine == null || !mine.swirling) {
            return false;
        }
        mine.swirling = false;
        begin(FlameMove.BURST);
        return true;
    }

    public static boolean wall(LocalPlayer player) {
        if (!ready() || !canPay(player, wheel().value("wallPowerCost"))) {
            return false;
        }
        if (FlameWall.base(player.level(), player, player.getLookAngle()) == null) {
            player.displayClientMessage(Component.translatable("ring." + MultiversePowers.MODID + ".wall_no_ground"),
                    true);
            return false;
        }
        begin(FlameMove.WALL);
        return true;
    }

    public static void picked(Construct construct) {
        float now = now(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (construct == Construct.FLAMETHROWER) {
            if (own == null || own.broke >= 0.0F) {
                if (own != null && drawn != null && now - own.broke < Flamethrower.BREAK_TICKS) {
                    shards = drawn;
                    shardsSince = own.broke;
                }
                own = new Own();
                own.start = now;
                own.taken = now;
            }
        } else if (own != null && own.broke < 0.0F) {
            own.broke = now;
            own.firing = false;
            own.swirling = false;
        }
    }

    public static void forget() {
        own = null;
        drawn = null;
        shards = null;
        NOZZLE.forget();
        FURTHER.forget();
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
            ClientConstructs.Flame told = ClientConstructs.flame(player.getId(), 0.0F);
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
                mine.firing = false;
                mine.swirling = false;
            } else if (ours) {
                follow(mine, told, now);
            }
            if (own != null && mine.broke < 0.0F && mine.move != FlameMove.EQUIP) {
                feel(mine, now);
            }
        }
        if (shards != null && now(0.0F) - shardsSince >= Flamethrower.BREAK_TICKS) {
            shards = null;
        }
        for (AbstractClientPlayer other : minecraft.level.players()) {
            State state = state(other, 0.0F);
            FlameSound.keep(other, state != null && state.firing() && state.t() >= FlameMove.BRACE - 1,
                    state != null && state.swirling());
        }
        BLENDS.keySet().removeIf(id -> minecraft.level.getEntity(id) == null);
    }

    // The server stops a stream or vortex by itself when the ring runs dry: the own hands follow it then. Until it
    // has told of the stream at all (a slow connection), only a long wait counts as a no.
    private static void follow(Own mine, ClientConstructs.Flame told, float now) {
        boolean going = (told.move() & (mine.firing ? FlameMove.FIRING : FlameMove.SWIRLING)) != 0;
        if (!mine.firing && !mine.swirling) {
            mine.confirmed = false;
            return;
        }
        if (going) {
            mine.confirmed = true;
            return;
        }
        if (now - mine.start < (mine.confirmed ? FOLLOW_AFTER : REFUSED_AFTER)) {
            return;
        }
        if (mine.firing) {
            mine.firing = false;
            begin(FlameMove.VENT);
        } else {
            mine.swirling = false;
            begin(FlameMove.BURST);
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
        if (mine.move != FlameMove.EQUIP || mine.broke >= 0.0F) {
            mine.heard = Float.POSITIVE_INFINITY;
            return;
        }
        float t = now(event.getPartialTick().getGameTimeDeltaPartialTick(false)) - mine.start;
        float was = mine.heard;
        mine.heard = t;
        if (!(t > was)) {
            return;
        }
        Flamethrower.equipSounds(was, t, (sound, volume, pitch) -> minecraft.level.playLocalSound(player.getX(),
                player.getY() + 1.0, player.getZ(), sound, SoundSource.PLAYERS, volume, pitch, false));
        float[][] beats = { { FlameMove.LEFT_GRAB, 0.3F }, { FlameMove.VALVE, 0.2F }, { FlameMove.PILOT, 0.2F },
                { FlameMove.TEST, 1.0F } };
        for (float[] beat : beats) {
            if (was < beat[0] && t >= beat[0]) {
                kick(mine.start + beat[0], beat[1], beat[0] == FlameMove.TEST ? 1.0F : -1.0F);
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
        FireStream.clear();
        FlameSound.clear();
    }

    // Also when the sword's hands took the event: while one construct breaks up and the other forms, both draw.
    @SubscribeEvent(priority = EventPriority.LOW, receiveCanceled = true)
    public static void onRenderHand(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || own == null || !SwordFirstPerson.handsFree(player)
                || event.isCanceled() && !SwordArms.present()) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            drawHands(event, player);
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        Camera camera = event.getCamera();
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || own == null || player == null
                || minecraft.level == null || camera.getEntity() == player && !camera.isDetached()
                || ClientConstructs.flame(player.getId(), 0.0F) != null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(), time(partialTick),
                event.getFrustum());
        draw(painter, player, RingSpot.of(player, camera, event.getProjectionMatrix(), event.getModelViewMatrix()),
                partialTick);
        painter.finish(minecraft.renderBuffers().bufferSource());
    }
}
