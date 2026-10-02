package nl.tivek.multiversepowers.character.greenlantern.client.victim;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import nl.tivek.multiversepowers.character.greenlantern.client.render.LanternPainter;
import nl.tivek.multiversepowers.character.greenlantern.hand.HandGroup;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;
import nl.tivek.multiversepowers.engine.client.render.entity.ClippedBuffers;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.engine.math.Noise;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// What the tear does to its creature, as everyone sees it: drawn out in an I between the two hands, then torn in two
// at its middle, each half carried off by its hand, dropped and tipped over, the cut sealed with green light.
final class HandVictimTears {
    static final double DRAWN_IN = 3.0;
    private static final float ARMS_UP = -3.0F;
    private static final float ARMS_IN = 0.3F;
    private static final double ANKLES = 0.1;
    private static final double RELAX = 3.0;
    private static final double GRAVITY = 0.08;
    private static final double LYING = 0.25;
    private static final double TOP_TIP = 1.6;
    private static final double BOTTOM_TIP = 1.45;
    private static final double BOTTOM_TIPS = 9.0;
    private static final double DRIFT = 0.5;
    private static final double GLOW_TICKS = 20.0;
    private static final int CUT = 0x9CFFB4;
    private static final double LET_GO = HandGroup.TEAR_LETS_GO - HandGroup.TEARS;

    private HandVictimTears() {
    }

    // Arms straight up with the hands together in the upper fist, legs straight and together in the lower one, the
    // head thrown back, writhing harder the further it is drawn out; limp once torn.
    static void drawnOut(HumanoidModel<?> humanoid, double since, boolean torn) {
        float on = (float) Ease.smooth(since / DRAWN_IN);
        double strain = torn ? 0.0 : 0.3 + 0.7 * HandGroup.tearStrain(HandGroup.TEAR_GRABS + since);
        float writhe = (float) (strain * Math.sin(since * 2.2));
        float twist = (float) (strain * Math.sin(since * 1.5 + 0.7));
        humanoid.rightArm.xRot = Mth.lerp(on, humanoid.rightArm.xRot, ARMS_UP + 0.06F * writhe);
        humanoid.rightArm.yRot = Mth.lerp(on, humanoid.rightArm.yRot, 0.0F);
        humanoid.rightArm.zRot = Mth.lerp(on, humanoid.rightArm.zRot, ARMS_IN);
        humanoid.leftArm.xRot = Mth.lerp(on, humanoid.leftArm.xRot, ARMS_UP - 0.06F * writhe);
        humanoid.leftArm.yRot = Mth.lerp(on, humanoid.leftArm.yRot, 0.0F);
        humanoid.leftArm.zRot = Mth.lerp(on, humanoid.leftArm.zRot, -ARMS_IN);
        humanoid.rightLeg.xRot = Mth.lerp(on, humanoid.rightLeg.xRot, 0.0F);
        humanoid.rightLeg.yRot = Mth.lerp(on, humanoid.rightLeg.yRot, 0.0F);
        humanoid.rightLeg.zRot = Mth.lerp(on, humanoid.rightLeg.zRot, 0.0F);
        humanoid.leftLeg.xRot = Mth.lerp(on, humanoid.leftLeg.xRot, 0.0F);
        humanoid.leftLeg.yRot = Mth.lerp(on, humanoid.leftLeg.yRot, 0.0F);
        humanoid.leftLeg.zRot = Mth.lerp(on, humanoid.leftLeg.zRot, 0.0F);
        humanoid.body.xRot = Mth.lerp(on, humanoid.body.xRot, 0.0F);
        humanoid.body.yRot = Mth.lerp(on, humanoid.body.yRot, 0.0F);
        humanoid.head.xRot = Mth.lerp(on, humanoid.head.xRot, torn ? 0.35F : -0.5F + 0.15F * writhe);
        humanoid.head.yRot = Mth.lerp(on, humanoid.head.yRot, 0.4F * twist);
        // Pulled taut between the grips: the feet point and the hands writhe at the wrists.
        Limbs.bend(humanoid, Limbs.Joint.RIGHT_ANKLE, on * 0.7F);
        Limbs.bend(humanoid, Limbs.Joint.LEFT_ANKLE, on * 0.7F);
        Limbs.bend(humanoid, Limbs.Joint.RIGHT_WRIST, on * 0.35F * writhe);
        Limbs.bend(humanoid, Limbs.Joint.LEFT_WRIST, -on * 0.35F * writhe);
    }

    // The model stretched along its height to lie between the two grips; the pose stack stands at its feet.
    static void stretch(PoseStack pose, double tall, double since) {
        double t = HandGroup.TEAR_GRABS + since;
        Vec3 top = HandGroup.tearGrip(true, tall, Vec3.ZERO, t);
        Vec3 bottom = HandGroup.tearGrip(false, tall, Vec3.ZERO, t);
        double stretch = (top.y - bottom.y) / (tall * (0.5 + HandGroup.TEAR_ARMS) - ANKLES);
        float thin = (float) (1.0 / Math.sqrt(stretch));
        pose.translate(0.0, bottom.y + 0.5 * tall, 0.0);
        pose.scale(thin, (float) stretch, thin);
        pose.translate(0.0, -ANKLES, 0.0);
    }

    // The torn creature: drawn twice, each time cut at its middle and moved as that half.
    @SuppressWarnings({ "unchecked", "rawtypes" })
    static void torn(RenderLivingEvent.Pre event, LivingEntity entity, Vec3 anchor, double ground, double tall,
            double since, int seed) {
        float partialTick = event.getPartialTick();
        float yaw = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
        Vec3 at = entity.getPosition(partialTick);
        PoseStack pose = event.getPoseStack();
        int death = entity.deathTime;
        int hurt = entity.hurtTime;
        // The dying body would tip over sideways and flush red; the halves fall their own way.
        entity.deathTime = 0;
        entity.hurtTime = 0;
        try {
            for (int k = 0; k < 2; k++) {
                boolean top = k == 0;
                pose.pushPose();
                pose.translate(anchor.x - at.x, anchor.y - at.y, anchor.z - at.z);
                half(pose, top, anchor.y - ground, tall, since, seed);
                Vector3f point = pose.last().pose().transformPosition(0.0F, (float) (0.5 * tall), 0.0F,
                        new Vector3f());
                Vector3f normal = pose.last().normal().transform(new Vector3f(0.0F, top ? 1.0F : -1.0F, 0.0F))
                        .normalize();
                ClippedBuffers buffers = new ClippedBuffers(event.getMultiBufferSource(), point, normal, CUT);
                event.getRenderer().render(entity, yaw, partialTick, pose, buffers, event.getPackedLight());
                buffers.close();
                pose.popPose();
            }
        } finally {
            entity.deathTime = death;
            entity.hurtTime = hurt;
        }
    }

    // Where a half is, from the creature's own space (feet at the origin) to the anchor, its middle as it tore: held
    // by its hand and springing back from the stretch, then dropped, the top half tumbling away from the bottom one
    // and the bottom one tipping over from its feet.
    private static void half(PoseStack pose, boolean top, double above, double tall, double since, int seed) {
        double pivot = top ? tall * (0.5 + HandGroup.TEAR_ARMS) : ANKLES;
        double spring = 1.0 + (HandGroup.TEAR_LONGEST - 1.0) * (1.0 - Ease.smooth(since / RELAX))
                + 0.06 * Math.sin(since * 2.4) * Math.exp(-since / 4.0);
        double free = Math.max(0.0, since - LET_GO);
        Vec3 grip = HandGroup.tearGrip(top, tall, Vec3.ZERO, HandGroup.TEARS + Math.min(since, LET_GO));
        double floor = (top ? LYING : ANKLES) - above;
        double lands = Math.sqrt(2.0 * Math.max(0.0, grip.y - floor) / GRAVITY);
        double fall = Math.min(free, lands);
        double down = lands <= 0.0 ? 1.0 : fall / lands;
        double tip = top ? TOP_TIP * Ease.smooth(down) : BOTTOM_TIP * Math.pow(Math.min(1.0, free / BOTTOM_TIPS), 2.0);
        double drift = top ? DRIFT * down : 0.0;
        double turn = seed * 2.39996;
        double awayX = Math.cos(turn);
        double awayZ = Math.sin(turn);
        pose.translate(-awayX * drift, grip.y - 0.5 * GRAVITY * fall * fall, -awayZ * drift);
        // About up x away: the bottom half's cut end tips away, the top half's the other way.
        pose.mulPose(new Quaternionf().rotateAxis((float) tip, (float) awayZ, 0.0F, (float) -awayX));
        float thin = (float) (1.0 / Math.sqrt(spring));
        pose.scale(thin, (float) spring, thin);
        pose.translate(0.0, -pivot, 0.0);
    }

    // The cuts glow: a flash as it tears, then a fading light, and sparks thrown off.
    static void light(LanternPainter painter, Vec3 anchor, double ground, double tall, double since, int seed) {
        double flash = 1.0 - Ease.smooth(since / 6.0);
        double fade = 1.0 - Ease.smooth(since / GLOW_TICKS);
        for (int k = 0; k < 2; k++) {
            PoseStack pose = new PoseStack();
            half(pose, k == 0, anchor.y - ground, tall, since, seed);
            Vector3f cut = pose.last().pose().transformPosition(0.0F, (float) (0.5 * tall), 0.0F, new Vector3f());
            Vec3 at = anchor.add(cut.x(), cut.y(), cut.z());
            painter.flare(at, 0.4 + 1.4 * flash, 0.3 * fade + 0.7 * flash);
            for (int s = 0; s < 6; s++) {
                Vec3 way = Noise.direction(seed * 7 + s + k * 31, 17);
                double out = 0.2 + 1.2 * Ease.smooth(Math.min(1.0, since / 8.0));
                Vec3 head = at.add(way.scale(out)).subtract(0.0, 0.02 * since * since, 0.0);
                painter.edge(at.add(way.scale(out * 0.6)), head, 0.04, 0.9 * flash);
            }
        }
    }

    // The top of the ground below a point, where the halves come to lie.
    static double groundBelow(Level level, Vec3 from) {
        BlockPos.MutableBlockPos pos = BlockPos.containing(from).mutable();
        for (int k = 0; k < 8; k++) {
            VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.max(Direction.Axis.Y);
            }
            pos.move(Direction.DOWN);
        }
        return from.y - 3.0;
    }
}
