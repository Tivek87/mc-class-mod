package nl.tivek.multiversepowers.character.docock.client;

import static nl.tivek.multiversepowers.character.docock.TentacleReach.COUNT;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.docock.OctoStatePayload;
import nl.tivek.multiversepowers.character.docock.TentacleReach;

// Every Doctor Octopus this game sees, as the server told it (OctoStatePayload): his harness and tentacles drawn after
// the world's creatures, his held creatures kept at their tentacles' tips, his ground tentacles stepping from foot spot
// to foot spot as he walks.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientOcto {
    private static final double STEP_AFTER = 1.1;
    private static final int STEP_TICKS = 5;
    private static final Map<Integer, View> VIEWS = new HashMap<>();
    private static int clock;

    private ClientOcto() {
    }

    // One Doctor Octopus's tentacles: the server's last word and, per tentacle, when its move began and where it
    // stands on the ground (stepping from `from` since `stepAt`).
    public static final class View {
        private OctoStatePayload state;
        private final int[] started = new int[COUNT];
        private final Vec3[] planted = new Vec3[COUNT];
        private final Vec3[] from = new Vec3[COUNT];
        private final int[] stepAt = new int[COUNT];

        View(OctoStatePayload state) {
            this.state = state;
            for (int i = 0; i < COUNT; i++) {
                this.started[i] = -1000;
                this.stepAt[i] = -1000;
            }
        }

        public int stance() {
            return this.state.stance();
        }

        public boolean ground(int i) {
            return (this.state.flags()[i] & OctoStatePayload.GROUND) != 0;
        }

        public boolean holding(int i) {
            return (this.state.flags()[i] & OctoStatePayload.HOLDING) != 0;
        }

        public boolean squeezing(int i) {
            return (this.state.flags()[i] & OctoStatePayload.SQUEEZING) != 0;
        }

        public int held(int i) {
            return this.state.held()[i];
        }

        public boolean holds(int entityId) {
            for (int i = 0; i < COUNT; i++) {
                if (this.holding(i) && this.held(i) == entityId) {
                    return true;
                }
            }
            return false;
        }

        public boolean free(int i) {
            return !this.ground(i) && !this.holding(i);
        }

        int move(int i) {
            return this.age(i, 0.0F) <= TentacleReach.MOVE_TICKS ? this.state.move()[i] : TentacleReach.NONE;
        }

        float age(int i, float partialTick) {
            return clock - this.started[i] + partialTick;
        }

        Vec3 way(int i) {
            float[] way = this.state.way();
            return new Vec3(way[i * 3], way[i * 3 + 1], way[i * 3 + 2]);
        }

        int target(int i) {
            return this.state.target()[i];
        }
    }

    public static void update(OctoStatePayload payload) {
        if (!payload.out()) {
            VIEWS.remove(payload.player());
            return;
        }
        View view = VIEWS.get(payload.player());
        if (view == null) {
            view = new View(payload);
            VIEWS.put(payload.player(), view);
            return;
        }
        for (int i = 0; i < COUNT; i++) {
            if (payload.seq()[i] != view.state.seq()[i]) {
                view.started[i] = clock;
            }
        }
        view.state = payload;
    }

    @Nullable
    public static View of(Entity entity) {
        return VIEWS.get(entity.getId());
    }

    static TentacleShapes.Body body(Player player, float partialTick) {
        return new TentacleShapes.Body(player.getPosition(partialTick), Mth.rotLerp(partialTick, player.yBodyRotO,
                player.yBodyRot), player.isCrouching());
    }

    // Before the world ticks, so a held creature's new place is set in time.
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        clock++;
        for (Map.Entry<Integer, View> entry : VIEWS.entrySet()) {
            if (!(level.getEntity(entry.getKey()) instanceof Player owner)) {
                continue;
            }
            View view = entry.getValue();
            TentacleShapes.Body body = body(owner, 1.0F);
            for (int i = 0; i < COUNT; i++) {
                if (view.ground(i)) {
                    plant(level, owner, view, body, i);
                } else {
                    view.planted[i] = null;
                }
                Entity held = view.holding(i) ? level.getEntity(view.held(i)) : null;
                if (held != null && held != minecraft.player) {
                    Vec3 at = TentacleReach.hold(owner.position(), owner.yBodyRot, i, held.getBbWidth())
                            .add(TentacleReach.swing(view.move(i), view.age(i, 0.0F), view.way(i)));
                    held.lerpTo(at.x, at.y - held.getBbHeight() * 0.5, at.z, held.getYRot(), held.getXRot(), 1);
                }
            }
        }
    }

    private static void plant(ClientLevel level, Player owner, View view, TentacleShapes.Body body, int i) {
        Vec3 want = body.foot(i);
        BlockHitResult hit = level.clip(new ClipContext(want.add(0.0, 2.0, 0.0), want.add(0.0, -3.0, 0.0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, owner));
        Vec3 spot = hit.getType() == HitResult.Type.MISS ? want.add(0.0, -0.5, 0.0) : hit.getLocation();
        Vec3 now = view.planted[i];
        if (now == null) {
            view.planted[i] = spot;
            view.from[i] = spot;
        } else if (now.distanceTo(spot) > STEP_AFTER && clock - view.stepAt[i] >= STEP_TICKS) {
            view.from[i] = now;
            view.planted[i] = spot;
            view.stepAt[i] = clock;
        }
    }

    // Where a ground tentacle's tip is: on its spot, or lifted on its way there.
    private static Vec3 foot(View view, int i, float partialTick) {
        float t = Mth.clamp((clock - view.stepAt[i] + partialTick) / STEP_TICKS, 0.0F, 1.0F);
        return view.from[i].lerp(view.planted[i], t).add(0.0, 0.5 * Mth.sin(Mth.PI * t), 0.0);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || VIEWS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float time = clock + partialTick;
        boolean firstPerson = minecraft.options.getCameraType().isFirstPerson();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        TentaclePainter painter = new TentaclePainter(event.getPoseStack(), level, event.getCamera().getPosition(),
                firstPerson);
        // All the metal first, then all the lamps' light: taking the second buffer ends the first.
        for (boolean glow : new boolean[] { false, true }) {
            painter.phase(buffers.getBuffer(glow ? RenderType.eyes(OctoSkin.TEXTURE.id())
                    : RenderType.entityCutoutNoCull(OctoSkin.TEXTURE.id())), glow);
            for (Map.Entry<Integer, View> entry : VIEWS.entrySet()) {
                if (!(level.getEntity(entry.getKey()) instanceof Player owner)
                        || owner.isInvisibleTo(minecraft.player)) {
                    continue;
                }
                View view = entry.getValue();
                TentacleShapes.Body body = body(owner, partialTick);
                boolean self = owner == minecraft.getCameraEntity() && firstPerson;
                if (!self) {
                    painter.harness(body);
                }
                for (int i = 0; i < COUNT; i++) {
                    draw(level, painter, view, body, i, partialTick, time);
                }
            }
        }
        buffers.endBatch();
    }

    private static void draw(ClientLevel level, TentaclePainter painter, View view, TentacleShapes.Body body, int i,
            float partialTick, float time) {
        Vec3 socket = body.socket(i);
        Vec3 out = body.out(i);
        Vec3 tip;
        Vec3 in;
        double open;
        Entity held = view.holding(i) ? level.getEntity(view.held(i)) : null;
        if (view.ground(i) && view.planted[i] != null) {
            tip = foot(view, i, partialTick);
            in = new Vec3(0.0, -1.0, 0.0);
            open = 1.0;
        } else if (held != null) {
            Vec3 middle = held.getPosition(partialTick).add(0.0, held.getBbHeight() * 0.5, 0.0);
            in = middle.distanceToSqr(socket) < 1.0E-4 ? body.forward() : middle.subtract(socket).normalize();
            tip = middle.subtract(in.scale(held.getBbWidth() * 0.45));
            open = view.squeezing(i) ? 0.05 * (1.0 + Mth.sin(time * 2.1F)) : 0.2;
        } else {
            tip = body.rest(i, time);
            in = body.forward().add(0.0, TentacleReach.upper(i) ? -0.4 : 0.1, 0.0).normalize();
            open = 0.65;
            Entity aimed = view.move(i) == TentacleReach.SLAP ? level.getEntity(view.target(i)) : null;
            if (aimed != null) {
                Vec3 at = aimed.getPosition(partialTick).add(0.0, aimed.getBbHeight() * 0.5, 0.0);
                float t = Mth.sin(Mth.PI * Mth.clamp(view.age(i, partialTick) / TentacleReach.MOVE_TICKS, 0.0F,
                        1.0F));
                Vec3 rest = tip;
                tip = rest.lerp(at, t);
                in = at.distanceToSqr(rest) < 1.0E-4 ? in : at.subtract(rest).normalize();
                open = 0.35;
            }
        }
        painter.tentacle(TentacleShapes.curve(socket, out, tip, in), open, view.squeezing(i));
    }
}
