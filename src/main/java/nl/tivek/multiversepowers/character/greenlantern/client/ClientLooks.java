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
import nl.tivek.multiversepowers.character.greenlantern.client.render.ArrivalAnimation;
import nl.tivek.multiversepowers.character.greenlantern.client.render.BeamCharge;
import nl.tivek.multiversepowers.engine.math.Ease;

/**
 * Who every player around is (the server tells), and how much of Green Lantern's uniform they have on while the ring
 * dresses them or undresses them.
 * <ul>
 * <li><b>Putting it on</b> follows the ring's arrival (see {@link Arrival} and {@link ArrivalAnimation}): the moment
 * the ring is on the finger the uniform bursts out of it, runs up the arm and down over the fingers, the lantern on the
 * chest lights up and the uniform spreads from there over the whole body, down the other arm and the legs; the mask
 * over the eyes comes last, once all the rest is on.</li>
 * <li><b>Taking it off</b> runs the other way: the mask goes first, then the uniform draws back from the hands and feet
 * into the lantern on the chest, from there up the arm, and back into the ring, which then slides off the finger and
 * flies away up into the sky (see {@link ArrivalAnimation}).</li>
 * </ul>
 * The uniform is cut off along the edge it has got to (see {@link SuitSpread}), so it is solid wherever it is.
 */
@EventBusSubscriber(modid = MultiversePowers.MODID, value = Dist.CLIENT)
public final class ClientLooks {
    /** How long the uniform takes to draw back into the ring, and then how long the ring takes to fly off, in ticks. */
    public static final int UNDRESS_TICKS = 56;
    public static final int DEPART_TICKS = UNDRESS_TICKS + 50;
    // How long someone who just became Green Lantern waits, unclothed, for the ring's arrival to be told, in ticks.
    private static final int WAIT_TICKS = 40;
    // Where on the ring hand the uniform starts, along the arm in its own pixels (the shoulder at -2, the fingertips
    // at 10): at the ring. And how far it has to go from there to cover the whole arm.
    private static final float RING_Y = 8.8F;
    private static final float ARM_REACH = 11.5F;
    // The lantern on the chest, where the uniform spreads over the body from, in the body's own pixels, and how far it
    // has to go to cover all of it; the same for the mask, from between the eyes.
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

    /** Who a player is, who they were before, and since which tick (for the change). */
    private record Look(@Nullable GameCharacter now, @Nullable GameCharacter before, int since, boolean animate) {
        boolean puttingOn() {
            return this.now == GameCharacter.GREEN_LANTERN;
        }

        boolean takingOff() {
            return this.before == GameCharacter.GREEN_LANTERN && this.now != GameCharacter.GREEN_LANTERN;
        }

        /** Ticks since the change (with the part of a tick). */
        float age(float partialTick) {
            return clientTicks - this.since + partialTick;
        }

        /** True while a change is still being played: the uniform being taken off, or waiting for the ring. */
        boolean changing() {
            if (!this.animate) {
                return false;
            }
            return clientTicks - this.since < (this.takingOff() ? DEPART_TICKS : WAIT_TICKS);
        }
    }

    /**
     * How much of the uniform a player has on right now, each part from 0 (none of it) to 1 (all of it).
     *
     * @param arm      the ring arm: from the ring up to the shoulder and down over the fingers
     * @param torso    the body: from the lantern on the chest outwards
     * @param rest     the other arm and the legs: from the body outwards
     * @param mask     the mask, from between the eyes outwards
     * @param ring     whether the ring is on the finger, 0 or 1
     * @param complete true when all of it is on and nothing changes
     */
    public record Uniform(float arm, float torso, float rest, float mask, float ring, boolean complete) {
        static final Uniform FULL = new Uniform(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, true);
        static final Uniform NONE = new Uniform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, false);

        /** The ring arm covered from the ring as far as it has got, up the arm and down over the fingers. */
        public SuitSpread.Field armField() {
            float reach = ARM_REACH * this.arm;
            return (x, y, z) -> reach - Math.abs(y - RING_Y);
        }

        /** The body covered from the lantern on the chest out to as far as it has got. */
        public SuitSpread.Field torsoField() {
            float reach = CORE_REACH * this.torso;
            return (x, y, z) -> reach - Mth.sqrt(x * x + (y - CORE_Y) * (y - CORE_Y) + (z - CORE_Z) * (z - CORE_Z));
        }

        /** The other arm covered from the shoulder down (the shoulder at -2, the hand at 10). */
        public SuitSpread.Field leftArmField() {
            float front = -2.5F + 13.0F * this.rest;
            return (x, y, z) -> front - y;
        }

        /** A leg covered from the hip down (the hip at 0, the foot at 12). */
        public SuitSpread.Field legField() {
            float front = -0.5F + 13.0F * this.rest;
            return (x, y, z) -> front - y;
        }

        /** The mask covered from between the eyes outwards. */
        public SuitSpread.Field maskField() {
            float reach = MASK_REACH * this.mask;
            return (x, y, z) -> reach - Mth.sqrt(x * x + (y - MASK_Y) * (y - MASK_Y) + (z - MASK_Z) * (z - MASK_Z));
        }

        /** How brightly the lantern on the chest flares as the uniform bursts out of it, 0 to 1. */
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

    /** How much of Green Lantern's uniform this player wears right now, or null for none at all. */
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
            // The mask first, then the other arm and the legs into the body, the body into the lantern on the chest,
            // and last the ring arm, back into the ring.
            return new Uniform(1.0F - (float) Ease.smooth((u - 0.6F) / 0.35F),
                    1.0F - (float) Ease.smooth((u - 0.35F) / 0.3F),
                    1.0F - (float) Ease.smooth((u - 0.1F) / 0.35F), 1.0F - (float) Ease.smooth(u / 0.15F), 1.0F, false);
        }
        float arrival = ClientRing.arrival(player, partialTick);
        if (arrival < 0.0F) {
            // Just become Green Lantern, and the ring's arrival not told yet: nothing on for a moment. Otherwise it is
            // all on, as it is for anyone who has been Green Lantern for a while.
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

    /**
     * How far into taking the uniform off this player is, in ticks, or -1 when they are not: the ring flies off once
     * the uniform is back in it (see {@link ArrivalAnimation}).
     */
    public static float departure(Entity player, float partialTick) {
        Look look = LOOKS.get(player.getId());
        if (look == null || !look.takingOff() || !look.animate()) {
            return -1.0F;
        }
        float age = look.age(partialTick);
        return age < DEPART_TICKS ? age : -1.0F;
    }

    /** Every player that is taking the uniform off right now, by entity id. */
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

    /**
     * Your own arm in first person: the sleeve and glove of the uniform over it, as far as it has got, and the ring on
     * the right hand. The game draws your own arm right after this; the sleeve is just outside it, so it covers it.
     */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = event.getPlayer();
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        Uniform uniform = uniform(player, partialTick);
        if (uniform == null) {
            return;
        }
        // The same pose the game gives your own arm here: standing still, nothing swinging.
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
            // Only as far as it has got, with a seam of light along its edge.
            SuitSpread spread = new SuitSpread(
                    buffers.getBuffer(RenderType.entityCutoutNoCull(GreenLanternSuitLayer.TEXTURE)));
            spread.render(sleeve, right ? uniform.armField() : uniform.leftArmField(), poseStack, light,
                    OverlayTexture.NO_OVERLAY);
            spread.seam(buffers, 1.0F);
        }
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        // The light running down your arm into the ring while it works.
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
