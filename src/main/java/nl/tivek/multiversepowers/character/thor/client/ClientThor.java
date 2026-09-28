package nl.tivek.multiversepowers.character.thor.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.engine.math.Vectors;

// What every Thor in sight is doing, as his server tells it (and, for your own, as your game already did it): whether
// he flies, floats or carries someone, and the move he last started and when.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientThor {
    public static final class View {
        int flags;
        int move = ThorStatePayload.NONE;
        int arg;
        int start;
        int carried = -1;
        // The move this game started itself, and when: the server's word of the same move is not taken twice.
        int predicted = ThorStatePayload.NONE;
        int predictedAt;

        public boolean has(int flag) {
            return (this.flags & flag) != 0;
        }

        public int move() {
            return this.move;
        }

        public int arg() {
            return this.arg;
        }

        // Ticks since the move started.
        public float age(float partialTick) {
            return ticks - this.start + partialTick;
        }
    }

    private static final int ECHO = 10;
    private static final Int2ObjectOpenHashMap<View> VIEWS = new Int2ObjectOpenHashMap<>();
    private static int ticks;

    private ClientThor() {
    }

    @Nullable
    public static View view(Entity entity) {
        return VIEWS.get(entity.getId());
    }

    public static boolean has(Entity entity, int flag) {
        View view = VIEWS.get(entity.getId());
        return view != null && view.has(flag);
    }

    public static void update(ThorStatePayload payload) {
        View view = VIEWS.computeIfAbsent(payload.entity(), id -> new View());
        Minecraft minecraft = Minecraft.getInstance();
        boolean own = minecraft.player != null && minecraft.player.getId() == payload.entity();
        view.flags = payload.flags();
        if (payload.move() != ThorStatePayload.NONE) {
            boolean echo = own && view.predicted == payload.move() && ticks - view.predictedAt <= ECHO;
            if (!echo) {
                start(view, payload.move(), payload.arg());
            } else if (payload.move() == ThorStatePayload.DIVE) {
                view.arg = payload.arg();
                view.carried = payload.arg() - 1;
            }
        }
        if (own) {
            ThorMotion.told(view);
        }
        if (view.flags == 0 && view.move == ThorStatePayload.NONE) {
            VIEWS.remove(payload.entity());
        }
    }

    // Your own game started a move: shown at once, without waiting for the server.
    static void predict(Entity entity, int move, int arg, int flags) {
        View view = VIEWS.computeIfAbsent(entity.getId(), id -> new View());
        view.flags = flags;
        view.predicted = move;
        view.predictedAt = ticks;
        start(view, move, arg);
    }

    static void flags(Entity entity, int flags) {
        View view = VIEWS.computeIfAbsent(entity.getId(), id -> new View());
        view.flags = flags;
    }

    private static void start(View view, int move, int arg) {
        view.move = move;
        view.arg = arg;
        view.start = ticks;
        if (move == ThorStatePayload.DIVE) {
            view.carried = arg - 1;
        }
    }

    // Where a creature Thor carries hangs: from his right hand, a little ahead of him (as the server puts it).
    static Vec3 hand(LivingEntity thor, Entity held, float partialTick) {
        Vec3 look = Vec3.directionFromRotation(0.0F, thor.getViewYRot(partialTick));
        Vec3 right = look.cross(Vectors.UP).normalize();
        return thor.getPosition(partialTick).add(look.scale(0.7)).add(right.scale(0.45))
                .add(0.0, 0.5 - held.getBbHeight() * 0.55, 0.0);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        ticks++;
        // A carried creature is put at the hand every tick here, where the server only tells it now and then.
        for (Int2ObjectOpenHashMap.Entry<View> entry : VIEWS.int2ObjectEntrySet()) {
            View view = entry.getValue();
            if (!view.has(ThorStatePayload.CARRYING) || view.carried < 0) {
                continue;
            }
            Entity thor = level.getEntity(entry.getIntKey());
            Entity held = level.getEntity(view.carried);
            if (thor instanceof LivingEntity living && held != null) {
                Vec3 at = hand(living, held, 1.0F);
                held.setPos(at.x, at.y, at.z);
                held.setDeltaMovement(Vec3.ZERO);
            }
        }
        VIEWS.int2ObjectEntrySet().removeIf(entry -> level.getEntity(entry.getIntKey()) == null
                && ticks - entry.getValue().start > 100);
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VIEWS.clear();
        ThorMotion.stop();
    }
}
