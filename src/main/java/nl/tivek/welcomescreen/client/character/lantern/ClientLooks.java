package nl.tivek.welcomescreen.client.character.lantern;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.Camera;
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
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderArmEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import nl.tivek.welcomescreen.WelcomeScreenMod;
import nl.tivek.welcomescreen.character.GameCharacter;
import nl.tivek.welcomescreen.network.CharacterLookPayload;
import org.joml.Vector3f;

/**
 * Who every player around is (the server tells), and the change when someone becomes Green Lantern: a line
 * of green light slides down from head to feet and leaves the uniform behind it. Changing back, the line
 * slides up and the uniform falls apart into green sparks.
 */
@EventBusSubscriber(modid = WelcomeScreenMod.MODID, value = Dist.CLIENT)
public final class ClientLooks {
    // How long the ring takes to put the uniform on, or to take it off, in ticks.
    private static final int CHANGE_TICKS = 20;
    private static final DustParticleOptions SPARK = new DustParticleOptions(new Vector3f(0.24F, 0.91F, 0.42F), 0.9F);

    private static final Map<Integer, Look> LOOKS = new HashMap<>();
    private static int clientTicks;

    private ClientLooks() {
    }

    /** Who a player is, who they were before, and since which tick (for the change). */
    private record Look(@Nullable GameCharacter now, @Nullable GameCharacter before, int since, boolean animate) {
        /** 0 at the start of the change, 1 once it is done. */
        float progress(float partialTick) {
            return this.animate ? Mth.clamp((clientTicks - this.since + partialTick) / CHANGE_TICKS, 0.0F, 1.0F)
                    : 1.0F;
        }

        boolean changing() {
            return this.animate && clientTicks - this.since < CHANGE_TICKS;
        }

        boolean puttingOn() {
            return this.now == GameCharacter.GREEN_LANTERN;
        }

        boolean takingOff() {
            return this.before == GameCharacter.GREEN_LANTERN && this.now != GameCharacter.GREEN_LANTERN;
        }
    }

    /**
     * How much of the uniform a player has on right now.
     *
     * @param line     height in the world of the line of light; the uniform shows above it
     * @param shown    how much of it is on, 0 to 1
     * @param ring     how brightly the ring shows, 0 to 1
     * @param complete true when it is fully on and nothing changes
     */
    record Uniform(double line, float shown, float ring, boolean complete) {
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
    static Uniform uniform(Entity player, float partialTick) {
        Look look = LOOKS.get(player.getId());
        if (look == null || !(look.puttingOn() || look.takingOff())) {
            return null;
        }
        float t = look.progress(partialTick);
        if (t >= 1.0F) {
            return look.puttingOn() ? new Uniform(Double.NEGATIVE_INFINITY, 1.0F, 1.0F, true) : null;
        }
        float eased = t * t * (3 - 2 * t);
        double bottom = Mth.lerp(partialTick, player.yOld, player.getY()) - 0.1;
        double top = bottom + player.getBbHeight() + 0.2;
        if (look.puttingOn()) {
            // The ring is there first: it is what makes the uniform.
            return new Uniform(top - (top - bottom) * eased, eased, 1.0F, false);
        }
        return new Uniform(bottom + (top - bottom) * eased, 1.0F - eased, 1.0F - eased, false);
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
            Map.Entry<Integer, Look> entry = looks.next();
            Look look = entry.getValue();
            if (!look.changing()) {
                if (look.now() == null) {
                    looks.remove();
                }
                continue;
            }
            Entity player = level.getEntity(entry.getKey());
            if (player != null && !player.isInvisible() && (look.puttingOn() || look.takingOff())) {
                sparks(level, player, look);
            }
        }
    }

    /** Green sparks flying off the line of light; falling ones while the uniform comes apart. */
    private static void sparks(ClientLevel level, Entity player, Look look) {
        Uniform uniform = uniform(player, 0.0F);
        if (uniform == null) {
            return;
        }
        double radius = player.getBbWidth() * 0.5 + 0.2;
        for (int i = 0; i < 3; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2;
            double x = Math.cos(angle);
            double z = Math.sin(angle);
            double fall = look.takingOff() ? -0.06 : 0.0;
            level.addParticle(SPARK, player.getX() + x * radius, uniform.line(), player.getZ() + z * radius,
                    x * 0.05, fall, z * 0.05);
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        LOOKS.clear();
    }

    /** The line of light itself: a ring of light around the body, sliding along it. */
    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || LOOKS.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return;
        }
        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera camera = event.getCamera();
        ConstructPainter painter = null;
        for (Map.Entry<Integer, Look> entry : LOOKS.entrySet()) {
            Look look = entry.getValue();
            Entity player = level.getEntity(entry.getKey());
            // Seen from your own eyes the line would run right through the camera.
            if (!look.changing() || player == null || player.isInvisible()
                    || player == minecraft.player && !camera.isDetached()) {
                continue;
            }
            Uniform uniform = uniform(player, partialTick);
            if (uniform == null || uniform.complete()) {
                continue;
            }
            if (painter == null) {
                painter = new ConstructPainter(event.getPoseStack(), camera.getPosition(),
                        (float) (level.getGameTime() % 24000L) + partialTick);
            }
            Vec3 at = player.getPosition(partialTick);
            float t = look.progress(partialTick);
            painter.band(new Vec3(at.x, uniform.line(), at.z), player.getBbWidth() * 0.5 + 0.2,
                    0.4 + 0.6 * Mth.sin(t * Mth.PI));
        }
        if (painter != null) {
            painter.finish(minecraft.renderBuffers().bufferSource());
        }
    }

    /**
     * Your own arm in first person: the sleeve and glove of the uniform over it, and the ring on the right
     * hand. The game draws your own arm right after this; the sleeve is just outside it, so it covers it.
     */
    @SubscribeEvent
    public static void onRenderArm(RenderArmEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        AbstractClientPlayer player = event.getPlayer();
        Uniform uniform = uniform(player, minecraft.getTimer().getGameTimeDeltaPartialTick(false));
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
        // Solid, halfway through the change: a see-through sleeve would hide your own arm under it.
        if (uniform.shown() >= 0.5F) {
            sleeve.render(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(GreenLanternSuitLayer.TEXTURE)),
                    light, OverlayTexture.NO_OVERLAY);
        }
        boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
        float partialTick = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        // The light running down your arm into the ring while it works.
        float glow = uniform.complete() ? SuitGlow.level(player, partialTick) : 0.0F;
        SuitGlow.arm(poseStack, buffers, sleeve, right, slim, glow, player.tickCount + partialTick);
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
