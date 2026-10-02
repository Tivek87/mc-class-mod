package nl.tivek.multiversepowers.engine.client.pose;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.ragdoll.Ragdolls;
import nl.tivek.multiversepowers.engine.client.render.entity.EntityPass;
import nl.tivek.multiversepowers.engine.entity.Fatigue;
import org.joml.Quaternionf;
import org.joml.Vector3f;

// How worn down by blows each creature near is (the server tells, Fatigue), and a person's pose for it: the more worn,
// the lower it sags at the hips with its knees bent deeper, the further it hunches over and hangs its head, and the
// wider its arms hang, swaying a little where it stands.
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class Tired {
    // The share of the way to how worn the server says it is that the pose goes each tick.
    private static final float FOLLOW = 0.2F;
    // Fully worn (all of Fatigue.HITS): the hips this much lower (pixels) and back, the hunch and the bow at the waist,
    // the head hung and the arms out (radians), and the sway.
    private static final float SAG = 2.6F;
    private static final float HIPS_BACK = 0.6F;
    private static final float HUNCH = 0.32F;
    private static final float BOW = 0.22F;
    private static final float HEAD_DOWN = 0.35F;
    private static final float ARMS_OUT = 0.16F;
    private static final float SWAY = 0.07F;
    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);

    // Per creature: how worn the server says it is, and how worn it is shown now and the tick before.
    private static final Int2ObjectOpenHashMap<float[]> WORN = new Int2ObjectOpenHashMap<>();
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Vector3f HIPS = new Vector3f();
    private static final Vector3f RIGHT = new Vector3f();
    private static final Vector3f LEFT = new Vector3f();

    private Tired() {
    }

    public static void told(int entity, int hits) {
        float[] worn = WORN.get(entity);
        if (worn == null) {
            if (hits <= 0) {
                return;
            }
            worn = new float[3];
            WORN.put(entity, worn);
        }
        worn[0] = Mth.clamp(hits, 0, Fatigue.HITS) / (float) Fatigue.HITS;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        WORN.int2ObjectEntrySet().removeIf(entry -> {
            float[] worn = entry.getValue();
            worn[2] = worn[1];
            worn[1] += (worn[0] - worn[1]) * FOLLOW;
            return worn[0] <= 0.0F && worn[1] < 0.01F;
        });
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            WORN.remove(event.getEntity().getId());
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        WORN.clear();
    }

    public static boolean pose(EntityModel<?> model, LivingEntity entity, float partialTick) {
        float[] state = WORN.get(entity.getId());
        // A creature a power holds or poses is not sagging on its own feet.
        if (state == null || !(model instanceof HumanoidModel<?> person) || !EntityPass.inWorld()
                || entity.isPassenger() || entity.isSleeping() || entity.isSwimming() || Ragdolls.taken(entity)) {
            return false;
        }
        float worn = Mth.lerp(partialTick, state[2], state[1]);
        if (worn < 0.01F) {
            return false;
        }
        Stance.foot(person, true, RIGHT);
        Stance.foot(person, false, LEFT);
        float time = entity.tickCount + partialTick + entity.getId() * 7.0F;
        float sway = (float) (Math.sin(time * 0.09) * 0.6 + Math.sin(time * 0.23) * 0.4) * SWAY * worn;
        LEAN.rotationX(HUNCH * worn).rotateZ(sway);
        WAIST.rotationX(BOW * worn);
        HIPS.set(0.0F, Stance.HIP_Y + SAG * worn, HIPS_BACK * worn);
        Stance.trunk(person, HIPS, LEAN, WAIST);
        Stance.leg(person, true, RIGHT, KNEE);
        Stance.leg(person, false, LEFT, KNEE);
        person.head.xRot += HEAD_DOWN * worn;
        person.rightArm.zRot += ARMS_OUT * worn;
        person.leftArm.zRot -= ARMS_OUT * worn;
        return true;
    }
}
