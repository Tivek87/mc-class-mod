package nl.tivek.multiversepowers.spell.client;

import it.unimi.dsi.fastutil.ints.Int2LongMap;
import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.engine.client.pose.Stance;
import nl.tivek.multiversepowers.engine.client.render.FirstPersonArm;
import nl.tivek.multiversepowers.engine.math.Ease;
import nl.tivek.multiversepowers.spell.ClapPayload;
import org.joml.Quaternionf;
import org.joml.Vector3f;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientClaps {
    // The server's effect takes its first step a tick after the cast.
    private static final float MEET = ClapPayload.HANDS_MEET + 1.0F;
    private static final float OPENED = 5.0F;
    private static final float WIDE = MEET - 2.0F;
    private static final float RAISED = 4.0F;
    private static final float HOLD = MEET + 9.0F;
    private static final float END = HOLD + 7.0F;

    private static final float ARM_FORWARD = -1.4F;
    private static final float OPEN_TURN = 1.25F;
    private static final float SHUT_TURN = -0.3F;
    private static final float HEAD_BACK = -0.6F;

    static final Vector3f CLAP = new Vector3f(0.05F, -0.1F, -0.75F);
    private static final Vector3f SPREAD = new Vector3f(0.78F, -0.02F, -0.52F);
    private static final Vector3f ARM_FROM = new Vector3f(0.75F, -1.1F, -0.15F);

    private static final Vector3f KNEE = new Vector3f(0.0F, 0.0F, -1.0F);
    private static final Vector3f RIGHT_ELBOW = new Vector3f(-0.6F, 0.1F, 0.8F);
    private static final Vector3f LEFT_ELBOW = new Vector3f(0.6F, 0.1F, 0.8F);
    private static final Quaternionf LEAN = new Quaternionf();
    private static final Quaternionf WAIST = new Quaternionf();
    private static final Quaternionf CHEST = new Quaternionf();
    private static final Vector3f HIPS = new Vector3f();
    private static final Vector3f NECK = new Vector3f();

    private static final Int2LongMap STARTED = new Int2LongOpenHashMap();

    private ClientClaps() {
    }

    public static void clap(int entity) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            STARTED.put(entity, minecraft.level.getGameTime());
        }
    }

    private static float age(Entity entity, float partialTick) {
        if (!STARTED.containsKey(entity.getId())) {
            return -1.0F;
        }
        float age = entity.level().getGameTime() - STARTED.get(entity.getId()) + partialTick;
        if (age >= END || age < 0.0F) {
            STARTED.remove(entity.getId());
            return -1.0F;
        }
        return age;
    }

    private static float raised(float age) {
        if (age < RAISED) {
            return (float) Ease.smooth(age / RAISED);
        }
        return age < HOLD ? 1.0F : 1.0F - (float) Ease.smooth((age - HOLD) / (END - HOLD));
    }

    private static float open(float age) {
        if (age < WIDE) {
            return (float) Ease.smooth(age / OPENED);
        }
        float shut = Math.min(1.0F, (age - WIDE) / (MEET - WIDE));
        return 1.0F - shut * shut;
    }

    public static boolean pose(PlayerModel<?> model, Entity entity) {
        float age = age(entity, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        if (age < 0.0F) {
            return false;
        }
        float up = raised(age);
        float open = open(age);
        float turn = Mth.lerp(open, SHUT_TURN, OPEN_TURN);
        float pitch = model.head.xRot;
        float forward = ARM_FORWARD + Mth.clamp(pitch, -1.2F, 1.0F);
        model.rightArm.xRot = Mth.lerp(up, model.rightArm.xRot, forward);
        model.rightArm.yRot = Mth.lerp(up, model.rightArm.yRot, turn);
        model.rightArm.zRot = Mth.lerp(up, model.rightArm.zRot, 0.0F);
        model.leftArm.xRot = Mth.lerp(up, model.leftArm.xRot, forward);
        model.leftArm.yRot = Mth.lerp(up, model.leftArm.yRot, -turn);
        model.leftArm.zRot = Mth.lerp(up, model.leftArm.zRot, 0.0F);
        float back = up * open;
        model.head.xRot = Mth.lerp(back, pitch, Math.max(-1.4F, pitch + HEAD_BACK));
        body(model, age, up, open);
        return true;
    }

    // The whole body in the clap: set wide and low, the chest thrown open with the arms drawn back (elbows soft), then
    // driving forward over the knees into the clap and rocking back from its shock.
    private static void body(PlayerModel<?> model, float age, float up, float open) {
        Vector3f[] feet = { Stance.foot(model, true, new Vector3f()), Stance.foot(model, false, new Vector3f()) };
        float drive = (float) (Ease.smooth((age - WIDE) / (MEET - WIDE))
                * (1.0 - Ease.smooth((age - HOLD) / (END - HOLD))));
        float shock = (float) Ease.bump(Mth.clamp((age - MEET) / 6.0F, 0.0F, 1.0F));
        float drop = up * (2.2F + 1.4F * drive) - 0.6F * shock;
        float lean = up * (-0.1F * open + 0.24F * drive - 0.08F * shock);
        float arch = up * (-0.16F * open + 0.2F * drive);
        LEAN.rotationX(lean);
        WAIST.rotationX(arch);
        HIPS.set(0.0F, Stance.HIP_Y + drop, 0.3F * up);
        Stance.trunk(model, HIPS, LEAN, WAIST);
        for (int side = 0; side < 2; side++) {
            float sign = side == 0 ? -1.0F : 1.0F;
            Vector3f foot = feet[side].lerp(new Vector3f(sign * 3.3F, Stance.GROUND, side == 0 ? 1.2F : -1.0F), up);
            Stance.leg(model, side == 0, foot, KNEE);
        }
        // Soft elbows: each hand pulled a little in along its arm while spread, straight at the clap.
        Stance.neck(NECK);
        Stance.chest(CHEST);
        for (int side = 0; side < 2; side++) {
            boolean right = side == 0;
            Vector3f hand = Stance.hand(model, right, new Vector3f());
            Vector3f shoulder = right ? new Vector3f(model.rightArm.x, model.rightArm.y, model.rightArm.z)
                    : new Vector3f(model.leftArm.x, model.leftArm.y, model.leftArm.z);
            hand.lerp(shoulder, 0.14F * up * open);
            Stance.arm(model, right, hand, right ? RIGHT_ELBOW : LEFT_ELBOW);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || player.isInvisible()) {
            return;
        }
        float age = age(player, event.getPartialTick());
        if (age < 0.0F) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        float up = raised(age);
        float open = open(age);
        PlayerRenderer renderer = (PlayerRenderer) minecraft.getEntityRenderDispatcher().getRenderer(player);
        for (float side = -1.0F; side <= 1.0F; side += 2.0F) {
            Vector3f rest = side > 0.0F ? FirstPersonArm.HAND_RIGHT : FirstPersonArm.HAND_LEFT;
            Vector3f target = new Vector3f(CLAP).lerp(SPREAD, open);
            target.x *= side;
            Vector3f hand = new Vector3f(rest).lerp(target, up);
            Vector3f from = new Vector3f(ARM_FROM.x * side, ARM_FROM.y, ARM_FROM.z);
            FirstPersonArm.arm(event.getPoseStack(), event.getMultiBufferSource(), event.getPackedLight(), player,
                    renderer, side, hand, from);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        STARTED.clear();
    }
}
