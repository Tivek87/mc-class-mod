package nl.tivek.multiversepowers.character.greenlantern.client.victim;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandPose;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandVictimPayload;
import nl.tivek.multiversepowers.engine.client.fx.CameraShake;
import nl.tivek.multiversepowers.engine.client.fx.ScreenFlash;
import nl.tivek.multiversepowers.engine.client.render.ConstructPainter;
import nl.tivek.multiversepowers.engine.client.render.entity.TintedBuffers;
import nl.tivek.multiversepowers.engine.math.Colors;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import nl.tivek.multiversepowers.engine.math.Vectors;

// What the evil eye, the megaphone and the tear do to a creature, as everyone sees it: strung up, a statue of hard
// light that shatters; clasping its ears, trembling, bursting in a small green blast; drawn out and torn in two.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class HandVictims {
    private static final int STATUE = 0x4FE872;
    private static final int KEEP = 100;
    private static final int STATUE_MOST = HandPose.EYE_SHATTERS - HandPose.EYE_STONE + 10;
    private static final int DEAF_MOST = HandPose.POPS - HandPose.BLARES[0] + 10;
    private static final int SPREAD_MOST = HandGroup.RING_BLASTS - HandGroup.HOLD_GRABS + 1;
    private static final double SPREAD_IN = 3.0;
    private static final int PUPPET_MOST = HandPose.EYE_SHATTERS - HandPose.EYE_STRINGS + 10;
    private static final int STRETCH_MOST = HandGroup.TEARS - HandGroup.TEAR_GRABS + 2;
    private static final int TORN_MOST = 40;
    private static final double MARIONETTE_IN = 4.0;
    private static final double FELT = 32.0;
    private static final float SPREAD_ARMS = 2.35F;
    private static final float SPREAD_LEGS = 0.5F;
    private static final double SHARDS = 16.0;
    private static final double POP_TICKS = 12.0;
    private static final double EARS_IN = 3.0;
    private static final double EARS_UP = -2.55;
    private static final double EARS_IN_TURN = 0.45;
    private static final double TREMBLE = 0.035;

    private static final class Victim {
        int hand;
        int eye = -1;
        int puppet = -1;
        int statue = -1;
        int shatter = -1;
        int deaf = -1;
        int spread = -1;
        int pop = -1;
        int stretch = -1;
        int split = -1;
        Vec3 anchor;
        double ground;
        double tall;
        AABB box;
        int latest;
    }

    private static final Map<Integer, Victim> VICTIMS = new HashMap<>();
    private static final Set<Integer> PUSHED = new HashSet<>();
    private static int ticks;
    private static int feltAt = -1;
    private static boolean redrawing;

    private HandVictims() {
    }

    public static void mark(int entity, int hand, int kind) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Victim victim = VICTIMS.computeIfAbsent(entity, id -> new Victim());
        if (kind == HandVictimPayload.GLARE) {
            victim.eye = hand;
        } else {
            victim.hand = hand;
        }
        victim.latest = ticks;
        Entity living = minecraft.level.getEntity(entity);
        if (living != null) {
            victim.box = living.getBoundingBox();
            felt(minecraft, living.position(), kind, victim.puppet < 0);
        }
        switch (kind) {
            case HandVictimPayload.PUPPET -> victim.puppet = ticks;
            case HandVictimPayload.STATUE -> victim.statue = ticks;
            case HandVictimPayload.SHATTER -> victim.shatter = ticks;
            case HandVictimPayload.DEAF -> victim.deaf = ticks;
            case HandVictimPayload.SPREAD -> victim.spread = ticks;
            case HandVictimPayload.POP -> victim.pop = ticks;
            case HandVictimPayload.STRETCH -> victim.stretch = ticks;
            case HandVictimPayload.SPLIT -> {
                victim.split = ticks;
                if (living != null) {
                    victim.anchor = living.getBoundingBox().getCenter();
                    victim.ground = HandVictimTears.groundBelow(minecraft.level, victim.anchor);
                    victim.tall = living.getBbHeight();
                }
            }
            default -> {
            }
        }
    }

    // The puppets an evil eye glares at, in the order they were strung.
    public static List<Entity> glaredAt(int eye) {
        return strung(eye, true);
    }

    // The creatures a hand has on its strings, in the order they were strung.
    public static List<Entity> puppets(int hand) {
        return strung(hand, false);
    }

    private static List<Entity> strung(int hand, boolean eye) {
        List<Entity> strung = new ArrayList<>();
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return strung;
        }
        VICTIMS.entrySet().stream().filter(entry -> (eye ? entry.getValue().eye : entry.getValue().hand) == hand
                && entry.getValue().puppet >= 0
                && entry.getValue().shatter < 0).sorted((a, b) -> Integer.compare(a.getValue().puppet,
                        b.getValue().puppet)).forEach(entry -> {
                            Entity entity = level.getEntity(entry.getKey());
                            if (entity != null) {
                                strung.add(entity);
                            }
                        });
        return strung;
    }

    // The evil eye's big moments are felt by everyone close: the view shakes, and at the worst the screen flashes.
    private static void felt(Minecraft minecraft, Vec3 at, int kind, boolean first) {
        float shake = switch (kind) {
            case HandVictimPayload.PUPPET -> first ? 0.8F : 0.0F;
            case HandVictimPayload.GLARE -> 1.4F;
            case HandVictimPayload.STATUE -> 2.0F;
            case HandVictimPayload.SHATTER -> 4.0F;
            default -> 0.0F;
        };
        if (shake <= 0.0F || feltAt == ticks) {
            return;
        }
        feltAt = ticks;
        double near = 1.0 - minecraft.gameRenderer.getMainCamera().getPosition().distanceTo(at) / FELT;
        if (near <= 0.0) {
            return;
        }
        CameraShake.add((float) (shake * near * near), kind == HandVictimPayload.SHATTER ? 14 : 8);
        if (kind == HandVictimPayload.SHATTER || kind == HandVictimPayload.STATUE) {
            ScreenFlash.add(0xD8FFE0, (float) ((kind == HandVictimPayload.SHATTER ? 0.5 : 0.25) * near), 8);
        }
    }

    // A creature's box where it is drawn this frame.
    public static AABB box(Entity entity) {
        float partialTick = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        return entity.getBoundingBox().move(entity.getPosition(partialTick).subtract(entity.position()));
    }

    // Whether a hand poses this creature itself now (strung up, spread out, deafened, a statue, stretched or torn),
    // or shattered or burst it: it never goes limp meanwhile.
    public static boolean has(int entity) {
        Victim victim = VICTIMS.get(entity);
        return victim != null && (victim.shatter >= 0 || victim.pop >= 0 || statue(victim) || puppet(victim)
                || deaf(victim) || spread(victim) || stretched(victim) || torn(victim));
    }

    public static double since(int mark, float partialTick) {
        return mark < 0 ? -1.0 : ticks - mark + partialTick;
    }

    // Both end by themselves too, should the hand that did it be gone before it shatters or bursts them.
    private static boolean statue(Victim victim) {
        return victim.statue >= 0 && victim.shatter < 0 && ticks - victim.statue < STATUE_MOST;
    }

    private static boolean puppet(Victim victim) {
        return victim.puppet >= 0 && victim.shatter < 0 && ticks - victim.puppet < PUPPET_MOST;
    }

    private static boolean deaf(Victim victim) {
        return victim.deaf >= 0 && victim.pop < 0 && ticks - victim.deaf < DEAF_MOST;
    }

    private static boolean spread(Victim victim) {
        return victim.spread >= 0 && ticks - victim.spread < SPREAD_MOST;
    }

    private static boolean stretched(Victim victim) {
        return victim.stretch >= 0 && victim.split < 0 && ticks - victim.stretch < STRETCH_MOST;
    }

    private static boolean torn(Victim victim) {
        return victim.split >= 0 && victim.anchor != null && ticks - victim.split < TORN_MOST;
    }

    // Called once the model has taken its own pose: held by the ring blast's four hands a creature is spread out in an
    // X, struggling; drawn out by the tear, an I; a deafened one clasps its ears and shakes its head.
    public static boolean pose(EntityModel<?> model, LivingEntity entity, float partialTick) {
        Victim victim = VICTIMS.get(entity.getId());
        if (victim == null || !(model instanceof HumanoidModel<?> humanoid)) {
            return false;
        }
        if (torn(victim) || stretched(victim)) {
            HandVictimTears.drawnOut(humanoid, victim.stretch < 0 ? HandVictimTears.DRAWN_IN
                    : since(victim.stretch, partialTick), torn(victim));
        } else if (spread(victim)) {
            spreadOut(humanoid, since(victim.spread, partialTick));
        } else if (puppet(victim)) {
            // A statue keeps the pose it was caught in.
            double since = victim.statue >= 0 ? victim.statue - victim.puppet : since(victim.puppet, partialTick);
            marionette(humanoid, since, entity.getId());
        } else if (deaf(victim)) {
            ears(humanoid, since(victim.deaf, partialTick));
        } else {
            return false;
        }
        humanoid.hat.copyFrom(humanoid.head);
        if (humanoid instanceof PlayerModel<?> player) {
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftSleeve.copyFrom(player.leftArm);
            player.rightPants.copyFrom(player.rightLeg);
            player.leftPants.copyFrom(player.leftLeg);
        }
        return true;
    }

    private static void spreadOut(HumanoidModel<?> humanoid, double since) {
        float on = (float) Ease.smooth(since / SPREAD_IN);
        float struggle = (float) Math.sin(since * 1.9);
        float kick = (float) Math.sin(since * 2.6 + 1.0);
        humanoid.rightArm.xRot = Mth.lerp(on, humanoid.rightArm.xRot, 0.08F * struggle);
        humanoid.rightArm.yRot = Mth.lerp(on, humanoid.rightArm.yRot, 0.0F);
        humanoid.rightArm.zRot = Mth.lerp(on, humanoid.rightArm.zRot, SPREAD_ARMS + 0.06F * struggle);
        humanoid.leftArm.xRot = Mth.lerp(on, humanoid.leftArm.xRot, -0.08F * struggle);
        humanoid.leftArm.yRot = Mth.lerp(on, humanoid.leftArm.yRot, 0.0F);
        humanoid.leftArm.zRot = Mth.lerp(on, humanoid.leftArm.zRot, -SPREAD_ARMS + 0.06F * struggle);
        humanoid.rightLeg.xRot = Mth.lerp(on, humanoid.rightLeg.xRot, 0.1F * kick);
        humanoid.rightLeg.yRot = Mth.lerp(on, humanoid.rightLeg.yRot, 0.0F);
        humanoid.rightLeg.zRot = Mth.lerp(on, humanoid.rightLeg.zRot, SPREAD_LEGS + 0.05F * kick);
        humanoid.leftLeg.xRot = Mth.lerp(on, humanoid.leftLeg.xRot, -0.1F * kick);
        humanoid.leftLeg.yRot = Mth.lerp(on, humanoid.leftLeg.yRot, 0.0F);
        humanoid.leftLeg.zRot = Mth.lerp(on, humanoid.leftLeg.zRot, -SPREAD_LEGS - 0.05F * kick);
        humanoid.head.xRot = Mth.lerp(on, humanoid.head.xRot, -0.25F + 0.1F * struggle);
        humanoid.head.yRot += on * 0.3F * kick;
    }

    // Hung on strings: arms pulled up and flopping, legs dangling and kicking, the head hanging.
    private static void marionette(HumanoidModel<?> humanoid, double since, int seed) {
        float on = (float) Ease.smooth(since / MARIONETTE_IN);
        double phase = seed * 1.7;
        float flop = (float) Math.sin(since * 0.7 + phase);
        float jerk = (float) Math.sin(since * 1.3 + phase * 0.5);
        float dangle = (float) Math.sin(since * 0.6 + phase);
        humanoid.rightArm.xRot = Mth.lerp(on, humanoid.rightArm.xRot, -2.0F + 0.4F * flop);
        humanoid.rightArm.yRot = Mth.lerp(on, humanoid.rightArm.yRot, 0.0F);
        humanoid.rightArm.zRot = Mth.lerp(on, humanoid.rightArm.zRot, 0.35F + 0.15F * jerk);
        humanoid.leftArm.xRot = Mth.lerp(on, humanoid.leftArm.xRot, -2.0F - 0.4F * jerk);
        humanoid.leftArm.yRot = Mth.lerp(on, humanoid.leftArm.yRot, 0.0F);
        humanoid.leftArm.zRot = Mth.lerp(on, humanoid.leftArm.zRot, -0.35F - 0.15F * flop);
        humanoid.rightLeg.xRot = Mth.lerp(on, humanoid.rightLeg.xRot, 0.35F * dangle);
        humanoid.rightLeg.zRot = Mth.lerp(on, humanoid.rightLeg.zRot, 0.1F);
        humanoid.leftLeg.xRot = Mth.lerp(on, humanoid.leftLeg.xRot, -0.35F * dangle);
        humanoid.leftLeg.zRot = Mth.lerp(on, humanoid.leftLeg.zRot, -0.1F);
        humanoid.head.xRot = Mth.lerp(on, humanoid.head.xRot, 0.45F + 0.1F * flop);
        humanoid.head.yRot += on * 0.25F * jerk;
    }

    private static void ears(HumanoidModel<?> humanoid, double since) {
        float on = (float) Ease.smooth(since / EARS_IN);
        float shake = (float) Math.sin(since * 2.3);
        humanoid.rightArm.xRot = Mth.lerp(on, humanoid.rightArm.xRot, (float) EARS_UP + 0.08F * shake);
        humanoid.rightArm.yRot = Mth.lerp(on, humanoid.rightArm.yRot, 0.0F);
        humanoid.rightArm.zRot = Mth.lerp(on, humanoid.rightArm.zRot, (float) EARS_IN_TURN);
        humanoid.leftArm.xRot = Mth.lerp(on, humanoid.leftArm.xRot, (float) EARS_UP - 0.08F * shake);
        humanoid.leftArm.yRot = Mth.lerp(on, humanoid.leftArm.yRot, 0.0F);
        humanoid.leftArm.zRot = Mth.lerp(on, humanoid.leftArm.zRot, (float) -EARS_IN_TURN);
        humanoid.head.yRot += on * 0.35F * (float) Math.sin(since * 1.7);
        humanoid.head.xRot = Mth.lerp(on, humanoid.head.xRot, 0.35F);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.isPaused() || VICTIMS.isEmpty()) {
            return;
        }
        ticks++;
        VICTIMS.entrySet().removeIf(entry -> {
            Victim victim = entry.getValue();
            Entity entity = minecraft.level.getEntity(entry.getKey());
            if (entity != null && !entity.isRemoved()) {
                victim.box = entity.getBoundingBox();
            }
            return ticks - victim.latest > KEEP;
        });
    }

    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Pre<?, ?> event) {
        LivingEntity entity = event.getEntity();
        Victim victim = VICTIMS.get(entity.getId());
        if (victim == null || redrawing) {
            return;
        }
        // Shattered or burst to death: only the pieces are left, not a falling body.
        if (entity.isDeadOrDying() && (victim.shatter >= 0 || victim.pop >= 0)) {
            event.setCanceled(true);
            return;
        }
        if (statue(victim) || torn(victim)) {
            event.setCanceled(true);
            redrawing = true;
            try {
                if (torn(victim)) {
                    HandVictimTears.torn(event, entity, victim.anchor, victim.ground, victim.tall,
                            since(victim.split, event.getPartialTick()), entity.getId());
                } else {
                    redraw(event, entity);
                }
            } finally {
                redrawing = false;
            }
            return;
        }
        if (stretched(victim)) {
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            HandVictimTears.stretch(pose, entity.getBbHeight(), since(victim.stretch, event.getPartialTick()));
            PUSHED.add(entity.getId());
        } else if (deaf(victim)) {
            double since = since(victim.deaf, event.getPartialTick());
            double on = Ease.smooth(since / EARS_IN);
            PoseStack pose = event.getPoseStack();
            pose.pushPose();
            double time = entity.tickCount + event.getPartialTick();
            pose.translate(on * TREMBLE * Math.sin(time * 5.1), 0.0, on * TREMBLE * Math.cos(time * 4.3));
            PUSHED.add(entity.getId());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void redraw(RenderLivingEvent.Pre event, LivingEntity entity) {
        float partialTick = event.getPartialTick();
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        event.getRenderer().render(entity, yaw, partialTick, event.getPoseStack(),
                new TintedBuffers(event.getMultiBufferSource(), STATUE), event.getPackedLight());
    }

    @SubscribeEvent
    public static void onRenderLivingPost(RenderLivingEvent.Post<?, ?> event) {
        if (PUSHED.remove(event.getEntity().getId())) {
            event.getPoseStack().popPose();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || VICTIMS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        LanternPainter painter = new LanternPainter(event.getPoseStack(), camera.getPosition(),
                (float) (minecraft.level.getGameTime() % 24000L) + partialTick, event.getFrustum());
        for (Map.Entry<Integer, Victim> entry : VICTIMS.entrySet()) {
            Victim victim = entry.getValue();
            if (victim.box == null) {
                continue;
            }
            int seed = entry.getKey();
            if (statue(victim)) {
                glow(painter, victim.box, since(victim.statue, partialTick));
            }
            double shattered = since(victim.shatter, partialTick);
            if (shattered >= 0.0 && shattered < SHARDS) {
                shatter(painter, victim.box, shattered / SHARDS, seed);
            }
            if (torn(victim)) {
                HandVictimTears.light(painter, victim.anchor, victim.ground, victim.tall,
                        since(victim.split, partialTick), seed);
            }
            double popped = since(victim.pop, partialTick);
            if (popped >= 0.0 && popped < POP_TICKS) {
                pop(painter, victim.box, popped, seed);
            }
        }
        painter.finish(minecraft.renderBuffers().bufferSource());
    }

    // The statue's bright edge: a flash as it turns, then a faint light round it.
    private static void glow(LanternPainter painter, AABB box, double since) {
        Vec3 middle = box.getCenter();
        double flash = 1.0 - Ease.smooth(since / 6.0);
        painter.flare(middle, (0.6 + 1.6 * flash) * Math.max(box.getXsize(), box.getYsize()), 0.25 + 0.75 * flash);
    }

    // The statue breaks into solid pieces of its own size, flying off and falling.
    private static void shatter(LanternPainter painter, AABB box, double apart, int seed) {
        Vec3 middle = box.getCenter();
        double wide = box.getXsize() * 0.5;
        double high = box.getYsize() * 0.5;
        double deep = box.getZsize() * 0.5;
        double[][] pieces = new double[18][];
        for (int k = 0; k < pieces.length; k++) {
            double x = (k % 2 - 0.5) * wide;
            double y = ((k / 2) % 3 - 1.0) * high * 0.66;
            double z = ((k / 6) % 3 - 1.0) * deep * 0.66;
            double size = 0.18 + 0.12 * Noise.of(seed, k, 3);
            pieces[k] = new double[] { x - size, y - size, z - size, x + size, y + size, z + size, 1.1 };
        }
        ConstructPainter.Frame frame = new ConstructPainter.Frame(middle, new Vec3(1.0, 0.0, 0.0), Vectors.UP,
                new Vec3(0.0, 0.0, 1.0), 1.0);
        painter.fling(1.4);
        painter.shattered(new ConstructPainter.Shape(pieces), frame, apart, 1.2, seed);
        painter.fling(1.0);
        if (apart < 0.25) {
            painter.flare(middle, 3.0 * (1.0 - apart / 0.25), 1.0);
        }
    }

    // A small green blast: a flash, a ring of light and sparks flung out.
    private static void pop(LanternPainter painter, AABB box, double since, int seed) {
        Vec3 middle = box.getCenter();
        double u = since / POP_TICKS;
        double fade = (1.0 - u) * (1.0 - u);
        painter.flare(middle, 2.4 * (1.0 - u) + 0.4, fade);
        painter.glowDisc(middle, 0.6 + 1.8 * Ease.smooth(u * 1.5), 0x5CFF7A, 0.7 * fade, 0.3, seed);
        for (int k = 0; k < 10; k++) {
            Vec3 way = Noise.direction(seed * 13 + k, 5);
            double far = 0.3 + 1.6 * Ease.smooth(Math.min(1.0, u * 1.6)) * (0.6 + 0.4 * Noise.of(seed, k, 6));
            Vec3 head = middle.add(way.scale(far));
            painter.edge(middle.add(way.scale(far * 0.6)), head, 0.06, fade);
        }
        painter.circle(middle, new Vec3(1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0), 0.4 + 2.2 * Ease.smooth(u), 0.08,
                0.5, Colors.alpha(0.9 * fade), Colors.alpha(0.4 * fade));
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        VICTIMS.clear();
        PUSHED.clear();
    }
}
