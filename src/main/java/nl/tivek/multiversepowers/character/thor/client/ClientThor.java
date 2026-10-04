package nl.tivek.multiversepowers.character.thor.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.thor.ThorGrab;
import nl.tivek.multiversepowers.character.thor.ThorStatePayload;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorMotion;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorPull;
import nl.tivek.multiversepowers.character.thor.client.motion.ThorRise;
import nl.tivek.multiversepowers.engine.math.Vectors;
import nl.tivek.multiversepowers.spell.client.ClientClaps;

// What every Thor in sight is doing, as his server tells it (and, for your own, as your game already did it): whether
// he flies, floats or carries someone, and the move he last started and when.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientThor {
    public static final class View {
        int flags;
        int move = ThorStatePayload.NONE;
        int arg;
        int start;
        public int carried = -1;
        // A grab: whether a dash brought him to it, and how it ends (ThorGrab.Act, -1 while his player picks) and
        // since when.
        public boolean dashed;
        public int act = -1;
        int actAt;
        // The move this game started itself, and when: the server's word of the same move is not taken twice.
        int predicted = ThorStatePayload.NONE;
        int predictedAt;
        // His last two blows (ThorBlow) and when they started, so one flows into the next; kept apart from the move
        // so a blow never cuts a dash or a landing short.
        public int blow = -1;
        int blowStart;
        public int lastBlow = -1;
        int lastBlowStart;
        int litAt;

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

        public float blowAge(float partialTick) {
            return ticks - this.blowStart + partialTick;
        }

        public float lastBlowAge(float partialTick) {
            return ticks - this.lastBlowStart + partialTick;
        }

        // Ticks since his hammer was last charged.
        public float litAge(float partialTick) {
            return ticks - this.litAt + partialTick;
        }

        // Ticks since the grab's ending was picked.
        public float actAge(float partialTick) {
            return ticks - this.actAt + partialTick;
        }
    }

    private static final int ECHO = 10;
    private static final int OWN = ThorStatePayload.FLYING | ThorStatePayload.FLOATING | ThorStatePayload.LIGHTNING;
    // A Thor who has thrown a blow this recently is kept in view, fists up, though nothing else goes on.
    static final int GUARD = 60;
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
        // Your own wind-up is shown by your own game, the moment you hold the button.
        if (!own && view.has(ThorStatePayload.CHARGING) != ((payload.flags() & ThorStatePayload.CHARGING) != 0)) {
            ClientClaps.charging(payload.entity(), !view.has(ThorStatePayload.CHARGING));
        }
        if ((payload.flags() & ThorStatePayload.HAMMER_CHARGED) != 0 && !view.has(ThorStatePayload.HAMMER_CHARGED)) {
            view.litAt = ticks;
        }
        view.flags = payload.flags();
        if (own) {
            ThorMotion.toldLightning(view.has(ThorStatePayload.LIGHTNING), ticks - view.predictedAt);
        }
        if (payload.move() == ThorStatePayload.BLOW) {
            if (!own) {
                blow(view, payload.arg());
            }
        } else if (payload.move() == ThorStatePayload.STRIKE) {
            blow(view, payload.arg());
        } else if (payload.move() == ThorStatePayload.PULL) {
            if (own) {
                ThorPull.start(payload.arg() - 1);
            }
        } else if (payload.move() == ThorStatePayload.GRAB_ACT) {
            view.act = payload.arg();
            view.actAt = ticks;
        } else if (payload.move() != ThorStatePayload.NONE) {
            boolean echo = own && view.predicted == payload.move() && ticks - view.predictedAt <= ECHO;
            if (!echo) {
                start(view, payload.move(), payload.arg());
            } else if (payload.move() == ThorStatePayload.DIVE) {
                view.arg = payload.arg();
                view.carried = payload.arg() - 1;
            }
            if (own) {
                ownMove(payload.move(), payload.arg());
            }
        }
        if (own) {
            ThorMotion.told(view);
        }
        if (view.flags == 0 && view.move == ThorStatePayload.NONE && (view.blow < 0 || ticks - view.blowStart > GUARD)) {
            VIEWS.remove(payload.entity());
        }
    }

    // The moves of a grab the server starts, which move your own Thor.
    private static void ownMove(int move, int arg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        switch (move) {
            case ThorStatePayload.GRAB -> ThorMotion.grabbed();
            case ThorStatePayload.HOIST -> ThorMotion.hoist(player);
            case ThorStatePayload.DROP -> ThorMotion.drop(arg - 1);
            case ThorStatePayload.TAKE_OFF -> {
                if (arg == ThorStatePayload.CAUGHT) {
                    ThorMotion.caught(player);
                }
            }
            case ThorStatePayload.BOMB -> ThorRise.told(arg == ThorStatePayload.PUT_OUT);
            default -> {
            }
        }
    }

    // A blow your own game threw: shown at once, the server's word of it is not taken again.
    public static void predictBlow(Entity entity, int index) {
        blow(VIEWS.computeIfAbsent(entity.getId(), id -> new View()), index);
    }

    private static void blow(View view, int index) {
        view.lastBlow = view.blow;
        view.lastBlowStart = view.blowStart;
        view.blow = index;
        view.blowStart = ticks;
    }

    public static int ticks() {
        return ticks;
    }

    // Your own game started a move: shown at once, without waiting for the server. It only decides how he moves; the
    // rest (his hammer, a charge, what he carries) is the server's word.
    public static void predict(Entity entity, int move, int arg, int flags) {
        View view = VIEWS.computeIfAbsent(entity.getId(), id -> new View());
        view.flags = view.flags & ~OWN | flags & OWN;
        view.predicted = move;
        view.predictedAt = ticks;
        start(view, move, arg);
    }

    public static void flags(Entity entity, int flags) {
        View view = VIEWS.computeIfAbsent(entity.getId(), id -> new View());
        view.flags = view.flags & ~OWN | flags & OWN;
    }

    // Your own game takes the hammer up or puts it away at once; the server's word follows.
    public static void flip(Entity entity, int flag) {
        View view = VIEWS.computeIfAbsent(entity.getId(), id -> new View());
        view.flags ^= flag;
    }

    private static void start(View view, int move, int arg) {
        view.move = move;
        view.arg = arg;
        view.start = ticks;
        if (move == ThorStatePayload.DIVE) {
            view.carried = arg - 1;
        } else if (move == ThorStatePayload.GRAB) {
            view.carried = (arg >> 1) - 1;
            view.dashed = (arg & 1) != 0;
            view.act = -1;
        }
    }

    // Every Thor in sight: his id and what is known of him.
    public static void each(BiConsumer<Integer, View> each) {
        for (Int2ObjectOpenHashMap.Entry<View> entry : VIEWS.int2ObjectEntrySet()) {
            each.accept(entry.getIntKey(), entry.getValue());
        }
    }

    // Every Thor in sight carrying a creature: his id and what is known of him.
    public static void carriers(BiConsumer<Integer, View> each) {
        for (Int2ObjectOpenHashMap.Entry<View> entry : VIEWS.int2ObjectEntrySet()) {
            if (entry.getValue().has(ThorStatePayload.CARRYING)) {
                each.accept(entry.getIntKey(), entry.getValue());
            }
        }
    }

    // Seen by others at lightning speed he is only the bolt tearing through the air.
    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        if (player != Minecraft.getInstance().player && has(player, ThorStatePayload.LIGHTNING)) {
            event.setCanceled(true);
        }
    }

    // Where a creature Thor carries hangs: from his right hand, a little ahead of him (as the server puts it).
    static Vec3 hand(LivingEntity thor, Entity held, float partialTick) {
        Vec3 look = Vec3.directionFromRotation(0.0F, thor.getViewYRot(partialTick));
        Vec3 right = look.cross(Vectors.UP).normalize();
        double size = thor.getScale();
        return thor.getPosition(partialTick).add(look.scale(0.7 * size)).add(right.scale(0.45 * size))
                .add(0.0, 0.5 * size - held.getBbHeight() * 0.55, 0.0);
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
                Vec3 at = view.move == ThorStatePayload.HOIST ? ThorGrab.overhead(living)
                        : view.move == ThorStatePayload.DIVE ? hand(living, held, 1.0F)
                        : ThorGrab.grip(living, 1.0F).subtract(0.0, ThorGrab.throat(held), 0.0);
                if (!view.has(ThorStatePayload.FLYING)) {
                    at = new Vec3(at.x, Math.max(at.y, living.getY()), at.z);
                }
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
