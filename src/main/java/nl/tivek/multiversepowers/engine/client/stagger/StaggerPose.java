package nl.tivek.multiversepowers.engine.client.stagger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import nl.tivek.multiversepowers.engine.client.model.ModelParts;
import nl.tivek.multiversepowers.engine.client.pose.Limbs;
import nl.tivek.multiversepowers.engine.client.pose.Poses;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.entity.impact.ImpactPayload;
import nl.tivek.multiversepowers.engine.math.Ease;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// How a reeling creature is drawn (Reaction). A person is posed whole through Stance: its trunk leaning from the
// blow, the pelvis taking part of it; its hips sunk as its knees take it; its feet where they are planted or stepping,
// flat at the ankles; a hand on the wound, palm to it, and the other arm out for balance, the shoulder blades
// following; its head snapped from the blow and then looking down at the wound. Any other creature bends at its waist
// from the blow and jerks its head. Eased in at once and out at the end.
final class StaggerPose {
    static final int FADE_OUT = 6;
    private static final float FADE_IN = 1.5F;
    // Where a person's trunk bends (pixels down from the neck), and the share of the lean its chest takes again.
    private static final float WAIST_Y = 6.0F;
    private static final float PELVIS_Y = 9.0F;
    private static final float CHEST_SHARE = 0.5F;
    private static final float HIPS_SHARE = 0.6F;
    // Hunched over the wound in pain (radians), looking down at it and turned to its side.
    private static final float HUNCH = 0.18F;
    private static final float LOOK_DOWN = 0.45F;
    private static final float LOOK_SIDE = 0.04F;
    // A hand held this far off the wound (pixels), and how far a flinching hand gets to it.
    private static final float OFF_SKIN = 1.2F;
    private static final float FLINCH_REACH = 0.5F;
    // The other arm out for balance: its hand this far out, down and ahead of its shoulder (pixels), for a lean
    // this far.
    private static final float OUT = 5.0F;
    private static final float DOWN = 5.0F;
    private static final float AHEAD = 6.0F;
    private static final float FULL_LEAN = 0.35F;
    // A creature's waist bent from the blow at this share of the person's lean, its head jerked at this share.
    private static final float CREATURE_BEND = 0.6F;
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);

    private static final Map<EntityModel<?>, Optional<ModelPart>> HEADS = new WeakHashMap<>();
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Quaternionf PELVIS = new Quaternionf();
    private static final Quaternionf TURN = new Quaternionf();
    private static final Quaternionf PALM = new Quaternionf();
    private static final Vector3f HIPS = new Vector3f();
    private static final Vector3f NECK = new Vector3f();
    private static final Vector3f FOOT = new Vector3f();
    private static final Vector3f HAND = new Vector3f();
    private static final Vector3f AT = new Vector3f();
    private static final Vector3f WOUND = new Vector3f();
    private static final Vector3f V = new Vector3f();
    private static final Vector3f POLE = new Vector3f();
    private static final Matrix3f AXES = new Matrix3f();
    private static final Matrix4f BACK = new Matrix4f();

    private StaggerPose() {
    }

    static boolean pose(EntityModel<?> model, Reaction reaction, float partialTick) {
        float t = reaction.age + partialTick;
        float w = weight(reaction, t);
        if (w <= 1.0E-3F) {
            return false;
        }
        if (model instanceof HumanoidModel<?> person) {
            person(person, reaction, t, partialTick, w);
        } else {
            creature(model, reaction, partialTick, w);
        }
        return true;
    }

    private static float weight(Reaction reaction, float t) {
        float in = Math.min(1.0F, t / FADE_IN);
        float out = 1.0F - Mth.clamp((t - reaction.ticks) / FADE_OUT, 0.0F, 1.0F);
        return (float) (Ease.smooth(in) * Ease.smooth(out));
    }

    private static void person(HumanoidModel<?> person, Reaction r, float t, float partialTick, float w) {
        float pitch = Mth.lerp(partialTick, r.leanWas[0], r.lean[0]) * w;
        float roll = Mth.lerp(partialTick, r.leanWas[1], r.lean[1]) * w;
        float twist = Mth.lerp(partialTick, r.leanWas[2], r.lean[2]) * w;
        float pain = r.kind == ImpactPayload.Reaction.STAGGER && r.where != Reaction.Where.LEG
                ? HUNCH * (float) Ease.smooth(Mth.clamp((t - 5.0F) / 8.0F, 0.0F, 1.0F)) * r.strength * w : 0.0F;
        LEAN.rotationY(twist).rotateX(pitch).rotateZ(roll);
        WAIST.rotationX(pitch * CHEST_SHARE + pain);
        HIPS.set(0.0F, Stance.HIP_Y + r.sink(t) * w, 0.0F);
        Stance.trunk(person, HIPS, LEAN, WAIST);
        Matrix4f drawn = Poses.drawn();
        for (int k = 0; k < 2; k++) {
            boolean right = k == 0;
            Reaction.Foot foot = r.feet[k];
            double x = Mth.lerp(partialTick, foot.wasX, foot.x);
            double y = Mth.lerp(partialTick, foot.wasY, foot.y);
            double z = Mth.lerp(partialTick, foot.wasZ, foot.z);
            float lift = 0.0F;
            if (foot.step >= 0) {
                float u = Mth.clamp((foot.step + partialTick) / Reaction.STEP_TICKS, 0.0F, 1.0F);
                lift = (float) (Reaction.LIFT * Mth.sin(u * Mth.PI));
            }
            model(r.entity, drawn, partialTick, x, y + lift, z, AT);
            Stance.foot(person, right, FOOT);
            FOOT.lerp(AT, w);
            Stance.leg(person, right, FOOT, KNEE);
            if (foot.step < 0) {
                Stance.flat(person, right, w);
            }
        }
        boolean flinch = r.kind == ImpactPayload.Reaction.FLINCH;
        float reach = flinch ? FLINCH_REACH * Mth.sin(Mth.clamp(t / r.ticks, 0.0F, 1.0F) * Mth.PI)
                : (float) Ease.smooth(Mth.clamp((t - 1.5F) / 5.0F, 0.0F, 1.0F));
        boolean holds = woundNow(person, r, WOUND);
        for (int k = 0; k < 2; k++) {
            boolean right = k == 0;
            boolean holding = holds && (right == r.rightHand || r.bothHands);
            if (holding) {
                hold(person, r, right, reach * w);
            } else if (!flinch) {
                balance(person, right, pitch, roll, w);
            }
        }
        float look = flinch ? 0.0F : (float) Ease.smooth(Mth.clamp((t - 4.0F) / 6.0F, 0.0F, 1.0F));
        person.head.xRot += (Mth.lerp(partialTick, r.headWas, r.head) + LOOK_DOWN * look) * w;
        person.head.yRot += -r.wound.x * LOOK_SIDE * look * w;
        person.hat.copyFrom(person.head);
    }

    // Where the wound is on the body as it is posed now (model pixels), and whether a hand can hold it there: not on
    // its back, not low on a leg.
    private static boolean woundNow(HumanoidModel<?> person, Reaction r, Vector3f out) {
        Vector3f wound = r.wound;
        Stance.neck(NECK);
        Stance.chest(CHEST);
        switch (r.where) {
            case HEAD -> {
                TURN.rotationZYX(person.head.zRot, person.head.yRot, person.head.xRot);
                TURN.transform(out.set(wound)).add(NECK);
            }
            case ARM -> {
                ModelPart arm = wound.x < 0.0F ? person.rightArm : person.leftArm;
                TURN.rotationZYX(arm.zRot, arm.yRot, arm.xRot);
                out.set(wound).sub(wound.x < 0.0F ? -Stance.SHOULDER_X : Stance.SHOULDER_X, Stance.SHOULDER_Y, 0.0F);
                TURN.transform(out).add(arm.x, arm.y, arm.z);
            }
            case LEG -> {
                ModelPart leg = wound.x < 0.0F ? person.rightLeg : person.leftLeg;
                TURN.rotationZYX(leg.zRot, leg.yRot, leg.xRot);
                out.set(wound).sub(wound.x < 0.0F ? -Stance.HIP_X : Stance.HIP_X, Stance.HIP_Y, 0.0F);
                TURN.transform(out).add(leg.x, leg.y, leg.z);
                // Only a wound high on the thigh is within reach.
                return wound.y < Stance.HIP_Y + Stance.THIGH * 0.6F && wound.z < 1.5F;
            }
            default -> {
                // The chest turns with the neck, the belly below the waist with the lean, the pelvis with its share.
                if (wound.y <= WAIST_Y) {
                    CHEST.transform(out.set(wound)).add(NECK);
                } else {
                    CHEST.transform(V.set(0.0F, WAIST_Y, 0.0F)).add(NECK);
                    if (wound.y <= PELVIS_Y) {
                        LEAN.transform(out.set(wound).sub(0.0F, WAIST_Y, 0.0F)).add(V);
                    } else {
                        LEAN.transform(AT.set(0.0F, PELVIS_Y - WAIST_Y, 0.0F)).add(V);
                        PELVIS.identity().slerp(LEAN, HIPS_SHARE)
                                .transform(out.set(wound).sub(0.0F, PELVIS_Y, 0.0F)).add(AT);
                    }
                }
            }
        }
        // A wound on its back is out of reach.
        return wound.z < 1.5F;
    }

    // A hand to the wound (WOUND), palm to it and fingers across the body, `reach` of the way from where it was.
    private static void hold(HumanoidModel<?> person, Reaction r, boolean right, float reach) {
        float side = right ? -1.0F : 1.0F;
        // The wound's outward face as the body stood: its front, a side, or the top of its head.
        Vector3f normal = V;
        Vector3f wound = r.wound;
        if (r.where == Reaction.Where.TRUNK && Math.abs(wound.x) > 3.0F) {
            normal.set(Math.signum(wound.x), 0.0F, 0.0F);
        } else if (r.where == Reaction.Where.HEAD && Math.abs(wound.x) > 3.0F) {
            normal.set(Math.signum(wound.x), 0.0F, 0.0F);
        } else {
            normal.set(0.0F, 0.0F, -1.0F);
        }
        Stance.chest(CHEST);
        CHEST.transform(normal);
        HAND.set(WOUND).add(normal.x * OFF_SKIN, normal.y * OFF_SKIN, normal.z * OFF_SKIN);
        // Two hands share the wound side by side.
        if (r.bothHands) {
            CHEST.transform(POLE.set(side * 1.5F, right ? 0.5F : -0.5F, 0.0F));
            HAND.add(POLE);
        }
        Stance.hand(person, right, AT);
        AT.lerp(HAND, reach);
        // The palm faces into the body: a right hand's inner side is its +x, a left hand's its -x; fingers across.
        Vector3f in = POLE.set(normal).mul(right ? -1.0F : 1.0F);
        Vector3f fingers = FOOT.set(-side, 0.6F, 0.0F);
        CHEST.transform(fingers);
        fingers.sub(in.x * fingers.dot(in), in.y * fingers.dot(in), in.z * fingers.dot(in));
        if (fingers.lengthSquared() < 1.0E-6F) {
            fingers.set(0.0F, 1.0F, 0.0F);
        }
        fingers.normalize();
        in.normalize();
        Vector3f across = new Vector3f(in).cross(fingers);
        AXES.set(in.x, in.y, in.z, fingers.x, fingers.y, fingers.z, across.x, across.y, across.z);
        AXES.getNormalizedRotation(PALM);
        Stance.arm(person, right, AT, POLE.set(side, 0.0F, 1.0F).normalize(), PALM, reach);
    }

    // The other arm thrown out for balance, the further the more the body leans.
    private static void balance(HumanoidModel<?> person, boolean right, float pitch, float roll, float w) {
        float lean = Mth.clamp((Math.abs(pitch) + Math.abs(roll)) / FULL_LEAN, 0.0F, 1.0F) * w;
        if (lean < 1.0E-3F) {
            return;
        }
        ModelPart arm = right ? person.rightArm : person.leftArm;
        float side = right ? -1.0F : 1.0F;
        Stance.chest(CHEST);
        // Leaning back it reaches ahead; leaning to a side the arm on the other side goes out.
        float ahead = AHEAD * Mth.clamp(-pitch / FULL_LEAN, 0.2F, 1.0F);
        float out = OUT * (1.0F + Mth.clamp(roll * -side / FULL_LEAN, 0.0F, 1.0F));
        CHEST.transform(HAND.set(side * out, DOWN, -ahead)).add(arm.x, arm.y, arm.z);
        Stance.hand(person, right, AT);
        AT.lerp(HAND, lean);
        Stance.arm(person, right, AT, POLE.set(side, 0.0F, 1.0F).normalize());
    }

    // A world point into the creature's model (pixels), as it is drawn now.
    private static void model(LivingEntity entity, @Nullable Matrix4f drawn, float partialTick, double x, double y,
            double z, Vector3f out) {
        if (drawn != null) {
            Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            BACK.set(drawn).invert().transformPosition((float) (x - camera.x), (float) (y - camera.y),
                    (float) (z - camera.z), out);
            out.mul(16.0F);
            return;
        }
        Vec3 at = entity.getPosition(partialTick);
        Reaction.toModel(entity, new Vec3(x - at.x, y - at.y, z - at.z), true, out);
    }

    // Any other creature: its trunk bent at the waist from the blow, its head jerked.
    private static void creature(EntityModel<?> model, Reaction r, float partialTick, float w) {
        float pitch = Mth.lerp(partialTick, r.leanWas[0], r.lean[0]) * w * CREATURE_BEND;
        float roll = Mth.lerp(partialTick, r.leanWas[1], r.lean[1]) * w * CREATURE_BEND;
        Limbs.spine(model, TURN.rotationX(pitch).rotateZ(roll));
        ModelPart head = HEADS.computeIfAbsent(model, StaggerPose::head).orElse(null);
        if (head != null) {
            head.xRot += Mth.lerp(partialTick, r.headWas, r.head) * w * CREATURE_BEND;
        }
    }

    private static Optional<ModelPart> head(EntityModel<?> model) {
        List<ModelParts.Part> parts = ModelParts.of(model);
        if (parts != null) {
            for (ModelParts.Part part : parts) {
                if (part.role() == ModelParts.Role.HEAD) {
                    return Optional.of(part.part());
                }
            }
        }
        return Optional.empty();
    }
}
