package nl.tivek.multiversepowers.engine.client.pose;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.config.client.ClientSettings;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

// The poses powers give a creature on top of its own animation, in one place and one order: each stage's layers,
// then (while the world is drawn) a short fade whenever a layer starts or stops, so a pose never jumps, and last the
// guard that keeps limbs out of the body. A ragdoll may take the whole body after that.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Poses {
    public enum Stage {
        // Inside a player model's own setupAnim, so the pose holds wherever that model is posed.
        MODEL,
        // After any creature's model took its pose, as the creature is drawn.
        CREATURE
    }

    @FunctionalInterface
    public interface Layer {
        // Poses the model; true when it changed anything this time.
        boolean pose(EntityModel<?> model, LivingEntity entity, float partialTick);
    }

    private static final int MOST = 32;
    private static final int PARTS = 6;
    private static final int VALUES = 6;
    // Ticks a pose takes to fade in or out when a layer starts or stops.
    private static final double FADE = 4.0;
    // Unseen this long, a creature's last pose is forgotten: it will not fade from where it was long ago.
    private static final int FORGET = 3;
    private static final int DROP = 100;

    private static final List<Layer> MODEL_LAYERS = new ArrayList<>();
    private static final List<Layer> CREATURE_LAYERS = new ArrayList<>();
    private static final Int2ObjectOpenHashMap<Memory> MEMORY = new Int2ObjectOpenHashMap<>();
    private static final float[] NOW = new float[PARTS * VALUES];
    private static final int JOINTS = Limbs.Joint.values().length;
    private static final Quaternionf[] JOINTS_NOW = turns();
    private static final Quaternionf MIXED = new Quaternionf();
    // A joint turned less than this (the cosine of half its angle) is straight.
    private static final float STRAIGHT = 0.99999F;
    private static int ticks;
    // The matrix the creature being posed is drawn with, while its creature-stage layers pose it (null otherwise).
    @Nullable
    private static Matrix4f drawing;

    private static final class Memory {
        long model;
        long on;
        boolean drawn;
        final float[] last = new float[PARTS * VALUES];
        final float[] from = new float[PARTS * VALUES];
        // Every joint's turn as last drawn, and as the fade under way began from.
        final Quaternionf[] joints = turns();
        final Quaternionf[] fromJoints = turns();
        double fading = -1.0;
        // When a pose began on a body no layer posed before: the shoulders come along as it fades in.
        double rising = -1.0;
        // How far the shoulder blades followed the arms last time, and when the last layer stopped.
        float followed;
        float followFrom;
        int seen;
    }

    private static Quaternionf[] turns() {
        Quaternionf[] turns = new Quaternionf[Limbs.Joint.values().length];
        for (int i = 0; i < turns.length; i++) {
            turns[i] = new Quaternionf();
        }
        return turns;
    }

    private Poses() {
    }

    // For a layer: the matrix from the creature's model space (blocks) to the camera, when known.
    @Nullable
    public static Matrix4f drawn() {
        return drawing;
    }

    public static void layer(Stage stage, Layer layer) {
        List<Layer> layers = stage == Stage.MODEL ? MODEL_LAYERS : CREATURE_LAYERS;
        if (layers.size() >= MOST) {
            throw new IllegalStateException("At most " + MOST + " pose layers in a stage");
        }
        layers.add(layer);
    }

    // `drawn`: the matrix the model is drawn with, when known (the creature stage), for the feet to find the ground.
    public static void apply(Stage stage, EntityModel<?> model, LivingEntity entity, float partialTick,
            @Nullable Matrix4f drawn) {
        List<Layer> layers = stage == Stage.MODEL ? MODEL_LAYERS : CREATURE_LAYERS;
        long on = 0L;
        drawing = stage == Stage.CREATURE ? drawn : null;
        try {
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i).pose(model, entity, partialTick)) {
                    on |= 1L << i;
                }
            }
        } finally {
            drawing = null;
        }
        if (!EntityPass.inWorld()) {
            return;
        }
        if (!(model instanceof HumanoidModel<?> humanoid)) {
            plant(model, entity, partialTick, drawn);
            return;
        }
        Memory memory = MEMORY.get(entity.getId());
        if (memory == null) {
            memory = new Memory();
            MEMORY.put(entity.getId(), memory);
        }
        if (stage == Stage.MODEL) {
            memory.model = on;
            return;
        }
        long all = on | memory.model << MOST;
        memory.model = 0L;
        read(humanoid, NOW);
        Limbs.turns(humanoid, JOINTS_NOW);
        double now = ticks + partialTick;
        boolean fresh = !memory.drawn || ticks - memory.seen > FORGET;
        if (!fresh && all != memory.on) {
            System.arraycopy(memory.last, 0, memory.from, 0, NOW.length);
            for (int j = 0; j < JOINTS; j++) {
                memory.fromJoints[j].set(memory.joints[j]);
            }
            memory.fading = now;
            memory.rising = memory.on == 0L ? now : memory.rising;
            // The last layer stopping: its shoulders settle back as the rest of it fades.
            memory.followFrom = all == 0L ? memory.followed : 0.0F;
        } else if (fresh) {
            memory.fading = -1.0;
            memory.rising = -1.0;
            memory.followFrom = 0.0F;
        }
        memory.on = all;
        float fade = 1.0F;
        if (memory.fading >= 0.0) {
            double t = (now - memory.fading) / FADE;
            if (t >= 1.0 || t < 0.0) {
                memory.fading = -1.0;
            } else {
                // The joints (elbows, knees, wrists, ankles, waist, pelvis) fade along with the limbs they bend.
                fade = (float) Ease.smooth(t);
                blend(memory.from, NOW, fade);
                write(humanoid, NOW);
                for (Limbs.Joint joint : Limbs.Joint.values()) {
                    int j = joint.ordinal();
                    Quaternionf from = memory.fromJoints[j];
                    Quaternionf to = JOINTS_NOW[j];
                    if (Math.abs(from.w) > STRAIGHT && Math.abs(to.w) > STRAIGHT) {
                        continue;
                    }
                    if (from.dot(to) < 0.0F) {
                        from.set(-from.x, -from.y, -from.z, -from.w);
                    }
                    Limbs.set(humanoid, joint, MIXED.set(from).slerp(to, fade));
                }
            }
        }
        // As the pose was before its shoulder blades follow its arms, so a fade never shrugs a shoulder twice.
        read(humanoid, memory.last);
        Limbs.turns(humanoid, memory.joints);
        float follow = 0.0F;
        if (all != 0L) {
            double t = memory.rising < 0.0 ? 1.0 : (now - memory.rising) / FADE;
            if (t >= 1.0 || t < 0.0) {
                memory.rising = -1.0;
                t = 1.0;
            }
            follow = (float) Ease.smooth(t);
        } else if (memory.fading >= 0.0) {
            follow = memory.followFrom * (1.0F - fade);
        }
        Shoulders.follow(humanoid, follow);
        memory.followed = follow;
        memory.drawn = true;
        memory.seen = ticks;
        plant(model, entity, partialTick, drawn);
        PoseGuard.guard(humanoid);
        humanoid.hat.copyFrom(humanoid.head);
        if (humanoid instanceof PlayerModel<?> player) {
            player.jacket.copyFrom(player.body);
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        }
    }

    private static void plant(EntityModel<?> model, LivingEntity entity, float partialTick, @Nullable Matrix4f drawn) {
        if (drawn != null && ClientSettings.footPlanting()) {
            FootPlanting.plant(model, entity, drawn, partialTick, ticks);
        }
    }

    private static ModelPart part(HumanoidModel<?> model, int i) {
        return switch (i) {
            case 0 -> model.head;
            case 1 -> model.body;
            case 2 -> model.rightArm;
            case 3 -> model.leftArm;
            case 4 -> model.rightLeg;
            default -> model.leftLeg;
        };
    }

    private static void read(HumanoidModel<?> model, float[] into) {
        for (int i = 0; i < PARTS; i++) {
            ModelPart part = part(model, i);
            int o = i * VALUES;
            into[o] = part.x;
            into[o + 1] = part.y;
            into[o + 2] = part.z;
            into[o + 3] = part.xRot;
            into[o + 4] = part.yRot;
            into[o + 5] = part.zRot;
        }
    }

    private static void write(HumanoidModel<?> model, float[] from) {
        for (int i = 0; i < PARTS; i++) {
            ModelPart part = part(model, i);
            int o = i * VALUES;
            part.setPos(from[o], from[o + 1], from[o + 2]);
            part.setRotation(from[o + 3], from[o + 4], from[o + 5]);
        }
    }

    // Moves `to` back towards `from` by 1 - w: turns the short way round.
    private static void blend(float[] from, float[] to, float w) {
        for (int i = 0; i < to.length; i++) {
            boolean angle = i % VALUES >= 3;
            float gap = angle ? Mth.wrapDegrees((to[i] - from[i]) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD : to[i] - from[i];
            to[i] = from[i] + gap * w;
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        ticks++;
        if (ticks % 20 == 0) {
            MEMORY.values().removeIf(memory -> ticks - memory.seen > DROP);
            FootPlanting.forgetOld(ticks, DROP);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MEMORY.clear();
        FootPlanting.forget();
    }
}
