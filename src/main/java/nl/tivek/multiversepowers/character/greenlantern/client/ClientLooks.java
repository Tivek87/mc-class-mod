package nl.tivek.multiversepowers.character.greenlantern.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import nl.tivek.multiversepowers.MultiversePowers;
import nl.tivek.multiversepowers.character.CharacterLookPayload;
import nl.tivek.multiversepowers.character.GameCharacter;
import nl.tivek.multiversepowers.character.greenlantern.Arrival;
import nl.tivek.multiversepowers.character.greenlantern.client.body.GreenLanternSuitLayer;
import nl.tivek.multiversepowers.character.greenlantern.client.body.Ring;
import nl.tivek.multiversepowers.character.greenlantern.client.body.RingSpot;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SuitGlow;
import nl.tivek.multiversepowers.character.greenlantern.client.body.SuitSpread;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import nl.tivek.multiversepowers.engine.math.Ease;

@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientLooks {
    public static final int UNDRESS_TICKS = 56;
    public static final int DEPART_TICKS = UNDRESS_TICKS + 50;
    private static final int WAIT_TICKS = 40;
    private static final float RING_Y = 8.8F;
    private static final float ARM_REACH = 11.5F;
    private static final float CORE_Y = 4.5F;
    private static final float CORE_Z = -2.3F;
    private static final float CORE_REACH = 15.0F;
    private static final float MASK_Y = -4.3F;
    private static final float MASK_Z = -4.8F;
    private static final float MASK_REACH = 9.0F;

    private static final Map<Integer, Look> LOOKS = new HashMap<>();
    private static int clientTicks;

    private ClientLooks() {
    }

    private record Look(@Nullable GameCharacter now, @Nullable GameCharacter before, int since, boolean animate) {
        boolean puttingOn() {
            return this.now == GameCharacter.GREEN_LANTERN;
        }

        boolean takingOff() {
            return this.before == GameCharacter.GREEN_LANTERN && this.now != GameCharacter.GREEN_LANTERN;
        }

        float age(float partialTick) {
            return clientTicks - this.since + partialTick;
        }

        boolean changing() {
            if (!this.animate) {
                return false;
            }
            return clientTicks - this.since < (this.takingOff() ? DEPART_TICKS : WAIT_TICKS);
        }
    }

    public record Uniform(float arm, float torso, float rest, float mask, float ring, boolean complete) {
        static final Uniform FULL = new Uniform(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, true);
        static final Uniform NONE = new Uniform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);

        public SuitSpread.Field armField() {
            float reach = ARM_REACH * this.arm;
            return (x, y, z) -> reach - Math.abs(y - RING_Y);
        }

        public SuitSpread.Field torsoField() {
            float reach = CORE_REACH * this.torso;
            return (x, y, z) -> reach - Mth.sqrt(x * x + (y - CORE_Y) * (y - CORE_Y) + (z - CORE_Z) * (z - CORE_Z));
        }

        public SuitSpread.Field leftArmField() {
            float front = -2.5F + 13.0F * this.rest;
            return (x, y, z) -> front - y;
        }

        public SuitSpread.Field legField() {
            float front = -0.5F + 13.0F * this.rest;
            return (x, y, z) -> front - y;
        }

        public SuitSpread.Field maskField() {
            float reach = MASK_REACH * this.mask;
            return (x, y, z) -> reach - Mth.sqrt(x * x + (y - MASK_Y) * (y - MASK_Y) + (z - MASK_Z) * (z - MASK_Z));
        }

        public float core() {
            return this.torso > 0.0F && this.torso < 1.0F ? Mth.sin(Mth.PI * Math.min(1.0F, this.torso * 1.6F)) : 0.0F;
        }
    }

    public static void update(CharacterLookPayload payload) {
        GameCharacter[] all = GameCharacter.values();
        GameCharacter now = payload.character() >= 0 && payload.character() < all.length
                ? all[payload.character()]
                : null;
        Look old = LOOKS.get(payload.entity());
        GameCharacter before = old == null ? null : old.now();
        if (now == null && (!payload.animate() || before == null)) {
            LOOKS.remove(payload.entity());
            return;
        }
        LOOKS.put(payload.entity(), new Look(now, before, clientTicks, payload.animate()));
    }

    @Nullable
    public static Uniform uniform(Entity player, float partialTick) {
        Look look = LOOKS.get(player.getId());
        if (look == null || !(look.puttingOn() || look.takingOff())) {
            return null;
        }
        if (look.takingOff()) {
            float age = look.age(partialTick);
            if (!look.animate() || age >= DEPART_TICKS) {
                return null;
            }
            float u = Mth.clamp(age / UNDRESS_TICKS, 0.0F, 1.0F);
            if (u >= 1.0F) {
                return Uniform.NONE;
            }
            return new Uniform(1.0F - (float) Ease.smooth((u - 0.6F) / 0.35F),
                    1.0F - (float) Ease.smooth((u - 0.35F) / 0.3F),
                    1.0F - (float) Ease.smooth((u - 0.1F) / 0.35F), 1.0F - (float) Ease.smooth(u / 0.15F), 1.0F, false);
        }
        float arrival = ClientRing.arrival(player, partialTick);
        if (arrival < 0.0F) {
            // Negative means the ring's arrival packet has not reached this client yet.
            return look.animate() && look.age(partialTick) < WAIT_TICKS ? Uniform.NONE : Uniform.FULL;
        }
        if (arrival < Arrival.RING_ON) {
            return Uniform.NONE;
        }
        float s = (arrival - Arrival.RING_ON) / Arrival.SUIT_TICKS;
        if (s >= 1.0F) {
            return Uniform.FULL;
        }
        return new Uniform((float) Ease.smooth(s / 0.28F), (float) Ease.smooth((s - 0.25F) / 0.35F),
                (float) Ease.smooth((s - 0.42F) / 0.4F), (float) Ease.smooth((s - 0.86F) / 0.14F), 1.0F, false);
    }

    public static float departure(Entity player, float partialTick) {
        Look look = LOOKS.get(player.getId());
        if (look == null || !look.takingOff() || !look.animate()) {
            return -1.0F;
        }
        float age = look.age(partialTick);
        return age < DEPART_TICKS ? age : -1.0F;
    }

    static List<Integer> departing() {
        return LOOKS.entrySet().stream().filter(entry -> entry.getValue().takingOff() && entry.getValue().changing())
                .map(Map.Entry::getKey).toList();
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }
        clientTicks++;
        Iterator<Map.Entry<Integer, Look>> looks = LOOKS.entrySet().iterator();
        while (looks.hasNext()) {
            Look look = looks.next().getValue();
            if (!look.changing() && look.now() == null) {
                looks.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LOOKS.clear();
    }

    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = event.getPlayer();
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        Uniform uniform = uniform(player, partialTick);
        if (uniform == null) {
            return;
        }
        PlayerModel<AbstractClientPlayer> suit = GreenLanternSuitLayer.model(player);
        suit.attackTime = 0.0F;
        suit.crouching = false;
        suit.swimAmount = 0.0F;
        suit.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        suit.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        suit.setupAnim(player, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        boolean right = event.getArm() == HumanoidArm.RIGHT;
        ModelPart sleeve = right ? suit.rightArm : suit.leftArm;
        sleeve.xRot = 0.0F;
        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        if (uniform.complete()) {
            sleeve.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(GreenLanternSuitLayer.TEXTURE)),
                    light, OverlayTexture.NO_OVERLAY);
        } else {
            SuitSpread spread = new SuitSpread(
                    buffers.getBuffer(RenderType.entityCutoutNoCull(GreenLanternSuitLayer.TEXTURE)));
            spread.render(sleeve, right ? uniform.armField() : uniform.leftArmField(), poseStack, light,
                    OverlayTexture.NO_OVERLAY);
            spread.seam(buffers, 1.0F);
        }
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        float glow = uniform.complete() ? SuitGlow.level(player, partialTick) : 0.0F;
        SuitGlow.arm(poseStack, buffers, sleeve, right, slim, glow, player.tickCount + partialTick,
                BeamCharge.charge(player, partialTick));
        if (right) {
            poseStack.pushPose();
            sleeve.translateAndRotate(poseStack);
            Ring.draw(poseStack, buffers, light, slim, uniform.ring(), ClientRing.charge(player), glow);
            if (player == minecraft.player) {
                RingSpot.onHand(poseStack, Ring.stone(slim));
            }
            poseStack.popPose();
        }
    }
}
