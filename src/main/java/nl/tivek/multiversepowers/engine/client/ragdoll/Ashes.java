package nl.tivek.multiversepowers.engine.client.ragdoll;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.render.ShadedBuffers;
import nl.tivek.multiversepowers.engine.math.Colors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

// A creature a power burns to ash: as it dies it glows red hot, chars black and turns grey, keeping its size and its
// pose, then falls apart all at once into ash in its own shape. It never goes limp and leaves no body.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Ashes {
    private static final int EMBER = 0xFF8A3C;
    private static final int CHAR = 0x2E2926;
    // Ash: a grey tint over a creature washed half white, so only a trace of its colours shows through.
    private static final int ASH = 0x6E6A66;
    private static final float ASH_WHITE = 0.6F;
    // Ticks into dying (a dying creature is drawn for 20) it glows by, is charred by, is ash by and falls apart at.
    private static final float HOT = 3.0F;
    private static final float CHARRED = 7.0F;
    private static final float ASHEN = 12.0F;
    private static final float CRUMBLE = 16.0F;
    // Ash for each cubic block of body as it falls apart, and the least and the most for one part.
    private static final double DUST = 900.0;
    private static final int LEAST = 4;
    private static final int MOST = 40;
    private static final int KEEP = 60;
    private static final ParticleOptions FALLING_ASH = new BlockParticleOption(ParticleTypes.FALLING_DUST,
            Blocks.LIGHT_GRAY_CONCRETE_POWDER.defaultBlockState());
    private static final Int2IntOpenHashMap BURNING = new Int2IntOpenHashMap();
    private static final IntOpenHashSet CRUMBLED = new IntOpenHashSet();
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Matrix4f FRAME = new Matrix4f();
    private static final Vector3f POINT = new Vector3f();
    private static boolean redrawing;
    private static int ticks;

    private Ashes() {
    }

    public static void burn(int entity) {
        BURNING.put(entity, ticks);
        CRUMBLED.remove(entity);
    }

    static boolean burning(int entity) {
        return BURNING.containsKey(entity);
    }

    // Whether this dying creature turns to ash, so it does not vanish in a puff of smoke as well.
    public static boolean burnt(LivingEntity entity) {
        return entity.isDeadOrDying() && BURNING.containsKey(entity.getId());
    }

    // Instead of the creature, its burning shape standing as it stood: red hot, charred, grey, then gone.
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (redrawing || !burnt(entity)) {
            return;
        }
        event.setCanceled(true);
        if (CRUMBLED.contains(entity.getId())) {
            return;
        }
        float t = entity.deathTime + event.getPartialTick();
        int deathTime = entity.deathTime;
        int hurtTime = entity.hurtTime;
        // Not turning over as the dying do: it burns where it stands.
        entity.deathTime = 0;
        entity.hurtTime = 0;
        redrawing = true;
        try {
            redraw(event, entity, tint(t), t < HOT + 1.0F, ASH_WHITE * Mth.clamp((t - CHARRED) / (ASHEN - CHARRED),
                    0.0F, 1.0F));
        } finally {
            redrawing = false;
            entity.deathTime = deathTime;
            entity.hurtTime = hurtTime;
        }
    }

    private static int tint(float t) {
        if (t < HOT) {
            return Colors.mix(0xFFFFFF, EMBER, t / HOT);
        }
        if (t < CHARRED) {
            return Colors.mix(EMBER, CHAR, (t - HOT) / (CHARRED - HOT));
        }
        return Colors.mix(CHAR, ASH, (t - CHARRED) / (ASHEN - CHARRED));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void redraw(RenderLivingEvent.Pre event, LivingEntity entity, int tint, boolean glow,
            float whiten) {
        float partialTick = event.getPartialTick();
        event.getRenderer().render(entity, Mth.lerp(partialTick, entity.yRotO, entity.getYRot()), partialTick,
                event.getPoseStack(), new ShadedBuffers(event.getMultiBufferSource(), tint, glow, whiten),
                event.getPackedLight());
    }

    // Called right after a creature's model took its pose (drawn: its pose stack then): one burnt through falls
    // apart into ash, each part a cloud in its own shape.
    public static void shape(EntityModel<?> model, LivingEntity entity, float partialTick, Matrix4f drawn) {
        if (!redrawing || !burnt(entity) || CRUMBLED.contains(entity.getId())
                || entity.deathTime + partialTick < CRUMBLE) {
            return;
        }
        CRUMBLED.add(entity.getId());
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        List<ModelParts.Part> parts = ModelParts.of(model);
        if (parts == null) {
            box(level, entity.getBoundingBox());
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        for (ModelParts.Part part : parts) {
            float[] b = part.bounds();
            ModelParts.frame(model, drawn, part, FRAME);
            double scale = Ragdoll.scaleOf(FRAME);
            double volume = (b[3] - b[0]) * (b[4] - b[1]) * (b[5] - b[2]) / 4096.0 * scale * scale * scale;
            int count = Mth.clamp((int) (volume * DUST), LEAST, MOST);
            for (int i = 0; i < count; i++) {
                FRAME.transformPosition(Mth.lerp(RANDOM.nextFloat(), b[0], b[3]) / 16.0F,
                        Mth.lerp(RANDOM.nextFloat(), b[1], b[4]) / 16.0F, Mth.lerp(RANDOM.nextFloat(), b[2], b[5])
                                / 16.0F, POINT);
                dust(level, camera.x + POINT.x, camera.y + POINT.y, camera.z + POINT.z, i);
            }
        }
    }

    // What a model not built the usual way leaves: ash through its box.
    private static void box(ClientLevel level, AABB box) {
        int count = Mth.clamp((int) (box.getXsize() * box.getYsize() * box.getZsize() * DUST * 0.3), LEAST, MOST * 4);
        for (int i = 0; i < count; i++) {
            dust(level, Mth.lerp(RANDOM.nextDouble(), box.minX, box.maxX), Mth.lerp(RANDOM.nextDouble(), box.minY,
                    box.maxY), Mth.lerp(RANDOM.nextDouble(), box.minZ, box.maxZ), i);
        }
    }

    // Mostly pale dust falling where it was, flakes of ash on the air, now and then a speck or a wisp of smoke.
    private static void dust(ClientLevel level, double x, double y, double z, int i) {
        double sx = (RANDOM.nextDouble() - 0.5) * 0.04;
        double sz = (RANDOM.nextDouble() - 0.5) * 0.04;
        switch (i % 8) {
            case 0, 1, 2, 3, 4 -> level.addParticle(FALLING_ASH, x, y, z, 0.0, 0.0, 0.0);
            case 5, 6 -> level.addParticle(ParticleTypes.WHITE_ASH, x, y, z, sx, 0.01, sz);
            default -> level.addParticle(i % 16 == 7 ? ParticleTypes.SMOKE : ParticleTypes.ASH, x, y, z, sx, 0.01,
                    sz);
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused() || BURNING.isEmpty()) {
            return;
        }
        ticks++;
        BURNING.int2IntEntrySet().removeIf(entry -> {
            Entity entity = level.getEntity(entry.getIntKey());
            if (entity == null || ticks - entry.getIntValue() > KEEP) {
                CRUMBLED.remove(entry.getIntKey());
                return true;
            }
            if (entity instanceof LivingEntity living && burnt(living) && !CRUMBLED.contains(living.getId())) {
                smoulder(level, living);
            }
            return false;
        });
    }

    // Flames lick off it while it glows, smoke rises as it chars, and flakes of ash drift off it once it is grey.
    private static void smoulder(ClientLevel level, LivingEntity entity) {
        AABB box = entity.getBoundingBox();
        float t = entity.deathTime;
        for (int i = 0; i < 3; i++) {
            double x = Mth.lerp(RANDOM.nextDouble(), box.minX, box.maxX);
            double y = Mth.lerp(RANDOM.nextDouble(), box.minY, box.maxY);
            double z = Mth.lerp(RANDOM.nextDouble(), box.minZ, box.maxZ);
            double sx = (RANDOM.nextDouble() - 0.5) * 0.03;
            double sz = (RANDOM.nextDouble() - 0.5) * 0.03;
            if (t < HOT) {
                // Few and small: a flame lives on well after the glow is gone.
                if (i == 0) {
                    level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, sx, 0.01, sz);
                }
            } else if (t < ASHEN) {
                level.addParticle(ParticleTypes.SMOKE, x, box.maxY - 0.2, z, sx, 0.04, sz);
            } else {
                level.addParticle(ParticleTypes.WHITE_ASH, x, box.maxY - 0.1, z, sx, 0.02, sz);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BURNING.clear();
        CRUMBLED.clear();
    }
}
