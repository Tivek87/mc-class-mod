package nl.tivek.multiversepowers.engine.client.ragdoll;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import javax.annotation.Nullable;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.math.Ease;

// A creature that got up faces the way it rose, drawn so until it walks or turns of its own; then it turns to its own
// way over TURN ticks, as it would turn anyway. Only what this player sees.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Facings {
    private static final int TURN = 10;
    // Moved this far (blocks) or turned this far (degrees) of its own since it stood, it turns to its own way.
    private static final double MOVED = 0.05;
    private static final float TURNED = 2.0F;
    // Drawn this near (degrees) its own way, it is left as it is.
    private static final float NEAR = 1.0F;
    // Getting up, its head turns to its own look from this far on.
    private static final float TURN_FROM = 0.65F;
    private static final float[] YAWS = new float[4];
    // The creature drawn now turned the way it rises or rose, its own yaws kept in YAWS, and how many drawings of it
    // run inside that one (a power drawing it again its own way).
    @Nullable
    private static LivingEntity turned;
    private static int inside;

    private static final class Facing {
        private final LivingEntity entity;
        // How far (degrees) it is drawn turned from its own way, its own way and where it stood as it got up, and
        // ticks into turning to its own way (-1 before).
        private final float offset;
        private final float own;
        private final double x;
        private final double z;
        private int turning = -1;

        private Facing(LivingEntity entity, float offset) {
            this.entity = entity;
            this.offset = offset;
            this.own = entity.yBodyRot;
            this.x = entity.getX();
            this.z = entity.getZ();
        }
    }

    private static final Int2ObjectOpenHashMap<Facing> ALL = new Int2ObjectOpenHashMap<>();

    private Facings() {
    }

    // It stands, drawn facing `yaw` (the game's degrees).
    static void rose(LivingEntity entity, float yaw) {
        float offset = Mth.wrapDegrees(yaw - entity.yBodyRot);
        if (Math.abs(offset) > NEAR) {
            ALL.put(entity.getId(), new Facing(entity, offset));
        } else {
            ALL.remove(entity.getId());
        }
    }

    static void tick() {
        ALL.values().removeIf(facing -> {
            LivingEntity entity = facing.entity;
            if (entity.isRemoved() || !entity.isAlive()) {
                return true;
            }
            if (facing.turning < 0) {
                double dx = entity.getX() - facing.x;
                double dz = entity.getZ() - facing.z;
                if (dx * dx + dz * dz > MOVED * MOVED
                        || Math.abs(Mth.wrapDegrees(entity.yBodyRot - facing.own)) > TURNED) {
                    facing.turning = 0;
                }
                return false;
            }
            return ++facing.turning >= TURN;
        });
    }

    // How far (degrees) the creature is drawn turned from its own way now.
    static float offset(LivingEntity entity, float partialTick) {
        Facing facing = ALL.get(entity.getId());
        if (facing == null || facing.entity != entity) {
            return 0.0F;
        }
        if (facing.turning < 0) {
            return facing.offset;
        }
        return facing.offset * (1.0F - (float) Ease.smoother((facing.turning + partialTick) / TURN));
    }

    // A creature getting up is drawn facing the way its body rises, not turning on the ground, its head turning to its
    // own look as it comes to stand; standing, it keeps facing that way until it moves or turns of its own (Facings).
    @SubscribeEvent
    public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!EntityPass.inWorld()) {
            return;
        }
        // Drawn again inside its own drawing: it is turned already.
        if (turned == entity) {
            inside++;
            return;
        }
        if (turned != null) {
            restore();
        }
        float partialTick = event.getPartialTick();
        Ragdoll doll = Ragdolls.live(entity);
        if (doll != null && doll.phase == Ragdoll.Phase.UP) {
            float u = (doll.up + partialTick) / doll.kind().ticks;
            float own = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
            float look = Mth.wrapDegrees(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot) - own);
            float share = (float) Ease.smoother((u - TURN_FROM) / (1.0F - TURN_FROM));
            keep(entity);
            entity.yBodyRot = doll.riseYaw;
            entity.yBodyRotO = doll.riseYaw;
            entity.yHeadRot = doll.riseYaw + look * share;
            entity.yHeadRotO = entity.yHeadRot;
            return;
        }
        float offset = offset(entity, partialTick);
        if (offset != 0.0F && !Ragdolls.claimed(entity)) {
            keep(entity);
            entity.yBodyRot += offset;
            entity.yBodyRotO += offset;
            entity.yHeadRot += offset;
            entity.yHeadRotO += offset;
        }
    }

    // The creature drawn now has its own yaws kept, to be put back once it is drawn.
    private static void keep(LivingEntity entity) {
        turned = entity;
        YAWS[0] = entity.yBodyRot;
        YAWS[1] = entity.yBodyRotO;
        YAWS[2] = entity.yHeadRot;
        YAWS[3] = entity.yHeadRotO;
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (turned != event.getEntity()) {
            return;
        }
        if (inside > 0) {
            inside--;
        } else {
            restore();
        }
    }

    // A drawing called off once its yaws were turned (a power hides the creature) never comes to its end: they are put
    // back at once, or they would be turned again on top every frame.
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRenderLivingCalledOff(RenderLivingEvent.Pre<?, ?> event) {
        if (event.isCanceled() && turned == event.getEntity() && inside == 0) {
            restore();
        }
    }

    private static void restore() {
        turned.yBodyRot = YAWS[0];
        turned.yBodyRotO = YAWS[1];
        turned.yHeadRot = YAWS[2];
        turned.yHeadRotO = YAWS[3];
        turned = null;
        inside = 0;
    }

    static void forget(int entity) {
        ALL.remove(entity);
    }

    static void clear() {
        ALL.clear();
    }
}
